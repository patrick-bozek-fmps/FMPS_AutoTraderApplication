# Issue #4: Logging Infrastructure Setup

**Status**: 📋 **PLANNED**  
**Assigned**: AI Assistant  
**Priority**: P0 (Critical - Foundation)  
**Epic**: Foundation & Infrastructure (Phase 1)  
**Dependencies**: Issue #1 (Gradle) ✅, Issue #2 (Database Layer) ✅, Issue #3 (REST API) ✅

---

## 📋 **Objective**

Implement a comprehensive logging infrastructure using SLF4J and Logback that provides structured logging, file rotation, log levels per component, performance metrics, and audit trails. This will be the foundation for monitoring, debugging, and operational visibility across all system components.

---

## 📝 **Tasks**

### **Phase 1: Logback Configuration** 
- [ ] Create `logback.xml` configuration file
- [ ] Configure console appender with colored output
- [ ] Configure file appenders:
  - [ ] Main application log (`application.log`)
  - [ ] Error-only log (`error.log`)
  - [ ] API request log (`api-requests.log`)
  - [ ] Trade execution log (`trades.log`)
  - [ ] Performance metrics log (`metrics.log`)
  - [ ] Audit log (`audit.log`)
- [ ] Set up log rotation policy:
  - [ ] Daily rotation
  - [ ] Size-based rotation (100MB per file)
  - [ ] Keep last 30 days
  - [ ] Compress old logs (.gz)
- [ ] Configure log patterns with timestamps, thread, level, logger
- [ ] Set up async logging for performance

### **Phase 2: Logger Utility Classes**
- [ ] Create `LoggerFactory` wrapper
- [ ] Create `StructuredLogger` class for JSON logging
- [ ] Create `AuditLogger` for security and business events
- [ ] Create `PerformanceLogger` for metrics and timings
- [ ] Create `TradeLogger` for trade-specific events
- [ ] Create `ApiLogger` for request/response logging
- [ ] Add MDC (Mapped Diagnostic Context) support:
  - [ ] Request ID tracking
  - [ ] User/Trader ID tracking
  - [ ] Session ID tracking
  - [ ] Correlation ID for distributed tracing

### **Phase 3: Log Levels Configuration**
- [ ] Define package-level log levels in `logback.xml`:
  - [ ] `com.fmps.autotrader` → INFO (default)
  - [ ] `com.fmps.autotrader.api` → DEBUG (API layer)
  - [ ] `com.fmps.autotrader.database` → INFO (database)
  - [ ] `com.fmps.autotrader.trading` → DEBUG (trading engine)
  - [ ] `io.ktor` → INFO (framework)
  - [ ] `Exposed` → WARN (ORM)
  - [ ] `org.flywaydb` → INFO (migrations)
  - [ ] `com.zaxxer.hikari` → WARN (connection pool)
- [ ] Create environment-specific configurations:
  - [ ] `logback-dev.xml` (verbose)
  - [ ] `logback-test.xml` (minimal)
  - [ ] `logback-prod.xml` (optimized)
- [ ] Add dynamic log level adjustment endpoint

### **Phase 4: Structured Logging Formats**
- [ ] Define JSON log format for machine parsing
- [ ] Create log event data classes:
  - [ ] `LogEvent` - Base structure
  - [ ] `ApiLogEvent` - API requests
  - [ ] `TradeLogEvent` - Trade executions
  - [ ] `ErrorLogEvent` - Exceptions and errors
  - [ ] `MetricLogEvent` - Performance metrics
  - [ ] `AuditLogEvent` - Audit trail
- [ ] Implement custom appenders if needed
- [ ] Add contextual metadata to all logs

### **Phase 5: Performance Metrics Collection**
- [ ] Create `MetricsCollector` class
- [ ] Implement metric types:
  - [ ] Counter metrics (requests, trades, errors)
  - [ ] Gauge metrics (active traders, open positions)
  - [ ] Timer metrics (request duration, trade execution time)
  - [ ] Histogram metrics (response sizes, latencies)
- [ ] Add metrics for:
  - [ ] API endpoint performance
  - [ ] Database query times
  - [ ] Trade execution times
  - [ ] Memory usage
  - [ ] Thread pool usage
- [ ] Create metrics export endpoints
- [ ] Add periodic metrics logging

### **Phase 6: Audit Trail System**
- [ ] Define auditable events:
  - [ ] AI Trader lifecycle (create, start, stop, delete)
  - [ ] Configuration changes
  - [ ] Trade executions
  - [ ] Risk limit breaches
  - [ ] System state changes
- [ ] Implement `AuditService` class
- [ ] Create audit log format (who, what, when, where)
- [ ] Add audit log persistence (database table)
- [ ] Create audit trail query API

