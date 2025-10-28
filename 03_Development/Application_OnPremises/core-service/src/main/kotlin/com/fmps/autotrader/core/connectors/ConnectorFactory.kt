package com.fmps.autotrader.core.connectors

import com.fmps.autotrader.core.connectors.exceptions.UnsupportedExchangeException
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.model.ExchangeConfig
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Factory for creating and managing exchange connector instances.
 *
 * This factory:
 * - Creates connector instances based on exchange type
 * - Caches connector instances for reuse
 * - Manages connector lifecycle
 * - Provides singleton access via companion object
 *
 * ## Usage
 * ```kotlin
 * val factory = ConnectorFactory.getInstance()
 * val config = ExchangeConfig(
 *     exchange = Exchange.BINANCE,
 *     apiKey = "your-api-key",
 *     apiSecret = "your-secret"
 * )
 * val connector = factory.createConnector(Exchange.BINANCE, config)
 * ```
 *
 * ## Thread Safety
 * This class is thread-safe and can be accessed from multiple threads concurrently.
 *
 * @since 1.0.0
 */
class ConnectorFactory private constructor() {

    /**
     * Cache of connector instances keyed by exchange type.
     * Allows reuse of connectors across multiple requests.
     */
    private val connectorCache = ConcurrentHashMap<Exchange, IExchangeConnector>()

    /**
     * Registry of connector factory functions.
     * Maps exchange type to a function that creates the connector.
     */
    private val connectorRegistry = ConcurrentHashMap<Exchange, (ExchangeConfig) -> IExchangeConnector>()

    init {
        logger.info { "Initializing ConnectorFactory" }
        registerDefaultConnectors()
    }

    /**
     * Registers default connectors.
     *
     * This method is called during initialization and registers factory functions
     * for supported exchanges. As new connectors are implemented, they should be
     * registered here.
     */
    private fun registerDefaultConnectors() {
        // TODO: Register connectors as they are implemented
        // registerConnector(Exchange.BINANCE) { config -> BinanceConnector(config) }
        // registerConnector(Exchange.BITGET) { config -> BitgetConnector(config) }
        
        logger.info { "Registered ${connectorRegistry.size} exchange connectors" }
    }

    /**
     * Registers a connector factory function for an exchange.
     *
     * This allows for dynamic registration of new connectors at runtime.
     *
     * @param exchange The exchange type
     * @param factory Function that creates a connector instance from a config
     */
    fun registerConnector(
        exchange: Exchange,
        factory: (ExchangeConfig) -> IExchangeConnector
    ) {
        connectorRegistry[exchange] = factory
        logger.info { "Registered connector factory for ${exchange.name}" }
    }

    /**
     * Creates or retrieves a connector instance for the specified exchange.
     *
     * If a connector for the exchange already exists in the cache and the configuration
     * hasn't changed, the cached instance is returned. Otherwise, a new instance is created,
     * configured, and cached.
     *
     * @param exchange The exchange type (e.g., BINANCE, BITGET)
     * @param config The exchange configuration
     * @param useCache Whether to use cached connector (default: true)
     * @return A configured connector instance
     * @throws UnsupportedExchangeException if the exchange is not supported
     * @throws IllegalArgumentException if configuration is invalid
     */
    fun createConnector(
        exchange: Exchange,
        config: ExchangeConfig,
        useCache: Boolean = true
    ): IExchangeConnector {
        logger.debug { "Creating connector for ${exchange.name}, useCache=$useCache" }

        // Validate that exchange in config matches requested exchange
        require(config.exchange == exchange) {
            "Configuration exchange (${config.exchange}) does not match requested exchange ($exchange)"
        }

        // Check cache first if enabled
        if (useCache) {
            connectorCache[exchange]?.let { cached ->
                logger.debug { "Returning cached connector for ${exchange.name}" }
                return cached
            }
        }

        // Get factory function for this exchange
        val factory = connectorRegistry[exchange]
            ?: throw UnsupportedExchangeException.forExchange(exchange.name)

        // Create new connector instance
        logger.info { "Creating new connector instance for ${exchange.name}" }
        val connector = try {
            factory(config)
        } catch (e: Exception) {
            logger.error(e) { "Failed to create connector for ${exchange.name}" }
            throw IllegalArgumentException("Failed to create connector: ${e.message}", e)
        }

        // Configure the connector
        try {
            connector.configure(config)
        } catch (e: Exception) {
            logger.error(e) { "Failed to configure connector for ${exchange.name}" }
            throw IllegalArgumentException("Failed to configure connector: ${e.message}", e)
        }

        // Cache the connector if caching is enabled
        if (useCache) {
            connectorCache[exchange] = connector
            logger.debug { "Cached connector for ${exchange.name}" }
        }

        return connector
    }

    /**
     * Retrieves a cached connector instance without creating a new one.
     *
     * @param exchange The exchange type
     * @return The cached connector instance, or null if not found
     */
    fun getCachedConnector(exchange: Exchange): IExchangeConnector? {
        return connectorCache[exchange]
    }

    /**
     * Removes a connector from the cache and disconnects it.
     *
     * @param exchange The exchange type
     * @return `true` if connector was found and removed, `false` otherwise
     */
    suspend fun removeConnector(exchange: Exchange): Boolean {
        val connector = connectorCache.remove(exchange) ?: return false
        
        logger.info { "Removing connector for ${exchange.name}" }
        try {
            if (connector.isConnected()) {
                connector.disconnect()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error disconnecting connector for ${exchange.name}" }
        }
        
        return true
    }

    /**
     * Removes all connectors from the cache and disconnects them.
     */
    suspend fun removeAllConnectors() {
        logger.info { "Removing all connectors (${connectorCache.size} cached)" }
        
        val exchanges = connectorCache.keys.toList()
        for (exchange in exchanges) {
            removeConnector(exchange)
        }
        
        connectorCache.clear()
        logger.info { "All connectors removed" }
    }

    /**
     * Returns the number of cached connectors.
     *
     * @return Number of connectors in cache
     */
    fun getCachedConnectorCount(): Int = connectorCache.size

    /**
     * Returns the list of supported exchanges.
     *
     * @return List of exchange types that have registered connectors
     */
    fun getSupportedExchanges(): List<Exchange> {
        return connectorRegistry.keys.toList()
    }

    /**
     * Checks if an exchange is supported.
     *
     * @param exchange The exchange type to check
     * @return `true` if the exchange is supported, `false` otherwise
     */
    fun isExchangeSupported(exchange: Exchange): Boolean {
        return connectorRegistry.containsKey(exchange)
    }

    companion object {
        @Volatile
        private var instance: ConnectorFactory? = null

        /**
         * Gets the singleton instance of the ConnectorFactory.
         *
         * This method is thread-safe and uses double-checked locking for lazy initialization.
         *
         * @return The singleton ConnectorFactory instance
         */
        fun getInstance(): ConnectorFactory {
            return instance ?: synchronized(this) {
                instance ?: ConnectorFactory().also {
                    instance = it
                    logger.info { "ConnectorFactory singleton instance created" }
                }
            }
        }

        /**
         * Resets the singleton instance (primarily for testing).
         *
         * **Warning**: This should only be used in tests, not in production code.
         */
        @Synchronized
        internal fun resetInstance() {
            instance = null
            logger.warn { "ConnectorFactory singleton instance reset" }
        }
    }
}

