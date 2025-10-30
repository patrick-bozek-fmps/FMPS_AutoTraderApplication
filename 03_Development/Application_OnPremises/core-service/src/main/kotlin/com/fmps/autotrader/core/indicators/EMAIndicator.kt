package com.fmps.autotrader.core.indicators

import com.fmps.autotrader.shared.model.Candlestick
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Exponential Moving Average (EMA) Indicator
 *
 * EMA gives more weight to recent prices, making it more responsive to new information
 * compared to SMA. This makes it better for identifying short-term trends.
 *
 * **Formula**:
 * - Multiplier (α) = 2 / (period + 1)
 * - EMA(today) = (Close(today) - EMA(yesterday)) × α + EMA(yesterday)
 * - First EMA = SMA of first 'period' data points
 *
 * **Usage**:
 * - Short-term trend identification (more responsive than SMA)
 * - Component of other indicators (MACD, RSI)
 * - Dynamic support/resistance levels
 *
 * @property period Number of periods for the moving average (default: 12)
 */
class EMAIndicator(
    private val period: Int = 12
) : ITechnicalIndicator<Double> {
    
    private val multiplier: Double = 2.0 / (period + 1)
    private var previousEMA: Double? = null
    
    init {
        require(period > 0) { "Period must be positive, got: $period" }
    }
    
    override fun calculate(data: List<Candlestick>): Double? {
        if (!validateData(data)) {
            logger.warn { "Insufficient data for EMA($period): need $period, got ${data.size}" }
            return null
        }
        
        try {
            // If we have exactly the required data points, calculate initial EMA using SMA
            if (data.size == period || previousEMA == null) {
                val sma = SMAIndicator(period).calculate(data.take(period))
                previousEMA = sma
                
                // If more data available, continue calculating EMA for remaining points
                if (data.size > period) {
                    for (i in period until data.size) {
                        val close = data[i].close.toDouble()
                        previousEMA = calculateEMA(close, previousEMA!!)
                    }
                }
                
                return previousEMA
            }
            
            // Continue EMA calculation with new data point
            val close = data.last().close.toDouble()
            previousEMA = calculateEMA(close, previousEMA!!)
            return previousEMA
            
        } catch (e: Exception) {
            logger.error(e) { "Error calculating EMA($period)" }
            throw IndicatorException.calculationError("EMA", e)
        }
    }
    
    /**
     * Calculate single EMA value using the EMA formula
     *
     * @param currentPrice Current closing price
     * @param previousEMA Previous EMA value
     * @return New EMA value
     */
    private fun calculateEMA(currentPrice: Double, previousEMA: Double): Double {
        return (currentPrice - previousEMA) * multiplier + previousEMA
    }
    
    /**
     * Calculate EMA for all possible windows in the data series
     *
     * @param data List of candlesticks
     * @return List of EMA values (null for periods with insufficient data)
     */
    fun calculateAll(data: List<Candlestick>): List<Double?> {
        val result = mutableListOf<Double?>()
        
        if (data.size < period) {
            return List(data.size) { null }
        }
        
        // Fill nulls for insufficient data periods
        repeat(period - 1) {
            result.add(null)
        }
        
        // Calculate initial EMA using SMA
        val initialSMA = SMAIndicator(period).calculate(data.take(period))
        result.add(initialSMA)
        var currentEMA = initialSMA ?: return result
        
        // Calculate EMA for remaining data points
        for (i in period until data.size) {
            val close = data[i].close.toDouble()
            currentEMA = calculateEMA(close, currentEMA)
            result.add(currentEMA)
        }
        
        return result
    }
    
    override fun getName(): String = "EMA"
    
    override fun getRequiredDataPoints(): Int = period
    
    override fun reset() {
        previousEMA = null
    }
    
    override fun toString(): String = "EMA(period=$period, multiplier=%.4f)".format(multiplier)
}

