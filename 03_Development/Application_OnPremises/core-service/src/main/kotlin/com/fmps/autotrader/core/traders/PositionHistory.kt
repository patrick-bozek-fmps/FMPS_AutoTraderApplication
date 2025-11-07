package com.fmps.autotrader.core.traders

import com.fmps.autotrader.shared.enums.TradeAction
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

/**
 * Represents a closed position in the position history.
 *
 * Used for tracking completed positions and calculating performance metrics.
 *
 * @property positionId Unique position identifier
 * @property traderId Trader ID that opened the position
 * @property symbol Trading pair symbol
 * @property action Position action (LONG or SHORT)
 * @property entryPrice Entry price
 * @property closePrice Close price
 * @property quantity Position quantity
 * @property realizedPnL Realized profit/loss
 * @property openedAt When position was opened
 * @property closedAt When position was closed
 * @property closeReason Reason for closing (StopLoss, TakeProfit, Manual, etc.)
 * @property duration Position duration
 *
 * @since 1.0.0
 */
data class PositionHistory(
    val positionId: String,
    val traderId: String,
    val symbol: String,
    val action: TradeAction,
    val entryPrice: BigDecimal,
    val closePrice: BigDecimal,
    val quantity: BigDecimal,
    val realizedPnL: BigDecimal,
    val openedAt: Instant,
    val closedAt: Instant,
    val closeReason: String,
    val duration: Duration
)

