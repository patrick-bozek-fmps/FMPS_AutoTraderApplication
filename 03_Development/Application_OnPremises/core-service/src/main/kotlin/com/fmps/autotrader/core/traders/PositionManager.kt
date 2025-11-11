package com.fmps.autotrader.core.traders

import com.fmps.autotrader.core.connectors.IExchangeConnector
import com.fmps.autotrader.core.database.repositories.TradeRepository
import com.fmps.autotrader.shared.enums.OrderType
import com.fmps.autotrader.shared.enums.TradeAction
import com.fmps.autotrader.shared.enums.TradeStatus
import com.fmps.autotrader.shared.model.Order
import com.fmps.autotrader.shared.model.Position
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.math.abs
import com.fmps.autotrader.core.telemetry.PositionTelemetryStatus
import com.fmps.autotrader.core.telemetry.TelemetryCollector

private val logger = KotlinLogging.logger {}

/**
 * Exception thrown when position operation fails.
 */
class PositionException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Represents a managed position with additional metadata.
 *
 * Extends the base Position model with position ID, trader ID, stop-loss, and take-profit.
 */
data class ManagedPosition(
    val positionId: String,
    val traderId: String,
    val tradeId: Int? = null, // Database trade ID
    val position: Position,
    val stopLossPrice: BigDecimal? = null,
    val takeProfitPrice: BigDecimal? = null,
    val trailingStopActivated: Boolean = false,
    val trailingStopDistance: BigDecimal? = null,
    val trailingStopReferencePrice: BigDecimal? = null,
    val lastUpdated: Instant = Instant.now()
) {
    /**
     * Check if stop-loss is triggered.
     */
    fun isStopLossTriggered(): Boolean {
        if (stopLossPrice == null) return false
        
        return when (position.action) {
            TradeAction.LONG -> position.currentPrice <= stopLossPrice
            TradeAction.SHORT -> position.currentPrice >= stopLossPrice
        }
    }
    
    /**
     * Check if take-profit is reached.
     */
    fun isTakeProfitReached(): Boolean {
        if (takeProfitPrice == null) return false
        
        return when (position.action) {
            TradeAction.LONG -> position.currentPrice >= takeProfitPrice
            TradeAction.SHORT -> position.currentPrice <= takeProfitPrice
        }
    }
}

/**
 * Manages trading positions, including tracking, P&L calculation, stop-loss execution, and position history.
 *
 * This manager:
 * - Tracks open positions with real-time updates
 * - Calculates unrealized and realized P&L
 * - Executes stop-loss orders automatically
 * - Maintains position history
 * - Persists positions to database
 * - Recovers positions on restart
 *
 * ## Thread Safety
 * All operations are thread-safe using Mutex for concurrent access.
 *
 * ## Usage
 * ```kotlin
 * val manager = PositionManager(exchangeConnector, tradeRepository)
 * val position = manager.openPosition(signal, traderId).getOrThrow()
 * manager.updatePosition(position.positionId, currentPrice)
 * manager.closePosition(position.positionId, "TakeProfit")
 * ```
 *
 * @property exchangeConnector Exchange connector for order execution and price updates
 * @property tradeRepository Database repository for position persistence
 * @property updateInterval Interval for periodic position updates (default: 5 seconds)
 *
 * @since 1.0.0
 */
