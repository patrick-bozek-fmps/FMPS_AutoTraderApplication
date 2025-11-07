package com.fmps.autotrader.shared.dto

import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Data Transfer Object for Trade
 * Used for API requests and responses
 */
@Serializable
data class TradeDTO(
    val id: Long? = null,
    val aiTraderId: Long,
    val exchange: String,
    val tradingPair: String,
    val side: String, // LONG, SHORT
    val entryPrice: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val exitPrice: @Serializable(with = BigDecimalSerializer::class) BigDecimal? = null,
    val quantity: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val leverage: Int,
    val stopLoss: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val takeProfit: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val status: String, // OPEN, CLOSED, CANCELLED
    val profitLoss: @Serializable(with = BigDecimalSerializer::class) BigDecimal? = null,
    val profitLossPercentage: @Serializable(with = BigDecimalSerializer::class) BigDecimal? = null,
    val entryReason: String? = null,
    val exitReason: String? = null,
    val patternId: Long? = null,
    val entryTime: String,
    val exitTime: String? = null,
    val duration: Long? = null // Duration in milliseconds
) {
    /**
     * Is trade currently open?
     */
    fun isOpen(): Boolean = status == "OPEN"
    
    /**
     * Calculate actual profit/loss
     */
    fun calculateProfitLoss(): BigDecimal? {
        return exitPrice?.let { exit ->
            val priceDiff = when (side) {
                "LONG" -> exit - entryPrice
                "SHORT" -> entryPrice - exit
                else -> BigDecimal.ZERO
            }
            (priceDiff * quantity * BigDecimal(leverage))
        }
    }
}

/**
 * Request DTO for creating a new trade (entry)
 */
@Serializable
data class CreateTradeRequest(
    val aiTraderId: Long,
    val side: String, // LONG, SHORT
    val entryPrice: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val quantity: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val stopLoss: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val takeProfit: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val entryReason: String? = null,
    val patternId: Long? = null
)

/**
 * Request DTO for closing a trade (exit)
 */
@Serializable
data class CloseTradeRequest(
    val exitPrice: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val exitReason: String? = null
)

/**
 * Request DTO for updating stop-loss
 */
@Serializable
data class UpdateStopLossRequest(
    val stopLoss: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val trailingActivated: Boolean = false
)

