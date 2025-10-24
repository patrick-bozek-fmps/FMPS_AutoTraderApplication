# Issue #3: Set up REST API Server with Ktor

**Status**: 📋 **PLANNED** (Not Started)  
**Assigned**: AI Assistant  
**Estimated Duration**: 3-4 hours  
**Epic**: Foundation & Infrastructure (Phase 1)  
**Priority**: P0 (Critical)  
**Dependencies**: Issue #1 (Gradle), Issue #2 (Database Layer)

---

## 📋 **Objective**

Implement a complete REST API server using Ktor framework to expose the database layer through HTTP endpoints. The API will serve as the communication bridge between the Core Service and the Desktop UI, supporting CRUD operations for AI traders, trades, patterns, and configurations, along with WebSocket support for real-time updates.

---

## 📝 **Tasks to Complete**

### **Phase 1: Ktor Server Setup**
- [ ] Configure Ktor application module
- [ ] Set up Netty engine with port configuration
- [ ] Configure content negotiation (JSON serialization)
- [ ] Set up CORS for local development
- [ ] Configure request/response logging
- [ ] Add development mode configuration
- [ ] Create server startup/shutdown lifecycle

### **Phase 2: Shared Data Models (DTOs)**
- [ ] Create `shared` module DTOs package structure
- [ ] Create `AITraderDTO` with all fields
- [ ] Create `TradeDTO` with all fields
- [ ] Create `PatternDTO` with all fields
- [ ] Create `ConfigurationDTO`
- [ ] Create `TradeStatisticsDTO`
- [ ] Create request/response wrapper classes:
  - [ ] `CreateAITraderRequest`
  - [ ] `UpdateAITraderRequest`
  - [ ] `CreateTradeRequest`
  - [ ] `CloseTradeRequest`
  - [ ] `CreatePatternRequest`
- [ ] Add Kotlinx Serialization annotations
- [ ] Create mapper functions (Entity ↔ DTO)

### **Phase 3: AI Trader Endpoints**
- [ ] **GET** `/api/v1/traders` - List all AI traders
- [ ] **GET** `/api/v1/traders/{id}` - Get trader by ID
- [ ] **POST** `/api/v1/traders` - Create new trader (enforce 3 limit)
- [ ] **PUT** `/api/v1/traders/{id}` - Update trader configuration
- [ ] **PATCH** `/api/v1/traders/{id}/status` - Update status (START/PAUSE/STOP)
- [ ] **PATCH** `/api/v1/traders/{id}/balance` - Update balance
- [ ] **DELETE** `/api/v1/traders/{id}` - Delete trader
- [ ] **GET** `/api/v1/traders/active` - List active traders
- [ ] **GET** `/api/v1/traders/can-create` - Check if can create more

### **Phase 4: Trade Endpoints**
- [ ] **GET** `/api/v1/trades` - List all trades (with filtering)
- [ ] **GET** `/api/v1/trades/{id}` - Get trade by ID
- [ ] **POST** `/api/v1/trades` - Create new trade (entry)
- [ ] **PATCH** `/api/v1/trades/{id}/close` - Close trade (exit)
- [ ] **PATCH** `/api/v1/trades/{id}/stop-loss` - Update stop-loss
- [ ] **GET** `/api/v1/trades/open` - List open trades
- [ ] **GET** `/api/v1/trades/trader/{traderId}` - Trades by AI trader
- [ ] **GET** `/api/v1/trades/statistics/{traderId}` - Trade statistics

### **Phase 5: Pattern Endpoints**
- [ ] **GET** `/api/v1/patterns` - List all patterns
- [ ] **GET** `/api/v1/patterns/{id}` - Get pattern by ID
- [ ] **POST** `/api/v1/patterns` - Create new pattern
- [ ] **PATCH** `/api/v1/patterns/{id}/statistics` - Update stats after trade
- [ ] **GET** `/api/v1/patterns/active` - List active patterns
- [ ] **GET** `/api/v1/patterns/match` - Find matching patterns (query params)
- [ ] **GET** `/api/v1/patterns/top` - Get top performing patterns
- [ ] **PATCH** `/api/v1/patterns/{id}/activate` - Activate pattern
- [ ] **PATCH** `/api/v1/patterns/{id}/deactivate` - Deactivate pattern
- [ ] **DELETE** `/api/v1/patterns/{id}` - Delete pattern

