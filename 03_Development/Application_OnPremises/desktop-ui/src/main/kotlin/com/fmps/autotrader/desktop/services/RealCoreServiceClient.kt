package com.fmps.autotrader.desktop.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class RealCoreServiceClient(
    private val httpClient: HttpClient,
    private val baseUrl: String = "http://localhost:8080"
) : CoreServiceClient {

    @Serializable
    private data class ApiResponse<T>(
        val success: Boolean,
        val data: T,
        val timestamp: String? = null
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
        val totalProfit: String? = null,
        val totalLoss: String? = null,
        val winCount: Int? = null,
        val lossCount: Int? = null,
        val createdAt: String
    )

    @Serializable
    private data class TradeDTO(
        val id: Int,
        val aiTraderId: Int,
        val status: String
    )

    override fun traderSummaries(): Flow<List<TraderSummary>> = flow {
        while (true) {
            try {
                val response = httpClient.get("$baseUrl/api/v1/traders")
                
                if (response.status == HttpStatusCode.OK) {
                    val json = Json { ignoreUnknownKeys = true }
                    val apiResponse = json.decodeFromString<ApiResponse<List<TraderDTO>>>(response.bodyAsText())
                    
                    val summaries = apiResponse.data.map { dto ->
                        // Calculate profit/loss from currentBalance - initialBalance
                        val profitLoss = try {
                            val current = dto.currentBalance.toDoubleOrNull() ?: 0.0
                            val initial = dto.initialBalance.toDoubleOrNull() ?: 0.0
                            current - initial
                        } catch (e: Exception) {
                            // Fallback: use totalProfit - totalLoss if available
                            try {
                                val profit = dto.totalProfit?.toDoubleOrNull() ?: 0.0
                                val loss = dto.totalLoss?.toDoubleOrNull() ?: 0.0
                                profit - loss
                            } catch (e2: Exception) {
                                0.0
                            }
                        }
                        
                        // Map status: ACTIVE/RUNNING -> RUNNING, PAUSED/STOPPING -> STOPPED, ERROR -> ERROR
                        val status = when (dto.status.uppercase()) {
                            "ACTIVE", "RUNNING" -> TraderStatus.RUNNING
                            "PAUSED", "STOPPING", "STOPPED", "IDLE" -> TraderStatus.STOPPED
                            "ERROR" -> TraderStatus.ERROR
                            else -> TraderStatus.STOPPED
                        }
                        
                        // Fetch open positions count for this trader
                        val openPositions = try {
                            val tradesResponse = httpClient.get("$baseUrl/api/v1/trades") {
                                parameter("aiTraderId", dto.id)
                                parameter("status", "OPEN")
                            }
                            if (tradesResponse.status == HttpStatusCode.OK) {
                                val tradesJson = Json { ignoreUnknownKeys = true }
                                val tradesApiResponse = tradesJson.decodeFromString<ApiResponse<List<TradeDTO>>>(tradesResponse.bodyAsText())
                                tradesApiResponse.data.size
                            } else {
                                0
                            }
                        } catch (e: Exception) {
                            // If fetching positions fails, default to 0
                            0
                        }
                        
                        TraderSummary(
                            id = dto.id.toString(),
                            name = dto.name,
                            exchange = dto.exchange,
                            status = status,
                            profitLoss = profitLoss,
                            positions = openPositions
                        )
                    }
                    emit(summaries)
                } else {
                    // If API not available, emit empty list
                    emit(emptyList())
                }
            } catch (e: Exception) {
                // If API call fails, emit empty list
                emit(emptyList())
            }
            
            delay(1_000) // Poll every 1 second
        }
    }
}

