package com.fmps.autotrader.core.api.routes

import com.fmps.autotrader.core.connectors.ConnectorFactory
import com.fmps.autotrader.core.connectors.exceptions.ConnectionException
import com.fmps.autotrader.shared.dto.*
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.model.ExchangeConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.time.Instant

private val logger = LoggerFactory.getLogger("ConfigurationRoutes")

/**
 * Configure Configuration API routes
 * 
 * All endpoints are under /api/v1/config
 * NOTE: ConfigurationRepository not implemented yet - returning placeholders for v1.0
 */
fun Route.configureConfigurationRoutes() {
    // TODO: Implement ConfigurationRepository in future version
    
    route("/config") {
        
        // ========================================================================
        // GET /api/v1/config - List all configurations
        // ========================================================================
        get {
            logger.info("GET /api/v1/config - List all configurations")
            
            // TODO: Implement with ConfigurationRepository
            call.respond(
                HttpStatusCode.OK,
                ConfigurationListResponse(
                    configurations = emptyList(),
                    total = 0
                )
            )
        }
        
        // ========================================================================
        // GET /api/v1/config/category/{category} - Get configs by category
        // ========================================================================
        get("/category/{category}") {
            val category = call.parameters["category"]
            
            if (category.isNullOrBlank()) {
                logger.warn("GET /api/v1/config/category/{category} - Invalid category")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_CATEGORY",
                            message = "Invalid category parameter"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@get
            }
            
            logger.info("GET /api/v1/config/category/$category - Get configs by category")
            
            // TODO: Implement with ConfigurationRepository
            call.respond(
                HttpStatusCode.OK,
                ConfigurationListResponse(
                    configurations = emptyList(),
                    total = 0
                )
            )
        }
        
        // ========================================================================
        // GET /api/v1/config/{key} - Get config by key
        // ========================================================================
        get("/{key}") {
            val key = call.parameters["key"]
            
            if (key.isNullOrBlank()) {
                logger.warn("GET /api/v1/config/{key} - Invalid key parameter")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_KEY",
                            message = "Invalid configuration key parameter"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@get
            }
            
            logger.info("GET /api/v1/config/$key - Get config by key")
            
            // TODO: Implement with ConfigurationRepository
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    success = false,
                    error = ErrorDetail(
                        code = "CONFIG_NOT_FOUND",
                        message = "Configuration repository not implemented yet"
                    ),
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // PUT /api/v1/config/general - Update general settings
        // ========================================================================
        put("/general") {
            logger.info("PUT /api/v1/config/general - Update general settings")
            
            try {
                val request = call.receive<GeneralSettingsRequest>()
                
                // Validate request
                if (request.updateIntervalSeconds !in 1..600) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(
                            success = false,
                            error = ErrorDetail(
                                code = "INVALID_UPDATE_INTERVAL",
                                message = "Update interval must be between 1-600 seconds"
                            ),
                            timestamp = Instant.now().toString()
                        )
                    )
                    return@put
                }
                
                if (request.telemetryPollingSeconds !in 1..120) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(
                            success = false,
                            error = ErrorDetail(
                                code = "INVALID_TELEMETRY_POLLING",
                                message = "Telemetry polling must be between 1-120 seconds"
                            ),
                            timestamp = Instant.now().toString()
                        )
                    )
                    return@put
                }
                
                val validLoggingLevels = setOf("TRACE", "DEBUG", "INFO", "WARN", "ERROR")
                if (request.loggingLevel.uppercase() !in validLoggingLevels) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(
                            success = false,
                            error = ErrorDetail(
                                code = "INVALID_LOGGING_LEVEL",
                                message = "Logging level must be one of: ${validLoggingLevels.joinToString(", ")}"
                            ),
                            timestamp = Instant.now().toString()
                        )
                    )
                    return@put
                }
                
                // Apply runtime configuration changes
                com.fmps.autotrader.core.config.RuntimeConfigManager.setUpdateIntervalSeconds(request.updateIntervalSeconds)
                com.fmps.autotrader.core.config.RuntimeConfigManager.setTelemetryHeartbeatIntervalSeconds(request.telemetryPollingSeconds)
                com.fmps.autotrader.core.config.RuntimeConfigManager.setLoggingLevel(request.loggingLevel)
                
                // Apply logging level change dynamically
                try {
                    val loggerContext = org.slf4j.LoggerFactory.getILoggerFactory() as? ch.qos.logback.classic.LoggerContext
                    if (loggerContext != null) {
                        val rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
                        val level = ch.qos.logback.classic.Level.toLevel(request.loggingLevel.uppercase())
                        rootLogger.level = level
                        logger.info("Logging level changed to: ${request.loggingLevel.uppercase()}")
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to change logging level dynamically: ${e.message}")
                    // Continue anyway - the value is stored and will be used on next restart
                }
                
                logger.info("General settings updated: updateInterval=${request.updateIntervalSeconds}s, telemetryPolling=${request.telemetryPollingSeconds}s, loggingLevel=${request.loggingLevel}")
                
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = GeneralSettingsResponse(
                            updateIntervalSeconds = request.updateIntervalSeconds,
                            telemetryPollingSeconds = request.telemetryPollingSeconds,
                            loggingLevel = request.loggingLevel.uppercase()
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
            } catch (e: Exception) {
                logger.error("Error processing general settings update request", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INTERNAL_ERROR",
                            message = "Failed to update general settings: ${e.message}"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
            }
        }
        
        // ========================================================================
        // PUT /api/v1/config/trader-defaults - Update trader defaults
        // ========================================================================
        put("/trader-defaults") {
            logger.info("PUT /api/v1/config/trader-defaults - Update trader defaults")
            try {
                val request = call.receive<TraderDefaultsRequest>()
                logger.info("Received trader defaults update request: budgetUsd=${request.budgetUsd}, leverage=${request.leverage}, stopLossPercent=${request.stopLossPercent}, takeProfitPercent=${request.takeProfitPercent}, strategy=${request.strategy}")
                
                // Validation
                if (request.budgetUsd <= 0) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(
                            success = false,
                            error = ErrorDetail(
                                code = "INVALID_BUDGET",
                                message = "Budget must be positive"
                            ),
                            timestamp = Instant.now().toString()
                        )
                    )
                    return@put
                }
                
                if (request.leverage !in 1..10) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(
                            success = false,
                            error = ErrorDetail(
                                code = "INVALID_LEVERAGE",
                                message = "Leverage must be between 1 and 10"
                            ),
                            timestamp = Instant.now().toString()
                        )
                    )
                    return@put
                }
                
                if (request.stopLossPercent !in 0.5..50.0) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(
                            success = false,
                            error = ErrorDetail(
                                code = "INVALID_STOP_LOSS",
                                message = "Stop loss must be between 0.5% and 50%"
                            ),
                            timestamp = Instant.now().toString()
                        )
                    )
                    return@put
                }
                
                if (request.takeProfitPercent !in 0.5..50.0) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(
                            success = false,
                            error = ErrorDetail(
                                code = "INVALID_TAKE_PROFIT",
                                message = "Take profit must be between 0.5% and 50%"
                            ),
                            timestamp = Instant.now().toString()
                        )
                    )
                    return@put
                }
                
                // Validate strategy is a valid TradingStrategy enum value
                try {
                    com.fmps.autotrader.shared.model.TradingStrategy.valueOf(request.strategy)
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(
                            success = false,
                            error = ErrorDetail(
                                code = "INVALID_STRATEGY",
                                message = "Invalid strategy. Must be one of: ${com.fmps.autotrader.shared.model.TradingStrategy.values().joinToString(", ") { it.name }}"
                            ),
                            timestamp = Instant.now().toString()
                        )
                    )
                    return@put
                }
                
                // Update RuntimeConfigManager
                com.fmps.autotrader.core.config.RuntimeConfigManager.setTraderDefaults(
                    com.fmps.autotrader.core.config.TraderDefaults(
                        budgetUsd = request.budgetUsd,
                        leverage = request.leverage,
                        stopLossPercent = request.stopLossPercent,
                        takeProfitPercent = request.takeProfitPercent,
                        strategy = request.strategy
                    )
                )
                
                logger.info("Trader defaults updated and applied successfully.")
                
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = "Trader defaults updated successfully",
                        timestamp = Instant.now().toString()
                    )
                )
            } catch (e: Exception) {
                logger.error("Error updating trader defaults", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INTERNAL_ERROR",
                            message = "Failed to update trader defaults: ${e.message}"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
            }
        }
        
        // ========================================================================
        // PUT /api/v1/config/{key} - Update config value
        // ========================================================================
        put("/{key}") {
            val key = call.parameters["key"]
            
            if (key.isNullOrBlank()) {
                logger.warn("PUT /api/v1/config/{key} - Invalid key parameter")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_KEY",
                            message = "Invalid configuration key parameter"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@put
            }
            
            logger.info("PUT /api/v1/config/$key - Update config value")
            
            // TODO: Implement with ConfigurationRepository
            call.respond(
                HttpStatusCode.NotImplemented,
                ErrorResponse(
                    success = false,
                    error = ErrorDetail(
                        code = "NOT_IMPLEMENTED",
                        message = "Configuration repository not implemented yet"
                    ),
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // POST /api/v1/config/test-connection - Test exchange connection
        // ========================================================================
        post("/test-connection") {
            logger.info("POST /api/v1/config/test-connection - Test exchange connection")
            
            try {
                val request = call.receive<ExchangeConnectionTestRequest>()
                
                // Validate request
                if (request.exchange.isBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(
                            success = false,
                            error = ErrorDetail(
                                code = "INVALID_EXCHANGE",
                                message = "Exchange name is required"
                            ),
                            timestamp = Instant.now().toString()
                        )
                    )
                    return@post
                }
                
                if (request.apiKey.isBlank() || request.secretKey.isBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(
                            success = false,
                            error = ErrorDetail(
                                code = "INVALID_CREDENTIALS",
                                message = "API key and secret are required"
                            ),
                            timestamp = Instant.now().toString()
                        )
                    )
                    return@post
                }
                
                // Parse exchange enum
                val exchange = try {
                    Exchange.valueOf(request.exchange.uppercase())
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(
                            success = false,
                            error = ErrorDetail(
                                code = "INVALID_EXCHANGE",
                                message = "Unsupported exchange: ${request.exchange}"
                            ),
                            timestamp = Instant.now().toString()
                        )
                    )
                    return@post
                }
                
                // Create exchange config
                // Note: For Bitget, the same URL is used for both testnet and production.
                // The API keys themselves determine the environment. We try both environments
                // automatically to detect which one the keys are for.
                // For Binance, we use testnet=true for connection testing.
                val exchangeConfig = ExchangeConfig(
                    exchange = exchange,
                    apiKey = request.apiKey,
                    apiSecret = request.secretKey,
                    passphrase = request.passphrase?.takeIf { it.isNotBlank() },
                    testnet = exchange != com.fmps.autotrader.shared.enums.Exchange.BITGET // Use testnet for Binance, will try both for Bitget
                )
                
                // Test connection using ConnectorFactory
                val connectorFactory = ConnectorFactory.getInstance()
                
                // IMPORTANT: Remove any cached connector for this exchange before testing
                // This ensures we use fresh credentials and avoid state conflicts
                runBlocking {
                    logger.info("Clearing any cached connector for ${exchange.name} before connection test")
                    connectorFactory.removeConnector(exchange)
                    // Small delay to ensure cache cleanup is complete
                    kotlinx.coroutines.delay(50)
                }
                
                var success = false
                var message = ""
                
                runBlocking {
                    if (exchange == com.fmps.autotrader.shared.enums.Exchange.BITGET) {
                        // For Bitget demo trading:
                        // 1. Must use Demo API Key (created in Demo mode)
                        // 2. Must add 'paptrading: 1' header (handled by BitgetAuthenticator when testnet=true)
                        // 3. Uses same REST URL (https://api.bitget.com) but different WebSocket URLs
                        // 
                        // Strategy: Try testnet/demo FIRST (most common for testing), then production if demo fails
                        logger.info("=== BITGET CONNECTION TEST STARTED ===")
                        logger.info("API Key: ${request.apiKey.take(8)}... (length: ${request.apiKey.length})")
                        logger.info("Secret Key: ${request.secretKey.take(8)}... (length: ${request.secretKey.length})")
                        logger.info("Passphrase: ${if (request.passphrase.isNullOrBlank()) "NOT PROVIDED" else "PROVIDED (length: ${request.passphrase.length})"}")
                        
                        var connected = false
                        var lastError: Exception? = null
                        var triedEnvironments = mutableListOf<String>()
                        
                        // Try testnet/demo FIRST (with paptrading header)
                        println("🔍 [Bitget] Trying demo/testnet environment (testnet=true, paptrading: 1)...")
                        logger.info("[Bitget Connection Test] Attempt 1/2: Trying demo/testnet environment (testnet=true, paptrading: 1 header)")
                        triedEnvironments.add("demo/testnet")
                        try {
                            val testnetConfig = exchangeConfig.copy(testnet = true)
                            val testnetConnector = connectorFactory.createConnector(exchange, testnetConfig, useCache = false)
                            // Ensure connector is disconnected before configuring with new credentials
                            if (testnetConnector.isConnected()) {
                                logger.info("Disconnecting existing connector before reconfiguration")
                                testnetConnector.disconnect()
                            }
                            testnetConnector.configure(testnetConfig)
                            println("🔍 [Bitget] Demo/testnet connector configured with paptrading header, attempting connection...")
                            // Small delay to ensure configuration is fully applied
                            kotlinx.coroutines.delay(100)
                            testnetConnector.connect()
                            success = true
                            message = "Successfully connected to ${exchange.name} demo/testnet"
                            testnetConnector.disconnect()
                            connected = true
                            println("✅ [Bitget] Demo/testnet connection successful!")
                        } catch (e: ConnectionException) {
                            lastError = e
                            val errorMsg = e.message ?: "Unknown error"
                            val causeMsg = e.cause?.message ?: "No cause"
                            println("❌ [Bitget] Demo/testnet failed: $errorMsg")
                            logger.error("[Bitget Connection Test] Demo/testnet attempt FAILED", e)
                            logger.error("Error message: $errorMsg")
                            logger.error("Error cause: $causeMsg")
                            logger.error("Exception type: ${e.javaClass.simpleName}")
                            
                            // Check if it's an environment error (40099) - means keys are for production
                            val isEnvironmentError = errorMsg.contains("exchange environment is incorrect") || 
                                                   errorMsg.contains("40099") ||
                                                   errorMsg.contains("exchange environment") ||
                                                   causeMsg.contains("40099") ||
                                                   causeMsg.contains("exchange environment")
                            
                            if (isEnvironmentError) {
                                // Keys are for production, try production (without paptrading header)
                                println("🔍 [Bitget] Environment error (40099) detected - keys appear to be for production. Trying production (testnet=false)...")
                                logger.info("[Bitget Connection Test] Environment error (40099) detected - keys are for production environment")
                                logger.info("[Bitget Connection Test] Attempt 2/2: Trying production environment (testnet=false, no paptrading header)")
                                triedEnvironments.add("production")
                                try {
                                    val productionConfig = exchangeConfig.copy(testnet = false)
                                    val productionConnector = connectorFactory.createConnector(exchange, productionConfig, useCache = false)
                                    // Ensure connector is disconnected before configuring with new credentials
                                    if (productionConnector.isConnected()) {
                                        logger.info("Disconnecting existing connector before reconfiguration")
                                        productionConnector.disconnect()
                                    }
                                    productionConnector.configure(productionConfig)
                                    println("🔍 [Bitget] Production connector configured (no paptrading header), attempting connection...")
                                    // Small delay to ensure configuration is fully applied
                                    kotlinx.coroutines.delay(100)
                                    productionConnector.connect()
                                    success = true
                                    message = "Successfully connected to ${exchange.name} production"
                                    productionConnector.disconnect()
                                    connected = true
                                    println("✅ [Bitget] Production connection successful!")
                                } catch (e2: Exception) {
                                    lastError = e2
                                    val errorMsg2 = e2.message ?: "Unknown error"
                                    val causeMsg2 = e2.cause?.message ?: "No cause"
                                    println("❌ [Bitget] Production also failed: $errorMsg2")
                                    logger.error("[Bitget Connection Test] Production attempt FAILED", e2)
                                    logger.error("Production error message: $errorMsg2")
                                    logger.error("Production error cause: $causeMsg2")
                                    logger.error("Production exception type: ${e2.javaClass.simpleName}")
                                }
                            } else {
                                // Not an environment error - likely invalid keys, wrong passphrase, or other issue
                                println("⚠️ [Bitget] Error is not environment-related (not 40099), not retrying with production")
                                logger.warn("[Bitget Connection Test] Error is not environment-related (not 40099)")
                                logger.warn("This likely indicates: invalid API keys, wrong passphrase, or network issue")
                                logger.warn("Error details: $errorMsg")
                                if (e.cause != null) {
                                    logger.warn("Root cause: ${e.cause?.message}")
                                }
                            }
                        } catch (e: Exception) {
                            lastError = e
                            val errorMsg = e.message ?: "Unknown error"
                            println("❌ [Bitget] Unexpected error during demo/testnet attempt: $errorMsg")
                            logger.error("[Bitget Connection Test] Unexpected error during demo/testnet attempt", e)
                            logger.error("Unexpected error type: ${e.javaClass.simpleName}")
                            logger.error("Unexpected error message: $errorMsg")
                            if (e.cause != null) {
                                logger.error("Unexpected error cause: ${e.cause?.message}")
                            }
                        }
                        
                        if (!connected) {
                            success = false
                            val finalError = lastError?.message ?: "Unknown error"
                            val environmentsTried = triedEnvironments.joinToString(" and ")
                            message = "Connection failed after trying $environmentsTried: $finalError"
                            println("❌ [Bitget] Connection test failed (tried: $environmentsTried): $finalError")
                            logger.error("=== BITGET CONNECTION TEST FAILED ===")
                            logger.error("Environments tried: $environmentsTried")
                            logger.error("Final error: $finalError")
                            if (lastError != null) {
                                logger.error("Exception stack trace:", lastError)
                            }
                        } else {
                            logger.info("=== BITGET CONNECTION TEST SUCCEEDED ===")
                            logger.info("Successfully connected to: ${triedEnvironments.last()}")
                        }
                    } else {
                        // For Binance, just try testnet
                        try {
                            val connector = connectorFactory.createConnector(exchange, exchangeConfig, useCache = false)
                            connector.configure(exchangeConfig)
                            connector.connect()
                            success = true
                            message = "Successfully connected to ${exchange.name} testnet"
                            connector.disconnect()
                        } catch (e: ConnectionException) {
                            success = false
                            message = "Connection failed: ${e.message}"
                            logger.warn("Connection test failed for ${exchange.name}", e)
                        } catch (e: Exception) {
                            success = false
                            message = "Connection test error: ${e.message}"
                            logger.error("Unexpected error during connection test for ${exchange.name}", e)
                        }
                    }
                }
                
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = success,
                        data = ConnectionTestResponse(
                            success = success,
                            message = message
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                
            } catch (e: Exception) {
                logger.error("Error processing connection test request", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INTERNAL_ERROR",
                            message = "Failed to process connection test: ${e.message}"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
            }
        }
    }
}

@kotlinx.serialization.Serializable
private data class GeneralSettingsRequest(
    val updateIntervalSeconds: Int,
    val telemetryPollingSeconds: Int,
    val loggingLevel: String,
    val theme: String? = null // Theme is Desktop UI-only, ignored by core-service
)

@kotlinx.serialization.Serializable
private data class GeneralSettingsResponse(
    val updateIntervalSeconds: Int,
    val telemetryPollingSeconds: Int,
    val loggingLevel: String
)

@kotlinx.serialization.Serializable
private data class TraderDefaultsRequest(
    val budgetUsd: Double,
    val leverage: Int,
    val stopLossPercent: Double,
    val takeProfitPercent: Double,
    val strategy: String
)

@kotlinx.serialization.Serializable
private data class ExchangeConnectionTestRequest(
    val exchange: String,
    val apiKey: String,
    val secretKey: String,
    val passphrase: String? = null
)

@kotlinx.serialization.Serializable
private data class ConnectionTestResponse(
    val success: Boolean,
    val message: String
)

