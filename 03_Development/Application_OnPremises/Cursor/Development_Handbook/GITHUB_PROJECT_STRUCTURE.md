# GitHub Project Structure for FMPS AutoTrader

## Project Setup

**Project Name**: FMPS AutoTrader v1.0 Development  
**Type**: Board  
**Template**: Team backlog  

### Columns
1. 📋 Backlog
2. 🔜 To Do
3. 🏗️ In Progress
4. 👀 In Review
5. ✅ Done

---

## Milestones

### Milestone 1: Foundation & Infrastructure (Weeks 1-2)
**Due**: Week 2  
**Description**: Project setup, multi-module structure, database, API framework

### Milestone 2: Exchange Integration (Weeks 3-5)
**Due**: Week 5  
**Description**: Binance & Bitget connectors, technical indicators

### Milestone 3: AI Trading Engine (Weeks 6-8)
**Due**: Week 8  
**Description**: AI Trader, Position Manager, Risk Manager, Pattern Storage

### Milestone 4: Desktop UI (Weeks 9-12)
**Due**: Week 12  
**Description**: JavaFX UI, API client, real-time updates

### Milestone 5: Windows Service (Weeks 13-14)
**Due**: Week 14  
**Description**: Service installation, deployment, integration

### Milestone 6: Testing & Polish (Weeks 15-18)
**Due**: Week 18  
**Description**: Complete testing, documentation, release preparation

---

## Labels

- `priority: critical` - Must be completed for v1.0
- `priority: high` - Important for v1.0
- `priority: medium` - Nice to have for v1.0
- `priority: low` - Can be deferred to v1.1

- `type: feature` - New functionality
- `type: bug` - Bug fix
- `type: documentation` - Documentation update
- `type: test` - Test coverage
- `type: infrastructure` - Build/CI/CD

- `module: core` - Core service
- `module: ui` - Desktop UI
- `module: connectors` - Exchange connectors
- `module: database` - Database layer
- `module: testing` - Testing infrastructure

- `status: blocked` - Blocked by dependency
- `status: needs-review` - Ready for code review

---

## Epic Issues

### Epic #1: Project Foundation
**Description**: Set up the foundational architecture for the project
**Milestone**: Foundation & Infrastructure
**Issues**: #2-#10

### Epic #2: Exchange Integration
**Description**: Integrate with Binance and Bitget exchanges
**Milestone**: Exchange Integration
**Issues**: #11-#25

### Epic #3: AI Trading Engine
**Description**: Core AI trading logic and management
**Milestone**: AI Trading Engine
**Issues**: #26-#45

### Epic #4: Desktop UI
**Description**: JavaFX desktop user interface
**Milestone**: Desktop UI
**Issues**: #46-#60

### Epic #5: Windows Service
**Description**: Deploy core as Windows service
**Milestone**: Windows Service
**Issues**: #61-#68

### Epic #6: Testing & Release
**Description**: Complete testing and release preparation
**Milestone**: Testing & Polish
**Issues**: #69-#80

---

## Individual Issues

---

### Issue #2: Set up Gradle multi-module project structure
**Epic**: #1  
**Milestone**: Foundation & Infrastructure  
**Labels**: `priority: critical`, `type: infrastructure`, `module: core`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Create the multi-module Gradle project structure with Kotlin DSL.

**Tasks:**
- [ ] Create root `build.gradle.kts`
- [ ] Create `settings.gradle.kts` with module declarations
- [ ] Set up `shared` module
- [ ] Set up `core-service` module
- [ ] Set up `desktop-ui` module
- [ ] Configure dependencies between modules
- [ ] Add version catalog for dependency management
- [ ] Test build: `./gradlew build`

**Acceptance Criteria:**
- All modules build successfully
- Modules can depend on each other
- Gradle wrapper works

**Dependencies:** None

---

### Issue #3: Configure database layer with Exposed ORM
**Epic**: #1  
**Milestone**: Foundation & Infrastructure  
**Labels**: `priority: critical`, `type: infrastructure`, `module: database`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Set up SQLite database with Exposed ORM for data persistence.

**Tasks:**
- [ ] Add Exposed dependencies
- [ ] Create `DatabaseConfig` class
- [ ] Implement connection pooling (HikariCP)
- [ ] Create migration system (Flyway)
- [ ] Define database schema:
  - [ ] `ai_traders` table
  - [ ] `trade_history` table
  - [ ] `patterns` table
  - [ ] `positions` table
- [ ] Add initial migration scripts
- [ ] Create database initialization logic
- [ ] Write tests for database setup

**Acceptance Criteria:**
- Database creates automatically on first run
- Migrations run successfully
- All tables created with correct schema
- Connection pooling works

**Dependencies:** #2

---

### Issue #4: Set up REST API server with Ktor
**Epic**: #1  
**Milestone**: Foundation & Infrastructure  
**Labels**: `priority: critical`, `type: infrastructure`, `module: core`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Set up Ktor server for REST API and WebSocket communication.

**Tasks:**
- [ ] Add Ktor dependencies
- [ ] Create `ApiServer` class
- [ ] Configure routing
- [ ] Add content negotiation (JSON with Jackson)
- [ ] Add CORS support
- [ ] Implement authentication/authorization (bearer token)
- [ ] Add WebSocket support for real-time updates
- [ ] Configure error handling middleware
- [ ] Add request/response logging
- [ ] Create health check endpoint `/health`
- [ ] Write API server tests

**Acceptance Criteria:**
- API server starts on port 8080
- Health check returns 200 OK
- JSON serialization works
- WebSocket connection established

**Dependencies:** #2

---

### Issue #5: Implement logging infrastructure
**Epic**: #1  
**Milestone**: Foundation & Infrastructure  
**Labels**: `priority: high`, `type: infrastructure`, `module: core`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Set up structured logging with SLF4J and Logback.

**Tasks:**
- [ ] Add SLF4J and Logback dependencies
- [ ] Create `logback.xml` configuration
- [ ] Configure log levels (INFO default, DEBUG for dev)
- [ ] Set up file appenders with rotation
- [ ] Configure console appender with color
- [ ] Add MDC for request tracing
- [ ] Create `LoggerFactory` wrapper
- [ ] Define logging format with timestamps
- [ ] Add performance metrics logging

**Acceptance Criteria:**
- Logs written to file and console
- File rotation works (10MB max, keep 7 days)
- Structured JSON log format for production
- Log levels configurable

**Dependencies:** #2

---

### Issue #6: Create shared data models
**Epic**: #1  
**Milestone**: Foundation & Infrastructure  
**Labels**: `priority: critical`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Create shared data models used across all modules.

**Tasks:**
- [ ] Create `shared` module data classes:
  - [ ] `TraderConfig`
  - [ ] `TraderState` enum
  - [ ] `TraderStatus`
  - [ ] `Position`
  - [ ] `PositionType` enum
  - [ ] `TradeSignal`
  - [ ] `MarketData`
  - [ ] `Candlestick`
  - [ ] `OrderRequest`
  - [ ] `OrderResponse`
  - [ ] `ApiResponse<T>`
- [ ] Add data validation
- [ ] Add JSON serialization annotations
- [ ] Create model builders for testing
- [ ] Write unit tests for models

**Acceptance Criteria:**
- All models compile
- JSON serialization/deserialization works
- Validation rules enforced
- 90%+ test coverage

**Dependencies:** #2

---

### Issue #7: Implement configuration management
**Epic**: #1  
**Milestone**: Foundation & Infrastructure  
**Labels**: `priority: high`, `type: infrastructure`, `module: core`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Create configuration system using HOCON format.

