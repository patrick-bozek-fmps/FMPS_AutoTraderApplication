Param(
    [string]$ServiceName = "FMPSAutoTraderCore",
    [string]$DisplayName = "FMPS AutoTrader Core Service",
    [string]$Description = "FMPS AutoTrader core trading service",
    [string]$InstallDir = "C:\Program Files\FMPSAutoTrader\CoreService",
    [string]$DistributionZip = "..\..\..\core-service\build\distributions\core-service.zip",
    [string]$PrunsrvPath = "$PSScriptRoot\prunsrv.exe",
    [string]$JvmPath = "auto",
    [string]$ConfigTemplate = "$PSScriptRoot\service-config.template.conf",
    [string]$LogConfigTemplate = "$PSScriptRoot\service-logging.xml",
    [string]$EnvironmentFile = "$PSScriptRoot\env.example",
    [switch]$Force,
    [switch]$NoStart
)

function Assert-Administrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        throw "Administrator privileges are required. Re-run this script from an elevated PowerShell session."
    }
}

function Resolve-PathOrThrow([string]$Path, [string]$Description) {
    if (-not (Test-Path $Path)) {
        throw "$Description not found at '$Path'."
    }
    return (Resolve-Path $Path).Path
}

Assert-Administrator

$resolvedZip = Resolve-PathOrThrow -Path $DistributionZip -Description "Core service distribution zip"
$resolvedPrunsrv = Resolve-PathOrThrow -Path $PrunsrvPath -Description "prunsrv.exe (Apache Procrun)"
$resolvedConfigTemplate = Resolve-PathOrThrow -Path $ConfigTemplate -Description "Configuration template"
$resolvedLogTemplate = Resolve-PathOrThrow -Path $LogConfigTemplate -Description "Logging template"

$installRoot = $InstallDir.TrimEnd('\')
$appDir = Join-Path $installRoot "app"
$configDir = Join-Path $installRoot "config"
$logDir = Join-Path $installRoot "logs"
$binDir = Join-Path $installRoot "bin"
$dataDir = Join-Path $installRoot "data"

Write-Host "Preparing installation directory: $installRoot"
if (Test-Path $installRoot) {
    if ($Force) {
        Write-Warning "Removing existing installation directory '$installRoot' (Force switch supplied)."
        Remove-Item $installRoot -Recurse -Force
    }
}

New-Item -ItemType Directory -Path $appDir -Force | Out-Null
New-Item -ItemType Directory -Path $configDir -Force | Out-Null
New-Item -ItemType Directory -Path $logDir -Force | Out-Null
New-Item -ItemType Directory -Path $binDir -Force | Out-Null
New-Item -ItemType Directory -Path $dataDir -Force | Out-Null

$serviceExecutable = Join-Path $binDir ("{0}.exe" -f $ServiceName)
Copy-Item -Path $resolvedPrunsrv -Destination $serviceExecutable -Force

Write-Host "Expanding distribution zip to $appDir"
Expand-Archive -Path $resolvedZip -DestinationPath $appDir -Force

# Gradle application plugin expands to {appDir}\core-service\*
$appHome = Join-Path $appDir "core-service"
if (-not (Test-Path $appHome)) {
    throw "Expected application folder '$appHome' was not created. Verify the distribution zip."
}

$configPath = Join-Path $configDir "service.conf"
if (-not (Test-Path $configPath)) {
    Copy-Item -Path $resolvedConfigTemplate -Destination $configPath
    Write-Host "Created config file from template: $configPath"
} else {
    Write-Host "Config file already exists, leaving untouched: $configPath"
}

$logConfigPath = Join-Path $configDir "service-logging.xml"
if (-not (Test-Path $logConfigPath)) {
    Copy-Item -Path $resolvedLogTemplate -Destination $logConfigPath
    Write-Host "Created logging config: $logConfigPath"
} else {
    Write-Host "Logging config already exists, leaving untouched: $logConfigPath"
}

$envPath = Join-Path $configDir "service.env"
if (-not (Test-Path $envPath)) {
    if (Test-Path $EnvironmentFile) {
        Copy-Item -Path $EnvironmentFile -Destination $envPath
        Write-Host "Copied environment example to: $envPath"
    }
}

Write-Host "Installing Windows service '$ServiceName'"
$classPath = Join-Path $appHome "lib\*"
$jvmOptions = @(
    "-Dconfig.file=$configPath"
    "-Dlogback.configurationFile=$logConfigPath"
    "-Dfmps.install.dir=$installRoot"
    "-Dfmps.data.dir=$dataDir"
    "-Dfmps.log.dir=$logDir"
)

$installArgs = @(
    "//IS//$ServiceName",
    "--DisplayName=$DisplayName",
    "--Description=$Description",
    "--Startup=auto",
    "--StartMode=jvm",
    "--StopMode=process",
    "--Classpath=$classPath",
    "--StartClass=com.fmps.autotrader.core.MainKt",
    "--StartMethod=main",
    "--StartPath=$appHome",
    "--LogPath=$logDir",
    "--StdOutput=stdout.log",
    "--StdError=stderr.log",
    "--JvmOptions=" + ($jvmOptions -join ";"),
    "--StopTimeout=30",
    "--LogLevel=Info"
)

if ($JvmPath -and $JvmPath -ne "auto") {
    $installArgs += "--Jvm=$JvmPath"
} else {
    $installArgs += "--Jvm=auto"
}

$envEntries = @(
    "FMPS_INSTALL_DIR=$installRoot",
    "FMPS_DATA_DIR=$dataDir",
    "FMPS_LOG_DIR=$logDir"
)

if (Test-Path $envPath) {
    $envEntries += Get-Content $envPath | Where-Object { $_ -and -not $_.StartsWith("#") }
}

foreach ($entry in $envEntries) {
    if ($entry -match "^\s*([^=]+)\s*=\s*(.+)\s*$") {
        $key = $Matches[1].Trim()
        $value = $Matches[2].Trim()
        $installArgs += "--Environment=$key=$value"
    }
}

& $serviceExecutable $installArgs
if ($LASTEXITCODE -ne 0) {
    throw "prunsrv.exe failed to install the service (exit code $LASTEXITCODE)."
}

Write-Host "Service '$ServiceName' installed successfully."

if (-not $NoStart) {
    Write-Host "Starting service '$ServiceName'..."
    Start-Service -Name $ServiceName
    Start-Sleep -Seconds 2
    Get-Service -Name $ServiceName | Select-Object Name, Status, StartType
} else {
    Write-Host "NoStart switch supplied - service installed but not started."
}

Write-Host "Installation complete."

