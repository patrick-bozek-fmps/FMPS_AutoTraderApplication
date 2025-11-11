# Epic 4: Core Service & API Hardening - Status Report

**Date**: November 11, 2025  
**Epic Status**: 📋 **PLANNED** (0/3 issues complete – 0%)  
**Version**: 1.0  
**Last Updated**: November 11, 2025 (Initial planning package published)

---

## 📊 **Executive Summary**

Epic 4 moves the Core service from prototype to production readiness. The planning phase is complete: requirements, issue breakdowns, and dependencies are captured across Issues #16–#18. Work has not started yet because the team just finalized Epic 3 follow-up remediations (Issues #13–#14).

The epic focuses on three deliverables: hardening REST APIs, expanding real-time WebSocket telemetry, and packaging the Core as a Windows service. Each issue now has a dedicated plan detailing scope, testing, documentation, and CI expectations. Once execution begins, Epic 4 will unlock downstream desktop UI work (Epic 5) and the broader release pipeline (Epic 6).

**Status**: Planning ready; implementation queued behind final Epic 3 hygiene.

**Key Components**:
- Core REST API Hardening – 📋 Planned
- Real-Time WebSocket Telemetry – 📋 Planned
- Windows Service Packaging – 📋 Planned

---

## 📋 **Epic Overview**

| Issue | Title | Status | Priority | Duration (Est.) | Dependencies |
|-------|-------|--------|----------|-----------------|--------------|
| #16 | Core Service REST API Hardening | 📋 Planned | P0 | ~3 days | Issue #11 ✅, #13 ✅, #14 ✅ |
| #17 | Real-Time WebSocket Telemetry | 📋 Planned | P1 | ~2.5 days | Issue #07 ✅, #13 ✅, #16 ⏳ |
| #18 | Windows Service Packaging & Deployment | 📋 Planned | P0 | ~4 days | Issue #16 ⏳, #17 ⏳ |

**Total Estimated Duration**: ~9.5 working days (≈2 weeks)  
**Actual Duration**: Not started  
**Current Progress**: 0/3 issues complete (0%)

---

## 🎯 **Epic Goals**

1. **Harden Core REST APIs**: Add authentication, validation, pagination, and operational telemetry required for on-prem deployments.
2. **Deliver reliable WebSocket telemetry**: Provide authenticated, resilient, multi-channel real-time feeds for the desktop UI.
3. **Package the Core as a Windows service**: Ship scripts, configuration, and operations playbook for 24/7 service management.
4. **Document production runbooks**: Update API/WebSocket references and create a Windows Service guide.

---

## ✅ **Completed Issues** (0/3 – None yet)

_None – execution begins after risk-management remediation sign-off._

---

## ⏳ **In Progress Issues** (0/3)

None – all workstreams currently scheduled.

---

## 📋 **Planned Issues** (3/3)

### **Issue #16: Core Service REST API Hardening** 📋 PLANNED
- **Priority**: P0  
- **Estimated Duration**: ~3 days  
- **Dependencies**: Issue #11 ✅, #13 ✅, #14 ✅  

**Planned Deliverables**:
- [ ] API key authentication middleware and configuration
- [ ] Pagination and validation enhancements across REST routes
- [ ] Health/metrics endpoints with Prometheus-compatible outputs
- [ ] Updated API reference and troubleshooting guide

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
| REST API | Auth middleware, validation/pagination, health metrics, error envelopes | Derived from Issue #16 plan |
| WebSocket | Multi-channel feeds, heartbeats, metrics, replay snapshots | Requires auth pipeline from Issue #16 |
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
1. ⏳ Issue #16 – REST API Hardening (must complete before WebSocket work)
2. ⏳ Issue #17 – WebSocket Telemetry (unblocks UI real-time features)
3. ⏳ Issue #18 – Windows Service Packaging (required for integrated testing)

**Result**: Epic 5 remains blocked until Epic 4 execution is underway.

---

## 📋 **Recommended Next Steps**

### **Current Epic Status**

1. 📋 Issue #16 – Core Service REST API Hardening – Planned (ready to start)
2. 📋 Issue #17 – Real-Time WebSocket Telemetry – Planned (awaits Issue #16)
3. 📋 Issue #18 – Windows Service Packaging & Deployment – Planned (awaits Issues #16–#17)

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
| **Planning Quality** | ✅ | Issue breakdowns provide clear scope, deliverables, and testing strategy |
| **Dependencies** | ⏳ | Awaiting final confirmation of API auth approach across services |
| **Blockers** | ✅ | No external blockers; resource availability will determine start date |
| **Documentation** | ✅ | Planning docs (Issue files, Development Plan v2, this status sheet) synchronized |
| **Risk Assessment** | ⏳ | Execution risks (auth regressions, packaging edge cases) tracked per issue |

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

