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
│  • Convert Excel requirements to Markdown (if needed)   │
│    - Navigate to: Cursor\Artifacts\                     │
│    - Run: convert_excel.bat (Windows)                    │
│    - Or: python excel_to_markdown_converter.py          │
│    - Ensures latest requirements are available          │
│  • Review requirements documents                        │
│    - Check 02_ReqMgn\*.md files                        │
│    - Identify relevant requirements                      │
│    - Note any new or updated requirements               │
│  • For NEW EPIC: Create Epic status document            │
│    - Create EPIC_[N]_STATUS.md                          │
│    - Use EPIC_STATUS_TEMPLATE.md as reference           │
│    - Run deviation analysis (EPIC_[N]_DEVIATION_ANALYSIS)│
│    - Document epic goals and issues                     │
│  • For EXISTING EPIC: Update Epic status                │
│    - Update EPIC_[N]_STATUS.md                          │
│    - Mark completed issues                              │
│    - Update progress and blockers                       │
│  • Create Issue_XX plan document                        │
│    - Use ISSUE_TEMPLATE.md as reference                 │
│    - Include requirements traceability                  │
│    - Link to Epic if part of one                        │
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
│  • Then check annotations:                               │
│    .\Cursor\check-ci-annotations.ps1                     │
│  • Review any warnings (cache failures are OK)           │
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

### **CI Annotations (Infrastructure Warnings)** ⚠️

GitHub Actions may report annotations (warnings from the CI infrastructure itself). These are **separate from code warnings**.

**Check CI Annotations**:
```powershell
# Check latest run annotations
.\03_Development\Application_OnPremises\Cursor\check-ci-annotations.ps1

# Show full details
.\03_Development\Application_OnPremises\Cursor\check-ci-annotations.ps1 -ShowAll

# Check specific run by ID
.\03_Development\Application_OnPremises\Cursor\check-ci-annotations.ps1 -RunId 12345678
```

**Common CI Annotations**:
1. **Cache Failures** (most common):
   - `Failed to save cache entry` - GitHub's cache service unavailable
   - `Failed to restore cache` - Cache service errors (400, 503)
   - **Impact**: Build runs slower (no cached dependencies)
   - **Action**: None required - these are GitHub infrastructure issues

2. **Node.js/Setup Warnings**:
   - Deprecated actions or runtime versions
   - **Action**: Update action versions in `.github/workflows/ci.yml`

3. **Deprecation Warnings**:
   - Actions using deprecated features
   - **Action**: Update to newer action versions

**Policy**:
- ✅ Cache failures: **Acceptable** (GitHub infrastructure, not our fault)
- ⚠️ Deprecation warnings: **Fix within 1 month** (update actions)
- ❌ Failure annotations: **Must fix immediately** (CI failures)

**Exit Codes**:
- `0` = No warnings/failures (clean)
- `1` = Failure annotations found (must fix)
- `2` = Warning annotations found (review)

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
1. **Build**: Compiles all modules (no tests)
2. **Unit Tests**: Runs all tests EXCLUDING `@integration` tag
   - Fast execution (~1-2 minutes)
   - No external dependencies required
   - Should always pass ✅
3. **Integration Tests**: Runs only `@integration` tagged tests
   - Only runs if API keys are configured in GitHub secrets
   - Requires exchange API credentials (Binance, Bitget)
   - Optional - workflow still passes if secrets not configured
4. **Code Quality** (when configured):
   - ktlint (code style)
   - detekt (static analysis)
   - JaCoCo (code coverage)
5. **Security Scan** (when configured):
   - OWASP dependency check

### **CI Configuration**
- **Location**: `.github/workflows/ci.yml`
- **Trigger**: Every push to main and pull requests
- **Runtime**: ~2-3 minutes (unit tests), +1-2 minutes (integration if secrets present)
- **Job Structure**:
  - `unit-tests`: Always runs, must pass
  - `integration-tests`: Runs only if secrets configured, depends on `unit-tests`
- **Test Separation**: Uses JUnit tags (`@integration`) to separate test types

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

## 📋 **Requirements Review Process** (Step 1 Detailed)

### **When Starting a New Issue or Epic**

Before creating issue documentation, ensure requirements are up-to-date:

#### **1. Convert Excel Requirements to Markdown**

**Purpose**: Excel files in `02_ReqMgn/` are the source of truth, but AI assistants and version control work better with Markdown.

**Quick Method (Windows)**:
```powershell
# Navigate to Artifacts folder
cd 03_Development\Application_OnPremises\Cursor\Artifacts

# Run converter (checks dependencies, installs if needed)
.\convert_excel.bat
```

