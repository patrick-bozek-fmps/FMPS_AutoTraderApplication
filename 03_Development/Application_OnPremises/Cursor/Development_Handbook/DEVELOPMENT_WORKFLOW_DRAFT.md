# Development Workflow (Draft v2)

**Status**: Draft for team review  
**Prepared by**: AI Process Engineer  
**Date**: November 11, 2025

This draft streamlines the current workflow, merges redundant diagrams, and embeds links to the supporting templates, scripts, and best-practice guides. It is intended to replace `DEVELOPMENT_WORKFLOW.md` once reviewed and approved.

---

## 🎯 Guiding Principles

- Keep `main` deployable at all times (respect CI gates and local test requirements).
- Document decisions where they happen – issue plans, epic plans, and reviews remain the system of record.
- Prefer small, traceable increments: one issue/bug/feature per commit sequence.
- Automate whenever possible using the scripts in `Cursor/Artifacts/`.
- Follow the project’s best practices (`Development_Handbook/`), surfacing deviations during reviews.

---

## 🛠️ Unified Workflow (New & Existing Workstreams)

```
STEP 1 – INTAKE & PLANNING
  Work Request / Backlog Item
    ├─ Yes → New item
    │          └→ Collect / convert requirements (convert_excel.bat, link dependencies)
    │              └→ Draft or update plan (ISSUE_TEMPLATE.md / EPIC_STATUS_TEMPLATE.md)
    └─ No  → Existing item
               └→ Review current plans & review findings
                   └→ Confirm scope & gaps (update plan, record new findings)

STEP 2 – READINESS CHECK
  Decision: Ready to implement?
    ├─ Yes → proceed to Step 3
    └─ No  → Resolve blockers (dependencies, approvals, missing inputs)
                 └→ reassess readiness (loop until “Yes”)

STEP 3 – LOCAL VERIFICATION
  Implement changes (follow guides, keep commits focused)
  Run local tests & static checks (`./gradlew clean test`, TESTING_GUIDE.md, ktlint)
    ├─ Pass → Step 4
    └─ Fail → Fix locally, rerun tests → re-enter Step 3

STEP 4 – COMMIT
  Record work (git add/commit, conventional message), push to remote

STEP 5 – CI VALIDATION
  Monitor CI (check-ci-status.ps1, check-ci-annotations.ps1)
    ├─ Pass → Step 6
    └─ Fail → Investigate & fix (analyze-ci-failures.ps1, diagnose.bat)
                 └→ return to Step 3 for re-test, then repeat Steps 4-5

STEP 6 – DOCUMENTATION & REVIEWS
  Update Issue/Epic plans, Development_Plan_v2.md, review templates

STEP 7 – HANDOFF & LEARNINGS
  Notify stakeholders, capture lessons & follow-up tasks → Ready for next item
```

---

## 🧭 Step-by-Step Playbook

### 1. Intake & Planning

| Scenario | Required Actions | References |
|----------|------------------|------------|
| **New Epic / Issue** | Convert Excel sources to Markdown (run [`convert_excel.bat`](../Artifacts/convert_excel.bat)), draft plan using [`ISSUE_TEMPLATE.md`](ISSUE_TEMPLATE.md) or [`EPIC_STATUS_TEMPLATE.md`](EPIC_STATUS_TEMPLATE.md), identify dependencies, success criteria, and deliverables. | `02_ReqMgn/*.md`, [`Development_Plan_v2.md`](../Development_Plan/Development_Plan_v2.md) |
| **Existing Item** | Re-read current issue/epic plan, confirm open findings (`Review Documentation/`), sync with stakeholders, and update scope if new work is required. | `Cursor/Development_Plan/Issue_*`, `Review Documentation/*` |

**Best practices**: capture assumptions explicitly, tag blocked tasks, and note required approvals.

### 2. Design & Preparation

- Review architecture, configuration, and module guidelines:
  - [`CONFIG_GUIDE.md`](CONFIG_GUIDE.md)
  - [`TESTING_GUIDE.md`](TESTING_GUIDE.md)
  - Module-specific guides (`AI_TRADER_CORE_GUIDE.md`, `POSITION_MANAGER_GUIDE.md`, etc.).
- Produce technical notes or diagrams if changes are non-trivial; store them alongside the issue plan.
- Ensure environment prerequisites are met (`pip install -r Cursor/Artifacts/requirements.txt`, secrets, API keys).

### 3. Implementation Sprint

