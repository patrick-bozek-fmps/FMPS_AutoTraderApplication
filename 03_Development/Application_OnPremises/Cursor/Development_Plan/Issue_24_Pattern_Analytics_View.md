# Issue #24: Pattern Analytics View

**Status**: 📋 **PLANNED**  
**Assigned**: AI Assistant  
**Created**: November 13, 2025  
**Started**: Not Started  
**Completed**: Not Completed  
**Duration**: ~3 days (estimated)  
**Epic**: Epic 5 (Desktop UI Application)  
**Priority**: P2 (Medium – analytics & optimization)  
**Dependencies**: Issue #19 ⏳, Issue #20 ⏳, Issue #21 ⏳, Issue #22 ⏳  
**Final Commit**: `TBD`

> **NOTE**: Delivers the analytics workspace for visualizing trading patterns, performance metrics, and enabling management of learned patterns.

---

## 📋 **Objective**

Expose pattern analytics to operators by listing learned patterns, displaying detailed metrics/visualizations, and providing management utilities to prune or filter patterns.

---

## 🎯 **Goals**

1. **Pattern Catalogue**: List and filter learned patterns with key performance metrics.
2. **Detailed Analytics**: Display confidence, success rates, associated indicators, and historical performance charts.
3. **Visualization & Filtering**: Provide charts/tables to analyze pattern efficacy across symbols/timeframes.
4. **Pattern Management**: Allow operators to archive/delete outdated patterns and refresh data from core-service.

---

## 📝 **Task Breakdown**

### **Task 1: Pattern List & Filters** [Status: ⏳ PENDING]
- [ ] Implement list/grid view of patterns showing name, symbol, timeframe, success rate, occurrences, last updated.
- [ ] Add filters (symbol, trader, timeframe, success threshold) and search.
- [ ] Include badges to highlight top performers/underperformers.

### **Task 2: Pattern Detail View** [Status: ⏳ PENDING]
- [ ] Build detail pane with KPI cards (confidence, profit factor, average hold time).
- [ ] Display indicator configuration and entry/exit conditions (from Issue #10 data models).
- [ ] Include historical chart showing occurrences vs. outcomes (use shared chart components from Issue #22 if possible).

### **Task 3: Visualization & Insights** [Status: ⏳ PENDING]
- [ ] Implement charts/tables for performance over time, by exchange, by trader.
- [ ] Provide download/export of analytics (CSV/JSON) for further analysis.
- [ ] Integrate toggles for comparing multiple patterns.

### **Task 4: Pattern Management Actions** [Status: ⏳ PENDING]
- [ ] Add archive/delete actions with confirmation (persist via pattern repository endpoint).
- [ ] Provide refresh button to re-fetch analytics from core-service.
- [ ] Log management actions for audit (future Epic 6 follow-up).

### **Task 5: Data Integration** [Status: ⏳ PENDING]
- [ ] Connect to pattern analytics REST endpoint (existing repository from Issue #10) to fetch data.
- [ ] Map responses to view model models; support pagination if dataset large.
- [ ] Handle empty states and loading placeholders.

### **Task 6: Testing & Validation** [Status: ⏳ PENDING]
- [ ] Unit tests for filtering, sorting, and analytics calculations.
- [ ] Integration tests with mocked analytics service.
- [ ] Manual QA using sample datasets (stored under `Cursor/Artifacts/patterns`).

### **Task 7: Documentation & Workflow** [Status: ⏳ PENDING]
- [ ] Update `Development_Plan_v2.md` (Epic 5 progress) and Epic status.
- [ ] Document analytics workflows in UI handbook (screenshots, usage tips).
- [ ] Follow `DEVELOPMENT_WORKFLOW.md` for testing, CI, documentation updates.

### **Task 8: Build & Commit** [Status: ⏳ PENDING]
- [ ] Run `./gradlew clean build --no-daemon`.
- [ ] Ensure `:desktop-ui:test` covers analytics view model logic.
- [ ] Commit (`feat(ui): add pattern analytics workspace (Issue #24)`) and push.
- [ ] Validate CI success with `check-ci-status.ps1 -Watch -WaitSeconds 20`.

---

## 📦 **Deliverables**

### **Source**
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/patterns/PatternAnalyticsView.kt`
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/patterns/PatternAnalyticsViewModel.kt`
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/services/PatternAnalyticsService.kt`

### **Tests**
- `desktop-ui/src/test/kotlin/com/fmps/autotrader/desktop/patterns/PatternAnalyticsViewModelTest.kt`
- `desktop-ui/src/test/kotlin/com/fmps/autotrader/desktop/patterns/FilteringLogicTest.kt`

### **Documentation**
- Pattern analytics section in `Development_Handbook/AI_TRADER_UI_GUIDE.md`
- Sample datasets/JSON in `Cursor/Artifacts/patterns/`

---

## 🔍 **Testing & Verification**

- `./gradlew :desktop-ui:test`
- `./gradlew clean build --no-daemon`
- Manual: load sample analytics dataset, verify charts and filters behave as expected.
- Record GitHub Actions run ID upon completion.

---

## 🎯 **Success Criteria**

| Criterion | Status | Verification Method |
|-----------|--------|---------------------|
| Pattern list/filtering works with live data | ⏳ | Unit tests + manual dataset verification |
| Detail view surfaces KPIs and historical chart accurately | ⏳ | Integration tests + manual checks |
| Analytics visualizations render without performance issues | ⏳ | Manual testing with large dataset |
| Archive/delete actions persist correctly | ⏳ | Mocked service tests + manual confirmation |
| Builds & CI pass | ⏳ | `./gradlew clean build --no-daemon` + GitHub Actions |

---

## 📈 **Definition of Done**

- [ ] Pattern analytics view & view model implemented with charts, filters, management actions.
- [ ] Automated tests and manual QA completed with documented results.
- [ ] Documentation/screenshots updated; plan/status reflect progress.
- [ ] CI green for final commit.

---

## 💡 **Notes & Risks**

- Charting complexity may overlap with Issue #22—reuse components to reduce maintenance.
- Ensure analytics endpoints provide necessary data; coordinate with backend if additional aggregations required.
- Anticipate need for pagination/virtualization if dataset grows; design UI to accommodate future scalability.
