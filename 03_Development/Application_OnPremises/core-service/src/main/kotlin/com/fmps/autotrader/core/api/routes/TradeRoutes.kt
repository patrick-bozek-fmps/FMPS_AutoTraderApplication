package com.fmps.autotrader.core.api.routes

import com.fmps.autotrader.core.api.mappers.toDTO
import com.fmps.autotrader.core.api.mappers.toTradeDTOs
import com.fmps.autotrader.core.database.repositories.AITraderRepository
import com.fmps.autotrader.core.database.repositories.TradeRepository
import com.fmps.autotrader.shared.dto.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

private val logger = LoggerFactory.getLogger("TradeRoutes")
private val paginationStatusValues = setOf("OPEN", "CLOSED")
private const val DEFAULT_PAGE_SIZE = 50
private const val MAX_PAGE_SIZE = 200

private suspend fun ApplicationCall.respondApiError(
    status: HttpStatusCode,
    code: String,
    message: String,
    details: Map<String, String>? = null
) {
    respond(
        status,
        ErrorResponse(
            success = false,
            error = ErrorDetail(code = code, message = message, details = details),
            timestamp = Instant.now().toString()
        )
    )
}

/**
 * Configure Trade API routes
 * 
 * All endpoints are under /api/v1/trades
 */
fun Route.configureTradeRoutes() {
    val tradeRepository = TradeRepository()
    val aiTraderRepository = AITraderRepository()
    
    route("/trades") {
        
        // ========================================================================
        // GET /api/v1/trades - List all trades (with optional filtering)
        // ========================================================================
        get {
            logger.info("GET /api/v1/trades - List trades with pagination")

            val queryParameters = call.request.queryParameters

            val page = queryParameters["page"]?.toIntOrNull() ?: 1
            if (page < 1) {
                call.respondApiError(
                    status = HttpStatusCode.BadRequest,
                    code = "INVALID_PAGE",
                    message = "Query parameter 'page' must be a positive integer.",
                    details = mapOf("page" to queryParameters["page"].orEmpty())
                )
                return@get
            }

            val legacyLimit = queryParameters["limit"]?.toIntOrNull()
            val requestedPageSize = queryParameters["pageSize"]?.toIntOrNull() ?: legacyLimit ?: DEFAULT_PAGE_SIZE
            if (requestedPageSize < 1 || requestedPageSize > MAX_PAGE_SIZE) {
                call.respondApiError(
                    status = HttpStatusCode.BadRequest,
                    code = "INVALID_PAGE_SIZE",
                    message = "Query parameter 'pageSize' must be between 1 and $MAX_PAGE_SIZE.",
                    details = mapOf("pageSize" to queryParameters["pageSize"].orEmpty())
                )
                return@get
            }

            val statusParam = queryParameters["status"]?.uppercase()
            val normalizedStatus = when {
                statusParam == null -> null
                statusParam in paginationStatusValues -> statusParam
                else -> {
                    call.respondApiError(
                        status = HttpStatusCode.BadRequest,
                        code = "INVALID_STATUS",
                        message = "Query parameter 'status' must be one of: ${paginationStatusValues.joinToString()}.",
                        details = mapOf("status" to statusParam)
                    )
                    return@get
                }
            }

            val aiTraderIdParam = queryParameters["aiTraderId"]
            val aiTraderId = when {
                aiTraderIdParam.isNullOrBlank() -> null
                aiTraderIdParam.toIntOrNull() != null -> aiTraderIdParam.toInt()
                else -> {
                    call.respondApiError(
                        status = HttpStatusCode.BadRequest,
                        code = "INVALID_TRADER_ID",
                        message = "Query parameter 'aiTraderId' must be a numeric identifier.",
                        details = mapOf("aiTraderId" to aiTraderIdParam)
                    )
                    return@get
                }
            }

            logger.info(
                "Listing trades with filters page=$page pageSize=$requestedPageSize status=$normalizedStatus aiTraderId=$aiTraderId"
            )

            val pagedResult = tradeRepository.findPaged(
                page = page,
                pageSize = requestedPageSize,
                status = normalizedStatus,
                aiTraderId = aiTraderId
            )

            val tradesDTO = pagedResult.items.toTradeDTOs()
            val paginationInfo = PaginationInfo.from(page, requestedPageSize, pagedResult.total)

            call.respond(
                HttpStatusCode.OK,
                PaginatedResponse(
                    success = true,
                    data = tradesDTO,
                    pagination = paginationInfo,
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // GET /api/v1/trades/open - List open trades
        // ========================================================================
        get("/open") {
            logger.info("GET /api/v1/trades/open - List open trades")
            
            val openTrades = tradeRepository.findAllOpenTrades()
            val tradesDTO = openTrades.toTradeDTOs()
            
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = tradesDTO,
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // GET /api/v1/trades/trader/{traderId} - Get trades by AI trader
        // ========================================================================
        get("/trader/{traderId}") {
            val traderId = call.parameters["traderId"]?.toIntOrNull()
            
            if (traderId == null) {
                logger.warn("GET /api/v1/trades/trader/{traderId} - Invalid trader ID")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_TRADER_ID",
                            message = "Invalid trader ID parameter"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@get
            }
            
            logger.info("GET /api/v1/trades/trader/$traderId - Get trades by AI trader")
            
            // Check if trader exists
            val trader = aiTraderRepository.findById(traderId)
            if (trader == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "TRADER_NOT_FOUND",
                            message = "AI Trader with ID $traderId not found"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@get
            }
            
            val trades = tradeRepository.findByAITrader(traderId)
            val tradesDTO = trades.toTradeDTOs()
            
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = tradesDTO,
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // GET /api/v1/trades/statistics/{traderId} - Get trade statistics
        // ========================================================================
        get("/statistics/{traderId}") {
            val traderId = call.parameters["traderId"]?.toIntOrNull()
            
            if (traderId == null) {
                logger.warn("GET /api/v1/trades/statistics/{traderId} - Invalid trader ID")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_TRADER_ID",
                            message = "Invalid trader ID parameter"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@get
            }
            
            logger.info("GET /api/v1/trades/statistics/$traderId - Get trade statistics")
            
            // Check if trader exists
            val trader = aiTraderRepository.findById(traderId)
            if (trader == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "TRADER_NOT_FOUND",
                            message = "AI Trader with ID $traderId not found"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@get
            }
            
            val statistics = tradeRepository.getStatistics(traderId)
            val statisticsDTO = statistics.toDTO(traderId.toLong())
            
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = statisticsDTO,
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // GET /api/v1/trades/{id} - Get trade by ID
        // ========================================================================
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            
            if (id == null) {
                logger.warn("GET /api/v1/trades/{id} - Invalid ID parameter")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_ID",
                            message = "Invalid trade ID parameter"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@get
            }
            
            logger.info("GET /api/v1/trades/$id - Get trade by ID")
            
            val trade = tradeRepository.findById(id)
            
            if (trade == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "TRADE_NOT_FOUND",
                            message = "Trade with ID $id not found"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = trade.toDTO(),
                        timestamp = Instant.now().toString()
                    )
                )
            }
        }
        
        // ========================================================================
        // POST /api/v1/trades - Create new trade (entry)
        // ========================================================================
        post {
            logger.info("POST /api/v1/trades - Create new trade")
            
            // Parse request body
            val request = try {
                call.receive<CreateTradeRequest>()
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
            val validation = validateCreateTradeRequest(request, aiTraderRepository)
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
            
            // Get trader information
            val trader = aiTraderRepository.findById(request.aiTraderId.toInt())!!
            
            // Create trade
            val tradeId = tradeRepository.create(
                aiTraderId = request.aiTraderId.toInt(),
                tradeType = request.side,
                exchange = trader.exchange,
                tradingPair = trader.tradingPair,
                leverage = trader.leverage,
                entryPrice = request.entryPrice,
                entryAmount = request.quantity,
                stopLossPrice = request.stopLoss,
                takeProfitPrice = request.takeProfit,
                patternId = request.patternId?.toInt()
            )
            
            if (tradeId == null) {
                logger.error("Failed to create trade")
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "CREATION_FAILED",
                            message = "Failed to create trade"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@post
            }
            
            // Fetch the created trade
            val createdTrade = tradeRepository.findById(tradeId)!!
            
            logger.info("Created trade with ID: $tradeId")
            call.respond(
                HttpStatusCode.Created,
                ApiResponse(
                    success = true,
                    data = createdTrade.toDTO(),
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // PATCH /api/v1/trades/{id}/close - Close trade (exit)
        // ========================================================================
        patch("/{id}/close") {
            val id = call.parameters["id"]?.toIntOrNull()
            
            if (id == null) {
                logger.warn("PATCH /api/v1/trades/{id}/close - Invalid ID parameter")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_ID",
                            message = "Invalid trade ID parameter"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            logger.info("PATCH /api/v1/trades/$id/close - Close trade")
            
            // Check if trade exists
            val existingTrade = tradeRepository.findById(id)
            if (existingTrade == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "TRADE_NOT_FOUND",
                            message = "Trade with ID $id not found"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            // Check if trade is already closed
            if (existingTrade.status != "OPEN") {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "TRADE_NOT_OPEN",
                            message = "Trade is not open. Current status: ${existingTrade.status}"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            // Parse request body
            val request = try {
                call.receive<CloseTradeRequest>()
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
            
            // Validate exit price
            if (request.exitPrice <= BigDecimal.ZERO) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_EXIT_PRICE",
                            message = "Exit price must be greater than zero"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            // Close trade
            val closed = tradeRepository.close(
                tradeId = id,
                exitPrice = request.exitPrice,
                exitAmount = existingTrade.entryAmount,
                exitReason = request.exitReason ?: "MANUAL"
            )
            
            if (!closed) {
                logger.error("Failed to close trade $id")
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "CLOSE_FAILED",
                            message = "Failed to close trade"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            // Fetch closed trade
            val closedTrade = tradeRepository.findById(id)!!
            
            logger.info("Closed trade $id at price ${request.exitPrice}")
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = closedTrade.toDTO(),
                    timestamp = Instant.now().toString()
                )
            )
        }
        
        // ========================================================================
        // PATCH /api/v1/trades/{id}/stop-loss - Update stop-loss
        // ========================================================================
        patch("/{id}/stop-loss") {
            val id = call.parameters["id"]?.toIntOrNull()
            
            if (id == null) {
                logger.warn("PATCH /api/v1/trades/{id}/stop-loss - Invalid ID parameter")
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_ID",
                            message = "Invalid trade ID parameter"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            logger.info("PATCH /api/v1/trades/$id/stop-loss - Update stop-loss")
            
            // Check if trade exists
            val existingTrade = tradeRepository.findById(id)
            if (existingTrade == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "TRADE_NOT_FOUND",
                            message = "Trade with ID $id not found"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            // Check if trade is open
            if (existingTrade.status != "OPEN") {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "TRADE_NOT_OPEN",
                            message = "Cannot update stop-loss for non-open trade. Current status: ${existingTrade.status}"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            // Parse request body
            val request = try {
                call.receive<UpdateStopLossRequest>()
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
            
            // Validate stop-loss price
            if (request.stopLoss <= BigDecimal.ZERO) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "INVALID_STOP_LOSS",
                            message = "Stop-loss price must be greater than zero"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            // Update stop-loss
            val updated = tradeRepository.updateStopLoss(id, request.stopLoss, request.trailingActivated)
            
            if (!updated) {
                logger.error("Failed to update stop-loss for trade $id")
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        success = false,
                        error = ErrorDetail(
                            code = "UPDATE_FAILED",
                            message = "Failed to update stop-loss"
                        ),
                        timestamp = Instant.now().toString()
                    )
                )
                return@patch
            }
            
            // Fetch updated trade
            val updatedTrade = tradeRepository.findById(id)!!
            
            if (request.trailingActivated) {
                logger.info("Updated stop-loss for trade $id to ${request.stopLoss} (trailing enabled)")
            } else {
                logger.info("Updated stop-loss for trade $id to ${request.stopLoss}")
            }
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = updatedTrade.toDTO(),
                    timestamp = Instant.now().toString()
                )
            )
        }
    }
}

/**
 * Validate CreateTradeRequest
 * Returns ErrorDetail if validation fails, null if valid
 */
private suspend fun validateCreateTradeRequest(
    request: CreateTradeRequest,
    aiTraderRepository: AITraderRepository
): ErrorDetail? {
    // Validate AI Trader exists
    val trader = aiTraderRepository.findById(request.aiTraderId.toInt())
    if (trader == null) {
        return ErrorDetail(
            code = "TRADER_NOT_FOUND",
            message = "AI Trader with ID ${request.aiTraderId} not found"
        )
    }
    
    // Validate trader is active
    if (trader.status != "ACTIVE") {
        return ErrorDetail(
            code = "TRADER_NOT_ACTIVE",
            message = "AI Trader is not active. Current status: ${trader.status}"
        )
    }
    
    // Validate side
    if (request.side !in listOf("LONG", "SHORT")) {
        return ErrorDetail(
            code = "INVALID_SIDE",
            message = "Invalid trade side. Must be LONG or SHORT",
            details = mapOf("providedSide" to request.side)
        )
    }
    
    // Validate prices
    if (request.entryPrice <= BigDecimal.ZERO) {
        return ErrorDetail(
            code = "INVALID_ENTRY_PRICE",
            message = "Entry price must be greater than zero"
        )
    }
    
    if (request.quantity <= BigDecimal.ZERO) {
        return ErrorDetail(
            code = "INVALID_QUANTITY",
            message = "Quantity must be greater than zero"
        )
    }
    
    if (request.stopLoss <= BigDecimal.ZERO) {
        return ErrorDetail(
            code = "INVALID_STOP_LOSS",
            message = "Stop-loss price must be greater than zero"
        )
    }
    
    if (request.takeProfit <= BigDecimal.ZERO) {
        return ErrorDetail(
            code = "INVALID_TAKE_PROFIT",
            message = "Take-profit price must be greater than zero"
        )
    }
    
    // Validate stop-loss and take-profit logic based on side
    if (request.side == "LONG") {
        if (request.stopLoss >= request.entryPrice) {
            return ErrorDetail(
                code = "INVALID_STOP_LOSS",
                message = "For LONG trades, stop-loss must be below entry price",
                details = mapOf(
                    "entryPrice" to request.entryPrice.toString(),
                    "stopLoss" to request.stopLoss.toString()
                )
            )
        }
        if (request.takeProfit <= request.entryPrice) {
            return ErrorDetail(
                code = "INVALID_TAKE_PROFIT",
                message = "For LONG trades, take-profit must be above entry price",
                details = mapOf(
                    "entryPrice" to request.entryPrice.toString(),
                    "takeProfit" to request.takeProfit.toString()
                )
            )
        }
    } else { // SHORT
        if (request.stopLoss <= request.entryPrice) {
            return ErrorDetail(
                code = "INVALID_STOP_LOSS",
                message = "For SHORT trades, stop-loss must be above entry price",
                details = mapOf(
                    "entryPrice" to request.entryPrice.toString(),
                    "stopLoss" to request.stopLoss.toString()
                )
            )
        }
        if (request.takeProfit >= request.entryPrice) {
            return ErrorDetail(
                code = "INVALID_TAKE_PROFIT",
                message = "For SHORT trades, take-profit must be below entry price",
                details = mapOf(
                    "entryPrice" to request.entryPrice.toString(),
                    "takeProfit" to request.takeProfit.toString()
                )
            )
        }
    }
    
    return null
}

