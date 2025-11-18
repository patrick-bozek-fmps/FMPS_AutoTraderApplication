package com.fmps.autotrader.desktop.dashboard

import com.fmps.autotrader.desktop.components.MetricTile
import com.fmps.autotrader.desktop.components.StatusBadge
import com.fmps.autotrader.desktop.components.ToolbarButton
import com.fmps.autotrader.desktop.i18n.Localization
import com.fmps.autotrader.desktop.mvvm.BaseView
import com.fmps.autotrader.desktop.services.TraderStatus
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.util.Locale
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.Separator
import javafx.scene.layout.BorderPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.util.Callback
import tornadofx.action
import tornadofx.addClass
import tornadofx.borderpane
import tornadofx.hbox
import tornadofx.hgrow
import tornadofx.information
import tornadofx.label
import tornadofx.listview
import tornadofx.separator
import tornadofx.paddingAll
import tornadofx.vbox

class DashboardView :
    BaseView<DashboardState, DashboardEvent, DashboardViewModel>(DashboardViewModel::class) {

    private val traders = FXCollections.observableArrayList<TraderItem>()
    private val notifications = FXCollections.observableArrayList<NotificationItem>()

    private val activeTile = MetricTile("Active Traders", "0").apply { id = "metric-active-traders" }
    private val stoppedTile = MetricTile("Stopped", "0").apply { id = "metric-stopped-traders" }
    private val pnlTile = MetricTile("Total P&L", "0.00", "Last 24h").apply { id = "metric-total-pnl" }
    private val positionsTile = MetricTile("Open Positions", "0").apply { id = "metric-open-positions" }

    private val coreServiceStatusLabel = Label().apply { addClass("status-label") }
    private val telemetryStatusLabel = Label().apply { addClass("status-label") }
    private val summaryTimestampLabel = Label().apply { addClass("timestamp-label") }
    private val telemetryTimestampLabel = Label().apply { addClass("timestamp-label") }

    private val traderListView: ListView<TraderItem> = listview(traders) {
        addClass("content-card")
        cellFactory = Callback { TraderSummaryCell() }
        prefHeight = 320.0
        id = "dashboard-trader-list"
    }

    private val notificationListView: ListView<NotificationItem> = listview(notifications) {
        addClass("content-card")
        cellFactory = Callback { NotificationCell() }
        placeholder = Label("No notifications yet.").apply { addClass("placeholder-label") }
        id = "dashboard-notifications"
    }

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    override val root: BorderPane = borderpane {
        paddingAll = 12.0
        center = buildContent()
    }

    override fun onStateChanged(state: DashboardState) {
        traders.setAll(state.traderItems)
        notifications.setAll(state.notifications)
        updateQuickStats(state.quickStats)
        updateSystemStatus(state.systemStatus)
    }

    override fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.ShowMessage -> information(event.message)
        }
    }

    private fun buildContent(): VBox = vbox(16.0) {
        addClass("dashboard-root")

        children += buildHeader()
        children += buildQuickStatsRow()
        children += buildMainPanels()
        children += buildNotificationsPanel()
    }

    private fun buildHeader(): HBox = hbox(12.0) {
        addClass("dashboard-header")
        alignment = Pos.CENTER_LEFT
        padding = Insets(4.0, 4.0, 8.0, 4.0)

        label(Localization.string("dashboard.title", "Operations Dashboard")) {
            addClass("view-header")
        }
        separator()
        label(Localization.string("dashboard.subtitle", "Real-time overview of traders and platform health.")) {
            addClass("view-description")
        }
    }

    private fun buildQuickStatsRow(): FlowPane = FlowPane().apply {
        addClass("quick-stats-row")
        hgap = 12.0
        vgap = 12.0
        children.addAll(activeTile, stoppedTile, pnlTile, positionsTile)
    }

    private fun buildMainPanels(): HBox = hbox(16.0) {
        addClass("dashboard-main-panels")

        val tradersSection = VBox(8.0).apply {
            addClass("content-card")
            paddingAll = 16.0
            children += Label(Localization.string("dashboard.trader_overview", "Trader Overview")).apply {
                addClass("section-title")
            }
            children += Separator()
            children += traderListView
        }

        val statusSection = VBox(12.0).apply {
            addClass("content-card")
            paddingAll = 16.0
            prefWidth = 320.0
            children += Label(Localization.string("dashboard.system_status", "System Status")).apply {
                addClass("section-title")
            }
            children += Separator()
            children += buildStatusRow(
                Localization.string("dashboard.core_service", "Core Service"),
                coreServiceStatusLabel
            )
            children += summaryTimestampLabel
            children += Separator()
            children += buildStatusRow(
                Localization.string("dashboard.telemetry", "Telemetry Stream"),
                telemetryStatusLabel
            )
            children += telemetryTimestampLabel
        }

        children += tradersSection
        children += statusSection
        HBox.setHgrow(tradersSection, Priority.ALWAYS)
    }

    private fun buildNotificationsPanel(): VBox = VBox(8.0).apply {
        addClass("content-card")
        paddingAll = 16.0
        children += Label(Localization.string("dashboard.notifications", "Notifications & Activity")).apply {
            addClass("section-title")
        }
        children += Separator()
        children += notificationListView.apply { prefHeight = 200.0 }
    }

    private fun buildStatusRow(labelText: String, statusLabel: Label): HBox = hbox(8.0) {
        alignment = Pos.CENTER_LEFT
        children += Label(labelText).apply { addClass("status-title") }
        children += statusLabel
    }

    private fun updateQuickStats(quickStats: QuickStats) {
        activeTile.valueProperty.set(quickStats.activeTraders.toString())
        stoppedTile.valueProperty.set(quickStats.stoppedTraders.toString())
        pnlTile.valueProperty.set(currencyFormat.format(quickStats.totalProfitLoss))
        positionsTile.valueProperty.set(quickStats.openPositions.toString())
        val alertsTemplate = Localization.string("dashboard.alerts_active", "%d alerts")
        positionsTile.subtitleProperty.set(String.format(Locale.getDefault(), alertsTemplate, quickStats.criticalAlerts))
    }

    private fun updateSystemStatus(status: SystemStatusSummary) {
        coreServiceStatusLabel.text = if (status.coreServiceHealthy) "Healthy" else "Unavailable"
        coreServiceStatusLabel.styleClass.setAll(
            "status-label",
            if (status.coreServiceHealthy) "status-label-ok" else "status-label-error"
        )

        telemetryStatusLabel.text = if (status.telemetryConnected) "Connected" else "Disconnected"
        telemetryStatusLabel.styleClass.setAll(
            "status-label",
            if (status.telemetryConnected) "status-label-ok" else "status-label-warning"
        )

        summaryTimestampLabel.text = status.lastSummaryUpdate?.let {
            "Trader data updated ${timeAgo(it)}"
        } ?: "Awaiting trader data…"

        telemetryTimestampLabel.text = if (status.telemetryConnected) {
            status.lastTelemetryEvent?.let {
                "Telemetry event ${timeAgo(it)}"
            } ?: "Connected, awaiting events"
        } else {
            status.lastTelemetryEvent?.let {
                "Disconnected (last event ${timeAgo(it)})"
            } ?: "Telemetry disconnected - check core-service"
        }
        
        // Show empty state message if no traders and telemetry disconnected
        if (traders.isEmpty() && !status.telemetryConnected && !status.coreServiceHealthy) {
            notificationListView.placeholder = Label("No data available. Please check core-service connectivity.").apply {
                addClass("placeholder-label")
                styleClass += "placeholder-error"
            }
        } else if (traders.isEmpty()) {
            notificationListView.placeholder = Label("No traders configured yet.").apply {
                addClass("placeholder-label")
            }
        }
    }

    private fun timeAgo(timestamp: Instant): String {
        val duration = Duration.between(timestamp, Instant.now()).coerceAtLeast(Duration.ZERO)
        val seconds = duration.seconds
        return when {
            seconds < 60 -> "${seconds}s ago"
            seconds < 3600 -> "${seconds / 60}m ago"
            else -> "${seconds / 3600}h ago"
        }
    }

    private inner class TraderSummaryCell : ListCell<TraderItem>() {
        override fun updateItem(item: TraderItem?, empty: Boolean) {
            super.updateItem(item, empty)
            if (empty || item == null) {
                graphic = null
                text = null
                return
            }

            graphic = hbox(12.0) {
                addClass("trader-row")
                alignment = Pos.CENTER_LEFT
                paddingAll = 8.0

                children += VBox(4.0).apply {
                    children += Label(item.name).apply { addClass("trader-name") }
                    children += Label("${item.exchange} • ${item.positions} positions").apply {
                        addClass("trader-meta")
                    }
                }

                children += StatusBadge(item.status, showText = true)

                children += Label(currencyFormat.format(item.profitLoss)).apply {
                    addClass(if (item.profitLoss >= 0) "profit-positive" else "profit-negative")
                }

                children += HBox(8.0).apply {
                    alignment = Pos.CENTER_RIGHT
                    hgrow = Priority.ALWAYS
                    children += ToolbarButton("Open", "→").apply {
                        action { viewModel.onTraderAction(item, TraderAction.OPEN) }
                    }
                    children += ToolbarButton("Start", "▲", ToolbarButton.Emphasis.SECONDARY).apply {
                        isDisable = item.status == TraderStatus.RUNNING
                        action { viewModel.onTraderAction(item, TraderAction.START) }
                    }
                    children += ToolbarButton("Stop", "■", ToolbarButton.Emphasis.SECONDARY).apply {
                        isDisable = item.status != TraderStatus.RUNNING
                        action { viewModel.onTraderAction(item, TraderAction.STOP) }
                    }
                }
            }
        }
    }

    private inner class NotificationCell : ListCell<NotificationItem>() {
        override fun updateItem(item: NotificationItem?, empty: Boolean) {
            super.updateItem(item, empty)
            if (empty || item == null) {
                graphic = null
                text = null
                return
            }

            graphic = hbox(12.0) {
                alignment = Pos.CENTER_LEFT
                paddingAll = 6.0

                children += Label(levelIcon(item.severity)).apply { addClass("notification-icon") }
                children += VBox(4.0).apply {
                    children += Label(item.title).apply { addClass("notification-title") }
                    children += Label(item.message).apply {
                        addClass("notification-message")
                        isWrapText = true
                        maxWidth = 480.0
                    }
                }
                children += Label(timeAgo(item.timestamp)).apply { addClass("notification-timestamp") }
            }
        }

        private fun levelIcon(severity: NotificationSeverity): String = when (severity) {
            NotificationSeverity.INFO -> "ℹ"
            NotificationSeverity.WARNING -> "⚠"
            NotificationSeverity.CRITICAL -> "⛔"
        }
    }
}


