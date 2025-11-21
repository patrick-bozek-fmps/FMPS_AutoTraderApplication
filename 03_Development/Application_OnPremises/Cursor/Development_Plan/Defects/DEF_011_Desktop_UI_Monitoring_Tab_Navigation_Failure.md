# DEF_011: Desktop UI Monitoring Tab Navigation Failure

**Status**: 🆕 **NEW**  
**Severity**: 🔴 **HIGH**  
**Priority**: **P1 (High)**  
**Reported By**: User  
**Reported Date**: 2025-11-21  
**Assigned To**: Unassigned  
**Assigned Date**: Not Assigned  
**Fixed By**: N/A  
**Fixed Date**: N/A  
**Verified By**: N/A  
**Verified Date**: N/A  
**Closed Date**: Not Closed  
**Epic**: Epic 5 (Desktop UI)  
**Issue**: Issue #22 (Trading Monitoring View)  
**Module/Component**: desktop-ui, navigation, dependency-injection  
**Version Found**: f13583f  
**Version Fixed**: N/A

> **NOTE**: Desktop UI fails to navigate to Monitoring tab with error "Could not create instance for '[Factory:'com.fmps.autotrader.desktop.monitoring.MonitoringView']'". This indicates a Koin dependency injection issue preventing MonitoringView from being instantiated.

---

## 📋 **Defect Summary**

Desktop UI navigation to the Monitoring tab fails with a Koin dependency injection error, preventing users from accessing the Trading Monitoring view. The error indicates that Koin cannot create an instance of `MonitoringView` or its dependencies.

---

## 🎯 **Impact Assessment**

### **Business Impact**
- **User Impact**: **HIGH** - Users cannot access the Trading Monitoring view, which is a core feature
- **Workaround Available**: No - feature is completely inaccessible
- **Data Loss Risk**: No
- **Security Risk**: No

### **Technical Impact**
- **Affected Components**: 
  - `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/monitoring/MonitoringView.kt`
  - `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/monitoring/MonitoringViewModel.kt`
  - `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/di/DesktopModule.kt`
  - `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/DesktopApp.kt`
- **System Stability**: **Medium Impact** - Feature is broken, but doesn't crash the entire application
- **Test Coverage Gap**: Yes - navigation failure not covered by tests
- **Regression Risk**: Medium - may affect other views if root cause is in BaseView or DI setup

### **Development Impact**
- **Blocks Other Work**: Yes - blocks Issue #22 completion and user testing
- **Estimated Fix Time**: 2-4 hours (requires investigation of Koin DI setup)
- **Complexity**: **Moderate** (needs investigation of dependency injection configuration)

---

## 🔍 **Detailed Description**

### **Expected Behavior**:
- User clicks on "Monitoring" tab in Desktop UI navigation
- MonitoringView should be instantiated and displayed
- Trading Monitoring view should show price charts, positions, and trade history

### **Actual Behavior**:
- **Error**: `Unable to navigate to 'monitoring' (Could not create instance for '[Factory:'com.fmps.autotrader.desktop.monitoring.MonitoringView']')`
- **Location**: Navigation service when attempting to create MonitoringView
- **Issue**: Koin cannot resolve dependencies for MonitoringView or MonitoringViewModel
- **Root Cause**: Needs investigation - likely:
  - Missing or incorrect Koin factory definition for MonitoringView
  - Missing dependencies for MonitoringViewModel (DispatcherProvider, MarketDataService)
  - Circular dependency issue
  - Koin context not properly initialized when view is created

---

## 🔄 **Steps to Reproduce**

1. Start Core Service: `.\gradlew :core-service:run`
2. Start Desktop UI: `.\gradlew :desktop-ui:run`
3. Wait for Desktop UI to load
4. Click on "Monitoring" tab in navigation
5. Observe: Error dialog appears with message "Unable to navigate to 'monitoring' (Could not create instance for '[Factory:'com.fmps.autotrader.desktop.monitoring.MonitoringView']')"

**Reproducibility**: **Always** (100%)

---

## 🌍 **Environment Details**

### **Test Environment**
- **OS**: Windows 10/11
- **Java Version**: OpenJDK 17
- **Gradle Version**: 8.5
- **Kotlin Version**: 1.9.x
- **Koin Version**: 3.5.0
- **TornadoFX Version**: 1.7.20

### **Configuration**
- **Core Service**: Running on http://localhost:8080
- **Desktop UI**: JavaFX application
- **Dependency Injection**: Koin

