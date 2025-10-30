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
     * Configures the connector with Bitget-specific settings
     */
    override fun configure(config: ExchangeConfig) {
        super.configure(config)
        
        // Ensure passphrase is provided
        require(!config.passphrase.isNullOrBlank()) {
            "Bitget requires a passphrase. Please provide config.passphrase"
        }
        
        // Wrap config in BitgetConfig for convenience
        bitgetConfig = BitgetConfig(baseExchangeConfig = config, passphrase = config.passphrase!!)
        
        // Create authenticator
        authenticator = BitgetAuthenticator(
            apiKey = config.apiKey,
            apiSecret = config.apiSecret,
            passphrase = config.passphrase!!,
            recvWindow = bitgetConfig.recvWindow,
            timestampOffset = bitgetConfig.timestampOffset
        )
        
        logger.info { "Bitget connector configured (testnet: ${config.testnet})" }
    }
    
    /**
     * Tests connectivity with Bitget API
     */
    override suspend fun testConnectivity() {
        val baseUrl = bitgetConfig.baseUrl ?: "https://api.bitget.com"
        
        try {
            // Test with server time endpoint
            val response = httpClient.get("$baseUrl/api/spot/v1/public/time")
            
            if (response.status != HttpStatusCode.OK) {
                throw ConnectionException("Connectivity test failed: ${response.status}", exchangeName = "BITGET")
            }
            
            logger.debug { "✓ Bitget connectivity test passed" }
            
        } catch (e: Exception) {
            if (e is ConnectionException) throw e
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
        } catch (e: Exception) {
            logger.warn(e) { "Failed to synchronize server time, will use local time" }
            // Non-fatal - can proceed with local time
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
            val params = buildMap {
                put("symbol", bitgetSymbol)
                put("granularity", bitgetInterval)
                if (startTime != null) put("startTime", startTime.toEpochMilli().toString())
                if (endTime != null) put("endTime", endTime.toEpochMilli().toString())
                if (limit > 0) put("limit", limit.toString())
            }
            
            val queryString = params.entries.joinToString("&") { "${it.key}=${it.value}" }
            val url = "$baseUrl/api/spot/v1/market/candles?$queryString"
            
            val response = httpClient.get(url)
            
            if (response.status != HttpStatusCode.OK) {
                errorHandler.handleHttpError(response.status.value, response.bodyAsText())
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
            val baseUrl = bitgetConfig.baseUrl ?: "https://api.bitget.com"
            val bitgetSymbol = convertSymbolToBitget(symbol)
            val url = "$baseUrl/api/spot/v1/market/ticker?symbol=$bitgetSymbol"
            
            val response = httpClient.get(url)
            
            if (response.status != HttpStatusCode.OK) {
                errorHandler.handleHttpError(response.status.value, response.bodyAsText())
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
            val baseUrl = bitgetConfig.baseUrl ?: "https://api.bitget.com"
            val bitgetSymbol = convertSymbolToBitget(symbol)
            val url = "$baseUrl/api/spot/v1/market/depth?symbol=$bitgetSymbol&limit=$limit&type=step0"
            
            val response = httpClient.get(url)
            
            if (response.status != HttpStatusCode.OK) {
                errorHandler.handleHttpError(response.status.value, response.bodyAsText())
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
            val requestPath = "/api/spot/v1/account/assets"
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
            val bitgetSymbol = convertSymbolToBitget(order.symbol)
            
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
            val bitgetSymbol = convertSymbolToBitget(symbol)
            
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
     * Converts symbol from standard format (BTCUSDT) to Bitget format (BTC_USDT)
     */
    private fun convertSymbolToBitget(symbol: String): String {
        // Bitget uses underscore format: BTC_USDT
        // Common quote currencies
        val quoteCurrencies = listOf("USDT", "USDC", "BTC", "ETH")
        
        for (quote in quoteCurrencies) {
            if (symbol.endsWith(quote)) {
                val base = symbol.removeSuffix(quote)
                return "${base}_$quote"
            }
        }
        
        // If no match, assume format is correct
        return symbol
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

