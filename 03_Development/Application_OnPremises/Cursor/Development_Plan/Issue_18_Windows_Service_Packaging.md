---
title: Issue #18: Windows Service Packaging & Deployment
---

# Issue #18: Windows Service Packaging & Deployment

**Status**: 📋 **PLANNED**  
**Assigned**: AI Assistant  
**Created**: November 11, 2025  
**Started**: Not Started  
**Completed**: Not Completed  
**Duration**: ~4 days (estimated)  
**Epic**: Epic 4 (Core Service & API)  
**Priority**: P0 (Critical – enables continuous core operation)  
**Dependencies**: Issue #16 ⏳ (API Hardening), Issue #17 ⏳ (WebSocket Telemetry), Epic 1 ✅ (Foundation)  
**Final Commit**: _Pending_

> **NOTE**: Packages the Core service as an installable Windows service (Procrun-based) with scripts, configuration templates, and an operational playbook, ensuring reliable 24/7 operation on customer environments.

---

## 📋 **Objective**

Deliver an installation bundle and operations guide for running the Core application as a Windows service, including installer scripts, configuration/logging defaults, recovery settings, and validation on Windows 10/11.

---

## 🎯 **Goals**

1. **Service Wrapper**: Bundle the core-service JAR with a Procrun (or equivalent) wrapper for install/start/stop management.
2. **Configuration & Logging**: Provide production-ready config templates, secrets handling guidance, and rotating logs.
3. **Operational Scripts**: Supply install/upgrade/uninstall scripts with validation steps.
4. **Documentation & Testing**: Document the operational playbook and verify installation on supported OS versions.

---

## 📝 **Task Breakdown**

### **Task 1: Packaging Strategy & Layout** [Status: ⏳ PENDING]
- [ ] Confirm packaging tooling (Apache Procrun) and directory structure.
- [ ] Define service name, description, start type, and required environment variables.
- [ ] Outline upgrade/rollback strategy (versioned bundles).

### **Task 2: Installer & Automation Scripts** [Status: ⏳ PENDING]
- [ ] Create `install-service.ps1/.bat` (installs service, sets configs, validates environment).
- [ ] Create `uninstall-service.ps1/.bat` and maintenance helpers (`start-service`, `stop-service`, `status`).
- [ ] Include diagnostics command for collecting logs/config snapshots.

### **Task 3: Configuration & Runtime Assets** [Status: ⏳ PENDING]
- [ ] Prepare configuration templates (`service-config.template.conf`, secrets placeholders).
- [ ] Ship logging configuration (Logback) with rotation policies.
- [ ] Document secure storage for API keys (environment vars, encrypted files).
- [ ] Ensure scripts set permissions on config/log directories.

### **Task 4: Monitoring & Recovery Setup** [Status: ⏳ PENDING]
- [ ] Configure service recovery options (restart on failure, delayed retries).
- [ ] Document health/metrics endpoints consumption for monitoring tools.
- [ ] Provide optional scheduled task template for nightly restarts (if required).

### **Task 5: Testing & Validation** [Status: ⏳ PENDING]
- [ ] Perform install/upgrade/uninstall tests on Windows 10 and Windows 11 VMs.
- [ ] Validate service auto-start on boot, graceful shutdown, and restart behaviour.
- [ ] Confirm metrics/log outputs accessible post-install.
- [ ] Run regression suite (`./gradlew clean build --no-daemon`) from packaged environment.

### **Task 6: Documentation & Playbook** [Status: ⏳ PENDING]
- [ ] Author `Development_Handbook/Windows_Service_Guide.md` (installation, operations, troubleshooting).
- [ ] Update `Development_Plan_v2.md` and `EPIC_4_STATUS.md` with progress/outcomes.
- [ ] Create release checklist for future service versions.

### **Task 7: Build & Commit** [Status: ⏳ PENDING]
- [ ] Ensure `.gitignore` handles generated installers/log directories.
- [ ] Commit packaging scripts/resources with descriptive messages.
- [ ] Push to GitHub and confirm CI (unit/integration) remains green.
- [ ] Record final commit hash in this issue file.

---

## 📦 **Deliverables**

### **Artifacts & Scripts**
- `artifacts/windows-service/install-service.ps1`
- `artifacts/windows-service/install-service.bat`
- `artifacts/windows-service/uninstall-service.ps1`
- `artifacts/windows-service/start-service.ps1` / `stop-service.ps1`
- `artifacts/windows-service/service-config.template.conf`
- `artifacts/windows-service/service-logging.xml`
- `artifacts/windows-service/env.example`
- `artifacts/windows-service/README.md`

