# Setup Guide - Excel to Markdown Converter

**Important**: Python is required to run the Excel converter. Follow these steps to get started.

---

## 🚀 Quick Setup (5 minutes)

### Step 1: Install Python

1. **Download Python**:
   - Go to: https://www.python.org/downloads/
   - Download Python 3.11 or later (recommended)
   - Or direct link: https://www.python.org/ftp/python/3.11.6/python-3.11.6-amd64.exe

2. **Install Python**:
   - Run the downloaded installer
   - ⚠️ **IMPORTANT**: Check "Add Python to PATH" at the bottom
   - Click "Install Now"
   - Wait for installation to complete
   - Click "Close"

3. **Verify Installation**:
   - Open **Command Prompt** (Win+R, type `cmd`, press Enter)
   - Type: `python --version`
   - You should see: `Python 3.11.x`

### Step 2: Install Required Packages

Open Command Prompt and run:

```bash
# Navigate to the Cursor folder
cd C:\PABLO\AI_Projects\FMPS_AutoTraderApplication\03_Development\Application_OnPremises\Cursor

# Install required packages
pip install -r requirements.txt
```

Wait for installation to complete (about 1-2 minutes).

### Step 3: Run the Converter

**Option A: Double-click the batch file**
- Find `convert_excel.bat` in the Cursor folder
- Double-click it
- Done! Excel files will be converted to Markdown

**Option B: Use command line**
```bash
# Convert once
python excel_to_markdown_converter.py

# Or watch for changes
python excel_to_markdown_converter.py --watch
```

---

## 📋 Detailed Installation Steps

### For Windows 10/11

#### Method 1: Download from Python.org (Recommended)

1. **Download**:
   ```
   https://www.python.org/downloads/
   ```
   - Click the big yellow button "Download Python 3.x.x"

2. **Run Installer**:
   - Locate the downloaded file (usually in Downloads folder)
   - Double-click `python-3.x.x-amd64.exe`

3. **Installation Options**:
   ```
   ✅ Install launcher for all users (recommended)
   ✅ Add Python 3.x to PATH              ← CRITICAL!
   
   Then click: [Install Now]
   ```

4. **Wait for Installation**:
   - Takes about 2-3 minutes
   - You may need to allow administrator access

5. **Verify**:
   - Open new Command Prompt (important: NEW window)
   - Type: `python --version`
   - Should show: `Python 3.11.x` or similar

#### Method 2: Microsoft Store (Alternative)

1. **Open Microsoft Store**:
   - Press Windows key
   - Type "Microsoft Store"
   - Press Enter

2. **Search for Python**:
   - In Store search box, type "Python 3.11"
   - Select "Python 3.11"
   - Click "Get" or "Install"

3. **Wait for Installation**:
   - Automatic installation
   - Python will be added to PATH automatically

4. **Verify**:
   - Open Command Prompt
   - Type: `python --version`

### For macOS/Linux

**macOS**:
```bash
# Using Homebrew (install Homebrew first if needed)
brew install python@3.11

# Verify
python3 --version
```

**Linux (Ubuntu/Debian)**:
```bash
# Install Python
sudo apt update
sudo apt install python3 python3-pip

# Verify
python3 --version
```

---

## 🔧 Installing Dependencies

After Python is installed:

### Automatic Installation (Easiest)

Just double-click `convert_excel.bat` - it will:
1. Check if packages are installed
2. Install them automatically if missing
3. Run the converter

### Manual Installation

```bash
# Open Command Prompt
cd C:\PABLO\AI_Projects\FMPS_AutoTraderApplication\03_Development\Application_OnPremises\Cursor

# Install all packages at once
pip install -r requirements.txt
```

### Individual Package Installation

If the above fails, try installing one by one:

```bash
pip install pandas
pip install openpyxl
pip install watchdog
pip install tabulate
```

---

## 🐛 Troubleshooting

### Problem: "python is not recognized"

**Cause**: Python not in system PATH

**Solutions**:

**Solution 1**: Reinstall Python with PATH option
1. Uninstall Python (Settings > Apps > Python)
2. Download installer again
3. Make sure to check "Add Python to PATH"
4. Install

**Solution 2**: Add Python to PATH manually
1. Find Python installation folder (usually `C:\Users\[YourName]\AppData\Local\Programs\Python\Python3xx`)
2. Press Win+X, select "System"
3. Click "Advanced system settings"
4. Click "Environment Variables"
5. Under "System variables", find "Path"
6. Click "Edit"
7. Click "New"
8. Add Python path (e.g., `C:\Users\YourName\AppData\Local\Programs\Python\Python311`)
9. Add another for Scripts: `C:\Users\YourName\AppData\Local\Programs\Python\Python311\Scripts`
10. Click OK on all windows
11. Close and reopen Command Prompt

