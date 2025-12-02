# General Settings Usage Analysis

## Summary

The three General Settings values (Update Interval, Telemetry Polling, Logging Level) are currently **saved locally in the Desktop UI** but **NOT used by the core-service**. They are stored in the Desktop UI's local configuration file but never actually applied to the running application.

## Current Status

### 1. Update Interval (`updateIntervalSeconds`)
- **Desktop UI**: Saved to local file, sent to `/api/v1/config/general` (endpoint doesn't exist)
- **Core-Service**: Uses hardcoded `aiTrader.monitoring.updateIntervalMs = 5000` from config files
- **Used by**: `PositionManager` and other monitoring services
- **Status**: ❌ **NOT CONNECTED** - Desktop UI value is ignored

### 2. Telemetry Polling (`telemetryPollingSeconds`)
- **Desktop UI**: Saved to local file, sent to `/api/v1/config/general` (endpoint doesn't exist)
- **Core-Service**: Uses hardcoded `telemetry.heartbeatIntervalSeconds = 15` from config files
- **Used by**: `TelemetryHub` for WebSocket heartbeat
- **Status**: ❌ **NOT CONNECTED** - Desktop UI value is ignored

### 3. Logging Level (`loggingLevel`)
- **Desktop UI**: Saved to local file, sent to `/api/v1/config/general` (endpoint doesn't exist)
- **Core-Service**: Uses hardcoded `logging.level = "INFO"` from config files (can be overridden by `LOG_LEVEL` env var)
- **Used by**: Logback logging framework
- **Status**: ❌ **NOT CONNECTED** - Desktop UI value is ignored

## API Endpoint Status

The Desktop UI sends General Settings to:
```
PUT /api/v1/config/general
```

**Current Implementation**: This endpoint **does not exist** in `ConfigurationRoutes.kt`. The generic `/api/v1/config/{key}` endpoint exists but returns `HttpStatusCode.NotImplemented`.

**Result**: Desktop UI receives `NotImplemented` response and falls back to saving locally only.

## Required Changes

To make these values actually work, the following changes are needed:

### 1. Create `/api/v1/config/general` Endpoint
- **File**: `core-service/src/main/kotlin/com/fmps/autotrader/core/api/routes/ConfigurationRoutes.kt`
- **Action**: Add a new `put("/general")` route handler
- **Accept**: `GeneralSettingsDTO` (updateIntervalSeconds, telemetryPollingSeconds, loggingLevel, theme)

### 2. Implement Runtime Configuration Storage
- **Option A**: Store in memory (volatile, lost on restart)
- **Option B**: Store in database (persistent, survives restart)
- **Option C**: Update config file and reload (persistent, requires file write permissions)

### 3. Apply Update Interval to Monitoring
- **File**: `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/PositionManager.kt`
- **Action**: Read `updateIntervalMs` from runtime config instead of constructor parameter
- **Note**: Currently uses `Duration.ofSeconds(5)` hardcoded

### 4. Apply Telemetry Polling to WebSocket
- **File**: `core-service/src/main/kotlin/com/fmps/autotrader/core/api/websocket/TelemetryHub.kt`
- **Action**: Read `heartbeatIntervalSeconds` from runtime config instead of config file
- **Note**: Currently reads from `telemetry.heartbeatIntervalSeconds` in config

### 5. Apply Logging Level Dynamically
- **File**: `core-service/src/main/kotlin/com/fmps/autotrader/core/logging/LoggingContext.kt` (if exists)
- **Action**: Use Logback's `LoggerContext` to change logger levels at runtime
- **Note**: Requires using Logback's API: `LoggerContext.setLevel(Level.toLevel(level))`

## Implementation Priority

1. **High Priority**: Create the API endpoint (allows Desktop UI to send values)
2. **High Priority**: Implement runtime config storage (in-memory is simplest)
3. **Medium Priority**: Apply Update Interval to monitoring services
4. **Medium Priority**: Apply Telemetry Polling to WebSocket
5. **Low Priority**: Apply Logging Level dynamically (can be done via restart for now)

## Files to Modify

### Core-Service
1. `core-service/src/main/kotlin/com/fmps/autotrader/core/api/routes/ConfigurationRoutes.kt`
   - Add `put("/general")` endpoint
2. `core-service/src/main/kotlin/com/fmps/autotrader/core/config/ConfigManager.kt`
   - Add runtime config storage (or create new `RuntimeConfigManager`)
3. `core-service/src/main/kotlin/com/fmps/autotrader/core/traders/PositionManager.kt`
   - Read update interval from runtime config
4. `core-service/src/main/kotlin/com/fmps/autotrader/core/api/websocket/TelemetryHub.kt`
   - Read heartbeat interval from runtime config
5. `core-service/src/main/kotlin/com/fmps/autotrader/core/logging/LoggingContext.kt` (create if needed)
   - Implement dynamic logging level changes

## Testing Requirements

1. Verify Desktop UI can successfully send General Settings to core-service
2. Verify Update Interval is applied to monitoring services
3. Verify Telemetry Polling is applied to WebSocket heartbeat
4. Verify Logging Level changes are reflected in log output
5. Verify values persist across service restarts (if using database/file storage)

## Notes

- The `theme` field in GeneralSettings is Desktop UI-only and doesn't need to be sent to core-service
- Runtime config changes may require restarting certain services (e.g., monitoring loops)
- Consider validation: Update Interval (10-600s), Telemetry Polling (1-120s), Logging Level (TRACE/DEBUG/INFO/WARN/ERROR)