### **Phase 6: Configuration Endpoints**
- [ ] **GET** `/api/v1/config` - List all configurations
- [ ] **GET** `/api/v1/config/{key}` - Get config by key
- [ ] **PUT** `/api/v1/config/{key}` - Update config value
- [ ] **GET** `/api/v1/config/category/{category}` - Get configs by category

### **Phase 7: Health & Status Endpoints**
- [ ] **GET** `/api/health` - Health check endpoint
- [ ] **GET** `/api/status` - System status (traders, DB stats)
- [ ] **GET** `/api/version` - API version information

### **Phase 8: Error Handling & Validation**
- [ ] Create custom exception classes:
  - [ ] `ResourceNotFoundException`
  - [ ] `ValidationException`
  - [ ] `BusinessRuleException`
  - [ ] `MaxLimitReachedException`
- [ ] Implement global exception handler
- [ ] Create standardized error response format
- [ ] Add request validation for all endpoints
- [ ] Add HTTP status code mapping
- [ ] Add detailed error messages

### **Phase 9: WebSocket Support (Real-Time Updates)**
- [ ] Configure WebSocket routing
- [ ] Create WebSocket session manager
- [ ] Implement trader status updates channel
- [ ] Implement trade updates channel
- [ ] Implement market data updates channel (placeholder)
- [ ] Add connection lifecycle management
- [ ] Add authentication for WebSocket connections

### **Phase 10: API Documentation**
- [ ] Add endpoint descriptions with KDoc
- [ ] Document request/response formats
- [ ] Document error responses
- [ ] Create OpenAPI/Swagger spec (optional for v1.0)
- [ ] Create API usage examples
- [ ] Document WebSocket protocol

### **Phase 11: Integration with Main.kt**
- [ ] Update Main.kt to start Ktor server
- [ ] Configure server port from application.conf
- [ ] Add graceful shutdown for server
- [ ] Integrate with DatabaseFactory
- [ ] Add startup logging

### **Phase 12: Testing**
- [ ] Create test utilities for Ktor
- [ ] Write endpoint tests for AI Trader routes (9 tests)
- [ ] Write endpoint tests for Trade routes (8 tests)
- [ ] Write endpoint tests for Pattern routes (10 tests)
- [ ] Write endpoint tests for Configuration routes (4 tests)
- [ ] Write WebSocket connection tests (3 tests)
- [ ] Write error handling tests (5 tests)
- [ ] Test with actual database (integration tests)
- [ ] Test concurrent requests
- [ ] Test request validation

### **Phase 13: Build & Quality Assurance**
- [ ] Fix any compilation errors
- [ ] Fix any test failures
- [ ] Run full test suite
- [ ] Verify all endpoints work with Postman/curl
- [ ] Check API response times
- [ ] Verify error handling
- [ ] Run `./gradlew build` successfully

### **Phase 14: Documentation & Commit**
- [ ] Update README with API endpoints
- [ ] Create API_DOCUMENTATION.md
- [ ] Update Development_Plan_v2.md progress
- [ ] Create ISSUE_03_SUMMARY.md
- [ ] Commit changes with detailed message
- [ ] Push to GitHub
- [ ] Verify CI pipeline passes

---

## 📦 **Deliverables**

### **Shared Module (DTOs)**
1. `shared/src/main/kotlin/com/fmps/autotrader/shared/dto/AITraderDTO.kt`
2. `shared/src/main/kotlin/com/fmps/autotrader/shared/dto/TradeDTO.kt`
3. `shared/src/main/kotlin/com/fmps/autotrader/shared/dto/PatternDTO.kt`
4. `shared/src/main/kotlin/com/fmps/autotrader/shared/dto/ConfigurationDTO.kt`
5. `shared/src/main/kotlin/com/fmps/autotrader/shared/dto/TradeStatisticsDTO.kt`
6. `shared/src/main/kotlin/com/fmps/autotrader/shared/dto/ErrorResponse.kt`
7. Request/Response classes (10+ files)

