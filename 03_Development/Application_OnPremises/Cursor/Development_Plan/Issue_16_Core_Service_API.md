---
title: Issue #16: Core Service REST API Hardening
---

# Issue #16: Core Service REST API Hardening

**Status**: 📋 **PLANNED**  
**Assigned**: AI Assistant  
**Created**: November 11, 2025  
**Started**: Not Started  
**Completed**: Not Completed  
**Duration**: ~3 days (estimated)  
**Epic**: Epic 4 (Core Service & API)  
**Priority**: P0 (Critical – prerequisite for Epic 5 UI and Epic 6 release)  
**Dependencies**: Issue #11 ✅ (AI Trader Core), Issue #13 ✅ (Position Manager), Issue #14 ✅ (Risk Manager)  
**Final Commit**: _Pending_

> **NOTE**: Hardens the existing REST API surface with authentication, validation, pagination, and operational telemetry so the core service is production-ready for on-premises deployment.

---

## 📋 **Objective**

Upgrade the Core REST API to production quality by adding API key authentication, consistent validation/error handling, pagination, and health/metrics instrumentation while keeping backward compatibility for existing automation scripts.

---

## 🎯 **Goals**

1. **Authentication**: Enforce API key authentication on all external-facing REST endpoints.
2. **Validation & Pagination**: Normalize request validation, introduce pagination filters, and align error envelopes.
3. **Observability**: Add health endpoints, structured logging, and metrics export for operations.
4. **Documentation & Testing**: Update API references and extend automated tests to cover new behaviors.

---

## 📝 **Task Breakdown**

### **Task 1: API Audit & Contract Definition** [Status: ⏳ PENDING]
- [ ] Review existing REST routes against desktop UI requirements.
- [ ] Document any new endpoints or payload adjustments (e.g., risk summary, service info).
- [ ] Produce updated internal API contract (OpenAPI-lite summary).

### **Task 2: Implement Authentication & Middleware** [Status: ⏳ PENDING]
- [ ] Introduce API key middleware with configuration support (`application.conf`, environment overrides).
- [ ] Ensure backwards compatibility with local dev/test defaults.
- [ ] Add structured request/response logging (status codes, latency).

### **Task 3: Validation, Pagination & Error Handling** [Status: ⏳ PENDING]
- [ ] Standardize validation for trader/position/risk inputs with clear error messages.
- [ ] Add pagination & filtering to history-heavy endpoints.
- [ ] Implement consistent error envelopes and map exceptions to HTTP responses.

### **Task 4: Observability Enhancements** [Status: ⏳ PENDING]
- [ ] Add `/health` and `/metrics` endpoints (Prometheus-ready via Micrometer).
- [ ] Expose key counters (requests, errors, auth failures).
- [ ] Ensure logging integrates with existing `LoggingContext` / MDC metadata.

### **Task 5: Testing** [Status: ⏳ PENDING]
- [ ] Update unit tests for `TraderRoutesTest`, `PositionRoutesTest`, etc., covering auth/validation paths.
- [ ] Add integration tests for pagination and error behaviour.
- [ ] Manual smoke tests with HTTPie/Postman for secured endpoints.
- [ ] Verify Jacoco coverage remains ≥80% for API package.

### **Task 6: Documentation & Release Prep** [Status: ⏳ PENDING]
- [ ] Update `Development_Handbook/API_REFERENCE.md` with new endpoints/auth flow.
- [ ] Document configuration steps for API keys and metrics.
- [ ] Note release highlights in `EPIC_4_STATUS.md` and `Development_Plan_v2.md`.

### **Task 7: Build & Commit** [Status: ⏳ PENDING]
- [ ] Run `./gradlew clean build --no-daemon`.
- [ ] Fix compilation/test issues; ensure CI passes.
- [ ] Commit changes with descriptive messages and push to GitHub.
- [ ] Update this issue file with actual dates and final commit hash.

---

## 📦 **Deliverables**

### **Updated Source**
- `core-service/src/main/kotlin/com/fmps/autotrader/core/api/routes/TraderRoutes.kt`
- `core-service/src/main/kotlin/com/fmps/autotrader/core/api/routes/PositionRoutes.kt`
- `core-service/src/main/kotlin/com/fmps/autotrader/core/api/routes/ConfigRoutes.kt`
- `core-service/src/main/kotlin/com/fmps/autotrader/core/api/middleware/AuthPipeline.kt` (new helper)
- `core-service/src/main/kotlin/com/fmps/autotrader/core/api/HealthRoutes.kt` (new)
- Related configuration files (`application.conf`, secrets templates as needed)

