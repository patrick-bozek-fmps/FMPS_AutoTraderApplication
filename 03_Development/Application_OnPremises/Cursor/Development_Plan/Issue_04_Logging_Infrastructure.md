# Issue #4: Implement Logging Infrastructure

**Status**: ⚠️ **NOT STARTED** (File structure created, implementation pending)  
**Assigned**: TBD  
**Created**: October 24, 2025  
**Started**: Not Started  
**Completed**: Not Completed  
**Duration**: ~1 day (estimated)  
**Epic**: Foundation & Infrastructure (Epic 1)  
**Priority**: P1 (High)  
**Dependencies**: Issue #1 (Gradle) ✅

> **NOTE**: Empty placeholder files were created (logback.xml, logback-dev.xml, logback-prod.xml, logback-test.xml, LoggingContext.kt, MetricsLogger.kt) but contain no implementation. This issue requires full implementation before it can be considered complete.

---

## 📋 **Objective**

Implement a comprehensive logging infrastructure using SLF4J and Logback to provide structured, configurable, and production-ready logging capabilities across all application modules. The system will support console and file logging with rotation, configurable log levels, MDC for request tracing, and performance metrics logging.

---

## 🎯 **Goals**

1. **Configure** SLF4J as the logging facade for the entire application
2. **Implement** Logback as the logging backend with XML configuration
3. **Set up** file-based logging with automatic rotation
4. **Configure** console logging with colored output for development
5. **Add** structured JSON logging for production environments
6. **Implement** MDC (Mapped Diagnostic Context) for request tracing
7. **Create** performance metrics logging capabilities
8. **Document** logging standards and best practices

---

## 📝 **Tasks Breakdown**

### **Task 1: Dependency Configuration** ⏳ PENDING
- [ ] Verify SLF4J API dependency in all modules
- [ ] Add Logback Classic dependency to core-service
- [ ] Add Logback dependencies to desktop-ui
- [ ] Add Logback JSON encoder for structured logging
- [ ] Configure SLF4J bridging for third-party libraries
- [ ] Add logstash-logback-encoder for JSON output
- [ ] Update build.gradle.kts files

### **Task 2: Logback Configuration** ⏳ PENDING
- [ ] Create `core-service/src/main/resources/logback.xml`
- [ ] Configure root logger level (INFO default)
- [ ] Set up console appender:
  - [ ] Add colored pattern layout
  - [ ] Configure timestamp format
  - [ ] Add thread name
  - [ ] Add logger name with abbreviation
  - [ ] Add log level with color coding
- [ ] Set up file appender:
  - [ ] Configure file path: `logs/autotrader.log`
  - [ ] Set up rolling policy (time + size based)
  - [ ] Configure max file size (10MB)
  - [ ] Set max history (7 days)
  - [ ] Set total size cap (1GB)
  - [ ] Add pattern with full context
- [ ] Create separate error log file:
  - [ ] Path: `logs/autotrader-error.log`
  - [ ] Filter: ERROR level only
  - [ ] Same rotation policy
- [ ] Configure async appenders for performance

### **Task 3: Environment-Specific Configuration** ⏳ PENDING
- [ ] Create `logback-dev.xml` for development:
  - [ ] DEBUG level for application packages
  - [ ] Colored console output
  - [ ] Minimal file logging
- [ ] Create `logback-prod.xml` for production:
  - [ ] INFO level default
  - [ ] JSON structured logging
  - [ ] Comprehensive file logging
  - [ ] Performance optimizations
- [ ] Create `logback-test.xml` for testing:
  - [ ] WARN level to reduce test noise
  - [ ] Console output only
  - [ ] No file appenders
- [ ] Configure environment selection via system property

### **Task 4: Structured Logging** ⏳ PENDING
- [ ] Configure JSON encoder for production logs
- [ ] Define JSON log format:
  - [ ] timestamp (ISO 8601)
  - [ ] level
  - [ ] thread
  - [ ] logger
  - [ ] message
  - [ ] exception (if present)
  - [ ] mdc (context)
  - [ ] markers
- [ ] Add correlation ID to MDC
- [ ] Add user ID to MDC (when available)
- [ ] Add request ID to MDC
- [ ] Configure JSON pretty-print for dev

