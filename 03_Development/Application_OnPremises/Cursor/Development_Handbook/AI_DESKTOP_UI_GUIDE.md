# FMPS AutoTrader Desktop UI – Developer Guide

**Version**: 0.1  
**Last Updated**: November 13, 2025  
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
| `desktop.views` | Feature placeholder views for Issues #20–#24 |
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

    factory { ShellViewModel(get(), get(), get()) }
    factory { DashboardView() }
    factory { TraderManagementPlaceholderView() }
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
| `traders` | AI Traders | `TraderManagementPlaceholderView` |
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

## 8. Service Stubs & Data Binding

- `CoreServiceClient.traderSummaries()` exposes a `Flow<List<TraderSummary>>`. The stub emits synthetic updates every 4 seconds to exercise bindings.
- `TelemetryClient` defines `samples()` for future WebSocket integration. `StubTelemetryClient` emits JSON snippets to demonstrate structure – Issue #22 will replace this with the real telemetry hub.
- `ShellViewModel` collects trader summaries and publishes quick stats + toast events.

---

## 9. Run & Build Commands

| Task | Command |
|------|---------|
| Standard desktop run | `./gradlew :desktop-ui:run` |
| Windows-tuned run | `./gradlew :desktop-ui:runDesktopWindows` or `desktop-ui\run-desktop-windows.bat` |
| macOS-tuned run | `./gradlew :desktop-ui:runDesktopMac` or `desktop-ui/run-desktop-macos.sh` |
| Clean build (module) | `./gradlew :desktop-ui:clean build --no-daemon` |
| Full project build | `./gradlew clean build --no-daemon` |

JavaFX launcher scripts honour `--add-opens=javafx.graphics/javafx.stage=ALL-UNNAMED` so TestFX and runtime injection work without JVM access errors.

---

## 10. Next Steps (Epics 5 & 6)

1. **Issue #20** – Replace `DashboardView` placeholder with real tiles bound to REST + telemetry feeds.
2. **Issue #21** – Swap `TraderManagementPlaceholderView` with CRUD forms and command controls.
3. **Issue #22** – Wire `TelemetryClient` to WebSocket hub (`TelemetryHub` from Issue #17) and build chart overlays.
4. **Issue #23** – Implement configuration forms leveraging `CoreServiceClient` + secure credential storage.
5. **Issue #24** – Build pattern analytics grid/visualisation using `PatternSummary` DTOs from the core service.

All issues should reuse the base MVVM scaffolding and follow the `Development_Workflow.md` gating steps.

---

## 11. Change Log

| Version | Date | Description |
|---------|------|-------------|
| 0.1 | 2025-11-13 | Initial publication after Issue #19 foundation completion |

--- 

**Questions / Feedback:** raise via Issue planning documents or ping the development handbook maintainer.  
**Reminder:** continue to document new components/views as they are implemented to keep this guide current.



