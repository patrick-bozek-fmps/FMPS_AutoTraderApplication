package com.fmps.autotrader.desktop.dashboard

import com.fmps.autotrader.desktop.mvvm.ViewEvent
import com.fmps.autotrader.desktop.services.TraderStatus
import java.time.Instant
import java.util.UUID

data class DashboardState(
    val traderItems: List<TraderItem> = emptyList(),
    val quickStats: QuickStats = QuickStats(),
    val systemStatus: SystemStatusSummary = SystemStatusSummary(),
    val notifications: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = true,
    val lastUpdated: Instant? = null
)

data class TraderItem(
    val id: String,
    val name: String,
    val exchange: String,
    val status: TraderStatus,
    val profitLoss: Double,
    val positions: Int
)

data class QuickStats(
    val activeTraders: Int = 0,
    val stoppedTraders: Int = 0,
    val totalProfitLoss: Double = 0.0,
    val openPositions: Int = 0,
    val criticalAlerts: Int = 0
)

data class SystemStatusSummary(
    val coreServiceHealthy: Boolean = true,
    val telemetryConnected: Boolean = false,
    val lastSummaryUpdate: Instant? = null,
    val lastTelemetryEvent: Instant? = null
)

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val severity: NotificationSeverity,
    val timestamp: Instant = Instant.now()
)

enum class NotificationSeverity {
    INFO,
    WARNING,
    CRITICAL
}

sealed interface DashboardEvent : ViewEvent {
    data class ShowMessage(val message: String) : DashboardEvent
}

enum class TraderAction {
    START,
    STOP,
    OPEN
}


