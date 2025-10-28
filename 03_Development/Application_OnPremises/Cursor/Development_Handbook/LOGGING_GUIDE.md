# Logging Guide - FMPS AutoTrader Application

**Version**: 1.0  
**Last Updated**: October 28, 2025  
**Author**: Development Team

---

## 📋 **Table of Contents**

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Configuration](#configuration)
4. [Using Logging in Code](#using-logging-in-code)
5. [MDC (Mapped Diagnostic Context)](#mdc-mapped-diagnostic-context)
6. [Performance Metrics](#performance-metrics)
7. [Log Levels](#log-levels)
8. [Best Practices](#best-practices)
9. [Troubleshooting](#troubleshooting)

---

## 🎯 **Overview**

The FMPS AutoTrader application uses a comprehensive logging infrastructure built on:
- **SLF4J** - Logging facade
- **Logback** - Logging implementation
- **Kotlin Logging** - Kotlin-friendly logging wrapper

### Key Features

✅ **Environment-Specific Configuration** (dev, test, prod)  
✅ **Structured Logging** (JSON in production)  
✅ **MDC Support** for request tracing  
✅ **Performance Metrics** logging  
✅ **Automatic Log Rotation**  
✅ **Separate Error Logs**  
✅ **Async Appenders** for performance

---

## 🏗️ **Architecture**

### Log Files

| File | Purpose | Rotation | Retention |
|------|---------|----------|-----------|
| `autotrader.log` | All application logs | 10MB/day | 7 days |
| `autotrader-error.log` | Error logs only | 10MB/day | 30 days |
| `autotrader-metrics.log` | Performance metrics | Daily | 30 days |

### Environments

| Environment | Config File | Log Level | Format | Output |
|-------------|-------------|-----------|--------|--------|
| **Development** | `logback-dev.xml` | DEBUG | Colored text | Console + File |
| **Test** | `logback-test.xml` | WARN | Plain text | Console only |
| **Production** | `logback-prod.xml` | INFO | JSON | Console + File |

---

## ⚙️ **Configuration**

### Selecting Environment

Set the `logback.configurationFile` system property:

```bash
# Development (default)
java -jar app.jar

# Production
java -Dlogback.configurationFile=logback-prod.xml -jar app.jar

# Testing
java -Dlogback.configurationFile=logback-test.xml -jar app.jar
```

### Custom Log Directory

```bash
java -DLOG_DIR=/var/log/autotrader -jar app.jar
```

### Changing Log Levels at Runtime

Modify `logback.xml` and the configuration will be reloaded automatically (scan period: 30 seconds).

---

## 💻 **Using Logging in Code**

### Basic Logging

```kotlin
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class MyService {
    fun doSomething() {
        logger.info { "Starting operation" }
        logger.debug { "Debug details: value=$value" }
        logger.warn { "Warning message" }
        logger.error(exception) { "Error occurred" }
    }
}
```

### Lazy Evaluation

Use lambda syntax for lazy evaluation (message only computed if level is enabled):

```kotlin
// ✅ Good - lazy evaluation
logger.debug { "Expensive calculation: ${expensiveOperation()}" }

// ❌ Bad - always evaluated
logger.debug("Expensive calculation: ${expensiveOperation()}")
```

### Logging with Exceptions

```kotlin
try {
    riskyOperation()
} catch (e: Exception) {
    logger.error(e) { "Operation failed for user $userId" }
}
```

---

## 🔍 **MDC (Mapped Diagnostic Context)**

MDC adds contextual information to all log messages within a scope.

### Setting MDC Values

```kotlin
import com.fmps.autotrader.core.logging.LoggingContext

// Set individual values
LoggingContext.setRequestId("req-123")
LoggingContext.setUserId("user-456")
LoggingContext.setTraderId("trader-789")

logger.info { "Processing request" }
// Output: ... [req-123] [user-456] [trader-789] - Processing request

// Clear when done
LoggingContext.clear()
```

### Using with Context Blocks

```kotlin
import com.fmps.autotrader.core.logging.withLoggingContext

withLoggingContext(
    requestId = "req-123",
    userId = "user-456",
    traderId = "trader-789"
) {
    logger.info { "All logs here will include context" }
    processRequest()
    logger.info { "Still has context" }
}
// Context automatically cleaned up
```

### Convenience Functions

```kotlin
// Auto-generate request ID
withRequestId {
    logger.info { "Processing request" }
}

// Trader-specific context
withTraderContext("trader-123") {
    logger.info { "Executing trade" }
}
```

### Available MDC Keys

- `requestId` - Unique request identifier
- `userId` - User identifier
- `traderId` - AI Trader identifier
- `sessionId` - Session identifier
- `correlationId` - Cross-service correlation ID
- `operation` - Current operation name

---

## 📊 **Performance Metrics**

Use `MetricsLogger` for structured metrics logging.

### Logging Performance

```kotlin
import com.fmps.autotrader.core.logging.MetricsLogger

// Manual timing
MetricsLogger.logPerformance(
    operation = "process_trade",
    durationMs = 150L,
    additionalInfo = mapOf("trader_id" to "trader-123")
)

// Automatic timing
val result = measureAndLog("database_query") {
    repository.findAll()
}
```

### Business Metrics

```kotlin
// Trade execution
MetricsLogger.logTradeExecution(
    traderId = "trader-123",
    action = "BUY",
    symbol = "BTC/USDT",
    amount = 0.5,
    price = 50000.0
)

// Position P&L
MetricsLogger.logPositionPnL(
    traderId = "trader-123",
    positionId = "pos-456",
    pnl = 1500.0,
    pnlPercent = 15.5
)

// Status changes
MetricsLogger.logTraderStatusChange(
    traderId = "trader-123",
    previousStatus = "ACTIVE",
    newStatus = "PAUSED"
)
```

### Database Queries

```kotlin
// Automatic query timing + row count
val users = measureDatabaseQuery("find_all_users") {
    userRepository.findAll()
}
```

### Exchange API Calls

```kotlin
val balance = measureExchangeApiCall("Binance", "get_balance") {
    binanceApi.getBalance()
}
```

---

## 📈 **Log Levels**

### When to Use Each Level

| Level | Use Case | Example |
|-------|----------|---------|
| **TRACE** | Very detailed debugging | Method entry/exit with parameters |
| **DEBUG** | Debugging information | Variable values, conditional branches |
| **INFO** | General information | Service started, request received |
| **WARN** | Warning conditions | Deprecated API used, retry attempted |
| **ERROR** | Error conditions | Exceptions, failed operations |

### Package-Specific Levels

Configure in `logback.xml`:

```xml
<!-- Application code -->
<logger name="com.fmps.autotrader" level="DEBUG" />

<!-- Database -->
<logger name="Exposed" level="INFO" />

<!-- Ktor -->
<logger name="io.ktor" level="INFO" />
```

---

## ✨ **Best Practices**

### DO ✅

```kotlin
// Use structured logging with key-value pairs
logger.info { "Trade executed: traderId=$traderId, symbol=$symbol, amount=$amount" }

// Use MDC for contextual information
withLoggingContext(traderId = traderId) {
    logger.info { "Processing trade" }
}

// Log exceptions with context
logger.error(e) { "Failed to process trade for trader $traderId" }

// Use lazy evaluation for expensive operations
logger.debug { "State: ${computeExpensiveState()}" }

// Log at appropriate boundaries (API entry/exit, service methods)
logger.info { "REST API: POST /api/traders - Creating new trader" }
```

### DON'T ❌

```kotlin
// Don't log sensitive information
logger.info { "API Key: $apiKey" }  // ❌

// Don't log in loops (causes performance issues)
for (item in items) {
    logger.debug { "Processing $item" }  // ❌
}

// Don't use string concatenation (not lazy)
logger.debug("Value: " + expensiveOperation())  // ❌

// Don't swallow exceptions silently
try {
    operation()
} catch (e: Exception) {
    // ❌ No logging!
}

// Don't log too much in production
logger.debug { "..." }  // Won't appear in prod (INFO level)
```

### Structured Logging Format

Use consistent key-value format:

```kotlin
logger.info { "operation=place_order, trader_id=$traderId, symbol=$symbol, result=success" }
```

---

## 🔧 **Troubleshooting**

### Logs Not Appearing

**Problem**: No log output

**Solutions**:
- Check log level configuration
- Verify logback configuration file is loaded
- Check file permissions on log directory
- Look for Logback initialization errors in stderr

### Log Files Growing Too Large

**Problem**: Disk space issues

**Solutions**:
- Reduce `maxHistory` in logback.xml
- Decrease `maxFileSize`
- Lower `totalSizeCap`
- Increase log level (e.g., DEBUG → INFO)

### Performance Issues

**Problem**: Logging causing slowdowns

**Solutions**:
- Use async appenders (already configured)
- Avoid logging in hot paths/loops
- Use appropriate log levels
- Enable lazy evaluation (lambda syntax)

### Finding Specific Requests

**Problem**: Need to trace a specific request

**Solutions**:
```bash
# Search by request ID
grep "req-123" logs/autotrader.log

# Search by user ID
grep "\[user-456\]" logs/autotrader.log

# Search by trader ID
grep "\[trader-789\]" logs/autotrader.log
```

### JSON Log Parsing (Production)

```bash
# Pretty-print JSON logs
cat logs/autotrader.json | jq '.'

# Filter by level
cat logs/autotrader.json | jq 'select(.level == "ERROR")'

# Filter by time range
cat logs/autotrader.json | jq 'select(.timestamp > "2025-10-28T10:00:00")'

# Extract specific fields
cat logs/autotrader.json | jq '{time: .timestamp, level: .level, message: .message}'
```

---

## 📚 **Additional Resources**

- [SLF4J Documentation](http://www.slf4j.org/manual.html)
- [Logback Documentation](https://logback.qos.ch/manual/)
- [Kotlin Logging](https://github.com/MicroUtils/kotlin-logging)
- [MDC Best Practices](https://www.baeldung.com/mdc-in-log4j-2-logback)

---

## 🆘 **Support**

For logging-related issues:
1. Check this guide first
2. Review logback configuration files
3. Check application logs for Logback initialization errors
4. Contact the development team

---

**Last Updated**: October 28, 2025  
**Version**: 1.0  
**Maintained by**: FMPS AutoTrader Development Team

