# AI Trader Manager Guide

**Version**: 1.0  
**Last Updated**: November 6, 2025  
**Module**: `core-service/traders`  
**Issue**: #12 - AI Trader Manager

---

## 📋 **Overview**

The AI Trader Manager is responsible for managing the lifecycle of multiple AI Trader instances. It enforces the maximum 3 traders limit (v1.0 scope per ATP_ProdSpec_52), handles creation, starting, stopping, updating, and deletion of traders, and provides state persistence, recovery, and health monitoring capabilities.

### **Key Features**

- ✅ **Lifecycle Management**: Create, start, stop, update, and delete traders
- ✅ **Max Limit Enforcement**: Enforces maximum 3 traders (v1.0 scope)
- ✅ **State Persistence**: Saves trader state to database
- ✅ **Recovery on Restart**: Recovers traders from database on application restart
- ✅ **Health Monitoring**: Monitors trader health and detects issues
- ✅ **Resource Management**: Manages exchange connectors and resources
- ✅ **Thread-Safe**: All operations protected with Mutex

---

## 🎯 **Quick Start**

### **Basic Usage**

```kotlin
import com.fmps.autotrader.core.traders.*
import com.fmps.autotrader.core.connectors.*
import com.fmps.autotrader.core.database.repositories.*
import com.fmps.autotrader.shared.enums.*
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.time.Duration

// 1. Initialize dependencies
val repository = AITraderRepository()
val connectorFactory = ConnectorFactory.getInstance()
val manager = AITraderManager(repository, connectorFactory, maxTraders = 3)

// 2. Create a trader
val config = AITraderConfig(
    id = "trader-1",
    name = "My Trader",
    exchange = Exchange.BINANCE,
    symbol = "BTCUSDT",
    virtualMoney = true,
    maxStakeAmount = BigDecimal("1000.00"),
    maxRiskLevel = 5,
    maxTradingDuration = Duration.ofHours(24),
    minReturnPercent = 5.0,
    strategy = TradingStrategy.TREND_FOLLOWING,
    candlestickInterval = TimeFrame.ONE_HOUR
)

runBlocking {
    // Create trader
    val traderId = manager.createTrader(config).getOrThrow()
    println("Created trader: $traderId")
    
    // Start trader
    manager.startTrader(traderId).getOrThrow()
    println("Trader started")
    
    // Check health
    val health = manager.checkTraderHealth(traderId)
    println("Trader health: ${health?.isHealthy}")
    
    // Stop trader
    manager.stopTrader(traderId).getOrThrow()
    println("Trader stopped")
}
```

---

## 🏗️ **Architecture**

### **Class Structure**

```
AITraderManager
├── activeTraders: Map<String, AITrader>
├── tradersMutex: Mutex (thread-safety)
├── statePersistence: TraderStatePersistence
├── healthMonitor: HealthMonitor
└── connectorCache: Map<Exchange, IExchangeConnector>
```

### **Dependencies**

