package com.fmps.autotrader.core.connectors

import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.TimeFrame
import com.fmps.autotrader.shared.model.*
import java.math.BigDecimal
import java.time.Instant

/**
 * Interface defining the standard contract for all cryptocurrency exchange connectors.
 *
 * This interface provides a unified API for interacting with different cryptocurrency exchanges,
 * abstracting away exchange-specific implementation details. All exchange connectors (Binance, Bitget, etc.)
 * must implement this interface to ensure consistent behavior across the application.
 *
 * ## Lifecycle
 * 1. Create connector via [ConnectorFactory]
 * 2. Configure with [configure]
 * 3. Connect with [connect]
 * 4. Use market data and trading methods
 * 5. Disconnect with [disconnect]
 *
 * ## Thread Safety
 * Implementations should be thread-safe for concurrent access from multiple AI traders.
 *
 * @since 1.0.0
 */
interface IExchangeConnector {

    // ============================================
    // Connection Management
    // ============================================

    /**
     * Returns the exchange type this connector is for.
     *
     * @return The exchange enum value (e.g., BINANCE, BITGET)
     */
    fun getExchange(): Exchange

    /**
     * Configures the connector with exchange-specific settings.
     *
     * This method must be called before [connect]. Configuration includes API credentials,
     * rate limits, timeouts, and other exchange-specific settings.
     *
     * @param config The exchange configuration containing API keys, endpoints, etc.
     * @throws IllegalStateException if connector is already connected
     * @throws IllegalArgumentException if configuration is invalid
     */
    fun configure(config: ExchangeConfig)

    /**
     * Establishes connection to the exchange.
     *
     * This method:
     * - Tests connectivity with a ping or server time request
     * - Verifies API credentials
     * - Initializes rate limiters and health monitors
     * - Sets up WebSocket connections if needed
     *
     * @throws ConnectionException if connection cannot be established
     * @throws AuthenticationException if API credentials are invalid
     */
    suspend fun connect()

    /**
     * Closes the connection to the exchange.
     *
     * This method:
     * - Closes all WebSocket connections
     * - Cancels pending requests
     * - Releases resources
     *
     * Safe to call multiple times.
     */
    suspend fun disconnect()

    /**
     * Checks if the connector is currently connected to the exchange.
     *
     * @return `true` if connected and ready to use, `false` otherwise
     */
    fun isConnected(): Boolean

    // ============================================
    // Market Data
    // ============================================

    /**
     * Retrieves historical candlestick (OHLCV) data for a trading pair.
     *
     * @param symbol The trading pair symbol (e.g., "BTCUSDT")
     * @param interval The candlestick time interval (e.g., 1m, 5m, 1h, 1d)
     * @param startTime Optional start time for historical data (inclusive)
     * @param endTime Optional end time for historical data (inclusive)
     * @param limit Maximum number of candles to retrieve (exchange-specific max, typically 1000-1500)
     * @return List of candlesticks ordered by time (oldest first)
     * @throws ConnectionException if not connected or network error occurs
     * @throws IllegalArgumentException if symbol or interval is invalid
     * @throws RateLimitException if rate limit is exceeded
     */
    suspend fun getCandles(
        symbol: String,
        interval: TimeFrame,
        startTime: Instant? = null,
        endTime: Instant? = null,
        limit: Int = 500
    ): List<Candlestick>

    /**
     * Retrieves the current 24-hour ticker information for a trading pair.
     *
     * @param symbol The trading pair symbol (e.g., "BTCUSDT")
     * @return Current ticker with price, volume, and 24h change information
     * @throws ConnectionException if not connected or network error occurs
     * @throws IllegalArgumentException if symbol is invalid
     * @throws RateLimitException if rate limit is exceeded
     */
    suspend fun getTicker(symbol: String): Ticker

    /**
     * Retrieves the current order book (market depth) for a trading pair.
     *
     * @param symbol The trading pair symbol (e.g., "BTCUSDT")
     * @param limit Maximum number of bids/asks to retrieve (typically 5, 10, 20, 50, 100, 500, 1000)
     * @return Current order book with bids and asks
     * @throws ConnectionException if not connected or network error occurs
     * @throws IllegalArgumentException if symbol or limit is invalid
     * @throws RateLimitException if rate limit is exceeded
     */
    suspend fun getOrderBook(symbol: String, limit: Int = 100): OrderBook

    // ============================================
    // Account Information
    // ============================================

    /**
     * Retrieves the current account balances for all assets.
     *
     * Only returns assets with non-zero balances.
     *
     * @return Map of asset symbol to available balance (e.g., {"BTC" -> 0.5, "USDT" -> 1000.0})
     * @throws ConnectionException if not connected or network error occurs
     * @throws AuthenticationException if API credentials are invalid or insufficient permissions
     * @throws RateLimitException if rate limit is exceeded
     */
    suspend fun getBalance(): Map<String, BigDecimal>

    /**
     * Retrieves all open positions for the account.
     *
     * For spot trading, this may return current balances as positions.
     * For futures/margin trading, returns actual open positions.
     *
     * @return List of open positions
     * @throws ConnectionException if not connected or network error occurs
     * @throws AuthenticationException if API credentials are invalid or insufficient permissions
     * @throws RateLimitException if rate limit is exceeded
     */
    suspend fun getPositions(): List<Position>