**Tasks:**
- [ ] Add Typesafe Config dependency
- [ ] Create `application.conf` structure
- [ ] Create `ConfigLoader` class
- [ ] Define configuration sections:
  - [ ] Server configuration
  - [ ] Database configuration
  - [ ] Exchange API settings
  - [ ] Trading limits
  - [ ] Logging configuration
- [ ] Support environment variable overrides
- [ ] Add configuration validation
- [ ] Create configuration documentation

**Acceptance Criteria:**
- Configuration loads from file
- Environment variables override settings
- Invalid configuration throws clear errors
- Documentation complete

**Dependencies:** #2

---

### Issue #8: Set up testing infrastructure
**Epic**: #1  
**Milestone**: Foundation & Infrastructure  
**Labels**: `priority: critical`, `type: test`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Configure testing framework with coverage reporting.

**Tasks:**
- [ ] Apply test configuration from `gradle-test-config-example.kts`
- [ ] Add JUnit 5, Mockk, Kotest dependencies
- [ ] Configure JaCoCo for coverage
- [ ] Set up integration test source sets
- [ ] Configure ktlint for code style
- [ ] Configure detekt for static analysis
- [ ] Create test utilities and fixtures
- [ ] Configure H2 in-memory DB for tests
- [ ] Set up test logging configuration
- [ ] Verify CI/CD pipeline runs tests

**Acceptance Criteria:**
- `./gradlew test` runs all unit tests
- `./gradlew integrationTest` runs integration tests
- Coverage report generated
- 80%+ coverage enforced
- CI/CD pipeline passes

**Dependencies:** #2

---

### Issue #9: Create API endpoint structure
**Epic**: #1  
**Milestone**: Foundation & Infrastructure  
**Labels**: `priority: high`, `type: infrastructure`, `module: core`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Define REST API routes and structure.

**Tasks:**
- [ ] Create route modules:
  - [ ] `/api/traders` - AI Trader management
  - [ ] `/api/positions` - Position tracking
  - [ ] `/api/history` - Trade history
  - [ ] `/api/config` - Configuration
  - [ ] `/api/health` - Health check
  - [ ] `/ws` - WebSocket endpoint
- [ ] Implement request validation
- [ ] Add API versioning (v1)
- [ ] Create OpenAPI/Swagger documentation
- [ ] Implement rate limiting
- [ ] Add request ID tracking
- [ ] Write API endpoint tests

**Acceptance Criteria:**
- All endpoints defined
- OpenAPI doc generated
- Request validation works
- Tests pass with 85%+ coverage

**Dependencies:** #4

---

### Issue #10: Create repository layer
**Epic**: #1  
**Milestone**: Foundation & Infrastructure  
**Labels**: `priority: critical`, `type: infrastructure`, `module: database`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Implement data access layer with repository pattern.

**Tasks:**
- [ ] Create repository interfaces:
  - [ ] `AITraderRepository`
  - [ ] `TradeHistoryRepository`
  - [ ] `PatternRepository`
  - [ ] `PositionRepository`
- [ ] Implement repositories with Exposed
- [ ] Add CRUD operations
- [ ] Implement query methods
- [ ] Add transaction management
- [ ] Create repository tests with H2

**Acceptance Criteria:**
- All repositories implemented
- CRUD operations work
- Transactions handled correctly
- 90%+ test coverage

**Dependencies:** #3, #6

---

### Issue #11: Design exchange connector interface
**Epic**: #2  
**Milestone**: Exchange Integration  
**Labels**: `priority: critical`, `type: feature`, `module: connectors`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Design `IExchangeConnector` interface for exchange integration.

**Tasks:**
- [ ] Define `IExchangeConnector` interface:
  - [ ] Connection methods (connect, disconnect, isConnected)
  - [ ] Market data methods (getCandlesticks, getTicker, getOrderBook)
  - [ ] Order methods (placeOrder, cancelOrder, getOrder)
  - [ ] Position methods (getPosition, closePosition)
  - [ ] Account methods (getBalance, getAccountInfo)
- [ ] Define result types (success/failure)
- [ ] Add error handling strategies
- [ ] Create connector factory
- [ ] Document interface contract

**Acceptance Criteria:**
- Interface complete and documented
- Error handling defined
- Factory pattern implemented

**Dependencies:** #6

---

### Issue #12: Implement Binance testnet connector - Market data
**Epic**: #2  
**Milestone**: Exchange Integration  
**Labels**: `priority: critical`, `type: feature`, `module: connectors`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Implement Binance testnet connector for market data retrieval.

**Tasks:**
- [ ] Create `BinanceConnector` class
- [ ] Implement authentication (HMAC SHA256)
- [ ] Implement REST endpoints:
  - [ ] Get server time
  - [ ] Get candlesticks (OHLCV)
  - [ ] Get ticker price
  - [ ] Get order book
- [ ] Add rate limiting (1200 req/min)
- [ ] Implement retry logic with exponential backoff
- [ ] Add request signing
- [ ] Handle API errors
- [ ] Write unit tests with mocked responses
- [ ] Write integration tests with testnet

**Acceptance Criteria:**
- Can fetch candlestick data from testnet
- Rate limiting works
- Errors handled gracefully
- 85%+ test coverage

**Dependencies:** #11

---

### Issue #13: Implement Binance testnet connector - Trading
**Epic**: #2  
**Milestone**: Exchange Integration  
**Labels**: `priority: critical`, `type: feature`, `module: connectors`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Implement Binance testnet trading operations.

**Tasks:**
- [ ] Implement trading endpoints:
  - [ ] Place market order
  - [ ] Place limit order
  - [ ] Cancel order
  - [ ] Get order status
  - [ ] Get open orders
- [ ] Implement position management:
  - [ ] Get positions
  - [ ] Close position
  - [ ] Set leverage
  - [ ] Set margin type
- [ ] Implement account endpoints:
  - [ ] Get balance
  - [ ] Get account info
- [ ] Add demo account validation
- [ ] Write tests (unit + integration)

**Acceptance Criteria:**
- Can place and cancel orders on testnet
- Position management works
- Demo account balance tracked
- 85%+ test coverage

**Dependencies:** #12

---

### Issue #14: Implement Binance WebSocket streaming
**Epic**: #2  
**Milestone**: Exchange Integration  
**Labels**: `priority: high`, `type: feature`, `module: connectors`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Add WebSocket support for real-time market data.

**Tasks:**
- [ ] Create `BinanceWebSocketClient`
- [ ] Implement WebSocket connection management
- [ ] Subscribe to streams:
  - [ ] Kline/candlestick stream
  - [ ] Trade stream
  - [ ] Ticker stream
  - [ ] User data stream (orders/positions)
- [ ] Handle reconnection logic
- [ ] Parse WebSocket messages
- [ ] Emit events to subscribers
- [ ] Write tests

**Acceptance Criteria:**
- WebSocket connects to testnet
- Receives real-time candle updates
- Reconnects on disconnect
- Events emitted correctly

**Dependencies:** #12

---

### Issue #15: Implement Bitget testnet connector - Market data
**Epic**: #2  
**Milestone**: Exchange Integration  
**Labels**: `priority: critical`, `type: feature`, `module: connectors`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Implement Bitget testnet connector for market data.

**Tasks:**
- [ ] Create `BitgetConnector` class
- [ ] Implement authentication
- [ ] Implement REST endpoints:
  - [ ] Get server time
  - [ ] Get candlesticks
  - [ ] Get ticker
  - [ ] Get order book
- [ ] Add rate limiting
- [ ] Implement error handling
- [ ] Write unit tests
- [ ] Write integration tests

**Acceptance Criteria:**
- Can fetch market data from Bitget testnet
- Authentication works
- 85%+ test coverage

