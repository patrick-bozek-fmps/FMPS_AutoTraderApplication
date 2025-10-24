# Issue #4: Exchange Integration - Connector Interface & Binance Implementation

**Status**: 📋 **PLANNED** (Not Started)  
**Assigned**: AI Assistant  
**Created**: October 24, 2025  
**Started**: TBD  
**Completed**: TBD  
**Epic**: Foundation & Infrastructure (Phase 1)  
**Priority**: P0 (Critical)  
**Dependencies**: Issue #1 (Gradle) ✅, Issue #2 (Database) ✅, Issue #3 (API) ✅

---

## 📋 **Objective**

Design and implement a standardized exchange connector interface with the first concrete implementation for Binance testnet. This will establish the foundation for integrating multiple cryptocurrency exchanges and enable market data retrieval and order execution for the AI trading system.

---

## 🎯 **Goals**

1. **Design** a flexible `IExchangeConnector` interface supporting multiple exchanges
2. **Implement** Binance testnet connector for market data (REST API)
3. **Implement** Binance testnet connector for trading operations
4. **Add** WebSocket support for real-time market data
5. **Integrate** with existing database and API layers
6. **Test** thoroughly with Binance testnet
7. **Document** usage and provide examples

---

## 📝 **Tasks Breakdown**

### **Phase 1: Exchange Connector Interface Design** ⏳ PENDING
- [ ] Define `IExchangeConnector` interface:
  - [ ] Connection management (connect, disconnect, isConnected, health)
  - [ ] Market data methods (getCandlesticks, getTicker, getOrderBook, getServerTime)
  - [ ] Order methods (placeOrder, cancelOrder, getOrder, getOpenOrders)
  - [ ] Position methods (getPositions, closePosition)
  - [ ] Account methods (getBalance, getAccountInfo)
  - [ ] Leverage/margin methods (setLeverage, setMarginType)
- [ ] Define result types:
  - [ ] `ExchangeResult<T>` (success/failure wrapper)
  - [ ] `ExchangeError` (error codes and messages)
  - [ ] `ConnectionStatus` enum
- [ ] Create exchange-specific models:
  - [ ] `Candlestick` (OHLCV data)
  - [ ] `Ticker` (current price info)
  - [ ] `OrderBook` (bids/asks)
  - [ ] `Order` (order details)
  - [ ] `Position` (position details)
  - [ ] `Balance` (account balance)
- [ ] Design error handling strategy
- [ ] Create `ExchangeFactory` for connector creation
- [ ] Document interface contract and usage

### **Phase 2: Binance Connector - Market Data (REST)** ⏳ PENDING
- [ ] Create `BinanceConnector` class implementing `IExchangeConnector`
- [ ] Implement authentication (HMAC SHA256 signing)
- [ ] Configure Binance testnet endpoints
- [ ] Implement REST API methods:
  - [ ] `getServerTime()` - Sync time with exchange
  - [ ] `getCandlesticks(symbol, interval, limit)` - Get OHLCV data
  - [ ] `getTicker(symbol)` - Get current price
  - [ ] `getOrderBook(symbol, depth)` - Get order book
  - [ ] `get24hrStats(symbol)` - Get 24h statistics
- [ ] Add rate limiting (1200 requests/minute for Binance)
- [ ] Implement retry logic with exponential backoff
- [ ] Add request signing and timestamp synchronization
- [ ] Handle API errors and map to `ExchangeError`
- [ ] Add connection health monitoring
- [ ] Write unit tests with mocked HTTP responses
- [ ] Write integration tests with Binance testnet

### **Phase 3: Binance Connector - Trading Operations** ⏳ PENDING
- [ ] Implement order placement:
  - [ ] `placeMarketOrder(symbol, side, quantity)`
  - [ ] `placeLimitOrder(symbol, side, quantity, price)`
  - [ ] `placeStopLossOrder(symbol, side, quantity, stopPrice)`
  - [ ] `placeTakeProfitOrder(symbol, side, quantity, price)`
- [ ] Implement order management:
  - [ ] `cancelOrder(symbol, orderId)`
  - [ ] `getOrder(symbol, orderId)` - Get order status
  - [ ] `getOpenOrders(symbol)` - Get all open orders
  - [ ] `cancelAllOrders(symbol)` - Cancel all open orders
