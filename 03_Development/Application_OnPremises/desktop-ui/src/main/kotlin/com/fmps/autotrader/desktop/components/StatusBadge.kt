package com.fmps.autotrader.desktop.components

import com.fmps.autotrader.desktop.services.TraderStatus
import javafx.scene.control.Label

class StatusBadge(
    initialStatus: TraderStatus,
    private val showText: Boolean = true
) : Label() {

    private var internalStatus: TraderStatus = initialStatus

    var status: TraderStatus
        get() = internalStatus
        set(value) {
            internalStatus = value
            updateAppearance()
        }

    private fun updateAppearance() {
        styleClass.removeIf { it.startsWith("status-") }
        styleClass += when (internalStatus) {
            TraderStatus.RUNNING -> "status-success"
            TraderStatus.STOPPED -> "status-idle"
            TraderStatus.ERROR -> "status-error"
        }
        text = if (showText) when (internalStatus) {
            TraderStatus.RUNNING -> "Running"
            TraderStatus.STOPPED -> "Stopped"
            TraderStatus.ERROR -> "Error"
        } else ""
    }

    init {
        styleClass += "status-badge"
        updateAppearance()
    }
}