### **Core-Service API Layer**
8. `core-service/src/main/kotlin/.../api/routes/AITraderRoutes.kt`
9. `core-service/src/main/kotlin/.../api/routes/TradeRoutes.kt`
10. `core-service/src/main/kotlin/.../api/routes/PatternRoutes.kt`
11. `core-service/src/main/kotlin/.../api/routes/ConfigurationRoutes.kt`
12. `core-service/src/main/kotlin/.../api/routes/HealthRoutes.kt`
13. `core-service/src/main/kotlin/.../api/routes/WebSocketRoutes.kt`

### **Utilities & Extensions**
14. `core-service/src/main/kotlin/.../api/extensions/RoutingExtensions.kt`
15. `core-service/src/main/kotlin/.../api/extensions/CallExtensions.kt`
16. `core-service/src/main/kotlin/.../api/mappers/EntityMappers.kt`

### **Error Handling**
17. `core-service/src/main/kotlin/.../api/exceptions/ApiExceptions.kt`
18. `core-service/src/main/kotlin/.../api/exceptions/ExceptionHandler.kt`

### **WebSocket**
19. `core-service/src/main/kotlin/.../api/websocket/WebSocketManager.kt`
20. `core-service/src/main/kotlin/.../api/websocket/WebSocketSession.kt`

### **Server Configuration**
21. `core-service/src/main/kotlin/.../api/Application.kt` (Ktor setup)
22. Updated `core-service/src/main/kotlin/.../core/Main.kt`
23. Updated `application.conf` with Ktor settings

### **Tests (40+ tests)**
24. `core-service/src/test/kotlin/.../api/routes/AITraderRoutesTest.kt`
25. `core-service/src/test/kotlin/.../api/routes/TradeRoutesTest.kt`
26. `core-service/src/test/kotlin/.../api/routes/PatternRoutesTest.kt`
27. `core-service/src/test/kotlin/.../api/routes/ConfigurationRoutesTest.kt`
28. `core-service/src/test/kotlin/.../api/routes/WebSocketRoutesTest.kt`
29. `core-service/src/test/kotlin/.../api/ErrorHandlingTest.kt`

### **Documentation**
30. `03_Development/Application_OnPremises/Cursor/API_DOCUMENTATION.md`
31. `03_Development/Application_OnPremises/Cursor/ISSUE_03_SUMMARY.md`
32. Updated `Development_Plan_v2.md` (v2.3)

---

## 🎯 **Success Criteria**

| Criterion | Target | Verification Method |
|-----------|--------|---------------------|
| All AI Trader endpoints working | 9 endpoints | Postman/curl tests |
| All Trade endpoints working | 8 endpoints | Postman/curl tests |
| All Pattern endpoints working | 10 endpoints | Postman/curl tests |
| Configuration endpoints working | 4 endpoints | Postman/curl tests |
| Health/Status endpoints working | 3 endpoints | Postman/curl tests |
| WebSocket connections working | 3 channels | Integration test |
| Error handling comprehensive | All error types | Test error scenarios |
| Request validation working | All endpoints | Send invalid requests |
| All tests pass | 40+ tests | `./gradlew test` |
| Build succeeds | Clean build | `./gradlew build` |
| CI pipeline passes | GitHub Actions | Green checkmark |
| API responds < 100ms | All endpoints | Performance check |
| 3-trader limit enforced | POST /traders | Try creating 4th |
| JSON serialization works | All responses | Check response format |
| CORS configured | Local dev | Browser console |

---

## 📊 **Estimated Code Statistics**