**Dependencies:** #11

---

### Issue #16: Implement Bitget testnet connector - Trading
**Epic**: #2  
**Milestone**: Exchange Integration  
**Labels**: `priority: critical`, `type: feature`, `module: connectors`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Implement Bitget testnet trading operations.

**Tasks:**
- [ ] Implement trading endpoints
- [ ] Implement position management
- [ ] Implement account endpoints
- [ ] Add demo validation
- [ ] Write tests (unit + integration)

**Acceptance Criteria:**
- Can trade on Bitget testnet
- Position management works
- 85%+ test coverage

**Dependencies:** #15

---

### Issue #17: Implement Bitget WebSocket streaming
**Epic**: #2  
**Milestone**: Exchange Integration  
**Labels**: `priority: high`, `type: feature`, `module: connectors`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Add WebSocket support for Bitget real-time data.

**Tasks:**
- [ ] Create `BitgetWebSocketClient`
- [ ] Implement connection management
- [ ] Subscribe to data streams
- [ ] Handle reconnection
- [ ] Parse messages
- [ ] Write tests

**Acceptance Criteria:**
- WebSocket works with Bitget
- Real-time updates received
- Reconnection works

**Dependencies:** #15

---

### Issue #18: Implement RSI indicator
**Epic**: #2  
**Milestone**: Exchange Integration  
**Labels**: `priority: critical`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Implement RSI (Relative Strength Index) technical indicator.

**Tasks:**
- [ ] Create `RSICalculator` class
- [ ] Implement RSI algorithm (period 14)
- [ ] Handle edge cases (insufficient data)
- [ ] Optimize for performance
- [ ] Write comprehensive tests with known values
- [ ] Document formula and usage

**Acceptance Criteria:**
- RSI calculated correctly (verified with known data)
- Handles edge cases
- 90%+ test coverage
- Performance < 1ms for 100 candles

**Dependencies:** #6

---

### Issue #19: Implement MACD indicator
**Epic**: #2  
**Milestone**: Exchange Integration  
**Labels**: `priority: critical`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Implement MACD (Moving Average Convergence Divergence) indicator.

**Tasks:**
- [ ] Create `MACDCalculator` class
- [ ] Implement MACD algorithm (12, 26, 9 periods)
- [ ] Calculate MACD line, signal line, histogram
- [ ] Handle edge cases
- [ ] Optimize performance
- [ ] Write tests with known values

**Acceptance Criteria:**
- MACD calculated correctly
- Returns all three values (MACD, signal, histogram)
- 90%+ test coverage

**Dependencies:** #6

---

### Issue #20: Implement SMA indicator
**Epic**: #2  
**Milestone**: Exchange Integration  
**Labels**: `priority: critical`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 0.5 days

**Description:**
Implement SMA (Simple Moving Average) indicator.

**Tasks:**
- [ ] Create `SMACalculator` class
- [ ] Implement SMA algorithm
- [ ] Support configurable period
- [ ] Write tests

**Acceptance Criteria:**
- SMA calculated correctly
- 90%+ test coverage

**Dependencies:** #6

---

### Issue #21: Implement EMA indicator
**Epic**: #2  
**Milestone**: Exchange Integration  
**Labels**: `priority: critical`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Implement EMA (Exponential Moving Average) indicator.

**Tasks:**
- [ ] Create `EMACalculator` class
- [ ] Implement EMA algorithm
- [ ] Support configurable period
- [ ] Write tests with known values

**Acceptance Criteria:**
- EMA calculated correctly
- 90%+ test coverage

**Dependencies:** #6

---

### Issue #22: Implement Bollinger Bands indicator
**Epic**: #2  
**Milestone**: Exchange Integration  
**Labels**: `priority: medium`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Implement Bollinger Bands technical indicator.

**Tasks:**
- [ ] Create `BollingerBandsCalculator` class
- [ ] Implement algorithm (20 period, 2 std dev)
- [ ] Calculate upper, middle, lower bands
- [ ] Write tests

**Acceptance Criteria:**
- Bollinger Bands calculated correctly
- Returns all three bands
- 90%+ test coverage

**Dependencies:** #20

---

### Issue #23: Create indicator calculation service
**Epic**: #2  
**Milestone**: Exchange Integration  
**Labels**: `priority: high`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Create service to coordinate indicator calculations.

**Tasks:**
- [ ] Create `IndicatorService` class
- [ ] Batch calculate multiple indicators
- [ ] Add result caching
- [ ] Optimize calculation order
- [ ] Write tests

**Acceptance Criteria:**
- Can calculate all indicators at once
- Caching works correctly
- Performance optimized

**Dependencies:** #18, #19, #20, #21, #22

---

### Issue #24: Add exchange connector error handling
**Epic**: #2  
**Milestone**: Exchange Integration  
**Labels**: `priority: high`, `type: feature`, `module: connectors`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Implement comprehensive error handling for connectors.

**Tasks:**
- [ ] Define error types:
  - [ ] Network errors
  - [ ] API errors
  - [ ] Rate limit errors
  - [ ] Authentication errors
  - [ ] Invalid request errors
- [ ] Implement retry strategies
- [ ] Add circuit breaker pattern
- [ ] Log all errors with context
- [ ] Create error recovery mechanisms
- [ ] Write error handling tests

**Acceptance Criteria:**
- All error types handled
- Retry logic works
- Circuit breaker prevents cascading failures
- Comprehensive logging

**Dependencies:** #12, #13, #15, #16

---

### Issue #25: Create exchange connector health monitoring
**Epic**: #2  
**Milestone**: Exchange Integration  
**Labels**: `priority: medium`, `type: feature`, `module: connectors`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Add health monitoring for exchange connections.

**Tasks:**
- [ ] Create `ConnectorHealthMonitor` class
- [ ] Periodic health checks (ping)
- [ ] Connection state tracking
- [ ] Latency monitoring
- [ ] Emit health events
- [ ] Add health metrics to API
- [ ] Write tests

**Acceptance Criteria:**
- Health check runs every 30 seconds
- Connection failures detected
- Metrics available via API

**Dependencies:** #11, #12, #15

---

### Issue #26: Implement AI Trader core class
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: critical`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Create core AI Trader class with state management.

**Tasks:**
- [ ] Create `AITrader` class
- [ ] Implement state machine:
  - [ ] IDLE, STARTING, RUNNING, STOPPING, STOPPED, ERROR
- [ ] Configuration management
- [ ] Start/stop lifecycle
- [ ] Market data fetching loop
- [ ] Signal generation logic
- [ ] Event emission
- [ ] Error handling
- [ ] Write comprehensive tests

**Acceptance Criteria:**
- State transitions work correctly
- Can start/stop trader
- Market data fetched continuously
- 90%+ test coverage

**Dependencies:** #23, #12

---

### Issue #27: Implement trend following strategy
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: critical`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Implement trend following trading strategy.

**Tasks:**
- [ ] Create `TrendFollowingStrategy` class
- [ ] Define strategy logic:
  - [ ] Use SMA crossovers (20/50 periods)
  - [ ] Confirm with RSI and MACD
  - [ ] Entry conditions
  - [ ] Exit conditions
- [ ] Implement signal generation
- [ ] Add strategy configuration
- [ ] Backtest on historical data
- [ ] Write tests with various market conditions

**Acceptance Criteria:**
- Strategy generates buy/sell signals
- Works with backtesting
- Tested on bull/bear/sideways markets
- 85%+ test coverage

**Dependencies:** #23, #26

---

