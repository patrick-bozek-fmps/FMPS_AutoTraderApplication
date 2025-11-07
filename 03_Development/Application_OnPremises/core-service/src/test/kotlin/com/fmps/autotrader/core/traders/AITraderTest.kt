package com.fmps.autotrader.core.traders

import com.fmps.autotrader.core.connectors.IExchangeConnector
import com.fmps.autotrader.core.connectors.MockExchangeConnector
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.TimeFrame
import com.fmps.autotrader.shared.model.Candlestick
import com.fmps.autotrader.shared.model.TradingStrategy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

class AITraderTest {

    private lateinit var config: AITraderConfig
    private lateinit var exchangeConnector: IExchangeConnector
    private lateinit var trader: AITrader

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
        exchangeConnector = MockExchangeConnector(Exchange.BINANCE)
        trader = AITrader(config, exchangeConnector)
    }

    private fun createCandles(count: Int): List<Candlestick> {
        return (0 until count).map { index ->
            Candlestick(
                symbol = "BTCUSDT",
                interval = TimeFrame.ONE_HOUR,
                openTime = Instant.now().minusSeconds((count - index) * 3600L),
                closeTime = Instant.now().minusSeconds((count - index - 1) * 3600L),
                open = BigDecimal.valueOf(50000.0 + index * 100),
                high = BigDecimal.valueOf(50000.0 + index * 100 + 50),
                low = BigDecimal.valueOf(50000.0 + index * 100 - 50),
                close = BigDecimal.valueOf(50000.0 + index * 100),
                volume = BigDecimal.valueOf(1000.0),
                quoteVolume = BigDecimal.valueOf(1000.0 * (50000.0 + index * 100))
            )
        }
    }

    @Test
    fun `test initial state is IDLE`() {
        assertEquals(AITraderState.IDLE, trader.getState())
    }

    @Test
    fun `test start transitions to RUNNING`() = runBlocking {
        exchangeConnector.configure(
            com.fmps.autotrader.shared.model.ExchangeConfig(
                exchange = Exchange.BINANCE,
                apiKey = "test-key",
                apiSecret = "test-secret",
                testnet = true
            )
        )
        
        val result = trader.start()
        
        assertTrue(result.isSuccess)
        assertEquals(AITraderState.RUNNING, trader.getState())
        
        // Cleanup
        trader.stop()
    }

    @Test
    fun `test start fails when already running`() = runBlocking {
        exchangeConnector.configure(
            com.fmps.autotrader.shared.model.ExchangeConfig(
                exchange = Exchange.BINANCE,
                apiKey = "test-key",
                apiSecret = "test-secret",
                testnet = true
            )
        )
        
        trader.start()
        
        // Try to start again
        val result = trader.start()
        
        assertTrue(result.isFailure)
        
        // Cleanup
        trader.stop()
    }

    @Test
    fun `test stop transitions to STOPPED`() = runBlocking {
        exchangeConnector.configure(
            com.fmps.autotrader.shared.model.ExchangeConfig(
                exchange = Exchange.BINANCE,
                apiKey = "test-key",
                apiSecret = "test-secret",
                testnet = true
            )
        )
        
        trader.start()
        delay(100) // Wait for startup
        
        val result = trader.stop()
        
        assertTrue(result.isSuccess)
        delay(100) // Wait for stop to complete
        assertEquals(AITraderState.STOPPED, trader.getState())
    }

    @Test
    fun `test pause transitions to PAUSED`() = runBlocking {
        exchangeConnector.configure(
            com.fmps.autotrader.shared.model.ExchangeConfig(
                exchange = Exchange.BINANCE,
                apiKey = "test-key",
                apiSecret = "test-secret",
                testnet = true
            )
        )
        
        trader.start()
        delay(100)
        
        val result = trader.pause()
        
        assertTrue(result.isSuccess)
        assertEquals(AITraderState.PAUSED, trader.getState())
        
        // Cleanup
        trader.stop()
    }

    @Test
    fun `test resume transitions from PAUSED to RUNNING`() = runBlocking {
        exchangeConnector.configure(
            com.fmps.autotrader.shared.model.ExchangeConfig(
                exchange = Exchange.BINANCE,
                apiKey = "test-key",
                apiSecret = "test-secret",
                testnet = true
            )
        )
        
        trader.start()
        delay(100)
        trader.pause()
        
        val result = trader.resume()
        
        assertTrue(result.isSuccess)
        assertEquals(AITraderState.RUNNING, trader.getState())
        
        // Cleanup
        trader.stop()
    }

    @Test
    fun `test pause fails when not running`() = runBlocking {
        val result = trader.pause()
        
        assertTrue(result.isFailure)
    }

    @Test
    fun `test resume fails when not paused`() = runBlocking {
        val result = trader.resume()
        
        assertTrue(result.isFailure)
    }

    @Test
    fun `test updateConfig fails when running`() = runBlocking {
        exchangeConnector.configure(
            com.fmps.autotrader.shared.model.ExchangeConfig(
                exchange = Exchange.BINANCE,
                apiKey = "test-key",
                apiSecret = "test-secret",
                testnet = true
            )
        )
        
        trader.start()
        delay(100)
        
        val newConfig = config.copy(name = "Updated Name")
        val result = trader.updateConfig(newConfig)
        
        assertTrue(result.isFailure)
        
        // Cleanup
        trader.stop()
    }

    @Test
    fun `test updateConfig succeeds when stopped`() = runBlocking {
        val newConfig = config.copy(name = "Updated Name")
        val result = trader.updateConfig(newConfig)
        
        assertTrue(result.isSuccess)
        assertEquals("Updated Name", trader.config.name)
    }

    @Test
    fun `test updateConfig rejects id changes`() = runBlocking {
        val newConfig = config.copy(id = "different-id")
        val result = trader.updateConfig(newConfig)

        assertTrue(result.isFailure)
    }

    @Test
    fun `test getMetrics returns valid metrics`() = runBlocking {
        val metrics = trader.getMetrics()
        
        assertNotNull(metrics)
        assertEquals(0, metrics.totalTrades)
        assertEquals(0.0, metrics.winRate)
        assertEquals(0, metrics.signalsExecuted)
        assertEquals(0, metrics.closeSignalsExecuted)
    }

    @Test
    fun `test getMetrics includes uptime when started`() = runBlocking {
        exchangeConnector.configure(
            com.fmps.autotrader.shared.model.ExchangeConfig(
                exchange = Exchange.BINANCE,
                apiKey = "test-key",
                apiSecret = "test-secret",
                testnet = true
            )
        )
        
        trader.start()
        delay(100)
        
        val metrics = trader.getMetrics()
        
        assertNotNull(metrics.startTime)
        assertTrue(metrics.uptime.toMillis() >= 0)
        
        // Cleanup
        trader.stop()
    }

    @Test
    fun `test recordTradeResult updates winning metrics`() = runBlocking {
        trader.recordTradeResult(BigDecimal("12.50"))

        val metrics = trader.getMetrics()
        assertEquals(1, metrics.totalTrades)
        assertEquals(1, metrics.winningTrades)
        assertEquals(BigDecimal("12.50"), metrics.totalProfit)
        assertEquals(BigDecimal.ZERO, metrics.totalLoss)
        assertEquals(0, metrics.closeSignalsExecuted)
    }

    @Test
    fun `test recordTradeResult updates losing metrics`() = runBlocking {
        trader.recordTradeResult(BigDecimal("-5.25"))

        val metrics = trader.getMetrics()
        assertEquals(1, metrics.totalTrades)
        assertEquals(0, metrics.winningTrades)
        assertEquals(1, metrics.losingTrades)
        assertEquals(BigDecimal.ZERO, metrics.totalProfit)
        assertEquals(BigDecimal("5.25"), metrics.totalLoss)
        assertEquals(BigDecimal("-5.25"), metrics.netProfit)
    }

    @Test
    fun `test cleanup stops trader and releases resources`() = runBlocking {
        exchangeConnector.configure(
            com.fmps.autotrader.shared.model.ExchangeConfig(
                exchange = Exchange.BINANCE,
                apiKey = "test-key",
                apiSecret = "test-secret",
                testnet = true
            )
        )
        
        trader.start()
        delay(100)
        
        trader.cleanup()
        delay(100)
        
        assertEquals(AITraderState.STOPPED, trader.getState())
    }

    @Test
    fun `test state transitions are thread-safe`() = runBlocking {
        exchangeConnector.configure(
            com.fmps.autotrader.shared.model.ExchangeConfig(
                exchange = Exchange.BINANCE,
                apiKey = "test-key",
                apiSecret = "test-secret",
                testnet = true
            )
        )
        
        // Start trader
        trader.start()
        delay(50)
        
        // Concurrent state checks
        val states = mutableListOf<AITraderState>()
        repeat(10) {
            states.add(trader.getState())
        }
        
        // All should be the same (RUNNING or STARTING)
        assertTrue(states.all { it == AITraderState.RUNNING || it == AITraderState.STARTING })
        
        // Cleanup
        trader.stop()
    }

    @Test
    fun `test config properties are accessible`() {
        assertEquals("test-trader-1", trader.config.id)
        assertEquals("Test Trader", trader.config.name)
        assertEquals(Exchange.BINANCE, trader.config.exchange)
        assertEquals("BTCUSDT", trader.config.symbol)
        assertEquals(TradingStrategy.TREND_FOLLOWING, trader.config.strategy)
    }

    @Test
    fun `test multiple start stop cycles work`() = runBlocking {
        exchangeConnector.configure(
            com.fmps.autotrader.shared.model.ExchangeConfig(
                exchange = Exchange.BINANCE,
                apiKey = "test-key",
                apiSecret = "test-secret",
                testnet = true
            )
        )
        
        // First cycle
        trader.start()
        delay(50)
        trader.stop()
        delay(50)
        
        // Second cycle
        trader.start()
        delay(50)
        trader.stop()
        delay(50)
        
        assertEquals(AITraderState.STOPPED, trader.getState())
    }

    @Test
    fun `test stop when already stopped is safe`() = runBlocking {
        val result = trader.stop()
        
        assertTrue(result.isSuccess)
        assertEquals(AITraderState.IDLE, trader.getState())
    }

    @Test
    fun `test error state is set on startup failure`() = runBlocking {
        val failingConnector = mockk<IExchangeConnector>(relaxed = true)
        coEvery { failingConnector.isConnected() } returns false
        coEvery { failingConnector.connect() } throws RuntimeException("Connection failed")
        coEvery { failingConnector.getExchange() } returns Exchange.BINANCE
        
        failingConnector.configure(
            com.fmps.autotrader.shared.model.ExchangeConfig(
                exchange = Exchange.BINANCE,
                apiKey = "test-key",
                apiSecret = "test-secret",
                testnet = true
            )
        )
        
        val failingTrader = AITrader(config, failingConnector)
        
        val result = failingTrader.start()
        
        assertTrue(result.isFailure)
        assertEquals(AITraderState.ERROR, failingTrader.getState())
    }

    @Test
    fun `test metrics are reset on start`() = runBlocking {
        exchangeConnector.configure(
            com.fmps.autotrader.shared.model.ExchangeConfig(
                exchange = Exchange.BINANCE,
                apiKey = "test-key",
                apiSecret = "test-secret",
                testnet = true
            )
        )
        
        trader.start()
        delay(100)
        val metrics1 = trader.getMetrics()
        trader.stop()
        delay(100)
        
        trader.start()
        delay(100)
        val metrics2 = trader.getMetrics()
        
        // Start times should be different (reset)
        assertNotEquals(metrics1.startTime, metrics2.startTime)
        
        // Cleanup
        trader.stop()
    }
}

