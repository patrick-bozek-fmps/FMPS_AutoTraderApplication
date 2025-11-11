package com.fmps.autotrader.core.traders

import com.fmps.autotrader.core.connectors.ConnectorFactory
import com.fmps.autotrader.core.connectors.IExchangeConnector
import com.fmps.autotrader.core.database.repositories.AITraderRepository
import com.fmps.autotrader.core.telemetry.TelemetryCollector
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.model.ExchangeConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.math.BigDecimal
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * Exception thrown when maximum trader limit is exceeded.
 */
class MaxTradersExceededException(message: String) : Exception(message)

/**
 * Manages the lifecycle of multiple AI Trader instances.
 *
 * This manager:
 * - Enforces maximum 3 traders limit (v1.0 scope)
 * - Handles creation, starting, stopping, updating, and deletion
 * - Persists trader state to database
 * - Recovers traders on application restart
 * - Monitors trader health
 * - Manages resources (exchange connectors, memory)
 *
 * ## Thread Safety
 * All operations are thread-safe using Mutex for concurrent access.
 *
 * ## Usage
 * ```kotlin
 * val manager = AITraderManager(repository, connectorFactory)
 * val traderId = manager.createTrader(config).getOrThrow()
 * manager.startTrader(traderId).getOrThrow()
 * ```
 *
 * @property repository Database repository for trader persistence
 * @property connectorFactory Factory for creating exchange connectors
 * @property maxTraders Maximum number of traders allowed (default: 3)
 *
 * @since 1.0.0
 */
