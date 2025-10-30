package com.fmps.autotrader.core.connectors.binance

import com.fmps.autotrader.core.connectors.AbstractExchangeConnector
import com.fmps.autotrader.core.connectors.exceptions.*
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.TimeFrame
import com.fmps.autotrader.shared.model.*
import mu.KotlinLogging
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.math.BigDecimal
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Binance exchange connector implementation
 * 
 * Supports:
 * - REST API for market data, account info, and order management
 * - WebSocket streaming for real-time data
 * - HMAC SHA256 authentication
 * - Weight-based rate limiting
 * - Comprehensive error handling
 * 
 * **Endpoints:**
 * - Testnet: https://testnet.binance.vision
 * - Production: https://api.binance.com
 * 
 * **Rate Limits:**
 * - 1200 requests per minute (general)
 * - 10 orders per second (order endpoints)
 * - Weight-based system (different endpoints have different weights)
 */
class BinanceConnector : AbstractExchangeConnector(Exchange.BINANCE) {
    
    private lateinit var binanceConfig: BinanceConfig
    private lateinit var authenticator: BinanceAuthenticator
    
    private val errorHandler = BinanceErrorHandler()
    
    private var webSocketManager: BinanceWebSocketManager? = null
    
    /**
     * Configures the connector with Binance-specific settings
     */
    override fun configure(config: ExchangeConfig) {
        super.configure(config)
        
        // Always wrap config to ensure we have BinanceConfig
        binanceConfig = BinanceConfig(baseExchangeConfig = config)
        
        authenticator = BinanceAuthenticator(
            apiKey = config.apiKey,
            apiSecret = config.apiSecret,
            recvWindow = binanceConfig.recvWindow,
            timestampOffset = binanceConfig.timestampOffset
        )
        
        logger.info { "Binance connector configured (testnet: ${config.testnet})" }
    }
    
    /**
     * Tests connectivity to the exchange
     */
    override suspend fun testConnectivity() {
        val baseUrl = config.baseUrl ?: if (config.testnet) {
            "https://testnet.binance.vision"
        } else {
            "https://api.binance.com"
        }
        
        val pingResponse = httpClient.get("$baseUrl/api/v3/ping")
        if (pingResponse.status != HttpStatusCode.OK) {
            throw ConnectionException("Ping test failed: ${pingResponse.status}")
        }
    }
    
    /**
     * Connects to Binance API and verifies connectivity
     */
    override suspend fun connect() {
        logger.info { "Connecting to Binance (${config.baseUrl ?: "default URL"})..." }
        
        try {
            val baseUrl = config.baseUrl ?: if (config.testnet) {
                "https://testnet.binance.vision"
            } else {
                "https://api.binance.com"
            }
            
            // Test connectivity
            val pingResponse = httpClient.get("$baseUrl/api/v3/ping")
            if (pingResponse.status != HttpStatusCode.OK) {
                throw ConnectionException("Ping test failed: ${pingResponse.status}")
            }
            logger.debug { "Connectivity test passed" }
            
            // Get server time and adjust timestamp offset
            val timeResponse = httpClient.get("$baseUrl/api/v3/time")
            if (timeResponse.status == HttpStatusCode.OK) {
                val timeJson = Json.parseToJsonElement(timeResponse.bodyAsText()).jsonObject
                val serverTime = timeJson["serverTime"]?.jsonPrimitive?.long
                if (serverTime != null) {
                    authenticator.updateTimestampOffset(serverTime)
                    logger.debug { "Server time synchronized: $serverTime" }
                }
            }
            
            // Test authentication with account endpoint
            val accountInfo = getBalance()
            logger.info { "Binance connection successful. Account has ${accountInfo.size} non-zero balances." }
            
        } catch (e: ExchangeException) {
            logger.error(e) { "Failed to connect to Binance" }
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Unexpected error connecting to Binance" }
            throw ConnectionException("Connection failed: ${e.message}", e)
        }
    }
    
    /**
     * Disconnects from Binance API
     */
    override suspend fun disconnect() {
        logger.info { "Disconnecting from Binance..." }
        
        try {
            // Close WebSocket connections
            webSocketManager?.close()
            webSocketManager = null
            
            logger.info { "Binance disconnected successfully" }
            
        } catch (e: Exception) {
            logger.error(e) { "Error during Binance disconnection" }
            throw ConnectionException("Disconnection failed: ${e.message}", e)
        }
    }
    
