# Issue #22: Trading Monitoring View

**Status**: 📋 **PLANNED**  
**Assigned**: AI Assistant  
**Created**: November 13, 2025  
**Started**: Not Started  
**Completed**: Not Completed  
**Duration**: ~5 days (estimated)  
**Epic**: Epic 5 (Desktop UI Application)  
**Priority**: P1 (High – mission-critical monitoring)  
**Dependencies**: Issue #19 ⏳, Issue #20 ⏳, Issue #21 ⏳  
**Final Commit**: `TBD`

> **NOTE**: Delivers the live trading monitoring workspace with charts, active positions, trade history, and telemetry-driven updates for market data.

---

## 📋 **Objective**

Provide real-time trading visualizations (price charts, indicators), active position dashboards, and trade history views with deep integration to telemetry and exchange data feeds.

---

## 🎯 **Goals**

1. **Real-Time Charts**: Render candlestick charts with indicator overlays and live price updates.
2. **Position Insights**: Display active positions, P&L, and risk metrics dynamically.
3. **Trade History & Order Book**: Provide historical trade timelines and an optional order book view.
4. **Robust Telemetry Integration**: Ensure seamless updates from WebSocket streams with fallbacks to REST polling.

---

## 📝 **Task Breakdown**

### **Task 1: Charting Infrastructure** [Status: ⏳ PENDING]
- [ ] Select/implement charting library (e.g., TornadoFX charts / third-party) for candlesticks and overlays.
- [ ] Build chart adapter to map market data ticks into UI updates.
- [ ] Support indicator overlays (e.g., SMA, Bollinger Bands) with toggles.
- [ ] Implement zoom/pan interactions and timeframe selection.

### **Task 2: Active Positions Panel** [Status: ⏳ PENDING]
- [ ] Create positions table/cards with symbol, size, entry, current price, P&L, stop/take-profit levels.
- [ ] Highlight risk alerts (stop-loss triggers, drawdown thresholds) using telemetry alerts.
- [ ] Add quick actions (close position, adjust stop) hooking into core-service endpoints (future Epic 6 enhancements flagged if not available).

### **Task 3: Trade History & Order Book** [Status: ⏳ PENDING]
- [ ] Implement trade history list with filters (time range, trader, symbol).
- [ ] Visualize trade outcomes (win/loss, duration) via icons or color codes.
- [ ] (Optional) Add order book snapshot component subscribed to market data channel.

### **Task 4: Data Services & Telemetry Binding** [Status: ⏳ PENDING]
- [ ] Extend telemetry client to subscribe to `market-data`, `positions`, and `risk-alerts` channels.
- [ ] Map incoming events to chart/position history structures with buffering to avoid UI thrash.
- [ ] Provide REST fallback fetch (initial load / reconnection).

### **Task 5: Performance & Resilience** [Status: ⏳ PENDING]
- [ ] Implement throttling/debouncing for chart updates to maintain smooth rendering.
- [ ] Handle connection drops gracefully (status indicators, retry logic).
- [ ] Profile UI responsiveness (JavaFX performance, memory usage) with sample datasets.

### **Task 6: Testing & Validation** [Status: ⏳ PENDING]
- [ ] Unit tests for telemetry data mapping, buffering, and fallback logic.
- [ ] Integration tests using simulated market data streams (mock service).
- [ ] Manual QA: run mock scenarios (volatile market, rapid trades, connection loss) and capture findings.

### **Task 7: Documentation & Workflow** [Status: ⏳ PENDING]
- [ ] Update `Development_Plan_v2.md` and Epic status with monitoring progress.
- [ ] Document monitoring view usage and troubleshooting tips in UI handbook.
- [ ] Follow `DEVELOPMENT_WORKFLOW.md` for testing, CI, and documentation sign-off.

### **Task 8: Build & Commit** [Status: ⏳ PENDING]
- [ ] `./gradlew clean build --no-daemon`
- [ ] Confirm `:desktop-ui:test` coverage (chart and telemetry tests).
- [ ] Commit (`feat(ui): add trading monitoring workspace (Issue #22)`) and push.
- [ ] Validate CI via `check-ci-status.ps1 -Watch -WaitSeconds 20`.

---

## 📦 **Deliverables**

### **Source**
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/monitoring/MonitoringView.kt`
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/monitoring/MonitoringViewModel.kt`
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/monitoring/charts/*`
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/services/MarketDataService.kt`

### **Tests**
- `desktop-ui/src/test/kotlin/com/fmps/autotrader/desktop/monitoring/MonitoringViewModelTest.kt`
- `desktop-ui/src/test/kotlin/com/fmps/autotrader/desktop/monitoring/TelemetryAdapterTest.kt`

### **Documentation**
- Monitoring chapter in `Development_Handbook/AI_TRADER_UI_GUIDE.md`
- Performance profiling notes (if applicable) in `Cursor/Artifacts/ui/monitoring`

---

## 🔍 **Testing & Verification**

- Automated: `./gradlew :desktop-ui:test`, `./gradlew clean build --no-daemon`
- Manual: market simulation script to push events (record demo video/screenshots).
- Log CI run URL in issue upon completion.

---

## 🎯 **Success Criteria**

| Criterion | Status | Verification Method |
|-----------|--------|---------------------|
| Candlestick/indicator charts update in near real-time | ⏳ | Simulated feed + performance monitoring |
| Active positions view reflects telemetry and REST fallback | ⏳ | Manual test + automated integration test |
| Trade history and (optional) order book display accurate data | ⏳ | REST fetch + UI assertions |
| UI remains responsive under high-frequency updates | ⏳ | Profiling + stress simulation |
| Builds/tests/CI pass | ⏳ | `./gradlew clean build --no-daemon` + GitHub Actions |

---

## 📈 **Definition of Done**

- [ ] Monitoring view and view model implemented with charting, positions, history components.
- [ ] Telemetry integration validated with simulated feeds and unit tests.
- [ ] Documentation updated with usage and troubleshooting guidance.
- [ ] CI green for final commit and issue cross-referenced in plan/status docs.

---

## 💡 **Notes & Risks**

- Charting performance may necessitate evaluating third-party libraries—log any license considerations.
- Coordinate with telemetry team for message format changes (Issue #17 artifacts).
- Consider accessibility (color usage) when implementing P&L indicators to avoid rework later.
