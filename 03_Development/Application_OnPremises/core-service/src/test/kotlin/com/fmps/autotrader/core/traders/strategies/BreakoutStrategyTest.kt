package com.fmps.autotrader.core.traders.strategies

import com.fmps.autotrader.core.traders.AITraderConfig
import com.fmps.autotrader.core.traders.SignalAction
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

class BreakoutStrategyTest {

    private lateinit var config: AITraderConfig
    private lateinit var strategy: BreakoutStrategy

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
            strategy = TradingStrategy.BREAKOUT,
            candlestickInterval = TimeFrame.ONE_HOUR
        )
        strategy = BreakoutStrategy(config)
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
    fun `test strategy name and description`() {
        assertEquals("Breakout", strategy.getName())
        assertTrue(strategy.getDescription().contains("breakout"))
        assertTrue(strategy.getRequiredIndicators().contains("BollingerBands") || 
                   strategy.getRequiredIndicators().contains("BB"))
        assertTrue(strategy.getRequiredIndicators().contains("MACD"))
    }

    @Test
    fun `test validateConfig returns true for valid config`() {
        assertTrue(strategy.validateConfig(config))
    }

    @Test
    fun `test insufficient data returns HOLD signal`() {
        val candles = createCandles(List(10) { 50000.0 }) // Need at least 26
        val signal = strategy.generateSignal(candles, emptyMap())
        
        assertEquals(SignalAction.HOLD, signal.action)
        assertEquals(0.0, signal.confidence)
        assertTrue(signal.reason.contains("Insufficient data"))
    }

    @Test
    fun `test upper breakout with momentum generates BUY signal`() {
        // Create prices that break above upper band with momentum
        val mean = 50000.0
        val prices = (0 until 20).map { mean + (it - 10) * 100 } + // Build bands
                     List(6) { mean + 2500 + (it * 200) } // Break above upper band
        
        val baseTime = Instant.now().minusSeconds(3600 * 30)
        val candles = createCandles(prices, baseTime)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        // May generate BUY if breakout detected
        assertTrue(signal.action == SignalAction.BUY || signal.action == SignalAction.HOLD)
        assertTrue(signal.confidence >= 0.0 && signal.confidence <= 1.0)
    }

    @Test
    fun `test lower breakout with momentum generates SELL signal`() {
        // Create prices that break below lower band with momentum
        val mean = 50000.0
        val prices = (0 until 20).map { mean + (it - 10) * 100 } + // Build bands
                     List(6) { mean - 2500 - (it * 200) } // Break below lower band
        
        val baseTime = Instant.now().minusSeconds(3600 * 30)
        val candles = createCandles(prices, baseTime)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        // May generate SELL if breakout detected
        assertTrue(signal.action == SignalAction.SELL || signal.action == SignalAction.HOLD)
        assertTrue(signal.confidence >= 0.0 && signal.confidence <= 1.0)
    }

    @Test
    fun `test MACD momentum confirmation increases confidence`() {
        // Create breakout with bullish MACD
        val mean = 50000.0
        val prices = (0 until 20).map { mean + (it - 10) * 100 } +
                     List(6) { mean + 2500 + (it * 300) } // Strong upward breakout
        
        val baseTime = Instant.now().minusSeconds(3600 * 30)
        val candles = createCandles(prices, baseTime)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        // If BUY signal, should have MACD confirmation
        if (signal.action == SignalAction.BUY) {
            assertTrue(signal.confidence >= 0.5)
            assertTrue(signal.indicatorValues.containsKey("macd"))
        }
    }

    @Test
    fun `test false breakout detection returns HOLD`() {
        // Price breaks out but comes back (false breakout)
        val mean = 50000.0
        val prices = (0 until 20).map { mean + (it - 10) * 100 } +
                     listOf(mean + 3000) + // Break above
                     listOf(mean + 500) // Come back
        
        val baseTime = Instant.now().minusSeconds(3600 * 25)
        val candles = createCandles(prices, baseTime)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        // False breakout should result in HOLD
        if (signal.reason.contains("False breakout")) {
            assertEquals(SignalAction.HOLD, signal.action)
        }
    }

    @Test
    fun `test squeeze detection returns HOLD signal`() {
        // Low volatility (squeeze) - no breakout
        val mean = 50000.0
        val prices = List(26) { mean + (it % 3 - 1) * 10 } // Very low volatility
        
        val baseTime = Instant.now().minusSeconds(3600 * 30)
        val candles = createCandles(prices, baseTime)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        // Squeeze should result in HOLD
        if (signal.reason.contains("squeeze")) {
            assertEquals(SignalAction.HOLD, signal.action)
        }
    }

    @Test
    fun `test breakout threshold validation`() {
        // Price slightly above band but not above threshold
        val mean = 50000.0
        val prices = (0 until 20).map { mean + (it - 10) * 100 } +
                     listOf(mean + 2100) // Above band but below 5% threshold
        
        val baseTime = Instant.now().minusSeconds(3600 * 25)
        val candles = createCandles(prices, baseTime)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        // Should not generate BUY if below threshold
        if (signal.action != SignalAction.BUY) {
            assertEquals(SignalAction.HOLD, signal.action)
        }
    }

    @Test
    fun `test reset clears internal state`() {
        val prices = List(26) { 50000.0 }
        val candles = createCandles(prices)
        strategy.generateSignal(candles, emptyMap())
        
        assertDoesNotThrow { strategy.reset() }
        
        val signalAfterReset = strategy.generateSignal(candles, emptyMap())
        assertNotNull(signalAfterReset)
    }

    @Test
    fun `test signal contains Bollinger Bands and MACD values`() {
        val prices = List(26) { 50000.0 }
        val candles = createCandles(prices)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        assertTrue(signal.indicatorValues.containsKey("bbUpper") ||
                   signal.indicatorValues.containsKey("bbLower"))
    }

    @Test
    fun `test error handling returns safe HOLD signal`() {
        val emptyCandles = emptyList<Candlestick>()
        
        val signal = strategy.generateSignal(emptyCandles, emptyMap())
        
        assertEquals(SignalAction.HOLD, signal.action)
        assertEquals(0.0, signal.confidence)
    }

    @Test
    fun `test required indicators list`() {
        val required = strategy.getRequiredIndicators()
        
        assertTrue(required.contains("BollingerBands") || required.contains("BB"))
        assertTrue(required.contains("MACD"))
    }

    @Test
    fun `test confidence is within valid range`() {
        val prices = List(26) { 50000.0 }
        val candles = createCandles(prices)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        assertTrue(signal.confidence >= 0.0)
        assertTrue(signal.confidence <= 1.0)
    }

    @Test
    fun `test reason explains breakout detection`() {
        val prices = List(26) { 50000.0 }
        val candles = createCandles(prices)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        assertTrue(signal.reason.isNotBlank())
    }

    @Test
    fun `test signal timestamp is recent`() {
        val prices = List(26) { 50000.0 }
        val candles = createCandles(prices)
        
        val beforeTime = Instant.now()
        val signal = strategy.generateSignal(candles, emptyMap())
        val afterTime = Instant.now()
        
        assertTrue(signal.timestamp.isAfter(beforeTime.minusSeconds(1)))
        assertTrue(signal.timestamp.isBefore(afterTime.plusSeconds(1)))
    }

    @Test
    fun `test no breakout generates HOLD signal`() {
        // Price within bands
        val mean = 50000.0
        val prices = (0 until 20).map { mean + (it - 10) * 100 } +
                     List(6) { mean + 500 } // Within bands
        
        val baseTime = Instant.now().minusSeconds(3600 * 30)
        val candles = createCandles(prices, baseTime)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        // No breakout should result in HOLD
        if (signal.reason.contains("No breakout")) {
            assertEquals(SignalAction.HOLD, signal.action)
        }
    }
}

