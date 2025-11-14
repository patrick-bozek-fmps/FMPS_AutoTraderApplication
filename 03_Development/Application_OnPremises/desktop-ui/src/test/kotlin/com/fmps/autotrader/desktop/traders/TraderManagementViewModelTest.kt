package com.fmps.autotrader.desktop.traders

import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.services.TraderDetail
import com.fmps.autotrader.desktop.services.TraderDraft
import com.fmps.autotrader.desktop.services.TraderRiskLevel
import com.fmps.autotrader.desktop.services.TraderService
import com.fmps.autotrader.desktop.services.TraderStatus
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TraderManagementViewModelTest {

    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var fakeService: FakeTraderService
    private lateinit var viewModel: TraderManagementViewModel
    private lateinit var testScope: TestScope

    @BeforeEach
    fun setup() {
        val dispatcher = StandardTestDispatcher()
        testScope = TestScope(dispatcher)
        dispatcherProvider = object : DispatcherProvider {
            override val main = dispatcher
            override val io = dispatcher
            override val computation = dispatcher
        }
        fakeService = FakeTraderService()
        viewModel = TraderManagementViewModel(dispatcherProvider, fakeService)
    }

    @Test
    fun `collecting traders updates state`() = testScope.runTest {
        advanceUntilIdle()
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(1, state.traders.size)
        assertEquals("Starter", state.traders.first().name)
    }

    @Test
    fun `creating trader validates input`() = testScope.runTest {
        advanceUntilIdle()

        viewModel.saveTrader()
        advanceUntilIdle()

        val errors = viewModel.state.value.form.errors
        assertTrue(errors.containsKey("name"))

        viewModel.updateForm {
            it.copy(
                name = "New Trader",
                baseAsset = "BTC",
                quoteAsset = "USDT",
                budget = 1500.0
            )
        }
        viewModel.saveTrader()
        advanceUntilIdle()

        assertTrue(fakeService.traders.value.any { it.name == "New Trader" })
    }

    @Test
    fun `start trader emits event`() = testScope.runTest {
        advanceUntilIdle()
        val detail = fakeService.traders.value.first()

        viewModel.startTrader(detail.id)
        advanceUntilIdle()

        assertEquals(TraderStatus.RUNNING, fakeService.traders.value.first().status)
    }

    private class FakeTraderService : TraderService {
        private val idCounter = AtomicInteger(1)
        val traders = MutableStateFlow(
            listOf(
                TraderDetail(
                    id = "T-${idCounter.getAndIncrement()}",
                    name = "Starter",
                    exchange = "Binance",
                    strategy = "Momentum",
                    riskLevel = TraderRiskLevel.BALANCED,
                    baseAsset = "BTC",
                    quoteAsset = "USDT",
                    budget = 1000.0,
                    status = TraderStatus.STOPPED,
                    profitLoss = 0.0,
                    openPositions = 0,
                    createdAt = Instant.now()
                )
            )
        )

        override fun traders(): Flow<List<TraderDetail>> = traders

        override suspend fun createTrader(draft: TraderDraft): TraderDetail {
            val detail = draft.toDetail("T-${idCounter.getAndIncrement()}")
            traders.value = listOf(detail) + traders.value
            return detail
        }

        override suspend fun updateTrader(id: String, draft: TraderDraft): TraderDetail {
            val detail = draft.toDetail(id)
            traders.value = traders.value.map { if (it.id == id) detail else it }
            return detail
        }

        override suspend fun deleteTrader(id: String) {
            traders.value = traders.value.filterNot { it.id == id }
        }

        override suspend fun startTrader(id: String) {
            traders.value = traders.value.map {
                if (it.id == id) it.copy(status = TraderStatus.RUNNING) else it
            }
        }

        override suspend fun stopTrader(id: String) {
            traders.value = traders.value.map {
                if (it.id == id) it.copy(status = TraderStatus.STOPPED) else it
            }
        }

        private fun TraderDraft.toDetail(id: String) = TraderDetail(
            id = id,
            name = name,
            exchange = exchange,
            strategy = strategy,
            riskLevel = riskLevel,
            baseAsset = baseAsset,
            quoteAsset = quoteAsset,
            budget = budget,
            status = TraderStatus.STOPPED,
            profitLoss = 0.0,
            openPositions = 0,
            createdAt = Instant.now()
        )
    }
}

