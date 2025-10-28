package com.fmps.autotrader.shared.model

import com.fmps.autotrader.shared.dto.BigDecimalSerializer
import com.fmps.autotrader.shared.dto.InstantSerializer
import com.fmps.autotrader.shared.enums.TradeAction
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant

/**
 * Represents an open trading position.
 */
@Serializable
data class Position(
    val symbol: String,
    val action: TradeAction,
    @Serializable(with = BigDecimalSerializer::class)
    val quantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val entryPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val currentPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val unrealizedPnL: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val leverage: BigDecimal = BigDecimal.ONE,
    @Serializable(with = InstantSerializer::class)
    val openedAt: Instant = Instant.now()
)

