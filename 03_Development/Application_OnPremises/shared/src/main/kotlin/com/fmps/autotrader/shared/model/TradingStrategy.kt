package com.fmps.autotrader.shared.model

import kotlinx.serialization.Serializable

/**
 * Trading strategy types available for AI Traders.
 *
 * Each strategy implements different trading logic:
 * - TREND_FOLLOWING: Follows market trends using moving averages
 * - MEAN_REVERSION: Trades against the trend, expecting price to revert to mean
 * - BREAKOUT: Trades on price breakouts from consolidation patterns
 *
 * @since 1.0.0
 */
@Serializable
enum class TradingStrategy {
    /**
     * Trend Following Strategy
     * 
     * Follows market trends using moving average crossovers.
     * Generates BUY signals when short-term MA crosses above long-term MA (golden cross).
     * Generates SELL signals when short-term MA crosses below long-term MA (death cross).
     */
    TREND_FOLLOWING,

    /**
     * Mean Reversion Strategy
     * 
     * Trades against the trend, expecting price to revert to its mean.
     * Uses Bollinger Bands to identify overbought/oversold conditions.
     * Generates BUY signals when price touches lower band (oversold).
     * Generates SELL signals when price touches upper band (overbought).
     */
    MEAN_REVERSION,

    /**
     * Breakout Strategy
     * 
     * Trades on price breakouts from consolidation patterns.
     * Uses Bollinger Bands to identify breakouts with momentum confirmation.
     * Generates BUY signals when price breaks above upper band with momentum.
     * Generates SELL signals when price breaks below lower band with momentum.
     */
    BREAKOUT
}

