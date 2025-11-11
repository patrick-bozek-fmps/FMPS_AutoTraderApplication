Param(
    [string]$ServiceName = "FMPSAutoTraderCore"
)

Get-Service -Name $ServiceName | Select-Object Name, Status, StartType, ServiceType

