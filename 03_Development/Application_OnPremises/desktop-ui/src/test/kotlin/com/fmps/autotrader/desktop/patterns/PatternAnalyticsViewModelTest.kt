package com.fmps.autotrader.desktop.patterns

import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.services.PatternAnalyticsService
import com.fmps.autotrader.desktop.services.PatternDetail
import com.fmps.autotrader.desktop.services.PatternPerformancePoint
import com.fmps.autotrader.desktop.services.PatternPerformanceStatus
import com.fmps.autotrader.desktop.services.PatternSummary
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class PatternAnalyticsViewModelTest {

    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var service: FakePatternAnalyticsService
    private lateinit var viewModel: PatternAnalyticsViewModel
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
        service = FakePatternAnalyticsService()
        viewModel = PatternAnalyticsViewModel(dispatcherProvider, service)
    }

    @AfterEach
    fun cleanup() {
        // Additional cleanup if needed - onCleared() is called in each test
        scope.cancel()
    }

    @Test
    fun `filters patterns by search term`() = scope.runTest {
        advanceUntilIdle()
        viewModel.updateSearch("Momentum")
        advanceUntilIdle()
        assertEquals(1, viewModel.state.value.filteredPatterns.size)
        viewModel.onCleared()
        advanceUntilIdle()
    }

    @Test
    fun `select pattern loads detail`() = scope.runTest {
        advanceUntilIdle()
        val id = service.summaries.value.first().id
        viewModel.selectPattern(id)
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.selectedDetail)
        viewModel.onCleared()
        advanceUntilIdle()
    }

    @Test
    fun `delete removes selection`() = scope.runTest {
        advanceUntilIdle()
        val id = service.summaries.value.first().id
        viewModel.selectPattern(id)
        advanceUntilIdle()
        viewModel.deleteSelected()
        advanceUntilIdle()
        assertNull(viewModel.state.value.selectedPatternId)
        viewModel.onCleared()
        advanceUntilIdle()
    }

    private class FakePatternAnalyticsService : PatternAnalyticsService {
        val summaries = MutableStateFlow(
            listOf(
                PatternSummary(
                    id = "P-1",
                    name = "Momentum Burst",
                    exchange = "Binance",
                    symbol = "BTCUSDT",
                    timeframe = "15m",
                    trader = "Alpha",
                    successRate = 70.0,
                    profitFactor = 2.0,
                    occurrences = 100,
                    lastUpdated = Instant.now(),
                    status = PatternPerformanceStatus.TOP
                ),
                PatternSummary(
                    id = "P-2",
                    name = "Range Fade",
                    exchange = "Bitget",
                    symbol = "ETHUSDT",
                    timeframe = "1h",
                    trader = "Beta",
                    successRate = 55.0,
                    profitFactor = 1.3,
                    occurrences = 80,
                    lastUpdated = Instant.now(),
                    status = PatternPerformanceStatus.STABLE
                )
            )
        )

        override fun patternSummaries(): Flow<List<PatternSummary>> = summaries

        override suspend fun patternDetail(id: String): PatternDetail =
            PatternDetail(
                summary = summaries.value.first { it.id == id },
                description = "Test pattern",
                indicators = listOf("RSI", "MACD"),
                entryCriteria = listOf("Condition A"),
                exitCriteria = listOf("Condition B"),
                averageHoldMinutes = 60,
                winRate = 70.0,
                averagePnL = 1.5,
                drawdown = 1.0,
                performance = listOf(
                    PatternPerformancePoint(Instant.now(), 70.0, 2.0)
                ),
                distribution = emptyList()
            )

        override suspend fun archivePattern(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun deletePattern(id: String): Result<Unit> {
            summaries.value = summaries.value.filterNot { it.id == id }
            return Result.success(Unit)
        }

        override suspend fun refresh(): Result<Unit> = Result.success(Unit)
    }
}

