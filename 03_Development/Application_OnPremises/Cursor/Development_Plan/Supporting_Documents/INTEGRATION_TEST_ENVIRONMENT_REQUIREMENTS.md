# Integration Test Environment Requirements

**Date**: November 19, 2025  
**Related**: Issue #25 - Integration Testing  
**Blocker**: #2b - Some Integration Tests Require Environment Setup

---

## 📋 **Overview**

This document details the environment setup requirements for running integration tests in the FMPS AutoTrader application. Integration tests are designed to validate end-to-end workflows and component interactions, requiring specific environment configurations.

---

## 🎯 **Test Categories & Requirements**

### **Category 1: Self-Contained Tests** ✅ **No Manual Setup Required**

These tests automatically set up their own environment:

| Test File | Auto-Setup | Requirements |
|-----------|------------|-------------|
| `E2ETraderWorkflowTest.kt` | ✅ Database, Mock Connectors | None - uses virtual money |
| `UICoreServiceIntegrationTest.kt` | ✅ Database, API Server | None - tests REST API only |
| `ErrorRecoveryTest.kt` | ✅ Database | None |
| `StatePersistenceTest.kt` | ✅ Database | None |
| `MultiTraderConcurrencyTest.kt` | ✅ Database | None - uses virtual money |

**Setup Details**:
- Tests use `TestUtilities.initTestDatabase()` to create isolated test databases
- Tests use `TestUtilities.createTestTraderConfig()` with `virtualMoney = true`
- No external services or API keys required
- **Total**: ~42 tests run without any environment setup

---

### **Category 2: WebSocket Tests** ⚠️ **Automatic Server Startup**

| Test File | Auto-Setup | Requirements |
|-----------|------------|-------------|
| `WebSocketIntegrationTest.kt` | ✅ Database, API Server | None - server starts automatically |

**Setup Details**:
- Test automatically starts API server using `TestUtilities.startTestApiServer()`
- Server runs on random available port (19000+)
- Database initialized automatically
- WebSocket client connects to test server
- **Total**: ~6 tests run without manual setup

---

### **Category 3: Exchange Connector Tests** 🔑 **Requires API Keys**

These tests require exchange testnet API credentials:

| Test File | Requirements | Environment Variables |
|-----------|--------------|----------------------|
| `ExchangeConnectorIntegrationTest.kt` | Binance/Bitget testnet API keys | See below |
| `BinanceConnectorIntegrationTest.kt` | Binance testnet API keys | `BINANCE_API_KEY`, `BINANCE_API_SECRET` |
| `BitgetConnectorIntegrationTest.kt` | Bitget testnet API keys | `BITGET_API_KEY`, `BITGET_API_SECRET`, `BITGET_API_PASSPHRASE` |

**Total**: ~8 tests require API keys (skipped gracefully if not available)

---

## 🔑 **Exchange API Keys Setup**

### **Binance Testnet**

**Required Environment Variables**:
- `BINANCE_API_KEY` - Your Binance testnet API key
- `BINANCE_API_SECRET` - Your Binance testnet API secret

**Setup Steps**:

1. **Create Binance Testnet Account**:
   - Visit: https://testnet.binance.vision/
   - Sign up for a testnet account (free, no real money)

2. **Generate API Keys**:
   - Log in to testnet account
   - Navigate to API Management
   - Create new API key (Key + Secret)
   - **Important**: Use testnet, not production!

3. **Set Environment Variables**:

   **PowerShell (Windows)**:
   ```powershell
   $env:BINANCE_API_KEY="your_testnet_api_key_here"
   $env:BINANCE_API_SECRET="your_testnet_api_secret_here"
   ```

   **Bash/Linux/Mac**:
   ```bash
   export BINANCE_API_KEY="your_testnet_api_key_here"
   export BINANCE_API_SECRET="your_testnet_api_secret_here"
   ```

   **Permanent Setup (Windows)**:
   ```powershell
   [System.Environment]::SetEnvironmentVariable("BINANCE_API_KEY", "your_key", "User")
   [System.Environment]::SetEnvironmentVariable("BINANCE_API_SECRET", "your_secret", "User")
   ```

4. **Verify Setup**:
   ```powershell
   # Check if variables are set
   echo $env:BINANCE_API_KEY
   echo $env:BINANCE_API_SECRET
   ```

---

### **Bitget Testnet**

**Required Environment Variables**:
- `BITGET_API_KEY` - Your Bitget testnet API key
- `BITGET_API_SECRET` - Your Bitget testnet API secret
- `BITGET_API_PASSPHRASE` - Your Bitget API passphrase (set when creating key)

