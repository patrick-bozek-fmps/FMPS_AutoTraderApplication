package com.fmps.autotrader.core.connectors.bitget

import com.fmps.autotrader.core.connectors.AbstractExchangeConnector
import com.fmps.autotrader.core.connectors.exceptions.*
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.TimeFrame
import com.fmps.autotrader.shared.enums.TradeAction
import com.fmps.autotrader.shared.enums.TradeStatus
import com.fmps.autotrader.shared.enums.OrderType
import com.fmps.autotrader.shared.model.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import mu.KotlinLogging
import java.math.BigDecimal
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Bitget exchange connector implementation
 * 
 * Supports:
 * - REST API for market data, account info, and order management
 * - WebSocket streaming for real-time data
 * - Testnet and production environments
 * 
 * **Authentication:**
 * - Requires API Key, Secret Key, and Passphrase
 * - Uses HMAC SHA256 signatures with Base64 encoding
 * 
 * **Endpoints:**
 * - REST: https://api.bitget.com
 * - WebSocket: wss://ws.bitget.com/spot/v1/stream
 * 
 * @see AbstractExchangeConnector
 * @see BitgetConfig
 * @see BitgetAuthenticator
 */
class BitgetConnector : AbstractExchangeConnector(Exchange.BITGET) {
    
    private lateinit var bitgetConfig: BitgetConfig
    private lateinit var authenticator: BitgetAuthenticator
    private var webSocketManager: BitgetWebSocketManager? = null
    private val errorHandler = BitgetErrorHandler()
    
    /**
     * Hybrid Testing Support: Discovered symbol compatibility
     * 
     * These are populated during connection to enable hybrid testing:
     * - Use V1-compatible symbols for V1 market endpoint tests
     * - Use V2-compatible symbols for V2 market endpoint tests (when available)
     */
    private var v1CompatibleSymbols: List<String> = emptyList()
    private var v2CompatibleSymbols: List<String> = emptyList()
    
    /**
     * Gets a V1-compatible symbol for testing
     * Returns the first available V1-compatible symbol, or null if none found
     */
    fun getV1CompatibleSymbol(): String? = v1CompatibleSymbols.firstOrNull()
    
    /**
     * Gets a V2-compatible symbol for testing
     * Returns the first available V2-compatible symbol, or null if none found
     */
    fun getV2CompatibleSymbol(): String? = v2CompatibleSymbols.firstOrNull()
    
    /**
     * Gets all discovered V1-compatible symbols
     */
    fun getV1CompatibleSymbols(): List<String> = v1CompatibleSymbols
    
    /**
     * Gets all discovered V2-compatible symbols
     */
    fun getV2CompatibleSymbols(): List<String> = v2CompatibleSymbols
    
    /**
     * Builds endpoint URL based on API version configuration
     * 
     * Hybrid Strategy:
     * - Symbols endpoint: Always V2 (works)
     * - Market endpoints: V1 by default, V2 when enabled
     * 
     * @param endpointType Type of endpoint (SYMBOLS always uses V2, MARKET uses config)
     * @param path Endpoint path (e.g., "market/candles", "market/ticker")
     * @param queryParams Query parameters (optional)
     * @return Full URL for the endpoint
     */
    private enum class EndpointType {
        SYMBOLS,    // Always uses V2
        MARKET      // Uses V1 by default, V2 when enabled
    }
    
    private fun buildEndpointUrl(endpointType: EndpointType, path: String, queryParams: Map<String, String> = emptyMap()): String {
        val baseUrl = bitgetConfig.baseUrl ?: "https://api.bitget.com"
        
        val apiVersion = when (endpointType) {
            EndpointType.SYMBOLS -> "v2"  // Symbols always use V2
            EndpointType.MARKET -> if (bitgetConfig.useV2MarketEndpoints) "v2" else "v1"  // Market uses config
        }
        
        val apiPath = when (endpointType) {
            EndpointType.SYMBOLS -> "/api/v2/spot/public/$path"  // Symbols use public path
            EndpointType.MARKET -> {
                if (bitgetConfig.useV2MarketEndpoints) {
                    "/api/v2/spot/market/$path"  // V2 market endpoints (when available)
                } else {
                    "/api/spot/v1/market/$path"  // V1 market endpoints (deprecated but required)
                }
            }
        }
        
        val queryString = if (queryParams.isNotEmpty()) {
            "?" + queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }
        } else {
            ""
        }
        
