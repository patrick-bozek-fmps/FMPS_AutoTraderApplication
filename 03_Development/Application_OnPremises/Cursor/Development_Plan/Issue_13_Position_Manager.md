# Issue #13: Position Manager

**Status**: 📋 **PLANNED**  
**Assigned**: AI Assistant  
**Created**: November 5, 2025  
**Started**: Not Started  
**Completed**: Not Completed  
**Duration**: 2-3 days (estimated)  
**Epic**: Epic 3 (AI Trading Engine)  
**Priority**: P1 (High - Required for trading operations)  
**Dependencies**: Issue #11 ⏳ (AI Trader Core), Epic 2 ✅ (Exchange Connectors)

> **NOTE**: Manages position tracking, P&L calculation, stop-loss execution, and position history. Integrates with exchange connectors and AITrader for real-time position updates.

---

## 📋 **Objective**

Implement `PositionManager` class that tracks open positions, calculates real-time profit and loss (P&L), manages stop-loss execution, maintains position history, and handles position persistence. The manager must integrate seamlessly with exchange connectors and support position recovery on restart.

---

## 🎯 **Goals**

1. **Position Tracking**: Track open positions with real-time updates from exchange
2. **P&L Calculation**: Calculate unrealized and realized profit/loss in real-time
3. **Stop-Loss Management**: Execute stop-loss orders automatically when thresholds are met
4. **Position History**: Maintain complete position history for analysis
5. **Position Persistence**: Save positions to database and recover on restart
6. **Position Recovery**: Recover open positions from exchange on restart
7. **Integration**: Seamless integration with exchange connectors and AITrader

---

## 📝 **Task Breakdown**

### **Task 1: Design PositionManager Architecture** [Status: ⏳ PENDING]
- [ ] Create `PositionManager` class:
  - [ ] Properties: `activePositions: Map<String, Position>`, `positionHistory: List<Position>`
  - [ ] Dependencies: `IExchangeConnector`, `TradeHistoryRepository` (Epic 1)
  - [ ] Thread-safety with `Mutex` for concurrent access
- [ ] Define core operations:
  - [ ] `openPosition(signal: TradingSignal, traderId: String): Result<Position>` - Open new position
  - [ ] `closePosition(positionId: String, reason: String): Result<Position>` - Close position
  - [ ] `updatePosition(positionId: String, currentPrice: BigDecimal): Result<Unit>` - Update position
  - [ ] `getPosition(positionId: String): Position?` - Get position by ID
  - [ ] `getPositionsByTrader(traderId: String): List<Position>` - Get all positions for trader
  - [ ] `getAllPositions(): List<Position>` - Get all active positions
  - [ ] `calculatePnL(position: Position): BigDecimal` - Calculate P&L
  - [ ] `checkStopLoss(position: Position): Boolean` - Check if stop-loss triggered
- [ ] Document architecture in KDoc

### **Task 2: Implement Position Opening** [Status: ⏳ PENDING]
- [ ] Implement `openPosition()`:
  - [ ] Validate signal (must be BUY or SELL)
  - [ ] Calculate position size based on signal and risk parameters
  - [ ] Place order via exchange connector
  - [ ] Create `Position` object from order result
  - [ ] Store in active positions map
  - [ ] Save to database via `TradeHistoryRepository`
  - [ ] Return position with ID
- [ ] Position creation logic:
  - [ ] Generate unique position ID (UUID)
  - [ ] Extract entry price from order execution
  - [ ] Set initial current price = entry price
  - [ ] Calculate initial P&L (0 for new position)
  - [ ] Set stop-loss price if provided
  - [ ] Set take-profit price if provided
- [ ] Handle edge cases:
  - [ ] Order execution failure
  - [ ] Partial fills
  - [ ] Exchange errors
- [ ] Unit tests for position opening

### **Task 3: Implement Position Updates** [Status: ⏳ PENDING]
- [ ] Implement `updatePosition()`:
  - [ ] Validate position exists
  - [ ] Fetch current market price from exchange connector
  - [ ] Update position's `currentPrice`
  - [ ] Recalculate `unrealizedPnL`
  - [ ] Update position timestamp
  - [ ] Persist update to database
- [ ] P&L calculation logic:
  - [ ] For LONG positions: `PnL = (currentPrice - entryPrice) * quantity`
  - [ ] For SHORT positions: `PnL = (entryPrice - currentPrice) * quantity`
  - [ ] Apply leverage if applicable
  - [ ] Handle fees (optional - can be deferred)
- [ ] Real-time update mechanism:
  - [ ] Subscribe to ticker updates via WebSocket (if available)
  - [ ] Fallback to periodic polling
  - [ ] Update all active positions periodically
- [ ] Unit tests for position updates and P&L calculation

### **Task 4: Implement Stop-Loss Management** [Status: ⏳ PENDING]
- [ ] Implement `checkStopLoss()`:
  - [ ] Check if position has stop-loss configured
  - [ ] Compare current price with stop-loss threshold
  - [ ] Return true if stop-loss triggered
