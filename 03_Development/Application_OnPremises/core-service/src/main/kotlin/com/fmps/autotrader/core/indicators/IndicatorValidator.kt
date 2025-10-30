package com.fmps.autotrader.core.indicators

import com.fmps.autotrader.shared.model.Candlestick
import java.time.Duration

/**
 * Validator for indicator input data and parameters
 */
object IndicatorValidator {
    
    /**
     * Validate candlestick data for indicator calculation
     *
     * @param data Candlestick data to validate
     * @param minDataPoints Minimum required data points
     * @return Validation result
     */
    fun validateData(data: List<Candlestick>, minDataPoints: Int): ValidationResult {
        // Check if data is empty
        if (data.isEmpty()) {
            return ValidationResult.invalid("Data is empty")
        }
        
        // Check if enough data points
        if (data.size < minDataPoints) {
            return ValidationResult.invalid(
                "Insufficient data: need $minDataPoints, got ${data.size}"
            )
        }
        
        // Check chronological order
        if (!isChronological(data)) {
            return ValidationResult.invalid("Data is not in chronological order")
        }
        
        // Check for gaps in data
        val gap = findLargeGap(data)
        if (gap != null) {
            return ValidationResult.warning(
                "Large time gap detected between ${gap.first.closeTime} and ${gap.second.openTime}"
            )
        }
        
        // Check for outliers
        val outliers = findOutliers(data)
        if (outliers.isNotEmpty()) {
            return ValidationResult.warning(
                "Found ${outliers.size} potential outliers in data"
            )
        }
        
        return ValidationResult.valid()
    }
    
    /**
     * Validate indicator period parameter
     *
     * @param period Period value
     * @param minPeriod Minimum allowed period
     * @param maxPeriod Maximum allowed period
     * @return Validation result
     */
    fun validatePeriod(period: Int, minPeriod: Int = 1, maxPeriod: Int = 1000): ValidationResult {
        return when {
            period < minPeriod -> ValidationResult.invalid("Period $period is less than minimum $minPeriod")
            period > maxPeriod -> ValidationResult.invalid("Period $period exceeds maximum $maxPeriod")
            else -> ValidationResult.valid()
        }
    }
    
    /**
     * Validate multiplier parameter (for Bollinger Bands, etc.)
     *
     * @param multiplier Multiplier value
     * @param minMultiplier Minimum allowed multiplier
     * @param maxMultiplier Maximum allowed multiplier
     * @return Validation result
     */
    fun validateMultiplier(
        multiplier: Double,
        minMultiplier: Double = 0.1,
        maxMultiplier: Double = 10.0
    ): ValidationResult {
        return when {
            multiplier < minMultiplier -> ValidationResult.invalid(
                "Multiplier $multiplier is less than minimum $minMultiplier"
            )
            multiplier > maxMultiplier -> ValidationResult.invalid(
                "Multiplier $multiplier exceeds maximum $maxMultiplier"
            )
            else -> ValidationResult.valid()
        }
    }
    
    /**
     * Check if candlesticks are in chronological order
     *
     * @param data Candlestick data
     * @return true if chronological
     */
    private fun isChronological(data: List<Candlestick>): Boolean {
        for (i in 1 until data.size) {
            if (data[i].openTime <= data[i - 1].openTime) {
                return false
            }
        }
        return true
    }
    
    /**
     * Find large gaps in candlestick data
     *
     * A gap is considered large if it's more than 2x the typical interval
     *
     * @param data Candlestick data
     * @return Pair of candlesticks with large gap, or null if none found
     */
    private fun findLargeGap(data: List<Candlestick>): Pair<Candlestick, Candlestick>? {
        if (data.size < 3) return null
        
        // Calculate typical interval
        val intervals = mutableListOf<Long>()
        for (i in 1 until minOf(10, data.size)) {
            val duration = Duration.between(data[i - 1].openTime, data[i].openTime)
            intervals.add(duration.toMillis())
        }
        
        if (intervals.isEmpty()) return null
        val typicalInterval = intervals.average()
        
        // Look for gaps > 2x typical interval
        for (i in 1 until data.size) {
            val duration = Duration.between(data[i - 1].closeTime, data[i].openTime)
            if (duration.toMillis() > typicalInterval * 2) {
                return Pair(data[i - 1], data[i])
            }
        }
        
        return null
    }
    
    /**
     * Find potential outliers in candlestick data
     *
     * An outlier is defined as a price that deviates significantly from recent prices
     *
     * @param data Candlestick data
     * @return List of candlestick indices that are potential outliers
     */
    private fun findOutliers(data: List<Candlestick>): List<Int> {
        if (data.size < 20) return emptyList()
        
        val outliers = mutableListOf<Int>()
        val closes = data.map { it.close.toDouble() }
        
        // Use a rolling window to detect outliers
        val windowSize = 20
        for (i in windowSize until closes.size) {
            val window = closes.subList(i - windowSize, i)
            val mean = window.average()
            val stdDev = IndicatorUtils.calculateStandardDeviation(window, mean)
            
            val currentPrice = closes[i]
            val zScore = kotlin.math.abs((currentPrice - mean) / stdDev)
            
            // Z-score > 3 indicates potential outlier (3 standard deviations)
            if (zScore > 3.0) {
                outliers.add(i)
            }
        }
        
        return outliers
    }
    
    /**
     * Check for missing or zero volumes (potential data quality issue)
     *
     * @param data Candlestick data
     * @return List of candlestick indices with suspicious volume
     */
    fun findSuspiciousVolumes(data: List<Candlestick>): List<Int> {
        return data.mapIndexedNotNull { index, candle ->
            if (candle.volume.toDouble() <= 0.0) index else null
        }
    }
}

/**
 * Result of validation operation
 */
sealed class ValidationResult {
    /** Validation passed with no issues */
    data class Valid(val message: String = "Valid") : ValidationResult()
    
    /** Validation passed but with warnings */
    data class Warning(val message: String) : ValidationResult()
    
    /** Validation failed */
    data class Invalid(val message: String) : ValidationResult()
    
    fun isValid(): Boolean = this is Valid || this is Warning
    fun isInvalid(): Boolean = this is Invalid
    fun hasWarning(): Boolean = this is Warning
    
    companion object {
        fun valid(message: String = "Valid"): ValidationResult = Valid(message)
        fun warning(message: String): ValidationResult = Warning(message)
        fun invalid(message: String): ValidationResult = Invalid(message)
    }
}

