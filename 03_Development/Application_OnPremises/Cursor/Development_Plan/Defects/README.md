# Defects Directory

**Purpose**: This directory contains all defect tracking documents for the FMPS AutoTrader Application.

---

## 📋 **Directory Structure**

```
Defects/
├── README.md (this file)
├── DEF-4-001_Brief_Description.md
├── DEF-5-023_Another_Defect_Description.md
└── ...
```

---

## 🎯 **Defect ID and File Naming Format**

### **Defect ID Format**
**Format**: `DEF-[EPIC]-[SEQUENCE]`

**Examples**:
- `DEF-4-001` - First defect found in Epic 4
- `DEF-5-023` - 23rd defect found in Epic 5
- `DEF-GEN-001` - First general defect (not tied to specific epic)

### **File Naming Convention**
**Format**: `DEF-[EPIC]-[SEQUENCE]_Brief_Description.md`

**Rules**:
- **NO "Defect_" prefix** - The ID already indicates it's a defect
- Use underscores instead of spaces in the description
- Keep description brief (3-5 words maximum)
- Use title case for description words

**Examples**:
- ✅ `DEF-6-001_Server_Startup_Configuration_Mismatch.md`
- ✅ `DEF-4-015_Database_Connection_Timeout.md`
- ✅ `DEF-GEN-002_Memory_Leak_In_Telemetry.md`
- ❌ `Defect_DEF-6-001_Server_Startup_Configuration_Mismatch.md` (redundant "Defect_" prefix)
- ❌ `DEF-6-001 Server Startup Configuration Mismatch.md` (spaces not allowed)

---

## 📝 **How to Create a New Defect Report**

1. **Copy the template**:
   - Source: `Development_Handbook/DEFECT_TRACKING_TEMPLATE.md`
   - Destination: `Development_Plan/Defects/DEF-[EPIC]-[SEQUENCE]_Brief_Description.md`
   - **Important**: Use format `DEF-[EPIC]-[SEQUENCE]_Brief_Description.md` (NO "Defect_" prefix)

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


