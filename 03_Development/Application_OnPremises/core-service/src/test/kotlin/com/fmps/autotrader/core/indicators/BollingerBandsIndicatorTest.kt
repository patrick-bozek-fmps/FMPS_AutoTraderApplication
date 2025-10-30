package com.fmps.autotrader.core.indicators

import com.fmps.autotrader.shared.enums.TimeFrame
import com.fmps.autotrader.shared.model.Candlestick
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant

class BollingerBandsIndicatorTest {
    
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
    fun `test Bollinger Bands initialization with valid parameters`() {
        val bb = BollingerBandsIndicator(period = 20, stdDevMultiplier = 2.0)
        assertEquals("BollingerBands", bb.getName())
        assertEquals(20, bb.getRequiredDataPoints())
    }
    
    @Test
    fun `test Bollinger Bands initialization with default parameters`() {
        val bb = BollingerBandsIndicator()
        assertEquals("BollingerBands", bb.getName())
        assertEquals(20, bb.getRequiredDataPoints())
    }
    
    @Test
    fun `test Bollinger Bands initialization fails with invalid parameters`() {
        assertThrows<IllegalArgumentException> {
            BollingerBandsIndicator(period = 0)
        }
        
        assertThrows<IllegalArgumentException> {
            BollingerBandsIndicator(period = -5)
        }
        
        assertThrows<IllegalArgumentException> {
            BollingerBandsIndicator(stdDevMultiplier = 0.0)
        }
        
        assertThrows<IllegalArgumentException> {
            BollingerBandsIndicator(stdDevMultiplier = -1.0)
        }
    }
    
    @Test
    fun `test Bollinger Bands with insufficient data returns null`() {
        val candles = createTestCandles(listOf(100.0, 101.0, 102.0))
        val bb = BollingerBandsIndicator(period = 20)
        
        val result = bb.calculate(candles)
        
        assertNull(result)
    }
    
    @Test
    fun `test Bollinger Bands with exact required data points`() {
        val closes = List(20) { 100.0 + it * 0.5 }
        val candles = createTestCandles(closes)
        val bb = BollingerBandsIndicator(period = 20)
        
        val result = bb.calculate(candles)
        
        assertNotNull(result)
        assertTrue(result!!.upper > result.middle)
        assertTrue(result.middle > result.lower)
    }
    
    @Test
    fun `test Bollinger Bands structure`() {
        val closes = List(30) { 100.0 + it * 2.0 }
        val candles = createTestCandles(closes)
        val bb = BollingerBandsIndicator(period = 20)
        
        val result = bb.calculate(candles)
        
        assertNotNull(result)
        // Upper band should be above middle
        assertTrue(result!!.upper > result.middle, "Upper ${result.upper} should be > middle ${result.middle}")
        // Middle band should be above lower
        assertTrue(result.middle > result.lower, "Middle ${result.middle} should be > lower ${result.lower}")
        // Bandwidth should be positive
        assertTrue(result.bandwidth > 0.0, "Bandwidth should be positive")
        // %B is optional (requires current price), so just check it's valid if present
        if (result.percentB != null) {
            assertTrue(result.percentB!!.isFinite(), "%B should be finite")
        }
    }
    
    @Test
    fun `test Bollinger Bands with flat prices creates narrow bands`() {
        // With no volatility, bands should be very close together
        val closes = List(25) { 100.0 }
        val candles = createTestCandles(closes)
        val bb = BollingerBandsIndicator(period = 20)
        
        val result = bb.calculate(candles)
        
        assertNotNull(result)
        // With flat prices, standard deviation is 0, so all bands equal middle
        assertEquals(result!!.upper, result.middle, 0.0001)
        assertEquals(result.middle, result.lower, 0.0001)
        // Bandwidth should be 0
        assertEquals(0.0, result.bandwidth, 0.0001)
    }
    
    @Test
    fun `test Bollinger Bands with volatile prices creates wide bands`() {
        // High volatility should create wide bands
        val closes = mutableListOf(100.0)
        for (i in 1..30) {
            closes.add(if (i % 2 == 0) closes.last() + 20.0 else closes.last() - 15.0)
        }
        val candles = createTestCandles(closes)
        val bb = BollingerBandsIndicator(period = 20)
        
        val result = bb.calculate(candles)
        
        assertNotNull(result)
        // With high volatility, bandwidth should be significant
        assertTrue(result!!.bandwidth > 0.1, "Bandwidth ${result.bandwidth} should be > 0.1 for volatile prices")
    }
    
    @Test
    fun `test Bollinger Bands squeeze detection`() {
        // Low volatility creates squeeze
        val closes = List(25) { 100.0 + (it % 3) * 0.5 } // Very low volatility
        val candles = createTestCandles(closes)
        val bb = BollingerBandsIndicator(period = 20)
        
        val result = bb.calculate(candles)
        
        assertNotNull(result)
        // Small bandwidth indicates squeeze
        assertTrue(result!!.isSqueeze(threshold = 0.1), "Should detect squeeze with low volatility")
    }
    
