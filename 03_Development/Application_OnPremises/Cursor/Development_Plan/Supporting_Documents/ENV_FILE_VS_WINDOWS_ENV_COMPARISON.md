# .env File vs Windows Environment Variables - Comparison

**Date**: November 19, 2025  
**Context**: Setting up Bitget/Binance API keys for integration tests

---

## 📋 **Question: Which Approach is Better?**

You have Bitget API keys stored in `.env` file at project root. Should you use:
1. **.env file** (current approach)
2. **Windows Environment Variables** (permanent system-wide)

---

## 🔍 **Comparison**

| Aspect | .env File | Windows Environment Variables |
|--------|----------|-------------------------------|
| **Location** | Project root (`.env`) | Windows User/System registry |
| **Scope** | Project-specific | System-wide (all applications) |
| **Persistence** | File-based (persists) | Registry-based (persists) |
| **Portability** | ✅ Easy to move/copy | ❌ Tied to Windows machine |
| **Version Control** | ✅ Can be gitignored | ❌ Not in version control |
| **Security** | ✅ Project-specific, gitignored | ⚠️ System-wide, visible to all apps |
| **Multiple Projects** | ✅ Different .env per project | ❌ Shared across all projects |
| **Setup Complexity** | ✅ Simple (just load file) | ⚠️ Requires script or manual setup |
| **CI/CD** | ✅ Easy (load in CI) | ⚠️ Requires CI secrets configuration |
| **Team Sharing** | ✅ Share .env.template | ❌ Each developer sets individually |

---

## ✅ **Recommendation: Use .env File**

### **Why .env File is Better for Development:**

1. **Project Isolation**
   - Keys are project-specific
   - Different projects can have different keys
   - No conflicts with other applications

2. **Version Control Safety**
   - `.env` is in `.gitignore` (not committed)
   - `.env.template` can be committed (without real keys)
   - Team members can copy template and add their own keys

3. **Portability**
   - Easy to backup/restore
   - Can be moved to different machines
   - Works across different operating systems (with dotenv loader)

4. **Security**
   - Keys only available to this project
   - Not visible to other applications on the system
   - Can be encrypted if needed

5. **Flexibility**
   - Can have multiple .env files (`.env.dev`, `.env.test`, `.env.prod`)
   - Easy to switch between environments
   - Can be loaded conditionally

---

## 🔧 **Current Implementation**

### **How Tests Currently Work:**

Integration tests use `System.getenv()` to read environment variables:

```kotlin
val apiKey = System.getenv("BITGET_API_KEY")
val apiSecret = System.getenv("BITGET_API_SECRET")
val passphrase = System.getenv("BITGET_API_PASSPHRASE")
```

### **Solution: Load .env File Before Running Tests**

We've created a script that:
1. Reads `.env` file from project root
2. Sets environment variables for current PowerShell session
3. Runs integration tests with those variables

**Usage:**
```powershell
# Load .env and run all integration tests
.\Cursor\Artifacts\run-integration-tests-with-env.ps1

# Load .env and run only Bitget tests
.\Cursor\Artifacts\run-integration-tests-with-env.ps1 -BitgetOnly

# Load .env and run only Binance tests
.\Cursor\Artifacts\run-integration-tests-with-env.ps1 -BinanceOnly
```

---

## 🔒 **Security Considerations**

### **Testnet API Keys - Risk Assessment**

| Risk Level | Description |
|-----------|-------------|
| **LOW** | Testnet API keys have **no real money** access |
| **LOW** | Keys can be revoked/regenerated at any time |
| **LOW** | Testnet accounts are isolated from production |
| **MEDIUM** | Keys could be used to spam testnet (rate limiting applies) |
| **MEDIUM** | Keys visible to other processes if in system env |

### **Best Practices:**

1. **✅ Use .env File** (project-specific, gitignored)
2. **✅ Never Commit .env** (ensure it's in `.gitignore`)
3. **✅ Use Testnet Keys Only** (never production keys in .env)
4. **✅ Rotate Keys Periodically** (good security practice)
5. **✅ Use Different Keys Per Developer** (if working in team)

### **What NOT to Do:**

- ❌ Don't commit `.env` file to Git
- ❌ Don't use production API keys in `.env`
- ❌ Don't share `.env` file with others (share `.env.template` instead)
- ❌ Don't store keys in code or config files that are committed

---

## 📝 **.env File Format**

Your `.env` file should look like this:

```bash
# Bitget Testnet API Keys
BITGET_API_KEY=your_bitget_api_key_here
BITGET_API_SECRET=your_bitget_api_secret_here
BITGET_API_PASSPHRASE=your_bitget_passphrase_here

# Binance Testnet API Keys (optional)
BINANCE_API_KEY=your_binance_api_key_here
BINANCE_API_SECRET=your_binance_api_secret_here
```

**Note:** 
- No quotes needed (script handles them)
- Comments start with `#`
- Empty lines are ignored
- Variable names are case-sensitive

---

## 🚀 **Quick Start with .env File**

### **Step 1: Verify .env File**

```powershell
# Check if .env exists
Test-Path "C:\PABLO\AI_Projects\FMPS_AutoTraderApplication\.env"

# View .env (be careful - contains secrets!)
Get-Content "C:\PABLO\AI_Projects\FMPS_AutoTraderApplication\.env"
```

### **Step 2: Load .env and Run Tests**

```powershell
cd C:\PABLO\AI_Projects\FMPS_AutoTraderApplication\03_Development\Application_OnPremises

# Load .env file (sets variables for current session)
. .\Cursor\Artifacts\load-env-file.ps1

# Verify keys are loaded
echo $env:BITGET_API_KEY
echo $env:BITGET_API_SECRET
echo $env:BITGET_API_PASSPHRASE

# Run integration tests
.\Cursor\Artifacts\run-integration-tests-with-env.ps1
```

---

## 🔄 **Hybrid Approach (Optional)**

You can use both approaches:

1. **.env file** for development (project-specific, portable)
2. **Windows Environment Variables** for CI/CD or system-wide tools

**When to Use Each:**

- **.env File**: Development, local testing, project-specific
- **Windows Env Vars**: CI/CD pipelines, system-wide tools, permanent setup

---

## ✅ **Final Recommendation**

**Use .env File** because:
1. ✅ Better for development workflow
2. ✅ Project-specific (no conflicts)
3. ✅ Portable and easy to manage
4. ✅ More secure (not system-wide)
5. ✅ Team-friendly (each dev has their own .env)

**Scripts Created:**
- `load-env-file.ps1` - Loads .env file into current session
- `run-integration-tests-with-env.ps1` - Loads .env and runs tests

**Next Steps:**
1. Verify your `.env` file has correct format
2. Use `load-env-file.ps1` to load keys
3. Run integration tests with `run-integration-tests-with-env.ps1`

---

**Last Updated**: November 19, 2025

