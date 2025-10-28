package com.fmps.autotrader.shared.model

import com.fmps.autotrader.shared.enums.Exchange
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for configuration models.
 */
class ExchangeConfigTest {

    // ============================================
    // RateLimitConfig Tests
    // ============================================

    @Test
    fun `RateLimitConfig with valid values`() {
        val config = RateLimitConfig(
            requestsPerSecond = 20.0,
            burstCapacity = 40,
            perEndpointLimit = true
        )

        assertEquals(20.0, config.requestsPerSecond)
        assertEquals(40, config.burstCapacity)
        assertTrue(config.perEndpointLimit)
    }

    @Test
    fun `RateLimitConfig uses defaults`() {
        val config = RateLimitConfig()

        assertEquals(10.0, config.requestsPerSecond)
        assertEquals(20, config.burstCapacity)
        assertFalse(config.perEndpointLimit)
    }

    @Test
    fun `RateLimitConfig rejects negative requestsPerSecond`() {
        assertThrows(IllegalArgumentException::class.java) {
            RateLimitConfig(requestsPerSecond = -1.0)
        }
    }

    @Test
    fun `RateLimitConfig rejects zero burstCapacity`() {
        assertThrows(IllegalArgumentException::class.java) {
            RateLimitConfig(burstCapacity = 0)
        }
    }

    // ============================================
    // RetryPolicyConfig Tests
    // ============================================

    @Test
    fun `RetryPolicyConfig with valid values`() {
        val config = RetryPolicyConfig(
            maxRetries = 5,
            initialDelayMillis = 2000,
            maxDelayMillis = 60000,
            backoffFactor = 3.0,
            jitterFactor = 0.2
        )

        assertEquals(5, config.maxRetries)
        assertEquals(2000, config.initialDelayMillis)
        assertEquals(60000, config.maxDelayMillis)
        assertEquals(3.0, config.backoffFactor)
        assertEquals(0.2, config.jitterFactor)
    }

    @Test
    fun `RetryPolicyConfig uses defaults`() {
        val config = RetryPolicyConfig()

        assertEquals(3, config.maxRetries)
        assertEquals(1000, config.initialDelayMillis)
        assertEquals(30000, config.maxDelayMillis)
        assertEquals(2.0, config.backoffFactor)
        assertEquals(0.1, config.jitterFactor)
    }

    @Test
    fun `RetryPolicyConfig rejects maxDelayMillis less than initialDelayMillis`() {
        assertThrows(IllegalArgumentException::class.java) {
            RetryPolicyConfig(
                initialDelayMillis = 5000,
                maxDelayMillis = 1000
            )
        }
    }

    @Test
    fun `RetryPolicyConfig rejects backoffFactor less than 1`() {
        assertThrows(IllegalArgumentException::class.java) {
            RetryPolicyConfig(backoffFactor = 0.5)
        }
    }

    @Test
    fun `RetryPolicyConfig rejects jitterFactor out of range`() {
        assertThrows(IllegalArgumentException::class.java) {
            RetryPolicyConfig(jitterFactor = 1.5)
        }
    }

    // ============================================
    // WebSocketConfig Tests
    // ============================================

    @Test
    fun `WebSocketConfig with valid values`() {
        val config = WebSocketConfig(
            enabled = false,
            pingIntervalMs = 15000,
            reconnectDelayMs = 3000,
            maxReconnectAttempts = 10,
            autoReconnect = false
        )

        assertFalse(config.enabled)
        assertEquals(15000, config.pingIntervalMs)
        assertEquals(3000, config.reconnectDelayMs)
        assertEquals(10, config.maxReconnectAttempts)
        assertFalse(config.autoReconnect)
    }

    @Test
    fun `WebSocketConfig uses defaults`() {
        val config = WebSocketConfig()

        assertTrue(config.enabled)
        assertEquals(30000, config.pingIntervalMs)
        assertEquals(5000, config.reconnectDelayMs)
        assertEquals(5, config.maxReconnectAttempts)
        assertTrue(config.autoReconnect)
    }

    @Test
    fun `WebSocketConfig rejects zero pingIntervalMs`() {
        assertThrows(IllegalArgumentException::class.java) {
            WebSocketConfig(pingIntervalMs = 0)
        }
    }

    // ============================================
    // HealthCheckConfig Tests
    // ============================================

