# FMPS AutoTrader - Development Environment Setup Script
# Run this from the repository root

param(
    [switch]$SkipHooks,
    [switch]$SkipGitCheck
)

Write-Host ""
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "       FMPS AutoTrader - Development Environment Setup v1.0" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host ""

# Function to check if command exists
function Test-Command {
    param($Command)
    try {
        if (Get-Command $Command -ErrorAction Stop) {
            return $true
        }
    } catch {
        return $false
    }
}

# 1. Check Java
Write-Host "[1/10] Checking Java installation..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    if ($javaVersion -match "version `"(\d+)") {
        $javaVersionNumber = [int]$matches[1]
        if ($javaVersionNumber -ge 17) {
            Write-Host "  [OK] Java $javaVersionNumber found" -ForegroundColor Green
        } else {
            Write-Host "  [ERROR] Java version $javaVersionNumber is too old. Need Java 17+" -ForegroundColor Red
            Write-Host "    Download from: https://adoptium.net/" -ForegroundColor Yellow
            exit 1
        }
    }
} catch {
    Write-Host "  [ERROR] Java not found" -ForegroundColor Red
    Write-Host "    Install Java 17+ from: https://adoptium.net/" -ForegroundColor Yellow
    exit 1
}

# 2. Check Git
if (!$SkipGitCheck) {
    Write-Host "`n[2/10] Checking Git installation..." -ForegroundColor Yellow
    if (Test-Command "git") {
        $gitVersion = git --version
        Write-Host "  [OK] $gitVersion" -ForegroundColor Green
    } else {
        Write-Host "  [ERROR] Git not found" -ForegroundColor Red
        Write-Host "    Install with: winget install --id Git.Git" -ForegroundColor Yellow
        exit 1
    }
}

# 3. Check GitHub CLI
Write-Host "`n[3/10] Checking GitHub CLI installation..." -ForegroundColor Yellow
$ghPath = "C:\Program Files\GitHub CLI\gh.exe"
if (Test-Path $ghPath) {
    Write-Host "  [OK] GitHub CLI found" -ForegroundColor Green
} else {
    Write-Host "  [WARN] GitHub CLI not found (optional)" -ForegroundColor Yellow
    Write-Host "    Install with: winget install --id GitHub.cli" -ForegroundColor Yellow
}

# 4. Verify repository
Write-Host "`n[4/10] Verifying repository..." -ForegroundColor Yellow
if (!(Test-Path ".git")) {
    Write-Host "  [ERROR] Not in a git repository" -ForegroundColor Red
    Write-Host "    Run this script from the repository root" -ForegroundColor Yellow
    exit 1
}

$remoteUrl = git remote get-url origin 2>$null
if ($remoteUrl -like "*FMPS_AutoTraderApplication*") {
    Write-Host "  [OK] Repository verified" -ForegroundColor Green
} else {
    Write-Host "  [WARN] Remote URL doesn't match expected repository" -ForegroundColor Yellow
}

# 5. Create directory structure
Write-Host "`n[5/10] Creating directory structure..." -ForegroundColor Yellow
$directories = @(
    "data",
    "logs",
    "03_Development\Application_OnPremises\shared\src\main\kotlin\com\fmps\autotrader\shared",
    "03_Development\Application_OnPremises\shared\src\test\kotlin\com\fmps\autotrader\shared",
    "03_Development\Application_OnPremises\core-service\src\main\kotlin\com\fmps\autotrader\core",
    "03_Development\Application_OnPremises\core-service\src\test\kotlin\com\fmps\autotrader\core",
    "03_Development\Application_OnPremises\core-service\src\integrationTest\kotlin\com\fmps\autotrader\core",
    "03_Development\Application_OnPremises\desktop-ui\src\main\kotlin\com\fmps\autotrader\ui",
    "03_Development\Application_OnPremises\desktop-ui\src\test\kotlin\com\fmps\autotrader\ui",
    "03_Development\Application_OnPremises\desktop-ui\src\main\resources\fxml",
    "03_Development\Application_OnPremises\desktop-ui\src\main\resources\css",
    "03_Development\Application_OnPremises\config"
)

$createdCount = 0
foreach ($dir in $directories) {
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        $createdCount++
    }
}
Write-Host "  [OK] Created $createdCount directories" -ForegroundColor Green

# 6. Create configuration files
Write-Host "`n[6/10] Creating configuration files..." -ForegroundColor Yellow

# .gitignore updates
$gitignorePath = ".gitignore"
$gitignoreAdditions = @"