### **Task 5: MDC Implementation** ⏳ PENDING
- [ ] Create `LoggingContext.kt` utility class
- [ ] Implement `withContext()` extension function
- [ ] Add `generateRequestId()` helper
- [ ] Implement MDC for HTTP requests:
  - [ ] requestId
  - [ ] method
  - [ ] path
  - [ ] remoteAddress
  - [ ] userAgent
- [ ] Implement MDC for AI Trader operations:
  - [ ] traderId
  - [ ] traderName
  - [ ] exchange
  - [ ] tradingPair
- [ ] Implement MDC for trade operations:
  - [ ] tradeId
  - [ ] symbol
  - [ ] side (LONG/SHORT)
- [ ] Add MDC cleanup in finally blocks

### **Task 6: Logger Wrapper (Optional)** ⏳ PENDING
- [ ] Create `AppLogger.kt` facade class
- [ ] Implement convenience methods:
  - [ ] `trace()`, `debug()`, `info()`, `warn()`, `error()`
  - [ ] Methods with context parameters
  - [ ] Methods with exception logging
- [ ] Add performance logging helpers:
  - [ ] `measureTime()` inline function
  - [ ] `logSlowOperation()` (threshold-based)
- [ ] Add structured logging helpers:
  - [ ] `logWithContext()`
  - [ ] `logMetric()`
  - [ ] `logEvent()`
- [ ] Create logger factory method

### **Task 7: Package-Specific Logging** ⏳ PENDING
- [ ] Configure logging levels per package:
  - [ ] `com.fmps.autotrader.core` → INFO
  - [ ] `com.fmps.autotrader.core.database` → DEBUG (dev), INFO (prod)
  - [ ] `com.fmps.autotrader.core.api` → DEBUG (dev), INFO (prod)
  - [ ] `com.fmps.autotrader.connectors` → INFO
  - [ ] `io.ktor` → INFO
  - [ ] `org.jetbrains.exposed` → WARN
  - [ ] `com.zaxxer.hikari` → INFO
  - [ ] `org.flywaydb` → INFO
- [ ] Add separate appender for security logs
- [ ] Add separate appender for audit logs

### **Task 8: Performance Metrics Logging** ⏳ PENDING
- [ ] Create `MetricsLogger.kt` class
- [ ] Implement method timing logging:
  - [ ] Database query times
  - [ ] API response times
  - [ ] Exchange API call times
- [ ] Add business metrics logging:
  - [ ] Trades executed count
  - [ ] Active traders count
  - [ ] Pattern matches found
- [ ] Add system metrics logging:
  - [ ] Memory usage
  - [ ] Thread pool stats
  - [ ] Database connection pool stats
- [ ] Configure metrics log file: `logs/metrics.log`
- [ ] Add metrics aggregation format

### **Task 9: Integration with Existing Code** ⏳ PENDING
- [ ] Update `Main.kt`:
  - [ ] Add startup logging
  - [ ] Log configuration loaded
  - [ ] Log database initialization
  - [ ] Log server startup
  - [ ] Log shutdown events
- [ ] Update `DatabaseFactory.kt`:
  - [ ] Log connection pool creation
  - [ ] Log migration execution
  - [ ] Log query execution times (slow query log)
- [ ] Update `AITraderRepository.kt`:
  - [ ] Log CRUD operations
  - [ ] Log validation failures
  - [ ] Log 3-trader limit checks
- [ ] Update API routes:
  - [ ] Log incoming requests
  - [ ] Log validation errors
  - [ ] Log responses with timing
- [ ] Add error logging with stack traces

### **Task 10: Testing** ⏳ PENDING
- [ ] Create `LoggingTest.kt`:
  - [ ] Test console appender works
  - [ ] Test file appender creates files
  - [ ] Test log rotation configuration
  - [ ] Test MDC context propagation
  - [ ] Test logger levels per package
- [ ] Create `LoggingContextTest.kt`:
  - [ ] Test MDC set/get/clear
  - [ ] Test context propagation in coroutines
  - [ ] Test requestId generation
- [ ] Create `MetricsLoggerTest.kt`:
  - [ ] Test timing measurements
  - [ ] Test metrics formatting
- [ ] Manual testing:
  - [ ] Generate various log levels
  - [ ] Verify file rotation after size limit
  - [ ] Check JSON format in production mode
  - [ ] Verify colored output in console

