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
            refreshTraders()
        }
    }

    override fun traders(): Flow<List<TraderDetail>> = tradersFlow.asStateFlow()

    override suspend fun createTrader(draft: TraderDraft): TraderDetail {
        val response = httpClient.post("$baseUrl/api/v1/traders") {
            contentType(ContentType.Application.Json)
            apiKey?.let { header("X-API-Key", it) }
            setBody(CreateTraderRequest(
                name = draft.name,
                exchange = draft.exchange,
                tradingPair = "${draft.baseAsset}/${draft.quoteAsset}",
                leverage = when (draft.riskLevel) {
                    TraderRiskLevel.CONSERVATIVE -> 3
                    TraderRiskLevel.BALANCED -> 5
                    TraderRiskLevel.AGGRESSIVE -> 10
                },
                initialBalance = draft.budget.toString(),
                stopLossPercentage = "0.02",
                takeProfitPercentage = "0.05"
            ))
        }
        
        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            val userFriendlyMessage = when {
                response.status.value == 503 -> "Core service is unavailable. Please ensure the core service is running."
                response.status.value == 500 -> "Server error occurred. Please try again later."
                else -> "Failed to create trader: ${response.status}"
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
        val response = httpClient.put("$baseUrl/api/v1/traders/$id") {
            contentType(ContentType.Application.Json)
            apiKey?.let { header("X-API-Key", it) }
            setBody(UpdateTraderRequest(
                name = draft.name,
                exchange = draft.exchange,
                tradingPair = "${draft.baseAsset}/${draft.quoteAsset}",
                leverage = when (draft.riskLevel) {
                    TraderRiskLevel.CONSERVATIVE -> 3
                    TraderRiskLevel.BALANCED -> 5
                    TraderRiskLevel.AGGRESSIVE -> 10
                },
                initialBalance = draft.budget.toString(),
                stopLossPercentage = "0.02",
                takeProfitPercentage = "0.05"
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
        
        return TraderDetail(
            id = dto.id.toString(),
            name = dto.name,
            exchange = dto.exchange,
            strategy = "Momentum", // Default, could be enhanced
            riskLevel = riskLevel,
            baseAsset = baseAsset,
            quoteAsset = quoteAsset,
            budget = dto.initialBalance.toDoubleOrNull() ?: 0.0,
            status = status,
            profitLoss = (dto.currentBalance.toDoubleOrNull() ?: 0.0) - (dto.initialBalance.toDoubleOrNull() ?: 0.0),
            openPositions = 0, // Would need separate API call
            createdAt = Instant.parse(dto.createdAt)
        )
    }

    @Serializable
    private data class ApiResponse<T>(val success: Boolean, val data: T, val timestamp: String)

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
}

