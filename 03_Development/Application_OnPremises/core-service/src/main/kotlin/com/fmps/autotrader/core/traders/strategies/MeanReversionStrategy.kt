package com.fmps.autotrader.core.traders.strategies

import com.fmps.autotrader.core.indicators.BollingerBandsIndicator
import com.fmps.autotrader.core.indicators.RSIIndicator
import com.fmps.autotrader.core.traders.AITraderConfig
import com.fmps.autotrader.core.traders.SignalAction
import com.fmps.autotrader.core.traders.TradingSignal
import com.fmps.autotrader.shared.model.Candlestick
import mu.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Mean Reversion Strategy
 *
 * This strategy trades against the trend, expecting price to revert to its mean.
 * It uses Bollinger Bands to identify overbought/oversold conditions and RSI for confirmation.
 *
 * Strategy Logic:
 * - BUY signal: Price touches lower band (oversold) + RSI < 30
 * - SELL signal: Price touches upper band (overbought) + RSI > 70
 * - Uses %B indicator for position in bands
 * - Confidence based on how far price is from mean
 *
 * Configuration Parameters:
 * - bbPeriod: Bollinger Bands period (default: 20)
 * - bbStdDev: Bollinger Bands standard deviation multiplier (default: 2.0)
 * - rsiPeriod: RSI period (default: 14)
 * - rsiOversold: RSI oversold threshold (default: 30.0)
 * - rsiOverbought: RSI overbought threshold (default: 70.0)
 *
 * @since 1.0.0
 */