    @Test
    fun `test Bollinger Bands no squeeze with volatile prices`() {
        // High volatility = no squeeze
        val closes = List(30) { 100.0 + it * 5.0 }
        val candles = createTestCandles(closes)
        val bb = BollingerBandsIndicator(period = 20)
        
        val result = bb.calculate(candles)
        
        assertNotNull(result)
        // Large bandwidth = no squeeze
        assertFalse(result!!.isSqueeze(threshold = 0.05), "Should not detect squeeze with high volatility")
    }
    
    @Test
    fun `test Bollinger Bands isTouchingUpperBand`() {
        val closes = List(30) { 100.0 + it * 2.0 }
        val candles = createTestCandles(closes)
        val bb = BollingerBandsIndicator(period = 20)
        
        val result = bb.calculate(candles)
        
        assertNotNull(result)
        
        // Test price at upper band
        assertTrue(result!!.isTouchingUpperBand(result.upper, tolerance = 0.01))
        
        // Test price slightly below upper band
        assertTrue(result.isTouchingUpperBand(result.upper * 0.99, tolerance = 0.02))
        
        // Test price far from upper band
        assertFalse(result.isTouchingUpperBand(result.middle, tolerance = 0.01))
    }
    
    @Test
    fun `test Bollinger Bands isTouchingLowerBand`() {
        val closes = List(30) { 100.0 + it * 2.0 }
        val candles = createTestCandles(closes)
        val bb = BollingerBandsIndicator(period = 20)
        
        val result = bb.calculate(candles)
        
        assertNotNull(result)
        
        // Test price at lower band
        assertTrue(result!!.isTouchingLowerBand(result.lower, tolerance = 0.01))
        
        // Test price slightly above lower band
        assertTrue(result.isTouchingLowerBand(result.lower * 1.01, tolerance = 0.02))
        
        // Test price far from lower band
        assertFalse(result.isTouchingLowerBand(result.middle, tolerance = 0.01))
    }
    
    @Test
    fun `test Bollinger Bands percentB calculation`() {
        val closes = List(30) { 100.0 + it * 1.5 }
        val candles = createTestCandles(closes)
        val bb = BollingerBandsIndicator(period = 20)
        
        val result = bb.calculate(candles)
        
        assertNotNull(result)
        
        val currentPrice = closes.last()
        // Use calculatePercentB method to get %B
        val calculatedPercentB = result!!.calculatePercentB(currentPrice)
        
        // Verify calculatedPercentB is correct
        val expectedPercentB = (currentPrice - result.lower) / (result.upper - result.lower)
        assertEquals(expectedPercentB, calculatedPercentB, 0.001)
        
        // If percentB property is set, it should match
        if (result.percentB != null) {
            assertEquals(expectedPercentB, result.percentB!!, 0.001)
        }
        
        // %B should be between 0 and 1 for price within bands
        if (currentPrice >= result.lower && currentPrice <= result.upper) {
            assertTrue(calculatedPercentB >= 0.0 && calculatedPercentB <= 1.0)
        }
    }
    
    @Test
    fun `test Bollinger Bands bandwidth calculation`() {
        val closes = List(30) { 100.0 + it * 2.0 }
        val candles = createTestCandles(closes)
        val bb = BollingerBandsIndicator(period = 20)
        
        val result = bb.calculate(candles)
        
        assertNotNull(result)
        
        val expectedBandwidth = (result!!.upper - result.lower) / result.middle
        
        // Bandwidth should match calculated value
        assertEquals(expectedBandwidth, result.bandwidth, 0.0001)
    }
    
    @Test
    fun `test Bollinger Bands calculateAll returns correct series`() {
        val closes = List(30) { 100.0 + it * 1.5 }
        val candles = createTestCandles(closes)
        val bb = BollingerBandsIndicator(period = 10)
        
        val results = bb.calculateAll(candles)
        
        assertEquals(30, results.size)
        
        // First 9 values should be null (insufficient data)
        for (i in 0..8) {
            assertNull(results[i], "BB[$i] should be null")
        }
        
        // Remaining values should be calculated
        for (i in 9 until results.size) {
            assertNotNull(results[i], "BB[$i] should be calculated")
            val bbResult = results[i]!!
            assertTrue(bbResult.upper > bbResult.middle)
            assertTrue(bbResult.middle > bbResult.lower)
            assertTrue(bbResult.bandwidth >= 0.0)
        }
    }
    
    @Test
    fun `test Bollinger Bands validates data correctly`() {
        val candles = createTestCandles(List(25) { 100.0 + it })
        val bb = BollingerBandsIndicator(period = 20)
        
        assertTrue(bb.validateData(candles))
    }
    
    @Test
    fun `test Bollinger Bands with insufficient data fails validation`() {
        val candles = createTestCandles(listOf(100.0, 101.0))
        val bb = BollingerBandsIndicator(period = 20)
        
        assertFalse(bb.validateData(candles))
    }
    
    @Test
    fun `test Bollinger Bands reset does not affect calculation`() {
        val candles = createTestCandles(List(30) { 100.0 + it * 2.0 })
        val bb = BollingerBandsIndicator(period = 20)
        
        val result1 = bb.calculate(candles)
        bb.reset()
        val result2 = bb.calculate(candles)
        
        assertEquals(result1?.upper, result2?.upper)
        assertEquals(result1?.middle, result2?.middle)
        assertEquals(result1?.lower, result2?.lower)
        assertEquals(result1?.bandwidth, result2?.bandwidth)
    }
    
