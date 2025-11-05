# Issue #15: Pattern Storage System

**Status**: 📋 **PLANNED**  
**Assigned**: AI Assistant  
**Created**: November 5, 2025  
**Started**: Not Started  
**Completed**: Not Completed  
**Duration**: 3-4 days (estimated)  
**Epic**: Epic 3 (AI Trading Engine)  
**Priority**: P1 (High - Required for ATP_ProdSpec_55-56)  
**Dependencies**: Epic 1 ✅ (Database, PatternRepository), Issue #11 ⏳ (AI Trader Core)

> **NOTE**: Implements pattern storage and retrieval system for trading knowledge database per ATP_ProdSpec_55-56. Stores successful trades as patterns, enables pattern matching, and provides pattern learning capabilities.

---

## 📋 **Objective**

Implement `PatternService` class that stores successful trading patterns, queries patterns by criteria, performs pattern matching against current market conditions, calculates pattern relevance scores, tracks pattern performance, and implements pattern learning and pruning logic. The system enables AI traders to learn from past successful trades.

---

## 🎯 **Goals**

1. **Pattern Storage**: Store successful trades as patterns with metadata per ATP_ProdSpec_55-56
2. **Pattern Querying**: Query patterns by exchange, asset, conditions, and performance
3. **Pattern Matching**: Match current market conditions to stored patterns
4. **Pattern Relevance**: Calculate relevance scores for pattern matching
5. **Pattern Learning**: Automatically extract patterns from successful trades
6. **Pattern Performance**: Track success rate and performance per pattern
7. **Pattern Pruning**: Remove old, irrelevant, or underperforming patterns

---

## 📝 **Task Breakdown**

### **Task 1: Design Pattern Storage Architecture** [Status: ⏳ PENDING]
- [ ] Create `PatternService` class:
  - [ ] Dependencies: `PatternRepository` (Epic 1), `TradeHistoryRepository` (Epic 1)
  - [ ] Thread-safety with `Mutex` for concurrent access
- [ ] Define core operations:
  - [ ] `storePattern(pattern: TradingPattern): Result<String>` - Store pattern
  - [ ] `queryPatterns(criteria: PatternCriteria): List<TradingPattern>` - Query patterns
  - [ ] `matchPatterns(marketConditions: MarketConditions): List<MatchedPattern>` - Match patterns
  - [ ] `updatePatternPerformance(patternId: String, outcome: TradeOutcome): Result<Unit>` - Update performance
  - [ ] `prunePatterns(criteria: PruneCriteria): Result<Int>` - Prune patterns
  - [ ] `getPattern(patternId: String): TradingPattern?` - Get pattern by ID
- [ ] Create `TradingPattern` data class (extend existing from Epic 1 if needed):
  - [ ] `id: String` - Pattern ID
  - [ ] `name: String` - Pattern name
  - [ ] `exchange: Exchange` - Exchange
  - [ ] `symbol: String` - Trading pair
  - [ ] `conditions: Map<String, Any>` - Market conditions (indicators, prices, etc.)
  - [ ] `action: TradeAction` - Recommended action (BUY/SELL)
  - [ ] `confidence: Double` - Initial confidence (0.0-1.0)
  - [ ] `createdAt: Instant` - Creation timestamp
  - [ ] `lastUsedAt: Instant` - Last usage timestamp
  - [ ] `usageCount: Int` - Number of times used
  - [ ] `successCount: Int` - Number of successful uses
  - [ ] `successRate: Double` - Success rate (successCount / usageCount)
  - [ ] `averageReturn: BigDecimal` - Average return per use
- [ ] Document architecture in KDoc

### **Task 2: Implement Pattern Storage** [Status: ⏳ PENDING]
- [ ] Implement `storePattern()`:
  - [ ] Validate pattern data
  - [ ] Generate unique pattern ID (UUID)
  - [ ] Store in database via `PatternRepository`
  - [ ] Return pattern ID
