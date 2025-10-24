package com.fmps.autotrader.core.database.repositories

import com.fmps.autotrader.core.database.DatabaseFactory.dbQuery
import com.fmps.autotrader.core.database.schema.TradesTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Repository for Trade operations
 * 
 * Provides CRUD operations for managing trade history
 */
class TradeRepository {
    private val logger = LoggerFactory.getLogger(TradeRepository::class.java)
    
    /**
     * Create a new trade (entry)
     */
    suspend fun create(
        aiTraderId: Int,
        tradeType: String,
        exchange: String,
        tradingPair: String,
        leverage: Int,
        entryPrice: BigDecimal,
        entryAmount: BigDecimal,
        stopLossPrice: BigDecimal,
        takeProfitPrice: BigDecimal,
        entryOrderId: String? = null,
        rsiValue: BigDecimal? = null,
        macdValue: BigDecimal? = null,
        smaShortValue: BigDecimal? = null,
        smaLongValue: BigDecimal? = null,
        patternId: Int? = null
    ): Int = dbQuery {
        TradesTable.insert {
            it[TradesTable.aiTraderId] = aiTraderId
            it[TradesTable.tradeType] = tradeType
            it[TradesTable.exchange] = exchange
            it[TradesTable.tradingPair] = tradingPair
            it[TradesTable.leverage] = leverage
            it[TradesTable.entryPrice] = entryPrice
            it[TradesTable.entryAmount] = entryAmount
            it[entryTimestamp] = LocalDateTime.now()
            it[TradesTable.entryOrderId] = entryOrderId
            it[TradesTable.stopLossPrice] = stopLossPrice
            it[TradesTable.takeProfitPrice] = takeProfitPrice
            it[TradesTable.rsiValue] = rsiValue
            it[TradesTable.macdValue] = macdValue
            it[TradesTable.smaShortValue] = smaShortValue
            it[TradesTable.smaLongValue] = smaLongValue
            it[TradesTable.patternId] = patternId
            it[status] = "OPEN"
        }[TradesTable.id]
    }
    
    /**
     * Close a trade (exit)
     */
    suspend fun close(
        tradeId: Int,
        exitPrice: BigDecimal,
        exitAmount: BigDecimal,
        exitReason: String,
        exitOrderId: String? = null,
        fees: BigDecimal = BigDecimal.ZERO
    ): Boolean = dbQuery {
        // Get the trade to calculate P&L
        val trade = TradesTable.selectAll()
            .where { TradesTable.id eq tradeId }
            .singleOrNull()
            ?: return@dbQuery false
        
        val entryPrice = trade[TradesTable.entryPrice]
        val entryAmount = trade[TradesTable.entryAmount]
        val tradeType = trade[TradesTable.tradeType]
        val leverage = trade[TradesTable.leverage]
        
        // Calculate P&L
        val (profitLoss, profitLossPercent) = calculateProfitLoss(
            tradeType = tradeType,
            entryPrice = entryPrice,
            exitPrice = exitPrice,
            amount = exitAmount,
            leverage = leverage,
            fees = fees
        )
        
        val updated = TradesTable.update({ TradesTable.id eq tradeId }) {
            it[TradesTable.exitPrice] = exitPrice
            it[TradesTable.exitAmount] = exitAmount
            it[exitTimestamp] = LocalDateTime.now()
            it[TradesTable.exitOrderId] = exitOrderId
            it[TradesTable.exitReason] = exitReason
            it[TradesTable.profitLoss] = profitLoss
            it[TradesTable.profitLossPercent] = profitLossPercent
            it[TradesTable.fees] = fees
            it[status] = "CLOSED"
            it[updatedAt] = LocalDateTime.now()
        }
        
        updated > 0
    }
    
    /**
     * Update stop loss for a trade
     */
    suspend fun updateStopLoss(tradeId: Int, newStopLoss: BigDecimal, trailingActivated: Boolean = false): Boolean = dbQuery {
        val updated = TradesTable.update({ TradesTable.id eq tradeId }) {
            it[stopLossPrice] = newStopLoss
            it[trailingStopActivated] = trailingActivated
            it[updatedAt] = LocalDateTime.now()
        }
        updated > 0
    }
    
