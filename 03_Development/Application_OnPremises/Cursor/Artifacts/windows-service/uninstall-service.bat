@echo off
setlocal

set SCRIPT_DIR=%~dp0
powershell.exe -ExecutionPolicy Bypass -File "%SCRIPT_DIR%uninstall-service.ps1" %*

endlocal