- [ ] Pattern extraction from successful trades:
  - [ ] Create `extractPatternFromTrade(trade: ClosedPosition): TradingPattern` method
  - [ ] Extract market conditions from trade entry:
    - [ ] Indicator values (RSI, MACD, SMA, EMA, Bollinger Bands)
    - [ ] Price levels
    - [ ] Volume (if available)
    - [ ] Market trend
  - [ ] Extract trade outcome:
    - [ ] Action taken (BUY/SELL)
    - [ ] Entry price
    - [ ] Exit price
    - [ ] Profit/loss
  - [ ] Create pattern with metadata
- [ ] Pattern validation:
  - [ ] Ensure all required fields present
  - [ ] Validate indicator values
  - [ ] Validate confidence range
- [ ] Integration with TradeHistoryRepository:
  - [ ] Query successful trades
  - [ ] Extract patterns automatically (optional - can be manual trigger)
- [ ] Unit tests for pattern storage

### **Task 3: Implement Pattern Querying** [Status: ⏳ PENDING]
- [ ] Create `PatternCriteria` data class:
  - [ ] `exchange: Exchange?` - Filter by exchange
  - [ ] `symbol: String?` - Filter by symbol
  - [ ] `minSuccessRate: Double?` - Minimum success rate
  - [ ] `minUsageCount: Int?` - Minimum usage count
  - [ ] `minConfidence: Double?` - Minimum confidence
  - [ ] `maxAge: Duration?` - Maximum age
  - [ ] `action: TradeAction?` - Filter by action
- [ ] Implement `queryPatterns()`:
  - [ ] Build query from criteria
  - [ ] Execute query via `PatternRepository`
  - [ ] Filter results in-memory if needed
  - [ ] Sort by relevance (success rate, usage count, etc.)
  - [ ] Return list of patterns
- [ ] Query optimization:
  - [ ] Use database indexes (from Epic 1)
  - [ ] Cache frequently queried patterns (optional)
  - [ ] Limit results to top N (configurable)
- [ ] Integration with PatternRepository:
  - [ ] Use existing repository methods
  - [ ] Add new methods if needed (queryByCriteria, etc.)
- [ ] Unit tests for pattern querying

### **Task 4: Implement Pattern Matching** [Status: ⏳ PENDING]
- [ ] Create `MarketConditions` data class:
  - [ ] `exchange: Exchange`
  - [ ] `symbol: String`
  - [ ] `currentPrice: BigDecimal`
  - [ ] `indicators: Map<String, Any>` - Current indicator values
  - [ ] `candlesticks: List<Candlestick>` - Recent candlesticks
  - [ ] `timestamp: Instant`
- [ ] Create `MatchedPattern` data class:
  - [ ] `pattern: TradingPattern` - The matched pattern
  - [ ] `relevanceScore: Double` (0.0-1.0) - How well it matches
  - [ ] `matchedConditions: Map<String, Any>` - Which conditions matched
  - [ ] `confidence: Double` - Combined confidence (pattern + match)
- [ ] Implement `matchPatterns()`:
  - [ ] Query patterns matching exchange and symbol
  - [ ] For each pattern, calculate relevance score
  - [ ] Filter by minimum relevance threshold
  - [ ] Sort by relevance score (descending)
  - [ ] Return top N matches
- [ ] Relevance score calculation:
  - [ ] Compare indicator values (RSI, MACD, etc.)
  - [ ] Calculate similarity for each indicator
  - [ ] Weight indicators by importance
  - [ ] Combine similarities into overall relevance score
  - [ ] Adjust for pattern performance (success rate)
- [ ] Pattern matching algorithm:
  - [ ] Exact match: All conditions match exactly (relevance = 1.0)
  - [ ] Fuzzy match: Conditions match within tolerance (relevance < 1.0)
  - [ ] Partial match: Some conditions match (relevance < 0.5, may be filtered out)
- [ ] Unit tests for pattern matching

