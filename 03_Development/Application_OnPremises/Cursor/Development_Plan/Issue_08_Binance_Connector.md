# Issue #8: Binance Connector Implementation (Testnet/Demo)

**Status**: 📋 **PLANNED**  
**Assigned**: TBD  
**Created**: October 28, 2025  
**Started**: Not Started  
**Completed**: Not Completed  
**Duration**: ~5-6 days (estimated)  
**Epic**: Epic 2 (Exchange Integration)  
**Priority**: P1 (High)  
**Dependencies**: Issue #7 (Exchange Connector Framework) ⏳, Issue #6 (Configuration Management) ✅

> **NOTE**: Requires Binance testnet API keys. Must complete Issue #7 framework before starting implementation.

---

## 📋 **Objective**

Implement a fully functional Binance exchange connector for the testnet/demo environment, supporting REST API operations (market data, account info, order management) and WebSocket streaming (real-time candlesticks, order updates) using the Exchange Connector Framework from Issue #7.

---

## 🎯 **Goals**

1. **REST API Integration**: Implement all required Binance testnet REST API endpoints
2. **Authentication**: Implement HMAC SHA256 signature-based authentication for API requests
3. **Market Data**: Retrieve candlesticks, tickers, and order book data
4. **Order Management**: Place, cancel, and query orders on demo accounts
5. **Account Information**: Retrieve balance and position information
6. **WebSocket Streaming**: Real-time candlestick and order update streams
7. **Error Handling**: Map Binance error codes to framework exceptions
8. **Rate Limiting**: Respect Binance rate limits (1200 requests/minute, with weights)
9. **Testing**: Comprehensive unit and integration tests using testnet

---

## 📝 **Task Breakdown**

### **Task 1: Setup and Configuration** [Status: ⏳ PENDING]
- [ ] Create Binance testnet account: https://testnet.binance.vision/
- [ ] Generate API keys (API Key + Secret Key)
- [ ] Add Binance configuration to `application.conf`:
  - [ ] Base URL: `https://testnet.binance.vision`
  - [ ] WebSocket URL: `wss://testnet.binance.vision/ws`
  - [ ] API key and secret (encrypted)
  - [ ] Rate limits configuration
- [ ] Create `BinanceConfig` data class extending `ExchangeConfig`
- [ ] Add test configuration to `application-test.conf`

### **Task 2: Implement BinanceConnector Class** [Status: ⏳ PENDING]
- [ ] Create `BinanceConnector` extending `AbstractExchangeConnector`
- [ ] Implement constructor with `BinanceConfig`
- [ ] Override `connect()` method:
  - [ ] Test connectivity with `/api/v3/ping`
  - [ ] Verify server time with `/api/v3/time`
  - [ ] Adjust timestamp offset if needed
- [ ] Override `disconnect()` method
- [ ] Implement `isConnected()` health check
- [ ] Add logger instance

### **Task 3: Implement Authentication** [Status: ⏳ PENDING]
- [ ] Create `BinanceAuthenticator` class:
  - [ ] Implement HMAC SHA256 signature generation
  - [ ] Add timestamp to all signed requests
  - [ ] Create query string signing method
  - [ ] Handle `recvWindow` parameter
- [ ] Integrate authenticator with HTTP client
- [ ] Add unit tests for signature generation
- [ ] Verify authentication with `/api/v3/account` test call

### **Task 4: Implement Market Data Methods** [Status: ⏳ PENDING]
- [ ] Implement `getCandles(symbol, interval, startTime, endTime, limit)`:
  - [ ] Map to `/api/v3/klines` endpoint
  - [ ] Convert Binance response to `Candlestick` model
  - [ ] Handle interval enum mapping (1m, 5m, 15m, 1h, 4h, 1d)
  - [ ] Implement pagination for large date ranges
- [ ] Implement `getTicker(symbol)`:
  - [ ] Map to `/api/v3/ticker/24hr`
  - [ ] Convert to `Ticker` model
- [ ] Implement `getOrderBook(symbol, limit)`:
  - [ ] Map to `/api/v3/depth`
  - [ ] Convert to `OrderBook` model
- [ ] Add error handling and logging
- [ ] Unit tests for all market data methods

### **Task 5: Implement Account Information Methods** [Status: ⏳ PENDING]
- [ ] Implement `getBalance()`:
  - [ ] Map to `/api/v3/account` (signed)
  - [ ] Parse balances array
  - [ ] Filter non-zero balances
  - [ ] Return as `Map<String, BigDecimal>`
