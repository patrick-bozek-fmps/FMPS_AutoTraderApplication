# DEF_[#ID]: [Brief Description of the Defect]

**Status**: [Choose one: 🆕 **NEW**, 👤 **ASSIGNED**, 🏗️ **IN PROGRESS**, ✅ **FIXED**, ✔️ **VERIFIED**, 🔒 **CLOSED**, ❌ **REJECTED**, ⚠️ **REOPENED**, ⏸️ **DEFERRED**]  
**Severity**: [Choose one: 🔴 **CRITICAL**, 🟠 **HIGH**, 🟡 **MEDIUM**, 🟢 **LOW**, 🔵 **TRIVIAL**]  
**Priority**: [Choose one: **P0 (Critical)**, **P1 (High)**, **P2 (Medium)**, **P3 (Low)**]  
**Reported By**: [Name - Role (e.g., SW Test Engineer, SW Developer, SW QA Engineer)]  
**Reported Date**: [YYYY-MM-DD HH:MM]  
**Assigned To**: [Developer Name or "Unassigned"]  
**Assigned Date**: [YYYY-MM-DD or "Not Assigned"]  
**Fixed By**: [Developer Name or "N/A"]  
**Fixed Date**: [YYYY-MM-DD or "N/A"]  
**Verified By**: [QA/Test Engineer Name or "N/A"]  
**Verified Date**: [YYYY-MM-DD or "N/A"]  
**Closed Date**: [YYYY-MM-DD or "Not Closed"]  
**Epic**: [Epic Name (Epic N) or "N/A"]  
**Issue**: [Related Issue #XX or "N/A"]  
**Module/Component**: [e.g., core-service, desktop-ui, shared, risk-manager, position-manager, etc.]  
**Version Found**: [Git commit SHA or version tag]  
**Version Fixed**: [Git commit SHA or version tag or "N/A"]  

> **NOTE**: [Optional: Add any important notes about the defect status, blockers, or special considerations]

---

## 📋 **Defect Summary**

[Provide a clear, concise one-sentence description of the defect. This should be understandable by both technical and non-technical stakeholders.]

**Example**: "Risk Manager fails to enforce maximum leverage limit when emergency stop is active, allowing trades to proceed despite stop condition."

---

## 🎯 **Impact Assessment**

### **Business Impact**
- **User Impact**: [Choose one: **None** / **Low** (minor inconvenience) / **Medium** (affects some users/workflows) / **High** (affects many users/critical workflows) / **Critical** (system unusable/data loss)]
- **Workaround Available**: [Yes/No - If yes, describe the workaround]
- **Data Loss Risk**: [Yes/No - If yes, describe potential data loss]
- **Security Risk**: [Yes/No - If yes, describe security implications]

### **Technical Impact**
- **Affected Components**: [List all components/modules affected]
- **System Stability**: [Choose one: **No Impact** / **Degraded Performance** / **Intermittent Failures** / **System Crash**]
- **Test Coverage Gap**: [Yes/No - If yes, describe why existing tests didn't catch this]
- **Regression Risk**: [Low/Medium/High - Risk of introducing new defects when fixing this]

### **Development Impact**
- **Blocks Other Work**: [Yes/No - List blocked issues/epics if yes]
- **Estimated Fix Time**: [X hours/days]
- **Complexity**: [Choose one: **Simple** (1-2 hours) / **Moderate** (half day) / **Complex** (1-2 days) / **Very Complex** (3+ days)]

---

## 🔍 **Detailed Description**

[Provide a detailed description of the defect. Include what was expected vs. what actually happened. Be specific and technical.]

**Expected Behavior**:
[Describe what should happen when the system is working correctly]

**Actual Behavior**:
[Describe what actually happens when the defect occurs]

**Example**:
```
Expected Behavior:
- When emergency stop is activated, Risk Manager should immediately reject all new trade requests
- Emergency stop status should be checked before any leverage calculations
- No trades should proceed while emergency stop is active

Actual Behavior:
- Risk Manager allows trades to proceed even when emergency stop is active
- checkRiskLimits() method ignores emergency stop state
- Trades execute successfully despite stop condition
```

---

## 🔄 **Steps to Reproduce**

[Provide clear, numbered steps that anyone can follow to reproduce the defect. Include specific inputs, configurations, and actions.]

1. [Step 1: Initial setup or precondition]
2. [Step 2: Specific action]
3. [Step 3: Another action]
4. [Step 4: Observe the defect]

**Example**:
```
1. Start the core service: ./gradlew :core-service:run
2. Create a trader with max leverage of 10x
3. Activate emergency stop via API: POST /api/traders/{id}/emergency-stop
4. Attempt to place a trade that would exceed leverage limit
5. Observe: Trade is accepted instead of being rejected
```

**Reproducibility**: [Choose one: **Always** (100%) / **Often** (50-99%) / **Sometimes** (10-49%) / **Rarely** (<10%) / **Unable to Reproduce**]

---

## 🌍 **Environment Details**

### **Test Environment**
- **OS**: [Windows 10/11, Linux, macOS - include version]
- **Java Version**: [e.g., OpenJDK 17.0.2]
- **Gradle Version**: [e.g., 8.5]
- **Database**: [e.g., H2, PostgreSQL - include version if applicable]
- **Exchange**: [Binance Testnet, Bitget Testnet, or "N/A"]

### **Configuration**
- **Configuration File**: [Path to config file or "Default"]
- **Environment Variables**: [List any relevant env vars or "None"]
- **API Keys**: [Testnet keys used or "N/A"]

### **Build Information**
- **Commit SHA**: [Git commit hash where defect was found]
- **Branch**: [Branch name, typically "main"]
- **Build Date**: [YYYY-MM-DD]
- **CI Run ID**: [GitHub Actions run ID if found in CI, or "N/A"]

---

## 📊 **Evidence & Logs**

### **Error Messages**
```
[Paste relevant error messages, stack traces, or log excerpts here]
```

### **Screenshots/Logs**
- **Screenshot**: [Path to screenshot file or "N/A"]
- **Log File**: [Path to log file or "N/A"]
- **Test Output**: [Path to test output or "N/A"]

### **Test Case**
- **Test Name**: [Name of failing test if applicable]
- **Test File**: [Path to test file or "N/A"]
- **Test Output**: 
```
[Paste test failure output here]
```

---

## 🔗 **Related Items**

### **Related Defects**
- Defect #[ID]: [Brief description] - [Relationship: Duplicate, Related, Blocks, Blocked By]
- Defect #[ID]: [Brief description] - [Relationship]

### **Related Issues**
- Issue #[ID]: [Brief description] - [Relationship]
- Issue #[ID]: [Brief description] - [Relationship]

### **Related Epics**
- Epic #[N]: [Brief description] - [Relationship]

### **Requirements**
- **ATP_ProdSpec_XX**: [Requirement description] - [How defect relates]
- **ATP_SysSpec_YY**: [Requirement description] - [How defect relates]

---

## 🛠️ **Resolution Details**

### **Root Cause Analysis**
[Describe the root cause of the defect. What was the underlying issue?]

**Example**:
```
Root Cause:
The checkRiskLimits() method in RiskManager.kt was not checking the emergency stop state before performing leverage calculations. The method signature and implementation focused only on budget and leverage limits, ignoring the global emergency stop flag that should take precedence over all other checks.
```

### **Solution Description**
[Describe how the defect was fixed. Include code changes, configuration changes, or process changes.]

**Example**:
```
Solution:
1. Modified checkRiskLimits() to check emergency stop state first
2. Added early return if emergency stop is active
3. Updated RiskManager documentation to clarify emergency stop precedence
4. Added unit test to verify emergency stop blocks all trades
```

### **Code Changes**
- **Files Modified**: 
  - `[path/to/file1.kt]` - [What was changed]
  - `[path/to/file2.kt]` - [What was changed]
- **Files Added**: 
  - `[path/to/new/file.kt]` - [Purpose]
- **Files Deleted**: 
  - `[path/to/deleted/file.kt]` - [Reason]

### **Test Changes**
- **Tests Added**: 
  - `[TestClassName.testMethodName]` - [What it tests]
- **Tests Modified**: 
  - `[TestClassName.testMethodName]` - [What changed]
- **Test Coverage**: [Coverage percentage before/after if applicable]

### **Documentation Updates**
- `[DocumentationFile.md]` - [What was updated]

---

## ✅ **Verification**

### **Verification Steps**
[Provide clear, numbered steps to verify the fix works correctly. Should mirror or extend the reproduction steps.]

1. [Step 1: Setup]
2. [Step 2: Action]
3. [Step 3: Verify expected behavior]
4. [Step 4: Verify defect no longer occurs]

### **Verification Results**
- **Status**: [✅ **PASSED** / ❌ **FAILED** / ⏳ **PENDING**]
- **Verified By**: [Name - Role]
- **Verification Date**: [YYYY-MM-DD]
- **Verification Environment**: [Same as test environment or different]
- **Test Results**: 
  ```
  [Paste test output showing fix works]
  ```

### **Regression Testing**
- **Related Tests Pass**: [Yes/No - List test names]
- **Full Test Suite**: [✅ Passed / ❌ Failed]
- **CI Pipeline**: [✅ Passed / ❌ Failed / ⏳ Pending]
- **CI Run ID**: [GitHub Actions run ID]

---

## 📈 **Metrics & Impact**

### **Time Tracking**
- **Time to Fix**: [X hours/days]
- **Time to Verify**: [X hours/days]
- **Total Time**: [X hours/days]

### **Code Metrics**
- **Lines Changed**: [Added: X, Removed: Y, Modified: Z]
- **Files Changed**: [X files]
- **Test Coverage Impact**: [Before: X%, After: Y%]

### **Quality Impact**
- **Similar Defects Found**: [Yes/No - If yes, list defect IDs]
- **Process Improvements**: [Any process changes to prevent similar defects]

---

## 🎓 **Lessons Learned**

[Document any lessons learned from this defect. What could have prevented it? What should be done differently?]

- [Lesson 1: e.g., "Emergency stop state should be checked at the entry point of all risk checks"]
- [Lesson 2: e.g., "Missing test coverage for emergency stop + leverage combination"]
- [Lesson 3: e.g., "Documentation should clarify precedence of risk checks"]

---

## 📝 **Comments & History**

### **Comment Log**
| Date | Author | Role | Comment |
|------|--------|------|---------|
| YYYY-MM-DD | [Name] | [Role] | [Comment] |
| YYYY-MM-DD | [Name] | [Role] | [Comment] |

### **Status History**
| Date | Status | Changed By | Notes |
|------|--------|------------|-------|
| YYYY-MM-DD | NEW | [Name] | Defect reported |
| YYYY-MM-DD | ASSIGNED | [Name] | Assigned to [Developer] |
| YYYY-MM-DD | IN PROGRESS | [Name] | Fix in progress |
| YYYY-MM-DD | FIXED | [Name] | Fix committed: [SHA] |
| YYYY-MM-DD | VERIFIED | [Name] | Verified by QA |
| YYYY-MM-DD | CLOSED | [Name] | Defect closed |

---

## 🔄 **Workflow Integration**

### **Development Workflow Steps**
This defect follows the standard development workflow from `DEVELOPMENT_WORKFLOW.md`:

1. ✅ **Defect Reported** - Initial defect report created
2. ⏳ **Assigned** - Defect assigned to developer
3. ⏳ **Fix Implemented** - Developer implements fix
4. ⏳ **Local Testing** - Developer tests fix locally: `./gradlew test`
5. ⏳ **Committed** - Fix committed with descriptive message
6. ⏳ **CI Verification** - CI pipeline passes: `check-ci-status.ps1 -Watch`
7. ⏳ **QA Verification** - QA verifies fix
8. ⏳ **Closed** - Defect closed after verification

### **Commit References**
- **Fix Commit**: [Git commit SHA] - `[commit message]`
- **Verification Commit**: [Git commit SHA] - `[commit message]` (if applicable)

---

## 🎯 **Definition of Done**

- [ ] Defect root cause identified and documented
- [ ] Fix implemented and tested locally
- [ ] All local tests pass: `./gradlew test`
- [ ] Code changes committed with descriptive message
- [ ] CI pipeline passes (GitHub Actions green checkmark)
- [ ] Fix verified by QA/Test Engineer
- [ ] Regression tests pass
- [ ] Documentation updated (if applicable)
- [ ] Defect status updated to VERIFIED
- [ ] Defect status updated to CLOSED
- [ ] Related issues/epics updated (if applicable)
- [ ] Lessons learned documented

---

## 📌 **Status Icons Reference**

### **Defect Status**
- 🆕 **NEW** - Defect reported, not yet reviewed
- 👤 **ASSIGNED** - Defect assigned to developer, not yet started
- 🏗️ **IN PROGRESS** - Developer actively working on fix
- ✅ **FIXED** - Fix implemented and committed, awaiting verification
- ✔️ **VERIFIED** - Fix verified by QA, defect resolved
- 🔒 **CLOSED** - Defect closed, no further action needed
- ❌ **REJECTED** - Defect rejected (duplicate, not a bug, etc.)
- ⚠️ **REOPENED** - Defect reopened after being closed
- ⏸️ **DEFERRED** - Defect deferred to future release

### **Severity Levels**
- 🔴 **CRITICAL** - System crash, data loss, security breach, system unusable
- 🟠 **HIGH** - Major functionality broken, significant impact on users
- 🟡 **MEDIUM** - Minor functionality broken, workaround available
- 🟢 **LOW** - Cosmetic issue, minor inconvenience
- 🔵 **TRIVIAL** - Typo, formatting issue, documentation error

### **Priority Levels**
- **P0 (Critical)**: Must be fixed immediately, blocks release
- **P1 (High)**: Should be fixed in current sprint/release
- **P2 (Medium)**: Can be fixed in next sprint/release
- **P3 (Low)**: Can be fixed when resources available

---

## 📚 **How to Use This Template**

### **For SW Test Engineers**
1. **When Finding a Defect**:
   - Copy this template to `Development_Plan/Defects/DEF_[#ID]_Brief_Description.md`
   - **Important**: Use format `DEF_[#ID]_Brief_Description.md` where #ID is a sequential number (zero-padded to 3 digits, e.g., 001, 002, 023)
   - **NO "Defect_" prefix** - The ID already indicates it's a defect
   - Fill in all sections up to "Resolution Details"
   - Set status to 🆕 **NEW**
   - Assign appropriate severity and priority
   - Add steps to reproduce and evidence

2. **When Verifying a Fix**:
   - Update "Verification" section
   - Run verification steps
   - Update status to ✔️ **VERIFIED** if fix works
   - Update status to ⚠️ **REOPENED** if fix doesn't work
   - Add comments to comment log

### **For SW Developers**
1. **When Assigned a Defect**:
   - Update status to 👤 **ASSIGNED** or 🏗️ **IN PROGRESS**
   - Reproduce the defect locally
   - Investigate root cause
   - Implement fix
   - Test fix locally: `./gradlew test`
   - Update "Resolution Details" section
   - Commit fix with descriptive message
   - Update status to ✅ **FIXED**
   - Push to GitHub and wait for CI

2. **When Fix is Verified**:
   - Update status history
   - Add any additional notes to comments

### **For SW QA and Process Engineers**
1. **When Reviewing Defects**:
   - Review defect reports for completeness
   - Verify severity and priority are appropriate
   - Ensure proper assignment
   - Track defect metrics
   - Identify patterns and process improvements

2. **When Closing Defects**:
   - Verify all "Definition of Done" items are complete
   - Update status to 🔒 **CLOSED**
   - Update related issues/epics
   - Document lessons learned
   - Update defect tracking metrics

### **Best Practices**
- **Be Specific**: Include exact error messages, stack traces, and steps
- **Include Evidence**: Screenshots, logs, and test outputs help developers
- **Update Regularly**: Keep status and comments current
- **Link Related Items**: Connect defects to issues, epics, and requirements
- **Document Root Cause**: Understanding why defects occur helps prevent future ones
- **Follow Workflow**: Adhere to `DEVELOPMENT_WORKFLOW.md` for all fixes

---

## 📋 **Defect ID and File Naming Format**

### **Defect ID Format**
**Format**: `DEF_[#ID]` (sequential number, zero-padded to 3 digits)

**Examples**:
- `DEF_001` - First defect
- `DEF_023` - 23rd defect
- `DEF_156` - 156th defect

**Note**: The Epic information is tracked in the defect document itself (see "Epic" field in header), not in the ID or filename.

### **File Naming Convention**
**Format**: `DEF_[#ID]_Brief_Description.md`

**Rules**:
- **NO "Defect_" prefix** - The ID already indicates it's a defect
- Use underscores instead of spaces in the description
- Keep description brief (3-5 words maximum)
- Use title case for description words
- ID is zero-padded to 3 digits (001, 002, ..., 999)

**Examples**:
- ✅ `DEF_001_Server_Startup_Configuration_Mismatch.md`
- ✅ `DEF_015_Database_Connection_Timeout.md`
- ✅ `DEF_002_Memory_Leak_In_Telemetry.md`
- ❌ `Defect_DEF_001_Server_Startup_Configuration_Mismatch.md` (redundant "Defect_" prefix)
- ❌ `DEF_1_Server_Startup_Configuration_Mismatch.md` (ID not zero-padded)
- ❌ `DEF_001 Server Startup Configuration Mismatch.md` (spaces not allowed)

**Storage Location**: `03_Development/Application_OnPremises/Cursor/Development_Plan/Defects/`

---

## 🔗 **Related Documents**

- `DEVELOPMENT_WORKFLOW.md` - Standard development workflow
- `TESTING_GUIDE.md` - Testing guidelines and best practices
- `ISSUE_TEMPLATE.md` - Template for feature issues
- `ISSUE_REVIEW_TEMPLATE.md` - Template for issue reviews
- `.github/workflows/ci.yml` - CI/CD pipeline configuration

---

**Template Version**: 1.0  
**Last Updated**: November 18, 2025  
**Maintained By**: Software Process and DevOps Expert  
**Status**: Active

---

## 📝 **Quick Reference Checklist**

### **When Reporting a Defect**
- [ ] Defect ID assigned
- [ ] Summary written (one sentence)
- [ ] Severity and priority set
- [ ] Steps to reproduce included
- [ ] Environment details documented
- [ ] Evidence attached (logs, screenshots)
- [ ] Related items linked
- [ ] Status set to 🆕 **NEW**

### **When Fixing a Defect**
- [ ] Root cause identified
- [ ] Solution documented
- [ ] Code changes made
- [ ] Tests added/updated
- [ ] Local tests pass
- [ ] Fix committed
- [ ] CI passes
- [ ] Status updated to ✅ **FIXED**

### **When Verifying a Fix**
- [ ] Verification steps executed
- [ ] Expected behavior confirmed
- [ ] Regression tests pass
- [ ] Status updated to ✔️ **VERIFIED**
- [ ] Comments added

### **When Closing a Defect**
- [ ] All "Definition of Done" items complete
- [ ] Status updated to 🔒 **CLOSED**
- [ ] Related items updated
- [ ] Lessons learned documented
- [ ] Metrics updated


