package com.fmps.autotrader.desktop.config

import com.fmps.autotrader.desktop.components.ToolbarButton
import com.fmps.autotrader.desktop.mvvm.BaseView
import com.fmps.autotrader.desktop.services.Exchange
import com.fmps.autotrader.desktop.services.LoggingLevel
import com.fmps.autotrader.desktop.services.ThemePreference
import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.TabPane
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.Node
import javafx.scene.control.TextFormatter
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import tornadofx.action
import tornadofx.borderpane
import tornadofx.hbox
import tornadofx.information
import tornadofx.label
import tornadofx.tab
import tornadofx.tabpane
import tornadofx.textarea
import tornadofx.textfield
import tornadofx.vbox

class ConfigurationView :
    BaseView<ConfigurationState, ConfigurationEvent, ConfigurationViewModel>(ConfigurationViewModel::class) {

    private val exchangeApiKey = TextField()
    private val exchangeSecret = TextField()
    private val exchangePassphrase = TextField()
    private val exchangeSelect = ComboBox<Exchange>()
    private val exchangeErrors = Label()

    private val updateIntervalField = TextField()
    private val telemetryField = TextField()
    private val loggingSelect = ComboBox<LoggingLevel>()
    private val themeSelect = ComboBox<ThemePreference>()
    private val generalErrors = Label()

    private val budgetField = TextField()
    private val leverageField = TextField()
    private val stopLossField = TextField()
    private val strategyField = TextField()
    private val traderErrors = Label()

    private val exportArea = TextArea()
    private val importArea = TextArea()
    private val importStatus = Label()
    private val connectionLabel = Label()

    private val isExchangeUpdating = SimpleBooleanProperty(false)

    // Helper to safely add a node, removing it from old parent first
    private fun Node.safeAddTo(parent: Pane) {
        this.parent?.let { (it as? Pane)?.children?.remove(this) }
        parent.children += this
    }

    override val root = borderpane {
        padding = Insets(20.0)
        top = label("Configuration Management") {
            styleClass += "section-title"
        }
        center = buildTabs()
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
        exchangeSelect.items.addAll(Exchange.values())
        exchangeSelect.selectionModel.selectedItemProperty().addListener { _, _, new ->
            new?.let { viewModel.updateExchangeForm { form -> form.copy(exchange = it) } }
        }
        children += labeledField("Exchange", exchangeSelect)

        exchangeApiKey.promptText = "API Key"
        exchangeApiKey.textProperty().addListener { _, _, new ->
            viewModel.updateExchangeForm { it.copy(apiKey = new.orEmpty()) }
        }
        children += labeledField("API Key", exchangeApiKey)

        exchangeSecret.promptText = "Secret Key"
        exchangeSecret.textProperty().addListener { _, _, new ->
            viewModel.updateExchangeForm { it.copy(secretKey = new.orEmpty()) }
        }
        children += labeledField("Secret Key", exchangeSecret)

        exchangePassphrase.promptText = "Passphrase (Bitget only)"
        exchangePassphrase.textProperty().addListener { _, _, new ->
            viewModel.updateExchangeForm { it.copy(passphrase = new.orEmpty()) }
        }
        children += labeledField("Passphrase", exchangePassphrase)

        exchangeErrors.styleClass += "validation-label"
        exchangeErrors.safeAddTo(this)
        connectionLabel.styleClass += "meta-muted"
        connectionLabel.safeAddTo(this)

        hbox(10.0) {
            alignment = Pos.CENTER_RIGHT
            children += ToolbarButton("Test Connection", icon = "🧪").apply {
                disableProperty().bind(isExchangeUpdating)
                action { viewModel.testConnection() }
            }
            children += ToolbarButton("Save Exchange", icon = "💾", emphasis = ToolbarButton.Emphasis.PRIMARY).apply {
                disableProperty().bind(isExchangeUpdating)
                action { viewModel.saveExchangeSettings() }
            }
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
            new?.let { viewModel.updateGeneralForm { form -> form.copy(theme = it) } }
        }
        children += labeledField("Theme Preference", themeSelect)

        generalErrors.styleClass += "validation-label"
        children += generalErrors

        hbox(10.0) {
            alignment = Pos.CENTER_RIGHT
            children += ToolbarButton("Save General", icon = "💾", emphasis = ToolbarButton.Emphasis.PRIMARY).apply {
                action { viewModel.saveGeneralSettings() }
            }
        }
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

        strategyField.textProperty().addListener { _, _, new ->
            viewModel.updateTraderDefaults { it.copy(strategy = new.orEmpty()) }
        }
        children += labeledField("Default Strategy", strategyField)

        traderErrors.styleClass += "validation-label"
        children += traderErrors

        hbox(10.0) {
            alignment = Pos.CENTER_RIGHT
            children += ToolbarButton("Save Trader Defaults", icon = "💾", emphasis = ToolbarButton.Emphasis.PRIMARY).apply {
                action { viewModel.saveTraderDefaults() }
            }
        }
    }

    private fun buildImportExportTab(): VBox = vbox(12.0) {
        padding = Insets(16.0)
        exportArea.isEditable = false
        VBox.setVgrow(exportArea, Priority.ALWAYS)
        VBox.setVgrow(importArea, Priority.ALWAYS)

        children += label("Export Snapshot") { styleClass += "section-title" }
        exportArea.safeAddTo(this)
        children += ToolbarButton("Export Configuration", icon = "⬇").apply {
            action { viewModel.exportConfiguration() }
        }

        children += Region().apply { prefHeight = 10.0 }
        children += label("Import Configuration") { styleClass += "section-title" }
        importArea.promptText = "Paste configuration text..."
        importArea.safeAddTo(this)
        children += ToolbarButton("Import Configuration", icon = "⬆").apply {
            action { viewModel.importConfiguration(importArea.text) }
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

    override fun onStateChanged(state: ConfigurationState) {
        exchangeSelect.selectionModel.select(state.exchangeForm.exchange)
        if (exchangeApiKey.text != state.exchangeForm.apiKey) exchangeApiKey.text = state.exchangeForm.apiKey
        if (exchangeSecret.text != state.exchangeForm.secretKey) exchangeSecret.text = state.exchangeForm.secretKey
        if (exchangePassphrase.text != state.exchangeForm.passphrase) exchangePassphrase.text = state.exchangeForm.passphrase
        isExchangeUpdating.set(state.isTestingConnection || state.isSaving)

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
        if (strategyField.text != state.traderDefaultsForm.strategy) {
            strategyField.text = state.traderDefaultsForm.strategy
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
            ?: state.validationErrors["strategy"]

        state.connectionTest?.let {
            connectionLabel.text = "Last connection test: ${it.message}"
        } ?: run { connectionLabel.text = "" }

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

    override fun onEvent(event: ConfigurationEvent) {
        when (event) {
            is ConfigurationEvent.ShowMessage -> {
                when (event.level) {
                    ConfigurationEvent.MessageLevel.INFO -> information(event.message)
                    ConfigurationEvent.MessageLevel.SUCCESS -> information(event.message)
                    ConfigurationEvent.MessageLevel.ERROR -> error(event.message)
                }
            }
        }
    }
}

