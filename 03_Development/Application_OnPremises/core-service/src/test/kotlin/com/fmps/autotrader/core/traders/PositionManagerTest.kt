package com.fmps.autotrader.core.traders

import com.fmps.autotrader.core.connectors.IExchangeConnector
import com.fmps.autotrader.core.database.DatabaseFactory
import com.fmps.autotrader.core.database.repositories.Trade
import com.fmps.autotrader.core.database.repositories.TradeRepository
import com.fmps.autotrader.core.traders.SignalAction
import com.fmps.autotrader.core.traders.TradingSignal
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.OrderType
import com.fmps.autotrader.shared.enums.TradeAction
import com.fmps.autotrader.shared.enums.TradeStatus
import com.fmps.autotrader.shared.model.Order
import com.fmps.autotrader.shared.model.Position
import com.fmps.autotrader.shared.model.Ticker
import com.typesafe.config.ConfigFactory
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.withContext

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PositionManagerTest {

    private val testDbPath = "./build/test-db/test-position-manager.db"
    private lateinit var tradeRepository: TradeRepository
    private lateinit var mockConnector: IExchangeConnector
    private lateinit var manager: PositionManager
    private var originalAppEnvironment: String? = null

    @BeforeAll
    fun setup() {
        // Setup test database
        File(testDbPath).parentFile?.mkdirs()
        File(testDbPath).delete()
        System.setProperty("database.url", "jdbc:sqlite:$testDbPath")
        originalAppEnvironment = System.getProperty("app.environment")
        System.setProperty("app.environment", "test")

        val config = ConfigFactory.load()
        DatabaseFactory.init(config)

        tradeRepository = TradeRepository()
    }

    @AfterAll
    fun teardown() {
        DatabaseFactory.close()
        File(testDbPath).delete()
        originalAppEnvironment?.let { System.setProperty("app.environment", it) } ?: System.clearProperty("app.environment")
    }

    @BeforeEach
    fun setupMockConnector() {
        // Create fresh mock connector for each test
        mockConnector = mockk<IExchangeConnector>(relaxed = true)
        every { mockConnector.getExchange() } returns Exchange.BINANCE
        manager = PositionManager(mockConnector, tradeRepository, updateInterval = Duration.ofSeconds(1))
    }

    @AfterEach
    fun cleanup() = runBlocking {
        manager.cleanup()
        // Clear all positions before each test
        val positions = manager.getAllPositions()
        positions.forEach { position ->
            manager.closePosition(position.positionId, "MANUAL").getOrNull()
        }
        // Clear database
        val openTrades = tradeRepository.findAllOpenTrades()
        openTrades.forEach { trade ->
            tradeRepository.close(
                tradeId = trade.id,
                exitPrice = trade.entryPrice,
                exitAmount = trade.entryAmount,
                exitReason = "MANUAL"
            )
        }
    }

    @Test
    @DisplayName("Should open position from BUY signal")
    fun `test open position from buy signal`() = runTest {
        // Arrange
        val signal = TradingSignal(
            action = SignalAction.BUY,
            confidence = 0.8,
            reason = "Test signal"
        )
        val traderId = "1"
        val symbol = "BTCUSDT"
        
        // Setup mock connector
        val ticker = Ticker(
            symbol = symbol,
            lastPrice = BigDecimal("50000"),
            bidPrice = BigDecimal("49999"),
            askPrice = BigDecimal("50001"),
            volume = BigDecimal("1000"),
            quoteVolume = BigDecimal("50000000"),
            priceChange = BigDecimal("100"),
            priceChangePercent = BigDecimal("0.2"),
            high = BigDecimal("51000"),
            low = BigDecimal("49000"),
            openPrice = BigDecimal("49900")
        )
        coEvery { mockConnector.getTicker(symbol) } returns ticker
        
        val order = Order(
            symbol = symbol,
            action = TradeAction.LONG,
            type = OrderType.MARKET,
            quantity = BigDecimal("0.001"),
            status = TradeStatus.FILLED,
            filledQuantity = BigDecimal("0.001"),
            averagePrice = BigDecimal("50000")
        )
        coEvery { mockConnector.placeOrder(any()) } returns order

        // Act
        val result = manager.openPosition(signal, traderId, symbol)

        // Assert
        assertTrue(result.isSuccess)
        val position = result.getOrNull()
        assertNotNull(position)
        assertEquals(symbol, position!!.position.symbol)
        assertEquals(TradeAction.LONG, position.position.action)
        assertEquals(BigDecimal("50000"), position.position.entryPrice)
    }

    @Test
    @DisplayName("Should open position from SELL signal")
    fun `test open position from sell signal`() = runTest {
        // Arrange
        val signal = TradingSignal(
            action = SignalAction.SELL,
            confidence = 0.8,
            reason = "Test signal"
        )
        val traderId = "1"
        val symbol = "BTCUSDT"
        
        val ticker = Ticker(
            symbol = symbol,
            lastPrice = BigDecimal("50000"),
            bidPrice = BigDecimal("49999"),
            askPrice = BigDecimal("50001"),
            volume = BigDecimal("1000"),
            quoteVolume = BigDecimal("50000000"),
            priceChange = BigDecimal("100"),
            priceChangePercent = BigDecimal("0.2"),
            high = BigDecimal("51000"),
            low = BigDecimal("49000"),
            openPrice = BigDecimal("49900")
        )
        coEvery { mockConnector.getTicker(symbol) } returns ticker
        
        val order = Order(
            symbol = symbol,
            action = TradeAction.SHORT,
            type = OrderType.MARKET,
            quantity = BigDecimal("0.001"),
            status = TradeStatus.FILLED,
            filledQuantity = BigDecimal("0.001"),
            averagePrice = BigDecimal("50000")
        )
        coEvery { mockConnector.placeOrder(any()) } returns order

        // Act
        val result = manager.openPosition(signal, traderId, symbol)

        // Assert
        assertTrue(result.isSuccess)
        val position = result.getOrNull()
        assertNotNull(position)
        assertEquals(TradeAction.SHORT, position!!.position.action)
    }

    @Test
    @DisplayName("Should fail to open position from HOLD signal")
    fun `test open position fails for hold signal`() = runTest {
        // Arrange
        val signal = TradingSignal(
            action = SignalAction.HOLD,
            confidence = 0.5,
            reason = "Hold signal"
        )
        val traderId = "1"
        val symbol = "BTCUSDT"

        // Act
        val result = manager.openPosition(signal, traderId, symbol)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PositionException)
    }

    @Test
    @DisplayName("Should block position when risk manager rejects exposure")
    fun `test open position blocked by risk manager`() = runTest {
        val signal = TradingSignal(
            action = SignalAction.BUY,
            confidence = 0.9,
            reason = "High notional"
        )
        val traderId = "risk-trader"
        val symbol = "BTCUSDT"

        val ticker = Ticker(
            symbol = symbol,
            lastPrice = BigDecimal("60000"),
            bidPrice = BigDecimal("59950"),
            askPrice = BigDecimal("60010"),
            volume = BigDecimal("1000"),
            quoteVolume = BigDecimal("60000000"),
            priceChange = BigDecimal("500"),
            priceChangePercent = BigDecimal("0.2"),
            high = BigDecimal("60500"),
            low = BigDecimal("59000"),
            openPrice = BigDecimal("59500")
        )
        coEvery { mockConnector.getTicker(symbol) } returns ticker

        val riskManager = RiskManager(
            positionProvider = manager,
            riskConfig = RiskConfig(
                maxTotalBudget = BigDecimal("500"),
                maxLeveragePerTrader = 1,
                maxTotalLeverage = 1,
                maxExposurePerTrader = BigDecimal("500"),
                maxTotalExposure = BigDecimal("500"),
                maxDailyLoss = BigDecimal("1000"),
                stopLossPercentage = 0.05
            )
        )
        manager.attachRiskManager(riskManager)

        val result = manager.openPosition(
            signal = signal,
            traderId = traderId,
            symbol = symbol,
            quantity = BigDecimal.ONE
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PositionException)
        coVerify(exactly = 1) { mockConnector.getTicker(symbol) }
        coVerify(exactly = 0) { mockConnector.placeOrder(any()) }
        riskManager.stopMonitoring()
    }

    @Test
    @DisplayName("Should update position with current price")
    fun `test update position`() = runTest {
        // Arrange - Open a position first
        val signal = TradingSignal(
            action = SignalAction.BUY,
            confidence = 0.8,
            reason = "Test signal"
        )
        val traderId = "1"
        val symbol = "BTCUSDT"
        
        val ticker = Ticker(
            symbol = symbol,
            lastPrice = BigDecimal("50000"),
            bidPrice = BigDecimal("49999"),
            askPrice = BigDecimal("50001"),
            volume = BigDecimal("1000"),
            quoteVolume = BigDecimal("50000000"),
            priceChange = BigDecimal("100"),
            priceChangePercent = BigDecimal("0.2"),
            high = BigDecimal("51000"),
            low = BigDecimal("49000"),
            openPrice = BigDecimal("49900")
        )
        coEvery { mockConnector.getTicker(symbol) } returns ticker
        
        val order = Order(
            symbol = symbol,
            action = TradeAction.LONG,
            type = OrderType.MARKET,
            quantity = BigDecimal("0.001"),
            status = TradeStatus.FILLED,
            filledQuantity = BigDecimal("0.001"),
            averagePrice = BigDecimal("50000")
        )
        coEvery { mockConnector.placeOrder(any()) } returns order
        
        val openResult = manager.openPosition(signal, traderId, symbol)
        assertTrue(openResult.isSuccess)
        val positionId = openResult.getOrNull()!!.positionId
        
        // Update ticker to new price
        val newTicker = ticker.copy(lastPrice = BigDecimal("51000"))
        coEvery { mockConnector.getTicker(symbol) } returns newTicker

        // Act
        val updateResult = manager.updatePosition(positionId)

        // Assert
        assertTrue(updateResult.isSuccess)
        val updatedPosition = manager.getPosition(positionId)
        assertNotNull(updatedPosition)
        assertEquals(BigDecimal("51000"), updatedPosition!!.position.currentPrice)
        // P&L should be positive (price increased for LONG position)
        assertTrue(updatedPosition.position.unrealizedPnL > BigDecimal.ZERO)
    }

    @Test
    @DisplayName("Should calculate P&L correctly for LONG position")
    fun `test calculate pnl for long position`() = runTest {
        // Arrange
        val position = Position(
            symbol = "BTCUSDT",
            action = TradeAction.LONG,
            quantity = BigDecimal("0.001"),
            entryPrice = BigDecimal("50000"),
            currentPrice = BigDecimal("51000"),
            unrealizedPnL = BigDecimal.ZERO,
            leverage = BigDecimal.ONE
        )

        // Act
        val pnl = manager.calculatePnL(position)

        // Assert
        // P&L = (51000 - 50000) * 0.001 * 1 = 1000 * 0.001 = 1
        assertEquals(0, BigDecimal("1").compareTo(pnl))
    }

    @Test
    @DisplayName("Should calculate P&L correctly for SHORT position")
    fun `test calculate pnl for short position`() = runTest {
        // Arrange
        val position = Position(
            symbol = "BTCUSDT",
            action = TradeAction.SHORT,
            quantity = BigDecimal("0.001"),
            entryPrice = BigDecimal("50000"),
            currentPrice = BigDecimal("49000"),
            unrealizedPnL = BigDecimal.ZERO,
            leverage = BigDecimal.ONE
        )

        // Act
        val pnl = manager.calculatePnL(position)

        // Assert
        // P&L = (50000 - 49000) * 0.001 * 1 = 1000 * 0.001 = 1
        assertEquals(0, BigDecimal("1").compareTo(pnl))
    }

    @Test
    @DisplayName("Should close position successfully")
    fun `test close position`() = runTest {
        // Arrange - Open a position first
        val signal = TradingSignal(
            action = SignalAction.BUY,
            confidence = 0.8,
            reason = "Test signal"
        )
        val traderId = "1"
        val symbol = "BTCUSDT"
        
        val ticker = Ticker(
            symbol = symbol,
            lastPrice = BigDecimal("50000"),
            bidPrice = BigDecimal("49999"),
            askPrice = BigDecimal("50001"),
            volume = BigDecimal("1000"),
            quoteVolume = BigDecimal("50000000"),
            priceChange = BigDecimal("100"),
            priceChangePercent = BigDecimal("0.2"),
            high = BigDecimal("51000"),
            low = BigDecimal("49000"),
            openPrice = BigDecimal("49900")
        )
        coEvery { mockConnector.getTicker(symbol) } returns ticker
        
        val order = Order(
            symbol = symbol,
            action = TradeAction.LONG,
            type = OrderType.MARKET,
            quantity = BigDecimal("0.001"),
            status = TradeStatus.FILLED,
            filledQuantity = BigDecimal("0.001"),
            averagePrice = BigDecimal("50000")
        )
        coEvery { mockConnector.placeOrder(any()) } returns order
        
        val openResult = manager.openPosition(signal, traderId, symbol)
        assertTrue(openResult.isSuccess)
        val positionId = openResult.getOrNull()!!.positionId
        
        // Setup close order
        val closeOrder = Order(
            symbol = symbol,
            action = TradeAction.SHORT,
            type = OrderType.MARKET,
            quantity = BigDecimal("0.001"),
            status = TradeStatus.FILLED,
            filledQuantity = BigDecimal("0.001"),
            averagePrice = BigDecimal("51000")
        )
        coEvery { mockConnector.getTicker(symbol) } returns ticker.copy(lastPrice = BigDecimal("51000"))
        coEvery { mockConnector.placeOrder(any()) } returns closeOrder

        // Act
        val closeResult = manager.closePosition(positionId, "MANUAL")

        // Assert
        assertTrue(closeResult.isSuccess)
        val closedPosition = manager.getPosition(positionId)
        assertNull(closedPosition) // Position should be removed from active positions
        
        // Check history
        val history = manager.getHistoryByTrader(traderId)
        assertEquals(1, history.size)
        assertEquals("MANUAL", history[0].closeReason)
    }

    @Test
    @DisplayName("Close position surfaces persistence error without removing position")
    fun `test close position fails when repository close fails`() = runTest {
        val failingRepo = TradeRepository()
        val failingPersistence = object : PositionPersistence(failingRepo) {
            private var failNext = true
            override suspend fun closePosition(
                tradeId: Int,
                exitPrice: BigDecimal,
                exitAmount: BigDecimal,
                exitReason: String,
                exitOrderId: String?,
                fees: BigDecimal
            ): Trade? {
                return if (failNext) {
                    failNext = false
                    null
                } else {
                    super.closePosition(tradeId, exitPrice, exitAmount, exitReason, exitOrderId, fees)
                }
            }
        }
        val failingManager = PositionManager(mockConnector, failingRepo, updateInterval = Duration.ofSeconds(1), positionPersistence = failingPersistence)

        val signal = TradingSignal(action = SignalAction.BUY, confidence = 0.85, reason = "Persistence failure")
        val traderId = "persist-err"
        val symbol = "BTCUSDT"

        val ticker = Ticker(
            symbol = symbol,
            lastPrice = BigDecimal("50000"),
            bidPrice = BigDecimal("49990"),
            askPrice = BigDecimal("50010"),
            volume = BigDecimal("900"),
            quoteVolume = BigDecimal("45000000"),
            priceChange = BigDecimal("120"),
            priceChangePercent = BigDecimal("0.24"),
            high = BigDecimal("50500"),
            low = BigDecimal("49500"),
            openPrice = BigDecimal("49900")
        )
        coEvery { mockConnector.getTicker(symbol) } returns ticker

        val order = Order(
            symbol = symbol,
            action = TradeAction.LONG,
            type = OrderType.MARKET,
            quantity = BigDecimal("0.001"),
            status = TradeStatus.FILLED,
            filledQuantity = BigDecimal("0.001"),
            averagePrice = BigDecimal("50000")
        )
        coEvery { mockConnector.placeOrder(any()) } returns order

        val openResult = failingManager.openPosition(signal, traderId, symbol)
        assertTrue(openResult.isSuccess)
        val managedPosition = openResult.getOrNull()!!

        val closeResult = failingManager.closePosition(managedPosition.positionId, "MANUAL")
        assertTrue(closeResult.isFailure)
        assertNotNull(failingManager.getPosition(managedPosition.positionId))

        val retryResult = failingManager.closePosition(managedPosition.positionId, "MANUAL")
        assertTrue(retryResult.isSuccess)
        assertNull(failingManager.getPosition(managedPosition.positionId))

        failingManager.cleanup()
    }

    @Test
    @DisplayName("Should check stop-loss correctly")
    fun `test check stop loss`() = runTest {
        // Arrange - Open a position with stop-loss
        val signal = TradingSignal(
            action = SignalAction.BUY,
            confidence = 0.8,
            reason = "Test signal"
        )
        val traderId = "1"
        val symbol = "BTCUSDT"
        val stopLossPrice = BigDecimal("49000")
        
        val ticker = Ticker(
            symbol = symbol,
            lastPrice = BigDecimal("50000"),
            bidPrice = BigDecimal("49999"),
            askPrice = BigDecimal("50001"),
            volume = BigDecimal("1000"),
            quoteVolume = BigDecimal("50000000"),
            priceChange = BigDecimal("100"),
            priceChangePercent = BigDecimal("0.2"),
            high = BigDecimal("51000"),
            low = BigDecimal("49000"),
            openPrice = BigDecimal("49900")
        )
        coEvery { mockConnector.getTicker(symbol) } returns ticker
        
        val order = Order(
            symbol = symbol,
            action = TradeAction.LONG,
            type = OrderType.MARKET,
            quantity = BigDecimal("0.001"),
            status = TradeStatus.FILLED,
            filledQuantity = BigDecimal("0.001"),
            averagePrice = BigDecimal("50000")
        )
        coEvery { mockConnector.placeOrder(any()) } returns order
        
        val openResult = manager.openPosition(signal, traderId, symbol, stopLossPrice = stopLossPrice)
        assertTrue(openResult.isSuccess)
        val positionId = openResult.getOrNull()!!.positionId
        
        // Update price below stop-loss
        coEvery { mockConnector.getTicker(symbol) } returns ticker.copy(lastPrice = BigDecimal("48000"))
        manager.updatePosition(positionId).getOrNull()

        // Act
        val isTriggered = manager.checkStopLoss(positionId)

        // Assert
        assertTrue(isTriggered)
    }

    @Test
    @DisplayName("Should get positions by trader")
    fun `test get positions by trader`() = runTest {
        // Arrange - Open positions for different traders
        val signal1 = TradingSignal(SignalAction.BUY, 0.8, "Test")
        val signal2 = TradingSignal(SignalAction.BUY, 0.8, "Test")
        val traderId1 = "1"
        val traderId2 = "2"
        val symbol = "BTCUSDT"
        
        val ticker = Ticker(
            symbol = symbol,
            lastPrice = BigDecimal("50000"),
            bidPrice = BigDecimal("49999"),
            askPrice = BigDecimal("50001"),
            volume = BigDecimal("1000"),
            quoteVolume = BigDecimal("50000000"),
            priceChange = BigDecimal("100"),
            priceChangePercent = BigDecimal("0.2"),
            high = BigDecimal("51000"),
            low = BigDecimal("49000"),
            openPrice = BigDecimal("49900")
        )
        coEvery { mockConnector.getTicker(symbol) } returns ticker
        
        val order = Order(
            symbol = symbol,
            action = TradeAction.LONG,
            type = OrderType.MARKET,
            quantity = BigDecimal("0.001"),
            status = TradeStatus.FILLED,
            filledQuantity = BigDecimal("0.001"),
            averagePrice = BigDecimal("50000")
        )
        coEvery { mockConnector.placeOrder(any()) } returns order
        
        manager.openPosition(signal1, traderId1, symbol).getOrNull()
        manager.openPosition(signal2, traderId2, symbol).getOrNull()

        // Act
        val positions1 = manager.getPositionsByTrader(traderId1)
        val positions2 = manager.getPositionsByTrader(traderId2)

        // Assert
        assertEquals(1, positions1.size)
        assertEquals(1, positions2.size)
        assertEquals(traderId1, positions1[0].traderId)
        assertEquals(traderId2, positions2[0].traderId)
    }

    @Test
    @DisplayName("Should get all active positions")
    fun `test get all positions`() = runTest {
        // Arrange - Open multiple positions
        val signal = TradingSignal(SignalAction.BUY, 0.8, "Test")
        val traderId = "1"
        val symbol = "BTCUSDT"
        
        val ticker = Ticker(
            symbol = symbol,
            lastPrice = BigDecimal("50000"),
            bidPrice = BigDecimal("49999"),
            askPrice = BigDecimal("50001"),
            volume = BigDecimal("1000"),
            quoteVolume = BigDecimal("50000000"),
            priceChange = BigDecimal("100"),
            priceChangePercent = BigDecimal("0.2"),
            high = BigDecimal("51000"),
            low = BigDecimal("49000"),
            openPrice = BigDecimal("49900")
        )
        coEvery { mockConnector.getTicker(symbol) } returns ticker
        
        val order = Order(
            symbol = symbol,
            action = TradeAction.LONG,
            type = OrderType.MARKET,
            quantity = BigDecimal("0.001"),
            status = TradeStatus.FILLED,
            filledQuantity = BigDecimal("0.001"),
            averagePrice = BigDecimal("50000")
        )
        coEvery { mockConnector.placeOrder(any()) } returns order
        
        manager.openPosition(signal, traderId, symbol).getOrNull()
        manager.openPosition(signal, traderId, symbol).getOrNull()

        // Act
        val allPositions = manager.getAllPositions()

        // Assert
        assertEquals(2, allPositions.size)
    }

    @Test
    @DisplayName("Should get position by ID")
    fun `test get position by id`() = runTest {
        // Arrange - Open a position
        val signal = TradingSignal(SignalAction.BUY, 0.8, "Test")
        val traderId = "1"
        val symbol = "BTCUSDT"
        
        val ticker = Ticker(
            symbol = symbol,
            lastPrice = BigDecimal("50000"),
            bidPrice = BigDecimal("49999"),
            askPrice = BigDecimal("50001"),
            volume = BigDecimal("1000"),
            quoteVolume = BigDecimal("50000000"),
            priceChange = BigDecimal("100"),
            priceChangePercent = BigDecimal("0.2"),
            high = BigDecimal("51000"),
            low = BigDecimal("49000"),
            openPrice = BigDecimal("49900")
        )
        coEvery { mockConnector.getTicker(symbol) } returns ticker
        
        val order = Order(
            symbol = symbol,
            action = TradeAction.LONG,
            type = OrderType.MARKET,
            quantity = BigDecimal("0.001"),
            status = TradeStatus.FILLED,
            filledQuantity = BigDecimal("0.001"),
            averagePrice = BigDecimal("50000")
        )
        coEvery { mockConnector.placeOrder(any()) } returns order
        
        val openResult = manager.openPosition(signal, traderId, symbol)
        assertTrue(openResult.isSuccess)
        val positionId = openResult.getOrNull()!!.positionId

        // Act
        val position = manager.getPosition(positionId)

        // Assert
        assertNotNull(position)
        assertEquals(positionId, position!!.positionId)
    }

    @Test
    @DisplayName("Should return null for non-existent position")
    fun `test get position returns null for non-existent`() = runTest {
        // Act
        val position = manager.getPosition("non-existent-id")

        // Assert
        assertNull(position)
    }

    @Test
    @DisplayName("Should get position history by trader")
    fun `test get history by trader`() = runTest {
        // Arrange - Open and close a position
        val signal = TradingSignal(SignalAction.BUY, 0.8, "Test")
        val traderId = "1"
        val symbol = "BTCUSDT"
        
        val ticker = Ticker(
            symbol = symbol,
            lastPrice = BigDecimal("50000"),
            bidPrice = BigDecimal("49999"),
            askPrice = BigDecimal("50001"),
            volume = BigDecimal("1000"),
            quoteVolume = BigDecimal("50000000"),
            priceChange = BigDecimal("100"),
            priceChangePercent = BigDecimal("0.2"),
            high = BigDecimal("51000"),
            low = BigDecimal("49000"),
            openPrice = BigDecimal("49900")
        )
        coEvery { mockConnector.getTicker(symbol) } returns ticker
        
        val order = Order(
            symbol = symbol,
            action = TradeAction.LONG,
            type = OrderType.MARKET,
            quantity = BigDecimal("0.001"),
            status = TradeStatus.FILLED,
            filledQuantity = BigDecimal("0.001"),
            averagePrice = BigDecimal("50000")
        )
        coEvery { mockConnector.placeOrder(any()) } returns order
        
        val openResult = manager.openPosition(signal, traderId, symbol)
        assertTrue(openResult.isSuccess)
        val positionId = openResult.getOrNull()!!.positionId
        
        val closeOrder = Order(
            symbol = symbol,
            action = TradeAction.SHORT,
            type = OrderType.MARKET,
            quantity = BigDecimal("0.001"),
            status = TradeStatus.FILLED,
            filledQuantity = BigDecimal("0.001"),
            averagePrice = BigDecimal("51000")
        )
        coEvery { mockConnector.placeOrder(any()) } returns closeOrder
        manager.closePosition(positionId, "MANUAL").getOrNull()

        // Act
        val history = manager.getHistoryByTrader(traderId)

        // Assert
        assertEquals(1, history.size)
        assertEquals(traderId, history[0].traderId)
        assertEquals("MANUAL", history[0].closeReason)
    }

    @Test
    @DisplayName("Should calculate total P&L from history")
    fun `test get total pnl`() = runTest {
        // Arrange - Create some history entries (simulated)
        // Note: In real scenario, these would come from closed positions
        // For now, we'll test with empty history and then with actual closed positions
        
        // Act
        val totalPnL = manager.getTotalPnL()
        
        // Assert
        assertNotNull(totalPnL)
        // Initially should be zero
        assertEquals(BigDecimal.ZERO, totalPnL)
    }

    @Test
    @DisplayName("Should update stop-loss and persist to repository")
    fun `test update stop loss repository sync`() = runTest {
        val signal = TradingSignal(action = SignalAction.BUY, confidence = 0.9, reason = "Test")
        val traderId = "5"
        val symbol = "BTCUSDT"

        val ticker = Ticker(
            symbol = symbol,
            lastPrice = BigDecimal("30000"),
            bidPrice = BigDecimal("29990"),
            askPrice = BigDecimal("30010"),
            volume = BigDecimal("800"),
            quoteVolume = BigDecimal("24000000"),
            priceChange = BigDecimal("50"),
            priceChangePercent = BigDecimal("0.15"),
            high = BigDecimal("30500"),
            low = BigDecimal("29500"),
            openPrice = BigDecimal("29950")
        )
        coEvery { mockConnector.getTicker(symbol) } returns ticker

        val order = Order(
            symbol = symbol,
            action = TradeAction.LONG,
            type = OrderType.MARKET,
            quantity = BigDecimal("0.002"),
            status = TradeStatus.FILLED,
            filledQuantity = BigDecimal("0.002"),
            averagePrice = BigDecimal("30000")
        )
        coEvery { mockConnector.placeOrder(any()) } returns order

        val openResult = manager.openPosition(signal, traderId, symbol)
        assertTrue(openResult.isSuccess)
        val managedPosition = openResult.getOrNull()!!
        assertNotNull(managedPosition.tradeId)

        val newStopLoss = BigDecimal("29000")
        val updateResult = manager.updateStopLoss(managedPosition.positionId, newStopLoss)
        assertTrue(updateResult.isSuccess)

        val tradeFromRepo = tradeRepository.findById(managedPosition.tradeId!!)
        assertNotNull(tradeFromRepo)
        assertEquals(0, tradeFromRepo!!.stopLossPrice.stripTrailingZeros().compareTo(newStopLoss.stripTrailingZeros()))
    }

    @Test
    @DisplayName("Trailing stop moves stop-loss as price improves")
    fun `test trailing stop adjusts with price`() = runTest {
        val signal = TradingSignal(action = SignalAction.BUY, confidence = 0.85, reason = "Trailing test")
        val traderId = "trail-1"
        val symbol = "BTCUSDT"

        val ticker = Ticker(
            symbol = symbol,
            lastPrice = BigDecimal("30000"),
            bidPrice = BigDecimal("29990"),
            askPrice = BigDecimal("30010"),
            volume = BigDecimal("750"),
            quoteVolume = BigDecimal("22500000"),
            priceChange = BigDecimal("40"),
            priceChangePercent = BigDecimal("0.13"),
            high = BigDecimal("30500"),
            low = BigDecimal("29500"),
            openPrice = BigDecimal("29960")
        )
        coEvery { mockConnector.getTicker(symbol) } returns ticker

        val order = Order(
            symbol = symbol,
            action = TradeAction.LONG,
            type = OrderType.MARKET,
            quantity = BigDecimal("0.0015"),
            status = TradeStatus.FILLED,
            filledQuantity = BigDecimal("0.0015"),
            averagePrice = BigDecimal("30000")
        )
        coEvery { mockConnector.placeOrder(any()) } returns order

        val openResult = manager.openPosition(signal, traderId, symbol)
        assertTrue(openResult.isSuccess)
        val managedPosition = openResult.getOrNull()!!

        val initialStop = BigDecimal("28000")
        val updateResult = manager.updateStopLoss(managedPosition.positionId, initialStop, trailingActivated = true)
        assertTrue(updateResult.isSuccess)

        val afterUpdate = manager.getPosition(managedPosition.positionId)!!
        assertTrue(afterUpdate.trailingStopActivated)
        assertEquals(0, afterUpdate.stopLossPrice!!.compareTo(initialStop))
        assertEquals(0, afterUpdate.trailingStopDistance!!.compareTo(BigDecimal("2000")))

        manager.updatePosition(managedPosition.positionId, BigDecimal("32000"))

        val adjusted = manager.getPosition(managedPosition.positionId)!!
        assertEquals(0, adjusted.stopLossPrice!!.compareTo(BigDecimal("30000")))
        assertEquals(0, adjusted.trailingStopReferencePrice!!.compareTo(BigDecimal("32000")))
        assertEquals(0, adjusted.trailingStopDistance!!.compareTo(BigDecimal("2000")))

        val tradeFromRepo = tradeRepository.findById(adjusted.tradeId!!)
        assertNotNull(tradeFromRepo)
        assertTrue(tradeFromRepo!!.trailingStopActivated)
        assertEquals(0, tradeFromRepo.stopLossPrice.compareTo(BigDecimal("30000")))
    }

    @Test
    @DisplayName("Should update take-profit and persist to repository")
    fun `test update take profit repository sync`() = runTest {
        val signal = TradingSignal(action = SignalAction.BUY, confidence = 0.9, reason = "Test")
        val traderId = "6"
        val symbol = "BTCUSDT"

        val ticker = Ticker(
            symbol = symbol,
            lastPrice = BigDecimal("32000"),
            bidPrice = BigDecimal("31990"),
            askPrice = BigDecimal("32010"),
            volume = BigDecimal("900"),
            quoteVolume = BigDecimal("28800000"),
            priceChange = BigDecimal("60"),
            priceChangePercent = BigDecimal("0.19"),
            high = BigDecimal("32500"),
            low = BigDecimal("31500"),
            openPrice = BigDecimal("31900")
        )
        coEvery { mockConnector.getTicker(symbol) } returns ticker

        val order = Order(
            symbol = symbol,
            action = TradeAction.LONG,
            type = OrderType.MARKET,
            quantity = BigDecimal("0.002"),
            status = TradeStatus.FILLED,
            filledQuantity = BigDecimal("0.002"),
            averagePrice = BigDecimal("32000")
        )
        coEvery { mockConnector.placeOrder(any()) } returns order

        val openResult = manager.openPosition(signal, traderId, symbol)
        assertTrue(openResult.isSuccess)
        val managedPosition = openResult.getOrNull()!!
        assertNotNull(managedPosition.tradeId)

        val newTakeProfit = BigDecimal("34000")
        val updateResult = manager.updateTakeProfit(managedPosition.positionId, newTakeProfit)
        assertTrue(updateResult.isSuccess)

        val tradeFromRepo = tradeRepository.findById(managedPosition.tradeId!!)
        assertNotNull(tradeFromRepo)
        assertEquals(0, tradeFromRepo!!.takeProfitPrice.stripTrailingZeros().compareTo(newTakeProfit.stripTrailingZeros()))
    }

    @Test
    @DisplayName("Should refresh position from exchange")
    fun `test refresh position`() = runTest {
        val signal = TradingSignal(action = SignalAction.BUY, confidence = 0.7, reason = "Test")
        val traderId = "7"
        val symbol = "BTCUSDT"

        val initialTicker = Ticker(
            symbol = symbol,
            lastPrice = BigDecimal("25000"),
            bidPrice = BigDecimal("24990"),
            askPrice = BigDecimal("25010"),
            volume = BigDecimal("700"),
            quoteVolume = BigDecimal("17500000"),
            priceChange = BigDecimal("40"),
            priceChangePercent = BigDecimal("0.16"),
            high = BigDecimal("25500"),
            low = BigDecimal("24500"),
            openPrice = BigDecimal("24950")
        )
        coEvery { mockConnector.getTicker(symbol) } returns initialTicker

        val openOrder = Order(
            symbol = symbol,
            action = TradeAction.LONG,
            type = OrderType.MARKET,
            quantity = BigDecimal("0.001"),
            status = TradeStatus.FILLED,
            filledQuantity = BigDecimal("0.001"),
            averagePrice = BigDecimal("25000")
        )
        coEvery { mockConnector.placeOrder(any()) } returns openOrder

        val openResult = manager.openPosition(signal, traderId, symbol)
        assertTrue(openResult.isSuccess)
        val managedPosition = openResult.getOrNull()!!

        val exchangePosition = Position(
            symbol = symbol,
            action = TradeAction.LONG,
            quantity = BigDecimal("0.001"),
            entryPrice = BigDecimal("25000"),
            currentPrice = BigDecimal("26000"),
            unrealizedPnL = BigDecimal.ZERO,
            leverage = BigDecimal.ONE
        )
        coEvery { mockConnector.getPosition(symbol) } returns exchangePosition
        val newTicker = initialTicker.copy(lastPrice = BigDecimal("26000"))
        coEvery { mockConnector.getTicker(symbol) } returns newTicker

        val refreshResult = manager.refreshPosition(managedPosition.positionId)
        assertTrue(refreshResult.isSuccess)
        val refreshed = refreshResult.getOrNull()
        assertNotNull(refreshed)
        assertEquals(BigDecimal("26000"), refreshed!!.position.currentPrice)
        assertTrue(refreshed.position.unrealizedPnL > BigDecimal.ZERO)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @DisplayName("Monitoring should auto-close positions when stop-loss triggers")
    fun `test monitoring auto close`() = runTest {
        val monitorInterval = Duration.ofMillis(100)
        manager = PositionManager(mockConnector, tradeRepository, updateInterval = monitorInterval)

        val signal = TradingSignal(action = SignalAction.BUY, confidence = 0.8, reason = "Test monitoring")
        val traderId = "8"
        val symbol = "BTCUSDT"
        val stopLossPrice = BigDecimal("29500")

        val initialTicker = Ticker(
            symbol = symbol,
            lastPrice = BigDecimal("30000"),
            bidPrice = BigDecimal("29990"),
            askPrice = BigDecimal("30010"),
            volume = BigDecimal("1000"),
            quoteVolume = BigDecimal("30000000"),
            priceChange = BigDecimal("100"),
            priceChangePercent = BigDecimal("0.33"),
            high = BigDecimal("30500"),
            low = BigDecimal("29500"),
            openPrice = BigDecimal("29900")
        )
        val dropTicker = initialTicker.copy(lastPrice = BigDecimal("29000"))
        coEvery { mockConnector.getTicker(symbol) } returnsMany listOf(initialTicker, dropTicker, dropTicker, dropTicker)

        val openOrder = Order(
            symbol = symbol,
            action = TradeAction.LONG,
            type = OrderType.MARKET,
            quantity = BigDecimal("0.001"),
            status = TradeStatus.FILLED,
            filledQuantity = BigDecimal("0.001"),
            averagePrice = BigDecimal("30000")
        )
        val closeOrder = openOrder.copy(action = TradeAction.SHORT, averagePrice = BigDecimal("29000"))
        coEvery { mockConnector.placeOrder(any()) } returns openOrder andThen closeOrder
        coEvery { mockConnector.getPosition(symbol) } returns null

        val openResult = manager.openPosition(signal, traderId, symbol, stopLossPrice = stopLossPrice)
        assertTrue(openResult.isSuccess)
        val positionId = openResult.getOrNull()!!.positionId

        manager.startMonitoring()

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            delay(1200)
        }
        manager.stopMonitoring()

        val activePosition = manager.getPosition(positionId)
        assertNull(activePosition)

        val history = manager.getHistoryByTrader(traderId)
        assertTrue(history.isNotEmpty())
        assertEquals("STOP_LOSS", history.last().closeReason)
    }

    @Test
    @DisplayName("Should calculate win rate from history")
    fun `test get win rate`() = runTest {
        val winRate = manager.getWinRate()
        assertEquals(0.0, winRate)
    }

    @Test
    @DisplayName("Should start and stop monitoring")
    fun `test start stop monitoring`() = runTest {
        manager.startMonitoring()
        delay(100)
        manager.stopMonitoring()
        assertTrue(true)
    }

    @Test
    @DisplayName("Should recover positions from database")
    fun `test recover positions`() = runTest {
        tradeRepository.create(
            aiTraderId = 1,
            tradeType = "LONG",
            exchange = "BINANCE",
            tradingPair = "BTCUSDT",
            leverage = 1,
            entryPrice = BigDecimal("50000"),
            entryAmount = BigDecimal("0.001"),
            stopLossPrice = BigDecimal("49000"),
            takeProfitPrice = BigDecimal("51000")
        )

        val exchangePosition = Position(
            symbol = "BTCUSDT",
            action = TradeAction.LONG,
            quantity = BigDecimal("0.001"),
            entryPrice = BigDecimal("50000"),
            currentPrice = BigDecimal("50500"),
            unrealizedPnL = BigDecimal.ZERO,
            leverage = BigDecimal.ONE
        )
        coEvery { mockConnector.getPosition("BTCUSDT") } returns exchangePosition

        val ticker = Ticker(
            symbol = "BTCUSDT",
            lastPrice = BigDecimal("50500"),
            bidPrice = BigDecimal("50499"),
            askPrice = BigDecimal("50501"),
            volume = BigDecimal("1000"),
            quoteVolume = BigDecimal("50000000"),
            priceChange = BigDecimal("100"),
            priceChangePercent = BigDecimal("0.2"),
            high = BigDecimal("51000"),
            low = BigDecimal("49000"),
            openPrice = BigDecimal("49900")
        )
        coEvery { mockConnector.getTicker("BTCUSDT") } returns ticker

        val result = manager.recoverPositions()
        assertTrue(result.isSuccess)
        val positions = manager.getAllPositions()
        assertTrue(positions.isNotEmpty())
    }
}