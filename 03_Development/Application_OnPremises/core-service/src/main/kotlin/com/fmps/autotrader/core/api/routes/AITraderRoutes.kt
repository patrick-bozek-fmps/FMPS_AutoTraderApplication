package com.fmps.autotrader.core.api.routes

import com.fmps.autotrader.core.api.mappers.toAITraderDTOs
import com.fmps.autotrader.core.api.mappers.toDTO
import com.fmps.autotrader.core.database.repositories.AITraderRepository
import com.fmps.autotrader.shared.dto.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.time.Instant

private val logger = LoggerFactory.getLogger("AITraderRoutes")

/**
 * Configure AI Trader API routes
 * 
 * All endpoints are under /api/v1/traders
 */
fun Route.configureAITraderRoutes() {
    val aiTraderRepository = AITraderRepository()
    
    route("/traders") {
        
        // ========================================================================
        // GET /api/v1/traders - List all AI traders
        // ========================================================================
        get {
            logger.info("GET /api/v1/traders - List all AI traders")
            
            val traders = aiTraderRepository.findAll()
            val tradersDTO = traders.toAITraderDTOs()
            
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = tradersDTO,
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // GET /api/v1/traders/active - List active AI traders
        // ========================================================================
        get("/active") {
            logger.info("GET /api/v1/traders/active - List active AI traders")
            
            val activeTraders = aiTraderRepository.findActive()
            val tradersDTO = activeTraders.toAITraderDTOs()
            
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = tradersDTO,
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // GET /api/v1/traders/can-create - Check if can create more traders
        // ========================================================================
        get("/can-create") {
            logger.info("GET /api/v1/traders/can-create - Check if can create more traders")
            
            val canCreate = aiTraderRepository.canCreateMore()
            val currentCount = aiTraderRepository.count()
            val maxAllowed = 3L
            
            val response = CanCreateTraderResponse(
                canCreate = canCreate,
                currentCount = currentCount,
                maxAllowed = maxAllowed,
                reason = if (!canCreate) "Maximum limit of $maxAllowed AI traders reached" else null
            )
            
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = response,
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // GET /api/v1/traders/{id} - Get trader by ID
        // ========================================================================
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            
            if (id == null) {
                logger.warn("GET /api/v1/traders/{id} - Invalid ID parameter")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_ID",
                            message = "Invalid trader ID parameter"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@get
            }
            
            logger.info("GET /api/v1/traders/$id - Get trader by ID")
            
            val trader = aiTraderRepository.findById(id)
            
            if (trader == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "TRADER_NOT_FOUND",
                            message = "AI Trader with ID $id not found"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = trader.toDTO(),
                        timestamp = Instant.now().toString()
                    )
                )
            }
        }
        
        // ========================================================================
        // POST /api/v1/traders - Create new AI trader
        // ========================================================================
        post {
            logger.info("POST /api/v1/traders - Create new AI trader")
            
            // Check if we can create more traders
            val canCreate = aiTraderRepository.canCreateMore()
            if (!canCreate) {
                logger.warn("Cannot create AI trader: maximum limit (3) reached")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "MAX_TRADERS_REACHED",
                            message = "Maximum of 3 AI traders already created",
                            details = mapOf(
                                "currentCount" to aiTraderRepository.count().toString(),
                                "maxAllowed" to "3"
                            )
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@post
            }
            
            // Parse request body
            val request = try {
                call.receive<CreateAITraderRequest>()
            } catch (e: Exception) {
                logger.error("Failed to parse request body", e)
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_REQUEST",
                            message = "Invalid request body: ${e.message}"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@post
            }
            
            // Validate request
            val validation = validateCreateTraderRequest(request)
            if (validation != null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = validation,
                        timestamp = Instant.now().toString()
                    )
                )
                return@post
            }
            
            // Create trader
            val traderId = aiTraderRepository.create(
                name = request.name,
                exchange = request.exchange,
                tradingPair = request.tradingPair,
                leverage = request.leverage,
                initialBalance = request.initialBalance,
                stopLossPercent = request.stopLossPercentage,
                takeProfitPercent = request.takeProfitPercentage
            )
            
            if (traderId == null) {
                logger.error("Failed to create AI trader")
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "CREATION_FAILED",
                            message = "Failed to create AI trader"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@post
            }
            
            // Fetch the created trader
            val createdTrader = aiTraderRepository.findById(traderId)!!
            
            logger.info("Created AI trader with ID: $traderId")
            call.respond(
                HttpStatusCode.Created,
                ApiResponse(
                    success = true,
                    data = createdTrader.toDTO(),
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // PUT /api/v1/traders/{id} - Update trader configuration
        // ========================================================================
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            
            if (id == null) {
                logger.warn("PUT /api/v1/traders/{id} - Invalid ID parameter")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_ID",
                            message = "Invalid trader ID parameter"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@put
            }
            
            logger.info("PUT /api/v1/traders/$id - Update trader configuration")
            
            // Check if trader exists
            val existingTrader = aiTraderRepository.findById(id)
            if (existingTrader == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "TRADER_NOT_FOUND",
                            message = "AI Trader with ID $id not found"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@put
            }
            
            // Parse request body
            val request = try {
                call.receive<UpdateAITraderRequest>()
            } catch (e: Exception) {
                logger.error("Failed to parse request body", e)
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_REQUEST",
                            message = "Invalid request body: ${e.message}"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@put
            }
            
            // Update trader configuration
            val updated = aiTraderRepository.updateConfiguration(
                id = id,
                leverage = request.leverage,
                stopLossPercent = request.stopLossPercentage,
                takeProfitPercent = request.takeProfitPercentage
            )
            
            if (!updated) {
                logger.error("Failed to update AI trader $id")
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "UPDATE_FAILED",
                            message = "Failed to update AI trader"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@put
            }
            
            // Fetch updated trader
            val updatedTrader = aiTraderRepository.findById(id)!!
            
            logger.info("Updated AI trader $id")
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = updatedTrader.toDTO(),
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // PATCH /api/v1/traders/{id}/status - Update trader status
        // ========================================================================
        patch("/{id}/status") {
            val id = call.parameters["id"]?.toIntOrNull()
            
            if (id == null) {
                logger.warn("PATCH /api/v1/traders/{id}/status - Invalid ID parameter")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_ID",
                            message = "Invalid trader ID parameter"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            logger.info("PATCH /api/v1/traders/$id/status - Update trader status")
            
            // Check if trader exists
            val existingTrader = aiTraderRepository.findById(id)
            if (existingTrader == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "TRADER_NOT_FOUND",
                            message = "AI Trader with ID $id not found"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            // Parse request body
            val request = try {
                call.receive<UpdateTraderStatusRequest>()
            } catch (e: Exception) {
                logger.error("Failed to parse request body", e)
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_REQUEST",
                            message = "Invalid request body: ${e.message}"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            // Validate status
            if (request.status !in listOf("ACTIVE", "PAUSED", "STOPPED")) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_STATUS",
                            message = "Invalid status. Must be one of: ACTIVE, PAUSED, STOPPED",
                            details = mapOf("providedStatus" to request.status)
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            // Update status
            val updated = aiTraderRepository.updateStatus(id, request.status)
            
            if (!updated) {
                logger.error("Failed to update AI trader $id status")
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "UPDATE_FAILED",
                            message = "Failed to update AI trader status"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            // Fetch updated trader
            val updatedTrader = aiTraderRepository.findById(id)!!
            
            logger.info("Updated AI trader $id status to ${request.status}")
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = updatedTrader.toDTO(),
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // PATCH /api/v1/traders/{id}/balance - Update trader balance
        // ========================================================================
        patch("/{id}/balance") {
            val id = call.parameters["id"]?.toIntOrNull()
            
            if (id == null) {
                logger.warn("PATCH /api/v1/traders/{id}/balance - Invalid ID parameter")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_ID",
                            message = "Invalid trader ID parameter"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            logger.info("PATCH /api/v1/traders/$id/balance - Update trader balance")
            
            // Check if trader exists
            val existingTrader = aiTraderRepository.findById(id)
            if (existingTrader == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "TRADER_NOT_FOUND",
                            message = "AI Trader with ID $id not found"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            // Parse request body
            val request = try {
                call.receive<UpdateTraderBalanceRequest>()
            } catch (e: Exception) {
                logger.error("Failed to parse request body", e)
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_REQUEST",
                            message = "Invalid request body: ${e.message}"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            // Validate balance
            if (request.balance <= java.math.BigDecimal.ZERO) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_BALANCE",
                            message = "Balance must be greater than zero"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            // Update balance
            val updated = aiTraderRepository.updateBalance(id, request.balance)
            
            if (!updated) {
                logger.error("Failed to update AI trader $id balance")
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "UPDATE_FAILED",
                            message = "Failed to update AI trader balance"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            // Fetch updated trader
            val updatedTrader = aiTraderRepository.findById(id)!!
            
            logger.info("Updated AI trader $id balance to ${request.balance}")
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = updatedTrader.toDTO(),
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // DELETE /api/v1/traders/{id} - Delete trader
        // ========================================================================
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            
            if (id == null) {
                logger.warn("DELETE /api/v1/traders/{id} - Invalid ID parameter")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_ID",
                            message = "Invalid trader ID parameter"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@delete
            }
            
            logger.info("DELETE /api/v1/traders/$id - Delete trader")
            
            // Check if trader exists
            val existingTrader = aiTraderRepository.findById(id)
            if (existingTrader == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "TRADER_NOT_FOUND",
                            message = "AI Trader with ID $id not found"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@delete
            }
            
            // Don't allow deletion of active traders
            if (existingTrader.status == "ACTIVE") {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "TRADER_ACTIVE",
                            message = "Cannot delete active AI trader. Stop the trader first.",
                            details = mapOf("currentStatus" to existingTrader.status)
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@delete
            }
            
            // Delete trader
            val deleted = aiTraderRepository.delete(id)
            
            if (!deleted) {
                logger.error("Failed to delete AI trader $id")
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "DELETE_FAILED",
                            message = "Failed to delete AI trader"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@delete
            }
            
            logger.info("Deleted AI trader $id")
            call.respond(
                HttpStatusCode.OK,
                MessageResponse(
                    success = true,
                    message = "AI Trader with ID $id successfully deleted",
                    timestamp = Instant.now().toString()
                )
            )
        }
    }
}

/**
 * Validate CreateAITraderRequest
 * Returns ErrorDetail if validation fails, null if valid
 */
private fun validateCreateTraderRequest(request: CreateAITraderRequest): ErrorDetail? {
    // Validate name
    if (request.name.isBlank()) {
        return ErrorDetail(
            code = "INVALID_NAME",
            message = "Trader name cannot be empty"
        )
    }
    
    if (request.name.length > 100) {
        return ErrorDetail(
            code = "INVALID_NAME",
            message = "Trader name must be 100 characters or less"
        )
    }
    
    // Validate exchange
    if (request.exchange !in listOf("BINANCE", "BITGET", "TRADINGVIEW")) {
        return ErrorDetail(
            code = "INVALID_EXCHANGE",
            message = "Invalid exchange. Must be one of: BINANCE, BITGET, TRADINGVIEW",
            details = mapOf("providedExchange" to request.exchange)
        )
    }
    
    // Validate leverage
    if (request.leverage < 1 || request.leverage > 125) {
        return ErrorDetail(
            code = "INVALID_LEVERAGE",
            message = "Leverage must be between 1 and 125",
            details = mapOf("providedLeverage" to request.leverage.toString())
        )
    }
    
    // Validate initial balance
    if (request.initialBalance <= java.math.BigDecimal.ZERO) {
        return ErrorDetail(
            code = "INVALID_BALANCE",
            message = "Initial balance must be greater than zero"
        )
    }
    
    // Validate stop loss percentage
    if (request.stopLossPercentage <= java.math.BigDecimal.ZERO || 
        request.stopLossPercentage >= java.math.BigDecimal.ONE) {
        return ErrorDetail(
            code = "INVALID_STOP_LOSS",
            message = "Stop loss percentage must be between 0 and 1 (e.g., 0.02 for 2%)",
            details = mapOf("providedValue" to request.stopLossPercentage.toString())
        )
    }
    
    // Validate take profit percentage
    if (request.takeProfitPercentage <= java.math.BigDecimal.ZERO || 
        request.takeProfitPercentage >= java.math.BigDecimal.ONE) {
        return ErrorDetail(
            code = "INVALID_TAKE_PROFIT",
            message = "Take profit percentage must be between 0 and 1 (e.g., 0.05 for 5%)",
            details = mapOf("providedValue" to request.takeProfitPercentage.toString())
        )
    }
    
    return null
}

