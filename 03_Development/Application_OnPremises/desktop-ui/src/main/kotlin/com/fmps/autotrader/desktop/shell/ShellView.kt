package com.fmps.autotrader.desktop.shell

import com.fmps.autotrader.desktop.components.MetricTile
import com.fmps.autotrader.desktop.components.StatusBadge
import com.fmps.autotrader.desktop.components.ToolbarButton
import com.fmps.autotrader.desktop.i18n.Localization
import com.fmps.autotrader.desktop.mvvm.BaseView
import com.fmps.autotrader.desktop.navigation.NavigationService
import com.fmps.autotrader.desktop.services.TraderSummary
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.control.Label
import javafx.scene.control.Separator
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import org.koin.core.component.get
import org.koin.core.context.GlobalContext
import tornadofx.*

class ShellView : BaseView<ShellState, ShellEvent, ShellViewModel>(ShellViewModel::class) {

    private val navigationService: NavigationService = GlobalContext.get().get()

    private val breadcrumbBar = HBox().apply { addClass("breadcrumb-bar") }
    private val quickStatsContainer = HBox(12.0)
    private val traderListContainer = VBox(10.0)
    private val contentHolder = StackPane().apply { paddingAll = 12.0 }
    private val backButton = ToolbarButton("Back", "⟲")

    override val root: BorderPane = borderpane {
        addClass("app-shell")
        left = buildSidebar()
        center = buildContent()
    }

    init {
        navigationService.currentViewProperty.addListener { _, _, newValue ->
            val node = newValue?.root
            contentHolder.children.setAll(node ?: placeholderView())
        }
        backButton.action {
            viewModel.goBack()
        }
    }

    override fun onStateChanged(state: ShellState) {
        updateBreadcrumbs(state)
        updateQuickStats(state)
        updateTraderList(state.traderSummaries)
        updateConnectionStatus(state)
        backButton.isDisable = !state.canNavigateBack
    }

    override fun onEvent(event: ShellEvent) {
        when (event) {
            is ShellEvent.Toast -> information(event.message)
            is ShellEvent.ShowConnectionHelp -> {
                information(
                    title = "How to Start Core Service",
                    header = "Core Service Not Running",
                    content = event.instructions
                )
            }
        }
    }

    private fun buildSidebar(): VBox = vbox(12.0) {
        addClass("sidebar")
        padding = Insets(12.0)

        label(Localization.string("app.title", "FMPS AutoTrader")) {
            styleClass += "sidebar-title"
        }
        separator(Orientation.HORIZONTAL)

        val navButtons = listOf(
            "dashboard" to "nav.dashboard",
            "traders" to "nav.traders",
            "monitoring" to "nav.monitoring",
            "configuration" to "nav.configuration",
            "patterns" to "nav.patterns"
        )

        navButtons.forEach { (route, key) ->
            val button = ToolbarButton(
                text = Localization.string(key),
                icon = "•",
                emphasis = ToolbarButton.Emphasis.SECONDARY
            ).apply {
                addClass("nav-button")
                action { viewModel.navigate(route) }
            }
            children += button
        }
        VBox.setVgrow(children.last(), Priority.ALWAYS)
    }

    private lateinit var connectionStatusIndicator: HBox
    
    private fun buildContent(): VBox = vbox(16.0) {
        padding = Insets(0.0, 0.0, 0.0, 4.0)

        hbox(10.0) {
            children += breadcrumbBar
            children += Separator(Orientation.VERTICAL)
            children += quickStatsContainer
            // Create connection status indicator inline
            connectionStatusIndicator = HBox(8.0).apply {
                addClass("connection-status")
                alignment = javafx.geometry.Pos.CENTER_LEFT
            }
            children += connectionStatusIndicator
            children += HBox().apply {
                hgrow = Priority.ALWAYS
            }
            children += backButton
        }

        separator(Orientation.HORIZONTAL)
        children += traderListContainer
        separator(Orientation.HORIZONTAL)
        children += contentHolder
    }

