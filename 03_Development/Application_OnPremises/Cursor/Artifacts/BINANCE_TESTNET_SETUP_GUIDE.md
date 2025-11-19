# Binance Testnet Setup Guide

**Date**: November 19, 2025  
**Purpose**: Get testnet API keys for safe integration testing

---

## ⚠️ **Current Status: Testnet Under Maintenance**

**As of November 19, 2025**: The Binance Spot Test Network is currently under maintenance.

**What this means:**
- You may see a maintenance message when visiting the site
- API key creation may be temporarily unavailable
- Wait a few minutes and try again

**What to do:**
1. Check the testnet website: https://testnet.binance.vision/
2. If maintenance message appears, wait and retry later
3. Once operational, follow the steps below

---

## 🌐 **Step 1: Access Binance Testnet**

1. **Open your web browser**
2. **Navigate to**: https://testnet.binance.vision/
3. **Check status**:
   - ✅ If site loads normally → proceed to Step 2
   - ⚠️ If "under maintenance" message → wait and retry later

---

## 📝 **Step 2: Sign Up / Log In**

### **Current Testnet Registration Method:**

The Binance testnet currently uses **GitHub OAuth** for authentication.

### **If you don't have a testnet account:**

1. **Click "Log In with GitHub"** button
2. **Authorize Binance testnet** to access your GitHub account
   - You'll be redirected to GitHub
   - Click "Authorize" to grant permissions
3. **First-time users**: This will **automatically create** a new testnet account
4. **Returning users**: This will log you into your existing testnet account

**Note**: 
- You need a GitHub account to use the testnet
- If you don't have GitHub, create one at: https://github.com/signup
- The testnet account is linked to your GitHub account

### **Alternative Registration Methods:**

If "Log In with GitHub" doesn't work or you prefer email registration:

1. **Look for registration links**:
   - Check for "Sign Up" or "Register" link near the login button
   - Some sites show registration options after clicking login
   - Check the bottom of the page for alternative registration

2. **Try direct registration URLs**:
   - https://testnet.binance.vision/register
   - https://testnet.binance.vision/signup
   - https://testnet.binance.vision/auth/register

3. **Contact support** if registration is unavailable

### **If you already have a testnet account:**

1. **Click "Log In with GitHub"**
2. **Authorize** with the same GitHub account you used before
3. **You'll be logged in** to your existing testnet account

---

## 🔑 **Step 3: Generate API Keys**

### **Navigate to API Management:**

1. **After logging in**, look for:
   - **"API Management"** link in the menu
   - Or go directly to: https://testnet.binance.vision/api-keys
   - Or look for **"API"** or **"Keys"** in the user dashboard

2. **Click "Create API Key"** or **"Generate API Key"**

### **Configure API Key:**

1. **Enter a label/name** for your API key:
   - Example: "FMPS AutoTrader Testnet" or "Development Testing"
   - This helps you identify the key later

2. **Set permissions** (if asked):
   - For testing, you typically need:
     - ✅ **Read** (to fetch market data)
     - ✅ **Trading** (to place test orders)
   - ⚠️ **Do NOT enable "Withdraw"** - not needed for testing

3. **Complete security verification** (if required):
   - Email verification code
   - 2FA code (if enabled)

### **Save Your Keys:**

1. **API Key** will be displayed (looks like: `IRppI5SnUn6ooUd0woTy...`)
2. **API Secret** will be displayed (looks like: `VyynSKpdKUqEVUCmfKo2...`)
3. **⚠️ IMPORTANT**: 
   - **Copy both keys immediately**
   - **The secret is shown ONLY ONCE**
   - **You cannot retrieve it later**
   - **Save them in a secure location**

---

## ✅ **Step 4: Verify Testnet Keys**

### **Test Your Keys:**

1. **Testnet keys should ONLY work with:**
   - `https://testnet.binance.vision` ✅
   - `wss://testnet.binance.vision/ws` ✅

2. **Testnet keys should NOT work with:**
   - `https://api.binance.com` ❌ (production)
   - `wss://stream.binance.com` ❌ (production)

### **Quick Test (PowerShell):**

