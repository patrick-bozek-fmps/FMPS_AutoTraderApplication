# Development Workflow & Best Practices

**Version**: 1.0  
**Date**: October 24, 2025  
**Status**: Active  
**Purpose**: Standard workflow for all development tasks to ensure quality and avoid breaking changes

---

## 🎯 **Core Principle**

**Never push code that breaks CI/CD pipeline**

All code must pass local tests AND GitHub Actions CI before proceeding to the next task. This ensures the main branch is always in a deployable state.

---

## 📋 **Standard Workflow for Each Issue**

```
┌─────────────────────────────────────────────────────────┐
│  Step 1: Plan & Document                                │
│  • Create Issue_XX plan document                        │
│  • Review and approve plan                              │
│  • Commit plan to repository                            │
└────────────────────┬────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Step 2: Implement                                       │
│  • Write code following plan phases                      │
│  • Add comprehensive comments                            │
│  • Follow coding standards                               │
└────────────────────┬────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Step 3: Test Locally (MANDATORY)                       │
│  • Run: ./gradlew clean test --no-daemon                │
│  • Verify: ALL tests pass ✅                            │
│  • Fix: Any failures immediately                         │
│  • Repeat until 100% pass rate                          │
└────────────────────┬────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Step 4: Commit & Push                                   │
│  • git add -A                                            │
│  • git commit -m "descriptive message"                   │
│  • git push origin main                                  │
└────────────────────┬────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Step 5: Verify CI Passes (MANDATORY) ⏱️ AUTOMATED     │
│  • Run: .\Cursor\check-ci-status.ps1 -Watch -Wait 20    │
│  • Script automatically monitors GitHub Actions          │
│  • Wait for: [SUCCESS] message                          │
│  • Verify: Green checkmark ✅                           │
│  • DO NOT PROCEED until CI passes                       │
│  • Alternative: Manual check at github.com/.../actions   │
└────────────────────┬────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Step 6: Update Documentation                            │
│  • Update Issue_XX document with:                        │
│    - Mark tasks as complete                              │
│    - Add final commit SHA                                │
│    - Document any issues/fixes                           │
│    - Update completion date                              │
│  • Update Development_Plan_v2.md progress                │
│  • Commit documentation updates                          │
└────────────────────┬────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Step 7: Move to Next Issue                              │
│  • Create next Issue_XX plan                             │
│  • Repeat from Step 1                                    │
└─────────────────────────────────────────────────────────┘
```

---

## 🚨 **If CI Fails**

```
┌─────────────────────────────────────────────────────────┐
│  CI FAILURE DETECTED ❌                                 │
└────────────────────┬────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────┐
│  STOP ALL OTHER WORK IMMEDIATELY                         │
│  Priority: Fix the failing build                         │
└────────────────────┬────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Analyze Failure                                         │
│  • Read CI logs carefully                                │
│  • Identify root cause                                   │
│  • Check if reproducible locally                         │
└────────────────────┬────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Fix Issue                                               │
│  • Make minimal targeted fix                             │
│  • Test locally until passes                             │
│  • Commit with clear "fix:" message                      │
│  • Push to main                                          │
└────────────────────┬────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Wait for CI Again ⏱️                                   │
│  • Verify fix resolved the issue                         │
│  • If still failing, repeat                              │
│  • Only proceed when ✅ green                           │
└────────────────────┬────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Document the Fix                                        │
│  • Update issue document with bugfix details             │
│  • Add to "Known Issues" section if applicable           │
│  • Note fix in commit history                            │
└────────────────────┬────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────┐
│  Resume Normal Workflow                                  │
└─────────────────────────────────────────────────────────┘
```

---

## ✅ **Pre-Commit Checklist**

Before every `git commit`, verify:

- [ ] All new code has been written
- [ ] Code compiles without errors
- [ ] All tests written for new functionality
- [ ] Local tests run: `./gradlew test`
- [ ] All tests pass (100%)
- [ ] No unused imports or variables
- [ ] Code formatted properly
- [ ] Comments added for complex logic
- [ ] No debug statements left in code

---

## 🔄 **Testing Strategy**

### **Local Testing (REQUIRED before push)**
```bash
# Navigate to project directory
cd 03_Development/Application_OnPremises

# Run all tests
./gradlew clean test --no-daemon

# Expected output:
# BUILD SUCCESSFUL
# XX tests completed, XX passed
```

