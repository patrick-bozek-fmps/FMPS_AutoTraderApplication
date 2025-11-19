# Verify Binance API Keys - Testnet vs Production
# This script helps identify if Binance keys are testnet or production

param(
    [string]$ApiKey = "",
    [string]$ApiSecret = ""
)

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Binance API Keys Verification" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Load .env file if keys not provided
if ([string]::IsNullOrWhiteSpace($ApiKey) -or [string]::IsNullOrWhiteSpace($ApiSecret)) {
    Write-Host "📄 Loading .env file..." -ForegroundColor Yellow
    $projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
    $envFile = Join-Path $projectRoot ".env"
    
    if (Test-Path $envFile) {
        Get-Content $envFile | ForEach-Object {
            if ($_ -match '^\s*BINANCE_API_KEY\s*=\s*(.+)$' -and [string]::IsNullOrWhiteSpace($ApiKey)) {
                $ApiKey = $matches[1].Trim()
                if ($ApiKey -match '^["''](.*)["'']$') { $ApiKey = $matches[1] }
            }
            if ($_ -match '^\s*BINANCE_API_SECRET\s*=\s*(.+)$' -and [string]::IsNullOrWhiteSpace($ApiSecret)) {
                $ApiSecret = $matches[1].Trim()
                if ($ApiSecret -match '^["''](.*)["'']$') { $ApiSecret = $matches[1] }
            }
        }
    } else {
        Write-Host "❌ .env file not found at: $envFile" -ForegroundColor Red
        exit 1
    }
}