### Issue #28: Implement mean reversion strategy
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: high`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Implement mean reversion trading strategy.

**Tasks:**
- [ ] Create `MeanReversionStrategy` class
- [ ] Define strategy logic:
  - [ ] Use Bollinger Bands
  - [ ] Identify oversold/overbought with RSI
  - [ ] Entry/exit conditions
- [ ] Implement signal generation
- [ ] Backtest strategy
- [ ] Write tests

**Acceptance Criteria:**
- Strategy generates signals
- Backtesting shows reasonable performance
- 85%+ test coverage

**Dependencies:** #22, #23, #26

---

### Issue #29: Implement breakout strategy
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: high`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Implement breakout trading strategy.

**Tasks:**
- [ ] Create `BreakoutStrategy` class
- [ ] Define strategy logic:
  - [ ] Identify support/resistance levels
  - [ ] Detect breakouts
  - [ ] Confirm with volume
  - [ ] Entry/exit conditions
- [ ] Implement signal generation
- [ ] Backtest strategy
- [ ] Write tests

**Acceptance Criteria:**
- Strategy detects breakouts
- Generates valid signals
- 85%+ test coverage

**Dependencies:** #23, #26

---

### Issue #30: Implement AI Trader Manager
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: critical`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Create AI Trader Manager to handle multiple trader instances.

**Tasks:**
- [ ] Create `AITraderManager` class
- [ ] Instance management:
  - [ ] Create trader (max 3)
  - [ ] Start trader
  - [ ] Stop trader
  - [ ] Delete trader
  - [ ] Get trader status
- [ ] Enforce 3-trader limit
- [ ] Resource allocation per trader
- [ ] State persistence to database
- [ ] Recovery logic on restart
- [ ] Write comprehensive tests

**Acceptance Criteria:**
- Can manage up to 3 traders
- 4th trader creation fails
- State persists across restarts
- 90%+ test coverage

**Dependencies:** #26, #10

---

### Issue #31: Implement Position Manager
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: critical`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Create Position Manager for tracking open positions.

**Tasks:**
- [ ] Create `PositionManager` class
- [ ] Position lifecycle:
  - [ ] Open position
  - [ ] Update position
  - [ ] Close position
- [ ] P&L calculation (real-time)
- [ ] Position history tracking
- [ ] Stop-loss management
- [ ] Position persistence
- [ ] Recovery on restart
- [ ] Write tests

**Acceptance Criteria:**
- Positions tracked accurately
- P&L calculated correctly
- Stop-loss triggers work
- 90%+ test coverage

**Dependencies:** #10, #12

---

### Issue #32: Implement Risk Manager
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: critical`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Create Risk Manager for trade validation and limits.

**Tasks:**
- [ ] Create `RiskManager` class
- [ ] Risk checks:
  - [ ] Budget validation
  - [ ] Leverage limit enforcement
  - [ ] Maximum position size
  - [ ] Total exposure limit
  - [ ] Stop-loss validation
- [ ] Emergency stop functionality
- [ ] Risk scoring system
- [ ] Validation before every trade
- [ ] Write comprehensive tests

**Acceptance Criteria:**
- All risk checks enforced
- Emergency stop works
- Invalid trades blocked
- 95%+ test coverage

**Dependencies:** #26, #31

---

### Issue #33: Design pattern storage schema
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: high`, `type: infrastructure`, `module: database`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Design database schema for pattern storage.

**Tasks:**
- [ ] Define `patterns` table structure:
  - [ ] Pattern ID
  - [ ] Symbol
  - [ ] Entry conditions (indicators)
  - [ ] Exit conditions
  - [ ] Success rate
  - [ ] Average profit
  - [ ] Sample size
  - [ ] Created/updated timestamps
- [ ] Create migration script
- [ ] Add indexes for queries
- [ ] Document schema

**Acceptance Criteria:**
- Schema designed and documented
- Migration script created
- Indexes optimized for queries

**Dependencies:** #3

---

### Issue #34: Implement Pattern Service
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: high`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Implement pattern storage and matching service.

**Tasks:**
- [ ] Create `PatternService` class
- [ ] Store successful trades as patterns:
  - [ ] Extract indicators at entry
  - [ ] Store outcome
  - [ ] Update success rate
- [ ] Pattern matching algorithm:
  - [ ] Find similar patterns
  - [ ] Calculate similarity score
  - [ ] Rank by relevance
- [ ] Query patterns by criteria
- [ ] Pattern expiration (old patterns)
- [ ] Write tests

**Acceptance Criteria:**
- Patterns stored correctly
- Matching algorithm works
- Similar patterns found
- 85%+ test coverage

**Dependencies:** #33, #10

---

### Issue #35: Implement backtesting engine
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: medium`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Create backtesting engine to test strategies on historical data.

**Tasks:**
- [ ] Create `BacktestingEngine` class
- [ ] Load historical candlestick data
- [ ] Simulate trading with strategy
- [ ] Calculate performance metrics:
  - [ ] Total return
  - [ ] Win rate
  - [ ] Average profit/loss
  - [ ] Maximum drawdown
  - [ ] Sharpe ratio
- [ ] Generate backtest report
- [ ] Write tests

**Acceptance Criteria:**
- Can backtest any strategy
- Performance metrics accurate
- Report generated

**Dependencies:** #27, #28, #29

---

### Issue #36: Implement trade execution logic
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: critical`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Implement logic to execute trades based on signals.

**Tasks:**
- [ ] Create `TradeExecutor` class
- [ ] Signal to order conversion
- [ ] Pre-trade validation (Risk Manager)
- [ ] Order placement via connector
- [ ] Order status tracking
- [ ] Retry logic for failed orders
- [ ] Record trade in history
- [ ] Emit trade events
- [ ] Write tests

**Acceptance Criteria:**
- Trades executed from signals
- Validation enforced
- Failed trades retried
- 90%+ test coverage

**Dependencies:** #26, #31, #32, #12

---

### Issue #37: Implement stop-loss automation
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: critical`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Automate stop-loss management for open positions.

**Tasks:**
- [ ] Create `StopLossManager` class
- [ ] Monitor positions continuously
- [ ] Calculate stop-loss prices
- [ ] Detect stop-loss triggers
- [ ] Automatically close position on trigger
- [ ] Update stop-loss dynamically (trailing)
- [ ] Log all stop-loss actions
- [ ] Write tests

**Acceptance Criteria:**
- Stop-loss triggers automatically
- Positions closed correctly
- Trailing stop-loss works
- 90%+ test coverage

**Dependencies:** #31, #36

---

### Issue #38: Implement performance analytics
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: medium`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Create analytics for trader performance tracking.

**Tasks:**
- [ ] Create `PerformanceAnalytics` class
- [ ] Calculate metrics:
  - [ ] Total profit/loss
  - [ ] Win rate
  - [ ] Average profit per trade
  - [ ] Maximum drawdown
  - [ ] ROI
- [ ] Time-series P&L tracking
- [ ] Performance comparison between traders
- [ ] Generate reports
- [ ] Write tests

**Acceptance Criteria:**
- All metrics calculated correctly
- Reports generated
- 85%+ test coverage

**Dependencies:** #10

---

### Issue #39: Create trader REST API endpoints
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: critical`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Create REST API endpoints for AI Trader management.

**Tasks:**
- [ ] Implement endpoints:
  - [ ] `POST /api/traders` - Create trader
  - [ ] `GET /api/traders` - List all traders
  - [ ] `GET /api/traders/{id}` - Get trader details
  - [ ] `PUT /api/traders/{id}` - Update trader config
  - [ ] `DELETE /api/traders/{id}` - Delete trader
  - [ ] `POST /api/traders/{id}/start` - Start trading
  - [ ] `POST /api/traders/{id}/stop` - Stop trading
  - [ ] `GET /api/traders/{id}/status` - Get status
  - [ ] `GET /api/traders/{id}/performance` - Get metrics
