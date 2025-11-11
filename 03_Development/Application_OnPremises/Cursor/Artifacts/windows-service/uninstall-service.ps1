Param(
    [string]$ServiceName = "FMPSAutoTraderCore",
    [string]$InstallDir = "C:\Program Files\FMPSAutoTrader\CoreService",
    [string]$PrunsrvPath = "$PSScriptRoot\prunsrv.exe",
    [switch]$RemoveFiles
)

function Assert-Administrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        throw "Administrator privileges are required. Re-run this script from an elevated PowerShell session."
    }
}

Assert-Administrator

$installRoot = $InstallDir.TrimEnd('\')
$serviceExecutable = Join-Path $installRoot ("bin\{0}.exe" -f $ServiceName)

if (Test-Path $serviceExecutable) {
    $prunsrvExec = $serviceExecutable
} elseif (Test-Path $PrunsrvPath) {
    $prunsrvExec = (Resolve-Path $PrunsrvPath).Path
} else {
    throw "Unable to locate prunsrv executable. Provide -PrunsrvPath pointing to prunsrv.exe."
}

Write-Host "Stopping service '$ServiceName' (if running)..."
$service = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if ($service) {
    if ($service.Status -eq [System.ServiceProcess.ServiceControllerStatus]::Running -or
        $service.Status -eq [System.ServiceProcess.ServiceControllerStatus]::StartPending) {
        Stop-Service -Name $ServiceName -Force
        $service.WaitForStatus('Stopped', '00:00:30')
    }
}

Write-Host "Removing service '$ServiceName'..."
& $prunsrvExec "//DS//$ServiceName"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to delete Windows service (exit code $LASTEXITCODE)."
}

if ($RemoveFiles -and (Test-Path $installRoot)) {
    Write-Warning "Removing installation directory '$installRoot'."
    Remove-Item $installRoot -Recurse -Force
}

Write-Host "Service '$ServiceName' removed successfully."