if ([string]::IsNullOrWhiteSpace($ApiKey) -or [string]::IsNullOrWhiteSpace($ApiSecret)) {
    Write-Host "❌ Binance API keys not found in .env file" -ForegroundColor Red
    Write-Host "   Expected variables: BINANCE_API_KEY, BINANCE_API_SECRET" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Found Binance API keys in .env file" -ForegroundColor Green
Write-Host "`n🔍 Analyzing keys..." -ForegroundColor Yellow

# Display key info (masked)
$keyPreview = if ($ApiKey.Length -gt 10) { $ApiKey.Substring(0, 10) + "..." } else { $ApiKey }
$secretPreview = if ($ApiSecret.Length -gt 10) { $ApiSecret.Substring(0, 10) + "..." } else { $ApiSecret }

Write-Host "  API Key: $keyPreview" -ForegroundColor White
Write-Host "  API Secret: $secretPreview" -ForegroundColor White

# Check for duplicates in .env
Write-Host "`n🔍 Checking for duplicate keys in .env..." -ForegroundColor Yellow
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$envFile = Join-Path $projectRoot ".env"
$content = Get-Content $envFile
$keyCount = ($content | Select-String -Pattern '^\s*BINANCE_API_KEY\s*=').Count
$secretCount = ($content | Select-String -Pattern '^\s*BINANCE_API_SECRET\s*=').Count

if ($keyCount -gt 1) {
    Write-Host "  ⚠️  WARNING: Found $keyCount BINANCE_API_KEY definitions!" -ForegroundColor Red
    Write-Host "     Only the LAST one will be used." -ForegroundColor Yellow
    Write-Host "`n  Duplicate lines:" -ForegroundColor Yellow
    $content | Select-String -Pattern '^\s*BINANCE_API_KEY\s*=' | ForEach-Object {
        $lineNum = $_.LineNumber
        $line = $_.Line.Trim()
        Write-Host "     Line $lineNum : $line" -ForegroundColor Gray
    }
}

if ($secretCount -gt 1) {
    Write-Host "  ⚠️  WARNING: Found $secretCount BINANCE_API_SECRET definitions!" -ForegroundColor Red
    Write-Host "     Only the LAST one will be used." -ForegroundColor Yellow
    Write-Host "`n  Duplicate lines:" -ForegroundColor Yellow
    $content | Select-String -Pattern '^\s*BINANCE_API_SECRET\s*=' | ForEach-Object {
        $lineNum = $_.LineNumber
        $line = $_.Line.Trim()
        Write-Host "     Line $secretCount : $line" -ForegroundColor Gray
    }
}

if ($keyCount -le 1 -and $secretCount -le 1) {
    Write-Host "  ✅ No duplicates found" -ForegroundColor Green
}

# Test the keys against Binance testnet
Write-Host "`n🧪 Testing keys against Binance testnet..." -ForegroundColor Yellow
Write-Host "   (This is SAFE - we're only testing connectivity)" -ForegroundColor Gray

try {
    # Create a simple test request to Binance testnet
    $testnetUrl = "https://testnet.binance.vision/api/v3/ping"
    $headers = @{
        "X-MBX-APIKEY" = $ApiKey
    }
    
    Write-Host "   Connecting to: $testnetUrl" -ForegroundColor Gray
    
    $response = Invoke-WebRequest -Uri $testnetUrl -Method Get -Headers $headers -ErrorAction SilentlyContinue -TimeoutSec 10
    
    if ($response.StatusCode -eq 200) {
        Write-Host "  ✅ SUCCESS: Keys work with Binance TESTNET" -ForegroundColor Green
        Write-Host "     These are TESTNET keys (safe to use)" -ForegroundColor Green
        return 0
    }
} catch {
    Write-Host "  ⚠️  Testnet connection failed: $($_.Exception.Message)" -ForegroundColor Yellow
    Write-Host "     This might mean:" -ForegroundColor Yellow
    Write-Host "     - Keys are for production (not testnet)" -ForegroundColor Yellow
    Write-Host "     - Keys are invalid" -ForegroundColor Yellow
    Write-Host "     - Network issue" -ForegroundColor Yellow
}

# Test against production (read-only endpoint)
Write-Host "`n🧪 Testing keys against Binance PRODUCTION (read-only)..." -ForegroundColor Yellow
Write-Host "   (This is SAFE - we're only using a read-only endpoint)" -ForegroundColor Gray

try {
    $prodUrl = "https://api.binance.com/api/v3/ping"
    $headers = @{
        "X-MBX-APIKEY" = $ApiKey
    }
    
    Write-Host "   Connecting to: $prodUrl" -ForegroundColor Gray
    
    $response = Invoke-WebRequest -Uri $prodUrl -Method Get -Headers $headers -ErrorAction SilentlyContinue -TimeoutSec 10
    
    if ($response.StatusCode -eq 200) {
        Write-Host "  ⚠️  WARNING: Keys work with Binance PRODUCTION!" -ForegroundColor Red
        Write-Host "     These are PRODUCTION keys (REAL MONEY!)" -ForegroundColor Red
        Write-Host "`n  ⛔ DO NOT USE THESE KEYS FOR TESTING!" -ForegroundColor Red
        Write-Host "     Use testnet keys from: https://testnet.binance.vision/" -ForegroundColor Yellow
        return 1
    }
} catch {
    Write-Host "  ✅ Production connection failed (expected for testnet keys)" -ForegroundColor Green
}

Write-Host "`n❓ Unable to determine key type automatically" -ForegroundColor Yellow
Write-Host "`n📋 Manual Verification Steps:" -ForegroundColor Cyan
Write-Host "  1. Check where you got the keys:" -ForegroundColor White
Write-Host "     • Testnet: https://testnet.binance.vision/" -ForegroundColor Green
Write-Host "     • Production: https://www.binance.com/ (API Management)" -ForegroundColor Red
Write-Host "`n  2. Check the API key name/description:" -ForegroundColor White
Write-Host "     • Testnet keys usually have 'testnet' in the name" -ForegroundColor Green
Write-Host "     • Production keys are for real trading" -ForegroundColor Red
Write-Host "`n  3. If unsure, create NEW testnet keys:" -ForegroundColor White
Write-Host "     - Visit: https://testnet.binance.vision/" -ForegroundColor Cyan
Write-Host "     - Sign up for testnet account (free)" -ForegroundColor Cyan
Write-Host "     - Generate new API keys" -ForegroundColor Cyan

return 2

