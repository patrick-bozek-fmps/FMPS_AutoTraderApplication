package com.fmps.autotrader.desktop.shell

import com.fmps.autotrader.desktop.mvvm.ViewEvent
import com.fmps.autotrader.desktop.services.ConnectionStatus
import com.fmps.autotrader.desktop.services.TraderSummary

data class ShellState(
    val currentRoute: String = "",
    val breadcrumbs: List<String> = emptyList(),
    val traderSummaries: List<TraderSummary> = emptyList(),
    val canNavigateBack: Boolean = false,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.RECONNECTING,
    val connectionErrorMessage: String? = null
)

sealed interface ShellEvent : ViewEvent {
    data class Toast(val message: String) : ShellEvent
    data class ShowConnectionHelp(val instructions: String) : ShellEvent
}


