# Setup Bitget API Keys - Permanent Configuration
# This script helps you set Bitget testnet API keys permanently on Windows

param(
    [string]$ApiKey = "",
    [string]$ApiSecret = "",
    [string]$ApiPassphrase = ""
)

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Bitget API Keys Setup" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Check if keys are already set
$existingKey = [System.Environment]::GetEnvironmentVariable("BITGET_API_KEY", "User")
$existingSecret = [System.Environment]::GetEnvironmentVariable("BITGET_API_SECRET", "User")
$existingPass = [System.Environment]::GetEnvironmentVariable("BITGET_API_PASSPHRASE", "User")

if ($existingKey -or $existingSecret -or $existingPass) {
    Write-Host "⚠️  WARNING: Bitget API keys are already configured!" -ForegroundColor Yellow
    Write-Host "`nCurrent values:" -ForegroundColor Yellow
    if ($existingKey) {
        Write-Host "  BITGET_API_KEY: $($existingKey.Substring(0, [Math]::Min(10, $existingKey.Length)))..." -ForegroundColor Gray
    }
    if ($existingSecret) {
        Write-Host "  BITGET_API_SECRET: $($existingSecret.Substring(0, [Math]::Min(10, $existingSecret.Length)))..." -ForegroundColor Gray
    }
    if ($existingPass) {
        Write-Host "  BITGET_API_PASSPHRASE: $($existingPass.Substring(0, [Math]::Min(10, $existingPass.Length)))..." -ForegroundColor Gray
    }
    Write-Host "`nDo you want to update them? (Y/N): " -ForegroundColor Yellow -NoNewline
    $response = Read-Host
    if ($response -ne "Y" -and $response -ne "y") {
        Write-Host "`n✅ Keeping existing keys. Exiting." -ForegroundColor Green
        exit 0
    }
}

# Get API keys from user if not provided as parameters
if ([string]::IsNullOrWhiteSpace($ApiKey)) {
    Write-Host "📝 Enter your Bitget Testnet API credentials:" -ForegroundColor Cyan
    Write-Host "`nNote: Get your API keys from Bitget testnet account" -ForegroundColor Gray
    Write-Host "      (Use testnet/demo mode, not production!)`n" -ForegroundColor Gray
    
    $ApiKey = Read-Host "Bitget API Key"
    if ([string]::IsNullOrWhiteSpace($ApiKey)) {
        Write-Host "`n❌ API Key cannot be empty. Exiting." -ForegroundColor Red
        exit 1
    }
}

if ([string]::IsNullOrWhiteSpace($ApiSecret)) {
    $ApiSecret = Read-Host "Bitget API Secret"
    if ([string]::IsNullOrWhiteSpace($ApiSecret)) {
        Write-Host "`n❌ API Secret cannot be empty. Exiting." -ForegroundColor Red
        exit 1
    }
}

if ([string]::IsNullOrWhiteSpace($ApiPassphrase)) {
    $ApiPassphrase = Read-Host "Bitget API Passphrase"
    if ([string]::IsNullOrWhiteSpace($ApiPassphrase)) {
        Write-Host "`n❌ API Passphrase cannot be empty. Exiting." -ForegroundColor Red
        exit 1
    }
}

# Set environment variables permanently (User scope)
Write-Host "`n🔧 Setting environment variables permanently..." -ForegroundColor Yellow

try {
    [System.Environment]::SetEnvironmentVariable("BITGET_API_KEY", $ApiKey, "User")
    [System.Environment]::SetEnvironmentVariable("BITGET_API_SECRET", $ApiSecret, "User")
    [System.Environment]::SetEnvironmentVariable("BITGET_API_PASSPHRASE", $ApiPassphrase, "User")
    
    Write-Host "✅ Environment variables set successfully!" -ForegroundColor Green
    Write-Host "`n📋 Summary:" -ForegroundColor Cyan
    Write-Host "  • BITGET_API_KEY: $($ApiKey.Substring(0, [Math]::Min(10, $ApiKey.Length)))..." -ForegroundColor White
    Write-Host "  • BITGET_API_SECRET: $($ApiSecret.Substring(0, [Math]::Min(10, $ApiSecret.Length)))..." -ForegroundColor White
    Write-Host "  • BITGET_API_PASSPHRASE: $($ApiPassphrase.Substring(0, [Math]::Min(10, $ApiPassphrase.Length)))..." -ForegroundColor White
    
    Write-Host "`n⚠️  IMPORTANT:" -ForegroundColor Yellow
    Write-Host "  • These keys are stored PERMANENTLY in your Windows user environment" -ForegroundColor White
    Write-Host "  • They will be available in all new PowerShell/Command Prompt sessions" -ForegroundColor White
    Write-Host "  • Current PowerShell session needs to be restarted to pick up the new variables" -ForegroundColor White
    Write-Host "  • Or run: `$env:BITGET_API_KEY = 'your_key'` in current session (temporary)" -ForegroundColor White
    
    Write-Host "`n🧪 To verify, restart PowerShell and run:" -ForegroundColor Cyan
    Write-Host "  echo `$env:BITGET_API_KEY" -ForegroundColor Gray
    Write-Host "  echo `$env:BITGET_API_SECRET" -ForegroundColor Gray
    Write-Host "  echo `$env:BITGET_API_PASSPHRASE" -ForegroundColor Gray
    
    Write-Host "`n✅ Setup complete! Keys are permanently stored." -ForegroundColor Green
    
} catch {
    Write-Host "`n❌ Error setting environment variables: $_" -ForegroundColor Red
    exit 1
}

