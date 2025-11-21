package com.fmps.autotrader.desktop.monitoring

import com.fmps.autotrader.desktop.components.ToolbarButton
import com.fmps.autotrader.desktop.mvvm.BaseView
import com.fmps.autotrader.desktop.services.ConnectionStatus
import com.fmps.autotrader.desktop.services.OpenPosition
import com.fmps.autotrader.desktop.services.Timeframe
import com.fmps.autotrader.desktop.services.TradeRecord
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.util.StringConverter
import tornadofx.action
import tornadofx.borderpane
import tornadofx.hbox
import tornadofx.information
import tornadofx.label
import tornadofx.vbox
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MonitoringView :
    BaseView<MonitoringState, MonitoringEvent, MonitoringViewModel>(MonitoringViewModel::class) {

    private val priceChart = LineChart<Number, Number>(NumberAxis(), NumberAxis())
    private val timeframePicker = ComboBox<Timeframe>()
    private val positionsTable = TableView<OpenPosition>()
    private val tradeTable = TableView<TradeRecord>()
    private val connectionChip = Label()
    private val lastUpdatedLabel = Label()
    private val latencyLabel = Label()
    private lateinit var refreshButton: ToolbarButton
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    override val root = borderpane {
        padding = Insets(20.0)
        top = buildHeader()
        center = buildContent()
    }

    // Helper to safely add a node, removing it from old parent first
    private fun Node.safeAddTo(parent: javafx.scene.layout.Pane) {
        this.parent?.let { (it as? javafx.scene.layout.Pane)?.children?.remove(this) }
        parent.children += this
    }

    private fun buildHeader(): VBox = vbox(8.0) {
        label("Trading Monitoring") { styleClass += "section-title" }
        val statusRow = hbox(12.0) {
            alignment = Pos.CENTER_LEFT
            connectionChip.styleClass.addAll("connection-chip", "connection-chip-good")
            connectionChip.safeAddTo(this)
            
            lastUpdatedLabel.styleClass += "meta-muted"
            lastUpdatedLabel.safeAddTo(this)
            
            latencyLabel.styleClass += "meta-muted"
            latencyLabel.safeAddTo(this)
            
            val spacer = Region()
            HBox.setHgrow(spacer, Priority.ALWAYS)
            children += spacer
        }
        children += statusRow
    }

    private fun buildContent() = HBox(16.0).apply {
        HBox.setHgrow(this, Priority.ALWAYS)
        val chartCard = VBox(10.0).apply {
            styleClass += "content-card"
            children += buildChartToolbar()
            setupChart()
            VBox.setVgrow(priceChart, Priority.ALWAYS)
            children += priceChart
        }
        HBox.setHgrow(chartCard, Priority.ALWAYS)
        children += chartCard

        val sidePanel = VBox(12.0).apply {
            prefWidth = 420.0
            children += buildPositionsCard()
            children += buildTradesCard()
        }
        children += sidePanel
    }

    private fun buildChartToolbar() = hbox(10.0) {
        alignment = Pos.CENTER_LEFT
        timeframePicker.items.addAll(Timeframe.values())
        timeframePicker.converter = object : StringConverter<Timeframe>() {
            override fun toString(timeframe: Timeframe?): String = timeframe?.label ?: ""
            override fun fromString(string: String?): Timeframe =
                Timeframe.values().first { it.label == string }
        }
        timeframePicker.selectionModel.selectedItemProperty().addListener { _, _, new ->
            new?.let { 
                // ViewModel will be available after onDock() is called
                try {
                    viewModel.changeTimeframe(it)
                } catch (e: IllegalStateException) {
                    // ViewModel not ready yet - this is normal during construction
                    // The listener will work once the View is docked
                }
            }
        }
        children += Label("Timeframe")
        children += timeframePicker
        refreshButton = ToolbarButton("Manual Refresh", icon = "⟳").apply {
            action { 
                // ViewModel will be available after onDock() is called
                try {
                    viewModel.refresh()
                } catch (e: IllegalStateException) {
                    // ViewModel not ready yet - this is normal during construction
                    // The button will work once the View is docked
                }
            }
        }
        children += refreshButton
    }

    private fun setupChart() {
        priceChart.title = "Price"
        priceChart.createSymbols = false
        priceChart.animated = false
        (priceChart.xAxis as NumberAxis).label = "Time"
        (priceChart.yAxis as NumberAxis).label = "Price"
    }

    private fun buildPositionsCard() = VBox(6.0).apply {
        styleClass += "content-card"
        label("Active Positions") { styleClass += "section-title" }
        positionsTable.apply {
            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            columns += createColumn("Trader", OpenPosition::traderName)
            columns += createColumn("Symbol", OpenPosition::symbol)
            columns += createColumn("Size", OpenPosition::size)
            columns += createColumn("Entry", OpenPosition::entryPrice)
            columns += createColumn("Mark", OpenPosition::markPrice)
            columns += createColumn("P&L", OpenPosition::pnl)
        }
        VBox.setVgrow(positionsTable, Priority.ALWAYS)
        children += positionsTable
    }

    private fun buildTradesCard() = VBox(6.0).apply {
        styleClass += "content-card"
        label("Recent Trades") { styleClass += "section-title" }
        tradeTable.apply {
            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            columns += createTradeColumn("Time") { record ->
                timeFormatter.format(record.timestamp)
            }
            columns += createTradeColumn("Trader") { it.traderName }
            columns += createTradeColumn("Symbol") { it.symbol }
            columns += createTradeColumn("Side") { it.side.name }
            columns += createTradeColumn("Qty") { "%.3f".format(it.qty) }
            columns += createTradeColumn("Price") { "%.2f".format(it.price) }
            columns += createTradeColumn("P&L") { "%.2f".format(it.pnl) }
        }
        VBox.setVgrow(tradeTable, Priority.ALWAYS)
        children += tradeTable
    }

    private fun <T> createColumn(title: String, extractor: (OpenPosition) -> T) =
        TableColumn<OpenPosition, T>(title).apply {
            setCellValueFactory { features -> javafx.beans.property.SimpleObjectProperty(extractor(features.value)) }
        }

    private fun createTradeColumn(title: String, extractor: (TradeRecord) -> String) =
        TableColumn<TradeRecord, String>(title).apply {
            setCellValueFactory { features ->
                javafx.beans.property.SimpleStringProperty(extractor(features.value))
            }
        }

    override fun onStateChanged(state: MonitoringState) {
        timeframePicker.selectionModel.select(state.timeframe)
        refreshButton.isDisable = state.isRefreshing
        updateConnectionChip(state.connectionStatus)
        updateMeta(state)
        positionsTable.items.setAll(state.positions)
        tradeTable.items.setAll(state.trades)
        updateChart(state)
    }

    private fun updateChart(state: MonitoringState) {
        val series = XYChart.Series<Number, Number>().apply {
            name = state.timeframe.label
            state.candles.forEachIndexed { index, candle ->
                data.add(XYChart.Data(index, candle.close))
            }
        }
        priceChart.data.setAll(series)
    }

    override fun onEvent(event: MonitoringEvent) {
        when (event) {
            is MonitoringEvent.ShowMessage -> information(event.message)
        }
    }

    private fun updateMeta(state: MonitoringState) {
        lastUpdatedLabel.text = "Updated: ${
            state.lastUpdated?.let { timeFormatter.format(it) } ?: "--"
        }"
        latencyLabel.text = "Latency: ${state.latencyMs} ms"
    }

    private fun updateConnectionChip(status: ConnectionStatus) {
        val classNames = listOf("connection-chip-good", "connection-chip-warn", "connection-chip-error")
        connectionChip.styleClass.removeAll(classNames)
        connectionChip.text = "Status: ${status.name.lowercase().replaceFirstChar { it.uppercase() }}"
        when (status) {
            ConnectionStatus.CONNECTED -> connectionChip.styleClass += "connection-chip-good"
            ConnectionStatus.RECONNECTING -> connectionChip.styleClass += "connection-chip-warn"
            ConnectionStatus.DISCONNECTED -> connectionChip.styleClass += "connection-chip-error"
        }
    }
}

