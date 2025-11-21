# How to Verify Desktop UI Tabs Are Working

## Prerequisites

1. **Core Service must be running** on `http://localhost:8080`
2. **Desktop UI** must be started

## Verification Steps

### Step 1: Start Core Service

```powershell
cd 03_Development\Application_OnPremises
.\gradlew :core-service:run
```

**Expected Output:**
```
✓ Database initialized successfully
✓ REST API server started on http://0.0.0.0:8080
✓ Core service is running
```

### Step 2: Start Desktop UI (in a separate terminal)

```powershell
cd 03_Development\Application_OnPremises
.\gradlew :desktop-ui:run
```

**Expected Output:**
- Desktop UI window should open
- You should see the navigation sidebar on the left
- Dashboard view should be displayed by default

### Step 3: Verify Each Tab

#### ✅ Dashboard Tab (Should work - already working)
1. Click on "Dashboard" or "Overview" in the navigation sidebar
2. **Expected**: Dashboard view displays with trader overview, metrics, and charts
3. **No errors** should appear

#### ✅ AI Traders Tab (Should work - already working)
1. Click on "AI Traders" in the navigation sidebar
2. **Expected**: Trader Management view displays with list of traders
3. **No errors** should appear

#### 🔍 Monitoring Tab (DEF_011 - Fixed)
1. Click on "Monitoring" in the navigation sidebar
2. **Expected**: Monitoring view displays with:
   - Price chart
   - Positions table
   - Trade history table
   - Timeframe picker
3. **No errors** should appear
4. **Previous Error** (now fixed): "Could not create instance for '[Factory:'com.fmps.autotrader.desktop.monitoring.MonitoringView']'"

#### 🔍 Configuration Tab (DEF_012 - Should be fixed)
1. Click on "Configuration" in the navigation sidebar
2. **Expected**: Configuration view displays with:
   - Exchange account settings
   - Risk management settings
   - System configuration options
3. **No errors** should appear
4. **Previous Error** (now fixed): "Could not create instance for '[Factory:'com.fmps.autotrader.desktop.config.ConfigurationView']'"

#### 🔍 Pattern Analytics Tab (DEF_013 - Should be fixed)
1. Click on "Pattern Analytics" in the navigation sidebar
2. **Expected**: Pattern Analytics view displays with:
   - Pattern performance charts
   - Pattern statistics
   - Pattern discovery tools
3. **No errors** should appear
4. **Previous Error** (now fixed): "Could not create instance for '[Factory:'com.fmps.autotrader.desktop.patterns.PatternAnalyticsView']'"

## What to Look For

### ✅ Success Indicators
- **No error dialogs** appear when clicking tabs
- **Views load immediately** (no long delays)
- **UI components render correctly** (charts, tables, buttons visible)
- **Navigation works smoothly** between tabs
- **No console errors** in the terminal where Desktop UI is running

### ❌ Failure Indicators
- **Error dialog** appears: "Unable to navigate to '[tab name]'"
- **Error message** contains: "Could not create instance for '[Factory:'..."
- **View doesn't load** (blank screen or placeholder)
- **Application freezes** when clicking a tab
- **Console errors** in the terminal

## Troubleshooting

### If a Tab Still Fails

1. **Check the error message** in the error dialog
2. **Check the console output** in the terminal where Desktop UI is running
3. **Verify Core Service is running**: 
   ```powershell
   Invoke-WebRequest -Uri http://localhost:8080/api/health
   ```
4. **Check Koin DI logs** - Look for dependency injection errors
5. **Restart Desktop UI** - Close and restart the application

### Common Issues

#### "Connection Refused" Errors
- **Cause**: Core Service is not running
- **Fix**: Start Core Service first with `.\gradlew :core-service:run`

#### "Could not create instance" Errors
- **Cause**: Koin dependency injection issue (should be fixed by DEF_011 fix)
- **Fix**: Verify BaseView.kt uses `getKoin()` instead of `GlobalContext.get()`

#### Blank Screen
- **Cause**: View loaded but no data or UI components not rendering
- **Fix**: Check if Core Service is responding, check WebSocket connections

## Quick Verification Script

```powershell
# Check Core Service
Write-Host "Checking Core Service..." -ForegroundColor Cyan
try {
    $health = Invoke-WebRequest -Uri "http://localhost:8080/api/health" -ErrorAction Stop
    Write-Host "✅ Core Service is UP" -ForegroundColor Green
} catch {
    Write-Host "❌ Core Service is DOWN - Start it first!" -ForegroundColor Red
    exit 1
}

# Check Desktop UI (manual verification required)
Write-Host "`n📋 Desktop UI Verification:" -ForegroundColor Cyan
Write-Host "   1. Open Desktop UI" -ForegroundColor White
Write-Host "   2. Click each tab in the navigation sidebar" -ForegroundColor White
Write-Host "   3. Verify no error dialogs appear" -ForegroundColor White
Write-Host "   4. Verify views load correctly" -ForegroundColor White
```

---

**Last Updated**: 2025-11-21  
**Related Defects**: DEF_011, DEF_012, DEF_013

