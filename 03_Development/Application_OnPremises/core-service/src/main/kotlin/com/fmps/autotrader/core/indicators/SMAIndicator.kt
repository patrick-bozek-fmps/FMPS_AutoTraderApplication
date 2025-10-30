package com.fmps.autotrader.core.indicators

import com.fmps.autotrader.shared.model.Candlestick
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Simple Moving Average (SMA) Indicator
 *
 * SMA is the average of closing prices over a specified period.
 * It smooths out price data to help identify trends.
 *
 * **Formula**: SMA = (Sum of closing prices over N periods) / N
 *
 * **Usage**:
 * - Trend identification: Price above SMA = uptrend, below = downtrend
 * - Support/Resistance: SMA often acts as dynamic support or resistance
 * - Golden Cross: Short-term SMA crossing above long-term SMA = bullish signal
 * - Death Cross: Short-term SMA crossing below long-term SMA = bearish signal
 *
 * @property period Number of periods for the moving average (default: 20)
 */
class SMAIndicator(
    private val period: Int = 20
) : ITechnicalIndicator<Double> {
    
    init {
        require(period > 0) { "Period must be positive, got: $period" }
    }
    
    override fun calculate(data: List<Candlestick>): Double? {
        if (!validateData(data)) {
            logger.warn { "Insufficient data for SMA($period): need $period, got ${data.size}" }
            return null
        }
        
        try {
            // Take the last 'period' candles and calculate average of close prices
            val closes = data.takeLast(period).map { it.close.toDouble() }
            return closes.average()
        } catch (e: Exception) {
            logger.error(e) { "Error calculating SMA($period)" }
            throw IndicatorException.calculationError("SMA", e)
        }
    }
    
    /**
     * Calculate SMA for all possible windows in the data series
     *
     * @param data List of candlesticks
     * @return List of SMA values (null for periods with insufficient data)
     */
    fun calculateAll(data: List<Candlestick>): List<Double?> {
        val result = mutableListOf<Double?>()
        
        for (i in data.indices) {
            if (i < period - 1) {
                result.add(null)  // Insufficient data
            } else {
                val window = data.subList(i - period + 1, i + 1)
                result.add(calculate(window))
            }
        }
        
        return result
    }
    
    override fun getName(): String = "SMA"
    
    override fun getRequiredDataPoints(): Int = period
    
    override fun reset() {
        // SMA is stateless, nothing to reset
    }
    
    override fun toString(): String = "SMA(period=$period)"
}

