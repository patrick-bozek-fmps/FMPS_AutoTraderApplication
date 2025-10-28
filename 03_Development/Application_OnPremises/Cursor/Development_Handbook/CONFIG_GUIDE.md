# Configuration Management Guide

**Version**: 1.0  
**Last Updated**: October 28, 2025  
**Status**: Complete  

---

## 📋 **Overview**

The FMPS AutoTrader application uses **HOCON** (Human-Optimized Config Object Notation) with **Typesafe Config** for configuration management. This provides a powerful, flexible, and type-safe configuration system with:

- ✅ Environment-specific configurations (dev, test, prod)
- ✅ Environment variable overrides
- ✅ Command-line argument overrides
- ✅ Configuration hot-reload
- ✅ Encrypted sensitive data
- ✅ Strongly-typed access
- ✅ Comprehensive validation

---

## 🏗️ **Configuration Architecture**

### **Configuration Hierarchy**

Configurations are loaded in the following order (later overrides earlier):

```
1. reference.conf         (Default values)
    ↓
2. application.conf       (Base configuration)
    ↓
3. application-{env}.conf (Environment-specific)
    ↓
4. Custom config file     (Optional)
    ↓
5. System properties      (Command-line args)
    ↓
6. Environment variables  (Highest priority)
```

### **File Structure**

```
core-service/src/main/resources/
├── reference.conf           # Default values fallback
├── application.conf         # Base configuration
├── application-dev.conf     # Development overrides
├── application-test.conf    # Test environment settings
└── application-prod.conf    # Production settings
```

---

## 🚀 **Quick Start**

### **1. Loading Configuration**

```kotlin
import com.fmps.autotrader.core.config.ConfigManager

// Load configuration for current environment
val config = ConfigManager.load()

// Or specify environment explicitly
val config = ConfigManager.load(environment = "prod")

// Get already-loaded configuration
val config = ConfigManager.get()
```

### **2. Accessing Configuration Values**

```kotlin
// Server settings
val port = config.server.port
val host = config.server.host

// Database settings
val dbPath = config.database.path
val poolSize = config.database.pool.maximumPoolSize

// AI Trader settings
val maxTraders = config.aiTrader.maxActiveTraders
val maxLeverage = config.aiTrader.defaultRisk.maxLeverage

// Check environment
if (config.app.isDevelopment) {
    // Development-specific code
}
```

### **3. Environment Variable Overrides**

```bash
# Override server port
export SERVER_PORT=9090

# Override database path
export DB_PATH=/custom/path/to/db

# Set Binance API credentials
export BINANCE_API_KEY=your_api_key
export BINANCE_API_SECRET=your_api_secret
```

---

## 📝 **Configuration Sections**

### **Application Info**

```hocon
app {
    name = "FMPS AutoTrader"
    version = "1.0.0"
    environment = "dev"  # dev, test, prod
}
```

**Kotlin Access**:
```kotlin
val appName = config.app.name
val version = config.app.version
val isDev = config.app.isDevelopment
val isTest = config.app.isTest
val isProd = config.app.isProduction
```

### **Server Configuration**

```hocon
server {
    host = "0.0.0.0"
    port = 8080
    requestTimeout = 60s
    connectionTimeout = 30s
    
    cors {
        enabled = true
        allowedHosts = ["localhost", "127.0.0.1"]
    }
    
    websocket {
        enabled = true
        pingIntervalMs = 30000
        timeoutMs = 60000
        maxFrameSize = 1048576
    }
}
```

**Environment Variables**:
- `SERVER_HOST` → `server.host`
- `SERVER_PORT` → `server.port`

### **Database Configuration**

```hocon
database {
    path = "data/autotrader.db"
    
    pool {
        maximumPoolSize = 10
        minimumIdle = 2
        connectionTimeout = 30000
        idleTimeout = 600000
        maxLifetime = 1800000
    }
    
    migration {
        enabled = true
        cleanOnValidationError = false
        validateOnMigrate = true
    }
}
```

**Environment Variables**:
- `DB_PATH` → `database.path`

### **Logging Configuration**

