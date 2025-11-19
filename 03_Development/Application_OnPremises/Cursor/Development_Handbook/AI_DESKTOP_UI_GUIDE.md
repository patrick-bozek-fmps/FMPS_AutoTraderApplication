# FMPS AutoTrader Desktop UI – Developer Guide

**Version**: 0.8  
**Last Updated**: November 19, 2025  
**Maintainer**: AI Assistant

---

## 1. Purpose

This guide captures the foundational decisions for the JavaFX/TornadoFX desktop client introduced in **Issue #19 – Desktop UI Foundation**. It explains the MVVM scaffolding, dependency injection strategy, navigation shell, and reusable components that subsequent Epic 5 issues will extend.

### 1.1 Scope & Limitations

**v1.0 Scope:**
- **Desktop-only**: The UI Application is currently available only on Windows desktop (personal computer, notebook)
- **Multi-device support** (tablet, smartphone, smartwatch) is deferred to v1.1+ per product roadmap
- This aligns with **ATP_ProdSpec_14** which requires availability on all computer-like devices, but acknowledges phased rollout

**Platform Support:**
- **Windows 10/11** (64-bit) - ✅ Supported
- **macOS** - ⏳ Planned for v1.1
- **Linux** - ⏳ Planned for v1.1

---

## 2. Module Overview

