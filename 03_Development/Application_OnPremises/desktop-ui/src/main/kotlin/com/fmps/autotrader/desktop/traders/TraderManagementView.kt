package com.fmps.autotrader.desktop.traders

import com.fmps.autotrader.desktop.components.ToolbarButton
import com.fmps.autotrader.desktop.mvvm.BaseView
import com.fmps.autotrader.desktop.services.TraderDetail
import com.fmps.autotrader.desktop.services.TraderRiskLevel
import com.fmps.autotrader.desktop.services.TraderStatus
import com.fmps.autotrader.shared.model.TradingStrategy
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.Separator
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.util.StringConverter
import tornadofx.action
import tornadofx.addClass
import tornadofx.error
import tornadofx.information
import javafx.scene.control.ScrollPane
import javafx.scene.Node
import javafx.scene.layout.Pane
import java.text.DecimalFormat

class TraderManagementView :
    BaseView<TraderManagementState, TraderManagementEvent, TraderManagementViewModel>(TraderManagementViewModel::class) {

    companion object {
        // Store trader ID to select when view is displayed
        @Volatile
        var traderIdToSelect: String? = null
            private set
        
        fun setTraderToSelect(traderId: String?) {
            traderIdToSelect = traderId
        }
    }

    private val traderTable = TableView<TraderDetail>()
    private val searchField = TextField()
    private val statusFilter = ComboBox<TraderStatusFilter>()
    private val nameField = TextField()
    private val exchangeField = ComboBox<String>()
    private val strategyField = ComboBox<String>()
    private val riskField = ComboBox<TraderRiskLevel>()
    private val baseAssetField = ComboBox<String>()
    private val quoteAssetField = ComboBox<String>()
    private val budgetField = TextField()
    private val leverageField = TextField()
    private val stopLossField = TextField()
    private val takeProfitField = TextField()
    private val validationLabel = Label()
    private val isUpdatingForm = SimpleBooleanProperty(false)
    private val isSavingProperty = SimpleBooleanProperty(false)
    private lateinit var hasUnsavedChangesProperty: SimpleBooleanProperty
    private lateinit var canStartProperty: SimpleBooleanProperty
    private lateinit var canStopProperty: SimpleBooleanProperty
    private lateinit var canDeleteProperty: SimpleBooleanProperty
    private lateinit var saveButton: ToolbarButton
    private val isRunningProperty = SimpleBooleanProperty(false)
    private val canStartStopProperty = SimpleBooleanProperty(false)

    private val exchanges = listOf("Binance", "Bitget")
    
    // Common base assets (cryptocurrencies)
    private val baseAssets = listOf("BTC", "ETH", "BNB", "SOL", "ADA", "XRP", "DOT", "DOGE", "AVAX", "MATIC", "LINK", "UNI", "LTC", "ATOM", "ETC", "ALGO", "FIL", "TRX", "XLM", "EOS", "AAVE", "MKR", "COMP", "SUSHI", "YFI", "SNX", "CRV", "1INCH", "BAL", "REN")
    
    // Common quote assets (stablecoins and fiat)
    private val quoteAssets = listOf("USDT", "USDC", "BUSD", "DAI", "TUSD", "EUR", "GBP", "JPY")
    
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
    
    // Use actual TradingStrategy enum values (only implemented strategies)
    private val strategies = TradingStrategy.values().map { getStrategyDisplayName(it.name) }

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

    // Cleanup all class property nodes from their old parents before root initialization
    private fun cleanupNodes() {
        listOf(
            traderTable, searchField, statusFilter, nameField, exchangeField,
            strategyField, riskField, baseAssetField, quoteAssetField, budgetField,
            leverageField, stopLossField, takeProfitField, validationLabel
        ).forEach { node ->
            node.parent?.let { oldParent ->
                (oldParent as? Pane)?.children?.remove(node)
            }
        }
    }

    override val root: BorderPane = try {
        BorderPane().apply {
            // Clean up any nodes from previous instantiations first
            cleanupNodes()
            padding = Insets(12.0)
            println("🔍 TraderManagementView: Building header...")
            top = buildHeader()
            println("🔍 TraderManagementView: Building content...")
            center = buildContent()
            println("✅ TraderManagementView: Root BorderPane initialized successfully")
            
            // Explicitly call onDock to ensure state collection starts
            // This is needed because onDock() might not be called automatically for views in TabPane
            javafx.application.Platform.runLater {
                try {
                    println("🔍 TraderManagementView: Manually calling onDock() to start state collection")
                    onDock()
                    // Also check for pending trader selection after a delay
                    checkAndSelectPendingTrader()
                } catch (e: Exception) {
                    println("❌ Error calling onDock() manually: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    } catch (e: Exception) {
        println("❌ TraderManagementView: Exception during root initialization")
        println("   Error type: ${e.javaClass.simpleName}")
        println("   Error message: ${e.message}")
        e.printStackTrace()
        throw e
    }

    private fun buildHeader(): HBox = HBox(12.0).apply {
        addClass("dashboard-header")
        alignment = Pos.CENTER_LEFT
        padding = Insets(4.0, 4.0, 8.0, 4.0)
        
        children += Label("AI Traders").apply {
            addClass("view-header")
        }
        children += Label("|").apply {
            addClass("view-header")
            style = "-fx-padding: 0 8 0 8;"
        }
        children += Label("Manage and configure your AI trading bots.").apply {
            addClass("view-description")
        }
    }

    private fun buildContent(): HBox = HBox(20.0).apply {
        HBox.setHgrow(this, Priority.ALWAYS)
        traderTable.apply {
            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            prefWidth = 520.0
            prefHeight = 20 * 30.0 // 20 rows * ~30px per row
            columns += createColumn("Name", TraderDetail::name)
            columns += createColumn("Exchange", TraderDetail::exchange)
            columns += createColumn("Strategy", TraderDetail::strategy)
            columns += statusColumn()
            columns += profitColumn()
            // Set placeholder with visible color for better readability
            placeholder = Label("No trader available").apply {
                styleClass += "table-placeholder"
                // Use a more visible color - lighter grey that's readable
                style = "-fx-text-fill: #999999; -fx-font-size: 14px; -fx-opacity: 1.0;"
            }
            selectionModel.selectedItemProperty().addListener { _, _, new ->
                // Clear all previous errors when selecting a trader
                viewModel.clearAllFormErrors()
                viewModel.selectTrader(new)
            }
        }
        traderTable.safeAddTo(this)

        // Create a VBox for the form area (header + form + buttons)
        val formArea = VBox(0.0).apply {
            HBox.setHgrow(this, Priority.ALWAYS)
            
            // Add "Trader Details" header outside ScrollPane
            children += Label("Trader Details").apply { 
                styleClass += "section-title"
                padding = Insets(0.0, 0.0, 8.0, 0.0) // Bottom padding for spacing
            }
            
            val (formContent, buttons) = buildForm()
            // Wrap only form content in ScrollPane (header and buttons stay outside)
            val formScroll = ScrollPane().apply {
                content = formContent
                isFitToWidth = true
                vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
                hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
                VBox.setVgrow(this, Priority.ALWAYS)
            }
            formScroll.safeAddTo(this)
            
            // Add buttons outside the ScrollPane, below the form
            buttons.safeAddTo(this)
        }
        formArea.safeAddTo(this)
    }

    private fun <T> createColumn(title: String, extractor: (TraderDetail) -> T) =
        TableColumn<TraderDetail, T>(title).apply {
            setCellValueFactory { features -> javafx.beans.property.SimpleObjectProperty(extractor(features.value)) }
        }

    private fun statusColumn() = TableColumn<TraderDetail, String>("Status").apply {
        setCellValueFactory { features ->
            val label = features.value.status.name.lowercase().replaceFirstChar { it.uppercase() }
            javafx.beans.property.SimpleStringProperty(label)
        }
    }

    private fun profitColumn() = TableColumn<TraderDetail, String>("P&L").apply {
        val formatter = DecimalFormat("#,##0.00")
        setCellValueFactory { features ->
            val pnl = formatter.format(features.value.profitLoss)
            javafx.beans.property.SimpleStringProperty("$pnl USDT")
        }
    }

    private fun buildForm(): Pair<VBox, HBox> {
        // Create form content (scrollable) - header is outside
        val formContent = VBox(10.0).apply {
            styleClass += "content-card"
            padding = Insets(16.0)
            // Note: "Trader Details" header is moved outside ScrollPane

        nameField.promptText = "Name"
        // Don't use TextFormatter for name field to avoid reverse typing issue
        nameField.textProperty().addListener { _, old, new ->
            if (!isUpdatingForm.get() && old != new) {
                viewModel.updateForm { it.copy(name = new.orEmpty()) }
            }
        }
        children += labeledField("Name", nameField)

        exchangeField.items.addAll(exchanges)
        exchangeField.selectionModel.selectFirst()
        exchangeField.selectionModel.selectedItemProperty().addListener { _, _, new ->
            if (!isUpdatingForm.get() && new != null) viewModel.updateForm { it.copy(exchange = new) }
        }
        // Exchange field is only editable when creating a new trader
        children += labeledField("Exchange", exchangeField)

        strategyField.items.addAll(strategies)
        strategyField.selectionModel.selectFirst()
        strategyField.selectionModel.selectedItemProperty().addListener { _, _, new ->
            if (!isUpdatingForm.get() && new != null) {
                // Convert display name to enum name for storage
                val enumName = getStrategyEnumName(new)
                viewModel.updateForm { it.copy(strategy = enumName) }
            }
        }
        children += labeledField("Strategy", strategyField)

        riskField.items.addAll(TraderRiskLevel.values())
        riskField.selectionModel.select(TraderRiskLevel.BALANCED)
        riskField.selectionModel.selectedItemProperty().addListener { _, _, new ->
            if (!isUpdatingForm.get() && new != null) viewModel.updateForm { it.copy(riskLevel = new) }
        }
        children += labeledField("Risk Profile", riskField)

        baseAssetField.items.addAll(baseAssets)
        baseAssetField.selectionModel.selectFirst()
        baseAssetField.selectionModel.selectedItemProperty().addListener { _, _, new ->
            if (!isUpdatingForm.get() && new != null) viewModel.updateForm { it.copy(baseAsset = new) }
        }
        // Base Asset field is only editable when creating a new trader
        children += labeledField("Base Asset", baseAssetField)

        quoteAssetField.items.addAll(quoteAssets)
        quoteAssetField.selectionModel.selectFirst()
        quoteAssetField.selectionModel.selectedItemProperty().addListener { _, _, new ->
            if (!isUpdatingForm.get() && new != null) viewModel.updateForm { it.copy(quoteAsset = new) }
        }
        // Quote Asset field is only editable when creating a new trader
        children += labeledField("Quote Asset", quoteAssetField)

        budgetField.textFormatter = TextFormatter(object : StringConverter<Double>() {
            override fun toString(value: Double?): String {
                return if (value != null && value != 0.0) {
                    // Format to remove unnecessary decimal places, but preserve decimals if they exist
                    if (value % 1.0 == 0.0) {
                        value.toInt().toString()
                    } else {
                        // Keep up to 2 decimal places, remove trailing zeros
                        String.format("%.2f", value).trimEnd('0').trimEnd('.')
                    }
                } else {
                    "" // Return empty string for null or 0.0 to allow user input
                }
            }
            override fun fromString(string: String?): Double {
                val trimmed = string?.trim() ?: ""
                // Allow partial input like "7." or "7.5" while typing
                if (trimmed.isEmpty() || trimmed == ".") return 0.0
                return trimmed.toDoubleOrNull() ?: 0.0
            }
        })
        // Use focusedProperty to only update when field loses focus to prevent cursor jumping
        budgetField.focusedProperty().addListener { _, wasFocused, isFocused ->
            if (!isFocused && wasFocused && !isUpdatingForm.get()) {
                val amount = budgetField.text?.toDoubleOrNull() ?: 0.0
                viewModel.updateForm { it.copy(budget = amount) }
            }
        }
        children += labeledField("Budget (USDT)", budgetField)
        
        // Leverage field
        leverageField.textFormatter = TextFormatter(object : StringConverter<Int>() {
            override fun toString(value: Int?): String = value?.toString() ?: ""
            override fun fromString(string: String?): Int = string?.toIntOrNull() ?: 0
        })
        leverageField.focusedProperty().addListener { _, wasFocused, isFocused ->
            if (!isFocused && wasFocused && !isUpdatingForm.get()) {
                val value = leverageField.text?.toIntOrNull() ?: 0
                viewModel.updateForm { it.copy(leverage = value) }
            }
        }
        children += labeledField("Leverage", leverageField)
        
        // Stop Loss field
        stopLossField.textFormatter = TextFormatter(object : StringConverter<Double>() {
            override fun toString(value: Double?): String {
                return if (value != null && value != 0.0) {
                    // Format to remove unnecessary decimal places, but preserve decimals if they exist
                    if (value % 1.0 == 0.0) {
                        value.toInt().toString()
                    } else {
                        // Keep up to 2 decimal places, remove trailing zeros
                        String.format("%.2f", value).trimEnd('0').trimEnd('.')
                    }
                } else {
                    "" // Return empty string for null or 0.0 to allow user input
                }
            }
            override fun fromString(string: String?): Double {
                val trimmed = string?.trim() ?: ""
                // Allow partial input like "7." or "7.5" while typing
                if (trimmed.isEmpty() || trimmed == ".") return 0.0
                return trimmed.toDoubleOrNull() ?: 0.0
            }
        })
        stopLossField.focusedProperty().addListener { _, wasFocused, isFocused ->
            if (!isFocused && wasFocused && !isUpdatingForm.get()) {
                val value = stopLossField.text?.toDoubleOrNull() ?: 0.0
                viewModel.updateForm { it.copy(stopLossPercent = value) }
            }
        }
        children += labeledField("Stop Loss (%)", stopLossField)
        
        // Take Profit field
        takeProfitField.textFormatter = TextFormatter(object : StringConverter<Double>() {
            override fun toString(value: Double?): String {
                return if (value != null && value != 0.0) {
                    // Format to remove unnecessary decimal places, but preserve decimals if they exist
                    if (value % 1.0 == 0.0) {
                        value.toInt().toString()
                    } else {
                        // Keep up to 2 decimal places, remove trailing zeros
                        String.format("%.2f", value).trimEnd('0').trimEnd('.')
                    }
                } else {
                    "" // Return empty string for null or 0.0 to allow user input
                }
            }
            override fun fromString(string: String?): Double {
                val trimmed = string?.trim() ?: ""
                // Allow partial input like "7." or "7.5" while typing
                if (trimmed.isEmpty() || trimmed == ".") return 0.0
                return trimmed.toDoubleOrNull() ?: 0.0
            }
        })
        takeProfitField.focusedProperty().addListener { _, wasFocused, isFocused ->
            if (!isFocused && wasFocused && !isUpdatingForm.get()) {
                val value = takeProfitField.text?.toDoubleOrNull() ?: 0.0
                viewModel.updateForm { it.copy(takeProfitPercent = value) }
            }
        }
        children += labeledField("Take Profit (%)", takeProfitField)

            validationLabel.styleClass += "validation-label"
            validationLabel.safeAddTo(this)
        }

        // Action buttons: New Trader, Delete, Save, Start, Stop (outside scrollable area)
        val newTraderButton = ToolbarButton("New Trader", icon = "＋")
        newTraderButton.setOnAction { 
            println("🔍 New Trader button clicked!")
            // Clear all previous errors first
            viewModel.clearAllFormErrors()
            // Always check conditions first (even if button should be disabled)
            if (viewModel.state.value.traders.size >= 3) {
                viewModel.setFormError("newTrader", "Maximum of 3 AI traders already created. Please delete a trader first.")
                return@setOnAction
            }
            try {
                viewModel.newTrader()
            } catch (e: Exception) {
                println("❌ Error in New Trader button: ${e.message}")
                e.printStackTrace()
            }
        }
        
        val deleteButton = ToolbarButton("Delete", icon = "✖", emphasis = ToolbarButton.Emphasis.DANGER)
        canDeleteProperty = SimpleBooleanProperty(false)
        // Keep button enabled but visually style as disabled when conditions aren't met
        // This allows clicks to show error messages
        // Opacity: 0.5 (greyed out) when canDeleteProperty is false, 1.0 (normal) when true
        deleteButton.opacityProperty().bind(
            Bindings.`when`(canDeleteProperty).then(1.0).otherwise(0.5)
        )
        deleteButton.setOnAction { 
            println("🔍 Delete button clicked!")
            // Clear all previous errors first
            viewModel.clearAllFormErrors()
            val selected = traderTable.selectionModel.selectedItem
            if (selected == null) {
                println("🔍 No trader selected, showing error message")
                viewModel.setFormError("delete", "Please select a trader first.")
                return@setOnAction
            }
            if (selected.status == TraderStatus.RUNNING) {
                println("🔍 Trader is running, showing error message")
                viewModel.setFormError("delete", "Cannot delete active trader. Stop the trader first.")
                return@setOnAction
            }
            // Conditions are met, proceed with deletion
            println("🔍 Conditions met, proceeding with deletion")
            try {
                viewModel.deleteSelectedTrader()
            } catch (e: Exception) {
                println("❌ Error in Delete button: ${e.message}")
                e.printStackTrace()
            }
        }
        
        saveButton = ToolbarButton("Save", icon = "💾", emphasis = ToolbarButton.Emphasis.PRIMARY)
        
        // Combined Start/Stop toggle button - use SECONDARY initially (will be updated based on state)
        val startStopButton = ToolbarButton("Start", icon = "▶", emphasis = ToolbarButton.Emphasis.SECONDARY)
        // Width will be set in buttonRow to match other buttons
        // Initially set to greyed green (disabled success style)
        startStopButton.styleClass.removeAll(listOf("toolbar-button-primary", "toolbar-button-secondary", "toolbar-button-danger", "toolbar-button-success"))
        startStopButton.styleClass += "toolbar-button-success-disabled"
        
        // Initialize properties
        hasUnsavedChangesProperty = SimpleBooleanProperty(false)
        canStartProperty = SimpleBooleanProperty(false)
        canStopProperty = SimpleBooleanProperty(false)
        
        // Bind button states
        // Save button: disabled when saving OR when no unsaved changes
        saveButton.disableProperty().bind(Bindings.or(
            isSavingProperty,
            Bindings.not(hasUnsavedChangesProperty)
        ))
        
        // Start/Stop toggle button: disabled when no trader selected, when saving, or when trader is in error
        startStopButton.disableProperty().bind(
            Bindings.or(
                Bindings.isNull(traderTable.selectionModel.selectedItemProperty()),
                Bindings.or(
                    isSavingProperty,
                    Bindings.not(canStartStopProperty)
                )
            )
        )
        
        // Update button appearance based on trader status
        // Also listen to disabled state to show greyed out when disabled
        val updateButtonAppearance: (Boolean, Boolean) -> Unit = { isRunning, isDisabled ->
            if (isRunning && !isDisabled) {
                // Running and enabled: Show "Stop" in red (DANGER)
                startStopButton.text = "Stop"
                startStopButton.graphic = javafx.scene.control.Label("■").apply {
                    styleClass += "toolbar-button-icon"
                    style = "-fx-font-size: 18px;"
                }
                startStopButton.styleClass.removeAll(listOf("toolbar-button-primary", "toolbar-button-secondary", "toolbar-button-danger", "toolbar-button-success", "toolbar-button-success-disabled"))
                startStopButton.styleClass += "toolbar-button-danger"
            } else if (!isDisabled) {
                // Stopped and enabled: Show "Start" in green (SUCCESS)
                startStopButton.text = "Start"
                startStopButton.graphic = javafx.scene.control.Label("▶").apply {
                    styleClass += "toolbar-button-icon"
                    style = "-fx-font-size: 18px;"
                }
                startStopButton.styleClass.removeAll(listOf("toolbar-button-primary", "toolbar-button-secondary", "toolbar-button-danger", "toolbar-button-success", "toolbar-button-success-disabled"))
                startStopButton.styleClass += "toolbar-button-success"
            } else {
                // Disabled: Show "Start" in greyed green (SUCCESS-DISABLED)
                startStopButton.text = "Start"
                startStopButton.graphic = javafx.scene.control.Label("▶").apply {
                    styleClass += "toolbar-button-icon"
                    style = "-fx-font-size: 18px;"
                }
                startStopButton.styleClass.removeAll(listOf("toolbar-button-primary", "toolbar-button-secondary", "toolbar-button-danger", "toolbar-button-success", "toolbar-button-success-disabled"))
                startStopButton.styleClass += "toolbar-button-success-disabled"
            }
        }
        
        // Listen to both running state and disabled state
        isRunningProperty.addListener { _, _, isRunning ->
            val isDisabled = startStopButton.isDisable
            updateButtonAppearance(isRunning, isDisabled)
        }
        startStopButton.disableProperty().addListener { _, _, isDisabled ->
            val isRunning = isRunningProperty.get()
            updateButtonAppearance(isRunning, isDisabled)
        }
        
        saveButton.action { 
            println("🔍 Save button clicked!")
            // Clear all previous errors when starting a new action
            viewModel.clearAllFormErrors()
            try {
                viewModel.saveTrader()
            } catch (e: Exception) {
                println("❌ Error in Save button action: ${e.message}")
                e.printStackTrace()
            }
        }
        startStopButton.action { 
            traderTable.selectionModel.selectedItem?.let { trader ->
                println("🔍 Start/Stop button clicked for trader: ${trader.id}, current status: ${trader.status}")
                // Clear all previous errors when starting a new action
                viewModel.clearAllFormErrors()
                if (trader.status == TraderStatus.RUNNING) {
                    viewModel.stopTrader(trader.id)
                } else {
                    viewModel.startTrader(trader.id)
                }
            }
        }
        
        // Create buttons outside the scrollable area
        val buttonRow = HBox(10.0).apply {
            alignment = Pos.CENTER_RIGHT
            padding = Insets(16.0, 0.0, 0.0, 0.0) // Top padding to separate from form
            
            // Set all buttons to the same width (wider to fit "New Trader" text)
            val buttonWidth = 125.0
            newTraderButton.minWidth = buttonWidth
            newTraderButton.prefWidth = buttonWidth
            deleteButton.minWidth = buttonWidth
            deleteButton.prefWidth = buttonWidth
            saveButton.minWidth = buttonWidth
            saveButton.prefWidth = buttonWidth
            startStopButton.minWidth = buttonWidth
            startStopButton.prefWidth = buttonWidth
            
            children += newTraderButton
            children += deleteButton
            children += saveButton
            children += startStopButton
        }
        
        return Pair(formContent, buttonRow)
    }

    private fun labeledField(labelText: String, field: javafx.scene.Node) = VBox(4.0).apply {
        children += Label(labelText).apply { styleClass += "field-label" }
        // Use safeAddTo for class property fields to prevent duplicate children
        // Remove from old parent first, then add to this VBox
        field.parent?.let { oldParent ->
            (oldParent as? Pane)?.children?.remove(field)
        }
        if (!children.contains(field)) {
            children += field
        }
    }

    override fun onDock() {
        super.onDock()
        println("🔍 TraderManagementView.onDock() called")
        // Check if there's a trader ID to select when view is displayed
        checkAndSelectPendingTrader()
    }
    
    private fun checkAndSelectPendingTrader() {
        val traderIdToSelect = TraderManagementView.traderIdToSelect
        if (traderIdToSelect != null) {
            println("🔍 TraderManagementView.checkAndSelectPendingTrader(): Found trader ID to select: $traderIdToSelect")
            // Check if traders are already loaded
            val currentState = viewModel.state.value
            if (currentState.traders.isNotEmpty()) {
                val trader = currentState.traders.firstOrNull { it.id == traderIdToSelect }
                if (trader != null) {
                    // Always select, even if already selected (ensures UI updates for subsequent clicks)
                    println("🔍 Traders already loaded, selecting: $traderIdToSelect (current: ${currentState.selectedTraderId})")
                    viewModel.selectTraderById(traderIdToSelect)
                } else {
                    println("⚠️ Trader $traderIdToSelect not found in ${currentState.traders.size} traders")
                }
            } else {
                // Delay to ensure view and ViewModel are fully initialized
                println("🔍 Traders not loaded yet, waiting...")
                javafx.application.Platform.runLater {
                    javafx.animation.PauseTransition(javafx.util.Duration.millis(600.0)).apply {
                        setOnFinished {
                            val stillPending = TraderManagementView.traderIdToSelect
                            if (stillPending == traderIdToSelect) {
                                println("🔍 TraderManagementView: Calling selectTraderById for: $traderIdToSelect")
                                viewModel.selectTraderById(traderIdToSelect)
                            } else {
                                println("🔍 TraderManagementView: Pending ID changed from $traderIdToSelect to $stillPending, skipping")
                            }
                        }
                        play()
                    }
                }
            }
        }
    }

    override fun onStateChanged(state: TraderManagementState) {
        println("🔍 ========== onStateChanged() CALLED ==========")
        println("🔍 State: form.name='${state.form.name}', form.budget=${state.form.budget}, form.leverage=${state.form.leverage}, form.stopLoss=${state.form.stopLossPercent}, form.takeProfit=${state.form.takeProfitPercent}")
        
        // Update table with filtered traders
        val currentItems = traderTable.items.toList()
        if (currentItems != state.filteredTraders) {
            traderTable.items.setAll(state.filteredTraders)
            println("🔍 Trader table updated: ${state.filteredTraders.size} traders (filtered from ${state.traders.size} total)")
        }
        
        // Update selection - ensure table selection matches state
        // Also check if there's a pending trader ID to select
        val traderIdToSelect = TraderManagementView.traderIdToSelect
        
        // If we have a pending trader ID to select and traders are loaded, try to select it
        if (traderIdToSelect != null && state.traders.isNotEmpty()) {
            val pendingTrader = state.traders.firstOrNull { it.id == traderIdToSelect }
            if (pendingTrader != null) {
                // Always trigger selection if there's a pending ID, regardless of current selection
                // This ensures clicking "Open" on different traders always works
                println("🔍 Found pending trader to select: ${pendingTrader.name} (ID: $traderIdToSelect), current selection: ${state.selectedTraderId}")
                // Always call selectTraderById when there's a pending ID - it handles the logic internally
                // This ensures subsequent "Open" clicks always work
                println("🔍 Calling selectTraderById for pending trader: $traderIdToSelect")
                viewModel.selectTraderById(traderIdToSelect)
            } else {
                println("🔍 Pending trader ID $traderIdToSelect not found in ${state.traders.size} traders")
            }
        }
        
        // Update table selection to match state
        val selected = state.traders.firstOrNull { it.id == state.selectedTraderId }
        val currentTableSelection = traderTable.selectionModel.selectedItem
        
        if (selected != null) {
            // If state has a selected trader, ensure table matches
            if (currentTableSelection?.id != selected.id) {
                println("🔍 Selecting trader in table: ${selected.name} (ID: ${selected.id})")
                traderTable.selectionModel.select(selected)
                // Scroll to selected row if possible
                val index = traderTable.items.indexOf(selected)
                if (index >= 0) {
                    traderTable.scrollTo(index)
                }
                println("✅ Trader selected in table: ${selected.name} (ID: ${selected.id})")
                // Clear the pending selection only if it matches the selected trader
                // This ensures we don't clear a new pending selection that was set while this one was processing
                val currentPendingId = TraderManagementView.traderIdToSelect
                if (currentPendingId != null && currentPendingId == selected.id) {
                    TraderManagementView.setTraderToSelect(null)
                    println("🔍 Cleared pending trader selection for: $currentPendingId")
                }
            }
        } else if (currentTableSelection != null && state.selectedTraderId == null) {
            // If state has no selection but table does, clear table selection
            traderTable.selectionModel.clearSelection()
            println("🔍 Trader selection cleared")
        }

        // Filters section removed - no need to update filter UI

        isUpdatingForm.set(true)
        println("🔍 onStateChanged: Updating form fields from state")
        println("🔍 Form state: name='${state.form.name}', budget=${state.form.budget}, leverage=${state.form.leverage}, stopLoss=${state.form.stopLossPercent}, takeProfit=${state.form.takeProfitPercent}")
        
        // Update name field only if different to avoid reverse typing issue
        if (nameField.text != state.form.name) {
            nameField.text = state.form.name
        }
        // Only update dropdowns if not currently focused to avoid disrupting user input
        // Exchange, Base Asset, and Quote Asset are read-only after save
        val isNewTrader = state.form.isNew
        exchangeField.isDisable = !isNewTrader
        if (!exchangeField.isFocused) {
            exchangeField.selectionModel.select(state.form.exchange.takeIf { exchanges.contains(it) } ?: exchanges.first())
        }
        // Convert enum name to display name for selection
        val strategyDisplayName = getStrategyDisplayName(state.form.strategy)
        if (!strategyField.isFocused) {
            val strategyToSelect = strategyDisplayName.takeIf { strategies.contains(it) } ?: strategies.first()
            if (strategyField.selectionModel.selectedItem != strategyToSelect) {
                strategyField.selectionModel.select(strategyToSelect)
            }
        }
        if (!riskField.isFocused) {
            riskField.selectionModel.select(state.form.riskLevel)
        }
        baseAssetField.isDisable = !isNewTrader
        if (!baseAssetField.isFocused) {
            baseAssetField.selectionModel.select(state.form.baseAsset.takeIf { baseAssets.contains(it) } ?: baseAssets.firstOrNull())
        }
        quoteAssetField.isDisable = !isNewTrader
        if (!quoteAssetField.isFocused) {
            quoteAssetField.selectionModel.select(state.form.quoteAsset.takeIf { quoteAssets.contains(it) } ?: quoteAssets.firstOrNull())
        }
        
        // Update budget field - only if not focused to prevent cursor jumping
        // Use TextFormatter's value property to update properly
        if (!budgetField.isFocused) {
            val formatter = budgetField.textFormatter as? TextFormatter<Double>
            val currentValue = formatter?.value ?: 0.0
            if (Math.abs(currentValue - state.form.budget) > 0.001) {
                // Only update if value actually changed (with small tolerance for floating point)
                formatter?.value = state.form.budget
            }
        }
        
        // Update leverage field - only if not focused to prevent cursor jumping
        if (!leverageField.isFocused) {
            val leverageText = if (state.form.leverage > 0) state.form.leverage.toString() else ""
            if (leverageField.text != leverageText) {
                println("🔍 Setting leverage field: '$leverageText' (current: '${leverageField.text}')")
                leverageField.text = leverageText
            }
        }
        
        // Update stop loss field - only if not focused to prevent cursor jumping
        // Use TextFormatter's value property to update properly
        if (!stopLossField.isFocused) {
            val formatter = stopLossField.textFormatter as? TextFormatter<Double>
            val currentValue = formatter?.value ?: 0.0
            if (Math.abs(currentValue - state.form.stopLossPercent) > 0.001) {
                formatter?.value = state.form.stopLossPercent
            }
        }
        
        // Update take profit field - only if not focused to prevent cursor jumping
        // Use TextFormatter's value property to update properly
        if (!takeProfitField.isFocused) {
            val formatter = takeProfitField.textFormatter as? TextFormatter<Double>
            val currentValue = formatter?.value ?: 0.0
            if (Math.abs(currentValue - state.form.takeProfitPercent) > 0.001) {
                formatter?.value = state.form.takeProfitPercent
            }
        }
        
        println("🔍 Form fields updated. Final values: budget='${budgetField.text}', leverage='${leverageField.text}', stopLoss='${stopLossField.text}', takeProfit='${takeProfitField.text}'")
        
        // Display validation errors like Configuration view (including delete and newTrader errors)
        // Format matches Configuration View: show first error, or specific action errors
        validationLabel.text = state.form.errors["delete"]
            ?: state.form.errors["newTrader"]
            ?: state.form.errors["name"]?.let { "Name: $it" }
            ?: state.form.errors["budget"]?.let { "Budget: $it" }
            ?: state.form.errors["baseAsset"]?.let { "Base Asset: $it" }
            ?: state.form.errors["quoteAsset"]?.let { "Quote Asset: $it" }
            ?: state.form.errors["leverage"]?.let { "Leverage: $it" }
            ?: state.form.errors["stopLoss"]?.let { "Stop Loss: $it" }
            ?: state.form.errors["takeProfit"]?.let { "Take Profit: $it" }
            ?: state.form.errors["strategy"]?.let { "Strategy: $it" }
            ?: ""
        
        validationLabel.isVisible = state.form.errors.isNotEmpty()
        // Use same style as Configuration View - just validation-label, no status-error
        if (!validationLabel.styleClass.contains("validation-label")) {
            validationLabel.styleClass += "validation-label"
        }
        // Remove status-error class if present (Configuration View doesn't use it)
        validationLabel.styleClass.removeAll(listOf("status-error"))
        
        // Update button states
        isSavingProperty.set(state.isSaving)
        hasUnsavedChangesProperty.set(state.hasUnsavedChanges)
        
        // Update Start/Stop/Delete button states based on selected trader status
        val selectedTrader = state.traders.firstOrNull { it.id == state.selectedTraderId }
        if (selectedTrader != null) {
            val isRunning = selectedTrader.status == TraderStatus.RUNNING
            val isError = selectedTrader.status == TraderStatus.ERROR
            val isNewTrader = state.form.isNew
            
            // Button shows "Start" when stopped, "Stop" when running, "Start" (greyed) when error or new trader
            if (isRunning) {
                isRunningProperty.set(true)
                canStartStopProperty.set(true) // Can stop when running
            } else if (isError || isNewTrader) {
                isRunningProperty.set(false) // Show "Start" but greyed out
                canStartStopProperty.set(false) // Disabled when error or new trader
            } else {
                isRunningProperty.set(false) // Show "Start"
                canStartStopProperty.set(true) // Can start when stopped
            }
            
            canStartProperty.set(selectedTrader.status == TraderStatus.STOPPED)
            canStopProperty.set(selectedTrader.status == TraderStatus.RUNNING)
            canDeleteProperty.set(selectedTrader.status != TraderStatus.RUNNING)
        } else {
            // No trader selected - show "Start" but greyed out
            isRunningProperty.set(false)
            canStartStopProperty.set(false)
            canStartProperty.set(false)
            canStopProperty.set(false)
            canDeleteProperty.set(false)
        }
        
        // Update Save button style based on unsaved changes
        if (state.hasUnsavedChanges) {
            saveButton.styleClass.removeAll(listOf("toolbar-button-primary-saved"))
            if (!saveButton.styleClass.contains("toolbar-button-primary-dirty")) {
                saveButton.styleClass += "toolbar-button-primary-dirty"
            }
        } else {
            saveButton.styleClass.removeAll(listOf("toolbar-button-primary-dirty"))
            if (!saveButton.styleClass.contains("toolbar-button-primary-saved")) {
                saveButton.styleClass += "toolbar-button-primary-saved"
            }
        }
        
        isUpdatingForm.set(false)
    }

    override fun onEvent(event: TraderManagementEvent) {
        when (event) {
            is TraderManagementEvent.ShowMessage -> {
                println("🔄 onEvent received: ShowMessage (${event.type}) - '${event.message}'")
                when (event.type) {
                    TraderManagementEvent.MessageType.ERROR -> {
                        println("🔄 Showing error dialog: '${event.message}'")
                        error(event.message)
                        println("✅ Error dialog shown")
                    }
                    TraderManagementEvent.MessageType.SUCCESS -> {
                        // No success dialogs - information is shown in the UI
                        println("✅ Success: '${event.message}' (no dialog shown)")
                    }
                    TraderManagementEvent.MessageType.INFO -> {
                        println("🔄 Showing info dialog: '${event.message}'")
                        information(event.message)
                        println("✅ Info dialog shown")
                    }
                }
            }
        }
    }
}