```hocon
logging {
    level = "INFO"
    
    file {
        enabled = true
        path = "logs"
        maxSize = "10MB"
        maxHistory = 7
        totalSizeCap = "1GB"
    }
    
    console {
        enabled = true
        colorOutput = true
    }
    
    loggers {
        "com.fmps.autotrader" = "INFO"
        "io.ktor" = "INFO"
        "org.jetbrains.exposed" = "WARN"
    }
}
```

**Environment Variables**:
- `LOG_LEVEL` → `logging.level`

### **Exchange Configuration**

```hocon
exchanges {
    binance {
        enabled = false
        
        api {
            key = ""  # Use BINANCE_API_KEY env var
            secret = ""  # Use BINANCE_API_SECRET env var
            baseUrl = "https://testnet.binance.vision"
            websocketUrl = "wss://testnet.binance.vision/ws"
        }
        
        rateLimit {
            requestsPerMinute = 1200
            requestsPerSecond = 20
        }
    }
    
    bitget {
        enabled = false
        
        api {
            key = ""  # Use BITGET_API_KEY env var
            secret = ""  # Use BITGET_API_SECRET env var
            passphrase = ""  # Use BITGET_PASSPHRASE env var
            baseUrl = "https://api.bitget.com"
            websocketUrl = "wss://ws.bitget.com/mix/v1/stream"
        }
        
        rateLimit {
            requestsPerMinute = 600
            requestsPerSecond = 10
        }
    }
}
```

**Environment Variables**:
- `BINANCE_ENABLED` → `exchanges.binance.enabled`
- `BINANCE_API_KEY` → `exchanges.binance.api.key`
- `BINANCE_API_SECRET` → `exchanges.binance.api.secret`
- `BINANCE_BASE_URL` → `exchanges.binance.api.baseUrl`
- `BITGET_ENABLED` → `exchanges.bitget.enabled`
- `BITGET_API_KEY` → `exchanges.bitget.api.key`
- `BITGET_API_SECRET` → `exchanges.bitget.api.secret`
- `BITGET_PASSPHRASE` → `exchanges.bitget.api.passphrase`

### **AI Trader Configuration**

```hocon
aiTrader {
    maxActiveTraders = 3
    
    defaultRisk {
        maxLeverage = 10
        defaultStopLossPercentage = 0.02  # 2%
        defaultTakeProfitPercentage = 0.05  # 5%
        maxDailyLossPercentage = 0.10  # 10%
        positionSizePercentage = 0.30  # 30%
        minProfitLossRatio = 2.0  # 2:1 reward/risk
    }
    
    defaultStrategy {
        analysisPeriod = 1h
        minConfidenceScore = 0.70  # 70%
        patternsRequired = 2
    }
    
    monitoring {
        updateIntervalMs = 5000
        alertOnLoss = true
        alertThreshold = -0.05  # -5%
    }
}
```

### **Trading Configuration**

```hocon
trading {
    demoMode {
        enabled = true  # Always true for v1.0
        initialBalance = 10000.0
        allowRealTrading = false  # Safety: never true in v1.0
    }
    
    marketData {
        candlestickRefreshMs = 60000  # 1 minute
        orderBookRefreshMs = 5000  # 5 seconds
        tickerRefreshMs = 2000  # 2 seconds
    }
    
    orders {
        defaultTimeoutMs = 30000
        retryFailedOrders = true
        maxRetries = 3
    }
}
```

---

## 🌍 **Environment-Specific Configurations**

### **Development (`application-dev.conf`)**

Optimized for local development:
- **Port**: 8080
- **Database**: `build/dev-db/autotrader-dev.db`
- **Logging**: DEBUG level with colored console output
- **Exchanges**: Disabled (demo mode only)
- **SQL Logging**: Enabled for debugging
- **Fast Refresh**: More frequent market data updates

**Usage**:
```bash
# Set environment
export APP_ENVIRONMENT=dev

# Or via system property
java -Dapp.environment=dev -jar application.jar
```

### **Test (`application-test.conf`)**

Optimized for unit and integration testing:
- **Port**: 0 (random port)
- **Database**: `build/test-db/autotrader-test.db`
- **Logging**: WARN level (reduce test noise)
- **Exchanges**: Disabled
- **WebSocket**: Disabled
- **Metrics**: Disabled
- **Fast Operations**: Short timeouts for quick tests

