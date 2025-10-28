# Exchange Connector Framework - Developer Guide

**Version**: 1.0  
**Last Updated**: October 28, 2025  
**Status**: ✅ Complete

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Core Components](#core-components)
4. [Implementing a New Connector](#implementing-a-new-connector)
5. [Error Handling](#error-handling)
6. [Rate Limiting](#rate-limiting)
7. [WebSocket Integration](#websocket-integration)
8. [Testing](#testing)
9. [Configuration](#configuration)
10. [Best Practices](#best-practices)
11. [Troubleshooting](#troubleshooting)

---

## 📖 Overview

The Exchange Connector Framework provides a **unified interface** for interacting with multiple cryptocurrency exchanges (Binance, Bitget, Kraken, etc.). It abstracts exchange-specific implementation details, providing consistent behavior across all exchanges.

### **Key Features**

✅ **Unified Interface**: Single `IExchangeConnector` interface for all exchanges  
✅ **Factory Pattern**: Dynamic connector creation via `ConnectorFactory`  
✅ **Error Handling**: Comprehensive exception hierarchy with automatic retry  
✅ **Rate Limiting**: Token bucket algorithm with per-endpoint support  
✅ **Health Monitoring**: Automatic health checks with circuit breaker  
✅ **WebSocket Streaming**: Real-time market data and order updates  
✅ **Testability**: `MockExchangeConnector` for unit testing  
✅ **Configuration**: Type-safe, validated configuration models  

---

## 🏗️ Architecture

### **Component Diagram**

```
┌─────────────────────────────────────────────────────────┐
│                   AI Trader Application                  │
└───────────────────────┬─────────────────────────────────┘
                        │
                        ▼
         ┌──────────────────────────────┐
         │    IExchangeConnector        │  ◄── Interface
         │  (Standard Contract)         │
         └──────────────┬───────────────┘
                        │
            ┌───────────┴───────────┐
            │                       │
            ▼                       ▼
   ┌─────────────────┐     ┌─────────────────┐
   │ BinanceConnector│     │ BitgetConnector │
   └────────┬────────┘     └────────┬────────┘
            │                       │
            └───────────┬───────────┘
                        │
                        ▼
         ┌──────────────────────────────┐
         │  AbstractExchangeConnector   │  ◄── Base Class
         │  (Common Functionality)      │
         └──────────────┬───────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
  ┌──────────┐   ┌───────────┐   ┌────────────────┐
  │RateLimiter   │RetryPolicy│   │HealthMonitor   │
  └──────────┘   └───────────┘   └────────────────┘
```

### **Lifecycle**

```
1. Configure → 2. Connect → 3. Use API → 4. Disconnect
```

---

## 🔧 Core Components

### **1. IExchangeConnector Interface**

**Location**: `core-service/src/main/kotlin/com/fmps/autotrader/core/connectors/IExchangeConnector.kt`

Defines the standard contract for all exchange connectors:

```kotlin
interface IExchangeConnector {
    // Connection Management
    fun getExchange(): Exchange
    fun configure(config: ExchangeConfig)
    suspend fun connect()
    suspend fun disconnect()
    fun isConnected(): Boolean
    
    // Market Data
    suspend fun getCandles(symbol: String, interval: TimeFrame, ...): List<Candlestick>
    suspend fun getTicker(symbol: String): Ticker
    suspend fun getOrderBook(symbol: String, limit: Int): OrderBook
    
    // Account Information
    suspend fun getBalance(): Map<String, BigDecimal>
    suspend fun getPositions(): List<Position>
    
    // Order Management
    suspend fun placeOrder(order: Order): Order
    suspend fun cancelOrder(orderId: String, symbol: String): Order
    suspend fun getOrders(symbol: String?): List<Order>
    
    // WebSocket Streaming
    suspend fun subscribeCandlesticks(symbol: String, interval: TimeFrame, callback: ...): String
    suspend fun subscribeTicker(symbol: String, callback: ...): String
    suspend fun unsubscribe(subscriptionId: String)
}
```

### **2. ConnectorFactory**

**Location**: `core-service/src/main/kotlin/com/fmps/autotrader/core/connectors/ConnectorFactory.kt`

Singleton factory for creating connector instances:

```kotlin
// Register a connector
ConnectorFactory.registerConnector(Exchange.BINANCE) { config -> BinanceConnector(config) }

// Get a connector
val connector = ConnectorFactory.getConnector(Exchange.BINANCE, config)
```

### **3. AbstractExchangeConnector**

**Location**: `core-service/src/main/kotlin/com/fmps/autotrader/core/connectors/AbstractExchangeConnector.kt`

Base class providing common functionality:
- HTTP client setup (Ktor CIO)
- Rate limiter integration
- Retry policy integration
- Connection state management
- Metrics tracking
- Lifecycle hooks

### **4. Exception Hierarchy**

**Location**: `core-service/src/main/kotlin/com/fmps/autotrader/core/connectors/exceptions/`

```
ExchangeException (base)
├── ConnectionException (network issues)
├── AuthenticationException (invalid API keys)
├── RateLimitException (rate limit exceeded)
├── OrderException (order-related errors)
├── InsufficientFundsException (not enough balance)
├── UnsupportedExchangeException (exchange not supported)
└── ConfigurationException (invalid configuration)
```

---

## 🛠️ Implementing a New Connector

### **Step 1: Create Connector Class**

Create a new class extending `AbstractExchangeConnector`:

```kotlin
class MyExchangeConnector(
    config: ExchangeConfig
) : AbstractExchangeConnector(Exchange.MY_EXCHANGE) {
    
    init {
        configure(config)
    }
    
    // Implement abstract methods
}
```

### **Step 2: Implement Required Methods**

```kotlin
override suspend fun testConnectivity() {
    // Make a lightweight request (e.g., ping or server time)
    val response = httpClient.get("$baseUrl/api/v1/ping")
    if (response.status != HttpStatusCode.OK) {
        throw ConnectionException("Connectivity test failed")
    }
}

override suspend fun getCandles(
    symbol: String,
    interval: TimeFrame,
    startTime: Instant?,
    endTime: Instant?,
    limit: Int
): List<Candlestick> {
    ensureConnected()
    rateLimiter.acquire() // Apply rate limiting
    
    return retryPolicy.execute("getCandles") { // Automatic retry
        val response = httpClient.get("$baseUrl/api/v1/candles") {
            parameter("symbol", symbol)
            parameter("interval", interval.value)
            parameter("limit", limit)
        }
        
        parseCandles(response.bodyAsText())
    }
}
```

### **Step 3: Register Connector**

```kotlin
// In application initialization
ConnectorFactory.registerConnector(Exchange.MY_EXCHANGE) { config ->
    MyExchangeConnector(config)
}
```

### **Step 4: Configure & Use**

```kotlin
val config = ExchangeConfig(
    exchange = Exchange.MY_EXCHANGE,
    apiKey = "your-api-key",
    apiSecret = "your-api-secret",
    testnet = true
)

val connector = ConnectorFactory.getConnector(Exchange.MY_EXCHANGE, config)
connector.connect()

val ticker = connector.getTicker("BTCUSDT")
println("BTC Price: ${ticker.lastPrice}")

connector.disconnect()
```

---

## ⚠️ Error Handling

### **Exception Hierarchy Usage**

```kotlin
try {
    connector.placeOrder(order)
} catch (e: InsufficientFundsException) {
    logger.error { "Not enough funds: ${e.asset}" }
} catch (e: RateLimitException) {
    logger.warn { "Rate limited. Retry after ${e.retryAfterSeconds}s" }
} catch (e: ConnectionException) {
    logger.error { "Connection lost. Retrying..." }
} catch (e: ExchangeException) {
    logger.error { "Exchange error: ${e.message}" }
}
```

### **Automatic Retry**

The `RetryPolicy` automatically retries failed requests:

```kotlin
val retryPolicy = RetryPolicy(
    maxRetries = 3,
    initialDelayMillis = 1000,
    maxDelayMillis = 30000,
    factor = 2.0,
    jitterFactor = 0.1
)

retryPolicy.execute("operation") {
    // Your code here - automatically retried on failure
}
```

**Retry Schedule**:
- Attempt 1: Immediate
- Attempt 2: 1s delay
- Attempt 3: 2s delay
- Attempt 4: 4s delay
- Attempt 5: Fail

---

## ⏱️ Rate Limiting

### **Token Bucket Algorithm**

The `RateLimiter` uses the token bucket algorithm:

```kotlin
val rateLimiter = RateLimiter(
    requestsPerSecond = 10.0,
    burstCapacity = 20
)

// Acquire tokens before request
rateLimiter.acquire(tokensToAcquire = 1.0, weight = 1.0)
```

### **Per-Endpoint Limits**

```kotlin
// Register endpoint-specific limiter
rateLimiter.registerEndpointLimiter("/api/v3/order", RateLimiter(5.0, 10))

// Use with endpoint
rateLimiter.acquire(endpoint = "/api/v3/order")
```

### **Configuration**

```kotlin
val config = ExchangeConfig(
    exchange = Exchange.BINANCE,
    apiKey = "...",
    apiSecret = "...",
    rateLimitConfig = RateLimitConfig(
        requestsPerSecond = 20.0,
        burstCapacity = 40,
        perEndpointLimit = true
    )
)
```

---

## 🌐 WebSocket Integration

### **Subscribing to Streams**

```kotlin
// Subscribe to candlesticks
val subscriptionId = connector.subscribeCandlesticks(
    symbol = "BTCUSDT",
    interval = TimeFrame.ONE_MINUTE
) { candle ->
    println("New candle: ${candle.close}")
}

// Subscribe to ticker
connector.subscribeTicker("BTCUSDT") { ticker ->
    println("Price: ${ticker.lastPrice}")
}

// Unsubscribe
connector.unsubscribe(subscriptionId)
```

### **Implementing WebSocket Support**

Extend `WebSocketManager`:

```kotlin
class MyWebSocketManager(
    httpClient: HttpClient,
    baseUrl: String
) : WebSocketManager(httpClient, baseUrl) {
    
    override suspend fun parseMessage(message: String): WebSocketMessage? {
        // Parse exchange-specific JSON
        val json = Json.parseToJsonElement(message).jsonObject
        val channel = json["channel"]?.jsonPrimitive?.content ?: return null
        return WebSocketMessage(channel, json)
    }
    
    override suspend fun buildSubscriptionMessage(
        channel: String,
        params: Map<String, Any>
    ): String {
        // Build exchange-specific subscription message
        return Json.encodeToString(mapOf(
            "method" to "SUBSCRIBE",
            "params" to listOf(channel),
            "id" to System.currentTimeMillis()
        ))
    }
}
```

---

## 🧪 Testing

### **Using MockExchangeConnector**

```kotlin
@Test
fun `test order placement`() = runBlocking {
    val mockConnector = MockExchangeConnector(
        simulatedLatencyMs = 100,
        failureRate = 0.0
    )
    
    mockConnector.configure(testConfig)
    mockConnector.connect()
    
    // Set initial balance
    mockConnector.setBalance("USDT", BigDecimal("10000.00"))
    
    // Place order
    val order = Order(
        symbol = "BTCUSDT",
        action = TradeAction.LONG,
        type = OrderType.MARKET,
        quantity = BigDecimal("0.01")
    )
    
    val placedOrder = mockConnector.placeOrder(order)
    
    assertEquals(TradeStatus.FILLED, placedOrder.status)
    assertNotNull(placedOrder.averagePrice)
}
```

### **Testing Error Scenarios**

```kotlin
// Test insufficient funds
mockConnector.setBalance("USDT", BigDecimal.ZERO)
assertThrows<InsufficientFundsException> {
    runBlocking { mockConnector.placeOrder(order) }
}

// Test with failure injection
val flakyConnector = MockExchangeConnector(failureRate = 0.5)
// 50% of operations will fail randomly
```

---

## ⚙️ Configuration

### **Comprehensive Configuration**

```kotlin
val config = ExchangeConfig(
    exchange = Exchange.BINANCE,
    apiKey = System.getenv("BINANCE_API_KEY"),
    apiSecret = System.getenv("BINANCE_API_SECRET"),
    testnet = true,
    
    // Rate Limiting
    rateLimitConfig = RateLimitConfig(
        requestsPerSecond = 20.0,
        burstCapacity = 40
    ),
    
    // Retry Policy
    retryPolicyConfig = RetryPolicyConfig(
        maxRetries = 5,
        initialDelayMillis = 2000,
        maxDelayMillis = 60000
    ),
    
    // WebSocket
    webSocketConfig = WebSocketConfig(
        enabled = true,
        pingIntervalMs = 30000,
        autoReconnect = true,
        maxReconnectAttempts = 10
    ),
    
    // Health Monitoring
    healthCheckConfig = HealthCheckConfig(
        enabled = true,
        checkIntervalMs = 30000,
        maxConsecutiveFailures = 5,
        circuitBreakerResetMs = 60000
    ),
    
    // Timeouts
    timeoutMs = 30000,
    connectTimeoutMs = 10000
)
```

---

## ✅ Best Practices

### **1. Always Check Connection**

```kotlin
if (!connector.isConnected()) {
    connector.connect()
}
```

### **2. Handle Errors Gracefully**

```kotlin
try {
    connector.placeOrder(order)
} catch (e: RateLimitException) {
    delay(e.retryAfterSeconds * 1000L)
    // Retry after rate limit reset
}
```

### **3. Clean Up Resources**

```kotlin
try {
    // Use connector
} finally {
    connector.disconnect()
}
```

### **4. Use Health Monitoring**

```kotlin
val monitor = ConnectionHealthMonitor(connector)
monitor.addStatusChangeListener { status, message ->
    when (status) {
        HealthStatus.UNHEALTHY -> logger.warn { "Connection unhealthy" }
        HealthStatus.CIRCUIT_OPEN -> logger.error { "Circuit breaker open!" }
        HealthStatus.HEALTHY -> logger.info { "Connection recovered" }
    }
}
monitor.start()
```

### **5. Log Operations**

```kotlin
logger.info { "Placing order: $order" }
val result = connector.placeOrder(order)
logger.info { "Order placed: ${result.id}" }
```

---

## 🐛 Troubleshooting

### **Connection Issues**

**Problem**: `ConnectionException: Failed to connect`  
**Solution**: 
- Check network connectivity
- Verify API base URL is correct
- Ensure firewall allows HTTPS connections

### **Authentication Failures**

**Problem**: `AuthenticationException: Invalid API key`  
**Solution**:
- Verify API key and secret are correct
- Check if keys have required permissions
- Ensure testnet keys for testnet, production keys for production

### **Rate Limit Exceeded**

**Problem**: `RateLimitException: Rate limit exceeded`  
**Solution**:
- Reduce `requestsPerSecond` in `RateLimitConfig`
- Increase `burstCapacity` for burst traffic
- Implement request queuing

### **WebSocket Disconnections**

**Problem**: WebSocket keeps disconnecting  
**Solution**:
- Check `pingIntervalMs` is appropriate (default: 30s)
- Verify network stability
- Enable `autoReconnect` in `WebSocketConfig`

### **Order Rejections**

**Problem**: `OrderException: Order rejected`  
**Solution**:
- Check account balance (`getBalance()`)
- Verify order parameters (quantity, price)
- Check exchange-specific order requirements

---

## 📚 Additional Resources

- **API Documentation**: `core-service/API_DOCUMENTATION.md`
- **Testing Guide**: `TESTING_GUIDE.md`
- **Configuration Guide**: `CONFIG_GUIDE.md`
- **Logging Guide**: `LOGGING_GUIDE.md`

---

## 📝 Summary

The Exchange Connector Framework provides a robust, extensible foundation for integrating multiple cryptocurrency exchanges into the AI Trader application. By following this guide and adhering to best practices, developers can quickly implement new exchange connectors with confidence.

**Key Takeaways**:
- ✅ Use `IExchangeConnector` interface for consistency
- ✅ Extend `AbstractExchangeConnector` for common functionality
- ✅ Leverage automatic retry and rate limiting
- ✅ Test with `MockExchangeConnector`
- ✅ Monitor health with `ConnectionHealthMonitor`
- ✅ Handle errors gracefully

**Next Steps**:
- Implement Binance connector (Issue #8)
- Implement Bitget connector (Issue #9)
- Add more exchanges as needed

---

**Document Version**: 1.0  
**Last Updated**: October 28, 2025  
**Status**: ✅ Complete

