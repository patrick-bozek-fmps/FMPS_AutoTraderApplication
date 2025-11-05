package com.fmps.autotrader.core.traders

import com.fmps.autotrader.core.traders.strategies.ITradingStrategy
import com.fmps.autotrader.core.traders.strategies.TrendFollowingStrategy
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.TimeFrame
import com.fmps.autotrader.shared.enums.TradeAction
import com.fmps.autotrader.shared.model.Candlestick
import com.fmps.autotrader.shared.model.Position
import com.fmps.autotrader.shared.model.TradingStrategy
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

class SignalGeneratorTest {

    private lateinit var config: AITraderConfig
    private lateinit var mockStrategy: ITradingStrategy
    private lateinit var signalGenerator: SignalGenerator

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
        mockStrategy = mockk<ITradingStrategy>()
        signalGenerator = SignalGenerator(mockStrategy, minConfidenceThreshold = 0.5)
    }

    private fun createCandles(closes: List<Double>): List<Candlestick> {
        return closes.mapIndexed { index, close ->
            Candlestick(
                symbol = "BTCUSDT",
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

    private fun createProcessedData(): ProcessedMarketData {
        val candles = createCandles(List(31) { 50000.0 })
        return ProcessedMarketData(
            candles = candles,
            indicators = mapOf("SMA" to 50000.0, "RSI" to 50.0),
            latestPrice = BigDecimal.valueOf(50000.0),
            timestamp = Instant.now()
        )
    }

    @Test
    fun `test generateSignal calls strategy`() {
        val processedData = createProcessedData()
        val strategySignal = TradingSignal(
            action = SignalAction.BUY,
            confidence = 0.7,
            reason = "Test signal",
            timestamp = Instant.now()
        )
        
        every { mockStrategy.generateSignal(any(), any()) } returns strategySignal
        
        val result = signalGenerator.generateSignal(processedData, null)
        
        verify { mockStrategy.generateSignal(processedData.candles, processedData.indicators) }
        assertEquals(SignalAction.BUY, result.action)
    }

    @Test
    fun `test confidence below threshold filters to HOLD`() {
        val processedData = createProcessedData()
        val strategySignal = TradingSignal(
            action = SignalAction.BUY,
            confidence = 0.3, // Below threshold
            reason = "Low confidence",
            timestamp = Instant.now()
        )
        
        every { mockStrategy.generateSignal(any(), any()) } returns strategySignal
        
        val result = signalGenerator.generateSignal(processedData, null)
        
        assertEquals(SignalAction.HOLD, result.action)
        assertTrue(result.reason.contains("below threshold"))
    }

    @Test
    fun `test confidence above threshold passes through`() {
        val processedData = createProcessedData()
        val strategySignal = TradingSignal(
            action = SignalAction.BUY,
            confidence = 0.7, // Above threshold
            reason = "High confidence",
            timestamp = Instant.now()
        )
        
        every { mockStrategy.generateSignal(any(), any()) } returns strategySignal
        
        val result = signalGenerator.generateSignal(processedData, null)
        
        assertEquals(SignalAction.BUY, result.action)
        assertTrue(result.confidence >= 0.5)
    }

    @Test
    fun `test existing long position prevents BUY signal`() {
        val processedData = createProcessedData()
        val strategySignal = TradingSignal(
            action = SignalAction.BUY,
            confidence = 0.7,
            reason = "Buy signal",
            timestamp = Instant.now()
        )
        
        val position = Position(
            symbol = "BTCUSDT",
            action = TradeAction.LONG,
            quantity = BigDecimal("0.1"),
            entryPrice = BigDecimal.valueOf(50000.0),
            currentPrice = BigDecimal.valueOf(50100.0),
            unrealizedPnL = BigDecimal("10.0")
        )
        
        every { mockStrategy.generateSignal(any(), any()) } returns strategySignal
        
        val result = signalGenerator.generateSignal(processedData, position)
        
        assertEquals(SignalAction.HOLD, result.action)
        assertTrue(result.reason.contains("Already have long position"))
    }

    @Test
    fun `test existing short position prevents SELL signal`() {
        val processedData = createProcessedData()
        val strategySignal = TradingSignal(
            action = SignalAction.SELL,
            confidence = 0.7,
            reason = "Sell signal",
            timestamp = Instant.now()
        )
        
        val position = Position(
            symbol = "BTCUSDT",
            action = TradeAction.SHORT,
            quantity = BigDecimal("0.1"),
            entryPrice = BigDecimal.valueOf(50000.0),
            currentPrice = BigDecimal.valueOf(49900.0),
            unrealizedPnL = BigDecimal("10.0")
        )
        
        every { mockStrategy.generateSignal(any(), any()) } returns strategySignal
        
        val result = signalGenerator.generateSignal(processedData, position)
        
        assertEquals(SignalAction.HOLD, result.action)
        assertTrue(result.reason.contains("Already have short position"))
    }

    @Test
    fun `test CLOSE signal always valid with position`() {
        val processedData = createProcessedData()
        val strategySignal = TradingSignal(
            action = SignalAction.CLOSE,
            confidence = 0.6,
            reason = "Close position",
            timestamp = Instant.now()
        )
        
        val position = Position(
            symbol = "BTCUSDT",
            action = TradeAction.LONG,
            quantity = BigDecimal("0.1"),
            entryPrice = BigDecimal.valueOf(50000.0),
            currentPrice = BigDecimal.valueOf(50100.0),
            unrealizedPnL = BigDecimal("10.0")
        )
        
        every { mockStrategy.generateSignal(any(), any()) } returns strategySignal
        
        val result = signalGenerator.generateSignal(processedData, position)
        
        assertEquals(SignalAction.CLOSE, result.action)
    }

    @Test
    fun `test profitable position close increases confidence`() {
        val processedData = createProcessedData()
        val strategySignal = TradingSignal(
            action = SignalAction.CLOSE,
            confidence = 0.6,
            reason = "Close position",
            timestamp = Instant.now()
        )
        
        val position = Position(
            symbol = "BTCUSDT",
            action = TradeAction.LONG,
            quantity = BigDecimal("0.1"),
            entryPrice = BigDecimal.valueOf(50000.0),
            currentPrice = BigDecimal.valueOf(51000.0),
            unrealizedPnL = BigDecimal("100.0") // Profitable
        )
        
        every { mockStrategy.generateSignal(any(), any()) } returns strategySignal
        
        val result = signalGenerator.generateSignal(processedData, position)
        
        assertTrue(result.confidence >= 0.6)
    }

    @Test
    fun `test losing position close reduces confidence`() {
        val processedData = createProcessedData()
        val strategySignal = TradingSignal(
            action = SignalAction.CLOSE,
            confidence = 0.6,
            reason = "Close position",
            timestamp = Instant.now()
        )
        
        val position = Position(
            symbol = "BTCUSDT",
            action = TradeAction.LONG,
            quantity = BigDecimal("0.1"),
            entryPrice = BigDecimal.valueOf(50000.0),
            currentPrice = BigDecimal.valueOf(49000.0),
            unrealizedPnL = BigDecimal("-100.0") // Losing
        )
        
        every { mockStrategy.generateSignal(any(), any()) } returns strategySignal
        
        val result = signalGenerator.generateSignal(processedData, position)
        
        assertTrue(result.confidence <= 0.6)
    }

    @Test
    fun `test error handling returns safe HOLD signal`() {
        val processedData = createProcessedData()
        
        every { mockStrategy.generateSignal(any(), any()) } throws RuntimeException("Test error")
        
        val result = signalGenerator.generateSignal(processedData, null)
        
        assertEquals(SignalAction.HOLD, result.action)
        assertEquals(0.0, result.confidence)
        assertTrue(result.reason.contains("Error"))
    }

    @Test
    fun `test final signal contains indicator values`() {
        val processedData = createProcessedData()
        val strategySignal = TradingSignal(
            action = SignalAction.BUY,
            confidence = 0.7,
            reason = "Buy",
            timestamp = Instant.now(),
            indicatorValues = mapOf("SMA" to 50000.0, "RSI" to 50.0)
        )
        
        every { mockStrategy.generateSignal(any(), any()) } returns strategySignal
        
        val result = signalGenerator.generateSignal(processedData, null)
        
        assertEquals(strategySignal.indicatorValues, result.indicatorValues)
    }

    @Test
    fun `test confidence is clamped to valid range`() {
        val processedData = createProcessedData()
        // Strategy should not return invalid confidence, but if it does, SignalGenerator should handle it
        // Since TradingSignal constructor validates confidence, we test with valid but high confidence
        val strategySignal = TradingSignal(
            action = SignalAction.BUY,
            confidence = 0.9, // Valid but high
            reason = "Buy",
            timestamp = Instant.now()
        )
        
        every { mockStrategy.generateSignal(any(), any()) } returns strategySignal
        
        val result = signalGenerator.generateSignal(processedData, null)
        
        assertTrue(result.confidence >= 0.0)
        assertTrue(result.confidence <= 1.0)
    }

    @Test
    fun `test custom confidence threshold works`() {
        val customGenerator = SignalGenerator(mockStrategy, minConfidenceThreshold = 0.8)
        val processedData = createProcessedData()
        val strategySignal = TradingSignal(
            action = SignalAction.BUY,
            confidence = 0.75, // Below 0.8 threshold
            reason = "Buy",
            timestamp = Instant.now()
        )
        
        every { mockStrategy.generateSignal(any(), any()) } returns strategySignal
        
        val result = customGenerator.generateSignal(processedData, null)
        
        assertEquals(SignalAction.HOLD, result.action)
    }

    @Test
    fun `test signal timestamp is updated`() {
        val processedData = createProcessedData()
        val oldTime = Instant.now().minusSeconds(10)
        val strategySignal = TradingSignal(
            action = SignalAction.BUY,
            confidence = 0.7,
            reason = "Buy",
            timestamp = oldTime
        )
        
        every { mockStrategy.generateSignal(any(), any()) } returns strategySignal
        
        val beforeTime = Instant.now()
        val result = signalGenerator.generateSignal(processedData, null)
        val afterTime = Instant.now()
        
        assertTrue(result.timestamp.isAfter(beforeTime.minusSeconds(1)))
        assertTrue(result.timestamp.isBefore(afterTime.plusSeconds(1)))
    }

    @Test
    fun `test reason includes position information when available`() {
        val processedData = createProcessedData()
        val strategySignal = TradingSignal(
            action = SignalAction.BUY,
            confidence = 0.7,
            reason = "Buy signal",
            timestamp = Instant.now()
        )
        
        val position = Position(
            symbol = "BTCUSDT",
            action = TradeAction.LONG,
            quantity = BigDecimal("0.1"),
            entryPrice = BigDecimal.valueOf(50000.0),
            currentPrice = BigDecimal.valueOf(50100.0),
            unrealizedPnL = BigDecimal("10.0")
        )
        
        every { mockStrategy.generateSignal(any(), any()) } returns strategySignal
        
        val result = signalGenerator.generateSignal(processedData, position)
        
        assertTrue(result.reason.contains("Current position"))
    }
}

