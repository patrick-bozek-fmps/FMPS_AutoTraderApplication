package com.fmps.autotrader.core.traders

import com.fmps.autotrader.core.indicators.BollingerBandsIndicator
import com.fmps.autotrader.core.indicators.EMAIndicator
import com.fmps.autotrader.core.indicators.MACDIndicator
import com.fmps.autotrader.core.indicators.RSIIndicator
import com.fmps.autotrader.core.indicators.SMAIndicator
import com.fmps.autotrader.core.traders.strategies.ITradingStrategy
import com.fmps.autotrader.shared.model.Candlestick
import mu.KotlinLogging
import java.math.BigDecimal
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Processes market data and calculates required indicators.
 *
 * This class handles:
 * - Calculating all indicators required by the active strategy
 * - Caching indicator values to avoid redundant calculations
 * - Validating data quality and completeness
 * - Handling missing/incomplete data gracefully
 *
 * @since 1.0.0
 */
class MarketDataProcessor(
    private val strategy: ITradingStrategy
) {
    // Indicator cache to avoid redundant calculations
    private val indicatorCache = mutableMapOf<String, Any?>()

    /**
     * Process candlestick data and calculate indicators.
     *
     * @param candles List of candlesticks in chronological order
     * @return Processed market data with indicators, or null if processing fails
     */
    fun processCandlesticks(candles: List<Candlestick>): ProcessedMarketData? {
        if (candles.isEmpty()) {
            logger.warn { "Cannot process empty candlestick list" }
            return null
        }

        try {
            // Validate data quality
            if (!validateData(candles)) {
                logger.warn { "Data validation failed for ${candles.size} candles" }
                return null
            }

            // Get required indicators from strategy
            val requiredIndicators = strategy.getRequiredIndicators()

            // Calculate indicators
            val indicators = calculateIndicators(candles, requiredIndicators)

            // Get latest price
            val latestPrice = candles.last().close

            // Create processed market data
            return ProcessedMarketData(
                candles = candles,
                indicators = indicators.filterValues { it != null }.mapValues { it.value!! },
                latestPrice = latestPrice,
                timestamp = Instant.now()
            )
        } catch (e: Exception) {
            logger.error(e) { "Error processing market data" }
            return null
        }
    }

    /**
     * Calculate indicators based on required list.
     */
    private fun calculateIndicators(
        candles: List<Candlestick>,
        requiredIndicators: List<String>
    ): Map<String, Any?> {
        val calculated = mutableMapOf<String, Any?>()

        for (indicatorName in requiredIndicators) {
            try {
                val value = when (indicatorName.uppercase()) {
                    "SMA" -> {
                        // Calculate SMA for common periods
                        val sma9 = SMAIndicator(9).calculate(candles)
                        val sma21 = SMAIndicator(21).calculate(candles)
                        mapOf("SMA9" to sma9, "SMA21" to sma21)
                    }
                    "EMA" -> {
                        // Calculate EMA for common periods
                        val ema12 = EMAIndicator(12).calculate(candles)
                        val ema26 = EMAIndicator(26).calculate(candles)
                        mapOf("EMA12" to ema12, "EMA26" to ema26)
                    }
                    "RSI" -> RSIIndicator().calculate(candles)
                    "MACD" -> MACDIndicator().calculate(candles)
                    "BOLLINGERBANDS", "BB" -> BollingerBandsIndicator().calculate(candles)
                    else -> {
                        logger.warn { "Unknown indicator: $indicatorName" }
                        null
                    }
                }
                calculated[indicatorName] = value
            } catch (e: Exception) {
                logger.error(e) { "Error calculating indicator: $indicatorName" }
                calculated[indicatorName] = null
            }
        }

        return calculated
    }

    /**
     * Validate data quality.
     */
    private fun validateData(candles: List<Candlestick>): Boolean {
        // Check minimum data points
        val minDataPoints = strategy.getRequiredIndicators().size
        if (candles.size < minDataPoints) {
            logger.warn { "Insufficient data points: need at least $minDataPoints, got ${candles.size}" }
            return false
        }

        // Check for data gaps (timestamps should be sequential)
        for (i in 1 until candles.size) {
            val prev = candles[i - 1].closeTime
            val curr = candles[i].openTime
            if (curr.isBefore(prev)) {
                logger.warn { "Data gap detected: candle $i has openTime before previous closeTime" }
                return false
            }
        }

        // Check for outliers (extreme price changes)
        for (i in 1 until candles.size) {
            val prevClose = candles[i - 1].close.toDouble()
            val currOpen = candles[i].open.toDouble()
            val changePercent = kotlin.math.abs((currOpen - prevClose) / prevClose)
            if (changePercent > 0.5) { // More than 50% change
                logger.warn { "Outlier detected: ${changePercent * 100}% price change at candle $i" }
                // Don't fail, just log - might be legitimate (e.g., news event)
            }
        }

        return true
    }

    /**
     * Clear indicator cache.
     */
    fun clearCache() {
        indicatorCache.clear()
        logger.debug { "Indicator cache cleared" }
    }
}

