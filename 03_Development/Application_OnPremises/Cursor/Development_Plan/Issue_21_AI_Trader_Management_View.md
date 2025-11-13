# Issue #21: AI Trader Management View

**Status**: 📋 **PLANNED**  
**Assigned**: AI Assistant  
**Created**: November 13, 2025  
**Started**: Not Started  
**Completed**: Not Completed  
**Duration**: ~4 days (estimated)  
**Epic**: Epic 5 (Desktop UI Application)  
**Priority**: P0 (Core trader lifecycle management)  
**Dependencies**: Issue #19 ⏳, Issue #20 ⏳  
**Final Commit**: `TBD`

> **NOTE**: Implements full CRUD-style management for AI traders—creation, configuration, status monitoring, and control—building on the shared UI foundation and dashboard context.

---

## 📋 **Objective**

Provide a dedicated management workspace for AI traders, including list/detail views, creation wizard, configuration editing, and integration with core-service APIs.

---

## 🎯 **Goals**

1. **Comprehensive Trader List**: Display all configured traders with filters, sorting, and health indicators.
2. **Creation & Editing Experience**: Deliver guided forms for creating/editing traders with validation and server interaction.
3. **Lifecycle Controls**: Enable start/stop/delete actions and show real-time performance metrics.
4. **Telemetry & Persistence Integration**: Bind view models to REST/WebSocket services for configuration and status updates.

---

## 📝 **Task Breakdown**

### **Task 1: Trader List & Filters** [Status: ⏳ PENDING]
- [ ] Implement list/table view with columns (name, exchange, status, strategy, P&L, uptime).
- [ ] Add filtering (status, exchange, strategy) and search.
- [ ] Integrate status badges and attention indicators (risk alerts, paused state).

### **Task 2: Trader Detail & Metrics** [Status: ⏳ PENDING]
- [ ] Create detail pane showcasing configuration, performance charts, and recent activity.
- [ ] Display open positions and historical stats summary via tabs/sections.
- [ ] Provide controls (start, stop, refresh, open logs) with confirmation dialogs.

### **Task 3: Create/Edit Trader Wizard** [Status: ⏳ PENDING]
- [ ] Build multi-step form (basic info → exchange settings → budget/risk → strategy selection → review).
- [ ] Implement validation rules (budget limits, API keys presence, unique names).
- [ ] Integrate with REST API (Issue #16 endpoints) for create/update/delete operations.
- [ ] Show success/failure notifications and update list automatically.

### **Task 4: Data & Telemetry Integration** [Status: ⏳ PENDING]
- [ ] Connect view model to REST service for trader CRUD, strategy catalogue, and metrics.
- [ ] Subscribe to telemetry events for live trader status updates (Issue #17).
- [ ] Handle optimistic updates and fallback refresh if telemetry lags.

### **Task 5: Testing & Validation** [Status: ⏳ PENDING]
- [ ] Unit tests for creation wizard validation, REST service interactions (mocked).
- [ ] Integration/UI tests for key flows (create trader, edit, start/stop) using TestFX or component harness.
- [ ] Manual scenarios: simulate API errors, duplicate trader names, invalid credentials.

### **Task 6: Documentation & Workflow Updates** [Status: ⏳ PENDING]
- [ ] Update `Development_Plan_v2.md` and Epic 5 status with trader management progress.
- [ ] Expand UI handbook with trader management usage instructions and screenshots.
- [ ] Follow `DEVELOPMENT_WORKFLOW.md` (tests, CI, documentation).

### **Task 7: Build & Commit** [Status: ⏳ PENDING]
- [ ] `./gradlew clean build --no-daemon`
- [ ] Ensure `:desktop-ui:test` (including wizard validation tests) passes.
- [ ] Commit (`feat(ui): add ai trader management workspace (Issue #21)`) and push.
- [ ] Verify CI success via `check-ci-status.ps1 -Watch -WaitSeconds 20`.

---

## 📦 **Deliverables**

### **Source**
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/traders/TraderManagementView.kt`
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/traders/TraderManagementViewModel.kt`
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/traders/wizard/*`
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/services/TraderService.kt`

### **Tests**
- `desktop-ui/src/test/kotlin/com/fmps/autotrader/desktop/traders/TraderManagementViewModelTest.kt`
- `desktop-ui/src/test/kotlin/com/fmps/autotrader/desktop/traders/TraderWizardValidationTest.kt`

### **Documentation**
- Trader management chapter in `Development_Handbook/AI_TRADER_UI_GUIDE.md`
- Screenshots/mockups stored under `Cursor/Artifacts/ui/`

---

## 🔍 **Testing & Verification**

- Automated: `./gradlew :desktop-ui:test`, `./gradlew clean build --no-daemon`
- Manual: create/edit/delete trader flows against staging core-service (or mocked backend).
- Confirm CI run recorded in issue log with successful conclusion.

---

## 🎯 **Success Criteria**

| Criterion | Status | Verification Method |
|-----------|--------|---------------------|
| Trader list displays accurate data with filters and live updates | ⏳ | Integration test + telemetry simulation |
| Create/Edit wizard validates input and persists changes | ⏳ | Unit tests + manual flow |
| Start/Stop/Delete commands execute and reflect in UI | ⏳ | Manual scenario tests + service mocks |
| Error states surfaced with actionable messaging | ⏳ | Forced API failure tests |
| Build & CI succeed | ⏳ | `./gradlew clean build --no-daemon` + GitHub Actions |

---

## 📈 **Definition of Done**

- [ ] Trader management list, detail, and wizard implemented.
- [ ] Telemetry & REST integration verified with automated and manual tests.
- [ ] Documentation/screenshots updated and linked.
- [ ] CI green for final commit.
- [ ] Issue dependencies resolved and cross-referenced in plan/status documents.

---

## 💡 **Notes & Risks**

- Coordinate with Risk Manager team for risk profile defaults (Issue #14 outcomes).
- Ensure secure handling of API keys—consider using masked inputs and secure storage placeholders until Epic 6 enhancements.
- Usability testing recommended to refine wizard flow prior to release.
