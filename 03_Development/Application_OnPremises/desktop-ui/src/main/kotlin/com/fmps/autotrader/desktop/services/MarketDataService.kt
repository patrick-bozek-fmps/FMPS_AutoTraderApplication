package com.fmps.autotrader.desktop.services

import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface MarketDataService {
    fun candlesticks(timeframe: Timeframe): Flow<List<Candlestick>>
    fun positions(): Flow<List<OpenPosition>>
    fun tradeHistory(): Flow<List<TradeRecord>>
}

enum class Timeframe(val label: String) {
    ONE_MIN("1m"),
    FIVE_MIN("5m"),
    FIFTEEN_MIN("15m"),
    ONE_HOUR("1h")
}

data class Candlestick(
    val timestamp: Instant,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class OpenPosition(
    val id: String,
    val traderName: String,
    val symbol: String,
    val size: Double,
    val entryPrice: Double,
    val markPrice: Double,
    val pnl: Double,
    val status: TraderStatus
)

data class TradeRecord(
    val id: String,
    val traderName: String,
    val symbol: String,
    val side: TradeSide,
    val qty: Double,
    val price: Double,
    val pnl: Double,
    val timestamp: Instant
)

enum class TradeSide { BUY, SELL }

