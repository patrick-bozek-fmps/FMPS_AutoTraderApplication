# Issue #11: AI Trader Core

**Status**: 📋 **PLANNED**  
**Assigned**: AI Assistant  
**Created**: November 5, 2025  
**Started**: Not Started  
**Completed**: Not Completed  
**Duration**: 3-4 days (estimated)  
**Epic**: Epic 3 (AI Trading Engine)  
**Priority**: P0 (Critical - Blocks Issues #12, #13, #14)  
**Dependencies**: Epic 1 ✅ (Data Models, Database, Config), Epic 2 ✅ (Exchange Connectors, Technical Indicators)

> **NOTE**: This is the foundation issue for Epic 3. Must be completed before starting Issues #12, #13, and #14. Implements the core trading engine with strategy logic and signal generation.

---

## 📋 **Objective**

Design and implement the core AI Trader class that processes market data, generates trading signals using technical indicators, and executes trading strategies (trend following, mean reversion, breakout). The trader must support configuration management, state management, and integration with exchange connectors for automated trading decisions.

---

## 🎯 **Goals**

1. **Core Trader Class**: Implement `AITrader` with configuration, state, and lifecycle management
2. **Trading Strategies**: Implement 3 trading strategies (trend following, mean reversion, breakout)
3. **Signal Generation**: Generate buy/sell/hold signals based on technical indicators and strategies
4. **Market Data Processing**: Process candlestick data and calculate indicators in real-time
5. **Strategy Selection**: Allow strategy selection and switching at runtime
6. **Performance Metrics**: Track trading performance and metrics
7. **Integration**: Seamless integration with exchange connectors and technical indicators
8. **Testing**: Comprehensive unit tests with >80% coverage

---

## 📝 **Task Breakdown**

### **Task 1: Design AITrader Core Architecture** [Status: ⏳ PENDING]
- [ ] Create `AITraderConfig` data class with all ATP_ProdSpec_53 parameters:
  - [ ] `id: String` - Unique trader identifier
  - [ ] `name: String` - Trader name
  - [ ] `exchange: Exchange` - Exchange enum (BINANCE, BITGET)
  - [ ] `symbol: String` - Trading pair (e.g., "BTCUSDT")
  - [ ] `virtualMoney: Boolean` - Virtual/real money flag (v1.0: always true)
  - [ ] `maxStakeAmount: BigDecimal` - Maximum amount to stake
  - [ ] `maxRiskLevel: Int` - Maximum risk/leverage level (1-10)
  - [ ] `maxTradingDuration: Duration` - Maximum trading duration
  - [ ] `minReturnPercent: Double` - Minimum return/profit target
  - [ ] `strategy: TradingStrategy` - Selected strategy enum
  - [ ] `candlestickInterval: CandlestickInterval` - Data interval (1m, 5m, 1h, etc.)
- [ ] Create `AITraderState` enum (IDLE, STARTING, RUNNING, PAUSED, STOPPING, STOPPED, ERROR)
- [ ] Create `AITrader` class structure:
  - [ ] Properties: config, state, exchangeConnector, positionManager, riskManager
  - [ ] Lifecycle methods: `start()`, `stop()`, `pause()`, `resume()`
  - [ ] State management with thread-safety
- [ ] Create `TradingSignal` data class:
  - [ ] `action: TradeAction` (BUY, SELL, HOLD, CLOSE)
  - [ ] `confidence: Double` (0.0-1.0)
  - [ ] `reason: String` - Why this signal was generated
  - [ ] `timestamp: Instant`
  - [ ] `indicatorValues: Map<String, Any>` - Supporting indicator values
- [ ] Document architecture in KDoc

### **Task 2: Implement Trading Strategy Interface** [Status: ⏳ PENDING]
- [ ] Create `ITradingStrategy` interface:
  - [ ] `generateSignal(candles: List<Candlestick>, indicators: Map<String, Any>): TradingSignal`
  - [ ] `getName(): String` - Strategy name
  - [ ] `getDescription(): String` - Strategy description
  - [ ] `getRequiredIndicators(): List<String>` - Required indicators (e.g., ["RSI", "MACD"])
  - [ ] `validateConfig(config: AITraderConfig): Boolean` - Validate strategy-specific config
  - [ ] `reset()` - Clear internal state
- [ ] Create `TradingStrategy` enum (TREND_FOLLOWING, MEAN_REVERSION, BREAKOUT)
- [ ] Create `StrategyFactory` for creating strategy instances
- [ ] Add strategy selection logic to `AITrader`
- [ ] Unit tests for interface and factory

### **Task 3: Implement Trend Following Strategy** [Status: ⏳ PENDING]
- [ ] Create `TrendFollowingStrategy` implementing `ITradingStrategy`
- [ ] Strategy logic:
  - [ ] Use SMA (short period) and SMA (long period) for trend detection
  - [ ] BUY signal: Short SMA crosses above long SMA (golden cross)
  - [ ] SELL signal: Short SMA crosses below long SMA (death cross)
  - [ ] Use MACD for confirmation
  - [ ] Use RSI to avoid overbought/oversold entries
  - [ ] Confidence based on indicator alignment
- [ ] Configuration parameters:
  - [ ] `smaShortPeriod: Int` (default: 9)
  - [ ] `smaLongPeriod: Int` (default: 21)
  - [ ] `rsiPeriod: Int` (default: 14)
  - [ ] `rsiOverbought: Double` (default: 70.0)
  - [ ] `rsiOversold: Double` (default: 30.0)
- [ ] Integration with indicators from Epic 2:
  - [ ] Use `SMAIndicator`, `EMAIndicator`, `MACDIndicator`, `RSIIndicator`
- [ ] Handle edge cases (insufficient data, flat markets)
- [ ] **15+ comprehensive unit tests** with known scenarios:
  - [ ] Golden cross (buy signal)
  - [ ] Death cross (sell signal)
  - [ ] RSI filter (overbought/oversold)
  - [ ] MACD confirmation
  - [ ] Insufficient data handling
  - [ ] Flat market (no signal)

### **Task 4: Implement Mean Reversion Strategy** [Status: ⏳ PENDING]
- [ ] Create `MeanReversionStrategy` implementing `ITradingStrategy`
- [ ] Strategy logic:
  - [ ] Use Bollinger Bands for mean reversion detection
  - [ ] BUY signal: Price touches lower band (oversold) + RSI < 30
  - [ ] SELL signal: Price touches upper band (overbought) + RSI > 70
  - [ ] Use %B indicator for position in bands
  - [ ] Confidence based on how far price is from mean
- [ ] Configuration parameters:
  - [ ] `bbPeriod: Int` (default: 20)
  - [ ] `bbStdDev: Double` (default: 2.0)
  - [ ] `rsiPeriod: Int` (default: 14)
  - [ ] `rsiOversold: Double` (default: 30.0)
  - [ ] `rsiOverbought: Double` (default: 70.0)
- [ ] Integration with indicators:
  - [ ] Use `BollingerBandsIndicator`, `RSIIndicator`
- [ ] Handle edge cases (squeeze, bands too wide)
- [ ] **15+ comprehensive unit tests**:
  - [ ] Lower band touch (buy signal)
  - [ ] Upper band touch (sell signal)
  - [ ] RSI confirmation
  - [ ] Squeeze detection (no signal)
  - [ ] %B calculation
  - [ ] Mean reversion logic

### **Task 5: Implement Breakout Strategy** [Status: ⏳ PENDING]
- [ ] Create `BreakoutStrategy` implementing `ITradingStrategy`
- [ ] Strategy logic:
  - [ ] Use Bollinger Bands for volatility detection
  - [ ] Use volume (if available) or price momentum
  - [ ] BUY signal: Price breaks above upper band with momentum
  - [ ] SELL signal: Price breaks below lower band with momentum
  - [ ] Use MACD for momentum confirmation
  - [ ] Avoid false breakouts (squeeze detection)
- [ ] Configuration parameters:
  - [ ] `bbPeriod: Int` (default: 20)
  - [ ] `bbStdDev: Double` (default: 2.0)
  - [ ] `breakoutThreshold: Double` (default: 1.05) - % above/below band
  - [ ] `momentumPeriod: Int` (default: 14)
- [ ] Integration with indicators:
  - [ ] Use `BollingerBandsIndicator`, `MACDIndicator`
- [ ] Handle edge cases (false breakouts, low volatility)
- [ ] **15+ comprehensive unit tests**:
  - [ ] Upper breakout (buy signal)
  - [ ] Lower breakout (sell signal)
  - [ ] MACD momentum confirmation
  - [ ] False breakout detection
  - [ ] Squeeze (no signal)
  - [ ] Breakout threshold validation

### **Task 6: Implement Market Data Processing Pipeline** [Status: ⏳ PENDING]
- [ ] Create `MarketDataProcessor` class:
  - [ ] `processCandlesticks(candles: List<Candlestick>): ProcessedMarketData`
  - [ ] Calculate all required indicators for active strategy
  - [ ] Cache indicator values to avoid redundant calculations
  - [ ] Handle missing/incomplete data gracefully
- [ ] Create `ProcessedMarketData` data class:
  - [ ] `candles: List<Candlestick>`
  - [ ] `indicators: Map<String, Any>` - Indicator values by name
  - [ ] `latestPrice: BigDecimal`
  - [ ] `timestamp: Instant`
- [ ] Integration with exchange connectors:
  - [ ] Subscribe to candlestick updates via WebSocket (if available)
  - [ ] Fallback to periodic polling if WebSocket unavailable
  - [ ] Handle connection errors gracefully
- [ ] Add data validation:
  - [ ] Minimum data points required
  - [ ] Data quality checks (gaps, outliers)
  - [ ] Timestamp validation
- [ ] Unit tests for data processing pipeline

### **Task 7: Implement Signal Generation Logic** [Status: ⏳ PENDING]
- [ ] Create `SignalGenerator` class:
  - [ ] `generateSignal(processedData: ProcessedMarketData, strategy: ITradingStrategy, currentPosition: Position?): TradingSignal`
  - [ ] Combine strategy signals with risk management checks
  - [ ] Apply confidence scoring
  - [ ] Log signal generation for debugging
- [ ] Signal generation flow:
  1. Get processed market data
  2. Call strategy's `generateSignal()` method
  3. Apply filters (risk limits, position limits)
  4. Calculate final confidence score
  5. Return `TradingSignal` with metadata
- [ ] Confidence calculation:
  - [ ] Base confidence from strategy
  - [ ] Adjust based on indicator alignment
  - [ ] Adjust based on market conditions
  - [ ] Minimum confidence threshold (configurable)
- [ ] Signal filtering:
  - [ ] Check if signal conflicts with current position
  - [ ] Apply risk manager constraints (if available)
  - [ ] Apply position manager constraints (if available)
- [ ] Unit tests for signal generation logic

### **Task 8: Implement AITrader Main Class** [Status: ⏳ PENDING]
- [ ] Implement `AITrader` class:
  - [ ] Constructor: `AITrader(config: AITraderConfig, exchangeConnector: IExchangeConnector, positionManager: PositionManager?, riskManager: RiskManager?)`
  - [ ] Properties:
    - [ ] `config: AITraderConfig`
    - [ ] `state: AITraderState`
    - [ ] `exchangeConnector: IExchangeConnector`
    - [ ] `strategy: ITradingStrategy`
    - [ ] `marketDataProcessor: MarketDataProcessor`
    - [ ] `signalGenerator: SignalGenerator`
    - [ ] `positionManager: PositionManager?` (nullable - added in Issue #13)
    - [ ] `riskManager: RiskManager?` (nullable - added in Issue #14)
  - [ ] Lifecycle methods:
    - [ ] `start(): Result<Unit>` - Start trading loop
    - [ ] `stop(): Result<Unit>` - Stop trading loop
    - [ ] `pause(): Result<Unit>` - Pause trading
    - [ ] `resume(): Result<Unit>` - Resume trading
    - [ ] `getState(): AITraderState` - Get current state
    - [ ] `updateConfig(newConfig: AITraderConfig): Result<Unit>` - Update configuration
  - [ ] Trading loop (runs in coroutine):
    1. Fetch market data
    2. Process market data
    3. Generate signal
    4. Execute signal (if valid) - delegated to PositionManager (Issue #13)
    5. Update performance metrics
    6. Sleep until next interval
  - [ ] Error handling:
    - [ ] Retry on transient errors
    - [ ] Log errors with context
    - [ ] Transition to ERROR state on critical failures
    - [ ] Recovery mechanism
  - [ ] Thread-safety:
    - [ ] Use `Mutex` for state changes
    - [ ] Use `AtomicReference` for state
    - [ ] Proper coroutine cancellation
- [ ] Integration points:
  - [ ] Exchange connector for market data
  - [ ] Position manager for position management (when available)
  - [ ] Risk manager for risk checks (when available)
  - [ ] Technical indicators from Epic 2
- [ ] Unit tests for `AITrader` class:
  - [ ] State transitions
  - [ ] Configuration updates
  - [ ] Trading loop execution
  - [ ] Error handling
  - [ ] Thread-safety

### **Task 9: Implement Performance Metrics** [Status: ⏳ PENDING]
- [ ] Create `AITraderMetrics` data class:
  - [ ] `totalTrades: Int`
  - [ ] `winningTrades: Int`
  - [ ] `losingTrades: Int`
  - [ ] `totalProfit: BigDecimal`
  - [ ] `totalLoss: BigDecimal`
  - [ ] `netProfit: BigDecimal`
  - [ ] `winRate: Double` - winningTrades / totalTrades
  - [ ] `averageProfit: BigDecimal`
  - [ ] `averageLoss: BigDecimal`
  - [ ] `profitFactor: Double` - totalProfit / totalLoss (if loss > 0)
  - [ ] `maxDrawdown: BigDecimal`
  - [ ] `sharpeRatio: Double?` (optional - complex calculation)
  - [ ] `startTime: Instant`
  - [ ] `uptime: Duration`
- [ ] Add metrics tracking to `AITrader`:
  - [ ] Update metrics on trade execution
  - [ ] Calculate derived metrics periodically
  - [ ] Reset metrics on restart
- [ ] Add `getMetrics(): AITraderMetrics` method
- [ ] Unit tests for metrics calculation

### **Task 10: Testing** [Status: ⏳ PENDING]
- [ ] Write unit tests for `AITrader` class:
  - [ ] Initialization and configuration
  - [ ] State management (IDLE → STARTING → RUNNING → STOPPING → STOPPED)
  - [ ] Configuration updates
  - [ ] Error handling and recovery
  - [ ] Thread-safety (concurrent access)
  - [ ] Trading loop execution (mocked)
- [ ] Write unit tests for `TrendFollowingStrategy`:
  - [ ] 15+ test scenarios (see Task 3)
- [ ] Write unit tests for `MeanReversionStrategy`:
  - [ ] 15+ test scenarios (see Task 4)
- [ ] Write unit tests for `BreakoutStrategy`:
  - [ ] 15+ test scenarios (see Task 5)
- [ ] Write unit tests for `MarketDataProcessor`:
  - [ ] Indicator calculation
  - [ ] Data validation
  - [ ] Caching behavior
  - [ ] Error handling
- [ ] Write unit tests for `SignalGenerator`:
  - [ ] Signal generation logic
  - [ ] Confidence calculation
  - [ ] Signal filtering
- [ ] Write integration tests (if applicable):
  - [ ] AITrader with mock exchange connector
  - [ ] Strategy execution with real indicator calculations
- [ ] Verify all tests pass: `./gradlew test`
- [ ] Code coverage meets targets (>80% for core classes)

### **Task 11: Documentation** [Status: ⏳ PENDING]
- [ ] Add comprehensive KDoc to all classes:
  - [ ] `AITrader` class
  - [ ] `ITradingStrategy` interface
  - [ ] All strategy implementations
  - [ ] `MarketDataProcessor`
  - [ ] `SignalGenerator`
  - [ ] `AITraderConfig`
  - [ ] `TradingSignal`
- [ ] Create `AI_TRADER_CORE_GUIDE.md` documentation:
  - [ ] Architecture overview
  - [ ] Strategy descriptions and usage
  - [ ] Configuration parameters
  - [ ] Signal generation explanation
  - [ ] Usage examples
  - [ ] Performance considerations
  - [ ] Troubleshooting guide
- [ ] Update relevant documentation files:
  - [ ] `Development_Plan_v2.md` (mark Issue #11 as complete)
  - [ ] `EPIC_3_STATUS.md` (update progress)

### **Task 12: Build & Commit** [Status: ⏳ PENDING]
- [ ] Run all tests: `./gradlew test`
- [ ] Build project: `./gradlew build`
- [ ] Fix any compilation errors
- [ ] Fix any test failures
- [ ] Code coverage verification (>80%)
- [ ] Commit changes with descriptive message
- [ ] Push to GitHub
- [ ] Verify CI pipeline passes
- [ ] Update this Issue file to reflect completion
- [ ] Update Development_Plan_v2.md

---

## 📦 **Deliverables**

### **New Files**
1. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/AITrader.kt` - Core trader class
2. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/AITraderConfig.kt` - Configuration data class
3. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/TradingSignal.kt` - Signal data class
4. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/AITraderState.kt` - State enum
5. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/AITraderMetrics.kt` - Metrics data class
6. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/strategies/ITradingStrategy.kt` - Strategy interface
7. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/strategies/TradingStrategy.kt` - Strategy enum
8. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/strategies/StrategyFactory.kt` - Strategy factory
9. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/strategies/TrendFollowingStrategy.kt` - Trend following implementation
10. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/strategies/MeanReversionStrategy.kt` - Mean reversion implementation
11. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/strategies/BreakoutStrategy.kt` - Breakout implementation
12. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/MarketDataProcessor.kt` - Market data processing
13. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/ProcessedMarketData.kt` - Processed data class
14. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/SignalGenerator.kt` - Signal generation logic

### **Test Files**
1. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/traders/AITraderTest.kt` - AITrader unit tests
2. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/traders/strategies/TrendFollowingStrategyTest.kt` - Trend following tests
3. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/traders/strategies/MeanReversionStrategyTest.kt` - Mean reversion tests
4. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/traders/strategies/BreakoutStrategyTest.kt` - Breakout tests
5. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/traders/MarketDataProcessorTest.kt` - Data processor tests
6. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/traders/SignalGeneratorTest.kt` - Signal generator tests

### **Documentation**
- `Development_Handbook/AI_TRADER_CORE_GUIDE.md` - Comprehensive guide (500+ lines)

---

## 🎯 **Success Criteria**

| Criterion | Status | Verification Method |
|-----------|--------|---------------------|
| AITrader class implemented with all lifecycle methods | ⏳ | File exists, unit tests pass |
| Three trading strategies implemented and tested | ⏳ | Files exist, 45+ strategy tests pass |
| Signal generation logic working correctly | ⏳ | Unit tests pass, manual verification |
| Market data processing pipeline functional | ⏳ | Unit tests pass, integration with indicators |
| Configuration management working | ⏳ | Unit tests pass, config validation tests |
| State management thread-safe | ⏳ | Concurrency tests pass |
| Performance metrics tracking implemented | ⏳ | Unit tests pass, metrics calculation tests |
| All tests pass | ⏳ | `./gradlew test` |
| Build succeeds | ⏳ | `./gradlew build` |
| CI pipeline passes | ⏳ | GitHub Actions green checkmark |
| Code coverage >80% | ⏳ | Coverage report |
| Documentation complete | ⏳ | Documentation review |

---

## 📊 **Test Coverage Approach**

### **What Will Be Tested**
✅ **Component-Level Unit Tests**:
- **AITrader**: 20+ tests (state management, lifecycle, configuration, error handling)
- **TrendFollowingStrategy**: 15+ tests (golden cross, death cross, RSI filter, MACD confirmation)
- **MeanReversionStrategy**: 15+ tests (Bollinger Bands, RSI confirmation, %B calculation)
- **BreakoutStrategy**: 15+ tests (breakout detection, momentum, false breakout prevention)
- **MarketDataProcessor**: 10+ tests (indicator calculation, data validation, caching)
- **SignalGenerator**: 10+ tests (signal generation, confidence calculation, filtering)

**Total**: 85+ tests ✅

✅ **Integration Tests** (if applicable):
- AITrader with mock exchange connector
- Strategy execution with real indicator calculations from Epic 2
- End-to-end signal generation flow

### **Test Strategy**
**Multi-tier coverage for production confidence**:
1. **Unit Tests**: All business logic, strategies, signal generation ✅
2. **Integration Tests**: AITrader with exchange connectors (mocked) ✅
3. **Edge Case Tests**: Error handling, insufficient data, edge conditions ✅

**Result**: ✅ All functionality covered through strategic test layering

---

## 🔧 **Key Technologies**

| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin | 1.9+ | Language |
| Kotlinx Coroutines | 1.7+ | Async operations, trading loop |
| Kotlinx Serialization | 1.6+ | Configuration serialization |
| JUnit 5 | 5.10+ | Unit testing |
| Mockk | 1.13+ | Mocking for tests |
| Technical Indicators (Epic 2) | - | RSI, MACD, SMA, EMA, Bollinger Bands |
| Exchange Connectors (Epic 2) | - | Market data, order execution |

**Add to `build.gradle.kts`** (if needed):
```kotlin
dependencies {
    // Already available from Epic 1 & Epic 2
    // Coroutines for async trading loop
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Testing
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
```

---

## 📊 **Architecture/Design**

```
┌─────────────────────────────────────────────────────────┐
│                     AITrader                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │   Config     │  │   State      │  │  Exchange    │ │
│  │  Management  │  │  Management  │  │  Connector   │ │
│  └──────────────┘  └──────────────┘  └──────────────┘ │
│         │                 │                 │          │
│         └─────────────────┴─────────────────┘          │
│                        │                              │
│                        ▼                              │
│         ┌──────────────────────────────┐              │
│         │   MarketDataProcessor        │              │
│         │  - Fetch candlesticks        │              │
│         │  - Calculate indicators      │              │
│         │  - Cache results             │              │
│         └──────────────────────────────┘              │
│                        │                              │
│                        ▼                              │
│         ┌──────────────────────────────┐              │
│         │      SignalGenerator         │              │
│         │  - Call strategy              │              │
│         │  - Apply filters              │              │
│         │  - Calculate confidence       │              │
│         └──────────────────────────────┘              │
│                        │                              │
│                        ▼                              │
│         ┌──────────────────────────────┐              │
│         │   ITradingStrategy          │              │
│         │  ┌────────┐  ┌────────┐    │              │
│         │  │ Trend  │  │  Mean  │    │              │
│         │  │Follow  │  │ Revert │    │              │
│         │  └────────┘  └────────┘    │              │
│         │  ┌────────┐                 │              │
│         │  │Breakout│                 │              │
│         │  └────────┘                 │              │
│         └──────────────────────────────┘              │
│                        │                              │
│                        ▼                              │
│         ┌──────────────────────────────┐              │
│         │    TradingSignal             │              │
│         │  (BUY/SELL/HOLD/CLOSE)       │              │
│         └──────────────────────────────┘              │
└─────────────────────────────────────────────────────────┘
```

---

## ⏱️ **Estimated Timeline**

| Task | Estimated Time |
|------|---------------|
| Task 1: Design AITrader Core Architecture | 4 hours |
| Task 2: Implement Trading Strategy Interface | 3 hours |
| Task 3: Implement Trend Following Strategy | 4 hours |
| Task 4: Implement Mean Reversion Strategy | 4 hours |
| Task 5: Implement Breakout Strategy | 4 hours |
| Task 6: Implement Market Data Processing Pipeline | 4 hours |
| Task 7: Implement Signal Generation Logic | 3 hours |
| Task 8: Implement AITrader Main Class | 6 hours |
| Task 9: Implement Performance Metrics | 2 hours |
| Task 10: Testing | 8 hours |
| Task 11: Documentation | 4 hours |
| Task 12: Build & Commit | 2 hours |
| **Total** | **~3-4 days** |

---

## 🔄 **Dependencies**

### **Depends On** (Must be complete first)
- ✅ Epic 1: Data Models, Database, Configuration Management
- ✅ Epic 2: Exchange Connectors, Technical Indicators

### **Blocks** (Cannot start until this is done)
- Issue #12: AI Trader Manager (depends on AITrader class)
- Issue #13: Position Manager (needs AITrader for integration)
- Issue #14: Risk Manager (needs AITrader for integration)

### **Related** (Related but not blocking)
- Issue #15: Pattern Storage System (can work in parallel, but AITrader will use patterns)

---

## 📚 **Resources**

### **Documentation**
- Technical Indicators Guide: `TECHNICAL_INDICATORS_GUIDE.md`
- Exchange Connector Guide: `EXCHANGE_CONNECTOR_GUIDE.md`
- Requirements: `02_ReqMgn/FMPS_AutoTraderApplication_Customer_Specification.md` (ATP_ProdSpec_53)

### **Reference Issues**
- Issue #5: Core Data Models (Candlestick, Order, Position models)
- Issue #7: Exchange Connector Framework (IExchangeConnector interface)
- Issue #10: Technical Indicators (RSI, MACD, SMA, EMA, Bollinger Bands)

---

## ⚠️ **Risks & Considerations**

| Risk | Impact | Mitigation Strategy |
|------|--------|---------------------|
| Strategy logic complexity | Medium | Start with simple strategies, iterate based on testing |
| Signal quality | High | Comprehensive testing with known scenarios, confidence thresholds |
| Performance with large datasets | Medium | Implement caching, optimize indicator calculations |
| Thread-safety issues | High | Use Kotlin coroutines, Mutex for state, thorough testing |
| Integration with PositionManager | Medium | Design interfaces clearly, use nullable dependencies initially |

---

## 📈 **Definition of Done**

- [ ] All tasks completed
- [ ] All subtasks checked off
- [ ] All deliverables created
- [ ] All success criteria met
- [ ] All tests written and passing (85+ tests)
- [ ] Code coverage meets targets (>80%)
- [ ] Documentation complete (AI_TRADER_CORE_GUIDE.md)
- [ ] All tests pass: `./gradlew test`
- [ ] Build succeeds: `./gradlew build`
- [ ] CI pipeline passes (GitHub Actions)
- [ ] Issue file updated to reflect completion
- [ ] Development_Plan_v2.md updated with progress
- [ ] EPIC_3_STATUS.md updated with progress
- [ ] Changes committed to Git
- [ ] Changes pushed to GitHub
- [ ] Issue closed

---

## 💡 **Notes & Learnings**

*To be filled during implementation*

---

## 📦 **Commit Strategy**

Follow the development workflow from `DEVELOPMENT_WORKFLOW.md`:

1. **Commit after major tasks** (not after every small change)
2. **Run tests before every commit**
3. **Wait for CI to pass** before proceeding
4. **Update documentation** as you go

Example commits:
```
feat: add AITrader core architecture and configuration (Issue #11 Task 1)
feat: implement ITradingStrategy interface and factory (Issue #11 Task 2)
feat: implement TrendFollowingStrategy (Issue #11 Task 3)
feat: implement MeanReversionStrategy (Issue #11 Task 4)
feat: implement BreakoutStrategy (Issue #11 Task 5)
feat: add MarketDataProcessor and SignalGenerator (Issue #11 Task 6-7)
feat: implement AITrader main class with trading loop (Issue #11 Task 8)
feat: add performance metrics tracking (Issue #11 Task 9)
test: add comprehensive unit tests for AITrader and strategies (Issue #11 Task 10)
docs: create AI_TRADER_CORE_GUIDE.md (Issue #11 Task 11)
feat: complete Issue #11 - AI Trader Core
```

---

**Issue Created**: November 5, 2025  
**Priority**: P0 (Critical)  
**Estimated Effort**: 3-4 days  
**Status**: 📋 **PLANNED**

---

**Next Steps**:
1. Review this plan
2. Begin Task 1: Design AITrader Core Architecture
3. Follow DEVELOPMENT_WORKFLOW.md throughout
4. Update status as progress is made

