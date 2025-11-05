package com.fmps.autotrader.core.patterns.models

import java.math.BigDecimal
import java.time.Instant

/**
 * Represents the outcome of using a pattern in a trade.
 * 
 * Used to update pattern performance statistics after a trade is closed.
 * 
 * @property patternId ID of the pattern that was used
 * @property success Whether the trade was successful (profit > 0)
 * @property returnAmount Profit or loss amount
 * @property timestamp When the trade was closed
 */
data class TradeOutcome(
    val patternId: String,
    val success: Boolean,
    val returnAmount: BigDecimal,
    val timestamp: Instant = Instant.now()
)

