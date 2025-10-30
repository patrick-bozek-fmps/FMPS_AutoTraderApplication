package com.fmps.autotrader.core.indicators.models

/**
 * Result of MACD (Moving Average Convergence Divergence) calculation
 *
 * MACD is a trend-following momentum indicator that shows the relationship
 * between two moving averages of a security's price.
 *
 * @property macd MACD line (fast EMA - slow EMA)
 * @property signal Signal line (EMA of MACD line)
 * @property histogram Histogram (MACD line - signal line)
 */
data class MACDResult(
    val macd: Double,
    val signal: Double,
    val histogram: Double
) {
    /**
     * Check if MACD is above the signal line (bullish)
     */
    fun isBullish(): Boolean = macd > signal
    
    /**
     * Check if MACD is below the signal line (bearish)
     */
    fun isBearish(): Boolean = macd < signal
    
    /**
     * Check if histogram is positive (momentum increasing)
     */
    fun hasPositiveMomentum(): Boolean = histogram > 0
    
    /**
     * Check if histogram is negative (momentum decreasing)
     */
    fun hasNegativeMomentum(): Boolean = histogram < 0
    
    /**
     * Detect bullish crossover (MACD crosses above signal)
     *
     * @param previous Previous MACD result
     * @return true if bullish crossover occurred
     */
    fun isBullishCrossover(previous: MACDResult): Boolean {
        return this.macd > this.signal && previous.macd <= previous.signal
    }
    
    /**
     * Detect bearish crossover (MACD crosses below signal)
     *
     * @param previous Previous MACD result
     * @return true if bearish crossover occurred
     */
    fun isBearishCrossover(previous: MACDResult): Boolean {
        return this.macd < this.signal && previous.macd >= previous.signal
    }
    
    override fun toString(): String {
        return "MACD(macd=%.4f, signal=%.4f, histogram=%.4f)".format(macd, signal, histogram)
    }
}