### **Documentation**
- `Development_Handbook/Windows_Service_Guide.md`
- Updates to `Development_Plan_v2.md`, `EPIC_4_STATUS.md`
- Release checklist / operations notes

---

## 🎯 **Success Criteria**

| Criterion | Status | Verification Method |
|-----------|--------|---------------------|
| Service installs/runs on Windows 10 & 11 | ⏳ | Manual VM validation |
| Auto-start and recovery options configured | ⏳ | Service properties inspection & failure tests |
| Config/log templates deployed with correct permissions | ⏳ | Post-install inspection |
| Health/metrics accessible post-install | ⏳ | `curl http://localhost:8080/health` |
| Documentation/playbook published | ⏳ | Documentation review |
| CI pipeline green after packaging changes | ⏳ | GitHub Actions |

---

## 📊 **Test Coverage Approach**

- **Manual Testing**: Primary validation on Windows 10/11 VMs, including install/uninstall and failure scenarios.
- **Automation**: Script dry-run using PowerShell `-WhatIf` where possible; existing automated tests ensure service logic intact.
- **Regression**: Run unit/integration tests after installation to ensure no regressions introduced.

---

## 🔧 **Key Technologies**

| Technology | Version | Purpose |
|------------|---------|---------|
| Apache Commons Daemon (Procrun) | 1.3+ | Windows service wrapper |
| PowerShell 5+ | — | Installation/management scripts |
| Logback | 1.4+ | Rolling log configuration |
| Kotlin/JVM 17 | — | Core service runtime |

---

## ⏱️ **Estimated Timeline**

| Task | Estimated Time |
|------|---------------|
| Task 1: Packaging Strategy | 4 hours |
| Task 2: Installer Scripts | 10 hours |
| Task 3: Configuration Assets | 6 hours |
| Task 4: Monitoring & Recovery | 4 hours |
| Task 5: Testing & Validation | 8 hours |
| Task 6: Documentation & Playbook | 6 hours |
| Task 7: Build & Commit | 2 hours |
| **Total** | **~4 working days** |

---

## 🔄 **Dependencies**

### **Depends On**
- ⏳ Issue #16 – Core Service REST API Hardening (health/metrics endpoints finalised)
- ⏳ Issue #17 – WebSocket Telemetry (final channel behaviour for docs)
- ✅ Epic 1 foundation (build/config infrastructure)

### **Blocks**
- Epic 5 – Desktop UI validation (requires packaged core service)
- Epic 6 – Release preparation (bundling & installer)

### **Related**
- Issue #04 – Logging Infrastructure (logback configs)
- Issue #06 – Configuration Management (config schema reuse)

---

## 📚 **Resources**

- Apache Procrun documentation: https://commons.apache.org/proper/commons-daemon/procrun.html
- Microsoft Docs – Windows service recovery options
- Internal ops handbook template
- Prior script examples (if any) in `artifacts/`

---

## ⚠️ **Risks & Considerations**

| Risk | Impact | Mitigation |
|------|--------|------------|
| Service fails to start post-install | High | Provide verbose logging, validation steps, rollback script |
| Permission issues on config/log directories | Medium | Set permissions in scripts; document prerequisites |
| Upgrade conflicts / leftover files | Medium | Versioned directories and cleanup steps |
| Missing runtime dependencies | Low | Document prerequisites (PowerShell, JRE) in installer README |

---

## 📈 **Definition of Done**

- [ ] Service installs, starts, and stops cleanly on Windows 10/11
- [ ] Recovery options and monitoring documented
- [ ] Scripts idempotent and checked into repo
- [ ] Documentation/playbook complete
- [ ] Tests/regressions verified locally and in CI
- [ ] Development_Plan_v2.md & EPIC_4_STATUS.md updated
- [ ] Issue closed with final commit/reference recorded

---

## 💡 **Notes & Learnings**

- [Pending – capture during implementation]

---

**Issue Created**: November 11, 2025  
**Priority**: P0  
**Estimated Effort**: ~4 days  
**Status**: 📋 PLANNED

---

**Next Steps**:
1. Align packaging approach with Ops (confirm Procrun usage and permissions).
2. Kick off Task 1 once Issues #16–#17 near completion.
3. Schedule Windows VM availability for testing.

---

