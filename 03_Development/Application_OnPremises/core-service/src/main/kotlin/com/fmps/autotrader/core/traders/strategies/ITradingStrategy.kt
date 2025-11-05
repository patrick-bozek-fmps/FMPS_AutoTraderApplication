package com.fmps.autotrader.core.traders.strategies

import com.fmps.autotrader.core.traders.AITraderConfig
import com.fmps.autotrader.core.traders.TradingSignal
import com.fmps.autotrader.shared.model.Candlestick

/**
 * Interface for all trading strategies.
 *
 * Trading strategies analyze market data and generate trading signals (BUY, SELL, HOLD, CLOSE).
 * Each strategy implements different trading logic based on technical indicators and market conditions.
 *
 * @since 1.0.0
 */
interface ITradingStrategy {

    /**
     * Generate a trading signal based on candlestick data and indicator values.
     *
     * @param candles List of candlesticks in chronological order (oldest first)
     * @param indicators Map of indicator names to their calculated values
     * @return Trading signal with action, confidence, and reasoning
     */
    fun generateSignal(
        candles: List<Candlestick>,
        indicators: Map<String, Any>
    ): TradingSignal

    /**
     * Get the name of this strategy.
     *
     * @return Strategy name (e.g., "Trend Following", "Mean Reversion")
     */
    fun getName(): String

    /**
     * Get a description of this strategy.
     *
     * @return Human-readable description of the strategy's logic
     */
    fun getDescription(): String

    /**
     * Get the list of required indicator names for this strategy.
     *
     * @return List of indicator names (e.g., ["SMA", "RSI", "MACD"])
     */
    fun getRequiredIndicators(): List<String>

    /**
     * Validate that the configuration is compatible with this strategy.
     *
     * @param config AI Trader configuration
     * @return true if configuration is valid for this strategy
     */
    fun validateConfig(config: AITraderConfig): Boolean

    /**
     * Reset any internal state or cached values.
     *
     * This should be called when switching to a new data series
     * or when the strategy needs to recalculate from scratch.
     */
    fun reset()
}

