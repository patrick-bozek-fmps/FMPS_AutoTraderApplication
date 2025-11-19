# Verify API Keys Configuration
# Checks if exchange API keys are properly configured

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  API Keys Verification" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Check Bitget keys
Write-Host "🔑 Bitget API Keys:" -ForegroundColor Yellow
$bitgetKey = [System.Environment]::GetEnvironmentVariable("BITGET_API_KEY", "User")
$bitgetSecret = [System.Environment]::GetEnvironmentVariable("BITGET_API_SECRET", "User")
$bitgetPass = [System.Environment]::GetEnvironmentVariable("BITGET_API_PASSPHRASE", "User")

if ($bitgetKey -and $bitgetSecret -and $bitgetPass) {
    Write-Host "  ✅ BITGET_API_KEY: Set ($($bitgetKey.Substring(0, [Math]::Min(10, $bitgetKey.Length)))...)" -ForegroundColor Green
    Write-Host "  ✅ BITGET_API_SECRET: Set ($($bitgetSecret.Substring(0, [Math]::Min(10, $bitgetSecret.Length)))...)" -ForegroundColor Green
    Write-Host "  ✅ BITGET_API_PASSPHRASE: Set ($($bitgetPass.Substring(0, [Math]::Min(10, $bitgetPass.Length)))...)" -ForegroundColor Green
    Write-Host "  Status: ✅ Complete" -ForegroundColor Green
} else {
    Write-Host "  ❌ BITGET_API_KEY: " -NoNewline -ForegroundColor Red
    if ($bitgetKey) { Write-Host "Set" -ForegroundColor Green } else { Write-Host "Not set" -ForegroundColor Red }
    Write-Host "  ❌ BITGET_API_SECRET: " -NoNewline -ForegroundColor Red
    if ($bitgetSecret) { Write-Host "Set" -ForegroundColor Green } else { Write-Host "Not set" -ForegroundColor Red }
    Write-Host "  ❌ BITGET_API_PASSPHRASE: " -NoNewline -ForegroundColor Red
    if ($bitgetPass) { Write-Host "Set" -ForegroundColor Green } else { Write-Host "Not set" -ForegroundColor Red }
    Write-Host "  Status: ❌ Incomplete" -ForegroundColor Red
}

Write-Host "`n🔑 Binance API Keys:" -ForegroundColor Yellow
$binanceKey = [System.Environment]::GetEnvironmentVariable("BINANCE_API_KEY", "User")
$binanceSecret = [System.Environment]::GetEnvironmentVariable("BINANCE_API_SECRET", "User")

if ($binanceKey -and $binanceSecret) {
    Write-Host "  ✅ BINANCE_API_KEY: Set ($($binanceKey.Substring(0, [Math]::Min(10, $binanceKey.Length)))...)" -ForegroundColor Green
    Write-Host "  ✅ BINANCE_API_SECRET: Set ($($binanceSecret.Substring(0, [Math]::Min(10, $binanceSecret.Length)))...)" -ForegroundColor Green
    Write-Host "  Status: ✅ Complete" -ForegroundColor Green
} else {
    Write-Host "  ❌ BINANCE_API_KEY: " -NoNewline -ForegroundColor Red
    if ($binanceKey) { Write-Host "Set" -ForegroundColor Green } else { Write-Host "Not set" -ForegroundColor Red }
    Write-Host "  ❌ BINANCE_API_SECRET: " -NoNewline -ForegroundColor Red
    if ($binanceSecret) { Write-Host "Set" -ForegroundColor Green } else { Write-Host "Not set" -ForegroundColor Red }
    Write-Host "  Status: ❌ Incomplete" -ForegroundColor Red
}

Write-Host "`n📝 Note:" -ForegroundColor Cyan
Write-Host "  • Keys are stored in Windows User environment (permanent)" -ForegroundColor Gray
Write-Host "  • Restart PowerShell to pick up new variables" -ForegroundColor Gray
Write-Host "  • Or use temporary session variables: `$env:KEY_NAME = 'value'" -ForegroundColor Gray

