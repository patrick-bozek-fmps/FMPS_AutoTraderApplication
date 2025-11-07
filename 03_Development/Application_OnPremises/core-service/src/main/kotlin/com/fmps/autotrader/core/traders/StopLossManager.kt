package com.fmps.autotrader.core.traders

import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Encapsulates stop-loss evaluation logic for positions and traders.
 */
class StopLossManager(
    private val positionProvider: RiskPositionProvider,
    private val riskConfig: RiskConfig,
    private val clock: Clock = Clock.systemUTC()
) {

    fun checkPositionStopLoss(position: ManagedPosition): Boolean {
        return position.isStopLossTriggered()
    }

    suspend fun checkTraderStopLoss(traderId: String): Boolean {
        if (riskConfig.maxDailyLoss <= BigDecimal.ZERO) return false
        val totalPnL = calculateRollingPnL(traderId)
        return totalPnL < riskConfig.maxDailyLoss.negate()
    }

    suspend fun executeStopLoss(traderId: String, reason: String = "STOP_LOSS"): Result<Unit> {
        val positions = positionProvider.getPositionsByTrader(traderId)
        positions.forEach { managedPosition ->
            positionProvider.closePosition(managedPosition.positionId, reason)
        }
        return Result.success(Unit)
    }

    suspend fun calculateRollingPnL(traderId: String): BigDecimal {
        val cutoff = Instant.now(clock).minus(Duration.ofDays(1))
        val realized = positionProvider.getHistoryByTrader(traderId)
            .filter { it.closedAt.isAfter(cutoff) }
            .fold(BigDecimal.ZERO) { acc, entry -> acc + entry.realizedPnL }

        val unrealized = positionProvider.getPositionsByTrader(traderId)
            .fold(BigDecimal.ZERO) { acc, position -> acc + position.position.unrealizedPnL }

        return realized + unrealized
    }
}

