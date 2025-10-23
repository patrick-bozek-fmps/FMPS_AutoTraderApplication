# Excel to Markdown Converter

**Purpose**: Automatically converts Excel requirement files to Markdown format that can be read by AI assistants and easily reviewed in version control.

---

## 🎯 Features

- ✅ **Converts all sheets** in Excel workbooks to Markdown
- ✅ **Preserves table structure** with proper formatting
- ✅ **Auto-watch mode** - monitors files and auto-converts on changes
- ✅ **Handles multiple formats** - key-value pairs and tables
- ✅ **Smart filtering** - skips empty sheets and temporary files
- ✅ **Timestamped output** - tracks when conversion occurred

---

## 📋 Prerequisites

### Required Software
- **Python 3.8+** - [Download from python.org](https://www.python.org/)
- **pip** (usually comes with Python)

### Python Packages
The script will automatically install these if missing:
- `pandas` - Excel file reading
- `openpyxl` - Excel file support
- `watchdog` - File monitoring
- `tabulate` - Table formatting

---

## 🚀 Quick Start

### Option 1: Use Batch File (Windows - Easiest)

1. **Double-click** `convert_excel.bat`
   - Automatically checks dependencies
   - Installs packages if needed
   - Converts all Excel files once

2. **For watch mode**, double-click `convert_excel_watch.bat`
   - Monitors files for changes
   - Auto-converts when Excel files are modified

### Option 2: Command Line

```bash
# 1. Install dependencies (first time only)
pip install -r requirements.txt

# 2. Convert all Excel files once
python excel_to_markdown_converter.py

# 3. Watch for changes and auto-convert
python excel_to_markdown_converter.py --watch
```

---

## 📖 Usage Examples

### Convert All Excel Files Once
```bash
python excel_to_markdown_converter.py
```

This will:
- Find all `.xlsx` and `.xls` files in `02_ReqMgn/`
- Convert each file to Markdown
- Save `.md` files in the same directory

### Watch for Changes
```bash
python excel_to_markdown_converter.py --watch
```

This will:
- Convert all files initially
- Monitor the directory for changes
- Auto-convert when any Excel file is modified
- Run until you press `Ctrl+C`

### Convert Specific File
```bash
python excel_to_markdown_converter.py --file path/to/myfile.xlsx
```

### Custom Directories
```bash
# Specify different source and output directories
python excel_to_markdown_converter.py --source C:\MyExcelFiles --output C:\MarkdownOutput

# Watch a different directory
python excel_to_markdown_converter.py --source C:\MyExcelFiles --watch
```

---

## 📁 File Structure

After conversion, you'll have:

```
02_ReqMgn/
├── CommonDefinitionOfTermsAndAbbreviations.xlsx
├── CommonDefinitionOfTermsAndAbbreviations.md          ← Generated
├── FMPS_AutoTraderApplication_Customer_Specification.xlsx
├── FMPS_AutoTraderApplication_Customer_Specification.md ← Generated
├── FMPS_AutoTraderApplication_System_Specification.xlsx
├── FMPS_AutoTraderApplication_System_Specification.md   ← Generated
├── Template_BaseFunction_Specification.xlsx
└── Template_BaseFunction_Specification.md               ← Generated
```

---

## 🎨 Output Format

### Markdown Structure

Each generated Markdown file contains:

```markdown
# Filename

**Source File**: `original.xlsx`
**Converted**: 2025-10-23 14:30:00
**Auto-generated**: Do not edit manually

---

## Sheet 1 Name

| Column 1 | Column 2 | Column 3 |
|----------|----------|----------|
| Data 1   | Data 2   | Data 3   |

---

## Sheet 2 Name

**Key 1**: Value 1
**Key 2**: Value 2

---
```

### Table Formatting

- Standard tables are converted to GitHub-flavored Markdown tables
- Key-value pairs (2-column sheets) are formatted as bold key-value lists
- Empty rows are automatically removed
- Empty sheets are noted but not included

---

## ⚙️ Configuration

### Default Behavior

- **Source directory**: `02_ReqMgn/` (relative to project root)
- **Output directory**: Same as source
- **File patterns**: `*.xlsx`, `*.xls`
- **Ignored files**: Temporary files starting with `~$`
- **Debounce time**: 2 seconds (prevents duplicate conversions)

### Customizing

Edit the script constants if needed:

```python
# In excel_to_markdown_converter.py

# Change debounce time (line ~189)
if current_time - last_time < 2:  # Change this value

# Add more file patterns (line ~231)
for pattern in ['*.xlsx', '*.xls', '*.xlsm']:  # Add patterns
```

---

## 🐛 Troubleshooting

### Python Not Found
**Error**: `'python' is not recognized as an internal or external command`

**Solution**:
1. Install Python from [python.org](https://www.python.org/)
2. During installation, check "Add Python to PATH"
3. Restart your terminal/command prompt

### Package Installation Fails
**Error**: `pip install` fails or packages not found

**Solution**:
```bash
# Try upgrading pip first
python -m pip install --upgrade pip

# Then install packages
pip install -r requirements.txt

# Or install individually
pip install pandas openpyxl watchdog tabulate
```

### Excel File Locked
**Error**: Permission denied or file in use

**Solution**:
- Close the Excel file before converting
- The script ignores temporary Excel files (`~$...`)
- Wait a moment after saving before conversion

### Conversion Fails
**Error**: Conversion errors or corrupted output

**Solution**:
- Check that Excel file is not corrupted
- Ensure file is a valid `.xlsx` or `.xls` format
- Try opening the file in Excel to verify it works
- Check for special characters in sheet names

### Watch Mode Not Detecting Changes
**Problem**: File changes not triggering conversion

**Solution**:
- Make sure you're editing the right file
- Save the file after making changes
- The script has a 2-second debounce (wait a bit)
- Restart watch mode if issues persist

---

## 💡 Tips & Best Practices

### Development Workflow

1. **Initial Setup**:
   ```bash
   # Convert all files once to establish baseline
   python excel_to_markdown_converter.py
   ```

2. **During Development**:
   ```bash
   # Run in watch mode while working on requirements
   python excel_to_markdown_converter.py --watch
   ```

3. **Commit Both Files**:
   - Commit both `.xlsx` and `.md` files to Git
   - This allows AI assistants to read requirements
   - And humans can still edit Excel files

### Version Control

Add to `.gitattributes`:
```
*.xlsx binary
*.md text
```

Add to `.gitignore` (if you don't want to commit generated files):
```
02_ReqMgn/*.md
```

### Keeping in Sync

**Recommended**: Use watch mode during requirements development
```bash
cd 03_Development/Application_OnPremises/Cursor
python excel_to_markdown_converter.py --watch
```

This ensures Markdown files are always up-to-date with Excel changes.

---

## 🔧 Advanced Usage

### Integrating with CI/CD

Add to GitHub Actions workflow:

```yaml
name: Convert Excel Requirements

on:
  push:
    paths:
      - '02_ReqMgn/*.xlsx'

jobs:
  convert:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-python@v4
        with:
          python-version: '3.11'
      - name: Install dependencies
        run: pip install -r 03_Development/Application_OnPremises/Cursor/requirements.txt
      - name: Convert Excel files
        run: python 03_Development/Application_OnPremises/Cursor/excel_to_markdown_converter.py
      - name: Commit changes
        run: |
          git config --local user.email "action@github.com"
          git config --local user.name "GitHub Action"
          git add 02_ReqMgn/*.md
          git commit -m "Auto-update: Convert Excel requirements to Markdown" || exit 0
          git push
```

### Custom Processing

Extend the converter for custom needs:

```python
# Add custom sheet processing
def _custom_sheet_processor(self, sheet_name: str, df: pd.DataFrame) -> str:
    if 'glossary' in sheet_name.lower():
        # Custom formatting for glossary sheets
        return self._format_as_glossary(df)
    return self._format_as_table(df)
```

---

## 📝 Script Details

### Main Functions

- `convert_excel_to_markdown()` - Converts single Excel file
- `watch_directory()` - Monitors directory for changes
- `find_excel_files()` - Finds all Excel files in directory
- `convert_all_files()` - Batch converts all files

### Class Structure

- `ExcelToMarkdownConverter` - Main conversion logic
- `ExcelFileChangeHandler` - File system event handler

### Error Handling

- Gracefully handles missing files
- Reports conversion errors without stopping batch
- Validates file existence before conversion
- Debounces file system events

---

## 🤝 Contributing

If you improve the converter:

1. Test with various Excel file formats
2. Update this README with new features
3. Add examples for new functionality
4. Update version number in script header

---

## 📄 License

This script is part of the FMPS AutoTrader Application project.

---

## 🆘 Support

If you encounter issues:

1. Check the troubleshooting section above
2. Review error messages carefully
3. Ensure all prerequisites are installed
4. Check file permissions and paths
5. Create an issue on GitHub if problem persists

---

**Last Updated**: October 23, 2025  
**Version**: 1.0  
**Compatibility**: Windows 10/11, Python 3.8+

