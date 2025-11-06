# Pattern Storage System Guide

**Version**: 1.0  
**Last Updated**: November 5, 2025  
**Module**: `core-service/patterns`  
**Issue**: #15 - Pattern Storage System

---

## 📋 **Overview**

The Pattern Storage System is a knowledge database that enables AI traders to learn from past successful trades. It stores trading patterns extracted from profitable trades, matches current market conditions to stored patterns, and provides pattern-based trading recommendations.

### **Key Features**

- ✅ **Pattern Storage**: Store successful trades as reusable patterns
- ✅ **Pattern Matching**: Match current market conditions to stored patterns
- ✅ **Relevance Scoring**: Calculate pattern relevance to current market conditions
- ✅ **Pattern Learning**: Automatically extract patterns from successful trades
- ✅ **Performance Tracking**: Track success rate and performance per pattern
- ✅ **Pattern Pruning**: Remove old, irrelevant, or underperforming patterns
- ✅ **Integration**: Seamless integration with AITrader and SignalGenerator

---

## 🎯 **Quick Start**

### **Basic Usage**

```kotlin
import com.fmps.autotrader.core.patterns.*
import com.fmps.autotrader.core.patterns.models.*
import com.fmps.autotrader.shared.enums.*
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.time.Instant

// 1. Create PatternService (typically injected via DI)
val patternService = PatternService(
    patternRepository = patternRepository,
    tradeRepository = tradeRepository
)

// 2. Store a pattern from a successful trade
runBlocking {
    val pattern = TradingPattern(
        id = UUID.randomUUID().toString(),
        name = "RSI Oversold Bounce",
        exchange = Exchange.BINANCE,
        symbol = "BTCUSDT",
        timeframe = "1h",
        action = TradeAction.LONG,
        conditions = mapOf(
            "RSI" to 30.0,  // RSI was oversold
            "MACD" to -0.5, // MACD was negative
            "SMA_50" to 50000.0
        ),
        confidence = 0.75,
        createdAt = Instant.now(),
        lastUsedAt = Instant.now(),
        usageCount = 0,
        successCount = 0,
        successRate = 0.0,
        averageReturn = BigDecimal.ZERO
    )
    
    val result = patternService.storePattern(pattern)
    if (result.isSuccess) {
        println("Pattern stored with ID: ${result.getOrNull()}")
    }
}

// 3. Match patterns to current market conditions
runBlocking {
    val marketConditions = MarketConditions(
        exchange = Exchange.BINANCE,
        symbol = "BTCUSDT",
        currentPrice = BigDecimal("50000.0"),
        indicators = mapOf(
            "RSI" to 32.0,
            "MACD" to -0.4,
            "SMA_50" to 50000.0
        ),
        candlesticks = emptyList(),
        timestamp = Instant.now()
    )
    
    val matches = patternService.matchPatterns(
        conditions = marketConditions,
        minRelevance = 0.7,
        maxResults = 5
    )
    
    matches.forEach { match ->
        println("Pattern: ${match.pattern.name}")
        println("Relevance: ${match.relevanceScore}")
        println("Confidence: ${match.confidence}")
    }
}

// 4. Extract patterns from successful trades
runBlocking {
    val patternLearner = PatternLearner(tradeRepository)
    
    // Extract pattern from a specific trade
    val pattern = patternLearner.extractPatternFromTrade(tradeId = 123)
    
    // Extract patterns from multiple successful trades
    val patterns = patternLearner.extractPatternsFromTrades(
        traderId = null, // All traders
        minProfitPercent = BigDecimal("0.02") // 2% minimum profit
    )
}

// 5. Update pattern performance after a trade
runBlocking {
    val outcome = TradeOutcome(
        patternId = "pattern-123",
        success = true,
        returnAmount = BigDecimal("50.0"),
        timestamp = Instant.now()
    )
    
    patternService.updatePatternPerformance("pattern-123", outcome)
}

// 6. Prune old or underperforming patterns
runBlocking {
    val criteria = PruneCriteria(
        maxAge = Duration.ofDays(90), // Remove patterns older than 90 days
        minSuccessRate = 0.3, // Remove patterns with <30% success rate
        minUsageCount = 5, // Keep patterns used at least 5 times
        maxPatternsToKeep = 100 // Keep top 100 patterns
    )
    
    val result = patternService.prunePatterns(criteria)
    if (result.isSuccess) {
        println("Pruned ${result.getOrNull()} patterns")
    }
}
```