- [ ] Implement position management:
  - [ ] `getPositions()` - Get all positions
  - [ ] `getPosition(symbol)` - Get specific position
  - [ ] `closePosition(symbol)` - Close position with market order
  - [ ] `setLeverage(symbol, leverage)` - Set leverage (1-125x)
  - [ ] `setMarginType(symbol, marginType)` - Set ISOLATED or CROSSED
- [ ] Implement account methods:
  - [ ] `getBalance()` - Get account balance
  - [ ] `getAccountInfo()` - Get full account info
- [ ] Add testnet API key validation
- [ ] Implement order confirmation and tracking
- [ ] Add safety checks (balance, limits, leverage)
- [ ] Write comprehensive tests for all trading operations

### **Phase 4: Binance WebSocket Support** ⏳ PENDING
- [ ] Create `BinanceWebSocketClient` class
- [ ] Implement WebSocket connection management
- [ ] Add automatic reconnection logic
- [ ] Subscribe to data streams:
  - [ ] Kline/candlestick stream (real-time candles)
  - [ ] Trade stream (individual trades)
  - [ ] Ticker stream (24hr ticker)
  - [ ] Order book stream (depth updates)
  - [ ] User data stream (orders, positions, balance updates)
- [ ] Parse WebSocket messages to domain models
- [ ] Emit events to subscribers (observer pattern)
- [ ] Handle connection errors and reconnects
- [ ] Add heartbeat/ping-pong mechanism
- [ ] Write tests for WebSocket functionality

### **Phase 5: Database Integration** ⏳ PENDING
- [ ] Create `exchange_connections` table:
  - [ ] exchange_name (BINANCE, COINBASE, etc.)
  - [ ] api_key (encrypted)
  - [ ] api_secret (encrypted)
  - [ ] testnet_mode (boolean)
  - [ ] is_active (boolean)
  - [ ] last_connected_at
  - [ ] created_at, updated_at
- [ ] Create `ExchangeConnectionRepository`
- [ ] Implement CRUD operations for exchange connections
- [ ] Add encryption for API keys (using Java Cipher)
- [ ] Create migration script (V5__Create_exchange_connections.sql)
- [ ] Write repository tests

### **Phase 6: API Endpoints for Exchange Management** ⏳ PENDING
- [ ] Create `ExchangeRoutes.kt`:
  - [ ] `GET /api/v1/exchanges` - List supported exchanges
  - [ ] `GET /api/v1/exchanges/:name/status` - Get connection status
  - [ ] `POST /api/v1/exchanges/:name/connect` - Test connection
  - [ ] `POST /api/v1/exchanges/:name/config` - Save API keys
  - [ ] `GET /api/v1/exchanges/:name/balance` - Get balance
  - [ ] `GET /api/v1/exchanges/:name/positions` - Get positions
  - [ ] `GET /api/v1/exchanges/:name/candles` - Get historical data
- [ ] Add request validation
- [ ] Add error handling
- [ ] Write API endpoint tests

### **Phase 7: Service Layer Integration** ⏳ PENDING
- [ ] Create `ExchangeService` class:
  - [ ] Manage connector instances
  - [ ] Route requests to appropriate connector
  - [ ] Cache connections
  - [ ] Handle failover and retry
- [ ] Create `MarketDataService`:
  - [ ] Fetch and cache candlestick data
  - [ ] Subscribe to real-time updates
  - [ ] Aggregate data from multiple timeframes
- [ ] Integrate with `AITraderRepository` and `TradeRepository`
- [ ] Add service-level error handling
- [ ] Write service layer tests

### **Phase 8: Configuration Management** ⏳ PENDING
- [ ] Update `application.conf` with exchange settings:
  ```hocon
  exchanges {
    binance {
      testnet {
        enabled = true
        rest-url = "https://testnet.binancefuture.com"
        ws-url = "wss://stream.binancefuture.com"
      }
      rate-limits {
        requests-per-minute = 1200
        orders-per-second = 100
      }
    }
  }
  ```
- [ ] Add exchange-specific timeouts
- [ ] Configure retry policies
- [ ] Add feature flags for exchanges

### **Phase 9: Error Handling & Resilience** ⏳ PENDING
- [ ] Define exchange-specific error types:
  - [ ] `NetworkError` - Connection issues
  - [ ] `RateLimitError` - Too many requests
  - [ ] `AuthenticationError` - Invalid API keys
  - [ ] `InvalidRequestError` - Bad request parameters
  - [ ] `InsufficientBalanceError` - Not enough funds
  - [ ] `OrderRejectedError` - Order rejected by exchange
