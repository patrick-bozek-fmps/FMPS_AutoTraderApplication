# Issue #20: Desktop UI Main Dashboard

**Status**: 📋 **PLANNED**  
**Assigned**: AI Assistant  
**Created**: November 13, 2025  
**Started**: Not Started  
**Completed**: Not Completed  
**Duration**: ~4 days (estimated)  
**Epic**: Epic 5 (Desktop UI Application)  
**Priority**: P0 (High visibility UI entry point)  
**Dependencies**: Issue #19 ⏳ (UI Foundation)
**Final Commit**: `TBD`

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

### **Task 1: View & ViewModel Structure** [Status: ⏳ PENDING]
- [ ] Create `DashboardView` and `DashboardViewModel` (TornadoFX).
- [ ] Define observable models for trader summaries, system status, notifications, and quick stats.
- [ ] Wire navigation entry (route registration with Issue #19 navigation service).

### **Task 2: Trader Overview Panel** [Status: ⏳ PENDING]
- [ ] Implement trader list component with status badges and trend indicators.
- [ ] Add quick actions (start/stop trader, open detail view) with command routing.
- [ ] Display performance metrics (daily P&L, win rate, uptime).

### **Task 3: System Status & Quick Stats** [Status: ⏳ PENDING]
- [ ] Create system status panel summarizing core service availability, exchange connectivity, telemetry health.
- [ ] Build quick stats tiles (active traders, open positions, daily trades, alerts).
- [ ] Implement warning/error highlighting rules (color codes, icons).

### **Task 4: Notifications & Activity Feed** [Status: ⏳ PENDING]
- [ ] Design notifications list with severity, timestamp, and action buttons (acknowledge, open details).
- [ ] Add activity feed or log snippet area fed by telemetry events.
- [ ] Support filtering (by severity/type) and archiving.

### **Task 5: Data Integration & Telemetry** [Status: ⏳ PENDING]
- [ ] Connect to `TelemetryCollector` WebSocket via service interface (Issue #19 stub -> implementation).
- [ ] Map incoming events (trader status, risk alerts, market data) to dashboard models.
- [ ] Add fallback polling (REST) for resilience if WebSocket unavailable.

### **Task 6: Testing & QA** [Status: ⏳ PENDING]
- [ ] Unit tests for `DashboardViewModel` (data mapping, command routing).
- [ ] UI smoke tests using TestFX (verify components render critical sections).
- [ ] Manual scenario validation: simulate telemetry events (mock service) and confirm UI updates.

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
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/dashboard/components/*`

### **Tests**
- `desktop-ui/src/test/kotlin/com/fmps/autotrader/desktop/dashboard/DashboardViewModelTest.kt`
- `desktop-ui/src/test/kotlin/com/fmps/autotrader/desktop/dashboard/DashboardViewTest.kt`

### **Documentation**
- Dashboard section in `Development_Handbook/AI_TRADER_UI_GUIDE.md`
- Epic 5 progress updates (plan/status docs)

---

## 🔍 **Testing & Verification**

- Automated: `./gradlew :desktop-ui:test`, `./gradlew clean build --no-daemon`
- Manual telemetry simulation (mock service) to ensure live updates appear within 1s refresh cadence.
- GitHub Actions run recorded in issue once complete.

---

## 🎯 **Success Criteria**

| Criterion | Status | Verification Method |
|-----------|--------|---------------------|
| Dashboard renders trader overview, system status, notifications, quick stats | ⏳ | Manual UI validation + TestFX screenshot assertions |
| Trader quick actions trigger navigation/commands correctly | ⏳ | Unit tests + manual click flows |
| Live telemetry updates reflected without app restart | ⏳ | Mock WebSocket event stream test |
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

- Coordinate with backend to confirm telemetry payload structure (Issue #17 documentation).
- Ensure quick actions do not block UI thread (use coroutines/async operations).
- Plan for responsive layout adjustments (wide vs. laptop screens) to minimize rework in Epic 6.
