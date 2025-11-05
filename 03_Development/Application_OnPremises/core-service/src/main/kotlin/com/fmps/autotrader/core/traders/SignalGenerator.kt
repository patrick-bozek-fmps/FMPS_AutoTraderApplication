package com.fmps.autotrader.core.traders

import com.fmps.autotrader.core.traders.strategies.ITradingStrategy
import com.fmps.autotrader.shared.model.Position
import mu.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Generates trading signals by combining strategy signals with risk management checks.
 *
 * This class:
 * - Calls the strategy's generateSignal() method
 * - Applies filters (risk limits, position limits)
 * - Calculates final confidence score
 * - Logs signal generation for debugging
 *
 * @since 1.0.0
 */
class SignalGenerator(
    private val strategy: ITradingStrategy,
    private val minConfidenceThreshold: Double = 0.5
) {
    init {
        require(minConfidenceThreshold >= 0.0 && minConfidenceThreshold <= 1.0) {
            "Min confidence threshold must be between 0.0 and 1.0, got: $minConfidenceThreshold"
        }
    }

    /**
     * Generate a trading signal based on processed market data.
     *
     * @param processedData Processed market data with indicators
     * @param currentPosition Current open position, if any
     * @return Trading signal with action, confidence, and reasoning
     */
    fun generateSignal(
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

            // Apply filters
            val filteredSignal = applyFilters(strategySignal, currentPosition)

            // Calculate final confidence
            val finalConfidence = calculateFinalConfidence(
                strategySignal,
                filteredSignal,
                currentPosition
            )

            // Create final signal
            val finalSignal = TradingSignal(
                action = filteredSignal.action,
                confidence = finalConfidence,
                reason = buildFinalReason(strategySignal, filteredSignal, currentPosition),
                timestamp = Instant.now(),
                indicatorValues = strategySignal.indicatorValues
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
     * Apply filters to the strategy signal.
     */
    private fun applyFilters(
        signal: TradingSignal,
        currentPosition: Position?
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
     * Calculate final confidence score.
     */
    private fun calculateFinalConfidence(
        strategySignal: TradingSignal,
        filteredSignal: TradingSignal,
        currentPosition: Position?
    ): Double {
        var confidence = strategySignal.confidence

        // If filter changed action to HOLD, reduce confidence
        if (filteredSignal.action != strategySignal.action) {
            confidence *= 0.5
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
        currentPosition: Position?
    ): String {
        val parts = mutableListOf<String>()

        if (filteredSignal.action != strategySignal.action) {
            parts.add("Filtered: ${strategySignal.action} -> ${filteredSignal.action}")
        }

        parts.add(filteredSignal.reason)

        currentPosition?.let {
            parts.add("Current position: ${it.action} at ${it.entryPrice}, P&L: ${it.unrealizedPnL}")
        }

        return parts.joinToString(". ")
    }
}

