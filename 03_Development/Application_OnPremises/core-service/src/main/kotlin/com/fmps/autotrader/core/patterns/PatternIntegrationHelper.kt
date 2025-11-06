package com.fmps.autotrader.core.patterns

import com.fmps.autotrader.core.database.repositories.TradeRepository
import com.fmps.autotrader.core.patterns.models.TradeOutcome
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Instant

/**
 * Helper class for integrating PatternService with AITrader.
 * 
 * Handles:
 * - Updating pattern performance after trades close
 * - Extracting patterns from successful trades
 * - Coordinating between PatternService and TradeRepository
 * 
 * This class bridges the gap between the trading system and pattern storage system.
 */
class PatternIntegrationHelper(
    private val patternService: PatternService,
    private val patternLearner: PatternLearner,
    private val tradeRepository: TradeRepository
) {
    private val logger = LoggerFactory.getLogger(PatternIntegrationHelper::class.java)
    
    /**
     * Update pattern performance after a trade closes.
     * 
     * Called when a trade that used a pattern is closed.
     * 
     * @param patternId Pattern ID that was used in the trade
     * @param tradeId Trade ID that just closed
     * @return Result indicating success or failure
     */
    suspend fun updatePatternAfterTradeClose(patternId: String, tradeId: Int): Result<Unit> {
        return try {
            logger.debug("Updating pattern $patternId performance after trade $tradeId closes")
            
            // Get trade details
            val trade = tradeRepository.findById(tradeId)
                ?: return Result.failure(Exception("Trade not found: $tradeId"))
            
            // Check if trade is closed
            if (trade.status != "CLOSED") {
                logger.debug("Trade $tradeId is not closed yet, skipping pattern update")
                return Result.success(Unit)
            }
            
            // Get profit/loss
            val profitLoss = trade.profitLoss ?: BigDecimal.ZERO
            val isSuccessful = profitLoss > BigDecimal.ZERO
            
            // Create trade outcome
            val outcome = TradeOutcome(
                patternId = patternId,
                success = isSuccessful,
                returnAmount = profitLoss,
                timestamp = trade.exitTimestamp?.atZone(java.time.ZoneId.systemDefault())?.toInstant() 
                    ?: Instant.now()
            )
            
            // Update pattern performance
            val result = patternService.updatePatternPerformance(patternId, outcome)
            
            if (result.isSuccess) {
                logger.info(
                    "Pattern $patternId updated: trade $tradeId " +
                    "resulted in ${if (isSuccessful) "profit" else "loss"} of $profitLoss"
                )
            } else {
                logger.warn("Failed to update pattern $patternId: ${result.exceptionOrNull()?.message}")
            }
            
            result
        } catch (e: Exception) {
            logger.error("Error updating pattern after trade close", e)
            Result.failure(e)
        }
    }
    
    /**
     * Extract and store patterns from successful trades.
     * 
     * Analyzes closed trades and extracts patterns for future use.
     * 
     * @param traderId Optional AI trader ID to filter trades
     * @param minProfitPercent Minimum profit percentage to consider (default: 1%)
     * @return Result containing number of patterns extracted
     */
    suspend fun extractPatternsFromSuccessfulTrades(
        traderId: Int? = null,
        minProfitPercent: BigDecimal = BigDecimal("0.01")
    ): Result<Int> {
        return try {
            logger.info("Extracting patterns from successful trades (traderId=$traderId, minProfit=$minProfitPercent%)")
            
            // Extract patterns using PatternLearner
            val patterns = patternLearner.extractPatternsFromTrades(traderId, minProfitPercent)
            
            logger.info("Extracted ${patterns.size} patterns from trades")
            
            // Store each pattern
            var storedCount = 0
            var failedCount = 0
            
            for (pattern in patterns) {
                // Validate pattern
                if (!patternLearner.validatePattern(pattern)) {
                    logger.warn("Pattern ${pattern.id} failed validation, skipping")
                    failedCount++
                    continue
                }
                
                // Store pattern
                val result = patternService.storePattern(pattern)
                if (result.isSuccess) {
                    storedCount++
                    logger.debug("Stored pattern: ${pattern.id} (${pattern.name ?: "unnamed"})")
                } else {
                    failedCount++
                    logger.warn("Failed to store pattern ${pattern.id}: ${result.exceptionOrNull()?.message}")
                }
            }
            
            logger.info(
                "Pattern extraction complete: $storedCount stored, $failedCount failed " +
                "out of ${patterns.size} extracted"
            )
            
            Result.success(storedCount)
        } catch (e: Exception) {
            logger.error("Error extracting patterns from trades", e)
            Result.failure(e)
        }
    }
    
    /**
     * Extract pattern from a single successful trade.
     * 
     * @param tradeId Trade ID to extract pattern from
     * @return Result containing pattern ID if successful
     */
    suspend fun extractPatternFromTrade(tradeId: Int): Result<String> {
        return try {
            logger.debug("Extracting pattern from trade $tradeId")
            
            // Extract pattern using PatternLearner
            val pattern = patternLearner.extractPatternFromTrade(tradeId)
                ?: return Result.failure(Exception("Failed to extract pattern from trade $tradeId"))
            
            // Validate pattern
            if (!patternLearner.validatePattern(pattern)) {
                return Result.failure(Exception("Extracted pattern failed validation"))
            }
            
            // Store pattern
            val result = patternService.storePattern(pattern)
            
            if (result.isSuccess) {
                logger.info("Extracted and stored pattern ${pattern.id} from trade $tradeId")
            }
            
            result
        } catch (e: Exception) {
            logger.error("Error extracting pattern from trade $tradeId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Process trade closure and update patterns.
     * 
     * This is a convenience method that:
     * 1. Updates pattern performance if pattern was used
     * 2. Optionally extracts new pattern if trade was successful
     * 
     * @param tradeId Trade ID that just closed
     * @param extractPatternIfSuccessful If true, extract pattern from successful trades
     * @return Result indicating success or failure
     */
    suspend fun processTradeClosure(
        tradeId: Int,
        extractPatternIfSuccessful: Boolean = false
    ): Result<Unit> {
        return try {
            logger.debug("Processing trade closure for trade $tradeId")
            
            // Get trade details
            val trade = tradeRepository.findById(tradeId)
                ?: return Result.failure(Exception("Trade not found: $tradeId"))
            
            // Update pattern performance if pattern was used
            trade.patternId?.let { patternId ->
                updatePatternAfterTradeClose(patternId.toString(), tradeId)
            }
            
            // Extract pattern if trade was successful and extraction is enabled
            if (extractPatternIfSuccessful && trade.status == "CLOSED") {
                val profitLoss = trade.profitLoss ?: BigDecimal.ZERO
                if (profitLoss > BigDecimal.ZERO) {
                    logger.debug("Trade $tradeId was successful, extracting pattern")
                    extractPatternFromTrade(tradeId)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Error processing trade closure", e)
            Result.failure(e)
        }
    }
    
    /**
     * Track pattern usage when a trade is created.
     * 
     * This method is called when a trade is created that uses a pattern.
     * Currently, pattern usage is tracked via the trade's patternId field,
     * so this is a no-op but kept for API consistency.
     * 
     * @param patternId Pattern ID that was used
     * @param tradeId Trade ID that was created
     */
    suspend fun trackPatternUsage(patternId: String?, tradeId: Int) {
        if (patternId != null) {
            logger.debug("Tracking pattern usage: pattern $patternId used in trade $tradeId")
            // Pattern usage is tracked via trade.patternId in the database
            // No additional action needed here
        }
    }
    
    /**
     * Update pattern performance when a trade closes.
     * 
     * Convenience method that matches AITrader's expected API.
     * 
     * @param tradeId Trade ID that closed
     * @param patternId Pattern ID (optional, will be retrieved from trade if not provided)
     */
    suspend fun updatePatternPerformance(tradeId: Int, patternId: String? = null) {
        val actualPatternId = patternId ?: run {
            val trade = tradeRepository.findById(tradeId)
            trade?.patternId?.toString()
        }
        
        if (actualPatternId != null) {
            updatePatternAfterTradeClose(actualPatternId, tradeId)
        } else {
            logger.debug("No pattern ID for trade $tradeId, skipping pattern update")
        }
    }
    
    /**
     * Learn pattern from a trade.
     * 
     * Convenience method that matches AITrader's expected API.
     * 
     * @param tradeId Trade ID to learn from
     */
    suspend fun learnFromTrade(tradeId: Int) {
        extractPatternFromTrade(tradeId)
    }
}