- [ ] Implement circuit breaker pattern
- [ ] Add comprehensive logging
- [ ] Create error recovery mechanisms
- [ ] Write error scenario tests

### **Phase 10: Testing & Verification** ⏳ PENDING
- [ ] Unit tests (25+ tests):
  - [ ] Interface implementation tests
  - [ ] Authentication tests
  - [ ] Request signing tests
  - [ ] Error handling tests
  - [ ] Rate limiting tests
- [ ] Integration tests (15+ tests):
  - [ ] Binance testnet connectivity
  - [ ] Market data retrieval
  - [ ] Order placement and cancellation
  - [ ] Position management
  - [ ] WebSocket streaming
- [ ] Manual testing checklist:
  - [ ] Connect to Binance testnet
  - [ ] Fetch candlestick data
  - [ ] Place test orders
  - [ ] Monitor WebSocket streams
  - [ ] Verify error handling
- [ ] Performance testing:
  - [ ] Rate limit compliance
  - [ ] Response time < 500ms
  - [ ] WebSocket latency < 100ms

### **Phase 11: Documentation** ⏳ PENDING
- [ ] Create `EXCHANGE_INTEGRATION_GUIDE.md`:
  - [ ] How to add a new exchange
  - [ ] Connector interface explanation
  - [ ] Binance connector usage
  - [ ] API key setup guide (testnet)
  - [ ] WebSocket subscription guide
  - [ ] Error handling guide
- [ ] Add KDoc to all public interfaces
- [ ] Create example code snippets
- [ ] Update API documentation

### **Phase 12: Build & Commit** ⏳ PENDING
- [ ] Run all tests: `./gradlew test`
- [ ] Build project: `./gradlew build`
- [ ] Fix any compilation errors
- [ ] Fix any test failures
- [ ] Commit changes with descriptive message
- [ ] Push to GitHub
- [ ] Verify CI pipeline passes
- [ ] Update Issue #4 document
- [ ] Update Development_Plan_v2.md

---

## 📦 **Deliverables**

### **Core Connector Files**
1. `core-service/src/main/kotlin/.../connectors/IExchangeConnector.kt` - Interface
2. `core-service/src/main/kotlin/.../connectors/ExchangeFactory.kt` - Factory
3. `core-service/src/main/kotlin/.../connectors/binance/BinanceConnector.kt` - Implementation
4. `core-service/src/main/kotlin/.../connectors/binance/BinanceWebSocketClient.kt` - WebSocket
5. `core-service/src/main/kotlin/.../connectors/binance/BinanceAuth.kt` - Authentication

### **Models**
6. `shared/src/main/kotlin/.../models/Candlestick.kt`
7. `shared/src/main/kotlin/.../models/Ticker.kt`
8. `shared/src/main/kotlin/.../models/OrderBook.kt`
9. `shared/src/main/kotlin/.../models/Order.kt`
10. `shared/src/main/kotlin/.../models/Position.kt`
11. `shared/src/main/kotlin/.../models/Balance.kt`
12. `shared/src/main/kotlin/.../models/ExchangeResult.kt`
13. `shared/src/main/kotlin/.../models/ExchangeError.kt`

### **Database**
14. `core-service/src/main/resources/db/migration/V5__Create_exchange_connections.sql`
15. `core-service/src/main/kotlin/.../database/repositories/ExchangeConnectionRepository.kt`

### **API Layer**
16. `core-service/src/main/kotlin/.../api/routes/ExchangeRoutes.kt`
17. `core-service/src/main/kotlin/.../api/dto/ExchangeDTO.kt`

### **Service Layer**
18. `core-service/src/main/kotlin/.../services/ExchangeService.kt`
19. `core-service/src/main/kotlin/.../services/MarketDataService.kt`

### **Tests (40+ tests)**
20. `core-service/src/test/kotlin/.../connectors/IExchangeConnectorTest.kt`
21. `core-service/src/test/kotlin/.../connectors/binance/BinanceConnectorTest.kt`
22. `core-service/src/test/kotlin/.../connectors/binance/BinanceAuthTest.kt`
23. `core-service/src/test/kotlin/.../connectors/binance/BinanceWebSocketClientTest.kt`
24. `core-service/src/integrationTest/kotlin/.../connectors/binance/BinanceIntegrationTest.kt`

