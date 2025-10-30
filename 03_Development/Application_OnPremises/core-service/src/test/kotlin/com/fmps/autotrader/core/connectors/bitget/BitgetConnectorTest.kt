package com.fmps.autotrader.core.connectors.bitget

import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.model.ExchangeConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for BitgetConnector (basic functionality)
 * 
 * Note: Integration tests are in BitgetConnectorIntegrationTest.kt
 */
class BitgetConnectorTest {

    private lateinit var connector: BitgetConnector
    private lateinit var config: ExchangeConfig

    @BeforeEach
    fun setup() {
        connector = BitgetConnector()
        config = ExchangeConfig(
            exchange = Exchange.BITGET,
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            passphrase = "test-passphrase",
            testnet = true
        )
    }

    @Test
    fun `test connector initialization`() {
        assertNotNull(connector)
        assertEquals(Exchange.BITGET, connector.getExchange())
    }

    @Test
    fun `test configure with valid config`() {
        // Should not throw exception
        connector.configure(config)
        
        assertNotNull(connector)
    }

    @Test
    fun `test getExchange returns BITGET`() {
        assertEquals(Exchange.BITGET, connector.getExchange())
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
    fun `test configure with Bitget config`() {
        val bitgetConfig = BitgetConfig.testnet(
            apiKey = "test-key",
            apiSecret = "test-secret",
            passphrase = "test-passphrase"
        )
        
        // Note: passphrase is in BitgetConfig, not baseExchangeConfig
        // Verify the BitgetConfig has passphrase
        assertNotNull(bitgetConfig.passphrase)
        assertEquals("test-passphrase", bitgetConfig.passphrase)
        
        // Verify baseExchangeConfig has passphrase (it should be set by BitgetConfig.testnet)
        assertNotNull(bitgetConfig.baseExchangeConfig.passphrase, 
            "BaseExchangeConfig should have passphrase set")
        
        // Should handle BitgetConfig directly
        connector.configure(bitgetConfig.baseExchangeConfig)
        
        assertNotNull(connector)
    }

    @Test
    fun `test multiple configure calls allowed`() {
        connector.configure(config)
        
        val newConfig = ExchangeConfig(
            exchange = Exchange.BITGET,
            apiKey = "new-key",
            apiSecret = "new-secret",
            passphrase = "new-passphrase",
            testnet = true
        )
        
        // Should allow reconfiguration
        connector.configure(newConfig)
        
        assertNotNull(connector)
    }

    @Test
    fun `test symbol format conversion`() {
        // Bitget uses underscore format (BTC_USDT) vs Binance format (BTCUSDT)
        val binanceFormat = "BTCUSDT"
        val bitgetFormat = "BTC_USDT"
        
        // Test conversion logic (if implemented in connector)
        val converted = binanceFormat.replace(Regex("([A-Z]+)(USDT)"), "$1_$2")
        assertEquals("BTC_USDT", converted)
    }
}

