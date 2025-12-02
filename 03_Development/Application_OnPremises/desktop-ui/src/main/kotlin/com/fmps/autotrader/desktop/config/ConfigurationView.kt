package com.fmps.autotrader.desktop.config

import com.fmps.autotrader.desktop.components.ToolbarButton
import com.fmps.autotrader.desktop.mvvm.BaseView
import com.fmps.autotrader.desktop.services.ConnectionStatus
import com.fmps.autotrader.desktop.services.Exchange
import com.fmps.autotrader.desktop.services.LoggingLevel
import com.fmps.autotrader.desktop.services.ThemePreference
import com.fmps.autotrader.shared.model.TradingStrategy
import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.TabPane
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.Node
import javafx.scene.control.TextFormatter
import javafx.scene.layout.Pane
import javafx.application.Platform
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import tornadofx.action
import tornadofx.addClass
import tornadofx.borderpane
import tornadofx.hbox
import tornadofx.information
import tornadofx.label
import tornadofx.tab
import tornadofx.tabpane
import tornadofx.textarea
import tornadofx.textfield
import tornadofx.vbox
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class ConfigurationView :
    BaseView<ConfigurationState, ConfigurationEvent, ConfigurationViewModel>(ConfigurationViewModel::class) {

    private val exchangeApiKey = PasswordField()
    private val exchangeSecret = PasswordField()
    private val exchangePassphrase = PasswordField()
    private val exchangeSelect = ComboBox<Exchange>()
    private val exchangeErrors = Label()
    
    // Field containers for visibility control
    private lateinit var apiKeyFieldContainer: Node
    private lateinit var secretKeyFieldContainer: Node
    private lateinit var passphraseFieldContainer: Node

    private val updateIntervalField = TextField()
    private val telemetryField = TextField()
    private val loggingSelect = ComboBox<LoggingLevel>()
    private val themeSelect = ComboBox<ThemePreference>()
    private val generalErrors = Label()

    private val budgetField = TextField()
    private val leverageField = TextField()
    private val stopLossField = TextField()
    private val takeProfitField = TextField()
    private val strategyField = ComboBox<String>()
    private val traderErrors = Label()
    
    /**
     * Maps TradingStrategy enum names to user-friendly display names
     */
    private fun getStrategyDisplayName(enumName: String): String {
        return when (enumName) {
            "TREND_FOLLOWING" -> "Trend Following"
            "MEAN_REVERSION" -> "Mean Reversion"
            "BREAKOUT" -> "Breakout"
            else -> enumName
        }
    }
    
    /**
     * Maps display name back to TradingStrategy enum name
     */
    private fun getStrategyEnumName(displayName: String): String {
        return when (displayName) {
            "Trend Following" -> "TREND_FOLLOWING"
            "Mean Reversion" -> "MEAN_REVERSION"
            "Breakout" -> "BREAKOUT"
            else -> displayName
        }
    }

    private val exportArea = TextArea()
    private val importArea = TextArea()
    private val importStatus = Label()
    private val connectionLabel = Label()
    private val savedOnLabel = Label() // Shows when the selected exchange was last saved
    private val connectionStatusLabel = Label() // Shows connection status for the selected exchange

    private val isExchangeUpdating = SimpleBooleanProperty(false)
    private lateinit var saveExchangeButton: ToolbarButton // Reference to Save Exchange button for style updates

    // Helper to safely add a node, removing it from old parent first
    private fun Node.safeAddTo(parent: Pane) {
        // Always remove from current parent first (if any)
        this.parent?.let { currentParent ->
            (currentParent as? Pane)?.children?.remove(this)
        }
        // Also remove from target parent if already there (defensive)
        if (parent.children.contains(this)) {
            parent.children.remove(this)
        }
        // Now safely add
        parent.children += this
    }

    override val root = borderpane {
        padding = Insets(12.0)
        top = buildHeader()
        center = buildTabs()
    }

    private fun buildHeader() = hbox(12.0) {
        addClass("dashboard-header")
        alignment = Pos.CENTER_LEFT
        padding = Insets(4.0, 4.0, 8.0, 4.0)

        label("Configuration Management") {
            addClass("view-header")
        }
        label("|") {
            addClass("view-header")
            style = "-fx-padding: 0 8 0 8;"
        }
        label("Configure exchange connections, trading defaults, and system settings.") {
            addClass("view-description")
        }
    }

    private fun buildTabs(): TabPane = tabpane {
        tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
        tab("Exchange") { content = buildExchangeTab() }
        tab("General") { content = buildGeneralTab() }
        tab("Trader Defaults") { content = buildTraderTab() }
        tab("Import / Export") { content = buildImportExportTab() }
    }

    private fun buildExchangeTab(): VBox = vbox(12.0) {
        padding = Insets(16.0)
        // Only add exchange values (no null option)
        exchangeSelect.items.addAll(Exchange.values())
        // Set initial selection to null (no selection) - this will be updated when state loads
        exchangeSelect.selectionModel.select(null)
        exchangeSelect.selectionModel.selectedItemProperty().addListener { _, old, new ->
            println("🔍 Exchange selection changed in UI: $old -> $new")
            new?.let { selectedExchange ->
                if (old != new) {
                    // Clear fields first when switching exchanges
                    println("🔍 Clearing password fields (switching from $old to $new)")
                    exchangeApiKey.clear()
                    exchangeSecret.clear()
                    exchangePassphrase.clear()
                    
                    // Get the saved configuration from the cache (not from ViewModel state which changes)
                    // We need to check what's actually saved, not what's in the current form state
                    var savedSettings = viewModel.getSavedExchangeSettings(selectedExchange)
                    if (savedSettings != null && (savedSettings.apiKey.isNotEmpty() || savedSettings.secretKey.isNotEmpty())) {
                        println("🔍 Loading saved values for exchange: $selectedExchange (found saved settings: apiKey length=${savedSettings.apiKey.length}, secretKey length=${savedSettings.secretKey.length})")
                        // Set password fields to saved values (they will be masked)
                        exchangeApiKey.text = savedSettings.apiKey
                        exchangeSecret.text = savedSettings.secretKey
                        if (selectedExchange == Exchange.BITGET) {
                            exchangePassphrase.text = savedSettings.passphrase
                        }
                        // Update ViewModel with the loaded values
                        // Note: updateExchangeForm will automatically check for changes and set hasUnsavedExchangeChanges
                        // Since we're loading saved values that match, checkHasUnsavedChanges should return false
                        viewModel.updateExchangeForm { form -> 
                            form.copy(
                                exchange = selectedExchange,
                                apiKey = savedSettings.apiKey,
                                secretKey = savedSettings.secretKey,
                                passphrase = if (selectedExchange == Exchange.BITGET) savedSettings.passphrase else ""
                            )
                        }
                        // Update status labels immediately after loading saved values
                        Platform.runLater {
                            updateExchangeStatus(selectedExchange, viewModel.state.value)
                        }
                    } else {
                        println("🔍 No saved values found for exchange: $selectedExchange on first check, will retry after delay")
                        // Update ViewModel with empty values for this exchange
                        viewModel.updateExchangeForm { form -> 
                            form.copy(
                                exchange = selectedExchange,
                                apiKey = "",
                                secretKey = "",
                                passphrase = ""
                            )
                        }
                        // Retry after a short delay in case service is still loading
                        javafx.animation.PauseTransition(javafx.util.Duration.millis(300.0)).apply {
                            setOnFinished {
                                val retrySettings = viewModel.getSavedExchangeSettings(selectedExchange)
                                if (retrySettings != null && (retrySettings.apiKey.isNotEmpty() || retrySettings.secretKey.isNotEmpty())) {
                                    println("🔍 Loading saved values for exchange: $selectedExchange (found on retry: apiKey length=${retrySettings.apiKey.length}, secretKey length=${retrySettings.secretKey.length})")
                                    exchangeApiKey.text = retrySettings.apiKey
                                    exchangeSecret.text = retrySettings.secretKey
                                    if (selectedExchange == Exchange.BITGET) {
                                        exchangePassphrase.text = retrySettings.passphrase
                                    }
                                    viewModel.updateExchangeForm { form -> 
                                        form.copy(
                                            exchange = selectedExchange,
                                            apiKey = retrySettings.apiKey,
                                            secretKey = retrySettings.secretKey,
                                            passphrase = if (selectedExchange == Exchange.BITGET) retrySettings.passphrase else ""
                                        )
                                    }
                                    // Update status after loading saved values
                                    Platform.runLater {
                                        updateExchangeStatus(selectedExchange, viewModel.state.value)
                                    }
                                }
                            }
                            play()
                        }
                    }
                } else {
                    // Same exchange selected, just update the exchange in state
                    viewModel.updateExchangeForm { form -> 
                        form.copy(exchange = selectedExchange)
                    }
                }
            } ?: run {
                // new is null - clear fields
                println("🔍 Exchange selection is null, clearing fields")
                exchangeApiKey.clear()
                exchangeSecret.clear()
                exchangePassphrase.clear()
            }
            // Update field visibility based on selection
            updateFieldVisibility(new)
            
            // Always update status labels for the selected exchange when selection changes
            if (new != null) {
                Platform.runLater {
                    updateExchangeStatus(new, viewModel.state.value)
                }
            } else {
                Platform.runLater {
                    updateExchangeStatus(null, viewModel.state.value)
                }
            }
        }
        children += labeledField("Exchange", exchangeSelect)
        
        // Explicitly call onDock to ensure state collection starts
        // This is needed because onDock() might not be called automatically for views in TabPane
        Platform.runLater {
            if (!isDocked) {
                println("🔍 ConfigurationView: Manually calling onDock() to start state collection")
                onDock()
            }
        }

        // API Key field
        exchangeApiKey.promptText = "API Key"
        exchangeApiKey.textProperty().addListener { _, _, new ->
            viewModel.updateExchangeForm { it.copy(apiKey = new.orEmpty()) }
        }
        apiKeyFieldContainer = labeledField("API Key", exchangeApiKey)
        children += apiKeyFieldContainer

        // Secret Key field
        exchangeSecret.promptText = "Secret Key"
        exchangeSecret.textProperty().addListener { _, _, new ->
            viewModel.updateExchangeForm { it.copy(secretKey = new.orEmpty()) }
        }
        secretKeyFieldContainer = labeledField("Secret Key", exchangeSecret)
        children += secretKeyFieldContainer

        // Passphrase field (Bitget only)
        exchangePassphrase.promptText = "Passphrase (Bitget only)"
        exchangePassphrase.textProperty().addListener { _, _, new ->
            viewModel.updateExchangeForm { it.copy(passphrase = new.orEmpty()) }
        }
        passphraseFieldContainer = labeledField("Passphrase", exchangePassphrase)
        children += passphraseFieldContainer
        
        // Initially set field visibility and status based on current selection (if any)
        // This will be called after fields are initialized
        Platform.runLater {
            val initialSelection = exchangeSelect.selectionModel.selectedItem
            updateFieldVisibility(initialSelection)
            if (initialSelection != null) {
                updateExchangeStatus(initialSelection, viewModel.state.value)
            }
        }

        exchangeErrors.styleClass += "validation-label"
        exchangeErrors.safeAddTo(this)
        
        // Status information for selected exchange
        vbox(8.0) {
            padding = Insets(8.0, 0.0, 0.0, 0.0)
            // Saved on timestamp
            savedOnLabel.styleClass += "meta-muted"
            savedOnLabel.text = "Saved on: Never"
            children += savedOnLabel
            
            // Connection status
            connectionStatusLabel.styleClass += "status-badge"
            connectionStatusLabel.text = "Connection Status: Not tested"
            connectionStatusLabel.styleClass.removeAll(listOf("status-success", "status-error", "status-idle"))
            connectionStatusLabel.styleClass += "status-error" // Red for "Not tested"
            children += connectionStatusLabel
        }

        hbox(10.0) {
            alignment = Pos.CENTER_RIGHT
            padding = Insets(8.0, 0.0, 0.0, 0.0)
            children += ToolbarButton("Test Connection", icon = "🧪").apply {
                disableProperty().bind(isExchangeUpdating)
                action { 
                    println("🔍 Configuration: Test Connection button clicked!")
                    logger.info { "🔍 Configuration: Test Connection button clicked!" }
                    logger.info { "🔍 Configuration: Button enabled=${!isExchangeUpdating.value}, isExchangeUpdating=${isExchangeUpdating.value}" }
                    // Ensure current field values are saved to ViewModel before testing
                    val currentExchange = exchangeSelect.selectionModel.selectedItem
                    if (currentExchange != null) {
                        // Update ViewModel with current field values
                        viewModel.updateExchangeForm { form ->
                            form.copy(
                                exchange = currentExchange,
                                apiKey = exchangeApiKey.text,
                                secretKey = exchangeSecret.text,
                                passphrase = if (currentExchange == Exchange.BITGET) exchangePassphrase.text else form.passphrase
                            )
                        }
                        logger.info { "🔍 Configuration: Current exchange=${viewModel.state.value.exchangeForm.exchange}, apiKey length=${exchangeApiKey.text.length}, secretKey length=${exchangeSecret.text.length}" }
                        try {
                            logger.info { "🔍 Configuration: Calling viewModel.testConnection()..." }
                            viewModel.testConnection()
                            logger.info { "🔍 Configuration: viewModel.testConnection() call completed" }
                        } catch (e: Exception) {
                            logger.error(e) { "❌ Configuration: Error calling testConnection(): ${e.message}" }
                        }
                    } else {
                        logger.warn { "⚠️ Configuration: No exchange selected, cannot test connection" }
                    }
                }
            }
            saveExchangeButton = ToolbarButton("Save Exchange", icon = "💾", emphasis = ToolbarButton.Emphasis.PRIMARY).apply {
                disableProperty().bind(isExchangeUpdating)
                action { 
                    // Use both println (for immediate visibility) and logger (for proper logging)
                    println("🔍 Configuration: Save Exchange button clicked!")
                    logger.info { "🔍 Configuration: Save Exchange button clicked!" }
                    val currentExchange = exchangeSelect.selectionModel.selectedItem
                    if (currentExchange != null) {
                        // Update ViewModel with current field values before saving
                        viewModel.updateExchangeForm { form ->
                            form.copy(
                                exchange = currentExchange,
                                apiKey = exchangeApiKey.text,
                                secretKey = exchangeSecret.text,
                                passphrase = if (currentExchange == Exchange.BITGET) exchangePassphrase.text else form.passphrase
                            )
                        }
                        println("🔍 Configuration: Updated ViewModel with current values before save")
                        logger.info { "🔍 Configuration: Updated ViewModel with current values before save" }
                        viewModel.saveExchangeSettings()
                        println("🔍 Configuration: Called viewModel.saveExchangeSettings()")
                        logger.info { "🔍 Configuration: Called viewModel.saveExchangeSettings()" }
                    } else {
                        println("⚠️ Configuration: No exchange selected, cannot save")
                    }
                }
            }
            children += saveExchangeButton
        }
    }

    private fun buildGeneralTab(): VBox = vbox(12.0) {
        padding = Insets(16.0)
        updateIntervalField.textFormatter = numericFormatter()
        updateIntervalField.textProperty().addListener { _, _, new ->
            viewModel.updateGeneralForm { it.copy(updateIntervalSeconds = new?.toIntOrNull() ?: 0) }
        }
        children += labeledField("Update Interval (seconds)", updateIntervalField)

        telemetryField.textFormatter = numericFormatter()
        telemetryField.textProperty().addListener { _, _, new ->
            viewModel.updateGeneralForm { it.copy(telemetryPollingSeconds = new?.toIntOrNull() ?: 0) }
        }
        children += labeledField("Telemetry Polling (seconds)", telemetryField)

        loggingSelect.items.addAll(LoggingLevel.values())
        loggingSelect.selectionModel.selectedItemProperty().addListener { _, _, new ->
            new?.let { viewModel.updateGeneralForm { form -> form.copy(loggingLevel = it) } }
        }
        children += labeledField("Logging Level", loggingSelect)

        themeSelect.items.addAll(ThemePreference.values())
        themeSelect.selectionModel.selectedItemProperty().addListener { _, _, new ->
            new?.let { 
                viewModel.updateGeneralForm { form -> form.copy(theme = it) }
                // Theme will be auto-applied by updateGeneralForm which triggers auto-save
                // No need to apply here - it's handled in ViewModel
            }
        }
        children += labeledField("Theme Preference", themeSelect)

        generalErrors.styleClass += "validation-label"
        children += generalErrors

        // Save General button removed - changes are auto-saved
    }

    private fun buildTraderTab(): VBox = vbox(12.0) {
        padding = Insets(16.0)
        budgetField.textFormatter = decimalFormatter()
        budgetField.textProperty().addListener { _, _, new ->
            viewModel.updateTraderDefaults { it.copy(budgetUsd = new?.toDoubleOrNull() ?: 0.0) }
        }
        children += labeledField("Default Budget (USDT)", budgetField)

        leverageField.textFormatter = numericFormatter()
        leverageField.textProperty().addListener { _, _, new ->
            viewModel.updateTraderDefaults { it.copy(leverage = new?.toIntOrNull() ?: 0) }
        }
        children += labeledField("Leverage", leverageField)

        stopLossField.textFormatter = decimalFormatter()
        stopLossField.textProperty().addListener { _, _, new ->
            viewModel.updateTraderDefaults { it.copy(stopLossPercent = new?.toDoubleOrNull() ?: 0.0) }
        }
        children += labeledField("Stop Loss (%)", stopLossField)

        takeProfitField.textFormatter = decimalFormatter()
        takeProfitField.textProperty().addListener { _, _, new ->
            viewModel.updateTraderDefaults { it.copy(takeProfitPercent = new?.toDoubleOrNull() ?: 0.0) }
        }
        children += labeledField("Take Profit (%)", takeProfitField)

        // Populate strategy ComboBox with TradingStrategy enum values
        strategyField.items.setAll(TradingStrategy.values().map { getStrategyDisplayName(it.name) })
        // Set default selection to "Trend Following" if no selection exists
        val trendFollowingDisplay = getStrategyDisplayName(TradingStrategy.TREND_FOLLOWING.name)
        if (strategyField.selectionModel.selectedItem == null) {
            strategyField.selectionModel.select(trendFollowingDisplay)
            // Also update ViewModel state to ensure validation passes
            viewModel.updateTraderDefaults { it.copy(strategy = TradingStrategy.TREND_FOLLOWING.name) }
        }
        strategyField.selectionModel.selectedItemProperty().addListener { _, _, new ->
            if (new != null) {
                val enumName = getStrategyEnumName(new)
                viewModel.updateTraderDefaults { it.copy(strategy = enumName) }
            }
        }
        children += labeledField("Default Strategy", strategyField)

        traderErrors.styleClass += "validation-label"
        children += traderErrors

        // Save Trader Defaults button removed - changes are auto-saved
    }

    private fun buildImportExportTab(): VBox = vbox(12.0) {
        padding = Insets(16.0)
        exportArea.isEditable = false
        VBox.setVgrow(exportArea, Priority.ALWAYS)
        VBox.setVgrow(importArea, Priority.ALWAYS)

        children += Label("Export Snapshot").apply { styleClass += "section-title" }
        exportArea.safeAddTo(this)
        children += ToolbarButton("Export Configuration", icon = "⬇").apply {
            action { 
                println("🔍 Configuration: Export Configuration button clicked!")
                try {
                    println("🔍 Attempting to call viewModel.exportConfiguration()...")
                    viewModel.exportConfiguration()
                    println("✅ viewModel.exportConfiguration() called successfully")
                } catch (e: Exception) {
                    println("❌ Exception calling viewModel.exportConfiguration(): ${e.javaClass.simpleName} - ${e.message}")
                    e.printStackTrace()
                }
            }
        }

        children += Region().apply { prefHeight = 10.0 }
        children += Label("Import Configuration").apply { styleClass += "section-title" }
        importArea.promptText = "Paste configuration text..."
        importArea.safeAddTo(this)
        children += ToolbarButton("Import Configuration", icon = "⬆").apply {
            action { 
                println("🔍 Configuration: Import Configuration button clicked! Text length: ${importArea.text.length}")
                try {
                    println("🔍 Attempting to call viewModel.importConfiguration()...")
                    viewModel.importConfiguration(importArea.text)
                    println("✅ viewModel.importConfiguration() called successfully")
                } catch (e: Exception) {
                    println("❌ Exception calling viewModel.importConfiguration(): ${e.javaClass.simpleName} - ${e.message}")
                    e.printStackTrace()
                }
            }
        }
        importStatus.styleClass += "validation-label"
        importStatus.safeAddTo(this)
    }

    private fun labeledField(labelText: String, node: javafx.scene.Node) = vbox(4.0) {
        label(labelText) { styleClass += "field-label" }
        children += node
    }

    private fun numericFormatter() = TextFormatter<String> { change ->
        if (change.controlNewText.matches(Regex("^\\d*\$"))) change else null
    }

    private fun decimalFormatter() = TextFormatter<String> { change ->
        if (change.controlNewText.matches(Regex("^\\d*(\\.\\d{0,2})?\$"))) change else null
    }
    
    private fun updateFieldVisibility(selectedExchange: Exchange?) {
        println("🔍 updateFieldVisibility called: exchange=$selectedExchange")
        if (!::apiKeyFieldContainer.isInitialized || !::secretKeyFieldContainer.isInitialized || !::passphraseFieldContainer.isInitialized) {
            println("⚠️ Field containers not initialized yet")
            return
        }
        
        when (selectedExchange) {
            Exchange.BINANCE -> {
                println("🔄 Showing fields for BINANCE: API Key, Secret Key")
                apiKeyFieldContainer.isVisible = true
                apiKeyFieldContainer.isManaged = true
                secretKeyFieldContainer.isVisible = true
                secretKeyFieldContainer.isManaged = true
                // Keep passphrase in layout but hidden to prevent layout shifts
                passphraseFieldContainer.isVisible = false
                passphraseFieldContainer.isManaged = true // Keep in layout
                (passphraseFieldContainer as? javafx.scene.layout.Region)?.prefHeight = 0.0
            }
            Exchange.BITGET -> {
                println("🔄 Showing fields for BITGET: API Key, Secret Key, Passphrase")
                apiKeyFieldContainer.isVisible = true
                apiKeyFieldContainer.isManaged = true
                secretKeyFieldContainer.isVisible = true
                secretKeyFieldContainer.isManaged = true
                passphraseFieldContainer.isVisible = true
                passphraseFieldContainer.isManaged = true
                (passphraseFieldContainer as? javafx.scene.layout.Region)?.prefHeight = javafx.scene.layout.Region.USE_COMPUTED_SIZE
            }
            null -> {
                println("🔄 Hiding all fields (no selection)")
                apiKeyFieldContainer.isVisible = false
                apiKeyFieldContainer.isManaged = false
                secretKeyFieldContainer.isVisible = false
                secretKeyFieldContainer.isManaged = false
                passphraseFieldContainer.isVisible = false
                passphraseFieldContainer.isManaged = false
            }
            else -> {
                println("🔄 Unknown exchange, hiding all fields")
                apiKeyFieldContainer.isVisible = false
                apiKeyFieldContainer.isManaged = false
                secretKeyFieldContainer.isVisible = false
                secretKeyFieldContainer.isManaged = false
                passphraseFieldContainer.isVisible = false
                passphraseFieldContainer.isManaged = false
            }
        }
    }

    override fun onStateChanged(state: ConfigurationState) {
        println("🔍 ConfigurationView.onStateChanged() called: exchange=${state.exchangeForm.exchange}, isTestingConnection=${state.isTestingConnection}, connectionTest=${state.connectionTest?.success}, isSaving=${state.isSaving}")
        // Only update exchange selection if it's different from current selection to avoid overwriting user's selection
        if (exchangeSelect.selectionModel.selectedItem != state.exchangeForm.exchange) {
            println("🔍 Updating exchange selection from ${exchangeSelect.selectionModel.selectedItem} to ${state.exchangeForm.exchange}")
            exchangeSelect.selectionModel.select(state.exchangeForm.exchange)
        }
        // For password fields, we only update from state when:
        // 1. Exchange selection changes (handled by listener)
        // 2. State is loaded from saved configuration (only if fields are empty)
        // Otherwise, we preserve user input while typing
        // The state is updated from the field's textProperty listener, not the other way around
        // Only load saved values if fields are currently empty (to avoid overwriting user input)
        if (exchangeApiKey.text.isEmpty() && state.exchangeForm.apiKey.isNotEmpty() && 
            exchangeSelect.selectionModel.selectedItem == state.exchangeForm.exchange) {
            println("🔍 Loading saved API key (field is empty)")
            exchangeApiKey.text = state.exchangeForm.apiKey
        }
        if (exchangeSecret.text.isEmpty() && state.exchangeForm.secretKey.isNotEmpty() && 
            exchangeSelect.selectionModel.selectedItem == state.exchangeForm.exchange) {
            println("🔍 Loading saved secret key (field is empty)")
            exchangeSecret.text = state.exchangeForm.secretKey
        }
        if (exchangePassphrase.text.isEmpty() && state.exchangeForm.passphrase.isNotEmpty() && 
            exchangeSelect.selectionModel.selectedItem == Exchange.BITGET) {
            println("🔍 Loading saved passphrase (field is empty)")
            exchangePassphrase.text = state.exchangeForm.passphrase
        }
        isExchangeUpdating.set(state.isTestingConnection || state.isSaving)
        
        // Update Save Exchange button style based on unsaved changes
        if (::saveExchangeButton.isInitialized) {
            saveExchangeButton.styleClass.removeAll(listOf("toolbar-button-primary-dirty", "toolbar-button-primary-saved"))
            if (state.hasUnsavedExchangeChanges) {
                // Light blue for unsaved changes (needs saving)
                saveExchangeButton.styleClass += "toolbar-button-primary-dirty"
            } else {
                // Grey for saved state (no saving needed)
                saveExchangeButton.styleClass += "toolbar-button-primary-saved"
            }
        }
        
        // Always update status labels based on currently selected exchange
        val currentExchange = exchangeSelect.selectionModel.selectedItem ?: state.exchangeForm.exchange
        println("🔍 onStateChanged: Updating status for exchange=$currentExchange, isTestingConnection=${state.isTestingConnection}, isSaving=${state.isSaving}, lastSavedTimestamp=${state.lastSavedTimestamp}, exchangeConnectionStatus=${state.exchangeConnectionStatus}")
        logger.info { "🔍 onStateChanged: Updating status for exchange=$currentExchange, isTestingConnection=${state.isTestingConnection}, isSaving=${state.isSaving}, lastSavedTimestamp=${state.lastSavedTimestamp}, exchangeConnectionStatus=${state.exchangeConnectionStatus}" }
        updateExchangeStatus(currentExchange, state)

        if (updateIntervalField.text != state.generalForm.updateIntervalSeconds.toString()) {
            updateIntervalField.text = state.generalForm.updateIntervalSeconds.toString()
        }
        if (telemetryField.text != state.generalForm.telemetryPollingSeconds.toString()) {
            telemetryField.text = state.generalForm.telemetryPollingSeconds.toString()
        }
        loggingSelect.selectionModel.select(state.generalForm.loggingLevel)
        themeSelect.selectionModel.select(state.generalForm.theme)

        if (budgetField.text != state.traderDefaultsForm.budgetUsd.toString()) {
            budgetField.text = state.traderDefaultsForm.budgetUsd.toString()
        }
        if (leverageField.text != state.traderDefaultsForm.leverage.toString()) {
            leverageField.text = state.traderDefaultsForm.leverage.toString()
        }
        if (stopLossField.text != state.traderDefaultsForm.stopLossPercent.toString()) {
            stopLossField.text = state.traderDefaultsForm.stopLossPercent.toString()
        }
        if (takeProfitField.text != state.traderDefaultsForm.takeProfitPercent.toString()) {
            takeProfitField.text = state.traderDefaultsForm.takeProfitPercent.toString()
        }
        // Update strategy ComboBox selection
        val currentStrategyDisplay = strategyField.selectionModel.selectedItem
        val expectedStrategyDisplay = getStrategyDisplayName(state.traderDefaultsForm.strategy)
        if (currentStrategyDisplay != expectedStrategyDisplay) {
            // Try to find the matching item in the ComboBox
            val matchingItem = strategyField.items.find { it == expectedStrategyDisplay }
            if (matchingItem != null) {
                strategyField.selectionModel.select(matchingItem)
            } else {
                // If not found, try to parse as enum name and convert to display name
                try {
                    val enumValue = TradingStrategy.valueOf(state.traderDefaultsForm.strategy)
                    val displayName = getStrategyDisplayName(enumValue.name)
                    val item = strategyField.items.find { it == displayName }
                    if (item != null) {
                        strategyField.selectionModel.select(item)
                    } else {
                        // Fallback: select "Trend Following" if no valid selection
                        val trendFollowingDisplay = getStrategyDisplayName(TradingStrategy.TREND_FOLLOWING.name)
                        val trendFollowingItem = strategyField.items.find { it == trendFollowingDisplay }
                        if (trendFollowingItem != null) {
                            strategyField.selectionModel.select(trendFollowingItem)
                            // Update ViewModel state to ensure validation passes
                            viewModel.updateTraderDefaults { it.copy(strategy = TradingStrategy.TREND_FOLLOWING.name) }
                        }
                    }
                } catch (e: IllegalArgumentException) {
                    // Invalid enum name, default to "Trend Following"
                    logger.warn { "Invalid strategy value: ${state.traderDefaultsForm.strategy}, defaulting to Trend Following" }
                    val trendFollowingDisplay = getStrategyDisplayName(TradingStrategy.TREND_FOLLOWING.name)
                    val trendFollowingItem = strategyField.items.find { it == trendFollowingDisplay }
                    if (trendFollowingItem != null) {
                        strategyField.selectionModel.select(trendFollowingItem)
                        // Update ViewModel state to ensure validation passes
                        viewModel.updateTraderDefaults { it.copy(strategy = TradingStrategy.TREND_FOLLOWING.name) }
                    }
                }
            }
        } else if (currentStrategyDisplay == null) {
            // If no selection exists, default to "Trend Following"
            val trendFollowingDisplay = getStrategyDisplayName(TradingStrategy.TREND_FOLLOWING.name)
            val trendFollowingItem = strategyField.items.find { it == trendFollowingDisplay }
            if (trendFollowingItem != null) {
                strategyField.selectionModel.select(trendFollowingItem)
                // Update ViewModel state to ensure validation passes
                viewModel.updateTraderDefaults { it.copy(strategy = TradingStrategy.TREND_FOLLOWING.name) }
            }
        }

        exchangeErrors.text = state.validationErrors["apiKey"]
            ?.let { "API Key: $it" }
            ?: state.validationErrors["secretKey"]?.let { "Secret Key: $it" }
            ?: state.validationErrors["passphrase"]

        generalErrors.text = state.validationErrors["updateInterval"]
            ?: state.validationErrors["telemetryPolling"]

        traderErrors.text = state.validationErrors["budget"]
            ?: state.validationErrors["leverage"]
            ?: state.validationErrors["stopLoss"]
            ?: state.validationErrors["takeProfit"]
            ?: state.validationErrors["strategy"]

        state.exportContent?.let {
            if (exportArea.text != it) {
                exportArea.text = it
            }
        }
        if (state.importSuccess) {
            importStatus.text = "Import successful"
        } else if (state.importError != null) {
            importStatus.text = state.importError
        }
    }

    private fun updateExchangeStatus(exchange: Exchange?, state: ConfigurationState) {
        // Note: This is already called from onStateChanged which is on JavaFX thread
        logger.info { "🔍 updateExchangeStatus called: exchange=$exchange, isTestingConnection=${state.isTestingConnection}, selectedExchange=${exchangeSelect.selectionModel.selectedItem}" }
        if (exchange == null) {
            savedOnLabel.text = "Saved on: Never"
            connectionStatusLabel.text = "Connection Status: Not tested"
            connectionStatusLabel.styleClass.removeAll(listOf("status-success", "status-error", "status-idle"))
            connectionStatusLabel.styleClass += "status-error" // Red
            return
        }
        
        // Update "Saved on" timestamp
        val savedTimestamp = state.lastSavedTimestamp[exchange]
        println("🔍 Saved timestamp for $exchange: $savedTimestamp (map keys: ${state.lastSavedTimestamp.keys})")
        logger.info { "🔍 Saved timestamp for $exchange: $savedTimestamp (map keys: ${state.lastSavedTimestamp.keys})" }
        if (savedTimestamp != null) {
            val dateTime = java.time.Instant.ofEpochMilli(savedTimestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            savedOnLabel.text = "Saved on: $dateTime"
            logger.info { "🔍 Updated savedOnLabel to: Saved on: $dateTime" }
        } else {
            savedOnLabel.text = "Saved on: Never"
            logger.info { "🔍 No saved timestamp, setting to Never" }
        }
        
        // Update connection status
        val connectionStatus = state.exchangeConnectionStatus[exchange]
        val selectedExchange = exchangeSelect.selectionModel.selectedItem
        logger.info { "🔍 Connection status for $exchange: $connectionStatus (map keys: ${state.exchangeConnectionStatus.keys}), isTestingConnection=${state.isTestingConnection}, selectedExchange=$selectedExchange" }
        when {
            state.isTestingConnection && selectedExchange == exchange -> {
                // Testing in progress for this exchange
                logger.info { "🔍 Setting connection status to Testing..." }
                connectionStatusLabel.text = "Connection Status: Testing..."
                connectionStatusLabel.styleClass.removeAll(listOf("status-success", "status-error", "status-idle"))
                connectionStatusLabel.styleClass += "status-idle" // Amber/orange
            }
            connectionStatus == true -> {
                // Connected
                logger.info { "🔍 Setting connection status to Connected" }
                connectionStatusLabel.text = "Connection Status: Connected"
                connectionStatusLabel.styleClass.removeAll(listOf("status-success", "status-error", "status-idle"))
                connectionStatusLabel.styleClass += "status-success" // Green
            }
            connectionStatus == false -> {
                // Not connected
                logger.info { "🔍 Setting connection status to Not Connected" }
                connectionStatusLabel.text = "Connection Status: Not Connected"
                connectionStatusLabel.styleClass.removeAll(listOf("status-success", "status-error", "status-idle"))
                connectionStatusLabel.styleClass += "status-error" // Red
            }
            else -> {
                // Not tested
                logger.info { "🔍 Setting connection status to Not tested" }
                connectionStatusLabel.text = "Connection Status: Not tested"
                connectionStatusLabel.styleClass.removeAll(listOf("status-success", "status-error", "status-idle"))
                connectionStatusLabel.styleClass += "status-error" // Red
            }
        }
    }

    override fun onEvent(event: ConfigurationEvent) {
        println("🔍 ConfigurationView.onEvent() called with: ${event.javaClass.simpleName}")
        // Note: We're already on JavaFX thread (BaseView uses withContext(Dispatchers.Main))
        when (event) {
            is ConfigurationEvent.ShowMessage -> {
                println("🔍 ShowMessage event: level=${event.level}, message='${event.message}'")
                when (event.level) {
                    ConfigurationEvent.MessageLevel.INFO -> {
                        println("🔄 Showing info dialog: '${event.message}'")
                        information(event.message)
                        println("✅ Info dialog shown")
                    }
                    ConfigurationEvent.MessageLevel.SUCCESS -> {
                        println("🔄 Showing success dialog: '${event.message}'")
                        information(event.message)
                        println("✅ Success dialog shown")
                        // Update status for current exchange after save
                        updateExchangeStatus(viewModel.state.value.exchangeForm.exchange, viewModel.state.value)
                    }
                    ConfigurationEvent.MessageLevel.ERROR -> {
                        val errorMessage = event.message ?: "An error occurred"
                        println("🔄 Showing error dialog: '$errorMessage'")
                        try {
                            error(errorMessage)
                            println("✅ Error dialog shown")
                        } catch (e: Exception) {
                            logger.error(e) { "Failed to show error dialog: ${e.message}" }
                            // Fallback: show as information dialog if error() fails
                            information(errorMessage)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Applies the theme preference to the JavaFX application.
     * This should be called on the JavaFX Application Thread.
     */
    private fun applyThemePreference(theme: ThemePreference) {
        try {
            val actualTheme = when (theme) {
                ThemePreference.AUTO -> {
                    // Detect system theme preference
                    try {
                        val darkModeMethod = javafx.application.Platform::class.java.getMethod("isDarkMode")
                        val isDark = darkModeMethod.invoke(null) as? Boolean ?: false
                        if (isDark) ThemePreference.DARK else ThemePreference.LIGHT
                    } catch (e: Exception) {
                        // Fallback: default to DARK (current theme is dark)
                        logger.warn { "Could not detect system theme, defaulting to DARK" }
                        ThemePreference.DARK
                    }
                }
                else -> theme
            }
            
            // Apply theme to all scenes and their roots
            javafx.application.Platform.runLater {
                try {
                    // Apply to primary stage scene
                    tornadofx.FX.primaryStage?.scene?.root?.let { root ->
                        root.styleClass.removeAll(listOf("theme-light", "theme-dark"))
                        when (actualTheme) {
                            ThemePreference.LIGHT -> root.styleClass.add("theme-light")
                            ThemePreference.DARK -> root.styleClass.add("theme-dark")
                            else -> {} // AUTO already resolved
                        }
                        logger.info { "✅ Theme applied to primary stage root: $actualTheme" }
                    }
                    
                    // Also apply to all windows/scenes
                    javafx.stage.Window.getWindows().forEach { window ->
                        if (window is javafx.stage.Stage) {
                            window.scene?.root?.let { root ->
                                root.styleClass.removeAll(listOf("theme-light", "theme-dark"))
                                when (actualTheme) {
                                    ThemePreference.LIGHT -> root.styleClass.add("theme-light")
                                    ThemePreference.DARK -> root.styleClass.add("theme-dark")
                                    else -> {}
                                }
                            }
                        }
                    }
                    logger.info { "✅ Theme applied: $actualTheme" }
                } catch (e: Exception) {
                    logger.error(e) { "❌ Error applying theme: ${e.message}" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to apply theme preference: ${e.message}" }
        }
    }
}