### **Documentation**
25. `EXCHANGE_INTEGRATION_GUIDE.md`
26. Updated `API_DOCUMENTATION.md`
27. Updated `Issue_04_Exchange_Integration.md`

### **Configuration**
28. Updated `application.conf` with exchange settings

---

## 🎯 **Success Criteria**

| Criterion | Target | Verification Method |
|-----------|--------|---------------------|
| Connector interface complete | Full interface defined | Code review |
| Binance REST API working | All methods implemented | Integration tests |
| Market data retrieval | Candles, ticker, orderbook | Testnet verification |
| Order placement working | Market, limit, stop orders | Testnet orders |
| WebSocket streaming | Real-time data received | Manual verification |
| Rate limiting enforced | Stay under 1200 req/min | Monitor logs |
| Authentication working | Valid signatures | Testnet API calls |
| Error handling comprehensive | All error types handled | Error tests |
| Database integration | Exchange config persisted | Repository tests |
| API endpoints functional | All exchange routes work | API tests |
| All tests pass | 40+ tests passing | `./gradlew test` |
| Build succeeds | Clean build | `./gradlew build` |
| CI pipeline passes | GitHub Actions green | Check Actions |
| Documentation complete | Usage guide written | Doc review |
| Response time acceptable | REST < 500ms, WS < 100ms | Performance tests |

---

## 📊 **Estimated Code Statistics**

| Metric | Estimated Value |
|--------|----------------|
| **New Files** | ~28 files |
| **Lines of Code** | ~4,000+ lines |
| **Interface/Classes** | 15+ classes |
| **API Endpoints** | 7 endpoints |
| **Database Tables** | 1 table |
| **Test Cases** | 40+ tests |
| **Test Coverage** | 85%+ target |
| **Documentation** | 1,000+ lines |

---

## 🔧 **Technologies & Dependencies**

| Technology | Version | Purpose |
|------------|---------|---------|
| Ktor Client | 2.3.7 | HTTP client for REST API |
| Ktor Client WebSocket | 2.3.7 | WebSocket client |
| Kotlinx Serialization | 1.6.0 | JSON parsing |
| Apache Commons Codec | 1.16.0 | HMAC SHA256 signing |
| Fuel | 2.3.1 | Alternative HTTP client |
| Java Cipher | Built-in | API key encryption |

**Add to `core-service/build.gradle.kts`**:
```kotlin
dependencies {
    // HTTP Client
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    
    // Encryption
    implementation("commons-codec:commons-codec:1.16.0")
    
    // Testing
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
}
```

---

## 🏗️ **Architecture**

```
┌─────────────────────────────────────────────────────────┐
│                   Desktop UI / API Clients               │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP/WebSocket
                     ▼
┌─────────────────────────────────────────────────────────┐
│               REST API Layer (Issue #3)                  │
│  ┌────────────────────────────────────────┐             │
│  │      ExchangeRoutes                     │             │
│  │  /api/v1/exchanges/*                    │             │
│  └────────────────────┬───────────────────┘             │
└─────────────────────────┼───────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│              Service Layer (This Issue)                  │
│  ┌──────────────────┐  ┌──────────────────┐            │
│  │ ExchangeService  │  │ MarketDataService│            │
│  └────────┬─────────┘  └──────────────────┘            │
│           │                                              │
│           ▼                                              │
│  ┌──────────────────────────────────────┐               │
│  │     ExchangeFactory                  │               │
│  └────────┬─────────────────────────────┘               │
└───────────┼──────────────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────────────────────┐
│           Connector Layer (This Issue)                   │
│  ┌──────────────────────────────────────┐               │
│  │      IExchangeConnector              │               │
│  │         (Interface)                  │               │
│  └────────┬─────────────────────────────┘               │
│           │                                              │
│           ▼                                              │
│  ┌──────────────────┐  ┌──────────────────┐            │
│  │ BinanceConnector │  │ Future: Coinbase │            │
│  │   (REST + WS)    │  │ Future: Bitget   │            │
│  └────────┬─────────┘  └──────────────────┘            │
└───────────┼──────────────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────────────────────┐
│              Binance Testnet API                         │
│  REST: https://testnet.binancefuture.com                │
│  WebSocket: wss://stream.binancefuture.com              │
└─────────────────────────────────────────────────────────┘
```

