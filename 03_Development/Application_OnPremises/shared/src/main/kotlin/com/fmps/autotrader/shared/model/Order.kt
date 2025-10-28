package com.fmps.autotrader.shared.model

import com.fmps.autotrader.shared.dto.BigDecimalSerializer
import com.fmps.autotrader.shared.dto.InstantSerializer
import com.fmps.autotrader.shared.enums.OrderType
import com.fmps.autotrader.shared.enums.TradeAction
import com.fmps.autotrader.shared.enums.TradeStatus
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant

/**
 * Represents a trading order.
 */
@Serializable
data class Order(
    val id: String? = null,
    val symbol: String,
    val action: TradeAction,
    val type: OrderType,
    @Serializable(with = BigDecimalSerializer::class)
    val quantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val stopPrice: BigDecimal? = null,
    val status: TradeStatus = TradeStatus.PENDING,
    @Serializable(with = BigDecimalSerializer::class)
    val filledQuantity: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val averagePrice: BigDecimal? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant = Instant.now(),
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant? = null
)

