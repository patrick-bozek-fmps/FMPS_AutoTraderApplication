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
