@echo off
REM Diagnostic Script for Excel Converter
REM =======================================

echo.
echo ================================================
echo Excel to Markdown Converter - DIAGNOSTICS
echo ================================================
echo.
echo This script will check your Python environment
echo and help identify any issues.
echo.
echo ================================================
echo.

REM Check Python
echo [1/5] Checking Python installation...
python --version >nul 2>&1
if errorlevel 1 (
    echo [FAIL] Python is NOT installed or not in PATH
    echo.
    echo [FIX] Install Python from: https://www.python.org/downloads/
    echo       Make sure to check "Add Python to PATH" during installation
    echo.
    goto :end_with_error
) else (
    echo [OK] Python is installed:
    python --version
    echo.
)

REM Check Python location
echo [2/5] Checking Python location...
where python
echo.

REM Check pip
echo [3/5] Checking pip...
python -m pip --version >nul 2>&1
if errorlevel 1 (
    echo [FAIL] pip is NOT working
    echo.
    echo [FIX] Reinstall Python or run: python -m ensurepip --upgrade
    echo.
    goto :end_with_error
) else (
    echo [OK] pip is working:
    python -m pip --version
    echo.
)

REM Check packages
echo [4/5] Checking required packages...
echo.

python -c "import pandas" >nul 2>&1
if errorlevel 1 (
    echo [MISSING] pandas - Required for Excel reading
    set MISSING_PACKAGES=1
) else (
    python -c "import pandas; print('[OK] pandas version: ' + pandas.__version__)"
)

python -c "import openpyxl" >nul 2>&1
if errorlevel 1 (
    echo [MISSING] openpyxl - Required for Excel support
    set MISSING_PACKAGES=1
) else (
    python -c "import openpyxl; print('[OK] openpyxl version: ' + openpyxl.__version__)"
)

python -c "import watchdog" >nul 2>&1
if errorlevel 1 (
    echo [MISSING] watchdog - Required for file watching
    set MISSING_PACKAGES=1
) else (
    python -c "import watchdog; print('[OK] watchdog version: ' + watchdog.__version__)"
)

python -c "import tabulate" >nul 2>&1
if errorlevel 1 (
    echo [MISSING] tabulate - Required for table formatting
    set MISSING_PACKAGES=1
) else (
    python -c "import tabulate; print('[OK] tabulate version: ' + tabulate.__version__)"
)

echo.

if defined MISSING_PACKAGES (
    echo [ACTION REQUIRED] Some packages are missing
    echo.
    echo To install missing packages, run:
    echo    python -m pip install -r requirements.txt
    echo.
    goto :end_with_error
)

REM Check Excel files
echo [5/5] Checking for Excel files...
echo.
if exist "..\..\..\..\02_ReqMgn\*.xlsx" (
    echo [OK] Found Excel files in 02_ReqMgn:
    dir /b "..\..\..\..\02_ReqMgn\*.xlsx"
    echo.
) else (
    echo [WARN] No Excel files found in 02_ReqMgn folder
    echo        Make sure Excel files exist there
    echo.
)

REM Success
echo ================================================
echo [SUCCESS] All checks passed!
echo ================================================
echo.
echo Your environment is ready to run the converter.
echo.
echo Next steps:
echo   1. Run: python excel_to_markdown_converter.py
echo   2. Or double-click: convert_excel.bat
echo.
goto :end

:end_with_error
echo ================================================
echo [FAILED] Some issues were found
echo ================================================
echo.
echo Please fix the issues above and run this again.
echo See SETUP_GUIDE.md for detailed help.
echo.

:end
pause