### **Phase 7: Error Tracking & Alerting**
- [ ] Create `ErrorTracker` class
- [ ] Implement error categorization:
  - [ ] Critical errors (system failures)
  - [ ] Trade errors (execution failures)
  - [ ] API errors (request failures)
  - [ ] Integration errors (exchange connectivity)
- [ ] Add error rate monitoring
- [ ] Create alert thresholds
- [ ] Implement email/notification placeholders
- [ ] Add error recovery suggestions in logs

### **Phase 8: Integration with Existing Code**
- [ ] Update `Main.kt` with logging initialization
- [ ] Add logging to `DatabaseFactory`
- [ ] Add logging to all API routes:
  - [ ] Request/response logging
  - [ ] Error logging
  - [ ] Performance timing
- [ ] Add logging to repositories:
  - [ ] Query logging
  - [ ] Transaction logging
  - [ ] Error logging
- [ ] Add logging to `WebSocketManager`
- [ ] Replace `println` statements with proper logging

### **Phase 9: Testing & Validation**
- [ ] Create `LoggingTest` test class
- [ ] Test log file creation and rotation
- [ ] Test log level filtering
- [ ] Test async logging performance
- [ ] Test structured log parsing
- [ ] Test MDC context propagation
- [ ] Test metrics collection accuracy
- [ ] Verify no sensitive data in logs (passwords, API keys)
- [ ] Test log volume under load

### **Phase 10: Documentation & Commit**
- [ ] Create `LOGGING_GUIDE.md` with:
  - [ ] How to use loggers
  - [ ] Log levels guidelines
  - [ ] Structured logging examples
  - [ ] Troubleshooting tips
  - [ ] Log file locations
- [ ] Document log formats and schemas
- [ ] Create log analysis examples
- [ ] Update `README.md` with logging section
- [ ] Update `Issue_04_Logging_Infrastructure.md`
- [ ] Update `Development_Plan_v2.md` to v2.4
- [ ] Commit all changes
- [ ] Push and verify CI passes

---

## 📦 **Deliverables**

### **Configuration Files**
1. `core-service/src/main/resources/logback.xml`
2. `core-service/src/main/resources/logback-dev.xml`
3. `core-service/src/main/resources/logback-test.xml`
4. `core-service/src/main/resources/logback-prod.xml`

### **Logger Utility Classes**
5. `core-service/src/main/kotlin/.../logging/LoggerFactory.kt`
6. `core-service/src/main/kotlin/.../logging/StructuredLogger.kt`
7. `core-service/src/main/kotlin/.../logging/AuditLogger.kt`
8. `core-service/src/main/kotlin/.../logging/PerformanceLogger.kt`
9. `core-service/src/main/kotlin/.../logging/TradeLogger.kt`
10. `core-service/src/main/kotlin/.../logging/ApiLogger.kt`
11. `core-service/src/main/kotlin/.../logging/ErrorTracker.kt`

### **Metrics & Monitoring**
12. `core-service/src/main/kotlin/.../metrics/MetricsCollector.kt`
13. `core-service/src/main/kotlin/.../metrics/MetricTypes.kt`
14. `core-service/src/main/kotlin/.../metrics/MetricsService.kt`

### **Data Models**
15. `shared/src/main/kotlin/.../dto/logging/LogEvent.kt`
16. `shared/src/main/kotlin/.../dto/logging/ApiLogEvent.kt`
17. `shared/src/main/kotlin/.../dto/logging/TradeLogEvent.kt`
18. `shared/src/main/kotlin/.../dto/logging/MetricLogEvent.kt`
19. `shared/src/main/kotlin/.../dto/logging/AuditLogEvent.kt`

### **Database Migration**
20. `core-service/src/main/resources/db/migration/V2__Audit_log_table.sql`

### **Tests**
21. `core-service/src/test/kotlin/.../logging/LoggingTest.kt`
22. `core-service/src/test/kotlin/.../metrics/MetricsCollectorTest.kt`

### **Documentation**
23. `03_Development/Application_OnPremises/core-service/LOGGING_GUIDE.md`
24. Updated `README.md`
25. Updated `Development_Plan_v2.md` (v2.4)

---

## 🎯 **Success Criteria**

| Criterion | Target | Verification Method |
|-----------|--------|---------------------|
| All log files created correctly | 6 files | Check logs directory |
| Log rotation works | Daily + size-based | Run for 24h, verify rotation |
| Async logging performs well | <5ms overhead | Performance test |
| No sensitive data in logs | Zero leaks | Security audit |
| Structured logs parse correctly | JSON valid | Parse with `jq` tool |
| MDC context propagates | Request ID in all logs | Trace single request |
| Performance metrics accurate | ±1% accuracy | Compare with actual timings |
| Audit trail complete | All events logged | Verify completeness |
| All tests pass | 100% | `./gradlew test` |
| Build succeeds | Clean build | `./gradlew build` |
| CI pipeline passes | GitHub Actions | Green checkmark ✅ |
| No compilation warnings | Zero warnings | Build output |
| Documentation complete | All sections | Manual review |

