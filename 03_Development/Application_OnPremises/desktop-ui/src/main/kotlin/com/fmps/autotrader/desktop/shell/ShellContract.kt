package com.fmps.autotrader.desktop.shell

import com.fmps.autotrader.desktop.mvvm.ViewEvent
import com.fmps.autotrader.desktop.services.ConnectionStatus
import com.fmps.autotrader.desktop.services.TraderSummary

data class ShellState(
    val currentRoute: String = "dashboard", // Default to dashboard
    val breadcrumbs: List<String> = emptyList(),
    val traderSummaries: List<TraderSummary> = emptyList(),
    val canNavigateBack: Boolean = false,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.RECONNECTING,
    val connectionErrorMessage: String? = null,
    val binanceConnected: Boolean? = null, // null = not tested, true = connected, false = failed
    val bitgetConnected: Boolean? = null   // null = not tested, true = connected, false = failed
)

sealed interface ShellEvent : ViewEvent {
    data class Toast(val message: String) : ShellEvent
    data class ShowConnectionHelp(val instructions: String) : ShellEvent
}


