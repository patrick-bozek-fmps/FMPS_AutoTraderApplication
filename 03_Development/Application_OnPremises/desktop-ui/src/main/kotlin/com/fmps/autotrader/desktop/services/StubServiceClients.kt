package com.fmps.autotrader.desktop.services

import java.time.Instant
import java.util.UUID
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
import kotlin.random.Random

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


