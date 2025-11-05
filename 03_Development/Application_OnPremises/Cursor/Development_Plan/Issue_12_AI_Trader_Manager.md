# Issue #12: AI Trader Manager

**Status**: 📋 **PLANNED**  
**Assigned**: AI Assistant  
**Created**: November 5, 2025  
**Started**: Not Started  
**Completed**: Not Completed  
**Duration**: 2-3 days (estimated)  
**Epic**: Epic 3 (AI Trading Engine)  
**Priority**: P0 (Critical - Required for multiple traders)  
**Dependencies**: Issue #11 ⏳ (AI Trader Core)

> **NOTE**: Manages lifecycle of multiple AI trader instances (max 3 per ATP_ProdSpec_52 and v1.0 scope). Handles creation, starting, stopping, updating, and deletion of traders with state persistence and recovery.

---

## 📋 **Objective**

Implement `AITraderManager` class that manages the lifecycle of multiple AI trader instances (maximum 3 for v1.0), including creation, starting, stopping, configuration updates, and deletion. The manager must handle state persistence, recovery on restart, resource allocation, and health monitoring.

---

## 🎯 **Goals**

1. **Lifecycle Management**: Create, start, stop, update, and delete AI trader instances
2. **Max Limit Enforcement**: Enforce maximum 3 traders limit (v1.0 scope)
3. **State Persistence**: Save trader state to database and recover on restart
4. **Resource Allocation**: Manage resources per trader (exchange connectors, memory)
5. **Health Monitoring**: Monitor trader health and handle failures
6. **Recovery**: Recover trader state on application restart
7. **Integration**: Integrate with AITraderRepository and AITrader class

---

## 📝 **Task Breakdown**

### **Task 1: Design AITraderManager Architecture** [Status: ⏳ PENDING]
- [ ] Create `AITraderManager` class:
  - [ ] Properties: `activeTraders: Map<String, AITrader>`, `maxTraders: Int = 3`
  - [ ] Dependencies: `AITraderRepository`, `ConnectorFactory`, `ConfigManager`
  - [ ] Thread-safety with `Mutex` for concurrent access
- [ ] Define lifecycle operations:
  - [ ] `createTrader(config: AITraderConfig): Result<String>` - Create and return trader ID
  - [ ] `startTrader(id: String): Result<Unit>` - Start trader
  - [ ] `stopTrader(id: String): Result<Unit>` - Stop trader
  - [ ] `updateTrader(id: String, newConfig: AITraderConfig): Result<Unit>` - Update config
  - [ ] `deleteTrader(id: String): Result<Unit>` - Delete trader
  - [ ] `getTrader(id: String): AITrader?` - Get trader instance
  - [ ] `getAllTraders(): List<AITrader>` - Get all active traders
  - [ ] `getTraderCount(): Int` - Get current count
- [ ] Document architecture in KDoc