| Metric | Estimated Value |
|--------|----------------|
| **New Files** | ~30 files |
| **Lines of Code** | ~3,000+ lines |
| **DTO Classes** | 10+ |
| **API Endpoints** | 34 endpoints |
| **WebSocket Channels** | 3 channels |
| **Exception Classes** | 4+ |
| **Mapper Functions** | 20+ |
| **Test Cases** | 40+ tests |
| **Test Coverage** | 80%+ target |

---

## 🔧 **Key Technologies**

| Technology | Version | Purpose |
|------------|---------|---------|
| Ktor Server | 2.3.7 | HTTP server framework |
| Ktor Netty | 2.3.7 | Server engine |
| Ktor Content Negotiation | 2.3.7 | JSON handling |
| Kotlinx Serialization | 1.6.0 | JSON serialization |
| Ktor WebSockets | 2.3.7 | Real-time updates |
| Ktor CORS | 2.3.7 | Cross-origin requests |
| Ktor Test Host | 2.3.7 | Testing framework |

---

## 🏗️ **Architecture**

```
┌─────────────────────────────────────────────────────────┐
│                    Desktop UI Client                     │
│              (Will connect in Phase 4)                   │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP/WebSocket
                     ▼
┌─────────────────────────────────────────────────────────┐
│               Ktor REST API Server                       │
│                  (This Issue)                            │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐        │
│  │  Trader    │  │   Trade    │  │  Pattern   │        │
│  │  Routes    │  │   Routes   │  │  Routes    │        │
│  └────────────┘  └────────────┘  └────────────┘        │
│                                                           │
│  ┌──────────────────────────────────────────┐           │
│  │      WebSocket Manager (Real-time)       │           │
│  └──────────────────────────────────────────┘           │
│                                                           │
│  ┌──────────────────────────────────────────┐           │
│  │      Exception Handler & Validation       │           │
│  └──────────────────────────────────────────┘           │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│            Repositories (Issue #2)                       │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐        │
│  │  AITrader  │  │   Trade    │  │  Pattern   │        │
│  │ Repository │  │ Repository │  │ Repository │        │
│  └────────────┘  └────────────┘  └────────────┘        │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│             SQLite Database (Issue #2)                   │
└─────────────────────────────────────────────────────────┘
```

---

## 📋 **API Endpoints Summary**

### **AI Traders (9 endpoints)**
```
GET    /api/v1/traders              - List all
GET    /api/v1/traders/{id}         - Get by ID
POST   /api/v1/traders              - Create
PUT    /api/v1/traders/{id}         - Update config
PATCH  /api/v1/traders/{id}/status  - Update status
PATCH  /api/v1/traders/{id}/balance - Update balance
DELETE /api/v1/traders/{id}         - Delete
GET    /api/v1/traders/active       - List active
GET    /api/v1/traders/can-create   - Check limit
```

### **Trades (8 endpoints)**
```
GET    /api/v1/trades                    - List all
GET    /api/v1/trades/{id}               - Get by ID
POST   /api/v1/trades                    - Create (entry)
PATCH  /api/v1/trades/{id}/close         - Close (exit)
PATCH  /api/v1/trades/{id}/stop-loss     - Update SL
GET    /api/v1/trades/open               - List open
GET    /api/v1/trades/trader/{traderId}  - By trader
GET    /api/v1/trades/statistics/{id}    - Statistics
```

### **Patterns (10 endpoints)**
```
GET    /api/v1/patterns                 - List all
GET    /api/v1/patterns/{id}            - Get by ID
POST   /api/v1/patterns                 - Create
PATCH  /api/v1/patterns/{id}/statistics - Update stats
GET    /api/v1/patterns/active          - List active
GET    /api/v1/patterns/match           - Find matching
GET    /api/v1/patterns/top             - Top performers
PATCH  /api/v1/patterns/{id}/activate   - Activate
PATCH  /api/v1/patterns/{id}/deactivate - Deactivate
DELETE /api/v1/patterns/{id}            - Delete
```

### **Configuration (4 endpoints)**
```
GET    /api/v1/config                 - List all
GET    /api/v1/config/{key}           - Get by key
PUT    /api/v1/config/{key}           - Update
GET    /api/v1/config/category/{cat}  - By category
```