**Solution 3**: Use full path
```bash
# Find where Python is installed
where python

# Use full path
C:\Users\YourName\AppData\Local\Programs\Python\Python311\python.exe excel_to_markdown_converter.py
```

### Problem: "pip is not recognized"

**Solution**:
```bash
# Use Python to run pip
python -m pip install -r requirements.txt
```

### Problem: Permission denied during pip install

**Solution**:
```bash
# Install for current user only
pip install --user -r requirements.txt
```

### Problem: "No module named 'pandas'"

**Cause**: Packages not installed

**Solution**:
```bash
# Install packages
pip install pandas openpyxl watchdog tabulate

# Or use requirements file
pip install -r requirements.txt
```

### Problem: Converter runs but fails on Excel files

**Check**:
1. Are Excel files in `02_ReqMgn` folder?
2. Are they `.xlsx` format (not `.xls`)?
3. Are they not open in Excel?
4. Do you have read permissions?

**Try**:
```bash
# Test with specific file
python excel_to_markdown_converter.py --file "C:\path\to\your\file.xlsx"
```

---

## ✅ Verification Checklist

After setup, verify everything works:

```bash
# 1. Check Python
python --version
# Expected: Python 3.8.x or higher

# 2. Check pip
pip --version
# Expected: pip xx.x.x from ...

# 3. Check pandas
python -c "import pandas; print(pandas.__version__)"
# Expected: 2.x.x

# 4. Check openpyxl
python -c "import openpyxl; print(openpyxl.__version__)"
# Expected: 3.x.x

# 5. Check watchdog
python -c "import watchdog; print(watchdog.__version__)"
# Expected: 3.x.x

# 6. Test converter
python excel_to_markdown_converter.py --help
# Expected: Help message displayed
```

All commands should work without errors.

---

## 🎯 Next Steps After Setup

Once everything is installed:

### First-Time Use

1. **Navigate to Cursor folder**:
   ```bash
   cd C:\PABLO\AI_Projects\FMPS_AutoTraderApplication\03_Development\Application_OnPremises\Cursor
   ```

2. **Run initial conversion**:
   ```bash
   python excel_to_markdown_converter.py
   ```
   
   This will convert all Excel files in `02_ReqMgn` to Markdown.

3. **Check the output**:
   - Go to `02_ReqMgn` folder
   - You should see `.md` files next to each `.xlsx` file
   - Open them in a text editor to verify

### Development Workflow

For ongoing work with requirements:

1. **Start watch mode**:
   ```bash
   python excel_to_markdown_converter.py --watch
   ```

2. **Edit Excel files** as normal in Excel

3. **Save changes** - conversion happens automatically

4. **Check Markdown files** are updated

5. **Commit both** Excel and Markdown files to Git

---

## 📞 Getting Help

If you still have issues:

1. **Check error messages** carefully
2. **Google the specific error** message
3. **Check Python version**: Must be 3.8 or higher
4. **Try in a new Command Prompt** window
5. **Restart computer** after Python installation
6. **Contact support** with:
   - Python version (`python --version`)
   - pip version (`pip --version`)
   - Error message (full text)
   - Operating system version

---

## 🔄 Alternative: Manual Conversion

If you can't get Python working, you can manually convert Excel to Markdown:

### Option 1: Excel to CSV, then format
1. Open Excel file
2. Save As > CSV (Comma delimited)
3. Open CSV in text editor
4. Manually format as Markdown tables

### Option 2: Use Online Converters
- Search for "Excel to Markdown converter online"
- Upload your Excel file
- Download the Markdown output

### Option 3: Excel to HTML to Markdown
1. Save Excel as HTML
2. Use online HTML to Markdown converter
3. Clean up the result

**Note**: These methods are tedious and don't auto-update. The Python script is much better for ongoing work.

---

## 📚 Additional Resources

### Python Learning
- Official Tutorial: https://docs.python.org/3/tutorial/
- Python for Beginners: https://www.python.org/about/gettingstarted/

### Package Documentation
- Pandas: https://pandas.pydata.org/
- OpenPyXL: https://openpyxl.readthedocs.io/
- Watchdog: https://python-watchdog.readthedocs.io/

### Video Tutorials
- YouTube: "How to install Python on Windows"
- YouTube: "Python pip tutorial"

---

**Last Updated**: October 23, 2025  
**Version**: 1.0  
**For**: Windows 10/11

