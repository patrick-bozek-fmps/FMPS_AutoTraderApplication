package com.fmps.autotrader.desktop.traders

import com.fmps.autotrader.desktop.mvvm.ViewEvent
import com.fmps.autotrader.desktop.services.TraderDetail
import com.fmps.autotrader.desktop.services.TraderDraft
import com.fmps.autotrader.desktop.services.TraderRiskLevel
import com.fmps.autotrader.desktop.services.TraderStatus

data class TraderManagementState(
    val isLoading: Boolean = true,
    val traders: List<TraderDetail> = emptyList(),
    val filteredTraders: List<TraderDetail> = emptyList(),
    val selectedTraderId: String? = null,
    val form: TraderForm = TraderForm(),
    val statusFilter: TraderStatusFilter = TraderStatusFilter.ALL,
    val searchQuery: String = "",
    val isSaving: Boolean = false
)

data class TraderForm(
    val id: String? = null,
    val name: String = "",
    val exchange: String = "Binance",
    val strategy: String = "Momentum",
    val riskLevel: TraderRiskLevel = TraderRiskLevel.BALANCED,
    val baseAsset: String = "BTC",
    val quoteAsset: String = "USDT",
    val budget: Double = 1000.0,
    val apiKey: String = "",
    val apiSecret: String = "",
    val apiPassphrase: String = "",
    val errors: Map<String, String> = emptyMap()
) {
    val isNew: Boolean get() = id == null

    fun toDraft() = TraderDraft(
        name = name,
        exchange = exchange,
        strategy = strategy,
        riskLevel = riskLevel,
        baseAsset = baseAsset,
        quoteAsset = quoteAsset,
        budget = budget,
        apiKey = apiKey.takeIf { it.isNotBlank() },
        apiSecret = apiSecret.takeIf { it.isNotBlank() },
        apiPassphrase = apiPassphrase.takeIf { it.isNotBlank() }
    )

    companion object {
        fun fromTrader(trader: TraderDetail) = TraderForm(
            id = trader.id,
            name = trader.name,
            exchange = trader.exchange,
            strategy = trader.strategy,
            riskLevel = trader.riskLevel,
            baseAsset = trader.baseAsset,
            quoteAsset = trader.quoteAsset,
            budget = trader.budget,
            apiKey = "", // Credentials not stored in TraderDetail for security
            apiSecret = "",
            apiPassphrase = ""
        )
    }
}

enum class TraderStatusFilter {
    ALL,
    RUNNING,
    STOPPED,
    ERROR
}

sealed interface TraderManagementEvent : ViewEvent {
    data class ShowMessage(val message: String, val type: MessageType = MessageType.INFO) : TraderManagementEvent

    enum class MessageType { INFO, SUCCESS, ERROR }
}

fun List<TraderDetail>.filter(
    query: String,
    statusFilter: TraderStatusFilter
): List<TraderDetail> {
    val normalized = query.trim().lowercase()
    return filter { trader ->
        val matchesText = normalized.isEmpty() ||
            trader.name.lowercase().contains(normalized) ||
            trader.exchange.lowercase().contains(normalized) ||
            trader.strategy.lowercase().contains(normalized)
        val matchesStatus = when (statusFilter) {
            TraderStatusFilter.ALL -> true
            TraderStatusFilter.RUNNING -> trader.status == TraderStatus.RUNNING
            TraderStatusFilter.STOPPED -> trader.status == TraderStatus.STOPPED
            TraderStatusFilter.ERROR -> trader.status == TraderStatus.ERROR
        }
        matchesText && matchesStatus
    }
}

