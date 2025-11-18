package com.fmps.autotrader.desktop.dashboard

import com.fmps.autotrader.desktop.mvvm.BaseViewModel
import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.services.CoreServiceClient
import com.fmps.autotrader.desktop.services.TelemetryClient
import com.fmps.autotrader.desktop.services.TelemetrySample
import com.fmps.autotrader.desktop.services.TraderService
import com.fmps.autotrader.desktop.services.TraderStatus
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach

class DashboardViewModel(
    dispatcherProvider: DispatcherProvider,
    private val coreServiceClient: CoreServiceClient,
    private val telemetryClient: TelemetryClient,
    private val traderService: TraderService
) : BaseViewModel<DashboardState, DashboardEvent>(
    initialState = DashboardState(),
    dispatcherProvider = dispatcherProvider
) {

    private val notificationBuffer = ArrayDeque<NotificationItem>()
    private val notificationLock = Any()
    private val maxNotifications = 12
    private var telemetryReconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private var lastTelemetryConnectionCheck = Instant.now()

    init {
        telemetryClient.start()
        observeTraderSummaries()
        observeTelemetry()
        monitorTelemetryConnection()
    }

    override fun onCleared() {
        telemetryClient.stop()
        super.onCleared()
    }

    fun onTraderAction(item: TraderItem, action: TraderAction) {
        when (action) {
            TraderAction.START -> {
                launchIO {
                    try {
                        traderService.startTrader(item.id)
                        publishEvent(DashboardEvent.ShowMessage("Trader ${item.name} started successfully"))
                        // Refresh trader summaries after action
                        delay(500)
                    } catch (e: Exception) {
                        publishEvent(DashboardEvent.ShowMessage("Failed to start trader ${item.name}: ${e.message}"))
                    }
                }
            }
            TraderAction.STOP -> {
                launchIO {
                    try {
                        traderService.stopTrader(item.id)
                        publishEvent(DashboardEvent.ShowMessage("Trader ${item.name} stopped successfully"))
                        // Refresh trader summaries after action
                        delay(500)
                    } catch (e: Exception) {
                        publishEvent(DashboardEvent.ShowMessage("Failed to stop trader ${item.name}: ${e.message}"))
                    }
                }
            }
            TraderAction.OPEN -> {
                publishEvent(DashboardEvent.ShowMessage("Opening details for ${item.name}…"))
            }
        }
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
            telemetryClient.samples()
                .onEach {
                    telemetryReconnectAttempts = 0 // Reset on successful sample
                    lastTelemetryConnectionCheck = Instant.now()
                }
                .collectLatest { sample ->
                    handleTelemetrySample(sample)
                }
        }
    }

    private fun monitorTelemetryConnection() {
        launchIO {
            while (true) {
                delay(10_000) // Check every 10 seconds
                val now = Instant.now()
                val timeSinceLastEvent = java.time.Duration.between(lastTelemetryConnectionCheck, now).seconds
                
                // If no telemetry events for 30 seconds, mark as disconnected
                if (timeSinceLastEvent > 30) {
                    setState { current ->
                        current.copy(
                            systemStatus = current.systemStatus.copy(
                                telemetryConnected = false
                            )
                        )
                    }
                    
                    // Attempt reconnection if not already trying
                    if (telemetryReconnectAttempts < maxReconnectAttempts) {
                        telemetryReconnectAttempts++
                        try {
                            telemetryClient.stop()
                            delay(1_000)
                            telemetryClient.start()
                            publishEvent(DashboardEvent.ShowMessage("Attempting to reconnect telemetry (attempt $telemetryReconnectAttempts/$maxReconnectAttempts)"))
                        } catch (e: Exception) {
                            // Reconnection will be retried on next check
                        }
                    } else if (telemetryReconnectAttempts == maxReconnectAttempts) {
                        publishEvent(DashboardEvent.ShowMessage("Telemetry connection lost. Please check core-service connectivity."))
                        telemetryReconnectAttempts++ // Prevent repeated messages
                    }
                }
            }
        }
    }

    private fun handleTelemetrySample(sample: TelemetrySample) {
        val now = Instant.now()
        
        // Check for connection error messages
        if (sample.channel.contains("system.error", ignoreCase = true) && 
            sample.payload.contains("CONNECTION", ignoreCase = true)) {
            setState { current ->
                current.copy(
                    systemStatus = current.systemStatus.copy(
                        telemetryConnected = false
                    )
                )
            }
            return
        }
        
        val severity = when {
            sample.channel.contains("alert", ignoreCase = true) || 
            sample.channel.contains("error", ignoreCase = true) -> NotificationSeverity.CRITICAL
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


