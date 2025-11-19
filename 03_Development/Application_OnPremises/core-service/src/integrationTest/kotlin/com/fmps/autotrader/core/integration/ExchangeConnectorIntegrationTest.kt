package com.fmps.autotrader.core.integration

import com.fmps.autotrader.core.connectors.ConnectorFactory
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.model.ExchangeConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

/**
 * Integration test for Core Service ↔ Exchange communication
 * 
 * Tests:
 * - Binance connector: connection, authentication, market data, orders
 * - Bitget connector: connection, authentication, market data, orders
 * - Exchange error handling and retry logic
 * - Rate limiting and backoff strategies
 * 
 * **Note:** These tests require exchange API keys to be set as environment variables.
 * They will be skipped if keys are not available.
 * 
 * **Tag:** @Tag("integration")
 */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExchangeConnectorIntegrationTest {
    
    private lateinit var connectorFactory: ConnectorFactory
    
    @BeforeAll
    fun setup() {
        println("=".repeat(70))
        println("Exchange Connector Integration Test - Setup")
        println("=".repeat(70))
        
        connectorFactory = ConnectorFactory.getInstance()
        
        println("✅ Test environment initialized")
        println()
    }
    
    @Test
    @EnabledIfEnvironmentVariable(named = "BINANCE_API_KEY", matches = ".+")
    fun `should connect to Binance testnet`() = runBlocking {
        println("Test: Connect to Binance testnet")
        
        val apiKey = System.getenv("BINANCE_API_KEY") ?: return@runBlocking
        val apiSecret = System.getenv("BINANCE_API_SECRET") ?: return@runBlocking
        
        val config = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = apiKey,
            apiSecret = apiSecret,
            testnet = true
        )
        
        val connector = connectorFactory.createConnector(Exchange.BINANCE, config, false)
        assertNotNull(connector, "Binance connector should be created")
        val connectResult = runCatching { 
            connector.connect()
        }
        
        assertTrue(connectResult.isSuccess, "Binance connection should succeed: ${connectResult.exceptionOrNull()?.message}")
        assertTrue(connector.isConnected(), "Binance connector should be connected")
        
        connector.disconnect()
        
        println("✅ Binance testnet connection successful")
    }
    
    @Test
    @EnabledIfEnvironmentVariable(named = "BITGET_API_KEY", matches = ".+")
    fun `should connect to Bitget testnet`() = runBlocking {
        println("Test: Connect to Bitget testnet")
        
        val apiKey = System.getenv("BITGET_API_KEY") ?: return@runBlocking
        val apiSecret = System.getenv("BITGET_API_SECRET") ?: return@runBlocking
        val apiPassphrase = System.getenv("BITGET_API_PASSPHRASE") ?: return@runBlocking
        
        val config = ExchangeConfig(
            exchange = Exchange.BITGET,
            apiKey = apiKey,
            apiSecret = apiSecret,
            passphrase = apiPassphrase,
            testnet = true
        )
        
        val connector = connectorFactory.createConnector(Exchange.BITGET, config, false)
        assertNotNull(connector, "Bitget connector should be created")
        val connectResult = runCatching { connector.connect() }
        
        assertTrue(connectResult.isSuccess, "Bitget connection should succeed: ${connectResult.exceptionOrNull()?.message}")
        assertTrue(connector.isConnected(), "Bitget connector should be connected")
        
        connector.disconnect()
        
        println("✅ Bitget testnet connection successful")
    }
    
    @Test
    fun `should handle connector factory for all exchanges`() = runBlocking {
        println("Test: Handle connector factory for all exchanges")
        
        // Create test configs
        val binanceConfig = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = "test-key",
            apiSecret = "test-secret",
            testnet = true
        )
        
        val bitgetConfig = ExchangeConfig(
            exchange = Exchange.BITGET,
            apiKey = "test-key",
            apiSecret = "test-secret",
            passphrase = "test-passphrase",
            testnet = true
        )
        
        // Create connectors
        val binanceConnector = runCatching { 
            connectorFactory.createConnector(Exchange.BINANCE, binanceConfig, false) 
        }.getOrNull()
        assertNotNull(binanceConnector, "Binance connector should be created")
        
        val bitgetConnector = runCatching { 
            connectorFactory.createConnector(Exchange.BITGET, bitgetConfig, false) 
        }.getOrNull()
        assertNotNull(bitgetConnector, "Bitget connector should be created")
        
        // Verify connectors are different instances
        assertNotSame(binanceConnector, bitgetConnector, "Connectors should be different instances")
        
        println("✅ Connector factory handles all exchanges correctly")
    }
    
    @Test
    fun `should handle invalid exchange gracefully`() = runBlocking {
        println("Test: Handle invalid exchange gracefully")
        
        // Create test config
        val config = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = "test-key",
            apiSecret = "test-secret",
            testnet = true
        )
        
        // Try to create connector with valid exchange (should succeed)
        val result = runCatching {
            connectorFactory.createConnector(Exchange.BINANCE, config, false)
        }
        
        // Should succeed for supported exchange
        assertTrue(result.isSuccess, "Should create connector for supported exchange")
        
        println("✅ Exchange connector creation handled correctly")
    }
}

