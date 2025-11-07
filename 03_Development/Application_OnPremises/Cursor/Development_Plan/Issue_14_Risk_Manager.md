# Issue #14: Risk Manager

**Status**: 📋 **PLANNED**  
**Assigned**: AI Assistant  
**Created**: November 5, 2025  
**Started**: Not Started  
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

### **Task 1: Design RiskManager Architecture** [Status: ⏳ PENDING]
- [ ] Create `RiskManager` class:
  - [ ] Properties: `totalBudget: BigDecimal`, `allocatedBudget: BigDecimal`, `riskConfig: RiskConfig`
  - [ ] Dependencies: `PositionManager`, `AITraderManager` (optional for now)
  - [ ] Thread-safety with `Mutex` for concurrent access
- [ ] Define core operations:
  - [ ] `validateBudget(requiredAmount: BigDecimal, traderId: String?): Result<Unit>` - Validate budget
  - [ ] `validateLeverage(leverage: Int, traderId: String?): Result<Unit>` - Validate leverage
  - [ ] `canOpenPosition(size: BigDecimal, leverage: Int, traderId: String): Result<Boolean>` - Check if position can be opened
  - [ ] `calculateExposure(traderId: String?): BigDecimal` - Calculate exposure
  - [ ] `calculateTotalExposure(): BigDecimal` - Calculate total system exposure
  - [ ] `checkRiskLimits(traderId: String): RiskCheckResult` - Comprehensive risk check
  - [ ] `emergencyStop(traderId: String?): Result<Unit>` - Emergency stop
- [ ] Create `RiskConfig` data class:
  - [ ] `maxTotalBudget: BigDecimal` - Maximum total budget
  - [ ] `maxLeveragePerTrader: Int` - Max leverage per trader
  - [ ] `maxTotalLeverage: Int` - Max total leverage across all traders
  - [ ] `maxExposurePerTrader: BigDecimal` - Max exposure per trader
  - [ ] `maxTotalExposure: BigDecimal` - Max total exposure
  - [ ] `maxDailyLoss: BigDecimal` - Max daily loss limit
  - [ ] `stopLossPercentage: Double` - Default stop-loss percentage
- [ ] Document architecture in KDoc

### **Task 2: Implement Budget Validation** [Status: ⏳ PENDING]
- [ ] Implement `validateBudget()`:
  - [ ] Check if required amount is available
  - [ ] Check if allocating would exceed total budget
  - [ ] Check per-trader budget allocation
  - [ ] Return error if validation fails (ATP_ProdSpec_54: "No money available")
  - [ ] Return error if insufficient funds (ATP_ProdSpec_54: "Not enough money to buy commodity")
- [ ] Budget tracking:
  - [ ] Track allocated budget per trader
  - [ ] Track total allocated budget
  - [ ] Update on position open/close
  - [ ] Handle budget release on position close
- [ ] Budget validation logic:
  - [ ] `availableBudget = totalBudget - allocatedBudget`
  - [ ] `requiredWithLeverage = requiredAmount * leverage`
  - [ ] Validate: `requiredWithLeverage <= availableBudget`
  - [ ] Validate: `requiredWithLeverage <= maxExposurePerTrader`
- [ ] Unit tests for budget validation

### **Task 3: Implement Leverage Limit Enforcement** [Status: ⏳ PENDING]
- [ ] Implement `validateLeverage()`:
  - [ ] Check per-trader leverage limit
  - [ ] Check total leverage limit
  - [ ] Calculate current leverage usage
  - [ ] Return error if limit would be exceeded
- [ ] Leverage calculation:
  - [ ] Per-trader leverage = sum of (position size * leverage) / allocated budget
  - [ ] Total leverage = sum of all trader leverages
  - [ ] Validate: `traderLeverage <= maxLeveragePerTrader`
  - [ ] Validate: `totalLeverage <= maxTotalLeverage`
- [ ] Leverage tracking:
  - [ ] Track leverage per trader
  - [ ] Track total leverage
  - [ ] Update on position open/close
