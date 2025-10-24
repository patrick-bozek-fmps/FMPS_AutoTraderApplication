# CI Automation Tools 🤖

This folder contains automation scripts for monitoring and verifying CI/CD pipeline health.

## 📋 Available Scripts

### 1. `check-ci-status.ps1` - CI Status Monitor

**Purpose**: Automatically monitor GitHub Actions workflow runs and their status.

**Usage**:
```powershell
# One-time check of latest CI run
.\check-ci-status.ps1

# Watch mode - auto-check every 20 seconds until complete
.\check-ci-status.ps1 -Watch -WaitSeconds 20

# Watch mode with custom interval
.\check-ci-status.ps1 -Watch -WaitSeconds 30
```

**Output**:
- Shows 5 most recent workflow runs
- Displays status (in_progress, completed, queued)
- Shows conclusion (success, failure)
- Provides commit SHA, timestamps
- Direct link to GitHub Actions run

**Exit Codes**:
- `0` = Success (CI passed)
- `1` = Failure (CI failed)
- `2` = Warning (CI completed with warnings)
- `3` = In Progress (CI still running)

**Example Output**:
```
======================================================================
  GitHub Actions CI Status Checker
======================================================================

Repository: patrick-bozek-fmps/FMPS_AutoTraderApplication
Fetching latest workflow runs...

Recent Workflow Runs:
----------------------------------------------------------------------
Status  | Commit  | State        | Conclusion | Created At
----------------------------------------------------------------------
[OK] 70639b9 | completed   | success   | 2025-10-24 12:01
[OK] 51fb12f | completed   | success   | 2025-10-24 11:40
[OK] c555152 | completed   | success   | 2025-10-24 11:04
[...] abc1234 | in_progress | N/A       | 2025-10-24 12:30
[X] def5678 | completed   | failure   | 2025-10-24 12:20

======================================================================
  Latest Run Details
======================================================================
Commit SHA:  70639b9
Status:      completed
Conclusion:  success
Started:     10/24/2025 12:01:25
Updated:     10/24/2025 12:04:19
Run URL:     https://github.com/.../actions/runs/18776384869

[SUCCESS] Latest CI run passed!
You can proceed with the next step.
```

---

### 2. `check-ci-annotations.ps1` - CI Annotations Inspector

**Purpose**: Retrieve and display annotations (warnings, errors, notices) from CI workflow runs.

**Usage**:
```powershell
# Check annotations from latest run
.\check-ci-annotations.ps1

# Show full details (includes messages)
.\check-ci-annotations.ps1 -ShowAll

# Check specific run by ID
.\check-ci-annotations.ps1 -RunId 18776384869
```

**What Are Annotations?**

Annotations are messages from GitHub Actions that provide additional context about workflow runs:
- **Warnings**: Non-critical issues (e.g., cache failures, deprecations)
- **Failures**: Critical issues that need immediate attention
- **Notices**: Informational messages

**Common Annotations**:

1. **Cache Failures** (Most Common):
   ```
   Failed to save cache entry with path '/home/runner/.gradle/caches,...'
   Failed to restore cache: Error: Cache service responded with 400
   ```
   - **Cause**: GitHub's cache service temporarily unavailable
   - **Impact**: Build runs slower (no cached dependencies)
   - **Action**: ✅ **None required** - GitHub infrastructure issue

2. **Deprecation Warnings**:
   ```
   Node.js 16 actions are deprecated
   ```
   - **Cause**: Using outdated action versions
   - **Impact**: Future CI failures when deprecated versions removed
   - **Action**: ⚠️ **Update action versions in `.github/workflows/ci.yml`**

3. **Failure Annotations**:
   ```
   Process completed with exit code 1
   ```
   - **Cause**: Test failures, build errors
   - **Impact**: CI fails
   - **Action**: ❌ **Must fix immediately**

**Output**:
```
======================================================================
  GitHub Actions Annotations Checker
======================================================================

Latest Run:
  Commit:     70639b9
  Status:     completed
  Conclusion: success
  Run ID:     18776384869

Fetching jobs for run 18776384869...
Found 1 job(s)

Job: Build and Test
  Status: completed | Conclusion: success
  Found 7 annotation(s)

======================================================================
  Annotations Summary
======================================================================
Total Annotations: 7
  Warnings: 7
  Failures: 0
  Notices:  0

----------------------------------------------------------------------
WARNINGS (7)
----------------------------------------------------------------------
[x6] Failed to save cache entry
  Job: Build and Test
  Message: Failed to save cache entry with path '/home/runner/.gradle/caches,...'
  File: .github:26

[x1] Failed to restore cache
  Job: Build and Test
  Message: Failed to restore v8-gradle|Linux|...: Error: Cache service responded with 400
  File: .github:22

[WARNING] 7 warning annotation(s) found
```

