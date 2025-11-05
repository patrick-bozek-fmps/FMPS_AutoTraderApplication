package com.fmps.autotrader.core.patterns.models

import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.TradeAction
import java.time.Duration

/**
 * Criteria for querying patterns from the database.
 * 
 * All fields are optional - if not specified, that criterion is not applied.
 * Multiple criteria are combined with AND logic.
 * 
 * @property exchange Filter by exchange
 * @property symbol Filter by trading pair
 * @property action Filter by trade action (LONG/SHORT)
 * @property minSuccessRate Minimum success rate threshold
 * @property minUsageCount Minimum number of times pattern was used
 * @property minConfidence Minimum confidence level
 * @property maxAge Maximum age of pattern (older patterns excluded)
 * @property timeframe Filter by timeframe
 * @property tags Filter by tags (any tag in list matches)
 */
data class PatternCriteria(
    val exchange: Exchange? = null,
    val symbol: String? = null,
    val action: TradeAction? = null,
    val minSuccessRate: Double? = null,
    val minUsageCount: Int? = null,
    val minConfidence: Double? = null,
    val maxAge: Duration? = null,
    val timeframe: String? = null,
    val tags: List<String>? = null
) {
    /**
     * Check if criteria is empty (no filters)
     */
    fun isEmpty(): Boolean {
        return exchange == null && symbol == null && action == null &&
               minSuccessRate == null && minUsageCount == null &&
               minConfidence == null && maxAge == null &&
               timeframe == null && tags.isNullOrEmpty()
    }
}

