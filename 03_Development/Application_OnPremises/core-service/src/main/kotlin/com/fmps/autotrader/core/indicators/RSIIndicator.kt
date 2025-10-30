package com.fmps.autotrader.core.indicators

import com.fmps.autotrader.shared.model.Candlestick
import mu.KotlinLogging
import kotlin.math.abs

private val logger = KotlinLogging.logger {}

/**
 * Relative Strength Index (RSI) Indicator
 *
 * RSI is a momentum oscillator that measures the speed and magnitude of price changes.
 * It oscillates between 0 and 100, typically using 70 as overbought and 30 as oversold thresholds.
 *
 * **Formula**:
 * 1. Calculate price changes: gain/loss for each period
 * 2. Average Gain = EMA of gains over period
 * 3. Average Loss = EMA of losses over period
 * 4. RS (Relative Strength) = Average Gain / Average Loss
 * 5. RSI = 100 - (100 / (1 + RS))
 *
 * **Interpretation**:
 * - RSI > 70: Overbought (potential sell signal)
 * - RSI < 30: Oversold (potential buy signal)
 * - RSI = 50: Neutral momentum
 * - Divergence: RSI moving opposite to price can signal reversal
 *
 * @property period Number of periods for RSI calculation (default: 14)
 */
class RSIIndicator(
    private val period: Int = 14
) : ITechnicalIndicator<Double> {
    
    init {
        require(period > 0) { "Period must be positive, got: $period" }
    }
    
    override fun calculate(data: List<Candlestick>): Double? {
        // Need period + 1 data points (to calculate 'period' price changes)
        if (data.size < period + 1) {
            logger.warn { "Insufficient data for RSI($period): need ${period + 1}, got ${data.size}" }
            return null
        }
        
        try {
            // Calculate price changes
            val changes = mutableListOf<Double>()
            for (i in 1 until data.size) {
                val change = data[i].close.toDouble() - data[i - 1].close.toDouble()
                changes.add(change)
            }
            
            // Separate gains and losses
            val gains = changes.map { if (it > 0) it else 0.0 }
            val losses = changes.map { if (it < 0) abs(it) else 0.0 }
            
            // Calculate average gain and average loss using SMA for initial values
            val avgGain = gains.take(period).average()
            val avgLoss = losses.take(period).average()
            
            // If we have more data, smooth with EMA-like calculation
            var currentAvgGain = avgGain
            var currentAvgLoss = avgLoss
            
            if (changes.size > period) {
                for (i in period until changes.size) {
                    currentAvgGain = (currentAvgGain * (period - 1) + gains[i]) / period
                    currentAvgLoss = (currentAvgLoss * (period - 1) + losses[i]) / period
                }
            }
            
            // Calculate RSI
            return calculateRSI(currentAvgGain, currentAvgLoss)
            
        } catch (e: Exception) {
            logger.error(e) { "Error calculating RSI($period)" }
            throw IndicatorException.calculationError("RSI", e)
        }
    }
    
    /**
     * Calculate RSI value from average gain and average loss
     *
     * @param avgGain Average gain
     * @param avgLoss Average loss
     * @return RSI value (0-100)
     */
    private fun calculateRSI(avgGain: Double, avgLoss: Double): Double {
        // Handle edge case: all losses (RS = 0, RSI = 0)
        if (avgGain == 0.0) return 0.0
        
        // Handle edge case: all gains (RS = infinity, RSI = 100)
        if (avgLoss == 0.0) return 100.0
        
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }
    
    /**
     * Calculate RSI for all possible windows in the data series
     *
     * @param data List of candlesticks
     * @return List of RSI values (null for periods with insufficient data)
     */
    fun calculateAll(data: List<Candlestick>): List<Double?> {
        val result = mutableListOf<Double?>()
        
        // Not enough data for RSI
        if (data.size < period + 1) {
            return List(data.size) { null }
        }
        
        // Fill nulls for insufficient data periods
        repeat(period) {
            result.add(null)
        }
        
        // Calculate RSI for each window
        for (i in period until data.size) {
            val window = data.subList(0, i + 1)
            result.add(calculate(window))
        }
        
        return result
    }
    
    override fun getName(): String = "RSI"
    
    override fun getRequiredDataPoints(): Int = period + 1
    
    override fun reset() {
        // RSI is stateless, nothing to reset
    }
    
    override fun toString(): String = "RSI(period=$period)"
    
    companion object {
        /**
         * Check if RSI value indicates overbought condition
         *
         * @param rsi RSI value
         * @param threshold Overbought threshold (default: 70.0)
         * @return true if overbought
         */
        fun isOverbought(rsi: Double, threshold: Double = 70.0): Boolean {
            return rsi > threshold
        }
        
        /**
         * Check if RSI value indicates oversold condition
         *
         * @param rsi RSI value
         * @param threshold Oversold threshold (default: 30.0)
         * @return true if oversold
         */
        fun isOversold(rsi: Double, threshold: Double = 30.0): Boolean {
            return rsi < threshold
        }
        
        /**
         * Check if RSI is in neutral zone
         *
         * @param rsi RSI value
         * @param lowerThreshold Lower bound (default: 40.0)
         * @param upperThreshold Upper bound (default: 60.0)
         * @return true if neutral
         */
        fun isNeutral(rsi: Double, lowerThreshold: Double = 40.0, upperThreshold: Double = 60.0): Boolean {
            return rsi in lowerThreshold..upperThreshold
        }
    }
}

