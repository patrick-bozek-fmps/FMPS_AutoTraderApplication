# Development Pipeline Setup Guide

## Overview

This guide covers the complete development pipeline for FMPS AutoTrader, from local development to production release.

---

## Pipeline Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     DEVELOPER WORKFLOW                           │
├─────────────────────────────────────────────────────────────────┤
│  Local Development → Git Commit → Push to Branch                │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                  CI/CD PIPELINE (GitHub Actions)                 │
├─────────────────────────────────────────────────────────────────┤
│  1. Build          → Compile all modules                         │
│  2. Test           → Unit + Integration tests                    │
│  3. Quality Checks → Coverage, linting, security                 │
│  4. Artifacts      → Generate JARs                               │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                     RELEASE PIPELINE                             │
├─────────────────────────────────────────────────────────────────┤
│  1. Version Tag    → Create release tag                          │
│  2. Build Release  → Production builds                           │
│  3. Package        → Create installer                            │
│  4. Deploy         → Publish to GitHub Releases                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 1. Local Development Pipeline

### 1.1 Prerequisites Installation

**Required Tools:**
```bash
# Java Development Kit 17+
# Download from: https://adoptium.net/

# Kotlin (comes with IntelliJ IDEA)

# Gradle 8.5+ (comes with project wrapper)

# Git
winget install --id Git.Git

# GitHub CLI (already installed)
# Verify: gh --version

# IntelliJ IDEA Community Edition (recommended)
winget install JetBrains.IntelliJIDEA.Community
```

### 1.2 Project Setup Script

Create this script to automate local setup:

**`setup-dev-environment.ps1`:**

```powershell
# FMPS AutoTrader - Development Environment Setup
Write-Host "Setting up FMPS AutoTrader Development Environment..." -ForegroundColor Cyan

# Check Java
Write-Host "`nChecking Java installation..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Host "  ✓ Java found: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Java not found. Please install JDK 17+" -ForegroundColor Red
    exit 1
}

# Check Git
Write-Host "`nChecking Git installation..." -ForegroundColor Yellow
try {
    $gitVersion = git --version
    Write-Host "  ✓ Git found: $gitVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Git not found. Please install Git" -ForegroundColor Red
    exit 1
}

# Clone repository (if not already cloned)
$repoPath = "C:\PABLO\AI_Projects\FMPS_AutoTraderApplication"
if (Test-Path $repoPath) {
    Write-Host "`n✓ Repository already exists at: $repoPath" -ForegroundColor Green
    Set-Location $repoPath
    
    # Pull latest changes
    Write-Host "`nPulling latest changes..." -ForegroundColor Yellow
    git pull origin main
} else {
    Write-Host "`nRepository not found. Please clone first:" -ForegroundColor Red
    Write-Host "  git clone https://github.com/patrick-bozek-fmps/FMPS_AutoTraderApplication.git" -ForegroundColor Yellow
    exit 1
}

# Create local configuration
Write-Host "`nCreating local configuration..." -ForegroundColor Yellow
$configDir = "03_Development\Application_OnPremises\config"
if (!(Test-Path $configDir)) {
    New-Item -ItemType Directory -Path $configDir -Force | Out-Null
}

# Create application.conf template
$appConfig = @"
# FMPS AutoTrader - Local Development Configuration

server {
  host = "localhost"
  port = 8080
}

database {
  url = "jdbc:sqlite:./data/autotrader-dev.db"
  driver = "org.sqlite.JDBC"
}

exchanges {
  binance {
    baseUrl = "https://testnet.binance.vision"
    apiKey = `${?BINANCE_TESTNET_API_KEY}
    apiSecret = `${?BINANCE_TESTNET_API_SECRET}
  }
  
  bitget {
    baseUrl = "https://api.bitget.com"
    apiKey = `${?BITGET_TESTNET_API_KEY}
    apiSecret = `${?BITGET_TESTNET_API_SECRET}
  }
}

trading {
  maxTraders = 3
  defaultLeverage = 3
  defaultStopLossPercent = 5.0
}

logging {
  level = "DEBUG"
  file = "./logs/autotrader-dev.log"
}
"@

