# FMPS AutoTrader - On-Premises Application

**Version**: 1.0.0-SNAPSHOT  
**Status**: 🚀 In Development - Phase 1

## Overview

FMPS AutoTrader is an automated cryptocurrency trading system that uses AI-powered pattern recognition to execute trades across multiple exchanges. This is the on-premises version designed to run on local servers.

## Features

### ✅ Implemented (Phase 1)

- **Multi-Module Architecture**: Gradle-based multi-module project structure
- **Database Layer**: SQLite database with Exposed ORM
  - Database migrations with Flyway
  - HikariCP connection pooling
  - Repositories for AI Traders, Trades, and Patterns
- **REST API Server**: Ktor-based HTTP server
  - 34 REST endpoints for CRUD operations
  - Health and status monitoring
  - JSON serialization/deserialization
  - CORS support
  - Call logging
- **WebSocket Support**: Real-time updates via 3 WebSocket channels
  - Trader status updates
  - Trade execution notifications
  - Market data streaming (placeholder)
- **Comprehensive Test Suite**: 39 automated tests
  - Database layer tests
  - Server startup tests
  - WebSocket manager tests
  - DTO serialization tests

### 🔜 Planned (Future Phases)

- Exchange connectors (Binance, Coinbase, Kraken)
- AI pattern recognition engine
- Risk management system
- Desktop UI (Compose Multiplatform)
- Authentication & authorization
- Backtesting framework

## Architecture

```
Application_OnPremises/
├── core-service/          # Main backend service
│   ├── database/          # Database layer (Exposed ORM)
│   │   ├── repositories/  # Data access layer
│   │   └── entities/      # Database entities
│   └── api/               # REST API & WebSocket
│       ├── routes/        # API endpoint routes
│       ├── plugins/       # Ktor plugins
│       └── mappers/       # Entity-DTO mappers
├── shared/                # Shared models & DTOs
├── desktop-ui/            # Desktop UI (Future)
└── config/                # Configuration files
```

## Getting Started

### Prerequisites

- **Java 17+** (OpenJDK or Oracle JDK)
- **Gradle 8.5+** (wrapper included)
- **Git** (for version control)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd FMPS_AutoTraderApplication/03_Development/Application_OnPremises
   ```

2. **Build the project**
   ```bash
   .\gradlew build
   ```

3. **Run tests**
   ```bash
   .\gradlew test
   ```

4. **Start the application**
   ```bash
   .\gradlew :core-service:run
   ```

The API server will start on `http://localhost:8080`

### Quick API Test

```bash
# Check health
curl http://localhost:8080/api/health

# Get system status
curl http://localhost:8080/api/status

# List AI traders
curl http://localhost:8080/api/v1/traders
```

## Configuration

Configuration is managed via `config/application.conf` (HOCON format):

```hocon
app {
  host = "0.0.0.0"
  port = 8080
}

database {
  driver = "org.sqlite.JDBC"
  url = "jdbc:sqlite:data/autotrader.db"
  hikari {
    maximumPoolSize = 10
    minimumIdle = 2
    # ... additional settings
  }
}
```

## API Documentation

Comprehensive API documentation is available in:
- [`core-service/API_DOCUMENTATION.md`](core-service/API_DOCUMENTATION.md)

### Key Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/health` | GET | Health check |
| `/api/status` | GET | Detailed system status |
| `/api/v1/traders` | GET/POST | Manage AI traders |
| `/api/v1/trades` | GET/POST | Manage trades |
| `/api/v1/patterns` | GET/POST | Manage patterns |
| `/ws/trader-status` | WS | Trader updates stream |
| `/ws/trades` | WS | Trade updates stream |

## Development

### Project Structure

- **core-service**: Main backend service with database and API
- **shared**: Shared DTOs and models
- **desktop-ui**: Desktop user interface (planned)
- **Cursor/**: Development documentation and planning

### Running Tests

```bash
# Run all tests
.\gradlew test

# Run specific module tests
.\gradlew :core-service:test
.\gradlew :shared:test

# Run with detailed output
.\gradlew test --info
```

### Building for Production

```bash
# Build distribution
.\gradlew :core-service:build

# Create distribution archives
.\gradlew :core-service:distZip
.\gradlew :core-service:distTar

# Distribution will be in:
# core-service/build/distributions/
```

## Testing

Test coverage includes:
- **32 core-service tests**
  - Database layer: 24 tests
  - API server: 2 tests
  - WebSocket: 6 tests
- **7 shared module tests**
  - BigDecimal serialization: 7 tests

**Total: 39 automated tests passing ✅**

## Database

### Schema Overview

- **ai_traders**: AI trading bots configuration and state
- **trades**: Trade execution records
- **patterns**: Pattern definitions and statistics
- **flyway_schema_history**: Database migration tracking

### Database Location

Development: `build/test-db/*.db`  
Production: `data/autotrader.db`

### Migrations

Database migrations are managed by Flyway and located in:
```
core-service/src/main/resources/db/migration/
```

## WebSocket Usage

### Connect to Trade Updates

```javascript
const ws = new WebSocket('ws://localhost:8080/ws/trades');

ws.onopen = () => console.log('Connected');
ws.onmessage = (event) => {
  const update = JSON.parse(event.data);
  console.log('Trade update:', update);
};
```

### Available Channels

1. `/ws/trader-status` - AI trader status changes
2. `/ws/trades` - Trade open/close/update events
3. `/ws/market-data` - Market data stream (placeholder)

## Troubleshooting

### Port Already in Use

If port 8080 is already in use, change it in `config/application.conf`:
```hocon
app {
  port = 8081
}
```

### Database Locked

If you see "database is locked" errors:
1. Ensure only one instance is running
2. Delete `*.db-journal` files
3. Restart the application

### Build Failures

```bash
# Clean and rebuild
.\gradlew clean build

# Verify Java version
java -version  # Should be 17+
```

## Contributing

1. Create a feature branch
2. Make your changes
3. Run tests: `.\gradlew test`
4. Build: `.\gradlew build`
5. Submit a pull request

See [`Cursor/DEVELOPMENT_WORKFLOW.md`](Cursor/DEVELOPMENT_WORKFLOW.md) for detailed workflow.

## Documentation

- [API Documentation](core-service/API_DOCUMENTATION.md)
- [Development Plan](Cursor/Development_Plan/Development_Plan_v2.md)
- [Development Workflow](Cursor/DEVELOPMENT_WORKFLOW.md)
- [Testing Guide](Cursor/TESTING_GUIDE.md)
- [Requirements Analysis](Cursor/Requirements_Analysis_Summary.md)

## License

[Your License Here]

## Support

For issues or questions:
- GitHub Issues: [Repository Issues]
- Documentation: See `Cursor/` directory
- Email: [Contact Email]

---

**Last Updated**: October 24, 2025  
**Current Phase**: Phase 1 - Core Foundation (3/9 issues completed)

