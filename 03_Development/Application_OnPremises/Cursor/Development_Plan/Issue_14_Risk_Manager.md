# Issue #14: Risk Manager

**Status**: 🚧 **IN PROGRESS**  
**Assigned**: AI Assistant  
**Created**: November 5, 2025  
**Started**: November 7, 2025  
**Completed**: Not Completed  
**Duration**: 2-3 days (estimated)  
**Epic**: Epic 3 (AI Trading Engine)  
**Priority**: P0 (Critical - Required for ATP_ProdSpec_54 compliance)  
**Dependencies**: Issue #11 ✅ (AI Trader Core), Issue #13 ✅ (Position Manager)

> **NOTE**: Implements risk management system to enforce budget limits, leverage limits, exposure limits, and prevent invalid trades per ATP_ProdSpec_54. Critical for preventing trader creation when conditions are not met.

---

## 📋 **Objective**

Implement `RiskManager` class that validates budgets, enforces leverage limits, monitors exposure (per trader and total), implements stop-loss logic, provides emergency stop functionality, and calculates risk scores. The manager must comply with ATP_ProdSpec_54 requirements for preventing AI trader creation when funds are insufficient.

---

## 🎯 **Goals**

1. **Budget Validation**: Validate available funds and prevent over-allocation per ATP_ProdSpec_54
2. **Leverage Limit Enforcement**: Enforce per-trader and total leverage limits
3. **Exposure Monitoring**: Monitor exposure per trader and total system exposure
4. **Stop-Loss Logic**: Position-level and trader-level stop-loss management
5. **Emergency Stop**: Emergency stop functionality for risk management
6. **Risk Scoring**: Calculate risk scores for trading decisions
7. **Compliance**: Full compliance with ATP_ProdSpec_54 requirements

---

## 📝 **Task Breakdown**

### **Task 1: Design RiskManager Architecture** [Status: ✅ COMPLETE]
- [x] Create `RiskManager` class:
  - [x] Properties: `totalBudget: BigDecimal`, `allocatedBudget: BigDecimal`, `riskConfig: RiskConfig`
  - [x] Dependencies: `PositionManager`, `AITraderManager` (optional for now)
  - [x] Thread-safety with `Mutex` for concurrent access
- [x] Define core operations:
  - [x] `validateBudget(requiredAmount: BigDecimal, traderId: String?): Result<Unit>` - Validate budget
  - [x] `validateLeverage(leverage: Int, traderId: String?): Result<Unit>` - Validate leverage
  - [x] `canOpenPosition(size: BigDecimal, leverage: Int, traderId: String): Result<Boolean>` - Check if position can be opened
  - [x] `calculateExposure(traderId: String?): BigDecimal` - Calculate exposure
  - [x] `calculateTotalExposure(): BigDecimal` - Calculate total system exposure
  - [x] `checkRiskLimits(traderId: String): RiskCheckResult` - Comprehensive risk check
  - [x] `emergencyStop(traderId: String?): Result<Unit>` - Emergency stop
- [x] Create `RiskConfig` data class:
  - [x] `maxTotalBudget: BigDecimal` - Maximum total budget
  - [x] `maxLeveragePerTrader: Int` - Max leverage per trader
  - [x] `maxTotalLeverage: Int` - Max total leverage across all traders
  - [x] `maxExposurePerTrader: BigDecimal` - Max exposure per trader
  - [x] `maxTotalExposure: BigDecimal` - Max total exposure
  - [x] `maxDailyLoss: BigDecimal` - Max daily loss limit
  - [x] `stopLossPercentage: Double` - Default stop-loss percentage
- [x] Document architecture in KDoc

### **Task 2: Implement Budget Validation** [Status: ✅ COMPLETE]
- [x] Implement `validateBudget()`:
  - [x] Check if required amount is available
  - [x] Check if allocating would exceed total budget
  - [x] Check per-trader budget allocation
  - [x] Return error if validation fails (ATP_ProdSpec_54: "No money available")
  - [x] Return error if insufficient funds (ATP_ProdSpec_54: "Not enough money to buy commodity")
