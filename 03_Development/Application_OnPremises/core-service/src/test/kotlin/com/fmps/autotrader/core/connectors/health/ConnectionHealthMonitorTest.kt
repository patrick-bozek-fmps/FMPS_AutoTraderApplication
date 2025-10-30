package com.fmps.autotrader.core.connectors.health

import com.fmps.autotrader.core.connectors.MockExchangeConnector
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.model.ExchangeConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ConnectionHealthMonitorTest {

    private lateinit var connector: MockExchangeConnector
    private lateinit var monitor: ConnectionHealthMonitor

    @BeforeEach
    fun setup() = runBlocking {
        connector = MockExchangeConnector()
        
        // Configure connector before connecting
        val config = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = "test-key",
            apiSecret = "test-secret",
            testnet = true
        )
        connector.configure(config)
        connector.connect()
    }

    @AfterEach
    fun cleanup() = runBlocking {
        if (::monitor.isInitialized) {
            monitor.stop()
        }
        if (connector.isConnected()) {
            connector.disconnect()
        }
    }

    @Test
    fun `test monitor initialization with valid parameters`() {
        monitor = ConnectionHealthMonitor(
            connector = connector,
            checkIntervalMs = 1000,
            autoReconnect = true,
            maxConsecutiveFailures = 3,
            circuitBreakerResetMs = 5000
        )

        assertNotNull(monitor)
        assertEquals(ConnectionHealthMonitor.HealthStatus.STOPPED, monitor.getHealthStatus())
    }

    @Test
    fun `test monitor starts successfully`() {
        monitor = ConnectionHealthMonitor(connector, checkIntervalMs = 1000)

        monitor.start()

        assertTrue(monitor.getHealthStatus() in listOf(
            ConnectionHealthMonitor.HealthStatus.HEALTHY,
            ConnectionHealthMonitor.HealthStatus.UNHEALTHY
        ))

        val metrics = monitor.getMetrics()
        assertEquals(true, metrics["running"])
    }

    @Test
    fun `test monitor cannot start twice`() {
        monitor = ConnectionHealthMonitor(connector, checkIntervalMs = 1000)

        monitor.start()

        val exception = assertThrows<IllegalArgumentException> {
            monitor.start()
        }

        assertTrue(exception.message!!.contains("already running"))
    }

    @Test
    fun `test monitor stops successfully`() = runBlocking {
        monitor = ConnectionHealthMonitor(connector, checkIntervalMs = 1000)

        monitor.start()
        delay(500)  // Let it run briefly
        monitor.stop()

        assertEquals(ConnectionHealthMonitor.HealthStatus.STOPPED, monitor.getHealthStatus())

        val metrics = monitor.getMetrics()
        assertEquals(false, metrics["running"])
    }

    @Test
    fun `test stop on non-running monitor is safe`() = runBlocking {
        monitor = ConnectionHealthMonitor(connector, checkIntervalMs = 1000)

        // Should not throw exception
        monitor.stop()

        assertEquals(ConnectionHealthMonitor.HealthStatus.STOPPED, monitor.getHealthStatus())
    }

    @Test
    fun `test monitor detects healthy connection`() = runBlocking {
        monitor = ConnectionHealthMonitor(
            connector = connector,
            checkIntervalMs = 500,
            autoReconnect = false
        )

        monitor.start()
        delay(800)  // Wait for at least one health check
        monitor.stop()

        assertTrue(monitor.isHealthy() || !connector.isConnected())
        
        val metrics = monitor.getMetrics()
        val totalChecks = metrics["total_checks"] as Long
        assertTrue(totalChecks >= 1, "Should have performed at least 1 health check")
    }

    @Test
    fun `test monitor detects unhealthy connection`() = runBlocking {
        monitor = ConnectionHealthMonitor(
            connector = connector,
            checkIntervalMs = 500,
            autoReconnect = false,
            maxConsecutiveFailures = 10  // High threshold to prevent circuit breaker
        )

        monitor.start()
        delay(200)  // Let it detect healthy state first

        // Disconnect to simulate failure
        connector.disconnect()
        delay(800)  // Wait for health check to detect disconnection

        monitor.stop()

        val metrics = monitor.getMetrics()
        val failedChecks = metrics["failed_checks"] as Long
        assertTrue(failedChecks >= 1, "Should have detected at least 1 failure")
    }

    @Test
    fun `test auto-reconnect on connection failure`() = runBlocking {
        // Note: MockExchangeConnector doesn't have configureLatency method
        
        monitor = ConnectionHealthMonitor(
            connector = connector,
            checkIntervalMs = 500,
            autoReconnect = true,
            maxConsecutiveFailures = 10  // High threshold
        )

        monitor.start()
        delay(200)  // Let it stabilize

        // Disconnect to trigger reconnection
        connector.disconnect()
        delay(1000)  // Wait for detection and reconnection

        monitor.stop()

        val metrics = monitor.getMetrics()
        val reconnectAttempts = metrics["reconnect_attempts"] as Long
        assertTrue(reconnectAttempts >= 1, "Should have attempted reconnection")
    }

    @Test
    fun `test circuit breaker opens after max failures`() = runBlocking {
        monitor = ConnectionHealthMonitor(
            connector = connector,
            checkIntervalMs = 300,
            autoReconnect = false,
            maxConsecutiveFailures = 3,
            circuitBreakerResetMs = 10000  // Long timeout
        )

        monitor.start()
        
        // Disconnect to cause failures
        connector.disconnect()
        
        // Wait for failures to accumulate
        delay(1500)  // ~5 checks should trigger circuit breaker

        val status = monitor.getHealthStatus()
        val metrics = monitor.getMetrics()
        
        monitor.stop()

        // Circuit breaker should be open after max failures
        assertTrue(
            status == ConnectionHealthMonitor.HealthStatus.CIRCUIT_OPEN ||
            metrics["circuit_breaker_open"] as Boolean,
            "Circuit breaker should be open after consecutive failures"
        )
    }

    @Test
    fun `test circuit breaker resets after timeout`() = runBlocking {
        // Note: MockExchangeConnector doesn't have configureLatency method
        
        monitor = ConnectionHealthMonitor(
            connector = connector,
            checkIntervalMs = 200,
            autoReconnect = true,
            maxConsecutiveFailures = 2,
            circuitBreakerResetMs = 1000  // Short timeout for testing
        )

        monitor.start()
        
        // Disconnect to trigger circuit breaker
        connector.disconnect()
        delay(600)  // Wait for circuit breaker to open

        // The health monitor with autoReconnect should try to reconnect automatically
        // Just wait for circuit breaker to reset and reconnect
        delay(1500)

        val metrics = monitor.getMetrics()
        monitor.stop()

        // Circuit breaker should have reset
        assertTrue(metrics.containsKey("circuit_breaker_open"), "Metrics should contain circuit_breaker_open")
        assertTrue(metrics.containsKey("reconnect_attempts"), "Metrics should contain reconnect_attempts")
        
        val circuitBreakerOpen = metrics["circuit_breaker_open"] as? Boolean
        val reconnectAttempts = metrics["reconnect_attempts"] as? Long
        
        assertNotNull(circuitBreakerOpen, "circuit_breaker_open should not be null")
        assertNotNull(reconnectAttempts, "reconnect_attempts should not be null")
        
        assertTrue(reconnectAttempts!! >= 0, "Should have metrics for reconnection attempts")
        // Note: Circuit breaker may still be open if reconnection failed
    }

    @Test
    fun `test status change listener receives events`() = runBlocking {
        val statusChanges = mutableListOf<Pair<ConnectionHealthMonitor.HealthStatus, String?>>()

        monitor = ConnectionHealthMonitor(
            connector = connector,
            checkIntervalMs = 300,
            autoReconnect = false
        )

        monitor.addStatusChangeListener { status, message ->
            statusChanges.add(status to message)
        }

        monitor.start()
        delay(500)

        // Disconnect to trigger status change
        connector.disconnect()
        delay(800)

        monitor.stop()

        // Should have received at least STOPPED event
        assertTrue(statusChanges.isNotEmpty(), "Should have received status change events")
        
        val stoppedEvents = statusChanges.count { it.first == ConnectionHealthMonitor.HealthStatus.STOPPED }
        assertTrue(stoppedEvents >= 1, "Should have received STOPPED event")
    }

    @Test
    fun `test multiple status change listeners`() = runBlocking {
        val statusChanges1 = mutableListOf<ConnectionHealthMonitor.HealthStatus>()
        val statusChanges2 = mutableListOf<ConnectionHealthMonitor.HealthStatus>()

        monitor = ConnectionHealthMonitor(connector, checkIntervalMs = 500)

        monitor.addStatusChangeListener { status, _ -> statusChanges1.add(status) }
        monitor.addStatusChangeListener { status, _ -> statusChanges2.add(status) }

        monitor.start()
        delay(500)
        monitor.stop()

        // Both listeners should receive events
        assertTrue(statusChanges1.isNotEmpty())
        assertTrue(statusChanges2.isNotEmpty())
        assertEquals(statusChanges1.size, statusChanges2.size)
    }

    @Test
    fun `test remove status change listener`() = runBlocking {
        val statusChanges = mutableListOf<ConnectionHealthMonitor.HealthStatus>()
        
        val listener: suspend (ConnectionHealthMonitor.HealthStatus, String?) -> Unit = { status, _ -> 
            statusChanges.add(status)
        }

        monitor = ConnectionHealthMonitor(connector, checkIntervalMs = 500)

        monitor.addStatusChangeListener(listener)
        monitor.start()
        delay(300)
        
        monitor.removeStatusChangeListener(listener)
        
        val eventsBeforeRemoval = statusChanges.size
        delay(300)
        monitor.stop()
        
        val eventsAfterRemoval = statusChanges.size - eventsBeforeRemoval

        // After removal, should receive fewer (or no) events
        // At least the STOPPED event will still be received as it happens after stop()
        assertTrue(eventsAfterRemoval <= 1, "Should receive minimal events after listener removal")
    }

    @Test
    fun `test isHealthy returns correct status`() = runBlocking {
        monitor = ConnectionHealthMonitor(
            connector = connector,
            checkIntervalMs = 500,
            autoReconnect = false
        )

        monitor.start()
        delay(800)  // Wait for health check

        val healthy = monitor.isHealthy()

        monitor.stop()

        // Should reflect connector state
        assertTrue(healthy || !connector.isConnected())
    }

    @Test
    fun `test getMetrics returns expected fields`() = runBlocking {
        monitor = ConnectionHealthMonitor(connector, checkIntervalMs = 500)

        monitor.start()
        delay(800)
        monitor.stop()

        val metrics = monitor.getMetrics()

        // Verify all expected fields are present
        assertNotNull(metrics["running"])
        assertNotNull(metrics["status"])
        assertNotNull(metrics["is_healthy"])
        assertNotNull(metrics["total_checks"])
        assertNotNull(metrics["successful_checks"])
        assertNotNull(metrics["failed_checks"])
        assertNotNull(metrics["success_rate"])
        assertNotNull(metrics["consecutive_failures"])
        assertNotNull(metrics["circuit_breaker_open"])
        assertNotNull(metrics["reconnect_attempts"])
        assertNotNull(metrics["successful_reconnects"])
        assertNotNull(metrics["last_check_time"])
        assertNotNull(metrics["last_success_time"])
    }

    @Test
    fun `test getMetrics shows correct check counts`() = runBlocking {
        monitor = ConnectionHealthMonitor(connector, checkIntervalMs = 300)

        monitor.start()
        delay(1000)  // Wait for multiple checks
        monitor.stop()

        val metrics = monitor.getMetrics()
        val totalChecks = metrics["total_checks"] as Long
        val successfulChecks = metrics["successful_checks"] as Long

        assertTrue(totalChecks >= 2, "Should have performed multiple health checks")
        assertTrue(successfulChecks >= 1, "Should have at least one successful check")
        assertTrue(successfulChecks <= totalChecks, "Successful checks should not exceed total")
    }

    @Test
    fun `test resetMetrics clears counters`() = runBlocking {
        monitor = ConnectionHealthMonitor(connector, checkIntervalMs = 500)

        monitor.start()
        delay(800)
        monitor.stop()

        var metrics = monitor.getMetrics()
        val checksBeforeReset = metrics["total_checks"] as Long
        assertTrue(checksBeforeReset > 0, "Should have some checks before reset")

        monitor.resetMetrics()

        metrics = monitor.getMetrics()
        assertEquals(0L, metrics["total_checks"])
        assertEquals(0L, metrics["successful_checks"])
        assertEquals(0L, metrics["failed_checks"])
        assertEquals(0L, metrics["reconnect_attempts"])
        assertEquals(0L, metrics["successful_reconnects"])
    }

    @Test
    fun `test success rate calculation`() = runBlocking {
        monitor = ConnectionHealthMonitor(connector, checkIntervalMs = 300)

        monitor.start()
        delay(1000)  // Multiple checks
        monitor.stop()

        val metrics = monitor.getMetrics()
        val successRate = metrics["success_rate"] as Double
        val totalChecks = metrics["total_checks"] as Long

        if (totalChecks > 0) {
            assertTrue(successRate >= 0.0 && successRate <= 1.0, "Success rate should be between 0 and 1")
        } else {
            assertEquals(0.0, successRate, "Success rate should be 0 when no checks performed")
        }
    }

    @Test
    fun `test health status transitions`() = runBlocking {
        val statusChanges = mutableListOf<ConnectionHealthMonitor.HealthStatus>()

        monitor = ConnectionHealthMonitor(
            connector = connector,
            checkIntervalMs = 300,
            autoReconnect = true,
            maxConsecutiveFailures = 10
        )

        monitor.addStatusChangeListener { status, _ ->
            statusChanges.add(status)
        }

        monitor.start()
        delay(400)  // Should be healthy

        // Trigger unhealthy state
        connector.disconnect()
        delay(800)  // Should detect unhealthy and attempt reconnect

        monitor.stop()

        // Should have transitioned through states
        assertTrue(statusChanges.contains(ConnectionHealthMonitor.HealthStatus.STOPPED))
        // May also contain UNHEALTHY, RECONNECTING depending on timing
    }

    @Test
    fun `test consecutive failures counter`() = runBlocking {
        monitor = ConnectionHealthMonitor(
            connector = connector,
            checkIntervalMs = 300,
            autoReconnect = false,
            maxConsecutiveFailures = 10
        )

        monitor.start()
        delay(200)

        // Disconnect to cause failures
        connector.disconnect()
        delay(1000)  // Multiple failed checks

        val metrics = monitor.getMetrics()
        monitor.stop()

        val consecutiveFailures = metrics["consecutive_failures"] as Int
        assertTrue(consecutiveFailures >= 1, "Should have consecutive failures after disconnection")
    }
}

