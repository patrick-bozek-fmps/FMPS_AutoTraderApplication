package com.fmps.autotrader.core.indicators.models

import kotlin.math.abs

/**
 * Result of Bollinger Bands calculation
 *
 * Bollinger Bands are volatility bands placed above and below a moving average.
 * Volatility is based on the standard deviation, which changes as volatility increases and decreases.
 *
 * @property upper Upper band (middle + stdDev * multiplier)
 * @property middle Middle band (Simple Moving Average)
 * @property lower Lower band (middle - stdDev * multiplier)
 * @property bandwidth Bandwidth as percentage: (upper - lower) / middle
 * @property percentB %B indicator: (price - lower) / (upper - lower)
 */
data class BollingerBandsResult(
    val upper: Double,
    val middle: Double,
    val lower: Double,
    val bandwidth: Double,
    val percentB: Double? = null  // Optional, requires current price
) {
    /**
     * Check if the bands are squeezing (low volatility)
     *
     * @param threshold Bandwidth threshold (default 0.05 = 5%)
     * @return true if bandwidth is below threshold
     */
    fun isSqueeze(threshold: Double = 0.05): Boolean {
        return bandwidth < threshold
    }
    
    /**
     * Check if the bands are expanding (high volatility)
     *
     * @param previous Previous Bollinger Bands result
     * @return true if bandwidth is increasing
     */
    fun isExpanding(previous: BollingerBandsResult): Boolean {
        return this.bandwidth > previous.bandwidth
    }
    
    /**
     * Check if the bands are contracting (decreasing volatility)
     *
     * @param previous Previous Bollinger Bands result
     * @return true if bandwidth is decreasing
     */
    fun isContracting(previous: BollingerBandsResult): Boolean {
        return this.bandwidth < previous.bandwidth
    }
    
    /**
     * Check if price is touching the upper band
     *
     * @param price Current price
     * @param tolerance Percentage tolerance (default 0.01 = 1%)
     * @return true if price is within tolerance of upper band
     */
    fun isTouchingUpperBand(price: Double, tolerance: Double = 0.01): Boolean {
        val threshold = upper * tolerance
        return abs(price - upper) <= threshold
    }
    
    /**
     * Check if price is touching the lower band
     *
     * @param price Current price
     * @param tolerance Percentage tolerance (default 0.01 = 1%)
     * @return true if price is within tolerance of lower band
     */
    fun isTouchingLowerBand(price: Double, tolerance: Double = 0.01): Boolean {
        val threshold = lower * tolerance
        return abs(price - lower) <= threshold
    }
    
    /**
     * Check if price is outside the upper band
     *
     * @param price Current price
     * @return true if price is above upper band
     */
    fun isAboveUpperBand(price: Double): Boolean = price > upper
    
    /**
     * Check if price is outside the lower band
     *
     * @param price Current price
     * @return true if price is below lower band
     */
    fun isBelowLowerBand(price: Double): Boolean = price < lower
    
    /**
     * Calculate %B for a given price
     *
     * %B = (price - lower) / (upper - lower)
     * - %B > 1.0: price above upper band
     * - %B = 0.5: price at middle band
     * - %B < 0.0: price below lower band
     *
     * @param price Current price
     * @return %B value
     */
    fun calculatePercentB(price: Double): Double {
        val bandWidth = upper - lower
        if (bandWidth == 0.0) return 0.5  // Avoid division by zero
        return (price - lower) / bandWidth
    }
    
    override fun toString(): String {
        return "BollingerBands(upper=%.4f, middle=%.4f, lower=%.4f, bandwidth=%.4f%%)".format(
            upper, middle, lower, bandwidth * 100
        )
    }
}

