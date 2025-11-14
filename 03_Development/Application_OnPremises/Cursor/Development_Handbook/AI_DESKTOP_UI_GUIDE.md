# FMPS AutoTrader Desktop UI – Developer Guide

**Version**: 0.6  
**Last Updated**: November 14, 2025  
**Maintainer**: AI Assistant

---

## 1. Purpose

This guide captures the foundational decisions for the JavaFX/TornadoFX desktop client introduced in **Issue #19 – Desktop UI Foundation**. It explains the MVVM scaffolding, dependency injection strategy, navigation shell, and reusable components that subsequent Epic 5 issues will extend.

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
| `desktop.views` | Placeholder views for upcoming screens (monitoring/config/patterns) |
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
- All views live under `desktop.views` and are registered with the navigation service in `DesktopApp.registerNavigation`.

### 3.3 Controllers
- Extend `BaseController` (wrapper around TornadoFX `Controller` + Koin) for non-UI orchestration.

---

## 4. Dependency Injection

The desktop client uses **Koin 3.5**:

```kotlin
val desktopModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single { NavigationService() }
    single<CoreServiceClient> { StubCoreServiceClient() }
    single<TelemetryClient> { StubTelemetryClient() }
    single<TraderService> { StubTraderService() }

    factory { ShellViewModel(get(), get(), get()) }
    factory { DashboardViewModel(get(), get(), get()) }
    factory { DashboardView() }
    factory { TraderManagementViewModel(get(), get()) }
    factory { TraderManagementView() }
    factory { MonitoringPlaceholderView() }
    factory { ConfigurationPlaceholderView() }
    factory { PatternAnalyticsPlaceholderView() }
}
```

`DesktopApp` starts Koin during `init()` and registers navigation routes using the `ViewDescriptor` abstraction. Each view is resolved via Koin so future issues can swap stubs for concrete implementations without touching the shell.

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
| `monitoring` | Monitoring | `MonitoringPlaceholderView` |
| `configuration` | Configuration | `ConfigurationPlaceholderView` |
| `patterns` | Pattern Analytics | `PatternAnalyticsPlaceholderView` |

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

### 8.1 ViewModel
- `DashboardViewModel` (package `desktop.dashboard`) orchestrates trader summaries and telemetry feeds.
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

## 10. Service Stubs & Data Binding

- `CoreServiceClient.traderSummaries()` exposes a `Flow<List<TraderSummary>>`. The stub emits synthetic updates every 4 seconds.
- `TelemetryClient.samples()` now randomises channels (`trader.status`, `system.warning`, `risk.alert`) to exercise notification severities.
- `ShellViewModel` continues to surface quick stats to the shell header; dashboard consumes the same flows for richer presentation.

---

## 10. Trading Monitoring Workspace (Issue #22)

- `MonitoringViewModel` consumes `MarketDataService` flows:
  - `candlesticks(timeframe)` – updates price chart, tracks `latencyMs`/`lastUpdated`.
  - `positions()` – drives active positions table.
  - `tradeHistory()` – surfaces rolling trade history.
  - `connectionStatus()` – toggles connection badge classes.
- `MonitoringView` features:
  - Connection badge + metadata pills for last updated and latency.
  - Manual refresh button (disabled while refresh in-flight).
  - Timeframe picker (1m/5m/15m/1h) triggers re-subscription.
  - Side panel summarizing positions/trades.
- `StubMarketDataService` simulates candlesticks, positions, trades, and fluctuating connection states.
- Manual QA flow:
  1. Navigate to **Monitoring** route.
  2. Switch timeframe; chart re-renders and latency resets.
  3. Toggle manual refresh — button disables briefly, then badge/latency update.
  4. Observe connection badge cycling between Connected/Reconnecting/Disconnected from stub.

---

## 11. Configuration Workspace (Issue #23)

- `ConfigurationViewModel` leverages `ConfigService` to stream/persist settings:
  - `configuration()` – snapshot flow consumed on init.
  - `saveExchangeSettings`, `saveGeneralSettings`, `saveTraderDefaults` – persist sections individually.
  - `testExchangeConnection` – async validation with toast feedback and inline status chip.
  - `exportConfiguration` / `importConfiguration` – text-based import/export with validation + preview.
- `ConfigurationView` is tabbed; highlights:
  - Exchange tab: API key/secret/passphrase fields, exchange selector, validation hints, manual connection test button.
  - General tab: numeric inputs for update interval/telemetry polling, logging level dropdown, theme preference toggle.
  - Trader defaults tab: budget/leverage/stop-loss/strategy fields feeding Issue #21 defaults.
  - Import/Export tab: read-only export area, editable import textarea, status label (success/error).
- `StubConfigService` hosts in-memory snapshot + fake connection test; replace with backend adapter prior to GA.
- Security: secrets remain masked in UI; encryption handled by server-side `ConfigManager` (Issue #6) — see Config Guide v1.1.

---

## 12. Run & Build Commands

| Task | Command |
|------|---------|
| Standard desktop run | `./gradlew :desktop-ui:run` |
| Windows-tuned run | `./gradlew :desktop-ui:runDesktopWindows` or `desktop-ui\run-desktop-windows.bat` |
| macOS-tuned run | `./gradlew :desktop-ui:runDesktopMac` or `desktop-ui/run-desktop-macos.sh` |
| Clean build (module) | `./gradlew :desktop-ui:clean build --no-daemon` |
| Full project build | `./gradlew clean build --no-daemon` |

JavaFX launcher scripts honour `--add-opens=javafx.graphics/javafx.stage=ALL-UNNAMED` so TestFX and runtime injection work without JVM access errors.

---

## 13. Next Steps (Epics 5 & 6)

1. **Issue #22** – Trading Monitoring View (charts/positions, WebSocket bindings).
2. **Issue #23** – Configuration Management View (API credentials, defaults, import/export).
3. **Issue #24** – Pattern Analytics View (pattern list/detail/visualisations).
4. **Epic 6 Prep** – Documentation polish, installer workflow, secure secret storage.

All issues should reuse the base MVVM scaffolding and follow the `Development_Workflow.md` gating steps.

---

## 14. Change Log

| Version | Date | Description |
|---------|------|-------------|
| 0.6 | 2025-11-14 | Added configuration workspace section + module references |
| 0.5 | 2025-11-14 | Added monitoring workspace section, DI/navigation updates |
| 0.4 | 2025-11-14 | Added trader management workspace section, DI/navigation updates |
| 0.3 | 2025-11-14 | Added dashboard usage/manual validation flow + workflow reminders |
| 0.2 | 2025-11-13 | Added dashboard implementation details (Issue #20), DI updates, test coverage |
| 0.1 | 2025-11-13 | Initial publication after Issue #19 foundation completion |

--- 

**Questions / Feedback:** raise via Issue planning documents or ping the development handbook maintainer.  
**Reminder:** continue to document new components/views as they are implemented to keep this guide current.



