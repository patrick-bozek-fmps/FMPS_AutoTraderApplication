# Technical Architecture - FMPS AutoTrader

**Version**: 1.0  
**Date**: October 23, 2025  
**Status**: Draft

---

## Architecture Overview

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                          │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐   │
│  │  Dashboard   │  │  Trading     │  │   Configuration    │   │
│  │   View       │  │   View       │  │      View          │   │
│  └──────┬───────┘  └──────┬───────┘  └────────┬───────────┘   │
│         │                  │                    │               │
│         └──────────────────┴────────────────────┘               │
│                            │                                    │
│  ┌─────────────────────────┴─────────────────────────────┐    │
│  │               View Models (MVVM)                       │    │
│  └─────────────────────────┬─────────────────────────────┘    │
└────────────────────────────┼──────────────────────────────────┘
                             │
┌────────────────────────────┼──────────────────────────────────┐
│                     APPLICATION LAYER                          │
│  ┌─────────────────────────┴─────────────────────────────┐    │
│  │              Trading Controller                        │    │
│  │  • Session Management                                  │    │
│  │  • Command Orchestration                               │    │
│  │  • Event Coordination                                  │    │
│  └─────────────────────────┬─────────────────────────────┘    │
│                            │                                    │
│  ┌─────────────────────────┴─────────────────────────────┐    │
│  │                  State Machine                         │    │
│  │  • Trading States (IDLE, MONITORING, TRADING, etc.)    │    │
│  │  • State Transitions                                   │    │
│  │  • State Persistence                                   │    │
│  └─────────────────────────┬─────────────────────────────┘    │
└────────────────────────────┼──────────────────────────────────┘
                             │
┌────────────────────────────┼──────────────────────────────────┐
│                       DOMAIN LAYER                             │
│  ┌─────────────────────────┴─────────────────────────────┐    │
│  │              AI Trading Agent                          │    │
│  │  • Market Analysis                                     │    │
│  │  • Strategy Selection                                  │    │
│  │  • Decision Making                                     │    │
│  └──────────┬───────────────────────────────┬────────────┘    │
│             │                                │                 │
│  ┌──────────┴────────┐           ┌──────────┴────────┐        │
│  │ Position Manager  │           │   Risk Manager    │        │
│  │ • Track Positions │           │ • Budget Limits   │        │
│  │ • P&L Calculation │           │ • Stop-Loss       │        │
│  │ • Entry/Exit      │           │ • Leverage Limits │        │
│  └───────────────────┘           └───────────────────┘        │
│             │                                │                 │
│  ┌──────────┴────────────────────────────────┴──────────┐     │
│  │          Technical Indicators Module                  │     │
│  │  • RSI, MACD, SMA, EMA                                │     │
│  │  • Bollinger Bands                                    │     │
│  │  • Custom Indicators                                  │     │
│  └───────────────────────────────────────────────────────┘     │
└────────────────────────────┬──────────────────────────────────┘
                             │
