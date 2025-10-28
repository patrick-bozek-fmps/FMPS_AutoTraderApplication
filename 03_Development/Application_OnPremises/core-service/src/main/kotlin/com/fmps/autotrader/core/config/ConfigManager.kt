package com.fmps.autotrader.core.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Central configuration manager for the FMPS AutoTrader application.
 * 
 * Loads configuration from:
 * 1. reference.conf (defaults)
 * 2. application.conf (base configuration)
 * 3. application-{environment}.conf (environment-specific overrides)
 * 4. Environment variables
 * 5. System properties (command-line arguments)
 * 
 * Usage:
 * ```kotlin
 * val config = ConfigManager.load()
 * val serverPort = config.server.port
 * ```
 */
object ConfigManager {
    
    private var loadedConfig: AppConfig? = null
    
    /**
     * Loads the application configuration based on the current environment.
     * 
     * @param environment The environment name (dev, test, prod). If null, reads from system property "app.environment" or defaults to "dev"
     * @param customConfigPath Optional path to a custom configuration file
     * @return The loaded application configuration
     * @throws ConfigurationException if configuration loading or validation fails
     */
    fun load(
        environment: String? = null,
        customConfigPath: String? = null
    ): AppConfig {
        val env = environment 
            ?: System.getProperty("app.environment") 
            ?: System.getenv("APP_ENVIRONMENT") 
            ?: "dev"
        
        logger.info { "Loading configuration for environment: $env" }
        
        try {
            // Load configuration hierarchy
            val config = loadConfigHierarchy(env, customConfigPath)
            
            // Parse into strongly-typed configuration
            val appConfig = parseConfig(config)
            
            // Validate configuration
            validateConfig(appConfig)
            
            loadedConfig = appConfig
            logger.info { "Configuration loaded successfully" }
            logger.debug { "Server will run on ${appConfig.server.host}:${appConfig.server.port}" }
            
            return appConfig
        } catch (e: Exception) {
            val message = "Failed to load configuration: ${e.message}"
            logger.error(e) { message }
            throw ConfigurationException(message, e)
        }
    }
    
    /**
     * Returns the currently loaded configuration.
     * Throws if configuration hasn't been loaded yet.
     */
    fun get(): AppConfig {
        return loadedConfig ?: throw ConfigurationException("Configuration not loaded. Call ConfigManager.load() first.")
    }
    
    /**
     * Reloads the configuration from disk.
     * Useful for hot-reload scenarios.
     */
    fun reload(environment: String? = null): AppConfig {
        logger.info { "Reloading configuration..." }
        ConfigFactory.invalidateCaches()
        return load(environment)
    }
    
    /**
     * Loads the configuration hierarchy in order:
     * 1. reference.conf (from classpath)
     * 2. application.conf (from classpath)
     * 3. application-{env}.conf (from classpath)
     * 4. Custom config file (if provided)
     * 5. System properties (command-line args)
     * 6. Environment variables
     */
    private fun loadConfigHierarchy(environment: String, customConfigPath: String?): Config {
        var config = ConfigFactory.load()
        
        // Load environment-specific configuration
        val envConfigName = "application-$environment"
        try {
            val envConfig = ConfigFactory.load(envConfigName)
            config = envConfig.withFallback(config)
            logger.debug { "Loaded environment configuration: $envConfigName.conf" }
        } catch (e: Exception) {
            logger.warn { "Environment configuration not found: $envConfigName.conf (using defaults)" }
        }
        
        // Load custom configuration file if provided
        if (customConfigPath != null) {
            val customFile = File(customConfigPath)
            if (customFile.exists()) {
                val customConfig = ConfigFactory.parseFile(customFile)
                config = customConfig.withFallback(config)
                logger.info { "Loaded custom configuration from: $customConfigPath" }
            } else {
                logger.warn { "Custom configuration file not found: $customConfigPath" }
            }
        }
        
        // Resolve all substitutions
        config = config.resolve()
        
        return config
    }
    