- [ ] Implement `getPositions()`:
  - [ ] For spot trading, positions = balances
  - [ ] For futures, use separate endpoint (if applicable)
  - [ ] Convert to `Position` model list
- [ ] Add authentication headers
- [ ] Handle insufficient permissions errors
- [ ] Unit tests for account methods

### **Task 6: Implement Order Management Methods** [Status: ⏳ PENDING]
- [ ] Implement `placeOrder(order: Order)`:
  - [ ] Map to `/api/v3/order` (POST, signed)
  - [ ] Support order types: MARKET, LIMIT
  - [ ] Support sides: BUY, SELL
  - [ ] Handle time-in-force (GTC, IOC, FOK)
  - [ ] Parse response to `Order` model
  - [ ] Handle insufficient balance errors
- [ ] Implement `cancelOrder(orderId: String, symbol: String)`:
  - [ ] Map to `/api/v3/order` (DELETE, signed)
  - [ ] Return cancelled `Order`
  - [ ] Handle order not found errors
- [ ] Implement `getOrder(orderId: String, symbol: String)`:
  - [ ] Map to `/api/v3/order` (GET, signed)
  - [ ] Convert to `Order` model
- [ ] Implement `getOrders(symbol: String?)`:
  - [ ] Map to `/api/v3/openOrders` (signed)
  - [ ] Optional symbol filter
  - [ ] Return list of `Order`
- [ ] Add retry logic for transient failures
- [ ] Unit tests for all order methods

### **Task 7: Implement WebSocket Streaming** [Status: ⏳ PENDING]
- [ ] Create `BinanceWebSocketManager` extending `WebSocketManager`
- [ ] Implement `subscribeCandlesticks(symbol, interval, callback)`:
  - [ ] Stream: `wss://testnet.binance.vision/ws/{symbol}@kline_{interval}`
  - [ ] Parse JSON to `Candlestick` model
  - [ ] Invoke callback on new candle
  - [ ] Handle subscription errors
- [ ] Implement `subscribeTicker(symbol, callback)`:
  - [ ] Stream: `wss://testnet.binance.vision/ws/{symbol}@ticker`
  - [ ] Parse to `Ticker` model
- [ ] Implement `subscribeOrderUpdates(callback)`:
  - [ ] User data stream (requires listen key)
  - [ ] POST `/api/v3/userDataStream` to get listen key
  - [ ] Stream: `wss://testnet.binance.vision/ws/{listenKey}`
  - [ ] Keep-alive: PUT `/api/v3/userDataStream` every 30 minutes
- [ ] Implement subscription management (subscribe/unsubscribe)
- [ ] Handle reconnection and resubscription
- [ ] Unit tests for WebSocket connections

### **Task 8: Error Handling and Mapping** [Status: ⏳ PENDING]
- [ ] Create Binance error code mapping:
  - [ ] Map HTTP status codes to exceptions
  - [ ] Map Binance error codes to framework exceptions:
    - [ ] `-1021` (Timestamp error) → `ConnectionException`
    - [ ] `-1022` (Invalid signature) → `AuthenticationException`
    - [ ] `-2010` (Insufficient funds) → `InsufficientFundsException`
    - [ ] `-2011` (Order not found) → `OrderException`
    - [ ] `-1003` (Rate limit) → `RateLimitException`
- [ ] Implement error parsing from JSON responses
- [ ] Add retry-after header parsing for rate limits
- [ ] Log all errors with context
- [ ] Unit tests for error handling

### **Task 9: Rate Limiting Implementation** [Status: ⏳ PENDING]
- [ ] Configure Binance rate limits:
  - [ ] General: 1200 requests per minute
  - [ ] Order endpoints: 10 orders per second per account
  - [ ] Weight-based limits (some endpoints cost more)
- [ ] Implement weight calculation per endpoint
- [ ] Integrate rate limiter from framework
- [ ] Add metrics for rate limit usage
- [ ] Test rate limit enforcement
- [ ] Unit tests for rate limiting behavior

### **Task 10: Integration Testing** [Status: ⏳ PENDING]
- [ ] Create `BinanceConnectorIntegrationTest` class:
  - [ ] Test connectivity with testnet
  - [ ] Test authentication
  - [ ] Test fetching candlesticks (BTCUSDT)
  - [ ] Test fetching account balance
  - [ ] Test placing market order (small amount)
  - [ ] Test cancelling order
  - [ ] Test WebSocket candlestick stream
  - [ ] Test WebSocket order updates