---

## 🏗️ **Architecture**

### **Component Structure**

```
Pattern Storage System
├── PatternService          Core service for pattern operations
├── PatternLearner          Extracts patterns from trades
├── RelevanceCalculator     Calculates pattern relevance scores
├── PatternIntegration      Integration layer with AITrader
└── Models
    ├── TradingPattern      Pattern data model
    ├── MarketConditions    Current market state
    ├── MatchedPattern      Pattern match result
    ├── PatternCriteria     Query criteria
    ├── PruneCriteria       Pruning criteria
    └── TradeOutcome        Trade result for performance tracking
```

### **Data Flow**

```
1. Successful Trade
   ↓
2. PatternLearner.extractPatternFromTrade()
   ↓
3. PatternService.storePattern()
   ↓
4. Pattern stored in database
   ↓
5. Market conditions arrive
   ↓
6. PatternService.matchPatterns()
   ↓
7. RelevanceCalculator calculates scores
   ↓
8. Matched patterns returned
   ↓
9. SignalGenerator uses patterns
   ↓
10. Trade executed
    ↓
11. PatternService.updatePatternPerformance()
    ↓
12. Pattern statistics updated
```

---

## 📚 **Core Components**

### **PatternService**

The main service class that provides all pattern operations.

#### **Key Methods**

```kotlin
// Store a new pattern
suspend fun storePattern(pattern: TradingPattern): Result<String>

// Query patterns by criteria
suspend fun queryPatterns(criteria: PatternCriteria): List<TradingPattern>

// Match patterns to market conditions
suspend fun matchPatterns(
    conditions: MarketConditions,
    minRelevance: Double = 0.6,
    maxResults: Int = 10
): List<MatchedPattern>

// Update pattern performance
suspend fun updatePatternPerformance(
    patternId: String,
    outcome: TradeOutcome
): Result<Unit>

// Prune patterns
suspend fun prunePatterns(criteria: PruneCriteria): Result<Int>

// Get pattern by ID
suspend fun getPattern(patternId: String): TradingPattern?
```

#### **Thread Safety**

`PatternService` uses `Mutex` for thread-safe concurrent access:

```kotlin
private val mutex = Mutex()

suspend fun storePattern(pattern: TradingPattern): Result<String> {
    return mutex.withLock {
        // Thread-safe pattern storage
    }
}
```

### **PatternLearner**

Extracts trading patterns from successful trades.

#### **Key Methods**

```kotlin
// Extract pattern from a single trade
suspend fun extractPatternFromTrade(tradeId: Int): TradingPattern?

// Extract patterns from multiple trades
suspend fun extractPatternsFromTrades(
    traderId: Int? = null,
    minProfitPercent: BigDecimal = BigDecimal("0.01")
): List<TradingPattern>

// Validate pattern quality
fun validatePattern(pattern: TradingPattern): Boolean
```

#### **Pattern Extraction Process**

1. **Filter Trades**: Only extract from successful trades (profit > 0)
2. **Check Threshold**: Ensure profit meets minimum threshold (default: 1%)
3. **Extract Conditions**: Extract market conditions from trade entry:
   - Indicator values (RSI, MACD, SMA, EMA, Bollinger Bands)
   - Price levels
   - Volume (if available)
4. **Create Pattern**: Generate pattern with metadata
5. **Validate**: Ensure pattern meets quality criteria

### **RelevanceCalculator**

Calculates how relevant a pattern is to current market conditions.

#### **Relevance Factors**

1. **Indicator Similarity**: How similar are indicator values?
2. **Pattern Performance**: Success rate and average return
3. **Recency**: How recently was the pattern used?
4. **Price Proximity**: How close is current price to pattern price?