    /**
     * Parses the Typesafe Config into strongly-typed configuration objects.
     */
    private fun parseConfig(config: Config): AppConfig {
        return AppConfig(
            app = AppInfo(
                name = config.getString("app.name"),
                version = config.getString("app.version"),
                environment = config.getString("app.environment")
            ),
            server = ServerConfig(
                host = config.getString("server.host"),
                port = config.getInt("server.port"),
                requestTimeout = config.getDuration("server.requestTimeout"),
                connectionTimeout = config.getDuration("server.connectionTimeout"),
                cors = CorsConfig(
                    enabled = config.getBoolean("server.cors.enabled"),
                    allowedHosts = config.getStringList("server.cors.allowedHosts")
                ),
                websocket = WebSocketConfig(
                    enabled = config.getBoolean("server.websocket.enabled"),
                    pingIntervalMs = config.getLong("server.websocket.pingIntervalMs"),
                    timeoutMs = config.getLong("server.websocket.timeoutMs"),
                    maxFrameSize = config.getLong("server.websocket.maxFrameSize")
                )
            ),
            database = DatabaseConfig(
                path = config.getString("database.path"),
                pool = PoolConfig(
                    maximumPoolSize = config.getInt("database.pool.maximumPoolSize"),
                    minimumIdle = config.getInt("database.pool.minimumIdle"),
                    connectionTimeout = config.getLong("database.pool.connectionTimeout"),
                    idleTimeout = config.getLong("database.pool.idleTimeout"),
                    maxLifetime = config.getLong("database.pool.maxLifetime")
                ),
                migration = MigrationConfig(
                    enabled = config.getBoolean("database.migration.enabled"),
                    cleanOnValidationError = config.getBoolean("database.migration.cleanOnValidationError"),
                    validateOnMigrate = config.getBoolean("database.migration.validateOnMigrate")
                )
            ),
            logging = LoggingConfig(
                level = config.getString("logging.level"),
                file = LogFileConfig(
                    enabled = config.getBoolean("logging.file.enabled"),
                    path = config.getString("logging.file.path"),
                    maxSize = config.getString("logging.file.maxSize"),
                    maxHistory = config.getInt("logging.file.maxHistory"),
                    totalSizeCap = config.getString("logging.file.totalSizeCap")
                ),
                console = LogConsoleConfig(
                    enabled = config.getBoolean("logging.console.enabled"),
                    colorOutput = config.getBoolean("logging.console.colorOutput")
                ),
                loggers = if (config.hasPath("logging.loggers")) {
                    config.getObject("logging.loggers").entries.associate { 
                        it.key to it.value.unwrapped().toString() 
                    }
                } else {
                    emptyMap()
                }
            ),
            exchanges = ExchangesConfig(
                binance = if (config.hasPath("exchanges.binance")) {
                    ExchangeConfig(
                        enabled = config.getBoolean("exchanges.binance.enabled"),
                        api = ApiConfig(
                            key = config.getString("exchanges.binance.api.key"),
                            secret = config.getString("exchanges.binance.api.secret"),
                            passphrase = null,
                            baseUrl = config.getString("exchanges.binance.api.baseUrl"),
                            websocketUrl = config.getString("exchanges.binance.api.websocketUrl")
                        ),
                        rateLimit = RateLimitConfig(
                            requestsPerMinute = config.getInt("exchanges.binance.rateLimit.requestsPerMinute"),
                            requestsPerSecond = config.getInt("exchanges.binance.rateLimit.requestsPerSecond")
                        )
                    )
                } else null,
                bitget = if (config.hasPath("exchanges.bitget")) {
                    ExchangeConfig(
                        enabled = config.getBoolean("exchanges.bitget.enabled"),
                        api = ApiConfig(
                            key = config.getString("exchanges.bitget.api.key"),
                            secret = config.getString("exchanges.bitget.api.secret"),
                            passphrase = if (config.hasPath("exchanges.bitget.api.passphrase")) 
                                config.getString("exchanges.bitget.api.passphrase") else null,
                            baseUrl = config.getString("exchanges.bitget.api.baseUrl"),
                            websocketUrl = config.getString("exchanges.bitget.api.websocketUrl")
                        ),
                        rateLimit = RateLimitConfig(
                            requestsPerMinute = config.getInt("exchanges.bitget.rateLimit.requestsPerMinute"),
                            requestsPerSecond = config.getInt("exchanges.bitget.rateLimit.requestsPerSecond")
                        )
                    )
                } else null
            ),
            aiTrader = AITraderConfig(
                maxActiveTraders = config.getInt("aiTrader.maxActiveTraders"),
                defaultRisk = RiskManagementConfig(
                    maxLeverage = config.getDouble("aiTrader.defaultRisk.maxLeverage"),
                    defaultStopLossPercentage = config.getDouble("aiTrader.defaultRisk.defaultStopLossPercentage"),
                    defaultTakeProfitPercentage = config.getDouble("aiTrader.defaultRisk.defaultTakeProfitPercentage"),
                    maxDailyLossPercentage = config.getDouble("aiTrader.defaultRisk.maxDailyLossPercentage"),
                    positionSizePercentage = config.getDouble("aiTrader.defaultRisk.positionSizePercentage"),
                    minProfitLossRatio = config.getDouble("aiTrader.defaultRisk.minProfitLossRatio")
                ),
                defaultStrategy = StrategyDefaultsConfig(
                    analysisPeriod = config.getDuration("aiTrader.defaultStrategy.analysisPeriod"),
                    minConfidenceScore = config.getDouble("aiTrader.defaultStrategy.minConfidenceScore"),
                    patternsRequired = config.getInt("aiTrader.defaultStrategy.patternsRequired")
                ),
                monitoring = MonitoringConfig(
                    updateIntervalMs = config.getLong("aiTrader.monitoring.updateIntervalMs"),
                    alertOnLoss = config.getBoolean("aiTrader.monitoring.alertOnLoss"),
                    alertThreshold = config.getDouble("aiTrader.monitoring.alertThreshold")
                )
            ),
            trading = TradingConfig(
                demoMode = DemoModeConfig(
                    enabled = config.getBoolean("trading.demoMode.enabled"),
                    initialBalance = config.getDouble("trading.demoMode.initialBalance"),
                    allowRealTrading = config.getBoolean("trading.demoMode.allowRealTrading")
                ),
                marketData = MarketDataConfig(
                    candlestickRefreshMs = config.getLong("trading.marketData.candlestickRefreshMs"),
                    orderBookRefreshMs = config.getLong("trading.marketData.orderBookRefreshMs"),
                    tickerRefreshMs = config.getLong("trading.marketData.tickerRefreshMs")
                ),
                orders = OrdersConfig(
                    defaultTimeoutMs = config.getLong("trading.orders.defaultTimeoutMs"),
                    retryFailedOrders = config.getBoolean("trading.orders.retryFailedOrders"),
                    maxRetries = config.getInt("trading.orders.maxRetries")
                )
            ),
            security = SecurityConfig(
                encryption = EncryptionConfig(
                    enabled = config.getBoolean("security.encryption.enabled"),
                    algorithm = config.getString("security.encryption.algorithm"),
                    keySize = config.getInt("security.encryption.keySize")
                ),
                session = SessionConfig(
                    timeoutMinutes = config.getInt("security.session.timeoutMinutes"),
                    extendOnActivity = config.getBoolean("security.session.extendOnActivity")
                )
            ),
            performance = PerformanceConfig(
                threadPool = ThreadPoolConfig(
                    corePoolSize = config.getInt("performance.threadPool.corePoolSize"),
                    maxPoolSize = config.getInt("performance.threadPool.maxPoolSize"),
                    queueCapacity = config.getInt("performance.threadPool.queueCapacity"),
                    keepAliveSeconds = config.getInt("performance.threadPool.keepAliveSeconds")
                ),
                metrics = MetricsConfig(
                    enabled = config.getBoolean("performance.metrics.enabled"),
                    collectionIntervalMs = config.getLong("performance.metrics.collectionIntervalMs"),
                    retentionDays = config.getInt("performance.metrics.retentionDays")
                ),
                healthCheck = HealthCheckConfig(
                    enabled = config.getBoolean("performance.healthCheck.enabled"),
                    intervalSeconds = config.getInt("performance.healthCheck.intervalSeconds")
                )
            )
        )
    }
    
