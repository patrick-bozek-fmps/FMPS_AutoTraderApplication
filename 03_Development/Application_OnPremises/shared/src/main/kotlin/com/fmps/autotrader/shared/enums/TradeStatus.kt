package com.fmps.autotrader.shared.enums

import kotlinx.serialization.Serializable

/**
 * Status of a trade or order.
 */
@Serializable
enum class TradeStatus {
    PENDING,          // Order created but not yet submitted
    SUBMITTED,        // Order submitted to exchange
    OPEN,             // Order is active on exchange
    PARTIALLY_FILLED, // Order partially filled
    FILLED,           // Order completely filled
    CANCELLED,        // Order cancelled
    REJECTED,         // Order rejected by exchange
    EXPIRED           // Order expired
}