```powershell
# Set your testnet keys
$testnetKey = "your_testnet_api_key_here"
$testnetSecret = "your_testnet_api_secret_here"

# Test against testnet (should work)
try {
    $headers = @{ "X-MBX-APIKEY" = $testnetKey }
    $response = Invoke-WebRequest -Uri "https://testnet.binance.vision/api/v3/ping" -Headers $headers
    Write-Host "✅ Testnet connection: SUCCESS" -ForegroundColor Green
} catch {
    Write-Host "❌ Testnet connection: FAILED" -ForegroundColor Red
}

# Test against production (should FAIL for testnet keys)
try {
    $headers = @{ "X-MBX-APIKEY" = $testnetKey }
    $response = Invoke-WebRequest -Uri "https://api.binance.com/api/v3/ping" -Headers $headers
    Write-Host "⚠️  WARNING: Key works with production (not a testnet key!)" -ForegroundColor Red
} catch {
    Write-Host "✅ Production test failed (expected for testnet keys)" -ForegroundColor Green
}
```

---

## 📄 **Step 5: Update .env File**

### **Update Your .env File:**

1. **Open** `.env` file at project root:
   ```
   C:\PABLO\AI_Projects\FMPS_AutoTraderApplication\.env
   ```

2. **Replace the current Binance keys** with your new testnet keys:

   ```bash
   # Binance Testnet API Keys (for development/testing)
   BINANCE_TESTNET_API_KEY=your_new_testnet_api_key_here
   BINANCE_TESTNET_API_SECRET=your_new_testnet_api_secret_here
   
   # Remove or comment out the old production keys
   # BINANCE_API_KEY=old_production_key_here
   # BINANCE_API_SECRET=old_production_secret_here
   ```

3. **Remove duplicate entries:**
   - Keep only `BINANCE_TESTNET_API_KEY` and `BINANCE_TESTNET_API_SECRET`
   - Remove `BINANCE_API_KEY` and `BINANCE_API_SECRET` (or comment them out)

4. **Save the file**

---

## 🧪 **Step 6: Verify Setup**

### **Run Verification Script:**

```powershell
cd C:\PABLO\AI_Projects\FMPS_AutoTraderApplication\03_Development\Application_OnPremises

# Load .env and verify keys
. .\Cursor\Artifacts\load-env-file.ps1

# Check keys
.\Cursor\Artifacts\check-binance-keys-simple.ps1
```

### **Expected Results:**

- ✅ Keys work with `testnet.binance.vision`
- ❌ Keys do NOT work with `api.binance.com` (production)
- ✅ No duplicate keys in .env
- ✅ Only testnet keys are defined

---

## 🎯 **Step 7: Run Integration Tests**

Once keys are verified as testnet-only:

```powershell
# Load .env and run Binance integration tests
.\Cursor\Artifacts\run-integration-tests-with-env.ps1 -BinanceOnly
```

---

## ⚠️ **Important Notes**

### **Testnet vs Production:**

| Feature | Testnet | Production |
|---------|---------|------------|
| **URL** | `testnet.binance.vision` | `api.binance.com` |
| **Money** | ❌ Virtual (fake) | ✅ Real money |
| **Account** | Separate testnet account | Your real Binance account |
| **Keys** | Testnet-specific keys | Production keys |
| **Purpose** | Development/Testing | Real trading |

### **Security Best Practices:**

- ✅ **Always use testnet keys** for development
- ✅ **Never commit .env** to Git (already in `.gitignore`)
- ✅ **Testnet keys are safe** - no real money access
- ✅ **Rotate keys periodically** (good security practice)
- ❌ **Never share testnet keys** (even though they're "safe")

---

## 🆘 **Troubleshooting**

### **Problem: Can't find API Management page**

**Solution:**
- Look for "API" or "Keys" in the user menu/dashboard
- Try direct URL: https://testnet.binance.vision/api-keys
- Check if you're logged in to the correct account

### **Problem: Keys work with both testnet and production**

**Solution:**
- You may have production keys, not testnet keys
- Create new keys from testnet account
- Verify keys ONLY work with testnet endpoint

### **Problem: "Invalid API key" error**

**Solution:**
- Check if you copied the keys correctly (no extra spaces)
- Verify you're using testnet keys with testnet endpoint
- Make sure keys are from testnet account, not production

---

## 📚 **Additional Resources**

- **Binance Testnet**: https://testnet.binance.vision/
- **Testnet Documentation**: Check Binance testnet website for API docs
- **Testnet Faucet**: Some testnets provide free test coins (check website)

---

## ✅ **Checklist**

- [ ] Signed up/logged in to Binance testnet
- [ ] Generated new API keys from testnet account
- [ ] Saved API key and secret securely
- [ ] Verified keys work ONLY with testnet (not production)
- [ ] Updated .env file with testnet keys
- [ ] Removed duplicate/old production keys from .env
- [ ] Verified setup with verification script
- [ ] Ready to run integration tests

---

**Last Updated**: November 19, 2025  
**Status**: Ready for use

