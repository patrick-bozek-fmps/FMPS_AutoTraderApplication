package com.fmps.autotrader.core.indicators

import com.fmps.autotrader.shared.enums.TimeFrame
import com.fmps.autotrader.shared.model.Candlestick
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant

class RSIIndicatorTest {
    
    private fun createTestCandles(closes: List<Double>): List<Candlestick> {
        return closes.mapIndexed { index, close ->
            Candlestick(
                symbol = "TEST/USDT",
                interval = TimeFrame.ONE_HOUR,
                openTime = Instant.now().plusSeconds(index * 3600L),
                closeTime = Instant.now().plusSeconds((index + 1) * 3600L),
                open = BigDecimal.valueOf(close),
                high = BigDecimal.valueOf(close + 1),
                low = BigDecimal.valueOf(close - 1),
                close = BigDecimal.valueOf(close),
                volume = BigDecimal.valueOf(1000.0),
                quoteVolume = BigDecimal.valueOf(1000.0 * close)
            )
        }
    }
    
    @Test
    fun `test RSI initialization with valid period`() {
        val rsi = RSIIndicator(period = 14)
        assertEquals("RSI", rsi.getName())
        assertEquals(15, rsi.getRequiredDataPoints()) // period + 1
    }
    
    @Test
    fun `test RSI initialization fails with invalid period`() {
        assertThrows<IllegalArgumentException> {
            RSIIndicator(period = 0)
        }
        
        assertThrows<IllegalArgumentException> {
            RSIIndicator(period = -5)
        }
    }
    
    @Test
    fun `test RSI with insufficient data returns null`() {
        val candles = createTestCandles(listOf(10.0, 11.0, 12.0))
        val rsi = RSIIndicator(period = 14)
        
        val result = rsi.calculate(candles)
        
        assertNull(result)
    }
    
    @Test
    fun `test RSI with exact required data points`() {
        // 15 data points for RSI(14): 14 periods + 1 for price change calculation
        val closes = List(15) { 100.0 + it }
        val candles = createTestCandles(closes)
        val rsi = RSIIndicator(period = 14)
        
        val result = rsi.calculate(candles)
        
        assertNotNull(result)
        assertTrue(result!! in 0.0..100.0, "RSI should be between 0 and 100, got $result")
    }
    
    @Test
    fun `test RSI with all gains returns 100`() {
        // All prices increasing - should approach RSI = 100
        val closes = List(30) { 100.0 + it * 10.0 } // Strong uptrend
        val candles = createTestCandles(closes)
        val rsi = RSIIndicator(period = 14)
        
        val result = rsi.calculate(candles)
        
        assertNotNull(result)
        // With consistent gains, RSI should be very high (near 100)
        assertTrue(result!! > 90.0, "RSI with all gains should be > 90, got $result")
    }
    
    @Test
    fun `test RSI with all losses approaches 0`() {
        // All prices decreasing - should approach RSI = 0
        val closes = List(30) { 1000.0 - it * 10.0 } // Strong downtrend
        val candles = createTestCandles(closes)
        val rsi = RSIIndicator(period = 14)
        
        val result = rsi.calculate(candles)
        
        assertNotNull(result)
        // With consistent losses, RSI should be very low (near 0)
        assertTrue(result!! < 10.0, "RSI with all losses should be < 10, got $result")
    }
    
    @Test
    fun `test RSI with flat prices`() {
        // No price changes - all gains and losses are 0
        val closes = List(20) { 100.0 }
        val candles = createTestCandles(closes)
        val rsi = RSIIndicator(period = 14)
        
        val result = rsi.calculate(candles)
        
        // With no changes, average gain and loss are both 0
        // RS = 0 / 0, implementation returns 0 for this edge case
        assertNotNull(result)
        // Flat prices result in RSI = 0 (division by zero handled as 0)
        assertTrue(result!! >= 0.0 && result <= 100.0, "RSI should be in valid range 0-100")
    }
    
    @Test
    fun `test RSI with alternating gains and losses`() {
        // Alternating up/down - RSI should be around 50
        val closes = mutableListOf(100.0)
        for (i in 1..20) {
            closes.add(if (i % 2 == 0) closes.last() + 5.0 else closes.last() - 5.0)
        }
        val candles = createTestCandles(closes)
        val rsi = RSIIndicator(period = 14)
        
        val result = rsi.calculate(candles)
        
        assertNotNull(result)
        // With balanced gains/losses, RSI should be near 50
        assertTrue(result!! in 40.0..60.0, "RSI with balanced moves should be 40-60, got $result")
    }
    
    @Test
    fun `test RSI overbought threshold`() {
        // isOverbought uses > not >=, so 70.0 is NOT overbought
        assertTrue(RSIIndicator.isOverbought(75.0))
        assertTrue(RSIIndicator.isOverbought(70.1))
        assertFalse(RSIIndicator.isOverbought(70.0)) // Equal to threshold is not overbought
        assertFalse(RSIIndicator.isOverbought(69.9))
        assertFalse(RSIIndicator.isOverbought(50.0))
    }
    
    @Test
    fun `test RSI oversold threshold`() {
        // isOversold uses < not <=, so 30.0 is NOT oversold
        assertTrue(RSIIndicator.isOversold(25.0))
        assertTrue(RSIIndicator.isOversold(29.9))
        assertFalse(RSIIndicator.isOversold(30.0)) // Equal to threshold is not oversold
        assertFalse(RSIIndicator.isOversold(30.1))
        assertFalse(RSIIndicator.isOversold(50.0))
    }
    
