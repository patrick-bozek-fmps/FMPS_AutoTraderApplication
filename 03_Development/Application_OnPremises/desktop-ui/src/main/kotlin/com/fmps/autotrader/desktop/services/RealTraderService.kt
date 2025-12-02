package com.fmps.autotrader.desktop.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.errors.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Real TraderService implementation using REST API calls to core-service.
 */
class RealTraderService(
    private val httpClient: HttpClient,
    private val baseUrl: String = "http://localhost:8080",
    private val apiKey: String? = null
) : TraderService {

    private val tradersFlow = MutableStateFlow<List<TraderDetail>>(emptyList())
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        // Poll for trader updates periodically
        scope.launch {
            while (true) {
                refreshTraders()
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    override fun traders(): Flow<List<TraderDetail>> = tradersFlow.asStateFlow()

    override suspend fun createTrader(draft: TraderDraft): TraderDetail {
        // Use leverage from draft (which comes from Trader Defaults), or fallback to risk-based calculation
        val leverage = draft.leverage ?: when (draft.riskLevel) {
            TraderRiskLevel.CONSERVATIVE -> 3
            TraderRiskLevel.BALANCED -> 5
            TraderRiskLevel.AGGRESSIVE -> 10
        }
        
        // Use stopLossPercentage from draft (which comes from Trader Defaults), or fallback to 2% (0.02)
        val stopLossDecimal = draft.stopLossPercentage ?: 0.02
        
        // Use takeProfitPercentage from draft, or fallback to 5% (0.05)
        val takeProfitDecimal = draft.takeProfitPercentage ?: 0.05
        
        logger.info { "🔍 Creating trader with: leverage=$leverage, stopLoss=$stopLossDecimal, takeProfit=$takeProfitDecimal, budget=${draft.budget}" }
        
        val response = httpClient.post("$baseUrl/api/v1/traders") {
            contentType(ContentType.Application.Json)
            apiKey?.let { header("X-API-Key", it) }
            setBody(CreateTraderRequest(
                name = draft.name,
                exchange = draft.exchange.uppercase(), // API expects uppercase: BINANCE, BITGET, TRADINGVIEW
                tradingPair = "${draft.baseAsset}/${draft.quoteAsset}",
                leverage = leverage,
                initialBalance = draft.budget.toString(),
                stopLossPercentage = stopLossDecimal.toString(),
                takeProfitPercentage = takeProfitDecimal.toString()
            ))
        }
        
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            val userFriendlyMessage = try {
                // Try to parse the error response to get the actual error message
                val errorResponse = json.decodeFromString<ErrorResponse>(errorBody)
                errorResponse.error?.message ?: "Failed to create trader: ${response.status}"
            } catch (e: Exception) {
                // Fallback to generic message if parsing fails
                when {
                    response.status.value == 503 -> "Core service is unavailable. Please ensure the core service is running."
                    response.status.value == 500 -> "Server error occurred. Please try again later."
                    response.status.value == 400 -> "Invalid request. Please check your input and try again."
                    else -> "Failed to create trader: ${response.status}"
                }
            }
            logger.error { "$userFriendlyMessage - $errorBody" }
            throw ClientRequestException(response, userFriendlyMessage)
        }
        
        val apiResponse = json.decodeFromString<ApiResponse<TraderDTO>>(response.bodyAsText())
        val trader = mapToTraderDetail(apiResponse.data)
        
        tradersFlow.update { it + trader }
        return trader
    }

    override suspend fun updateTrader(id: String, draft: TraderDraft): TraderDetail {
        // Use leverage from draft (which comes from Trader Defaults), or fallback to risk-based calculation
        val leverage = draft.leverage ?: when (draft.riskLevel) {
            TraderRiskLevel.CONSERVATIVE -> 3
            TraderRiskLevel.BALANCED -> 5
            TraderRiskLevel.AGGRESSIVE -> 10
        }
        
        // Use stopLossPercentage from draft (which comes from Trader Defaults), or fallback to 2% (0.02)
        val stopLossDecimal = draft.stopLossPercentage ?: 0.02
        
        // Use takeProfitPercentage from draft, or fallback to 5% (0.05)
        val takeProfitDecimal = draft.takeProfitPercentage ?: 0.05
        
        logger.info { "🔍 Updating trader $id with: leverage=$leverage, stopLoss=$stopLossDecimal, takeProfit=$takeProfitDecimal, budget=${draft.budget}" }
        
        val response = httpClient.put("$baseUrl/api/v1/traders/$id") {
            contentType(ContentType.Application.Json)
            apiKey?.let { header("X-API-Key", it) }
            setBody(UpdateTraderRequest(
                name = draft.name,
                exchange = draft.exchange.uppercase(), // API expects uppercase: BINANCE, BITGET, TRADINGVIEW
                tradingPair = "${draft.baseAsset}/${draft.quoteAsset}",
                leverage = leverage,
                initialBalance = draft.budget.toString(),
                stopLossPercentage = stopLossDecimal.toString(),
                takeProfitPercentage = takeProfitDecimal.toString()
            ))
        }
        
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            val userFriendlyMessage = when {
                response.status.value == 503 -> "Core service is unavailable. Please ensure the core service is running."
                response.status.value == 500 -> "Server error occurred. Please try again later."
                else -> "Failed to update trader: ${response.status}"
            }
            logger.error { "$userFriendlyMessage - $errorBody" }
            throw ClientRequestException(response, userFriendlyMessage)
        }
        
        val apiResponse = json.decodeFromString<ApiResponse<TraderDTO>>(response.bodyAsText())
        val trader = mapToTraderDetail(apiResponse.data)
        
        tradersFlow.update { current ->
            current.map { if (it.id == id) trader else it }
        }
        return trader
    }

    override suspend fun deleteTrader(id: String) {
        val response = httpClient.delete("$baseUrl/api/v1/traders/$id") {
            apiKey?.let { header("X-API-Key", it) }
        }
        
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            val userFriendlyMessage = when {
                response.status.value == 503 -> "Core service is unavailable. Please ensure the core service is running."
                response.status.value == 500 -> "Server error occurred. Please try again later."
                else -> "Failed to delete trader: ${response.status}"
            }
            logger.error { "$userFriendlyMessage - $errorBody" }
            throw ClientRequestException(response, userFriendlyMessage)
        }
        
        tradersFlow.update { it.filterNot { trader -> trader.id == id } }
    }

    override suspend fun startTrader(id: String) {
        val response = httpClient.patch("$baseUrl/api/v1/traders/$id/status") {
            contentType(ContentType.Application.Json)
            apiKey?.let { header("X-API-Key", it) }
            setBody(UpdateStatusRequest(status = "ACTIVE"))
        }
        
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            val userFriendlyMessage = when {
                response.status.value == 503 -> "Core service is unavailable. Please ensure the core service is running."
                response.status.value == 500 -> "Server error occurred. Please try again later."
                else -> "Failed to start trader: ${response.status}"
            }
            logger.error { "$userFriendlyMessage - $errorBody" }
            throw ClientRequestException(response, userFriendlyMessage)
        }
        
        refreshTraders()
    }

    override suspend fun stopTrader(id: String) {
        val response = httpClient.patch("$baseUrl/api/v1/traders/$id/status") {
            contentType(ContentType.Application.Json)
            apiKey?.let { header("X-API-Key", it) }
            setBody(UpdateStatusRequest(status = "PAUSED"))
        }
        
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            val userFriendlyMessage = when {
                response.status.value == 503 -> "Core service is unavailable. Please ensure the core service is running."
                response.status.value == 500 -> "Server error occurred. Please try again later."
                else -> "Failed to stop trader: ${response.status}"
            }
            logger.error { "$userFriendlyMessage - $errorBody" }
            throw ClientRequestException(response, userFriendlyMessage)
        }
        
        refreshTraders()
    }

    override suspend fun updateTraderBalance(id: String, balance: Double) {
        val response = httpClient.patch("$baseUrl/api/v1/traders/$id/balance") {
            contentType(ContentType.Application.Json)
            apiKey?.let { header("X-API-Key", it) }
            setBody(UpdateBalanceRequest(balance = balance.toString()))
        }
        
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            val userFriendlyMessage = when {
                response.status.value == 503 -> "Core service is unavailable. Please ensure the core service is running."
                response.status.value == 500 -> "Server error occurred. Please try again later."
                else -> "Failed to update trader balance: ${response.status}"
            }
            logger.error { "$userFriendlyMessage - $errorBody" }
            throw ClientRequestException(response, userFriendlyMessage)
        }
        
        refreshTraders()
    }

    private suspend fun refreshTraders() {
        try {
            val response = httpClient.get("$baseUrl/api/v1/traders") {
                apiKey?.let { header("X-API-Key", it) }
            }
            
            if (response.status.isSuccess()) {
                val apiResponse = json.decodeFromString<ApiResponse<List<TraderDTO>>>(response.bodyAsText())
                tradersFlow.value = apiResponse.data.map { mapToTraderDetail(it) }
            } else {
                logger.warn { "Failed to refresh traders: ${response.status}" }
            }
        } catch (e: Exception) {
            val userFriendlyMessage = when {
                e.message?.contains("Connection refused") == true -> 
                    "Cannot connect to core service. Please ensure the core service is running on localhost:8080"
                e.message?.contains("getsockopt") == true -> 
                    "Cannot connect to core service. Please ensure the core service is running."
                e.message?.contains("timeout") == true -> 
                    "Connection timeout. The core service may be slow to respond."
                else -> 
                    "Error refreshing traders: ${e.message ?: "Unknown error"}"
            }
            logger.error(e) { userFriendlyMessage }
        }
    }

    private fun mapToTraderDetail(dto: TraderDTO): TraderDetail {
        val status = when (dto.status) {
            "ACTIVE", "RUNNING" -> TraderStatus.RUNNING
            "PAUSED", "STOPPING" -> TraderStatus.STOPPED
            else -> TraderStatus.STOPPED
        }
        
        val riskLevel = when {
            dto.leverage <= 3 -> TraderRiskLevel.CONSERVATIVE
            dto.leverage <= 7 -> TraderRiskLevel.BALANCED
            else -> TraderRiskLevel.AGGRESSIVE
        }
        
        val (baseAsset, quoteAsset) = dto.tradingPair.split("/").let {
            if (it.size == 2) Pair(it[0], it[1]) else Pair("BTC", "USDT")
        }
        
        // Parse date with fallback for different formats
        val createdAt = try {
            Instant.parse(dto.createdAt)
        } catch (e: Exception) {
            // Try parsing with different formats if standard ISO format fails
            try {
                // Handle format like "2025-11-21T15:16:20.483" (missing timezone)
                if (dto.createdAt.contains("T") && !dto.createdAt.endsWith("Z") && !dto.createdAt.contains("+")) {
                    Instant.parse("${dto.createdAt}Z")
                } else {
                    Instant.now() // Fallback to current time if parsing fails
                }
            } catch (e2: Exception) {
                logger.warn { "Failed to parse createdAt date: ${dto.createdAt}, using current time" }
                Instant.now()
            }
        }
        
        // Normalize exchange: API returns uppercase (BINANCE, BITGET), but UI expects capitalized (Binance, Bitget)
        val normalizedExchange = when (dto.exchange.uppercase()) {
            "BINANCE" -> "Binance"
            "BITGET" -> "Bitget"
            "TRADINGVIEW" -> "TradingView"
            else -> dto.exchange.capitalize()
        }
        
        return TraderDetail(
            id = dto.id.toString(),
            name = dto.name,
            exchange = normalizedExchange, // Normalize to match UI dropdown values
            strategy = "TREND_FOLLOWING", // Default strategy - API doesn't return strategy, so use default
            riskLevel = riskLevel,
            baseAsset = baseAsset,
            quoteAsset = quoteAsset,
            budget = dto.initialBalance.toDoubleOrNull() ?: 0.0,
            leverage = dto.leverage,
            stopLossPercentage = dto.stopLossPercentage?.toDoubleOrNull(),
            takeProfitPercentage = dto.takeProfitPercentage?.toDoubleOrNull(),
            status = status,
            profitLoss = (dto.currentBalance.toDoubleOrNull() ?: 0.0) - (dto.initialBalance.toDoubleOrNull() ?: 0.0),
            openPositions = 0, // Would need separate API call
            createdAt = createdAt
        )
    }

    @Serializable
    private data class ApiResponse<T>(val success: Boolean, val data: T, val timestamp: String)
    
    @Serializable
    private data class ErrorResponse(
        val success: Boolean,
        val error: ErrorDetail? = null,
        val timestamp: String
    )
    
    @Serializable
    private data class ErrorDetail(
        val code: String,
        val message: String,
        val details: Map<String, String>? = null
    )

    @Serializable
    private data class TraderDTO(
        val id: Int,
        val name: String,
        val exchange: String,
        val tradingPair: String,
        val status: String,
        val leverage: Int,
        val initialBalance: String,
        val currentBalance: String,
        val stopLossPercentage: String? = null,
        val takeProfitPercentage: String? = null,
        val createdAt: String
    )

    @Serializable
    private data class CreateTraderRequest(
        val name: String,
        val exchange: String,
        val tradingPair: String,
        val leverage: Int,
        val initialBalance: String,
        val stopLossPercentage: String,
        val takeProfitPercentage: String
    )

    @Serializable
    private data class UpdateTraderRequest(
        val name: String,
        val exchange: String,
        val tradingPair: String,
        val leverage: Int,
        val initialBalance: String,
        val stopLossPercentage: String,
        val takeProfitPercentage: String
    )

    @Serializable
    private data class UpdateStatusRequest(val status: String)
    
    @Serializable
    private data class UpdateBalanceRequest(val balance: String)
}

