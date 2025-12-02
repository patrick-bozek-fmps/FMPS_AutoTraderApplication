package com.fmps.autotrader.desktop.services

import kotlinx.coroutines.flow.Flow

interface ConfigService {
    fun configuration(): Flow<ConfigurationSnapshot>

    suspend fun saveExchangeSettings(settings: ExchangeSettings)

    suspend fun saveGeneralSettings(settings: GeneralSettings)

    suspend fun saveTraderDefaults(defaults: TraderDefaults)

    suspend fun testExchangeConnection(settings: ExchangeSettings): ConnectionTestResult

    suspend fun exportConfiguration(): String

    suspend fun importConfiguration(serialized: String): ConfigurationSnapshot
    
    /**
     * Gets saved exchange settings for a specific exchange.
     * This allows the ViewModel to query the service's cache directly.
     * This is synchronous since it just reads from cache.
     */
    fun getExchangeSettings(exchange: Exchange): ExchangeSettings?
    
    /**
     * Gets the last saved timestamp for a specific exchange.
     * Returns null if the exchange has never been saved.
     * This is synchronous since it just reads from cache.
     */
    fun getExchangeTimestamp(exchange: Exchange): Long?
    
    /**
     * Saves the timestamp for an exchange.
     * This is called after successfully saving exchange settings.
     */
    fun saveExchangeTimestamp(exchange: Exchange, timestamp: Long)
}

data class ConfigurationSnapshot(
    val exchange: ExchangeSettings = ExchangeSettings(),
    val general: GeneralSettings = GeneralSettings(),
    val traderDefaults: TraderDefaults = TraderDefaults()
)

data class ExchangeSettings(
    val apiKey: String = "",
    val secretKey: String = "",
    val passphrase: String = "",
    val exchange: Exchange = Exchange.BINANCE
)

enum class Exchange { BINANCE, BITGET }

data class GeneralSettings(
    val updateIntervalSeconds: Int = 30,
    val telemetryPollingSeconds: Int = 5,
    val loggingLevel: LoggingLevel = LoggingLevel.INFO,
    val theme: ThemePreference = ThemePreference.AUTO
)

enum class LoggingLevel { TRACE, DEBUG, INFO, WARN, ERROR }
enum class ThemePreference { LIGHT, DARK, AUTO }

data class TraderDefaults(
    val budgetUsd: Double = 1000.0,
    val leverage: Int = 3,
    val stopLossPercent: Double = 5.0,
    val takeProfitPercent: Double = 5.0,
    val strategy: String = "TREND_FOLLOWING" // TradingStrategy enum name
)

data class ConnectionTestResult(
    val success: Boolean,
    val message: String
)