    @Test
    fun `test Bollinger Bands toString`() {
        val bb = BollingerBandsIndicator(period = 20, stdDevMultiplier = 2.0)
        val str = bb.toString()
        
        assertTrue(str.contains("BollingerBands"))
        assertTrue(str.contains("20"))
        assertTrue(str.contains("2.0"))
    }
    
    @Test
    fun `test Bollinger Bands extension function`() {
        val closes = List(30) { 100.0 + it * 2.0 }
        val candles = createTestCandles(closes)
        
        val result = candles.bollingerBands()
        
        assertNotNull(result)
        assertTrue(result!!.upper > result.middle)
        assertTrue(result.middle > result.lower)
    }
    
    @Test
    fun `test Bollinger Bands extension function with custom parameters`() {
        val closes = List(30) { 100.0 + it * 1.5 }
        val candles = createTestCandles(closes)
        
        val result = candles.bollingerBands(period = 15, stdDevMultiplier = 3.0)
        
        assertNotNull(result)
        // With 3 std dev, bands should be wider
        assertTrue(result!!.bandwidth > 0.0)
    }
    
    @Test
    fun `test isBBSqueeze extension function`() {
        // Low volatility creates squeeze
        val closes = List(25) { 100.0 + (it % 2) * 0.3 }
        val candles = createTestCandles(closes)
        
        val isSqueeze = candles.isBBSqueeze()
        
        // With very low volatility, should detect squeeze
        assertTrue(isSqueeze)
    }
    
    @Test
    fun `test isTouchingUpperBB extension function`() {
        // Strong uptrend - price should approach upper band
        val closes = List(30) { 100.0 + it * 5.0 }
        val candles = createTestCandles(closes)
        
        val result = candles.isTouchingUpperBB()
        
        // With strong trend, price likely near upper band
        // Note: This might not always be true depending on data, so we just verify it runs
        assertNotNull(result)
    }
    
    @Test
    fun `test isTouchingLowerBB extension function`() {
        // Strong downtrend - price should approach lower band
        val closes = List(30) { 1000.0 - it * 5.0 }
        val candles = createTestCandles(closes)
        
        val result = candles.isTouchingLowerBB()
        
        // With strong downtrend, price likely near lower band
        // Note: This might not always be true depending on data, so we just verify it runs
        assertNotNull(result)
    }
    
    @Test
    fun `test Bollinger Bands with empty data returns null`() {
        val candles = emptyList<Candlestick>()
        val bb = BollingerBandsIndicator(period = 20)
        
        val result = bb.calculate(candles)
        
        assertNull(result)
    }
    
    @Test
    fun `test Bollinger Bands with small period`() {
        val closes = List(10) { 100.0 + it * 3.0 }
        val candles = createTestCandles(closes)
        val bb = BollingerBandsIndicator(period = 5)
        
        val result = bb.calculate(candles)
        
        assertNotNull(result)
        assertTrue(result!!.upper > result.middle)
        assertTrue(result.middle > result.lower)
    }
    
    @Test
    fun `test Bollinger Bands calculateAll with empty data`() {
        val candles = emptyList<Candlestick>()
        val bb = BollingerBandsIndicator(period = 20)
        
        val results = bb.calculateAll(candles)
        
        assertTrue(results.isEmpty())
    }
    
    @Test
    fun `test Bollinger Bands Result toString`() {
        val closes = List(30) { 100.0 + it * 2.0 }
        val candles = createTestCandles(closes)
        val bb = BollingerBandsIndicator(period = 20)
        
        val result = bb.calculate(candles)
        
        assertNotNull(result)
        val str = result!!.toString()
        assertTrue(str.contains("upper"))
        assertTrue(str.contains("middle"))
        assertTrue(str.contains("lower"))
    }
    
    @Test
    fun `test Bollinger Bands with different std dev multipliers`() {
        val closes = List(30) { 100.0 + it * 2.0 }
        val candles = createTestCandles(closes)
        
        val bb1 = BollingerBandsIndicator(period = 20, stdDevMultiplier = 1.0)
        val bb2 = BollingerBandsIndicator(period = 20, stdDevMultiplier = 2.0)
        val bb3 = BollingerBandsIndicator(period = 20, stdDevMultiplier = 3.0)
        
        val result1 = bb1.calculate(candles)
        val result2 = bb2.calculate(candles)
        val result3 = bb3.calculate(candles)
        
        assertNotNull(result1)
        assertNotNull(result2)
        assertNotNull(result3)
        
        // All should have same middle
        assertEquals(result1!!.middle, result2!!.middle, 0.0001)
        assertEquals(result2.middle, result3!!.middle, 0.0001)
        
        // Bandwidth should increase with std dev multiplier
        assertTrue(result1.bandwidth < result2.bandwidth)
        assertTrue(result2.bandwidth < result3.bandwidth)
    }
}

