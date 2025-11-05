package com.fmps.autotrader.core.patterns.models

import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.model.Candlestick
import java.math.BigDecimal
import java.time.Instant

/**
 * Represents current market conditions for pattern matching.
 * 
 * Contains all relevant market data needed to match against stored patterns:
 * - Current price and recent price movements
 * - Technical indicator values (RSI, MACD, SMA, EMA, Bollinger Bands)
 * - Market trend and volatility information
 * 
 * @property exchange Exchange identifier
 * @property symbol Trading pair (e.g., "BTCUSDT")
 * @property currentPrice Current market price
 * @property indicators Map of indicator names to their current values
 * @property candlesticks Recent candlestick data
 * @property timestamp When these conditions were captured
 */
data class MarketConditions(
    val exchange: Exchange,
    val symbol: String,
    val currentPrice: BigDecimal,
    val indicators: Map<String, Any>, // e.g., "RSI" -> 65.5, "MACD" -> MACDResult(...)
    val candlesticks: List<Candlestick> = emptyList(),
    val timestamp: Instant = Instant.now()
) {
    /**
     * Get indicator value by name
     */
    fun getIndicator(name: String): Any? = indicators[name]
    
    /**
     * Get RSI value if available
     */
    fun getRSI(): Double? = indicators["RSI"] as? Double
    
    /**
     * Get MACD result if available
     */
    fun getMACD(): com.fmps.autotrader.core.indicators.models.MACDResult? {
        return indicators["MACD"] as? com.fmps.autotrader.core.indicators.models.MACDResult
    }
    
    /**
     * Get SMA value if available
     */
    fun getSMA(period: Int? = null): Double? {
        val key = period?.let { "SMA_$it" } ?: "SMA"
        return indicators[key] as? Double
    }
    
    /**
     * Get EMA value if available
     */
    fun getEMA(period: Int? = null): Double? {
        val key = period?.let { "EMA_$it" } ?: "EMA"
        return indicators[key] as? Double
    }
    
    /**
     * Get Bollinger Bands result if available
     */
    fun getBollingerBands(): com.fmps.autotrader.core.indicators.models.BollingerBandsResult? {
        return indicators["BollingerBands"] as? com.fmps.autotrader.core.indicators.models.BollingerBandsResult
    }
}

