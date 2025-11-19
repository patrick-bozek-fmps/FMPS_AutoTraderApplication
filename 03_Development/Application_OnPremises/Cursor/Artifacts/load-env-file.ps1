# Load .env file and set environment variables
# This script reads the .env file from project root and sets environment variables
# Usage: . .\Cursor\Artifacts\load-env-file.ps1

$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$envFile = Join-Path $projectRoot ".env"

if (-not (Test-Path $envFile)) {
    Write-Host "❌ .env file not found at: $envFile" -ForegroundColor Red
    Write-Host "   Expected location: C:\PABLO\AI_Projects\FMPS_AutoTraderApplication\.env" -ForegroundColor Yellow
    return
}

Write-Host "📄 Loading .env file from: $envFile" -ForegroundColor Cyan

$envVarsSet = 0
$envVarsSkipped = 0

Get-Content $envFile | ForEach-Object {
    # Skip empty lines and comments
    if ($_ -match '^\s*#' -or $_ -match '^\s*$') {
        return
    }
    
    # Parse KEY=VALUE format
    if ($_ -match '^\s*([^#=]+)=(.*?)(\s*#.*)?$') {
        $key = $matches[1].Trim()
        $value = $matches[2].Trim()
        
        # Remove quotes if present
        if ($value -match '^["''](.*)["'']$') {
            $value = $matches[1]
        }
        
        # Set environment variable for current session
        [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
        Set-Item -Path "env:$key" -Value $value
        
        # Track which exchange API keys we're setting
        if ($key -match "BITGET|BINANCE") {
            Write-Host "  ✅ Set: $key" -ForegroundColor Green
            $envVarsSet++
        } else {
            $envVarsSkipped++
        }
    }
}

Write-Host "`n✅ Loaded $envVarsSet exchange API key variables from .env file" -ForegroundColor Green
if ($envVarsSkipped -gt 0) {
    Write-Host "   (Skipped $envVarsSkipped non-API-key variables)" -ForegroundColor Gray
}

Write-Host "`n📋 Current Exchange API Keys Status:" -ForegroundColor Cyan
$bitgetKey = $env:BITGET_API_KEY
$bitgetSecret = $env:BITGET_API_SECRET
$bitgetPass = $env:BITGET_API_PASSPHRASE
$binanceKey = $env:BINANCE_API_KEY
$binanceSecret = $env:BINANCE_API_SECRET

Write-Host "`n  Bitget:" -ForegroundColor Yellow
if ($bitgetKey -and $bitgetSecret -and $bitgetPass) {
    Write-Host "    Complete (Key, Secret, Passphrase)" -ForegroundColor Green
} else {
    Write-Host "    ⚠️  Incomplete" -ForegroundColor Yellow
    if (-not $bitgetKey) { Write-Host "      ❌ BITGET_API_KEY missing" -ForegroundColor Red }
    if (-not $bitgetSecret) { Write-Host "      ❌ BITGET_API_SECRET missing" -ForegroundColor Red }
    if (-not $bitgetPass) { Write-Host "      ❌ BITGET_API_PASSPHRASE missing" -ForegroundColor Red }
}

Write-Host "`n  Binance:" -ForegroundColor Yellow
if ($binanceKey -and $binanceSecret) {
    Write-Host "    Complete (Key, Secret)" -ForegroundColor Green
} else {
    Write-Host "    ⚠️  Incomplete" -ForegroundColor Yellow
    if (-not $binanceKey) { Write-Host "      ❌ BINANCE_API_KEY missing" -ForegroundColor Red }
    if (-not $binanceSecret) { Write-Host "      ❌ BINANCE_API_SECRET missing" -ForegroundColor Red }
}

Write-Host "`nNote: These variables are set for this PowerShell session only." -ForegroundColor Cyan
Write-Host "   To make them permanent, use: .\Cursor\Artifacts\setup-bitget-keys.ps1" -ForegroundColor Gray

