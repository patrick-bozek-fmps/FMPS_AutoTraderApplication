package com.fmps.autotrader.core.connectors

import com.fmps.autotrader.core.connectors.exceptions.ConnectionException
import com.fmps.autotrader.core.connectors.exceptions.InsufficientFundsException
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.OrderType
import com.fmps.autotrader.shared.enums.TimeFrame
import com.fmps.autotrader.shared.enums.TradeAction
import com.fmps.autotrader.shared.enums.TradeStatus
import com.fmps.autotrader.shared.model.ExchangeConfig
import com.fmps.autotrader.shared.model.Order
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Unit tests for MockExchangeConnector.
 *
 * These tests verify:
 * - Connection lifecycle (configure, connect, disconnect)
 * - Market data retrieval (candles, ticker, order book)
 * - Account information (balances, positions)
 * - Order management (place, cancel, get orders)
 * - WebSocket subscriptions
 * - Error scenarios (not connected, insufficient funds, etc.)
 */
class MockExchangeConnectorTest {

    private lateinit var mockConnector: MockExchangeConnector
    private lateinit var config: ExchangeConfig

    @BeforeEach
    fun setup() {
        mockConnector = MockExchangeConnector(
            exchange = Exchange.BINANCE,
            simulatedLatencyMs = 0, // No latency for faster tests
            failureRate = 0.0
        )

        config = ExchangeConfig(
            exchange = Exchange.BINANCE,
            apiKey = "test-key",
            apiSecret = "test-secret",
            testnet = true
        )
    }

    @AfterEach
    fun tearDown() = runBlocking {
        if (mockConnector.isConnected()) {
            mockConnector.disconnect()
        }
    }

    // ============================================
    // Connection Tests
    // ============================================

    @Test
    fun `test configure and connect`() = runBlocking {
        assertFalse(mockConnector.isConnected())

        mockConnector.configure(config)
        mockConnector.connect()

        assertTrue(mockConnector.isConnected())
        assertEquals(Exchange.BINANCE, mockConnector.getExchange())
    }

    @Test
    fun `test disconnect`() = runBlocking {
        mockConnector.configure(config)
        mockConnector.connect()
        assertTrue(mockConnector.isConnected())

        mockConnector.disconnect()
        assertFalse(mockConnector.isConnected())
    }

    @Test
    fun `test operations fail when not connected`() {
        assertThrows(ConnectionException::class.java) {
            runBlocking { mockConnector.getTicker("BTCUSDT") }
        }
    }

    // ============================================
    // Market Data Tests
    // ============================================

    @Test
    fun `test getCandles returns data`() = runBlocking {
        mockConnector.configure(config)
        mockConnector.connect()

        val candles = mockConnector.getCandles(
            symbol = "BTCUSDT",
            interval = TimeFrame.ONE_HOUR,
            limit = 10
        )

        assertEquals(10, candles.size)
        assertEquals("BTCUSDT", candles[0].symbol)
        assertEquals(TimeFrame.ONE_HOUR, candles[0].interval)
        assertTrue(candles[0].open > BigDecimal.ZERO)
    }

    @Test
    fun `test getTicker returns valid data`() = runBlocking {
        mockConnector.configure(config)
        mockConnector.connect()

        val ticker = mockConnector.getTicker("BTCUSDT")

        assertEquals("BTCUSDT", ticker.symbol)
        assertTrue(ticker.lastPrice > BigDecimal.ZERO)
        assertTrue(ticker.bidPrice < ticker.askPrice)
        assertTrue(ticker.volume > BigDecimal.ZERO)
    }

    @Test
    fun `test getOrderBook returns bids and asks`() = runBlocking {
        mockConnector.configure(config)
        mockConnector.connect()

        val orderBook = mockConnector.getOrderBook("BTCUSDT", limit = 10)

        assertEquals("BTCUSDT", orderBook.symbol)
        assertEquals(10, orderBook.bids.size)
        assertEquals(10, orderBook.asks.size)
        
        // Best bid should be less than best ask (spread exists)
        val bestBid = orderBook.getBestBid()?.price
        val bestAsk = orderBook.getBestAsk()?.price
        assertNotNull(bestBid)
        assertNotNull(bestAsk)
        assertTrue(bestBid!! < bestAsk!!)
    }

    // ============================================
    // Account Information Tests
    // ============================================

    @Test
    fun `test getBalance returns initial balances`() = runBlocking {
        mockConnector.configure(config)
        mockConnector.connect()

        val balances = mockConnector.getBalance()

        assertTrue(balances.containsKey("USDT"))
        assertTrue(balances.containsKey("BTC"))
        assertEquals(BigDecimal("10000.00"), balances["USDT"])
        assertEquals(BigDecimal("0.5"), balances["BTC"])
    }

    @Test
    fun `test setBalance updates balance`() = runBlocking {
        mockConnector.configure(config)
        mockConnector.connect()

        mockConnector.setBalance("USDT", BigDecimal("5000.00"))

        val balances = mockConnector.getBalance()
        assertEquals(BigDecimal("5000.00"), balances["USDT"])
    }

    // ============================================
    // Order Management Tests
    // ============================================

    @Test
    fun `test placeOrder creates order with ID`() = runBlocking {
        mockConnector.configure(config)
        mockConnector.connect()

        val order = Order(
            symbol = "BTCUSDT",
            action = TradeAction.LONG,
            type = OrderType.LIMIT,
            quantity = BigDecimal("0.01"),
            price = BigDecimal("50000.00")
        )

        val placedOrder = mockConnector.placeOrder(order)

        assertNotNull(placedOrder.id)
        assertTrue(placedOrder.id!!.startsWith("MOCK_"))
        assertEquals(TradeStatus.OPEN, placedOrder.status)
        assertEquals(order.symbol, placedOrder.symbol)
        assertEquals(order.quantity, placedOrder.quantity)
    }

