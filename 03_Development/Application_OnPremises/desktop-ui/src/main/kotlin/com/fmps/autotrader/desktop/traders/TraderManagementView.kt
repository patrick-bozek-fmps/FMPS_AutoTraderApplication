package com.fmps.autotrader.desktop.traders

import com.fmps.autotrader.desktop.components.ToolbarButton
import com.fmps.autotrader.desktop.mvvm.BaseView
import com.fmps.autotrader.desktop.services.TraderDetail
import com.fmps.autotrader.desktop.services.TraderRiskLevel
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
import tornadofx.error
import tornadofx.information
import java.text.DecimalFormat

class TraderManagementView :
    BaseView<TraderManagementState, TraderManagementEvent, TraderManagementViewModel>(TraderManagementViewModel::class) {

    private val traderTable = TableView<TraderDetail>()
    private val searchField = TextField()
    private val statusFilter = ComboBox<TraderStatusFilter>()
    private val nameField = TextField()
    private val exchangeField = ComboBox<String>()
    private val strategyField = ComboBox<String>()
    private val riskField = ComboBox<TraderRiskLevel>()
    private val baseAssetField = TextField()
    private val quoteAssetField = TextField()
    private val budgetField = TextField()
    private val apiKeyField = TextField()
    private val apiSecretField = TextField()
    private val apiPassphraseField = TextField()
    private val validationLabel = Label()
    private val isUpdatingForm = SimpleBooleanProperty(false)

    private val exchanges = listOf("Binance", "Bitget")
    private val strategies = listOf("Momentum", "Mean Reversion", "Arbitrage", "Scalping")

    override val root: BorderPane = BorderPane().apply {
        padding = Insets(20.0)
        left = buildSidebar()
        center = buildContent()
    }

    private fun buildSidebar(): VBox = VBox(12.0).apply {
        prefWidth = 320.0
        padding = Insets(0.0, 20.0, 0.0, 0.0)

        children += Label("Filters").apply { styleClass += "section-title" }
        searchField.promptText = "Search traders..."
        searchField.textProperty().addListener { _, _, newValue ->
            viewModel.updateSearch(newValue.orEmpty())
        }
        children += searchField

        statusFilter.items.addAll(TraderStatusFilter.values())
        statusFilter.selectionModel.select(TraderStatusFilter.ALL)
        statusFilter.selectionModel.selectedItemProperty().addListener { _, _, new ->
            new?.let { viewModel.updateStatusFilter(it) }
        }
        statusFilter.prefWidth = Double.MAX_VALUE
        children += statusFilter

        children += Separator()

        children += Label("Actions").apply { styleClass += "section-title" }
        val buttonRow = HBox(10.0).apply {
            children += ToolbarButton("New Trader", icon = "＋").apply {
                action { viewModel.newTrader() }
            }
            children += ToolbarButton("Delete", icon = "✖", emphasis = ToolbarButton.Emphasis.DANGER).apply {
                disableProperty().bind(Bindings.isNull(traderTable.selectionModel.selectedItemProperty()))
                action { viewModel.deleteSelectedTrader() }
            }
        }
        children += buttonRow

        val spacer = Region()
        VBox.setVgrow(spacer, Priority.ALWAYS)
        children += spacer
    }

    private fun buildContent(): HBox = HBox(20.0).apply {
        HBox.setHgrow(this, Priority.ALWAYS)
        traderTable.apply {
            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            prefWidth = 520.0
            columns += createColumn("Name", TraderDetail::name)
            columns += createColumn("Exchange", TraderDetail::exchange)
            columns += createColumn("Strategy", TraderDetail::strategy)
            columns += statusColumn()
            columns += profitColumn()
            selectionModel.selectedItemProperty().addListener { _, _, new ->
                viewModel.selectTrader(new)
            }
        }
        children += traderTable

        val form = buildForm()
        HBox.setHgrow(form, Priority.ALWAYS)
        children += form
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

    private fun buildForm(): VBox = VBox(10.0).apply {
        styleClass += "content-card"
        padding = Insets(16.0)

        children += Label("Trader Details").apply { styleClass += "section-title" }

        nameField.promptText = "Name"
        nameField.textProperty().addListener { _, _, new ->
            if (!isUpdatingForm.get()) viewModel.updateForm { it.copy(name = new.orEmpty()) }
        }
        children += labeledField("Name", nameField)

        exchangeField.items.addAll(exchanges)
        exchangeField.selectionModel.selectFirst()
        exchangeField.selectionModel.selectedItemProperty().addListener { _, _, new ->
            if (!isUpdatingForm.get() && new != null) viewModel.updateForm { it.copy(exchange = new) }
        }
        children += labeledField("Exchange", exchangeField)

        strategyField.items.addAll(strategies)
        strategyField.selectionModel.selectFirst()
        strategyField.selectionModel.selectedItemProperty().addListener { _, _, new ->
            if (!isUpdatingForm.get() && new != null) viewModel.updateForm { it.copy(strategy = new) }
        }
        children += labeledField("Strategy", strategyField)

        riskField.items.addAll(TraderRiskLevel.values())
        riskField.selectionModel.select(TraderRiskLevel.BALANCED)
        riskField.selectionModel.selectedItemProperty().addListener { _, _, new ->
            if (!isUpdatingForm.get() && new != null) viewModel.updateForm { it.copy(riskLevel = new) }
        }
        children += labeledField("Risk Profile", riskField)

        baseAssetField.textProperty().addListener { _, _, new ->
            if (!isUpdatingForm.get()) viewModel.updateForm { it.copy(baseAsset = new.orEmpty()) }
        }
        children += labeledField("Base Asset", baseAssetField)

        quoteAssetField.textProperty().addListener { _, _, new ->
            if (!isUpdatingForm.get()) viewModel.updateForm { it.copy(quoteAsset = new.orEmpty()) }
        }
        children += labeledField("Quote Asset", quoteAssetField)

        budgetField.textFormatter = TextFormatter(object : StringConverter<Double>() {
            override fun toString(value: Double?): String = value?.toString() ?: ""
            override fun fromString(string: String?): Double = string?.toDoubleOrNull() ?: 0.0
        })
        budgetField.textProperty().addListener { _, _, new ->
            if (!isUpdatingForm.get()) {
                val amount = new?.toDoubleOrNull() ?: 0.0
                viewModel.updateForm { it.copy(budget = amount) }
            }
        }
        children += labeledField("Budget (USDT)", budgetField)

        children += Separator()
        children += Label("Exchange API Credentials (Optional)").apply { styleClass += "section-title" }

        apiKeyField.promptText = "API Key"
        apiKeyField.textProperty().addListener { _, _, new ->
            if (!isUpdatingForm.get()) viewModel.updateForm { it.copy(apiKey = new.orEmpty()) }
        }
        children += labeledField("API Key", apiKeyField)

        apiSecretField.promptText = "API Secret"
        apiSecretField.textProperty().addListener { _, _, new ->
            if (!isUpdatingForm.get()) viewModel.updateForm { it.copy(apiSecret = new.orEmpty()) }
        }
        children += labeledField("API Secret", apiSecretField)

        apiPassphraseField.promptText = "Passphrase (Bitget only)"
        apiPassphraseField.textProperty().addListener { _, _, new ->
            if (!isUpdatingForm.get()) viewModel.updateForm { it.copy(apiPassphrase = new.orEmpty()) }
        }
        children += labeledField("Passphrase", apiPassphraseField)

        validationLabel.styleClass += "validation-label"
        children += validationLabel

        val buttonRow = HBox(10.0).apply {
            alignment = Pos.CENTER_RIGHT
            children += ToolbarButton("Save", icon = "💾", emphasis = ToolbarButton.Emphasis.PRIMARY).apply {
                action { viewModel.saveTrader() }
            }
            children += ToolbarButton("Start", icon = "▶").apply {
                disableProperty().bind(Bindings.isNull(traderTable.selectionModel.selectedItemProperty()))
                action { traderTable.selectionModel.selectedItem?.let { viewModel.startTrader(it.id) } }
            }
            children += ToolbarButton("Stop", icon = "■", emphasis = ToolbarButton.Emphasis.SECONDARY).apply {
                disableProperty().bind(Bindings.isNull(traderTable.selectionModel.selectedItemProperty()))
                action { traderTable.selectionModel.selectedItem?.let { viewModel.stopTrader(it.id) } }
            }
        }
        children += buttonRow
    }

    private fun labeledField(labelText: String, field: javafx.scene.Node) = VBox(4.0).apply {
        children += Label(labelText).apply { styleClass += "field-label" }
        children += field
    }

    override fun onStateChanged(state: TraderManagementState) {
        traderTable.items.setAll(state.filteredTraders)
        val selected = state.traders.firstOrNull { it.id == state.selectedTraderId }
        if (selected != null && traderTable.selectionModel.selectedItem != selected) {
            traderTable.selectionModel.select(selected)
        } else if (selected == null) {
            traderTable.selectionModel.clearSelection()
        }

        if (statusFilter.selectionModel.selectedItem != state.statusFilter) {
            statusFilter.selectionModel.select(state.statusFilter)
        }
        if (searchField.text != state.searchQuery) {
            searchField.text = state.searchQuery
        }

        isUpdatingForm.set(true)
        nameField.text = state.form.name
        exchangeField.selectionModel.select(state.form.exchange.takeIf { exchanges.contains(it) } ?: exchanges.first())
        strategyField.selectionModel.select(state.form.strategy.takeIf { strategies.contains(it) } ?: strategies.first())
        riskField.selectionModel.select(state.form.riskLevel)
        baseAssetField.text = state.form.baseAsset
        quoteAssetField.text = state.form.quoteAsset
        budgetField.text = if (state.form.budget == 0.0) "" else state.form.budget.toString()
        apiKeyField.text = state.form.apiKey
        apiSecretField.text = state.form.apiSecret
        apiPassphraseField.text = state.form.apiPassphrase
        validationLabel.text = state.form.errors.values.joinToString("\n")
        validationLabel.isVisible = state.form.errors.isNotEmpty()
        isUpdatingForm.set(false)
    }

    override fun onEvent(event: TraderManagementEvent) {
        when (event) {
            is TraderManagementEvent.ShowMessage -> {
                when (event.type) {
                    TraderManagementEvent.MessageType.ERROR -> error(event.message)
                    TraderManagementEvent.MessageType.SUCCESS -> information(event.message)
                    TraderManagementEvent.MessageType.INFO -> information(event.message)
                }
            }
        }
    }
}