$configFile = Join-Path $configDir "application.conf"
Set-Content -Path $configFile -Value $appConfig
Write-Host "  ✓ Created: $configFile" -ForegroundColor Green

# Create environment variables template
$envTemplate = @"
# FMPS AutoTrader - Environment Variables
# Copy this to .env and fill in your API keys

# Binance Testnet (get from https://testnet.binance.vision/)
BINANCE_TESTNET_API_KEY=your_api_key_here
BINANCE_TESTNET_API_SECRET=your_api_secret_here

# Bitget Testnet (get from https://www.bitget.com/testnet)
BITGET_TESTNET_API_KEY=your_api_key_here
BITGET_TESTNET_API_SECRET=your_api_secret_here

# Database
DATABASE_URL=jdbc:sqlite:./data/autotrader-dev.db

# Server
SERVER_PORT=8080
"@

$envFile = Join-Path $repoPath ".env.template"
Set-Content -Path $envFile -Value $envTemplate
Write-Host "  ✓ Created: .env.template" -ForegroundColor Green

# Add .env to .gitignore
$gitignorePath = Join-Path $repoPath ".gitignore"
$gitignoreContent = Get-Content $gitignorePath -ErrorAction SilentlyContinue
if ($gitignoreContent -notcontains ".env") {
    Add-Content -Path $gitignorePath -Value "`n# Environment variables`n.env"
    Write-Host "  ✓ Added .env to .gitignore" -ForegroundColor Green
}

# Create directory structure
Write-Host "`nCreating directory structure..." -ForegroundColor Yellow
$directories = @(
    "data",
    "logs",
    "03_Development\Application_OnPremises\shared\src\main\kotlin",
    "03_Development\Application_OnPremises\shared\src\test\kotlin",
    "03_Development\Application_OnPremises\core-service\src\main\kotlin",
    "03_Development\Application_OnPremises\core-service\src\test\kotlin",
    "03_Development\Application_OnPremises\core-service\src\integrationTest\kotlin",
    "03_Development\Application_OnPremises\desktop-ui\src\main\kotlin",
    "03_Development\Application_OnPremises\desktop-ui\src\test\kotlin"
)

foreach ($dir in $directories) {
    $fullPath = Join-Path $repoPath $dir
    if (!(Test-Path $fullPath)) {
        New-Item -ItemType Directory -Path $fullPath -Force | Out-Null
        Write-Host "  ✓ Created: $dir" -ForegroundColor Green
    }
}

# Test Gradle build
Write-Host "`nTesting Gradle build..." -ForegroundColor Yellow
Set-Location "03_Development\Application_OnPremises"

# Make gradlew executable (if on WSL/Linux)
if ($IsLinux -or $IsMacOS) {
    chmod +x gradlew
}

# This will fail until we create build.gradle.kts files, but that's expected
Write-Host "  Note: Gradle build will fail until project structure is implemented" -ForegroundColor Yellow

# Setup Git hooks
Write-Host "`nSetting up Git hooks..." -ForegroundColor Yellow
$hooksDir = Join-Path $repoPath ".git\hooks"

# Pre-commit hook - runs tests before commit
$preCommitHook = @"
#!/bin/sh
# Pre-commit hook: Run tests before committing

echo "Running pre-commit checks..."

# Navigate to project
cd 03_Development/Application_OnPremises

# Run tests
./gradlew test

if [ `$? -ne 0 ]; then
    echo "Tests failed. Commit aborted."
    exit 1
fi

echo "All checks passed!"
exit 0
"@

$preCommitPath = Join-Path $hooksDir "pre-commit"
Set-Content -Path $preCommitPath -Value $preCommitHook
Write-Host "  ✓ Created pre-commit hook" -ForegroundColor Green

# Pre-push hook - runs full checks before push
$prePushHook = @"
#!/bin/sh
# Pre-push hook: Run full checks before pushing

echo "Running pre-push checks..."

cd 03_Development/Application_OnPremises

# Run all tests and checks
./gradlew clean build test integrationTest

if [ `$? -ne 0 ]; then
    echo "Checks failed. Push aborted."
    exit 1
fi

