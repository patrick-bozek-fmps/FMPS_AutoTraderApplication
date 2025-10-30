# Issue #[NUMBER]: [Issue Title - Brief Description]

**Status**: [Choose one: 📋 **PLANNED**, 🏗️ **IN PROGRESS**, ✅ **COMPLETED**, ⚠️ **BLOCKED**, ❌ **CANCELLED**]  
**Assigned**: [Team Member Name or TBD]  
**Created**: [Creation Date - YYYY-MM-DD format]  
**Started**: [Start Date or "Not Started"]  
**Completed**: [Completion Date or "Not Completed"]  
**Duration**: [Actual time (e.g., ~2 hours) or estimated time (e.g., ~3 days estimated)]  
**Epic**: [Epic Name (Epic N)]  
**Priority**: [P0 (Critical), P1 (High), P2 (Medium), P3 (Low)]  
**Dependencies**: [List prerequisite issues with checkmarks - e.g., Issue #X (Description) ✅, Issue #Y (Description) ⏳, or "None"]

> **NOTE**: [Optional: Add any important notes about the issue status, blockers, or special considerations]

---

## 📋 **Objective**

[Provide a clear, concise statement of what this issue aims to achieve. This should be 1-2 sentences describing the primary goal.]

---

## 🎯 **Goals**

1. **[Primary Goal 1]**: [Detailed description]
2. **[Primary Goal 2]**: [Detailed description]
3. **[Primary Goal 3]**: [Detailed description]
4. **[Additional goals as needed]**

---

## 📝 **Task Breakdown**

### **Task 1: [Task Name]** [Status: ⏳ PENDING / 🏗️ IN PROGRESS / ✅ COMPLETE]
- [ ] Subtask 1.1: [Specific action item]
- [ ] Subtask 1.2: [Specific action item]
- [ ] Subtask 1.3: [Specific action item]
- [ ] [Add more subtasks as needed]

### **Task 2: [Task Name]** [Status: ⏳ PENDING / 🏗️ IN PROGRESS / ✅ COMPLETE]
- [ ] Subtask 2.1: [Specific action item]
- [ ] Subtask 2.2: [Specific action item]
  - [ ] Sub-subtask 2.2.1: [Nested action if needed]
  - [ ] Sub-subtask 2.2.2: [Nested action if needed]
- [ ] Subtask 2.3: [Specific action item]

### **Task 3: [Task Name]** [Status: ⏳ PENDING / 🏗️ IN PROGRESS / ✅ COMPLETE]
- [ ] Subtask 3.1: [Specific action item]
- [ ] Subtask 3.2: [Specific action item]

### **Task 4: Testing** [Status: ⏳ PENDING / 🏗️ IN PROGRESS / ✅ COMPLETE]
- [ ] Write unit tests for [component]
- [ ] Write integration tests for [component]
- [ ] Manual testing: [specific scenarios]
- [ ] Verify all tests pass: `./gradlew test`
- [ ] Code coverage meets targets (>80%)

### **Task 5: Documentation** [Status: ⏳ PENDING / 🏗️ IN PROGRESS / ✅ COMPLETE]
- [ ] Update relevant documentation files
- [ ] Add KDoc/comments to new code
- [ ] Create/update user guide if applicable
- [ ] Document any configuration changes
- [ ] Add troubleshooting section if needed

### **Task 6: Build & Commit** [Status: ⏳ PENDING / 🏗️ IN PROGRESS / ✅ COMPLETE]
- [ ] Run all tests: `./gradlew test`
- [ ] Build project: `./gradlew build`
- [ ] Fix any compilation errors
- [ ] Fix any test failures
- [ ] Commit changes with descriptive message
- [ ] Push to GitHub
- [ ] Verify CI pipeline passes
- [ ] Update this Issue file to reflect completion
- [ ] Update Development_Plan_v2.md

---

## 📦 **Deliverables**

### **New Files**
1. ✅ `[path/to/file1.kt]` - [Brief description]
2. ✅ `[path/to/file2.kt]` - [Brief description]
3. ✅ `[path/to/file3.xml]` - [Brief description]

### **Updated Files**
- `[path/to/existing/file1.kt]` - [What was changed]
- `[path/to/existing/file2.gradle.kts]` - [What was changed]

### **Test Files**
1. ✅ `[path/to/test1.kt]` - [What it tests]
2. ✅ `[path/to/test2.kt]` - [What it tests]

### **Documentation**
- `[DocumentationFile.md]` - [Brief description]

---

## 🎯 **Success Criteria**

| Criterion | Status | Verification Method |
|-----------|--------|---------------------|
| [Criterion 1] | [✅/⏳/❌] | [How to verify - e.g., file exists, tests pass, manual check] |
| [Criterion 2] | [✅/⏳/❌] | [How to verify] |
| [Criterion 3] | [✅/⏳/❌] | [How to verify] |
| All tests pass | [✅/⏳/❌] | `./gradlew test` |
| Build succeeds | [✅/⏳/❌] | `./gradlew build` |
| CI pipeline passes | [✅/⏳/❌] | GitHub Actions green checkmark |
| Documentation complete | [✅/⏳/❌] | Documentation review |
| Code review completed | [✅/⏳/❌] | Review approval |

---

## 📊 **Test Coverage Approach** (Optional)

[Document the testing strategy for this issue. Include this section if the issue involves significant new code that requires testing.]

### **What Was Tested**
✅ **Component-Level Unit Tests**:
- **[Component 1]**: [N tests] ([what they test - e.g., validation, state management, business logic])
- **[Component 2]**: [N tests] ([what they test])
- **[Component 3]**: [N tests] ([what they test])

**Total**: [N] tests ✅

✅ **Integration Tests** (if applicable):
[Describe integration test scenarios - e.g., "11 scenarios with real API", "Database integration tests", etc.]
1. [Integration test scenario 1]
2. [Integration test scenario 2]
3. [Integration test scenario 3]

### **What Was NOT Unit Tested (By Design)** (Optional)
❌ **[Category of untested code]**:
- `[method/class]` - [Why not unit tested - e.g., "Tested via integration tests", "Abstract class tested via concrete implementations"]

**Rationale**: 
- [Explain why certain code wasn't unit tested]
- [Explain alternative testing approach used]
- [Justify the testing strategy]

### **Test Strategy**
**[N]-tier coverage for production confidence**:
1. **Unit Tests**: [What's covered at unit level] ✅
2. **Integration Tests**: [What's covered at integration level] ✅
3. **[Other test level]**: [Additional test coverage] ✅

**Result**: ✅ [Summary of overall coverage - e.g., "All functionality covered through strategic test layering"]

---

## 🔧 **Key Technologies**

| Technology | Version | Purpose |
|------------|---------|---------|
| [Technology 1] | [Version] | [What it's used for] |
| [Technology 2] | [Version] | [What it's used for] |
| [Technology 3] | [Version] | [What it's used for] |

**Add to `build.gradle.kts`** (if applicable):
```kotlin
dependencies {
    // [Dependency Category]
    implementation("[group]:[artifact]:[version]")
    implementation("[group]:[artifact]:[version]")
    
    // Testing
    testImplementation("[group]:[artifact]:[version]")
}
```

---

## 📊 **Architecture/Design** (Optional)

[Include diagrams, flowcharts, or architectural descriptions if relevant]

```
┌─────────────────────────────────────────────┐
│         [Component/Layer Name]              │
│  ┌────────────┐  ┌────────────┐            │
│  │ Module A   │  │ Module B   │            │
│  └────────────┘  └────────────┘            │
│         │                │                  │
│         └────────────────┘                  │
│                  │                          │
│                  ▼                          │
│         ┌────────────────┐                  │
│         │   [Component]  │                  │
│         └────────────────┘                  │
└─────────────────────────────────────────────┘
```

---

## ⏱️ **Estimated Timeline**

| Task | Estimated Time |
|------|---------------|
| Task 1: [Name] | [X hours] |
| Task 2: [Name] | [X hours] |
| Task 3: [Name] | [X hours] |
| Task 4: Testing | [X hours] |
| Task 5: Documentation | [X hours] |
| Task 6: Build & Commit | [X hours] |
| **Total** | **~[X days/hours]** |

---

## 🔄 **Dependencies**

### **Depends On** (Must be complete first)
- [Status] Issue #X: [Description]
- [Status] Issue #Y: [Description]

### **Blocks** (Cannot start until this is done)
- Issue #Z: [Description]
- Epic N: [Description]

### **Related** (Related but not blocking)
- Issue #W: [Description]
- Issue #V: [Description]

---

## 📚 **Resources**

### **Documentation**
- [Resource Name]: [URL]
- [Resource Name]: [URL]

### **Examples**
- [Example Name]: [URL]
- [Example Name]: [URL]

### **Reference Issues**
- Issue #X: [Brief description of relevance]

---

## ⚠️ **Risks & Considerations**

| Risk | Impact | Mitigation Strategy |
|------|--------|---------------------|
| [Risk 1] | [High/Medium/Low] | [How to mitigate] |
| [Risk 2] | [High/Medium/Low] | [How to mitigate] |
| [Risk 3] | [High/Medium/Low] | [How to mitigate] |

---

## 📈 **Definition of Done**

- [ ] All tasks completed
- [ ] All subtasks checked off
- [ ] All deliverables created/updated
- [ ] All success criteria met
- [ ] All tests written and passing
- [ ] Code coverage meets targets
- [ ] Documentation complete
- [ ] Code review completed
- [ ] All tests pass: `./gradlew test`
- [ ] Build succeeds: `./gradlew build`
- [ ] CI pipeline passes (GitHub Actions)
- [ ] Issue file updated to reflect completion
- [ ] Development_Plan_v2.md updated with progress
- [ ] Changes committed to Git
- [ ] Changes pushed to GitHub
- [ ] Issue closed

---

## 💡 **Notes & Learnings** (Optional)

[Add any notes, lessons learned, or important observations during implementation]

- [Note 1]
- [Note 2]
- [Note 3]

---

## 📦 **Commit Strategy**

Follow the development workflow from `DEVELOPMENT_WORKFLOW.md`:

1. **Commit after major tasks** (not after every small change)
2. **Run tests before every commit**
3. **Wait for CI to pass** before proceeding
4. **Update documentation** as you go

Example commits:
```
feat: [Brief description of feature] (Issue #N Task 1-2)
feat: [Brief description of feature] (Issue #N Task 3-4)
test: Add unit tests for [component] (Issue #N Task 4)
docs: Update [documentation] (Issue #N Task 5)
feat: Complete Issue #N - [Issue Title]
```

---

**Issue Created**: [Date]  
**Priority**: [P0/P1/P2/P3]  
**Estimated Effort**: [X days/hours]  
**Status**: [Current Status]

---

**Next Steps**:
1. Review this plan with team/stakeholder
2. Begin Task 1: [First task name]
3. Follow DEVELOPMENT_WORKFLOW.md throughout
4. Update status as progress is made

---

## 📝 **How to Use This Template**

1. **Copy this template** to a new file named `Issue_[NUMBER]_[Brief_Name].md`
2. **Replace all placeholders** in square brackets [like this] with actual content
3. **Remove optional sections** if not needed for your issue
4. **Add additional sections** as needed for your specific issue
5. **Update task checkboxes** as work progresses
6. **Mark sections as complete** by adding ✅ next to task headings
7. **Update the header** when status changes (PLANNED → IN PROGRESS → COMPLETED)
8. **Add actual dates** when tasks start and complete
9. **Update duration** with actual time once work is complete
10. **Keep synchronized** with Development_Plan_v2.md

### **Status Icons Reference**
- 📋 PLANNED - Not yet started, planning complete
- 🏗️ IN PROGRESS - Actively being worked on
- ✅ COMPLETED - All work done and verified
- ⚠️ BLOCKED - Cannot proceed due to external factors
- ⏳ PENDING - Waiting on dependencies
- ❌ CANCELLED - No longer needed

### **Priority Levels**
- **P0 (Critical)**: Blocking other work, must be done immediately
- **P1 (High)**: Important for current epic, should be done soon
- **P2 (Medium)**: Nice to have, can be scheduled flexibly
- **P3 (Low)**: Optional enhancement, can be deferred

---

**Template Version**: 1.0  
**Last Updated**: October 28, 2025  
**Maintained By**: AI Assistant

