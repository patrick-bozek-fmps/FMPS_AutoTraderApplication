package com.fmps.autotrader.core.traders

import com.fmps.autotrader.core.traders.AITraderConfig
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.TimeFrame
import com.fmps.autotrader.shared.enums.TradeAction
import com.fmps.autotrader.shared.model.Position
import com.fmps.autotrader.shared.model.TradingStrategy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger

class RiskManagerTest {

    private lateinit var provider: FakePositionProvider
    private lateinit var riskConfig: RiskConfig
    private lateinit var riskManager: RiskManager

    @BeforeEach
    fun setup() {
        provider = FakePositionProvider()
        riskConfig = RiskConfig(
            maxTotalBudget = BigDecimal("10000"),
            maxLeveragePerTrader = 5,
            maxTotalLeverage = 10,
            maxExposurePerTrader = BigDecimal("5000"),
            maxTotalExposure = BigDecimal("12000"),
            maxDailyLoss = BigDecimal("1000"),
            stopLossPercentage = 0.05,
            monitoringIntervalSeconds = 1
        )
        riskManager = RiskManager(provider, riskConfig)
    }

    @Test
    fun `validateBudget should allow allocations within limits`() = runTest {
        val result = riskManager.validateBudget(BigDecimal("1000"), "trader-1", BigDecimal("2"))
        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateBudget should fail when exposure exceeds trader limit`() = runTest {
        provider.addPosition("trader-1", quantity = BigDecimal("0.2"), price = BigDecimal("10000"), leverage = BigDecimal("2"))

        val result = riskManager.validateBudget(BigDecimal("1000"), "trader-1", BigDecimal("3"))
        assertTrue(result.isFailure)
    }

    @Test
    fun `validateLeverage should fail when requested leverage exceeds limits`() = runTest {
        provider.addPosition("trader-1", quantity = BigDecimal("0.5"), price = BigDecimal("1000"), leverage = BigDecimal("4"))

        val result = riskManager.validateLeverage(BigDecimal("6"), "trader-1")
        assertTrue(result.isFailure)
    }

    @Test
    fun `canOpenPosition returns false when limits violated`() = runTest {
        provider.addPosition("trader-1", quantity = BigDecimal("0.4"), price = BigDecimal("6000"), leverage = BigDecimal("2"))

        val result = riskManager.canOpenPosition(
            traderId = "trader-1",
            notionalAmount = BigDecimal("2000"),
            leverage = BigDecimal("2")
        )

        assertEquals(false, result.getOrNull())
    }

    @Test
    fun `emergencyStop closes positions and invokes handler`() = runTest {
        provider.addPosition("trader-1", quantity = BigDecimal("0.1"), price = BigDecimal("2000"), leverage = BigDecimal("2"))
        riskManager.registerTrader("trader-1")

        val stopCall = CompletableDeferred<String>()
        riskManager.registerStopHandlers(
            traderStopHandler = { traderId ->
                stopCall.complete(traderId)
                Result.success(Unit)
            }
        )

        riskManager.emergencyStop("trader-1")

        assertTrue(provider.positionsById.isEmpty())
        assertEquals("trader-1", stopCall.await())
    }

    @Test
    fun `calculateRiskScore produces recommendation`() = runTest {
        provider.addPosition("trader-1", quantity = BigDecimal("0.1"), price = BigDecimal("1000"), leverage = BigDecimal("2"))
        provider.addHistory(
            traderId = "trader-1",
            realizedPnL = BigDecimal("-600"),
            closedAt = Instant.now()
        )

        val score = riskManager.calculateRiskScore("trader-1")

        assertNotNull(score)
        assertTrue(score.overallScore >= 0.0)
        assertNotEquals(RiskRecommendation.EMERGENCY_STOP, score.recommendation)
    }

    @Test
    fun `stopLossManager detects trader level losses`() = runTest {
        val stopLossManager = StopLossManager(provider, riskConfig)
        provider.addHistory(
            traderId = "trader-1",
            realizedPnL = BigDecimal("-1500"),
            closedAt = Instant.now()
        )

        val result = stopLossManager.checkTraderStopLoss("trader-1")
        assertTrue(result)
    }

    @Test
    fun `validateTraderCreation fails when no budget available`() = runTest {
        val zeroBudgetConfig = riskConfig.copy(maxTotalBudget = BigDecimal.ZERO)
        val constrainedManager = RiskManager(provider, zeroBudgetConfig)

        val traderConfig = AITraderConfig(
            id = "risk-trader",
            name = "Risk Trader",
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT",
            maxStakeAmount = BigDecimal("1000"),
            maxRiskLevel = 3,
            maxTradingDuration = Duration.ofHours(12),
            minReturnPercent = 2.0,
            strategy = TradingStrategy.TREND_FOLLOWING,
            candlestickInterval = TimeFrame.ONE_HOUR
        )

        val result = constrainedManager.validateTraderCreation(traderConfig)

        assertTrue(result.isFailure)
        val violation = (result.exceptionOrNull() as? RiskValidationException)?.violation
        assertEquals(RiskViolationType.BUDGET, violation?.type)
    }

    @Test
    fun `checkRiskLimits flags total exposure violations`() = runTest {
        val localProvider = FakePositionProvider()
        val tightConfig = riskConfig.copy(
            maxExposurePerTrader = BigDecimal("20000"),
            maxTotalExposure = BigDecimal("5000")
        )
        val localManager = RiskManager(localProvider, tightConfig)

        localProvider.addPosition("trader-1", quantity = BigDecimal("0.3"), price = BigDecimal("10000"), leverage = BigDecimal.ONE)
        localProvider.addPosition("trader-2", quantity = BigDecimal("0.3"), price = BigDecimal("10000"), leverage = BigDecimal.ONE)

        localManager.registerTrader("trader-1")

        val result = localManager.checkRiskLimits("trader-1")

        assertFalse(result.isAllowed)
        assertTrue(result.violations.any { violation ->
            violation.type == RiskViolationType.EXPOSURE && violation.message.contains("Total exposure")
        })
    }

    @Test
    fun `validateLeverage fails when global leverage limit exceeded`() = runTest {
        val constrainedConfig = riskConfig.copy(maxTotalLeverage = 3)
        val constrainedManager = RiskManager(provider, constrainedConfig)

        provider.addPosition(
            traderId = "trader-1",
            quantity = BigDecimal("0.1"),
            price = BigDecimal("10000"),
            leverage = BigDecimal("3")
        )

        val result = constrainedManager.validateLeverage(BigDecimal("4"), "trader-2")

        assertTrue(result.isFailure)
        val violation = (result.exceptionOrNull() as? RiskValidationException)?.violation
        assertEquals(RiskViolationType.LEVERAGE, violation?.type)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `monitoring triggers emergency stop when thresholds breached`() = runTest {
        val now = Instant.parse("2025-11-07T12:00:00Z")
        val monitoringConfig = riskConfig.copy(
            maxTotalBudget = BigDecimal("1000"),
            maxLeveragePerTrader = 1,
            maxTotalLeverage = 1,
            maxExposurePerTrader = BigDecimal("1000"),
            maxTotalExposure = BigDecimal("1000"),
            maxDailyLoss = BigDecimal("100")
        )

        val stopSignal = CompletableDeferred<String>()
        val monitoringRiskManager = RiskManager(
            positionProvider = provider,
            riskConfig = monitoringConfig,
            clock = Clock.fixed(now, ZoneOffset.UTC),
            monitoringScope = this
        )
        monitoringRiskManager.registerStopHandlers(
            traderStopHandler = { traderId ->
                stopSignal.complete(traderId)
                Result.success(Unit)
            }
        )

        provider.addPosition(
            traderId = "trader-1",
            quantity = BigDecimal.ONE,
            price = BigDecimal("2000"),
            leverage = BigDecimal("3")
        )
        provider.addHistory(
            traderId = "trader-1",
            realizedPnL = BigDecimal("-500"),
            closedAt = now
        )

        monitoringRiskManager.registerTrader("trader-1")
        monitoringRiskManager.startMonitoring()

        runCurrent()

        assertEquals("trader-1", stopSignal.await())
        monitoringRiskManager.stopMonitoring()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `emergency stop is idempotent under concurrent calls`() = runTest {
        provider.addPosition(
            traderId = "trader-1",
            quantity = BigDecimal("0.5"),
            price = BigDecimal("1000"),
            leverage = BigDecimal("2")
        )

        riskManager.registerTrader("trader-1")

        val jobs = List(10) {
            launch { riskManager.emergencyStop("trader-1") }
        }
        jobs.joinAll()

        assertEquals(1, provider.closeCallCount.get())
        assertTrue(provider.positionsById.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `end to end risk flow closes positions and notifies handlers`() = runTest {
        provider.addPosition(
            traderId = "trader-1",
            quantity = BigDecimal("1.5"),
            price = BigDecimal("6000"),
            leverage = BigDecimal("3")
        )
        provider.addHistory(
            traderId = "trader-1",
            realizedPnL = BigDecimal("-2000"),
            closedAt = Instant.now()
        )

        val stopSignal = CompletableDeferred<String>()
        riskManager.registerTrader("trader-1")
        riskManager.registerStopHandlers(
            traderStopHandler = { traderId ->
                stopSignal.complete(traderId)
                Result.success(Unit)
            }
        )

        val openCheck = riskManager.canOpenPosition(
            traderId = "trader-1",
            notionalAmount = BigDecimal("4000"),
            leverage = BigDecimal("2")
        )
        assertEquals(false, openCheck.getOrNull())

        val limits = riskManager.checkRiskLimits("trader-1")
        assertFalse(limits.isAllowed)
        assertTrue(limits.violations.isNotEmpty())

        riskManager.emergencyStop("trader-1")

        assertEquals("trader-1", stopSignal.await())
        assertEquals(1, provider.closeCallCount.get())
        assertTrue(provider.positionsById.isEmpty())
    }

    private class FakePositionProvider : RiskPositionProvider {
        val positionsById = mutableMapOf<String, ManagedPosition>()
        private val traderPositions = mutableMapOf<String, MutableList<String>>()
        private val traderHistory = mutableMapOf<String, MutableList<PositionHistory>>()
        val closeCallCount = AtomicInteger(0)

        fun addPosition(traderId: String, quantity: BigDecimal, price: BigDecimal, leverage: BigDecimal) {
            val position = Position(
                symbol = "BTCUSDT",
                action = TradeAction.LONG,
                quantity = quantity,
                entryPrice = price,
                currentPrice = price,
                unrealizedPnL = BigDecimal.ZERO,
                leverage = leverage,
                openedAt = Instant.now()
            )
            val managed = ManagedPosition(
                positionId = "pos-${positionsById.size + 1}",
                traderId = traderId,
                position = position,
                tradeId = null,
                stopLossPrice = null,
                takeProfitPrice = null,
                lastUpdated = Instant.now()
            )
            positionsById[managed.positionId] = managed
            traderPositions.getOrPut(traderId) { mutableListOf() }.add(managed.positionId)
        }

        fun addHistory(traderId: String, realizedPnL: BigDecimal, closedAt: Instant) {
            val history = PositionHistory(
                positionId = "history-${traderHistory.size + 1}",
                traderId = traderId,
                symbol = "BTCUSDT",
                action = TradeAction.LONG,
                entryPrice = BigDecimal("1000"),
                closePrice = BigDecimal("1000"),
                quantity = BigDecimal.ONE,
                realizedPnL = realizedPnL,
                openedAt = closedAt.minus(Duration.ofHours(1)),
                closedAt = closedAt,
                closeReason = "TEST",
                duration = Duration.ofHours(1)
            )
            traderHistory.getOrPut(traderId) { mutableListOf() }.add(history)
        }

        override suspend fun getPositionsByTrader(traderId: String): List<ManagedPosition> {
            return traderPositions[traderId]?.mapNotNull { positionsById[it] } ?: emptyList()
        }

        override suspend fun getAllPositions(): List<ManagedPosition> {
            return positionsById.values.toList()
        }

        override suspend fun getHistoryByTrader(traderId: String): List<PositionHistory> {
            return traderHistory[traderId]?.toList() ?: emptyList()
        }

        override suspend fun closePosition(positionId: String, reason: String): Result<ManagedPosition> {
            val position = positionsById.remove(positionId)
                ?: return Result.failure(IllegalArgumentException("Position not found"))
            traderPositions[position.traderId]?.remove(positionId)
            closeCallCount.incrementAndGet()
            return Result.success(position)
        }
    }
}

