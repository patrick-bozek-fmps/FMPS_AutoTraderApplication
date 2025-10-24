package com.fmps.autotrader.core.api.routes

import com.fmps.autotrader.core.database.repositories.PatternRepository
import com.fmps.autotrader.shared.dto.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Instant

private val logger = LoggerFactory.getLogger("PatternRoutes")

/**
 * Configure Pattern API routes
 * 
 * All endpoints are under /api/v1/patterns
 */
fun Route.configurePatternRoutes() {
    val patternRepository = PatternRepository()
    
    route("/patterns") {
        
        // ========================================================================
        // GET /api/v1/patterns - List all patterns
        // ========================================================================
        get {
            logger.info("GET /api/v1/patterns - List all patterns")
            
            val activeOnly = call.request.queryParameters["activeOnly"]?.toBoolean() ?: false
            
            val patterns = if (activeOnly) {
                patternRepository.findActive()
            } else {
                patternRepository.findActive() // Only active for v1.0
            }
            
            // Convert to DTOs (simplified for now - PatternDTO exists but entity mapping needed)
            logger.info("Found ${patterns.size} patterns")
            
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = emptyList<PatternDTO>(), // TODO: Add mapper
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // GET /api/v1/patterns/active - List active patterns
        // ========================================================================
        get("/active") {
            logger.info("GET /api/v1/patterns/active - List active patterns")
            
            val patterns = patternRepository.findActive()
            
            logger.info("Found ${patterns.size} active patterns")
            
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = emptyList<PatternDTO>(), // TODO: Add mapper
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // GET /api/v1/patterns/top - Get top performing patterns
        // ========================================================================
        get("/top") {
            logger.info("GET /api/v1/patterns/top - Get top performing patterns")
            
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            val patterns = patternRepository.getTopPatterns(limit)
            
            logger.info("Found ${patterns.size} top patterns")
            
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = emptyList<PatternDTO>(), // TODO: Add mapper
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // GET /api/v1/patterns/{id} - Get pattern by ID
        // ========================================================================
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            
            if (id == null) {
                logger.warn("GET /api/v1/patterns/{id} - Invalid ID parameter")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_ID",
                            message = "Invalid pattern ID parameter"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@get
            }
            
            logger.info("GET /api/v1/patterns/$id - Get pattern by ID")
            
            val pattern = patternRepository.findById(id)
            
            if (pattern == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "PATTERN_NOT_FOUND",
                            message = "Pattern with ID $id not found"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.OK,
                    MessageResponse(
                        success = true,
                        message = "Pattern found (mapper TODO)",
                        timestamp = Instant.now().toString()
                    )
                )
            }
        }
        
        // ========================================================================
        // POST /api/v1/patterns - Create new pattern
        // ========================================================================
        post {
            logger.info("POST /api/v1/patterns - Create new pattern")
            
            // Parse request body
            val request = try {
                call.receive<CreatePatternRequest>()
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
            val validation = validateCreatePatternRequest(request)
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
            
            // Create pattern (simplified - full implementation would parse indicators JSON)
            val patternId = patternRepository.create(
                name = request.name,
                patternType = "CUSTOM",
                tradingPair = request.tradingPair,
                timeframe = "1h",
                tradeType = "BOTH",
                description = request.description
            )
            
            logger.info("Created pattern with ID: $patternId")
            call.respond(
                HttpStatusCode.Created,
                MessageResponse(
                    success = true,
                    message = "Pattern created with ID: $patternId",
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // PATCH /api/v1/patterns/{id}/statistics - Update pattern statistics
        // ========================================================================
        patch("/{id}/statistics") {
            val id = call.parameters["id"]?.toIntOrNull()
            
            if (id == null) {
                logger.warn("PATCH /api/v1/patterns/{id}/statistics - Invalid ID")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_ID",
                            message = "Invalid pattern ID parameter"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            logger.info("PATCH /api/v1/patterns/$id/statistics - Update statistics")
            
            // Check if pattern exists
            val existingPattern = patternRepository.findById(id)
            if (existingPattern == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "PATTERN_NOT_FOUND",
                            message = "Pattern with ID $id not found"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            // Parse request body
            val request = try {
                call.receive<UpdatePatternStatisticsRequest>()
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
            
            // Update statistics
            val updated = patternRepository.updateStatistics(
                patternId = id,
                profitLoss = request.profitLoss,
                isSuccessful = request.wasWinningTrade
            )
            
            if (!updated) {
                logger.error("Failed to update pattern $id statistics")
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "UPDATE_FAILED",
                            message = "Failed to update pattern statistics"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            logger.info("Updated pattern $id statistics")
            call.respond(
                HttpStatusCode.OK,
                MessageResponse(
                    success = true,
                    message = "Pattern statistics updated successfully",
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // PATCH /api/v1/patterns/{id}/activate - Activate pattern
        // ========================================================================
        patch("/{id}/activate") {
            val id = call.parameters["id"]?.toIntOrNull()
            
            if (id == null) {
                logger.warn("PATCH /api/v1/patterns/{id}/activate - Invalid ID")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_ID",
                            message = "Invalid pattern ID parameter"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            logger.info("PATCH /api/v1/patterns/$id/activate - Activate pattern")
            
            val updated = patternRepository.activate(id)
            
            if (!updated) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "PATTERN_NOT_FOUND",
                            message = "Pattern with ID $id not found"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            logger.info("Activated pattern $id")
            call.respond(
                HttpStatusCode.OK,
                MessageResponse(
                    success = true,
                    message = "Pattern activated successfully",
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // PATCH /api/v1/patterns/{id}/deactivate - Deactivate pattern
        // ========================================================================
        patch("/{id}/deactivate") {
            val id = call.parameters["id"]?.toIntOrNull()
            
            if (id == null) {
                logger.warn("PATCH /api/v1/patterns/{id}/deactivate - Invalid ID")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_ID",
                            message = "Invalid pattern ID parameter"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            logger.info("PATCH /api/v1/patterns/$id/deactivate - Deactivate pattern")
            
            val updated = patternRepository.deactivate(id)
            
            if (!updated) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "PATTERN_NOT_FOUND",
                            message = "Pattern with ID $id not found"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            logger.info("Deactivated pattern $id")
            call.respond(
                HttpStatusCode.OK,
                MessageResponse(
                    success = true,
                    message = "Pattern deactivated successfully",
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // DELETE /api/v1/patterns/{id} - Delete pattern
        // ========================================================================
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            
            if (id == null) {
                logger.warn("DELETE /api/v1/patterns/{id} - Invalid ID")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_ID",
                            message = "Invalid pattern ID parameter"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@delete
            }
            
            logger.info("DELETE /api/v1/patterns/$id - Delete pattern")
            
            val deleted = patternRepository.delete(id)
            
            if (!deleted) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "PATTERN_NOT_FOUND",
                            message = "Pattern with ID $id not found"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@delete
            }
            
            logger.info("Deleted pattern $id")
            call.respond(
                HttpStatusCode.OK,
                MessageResponse(
                    success = true,
                    message = "Pattern with ID $id successfully deleted",
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // GET /api/v1/patterns/match - Find matching patterns (placeholder)
        // ========================================================================
        get("/match") {
            logger.info("GET /api/v1/patterns/match - Find matching patterns")
            
            // Query parameters for matching
            val exchange = call.request.queryParameters["exchange"]
            val tradingPair = call.request.queryParameters["tradingPair"]
            val minSuccessRate = call.request.queryParameters["minSuccessRate"]?.toBigDecimalOrNull()
            
            logger.info("Match criteria: exchange=$exchange, pair=$tradingPair, minSuccessRate=$minSuccessRate")
            
            // For v1.0, return empty list (pattern matching to be implemented in future)
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = emptyList<PatternDTO>(),
                    timestamp = Instant.now().toString()
                )
            )
        }
    }
}

/**
 * Validate CreatePatternRequest
 * Returns ErrorDetail if validation fails, null if valid
 */
private fun validateCreatePatternRequest(request: CreatePatternRequest): ErrorDetail? {
    // Validate name
    if (request.name.isBlank()) {
        return ErrorDetail(
            code = "INVALID_NAME",
            message = "Pattern name cannot be empty"
        )
    }
    
    if (request.name.length > 100) {
        return ErrorDetail(
            code = "INVALID_NAME",
            message = "Pattern name must be 100 characters or less"
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
    
    // Validate trading pair
    if (request.tradingPair.isBlank()) {
        return ErrorDetail(
            code = "INVALID_TRADING_PAIR",
            message = "Trading pair cannot be empty"
        )
    }
    
    return null
}