#### **Relevance Score Formula**

```
relevance = (
    indicatorSimilarity * 0.4 +
    performanceScore * 0.3 +
    recencyScore * 0.2 +
    priceProximity * 0.1
)
```

Where:
- `indicatorSimilarity`: 0.0-1.0 based on indicator value differences
- `performanceScore`: 0.0-1.0 based on success rate and average return
- `recencyScore`: 0.0-1.0 based on last usage time (more recent = higher)
- `priceProximity`: 0.0-1.0 based on price difference percentage

### **PatternIntegration**

Integration layer between PatternService and AITrader.

#### **Key Methods**

```kotlin
// Get pattern-based trading signals
suspend fun getPatternBasedSignals(
    processedData: ProcessedMarketData,
    exchange: Exchange,
    symbol: String,
    minRelevance: Double = 0.7,
    maxPatterns: Int = 5
): List<TradingSignal>

// Store pattern from trade
suspend fun storePatternFromTrade(tradeId: Int): String?

// Update pattern performance
suspend fun updatePatternPerformance(
    patternId: String,
    success: Boolean,
    returnAmount: BigDecimal
)

// Extract patterns from trades
suspend fun extractPatternsFromTrades(
    traderId: Int? = null,
    minProfitPercent: BigDecimal = BigDecimal("0.01")
): Int
```

---

## 📊 **Data Models**

### **TradingPattern**

Represents a stored trading pattern.

```kotlin
data class TradingPattern(
    val id: String,                    // Unique pattern ID
    val name: String?,                 // Pattern name (optional)
    val exchange: Exchange,            // Exchange (BINANCE, BITGET)
    val symbol: String,                // Trading pair (e.g., "BTCUSDT")
    val timeframe: String,             // Timeframe (e.g., "1h", "4h")
    val action: TradeAction,           // Recommended action (LONG, SHORT)
    val conditions: Map<String, Any>,  // Market conditions (indicators, prices)
    val confidence: Double,            // Initial confidence (0.0-1.0)
    val createdAt: Instant,           // Creation timestamp
    val lastUsedAt: Instant,          // Last usage timestamp
    val usageCount: Int,               // Number of times used
    val successCount: Int,             // Number of successful uses
    val successRate: Double,           // Success rate (successCount / usageCount)
    val averageReturn: BigDecimal,     // Average return per use
    val description: String? = null,   // Pattern description
    val tags: List<String> = emptyList() // Pattern tags
)
```

### **MarketConditions**

Represents current market conditions for pattern matching.

```kotlin
data class MarketConditions(
    val exchange: Exchange,
    val symbol: String,
    val currentPrice: BigDecimal,
    val indicators: Map<String, Any>,  // Current indicator values
    val candlesticks: List<Candlestick>,
    val timestamp: Instant
)
```

### **MatchedPattern**

Represents a pattern match result.

```kotlin
data class MatchedPattern(
    val pattern: TradingPattern,       // The matched pattern
    val relevanceScore: Double,        // Relevance score (0.0-1.0)
    val matchedConditions: Map<String, Any>, // Conditions that matched
    val confidence: Double             // Combined confidence
) {
    fun getFinalConfidence(): Double {
        return (relevanceScore * 0.6 + pattern.successRate * 0.3 + confidence * 0.1)
            .coerceIn(0.0, 1.0)
    }
}
```

### **PatternCriteria**

Query criteria for pattern retrieval.

```kotlin
data class PatternCriteria(
    val exchange: Exchange? = null,        // Filter by exchange (BINANCE, BITGET)
    val symbol: String? = null,            // Filter by trading pair (e.g., "BTCUSDT")
    val minSuccessRate: Double? = null,    // Minimum success rate (0.0-1.0)
    val minUsageCount: Int? = null,        // Minimum number of times pattern was used
    val minConfidence: Double? = null,     // Minimum confidence level (0.0-1.0)
    val maxAge: Duration? = null,          // Maximum age of pattern
    val action: TradeAction? = null,       // Filter by trade action (LONG, SHORT)
    val timeframe: String? = null,         // Filter by timeframe (e.g., "1h", "4h")
    val tags: List<String>? = null         // Filter by tags (any tag in list matches)
)
```

