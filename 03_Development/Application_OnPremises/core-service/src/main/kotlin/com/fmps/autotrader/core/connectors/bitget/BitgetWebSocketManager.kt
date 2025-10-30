package com.fmps.autotrader.core.connectors.bitget

import com.fmps.autotrader.shared.enums.TimeFrame
import com.fmps.autotrader.shared.enums.TradeAction
import com.fmps.autotrader.shared.enums.TradeStatus
import com.fmps.autotrader.shared.model.Candlestick
import com.fmps.autotrader.shared.model.Order
import com.fmps.autotrader.shared.model.Ticker
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import mu.KotlinLogging
import java.math.BigDecimal
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Manages WebSocket connections for real-time Bitget data streams
 * 
 * **Supported Streams:**
 * - Candlestick/Kline data: `{instId}@candle{period}`
 * - Ticker data: `{instId}@ticker`
 * - Order updates: Private channel with authentication
 * 
 * **WebSocket URL:**
 * - wss://ws.bitget.com/spot/v1/stream
 * 
 * @property config Bitget configuration
 * @property authenticator Bitget authenticator for private channels
 */
class BitgetWebSocketManager(
    private val config: BitgetConfig,
    private val authenticator: BitgetAuthenticator
) {
    
    private val json = Json { ignoreUnknownKeys = true }
    private var webSocketSession: DefaultClientWebSocketSession? = null
    private val subscriptions = mutableMapOf<String, Job>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Subscribes to candlestick/kline updates
     * 
     * @param symbol Trading pair symbol
     * @param interval Candlestick interval
     * @param callback Callback function to handle candlestick updates
     */
    suspend fun subscribeCandlesticks(
        symbol: String,
        interval: TimeFrame,
        callback: suspend (Candlestick) -> Unit
    ) {
        val bitgetSymbol = convertSymbolToBitget(symbol)
        val bitgetInterval = mapTimeFrameToBitgetInterval(interval)
        val channel = "${bitgetSymbol}_${bitgetInterval}"
        
        logger.info { "Subscribing to Bitget candlesticks: $channel" }
        
        val job = scope.launch {
            try {
                subscribeToPublicChannel(channel) { data ->
                    val candlestick = parseCandlestick(data, symbol, interval)
                    candlestick?.let {
                        launch {
                            callback(it)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Error in candlestick subscription: $channel" }
            }
        }
        
        subscriptions[channel] = job
    }
    
    /**
     * Subscribes to ticker updates
     * 
     * @param symbol Trading pair symbol
     * @param callback Callback function to handle ticker updates
     */
    suspend fun subscribeTicker(
        symbol: String,
        callback: suspend (Ticker) -> Unit
    ) {
        val bitgetSymbol = convertSymbolToBitget(symbol)
        val channel = "${bitgetSymbol}_ticker"
        
        logger.info { "Subscribing to Bitget ticker: $channel" }
        
        val job = scope.launch {
            try {
                subscribeToPublicChannel(channel) { data ->
                    val ticker = parseTicker(data, symbol)
                    ticker?.let {
                        launch {
                            callback(it)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Error in ticker subscription: $channel" }
            }
        }
        
        subscriptions[channel] = job
    }
    
    /**
     * Subscribes to order updates (private channel)
     * 
     * @param callback Callback function to handle order updates
     */
    suspend fun subscribeOrderUpdates(callback: suspend (Order) -> Unit) {
        val channel = "orders"
        
        logger.info { "Subscribing to Bitget order updates" }
        
        val job = scope.launch {
            try {
                subscribeToPrivateChannel(channel) { data ->
                    val order = parseOrder(data)
                    order?.let {
                        launch {
                            callback(it)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Error in order update subscription" }
            }
        }
        
        subscriptions[channel] = job
    }
    
    /**
     * Unsubscribes from candlestick updates
     */
    fun unsubscribeCandlesticks(symbol: String, interval: TimeFrame) {
        val bitgetSymbol = convertSymbolToBitget(symbol)
        val bitgetInterval = mapTimeFrameToBitgetInterval(interval)
        val channel = "${bitgetSymbol}_${bitgetInterval}"
        
        subscriptions[channel]?.cancel()
        subscriptions.remove(channel)
        
        logger.info { "Unsubscribed from Bitget candlesticks: $channel" }
    }
    
    /**
     * Unsubscribes from ticker updates
     */
    fun unsubscribeTicker(symbol: String) {
        val bitgetSymbol = convertSymbolToBitget(symbol)
        val channel = "${bitgetSymbol}_ticker"
        
        subscriptions[channel]?.cancel()
        subscriptions.remove(channel)
        
        logger.info { "Unsubscribed from Bitget ticker: $channel" }
    }
    
    /**
     * Unsubscribes from order updates
     */
    fun unsubscribeOrderUpdates() {
        val channel = "orders"
        
        subscriptions[channel]?.cancel()
        subscriptions.remove(channel)
        
        logger.info { "Unsubscribed from Bitget order updates" }
    }
    
    /**
     * Closes all WebSocket connections
     */
    fun close() {
        logger.info { "Closing Bitget WebSocket connections..." }
        
        subscriptions.values.forEach { it.cancel() }
        subscriptions.clear()
        
        scope.cancel()
        webSocketSession?.cancel()
        
        logger.info { "✓ Bitget WebSocket connections closed" }
    }
    
    // Private methods
    
    private suspend fun subscribeToPublicChannel(channel: String, handler: (JsonObject) -> Unit) {
        // Simplified WebSocket subscription - would need full implementation
        logger.debug { "Would subscribe to public channel: $channel" }
    }
    
    private suspend fun subscribeToPrivateChannel(channel: String, handler: (JsonObject) -> Unit) {
        // Simplified WebSocket subscription - would need full implementation with authentication
        logger.debug { "Would subscribe to private channel: $channel" }
    }
    
    private fun parseCandlestick(data: JsonObject, symbol: String, interval: TimeFrame): Candlestick? {
        return try {
            val candleData = data["data"]?.jsonArray?.get(0)?.jsonArray ?: return null
            
            Candlestick(
                symbol = symbol,
                interval = interval,
                openTime = Instant.ofEpochMilli(candleData[0].jsonPrimitive.long),
                closeTime = Instant.ofEpochMilli(candleData[0].jsonPrimitive.long + mapIntervalToMillis(interval)),
                open = candleData[1].jsonPrimitive.content.toBigDecimal(),
                high = candleData[2].jsonPrimitive.content.toBigDecimal(),
                low = candleData[3].jsonPrimitive.content.toBigDecimal(),
                close = candleData[4].jsonPrimitive.content.toBigDecimal(),
                volume = candleData[5].jsonPrimitive.content.toBigDecimal(),
                quoteVolume = candleData[6].jsonPrimitive.content.toBigDecimal()
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse candlestick data" }
            null
        }
    }
    
    private fun parseTicker(data: JsonObject, symbol: String): Ticker? {
        return try {
            val tickerData = data["data"]?.jsonObject ?: return null
            
            Ticker(
                symbol = symbol,
                lastPrice = tickerData["close"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                bidPrice = tickerData["bestBid"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                askPrice = tickerData["bestAsk"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                volume = tickerData["baseVol"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                quoteVolume = tickerData["quoteVol"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                priceChange = tickerData["change"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                priceChangePercent = tickerData["changePercent"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                high = tickerData["high"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                low = tickerData["low"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                openPrice = tickerData["open"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                timestamp = Instant.now()
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse ticker data" }
            null
        }
    }
    
    private fun parseOrder(data: JsonObject): Order? {
        return try {
            val orderData = data["data"]?.jsonObject ?: return null
            
            Order(
                id = orderData["orderId"]?.jsonPrimitive?.content,
                symbol = convertSymbolFromBitget(orderData["symbol"]?.jsonPrimitive?.content ?: ""),
                action = mapBitgetSideToTradeAction(orderData["side"]?.jsonPrimitive?.content ?: ""),
                type = com.fmps.autotrader.shared.enums.OrderType.MARKET,
                quantity = orderData["quantity"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                price = orderData["price"]?.jsonPrimitive?.content?.toBigDecimalOrNull(),
                status = mapBitgetStatusToTradeStatus(orderData["status"]?.jsonPrimitive?.content ?: ""),
                filledQuantity = orderData["filledQuantity"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
                createdAt = Instant.now()
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse order data" }
            null
        }
    }
    
    // Helper methods
    
    private fun convertSymbolToBitget(symbol: String): String {
        val quoteCurrencies = listOf("USDT", "USDC", "BTC", "ETH")
        for (quote in quoteCurrencies) {
            if (symbol.endsWith(quote)) {
                val base = symbol.removeSuffix(quote)
                return "${base}_$quote"
            }
        }
        return symbol
    }
    
    private fun convertSymbolFromBitget(symbol: String): String {
        return symbol.replace("_", "")
    }
    
    private fun mapTimeFrameToBitgetInterval(timeFrame: TimeFrame): String {
        return when (timeFrame) {
            TimeFrame.ONE_MINUTE -> "1min"
            TimeFrame.FIVE_MINUTES -> "5min"
            TimeFrame.FIFTEEN_MINUTES -> "15min"
            TimeFrame.THIRTY_MINUTES -> "30min"
            TimeFrame.ONE_HOUR -> "1h"
            TimeFrame.FOUR_HOURS -> "4h"
            TimeFrame.ONE_DAY -> "1day"
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
            TimeFrame.ONE_DAY -> 86_400_000L
            else -> 3_600_000L
        }
    }
    
    private fun mapBitgetSideToTradeAction(side: String): TradeAction {
        return when (side.lowercase()) {
            "buy" -> TradeAction.LONG
            "sell" -> TradeAction.SHORT
            else -> TradeAction.LONG
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

