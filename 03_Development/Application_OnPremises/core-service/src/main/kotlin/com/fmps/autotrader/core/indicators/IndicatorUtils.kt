package com.fmps.autotrader.core.indicators

import com.fmps.autotrader.shared.model.Candlestick
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Utility functions for technical indicator calculations
 */
object IndicatorUtils {
    
    /**
     * Calculate standard deviation of a list of values
     *
     * @param values List of values
     * @return Standard deviation
     */
    fun calculateStandardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val mean = values.average()
        return calculateStandardDeviation(values, mean)
    }
    
    /**
     * Calculate standard deviation with a known mean
     *
     * @param values List of values
     * @param mean Pre-calculated mean
     * @return Standard deviation
     */
    fun calculateStandardDeviation(values: List<Double>, mean: Double): Double {
        if (values.size < 2) return 0.0
        
        val variance = calculateVariance(values, mean)
        return sqrt(variance)
    }
    
    /**
     * Calculate variance of a list of values
     *
     * @param values List of values
     * @return Variance
     */
    fun calculateVariance(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val mean = values.average()
        return calculateVariance(values, mean)
    }
    
    /**
     * Calculate variance with a known mean
     *
     * @param values List of values
     * @param mean Pre-calculated mean
     * @return Variance
     */
    fun calculateVariance(values: List<Double>, mean: Double): Double {
        if (values.size < 2) return 0.0
        
        return values.map { (it - mean).pow(2) }.average()
    }
    
    /**
     * Detect crossover between two values
     *
     * @param current Current value of primary series
     * @param previous Previous value of primary series
     * @param threshold Current value of threshold/baseline series
     * @param previousThreshold Previous value of threshold/baseline series
     * @return CrossoverType (BULLISH, BEARISH, or NONE)
     */
    fun detectCrossover(
        current: Double,
        previous: Double,
        threshold: Double,
        previousThreshold: Double
    ): CrossoverType {
        // Bullish crossover: was below, now above
        if (previous <= previousThreshold && current > threshold) {
            return CrossoverType.BULLISH
        }
        
        // Bearish crossover: was above, now below
        if (previous >= previousThreshold && current < threshold) {
            return CrossoverType.BEARISH
        }
        
        return CrossoverType.NONE
    }
    
    /**
     * Detect simple crossover (crossing a fixed threshold)
     *
     * @param current Current value
     * @param previous Previous value
     * @param threshold Fixed threshold value
     * @return CrossoverType (BULLISH, BEARISH, or NONE)
     */
    fun detectCrossover(
        current: Double,
        previous: Double,
        threshold: Double
    ): CrossoverType {
        return detectCrossover(current, previous, threshold, threshold)
    }
    
    /**
     * Detect trend in a series of values
     *
     * @param values List of values (chronological order)
     * @param minPeriod Minimum periods to consider (default: 3)
     * @return TrendType (UPTREND, DOWNTREND, or SIDEWAYS)
     */
    fun detectTrend(values: List<Double>, minPeriod: Int = 3): TrendType {
        if (values.size < minPeriod) return TrendType.SIDEWAYS
        
        // Calculate linear regression slope
        val n = values.size
        val sumX = (0 until n).sum()
        val sumY = values.sum()
        val sumXY = values.mapIndexed { index, value -> index * value }.sum()
        val sumX2 = (0 until n).map { it * it }.sum()
        
        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
        
        // Determine trend based on slope
        val threshold = values.average() * 0.001  // 0.1% threshold
        
        return when {
            slope > threshold -> TrendType.UPTREND
            slope < -threshold -> TrendType.DOWNTREND
            else -> TrendType.SIDEWAYS
        }
    }
    
    /**
     * Calculate percentage change between two values
     *
     * @param from Starting value
     * @param to Ending value
     * @return Percentage change (e.g., 0.05 = 5% increase)
     */
    fun calculatePercentageChange(from: Double, to: Double): Double {
        if (from == 0.0) return 0.0
        return (to - from) / from
    }
    
    /**
     * Calculate true range (used in ATR and other indicators)
     *
     * True Range = max(high - low, abs(high - prev_close), abs(low - prev_close))
     *
     * @param current Current candlestick
     * @param previous Previous candlestick
     * @return True range value
     */
    fun calculateTrueRange(current: Candlestick, previous: Candlestick): Double {
        val high = current.high.toDouble()
        val low = current.low.toDouble()
        val prevClose = previous.close.toDouble()
        
        val range1 = high - low
        val range2 = kotlin.math.abs(high - prevClose)
        val range3 = kotlin.math.abs(low - prevClose)
        
        return maxOf(range1, range2, range3)
    }
    
    /**
     * Check if a value is within a range (with tolerance)
     *
     * @param value Value to check
     * @param target Target value
     * @param tolerance Tolerance percentage (default: 0.01 = 1%)
     * @return true if within range
     */
    fun isWithinTolerance(value: Double, target: Double, tolerance: Double = 0.01): Boolean {
        val threshold = target * tolerance
        return kotlin.math.abs(value - target) <= threshold
    }
    
    /**
     * Normalize values to 0-1 range
     *
     * @param values List of values
     * @return Normalized values
     */
    fun normalize(values: List<Double>): List<Double> {
        if (values.isEmpty()) return emptyList()
        
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 0.0
        val range = max - min
        
        if (range == 0.0) return List(values.size) { 0.5 }
        
        return values.map { (it - min) / range }
    }
}

/**
 * Crossover type enumeration
 */
enum class CrossoverType {
    /** Crossed from below to above (bullish signal) */
    BULLISH,
    
    /** Crossed from above to below (bearish signal) */
    BEARISH,
    
    /** No crossover occurred */
    NONE
}

/**
 * Trend type enumeration
 */
enum class TrendType {
    /** Upward trend (prices increasing) */
    UPTREND,
    
    /** Downward trend (prices decreasing) */
    DOWNTREND,
    
    /** Sideways/consolidation (no clear direction) */
    SIDEWAYS
}

