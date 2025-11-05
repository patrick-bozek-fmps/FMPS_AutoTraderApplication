package com.fmps.autotrader.core.traders.strategies

import com.fmps.autotrader.core.indicators.MACDIndicator
import com.fmps.autotrader.core.indicators.RSIIndicator
import com.fmps.autotrader.core.indicators.SMAIndicator
import com.fmps.autotrader.core.traders.AITraderConfig
import com.fmps.autotrader.core.traders.SignalAction
import com.fmps.autotrader.core.traders.TradingSignal
import com.fmps.autotrader.shared.model.Candlestick
import mu.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Trend Following Strategy
 *
 * This strategy follows market trends using moving average crossovers.
 * It generates signals based on:
 * - Golden Cross: Short-term SMA crosses above long-term SMA (BUY signal)
 * - Death Cross: Short-term SMA crosses below long-term SMA (SELL signal)
 * - MACD confirmation for trend strength
 * - RSI filter to avoid overbought/oversold entries
 *
 * Configuration Parameters:
 * - smaShortPeriod: Short-term SMA period (default: 9)
 * - smaLongPeriod: Long-term SMA period (default: 21)
 * - rsiPeriod: RSI period (default: 14)
 * - rsiOverbought: RSI overbought threshold (default: 70.0)
 * - rsiOversold: RSI oversold threshold (default: 30.0)
 *
 * @since 1.0.0
 */
