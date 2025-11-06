package com.fmps.autotrader.core.traders

import com.fmps.autotrader.core.patterns.PatternService
import com.fmps.autotrader.core.patterns.models.MarketConditions
import com.fmps.autotrader.core.traders.strategies.ITradingStrategy
import com.fmps.autotrader.shared.model.Position
import mu.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Generates trading signals by combining strategy signals with risk management checks and pattern matching.
 *
 * This class:
 * - Calls the strategy's generateSignal() method
 * - Matches patterns from PatternService (if available)
 * - Applies filters (risk limits, position limits)
 * - Calculates final confidence score (combining strategy and pattern confidence)
 * - Logs signal generation for debugging
 *
 * @since 1.0.0
 */
class SignalGenerator(
    private val strategy: ITradingStrategy,
    private val minConfidenceThreshold: Double = 0.5,
    private val patternService: PatternService? = null,
    private val config: AITraderConfig? = null,
    private val patternWeight: Double = 0.3 // Weight of pattern confidence (0.0-1.0)
) {
    init {
        require(minConfidenceThreshold >= 0.0 && minConfidenceThreshold <= 1.0) {
            "Min confidence threshold must be between 0.0 and 1.0, got: $minConfidenceThreshold"
        }
        require(patternWeight >= 0.0 && patternWeight <= 1.0) {
            "Pattern weight must be between 0.0 and 1.0, got: $patternWeight"
        }
    }

    /**
     * Generate a trading signal based on processed market data.
     *
     * @param processedData Processed market data with indicators
     * @param currentPosition Current open position, if any
     * @return Trading signal with action, confidence, and reasoning
     */
    suspend fun generateSignal(
        processedData: ProcessedMarketData,
        currentPosition: Position? = null
    ): TradingSignal {
        try {
            // Call strategy to generate signal
            val strategySignal = strategy.generateSignal(
                candles = processedData.candles,
                indicators = processedData.indicators
            )

            logger.debug {
                "Strategy generated signal: ${strategySignal.action} " +
                        "with confidence ${strategySignal.confidence}"
            }

            // Match patterns if PatternService is available
            val patternMatch = if (patternService != null && config != null) {
                matchPatterns(processedData, strategySignal)
            } else {
                null
            }

            // Apply filters
            val filteredSignal = applyFilters(strategySignal, currentPosition, patternMatch)

            // Calculate final confidence (combining strategy and pattern confidence)
            val finalConfidence = calculateFinalConfidence(
                strategySignal,
                filteredSignal,
                currentPosition,
                patternMatch
            )

            // Create final signal with pattern information
            val finalSignal = TradingSignal(
                action = filteredSignal.action,
                confidence = finalConfidence,
                reason = buildFinalReason(strategySignal, filteredSignal, currentPosition, patternMatch),
                timestamp = Instant.now(),
                indicatorValues = strategySignal.indicatorValues,
                matchedPatternId = patternMatch?.pattern?.id
            )

            logger.info {
                "Final signal: ${finalSignal.action} " +
                        "with confidence ${finalSignal.confidence} " +
                        "- ${finalSignal.reason}"
            }

            return finalSignal
        } catch (e: Exception) {
            logger.error(e) { "Error generating signal" }
            return TradingSignal(
                action = SignalAction.HOLD,
                confidence = 0.0,
                reason = "Error in signal generation: ${e.message}",
                timestamp = Instant.now(),
                indicatorValues = emptyMap()
            )
        }
    }

    /**
     * Match patterns to current market conditions.
     */
    private suspend fun matchPatterns(
        processedData: ProcessedMarketData,
        strategySignal: TradingSignal
    ): com.fmps.autotrader.core.patterns.models.MatchedPattern? {
        if (patternService == null || config == null) return null

        try {
            // Create MarketConditions from ProcessedMarketData
            val marketConditions = MarketConditions(
                exchange = config.exchange,
                symbol = config.symbol,
                currentPrice = processedData.latestPrice,
                indicators = processedData.indicators,
                candlesticks = processedData.candles,
                timestamp = processedData.timestamp
            )

            // Match patterns (get top match)
            val matches = patternService.matchPatterns(
                conditions = marketConditions,
                minRelevance = 0.6,
                maxResults = 1
            )

            if (matches.isNotEmpty()) {
                val match = matches.first()
                logger.debug {
                    "Pattern matched: ${match.pattern.id} " +
                            "with relevance ${match.relevanceScore} " +
                            "and confidence ${match.confidence}"
                }
                return match
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to match patterns, continuing without pattern enhancement" }
        }

        return null
    }

    /**
     * Apply filters to the strategy signal.
     */
    private fun applyFilters(
        signal: TradingSignal,
        currentPosition: Position?,
        patternMatch: com.fmps.autotrader.core.patterns.models.MatchedPattern?
    ): TradingSignal {
        // Check if signal conflicts with current position
        if (currentPosition != null) {
            when {
                // Already long, can't buy again
                signal.action == SignalAction.BUY && currentPosition.action == com.fmps.autotrader.shared.enums.TradeAction.LONG -> {
                    return signal.copy(
                        action = SignalAction.HOLD,
                        reason = "Already have long position. ${signal.reason}"
                    )
                }
                // Already short, can't sell again
                signal.action == SignalAction.SELL && currentPosition.action == com.fmps.autotrader.shared.enums.TradeAction.SHORT -> {
                    return signal.copy(
                        action = SignalAction.HOLD,
                        reason = "Already have short position. ${signal.reason}"
                    )
                }
                // Close signal is always valid if there's a position
                signal.action == SignalAction.CLOSE -> {
                    return signal // Close signal is valid
                }
            }
        }

        // Check confidence threshold
        if (!signal.meetsConfidenceThreshold(minConfidenceThreshold)) {
            return signal.copy(
                action = SignalAction.HOLD,
                reason = "Signal confidence (${signal.confidence}) below threshold ($minConfidenceThreshold). ${signal.reason}"
            )
        }

        return signal
    }

    /**
     * Calculate final confidence score, combining strategy and pattern confidence.
     */
    private fun calculateFinalConfidence(
        strategySignal: TradingSignal,
        filteredSignal: TradingSignal,
        currentPosition: Position?,
        patternMatch: com.fmps.autotrader.core.patterns.models.MatchedPattern?
    ): Double {
        var confidence = strategySignal.confidence

        // If filter changed action to HOLD, reduce confidence
        if (filteredSignal.action != strategySignal.action) {
            confidence *= 0.5
        }

        // Combine with pattern confidence if pattern matched
        if (patternMatch != null) {
            val patternConfidence = patternMatch.getFinalConfidence()
            // Weighted combination: (1 - patternWeight) * strategy + patternWeight * pattern
            confidence = (1.0 - patternWeight) * confidence + patternWeight * patternConfidence
            
            logger.debug {
                "Combined confidence: strategy=${strategySignal.confidence}, " +
                        "pattern=${patternConfidence}, final=${confidence}"
            }
        }

        // Adjust based on position state
        currentPosition?.let { position ->
            when {
                // Closing a profitable position adds confidence
                filteredSignal.action == SignalAction.CLOSE && position.unrealizedPnL > java.math.BigDecimal.ZERO -> {
                    confidence += 0.1
                }
                // Closing a losing position reduces confidence
                filteredSignal.action == SignalAction.CLOSE && position.unrealizedPnL < java.math.BigDecimal.ZERO -> {
                    confidence -= 0.1
                }
            }
        }

        return confidence.coerceIn(0.0, 1.0)
    }

    /**
     * Build final reason string.
     */
    private fun buildFinalReason(
        strategySignal: TradingSignal,
        filteredSignal: TradingSignal,
        currentPosition: Position?,
        patternMatch: com.fmps.autotrader.core.patterns.models.MatchedPattern?
    ): String {
        val parts = mutableListOf<String>()

        if (filteredSignal.action != strategySignal.action) {
            parts.add("Filtered: ${strategySignal.action} -> ${filteredSignal.action}")
        }

        parts.add(filteredSignal.reason)

        // Add pattern match information
        patternMatch?.let { match ->
            parts.add(
                "Pattern matched: ${match.pattern.name ?: match.pattern.id} " +
                        "(relevance: ${String.format("%.2f", match.relevanceScore)}, " +
                        "confidence: ${String.format("%.2f", match.confidence)})"
            )
        }

        currentPosition?.let {
            parts.add("Current position: ${it.action} at ${it.entryPrice}, P&L: ${it.unrealizedPnL}")
        }

        return parts.joinToString(". ")
    }
}

