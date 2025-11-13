package com.fmps.autotrader.desktop.services

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
                samples.tryEmit(
                    TelemetrySample(
                        channel = "trader.status",
                        payload = """{"id":"T-001","status":"RUNNING","heartbeat":"${System.currentTimeMillis()}"}"""
                    )
                )
            }
        }
    }

    override fun stop() {
        running = false
    }

    override fun samples(): Flow<TelemetrySample> = samples.asSharedFlow()
}


