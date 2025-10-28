package com.fmps.autotrader.core.connectors.health

import com.fmps.autotrader.core.connectors.IExchangeConnector
import com.fmps.autotrader.core.connectors.exceptions.ConnectionException
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * Monitors the health of an exchange connector and handles automatic reconnection.
 *
 * This class:
 * - Performs periodic health checks (ping/heartbeat)
 * - Detects connection failures
 * - Automatically attempts to reconnect
 * - Implements circuit breaker pattern for repeated failures
 * - Emits health status change events
 *
 * ## Usage
 * ```kotlin
 * val monitor = ConnectionHealthMonitor(
 *     connector = binanceConnector,
 *     checkIntervalMs = 30000,
 *     autoReconnect = true
 * )
 *
 * monitor.start()
 * // Monitor runs in background
 * monitor.stop()
 * ```
 *
 * @param connector The exchange connector to monitor
 * @param checkIntervalMs Interval between health checks in milliseconds (default: 30s)
 * @param autoReconnect Whether to automatically reconnect on failure (default: true)
 * @param maxConsecutiveFailures Max consecutive failures before circuit breaker opens (default: 5)
 * @param circuitBreakerResetMs Time to wait before retrying after circuit breaker opens (default: 60s)
 *
 * @since 1.0.0
 */