### **Task 11: Documentation** ⏳ PENDING
- [ ] Create `LOGGING_GUIDE.md`:
  - [ ] Logging standards and conventions
  - [ ] When to use each log level
  - [ ] How to add MDC context
  - [ ] How to log exceptions properly
  - [ ] Performance logging best practices
  - [ ] Structured logging examples
- [ ] Add KDoc to logging utilities
- [ ] Create example code snippets
- [ ] Document log file locations
- [ ] Document rotation policies
- [ ] Add troubleshooting section

### **Task 12: Build & Commit** ⏳ PENDING
- [ ] Run all tests: `./gradlew test`
- [ ] Build project: `./gradlew build`
- [ ] Fix any compilation errors
- [ ] Fix any test failures
- [ ] Verify log files created correctly
- [ ] Check log rotation works
- [ ] Commit changes with descriptive message
- [ ] Push to GitHub
- [ ] Verify CI pipeline passes
- [ ] Update Issue #4 document to reflect completion
- [ ] Update Development_Plan_v2.md

---

## 📦 **Deliverables**

### **Configuration Files**
1. `core-service/src/main/resources/logback.xml` - Main configuration
2. `core-service/src/main/resources/logback-dev.xml` - Development config
3. `core-service/src/main/resources/logback-prod.xml` - Production config
4. `core-service/src/main/resources/logback-test.xml` - Test config

### **Utility Classes**
5. `core-service/src/main/kotlin/.../logging/LoggingContext.kt` - MDC utilities
6. `core-service/src/main/kotlin/.../logging/AppLogger.kt` - Logger facade (optional)
7. `core-service/src/main/kotlin/.../logging/MetricsLogger.kt` - Performance metrics

### **Tests**
8. `core-service/src/test/kotlin/.../logging/LoggingTest.kt`
9. `core-service/src/test/kotlin/.../logging/LoggingContextTest.kt`
10. `core-service/src/test/kotlin/.../logging/MetricsLoggerTest.kt`

### **Documentation**
11. `LOGGING_GUIDE.md` - Comprehensive logging guide
12. Updated existing code files with logging statements

### **Log Files (Created at Runtime)**
13. `logs/autotrader.log` - Main application log
14. `logs/autotrader-error.log` - Error-level logs only
15. `logs/metrics.log` - Performance metrics
16. `logs/security.log` - Security events (optional)
17. `logs/audit.log` - Audit trail (optional)

---

## 🎯 **Success Criteria**

| Criterion | Target | Verification Method |
|-----------|--------|---------------------|
| Logback configured | Full setup | Config files exist |
| Console logging works | Colored output | Visual verification |
| File logging works | Files created | Check logs/ directory |
| Log rotation configured | 10MB, 7 days | Check policy in XML |
| JSON logging available | Production mode | Verify JSON format |
| MDC context works | Request tracing | Check correlation IDs |
| Performance logging | Timing metrics | Verify metrics.log |
| Package-level control | Per-package levels | Test log filtering |
| All tests pass | 10+ tests | `./gradlew test` |
| Build succeeds | Clean build | `./gradlew build` |
| CI pipeline passes | GitHub Actions | Green checkmark |
| Documentation complete | Logging guide | Doc review |
| No sensitive data logged | Security check | Code review |
| Async performance | < 5% overhead | Benchmark |

---

## 📊 **Estimated Code Statistics**

| Metric | Estimated Value |
|--------|----------------|
| **New Files** | ~10 files |
| **Configuration Files** | 4 (XML) |
| **Utility Classes** | 3 classes |
| **Lines of Code** | ~800+ lines |
| **Test Cases** | 10+ tests |
| **Test Coverage** | 80%+ target |
| **Documentation** | 500+ lines |

---

## 🔧 **Technologies & Dependencies**

| Technology | Version | Purpose |
|------------|---------|---------|
| SLF4J API | 2.0.9 | Logging facade |
| Logback Classic | 1.4.14 | Logging implementation |
| Logback Encoder | 7.4 | JSON encoding |
| Kotlin Coroutines | 1.7.3 | MDC propagation |

**Add to `build.gradle.kts`** (root level dependencies already include SLF4J):
```kotlin
dependencies {
    // Logging (SLF4J already configured)
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
}
```

---

## 🏗️ **Logging Architecture**

