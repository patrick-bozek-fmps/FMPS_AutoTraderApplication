package com.fmps.autotrader.core.connectors.binance

import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.model.ExchangeConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for BinanceConnector (basic functionality)
 * 
 * Note: Integration tests are in BinanceConnectorIntegrationTest.kt
 */
class BinanceConnectorTest {

    private lateinit var connector: BinanceConnector
    private lateinit var config: ExchangeConfig

    @BeforeEach
    fun setup() {
        connector = BinanceConnector()
        config = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            testnet = true
        )
    }

    @Test
    fun `test connector initialization`() {
        assertNotNull(connector)
        assertEquals(Exchange.BINANCE, connector.getExchange())
    }

    @Test
    fun `test configure with valid config`() {
        // Should not throw exception
        connector.configure(config)
        
        // Note: We can't easily test internal state without integration tests
        assertNotNull(connector)
    }

    @Test
    fun `test getExchange returns BINANCE`() {
        assertEquals(Exchange.BINANCE, connector.getExchange())
    }

    @Test
    fun `test isConnected returns false before connect`() = runBlocking {
        assertFalse(connector.isConnected())
    }

    @Test
    fun `test disconnect on non-connected connector is safe`() = runBlocking {
        // Should not throw exception
        connector.disconnect()
        assertFalse(connector.isConnected())
    }

    @Test
    fun `test configure with Binance config`() {
        val binanceConfig = BinanceConfig.testnet(
            apiKey = "test-key",
            apiSecret = "test-secret"
        )
        
        // Should handle BinanceConfig directly
        connector.configure(binanceConfig.baseExchangeConfig)
        
        assertNotNull(connector)
    }

    @Test
    fun `test multiple configure calls allowed`() {
        connector.configure(config)
        
        val newConfig = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = "new-key",
            apiSecret = "new-secret",
            testnet = true
        )
        
        // Should allow reconfiguration
        connector.configure(newConfig)
        
        assertNotNull(connector)
    }
}

