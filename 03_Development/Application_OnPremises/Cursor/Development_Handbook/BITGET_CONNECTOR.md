# Bitget Connector Documentation

**Status**: ✅ Complete  
**Version**: 1.0  
**Last Updated**: October 30, 2025  
**Related Issue**: [#9 - Bitget Connector Implementation](../Development_Plan/Issue_09_Bitget_Connector.md)

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Configuration](#configuration)
4. [API Methods](#api-methods)
5. [WebSocket Streams](#websocket-streams)
6. [Authentication](#authentication)
7. [Error Handling](#error-handling)
8. [Rate Limiting](#rate-limiting)
9. [Testing](#testing)
10. [Usage Examples](#usage-examples)

---

## Overview

The Bitget connector provides a unified interface to interact with the Bitget exchange API, supporting both testnet and production environments. It implements the `IExchangeConnector` interface and provides comprehensive functionality for market data retrieval, account management, order execution, and real-time WebSocket streaming.

### Key Features

- ✅ Full REST API integration for spot trading
- ✅ HMAC SHA256 authentication with passphrase support
- ✅ Automatic symbol format conversion (BTCUSDT ↔ BTC_USDT)
- ✅ WebSocket streaming for real-time data
- ✅ Comprehensive error handling with Bitget-specific error codes
- ✅ Client-side rate limiting
- ✅ Server time synchronization
- ✅ Retry logic with exponential backoff
- ✅ Connection health monitoring

### Components

| Component | Purpose |
|-----------|---------|
| `BitgetConnector` | Main connector implementing `IExchangeConnector` |
| `BitgetAuthenticator` | HMAC SHA256 signature generation with passphrase |
| `BitgetErrorHandler` | Maps Bitget error codes to framework exceptions |
| `BitgetWebSocketManager` | Manages WebSocket connections and subscriptions |
| `BitgetConfig` | Configuration wrapper with testnet/production modes |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        BitgetConnector                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  IExchangeConnector Interface                           │   │
│  │  - Market Data (Candles, Ticker, OrderBook)             │   │
│  │  - Account Management (Balance, Positions)              │   │
│  │  - Order Management (Place, Cancel, Query)              │   │
│  │  - WebSocket Streaming                                  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │ BitgetAuthenti  │  │ BitgetErrorHand  │  │ BitgetWebSoc │  │
│  │ cator           │  │ ler              │  │ ketManager   │  │
│  │                 │  │                  │  │              │  │
│  │ - Sign Request  │  │ - Map Error      │  │ - Subscribe  │  │
│  │ - Add Headers   │  │   Codes          │  │ - Parse Data │  │
│  │ - Sync Time     │  │ - Create         │  │ - Manage     │  │
│  │                 │  │   Exceptions     │  │   Callbacks  │  │
│  └────────┬────────┘  └────────┬─────────┘  └──────┬───────┘  │
│           │                    │                    │          │
│           └────────────────────┴────────────────────┘          │
│                               │                                │
└───────────────────────────────┼────────────────────────────────┘
                                │
                    ┌───────────┴────────────┐
                    │                        │
            ┌───────▼────────┐      ┌────────▼─────────┐
            │  Bitget REST   │      │  Bitget WebSocket│
            │  API           │      │  Streams         │
            │  (HTTPS)       │      │  (WSS)           │
            └────────────────┘      └──────────────────┘
```

### Symbol Format Conversion

Bitget uses underscore-separated symbols while our system uses concatenated format:

| System Format | Bitget Format |
|---------------|---------------|
| BTCUSDT       | BTC_USDT     |
| ETHUSDT       | ETH_USDT     |
| BNBUSDC       | BNB_USDC     |

The connector automatically handles this conversion in both directions.

---

## Configuration

### Environment Variables

```bash
# PowerShell
$env:BITGET_API_KEY="your_api_key"
$env:BITGET_API_SECRET="your_api_secret"
$env:BITGET_API_PASSPHRASE="your_passphrase"

# Bash/Linux
export BITGET_API_KEY="your_api_key"
export BITGET_API_SECRET="your_api_secret"
export BITGET_API_PASSPHRASE="your_passphrase"
```

### Configuration File (application.conf)

```hocon
exchanges {
    bitget {
        enabled = false  # Set to true to enable
        
        api {
            key = ""  # Or use environment variable
            key = ${?BITGET_API_KEY}
            
            secret = ""
            secret = ${?BITGET_API_SECRET}
            
            passphrase = ""
            passphrase = ${?BITGET_API_PASSPHRASE}
            
            baseUrl = "https://api.bitget.com"
            websocketUrl = "wss://ws.bitget.com/spot/v1/stream"
        }
        
        rateLimit {
            requestsPerMinute = 600
            requestsPerSecond = 10
        }
    }
}
```

### Kotlin Configuration

```kotlin
// Testnet/Demo Configuration
val config = BitgetConfig.testnet(
    apiKey = "your_api_key",
    apiSecret = "your_api_secret",
    passphrase = "your_passphrase"
)

// Production Configuration
val config = BitgetConfig.production(
    apiKey = "your_api_key",
    apiSecret = "your_api_secret",
    passphrase = "your_passphrase"
)

// Using ExchangeConfig directly (passphrase must be provided)
val config = ExchangeConfig(
    exchange = Exchange.BITGET,
    apiKey = "your_api_key",
    apiSecret = "your_api_secret",
    passphrase = "your_passphrase",  // REQUIRED for Bitget
    testnet = true,
    rateLimitConfig = RateLimitConfig(...),
    // ... other settings
)
```

### Important Notes

⚠️ **Passphrase is Required**: Unlike Binance, Bitget requires a passphrase for API authentication. The connector will fail if the passphrase is not provided.

⚠️ **Testnet Access**: Bitget may not have a separate testnet URL. Check with Bitget documentation for current testnet availability.

---

## API Methods

### Connection Management

```kotlin
val connector = BitgetConnector()
connector.configure(config)

// Connect to Bitget
connector.connect()

// Check connection status
if (connector.isConnected()) {
    println("Connected to Bitget")
}

// Disconnect
connector.disconnect()
```

### Market Data

#### Get Candlesticks

```kotlin
val candlesticks = connector.getCandles(
    symbol = "BTCUSDT",
    interval = TimeFrame.ONE_HOUR,
    startTime = Instant.now().minus(1, ChronoUnit.DAYS),
    endTime = Instant.now(),
    limit = 100
)

candlesticks.forEach { candle ->
    println("${candle.openTime}: O=${candle.open} H=${candle.high} L=${candle.low} C=${candle.close}")
}
```

#### Get Ticker

```kotlin
val ticker = connector.getTicker("BTCUSDT")
println("Last Price: ${ticker.lastPrice}")
println("24h Volume: ${ticker.volume}")
println("24h Change: ${ticker.priceChangePercent}%")
```

#### Get Order Book

```kotlin
val orderBook = connector.getOrderBook(
    symbol = "BTCUSDT",
    limit = 20
)

println("Best Bid: ${orderBook.bids.first().price}")
println("Best Ask: ${orderBook.asks.first().price}")
```

### Account Information

#### Get Balance

```kotlin
val balances = connector.getBalance()
balances.forEach { (asset, amount) ->
    println("$asset: $amount")
}
```

#### Get Positions

```kotlin
val positions = connector.getPositions()
// Note: Spot trading returns empty list
println("Open positions: ${positions.size}")
```

### Order Management

#### Place Order

```kotlin
val order = Order(
    symbol = "BTCUSDT",
    action = TradeAction.LONG,
    type = OrderType.LIMIT,
    quantity = BigDecimal("0.001"),
    price = BigDecimal("50000.00")
)

val placedOrder = connector.placeOrder(order)
println("Order placed: ${placedOrder.id}")
```

#### Cancel Order

```kotlin
val cancelledOrder = connector.cancelOrder(
    orderId = "12345678",
    symbol = "BTCUSDT"
)
println("Order cancelled: ${cancelledOrder.status}")
```

#### Get Order Status

```kotlin
val order = connector.getOrder(
    orderId = "12345678",
    symbol = "BTCUSDT"
)
println("Order status: ${order.status}")
println("Filled: ${order.filledQuantity} / ${order.quantity}")
```

---

## WebSocket Streams

### Subscribe to Candlesticks

```kotlin
val subscriptionId = connector.subscribeCandlesticks(
    symbol = "BTCUSDT",
    interval = TimeFrame.ONE_MINUTE
) { candlestick ->
    println("New candle: ${candlestick.close} @ ${candlestick.closeTime}")
}

// Later...
connector.unsubscribe(subscriptionId)
```

### Subscribe to Ticker Updates

```kotlin
val subscriptionId = connector.subscribeTicker("BTCUSDT") { ticker ->
    println("Price update: ${ticker.lastPrice}")
}

// Unsubscribe
connector.unsubscribe(subscriptionId)
```

### Subscribe to Order Updates

```kotlin
val subscriptionId = connector.subscribeOrderUpdates { order ->
    println("Order update: ${order.id} - ${order.status}")
}

// Unsubscribe
connector.unsubscribe(subscriptionId)
```

### Unsubscribe from All Streams

```kotlin
connector.unsubscribeAll()
```

---

## Authentication

Bitget uses HMAC SHA256 signature-based authentication with an additional passphrase parameter.

### Signature Generation Process

1. **Create Prehash String**:
   ```
   prehash = timestamp + method + requestPath + queryString
   ```

2. **Generate Signature**:
   ```
   signature = Base64(HMAC_SHA256(prehash, apiSecret))
   ```

3. **Add Headers**:
   - `ACCESS-KEY`: API key
   - `ACCESS-SIGN`: Base64 encoded signature
   - `ACCESS-TIMESTAMP`: Request timestamp (milliseconds)
   - `ACCESS-PASSPHRASE`: API passphrase
   - `Content-Type`: application/json

### Example

```kotlin
val authenticator = BitgetAuthenticator(
    apiKey = "your_api_key",
    apiSecret = "your_api_secret",
    passphrase = "your_passphrase"
)

val headers = authenticator.createHeaders(
    method = "GET",
    requestPath = "/api/spot/v1/account/assets",
    queryString = ""
)
```

### Time Synchronization

The connector automatically synchronizes with Bitget server time to prevent timestamp errors:

```kotlin
// Automatic synchronization happens during connect()
connector.connect()

// Manual time offset update
authenticator.updateTimestampOffset(serverTimeMillis)
```

---

## Error Handling

### Error Code Mapping

| Bitget Error Code | Exception Type | Description |
|-------------------|----------------|-------------|
| 40002, 40003, 40005, 40008 | `AuthenticationException` | Invalid credentials or signature |
| 40004 | `ConnectionException` | Timestamp error (clock sync issue) |
| 40006, 40007 | `AuthenticationException` | Access denied or IP restrictions |
| 40014 | `RateLimitException` | Rate limit exceeded |
| 43001 | `InsufficientFundsException` | Insufficient balance |
| 43002, 43003 | `OrderException` | Order not found or already cancelled |
| 43111, 43112 | `OrderException` | Order size errors |
| 40001 | `ExchangeException` | Invalid parameters |

### Example Error Handling

```kotlin
try {
    val ticker = connector.getTicker("BTCUSDT")
} catch (e: RateLimitException) {
    println("Rate limit exceeded. Retry after: ${e.retryAfterMs}ms")
    delay(e.retryAfterMs ?: 60000)
} catch (e: AuthenticationException) {
    println("Authentication failed: ${e.message}")
    // Check API credentials
} catch (e: ConnectionException) {
    println("Connection error: ${e.message}")
    // Retry connection
} catch (e: ExchangeException) {
    println("Bitget API error: ${e.message}")
}
```

---

## Rate Limiting

### Bitget Rate Limits

- **General API**: ~10 requests per second
- **Weight-based system**: Similar to Binance

### Client-Side Rate Limiting

The connector implements token bucket algorithm for client-side rate limiting:

```kotlin
val config = BitgetConfig.testnet(
    apiKey = apiKey,
    apiSecret = apiSecret,
    passphrase = passphrase
)

// Rate limit is configured in baseExchangeConfig
config.baseExchangeConfig.rateLimitConfig.apply {
    requestsPerSecond = 10.0
    burstCapacity = 20
}
```

### Rate Limit Headers

Bitget may return rate limit information in response headers:
- Check response headers for limit details
- The connector automatically handles 429 (Too Many Requests) responses

---

## Testing

### Unit Tests

```kotlin
// MockExchangeConnector can simulate Bitget responses
val mockConnector = MockExchangeConnector(Exchange.BITGET)
mockConnector.configure(config)
```

### Integration Tests

```bash
# Set environment variables
$env:BITGET_API_KEY="your_testnet_api_key"
$env:BITGET_API_SECRET="your_testnet_api_secret"
$env:BITGET_API_PASSPHRASE="your_testnet_passphrase"

# Run integration tests
./gradlew :core-service:integrationTest --tests "*BitgetConnectorIntegrationTest"
```

### Test Coverage

Integration tests cover:
- ✅ Connection and authentication
- ✅ Candlestick data retrieval
- ✅ Ticker data retrieval
- ✅ Order book retrieval
- ✅ Balance queries
- ✅ Position queries (returns empty for spot)
- ✅ WebSocket subscription methods
- ✅ Error handling
- ✅ Symbol format conversion

---

## Usage Examples

### Complete Trading Flow

```kotlin
// 1. Create and configure connector
val connector = BitgetConnector()
val config = BitgetConfig.testnet(
    apiKey = System.getenv("BITGET_API_KEY")!!,
    apiSecret = System.getenv("BITGET_API_SECRET")!!,
    passphrase = System.getenv("BITGET_API_PASSPHRASE")!!
)
connector.configure(config)

// 2. Connect
connector.connect()

try {
    // 3. Get market data
    val ticker = connector.getTicker("BTCUSDT")
    println("Current BTC price: ${ticker.lastPrice}")
    
    // 4. Check balance
    val balances = connector.getBalance()
    val usdtBalance = balances["USDT"] ?: BigDecimal.ZERO
    println("USDT Balance: $usdtBalance")
    
    // 5. Place order (if sufficient balance)
    if (usdtBalance > BigDecimal("100")) {
        val order = Order(
            symbol = "BTCUSDT",
            action = TradeAction.LONG,
            type = OrderType.LIMIT,
            quantity = BigDecimal("0.001"),
            price = ticker.lastPrice * BigDecimal("0.99") // 1% below market
        )
        
        val placedOrder = connector.placeOrder(order)
        println("Order placed: ${placedOrder.id}")
        
        // 6. Monitor order status
        delay(5000)
        val orderStatus = connector.getOrder(placedOrder.id!!, "BTCUSDT")
        println("Order status: ${orderStatus.status}")
        
        // 7. Cancel if not filled
        if (orderStatus.status == TradeStatus.PENDING) {
            connector.cancelOrder(placedOrder.id!!, "BTCUSDT")
            println("Order cancelled")
        }
    }
    
} catch (e: Exception) {
    println("Error: ${e.message}")
    e.printStackTrace()
} finally {
    // 8. Disconnect
    connector.disconnect()
}
```

### Real-Time Price Monitoring

```kotlin
val connector = BitgetConnector()
connector.configure(config)
connector.connect()

// Subscribe to real-time ticker updates
val subscriptionId = connector.subscribeTicker("BTCUSDT") { ticker ->
    println("[${Instant.now()}] BTC: ${ticker.lastPrice} (${ticker.priceChangePercent}%)")
}

// Run for 1 minute
delay(60000)

// Unsubscribe and disconnect
connector.unsubscribe(subscriptionId)
connector.disconnect()
```

---

## Best Practices

### 1. Configuration Management
- ✅ Always use environment variables for API credentials
- ✅ Never commit API keys to version control
- ✅ Use testnet for development and testing
- ✅ Validate configuration before connecting

### 2. Error Handling
- ✅ Always wrap API calls in try-catch blocks
- ✅ Implement retry logic for transient errors
- ✅ Log errors with appropriate detail levels
- ✅ Handle rate limits gracefully with exponential backoff

### 3. Connection Management
- ✅ Reuse connector instances when possible
- ✅ Always disconnect when done
- ✅ Monitor connection health
- ✅ Implement automatic reconnection for WebSocket streams

### 4. Rate Limiting
- ✅ Respect Bitget rate limits
- ✅ Use client-side rate limiting
- ✅ Batch requests when possible
- ✅ Monitor API usage

### 5. Testing
- ✅ Use integration tests with testnet
- ✅ Test error scenarios
- ✅ Validate all data transformations
- ✅ Test WebSocket reconnection logic

---

## Troubleshooting

### Authentication Errors

**Problem**: `AuthenticationException: Invalid signature`

**Solutions**:
- Check that passphrase is correctly set
- Verify API key and secret are correct
- Ensure system clock is synchronized
- Check that API key has required permissions

---

### Timestamp Errors

**Problem**: `ConnectionException: Bitget timestamp error`

**Solution**:
```bash
# Synchronize system clock (Windows)
w32tm /resync

# Synchronize system clock (Linux)
sudo ntpdate pool.ntp.org
```

---

### Rate Limit Errors

**Problem**: `RateLimitException: Bitget rate limit exceeded`

**Solutions**:
- Reduce request frequency
- Implement exponential backoff
- Use WebSocket streams instead of polling
- Check Bitget API documentation for current limits

---

### Symbol Format Errors

**Problem**: Invalid symbol errors

**Solution**: The connector automatically converts symbols, but ensure you're using standard format:
- ✅ Use: `BTCUSDT`
- ❌ Don't use: `BTC_USDT` (Bitget internal format)

---

## Additional Resources

- [Bitget API Documentation](https://www.bitget.com/api-doc/spot/intro)
- [Exchange Connector Framework Guide](EXCHANGE_CONNECTOR_GUIDE.md)
- [Issue #9: Bitget Connector Implementation](../Development_Plan/Issue_09_Bitget_Connector.md)
- [EPIC 2: Exchange Integration Status](../Development_Plan/EPIC_2_STATUS.md)

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-10-30 | Initial release with full Bitget connector implementation |

---

**For questions or issues, please refer to the development team or create an issue in the project repository.**

