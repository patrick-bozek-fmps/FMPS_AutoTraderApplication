# Issue #9: Bitget Connector Implementation (Testnet/Demo)

**Status**: 📋 **PLANNED**  
**Assigned**: TBD  
**Created**: October 28, 2025  
**Started**: Not Started  
**Completed**: Not Completed  
**Duration**: ~4-5 days (estimated)  
**Epic**: Epic 2 (Exchange Integration)  
**Priority**: P1 (High)  
**Dependencies**: Issue #7 (Exchange Connector Framework) ⏳, Issue #8 (Binance Connector) ⏳

> **NOTE**: Requires Bitget testnet API keys. Can leverage patterns from Binance connector (Issue #8). Potentially faster implementation due to reusable patterns.

---

## 📋 **Objective**

Implement a fully functional Bitget exchange connector for the testnet/demo environment, supporting REST API operations (market data, account info, order management) and WebSocket streaming (real-time candlesticks, order updates) using the Exchange Connector Framework, following similar patterns established in the Binance connector.

---

## 🎯 **Goals**

1. **REST API Integration**: Implement all required Bitget testnet REST API endpoints
2. **Authentication**: Implement API signature-based authentication (similar to Binance)
3. **Market Data**: Retrieve candlesticks, tickers, and order book data
4. **Order Management**: Place, cancel, and query orders on demo accounts
5. **Account Information**: Retrieve balance and position information
6. **WebSocket Streaming**: Real-time candlestick and order update streams
7. **Error Handling**: Map Bitget error codes to framework exceptions
8. **Rate Limiting**: Respect Bitget rate limits
9. **Testing**: Comprehensive unit and integration tests using testnet
10. **Reuse Patterns**: Leverage patterns from Binance connector to accelerate development

---

## 📝 **Task Breakdown**

### **Task 1: Setup and Configuration** [Status: ⏳ PENDING]
- [ ] Create Bitget testnet account: https://www.bitget.com/api-doc/spot/intro
- [ ] Generate API keys (API Key, Secret Key, Passphrase)
- [ ] Add Bitget configuration to `application.conf`:
  - [ ] Base URL: `https://api.bitget.com` (or testnet equivalent)
  - [ ] WebSocket URL: `wss://ws.bitget.com/spot/v1/stream`
  - [ ] API key, secret, and passphrase (encrypted)
  - [ ] Rate limits configuration
- [ ] Create `BitgetConfig` data class extending `ExchangeConfig`
- [ ] Add test configuration to `application-test.conf`
- [ ] Note differences from Binance (e.g., passphrase requirement)

### **Task 2: Implement BitgetConnector Class** [Status: ⏳ PENDING]
- [ ] Create `BitgetConnector` extending `AbstractExchangeConnector`
- [ ] Implement constructor with `BitgetConfig`
- [ ] Override `connect()` method:
  - [ ] Test connectivity with server time endpoint
  - [ ] Verify authentication
- [ ] Override `disconnect()` method
- [ ] Implement `isConnected()` health check
- [ ] Add logger instance
- [ ] Follow Binance connector structure for consistency

### **Task 3: Implement Authentication** [Status: ⏳ PENDING]
- [ ] Create `BitgetAuthenticator` class:
  - [ ] Implement signature generation (verify Bitget's specific algorithm)
  - [ ] Add timestamp to all signed requests
  - [ ] Handle passphrase in authentication
  - [ ] Create request signing method
- [ ] Add required headers:
  - [ ] `ACCESS-KEY`: API key
  - [ ] `ACCESS-SIGN`: Signature
  - [ ] `ACCESS-TIMESTAMP`: Request timestamp
  - [ ] `ACCESS-PASSPHRASE`: Passphrase
- [ ] Integrate authenticator with HTTP client
- [ ] Add unit tests for signature generation
- [ ] Verify authentication with account endpoint

### **Task 4: Implement Market Data Methods** [Status: ⏳ PENDING]
- [ ] Implement `getCandles(symbol, interval, startTime, endTime, limit)`:
  - [ ] Map to Bitget candles endpoint
  - [ ] Convert Bitget response to `Candlestick` model
  - [ ] Handle interval enum mapping
  - [ ] Note: symbol format may differ from Binance (e.g., "BTC_USDT" vs "BTCUSDT")
- [ ] Implement `getTicker(symbol)`:
  - [ ] Map to Bitget ticker endpoint
  - [ ] Convert to `Ticker` model
- [ ] Implement `getOrderBook(symbol, limit)`:
  - [ ] Map to Bitget depth endpoint
  - [ ] Convert to `OrderBook` model
- [ ] Add error handling and logging
- [ ] Unit tests for all market data methods
- [ ] Document symbol format conversion if needed

### **Task 5: Implement Account Information Methods** [Status: ⏳ PENDING]
- [ ] Implement `getBalance()`:
  - [ ] Map to Bitget account endpoint (signed)
  - [ ] Parse balances array
  - [ ] Return as `Map<String, BigDecimal>`
- [ ] Implement `getPositions()`:
  - [ ] Bitget-specific endpoint for positions
  - [ ] Convert to `Position` model list
- [ ] Add authentication headers
- [ ] Handle insufficient permissions errors
- [ ] Unit tests for account methods

### **Task 6: Implement Order Management Methods** [Status: ⏳ PENDING]
- [ ] Implement `placeOrder(order: Order)`:
  - [ ] Map to Bitget place order endpoint (POST, signed)
  - [ ] Support order types: MARKET, LIMIT
  - [ ] Support sides: BUY, SELL
  - [ ] Parse response to `Order` model
  - [ ] Handle insufficient balance errors
- [ ] Implement `cancelOrder(orderId: String, symbol: String)`:
  - [ ] Map to Bitget cancel order endpoint (POST, signed)
  - [ ] Return cancelled `Order`
  - [ ] Handle order not found errors
- [ ] Implement `getOrder(orderId: String, symbol: String)`:
  - [ ] Map to Bitget query order endpoint (GET, signed)
  - [ ] Convert to `Order` model
- [ ] Implement `getOrders(symbol: String?)`:
  - [ ] Map to Bitget open orders endpoint (signed)
  - [ ] Optional symbol filter
  - [ ] Return list of `Order`
- [ ] Add retry logic for transient failures
- [ ] Unit tests for all order methods
- [ ] Note differences from Binance API response format

### **Task 7: Implement WebSocket Streaming** [Status: ⏳ PENDING]
- [ ] Create `BitgetWebSocketManager` extending `WebSocketManager`
- [ ] Implement `subscribeCandlesticks(symbol, interval, callback)`:
  - [ ] Bitget-specific WebSocket stream format
  - [ ] Parse JSON to `Candlestick` model
  - [ ] Invoke callback on new candle
  - [ ] Handle subscription errors
- [ ] Implement `subscribeTicker(symbol, callback)`:
  - [ ] Bitget ticker stream
  - [ ] Parse to `Ticker` model
- [ ] Implement `subscribeOrderUpdates(callback)`:
  - [ ] Bitget user data stream (if available)
  - [ ] Note: authentication mechanism may differ from Binance
- [ ] Implement subscription management
- [ ] Handle reconnection and resubscription
- [ ] Unit tests for WebSocket connections
- [ ] Document differences from Binance WebSocket API

### **Task 8: Error Handling and Mapping** [Status: ⏳ PENDING]
- [ ] Create Bitget error code mapping:
  - [ ] Map HTTP status codes to exceptions
  - [ ] Map Bitget error codes to framework exceptions
  - [ ] Research Bitget-specific error codes
- [ ] Implement error parsing from JSON responses
- [ ] Add retry-after header parsing if applicable
- [ ] Log all errors with context
- [ ] Unit tests for error handling
- [ ] Document common Bitget errors

### **Task 9: Rate Limiting Implementation** [Status: ⏳ PENDING]
- [ ] Research Bitget rate limits (verify current limits from docs)
- [ ] Configure rate limits in application.conf
- [ ] Implement weight calculation per endpoint (if applicable)
- [ ] Integrate rate limiter from framework
- [ ] Add metrics for rate limit usage
- [ ] Test rate limit enforcement
- [ ] Unit tests for rate limiting behavior

### **Task 10: Integration Testing** [Status: ⏳ PENDING]
- [ ] Create `BitgetConnectorIntegrationTest` class:
  - [ ] Test connectivity with testnet
  - [ ] Test authentication
  - [ ] Test fetching candlesticks (BTCUSDT or equivalent)
  - [ ] Test fetching account balance
  - [ ] Test placing market order (small amount)
  - [ ] Test cancelling order
  - [ ] Test WebSocket candlestick stream
  - [ ] Test WebSocket order updates
- [ ] Mark tests as `@Tag("integration")`
- [ ] Document testnet account requirements
- [ ] Add test data cleanup
- [ ] Compare behavior with Binance connector for consistency

### **Task 11: Documentation** [Status: ⏳ PENDING]
- [ ] Update `EXCHANGE_CONNECTOR_GUIDE.md` with Bitget specifics
- [ ] Add KDoc to all public methods
- [ ] Create `BITGET_CONNECTOR.md`:
  - [ ] Quick start guide
  - [ ] Configuration examples
  - [ ] Authentication differences from Binance
  - [ ] Symbol format conversion
  - [ ] Common issues and troubleshooting
  - [ ] Testnet vs Production differences
  - [ ] Comparison table: Binance vs Bitget

### **Task 12: Build & Commit** [Status: ⏳ PENDING]
- [ ] Run all tests: `./gradlew test`
- [ ] Run integration tests: `./gradlew integrationTest`
- [ ] Build project: `./gradlew build`
- [ ] Fix any compilation errors
- [ ] Fix any test failures
- [ ] Commit changes: `feat: Issue #9 - Bitget Connector Implementation`
- [ ] Push to GitHub
- [ ] Verify CI pipeline passes
- [ ] Update this Issue file to reflect completion
- [ ] Update Development_Plan_v2.md

---

## 📦 **Deliverables**

### **New Files**
1. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/connectors/bitget/BitgetConnector.kt` - Main connector
2. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/connectors/bitget/BitgetAuthenticator.kt` - Authentication
3. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/connectors/bitget/BitgetWebSocketManager.kt` - WebSocket
4. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/connectors/bitget/BitgetErrorHandler.kt` - Error mapping
5. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/connectors/bitget/BitgetConfig.kt` - Configuration

### **Updated Files**
- `core-service/src/main/resources/application.conf` - Bitget configuration
- `core-service/src/main/resources/application-test.conf` - Test configuration
- `core-service/src/main/kotlin/com/fmps/autotrader/core/connectors/ConnectorFactory.kt` - Register Bitget connector

### **Test Files**
1. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/connectors/bitget/BitgetConnectorTest.kt` - Unit tests
2. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/connectors/bitget/BitgetAuthenticatorTest.kt` - Auth tests
3. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/connectors/bitget/BitgetWebSocketManagerTest.kt` - WebSocket tests
4. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/connectors/bitget/BitgetErrorHandlerTest.kt` - Error handling tests
5. ✅ `core-service/src/integrationTest/kotlin/com/fmps/autotrader/core/connectors/bitget/BitgetConnectorIntegrationTest.kt` - Integration tests

### **Documentation**
- `Cursor/Development_Handbook/BITGET_CONNECTOR.md` - Bitget-specific guide (200+ lines)

---

## 🎯 **Success Criteria**

| Criterion | Status | Verification Method |
|-----------|--------|---------------------|
| Connects to Bitget testnet successfully | ⏳ | Integration test passes |
| Authentication working with API keys + passphrase | ⏳ | Signed requests succeed |
| Fetches candlestick data correctly | ⏳ | Market data tests pass |
| Retrieves account balance | ⏳ | Account tests pass |
| Places market orders successfully | ⏳ | Order tests pass |
| Cancels orders correctly | ⏳ | Cancel order test passes |
| WebSocket candlestick stream working | ⏳ | WebSocket tests pass |
| WebSocket order updates working | ⏳ | Order update tests pass |
| Error handling maps all Bitget errors | ⏳ | Error handling tests pass |
| Rate limiting prevents API abuse | ⏳ | Rate limit tests pass |
| Symbol format conversion working | ⏳ | Market data tests with various symbols |
| All unit tests pass | ⏳ | `./gradlew test` |
| All integration tests pass | ⏳ | `./gradlew integrationTest` |
| Build succeeds | ⏳ | `./gradlew build` |
| CI pipeline passes | ⏳ | GitHub Actions green checkmark |
| Documentation complete | ⏳ | BITGET_CONNECTOR.md exists |
| Code coverage >80% | ⏳ | Coverage report |

---

## 🔧 **Key Technologies**

| Technology | Version | Purpose |
|------------|---------|---------|
| Bitget API | v1 | REST API integration |
| Bitget WebSocket API | v1 | Real-time data streaming |
| Ktor Client | 2.3.7 | HTTP client |
| Ktor Client WebSockets | 2.3.7 | WebSocket client |
| Kotlinx Serialization | 1.6.2 | JSON parsing |
| Java Crypto | JDK 17 | Signature generation |
| JUnit 5 | 5.10+ | Testing |

**No additional dependencies needed** (all from Epic 1 and Issue #7)

---

## 📊 **Architecture/Design**

### **Bitget Connector Architecture**

```
┌─────────────────────────────────────────────────────────────────┐
│                        BitgetConnector                           │
│                                                                   │
│  extends AbstractExchangeConnector                               │
│  implements IExchangeConnector                                   │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                  REST API Methods                         │   │
│  │                                                            │   │
│  │  getCandles() ─────────┐                                  │   │
│  │  getTicker() ──────────┼──▶ BitgetAuthenticator          │   │
│  │  getOrderBook() ───────┤                                  │   │
│  │  getBalance() ─────────┤         │                        │   │
│  │  placeOrder() ─────────┤         ▼                        │   │
│  │  cancelOrder() ────────┤   Sign Request                   │   │
│  │  getOrder() ───────────┤   (API Key + Secret + Passphrase)│   │
│  └──────────────┬──────────┤         │                        │   │
│                 │          └─────────┼────────────────────────┘   │
│                 │                    │                            │
│                 │     ┌──────────────▼──────────────┐            │
│                 │     │      HTTP Client              │            │
│                 │     │   (Ktor Client CIO)           │            │
│                 │     │                               │            │
│                 │     │  GET /api/spot/v1/market/candles│         │
│                 │     │  GET /api/spot/v1/account/assets│         │
│                 │     │  POST /api/spot/v1/trade/orders │         │
│                 │     └──────────────┬────────────────┘            │
│                 │                    │                            │
│                 │                    ▼                            │
│                 │             Bitget API                          │
│                 │         https://api.bitget.com                  │
│                 │                                                 │
│  ┌──────────────▼─────────────────────────────────────────────┐ │
│  │            WebSocket Streaming                              │ │
│  │                                                              │ │
│  │  BitgetWebSocketManager                                     │ │
│  │                                                              │ │
│  │  subscribeCandlesticks() ─────┐                            │ │
│  │  subscribeTicker() ───────────┼──▶ ws streams              │ │
│  │  subscribeOrderUpdates() ─────┤                            │ │
│  │                                │                            │ │
│  │                                ▼                            │ │
│  │                    Bitget WebSocket API                     │ │
│  │                wss://ws.bitget.com/spot/v1/stream           │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │              Error Handling & Rate Limiting                  ││
│  │                                                               ││
│  │  BitgetErrorHandler ──▶ Map error codes                     ││
│  │  RateLimiter ──────────▶ Bitget-specific limits             ││
│  └─────────────────────────────────────────────────────────────┘│
└───────────────────────────────────────────────────────────────────┘
```

### **Key Differences from Binance**

| Aspect | Binance | Bitget | Impact |
|--------|---------|--------|--------|
| Authentication | API Key + Secret | API Key + Secret + **Passphrase** | Extra header required |
| Symbol Format | BTCUSDT | BTC_USDT (or similar) | Conversion needed |
| Signature Algorithm | HMAC SHA256 | (Verify from docs) | May need different implementation |
| Rate Limits | 1200 req/min | (Verify from docs) | Different configuration |
| WebSocket Auth | Listen key | (Verify from docs) | May differ |

---

## ⏱️ **Estimated Timeline**

| Task | Estimated Time |
|------|---------------|
| Task 1: Setup and Configuration | 2 hours |
| Task 2: Implement BitgetConnector Class | 3 hours |
| Task 3: Implement Authentication | 5 hours |
| Task 4: Implement Market Data Methods | 6 hours |
| Task 5: Implement Account Information Methods | 3 hours |
| Task 6: Implement Order Management Methods | 8 hours |
| Task 7: Implement WebSocket Streaming | 6 hours |
| Task 8: Error Handling and Mapping | 4 hours |
| Task 9: Rate Limiting Implementation | 3 hours |
| Task 10: Integration Testing | 6 hours |
| Task 11: Documentation | 4 hours |
| Task 12: Build & Commit | 2 hours |
| **Total** | **~52 hours (~6.5 days)** |

**Realistic Estimate**: 4-5 days with focused work (faster than Binance due to reusable patterns)

---

## 🔄 **Dependencies**

### **Depends On** (Must be complete first)
- ⏳ Issue #7: Exchange Connector Framework (must be complete)
- ⏳ Issue #8: Binance Connector (recommended for pattern reuse)
- ✅ Issue #6: Configuration Management
- ✅ Issue #5: Core Data Models

### **Blocks** (Cannot start until this is done)
- Epic 3: AI Trading Engine (needs both exchange connectors)

### **Related** (Related but not blocking)
- Issue #10: Technical Indicators (can use Bitget data for testing)
- Issue #4: Logging Infrastructure (will use for logging)

---

## 📚 **Resources**

### **Documentation**
- Bitget API Docs: https://bitgetlimited.github.io/apidoc/en/spot/
- Bitget Authentication: https://bitgetlimited.github.io/apidoc/en/spot/#authentication
- Bitget WebSocket: https://bitgetlimited.github.io/apidoc/en/spot/#websocket
- Bitget Error Codes: (Verify from docs)

### **Examples**
- Issue #8: Binance Connector (reference implementation)

### **Reference Issues**
- Issue #7: Exchange Connector Framework
- Issue #8: Binance Connector (similar patterns)
- Issue #6: Configuration Management

---

## ⚠️ **Risks & Considerations**

| Risk | Impact | Mitigation Strategy |
|------|--------|---------------------|
| Bitget API docs less comprehensive than Binance | Medium | Thorough testing, community resources, trial and error |
| Testnet availability/stability | Medium | Document limitations, use production read-only as fallback |
| Authentication differences from Binance | High | Careful implementation, comprehensive auth testing |
| Symbol format differences | Low | Abstraction layer for symbol conversion |
| WebSocket API differences | Medium | Separate WebSocket manager, reuse patterns where possible |
| Smaller community support | Low | Rely on Binance patterns, official docs, support channels |

---

## 📈 **Definition of Done**

- [ ] All tasks completed
- [ ] All subtasks checked off
- [ ] Bitget testnet account created with API keys
- [ ] `BitgetConnector` fully implemented
- [ ] Authentication working with passphrase
- [ ] Symbol format conversion working
- [ ] All market data methods implemented
- [ ] All account methods implemented
- [ ] All order management methods implemented
- [ ] WebSocket streaming working
- [ ] Error handling complete with all Bitget error codes mapped
- [ ] Rate limiting configured and tested
- [ ] All deliverables created/updated
- [ ] All success criteria met
- [ ] All unit tests written and passing (>80% coverage)
- [ ] All integration tests passing with testnet
- [ ] Documentation complete (BITGET_CONNECTOR.md)
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
- [ ] Both Binance and Bitget connectors working side-by-side

---

## 💡 **Notes & Learnings**

*This section will be populated during implementation*

**Important Notes**:
- Leverage Binance connector patterns to accelerate development
- Document all differences from Binance for future reference
- Keep API keys secure - never commit to Git
- Test symbol format conversion thoroughly
- Verify all authentication headers are present

---

## 📦 **Commit Strategy**

```
feat: Issue #9 Task 1-3 - Bitget connector setup and authentication
feat: Issue #9 Task 4-5 - Market data and account methods
feat: Issue #9 Task 6 - Order management implementation
feat: Issue #9 Task 7-8 - WebSocket streaming and error handling
feat: Issue #9 Task 9-10 - Rate limiting and integration tests
docs: Issue #9 Task 11 - Bitget connector documentation
feat: Complete Issue #9 - Bitget Connector Implementation
```

---

**Issue Created**: October 28, 2025  
**Priority**: P1 (High - Critical for Epic 2)  
**Estimated Effort**: 4-5 days  
**Status**: 📋 PLANNED

---

**Next Steps**:
1. Wait for Issue #7 (Exchange Connector Framework) to complete
2. Review Issue #8 (Binance Connector) for reusable patterns
3. Create Bitget testnet account and generate API keys
4. Begin Task 1: Setup and Configuration
5. Follow DEVELOPMENT_WORKFLOW.md throughout
6. Update status as progress is made

