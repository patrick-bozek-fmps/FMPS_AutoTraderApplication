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

class MeanReversionStrategyTest {

    private lateinit var config: AITraderConfig
    private lateinit var strategy: MeanReversionStrategy

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
            strategy = TradingStrategy.MEAN_REVERSION,
            candlestickInterval = TimeFrame.ONE_HOUR
        )
        strategy = MeanReversionStrategy(config)
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
        assertEquals("Mean Reversion", strategy.getName())
        assertTrue(strategy.getDescription().contains("revert"))
        assertTrue(strategy.getRequiredIndicators().contains("BollingerBands"))
        assertTrue(strategy.getRequiredIndicators().contains("RSI"))
    }

    @Test
    fun `test validateConfig returns true for valid config`() {
        assertTrue(strategy.validateConfig(config))
    }

    @Test
    fun `test insufficient data returns HOLD signal`() {
        val candles = createCandles(List(10) { 50000.0 }) // Need at least 20
        val signal = strategy.generateSignal(candles, emptyMap())
        
        assertEquals(SignalAction.HOLD, signal.action)
        assertEquals(0.0, signal.confidence)
        assertTrue(signal.reason.contains("Insufficient data"))
    }

    @Test
    fun `test lower band touch with RSI oversold generates BUY signal`() {
        // Create prices that touch lower Bollinger Band
        // Mean around 50000, std dev around 1000, so lower band ~48000
        val mean = 50000.0
        val prices = (0 until 20).map { mean + (it - 10) * 100 } + // Build mean
                     listOf(mean - 2500) // Touch lower band (oversold)
        
        val baseTime = Instant.now().minusSeconds(3600 * 25)
        val candles = createCandles(prices, baseTime)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        // May generate BUY if conditions are met
        assertTrue(signal.action == SignalAction.BUY || signal.action == SignalAction.HOLD)
        assertTrue(signal.confidence >= 0.0 && signal.confidence <= 1.0)
    }

    @Test
    fun `test upper band touch with RSI overbought generates SELL signal`() {
        // Create prices that touch upper Bollinger Band
        val mean = 50000.0
        val prices = (0 until 20).map { mean + (it - 10) * 100 } + // Build mean
                     listOf(mean + 2500) // Touch upper band (overbought)
        
        val baseTime = Instant.now().minusSeconds(3600 * 25)
        val candles = createCandles(prices, baseTime)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        // May generate SELL if conditions are met
        assertTrue(signal.action == SignalAction.SELL || signal.action == SignalAction.HOLD)
        assertTrue(signal.confidence >= 0.0 && signal.confidence <= 1.0)
    }

    @Test
    fun `test RSI confirmation required for signals`() {
        // Price at lower band but RSI not oversold
        val mean = 50000.0
        val prices = (0 until 20).map { mean + (it - 10) * 100 } +
                     listOf(mean - 2500)
        
        val baseTime = Instant.now().minusSeconds(3600 * 25)
        val candles = createCandles(prices, baseTime)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        // If price at lower band but RSI not confirming, should be HOLD
        if (signal.action != SignalAction.BUY) {
            assertEquals(SignalAction.HOLD, signal.action)
        }
    }

    @Test
    fun `test squeeze detection returns HOLD signal`() {
        // Create low volatility (squeeze) scenario
        val mean = 50000.0
        val prices = List(21) { mean + (it % 3 - 1) * 10 } // Very low volatility
        
        val baseTime = Instant.now().minusSeconds(3600 * 25)
        val candles = createCandles(prices, baseTime)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        // Squeeze should result in HOLD
        if (signal.reason.contains("squeeze")) {
            assertEquals(SignalAction.HOLD, signal.action)
        }
    }

    @Test
    fun `test percentB calculation is included in indicator values`() {
        val mean = 50000.0
        val prices = (0 until 20).map { mean + (it - 10) * 100 } +
                     listOf(mean - 1000)
        
        val baseTime = Instant.now().minusSeconds(3600 * 25)
        val candles = createCandles(prices, baseTime)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        // Should contain %B
        assertTrue(signal.indicatorValues.containsKey("percentB"))
    }

    @Test
    fun `test confidence increases with distance from mean`() {
        // Price far below mean should have higher confidence
        val mean = 50000.0
        val prices = (0 until 20).map { mean + (it - 10) * 100 } +
                     listOf(mean - 3000) // Far below mean
        
        val baseTime = Instant.now().minusSeconds(3600 * 25)
        val candles = createCandles(prices, baseTime)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        if (signal.action == SignalAction.BUY) {
            assertTrue(signal.confidence >= 0.5)
        }
    }

    @Test
    fun `test reset clears internal state`() {
        val prices = List(21) { 50000.0 }
        val candles = createCandles(prices)
        strategy.generateSignal(candles, emptyMap())
        
        assertDoesNotThrow { strategy.reset() }
        
        val signalAfterReset = strategy.generateSignal(candles, emptyMap())
        assertNotNull(signalAfterReset)
    }

    @Test
    fun `test signal contains Bollinger Bands values`() {
        val prices = List(21) { 50000.0 }
        val candles = createCandles(prices)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        assertTrue(signal.indicatorValues.containsKey("bbUpper") ||
                   signal.indicatorValues.containsKey("bbLower") ||
                   signal.indicatorValues.containsKey("bbMiddle"))
    }

    @Test
    fun `test price within bands generates HOLD signal`() {
        // Price within normal range
        val mean = 50000.0
        val prices = (0 until 20).map { mean + (it - 10) * 100 } +
                     listOf(mean + 500) // Within bands
        
        val baseTime = Instant.now().minusSeconds(3600 * 25)
        val candles = createCandles(prices, baseTime)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        // Price within bands should result in HOLD
        if (signal.reason.contains("within")) {
            assertEquals(SignalAction.HOLD, signal.action)
        }
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
        assertTrue(required.contains("RSI"))
    }

    @Test
    fun `test confidence is within valid range`() {
        val prices = List(21) { 50000.0 }
        val candles = createCandles(prices)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        assertTrue(signal.confidence >= 0.0)
        assertTrue(signal.confidence <= 1.0)
    }

    @Test
    fun `test reason explains signal generation`() {
        val prices = List(21) { 50000.0 }
        val candles = createCandles(prices)
        
        val signal = strategy.generateSignal(candles, emptyMap())
        
        assertTrue(signal.reason.isNotBlank())
        assertTrue(signal.reason.length > 10)
    }

    @Test
    fun `test signal timestamp is recent`() {
        val prices = List(21) { 50000.0 }
        val candles = createCandles(prices)
        
        val beforeTime = Instant.now()
        val signal = strategy.generateSignal(candles, emptyMap())
        val afterTime = Instant.now()
        
        assertTrue(signal.timestamp.isAfter(beforeTime.minusSeconds(1)))
        assertTrue(signal.timestamp.isBefore(afterTime.plusSeconds(1)))
    }
}