    @Test
    fun `HealthCheckConfig with valid values`() {
        val config = HealthCheckConfig(
            enabled = false,
            checkIntervalMs = 60000,
            maxConsecutiveFailures = 10,
            circuitBreakerResetMs = 120000
        )

        assertFalse(config.enabled)
        assertEquals(60000, config.checkIntervalMs)
        assertEquals(10, config.maxConsecutiveFailures)
        assertEquals(120000, config.circuitBreakerResetMs)
    }

    @Test
    fun `HealthCheckConfig uses defaults`() {
        val config = HealthCheckConfig()

        assertTrue(config.enabled)
        assertEquals(30000, config.checkIntervalMs)
        assertEquals(5, config.maxConsecutiveFailures)
        assertEquals(60000, config.circuitBreakerResetMs)
    }

    @Test
    fun `HealthCheckConfig rejects zero checkIntervalMs`() {
        assertThrows(IllegalArgumentException::class.java) {
            HealthCheckConfig(checkIntervalMs = 0)
        }
    }

    // ============================================
    // ExchangeConfig Tests
    // ============================================

    @Test
    fun `ExchangeConfig with minimal required fields`() {
        val config = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = "test-key",
            apiSecret = "test-secret"
        )

        assertEquals(Exchange.BINANCE, config.exchange)
        assertEquals("test-key", config.apiKey)
        assertEquals("test-secret", config.apiSecret)
        assertNull(config.passphrase)
        assertNull(config.baseUrl)
        assertTrue(config.testnet)
        assertEquals(30000, config.timeoutMs)
        assertEquals(10000, config.connectTimeoutMs)
    }

    @Test
    fun `ExchangeConfig with all fields`() {
        val config = ExchangeConfig(
            exchange = Exchange.BITGET,
            apiKey = "key",
            apiSecret = "secret",
            passphrase = "pass",
            baseUrl = "https://testnet.bitget.com",
            testnet = true,
            rateLimitConfig = RateLimitConfig(requestsPerSecond = 15.0),
            retryPolicyConfig = RetryPolicyConfig(maxRetries = 5),
            webSocketConfig = WebSocketConfig(enabled = false),
            healthCheckConfig = HealthCheckConfig(enabled = false),
            timeoutMs = 60000,
            connectTimeoutMs = 15000
        )

        assertEquals(Exchange.BITGET, config.exchange)
        assertEquals("pass", config.passphrase)
        assertEquals("https://testnet.bitget.com", config.baseUrl)
        assertEquals(15.0, config.rateLimitConfig.requestsPerSecond)
        assertEquals(5, config.retryPolicyConfig.maxRetries)
        assertFalse(config.webSocketConfig.enabled)
        assertFalse(config.healthCheckConfig.enabled)
        assertEquals(60000, config.timeoutMs)
        assertEquals(15000, config.connectTimeoutMs)
    }

    @Test
    fun `ExchangeConfig rejects blank apiKey`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExchangeConfig(
                exchange = Exchange.BINANCE,
                apiKey = "",
                apiSecret = "secret"
            )
        }
    }

    @Test
    fun `ExchangeConfig rejects blank apiSecret`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExchangeConfig(
                exchange = Exchange.BINANCE,
                apiKey = "key",
                apiSecret = ""
            )
        }
    }

    @Test
    fun `ExchangeConfig rejects zero timeoutMs`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExchangeConfig(
                exchange = Exchange.BINANCE,
                apiKey = "key",
                apiSecret = "secret",
                timeoutMs = 0
            )
        }
    }

    @Test
    fun `ExchangeConfig forTestnet creates testnet config`() {
        val prodConfig = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = "key",
            apiSecret = "secret",
            testnet = false
        )

        val testnetConfig = prodConfig.forTestnet()

        assertTrue(testnetConfig.testnet)
        assertEquals(prodConfig.exchange, testnetConfig.exchange)
        assertEquals(prodConfig.apiKey, testnetConfig.apiKey)
        assertEquals(prodConfig.apiSecret, testnetConfig.apiSecret)
    }

    @Test
    fun `ExchangeConfig forProduction creates production config`() {
        val testnetConfig = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = "key",
            apiSecret = "secret",
            testnet = true
        )

        val prodConfig = testnetConfig.forProduction()

        assertFalse(prodConfig.testnet)
        assertEquals(testnetConfig.exchange, prodConfig.exchange)
        assertEquals(testnetConfig.apiKey, prodConfig.apiKey)
        assertEquals(testnetConfig.apiSecret, prodConfig.apiSecret)
    }
}

