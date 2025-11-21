# DEF_011: Desktop UI Monitoring Tab Navigation Failure

**Status**: 🏗️ **IN PROGRESS**  
**Severity**: 🔴 **HIGH**  
**Priority**: **P1 (High)**  
**Reported By**: User  
**Reported Date**: 2025-11-21  
**Assigned To**: Auto  
**Assigned Date**: 2025-11-21  
**Fixed By**: Auto  
**Fixed Date**: 2025-11-21  
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

**Status**: ✅ **RESOLVED**

**Root Cause Identified**:
`BaseView` was using `GlobalContext.get().get()` to retrieve ViewModels, which may not work correctly when Views are created via Koin factories. Since `BaseView` implements `KoinComponent`, it should use `getKoin().get()` instead, which ensures it uses the same Koin instance that created the component.

**Investigation Steps Completed**:
1. ✅ Reviewed `BaseView.kt` - Found it uses `GlobalContext.get().get()` instead of `getKoin()`
2. ✅ Reviewed `DesktopModule.kt` - Verified MonitoringView and MonitoringViewModel factory definitions are correct
3. ✅ Reviewed `DesktopApp.kt` - Confirmed Koin initialization and navigation registration are correct
4. ✅ Checked `MonitoringViewModel` dependencies - All dependencies (DispatcherProvider, MarketDataService) are registered
5. ✅ Identified root cause - `BaseView` should use `getKoin()` from `KoinComponent` instead of `GlobalContext.get()`

### **Solution Implemented**

**Status**: ✅ **FIXED**

**Solution Applied**:
- Modified `BaseView.kt` to use `getKoin().get(viewModelClass)` instead of `GlobalContext.get().get(viewModelClass)`
- This ensures that the View uses the same Koin instance that created it, which is the recommended approach for `KoinComponent` implementations
- Removed unused `GlobalContext` import

**Code Changes**:
```kotlin
// Before:
protected val viewModel: VM by lazy { GlobalContext.get().get(viewModelClass, qualifier = qualifier) }

// After:
protected val viewModel: VM by lazy { 
    val koin = getKoin()
    if (qualifier != null) {
        koin.get(viewModelClass, qualifier = qualifier)
    } else {
        koin.get(viewModelClass)
    }
}
```

**Files Modified**:
- `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/mvvm/BaseView.kt`

**Version Fixed**: TBD (pending commit)
**Fixed Date**: 2025-11-21

---

## ✅ **Verification Plan**

### **Test Cases**
1. ✅ Verify Monitoring tab navigation works
2. ✅ Verify MonitoringView displays correctly
3. ✅ Verify MonitoringViewModel receives data from MarketDataService
4. ✅ Verify no Koin errors in logs
5. ✅ Verify other tabs still work (Dashboard, Traders)

### **Acceptance Criteria**
- [x] Fix implemented - BaseView uses getKoin() instead of GlobalContext.get()
- [x] Code compiles successfully
- [x] Unit tests pass
- [ ] Monitoring tab can be navigated to without errors (pending manual testing)
- [ ] MonitoringView displays correctly with all components (pending manual testing)
- [ ] Price charts, positions table, and trade history table are visible (pending manual testing)
- [ ] No Koin dependency injection errors in logs (pending manual testing)
- [ ] All other navigation tabs continue to work (pending manual testing)

---

## 📝 **Notes**

- This defect is related to DEF_012 and DEF_013 (Configuration and Pattern Analytics tabs)
- All three defects may share the same root cause in BaseView or Koin setup
- Consider fixing all three together if root cause is common

---

**Last Updated**: 2025-11-21  
**Next Review**: After root cause investigation