### **Task 5: Implement Pattern Relevance Scoring** [Status: ⏳ PENDING]
- [ ] Create `RelevanceCalculator` class:
  - [ ] `calculateRelevance(pattern: TradingPattern, conditions: MarketConditions): Double`
  - [ ] `calculateIndicatorSimilarity(patternValue: Any, currentValue: Any, tolerance: Double): Double`
  - [ ] `calculateOverallRelevance(componentScores: Map<String, Double>, weights: Map<String, Double>): Double`
- [ ] Relevance components:
  - [ ] Indicator similarity (RSI, MACD, SMA, etc.)
  - [ ] Price level similarity (entry price range)
  - [ ] Market trend similarity
  - [ ] Pattern performance (success rate, usage count)
  - [ ] Pattern recency (how recently used)
- [ ] Scoring weights:
  - [ ] Configurable weights per component
  - [ ] Default weights: indicators (40%), performance (30%), recency (20%), price (10%)
  - [ ] Adjustable based on testing
- [ ] Relevance thresholds:
  - [ ] Minimum relevance for match (default: 0.6)
  - [ ] High relevance threshold (default: 0.8)
  - [ ] Configurable thresholds
- [ ] Unit tests for relevance scoring

### **Task 6: Implement Pattern Learning Logic** [Status: ⏳ PENDING]
- [ ] Create `PatternLearner` class:
  - [ ] `learnFromSuccessfulTrade(trade: ClosedPosition): TradingPattern?` - Extract pattern from trade
  - [ ] `learnFromMultipleTrades(trades: List<ClosedPosition>): List<TradingPattern>` - Extract patterns from trades
  - [ ] `validatePattern(pattern: TradingPattern): Boolean` - Validate pattern quality
- [ ] Pattern extraction logic:
  - [ ] Analyze successful trades (profit > threshold)
  - [ ] Extract common conditions
  - [ ] Create patterns with high initial confidence
  - [ ] Store patterns automatically (or manual review)
- [ ] Pattern quality assessment:
  - [ ] Minimum profit threshold
  - [ ] Minimum number of similar successful trades
  - [ ] Pattern uniqueness (not duplicate)
  - [ ] Pattern completeness (all required fields)
- [ ] Learning triggers:
  - [ ] Automatic: After each successful trade
  - [ ] Batch: Periodic analysis of trade history
  - [ ] Manual: User-triggered learning
- [ ] Integration with TradeHistoryRepository:
  - [ ] Query successful trades
  - [ ] Extract patterns
  - [ ] Store patterns
- [ ] Unit tests for pattern learning

### **Task 7: Implement Pattern Pruning** [Status: ⏳ PENDING]
- [ ] Create `PruneCriteria` data class:
  - [ ] `maxAge: Duration?` - Remove patterns older than this
  - [ ] `minSuccessRate: Double?` - Remove patterns below this success rate
  - [ ] `minUsageCount: Int?` - Remove patterns with less usage
  - [ ] `maxPatterns: Int?` - Keep only top N patterns
- [ ] Implement `prunePatterns()`:
  - [ ] Query patterns matching prune criteria
  - [ ] Delete patterns from database
  - [ ] Return count of pruned patterns
- [ ] Pruning strategies:
  - [ ] Age-based: Remove patterns older than X days (default: 90 days)
  - [ ] Performance-based: Remove patterns with success rate < threshold (default: 0.3)
  - [ ] Usage-based: Remove patterns with usage count < threshold (default: 3)
  - [ ] Top-N: Keep only top N patterns by performance
- [ ] Pruning triggers:
  - [ ] Automatic: Periodic pruning (daily/weekly)
  - [ ] Manual: User-triggered pruning
- [ ] Pruning safety:
  - [ ] Log all pruned patterns
  - [ ] Allow recovery (optional - can be deferred)
  - [ ] Validate before pruning
- [ ] Unit tests for pattern pruning

