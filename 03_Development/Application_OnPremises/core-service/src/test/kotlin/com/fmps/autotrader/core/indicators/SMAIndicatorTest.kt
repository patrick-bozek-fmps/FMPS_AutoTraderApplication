package com.fmps.autotrader.core.indicators

import com.fmps.autotrader.shared.enums.TimeFrame
import com.fmps.autotrader.shared.model.Candlestick
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant

class SMAIndicatorTest {
    
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
    fun `test SMA initialization with valid period`() {
        val sma = SMAIndicator(period = 10)
        assertEquals("SMA", sma.getName())
        assertEquals(10, sma.getRequiredDataPoints())
    }
    
    @Test
    fun `test SMA initialization fails with invalid period`() {
        assertThrows<IllegalArgumentException> {
            SMAIndicator(period = 0)
        }
        
        assertThrows<IllegalArgumentException> {
            SMAIndicator(period = -5)
        }
    }
    
    @Test
    fun `test SMA calculation with known values`() {
        // Test data: closes = [10, 11, 12, 13, 14]
        // SMA(5) = (10 + 11 + 12 + 13 + 14) / 5 = 60 / 5 = 12.0
        val candles = createTestCandles(listOf(10.0, 11.0, 12.0, 13.0, 14.0))
        val sma = SMAIndicator(period = 5)
        
        val result = sma.calculate(candles)
        
        assertNotNull(result)
        assertEquals(12.0, result!!, 0.0001)
    }
    
    @Test
    fun `test SMA with insufficient data returns null`() {
        val candles = createTestCandles(listOf(10.0, 11.0, 12.0))
        val sma = SMAIndicator(period = 5)
        
        val result = sma.calculate(candles)
        
        assertNull(result)
    }
    
    @Test
    fun `test SMA with exact required data points`() {
        val candles = createTestCandles(listOf(20.0, 22.0, 24.0, 26.0, 28.0))
        val sma = SMAIndicator(period = 5)
        
        val result = sma.calculate(candles)
        
        assertNotNull(result)
        assertEquals(24.0, result!!, 0.0001)
    }
    
    @Test
    fun `test SMA with more data than period uses last N values`() {
        // Data: [10, 20, 30, 40, 50, 60]
        // SMA(3) should use last 3 values: (40 + 50 + 60) / 3 = 50.0
        val candles = createTestCandles(listOf(10.0, 20.0, 30.0, 40.0, 50.0, 60.0))
        val sma = SMAIndicator(period = 3)
        
        val result = sma.calculate(candles)
        
        assertNotNull(result)
        assertEquals(50.0, result!!, 0.0001)
    }
    
    @Test
    fun `test SMA calculateAll returns correct series`() {
        val candles = createTestCandles(listOf(10.0, 20.0, 30.0, 40.0, 50.0))
        val sma = SMAIndicator(period = 3)
        
        val results = sma.calculateAll(candles)
        
        assertEquals(5, results.size)
        
        // First 2 values should be null (insufficient data)
        assertNull(results[0])
        assertNull(results[1])
        
        // SMA values for windows
        assertNotNull(results[2])
        assertEquals(20.0, results[2]!!, 0.0001)  // (10 + 20 + 30) / 3
        
        assertNotNull(results[3])
        assertEquals(30.0, results[3]!!, 0.0001)  // (20 + 30 + 40) / 3
        
        assertNotNull(results[4])
        assertEquals(40.0, results[4]!!, 0.0001)  // (30 + 40 + 50) / 3
    }
    
    @Test
    fun `test SMA with flat prices`() {
        val candles = createTestCandles(listOf(100.0, 100.0, 100.0, 100.0, 100.0))
        val sma = SMAIndicator(period = 5)
        
        val result = sma.calculate(candles)
        
        assertNotNull(result)
        assertEquals(100.0, result!!, 0.0001)
    }
    
    @Test
    fun `test SMA with volatile prices`() {
        val candles = createTestCandles(listOf(50.0, 100.0, 50.0, 100.0, 50.0))
        val sma = SMAIndicator(period = 5)
        
        val result = sma.calculate(candles)
        
        assertNotNull(result)
        assertEquals(70.0, result!!, 0.0001)  // (50 + 100 + 50 + 100 + 50) / 5
    }
    
    @Test
    fun `test SMA validateData with sufficient data`() {
        val candles = createTestCandles(listOf(10.0, 20.0, 30.0))
        val sma = SMAIndicator(period = 3)
        
        assertTrue(sma.validateData(candles))
    }
    
    @Test
    fun `test SMA validateData with insufficient data`() {
        val candles = createTestCandles(listOf(10.0, 20.0))
        val sma = SMAIndicator(period = 3)
        
        assertFalse(sma.validateData(candles))
    }
    
    @Test
    fun `test SMA with empty data returns null`() {
        val candles = emptyList<Candlestick>()
        val sma = SMAIndicator(period = 5)
        
        val result = sma.calculate(candles)
        
        assertNull(result)
    }
    
    @Test
    fun `test SMA with period of 1`() {
        val candles = createTestCandles(listOf(42.5))
        val sma = SMAIndicator(period = 1)
        
        val result = sma.calculate(candles)
        
        assertNotNull(result)
        assertEquals(42.5, result!!, 0.0001)
    }
    
    @Test
    fun `test SMA reset does not affect calculation`() {
        val candles = createTestCandles(listOf(10.0, 20.0, 30.0))
        val sma = SMAIndicator(period = 3)
        
        val result1 = sma.calculate(candles)
        sma.reset()
        val result2 = sma.calculate(candles)
        
        assertEquals(result1, result2)
    }
    
    @Test
    fun `test SMA toString`() {
        val sma = SMAIndicator(period = 20)
        val str = sma.toString()
        
        assertTrue(str.contains("SMA"))
        assertTrue(str.contains("20"))
    }
    
    @Test
    fun `test SMA extension function`() {
        val candles = createTestCandles(listOf(10.0, 20.0, 30.0, 40.0, 50.0))
        
        val result = candles.sma(period = 5)
        
        assertNotNull(result)
        assertEquals(30.0, result!!, 0.0001)
    }
    
    @Test
    fun `test SMA extension function with default period`() {
        val candles = createTestCandles(List(20) { (it + 1).toDouble() })
        
        val result = candles.sma()  // Default period = 20
        
        assertNotNull(result)
        assertEquals(10.5, result!!, 0.0001)  // Average of 1 to 20
    }
}