echo "All checks passed!"
exit 0
"@

$prePushPath = Join-Path $hooksDir "pre-push"
Set-Content -Path $prePushPath -Value $prePushHook
Write-Host "  ✓ Created pre-push hook" -ForegroundColor Green

Write-Host "`n" + "="*70 -ForegroundColor Cyan
Write-Host "✓ Development environment setup complete!" -ForegroundColor Green
Write-Host "="*70 -ForegroundColor Cyan

Write-Host "`nNext steps:" -ForegroundColor Yellow
Write-Host "1. Copy .env.template to .env and add your API keys"
Write-Host "2. Open project in IntelliJ IDEA"
Write-Host "3. Start with issue #1: Set up Gradle multi-module project"
Write-Host "4. Run './gradlew build' to test setup"
Write-Host "`nHappy coding! 🚀" -ForegroundColor Cyan
```

---

## 2. Git Workflow Pipeline

### 2.1 Branch Strategy

```
main (protected)
  ↑
  └─ develop (default branch)
       ↑
       ├─ feature/issue-1-gradle-setup
       ├─ feature/issue-2-database
       ├─ feature/issue-3-api-server
       ├─ bugfix/fix-connection-timeout
       └─ hotfix/critical-security-fix
```

### 2.2 Workflow Commands

**Start new feature:**
```bash
# Create feature branch from develop
git checkout develop
git pull origin develop
git checkout -b feature/issue-1-gradle-setup

# Make changes
# ...

# Commit with conventional commits
git add .
git commit -m "feat(core): set up Gradle multi-module structure

- Add root build.gradle.kts
- Configure shared, core-service, desktop-ui modules
- Add version catalog for dependencies

Closes #1"

# Push to remote
git push -u origin feature/issue-1-gradle-setup