**Manual Method**:
```powershell
# Install dependencies (first time only)
pip install pandas openpyxl watchdog tabulate

# Run converter
cd 03_Development\Application_OnPremises\Cursor\Artifacts
python excel_to_markdown_converter.py
```

**Watch Mode** (for active requirements development):
```powershell
# Auto-converts when Excel files change
.\convert_excel_watch.bat
```

**What Gets Converted**:
- `02_ReqMgn/FMPS_AutoTraderApplication_Customer_Specification.xlsx` → `.md`
- `02_ReqMgn/FMPS_AutoTraderApplication_System_Specification.xlsx` → `.md`
- `02_ReqMgn/CommonDefinitionOfTermsAndAbbreviations.xlsx` → `.md`
- Other Excel files in `02_ReqMgn/`

**Output**: Markdown files in `02_ReqMgn/` with same name as Excel files

#### **2. Review Requirements Documents**

**Check These Files**:
- `02_ReqMgn/FMPS_AutoTraderApplication_Customer_Specification.md`
  - Customer requirements (ATP_ProdSpec_XX)
  - Functional requirements
  - Non-functional requirements
- `02_ReqMgn/FMPS_AutoTraderApplication_System_Specification.md`
  - System requirements (ATP_SysSpec_XX)
  - Architecture requirements
  - Test cases

**Identify Relevant Requirements**:
- Search for requirement IDs related to your issue/epic
- Note any new requirements not yet implemented
- Check for updated requirements that might affect implementation
- Document requirement traceability in issue plan

**Example Search**:
```bash
# Search for AI Trading requirements
grep -i "AI.*trad" 02_ReqMgn/*.md
grep -i "ATP_ProdSpec_5" 02_ReqMgn/*.md
```

#### **3. Epic Creation/Update (Before Creating Issues)**

**For NEW EPIC** (e.g., starting Epic 3):

1. **Create Epic Status Document**:
   - Create `Development_Plan/EPIC_[N]_STATUS.md`
   - Use `Development_Handbook/EPIC_STATUS_TEMPLATE.md` as reference
   - Fill in epic overview, goals, and issue breakdown

2. **Run Deviation Analysis**:
   - Create `Development_Plan/EPIC_[N]_DEVIATION_ANALYSIS.md`
   - Review requirements vs. `Development_Plan_v2.md`
   - Compare planned scope with actual requirements
   - Document any deviations found
   - **If deviations found**: Review and decide on approach
   - **If no deviations**: Proceed with planning

3. **Plan Epic Issues**:
   - Break epic into detailed issues
   - Create `Issue_XX_*.md` files using `ISSUE_TEMPLATE.md`
   - Include requirements traceability in each issue
   - Link issues to Epic in both directions

4. **Update Development Plan**:
   - Update `Development_Plan_v2.md` with epic details
   - Document issue breakdown and dependencies

**For EXISTING EPIC** (when creating a new issue in ongoing epic):

1. **Update Epic Status**:
   - Open `Development_Plan/EPIC_[N]_STATUS.md`
   - Add new issue to "Planned Issues" section
   - Update progress counters
   - Note any blockers or dependencies

2. **Check Epic Goals**:
   - Verify new issue aligns with epic goals
   - Update epic success criteria if needed
   - Document any scope changes

#### **4. Requirements Traceability in Issues**

Each Issue document should include:

```markdown
## 📋 **Requirements Traceability**

**Related Requirements**:
- ATP_ProdSpec_XX: [Requirement description]
- ATP_SysSpec_YY: [Requirement description]

**Requirement Mapping**:
- Task 1.1 → ATP_ProdSpec_XX
- Task 2.1 → ATP_SysSpec_YY
```

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
- `Development_Plan/EPIC_[N]_STATUS.md` - Epic status tracking
- `Development_Plan/EPIC_[N]_DEVIATION_ANALYSIS.md` - Epic deviation analysis
- `Development_Handbook/ISSUE_TEMPLATE.md` - Issue planning template
- `Development_Handbook/EPIC_STATUS_TEMPLATE.md` - Epic status template
- `Development_Handbook/CONVERTER_README.md` - Excel to Markdown converter guide
- `Artifacts/excel_to_markdown_converter.py` - Converter script
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

**Last Updated**: October 30, 2025  
**Version**: 1.1  
**Status**: Active and Required for All Development

**Changes in v1.1**:
- ✅ Added requirements review process to Step 1
- ✅ Added Excel to Markdown conversion steps
- ✅ Added Epic creation/update steps (before creating issues)
- ✅ Added deviation analysis process for Epic planning
- ✅ Added requirements traceability guidelines

