package com.fmps.autotrader.core.traders

import com.fmps.autotrader.shared.dto.BigDecimalSerializer
import com.fmps.autotrader.shared.dto.InstantSerializer
import com.fmps.autotrader.shared.model.Candlestick
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant

/**
 * Processed market data with calculated indicators.
 *
 * This data class contains candlestick data along with pre-calculated indicator values
 * to avoid redundant calculations during signal generation.
 *
 * @property candles List of candlesticks in chronological order
 * @property indicators Map of indicator names to their calculated values
 * @property latestPrice Current/latest price from the most recent candlestick
 * @property timestamp When this data was processed
 *
 * @since 1.0.0
 */
@Serializable
data class ProcessedMarketData(
    val candles: List<Candlestick>,
    val indicators: Map<String, @Contextual Any>,
    @Serializable(with = BigDecimalSerializer::class) val latestPrice: BigDecimal,
    @Serializable(with = InstantSerializer::class)
    val timestamp: Instant = Instant.now()
) {
    init {
        require(candles.isNotEmpty()) { "Candles list cannot be empty" }
        require(latestPrice > BigDecimal.ZERO) { "Latest price must be positive" }
    }

    /**
     * Get the latest candlestick.
     */
    fun getLatestCandle(): Candlestick = candles.last()

    /**
     * Get indicator value by name.
     *
     * @param name Indicator name
     * @return Indicator value, or null if not found
     */
    fun getIndicator(name: String): Any? = indicators[name]

    /**
     * Check if indicator is available.
     */
    fun hasIndicator(name: String): Boolean = indicators.containsKey(name)
}

