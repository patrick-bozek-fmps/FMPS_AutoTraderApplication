@echo off
REM FMPS AutoTrader Desktop UI launcher (Windows placeholder)
SETLOCAL

REM Resolve repository root (one level up from module directory)
PUSHD "%~dp0.."
CALL gradlew.bat :desktop-ui:runDesktopWindows %*
POPD

ENDLOCAL


