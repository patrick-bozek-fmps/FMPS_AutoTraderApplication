package com.fmps.autotrader.core.connectors

import com.fmps.autotrader.core.connectors.exceptions.ConnectionException
import com.fmps.autotrader.core.connectors.ratelimit.RateLimiter
import com.fmps.autotrader.core.connectors.retry.RetryPolicy
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.model.ExchangeConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Abstract base class for all exchange connectors.
 *
 * This class provides common functionality shared by all exchange implementations:
 * - HTTP client setup and configuration
 * - Rate limiting integration
 * - Retry policy integration
 * - Connection state management
 * - Logging and metrics
 * - Helper methods for request signing, timestamp handling, and response parsing
 *
 * Subclasses must implement exchange-specific logic for authentication, API endpoints,
 * and response parsing.
 *
 * ## Lifecycle
 * 1. Constructor called with exchange type
 * 2. [configure] called with exchange config
 * 3. [connect] called to establish connection
 * 4. Use connector methods
 * 5. [disconnect] called to close connection
 *
 * @param exchange The exchange type this connector is for
 * @since 1.0.0
 */
abstract class AbstractExchangeConnector(
    private val exchange: Exchange
) : IExchangeConnector {

    protected val logger = KotlinLogging.logger {}

    // ============================================
    // Configuration and State
    // ============================================

    /**
     * Exchange configuration (set via configure method)
     */
    protected lateinit var config: ExchangeConfig

    /**
     * Connection state
     */
    private val connected = AtomicBoolean(false)

    /**
     * Configuration state
     */
    private val configured = AtomicBoolean(false)

    // ============================================
    // Core Components
    // ============================================

    /**
     * HTTP client for REST API requests
     */
    protected lateinit var httpClient: HttpClient

    /**
     * Rate limiter for controlling request rates
     */
    protected lateinit var rateLimiter: RateLimiter

    /**
     * Retry policy for handling failed requests
     */
    protected lateinit var retryPolicy: RetryPolicy

    /**
     * JSON serializer for parsing responses
     */
    protected val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }

    // ============================================
    // Metrics
    // ============================================

    protected val requestCount = AtomicLong(0)
    protected val successCount = AtomicLong(0)
    protected val errorCount = AtomicLong(0)
    protected val totalRequestTimeMs = AtomicLong(0)

    // ============================================
    // WebSocket Management
    // ============================================

    /**
     * Active WebSocket subscriptions
     */
    protected val subscriptions = ConcurrentHashMap<String, suspend (String) -> Unit>()

    /**
     * Subscription ID counter
     */
    private val subscriptionIdCounter = AtomicLong(0)

    // ============================================
    // IExchangeConnector Implementation
    // ============================================

    override fun getExchange(): Exchange = exchange

    override fun configure(config: ExchangeConfig) {
        require(!connected.get()) { "Cannot configure while connected. Disconnect first." }
        require(config.exchange == exchange) {
            "Configuration exchange (${config.exchange}) does not match connector exchange ($exchange)"
        }

        logger.info { "Configuring ${exchange.name} connector" }

        this.config = config

        // Initialize HTTP client
        httpClient = createHttpClient()

        // Initialize rate limiter
        rateLimiter = createRateLimiter()

        // Initialize retry policy
        retryPolicy = createRetryPolicy()

        configured.set(true)
        logger.info { "✓ ${exchange.name} connector configured successfully" }
    }

    override suspend fun connect() {
        require(configured.get()) { "Connector must be configured before connecting" }
        require(!connected.get()) { "Already connected" }

        logger.info { "Connecting to ${exchange.name}..." }

        try {
            // Test connectivity
            testConnectivity()

            // Perform exchange-specific connection setup
            onConnect()

            connected.set(true)
            logger.info { "✓ Connected to ${exchange.name}" }

        } catch (e: Exception) {
            logger.error(e) { "✗ Failed to connect to ${exchange.name}" }
            throw ConnectionException(
                message = "Failed to connect to ${exchange.name}: ${e.message}",
                cause = e,
                exchangeName = exchange.name
            )
        }
    }

    override suspend fun disconnect() {
        if (!connected.get()) {
            logger.debug { "${exchange.name} already disconnected" }
            return
        }

        logger.info { "Disconnecting from ${exchange.name}..." }

        try {
            // Unsubscribe from all WebSocket streams
            unsubscribeAll()

            // Perform exchange-specific cleanup
            onDisconnect()

            // Close HTTP client
            httpClient.close()

            connected.set(false)
            logger.info { "✓ Disconnected from ${exchange.name}" }

        } catch (e: Exception) {
            logger.error(e) { "Error during disconnect from ${exchange.name}" }
            connected.set(false)
        }
    }

    override fun isConnected(): Boolean = connected.get()

    override suspend fun unsubscribeAll() {
        logger.info { "Unsubscribing from all streams (${subscriptions.size} active)" }
        
        val subscriptionIds = subscriptions.keys.toList()
        for (subscriptionId in subscriptionIds) {
            try {
                unsubscribe(subscriptionId)
            } catch (e: Exception) {
                logger.error(e) { "Error unsubscribing from $subscriptionId" }
            }
        }
        
        subscriptions.clear()
        logger.info { "✓ All subscriptions removed" }
    }

    // ============================================
    // Abstract Methods (Exchange-Specific)
    // ============================================

    /**
     * Tests connectivity to the exchange.
     *
     * This should make a lightweight request (e.g., ping, server time) to verify
     * that the exchange is reachable and responding.
     *
     * @throws ConnectionException if connectivity test fails
     */
    protected abstract suspend fun testConnectivity()

    /**
     * Called after successful connection establishment.
     *
     * Subclasses can override this to perform exchange-specific initialization
     * (e.g., subscribe to user data streams, sync timestamps, etc.)
     */
    protected open suspend fun onConnect() {
        // Default: no additional setup needed
    }

    /**
     * Called before disconnection.
     *
     * Subclasses can override this to perform exchange-specific cleanup.
     */
    protected open suspend fun onDisconnect() {
        // Default: no additional cleanup needed
    }

    // ============================================
    // Helper Methods
    // ============================================

    /**
     * Creates the HTTP client with default configuration.
     *
     * Subclasses can override to customize client configuration.
     *
     * @return Configured HttpClient instance
     */
    protected open fun createHttpClient(): HttpClient {
        return HttpClient(CIO) {
            // Timeouts
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 30000
            }

            // JSON content negotiation
            install(ContentNegotiation) {
                json(json)
            }

            // WebSocket support
            install(WebSockets) {
                pingInterval = 30_000 // 30 seconds
                maxFrameSize = Long.MAX_VALUE
            }

            // Logging (configurable)
            if (logger.isDebugEnabled) {
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.INFO
                }
            }

            // Default request configuration
            defaultRequest {
                // Exchange-specific headers can be added by subclasses
            }
        }
    }

    /**
     * Creates the rate limiter with exchange-specific limits.
     *
     * Subclasses should override to set appropriate limits for the exchange.
     *
     * @return Configured RateLimiter instance
     */
    protected open fun createRateLimiter(): RateLimiter {
        // Default: conservative limits (adjust per exchange)
        return RateLimiter(
            requestsPerSecond = 10.0,
            burstCapacity = 20,
            perEndpointLimit = false
        )
    }

    /**
     * Creates the retry policy.
     *
     * Subclasses can override to customize retry behavior.
     *
     * @return Configured RetryPolicy instance
     */
    protected open fun createRetryPolicy(): RetryPolicy {
        return RetryPolicy.DEFAULT
    }

    /**
     * Gets the current server timestamp.
     *
     * This should be implemented by subclasses to return the exchange's current time,
     * or fall back to local time if not available.
     *
     * @return Current timestamp as Instant
     */
    protected open fun getCurrentTimestamp(): Instant {
        return Instant.now()
    }

    /**
     * Generates a unique subscription ID.
     *
     * @return Unique subscription ID
     */
    protected fun generateSubscriptionId(): String {
        return "${exchange.name.lowercase()}_sub_${subscriptionIdCounter.incrementAndGet()}"
    }

    /**
     * Ensures the connector is connected.
     *
     * @throws ConnectionException if not connected
     */
    protected fun ensureConnected() {
        if (!isConnected()) {
            throw ConnectionException(
                message = "Not connected to ${exchange.name}. Call connect() first.",
                exchangeName = exchange.name,
                retryable = false
            )
        }
    }

    /**
     * Records a successful request for metrics.
     *
     * @param durationMs Request duration in milliseconds
     */
    protected fun recordSuccess(durationMs: Long) {
        requestCount.incrementAndGet()
        successCount.incrementAndGet()
        totalRequestTimeMs.addAndGet(durationMs)
    }

    /**
     * Records a failed request for metrics.
     *
     * @param durationMs Request duration in milliseconds
     */
    protected fun recordError(durationMs: Long) {
        requestCount.incrementAndGet()
        errorCount.incrementAndGet()
        totalRequestTimeMs.addAndGet(durationMs)
    }

    /**
     * Gets connector metrics.
     *
     * @return Metrics map
     */
    fun getMetrics(): Map<String, Any> {
        val requests = requestCount.get()
        val avgTime = if (requests > 0) totalRequestTimeMs.get() / requests else 0

        return mapOf(
            "exchange" to exchange.name,
            "connected" to isConnected(),
            "total_requests" to requests,
            "successful_requests" to successCount.get(),
            "failed_requests" to errorCount.get(),
            "success_rate" to if (requests > 0) successCount.get().toDouble() / requests else 0.0,
            "average_request_time_ms" to avgTime,
            "active_subscriptions" to subscriptions.size,
            "rate_limiter" to rateLimiter.getMetrics()
        )
    }

    /**
     * Resets connector metrics.
     */
    fun resetMetrics() {
        requestCount.set(0)
        successCount.set(0)
        errorCount.set(0)
        totalRequestTimeMs.set(0)
        rateLimiter.resetMetrics()
        logger.debug { "${exchange.name} metrics reset" }
    }

    // ============================================
    // String Representation
    // ============================================

    override fun toString(): String {
        return "${exchange.name}Connector(connected=${isConnected()}, " +
                "requests=${requestCount.get()}, " +
                "subscriptions=${subscriptions.size})"
    }
}

