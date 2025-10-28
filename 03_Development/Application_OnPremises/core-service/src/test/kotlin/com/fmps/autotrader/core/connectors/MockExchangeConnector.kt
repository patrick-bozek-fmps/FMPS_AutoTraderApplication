package com.fmps.autotrader.core.connectors

import com.fmps.autotrader.core.connectors.exceptions.*
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.OrderType
import com.fmps.autotrader.shared.enums.TimeFrame
import com.fmps.autotrader.shared.enums.TradeStatus
import com.fmps.autotrader.shared.model.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * Mock implementation of IExchangeConnector for testing purposes.
 *
 * This mock connector simulates exchange behavior without making real API calls:
 * - Configurable latency for simulating network delays
 * - Configurable error injection for testing error handling
 * - In-memory order book and balance tracking
 * - Simulated order execution with realistic fills
 * - WebSocket stream simulation
 *
 * ## Usage in Tests
 * ```kotlin
 * val mockConnector = MockExchangeConnector(
 *     simulatedLatencyMs = 100,
 *     failureRate = 0.1 // 10% of operations fail
 * )
 * mockConnector.configure(testConfig)
 * mockConnector.connect()
 * ```
 *
 * @param exchange The exchange to simulate (default: BINANCE)
 * @param simulatedLatencyMs Simulated network latency in milliseconds
 * @param failureRate Probability of random failures (0.0 to 1.0)
 * @param simulateSlowness Whether to simulate slow responses
 *
 * @since 1.0.0
 */