---

## 📋 **Binance Testnet Setup**

### **Prerequisites**
1. Create Binance Testnet Account:
   - Go to: https://testnet.binancefuture.com/
   - Register with email
   - Generate API keys (with trading permissions)

2. Get Test Funds:
   - Use testnet faucet to get test USDT
   - Available at: https://testnet.binancefuture.com/en/futures/BTCUSDT

### **API Key Configuration**
Store in `application.conf`:
```hocon
exchanges {
  binance {
    testnet {
      api-key = "your-testnet-api-key"
      api-secret = "your-testnet-api-secret"
      # NOTE: Never commit real API keys!
      # Use environment variables in production
    }
  }
}
```

### **Environment Variables** (Production/CI)
```bash
export BINANCE_TESTNET_API_KEY="your-key"
export BINANCE_TESTNET_API_SECRET="your-secret"
```

---

## 📝 **API Examples**

### **Get Candlestick Data**
```kotlin
val connector = ExchangeFactory.create("BINANCE", testnetMode = true)
connector.connect()

val candles = connector.getCandlesticks(
    symbol = "BTCUSDT",
    interval = "1h",
    limit = 100
)

when (candles) {
    is ExchangeResult.Success -> {
        candles.data.forEach { candle ->
            println("${candle.openTime}: O=${candle.open} H=${candle.high} L=${candle.low} C=${candle.close}")
        }
    }
    is ExchangeResult.Failure -> {
        println("Error: ${candles.error.message}")
    }
}
```

### **Place Market Order**
```kotlin
val order = connector.placeMarketOrder(
    symbol = "BTCUSDT",
    side = "BUY",
    quantity = 0.001
)

when (order) {
    is ExchangeResult.Success -> {
        println("Order placed: ${order.data.orderId}")
    }
    is ExchangeResult.Failure -> {
        println("Order failed: ${order.error.message}")
    }
}
```

### **Subscribe to WebSocket**
```kotlin
val wsClient = BinanceWebSocketClient(testnetMode = true)
wsClient.subscribeToKline("BTCUSDT", "1m") { candle ->
    println("New candle: ${candle.close}")
}
wsClient.connect()
```

---

## 🔒 **Security Considerations**

1. **API Key Storage**:
   - ✅ Encrypt API keys in database
   - ✅ Use environment variables for production
   - ❌ Never commit API keys to Git
   - ✅ Add `.env` to `.gitignore`

2. **Request Signing**:
   - ✅ All authenticated requests must be signed
   - ✅ Use HMAC SHA256 with API secret
   - ✅ Include timestamp to prevent replay attacks
   - ✅ Sync time with exchange server

3. **Rate Limiting**:
   - ✅ Respect exchange rate limits
   - ✅ Implement exponential backoff
   - ✅ Track requests per minute
   - ✅ Use circuit breaker for failures

4. **Error Handling**:
   - ✅ Validate all inputs before sending
   - ✅ Handle network timeouts gracefully
   - ✅ Log errors without exposing secrets
   - ✅ Implement retry with jitter

---

## ⚠️ **Known Challenges**

1. **Time Synchronization**:
   - Binance requires timestamp within 5000ms of server time
   - Solution: Sync with `getServerTime()` on connect
   - Cache time offset and adjust locally

2. **Rate Limiting**:
   - Easy to hit limits during testing
   - Solution: Implement request queue with rate limiter
   - Use WebSocket for real-time data instead of polling

3. **WebSocket Reconnection**:
   - Connections can drop unexpectedly
   - Solution: Automatic reconnection with exponential backoff
   - Resubscribe to all streams after reconnect

4. **Test Order Simulation**:
   - Testnet may have limited liquidity
   - Solution: Use small quantities for testing
   - Verify order behavior before production use

---

## 📈 **Definition of Done**

