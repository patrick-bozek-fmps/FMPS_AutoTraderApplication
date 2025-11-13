package com.fmps.autotrader.desktop.services

import kotlinx.coroutines.flow.Flow

data class TraderSummary(
    val id: String,
    val name: String,
    val exchange: String,
    val status: TraderStatus,
    val profitLoss: Double,
    val positions: Int
)

enum class TraderStatus {
    RUNNING,
    STOPPED,
    ERROR
}

interface CoreServiceClient {
    fun traderSummaries(): Flow<List<TraderSummary>>
}