- [ ] Request/response validation
- [ ] Error handling
- [ ] Write API tests

**Acceptance Criteria:**
- All endpoints work
- Validation enforced
- API tests pass
- 85%+ test coverage

**Dependencies:** #30, #9

---

### Issue #40: Create position REST API endpoints
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: high`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Create REST API endpoints for position management.

**Tasks:**
- [ ] Implement endpoints:
  - [ ] `GET /api/positions` - List all positions
  - [ ] `GET /api/positions/{id}` - Get position details
  - [ ] `POST /api/positions/{id}/close` - Manually close position
  - [ ] `PUT /api/positions/{id}/stop-loss` - Update stop-loss
- [ ] Validation
- [ ] Write tests

**Acceptance Criteria:**
- All endpoints work
- 85%+ test coverage

**Dependencies:** #31, #9

---

### Issue #41: Create trade history REST API endpoints
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: medium`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Create REST API endpoints for trade history.

**Tasks:**
- [ ] Implement endpoints:
  - [ ] `GET /api/history` - Get trade history (paginated)
  - [ ] `GET /api/history/{id}` - Get trade details
  - [ ] `GET /api/history/stats` - Get statistics
- [ ] Add filtering (date range, trader, symbol)
- [ ] Pagination support
- [ ] Write tests

**Acceptance Criteria:**
- Endpoints work
- Filtering and pagination work
- 85%+ test coverage

**Dependencies:** #10, #9

---

### Issue #42: Implement WebSocket real-time updates
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: high`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Implement WebSocket server for real-time updates to UI.

**Tasks:**
- [ ] Create `WebSocketService` class
- [ ] Implement connection management
- [ ] Emit events:
  - [ ] Trader state changes
  - [ ] New positions opened
  - [ ] Positions closed
  - [ ] Stop-loss triggered
  - [ ] Market data updates
  - [ ] Performance updates
- [ ] Subscribe/unsubscribe logic
- [ ] Authentication for connections
- [ ] Write tests

**Acceptance Criteria:**
- WebSocket server works
- Events emitted in real-time
- Multiple clients supported
- 85%+ test coverage

**Dependencies:** #4, #30

---

### Issue #43: Implement trader state persistence
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: high`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Persist trader state to survive restarts.

**Tasks:**
- [ ] Save trader configuration
- [ ] Save running state
- [ ] Save active strategies
- [ ] Recovery logic on startup:
  - [ ] Restore traders
  - [ ] Resume if was running
  - [ ] Reconnect to exchanges
- [ ] Handle corrupted state
- [ ] Write tests

**Acceptance Criteria:**
- Traders restored on restart
- Running traders resume
- 90%+ test coverage

**Dependencies:** #30, #10

---

