@echo off
REM Excel to Markdown Converter - Watch Mode
REM =========================================

echo.
echo ============================================
echo Excel to Markdown Converter - WATCH MODE
echo ============================================
echo.
echo This will monitor Excel files for changes
echo and automatically convert them to Markdown.
echo.
echo Press Ctrl+C to stop watching
echo.
echo ============================================
echo.

REM Run the converter in watch mode
python excel_to_markdown_converter.py --watch

pause

