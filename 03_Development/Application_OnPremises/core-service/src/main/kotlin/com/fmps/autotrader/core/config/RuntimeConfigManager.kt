package com.fmps.autotrader.core.config

import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

/**
 * Runtime configuration manager for dynamic configuration changes.
 * 
 * Stores configuration values that can be changed at runtime without restarting the service.
 * These values override the static configuration from config files.
 * 
 * Thread-safe singleton for managing runtime configuration.
 */
object RuntimeConfigManager {
    
    // Runtime configuration values (with defaults from static config)
    private val updateIntervalMs = AtomicLong(5000L) // Default: 5 seconds
    private val telemetryHeartbeatIntervalSeconds = AtomicLong(15L) // Default: 15 seconds
    private val loggingLevel = AtomicReference<String>("INFO") // Default: INFO
    
    // Trader Defaults
    private val traderDefaults = AtomicReference<TraderDefaults>(
        TraderDefaults(
            budgetUsd = 1000.0,
            leverage = 3,
            stopLossPercent = 5.0,
            takeProfitPercent = 5.0,
            strategy = "TREND_FOLLOWING"
        )
    )
    
    /**
     * Gets the current update interval in milliseconds.
     * Returns the runtime value if set, otherwise falls back to static config.
     */
    fun getUpdateIntervalMs(): Long {
        return updateIntervalMs.get()
    }
    
    /**
     * Sets the update interval in milliseconds.
     * @param seconds The interval in seconds (will be converted to milliseconds)
     */
    fun setUpdateIntervalSeconds(seconds: Int) {
        require(seconds in 1..600) { "Update interval must be between 1-600 seconds" }
        val ms = seconds * 1000L
        updateIntervalMs.set(ms)
        logger.info { "Runtime config updated: updateIntervalMs = $ms (from ${seconds}s)" }
    }
    
    /**
     * Gets the current telemetry heartbeat interval in seconds.
     * Returns the runtime value if set, otherwise falls back to static config.
     */
    fun getTelemetryHeartbeatIntervalSeconds(): Long {
        return telemetryHeartbeatIntervalSeconds.get()
    }
    
    /**
     * Sets the telemetry heartbeat interval in seconds.
     */
    fun setTelemetryHeartbeatIntervalSeconds(seconds: Int) {
        require(seconds in 1..120) { "Telemetry heartbeat interval must be between 1-120 seconds" }
        telemetryHeartbeatIntervalSeconds.set(seconds.toLong())
        logger.info { "Runtime config updated: telemetryHeartbeatIntervalSeconds = $seconds" }
    }
    
    /**
     * Gets the current logging level.
     * Returns the runtime value if set, otherwise falls back to static config.
     */
    fun getLoggingLevel(): String {
        return loggingLevel.get()
    }
    
    /**
     * Sets the logging level.
     * @param level The logging level (TRACE, DEBUG, INFO, WARN, ERROR)
     */
    fun setLoggingLevel(level: String) {
        val validLevels = setOf("TRACE", "DEBUG", "INFO", "WARN", "ERROR")
        require(level.uppercase() in validLevels) { "Logging level must be one of: $validLevels" }
        loggingLevel.set(level.uppercase())
        logger.info { "Runtime config updated: loggingLevel = ${level.uppercase()}" }
    }
    
    /**
     * Initializes runtime config with values from static config.
     * Should be called during application startup.
     */
    fun initializeFromStaticConfig(config: AppConfig) {
        // Initialize from static config if not already set
        updateIntervalMs.set(config.aiTrader.monitoring.updateIntervalMs)
        // Telemetry config is not in AppConfig, read from Typesafe Config directly
        val telemetryConfig = com.typesafe.config.ConfigFactory.load()
        val heartbeatInterval = if (telemetryConfig.hasPath("telemetry.heartbeatIntervalSeconds")) {
            telemetryConfig.getLong("telemetry.heartbeatIntervalSeconds")
        } else {
            15L // Default
        }
        telemetryHeartbeatIntervalSeconds.set(heartbeatInterval)
        loggingLevel.set(config.logging.level.uppercase())
        logger.info { "Runtime config initialized from static config" }
    }
    
    /**
     * Gets the current trader defaults.
     */
    fun getTraderDefaults(): TraderDefaults {
        return traderDefaults.get()
    }
    
    /**
     * Sets the trader defaults.
     */
    fun setTraderDefaults(defaults: TraderDefaults) {
        traderDefaults.set(defaults)
        logger.info { "Runtime trader defaults updated: budgetUsd=${defaults.budgetUsd}, leverage=${defaults.leverage}, stopLossPercent=${defaults.stopLossPercent}, strategy=${defaults.strategy}" }
    }
}

/**
 * Trader defaults configuration.
 */
data class TraderDefaults(
    val budgetUsd: Double,
    val leverage: Int,
    val stopLossPercent: Double,
    val takeProfitPercent: Double,
    val strategy: String // TradingStrategy enum name
)

