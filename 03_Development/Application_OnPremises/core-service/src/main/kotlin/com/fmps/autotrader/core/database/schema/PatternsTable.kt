package com.fmps.autotrader.core.database.schema

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Database table for successful trading patterns
 * 
 * Simple pattern storage for v1.0 (no ML/AI, just historical data)
 * Stores market conditions and outcomes for pattern matching
 */
object PatternsTable : Table("patterns") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100).nullable()
    val patternType = varchar("pattern_type", 50) // REVERSAL, CONTINUATION, BREAKOUT, etc.
    
    // Market Conditions
    val tradingPair = varchar("trading_pair", 20)
    val timeframe = varchar("timeframe", 10) // 1m, 5m, 15m, 1h, 4h, 1d
    val tradeType = varchar("trade_type", 10) // LONG, SHORT
    
    // Technical Indicator Ranges (for pattern matching)
    val rsiMin = decimal("rsi_min", 5, 2).nullable()
    val rsiMax = decimal("rsi_max", 5, 2).nullable()
    val macdMin = decimal("macd_min", 10, 4).nullable()
    val macdMax = decimal("macd_max", 10, 4).nullable()
    val volumeChangeMin = decimal("volume_change_min", 10, 4).nullable()
    val volumeChangeMax = decimal("volume_change_max", 10, 4).nullable()
    val priceChangeMin = decimal("price_change_min", 10, 6).nullable()
    val priceChangeMax = decimal("price_change_max", 10, 6).nullable()
    
    // Outcome Statistics
    val totalOccurrences = integer("total_occurrences").default(0)
    val successfulTrades = integer("successful_trades").default(0)
    val failedTrades = integer("failed_trades").default(0)
    val successRate = decimal("success_rate", 5, 2).default(0.toBigDecimal()) // Percentage
    val averageProfitLoss = decimal("average_profit_loss", 10, 6).default(0.toBigDecimal())
    val bestProfitLoss = decimal("best_profit_loss", 10, 6).default(0.toBigDecimal())
    val worstProfitLoss = decimal("worst_profit_loss", 10, 6).default(0.toBigDecimal())
    
    // Confidence Metrics
    val confidence = decimal("confidence", 5, 2).default(0.toBigDecimal()) // 0-100
    val minOccurrencesForConfidence = integer("min_occurrences").default(10)
    
    // Status
    val isActive = bool("is_active").default(true)
    
    // Metadata
    val description = text("description").nullable()
    val tags = varchar("tags", 500).nullable() // Comma-separated tags
    
    // Timestamps
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    val updatedAt = datetime("updated_at").clientDefault { LocalDateTime.now() }
    val lastMatchedAt = datetime("last_matched_at").nullable()
    
    override val primaryKey = PrimaryKey(id)
}

