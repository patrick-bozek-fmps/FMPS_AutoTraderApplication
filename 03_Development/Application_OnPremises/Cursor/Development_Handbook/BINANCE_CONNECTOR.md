# Binance Connector Guide

## Overview

The Binance Connector provides integration with the Binance exchange for the FMPS AutoTrader application. It supports both testnet (for development/testing) and production environments.

**Version**: 1.0.0  
**Status**: ✅ IMPLEMENTED (Issue #8)  
**Last Updated**: October 30, 2025

---

## 📋 Features

### Implemented
- ✅ REST API for market data retrieval
- ✅ REST API for account information
- ✅ Order management (place, cancel, query orders)
- ✅ Position management
- ✅ WebSocket streaming for real-time data
- ✅ HMAC SHA256 authentication
- ✅ Comprehensive error handling
- ✅ Rate limiting (token bucket algorithm)
- ✅ Automatic timestamp synchronization
- ✅ User data stream with keep-alive

---

## 🚀 Quick Start

### 1. Get Binance Testnet API Keys

1. Visit https://testnet.binance.vision/
2. Create an account
3. Generate API Key and Secret Key
4. **IMPORTANT**: Store these keys securely, never commit them to Git!

### 2. Configure Environment Variables

Create a `.env` file in the `Application_OnPremises` directory:

```bash
BINANCE_API_KEY=your_testnet_api_key_here
BINANCE_API_SECRET=your_testnet_secret_key_here
BINANCE_BASE_URL=https://testnet.binance.vision
```

### 3. Use in Code

```kotlin
import com.fmps.autotrader.core.connectors.binance.BinanceConfig
import com.fmps.autotrader.core.connectors.binance.BinanceConnector
import com.fmps.autotrader.shared.enums.TimeFrame
import java.time.Instant

// Create configuration
val config = BinanceConfig.testnet(
    apiKey = System.getenv("BINANCE_API_KEY"),
    apiSecret = System.getenv("BINANCE_API_SECRET")
)

// Create and configure connector
val connector = BinanceConnector()
connector.configure(config)

// Connect
connector.connect()

// Fetch market data
val candles = connector.getCandles(
    symbol = "BTCUSDT",
    interval = TimeFrame.ONE_HOUR,
    startTime = Instant.now().minusSeconds(86400),  // Last 24 hours
    endTime = Instant.now(),
    limit = 100
)

// Get account balance
val balances = connector.getBalance()
println("Balances: $balances")

// Subscribe to real-time candlesticks
val subscriptionId = connector.subscribeCandlesticks(
    symbol = "BTCUSDT",
    interval = TimeFrame.ONE_MINUTE
) { candlestick ->
    println("New candle: ${candlestick.symbol} ${candlestick.closePrice}")
}

// Disconnect when done
connector.disconnect()
```

---

## 📚 API Reference

### BinanceConnector

Main class for interacting with Binance.

#### Market Data Methods

| Method | Description | Endpoint |
|--------|-------------|----------|
| `getCandles(symbol, interval, startTime, endTime, limit)` | Retrieve historical candlestick data | GET /api/v3/klines |
| `getTicker(symbol)` | Get 24-hour ticker information | GET /api/v3/ticker/24hr |
| `getOrderBook(symbol, limit)` | Get order book depth | GET /api/v3/depth |

#### Account Methods

| Method | Description | Endpoint | Auth |
|--------|-------------|----------|------|
| `getBalance()` | Get account balances | GET /api/v3/account | ✅ Signed |
| `getPosition(symbol)` | Get single position | GET /api/v3/account | ✅ Signed |
| `getPositions()` | Get all positions | GET /api/v3/account | ✅ Signed |

#### Order Methods

| Method | Description | Endpoint | Auth |
|--------|-------------|----------|------|
| `placeOrder(order)` | Place a new order | POST /api/v3/order | ✅ Signed |
| `cancelOrder(orderId, symbol)` | Cancel an order | DELETE /api/v3/order | ✅ Signed |
| `getOrder(orderId, symbol)` | Query order status | GET /api/v3/order | ✅ Signed |
| `getOrders(symbol?)` | Get open orders | GET /api/v3/openOrders | ✅ Signed |
| `closePosition(symbol)` | Close a position | POST /api/v3/order | ✅ Signed |

#### WebSocket Methods

| Method | Description | Stream |
|--------|-------------|--------|
| `subscribeCandlesticks(symbol, interval, callback)` | Real-time candlesticks | `{symbol}@kline_{interval}` |
| `subscribeTicker(symbol, callback)` | Real-time ticker updates | `{symbol}@ticker` |
| `subscribeOrderUpdates(callback)` | Real-time order updates | User data stream |
| `unsubscribe(subscriptionId)` | Unsubscribe from a stream | N/A |

---

## 🔐 Authentication

Binance uses HMAC SHA256 signature-based authentication:

1. Create a query string with all parameters
2. Add timestamp and recvWindow
3. Sign the query string with your API secret
4. Append the signature to the query string
5. Add X-MBX-APIKEY header with your API key

**Example Signature Generation:**
```
Query: symbol=BTCUSDT&side=BUY&type=MARKET&quantity=0.001&timestamp=1635789012345&recvWindow=5000
Signature: HMAC_SHA256(query, apiSecret) = "a3b2c1d4..."
Final URL: /api/v3/order?symbol=BTCUSDT&side=BUY&type=MARKET&quantity=0.001&timestamp=1635789012345&recvWindow=5000&signature=a3b2c1d4...
```

The connector handles this automatically via `BinanceAuthenticator`.

---

## ⚠️ Error Handling

The connector maps Binance error codes to framework exceptions:

| Binance Error | Framework Exception | Description |
|--------------|---------------------|-------------|
| -1021 | ConnectionException | Timestamp outside recvWindow |
| -1022 | AuthenticationException | Invalid signature |
| -2015 | AuthenticationException | Invalid API key |
| -1003, 429 | RateLimitException | Rate limit exceeded |
| -2010 | InsufficientFundsException | Insufficient balance |
| -2011, -2013 | OrderException | Order not found |
| -1111, -1121 | ExchangeException | Invalid symbol |
| 500, 502, 503, 504 | ConnectionException | Server unavailable |

**Example Error Handling:**
```kotlin
try {
    connector.placeOrder(order)
} catch (e: InsufficientFundsException) {
    logger.error { "Not enough balance: ${e.message}" }
} catch (e: RateLimitException) {
    logger.warn { "Rate limit hit, retry after ${e.retryAfterSeconds}s" }
} catch (e: AuthenticationException) {
    logger.error { "Auth failed: ${e.message}" }
}
```

---

## 🚦 Rate Limiting

Binance has strict rate limits:

- **General**: 1200 requests per minute
- **Order Endpoints**: 10 orders per second per account
- **Weight-based**: Different endpoints have different weights

The connector implements client-side rate limiting using a token bucket algorithm:

```kotlin
val config = BinanceConfig.testnet(apiKey, apiSecret)
config.rateLimitConfig.requestsPerSecond = 15.0  // Conservative setting
config.rateLimitConfig.burstCapacity = 30
```

**Best Practices:**
- Use conservative rate limits (15-20 req/s instead of 20)
- Enable burst capacity for occasional spikes
- Monitor rate limit metrics
- Handle RateLimitException gracefully

---

## 📡 WebSocket Streaming

### Candlestick Stream

```kotlin
val subId = connector.subscribeCandlesticks(
    symbol = "BTCUSDT",
    interval = TimeFrame.FIVE_MINUTES
) { candlestick ->
    println("${candlestick.closeTime}: ${candlestick.closePrice}")
}
```

### Ticker Stream

```kotlin
val subId = connector.subscribeTicker("ETHUSDT") { ticker ->
    println("Last: ${ticker.lastPrice}, Vol: ${ticker.volume}")
}
```

### Order Update Stream

```kotlin
val subId = connector.subscribeOrderUpdates { order ->
    println("Order ${order.id} status: ${order.status}")
}
```

### Unsubscribing

```kotlin
connector.unsubscribe(subId)
```

---

## 🧪 Testing

### Unit Tests

Unit tests use the `MockExchangeConnector` to avoid hitting the real API:

```kotlin
val mockConnector = MockExchangeConnector(Exchange.BINANCE)
mockConnector.configure(config)
```

### Integration Tests (Requires API Keys)

Integration tests require actual Binance testnet API keys:

1. Set environment variables:
   ```bash
   export BINANCE_API_KEY=your_testnet_key
   export BINANCE_API_SECRET=your_testnet_secret
   ```

2. Run integration tests:
   ```bash
   ./gradlew :core-service:integrationTest
   ```

**⚠️ Important**: Integration tests are NOT run in CI/CD by default to avoid exposing API keys.

---

## 🐛 Troubleshooting

### "Timestamp outside recvWindow" Error

**Cause**: System clock is out of sync with Binance server.

**Solution**:
1. Synchronize your system clock
2. The connector auto-adjusts timestamp offset on connect
3. Increase recvWindow if needed:
   ```kotlin
   config.recvWindow = 10000  // 10 seconds
   ```

### "Invalid signature" Error

**Cause**: API secret is incorrect or signature generation failed.

**Solution**:
1. Verify API secret is correct
2. Check for trailing spaces in .env file
3. Regenerate API keys if needed

### Rate Limit Errors

**Cause**: Too many requests sent to Binance.

**Solution**:
1. Reduce `requestsPerSecond` in config
2. Implement exponential backoff
3. Use WebSocket streams instead of polling

### Connection Timeout

**Cause**: Network issues or Binance testnet is down.

**Solution**:
1. Check https://testnet.binance.vision/ status
2. Increase timeout in config:
   ```kotlin
   config.timeout = 60000  // 60 seconds
   ```
3. Check firewall/proxy settings

---

## 📖 Related Documentation

- [Exchange Connector Guide](./EXCHANGE_CONNECTOR_GUIDE.md) - General connector framework
- [Logging Guide](./LOGGING_GUIDE.md) - Logging standards
- [Development Workflow](./DEVELOPMENT_WORKFLOW.md) - Development practices
- [Binance API Docs](https://binance-docs.github.io/apidocs/spot/en/) - Official API documentation
- [Binance Testnet](https://testnet.binance.vision/) - Get testnet API keys

---

## 📝 Notes

- **Production vs Testnet**: Never use production keys in development!
- **API Key Security**: Store keys in environment variables, never in code
- **Testnet Resets**: Binance testnet may reset periodically
- **Demo Mode**: For UI testing without real connections, use `MockExchangeConnector`
- **WebSocket Keep-Alive**: User data streams require keep-alive every 30 minutes (handled automatically)

---

**Created**: October 30, 2025  
**Issue**: #8 - Binance Connector Implementation  
**Author**: AI Assistant  
**Status**: ✅ Complete

