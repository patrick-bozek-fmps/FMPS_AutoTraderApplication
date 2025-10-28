# FMPS AutoTrader Application - Development Plan v2

**Version**: 2.6  
**Date**: October 28, 2025  
**Status**: 🚀 In Development - Phase 1 Nearly Complete  
**Based on**: Actual requirements analysis and stakeholder decisions

---

## 🎯 **Current Development Status**

| Phase | Duration | Status | Progress | Key Deliverables |
|-------|----------|--------|----------|------------------|
| **Phase 1: Foundation & Infrastructure** | 2 weeks | 🏗️ **5/6 complete** | Issues #1, #2, #3, #4, #5 ✅ | Gradle, Database, REST API, Logging, Models |
| **Phase 2: Exchange Integration** | 3 weeks | ⏳ Not Started | 0/4 sections | Binance & Bitget connectors |
| **Phase 3: AI Trading Engine** | 3 weeks | ⏳ Not Started | 0/5 sections | 3 AI traders with pattern storage |
| **Phase 4: Desktop UI** | 3 weeks | ⏳ Not Started | 0/5 sections | Complete JavaFX application |
| **Phase 5: Windows Service** | 2 weeks | ⏳ Not Started | 0/3 sections | Windows Service wrapper |
| **Phase 6: Testing & Polish** | 2 weeks | ⏳ Not Started | 0/4 sections | Testing, docs, release |

**Total Project**: 15 weeks estimated → 27 major sections → ~50-80 GitHub issues

**Overall Progress**: **5 GitHub Issues completed (~83% of Phase 1)**

**Last Milestone**: ✅ **Issue #5** - Core Data Models (Oct 28, 2025)

**Next Up**: 🔜 **Issue #6** - Configuration Management (final Phase 1 task!)

### 📋 **Phase 1 Breakdown** (5/6 sections complete)

| Section | Issue | Status | Description |
|---------|-------|--------|-------------|
| 5.1 Project Setup | ✅ **#1** | Complete | Gradle multi-module, CI/CD, structure |
| 5.2 Core Data Models | ✅ **#5** | Complete | Data classes, enums, validation, serialization |
| 5.3 Database Setup | ✅ **#2** | Complete | Exposed ORM, migrations, repositories |
| 5.4 Configuration Mgmt | 📋 **#6** | **TO DO** | Config files, env variables, validation |
| 5.5 Logging Setup | ✅ **#4** | Complete | SLF4J, Logback, MDC, metrics |
| 5.6 REST API Server | ✅ **#3** | Complete | Ktor, 34 endpoints, WebSocket |

---

## 📌 **Document Control**

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | Oct 23, 2025 | Initial plan based on assumptions | AI Assistant |
| 2.0 | Oct 23, 2025 | Updated with actual requirements & decisions | AI Assistant |
| 2.1 | Oct 23, 2025 | Added progress tracking, Issue #1 complete (was #2) | AI Assistant |
| 2.2 | Oct 24, 2025 | Issue #2 complete: Database layer implemented (was #3) | AI Assistant |
| 2.3 | Oct 24, 2025 | Issue #3 complete: REST API Server with Ktor (was #4) | AI Assistant |
| 2.4 | Oct 24, 2025 | Issue numbering realigned with GitHub structure | AI Assistant |
| 2.5 | Oct 28, 2025 | Issue #4 complete: Logging Infrastructure + Progress clarification (was #5) | AI Assistant |
| **2.6** | **Oct 28, 2025** | **Issue renumbering: Phase 1 now starts with Issue #1 (all issues renumbered -1)** | **AI Assistant** |

**Changes from v2.5:**
- ✅ Issue renumbering: All issues renumbered to start from #1 (was starting from #2)
- ✅ File renames: Issue_02→Issue_01, Issue_03→Issue_02, Issue_04→Issue_03, Issue_05→Issue_04
- ✅ Created Issue_05_Core_Data_Models.md with complete specification
- ✅ Updated all cross-references and dependencies throughout documentation
- ✅ All issue documentation files now correctly numbered and aligned

**Changes from v2.4:**
- ✅ Issue #4 completed: Comprehensive logging infrastructure with SLF4J + Logback (was #5)
- ✅ Project reorganization: Development_Handbook/ and Artifacts/ folders created
- ✅ Progress tracking updated: 4/6 sections of Phase 1 complete (~67%)
- ✅ Clarified phase structure: 6 phases with 27 total sections (not 79 "issues")
- ✅ Updated "Next Up" to show remaining Phase 1 work

**Changes from v2.2:**
- ✅ Issue #4 completed: REST API Server with Ktor framework (was Issue #3)
- ✅ 34 REST API endpoints (AI Traders, Trades, Patterns, Configurations, Health)
- ✅ Complete DTO layer with kotlinx.serialization
- ✅ Entity-to-DTO mappers
- ✅ Error handling and validation
- ✅ CORS and request logging configured
- ✅ Project builds successfully

**Changes from v1.0:**
- ✅ Client-server architecture instead of monolithic
- ✅ Maximum 3 AI traders (not unlimited)
- ✅ Demo-only for v1.0 (real money deferred)
- ✅ Simple pattern storage (no ML)
- ✅ Desktop-only for v1.0 (mobile deferred)
- ✅ Updated timeline: 16-18 weeks (simplified scope)

---

## 1. PROJECT OVERVIEW

### 1.1 Purpose

The FMPS AutoTrader Application is an **on-premises desktop application** for Windows that automates cryptocurrency and stock trading using AI-driven decision-making. The system enables users to create and manage multiple AI trading agents that execute trades on demo accounts based on technical analysis and stored trading patterns.

### 1.2 Key Features (v1.0 Scope)

**Core Capabilities:**
- ✅ **Client-Server Architecture**: Core runs as Windows service (24/7)
- ✅ **Desktop UI**: Windows application connecting to Core via REST API
- ✅ **3 AI Trader Instances**: Up to 3 concurrent AI traders
- ✅ **Multi-Exchange Support**: Binance and Bitget connectors
- ✅ **Demo Trading Only**: Virtual money simulation (no real money)
- ✅ **Technical Analysis**: RSI, MACD, SMA, EMA indicators
- ✅ **Pattern Storage**: SQLite database for successful patterns
- ✅ **Real-Time Monitoring**: Live market data and position tracking
- ✅ **Risk Management**: Stop-loss, leverage limits, budget controls

