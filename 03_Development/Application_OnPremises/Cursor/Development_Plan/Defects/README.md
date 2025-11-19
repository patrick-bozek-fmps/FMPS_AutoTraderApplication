# Defects Directory

**Purpose**: This directory contains all defect tracking documents for the FMPS AutoTrader Application.

---

## 📋 **Directory Structure**

```
Defects/
├── README.md (this file)
├── Defect_DEF-4-001_[Brief_Name].md
├── Defect_DEF-5-023_[Brief_Name].md
└── ...
```

---

## 🎯 **Defect ID Format**

**Format**: `DEF-[EPIC]-[SEQUENCE]`

**Examples**:
- `DEF-4-001` - First defect found in Epic 4
- `DEF-5-023` - 23rd defect found in Epic 5
- `DEF-GEN-001` - First general defect (not tied to specific epic)

---

## 📝 **How to Create a New Defect Report**

1. **Copy the template**:
   - Source: `Development_Handbook/DEFECT_TRACKING_TEMPLATE.md`
   - Destination: `Development_Plan/Defects/Defect_[ID]_[Brief_Name].md`

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


