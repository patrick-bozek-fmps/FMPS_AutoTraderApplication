package com.fmps.autotrader.core.traders

import com.fmps.autotrader.core.database.repositories.AITraderRepository
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.TimeFrame
import com.fmps.autotrader.shared.model.TradingStrategy
import mu.KotlinLogging
import java.math.BigDecimal
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * Handles persistence and recovery of AI Trader state.
 *
 * Bridges the gap between the database model (AITrader from repository)
 * and the runtime model (AITrader class and AITraderConfig).
 *
 * @property repository Database repository for trader persistence
 *
 * @since 1.0.0
 */
class TraderStatePersistence(
    private val repository: AITraderRepository
) {
    /**
     * Convert database AITrader to AITraderConfig.
     */
    fun dbTraderToConfig(dbTrader: com.fmps.autotrader.core.database.repositories.AITrader): AITraderConfig {
        return AITraderConfig(
            id = dbTrader.id.toString(),
            name = dbTrader.name,
            exchange = Exchange.valueOf(dbTrader.exchange),
            symbol = dbTrader.tradingPair.replace("/", ""), // Convert "BTC/USDT" to "BTCUSDT"
            virtualMoney = true, // v1.0: always true
            maxStakeAmount = dbTrader.currentBalance,
            maxRiskLevel = dbTrader.leverage.coerceIn(1, 10),
            maxTradingDuration = Duration.ofHours(24), // Default, can be made configurable
            minReturnPercent = dbTrader.takeProfitPercent.toDouble() * 100.0,
            strategy = TradingStrategy.TREND_FOLLOWING, // Default, can be made configurable
            candlestickInterval = TimeFrame.ONE_HOUR // Default, can be made configurable
        )
    }

    /**
     * Save trader state to database.
     *
     * @param traderId Trader ID
     * @param state Current trader state
     * @param metrics Current metrics (optional)
     * @return Result indicating success or failure
     */
    suspend fun saveState(
        traderId: String,
        state: AITraderState,
        metrics: AITraderMetrics? = null
    ): Result<Unit> {
        return try {
            val id = traderId.toIntOrNull()
                ?: return Result.failure(IllegalArgumentException("Invalid trader ID: $traderId"))

            // Map AITraderState to database status string
            val dbStatus = when (state) {
                AITraderState.IDLE, AITraderState.STOPPED -> "STOPPED"
                AITraderState.STARTING, AITraderState.RUNNING -> "ACTIVE"
                AITraderState.PAUSED -> "PAUSED"
                AITraderState.STOPPING -> "STOPPED"
                AITraderState.ERROR -> "ERROR"
            }

            val success = repository.updateStatus(id, dbStatus)
            if (success) {
                logger.debug { "Saved state for trader $traderId: $dbStatus" }
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Failed to update trader status"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to save state for trader $traderId" }
            Result.failure(e)
        }
    }

    /**
     * Load trader state from database.
     *
     * @param traderId Trader ID
     * @return AITraderState if found, null otherwise
     */
    suspend fun loadState(traderId: String): AITraderState? {
        return try {
            val id = traderId.toIntOrNull()
                ?: return null

            val dbTrader = repository.findById(id) ?: return null

            // Map database status to AITraderState
            when (dbTrader.status.uppercase()) {
                "STOPPED" -> AITraderState.STOPPED
                "ACTIVE" -> AITraderState.RUNNING // Assume running if active
                "PAUSED" -> AITraderState.PAUSED
                "ERROR" -> AITraderState.ERROR
                else -> AITraderState.IDLE
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to load state for trader $traderId" }
            null
        }
    }

    /**
     * Update trader balance in database.
     */
    suspend fun updateBalance(traderId: String, balance: BigDecimal): Result<Unit> {
        return try {
            val id = traderId.toIntOrNull()
                ?: return Result.failure(IllegalArgumentException("Invalid trader ID: $traderId"))

            val success = repository.updateBalance(id, balance)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Failed to update trader balance"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to update balance for trader $traderId" }
            Result.failure(e)
        }
    }
}



