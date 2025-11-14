package com.fmps.autotrader.desktop.dashboard

import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.services.CoreServiceClient
import com.fmps.autotrader.desktop.services.TelemetryClient
import com.fmps.autotrader.desktop.services.TelemetrySample
import com.fmps.autotrader.desktop.services.TraderStatus
import com.fmps.autotrader.desktop.services.TraderSummary
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javafx.application.Platform
import javafx.scene.control.ListView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.koin.core.component.get
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("ui")
class DashboardViewTest {

    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)
    private val dispatcherProvider = object : DispatcherProvider {
        override val main = dispatcher
        override val io = dispatcher
        override val computation = dispatcher
    }
    private val coreServiceClient = ViewFakeCoreServiceClient()
    private val telemetryClient = ViewFakeTelemetryClient()

    @BeforeAll
    fun initialiseToolkit() {
        val latch = CountDownLatch(1)
        try {
            Platform.startup { latch.countDown() }
        } catch (_: IllegalStateException) {
            Platform.runLater { latch.countDown() }
        }
        latch.await(5, TimeUnit.SECONDS)
    }

    @BeforeEach
    fun setupKoin() {
        startKoin {
            modules(
                module {
                    single<DispatcherProvider> { dispatcherProvider }
                    single<CoreServiceClient> { coreServiceClient }
                    single<TelemetryClient> { telemetryClient }
                    factory { DashboardViewModel(get(), get(), get()) }
                    factory { DashboardView() }
                }
            )
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
        coreServiceClient.reset()
        telemetryClient.reset()
    }

    @AfterAll
    fun shutdownToolkit() {
        val latch = CountDownLatch(1)
        Platform.runLater {
            Platform.exit()
            latch.countDown()
        }
        latch.await(5, TimeUnit.SECONDS)
    }

    @Test
    fun `dashboard view renders trader list`() = testScope.runTest {
        val view = createView()
        val samples = listOf(
            TraderSummary("T-100", "Momentum Alpha", "Binance", TraderStatus.RUNNING, 150.0, 2),
            TraderSummary("T-200", "Range Rider", "Bitget", TraderStatus.STOPPED, -12.0, 0)
        )
        coreServiceClient.emit(samples)
        advanceUntilIdle()

        val latch = CountDownLatch(1)
        Platform.runLater {
            val list = view.root.lookup("#dashboard-trader-list") as ListView<*>
            assertEquals(2, list.items.size)
            latch.countDown()
        }
        latch.await(5, TimeUnit.SECONDS)

        // clean up view model
        GlobalContext.get().get<DashboardViewModel>().onCleared()
    }

    private fun createView(): DashboardView {
        val latch = CountDownLatch(1)
        var view: DashboardView? = null
        Platform.runLater {
            view = GlobalContext.get().get()
            latch.countDown()
        }
        latch.await(5, TimeUnit.SECONDS)
        return view!!.also { assertNotNull(it.root) }
    }

    private class ViewFakeCoreServiceClient : CoreServiceClient {
        private val flow = MutableSharedFlow<List<TraderSummary>>(replay = 1)

        override fun traderSummaries(): Flow<List<TraderSummary>> = flow

        suspend fun emit(value: List<TraderSummary>) {
            flow.emit(value)
        }

        fun reset() {
            flow.resetReplayCache()
        }
    }

    private class ViewFakeTelemetryClient : TelemetryClient {
        private val flow = MutableSharedFlow<TelemetrySample>(extraBufferCapacity = 8)

        override fun start() = Unit

        override fun stop() = Unit

        override fun samples(): Flow<TelemetrySample> = flow

        fun reset() {
            flow.resetReplayCache()
        }
    }
}