class PositionManager(
    private val exchangeConnector: IExchangeConnector,
    private val tradeRepository: TradeRepository,
    private val updateInterval: Duration = Duration.ofSeconds(5),
    positionPersistence: PositionPersistence? = null
) : RiskPositionProvider {
    // Active positions map: position ID -> ManagedPosition
    private val activePositions = mutableMapOf<String, ManagedPosition>()
    private val positionsMutex = Mutex()
    
    // Position history (in-memory cache)
    private val positionHistory = mutableListOf<PositionHistory>()
    private val historyMutex = Mutex()
    
    // Position persistence helper
    private val positionPersistence = positionPersistence ?: PositionPersistence(tradeRepository)
    
    // Risk management integration
    private var riskManager: RiskManager? = null

    // Background monitoring job
    private var monitoringJob: Job? = null
    private val managerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    init {
        logger.info { "Initialized PositionManager (update interval: $updateInterval)" }
    }

    private suspend fun emitPositionTelemetry(
        position: ManagedPosition,
        status: PositionTelemetryStatus,
        reason: String?,
        realizedPnL: BigDecimal? = null
    ) {
        runCatching {
            TelemetryCollector.publishPositionSnapshot(position, status, reason, realizedPnL)
        }.onFailure {
            logger.debug(it) { "Failed to publish position telemetry for ${position.positionId}" }
        }
    }

    private suspend fun emitMarketData(symbol: String, price: BigDecimal, source: String) {
        runCatching {
            TelemetryCollector.publishMarketData(symbol, price, source)
        }.onFailure {
            logger.debug(it) { "Failed to publish market data telemetry for $symbol" }
        }
    }
    
    /**
     * Open a new position from a trading signal.
     *
     * @param signal Trading signal (must be BUY or SELL)
     * @param traderId Trader ID that generated the signal
     * @param symbol Trading pair symbol (e.g., "BTCUSDT")
     * @param quantity Position quantity (optional, calculated from signal if not provided)
     * @param stopLossPrice Stop-loss price (optional)
     * @param takeProfitPrice Take-profit price (optional)
     * @return Result containing ManagedPosition on success, or error on failure
     */
    suspend fun openPosition(
        signal: TradingSignal,
        traderId: String,
        symbol: String,
        quantity: BigDecimal? = null,
        stopLossPrice: BigDecimal? = null,
        takeProfitPrice: BigDecimal? = null
    ): Result<ManagedPosition> {
        // Validate signal
        if (!signal.isActionable()) {
            return Result.failure(
                PositionException("Signal must be BUY or SELL, got: ${signal.action}")
            )
        }

        val tradeAction = when (signal.action) {
            SignalAction.BUY -> TradeAction.LONG
            SignalAction.SELL -> TradeAction.SHORT
            else -> return Result.failure(
                PositionException("Invalid signal action: ${signal.action}")
            )
        }

        val ticker = exchangeConnector.getTicker(symbol)
        val currentPrice = ticker.lastPrice
        val positionQuantity = quantity ?: calculatePositionSize(signal, currentPrice)
        val leverage = BigDecimal.ONE

        riskManager?.let { manager ->
            val notional = positionQuantity.multiply(currentPrice)
            val result = manager.canOpenPosition(traderId, notional, leverage)
            if (result.isFailure) {
                val exception = result.exceptionOrNull()
                return Result.failure(
                    PositionException("Risk validation failed: ${exception?.message ?: "unknown error"}", exception)
                )
            }
            if (result.getOrDefault(false).not()) {
                return Result.failure(
                    PositionException("Risk limits prevented opening position for trader: $traderId")
                )
            }
        }

        return positionsMutex.withLock {
            try {
                val order = Order(
                    symbol = symbol,
                    action = tradeAction,
                    type = OrderType.MARKET,
                    quantity = positionQuantity,
                    price = null
                )

                val placedOrder = exchangeConnector.placeOrder(order)

                if (placedOrder.status != TradeStatus.FILLED && placedOrder.status != TradeStatus.PARTIALLY_FILLED) {
                    return Result.failure(
                        PositionException("Order not filled: ${placedOrder.status}")
                    )
                }

                val entryPrice = placedOrder.averagePrice ?: currentPrice

                val position = Position(
                    symbol = order.symbol,
                    action = tradeAction,
                    quantity = placedOrder.filledQuantity,
                    entryPrice = entryPrice,
                    currentPrice = entryPrice,
                    unrealizedPnL = BigDecimal.ZERO,
                    leverage = leverage,
                    openedAt = Instant.now()
                )

                val positionId = UUID.randomUUID().toString()

                val managedPosition = ManagedPosition(
                    positionId = positionId,
                    traderId = traderId,
                    tradeId = null,
                    position = position,
                    stopLossPrice = stopLossPrice,
                    takeProfitPrice = takeProfitPrice
                )

                val tradeParams = positionPersistence.positionToTradeParams(
                    position = position,
                    traderId = parseTraderId(traderId),
                    exchange = exchangeConnector.getExchange().name,
                    stopLossPrice = stopLossPrice,
                    takeProfitPrice = takeProfitPrice,
                    trailingStopActivated = managedPosition.trailingStopActivated,
                    orderId = placedOrder.id
                )

                val tradeId = positionPersistence.savePosition(tradeParams)
                val persistedManagedPosition = managedPosition.copy(tradeId = tradeId)
                activePositions[positionId] = persistedManagedPosition

                logger.info { "Opened position: $positionId (${position.symbol} ${position.action})" }
                emitPositionTelemetry(persistedManagedPosition, PositionTelemetryStatus.OPEN, "OPENED")
                emitMarketData(position.symbol, position.currentPrice, "position-open")
                Result.success(persistedManagedPosition)
            } catch (e: Exception) {
                logger.error(e) { "Failed to open position for trader: $traderId" }
                Result.failure(PositionException("Failed to open position: ${e.message}", e))
            }
        }
    }
    
    /**
     * Update position with current market price.
     *
     * @param positionId Position ID
     * @param currentPrice Current market price (optional, fetched if not provided)
     * @return Result indicating success or failure
     */
    suspend fun updatePosition(
        positionId: String,
        currentPrice: BigDecimal? = null
    ): Result<Unit> {
        return positionsMutex.withLock {
            try {
                val managedPosition = activePositions[positionId]
                    ?: return Result.failure(IllegalArgumentException("Position not found: $positionId"))
                
                // Get current price if not provided
                val price = currentPrice ?: exchangeConnector.getTicker(managedPosition.position.symbol).lastPrice
                
                // Calculate new P&L
                val newPnL = calculatePnL(managedPosition.position.copy(currentPrice = price))
                
                // Update position
                val updatedPosition = managedPosition.position.copy(
                    currentPrice = price,
                    unrealizedPnL = newPnL
                )
                
                // Update managed position
                var updatedManagedPosition = managedPosition.copy(
                    position = updatedPosition,
                    lastUpdated = Instant.now()
                )

                updatedManagedPosition = applyTrailingStopIfNeeded(positionId, managedPosition, updatedManagedPosition)
                
                activePositions[positionId] = updatedManagedPosition

                emitPositionTelemetry(updatedManagedPosition, PositionTelemetryStatus.UPDATED, "MARKET_TICK")
                emitMarketData(updatedManagedPosition.position.symbol, price, "position-update")
                
                // Persist update (periodic, not every update)
                // Full persistence happens on close
                
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Failed to update position: $positionId" }
                Result.failure(PositionException("Failed to update position: ${e.message}", e))
            }
        }
    }
    
    /**
     * Close a position.
     *
     * @param positionId Position ID
     * @param reason Close reason (StopLoss, TakeProfit, Manual, Signal, Error)
     * @return Result containing closed position on success, or error on failure
     */
    override suspend fun closePosition(
        positionId: String,
        reason: String
    ): Result<ManagedPosition> {
        return positionsMutex.withLock {
            try {
                val managedPosition = activePositions[positionId]
                    ?: return Result.failure(IllegalArgumentException("Position not found: $positionId"))
                
                val position = managedPosition.position
                
                // Get current price
                val currentPrice = exchangeConnector.getTicker(position.symbol).lastPrice
                
                // Create close order (opposite direction)
                val closeAction = when (position.action) {
                    TradeAction.LONG -> TradeAction.SHORT
                    TradeAction.SHORT -> TradeAction.LONG
                }
                
                val closeOrder = Order(
                    symbol = position.symbol,
                    action = closeAction,
                    type = OrderType.MARKET,
                    quantity = position.quantity,
                    price = null // Market order
                )
                
                // Place close order
                val placedOrder = exchangeConnector.placeOrder(closeOrder)

                // Calculate final P&L
                val exitPrice = placedOrder.averagePrice ?: currentPrice
                val finalPnL = calculatePnL(position.copy(currentPrice = exitPrice))

                val normalizedReason = reason.uppercase()
                var realizedPnL = finalPnL
                if (managedPosition.tradeId != null) {
                    val closedTrade = positionPersistence.closePosition(
                        tradeId = managedPosition.tradeId,
                        exitPrice = exitPrice,
                        exitAmount = placedOrder.filledQuantity,
                        exitReason = normalizedReason,
                        exitOrderId = placedOrder.id
                    )

                    if (closedTrade == null) {
                        throw PositionException("Failed to persist trade closure for position $positionId")
                    }

                    if (closedTrade.profitLoss != null) {
                        realizedPnL = closedTrade.profitLoss
                    }
                }

                // Create position history entry
                val historyEntry = PositionHistory(
                    positionId = managedPosition.tradeId?.toString() ?: positionId,
                    traderId = managedPosition.traderId,
                    symbol = position.symbol,
                    action = position.action,
                    entryPrice = position.entryPrice,
                    closePrice = exitPrice,
                    quantity = position.quantity,
                    realizedPnL = realizedPnL,
                    openedAt = position.openedAt,
                    closedAt = Instant.now(),
                    closeReason = normalizedReason,
                    duration = Duration.between(position.openedAt, Instant.now())
                )
                
                // Add to history
                historyMutex.withLock {
                    positionHistory.add(historyEntry)
                }
                
                // Remove from active positions and return closed view
                activePositions.remove(positionId)
                val closedManagedPosition = managedPosition.copy(
                    position = position.copy(currentPrice = exitPrice, unrealizedPnL = finalPnL),
                    lastUpdated = Instant.now()
                )

                logger.info { "Closed position: $positionId (reason: $normalizedReason, P&L: $realizedPnL)" }
                emitPositionTelemetry(closedManagedPosition, PositionTelemetryStatus.CLOSED, normalizedReason, realizedPnL)
                emitMarketData(position.symbol, exitPrice, "position-close")
                Result.success(closedManagedPosition)
            } catch (e: Exception) {
                logger.error(e) { "Failed to close position: $positionId" }
                Result.failure(PositionException("Failed to close position: ${e.message}", e))
            }
        }
    }
    
    /**
     * Get position by ID.
     *
     * @param positionId Position ID
     * @return ManagedPosition if found, null otherwise
     */
    suspend fun getPosition(positionId: String): ManagedPosition? {
        return positionsMutex.withLock {
            activePositions[positionId]
        }
    }
    
    /**
     * Get all positions for a trader.
     *
     * @param traderId Trader ID
     * @return List of managed positions for the trader
     */
    override suspend fun getPositionsByTrader(traderId: String): List<ManagedPosition> {
        return positionsMutex.withLock {
            activePositions.values.filter { it.traderId == traderId }
        }
    }

    /**
     * Get position by trading pair symbol.
     */
    suspend fun getPositionBySymbol(symbol: String): ManagedPosition? {
        return positionsMutex.withLock {
            activePositions.values.firstOrNull { it.position.symbol == symbol }
        }
    }

    /**
     * Get total number of active positions.
     */
    suspend fun getPositionCount(): Int {
        return positionsMutex.withLock { activePositions.size }
    }
    
    /**
     * Get all active positions.
     *
     * @return List of all active managed positions
     */
    override suspend fun getAllPositions(): List<ManagedPosition> {
        return positionsMutex.withLock {
            activePositions.values.toList()
        }
    }
    
    /**
     * Calculate P&L for a position.
     *
     * @param position Position to calculate P&L for
     * @return Calculated P&L (positive for profit, negative for loss)
     */
    fun calculatePnL(position: Position): BigDecimal {
        val priceDiff = when (position.action) {
            TradeAction.LONG -> position.currentPrice - position.entryPrice
            TradeAction.SHORT -> position.entryPrice - position.currentPrice
        }
        
        return priceDiff * position.quantity * position.leverage
    }

    private fun calculateTrailingDistance(position: Position, stopLoss: BigDecimal?): BigDecimal? {
        val stop = stopLoss ?: return null
        val distance = when (position.action) {
            TradeAction.LONG -> position.currentPrice - stop
            TradeAction.SHORT -> stop - position.currentPrice
        }
        return distance.takeIf { it.compareTo(BigDecimal.ZERO) > 0 }
    }

    private suspend fun applyTrailingStopIfNeeded(
        positionId: String,
        previous: ManagedPosition,
        current: ManagedPosition
    ): ManagedPosition {
        if (!current.trailingStopActivated) return current
        val distance = current.trailingStopDistance ?: return current

        val anchor = current.trailingStopReferencePrice
            ?: previous.trailingStopReferencePrice
            ?: previous.position.currentPrice

        val price = current.position.currentPrice
        var newAnchor = anchor
        var newStop: BigDecimal? = null

        when (current.position.action) {
            TradeAction.LONG -> if (price > anchor) {
                newAnchor = price
                newStop = price - distance
            }

            TradeAction.SHORT -> if (price < anchor) {
                newAnchor = price
                newStop = price + distance
            }
        }

        if (newStop != null && newStop.compareTo(BigDecimal.ZERO) > 0) {
            if (current.stopLossPrice == null || current.stopLossPrice.compareTo(newStop) != 0) {
                current.tradeId?.let { tradeId ->
                    positionPersistence.updateStopLoss(tradeId, newStop, true)
                }
                logger.debug {
                    "Trailing stop adjusted for $positionId to $newStop (anchor $newAnchor, distance $distance)"
                }
                return current.copy(
                    stopLossPrice = newStop,
                    trailingStopReferencePrice = newAnchor,
                    lastUpdated = Instant.now()
                )
            }
        }

        if (current.trailingStopReferencePrice == null && newAnchor != anchor) {
            return current.copy(trailingStopReferencePrice = newAnchor)
        }

        return current
    }
    
    /**
     * Check if stop-loss is triggered for a position.
     *
     * @param positionId Position ID
     * @return true if stop-loss triggered, false otherwise
     */
    suspend fun checkStopLoss(positionId: String): Boolean {
        return positionsMutex.withLock {
            val managedPosition = activePositions[positionId] ?: return false
            managedPosition.isStopLossTriggered()
        }
    }

    /**
     * Update stop-loss for an active position.
     */
    suspend fun updateStopLoss(
        positionId: String,
        newStopLoss: BigDecimal,
        trailingActivated: Boolean = false
    ): Result<Unit> {
        return positionsMutex.withLock {
            val managedPosition = activePositions[positionId]
                ?: return Result.failure(IllegalArgumentException("Position not found: $positionId"))

            val trailingDistance = if (trailingActivated) {
                calculateTrailingDistance(managedPosition.position, newStopLoss)
                    ?: return Result.failure(PositionException("Trailing stop requires stop-loss to respect current price movement"))
            } else {
                null
            }

            if (managedPosition.tradeId != null) {
                val updated = positionPersistence.updateStopLoss(managedPosition.tradeId, newStopLoss, trailingActivated)
                if (!updated) {
                    return Result.failure(PositionException("Failed to update stop-loss in repository"))
                }
            }

            val updatedManaged = managedPosition.copy(
                stopLossPrice = newStopLoss,
                trailingStopActivated = trailingActivated,
                trailingStopDistance = trailingDistance,
                trailingStopReferencePrice = if (trailingActivated) managedPosition.position.currentPrice else null,
                lastUpdated = Instant.now()
            )

            activePositions[positionId] = updatedManaged
            logger.info {
                if (trailingActivated) {
                    "Updated stop-loss for position $positionId to $newStopLoss (trailing enabled with distance $trailingDistance)"
                } else {
                    "Updated stop-loss for position $positionId to $newStopLoss"
                }
            }
            Result.success(Unit)
        }
    }

    /**
     * Update take-profit for an active position.
     */
    suspend fun updateTakeProfit(positionId: String, newTakeProfit: BigDecimal): Result<Unit> {
        return positionsMutex.withLock {
            val managedPosition = activePositions[positionId]
                ?: return Result.failure(IllegalArgumentException("Position not found: $positionId"))

            if (managedPosition.tradeId != null) {
                val updated = positionPersistence.updateTakeProfit(managedPosition.tradeId, newTakeProfit)
                if (!updated) {
                    return Result.failure(PositionException("Failed to update take-profit in repository"))
                }
            }

            activePositions[positionId] = managedPosition.copy(takeProfitPrice = newTakeProfit)
            logger.info { "Updated take-profit for position $positionId to $newTakeProfit" }
            Result.success(Unit)
        }
    }

    /**
     * Refresh position state from the exchange.
     */
    suspend fun refreshPosition(positionId: String): Result<ManagedPosition> =
        positionsMutex.withLock {
            val managedPosition = activePositions[positionId]
                ?: return@withLock Result.failure(IllegalArgumentException("Position not found: $positionId"))

            try {
                val exchangePosition = exchangeConnector.getPosition(managedPosition.position.symbol)
                    ?: return@withLock Result.failure(PositionException("Exchange position not found for symbol ${managedPosition.position.symbol}"))

                val ticker = exchangeConnector.getTicker(managedPosition.position.symbol)
                val recalculatedPnL = calculatePnL(
                    managedPosition.position.copy(
                        currentPrice = ticker.lastPrice,
                        quantity = exchangePosition.quantity,
                        entryPrice = exchangePosition.entryPrice
                    )
                )

                val updatedPosition = managedPosition.position.copy(
                    quantity = exchangePosition.quantity,
                    entryPrice = exchangePosition.entryPrice,
                    currentPrice = ticker.lastPrice,
                    unrealizedPnL = recalculatedPnL
                )

                val refreshed = managedPosition.copy(position = updatedPosition, lastUpdated = Instant.now())
                activePositions[positionId] = refreshed
                Result.success(refreshed)
            } catch (e: Exception) {
                Result.failure(PositionException("Failed to refresh position: ${e.message}", e))
            }
        }
    
    /**
     * Start position monitoring (periodic updates and stop-loss checks).
     */
    fun startMonitoring() {
        if (monitoringJob?.isActive == true) {
            logger.warn { "Position monitoring already started" }
            return
        }
        
        monitoringJob = managerScope.launch {
            while (isActive) {
                try {
                    val positionsSnapshot = positionsMutex.withLock {
                        activePositions.values.map { it.positionId }
                    }

                    for (positionId in positionsSnapshot) {
                        val updateResult = updatePosition(positionId)
                        if (updateResult.isFailure) continue

                        val latestPosition = getPosition(positionId) ?: continue

                        if (latestPosition.isStopLossTriggered()) {
                            logger.warn { "Stop-loss triggered for position: $positionId" }
                            closePosition(positionId, "STOP_LOSS").getOrNull()
                            continue
                        }

                        if (latestPosition.isTakeProfitReached()) {
                            logger.info { "Take-profit reached for position: $positionId" }
                            closePosition(positionId, "TAKE_PROFIT").getOrNull()
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error during position monitoring" }
                }

                delay(updateInterval.toMillis())
            }
        }
        
        logger.info { "Started position monitoring (interval: $updateInterval)" }
    }
    
    /**
     * Stop position monitoring.
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        logger.info { "Stopped position monitoring" }
    }
    
    /**
     * Recover positions from database on restart.
     *
     * @return Result indicating success or failure
     */
    suspend fun recoverPositions(): Result<Unit> {
        return positionsMutex.withLock {
            try {
                val openTrades = positionPersistence.loadAllActiveTrades()

                logger.info { "Recovering ${openTrades.size} positions from database" }

                for (trade in openTrades) {
                    try {
                        val exchangePosition = exchangeConnector.getPosition(trade.tradingPair)

                        if (exchangePosition != null) {
                            val ticker = exchangeConnector.getTicker(trade.tradingPair)
                            val restoredPosition = positionPersistence.tradeToPosition(trade, ticker.lastPrice)
                            val pnl = calculatePnL(restoredPosition)
                            val updatedPosition = restoredPosition.copy(unrealizedPnL = pnl)

                            val trailingActive = trade.trailingStopActivated
                            val stopLoss = trade.stopLossPrice.takeIf { it > BigDecimal.ZERO }
                            val trailingDistance = if (trailingActive) {
                                calculateTrailingDistance(updatedPosition, stopLoss)
                            } else {
                                null
                            }

                            val managedPosition = ManagedPosition(
                                positionId = UUID.randomUUID().toString(),
                                traderId = trade.aiTraderId.toString(),
                                tradeId = trade.id,
                                position = updatedPosition,
                                stopLossPrice = stopLoss,
                                takeProfitPrice = trade.takeProfitPrice.takeIf { it > BigDecimal.ZERO },
                                trailingStopActivated = trailingActive,
                                trailingStopDistance = trailingDistance,
                                trailingStopReferencePrice = if (trailingActive) updatedPosition.currentPrice else null
                            )

                            activePositions[managedPosition.positionId] = managedPosition

                            logger.info { "Recovered position: ${managedPosition.positionId} (${trade.tradingPair})" }
                            emitPositionTelemetry(managedPosition, PositionTelemetryStatus.OPEN, "RECOVERED")
                            emitMarketData(managedPosition.position.symbol, managedPosition.position.currentPrice, "position-recover")
                        } else {
                            logger.warn { "Position orphaned: trade ${trade.id} (${trade.tradingPair})" }
                            positionPersistence.closePosition(
                                tradeId = trade.id,
                                exitPrice = trade.entryPrice,
                                exitAmount = trade.entryAmount,
                                exitReason = "ORPHANED",
                                exitOrderId = null
                            )
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to recover position: trade ${trade.id}" }
                    }
                }

                logger.info { "Recovery complete: ${activePositions.size} positions recovered" }
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Failed to recover positions" }
                Result.failure(PositionException("Failed to recover positions: ${e.message}", e))
            }
        }
    }
    
    /**
     * Get position history for a trader.
     *
     * @param traderId Trader ID
     * @return List of position history entries
     */
    override suspend fun getHistoryByTrader(traderId: String): List<PositionHistory> {
        val inMemory = historyMutex.withLock {
            positionHistory.filter { it.traderId == traderId }
        }
        val persisted = positionPersistence.loadHistoryByTrader(parseTraderId(traderId))
        return mergeHistoryLists(inMemory, persisted)
    }

    /**
     * Get position history for a symbol.
     *
     * @param symbol Trading pair symbol
     * @return List of position history entries for the symbol
     */
    suspend fun getHistoryBySymbol(symbol: String): List<PositionHistory> {
        val inMemory = historyMutex.withLock {
            positionHistory.filter { it.symbol == symbol }
        }
        val persisted = positionPersistence.loadHistoryBySymbol(symbol)
        return mergeHistoryLists(inMemory, persisted)
    }

    /**
     * Get position history within a date range.
     *
     * @param start Start timestamp (inclusive)
     * @param end End timestamp (inclusive)
     * @return List of position history entries within the date range
     */
    suspend fun getHistoryByDateRange(start: Instant, end: Instant): List<PositionHistory> {
        val inMemory = historyMutex.withLock {
            positionHistory.filter {
                it.closedAt.isAfter(start.minusSeconds(1)) && it.closedAt.isBefore(end.plusSeconds(1))
            }
        }
        val persisted = positionPersistence.loadHistoryByDateRange(start, end)
        return mergeHistoryLists(inMemory, persisted)
    }

    /**
     * Get total realized P&L from history.
     *
     * @return Total realized P&L
     */
    suspend fun getTotalPnL(): BigDecimal {
        val inMemorySnapshot = historyMutex.withLock { positionHistory.toList() }
        val persisted = collectPersistedHistoryForKnownTraders()
        val historyEntries = mergeHistoryLists(inMemorySnapshot, persisted)
        return historyEntries.fold(BigDecimal.ZERO) { acc, entry -> acc + entry.realizedPnL }
    }

    /**
     * Get win rate from history.
     *
     * @return Win rate as percentage (0.0 to 100.0)
     */
    suspend fun getWinRate(): Double {
        val inMemorySnapshot = historyMutex.withLock { positionHistory.toList() }
        val persisted = collectPersistedHistoryForKnownTraders()
        val historyEntries = mergeHistoryLists(inMemorySnapshot, persisted)

        if (historyEntries.isEmpty()) {
            return 0.0
        }

        val winningTrades = historyEntries.count { it.realizedPnL > BigDecimal.ZERO }
        return (winningTrades.toDouble() / historyEntries.size) * 100.0
    }
    
    /**
     * Cleanup resources.
     */
    fun cleanup() {
        stopMonitoring()
        managerScope.cancel()
    }
    
    fun attachRiskManager(riskManager: RiskManager) {
        this.riskManager = riskManager
    }

    // Helper methods
    
    private fun calculatePositionSize(signal: TradingSignal, currentPrice: BigDecimal): BigDecimal {
        val baseSize = BigDecimal("0.001")
        val confidenceFactor = BigDecimal.valueOf(signal.confidence.coerceIn(0.1, 1.0))
        val priceFactor = try {
            BigDecimal("100000").divide(currentPrice.max(BigDecimal.ONE), 6, RoundingMode.HALF_UP)
        } catch (e: ArithmeticException) {
            BigDecimal.ONE
        }
        val calculated = (baseSize * confidenceFactor * priceFactor).setScale(6, RoundingMode.HALF_UP)
        val minimumSize = BigDecimal("0.0001")
        return if (calculated < minimumSize) minimumSize else calculated
    }

    private fun parseTraderId(traderId: String): Int {
        return traderId.toIntOrNull() ?: abs(traderId.hashCode())
    }

    private suspend fun collectPersistedHistoryForKnownTraders(): List<PositionHistory> {
        val traderIds = mutableSetOf<Int>()
        historyMutex.withLock {
            positionHistory.forEach { traderIds.add(parseTraderId(it.traderId)) }
        }
        positionsMutex.withLock {
            activePositions.values.forEach { traderIds.add(parseTraderId(it.traderId)) }
        }
        if (traderIds.isEmpty()) return emptyList()
        return traderIds.flatMap { positionPersistence.loadHistoryByTrader(it) }
    }

    private fun mergeHistoryLists(vararg lists: List<PositionHistory>): List<PositionHistory> {
        val map = LinkedHashMap<String, PositionHistory>()
        lists.forEach { list ->
            list.forEach { entry ->
                map[entry.positionId] = entry
            }
        }
        return map.values.sortedByDescending { it.closedAt }
    }

    // Helper extension function for LocalDateTime to Instant conversion
    private fun java.time.LocalDateTime.toInstant(): Instant {
        return this.atZone(java.time.ZoneId.systemDefault()).toInstant()
    }
}