    /**
     * Find trade by ID
     */
    suspend fun findById(id: Int): Trade? = dbQuery {
        TradesTable.selectAll()
            .where { TradesTable.id eq id }
            .map { rowToTrade(it) }
            .singleOrNull()
    }
    
    /**
     * Find all trades for an AI trader
     */
    suspend fun findByAITrader(aiTraderId: Int): List<Trade> = dbQuery {
        TradesTable.selectAll()
            .where { TradesTable.aiTraderId eq aiTraderId }
            .orderBy(TradesTable.entryTimestamp to SortOrder.DESC)
            .map { rowToTrade(it) }
    }
    
    /**
     * Find open trades for an AI trader
     */
    suspend fun findOpenTrades(aiTraderId: Int): List<Trade> = dbQuery {
        TradesTable.selectAll()
            .where { 
                (TradesTable.aiTraderId eq aiTraderId) and (TradesTable.status eq "OPEN")
            }
            .orderBy(TradesTable.entryTimestamp to SortOrder.DESC)
            .map { rowToTrade(it) }
    }
    
    /**
     * Find closed trades for an AI trader
     */
    suspend fun findClosedTrades(aiTraderId: Int, limit: Int = 100): List<Trade> = dbQuery {
        TradesTable.selectAll()
            .where { 
                (TradesTable.aiTraderId eq aiTraderId) and (TradesTable.status eq "CLOSED")
            }
            .orderBy(TradesTable.exitTimestamp to SortOrder.DESC)
            .limit(limit)
            .map { rowToTrade(it) }
    }
    
    /**
     * Find successful trades (profit > 0)
     */
    suspend fun findSuccessfulTrades(aiTraderId: Int? = null, limit: Int = 100): List<Trade> = dbQuery {
        val query = TradesTable.selectAll()
            .where { 
                (TradesTable.status eq "CLOSED") and 
                (TradesTable.profitLoss greater BigDecimal.ZERO)
            }
        
        if (aiTraderId != null) {
            query.andWhere { TradesTable.aiTraderId eq aiTraderId }
        }
        
        query.orderBy(TradesTable.profitLoss to SortOrder.DESC)
            .limit(limit)
            .map { rowToTrade(it) }
    }
    
    /**
     * Get trade statistics for an AI trader
     */
    suspend fun getStatistics(aiTraderId: Int): TradeStatistics = dbQuery {
        val allTrades = TradesTable.selectAll()
            .where { 
                (TradesTable.aiTraderId eq aiTraderId) and (TradesTable.status eq "CLOSED")
            }
        
        val totalTrades = allTrades.count()
        if (totalTrades == 0L) {
            return@dbQuery TradeStatistics(
                totalTrades = 0,
                successfulTrades = 0,
                failedTrades = 0,
                successRate = BigDecimal.ZERO,
                totalProfit = BigDecimal.ZERO,
                totalLoss = BigDecimal.ZERO,
                netProfitLoss = BigDecimal.ZERO,
                averageProfitLoss = BigDecimal.ZERO,
                bestTrade = BigDecimal.ZERO,
                worstTrade = BigDecimal.ZERO
            )
        }
        
        var successfulTrades = 0
        var failedTrades = 0
        var totalProfit = BigDecimal.ZERO
        var totalLoss = BigDecimal.ZERO
        var bestTrade = BigDecimal.ZERO
        var worstTrade = BigDecimal.ZERO
        
        allTrades.forEach { trade ->
            val profitLoss = trade[TradesTable.profitLoss] ?: BigDecimal.ZERO
            
            if (profitLoss > BigDecimal.ZERO) {
                successfulTrades++
                totalProfit += profitLoss
                if (profitLoss > bestTrade) bestTrade = profitLoss
            } else {
                failedTrades++
                totalLoss += profitLoss
                if (profitLoss < worstTrade) worstTrade = profitLoss
            }
        }
        
        val netProfitLoss = totalProfit + totalLoss
        val averageProfitLoss = netProfitLoss.divide(totalTrades.toBigDecimal(), 6, BigDecimal.ROUND_HALF_UP)
        val successRate = (successfulTrades.toBigDecimal().divide(totalTrades.toBigDecimal(), 4, BigDecimal.ROUND_HALF_UP)) * BigDecimal("100")
        
        TradeStatistics(
            totalTrades = totalTrades.toInt(),
            successfulTrades = successfulTrades,
            failedTrades = failedTrades,
            successRate = successRate,
            totalProfit = totalProfit,
            totalLoss = totalLoss,
            netProfitLoss = netProfitLoss,
            averageProfitLoss = averageProfitLoss,
            bestTrade = bestTrade,
            worstTrade = worstTrade
        )
    }
    