---

## 📊 **Estimated Code Statistics**

| Metric | Estimated Value |
|--------|----------------|
| **New Files** | ~25 files |
| **Lines of Code** | ~2,000+ lines |
| **Configuration Files** | 4 Logback configs |
| **Logger Classes** | 7 |
| **Metrics Classes** | 3 |
| **Log Event DTOs** | 5 |
| **Test Cases** | 15+ tests |
| **Documentation** | 500+ lines |

---

## 🔧 **Key Technologies**

| Technology | Version | Purpose |
|------------|---------|---------|
| SLF4J API | 2.0.9 | Logging facade |
| Logback Classic | 1.4.11 | Logging implementation |
| Logback Jackson | 1.4.11 | JSON log format |
| Kotlin Extensions | - | Inline logger functions |

---

## 🏗️ **Architecture**

```
┌─────────────────────────────────────────────────────────┐
│                  Application Components                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │   API    │  │ Trading  │  │ Database │              │
│  │  Layer   │  │  Engine  │  │  Layer   │              │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘              │
│       │             │             │                      │
│       └─────────────┴─────────────┘                      │
│                     │                                     │
│                     ▼                                     │
│  ┌─────────────────────────────────────────────────────┐│
│  │            Logging Infrastructure                    ││
│  │  ┌──────────────────────────────────────────────┐  ││
│  │  │         SLF4J API (Facade)                   │  ││
│  │  └─────────────────┬────────────────────────────┘  ││
│  │                    │                                ││
│  │  ┌─────────────────┴────────────────────────────┐  ││
│  │  │         Logback Core Engine                   │  ││
│  │  │  • Log level filtering                        │  ││
│  │  │  • Async processing                           │  ││
│  │  │  • MDC context                                │  ││
│  │  └─────────────────┬────────────────────────────┘  ││
│  │                    │                                ││
│  │  ┌─────────┬───────┴────────┬──────────┬─────────┐││
│  │  │ Console │ File Appender  │ Metrics  │  Audit  │││
│  │  │Appender │ (Rolling)      │Appender  │Appender │││
│  │  └─────────┴────────────────┴──────────┴─────────┘││
│  └──────────────────────────────────────────────────────││
└────────────────────────┬─────────────────────────────────┘
                         │
         ┌───────────────┴───────────────┐
         │                               │
         ▼                               ▼
   Log Files                       Metrics System
   /logs/                          (Future: Prometheus)
   ├── application.log
   ├── error.log
   ├── api-requests.log
   ├── trades.log
   ├── metrics.log
   └── audit.log
```

---

## 📋 **Logging Standards**

### **Log Levels Usage**

```kotlin
// TRACE: Very detailed information (method entry/exit)
logger.trace("Entering calculateIndicator() with params: $params")

// DEBUG: Detailed information for debugging
logger.debug("Retrieved ${trades.size} trades for trader $traderId")

// INFO: Important business events
logger.info("AI Trader #$traderId started successfully")

// WARN: Potentially harmful situations
logger.warn("Trade execution delayed by ${delay}ms, retrying...")

// ERROR: Error events that might still allow the application to continue
logger.error("Failed to connect to Binance API", exception)
```

### **Structured Logging Example**

```kotlin
// Traditional logging
logger.info("Trade executed: id=123, pair=BTC/USDT, side=LONG, price=50000")

// Structured logging (better for parsing/analysis)
logger.info(
    "Trade executed",
    mapOf(
        "tradeId" to 123,
        "tradingPair" to "BTC/USDT",
        "side" to "LONG",
        "entryPrice" to 50000,
        "timestamp" to Instant.now()
    )
)
```

### **Performance Logging Example**

```kotlin
val startTime = System.nanoTime()
try {
    // Execute operation
    tradeRepository.create(trade)
} finally {
    val duration = (System.nanoTime() - startTime) / 1_000_000.0
    performanceLogger.logMetric("trade.create.duration", duration)
}
```

---

## 🎓 **Key Implementation Patterns**

### **1. Logger per Class**
```kotlin
class AITraderService {
    private val logger = LoggerFactory.getLogger<AITraderService>()
    
    fun startTrader(id: Int) {
        logger.info("Starting AI Trader #$id")
        // ...
    }
}
```