class ConnectionHealthMonitor(
    private val connector: IExchangeConnector,
    private val checkIntervalMs: Long = 30000,
    private val autoReconnect: Boolean = true,
    private val maxConsecutiveFailures: Int = 5,
    private val circuitBreakerResetMs: Long = 60000
) {

    /**
     * Health status enum
     */
    enum class HealthStatus {
        HEALTHY,        // Connection is healthy
        UNHEALTHY,      // Connection issues detected
        RECONNECTING,   // Attempting to reconnect
        CIRCUIT_OPEN,   // Circuit breaker open (too many failures)
        STOPPED         // Monitor is stopped
    }

    // State
    private val running = AtomicBoolean(false)
    private val currentStatus = AtomicBoolean(true) // true = healthy
    private val consecutiveFailures = AtomicInteger(0)
    private val circuitBreakerOpen = AtomicBoolean(false)
    private var circuitBreakerOpenTime: Instant? = null
    
    // Metrics
    private val totalChecks = AtomicLong(0)
    private val successfulChecks = AtomicLong(0)
    private val failedChecks = AtomicLong(0)
    private val reconnectAttempts = AtomicLong(0)
    private val successfulReconnects = AtomicLong(0)
    private var lastCheckTime: Instant? = null
    private var lastSuccessTime: Instant? = null

    // Job management
    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Event listeners
    private val statusChangeListeners = mutableListOf<suspend (HealthStatus, String?) -> Unit>()

    /**
     * Starts the health monitor.
     *
     * This launches a background coroutine that performs periodic health checks.
     *
     * @throws IllegalStateException if monitor is already running
     */
    fun start() {
        require(!running.get()) { "Monitor is already running" }

        logger.info { "Starting health monitor for ${connector.getExchange().name} (check interval: ${checkIntervalMs}ms)" }

        running.set(true)
        monitorJob = scope.launch {
            monitorLoop()
        }

        logger.info { "✓ Health monitor started for ${connector.getExchange().name}" }
    }

    /**
     * Stops the health monitor.
     */
    suspend fun stop() {
        if (!running.get()) {
            logger.debug { "Monitor already stopped" }
            return
        }

        logger.info { "Stopping health monitor for ${connector.getExchange().name}..." }

        running.set(false)
        monitorJob?.cancelAndJoin()
        monitorJob = null

        emitStatusChange(HealthStatus.STOPPED, "Monitor stopped")
        logger.info { "✓ Health monitor stopped for ${connector.getExchange().name}" }
    }

    /**
     * Main monitoring loop.
     */
    private suspend fun monitorLoop() {
        logger.debug { "Monitor loop started" }

        while (running.get()) {
            try {
                // Perform health check
                performHealthCheck()

                // Wait before next check
                delay(checkIntervalMs)

            } catch (e: CancellationException) {
                logger.debug { "Monitor loop cancelled" }
                throw e
            } catch (e: Exception) {
                logger.error(e) { "Unexpected error in monitor loop" }
                delay(checkIntervalMs)
            }
        }

        logger.debug { "Monitor loop ended" }
    }

    /**
     * Performs a single health check.
     */
    private suspend fun performHealthCheck() {
        totalChecks.incrementAndGet()
        lastCheckTime = Instant.now()

        // Check if circuit breaker should be reset
        if (circuitBreakerOpen.get()) {
            checkCircuitBreakerReset()
            return
        }

        // Check connection status
        val isHealthy = try {
            checkConnectorHealth()
        } catch (e: Exception) {
            logger.error(e) { "Health check failed for ${connector.getExchange().name}" }
            false
        }

        if (isHealthy) {
            handleHealthyStatus()
        } else {
            handleUnhealthyStatus()
        }
    }

    /**
     * Checks if the connector is healthy.
     *
     * @return true if healthy, false otherwise
     */
    private suspend fun checkConnectorHealth(): Boolean {
        // Check if connected
        if (!connector.isConnected()) {
            logger.warn { "${connector.getExchange().name} is not connected" }
            return false
        }

        // Additional health checks could be implemented here
        // (e.g., test a lightweight API endpoint, check WebSocket connections, etc.)
        // For now, we just check the connection status

        return true
    }

    /**
     * Handles healthy status.
     */
    private suspend fun handleHealthyStatus() {
        successfulChecks.incrementAndGet()
        lastSuccessTime = Instant.now()

        // Reset consecutive failures
        val previousFailures = consecutiveFailures.getAndSet(0)

        // Emit status change if recovering from failure
        if (previousFailures > 0) {
            logger.info { "✓ ${connector.getExchange().name} connection recovered after $previousFailures failures" }
            currentStatus.set(true)
            emitStatusChange(HealthStatus.HEALTHY, "Connection recovered")
        }
    }

    /**
     * Handles unhealthy status.
     */
    private suspend fun handleUnhealthyStatus() {
        failedChecks.incrementAndGet()

        val failures = consecutiveFailures.incrementAndGet()
        logger.warn { "${connector.getExchange().name} health check failed (${failures}/${maxConsecutiveFailures} consecutive failures)" }

        // Emit status change if newly unhealthy
        if (failures == 1) {
            currentStatus.set(false)
            emitStatusChange(HealthStatus.UNHEALTHY, "Connection unhealthy")
        }

        // Check if circuit breaker should open
        if (failures >= maxConsecutiveFailures) {
            openCircuitBreaker()
            return
        }

        // Attempt reconnection if enabled
        if (autoReconnect) {
            attemptReconnect()
        }
    }

    /**
     * Attempts to reconnect to the exchange.
     */
    private suspend fun attemptReconnect() {
        reconnectAttempts.incrementAndGet()
        
        logger.info { "Attempting to reconnect to ${connector.getExchange().name}..." }
        emitStatusChange(HealthStatus.RECONNECTING, "Attempting reconnection")

        try {
            // Disconnect first if connected
            if (connector.isConnected()) {
                connector.disconnect()
            }

            // Reconnect
            connector.connect()

            // Success
            successfulReconnects.incrementAndGet()
            consecutiveFailures.set(0)
            currentStatus.set(true)

            logger.info { "✓ Successfully reconnected to ${connector.getExchange().name}" }
            emitStatusChange(HealthStatus.HEALTHY, "Reconnection successful")

        } catch (e: Exception) {
            logger.error(e) { "✗ Failed to reconnect to ${connector.getExchange().name}" }
        }
    }

    /**
     * Opens the circuit breaker after too many consecutive failures.
     */
    private suspend fun openCircuitBreaker() {
        if (circuitBreakerOpen.compareAndSet(false, true)) {
            circuitBreakerOpenTime = Instant.now()
            
            logger.error { 
                "Circuit breaker OPEN for ${connector.getExchange().name} after $maxConsecutiveFailures " +
                "consecutive failures. Will retry in ${circuitBreakerResetMs}ms"
            }
            
            emitStatusChange(
                HealthStatus.CIRCUIT_OPEN,
                "Circuit breaker open after $maxConsecutiveFailures failures"
            )
        }
    }

    /**
     * Checks if the circuit breaker should be reset.
     */
    private suspend fun checkCircuitBreakerReset() {
        val openTime = circuitBreakerOpenTime ?: return
        val elapsed = Instant.now().toEpochMilli() - openTime.toEpochMilli()

        if (elapsed >= circuitBreakerResetMs) {
            logger.info { "Circuit breaker timeout expired for ${connector.getExchange().name}. Attempting reset..." }
            
            // Reset circuit breaker
            circuitBreakerOpen.set(false)
            circuitBreakerOpenTime = null
            consecutiveFailures.set(0)

            // Attempt reconnection
            if (autoReconnect) {
                attemptReconnect()
            }
        }
    }

    /**
     * Adds a status change listener.
     *
     * @param listener Function to be called when health status changes
     */
    fun addStatusChangeListener(listener: suspend (HealthStatus, String?) -> Unit) {
        statusChangeListeners.add(listener)
    }

    /**
     * Removes a status change listener.
     *
     * @param listener The listener to remove
     */
    fun removeStatusChangeListener(listener: suspend (HealthStatus, String?) -> Unit) {
        statusChangeListeners.remove(listener)
    }

    /**
     * Emits a status change event to all listeners.
     */
    private suspend fun emitStatusChange(status: HealthStatus, message: String?) {
        for (listener in statusChangeListeners) {
            try {
                listener(status, message)
            } catch (e: Exception) {
                logger.error(e) { "Error in status change listener" }
            }
        }
    }

    /**
     * Gets the current health status.
     *
     * @return Current health status
     */
    fun getHealthStatus(): HealthStatus {
        return when {
            !running.get() -> HealthStatus.STOPPED
            circuitBreakerOpen.get() -> HealthStatus.CIRCUIT_OPEN
            !currentStatus.get() -> HealthStatus.UNHEALTHY
            else -> HealthStatus.HEALTHY
        }
    }

    /**
     * Checks if the connection is healthy.
     *
     * @return true if healthy, false otherwise
     */
    fun isHealthy(): Boolean {
        return currentStatus.get() && !circuitBreakerOpen.get()
    }

    /**
     * Gets monitor metrics.
     *
     * @return Metrics map
     */
    fun getMetrics(): Map<String, Any> {
        val checks = totalChecks.get()
        
        return mapOf(
            "running" to running.get(),
            "status" to getHealthStatus().name,
            "is_healthy" to isHealthy(),
            "total_checks" to checks,
            "successful_checks" to successfulChecks.get(),
            "failed_checks" to failedChecks.get(),
            "success_rate" to if (checks > 0) successfulChecks.get().toDouble() / checks else 0.0,
            "consecutive_failures" to consecutiveFailures.get(),
            "circuit_breaker_open" to circuitBreakerOpen.get(),
            "reconnect_attempts" to reconnectAttempts.get(),
            "successful_reconnects" to successfulReconnects.get(),
            "last_check_time" to (lastCheckTime?.toString() ?: "never"),
            "last_success_time" to (lastSuccessTime?.toString() ?: "never")
        )
    }

    /**
     * Resets monitor metrics.
     */
    fun resetMetrics() {
        totalChecks.set(0)
        successfulChecks.set(0)
        failedChecks.set(0)
        reconnectAttempts.set(0)
        successfulReconnects.set(0)
        lastCheckTime = null
        lastSuccessTime = null
        logger.debug { "Health monitor metrics reset" }
    }
}