- [ ] Mark tests as `@Tag("integration")` for selective execution
- [ ] Document testnet account requirements
- [ ] Add test data cleanup

### **Task 11: Documentation** [Status: ⏳ PENDING]
- [ ] Update `EXCHANGE_CONNECTOR_GUIDE.md` with Binance specifics:
  - [ ] Authentication process
  - [ ] Signature generation
  - [ ] Endpoint mappings
  - [ ] Error code reference
  - [ ] Rate limits
  - [ ] WebSocket streams
- [ ] Add KDoc to all public methods
- [ ] Create `BINANCE_CONNECTOR.md`:
  - [ ] Quick start guide
  - [ ] Configuration examples
  - [ ] Common issues and troubleshooting
  - [ ] Testnet vs Production differences

### **Task 12: Build & Commit** [Status: ⏳ PENDING]
- [ ] Run all tests: `./gradlew test`
- [ ] Run integration tests: `./gradlew integrationTest`
- [ ] Build project: `./gradlew build`
- [ ] Fix any compilation errors
- [ ] Fix any test failures
- [ ] Commit changes: `feat: Issue #8 - Binance Connector Implementation`
- [ ] Push to GitHub
- [ ] Verify CI pipeline passes
- [ ] Update this Issue file to reflect completion
- [ ] Update Development_Plan_v2.md

---

## 📦 **Deliverables**

### **New Files**
1. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/connectors/binance/BinanceConnector.kt` - Main connector
2. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/connectors/binance/BinanceAuthenticator.kt` - Authentication
3. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/connectors/binance/BinanceWebSocketManager.kt` - WebSocket
4. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/connectors/binance/BinanceErrorHandler.kt` - Error mapping
5. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/connectors/binance/BinanceConfig.kt` - Configuration

### **Updated Files**
- `core-service/src/main/resources/application.conf` - Binance configuration
- `core-service/src/main/resources/application-test.conf` - Test configuration
- `core-service/src/main/kotlin/com/fmps/autotrader/core/connectors/ConnectorFactory.kt` - Register Binance connector

### **Test Files**
1. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/connectors/binance/BinanceConnectorTest.kt` - Unit tests
2. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/connectors/binance/BinanceAuthenticatorTest.kt` - Auth tests
3. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/connectors/binance/BinanceWebSocketManagerTest.kt` - WebSocket tests
4. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/connectors/binance/BinanceErrorHandlerTest.kt` - Error handling tests
5. ✅ `core-service/src/integrationTest/kotlin/com/fmps/autotrader/core/connectors/binance/BinanceConnectorIntegrationTest.kt` - Integration tests

### **Documentation**
- `Cursor/Development_Handbook/BINANCE_CONNECTOR.md` - Binance-specific guide (200+ lines)

---

## 🎯 **Success Criteria**

| Criterion | Status | Verification Method |
|-----------|--------|---------------------|
| Connects to Binance testnet successfully | ⏳ | Integration test passes |
| Authentication working with API keys | ⏳ | Signed requests succeed |
| Fetches candlestick data correctly | ⏳ | Market data tests pass |
| Retrieves account balance | ⏳ | Account tests pass |
| Places market orders successfully | ⏳ | Order tests pass |
| Cancels orders correctly | ⏳ | Cancel order test passes |
| WebSocket candlestick stream working | ⏳ | WebSocket tests pass |
| WebSocket order updates working | ⏳ | Order update tests pass |
| Error handling maps all Binance errors | ⏳ | Error handling tests pass |
| Rate limiting prevents API abuse | ⏳ | Rate limit tests pass |
| All unit tests pass | ⏳ | `./gradlew test` |
| All integration tests pass | ⏳ | `./gradlew integrationTest` |
| Build succeeds | ⏳ | `./gradlew build` |
| CI pipeline passes | ⏳ | GitHub Actions green checkmark |
| Documentation complete | ⏳ | BINANCE_CONNECTOR.md exists |
| Code coverage >80% | ⏳ | Coverage report |

---

## 🔧 **Key Technologies**

| Technology | Version | Purpose |
|------------|---------|---------|
| Binance API | v3 | REST API integration |
| Binance WebSocket API | v1 | Real-time data streaming |
| Ktor Client | 2.3.7 | HTTP client |
| Ktor Client WebSockets | 2.3.7 | WebSocket client |
| Kotlinx Serialization | 1.6.2 | JSON parsing |
| Java Crypto | JDK 17 | HMAC SHA256 signatures |
| JUnit 5 | 5.10+ | Testing |

**No additional dependencies needed** (all from Epic 1 and Issue #7)

---

## 📊 **Architecture/Design**

### **Binance Connector Architecture**

```
┌─────────────────────────────────────────────────────────────────┐
│                       BinanceConnector                           │
│                                                                   │
│  extends AbstractExchangeConnector                               │
│  implements IExchangeConnector                                   │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                  REST API Methods                         │   │
│  │                                                            │   │
│  │  getCandles() ─────────┐                                  │   │
│  │  getTicker() ──────────┼──▶ BinanceAuthenticator        │   │
│  │  getOrderBook() ───────┤                                  │   │
│  │  getBalance() ─────────┤         │                        │   │
│  │  placeOrder() ─────────┤         ▼                        │   │
│  │  cancelOrder() ────────┤   Sign Request                   │   │
│  │  getOrder() ───────────┤   (HMAC SHA256)                  │   │
│  └──────────────┬──────────┤         │                        │   │
│                 │          └─────────┼────────────────────────┘   │
│                 │                    │                            │
│                 │     ┌──────────────▼──────────────┐            │
│                 │     │      HTTP Client              │            │
│                 │     │   (Ktor Client CIO)           │            │
│                 │     │                               │            │
│                 │     │  GET /api/v3/klines          │            │
│                 │     │  GET /api/v3/account          │            │
│                 │     │  POST /api/v3/order           │            │
│                 │     └──────────────┬────────────────┘            │
│                 │                    │                            │
│                 │                    ▼                            │
│                 │         Binance Testnet REST API               │
│                 │     https://testnet.binance.vision             │
│                 │                                                 │
│  ┌──────────────▼─────────────────────────────────────────────┐ │
│  │            WebSocket Streaming                              │ │
│  │                                                              │ │
│  │  BinanceWebSocketManager                                    │ │
│  │                                                              │ │
│  │  subscribeCandlesticks() ─────┐                            │ │
│  │  subscribeTicker() ───────────┼──▶ ws://{symbol}@kline_1m │ │
│  │  subscribeOrderUpdates() ─────┤                            │ │
│  │                                │                            │ │
│  │                                ▼                            │ │
│  │                    Binance WebSocket API                    │ │
│  │                wss://testnet.binance.vision/ws              │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │              Error Handling & Rate Limiting                  ││
│  │                                                               ││
│  │  BinanceErrorHandler ──▶ Map error codes                    ││
│  │  RateLimiter ──────────▶ 1200 req/min, weight-based         ││
│  └─────────────────────────────────────────────────────────────┘│
└───────────────────────────────────────────────────────────────────┘
```

### **Signature Generation Flow**

```
┌────────────┐
│ API Request│
└──────┬─────┘
       │
       ▼
