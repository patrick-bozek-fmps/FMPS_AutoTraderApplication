package com.fmps.autotrader.core.indicators

import com.fmps.autotrader.core.indicators.models.BollingerBandsResult
import com.fmps.autotrader.shared.model.Candlestick
import mu.KotlinLogging
import kotlin.math.pow
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

/**
 * Bollinger Bands Indicator
 *
 * Bollinger Bands consist of three lines:
 * - Middle Band: Simple Moving Average
 * - Upper Band: Middle Band + (Standard Deviation × multiplier)
 * - Lower Band: Middle Band - (Standard Deviation × multiplier)
 *
 * The bands expand and contract based on market volatility.
 *
 * **Formula**:
 * - Middle Band = SMA(period)
 * - Standard Deviation = sqrt(sum((price - SMA)²) / period)
 * - Upper Band = Middle + (stdDev × multiplier)
 * - Lower Band = Middle - (stdDev × multiplier)
 * - Bandwidth = (Upper - Lower) / Middle
 * - %B = (Price - Lower) / (Upper - Lower)
 *
 * **Interpretation**:
 * - Price touching upper band: Potential overbought (sell signal)
 * - Price touching lower band: Potential oversold (buy signal)
 * - Squeeze (narrow bands): Low volatility, potential breakout coming
 * - Expansion (wide bands): High volatility
 * - Walking the bands: Strong trend (price consistently near one band)
 *
 * @property period Number of periods for SMA (default: 20)
 * @property stdDevMultiplier Standard deviation multiplier (default: 2.0)
 */
class BollingerBandsIndicator(
    private val period: Int = 20,
    private val stdDevMultiplier: Double = 2.0
) : ITechnicalIndicator<BollingerBandsResult> {
    
    private val smaIndicator = SMAIndicator(period)
    
    init {
        require(period > 0) { "Period must be positive, got: $period" }
        require(stdDevMultiplier > 0) { "Standard deviation multiplier must be positive, got: $stdDevMultiplier" }
    }
    
    override fun calculate(data: List<Candlestick>): BollingerBandsResult? {
        if (!validateData(data)) {
            logger.warn { "Insufficient data for BollingerBands($period,$stdDevMultiplier): need $period, got ${data.size}" }
            return null
        }
        
        try {
            // Calculate middle band (SMA)
            val middle = smaIndicator.calculate(data) ?: return null
            
            // Calculate standard deviation
            val closes = data.takeLast(period).map { it.close.toDouble() }
            val stdDev = calculateStandardDeviation(closes, middle)
            
            // Calculate upper and lower bands
            val upper = middle + (stdDev * stdDevMultiplier)
            val lower = middle - (stdDev * stdDevMultiplier)
            
            // Calculate bandwidth
            val bandwidth = (upper - lower) / middle
            
            // Calculate %B for current price
            val currentPrice = data.last().close.toDouble()
            val percentB = if (upper - lower != 0.0) {
                (currentPrice - lower) / (upper - lower)
            } else {
                0.5  // Neutral if bands have collapsed
            }
            
            return BollingerBandsResult(
                upper = upper,
                middle = middle,
                lower = lower,
                bandwidth = bandwidth,
                percentB = percentB
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Error calculating BollingerBands($period,$stdDevMultiplier)" }
            throw IndicatorException.calculationError("BollingerBands", e)
        }
    }
    
    /**
     * Calculate standard deviation of prices
     *
     * @param prices List of prices
     * @param mean Mean (average) of prices
     * @return Standard deviation
     */
    private fun calculateStandardDeviation(prices: List<Double>, mean: Double): Double {
        if (prices.size < 2) return 0.0
        
        // Calculate variance: average of squared differences from mean
        val variance = prices.map { (it - mean).pow(2) }.average()
        
        // Standard deviation is square root of variance
        return sqrt(variance)
    }
    
    /**
     * Calculate Bollinger Bands for all possible windows in the data series
     *
     * @param data List of candlesticks
     * @return List of Bollinger Bands results (null for periods with insufficient data)
     */
    fun calculateAll(data: List<Candlestick>): List<BollingerBandsResult?> {
        val result = mutableListOf<BollingerBandsResult?>()
        
        if (data.size < period) {
            return List(data.size) { null }
        }
        
        // Fill nulls for insufficient data periods
        repeat(period - 1) {
            result.add(null)
        }
        
        // Calculate Bollinger Bands for each window
        for (i in period - 1 until data.size) {
            val window = data.subList(i - period + 1, i + 1)
            result.add(calculate(window))
        }
        
        return result
    }
    
    override fun getName(): String = "BollingerBands"
    
    override fun getRequiredDataPoints(): Int = period
    
    override fun reset() {
        smaIndicator.reset()
    }
    
    override fun toString(): String = "BollingerBands(period=$period, stdDev=$stdDevMultiplier)"
}

