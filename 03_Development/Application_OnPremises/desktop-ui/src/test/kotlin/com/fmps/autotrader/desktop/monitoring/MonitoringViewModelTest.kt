package com.fmps.autotrader.desktop.monitoring

import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.services.Candlestick
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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

    @Test
    fun `collects candlesticks positions and trades`() = scope.runTest {
        advanceUntilIdle()
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(2, state.positions.size)
        assertEquals(1, state.trades.size)
    }

    @Test
    fun `changing timeframe re-subscribes`() = scope.runTest {
        advanceUntilIdle()
        viewModel.changeTimeframe(Timeframe.ONE_MIN)
        advanceUntilIdle()
        assertEquals(Timeframe.ONE_MIN, viewModel.state.value.timeframe)
    }

    private class FakeMarketDataService : MarketDataService {
        private val candles = MutableStateFlow(listOf(sampleCandle()))
        private val positions = MutableStateFlow(samplePositions())
        private val trades = MutableStateFlow(sampleTrades())

        override fun candlesticks(timeframe: Timeframe): Flow<List<Candlestick>> = candles
        override fun positions(): Flow<List<OpenPosition>> = positions
        override fun tradeHistory(): Flow<List<TradeRecord>> = trades

        private fun sampleCandle() = Candlestick(
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

