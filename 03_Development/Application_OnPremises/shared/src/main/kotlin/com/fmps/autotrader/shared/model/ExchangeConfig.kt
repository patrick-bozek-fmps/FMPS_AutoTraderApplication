package com.fmps.autotrader.shared.model

import com.fmps.autotrader.shared.enums.Exchange
import kotlinx.serialization.Serializable

/**
 * Rate limiting configuration for exchange API requests.
 *
 * @param requestsPerSecond Maximum requests per second
 * @param burstCapacity Maximum burst capacity (token bucket size)
 * @param perEndpointLimit Whether to apply limits per endpoint
 */
@Serializable
data class RateLimitConfig(
    val requestsPerSecond: Double = 10.0,
    val burstCapacity: Int = 20,
    val perEndpointLimit: Boolean = false
) {
    init {
        require(requestsPerSecond > 0) { "requestsPerSecond must be positive" }
        require(burstCapacity > 0) { "burstCapacity must be positive" }
    }
}

/**
 * Retry policy configuration for failed requests.
 *
 * @param maxRetries Maximum number of retry attempts
 * @param initialDelayMillis Initial delay before first retry
 * @param maxDelayMillis Maximum delay between retries
 * @param backoffFactor Exponential backoff multiplier
 * @param jitterFactor Random jitter factor (0.0 to 1.0)
 */
@Serializable
data class RetryPolicyConfig(
    val maxRetries: Int = 3,
    val initialDelayMillis: Long = 1000,
    val maxDelayMillis: Long = 30000,
    val backoffFactor: Double = 2.0,
    val jitterFactor: Double = 0.1
) {
    init {
        require(maxRetries >= 0) { "maxRetries must be non-negative" }
        require(initialDelayMillis > 0) { "initialDelayMillis must be positive" }
        require(maxDelayMillis >= initialDelayMillis) { "maxDelayMillis must be >= initialDelayMillis" }
        require(backoffFactor >= 1.0) { "backoffFactor must be >= 1.0" }
        require(jitterFactor in 0.0..1.0) { "jitterFactor must be between 0.0 and 1.0" }
    }
}

/**
 * WebSocket configuration for real-time data streaming.
 *
 * @param enabled Whether WebSocket streaming is enabled
 * @param pingIntervalMs Ping interval for keep-alive
 * @param reconnectDelayMs Delay before attempting reconnection
 * @param maxReconnectAttempts Maximum reconnection attempts
 * @param autoReconnect Whether to automatically reconnect on disconnect
 */
@Serializable
data class WebSocketConfig(
    val enabled: Boolean = true,
    val pingIntervalMs: Long = 30000,
    val reconnectDelayMs: Long = 5000,
    val maxReconnectAttempts: Int = 5,
    val autoReconnect: Boolean = true
) {
    init {
        require(pingIntervalMs > 0) { "pingIntervalMs must be positive" }
        require(reconnectDelayMs > 0) { "reconnectDelayMs must be positive" }
        require(maxReconnectAttempts > 0) { "maxReconnectAttempts must be positive" }
    }
}

/**
 * Health check configuration for connection monitoring.
 *
 * @param enabled Whether health monitoring is enabled
 * @param checkIntervalMs Interval between health checks
 * @param maxConsecutiveFailures Max failures before circuit breaker opens
 * @param circuitBreakerResetMs Time to wait before retrying after circuit breaker opens
 */
@Serializable
data class HealthCheckConfig(
    val enabled: Boolean = true,
    val checkIntervalMs: Long = 30000,
    val maxConsecutiveFailures: Int = 5,
    val circuitBreakerResetMs: Long = 60000
) {
    init {
        require(checkIntervalMs > 0) { "checkIntervalMs must be positive" }
        require(maxConsecutiveFailures > 0) { "maxConsecutiveFailures must be positive" }
        require(circuitBreakerResetMs > 0) { "circuitBreakerResetMs must be positive" }
    }
}

/**
 * Comprehensive configuration for an exchange connector.
 *
 * @param exchange The exchange type (BINANCE, BITGET, etc.)
 * @param apiKey API key for authentication
 * @param apiSecret API secret for authentication
 * @param passphrase Optional passphrase (required by some exchanges like Bitget)
 * @param baseUrl Optional custom base URL (useful for testnet)
 * @param testnet Whether to use testnet environment
 * @param rateLimitConfig Rate limiting configuration
 * @param retryPolicyConfig Retry policy configuration
 * @param webSocketConfig WebSocket streaming configuration
 * @param healthCheckConfig Health monitoring configuration
 * @param timeoutMs Request timeout in milliseconds
 * @param connectTimeoutMs Connection timeout in milliseconds
 */
@Serializable
data class ExchangeConfig(
    val exchange: Exchange,
    val apiKey: String,
    val apiSecret: String,
    val passphrase: String? = null,
    val baseUrl: String? = null,
    val testnet: Boolean = true,
    val rateLimitConfig: RateLimitConfig = RateLimitConfig(),
    val retryPolicyConfig: RetryPolicyConfig = RetryPolicyConfig(),
    val webSocketConfig: WebSocketConfig = WebSocketConfig(),
    val healthCheckConfig: HealthCheckConfig = HealthCheckConfig(),
    val timeoutMs: Long = 30000,
    val connectTimeoutMs: Long = 10000
) {
    init {
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
        require(apiSecret.isNotBlank()) { "apiSecret must not be blank" }
        require(timeoutMs > 0) { "timeoutMs must be positive" }
        require(connectTimeoutMs > 0) { "connectTimeoutMs must be positive" }
    }

    /**
     * Creates a copy of this config for testnet environment.
     */
    fun forTestnet(): ExchangeConfig = copy(testnet = true)

    /**
     * Creates a copy of this config for production environment.
     */
    fun forProduction(): ExchangeConfig = copy(testnet = false)
}

