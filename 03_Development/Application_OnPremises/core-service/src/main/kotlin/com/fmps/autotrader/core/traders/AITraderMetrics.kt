package com.fmps.autotrader.core.traders

import com.fmps.autotrader.shared.dto.BigDecimalSerializer
import com.fmps.autotrader.shared.dto.InstantSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

/**
 * Performance metrics for an AI Trader instance.
 *
 * Tracks trading performance including win rate, profit/loss, and other key metrics.
 *
 * @property totalTrades Total number of trades executed
 * @property winningTrades Number of profitable trades
 * @property losingTrades Number of losing trades
 * @property totalProfit Sum of all profitable trades
 * @property totalLoss Sum of all losing trades (absolute value)
 * @property netProfit Total profit minus total loss
 * @property winRate Winning trades / total trades (0.0 to 1.0)
 * @property averageProfit Average profit per winning trade
 * @property averageLoss Average loss per losing trade
 * @property profitFactor Total profit / total loss (if loss > 0, else null)
 * @property maxDrawdown Maximum peak-to-trough decline
 * @property sharpeRatio Risk-adjusted return metric (optional, complex calculation)
 * @property startTime When trading started
 * @property uptime How long the trader has been running
 *
 * @since 1.0.0
 */
@Serializable
data class AITraderMetrics(
    val totalTrades: Int = 0,
    val winningTrades: Int = 0,
    val losingTrades: Int = 0,
    @Serializable(with = BigDecimalSerializer::class)
    val totalProfit: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val totalLoss: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val netProfit: BigDecimal = BigDecimal.ZERO,
    val winRate: Double = 0.0,
    @Serializable(with = BigDecimalSerializer::class)
    val averageProfit: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val averageLoss: BigDecimal = BigDecimal.ZERO,
    val profitFactor: Double? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val maxDrawdown: BigDecimal = BigDecimal.ZERO,
    val sharpeRatio: Double? = null,
    @Serializable(with = InstantSerializer::class)
    val startTime: Instant? = null,
    @Contextual val uptime: Duration = Duration.ZERO
) {
    init {
        require(totalTrades >= 0) { "Total trades cannot be negative" }
        require(winningTrades >= 0) { "Winning trades cannot be negative" }
        require(losingTrades >= 0) { "Losing trades cannot be negative" }
        require(winningTrades + losingTrades <= totalTrades) {
            "Winning + losing trades cannot exceed total trades"
        }
        require(totalProfit >= BigDecimal.ZERO) { "Total profit cannot be negative" }
        require(totalLoss >= BigDecimal.ZERO) { "Total loss cannot be negative" }
        require(winRate >= 0.0 && winRate <= 1.0) {
            "Win rate must be between 0.0 and 1.0, got: $winRate"
        }
    }

    /**
     * Calculate win rate from trades.
     */
    fun calculateWinRate(): Double {
        return if (totalTrades > 0) {
            winningTrades.toDouble() / totalTrades.toDouble()
        } else {
            0.0
        }
    }

    /**
     * Calculate profit factor.
     * Returns null if no losses occurred.
     */
    fun calculateProfitFactor(): Double? {
        return if (totalLoss > BigDecimal.ZERO) {
            totalProfit.toDouble() / totalLoss.toDouble()
        } else {
            null
        }
    }

    /**
     * Calculate average profit per winning trade.
     */
    fun calculateAverageProfit(): BigDecimal {
        return if (winningTrades > 0) {
            totalProfit / BigDecimal.valueOf(winningTrades.toLong())
        } else {
            BigDecimal.ZERO
        }
    }

    /**
     * Calculate average loss per losing trade.
     */
    fun calculateAverageLoss(): BigDecimal {
        return if (losingTrades > 0) {
            totalLoss / BigDecimal.valueOf(losingTrades.toLong())
        } else {
            BigDecimal.ZERO
        }
    }

    companion object {
        /**
         * Create an empty metrics instance.
         */
        fun empty(): AITraderMetrics {
            return AITraderMetrics()
        }

        /**
         * Create metrics with start time set to now.
         */
        fun withStartTime(): AITraderMetrics {
            return AITraderMetrics(startTime = Instant.now())
        }
    }
}

