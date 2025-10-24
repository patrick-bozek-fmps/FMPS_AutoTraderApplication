package com.fmps.autotrader.core.database.schema

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Database table for trade history
 * 
 * Records all executed trades (demo/virtual money only for v1.0)
 */
object TradesTable : Table("trades") {
    val id = integer("id").autoIncrement()
    val aiTraderId = integer("ai_trader_id").references(AITradersTable.id)
    
    // Trade Details
    val tradeType = varchar("trade_type", 10) // LONG, SHORT
    val exchange = varchar("exchange", 20)
    val tradingPair = varchar("trading_pair", 20)
    val leverage = integer("leverage")
    
    // Entry
    val entryPrice = decimal("entry_price", 18, 8)
    val entryAmount = decimal("entry_amount", 18, 8)
    val entryTimestamp = datetime("entry_timestamp")
    val entryOrderId = varchar("entry_order_id", 100).nullable()
    
    // Exit
    val exitPrice = decimal("exit_price", 18, 8).nullable()
    val exitAmount = decimal("exit_amount", 18, 8).nullable()
    val exitTimestamp = datetime("exit_timestamp").nullable()
    val exitOrderId = varchar("exit_order_id", 100).nullable()
    val exitReason = varchar("exit_reason", 50).nullable() // TAKE_PROFIT, STOP_LOSS, MANUAL, SIGNAL
    
    // P&L
    val profitLoss = decimal("profit_loss", 18, 8).nullable()
    val profitLossPercent = decimal("profit_loss_percent", 10, 6).nullable()
    val fees = decimal("fees", 18, 8).default(0.toBigDecimal())
    
    // Risk Management
    val stopLossPrice = decimal("stop_loss_price", 18, 8)
    val takeProfitPrice = decimal("take_profit_price", 18, 8)
    val trailingStopActivated = bool("trailing_stop_activated").default(false)
    
    // Technical Indicators at Entry
    val rsiValue = decimal("rsi_value", 5, 2).nullable()
    val macdValue = decimal("macd_value", 10, 4).nullable()
    val smaShortValue = decimal("sma_short_value", 18, 8).nullable()
    val smaLongValue = decimal("sma_long_value", 18, 8).nullable()
    
    // Status
    val status = varchar("status", 20) // OPEN, CLOSED, CANCELLED
    
    // Metadata
    val notes = text("notes").nullable()
    val patternId = integer("pattern_id").references(PatternsTable.id).nullable()
    
    // Timestamps
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    val updatedAt = datetime("updated_at").clientDefault { LocalDateTime.now() }
    
    override val primaryKey = PrimaryKey(id)
}