### Issue #44: Implement logging for all trading actions
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: high`, `type: infrastructure`, `module: core`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Add comprehensive logging for trading operations.

**Tasks:**
- [ ] Log trader lifecycle events
- [ ] Log all signals generated
- [ ] Log all trades executed
- [ ] Log position changes
- [ ] Log stop-loss triggers
- [ ] Log risk checks
- [ ] Add structured logging with context
- [ ] Performance metrics logging

**Acceptance Criteria:**
- All key events logged
- Logs structured and searchable
- Performance overhead < 5%

**Dependencies:** #5, #26

---

### Issue #45: End-to-end integration tests for AI Trader
**Epic**: #3  
**Milestone**: AI Trading Engine  
**Labels**: `priority: high`, `type: test`, `module: core`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Create end-to-end tests for complete AI Trader workflows.

**Tasks:**
- [ ] Test: Create → Start → Trade → Stop → Delete
- [ ] Test: Multiple traders running concurrently
- [ ] Test: Stop-loss automation
- [ ] Test: Recovery after restart
- [ ] Test: Error scenarios
- [ ] Test with mock exchange data
- [ ] Performance testing (3 traders)

**Acceptance Criteria:**
- All workflows tested
- Tests pass consistently
- Performance acceptable

**Dependencies:** #30, #31, #32, #36

---

### Issue #46: Set up JavaFX project structure
**Epic**: #4  
**Milestone**: Desktop UI  
**Labels**: `priority: critical`, `type: infrastructure`, `module: ui`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Set up JavaFX desktop UI module.

**Tasks:**
- [ ] Add JavaFX dependencies
- [ ] Configure JavaFX plugin
- [ ] Create FXML file structure
- [ ] Set up CSS for styling
- [ ] Create main application class
- [ ] Configure scenes and navigation
- [ ] Test JavaFX launches

**Acceptance Criteria:**
- JavaFX application launches
- Navigation between scenes works
- Modern UI theme applied

**Dependencies:** #2

---

### Issue #47: Create API client library
**Epic**: #4  
**Milestone**: Desktop UI  
**Labels**: `priority: critical`, `type: infrastructure`, `module: ui`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Create client library to communicate with Core API.

**Tasks:**
- [ ] Create `ApiClient` class with Ktor client
- [ ] Implement all REST endpoints:
  - [ ] Trader management
  - [ ] Position management
  - [ ] History retrieval
  - [ ] Configuration
- [ ] Add authentication (bearer token)
- [ ] Error handling
- [ ] Retry logic
- [ ] Connection monitoring
- [ ] Write tests

**Acceptance Criteria:**
- All API calls work
- Errors handled gracefully
- 85%+ test coverage

**Dependencies:** #39, #40, #41

---

### Issue #48: Create WebSocket client for real-time updates
**Epic**: #4  
**Milestone**: Desktop UI  
**Labels**: `priority: high`, `type: infrastructure`, `module: ui`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Create WebSocket client for real-time UI updates.

**Tasks:**
- [ ] Create `WebSocketClient` class
- [ ] Connect to Core WebSocket
- [ ] Subscribe to events
- [ ] Parse incoming messages
- [ ] Emit to UI observers
- [ ] Reconnection logic
- [ ] Write tests

**Acceptance Criteria:**
- WebSocket connects to Core
- Real-time updates received
- Reconnection works
- 85%+ test coverage

**Dependencies:** #42, #47

---

### Issue #49: Implement Dashboard View
**Epic**: #4  
**Milestone**: Desktop UI  
**Labels**: `priority: critical`, `type: feature`, `module: ui`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Create main dashboard view with overview.

**Tasks:**
- [ ] Create `DashboardView.fxml`
- [ ] Create `DashboardViewModel`
- [ ] Show summary:
  - [ ] Active traders count
  - [ ] Total P&L
  - [ ] Open positions count
  - [ ] Recent trades
- [ ] Real-time updates via WebSocket
- [ ] Refresh data periodically
- [ ] Add navigation to other views
- [ ] Style with modern UI
- [ ] Write UI tests

**Acceptance Criteria:**
- Dashboard displays correctly
- Real-time updates work
- Navigation functional
- UI tests pass

**Dependencies:** #47, #48

---

### Issue #50: Implement AI Trader Management View
**Epic**: #4  
**Milestone**: Desktop UI  
**Labels**: `priority: critical`, `type: feature`, `module: ui`  
**Assignee**: -  
**Estimate**: 4 days

**Description:**
Create view to manage AI Traders.

**Tasks:**
- [ ] Create `TraderManagementView.fxml`
- [ ] Create `TraderManagementViewModel`
- [ ] List all traders (table view)
- [ ] Show trader details
- [ ] Create new trader (dialog):
  - [ ] Name
  - [ ] Exchange
  - [ ] Trading pair
  - [ ] Budget
  - [ ] Leverage
  - [ ] Strategy selection
- [ ] Start/stop trader buttons
- [ ] Delete trader (with confirmation)
- [ ] Update trader configuration
- [ ] Real-time status updates
- [ ] Validation (max 3 traders)
- [ ] Write UI tests

**Acceptance Criteria:**
- Can create/start/stop/delete traders
- Max 3 traders enforced in UI
- Real-time status updates
- UI tests pass

**Dependencies:** #47, #48

---

### Issue #51: Implement Position Monitoring View
**Epic**: #4  
**Milestone**: Desktop UI  
**Labels**: `priority: critical`, `type: feature`, `module: ui`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Create view to monitor open positions.

**Tasks:**
- [ ] Create `PositionMonitoringView.fxml`
- [ ] Create `PositionMonitoringViewModel`
- [ ] List open positions (table):
  - [ ] Symbol
  - [ ] Type (LONG/SHORT)
  - [ ] Entry price
  - [ ] Current price
  - [ ] P&L
  - [ ] Stop-loss
- [ ] Real-time P&L updates
- [ ] Manual close position button
- [ ] Update stop-loss dialog
- [ ] Color coding (green profit, red loss)
- [ ] Write UI tests

**Acceptance Criteria:**
- Positions displayed correctly
- Real-time P&L updates
- Can close positions manually
- UI tests pass

**Dependencies:** #47, #48

---

### Issue #52: Implement Trade History View
**Epic**: #4  
**Milestone**: Desktop UI  
**Labels**: `priority: high`, `type: feature`, `module: ui`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Create view to display trade history.

**Tasks:**
- [ ] Create `TradeHistoryView.fxml`
- [ ] Create `TradeHistoryViewModel`
- [ ] List historical trades (table):
  - [ ] Date/time
  - [ ] Trader
  - [ ] Symbol
  - [ ] Type
  - [ ] Entry/exit prices
  - [ ] Profit/loss
- [ ] Pagination
- [ ] Filtering (date range, trader, symbol)
- [ ] Export to CSV
- [ ] Show statistics summary
- [ ] Write UI tests

**Acceptance Criteria:**
- Trade history displayed
- Filtering works
- Export to CSV works
- UI tests pass

**Dependencies:** #47

---

### Issue #53: Implement Configuration View
**Epic**: #4  
**Milestone**: Desktop UI  
**Labels**: `priority: medium`, `type: feature`, `module: ui`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Create view for application settings.

**Tasks:**
- [ ] Create `ConfigurationView.fxml`
- [ ] Create `ConfigurationViewModel`
- [ ] Exchange API settings:
  - [ ] Binance testnet keys
  - [ ] Bitget testnet keys
- [ ] Trading limits:
  - [ ] Max traders
  - [ ] Default leverage
  - [ ] Default stop-loss %
- [ ] UI preferences:
  - [ ] Theme selection
  - [ ] Refresh interval
- [ ] Save/load configuration
- [ ] Validation
- [ ] Write tests

**Acceptance Criteria:**
- Configuration can be updated
- Settings persist
- Validation works

**Dependencies:** #47

---

### Issue #54: Implement Performance Analytics View
**Epic**: #4  
**Milestone**: Desktop UI  
**Labels**: `priority: medium`, `type: feature`, `module: ui`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Create view with charts and analytics.

**Tasks:**
- [ ] Create `AnalyticsView.fxml`
- [ ] Create `AnalyticsViewModel`
- [ ] Add charts:
  - [ ] P&L over time (line chart)
  - [ ] Win rate per trader (bar chart)
  - [ ] Profit distribution (pie chart)
- [ ] Show key metrics
- [ ] Time range selection
- [ ] Export charts as images
- [ ] Write tests

**Acceptance Criteria:**
- Charts display correctly
- Data accurate
- Interactive time ranges

**Dependencies:** #47

---

### Issue #55: Implement real-time notifications
**Epic**: #4  
**Milestone**: Desktop UI  
**Labels**: `priority: medium`, `type: feature`, `module: ui`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Add toast notifications for important events.

**Tasks:**
- [ ] Create notification system
- [ ] Show notifications for:
  - [ ] Position opened
  - [ ] Position closed
  - [ ] Stop-loss triggered
  - [ ] Trader started/stopped
  - [ ] Errors
- [ ] Notification queue
- [ ] Auto-dismiss after timeout
- [ ] Click to view details

**Acceptance Criteria:**
- Notifications appear for events
- Auto-dismiss works
- Queue handles multiple notifications

**Dependencies:** #48

---

### Issue #56: Create modern UI theme
**Epic**: #4  
**Milestone**: Desktop UI  
**Labels**: `priority: medium`, `type: feature`, `module: ui`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Design and implement modern UI theme.

**Tasks:**
- [ ] Create CSS stylesheet
- [ ] Define color scheme:
  - [ ] Primary color
  - [ ] Accent color
  - [ ] Success/error colors
  - [ ] Background colors
- [ ] Style all components:
  - [ ] Buttons
  - [ ] Tables
  - [ ] Forms
  - [ ] Dialogs
  - [ ] Charts
- [ ] Add icons
- [ ] Ensure accessibility
- [ ] Test on different screen sizes

**Acceptance Criteria:**
- UI looks modern and professional
- Consistent styling across app
- Accessible (contrast ratios met)

**Dependencies:** #46

---

### Issue #57: Add input validation to all forms
**Epic**: #4  
**Milestone**: Desktop UI  
**Labels**: `priority: high`, `type: feature`, `module: ui`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Implement comprehensive input validation.

**Tasks:**
- [ ] Validate trader creation form
- [ ] Validate configuration form
- [ ] Show validation errors inline
- [ ] Prevent invalid submissions
- [ ] Add tooltips for field requirements
- [ ] Test all validation rules

**Acceptance Criteria:**
- Invalid inputs blocked
- Clear error messages shown
- Tooltips helpful

**Dependencies:** #50, #53

---

### Issue #58: Implement error handling in UI
**Epic**: #4  
**Milestone**: Desktop UI  
**Labels**: `priority: high`, `type: feature`, `module: ui`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Add comprehensive error handling for UI.

**Tasks:**
- [ ] Catch API errors
- [ ] Display user-friendly error messages
- [ ] Handle network errors gracefully
- [ ] Show retry options
- [ ] Log errors for debugging
- [ ] Create error dialog component
- [ ] Test error scenarios

**Acceptance Criteria:**
- All errors caught and displayed
- Messages user-friendly
- Retry options available

**Dependencies:** #47

---

### Issue #59: Write UI integration tests
**Epic**: #4  
**Milestone**: Desktop UI  
**Labels**: `priority: high`, `type: test`, `module: ui`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Create integration tests for UI using TestFX.

**Tasks:**
- [ ] Set up TestFX framework
- [ ] Test workflows:
  - [ ] Create trader
  - [ ] Start trader
  - [ ] View positions
  - [ ] Close position
  - [ ] View history
  - [ ] Update configuration
- [ ] Test navigation
- [ ] Test validation
- [ ] Test error handling

**Acceptance Criteria:**
- All critical workflows tested
- Tests run in CI/CD
- 75%+ coverage for UI

**Dependencies:** #50, #51, #52, #53

---

### Issue #60: Create user documentation
**Epic**: #4  
**Milestone**: Desktop UI  
**Labels**: `priority: medium`, `type: documentation`, `module: ui`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Write user guide for the application.

**Tasks:**
- [ ] Create user manual:
  - [ ] Installation instructions
  - [ ] Getting started guide
  - [ ] Creating first AI Trader
  - [ ] Monitoring positions
  - [ ] Understanding analytics
  - [ ] Troubleshooting
- [ ] Add screenshots
- [ ] Create video tutorial (optional)
- [ ] Include FAQ section

**Acceptance Criteria:**
- Complete user manual created
- Screenshots included
- Clear and easy to follow

**Dependencies:** #50, #51, #52

---

### Issue #61: Create Windows service wrapper
**Epic**: #5  
**Milestone**: Windows Service  
**Labels**: `priority: critical`, `type: infrastructure`, `module: core`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Create wrapper to run Core as Windows service.

**Tasks:**
- [ ] Research Windows service options:
  - [ ] Apache Commons Daemon (procrun)
  - [ ] NSSM (Non-Sucking Service Manager)
  - [ ] Java Service Wrapper
- [ ] Select solution (recommend NSSM)
- [ ] Create service configuration
- [ ] Handle service lifecycle:
  - [ ] Start
  - [ ] Stop
  - [ ] Restart
- [ ] Configure auto-start on boot
- [ ] Set up logging to Windows Event Log
- [ ] Create installation script
- [ ] Test service installation

**Acceptance Criteria:**
- Core runs as Windows service
- Auto-starts on boot
- Can be controlled via Services app
- Logs to Event Log

**Dependencies:** #30, #43

---

### Issue #62: Create installer with Inno Setup
**Epic**: #5  
**Milestone**: Windows Service  
**Labels**: `priority: critical`, `type: infrastructure`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Create Windows installer for the application.

**Tasks:**
- [ ] Set up Inno Setup
- [ ] Create installer script:
  - [ ] Install Core service
  - [ ] Install UI application
  - [ ] Install JRE (bundle or detect)
  - [ ] Create desktop shortcuts
  - [ ] Create Start Menu entries
- [ ] Install service automatically
- [ ] Configure service to auto-start
- [ ] Create uninstaller
- [ ] Test installation process
- [ ] Test uninstallation

**Acceptance Criteria:**
- Installer creates working setup
- Service installed and running
- UI launches correctly
- Uninstaller removes everything

**Dependencies:** #61

---

### Issue #63: Create deployment package
**Epic**: #5  
**Milestone**: Windows Service  
**Labels**: `priority: critical`, `type: infrastructure`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Package all components for distribution.

**Tasks:**
- [ ] Build all modules
- [ ] Create JAR files with dependencies
- [ ] Bundle JRE (optional)
- [ ] Include configuration files
- [ ] Include documentation
- [ ] Create version info file
- [ ] Package into installer
- [ ] Test on clean Windows machine

**Acceptance Criteria:**
- Complete package builds
- Installer works on fresh system
- All components included

**Dependencies:** #62

---

### Issue #64: Configure service logging
**Epic**: #5  
**Milestone**: Windows Service  
**Labels**: `priority: high`, `type: infrastructure`, `module: core`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Configure logging specifically for Windows service.

**Tasks:**
- [ ] Configure file logging location:
  - [ ] `C:\ProgramData\FMPS\AutoTrader\logs\`
- [ ] Log to Windows Event Log
- [ ] Set up log rotation
- [ ] Configure log levels
- [ ] Test logging from service
- [ ] Document log locations

**Acceptance Criteria:**
- Logs written correctly when running as service
- Event Log entries visible
- Log rotation works

**Dependencies:** #61, #5

---

### Issue #65: Implement service health monitoring
**Epic**: #5  
**Milestone**: Windows Service  
**Labels**: `priority: high`, `type: feature`, `module: core`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Add health monitoring for the Windows service.

**Tasks:**
- [ ] Create health check endpoint
- [ ] Monitor Core service status
- [ ] Check database connectivity
- [ ] Check exchange connections
- [ ] Restart on failure (configurable)
- [ ] Send alerts on errors
- [ ] Write tests

**Acceptance Criteria:**
- Health endpoint returns status
- Service restarts on critical errors
- Alerts generated for failures

**Dependencies:** #61

---

### Issue #66: Create service update mechanism
**Epic**: #5  
**Milestone**: Windows Service  
**Labels**: `priority: medium`, `type: infrastructure`, `module: core`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Implement update mechanism for installed service.

**Tasks:**
- [ ] Version checking
- [ ] Download updates
- [ ] Stop service
- [ ] Replace files
- [ ] Restart service
- [ ] Rollback on failure
- [ ] Test update process

**Acceptance Criteria:**
- Updates can be installed
- Service restarts correctly
- Rollback works if update fails

**Dependencies:** #61, #63

---

### Issue #67: Write service deployment documentation
**Epic**: #5  
**Milestone**: Windows Service  
**Labels**: `priority: high`, `type: documentation`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Document deployment and installation process.

**Tasks:**
- [ ] Installation guide
- [ ] Configuration guide
- [ ] Service management guide
- [ ] Troubleshooting guide
- [ ] Uninstallation guide
- [ ] Include screenshots

**Acceptance Criteria:**
- Complete documentation
- Easy to follow
- Covers all scenarios

**Dependencies:** #62, #64

---

### Issue #68: End-to-end deployment testing
**Epic**: #5  
**Milestone**: Windows Service  
**Labels**: `priority: critical`, `type: test`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Test complete deployment on clean Windows systems.

**Tasks:**
- [ ] Test on Windows 10
- [ ] Test on Windows 11
- [ ] Test installation
- [ ] Test service startup
- [ ] Test UI connection to service
- [ ] Test uninstallation
- [ ] Test upgrade process
- [ ] Document any issues

**Acceptance Criteria:**
- Works on Windows 10 and 11
- Installation smooth
- No manual steps required
- Uninstall clean

**Dependencies:** #62, #63

---

### Issue #69: Complete unit test coverage
**Epic**: #6  
**Milestone**: Testing & Polish  
**Labels**: `priority: critical`, `type: test`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Ensure all modules meet 80%+ test coverage.

**Tasks:**
- [ ] Review coverage reports
- [ ] Identify gaps
- [ ] Write missing tests:
  - [ ] Core service: 85%+
  - [ ] Connectors: 85%+
  - [ ] UI: 75%+
  - [ ] Overall: 80%+
- [ ] Fix flaky tests
- [ ] Verify all tests pass

**Acceptance Criteria:**
- Coverage thresholds met
- All tests pass consistently
- No flaky tests

**Dependencies:** All core features

---

### Issue #70: Complete integration test suite
**Epic**: #6  
**Milestone**: Testing & Polish  
**Labels**: `priority: critical`, `type: test`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Create comprehensive integration test suite.

**Tasks:**
- [ ] Exchange connector integration tests
- [ ] Database integration tests
- [ ] API integration tests
- [ ] WebSocket integration tests
- [ ] End-to-end workflow tests
- [ ] Performance tests

**Acceptance Criteria:**
- All integration points tested
- Tests run in CI/CD
- Performance acceptable

**Dependencies:** All integrations complete

---

### Issue #71: Perform security audit
**Epic**: #6  
**Milestone**: Testing & Polish  
**Labels**: `priority: high`, `type: test`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Audit application for security vulnerabilities.

**Tasks:**
- [ ] Run OWASP Dependency Check
- [ ] Review API authentication
- [ ] Check for SQL injection risks
- [ ] Validate input sanitization
- [ ] Review secrets management
- [ ] Test for XSS in UI
- [ ] Check HTTPS/TLS usage
- [ ] Fix any vulnerabilities found

**Acceptance Criteria:**
- No HIGH or CRITICAL vulnerabilities
- Security best practices followed
- Documentation updated

**Dependencies:** All features complete

---

### Issue #72: Performance optimization
**Epic**: #6  
**Milestone**: Testing & Polish  
**Labels**: `priority: medium`, `type: feature`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Optimize application performance.

**Tasks:**
- [ ] Profile application
- [ ] Optimize database queries
- [ ] Add caching where appropriate
- [ ] Optimize indicator calculations
- [ ] Reduce API call frequency
- [ ] Optimize UI rendering
- [ ] Test with 3 concurrent traders
- [ ] Measure and document performance

**Acceptance Criteria:**
- 3 traders run smoothly
- CPU usage < 30% normal operation
- Memory usage stable
- UI responsive

**Dependencies:** All features complete

---

### Issue #73: Create API documentation
**Epic**: #6  
**Milestone**: Testing & Polish  
**Labels**: `priority: high`, `type: documentation`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Document the REST API comprehensively.

**Tasks:**
- [ ] Generate OpenAPI spec
- [ ] Document all endpoints:
  - [ ] Request formats
  - [ ] Response formats
  - [ ] Error codes
  - [ ] Examples
- [ ] Create Postman collection
- [ ] Add authentication guide
- [ ] Include rate limiting info

**Acceptance Criteria:**
- Complete API documentation
- OpenAPI spec validates
- Postman collection works

**Dependencies:** All API endpoints complete

---

### Issue #74: Create developer documentation
**Epic**: #6  
**Milestone**: Testing & Polish  
**Labels**: `priority: medium`, `type: documentation`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Write documentation for developers.

**Tasks:**
- [ ] Architecture overview
- [ ] Module descriptions
- [ ] Build instructions
- [ ] Testing guide
- [ ] CI/CD pipeline guide
- [ ] Contributing guidelines
- [ ] Code style guide
- [ ] Release process

**Acceptance Criteria:**
- Complete developer docs
- Easy for new developers to onboard
- Build and test instructions clear

**Dependencies:** All features complete

---

### Issue #75: Create release notes
**Epic**: #6  
**Milestone**: Testing & Polish  
**Labels**: `priority: high`, `type: documentation`  
**Assignee**: -  
**Estimate**: 1 day

**Description:**
Write release notes for v1.0.

**Tasks:**
- [ ] List all features
- [ ] Document known limitations
- [ ] Include system requirements
- [ ] Add upgrade notes
- [ ] Thank contributors
- [ ] Create changelog

**Acceptance Criteria:**
- Complete release notes
- All features documented
- Limitations clearly stated

**Dependencies:** All features complete

---

### Issue #76: Fix all known bugs
**Epic**: #6  
**Milestone**: Testing & Polish  
**Labels**: `priority: critical`, `type: bug`  
**Assignee**: -  
**Estimate**: 3 days

**Description:**
Fix all identified bugs before release.

**Tasks:**
- [ ] Review all open bug reports
- [ ] Prioritize critical bugs
- [ ] Fix all critical bugs
- [ ] Fix high-priority bugs
- [ ] Test fixes
- [ ] Update regression tests

**Acceptance Criteria:**
- No critical bugs remaining
- All high-priority bugs fixed
- Tests verify fixes

**Dependencies:** Testing complete

---

### Issue #77: Polish UI/UX
**Epic**: #6  
**Milestone**: Testing & Polish  
**Labels**: `priority: medium`, `type: feature`, `module: ui`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Final UI/UX improvements.

**Tasks:**
- [ ] Review UI consistency
- [ ] Improve loading states
- [ ] Add better error messages
- [ ] Improve accessibility
- [ ] Add keyboard shortcuts
- [ ] Polish animations/transitions
- [ ] Test on different screen resolutions

**Acceptance Criteria:**
- UI polished and consistent
- Accessible (WCAG AA)
- Responsive on different screens

**Dependencies:** UI complete

---

### Issue #78: Create demo trading scenarios
**Epic**: #6  
**Milestone**: Testing & Polish  
**Labels**: `priority: medium`, `type: documentation`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Create demo scenarios for users to try.

**Tasks:**
- [ ] Create sample configurations
- [ ] Document demo scenarios:
  - [ ] Simple trend following
  - [ ] Mean reversion strategy
  - [ ] Multi-trader setup
- [ ] Record demo videos
- [ ] Create quick start tutorial

**Acceptance Criteria:**
- 3 demo scenarios documented
- Videos recorded
- Easy to follow

**Dependencies:** All features complete

---

### Issue #79: Beta testing phase
**Epic**: #6  
**Milestone**: Testing & Polish  
**Labels**: `priority: high`, `type: test`  
**Assignee**: -  
**Estimate**: 5 days

**Description:**
Conduct beta testing with real users.

**Tasks:**
- [ ] Recruit beta testers (5-10 users)
- [ ] Distribute beta build
- [ ] Collect feedback
- [ ] Monitor for issues
- [ ] Fix critical issues
- [ ] Iterate on feedback

**Acceptance Criteria:**
- Beta testing complete
- Major feedback addressed
- No show-stopper bugs

**Dependencies:** All features complete, #62

---

### Issue #80: Prepare v1.0 release
**Epic**: #6  
**Milestone**: Testing & Polish  
**Labels**: `priority: critical`, `type: infrastructure`  
**Assignee**: -  
**Estimate**: 2 days

**Description:**
Final release preparation.

**Tasks:**
- [ ] Update version numbers
- [ ] Create release tag (v1.0.0)
- [ ] Build release artifacts
- [ ] Test release installer
- [ ] Upload to GitHub releases
- [ ] Update README
- [ ] Announce release

**Acceptance Criteria:**
- v1.0.0 released on GitHub
- Installer available for download
- Documentation complete
- Announcement published

**Dependencies:** All issues complete

---

## Issue Creation Instructions

### Using GitHub Web Interface:

1. Go to: https://github.com/patrick-bozek-fmps/FMPS_AutoTraderApplication/issues

2. Create Milestones first:
   - Go to Issues → Milestones
   - Create 6 milestones with due dates

3. Create Labels:
   - Go to Issues → Labels
   - Create all labels listed above

4. Create Epic Issues first (#1-#6):
   - Create with "Epic" label
   - Add to corresponding milestone

5. Create individual issues (#2-#80):
   - Copy description, tasks, acceptance criteria
   - Assign epic number
   - Add milestone
   - Add appropriate labels

6. Create Project Board:
   - Go to Projects → New Project
   - Choose "Board" template
   - Add 5 columns
   - Add all issues to Backlog

### Using GitHub CLI (if installed):

```bash
# Install GitHub CLI first: https://cli.github.com/

# Login
gh auth login

# Navigate to repo
cd C:\PABLO\AI_Projects\FMPS_AutoTraderApplication

# Create milestones
gh api repos/:owner/:repo/milestones -X POST -f title="Foundation & Infrastructure" -f description="Weeks 1-2" -f due_on="2025-11-06T00:00:00Z"
gh api repos/:owner/:repo/milestones -X POST -f title="Exchange Integration" -f description="Weeks 3-5" -f due_on="2025-11-27T00:00:00Z"
# ... create remaining milestones

# Create labels
gh label create "priority: critical" --color "d73a4a"
gh label create "priority: high" --color "ff6b6b"
# ... create remaining labels

# Create issues from template (repeat for each issue)
gh issue create \
  --title "Set up Gradle multi-module project structure" \
  --body "$(cat issue_template_2.md)" \
  --label "priority: critical,type: infrastructure,module: core" \
  --milestone "Foundation & Infrastructure"
```

---

## Summary

**Total Issues**: 79 issues (excluding Epic placeholders)
**Estimated Duration**: 16-18 weeks
**Critical Path**: Foundation → Exchange Integration → AI Trading Engine → UI → Service → Testing

This structure ensures systematic development tracking and clear progress visibility.