### **2. MDC Context**
```kotlin
// In API request handler
MDC.put("requestId", UUID.randomUUID().toString())
MDC.put("traderId", traderId.toString())
try {
    // All logs in this context will include requestId and traderId
    processRequest()
} finally {
    MDC.clear()
}
```

### **3. Audit Trail**
```kotlin
auditLogger.log(
    event = "TRADER_CREATED",
    userId = "admin",
    traderId = newTrader.id,
    details = "Trader '${newTrader.name}' created with leverage ${newTrader.leverage}",
    severity = AuditSeverity.INFO
)
```

### **4. Error Context**
```kotlin
try {
    executeTrade(trade)
} catch (e: Exception) {
    logger.error(
        "Trade execution failed",
        mapOf(
            "tradeId" to trade.id,
            "exchange" to trade.exchange,
            "errorType" to e::class.simpleName
        ),
        e
    )
    throw e
}
```

---

## 🔗 **Related Issues**

- **Depends On**: 
  - Issue #1 (Gradle Multi-Module Setup) ✅
  - Issue #2 (Database Layer) ✅
  - Issue #3 (REST API Server) ✅
- **Blocks**: 
  - Issue #5 (Exchange Integration) - Needs logging for API calls
  - Issue #6 (Trading Engine) - Needs logging for trade decisions
  - All future issues - Logging is foundational
- **Related**: 
  - Issue #7 (Monitoring Dashboard) - Will consume logs
  - Issue #8 (Performance Optimization) - Uses metrics

---

## 📚 **References**

- SLF4J Documentation: https://www.slf4j.org/manual.html
- Logback Manual: https://logback.qos.ch/manual/
- Logback Configuration: https://logback.qos.ch/manual/configuration.html
- MDC (Mapped Diagnostic Context): https://logback.qos.ch/manual/mdc.html
- Async Logging: https://logback.qos.ch/manual/appenders.html#AsyncAppender
- Structured Logging Best Practices: https://www.loggly.com/blog/best-practices-for-structured-logging/

---

## ⚠️ **Important Considerations**

### **Security**
1. **Never log sensitive data**:
   - API keys, passwords, tokens
   - Credit card numbers
   - Personal identifiable information (PII)
   - Session IDs in plain text
2. **Sanitize user inputs** in logs
3. **Restrict log file permissions**
4. **Encrypt audit logs** if storing sensitive events

### **Performance**
1. Use **async appenders** for high-throughput logging
2. Avoid **expensive operations** in log statements
3. Use **conditional logging** for DEBUG/TRACE:
   ```kotlin
   if (logger.isDebugEnabled) {
       logger.debug("Expensive computation: ${computeExpensiveValue()}")
   }
   ```
4. Configure **buffer sizes** appropriately
5. Monitor **log file disk usage**

### **Operations**
1. **Centralize logs** for multi-instance deployments
2. Set up **log aggregation** (future: ELK stack)
3. Configure **alerts** for ERROR logs
4. Implement **log retention** policies
5. Test **log rotation** thoroughly

---

## 🎯 **Definition of Done**

- [ ] Logback configuration files created (4 environments)
- [ ] All logger utility classes implemented
- [ ] Metrics collection system working
- [ ] Audit trail system operational
- [ ] Integration with existing code complete
- [ ] All `println` statements replaced with logging
- [ ] No sensitive data logged (verified)
- [ ] Log rotation working correctly
- [ ] Async logging configured
- [ ] MDC context propagating correctly
- [ ] All tests written and passing (15+ tests)
- [ ] Performance impact < 5ms per log statement
- [ ] Documentation complete (LOGGING_GUIDE.md)
- [ ] Build successful (`./gradlew build`)
- [ ] CI pipeline passes (GitHub Actions)
- [ ] Code committed and pushed
- [ ] Issue #4 marked complete in Development Plan

---

## 📦 **Commit Strategy**

### **Commit 1: Logback Configuration**
- Add logback.xml files
- Configure file appenders
- Set up log rotation

### **Commit 2: Logger Utilities**
- Create logger wrapper classes
- Add structured logging support
- Implement MDC context

### **Commit 3: Metrics & Audit**
- Add metrics collection
- Implement audit trail
- Create error tracking

### **Commit 4: Integration**
- Update Main.kt
- Add logging to API routes
- Add logging to repositories
- Add logging to database layer

### **Commit 5: Tests & Documentation**
- Write logging tests
- Create LOGGING_GUIDE.md
- Update README and Development Plan

---

**Estimated Duration**: 1-2 days  
**Complexity**: Medium  
**Risk Level**: Low (non-breaking changes)

**Ready to Start**: ✅ All dependencies complete

---

**Status**: 📋 **PLANNED** - Ready for implementation  
**Next Action**: Review plan, approve, and begin Phase 1