    /**
     * Retrieves candlestick data (OHLCV) for a symbol
     */
    override suspend fun getCandles(
        symbol: String,
        interval: TimeFrame,
        startTime: Instant?,
        endTime: Instant?,
        limit: Int
    ): List<Candlestick> {
        logger.debug { "Fetching candlesticks for $symbol ($interval, limit: $limit)" }
        
        ensureConnected()
        
        try {
            val baseUrl = config.baseUrl ?: "https://testnet.binance.vision"
            val binanceInterval = mapTimeFrameToBinance(interval)
            
            val url = buildString {
                append("$baseUrl/api/v3/klines")
                append("?symbol=${symbol.uppercase()}")
                append("&interval=$binanceInterval")
                if (startTime != null) append("&startTime=${startTime.toEpochMilli()}")
                if (endTime != null) append("&endTime=${endTime.toEpochMilli()}")
                append("&limit=${limit.coerceIn(1, 1000)}")
            }
            
            val response = httpClient.get(url)
            errorHandler.handleResponse(response)
            
            val jsonArray = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            
            return jsonArray.map { element ->
                val arr = element.jsonArray
                Candlestick(
                    symbol = symbol.uppercase(),
                    interval = interval,
                    openTime = Instant.ofEpochMilli(arr[0].jsonPrimitive.long),
                    closeTime = Instant.ofEpochMilli(arr[6].jsonPrimitive.long),
                    open = BigDecimal(arr[1].jsonPrimitive.content),
                    high = BigDecimal(arr[2].jsonPrimitive.content),
                    low = BigDecimal(arr[3].jsonPrimitive.content),
                    close = BigDecimal(arr[4].jsonPrimitive.content),
                    volume = BigDecimal(arr[5].jsonPrimitive.content),
                    quoteVolume = BigDecimal(arr[7].jsonPrimitive.content)
                )
            }.also {
                logger.debug { "Retrieved ${it.size} candlesticks for $symbol" }
            }
            
        } catch (e: ExchangeException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error fetching candlesticks for $symbol" }
            throw ExchangeException("Failed to fetch candlesticks: ${e.message}", e)
        }
    }
    
    /**
     * Retrieves ticker information for a symbol
     */
    override suspend fun getTicker(symbol: String): Ticker {
        logger.debug { "Fetching ticker for $symbol" }
        
        ensureConnected()
        
        try {
            val baseUrl = config.baseUrl ?: "https://testnet.binance.vision"
            val url = "$baseUrl/api/v3/ticker/24hr?symbol=${symbol.uppercase()}"
            
            val response = httpClient.get(url)
            errorHandler.handleResponse(response)
            
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            
            return Ticker(
                symbol = symbol.uppercase(),
                lastPrice = BigDecimal(json["lastPrice"]?.jsonPrimitive?.content ?: "0"),
                bidPrice = BigDecimal(json["bidPrice"]?.jsonPrimitive?.content ?: "0"),
                askPrice = BigDecimal(json["askPrice"]?.jsonPrimitive?.content ?: "0"),
                volume = BigDecimal(json["volume"]?.jsonPrimitive?.content ?: "0"),
                quoteVolume = BigDecimal(json["quoteVolume"]?.jsonPrimitive?.content ?: "0"),
                priceChange = BigDecimal(json["priceChange"]?.jsonPrimitive?.content ?: "0"),
                priceChangePercent = BigDecimal(json["priceChangePercent"]?.jsonPrimitive?.content ?: "0"),
                high = BigDecimal(json["highPrice"]?.jsonPrimitive?.content ?: "0"),
                low = BigDecimal(json["lowPrice"]?.jsonPrimitive?.content ?: "0"),
                openPrice = BigDecimal(json["openPrice"]?.jsonPrimitive?.content ?: "0"),
                timestamp = Instant.ofEpochMilli(json["closeTime"]?.jsonPrimitive?.long ?: System.currentTimeMillis())
            ).also {
                logger.debug { "Retrieved ticker for $symbol: ${it.lastPrice}" }
            }
            
        } catch (e: ExchangeException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error fetching ticker for $symbol" }
            throw ExchangeException("Failed to fetch ticker: ${e.message}", e)
        }
    }
    
