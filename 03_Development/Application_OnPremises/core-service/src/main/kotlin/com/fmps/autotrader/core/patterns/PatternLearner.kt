package com.fmps.autotrader.core.patterns

import com.fmps.autotrader.core.database.repositories.TradeRepository
import com.fmps.autotrader.core.patterns.models.TradingPattern
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.TradeAction
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Extracts trading patterns from successful trades.
 * 
 * Analyzes closed trades and extracts market conditions that led to success,
 * creating reusable patterns for future trading decisions.
 */
class PatternLearner(
    private val tradeRepository: TradeRepository
) {
    private val logger = LoggerFactory.getLogger(PatternLearner::class.java)
    
    // Minimum profit threshold for pattern extraction (1% by default)
    // Note: profitLossPercent is stored as percentage (1% = 1.0, not 0.01)
    private val minProfitThreshold = BigDecimal("1.0")
    
    // Minimum number of similar successful trades to create a reliable pattern
    private val minSimilarTrades = 2
    
    /**
     * Extract a pattern from a successful trade.
     * 
     * @param tradeId ID of the successful trade
     * @return TradingPattern if extraction successful, null otherwise
     */
    suspend fun extractPatternFromTrade(tradeId: Int): TradingPattern? {
        val trade = tradeRepository.findById(tradeId) ?: return null
        
        // Only extract from successful trades
        if (trade.status != "CLOSED" || trade.profitLoss == null || trade.profitLoss <= BigDecimal.ZERO) {
            logger.debug("Trade $tradeId is not successful - skipping pattern extraction")
            return null
        }
        
        // Check profit threshold
        val profitPercent = trade.profitLossPercent ?: BigDecimal.ZERO
        if (profitPercent < minProfitThreshold) {
            logger.debug("Trade $tradeId profit ${profitPercent}% below threshold ${minProfitThreshold}% - skipping")
            return null
        }
        
        logger.info("Extracting pattern from successful trade $tradeId (profit: ${trade.profitLoss})")
        
        // Extract market conditions from trade entry
        val conditions = extractMarketConditions(trade)
        
        // Convert trade type to TradeAction
        val action = when (trade.tradeType.uppercase()) {
            "LONG" -> TradeAction.LONG
            "SHORT" -> TradeAction.SHORT
            else -> TradeAction.LONG
        }
        
        // Convert exchange string to Exchange enum
        val exchange = try {
            Exchange.valueOf(trade.exchange.uppercase())
        } catch (e: Exception) {
            logger.warn("Unknown exchange: ${trade.exchange}, defaulting to BINANCE")
            Exchange.BINANCE
        }
        
        // Create pattern
        val pattern = TradingPattern(
            id = UUID.randomUUID().toString(),
            name = generatePatternName(trade),
            exchange = exchange,
            symbol = trade.tradingPair,
            timeframe = "1h", // Default - can be extracted from trade if stored
            action = action,
            conditions = conditions,
            confidence = 0.7, // Initial confidence for new patterns
            createdAt = Instant.now(),
            description = "Extracted from successful trade #${trade.id}",
            tags = listOf("auto-extracted", "trade-${trade.id}")
        )
        
        logger.info("Pattern extracted: ${pattern.id} for ${pattern.symbol}")
        return pattern
    }
    
    /**
     * Extract patterns from multiple successful trades.
     * 
     * Groups similar trades and creates patterns from them.
     * 
     * @param traderId Optional AI trader ID to filter trades
     * @param minProfitPercent Minimum profit percentage threshold
     * @return List of extracted patterns
     */
    suspend fun extractPatternsFromTrades(
        traderId: Int? = null,
        minProfitPercent: BigDecimal = minProfitThreshold
    ): List<TradingPattern> {
        logger.info("Extracting patterns from successful trades (traderId=$traderId, minProfit=$minProfitPercent%)")
        
        // Get successful trades
        val successfulTrades = tradeRepository.findSuccessfulTrades(traderId, limit = 1000)
            .filter { 
                it.profitLossPercent != null && 
                it.profitLossPercent >= minProfitPercent 
            }
        
        logger.info("Found ${successfulTrades.size} successful trades to analyze")
        
        // Extract patterns from each trade
        val patterns = successfulTrades.mapNotNull { trade ->
            extractPatternFromTrade(trade.id)
        }
        
        // Group similar patterns and merge them
        val mergedPatterns = mergeSimilarPatterns(patterns)
        
        logger.info("Extracted ${mergedPatterns.size} unique patterns from ${successfulTrades.size} trades")
        return mergedPatterns
    }
    
    /**
     * Extract market conditions from a trade.
     * 
     * Builds a conditions map with indicator values at trade entry.
     */
    private fun extractMarketConditions(trade: com.fmps.autotrader.core.database.repositories.Trade): Map<String, Any> {
        val conditions = mutableMapOf<String, Any>()
        
        // Add pattern type
        conditions["patternType"] = determinePatternType(trade)
        
        // Add RSI range if available
        trade.rsiValue?.let { rsi ->
            // Create a small range around the RSI value (±5 points)
            val rsiDouble = rsi.toDouble()
            conditions["RSI_Range"] = Pair(
                (rsiDouble - 5.0).coerceIn(0.0, 100.0),
                (rsiDouble + 5.0).coerceIn(0.0, 100.0)
            )
        }
        
        // Add MACD range if available
        trade.macdValue?.let { macd ->
            val macdDouble = macd.toDouble()
            conditions["MACD_Range"] = Pair(
                macdDouble - 0.001,
                macdDouble + 0.001
            )
        }
        
        // Add SMA values if available
        trade.smaShortValue?.let { smaShort ->
            conditions["SMA_Short"] = smaShort.toDouble()
        }
        
        trade.smaLongValue?.let { smaLong ->
            conditions["SMA_Long"] = smaLong.toDouble()
        }
        
        // Add entry price for reference
        conditions["entryPrice"] = trade.entryPrice.toDouble()
        
        // Add price range (±2% around entry price)
        val priceDouble = trade.entryPrice.toDouble()
        val priceRange = priceDouble * 0.02
        conditions["priceRange"] = Pair(
            priceDouble - priceRange,
            priceDouble + priceRange
        )
        
        return conditions
    }
    
    /**
     * Determine pattern type based on trade characteristics.
     * 
     * Priority order:
     * 1. RSI extremes (oversold/overbought reversals)
     * 2. Trend following (SMA short > SMA long) - checked before momentum
     * 3. Momentum continuation (positive MACD)
     * 4. Default to CUSTOM
     */
    private fun determinePatternType(trade: com.fmps.autotrader.core.database.repositories.Trade): String {
        // Simple heuristic based on indicators
        return when {
            trade.rsiValue != null && trade.rsiValue.toDouble() < 35.0 -> "OVERSOLD_REVERSAL"
            trade.rsiValue != null && trade.rsiValue.toDouble() > 65.0 -> "OVERBOUGHT_REVERSAL"
            // Check trend following before momentum (when both SMA values present, trend is more specific)
            trade.smaShortValue != null && trade.smaLongValue != null &&
                trade.smaShortValue > trade.smaLongValue -> "TREND_FOLLOWING"
            trade.macdValue != null && trade.macdValue > BigDecimal.ZERO -> "MOMENTUM_CONTINUATION"
            else -> "CUSTOM"
        }
    }
    
    /**
     * Generate a descriptive name for a pattern.
     */
    private fun generatePatternName(trade: com.fmps.autotrader.core.database.repositories.Trade): String {
        val patternType = determinePatternType(trade)
        val symbol = trade.tradingPair
        val profit = trade.profitLossPercent?.toDouble() ?: 0.0
        
        return "${patternType}_${symbol}_${String.format("%.2f", profit)}%"
    }
    
    /**
     * Merge similar patterns into a single pattern.
     * 
     * Patterns are considered similar if they have:
     * - Same exchange, symbol, action
     * - Similar indicator values (within tolerance)
     */
    private fun mergeSimilarPatterns(patterns: List<TradingPattern>): List<TradingPattern> {
        if (patterns.isEmpty()) return emptyList()
        
        val merged = mutableListOf<TradingPattern>()
        val processed = mutableSetOf<String>()
        
        for (pattern in patterns) {
            if (processed.contains(pattern.id)) continue
            
            // Find similar patterns
            val similar = patterns.filter { other ->
                !processed.contains(other.id) &&
                isSimilar(pattern, other)
            }
            
            if (similar.size >= minSimilarTrades) {
                // Merge similar patterns
                val mergedPattern = mergePatterns(similar)
                merged.add(mergedPattern)
                similar.forEach { processed.add(it.id) }
            } else {
                // Keep individual pattern if no similar ones found
                merged.add(pattern)
                processed.add(pattern.id)
            }
        }
        
        return merged
    }
    
    /**
     * Check if two patterns are similar enough to merge.
     */
    private fun isSimilar(pattern1: TradingPattern, pattern2: TradingPattern): Boolean {
        // Must have same exchange, symbol, and action
        if (pattern1.exchange != pattern2.exchange) return false
        if (pattern1.symbol != pattern2.symbol) return false
        if (pattern1.action != pattern2.action) return false
        
        // Check RSI similarity
        val rsi1 = pattern1.conditions["RSI_Range"] as? Pair<*, *>
        val rsi2 = pattern2.conditions["RSI_Range"] as? Pair<*, *>
        if (rsi1 != null && rsi2 != null) {
            val min1 = (rsi1.first as? Number)?.toDouble() ?: 0.0
            val max1 = (rsi1.second as? Number)?.toDouble() ?: 100.0
            val min2 = (rsi2.first as? Number)?.toDouble() ?: 0.0
            val max2 = (rsi2.second as? Number)?.toDouble() ?: 100.0
            
            // Check if ranges overlap
            if (max1 < min2 || max2 < min1) return false
        }
        
        // Check MACD similarity
        val macd1 = pattern1.conditions["MACD_Range"] as? Pair<*, *>
        val macd2 = pattern2.conditions["MACD_Range"] as? Pair<*, *>
        if (macd1 != null && macd2 != null) {
            val min1 = (macd1.first as? Number)?.toDouble() ?: Double.MIN_VALUE
            val max1 = (macd1.second as? Number)?.toDouble() ?: Double.MAX_VALUE
            val min2 = (macd2.first as? Number)?.toDouble() ?: Double.MIN_VALUE
            val max2 = (macd2.second as? Number)?.toDouble() ?: Double.MAX_VALUE
            
            if (max1 < min2 || max2 < min1) return false
        }
        
        return true
    }
    
    /**
     * Merge multiple similar patterns into one.
     */
    private fun mergePatterns(patterns: List<TradingPattern>): TradingPattern {
        require(patterns.isNotEmpty()) { "Cannot merge empty pattern list" }
        
        val first = patterns.first()
        
        // Merge conditions (expand ranges)
        val mergedConditions = mutableMapOf<String, Any>()
        mergedConditions["patternType"] = first.conditions["patternType"] ?: "CUSTOM"
        
        // Merge RSI ranges
        val rsiRanges = patterns.mapNotNull { it.conditions["RSI_Range"] as? Pair<*, *> }
        if (rsiRanges.isNotEmpty()) {
            val mins = rsiRanges.map { (it.first as? Number)?.toDouble() ?: 0.0 }
            val maxs = rsiRanges.map { (it.second as? Number)?.toDouble() ?: 100.0 }
            mergedConditions["RSI_Range"] = Pair(mins.minOrNull() ?: 0.0, maxs.maxOrNull() ?: 100.0)
        }
        
        // Merge MACD ranges
        val macdRanges = patterns.mapNotNull { it.conditions["MACD_Range"] as? Pair<*, *> }
        if (macdRanges.isNotEmpty()) {
            val mins = macdRanges.map { (it.first as? Number)?.toDouble() ?: Double.MIN_VALUE }
            val maxs = macdRanges.map { (it.second as? Number)?.toDouble() ?: Double.MAX_VALUE }
            mergedConditions["MACD_Range"] = Pair(mins.minOrNull() ?: Double.MIN_VALUE, maxs.maxOrNull() ?: Double.MAX_VALUE)
        }
        
        // Merge price ranges
        val priceRanges = patterns.mapNotNull { it.conditions["priceRange"] as? Pair<*, *> }
        if (priceRanges.isNotEmpty()) {
            val mins = priceRanges.map { (it.first as? Number)?.toDouble() ?: 0.0 }
            val maxs = priceRanges.map { (it.second as? Number)?.toDouble() ?: Double.MAX_VALUE }
            mergedConditions["priceRange"] = Pair(mins.minOrNull() ?: 0.0, maxs.maxOrNull() ?: Double.MAX_VALUE)
        }
        
        // Calculate average confidence
        val avgConfidence = patterns.map { it.confidence }.average()
        
        // Combine tags
        val allTags = patterns.flatMap { it.tags }.distinct()
        
        return first.copy(
            id = UUID.randomUUID().toString(),
            name = "${first.name}_merged_${patterns.size}",
            conditions = mergedConditions,
            confidence = avgConfidence,
            description = "Merged from ${patterns.size} similar patterns",
            tags = allTags + "merged"
        )
    }
    
    /**
     * Validate pattern quality before storing.
     * 
     * @param pattern Pattern to validate
     * @return true if pattern is valid, false otherwise
     */
    fun validatePattern(pattern: TradingPattern): Boolean {
        // Must have at least one indicator condition
        val hasIndicator = pattern.conditions.containsKey("RSI_Range") ||
                          pattern.conditions.containsKey("MACD_Range") ||
                          pattern.conditions.containsKey("SMA_Short") ||
                          pattern.conditions.containsKey("SMA_Long")
        
        if (!hasIndicator) {
            logger.warn("Pattern ${pattern.id} has no indicator conditions")
            return false
        }
        
        // Must have valid symbol
        if (pattern.symbol.isBlank()) {
            logger.warn("Pattern ${pattern.id} has blank symbol")
            return false
        }
        
        // Confidence must be in valid range
        if (pattern.confidence < 0.0 || pattern.confidence > 1.0) {
            logger.warn("Pattern ${pattern.id} has invalid confidence: ${pattern.confidence}")
            return false
        }
        
        return true
    }
}

