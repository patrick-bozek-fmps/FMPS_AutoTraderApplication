# Defects Directory

**Purpose**: This directory contains all defect tracking documents for the FMPS AutoTrader Application.

---

## 📋 **Directory Structure**

```
Defects/
├── README.md (this file)
├── DEF_001_Brief_Description.md
├── DEF_002_Another_Defect_Description.md
└── ...
```

---

## 🎯 **Defect ID and File Naming Format**

### **Defect ID Format**
**Format**: `DEF_[#ID]` (sequential number, zero-padded to 3 digits)

**Examples**:
- `DEF_001` - First defect
- `DEF_023` - 23rd defect
- `DEF_156` - 156th defect

**Note**: The Epic information is tracked in the defect document itself (see "Epic" field in header), not in the ID or filename.

### **File Naming Convention**
**Format**: `DEF_[#ID]_Brief_Description.md`

**Rules**:
- **NO "Defect_" prefix** - The ID already indicates it's a defect
- Use underscores instead of spaces in the description
- Keep description brief (3-5 words maximum)
- Use title case for description words
- ID is zero-padded to 3 digits (001, 002, ..., 999)

**Examples**:
- ✅ `DEF_001_Server_Startup_Configuration_Mismatch.md`
- ✅ `DEF_015_Database_Connection_Timeout.md`
- ✅ `DEF_002_Memory_Leak_In_Telemetry.md`
- ❌ `Defect_DEF_001_Server_Startup_Configuration_Mismatch.md` (redundant "Defect_" prefix)
- ❌ `DEF_1_Server_Startup_Configuration_Mismatch.md` (ID not zero-padded)
- ❌ `DEF_001 Server Startup Configuration Mismatch.md` (spaces not allowed)

---

## 📝 **How to Create a New Defect Report**

1. **Copy the template**:
   - Source: `Development_Handbook/DEFECT_TRACKING_TEMPLATE.md`
   - Destination: `Development_Plan/Defects/DEF_[#ID]_Brief_Description.md`
   - **Important**: Use format `DEF_[#ID]_Brief_Description.md` where #ID is a sequential number (zero-padded to 3 digits, e.g., 001, 002, 023)
   - **NO "Defect_" prefix** - The ID already indicates it's a defect

2. **Fill in the defect details**:
   - Assign a unique defect ID
   - Fill in all relevant sections
   - Include steps to reproduce
   - Attach evidence (logs, screenshots)

3. **Set initial status**:
   - Status: 🆕 **NEW**
   - Assign appropriate severity and priority
   - Assign to developer if known

4. **Follow the workflow**:
   - See `DEVELOPMENT_WORKFLOW.md` for standard process
   - Update status as defect progresses
   - Document resolution and verification

---

## 🔄 **Defect Lifecycle**

```
🆕 NEW
  ↓
👤 ASSIGNED
  ↓
🏗️ IN PROGRESS
  ↓
✅ FIXED
  ↓
✔️ VERIFIED
  ↓
🔒 CLOSED
```

**Alternative paths**:
- ❌ **REJECTED** (if not a bug, duplicate, etc.)
- ⚠️ **REOPENED** (if fix doesn't work)
- ⏸️ **DEFERRED** (if moved to future release)

---

## 📊 **Defect Status Summary**

To get a quick overview of all defects:

```powershell
# Count defects by status (PowerShell)
Get-ChildItem *.md | Select-String -Pattern "^\*\*Status\*\*:" | Group-Object
```

---

## 🔗 **Related Documents**

- `Development_Handbook/DEFECT_TRACKING_TEMPLATE.md` - Template for defect reports
- `Development_Handbook/DEVELOPMENT_WORKFLOW.md` - Standard development workflow
- `Development_Handbook/TESTING_GUIDE.md` - Testing guidelines

---

**Last Updated**: November 18, 2025  
**Maintained By**: Software Process and DevOps Expert


