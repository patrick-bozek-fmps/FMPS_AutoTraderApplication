# AI Trader Core Guide

**Version**: 1.0  
**Last Updated**: November 5, 2025  
**Module**: `core-service/traders`  
**Issue**: #11 - AI Trader Core

---

## 📋 **Overview**

The AI Trader Core module is the heart of the FMPS AutoTrader automated trading system. It provides the main `AITrader` class that orchestrates automated trading operations, processes market data, generates trading signals using technical indicators, and executes trading strategies.

### **Key Features**

- ✅ **Lifecycle Management**: Start, stop, pause, resume trading operations
- ✅ **Multiple Strategies**: Trend Following, Mean Reversion, Breakout
- ✅ **Signal Generation**: Intelligent trading signal generation with confidence scoring
- ✅ **Market Data Processing**: Real-time candlestick processing and indicator calculation
- ✅ **State Management**: Thread-safe state transitions and error recovery
- ✅ **Performance Metrics**: Comprehensive trading performance tracking
- ✅ **Integration Ready**: Designed for PositionManager (Issue #13) and RiskManager (Issue #14)

---

## 🎯 **Quick Start**

### **Basic Usage**

```kotlin
import com.fmps.autotrader.core.traders.*
import com.fmps.autotrader.core.connectors.*
import com.fmps.autotrader.shared.enums.*
import com.fmps.autotrader.shared.model.*
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.time.Duration

// 1. Create configuration
val config = AITraderConfig(
    id = "trader-1",
    name = "My Trend Trader",
    exchange = Exchange.BINANCE,
    symbol = "BTCUSDT",
    virtualMoney = true, // v1.0: always true
    maxStakeAmount = BigDecimal("1000.00"),
    maxRiskLevel = 5, // 1-10 scale
    maxTradingDuration = Duration.ofHours(24),
    minReturnPercent = 5.0,
    strategy = TradingStrategy.TREND_FOLLOWING,
    candlestickInterval = TimeFrame.ONE_HOUR
)

// 2. Create exchange connector
val exchangeConnector = ConnectorFactory.createConnector(Exchange.BINANCE)
exchangeConnector.configure(
    ExchangeConfig(
        exchange = Exchange.BINANCE,
        apiKey = "your-api-key",
        apiSecret = "your-api-secret",
        testnet = true
    )
)

// 3. Create AI Trader
val trader = AITrader(
    config = config,
    exchangeConnector = exchangeConnector
)

// 4. Start trading
runBlocking {
    trader.start()
    
    // Trader is now running and generating signals
    // Trading loop runs automatically
    
    // Monitor state
    println("Trader state: ${trader.getState()}")
    
    // Get metrics
    val metrics = trader.getMetrics()
    println("Total trades: ${metrics.totalTrades}")
    println("Win rate: ${metrics.winRate}")
    
    // Stop when done
    trader.stop()
}
```

---

## 🏗️ **Architecture**

### **Component Structure**

```
AITrader
├── AITraderConfig        Configuration parameters
├── AITraderState         Lifecycle state (IDLE, RUNNING, etc.)
├── AITraderMetrics       Performance metrics
│
├── Strategy Layer
│   ├── ITradingStrategy  Strategy interface
│   ├── StrategyFactory   Strategy creation
│   ├── TrendFollowingStrategy
│   ├── MeanReversionStrategy
│   └── BreakoutStrategy
│
├── Processing Layer
│   ├── MarketDataProcessor   Candlestick processing
│   ├── ProcessedMarketData   Processed data with indicators
│   └── SignalGenerator       Signal generation logic
│
├── Integration Layer
│   ├── IExchangeConnector     Market data & orders
│   ├── PositionManager?       Position tracking (Issue #13)
│   └── RiskManager?           Risk management (Issue #14)
│
└── Trading Loop
    ├── Fetch market data
    ├── Process & calculate indicators
    ├── Generate signals
    └── Execute trades (via PositionManager)
```

### **Data Flow**

```
1. Exchange Connector
   └─> Fetches candlesticks

2. MarketDataProcessor
   └─> Calculates indicators (SMA, RSI, MACD, Bollinger Bands)
   └─> Creates ProcessedMarketData

3. SignalGenerator
   └─> Calls strategy.generateSignal()
   └─> Applies filters (confidence, position limits)
   └─> Returns TradingSignal

4. AITrader
   └─> Executes signal (if actionable)
   └─> Updates metrics
   └─> Sleeps until next interval
```

---

## 📊 **Configuration**

### **AITraderConfig Parameters**

The `AITraderConfig` data class contains all configuration parameters as specified in ATP_ProdSpec_53:

```kotlin
data class AITraderConfig(
    val id: String,                    // Unique trader identifier
    val name: String,                  // Human-readable name
    val exchange: Exchange,             // BINANCE or BITGET
    val symbol: String,                // Trading pair (e.g., "BTCUSDT")
    val virtualMoney: Boolean = true,  // v1.0: always true (demo only)
    val maxStakeAmount: BigDecimal,    // Maximum amount to stake per trade
    val maxRiskLevel: Int,             // Risk/leverage level (1-10)
    val maxTradingDuration: Duration, // Maximum trading duration
    val minReturnPercent: Double,      // Minimum return/profit target (%)
    val strategy: TradingStrategy,      // TREND_FOLLOWING, MEAN_REVERSION, BREAKOUT
    val candlestickInterval: TimeFrame // 1m, 5m, 1h, 1d, etc.
)
```

### **Configuration Validation**

The config validates parameters on creation:

```kotlin
val config = AITraderConfig(
    id = "", // ❌ Will throw: "ID cannot be blank"
    maxStakeAmount = BigDecimal("-100"), // ❌ Will throw: "Max stake amount must be positive"
    maxRiskLevel = 15, // ❌ Will throw: "Max risk level must be between 1 and 10"
    // ... other parameters
)
```

### **Example Configurations**

#### **Conservative Trend Trader**

```kotlin
val conservativeConfig = AITraderConfig(
    id = "trend-conservative",
    name = "Conservative Trend Trader",
    exchange = Exchange.BINANCE,
    symbol = "BTCUSDT",
    virtualMoney = true,
    maxStakeAmount = BigDecimal("500.00"), // Lower stake
    maxRiskLevel = 3, // Low risk
    maxTradingDuration = Duration.ofHours(12),
    minReturnPercent = 3.0, // Lower target
    strategy = TradingStrategy.TREND_FOLLOWING,
    candlestickInterval = TimeFrame.ONE_HOUR
)
```

#### **Aggressive Breakout Trader**

```kotlin
val aggressiveConfig = AITraderConfig(
    id = "breakout-aggressive",
    name = "Aggressive Breakout Trader",
    exchange = Exchange.BITGET,
    symbol = "ETHUSDT",
    virtualMoney = true,
    maxStakeAmount = BigDecimal("2000.00"), // Higher stake
    maxRiskLevel = 8, // High risk
    maxTradingDuration = Duration.ofHours(48),
    minReturnPercent = 10.0, // Higher target
    strategy = TradingStrategy.BREAKOUT,
    candlestickInterval = TimeFrame.FIVE_MINUTES
)
```

---

## 🎯 **Trading Strategies**

### **1. Trend Following Strategy**

**Purpose**: Follows market trends using moving average crossovers.

**Logic**:
- **BUY Signal**: Golden Cross (short SMA crosses above long SMA) + RSI not overbought + MACD bullish
- **SELL Signal**: Death Cross (short SMA crosses below long SMA) + RSI not oversold + MACD bearish
- **HOLD Signal**: No crossover or conflicting indicators

**Configuration Parameters**:
- `smaShortPeriod`: Short-term SMA period (default: 9)
- `smaLongPeriod`: Long-term SMA period (default: 21)
- `rsiPeriod`: RSI period (default: 14)
- `rsiOverbought`: RSI overbought threshold (default: 70.0)
- `rsiOversold`: RSI oversold threshold (default: 30.0)

**Required Indicators**: SMA, EMA, MACD, RSI

**Example**:
```kotlin
val config = AITraderConfig(
    // ... other params
    strategy = TradingStrategy.TREND_FOLLOWING,
    candlestickInterval = TimeFrame.ONE_HOUR
)

val trader = AITrader(config, exchangeConnector)

// Strategy automatically created by StrategyFactory
// Uses SMA(9), SMA(21), RSI(14), MACD for signal generation
```

**Best For**:
- Trending markets (clear up or down trends)
- Medium to long-term timeframes (1h, 4h, 1d)
- Markets with strong momentum

---

### **2. Mean Reversion Strategy**

**Purpose**: Trades against the trend, expecting price to revert to its mean.

**Logic**:
- **BUY Signal**: Price touches lower Bollinger Band (oversold) + RSI < 30
- **SELL Signal**: Price touches upper Bollinger Band (overbought) + RSI > 70
- **HOLD Signal**: Price within bands, squeeze detected, or conditions not met

**Configuration Parameters**:
- `bbPeriod`: Bollinger Bands period (default: 20)
- `bbStdDev`: Standard deviation multiplier (default: 2.0)
- `rsiPeriod`: RSI period (default: 14)
- `rsiOversold`: RSI oversold threshold (default: 30.0)
- `rsiOverbought`: RSI overbought threshold (default: 70.0)

**Required Indicators**: BollingerBands, RSI

**Example**:
```kotlin
val config = AITraderConfig(
    // ... other params
    strategy = TradingStrategy.MEAN_REVERSION,
    candlestickInterval = TimeFrame.FIFTEEN_MINUTES
)

val trader = AITrader(config, exchangeConnector)

// Strategy uses Bollinger Bands + RSI for mean reversion signals
```

**Best For**:
- Range-bound markets (sideways trading)
- Short to medium-term timeframes (15m, 1h)
- Markets with mean reversion characteristics

---

### **3. Breakout Strategy**

**Purpose**: Trades on price breakouts from consolidation patterns.

**Logic**:
- **BUY Signal**: Price breaks above upper Bollinger Band with momentum + MACD bullish
- **SELL Signal**: Price breaks below lower Bollinger Band with momentum + MACD bearish
- **HOLD Signal**: No breakout, squeeze detected, or false breakout

**Configuration Parameters**:
- `bbPeriod`: Bollinger Bands period (default: 20)
- `bbStdDev`: Standard deviation multiplier (default: 2.0)
- `breakoutThreshold`: % above/below band to confirm breakout (default: 1.05 = 5%)
- `momentumPeriod`: Period for momentum calculation (default: 14)

**Required Indicators**: BollingerBands, MACD

**Example**:
```kotlin
val config = AITraderConfig(
    // ... other params
    strategy = TradingStrategy.BREAKOUT,
    candlestickInterval = TimeFrame.FIVE_MINUTES
)

val trader = AITrader(config, exchangeConnector)

// Strategy detects breakouts with momentum confirmation
```

**Best For**:
- Volatile markets with clear breakouts
- Short to medium-term timeframes (5m, 15m, 1h)
- Markets exiting consolidation periods

---

## 🔄 **Lifecycle Management**

### **State Transitions**

```
IDLE → STARTING → RUNNING → STOPPING → STOPPED
  ↓       ↓          ↓
  └───────┴──────────┴→ ERROR
              ↓
            PAUSED → RUNNING (via resume)
```

### **State Description**

| State | Description | Can Transition To |
|-------|-------------|-------------------|
| **IDLE** | Initial state, trader not started | STARTING |
| **STARTING** | Trader is initializing | RUNNING, ERROR |
| **RUNNING** | Trader is actively trading | PAUSED, STOPPING, ERROR |
| **PAUSED** | Trading temporarily stopped | RUNNING (via resume) |
| **STOPPING** | Trader is shutting down | STOPPED, ERROR |
| **STOPPED** | Trader has stopped | STARTING, IDLE |
| **ERROR** | Error occurred, requires intervention | IDLE (after fix) |

### **Lifecycle Methods**

```kotlin
// Start trading
runBlocking {
    val result = trader.start()
    if (result.isSuccess) {
        println("Trader started successfully")
    } else {
        println("Failed to start: ${result.exceptionOrNull()?.message}")
    }
}

// Check state
val state = trader.getState()
println("Current state: $state")

// Pause trading (maintains state)
runBlocking {
    trader.pause()
}

// Resume trading
runBlocking {
    trader.resume()
}

// Stop trading
runBlocking {
    trader.stop()
}

// Cleanup (stops and releases resources)
runBlocking {
    trader.cleanup()
}
```

### **Thread Safety**

All state transitions are thread-safe using `Mutex` and `AtomicReference`:

```kotlin
// Safe to call from multiple threads
val state1 = trader.getState()
val state2 = trader.getState() // Always consistent

// State changes are synchronized
runBlocking {
    trader.start() // Thread-safe
}
```

---

## 📈 **Signal Generation**

### **TradingSignal Structure**

```kotlin
data class TradingSignal(
    val action: SignalAction,        // BUY, SELL, HOLD, CLOSE
    val confidence: Double,          // 0.0 (low) to 1.0 (high)
    val reason: String,              // Human-readable explanation
    val timestamp: Instant,          // When signal was generated
    val indicatorValues: Map<String, Any> // Supporting indicator values
)
```

### **Signal Actions**

- **BUY**: Open a long position (maps to TradeAction.LONG)
- **SELL**: Open a short position (maps to TradeAction.SHORT)
- **HOLD**: No action should be taken
- **CLOSE**: Close the current position

### **Confidence Scoring**

Confidence is calculated based on:
1. **Strategy Base Confidence**: From strategy logic (0.6-0.8 typically)
2. **Indicator Alignment**: All indicators confirming adds confidence
3. **Filter Adjustments**: Position conflicts, risk limits reduce confidence
4. **Market Conditions**: Volatility, trend strength affect confidence

**Example**:
```kotlin
val signal = TradingSignal(
    action = SignalAction.BUY,
    confidence = 0.75, // 75% confidence
    reason = "Golden Cross detected. RSI: 45.2 (not overbought), MACD: confirmed",
    timestamp = Instant.now(),
    indicatorValues = mapOf(
        "smaShort" to 50200.0,
        "smaLong" to 49800.0,
        "rsi" to 45.2,
        "macd" to 150.5,
        "macdSignal" to 140.2
    )
)

// Check if signal is actionable
if (signal.isActionable() && signal.meetsConfidenceThreshold(0.5)) {
    // Execute trade
}

// Check confidence
if (signal.confidence >= 0.7) {
    println("High confidence signal")
}
```

---

## 🔧 **Market Data Processing**

### **MarketDataProcessor**

The `MarketDataProcessor` handles:
- Calculating all required indicators for the active strategy
- Caching indicator values to avoid redundant calculations
- Validating data quality and completeness
- Handling missing/incomplete data gracefully

```kotlin
val processor = MarketDataProcessor(strategy)

// Process candlesticks
val processedData = processor.processCandlesticks(candles)

if (processedData != null) {
    // Access processed data
    val latestPrice = processedData.latestPrice
    val indicators = processedData.indicators
    val latestCandle = processedData.getLatestCandle()
    
    // Get specific indicator
    val rsi = processedData.getIndicator("RSI")
    val hasMacd = processedData.hasIndicator("MACD")
}

// Clear cache when needed
processor.clearCache()
```

### **ProcessedMarketData**

Contains:
- `candles`: List of candlesticks in chronological order
- `indicators`: Map of indicator names to calculated values
- `latestPrice`: Current/latest price
- `timestamp`: When data was processed

---

## 📊 **Performance Metrics**

### **AITraderMetrics**

Tracks comprehensive trading performance:

```kotlin
data class AITraderMetrics(
    val totalTrades: Int,              // Total trades executed
    val winningTrades: Int,             // Profitable trades
    val losingTrades: Int,              // Losing trades
    val totalProfit: BigDecimal,        // Sum of profits
    val totalLoss: BigDecimal,          // Sum of losses (absolute)
    val netProfit: BigDecimal,          // Total profit - total loss
    val winRate: Double,                // winningTrades / totalTrades
    val averageProfit: BigDecimal,      // Average profit per winning trade
    val averageLoss: BigDecimal,        // Average loss per losing trade
    val profitFactor: Double?,          // totalProfit / totalLoss
    val maxDrawdown: BigDecimal,        // Maximum peak-to-trough decline
    val sharpeRatio: Double?,          // Risk-adjusted return (optional)
    val startTime: Instant?,            // When trading started
    val uptime: Duration                // How long trader has been running
)
```

### **Accessing Metrics**

```kotlin
runBlocking {
    val metrics = trader.getMetrics()
    
    println("Total Trades: ${metrics.totalTrades}")
    println("Win Rate: ${metrics.winRate * 100}%")
    println("Net Profit: ${metrics.netProfit}")
    println("Profit Factor: ${metrics.profitFactor ?: "N/A"}")
    println("Uptime: ${metrics.uptime}")
}
```

### **Metrics Calculation**

Metrics are automatically calculated:
- Win rate: `winningTrades / totalTrades`
- Profit factor: `totalProfit / totalLoss` (if loss > 0)
- Average profit: `totalProfit / winningTrades`
- Average loss: `totalLoss / losingTrades`

**Note**: Full trade execution and metrics tracking will be enhanced in Issue #13 (Position Manager).

---

## 🎨 **Strategy Factory**

### **Creating Strategies**

The `StrategyFactory` creates strategy instances based on configuration:

```kotlin
// Factory automatically creates strategy from config
val strategy = StrategyFactory.createStrategy(config)

// Or create directly with strategy type
val strategy2 = StrategyFactory.createStrategy(
    TradingStrategy.MEAN_REVERSION,
    config
)
```

### **Strategy Interface**

All strategies implement `ITradingStrategy`:

```kotlin
interface ITradingStrategy {
    fun generateSignal(
        candles: List<Candlestick>,
        indicators: Map<String, Any>
    ): TradingSignal
    
    fun getName(): String
    fun getDescription(): String
    fun getRequiredIndicators(): List<String>
    fun validateConfig(config: AITraderConfig): Boolean
    fun reset()
}
```

---

## 🔌 **Integration**

### **Exchange Connector Integration**

The trader uses `IExchangeConnector` for:
- Fetching market data (candlesticks)
- Executing orders (via PositionManager in Issue #13)
- Getting account information

```kotlin
// Trader automatically uses connector
val trader = AITrader(config, exchangeConnector)

// Connector must be configured before starting trader
exchangeConnector.configure(exchangeConfig)
runBlocking {
    trader.start() // Connects to exchange automatically
}
```

### **PositionManager Integration (Issue #13)**

The trader is designed to integrate with `PositionManager`:

```kotlin
// Future integration (Issue #13)
val positionManager = PositionManager(...)
val trader = AITrader(
    config = config,
    exchangeConnector = exchangeConnector,
    positionManager = positionManager // Will be added in Issue #13
)
```

Currently, position management is a placeholder (TODO in code).

### **RiskManager Integration (Issue #14)**

The trader is designed to integrate with `RiskManager`:

```kotlin
// Future integration (Issue #14)
val riskManager = RiskManager(...)
val trader = AITrader(
    config = config,
    exchangeConnector = exchangeConnector,
    positionManager = positionManager,
    riskManager = riskManager // Will be added in Issue #14
)
```

Currently, risk management is a placeholder (TODO in code).

---

## 🧪 **Testing**

### **Test Coverage**

Comprehensive test suite with **85+ tests**:

- **AITraderTest**: 20+ tests (state management, lifecycle, error handling)
- **TrendFollowingStrategyTest**: 15+ tests (golden cross, death cross, RSI filters)
- **MeanReversionStrategyTest**: 15+ tests (Bollinger Bands, mean reversion logic)
- **BreakoutStrategyTest**: 15+ tests (breakout detection, momentum confirmation)
- **MarketDataProcessorTest**: 10+ tests (indicator calculation, data validation)
- **SignalGeneratorTest**: 10+ tests (signal generation, confidence calculation, filtering)

### **Running Tests**

```bash
# Run all trader tests
./gradlew :core-service:test --tests "*traders*"

# Run specific test class
./gradlew :core-service:test --tests "*TrendFollowingStrategyTest*"

# Run with coverage
./gradlew :core-service:test jacocoTestReport
```

### **Test Examples**

```kotlin
// Example: Testing strategy signal generation
@Test
fun `test golden cross generates BUY signal`() {
    val candles = createTestCandles(...)
    val signal = strategy.generateSignal(candles, emptyMap())
    
    assertEquals(SignalAction.BUY, signal.action)
    assertTrue(signal.confidence >= 0.5)
}

// Example: Testing state transitions
@Test
fun `test start transitions to RUNNING`() = runBlocking {
    trader.start()
    assertEquals(AITraderState.RUNNING, trader.getState())
}
```

---

## 🚨 **Error Handling**

### **Error Recovery**

The trader handles errors gracefully:

```kotlin
// Trading loop catches exceptions
try {
    // Fetch and process market data
    val candles = fetchMarketData()
    // ... process and generate signals
} catch (e: CancellationException) {
    // Normal cancellation
    logger.info { "Trading loop cancelled" }
    break
} catch (e: Exception) {
    // Error occurred
    logger.error(e) { "Error in trading loop" }
    state.set(AITraderState.ERROR)
    delay(10000) // Wait before retrying
}
```

### **Error States**

When an error occurs:
1. State transitions to `ERROR`
2. Error is logged with context
3. Trading loop waits before retrying
4. User intervention may be required

### **Recovery**

To recover from error state:
```kotlin
// Check if in error state
if (trader.getState() == AITraderState.ERROR) {
    // Stop and restart
    runBlocking {
        trader.stop()
        trader.start() // Attempts to recover
    }
}
```

---

## 📝 **Best Practices**

### **1. Configuration**

✅ **DO**:
- Use descriptive trader IDs and names
- Set appropriate risk levels (1-10)
- Choose timeframes that match strategy (trend following: 1h+, mean reversion: 15m-1h)
- Set realistic profit targets

❌ **DON'T**:
- Use blank IDs or names
- Set risk level > 10 or < 1
- Use too short timeframes for trend strategies
- Set unrealistic profit targets

### **2. State Management**

✅ **DO**:
- Always check state before operations
- Wait for state transitions to complete
- Handle ERROR state appropriately
- Use cleanup() when done

❌ **DON'T**:
- Start trader when already running
- Ignore error states
- Forget to stop trader before shutdown
- Access state concurrently without synchronization

### **3. Strategy Selection**

✅ **DO**:
- Use Trend Following for trending markets
- Use Mean Reversion for range-bound markets
- Use Breakout for volatile markets with clear breakouts
- Match timeframe to strategy characteristics

❌ **DON'T**:
- Use Mean Reversion in strong trends
- Use Trend Following in sideways markets
- Use Breakout in low volatility periods
- Mix incompatible timeframes and strategies

### **4. Signal Confidence**

✅ **DO**:
- Set minimum confidence threshold (default: 0.5)
- Consider higher thresholds for higher risk
- Monitor signal confidence over time
- Adjust threshold based on performance

❌ **DON'T**:
- Execute signals with very low confidence (< 0.3)
- Ignore confidence values
- Use same threshold for all strategies
- Blindly trust all signals

---

## 🐛 **Troubleshooting**

### **Common Issues**

#### **1. Trader Won't Start**

**Symptoms**: `start()` returns failure

**Possible Causes**:
- Exchange connector not configured
- Exchange connector not connected
- Invalid configuration

**Solution**:
```kotlin
// Ensure connector is configured and connected
exchangeConnector.configure(exchangeConfig)
runBlocking {
    exchangeConnector.connect()
    trader.start()
}
```

#### **2. No Signals Generated**

**Symptoms**: Trader running but no signals

**Possible Causes**:
- Insufficient market data
- Strategy conditions not met
- Confidence threshold too high

**Solution**:
```kotlin
// Check data availability
val candles = exchangeConnector.getCandles(...)
if (candles.size < 30) {
    // Need more data
}

// Lower confidence threshold
val signalGenerator = SignalGenerator(strategy, minConfidenceThreshold = 0.3)
```

#### **3. Trader Stuck in ERROR State**

**Symptoms**: State is ERROR, won't recover

**Solution**:
```kotlin
// Stop and restart
runBlocking {
    trader.stop()
    delay(1000)
    trader.start()
}
```

#### **4. High Memory Usage**

**Symptoms**: Memory growing over time

**Possible Causes**:
- Indicator cache not cleared
- Too many candles in memory

**Solution**:
```kotlin
// Clear processor cache periodically
marketDataProcessor.clearCache()

// Limit candle history
val candles = exchangeConnector.getCandles(..., limit = 100)
```

---

## 📚 **API Reference**

### **AITrader Class**

```kotlin
class AITrader(
    val config: AITraderConfig,
    private val exchangeConnector: IExchangeConnector,
    private val positionManager: Any? = null,
    private val riskManager: Any? = null
) {
    fun getState(): AITraderState
    suspend fun getMetrics(): AITraderMetrics
    suspend fun start(): Result<Unit>
    suspend fun stop(): Result<Unit>
    suspend fun pause(): Result<Unit>
    suspend fun resume(): Result<Unit>
    suspend fun updateConfig(newConfig: AITraderConfig): Result<Unit>
    suspend fun cleanup()
}
```

### **AITraderConfig**

```kotlin
data class AITraderConfig(
    val id: String,
    val name: String,
    val exchange: Exchange,
    val symbol: String,
    val virtualMoney: Boolean = true,
    val maxStakeAmount: BigDecimal,
    val maxRiskLevel: Int,
    val maxTradingDuration: Duration,
    val minReturnPercent: Double,
    val strategy: TradingStrategy,
    val candlestickInterval: TimeFrame
)
```

### **TradingSignal**

```kotlin
data class TradingSignal(
    val action: SignalAction,
    val confidence: Double,
    val reason: String,
    val timestamp: Instant = Instant.now(),
    val indicatorValues: Map<String, Any> = emptyMap()
) {
    fun isActionable(): Boolean
    fun isHold(): Boolean
    fun isClose(): Boolean
    fun meetsConfidenceThreshold(threshold: Double = 0.5): Boolean
}
```

---

## 🔗 **Related Documentation**

- **Technical Indicators Guide**: `TECHNICAL_INDICATORS_GUIDE.md`
- **Exchange Connector Guide**: `EXCHANGE_CONNECTOR_GUIDE.md`
- **Issue #11**: `Issue_11_AI_Trader_Core.md`
- **Epic 3 Status**: `EPIC_3_STATUS.md`

---

## 📝 **Changelog**

### **Version 1.0** (November 5, 2025)
- ✅ Initial implementation of AI Trader Core
- ✅ Three trading strategies implemented
- ✅ Signal generation with confidence scoring
- ✅ Market data processing pipeline
- ✅ Comprehensive test suite (85+ tests)
- ✅ Performance metrics tracking
- ✅ Thread-safe state management

---

**Created**: November 5, 2025  
**Last Updated**: November 5, 2025  
**Status**: ✅ Complete (Issue #11)