class TrendFollowingStrategy(
    private val config: AITraderConfig
) : ITradingStrategy {

    // Configuration parameters
    private val smaShortPeriod: Int = 9
    private val smaLongPeriod: Int = 21
    private val rsiPeriod: Int = 14
    private val rsiOverbought: Double = 70.0
    private val rsiOversold: Double = 30.0

    // Indicator instances
    private val smaShortIndicator = SMAIndicator(smaShortPeriod)
    private val smaLongIndicator = SMAIndicator(smaLongPeriod)
    private val rsiIndicator = RSIIndicator(rsiPeriod)
    private val macdIndicator = MACDIndicator()

    // Internal state for tracking previous SMA values
    private var previousSMAShort: Double? = null
    private var previousSMALong: Double? = null

    override fun generateSignal(
        candles: List<Candlestick>,
        indicators: Map<String, Any>
    ): TradingSignal {
        // Check minimum data requirements
        val minDataPoints = maxOf(smaLongPeriod, rsiPeriod + 1, 26) // MACD needs 26 periods
        if (candles.size < minDataPoints) {
            return TradingSignal(
                action = SignalAction.HOLD,
                confidence = 0.0,
                reason = "Insufficient data: need $minDataPoints candles, got ${candles.size}",
                timestamp = Instant.now(),
                indicatorValues = emptyMap()
            )
        }

        try {
            // Calculate indicators
            val smaShort = smaShortIndicator.calculate(candles)
            val smaLong = smaLongIndicator.calculate(candles)
            val rsi = rsiIndicator.calculate(candles)
            val macdResult = macdIndicator.calculate(candles)

            // Check if indicators are available
            if (smaShort == null || smaLong == null) {
                return TradingSignal(
                    action = SignalAction.HOLD,
                    confidence = 0.0,
                    reason = "Unable to calculate SMA indicators",
                    timestamp = Instant.now(),
                    indicatorValues = mapOf(
                        "smaShort" to (smaShort ?: "null"),
                        "smaLong" to (smaLong ?: "null")
                    )
                )
            }

            // Detect Golden Cross (BUY signal)
            val goldenCross = detectGoldenCross(smaShort, smaLong)
            // Detect Death Cross (SELL signal)
            val deathCross = detectDeathCross(smaShort, smaLong)

            // Build indicator values map
            val indicatorMap = mutableMapOf<String, Any>(
                "smaShort" to smaShort,
                "smaLong" to smaLong
            ).apply {
                rsi?.let { put("rsi", it) }
                macdResult?.let {
                    put("macd", it.macd)
                    put("macdSignal", it.signal)
                    put("macdHistogram", it.histogram)
                }
            }

            // Generate signal based on crossover
            return when {
                goldenCross -> {
                    // Check RSI filter (avoid overbought)
                    val rsiFilterPass = rsi == null || rsi < rsiOverbought
                    // Check MACD confirmation
                    val macdConfirmation = macdResult?.isBullish() ?: true

                    val confidence = calculateConfidence(
                        rsiFilterPass,
                        macdConfirmation,
                        true,
                        rsi,
                        macdResult
                    )

                    TradingSignal(
                        action = SignalAction.BUY,
                        confidence = confidence,
                        reason = buildSignalReason(
                            "Golden Cross",
                            rsiFilterPass,
                            macdConfirmation,
                            rsi,
                            macdResult
                        ),
                        timestamp = Instant.now(),
                        indicatorValues = indicatorMap
                    )
                }
                deathCross -> {
                    // Check RSI filter (avoid oversold)
                    val rsiFilterPass = rsi == null || rsi > rsiOversold
                    // Check MACD confirmation
                    val macdConfirmation = macdResult?.isBearish() ?: true

                    val confidence = calculateConfidence(
                        rsiFilterPass,
                        macdConfirmation,
                        false,
                        rsi,
                        macdResult
                    )

                    TradingSignal(
                        action = SignalAction.SELL,
                        confidence = confidence,
                        reason = buildSignalReason(
                            "Death Cross",
                            rsiFilterPass,
                            macdConfirmation,
                            rsi,
                            macdResult
                        ),
                        timestamp = Instant.now(),
                        indicatorValues = indicatorMap
                    )
                }
                else -> {
                    // No crossover - hold
                    TradingSignal(
                        action = SignalAction.HOLD,
                        confidence = 0.3,
                        reason = "No trend crossover detected. SMA Short: $smaShort, SMA Long: $smaLong",
                        timestamp = Instant.now(),
                        indicatorValues = indicatorMap
                    )
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error generating trend following signal" }
            return TradingSignal(
                action = SignalAction.HOLD,
                confidence = 0.0,
                reason = "Error in signal generation: ${e.message}",
                timestamp = Instant.now(),
                indicatorValues = emptyMap()
            )
        } finally {
            // Update previous values for next iteration
            val currentSMAShort = smaShortIndicator.calculate(candles)
            val currentSMALong = smaLongIndicator.calculate(candles)
            previousSMAShort = currentSMAShort
            previousSMALong = currentSMALong
        }
    }

    /**
     * Detect Golden Cross (short SMA crosses above long SMA).
     */
    private fun detectGoldenCross(smaShort: Double, smaLong: Double): Boolean {
        return if (previousSMAShort != null && previousSMALong != null) {
            // Previous: short <= long, Current: short > long
            previousSMAShort!! <= previousSMALong!! && smaShort > smaLong
        } else {
            // First time - check if short is already above long
            smaShort > smaLong
        }
    }

    /**
     * Detect Death Cross (short SMA crosses below long SMA).
     */
    private fun detectDeathCross(smaShort: Double, smaLong: Double): Boolean {
        return if (previousSMAShort != null && previousSMALong != null) {
            // Previous: short >= long, Current: short < long
            previousSMAShort!! >= previousSMALong!! && smaShort < smaLong
        } else {
            // First time - check if short is already below long
            smaShort < smaLong
        }
    }

    /**
     * Calculate confidence based on indicator alignment.
     */
    private fun calculateConfidence(
        rsiFilterPass: Boolean,
        macdConfirmation: Boolean,
        isBullish: Boolean,
        rsi: Double?,
        macdResult: com.fmps.autotrader.core.indicators.models.MACDResult?
    ): Double {
        var confidence = 0.6 // Base confidence for crossover

        // RSI filter adds confidence
        if (rsiFilterPass) {
            confidence += 0.15
        } else {
            confidence -= 0.2 // RSI filter failed
        }

        // MACD confirmation adds confidence
        if (macdConfirmation) {
            confidence += 0.15
        } else {
            confidence -= 0.1 // MACD not confirming
        }

        // RSI extreme values (but not overbought/oversold) add confidence
        rsi?.let {
            when {
                isBullish && it in 30.0..50.0 -> confidence += 0.1 // Not oversold, room to grow
                !isBullish && it in 50.0..70.0 -> confidence += 0.1 // Not overbought, room to fall
            }
        }

        // MACD histogram strength
        macdResult?.let {
            if (it.hasPositiveMomentum() && isBullish) {
                confidence += 0.05
            } else if (it.hasNegativeMomentum() && !isBullish) {
                confidence += 0.05
            }
        }

        return confidence.coerceIn(0.0, 1.0)
    }

    /**
     * Build human-readable signal reason.
     */
    private fun buildSignalReason(
        crossoverType: String,
        rsiFilterPass: Boolean,
        macdConfirmation: Boolean,
        rsi: Double?,
        macdResult: com.fmps.autotrader.core.indicators.models.MACDResult?
    ): String {
        val parts = mutableListOf<String>()
        parts.add("$crossoverType detected")

        rsi?.let {
            parts.add("RSI: ${"%.2f".format(it)}")
            if (!rsiFilterPass) {
                parts.add("RSI filter: failed")
            }
        }

        macdResult?.let {
            parts.add("MACD: ${if (macdConfirmation) "confirmed" else "not confirmed"}")
        }

        return parts.joinToString(", ")
    }

    override fun getName(): String = "Trend Following"

    override fun getDescription(): String =
        "Follows market trends using moving average crossovers. " +
                "Generates BUY signals on golden cross (short SMA > long SMA) " +
                "and SELL signals on death cross (short SMA < long SMA). " +
                "Uses MACD for confirmation and RSI to filter overbought/oversold conditions."

    override fun getRequiredIndicators(): List<String> =
        listOf("SMA", "EMA", "MACD", "RSI")

    override fun validateConfig(config: AITraderConfig): Boolean {
        // Trend following strategy works with any valid config
        return true
    }

    override fun reset() {
        previousSMAShort = null
        previousSMALong = null
        logger.debug { "TrendFollowingStrategy reset for trader ${config.id}" }
    }
}