    /**
     * Calculate profit/loss for a trade
     */
    private fun calculateProfitLoss(
        tradeType: String,
        entryPrice: BigDecimal,
        exitPrice: BigDecimal,
        amount: BigDecimal,
        leverage: Int,
        fees: BigDecimal
    ): Pair<BigDecimal, BigDecimal> {
        val priceDiff = if (tradeType == "LONG") {
            exitPrice - entryPrice
        } else {
            entryPrice - exitPrice
        }
        
        val profitLoss = (priceDiff * amount * leverage.toBigDecimal()) - fees
        val profitLossPercent = (priceDiff / entryPrice) * BigDecimal("100") * leverage.toBigDecimal()
        
        return Pair(profitLoss, profitLossPercent)
    }
    
    /**
     * Convert database row to Trade data class
     */
    private fun rowToTrade(row: ResultRow): Trade =
        Trade(
            id = row[TradesTable.id],
            aiTraderId = row[TradesTable.aiTraderId],
            tradeType = row[TradesTable.tradeType],
            exchange = row[TradesTable.exchange],
            tradingPair = row[TradesTable.tradingPair],
            leverage = row[TradesTable.leverage],
            entryPrice = row[TradesTable.entryPrice],
            entryAmount = row[TradesTable.entryAmount],
            entryTimestamp = row[TradesTable.entryTimestamp],
            entryOrderId = row[TradesTable.entryOrderId],
            exitPrice = row[TradesTable.exitPrice],
            exitAmount = row[TradesTable.exitAmount],
            exitTimestamp = row[TradesTable.exitTimestamp],
            exitOrderId = row[TradesTable.exitOrderId],
            exitReason = row[TradesTable.exitReason],
            profitLoss = row[TradesTable.profitLoss],
            profitLossPercent = row[TradesTable.profitLossPercent],
            fees = row[TradesTable.fees],
            stopLossPrice = row[TradesTable.stopLossPrice],
            takeProfitPrice = row[TradesTable.takeProfitPrice],
            trailingStopActivated = row[TradesTable.trailingStopActivated],
            rsiValue = row[TradesTable.rsiValue],
            macdValue = row[TradesTable.macdValue],
            smaShortValue = row[TradesTable.smaShortValue],
            smaLongValue = row[TradesTable.smaLongValue],
            status = row[TradesTable.status],
            notes = row[TradesTable.notes],
            patternId = row[TradesTable.patternId],
            createdAt = row[TradesTable.createdAt],
            updatedAt = row[TradesTable.updatedAt]
        )
}

/**
 * Data class representing a Trade
 */
data class Trade(
    val id: Int,
    val aiTraderId: Int,
    val tradeType: String,
    val exchange: String,
    val tradingPair: String,
    val leverage: Int,
    val entryPrice: BigDecimal,
    val entryAmount: BigDecimal,
    val entryTimestamp: LocalDateTime,
    val entryOrderId: String?,
    val exitPrice: BigDecimal?,
    val exitAmount: BigDecimal?,
    val exitTimestamp: LocalDateTime?,
    val exitOrderId: String?,
    val exitReason: String?,
    val profitLoss: BigDecimal?,
    val profitLossPercent: BigDecimal?,
    val fees: BigDecimal,
    val stopLossPrice: BigDecimal,
    val takeProfitPrice: BigDecimal,
    val trailingStopActivated: Boolean,
    val rsiValue: BigDecimal?,
    val macdValue: BigDecimal?,
    val smaShortValue: BigDecimal?,
    val smaLongValue: BigDecimal?,
    val status: String,
    val notes: String?,
    val patternId: Int?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * Data class for trade statistics
 */
data class TradeStatistics(
    val totalTrades: Int,
    val successfulTrades: Int,
    val failedTrades: Int,
    val successRate: BigDecimal,
    val totalProfit: BigDecimal,
    val totalLoss: BigDecimal,
    val netProfitLoss: BigDecimal,
    val averageProfitLoss: BigDecimal,
    val bestTrade: BigDecimal,
    val worstTrade: BigDecimal
)