    /**
     * Retrieves order book for a symbol
     */
    override suspend fun getOrderBook(symbol: String, limit: Int): OrderBook {
        logger.debug { "Fetching order book for $symbol (limit: $limit)" }
        
        ensureConnected()
        
        try {
            val baseUrl = config.baseUrl ?: "https://testnet.binance.vision"
            val actualLimit = limit.coerceIn(1, 5000)
            val url = "$baseUrl/api/v3/depth?symbol=${symbol.uppercase()}&limit=$actualLimit"
            
            val response = httpClient.get(url)
            errorHandler.handleResponse(response)
            
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            
            val bids = json["bids"]?.jsonArray?.map { entry ->
                val arr = entry.jsonArray
                OrderBookEntry(
                    price = BigDecimal(arr[0].jsonPrimitive.content),
                    quantity = BigDecimal(arr[1].jsonPrimitive.content)
                )
            } ?: emptyList()
            
            val asks = json["asks"]?.jsonArray?.map { entry ->
                val arr = entry.jsonArray
                OrderBookEntry(
                    price = BigDecimal(arr[0].jsonPrimitive.content),
                    quantity = BigDecimal(arr[1].jsonPrimitive.content)
                )
            } ?: emptyList()
            
            return OrderBook(
                symbol = symbol.uppercase(),
                bids = bids,
                asks = asks,
                timestamp = Instant.ofEpochMilli(System.currentTimeMillis())
            ).also {
                logger.debug { "Retrieved order book for $symbol: ${bids.size} bids, ${asks.size} asks" }
            }
            
        } catch (e: ExchangeException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error fetching order book for $symbol" }
            throw ExchangeException("Failed to fetch order book: ${e.message}", e)
        }
    }
    
    /**
     * Retrieves account balance
     */
    override suspend fun getBalance(): Map<String, BigDecimal> {
        logger.debug { "Fetching account balance" }
        
        ensureConnected()
        
        try {
            val baseUrl = config.baseUrl ?: "https://testnet.binance.vision"
            val signedQuery = authenticator.signQueryString("")
            val url = "$baseUrl/api/v3/account?$signedQuery"
            
            val response = httpClient.get(url) {
                headers {
                    authenticator.createHeaders().forEach { (key, value) ->
                        append(key, value)
                    }
                }
            }
            errorHandler.handleResponse(response)
            
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val balances = json["balances"]?.jsonArray ?: JsonArray(emptyList())
            
            return balances
                .map { it.jsonObject }
                .associate { balance ->
                    val asset = balance["asset"]?.jsonPrimitive?.content ?: ""
                    val free = BigDecimal(balance["free"]?.jsonPrimitive?.content ?: "0")
                    val locked = BigDecimal(balance["locked"]?.jsonPrimitive?.content ?: "0")
                    asset to (free + locked)
                }
                .filterValues { it > BigDecimal.ZERO }
                .also {
                    logger.debug { "Retrieved balances for ${it.size} assets" }
                }
            
        } catch (e: ExchangeException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error fetching account balance" }
            throw ExchangeException("Failed to fetch balance: ${e.message}", e)
        }
    }
    
    /**
     * Retrieves a single position for a symbol
     */
    override suspend fun getPosition(symbol: String): Position? {
        logger.debug { "Fetching position for $symbol" }
        
        val baseAsset = symbol.replace("USDT", "").replace("BUSD", "")
        val balances = getBalance()
        val quantity = balances[baseAsset] ?: return null
        
        if (quantity <= BigDecimal.ZERO) {
            return null
        }
        
        return Position(
            symbol = symbol,
            action = com.fmps.autotrader.shared.enums.TradeAction.LONG,
            quantity = quantity,
            entryPrice = BigDecimal.ZERO,
            currentPrice = BigDecimal.ZERO,
            unrealizedPnL = BigDecimal.ZERO,
            leverage = BigDecimal.ONE,
            openedAt = Instant.now()
        )
    }
    
    /**
     * Retrieves open positions
     */
    override suspend fun getPositions(): List<Position> {
        logger.debug { "Fetching positions" }
        
        val balances = getBalance()
        
        return balances.map { (asset, amount) ->
            Position(
                symbol = "${asset}USDT",
                action = com.fmps.autotrader.shared.enums.TradeAction.LONG,
                quantity = amount,
                entryPrice = BigDecimal.ZERO,
                currentPrice = BigDecimal.ZERO,
                unrealizedPnL = BigDecimal.ZERO,
                leverage = BigDecimal.ONE,
                openedAt = Instant.now()
            )
        }.also {
            logger.debug { "Retrieved ${it.size} positions" }
        }
    }
    
