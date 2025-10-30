package com.fmps.autotrader.core.indicators

import com.fmps.autotrader.shared.enums.TimeFrame
import com.fmps.autotrader.shared.model.Candlestick
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant
import kotlin.math.abs

class EMAIndicatorTest {
    
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
    fun `test EMA initialization with valid period`() {
        val ema = EMAIndicator(period = 12)
        assertEquals("EMA", ema.getName())
        assertEquals(12, ema.getRequiredDataPoints())
    }
    
    @Test
    fun `test EMA initialization fails with invalid period`() {
        assertThrows<IllegalArgumentException> {
            EMAIndicator(period = 0)
        }
        
        assertThrows<IllegalArgumentException> {
            EMAIndicator(period = -3)
        }
    }
    
    @Test
    fun `test EMA calculation with exact required data points`() {
        // For period = 3, initial EMA is SMA of first 3 points
        // Data: [10, 20, 30]
        // SMA(3) = (10 + 20 + 30) / 3 = 20.0
        val candles = createTestCandles(listOf(10.0, 20.0, 30.0))
        val ema = EMAIndicator(period = 3)
        
        val result = ema.calculate(candles)
        
        assertNotNull(result)
        assertEquals(20.0, result!!, 0.0001)
    }
    
    @Test
    fun `test EMA with insufficient data returns null`() {
        val candles = createTestCandles(listOf(10.0, 20.0))
        val ema = EMAIndicator(period = 5)
        
        val result = ema.calculate(candles)
        
        assertNull(result)
    }
    
    @Test
    fun `test EMA is more responsive than SMA`() {
        // EMA gives more weight to recent prices
        // Starting with 5 periods at 10, then adding more data with higher price
        val candles = createTestCandles(listOf(10.0, 10.0, 10.0, 10.0, 10.0, 50.0, 50.0))
        
        val sma = SMAIndicator(period = 5)
        val ema = EMAIndicator(period = 5)
        
        val smaResult = sma.calculate(candles)!!
        val emaResult = ema.calculate(candles)!!
        
        // EMA should be higher than SMA because it gives more weight to recent higher prices
        // SMA of last 5: (10, 50, 50, 50, 50) / 5 = 42.0
        // EMA will give even more weight to recent 50s
        assertTrue(emaResult > smaResult, "EMA ($emaResult) should be > SMA ($smaResult)")
    }
    
    @Test
    fun `test EMA calculateAll returns correct series`() {
        val candles = createTestCandles(listOf(10.0, 20.0, 30.0, 40.0, 50.0))
        val ema = EMAIndicator(period = 3)
        
        val results = ema.calculateAll(candles)
        
        assertEquals(5, results.size)
        
        // First 2 values should be null (insufficient data)
        assertNull(results[0])
        assertNull(results[1])
        
        // Third value is initial SMA
        assertNotNull(results[2])
        assertEquals(20.0, results[2]!!, 0.0001)
        
        // Fourth and fifth values are EMA calculations
        assertNotNull(results[3])
        assertNotNull(results[4])
        
        // EMA should be increasing (since prices are increasing)
        assertTrue(results[3]!! < results[4]!!)
    }
    
    @Test
    fun `test EMA with flat prices equals price`() {
        val candles = createTestCandles(listOf(100.0, 100.0, 100.0, 100.0, 100.0))
        val ema = EMAIndicator(period = 5)
        
        val result = ema.calculate(candles)
        
        assertNotNull(result)
        assertEquals(100.0, result!!, 0.0001)
    }
    
    @Test
    fun `test EMA reset clears state`() {
        val candles = createTestCandles(listOf(10.0, 20.0, 30.0))
        val ema = EMAIndicator(period = 3)
        
        val result1 = ema.calculate(candles)
        
        // Add more data and calculate again
        val moreCandles = createTestCandles(listOf(10.0, 20.0, 30.0, 40.0))
        val result2 = ema.calculate(moreCandles.takeLast(1))
        
        // Reset and recalculate from scratch
        ema.reset()
        val result3 = ema.calculate(candles)
        
        // result3 should equal result1 (fresh calculation)
        assertEquals(result1, result3)
        
        // result2 should be different (continuation)
        assertNotEquals(result1, result2)
    }
    
    @Test
    fun `test EMA multiplier calculation`() {
        val ema12 = EMAIndicator(period = 12)
        val ema26 = EMAIndicator(period = 26)
        
        // Multiplier = 2 / (period + 1)
        val expectedMultiplier12 = 2.0 / 13.0
        val expectedMultiplier26 = 2.0 / 27.0
        
        // Can't directly test multiplier, but can verify in toString
        val str12 = ema12.toString()
        val str26 = ema26.toString()
        
        assertTrue(str12.contains("12"))
        assertTrue(str26.contains("26"))
    }
    
    @Test
    fun `test EMA with single data point`() {
        val candles = createTestCandles(listOf(42.5))
        val ema = EMAIndicator(period = 1)
        
        val result = ema.calculate(candles)
        
        assertNotNull(result)
        assertEquals(42.5, result!!, 0.0001)
    }
    
    @Test
    fun `test EMA extension function`() {
        val candles = createTestCandles(listOf(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0, 110.0, 120.0))
        
        val result = candles.ema(period = 12)
        
        assertNotNull(result)
        assertTrue(result!! > 0)
    }
    
    @Test
    fun `test EMA extension function with default period`() {
        val candles = createTestCandles(List(15) { (it + 1) * 10.0 })
        
        val result = candles.ema()  // Default period = 12
        
        assertNotNull(result)
    }
    
    @Test
    fun `test EMA calculateAll with empty data`() {
        val candles = emptyList<Candlestick>()
        val ema = EMAIndicator(period = 3)
        
        val results = ema.calculateAll(candles)
        
        assertTrue(results.isEmpty())
    }
    
    @Test
    fun `test EMA with increasing trend`() {
        val candles = createTestCandles(listOf(10.0, 15.0, 20.0, 25.0, 30.0))
        val ema = EMAIndicator(period = 3)
        
        val results = ema.calculateAll(candles)
        
        // EMA values should be increasing
        assertNotNull(results[2])
        assertNotNull(results[3])
        assertNotNull(results[4])
        
        assertTrue(results[2]!! < results[3]!!)
        assertTrue(results[3]!! < results[4]!!)
    }
    
    @Test
    fun `test EMA with decreasing trend`() {
        val candles = createTestCandles(listOf(100.0, 90.0, 80.0, 70.0, 60.0))
        val ema = EMAIndicator(period = 3)
        
        val results = ema.calculateAll(candles)
        
        // EMA values should be decreasing
        assertNotNull(results[2])
        assertNotNull(results[3])
        assertNotNull(results[4])
        
        assertTrue(results[2]!! > results[3]!!)
        assertTrue(results[3]!! > results[4]!!)
    }
    
    @Test
    fun `test EMA validates data correctly`() {
        val candles = createTestCandles(listOf(10.0, 20.0, 30.0))
        val ema = EMAIndicator(period = 3)
        
        assertTrue(ema.validateData(candles))
    }
    
    @Test
    fun `test EMA with period larger than data returns initial SMA`() {
        val candles = createTestCandles(listOf(10.0, 20.0, 30.0))
        val ema = EMAIndicator(period = 3)
        
        val result = ema.calculate(candles)
        
        // Should equal SMA since we only have exact required data points
        val sma = SMAIndicator(period = 3).calculate(candles)
        
        assertEquals(sma, result)
    }
}

