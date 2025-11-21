# GitHub Secrets Setup Guide for Integration Tests

**Purpose**: Configure API keys in GitHub Secrets to enable integration tests in CI/CD pipeline  
**Date**: November 21, 2025  
**Status**: Required for Integration Tests

---

## 📋 Overview

Integration tests require API keys for Binance and Bitget exchanges. These keys must be configured as GitHub Secrets to enable integration tests in the CI/CD pipeline.

---

## 🔐 Required Secrets

The following secrets must be configured in GitHub:

### **Binance API Keys**
- `BINANCE_API_KEY` - Binance testnet API key
- `BINANCE_API_SECRET` - Binance testnet API secret

### **Bitget API Keys**
- `BITGET_API_KEY` - Bitget testnet/demo API key
- `BITGET_API_SECRET` - Bitget testnet/demo API secret
- `BITGET_API_PASSPHRASE` - Bitget passphrase (required for Bitget)

---

## 📝 Step-by-Step Setup Instructions

### **Step 1: Navigate to Repository Settings**

1. Go to your GitHub repository:
   ```
   https://github.com/patrick-bozek-fmps/FMPS_AutoTraderApplication
   ```

2. Click on **Settings** (top menu bar)

3. In the left sidebar, click **Secrets and variables** → **Actions**

### **Step 2: Add Binance Secrets**

1. Click **New repository secret**

2. **Add BINANCE_API_KEY**:
   - **Name**: `BINANCE_API_KEY`
   - **Secret**: Your Binance testnet API key
   - Click **Add secret**

3. **Add BINANCE_API_SECRET**:
   - **Name**: `BINANCE_API_SECRET`
   - **Secret**: Your Binance testnet API secret
   - Click **Add secret**

### **Step 3: Add Bitget Secrets**

1. **Add BITGET_API_KEY**:
   - **Name**: `BITGET_API_KEY`
   - **Secret**: Your Bitget testnet/demo API key
   - Click **Add secret**

2. **Add BITGET_API_SECRET**:
   - **Name**: `BITGET_API_SECRET`
   - **Secret**: Your Bitget testnet/demo API secret
   - Click **Add secret**

3. **Add BITGET_API_PASSPHRASE**:
   - **Name**: `BITGET_API_PASSPHRASE`
   - **Secret**: Your Bitget passphrase
   - Click **Add secret**

### **Step 4: Verify Secrets**

After adding all secrets, you should see:

```
BINANCE_API_KEY        [Update] [Remove]
BINANCE_API_SECRET     [Update] [Remove]
BITGET_API_KEY         [Update] [Remove]
BITGET_API_SECRET      [Update] [Remove]
BITGET_API_PASSPHRASE  [Update] [Remove]
```

---

## 🔍 How to Get API Keys

### **Binance Testnet Keys**

1. Go to: https://testnet.binance.vision/
2. Sign in with GitHub OAuth
3. Navigate to **API Management**
4. Create a new API key (or use existing)
5. Copy:
   - **API Key** → `BINANCE_API_KEY`
   - **Secret Key** → `BINANCE_API_SECRET`

**Note**: Ensure keys are for **testnet**, not production!

### **Bitget Testnet/Demo Keys**

1. Go to: https://www.bitget.com/
2. Sign in to your account
3. Navigate to **API Management** (or similar)
4. Create a new API key for **testnet/demo** environment
5. Copy:
   - **API Key** → `BITGET_API_KEY`
   - **Secret Key** → `BITGET_API_SECRET`
   - **Passphrase** → `BITGET_API_PASSPHRASE` (set during key creation)

**Note**: Ensure keys are for **testnet/demo**, not production!

---

## ✅ Verification

### **After Adding Secrets**

1. **Trigger a Workflow Run**:
   - Go to **Actions** tab
   - Click **CI Pipeline**
   - Click **Run workflow** (dropdown)
   - Check **force-integration-tests**
   - Click **Run workflow**

2. **Check Integration Tests Job**:
   - Wait for workflow to start
   - Check the `integration-tests` job
   - Verify it runs (not skipped)
   - Check test output for successful execution

### **Expected Behavior**

**Before Secrets** (Current State):
```
Integration tests skipped: API keys not configured in GitHub secrets
```

**After Secrets** (Expected):
```
> Task :core-service:integrationTest
...
BitgetConnectorIntegrationTest > test connection() PASSED
BitgetConnectorIntegrationTest > test get candlesticks() PASSED
...
BUILD SUCCESSFUL
```

---

## 🔒 Security Best Practices

1. **Use Testnet Keys Only**: Never use production API keys in CI/CD
2. **Restrict Permissions**: When creating API keys, grant minimum required permissions
3. **Regular Rotation**: Rotate API keys periodically
4. **Monitor Usage**: Check API key usage logs regularly
5. **Never Commit Keys**: Keys should only exist in GitHub Secrets, never in code

---

## 🐛 Troubleshooting

### **Integration Tests Still Skipped**

**Symptom**: Tests show "Integration tests skipped: API keys not configured"

**Possible Causes**:
1. Secrets not added correctly
2. Secret names don't match exactly (case-sensitive)
3. Secrets added but workflow not re-run

**Solution**:
1. Verify secret names match exactly:
   - `BINANCE_API_KEY` (not `binance_api_key`)
   - `BINANCE_API_SECRET` (not `binance_api_secret`)
   - `BITGET_API_KEY` (not `bitget_api_key`)
   - `BITGET_API_SECRET` (not `bitget_api_secret`)
   - `BITGET_API_PASSPHRASE` (not `bitget_api_passphrase`)

2. Re-run workflow after adding secrets

### **Tests Fail with Authentication Errors**

**Symptom**: Tests run but fail with "Invalid API key" or "Authentication failed"

**Possible Causes**:
1. Wrong API keys (production instead of testnet)
2. Keys expired or revoked
3. Keys don't have required permissions

**Solution**:
1. Verify keys are for testnet environment
2. Check key status in exchange dashboard
3. Regenerate keys if needed

### **Bitget Tests Fail with "Passphrase Required"**

**Symptom**: Bitget tests fail with passphrase-related errors

**Solution**:
1. Ensure `BITGET_API_PASSPHRASE` secret is added
2. Verify passphrase matches the one set during API key creation
3. Passphrase is case-sensitive

---

## 📚 Related Documentation

- [DEVELOPMENT_WORKFLOW.md](../../Development_Handbook/DEVELOPMENT_WORKFLOW.md) - CI/CD workflow
- [INTEGRATION_TEST_ENVIRONMENT_REQUIREMENTS.md](./INTEGRATION_TEST_ENVIRONMENT_REQUIREMENTS.md) - Test environment setup
- [BITGET_HYBRID_TESTING_GUIDE.md](./BITGET_HYBRID_TESTING_GUIDE.md) - Bitget testing guide
- [BINANCE_TESTNET_SETUP_GUIDE.md](../../Artifacts/BINANCE_TESTNET_SETUP_GUIDE.md) - Binance testnet setup

---

## ✅ Checklist

- [ ] Binance testnet API key obtained
- [ ] Binance testnet API secret obtained
- [ ] Bitget testnet API key obtained
- [ ] Bitget testnet API secret obtained
- [ ] Bitget passphrase obtained
- [ ] All 5 secrets added to GitHub repository
- [ ] Secret names verified (exact match, case-sensitive)
- [ ] Workflow re-run to verify integration tests execute
- [ ] Integration tests pass successfully

---

**Last Updated**: November 21, 2025  
**Status**: Ready for Configuration