# Create pull request
gh pr create --base develop --title "feat(core): Set up Gradle multi-module structure" --body "Closes #1"
```

**Conventional Commit Types:**
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation only
- `style:` - Code style changes (formatting)
- `refactor:` - Code refactoring
- `test:` - Adding or updating tests
- `chore:` - Maintenance tasks

### 2.3 Pull Request Checklist

Every PR must:
- [ ] Pass all CI checks
- [ ] Have 80%+ code coverage
- [ ] Include tests
- [ ] Update documentation if needed
- [ ] Follow code style guidelines
- [ ] Be reviewed by at least one person
- [ ] Reference an issue number

---

## 3. CI/CD Pipeline (GitHub Actions)

### 3.1 Current Pipeline Status

✅ **Already configured** in `.github/workflows/ci.yml`

**Triggers on:**
- Every push to `main`, `develop`, `feature/*`
- Every pull request to `main`, `develop`

**Pipeline stages:**

**Stage 1: Targeted Build & Test**
```
1. Checkout code
2. Use paths-filter to detect impacted modules
3. Set up JDK 17 + Gradle cache when tests are required
4. Run targeted Gradle test tasks (excludes @integration tagged tests)
5. Run `gradlew check -x test -x integrationTest` for detekt, ktlint, coverage gates
6. Short-circuit with a log message if no unit tests are required
```

**Stage 2: Integration Tests**
```
1. Triggered only when core service code changes (or forced via workflow_dispatch)
2. Requires exchange secrets; otherwise skipped automatically
3. Runs Gradle test task with `@integration` include tag
```

**Stage 3: Security**
```
1. OWASP Dependency Check
2. Vulnerability scanning
```

### 3.2 Setting Up Required Secrets

Add these secrets in GitHub repository settings:

**Go to:** Repository → Settings → Secrets and variables → Actions

**Required secrets:**

| Secret Name | Description | How to Get |
|-------------|-------------|------------|
| `BINANCE_TESTNET_API_KEY` | Binance testnet API key | https://testnet.binance.vision/ |
| `BINANCE_TESTNET_API_SECRET` | Binance testnet API secret | https://testnet.binance.vision/ |
| `BITGET_TESTNET_API_KEY` | Bitget testnet API key | https://www.bitget.com/testnet |
| `BITGET_TESTNET_API_SECRET` | Bitget testnet API secret | https://www.bitget.com/testnet |
| `CODECOV_TOKEN` | Codecov upload token (optional) | https://codecov.io |
| `SONAR_TOKEN` | SonarCloud token (optional) | https://sonarcloud.io |

**PowerShell commands to add secrets:**
```powershell
# Add GitHub secrets via CLI
gh secret set BINANCE_TESTNET_API_KEY --body "your_api_key_here"
gh secret set BINANCE_TESTNET_API_SECRET --body "your_api_secret_here"
gh secret set BITGET_TESTNET_API_KEY --body "your_api_key_here"
gh secret set BITGET_TESTNET_API_SECRET --body "your_api_secret_here"
```

---

## 4. Build Pipeline

### 4.1 Local Build Commands

```bash
# Navigate to project
cd 03_Development/Application_OnPremises

# Clean build
./gradlew clean

# Build all modules
./gradlew build

# Build without tests (faster)
./gradlew build -x test

# Build specific module
./gradlew :core-service:build

# Build and run
./gradlew :core-service:run
```

### 4.2 Build Profiles

**Development Build:**
```bash
./gradlew build -Pprofile=dev
```
- Debug symbols included
- Logging level: DEBUG
- No optimization

**Production Build:**
```bash
./gradlew build -Pprofile=prod
```
- Optimized bytecode
- Logging level: INFO
- Stripped debug symbols
- Minified JARs

### 4.3 Continuous Build (Watch Mode)

```bash
# Auto-rebuild on file changes
./gradlew --continuous build
```

---

## 5. Test Pipeline

### 5.1 Running Tests

```bash
# Run all unit tests
./gradlew test

# Run integration tests
./gradlew integrationTest

# Run all tests
./gradlew testAll

# Run specific test class
./gradlew test --tests "AITraderTest"

# Run with coverage
./gradlew test jacocoTestReport

# View coverage report
start build/reports/jacoco/test/html/index.html  # Windows
```

### 5.2 Test Categories

**Unit Tests** (Fast - seconds):
```bash
./gradlew test
```
- No external dependencies
- Mocked services
- Run on every commit

**Integration Tests** (Slower - minutes):
```bash
./gradlew integrationTest
```
- Real database (H2 in-memory)
- Exchange testnet APIs
- Run before merge

**E2E Tests** (Slowest - minutes):
```bash
./gradlew e2eTest
```
- Full application stack
- Real UI interactions
- Run before release

### 5.3 Test Coverage Requirements

| Module | Minimum | Target |
|--------|---------|--------|
| core-service | 85% | 90% |
| shared | 90% | 95% |
| desktop-ui | 75% | 80% |
| **Overall** | **80%** | **85%** |

---

## 6. Release Pipeline

### 6.1 Version Management

**Semantic Versioning:** `MAJOR.MINOR.PATCH`

- `MAJOR` - Breaking changes
- `MINOR` - New features (backward compatible)
- `PATCH` - Bug fixes

**Examples:**
- `1.0.0` - Initial release
- `1.1.0` - Add new feature
- `1.1.1` - Bug fix
- `2.0.0` - Breaking change

### 6.2 Creating a Release

**Step 1: Prepare release branch**
```bash
git checkout develop
git pull origin develop
git checkout -b release/v1.0.0
```

**Step 2: Update version numbers**
```bash
# Update version in build.gradle.kts
# Update CHANGELOG.md
# Update README.md

git add .
git commit -m "chore: prepare release v1.0.0"
git push origin release/v1.0.0
```

**Step 3: Merge to main**
```bash
# Create PR: release/v1.0.0 → main
gh pr create --base main --title "Release v1.0.0"

# After approval and merge:
git checkout main
git pull origin main
```

**Step 4: Create release tag**
```bash
git tag -a v1.0.0 -m "Release v1.0.0

Features:
- Complete AI Trading Engine
- Desktop UI
- Windows Service deployment
- Demo trading support

See CHANGELOG.md for details."

git push origin v1.0.0
```

**Step 5: Build release artifacts**
```bash
./gradlew clean build -Pprofile=prod

# Create installer (after implementing Inno Setup)
./gradlew createInstaller
```

**Step 6: Create GitHub Release**
```bash
gh release create v1.0.0 \
  --title "FMPS AutoTrader v1.0.0" \
  --notes-file CHANGELOG.md \
  --draft \
  ./build/distributions/FMPS-AutoTrader-1.0.0-setup.exe
```

**Step 7: Merge back to develop**
```bash
git checkout develop
git merge main
git push origin develop
```

---

## 7. Monitoring & Observability Pipeline

### 7.1 Application Logs

**Location:**
- Development: `./logs/autotrader-dev.log`
- Production: `C:\ProgramData\FMPS\AutoTrader\logs\`

**Log Levels:**
- `ERROR` - Errors requiring attention
- `WARN` - Warning messages
- `INFO` - Important events (default)
- `DEBUG` - Detailed debugging
- `TRACE` - Very detailed tracing

**Viewing logs:**
```powershell
# Tail logs in real-time
Get-Content -Path ".\logs\autotrader-dev.log" -Wait -Tail 50

# Filter errors
Get-Content ".\logs\autotrader-dev.log" | Select-String "ERROR"

# Export last hour
Get-Content ".\logs\autotrader-dev.log" | Select-Object -Last 1000 | Out-File "debug-export.txt"
```

### 7.2 Performance Metrics

**Collect metrics for:**
- API response times
- Database query performance
- Exchange API latency
- Trading decision time
- Memory usage
- CPU usage

**Metrics endpoint:**
```
GET http://localhost:8080/api/metrics
```

---

## 8. Database Migration Pipeline

### 8.1 Migration Strategy

Using **Flyway** for database migrations.

**Migration files:** `src/main/resources/db/migration/`

```
V1__initial_schema.sql
V2__add_patterns_table.sql
V3__add_performance_metrics.sql
```

### 8.2 Running Migrations

```bash
# Run migrations
./gradlew flywayMigrate

# Check migration status
./gradlew flywayInfo

# Validate migrations
./gradlew flywayValidate

# Repair migrations (if needed)
./gradlew flywayRepair
```

---

## 9. Documentation Pipeline

### 9.1 Auto-Generated Documentation

**API Documentation:**
```bash
# Generate OpenAPI spec
./gradlew generateOpenApiSpec

# Output: build/docs/openapi.json
```

**Code Documentation:**
```bash
# Generate Kotlin docs
./gradlew dokkaHtml

# Output: build/docs/kdoc/index.html
```

### 9.2 Documentation Sites

- **User Manual**: `docs/user-manual/`
- **Developer Docs**: `docs/developer/`
- **API Reference**: Auto-generated
- **Architecture Diagrams**: `docs/architecture/`

---

## 10. Quick Reference

### Common Commands

| Task | Command |
|------|---------|
| **Setup dev environment** | `.\setup-dev-environment.ps1` |
| **Build project** | `./gradlew build` |
| **Run tests** | `./gradlew test` |
| **Run integration tests** | `./gradlew integrationTest` |
| **Generate coverage** | `./gradlew jacocoTestReport` |
| **Run locally** | `./gradlew :core-service:run` |
| **Create feature branch** | `git checkout -b feature/issue-X-name` |
| **Create PR** | `gh pr create --base develop` |
| **Check CI status** | `gh pr checks` |
| **Create release** | `git tag -a v1.0.0` |
| **View logs** | `Get-Content .\logs\* -Wait` |

### Troubleshooting

**Build fails:**
```bash
./gradlew clean build --refresh-dependencies --info
```

**Tests fail:**
```bash
./gradlew test --tests "FailingTest" --debug-jvm
# Then attach debugger to port 5005
```

**CI/CD fails:**
```bash
# View detailed logs
gh run view

# Re-run failed jobs
gh run rerun
```

---

## Next Steps

1. ✅ Run `setup-dev-environment.ps1` to configure local environment
2. ✅ Add exchange API keys to `.env` file
3. ✅ Add GitHub secrets for CI/CD
4. ✅ Start with issue #1: Gradle setup
5. ✅ Follow the git workflow for all changes
6. ✅ Ensure CI/CD passes before merging

---

**Pipeline is ready! Start developing! 🚀**

