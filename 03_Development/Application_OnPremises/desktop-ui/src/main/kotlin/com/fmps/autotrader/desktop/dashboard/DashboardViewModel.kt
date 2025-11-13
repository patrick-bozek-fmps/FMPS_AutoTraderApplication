package com.fmps.autotrader.desktop.dashboard

import com.fmps.autotrader.desktop.mvvm.BaseViewModel
import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.services.CoreServiceClient
import com.fmps.autotrader.desktop.services.TelemetryClient
import com.fmps.autotrader.desktop.services.TelemetrySample
import com.fmps.autotrader.desktop.services.TraderStatus
import java.time.Instant
import kotlinx.coroutines.flow.collectLatest

class DashboardViewModel(
    dispatcherProvider: DispatcherProvider,
    private val coreServiceClient: CoreServiceClient,
    private val telemetryClient: TelemetryClient
) : BaseViewModel<DashboardState, DashboardEvent>(
    initialState = DashboardState(),
    dispatcherProvider = dispatcherProvider
) {

    private val notificationBuffer = ArrayDeque<NotificationItem>()
    private val notificationLock = Any()
    private val maxNotifications = 12

    init {
        telemetryClient.start()
        observeTraderSummaries()
        observeTelemetry()
    }

    override fun onCleared() {
        telemetryClient.stop()
        super.onCleared()
    }

    fun onTraderAction(item: TraderItem, action: TraderAction) {
        val message = when (action) {
            TraderAction.START -> "Starting ${item.name}… (stub action)"
            TraderAction.STOP -> "Stopping ${item.name}… (stub action)"
            TraderAction.OPEN -> "Opening details for ${item.name}…"
        }
        publishEvent(DashboardEvent.ShowMessage(message))
    }

    private fun observeTraderSummaries() {
        launchIO {
            coreServiceClient.traderSummaries().collectLatest { summaries ->
                val now = Instant.now()
                val traderItems = summaries.map {
                    TraderItem(
                        id = it.id,
                        name = it.name,
                        exchange = it.exchange,
                        status = it.status,
                        profitLoss = it.profitLoss,
                        positions = it.positions
                    )
                }

                val quickStats = synchronized(notificationLock) {
                    QuickStats(
                        activeTraders = summaries.count { it.status == TraderStatus.RUNNING },
                        stoppedTraders = summaries.count { it.status == TraderStatus.STOPPED },
                        totalProfitLoss = summaries.sumOf { it.profitLoss },
                        openPositions = summaries.sumOf { it.positions },
                        criticalAlerts = notificationBuffer.count { it.severity == NotificationSeverity.CRITICAL }
                    )
                }

                setState { current ->
                    current.copy(
                        traderItems = traderItems,
                        quickStats = quickStats,
                        systemStatus = current.systemStatus.copy(
                            coreServiceHealthy = traderItems.isNotEmpty(),
                            lastSummaryUpdate = now
                        ),
                        lastUpdated = now,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun observeTelemetry() {
        launchIO {
            telemetryClient.samples().collectLatest { sample ->
                handleTelemetrySample(sample)
            }
        }
    }

    private fun handleTelemetrySample(sample: TelemetrySample) {
        val now = Instant.now()
        val severity = when {
            sample.channel.contains("alert", ignoreCase = true) -> NotificationSeverity.CRITICAL
            sample.channel.contains("warning", ignoreCase = true) -> NotificationSeverity.WARNING
            else -> NotificationSeverity.INFO
        }

        val notification = NotificationItem(
            title = channelTitle(sample.channel),
            message = sample.payload,
            severity = severity,
            timestamp = now
        )

        val snapshot: List<NotificationItem>
        val criticalCount: Int
        synchronized(notificationLock) {
            notificationBuffer.addFirst(notification)
            while (notificationBuffer.size > maxNotifications) {
                notificationBuffer.removeLast()
            }
            criticalCount = notificationBuffer.count { it.severity == NotificationSeverity.CRITICAL }
            snapshot = notificationBuffer.toList()
        }

        setState { current ->
            current.copy(
                systemStatus = current.systemStatus.copy(
                    telemetryConnected = true,
                    lastTelemetryEvent = now
                ),
                quickStats = current.quickStats.copy(criticalAlerts = criticalCount),
                notifications = snapshot
            )
        }
    }

    private fun channelTitle(channel: String): String = when {
        channel.contains("trader.status", ignoreCase = true) -> "Trader Status"
        channel.contains("risk", ignoreCase = true) -> "Risk Alert"
        channel.contains("system", ignoreCase = true) -> "System Notification"
        else -> channel.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}


