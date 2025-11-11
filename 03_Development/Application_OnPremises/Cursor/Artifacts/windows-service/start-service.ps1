Param(
    [string]$ServiceName = "FMPSAutoTraderCore",
    [switch]$VerboseStatus
)

Start-Service -Name $ServiceName

if ($VerboseStatus) {
    Get-Service -Name $ServiceName | Select-Object Name, Status, StartType
}

Write-Host "Service '$ServiceName' start command issued."