**Explicitly Deferred to v1.1+:**
- ⏳ Real money trading
- ⏳ Mobile/tablet applications
- ⏳ More than 3 AI traders
- ⏳ Machine learning capabilities
- ⏳ Advanced rule-based systems

### 1.3 Target Platform

- **OS**: Windows 10/11 (64-bit)
- **Core Service**: Background Windows service
- **UI Application**: JavaFX desktop client
- **Runtime**: JVM 17+
- **Build System**: Gradle 8.5+ with Kotlin DSL

---

## 2. ARCHITECTURAL DECISIONS

### 2.1 System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    DESKTOP UI APPLICATION                        │
│                      (JavaFX Client)                             │
│                                                                   │
│  ┌────────────┐  ┌──────────────┐  ┌──────────────────┐        │
│  │ Dashboard  │  │  AI Trader   │  │   Configuration  │        │
│  │   View     │  │  Management  │  │      View        │        │
│  └────────────┘  └──────────────┘  └──────────────────┘        │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                    REST API + WebSocket
                           │
┌──────────────────────────┴──────────────────────────────────────┐
│              CORE APPLICATION (Windows Service)                  │
│                    Runs 24/7 in background                       │
│                                                                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    REST API Server                         │  │
│  │              (Ktor Server + WebSocket)                     │  │
│  └───────────────────────────┬───────────────────────────────┘  │
│                              │                                   │
│  ┌───────────────────────────┴───────────────────────────────┐  │
│  │              AI Trader Manager                             │  │
│  │         (Manages up to 3 AI Trader instances)              │  │
│  └───────────────────────────┬───────────────────────────────┘  │
│                              │                                   │
│  ┌──────────────┬────────────┴────────────┬──────────────────┐  │
│  │  AI Trader 1 │      AI Trader 2        │   AI Trader 3    │  │
│  │              │                          │                  │  │
│  │ - Analysis   │     - Analysis           │  - Analysis      │  │
│  │ - Trading    │     - Trading            │  - Trading       │  │
│  │ - Monitoring │     - Monitoring         │  - Monitoring    │  │
│  └──────────────┴─────────────────────────┴──────────────────┘  │
│                              │                                   │
│  ┌───────────────────────────┴───────────────────────────────┐  │
│  │          Exchange Connector Framework                      │  │
│  │   ┌──────────────┐           ┌──────────────┐             │  │
│  │   │   Binance    │           │    Bitget    │             │  │
│  │   │  Connector   │           │  Connector   │             │  │
│  │   │ (Demo API)   │           │ (Demo API)   │             │  │
│  │   └──────────────┘           └──────────────┘             │  │
│  └───────────────────────────┬───────────────────────────────┘  │
│                              │                                   │
│  ┌───────────────────────────┴───────────────────────────────┐  │
│  │              Pattern Storage Database                      │  │
│  │                  (SQLite)                                  │  │
│  │  - Trade history                                           │  │
│  │  - Successful patterns                                     │  │
│  │  - Performance metrics                                     │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
                              │
           ┌──────────────────┴──────────────────┐
           │                                     │
    [Binance Testnet]                    [Bitget Testnet]
