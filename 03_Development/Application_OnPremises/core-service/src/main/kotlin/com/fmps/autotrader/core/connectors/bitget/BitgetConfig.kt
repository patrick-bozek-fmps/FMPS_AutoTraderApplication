package com.fmps.autotrader.core.connectors.bitget

import com.fmps.autotrader.shared.model.ExchangeConfig
import com.fmps.autotrader.shared.model.RateLimitConfig
import com.fmps.autotrader.shared.model.RetryPolicyConfig
import com.fmps.autotrader.shared.model.WebSocketConfig
import com.fmps.autotrader.shared.model.HealthCheckConfig

/**
 * Configuration for Bitget exchange connector
 * 
 * **Bitget Rate Limits:**
 * - General: 10 requests per second
 * - Order endpoints: May have different limits
 * - Weight-based system similar to Binance
 * 
 * **Authentication:**
 * - API Key: Provided in ACCESS-KEY header
 * - Secret Key: Used for signature generation
 * - Passphrase: Additional security layer (ACCESS-PASSPHRASE header)
 * 
 * **Endpoints:**
 * - Production REST: https://api.bitget.com
 * - Production WebSocket: wss://ws.bitget.com/spot/v1/stream
 * - Note: Bitget may not have a separate testnet URL
 * 
 * @property baseExchangeConfig Base exchange configuration
 * @property passphrase API passphrase (required by Bitget)
 * @property recvWindow Request validity window in milliseconds (default: 5000ms)
 * @property timestampOffset Server time offset adjustment in milliseconds
 */
data class BitgetConfig(
    val baseExchangeConfig: ExchangeConfig,
    
    // Bitget-specific fields
    val passphrase: String,
    val recvWindow: Long = 5000,
    val timestampOffset: Long = 0
) {
    
    // Convenience accessors for base config properties
    val exchange get() = baseExchangeConfig.exchange
    val apiKey get() = baseExchangeConfig.apiKey
    val apiSecret get() = baseExchangeConfig.apiSecret
    val baseUrl get() = baseExchangeConfig.baseUrl
    val testnet get() = baseExchangeConfig.testnet
    val timeout get() = baseExchangeConfig.timeoutMs
    val rateLimitConfig get() = baseExchangeConfig.rateLimitConfig
    val retryPolicyConfig get() = baseExchangeConfig.retryPolicyConfig
    val webSocketConfig get() = baseExchangeConfig.webSocketConfig
    val healthCheckConfig get() = baseExchangeConfig.healthCheckConfig
    
    companion object {
        /**
         * Default Bitget testnet/demo configuration
         * 
         * Note: Bitget may use the same endpoints for testnet with special API keys
         */
        fun testnet(apiKey: String, apiSecret: String, passphrase: String): BitgetConfig {
            val baseConfig = ExchangeConfig(
                exchange = com.fmps.autotrader.shared.enums.Exchange.BITGET,
                apiKey = apiKey,
                apiSecret = apiSecret,
                passphrase = passphrase,  // Add passphrase parameter
                baseUrl = "https://api.bitget.com",
                testnet = true,
                rateLimitConfig = RateLimitConfig(
                    requestsPerSecond = 10.0,  // Conservative rate limit
                    burstCapacity = 20,
                    perEndpointLimit = false
                ),
                retryPolicyConfig = RetryPolicyConfig(
                    maxRetries = 3,
                    initialDelayMillis = 1000,
                    maxDelayMillis = 30000,
                    backoffFactor = 2.0,
                    jitterFactor = 0.1
                ),
                webSocketConfig = WebSocketConfig(
                    enabled = true,
                    pingIntervalMs = 30000,
                    reconnectDelayMs = 5000,
                    maxReconnectAttempts = 10,
                    autoReconnect = true
                ),
                healthCheckConfig = HealthCheckConfig(
                    enabled = true,
                    checkIntervalMs = 60000,
                    maxConsecutiveFailures = 3,
                    circuitBreakerResetMs = 60000
                ),
                timeoutMs = 30000,
                connectTimeoutMs = 10000
            )
            
            return BitgetConfig(
                baseExchangeConfig = baseConfig,
                passphrase = passphrase,
                recvWindow = 5000,
                timestampOffset = 0
            )
        }
        
        /**
         * Default Bitget production configuration
         */
        fun production(apiKey: String, apiSecret: String, passphrase: String): BitgetConfig {
            val baseConfig = ExchangeConfig(
                exchange = com.fmps.autotrader.shared.enums.Exchange.BITGET,
                apiKey = apiKey,
                apiSecret = apiSecret,
                passphrase = passphrase,  // Add passphrase parameter
                baseUrl = "https://api.bitget.com",
                testnet = false,
                rateLimitConfig = RateLimitConfig(
                    requestsPerSecond = 8.0,  // More conservative for production
                    burstCapacity = 15,
                    perEndpointLimit = false
                ),
                retryPolicyConfig = RetryPolicyConfig(
                    maxRetries = 3,
                    initialDelayMillis = 1000,
                    maxDelayMillis = 30000,
                    backoffFactor = 2.0,
                    jitterFactor = 0.1
                ),
                webSocketConfig = WebSocketConfig(
                    enabled = true,
                    pingIntervalMs = 30000,
                    reconnectDelayMs = 5000,
                    maxReconnectAttempts = 10,
                    autoReconnect = true
                ),
                healthCheckConfig = HealthCheckConfig(
                    enabled = true,
                    checkIntervalMs = 60000,
                    maxConsecutiveFailures = 3,
                    circuitBreakerResetMs = 60000
                ),
                timeoutMs = 30000,
                connectTimeoutMs = 10000
            )
            
            return BitgetConfig(
                baseExchangeConfig = baseConfig,
                passphrase = passphrase,
                recvWindow = 5000,
                timestampOffset = 0
            )
        }
    }
    
    /**
     * Validates the configuration
     */
    fun validate() {
        require(apiKey.isNotBlank()) { "Bitget API key cannot be blank (got: '$apiKey')" }
        require(apiSecret.isNotBlank()) { "Bitget API secret cannot be blank (got: '${apiSecret.take(5)}...')" }
        require(passphrase.isNotBlank()) { "Bitget passphrase cannot be blank" }
        require(recvWindow in 1000..60000) { "recvWindow must be between 1000 and 60000 ms (got: $recvWindow)" }
        require(timeout > 0) { "Timeout must be positive (got: $timeout)" }
        
        // Validate base config too
        baseExchangeConfig.let {
            require(it.apiKey.isNotBlank()) { "Base config API key cannot be blank" }
            require(it.apiSecret.isNotBlank()) { "Base config API secret cannot be blank" }
        }
    }
}