### **Health & Status (3 endpoints)**
```
GET    /api/health   - Health check
GET    /api/status   - System status
GET    /api/version  - API version
```

### **WebSocket (3 channels)**
```
WS     /ws/traders   - Trader updates
WS     /ws/trades    - Trade updates
WS     /ws/market    - Market data (placeholder)
```

---

## 🎓 **Key Implementation Details**

### **1. DTO Mapping Strategy**
```kotlin
// Entity (DB) -> DTO (API)
fun AITrader.toDTO() = AITraderDTO(
    id = this.id,
    name = this.name,
    status = this.status,
    // ... all fields
)

// DTO (API) -> Entity (DB)
fun AITraderDTO.toEntity() = AITrader(
    id = this.id,
    name = this.name,
    // ... all fields
)
```

### **2. Error Response Format**
```json
{
  "success": false,
  "error": {
    "code": "MAX_TRADERS_REACHED",
    "message": "Maximum of 3 AI traders already created",
    "details": {
      "currentCount": 3,
      "maxAllowed": 3
    }
  },
  "timestamp": "2025-10-24T10:30:00Z"
}
```

### **3. WebSocket Message Format**
```json
{
  "type": "TRADER_STATUS_UPDATE",
  "data": {
    "traderId": 1,
    "status": "ACTIVE",
    "timestamp": "2025-10-24T10:30:00Z"
  }
}
```

### **4. 3-Trader Limit Enforcement**
```kotlin
post("/api/v1/traders") {
    val request = call.receive<CreateAITraderRequest>()
    
    if (!aiTraderRepository.canCreateMore()) {
        throw MaxLimitReachedException("Maximum of 3 AI traders reached")
    }
    
    val traderId = aiTraderRepository.create(...)
    // ...
}
```

---

## 🔗 **Related Issues**

- **Depends On**: 
  - Issue #1 (Gradle Multi-Module Setup) ✅
  - Issue #2 (Database Layer) ✅
- **Blocks**: 
  - Issue #5 (Shared Data Models) - Will create DTOs here
  - Phase 4 (Desktop UI) - UI will consume this API
- **Related**: 
  - Issue #4 (Logging Infrastructure)
  - Issue #6 (Dependency Injection)

---

## 📚 **References**

- Ktor Documentation: https://ktor.io/docs/
- Ktor Server: https://ktor.io/docs/server.html
- Ktor Routing: https://ktor.io/docs/routing-in-ktor.html
- Ktor WebSockets: https://ktor.io/docs/websocket.html
- Kotlinx Serialization: https://github.com/Kotlin/kotlinx.serialization
- REST API Best Practices: https://restfulapi.net/

---

## ⚠️ **Known Challenges**

1. **Coroutine Context**: Ensure proper coroutine context for database calls
2. **Error Handling**: Comprehensive error mapping from repository to HTTP
3. **WebSocket State**: Managing WebSocket connections and subscriptions
4. **Validation**: Input validation for all request bodies
5. **Testing**: Mocking WebSocket connections in tests
6. **CORS**: Proper configuration for local development and production

---

## 🎯 **Definition of Done**

- [ ] All 34 API endpoints implemented and working
- [ ] All 3 WebSocket channels functional
- [ ] Request/response DTOs created with serialization
- [ ] Error handling covers all scenarios
- [ ] All 40+ tests written and passing
- [ ] API responds in < 100ms for simple queries
- [ ] Documentation complete (API_DOCUMENTATION.md)
- [ ] Build successful (`./gradlew build`)
- [ ] CI pipeline passes
- [ ] Code committed and pushed to GitHub
- [ ] Issue #3 marked complete in Development Plan
- [ ] Summary document created (ISSUE_03_SUMMARY.md)

---

**Status**: 📋 **READY TO START**  
**Next Step**: Begin Phase 1 - Ktor Server Setup  
**Estimated Completion**: October 24, 2025 (EOD)