- Work in focused branches when collaborating; otherwise follow the mainline policy described in [`DEVELOPMENT_WORKFLOW.md`](DEVELOPMENT_WORKFLOW.md) until this draft is approved.
- Follow coding standards (see `Development_Handbook/` best practice documents).
- Keep commits atomic and descriptive (use `feat:`, `fix:`, `docs:` etc.).

### 4. Local Verification

- Run local tests before every commit:
  - `./gradlew clean test --no-daemon` (unit + integration separation described in [`TESTING_GUIDE.md`](TESTING_GUIDE.md)).
  - Optional targeted suites using `gradlew test --tests "*ClassName*"`.
- Static checks / formatting as required (ktlint, detekt, etc.).
- Capture evidence (test counts, coverage deltas) for documentation updates.

### 5. CI & Delivery

- Push changes and monitor CI with [`check-ci-status.ps1`](../Artifacts/check-ci-status.ps1).
- If CI fails:
  1. Inspect logs or run [`analyze-ci-failures.ps1`](../Artifacts/analyze-ci-failures.ps1).
  2. Replicate locally; adjust code/tests.
  3. Document the failure and resolution in the issue plan (Post-Completion Fixes if applicable).
- For long-running investigations use [`diagnose.bat`](../Artifacts/diagnose.bat) and capture findings.
- Await a green pipeline before moving to documentation/handoff.

### 6. Documentation & Review

- Update the following once CI passes:
  - Issue/Epic documents (`Issue_XX_*.md`, `EPIC_X_STATUS.md`) with task status, test counts, and final commit hash.
  - `Development_Plan_v2.md` and epic status tables.
  - Review artifacts using [`ISSUE_REVIEW_TEMPLATE.md`](ISSUE_REVIEW_TEMPLATE.md) or [`EPIC_REVIEW_TEMPLATE.md`](EPIC_REVIEW_TEMPLATE.md).
- Move completed reviews into `Development_Plan/Review Documentation/`.
- Record lessons learned and follow-up actions.

### 7. Handoff & Continuous Improvement

- Notify stakeholders or downstream teams when work is ready.
- Create follow-up tickets for deferred tasks or risks.
- Update best-practice guides if new lessons emerge.

---

## 📌 Quick Reference

| Task | Tool / Script | Notes |
|------|---------------|-------|
| Requirements conversion | [`convert_excel.bat`](../Artifacts/convert_excel.bat) | Converts `02_ReqMgn` Excel sheets to Markdown. |
| CI monitoring | [`check-ci-status.ps1`](../Artifacts/check-ci-status.ps1) | `-Watch -WaitSeconds 20` for live tracking. |
| CI annotations | [`check-ci-annotations.ps1`](../Artifacts/check-ci-annotations.ps1) | Review warnings/errors per run. |
| Failure triage | [`analyze-ci-failures.ps1`](../Artifacts/analyze-ci-failures.ps1) | Summarises common failure signatures. |
| Environment diagnostics | [`diagnose.bat`](../Artifacts/diagnose.bat) | Gathers system state for support tickets. |

---

## ✅ Best Practices Checklist (Use per Issue)

- [ ] Plan updated with dependencies, success criteria, and owners.
- [ ] Code follows module guides and logging standards.
- [ ] All local tests run (record counts per module).
- [ ] CI green (capture run ID in documentation).
- [ ] Issue/Epic/Plan documents updated with final commit hash.
- [ ] Review records created/updated (Issue/Epic review templates).
- [ ] Lessons learned captured; follow-up actions assigned.

---

## 📚 Related Documents

- [`DEVELOPMENT_WORKFLOW.md`](DEVELOPMENT_WORKFLOW.md) – current published workflow (to be superseded).
- [`Development_Plan_v2.md`](../Development_Plan/Development_Plan_v2.md) – master project plan.
- [`TESTING_GUIDE.md`](TESTING_GUIDE.md) – detailed testing strategy.
- [`CONFIG_GUIDE.md`](CONFIG_GUIDE.md) – configuration architecture and best practices.
- Module guides in `Development_Handbook/` (AI Trader Core, Manager, Risk, Pattern Storage, etc.).
- Review templates (`ISSUE_REVIEW_TEMPLATE.md`, `EPIC_REVIEW_TEMPLATE.md`).

---

## ✍️ Feedback & Next Steps

Please add comments directly in this draft or open a review issue if broader discussion is needed. Once approved, we will:

1. Replace the existing `DEVELOPMENT_WORKFLOW.md` with this structure.
2. Update cross-references across the handbook and plan documents.
3. Schedule onboarding sessions to walk the team through the revised workflow.

--- 

