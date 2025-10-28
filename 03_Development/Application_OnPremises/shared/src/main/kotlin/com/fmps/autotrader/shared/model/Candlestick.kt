package com.fmps.autotrader.shared.model

import com.fmps.autotrader.shared.dto.BigDecimalSerializer
import com.fmps.autotrader.shared.dto.InstantSerializer
import com.fmps.autotrader.shared.enums.TimeFrame
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant

/**
 * Represents an OHLCV candlestick for technical analysis.
 */
@Serializable
data class Candlestick(
    val symbol: String,
    val interval: TimeFrame,
    @Serializable(with = InstantSerializer::class)
    val openTime: Instant,
    @Serializable(with = InstantSerializer::class)
    val closeTime: Instant,
    @Serializable(with = BigDecimalSerializer::class)
    val open: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val high: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val low: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val close: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val volume: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val quoteVolume: BigDecimal? = null
)

