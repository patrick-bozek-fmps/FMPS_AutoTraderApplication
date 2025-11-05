package com.fmps.autotrader.core.traders.strategies

import com.fmps.autotrader.core.traders.AITraderConfig
import com.fmps.autotrader.shared.model.TradingStrategy
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Factory for creating trading strategy instances.
 *
 * This factory creates strategy instances based on the [TradingStrategy] enum value
 * specified in the trader configuration.
 *
 * @since 1.0.0
 */
object StrategyFactory {

    /**
     * Create a strategy instance based on the configuration.
     *
     * @param config AI Trader configuration containing the strategy type
     * @return Strategy instance implementing [ITradingStrategy]
     * @throws IllegalArgumentException if strategy type is not supported
     */
    fun createStrategy(config: AITraderConfig): ITradingStrategy {
        return when (config.strategy) {
            TradingStrategy.TREND_FOLLOWING -> {
                logger.debug { "Creating TrendFollowingStrategy for trader ${config.id}" }
                TrendFollowingStrategy(config)
            }
            TradingStrategy.MEAN_REVERSION -> {
                logger.debug { "Creating MeanReversionStrategy for trader ${config.id}" }
                MeanReversionStrategy(config)
            }
            TradingStrategy.BREAKOUT -> {
                logger.debug { "Creating BreakoutStrategy for trader ${config.id}" }
                BreakoutStrategy(config)
            }
        }
    }

    /**
     * Create a strategy instance based on the strategy type.
     *
     * @param strategy Strategy type
     * @param config Optional configuration (may be null for some strategies)
     * @return Strategy instance implementing [ITradingStrategy]
     * @throws IllegalArgumentException if strategy type is not supported
     */
    fun createStrategy(
        strategy: TradingStrategy,
        config: AITraderConfig? = null
    ): ITradingStrategy {
        return when (strategy) {
            TradingStrategy.TREND_FOLLOWING -> {
                requireNotNull(config) { "TrendFollowingStrategy requires configuration" }
                TrendFollowingStrategy(config)
            }
            TradingStrategy.MEAN_REVERSION -> {
                requireNotNull(config) { "MeanReversionStrategy requires configuration" }
                MeanReversionStrategy(config)
            }
            TradingStrategy.BREAKOUT -> {
                requireNotNull(config) { "BreakoutStrategy requires configuration" }
                BreakoutStrategy(config)
            }
        }
    }
}

