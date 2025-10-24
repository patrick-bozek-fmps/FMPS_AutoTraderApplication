package com.fmps.autotrader.shared.dto

import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Data Transfer Object for Trade Statistics
 * Aggregated statistics for an AI Trader
 */
@Serializable
data class TradeStatisticsDTO(
    val aiTraderId: Long,
    val totalTrades: Long,
    val openTrades: Long,
    val closedTrades: Long,
    val winningTrades: Long,
    val losingTrades: Long,
    val winRate: Double,
    val totalProfit: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val totalLoss: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val netProfitLoss: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val averageProfit: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val averageLoss: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val largestWin: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val largestLoss: @Serializable(with = BigDecimalSerializer::class) BigDecimal,
    val averageTradeDuration: Long, // in milliseconds
    val longestTrade: Long, // in milliseconds
    val shortestTrade: Long, // in milliseconds
    val profitFactor: @Serializable(with = BigDecimalSerializer::class) BigDecimal
) {
    /**
     * Calculate risk-reward ratio
     */
    fun riskRewardRatio(): BigDecimal {
        return if (averageLoss > BigDecimal.ZERO) {
            averageProfit.divide(averageLoss, 2, java.math.RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
    }
}