    /**
     * Places an order on Binance
     */
    override suspend fun placeOrder(order: Order): Order {
        logger.info { "Placing ${order.action} order for ${order.symbol}: ${order.quantity} @ ${order.price ?: "MARKET"}" }
        
        ensureConnected()
        
        try {
            val baseUrl = config.baseUrl ?: "https://testnet.binance.vision"
            val params = buildMap {
                put("symbol", order.symbol.uppercase())
                put("side", mapTradeActionToBinance(order.action))
                put("type", order.type.name)
                put("quantity", order.quantity.toPlainString())
                
                val orderPrice = order.price
                if (orderPrice != null && orderPrice > BigDecimal.ZERO) {
                    put("price", orderPrice.toPlainString())
                    put("timeInForce", "GTC")
                }
                
                if (binanceConfig.enableTestOrders) {
                    put("newOrderRespType", "FULL")
                }
            }
            
            val signedParams = authenticator.signParameters(params)
            val queryString = signedParams.entries.joinToString("&") { "${it.key}=${it.value}" }
            val url = "$baseUrl/api/v3/order${if (binanceConfig.enableTestOrders) "/test" else ""}?$queryString"
            
            val response = httpClient.post(url) {
                headers {
                    authenticator.createHeaders().forEach { (key, value) ->
                        append(key, value)
                    }
                }
            }
            errorHandler.handleResponse(response)
            
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            
            return order.copy(
                id = json["orderId"]?.jsonPrimitive?.long?.toString() ?: order.id,
                status = mapBinanceStatusToTradeStatus(json["status"]?.jsonPrimitive?.content),
                filledQuantity = BigDecimal(json["executedQty"]?.jsonPrimitive?.content ?: "0"),
                createdAt = Instant.ofEpochMilli(json["transactTime"]?.jsonPrimitive?.long ?: System.currentTimeMillis())
            ).also {
                logger.info { "Order placed successfully: ${it.id}" }
            }
            
        } catch (e: ExchangeException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error placing order for ${order.symbol}" }
            throw OrderException("Failed to place order: ${e.message}", e)
        }
    }
    
    /**
     * Cancels an order on Binance
     */
    override suspend fun cancelOrder(orderId: String, symbol: String): Order {
        logger.info { "Cancelling order $orderId for $symbol" }
        
        ensureConnected()
        
        try {
            val baseUrl = config.baseUrl ?: "https://testnet.binance.vision"
            val params = mapOf(
                "symbol" to symbol.uppercase(),
                "orderId" to orderId
            )
            
            val signedParams = authenticator.signParameters(params)
            val queryString = signedParams.entries.joinToString("&") { "${it.key}=${it.value}" }
            val url = "$baseUrl/api/v3/order?$queryString"
            
            val response = httpClient.delete(url) {
                headers {
                    authenticator.createHeaders().forEach { (key, value) ->
                        append(key, value)
                    }
                }
            }
            errorHandler.handleResponse(response)
            
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            
            return Order(
                id = json["orderId"]?.jsonPrimitive?.long?.toString() ?: orderId,
                symbol = symbol.uppercase(),
                action = mapBinanceToTradeAction(json["side"]?.jsonPrimitive?.content ?: "BUY"),
                type = com.fmps.autotrader.shared.enums.OrderType.valueOf(
                    json["type"]?.jsonPrimitive?.content ?: "MARKET"
                ),
                quantity = BigDecimal(json["origQty"]?.jsonPrimitive?.content ?: "0"),
                price = json["price"]?.jsonPrimitive?.content?.toBigDecimalOrNull(),
                status = com.fmps.autotrader.shared.enums.TradeStatus.CANCELLED,
                filledQuantity = BigDecimal(json["executedQty"]?.jsonPrimitive?.content ?: "0"),
                createdAt = Instant.now()
            ).also {
                logger.info { "Order cancelled successfully: $orderId" }
            }
            
        } catch (e: ExchangeException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error cancelling order $orderId" }
            throw OrderException("Failed to cancel order: ${e.message}", e)
        }
    }
    
