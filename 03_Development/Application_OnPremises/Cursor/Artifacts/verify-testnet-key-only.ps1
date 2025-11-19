# Verify Binance API Key is Testnet-Only
# This script verifies that your API key ONLY works with testnet (not production)

param(
    [string]$ApiKey = ""
)

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Binance Testnet Key Verification" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Load from .env if not provided
if ([string]::IsNullOrWhiteSpace($ApiKey)) {
    Write-Host "📄 Loading API key from .env file..." -ForegroundColor Yellow
    $projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
    $envFile = Join-Path $projectRoot ".env"
    
    if (Test-Path $envFile) {
        $content = Get-Content $envFile
        $testnetKey = ($content | Where-Object { $_ -match '^\s*BINANCE_TESTNET_API_KEY\s*=' } | Select-Object -First 1) -replace '.*?=\s*(.+)$', '$1'
        $testnetKey = $testnetKey.Trim()
        if ($testnetKey -match '^["''](.*)["'']$') { $testnetKey = $matches[1] }
        
        if ($testnetKey) {
            $ApiKey = $testnetKey
            Write-Host "✅ Found key in .env file" -ForegroundColor Green
        } else {
            Write-Host "❌ BINANCE_TESTNET_API_KEY not found in .env" -ForegroundColor Red
            Write-Host "   Please provide API key as parameter or update .env file" -ForegroundColor Yellow
            exit 1
        }
    } else {
        Write-Host "❌ .env file not found" -ForegroundColor Red
        exit 1
    }
}

$keyPreview = if ($ApiKey.Length -gt 15) { $ApiKey.Substring(0, 15) + "..." } else { $ApiKey }
Write-Host "Testing key: $keyPreview`n" -ForegroundColor Gray

# Test 1: Testnet endpoint
Write-Host "🧪 Test 1: Testing against Binance TESTNET..." -ForegroundColor Yellow
Write-Host "   Endpoint: https://testnet.binance.vision/api/v3/ping" -ForegroundColor Gray

try {
    $headers = @{ "X-MBX-APIKEY" = $ApiKey }
    $response = Invoke-WebRequest -Uri "https://testnet.binance.vision/api/v3/ping" -Method Get -Headers $headers -TimeoutSec 10 -ErrorAction Stop
    
    if ($response.StatusCode -eq 200) {
        Write-Host "  ✅ SUCCESS: Key works with TESTNET" -ForegroundColor Green
        $testnetWorks = $true
    } else {
        Write-Host "  ❌ FAILED: Unexpected status code: $($response.StatusCode)" -ForegroundColor Red
        $testnetWorks = $false
    }
} catch {
    Write-Host "  ❌ FAILED: $($_.Exception.Message)" -ForegroundColor Red
    $testnetWorks = $false
}

Write-Host ""

# Test 2: Production endpoint
Write-Host "🧪 Test 2: Testing against Binance PRODUCTION..." -ForegroundColor Yellow
Write-Host "   Endpoint: https://api.binance.com/api/v3/ping" -ForegroundColor Gray
Write-Host "   (This should FAIL for testnet keys)" -ForegroundColor Gray

try {
    $headers = @{ "X-MBX-APIKEY" = $ApiKey }
    $response = Invoke-WebRequest -Uri "https://api.binance.com/api/v3/ping" -Method Get -Headers $headers -TimeoutSec 10 -ErrorAction Stop
    
    if ($response.StatusCode -eq 200) {
        Write-Host "  ⚠️  WARNING: Key ALSO works with PRODUCTION!" -ForegroundColor Red
        Write-Host "     This means it's a PRODUCTION key (not testnet-only)" -ForegroundColor Red
        $productionWorks = $true
    } else {
        Write-Host "  ✅ Expected: Key does NOT work with production" -ForegroundColor Green
        $productionWorks = $false
    }
} catch {
    Write-Host "  ✅ Expected: Key does NOT work with production" -ForegroundColor Green
    Write-Host "     Error: $($_.Exception.Message)" -ForegroundColor Gray
    $productionWorks = $false
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Verification Result" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

if ($testnetWorks -and -not $productionWorks) {
    Write-Host "✅ VERIFIED: This is a TESTNET-ONLY key!" -ForegroundColor Green
    Write-Host "   ✅ Works with testnet: YES" -ForegroundColor Green
    Write-Host "   ✅ Works with production: NO (correct)" -ForegroundColor Green
    Write-Host "`n✅ SAFE to use for integration testing!" -ForegroundColor Green
    exit 0
} elseif ($testnetWorks -and $productionWorks) {
    Write-Host "⚠️  WARNING: This is a PRODUCTION key!" -ForegroundColor Red
    Write-Host "   ✅ Works with testnet: YES" -ForegroundColor Yellow
    Write-Host "   ⚠️  Works with production: YES (PROBLEM!)" -ForegroundColor Red
    Write-Host "`n⛔ DO NOT USE for testing - This is a PRODUCTION key!" -ForegroundColor Red
    Write-Host "   Get a new testnet key from: https://testnet.binance.vision/" -ForegroundColor Yellow
    exit 1
} elseif (-not $testnetWorks) {
    Write-Host "❌ ERROR: Key does not work with testnet" -ForegroundColor Red
    Write-Host "   This might be:" -ForegroundColor Yellow
    Write-Host "   - An invalid key" -ForegroundColor Yellow
    Write-Host "   - A key with wrong permissions" -ForegroundColor Yellow
    Write-Host "   - Network connectivity issue" -ForegroundColor Yellow
    exit 1
} else {
    Write-Host "❓ UNKNOWN: Unable to determine key type" -ForegroundColor Yellow
    exit 2
}

