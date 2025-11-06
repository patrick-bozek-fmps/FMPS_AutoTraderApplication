package com.fmps.autotrader.core.patterns

import com.fmps.autotrader.core.indicators.models.MACDResult
import com.fmps.autotrader.core.patterns.models.MarketConditions
import com.fmps.autotrader.core.patterns.models.MatchedPattern
import com.fmps.autotrader.core.patterns.models.TradeOutcome
import com.fmps.autotrader.core.patterns.models.TradingPattern
import com.fmps.autotrader.core.traders.ProcessedMarketData
import com.fmps.autotrader.core.traders.SignalAction
import com.fmps.autotrader.core.traders.TradingSignal
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.TradeAction
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Instant

/**
 * Integration layer between PatternService and AITrader.
 * 
 * Provides methods to:
 * - Use patterns when generating trading signals
 * - Store patterns after successful trades
 * - Update pattern performance after trade outcomes
 */
class PatternIntegration(
    private val patternService: PatternService,
    private val patternLearner: PatternLearner
) {
    private val logger = LoggerFactory.getLogger(PatternIntegration::class.java)
    
    /**
     * Get pattern-based trading signals for current market conditions.
     * 
     * Matches current market conditions against stored patterns and returns
     * trading signals based on matched patterns with high relevance.
     * 
     * @param processedData Processed market data from AITrader
     * @param exchange Exchange identifier
     * @param symbol Trading pair symbol (e.g., "BTCUSDT")
     * @param minRelevance Minimum relevance score (default: 0.7)
     * @param maxPatterns Maximum number of patterns to consider (default: 5)
     * @return List of trading signals based on matched patterns
     */
    suspend fun getPatternBasedSignals(
        processedData: ProcessedMarketData,
        exchange: Exchange,
        symbol: String,
        minRelevance: Double = 0.7,
        maxPatterns: Int = 5
    ): List<TradingSignal> {
        try {
            // Convert ProcessedMarketData to MarketConditions
            val marketConditions = convertToMarketConditions(processedData, exchange, symbol)
            
            // Match patterns
            val matchedPatterns = patternService.matchPatterns(
                conditions = marketConditions,
                minRelevance = minRelevance,
                maxResults = maxPatterns
            )
            
            if (matchedPatterns.isEmpty()) {
                logger.debug("No patterns matched for $symbol on $exchange")
                return emptyList()
            }
            
            logger.info("Found ${matchedPatterns.size} matching patterns for $symbol")
            
            // Convert matched patterns to trading signals
            return matchedPatterns.mapNotNull { matchedPattern ->
                convertToTradingSignal(matchedPattern, processedData)
            }
        } catch (e: Exception) {
            logger.error("Failed to get pattern-based signals", e)
            return emptyList()
        }
    }
    
    /**
     * Get the best pattern-based signal (highest relevance).
     * 
     * @param processedData Processed market data
     * @param exchange Exchange identifier
     * @param symbol Trading pair symbol
     * @param minRelevance Minimum relevance score
     * @return Best trading signal based on patterns, or null if no match
     */
    suspend fun getBestPatternSignal(
        processedData: ProcessedMarketData,
        exchange: Exchange,
        symbol: String,
        minRelevance: Double = 0.7
    ): TradingSignal? {
        val signals = getPatternBasedSignals(processedData, exchange, symbol, minRelevance, maxPatterns = 1)
        return signals.firstOrNull()
    }
    
    /**
     * Store a pattern from a successful trade.
     * 
     * Extracts market conditions from a successful trade and stores them as a pattern.
     * 
     * @param tradeId ID of the successful trade
     * @return Pattern ID if extraction and storage successful, null otherwise
     */
    suspend fun storePatternFromTrade(tradeId: Int): String? {
        try {
            logger.info("Extracting pattern from trade $tradeId")
            
            // Extract pattern from trade
            val pattern = patternLearner.extractPatternFromTrade(tradeId)
                ?: return null
            
            // Validate pattern
            if (!patternLearner.validatePattern(pattern)) {
                logger.warn("Extracted pattern ${pattern.id} failed validation")
                return null
            }
            
            // Store pattern
            val result = patternService.storePattern(pattern)
            
            return result.getOrNull()
        } catch (e: Exception) {
            logger.error("Failed to store pattern from trade $tradeId", e)
            return null
        }
    }
    
    /**
     * Update pattern performance after a trade outcome.
     * 
     * @param patternId Pattern ID that was used in the trade
     * @param success Whether the trade was successful
     * @param returnAmount Profit or loss amount
     */
    suspend fun updatePatternPerformance(
        patternId: String,
        success: Boolean,
        returnAmount: BigDecimal
    ) {
        try {
            val outcome = TradeOutcome(
                patternId = patternId,
                success = success,
                returnAmount = returnAmount,
                timestamp = Instant.now()
            )
            
            val result = patternService.updatePatternPerformance(patternId, outcome)
            
            if (result.isFailure) {
                logger.warn("Failed to update pattern performance for $patternId: ${result.exceptionOrNull()?.message}")
            } else {
                logger.debug("Updated pattern performance for $patternId: success=$success, return=$returnAmount")
            }
        } catch (e: Exception) {
            logger.error("Failed to update pattern performance for $patternId", e)
        }
    }
    
    /**
     * Extract and store patterns from multiple successful trades.
     * 
     * @param traderId Optional AI trader ID to filter trades
     * @param minProfitPercent Minimum profit percentage threshold
     * @return Number of patterns extracted and stored
     */
    suspend fun extractPatternsFromTrades(
        traderId: Int? = null,
        minProfitPercent: BigDecimal = BigDecimal("0.01")
    ): Int {
        try {
            logger.info("Extracting patterns from successful trades (traderId=$traderId, minProfit=$minProfitPercent%)")
            
            val patterns = patternLearner.extractPatternsFromTrades(traderId, minProfitPercent)
            
            var storedCount = 0
            for (pattern in patterns) {
                if (patternLearner.validatePattern(pattern)) {
                    val result = patternService.storePattern(pattern)
                    if (result.isSuccess) {
                        storedCount++
                        logger.debug("Stored pattern ${pattern.id} from trade extraction")
                    }
                }
            }
            
            logger.info("Extracted and stored $storedCount patterns from trades")
            return storedCount
        } catch (e: Exception) {
            logger.error("Failed to extract patterns from trades", e)
            return 0
        }
    }
    
    /**
     * Get pattern recommendations for a trading decision.
     * 
     * Combines pattern matching with confidence scoring to provide recommendations.
     * 
     * @param processedData Processed market data
     * @param exchange Exchange identifier
     * @param symbol Trading pair symbol
     * @return Pattern recommendation with confidence score
     */
    suspend fun getPatternRecommendation(
        processedData: ProcessedMarketData,
        exchange: Exchange,
        symbol: String
    ): PatternRecommendation? {
        val matchedPatterns = patternService.matchPatterns(
            conditions = convertToMarketConditions(processedData, exchange, symbol),
            minRelevance = 0.6,
            maxResults = 3
        )
        
        if (matchedPatterns.isEmpty()) {
            return null
        }
        
        // Get best match
        val bestMatch = matchedPatterns.first()
        
        // Calculate overall confidence
        val confidence = bestMatch.getFinalConfidence()
        
        return PatternRecommendation(
            pattern = bestMatch.pattern,
            relevanceScore = bestMatch.relevanceScore,
            confidence = confidence,
            action = bestMatch.pattern.action,
            alternativePatterns = matchedPatterns.drop(1)
        )
    }
    
    // Helper methods
    
    /**
     * Convert ProcessedMarketData to MarketConditions.
     */
    private fun convertToMarketConditions(
        processedData: ProcessedMarketData,
        exchange: Exchange,
        symbol: String
    ): MarketConditions {
        // Use indicators directly from ProcessedMarketData
        // They are already in a map format
        val indicators = processedData.indicators.toMutableMap()
        
        return MarketConditions(
            exchange = exchange,
            symbol = symbol,
            currentPrice = processedData.latestPrice,
            indicators = indicators,
            candlesticks = processedData.candles,
            timestamp = processedData.timestamp
        )
    }
    
    /**
     * Convert MatchedPattern to TradingSignal.
     */
    private fun convertToTradingSignal(
        matchedPattern: MatchedPattern,
        processedData: ProcessedMarketData
    ): TradingSignal? {
        val pattern = matchedPattern.pattern
        
        // Only generate signal if pattern action matches market conditions
        // For now, we trust the pattern's action
        val action = when (pattern.action) {
            TradeAction.LONG -> SignalAction.BUY
            TradeAction.SHORT -> SignalAction.SELL
        }
        
        // Calculate confidence from pattern
        val confidence = matchedPattern.getFinalConfidence()
        
        // Build reason from pattern
        val reason = buildString {
            append("Pattern: ${pattern.name ?: pattern.id}")
            append(" (relevance: ${String.format("%.2f", matchedPattern.relevanceScore * 100)}%)")
            if (pattern.successRate > 0) {
                append(", success rate: ${String.format("%.1f", pattern.successRate * 100)}%")
            }
        }
        
        return TradingSignal(
            action = action,
            confidence = confidence,
            reason = reason,
            timestamp = Instant.now(),
            matchedPatternId = pattern.id
        )
    }
}

/**
 * Pattern recommendation result.
 */
data class PatternRecommendation(
    val pattern: TradingPattern,
    val relevanceScore: Double,
    val confidence: Double,
    val action: TradeAction,
    val alternativePatterns: List<MatchedPattern> = emptyList()
) {
    /**
     * Check if recommendation is strong enough for trading.
     */
    fun isStrongRecommendation(threshold: Double = 0.7): Boolean {
        return confidence >= threshold && relevanceScore >= threshold
    }
}

