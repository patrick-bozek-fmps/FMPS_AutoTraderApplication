package com.fmps.autotrader.core.patterns.models

import java.time.Duration

/**
 * Criteria for pruning (removing) patterns from the database.
 * 
 * Patterns matching any of these criteria will be removed:
 * - Too old (maxAge)
 * - Low success rate (below minSuccessRate)
 * - Low usage (below minUsageCount)
 * - Keep only top N patterns (maxPatterns)
 * 
 * @property maxAge Remove patterns older than this
 * @property minSuccessRate Remove patterns with success rate below this
 * @property minUsageCount Remove patterns with usage count below this
 * @property maxPatterns Keep only top N patterns by performance
 */
data class PruneCriteria(
    val maxAge: Duration? = null,
    val minSuccessRate: Double? = null,
    val minUsageCount: Int? = null,
    val maxPatterns: Int? = null
) {
    /**
     * Check if criteria is empty (no pruning)
     */
    fun isEmpty(): Boolean {
        return maxAge == null && minSuccessRate == null &&
               minUsageCount == null && maxPatterns == null
    }
}

