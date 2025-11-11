Param(
    [string]$ServiceName = "FMPSAutoTraderCore",
    [switch]$Force,
    [switch]$VerboseStatus
)

if ($Force) {
    Stop-Service -Name $ServiceName -Force
} else {
    Stop-Service -Name $ServiceName
}

if ($VerboseStatus) {
    Get-Service -Name $ServiceName | Select-Object Name, Status, StartType
}

Write-Host "Service '$ServiceName' stop command issued."