- [ ] Unit tests for leverage validation

### **Task 4: Implement Exposure Monitoring** [Status: ⏳ PENDING]
- [ ] Implement `calculateExposure()`:
  - [ ] For single trader: sum of all position sizes (with leverage)
  - [ ] For all traders: sum of all trader exposures
  - [ ] Return exposure in base currency
- [ ] Exposure calculation logic:
  - [ ] Per-trader exposure = sum of (position size * leverage)
  - [ ] Total exposure = sum of all trader exposures
  - [ ] Real-time updates on position changes
- [ ] Exposure limits:
  - [ ] Check: `traderExposure <= maxExposurePerTrader`
  - [ ] Check: `totalExposure <= maxTotalExposure`
  - [ ] Alert when approaching limits
- [ ] Integration with PositionManager:
  - [ ] Query active positions per trader
  - [ ] Calculate exposure from positions
  - [ ] Update on position changes
- [ ] Unit tests for exposure calculation

### **Task 5: Implement Stop-Loss Logic** [Status: ⏳ PENDING]
- [ ] Create `StopLossManager` class:
  - [ ] `checkPositionStopLoss(position: Position): Boolean` - Check position stop-loss
  - [ ] `checkTraderStopLoss(traderId: String): Boolean` - Check trader-level stop-loss
  - [ ] `executeStopLoss(position: Position, reason: String): Result<Unit>` - Execute stop-loss
