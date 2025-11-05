package com.fmps.autotrader.core.patterns

import com.fmps.autotrader.core.indicators.models.BollingerBandsResult
import com.fmps.autotrader.core.indicators.models.MACDResult
import com.fmps.autotrader.core.patterns.models.MarketConditions
import com.fmps.autotrader.core.patterns.models.TradingPattern
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.TradeAction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.time.Instant

class RelevanceCalculatorTest {
    private val calculator = RelevanceCalculator()
    
    @Test
    fun `test calculateRelevance - perfect match`() {
        val pattern = createPattern(
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT",
            conditions = mapOf(
                "RSI_Range" to Pair(60.0, 70.0),
                "MACD_Range" to Pair(0.001, 0.002)
            )
        )
        
        val conditions = createMarketConditions(
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT",
            indicators = mapOf(
                "RSI" to 65.0,
                "MACD" to MACDResult(0.0015, 0.001, 0.0005)
            )
        )
        
        val relevance = calculator.calculateRelevance(pattern, conditions)
        
        assertTrue(relevance > 0.7, "Relevance should be high for perfect match")
        assertTrue(relevance <= 1.0, "Relevance should not exceed 1.0")
    }
    
    @Test
    fun `test calculateRelevance - incompatible exchange`() {
        val pattern = createPattern(
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT"
        )
        
        val conditions = createMarketConditions(
            exchange = Exchange.BITGET,
            symbol = "BTCUSDT"
        )
        
        val relevance = calculator.calculateRelevance(pattern, conditions)
        
        assertEquals(0.0, relevance, "Relevance should be 0 for incompatible exchange")
    }
    
    @Test
    fun `test calculateRelevance - incompatible symbol`() {
        val pattern = createPattern(
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT"
        )
        
        val conditions = createMarketConditions(
            exchange = Exchange.BINANCE,
            symbol = "ETHUSDT"
        )
        
        val relevance = calculator.calculateRelevance(pattern, conditions)
        
        assertEquals(0.0, relevance, "Relevance should be 0 for incompatible symbol")
    }
    
    @Test
    fun `test calculateRelevance - RSI out of range`() {
        val pattern = createPattern(
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT",
            conditions = mapOf(
                "RSI_Range" to Pair(60.0, 70.0)
            )
        )
        
        val conditions = createMarketConditions(
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT",
            indicators = mapOf(
                "RSI" to 30.0 // Way outside range
            )
        )
        
        val relevance = calculator.calculateRelevance(pattern, conditions)
        
        assertTrue(relevance < 0.5, "Relevance should be low when RSI is out of range")
    }
    
    @Test
    fun `test calculateRelevance - high performance pattern`() {
        val pattern = createPattern(
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT",
            successRate = 0.9,
            usageCount = 10
        )
        
        val conditions = createMarketConditions(
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT"
        )
        
        val relevance = calculator.calculateRelevance(pattern, conditions)
        
        // High performance should boost relevance
        assertTrue(relevance > 0.0, "Relevance should be positive")
    }
    
    @Test
    fun `test calculateIndicatorSimilarity - RSI range matching`() {
        val pattern = createPattern(
            conditions = mapOf("RSI_Range" to Pair(60.0, 70.0))
        )
        
        val conditions = createMarketConditions(
            indicators = mapOf("RSI" to 65.0)
        )
        
        val relevance = calculator.calculateRelevance(pattern, conditions)
        
        assertTrue(relevance > 0.5, "Should match when RSI is in range")
    }
    
    @Test
    fun `test calculateIndicatorSimilarity - MACD matching`() {
        val pattern = createPattern(
            conditions = mapOf(
                "MACD_Range" to Pair(0.001, 0.002)
            )
        )
        
        val conditions = createMarketConditions(
            indicators = mapOf(
                "MACD" to MACDResult(0.0015, 0.001, 0.0005)
            )
        )
        
        val relevance = calculator.calculateRelevance(pattern, conditions)
        
        assertTrue(relevance > 0.0, "Should calculate relevance for MACD")
    }
    
