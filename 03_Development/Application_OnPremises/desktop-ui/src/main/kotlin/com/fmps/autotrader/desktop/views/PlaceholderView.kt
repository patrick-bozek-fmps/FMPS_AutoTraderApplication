package com.fmps.autotrader.desktop.views

import javafx.geometry.Insets
import javafx.scene.layout.VBox
import tornadofx.label
import tornadofx.vbox

open class PlaceholderView(
    heading: String,
    private val message: String
) : tornadofx.View(heading) {

    override val root: VBox = vbox(12.0) {
        padding = Insets(12.0)
        styleClass += "content-card"
        label(heading) {
            styleClass += "view-header"
        }
        label(message) {
            styleClass += "view-description"
            isWrapText = true
        }
    }
}