class MeanReversionStrategy(
    private val config: AITraderConfig
) : ITradingStrategy {

    // Configuration parameters
    private val bbPeriod: Int = 20
    private val bbStdDev: Double = 2.0
    private val rsiPeriod: Int = 14
    private val rsiOversold: Double = 30.0
    private val rsiOverbought: Double = 70.0

    // Indicator instances
    private val bbIndicator = BollingerBandsIndicator(bbPeriod, bbStdDev)
    private val rsiIndicator = RSIIndicator(rsiPeriod)

    override fun generateSignal(
        candles: List<Candlestick>,
        indicators: Map<String, Any>
    ): TradingSignal {
        // Check minimum data requirements
        val minDataPoints = maxOf(bbPeriod, rsiPeriod + 1)
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
            val rsi = rsiIndicator.calculate(candles)

            // Check if indicators are available
            if (bbResult == null) {
                return TradingSignal(
                    action = SignalAction.HOLD,
                    confidence = 0.0,
                    reason = "Unable to calculate Bollinger Bands",
                    timestamp = Instant.now(),
                    indicatorValues = mapOf(
                        "rsi" to (rsi ?: "null")
                    )
                )
            }

            // Check for squeeze (low volatility - no signal)
            if (bbResult.isSqueeze()) {
                return TradingSignal(
                    action = SignalAction.HOLD,
                    confidence = 0.2,
                    reason = "Bollinger Bands squeeze detected (low volatility). Waiting for expansion.",
                    timestamp = Instant.now(),
                    indicatorValues = buildIndicatorMap(bbResult, rsi, currentPrice)
                )
            }

            // Calculate %B
            val percentB = bbResult.calculatePercentB(currentPrice)

            // Build indicator values map
            val indicatorMap = buildIndicatorMap(bbResult, rsi, currentPrice)

            // Generate signals based on band touches
            return when {
                // BUY signal: Price touches lower band + RSI oversold
                bbResult.isTouchingLowerBand(currentPrice) && (rsi == null || rsi < rsiOversold) -> {
                    val confidence = calculateBuyConfidence(bbResult, rsi, currentPrice, percentB)
                    TradingSignal(
                        action = SignalAction.BUY,
                        confidence = confidence,
                        reason = buildBuyReason(bbResult, rsi, percentB),
                        timestamp = Instant.now(),
                        indicatorValues = indicatorMap
                    )
                }
                // SELL signal: Price touches upper band + RSI overbought
                bbResult.isTouchingUpperBand(currentPrice) && (rsi == null || rsi > rsiOverbought) -> {
                    val confidence = calculateSellConfidence(bbResult, rsi, currentPrice, percentB)
                    TradingSignal(
                        action = SignalAction.SELL,
                        confidence = confidence,
                        reason = buildSellReason(bbResult, rsi, percentB),
                        timestamp = Instant.now(),
                        indicatorValues = indicatorMap
                    )
                }
                // Price below lower band but RSI not oversold yet
                bbResult.isBelowLowerBand(currentPrice) && (rsi == null || rsi >= rsiOversold) -> {
                    TradingSignal(
                        action = SignalAction.HOLD,
                        confidence = 0.4,
                        reason = "Price below lower band but RSI not oversold (RSI: ${rsi?.let { "%.2f".format(it) } ?: "N/A"})",
                        timestamp = Instant.now(),
                        indicatorValues = indicatorMap
                    )
                }
                // Price above upper band but RSI not overbought yet
                bbResult.isAboveUpperBand(currentPrice) && (rsi == null || rsi <= rsiOverbought) -> {
                    TradingSignal(
                        action = SignalAction.HOLD,
                        confidence = 0.4,
                        reason = "Price above upper band but RSI not overbought (RSI: ${rsi?.let { "%.2f".format(it) } ?: "N/A"})",
                        timestamp = Instant.now(),
                        indicatorValues = indicatorMap
                    )
                }
                else -> {
                    // Price within bands or conditions not met
                    TradingSignal(
                        action = SignalAction.HOLD,
                        confidence = 0.3,
                        reason = "Price within Bollinger Bands. %B: ${"%.2f".format(percentB)}, RSI: ${rsi?.let { "%.2f".format(it) } ?: "N/A"}",
                        timestamp = Instant.now(),
                        indicatorValues = indicatorMap
                    )
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error generating mean reversion signal" }
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
     * Build indicator values map.
     */
    private fun buildIndicatorMap(
        bbResult: com.fmps.autotrader.core.indicators.models.BollingerBandsResult,
        rsi: Double?,
        currentPrice: Double
    ): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "bbUpper" to bbResult.upper,
            "bbMiddle" to bbResult.middle,
            "bbLower" to bbResult.lower,
            "bbBandwidth" to bbResult.bandwidth,
            "percentB" to bbResult.calculatePercentB(currentPrice),
            "currentPrice" to currentPrice
        )
        rsi?.let { map["rsi"] = it }
        return map
    }

    /**
     * Calculate confidence for BUY signal.
     */
    private fun calculateBuyConfidence(
        bbResult: com.fmps.autotrader.core.indicators.models.BollingerBandsResult,
        rsi: Double?,
        currentPrice: Double,
        percentB: Double
    ): Double {
        var confidence = 0.6 // Base confidence for lower band touch

        // RSI confirmation adds confidence
        rsi?.let {
            when {
                it < 25.0 -> confidence += 0.2 // Very oversold
                it < 30.0 -> confidence += 0.15 // Oversold
                else -> confidence -= 0.1 // RSI not confirming
            }
        }

        // %B adds confidence (closer to 0 is better for buy)
        when {
            percentB < 0.0 -> confidence += 0.1 // Price below lower band
            percentB < 0.1 -> confidence += 0.05 // Very close to lower band
            else -> confidence -= 0.05
        }

        // Distance from mean adds confidence
        val distanceFromMean = (bbResult.middle - currentPrice) / bbResult.middle
        if (distanceFromMean > 0.02) { // More than 2% below mean
            confidence += 0.05
        }

        return confidence.coerceIn(0.0, 1.0)
    }

    /**
     * Calculate confidence for SELL signal.
     */
    private fun calculateSellConfidence(
        bbResult: com.fmps.autotrader.core.indicators.models.BollingerBandsResult,
        rsi: Double?,
        currentPrice: Double,
        percentB: Double
    ): Double {
        var confidence = 0.6 // Base confidence for upper band touch

        // RSI confirmation adds confidence
        rsi?.let {
            when {
                it > 75.0 -> confidence += 0.2 // Very overbought
                it > 70.0 -> confidence += 0.15 // Overbought
                else -> confidence -= 0.1 // RSI not confirming
            }
        }

        // %B adds confidence (closer to 1 is better for sell)
        when {
            percentB > 1.0 -> confidence += 0.1 // Price above upper band
            percentB > 0.9 -> confidence += 0.05 // Very close to upper band
            else -> confidence -= 0.05
        }

        // Distance from mean adds confidence
        val distanceFromMean = (currentPrice - bbResult.middle) / bbResult.middle
        if (distanceFromMean > 0.02) { // More than 2% above mean
            confidence += 0.05
        }

        return confidence.coerceIn(0.0, 1.0)
    }

    /**
     * Build BUY signal reason.
     */
    private fun buildBuyReason(
        bbResult: com.fmps.autotrader.core.indicators.models.BollingerBandsResult,
        rsi: Double?,
        percentB: Double
    ): String {
        val parts = mutableListOf<String>()
        parts.add("Price touching lower Bollinger Band")
        parts.add("%B: ${"%.2f".format(percentB)}")
        rsi?.let {
            parts.add("RSI: ${"%.2f".format(it)} (oversold)")
        }
        return parts.joinToString(", ")
    }

    /**
     * Build SELL signal reason.
     */
    private fun buildSellReason(
        bbResult: com.fmps.autotrader.core.indicators.models.BollingerBandsResult,
        rsi: Double?,
        percentB: Double
    ): String {
        val parts = mutableListOf<String>()
        parts.add("Price touching upper Bollinger Band")
        parts.add("%B: ${"%.2f".format(percentB)}")
        rsi?.let {
            parts.add("RSI: ${"%.2f".format(it)} (overbought)")
        }
        return parts.joinToString(", ")
    }

    override fun getName(): String = "Mean Reversion"

    override fun getDescription(): String =
        "Trades against the trend, expecting price to revert to its mean. " +
                "Generates BUY signals when price touches lower Bollinger Band (oversold) " +
                "and SELL signals when price touches upper Bollinger Band (overbought). " +
                "Uses RSI for confirmation."

    override fun getRequiredIndicators(): List<String> =
        listOf("BollingerBands", "RSI")

    override fun validateConfig(config: AITraderConfig): Boolean {
        // Mean reversion strategy works with any valid config
        return true
    }

    override fun reset() {
        bbIndicator.reset()
        rsiIndicator.reset()
        logger.debug { "MeanReversionStrategy reset for trader ${config.id}" }
    }
}