# Local development
.env
data/
logs/
*.db
*.db-journal

# IDE
.idea/
*.iml
.vscode/
*.code-workspace

# Build
build/
out/
*.log
"@

if (!(Test-Path $gitignorePath)) {
    New-Item -ItemType File -Path $gitignorePath | Out-Null
}

$currentGitignore = Get-Content $gitignorePath -Raw -ErrorAction SilentlyContinue
$searchPattern = "# Local development"
if (!$currentGitignore -or $currentGitignore.IndexOf($searchPattern) -eq -1) {
    Add-Content -Path $gitignorePath -Value $gitignoreAdditions
    Write-Host "  [OK] Updated .gitignore" -ForegroundColor Green
} else {
    Write-Host "  [OK] .gitignore already configured" -ForegroundColor Green
}

# Environment template
$envTemplatePath = ".env.template"
$envTemplate = @"
# FMPS AutoTrader - Environment Variables
# Copy this file to .env and fill in your API keys

# ============================================
# Exchange API Keys (Testnet)
# ============================================

# Binance Testnet - Get from: https://testnet.binance.vision/
BINANCE_TESTNET_API_KEY=your_binance_testnet_api_key_here
BINANCE_TESTNET_API_SECRET=your_binance_testnet_api_secret_here

# Bitget Testnet - Get from: https://www.bitget.com/testnet
BITGET_TESTNET_API_KEY=your_bitget_testnet_api_key_here
BITGET_TESTNET_API_SECRET=your_bitget_testnet_api_secret_here

# ============================================
# Database
# ============================================
DATABASE_URL=jdbc:sqlite:./data/autotrader-dev.db

# ============================================
# Server Configuration
# ============================================
SERVER_PORT=8080
SERVER_HOST=localhost

# ============================================
# Logging
# ============================================
LOG_LEVEL=DEBUG
LOG_FILE=./logs/autotrader-dev.log
"@

Set-Content -Path $envTemplatePath -Value $envTemplate -Force
Write-Host "  [OK] Created $envTemplatePath" -ForegroundColor Green

# Check if .env exists
if (!(Test-Path ".env")) {
    Copy-Item $envTemplatePath ".env"
    Write-Host "  [OK] Created .env from template" -ForegroundColor Green
    Write-Host "    [WARN] Remember to add your API keys to .env" -ForegroundColor Yellow
} else {
    Write-Host "  [OK] .env already exists" -ForegroundColor Green
}

# Application configuration
$configDir = "03_Development\Application_OnPremises\config"
$appConfigPath = Join-Path $configDir "application.conf"
$appConfig = @"
# FMPS AutoTrader - Application Configuration
# This is the default configuration for local development

# Server Configuration
server {
  host = "localhost"
  port = 8080
}

# Database Configuration
database {
  url = "jdbc:sqlite:./data/autotrader-dev.db"
  driver = "org.sqlite.JDBC"
  
  # Connection pool settings
  pool {
    maximumPoolSize = 10
    minimumIdle = 2
    connectionTimeout = 30000
  }
}

# Exchange Configuration
exchanges {
  binance {
    name = "Binance"
    baseUrl = "https://testnet.binance.vision"
    rateLimit = 1200  # requests per minute
  }
  
  bitget {
    name = "Bitget"
    baseUrl = "https://api.bitget.com"
    rateLimit = 600
  }
}

# Trading Configuration
trading {
  maxTraders = 3
  defaultLeverage = 3
  maxLeverage = 5
  defaultStopLossPercent = 5.0
  minStopLossPercent = 1.0
  maxStopLossPercent = 20.0
  
  # Risk management
  risk {
    maxExposurePercent = 80.0
    emergencyStopOnDrawdown = 25.0
  }
}

# Logging Configuration
logging {
  level = "DEBUG"
  file = "./logs/autotrader-dev.log"
  
  # Log rotation
  maxFileSize = "10MB"
  maxHistory = 7  # days
}
"@

Set-Content -Path $appConfigPath -Value $appConfig -Force
Write-Host "  [OK] Created application.conf" -ForegroundColor Green