```
┌─────────────────────────────────────────────────────────┐
│              Application Code                            │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐        │
│  │  Main.kt   │  │   API      │  │ Repository │        │
│  │            │  │  Routes    │  │  Layer     │        │
│  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘        │
│        │                │                │               │
│        └────────────────┴────────────────┘               │
│                         │                                 │
│                         ▼                                 │
│            ┌───────────────────────┐                     │
│            │   SLF4J API (Facade)  │                     │
│            └───────────┬───────────┘                     │
└────────────────────────┼─────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                  Logback Core                            │
│  ┌──────────────────────────────────────────┐           │
│  │          Loggers & Filters               │           │
│  └────────┬─────────────────────────────────┘           │
│           │                                              │
│           ▼                                              │
│  ┌────────────────────────────────────┐                 │
│  │         Appenders                  │                 │
│  │  ┌──────────┐  ┌──────────┐       │                 │
│  │  │ Console  │  │  File    │       │                 │
│  │  │ Appender │  │ Appender │       │                 │
│  │  └──────────┘  └──────────┘       │                 │
│  │  ┌──────────┐  ┌──────────┐       │                 │
│  │  │  Error   │  │ Metrics  │       │                 │
│  │  │   File   │  │   File   │       │                 │
│  │  └──────────┘  └──────────┘       │                 │
│  └────────────────────────────────────┘                 │
│                                                          │
│  ┌──────────────────────────────────────────┐           │
│  │      Rolling Policy & Rotation           │           │
│  └──────────────────────────────────────────┘           │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
              ┌──────────────────┐
              │   Log Files      │
              │  - autotrader.log │
              │  - error.log      │
              │  - metrics.log    │
              └───────────────────┘
```

---

## 📋 **Logback Configuration Example**

### **Basic logback.xml Structure**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Console Appender with Color -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %highlight(%-5level) [%thread] %cyan(%logger{36}) - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Rolling File Appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/autotrader.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/autotrader-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>10MB</maxFileSize>
            <maxHistory>7</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Package-specific levels -->
    <logger name="com.fmps.autotrader" level="INFO"/>
    <logger name="io.ktor" level="INFO"/>
    <logger name="org.jetbrains.exposed" level="WARN"/>
    
    <!-- Root logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

---

## 📝 **Logging Standards**

### **Log Levels**
- **TRACE**: Very detailed debugging (e.g., loop iterations)
- **DEBUG**: Detailed debugging info (e.g., variable values)
- **INFO**: General informational messages (e.g., service started)
- **WARN**: Warning messages (e.g., deprecated API used)
- **ERROR**: Error events (e.g., exceptions, failures)

### **When to Use Each Level**
```kotlin
// TRACE - Extremely detailed
logger.trace("Processing trader ID: $traderId, iteration: $i")

// DEBUG - Detailed debugging
logger.debug("Fetching candlesticks: symbol=$symbol, interval=$interval, limit=$limit")

// INFO - General information
logger.info("AI Trader created successfully: id=$traderId, name=$name")

// WARN - Warnings
logger.warn("Rate limit approaching: ${requests}/1200 requests in last minute")

// ERROR - Errors
logger.error("Failed to connect to exchange: ${exchange.name}", exception)
```

### **MDC Usage**
```kotlin
// Set context
LoggingContext.withContext(
    "traderId" to traderId,
    "exchange" to exchange
) {
    logger.info("Executing trade")
    // Context automatically available in all logs within this block
}

// Manual MDC
MDC.put("requestId", requestId)
try {
    // Process request
} finally {
    MDC.remove("requestId")
}
```

---

## ⚠️ **Security Considerations**

### **Sensitive Data Protection**
1. **Never log**:
   - API keys or secrets
   - Passwords
   - Authentication tokens
   - Credit card numbers
   - Personal identification information

2. **Sanitize** before logging:
   - User email addresses (mask domain)
   - IP addresses (mask last octets if needed)
   - Account numbers (show last 4 digits only)

3. **Use placeholders**:
```kotlin
// BAD
logger.info("User logged in: ${user.email}, password: ${user.password}")

// GOOD
logger.info("User logged in: ${user.id}, email: ${maskEmail(user.email)}")
```

---

## 🎓 **Best Practices**

### **DO**
✅ Use appropriate log levels  
✅ Include context in log messages  
✅ Log exceptions with stack traces  
✅ Use structured logging (MDC)  
✅ Keep messages concise but informative  
✅ Log entry/exit of critical operations  
✅ Use lazy evaluation for expensive operations  