    /**
     * Retrieves a specific position by symbol.
     *
     * @param symbol The trading pair symbol (e.g., "BTCUSDT")
     * @return The position for the specified symbol, or null if no position exists
     * @throws ConnectionException if not connected or network error occurs
     * @throws AuthenticationException if API credentials are invalid or insufficient permissions
     * @throws RateLimitException if rate limit is exceeded
     */
    suspend fun getPosition(symbol: String): Position?

    // ============================================
    // Order Management
    // ============================================

    /**
     * Places a new order on the exchange.
     *
     * @param order The order to place (must have all required fields populated)
     * @return The placed order with exchange-assigned ID and status
     * @throws ConnectionException if not connected or network error occurs
     * @throws AuthenticationException if API credentials are invalid or insufficient permissions
     * @throws InsufficientFundsException if account balance is insufficient
     * @throws OrderException if order is rejected by the exchange (invalid parameters, etc.)
     * @throws RateLimitException if rate limit is exceeded
     */
    suspend fun placeOrder(order: Order): Order

    /**
     * Cancels an existing order.
     *
     * @param orderId The exchange-assigned order ID
     * @param symbol The trading pair symbol (required by some exchanges)
     * @return The cancelled order with updated status
     * @throws ConnectionException if not connected or network error occurs
     * @throws AuthenticationException if API credentials are invalid or insufficient permissions
     * @throws OrderException if order cannot be cancelled (already filled, not found, etc.)
     * @throws RateLimitException if rate limit is exceeded
     */
    suspend fun cancelOrder(orderId: String, symbol: String): Order

    /**
     * Retrieves information about a specific order.
     *
     * @param orderId The exchange-assigned order ID
     * @param symbol The trading pair symbol (required by some exchanges)
     * @return The order with current status and fill information
     * @throws ConnectionException if not connected or network error occurs
     * @throws AuthenticationException if API credentials are invalid or insufficient permissions
     * @throws OrderException if order is not found
     * @throws RateLimitException if rate limit is exceeded
     */
    suspend fun getOrder(orderId: String, symbol: String): Order

    /**
     * Retrieves all open orders for the account.
     *
     * @param symbol Optional trading pair symbol to filter orders. If null, returns orders for all symbols.
     * @return List of open orders
     * @throws ConnectionException if not connected or network error occurs
     * @throws AuthenticationException if API credentials are invalid or insufficient permissions
     * @throws RateLimitException if rate limit is exceeded
     */
    suspend fun getOrders(symbol: String? = null): List<Order>

    // ============================================
    // Position Management
    // ============================================

    /**
     * Closes an open position (for futures/margin trading).
     *
     * For spot trading, this method may not be applicable and could throw [UnsupportedOperationException].
     *
     * @param symbol The trading pair symbol (e.g., "BTCUSDT")
     * @return The order that closed the position
     * @throws ConnectionException if not connected or network error occurs
     * @throws AuthenticationException if API credentials are invalid or insufficient permissions
     * @throws OrderException if position cannot be closed
     * @throws RateLimitException if rate limit is exceeded
     * @throws UnsupportedOperationException if exchange/account type doesn't support positions
     */
    suspend fun closePosition(symbol: String): Order

    // ============================================
    // WebSocket Streaming
    // ============================================

    /**
     * Subscribes to real-time candlestick updates for a trading pair.
     *
     * The callback will be invoked whenever a new candlestick is received or an existing one is updated.
     *
     * @param symbol The trading pair symbol (e.g., "BTCUSDT")
     * @param interval The candlestick time interval
     * @param callback Function to be called with each candlestick update
     * @return Subscription ID that can be used to unsubscribe
     * @throws ConnectionException if WebSocket connection fails
     * @throws IllegalArgumentException if symbol or interval is invalid
     */
    suspend fun subscribeCandlesticks(
        symbol: String,
        interval: TimeFrame,
        callback: suspend (Candlestick) -> Unit
    ): String

    /**
     * Subscribes to real-time ticker updates for a trading pair.
     *
     * The callback will be invoked whenever the ticker is updated (usually every 1-3 seconds).
     *
     * @param symbol The trading pair symbol (e.g., "BTCUSDT")
     * @param callback Function to be called with each ticker update
     * @return Subscription ID that can be used to unsubscribe
     * @throws ConnectionException if WebSocket connection fails
     * @throws IllegalArgumentException if symbol is invalid
     */
    suspend fun subscribeTicker(
        symbol: String,
        callback: suspend (Ticker) -> Unit
    ): String

    /**
     * Subscribes to real-time order updates for the account.
     *
     * The callback will be invoked whenever an order is filled, partially filled, cancelled, or rejected.
     *
     * @param callback Function to be called with each order update
     * @return Subscription ID that can be used to unsubscribe
     * @throws ConnectionException if WebSocket connection fails
     * @throws AuthenticationException if API credentials are invalid
     */
    suspend fun subscribeOrderUpdates(
        callback: suspend (Order) -> Unit
    ): String

    /**
     * Unsubscribes from a WebSocket stream.
     *
     * @param subscriptionId The subscription ID returned by a subscribe method
     * @throws IllegalArgumentException if subscription ID is not found
     */
    suspend fun unsubscribe(subscriptionId: String)

    /**
     * Unsubscribes from all active WebSocket streams.
     */
    suspend fun unsubscribeAll()
}