**Usage**:
```bash
# Usually set automatically by test framework
./gradlew test
```

### **Production (`application-prod.conf`)**

Production-ready settings:
- **Port**: 8080 (or from `SERVER_PORT`)
- **Database**: `/var/lib/fmps-autotrader/data/autotrader.db`
- **Logging**: INFO level, larger files, 30-day retention
- **Security**: Encryption enabled, strict validation
- **Performance**: Larger thread pools, connection pools
- **Monitoring**: Full metrics and health checks

**Usage**:
```bash
# Set environment
export APP_ENVIRONMENT=prod

# Set required environment variables
export BINANCE_API_KEY=your_key
export BINANCE_API_SECRET=your_secret

# Run application
java -Dapp.environment=prod -jar application.jar
```

---

## 🔐 **Sensitive Data Encryption**

### **Why Encrypt Configuration?**

- **Security**: API keys and secrets should never be stored in plain text
- **Version Control**: Encrypted values are safe to commit to git
- **Compliance**: Meet security requirements for credential storage

### **Setup Encryption**

1. **Generate Master Key**:
```kotlin
val masterKey = ConfigEncryption.generateMasterKey()
println("Master Key: $masterKey")
// Store this securely! (password manager, secret vault, etc.)
```

2. **Set Environment Variable**:
```bash
# Linux/macOS
export FMPS_MASTER_KEY=your_generated_master_key

# Windows PowerShell
$env:FMPS_MASTER_KEY="your_generated_master_key"

# Windows CMD
set FMPS_MASTER_KEY=your_generated_master_key
```

3. **Encrypt Sensitive Values**:
```kotlin
val encrypted = ConfigEncryption.encrypt("my-secret-api-key")
// Use this encrypted value in your configuration files
```

4. **Store Encrypted Values**:
```hocon
exchanges {
    binance {
        api {
            key = "encrypted_base64_value_here"
            secret = "encrypted_base64_value_here"
        }
    }
}
```

5. **Decrypt When Needed**:
```kotlin
// Manually decrypt
val decrypted = ConfigEncryption.decrypt(encryptedValue)

// Or use extension function
val apiConfig = config.exchanges.binance?.api?.decrypted()
```

### **Encryption Best Practices**

✅ **DO**:
- Generate a unique master key for each environment
- Store master keys in secure vaults (not in code/config)
- Use environment variables for master keys
- Rotate keys periodically
- Encrypt API keys, secrets, passwords

❌ **DON'T**:
- Commit master keys to git
- Share master keys via email/chat
- Use the same master key across environments
- Store master keys in configuration files

---

## 🔄 **Hot-Reload Configuration**

Configuration can be reloaded without restarting the application:

```kotlin
// Reload configuration
val updatedConfig = ConfigManager.reload()

// Reload with specific environment
val updatedConfig = ConfigManager.reload(environment = "prod")
```

**Use Cases**:
- Update logging levels without restart
- Adjust rate limiting parameters
- Change monitoring thresholds
- Update non-critical settings

**Limitations**:
- Cannot change server port (requires restart)
- Cannot change database path (requires restart)
- Sensitive data (API keys) require restart for security

---

## 🎯 **Command-Line Overrides**

Override any configuration via command-line arguments:

```bash
# Override server port
java -Dserver.port=9090 -jar application.jar

# Override database path
java -Ddatabase.path=/custom/path/db -jar application.jar

# Multiple overrides
java -Dapp.environment=prod \
     -Dserver.port=8080 \
     -Ddatabase.path=/data/prod.db \
     -Dlogging.level=DEBUG \
     -jar application.jar

# Custom config file
java -Dconfig.file=/path/to/custom.conf -jar application.jar
```

---

## ✅ **Configuration Validation**

All configurations are validated on load:

### **Validation Rules**

- **Server Port**: 0-65535 (0 for random port in testing)
- **Database Path**: Non-blank
- **Pool Sizes**: maximumPoolSize ≥ 1, minimumIdle ≤ maximumPoolSize
- **AI Trader Count**: 1-10
- **Leverage**: ≥ 1.0
- **Percentages**: 0.0 < value < 1.0
- **Demo Mode**: Must be enabled in v1.0
- **Real Trading**: Must be disabled in v1.0
- **Exchange API**: Keys/secrets required when exchange is enabled

