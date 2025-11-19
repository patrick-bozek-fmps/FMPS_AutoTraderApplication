# How to Run the FMPS AutoTrader Application

## 🚀 Quick Start

### 1. Start the Core Service (Backend API)

The Core Service provides the REST API and WebSocket endpoints for the trading system.

```powershell
# Navigate to project directory
cd 03_Development\Application_OnPremises

# Start the Core Service
.\gradlew :core-service:run
```

**Expected Output:**
```
> Task :core-service:run
=== FMPS AutoTrader Application ===
Version: 1.0.0-SNAPSHOT
Loading configuration...
Configuration loaded successfully
✓ Database initialized successfully
✓ REST API server started on http://0.0.0.0:8080
✓ Core service is running
```

**The API will be available at:**
- REST API: `http://localhost:8080`
- Health Check: `http://localhost:8080/api/health`
- WebSocket: `ws://localhost:8080/ws/telemetry`

### 2. Start the Desktop UI (JavaFX Application)

The Desktop UI is a JavaFX application that connects to the Core Service.

```powershell
# Navigate to project directory
cd 03_Development\Application_OnPremises

# Start the Desktop UI
.\gradlew :desktop-ui:run
```

**Note**: The Desktop UI requires the Core Service to be running first.

### 3. Verify the Application is Running

#### Check Core Service Health
```powershell
# Using PowerShell
Invoke-WebRequest -Uri http://localhost:8080/api/health

# Using curl (if available)
curl http://localhost:8080/api/health
```

**Expected Response:**
```json
{
  "status": "UP",
  "timestamp": "2025-11-19T12:39:42.223411900Z",
  "uptime": 587331
}
```

**Note**: Status value is `"UP"` (not "healthy"). See `core-service/API_DOCUMENTATION.md` for full API reference.

#### Check System Status
```powershell
Invoke-WebRequest -Uri http://localhost:8080/api/status
```

**Expected Response:**
```json
{
  "status": "OPERATIONAL",
  "timestamp": "2025-11-19T12:30:15.415306200Z",
  "activeTraders": 0,
  "databaseStats": {
    "status": "connected",
    "type": "SQLite",
    "activeConnections": "0",
    "idleConnections": "2",
    "totalConnections": "2"
  }
}
```

**Note**: Status value is `"OPERATIONAL"` when system is healthy, or `"DEGRADED"` when there are issues. See `core-service/API_DOCUMENTATION.md` for full API reference.

## 📋 Prerequisites

- **Java 17+** (OpenJDK or Oracle JDK)
- **Gradle 8.5+** (wrapper included, no installation needed)
- **Windows/Linux/macOS**

## 🔧 Configuration

Configuration is managed via `config/application.conf`:

```hocon
app {
  host = "0.0.0.0"
  port = 8080
}

database {
  driver = "org.sqlite.JDBC"
  url = "jdbc:sqlite:data/autotrader.db"
}
```

## 🧪 Testing

### Run Unit Tests
```powershell
.\gradlew test
```

### Run Integration Tests
```powershell
.\gradlew :core-service:integrationTest
```

**Note**: Integration tests require exchange API keys (optional):
- `BINANCE_API_KEY`
- `BINANCE_API_SECRET`
- `BITGET_API_KEY`
- `BITGET_API_SECRET`
- `BITGET_API_PASSPHRASE`

### Run All Tests
```powershell
.\gradlew testAll
```

## 📚 API Documentation

Once the Core Service is running, you can access:

- **Health Check**: `GET http://localhost:8080/api/health`
- **System Status**: `GET http://localhost:8080/api/status`
- **AI Traders**: `GET http://localhost:8080/api/v1/traders`
- **WebSocket Telemetry**: `ws://localhost:8080/ws/telemetry`

For detailed API documentation, see:
- `core-service/API_DOCUMENTATION.md`

## 🐛 Troubleshooting

### Port Already in Use
If port 8080 is already in use:
1. Check what's using it: `netstat -ano | findstr :8080` (Windows)
2. Change port in `config/application.conf`
3. Restart the service

### Database Issues
If you see database errors:
1. Check `data/autotrader.db` exists
2. Delete `data/autotrader.db` to reset (⚠️ loses data)
3. Restart the service

### Connection Refused
If the Desktop UI can't connect:
1. Verify Core Service is running
2. Check `http://localhost:8080/api/health` responds
3. Verify firewall isn't blocking port 8080

## 📝 Next Steps

1. **Start Core Service**: `.\gradlew :core-service:run`
2. **Start Desktop UI**: `.\gradlew :desktop-ui:run` (in another terminal)
3. **Create AI Traders** via the Desktop UI
4. **Monitor Trading** via WebSocket telemetry

---

**Last Updated**: November 19, 2025