class AITraderManager(
    private val repository: AITraderRepository,
    private val connectorFactory: ConnectorFactory,
    private val maxTraders: Int = 3,
    riskManager: RiskManager? = null
) {
    // Active traders map: trader ID -> AITrader instance
    private val activeTraders = mutableMapOf<String, AITrader>()
    private val tradersMutex = Mutex()

    // State persistence
    private val statePersistence = TraderStatePersistence(repository)

    // Health monitoring
    private val healthMonitor = HealthMonitor()

    // Exchange connector cache (reuse connectors for same exchange)
    private val connectorCache = mutableMapOf<Exchange, IExchangeConnector>()

    private var riskManager: RiskManager? = riskManager

    init {
        require(maxTraders > 0) { "maxTraders must be positive" }
        logger.info { "Initialized AITraderManager (max traders: $maxTraders)" }
        this.riskManager?.registerStopHandlers({ id -> stopTrader(id) }, null)
        this.riskManager?.startMonitoring()
    }

    private suspend fun emitTraderTelemetry(traderId: String, trader: AITrader, reason: String) {
        runCatching {
            TelemetryCollector.publishTraderStatus(
                traderId = traderId,
                name = trader.config.name,
                state = trader.getState(),
                exchange = trader.config.exchange.name,
                symbol = trader.config.symbol,
                strategy = trader.config.strategy.name,
                reason = reason,
                metrics = trader.getMetrics()
            )
        }.onFailure {
            logger.debug(it) { "Failed to publish trader telemetry for $traderId" }
        }
    }

    suspend fun attachRiskManager(riskManager: RiskManager) {
        tradersMutex.withLock {
            this.riskManager = riskManager
            riskManager.registerStopHandlers({ id -> stopTrader(id) }, null)
            activeTraders.keys.forEach { traderId ->
                riskManager.registerTrader(traderId)
            }
        }
        riskManager.startMonitoring()
    }

    /**
     * Create a new AI trader.
     *
     * @param config Trader configuration
     * @return Result containing trader ID on success, or error on failure
     */
    suspend fun createTrader(config: AITraderConfig): Result<String> {
        return tradersMutex.withLock {
            try {
                // Check max limit
                val currentCount = activeTraders.size
                if (currentCount >= maxTraders) {
                    val error = MaxTradersExceededException(
                        "Maximum traders limit ($maxTraders) reached. Cannot create more traders."
                    )
                    logger.warn { error.message }
                    return Result.failure(error)
                }

                // Check database limit as well
                val canCreate = repository.canCreateMore()
                if (!canCreate) {
                    val error = MaxTradersExceededException(
                        "Maximum traders limit ($maxTraders) reached in database. Cannot create more traders."
                    )
                    logger.warn { error.message }
                    return Result.failure(error)
                }

                // Validate configuration
                val riskValidation = riskManager?.validateTraderCreation(config)
                if (riskValidation != null && riskValidation.isFailure) {
                    val exception = riskValidation.exceptionOrNull()
                    if (exception != null) {
                        return Result.failure(exception)
                    }
                }

                // Create exchange connector
                val exchangeConfig = createExchangeConfig(config.exchange)
                val connector = connectorFactory.createConnector(
                    config.exchange,
                    exchangeConfig,
                    useCache = true
                )

                // Create AITrader instance
                val trader = AITrader(config, connector)

                // Save to database
                val dbId = repository.create(
                    name = config.name,
                    exchange = config.exchange.name,
                    tradingPair = formatTradingPair(config.symbol),
                    leverage = config.maxRiskLevel,
                    initialBalance = config.maxStakeAmount
                )

                if (dbId == null) {
                    return Result.failure(IllegalStateException("Failed to create trader in database"))
                }

                val traderId = dbId.toString()

                // Store in active traders map
                activeTraders[traderId] = trader

                riskManager?.registerTrader(traderId)

                // Save initial state
                statePersistence.saveState(traderId, trader.getState())

                logger.info { "Created trader: $traderId (${config.name})" }
                emitTraderTelemetry(traderId, trader, "CREATED")
                Result.success(traderId)
            } catch (e: Exception) {
                logger.error(e) { "Failed to create trader: ${config.name}" }
                Result.failure(e)
            }
        }
    }

    /**
     * Start a trader.
     *
     * @param traderId Trader ID
     * @return Result indicating success or failure
     */
    suspend fun startTrader(traderId: String): Result<Unit> {
        return tradersMutex.withLock {
            try {
                val trader = activeTraders[traderId]
                    ?: return Result.failure(IllegalArgumentException("Trader not found: $traderId"))

                // Check state
                val state = trader.getState()
                if (state != AITraderState.IDLE && state != AITraderState.STOPPED) {
                    return Result.failure(
                        IllegalStateException("Cannot start trader in state: $state")
                    )
                }

                // Start trader
                val startResult = trader.start()
                if (startResult.isFailure) {
                    return startResult
                }

                // Update database status
                statePersistence.saveState(traderId, trader.getState())

                logger.info { "Started trader: $traderId" }
                emitTraderTelemetry(traderId, trader, "STARTED")
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Failed to start trader: $traderId" }
                Result.failure(e)
            }
        }
    }

    /**
     * Stop a trader.
     *
     * @param traderId Trader ID
     * @return Result indicating success or failure
     */
    suspend fun stopTrader(traderId: String): Result<Unit> {
        return tradersMutex.withLock {
            try {
                val trader = activeTraders[traderId]
                    ?: return Result.failure(IllegalArgumentException("Trader not found: $traderId"))

                // Stop trader
                val stopResult = trader.stop()
                if (stopResult.isFailure) {
                    return stopResult
                }

                // Save state to database
                statePersistence.saveState(traderId, trader.getState())

                logger.info { "Stopped trader: $traderId" }
                emitTraderTelemetry(traderId, trader, "STOPPED")
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Failed to stop trader: $traderId" }
                Result.failure(e)
            }
        }
    }

    /**
     * Update trader configuration.
     *
     * @param traderId Trader ID
     * @param newConfig New configuration
     * @return Result indicating success or failure
     */
    suspend fun updateTrader(traderId: String, newConfig: AITraderConfig): Result<Unit> {
        return tradersMutex.withLock {
            try {
                val trader = activeTraders[traderId]
                    ?: return Result.failure(IllegalArgumentException("Trader not found: $traderId"))

                // Check if trader is running (may need to stop first)
                val state = trader.getState()
                val wasRunning = state == AITraderState.RUNNING || state == AITraderState.PAUSED

                if (wasRunning) {
                    // Stop trader before updating
                    val stopResult = trader.stop()
                    if (stopResult.isFailure) {
                        return Result.failure(
                            IllegalStateException("Failed to stop trader for update: ${stopResult.exceptionOrNull()?.message}")
                        )
                    }
                }

                // Update configuration
                val updateResult = trader.updateConfig(newConfig)
                if (updateResult.isFailure) {
                    return updateResult
                }

                // Update database (simplified - would need to update all fields)
                // For now, just update status
                statePersistence.saveState(traderId, trader.getState())

                // Restart if it was running
                if (wasRunning) {
                    trader.start()
                }

                logger.info { "Updated trader: $traderId" }
                emitTraderTelemetry(traderId, trader, "UPDATED")
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Failed to update trader: $traderId" }
                Result.failure(e)
            }
        }
    }

    /**
     * Delete a trader.
     *
     * @param traderId Trader ID
     * @return Result indicating success or failure
     */
    suspend fun deleteTrader(traderId: String): Result<Unit> {
        return tradersMutex.withLock {
            try {
                val trader = activeTraders[traderId]
                    ?: return Result.failure(IllegalArgumentException("Trader not found: $traderId"))

                // Stop trader if running
                val state = trader.getState()
                if (state == AITraderState.RUNNING || state == AITraderState.PAUSED) {
                    trader.stop()
                }

                // Cleanup trader resources
                trader.cleanup()

                // Remove from active traders map
                activeTraders.remove(traderId)

                // Delete from database
                val id = traderId.toIntOrNull()
                    ?: return Result.failure(IllegalArgumentException("Invalid trader ID: $traderId"))

                val deleted = repository.delete(id)
                if (!deleted) {
                    return Result.failure(IllegalStateException("Failed to delete trader from database"))
                }

                riskManager?.deregisterTrader(traderId)

                logger.info { "Deleted trader: $traderId" }
                emitTraderTelemetry(traderId, trader, "DELETED")
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Failed to delete trader: $traderId" }
                Result.failure(e)
            }
        }
    }

    /**
     * Get a trader instance by ID.
     *
     * @param traderId Trader ID
     * @return Trader instance, or null if not found
     */
    suspend fun getTrader(traderId: String): AITrader? {
        return tradersMutex.withLock {
            activeTraders[traderId]
        }
    }

    /**
     * Get all active traders.
     *
     * @return List of all active trader instances
     */
    suspend fun getAllTraders(): List<AITrader> {
        return tradersMutex.withLock {
            activeTraders.values.toList()
        }
    }

    /**
     * Get current trader count.
     *
     * @return Number of active traders
     */
    suspend fun getTraderCount(): Int {
        return tradersMutex.withLock {
            activeTraders.size
        }
    }

    /**
     * Recover traders from database on application restart.
     *
     * This method:
     * 1. Loads all traders from database
     * 2. Recreates AITrader instances
     * 3. Restores exchange connectors
     * 4. Restores trader state (but doesn't auto-start)
     *
     * @return Result indicating success or failure
     */
    suspend fun recoverTraders(): Result<Unit> {
        return tradersMutex.withLock {
            try {
                val dbTraders = repository.findAll()

                logger.info { "Recovering ${dbTraders.size} traders from database" }

                for (dbTrader in dbTraders) {
                    try {
                        // Convert database model to config
                        val config = statePersistence.dbTraderToConfig(dbTrader)

                        // Create exchange connector
                        val exchangeConfig = createExchangeConfig(config.exchange)
                        val connector = connectorFactory.createConnector(
                            config.exchange,
                            exchangeConfig,
                            useCache = true
                        )

                        // Create AITrader instance
                        val trader = AITrader(config, connector)

                        // Restore state (but don't auto-start)
                        val traderId = dbTrader.id.toString()

                        activeTraders[traderId] = trader
                        statePersistence.saveState(traderId, trader.getState())
                        emitTraderTelemetry(traderId, trader, "RECOVERED")

                        logger.info { "Recovered trader: $traderId" }
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to recover trader from database entry: ${dbTrader.id}" }
                    }
                }

                logger.info { "Recovery complete: ${activeTraders.size} traders recovered" }
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Failed to recover traders" }
                Result.failure(e)
            }
        }
    }

    /**
     * Start health monitoring for all active traders.
     */
    fun startHealthMonitoring() {
        healthMonitor.startMonitoring(activeTraders) { traderId, health ->
            if (!health.isHealthy) {
                logger.warn {
                    "Trader $traderId health issues: ${health.issues.joinToString(", ")}"
                }
            }
        }
    }

    /**
     * Stop health monitoring.
     */
    fun stopHealthMonitoring() {
        healthMonitor.stopMonitoring()
    }

    /**
     * Check health of a specific trader.
     *
     * @param traderId Trader ID
     * @return TraderHealth status
     */
    suspend fun checkTraderHealth(traderId: String): TraderHealth? {
        return tradersMutex.withLock {
            val trader = activeTraders[traderId] ?: return null
            healthMonitor.checkTraderHealth(traderId, trader)
        }
    }

    /**
     * Check health of all traders.
     *
     * @return Map of trader IDs to their health status
     */
    suspend fun checkAllTradersHealth(): Map<String, TraderHealth> {
        return tradersMutex.withLock {
            healthMonitor.checkAllTradersHealth(activeTraders)
        }
    }

    // Helper methods

    private fun createExchangeConfig(exchange: Exchange): ExchangeConfig {
        // Create a default testnet config
        // In production, this would load from ConfigManager
        return ExchangeConfig(
            exchange = exchange,
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            testnet = true
        )
    }

    private fun formatTradingPair(symbol: String): String {
        // Convert "BTCUSDT" to "BTC/USDT" format
        return when {
            symbol.endsWith("USDT") -> {
                val base = symbol.removeSuffix("USDT")
                "$base/USDT"
            }
            symbol.endsWith("USD") -> {
                val base = symbol.removeSuffix("USD")
                "$base/USD"
            }
            else -> symbol
        }
    }
}