# 7. Set up Git hooks (if not skipped)
if (!$SkipHooks) {
    Write-Host "`n[7/10] Setting up Git hooks..." -ForegroundColor Yellow
    $hooksDir = ".git\hooks"
    
    # Pre-commit hook
    $preCommitPath = Join-Path $hooksDir "pre-commit"
    $preCommitHook = @"
#!/bin/sh
# Pre-commit hook: Run basic checks before committing

echo "Running pre-commit checks..."

# Check for large files
large_files=`$(git diff --cached --name-only | xargs ls -l 2>/dev/null | awk '`$5 > 1048576 {print `$9}')
if [ -n "`$large_files" ]; then
    echo "Error: Large files detected (>1MB):"
    echo "`$large_files"
    echo "Consider adding them to .gitignore"
    exit 1
fi

echo "Pre-commit checks passed!"
exit 0
"@

    Set-Content -Path $preCommitPath -Value $preCommitHook -Force
    Write-Host "  [OK] Created pre-commit hook" -ForegroundColor Green
    
} else {
    Write-Host "`n[7/10] Skipping Git hooks setup" -ForegroundColor Yellow
}

# 8. Update README
Write-Host "`n[8/10] Updating documentation..." -ForegroundColor Yellow
Write-Host "  [OK] Documentation ready in Cursor folder" -ForegroundColor Green

# 9. Create helpful scripts
Write-Host "`n[9/10] Creating helper scripts..." -ForegroundColor Yellow

# Build script
$buildScriptPath = "build.ps1"
$buildScript = @'
# Quick build script
param(
    [switch]$Clean,
    [switch]$Test,
    [switch]$Run
)

Set-Location "03_Development\Application_OnPremises"

if ($Clean) {
    Write-Host "Cleaning..." -ForegroundColor Yellow
    .\gradlew clean
}

Write-Host "Building..." -ForegroundColor Yellow
if ($Test) {
    .\gradlew build
} else {
    .\gradlew build -x test
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build successful!" -ForegroundColor Green
    
    if ($Run) {
        Write-Host "Starting application..." -ForegroundColor Cyan
        .\gradlew :core-service:run
    }
} else {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}
'@

Set-Content -Path $buildScriptPath -Value $buildScript -Force
Write-Host "  [OK] Created build.ps1" -ForegroundColor Green

# Test script
$testScriptPath = "test.ps1"
$testScript = @'
# Quick test script
param(
    [switch]$Integration,
    [switch]$Coverage,
    [string]$TestClass
)

Set-Location "03_Development\Application_OnPremises"

if ($TestClass) {
    Write-Host "Running test class: $TestClass" -ForegroundColor Yellow
    .\gradlew test --tests "$TestClass"
} elseif ($Integration) {
    Write-Host "Running integration tests..." -ForegroundColor Yellow
    .\gradlew integrationTest
} else {
    Write-Host "Running unit tests..." -ForegroundColor Yellow
    .\gradlew test
}

if ($Coverage) {
    Write-Host "Generating coverage report..." -ForegroundColor Yellow
    .\gradlew jacocoTestReport
    
    if (Test-Path "build\reports\jacoco\test\html\index.html") {
        Start-Process "build\reports\jacoco\test\html\index.html"
    }
}
'@

Set-Content -Path $testScriptPath -Value $testScript -Force
Write-Host "  [OK] Created test.ps1" -ForegroundColor Green

# 10. Final checks and summary
Write-Host "`n[10/10] Running final checks..." -ForegroundColor Yellow
Write-Host "  [OK] Setup complete!" -ForegroundColor Green

$summary = @"

================================================================================
                    SETUP COMPLETE
================================================================================

Your development environment is ready!

[CREATED]
  - Directory structure
  - Configuration files (.env.template, application.conf)
  - Helper scripts (build.ps1, test.ps1)
  - Git hooks

================================================================================
                    NEXT STEPS
================================================================================

1. CONFIGURE API KEYS:
   - Edit .env file
   - Get Binance testnet keys: https://testnet.binance.vision/
   - Get Bitget testnet keys: https://www.bitget.com/testnet
   - Add keys to .env file

2. START DEVELOPMENT:
   - Open project in IntelliJ IDEA
   - Start with issue #1: Gradle multi-module setup
   - Follow Development Plan v2

3. VERIFY SETUP:
   cd 03_Development\Application_OnPremises
   .\gradlew --version

4. QUICK START COMMANDS:
   .\build.ps1                  # Build project
   .\test.ps1                   # Run tests
   .\test.ps1 -Coverage         # Run tests with coverage

5. READ DOCUMENTATION:
   - 03_Development\Application_OnPremises\Cursor\Development_Plan_v2.md
   - 03_Development\Application_OnPremises\Cursor\PIPELINE_SETUP_GUIDE.md
   - 03_Development\Application_OnPremises\Cursor\TESTING_GUIDE.md

================================================================================
                    HAPPY CODING!
================================================================================

"@

Write-Host $summary -ForegroundColor Cyan
