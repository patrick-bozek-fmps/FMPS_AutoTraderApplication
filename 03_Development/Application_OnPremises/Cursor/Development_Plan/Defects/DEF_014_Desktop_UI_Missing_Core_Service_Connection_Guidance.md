# DEF_014: Desktop UI Missing Core Service Connection Guidance

**Status**: ✅ **FIXED**  
**Severity**: 🟡 **MEDIUM**  
**Priority**: **P2 (Medium)**  
**Reported By**: User  
**Reported Date**: 2025-11-25  
**Assigned To**: Auto  
**Assigned Date**: 2025-11-25  
**Fixed By**: Auto  
**Fixed Date**: 2025-11-25  
**Verified By**: User  
**Verified Date**: 2025-11-25  
**Closed Date**: 2025-11-25  
**Epic**: Epic 5 (Desktop UI)  
**Issue**: Issue #19 (Desktop UI Foundation)  
**Module/Component**: desktop-ui, user-experience, documentation  
**Version Found**: ae89d77  
**Version Fixed**: a484c27  

> **NOTE**: This is a UX/documentation issue, not a technical bug. The core service is designed to run as a separate process. However, users need better guidance when the service is not running.

---

## 📋 **Defect Summary**

Desktop UI shows connection refused errors when core service is not running, but provides no user guidance on how to start the service or check its status. Users may incorrectly assume this is a bug rather than understanding that the core service must be started separately.

---

## 🎯 **Impact Assessment**

### **Business Impact**
- **User Impact**: **MEDIUM** - Users cannot use the application without understanding how to start the core service
- **Workaround Available**: Yes - Start core service manually: `.\gradlew :core-service:run`
- **Data Loss Risk**: No
- **Security Risk**: No

### **Technical Impact**
- **Affected Components**: 
  - `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/services/RealTelemetryClient.kt`
  - `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/services/RealTraderService.kt`
  - `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/DesktopApp.kt`
  - Documentation/README files
- **System Stability**: **No Impact** - Application works correctly when core service is running
- **Test Coverage Gap**: Yes - No tests for user guidance/error messaging
- **Regression Risk**: Low

### **Development Impact**
- **Blocks Other Work**: No
- **Estimated Fix Time**: 2-4 hours (UX improvements + documentation)
- **Complexity**: **Simple** (UX improvements and documentation updates)

---

## 🔍 **Detailed Description**

### **Expected Behavior**:
- When core service is not running, Desktop UI should:
  - Display clear, user-friendly error messages
  - Provide instructions on how to start the core service
  - Show connection status indicator in UI
  - Optionally: Provide a button/link to start the service (if possible)

### **Actual Behavior**:
- Desktop UI shows technical error messages: `Connection refused: getsockopt`
- No user guidance on what to do
- No status indicator showing connection state
- Users may think the application is broken
- Error messages appear in console logs, not in user-friendly UI dialogs

**Example Error Messages**:
```
Connection refused: getsockopt
Telemetry WebSocket connection failed (attempt 1/5)
Error refreshing traders
```

---

## 🔄 **Steps to Reproduce**

1. Start Desktop UI: `.\gradlew :desktop-ui:run`
2. Do NOT start core service
3. Observe: Connection errors appear in console
4. Try to use any feature that requires core service (e.g., view traders, create trader)
5. Observe: Features don't work, but no clear guidance is provided

**Reproducibility**: **Always** (100%)

---

## 🌍 **Environment Details**

### **Test Environment**
- **OS**: Windows 10/11
- **Java Version**: OpenJDK 17
- **Gradle Version**: 8.5
- **Kotlin Version**: 1.9.x

### **Configuration**
- **Core Service**: Not running (expected scenario)
- **Desktop UI**: Running standalone
- **Expected Behavior**: Core service should be started separately

### **Build Information**
- **Commit SHA**: ae89d77
- **Branch**: main
- **Build Date**: 2025-11-25

---

## 📊 **Evidence & Logs**

### **Error Messages**
```
Connection refused: getsockopt
Telemetry WebSocket connection failed (attempt 1/5)
Error refreshing traders
Max reconnection attempts reached. Telemetry client will stop retrying.
```

### **Architecture Context**
- Core service is a separate backend process (by design)
- Desktop UI is a client that connects via REST API + WebSocket
- Both must be started separately
- This is documented in README.md but not clear to users from error messages