### **Task 2: Implement Trader Creation** [Status: ⏳ PENDING]
- [ ] Implement `createTrader()`:
  - [ ] Check max limit (3 traders) - throw `MaxTradersExceededException` if limit reached
  - [ ] Validate configuration (use RiskManager if available - Issue #14)
  - [ ] Create exchange connector via `ConnectorFactory`
  - [ ] Create `AITrader` instance
  - [ ] Save to database via `AITraderRepository`
  - [ ] Store in active traders map
  - [ ] Return trader ID
- [ ] Handle edge cases:
  - [ ] Duplicate trader names
  - [ ] Invalid exchange configuration
  - [ ] Database errors
- [ ] Unit tests for creation logic

### **Task 3: Implement Start/Stop Operations** [Status: ⏳ PENDING]
- [ ] Implement `startTrader()`:
  - [ ] Validate trader exists
  - [ ] Check trader state (must be IDLE or STOPPED)
  - [ ] Load state from database if needed
  - [ ] Call `AITrader.start()`
  - [ ] Update database status
  - [ ] Handle errors gracefully
- [ ] Implement `stopTrader()`:
  - [ ] Validate trader exists
  - [ ] Call `AITrader.stop()`
  - [ ] Save state to database
  - [ ] Update database status
  - [ ] Clean up resources
- [ ] Unit tests for start/stop operations

### **Task 4: Implement Update and Delete Operations** [Status: ⏳ PENDING]
- [ ] Implement `updateTrader()`:
  - [ ] Validate trader exists
  - [ ] Check if trader is running (may need to stop first)
  - [ ] Validate new configuration
  - [ ] Call `AITrader.updateConfig()`
  - [ ] Update database
  - [ ] Restart trader if it was running
- [ ] Implement `deleteTrader()`:
  - [ ] Validate trader exists
  - [ ] Stop trader if running
  - [ ] Remove from active traders map
  - [ ] Delete from database
  - [ ] Clean up resources (exchange connector, etc.)
- [ ] Unit tests for update/delete operations

### **Task 5: Implement State Persistence** [Status: ⏳ PENDING]
- [ ] Create `TraderStatePersistence` class:
  - [ ] `saveState(trader: AITrader): Result<Unit>` - Save trader state to database
  - [ ] `loadState(id: String): Result<AITraderState>` - Load trader state from database
- [ ] State to persist:
  - [ ] Trader configuration
  - [ ] Current state (IDLE, RUNNING, etc.)
  - [ ] Performance metrics
  - [ ] Last update timestamp
- [ ] Integration with `AITraderRepository`:
  - [ ] Use existing repository methods
  - [ ] Add new methods if needed (updateState, getState, etc.)
- [ ] Unit tests for state persistence

### **Task 6: Implement Recovery on Restart** [Status: ⏳ PENDING]
- [ ] Create `recoverTraders()` method:
  - [ ] Load all traders from database on startup
  - [ ] Recreate `AITrader` instances from saved state
  - [ ] Restore exchange connectors
  - [ ] Restore trader state (IDLE, RUNNING, etc.)
  - [ ] Handle corrupted state gracefully
- [ ] Recovery logic:
  - [ ] Query `AITraderRepository.findAll()`
  - [ ] For each trader, recreate `AITrader` instance
  - [ ] If trader was RUNNING, restart it (or leave in STOPPED - user decision)
  - [ ] Log recovery actions
- [ ] Unit tests for recovery logic

### **Task 7: Implement Health Monitoring** [Status: ⏳ PENDING]
- [ ] Create `HealthMonitor` class:
  - [ ] `checkTraderHealth(id: String): TraderHealth` - Check single trader
  - [ ] `checkAllTradersHealth(): Map<String, TraderHealth>` - Check all traders
- [ ] Health metrics:
  - [ ] Trader state (RUNNING, ERROR, etc.)
  - [ ] Last signal generation time
  - [ ] Exchange connector status
  - [ ] Error count
  - [ ] Performance metrics
- [ ] Create `TraderHealth` data class:
  - [ ] `isHealthy: Boolean`
  - [ ] `status: String`
  - [ ] `lastUpdate: Instant`
  - [ ] `issues: List<String>`
- [ ] Periodic health checks (background coroutine)
- [ ] Alert on health issues (logging)
- [ ] Unit tests for health monitoring

### **Task 8: Implement Resource Management** [Status: ⏳ PENDING]
- [ ] Resource tracking per trader:
  - [ ] Exchange connector instances
  - [ ] Memory usage (approximate)
  - [ ] Thread/coroutine usage
- [ ] Resource cleanup:
  - [ ] Clean up on trader deletion
  - [ ] Clean up on stop
  - [ ] Clean up on error
- [ ] Resource limits:
  - [ ] Max 3 traders (hard limit)
  - [ ] Exchange connector reuse (if same exchange)
- [ ] Unit tests for resource management

### **Task 9: Testing** [Status: ⏳ PENDING]
- [ ] Write unit tests for `AITraderManager`:
  - [ ] Trader creation (success, max limit, invalid config)
  - [ ] Start/stop operations
  - [ ] Update/delete operations
  - [ ] State persistence
  - [ ] Recovery on restart
  - [ ] Health monitoring
  - [ ] Resource management
  - [ ] Thread-safety (concurrent access)
- [ ] Write integration tests:
  - [ ] AITraderManager with real AITraderRepository
  - [ ] Recovery scenario with database
  - [ ] Multi-trader scenarios (up to 3)
- [ ] Verify all tests pass: `./gradlew test`
- [ ] Code coverage meets targets (>80%)

### **Task 10: Documentation** [Status: ⏳ PENDING]
- [ ] Add comprehensive KDoc to all classes
- [ ] Create `AI_TRADER_MANAGER_GUIDE.md`:
  - [ ] Architecture overview
  - [ ] Lifecycle management explanation
  - [ ] State persistence details
  - [ ] Recovery process
  - [ ] Usage examples
  - [ ] Troubleshooting guide
- [ ] Update relevant documentation files

### **Task 11: Build & Commit** [Status: ⏳ PENDING]
- [ ] Run all tests: `./gradlew test`
- [ ] Build project: `./gradlew build`
- [ ] Fix any compilation errors
- [ ] Fix any test failures
- [ ] Commit changes
- [ ] Push to GitHub
- [ ] Verify CI pipeline passes
- [ ] Update this Issue file and Development_Plan_v2.md

---

## 📦 **Deliverables**

### **New Files**
1. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/AITraderManager.kt`
2. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/TraderStatePersistence.kt`
3. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/TraderHealth.kt`
4. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/HealthMonitor.kt`

### **Test Files**
1. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/traders/AITraderManagerTest.kt`

### **Documentation**
- `Development_Handbook/AI_TRADER_MANAGER_GUIDE.md`

---

## 🎯 **Success Criteria**

| Criterion | Status | Verification Method |
|-----------|--------|---------------------|
| AITraderManager implemented with all lifecycle methods | ⏳ | File exists, unit tests pass |
| Max 3 traders limit enforced | ⏳ | Unit tests pass, negative test cases |
| State persistence working | ⏳ | Unit tests pass, database integration tests |
| Recovery on restart working | ⏳ | Integration tests pass |
| Health monitoring implemented | ⏳ | Unit tests pass |
| All tests pass | ⏳ | `./gradlew test` |
| Build succeeds | ⏳ | `./gradlew build` |
| CI pipeline passes | ⏳ | GitHub Actions |
| Code coverage >80% | ⏳ | Coverage report |
| Documentation complete | ⏳ | Documentation review |

---

## 🔧 **Key Technologies**

| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin Coroutines | 1.7+ | Async operations, health monitoring |
| Exposed (Epic 1) | - | Database persistence |
| AITraderRepository (Epic 1) | - | Database operations |
| AITrader (Issue #11) | - | Trader instances |

---

## ⏱️ **Estimated Timeline**

| Task | Estimated Time |
|------|---------------|
| Task 1: Design Architecture | 3 hours |
| Task 2: Implement Creation | 3 hours |
| Task 3: Implement Start/Stop | 3 hours |
| Task 4: Implement Update/Delete | 3 hours |
| Task 5: State Persistence | 3 hours |
| Task 6: Recovery on Restart | 3 hours |
| Task 7: Health Monitoring | 3 hours |
| Task 8: Resource Management | 2 hours |
| Task 9: Testing | 6 hours |
| Task 10: Documentation | 3 hours |
| Task 11: Build & Commit | 2 hours |
| **Total** | **~2-3 days** |

---

## 🔄 **Dependencies**

### **Depends On** (Must be complete first)
- Issue #11: AI Trader Core ⏳

### **Blocks** (Cannot start until this is done)
- None (allows Issues #13-15 to proceed)

### **Related** (Related but not blocking)
- Issue #13: Position Manager
- Issue #14: Risk Manager
- Epic 1: AITraderRepository

---

## ⚠️ **Risks & Considerations**

| Risk | Impact | Mitigation Strategy |
|------|--------|---------------------|
| State corruption on restart | High | Validate state, handle gracefully, log errors |
| Concurrent access issues | High | Use Mutex, thorough thread-safety testing |
| Resource leaks | Medium | Proper cleanup, resource tracking |
| Max limit conflicts | Medium | Clear error messages, validation |

---

**Issue Created**: November 5, 2025  
**Priority**: P0 (Critical)  
**Estimated Effort**: 2-3 days  
**Status**: 📋 **PLANNED**

