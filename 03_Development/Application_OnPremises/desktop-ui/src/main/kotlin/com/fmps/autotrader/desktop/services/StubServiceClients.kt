package com.fmps.autotrader.desktop.services

import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StubCoreServiceClient : CoreServiceClient {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val summariesFlow = MutableStateFlow(
        listOf(
            TraderSummary("T-001", "Momentum Alpha", "Binance", TraderStatus.RUNNING, 1245.32, 3),
            TraderSummary("T-002", "Range Rider", "Bitget", TraderStatus.STOPPED, -212.04, 0),
            TraderSummary("T-003", "Arbitrage Scout", "Binance", TraderStatus.RUNNING, 842.11, 1)
        )
    )

    init {
        scope.launch {
            while (true) {
                delay(4_000)
                summariesFlow.update { current ->
                    current.map { summary ->
                        if (summary.status == TraderStatus.RUNNING) {
                            val delta = Random.nextDouble(-25.0, 25.0)
                            summary.copy(profitLoss = (summary.profitLoss + delta))
                        } else summary
                    }
                }
            }
        }
    }

    override fun traderSummaries(): Flow<List<TraderSummary>> = summariesFlow.asStateFlow()
}

class StubTelemetryClient : TelemetryClient {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val samples = MutableSharedFlow<TelemetrySample>(extraBufferCapacity = 32)
    private var running = false

    override fun start() {
        if (running) return
        running = true
        scope.launch {
            while (running) {
                delay(2_000)
                samples.tryEmit(randomSample())
            }
        }
    }

    override fun stop() {
        running = false
    }

    override fun samples(): Flow<TelemetrySample> = samples.asSharedFlow()

    private fun randomSample(): TelemetrySample {
        val now = System.currentTimeMillis()
        return when (Random.nextInt(0, 4)) {
            0 -> TelemetrySample(
                channel = "system.warning",
                payload = """{"type":"CPU","message":"CPU usage at ${Random.nextInt(70, 95)}%","timestamp":$now}"""
            )
            1 -> TelemetrySample(
                channel = "risk.alert",
                payload = """{"trader":"T-${Random.nextInt(100, 999)}","reason":"Drawdown threshold approached","timestamp":$now}"""
            )
            else -> TelemetrySample(
                channel = "trader.status",
                payload = """{"id":"T-001","status":"RUNNING","heartbeat":$now}"""
            )
        }
    }
}

class StubTraderService : TraderService {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val tradersFlow = MutableStateFlow(
        listOf(
            TraderDetail(
                id = "T-001",
                name = "Momentum Alpha",
                exchange = "Binance",
                strategy = "Momentum",
                riskLevel = TraderRiskLevel.BALANCED,
                baseAsset = "BTC",
                quoteAsset = "USDT",
                budget = 1500.0,
                status = TraderStatus.RUNNING,
                profitLoss = 1245.0,
                openPositions = 2,
                createdAt = Instant.now().minusSeconds(86_400)
            ),
            TraderDetail(
                id = "T-002",
                name = "Range Rider",
                exchange = "Bitget",
                strategy = "Mean Reversion",
                riskLevel = TraderRiskLevel.CONSERVATIVE,
                baseAsset = "ETH",
                quoteAsset = "USDT",
                budget = 900.0,
                status = TraderStatus.STOPPED,
                profitLoss = -120.0,
                openPositions = 0,
                createdAt = Instant.now().minusSeconds(172_800)
            )
        )
    )

    override fun traders(): Flow<List<TraderDetail>> = tradersFlow.asStateFlow()

    override suspend fun createTrader(draft: TraderDraft): TraderDetail {
        val detail = draft.toDetail(UUID.randomUUID().toString(), TraderStatus.STOPPED)
        tradersFlow.update { listOf(detail) + it }
        return detail
    }

    override suspend fun updateTrader(id: String, draft: TraderDraft): TraderDetail {
        var updated: TraderDetail? = null
        tradersFlow.update { current ->
            current.map { trader ->
                if (trader.id == id) {
                    trader.copy(
                        name = draft.name,
                        exchange = draft.exchange,
                        strategy = draft.strategy,
                        riskLevel = draft.riskLevel,
                        baseAsset = draft.baseAsset,
                        quoteAsset = draft.quoteAsset,
                        budget = draft.budget
                    ).also { updated = it }
                } else trader
            }
        }
        return updated ?: throw IllegalArgumentException("Trader $id not found")
    }

    override suspend fun deleteTrader(id: String) {
        tradersFlow.update { current -> current.filterNot { it.id == id } }
    }

    override suspend fun startTrader(id: String) {
        tradersFlow.update { current ->
            current.map { trader ->
                if (trader.id == id) trader.copy(status = TraderStatus.RUNNING) else trader
            }
        }
        emitHeartbeat(id)
    }

    override suspend fun stopTrader(id: String) {
        tradersFlow.update { current ->
            current.map { trader ->
                if (trader.id == id) trader.copy(status = TraderStatus.STOPPED) else trader
            }
        }
    }

    private fun TraderDraft.toDetail(id: String, status: TraderStatus) = TraderDetail(
        id = id,
        name = name,
        exchange = exchange,
        strategy = strategy,
        riskLevel = riskLevel,
        baseAsset = baseAsset,
        quoteAsset = quoteAsset,
        budget = budget,
        status = status,
        profitLoss = 0.0,
        openPositions = 0,
        createdAt = Instant.now()
    )

    private fun emitHeartbeat(id: String) {
        scope.launch {
            repeat(3) {
                delay(1_000)
                tradersFlow.update { current ->
                    current.map { trader ->
                        if (trader.id == id) {
                            trader.copy(
                                profitLoss = trader.profitLoss + Random.nextDouble(5.0, 25.0),
                                openPositions = Random.nextInt(0, 3)
                            )
                        } else trader
                    }
                }
            }
        }
    }
}