| Package | Responsibility |
|---------|----------------|
| `com.fmps.autotrader.desktop` | Application bootstrap (`DesktopApp`) and `main` entry point |
| `desktop.di` | Koin module wiring (dispatchers, services, view models, views) |
| `desktop.mvvm` | MVVM base classes (`BaseViewModel`, `BaseView`, `BaseController`, `DispatcherProvider`) |
| `desktop.navigation` | Route registry, back-stack handling, current view tracking |
| `desktop.shell` | Main shell view + view model (sidebar, breadcrumbs, quick stats) |
| `desktop.components` | Reusable UI controls (status badge, metric tile, toolbar button) |
| `desktop.services` | Client interfaces + stub implementations for REST/telemetry |
| `desktop.dashboard` | Dashboard view + view model (Issue #20 implementation) |
| `desktop.traders` | Trader management contract/view/viewmodel (Issue #21) |
| `desktop.monitoring` | Monitoring contract/view/viewmodel (Issue #22) |
| `desktop.config` | Configuration contract/view/viewmodel (Issue #23) |
| `desktop.patterns` | Pattern analytics contract/view/viewmodel (Issue #24) |
| `desktop.i18n` | Localization helper and resource bundle loader |
| `src/main/resources/styles/theme.css` | Global theme (color system, typography, component styles) |
| `src/main/resources/i18n/messages.properties` | Default locale strings |

---

## 3. MVVM Conventions

### 3.1 ViewModels
- Extend `BaseViewModel<State, Event>` with an immutable state data class and sealed event type.
- Use `setState { copy(...) }` for atomic updates and `publishEvent()` for one-time UI actions (toasts, dialogs).
- `launchIO` / `launchComputation` helpers dispatch to the injected `DispatcherProvider`.
- Override `onCleared()` to release resources (called automatically via `BaseView` lifecycle).

### 3.2 Views
- Extend `BaseView<State, Event, VM>` when the view consumes a view model. State and event updates are marshalled onto the JavaFX thread automatically.
- Use the shared components (`StatusBadge`, `MetricTile`, `ToolbarButton`) for consistent look-and-feel.
- Views are organized in feature-specific packages:
  - `desktop.dashboard.DashboardView`
  - `desktop.traders.TraderManagementView`
  - `desktop.monitoring.MonitoringView`
  - `desktop.config.ConfigurationView`
  - `desktop.patterns.PatternAnalyticsView`
- All views are registered with the navigation service in `DesktopApp.registerNavigation`.
- Note: The `desktop.views` package exists but contains only placeholder views (not the actual implementations).

### 3.3 Controllers
- Extend `BaseController` (wrapper around TornadoFX `Controller` + Koin) for non-UI orchestration.

---

## 4. Dependency Injection

The desktop client uses **Koin 3.5**:

```kotlin
val desktopModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single { NavigationService() }
    
    // HTTP Client for REST API calls
    single<HttpClient> { HttpClientFactory.create() }
    
    // Service implementations - Most use real services that connect to backend API
    single<CoreServiceClient> { StubCoreServiceClient() }  // Only CoreServiceClient uses stub
    single<TelemetryClient> { RealTelemetryClient(get()) }  // Real WebSocket client
    single<TraderService> { RealTraderService(get()) }  // Real REST API client
    single<MarketDataService> { RealMarketDataService(get(), get()) }  // Real service with WebSocket + REST fallback
    single<ConfigService> { RealConfigService(get()) }  // Real REST API client
    single<PatternAnalyticsService> { RealPatternAnalyticsService(get()) }  // Real REST API client

    factory { ShellViewModel(get(), get(), get()) }
    factory { DashboardViewModel(get(), get(), get(), get()) }  // 4 parameters: dispatcher, coreService, telemetry, traderService
    factory { DashboardView() }
    factory { TraderManagementViewModel(get(), get(), get()) }
    factory { TraderManagementView() }
    factory { MonitoringViewModel(get(), get()) }
    factory { MonitoringView() }
    factory { ConfigurationViewModel(get(), get()) }
    factory { ConfigurationView() }
    factory { PatternAnalyticsViewModel(get(), get()) }
    factory { PatternAnalyticsView() }
}
```

**Service Implementation Status:**
- **Real Services** (connect to backend API): `RealTelemetryClient`, `RealTraderService`, `RealMarketDataService`, `RealConfigService`, `RealPatternAnalyticsService`
- **Stub Service** (synthetic data): `StubCoreServiceClient` (only service using stub)
- **HttpClient**: Injected via `HttpClientFactory.create()` for REST API communication

`DesktopApp` starts Koin during `init()` and registers navigation routes using the `ViewDescriptor` abstraction. Each view is resolved via Koin, and real services are wired to connect to the Core Service REST API and WebSocket endpoints.

---

## 5. Navigation Shell

- `NavigationService` keeps a registry of routes, manages a stack for back navigation, and exposes the current `UIComponent` via a read-only JavaFX property.
- `ShellView` listens for navigation changes, updates breadcrumbs + quick stats, and hosts the active `UIComponent`.
- Breadcrumbs are currently `["Home", current.title]`; future issues may extend this using multi-level descriptors.

**Registered routes (Issue #19 baseline):**

| Route | Title | View |
|-------|-------|------|
| `dashboard` | Overview | `DashboardView` |
| `traders` | AI Traders | `TraderManagementView` |
| `monitoring` | Monitoring | `MonitoringView` |
| `configuration` | Configuration | `ConfigurationView` |
| `patterns` | Pattern Analytics | `PatternAnalyticsView` |

---

## 6. Reusable Components & Theme

- **StatusBadge** – semantic status chip with tone-aware styles (`status-success`, `status-idle`, `status-error`).
- **MetricTile** – KPI tile with title/value/subtitle bindings.
- **ToolbarButton** – unlabeled/labelled command button with primary/secondary/danger emphases.
- `styles/theme.css` defines root colors, typography, metric/toolbar styling, cards, breadcrumbs, and profit accenting.
- `styles` automatically loaded via `importStylesheet("/styles/theme.css")` in `DesktopApp`.

---

## 7. Localization Hooks

- `Localization.string(key, defaultValue)` reads from `i18n/messages.properties`.
- `Localization.setLocale(locale)` switches runtime locale (future enhancement).
- Strings currently sourced for navigation labels, app title, toasts, and placeholders.

---

## 8. Dashboard Implementation (Issue #20)

**Status**: ✅ **COMPLETE** (November 18, 2025)  
**Commits**: `535e114`, `037034f`, `44afbf0`  
**Review**: ✅ PASS (see `Issue_20_REVIEW.md`)

### 8.1 ViewModel
- `DashboardViewModel` (package `desktop.dashboard`) orchestrates trader summaries and telemetry feeds.
- **Constructor Parameters** (4 total):
  - `dispatcherProvider: DispatcherProvider` - Coroutine dispatcher provider
  - `coreServiceClient: CoreServiceClient` - Core service client (stub)
  - `telemetryClient: TelemetryClient` - Real telemetry client (WebSocket)
  - `traderService: TraderService` - Real trader service (REST API)
- State surface (`DashboardState`) captures:
  - `traderItems`: mapped trader summaries for UI list rendering.
  - `quickStats`: KPI counts (active/stopped traders, open positions, aggregated P&L, critical alert count).
  - `systemStatus`: heartbeat timestamps and connectivity flags for core service + telemetry stream.
  - `notifications`: rolling list (12 max) of `NotificationItem` derived from telemetry channels.
- Collectors:
  - `CoreServiceClient.traderSummaries()` (Flow) → updates traders, quick stats, `systemStatus.lastSummaryUpdate`.
  - `TelemetryClient.samples()` (Flow) → appends notifications, toggles telemetry connectivity, increments alert count.
- Emits user feedback via `DashboardEvent.ShowMessage` for quick actions (start/stop/open trader).
- Lifecycle: `telemetryClient.start()` at init, `telemetryClient.stop()` in `onCleared()`.

### 8.2 View
- `DashboardView` extends `BaseView`, binds to `DashboardViewModel`, and renders:
  - Quick metric tiles (`MetricTile`) for active/stopped traders, aggregate P&L, open positions.
  - Trader overview list (`ListView`) with status badges, profit colouring, and quick action buttons (“Open”, “Start”, “Stop”).
  - System health card displaying core service & telemetry status with relative timestamps.
  - Notifications/activity feed leveraging severity icons (info/warning/critical).
- Adds test-friendly node IDs (`#metric-active-traders`, `#dashboard-trader-list`, etc.) and styles (`status-label-ok`, `notification-*`).

### 8.3 Styling & i18n
- `theme.css` expanded with dashboard-specific classes: quick stats row, status labels, notification typography.
- `messages.properties` extended with dashboard keys (`dashboard.title`, `dashboard.notifications`, etc.).

### 8.4 Tests
- `DashboardViewModelTest` validates state aggregation, telemetry-driven notifications, and event emission.
- `DashboardViewTest` smoke-instantiates the view (with Koin + JavaFX toolkit) and verifies trader list binding.

### 8.5 Usage & Manual Validation Flow
1. **Launch the dashboard**  
   ```powershell
   cd 03_Development\Application_OnPremises
   ./gradlew :desktop-ui:run
   ```
   - The shell auto-navigates to the `dashboard` route on boot.
2. **Simulate trader updates** – `StubCoreServiceClient` mutates running trader P&L every ~4 s; verify quick stats and list rows update without interaction.
3. **Simulate telemetry** – `StubTelemetryClient` emits CPU/memory samples and random `trader.alert` notifications every ~1 s once `DashboardViewModel` calls `telemetryClient.start()`. Confirm:
   - System status tiles flip to “Connected” once samples arrive.
   - Notifications list prepends latest alert; severity colouring matches payload.
4. **Exercise quick actions** – Click `Start`, `Stop`, `View` buttons inside each trader row and observe toast messaging via `DashboardEvent.ShowMessage`.
5. **Headless/TestFX runs** – Before pushing, execute:
   ```powershell
   ./gradlew :desktop-ui:test --no-daemon
   ```
   This uses the headless configuration (Monocle) described in `DEVELOPMENT_WORKFLOW.md` to ensure TestFX smoke tests don’t hang CI.
6. **Full workflow gate** – For Issue #20, run `./gradlew clean test --no-daemon` and `./gradlew clean build --no-daemon` prior to committing, then monitor CI via `Cursor\Artifacts\check-ci-status.ps1`.

---

## 9. Trader Management Workspace (Issue #21)

### 9.1 ViewModel
- `TraderManagementViewModel` owns `TraderManagementState` (trader list, filtered list, selected trader, form state, filters, saving flag).
- Subscribes to `TraderService.traders()` (Flow) to keep UI list + form synchronized; filters applied via `TraderStatusFilter` + search query.
- Exposes actions: `newTrader`, `updateForm`, `saveTrader`, `startTrader`, `stopTrader`, `deleteSelectedTrader`, `updateSearch`, `updateStatusFilter`.
- Emits `TraderManagementEvent.ShowMessage` for toast-style feedback (info/success/error). Validation errors stored inline via `form.errors`.

### 9.2 View
- `TraderManagementView` replaces the placeholder route (`traders`) with:
  - **Left sidebar** – search box, status filter, “New / Delete” actions.
  - **Center table** – constrained `TableView` showing name/exchange/strategy/status/P&L; selection drives detail form.
  - **Right form** – editable fields (name, exchange, strategy, risk, base/quote asset, budget) plus validation banner and Save/Start/Stop buttons.
- Uses shared styling (`content-card`, `section-title`, new `.validation-label`) and `ToolbarButton` component.
- `StubTraderService` feeds sample data and simulates lifecycle changes (profit updates, open position jitter) so UI remains interactive without backend.

### 9.3 Usage Flow
1. Navigate to **AI Traders** tab (route `traders`).
2. Use search or status filter to narrow list; table auto-refreshes as stub emits updates.
3. To create a trader, press **New Trader**, fill out the form, and click **Save**.
4. Select an existing trader and press **Start** / **Stop** to toggle lifecycle, or **Delete** to remove.
5. Validation errors appear inline under the form with red banner + toast; successful actions emit success toasts.
6. Pre-push checks: `./gradlew :desktop-ui:test --no-daemon` then `./gradlew clean test --no-daemon`; monitor CI as usual.

---

## 10. Service Implementations & Data Binding

### 10.1 Real Service Implementations

**RealTelemetryClient:**
- Connects to WebSocket endpoint `/ws/telemetry`
- Subscribes to channels: `trader.status`, `risk.alert`, `system.warning`
- Automatic reconnection (up to 5 attempts)
- Emits `TelemetrySample` events via Flow

**RealTraderService:**
- REST API client for trader CRUD operations
- Endpoints: `/api/v1/traders` (GET, POST, PUT, DELETE)
- Lifecycle operations: `/api/v1/traders/{id}/start`, `/api/v1/traders/{id}/stop`
- Retry logic with exponential backoff

**RealMarketDataService:**
- WebSocket + REST fallback for market data
- WebSocket channels: `market.candlestick`, `position.update`, `trade.executed`
- Automatic REST polling (every 5 seconds) when WebSocket disconnected
- Connection status monitoring

**RealConfigService:**
- REST API client for configuration management
- Endpoints: `/api/v1/config/*`
- Connection testing via `/api/v1/config/test-connection`
- HOCON format import/export
- File-based persistence fallback (`~/.fmps-autotrader/desktop-config.conf`)

**RealPatternAnalyticsService:**
- REST API client for pattern analytics
- Endpoints: `/api/v1/patterns` (GET, POST, GET by ID, DELETE)
- Archive operations: `/api/v1/patterns/{id}/deactivate`
- Retry logic with exponential backoff

### 10.2 Stub Service

**StubCoreServiceClient:**
- Only service using stub implementation
- `traderSummaries()` exposes a `Flow<List<TraderSummary>>`
- Emits synthetic updates every 4 seconds for UI testing
- Used for dashboard quick stats when real trader summaries are not available

### 10.3 Data Binding

- `ShellViewModel` continues to surface quick stats to the shell header
- Dashboard consumes real service flows for richer presentation
- All real services provide automatic reconnection and error handling

---

## 11. Contract Pattern

The Desktop UI uses a **Contract pattern** to define state and event types for each view. This pattern provides clear separation between state management and UI events.

### 11.1 Contract Files

Each view has a corresponding Contract file that defines:
- **State**: Immutable data class containing all view state
- **Event**: Sealed interface for one-time UI actions (toasts, dialogs, navigation)

**Contract Files:**
- `DashboardContract.kt` - Defines `DashboardState`, `DashboardEvent`, `TraderItem`, `NotificationItem`, `QuickStats`, `SystemStatusSummary`
- `TraderManagementContract.kt` - Defines `TraderManagementState`, `TraderManagementEvent`
- `MonitoringContract.kt` - Defines `MonitoringState`, `MonitoringEvent`
- `ConfigurationContract.kt` - Defines `ConfigurationState`, `ConfigurationEvent`
- `PatternAnalyticsContract.kt` - Defines `PatternAnalyticsState`, `PatternAnalyticsEvent`
- `ShellContract.kt` - Defines `ShellState`, `ShellEvent`

### 11.2 Contract Pattern Benefits

- **Type Safety**: State and events are strongly typed
- **Separation of Concerns**: State/event definitions separate from ViewModel logic
- **Testability**: Contracts can be tested independently
- **Documentation**: Contracts serve as clear API documentation for view state

### 11.3 Usage Example

```kotlin
// Contract file: DashboardContract.kt
data class DashboardState(
    val traderItems: List<TraderItem> = emptyList(),
    val quickStats: QuickStats = QuickStats(),
    val systemStatus: SystemStatusSummary = SystemStatusSummary(),
    val notifications: List<NotificationItem> = emptyList()
)

sealed interface DashboardEvent : ViewEvent {
    data class ShowMessage(val message: String) : DashboardEvent
}

// ViewModel uses the contract
class DashboardViewModel(...) : BaseViewModel<DashboardState, DashboardEvent>(...)
```

---

## 12. Trading Monitoring Workspace (Issue #22)

**Status**: ✅ **COMPLETE** (November 18, 2025)  
**Commits**: `6d8b359`, `844946a`, `92c6a15`, `ae3f36a`  
**Review**: ✅ PASS (see `Issue_22_REVIEW.md`)

- `MonitoringViewModel` consumes `RealMarketDataService` flows:
  - `candlesticks(timeframe)` – updates price chart, tracks `latencyMs`/`lastUpdated`.
  - `positions()` – drives active positions table.
  - `tradeHistory()` – surfaces rolling trade history.
  - `connectionStatus()` – toggles connection badge classes.
- `MonitoringView` features:
  - Connection badge + metadata pills for last updated and latency.
  - Manual refresh button (disabled while refresh in-flight).
  - Timeframe picker (1m/5m/15m/1h) triggers re-subscription.
  - Side panel summarizing positions/trades.
- `RealMarketDataService` connects to WebSocket telemetry (`/ws/telemetry` channels: `market.candlestick`, `position.update`, `trade.executed`) with automatic REST polling fallback when WebSocket disconnected.
- Manual QA flow:
  1. Navigate to **Monitoring** route.
  2. Switch timeframe; chart re-renders and latency resets.
  3. Toggle manual refresh — button disables briefly, then badge/latency update.
  4. Observe connection badge cycling between Connected/Reconnecting/Disconnected from stub.

---

## 13. Configuration Workspace (Issue #23)

**Status**: ✅ **COMPLETE** (November 18, 2025)  
**Commits**: `24c84b3`, `ded548c`  
**Review**: ✅ PASS (see `Issue_23_REVIEW.md`)

- `ConfigurationViewModel` leverages `RealConfigService` to stream/persist settings:
  - `configuration()` – snapshot flow consumed on init.
  - `saveExchangeSettings`, `saveGeneralSettings`, `saveTraderDefaults` – persist sections individually.
  - `testExchangeConnection` – async validation via `/api/v1/config/test-connection` endpoint using real exchange connectors, with toast feedback and inline status chip.
  - `exportConfiguration` / `importConfiguration` – HOCON format import/export with validation + preview.
- `ConfigurationView` is tabbed; highlights:
  - Exchange tab: API key/secret/passphrase fields, exchange selector, validation hints, manual connection test button.
  - General tab: numeric inputs for update interval/telemetry polling, logging level dropdown, theme preference toggle.
  - Trader defaults tab: budget/leverage/stop-loss/strategy fields feeding Issue #21 defaults.
  - Import/Export tab: read-only export area, editable import textarea, status label (success/error).
- `RealConfigService` connects to REST API (`/api/v1/config/*` endpoints) with file-based persistence fallback (`~/.fmps-autotrader/desktop-config.conf`).
- Security: secrets remain masked in UI; encryption handled by server-side `ConfigManager` (Issue #6) — see Config Guide v1.1.

---

## 14. Pattern Analytics Workspace (Issue #24)

**Status**: ✅ **COMPLETE** (November 18, 2025)  
**Commits**: `54d6165`, `c8d14bb`, `d465e5b`  
**Review**: ✅ PASS (see `Issue_24_REVIEW.md`)

- `PatternAnalyticsViewModel` consumes `RealPatternAnalyticsService.patternSummaries()` (Flow) and keeps `PatternAnalyticsState` in sync:
  - Filters: search, exchange, timeframe, performance status, minimum success slider.
  - `selectPattern(id)` lazily loads `patternDetail(id)` from `/api/v1/patterns/{id}` endpoint for the side panel.
  - `refresh`, `archiveSelected`, `deleteSelected` delegate to service and emit toasts on completion.
- `PatternAnalyticsView` splits layout into:
  - Left column list with badges for success %, profit factor, status colour.
  - Filter toolbar (search, dropdowns, slider, refresh button).
  - Detail pane containing KPI cards, indicator/criteria summary, AreaChart overlay (success % & profit factor), and management buttons.
- `RealPatternAnalyticsService` connects to REST API:
  - `/api/v1/patterns` (GET) - Fetch pattern summaries
  - `/api/v1/patterns/{id}` (GET) - Fetch pattern details
  - `/api/v1/patterns/{id}/deactivate` (POST) - Archive pattern
  - `/api/v1/patterns/{id}` (DELETE) - Delete pattern
- Archive/delete operations persist to database. Retry logic with exponential backoff implemented.

---

## 15. Requirements Traceability

This section maps the Desktop UI implementation to customer requirements (ATP_ProdSpec).

| Requirement ID | Requirement Description | Implementation | Status |
|----------------|------------------------|-----------------|--------|
| **ATP_ProdSpec_13** | The UI Application shall connect the User with the Core Application | ✅ **IMPLEMENTED** | Section 4 (DI), Section 8 (Dashboard), Section 9 (Trader Management) - Real services (`RealTelemetryClient`, `RealTraderService`, `RealMarketDataService`, `RealConfigService`, `RealPatternAnalyticsService`) connect to Core Service via REST API and WebSocket |
| **ATP_ProdSpec_14** | The UI Application shall be available on all computer-like devices | ⚠️ **PARTIAL** | v1.0: Desktop-only (Windows 10/11). Multi-device support (tablet, smartphone, smartwatch) deferred to v1.1+ per product roadmap. See Section 1.1 (Scope & Limitations) |
| **ATP_ProdSpec_15** | The UI Application shall allow the user to select the exchange and configure their existing account | ✅ **IMPLEMENTED** | Section 13 (Configuration Workspace) - Exchange selection, API key management, connection testing via `/api/v1/config/test-connection` endpoint |
| **ATP_ProdSpec_16** | The UI Application shall be the command center to create, re-configure and observe the AI trader, as well as providing a real-time overview of the status and trading success | ✅ **IMPLEMENTED** | Section 8 (Dashboard), Section 9 (Trader Management), Section 12 (Monitoring) - All required capabilities implemented with real-time updates via WebSocket telemetry |

**Requirements Coverage**: 3/4 fully implemented, 1/4 partially implemented (multi-device support deferred to v1.1+)

---

## 16. Run & Build Commands

| Task | Command |
|------|---------|
| Standard desktop run | `./gradlew :desktop-ui:run` |
| Windows-tuned run | `./gradlew :desktop-ui:runDesktopWindows` or `desktop-ui\run-desktop-windows.bat` |
| macOS-tuned run | `./gradlew :desktop-ui:runDesktopMac` or `desktop-ui/run-desktop-macos.sh` |
| Clean build (module) | `./gradlew :desktop-ui:clean build --no-daemon` |
| Full project build | `./gradlew clean build --no-daemon` |

JavaFX launcher scripts honour `--add-opens=javafx.graphics/javafx.stage=ALL-UNNAMED` so TestFX and runtime injection work without JVM access errors.

---

## 17. Epic 5 Completion Status

**Epic 5: Desktop UI Application** - ✅ **COMPLETE** (November 18, 2025)

All Epic 5 issues have been completed and reviewed:

| Issue | Title | Status | Completion Date | Review Status |
|-------|-------|--------|-----------------|---------------|
| **#19** | Desktop UI Foundation | ✅ **COMPLETE** | November 13, 2025 | ✅ PASS |
| **#20** | Main Dashboard | ✅ **COMPLETE** | November 18, 2025 | ✅ PASS |
| **#21** | AI Trader Management View | ✅ **COMPLETE** | November 18, 2025 | ✅ PASS |
| **#22** | Trading Monitoring View | ✅ **COMPLETE** | November 18, 2025 | ✅ PASS |
| **#23** | Configuration Management View | ✅ **COMPLETE** | November 18, 2025 | ✅ PASS |
| **#24** | Pattern Analytics View | ✅ **COMPLETE** | November 18, 2025 | ✅ PASS |

**Key Achievements:**
- All real services wired via DI (`RealTelemetryClient`, `RealTraderService`, `RealMarketDataService`, `RealConfigService`, `RealPatternAnalyticsService`)
- All views implemented with real backend integration
- WebSocket telemetry integration active
- REST API integration complete
- All review findings addressed

**Next Steps:**
- **Epic 6** – Integration Testing, Performance Testing, Bug Fixing & Polish, Documentation, Release Preparation
- See `Development_Plan_v2.md` Section 10 for Epic 6 details

---

## 18. Change Log

| Version | Date | Description |
|---------|------|-------------|
| 0.8 | 2025-11-19 | Fixed documentation discrepancies (DEF_005): Updated service implementations to reflect real services, added Contract pattern documentation, added requirements traceability, updated Epic 5 completion status, added scope & limitations section, fixed DashboardViewModel constructor, clarified views package structure |
| 0.7 | 2025-11-14 | Added pattern analytics workspace section + updated DI/navigation |
| 0.6 | 2025-11-14 | Added configuration workspace section + module references |
| 0.5 | 2025-11-14 | Added monitoring workspace section, DI/navigation updates |
| 0.4 | 2025-11-14 | Added trader management workspace section, DI/navigation updates |
| 0.3 | 2025-11-14 | Added dashboard usage/manual validation flow + workflow reminders |
| 0.2 | 2025-11-13 | Added dashboard implementation details (Issue #20), DI updates, test coverage |
| 0.1 | 2025-11-13 | Initial publication after Issue #19 foundation completion |

--- 

**Questions / Feedback:** raise via Issue planning documents or ping the development handbook maintainer.  
**Reminder:** continue to document new components/views as they are implemented to keep this guide current.



