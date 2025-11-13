# Windows Service Deployment Guide

**Version**: 1.1  
**Last Updated**: November 13, 2025  
**Status**: Active  

---

## 1. Overview

The FMPS AutoTrader core service can run as a Windows service to provide 24/7 availability. This guide describes how to package, install, upgrade, and uninstall the service using Apache Procrun (prunsrv).

---

## 2. Prerequisites

- Windows 10 or Windows 11 (64-bit).
- Administrator privileges for installation.
- JDK/JRE 17 (or newer) installed (e.g., `C:\Program Files\Java\jdk-17`).
- Built distribution archive:  
  ```
  cd 03_Development/Application_OnPremises
  ./gradlew :core-service:distZip --no-daemon
  ```
- Apache Procrun executables (`prunsrv.exe`, optionally `prunmgr.exe`). Place them in `Cursor/Artifacts/windows-service/`.

---

## 3. Directory Layout

```
C:\Program Files\FMPSAutoTrader\CoreService\
├── app\                # Expanded Gradle distribution (core-service)
├── bin\                # Procrun service executable (ServiceName.exe)
├── config\             # service.conf, service-logging.xml, service.env
├── data\               # Database and persistence files
└── logs\               # Rolling service logs
```

---

## 4. Installation

1. Open **PowerShell (Run as Administrator)**.
2. Navigate to the artifacts directory:
   ```
   cd C:\PABLO\AI_Projects\FMPS_AutoTraderApplication\03_Development\Application_OnPremises\Cursor\Artifacts\windows-service
   ```
3. Run the installer:
   ```
   .\install-service.ps1 `
     -DistributionZip ..\..\..\core-service\build\distributions\core-service.zip `
     -PrunsrvPath .\prunsrv.exe `
     -InstallDir "C:\Program Files\FMPSAutoTrader\CoreService"
   ```
   Optional arguments:
   - `-ServiceName`, `-DisplayName`, `-Description`.
   - `-JvmPath` (path to `java.exe` if auto detection fails).
   - `-Force` (overwrite existing installation).
   - `-NoStart` (skip automatic start).

4. Update `config\service.conf` and `config\service.env` with environment-specific values (API keys, database connection, etc.).
5. Start the service (if not already running):
   ```
   .\start-service.ps1
   ```
6. Verify status:
   ```
   .\status-service.ps1
   ```

---

## 5. Configuration Files

- `config\service.conf` – HOCON overrides loaded in addition to `application.conf`.
- `config\service.env` – Environment variables injected by Procrun (key=value pairs, `#` comments).
- `config\service-logging.xml` – Logback configuration (rolling file and console appenders).

Environment variables automatically provided by the installer:

| Variable | Purpose |
|----------|---------|
| `FMPS_INSTALL_DIR` | Installation root. |
| `FMPS_DATA_DIR` | Data directory (default `%INSTALL%\data`). |
| `FMPS_LOG_DIR` | Log directory (default `%INSTALL%\logs`). |

---

## 6. Routine Operations

| Operation | Command |
|-----------|---------|
| Start service | `. \start-service.ps1` |
| Stop service | `. \stop-service.ps1` |
| Force stop | `. \stop-service.ps1 -Force` |
| Service status | `. \status-service.ps1` |
| View Windows service | `services.msc` |
| Open Procrun GUI (optional) | `prunmgr.exe //ES//FMPSAutoTraderCore` (if available) |

Logs are written to `logs\core-service.log` with daily/time-based rotation (up to 2 GB retained).

---

## 7. Upgrade Procedure

1. Stop the service: `. \stop-service.ps1`.
2. Run the installer with `-Force` to overwrite the installation (or supply a new `-InstallDir` for side-by-side deployment).
3. Review/merge configuration changes.
4. Start the service: `. \start-service.ps1`.
5. Verify status and logs.

---

## 8. Uninstall

```
.\uninstall-service.ps1 -InstallDir "C:\Program Files\FMPSAutoTrader\CoreService" -RemoveFiles
```

Omit `-RemoveFiles` to keep configuration/logs for forensic analysis.

---

## 9. Troubleshooting

| Symptom | Checklist |
|---------|-----------|
| Service fails to start | Check `logs\core-service.log` and Windows Event Viewer. Ensure `java.exe` path is correct. |
| API unreachable | Verify `service.conf` host/port, firewall rules, and that service status is `Running`. |
| Missing API key | Update `service.env` with `FMPS_API_KEY` and restart the service. |
| Database locked | Ensure only one instance is running and the data directory is writable. |
| Script requires elevation | Re-run PowerShell as Administrator. |

---

## 10. References

- [Apache Commons Daemon (Procrun)](https://commons.apache.org/proper/commons-daemon/procrun.html)
- FMPS AutoTrader `Development_Workflow.md`
- Issue documentation: `Issue_18_Windows_Service_Packaging.md`
- Status tracking: `EPIC_4_STATUS.md`, `Development_Plan_v2.md`
- Validation tracker below (update after each Ops dry run)

---

## 11. Validation Tracker (Ops)

| Date (Target) | Platform | Responsible | Status | Notes |
|---------------|----------|-------------|--------|-------|
| 2025-11-18 | Windows 10 (64-bit) | Ops Team | Scheduled | Run full install/start/stop checklist and log results here |
| 2025-11-19 | Windows 11 (64-bit) | Ops Team | Scheduled | Repeat validation; capture metrics/health checks |

> After each run, record command output, service status, and any remediation steps in this table and attach logs to the Operations share. Once both rows read `Completed`, notify the Release Manager to flip success criteria in Issue #18 to ✅.

---

**End of Guide**

