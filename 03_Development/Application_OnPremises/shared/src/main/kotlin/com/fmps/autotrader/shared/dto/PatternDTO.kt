package com.fmps.autotrader.shared.dto

import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Data Transfer Object for Pattern
 * Used for API requests and responses
 */
@Serializable
data class PatternDTO(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    val exchange: String,
    val tradingPair: String,
    val indicators: String, // JSON string of indicator conditions
    val candlestickPattern: String? = null,
    val successRate: @Serializable(with = BigDecimalSerializer::class) BigDecimal = BigDecimal.ZERO,
    val totalTrades: Long = 0,
    val winningTrades: Long = 0,
    val losingTrades: Long = 0,
    val averageProfit: @Serializable(with = BigDecimalSerializer::class) BigDecimal = BigDecimal.ZERO,
    val averageLoss: @Serializable(with = BigDecimalSerializer::class) BigDecimal = BigDecimal.ZERO,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String,
    val lastUsedAt: String? = null
) {
    /**
     * Calculate win rate as percentage
     */
    fun winRatePercentage(): Double {
        return if (totalTrades > 0) {
            (winningTrades.toDouble() / totalTrades) * 100
        } else {
            0.0
        }
    }
    
    /**
     * Calculate expected value (EV)
     */
    fun expectedValue(): BigDecimal {
        val winRate = if (totalTrades > 0) {
            BigDecimal(winningTrades).divide(BigDecimal(totalTrades), 4, java.math.RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        
        val lossRate = BigDecimal.ONE - winRate
        
        return (averageProfit * winRate) - (averageLoss * lossRate)
    }
}

/**
 * Request DTO for creating a new pattern
 */
@Serializable
data class CreatePatternRequest(
    val name: String,
    val description: String? = null,
    val exchange: String,
    val tradingPair: String,
    val indicators: String, // JSON string
    val candlestickPattern: String? = null
)

/**
 * Request DTO for updating pattern statistics
 */
@Serializable
data class UpdatePatternStatisticsRequest(
    val wasWinningTrade: Boolean,
    val profitLoss: @Serializable(with = BigDecimalSerializer::class) BigDecimal
)

/**
 * Request DTO for finding matching patterns
 */
@Serializable
data class FindMatchingPatternsRequest(
    val exchange: String? = null,
    val tradingPair: String? = null,
    val indicators: String? = null,
    val minSuccessRate: @Serializable(with = BigDecimalSerializer::class) BigDecimal? = null,
    val minTrades: Long? = null,
    val onlyActive: Boolean = true
)