- [x] Budget tracking:
  - [x] Derive allocated budget per trader from live exposure snapshots
  - [x] Derive total allocated budget from aggregate exposure
  - [x] Update budgets automatically via exposure recalculation on open/close
- [x] Budget validation logic:
  - [x] `availableBudget = maxTotalBudget - currentExposure`
  - [x] `requiredWithLeverage = requiredAmount * leverage`
  - [x] Validate: `requiredWithLeverage <= availableBudget`
  - [x] Validate: `requiredWithLeverage <= maxExposurePerTrader`
- [x] Unit tests for budget validation

### **Task 3: Implement Leverage Limit Enforcement** [Status: ✅ COMPLETE]
- [x] Implement `validateLeverage()`:
  - [x] Check per-trader leverage limit
  - [x] Check total leverage limit
  - [x] Calculate current leverage usage
  - [x] Return error if limit would be exceeded
- [x] Leverage calculation:
  - [x] Derive per-trader leverage from maximum active position leverage
  - [x] Derive total leverage from maximum leverage across all traders
  - [x] Validate: `traderLeverage <= maxLeveragePerTrader`
  - [x] Validate: `totalLeverage <= maxTotalLeverage`
- [x] Leverage tracking:
  - [x] Monitor leverage via live position snapshots
  - [x] Update automatically on position open/close
- [x] Unit tests for leverage validation

### **Task 4: Implement Exposure Monitoring** [Status: ✅ COMPLETE]
- [x] Implement `calculateExposure()`:
  - [x] For single trader: sum of all position sizes (with leverage)
  - [x] For all traders: sum of all trader exposures
  - [x] Return exposure in base currency
- [x] Exposure calculation logic:
  - [x] Per-trader exposure = sum of (position size * leverage)
  - [x] Total exposure = sum of all trader exposures
  - [x] Real-time updates on position changes
- [x] Exposure limits:
  - [x] Check: `traderExposure <= maxExposurePerTrader`
  - [x] Check: `totalExposure <= maxTotalExposure`
  - [x] Alert when approaching limits (logged warnings during monitoring)
- [x] Integration with PositionManager:
  - [x] Query active positions per trader
  - [x] Calculate exposure from positions
  - [x] Update on position changes via live snapshot lookups
- [x] Unit tests for exposure calculation

### **Task 5: Implement Stop-Loss Logic** [Status: ✅ COMPLETE]
- [x] Create `StopLossManager` class:
  - [x] `checkPositionStopLoss(position: Position): Boolean` - Check position stop-loss
  - [x] `checkTraderStopLoss(traderId: String): Boolean` - Check trader-level stop-loss
  - [x] `executeStopLoss(position: Position, reason: String): Result<Unit>` - Execute stop-loss
- [x] Position-level stop-loss:
  - [x] Leverage existing PositionManager triggers
  - [x] RiskManager provides configuration and thresholds via StopLossManager
- [x] Trader-level stop-loss:
  - [x] Calculate rolling 24h P&L for trader
  - [x] Check if P&L <= -maxDailyLoss
  - [x] If triggered, stop all trader positions
  - [x] Flag trader via emergency stop handler
- [x] Stop-loss execution:
  - [x] Call PositionManager to close positions
  - [x] Log stop-loss execution
  - [x] Update risk metrics
- [x] Unit tests for stop-loss logic

### **Task 6: Implement Emergency Stop** [Status: ✅ COMPLETE]
- [x] Implement `emergencyStop()`:
  - [x] If traderId provided: stop specific trader
  - [x] If no traderId: stop all traders
  - [x] Close all open positions immediately
  - [x] Prevent new positions
  - [x] Log emergency stop event
- [x] Emergency stop triggers:
  - [x] Manual trigger (user/admin)
  - [x] System trigger (critical risk threshold)
  - [x] Error trigger (system errors)
- [x] Emergency stop recovery:
  - [x] Can be manually resumed
  - [x] Requires risk review before resuming
- [x] Integration with AITraderManager:
  - [x] Call `stopTrader()` for affected traders
  - [x] Prevent trader restart until cleared
- [x] Unit tests for emergency stop

