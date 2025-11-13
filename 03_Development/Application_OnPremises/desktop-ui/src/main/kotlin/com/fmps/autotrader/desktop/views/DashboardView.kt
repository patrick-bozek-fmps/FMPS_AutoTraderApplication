package com.fmps.autotrader.desktop.views

import javafx.geometry.Insets
import javafx.scene.layout.VBox
import tornadofx.label
import tornadofx.vbox

class DashboardView : tornadofx.View("Overview") {
    override val root: VBox = vbox(12.0) {
        padding = Insets(12.0)
        styleClass += "content-card"
        label("Desktop UI Foundation") {
            styleClass += "view-header"
        }
        label("Use the navigation items to the left to explore modules as they are implemented.") {
            styleClass += "view-description"
            isWrapText = true
        }
    }
}