- [ ] Implement stop-loss execution:
  - [ ] When stop-loss triggered, automatically close position
  - [ ] Place close order via exchange connector
  - [ ] Update position with close price and final P&L
  - [ ] Move position to history
  - [ ] Log stop-loss execution
- [ ] Stop-loss types:
  - [ ] Fixed stop-loss (percentage below/above entry)
  - [ ] Trailing stop-loss (optional - can be deferred)
  - [ ] Time-based stop-loss (optional - can be deferred)
- [ ] Stop-loss validation:
  - [ ] Validate stop-loss price is reasonable
  - [ ] Prevent stop-loss too close to entry (optional)
- [ ] Unit tests for stop-loss logic

### **Task 5: Implement Position Closing** [Status: ⏳ PENDING]
- [ ] Implement `closePosition()`:
  - [ ] Validate position exists and is open
  - [ ] Place close order via exchange connector
  - [ ] Calculate final P&L (realized)
  - [ ] Update position with close price and close time
  - [ ] Remove from active positions map
  - [ ] Add to position history
  - [ ] Persist to database
  - [ ] Return closed position
- [ ] Close reasons:
  - [ ] `StopLoss` - Stop-loss triggered
  - [ ] `TakeProfit` - Take-profit reached
  - [ ] `Manual` - Manual close by user
  - [ ] `Signal` - Close signal from strategy
  - [ ] `Error` - Error-related close
- [ ] Handle edge cases:
  - [ ] Position already closed
  - [ ] Order execution failure
  - [ ] Partial close (optional - can be deferred)
- [ ] Unit tests for position closing

### **Task 6: Implement Position History** [Status: ⏳ PENDING]
- [ ] Create `PositionHistory` data class:
  - [ ] `positionId: String`
  - [ ] `traderId: String`
  - [ ] `symbol: String`
  - [ ] `action: TradeAction`
  - [ ] `entryPrice: BigDecimal`
  - [ ] `closePrice: BigDecimal`
  - [ ] `quantity: BigDecimal`
  - [ ] `realizedPnL: BigDecimal`
  - [ ] `openedAt: Instant`
  - [ ] `closedAt: Instant`
  - [ ] `closeReason: String`
  - [ ] `duration: Duration`
- [ ] History operations:
  - [ ] `getHistoryByTrader(traderId: String): List<PositionHistory>` - Get trader's history
  - [ ] `getHistoryBySymbol(symbol: String): List<PositionHistory>` - Get symbol's history
  - [ ] `getHistoryByDateRange(start: Instant, end: Instant): List<PositionHistory>` - Get by date range
  - [ ] `getTotalPnL(): BigDecimal` - Get total realized P&L
  - [ ] `getWinRate(): Double` - Get win rate from history
- [ ] Integration with `TradeHistoryRepository`:
  - [ ] Use existing repository methods
  - [ ] Add new methods if needed
- [ ] Unit tests for position history

### **Task 7: Implement Position Persistence** [Status: ⏳ PENDING]
- [ ] Create `PositionPersistence` class:
  - [ ] `savePosition(position: Position): Result<Unit>` - Save position to database
  - [ ] `loadPosition(positionId: String): Result<Position>` - Load position from database
  - [ ] `loadAllActivePositions(): Result<List<Position>>` - Load all active positions
- [ ] Persistence logic:
  - [ ] Save position on open
  - [ ] Update position on price update (periodic)
  - [ ] Save to history on close
  - [ ] Handle database errors gracefully
- [ ] Integration with `TradeHistoryRepository`:
  - [ ] Map Position to database model
  - [ ] Map database model to Position
  - [ ] Handle schema differences
- [ ] Unit tests for position persistence

### **Task 8: Implement Position Recovery** [Status: ⏳ PENDING]
- [ ] Create `recoverPositions()` method:
  - [ ] Load saved positions from database on startup
  - [ ] For each position, verify it still exists on exchange
  - [ ] If position exists on exchange, restore it
  - [ ] If position doesn't exist, mark as closed (orphaned)
  - [ ] Update positions with current market prices
- [ ] Recovery logic:
  - [ ] Query `TradeHistoryRepository` for active positions
  - [ ] For each position, call exchange connector's `getPosition()`
  - [ ] Reconcile database state with exchange state
  - [ ] Handle discrepancies (position closed externally, etc.)
  - [ ] Log recovery actions
- [ ] Orphaned position handling:
  - [ ] Detect positions in database but not on exchange
  - [ ] Mark as closed with reason "Orphaned"
  - [ ] Calculate final P&L based on last known price
- [ ] Unit tests for position recovery

### **Task 9: Implement Position Monitoring** [Status: ⏳ PENDING]
- [ ] Create background monitoring coroutine:
  - [ ] Periodically update all active positions
  - [ ] Check stop-loss conditions
  - [ ] Check take-profit conditions (if implemented)
  - [ ] Log position status