┌────────────────────────────┼──────────────────────────────────┐
│                   INFRASTRUCTURE LAYER                         │
│  ┌─────────────────────────┴─────────────────────────────┐    │
│  │            Exchange Connector Framework                │    │
│  │  ┌──────────────┐  ┌──────────────┐                   │    │
│  │  │   Binance    │  │    Bitget    │  [Future...]      │    │
│  │  │  Connector   │  │  Connector   │                   │    │
│  │  └──────────────┘  └──────────────┘                   │    │
│  └────────────────────────────────────────────────────────┘    │
│                            │                                    │
│  ┌─────────────────────────┴─────────────────────────────┐    │
│  │         External Integrations                          │    │
│  │  • TradingView Webhook Server                          │    │
│  │  • Market Data Feeds                                   │    │
│  └────────────────────────────────────────────────────────┘    │
│                            │                                    │
│  ┌─────────────────────────┴─────────────────────────────┐    │
│  │              Data Persistence                          │    │
│  │  • SQLite Database                                     │    │
│  │  • Trade History Repository                            │    │
│  │  • Configuration Repository                            │    │
│  └────────────────────────────────────────────────────────┘    │
│                            │                                    │
│  ┌─────────────────────────┴─────────────────────────────┐    │
│  │         Cross-Cutting Concerns                         │    │
│  │  • Logging (SLF4J + Logback)                           │    │
│  │  • Configuration Management                            │    │
│  │  • Error Handling                                      │    │
│  │  • Security (API Key Encryption)                       │    │
│  └────────────────────────────────────────────────────────┘    │
└────────────────────────────────────────────────────────────────┘
```

---

## Layer Responsibilities

### 1. Presentation Layer (UI)
**Technology**: JavaFX 21+

**Responsibilities**:
- User interface rendering
- User input handling
- Data visualization (charts, tables)
- View state management (MVVM pattern)

**Key Components**:
- `MainWindow`: Application main window
- `DashboardView`: Real-time monitoring and status
- `TradingView`: Manual trading interface
- `ConfigView`: Application configuration
- `ViewModels`: Data binding between UI and business logic

### 2. Application Layer
**Responsibilities**:
- Application workflow coordination
- Use case implementation
- Transaction management
- Event handling

**Key Components**:
- `TradingController`: Orchestrates trading operations
- `StateMachine`: Manages trading lifecycle states
- `EventBus`: Publish/subscribe for application events

### 3. Domain Layer
**Responsibilities**:
- Core business logic
- Trading algorithms
- Domain rules enforcement
- Entity behavior

**Key Components**:
- `AITradingAgent`: Main trading decision engine
- `PositionManager`: Position lifecycle management
- `RiskManager`: Risk rules and limits
- `TechnicalIndicators`: Market analysis calculations
- `TradingStrategy`: Strategy implementations

**Domain Models**:
- `Position`: Trading position entity
- `Order`: Order entity
- `Candlestick`: Market data
- `TradeSignal`: Trading signal
- `TradingState`: Application state

### 4. Infrastructure Layer
**Responsibilities**:
- External system integration
- Data persistence
- Technical services
- Framework implementations

**Key Components**:
- `ExchangeConnectors`: API integration with exchanges
- `Database`: SQLite persistence
- `ConfigurationManager`: Configuration handling
- `Logger`: Logging infrastructure
- `WebhookServer`: TradingView webhook listener

---

## Component Details

### AI Trading Agent

```kotlin
class AITradingAgent(
    private val config: TradingConfig,
    private val indicators: TechnicalIndicators,
    private val positionManager: PositionManager,
    private val riskManager: RiskManager
) {
    // Analyze market and make trading decisions
    suspend fun analyzeMarket(candlesticks: List<Candlestick>): TradeSignal {
        val rsi = indicators.calculateRSI(candlesticks)
        val macd = indicators.calculateMACD(candlesticks)
        val sma = indicators.calculateSMA(candlesticks, period = 20)
        
        // Strategy logic here
        return determineSignal(rsi, macd, sma)
    }
    
    // Execute trade based on signal
    suspend fun executeTrade(signal: TradeSignal) {
        if (!riskManager.canExecute(signal)) return
        
        val position = positionManager.openPosition(signal)
        // Monitor position
    }
}
```

### State Machine

```kotlin
enum class TradingState {
    IDLE,           // Not trading
    INITIALIZING,   // Setting up connections
    MONITORING,     // Watching market
    ANALYZING,      // Running analysis
    EXECUTING,      // Placing orders
    MANAGING,       // Managing positions
    CLOSING,        // Closing positions
    ERROR,          // Error state
    STOPPED         // User stopped
}

class TradingStateMachine {
    private var currentState: TradingState = TradingState.IDLE
    
    fun transition(to: TradingState) {
        if (isValidTransition(currentState, to)) {
            onExit(currentState)
            currentState = to
            onEnter(to)
        }
    }
}
```

### Exchange Connector Interface

```kotlin
interface IExchangeConnector {
    // Connection management
    suspend fun connect(): Result<Unit>
    suspend fun disconnect()
    fun isConnected(): Boolean
    
    // Market data
    suspend fun getCandlesticks(
        symbol: String,
        interval: String,
        limit: Int = 100
    ): Result<List<Candlestick>>
    
    suspend fun getTicker(symbol: String): Result<Ticker>
    
    // Order management
    suspend fun placeOrder(order: OrderRequest): Result<Order>
    suspend fun cancelOrder(orderId: String): Result<Unit>
    suspend fun getOrder(orderId: String): Result<Order>
    