    /**
     * Retrieves order status
     */
    override suspend fun getOrder(orderId: String, symbol: String): Order {
        logger.debug { "Fetching order $orderId for $symbol" }
        
        ensureConnected()
        
        try {
            val baseUrl = config.baseUrl ?: "https://testnet.binance.vision"
            val params = mapOf(
                "symbol" to symbol.uppercase(),
                "orderId" to orderId
            )
            
            val signedParams = authenticator.signParameters(params)
            val queryString = signedParams.entries.joinToString("&") { "${it.key}=${it.value}" }
            val url = "$baseUrl/api/v3/order?$queryString"
            
            val response = httpClient.get(url) {
                headers {
                    authenticator.createHeaders().forEach { (key, value) ->
                        append(key, value)
                    }
                }
            }
            errorHandler.handleResponse(response)
            
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            
            return Order(
                id = json["orderId"]?.jsonPrimitive?.long?.toString() ?: orderId,
                symbol = symbol.uppercase(),
                action = mapBinanceToTradeAction(json["side"]?.jsonPrimitive?.content ?: "BUY"),
                type = com.fmps.autotrader.shared.enums.OrderType.valueOf(
                    json["type"]?.jsonPrimitive?.content ?: "MARKET"
                ),
                quantity = BigDecimal(json["origQty"]?.jsonPrimitive?.content ?: "0"),
                price = json["price"]?.jsonPrimitive?.content?.toBigDecimalOrNull(),
                status = mapBinanceStatusToTradeStatus(json["status"]?.jsonPrimitive?.content),
                filledQuantity = BigDecimal(json["executedQty"]?.jsonPrimitive?.content ?: "0"),
                createdAt = Instant.ofEpochMilli(json["time"]?.jsonPrimitive?.long ?: System.currentTimeMillis())
            ).also {
                logger.debug { "Retrieved order: ${it.id} (${it.status})" }
            }
            
        } catch (e: ExchangeException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error fetching order $orderId" }
            throw OrderException("Failed to fetch order: ${e.message}", e)
        }
    }
    
    /**
     * Closes a position by placing a market order to sell the entire balance
     */
    override suspend fun closePosition(symbol: String): Order {
        logger.info { "Closing position for $symbol" }
        
        // Get current balance for the base asset
        val baseAsset = symbol.replace("USDT", "").replace("BUSD", "")
        val balances = getBalance()
        val quantity = balances[baseAsset] ?: BigDecimal.ZERO
        
        if (quantity <= BigDecimal.ZERO) {
            throw OrderException("No position to close for $symbol")
        }
        
        // Place a market sell order
        val order = Order(
            symbol = symbol,
            action = com.fmps.autotrader.shared.enums.TradeAction.SHORT,
            type = com.fmps.autotrader.shared.enums.OrderType.MARKET,
            quantity = quantity
        )
        
        return placeOrder(order)
    }
    
    /**
     * Retrieves orders (implementation required by AbstractExchangeConnector)
     */
    override suspend fun getOrders(symbol: String?): List<Order> {
        return getOpenOrders(symbol)
    }
    
    /**
     * Retrieves all open orders
     */
    suspend fun getOpenOrders(symbol: String?): List<Order> {
        logger.debug { "Fetching open orders" + if (symbol != null) " for $symbol" else "" }
        
        ensureConnected()
        
        try {
            val baseUrl = config.baseUrl ?: "https://testnet.binance.vision"
            val params = if (symbol != null) {
                mapOf("symbol" to symbol.uppercase())
            } else {
                emptyMap()
            }
            
            val signedParams = authenticator.signParameters(params)
            val queryString = signedParams.entries.joinToString("&") { "${it.key}=${it.value}" }
            val url = "$baseUrl/api/v3/openOrders?$queryString"
            
            val response = httpClient.get(url) {
                headers {
                    authenticator.createHeaders().forEach { (key, value) ->
                        append(key, value)
                    }
                }
            }
            errorHandler.handleResponse(response)
            
            val jsonArray = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            
            return jsonArray.map { element ->
                val json = element.jsonObject
                Order(
                    id = json["orderId"]?.jsonPrimitive?.long?.toString() ?: "",
                    symbol = json["symbol"]?.jsonPrimitive?.content ?: "",
                    action = mapBinanceToTradeAction(json["side"]?.jsonPrimitive?.content ?: "BUY"),
                    type = com.fmps.autotrader.shared.enums.OrderType.valueOf(
                        json["type"]?.jsonPrimitive?.content ?: "MARKET"
                    ),
                    quantity = BigDecimal(json["origQty"]?.jsonPrimitive?.content ?: "0"),
                    price = json["price"]?.jsonPrimitive?.content?.toBigDecimalOrNull(),
                    status = com.fmps.autotrader.shared.enums.TradeStatus.OPEN,
                    filledQuantity = BigDecimal(json["executedQty"]?.jsonPrimitive?.content ?: "0"),
                    createdAt = Instant.ofEpochMilli(json["time"]?.jsonPrimitive?.long ?: System.currentTimeMillis())
                )
            }.also {
                logger.debug { "Retrieved ${it.size} open orders" }
            }
            
        } catch (e: ExchangeException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error fetching open orders" }
            throw OrderException("Failed to fetch open orders: ${e.message}", e)
        }
    }
    
