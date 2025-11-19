# Simple Binance Keys Checker
# Checks .env file for duplicate keys and verifies if they're testnet

$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$envFile = Join-Path $projectRoot ".env"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Binance Keys Verification" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

if (-not (Test-Path $envFile)) {
    Write-Host "❌ .env file not found at: $envFile" -ForegroundColor Red
    exit 1
}

Write-Host "📄 Reading .env file..." -ForegroundColor Yellow
$content = Get-Content $envFile

# Extract keys
$testnetKey = $null
$testnetSecret = $null
$prodKey = $null
$prodSecret = $null

$content | ForEach-Object {
    $line = $_.Trim()
    if ($line -match '^\s*BINANCE_TESTNET_API_KEY\s*=\s*(.+)$') {
        $testnetKey = $matches[1].Trim()
        if ($testnetKey -match '^["''](.*)["'']$') { $testnetKey = $matches[1] }
    }
    if ($line -match '^\s*BINANCE_TESTNET_API_SECRET\s*=\s*(.+)$') {
        $testnetSecret = $matches[1].Trim()
        if ($testnetSecret -match '^["''](.*)["'']$') { $testnetSecret = $matches[1] }
    }
    if ($line -match '^\s*BINANCE_API_KEY\s*=\s*(.+)$' -and $line -notmatch 'TESTNET') {
        $prodKey = $matches[1].Trim()
        if ($prodKey -match '^["''](.*)["'']$') { $prodKey = $matches[1] }
    }
    if ($line -match '^\s*BINANCE_API_SECRET\s*=\s*(.+)$' -and $line -notmatch 'TESTNET') {
        $prodSecret = $matches[1].Trim()
        if ($prodSecret -match '^["''](.*)["'']$') { $prodSecret = $matches[1] }
    }
}

Write-Host "`n📋 Found Keys:" -ForegroundColor Yellow

if ($testnetKey) {
    $preview = if ($testnetKey.Length -gt 15) { $testnetKey.Substring(0, 15) + "..." } else { $testnetKey }
    Write-Host "  ✅ BINANCE_TESTNET_API_KEY: $preview" -ForegroundColor Green
} else {
    Write-Host "  ❌ BINANCE_TESTNET_API_KEY: Not found" -ForegroundColor Red
}

if ($prodKey) {
    $preview = if ($prodKey.Length -gt 15) { $prodKey.Substring(0, 15) + "..." } else { $prodKey }
    Write-Host "  ✅ BINANCE_API_KEY: $preview" -ForegroundColor Green
} else {
    Write-Host "  ❌ BINANCE_API_KEY: Not found" -ForegroundColor Red
}

# Check for duplicates
Write-Host "`n🔍 Duplicate Check:" -ForegroundColor Yellow
if ($testnetKey -and $prodKey) {
    if ($testnetKey -eq $prodKey) {
        Write-Host "  ⚠️  WARNING: Both keys are IDENTICAL!" -ForegroundColor Red
        Write-Host "     BINANCE_TESTNET_API_KEY = BINANCE_API_KEY" -ForegroundColor Yellow
        Write-Host "`n  💡 This means:" -ForegroundColor Cyan
        Write-Host "     - You have the same key defined twice" -ForegroundColor White
        Write-Host "     - Only one set of variables is needed" -ForegroundColor White
        Write-Host "     - The key is likely TESTNET (since it's in TESTNET variable)" -ForegroundColor White
    } else {
        Write-Host "  ✅ Keys are different (good - no duplicates)" -ForegroundColor Green
        Write-Host "  ⚠️  You have BOTH testnet and production keys!" -ForegroundColor Yellow
        Write-Host "     Make sure you know which one is which!" -ForegroundColor Yellow
    }
}

# Test against testnet
Write-Host "`n🧪 Testing Key Against Binance Testnet..." -ForegroundColor Yellow
$keyToTest = if ($testnetKey) { $testnetKey } else { $prodKey }

if ($keyToTest) {
    try {
        $testnetUrl = "https://testnet.binance.vision/api/v3/ping"
        $headers = @{
            "X-MBX-APIKEY" = $keyToTest
        }
        
        Write-Host "   Connecting to: $testnetUrl" -ForegroundColor Gray
        
        $response = Invoke-WebRequest -Uri $testnetUrl -Method Get -Headers $headers -ErrorAction Stop -TimeoutSec 10
        
        if ($response.StatusCode -eq 200) {
            Write-Host "  ✅ SUCCESS: Key works with Binance TESTNET!" -ForegroundColor Green
            Write-Host "     These are TESTNET keys (SAFE to use)" -ForegroundColor Green
            Write-Host "`n  ✅ VERDICT: Safe to use for testing" -ForegroundColor Green
        }
    } catch {
        Write-Host "  ⚠️  Testnet connection failed" -ForegroundColor Yellow
        Write-Host "     Error: $($_.Exception.Message)" -ForegroundColor Gray
    }
} else {
    Write-Host "  ❌ No key found to test" -ForegroundColor Red
}

# Test against production (read-only)
Write-Host "`n🧪 Testing Key Against Binance Production (read-only)..." -ForegroundColor Yellow
Write-Host "   (This is safe - read-only endpoint)" -ForegroundColor Gray

if ($keyToTest) {
    try {
        $prodUrl = "https://api.binance.com/api/v3/ping"
        $headers = @{
            "X-MBX-APIKEY" = $keyToTest
        }
        
        Write-Host "   Connecting to: $prodUrl" -ForegroundColor Gray
        
        $response = Invoke-WebRequest -Uri $prodUrl -Method Get -Headers $headers -ErrorAction Stop -TimeoutSec 10
        
        if ($response.StatusCode -eq 200) {
            Write-Host "  ⚠️  WARNING: Key works with Binance PRODUCTION!" -ForegroundColor Red
            Write-Host "     These are PRODUCTION keys (REAL MONEY!)" -ForegroundColor Red
            Write-Host "`n  ⛔ DO NOT USE FOR TESTING!" -ForegroundColor Red
            Write-Host "     Get testnet keys from: https://testnet.binance.vision/" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "  ✅ Production connection failed (expected for testnet keys)" -ForegroundColor Green
    }
}

Write-Host "`n📝 Recommendations:" -ForegroundColor Cyan
if ($testnetKey -and $prodKey -and $testnetKey -eq $prodKey) {
    Write-Host "  1. Remove duplicate: Keep only BINANCE_TESTNET_API_KEY" -ForegroundColor White
    Write-Host "  2. Remove: BINANCE_API_KEY (it's the same as testnet)" -ForegroundColor White
    Write-Host "  3. Update load-env-file.ps1 to use BINANCE_TESTNET_API_KEY" -ForegroundColor White
} elseif ($testnetKey -and $prodKey) {
    Write-Host "  1. Verify which key is testnet vs production" -ForegroundColor White
    Write-Host "  2. Use BINANCE_TESTNET_API_KEY for testing" -ForegroundColor White
    Write-Host "  3. Keep BINANCE_API_KEY for production (if needed)" -ForegroundColor White
} else {
    Write-Host "  1. Use the key you have for testing" -ForegroundColor White
    Write-Host "  2. Make sure it's from testnet: https://testnet.binance.vision/" -ForegroundColor White
}

Write-Host "`nAnalysis complete!" -ForegroundColor Green