- **AITraderRepository**: Database operations for trader persistence
- **ConnectorFactory**: Creates exchange connectors
- **AITrader**: Individual trader instances (from Issue #11)

### **Thread Safety**

All operations are protected with `Mutex` to ensure thread-safe concurrent access:

```kotlin
suspend fun createTrader(config: AITraderConfig): Result<String> {
    return tradersMutex.withLock {
        // Thread-safe operations
    }
}
```

---

## 🔄 **Lifecycle Management**

### **Trader Creation**

```kotlin
val config = AITraderConfig(...)
val result = manager.createTrader(config)

result.fold(
    onSuccess = { traderId -> 
        println("Trader created: $traderId")
    },
    onFailure = { error ->
        when (error) {
            is MaxTradersExceededException -> 
                println("Cannot create: max 3 traders limit reached")
            else -> 
                println("Error: ${error.message}")
        }
    }
)
```

**What Happens**:
1. Checks max limit (3 traders)
2. Validates configuration
3. Creates exchange connector
4. Creates AITrader instance
5. Saves to database
6. Stores in active traders map
7. Returns trader ID

**Edge Cases**:
- **Max Limit Reached**: Returns `MaxTradersExceededException`
- **Database Error**: Returns failure with error details
- **Invalid Config**: Returns failure with validation error

### **Starting a Trader**

```kotlin
val result = manager.startTrader(traderId)

result.fold(
    onSuccess = { 
        println("Trader started successfully")
    },
    onFailure = { error ->
        when (error) {
            is IllegalArgumentException -> 
                println("Trader not found: $traderId")
            is IllegalStateException -> 
                println("Cannot start: ${error.message}")
            else -> 
                println("Error: ${error.message}")
        }
    }
)
```

**What Happens**:
1. Validates trader exists
2. Checks trader state (must be IDLE or STOPPED)
3. Calls `AITrader.start()`
4. Updates database status
5. Saves state

**State Requirements**:
- Trader must be in `IDLE` or `STOPPED` state
- Cannot start if already `RUNNING` or `PAUSED`

### **Stopping a Trader**

```kotlin
val result = manager.stopTrader(traderId)

result.fold(
    onSuccess = { 
        println("Trader stopped successfully")
    },
    onFailure = { error ->
        println("Error stopping trader: ${error.message}")
    }
)
```

**What Happens**:
1. Validates trader exists
2. Calls `AITrader.stop()`
3. Saves state to database
4. Updates database status

### **Updating Trader Configuration**

```kotlin
val newConfig = config.copy(maxStakeAmount = BigDecimal("2000.00"))
val result = manager.updateTrader(traderId, newConfig)

result.fold(
    onSuccess = { 
        println("Trader updated successfully")
    },
    onFailure = { error ->
        println("Error updating trader: ${error.message}")
    }
)
```

**What Happens**:
1. Validates trader exists
2. If trader is running, stops it first
3. Updates configuration via `AITrader.updateConfig()`
4. Updates database
5. Restarts trader if it was running

**Note**: Currently, `AITrader.updateConfig()` has TODOs for full implementation. The manager handles the stop/restart logic correctly.

### **Deleting a Trader**

```kotlin
val result = manager.deleteTrader(traderId)

result.fold(
    onSuccess = { 
        println("Trader deleted successfully")
    },
    onFailure = { error ->
        println("Error deleting trader: ${error.message}")
    }
)
```

**What Happens**:
1. Validates trader exists
2. Stops trader if running
3. Cleans up trader resources (`AITrader.cleanup()`)
4. Removes from active traders map
5. Deletes from database

---

## 💾 **State Persistence**

### **TraderStatePersistence Class**

The `TraderStatePersistence` class handles saving and loading trader state:

```kotlin
val statePersistence = TraderStatePersistence(repository)

// Save state
statePersistence.saveState(traderId, trader.getState())

// Load state
val state = statePersistence.loadState(traderId)
```

**What Gets Persisted**:
- Trader state (IDLE, RUNNING, STOPPED, etc.)
- Database status field (mapped from AITraderState)
- Balance updates (via `updateBalance()`)

**State Mapping**:
- `IDLE`, `STOPPED` → Database: `"STOPPED"`
- `STARTING`, `RUNNING` → Database: `"ACTIVE"`
- `PAUSED` → Database: `"PAUSED"`
- `ERROR` → Database: `"ERROR"`

### **Database Integration**

The manager uses `AITraderRepository` for persistence:

- **Create**: `repository.create(...)` - Creates new trader record
- **Update Status**: `repository.updateStatus(id, status)` - Updates state
- **Update Balance**: `repository.updateBalance(id, balance)` - Updates balance
- **Delete**: `repository.delete(id)` - Removes trader record
- **Find All**: `repository.findAll()` - Gets all traders for recovery

---

## 🔄 **Recovery on Restart**

### **Recovering Traders**

On application restart, call `recoverTraders()` to restore all traders from the database:

```kotlin
runBlocking {
    val result = manager.recoverTraders()
    
    result.fold(
        onSuccess = { 
            println("Recovery complete: ${manager.getTraderCount()} traders recovered")
        },
        onFailure = { error ->
            println("Recovery failed: ${error.message}")
        }
    )
}
```

**What Happens**:
1. Queries `AITraderRepository.findAll()`
2. For each trader in database:
   - Converts database model to `AITraderConfig`
   - Creates exchange connector
   - Creates `AITrader` instance
   - Restores to `activeTraders` map
3. **Note**: Traders are restored but NOT auto-started (user must start manually)

**Recovery Behavior**:
- Traders are restored in `STOPPED` state (not auto-started)
- Exchange connectors are recreated
- Configuration is restored from database
- Corrupted state is handled gracefully (logged, skipped)

### **Recovery Example**

```kotlin
// On application startup
runBlocking {
    // Initialize manager
    val manager = AITraderManager(repository, connectorFactory)
    
    // Recover traders from previous session
    manager.recoverTraders().getOrThrow()
    
    // Get recovered traders
    val traders = manager.getAllTraders()
    println("Recovered ${traders.size} traders")
    
    // Optionally start them
    traders.forEach { trader ->
        val traderId = trader.config.id
        manager.startTrader(traderId)
    }
}
```

---

## 🏥 **Health Monitoring**

### **HealthMonitor Class**

The `HealthMonitor` class provides periodic health checks:

```kotlin
// Start health monitoring
manager.startHealthMonitoring()

// Check health of specific trader
val health = manager.checkTraderHealth(traderId)

// Check health of all traders
val allHealth = manager.checkAllTradersHealth()

// Stop health monitoring
manager.stopHealthMonitoring()
```

### **TraderHealth Data Class**

```kotlin
data class TraderHealth(
    val isHealthy: Boolean,
    val status: AITraderState,
    val lastUpdate: Instant,
    val lastSignalTime: Instant? = null,
    val exchangeConnectorHealthy: Boolean = true,
    val errorCount: Int = 0,
    val issues: List<String> = emptyList()
)
```

**Health Checks**:
- **State Check**: Flags ERROR state as unhealthy
- **Exchange Connector**: Checks connector health
- **Signal Generation**: Monitors last signal time (if tracked)
- **Error Count**: Tracks error occurrences

### **Health Monitoring Example**

```kotlin
// Start monitoring with callback
manager.startHealthMonitoring()

// The manager automatically monitors and logs issues
// You can also check health manually:

runBlocking {
    // Check single trader
    val health = manager.checkTraderHealth(traderId)
    if (health != null) {
        if (!health.isHealthy) {
            println("Trader $traderId has issues: ${health.issues}")
        }
    }
    
    // Check all traders
    val allHealth = manager.checkAllTradersHealth()
    allHealth.forEach { (id, health) ->
        if (!health.isHealthy) {
            println("Trader $id: ${health.issues.joinToString()}")
        }
    }
}
```

---

## 🔧 **Resource Management**

### **Exchange Connector Caching**

The manager caches exchange connectors to reuse them for traders on the same exchange:

```kotlin
// Connector cache: Exchange -> IExchangeConnector
private val connectorCache = mutableMapOf<Exchange, IExchangeConnector>()
```

**Benefits**:
- Reduces resource usage
- Faster trader creation
- Shared connection pool

### **Resource Cleanup**

Resources are cleaned up when:
- **Trader Deleted**: `AITrader.cleanup()` is called
- **Trader Stopped**: Resources are released
- **Error Occurred**: Cleanup is performed

### **Resource Limits**

- **Max Traders**: 3 (hard limit, enforced in both manager and database)
- **Connector Reuse**: Connectors are cached and reused for same exchange
- **Memory**: Each trader instance manages its own memory

---

## 📊 **Usage Examples**

### **Example 1: Create and Manage Multiple Traders**

```kotlin
runBlocking {
    val manager = AITraderManager(repository, connectorFactory)
    
    // Create 3 traders (max limit)
    val traderIds = mutableListOf<String>()
    
    for (i in 1..3) {
        val config = AITraderConfig(
            id = "trader-$i",
            name = "Trader $i",
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT",
            virtualMoney = true,
            maxStakeAmount = BigDecimal("1000.00"),
            maxRiskLevel = 5,
            maxTradingDuration = Duration.ofHours(24),
            minReturnPercent = 5.0,
            strategy = TradingStrategy.TREND_FOLLOWING,
            candlestickInterval = TimeFrame.ONE_HOUR
        )
        
        val traderId = manager.createTrader(config).getOrThrow()
        traderIds.add(traderId)
        println("Created trader: $traderId")
    }
    
    // Try to create 4th trader (should fail)
    val config4 = config.copy(id = "trader-4", name = "Trader 4")
    val result4 = manager.createTrader(config4)
    assertTrue(result4.isFailure)
    assertTrue(result4.exceptionOrNull() is MaxTradersExceededException)
    
    // Start all traders
    traderIds.forEach { id ->
        manager.startTrader(id).getOrThrow()
    }
    
    // Check count
    assertEquals(3, manager.getTraderCount())
    
    // Get all traders
    val allTraders = manager.getAllTraders()
    assertEquals(3, allTraders.size)
}
```

### **Example 2: Recovery After Restart**

```kotlin
// Session 1: Create and start traders
runBlocking {
    val manager = AITraderManager(repository, connectorFactory)
    
    val traderId = manager.createTrader(config).getOrThrow()
    manager.startTrader(traderId).getOrThrow()
    
    // Application shuts down (traders saved to database)
}

// Session 2: Restart and recover
runBlocking {
    val manager = AITraderManager(repository, connectorFactory)
    
    // Recover traders from database
    manager.recoverTraders().getOrThrow()
    
    // Traders are restored but not started
    val traders = manager.getAllTraders()
    traders.forEach { trader ->
        assertEquals(AITraderState.STOPPED, trader.getState())
        
        // Start if needed
        val traderId = trader.config.id
        manager.startTrader(traderId).getOrThrow()
    }
}
```

### **Example 3: Health Monitoring**

```kotlin
runBlocking {
    val manager = AITraderManager(repository, connectorFactory)
    
    // Create and start trader
    val traderId = manager.createTrader(config).getOrThrow()
    manager.startTrader(traderId).getOrThrow()
    
    // Start health monitoring
    manager.startHealthMonitoring()
    
    // Check health periodically
    delay(60000) // Wait 1 minute
    
    val health = manager.checkTraderHealth(traderId)
    if (health != null) {
        println("Trader $traderId health:")
        println("  Healthy: ${health.isHealthy}")
        println("  Status: ${health.status}")
        println("  Issues: ${health.issues.joinToString()}")
    }
    
    // Check all traders
    val allHealth = manager.checkAllTradersHealth()
    allHealth.forEach { (id, health) ->
        println("Trader $id: ${if (health.isHealthy) "OK" else "ISSUES"}")
    }
    
    // Stop monitoring
    manager.stopHealthMonitoring()
}
```

---

## 🐛 **Troubleshooting**

### **Issue: MaxTradersExceededException**

**Symptom**: Cannot create more than 3 traders.

**Cause**: Maximum limit reached (v1.0 scope: 3 traders).

**Solution**:
```kotlin
// Check current count
val count = manager.getTraderCount()
if (count >= 3) {
    // Delete a trader first
    manager.deleteTrader(existingTraderId).getOrThrow()
    // Then create new one
    manager.createTrader(newConfig).getOrThrow()
}
```

### **Issue: Trader Not Found**

**Symptom**: `IllegalArgumentException("Trader not found: $traderId")`

**Cause**: Trader ID doesn't exist in active traders map.

**Solution**:
```kotlin
// Check if trader exists
val trader = manager.getTrader(traderId)
if (trader == null) {
    // Trader might need to be recovered from database
    manager.recoverTraders()
    // Or trader was deleted
}
```

### **Issue: Cannot Start Trader in Current State**

**Symptom**: `IllegalStateException("Cannot start trader in state: RUNNING")`

**Cause**: Trader is already running or in invalid state.

**Solution**:
```kotlin
// Check trader state first
val trader = manager.getTrader(traderId)
val state = trader?.getState()

when (state) {
    AITraderState.RUNNING -> {
        // Already running, no action needed
    }
    AITraderState.PAUSED -> {
        // Resume instead of start
        trader.resume()
    }
    AITraderState.ERROR -> {
        // Fix error first, then restart
        // (Error recovery logic)
    }
    else -> {
        // Can start
        manager.startTrader(traderId)
    }
}
```

### **Issue: Recovery Fails for Some Traders**

**Symptom**: Some traders not recovered, errors in logs.

**Cause**: Corrupted database state or missing configuration.

**Solution**:
```kotlin
// Recovery handles errors gracefully
manager.recoverTraders().fold(
    onSuccess = { 
        val recovered = manager.getTraderCount()
        println("Recovered $recovered traders")
        // Check logs for any skipped traders
    },
    onFailure = { error ->
        println("Recovery failed: ${error.message}")
        // Check database integrity
    }
)
```

### **Issue: Health Monitor Not Detecting Issues**

**Symptom**: Health checks show healthy but trader has problems.

**Cause**: Health checks may not cover all scenarios yet.

**Solution**:
```kotlin
// Manually check trader state
val trader = manager.getTrader(traderId)
val state = trader?.getState()

if (state == AITraderState.ERROR) {
    // Handle error state
}

// Check metrics
val metrics = trader?.getMetrics()
// Review metrics for issues
```

---

## 🔍 **API Reference**

### **AITraderManager Methods**

| Method | Description | Returns |
|--------|-------------|---------|
| `createTrader(config)` | Create new trader | `Result<String>` (trader ID) |
| `startTrader(id)` | Start trader | `Result<Unit>` |
| `stopTrader(id)` | Stop trader | `Result<Unit>` |
| `updateTrader(id, config)` | Update trader config | `Result<Unit>` |
| `deleteTrader(id)` | Delete trader | `Result<Unit>` |
| `getTrader(id)` | Get trader instance | `AITrader?` |
| `getAllTraders()` | Get all active traders | `List<AITrader>` |
| `getTraderCount()` | Get current count | `Int` |
| `recoverTraders()` | Recover from database | `Result<Unit>` |
| `checkTraderHealth(id)` | Check trader health | `TraderHealth?` |
| `checkAllTradersHealth()` | Check all traders health | `Map<String, TraderHealth>` |
| `startHealthMonitoring()` | Start periodic monitoring | `Unit` |
| `stopHealthMonitoring()` | Stop monitoring | `Unit` |

### **TraderStatePersistence Methods**

| Method | Description | Returns |
|--------|-------------|---------|
| `saveState(id, state)` | Save trader state | `Result<Unit>` |
| `loadState(id)` | Load trader state | `AITraderState?` |
| `updateBalance(id, balance)` | Update balance | `Result<Unit>` |
| `dbTraderToConfig(dbTrader)` | Convert DB model to config | `AITraderConfig` |

### **HealthMonitor Methods**

| Method | Description | Returns |
|--------|-------------|---------|
| `startMonitoring(traders, callback)` | Start periodic checks | `Unit` |
| `stopMonitoring()` | Stop monitoring | `Unit` |
| `checkTraderHealth(id, trader)` | Check single trader | `TraderHealth` |
| `checkAllTradersHealth(traders)` | Check all traders | `Map<String, TraderHealth>` |

---

## 📚 **Related Documentation**

- **AI_TRADER_CORE_GUIDE.md**: Details about AITrader class
- **TESTING_GUIDE.md**: Testing patterns and best practices
- **DEVELOPMENT_WORKFLOW.md**: Development workflow and CI/CD
- **Issue #12**: Issue planning document

---

## ✅ **Best Practices**

1. **Always Check Results**: Use `Result.fold()` or `getOrThrow()` to handle errors
2. **Recover on Startup**: Call `recoverTraders()` on application startup
3. **Monitor Health**: Start health monitoring for production use
4. **Clean Up**: Always delete traders when no longer needed
5. **Handle Limits**: Check `getTraderCount()` before creating new traders
6. **Thread Safety**: All operations are thread-safe, but avoid concurrent modifications
7. **State Management**: Check trader state before operations (start/stop/update)

---

**Last Updated**: November 6, 2025  
**Version**: 1.0  
**Status**: Complete