### **What to Do if Local Tests Fail**
1. Read the failure message carefully
2. Fix the failing test or code
3. Run tests again
4. Repeat until all pass
5. **NEVER push failing tests**

### **CI Testing (REQUIRED after push) - AUTOMATED** ✨

#### **Option 1: Automated Checking (RECOMMENDED)**
```powershell
# Navigate to project root
cd C:\PABLO\AI_Projects\FMPS_AutoTraderApplication

# One-time check
.\03_Development\Application_OnPremises\Cursor\check-ci-status.ps1

# Watch mode (auto-checks every 20 seconds until complete)
.\03_Development\Application_OnPremises\Cursor\check-ci-status.ps1 -Watch -WaitSeconds 20
```

**Output**:
- Shows 5 most recent workflow runs
- Displays status: `in_progress`, `completed`
- Shows conclusion: `success`, `failure`
- Provides commit SHA, timestamps, run URL
- Exit codes: 0=success, 1=failure, 3=in progress

**Wait for**:
```
[SUCCESS] Latest CI run passed!
You can proceed with the next step.
```

#### **Option 2: Manual Checking**
1. Navigate to: https://github.com/patrick-bozek-fmps/FMPS_AutoTraderApplication/actions
2. Find your commit in the list
3. Wait for workflow to complete (2-3 minutes)
4. Verify green checkmark ✅
5. If red ❌, follow "If CI Fails" workflow above

### **Build Warnings Detection** ⚠️

After running tests, check for warnings:
```powershell
# Run build and capture warnings
.\gradlew build --no-daemon 2>&1 | Select-String -Pattern "^w:" -Context 0,0

# Or run tests with warning capture
.\gradlew test --no-daemon 2>&1 | Select-String -Pattern "^w:" -Context 0,0

# Save warnings to file for review
.\gradlew build --no-daemon 2>&1 | Select-String -Pattern "^w:" > warnings.txt
```

**Common Warnings to Fix**:
- Unused variables
- Deprecated API usage
- Unchecked casts
- Missing documentation

**Policy**: Fix all warnings before pushing (target: 0 warnings)

---

## 📝 **Commit Message Guidelines**

### **Format**
```
<type>: <subject>

<body>

<footer>
```

### **Types**
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation only
- `test:` Adding or fixing tests
- `refactor:` Code change that neither fixes a bug nor adds a feature
- `style:` Formatting changes (no code change)
- `chore:` Maintenance tasks

### **Examples**

**Good:**
```
feat: Implement database layer with Exposed ORM (Issue #2)

- Created 5 database tables with Exposed DSL
- Implemented 3 repositories with CRUD operations
- Added 24 unit tests (all passing)
- Configured Flyway migrations

All tests passing ✅
```

**Good:**
```
fix: Resolve TradeRepositoryTest CI failure

- Fixed NullPointerException in test setup
- Added proper directory creation
- Use requireNotNull() instead of !!
- Removed unused variables

All 24 tests now pass ✅
```

**Bad:**
```
updated stuff
```

**Bad:**
```
WIP
```

---

## 🎯 **Quality Gates**

### **Gate 1: Local Build**
- **Requirement**: `./gradlew build` succeeds
- **When**: Before every commit
- **Action**: Fix compilation errors immediately

### **Gate 2: Local Tests**
- **Requirement**: `./gradlew test` passes with 100%
- **When**: Before every push
- **Action**: Fix failing tests before pushing

### **Gate 3: CI Pipeline**
- **Requirement**: GitHub Actions shows ✅ green
- **When**: After every push
- **Action**: Wait for CI, fix if fails

### **Gate 4: Code Review** (Future)
- **Requirement**: Peer review approval
- **When**: Before merging to main
- **Action**: Address review comments

---

## 🚀 **CI/CD Pipeline Details**

### **What CI Tests**
1. **Build**: Compiles all modules
2. **Unit Tests**: Runs all test suites
3. **Code Quality** (when configured):
   - ktlint (code style)
   - detekt (static analysis)
   - JaCoCo (code coverage)
4. **Security Scan** (when configured):
   - OWASP dependency check

