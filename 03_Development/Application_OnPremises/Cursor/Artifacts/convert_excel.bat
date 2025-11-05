@echo off
REM Excel to Markdown Converter - Windows Batch Script
REM ==================================================

echo.
echo ============================================
echo Excel to Markdown Converter
echo ============================================
echo.

REM Check if Python is installed (try py launcher first, then python)
py --version >nul 2>&1
if errorlevel 1 (
    python --version >nul 2>&1
    if errorlevel 1 (
        echo [ERROR] Python is not installed or not in PATH
        echo Please install Python 3.8+ from https://www.python.org/
        pause
        exit /b 1
    )
    set PYTHON_CMD=python
) else (
    set PYTHON_CMD=py
)

echo [INFO] Python found: 
%PYTHON_CMD% --version
echo.

REM Check if required packages are installed
echo [INFO] Checking dependencies...
%PYTHON_CMD% -c "import pandas, openpyxl, watchdog" >nul 2>&1
if errorlevel 1 (
    echo [WARN] Required packages not found. Installing...
    echo.
    REM Use py/python -m pip to avoid PATH issues
    %PYTHON_CMD% -m pip install -r requirements.txt
    if errorlevel 1 (
        echo [ERROR] Failed to install dependencies
        echo [ERROR] Make sure Python is properly installed
        pause
        exit /b 1
    )
    echo.
    echo [INFO] Dependencies installed successfully
    echo.
)

REM Run the converter (from project root with correct source path)
echo [INFO] Running converter...
echo.
cd ..\..\..\..\..
%PYTHON_CMD% 03_Development\Application_OnPremises\Cursor\Artifacts\excel_to_markdown_converter.py --source 02_ReqMgn %*

echo.
pause