### **Task 7: Implement Risk Scoring** [Status: ✅ COMPLETE]
- [x] Create `RiskScore` data class:
  - [x] `overallScore: Double` (0.0-1.0, higher = riskier)
  - [x] `budgetScore: Double` - Budget utilization risk
  - [x] `leverageScore: Double` - Leverage risk
  - [x] `exposureScore: Double` - Exposure risk
  - [x] `pnlScore: Double` - P&L risk
  - [x] `recommendation: RiskRecommendation` - Action recommendation
- [x] Create `RiskRecommendation` enum:
  - [x] `ALLOW` - Safe to proceed
  - [x] `WARN` - Proceed with caution
  - [x] `BLOCK` - Block operation
  - [x] `EMERGENCY_STOP` - Emergency stop required
- [x] Implement `calculateRiskScore()`:
  - [x] Calculate individual risk components
  - [x] Weight and combine scores
  - [x] Generate recommendation
  - [x] Return `RiskScore` object
- [x] Risk score usage:
  - [x] Used in `canOpenPosition()` decision
  - [x] Used in trader creation validation (ATP_ProdSpec_54)
  - [x] Used in risk monitoring dashboard
- [x] Unit tests for risk scoring

### **Task 8: Implement ATP_ProdSpec_54 Compliance** [Status: ✅ COMPLETE]
- [x] Trader creation prevention conditions:
  - [x] **Condition 1**: "No money available"
    - [x] Check: `totalBudget <= 0`
    - [x] Return error: `InsufficientFundsException`
  - [x] **Condition 2**: "Not enough money to buy commodity (risk/leverage considered)"
    - [x] Check: `requiredAmount * leverage > availableBudget`
    - [x] Return error: `InsufficientFundsException`
- [x] Integration with AITraderManager:
  - [x] Call `validateBudget()` before trader creation
  - [x] Block creation if validation fails
  - [x] Return clear error messages
- [x] Validation flow:
  1. User attempts to create trader
  2. AITraderManager calls RiskManager.validateTraderCreation()
  3. RiskManager checks conditions
  4. If fails, return error (prevents creation)
  5. If passes, allow creation
- [x] Unit tests for ATP_ProdSpec_54 compliance

### **Task 9: Implement Risk Monitoring** [Status: ✅ COMPLETE]
- [x] Create background monitoring coroutine:
  - [x] Periodically check all risk metrics
  - [x] Calculate risk scores for all traders
  - [x] Check for limit violations
  - [x] Alert on risk issues (logging)
- [x] Monitoring metrics:
  - [x] Budget utilization
  - [x] Leverage usage
  - [x] Exposure levels
  - [x] P&L trends
  - [x] Stop-loss triggers
- [x] Risk alerts:
  - [x] Warning when approaching limits (80% threshold)
  - [x] Critical alert when limits reached
  - [x] Emergency alert when limits exceeded
- [x] Monitoring interval:
  - [x] Configurable (default: 10 seconds)
  - [x] Adjust based on system load
- [x] Unit tests for risk monitoring

### **Task 10: Testing** [Status: 🚧 IN PROGRESS]
- [x] Write unit tests for `RiskManager`:
  - [x] Budget validation (success, insufficient funds, no money)
  - [x] Leverage validation (success, limit exceeded)
  - [x] Exposure calculation
  - [x] Stop-loss logic
  - [x] Emergency stop
  - [x] Risk scoring
  - [x] ATP_ProdSpec_54 compliance (all conditions)
- [x] Risk monitoring
- [x] Thread-safety (concurrent access)
- [x] Write integration tests:
  - [x] RiskManager with PositionManager
  - [x] Trader creation prevention scenarios
  - [x] End-to-end risk management flow
- [x] Negative test cases:
  - [x] Test all ATP_ProdSpec_54 prevention conditions
  - [x] Test limit enforcement
  - [x] Test error scenarios
- [x] Verify all tests pass: `./gradlew test`
- 🔄 Coverage snapshot (Nov 7, 2025): `RiskManager` 73 % lines, `StopLossManager` 82 %, trader package aggregate 60 %; additional scenarios queued to boost RiskManager >90 %.

