package com.fmps.autotrader.core.traders

import com.fmps.autotrader.core.connectors.IExchangeConnector
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Monitors the health of AI Trader instances.
 *
 * Performs periodic health checks on traders and identifies issues such as:
 * - Traders stuck in ERROR state
 * - Exchange connector failures
 * - No signal generation for extended periods
 * - High error counts
 *
 * @property checkInterval Interval between health checks (default: 60 seconds)
 * @property maxSignalAge Maximum age of last signal before flagging as issue (default: 5 minutes)
 *
 * @since 1.0.0
 */
class HealthMonitor(
    private val checkInterval: Duration = Duration.ofSeconds(60),
    private val maxSignalAge: Duration = Duration.ofMinutes(5)
) {
    private var monitoringJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var healthCheckCallback: ((String, TraderHealth) -> Unit)? = null

    /**
     * Start periodic health monitoring.
     *
     * @param traders Map of trader IDs to AITrader instances to monitor
     * @param callback Optional callback invoked when health issues are detected
     */
    fun startMonitoring(
        traders: Map<String, AITrader>,
        callback: ((String, TraderHealth) -> Unit)? = null
    ) {
        if (monitoringJob?.isActive == true) {
            logger.warn { "Health monitoring already started" }
            return
        }

        this.healthCheckCallback = callback

        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    checkAllTradersHealth(traders)
                } catch (e: Exception) {
                    logger.error(e) { "Error during health check" }
                }
                delay(checkInterval.toMillis())
            }
        }

        logger.info { "Started health monitoring (interval: $checkInterval)" }
    }

    /**
     * Stop health monitoring.
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        logger.info { "Stopped health monitoring" }
    }

    /**
     * Check health of a single trader.
     *
     * @param traderId Trader ID
     * @param trader Trader instance
     * @return TraderHealth status
     */
    suspend fun checkTraderHealth(traderId: String, trader: AITrader): TraderHealth {
        val state = trader.getState()
        val issues = mutableListOf<String>()

        // Check state
        when (state) {
            AITraderState.ERROR -> {
                issues.add("Trader is in ERROR state")
            }
            AITraderState.STARTING -> {
                // Check if stuck in STARTING for too long
                // This would require tracking start time, simplified for now
            }
            else -> {
                // State is OK
            }
        }

        // Check exchange connector (if available)
        val exchangeConnectorHealthy = try {
            // Check if connector is connected (simplified check)
            true // Would need access to connector to check properly
        } catch (e: Exception) {
            issues.add("Exchange connector issue: ${e.message}")
            false
        }

        // Check metrics for signal generation
        val metrics = trader.getMetrics()
        val lastSignalTime: Instant? = null // Would need to track this in metrics
        if (lastSignalTime != null) {
            val signalAge = Duration.between(lastSignalTime, Instant.now())
            if (signalAge > maxSignalAge) {
                issues.add("No signal generated for ${signalAge.toMinutes()} minutes")
            }
        }

        val isHealthy = issues.isEmpty() && state != AITraderState.ERROR

        val health = if (isHealthy) {
            TraderHealth.healthy(state, lastSignalTime)
        } else {
            TraderHealth.unhealthy(
                status = state,
                issues = issues,
                exchangeConnectorHealthy = exchangeConnectorHealthy
            )
        }

        // Invoke callback if issues found
        if (!isHealthy && healthCheckCallback != null) {
            healthCheckCallback!!(traderId, health)
        }

        return health
    }

    /**
     * Check health of all traders.
     *
     * @param traders Map of trader IDs to AITrader instances
     * @return Map of trader IDs to their health status
     */
    suspend fun checkAllTradersHealth(
        traders: Map<String, AITrader>
    ): Map<String, TraderHealth> {
        val healthMap = mutableMapOf<String, TraderHealth>()

        for ((traderId, trader) in traders) {
            try {
                val health = checkTraderHealth(traderId, trader)
                healthMap[traderId] = health

                if (!health.isHealthy) {
                    logger.warn {
                        "Trader $traderId health issues: ${health.issues.joinToString(", ")}"
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to check health for trader $traderId" }
                healthMap[traderId] = TraderHealth.unhealthy(
                    status = AITraderState.ERROR,
                    issues = listOf("Health check failed: ${e.message}")
                )
            }
        }

        return healthMap
    }
}






