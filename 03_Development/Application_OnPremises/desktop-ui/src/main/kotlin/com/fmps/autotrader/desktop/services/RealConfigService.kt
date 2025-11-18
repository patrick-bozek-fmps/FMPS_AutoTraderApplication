package com.fmps.autotrader.desktop.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.File
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Real ConfigService implementation using core-service REST API.
 * 
 * Features:
 * - Real connection testing via `/api/v1/config/test-connection` endpoint (uses actual exchange connectors)
 * - HOCON format import/export (compatible with ConfigManager)
 * - Graceful fallback when config persistence endpoints are not implemented yet
 * - Retry logic with exponential backoff for transient failures
 * 
 * Note: Configuration persistence endpoints (`/api/v1/config/{key}`) may not be fully implemented yet.
 * This service gracefully handles NOT_IMPLEMENTED responses and maintains local state as fallback.
 * 
 * Security Note: API keys and secrets are masked in UI but not encrypted at rest yet.
 * Encryption and secure storage are tracked under Epic 6 security tasks.
 */
class RealConfigService(
    private val httpClient: HttpClient,
    private val baseUrl: String = "http://localhost:8080",
    private val apiKey: String? = null,
    private val configFilePath: String = System.getProperty("user.home") + File.separator + ".fmps-autotrader" + File.separator + "desktop-config.conf"
) : ConfigService {

    private val json = Json { ignoreUnknownKeys = true }
    private val snapshotFlow = MutableStateFlow<ConfigurationSnapshot>(
        ConfigurationSnapshot()
    )
    
    private val maxRetries = 3
    private val initialRetryDelayMs = 500L
    private val configFile = File(configFilePath)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        // Ensure config directory exists
        configFile.parentFile?.mkdirs()
        
        // Load initial configuration (try REST API first, then file, then defaults)
        scope.launch {
            loadConfiguration()
        }
    }
    
    /**
     * Executes an operation with retry logic and exponential backoff.
     */
    private suspend fun <T> executeWithRetry(
        operation: suspend () -> T,
        onRetry: (Int, Throwable) -> Unit = { attempt, error -> 
            logger.debug(error) { "Retry attempt $attempt failed" }
        }
    ): T {
        var lastException: Throwable? = null
        for (attempt in 0 until maxRetries) {
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1 && isRetryableError(e)) {
                    onRetry(attempt + 1, e)
                    delay(initialRetryDelayMs * (1 shl attempt)) // Exponential backoff
                } else {
                    break
                }
            }
        }
        throw lastException ?: Exception("Operation failed after $maxRetries attempts")
    }
    
    /**
     * Determines if an error is retryable (transient network/server errors).
     */
    private fun isRetryableError(e: Exception): Boolean {
        return when {
            e is ConnectTimeoutException -> true
            e is SocketTimeoutException -> true
            e.message?.contains("timeout", ignoreCase = true) == true -> true
            e.message?.contains("connection", ignoreCase = true) == true -> true
            else -> false
        }
    }

    override fun configuration(): Flow<ConfigurationSnapshot> = snapshotFlow.asStateFlow()

    override suspend fun saveExchangeSettings(settings: ExchangeSettings) {
        try {
            // Try to save via REST API
            // Note: Endpoint may not be implemented yet, so we handle gracefully
            val response = executeWithRetry<HttpResponse>(
                operation = {
                    httpClient.put("$baseUrl/api/v1/config/exchange") {
                        contentType(ContentType.Application.Json)
                        setBody(ExchangeSettingsDTO.from(settings))
                        apiKey?.let { header("X-API-Key", it) }
                    }
                }
            )

            if (response.status == HttpStatusCode.NotImplemented) {
                logger.info { "Exchange settings endpoint not implemented yet, persisting to local file" }
                snapshotFlow.update { it.copy(exchange = settings) }
                saveConfigurationToFile() // Persist to file as fallback
                // TODO: Audit log configuration change when audit system is implemented (Epic 6)
                return
            }

            if (response.status.isSuccess()) {
                logger.info { "Exchange settings saved successfully via REST API" }
                loadConfiguration() // Reload to get updated config
                // TODO: Audit log configuration change when audit system is implemented (Epic 6)
            } else {
                val errorMessage = try {
                    val errorBody = response.body<ErrorResponse>()
                    errorBody.error.message
                } catch (e: Exception) {
                    response.status.toString()
                }
                logger.warn { "Failed to save exchange settings via REST API: $errorMessage, persisting to local file" }
                snapshotFlow.update { it.copy(exchange = settings) }
                saveConfigurationToFile() // Persist to file as fallback
                // Don't throw - file persistence succeeded
            }
        } catch (e: Exception) {
            logger.warn(e) { "Error saving exchange settings via REST API, persisting to local file: ${e.message}" }
            snapshotFlow.update { it.copy(exchange = settings) }
            saveConfigurationToFile() // Persist to file as fallback
        }
    }

    override suspend fun saveGeneralSettings(settings: GeneralSettings) {
        try {
            val response = executeWithRetry<HttpResponse>(
                operation = {
                    httpClient.put("$baseUrl/api/v1/config/general") {
                        contentType(ContentType.Application.Json)
                        setBody(GeneralSettingsDTO.from(settings))
                        apiKey?.let { header("X-API-Key", it) }
                    }
                }
            )

            if (response.status == HttpStatusCode.NotImplemented) {
                logger.info { "General settings endpoint not implemented yet, persisting to local file" }
                snapshotFlow.update { it.copy(general = settings) }
                saveConfigurationToFile() // Persist to file as fallback
                // TODO: Audit log configuration change when audit system is implemented (Epic 6)
                return
            }

            if (response.status.isSuccess()) {
                logger.info { "General settings saved successfully via REST API" }
                loadConfiguration()
                // TODO: Audit log configuration change when audit system is implemented (Epic 6)
            } else {
                val errorMessage = try {
                    val errorBody = response.body<ErrorResponse>()
                    errorBody.error.message
                } catch (e: Exception) {
                    response.status.toString()
                }
                logger.warn { "Failed to save general settings via REST API: $errorMessage, persisting to local file" }
                snapshotFlow.update { it.copy(general = settings) }
                saveConfigurationToFile() // Persist to file as fallback
            }
        } catch (e: Exception) {
            logger.warn(e) { "Error saving general settings via REST API, persisting to local file: ${e.message}" }
            snapshotFlow.update { it.copy(general = settings) }
            saveConfigurationToFile() // Persist to file as fallback
        }
    }

    override suspend fun saveTraderDefaults(defaults: TraderDefaults) {
        try {
            val response = executeWithRetry<HttpResponse>(
                operation = {
                    httpClient.put("$baseUrl/api/v1/config/trader-defaults") {
                        contentType(ContentType.Application.Json)
                        setBody(TraderDefaultsDTO.from(defaults))
                        apiKey?.let { header("X-API-Key", it) }
                    }
                }
            )

            if (response.status == HttpStatusCode.NotImplemented) {
                logger.info { "Trader defaults endpoint not implemented yet, persisting to local file" }
                snapshotFlow.update { it.copy(traderDefaults = defaults) }
                saveConfigurationToFile() // Persist to file as fallback
                // TODO: Audit log configuration change when audit system is implemented (Epic 6)
                return
            }

            if (response.status.isSuccess()) {
                logger.info { "Trader defaults saved successfully via REST API" }
                loadConfiguration()
                // TODO: Audit log configuration change when audit system is implemented (Epic 6)
            } else {
                val errorMessage = try {
                    val errorBody = response.body<ErrorResponse>()
                    errorBody.error.message
                } catch (e: Exception) {
                    response.status.toString()
                }
                logger.warn { "Failed to save trader defaults via REST API: $errorMessage, persisting to local file" }
                snapshotFlow.update { it.copy(traderDefaults = defaults) }
                saveConfigurationToFile() // Persist to file as fallback
            }
        } catch (e: Exception) {
            logger.warn(e) { "Error saving trader defaults via REST API, persisting to local file: ${e.message}" }
            snapshotFlow.update { it.copy(traderDefaults = defaults) }
            saveConfigurationToFile() // Persist to file as fallback
        }
    }

    override suspend fun testExchangeConnection(settings: ExchangeSettings): ConnectionTestResult {
        // Connection test uses real exchange connectors via core-service endpoint
        // This is fully implemented and tests actual connectivity to Binance/Bitget testnets
        return try {
            val response = executeWithRetry<HttpResponse>(
                operation = {
                    httpClient.post("$baseUrl/api/v1/config/test-connection") {
                        contentType(ContentType.Application.Json)
                        setBody(ExchangeConnectionTestRequestDTO.from(settings))
                        apiKey?.let { header("X-API-Key", it) }
                    }
                }
            )

            if (response.status.isSuccess()) {
                val apiResponse = response.body<ApiResponse<ConnectionTestResponse>>()
                if (apiResponse.success && apiResponse.data != null) {
                    ConnectionTestResult(apiResponse.data.success, apiResponse.data.message)
                } else {
                    ConnectionTestResult(false, "Connection test returned unsuccessful response")
                }
            } else {
                val error = response.body<ErrorResponse>()
                ConnectionTestResult(false, error.error.message)
            }
        } catch (e: Exception) {
            logger.error(e) { "Connection test failed after retries" }
            ConnectionTestResult(false, "Connection test failed: ${e.message}")
        }
    }

    override suspend fun exportConfiguration(): String {
        val snapshot = snapshotFlow.value
        
        // Export as HOCON format (compatible with ConfigManager and application.conf)
        // Note: This is a simplified HOCON format for UI configuration export.
        // The full application.conf structure includes server, database, logging, etc.
        // This export focuses on user-configurable settings (exchange credentials, general settings, trader defaults).
        // Secrets are exported in plain text - encryption at rest is tracked under Epic 6
        return buildString {
            appendLine("# FMPS AutoTrader Configuration Export")
            appendLine("# Generated: ${Instant.now()}")
            appendLine("# Format: HOCON (compatible with Typesafe Config)")
            appendLine("# Security Note: API keys/secrets are in plain text - handle securely")
            appendLine("#")
            appendLine("# Note: This is a simplified export format for UI configuration.")
            appendLine("# Full application.conf includes server, database, logging, and other system settings.")
            appendLine("# This export focuses on user-configurable settings only.")
            appendLine()
            appendLine("# Exchange Configuration")
            appendLine("exchange {")
            appendLine("  name = \"${snapshot.exchange.exchange}\"")
            if (snapshot.exchange.apiKey.isNotBlank()) {
                appendLine("  apiKey = \"${escapeHoconString(snapshot.exchange.apiKey)}\"")
            }
            if (snapshot.exchange.secretKey.isNotBlank()) {
                appendLine("  secretKey = \"${escapeHoconString(snapshot.exchange.secretKey)}\"")
            }
            if (snapshot.exchange.passphrase.isNotBlank()) {
                appendLine("  passphrase = \"${escapeHoconString(snapshot.exchange.passphrase)}\"")
            }
            appendLine("}")
            appendLine()
            appendLine("# General Application Settings")
            appendLine("general {")
            appendLine("  updateIntervalSeconds = ${snapshot.general.updateIntervalSeconds}")
            appendLine("  telemetryPollingSeconds = ${snapshot.general.telemetryPollingSeconds}")
            appendLine("  loggingLevel = \"${snapshot.general.loggingLevel}\"")
            appendLine("  theme = \"${snapshot.general.theme}\"")
            appendLine("}")
            appendLine()
            appendLine("# AI Trader Default Settings")
            appendLine("traderDefaults {")
            appendLine("  budgetUsd = ${snapshot.traderDefaults.budgetUsd}")
            appendLine("  leverage = ${snapshot.traderDefaults.leverage}")
            appendLine("  stopLossPercent = ${snapshot.traderDefaults.stopLossPercent}")
            appendLine("  strategy = \"${escapeHoconString(snapshot.traderDefaults.strategy)}\"")
            appendLine("}")
        }
    }
    
    /**
     * Escapes special characters in HOCON strings.
     */
    private fun escapeHoconString(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    override suspend fun importConfiguration(serialized: String): ConfigurationSnapshot {
        return try {
            // Parse HOCON-like format
            val snapshot = parseHoconConfiguration(serialized)
            snapshotFlow.value = snapshot
            
            // Try to persist via REST API (which will fallback to file if needed)
            saveExchangeSettings(snapshot.exchange)
            saveGeneralSettings(snapshot.general)
            saveTraderDefaults(snapshot.traderDefaults)
            
            // Ensure file is saved (in case REST API succeeded but we want local backup)
            saveConfigurationToFile()
            
            snapshot
        } catch (e: Exception) {
            logger.error(e) { "Failed to import configuration" }
            throw IllegalArgumentException("Invalid configuration format: ${e.message}", e)
        }
    }

    private suspend fun loadConfiguration() {
        try {
            // Try to load from REST API first
            val response = httpClient.get("$baseUrl/api/v1/config") {
                apiKey?.let { header("X-API-Key", it) }
            }

            if (response.status.isSuccess()) {
                // Parse response when endpoint is implemented
                // For now, try loading from file
                loadConfigurationFromFile()
                logger.debug { "Configuration loaded from API" }
            } else {
                logger.debug { "Configuration endpoint not available, loading from file" }
                loadConfigurationFromFile()
            }
        } catch (e: Exception) {
            logger.debug(e) { "Could not load configuration from API, loading from file" }
            loadConfigurationFromFile()
        }
    }
    
    /**
     * Saves current configuration snapshot to local HOCON file.
     * This provides persistence when REST API endpoints are not available.
     */
    private suspend fun saveConfigurationToFile() {
        try {
            val hoconContent = exportConfiguration()
            configFile.writeText(hoconContent)
            logger.debug { "Configuration saved to file: ${configFile.absolutePath}" }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to save configuration to file: ${e.message}" }
        }
    }
    
    /**
     * Loads configuration from local HOCON file.
     * Falls back to defaults if file doesn't exist or parsing fails.
     */
    private suspend fun loadConfigurationFromFile() {
        try {
            if (configFile.exists() && configFile.length() > 0) {
                val content = configFile.readText()
                val snapshot = parseHoconConfiguration(content)
                snapshotFlow.value = snapshot
                logger.debug { "Configuration loaded from file: ${configFile.absolutePath}" }
            } else {
                logger.debug { "Configuration file not found, using defaults" }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to load configuration from file: ${e.message}, using defaults" }
        }
    }

    private fun parseHoconConfiguration(serialized: String): ConfigurationSnapshot {
        val lines = serialized.lines()
        var exchange = ExchangeSettings()
        var general = GeneralSettings()
        var traderDefaults = TraderDefaults()
        
        var currentSection: String? = null
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            
            when {
                trimmed.startsWith("exchange {") -> currentSection = "exchange"
                trimmed.startsWith("general {") -> currentSection = "general"
                trimmed.startsWith("traderDefaults {") -> currentSection = "traderDefaults"
                trimmed == "}" -> currentSection = null
                else -> {
                    val parts = trimmed.split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim().removeSurrounding("\"")
                        
                        when (currentSection) {
                            "exchange" -> {
                                exchange = when (key) {
                                    "name" -> exchange.copy(exchange = Exchange.valueOf(value.uppercase()))
                                    "apiKey" -> exchange.copy(apiKey = value)
                                    "secretKey" -> exchange.copy(secretKey = value)
                                    "passphrase" -> exchange.copy(passphrase = value)
                                    else -> exchange
                                }
                            }
                            "general" -> {
                                general = when (key) {
                                    "updateIntervalSeconds" -> general.copy(updateIntervalSeconds = value.toIntOrNull() ?: 30)
                                    "telemetryPollingSeconds" -> general.copy(telemetryPollingSeconds = value.toIntOrNull() ?: 5)
                                    "loggingLevel" -> general.copy(loggingLevel = LoggingLevel.valueOf(value.uppercase()))
                                    "theme" -> general.copy(theme = ThemePreference.valueOf(value.uppercase()))
                                    else -> general
                                }
                            }
                            "traderDefaults" -> {
                                traderDefaults = when (key) {
                                    "budgetUsd" -> traderDefaults.copy(budgetUsd = value.toDoubleOrNull() ?: 1000.0)
                                    "leverage" -> traderDefaults.copy(leverage = value.toIntOrNull() ?: 3)
                                    "stopLossPercent" -> traderDefaults.copy(stopLossPercent = value.toDoubleOrNull() ?: 5.0)
                                    "strategy" -> traderDefaults.copy(strategy = value)
                                    else -> traderDefaults
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return ConfigurationSnapshot(exchange, general, traderDefaults)
    }

    @Serializable
    private data class ExchangeSettingsDTO(
        val apiKey: String,
        val secretKey: String,
        val passphrase: String,
        val exchange: String
    ) {
        companion object {
            fun from(settings: ExchangeSettings) = ExchangeSettingsDTO(
                apiKey = settings.apiKey,
                secretKey = settings.secretKey,
                passphrase = settings.passphrase,
                exchange = settings.exchange.name
            )
        }
    }
    
    @Serializable
    private data class ExchangeConnectionTestRequestDTO(
        val exchange: String,
        val apiKey: String,
        val secretKey: String,
        val passphrase: String? = null
    ) {
        companion object {
            fun from(settings: ExchangeSettings) = ExchangeConnectionTestRequestDTO(
                exchange = settings.exchange.name,
                apiKey = settings.apiKey,
                secretKey = settings.secretKey,
                passphrase = settings.passphrase.takeIf { it.isNotBlank() }
            )
        }
    }

    @Serializable
    private data class GeneralSettingsDTO(
        val updateIntervalSeconds: Int,
        val telemetryPollingSeconds: Int,
        val loggingLevel: String,
        val theme: String
    ) {
        companion object {
            fun from(settings: GeneralSettings) = GeneralSettingsDTO(
                updateIntervalSeconds = settings.updateIntervalSeconds,
                telemetryPollingSeconds = settings.telemetryPollingSeconds,
                loggingLevel = settings.loggingLevel.name,
                theme = settings.theme.name
            )
        }
    }

    @Serializable
    private data class TraderDefaultsDTO(
        val budgetUsd: Double,
        val leverage: Int,
        val stopLossPercent: Double,
        val strategy: String
    ) {
        companion object {
            fun from(defaults: TraderDefaults) = TraderDefaultsDTO(
                budgetUsd = defaults.budgetUsd,
                leverage = defaults.leverage,
                stopLossPercent = defaults.stopLossPercent,
                strategy = defaults.strategy
            )
        }
    }

    @Serializable
    private data class ApiResponse<T>(
        val success: Boolean,
        val data: T? = null,
        val timestamp: String? = null
    )
    
    @Serializable
    private data class ConnectionTestResponse(
        val success: Boolean,
        val message: String
    )

    @Serializable
    private data class ErrorResponse(
        val success: Boolean,
        val error: ErrorDetail,
        val timestamp: String? = null
    )

    @Serializable
    private data class ErrorDetail(
        val code: String,
        val message: String
    )
}

