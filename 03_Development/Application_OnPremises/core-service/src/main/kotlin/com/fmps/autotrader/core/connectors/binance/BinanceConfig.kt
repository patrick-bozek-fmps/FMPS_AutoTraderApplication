package com.fmps.autotrader.core.connectors.binance

import com.fmps.autotrader.shared.model.ExchangeConfig
import com.fmps.autotrader.shared.model.RateLimitConfig
import com.fmps.autotrader.shared.model.RetryPolicyConfig
import com.fmps.autotrader.shared.model.WebSocketConfig
import com.fmps.autotrader.shared.model.HealthCheckConfig
import java.math.BigDecimal

/**
 * Configuration for Binance exchange connector
 * 
 * **Binance Rate Limits:**
 * - General: 1200 requests per minute
 * - Order endpoints: 10 orders per second per account
 * - Weight-based system: Different endpoints have different weights
 * 
 * **Authentication:**
 * - API Key: Provided in X-MBX-APIKEY header
 * - Secret Key: Used for HMAC SHA256 signature
 * 
 * **Endpoints:**
 * - Testnet REST: https://testnet.binance.vision
 * - Testnet WebSocket: wss://testnet.binance.vision/ws
 * - Production REST: https://api.binance.com
 * - Production WebSocket: wss://stream.binance.com:9443/ws
 * 
 * @property baseExchangeConfig Base exchange configuration
 * @property recvWindow Request validity window in milliseconds (default: 5000ms)
 * @property timestampOffset Server time offset adjustment in milliseconds
 * @property weightMultiplier Multiplier for weight-based rate limiting (default: 1.0)
 * @property enableTestOrders Whether to enable test order mode
 */
data class BinanceConfig(
    val baseExchangeConfig: ExchangeConfig,
    
    // Binance-specific fields
    val recvWindow: Long = 5000,
    val timestampOffset: Long = 0,
    val weightMultiplier: Double = 1.0,
    val enableTestOrders: Boolean = false
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
         * Default Binance testnet configuration
         */
        fun testnet(apiKey: String, apiSecret: String): BinanceConfig {
            val baseConfig = ExchangeConfig(
                exchange = com.fmps.autotrader.shared.enums.Exchange.BINANCE,
                apiKey = apiKey,
                apiSecret = apiSecret,
                baseUrl = "https://testnet.binance.vision",
                testnet = true,
                rateLimitConfig = RateLimitConfig(
                    requestsPerSecond = 20.0,  // Conservative: 1200/60 = 20 per second
                    burstCapacity = 50,
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
            
            return BinanceConfig(
                baseExchangeConfig = baseConfig,
                recvWindow = 5000,
                timestampOffset = 0,
                weightMultiplier = 1.0,
                enableTestOrders = true
            )
        }
        
        /**
         * Default Binance production configuration
         */
        fun production(apiKey: String, apiSecret: String): BinanceConfig {
            val baseConfig = ExchangeConfig(
                exchange = com.fmps.autotrader.shared.enums.Exchange.BINANCE,
                apiKey = apiKey,
                apiSecret = apiSecret,
                baseUrl = "https://api.binance.com",
                testnet = false,
                rateLimitConfig = RateLimitConfig(
                    requestsPerSecond = 15.0,  // More conservative for production
                    burstCapacity = 30,
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
            
            return BinanceConfig(
                baseExchangeConfig = baseConfig,
                recvWindow = 5000,
                timestampOffset = 0,
                weightMultiplier = 1.0,
                enableTestOrders = false
            )
        }
    }
    
    /**
     * Validates the configuration
     */
    fun validate() {
        require(apiKey.isNotBlank()) { "API key cannot be blank" }
        require(apiSecret.isNotBlank()) { "API secret cannot be blank" }
        require(recvWindow in 1000..60000) { "recvWindow must be between 1000 and 60000 ms" }
        require(timeout > 0) { "Timeout must be positive" }
        require(weightMultiplier > 0) { "Weight multiplier must be positive" }
    }
}