### **CI Configuration**
- **Location**: `.github/workflows/ci.yml`
- **Trigger**: Every push to main
- **Runtime**: ~2-3 minutes
- **Parallel Jobs**: Build, test, quality checks

### **Viewing CI Results**
1. Go to GitHub repository
2. Click "Actions" tab
3. Find your commit
4. Click to see detailed logs
5. Check each job status

---

## 📊 **Issue Tracking**

### **Issue Document Structure**
Each issue has a dedicated markdown file:
- `Development_Plan/Issue_XX_Title.md`

### **Issue Document Sections**
1. **Header**: Status, dates, dependencies
2. **Objective**: What needs to be accomplished
3. **Tasks**: Checkbox list of all tasks
4. **Deliverables**: List of files created
5. **Success Criteria**: How to verify completion
6. **Code Statistics**: Metrics
7. **Completion Checklist**: Final verification
8. **Footer**: Completion date, commits

### **Updating Issue Documents**
- **During Development**: Mark tasks as complete [x]
- **After CI Passes**: Add final commit SHA
- **On Completion**: Update status to ✅ COMPLETED
- **If Issues Found**: Document in "Known Issues" section

---

## 🐛 **Bug Fix Protocol**

### **When a Bug is Found**

1. **Document in Issue File**
   ```markdown
   ### Post-Completion Fixes
   
   #### Fix #1: TradeRepositoryTest CI Failure
   **Date**: October 24, 2025
   **Commit**: c555152
   **Issue**: NullPointerException in test setup
   **Fix**: Added directory creation and requireNotNull()
   **Verification**: All 24 tests passing ✅
   ```

2. **Create Fix Commit**
   ```bash
   git commit -m "fix: Resolve specific issue"
   ```

3. **Wait for CI**
   - Verify fix works in CI environment

4. **Update Documentation**
   - Add to issue document
   - Update commit references
   - Note in Development Plan if significant

---

## 📈 **Progress Tracking**

### **Update These Files**
1. **Issue_XX Document**: Mark tasks complete, add commits
2. **Development_Plan_v2.md**: Update progress percentages
3. **Cursor/README.md**: Update status and next steps

### **Frequency**
- After each issue completion
- After each significant bugfix
- Weekly progress review

---

## 🔒 **Branch Protection (Future)**

When team grows, consider:
- Protect main branch
- Require pull requests
- Require CI passing before merge
- Require code review approval
- Prevent force pushes

---

## 🎓 **Best Practices**

### **DO**
✅ Test locally before pushing  
✅ Wait for CI to pass  
✅ Write descriptive commit messages  
✅ Document issues and fixes  
✅ Keep commits focused and atomic  
✅ Update issue documents regularly  
✅ Fix CI failures immediately  

### **DON'T**
❌ Push without local testing  
❌ Proceed without CI verification  
❌ Ignore CI failures  
❌ Commit commented-out code  
❌ Leave debug statements  
❌ Make unrelated changes in one commit  
❌ Skip documentation updates  

---

## 📞 **When in Doubt**

1. **Run tests locally**: `./gradlew test`
2. **Check CI status**: GitHub Actions page
3. **Review this workflow**: Follow the steps
4. **Ask for help**: Better to ask than break main

---

## 🔄 **Workflow Updates**

This workflow document should be updated when:
- New tools are added to CI
- Testing strategy changes
- Quality gates are modified
- Team processes evolve

---

## 📚 **Related Documents**

- `Development_Plan/Development_Plan_v2.md` - Overall project plan
- `Development_Plan/Issue_XX_*.md` - Individual issue plans
- `TESTING_GUIDE.md` - Comprehensive testing guide
- `PIPELINE_SETUP_GUIDE.md` - CI/CD configuration
- `.github/workflows/ci.yml` - CI configuration

---

## 📌 **Quick Reference**

### **Before Every Commit**
```bash
./gradlew test --no-daemon  # Must pass ✅
```

### **After Every Push**
```
1. Go to GitHub Actions
2. Wait for ✅ green checkmark
3. Only then proceed to next task
```

### **If CI Fails**
```
STOP → Fix → Test → Push → Wait → Verify ✅
```

---

**Last Updated**: October 24, 2025  
**Version**: 1.0  
**Status**: Active and Required for All Development

