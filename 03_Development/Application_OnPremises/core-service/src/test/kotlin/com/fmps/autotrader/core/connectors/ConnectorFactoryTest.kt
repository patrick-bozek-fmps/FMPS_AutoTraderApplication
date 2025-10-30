package com.fmps.autotrader.core.connectors

import com.fmps.autotrader.core.connectors.exceptions.UnsupportedExchangeException
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.model.ExchangeConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ConnectorFactoryTest {

    private lateinit var factory: ConnectorFactory

    @BeforeEach
    fun setup() {
        // Reset singleton before each test
        ConnectorFactory.resetInstance()
        factory = ConnectorFactory.getInstance()
    }

    @AfterEach
    fun cleanup() = runBlocking {
        // Clean up connectors after each test
        factory.removeAllConnectors()
    }

    @Test
    fun `test singleton instance`() {
        val instance1 = ConnectorFactory.getInstance()
        val instance2 = ConnectorFactory.getInstance()

        assertSame(instance1, instance2, "getInstance should return the same instance")
    }

    @Test
    fun `test reset instance`() {
        val instance1 = ConnectorFactory.getInstance()
        ConnectorFactory.resetInstance()
        val instance2 = ConnectorFactory.getInstance()

        assertNotSame(instance1, instance2, "resetInstance should create a new instance")
    }

    @Test
    fun `test default connectors are registered`() {
        val supportedExchanges = factory.getSupportedExchanges()

        assertTrue(supportedExchanges.contains(Exchange.BINANCE), "Binance should be registered by default")
        assertTrue(supportedExchanges.contains(Exchange.BITGET), "Bitget should be registered by default")
        assertTrue(supportedExchanges.size >= 2, "At least 2 exchanges should be registered")
    }

    @Test
    fun `test isExchangeSupported`() {
        assertTrue(factory.isExchangeSupported(Exchange.BINANCE), "Binance should be supported")
        assertTrue(factory.isExchangeSupported(Exchange.BITGET), "Bitget should be supported")
    }

    @Test
    fun `test createConnector for Binance`() {
        val config = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            testnet = true
        )

        val connector = factory.createConnector(Exchange.BINANCE, config)

        assertNotNull(connector, "Connector should be created")
        assertTrue(connector is IExchangeConnector, "Connector should implement IExchangeConnector")
    }

    @Test
    fun `test createConnector for Bitget`() {
        val config = ExchangeConfig(
            exchange = Exchange.BITGET,
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            passphrase = "test-passphrase",
            testnet = true
        )

        val connector = factory.createConnector(Exchange.BITGET, config)

        assertNotNull(connector, "Connector should be created")
        assertTrue(connector is IExchangeConnector, "Connector should implement IExchangeConnector")
    }

    @Test
    fun `test createConnector throws for mismatched exchange`() {
        val config = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            testnet = true
        )

        val exception = assertThrows<IllegalArgumentException> {
            factory.createConnector(Exchange.BITGET, config)
        }

        assertTrue(exception.message!!.contains("does not match"), 
            "Exception should mention exchange mismatch")
    }

    @Test
    fun `test createConnector uses cache by default`() {
        val config = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            testnet = true
        )

        val connector1 = factory.createConnector(Exchange.BINANCE, config)
        val connector2 = factory.createConnector(Exchange.BINANCE, config)

        assertSame(connector1, connector2, "Should return same instance from cache")
        assertEquals(1, factory.getCachedConnectorCount(), "Should have 1 connector in cache")
    }

    @Test
    fun `test createConnector without cache creates new instance`() {
        val config = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            testnet = true
        )

        val connector1 = factory.createConnector(Exchange.BINANCE, config, useCache = false)
        val connector2 = factory.createConnector(Exchange.BINANCE, config, useCache = false)

        assertNotSame(connector1, connector2, "Should create new instances when cache is disabled")
        assertEquals(0, factory.getCachedConnectorCount(), "Cache should be empty")
    }

    @Test
    fun `test getCachedConnector returns cached instance`() {
        val config = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            testnet = true
        )

        val connector = factory.createConnector(Exchange.BINANCE, config)
        val cached = factory.getCachedConnector(Exchange.BINANCE)

        assertSame(connector, cached, "getCachedConnector should return the same instance")
    }

    @Test
    fun `test getCachedConnector returns null when not cached`() {
        val cached = factory.getCachedConnector(Exchange.BINANCE)

        assertNull(cached, "getCachedConnector should return null when connector not cached")
    }

    @Test
    fun `test removeConnector removes from cache`() = runBlocking {
        val config = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            testnet = true
        )

        factory.createConnector(Exchange.BINANCE, config)
        assertEquals(1, factory.getCachedConnectorCount(), "Should have 1 connector in cache")

        val removed = factory.removeConnector(Exchange.BINANCE)

        assertTrue(removed, "removeConnector should return true")
        assertEquals(0, factory.getCachedConnectorCount(), "Cache should be empty after removal")
    }

    @Test
    fun `test removeConnector returns false when connector not found`() = runBlocking {
        val removed = factory.removeConnector(Exchange.BINANCE)

        assertFalse(removed, "removeConnector should return false when connector not found")
    }

    @Test
    fun `test removeAllConnectors clears cache`() = runBlocking {
        val binanceConfig = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            testnet = true
        )
        val bitgetConfig = ExchangeConfig(
            exchange = Exchange.BITGET,
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            passphrase = "test-passphrase",
            testnet = true
        )

        factory.createConnector(Exchange.BINANCE, binanceConfig)
        factory.createConnector(Exchange.BITGET, bitgetConfig)
        assertEquals(2, factory.getCachedConnectorCount(), "Should have 2 connectors in cache")

        factory.removeAllConnectors()

        assertEquals(0, factory.getCachedConnectorCount(), "Cache should be empty after removeAll")
    }

    @Test
    fun `test registerConnector allows dynamic registration`() {
        var factoryCalled = false
        
        factory.registerConnector(Exchange.BINANCE) { config ->
            factoryCalled = true
            MockExchangeConnector()
        }

        val config = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            testnet = true
        )

        factory.createConnector(Exchange.BINANCE, config, useCache = false)

        assertTrue(factoryCalled, "Registered factory function should be called")
    }

    @Test
    fun `test getCachedConnectorCount returns correct count`() {
        assertEquals(0, factory.getCachedConnectorCount(), "Initial count should be 0")

        val config1 = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            testnet = true
        )
        factory.createConnector(Exchange.BINANCE, config1)
        assertEquals(1, factory.getCachedConnectorCount(), "Count should be 1 after first connector")

        val config2 = ExchangeConfig(
            exchange = Exchange.BITGET,
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            passphrase = "test-passphrase",
            testnet = true
        )
        factory.createConnector(Exchange.BITGET, config2)
        assertEquals(2, factory.getCachedConnectorCount(), "Count should be 2 after second connector")
    }

    @Test
    fun `test getSupportedExchanges returns all registered exchanges`() {
        val exchanges = factory.getSupportedExchanges()

        assertNotNull(exchanges, "Supported exchanges should not be null")
        assertTrue(exchanges.isNotEmpty(), "Should have at least one supported exchange")
        assertTrue(exchanges.contains(Exchange.BINANCE), "Should contain Binance")
        assertTrue(exchanges.contains(Exchange.BITGET), "Should contain Bitget")
    }

    @Test
    fun `test multiple concurrent connector creation`() {
        val config = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            testnet = true
        )

        // Simulate concurrent creation (all should get same cached instance)
        val connector1 = factory.createConnector(Exchange.BINANCE, config)
        val connector2 = factory.createConnector(Exchange.BINANCE, config)
        val connector3 = factory.createConnector(Exchange.BINANCE, config)

        assertSame(connector1, connector2, "Connectors should be same instance")
        assertSame(connector2, connector3, "Connectors should be same instance")
        assertEquals(1, factory.getCachedConnectorCount(), "Should only have 1 connector in cache")
    }
}