### **Task 8: Implement Pattern Performance Tracking** [Status: ⏳ PENDING]
- [ ] Create `TradeOutcome` data class:
  - [ ] `patternId: String` - Pattern used
  - [ ] `success: Boolean` - Was trade successful
  - [ ] `return: BigDecimal` - Return amount
  - [ ] `timestamp: Instant` - When pattern was used
- [ ] Implement `updatePatternPerformance()`:
  - [ ] Update pattern's usage count
  - [ ] Update success count if successful
  - [ ] Recalculate success rate
  - [ ] Update average return
  - [ ] Update last used timestamp
  - [ ] Persist to database
- [ ] Performance metrics:
  - [ ] Success rate: `successCount / usageCount`
  - [ ] Average return: `sum of returns / usageCount`
  - [ ] Total return: `sum of all returns`
  - [ ] Win rate: `winning trades / total trades`
- [ ] Performance tracking integration:
  - [ ] Called when pattern is used in trade
  - [ ] Called when trade is closed
  - [ ] Update pattern metrics automatically
- [ ] Performance reporting:
  - [ ] `getTopPerformingPatterns(limit: Int): List<TradingPattern>` - Get top patterns
  - [ ] `getPatternStatistics(patternId: String): PatternStatistics` - Get detailed stats
- [ ] Unit tests for performance tracking