### **Validation Failure**

If validation fails, a `ConfigurationException` is thrown with detailed error messages:

```
Configuration validation failed:
  - Server port must be between 0 and 65535, got: 99999
  - Max active traders must be between 1 and 10, got: 15
  - Binance API key is required when Binance is enabled
```

---

## 🎓 **Best Practices**

### **✅ DO**

1. **Use Environment Variables for Secrets**:
   ```hocon
   api.key = ${?BINANCE_API_KEY}
   ```

2. **Set Defaults in `reference.conf`**:
   ```hocon
   # reference.conf
   server.port = 8080
   ```

3. **Override in Environment Files**:
   ```hocon
   # application-dev.conf
   server.port = 8080
   
   # application-prod.conf
   server.port = 443
   ```

4. **Use Strongly-Typed Access**:
   ```kotlin
   val port = config.server.port  // Type-safe Int
   ```

5. **Validate Early**:
   ```kotlin
   // ConfigManager validates on load
   val config = ConfigManager.load()
   ```

6. **Document Configuration Changes**:
   - Update this guide
   - Add comments in config files
   - Document environment variables

### **❌ DON'T**

1. **Never Commit Secrets**:
   ```hocon
   # BAD
   api.key = "actual-secret-key"
   
   # GOOD
   api.key = ${?API_KEY}
   ```

2. **Don't Hardcode Environment-Specific Values**:
   ```kotlin
   // BAD
   val port = if (isDev) 8080 else 443
   
   // GOOD
   val port = config.server.port
   ```

3. **Don't Skip Validation**:
   ```kotlin
   // BAD - manual parsing without validation
   val port = rawConfig.getInt("server.port")
   
   // GOOD - use ConfigManager (validates)
   val port = config.server.port
   ```

---

## 🐛 **Troubleshooting**

### **Issue: Configuration Not Loading**

**Symptoms**: Application fails to start with configuration errors

**Solutions**:
1. Check environment variable is set correctly:
   ```bash
   echo $APP_ENVIRONMENT
   ```

2. Verify configuration file exists:
   ```bash
   ls core-service/src/main/resources/application-{env}.conf
   ```

3. Check for syntax errors in HOCON files

4. Review validation errors in logs

### **Issue: Environment Variables Not Working**

**Symptoms**: Configuration uses default values instead of environment variables

**Solutions**:
1. Verify environment variable naming:
   - Use UPPERCASE with underscores
   - Match the pattern in config: `${?VAR_NAME}`

2. Check environment variable is exported:
   ```bash
   printenv | grep FMPS
   ```

3. Restart application after setting variables

### **Issue: Encryption Fails**

**Symptoms**: `ConfigurationException: Master key not configured`

**Solutions**:
1. Generate and set master key:
   ```bash
   export FMPS_MASTER_KEY=$(your_generated_key)
   ```

2. Verify master key format (Base64, 44 characters)

3. Check environment variable is accessible

### **Issue: Hot-Reload Not Working**

**Symptoms**: Configuration changes don't take effect

**Solutions**:
1. Call `ConfigManager.reload()` explicitly

2. Check if value is cached elsewhere in application

3. Verify configuration file was actually saved

4. Some values (port, database path) require restart

---

## 📚 **References**

### **HOCON Documentation**
- [Typesafe Config GitHub](https://github.com/lightbend/config)
- [HOCON Specification](https://github.com/lightbend/config/blob/master/HOCON.md)

### **Related Documentation**
- `DEVELOPMENT_WORKFLOW.md` - Development process
- `SETUP_GUIDE.md` - Initial setup instructions
- `LOGGING_GUIDE.md` - Logging configuration details

### **Code References**
- `ConfigManager.kt` - Main configuration loader
- `ConfigModels.kt` - Configuration data classes
- `ConfigEncryption.kt` - Encryption utilities

---

## 📞 **Support**

For configuration-related questions or issues:

1. Check this guide first
2. Review example configurations in `application-{env}.conf`
3. Check unit tests in `ConfigManagerTest.kt`
4. Contact the development team

---

**Version**: 1.0  
**Last Updated**: October 28, 2025  
**Maintained By**: Development Team