┌────────────────────┐
│ Add timestamp      │
│ timestamp=165...   │
└──────┬─────────────┘
       │
       ▼
┌────────────────────┐
│ Create query string│
│ symbol=BTCUSDT&... │
└──────┬─────────────┘
       │
       ▼
┌────────────────────┐
│ HMAC SHA256 sign   │
│ with API secret    │
└──────┬─────────────┘
       │
       ▼
┌────────────────────┐
│ Append signature   │
│ &signature=a3b2... │
└──────┬─────────────┘
       │
       ▼
┌────────────────────┐
│ Add API key header │
│ X-MBX-APIKEY       │
└──────┬─────────────┘
       │
       ▼
┌────────────────────┐
│ Send to Binance    │
└────────────────────┘
```

---

## ⏱️ **Estimated Timeline**

| Task | Estimated Time |
|------|---------------|
| Task 1: Setup and Configuration | 2 hours |
| Task 2: Implement BinanceConnector Class | 4 hours |
| Task 3: Implement Authentication | 6 hours |
| Task 4: Implement Market Data Methods | 8 hours |
| Task 5: Implement Account Information Methods | 4 hours |
| Task 6: Implement Order Management Methods | 10 hours |
| Task 7: Implement WebSocket Streaming | 8 hours |
| Task 8: Error Handling and Mapping | 5 hours |
| Task 9: Rate Limiting Implementation | 4 hours |
| Task 10: Integration Testing | 8 hours |
| Task 11: Documentation | 5 hours |
| Task 12: Build & Commit | 2 hours |
| **Total** | **~66 hours (~8 days)** |

**Realistic Estimate**: 5-6 days with focused work

---

## 🔄 **Dependencies**

### **Depends On** (Must be complete first)
- ⏳ Issue #7: Exchange Connector Framework (must be complete)
- ✅ Issue #6: Configuration Management
- ✅ Issue #5: Core Data Models

### **Blocks** (Cannot start until this is done)
- Epic 3: AI Trading Engine (needs Binance connector for trading)
- Issue #10: Technical Indicators (can use Binance data for testing)

### **Related** (Related but not blocking)
- Issue #9: Bitget Connector (similar implementation pattern)
- Issue #4: Logging Infrastructure (will use for logging)

---

## 📚 **Resources**

### **Documentation**
- Binance API Docs: https://binance-docs.github.io/apidocs/spot/en/
- Binance Testnet: https://testnet.binance.vision/
- Authentication Guide: https://binance-docs.github.io/apidocs/spot/en/#endpoint-security-type
- WebSocket Streams: https://binance-docs.github.io/apidocs/spot/en/#websocket-market-streams
- User Data Streams: https://binance-docs.github.io/apidocs/spot/en/#user-data-streams
- Error Codes: https://binance-docs.github.io/apidocs/spot/en/#error-codes

### **Examples**
- Binance Kotlin Client: https://github.com/Binance-docs/Binance_Futures_Kotlin
- HMAC SHA256 in Kotlin: https://www.baeldung.com/kotlin/hmac

### **Reference Issues**
- Issue #7: Exchange Connector Framework (interface implementation)
- Issue #6: Configuration Management (config patterns)

---

## ⚠️ **Risks & Considerations**

| Risk | Impact | Mitigation Strategy |
|------|--------|---------------------|
| Testnet instability | Medium | Retry logic, fallback to production endpoints (read-only) |
| Authentication failures | High | Comprehensive testing, clear error messages, debug logging |
| Rate limit violations | Medium | Conservative rate limits, monitoring, graceful degradation |
| WebSocket disconnections | Medium | Auto-reconnect, subscription persistence |
| API response format changes | Low | Version locking, comprehensive parsing tests |
| Testnet data quality issues | Low | Document limitations, test with multiple symbols |
| Order execution timing | Medium | Async order handling, status polling |

---

## 📈 **Definition of Done**

- [ ] All tasks completed
- [ ] All subtasks checked off
- [ ] Binance testnet account created with API keys
- [ ] `BinanceConnector` fully implemented
- [ ] Authentication working with HMAC SHA256 signatures
- [ ] All market data methods implemented
- [ ] All account methods implemented
- [ ] All order management methods implemented
- [ ] WebSocket streaming working
- [ ] Error handling complete with all Binance error codes mapped
- [ ] Rate limiting configured and tested
- [ ] All deliverables created/updated
- [ ] All success criteria met
- [ ] All unit tests written and passing (>80% coverage)
- [ ] All integration tests passing with testnet
- [ ] Documentation complete (BINANCE_CONNECTOR.md)
- [ ] Code review completed
- [ ] All tests pass: `./gradlew test`
- [ ] Integration tests pass: `./gradlew integrationTest`
- [ ] Build succeeds: `./gradlew build`
- [ ] CI pipeline passes (GitHub Actions)
- [ ] Issue file updated to reflect completion
- [ ] Development_Plan_v2.md updated with progress
- [ ] Changes committed to Git
- [ ] Changes pushed to GitHub
- [ ] Connector available via ConnectorFactory

---

## 💡 **Notes & Learnings**

*This section will be populated during implementation*

**Important Notes**:
- Testnet resets periodically - document reset schedule
- Some endpoints may behave differently on testnet vs production
- Keep API keys secure - never commit to Git
- User data streams require keep-alive every 30 minutes

---

## 📦 **Commit Strategy**

```
feat: Issue #8 Task 1-3 - Binance connector setup and authentication
feat: Issue #8 Task 4-5 - Market data and account methods
feat: Issue #8 Task 6 - Order management implementation
feat: Issue #8 Task 7-8 - WebSocket streaming and error handling
feat: Issue #8 Task 9-10 - Rate limiting and integration tests
docs: Issue #8 Task 11 - Binance connector documentation
feat: Complete Issue #8 - Binance Connector Implementation
```

---

**Issue Created**: October 28, 2025  
**Priority**: P1 (High - Critical for Epic 2)  
**Estimated Effort**: 5-6 days  
**Status**: 📋 PLANNED

---

**Next Steps**:
1. Wait for Issue #7 (Exchange Connector Framework) to complete
2. Create Binance testnet account and generate API keys
3. Begin Task 1: Setup and Configuration
4. Follow DEVELOPMENT_WORKFLOW.md throughout
5. Update status as progress is made

