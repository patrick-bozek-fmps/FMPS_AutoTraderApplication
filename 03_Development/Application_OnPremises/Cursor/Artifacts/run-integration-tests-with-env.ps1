# Run integration tests with .env file loaded
# This script loads the .env file and then runs integration tests

param(
    [switch]$AllTests,
    [switch]$BitgetOnly,
    [switch]$BinanceOnly
)

$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$loadEnvScript = Join-Path $scriptPath "load-env-file.ps1"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Integration Tests Runner" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Load .env file
Write-Host "📄 Loading .env file..." -ForegroundColor Yellow
. $loadEnvScript

if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne $null) {
    Write-Host "`n❌ Failed to load .env file. Exiting." -ForegroundColor Red
    exit 1
}

Write-Host "`n🧪 Running integration tests..." -ForegroundColor Yellow

$testCommand = ".\gradlew :core-service:integrationTest --no-daemon"

if ($BitgetOnly) {
    $testCommand += " --tests `"*BitgetConnectorIntegrationTest*`""
    Write-Host "   Scope: Bitget connector tests only" -ForegroundColor Gray
} elseif ($BinanceOnly) {
    $testCommand += " --tests `"*BinanceConnectorIntegrationTest*`""
    Write-Host "   Scope: Binance connector tests only" -ForegroundColor Gray
} else {
    Write-Host "   Scope: All integration tests" -ForegroundColor Gray
}

Write-Host "`nExecuting: $testCommand`n" -ForegroundColor Cyan

Invoke-Expression $testCommand

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ Integration tests completed successfully!" -ForegroundColor Green
} else {
    Write-Host "`n❌ Integration tests failed (exit code: $LASTEXITCODE)" -ForegroundColor Red
    exit $LASTEXITCODE
}

