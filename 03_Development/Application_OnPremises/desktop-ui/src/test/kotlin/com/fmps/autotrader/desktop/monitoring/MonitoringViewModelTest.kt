package com.fmps.autotrader.desktop.monitoring

import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.services.Candlestick
import com.fmps.autotrader.desktop.services.ConnectionStatus
import com.fmps.autotrader.desktop.services.MarketDataService
import com.fmps.autotrader.desktop.services.OpenPosition
import com.fmps.autotrader.desktop.services.Timeframe
import com.fmps.autotrader.desktop.services.TradeRecord
import com.fmps.autotrader.desktop.services.TradeSide
import com.fmps.autotrader.desktop.services.TraderStatus
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class MonitoringViewModelTest {

    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var fakeService: FakeMarketDataService
    private lateinit var viewModel: MonitoringViewModel
    private lateinit var scope: TestScope

    @BeforeEach
    fun setup() {
        val dispatcher = StandardTestDispatcher()
        scope = TestScope(dispatcher)
        dispatcherProvider = object : DispatcherProvider {
            override val main = dispatcher
            override val io = dispatcher
            override val computation = dispatcher
        }
        fakeService = FakeMarketDataService()
        viewModel = MonitoringViewModel(dispatcherProvider, fakeService)
    }

    @AfterEach
    fun cleanup() {
        // Additional cleanup if needed - onCleared() is called in each test
        scope.cancel()
    }

    @Test
    fun `collects candlesticks positions and trades`() = scope.runTest {
        advanceUntilIdle()
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(2, state.positions.size)
        assertEquals(1, state.trades.size)
        viewModel.onCleared()
        advanceUntilIdle()
    }

    @Test
    fun `changing timeframe re-subscribes`() = scope.runTest {
        advanceUntilIdle()
        viewModel.changeTimeframe(Timeframe.ONE_MIN)
        advanceUntilIdle()
        assertEquals(Timeframe.ONE_MIN, viewModel.state.value.timeframe)
        viewModel.onCleared()
        advanceUntilIdle()
    }

    @Test
    fun `connection status updates state`() = scope.runTest {
        advanceUntilIdle()
        fakeService.emitConnection(ConnectionStatus.RECONNECTING)
        advanceUntilIdle()
        assertEquals(ConnectionStatus.RECONNECTING, viewModel.state.value.connectionStatus)
        viewModel.onCleared()
        advanceUntilIdle()
    }

    @Test
    fun `manual refresh clears refreshing flag`() = scope.runTest {
        advanceUntilIdle()
        viewModel.refresh()
        fakeService.emitCandles(
            Timeframe.FIVE_MIN,
            listOf(fakeService.sampleCandle().copy(close = 2.0))
        )
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isRefreshing)
        assertNotNull(viewModel.state.value.lastUpdated)
        viewModel.onCleared()
        advanceUntilIdle()
    }

    private class FakeMarketDataService : MarketDataService {
        private val candles = Timeframe.values().associateWith { MutableStateFlow(listOf(sampleCandle())) }
        private val positions = MutableStateFlow(samplePositions())
        private val trades = MutableStateFlow(sampleTrades())
        private val connection = MutableStateFlow(ConnectionStatus.CONNECTED)

        override fun candlesticks(timeframe: Timeframe): Flow<List<Candlestick>> = candles.getValue(timeframe)
        override fun positions(): Flow<List<OpenPosition>> = positions
        override fun tradeHistory(): Flow<List<TradeRecord>> = trades
        override fun connectionStatus(): Flow<ConnectionStatus> = connection

        fun emitConnection(status: ConnectionStatus) {
            connection.value = status
        }

        fun emitCandles(timeframe: Timeframe, data: List<Candlestick>) {
            candles.getValue(timeframe).value = data
        }

        fun sampleCandle() = Candlestick(
            timestamp = Instant.now(),
            open = 1.0,
            high = 2.0,
            low = 0.5,
            close = 1.5,
            volume = 100.0
        )

        private fun samplePositions() = listOf(
            OpenPosition(
                id = "POS-1",
                traderName = "Momentum Alpha",
                symbol = "BTCUSDT",
                size = 0.5,
                entryPrice = 25000.0,
                markPrice = 25500.0,
                pnl = 250.0,
                status = TraderStatus.RUNNING
            ),
            OpenPosition(
                id = "POS-2",
                traderName = "Range Rider",
                symbol = "ETHUSDT",
                size = 1.5,
                entryPrice = 1800.0,
                markPrice = 1780.0,
                pnl = -30.0,
                status = TraderStatus.RUNNING
            )
        )

        private fun sampleTrades() = listOf(
            TradeRecord(
                id = "TR-1",
                traderName = "Momentum Alpha",
                symbol = "BTCUSDT",
                side = TradeSide.BUY,
                qty = 0.25,
                price = 25200.0,
                pnl = 120.0,
                timestamp = Instant.now()
            )
        )
    }
}

