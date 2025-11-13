# Issue #19: Desktop UI Foundation

**Status**: 📋 **PLANNED**  
**Assigned**: AI Assistant  
**Created**: November 13, 2025  
**Started**: Not Started  
**Completed**: Not Completed  
**Duration**: ~3 days (estimated)  
**Epic**: Epic 5 (Desktop UI Application)  
**Priority**: P0 (Critical – unlocks remaining Epic 5 workstreams)  
**Dependencies**: Issue #16 ✅, Issue #17 ✅, Issue #18 ✅  
**Final Commit**: `TBD`

> **NOTE**: Establishes the JavaFX/TornadoFX MVVM scaffold, navigation shell, and shared UI components so the remaining Epic 5 views can plug into a consistent desktop framework.

---

## 📋 **Objective**

Create the core JavaFX/TornadoFX infrastructure (MVVM layers, navigation, shared components, styling, and data binding hooks) required for the Desktop UI application.

---

## 🎯 **Goals**

1. **Project Scaffold**: Confirm JavaFX module structure, build configuration, and runtime wiring for Windows/Mac (future).
2. **Architecture**: Implement MVVM base classes (views, view models, controllers) with navigation and dependency injection hooks.
3. **Shared UI Toolkit**: Deliver reusable controls, styling (theme), and layout conventions to accelerate subsequent Epic 5 issues.
4. **Integration Readiness**: Ensure core service telemetry and REST endpoints can be injected/bound once feature views are implemented.

---

## 📝 **Task Breakdown**

### **Task 1: Project & Build Configuration** [Status: ⏳ PENDING]
- [ ] Verify `:desktop-ui` Gradle configuration for JavaFX/TornadoFX (JDK 17, JavaFX 21 modules).
- [ ] Add platform-specific run configurations (Windows/macOS launch scripts as placeholders).
- [ ] Confirm packaging plugin setup (if required) and align with `Development_Workflow.md` build steps.

### **Task 2: MVVM Infrastructure** [Status: ⏳ PENDING]
- [ ] Define base `ViewModel`, `View`, and `Controller` abstractions (TornadoFX) including lifecycle hooks.
- [ ] Implement dependency injection wiring (Koin module or equivalent) for UI components.
- [ ] Add navigation service (stacked views, breadcrumbs, route registry).
- [ ] Document MVVM conventions in code (KDoc) and quick-start notes.

### **Task 3: Shared Components & Styling** [Status: ⏳ PENDING]
- [ ] Create reusable UI controls (e.g., status badge, metric tile, toolbar button).
- [ ] Implement global stylesheet (colors, typography, spacing) with light/dark theme placeholders.
- [ ] Add layout scaffolds (main shell, content panes, dialogs) with responsive behavior guidelines.
- [ ] Wire localization/internationalization hooks (optional placeholder for future Epic 6 polish).

### **Task 4: Integration Hooks & Telemetry Binding** [Status: ⏳ PENDING]
- [ ] Stub service interfaces for REST (`CoreServiceClient`) and WebSocket (`TelemetryClient`).
- [ ] Provide base view model utilities for coroutine scope, error handling, and toast/notification events.
- [ ] Add sample binding (e.g., fake trader list) to validate data flow from view model to UI components.

### **Task 5: Testing & Quality Gates** [Status: ⏳ PENDING]
- [ ] Unit tests for navigation service, theming utility, and base view model behaviors (`desktop-ui` module).
- [ ] Smoke test via `./gradlew :desktop-ui:run` (manual verification) to confirm shell boots without runtime errors.
- [ ] Automated checks: `./gradlew clean build --no-daemon`.

### **Task 6: Documentation & Workflow Updates** [Status: ⏳ PENDING]
- [ ] Update `Development_Plan_v2.md` (Epic 5 progress) and create any necessary design notes.
- [ ] Capture setup notes in `Development_Handbook/AI_TRADER_UI_GUIDE.md` (new) or existing handbook if applicable.
- [ ] Follow `DEVELOPMENT_WORKFLOW.md` Steps 1–7 (plan → implement → test → commit → CI → docs).

### **Task 7: Build & Commit** [Status: ⏳ PENDING]
- [ ] Run `./gradlew clean build --no-daemon` (all modules).
- [ ] Fix any compilation/test issues before committing.
- [ ] Commit with descriptive message (e.g., `feat(ui): bootstrap desktop ui foundation (Issue #19)`).
- [ ] Push to GitHub and monitor CI via `check-ci-status.ps1 -Watch -WaitSeconds 20`.

---

## 📦 **Deliverables**

### **New/Updated Source**
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/App.kt` (entry point & navigation shell)
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/navigation/NavigationService.kt`
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/mvvm/BaseViewModel.kt`
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/components/*` (shared controls)
- `desktop-ui/src/main/resources/style/theme.css`

### **Tests**
- `desktop-ui/src/test/kotlin/com/fmps/autotrader/desktop/navigation/NavigationServiceTest.kt`
- `desktop-ui/src/test/kotlin/com/fmps/autotrader/desktop/mvvm/BaseViewModelTest.kt`

### **Documentation**
- Updates to `Development_Plan_v2.md` (Epic 5 progress)
- New/updated UI developer notes in `Development_Handbook` (file TBD)

---

## 🔍 **Testing & Verification**

- `./gradlew :desktop-ui:clean build --no-daemon`
- `./gradlew clean build --no-daemon`
- Manual launch: `./gradlew :desktop-ui:run` (ensure application shell opens without errors)
- Verify CI success via GitHub Actions run (tracked in issue log)

---

## 🎯 **Success Criteria**

| Criterion | Status | Verification Method |
|-----------|--------|---------------------|
| JavaFX/TornadoFX scaffold compiles and launches | ⏳ | `./gradlew :desktop-ui:run` smoke test |
| MVVM base classes & DI wiring implemented with documentation | ⏳ | Code review + KDoc rendered |
| Navigation service handles view registration and switching | ⏳ | Unit tests + manual demo |
| Shared components and theme available to downstream views | ⏳ | Component showcase screen / Storybook stub |
| Build and CI pipeline remain green | ⏳ | `./gradlew clean build --no-daemon` + GitHub Actions |

---

## 📈 **Definition of Done**

- [ ] Tasks 1–7 completed with all subtasks checked.
- [ ] All automated tests and manual smoke tests passing.
- [ ] Documentation updated (plan, handbook, issue file).
- [ ] CI pipeline green for final commit.
- [ ] Epic 5 progress reflected in `Development_Plan_v2.md` and `EPIC_5_STATUS.md` (once created).

---

## 💡 **Notes & Risks**

- Coordinate with Core Service team to confirm WebSocket security keys for local development (reuse Issue #17 patterns).
- Ensure theme/styling aligns with branding guidelines (if updated) to avoid rework in Epic 6 polish.
- Consider scaffolding UI showcase screen (optional) to accelerate component validation.
