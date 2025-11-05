package com.fmps.autotrader.core.patterns.models

import java.math.BigDecimal

/**
 * Represents a pattern that matched current market conditions.
 * 
 * Contains the matched pattern along with relevance score and details
 * about which conditions matched.
 * 
 * @property pattern The trading pattern that matched
 * @property relevanceScore How well the pattern matches (0.0-1.0)
 * @property matchedConditions Map of which specific conditions matched
 * @property confidence Combined confidence (pattern confidence adjusted by relevance)
 */
data class MatchedPattern(
    val pattern: TradingPattern,
    val relevanceScore: Double, // 0.0-1.0, how well it matches
    val matchedConditions: Map<String, Any>, // Which conditions matched
    val confidence: Double // Combined: pattern confidence * relevance score
) {
    /**
     * Calculate final confidence for trading decision
     */
    fun getFinalConfidence(): Double {
        return (pattern.calculateConfidence() * 0.6 + relevanceScore * 0.4).coerceIn(0.0, 1.0)
    }
    
    /**
     * Check if match is strong enough for trading
     */
    fun isStrongMatch(threshold: Double = 0.7): Boolean {
        return relevanceScore >= threshold && confidence >= threshold
    }
}

