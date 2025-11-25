package com.fmps.autotrader.desktop.patterns

import com.fmps.autotrader.desktop.components.ToolbarButton
import com.fmps.autotrader.desktop.mvvm.BaseView
import com.fmps.autotrader.desktop.services.PatternDetail
import com.fmps.autotrader.desktop.services.PatternPerformanceStatus
import com.fmps.autotrader.desktop.services.PatternSummary
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.chart.AreaChart
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.Slider
import javafx.scene.control.TextField
import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.util.Callback
import tornadofx.action
import tornadofx.borderpane
import tornadofx.error
import tornadofx.hbox
import tornadofx.information
import tornadofx.label
import tornadofx.scrollpane
import tornadofx.vbox

class PatternAnalyticsView :
    BaseView<PatternAnalyticsState, PatternAnalyticsEvent, PatternAnalyticsViewModel>(PatternAnalyticsViewModel::class) {

    private val patternList = ListView<PatternSummary>()
    private val detailContainer = VBox()
    private val searchField = TextField()
    private val exchangeFilter = ComboBox<String>()
    private val timeframeFilter = ComboBox<String>()
    private val statusFilter = ComboBox<PatternPerformanceStatus>()
    private val successSlider = Slider(0.0, 100.0, 0.0)
    private val refreshButton = ToolbarButton("Refresh", icon = "⟳")
    private val primaryChart = AreaChart<String, Number>(CategoryAxis(), NumberAxis())

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
        padding = Insets(20.0)
        top = buildFilters()
        center = buildContent()
    }

    private fun buildFilters() = HBox(12.0).apply {
        alignment = Pos.CENTER_LEFT
        searchField.promptText = "Search patterns..."
        searchField.textProperty().addListener { _, _, new -> viewModel.updateSearch(new.orEmpty()) }
        searchField.apply { prefWidth = 200.0 }
        searchField.safeAddTo(this)

        exchangeFilter.promptText = "Exchange"
        exchangeFilter.items.addAll("All", "Binance", "Bitget", "Coinbase")
        exchangeFilter.selectionModel.selectFirst()
        exchangeFilter.selectionModel.selectedItemProperty().addListener { _, _, new ->
            val value = new.takeUnless { it == null || it == "All" }
            viewModel.updateExchange(value)
        }
        exchangeFilter.safeAddTo(this)

        timeframeFilter.promptText = "Timeframe"
        timeframeFilter.items.addAll("All", "5m", "15m", "1h", "4h")
        timeframeFilter.selectionModel.selectFirst()
        timeframeFilter.selectionModel.selectedItemProperty().addListener { _, _, new ->
            val value = new.takeUnless { it == null || it == "All" }
            viewModel.updateTimeframe(value)
        }
        timeframeFilter.safeAddTo(this)

        statusFilter.promptText = "Status"
        statusFilter.items.addAll(PatternPerformanceStatus.values())
        statusFilter.selectionModel.selectedItemProperty().addListener { _, _, new ->
            viewModel.updateStatus(new)
        }
        statusFilter.safeAddTo(this)

        successSlider.isShowTickMarks = true
        successSlider.isShowTickLabels = true
        successSlider.majorTickUnit = 20.0
        successSlider.valueProperty().addListener { _, _, new -> viewModel.updateSuccessThreshold(new.toDouble()) }
        // Create sliderBox fresh each time to avoid duplicate children
        val sliderBox = VBox(4.0).apply {
            // Create label explicitly to avoid TornadoFX DSL auto-addition
            val minSuccessLabel = Label("Min Success %")
            children += minSuccessLabel
            // Ensure successSlider is removed from any previous parent before adding
            successSlider.safeAddTo(this)
        }
        sliderBox.safeAddTo(this)

        val spacer = Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)
        children += spacer

        refreshButton.action { viewModel.refresh() }
        refreshButton.safeAddTo(this)
    }

    private fun buildContent() = HBox(16.0).apply {
        HBox.setHgrow(this, Priority.ALWAYS)
        patternList.cellFactory = Callback { PatternCell() }
        patternList.selectionModel.selectedItemProperty().addListener { _, _, new ->
            new?.let { viewModel.selectPattern(it.id) }
        }
        patternList.prefWidth = 360.0

        val listPane = VBox(8.0).apply {
            // Create label explicitly to avoid TornadoFX DSL auto-addition
            val patternsLabel = Label("Patterns")
            patternsLabel.styleClass += "section-title"
            children += patternsLabel
            VBox.setVgrow(patternList, Priority.ALWAYS)
            patternList.safeAddTo(this)
        }
        listPane.prefWidth = 360.0
        listPane.safeAddTo(this)

        detailContainer.apply {
            spacing = 12.0
            padding = Insets(0.0, 0.0, 0.0, 12.0)
        }
        val detailScroll = scrollpane {
            content = detailContainer
            isFitToWidth = true
        }
        HBox.setHgrow(detailScroll, Priority.ALWAYS)
        detailScroll.safeAddTo(this)
    }

    override fun onStateChanged(state: PatternAnalyticsState) {
        refreshButton.isDisable = state.isRefreshing
        patternList.items.setAll(state.filteredPatterns)
        state.selectedPatternId?.let { id ->
            val match = state.filteredPatterns.firstOrNull { it.id == id }
            if (match != null) {
                patternList.selectionModel.select(match)
            }
        }
        renderDetail(state.selectedDetail)
    }

    private fun renderDetail(detail: PatternDetail?) {
        detailContainer.children.clear()
        if (detail == null) {
            detailContainer.children += label("Select a pattern to view analytics") { styleClass += "meta-muted" }
            return
        }

        detailContainer.children += hbox(12.0) {
            alignment = Pos.CENTER_LEFT
            label(detail.summary.name) { styleClass += "section-title" }
            label("${detail.summary.symbol} • ${detail.summary.timeframe}") { styleClass += "meta-muted" }
            val spacer = Region()
            HBox.setHgrow(spacer, Priority.ALWAYS)
            children += spacer
            children += ToolbarButton("Archive", icon = "🗂").apply { action { viewModel.archiveSelected() } }
            children += ToolbarButton("Delete", icon = "✖", emphasis = ToolbarButton.Emphasis.DANGER).apply { action { viewModel.deleteSelected() } }
        }

        detailContainer.children += buildKpiRow(detail)
        detailContainer.children += buildIndicators(detail)
        detailContainer.children += buildChart(detail)
    }

    private fun buildKpiRow(detail: PatternDetail) = HBox(12.0).apply {
        spacing = 12.0
        children += metric("Success Rate", "${detail.winRate.format(1)}%")
        children += metric("Profit Factor", detail.summary.profitFactor.format(2))
        children += metric("Avg PnL", "${detail.averagePnL.format(2)}%")
        children += metric("Drawdown", "${detail.drawdown.format(2)}%")
    }

    private fun metric(title: String, value: String) = VBox(4.0).apply {
        styleClass += "content-card"
        padding = Insets(12.0)
        label(title) { styleClass += "field-label" }
        label(value) { styleClass += "kpi-value" }
    }

    private fun buildIndicators(detail: PatternDetail) = VBox(8.0).apply {
        styleClass += "content-card"
        padding = Insets(12.0)
        label("Indicators & Criteria") { styleClass += "section-title" }
        hbox(16.0) {
            vbox(4.0) {
                label("Indicators") { styleClass += "field-label" }
                detail.indicators.forEach { label("• $it") }
            }
            vbox(4.0) {
                label("Entry") { styleClass += "field-label" }
                detail.entryCriteria.forEach { label("→ $it") }
            }
            vbox(4.0) {
                label("Exit") { styleClass += "field-label" }
                detail.exitCriteria.forEach { label("← $it") }
            }
        }
    }

    private fun buildChart(detail: PatternDetail) = VBox(8.0).apply {
        styleClass += "content-card"
        padding = Insets(12.0)
        label("Performance Over Time") { styleClass += "section-title" }
        primaryChart.data.clear()
        val successSeries = XYChart.Series<String, Number>().apply {
            name = "Success %"
            detail.performance.forEach { point ->
                data.add(XYChart.Data(point.timestamp.toString(), point.successRate))
            }
        }
        val profitSeries = XYChart.Series<String, Number>().apply {
            name = "Profit Factor"
            detail.performance.forEach { point ->
                data.add(XYChart.Data(point.timestamp.toString(), point.profitFactor))
            }
        }
        primaryChart.data.addAll(successSeries, profitSeries)
        primaryChart.createSymbols = false
        primaryChart.animated = false
        VBox.setVgrow(primaryChart, Priority.ALWAYS)
        children += primaryChart
    }

    override fun onEvent(event: PatternAnalyticsEvent) {
        when (event) {
            is PatternAnalyticsEvent.ShowMessage -> {
                when (event.type) {
                    PatternAnalyticsEvent.MessageType.INFO -> information(event.message)
                    PatternAnalyticsEvent.MessageType.SUCCESS -> information(event.message)
                    PatternAnalyticsEvent.MessageType.ERROR -> error(event.message)
                }
            }
        }
    }

    private inner class PatternCell : ListCell<PatternSummary>() {
        override fun updateItem(item: PatternSummary?, empty: Boolean) {
            super.updateItem(item, empty)
            if (empty || item == null) {
                graphic = null
                return
            }
            graphic = hbox(8.0) {
                padding = Insets(8.0)
                vbox(2.0) {
                    label(item.name) { styleClass += "trader-name" }
                    label("${item.symbol} • ${item.timeframe}") { styleClass += "meta-muted" }
                }
                val spacer = Region()
                HBox.setHgrow(spacer, Priority.ALWAYS)
                children += spacer
                vbox(2.0) {
                    alignment = Pos.CENTER_RIGHT
                    label("${item.successRate.format(1)}%") { styleClass += "kpi-value" }
                    label("PF ${item.profitFactor.format(2)}") { styleClass += "meta-muted" }
                }
            }
        }
    }

    private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)
}