- [ ] Position-level stop-loss:
  - [ ] Already implemented in PositionManager (Issue #13)
  - [ ] RiskManager provides configuration and thresholds
- [ ] Trader-level stop-loss:
  - [ ] Calculate total P&L for trader
  - [ ] Check if P&L <= -maxDailyLoss
  - [ ] If triggered, stop all trader positions
  - [ ] Disable trader (prevent new positions)
- [ ] Stop-loss execution:
  - [ ] Call PositionManager to close positions
  - [ ] Log stop-loss execution
  - [ ] Update risk metrics
- [ ] Unit tests for stop-loss logic

### **Task 6: Implement Emergency Stop** [Status: ⏳ PENDING]
- [ ] Implement `emergencyStop()`:
  - [ ] If traderId provided: stop specific trader
  - [ ] If no traderId: stop all traders
  - [ ] Close all open positions immediately
  - [ ] Prevent new positions
  - [ ] Log emergency stop event
- [ ] Emergency stop triggers:
  - [ ] Manual trigger (user/admin)
  - [ ] System trigger (critical risk threshold)
  - [ ] Error trigger (system errors)
- [ ] Emergency stop recovery:
  - [ ] Can be manually resumed
  - [ ] Requires risk review before resuming
- [ ] Integration with AITraderManager:
  - [ ] Call `stopTrader()` for affected traders
  - [ ] Prevent trader restart until cleared
- [ ] Unit tests for emergency stop

### **Task 7: Implement Risk Scoring** [Status: ⏳ PENDING]
- [ ] Create `RiskScore` data class:
  - [ ] `overallScore: Double` (0.0-1.0, higher = riskier)
  - [ ] `budgetScore: Double` - Budget utilization risk
  - [ ] `leverageScore: Double` - Leverage risk
  - [ ] `exposureScore: Double` - Exposure risk
  - [ ] `pnlScore: Double` - P&L risk
  - [ ] `recommendation: RiskRecommendation` - Action recommendation
- [ ] Create `RiskRecommendation` enum:
  - [ ] `ALLOW` - Safe to proceed
  - [ ] `WARN` - Proceed with caution
  - [ ] `BLOCK` - Block operation
  - [ ] `EMERGENCY_STOP` - Emergency stop required
- [ ] Implement `calculateRiskScore()`:
  - [ ] Calculate individual risk components
  - [ ] Weight and combine scores
  - [ ] Generate recommendation
  - [ ] Return `RiskScore` object
- [ ] Risk score usage:
  - [ ] Used in `canOpenPosition()` decision
  - [ ] Used in trader creation validation (ATP_ProdSpec_54)
  - [ ] Used in risk monitoring dashboard
- [ ] Unit tests for risk scoring

### **Task 8: Implement ATP_ProdSpec_54 Compliance** [Status: ⏳ PENDING]
- [ ] Trader creation prevention conditions:
  - [ ] **Condition 1**: "No money available"
    - [ ] Check: `totalBudget <= 0`
    - [ ] Return error: `InsufficientFundsException`
  - [ ] **Condition 2**: "Not enough money to buy commodity (risk/leverage considered)"
    - [ ] Check: `requiredAmount * leverage > availableBudget`
    - [ ] Return error: `InsufficientFundsException`
- [ ] Integration with AITraderManager:
  - [ ] Call `validateBudget()` before trader creation
  - [ ] Block creation if validation fails
  - [ ] Return clear error messages
- [ ] Validation flow:
  1. User attempts to create trader
  2. AITraderManager calls RiskManager.validateBudget()
  3. RiskManager checks conditions
  4. If fails, return error (prevents creation)
  5. If passes, allow creation
- [ ] Unit tests for ATP_ProdSpec_54 compliance

### **Task 9: Implement Risk Monitoring** [Status: ⏳ PENDING]
- [ ] Create background monitoring coroutine:
  - [ ] Periodically check all risk metrics
  - [ ] Calculate risk scores for all traders
  - [ ] Check for limit violations
  - [ ] Alert on risk issues (logging)
- [ ] Monitoring metrics:
  - [ ] Budget utilization
  - [ ] Leverage usage
  - [ ] Exposure levels
  - [ ] P&L trends
  - [ ] Stop-loss triggers
- [ ] Risk alerts:
  - [ ] Warning when approaching limits (80% threshold)
  - [ ] Critical alert when limits reached
  - [ ] Emergency alert when limits exceeded
- [ ] Monitoring interval:
  - [ ] Configurable (default: 10 seconds)
  - [ ] Adjust based on system load
- [ ] Unit tests for risk monitoring

### **Task 10: Testing** [Status: ⏳ PENDING]
- [ ] Write unit tests for `RiskManager`:
  - [ ] Budget validation (success, insufficient funds, no money)
  - [ ] Leverage validation (success, limit exceeded)
  - [ ] Exposure calculation
  - [ ] Stop-loss logic
  - [ ] Emergency stop
  - [ ] Risk scoring
  - [ ] ATP_ProdSpec_54 compliance (all conditions)
  - [ ] Risk monitoring
  - [ ] Thread-safety (concurrent access)
- [ ] Write integration tests:
  - [ ] RiskManager with PositionManager
  - [ ] Trader creation prevention scenarios
  - [ ] End-to-end risk management flow
- [ ] Negative test cases:
  - [ ] Test all ATP_ProdSpec_54 prevention conditions
  - [ ] Test limit enforcement
  - [ ] Test error scenarios
- [ ] Verify all tests pass: `./gradlew test`
- [ ] Code coverage meets targets (>80%, >90% for critical paths)

### **Task 11: Documentation** [Status: ⏳ PENDING]
- [ ] Add comprehensive KDoc to all classes
- [ ] Create `RISK_MANAGER_GUIDE.md`:
  - [ ] Architecture overview
  - [ ] Risk management explanation
  - [ ] Budget and leverage limits
  - [ ] Exposure monitoring
  - [ ] Stop-loss management
  - [ ] Emergency stop procedures
  - [ ] ATP_ProdSpec_54 compliance details
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
1. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/RiskManager.kt`
2. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/RiskConfig.kt`
3. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/RiskScore.kt`
4. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/StopLossManager.kt`

### **Test Files**
1. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/traders/RiskManagerTest.kt`

### **Documentation**
- `Development_Handbook/RISK_MANAGER_GUIDE.md`

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
- Issue #11: AI Trader Core ⏳ (for TradingSignal validation)
- Issue #13: Position Manager ⏳ (for position data and exposure calculations)

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
**Status**: 📋 **PLANNED**

