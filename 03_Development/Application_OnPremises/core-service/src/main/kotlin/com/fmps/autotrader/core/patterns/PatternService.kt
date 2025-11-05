package com.fmps.autotrader.core.patterns

import com.fmps.autotrader.core.database.repositories.PatternRepository
import com.fmps.autotrader.core.database.repositories.TradeRepository
import com.fmps.autotrader.core.patterns.models.*
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.TradeAction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Service for managing trading patterns.
 * 
 * Provides pattern storage, querying, matching, learning, and pruning capabilities.
 * Patterns are extracted from successful trades and used to inform future trading decisions.
 * 
 * Thread-safe implementation using Mutex for concurrent access.
 */
class PatternService(
    private val patternRepository: PatternRepository,
    private val tradeRepository: TradeRepository,
    private val relevanceCalculator: RelevanceCalculator = RelevanceCalculator()
) {
    private val logger = LoggerFactory.getLogger(PatternService::class.java)
    private val mutex = Mutex()
    
    /**
     * Store a new trading pattern.
     * 
     * @param pattern Pattern to store
     * @return Result containing pattern ID on success, or error
     */
    suspend fun storePattern(pattern: TradingPattern): Result<String> {
        return mutex.withLock {
            try {
                logger.info("Storing pattern: ${pattern.name ?: pattern.id} for ${pattern.symbol} on ${pattern.exchange}")
                
                // Convert TradingPattern to database format
                val rsiRange = pattern.conditions["RSI_Range"] as? Pair<*, *>
                val macdRange = pattern.conditions["MACD_Range"] as? Pair<*, *>
                
                val patternId = patternRepository.create(
                    name = pattern.name,
                    patternType = pattern.conditions["patternType"] as? String ?: "CUSTOM",
                    tradingPair = pattern.symbol,
                    timeframe = pattern.timeframe,
                    tradeType = pattern.action.name,
                    rsiMin = rsiRange?.first?.let { 
                        when (it) {
                            is BigDecimal -> it
                            is Double -> it.toBigDecimal()
                            is Number -> it.toDouble().toBigDecimal()
                            else -> null
                        }
                    },
                    rsiMax = rsiRange?.second?.let {
                        when (it) {
                            is BigDecimal -> it
                            is Double -> it.toBigDecimal()
                            is Number -> it.toDouble().toBigDecimal()
                            else -> null
                        }
                    },
                    macdMin = macdRange?.first?.let {
                        when (it) {
                            is BigDecimal -> it
                            is Double -> it.toBigDecimal()
                            is Number -> it.toDouble().toBigDecimal()
                            else -> null
                        }
                    },
                    macdMax = macdRange?.second?.let {
                        when (it) {
                            is BigDecimal -> it
                            is Double -> it.toBigDecimal()
                            is Number -> it.toDouble().toBigDecimal()
                            else -> null
                        }
                    },
                    description = pattern.description,
                    tags = pattern.tags.joinToString(",").takeIf { pattern.tags.isNotEmpty() }
                )
                
                logger.info("Pattern stored with database ID: $patternId")
                // Return database ID as string (pattern.id is UUID, but we use DB ID for lookups)
                Result.success(patternId.toString())
            } catch (e: Exception) {
                logger.error("Failed to store pattern", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Query patterns by criteria.
     * 
     * @param criteria Search criteria
     * @return List of matching patterns
     */
    suspend fun queryPatterns(criteria: PatternCriteria): List<TradingPattern> {
        return mutex.withLock {
            try {
                logger.debug("Querying patterns with criteria: $criteria")
                
                // Query database patterns
                val dbPatterns = when {
                    criteria.symbol != null && criteria.timeframe != null -> {
                        patternRepository.findByTradingPair(criteria.symbol)
                            .filter { it.timeframe == criteria.timeframe }
                    }
                    criteria.symbol != null -> {
                        patternRepository.findByTradingPair(criteria.symbol)
                    }
                    else -> {
                        patternRepository.findActive()
                    }
                }
                
                // Convert database patterns to TradingPattern
                val patterns = dbPatterns.map { dbPattern ->
                    convertToTradingPattern(dbPattern)
                }
                
                // Apply in-memory filters
                val filtered = patterns.filter { pattern ->
                    matchesCriteria(pattern, criteria)
                }
                
                // Sort by confidence and success rate
                val sorted = filtered.sortedByDescending { 
                    it.calculateConfidence() * it.successRate 
                }
                
                logger.debug("Found ${sorted.size} patterns matching criteria")
                sorted
            } catch (e: Exception) {
                logger.error("Failed to query patterns", e)
                emptyList()
            }
        }
    }
    
    /**
     * Match current market conditions to stored patterns.
     * 
     * @param conditions Current market conditions
     * @param minRelevance Minimum relevance score (default: 0.6)
     * @param maxResults Maximum number of results (default: 10)
     * @return List of matched patterns sorted by relevance
     */
    suspend fun matchPatterns(
        conditions: MarketConditions,
        minRelevance: Double = 0.6,
        maxResults: Int = 10
    ): List<MatchedPattern> {
        return mutex.withLock {
            try {
                logger.debug("Matching patterns for ${conditions.symbol} on ${conditions.exchange}")
                
                // Query relevant patterns
                val criteria = PatternCriteria(
                    exchange = conditions.exchange,
                    symbol = conditions.symbol
                )
                val patterns = queryPatterns(criteria)
                
                // Calculate relevance for each pattern
                val matches = patterns.mapNotNull { pattern ->
                    val relevance = relevanceCalculator.calculateRelevance(pattern, conditions)
                    if (relevance >= minRelevance) {
                        val matchedConditions = extractMatchedConditions(pattern, conditions)
                        val confidence = pattern.calculateConfidence() * relevance
                        
                        MatchedPattern(
                            pattern = pattern,
                            relevanceScore = relevance,
                            matchedConditions = matchedConditions,
                            confidence = confidence
                        )
                    } else {
                        null
                    }
                }
                
                // Sort by relevance (descending) and limit results
                val sorted = matches.sortedByDescending { it.relevanceScore }
                val limited = sorted.take(maxResults)
                
                logger.debug("Found ${limited.size} matching patterns (min relevance: $minRelevance)")
                limited
            } catch (e: Exception) {
                logger.error("Failed to match patterns", e)
                emptyList()
            }
        }
    }
    
    /**
     * Update pattern performance after a trade outcome.
     * 
     * @param patternId Pattern ID
     * @param outcome Trade outcome
     * @return Result indicating success or failure
     */
    suspend fun updatePatternPerformance(patternId: String, outcome: TradeOutcome): Result<Unit> {
        return mutex.withLock {
            try {
                logger.debug("Updating performance for pattern $patternId: success=${outcome.success}, return=${outcome.returnAmount}")
                
                // Convert pattern ID string to database ID
                val dbPatternId = patternId.toIntOrNull()
                    ?: return Result.failure(Exception("Invalid pattern ID: $patternId"))
                
                val dbPattern = patternRepository.findById(dbPatternId)
                
                if (dbPattern != null) {
                    val success = patternRepository.updateStatistics(
                        patternId = dbPattern.id,
                        profitLoss = outcome.returnAmount,
                        isSuccessful = outcome.success
                    )
                    
                    if (success) {
                        logger.debug("Pattern performance updated successfully")
                        Result.success(Unit)
                    } else {
                        logger.warn("Failed to update pattern statistics")
                        Result.failure(Exception("Failed to update pattern statistics"))
                    }
                } else {
                    logger.warn("Pattern not found: $patternId")
                    Result.failure(Exception("Pattern not found: $patternId"))
                }
            } catch (e: Exception) {
                logger.error("Failed to update pattern performance", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Prune patterns based on criteria.
     * 
     * Removes patterns that match the prune criteria (old, low performance, etc.).
     * 
     * @param criteria Prune criteria
     * @return Result containing number of pruned patterns
     */
    suspend fun prunePatterns(criteria: PruneCriteria): Result<Int> {
        return mutex.withLock {
            try {
                if (criteria.isEmpty()) {
                    logger.warn("Empty prune criteria - nothing to prune")
                    return Result.success(0)
                }
                
                logger.info("Pruning patterns with criteria: $criteria")
                
                val allPatterns = patternRepository.findActive()
                var prunedCount = 0
                
                for (dbPattern in allPatterns) {
                    val pattern = convertToTradingPattern(dbPattern)
                    var shouldPrune = false
                    
                    // Check age
                    if (criteria.maxAge != null) {
                        val age = Duration.between(
                            pattern.createdAt,
                            Instant.now()
                        )
                        if (age > criteria.maxAge) {
                            shouldPrune = true
                            logger.debug("Pruning pattern ${pattern.id}: age ${age.toDays()} days exceeds max ${criteria.maxAge.toDays()}")
                        }
                    }
                    
                    // Check success rate
                    if (criteria.minSuccessRate != null && pattern.successRate < criteria.minSuccessRate) {
                        shouldPrune = true
                        logger.debug("Pruning pattern ${pattern.id}: success rate ${pattern.successRate} below min ${criteria.minSuccessRate}")
                    }
                    
                    // Check usage count
                    if (criteria.minUsageCount != null && pattern.usageCount < criteria.minUsageCount) {
                        shouldPrune = true
                        logger.debug("Pruning pattern ${pattern.id}: usage count ${pattern.usageCount} below min ${criteria.minUsageCount}")
                    }
                    
                    if (shouldPrune) {
                        patternRepository.deactivate(dbPattern.id)
                        prunedCount++
                    }
                }
                
                // If maxPatterns is specified, keep only top N
                if (criteria.maxPatterns != null) {
                    val remaining = patternRepository.findActive()
                        .sortedByDescending { it.successRate }
                        .sortedByDescending { it.totalOccurrences }
                    
                    if (remaining.size > criteria.maxPatterns) {
                        val toRemove = remaining.drop(criteria.maxPatterns)
                        for (pattern in toRemove) {
                            patternRepository.deactivate(pattern.id)
                            prunedCount++
                        }
                    }
                }
                
                logger.info("Pruned $prunedCount patterns")
                Result.success(prunedCount)
            } catch (e: Exception) {
                logger.error("Failed to prune patterns", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get pattern by ID.
     * 
     * @param patternId Pattern ID
     * @return Pattern if found, null otherwise
     */
    suspend fun getPattern(patternId: String): TradingPattern? {
        return mutex.withLock {
            try {
                // Convert pattern ID string to database ID
                val dbPatternId = patternId.toIntOrNull()
                    ?: return null
                
                val dbPattern = patternRepository.findById(dbPatternId)
                
                dbPattern?.let { convertToTradingPattern(it) }
            } catch (e: Exception) {
                logger.error("Failed to get pattern", e)
                null
            }
        }
    }
    
    /**
     * Get top performing patterns.
     * 
     * @param limit Maximum number of patterns to return
     * @param minOccurrences Minimum occurrences required
     * @return List of top performing patterns
     */
    suspend fun getTopPerformingPatterns(limit: Int = 10, minOccurrences: Int = 5): List<TradingPattern> {
        return mutex.withLock {
            try {
                val dbPatterns = patternRepository.getTopPatterns(limit, minOccurrences)
                dbPatterns.map { convertToTradingPattern(it) }
            } catch (e: Exception) {
                logger.error("Failed to get top performing patterns", e)
                emptyList()
            }
        }
    }
    
    // Helper methods
    
    /**
     * Convert database Pattern to TradingPattern model.
     */
    private fun convertToTradingPattern(dbPattern: com.fmps.autotrader.core.database.repositories.Pattern): TradingPattern {
        // Build conditions map from database fields
        val conditions = mutableMapOf<String, Any>()
        
        if (dbPattern.rsiMin != null || dbPattern.rsiMax != null) {
            conditions["RSI_Range"] = Pair(
                dbPattern.rsiMin?.toDouble() ?: 0.0,
                dbPattern.rsiMax?.toDouble() ?: 100.0
            )
        }
        
        if (dbPattern.macdMin != null || dbPattern.macdMax != null) {
            conditions["MACD_Range"] = Pair(
                dbPattern.macdMin?.toDouble() ?: Double.MIN_VALUE,
                dbPattern.macdMax?.toDouble() ?: Double.MAX_VALUE
            )
        }
        
        conditions["patternType"] = dbPattern.patternType
        
        // Parse tags
        val tags = dbPattern.tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        
        // Convert trade type to TradeAction
        val action = when (dbPattern.tradeType.uppercase()) {
            "LONG" -> TradeAction.LONG
            "SHORT" -> TradeAction.SHORT
            else -> TradeAction.LONG // Default
        }
        
        // Note: Exchange is not stored in PatternsTable directly
        // For now, we'll need to store it in description or tags, or add it to schema later
        // Defaulting to BINANCE for now (can be enhanced later)
        val exchange = Exchange.BINANCE // TODO: Extract from description/tags or add to schema
        
        return TradingPattern(
            id = dbPattern.id.toString(),
            name = dbPattern.name,
            exchange = exchange,
            symbol = dbPattern.tradingPair,
            timeframe = dbPattern.timeframe,
            action = action,
            conditions = conditions,
            confidence = dbPattern.confidence.toDouble() / 100.0, // Convert from percentage
            createdAt = dbPattern.createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant(),
            lastUsedAt = dbPattern.lastMatchedAt?.atZone(java.time.ZoneId.systemDefault())?.toInstant(),
            usageCount = dbPattern.totalOccurrences,
            successCount = dbPattern.successfulTrades,
            successRate = dbPattern.successRate.toDouble() / 100.0, // Convert from percentage
            averageReturn = dbPattern.averageProfitLoss,
            description = dbPattern.description,
            tags = tags
        )
    }
    
    /**
     * Check if pattern matches criteria.
     */
    private fun matchesCriteria(pattern: TradingPattern, criteria: PatternCriteria): Boolean {
        if (criteria.exchange != null && pattern.exchange != criteria.exchange) return false
        if (criteria.symbol != null && pattern.symbol != criteria.symbol) return false
        if (criteria.action != null && pattern.action != criteria.action) return false
        if (criteria.timeframe != null && pattern.timeframe != criteria.timeframe) return false
        if (criteria.minSuccessRate != null && pattern.successRate < criteria.minSuccessRate) return false
        if (criteria.minUsageCount != null && pattern.usageCount < criteria.minUsageCount) return false
        if (criteria.minConfidence != null && pattern.calculateConfidence() < criteria.minConfidence) return false
        if (criteria.maxAge != null) {
            val age = Duration.between(pattern.createdAt, Instant.now())
            if (age > criteria.maxAge) return false
        }
        if (criteria.tags != null && criteria.tags.isNotEmpty()) {
            val hasMatchingTag = criteria.tags.any { tag -> pattern.tags.contains(tag) }
            if (!hasMatchingTag) return false
        }
        return true
    }
    
    /**
     * Extract which conditions matched between pattern and market conditions.
     */
    private fun extractMatchedConditions(
        pattern: TradingPattern,
        conditions: MarketConditions
    ): Map<String, Any> {
        val matched = mutableMapOf<String, Any>()
        
        // Check each indicator
        pattern.conditions.forEach { (key, value) ->
            when (key) {
                "RSI_Range" -> {
                    val currentRSI = conditions.getRSI()
                    if (currentRSI != null) {
                        val range = value as? Pair<*, *>
                        if (range != null) {
                            val min = (range.first as? Number)?.toDouble() ?: 0.0
                            val max = (range.second as? Number)?.toDouble() ?: 100.0
                            if (currentRSI in min..max) {
                                matched["RSI"] = currentRSI
                            }
                        }
                    }
                }
                "MACD_Range" -> {
                    val currentMACD = conditions.getMACD()
                    if (currentMACD != null) {
                        matched["MACD"] = currentMACD
                    }
                }
                "SMA" -> {
                    val currentSMA = conditions.getSMA()
                    if (currentSMA != null) {
                        matched["SMA"] = currentSMA
                    }
                }
                "EMA" -> {
                    val currentEMA = conditions.getEMA()
                    if (currentEMA != null) {
                        matched["EMA"] = currentEMA
                    }
                }
                "BollingerBands" -> {
                    val currentBB = conditions.getBollingerBands()
                    if (currentBB != null) {
                        matched["BollingerBands"] = currentBB
                    }
                }
            }
        }
        
        return matched
    }
}

