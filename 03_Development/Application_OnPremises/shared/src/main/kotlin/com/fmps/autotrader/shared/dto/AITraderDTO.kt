package com.fmps.autotrader.shared.dto

import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Data Transfer Object for AI Trader
 * Used for API requests and responses
 */
@Serializable
data class AITraderDTO(
    val id: Long? = null,
    val name: String,
    val exchange: String,
    val tradingPair: String,
    val status: String, // IDLE, ACTIVE, PAUSED, ERROR
    val leverage: Int = 10,
    val stopLossPercentage: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val takeProfitPercentage: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val initialBalance: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val currentBalance: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val totalProfit: @Serializable(with = BigDecimalSerializer::class) BigDecimal = BigDecimal.ZERO,
    val totalLoss: @Serializable(with = BigDecimalSerializer::class) BigDecimal = BigDecimal.ZERO,
    val winCount: Long = 0,
    val lossCount: Long = 0,
    val createdAt: String,
    val updatedAt: String,
    val lastActiveAt: String? = null
) {
    /**
     * Calculate win rate as percentage
     */
    fun winRatePercentage(): Double {
        val totalTrades = winCount + lossCount
        return if (totalTrades > 0) {
            (winCount.toDouble() / totalTrades) * 100
        } else {
            0.0
        }
    }
    
    /**
     * Calculate net profit/loss
     */
    fun netProfitLoss(): BigDecimal {
        return totalProfit - totalLoss
    }
    
    /**
     * Is trader currently active?
     */
    fun isActive(): Boolean = status == "ACTIVE"
}

/**
 * Request DTO for creating a new AI Trader
 */
@Serializable
data class CreateAITraderRequest(
    val name: String,
    val exchange: String,
    val tradingPair: String,
    val leverage: Int = 10,
    val stopLossPercentage: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val takeProfitPercentage: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val initialBalance: @Serializable(with = BigDecimalSerializer::class) BigDecimal
)

/**
 * Request DTO for updating an AI Trader
 */
@Serializable
data class UpdateAITraderRequest(
    val name: String? = null,
    val leverage: Int? = null,
    val stopLossPercentage: @Serializable(with = BigDecimalSerializer::class) BigDecimal? = null,
    val takeProfitPercentage: @Serializable(with = BigDecimalSerializer::class) BigDecimal? = null
)

/**
 * Request DTO for updating trader status
 */
@Serializable
data class UpdateTraderStatusRequest(
    val status: String // ACTIVE, PAUSED, STOPPED
)

/**
 * Request DTO for updating trader balance
 */
@Serializable
data class UpdateTraderBalanceRequest(
    val balance: @Serializable(with = BigDecimalSerializer::class) BigDecimal
)

/**
 * Response DTO indicating whether more traders can be created
 */
@Serializable
data class CanCreateTraderResponse(
    val canCreate: Boolean,
    val currentCount: Long,
    val maxAllowed: Long,
    val reason: String? = null
)