**Tag Filtering**: The `tags` field allows filtering patterns by tags. If provided, patterns matching any tag in the list will be returned. Tags are stored as comma-separated values in the database and matched case-insensitively.

### **PruneCriteria**

Criteria for pattern pruning.

```kotlin
data class PruneCriteria(
    val maxAge: Duration? = null,      // Remove patterns older than this
    val minSuccessRate: Double? = null, // Remove patterns below this success rate
    val minUsageCount: Int? = null,    // Keep patterns used at least this many times
    val maxPatternsToKeep: Int? = null // Keep only top N patterns
)
```

---

## 🔄 **Pattern Lifecycle**

### **1. Pattern Creation**

Patterns are created in two ways:

#### **Manual Creation**

```kotlin
val pattern = TradingPattern(
    id = UUID.randomUUID().toString(),
    name = "Custom Pattern",
    exchange = Exchange.BINANCE,
    symbol = "BTCUSDT",
    timeframe = "1h",
    action = TradeAction.LONG,
    conditions = mapOf("RSI" to 30.0),
    confidence = 0.8,
    createdAt = Instant.now(),
    lastUsedAt = Instant.now(),
    usageCount = 0,
    successCount = 0,
    successRate = 0.0,
    averageReturn = BigDecimal.ZERO
)

patternService.storePattern(pattern)
```

#### **Automatic Extraction**

```kotlin
val patternLearner = PatternLearner(tradeRepository)

// Extract from a specific trade
val pattern = patternLearner.extractPatternFromTrade(tradeId = 123)

if (pattern != null && patternLearner.validatePattern(pattern)) {
    patternService.storePattern(pattern)
}
```

### **2. Pattern Matching**

When market conditions arrive, patterns are matched:

```kotlin
val marketConditions = MarketConditions(
    exchange = Exchange.BINANCE,
    symbol = "BTCUSDT",
    currentPrice = BigDecimal("50000.0"),
    indicators = mapOf("RSI" to 32.0),
    candlesticks = emptyList(),
    timestamp = Instant.now()
)

val matches = patternService.matchPatterns(
    conditions = marketConditions,
    minRelevance = 0.7,
    maxResults = 5
)
```

### **3. Pattern Usage**

When a pattern is used in a trade:

```kotlin
// Track pattern usage (stored in trade.patternId)
val trade = createTrade(patternId = matchedPattern.pattern.id)

// After trade closes, update pattern performance
val outcome = TradeOutcome(
    patternId = pattern.id,
    success = trade.profitLoss > BigDecimal.ZERO,
    returnAmount = trade.profitLoss,
    timestamp = Instant.now()
)

patternService.updatePatternPerformance(pattern.id, outcome)
```

### **4. Pattern Pruning**

Periodically prune old or underperforming patterns:

```kotlin
val criteria = PruneCriteria(
    maxAge = Duration.ofDays(90),
    minSuccessRate = 0.3,
    minUsageCount = 5,
    maxPatternsToKeep = 100
)

patternService.prunePatterns(criteria)
```

---

## 🎯 **Integration with AITrader**

### **SignalGenerator Integration**

`SignalGenerator` uses patterns to enhance trading signals:

```kotlin
class SignalGenerator(
    private val strategy: ITradingStrategy,
    private val patternService: PatternService? = null,
    private val config: AITraderConfig? = null,
    private val patternWeight: Double = 0.3 // Weight of pattern confidence
) {
    suspend fun generateSignal(
        processedData: ProcessedMarketData,
        currentPosition: Position? = null
    ): TradingSignal {
        // Get strategy signal
        val strategySignal = strategy.generateSignal(...)
        
        // Match patterns if available
        val patternMatch = if (patternService != null && config != null) {
            matchPatterns(processedData, strategySignal)
        } else null
        
        // Combine strategy and pattern confidence
        val finalConfidence = calculateFinalConfidence(
            strategySignal,
            filteredSignal,
            currentPosition,
            patternMatch
        )
        
        // Create signal with pattern information
        return TradingSignal(
            action = filteredSignal.action,
            confidence = finalConfidence,
            reason = buildFinalReason(...),
            matchedPatternId = patternMatch?.pattern?.id
        )
    }
}
```