    // Position management
    suspend fun getPosition(symbol: String): Result<Position?>
    suspend fun getPositions(): Result<List<Position>>
    suspend fun closePosition(symbol: String): Result<Unit>
    
    // Account
    suspend fun getAccountInfo(): Result<AccountInfo>
    suspend fun getBalance(): Result<Balance>
}
```

---

## Data Models

### Core Entities

```kotlin
// Position entity
data class Position(
    val id: String,
    val symbol: String,
    val side: PositionSide,        // LONG, SHORT
    val entryPrice: Double,
    val currentPrice: Double,
    val quantity: Double,
    val leverage: Int,
    val stopLoss: Double?,
    val takeProfit: Double?,
    val unrealizedPnL: Double,
    val realizedPnL: Double,
    val status: PositionStatus,     // OPEN, CLOSED
    val openedAt: Instant,
    val closedAt: Instant?
)

// Candlestick data
data class Candlestick(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long
)

// Trade signal
data class TradeSignal(
    val action: TradeAction,        // BUY, SELL, HOLD, CLOSE
    val symbol: String,
    val confidence: Double,         // 0.0 to 1.0
    val reason: String,
    val stopLoss: Double?,
    val takeProfit: Double?,
    val indicators: Map<String, Double>,
    val timestamp: Instant
)

// Order request
data class OrderRequest(
    val symbol: String,
    val side: OrderSide,            // BUY, SELL
    val type: OrderType,            // MARKET, LIMIT
    val quantity: Double,
    val price: Double?,
    val stopLoss: Double?,
    val takeProfit: Double?,
    val leverage: Int?
)
```

### Enumerations

```kotlin
enum class TradeAction {
    BUY,     // Open long position
    SELL,    // Open short position
    HOLD,    // No action
    CLOSE    // Close position
}

enum class PositionSide {
    LONG,
    SHORT
}

enum class PositionStatus {
    OPENING,
    OPEN,
    CLOSING,
    CLOSED,
    ERROR
}

enum class OrderSide {
    BUY,
    SELL
}

enum class OrderType {
    MARKET,
    LIMIT,
    STOP_LOSS,
    TAKE_PROFIT
}

enum class OrderStatus {
    NEW,
    PARTIALLY_FILLED,
    FILLED,
    CANCELED,
    REJECTED,
    EXPIRED
}
```

---

## Design Patterns Used

### 1. Factory Pattern
- `ConnectorFactory`: Creates exchange connector instances
- `IndicatorFactory`: Creates technical indicator calculators
- `StrategyFactory`: Creates trading strategy instances

### 2. Strategy Pattern
- `TradingStrategy`: Different trading algorithms
- `RiskStrategy`: Different risk management approaches
- `IndicatorStrategy`: Different indicator combinations

### 3. Observer Pattern
- Event-driven architecture for state changes
- Market data updates
- Position changes

### 4. Repository Pattern
- `TradeRepository`: Trade history persistence
- `ConfigRepository`: Configuration storage
- `MetricsRepository`: Performance metrics

### 5. Command Pattern
- Trading operations (open, close, modify positions)
- Undo/redo functionality
- Transaction logging

### 6. State Pattern
- `TradingStateMachine`: State transitions
- Context-dependent behavior

### 7. Singleton Pattern
- `ConfigurationManager`: Global configuration
- `DatabaseManager`: Database connection
- `Logger`: Application logging

---

## Data Flow

### Trading Flow

```
1. Market Data Collection
   ↓
2. Data Validation & Storage
   ↓
3. Technical Analysis
   ↓
4. Strategy Evaluation
   ↓
5. Signal Generation
   ↓
6. Risk Assessment
   ↓
7. Order Creation
   ↓
8. Order Execution
   ↓
9. Position Monitoring
   ↓
10. Position Management
    ↓
11. Position Closing
    ↓