- [ ] Monitoring interval:
  - [ ] Configurable update interval (default: 5 seconds)
  - [ ] Adjust based on exchange rate limits
  - [ ] Handle rate limiting gracefully
- [ ] Position health checks:
  - [ ] Verify position still exists on exchange
  - [ ] Check for position discrepancies
  - [ ] Alert on issues (logging)
- [ ] Unit tests for position monitoring

### **Task 10: Testing** [Status: ⏳ PENDING]
- [ ] Write unit tests for `PositionManager`:
  - [ ] Position opening (success, failure, edge cases)
  - [ ] Position updates and P&L calculation
  - [ ] Stop-loss execution
  - [ ] Position closing
  - [ ] Position history operations
  - [ ] Position persistence
  - [ ] Position recovery
  - [ ] Position monitoring
  - [ ] Thread-safety (concurrent access)
- [ ] Write integration tests:
  - [ ] PositionManager with mock exchange connector
  - [ ] Position recovery scenario
  - [ ] End-to-end position lifecycle
- [ ] Verify all tests pass: `./gradlew test`
- [ ] Code coverage meets targets (>80%)

### **Task 11: Documentation** [Status: ⏳ PENDING]
- [ ] Add comprehensive KDoc to all classes
- [ ] Create `POSITION_MANAGER_GUIDE.md`:
  - [ ] Architecture overview
  - [ ] Position lifecycle explanation
  - [ ] P&L calculation details
  - [ ] Stop-loss management
  - [ ] Position recovery process
  - [ ] Usage examples
  - [ ] Troubleshooting guide
- [ ] Update relevant documentation files

### **Task 12: Build & Commit** [Status: ⏳ PENDING]
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
1. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/PositionManager.kt`
2. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/PositionHistory.kt`
3. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/PositionPersistence.kt`

### **Test Files**
1. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/traders/PositionManagerTest.kt`

### **Documentation**
- `Development_Handbook/POSITION_MANAGER_GUIDE.md`

---

## 🎯 **Success Criteria**

| Criterion | Status | Verification Method |
|-----------|--------|---------------------|
| PositionManager implemented with all operations | ⏳ | File exists, unit tests pass |
| Real-time P&L calculation working | ⏳ | Unit tests pass, integration tests |
| Stop-loss execution working | ⏳ | Unit tests pass, integration tests |
| Position history tracking working | ⏳ | Unit tests pass, database tests |
| Position recovery on restart working | ⏳ | Integration tests pass |
| All tests pass | ⏳ | `./gradlew test` |
| Build succeeds | ⏳ | `./gradlew build` |
| CI pipeline passes | ⏳ | GitHub Actions |
| Code coverage >80% | ⏳ | Coverage report |
| Documentation complete | ⏳ | Documentation review |

---

## 🔧 **Key Technologies**

| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin Coroutines | 1.7+ | Async operations, position monitoring |
| Position Model (Epic 1) | - | Position data class |
| TradeHistoryRepository (Epic 1) | - | Database persistence |
| IExchangeConnector (Epic 2) | - | Exchange operations |

---

## ⏱️ **Estimated Timeline**

| Task | Estimated Time |
|------|---------------|
| Task 1: Design Architecture | 3 hours |
| Task 2: Position Opening | 3 hours |
| Task 3: Position Updates | 3 hours |
| Task 4: Stop-Loss Management | 3 hours |
| Task 5: Position Closing | 3 hours |
| Task 6: Position History | 2 hours |
| Task 7: Position Persistence | 2 hours |
| Task 8: Position Recovery | 3 hours |
| Task 9: Position Monitoring | 2 hours |
| Task 10: Testing | 6 hours |
| Task 11: Documentation | 3 hours |
| Task 12: Build & Commit | 2 hours |
| **Total** | **~2-3 days** |

---

## 🔄 **Dependencies**

### **Depends On** (Must be complete first)
- Issue #11: AI Trader Core ⏳ (for TradingSignal)
- Epic 2: Exchange Connectors ✅ (for order execution and position queries)

### **Blocks** (Cannot start until this is done)
- None (allows Issue #14 to proceed)

### **Related** (Related but not blocking)
- Issue #14: Risk Manager (will use PositionManager for exposure calculations)
- Epic 1: TradeHistoryRepository, Position model

---

## ⚠️ **Risks & Considerations**

| Risk | Impact | Mitigation Strategy |
|------|--------|---------------------|
| Position state mismatch | High | Regular reconciliation, recovery on restart |
| Exchange position sync issues | High | Periodic verification, handle discrepancies gracefully |
| Stop-loss execution delays | Medium | Immediate execution, retry logic |
| P&L calculation accuracy | High | Comprehensive testing, known test cases |

---

**Issue Created**: November 5, 2025  
**Priority**: P1 (High)  
**Estimated Effort**: 2-3 days  
**Status**: 📋 **PLANNED**