```

### 2.2 Communication

**UI ↔ Core Communication:**
- **REST API**: Commands and queries (Ktor Server)
- **WebSocket**: Real-time updates (prices, positions, status)
- **Port**: 8080 (HTTP), 8081 (WebSocket)
- **Format**: JSON
- **Security**: Local connection only (localhost)

### 2.3 Component Responsibilities

#### **Desktop UI Application**
- User interface and visualization
- Configuration management
- Real-time data display
- User input validation
- Chart rendering

#### **Core Service Application**
- AI Trader lifecycle management
- Market data collection
- Trading decision execution
- Pattern storage and retrieval
- Exchange connectivity
- Risk management enforcement

---

## 3. TECHNOLOGY STACK

### 3.1 Core Technologies

#### **Language & Runtime**
- **Kotlin**: 1.9.24
- **JDK**: OpenJDK 17 LTS or later
- **Build Tool**: Gradle 8.5+ with Kotlin DSL

#### **Core Service**
- **Ktor Server 2.3+**: REST API and WebSocket server
- **Kotlin Coroutines**: Asynchronous operations
- **Koin**: Dependency injection

#### **Desktop UI**
- **JavaFX 21+**: Desktop UI framework
- **TornadoFX**: Kotlin wrapper for JavaFX
- **ControlsFX**: Additional UI controls
- **JFreeChart**: Charting library for candlesticks

#### **Networking**
- **Ktor Client 2.3+**: HTTP client for exchange APIs
- **OkHttp 4.12+**: Alternative HTTP client
- **WebSocket**: Real-time bidirectional communication

#### **Database**
- **SQLite 3.43+**: Embedded database
- **Exposed ORM**: Kotlin SQL framework
- **HikariCP**: Connection pooling

#### **Configuration**
- **Typesafe Config (HOCON)**: Configuration files
- **Dotenv Kotlin**: Environment variables

#### **Logging**
- **SLF4J 2.0+**: Logging facade
- **Logback 1.4+**: Logging implementation

#### **Testing**
- **JUnit 5**: Unit testing
- **Mockk**: Mocking for Kotlin
- **Kotest**: Kotlin testing framework
- **TestFX**: JavaFX UI testing

#### **Windows Service**
- **Apache Commons Daemon (procrun)**: Windows service wrapper

### 3.2 Key Dependencies

```kotlin
// build.gradle.kts

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Ktor Server (Core Service)
    implementation("io.ktor:ktor-server-core:2.3.5")
    implementation("io.ktor:ktor-server-cio:2.3.5")
    implementation("io.ktor:ktor-server-websockets:2.3.5")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.5")
    implementation("io.ktor:ktor-serialization-jackson:2.3.5")
    
    // Ktor Client (Exchange APIs)
    implementation("io.ktor:ktor-client-core:2.3.5")
    implementation("io.ktor:ktor-client-cio:2.3.5")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
    
    // JavaFX (UI)
    implementation("org.openjfx:javafx-controls:21")
    implementation("org.openjfx:javafx-fxml:21")
    implementation("no.tornado:tornadofx:1.7.20")
    
    // Database
    implementation("org.jetbrains.exposed:exposed-core:0.44.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.44.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.44.0")
    implementation("org.xerial:sqlite-jdbc:3.43.0.0")
    
    // JSON
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
    
    // Configuration
    implementation("com.typesafe:config:1.4.2")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.11")
    
    // Dependency Injection
    implementation("io.insert-koin:koin-core:3.5.0")
    
    // Testing
    testImplementation("io.kotest:kotest-runner-junit5:5.7.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.testfx:testfx-junit5:4.0.17")
}
```

---

## 4. DEVELOPMENT PHASES

### Overview

| Phase | Duration | Focus | Key Deliverables |
|-------|----------|-------|------------------|
| **Phase 1** | 2 weeks | Foundation | Core service scaffold, data models, database |
| **Phase 2** | 3 weeks | Exchange Integration | Binance & Bitget connectors working |
| **Phase 3** | 3 weeks | AI Trading Engine | 3 AI traders with pattern storage |
| **Phase 4** | 3 weeks | Desktop UI | Complete JavaFX application |
| **Phase 5** | 3 weeks | Integration & Testing | End-to-end testing, bug fixes |
| **Phase 6** | 2 weeks | Polish & Release | Documentation, installer, v1.0 release |

**Total**: 16 weeks (4 months)

---

## 5. PHASE 1: Foundation & Infrastructure (Weeks 1-2)

**Status**: 🏗️ **In Progress** (Started: Oct 23, 2025)

### 5.1 Project Setup ✅ **COMPLETE**

**Issue**: #2 - Set up Gradle multi-module project structure  
**Status**: ✅ **Completed** (Oct 23, 2025)  
**Commit**: `906c2c3` - feat(core): set up Gradle multi-module project structure  
**Documentation**: `Issue_01_Gradle_MultiModule_Setup.md`

**Tasks:**
- [x] ~~Clean up existing codebase structure~~
- [x] ~~Set up proper Gradle multi-module project~~
  - [x] ~~`:core-service` module (Windows service)~~
  - [x] ~~`:desktop-ui` module (JavaFX client)~~
  - [x] ~~`:shared` module (common models and utilities)~~
- [x] ~~Configure all dependencies (Ktor, Exposed, JavaFX, Testing)~~
- [x] ~~Set up Git workflow and branching strategy~~
- [x] ~~Configure CI/CD pipeline (GitHub Actions)~~
- [x] ~~Add Gradle wrapper (Linux + Windows)~~
- [x] ~~Create placeholder Main.kt files~~
- [x] ~~Configure integration test source sets~~

**Deliverable**: ✅ Clean project structure with all modules - **BUILD SUCCESSFUL**

**What was accomplished:**
- ✅ Root `build.gradle.kts` with Kotlin 1.9.21
- ✅ `settings.gradle.kts` declaring 3 modules
- ✅ Shared module with Kotlinx serialization
- ✅ Core-service with Ktor 2.3.7, Exposed ORM, SQLite
- ✅ Desktop-ui with JavaFX 21, TornadoFX
- ✅ Gradle wrapper 8.5 (Windows + Linux)
- ✅ GitHub Actions CI/CD working
- ✅ Helper scripts (`build.ps1`, `test.ps1`)
- ✅ Development environment setup script

### 5.2 Core Data Models

**Tasks:**
- [ ] Define all data classes in `:shared` module
  - [ ] `AITrader` - Configuration and state
  - [ ] `Position` - Open position data
  - [ ] `Order` - Order information
  - [ ] `Candlestick` - Market data
  - [ ] `TradeSignal` - Trading decision
  - [ ] `TradePattern` - Stored pattern
  - [ ] `TradingConfig` - Configuration
- [ ] Create enum classes
  - [ ] `TradeAction`, `PositionSide`, `OrderType`, etc.
- [ ] Implement value objects with validation
- [ ] Create domain exceptions hierarchy
- [ ] Document all models with KDoc

**Deliverable**: Complete data model library

### 5.3 Database Setup ✅ **COMPLETE**

**Issue**: #3 - Configure database layer with Exposed ORM  
**Status**: ✅ **Completed** (Oct 24, 2025)  
**Commit**: `df6e2dd` - feat: Implement database layer with Exposed ORM, migrations, and repositories  
**Documentation**: `Issue_02_Database_Layer.md`

**Tasks:**
- [x] ~~Design SQLite database schema~~
  - [x] ~~`ai_traders` table~~
  - [x] ~~`positions` table (deferred)~~
  - [x] ~~`orders` table (deferred)~~
  - [x] ~~`trade_history` table~~
  - [x] ~~`patterns` table~~
  - [x] ~~`configurations` table (deferred)~~
- [x] ~~Implement database migrations (Flyway)~~
- [x] ~~Create DAO layer with Exposed~~
- [x] ~~Implement repositories~~
  - [x] ~~`AITraderRepository`~~
  - [x] ~~`TradeHistoryRepository`~~
  - [x] ~~`PatternRepository`~~
- [x] ~~Add connection pooling (HikariCP)~~
- [x] ~~Write database tests (24 tests passing)~~

**Deliverable**: ✅ Working database layer with comprehensive tests

### 5.4 Configuration Management

**Tasks:**
- [ ] Design configuration schema (HOCON)
- [ ] Implement `ConfigManager` class
- [ ] Create default configuration files
  - [ ] `core-service.conf`
  - [ ] `exchanges.conf`
  - [ ] `ai-traders.conf`
- [ ] Add environment variable support
- [ ] Implement configuration validation
- [ ] Write configuration tests

**Deliverable**: Configuration system ready

### 5.5 Logging Setup ✅ **COMPLETE**

**Issue**: #5 - Implement logging infrastructure  
**Status**: ✅ **Completed** (Oct 28, 2025)  
**Commit**: `8a3234b` - feat: Issue #5 - Implement Logging Infrastructure  
**Documentation**: `Issue_04_Logging_Infrastructure.md`

**Tasks:**
- [x] ~~Configure SLF4J + Logback~~
- [x] ~~Create logging utility classes (LoggingContext, MetricsLogger)~~
- [x] ~~Define log levels and categories~~
- [x] ~~Configure file rotation and archiving (4 config files)~~
- [x] ~~Add performance metrics collection~~
- [x] ~~Create structured logging format (JSON for production)~~
- [x] ~~Add MDC for request tracing~~
- [x] ~~Create comprehensive LOGGING_GUIDE.md (543 lines)~~

**Deliverable**: ✅ Logging infrastructure operational with 4 environment configs

### 5.6 REST API Server ✅ **COMPLETE**

**Issue**: #4 - Set up REST API server with Ktor  
**Status**: ✅ **Completed** (Oct 24, 2025)  
**Commits**: `ec0a49a`, `1aec787` - feat: Implement REST API server with Ktor + WebSocket support  
**Documentation**: `Issue_03_REST_API_Server.md`

**Tasks:**
- [x] ~~Set up Ktor server with Netty~~
- [x] ~~Create 34 REST API endpoints~~
  - [x] ~~9 AI Trader endpoints (CRUD, status, balance)~~
  - [x] ~~8 Trade endpoints (create, close, statistics)~~
  - [x] ~~10 Pattern endpoints (match, activate, top performers)~~
  - [x] ~~4 Configuration endpoints (placeholders)~~
  - [x] ~~3 Health/Status endpoints~~
- [x] ~~Complete DTO layer with kotlinx.serialization~~
- [x] ~~Entity-to-DTO mappers~~
- [x] ~~Error handling and validation~~
- [x] ~~CORS and request logging configured~~
- [x] ~~WebSocket support (3 channels)~~
- [x] ~~Integration with Main.kt~~
- [x] ~~39 tests passing~~

**Deliverable**: ✅ Complete REST API with 34 endpoints + WebSocket

---

## 6. PHASE 2: Exchange Integration (Weeks 3-5)

### 6.1 Exchange Connector Framework

**Tasks:**
- [ ] Design `IExchangeConnector` interface
  - [ ] Connection management
  - [ ] Market data methods
  - [ ] Order management methods
  - [ ] Position management methods
- [ ] Create `ConnectorFactory`
- [ ] Implement error handling and retry logic
- [ ] Add rate limiting
- [ ] Create connection health monitoring
- [ ] Implement WebSocket streaming support

**Deliverable**: Exchange connector framework

### 6.2 Binance Connector (Demo/Testnet)

**Tasks:**
- [ ] Implement Binance testnet connector
- [ ] REST API endpoints:
  - [ ] Market data (candlesticks, ticker)
  - [ ] Account information
  - [ ] Order management (create, cancel, query)
  - [ ] Demo account balance
- [ ] WebSocket streaming:
  - [ ] Candlestick streams
  - [ ] Order update streams
- [ ] Add proper error handling
- [ ] Implement authentication (demo API keys)
- [ ] Write unit tests
- [ ] Write integration tests

**Deliverable**: Working Binance testnet connector

### 6.3 Bitget Connector (Demo/Testnet)

**Tasks:**
- [ ] Implement Bitget testnet connector
- [ ] REST API endpoints:
  - [ ] Market data
  - [ ] Account information
  - [ ] Order management
  - [ ] Demo positions
- [ ] WebSocket support
- [ ] Error handling
- [ ] Authentication
- [ ] Tests (unit + integration)

**Deliverable**: Working Bitget testnet connector

### 6.4 Technical Indicators Module

**Tasks:**
- [ ] Refactor and clean up indicators
- [ ] Implement with tests:
  - [ ] RSI (Relative Strength Index)
  - [ ] MACD (Moving Average Convergence Divergence)
  - [ ] SMA (Simple Moving Average)
  - [ ] EMA (Exponential Moving Average)
  - [ ] Bollinger Bands
- [ ] Create `IndicatorCalculator` interface
- [ ] Optimize for real-time calculation
- [ ] Add caching where appropriate

**Deliverable**: Complete technical indicators library

---

## 7. PHASE 3: AI Trading Engine (Weeks 6-8)

### 7.1 AI Trader Core

**Tasks:**
- [ ] Implement `AITrader` class
  - [ ] Configuration management
  - [ ] State management
  - [ ] Market data processing
  - [ ] Signal generation
- [ ] Create trading strategies:
  - [ ] Trend following strategy
  - [ ] Mean reversion strategy
  - [ ] Breakout strategy
- [ ] Implement strategy selection logic
- [ ] Add backtesting capabilities
- [ ] Create performance analytics

**Deliverable**: Working AI Trader implementation

### 7.2 AI Trader Manager

**Tasks:**
- [ ] Implement `AITraderManager` class
- [ ] Instance lifecycle management
  - [ ] Create trader (max 3)
  - [ ] Start/stop trader
  - [ ] Update configuration
  - [ ] Delete trader
- [ ] Resource allocation per trader
- [ ] State persistence
- [ ] Recovery on restart
- [ ] Health monitoring

**Deliverable**: AI Trader management system

### 7.3 Position Manager

**Tasks:**
- [ ] Create `PositionManager` class
- [ ] Position tracking and state management
- [ ] P&L calculation (real-time)
- [ ] Position history
- [ ] Position persistence
- [ ] Stop-loss management
- [ ] Position recovery logic

**Deliverable**: Position management system

### 7.4 Risk Manager

**Tasks:**
- [ ] Create `RiskManager` class
- [ ] Budget validation and tracking
- [ ] Leverage limit enforcement
- [ ] Stop-loss logic
- [ ] Exposure monitoring (per trader and total)
- [ ] Emergency stop functionality
- [ ] Risk scoring system

**Deliverable**: Risk management system

### 7.5 Pattern Storage System

**Tasks:**
- [ ] Design pattern storage schema
- [ ] Implement `PatternService` class
  - [ ] Store successful trades as patterns
  - [ ] Query patterns by criteria
  - [ ] Pattern matching algorithm
  - [ ] Pattern relevance scoring
- [ ] Create pattern learning logic
- [ ] Add pattern pruning (remove old/irrelevant)
- [ ] Performance tracking per pattern

**Deliverable**: Pattern storage and retrieval system

---

## 8. PHASE 4: Core Service & API (Weeks 9-11)

### 8.1 REST API Server

**Tasks:**
- [ ] Set up Ktor server
- [ ] Implement REST endpoints:
  - [ ] `/api/traders` - AI Trader CRUD
  - [ ] `/api/traders/{id}/start` - Start trader
  - [ ] `/api/traders/{id}/stop` - Stop trader
  - [ ] `/api/traders/{id}/status` - Get status
  - [ ] `/api/positions` - Get positions
  - [ ] `/api/history` - Get trade history
  - [ ] `/api/patterns` - Get patterns
  - [ ] `/api/config` - Configuration
  - [ ] `/api/health` - Health check
- [ ] Add request validation
- [ ] Implement error responses
- [ ] Add API documentation

**Deliverable**: Complete REST API

### 8.2 WebSocket Server

**Tasks:**
- [ ] Set up WebSocket endpoint `/ws`
- [ ] Implement real-time updates:
  - [ ] Market data (candlesticks, prices)
  - [ ] Position updates
  - [ ] Order updates
  - [ ] AI Trader status changes
  - [ ] System alerts
- [ ] Add connection management
- [ ] Implement message queuing
- [ ] Add reconnection logic

**Deliverable**: WebSocket real-time communication

### 8.3 Windows Service Setup

**Tasks:**
- [ ] Create Windows service wrapper
- [ ] Implement service lifecycle:
  - [ ] Install
  - [ ] Start
  - [ ] Stop
  - [ ] Uninstall
- [ ] Add auto-start on boot
- [ ] Implement graceful shutdown
- [ ] Add service recovery options
- [ ] Create installation scripts

**Deliverable**: Core runs as Windows service

---

## 9. PHASE 5: Desktop UI Application (Weeks 12-14)

### 9.1 UI Foundation

**Tasks:**
- [ ] Set up JavaFX project structure
- [ ] Implement MVVM architecture
- [ ] Create base view models
- [ ] Set up navigation system
- [ ] Create reusable UI components
- [ ] Design application theme/styling
- [ ] Implement data binding

**Deliverable**: UI framework ready

### 9.2 Main Dashboard

**Tasks:**
- [ ] Create main dashboard layout
- [ ] Add AI Trader overview panel
  - [ ] List of traders with status
  - [ ] Quick actions (start/stop)
  - [ ] Performance summary
- [ ] Add system status panel
  - [ ] Core service connection
  - [ ] Exchange connections
  - [ ] System resources
- [ ] Add notifications panel
- [ ] Add quick stats panel

**Deliverable**: Main dashboard view

### 9.3 AI Trader Management View

**Tasks:**
- [ ] Create trader list view
- [ ] Implement "Create New Trader" form
  - [ ] Name and description
  - [ ] Exchange selection
  - [ ] Trading pair selection
  - [ ] Budget configuration
  - [ ] Risk profile selection
  - [ ] Strategy selection
- [ ] Add trader detail view
  - [ ] Configuration display/edit
  - [ ] Performance metrics
  - [ ] Position history
  - [ ] Controls (start/stop/delete)
- [ ] Add validation and error handling

**Deliverable**: AI Trader management UI

### 9.4 Trading Monitoring View

**Tasks:**
- [ ] Create real-time price charts
  - [ ] Candlestick chart
  - [ ] Indicator overlays
- [ ] Add active positions display
  - [ ] Position details
  - [ ] Current P&L
  - [ ] Entry/exit prices
- [ ] Add order book display (optional)
- [ ] Create trade history view
- [ ] Add real-time updates via WebSocket

**Deliverable**: Trading monitoring UI

### 9.5 Configuration View

**Tasks:**
- [ ] Create configuration UI
- [ ] Add exchange connection settings
  - [ ] API key management (encrypted)
  - [ ] Connection test
- [ ] Add general settings
  - [ ] Update intervals
  - [ ] UI preferences
  - [ ] Logging level
- [ ] Add AI Trader defaults
- [ ] Implement save/load configuration
- [ ] Add export/import functionality

**Deliverable**: Configuration UI

### 9.6 Pattern Analytics View

**Tasks:**
- [ ] Create pattern list view
- [ ] Add pattern details display
  - [ ] Pattern conditions
  - [ ] Success rate
  - [ ] Historical performance
- [ ] Add pattern search/filter
- [ ] Create pattern visualization
- [ ] Add pattern management (delete old patterns)

**Deliverable**: Pattern analytics UI

---

## 10. PHASE 6: Integration & Testing (Weeks 15-16)

### 10.1 Integration Testing

**Tasks:**
- [ ] End-to-end workflow testing
  - [ ] Create AI Trader → Start → Execute trades → Monitor → Stop
- [ ] Test Core Service ↔ UI communication
- [ ] Test Core Service ↔ Exchange communication
- [ ] Test multi-trader scenarios (3 concurrent)
- [ ] Test error scenarios and recovery
- [ ] Test state persistence and recovery
- [ ] Load testing (continuous operation)

**Deliverable**: Comprehensive integration tests

### 10.2 Performance Testing

**Tasks:**
- [ ] Test with 3 concurrent AI traders
- [ ] Measure latency (decision to execution)
- [ ] Test WebSocket performance (message throughput)
- [ ] Memory leak detection
- [ ] CPU usage optimization
- [ ] Database query optimization
- [ ] Network efficiency testing

**Deliverable**: Performance benchmarks and optimizations

### 10.3 Bug Fixing & Polish

**Tasks:**
- [ ] Fix all critical bugs
- [ ] Fix high-priority bugs
- [ ] Address medium-priority bugs
- [ ] UI polish and UX improvements
- [ ] Error message improvements
- [ ] Add loading indicators
- [ ] Improve responsiveness

**Deliverable**: Stable, polished application

### 10.4 Documentation

**Tasks:**
- [ ] User manual
  - [ ] Installation guide
  - [ ] Quick start guide
  - [ ] Feature documentation
  - [ ] Troubleshooting
- [ ] Developer documentation
  - [ ] Architecture documentation
  - [ ] API documentation
  - [ ] Code documentation (KDoc)
  - [ ] Build instructions
- [ ] Deployment guide

**Deliverable**: Complete documentation

### 10.5 Release Preparation

**Tasks:**
- [ ] Create Windows installer (MSI)
- [ ] Create portable version (ZIP)
- [ ] Set up auto-update mechanism
- [ ] Version management
- [ ] Create GitHub release
- [ ] Prepare release notes
- [ ] Beta testing with selected users

**Deliverable**: Release-ready v1.0 package

---

## 11. MODULE STRUCTURE

```
FMPS_AutoTraderApplication/
├── 03_Development/
│   └── Application_OnPremises/
│       ├── build.gradle.kts              # Root build file
│       ├── settings.gradle.kts            # Module configuration
│       │
│       ├── shared/                        # Shared module
│       │   └── src/main/kotlin/
│       │       └── com/fmps/autotrader/shared/
│       │           ├── models/            # Data models
│       │           │   ├── AITrader.kt
│       │           │   ├── Position.kt
│       │           │   ├── Order.kt
│       │           │   ├── Candlestick.kt
│       │           │   ├── TradeSignal.kt
│       │           │   └── TradePattern.kt
│       │           ├── enums/             # Enumerations
│       │           │   ├── TradeAction.kt
│       │           │   ├── PositionSide.kt
│       │           │   └── OrderType.kt
│       │           └── utils/             # Shared utilities
│       │               ├── DateTimeUtils.kt
│       │               └── CryptoUtils.kt
│       │
│       ├── core-service/                  # Core Service module
│       │   └── src/main/kotlin/
│       │       └── com/fmps/autotrader/core/
│       │           ├── CoreApplication.kt # Main entry point
│       │           ├── api/               # REST API
│       │           │   ├── routes/
│       │           │   │   ├── TraderRoutes.kt
│       │           │   │   ├── PositionRoutes.kt
│       │           │   │   └── ConfigRoutes.kt
│       │           │   └── websocket/
│       │           │       └── WebSocketHandler.kt
│       │           ├── traders/           # AI Trading
│       │           │   ├── AITrader.kt
│       │           │   ├── AITraderManager.kt
│       │           │   ├── PositionManager.kt
│       │           │   ├── RiskManager.kt
│       │           │   └── strategies/
│       │           │       ├── TrendFollowingStrategy.kt
│       │           │       ├── MeanReversionStrategy.kt
│       │           │       └── BreakoutStrategy.kt
│       │           ├── connectors/        # Exchange connectors
│       │           │   ├── IExchangeConnector.kt
│       │           │   ├── ConnectorFactory.kt
│       │           │   ├── BinanceConnector.kt
│       │           │   └── BitgetConnector.kt
│       │           ├── indicators/        # Technical indicators
│       │           │   ├── TechnicalIndicators.kt
│       │           │   ├── RSI.kt
│       │           │   ├── MACD.kt
│       │           │   └── SMA.kt
│       │           ├── patterns/          # Pattern storage
│       │           │   ├── PatternService.kt
│       │           │   └── PatternMatcher.kt
│       │           ├── database/          # Database layer
│       │           │   ├── DatabaseManager.kt
│       │           │   └── repositories/
│       │           │       ├── AITraderRepository.kt
│       │           │       ├── TradeHistoryRepository.kt
│       │           │       └── PatternRepository.kt
│       │           └── config/            # Configuration
│       │               └── ConfigManager.kt
│       │
│       └── desktop-ui/                    # Desktop UI module
│           └── src/main/kotlin/
│               └── com/fmps/autotrader/ui/
│                   ├── DesktopApplication.kt # UI entry point
│                   ├── views/              # Views
│                   │   ├── MainView.kt
│                   │   ├── DashboardView.kt
│                   │   ├── TraderManagementView.kt
│                   │   ├── TradingMonitorView.kt
│                   │   ├── ConfigurationView.kt
│                   │   └── PatternAnalyticsView.kt
│                   ├── viewmodels/         # View Models
│                   │   ├── DashboardViewModel.kt
│                   │   ├── TraderViewModel.kt
│                   │   └── MonitoringViewModel.kt
│                   ├── components/         # Reusable components
│                   │   ├── ChartComponent.kt
│                   │   ├── TraderCard.kt
│                   │   └── StatusIndicator.kt
│                   └── services/           # UI services
│                       ├── ApiClient.kt
│                       └── WebSocketClient.kt
```

---

## 12. CODING STANDARDS

### 12.1 Kotlin Style Guide

- Follow [Kotlin Official Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Maximum line length: 120 characters
- Use meaningful names (no single letters except loop indices)
- Prefer immutability (`val` over `var`)
- Use data classes for models
- Leverage Kotlin stdlib functions

### 12.2 Architecture Principles

- **SOLID principles**: Single responsibility, Open/closed, Liskov substitution, Interface segregation, Dependency inversion
- **Clean Architecture**: Clear separation of concerns
- **DRY**: Don't Repeat Yourself
- **KISS**: Keep It Simple

### 12.3 Error Handling

- Use `Result<T>` for expected errors
- Throw exceptions only for unexpected errors
- Create custom exception hierarchy
- Always log errors with context
- Fail fast on critical errors
- Graceful degradation for non-critical errors

### 12.4 Asynchronous Programming

- Use Kotlin Coroutines for async operations
- Prefer structured concurrency
- Use appropriate dispatchers (IO, Default, Main)
- Handle cancellation properly
- Avoid blocking calls in coroutines

### 12.5 Testing Standards

- Minimum 80% code coverage
- Test naming: `methodName_scenario_expectedBehavior`
- AAA pattern: Arrange, Act, Assert
- Mock external dependencies
- Test edge cases and error scenarios

### 12.6 Git Workflow

- **Main branch**: Production-ready code only
- **Develop branch**: Integration branch
- **Feature branches**: `feature/TICKET-description`
- **Bugfix branches**: `bugfix/TICKET-description`
- Commit message format: `type(scope): description`
  - Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
- Pull request required for main/develop
- Code review required before merge

---

## 13. RISK MANAGEMENT

### 13.1 Technical Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Exchange API changes | High | Medium | Version locking, adapter pattern, monitoring |
| API rate limiting | Medium | High | Rate limiter, caching, batch requests |
| Network connectivity | High | Medium | Retry logic, offline handling, monitoring |
| Service crashes | High | Low | Auto-restart, state persistence, monitoring |
| Database corruption | High | Low | Regular backups, transaction management |
| Performance issues | Medium | Medium | Profiling, optimization, load testing |
| Windows service issues | Medium | Low | Extensive testing, fallback mechanisms |

### 13.2 Project Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Scope creep | Medium | Medium | Clear requirements, change control |
| Timeline delays | Medium | Medium | Buffer time, agile approach, MVP focus |
| Knowledge gaps | Low | Low | Research time, documentation |
| Testing gaps | High | Medium | Test automation, CI/CD, code review |

---

## 14. QUALITY ASSURANCE

### 14.1 Code Quality Metrics

- **Code Coverage**: Minimum 80%
- **Code Duplication**: Maximum 5%
- **Cyclomatic Complexity**: Maximum 10 per method
- **Method Length**: Maximum 50 lines
- **Class Size**: Maximum 500 lines

### 14.2 Testing Strategy

**Unit Tests:**
- All business logic classes
- Technical indicators
- Data models with validation
- Utility functions

**Integration Tests:**
- Exchange connector integration (testnet)
- Database operations
- API endpoints
- WebSocket communication

**End-to-End Tests:**
- Complete trading workflows
- Multi-trader scenarios
- Error recovery scenarios

**Performance Tests:**
- Load testing with 3 concurrent traders
- WebSocket throughput
- Database query performance

### 14.3 Test Suite Structure

**Directory Organization:**
```
Application_OnPremises/
├── shared/
│   └── src/test/kotlin/
│       └── com/fmps/autotrader/shared/
│           ├── models/           # Model validation tests
│           └── utils/            # Utility function tests
│
├── core-service/
│   └── src/test/kotlin/
│       └── com/fmps/autotrader/core/
│           ├── api/              # API endpoint tests
│           │   ├── TraderRoutesTest.kt
│           │   ├── PositionRoutesTest.kt
│           │   └── WebSocketTest.kt
│           ├── traders/          # Trading logic tests
│           │   ├── AITraderTest.kt
│           │   ├── AITraderManagerTest.kt
│           │   ├── PositionManagerTest.kt
│           │   ├── RiskManagerTest.kt
│           │   └── strategies/
│           │       ├── TrendFollowingStrategyTest.kt
│           │       ├── MeanReversionStrategyTest.kt
│           │       └── BreakoutStrategyTest.kt
│           ├── connectors/       # Exchange connector tests
│           │   ├── BinanceConnectorTest.kt
│           │   ├── BitgetConnectorTest.kt
│           │   └── integration/  # Integration tests
│           │       ├── BinanceIntegrationTest.kt
│           │       └── BitgetIntegrationTest.kt
│           ├── indicators/       # Technical indicator tests
│           │   ├── RSITest.kt
│           │   ├── MACDTest.kt
│           │   └── SMATest.kt
│           ├── patterns/         # Pattern storage tests
│           │   ├── PatternServiceTest.kt
│           │   └── PatternMatcherTest.kt
│           └── database/         # Database tests
│               └── repositories/
│                   ├── AITraderRepositoryTest.kt
│                   ├── TradeHistoryRepositoryTest.kt
│                   └── PatternRepositoryTest.kt
│
└── desktop-ui/
    └── src/test/kotlin/
        └── com/fmps/autotrader/ui/
            ├── viewmodels/       # ViewModel tests
            │   ├── DashboardViewModelTest.kt
            │   └── TraderViewModelTest.kt
            ├── services/         # Service tests
            │   ├── ApiClientTest.kt
            │   └── WebSocketClientTest.kt
            └── e2e/              # End-to-end tests
                ├── TraderCreationE2ETest.kt
                └── TradingWorkflowE2ETest.kt