### **Tests**
- `core-service/src/test/kotlin/com/fmps/autotrader/core/api/routes/*Test.kt`
- New/updated integration tests for auth and pagination flows.

### **Documentation**
- `Development_Handbook/API_REFERENCE.md`
- `Development_Plan_v2.md` (progress update)
- `EPIC_4_STATUS.md` (status change)

---

## 🎯 **Success Criteria**

| Criterion | Status | Verification Method |
|-----------|--------|---------------------|
| API key authentication enforced across REST endpoints | ⏳ | Manual and automated auth tests |
| Validation/pagination standardized with consistent error envelopes | ⏳ | Unit/integration tests & manual checks |
| Health/metrics endpoints exposed for monitoring | ⏳ | `curl /health`, Prometheus scrape |
| Automated tests updated and passing (≥80% coverage) | ⏳ | `./gradlew :core-service:test`, Jacoco report |
| Documentation updated (API reference, configuration) | ⏳ | Documentation review |
| CI pipeline green post-merge | ⏳ | GitHub Actions checkmark |

---

## 📊 **Test Coverage Approach**

- **Unit Tests**: Route handler auth/validation, middleware behavior, error mapping.
- **Integration Tests**: Auth success/failure, pagination endpoints, health check.
- **Manual QA**: Smoke tests with API key rotation, metrics inspection, logging verification.

---

## 🔧 **Key Technologies**

| Technology | Version | Purpose |
|------------|---------|---------|
| Ktor Server | 2.3+ | REST framework & middleware |
| Micrometer + Prometheus Registry | 1.11+ | Metrics export |
| Kotlin Coroutines | 1.7+ | Async request handling |
| SLF4J + Logback | 1.4+ | Structured logging |

```kotlin
dependencies {
    implementation("io.ktor:ktor-server-auth:2.3.5")
    implementation("io.ktor:ktor-server-metrics-micrometer:2.3.5")
    implementation("io.micrometer:micrometer-registry-prometheus:1.11.5")
    testImplementation("io.ktor:ktor-server-tests:2.3.5")
}
```

---

## ⏱️ **Estimated Timeline**

| Task | Estimated Time |
|------|---------------|
| Task 1: API Audit & Contract | 4 hours |
| Task 2: Authentication & Middleware | 8 hours |
| Task 3: Validation/Pagination/Error Handling | 6 hours |
| Task 4: Observability Enhancements | 4 hours |
| Task 5: Testing | 6 hours |
| Task 6: Documentation | 4 hours |
| Task 7: Build & Commit | 2 hours |
| **Total** | **~3 working days** |

---

## 🔄 **Dependencies**

### **Depends On**
- ✅ Issue #11 – AI Trader Core (service endpoints rely on trader services)
- ✅ Issue #13 – Position Manager (position endpoints & metrics)
- ✅ Issue #14 – Risk Manager (risk exposure endpoints)

### **Blocks**
- Issue #17 – Real-Time WebSocket Telemetry (shares auth & logging)
- Issue #18 – Windows Service Packaging (needs finalized health/metrics endpoints)

### **Related**
- Issue #03 – REST API Server (foundation)
- Epic 5 – Desktop UI (consumes hardened APIs)

---

## 📚 **Resources**

- Ktor Authentication docs: https://ktor.io/docs/authentication.html
- Micrometer Prometheus guide: https://micrometer.io/docs/registry/prometheus
- Internal API usage notes (UI contract spreadsheet)

---

## ⚠️ **Risks & Considerations**

| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking existing automation scripts | Medium | Maintain backward-compatible defaults; document deprecations |
| Misconfiguration of API keys | High | Provide sample config, fallback dev key, and troubleshooting docs |
| Performance regression from extra validation/logging | Medium | Benchmark endpoints; allow tuning via config |
| Exposure of metrics endpoint | Low | Restrict to localhost or require API key |

---

## 📈 **Definition of Done**

- [ ] All tasks completed with checklists updated
- [ ] REST API secured and validated per success criteria
- [ ] Metrics/health endpoints operational
- [ ] Unit/integration tests added and passing locally & in CI
- [ ] Documentation updated (API reference, config guide, status docs)
- [ ] Development_Plan_v2.md reflects progress
- [ ] Code merged and Issue #16 closed

---

## 💡 **Notes & Learnings** (to be filled during execution)

- [Pending – add during implementation]

---

**Issue Created**: November 11, 2025  
**Priority**: P0  
**Estimated Effort**: ~3 days  
**Status**: 📋 PLANNED

---

**Next Steps**:
1. Review auth/pagination approach with stakeholders.
2. Begin Task 1: API audit & contract refinement.
3. Keep status synced with `EPIC_4_STATUS.md` and `Development_Plan_v2.md`.

---

