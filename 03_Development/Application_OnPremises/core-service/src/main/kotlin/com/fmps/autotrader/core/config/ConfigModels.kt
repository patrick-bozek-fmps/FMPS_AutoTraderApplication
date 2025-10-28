package com.fmps.autotrader.core.config

import java.time.Duration

/**
 * Root configuration object containing all application settings.
 */
data class AppConfig(
    val app: AppInfo,
    val server: ServerConfig,
    val database: DatabaseConfig,
    val logging: LoggingConfig,
    val exchanges: ExchangesConfig,
    val aiTrader: AITraderConfig,
    val trading: TradingConfig,
    val security: SecurityConfig,
    val performance: PerformanceConfig
)

/**
 * Application information.
 */
data class AppInfo(
    val name: String,
    val version: String,
    val environment: String
) {
    val isDevelopment: Boolean get() = environment == "dev"
    val isTest: Boolean get() = environment == "test"
    val isProduction: Boolean get() = environment == "production" || environment == "prod"
}

/**
 * Server configuration for the REST API and WebSocket server.
 */
data class ServerConfig(
    val host: String,
    val port: Int,
    val requestTimeout: Duration,
    val connectionTimeout: Duration,
    val cors: CorsConfig,
    val websocket: WebSocketConfig
)

/**
 * CORS (Cross-Origin Resource Sharing) configuration.
 */
data class CorsConfig(
    val enabled: Boolean,
    val allowedHosts: List<String>
)

/**
 * WebSocket server configuration.
 */
data class WebSocketConfig(
    val enabled: Boolean,
    val pingIntervalMs: Long,
    val timeoutMs: Long,
    val maxFrameSize: Long
)

/**
 * Database configuration.
 */
data class DatabaseConfig(
    val path: String,
    val pool: PoolConfig,
    val migration: MigrationConfig
)

/**
 * Database connection pool configuration (HikariCP).
 */
data class PoolConfig(
    val maximumPoolSize: Int,
    val minimumIdle: Int,
    val connectionTimeout: Long,
    val idleTimeout: Long,
    val maxLifetime: Long
)

/**
 * Database migration configuration (Flyway).
 */
data class MigrationConfig(
    val enabled: Boolean,
    val cleanOnValidationError: Boolean,
    val validateOnMigrate: Boolean
)

/**
 * Logging configuration.
 */
data class LoggingConfig(
    val level: String,
    val file: LogFileConfig,
    val console: LogConsoleConfig,
    val loggers: Map<String, String>
)

/**
 * File logging configuration.
 */
data class LogFileConfig(
    val enabled: Boolean,
    val path: String,
    val maxSize: String,
    val maxHistory: Int,
    val totalSizeCap: String
)

/**
 * Console logging configuration.
 */
data class LogConsoleConfig(
    val enabled: Boolean,
    val colorOutput: Boolean
)

/**
 * Exchange configurations container.
 */
data class ExchangesConfig(
    val binance: ExchangeConfig?,
    val bitget: ExchangeConfig?
)

/**
 * Individual exchange configuration.
 */
data class ExchangeConfig(
    val enabled: Boolean,
    val api: ApiConfig,
    val rateLimit: RateLimitConfig
)

/**
 * API credentials and endpoints for an exchange.
 */
data class ApiConfig(
    val key: String,
    val secret: String,
    val passphrase: String?,
    val baseUrl: String,
    val websocketUrl: String
) {
    /**
     * Returns a masked version of the API key for safe logging.
     */
    fun maskedKey(): String {
        if (key.isBlank() || key.length <= 8) return "****"
        return "${key.substring(0, 4)}...${key.substring(key.length - 4)}"
    }
    
    /**
     * Returns true if API credentials are configured.
     */
    fun hasCredentials(): Boolean {
        return key.isNotBlank() && secret.isNotBlank()
    }
}

/**
 * Rate limiting configuration for exchange API calls.
 */
data class RateLimitConfig(
    val requestsPerMinute: Int,
    val requestsPerSecond: Int
)

/**
 * AI Trader configuration.
 */
data class AITraderConfig(
    val maxActiveTraders: Int,
    val defaultRisk: RiskManagementConfig,
    val defaultStrategy: StrategyDefaultsConfig,
    val monitoring: MonitoringConfig
)

/**
 * Risk management configuration for AI traders.
 */
data class RiskManagementConfig(
    val maxLeverage: Double,
    val defaultStopLossPercentage: Double,
    val defaultTakeProfitPercentage: Double,
    val maxDailyLossPercentage: Double,
    val positionSizePercentage: Double,
    val minProfitLossRatio: Double
)

/**
 * Default strategy configuration for AI traders.
 */
data class StrategyDefaultsConfig(
    val analysisPeriod: Duration,
    val minConfidenceScore: Double,
    val patternsRequired: Int
)

/**
 * Monitoring configuration for AI traders.
 */
data class MonitoringConfig(
    val updateIntervalMs: Long,
    val alertOnLoss: Boolean,
    val alertThreshold: Double
)

/**
 * Trading system configuration.
 */
data class TradingConfig(
    val demoMode: DemoModeConfig,
    val marketData: MarketDataConfig,
    val orders: OrdersConfig
)

/**
 * Demo mode configuration.
 */
data class DemoModeConfig(
    val enabled: Boolean,
    val initialBalance: Double,
    val allowRealTrading: Boolean
)

/**
 * Market data refresh intervals.
 */
data class MarketDataConfig(
    val candlestickRefreshMs: Long,
    val orderBookRefreshMs: Long,
    val tickerRefreshMs: Long
)

/**
 * Order execution configuration.
 */
data class OrdersConfig(
    val defaultTimeoutMs: Long,
    val retryFailedOrders: Boolean,
    val maxRetries: Int
)

/**
 * Security configuration.
 */
data class SecurityConfig(
    val encryption: EncryptionConfig,
    val session: SessionConfig
)

/**
 * Encryption configuration for sensitive data.
 */
data class EncryptionConfig(
    val enabled: Boolean,
    val algorithm: String,
    val keySize: Int
)

/**
 * Session management configuration.
 */
data class SessionConfig(
    val timeoutMinutes: Int,
    val extendOnActivity: Boolean
)

/**
 * Performance and monitoring configuration.
 */
data class PerformanceConfig(
    val threadPool: ThreadPoolConfig,
    val metrics: MetricsConfig,
    val healthCheck: HealthCheckConfig
)

/**
 * Thread pool configuration.
 */
data class ThreadPoolConfig(
    val corePoolSize: Int,
    val maxPoolSize: Int,
    val queueCapacity: Int,
    val keepAliveSeconds: Int
)

/**
 * Metrics collection configuration.
 */
data class MetricsConfig(
    val enabled: Boolean,
    val collectionIntervalMs: Long,
    val retentionDays: Int
)

/**
 * Health check configuration.
 */
data class HealthCheckConfig(
    val enabled: Boolean,
    val intervalSeconds: Int
)