class StubMarketDataService : MarketDataService {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val candleFlows = Timeframe.values().associateWith { MutableStateFlow(generateCandles(it)) }
    private val positionsFlow = MutableStateFlow(generatePositions())
    private val tradesFlow = MutableStateFlow(generateTrades())
    private val connectionFlow = MutableStateFlow(ConnectionStatus.CONNECTED)

    init {
        var tick = 0
        scope.launch {
            while (true) {
                delay(2_000)
                tick++
                candleFlows.forEach { (timeframe, flow) ->
                    flow.update { candles ->
                        val next = candles.last().let { previous ->
                            val delta = Random.nextDouble(-10.0, 10.0)
                            val nextClose = (previous.close + delta).coerceAtLeast(1.0)
                            previous.copy(
                                timestamp = previous.timestamp.plusSeconds(timeframe.toSeconds()),
                                open = previous.close,
                                high = maxOf(previous.close, nextClose) + Random.nextDouble(0.0, 5.0),
                                low = minOf(previous.close, nextClose) - Random.nextDouble(0.0, 5.0),
                                close = nextClose,
                                volume = Random.nextDouble(10.0, 100.0)
                            )
                        }
                        (candles.drop(1) + next)
                    }
                }
                positionsFlow.update { positions ->
                    positions.map {
                        it.copy(
                            markPrice = it.markPrice + Random.nextDouble(-5.0, 5.0),
                            pnl = it.pnl + Random.nextDouble(-25.0, 25.0)
                        )
                    }
                }
                tradesFlow.update { trades ->
                    val newTrade = TradeRecord(
                        id = UUID.randomUUID().toString(),
                        traderName = listOf("Momentum Alpha", "Range Rider").random(),
                        symbol = listOf("BTCUSDT", "ETHUSDT").random(),
                        side = if (Random.nextBoolean()) TradeSide.BUY else TradeSide.SELL,
                        qty = Random.nextDouble(0.1, 1.5),
                        price = Random.nextDouble(25000.0, 31000.0),
                        pnl = Random.nextDouble(-50.0, 75.0),
                        timestamp = Instant.now()
                    )
                    listOf(newTrade) + trades.take(49)
                }
                connectionFlow.update { current ->
                    when {
                        tick % 20 == 0 -> ConnectionStatus.RECONNECTING
                        tick % 20 == 2 && current == ConnectionStatus.RECONNECTING -> ConnectionStatus.CONNECTED
                        tick % 45 == 0 -> ConnectionStatus.DISCONNECTED
                        current == ConnectionStatus.DISCONNECTED -> ConnectionStatus.CONNECTED
                        else -> current
                    }
                }
            }
        }
    }

    override fun candlesticks(timeframe: Timeframe): Flow<List<Candlestick>> = candleFlows.getValue(timeframe).asStateFlow()

    override fun positions(): Flow<List<OpenPosition>> = positionsFlow.asStateFlow()

    override fun tradeHistory(): Flow<List<TradeRecord>> = tradesFlow.asStateFlow()

    override fun connectionStatus(): Flow<ConnectionStatus> = connectionFlow.asStateFlow()

    private fun generateCandles(timeframe: Timeframe): List<Candlestick> {
        val now = ZonedDateTime.now(ZoneOffset.UTC).withSecond(0).withNano(0)
        return (0 until 50).map { index ->
            val ts = now.minusMinutes(timeframe.toMinutes() * (50 - index)).toInstant()
            val base = 28000 + Random.nextDouble(-500.0, 500.0)
            Candlestick(
                timestamp = ts,
                open = base,
                high = base + Random.nextDouble(0.0, 50.0),
                low = base - Random.nextDouble(0.0, 50.0),
                close = base + Random.nextDouble(-25.0, 25.0),
                volume = Random.nextDouble(10.0, 100.0)
            )
        }
    }

    private fun Timeframe.toMinutes(): Long = when (this) {
        Timeframe.ONE_MIN -> 1
        Timeframe.FIVE_MIN -> 5
        Timeframe.FIFTEEN_MIN -> 15
        Timeframe.ONE_HOUR -> 60
    }

    private fun Timeframe.toSeconds(): Long = toMinutes() * 60

    private fun generatePositions(): List<OpenPosition> = listOf(
        OpenPosition(
            id = "POS-001",
            traderName = "Momentum Alpha",
            symbol = "BTCUSDT",
            size = 0.5,
            entryPrice = 27650.0,
            markPrice = 28010.0,
            pnl = 180.0,
            status = TraderStatus.RUNNING
        ),
        OpenPosition(
            id = "POS-002",
            traderName = "Range Rider",
            symbol = "ETHUSDT",
            size = 2.0,
            entryPrice = 1780.0,
            markPrice = 1755.0,
            pnl = -50.0,
            status = TraderStatus.RUNNING
        )
    )

    private fun generateTrades(): List<TradeRecord> = (0 until 10).map {
        TradeRecord(
            id = "TR-${1000 + it}",
            traderName = listOf("Momentum Alpha", "Range Rider").random(),
            symbol = listOf("BTCUSDT", "ETHUSDT").random(),
            side = if (Random.nextBoolean()) TradeSide.BUY else TradeSide.SELL,
            qty = Random.nextDouble(0.1, 1.0),
            price = Random.nextDouble(25000.0, 31000.0),
            pnl = Random.nextDouble(-100.0, 150.0),
            timestamp = Instant.now().minusSeconds((it * 300).toLong())
        )
    }
}


