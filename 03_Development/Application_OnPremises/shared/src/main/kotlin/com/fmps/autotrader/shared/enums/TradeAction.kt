package com.fmps.autotrader.shared.enums

import kotlinx.serialization.Serializable

/**
 * Trade action (direction).
 */
@Serializable
enum class TradeAction {
    LONG,  // Buy / Long position
    SHORT  // Sell / Short position
}

