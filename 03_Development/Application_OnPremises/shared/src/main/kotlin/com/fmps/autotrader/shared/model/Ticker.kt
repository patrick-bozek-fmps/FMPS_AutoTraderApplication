package com.fmps.autotrader.shared.model

import com.fmps.autotrader.shared.dto.BigDecimalSerializer
import com.fmps.autotrader.shared.dto.InstantSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant

/**
 * Represents 24-hour ticker information for a trading pair.
 *
 * @param symbol The trading pair symbol (e.g., "BTCUSDT")
 * @param lastPrice The last traded price
 * @param bidPrice Best bid price
 * @param askPrice Best ask price
 * @param volume 24-hour trading volume
 * @param quoteVolume 24-hour trading volume in quote currency
 * @param priceChange 24-hour price change
 * @param priceChangePercent 24-hour price change percentage
 * @param high 24-hour high price
 * @param low 24-hour low price
 * @param openPrice Price at the start of 24-hour period
 * @param timestamp Ticker timestamp
 */
@Serializable
data class Ticker(
    val symbol: String,
    @Serializable(with = BigDecimalSerializer::class)
    val lastPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val bidPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val askPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val volume: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val quoteVolume: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val priceChange: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val priceChangePercent: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val high: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val low: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val openPrice: BigDecimal,
    @Serializable(with = InstantSerializer::class)
    val timestamp: Instant = Instant.now()
)

