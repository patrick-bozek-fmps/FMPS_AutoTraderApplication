@echo off
REM Excel to Markdown Converter - Windows Batch Script
REM ==================================================

echo.
echo ============================================
echo Excel to Markdown Converter
echo ============================================
echo.

REM Check if Python is installed
python --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python is not installed or not in PATH
    echo Please install Python 3.8+ from https://www.python.org/
    pause
    exit /b 1
)

echo [INFO] Python found: 
python --version
echo.

REM Check if required packages are installed
echo [INFO] Checking dependencies...
python -c "import pandas, openpyxl, watchdog" >nul 2>&1
if errorlevel 1 (
    echo [WARN] Required packages not found. Installing...
    echo.
    REM Use python -m pip to avoid PATH issues
    python -m pip install -r requirements.txt
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

REM Run the converter
echo [INFO] Running converter...
echo.
python excel_to_markdown_converter.py %*

echo.
pause