    /**
     * Validates the loaded configuration for correctness and consistency.
     */
    private fun validateConfig(config: AppConfig) {
        val errors = mutableListOf<String>()
        
        // Validate server configuration (port 0 allowed for random port in testing)
        if (config.server.port !in 0..65535) {
            errors.add("Server port must be between 0 and 65535, got: ${config.server.port}")
        }
        
        // Validate database configuration
        if (config.database.path.isBlank()) {
            errors.add("Database path cannot be blank")
        }
        
        if (config.database.pool.maximumPoolSize < 1) {
            errors.add("Database pool maximum size must be at least 1")
        }
        
        if (config.database.pool.minimumIdle > config.database.pool.maximumPoolSize) {
            errors.add("Database pool minimum idle cannot exceed maximum pool size")
        }
        
        // Validate AI trader configuration
        if (config.aiTrader.maxActiveTraders < 1 || config.aiTrader.maxActiveTraders > 10) {
            errors.add("Max active traders must be between 1 and 10, got: ${config.aiTrader.maxActiveTraders}")
        }
        
        if (config.aiTrader.defaultRisk.maxLeverage < 1.0) {
            errors.add("Max leverage must be at least 1.0")
        }
        
        if (config.aiTrader.defaultRisk.defaultStopLossPercentage <= 0.0 || 
            config.aiTrader.defaultRisk.defaultStopLossPercentage >= 1.0) {
            errors.add("Stop loss percentage must be between 0.0 and 1.0")
        }
        
        if (config.aiTrader.defaultRisk.defaultTakeProfitPercentage <= 0.0 || 
            config.aiTrader.defaultRisk.defaultTakeProfitPercentage >= 1.0) {
            errors.add("Take profit percentage must be between 0.0 and 1.0")
        }
        
        // Validate demo mode (v1.0 must be demo only)
        if (!config.trading.demoMode.enabled) {
            logger.warn { "Demo mode is disabled. For v1.0, demo mode should always be enabled." }
        }
        
        if (config.trading.demoMode.allowRealTrading) {
            errors.add("Real trading is not allowed in v1.0. Set trading.demoMode.allowRealTrading to false.")
        }
        
        // Validate exchange configurations if enabled
        if (config.exchanges.binance?.enabled == true) {
            if (config.exchanges.binance.api.key.isBlank()) {
                errors.add("Binance API key is required when Binance is enabled")
            }
            if (config.exchanges.binance.api.secret.isBlank()) {
                errors.add("Binance API secret is required when Binance is enabled")
            }
        }
        
        if (config.exchanges.bitget?.enabled == true) {
            if (config.exchanges.bitget.api.key.isBlank()) {
                errors.add("Bitget API key is required when Bitget is enabled")
            }
            if (config.exchanges.bitget.api.secret.isBlank()) {
                errors.add("Bitget API secret is required when Bitget is enabled")
            }
        }
        
        // Throw exception if any validation errors
        if (errors.isNotEmpty()) {
            val message = "Configuration validation failed:\n" + errors.joinToString("\n") { "  - $it" }
            throw ConfigurationException(message)
        }
        
        logger.info { "Configuration validation passed" }
    }
}