        return "$baseUrl$apiPath$queryString"
    }
    
    /**
     * Configures the connector with Bitget-specific settings
     */
    override fun configure(config: ExchangeConfig) {
        super.configure(config)
        
        // Ensure passphrase is provided
        require(!config.passphrase.isNullOrBlank()) {
            "Bitget requires a passphrase. Please provide config.passphrase"
        }
        
        // Wrap config in BitgetConfig for convenience
        // Wrap config in BitgetConfig for convenience
        // Note: useV2MarketEndpoints defaults to false (V2 spot market endpoints not available yet)
        // Set to true when Bitget releases V2 spot market endpoints
        bitgetConfig = BitgetConfig(
            baseExchangeConfig = config,
            passphrase = config.passphrase!!,
            useV2MarketEndpoints = false  // TODO: Set to true when V2 spot market endpoints become available
        )
        
        // Create authenticator
        authenticator = BitgetAuthenticator(
            apiKey = config.apiKey,
            apiSecret = config.apiSecret,
            passphrase = config.passphrase!!,
            testnet = config.testnet,  // Pass testnet flag to add paptrading header for demo keys
            recvWindow = bitgetConfig.recvWindow,
            timestampOffset = bitgetConfig.timestampOffset
        )
        
        logger.info { "Bitget connector configured (testnet: ${config.testnet})" }
    }
    
    /**
     * Tests connectivity with Bitget API and validates API keys
     * 
     * Uses an authenticated endpoint (/api/spot/v1/account/assets) to verify that:
     * 1. The exchange API is reachable
     * 2. The API keys, secret, and passphrase are valid
     * 3. The API key has proper permissions
     * 
     * This will fail if:
     * - Network connectivity issues
     * - Invalid API key, secret, or passphrase
     * - API key doesn't have required permissions
     * - Clock synchronization issues (timestamp errors)
     */
    override suspend fun testConnectivity() {
        val baseUrl = bitgetConfig.baseUrl ?: "https://api.bitget.com"
        
        try {
            // First, test basic connectivity with server time endpoint (public)
            val timeResponse = httpClient.get("$baseUrl/api/spot/v1/public/time")
            if (timeResponse.status != HttpStatusCode.OK) {
                throw ConnectionException("Connectivity test failed: ${timeResponse.status}", exchangeName = "BITGET")
            }
            
            // Test both account endpoints (V2) and market endpoints (V1) since we use both for trading
            // V2 account endpoints work for demo trading with paptrading header
            // V1 market endpoints are still required for actual trading operations
            var accountSuccess = false
            var marketSuccess = false
            var lastError: Exception? = null
            
            // Test 1: V2 Account endpoint (for account operations like getBalance)
            logger.info { "Testing V2 account endpoint for connectivity (testnet: ${bitgetConfig.testnet})" }
            println("🔍 [BitgetConnector] Testing V2 account endpoint: /api/v2/spot/account/assets")
            try {
                val accountPath = "/api/v2/spot/account/assets"
                val accountHeaders = authenticator.createHeaders("GET", accountPath)
                
                if (accountHeaders.containsKey("paptrading")) {
                    println("✅ [BitgetConnector] paptrading header is present: ${accountHeaders["paptrading"]}")
                }
                
                val accountResponse = httpClient.get("$baseUrl$accountPath") {
                    headers {
                        accountHeaders.forEach { (key, value) ->
                            append(key, value)
                        }
                    }
                }
                
                if (accountResponse.status == HttpStatusCode.OK) {
                    logger.info { "✓ V2 account endpoint test passed" }
                    println("✅ [BitgetConnector] V2 account endpoint test passed")
                    accountSuccess = true
                } else {
                    val errorBody = accountResponse.bodyAsText()
                    val errorMsg = "V2 account endpoint failed: ${accountResponse.status} - $errorBody"
                    logger.warn { errorMsg }
                    println("❌ [BitgetConnector] V2 account endpoint failed: ${accountResponse.status}")
                    
                    if (errorBody.contains("40099") || errorBody.contains("exchange environment")) {
                        throw ConnectionException(errorMsg, exchangeName = "BITGET")
                    }
                    lastError = ConnectionException(errorMsg, exchangeName = "BITGET")
                }
            } catch (e: ConnectionException) {
                lastError = e
                val errorMsg = e.message ?: ""
                if (errorMsg.contains("40099") || errorMsg.contains("exchange environment")) {
                    throw e
                }
            } catch (e: Exception) {
                lastError = e
                logger.warn(e) { "Exception testing V2 account endpoint" }
                println("⚠️ [BitgetConnector] Exception testing V2 account endpoint: ${e.message}")
            }
            
            // Test 2: V1 Market endpoint (for trading operations like getCandles, getTicker, getOrderBook)
            // Use a simple public market endpoint that doesn't require authentication to verify V1 works
            logger.info { "Testing V1 market endpoint for connectivity" }
            println("🔍 [BitgetConnector] Testing V1 market endpoint: /api/spot/v1/market/ticker")
            try {
                // Test with a common symbol - use BTCUSDT_SPBL (Bitget format) or just test the endpoint structure
                // For connectivity, we just need to verify the endpoint responds (even if symbol doesn't exist, we get a proper error, not 404)
                val marketPath = "/api/spot/v1/market/ticker"
                val marketUrl = "$baseUrl$marketPath?symbol=BTCUSDT_SPBL"
                
                val marketResponse = httpClient.get(marketUrl)
                
                // V1 market endpoints are public, so 200 OK or 400 (bad symbol) both indicate the endpoint exists
                if (marketResponse.status == HttpStatusCode.OK || marketResponse.status == HttpStatusCode.BadRequest) {
                    logger.info { "✓ V1 market endpoint test passed (status: ${marketResponse.status})" }
                    println("✅ [BitgetConnector] V1 market endpoint test passed (status: ${marketResponse.status})")
                    marketSuccess = true
                } else if (marketResponse.status == HttpStatusCode.NotFound) {
                    val errorMsg = "V1 market endpoint returned 404 - endpoint may not exist for demo trading"
                    logger.warn { errorMsg }
                    println("⚠️ [BitgetConnector] V1 market endpoint returned 404")
                    lastError = ConnectionException(errorMsg, exchangeName = "BITGET")
                } else {
                    val errorBody = marketResponse.bodyAsText()
                    val errorMsg = "V1 market endpoint failed: ${marketResponse.status} - $errorBody"
                    logger.warn { errorMsg }
                    println("❌ [BitgetConnector] V1 market endpoint failed: ${marketResponse.status}")
                    lastError = ConnectionException(errorMsg, exchangeName = "BITGET")
                }
            } catch (e: Exception) {
                lastError = e
                logger.warn(e) { "Exception testing V1 market endpoint" }
                println("⚠️ [BitgetConnector] Exception testing V1 market endpoint: ${e.message}")
            }
            
            // Both tests must pass for connectivity to be considered successful
            if (!accountSuccess || !marketSuccess) {
                val missingTests = mutableListOf<String>()
                if (!accountSuccess) missingTests.add("V2 account endpoint")
                if (!marketSuccess) missingTests.add("V1 market endpoint")
                
                val errorDetails = "Connection test incomplete. Failed tests: ${missingTests.joinToString(", ")}.\n" +
                        "Account operations require V2 endpoints, trading operations require V1 market endpoints.\n" +
                        if (lastError != null) "Last error: ${lastError.message}" else "No additional error details"
                
                throw ConnectionException(
                    errorDetails,
                    lastError,
                    exchangeName = "BITGET"
                )
            }
            
            logger.info { "✓ Bitget connectivity test passed - both V2 account and V1 market endpoints are working" }
            println("✅ [BitgetConnector] Connectivity test passed - both V2 account and V1 market endpoints are working")
            
        } catch (e: ConnectionException) {
            // Re-throw connection exceptions
            throw e
        } catch (e: Exception) {
            // Wrap other exceptions
            throw ConnectionException("Failed to connect to Bitget: ${e.message}", e, exchangeName = "BITGET")
        }
    }
    
    /**
     * Called after successful connection to perform Bitget-specific initialization
     */
    override suspend fun onConnect() {
        try {
            val baseUrl = bitgetConfig.baseUrl ?: "https://api.bitget.com"
            
            // Get server time and adjust timestamp offset
            val timeResponse = httpClient.get("$baseUrl/api/spot/v1/public/time")
            if (timeResponse.status == HttpStatusCode.OK) {
                val timeJson = Json.parseToJsonElement(timeResponse.bodyAsText()).jsonObject
                val serverTime = timeJson["data"]?.jsonPrimitive?.long
                if (serverTime != null) {
                    authenticator.updateTimestampOffset(serverTime)
                    logger.info { "Synchronized with Bitget server time (offset: ${authenticator.getTimestampOffset()}ms)" }
                }
            }
            
            // Query available symbols to verify environment and symbol availability
            // Check both v2 and v1 symbols endpoints to understand version differences
            try {
                // Query v2 symbols endpoint (always uses V2 - works)
                val symbolsUrl = buildEndpointUrl(EndpointType.SYMBOLS, "symbols")
                logger.info { "Querying Bitget symbols endpoint: $symbolsUrl" }
                val v2SymbolsResponse = httpClient.get(symbolsUrl)
                logger.info { "V2 symbols endpoint response status: ${v2SymbolsResponse.status}" }
                
                if (v2SymbolsResponse.status == HttpStatusCode.OK) {
                    val responseBody = v2SymbolsResponse.bodyAsText()
                    val symbolsJson = Json.parseToJsonElement(responseBody).jsonObject
                    val symbolsArray = symbolsJson["data"]?.jsonArray
                    if (symbolsArray != null) {
                        val symbolCount = symbolsArray.size
                        println("✅ Bitget v2 API has $symbolCount available trading pairs")
                        logger.info { "Bitget v2 API has $symbolCount available trading pairs" }
                        
                        // Check if BTCUSDT is available in v2
                        val btcusdtV2 = symbolsArray.firstOrNull { symbol ->
                            val symbolObj = symbol.jsonObject
                            symbolObj["symbol"]?.jsonPrimitive?.content == "BTCUSDT"
                        }
                        
                        // Collect all V2-compatible symbols (online status)
                        v2CompatibleSymbols = symbolsArray.mapNotNull { symbol ->
                            val symbolObj = symbol.jsonObject
                            val symbolName = symbolObj["symbol"]?.jsonPrimitive?.content
                            val status = symbolObj["status"]?.jsonPrimitive?.content
                            if (status == "online" && symbolName != null) {
                                symbolName
                            } else null
                        }
                        
                        println("✅ Discovered ${v2CompatibleSymbols.size} V2-compatible symbols (online)")
                        logger.info { "Discovered ${v2CompatibleSymbols.size} V2-compatible symbols" }
                        
                        // Check if BTCUSDT is available in v2 (using already found btcusdtV2)
                        if (btcusdtV2 != null) {
                            val symbolObj = btcusdtV2.jsonObject
                            val status = symbolObj["status"]?.jsonPrimitive?.content
                            println("✅ BTCUSDT found in v2 API, status: $status")
                            logger.info { "✓ BTCUSDT found in v2 API, status: $status" }
                        } else {
                            println("⚠️  BTCUSDT NOT found in v2 API")
                            logger.warn { "⚠ BTCUSDT NOT found in v2 API" }
                        }
                        
                        // Log API version configuration
                        val marketApiVersion = if (bitgetConfig.useV2MarketEndpoints) "V2" else "V1 (deprecated)"
                        println("   📌 Market endpoints using: $marketApiVersion")
                        logger.info { "Market endpoints using: $marketApiVersion" }
                        
                        if (!bitgetConfig.useV2MarketEndpoints) {
                            println("   ⚠️  Note: V1 API is deprecated (discontinued Nov 28, 2025)")
                            println("   ⚠️  V2 spot market endpoints not yet available - using V1")
                            logger.warn { "Using deprecated V1 API for market endpoints. V2 spot market endpoints not yet available." }
                            
                            // Discover V1-compatible symbols by testing them
                            println("   🔍 Discovering V1-compatible symbols...")
                            discoverV1CompatibleSymbols(v2CompatibleSymbols.take(20)) // Test first 20 V2 symbols
                        }
                    }
                }
                
                // Try v1 symbols endpoint to see what v1 supports
                try {
                    logger.info { "Querying Bitget v1 symbols endpoint: $baseUrl/api/spot/v1/public/symbols" }
                    val v1SymbolsResponse = httpClient.get("$baseUrl/api/spot/v1/public/symbols")
                    if (v1SymbolsResponse.status == HttpStatusCode.OK) {
                        val v1ResponseBody = v1SymbolsResponse.bodyAsText()
                        val v1SymbolsJson = Json.parseToJsonElement(v1ResponseBody).jsonObject
                        val v1SymbolsArray = v1SymbolsJson["data"]?.jsonArray
                        if (v1SymbolsArray != null) {
                            val v1SymbolCount = v1SymbolsArray.size
                            println("✅ Bitget v1 API has $v1SymbolCount available trading pairs")
                            logger.info { "Bitget v1 API has $v1SymbolCount available trading pairs" }
                            
                            // Get first few v1 symbols for comparison
                            val firstV1Symbols = v1SymbolsArray.take(5).mapNotNull { symbol ->
                                val symbolObj = symbol.jsonObject
                                symbolObj["symbol"]?.jsonPrimitive?.content
                            }
                            println("   V1 sample symbols: ${firstV1Symbols.joinToString(", ")}")
                            logger.info { "V1 sample symbols: ${firstV1Symbols.joinToString(", ")}" }
                            
                            // Check if BTCUSDT exists in v1
                            val btcusdtV1 = v1SymbolsArray.firstOrNull { symbol ->
                                val symbolObj = symbol.jsonObject
                                symbolObj["symbol"]?.jsonPrimitive?.content?.contains("BTC") == true
                            }
                            if (btcusdtV1 != null) {
                                val symbolObj = btcusdtV1.jsonObject
                                val v1SymbolName = symbolObj["symbol"]?.jsonPrimitive?.content
                                println("   V1 BTC symbol format: $v1SymbolName")
                                logger.info { "V1 BTC symbol format: $v1SymbolName" }
                            }
                        }
                    } else {
                        println("⚠️  V1 symbols endpoint returned: ${v1SymbolsResponse.status}")
                        logger.warn { "V1 symbols endpoint returned: ${v1SymbolsResponse.status}" }
                    }
                } catch (e: Exception) {
                    println("⚠️  V1 symbols endpoint not available: ${e.message}")
                    logger.warn(e) { "V1 symbols endpoint not available" }
                }
            } catch (e: Exception) {
                println("⚠️  Failed to query available symbols: ${e.message}")
                logger.warn(e) { "Failed to query available symbols (non-fatal)" }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to synchronize server time, will use local time" }
            // Non-fatal - can proceed with local time
        }
    }
    
    /**
     * Discovers which symbols are compatible with V1 market endpoints
     * Tests symbols by attempting to fetch ticker data
     * 
     * @param candidateSymbols List of symbols to test (typically from V2 symbols endpoint)
     * @return List of symbols that work with V1 endpoints
     */
    private suspend fun discoverV1CompatibleSymbols(candidateSymbols: List<String>) {
        val baseUrl = bitgetConfig.baseUrl ?: "https://api.bitget.com"
        val discovered = mutableListOf<String>()
        
        // Test more symbols (up to 50) to increase chances of finding V1-compatible ones
        // V1 API is deprecated and may have limited symbol support
        val symbolsToTest = candidateSymbols.take(50)
        
        println("   Testing ${symbolsToTest.size} symbols with V1 endpoints...")
        logger.info { "Testing ${symbolsToTest.size} symbols with V1 endpoints for compatibility" }
        
        for ((index, symbol) in symbolsToTest.withIndex()) {
            try {
                // Test with ticker endpoint (lightweight test)
                val testUrl = buildEndpointUrl(EndpointType.MARKET, "ticker", mapOf("symbol" to symbol))
                val testResponse = httpClient.get(testUrl)
                
                if (testResponse.status == HttpStatusCode.OK) {
                    discovered.add(symbol)
                    println("   ✓ V1 accepts: $symbol (${discovered.size}/${symbolsToTest.size})")
                    logger.info { "✓ V1 accepts: $symbol" }
                } else {
                    // Only log first few rejections to avoid spam
                    if (index < 5) {
                        logger.debug { "✗ V1 rejects: $symbol (${testResponse.status})" }
                    }
                }
            } catch (e: Exception) {
                // Only log first few errors to avoid spam
                if (index < 5) {
                    logger.debug(e) { "Error testing symbol $symbol with V1" }
                }
            }
            
            // Limit discovery to avoid rate limiting, but try to find at least 3
            if (discovered.size >= 3) {
                break // Found enough symbols
            }
            
            // Add small delay to avoid rate limiting
            if (index > 0 && index % 10 == 0) {
                kotlinx.coroutines.delay(100) // 100ms delay every 10 requests
            }
        }
        
        v1CompatibleSymbols = discovered
        
        if (v1CompatibleSymbols.isNotEmpty()) {
            println("   ✅ Discovered ${v1CompatibleSymbols.size} V1-compatible symbols: ${v1CompatibleSymbols.joinToString(", ")}")
            logger.info { "Discovered ${v1CompatibleSymbols.size} V1-compatible symbols: ${v1CompatibleSymbols.joinToString(", ")}" }
        } else {
            println("   ⚠️  No V1-compatible symbols found after testing ${symbolsToTest.size} symbols")
            println("   ⚠️  This is expected - V1 API is deprecated and may not support V2 symbols")
            logger.warn { "No V1-compatible symbols found after testing ${symbolsToTest.size} symbols. V1 API is deprecated." }
        }
    }
    
    /**
     * Disconnects from Bitget API
     */
    override suspend fun disconnect() {
        logger.info { "Disconnecting from Bitget..." }
        
        try {
            // Close WebSocket connections
            webSocketManager?.close()
            webSocketManager = null
            
            // Call parent disconnect
            super.disconnect()
            
            logger.info { "✓ Disconnected from Bitget" }
            
        } catch (e: Exception) {
            logger.error(e) { "Error during Bitget disconnection" }
            throw ConnectionException("Failed to disconnect: ${e.message}", e, exchangeName = "BITGET")
        }
    }
    
    /**
     * Retrieves candlestick (OHLCV) data
     * 
     * @param symbol Trading pair symbol (e.g., "BTCUSDT" will be converted to "BTC_USDT")
     * @param interval Candlestick interval
     * @param startTime Start time (optional)
     * @param endTime End time (optional)
     * @param limit Number of candlesticks to retrieve (max 1000)
     * @return List of candlesticks
     */
    override suspend fun getCandles(
        symbol: String,
        interval: TimeFrame,
        startTime: Instant?,
        endTime: Instant?,
        limit: Int
    ): List<Candlestick> {
        logger.debug { "Fetching candlesticks for $symbol, interval: $interval" }
        
        ensureConnected()
        
        try {
            val baseUrl = bitgetConfig.baseUrl ?: "https://api.bitget.com"
            val bitgetSymbol = convertSymbolToBitget(symbol)
            val bitgetInterval = mapTimeFrameToBitgetInterval(interval)
            
            // Build query parameters
            // Note: Bitget API uses 'period' parameter for candles endpoint, not 'granularity' or 'interval'
            val params = mutableMapOf<String, String>(
                "symbol" to bitgetSymbol,
                "period" to bitgetInterval
            )
            if (startTime != null) params["startTime"] = startTime.toEpochMilli().toString()
            if (endTime != null) params["endTime"] = endTime.toEpochMilli().toString()
            if (limit > 0) params["limit"] = limit.toString()
            
            // Hybrid approach: Use V2 if enabled, otherwise V1 (deprecated but required)
            val url = buildEndpointUrl(EndpointType.MARKET, "candles", params)
            
            val response = httpClient.get(url)
            
            if (response.status != HttpStatusCode.OK) {
                val responseBody = response.bodyAsText()
                logger.error { "Bitget candles API error - URL: $url, Status: ${response.status}, Response: $responseBody" }
                errorHandler.handleHttpError(response.status.value, responseBody)
            }
            
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val dataArray = json["data"]?.jsonArray ?: throw ExchangeException("Invalid response format", exchangeName = "BITGET")
            
            return dataArray.map { element ->
                val candle = element.jsonArray
                Candlestick(
                    symbol = symbol,
                    interval = interval,
                    openTime = Instant.ofEpochMilli(candle[0].jsonPrimitive.long),
                    closeTime = Instant.ofEpochMilli(candle[0].jsonPrimitive.long + mapIntervalToMillis(interval)),
                    open = candle[1].jsonPrimitive.content.toBigDecimal(),
                    high = candle[2].jsonPrimitive.content.toBigDecimal(),
                    low = candle[3].jsonPrimitive.content.toBigDecimal(),
                    close = candle[4].jsonPrimitive.content.toBigDecimal(),
                    volume = candle[5].jsonPrimitive.content.toBigDecimal(),
                    quoteVolume = candle[6].jsonPrimitive.content.toBigDecimal()
                )
            }
            
        } catch (e: ExchangeException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error fetching candlesticks" }
            throw ExchangeException("Failed to fetch candlesticks: ${e.message}", e, exchangeName = "BITGET")
        }
    }
    
    /**
     * Retrieves ticker (24hr statistics) for a symbol
     */
    override suspend fun getTicker(symbol: String): Ticker {
        logger.debug { "Fetching ticker for $symbol" }
        
        ensureConnected()
        
        try {
            val bitgetSymbol = convertSymbolToBitget(symbol)
            // Hybrid approach: Use V2 if enabled, otherwise V1 (deprecated but required)
            val url = buildEndpointUrl(EndpointType.MARKET, "ticker", mapOf("symbol" to bitgetSymbol))
            
            val response = httpClient.get(url)
            
            if (response.status != HttpStatusCode.OK) {
                val responseBody = response.bodyAsText()
                logger.error { "Bitget ticker API error - URL: $url, Status: ${response.status}, Response: $responseBody" }
                errorHandler.handleHttpError(response.status.value, responseBody)
            }
            
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val data = json["data"]?.jsonObject ?: throw ExchangeException("Invalid response format", exchangeName = "BITGET")
            
            return Ticker(
                symbol = symbol,
                lastPrice = data["close"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                bidPrice = data["bestBid"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                askPrice = data["bestAsk"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                volume = data["baseVol"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                quoteVolume = data["quoteVol"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                priceChange = data["change"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                priceChangePercent = data["changePercent"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                high = data["high"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                low = data["low"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                openPrice = data["open"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                timestamp = Instant.now()
            )
            
        } catch (e: ExchangeException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error fetching ticker" }
            throw ExchangeException("Failed to fetch ticker: ${e.message}", e, exchangeName = "BITGET")
        }
    }
    
    /**
     * Retrieves order book (market depth) for a symbol
     */
    override suspend fun getOrderBook(symbol: String, limit: Int): OrderBook {
        logger.debug { "Fetching order book for $symbol" }
        
        ensureConnected()
        
        try {
            val bitgetSymbol = convertSymbolToBitget(symbol)
            // Hybrid approach: Use V2 if enabled, otherwise V1 (deprecated but required)
            val url = buildEndpointUrl(EndpointType.MARKET, "depth", mapOf(
                "symbol" to bitgetSymbol,
                "limit" to limit.toString(),
                "type" to "step0"
            ))
            
            val response = httpClient.get(url)
            
            if (response.status != HttpStatusCode.OK) {
                val responseBody = response.bodyAsText()
                logger.error { "Bitget order book API error - URL: $url, Status: ${response.status}, Response: $responseBody" }
                errorHandler.handleHttpError(response.status.value, responseBody)
            }
            
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val data = json["data"]?.jsonObject ?: throw ExchangeException("Invalid response format", exchangeName = "BITGET")
            
            val bids = data["bids"]?.jsonArray?.map { entry ->
                val bid = entry.jsonArray
                OrderBookEntry(
                    price = bid[0].jsonPrimitive.content.toBigDecimal(),
                    quantity = bid[1].jsonPrimitive.content.toBigDecimal()
                )
            } ?: emptyList()
            
            val asks = data["asks"]?.jsonArray?.map { entry ->
                val ask = entry.jsonArray
                OrderBookEntry(
                    price = ask[0].jsonPrimitive.content.toBigDecimal(),
                    quantity = ask[1].jsonPrimitive.content.toBigDecimal()
                )
            } ?: emptyList()
            
            return OrderBook(
                symbol = symbol,
                bids = bids,
                asks = asks,
                timestamp = Instant.now()
            )
            
        } catch (e: ExchangeException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error fetching order book" }
            throw ExchangeException("Failed to fetch order book: ${e.message}", e, exchangeName = "BITGET")
        }
    }
    
    /**
     * Retrieves account balance
     */
    override suspend fun getBalance(): Map<String, BigDecimal> {
        logger.debug { "Fetching account balance" }
        
        ensureConnected()
        
        try {
            val baseUrl = bitgetConfig.baseUrl ?: "https://api.bitget.com"
            // Use V2 endpoint for account assets (works for demo trading with paptrading header)
            // V1 endpoint returns 404 for demo trading
            val requestPath = "/api/v2/spot/account/assets"
            val (_, headers) = authenticator.signRequest("GET", requestPath)
            val url = "$baseUrl$requestPath"
            
            val response = httpClient.get(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
            
            if (response.status != HttpStatusCode.OK) {
                errorHandler.handleHttpError(response.status.value, response.bodyAsText())
            }
            
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val dataArray = json["data"]?.jsonArray ?: throw ExchangeException("Invalid response format", exchangeName = "BITGET")
            
            return dataArray
                .map { it.jsonObject }
                .associate { asset ->
                    val coin = asset["coinName"]?.jsonPrimitive?.content ?: ""
                    val available = asset["available"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO
                    coin to available
                }
                .filterValues { it > BigDecimal.ZERO }
            
        } catch (e: ExchangeException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error fetching balance" }
            throw ExchangeException("Failed to fetch balance: ${e.message}", e, exchangeName = "BITGET")
        }
    }
    
    /**
     * Retrieves current open position for a symbol (for futures/margin)
     * For spot trading, returns empty
     */
    override suspend fun getPosition(symbol: String): Position? {
        // Spot trading doesn't have positions
        return null
    }
    
    /**
     * Retrieves all open positions (for futures/margin)
     * For spot trading, returns empty list
     */
    override suspend fun getPositions(): List<Position> {
        // Spot trading doesn't have positions
        return emptyList()
    }
    
    /**
     * Places a new order
     */
    override suspend fun placeOrder(order: Order): Order {
        logger.info { "Placing ${order.action} ${order.type} order for ${order.symbol}: ${order.quantity}" }
        
        ensureConnected()
        
        try {
            val baseUrl = bitgetConfig.baseUrl ?: "https://api.bitget.com"
            val requestPath = "/api/spot/v1/trade/orders"
            val bitgetSymbol = convertSymbolToBitgetAuthenticated(order.symbol)
            
            val requestBody = buildJsonObject {
                put("symbol", bitgetSymbol)
                put("side", mapTradeActionToBitgetSide(order.action))
                put("orderType", mapOrderTypeToBitgetType(order.type))
                put("quantity", order.quantity.toPlainString())
                order.price?.let { price ->
                    put("price", price.toPlainString())
                }
            }.toString()
            
            val (_, headers) = authenticator.signRequest("POST", requestPath, mapOf("body" to requestBody))
            val url = "$baseUrl$requestPath"
            
            val response = httpClient.post(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
                setBody(requestBody)
            }
            
            if (response.status != HttpStatusCode.OK) {
                errorHandler.handleHttpError(response.status.value, response.bodyAsText())
            }
            
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val data = json["data"]?.jsonObject ?: throw ExchangeException("Invalid response format", exchangeName = "BITGET")
            
            return order.copy(
                id = data["orderId"]?.jsonPrimitive?.content,
                status = TradeStatus.PENDING,
                createdAt = Instant.now()
            )
            
        } catch (e: ExchangeException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error placing order" }
            throw OrderException("Failed to place order: ${e.message}", e, exchangeName = "BITGET")
        }
    }
    
    /**
     * Cancels an existing order
     */
    override suspend fun cancelOrder(orderId: String, symbol: String): Order {
        logger.info { "Cancelling order $orderId for $symbol" }
        
        ensureConnected()
        
        try {
            val baseUrl = bitgetConfig.baseUrl ?: "https://api.bitget.com"
            val requestPath = "/api/spot/v1/trade/cancel-order"
            val bitgetSymbol = convertSymbolToBitget(symbol)
            
            val requestBody = buildJsonObject {
                put("symbol", bitgetSymbol)
                put("orderId", orderId)
            }.toString()
            
            val (_, headers) = authenticator.signRequest("POST", requestPath, mapOf("body" to requestBody))
            val url = "$baseUrl$requestPath"
            
            val response = httpClient.post(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
                setBody(requestBody)
            }
            
            if (response.status != HttpStatusCode.OK) {
                errorHandler.handleHttpError(response.status.value, response.bodyAsText())
            }
            
            return Order(
                id = orderId,
                symbol = symbol,
                action = TradeAction.LONG, // Unknown, would need to query
                type = OrderType.MARKET,
                quantity = BigDecimal.ZERO,
                status = TradeStatus.CANCELLED,
                updatedAt = Instant.now()
            )
            
        } catch (e: ExchangeException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error cancelling order" }
            throw OrderException("Failed to cancel order: ${e.message}", e, exchangeName = "BITGET")
        }
    }
    
    /**
     * Retrieves an order by ID
     */
    override suspend fun getOrder(orderId: String, symbol: String): Order {
        logger.debug { "Fetching order $orderId for $symbol" }
        
        ensureConnected()
        
        try {
            val baseUrl = bitgetConfig.baseUrl ?: "https://api.bitget.com"
            val requestPath = "/api/spot/v1/trade/orderInfo"
            val bitgetSymbol = convertSymbolToBitgetAuthenticated(symbol)
            
            val params = mapOf("symbol" to bitgetSymbol, "orderId" to orderId)
            val (queryString, headers) = authenticator.signRequest("GET", requestPath, params)
            val url = "$baseUrl$requestPath?$queryString"
            
            val response = httpClient.get(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
            
            if (response.status != HttpStatusCode.OK) {
                errorHandler.handleHttpError(response.status.value, response.bodyAsText())
            }
            
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val data = json["data"]?.jsonObject ?: throw ExchangeException("Invalid response format", exchangeName = "BITGET")
            
            return Order(
                id = data["orderId"]?.jsonPrimitive?.content,
                symbol = symbol,
                action = mapBitgetSideToTradeAction(data["side"]?.jsonPrimitive?.content ?: ""),
                type = OrderType.MARKET,
                quantity = data["quantity"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                price = data["price"]?.jsonPrimitive?.content?.toBigDecimalOrNull(),
                status = mapBitgetStatusToTradeStatus(data["status"]?.jsonPrimitive?.content ?: ""),
                filledQuantity = data["filledQuantity"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                averagePrice = data["averagePrice"]?.jsonPrimitive?.content?.toBigDecimalOrNull(),
                createdAt = Instant.ofEpochMilli(data["cTime"]?.jsonPrimitive?.long ?: 0L)
            )
            
        } catch (e: ExchangeException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error fetching order" }
            throw OrderException("Failed to fetch order: ${e.message}", e, exchangeName = "BITGET")
        }
    }
    
    /**
     * Retrieves all open orders for a symbol (or all symbols if null)
     */
    override suspend fun getOrders(symbol: String?): List<Order> {
        return emptyList() // Simplified for now
    }
    
    /**
     * Closes a position (for futures/margin)
     * Not applicable for spot trading
     */
    override suspend fun closePosition(symbol: String): Order {
        throw UnsupportedOperationException("Spot trading does not support positions")
    }
    
    // WebSocket subscription methods
    
    override suspend fun subscribeCandlesticks(
        symbol: String,
        interval: TimeFrame,
        callback: suspend (Candlestick) -> Unit
    ): String {
        ensureConnected()
        if (webSocketManager == null) {
            webSocketManager = BitgetWebSocketManager(bitgetConfig, authenticator)
        }
        webSocketManager?.subscribeCandlesticks(symbol, interval, callback)
        return "${symbol}_${interval}_candlesticks"
    }
    
    override suspend fun subscribeTicker(
        symbol: String,
        callback: suspend (Ticker) -> Unit
    ): String {
        ensureConnected()
        if (webSocketManager == null) {
            webSocketManager = BitgetWebSocketManager(bitgetConfig, authenticator)
        }
        webSocketManager?.subscribeTicker(symbol, callback)
        return "${symbol}_ticker"
    }
    
    override suspend fun subscribeOrderUpdates(
        callback: suspend (Order) -> Unit
    ): String {
        ensureConnected()
        if (webSocketManager == null) {
            webSocketManager = BitgetWebSocketManager(bitgetConfig, authenticator)
        }
        webSocketManager?.subscribeOrderUpdates(callback)
        return "order_updates"
    }
    
    override suspend fun unsubscribe(subscriptionId: String) {
        // Simplified unsubscribe - would need proper implementation
        logger.info { "Unsubscribing from: $subscriptionId" }
    }
    
    override suspend fun unsubscribeAll() {
        webSocketManager?.close()
        webSocketManager = null
        logger.info { "Unsubscribed from all Bitget streams" }
    }
    
    // Helper methods
    
    /**
     * Converts symbol from standard format (BTCUSDT) to Bitget format
     * 
     * Bitget API version differences:
     * - V2 API: Uses standard format (BTCUSDT) - confirmed by /api/v2/spot/public/symbols endpoint
     * - V1 API: May require different format (BTCUSDT_UMCBL for futures, but spot might be different)
     * 
     * Since we're using v1 market endpoints (/api/spot/v1/market/...) but v1 says BTCUSDT doesn't exist
     * while v2 symbols endpoint confirms it exists, there's a version mismatch.
     * 
     * For now, we use standard format (BTCUSDT) as v2 symbols endpoint confirms this format exists.
     * If v1 endpoints require different format, we may need to query v1 symbols endpoint or use v2 market endpoints.
     */
    private fun convertSymbolToBitget(symbol: String): String {
        // Use standard format: BTCUSDT (uppercase, no underscore)
        // v2 symbols endpoint confirms BTCUSDT exists in this format
        // If v1 endpoints need different format, we'll need to handle that separately
        return symbol.uppercase()
    }
    
    /**
     * Converts symbol to Bitget authenticated endpoint format (BTC_USDT)
     * Used for trading endpoints that require underscore format
     */
    private fun convertSymbolToBitgetAuthenticated(symbol: String): String {
        // Bitget authenticated endpoints use underscore format: BTC_USDT (uppercase)
        val quoteCurrencies = listOf("USDT", "USDC", "BTC", "ETH")
        
        val upperSymbol = symbol.uppercase()
        
        for (quote in quoteCurrencies) {
            if (upperSymbol.endsWith(quote)) {
                val base = upperSymbol.removeSuffix(quote)
                return "${base}_$quote"
            }
        }
        
        // If no match, assume format is correct but ensure uppercase
        return upperSymbol
    }
    
    private fun mapTimeFrameToBitgetInterval(timeFrame: TimeFrame): String {
        return when (timeFrame) {
            TimeFrame.ONE_MINUTE -> "1min"
            TimeFrame.FIVE_MINUTES -> "5min"
            TimeFrame.FIFTEEN_MINUTES -> "15min"
            TimeFrame.THIRTY_MINUTES -> "30min"
            TimeFrame.ONE_HOUR -> "1h"
            TimeFrame.FOUR_HOURS -> "4h"
            TimeFrame.TWELVE_HOURS -> "12h"
            TimeFrame.ONE_DAY -> "1day"
            TimeFrame.ONE_WEEK -> "1week"
            else -> "1h"
        }
    }
    
    private fun mapIntervalToMillis(interval: TimeFrame): Long {
        return when (interval) {
            TimeFrame.ONE_MINUTE -> 60_000L
            TimeFrame.FIVE_MINUTES -> 300_000L
            TimeFrame.FIFTEEN_MINUTES -> 900_000L
            TimeFrame.THIRTY_MINUTES -> 1_800_000L
            TimeFrame.ONE_HOUR -> 3_600_000L
            TimeFrame.FOUR_HOURS -> 14_400_000L
            TimeFrame.TWELVE_HOURS -> 43_200_000L
            TimeFrame.ONE_DAY -> 86_400_000L
            TimeFrame.ONE_WEEK -> 604_800_000L
            else -> 3_600_000L
        }
    }
    
    private fun mapTradeActionToBitgetSide(action: TradeAction): String {
        return when (action) {
            TradeAction.LONG -> "buy"
            TradeAction.SHORT -> "sell"
        }
    }
    
    private fun mapBitgetSideToTradeAction(side: String): TradeAction {
        return when (side.lowercase()) {
            "buy" -> TradeAction.LONG
            "sell" -> TradeAction.SHORT
            else -> TradeAction.LONG
        }
    }
    
    private fun mapOrderTypeToBitgetType(orderType: OrderType): String {
        return when (orderType) {
            OrderType.MARKET -> "market"
            OrderType.LIMIT -> "limit"
            else -> "limit"
        }
    }
    
    private fun mapBitgetStatusToTradeStatus(status: String): TradeStatus {
        return when (status.lowercase()) {
            "init", "new" -> TradeStatus.PENDING
            "partial_fill" -> TradeStatus.PARTIALLY_FILLED
            "full_fill", "filled" -> TradeStatus.FILLED
            "cancelled" -> TradeStatus.CANCELLED
            "rejected" -> TradeStatus.REJECTED
            else -> TradeStatus.PENDING
        }
    }
}