### **Task 9: Implement Pattern Usage Integration** [Status: ⏳ PENDING]
- [ ] Integration with AITrader (Issue #11):
  - [ ] Add pattern matching to signal generation
  - [ ] Use matched patterns to influence trading decisions
  - [ ] Track which patterns were used in trades
- [ ] Pattern usage flow:
  1. AITrader generates initial signal
  2. PatternService matches patterns to current conditions
  3. If high-relevance patterns found, adjust signal confidence
  4. If pattern suggests different action, consider it
  5. Execute trade with pattern metadata
  6. Update pattern performance on trade close
- [ ] Pattern confidence integration:
  - [ ] Combine pattern confidence with strategy confidence
  - [ ] Weight pattern influence (configurable)
  - [ ] Use patterns as confirmation or primary signal
- [ ] Usage tracking:
  - [ ] Log pattern usage in trades
  - [ ] Track pattern effectiveness
  - [ ] Enable pattern-based decision making
- [ ] Unit tests for pattern usage integration

### **Task 10: Testing** [Status: ⏳ PENDING]
- [ ] Write unit tests for `PatternService`:
  - [ ] Pattern storage (success, validation errors)
  - [ ] Pattern querying (all criteria combinations)
  - [ ] Pattern matching (exact, fuzzy, partial matches)
  - [ ] Relevance scoring (various scenarios)
  - [ ] Pattern learning (extraction from trades)
  - [ ] Pattern pruning (all strategies)
  - [ ] Performance tracking (updates, calculations)
  - [ ] Thread-safety (concurrent access)
- [ ] Write integration tests:
  - [ ] PatternService with PatternRepository
  - [ ] Pattern extraction from TradeHistoryRepository
  - [ ] End-to-end pattern lifecycle
- [ ] Test with real data:
  - [ ] Create test patterns
  - [ ] Test matching with known conditions
  - [ ] Verify relevance scores
- [ ] Verify all tests pass: `./gradlew test`
- [ ] Code coverage meets targets (>80%)

### **Task 11: Documentation** [Status: ⏳ PENDING]
- [ ] Add comprehensive KDoc to all classes
- [ ] Create `PATTERN_STORAGE_GUIDE.md`:
  - [ ] Architecture overview
  - [ ] Pattern storage explanation
  - [ ] Pattern matching algorithm details
  - [ ] Relevance scoring explanation
  - [ ] Pattern learning process
  - [ ] Pattern pruning strategies
  - [ ] Performance tracking
  - [ ] Integration with AITrader
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
1. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/patterns/PatternService.kt`
2. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/patterns/TradingPattern.kt` (extend if needed)
3. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/patterns/PatternCriteria.kt`
4. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/patterns/MarketConditions.kt`
5. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/patterns/MatchedPattern.kt`
6. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/patterns/RelevanceCalculator.kt`
7. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/patterns/PatternLearner.kt`
8. ✅ `core-service/src/main/kotlin/com/fmps/autotrader/core/patterns/PruneCriteria.kt`

### **Test Files**
1. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/patterns/PatternServiceTest.kt`
2. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/patterns/RelevanceCalculatorTest.kt`
3. ✅ `core-service/src/test/kotlin/com/fmps/autotrader/core/patterns/PatternLearnerTest.kt`

### **Documentation**
- `Development_Handbook/PATTERN_STORAGE_GUIDE.md`

---

## 🎯 **Success Criteria**

| Criterion | Status | Verification Method |
|-----------|--------|---------------------|
| PatternService implemented with all operations | ⏳ | File exists, unit tests pass |
| Pattern storage working | ⏳ | Unit tests pass, database integration tests |
| Pattern querying working | ⏳ | Unit tests pass, various criteria |
| Pattern matching working | ⏳ | Unit tests pass, known scenarios |
| Relevance scoring accurate | ⏳ | Unit tests pass, known test cases |
| Pattern learning working | ⏳ | Unit tests pass, integration tests |
| Pattern pruning working | ⏳ | Unit tests pass, integration tests |
| Performance tracking working | ⏳ | Unit tests pass, integration tests |
| All tests pass | ⏳ | `./gradlew test` |
| Build succeeds | ⏳ | `./gradlew build` |
| CI pipeline passes | ⏳ | GitHub Actions |
| Code coverage >80% | ⏳ | Coverage report |
| Documentation complete | ⏳ | Documentation review |

---

## 🔧 **Key Technologies**

| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin Coroutines | 1.7+ | Async operations |
| PatternRepository (Epic 1) | - | Database persistence |
| TradeHistoryRepository (Epic 1) | - | Trade history access |
| Technical Indicators (Epic 2) | - | Pattern condition matching |

---

## ⏱️ **Estimated Timeline**

| Task | Estimated Time |
|------|---------------|
| Task 1: Design Architecture | 4 hours |
| Task 2: Pattern Storage | 4 hours |
| Task 3: Pattern Querying | 3 hours |
| Task 4: Pattern Matching | 5 hours |
| Task 5: Relevance Scoring | 4 hours |
| Task 6: Pattern Learning | 4 hours |
| Task 7: Pattern Pruning | 3 hours |
| Task 8: Performance Tracking | 3 hours |
| Task 9: Usage Integration | 3 hours |
| Task 10: Testing | 8 hours |
| Task 11: Documentation | 4 hours |
| Task 12: Build & Commit | 2 hours |
| **Total** | **~3-4 days** |

---

## 🔄 **Dependencies**

### **Depends On** (Must be complete first)
- Epic 1: Database, PatternRepository ✅
- Issue #11: AI Trader Core ⏳ (for integration with signal generation)

### **Blocks** (Cannot start until this is done)
- None (final issue in Epic 3)

### **Related** (Related but not blocking)
- Issue #13: Position Manager (for successful trade extraction)
- Epic 1: TradeHistoryRepository

---

## ⚠️ **Risks & Considerations**

| Risk | Impact | Mitigation Strategy |
|------|--------|---------------------|
| Pattern matching accuracy | Medium | Comprehensive testing, known scenarios, tuning |
| Pattern storage performance | Medium | Database indexing, query optimization, caching |
| Pattern relevance calculation | Medium | Extensive testing, known test cases, iterative tuning |
| Pattern learning quality | Medium | Validation rules, manual review option, quality thresholds |

---

**Issue Created**: November 5, 2025  
**Priority**: P1 (High)  
**Estimated Effort**: 3-4 days  
**Status**: 📋 **PLANNED**

