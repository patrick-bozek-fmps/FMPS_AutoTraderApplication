package com.fmps.autotrader.desktop.monitoring

import com.fmps.autotrader.desktop.mvvm.BaseViewModel
import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.services.MarketDataService
import com.fmps.autotrader.desktop.services.Timeframe
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest

class MonitoringViewModel(
    dispatcherProvider: DispatcherProvider,
    private val marketDataService: MarketDataService
) : BaseViewModel<MonitoringState, MonitoringEvent>(MonitoringState(), dispatcherProvider) {

    private var candleJob: Job? = null

    init {
        observeCandles(Timeframe.FIVE_MIN)
        observePositions()
        observeTrades()
    }

    private fun observeCandles(timeframe: Timeframe) {
        candleJob?.cancel()
        candleJob = launchIO {
            marketDataService.candlesticks(timeframe).collectLatest { candles ->
                setState { it.copy(isLoading = false, timeframe = timeframe, candles = candles) }
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

    fun changeTimeframe(timeframe: Timeframe) {
        observeCandles(timeframe)
    }
}