### **Task 11: Documentation** [Status: ✅ COMPLETE]
- [x] Add comprehensive KDoc to all classes
- [x] Create `RISK_MANAGER_GUIDE.md`:
  - [x] Architecture overview
  - [x] Risk management explanation
  - [x] Budget and leverage limits
  - [x] Exposure monitoring
  - [x] Stop-loss management
  - [x] Emergency stop procedures
  - [x] ATP_ProdSpec_54 compliance details
  - [x] Usage examples
  - [x] Troubleshooting guide
- [x] Update relevant documentation files

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
1. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/RiskManager.kt`
2. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/RiskModels.kt`
3. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/StopLossManager.kt`

### **Test Files**
1. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/traders/RiskManagerTest.kt`

### **Documentation**
- ✅ `Development_Handbook/RISK_MANAGER_GUIDE.md`

---

## 🎯 **Success Criteria**

| Criterion | Status | Verification Method |
|-----------|--------|---------------------|
| RiskManager implemented with all operations | ⏳ | File exists, unit tests pass |
| Budget validation working (ATP_ProdSpec_54) | ⏳ | Unit tests pass, negative test cases |
| Leverage limit enforcement working | ⏳ | Unit tests pass, negative test cases |
| Exposure monitoring working | ⏳ | Unit tests pass, integration tests |
| Stop-loss logic working | ⏳ | Unit tests pass, integration tests |
| Emergency stop functionality working | ⏳ | Unit tests pass, integration tests |
| ATP_ProdSpec_54 compliance verified | ⏳ | Comprehensive test cases |
| All tests pass | ⏳ | `./gradlew test` |
| Build succeeds | ⏳ | `./gradlew build` |
| CI pipeline passes | ⏳ | GitHub Actions |
| Code coverage >80% (>90% for critical paths) | ⏳ | Coverage report |
| Documentation complete | ⏳ | Documentation review |

---

## 🔧 **Key Technologies**

| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin Coroutines | 1.7+ | Async operations, risk monitoring |
| PositionManager (Issue #13) | - | Position data for exposure calculations |
| AITraderManager (Issue #12) | - | Trader lifecycle management |

---

## ⏱️ **Estimated Timeline**

| Task | Estimated Time |
|------|---------------|
| Task 1: Design Architecture | 3 hours |
| Task 2: Budget Validation | 4 hours |
| Task 3: Leverage Enforcement | 3 hours |
| Task 4: Exposure Monitoring | 3 hours |
| Task 5: Stop-Loss Logic | 3 hours |
| Task 6: Emergency Stop | 2 hours |
| Task 7: Risk Scoring | 3 hours |
| Task 8: ATP_ProdSpec_54 Compliance | 3 hours |
| Task 9: Risk Monitoring | 2 hours |
| Task 10: Testing | 8 hours |
| Task 11: Documentation | 3 hours |
| Task 12: Build & Commit | 2 hours |
| **Total** | **~2-3 days** |

---

## 🔄 **Dependencies**

### **Depends On** (Must be complete first)
- Issue #11: AI Trader Core ✅ (for TradingSignal validation)
- Issue #13: Position Manager ✅ (for position data and exposure calculations)

### **Blocks** (Cannot start until this is done)
- None (allows Issue #15 to proceed)

### **Related** (Related but not blocking)
- Issue #12: AI Trader Manager (integration for trader creation validation)
- Epic 1: Database, Configuration

---

## ⚠️ **Risks & Considerations**

| Risk | Impact | Mitigation Strategy |
|------|--------|---------------------|
| Risk calculation errors | High | Comprehensive testing, known test cases, peer review |
| Compliance gaps | High | Detailed ATP_ProdSpec_54 test cases, validation |
| Performance with many positions | Medium | Optimize calculations, caching, periodic updates |
| Thread-safety issues | High | Use Mutex, thorough concurrency testing |

---

**Issue Created**: November 5, 2025  
**Priority**: P0 (Critical)  
**Estimated Effort**: 2-3 days  
**Status**: 🚧 **IN PROGRESS**

