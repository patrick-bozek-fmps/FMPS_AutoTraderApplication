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
import com.fmps.autotrader.desktop.traders.TraderManagementViewModel
import tornadofx.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShellView : BaseView<ShellState, ShellEvent, ShellViewModel>(ShellViewModel::class) {

    private val navigationService: NavigationService = GlobalContext.get().get()

    private val breadcrumbBar = HBox().apply { addClass("breadcrumb-bar") }
    private val quickStatsContainer = HBox(12.0)
    private val traderListContainer = VBox(10.0)
    private val contentHolder = StackPane().apply { paddingAll = 12.0 }
    private val backButton = ToolbarButton("Back", "⟲")
    private val navButtonMap = mutableMapOf<String, ToolbarButton>()

    override val root: BorderPane = borderpane {
        addClass("app-shell")
        left = buildSidebar()
        center = buildContent()
    }
    
    override fun onDock() {
        super.onDock()
        // Apply theme class directly to ShellView root and nav-buttons for CSS selectors
        javafx.application.Platform.runLater {
            root.scene?.root?.styleClass?.let { rootClasses ->
                val themeClasses = rootClasses.filter { it.startsWith("theme-") }
                if (themeClasses.isNotEmpty()) {
                    val themeClass = themeClasses.first()
                    
                    // Only apply if it's actually a theme class (safety check)
                    if (themeClass == "theme-light" || themeClass == "theme-dark") {
                        // Remove any existing theme classes
                        root.styleClass.removeAll(listOf("theme-light", "theme-dark"))
                        // Add the theme class directly to app-shell
                        root.styleClass.add(themeClass)
                        println("🎨 ShellView: Added theme class to app-shell root: $themeClass")
                        
                        // Also try applying to sidebar directly
                        root.left?.let { sidebar ->
                            sidebar.styleClass.removeAll(listOf("theme-light", "theme-dark"))
                            sidebar.styleClass.add(themeClass)
                            println("🎨 ShellView: Added theme class to sidebar: $themeClass")
                        }
                        
                        // Apply theme class directly to all nav-buttons ONLY if light theme
                        // In dark theme, nav-buttons should NOT have theme-dark class (use default dark styles)
                        if (themeClass == "theme-light") {
                            navButtonMap.values.forEach { button ->
                                button.styleClass.removeAll(listOf("theme-light", "theme-dark"))
                                button.styleClass.add(themeClass)
                            }
                            println("🎨 ShellView: Added theme-light class to ${navButtonMap.size} nav-buttons")
                        } else {
                            // Dark theme: remove theme classes from nav-buttons to use default dark styles
                            navButtonMap.values.forEach { button ->
                                button.styleClass.removeAll(listOf("theme-light", "theme-dark"))
                            }
                            println("🎨 ShellView: Removed theme classes from ${navButtonMap.size} nav-buttons (dark mode)")
                        }
                    }
                }
            }
        }
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
        updateNavigationHighlight(state)
        backButton.isDisable = !state.canNavigateBack
    }
    
    private fun updateNavigationHighlight(state: ShellState) {
        if (navButtonMap.isEmpty()) return
        
        navButtonMap.forEach { (route, button) ->
            if (state.currentRoute == route) {
                if (!button.styleClass.contains("active")) {
                    button.styleClass.add("active")
                }
            } else {
                button.styleClass.remove("active")
            }
        }
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
            "dashboard" to ("nav.dashboard" to "📊"),  // Chart icon for Overview/Dashboard
            "traders" to ("nav.traders" to "🤖"),      // Robot icon for AI Traders
            "monitoring" to ("nav.monitoring" to "🔍"), // Search/monitor icon for Monitoring (different from analytics)
            "configuration" to ("nav.configuration" to "🔧"), // Settings/wrench icon for Configuration
            "patterns" to ("nav.patterns" to "📉")     // Analytics chart icon for Pattern Analytics
        )

        navButtons.forEach { (route, keyAndIcon) ->
            val (key, icon) = keyAndIcon
            val button = ToolbarButton(
                text = Localization.string(key),
                icon = icon,
                emphasis = ToolbarButton.Emphasis.SECONDARY
            ).apply {
                addClass("nav-button")
                action { viewModel.navigate(route) }
            }
            navButtonMap[route] = button
            children += button
        }
        
        VBox.setVgrow(children.last(), Priority.ALWAYS)
    }

    private lateinit var connectionStatusIndicator: VBox
    
    private fun buildContent(): VBox = vbox(16.0) {
        padding = Insets(0.0, 0.0, 0.0, 4.0)

        hbox(10.0) {
            children += breadcrumbBar
            children += Separator(Orientation.VERTICAL)
            children += quickStatsContainer
            // Create connection status indicator inline (vertical layout)
            connectionStatusIndicator = VBox(4.0).apply {
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
            traderListContainer.children += Label("No traders available.").apply {
                styleClass += "section-title"
                style = "-fx-font-size: 13px;"
            }
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
                    action { 
                        // Store trader ID to select - the view will check this when displayed
                        val traderId = trader.id
                        println("🔍 ShellView: Setting trader to select: $traderId")
                        com.fmps.autotrader.desktop.traders.TraderManagementView.setTraderToSelect(traderId)
                        viewModel.navigate("traders")
                        // Force selection immediately, even if already on traders view
                        javafx.application.Platform.runLater {
                            javafx.animation.PauseTransition(javafx.util.Duration.millis(200.0)).apply {
                                setOnFinished {
                                    try {
                                        val traderViewModel = GlobalContext.get().get<TraderManagementViewModel>()
                                        println("🔍 ShellView: Forcing selection of trader: $traderId")
                                        traderViewModel.selectTraderById(traderId)
                                        // Also trigger a state check in the view if it exists
                                        javafx.application.Platform.runLater {
                                            val currentPending = com.fmps.autotrader.desktop.traders.TraderManagementView.traderIdToSelect
                                            if (currentPending == traderId) {
                                                println("🔍 ShellView: Pending ID still set, view should pick it up")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        println("⚠️ ShellView: Could not select trader immediately: ${e.message}")
                                    }
                                }
                                play()
                            }
                        }
                    }
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
        
        // Core Service status
        val coreStatusColor = when (state.connectionStatus) {
            com.fmps.autotrader.desktop.services.ConnectionStatus.CONNECTED -> "#4CAF50"
            com.fmps.autotrader.desktop.services.ConnectionStatus.DISCONNECTED -> "#F44336"
            com.fmps.autotrader.desktop.services.ConnectionStatus.RECONNECTING -> "#FF9800"
        }
        
        val coreStatusText = when (state.connectionStatus) {
            com.fmps.autotrader.desktop.services.ConnectionStatus.CONNECTED -> "Core Service: Connected"
            com.fmps.autotrader.desktop.services.ConnectionStatus.DISCONNECTED -> "Core Service: Disconnected"
            com.fmps.autotrader.desktop.services.ConnectionStatus.RECONNECTING -> "Core Service: Connecting..."
        }
        
        val coreIndicator = javafx.scene.shape.Circle(6.0).apply {
            fill = javafx.scene.paint.Color.web(coreStatusColor)
        }
        
        val coreStatusLabel = Label(coreStatusText).apply {
            styleClass += "connection-status-label"
            style = "-fx-text-fill: $coreStatusColor; -fx-font-size: 12px;"
        }
        
        // Core Service row
        val coreRow = HBox(8.0).apply {
            alignment = javafx.geometry.Pos.CENTER_LEFT
            children.addAll(coreIndicator, coreStatusLabel)
        }
        connectionStatusIndicator.children.add(coreRow)
        
        // Binance status row
        val binanceColor = when (state.binanceConnected) {
            true -> "#4CAF50"
            false -> "#F44336"
            null -> "#9E9E9E" // Gray for not tested
        }
        val binanceText = when (state.binanceConnected) {
            true -> "Binance: Connected"
            false -> "Binance: Not Connected"
            null -> "Binance: Not tested"
        }
        val binanceIndicator = javafx.scene.shape.Circle(6.0).apply {
            fill = javafx.scene.paint.Color.web(binanceColor)
        }
        val binanceLabel = Label(binanceText).apply {
            styleClass += "connection-status-label"
            style = "-fx-text-fill: $binanceColor; -fx-font-size: 12px;"
        }
        val binanceRow = HBox(8.0).apply {
            alignment = javafx.geometry.Pos.CENTER_LEFT
            children.addAll(binanceIndicator, binanceLabel)
        }
        connectionStatusIndicator.children.add(binanceRow)
        
        // Bitget status row
        val bitgetColor = when (state.bitgetConnected) {
            true -> "#4CAF50"
            false -> "#F44336"
            null -> "#9E9E9E" // Gray for not tested
        }
        val bitgetText = when (state.bitgetConnected) {
            true -> "Bitget: Connected"
            false -> "Bitget: Not Connected"
            null -> "Bitget: Not tested"
        }
        val bitgetIndicator = javafx.scene.shape.Circle(6.0).apply {
            fill = javafx.scene.paint.Color.web(bitgetColor)
        }
        val bitgetLabel = Label(bitgetText).apply {
            styleClass += "connection-status-label"
            style = "-fx-text-fill: $bitgetColor; -fx-font-size: 12px;"
        }
        val bitgetRow = HBox(8.0).apply {
            alignment = javafx.geometry.Pos.CENTER_LEFT
            children.addAll(bitgetIndicator, bitgetLabel)
        }
        connectionStatusIndicator.children.add(bitgetRow)
        
        // Add help button if disconnected
        if (state.connectionStatus == com.fmps.autotrader.desktop.services.ConnectionStatus.DISCONNECTED) {
            val helpButton = ToolbarButton("?", icon = "?", emphasis = ToolbarButton.Emphasis.SECONDARY).apply {
                tooltip("Click for instructions on starting the core service")
                action { viewModel.showConnectionHelp() }
            }
            connectionStatusIndicator.children += helpButton
            
            // Show error message tooltip if available
            state.connectionErrorMessage?.let { errorMsg ->
                coreStatusLabel.tooltip(errorMsg)
            }
        }
    }
}