    @Test
    fun `test placeOrder market order fills immediately`() = runBlocking {
        mockConnector.configure(config)
        mockConnector.connect()

        val order = Order(
            symbol = "BTCUSDT",
            action = TradeAction.LONG,
            type = OrderType.MARKET,
            quantity = BigDecimal("0.01")
        )

        val filledOrder = mockConnector.placeOrder(order)

        assertEquals(TradeStatus.FILLED, filledOrder.status)
        assertEquals(order.quantity, filledOrder.filledQuantity)
        assertNotNull(filledOrder.averagePrice)
    }

    @Test
    fun `test placeOrder throws InsufficientFundsException`() {
        runBlocking {
            mockConnector.configure(config)
            mockConnector.connect()

            // Set balance to zero
            mockConnector.setBalance("USDT", BigDecimal.ZERO)

            val order = Order(
                symbol = "BTCUSDT",
                action = TradeAction.LONG,
                type = OrderType.MARKET,
                quantity = BigDecimal("0.01")
            )

            assertThrows(InsufficientFundsException::class.java) {
                runBlocking { mockConnector.placeOrder(order) }
            }
        }
    }

    @Test
    fun `test cancelOrder cancels open order`() = runBlocking {
        mockConnector.configure(config)
        mockConnector.connect()

        // Place a limit order
        val order = Order(
            symbol = "BTCUSDT",
            action = TradeAction.LONG,
            type = OrderType.LIMIT,
            quantity = BigDecimal("0.01"),
            price = BigDecimal("50000.00")
        )
        val placedOrder = mockConnector.placeOrder(order)

        // Cancel it
        val cancelledOrder = mockConnector.cancelOrder(placedOrder.id!!, "BTCUSDT")

        assertEquals(TradeStatus.CANCELLED, cancelledOrder.status)
        assertEquals(placedOrder.id, cancelledOrder.id)
    }

    @Test
    fun `test getOrders returns open orders`() = runBlocking {
        mockConnector.configure(config)
        mockConnector.connect()

        // Place two orders
        val order1 = Order(
            symbol = "BTCUSDT",
            action = TradeAction.LONG,
            type = OrderType.LIMIT,
            quantity = BigDecimal("0.01"),
            price = BigDecimal("50000.00")
        )
        mockConnector.placeOrder(order1)

        val order2 = Order(
            symbol = "ETHUSDT",
            action = TradeAction.LONG,
            type = OrderType.LIMIT,
            quantity = BigDecimal("0.1"),
            price = BigDecimal("3000.00")
        )
        mockConnector.placeOrder(order2)

        // Get all orders
        val allOrders = mockConnector.getOrders()
        assertEquals(2, allOrders.size)

        // Get orders for specific symbol
        val btcOrders = mockConnector.getOrders("BTCUSDT")
        assertEquals(1, btcOrders.size)
        assertEquals("BTCUSDT", btcOrders[0].symbol)
    }

    // ============================================
    // WebSocket Tests
    // ============================================

    @Test
    fun `test subscribeCandlesticks returns subscription ID`() = runBlocking {
        mockConnector.configure(config)
        mockConnector.connect()

        var receivedCandle = false
        val subscriptionId = mockConnector.subscribeCandlesticks(
            symbol = "BTCUSDT",
            interval = TimeFrame.ONE_MINUTE
        ) { candle ->
            receivedCandle = true
            assertEquals("BTCUSDT", candle.symbol)
        }

        assertNotNull(subscriptionId)
        assertTrue(subscriptionId.startsWith("candles_"))

        // Unsubscribe
        mockConnector.unsubscribe(subscriptionId)
    }

    @Test
    fun `test subscribeTicker returns subscription ID`() = runBlocking {
        mockConnector.configure(config)
        mockConnector.connect()

        val subscriptionId = mockConnector.subscribeTicker("BTCUSDT") { ticker ->
            assertEquals("BTCUSDT", ticker.symbol)
        }

        assertNotNull(subscriptionId)
        assertTrue(subscriptionId.startsWith("ticker_"))

        // Unsubscribe
        mockConnector.unsubscribe(subscriptionId)
    }

    @Test
    fun `test unsubscribeAll removes all subscriptions`() = runBlocking {
        mockConnector.configure(config)
        mockConnector.connect()

        // Subscribe to multiple streams
        mockConnector.subscribeCandlesticks("BTCUSDT", TimeFrame.ONE_MINUTE) {}
        mockConnector.subscribeTicker("BTCUSDT") {}
        mockConnector.subscribeOrderUpdates {}

        // Unsubscribe from all
        mockConnector.unsubscribeAll()

        // This is a simple test - in a real scenario, we'd verify no more callbacks are received
    }

    // ============================================
    // Mock-Specific Tests
    // ============================================

    @Test
    fun `test setPrice updates market price`() = runBlocking {
        mockConnector.configure(config)
        mockConnector.connect()

        mockConnector.setPrice("BTCUSDT", BigDecimal("60000.00"))

        val ticker = mockConnector.getTicker("BTCUSDT")
        assertEquals(BigDecimal("60000.00"), ticker.lastPrice)
    }

    @Test
    fun `test operation count increments`() = runBlocking {
        mockConnector.configure(config)
        mockConnector.connect()

        val initialCount = mockConnector.getOperationCount()
        
        mockConnector.getTicker("BTCUSDT")
        mockConnector.getBalance()

        val finalCount = mockConnector.getOperationCount()
        assertTrue(finalCount > initialCount)
    }
}

