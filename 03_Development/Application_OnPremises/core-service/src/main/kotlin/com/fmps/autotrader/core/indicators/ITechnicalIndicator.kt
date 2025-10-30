package com.fmps.autotrader.core.indicators

import com.fmps.autotrader.shared.model.Candlestick

/**
 * Interface for all technical indicators
 *
 * Technical indicators analyze historical price data to identify trends,
 * momentum, volatility, and other market characteristics.
 *
 * @param T The result type (Double for simple indicators, complex types for multi-value indicators)
 */
interface ITechnicalIndicator<T> {
    
    /**
     * Calculate the indicator value(s) based on the provided candlestick data
     *
     * @param data List of candlesticks in chronological order (oldest first)
     * @return Calculated indicator value(s), or null if insufficient data
     * @throws IndicatorException if calculation fails
     */
    fun calculate(data: List<Candlestick>): T?
    
    /**
     * Get the name of this indicator
     *
     * @return Indicator name (e.g., "SMA", "RSI", "MACD")
     */
    fun getName(): String
    
    /**
     * Get the minimum number of data points required for calculation
     *
     * @return Minimum required data points
     */
    fun getRequiredDataPoints(): Int
    
    /**
     * Reset any internal state or cached values
     *
     * This should be called when switching to a new data series
     * or when the indicator needs to recalculate from scratch.
     */
    fun reset()
    
    /**
     * Validate that the input data is sufficient and valid
     *
     * @param data Candlestick data to validate
     * @return true if data is valid for calculation
     */
    fun validateData(data: List<Candlestick>): Boolean {
        return data.size >= getRequiredDataPoints() && data.isNotEmpty()
    }
}

