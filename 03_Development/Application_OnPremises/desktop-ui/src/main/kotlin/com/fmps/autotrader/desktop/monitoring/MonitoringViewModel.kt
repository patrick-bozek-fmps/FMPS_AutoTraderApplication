package com.fmps.autotrader.desktop.monitoring

import com.fmps.autotrader.desktop.mvvm.BaseViewModel
import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.services.MarketDataService
import com.fmps.autotrader.desktop.services.Timeframe
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest

class MonitoringViewModel(
    dispatcherProvider: DispatcherProvider,
    private val marketDataService: MarketDataService
) : BaseViewModel<MonitoringState, MonitoringEvent>(MonitoringState(), dispatcherProvider) {

    private var candleJob: Job? = null
    private var currentTimeframe: Timeframe = Timeframe.FIVE_MIN

    init {
        observeCandles(currentTimeframe)
        observePositions()
        observeTrades()
        observeConnectionStatus()
    }

    private fun observeCandles(timeframe: Timeframe) {
        currentTimeframe = timeframe
        candleJob?.cancel()
        setState { it.copy(isRefreshing = true, timeframe = timeframe) }
        candleJob = launchIO {
            marketDataService.candlesticks(timeframe).collectLatest { candles ->
                val now = Instant.now()
                val latency = candles.lastOrNull()?.timestamp?.let { ts ->
                    Duration.between(ts, now).abs().toMillis()
                } ?: 0
                setState {
                    it.copy(
                        isLoading = false,
                        timeframe = timeframe,
                        candles = candles,
                        lastUpdated = now,
                        latencyMs = latency,
                        isRefreshing = false
                    )
                }
            }
        }
    }

    private fun observePositions() {
        launchIO {
            marketDataService.positions().collectLatest { positions ->
                setState { it.copy(positions = positions) }
            }
        }
    }

    private fun observeTrades() {
        launchIO {
            marketDataService.tradeHistory().collectLatest { trades ->
                setState { it.copy(trades = trades) }
            }
        }
    }

    private fun observeConnectionStatus() {
        launchIO {
            marketDataService.connectionStatus().collectLatest { status ->
                setState { it.copy(connectionStatus = status) }
            }
        }
    }

    fun changeTimeframe(timeframe: Timeframe) {
        observeCandles(timeframe)
    }

    fun refresh() {
        observeCandles(currentTimeframe)
    }
}

