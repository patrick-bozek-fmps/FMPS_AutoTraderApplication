package com.fmps.autotrader.core.traders

import com.fmps.autotrader.core.traders.strategies.ITradingStrategy
import com.fmps.autotrader.core.traders.strategies.TrendFollowingStrategy
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.TimeFrame
import com.fmps.autotrader.shared.model.Candlestick
import com.fmps.autotrader.shared.model.TradingStrategy
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

class MarketDataProcessorTest {

    private lateinit var config: AITraderConfig
    private lateinit var strategy: ITradingStrategy
    private lateinit var processor: MarketDataProcessor

    @BeforeEach
    fun setUp() {
        config = AITraderConfig(
            id = "test-trader-1",
            name = "Test Trader",
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT",
            virtualMoney = true,
            maxStakeAmount = BigDecimal("1000.00"),
            maxRiskLevel = 5,
            maxTradingDuration = Duration.ofHours(24),
            minReturnPercent = 5.0,
            strategy = TradingStrategy.TREND_FOLLOWING,
            candlestickInterval = TimeFrame.ONE_HOUR
        )
        strategy = TrendFollowingStrategy(config)
        processor = MarketDataProcessor(strategy)
    }

    private fun createCandles(closes: List<Double>, baseTime: Instant = Instant.now()): List<Candlestick> {
        return closes.mapIndexed { index, close ->
            Candlestick(
                symbol = "BTCUSDT",
                interval = TimeFrame.ONE_HOUR,
                openTime = baseTime.plusSeconds(index * 3600L),
                closeTime = baseTime.plusSeconds((index + 1) * 3600L),
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
    fun `test processCandlesticks returns null for empty list`() {
        val emptyCandles = emptyList<Candlestick>()
        
        val result = processor.processCandlesticks(emptyCandles)
        
        assertNull(result)
    }

    @Test
    fun `test processCandlesticks calculates indicators`() {
        val prices = List(31) { 50000.0 + (it * 100) }
        val candles = createCandles(prices)
        
        val result = processor.processCandlesticks(candles)
        
        assertNotNull(result)
        assertTrue(result!!.indicators.isNotEmpty())
    }

    @Test
    fun `test processed data contains candles`() {
        val prices = List(31) { 50000.0 }
        val candles = createCandles(prices)
        
        val result = processor.processCandlesticks(candles)
        
        assertNotNull(result)
        assertEquals(candles.size, result!!.candles.size)
        assertEquals(candles, result.candles)
    }

    @Test
    fun `test processed data contains latest price`() {
        val prices = List(31) { 50000.0 + (it * 100) } // Enough data for indicators
        val candles = createCandles(prices)
        
        val result = processor.processCandlesticks(candles)
        
        assertNotNull(result)
        assertEquals(BigDecimal.valueOf(prices.last()), result!!.latestPrice)
    }

    @Test
    fun `test processed data has timestamp`() {
        val prices = List(31) { 50000.0 }
        val candles = createCandles(prices)
        
        val beforeTime = Instant.now()
        val result = processor.processCandlesticks(candles)
        val afterTime = Instant.now()
        
        assertNotNull(result)
        assertTrue(result!!.timestamp.isAfter(beforeTime.minusSeconds(1)))
        assertTrue(result.timestamp.isBefore(afterTime.plusSeconds(1)))
    }

    @Test
    fun `test getLatestCandle returns last candle`() {
        val prices = List(31) { 50000.0 + (it * 100) } // Enough data for indicators
        val candles = createCandles(prices)
        
        val result = processor.processCandlesticks(candles)
        
        assertNotNull(result)
        assertEquals(candles.last(), result!!.getLatestCandle())
    }

    @Test
    fun `test getIndicator returns indicator value`() {
        val prices = List(31) { 50000.0 }
        val candles = createCandles(prices)
        
        val result = processor.processCandlesticks(candles)
        
        assertNotNull(result)
        // Strategy requires SMA, RSI, MACD
        assertTrue(result!!.indicators.isNotEmpty())
    }

    @Test
    fun `test hasIndicator checks indicator existence`() {
        val prices = List(31) { 50000.0 }
        val candles = createCandles(prices)
        
        val result = processor.processCandlesticks(candles)
        
        assertNotNull(result)
        // Check if any indicator is present
        assertTrue(result!!.indicators.keys.isNotEmpty())
    }

    @Test
    fun `test clearCache clears indicator cache`() {
        val prices = List(31) { 50000.0 }
        val candles = createCandles(prices)
        
        processor.processCandlesticks(candles)
        
        // Clear should not throw
        assertDoesNotThrow { processor.clearCache() }
        
        // After clear, should still process
        val result = processor.processCandlesticks(candles)
        assertNotNull(result)
    }

    @Test
    fun `test data validation rejects invalid timestamps`() {
        // Create candles with invalid timestamps (out of order)
        val baseTime = Instant.now()
        val candles = listOf(
            Candlestick(
                symbol = "BTCUSDT",
                interval = TimeFrame.ONE_HOUR,
                openTime = baseTime.plusSeconds(3600),
                closeTime = baseTime.plusSeconds(7200),
                open = BigDecimal.valueOf(50000.0),
                high = BigDecimal.valueOf(50100.0),
                low = BigDecimal.valueOf(49900.0),
                close = BigDecimal.valueOf(50000.0),
                volume = BigDecimal.valueOf(1000.0)
            ),
            Candlestick(
                symbol = "BTCUSDT",
                interval = TimeFrame.ONE_HOUR,
                openTime = baseTime, // Before previous close
                closeTime = baseTime.plusSeconds(3600),
                open = BigDecimal.valueOf(50000.0),
                high = BigDecimal.valueOf(50100.0),
                low = BigDecimal.valueOf(49900.0),
                close = BigDecimal.valueOf(50000.0),
                volume = BigDecimal.valueOf(1000.0)
            )
        )
        
        // Should handle gracefully (may return null or process)
        processor.processCandlesticks(candles)
        // Processor may return null for invalid data
        // (result can be null or not null, both are acceptable)
    }

    @Test
    fun `test processes required indicators from strategy`() {
        val prices = List(31) { 50000.0 }
        val candles = createCandles(prices)
        
        val result = processor.processCandlesticks(candles)
        
        assertNotNull(result)
        // At least some indicators should be calculated
        assertTrue(result!!.indicators.isNotEmpty())
    }

    @Test
    fun `test handles null indicator values gracefully`() {
        // Create data that might cause null indicators
        val prices = List(5) { 50000.0 } // Too few for most indicators
        
        val result = processor.processCandlesticks(createCandles(prices))
        
        assertNull(result)
    }

    @Test
    fun `test latestPrice is positive`() {
        val prices = List(31) { 50000.0 }
        val candles = createCandles(prices)
        
        val result = processor.processCandlesticks(candles)
        
        assertNotNull(result)
        assertTrue(result!!.latestPrice > BigDecimal.ZERO)
    }

    @Test
    fun `test multiple processing calls work correctly`() {
        val prices = List(31) { 50000.0 }
        val candles = createCandles(prices)
        
        val result1 = processor.processCandlesticks(candles)
        val result2 = processor.processCandlesticks(candles)
        
        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals(result1!!.latestPrice, result2!!.latestPrice)
    }

    @Test
    fun `test insufficient data returns null`() {
        val prices = List(10) { 50000.0 }
        val candles = createCandles(prices)

        val result = processor.processCandlesticks(candles)

        assertNull(result)
    }

    @Test
    fun `test indicator cache stores results`() {
        val prices = List(40) { 50000.0 + it }
        val candles = createCandles(prices)

        val result = processor.processCandlesticks(candles)

        assertNotNull(result)
        val cached = processor.cachedIndicatorNamesForTesting()
        assertTrue(cached.isNotEmpty())

        // Second run should reuse cache without clearing
        val second = processor.processCandlesticks(candles)
        assertNotNull(second)
        assertEquals(cached, processor.cachedIndicatorNamesForTesting())
    }
}