**Setup Steps**:

1. **Create Bitget Testnet Account**:
   - Visit: https://www.bitget.com/ (use demo/testnet mode if available)
   - Sign up for testnet account

2. **Generate API Keys**:
   - Log in to Bitget account
   - Navigate to API Management
   - Create new API key
   - **Important**: Save the passphrase - it's required and cannot be retrieved!

3. **Set Environment Variables**:

   **PowerShell (Windows)**:
   ```powershell
   $env:BITGET_API_KEY="your_testnet_api_key_here"
   $env:BITGET_API_SECRET="your_testnet_api_secret_here"
   $env:BITGET_API_PASSPHRASE="your_passphrase_here"
   ```

   **Bash/Linux/Mac**:
   ```bash
   export BITGET_API_KEY="your_testnet_api_key_here"
   export BITGET_API_SECRET="your_testnet_api_secret_here"
   export BITGET_API_PASSPHRASE="your_passphrase_here"
   ```

4. **Verify Setup**:
   ```powershell
   echo $env:BITGET_API_KEY
   echo $env:BITGET_API_SECRET
   echo $env:BITGET_API_PASSPHRASE
   ```

---

## 🧪 **Running Integration Tests**

### **Run All Integration Tests** (No API Keys Required)

```powershell
# Run all integration tests (tests requiring API keys will be skipped)
.\gradlew :core-service:integrationTest
```

**Expected Result**:
- ✅ ~48 tests pass (self-contained + WebSocket)
- ⏭️ ~8 tests skipped (exchange connector tests - no API keys)

---

### **Run Exchange Connector Tests** (API Keys Required)

**Binance Tests**:
```powershell
# Set environment variables first
$env:BINANCE_API_KEY="your_key"
$env:BINANCE_API_SECRET="your_secret"

# Run Binance connector tests
.\gradlew :core-service:integrationTest --tests "*BinanceConnectorIntegrationTest*"
```

**Bitget Tests**:
```powershell
# Set environment variables first
$env:BITGET_API_KEY="your_key"
$env:BITGET_API_SECRET="your_secret"
$env:BITGET_API_PASSPHRASE="your_passphrase"

# Run Bitget connector tests
.\gradlew :core-service:integrationTest --tests "*BitgetConnectorIntegrationTest*"
```

**All Exchange Tests**:
```powershell
# Set all environment variables
$env:BINANCE_API_KEY="your_binance_key"
$env:BINANCE_API_SECRET="your_binance_secret"
$env:BITGET_API_KEY="your_bitget_key"
$env:BITGET_API_SECRET="your_bitget_secret"
$env:BITGET_API_PASSPHRASE="your_bitget_passphrase"

# Run all exchange connector tests
.\gradlew :core-service:integrationTest --tests "*ConnectorIntegrationTest*"
```

---

## 📊 **Test Execution Summary**

### **Without API Keys** (Default)

| Test Category | Tests | Status |
|---------------|-------|--------|
| E2E Workflow Tests | ~12 | ✅ Run |
| UI-Core Service Tests | ~8 | ✅ Run |
| WebSocket Tests | ~6 | ✅ Run |
| Error Recovery Tests | ~4 | ✅ Run |
| State Persistence Tests | ~6 | ✅ Run |
| Multi-Trader Tests | ~6 | ✅ Run |
| Exchange Connector Tests | ~8 | ⏭️ Skipped |
| **Total** | **~50** | **~42 Pass, ~8 Skipped** |

### **With API Keys** (Full Suite)

| Test Category | Tests | Status |
|---------------|-------|--------|
| All above tests | ~42 | ✅ Run |
| Exchange Connector Tests | ~8 | ✅ Run |
| **Total** | **~50** | **All Pass** |

---

## 🔍 **How Tests Handle Missing Environment**

### **Graceful Skipping**

Tests that require API keys use JUnit's `@EnabledIfEnvironmentVariable` annotation:

```kotlin
@Test
@EnabledIfEnvironmentVariable(named = "BINANCE_API_KEY", matches = ".+")
fun `should connect to Binance testnet`() {
    // Test only runs if BINANCE_API_KEY is set
}
```

**Behavior**:
- ✅ If API key is set: Test runs
- ⏭️ If API key is missing: Test is skipped (not a failure)
- ✅ Test suite still passes with skipped tests

### **Runtime Checks**

Tests also check for API keys at runtime:

```kotlin
val apiKey = System.getenv("BINANCE_API_KEY") ?: return@runBlocking
// Test exits early if key not found
```

