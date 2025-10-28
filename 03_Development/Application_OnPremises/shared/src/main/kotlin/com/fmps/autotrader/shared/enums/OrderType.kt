package com.fmps.autotrader.shared.enums

import kotlinx.serialization.Serializable

/**
 * Order types supported by exchanges.
 */
@Serializable
enum class OrderType {
    MARKET,      // Execute immediately at current market price
    LIMIT,       // Execute only at specified price or better
    STOP_LOSS,   // Trigger market order when price reaches stop price
    STOP_LIMIT,  // Trigger limit order when price reaches stop price
    TAKE_PROFIT, // Trigger order to take profit at specified price
    TRAILING_STOP // Dynamic stop loss that trails price
}

