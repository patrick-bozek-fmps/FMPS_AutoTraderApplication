package com.fmps.autotrader.desktop.dashboard

import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.services.CoreServiceClient
import com.fmps.autotrader.desktop.services.TelemetryClient
import com.fmps.autotrader.desktop.services.TelemetrySample
import com.fmps.autotrader.desktop.services.TraderService
import com.fmps.autotrader.desktop.services.TraderStatus
import com.fmps.autotrader.desktop.services.TraderSummary
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private lateinit var testScope: TestScope
    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var coreServiceClient: FakeCoreServiceClient
    private lateinit var telemetryClient: FakeTelemetryClient
    private lateinit var traderService: FakeTraderService

    @BeforeEach
    fun setup(testInfo: TestInfo) {
        println("[DEBUG] DashboardViewModelTest: Starting test: ${testInfo.displayName}")
        val dispatcher = StandardTestDispatcher()
        testScope = TestScope(dispatcher)
        dispatcherProvider = object : DispatcherProvider {
            override val main = dispatcher
            override val io = dispatcher
            override val computation = dispatcher
        }
        coreServiceClient = FakeCoreServiceClient()
        telemetryClient = FakeTelemetryClient()
        traderService = FakeTraderService()
        println("[DEBUG] DashboardViewModelTest: Setup complete for: ${testInfo.displayName}")
    }

    @AfterEach
    fun cleanup(testInfo: TestInfo) {
        println("[DEBUG] DashboardViewModelTest: Cleaning up after test: ${testInfo.displayName}")
        // Reset fake services
        traderService.startCalled = false
        traderService.stopCalled = false
        println("[DEBUG] DashboardViewModelTest: Cleanup complete for: ${testInfo.displayName}")
    }

    @Test
    fun `state reflects trader summaries`() = testScope.runTest {
        println("[DEBUG] DashboardViewModelTest: Test 'state reflects trader summaries' - starting")
        val viewModel = createViewModel()
        advanceUntilIdle()

        val summaries = listOf(
            TraderSummary("T-1", "Alpha", "Binance", TraderStatus.RUNNING, 120.0, 2),
            TraderSummary("T-2", "Beta", "Bitget", TraderStatus.STOPPED, -20.0, 0)
        )

        coreServiceClient.emit(summaries)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.traderItems.size)
        assertEquals(1, state.quickStats.activeTraders)
        assertEquals(1, state.quickStats.stoppedTraders)
        assertEquals(100.0, state.quickStats.totalProfitLoss, 0.01)
        assertFalse(state.isLoading)

        println("[DEBUG] DashboardViewModelTest: Test 'state reflects trader summaries' - assertions passed, cleaning up")
        viewModel.onCleared()
        advanceUntilIdle()
        advanceUntilIdle()
        println("[DEBUG] DashboardViewModelTest: Test 'state reflects trader summaries' - complete")
    }

    @Test
    fun `telemetry updates generate notifications`() = testScope.runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        telemetryClient.emit(TelemetrySample(channel = "system.warning", payload = "CPU usage high"))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.systemStatus.telemetryConnected)
        assertEquals(1, state.notifications.size)
        assertEquals(NotificationSeverity.WARNING, state.notifications.first().severity)

        viewModel.onCleared()
        advanceUntilIdle()
    }

    @Test
    fun `trader action emits toast event`() = testScope.runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val trader = TraderItem("T-1", "Alpha", "Binance", TraderStatus.RUNNING, 10.0, 1)

        val receivedEvents = mutableListOf<DashboardEvent>()
        val collector = launch {
            viewModel.events.take(1).collect { event ->
                receivedEvents += event
            }
        }
        advanceUntilIdle()
        viewModel.onTraderAction(trader, TraderAction.OPEN)
        advanceUntilIdle()

        assertTrue(receivedEvents.firstOrNull() is DashboardEvent.ShowMessage)

        collector.cancelAndJoin()
        viewModel.onCleared()
        advanceUntilIdle()
    }

    @Test
    fun `start trader calls trader service`() = testScope.runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        val trader = TraderItem("T-1", "Alpha", "Binance", TraderStatus.STOPPED, 10.0, 1)

        val receivedEvents = mutableListOf<DashboardEvent>()
        val collector = launch {
            viewModel.events.take(1).collect { event ->
                receivedEvents += event
            }
        }
        advanceUntilIdle()
        viewModel.onTraderAction(trader, TraderAction.START)
        advanceUntilIdle()

        assertTrue(receivedEvents.firstOrNull() is DashboardEvent.ShowMessage)
        assertTrue(traderService.startCalled)

        collector.cancelAndJoin()
        viewModel.onCleared()
        advanceUntilIdle()
    }

    @Test
    fun `stop trader calls trader service`() = testScope.runTest {
        println("[DEBUG] DashboardViewModelTest: Test 'stop trader calls trader service' - starting")
        val viewModel = createViewModel()
        advanceUntilIdle()
        val trader = TraderItem("T-1", "Alpha", "Binance", TraderStatus.RUNNING, 10.0, 1)

        val receivedEvents = mutableListOf<DashboardEvent>()
        val collector = launch {
            viewModel.events.take(1).collect { event ->
                receivedEvents += event
            }
        }
        advanceUntilIdle()
        viewModel.onTraderAction(trader, TraderAction.STOP)
        advanceUntilIdle()

        assertTrue(receivedEvents.firstOrNull() is DashboardEvent.ShowMessage)
        assertTrue(traderService.stopCalled)

        println("[DEBUG] DashboardViewModelTest: Test 'stop trader calls trader service' - assertions passed, cleaning up")
        collector.cancelAndJoin()
        viewModel.onCleared()
        advanceUntilIdle()
        advanceUntilIdle()
        println("[DEBUG] DashboardViewModelTest: Test 'stop trader calls trader service' - complete")
    }

    private fun createViewModel(): DashboardViewModel =
        DashboardViewModel(dispatcherProvider, coreServiceClient, telemetryClient, traderService)

    private class FakeCoreServiceClient : CoreServiceClient {
        private val flow = MutableSharedFlow<List<TraderSummary>>(replay = 1)

        override fun traderSummaries(): Flow<List<TraderSummary>> = flow

        suspend fun emit(value: List<TraderSummary>) {
            flow.emit(value)
        }
    }

    private class FakeTelemetryClient : TelemetryClient {
        private val flow = MutableSharedFlow<TelemetrySample>(extraBufferCapacity = 8)
        private val started = AtomicBoolean(false)

        override fun start() {
            started.set(true)
        }

        override fun stop() {
            started.set(false)
        }

        override fun samples(): Flow<TelemetrySample> = flow

        suspend fun emit(sample: TelemetrySample) {
            require(started.get()) { "Telemetry client not started" }
            flow.emit(sample)
        }
    }

    private class FakeTraderService : TraderService {
        var startCalled = false
        var stopCalled = false

        override fun traders(): Flow<List<com.fmps.autotrader.desktop.services.TraderDetail>> {
            return MutableSharedFlow()
        }

        override suspend fun createTrader(draft: com.fmps.autotrader.desktop.services.TraderDraft): com.fmps.autotrader.desktop.services.TraderDetail {
            throw NotImplementedError()
        }

        override suspend fun updateTrader(id: String, draft: com.fmps.autotrader.desktop.services.TraderDraft): com.fmps.autotrader.desktop.services.TraderDetail {
            throw NotImplementedError()
        }

        override suspend fun deleteTrader(id: String) {
            throw NotImplementedError()
        }

        override suspend fun startTrader(id: String) {
            startCalled = true
        }

        override suspend fun stopTrader(id: String) {
            stopCalled = true
        }
    }
}


