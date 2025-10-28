package com.fmps.autotrader.shared.enums

import kotlinx.serialization.Serializable

/**
 * Status of an AI Trader instance.
 */
@Serializable
enum class AITraderStatus {
    CREATED,    // Trader created but not started
    STARTING,   // Trader is starting up
    RUNNING,    // Trader is actively trading
    PAUSED,     // Trader is paused
    STOPPING,   // Trader is shutting down
    STOPPED,    // Trader is stopped
    ERROR       // Trader encountered an error
}