    @Test
    fun `test calculateIndicatorSimilarity - Bollinger Bands matching`() {
        val pattern = createPattern(
            conditions = mapOf(
                "BollingerBands" to BollingerBandsResult(
                    upper = 50000.0,
                    middle = 49000.0,
                    lower = 48000.0,
                    bandwidth = 0.04,
                    percentB = 0.5
                )
            )
        )
        
        val conditions = createMarketConditions(
            currentPrice = BigDecimal("49000"),
            indicators = mapOf(
                "BollingerBands" to BollingerBandsResult(
                    upper = 50100.0,
                    middle = 49100.0,
                    lower = 48100.0,
                    bandwidth = 0.0407, // (50100 - 48100) / 49100
                    percentB = 0.5
                )
            )
        )
        
        val relevance = calculator.calculateRelevance(pattern, conditions)
        
        assertTrue(relevance > 0.0, "Should calculate relevance for Bollinger Bands")
    }
    
    @Test
    fun `test calculatePerformanceScore - high success rate`() {
        val pattern = createPattern(
            successRate = 0.9,
            usageCount = 10
        )
        
        val conditions = createMarketConditions()
        
        val relevance = calculator.calculateRelevance(pattern, conditions)
        
        // Performance should contribute to relevance
        assertTrue(relevance > 0.0)
    }
    
    @Test
    fun `test calculateRecencyScore - recently used pattern`() {
        val pattern = createPattern(
            lastUsedAt = Instant.now().minusSeconds(3600) // 1 hour ago
        )
        
        val conditions = createMarketConditions()
        
        val relevance = calculator.calculateRelevance(pattern, conditions)
        
        // Recent usage should boost relevance
        assertTrue(relevance > 0.0)
    }
    
    @Test
    fun `test calculateRecencyScore - old pattern`() {
        val pattern = createPattern(
            lastUsedAt = Instant.now().minusSeconds(100 * 24 * 3600) // 100 days ago
        )
        
        val conditions = createMarketConditions()
        
        val relevance = calculator.calculateRelevance(pattern, conditions)
        
        // Old pattern should have lower relevance
        assertTrue(relevance >= 0.0)
    }
    
    @Test
    fun `test calculateIndicatorSimilarity - direct method`() {
        val similarity1 = calculator.calculateIndicatorSimilarity(65.0, 65.0, tolerance = 5.0)
        assertEquals(1.0, similarity1, 0.01, "Perfect match should be 1.0")
        
        val similarity2 = calculator.calculateIndicatorSimilarity(65.0, 70.0, tolerance = 5.0)
        assertTrue(similarity2 > 0.5, "Within tolerance should be high")
        
        val similarity3 = calculator.calculateIndicatorSimilarity(65.0, 100.0, tolerance = 5.0)
        assertTrue(similarity3 < 0.5, "Far outside tolerance should be low")
    }
    
    // Helper methods
    
    private fun createPattern(
        id: String = "test-pattern",
        exchange: Exchange = Exchange.BINANCE,
        symbol: String = "BTCUSDT",
        timeframe: String = "1h",
        action: TradeAction = TradeAction.LONG,
        conditions: Map<String, Any> = emptyMap(),
        confidence: Double = 0.7,
        createdAt: Instant = Instant.now(),
        lastUsedAt: Instant? = null,
        usageCount: Int = 0,
        successCount: Int = 0,
        successRate: Double = 0.0,
        averageReturn: BigDecimal = BigDecimal.ZERO
    ): TradingPattern {
        return TradingPattern(
            id = id,
            exchange = exchange,
            symbol = symbol,
            timeframe = timeframe,
            action = action,
            conditions = conditions,
            confidence = confidence,
            createdAt = createdAt,
            lastUsedAt = lastUsedAt,
            usageCount = usageCount,
            successCount = successCount,
            successRate = successRate,
            averageReturn = averageReturn
        )
    }
    
    private fun createMarketConditions(
        exchange: Exchange = Exchange.BINANCE,
        symbol: String = "BTCUSDT",
        currentPrice: BigDecimal = BigDecimal("50000"),
        indicators: Map<String, Any> = emptyMap(),
        timestamp: Instant = Instant.now()
    ): MarketConditions {
        return MarketConditions(
            exchange = exchange,
            symbol = symbol,
            currentPrice = currentPrice,
            indicators = indicators,
            timestamp = timestamp
        )
    }
}