### **PatternIntegrationHelper**

Helper class for integrating patterns with AITrader:

```kotlin
class PatternIntegrationHelper(
    private val patternService: PatternService,
    private val patternLearner: PatternLearner,
    private val tradeRepository: TradeRepository
) {
    // Update pattern performance after trade closes
    suspend fun updatePatternAfterTradeClose(
        patternId: String,
        tradeId: Int
    ): Result<Unit>
    
    // Extract patterns from successful trades
    suspend fun extractPatternsFromSuccessfulTrades(
        traderId: Int? = null,
        minProfitPercent: BigDecimal = BigDecimal("0.01")
    ): Result<Int>
    
    // Process trade closure and update patterns
    suspend fun processTradeClosure(
        tradeId: Int,
        extractPatternIfSuccessful: Boolean = false
    ): Result<Unit>
}
```

---

## 📈 **Pattern Performance Tracking**

### **Success Rate Calculation**

Success rate is calculated as:

```
successRate = successCount / usageCount
```

Updated after each trade:

```kotlin
if (outcome.success) {
    pattern.successCount++
}
pattern.usageCount++
pattern.successRate = pattern.successCount.toDouble() / pattern.usageCount
```

### **Average Return Calculation**

Average return is calculated as:

```
averageReturn = totalReturn / usageCount
```

Updated incrementally:

```kotlin
val totalReturn = pattern.averageReturn * pattern.usageCount + outcome.returnAmount
pattern.usageCount++
pattern.averageReturn = totalReturn / pattern.usageCount
```

---

## 🔍 **Pattern Matching Algorithm**

### **Relevance Calculation**

The relevance score combines multiple factors:

1. **Indicator Similarity** (40% weight)
   - Compares current indicator values to pattern conditions
   - Uses tolerance-based matching for numeric values
   - Handles ranges (e.g., RSI between 30-40)

2. **Pattern Performance** (30% weight)
   - Based on success rate and average return
   - Higher performance = higher relevance

3. **Recency** (20% weight)
   - More recently used patterns score higher
   - Decays over time

4. **Price Proximity** (10% weight)
   - Patterns closer to current price score higher
   - Uses percentage difference

### **Example Matching**

```kotlin
// Pattern conditions
pattern.conditions = mapOf(
    "RSI" to 30.0,
    "MACD" to -0.5,
    "SMA_50" to 50000.0
)

// Current market conditions
marketConditions.indicators = mapOf(
    "RSI" to 32.0,      // Close match (within tolerance)
    "MACD" to -0.4,     // Close match
    "SMA_50" to 50100.0 // Close match (0.2% difference)
)

// Relevance calculation
indicatorSimilarity = 0.9  // Very similar
performanceScore = 0.8      // Good performance
recencyScore = 0.7          // Recently used
priceProximity = 0.95      // Very close price

relevance = 0.9 * 0.4 + 0.8 * 0.3 + 0.7 * 0.2 + 0.95 * 0.1
         = 0.36 + 0.24 + 0.14 + 0.095
         = 0.835
```

---

## 🧹 **Pattern Pruning**

### **Pruning Strategies**

1. **Age-Based Pruning**: Remove patterns older than threshold
2. **Performance-Based Pruning**: Remove patterns below success rate threshold
3. **Usage-Based Pruning**: Keep only patterns used minimum times
4. **Top-N Pruning**: Keep only top N patterns by performance

### **Example Pruning**

```kotlin
val criteria = PruneCriteria(
    maxAge = Duration.ofDays(90),      // Remove patterns > 90 days old
    minSuccessRate = 0.3,              // Remove patterns < 30% success
    minUsageCount = 5,                 // Keep patterns used ≥ 5 times
    maxPatternsToKeep = 100            // Keep top 100 patterns
)

val result = patternService.prunePatterns(criteria)
// Removes patterns that don't meet criteria
// Keeps top 100 patterns by performance
```

