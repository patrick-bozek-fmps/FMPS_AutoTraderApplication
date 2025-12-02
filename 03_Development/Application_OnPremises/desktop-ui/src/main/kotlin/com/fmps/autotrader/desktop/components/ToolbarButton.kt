package com.fmps.autotrader.desktop.components

import javafx.scene.control.Button

class ToolbarButton(
    text: String,
    icon: String? = null,
    private val emphasis: Emphasis = Emphasis.SECONDARY
) : Button(text) {

    enum class Emphasis {
        PRIMARY, SECONDARY, DANGER
    }

    init {
        styleClass += "toolbar-button"
        styleClass += when (emphasis) {
            Emphasis.PRIMARY -> "toolbar-button-primary"
            Emphasis.SECONDARY -> "toolbar-button-secondary"
            Emphasis.DANGER -> "toolbar-button-danger"
        }
        icon?.let { graphicText ->
            graphic = javafx.scene.control.Label(graphicText).apply {
                styleClass += "toolbar-button-icon"
                // Set font size explicitly for better emoji rendering
                style = "-fx-font-size: 18px;"
            }
            // Add spacing between icon and text using CSS
            style = "-fx-graphic-text-gap: 8px;"
        }
    }
}


