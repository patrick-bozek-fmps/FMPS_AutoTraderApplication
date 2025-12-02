package com.fmps.autotrader.desktop.services

import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * Service abstraction for managing AI traders through the core-service REST API.
 */
interface TraderService {
    fun traders(): Flow<List<TraderDetail>>

    suspend fun createTrader(draft: TraderDraft): TraderDetail

    suspend fun updateTrader(id: String, draft: TraderDraft): TraderDetail

    suspend fun deleteTrader(id: String)

    suspend fun startTrader(id: String)

    suspend fun stopTrader(id: String)
    
    suspend fun updateTraderBalance(id: String, balance: Double)
}

data class TraderDetail(
    val id: String,
    val name: String,
    val exchange: String,
    val strategy: String,
    val riskLevel: TraderRiskLevel,
    val baseAsset: String,
    val quoteAsset: String,
    val budget: Double,
    val leverage: Int? = null,
    val stopLossPercentage: Double? = null, // As decimal (e.g., 0.02 for 2%)
    val takeProfitPercentage: Double? = null, // As decimal (e.g., 0.05 for 5%)
    val status: TraderStatus,
    val profitLoss: Double,
    val openPositions: Int,
    val createdAt: Instant
)

data class TraderDraft(
    val name: String,
    val exchange: String,
    val strategy: String,
    val riskLevel: TraderRiskLevel,
    val baseAsset: String,
    val quoteAsset: String,
    val budget: Double,
    val leverage: Int? = null, // If null, will use Trader Defaults
    val stopLossPercentage: Double? = null, // If null, will use Trader Defaults (as decimal, e.g., 0.02 for 2%)
    val takeProfitPercentage: Double? = null, // If null, will use default 0.05 (5%)
    val apiKey: String? = null,
    val apiSecret: String? = null,
    val apiPassphrase: String? = null
)

enum class TraderRiskLevel {
    CONSERVATIVE,
    BALANCED,
    AGGRESSIVE
}


