# Issue #20: Desktop UI Main Dashboard

**Status**: 🏗️ **IN PROGRESS**  
**Assigned**: AI Assistant  
**Created**: November 13, 2025  
**Started**: Not Started  
**Completed**: Not Completed  
**Duration**: ~4 days (estimated)  
**Epic**: Epic 5 (Desktop UI Application)  
**Priority**: P0 (High visibility UI entry point)  
**Dependencies**: Issue #19 ⏳ (UI Foundation)
**Final Commit**: `f36a59c`

> **NOTE**: Builds the primary dashboard surface providing trader summaries, system status, notifications, and quick stats atop the UI foundation delivered in Issue #19.

---

## 📋 **Objective**

Deliver the Desktop UI main dashboard view with real-time summaries for AI traders, system health, notifications, and key metrics, leveraging the MVVM/navigation framework.

---

## 🎯 **Goals**

1. **Trader Overview**: Present AI trader list with status, performance KPIs, and quick actions.
2. **System Health**: Surface core service, exchange connections, and resource metrics to operators.
3. **Notifications & Stats**: Provide contextual alerts and at-a-glance statistics drawn from telemetry feeds.
4. **Real-Time Updates**: Integrate WebSocket telemetry streams for live dashboard refreshes.

---

## 📝 **Task Breakdown**

### **Task 1: View & ViewModel Structure** [Status: ✅ COMPLETE]
- [x] Create `DashboardView` and `DashboardViewModel` (TornadoFX).
- [x] Define observable models for trader summaries, system status, notifications, and quick stats.
- [x] Wire navigation entry (route registration with Issue #19 navigation service).

### **Task 2: Trader Overview Panel** [Status: ✅ COMPLETE]
- [x] Implement trader list component with status badges and trend indicators.
- [x] Add quick actions (start/stop trader, open detail view) with command routing.
- [x] Display performance metrics (daily P&L, win rate, uptime).

### **Task 3: System Status & Quick Stats** [Status: ✅ COMPLETE]
- [x] Create system status panel summarizing core service availability, exchange connectivity, telemetry health.
- [x] Build quick stats tiles (active traders, open positions, daily trades, alerts).
- [x] Implement warning/error highlighting rules (color codes, icons).

### **Task 4: Notifications & Activity Feed** [Status: ✅ COMPLETE]
- [x] Design notifications list with severity, timestamp, and action buttons (acknowledge, open details).
- [x] Add activity feed or log snippet area fed by telemetry events.
- [x] Support filtering (by severity/type) and archiving (rolling buffer of 12 items).

### **Task 5: Data Integration & Telemetry** [Status: ✅ COMPLETE]
- [x] Connect to `TelemetryClient` via service interface (Issue #19 stub updated to emit info/warning/critical samples).
- [x] Map incoming events (trader status, risk alerts, market data) to dashboard models.
- [x] Add quick stats + system status derivations and telemetry connectivity tracking.

### **Task 6: Testing & QA** [Status: ✅ COMPLETE]
- [x] Unit tests for `DashboardViewModel` (state aggregation, notifications, command routing).
- [x] UI smoke tests using JavaFX/TestFX harness (`DashboardViewTest`).
- [x] Manual scenario validation: run stub telemetry events (observed in UI) + `:desktop-ui:test`.

### **Task 7: Documentation & Workflow** [Status: ⏳ PENDING]
- [ ] Update `Development_Plan_v2.md` and Issue log with dashboard scope.
- [ ] Document dashboard usage in `Development_Handbook/AI_TRADER_UI_GUIDE.md` (new section).
- [ ] Follow `DEVELOPMENT_WORKFLOW.md` (Steps 1–7) for planning, testing, CI verification.

### **Task 8: Build & Commit** [Status: ⏳ PENDING]
- [ ] Run `./gradlew clean build --no-daemon`.
- [ ] Ensure `:desktop-ui:test` passes (including TestFX tests).
- [ ] Commit and push (`feat(ui): implement desktop main dashboard (Issue #20)`).
- [ ] Monitor CI with `check-ci-status.ps1 -Watch -WaitSeconds 20`.

---

## 📦 **Deliverables**

### **Source**
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/dashboard/DashboardView.kt`
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/dashboard/DashboardViewModel.kt`
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/dashboard/DashboardContract.kt`

### **Tests**
- `desktop-ui/src/test/kotlin/com/fmps/autotrader/desktop/dashboard/DashboardViewModelTest.kt`
- `desktop-ui/src/test/kotlin/com/fmps/autotrader/desktop/dashboard/DashboardViewTest.kt`

### **Documentation**
- Dashboard section in `Development_Handbook/AI_DESKTOP_UI_GUIDE.md` (v0.2)
- Epic 5 progress updates (plan/status docs)
- Localization & theme references (`messages.properties`, `styles/theme.css`)

---

## 🔍 **Testing & Verification**

- Automated: `./gradlew :desktop-ui:test` (Nov 13 2025 – dashboard module tests green)
- Automated: `./gradlew clean build --no-daemon` (Nov 13 2025 – full project build ✅)
- Manual telemetry simulation (stub service emits info/warn/critical samples observed in UI).
- GitHub Actions run recorded in issue once complete.

---

## 🎯 **Success Criteria**

| Criterion | Status | Verification Method |
|-----------|--------|---------------------|
| Dashboard renders trader overview, system status, notifications, quick stats | ✅ | Manual UI validation + TestFX smoke test |
| Trader quick actions trigger navigation/commands correctly | ✅ | Unit tests + manual click flows |
| Live telemetry updates reflected without app restart | ✅ | Stub telemetry stream observed in UI |
| Error/fallback states handled gracefully (UI messaging) | ⏳ | Simulated service downtime scenarios |
| CI & local builds pass | ⏳ | `./gradlew clean build --no-daemon` + GitHub Actions |

---

## 📈 **Definition of Done**

- [ ] Dashboard view & view model implemented with binding to telemetry services.
- [ ] Unit/UI tests passing; manual scenario sign-off completed.
- [ ] Documentation and plan/status updates committed.
- [ ] CI green for final commit.
- [ ] Issue dependencies (Issue #19) satisfied and referenced.

---

## 💡 **Notes & Risks**

- Coordinate with backend to confirm telemetry payload structure (Issue #17 documentation); stub currently emits synthetic JSON.
- Ensure quick actions do not block UI thread (coroutines wrap future core-service commands).
- Plan for responsive layout adjustments (wide vs. laptop screens) to minimize rework in Epic 6.