    private fun updateBreadcrumbs(state: ShellState) {
        breadcrumbBar.children.clear()
        if (state.breadcrumbs.isEmpty()) {
            breadcrumbBar.children += Label("Home").apply { addClass("breadcrumb-label") }
        } else {
            state.breadcrumbs.forEachIndexed { index, crumb ->
                if (index > 0) {
                    breadcrumbBar.children += Label("›").apply { addClass("breadcrumb-label") }
                }
                breadcrumbBar.children += Label(crumb).apply { addClass("breadcrumb-label") }
            }
        }
    }

    private fun updateQuickStats(state: ShellState) {
        quickStatsContainer.children.setAll(
            MetricTile(
                title = "Active Traders",
                value = state.traderSummaries.count { it.status == com.fmps.autotrader.desktop.services.TraderStatus.RUNNING }.toString()
            ),
            MetricTile(
                title = "Stopped",
                value = state.traderSummaries.count { it.status == com.fmps.autotrader.desktop.services.TraderStatus.STOPPED }.toString()
            ),
            MetricTile(
                title = "Last Updated",
                value = "${((System.currentTimeMillis() - state.lastUpdatedTimestamp) / 1000)}s ago"
            )
        )
    }

    private fun updateTraderList(traders: List<TraderSummary>) {
        traderListContainer.children.clear()
        traderListContainer.children += Label("Trader Overview").apply {
            styleClass += "section-title"
        }

        if (traders.isEmpty()) {
            traderListContainer.children += Label("No traders available.")
            return
        }

        traders.forEach { trader ->
            traderListContainer.children += hbox(12.0) {
                addClass("content-card")
                children += VBox().apply {
                    children += Label(trader.name).apply {
                        styleClass += "trader-name"
                    }
                    children += Label("${trader.exchange} • Positions: ${trader.positions}").apply {
                        styleClass += "trader-meta"
                    }
                }
                children += ToolbarButton("Open", icon = "→", emphasis = ToolbarButton.Emphasis.PRIMARY).apply {
                    action { viewModel.navigate("traders") }
                }
                children += StatusBadge(trader.status, showText = true)
                children += Label(String.format("%.2f USDT", trader.profitLoss)).apply {
                    styleClass += if (trader.profitLoss >= 0) "profit-positive" else "profit-negative"
                }
            }
        }
    }

    private fun placeholderView() = Label(Localization.string("placeholder.select_module")).apply {
        styleClass += "placeholder-label"
    }
    
    private fun updateConnectionStatus(state: ShellState) {
        if (!::connectionStatusIndicator.isInitialized) return
        connectionStatusIndicator.children.clear()
        
        val statusColor = when (state.connectionStatus) {
            com.fmps.autotrader.desktop.services.ConnectionStatus.CONNECTED -> "#4CAF50"
            com.fmps.autotrader.desktop.services.ConnectionStatus.DISCONNECTED -> "#F44336"
            com.fmps.autotrader.desktop.services.ConnectionStatus.RECONNECTING -> "#FF9800"
        }
        
        val statusText = when (state.connectionStatus) {
            com.fmps.autotrader.desktop.services.ConnectionStatus.CONNECTED -> "Core Service: Connected"
            com.fmps.autotrader.desktop.services.ConnectionStatus.DISCONNECTED -> "Core Service: Disconnected"
            com.fmps.autotrader.desktop.services.ConnectionStatus.RECONNECTING -> "Core Service: Connecting..."
        }
        
        // Status indicator circle
        val indicator = javafx.scene.shape.Circle(6.0).apply {
            fill = javafx.scene.paint.Color.web(statusColor)
        }
        
        val statusLabel = Label(statusText).apply {
            styleClass += "connection-status-label"
            style = "-fx-text-fill: $statusColor; -fx-font-size: 12px;"
        }
        
        connectionStatusIndicator.children.addAll(indicator, statusLabel)
        
        // Add help button if disconnected
        if (state.connectionStatus == com.fmps.autotrader.desktop.services.ConnectionStatus.DISCONNECTED) {
            val helpButton = ToolbarButton("?", icon = "?", emphasis = ToolbarButton.Emphasis.SECONDARY).apply {
                tooltip("Click for instructions on starting the core service")
                action { viewModel.showConnectionHelp() }
            }
            connectionStatusIndicator.children += helpButton
            
            // Show error message tooltip if available
            state.connectionErrorMessage?.let { errorMsg ->
                statusLabel.tooltip(errorMsg)
            }
        }
    }
}