- [ ] All 12 phases completed
- [ ] IExchangeConnector interface fully defined
- [ ] BinanceConnector implemented (REST + WebSocket)
- [ ] All market data methods working
- [ ] All trading methods working (orders, positions)
- [ ] WebSocket streaming functional
- [ ] Database integration complete
- [ ] API endpoints implemented and tested
- [ ] 40+ tests written and passing
- [ ] Integration tests with Binance testnet passing
- [ ] Error handling comprehensive
- [ ] Rate limiting implemented
- [ ] Documentation complete (EXCHANGE_INTEGRATION_GUIDE.md)
- [ ] Code review completed
- [ ] All tests pass: `./gradlew test`
- [ ] Build succeeds: `./gradlew build`
- [ ] CI pipeline passes (GitHub Actions)
- [ ] Issue #4 document updated with completion details
- [ ] Development_Plan_v2.md updated

---

## 🔄 **Dependencies**

### **Depends On** (Must be complete first)
- ✅ Issue #1: Gradle multi-module setup
- ✅ Issue #2: Database layer with repositories
- ✅ Issue #3: REST API server with Ktor

### **Blocks** (Cannot start until this is done)
- Issue #5: Technical indicators (needs market data)
- Issue #6: AI Trader implementation (needs exchange integration)
- Issue #7: Pattern matching (needs real trading data)

---

## 📚 **Resources**

### **Binance API Documentation**
- REST API: https://binance-docs.github.io/apidocs/futures/en/
- WebSocket: https://binance-docs.github.io/apidocs/futures/en/#websocket-market-streams
- Testnet: https://testnet.binancefuture.com/

### **Binance Testnet**
- Registration: https://testnet.binancefuture.com/en/register
- Faucet: Get free test USDT

### **Code Examples**
- Binance signature: https://binance-docs.github.io/apidocs/spot/en/#signed-trade-user_data-and-margin-endpoint-security
- Kotlin HTTP clients: https://ktor.io/docs/client.html

---

## 🎓 **Learning Objectives**

By completing this issue, you will learn:
1. How to design flexible connector interfaces
2. Cryptocurrency exchange API integration
3. HMAC SHA256 authentication and request signing
4. Rate limiting and circuit breaker patterns
5. WebSocket real-time data streaming
6. Error handling in distributed systems
7. API key encryption and security best practices

---

## ✅ **Completion Checklist**

Before marking this issue as complete, verify:

- [ ] Interface design reviewed and approved
- [ ] Binance connector fully functional
- [ ] Can fetch market data from testnet
- [ ] Can place and cancel orders on testnet
- [ ] WebSocket streaming works reliably
- [ ] All error scenarios handled
- [ ] Rate limiting prevents API bans
- [ ] Database stores exchange configs securely
- [ ] API endpoints accessible and working
- [ ] All 40+ tests passing
- [ ] Integration tests verified on testnet
- [ ] Documentation complete and clear
- [ ] Code follows project standards
- [ ] No compilation warnings
- [ ] CI pipeline green
- [ ] Manual testing completed successfully

---

## 📅 **Estimated Timeline**

| Phase | Estimated Time |
|-------|---------------|
| Phase 1: Interface Design | 0.5 days |
| Phase 2: Binance REST API | 2 days |
| Phase 3: Trading Operations | 2 days |
| Phase 4: WebSocket Support | 1.5 days |
| Phase 5: Database Integration | 1 day |
| Phase 6: API Endpoints | 1 day |
| Phase 7: Service Layer | 1 day |
| Phase 8: Configuration | 0.5 days |
| Phase 9: Error Handling | 1 day |
| Phase 10: Testing | 2 days |
| Phase 11: Documentation | 1 day |
| Phase 12: Build & Commit | 0.5 days |
| **Total** | **~14 days** |

---

## 📦 **Commit Strategy**

Follow the development workflow from `DEVELOPMENT_WORKFLOW.md`:

1. **Commit after each phase** (incremental progress)
2. **Run tests before every commit**
3. **Wait for CI to pass** before proceeding
4. **Update documentation** as you go

Example commits:
```
feat: Design IExchangeConnector interface (Issue #4 Phase 1)
feat: Implement Binance REST market data (Issue #4 Phase 2)
feat: Add Binance trading operations (Issue #4 Phase 3)
feat: Complete Issue #4 - Binance exchange integration
```

---

**Issue Created**: October 24, 2025  
**Priority**: Critical (P0)  
**Estimated Effort**: 14 days  
**Status**: 📋 PLANNED - Ready to begin after Issue #3 completion

---

**Next Steps**:
1. Review this plan with team/stakeholder
2. Set up Binance testnet account and get API keys
3. Begin Phase 1: Interface design
4. Follow DEVELOPMENT_WORKFLOW.md throughout