12. Performance Recording
```

### Detailed Sequence

```
User/Timer → TradingController.startTrading()
                ↓
            StateMachine.transition(MONITORING)
                ↓
            ExchangeConnector.getCandlesticks()
                ↓
            TechnicalIndicators.analyze()
                ↓
            AITradingAgent.generateSignal()
                ↓
            RiskManager.validateSignal()
                ↓
            [If approved]
                ↓
            StateMachine.transition(EXECUTING)
                ↓
            ExchangeConnector.placeOrder()
                ↓
            PositionManager.trackPosition()
                ↓
            StateMachine.transition(MANAGING)
                ↓
            [Monitor position continuously]
                ↓
            [On exit condition]
                ↓
            StateMachine.transition(CLOSING)
                ↓
            ExchangeConnector.closePosition()
                ↓
            TradeRepository.save()
                ↓
            StateMachine.transition(MONITORING)
```

---

## Error Handling Strategy

### Error Categories

1. **Expected Errors** (use Result<T>)
   - Network timeouts
   - API rate limits
   - Invalid parameters
   - Insufficient funds

2. **Unexpected Errors** (throw exceptions)
   - Programming errors
   - Corrupted data
   - System failures

### Error Handling Hierarchy

```kotlin
sealed class TradingError {
    data class NetworkError(val message: String, val cause: Throwable?) : TradingError()
    data class ApiError(val code: Int, val message: String) : TradingError()
    data class ValidationError(val field: String, val message: String) : TradingError()
    data class InsufficientFundsError(val required: Double, val available: Double) : TradingError()
    data class RateLimitError(val retryAfter: Long) : TradingError()
    data class OrderError(val orderId: String, val message: String) : TradingError()
    data class ConfigurationError(val message: String) : TradingError()
    data class UnknownError(val message: String, val cause: Throwable?) : TradingError()
}
```

### Recovery Strategies

- **Retry with exponential backoff**: Network errors, timeouts
- **Circuit breaker**: Repeated API failures
- **Fallback to cached data**: Market data unavailable
- **Safe mode**: Critical errors → stop trading, preserve positions
- **User notification**: All errors logged and displayed

---

## Security Considerations

### API Key Management
- Encrypted storage using AES-256
- Never logged or displayed
- Loaded in memory only when needed
- Cleared after use

### Network Security
- HTTPS for all API calls
- Certificate validation
- No proxy bypassing (security risk)

### Data Protection
- Local database encryption option
- Secure deletion of sensitive data
- No cloud sync without encryption

### Access Control
- Application-level authentication (optional)
- File system permissions
- Process isolation

---

## Performance Considerations

### Optimization Strategies

1. **Caching**
   - Market data caching (short TTL)
   - Configuration caching
   - Indicator results caching

2. **Asynchronous Operations**
   - Non-blocking API calls (coroutines)
   - Parallel market data fetching
   - Background indicator calculation

3. **Database Optimization**
   - Indexed queries
   - Connection pooling
   - Batch inserts
   - Prepared statements

4. **Memory Management**
   - Limited candlestick history
   - Circular buffers for real-time data
   - Lazy loading of trade history

5. **UI Responsiveness**
   - Background threads for heavy operations
   - Progressive rendering
   - Throttled updates (max 60fps)

---

## Testing Strategy

### Unit Tests
- All domain logic (indicators, strategies, risk management)
- Data models and validation
- Utility functions

### Integration Tests
- Exchange connector APIs (using testnet)
- Database operations
- Configuration loading

### UI Tests
- TestFX for JavaFX components
- User workflow scenarios
- Error handling in UI

### Performance Tests
- Load testing with historical data
- Memory leak detection
- Concurrent operations

---

## Deployment Architecture

### Application Structure

```
FMPSAutoTrader/
├── bin/
│   ├── FMPSAutoTrader.exe       # Windows executable
│   └── FMPSAutoTrader.bat        # Batch launcher
├── lib/
│   ├── app.jar                   # Application JAR
│   └── dependencies/             # All dependencies
├── config/
│   ├── application.conf          # Default config
│   └── exchanges.conf            # Exchange settings
├── data/
│   └── autotrader.db             # SQLite database
├── logs/
│   └── application.log           # Log files
├── jre/                          # Bundled JRE (optional)
└── README.txt
```

### System Requirements

- **OS**: Windows 10/11 (64-bit)
- **RAM**: 2GB minimum, 4GB recommended
- **Disk**: 500MB for application + data
- **Java**: JRE 17+ (bundled or system)
- **Network**: Stable internet connection

---

**Document Status**: DRAFT v1.0  
**Next Steps**: Implementation following Development Plan

