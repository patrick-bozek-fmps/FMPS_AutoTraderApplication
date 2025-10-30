package com.fmps.autotrader.core.connectors.binance

import com.fmps.autotrader.shared.model.Candlestick
import com.fmps.autotrader.shared.model.Ticker
import com.fmps.autotrader.shared.model.Order
import com.fmps.autotrader.shared.enums.TimeFrame
import mu.KotlinLogging
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * Manages WebSocket connections for Binance real-time data streams
 */
class BinanceWebSocketManager(
    private val config: BinanceConfig,
    private val httpClient: HttpClient
) {
    private val wsHost: String = if (config.testnet) {
        "testnet.binance.vision"
    } else {
        "stream.binance.com:9443"
    }
    private val subscriptionIdCounter = AtomicLong(0)
    private val activeSubscriptions = ConcurrentHashMap<String, WebSocketSession>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var listenKey: String? = null
    private var listenKeyJob: Job? = null
    
    /**
     * Subscribes to real-time candlestick updates
     */
    suspend fun subscribeCandlesticks(
        symbol: String,
        interval: String,
        callback: suspend (Candlestick) -> Unit
    ): String {
        val streamName = "${symbol.lowercase()}@kline_$interval"
        val subscriptionId = "candle_${subscriptionIdCounter.incrementAndGet()}"
        
        logger.info { "Subscribing to candlestick stream: $streamName (ID: $subscriptionId)" }
        
        coroutineScope.launch {
            try {
                httpClient.webSocket(
                    host = wsHost,
                    path = "/ws/$streamName"
                ) {
                    activeSubscriptions[subscriptionId] = this
                    logger.debug { "WebSocket connected for $streamName" }
                    
                    try {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val text = frame.readText()
                                    try {
                                        val candlestick = parseCandlestickMessage(text, symbol.uppercase(), interval)
                                        callback(candlestick)
                                    } catch (e: Exception) {
                                        logger.error(e) { "Error parsing candlestick message: $text" }
                                    }
                                }
                                is Frame.Close -> {
                                    logger.warn { "WebSocket closed for $streamName: ${frame.readReason()}" }
                                    break
                                }
                                else -> {
                                    // Ignore other frame types (Ping, Pong, Binary)
                                }
                            }
                        }
                    } finally {
                        activeSubscriptions.remove(subscriptionId)
                        logger.debug { "WebSocket disconnected for $streamName" }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "WebSocket connection failed for $streamName" }
                activeSubscriptions.remove(subscriptionId)
            }
        }
        
        return subscriptionId
    }
    
    /**
     * Subscribes to real-time ticker updates
     */
    suspend fun subscribeTicker(
        symbol: String,
        callback: suspend (Ticker) -> Unit
    ): String {
        val streamName = "${symbol.lowercase()}@ticker"
        val subscriptionId = "ticker_${subscriptionIdCounter.incrementAndGet()}"
        
        logger.info { "Subscribing to ticker stream: $streamName (ID: $subscriptionId)" }
        
        coroutineScope.launch {
            try {
                httpClient.webSocket(
                    host = wsHost,
                    path = "/ws/$streamName"
                ) {
                    activeSubscriptions[subscriptionId] = this
                    logger.debug { "WebSocket connected for $streamName" }
                    
                    try {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val text = frame.readText()
                                    try {
                                        val ticker = parseTickerMessage(text, symbol.uppercase())
                                        callback(ticker)
                                    } catch (e: Exception) {
                                        logger.error(e) { "Error parsing ticker message: $text" }
                                    }
                                }
                                is Frame.Close -> {
                                    logger.warn { "WebSocket closed for $streamName: ${frame.readReason()}" }
                                    break
                                }
                                else -> {
                                    // Ignore other frame types
                                }
                            }
                        }
                    } finally {
                        activeSubscriptions.remove(subscriptionId)
                        logger.debug { "WebSocket disconnected for $streamName" }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "WebSocket connection failed for $streamName" }
                activeSubscriptions.remove(subscriptionId)
            }
        }
        
        return subscriptionId
    }
    
    /**
     * Subscribes to real-time order updates via user data stream
     */
    suspend fun subscribeOrderUpdates(
        authenticator: BinanceAuthenticator,
        callback: suspend (Order) -> Unit
    ): String {
        val subscriptionId = "orders_${subscriptionIdCounter.incrementAndGet()}"
        
        logger.info { "Subscribing to order updates stream (ID: $subscriptionId)" }
        
        // Create listen key if not already created
        if (listenKey == null) {
            listenKey = createListenKey(authenticator)
            startListenKeyKeepAlive(authenticator)
        }
        
        val currentListenKey = listenKey ?: run {
            logger.error { "Failed to create listen key for order updates" }
            return subscriptionId
        }
        
        coroutineScope.launch {
            try {
                httpClient.webSocket(
                    host = wsHost,
                    path = "/ws/$currentListenKey"
                ) {
                    activeSubscriptions[subscriptionId] = this
                    logger.debug { "WebSocket connected for user data stream" }
                    
                    try {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val text = frame.readText()
                                    try {
                                        val order = parseOrderUpdateMessage(text)
                                        if (order != null) {
                                            callback(order)
                                        }
                                    } catch (e: Exception) {
                                        logger.error(e) { "Error parsing order update message: $text" }
                                    }
                                }
                                is Frame.Close -> {
                                    logger.warn { "WebSocket closed for user data stream: ${frame.readReason()}" }
                                    break
                                }
                                else -> {
                                    // Ignore other frame types
                                }
                            }
                        }
                    } finally {
                        activeSubscriptions.remove(subscriptionId)
                        logger.debug { "WebSocket disconnected for user data stream" }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "WebSocket connection failed for user data stream" }
                activeSubscriptions.remove(subscriptionId)
            }
        }
        
        return subscriptionId
    }
    
    /**
     * Unsubscribes from a specific stream
     */
    suspend fun unsubscribe(subscriptionId: String) {
        logger.info { "Unsubscribing from stream: $subscriptionId" }
        
        val session = activeSubscriptions.remove(subscriptionId)
        session?.close(CloseReason(CloseReason.Codes.NORMAL, "Client unsubscribed"))
    }
    
    /**
     * Closes all WebSocket connections
     */
    suspend fun close() {
        logger.info { "Closing all WebSocket connections (${activeSubscriptions.size} active)" }
        
        // Stop listen key keep-alive
        listenKeyJob?.cancel()
        listenKeyJob = null
        
        // Close listen key
        listenKey?.let { deleteListenKey(it) }
        listenKey = null
        
        // Close all active subscriptions
        activeSubscriptions.values.forEach { session ->
            try {
                session.close(CloseReason(CloseReason.Codes.NORMAL, "Manager shutdown"))
            } catch (e: Exception) {
                logger.warn(e) { "Error closing WebSocket session" }
            }
        }
        activeSubscriptions.clear()
        
        // Cancel coroutine scope
        coroutineScope.cancel()
        
        logger.info { "All WebSocket connections closed" }
    }
    
    /**
     * Creates a listen key for user data stream
     */
    private suspend fun createListenKey(authenticator: BinanceAuthenticator): String {
        logger.debug { "Creating listen key for user data stream" }
        
        val baseUrl = config.baseUrl ?: "https://testnet.binance.vision"
        val response = httpClient.post("$baseUrl/api/v3/userDataStream") {
            headers {
                authenticator.createHeaders().forEach { (key, value) ->
                    append(key, value)
                }
            }
        }
        
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val key = json["listenKey"]?.jsonPrimitive?.content
            ?: throw Exception("Failed to create listen key")
        
        logger.debug { "Listen key created: ${key.take(8)}..." }
        return key
    }
    
    /**
     * Deletes a listen key
     */
    private suspend fun deleteListenKey(key: String) {
        logger.debug { "Deleting listen key: ${key.take(8)}..." }
        
        try {
            val baseUrl = config.baseUrl ?: "https://testnet.binance.vision"
            httpClient.delete("$baseUrl/api/v3/userDataStream?listenKey=$key")
            logger.debug { "Listen key deleted" }
        } catch (e: Exception) {
            logger.warn(e) { "Error deleting listen key" }
        }
    }
    
    /**
     * Starts a background job to keep the listen key alive
     */
    private fun startListenKeyKeepAlive(authenticator: BinanceAuthenticator) {
        listenKeyJob?.cancel()
        
        listenKeyJob = coroutineScope.launch {
            while (isActive) {
                delay(30 * 60 * 1000) // 30 minutes
                
                listenKey?.let { key ->
                    try {
                        val baseUrl = config.baseUrl ?: "https://testnet.binance.vision"
                        logger.debug { "Keeping listen key alive: ${key.take(8)}..." }
                        httpClient.put("$baseUrl/api/v3/userDataStream?listenKey=$key") {
                            headers {
                                authenticator.createHeaders().forEach { (k, v) ->
                                    append(k, v)
                                }
                            }
                        }
                        logger.debug { "Listen key keep-alive sent" }
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to keep listen key alive" }
                    }
                }
            }
        }
    }
    
    /**
     * Parses candlestick message from WebSocket
     */
    private fun parseCandlestickMessage(message: String, symbol: String, intervalStr: String): Candlestick {
        val json = Json.parseToJsonElement(message).jsonObject
        val kline = json["k"]?.jsonObject ?: throw Exception("Missing 'k' field in candlestick message")
        
        val interval = mapBinanceIntervalToTimeFrame(intervalStr)
        
        return Candlestick(
            symbol = symbol,
            interval = interval,
            openTime = Instant.ofEpochMilli(kline["t"]?.jsonPrimitive?.long ?: 0),
            closeTime = Instant.ofEpochMilli(kline["T"]?.jsonPrimitive?.long ?: 0),
            open = BigDecimal(kline["o"]?.jsonPrimitive?.content ?: "0"),
            high = BigDecimal(kline["h"]?.jsonPrimitive?.content ?: "0"),
            low = BigDecimal(kline["l"]?.jsonPrimitive?.content ?: "0"),
            close = BigDecimal(kline["c"]?.jsonPrimitive?.content ?: "0"),
            volume = BigDecimal(kline["v"]?.jsonPrimitive?.content ?: "0"),
            quoteVolume = BigDecimal(kline["q"]?.jsonPrimitive?.content ?: "0")
        )
    }
    
    /**
     * Parses ticker message from WebSocket
     */
    private fun parseTickerMessage(message: String, symbol: String): Ticker {
        val json = Json.parseToJsonElement(message).jsonObject
        
        return Ticker(
            symbol = symbol,
            lastPrice = BigDecimal(json["c"]?.jsonPrimitive?.content ?: "0"),
            bidPrice = BigDecimal(json["b"]?.jsonPrimitive?.content ?: "0"),
            askPrice = BigDecimal(json["a"]?.jsonPrimitive?.content ?: "0"),
            volume = BigDecimal(json["v"]?.jsonPrimitive?.content ?: "0"),
            quoteVolume = BigDecimal(json["q"]?.jsonPrimitive?.content ?: "0"),
            priceChange = BigDecimal(json["p"]?.jsonPrimitive?.content ?: "0"),
            priceChangePercent = BigDecimal(json["P"]?.jsonPrimitive?.content ?: "0"),
            high = BigDecimal(json["h"]?.jsonPrimitive?.content ?: "0"),
            low = BigDecimal(json["l"]?.jsonPrimitive?.content ?: "0"),
            openPrice = BigDecimal(json["o"]?.jsonPrimitive?.content ?: "0"),
            timestamp = Instant.ofEpochMilli(json["E"]?.jsonPrimitive?.long ?: System.currentTimeMillis())
        )
    }
    
    /**
     * Parses order update message from WebSocket
     */
    private fun parseOrderUpdateMessage(message: String): Order? {
        val json = Json.parseToJsonElement(message).jsonObject
        
        // Check if this is an executionReport event
        if (json["e"]?.jsonPrimitive?.content != "executionReport") {
            return null
        }
        
        return Order(
            id = json["i"]?.jsonPrimitive?.long?.toString() ?: "",
            symbol = json["s"]?.jsonPrimitive?.content ?: "",
            action = when (json["S"]?.jsonPrimitive?.content) {
                "BUY" -> com.fmps.autotrader.shared.enums.TradeAction.LONG
                "SELL" -> com.fmps.autotrader.shared.enums.TradeAction.SHORT
                else -> com.fmps.autotrader.shared.enums.TradeAction.LONG
            },
            type = when (json["o"]?.jsonPrimitive?.content) {
                "MARKET" -> com.fmps.autotrader.shared.enums.OrderType.MARKET
                "LIMIT" -> com.fmps.autotrader.shared.enums.OrderType.LIMIT
                else -> com.fmps.autotrader.shared.enums.OrderType.MARKET
            },
            quantity = BigDecimal(json["q"]?.jsonPrimitive?.content ?: "0"),
            price = json["p"]?.jsonPrimitive?.content?.toBigDecimalOrNull(),
            status = when (json["X"]?.jsonPrimitive?.content) {
                "NEW" -> com.fmps.autotrader.shared.enums.TradeStatus.OPEN
                "FILLED" -> com.fmps.autotrader.shared.enums.TradeStatus.FILLED
                "PARTIALLY_FILLED" -> com.fmps.autotrader.shared.enums.TradeStatus.OPEN
                "CANCELED" -> com.fmps.autotrader.shared.enums.TradeStatus.CANCELLED
                else -> com.fmps.autotrader.shared.enums.TradeStatus.OPEN
            },
            filledQuantity = BigDecimal(json["z"]?.jsonPrimitive?.content ?: "0"),
            createdAt = Instant.ofEpochMilli(json["E"]?.jsonPrimitive?.long ?: System.currentTimeMillis())
        )
    }
    
    private fun mapBinanceIntervalToTimeFrame(interval: String): TimeFrame {
        return when (interval) {
            "1m" -> TimeFrame.ONE_MINUTE
            "3m" -> TimeFrame.THREE_MINUTES
            "5m" -> TimeFrame.FIVE_MINUTES
            "15m" -> TimeFrame.FIFTEEN_MINUTES
            "30m" -> TimeFrame.THIRTY_MINUTES
            "1h" -> TimeFrame.ONE_HOUR
            "2h" -> TimeFrame.TWO_HOURS
            "4h" -> TimeFrame.FOUR_HOURS
            "6h" -> TimeFrame.SIX_HOURS
            "8h" -> TimeFrame.EIGHT_HOURS
            "12h" -> TimeFrame.TWELVE_HOURS
            "1d" -> TimeFrame.ONE_DAY
            "3d" -> TimeFrame.THREE_DAYS
            "1w" -> TimeFrame.ONE_WEEK
            "1M" -> TimeFrame.ONE_MONTH
            else -> TimeFrame.ONE_MINUTE
        }
    }
}