    /**
     * Subscribes to real-time candlestick updates via WebSocket
     */
    override suspend fun subscribeCandlesticks(
        symbol: String,
        interval: TimeFrame,
        callback: suspend (Candlestick) -> Unit
    ): String {
        logger.info { "Subscribing to candlestick stream: $symbol ($interval)" }
        
        if (webSocketManager == null) {
            webSocketManager = BinanceWebSocketManager(binanceConfig, httpClient)
        }
        
        return webSocketManager!!.subscribeCandlesticks(symbol, mapTimeFrameToBinance(interval), callback)
    }
    
    /**
     * Subscribes to real-time ticker updates via WebSocket
     */
    override suspend fun subscribeTicker(symbol: String, callback: suspend (Ticker) -> Unit): String {
        logger.info { "Subscribing to ticker stream: $symbol" }
        
        if (webSocketManager == null) {
            webSocketManager = BinanceWebSocketManager(binanceConfig, httpClient)
        }
        
        return webSocketManager!!.subscribeTicker(symbol, callback)
    }
    
    /**
     * Subscribes to real-time order updates via WebSocket
     */
    override suspend fun subscribeOrderUpdates(callback: suspend (Order) -> Unit): String {
        logger.info { "Subscribing to order updates stream" }
        
        if (webSocketManager == null) {
            webSocketManager = BinanceWebSocketManager(binanceConfig, httpClient)
        }
        
        return webSocketManager!!.subscribeOrderUpdates(authenticator, callback)
    }
    
    /**
     * Unsubscribes from a specific stream
     */
    override suspend fun unsubscribe(subscriptionId: String) {
        logger.info { "Unsubscribing from stream: $subscriptionId" }
        webSocketManager?.unsubscribe(subscriptionId)
    }
    
    // Helper methods
    
    private fun mapTimeFrameToBinance(timeFrame: TimeFrame): String {
        return when (timeFrame) {
            TimeFrame.ONE_MINUTE -> "1m"
            TimeFrame.THREE_MINUTES -> "3m"
            TimeFrame.FIVE_MINUTES -> "5m"
            TimeFrame.FIFTEEN_MINUTES -> "15m"
            TimeFrame.THIRTY_MINUTES -> "30m"
            TimeFrame.ONE_HOUR -> "1h"
            TimeFrame.TWO_HOURS -> "2h"
            TimeFrame.FOUR_HOURS -> "4h"
            TimeFrame.SIX_HOURS -> "6h"
            TimeFrame.EIGHT_HOURS -> "8h"
            TimeFrame.TWELVE_HOURS -> "12h"
            TimeFrame.ONE_DAY -> "1d"
            TimeFrame.THREE_DAYS -> "3d"
            TimeFrame.ONE_WEEK -> "1w"
            TimeFrame.ONE_MONTH -> "1M"
        }
    }
    
    private fun mapBinanceStatusToTradeStatus(status: String?): com.fmps.autotrader.shared.enums.TradeStatus {
        return when (status) {
            "NEW" -> com.fmps.autotrader.shared.enums.TradeStatus.OPEN
            "FILLED" -> com.fmps.autotrader.shared.enums.TradeStatus.FILLED
            "PARTIALLY_FILLED" -> com.fmps.autotrader.shared.enums.TradeStatus.OPEN
            "CANCELED" -> com.fmps.autotrader.shared.enums.TradeStatus.CANCELLED
            else -> com.fmps.autotrader.shared.enums.TradeStatus.OPEN
        }
    }
    
    private fun mapTradeActionToBinance(action: com.fmps.autotrader.shared.enums.TradeAction): String {
        return when (action) {
            com.fmps.autotrader.shared.enums.TradeAction.LONG -> "BUY"
            com.fmps.autotrader.shared.enums.TradeAction.SHORT -> "SELL"
        }
    }
    
    private fun mapBinanceToTradeAction(side: String): com.fmps.autotrader.shared.enums.TradeAction {
        return when (side.uppercase()) {
            "BUY" -> com.fmps.autotrader.shared.enums.TradeAction.LONG
            "SELL" -> com.fmps.autotrader.shared.enums.TradeAction.SHORT
            else -> com.fmps.autotrader.shared.enums.TradeAction.LONG
        }
    }
}