class MockExchangeConnector(
    private val exchange: Exchange = Exchange.BINANCE,
    private val simulatedLatencyMs: Long = 50,
    private val failureRate: Double = 0.0,
    private val simulateSlowness: Boolean = false
) : IExchangeConnector {

    // State
    private val connected = AtomicBoolean(false)
    private val configured = AtomicBoolean(false)
    private lateinit var config: ExchangeConfig

    // In-memory data storage
    private val balances = ConcurrentHashMap<String, BigDecimal>()
    private val openOrders = ConcurrentHashMap<String, Order>()
    private val orderHistory = mutableListOf<Order>()
    private val positions = ConcurrentHashMap<String, Position>()
    private val orderIdCounter = AtomicLong(1)

    // WebSocket subscriptions
    private val subscriptions = ConcurrentHashMap<String, Job>()
    private val subscriptionIdCounter = AtomicLong(1)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Market data simulation
    private val currentPrices = ConcurrentHashMap<String, BigDecimal>()

    // Metrics
    private val operationCount = AtomicLong(0)

    init {
        // Initialize default balances
        balances["USDT"] = BigDecimal("10000.00")
        balances["BTC"] = BigDecimal("0.5")
        balances["ETH"] = BigDecimal("5.0")

        // Initialize default prices
        currentPrices["BTCUSDT"] = BigDecimal("50000.00")
        currentPrices["ETHUSDT"] = BigDecimal("3000.00")
    }

    // ============================================
    // Connection Management
    // ============================================

    override fun getExchange(): Exchange = exchange

    override fun configure(config: ExchangeConfig) {
        require(!connected.get()) { "Cannot configure while connected" }
        require(config.exchange == exchange) {
            "Configuration exchange mismatch: expected $exchange, got ${config.exchange}"
        }

        this.config = config
        configured.set(true)
        logger.info { "Mock connector configured for $exchange" }
    }

    override suspend fun connect() {
        checkConfigured()
        require(!connected.get()) { "Already connected" }

        simulateLatency()
        maybeFailRandomly("connect")

        connected.set(true)
        logger.info { "✓ Mock connector connected to $exchange" }
    }

    override suspend fun disconnect() {
        if (!connected.get()) return

        simulateLatency()
        
        // Cancel all subscriptions
        subscriptions.values.forEach { it.cancel() }
        subscriptions.clear()
        
        connected.set(false)
        logger.info { "✓ Mock connector disconnected from $exchange" }
    }

    override fun isConnected(): Boolean = connected.get()

    // ============================================
    // Market Data
    // ============================================

    override suspend fun getCandles(
        symbol: String,
        interval: TimeFrame,
        startTime: Instant?,
        endTime: Instant?,
        limit: Int
    ): List<Candlestick> {
        checkConnected()
        simulateLatency()
        maybeFailRandomly("getCandles")

        val basePrice = currentPrices[symbol] ?: BigDecimal("1000.00")
        val now = Instant.now()

        return (0 until limit).map { i ->
            val timeOffset = (limit - i) * timeFrameToSeconds(interval)
            val open = basePrice.add(BigDecimal(i * 10))
            val variation = BigDecimal("50")

            Candlestick(
                symbol = symbol,
                interval = interval,
                openTime = now.minusSeconds(timeOffset.toLong()),
                closeTime = now.minusSeconds((timeOffset - timeFrameToSeconds(interval)).toLong()),
                open = open,
                high = open.add(variation),
                low = open.subtract(variation),
                close = open.add(BigDecimal(i % 10)),
                volume = BigDecimal("100.5"),
                quoteVolume = BigDecimal("5000000")
            )
        }
    }

    override suspend fun getTicker(symbol: String): Ticker {
        checkConnected()
        simulateLatency()
        maybeFailRandomly("getTicker")

        val price = currentPrices[symbol] ?: BigDecimal("1000.00")

        return Ticker(
            symbol = symbol,
            lastPrice = price,
            bidPrice = price.subtract(BigDecimal("0.50")),
            askPrice = price.add(BigDecimal("0.50")),
            volume = BigDecimal("1000.0"),
            quoteVolume = BigDecimal("50000000.0"),
            priceChange = BigDecimal("100.00"),
            priceChangePercent = BigDecimal("2.5"),
            high = price.add(BigDecimal("500")),
            low = price.subtract(BigDecimal("500")),
            openPrice = price.subtract(BigDecimal("100")),
            timestamp = Instant.now()
        )
    }

    override suspend fun getOrderBook(symbol: String, limit: Int): OrderBook {
        checkConnected()
        simulateLatency()
        maybeFailRandomly("getOrderBook")

        val midPrice = currentPrices[symbol] ?: BigDecimal("1000.00")

        val bids = (1..limit).map { i ->
            OrderBookEntry(
                price = midPrice.subtract(BigDecimal(i)),
                quantity = BigDecimal("${i * 0.1}")
            )
        }

        val asks = (1..limit).map { i ->
            OrderBookEntry(
                price = midPrice.add(BigDecimal(i)),
                quantity = BigDecimal("${i * 0.1}")
            )
        }

        return OrderBook(
            symbol = symbol,
            bids = bids,
            asks = asks,
            timestamp = Instant.now()
        )
    }

    // ============================================
    // Account Information
    // ============================================

    override suspend fun getBalance(): Map<String, BigDecimal> {
        checkConnected()
        simulateLatency()
        maybeFailRandomly("getBalance")

        return balances.toMap()
    }

    override suspend fun getPositions(): List<Position> {
        checkConnected()
        simulateLatency()
        maybeFailRandomly("getPositions")

        return positions.values.toList()
    }

    override suspend fun getPosition(symbol: String): Position? {
        checkConnected()
        simulateLatency()
        maybeFailRandomly("getPosition")

        return positions[symbol]
    }

    // ============================================
    // Order Management
    // ============================================

    override suspend fun placeOrder(order: Order): Order {
        checkConnected()
        simulateLatency()
        maybeFailRandomly("placeOrder")

        // Generate order ID
        val orderId = "MOCK_${orderIdCounter.getAndIncrement()}"

        // Simulate order validation
        validateOrder(order)

        // Create order with ID and OPEN status
        val placedOrder = order.copy(
            id = orderId,
            status = TradeStatus.OPEN,
            createdAt = Instant.now()
        )

        // Store order
        openOrders[orderId] = placedOrder
        orderHistory.add(placedOrder)

        // Simulate immediate fill for market orders
        if (order.type == OrderType.MARKET) {
            return fillOrder(placedOrder)
        }

        logger.info { "✓ Mock order placed: $orderId" }
        return placedOrder
    }

    override suspend fun cancelOrder(orderId: String, symbol: String): Order {
        checkConnected()
        simulateLatency()
        maybeFailRandomly("cancelOrder")

        val order = openOrders[orderId]
            ?: throw OrderException(
                message = "Order not found: $orderId",
                orderId = orderId
            )

        val cancelledOrder = order.copy(
            status = TradeStatus.CANCELLED,
            updatedAt = Instant.now()
        )

        openOrders.remove(orderId)
        orderHistory.add(cancelledOrder)

        logger.info { "✓ Mock order cancelled: $orderId" }
        return cancelledOrder
    }

    override suspend fun getOrder(orderId: String, symbol: String): Order {
        checkConnected()
        simulateLatency()
        maybeFailRandomly("getOrder")

        return openOrders[orderId]
            ?: orderHistory.lastOrNull { it.id == orderId }
            ?: throw OrderException(
                message = "Order not found: $orderId",
                orderId = orderId
            )
    }

    override suspend fun getOrders(symbol: String?): List<Order> {
        checkConnected()
        simulateLatency()
        maybeFailRandomly("getOrders")

        return if (symbol != null) {
            openOrders.values.filter { it.symbol == symbol }
        } else {
            openOrders.values.toList()
        }
    }

    // ============================================
    // Position Management
    // ============================================

    override suspend fun closePosition(symbol: String): Order {
        checkConnected()
        simulateLatency()
        maybeFailRandomly("closePosition")

        val position = positions[symbol]
            ?: throw OrderException("No open position for $symbol")

        // Create a closing order
        val closeOrder = Order(
            symbol = symbol,
            action = when (position.action) {
                com.fmps.autotrader.shared.enums.TradeAction.LONG -> com.fmps.autotrader.shared.enums.TradeAction.SHORT
                com.fmps.autotrader.shared.enums.TradeAction.SHORT -> com.fmps.autotrader.shared.enums.TradeAction.LONG
            },
            type = OrderType.MARKET,
            quantity = position.quantity
        )

        val result = placeOrder(closeOrder)
        positions.remove(symbol)

        return result
    }

    // ============================================
    // WebSocket Streaming
    // ============================================

    override suspend fun subscribeCandlesticks(
        symbol: String,
        interval: TimeFrame,
        callback: suspend (Candlestick) -> Unit
    ): String {
        checkConnected()

        val subscriptionId = "candles_${subscriptionIdCounter.getAndIncrement()}"
        
        val job = scope.launch {
            logger.info { "Mock: Subscribed to candlesticks for $symbol ($interval)" }
            
            while (isActive) {
                delay(timeFrameToMillis(interval))
                
                try {
                    val price = currentPrices[symbol] ?: BigDecimal("1000.00")
                    val variation = BigDecimal("10")
                    
                    callback(Candlestick(
                        symbol = symbol,
                        interval = interval,
                        openTime = Instant.now().minusSeconds(timeFrameToSeconds(interval).toLong()),
                        closeTime = Instant.now(),
                        open = price,
                        high = price.add(variation),
                        low = price.subtract(variation),
                        close = price.add(BigDecimal((Math.random() * 10).toInt())),
                        volume = BigDecimal("50.0"),
                        quoteVolume = BigDecimal("2500000")
                    ))
                } catch (e: Exception) {
                    logger.error(e) { "Error in candlestick callback" }
                }
            }
        }
        
        subscriptions[subscriptionId] = job
        return subscriptionId
    }

    override suspend fun subscribeTicker(
        symbol: String,
        callback: suspend (Ticker) -> Unit
    ): String {
        checkConnected()

        val subscriptionId = "ticker_${subscriptionIdCounter.getAndIncrement()}"
        
        val job = scope.launch {
            logger.info { "Mock: Subscribed to ticker for $symbol" }
            
            while (isActive) {
                delay(1000) // Update every second
                
                try {
                    callback(getTicker(symbol))
                } catch (e: Exception) {
                    logger.error(e) { "Error in ticker callback" }
                }
            }
        }
        
        subscriptions[subscriptionId] = job
        return subscriptionId
    }

    override suspend fun subscribeOrderUpdates(
        callback: suspend (Order) -> Unit
    ): String {
        checkConnected()

        val subscriptionId = "orders_${subscriptionIdCounter.getAndIncrement()}"
        
        val job = scope.launch {
            logger.info { "Mock: Subscribed to order updates" }
            
            // This would normally listen for order updates
            // For mock, we just keep the subscription active
            while (isActive) {
                delay(1000)
            }
        }
        
        subscriptions[subscriptionId] = job
        return subscriptionId
    }

    override suspend fun unsubscribe(subscriptionId: String) {
        val job = subscriptions.remove(subscriptionId)
        if (job != null) {
            job.cancel()
            logger.info { "Mock: Unsubscribed from $subscriptionId" }
        } else {
            throw IllegalArgumentException("Subscription not found: $subscriptionId")
        }
    }

    override suspend fun unsubscribeAll() {
        logger.info { "Mock: Unsubscribing from all streams (${subscriptions.size} active)" }
        subscriptions.values.forEach { it.cancel() }
        subscriptions.clear()
    }

    // ============================================
    // Mock-Specific Methods
    // ============================================

    /**
     * Sets the current price for a symbol.
     */
    fun setPrice(symbol: String, price: BigDecimal) {
        currentPrices[symbol] = price
    }

    /**
     * Sets the balance for an asset.
     */
    fun setBalance(asset: String, amount: BigDecimal) {
        balances[asset] = amount
    }

    /**
     * Clears all orders.
     */
    fun clearOrders() {
        openOrders.clear()
        orderHistory.clear()
    }

    /**
     * Gets operation count for testing.
     */
    fun getOperationCount(): Long = operationCount.get()

    /**
     * Resets operation count.
     */
    fun resetOperationCount() = operationCount.set(0)

    // ============================================
    // Private Helper Methods
    // ============================================

    private fun checkConfigured() {
        if (!configured.get()) {
            throw ExchangeException("Mock connector not configured")
        }
    }

    private fun checkConnected() {
        if (!connected.get()) {
            throw ConnectionException(
                message = "Mock connector not connected to $exchange",
                exchangeName = exchange.name,
                retryable = true
            )
        }
    }

    private suspend fun simulateLatency() {
        operationCount.incrementAndGet()
        
        if (simulatedLatencyMs > 0) {
            val actualLatency = if (simulateSlowness) {
                (simulatedLatencyMs * (0.5 + Math.random())).toLong()
            } else {
                simulatedLatencyMs
            }
            delay(actualLatency)
        }
    }

    private fun maybeFailRandomly(operation: String) {
        if (failureRate > 0 && Math.random() < failureRate) {
            throw ExchangeException(
                message = "Mock simulated failure for operation: $operation"
            )
        }
    }

    private fun validateOrder(order: Order) {
        // Validate sufficient balance
        val baseAsset = order.symbol.replace("USDT", "")
        val quoteAsset = "USDT"

        when (order.action) {
            com.fmps.autotrader.shared.enums.TradeAction.LONG -> {
                // Buying requires quote asset (USDT)
                val balance = balances[quoteAsset] ?: BigDecimal.ZERO
                val required = (order.price ?: currentPrices[order.symbol] ?: BigDecimal.ONE) * order.quantity
                if (balance < required) {
                    throw InsufficientFundsException(
                        message = "Insufficient $quoteAsset balance: have $balance, need $required",
                        asset = quoteAsset
                    )
                }
            }
            com.fmps.autotrader.shared.enums.TradeAction.SHORT -> {
                // Selling requires base asset
                val balance = balances[baseAsset] ?: BigDecimal.ZERO
                if (balance < order.quantity) {
                    throw InsufficientFundsException(
                        message = "Insufficient $baseAsset balance: have $balance, need ${order.quantity}",
                        asset = baseAsset
                    )
                }
            }
        }
    }

    private fun fillOrder(order: Order): Order {
        val fillPrice = currentPrices[order.symbol] ?: order.price ?: BigDecimal("1000.00")

        return order.copy(
            status = TradeStatus.FILLED,
            filledQuantity = order.quantity,
            averagePrice = fillPrice,
            updatedAt = Instant.now()
        ).also {
            openOrders.remove(order.id)
            orderHistory.add(it)
            logger.info { "✓ Mock order filled: ${order.id} at $fillPrice" }
        }
    }

    private fun timeFrameToSeconds(timeFrame: TimeFrame): Int {
        return when (timeFrame) {
            TimeFrame.ONE_MINUTE -> 60
            TimeFrame.THREE_MINUTES -> 180
            TimeFrame.FIVE_MINUTES -> 300
            TimeFrame.FIFTEEN_MINUTES -> 900
            TimeFrame.THIRTY_MINUTES -> 1800
            TimeFrame.ONE_HOUR -> 3600
            TimeFrame.TWO_HOURS -> 7200
            TimeFrame.FOUR_HOURS -> 14400
            TimeFrame.SIX_HOURS -> 21600
            TimeFrame.EIGHT_HOURS -> 28800
            TimeFrame.TWELVE_HOURS -> 43200
            TimeFrame.ONE_DAY -> 86400
            TimeFrame.THREE_DAYS -> 259200
            TimeFrame.ONE_WEEK -> 604800
            TimeFrame.ONE_MONTH -> 2592000
        }
    }

    private fun timeFrameToMillis(timeFrame: TimeFrame): Long {
        return timeFrameToSeconds(timeFrame) * 1000L
    }
}
