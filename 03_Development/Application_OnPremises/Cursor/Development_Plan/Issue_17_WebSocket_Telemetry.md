---
title: Issue #17: Real-Time WebSocket Telemetry
---

# Issue #17: Real-Time WebSocket Telemetry

**Status**: 📋 **PLANNED**  
**Assigned**: AI Assistant  
**Created**: November 11, 2025  
**Started**: Not Started  
**Completed**: Not Completed  
**Duration**: ~2.5 days (estimated)  
**Epic**: Epic 4 (Core Service & API)  
**Priority**: P1 (High – required for Desktop UI dashboards)  
**Dependencies**: Issue #07 ✅ (Exchange Connector Framework), Issue #13 ✅ (Position Manager), Issue #16 ⏳ (REST API Hardening)  
**Final Commit**: _Pending_

> **NOTE**: Delivers authenticated, resilient WebSocket channels supplying trader status, position updates, risk alerts, and market snapshots for the desktop UI and future automation clients.

---

## 📋 **Objective**

Enhance the Core WebSocket subsystem to provide multi-channel, authenticated telemetry with heartbeat/replay support, observability metrics, and client-facing documentation so the desktop UI can rely on real-time data feeds.

---

## 🎯 **Goals**

1. **Channel Coverage**: Offer dedicated channels for traders, positions, risk alerts, and market data.
2. **Reliability**: Provide heartbeats, reconnection logic, and optional replay of latest state.
3. **Security & Throttling**: Reuse API key auth and enforce per-connection limits.
4. **Observability & Documentation**: Emit metrics/logs and publish integration guidance.

---

## 📝 **Task Breakdown**

### **Task 1: Protocol & Schema Design** [Status: ⏳ PENDING]
- [ ] Document channel catalogue, subscription syntax, and payload schema.
- [ ] Define heartbeat cadence, replay options, and client ACK expectations.
- [ ] Align with REST API naming to ensure consistency.