### **Build Information**
- **Commit SHA**: f13583f
- **Branch**: main
- **Build Date**: 2025-11-21

---

## 📊 **Evidence & Logs**

### **Error Messages**
```
Unable to navigate to 'monitoring' (Could not create instance for '[Factory:'com.fmps.autotrader.desktop.monitoring.MonitoringView']')
```

### **Code Analysis**
- `MonitoringView` extends `BaseView<MonitoringState, MonitoringEvent, MonitoringViewModel>`
- `BaseView` uses `GlobalContext.get().get(viewModelClass)` to get ViewModel from Koin
- `MonitoringViewModel` requires: `DispatcherProvider` and `MarketDataService`
- `DesktopModule.kt` defines:
  - `factory { MonitoringViewModel(get(), get()) }`
  - `factory { MonitoringView() }`
- `DesktopApp.kt` registers navigation: `ViewDescriptor(route = "monitoring", title = "Monitoring", factory = { koin.get<MonitoringView>() })`

### **Potential Issues**
1. **Koin Context**: `BaseView` uses `GlobalContext.get()` which may not be initialized when view is created
2. **Factory vs Single**: Views are registered as `factory` but `BaseView` tries to get ViewModel directly
3. **Dependency Resolution**: `MonitoringViewModel(get(), get())` may not resolve correctly if dependencies are missing
4. **Timing Issue**: View creation may happen before Koin is fully initialized

---

## 🔗 **Related Items**

### **Related Defects**
- DEF_012: Desktop UI Configuration Tab Navigation Failure (similar issue)
- DEF_013: Desktop UI Pattern Analytics Tab Navigation Failure (similar issue)

### **Related Issues**
- Issue #22 (Trading Monitoring View) - Feature is broken
- Issue #19 (Desktop UI Foundation) - May be related to BaseView/DI setup

### **Related Epics**
- Epic 5 (Desktop UI) - Core feature broken

### **Requirements**
- ATP_ProdSpec: Desktop UI must provide Trading Monitoring view

---

## 🛠️ **Resolution Details**

### **Root Cause Analysis**

**Status**: 🔍 **INVESTIGATING**

**Investigation Steps**:
1. ⏳ Review `BaseView.kt` - Check how it gets ViewModel from Koin
2. ⏳ Review `DesktopModule.kt` - Verify MonitoringView and MonitoringViewModel factory definitions
3. ⏳ Review `DesktopApp.kt` - Check Koin initialization and navigation registration
4. ⏳ Check `MonitoringViewModel` dependencies - Verify DispatcherProvider and MarketDataService are registered
5. ⏳ Test Koin context - Verify GlobalContext is available when BaseView tries to get ViewModel

**Potential Root Causes**:
1. `BaseView` uses `GlobalContext.get()` which may not be initialized
2. ViewModel factory dependencies not properly resolved
3. Timing issue - View created before Koin is ready
4. Circular dependency between View and ViewModel

### **Proposed Solution**

**Status**: ⏳ **PENDING INVESTIGATION**

**Solution Options**:
1. **Option A**: Fix `BaseView` to use KoinComponent properly instead of GlobalContext
2. **Option B**: Ensure Koin is fully initialized before navigation registration
3. **Option C**: Change View factory registration to use lazy initialization
4. **Option D**: Fix dependency resolution in DesktopModule

**Recommended Approach**: Investigate root cause first, then implement appropriate fix.

---

## ✅ **Verification Plan**

### **Test Cases**
1. ✅ Verify Monitoring tab navigation works
2. ✅ Verify MonitoringView displays correctly
3. ✅ Verify MonitoringViewModel receives data from MarketDataService
4. ✅ Verify no Koin errors in logs
5. ✅ Verify other tabs still work (Dashboard, Traders)

### **Acceptance Criteria**
- [ ] Monitoring tab can be navigated to without errors
- [ ] MonitoringView displays correctly with all components
- [ ] Price charts, positions table, and trade history table are visible
- [ ] No Koin dependency injection errors in logs
- [ ] All other navigation tabs continue to work

---

## 📝 **Notes**

- This defect is related to DEF_012 and DEF_013 (Configuration and Pattern Analytics tabs)
- All three defects may share the same root cause in BaseView or Koin setup
- Consider fixing all three together if root cause is common

---

**Last Updated**: 2025-11-21  
**Next Review**: After root cause investigation

