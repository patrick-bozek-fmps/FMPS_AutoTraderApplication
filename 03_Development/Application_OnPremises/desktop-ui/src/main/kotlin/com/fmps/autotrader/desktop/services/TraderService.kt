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
    val apiKey: String? = null,
    val apiSecret: String? = null,
    val apiPassphrase: String? = null
)

enum class TraderRiskLevel {
    CONSERVATIVE,
    BALANCED,
    AGGRESSIVE
}