---

## 🧪 **Testing**

### **Unit Tests**

All components have comprehensive unit tests:

- `PatternServiceTest.kt`: Tests pattern storage, querying, matching, pruning
- `PatternLearnerTest.kt`: Tests pattern extraction from trades
- `RelevanceCalculatorTest.kt`: Tests relevance scoring algorithm

### **Test Examples**

```kotlin
@Test
fun `test storePattern stores pattern successfully`() = runBlocking {
    val pattern = createTestPattern()
    val result = patternService.storePattern(pattern)
    
    assertTrue(result.isSuccess)
    assertNotNull(result.getOrNull())
}

@Test
fun `test matchPatterns returns relevant patterns`() = runBlocking {
    // Store test patterns
    patternService.storePattern(pattern1)
    patternService.storePattern(pattern2)
    
    // Match against market conditions
    val matches = patternService.matchPatterns(
        conditions = marketConditions,
        minRelevance = 0.7
    )
    
    assertEquals(2, matches.size)
    assertTrue(matches.all { it.relevanceScore >= 0.7 })
}
```

---

## 🐛 **Troubleshooting**

### **Common Issues**

#### **Patterns Not Matching**

**Problem**: Patterns not matching current market conditions.

**Solutions**:
- Check `minRelevance` threshold (may be too high)
- Verify indicator values in pattern conditions match current indicators
- Ensure patterns are stored with correct exchange and symbol
- Check pattern age (old patterns may not match current market)
- Verify exchange matches (patterns are exchange-specific)

#### **Exchange Field Limitation (Resolved)**

**Status**: ✅ **RESOLVED** (Migration V2 applied)

**Previous Issue**: Exchange field was not stored in the database, causing all patterns to default to BINANCE.

**Resolution**: 
- Database migration V2 (`V2__Add_exchange_to_patterns.sql`) adds `exchange` column to `patterns` table
- PatternRepository now stores and retrieves exchange information
- PatternService uses stored exchange instead of defaulting
- Existing patterns defaulted to BINANCE for backward compatibility

**Migration Notes**:
- Migration automatically applies on next database initialization
- Existing patterns are updated to have BINANCE as exchange
- New patterns will store the correct exchange (BINANCE or BITGET)
- Pattern matching now correctly filters by exchange

#### **Low Pattern Performance**

**Problem**: Patterns have low success rates.

**Solutions**:
- Increase `minProfitPercent` threshold for pattern extraction
- Prune underperforming patterns more aggressively
- Review pattern validation criteria
- Ensure patterns are updated after each trade

#### **Pattern Storage Failures**

**Problem**: `storePattern()` returns error.

**Solutions**:
- Verify pattern data is valid (all required fields present)
- Check database connection
- Ensure pattern ID is unique
- Review pattern validation logic

---

## 📚 **Best Practices**

1. **Pattern Quality**: Only extract patterns from trades with significant profit (>1%)
2. **Regular Pruning**: Prune patterns periodically to maintain quality
3. **Relevance Threshold**: Use appropriate `minRelevance` (0.6-0.8 recommended)
4. **Pattern Validation**: Always validate patterns before storing
5. **Performance Tracking**: Update pattern performance after every trade
6. **Pattern Naming**: Use descriptive names for manual patterns
7. **Tagging**: Use tags to categorize patterns for easier querying

---

## 🔗 **Related Documentation**

- **Issue #15**: Pattern Storage System - Detailed implementation plan
- **AI Trader Core Guide**: Integration with AITrader
- **Technical Indicators Guide**: Indicator values used in patterns
- **Database Schema**: Pattern storage schema (Epic 1)

---

## 📝 **Changelog**

### **Version 1.0** (November 5, 2025)
- Initial implementation
- Pattern storage and retrieval
- Pattern matching with relevance scoring
- Pattern learning from trades
- Pattern pruning
- Performance tracking

---

**End of Pattern Storage System Guide**