**Exit Codes**:
- `0` = No warnings/failures (clean)
- `1` = Failure annotations found (must fix)
- `2` = Warning annotations found (review, may be acceptable)

---

## 🔄 Typical Workflow

### After Pushing Code:

```powershell
# Step 1: Wait for CI to start (30 seconds)
Start-Sleep -Seconds 30

# Step 2: Monitor CI until complete
.\03_Development\Application_OnPremises\Cursor\check-ci-status.ps1 -Watch -WaitSeconds 20

# Step 3: Check for annotations
.\03_Development\Application_OnPremises\Cursor\check-ci-annotations.ps1

# Step 4: Review results
# - If CI passed and only cache warnings → ✅ Proceed
# - If CI passed with deprecation warnings → ⚠️ Create issue to fix
# - If CI failed or failure annotations → ❌ Fix immediately
```

---

## 🔑 Authentication

Both scripts require GitHub authentication. They support:

1. **Environment Variable** (Recommended for CI/CD):
   ```powershell
   $env:GITHUB_TOKEN = "your_personal_access_token"
   ```

2. **GitHub CLI** (Recommended for local development):
   ```powershell
   gh auth login
   ```
   The scripts will automatically detect and use `gh auth token`.

**Token Permissions Required**:
- `repo` scope (for private repositories)
- `actions:read` (to read workflow runs and annotations)

---

## 📊 Exit Code Summary

| Script | Exit Code | Meaning | Action |
|--------|-----------|---------|--------|
| `check-ci-status.ps1` | 0 | CI passed | ✅ Proceed |
| | 1 | CI failed | ❌ Fix issues |
| | 2 | CI completed with warnings | ⚠️ Review |
| | 3 | CI in progress | ⏱️ Wait |
| `check-ci-annotations.ps1` | 0 | No annotations | ✅ Clean build |
| | 1 | Failure annotations | ❌ Fix immediately |
| | 2 | Warning annotations | ⚠️ Review |

---

## 🛠️ Troubleshooting

### "gh is not recognized"
- **Solution**: Install GitHub CLI or set `GITHUB_TOKEN` environment variable
- **Download**: https://cli.github.com/

### "Failed to fetch workflow runs: 401"
- **Cause**: Authentication failed
- **Solution**: Run `gh auth login` or set valid `GITHUB_TOKEN`

### "Failed to fetch workflow runs: 404"
- **Cause**: Repository not found or no access
- **Solution**: Verify repository name and access permissions

### Cache Warnings Persist
- **Status**: Normal
- **Explanation**: GitHub's cache service has intermittent availability
- **Impact**: Builds are slower but still succeed
- **Action**: None required

---

## 📝 Integration with Development Workflow

These scripts are integrated into the main development workflow. See `DEVELOPMENT_WORKFLOW.md` for the complete process:

- **Step 5**: Use `check-ci-status.ps1` to verify CI passes
- **Step 5 (continued)**: Use `check-ci-annotations.ps1` to check for warnings

---

## 🔍 Advanced Usage

### Scripting/Automation

Both scripts return exit codes suitable for automation:

```powershell
# Example: Automated deployment script
.\check-ci-status.ps1
if ($LASTEXITCODE -eq 0) {
    Write-Host "CI passed, proceeding with deployment..."
    # Deploy code
} else {
    Write-Host "CI failed, aborting deployment"
    exit 1
}
```

### CI/CD Integration

Use in GitHub Actions or other CI systems:

```yaml
- name: Wait for CI
  run: |
    .\03_Development\Application_OnPremises\Cursor\check-ci-status.ps1 -Watch -WaitSeconds 30
  
- name: Check annotations
  run: |
    .\03_Development\Application_OnPremises\Cursor\check-ci-annotations.ps1
```

---

## 📚 Related Documentation

- **DEVELOPMENT_WORKFLOW.md**: Complete development process
- **CONVERTER_README.md**: Excel to Markdown conversion tools
- **PIPELINE_SETUP_GUIDE.md**: CI/CD pipeline setup and configuration

---

**Last Updated**: October 24, 2025  
**Version**: 1.0