### **Task 2: Implementation Enhancements** [Status: ⏳ PENDING]
- [ ] Extend `WebSocketManager`/`SubscriptionManager` to manage named channels.
- [ ] Integrate API key authentication (leveraging Issue #16 middleware).
- [ ] Implement heartbeat/ping handling and connection timeouts.
- [ ] Provide optional replay of latest state upon subscription (where applicable).

### **Task 3: Resiliency & Observability** [Status: ⏳ PENDING]
- [ ] Add per-connection rate limiting / backpressure logic.
- [ ] Capture metrics: active connections, messages per channel, dropped messages.
- [ ] Add structured logging for subscriptions, disconnects, and errors.
- [ ] Create admin command/tooling to disconnect stale clients.

### **Task 4: Testing** [Status: ⏳ PENDING]
- [ ] Update unit tests for `SubscriptionManagerTest` and `WebSocketManagerTest`.
- [ ] Add integration tests covering auth success/failure and heartbeat/replay flows.
- [ ] Run manual smoke tests using `wscat`/Postman with various channel scenarios.
- [ ] Ensure Jacoco coverage remains ≥80% for WebSocket package.

### **Task 5: Documentation** [Status: ⏳ PENDING]
- [ ] Author/update `Development_Handbook/WEBSOCKET_GUIDE.md` (channel reference, sample clients).
- [ ] Provide troubleshooting tips (heartbeat timeout, invalid auth, rate limit).
- [ ] Update `Development_Plan_v2.md` and `EPIC_4_STATUS.md` with progress.

### **Task 6: Build & Commit** [Status: ⏳ PENDING]
- [ ] Run `./gradlew clean build --no-daemon`.
- [ ] Resolve failing tests or lint issues.
- [ ] Commit with descriptive messages and push to GitHub.
- [ ] Record final commit hash in this issue file.

---

## 📦 **Deliverables**

### **Updated/New Source**
- `core-service/src/main/kotlin/com/fmps/autotrader/core/connectors/websocket/WebSocketManager.kt`
- `core-service/src/main/kotlin/com/fmps/autotrader/core/connectors/websocket/SubscriptionManager.kt`
- `core-service/src/main/kotlin/com/fmps/autotrader/core/connectors/websocket/WebSocketAuth.kt` (new)
- `core-service/src/main/kotlin/com/fmps/autotrader/core/api/routes/WebSocketRoutes.kt`

### **Tests**
- `core-service/src/test/kotlin/com/fmps/autotrader/core/connectors/WebSocketManagerTest.kt`
- `core-service/src/test/kotlin/com/fmps/autotrader/core/connectors/SubscriptionManagerTest.kt`
- New integration tests simulating multi-channel subscriptions and failures.

### **Documentation**
- `Development_Handbook/WEBSOCKET_GUIDE.md`
- Updates to `Development_Plan_v2.md` and `EPIC_4_STATUS.md`

---

## 🎯 **Success Criteria**

| Criterion | Status | Verification Method |
|-----------|--------|---------------------|
| Authenticated WebSocket connections with heartbeat handling | ⏳ | Automated tests + manual clients |
| Channels deliver trader, position, risk, and market telemetry | ⏳ | Integration tests + UI smoke scripts |
| Metrics/logging capture connection health | ⏳ | Inspect Prometheus metrics & logs |
| Rate limiting/backpressure prevents resource exhaustion | ⏳ | Stress tests & configuration review |
| Documentation published for clients/admins | ⏳ | Document review |
| CI pipeline green post changes | ⏳ | GitHub Actions |

---

## 📊 **Test Coverage Approach**

- **Unit Tests**: Channel subscription management, heartbeat logic, auth gating.
- **Integration Tests**: Multi-channel broadcasting, reconnection scenarios, replay.
- **Manual Tests**: `wscat`/Postman sessions, network interruption simulations.

---

## 🔧 **Key Technologies**

| Technology | Version | Purpose |
|------------|---------|---------|
| Ktor WebSockets | 2.3+ | WebSocket endpoint implementation |
| Kotlin Coroutines | 1.7+ | Structured concurrency for broadcasting |
| Micrometer | 1.11+ | Metrics instrumentation |
| SLF4J + Logback | 1.4+ | Structured logging |

```kotlin
dependencies {
    implementation("io.ktor:ktor-server-websockets:2.3.5")
    implementation("io.ktor:ktor-server-auth:2.3.5")
    implementation("io.micrometer:micrometer-registry-prometheus:1.11.5")
    testImplementation("io.ktor:ktor-server-tests:2.3.5")
}
```

---

## ⏱️ **Estimated Timeline**

| Task | Estimated Time |
|------|---------------|
| Task 1: Protocol Design | 4 hours |
| Task 2: Implementation | 8 hours |
| Task 3: Resiliency & Observability | 5 hours |
| Task 4: Testing | 6 hours |
| Task 5: Documentation | 3 hours |
| Task 6: Build & Commit | 2 hours |
| **Total** | **~2.5 working days** |

---

## 🔄 **Dependencies**

### **Depends On**
- ✅ Issue #07 – Exchange Connector Framework (provides data sources)
- ✅ Issue #13 – Position Manager (position feeds)
- ⏳ Issue #16 – REST API Hardening (shared auth/middleware)

### **Blocks**
- Epic 5 – Desktop UI (real-time dashboards)
- Issue #18 – Windows Service Packaging (documentation references telemetry behavior)

### **Related**
- Issue #03 – REST API Server (initial WebSocket scaffold)
- Issue #14 – Risk Manager (risk alert payloads)

---

## 📚 **Resources**

- Ktor WebSocket docs: https://ktor.io/docs/servers-websockets.html
- Existing `SubscriptionManager` implementation (Issue #07 baseline)
- Desktop UI telemetry requirements

---

## ⚠️ **Risks & Considerations**

| Risk | Impact | Mitigation |
|------|--------|------------|
| Message floods causing memory pressure | Medium | Implement queue limits & drop policy |
| Auth misalignment with REST API | Medium | Reuse Issue #16 middleware/config |
| Debugging connection issues | Medium | Add detailed logs & correlation IDs |
| Replay consistency | Low | Guard with versioned payloads and default fallbacks |

---

## 📈 **Definition of Done**

- [ ] Channel catalogue implemented and documented
- [ ] Authentication, heartbeat, replay, and metrics working end-to-end
- [ ] Unit/integration tests updated and passing locally & in CI
- [ ] Documentation published (WebSocket guide, plan/status updates)
- [ ] Coverage targets maintained
- [ ] Changes merged and Issue #17 closed

---

## 💡 **Notes & Learnings**

- [Pending – populate during implementation]

---

**Issue Created**: November 11, 2025  
**Priority**: P1  
**Estimated Effort**: ~2.5 days  
**Status**: 📋 PLANNED

---

**Next Steps**:
1. Validate schema/heartbeat plan with UI stakeholders.
2. Kick off Task 1 once Issue #16 auth work is underway.
3. Coordinate release notes with Epic 4 status updates.

---