```

### 14.4 Test Coverage Requirements

**Per Module:**
- **shared**: 90%+ (models are critical)
- **core-service**: 85%+ (business logic critical)
- **desktop-ui**: 75%+ (UI harder to test)
- **Overall**: 80%+ minimum

**Critical Areas (90%+ required):**
- Risk management logic
- Position management
- Trade execution
- Pattern storage
- API endpoints

### 14.5 CI/CD Pipeline

**GitHub Actions Workflow - Automatic on Every Commit:**

```yaml
# .github/workflows/ci.yml
name: CI Pipeline

on:
  push:
    branches: [ main, develop, feature/* ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      working-directory: 03_Development/Application_OnPremises
    
    - name: Build all modules
      run: ./gradlew build -x test
      working-directory: 03_Development/Application_OnPremises
    
    - name: Run unit tests
      run: ./gradlew test
      working-directory: 03_Development/Application_OnPremises
    
    - name: Run integration tests
      run: ./gradlew integrationTest
      working-directory: 03_Development/Application_OnPremises
    
    - name: Generate test coverage report
      run: ./gradlew jacocoTestReport
      working-directory: 03_Development/Application_OnPremises
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        files: ./03_Development/Application_OnPremises/build/reports/jacoco/test/jacocoTestReport.xml
        fail_ci_if_error: true
    
    - name: Enforce minimum coverage
      run: ./gradlew jacocoTestCoverageVerification
      working-directory: 03_Development/Application_OnPremises
    
    - name: Code style check
      run: ./gradlew ktlintCheck
      working-directory: 03_Development/Application_OnPremises
    
    - name: Static code analysis
      run: ./gradlew detekt
      working-directory: 03_Development/Application_OnPremises
    
    - name: Publish test results
      uses: EnricoMi/publish-unit-test-result-action@v2
      if: always()
      with:
        files: |
          03_Development/Application_OnPremises/**/build/test-results/**/*.xml
    
    - name: Comment PR with test results
      uses: dorny/test-reporter@v1
      if: github.event_name == 'pull_request'
      with:
        name: Test Results
        path: '03_Development/Application_OnPremises/**/build/test-results/**/*.xml'
        reporter: java-junit

  code-quality:
    runs-on: ubuntu-latest
    needs: build-and-test
    
    steps:
    - uses: actions/checkout@v3
    
    - name: SonarCloud Scan
      uses: SonarSource/sonarcloud-github-action@master
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

**On Pull Request:**
- ✅ Build all modules
- ✅ Run all unit tests
- ✅ Run integration tests
- ✅ Generate coverage report
- ✅ Enforce 80% minimum coverage
- ✅ Code style check (ktlint)
- ✅ Static analysis (detekt)
- ✅ Comment results on PR

**On Merge to Develop:**
- ✅ All PR checks
- ✅ Additional integration tests
- ✅ Upload coverage to Codecov
- ✅ SonarCloud analysis

**On Merge to Main:**
- ✅ All develop checks
- ✅ Create release candidate
- ✅ Generate changelog
- ✅ Tag release

---

## 15. DEPLOYMENT

### 15.1 Installation Package

**Contents:**
```
FMPSAutoTrader/
├── bin/
│   ├── core-service.exe          # Core service executable
│   ├── desktop-ui.exe             # Desktop UI executable
│   └── service-manager.exe        # Service installer
├── lib/
│   └── *.jar                      # All dependencies
├── config/
│   ├── core-service.conf          # Default Core configuration
│   ├── exchanges.conf             # Exchange settings
│   └── logging.xml                # Logback configuration
├── data/                          # Created on first run
│   └── autotrader.db              # SQLite database
├── logs/                          # Created on first run
│   └── application.log
├── jre/                           # Bundled JRE 17
├── install.bat                    # Installation script
├── uninstall.bat                  # Uninstallation script
└── README.txt
```

### 15.2 Installation Process

1. Run `install.bat` as Administrator
2. Script installs Core Service as Windows service
3. Script creates desktop shortcut for UI
4. Service starts automatically
5. User launches desktop UI
6. First-run wizard guides configuration

### 15.3 System Requirements

**Minimum:**
- Windows 10 64-bit or Windows 11
- 4GB RAM
- 500MB disk space
- Internet connection

**Recommended:**
- Windows 11 64-bit
- 8GB RAM
- 1GB disk space
- Stable internet connection (10+ Mbps)

---

## 16. SUCCESS CRITERIA

### 16.1 Functional Criteria

- ✅ Core Service runs as Windows service (24/7)
- ✅ Desktop UI connects to Core Service successfully
- ✅ User can create up to 3 AI traders
- ✅ AI traders connect to Binance and Bitget testnets
- ✅ AI traders execute demo trades based on technical analysis
- ✅ Positions are tracked accurately with real-time P&L
- ✅ Patterns are stored and retrieved correctly
- ✅ Risk management prevents invalid trades
- ✅ Real-time monitoring dashboard works correctly
- ✅ System recovers gracefully from errors

### 16.2 Non-Functional Criteria

- ✅ 99% uptime during operation
- ✅ < 100ms latency for trading decisions
- ✅ < 500ms UI response time
- ✅ Handles 3 concurrent traders without performance degradation
- ✅ < 500MB memory footprint per AI trader
- ✅ 80%+ code coverage
- ✅ Zero critical security vulnerabilities
- ✅ Core Service startup time < 5 seconds
- ✅ Desktop UI startup time < 10 seconds

---

## 17. TIMELINE SUMMARY

| Phase | Duration | Start | End | Key Deliverables |
|-------|----------|-------|-----|------------------|
| **Phase 1** | 2 weeks | Week 1 | Week 2 | Foundation: data models, database, config |
| **Phase 2** | 3 weeks | Week 3 | Week 5 | Exchange connectors, indicators |
| **Phase 3** | 3 weeks | Week 6 | Week 8 | AI Trading engine, patterns |
| **Phase 4** | 3 weeks | Week 9 | Week 11 | Core Service + API |
| **Phase 5** | 3 weeks | Week 12 | Week 14 | Desktop UI |
| **Phase 6** | 2 weeks | Week 15 | Week 16 | Testing, polish, release |

**Total Duration**: 16 weeks (4 months)

**Target Release**: Week 16

---

## 18. FUTURE ROADMAP (v1.1+)

### Planned for v1.1
- ✅ Real money trading mode (with extensive safety)
- ✅ Increase AI trader limit to 5-10
- ✅ Advanced rule-based pattern learning
- ✅ Additional exchanges (Kraken, Coinbase)
- ✅ More trading strategies

### Planned for v1.2
- ✅ Mobile companion app (monitoring only)
- ✅ Telegram/Discord notifications
- ✅ Advanced analytics and reporting
- ✅ Portfolio management features

### Planned for v2.0
- ✅ Machine learning capabilities
- ✅ Cloud sync option
- ✅ Multi-user support
- ✅ Professional trading features

---

## 19. TEAM & RESOURCES

### 19.1 Required Roles

- **Lead Developer/Architect** (1): Overall design and Core Service
- **Backend Developer** (1): Exchange connectors and AI engine
- **Frontend Developer** (1): Desktop UI development
- **QA Engineer** (1): Testing and quality assurance
- **Technical Writer** (0.5): Documentation

**Team Size**: 4.5 FTE

### 19.2 Development Tools

- **IDE**: IntelliJ IDEA Ultimate
- **JDK**: OpenJDK 17 LTS
- **Build**: Gradle 8.5+
- **VCS**: Git + GitHub
- **CI/CD**: GitHub Actions
- **Testing**: JUnit 5, Mockk, TestFX
- **Code Quality**: Detekt, ktlint
- **Database**: DBeaver, SQLite Browser

### 19.3 External Requirements

- Binance testnet account and API keys
- Bitget testnet account and API keys
- Windows development machine
- Windows test machines (Win 10 and Win 11)

---

## 20. APPENDIX

### 20.1 Key Changes from v1.0 Plan

1. **Architecture**: Monolithic → Client-Server
2. **AI Traders**: Unlimited → Maximum 3
3. **Trading Mode**: Both demo & real → Demo only
4. **Knowledge DB**: Vague → Simple pattern storage
5. **Devices**: Multi-device → Desktop only
6. **Timeline**: 14 weeks → 16 weeks (more realistic)

### 20.2 Glossary

- **AI Trader**: Automated trading agent instance
- **Pattern**: Stored successful trading scenario
- **Demo Trading**: Trading with virtual money on testnet
- **Technical Indicator**: Mathematical calculation on price data
- **Position**: Open trade (long or short)
- **Stop-Loss**: Automatic exit at loss threshold

### 20.3 References

- Customer Specification: `02_ReqMgn/FMPS_AutoTraderApplication_Customer_Specification.md`
- Requirements Analysis: `Cursor/Requirements_Analysis_Summary.md`
- Decisions Document: `Cursor/Requirements_Answers_Summary.md`
- Binance API: https://binance-docs.github.io/apidocs/
- Bitget API: https://bitgetlimited.github.io/apidoc/
- Kotlin Docs: https://kotlinlang.org/docs/

---

## 21. DOCUMENT APPROVAL

**Prepared by**: AI Development Assistant  
**Date**: October 23, 2025  
**Version**: 2.0  
**Status**: Ready for Review

**Approval Required From:**
- [ ] Project Owner
- [ ] Lead Developer
- [ ] Stakeholders

**Once Approved**: Begin Phase 1 implementation

---

**End of Development Plan v2**



