package com.fmps.autotrader.desktop.components

import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.Label
import javafx.scene.layout.VBox

class MetricTile(
    title: String,
    value: String,
    subtitle: String? = null
) : VBox() {

    val titleProperty = SimpleStringProperty(title)
    val valueProperty = SimpleStringProperty(value)
    val subtitleProperty = SimpleStringProperty(subtitle)

    private val titleLabel = Label().apply {
        styleClass += "metric-title"
        textProperty().bind(titleProperty)
    }
    private val valueLabel = Label().apply {
        styleClass += "metric-value"
        textProperty().bind(valueProperty)
    }
    private val subtitleLabel = Label().apply {
        styleClass += "metric-subtitle"
        textProperty().bind(subtitleProperty)
        isVisible = subtitle != null
        managedProperty().bind(visibleProperty())
    }

    init {
        styleClass += "metric-tile"
        children.addAll(titleLabel, valueLabel, subtitleLabel)
    }
}


