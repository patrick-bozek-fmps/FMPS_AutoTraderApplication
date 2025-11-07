package com.fmps.autotrader.core.traders

import com.fmps.autotrader.core.database.repositories.Trade
import com.fmps.autotrader.core.database.repositories.TradeRepository
import com.fmps.autotrader.shared.enums.TradeAction
import com.fmps.autotrader.shared.model.Position
import mu.KotlinLogging
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private val logger = KotlinLogging.logger {}

/**
 * Handles persistence and recovery of trading positions.
 *
 * Bridges the gap between the database model (Trade from TradeRepository)
 * and the runtime model (Position and ManagedPosition).
 *
 * @property tradeRepository Database repository for trade persistence
 *
 * @since 1.0.0
 */
class PositionPersistence(
    private val tradeRepository: TradeRepository
) {
    /**
     * Persist a newly opened position.
     */
    suspend fun savePosition(params: TradeCreationParams): Int {
        return tradeRepository.create(
            aiTraderId = params.aiTraderId,
            tradeType = params.tradeType,
            exchange = params.exchange,
            tradingPair = params.tradingPair,
            leverage = params.leverage,
            entryPrice = params.entryPrice,
            entryAmount = params.entryAmount,
            stopLossPrice = params.stopLossPrice,
            takeProfitPrice = params.takeProfitPrice,
            entryOrderId = params.entryOrderId
        )
    }

    /**
     * Persist the closing details for a position and return the updated trade.
     */
    suspend fun closePosition(
        tradeId: Int,
        exitPrice: BigDecimal,
        exitAmount: BigDecimal,
        exitReason: String,
        exitOrderId: String?,
        fees: BigDecimal = BigDecimal.ZERO
    ): Trade? {
        val closed = tradeRepository.close(
            tradeId = tradeId,
            exitPrice = exitPrice,
            exitAmount = exitAmount,
            exitReason = exitReason,
            exitOrderId = exitOrderId,
            fees = fees
        )

        if (!closed) {
            logger.warn("Failed to close trade $tradeId in repository")
            return null
        }

        return tradeRepository.findById(tradeId)
    }

    /**
     * Load a single trade by ID.
     */
    suspend fun loadTrade(tradeId: Int): Trade? = tradeRepository.findById(tradeId)

    /**
     * Load all active trades.
     */
    suspend fun loadAllActiveTrades(): List<Trade> = tradeRepository.findAllOpenTrades()

    /**
     * Load active trades for a specific trader.
     */
    suspend fun loadActiveTradesForTrader(traderId: Int): List<Trade> = tradeRepository.findOpenTrades(traderId)

    /**
     * Load closed trades for a trader (for history queries).
     */
    suspend fun loadClosedTradesForTrader(traderId: Int, limit: Int = 100): List<Trade> =
        tradeRepository.findClosedTrades(traderId, limit)

    /**
     * Update stop-loss information in the repository.
     */
    suspend fun updateStopLoss(tradeId: Int, newStopLoss: BigDecimal, trailingActivated: Boolean = false): Boolean =
        tradeRepository.updateStopLoss(tradeId, newStopLoss, trailingActivated)

    /**
     * Update take-profit information in the repository.
     */
    suspend fun updateTakeProfit(tradeId: Int, newTakeProfit: BigDecimal): Boolean =
        tradeRepository.updateTakeProfit(tradeId, newTakeProfit)

    /**
     * Load closed trades for trader and map to history.
     */
    suspend fun loadHistoryByTrader(traderId: Int, limit: Int = 100): List<PositionHistory> {
        return runCatching {
            tradeRepository.findClosedTrades(traderId, limit).mapNotNull { tradeToHistory(it) }
        }.getOrElse {
            logger.warn(it) { "Failed to load history for trader $traderId from repository" }
            emptyList()
        }
    }

    /**
     * Load closed trades for symbol and map to history.
     */
    suspend fun loadHistoryBySymbol(symbol: String, limit: Int = 100): List<PositionHistory> {
        return runCatching {
            tradeRepository.findClosedTradesBySymbol(symbol, limit).mapNotNull { tradeToHistory(it) }
        }.getOrElse {
            logger.warn(it) { "Failed to load history for symbol $symbol from repository" }
            emptyList()
        }
    }

    /**
     * Load closed trades in date range and map to history.
     */
    suspend fun loadHistoryByDateRange(start: Instant, end: Instant): List<PositionHistory> {
        val startDateTime = LocalDateTime.ofInstant(start, ZoneId.systemDefault())
        val endDateTime = LocalDateTime.ofInstant(end, ZoneId.systemDefault())
        return runCatching {
            tradeRepository.findClosedTradesByDateRange(startDateTime, endDateTime).mapNotNull { tradeToHistory(it) }
        }.getOrElse {
            logger.warn(it) { "Failed to load history for date range $start - $end" }
            emptyList()
        }
    }

    /**
     * Convert database Trade to Position.
     */
    fun tradeToPosition(trade: Trade, currentPrice: BigDecimal): Position {
        return Position(
            symbol = trade.tradingPair,
            action = TradeAction.valueOf(trade.tradeType),
            quantity = trade.entryAmount,
            entryPrice = trade.entryPrice,
            currentPrice = currentPrice,
            unrealizedPnL = BigDecimal.ZERO, // Will be recalculated
            leverage = trade.leverage.toBigDecimal(),
            openedAt = trade.entryTimestamp.toInstant()
        )
    }
    
    /**
     * Convert Position to database Trade creation parameters.
     */
    fun positionToTradeParams(
        position: Position,
        traderId: Int,
        exchange: String,
        stopLossPrice: BigDecimal? = null,
        takeProfitPrice: BigDecimal? = null,
        orderId: String? = null
    ): TradeCreationParams {
        return TradeCreationParams(
            aiTraderId = traderId,
            tradeType = position.action.name,
            exchange = exchange,
            tradingPair = position.symbol,
            leverage = position.leverage.toInt(),
            entryPrice = position.entryPrice,
            entryAmount = position.quantity,
            stopLossPrice = stopLossPrice ?: BigDecimal.ZERO,
            takeProfitPrice = takeProfitPrice ?: BigDecimal.ZERO,
            entryOrderId = orderId
        )
    }
    
    /**
     * Helper function to convert LocalDateTime to Instant.
     */
    private fun java.time.LocalDateTime.toInstant(): Instant {
        return this.atZone(java.time.ZoneId.systemDefault()).toInstant()
    }

    private fun tradeToHistory(trade: Trade): PositionHistory? {
        val exitPrice = trade.exitPrice ?: return null
        val exitAmount = trade.exitAmount ?: return null
        val exitTimestamp = trade.exitTimestamp ?: return null

        val realizedPnL = trade.profitLoss ?: BigDecimal.ZERO
        val closeReason = trade.exitReason ?: "UNKNOWN"

        val duration = Duration.between(trade.entryTimestamp.toInstant(), exitTimestamp.toInstant())

        return PositionHistory(
            positionId = trade.id.toString(),
            traderId = trade.aiTraderId.toString(),
            symbol = trade.tradingPair,
            action = TradeAction.valueOf(trade.tradeType),
            entryPrice = trade.entryPrice,
            closePrice = exitPrice,
            quantity = exitAmount,
            realizedPnL = realizedPnL,
            openedAt = trade.entryTimestamp.toInstant(),
            closedAt = exitTimestamp.toInstant(),
            closeReason = closeReason,
            duration = duration
        )
    }
}

/**
 * Parameters for creating a trade in the database.
 */
data class TradeCreationParams(
    val aiTraderId: Int,
    val tradeType: String,
    val exchange: String,
    val tradingPair: String,
    val leverage: Int,
    val entryPrice: BigDecimal,
    val entryAmount: BigDecimal,
    val stopLossPrice: BigDecimal,
    val takeProfitPrice: BigDecimal,
    val entryOrderId: String? = null
)

