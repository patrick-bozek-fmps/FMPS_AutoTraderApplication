# FMPS AutoTrader – Windows Service Packaging Guide

This folder contains helper scripts and templates for installing the FMPS AutoTrader core service as a Windows service using [Apache Procrun](https://commons.apache.org/proper/commons-daemon/procrun.html).

## Prerequisites

1. **Java Runtime**: Install a 64-bit JDK/JRE 17 (or newer) on the target host.
2. **Build the distribution**: From the project root run:
   ```
   cd 03_Development/Application_OnPremises
   ./gradlew :core-service:distZip --no-daemon
   ```
   The archive `core-service/build/distributions/core-service.zip` will be used by the installer.
3. **Download Apache Procrun**: Obtain `prunsrv.exe` from the Apache Commons Daemon distribution and place it in this directory (next to the scripts).
4. **Administrator shell**: Run installation scripts from an elevated PowerShell session.

## Files

| File | Description |
|------|-------------|
| `install-service.ps1` | Installs the Windows service, expands the distribution zip, and configures Apache Procrun. |
| `install-service.bat` | Convenience wrapper that forwards to `install-service.ps1`. |
| `uninstall-service.ps1` | Stops and removes the Windows service (optional removal of files). |
| `uninstall-service.bat` | Batch wrapper for `uninstall-service.ps1`. |
| `start-service.ps1` / `stop-service.ps1` / `status-service.ps1` | Utility scripts to manage the service via PowerShell. |
| `service-config.template.conf` | HOCON configuration template copied to `config\service.conf` during installation. |
| `service-logging.xml` | Logback configuration copied to `config\service-logging.xml`. |
| `env.example` | Example environment variables consumed by the installer (copied to `config\service.env`). |

## Installation Steps

1. Open **Windows PowerShell (Admin)**.
2. Navigate to this directory:
   ```
   cd C:\PABLO\AI_Projects\FMPS_AutoTraderApplication\03_Development\Application_OnPremises\Cursor\Artifacts\windows-service
   ```
3. Run the installer (adjust parameters if required):
   ```
   .\install-service.ps1 `
     -DistributionZip ..\..\..\core-service\build\distributions\core-service.zip `
     -PrunsrvPath .\prunsrv.exe `
     -InstallDir "C:\Program Files\FMPSAutoTrader\CoreService"
   ```
   Common optional parameters:
   - `-ServiceName`: Windows service name (default `FMPSAutoTraderCore`).
   - `-DisplayName`: Friendly display name.
   - `-JvmPath`: Full path to `java.exe` (defaults to `auto`).
   - `-Force`: Overwrites an existing installation directory.
   - `-NoStart`: Install but do not start the service immediately.

4. Update `C:\Program Files\FMPSAutoTrader\CoreService\config\service.conf` and `service.env` with production values (API keys, database connection, etc.).

5. Verify service status:
   ```
   .\status-service.ps1
   ```

## Uninstallation

Run:
```
.\uninstall-service.ps1 -InstallDir "C:\Program Files\FMPSAutoTrader\CoreService" -RemoveFiles
```
Omit `-RemoveFiles` if you want to preserve configuration/logs.

## Service Maintenance

- Start service:
  ```
  .\start-service.ps1
  ```
- Stop service:
  ```
  .\stop-service.ps1
  ```
- Check status:
  ```
  .\status-service.ps1
  ```

## Notes

- Environment variables defined in `service.env` are injected into the Windows service via Apache Procrun. Lines starting with `#` are ignored.
- Log files are written to `logs\` inside the installation directory by default (overridable via `FMPS_LOG_DIR`).
- The installer copies `prunsrv.exe` to `bin\<ServiceName>.exe`. You can safely remove `prunsrv.exe` from this folder after installation if desired.
- The installer registers JVM options for configuration and logging files and configures Procrun to terminate the JVM process on service stop.

