package com.fmps.autotrader.core.traders.strategies

import com.fmps.autotrader.core.indicators.BollingerBandsIndicator
import com.fmps.autotrader.core.indicators.MACDIndicator
import com.fmps.autotrader.core.traders.AITraderConfig
import com.fmps.autotrader.core.traders.SignalAction
import com.fmps.autotrader.core.traders.TradingSignal
import com.fmps.autotrader.shared.model.Candlestick
import mu.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Breakout Strategy
 *
 * This strategy trades on price breakouts from consolidation patterns.
 * It uses Bollinger Bands for volatility detection and MACD for momentum confirmation.
 *
 * Strategy Logic:
 * - BUY signal: Price breaks above upper band with momentum
 * - SELL signal: Price breaks below lower band with momentum
 * - Uses MACD for momentum confirmation
 * - Avoids false breakouts (squeeze detection)
 *
 * Configuration Parameters:
 * - bbPeriod: Bollinger Bands period (default: 20)
 * - bbStdDev: Bollinger Bands standard deviation multiplier (default: 2.0)
 * - breakoutThreshold: Percentage above/below band to confirm breakout (default: 1.05 = 5%)
 * - momentumPeriod: Period for momentum calculation (default: 14)
 *
 * @since 1.0.0
 */
class BreakoutStrategy(
    private val config: AITraderConfig
) : ITradingStrategy {

    // Configuration parameters
    private val bbPeriod: Int = 20
    private val bbStdDev: Double = 2.0
    private val breakoutThreshold: Double = 1.05 // 5% above/below band
    private val momentumPeriod: Int = 14

    // Indicator instances
    private val bbIndicator = BollingerBandsIndicator(bbPeriod, bbStdDev)
    private val macdIndicator = MACDIndicator()

    // Internal state for tracking previous band values
    private var previousBBResult: com.fmps.autotrader.core.indicators.models.BollingerBandsResult? = null

    override fun generateSignal(
        candles: List<Candlestick>,
        indicators: Map<String, Any>
    ): TradingSignal {
        // Check minimum data requirements
        val minDataPoints = maxOf(bbPeriod, 26) // MACD needs 26 periods
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
            val currentPrice = candles.last().close.toDouble()

            // Calculate indicators
            val bbResult = bbIndicator.calculate(candles)
            val macdResult = macdIndicator.calculate(candles)

            // Check if indicators are available
            if (bbResult == null) {
                return TradingSignal(
                    action = SignalAction.HOLD,
                    confidence = 0.0,
                    reason = "Unable to calculate Bollinger Bands",
                    timestamp = Instant.now(),
                    indicatorValues = emptyMap()
                )
            }

            // Check for squeeze (low volatility - no breakout signal)
            if (bbResult.isSqueeze()) {
                return TradingSignal(
                    action = SignalAction.HOLD,
                    confidence = 0.2,
                    reason = "Bollinger Bands squeeze detected (low volatility). Waiting for breakout.",
                    timestamp = Instant.now(),
                    indicatorValues = buildIndicatorMap(bbResult, macdResult, currentPrice)
                )
            }

            // Build indicator values map
            val indicatorMap = buildIndicatorMap(bbResult, macdResult, currentPrice)

            // Check for upper breakout (BUY signal)
            val upperBreakout = detectUpperBreakout(bbResult, currentPrice, macdResult)
            if (upperBreakout != null) {
                return TradingSignal(
                    action = SignalAction.BUY,
                    confidence = upperBreakout.confidence,
                    reason = upperBreakout.reason,
                    timestamp = Instant.now(),
                    indicatorValues = indicatorMap
                )
            }

            // Check for lower breakout (SELL signal)
            val lowerBreakout = detectLowerBreakout(bbResult, currentPrice, macdResult)
            if (lowerBreakout != null) {
                return TradingSignal(
                    action = SignalAction.SELL,
                    confidence = lowerBreakout.confidence,
                    reason = lowerBreakout.reason,
                    timestamp = Instant.now(),
                    indicatorValues = indicatorMap
                )
            }

            // Check for false breakout (price broke out but came back)
            val falseBreakout = detectFalseBreakout(bbResult, currentPrice)
            if (falseBreakout) {
                return TradingSignal(
                    action = SignalAction.HOLD,
                    confidence = 0.3,
                    reason = "False breakout detected. Price returned within bands.",
                    timestamp = Instant.now(),
                    indicatorValues = indicatorMap
                )
            }

            // No breakout detected
            return TradingSignal(
                action = SignalAction.HOLD,
                confidence = 0.3,
                reason = "No breakout detected. Price within Bollinger Bands.",
                timestamp = Instant.now(),
                indicatorValues = indicatorMap
            )
        } catch (e: Exception) {
            logger.error(e) { "Error generating breakout signal" }
            return TradingSignal(
                action = SignalAction.HOLD,
                confidence = 0.0,
                reason = "Error in signal generation: ${e.message}",
                timestamp = Instant.now(),
                indicatorValues = emptyMap()
            )
        } finally {
            // Update previous values for next iteration
            val currentBBResult = bbIndicator.calculate(candles)
            previousBBResult = currentBBResult
        }
    }

    /**
     * Detect upper breakout (price breaks above upper band).
     */
    private fun detectUpperBreakout(
        bbResult: com.fmps.autotrader.core.indicators.models.BollingerBandsResult,
        currentPrice: Double,
        macdResult: com.fmps.autotrader.core.indicators.models.MACDResult?
    ): BreakoutSignal? {
        // Check if price is above upper band with threshold
        val thresholdPrice = bbResult.upper * breakoutThreshold
        if (currentPrice <= thresholdPrice) {
            return null // Not a breakout
        }

        // Check MACD momentum confirmation
        val macdConfirmation = macdResult?.isBullish() ?: false
        val macdMomentum = macdResult?.hasPositiveMomentum() ?: false

        // Calculate confidence
        var confidence = 0.6 // Base confidence for breakout
        if (macdConfirmation) confidence += 0.2
        if (macdMomentum) confidence += 0.1

        // Strength of breakout (how far above band)
        val breakoutStrength = (currentPrice - bbResult.upper) / bbResult.upper
        if (breakoutStrength > 0.05) { // More than 5% above band
            confidence += 0.1
        }

        val reason = buildBreakoutReason(
            "Upper breakout",
            currentPrice,
            bbResult.upper,
            macdConfirmation,
            macdMomentum
        )

        return BreakoutSignal(confidence.coerceIn(0.0, 1.0), reason)
    }

    /**
     * Detect lower breakout (price breaks below lower band).
     */
    private fun detectLowerBreakout(
        bbResult: com.fmps.autotrader.core.indicators.models.BollingerBandsResult,
        currentPrice: Double,
        macdResult: com.fmps.autotrader.core.indicators.models.MACDResult?
    ): BreakoutSignal? {
        // Check if price is below lower band with threshold
        val thresholdPrice = bbResult.lower / breakoutThreshold
        if (currentPrice >= thresholdPrice) {
            return null // Not a breakout
        }

        // Check MACD momentum confirmation
        val macdConfirmation = macdResult?.isBearish() ?: false
        val macdMomentum = macdResult?.hasNegativeMomentum() ?: false

        // Calculate confidence
        var confidence = 0.6 // Base confidence for breakout
        if (macdConfirmation) confidence += 0.2
        if (macdMomentum) confidence += 0.1

        // Strength of breakout (how far below band)
        val breakoutStrength = (bbResult.lower - currentPrice) / bbResult.lower
        if (breakoutStrength > 0.05) { // More than 5% below band
            confidence += 0.1
        }

        val reason = buildBreakoutReason(
            "Lower breakout",
            currentPrice,
            bbResult.lower,
            macdConfirmation,
            macdMomentum
        )

        return BreakoutSignal(confidence.coerceIn(0.0, 1.0), reason)
    }

    /**
     * Detect false breakout (price broke out but came back).
     */
    private fun detectFalseBreakout(
        bbResult: com.fmps.autotrader.core.indicators.models.BollingerBandsResult,
        currentPrice: Double
    ): Boolean {
        // Check if previous price was outside bands but current is within
        return previousBBResult?.let { prev ->
            (prev.isAboveUpperBand(currentPrice) && bbResult.middle >= currentPrice) ||
            (prev.isBelowLowerBand(currentPrice) && bbResult.middle <= currentPrice)
        } ?: false
    }

    /**
     * Build breakout signal reason.
     */
    private fun buildBreakoutReason(
        type: String,
        currentPrice: Double,
        bandPrice: Double,
        macdConfirmation: Boolean,
        macdMomentum: Boolean
    ): String {
        val parts = mutableListOf<String>()
        parts.add("$type detected")
        parts.add("Price: ${"%.2f".format(currentPrice)}, Band: ${"%.2f".format(bandPrice)}")
        parts.add("MACD: ${if (macdConfirmation) "confirmed" else "not confirmed"}")
        if (macdMomentum) {
            parts.add("Positive momentum")
        }
        return parts.joinToString(", ")
    }

    /**
     * Build indicator values map.
     */
    private fun buildIndicatorMap(
        bbResult: com.fmps.autotrader.core.indicators.models.BollingerBandsResult,
        macdResult: com.fmps.autotrader.core.indicators.models.MACDResult?,
        currentPrice: Double
    ): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "bbUpper" to bbResult.upper,
            "bbMiddle" to bbResult.middle,
            "bbLower" to bbResult.lower,
            "bbBandwidth" to bbResult.bandwidth,
            "currentPrice" to currentPrice
        )
        macdResult?.let {
            map["macd"] = it.macd
            map["macdSignal"] = it.signal
            map["macdHistogram"] = it.histogram
        }
        return map
    }

    /**
     * Internal data class for breakout signal details.
     */
    private data class BreakoutSignal(
        val confidence: Double,
        val reason: String
    )

    override fun getName(): String = "Breakout"

    override fun getDescription(): String =
        "Trades on price breakouts from consolidation patterns. " +
                "Generates BUY signals when price breaks above upper Bollinger Band with momentum " +
                "and SELL signals when price breaks below lower Bollinger Band with momentum. " +
                "Uses MACD for momentum confirmation and avoids false breakouts."

    override fun getRequiredIndicators(): List<String> =
        listOf("BollingerBands", "MACD")

    override fun validateConfig(config: AITraderConfig): Boolean {
        // Breakout strategy works with any valid config
        return true
    }

    override fun reset() {
        bbIndicator.reset()
        macdIndicator.reset()
        previousBBResult = null
        logger.debug { "BreakoutStrategy reset for trader ${config.id}" }
    }
}