---

## 🔗 **Related Items**

### **Related Defects**
- None

### **Related Issues**
- Issue #19 (Desktop UI Foundation) - UX improvements needed

### **Related Epics**
- Epic 5 (Desktop UI) - User experience improvements

### **Requirements**
- ATP_ProdSpec_18: Core Application connects to User Interface
- ATP_ProdSpec_19: Core Application shall be available 24/7

---

## 🛠️ **Resolution Details**

### **Root Cause Analysis**

**Status**: ✅ **COMPLETE**

**Root Cause Identified**:
The Desktop UI is designed as a client that connects to a separate core service process. This is correct architecture, but the user experience was poor when the service is not running:
1. Error messages were technical and not user-friendly
2. No status indicator in UI showing connection state
3. No guidance on how to start the core service
4. No documentation visible within the application

### **Solution Description**

**Status**: ✅ **IMPLEMENTED**

**Solutions Implemented**:

1. **✅ Added Connection Status Indicator**:
   - Connection status displayed in UI top bar (next to quick stats)
   - Shows "Core Service: Connected" / "Core Service: Disconnected" / "Core Service: Connecting..." with visual indicator (green/red/orange)
   - Updates in real-time as connection state changes (every 5 seconds)
   - Status text clearly indicates it refers to Core Service connection

2. **✅ Improved Error Messages**:
   - Replaced technical errors with user-friendly messages
   - Example: "Cannot connect to core service. Please ensure the core service is running on localhost:8080"
   - Error messages in RealTelemetryClient and RealTraderService now provide clear guidance

3. **✅ Added Help/Documentation**:
   - Added "?" help button when disconnected
   - Shows instructions dialog: "How to Start Core Service" with step-by-step guide
   - Instructions include: navigate to project directory, run `.\gradlew.bat :core-service:run`

4. **✅ Connection Status Monitoring**:
   - Created `ConnectionStatusService` to monitor REST API health endpoint
   - Checks connection status every 5 seconds
   - Automatically detects when core service becomes available
   - WebSocket connection starts automatically on app startup (not lazy)

### **Code Changes**

**Status**: ✅ **COMPLETE**

**Files Modified**:
- ✅ `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/services/ConnectionStatusService.kt` - **NEW** - Connection status monitoring service
- ✅ `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/shell/ShellView.kt` - Added status indicator with "Core Service:" prefix
- ✅ `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/shell/ShellViewModel.kt` - Observes connection status, starts telemetry client automatically
- ✅ `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/shell/ShellContract.kt` - Added connection status to ShellState
- ✅ `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/services/RealTelemetryClient.kt` - Improved error messages with user-friendly text
- ✅ `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/services/RealTraderService.kt` - Improved error messages for all operations
- ✅ `desktop-ui/src/main/kotlin/com/fmps/autotrader/desktop/di/DesktopModule.kt` - Added ConnectionStatusService to DI, updated ShellViewModel factory

---

## ✅ **Verification Plan**

### **Verification Steps**

1. Start Desktop UI without core service
2. Verify: User-friendly error message appears (not technical stack trace)
3. Verify: Connection status indicator shows "Disconnected"
4. Verify: Instructions provided on how to start core service
5. Start core service
6. Verify: Connection status updates to "Connected"
7. Verify: Features work correctly

### **Acceptance Criteria**
- [x] User-friendly error messages when core service is not running ✅
- [x] Connection status indicator visible in UI ✅
- [x] Instructions provided on how to start core service ✅
- [x] Status updates automatically when connection is established ✅
- [x] No technical stack traces shown to end users ✅
- [x] Status text clearly indicates "Core Service:" prefix ✅
- [x] WebSocket connects automatically on startup ✅

---

## 📝 **Notes**

- This is a UX/documentation improvement, not a technical bug
- The architecture (separate core service) is correct and by design
- The issue is lack of user guidance, not a functional problem
- Similar improvements may be needed for other connection errors (database, exchange APIs, etc.)

---

**Last Updated**: 2025-11-25  
**Resolution Summary**: 
- Connection status indicator implemented with "Core Service:" prefix
- User-friendly error messages added to all service clients
- Help button with instructions added when disconnected
- Connection status monitoring service created
- WebSocket auto-connects on app startup
- All acceptance criteria met and verified by user

**Next Review**: N/A - Defect resolved and closed

