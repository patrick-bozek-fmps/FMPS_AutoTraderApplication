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
