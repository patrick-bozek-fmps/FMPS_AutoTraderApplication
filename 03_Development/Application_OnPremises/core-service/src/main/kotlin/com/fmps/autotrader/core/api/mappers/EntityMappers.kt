package com.fmps.autotrader.core.api.mappers

import com.fmps.autotrader.core.database.repositories.AITrader
import com.fmps.autotrader.core.database.repositories.Trade
import com.fmps.autotrader.core.database.repositories.TradeStatistics
import com.fmps.autotrader.shared.dto.*
import java.time.Duration
import java.time.format.DateTimeFormatter

/**
 * Extension functions to convert between database entities and DTOs
 */

// ============================================================================
// AI Trader Mappers
// ============================================================================

/**
 * Convert AITrader entity to AITraderDTO
 */
fun AITrader.toDTO(): AITraderDTO {
    // Calculate statistics
    val totalProfit = if (currentBalance > initialBalance) {
        currentBalance - initialBalance
    } else {
        java.math.BigDecimal.ZERO
    }
    
    val totalLoss = if (currentBalance < initialBalance) {
        initialBalance - currentBalance
    } else {
        java.math.BigDecimal.ZERO
    }
    
    return AITraderDTO(
        id = this.id.toLong(),
        name = this.name,
        exchange = this.exchange,
        tradingPair = this.tradingPair,
        status = this.status,
        leverage = this.leverage,
        stopLossPercentage = this.stopLossPercent,
        takeProfitPercentage = this.takeProfitPercent,
        initialBalance = this.initialBalance,
        currentBalance = this.currentBalance,
        totalProfit = totalProfit,
        totalLoss = totalLoss,
        winCount = 0, // TODO: Calculate from trades
        lossCount = 0, // TODO: Calculate from trades
        createdAt = this.createdAt.format(DateTimeFormatter.ISO_DATE_TIME),
        updatedAt = this.updatedAt.format(DateTimeFormatter.ISO_DATE_TIME),
        lastActiveAt = this.lastActiveAt?.format(DateTimeFormatter.ISO_DATE_TIME)
    )
}

/**
 * Convert list of AITrader entities to DTOs
 */
fun List<AITrader>.toAITraderDTOs(): List<AITraderDTO> = this.map { it.toDTO() }

// ============================================================================
// Trade Mappers
// ============================================================================

/**
 * Convert Trade entity to TradeDTO
 */
fun Trade.toDTO(): TradeDTO {
    val duration = if (exitTimestamp != null) {
        Duration.between(entryTimestamp, exitTimestamp).toMillis()
    } else {
        null
    }
    
    return TradeDTO(
        id = this.id.toLong(),
        aiTraderId = this.aiTraderId.toLong(),
        exchange = this.exchange,
        tradingPair = this.tradingPair,
        side = this.tradeType,
        entryPrice = this.entryPrice,
        exitPrice = this.exitPrice,
        quantity = this.entryAmount,
        leverage = this.leverage,
        stopLoss = this.stopLossPrice,
        takeProfit = this.takeProfitPrice,
        status = this.status,
        profitLoss = this.profitLoss,
        profitLossPercentage = this.profitLossPercent,
        entryReason = this.notes, // Using notes field for entry reason
        exitReason = this.exitReason,
        patternId = this.patternId?.toLong(),
        entryTime = this.entryTimestamp.format(DateTimeFormatter.ISO_DATE_TIME),
        exitTime = this.exitTimestamp?.format(DateTimeFormatter.ISO_DATE_TIME),
        duration = duration
    )
}

/**
 * Convert list of Trade entities to DTOs
 */
fun List<Trade>.toTradeDTOs(): List<TradeDTO> = this.map { it.toDTO() }

// ============================================================================
// Trade Statistics Mappers
// ============================================================================

/**
 * Convert TradeStatistics entity to TradeStatisticsDTO
 */
fun TradeStatistics.toDTO(aiTraderId: Long): TradeStatisticsDTO {
    val openTrades = 0L // TODO: Pass from repository
    val closedTrades = this.totalTrades.toLong()
    val totalTrades = openTrades + closedTrades
    
    val winRate = if (totalTrades > 0) {
        (this.successfulTrades.toDouble() / totalTrades) * 100
    } else {
        0.0
    }
    
    val averageProfit = if (successfulTrades > 0) {
        this.totalProfit / successfulTrades.toBigDecimal()
    } else {
        java.math.BigDecimal.ZERO
    }
    
    val averageLoss = if (failedTrades > 0) {
        this.totalLoss / failedTrades.toBigDecimal()
    } else {
        java.math.BigDecimal.ZERO
    }
    
    val profitFactor = if (totalLoss != java.math.BigDecimal.ZERO && totalLoss.abs() > java.math.BigDecimal.ZERO) {
        this.totalProfit.divide(totalLoss.abs(), 2, java.math.RoundingMode.HALF_UP)
    } else {
        java.math.BigDecimal.ZERO
    }
    
    return TradeStatisticsDTO(
        aiTraderId = aiTraderId,
        totalTrades = totalTrades,
        openTrades = openTrades,
        closedTrades = closedTrades,
        winningTrades = this.successfulTrades.toLong(),
        losingTrades = this.failedTrades.toLong(),
        winRate = winRate,
        totalProfit = this.totalProfit,
        totalLoss = this.totalLoss,
        netProfitLoss = this.netProfitLoss,
        averageProfit = averageProfit,
        averageLoss = averageLoss,
        largestWin = this.bestTrade,
        largestLoss = this.worstTrade,
        averageTradeDuration = 0L, // TODO: Calculate
        longestTrade = 0L, // TODO: Calculate
        shortestTrade = 0L, // TODO: Calculate
        profitFactor = profitFactor
    )
}