    @Test
    fun `test RSI overbought with custom threshold`() {
        assertTrue(RSIIndicator.isOverbought(80.0, threshold = 75.0))
        assertFalse(RSIIndicator.isOverbought(70.0, threshold = 75.0))
    }
    
    @Test
    fun `test RSI oversold with custom threshold`() {
        assertTrue(RSIIndicator.isOversold(25.0, threshold = 35.0))
        assertFalse(RSIIndicator.isOversold(40.0, threshold = 35.0))
    }
    
    @Test
    fun `test RSI calculateAll returns correct series`() {
        val closes = List(20) { 100.0 + it * 5.0 }
        val candles = createTestCandles(closes)
        val rsi = RSIIndicator(period = 5)
        
        val results = rsi.calculateAll(candles)
        
        assertEquals(20, results.size)
        
        // First 5 values should be null (insufficient data)
        for (i in 0..4) {
            assertNull(results[i], "RSI[$i] should be null")
        }
        
        // Remaining values should be calculated
        for (i in 5 until results.size) {
            assertNotNull(results[i], "RSI[$i] should be calculated")
            assertTrue(results[i]!! in 0.0..100.0, "RSI should be 0-100, got ${results[i]}")
        }
    }
    
    @Test
    fun `test RSI validates data correctly`() {
        val candles = createTestCandles(List(15) { 100.0 + it })
        val rsi = RSIIndicator(period = 14)
        
        assertTrue(rsi.validateData(candles))
    }
    
    @Test
    fun `test RSI with insufficient data fails validation`() {
        val candles = createTestCandles(listOf(100.0, 101.0))
        val rsi = RSIIndicator(period = 14)
        
        assertFalse(rsi.validateData(candles))
    }
    
    @Test
    fun `test RSI reset does not affect calculation`() {
        val candles = createTestCandles(List(20) { 100.0 + it * 2.0 })
        val rsi = RSIIndicator(period = 14)
        
        val result1 = rsi.calculate(candles)
        rsi.reset()
        val result2 = rsi.calculate(candles)
        
        assertEquals(result1, result2)
    }
    
    @Test
    fun `test RSI toString`() {
        val rsi = RSIIndicator(period = 14)
        val str = rsi.toString()
        
        assertTrue(str.contains("RSI"))
        assertTrue(str.contains("14"))
    }
    
    @Test
    fun `test RSI extension function`() {
        val closes = List(20) { 100.0 + it * 3.0 }
        val candles = createTestCandles(closes)
        
        val result = candles.rsi(period = 14)
        
        assertNotNull(result)
        assertTrue(result!! in 0.0..100.0)
    }
    
    @Test
    fun `test RSI extension function with default period`() {
        val closes = List(20) { 100.0 + it * 2.0 }
        val candles = createTestCandles(closes)
        
        val result = candles.rsi() // Default period = 14
        
        assertNotNull(result)
        assertTrue(result!! in 0.0..100.0)
    }
    
    @Test
    fun `test isRSIOverbought extension function`() {
        // Create strong uptrend to get overbought RSI
        val closes = List(30) { 100.0 + it * 15.0 }
        val candles = createTestCandles(closes)
        
        val isOverbought = candles.isRSIOverbought()
        
        // With strong uptrend, RSI should be overbought
        assertTrue(isOverbought)
    }
    
    @Test
    fun `test isRSIOversold extension function`() {
        // Create strong downtrend to get oversold RSI
        val closes = List(30) { 1000.0 - it * 15.0 }
        val candles = createTestCandles(closes)
        
        val isOversold = candles.isRSIOversold()
        
        // With strong downtrend, RSI should be oversold
        assertTrue(isOversold)
    }
    
    @Test
    fun `test RSI with empty data returns null`() {
        val candles = emptyList<Candlestick>()
        val rsi = RSIIndicator(period = 14)
        
        val result = rsi.calculate(candles)
        
        assertNull(result)
    }
    
    @Test
    fun `test RSI with very small period`() {
        val closes = List(10) { 100.0 + it * 5.0 }
        val candles = createTestCandles(closes)
        val rsi = RSIIndicator(period = 2)
        
        val result = rsi.calculate(candles)
        
        assertNotNull(result)
        assertTrue(result!! in 0.0..100.0)
    }
    
    @Test
    fun `test RSI with volatile prices`() {
        // Create highly volatile price movement
        val closes = listOf(100.0, 110.0, 95.0, 120.0, 90.0, 130.0, 85.0, 
                           140.0, 80.0, 150.0, 75.0, 160.0, 70.0, 170.0, 65.0, 180.0)
        val candles = createTestCandles(closes)
        val rsi = RSIIndicator(period = 5)
        
        val result = rsi.calculate(candles)
        
        assertNotNull(result)
        assertTrue(result!! in 0.0..100.0)
    }
    
    @Test
    fun `test RSI calculateAll with empty data`() {
        val candles = emptyList<Candlestick>()
        val rsi = RSIIndicator(period = 14)
        
        val results = rsi.calculateAll(candles)
        
        assertTrue(results.isEmpty())
    }
}