**Behavior**:
- Tests check for keys in `@BeforeAll` setup
- Print helpful messages if keys are missing
- Tests are skipped with clear messages

---

## 🌐 **Network Requirements**

### **Required Network Access**

- ✅ **Internet Connection**: Required for exchange testnet connectivity
- ✅ **HTTPS Access**: Required for REST API calls to exchanges
- ✅ **WebSocket Access**: Required for real-time data streams (if testing WebSocket features)

### **Firewall Considerations**

If behind a corporate firewall:
- Ensure outbound HTTPS (port 443) is allowed
- Ensure WebSocket connections (wss://) are allowed
- Exchange testnet endpoints:
  - Binance: `https://testnet.binance.vision/`
  - Bitget: `https://www.bitget.com/` (testnet endpoints)

---

## 🗄️ **Database Requirements**

### **Automatic Database Setup**

All integration tests automatically:
- ✅ Create isolated test databases in `build/test-db/`
- ✅ Run Flyway migrations automatically
- ✅ Clean up databases after tests complete
- ✅ Use SQLite (no external database server required)

**No Manual Database Setup Required**

---

## 🖥️ **System Requirements**

### **Minimum Requirements**

- ✅ **Java 17+**: Required for running tests
- ✅ **Gradle**: Build tool (included in project)
- ✅ **Internet Connection**: For exchange connector tests
- ✅ **Disk Space**: ~100MB for test databases and logs

### **Operating System**

- ✅ **Windows 10/11**: Fully supported
- ✅ **Linux**: Supported
- ✅ **macOS**: Supported

---

## 📝 **CI/CD Environment Setup**

### **GitHub Actions**

Integration tests in CI require API keys to be configured as GitHub Secrets:

**Required Secrets**:
- `BINANCE_API_KEY`
- `BINANCE_API_SECRET`
- `BITGET_API_KEY`
- `BITGET_API_SECRET`
- `BITGET_API_PASSPHRASE`

**CI Behavior**:
- If secrets are configured: All integration tests run
- If secrets are missing: Exchange connector tests are skipped (not a failure)
- CI pipeline still passes with skipped tests

**See**: `.github/workflows/ci.yml` for CI configuration

---

## ✅ **Verification Checklist**

### **Before Running Integration Tests**

- [ ] Java 17+ installed and in PATH
- [ ] Gradle accessible (or use `gradlew`)
- [ ] Internet connection available
- [ ] (Optional) Binance testnet API keys set
- [ ] (Optional) Bitget testnet API keys set

### **Verify Environment Variables**

**PowerShell**:
```powershell
# Check all exchange API keys
Write-Host "Binance:" -ForegroundColor Cyan
Write-Host "  API Key: $($env:BINANCE_API_KEY -ne $null)"
Write-Host "  API Secret: $($env:BINANCE_API_SECRET -ne $null)"
Write-Host "Bitget:" -ForegroundColor Cyan
Write-Host "  API Key: $($env:BITGET_API_KEY -ne $null)"
Write-Host "  API Secret: $($env:BITGET_API_SECRET -ne $null)"
Write-Host "  Passphrase: $($env:BITGET_API_PASSPHRASE -ne $null)"
```

---

## 🎯 **Summary**

### **Environment Setup Required**

| Component | Setup Required | Manual Steps |
|-----------|----------------|-------------|
| **Database** | ❌ No | Automatic (SQLite test databases) |
| **API Server** | ❌ No | Automatic (test server startup) |
| **Binance API Keys** | ⚠️ Optional | 1. Create testnet account<br>2. Generate API keys<br>3. Set environment variables |
| **Bitget API Keys** | ⚠️ Optional | 1. Create testnet account<br>2. Generate API keys<br>3. Set environment variables |
| **Network** | ✅ Yes | Internet connection required |

### **Test Execution**

- ✅ **~42 tests** run without any environment setup
- ⏭️ **~8 tests** require API keys (skip gracefully if not available)
- ✅ **All tests** pass or skip gracefully - no failures due to missing environment

### **Conclusion**

Integration tests are designed to be **self-contained** and **graceful**:
- Most tests require **no manual setup**
- Exchange connector tests require **optional API keys**
- Tests **skip gracefully** if environment is not available
- **No blockers** for running the test suite

---

**Last Updated**: November 19, 2025  
**Related Documents**:
- `Issue_25_Integration_Testing.md` - Main issue documentation
- `Issue_25_BLOCKERS_AND_RESOLUTION.md` - Blocker #2b details
- `HOW_TO_RUN.md` - Application startup instructions

