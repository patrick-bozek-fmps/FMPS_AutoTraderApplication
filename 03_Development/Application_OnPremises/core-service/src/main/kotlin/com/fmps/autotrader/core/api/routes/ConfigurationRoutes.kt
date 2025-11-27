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
                        var connected = false
                        var lastError: Exception? = null
                        var triedEnvironments = mutableListOf<String>()
                        
                        // Try testnet/demo FIRST (with paptrading header)
                        println("🔍 [Bitget] Trying demo/testnet environment (testnet=true, paptrading: 1)...")
                        logger.info("Trying Bitget with demo/testnet environment (paptrading header)...")
                        triedEnvironments.add("demo/testnet")
                        try {
                            val testnetConfig = exchangeConfig.copy(testnet = true)
                            val testnetConnector = connectorFactory.createConnector(exchange, testnetConfig, useCache = false)
                            testnetConnector.configure(testnetConfig)
                            println("🔍 [Bitget] Demo/testnet connector configured with paptrading header, attempting connection...")
                            testnetConnector.connect()
                            success = true
                            message = "Successfully connected to ${exchange.name} demo/testnet"
                            testnetConnector.disconnect()
                            connected = true
                            println("✅ [Bitget] Demo/testnet connection successful!")
                        } catch (e: ConnectionException) {
                            lastError = e
                            val errorMsg = e.message ?: "Unknown error"
                            println("❌ [Bitget] Demo/testnet failed: $errorMsg")
                            logger.info("Bitget demo/testnet failed: $errorMsg")
                            
                            // Check if it's an environment error (40099) - means keys are for production
                            val isEnvironmentError = errorMsg.contains("exchange environment is incorrect") || 
                                                   errorMsg.contains("40099") ||
                                                   errorMsg.contains("exchange environment")
                            
                            if (isEnvironmentError) {
                                // Keys are for production, try production (without paptrading header)
                                println("🔍 [Bitget] Environment error (40099) detected - keys appear to be for production. Trying production (testnet=false)...")
                                logger.info("Bitget environment error detected, trying production environment...")
                                triedEnvironments.add("production")
                                try {
                                    val productionConfig = exchangeConfig.copy(testnet = false)
                                    val productionConnector = connectorFactory.createConnector(exchange, productionConfig, useCache = false)
                                    productionConnector.configure(productionConfig)
                                    println("🔍 [Bitget] Production connector configured (no paptrading header), attempting connection...")
                                    productionConnector.connect()
                                    success = true
                                    message = "Successfully connected to ${exchange.name} production"
                                    productionConnector.disconnect()
                                    connected = true
                                    println("✅ [Bitget] Production connection successful!")
                                } catch (e2: Exception) {
                                    lastError = e2
                                    val errorMsg2 = e2.message ?: "Unknown error"
                                    println("❌ [Bitget] Production also failed: $errorMsg2")
                                    logger.warn("Bitget production also failed: $errorMsg2")
                                }
                            } else {
                                // Not an environment error - likely invalid keys, wrong passphrase, or other issue
                                println("⚠️ [Bitget] Error is not environment-related (not 40099), not retrying with production")
                                logger.info("Bitget error is not environment-related: $errorMsg")
                            }
                        } catch (e: Exception) {
                            lastError = e
                            println("❌ [Bitget] Unexpected error during demo/testnet attempt: ${e.message}")
                            logger.error("Unexpected error during Bitget demo/testnet connection test", e)
                        }
                        
                        if (!connected) {
                            success = false
                            val finalError = lastError?.message ?: "Unknown error"
                            val environmentsTried = triedEnvironments.joinToString(" and ")
                            message = "Connection failed after trying $environmentsTried: $finalError"
                            println("❌ [Bitget] Connection test failed (tried: $environmentsTried): $finalError")
                            logger.warn("Connection test failed for Bitget (tried: $environmentsTried)")
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

