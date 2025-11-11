# Epic 4: Core Service & API Hardening - Status Report

**Date**: November 11, 2025  
**Epic Status**: 🚀 **IN PROGRESS** (1/3 issues complete – 33%)  
**Version**: 1.1  
**Last Updated**: November 11, 2025 (Issue #16 delivered)

---

## 📊 **Executive Summary**

Epic 4 moves the Core service from prototype to production readiness. Planning is complete and execution has started: Issue #16 (REST API Hardening) shipped on November 11, delivering authentication, pagination, and Prometheus metrics. Remaining issues (#17–#18) now build on the hardened API to roll out telemetry and Windows packaging.

The epic still targets three deliverables—REST hardening, real-time telemetry, and Windows service packaging. Downstream dependencies (Epic 5 desktop UI, Epic 6 release prep) now have a secured API contract to integrate against.

**Status**: Implementation underway; telemetry (Issue #17) queued next.

**Key Components**:
- Core REST API Hardening – ✅ Complete (Issue #16)
- Real-Time WebSocket Telemetry – 📋 Planned
- Windows Service Packaging – 📋 Planned

---

## 📋 **Epic Overview**

| Issue | Title | Status | Priority | Duration (Est.) | Dependencies |
|-------|-------|--------|----------|-----------------|--------------|
| #16 | Core Service REST API Hardening | ✅ Complete | P0 | ~3 days (actual: 1 day) | Issue #11 ✅, #13 ✅, #14 ✅ |
| #17 | Real-Time WebSocket Telemetry | 📋 Planned | P1 | ~2.5 days | Issue #07 ✅, #13 ✅, #16 ✅ |
| #18 | Windows Service Packaging & Deployment | 📋 Planned | P0 | ~4 days | Issue #16 ✅, #17 ⏳ |

**Total Estimated Duration**: ~9.5 working days (≈2 weeks)  
**Actual Duration**: 1 day logged (Issue #16)  
**Current Progress**: 1/3 issues complete (33%)

---

## 🎯 **Epic Goals**

1. **Harden Core REST APIs**: Add authentication, validation, pagination, and operational telemetry required for on-prem deployments.
2. **Deliver reliable WebSocket telemetry**: Provide authenticated, resilient, multi-channel real-time feeds for the desktop UI.
3. **Package the Core as a Windows service**: Ship scripts, configuration, and operations playbook for 24/7 service management.
4. **Document production runbooks**: Update API/WebSocket references and create a Windows Service guide.

---

## ✅ **Completed Issues** (1/3 – 33%)

### Issue #16 – Core Service REST API Hardening (Completed Nov 11, 2025)
- API key middleware with configurable key sources (single or list + env overrides).
- Query validation & pagination standardised for `/api/v1/trades` with consistent error envelopes.
- Prometheus metrics exposed via `/metrics` (auth-gated) and Micrometer binders enabled.
- New API security regression tests + documentation set (API reference, config guide).

---

## ⏳ **In Progress Issues** (0/3)

No active development threads; Issue #17 queued to start next.

---

## 📋 **Planned Issues** (3/3)

### **Issue #16: Core Service REST API Hardening** ✅ COMPLETE
- **Priority**: P0  
- **Actual Duration**: 1 day  
- **Dependencies**: Issue #11 ✅, #13 ✅, #14 ✅  

**Delivered**:
- API key middleware (`Security.kt`) with config + env support.
- Pagination/validation upgrades for trade routes and repository.
- Prometheus metrics endpoint and Micrometer instrumentation.
- Updated API reference, config guide, and epic/dev plan documentation.

---

### **Issue #17: Real-Time WebSocket Telemetry** 📋 PLANNED
- **Priority**: P1  
- **Estimated Duration**: ~2.5 days  
- **Dependencies**: Issue #07 ✅, #13 ✅, Issue #16 ⏳  

**Planned Deliverables**:
- [ ] Channel catalogue (trader status, positions, risk alerts, market data)
- [ ] Authenticated subscription protocol with heartbeats/replay
- [ ] Connection metrics, logging, and backpressure controls
- [ ] WebSocket client guide with sample snippets

---

### **Issue #18: Windows Service Packaging & Deployment** 📋 PLANNED
- **Priority**: P0  
- **Estimated Duration**: ~4 days  
- **Dependencies**: Issue #16 ⏳, Issue #17 ⏳  

**Planned Deliverables**:
- [ ] Procrun-based installer scripts (install/upgrade/uninstall)
- [ ] Configuration/logging templates and secure secrets guidance
- [ ] Operations playbook (Windows service guide, recovery checklist)
- [ ] Validation across Windows 10/11 with documented test matrix

---

## 🎯 **Epic Success Criteria**

| Criterion | Status | Verification Method | Notes |
|-----------|--------|---------------------|-------|
| REST API secured with auth, validation, and telemetry | ⏳ | Post-implementation API review + tests | Covered by Issue #16 |
| WebSocket telemetry resilient with metrics and replay | ⏳ | Integration tests + manual client verification | Covered by Issue #17 |
| Windows service installation bundle passes QA | ⏳ | Service install/runbook dry run on Win10/11 | Covered by Issue #18 |
| Documentation (API, WebSocket, Windows Service) updated | ⏳ | Documentation review checklist | Shared across Issues #16–#18 |

---

## 📊 **Planned vs. Actual Implementation Analysis**

### **What Is Planned (Complete Scope)**

| Component | Planned Features | Notes |
|-----------|------------------|-------|
| REST API | Auth middleware, validation/pagination, health metrics, error envelopes | ✅ Delivered in Issue #16 |
| WebSocket | Multi-channel feeds, heartbeats, metrics, replay snapshots | Requires auth pipeline from Issue #16 (now available) |
| Windows Service | Installer scripts, config/log templates, recovery options | Builds on logging/config work from Epic 1 |

### **Additional Features Beyond Minimum**

✅ **API Enhancements**:
- Structured logging for REST endpoints  
- Optional Prometheus export (leveraging Micrometer)

✅ **WebSocket Enhancements**:
- Admin disconnect command for stale clients  
- Per-connection rate limiting/backpressure controls

### **Deferred/Not Implemented (Out of Scope for v1.0)**

❌ **OAuth2 / Role-Based Access Control**  
- **Deferred to**: Epic 6 / v1.1  
- **Reason**: On-prem deployment with single admin user; API key sufficient for MVP.

❌ **MSI Installer**  
- **Deferred to**: Epic 6 (Release preparation)  
- **Reason**: Initial release will use scripted installation to reduce risk.

---

## 🚦 **Critical Path Analysis**

### **What's Blocking Next Epic?**

```
Epic 5: Desktop UI Application
├─ ⏳ Issue #16 – REST API Hardening
│  └─ Required for updated endpoints/auth
├─ ⏳ Issue #17 – WebSocket Telemetry
│  └─ Supplies real-time feeds for UI dashboards
└─ ⏳ Issue #18 – Windows Service Packaging
   └─ Ensures Core is deployable before UI integration tests
```

**Critical Path**:
1. ✅ Issue #16 – REST API Hardening (completed)
2. ⏳ Issue #17 – WebSocket Telemetry (unblocks UI real-time features)
3. ⏳ Issue #18 – Windows Service Packaging (required for integrated testing)

**Result**: Epic 5 remains blocked until Epic 4 execution is underway.

---

## 📋 **Recommended Next Steps**

### **Current Epic Status**

1. ✅ Issue #16 – Core Service REST API Hardening – complete; waiting on CI confirmation
2. 📋 Issue #17 – Real-Time WebSocket Telemetry – ready to kick off (auth prerequisites met)
3. 📋 Issue #18 – Windows Service Packaging & Deployment – blocked until Issue #17 closes

### **Next: Epic 5 – Desktop UI Application**

1. Finalize REST/WebSocket contracts (output from Issues #16–#17)
2. Provide mock servers or fixtures for UI development
3. Align UI backlog with new telemetry capabilities

### **🏆 Key Achievement**

- Comprehensive Epic 4 planning completed on November 11, 2025, with clear dependencies, deliverables, and success criteria captured for all three issues.

---

## 📊 **Current Project Health**

| Metric | Status | Notes |
|--------|--------|-------|
| **Planning Quality** | ✅ | Issue breakdowns remain accurate post Issue #16 delivery |
| **Dependencies** | ✅ | REST auth & metrics prerequisites satisfied for Issue #17 |
| **Blockers** | ✅ | No external blockers; scheduling Issue #17 next |
| **Documentation** | ✅ | Issue file, API reference, Dev Plan v2 updated |
| **Risk Assessment** | ⏳ | Monitor for auth misconfiguration regressions during rollout |

---

## ✅ **Action Items**

### **Completed (Epic 4 Planning)**
1. [x] Publish Issue #16, #17, #18 planning documents  
2. [x] Update `Development_Plan_v2.md` with Epic 4 scope and milestones  
3. [x] Produce Epic 4 status baseline (this document)

### **Next (Execution Kickoff)**
1. [ ] Schedule start date for Issue #16 and assign owner  
2. [ ] Prepare baseline test plans (API/WebSocket smoke suites)  
3. [ ] Coordinate with Ops for Windows service test environment

---

## 📝 **Notes & Risks**

- **Auth Key Distribution**: Need secure process for generating and rotating API keys before Issue #16 work begins.
- **Windows Environment Availability**: Ensure QA has access to both Windows 10 and Windows 11 VMs for Issue #18 testing.
- **Documentation Load**: Epic 4 requires significant documentation updates; allocate technical writer bandwidth early.

---

## 📎 **Attachments & References**

- `Development_Plan_v2.md` (version 4.5 – Epic 4 section)
- `Issue_16_Core_Service_API.md`
- `Issue_17_WebSocket_Telemetry.md`
- `Issue_18_Windows_Service_Packaging.md`
- `RISK_MANAGER_GUIDE.md` (for dependency context)

---

**Prepared by**: AI Development Assistant  
**Date**: November 11, 2025  
**Status**: Planning complete – execution pending kickoff

---

