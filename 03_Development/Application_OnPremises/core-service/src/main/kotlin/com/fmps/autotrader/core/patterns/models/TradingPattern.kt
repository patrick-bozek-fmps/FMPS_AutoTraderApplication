package com.fmps.autotrader.core.patterns.models

import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.TradeAction
import java.math.BigDecimal
import java.time.Instant

/**
 * Represents a trading pattern extracted from successful trades.
 * 
 * Patterns capture market conditions (indicators, prices, etc.) that led to
 * successful trading outcomes. They are used to match current market conditions
 * and inform trading decisions.
 * 
 * @property id Unique pattern identifier
 * @property name Optional pattern name for identification
 * @property exchange Exchange where this pattern was observed
 * @property symbol Trading pair (e.g., "BTCUSDT")
 * @property timeframe Timeframe for pattern (e.g., "1h", "5m")
 * @property action Recommended trade action (LONG or SHORT)
 * @property conditions Market conditions that define this pattern (indicator values, ranges, etc.)
 * @property confidence Initial confidence level (0.0-1.0)
 * @property createdAt When this pattern was created
 * @property lastUsedAt When this pattern was last used in a trade
 * @property usageCount Number of times this pattern has been used
 * @property successCount Number of successful uses
 * @property successRate Success rate (successCount / usageCount)
 * @property averageReturn Average return per use
 */
data class TradingPattern(
    val id: String,
    val name: String? = null,
    val exchange: Exchange,
    val symbol: String,
    val timeframe: String,
    val action: TradeAction,
    val conditions: Map<String, Any>, // Indicator values, price ranges, etc.
    val confidence: Double = 0.5, // 0.0-1.0
    val createdAt: Instant = Instant.now(),
    val lastUsedAt: Instant? = null,
    val usageCount: Int = 0,
    val successCount: Int = 0,
    val successRate: Double = 0.0, // successCount / usageCount
    val averageReturn: BigDecimal = BigDecimal.ZERO,
    val description: String? = null,
    val tags: List<String> = emptyList()
) {
    /**
     * Calculate current confidence based on performance
     */
    fun calculateConfidence(): Double {
        if (usageCount == 0) return confidence
        
        // Confidence = base confidence * success rate
        // Higher usage with good success rate = higher confidence
        val performanceFactor = successRate
        val usageFactor = minOf(usageCount.toDouble() / 10.0, 1.0) // Cap at 10 uses
        
        return (confidence * 0.3 + performanceFactor * 0.5 + usageFactor * 0.2).coerceIn(0.0, 1.0)
    }
    
    /**
     * Check if pattern has sufficient data for reliable matching
     */
    fun isReliable(): Boolean {
        return usageCount >= 3 && successRate >= 0.5
    }
    
    /**
     * Get pattern age in days
     */
    fun getAgeDays(): Long {
        val duration = java.time.Duration.between(createdAt, Instant.now())
        return duration.toDays()
    }
}