### **DON'T**
❌ Log sensitive information  
❌ Log in tight loops (use sampling)  
❌ Use `printStackTrace()`  
❌ Concatenate strings in log statements  
❌ Log without context  
❌ Ignore exceptions  
❌ Over-log or under-log  

### **Lazy Logging Example**
```kotlin
// BAD - Always evaluates
logger.debug("Complex calc: " + expensiveOperation())

// GOOD - Only evaluates if DEBUG enabled
logger.debug { "Complex calc: ${expensiveOperation()}" }
```

---

## 📈 **Definition of Done**

- [ ] All 12 tasks completed
- [ ] Logback.xml configurations created (4 files) and implemented
- [ ] Console logging with colors working
- [ ] File logging with rotation configured
- [ ] JSON logging for production available
- [ ] MDC context utilities implemented
- [ ] Performance metrics logging functional
- [ ] Package-specific log levels configured
- [ ] Logging integrated in Main.kt
- [ ] Logging integrated in DatabaseFactory
- [ ] Logging integrated in repositories
- [ ] Logging integrated in API routes
- [ ] 10+ tests written and passing
- [ ] LOGGING_GUIDE.md created
- [ ] Code review completed
- [ ] All tests pass: `./gradlew test`
- [ ] Build succeeds: `./gradlew build`
- [ ] CI pipeline passes (GitHub Actions)
- [ ] Log files created in logs/ directory
- [ ] Log rotation verified
- [ ] Issue #4 document updated to reflect completion
- [ ] Development_Plan_v2.md updated

---

## 🔄 **Dependencies**

### **Depends On** (Must be complete first)
- ✅ Issue #1: Gradle multi-module setup

### **Blocks** (Cannot start until this is done)
- None (logging is independent but beneficial for all subsequent issues)

### **Enhanced By**
- Issue #2: Database layer (add database logging)
- Issue #3: REST API (add request/response logging)

---

## 📚 **Resources**

### **Documentation**
- Logback Manual: https://logback.qos.ch/manual/
- SLF4J Documentation: https://www.slf4j.org/manual.html
- MDC Guide: https://logback.qos.ch/manual/mdc.html
- JSON Encoder: https://github.com/logfellow/logstash-logback-encoder

### **Examples**
- Logback Configuration: https://logback.qos.ch/manual/configuration.html
- Kotlin Logging Best Practices: https://github.com/MicroUtils/kotlin-logging

---

## 📅 **Estimated Timeline**

| Task | Estimated Time |
|-------|---------------|
| Task 1: Dependencies | 0.5 hours |
| Task 2: Logback Config | 1 hour |
| Task 3: Environment Configs | 1 hour |
| Task 4: Structured Logging | 1 hour |
| Task 5: MDC Implementation | 1.5 hours |
| Task 6: Logger Wrapper | 1 hour |
| Task 7: Package Logging | 0.5 hours |
| Task 8: Metrics Logging | 1.5 hours |
| Task 9: Integration | 2 hours |
| Task 10: Testing | 1.5 hours |
| Task 11: Documentation | 1 hour |
| Task 12: Build & Commit | 0.5 hours |
| **Total** | **~1 day (13 hours)** |

---

## 📦 **Commit Strategy**

Follow the development workflow from `DEVELOPMENT_WORKFLOW.md`:

1. **Commit after major phases** (config, integration, testing)
2. **Run tests before every commit**
3. **Wait for CI to pass** before proceeding
4. **Update documentation** as you go

Example commits:
```
feat: Add Logback configuration with file rotation (Issue #4 Tasks 1-2)
feat: Implement MDC and structured logging (Issue #4 Tasks 4-5)
feat: Integrate logging across all modules (Issue #4 Task 9)
feat: Complete Issue #4 - Logging infrastructure
```

---

**Issue Created**: October 24, 2025  
**Priority**: High (P1)  
**Estimated Effort**: 1 day  
**Status**: ⚠️ NOT STARTED - File structure created, awaiting implementation

---

**Next Steps**:
1. Review this plan with team/stakeholder
2. Begin Task 1: Verify and complete dependencies
3. Implement all logback configuration files with proper XML configuration
4. Implement LoggingContext.kt and MetricsLogger.kt utility classes
5. Follow DEVELOPMENT_WORKFLOW.md throughout
6. Integrate logging into existing code from Issues #1, #2, #3, #5


