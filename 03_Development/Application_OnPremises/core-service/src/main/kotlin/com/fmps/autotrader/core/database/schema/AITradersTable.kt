package com.fmps.autotrader.core.database.schema

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Database table for AI Trader instances
 * 
 * Stores configuration and state for up to 3 concurrent AI traders
 */
object AITradersTable : Table("ai_traders") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val status = varchar("status", 20) // ACTIVE, PAUSED, STOPPED
    
    // Trading Configuration
    val exchange = varchar("exchange", 20) // BINANCE, BITGET, TRADINGVIEW
    val tradingPair = varchar("trading_pair", 20) // e.g., BTC/USDT
    val leverage = integer("leverage").default(10)
    val initialBalance = decimal("initial_balance", 18, 8)
    val currentBalance = decimal("current_balance", 18, 8)
    
    // Risk Management
    val stopLossPercent = decimal("stop_loss_percent", 5, 4).default(0.02.toBigDecimal()) // 2%
    val takeProfitPercent = decimal("take_profit_percent", 5, 4).default(0.05.toBigDecimal()) // 5%
    val maxDailyLoss = decimal("max_daily_loss", 5, 4).default(0.10.toBigDecimal()) // 10%
    
    // Technical Indicators Config
    val rsiPeriod = integer("rsi_period").default(14)
    val rsiOverbought = integer("rsi_overbought").default(70)
    val rsiOversold = integer("rsi_oversold").default(30)
    val macdFast = integer("macd_fast").default(12)
    val macdSlow = integer("macd_slow").default(26)
    val macdSignal = integer("macd_signal").default(9)
    val smaShort = integer("sma_short").default(20)
    val smaLong = integer("sma_long").default(50)
    
    // Timestamps
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    val updatedAt = datetime("updated_at").clientDefault { LocalDateTime.now() }
    val lastActiveAt = datetime("last_active_at").nullable()
    
    override val primaryKey = PrimaryKey(id)
}

