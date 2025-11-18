package com.fmps.autotrader.desktop.config

import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.services.ConfigService
import com.fmps.autotrader.desktop.services.ConfigurationSnapshot
import com.fmps.autotrader.desktop.services.ConnectionTestResult
import com.fmps.autotrader.desktop.services.Exchange
import com.fmps.autotrader.desktop.services.ExchangeSettings
import com.fmps.autotrader.desktop.services.GeneralSettings
import com.fmps.autotrader.desktop.services.LoggingLevel
import com.fmps.autotrader.desktop.services.ThemePreference
import com.fmps.autotrader.desktop.services.TraderDefaults
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigurationViewModelTest {

    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var testScope: TestScope
    private lateinit var fakeService: FakeConfigService
    private lateinit var viewModel: ConfigurationViewModel

    @BeforeAll
    fun beforeAll() {
        println("[DEBUG] ConfigurationViewModelTest class: Starting test class")
    }

    @AfterAll
    fun afterAll() {
        println("[DEBUG] ConfigurationViewModelTest class: All tests complete, cleaning up")
        // Give a moment for any pending operations to complete
        Thread.sleep(100)
        println("[DEBUG] ConfigurationViewModelTest class: Cleanup complete")
    }

    @BeforeEach
    fun setup(testInfo: TestInfo) {
        println("[DEBUG] Starting test: ${testInfo.displayName}")
        val dispatcher = StandardTestDispatcher()
        testScope = TestScope(dispatcher)
        dispatcherProvider = object : DispatcherProvider {
            override val main = dispatcher
            override val io = dispatcher
            override val computation = dispatcher
        }
        fakeService = FakeConfigService()
        viewModel = ConfigurationViewModel(dispatcherProvider, fakeService)
        println("[DEBUG] ViewModel created for test: ${testInfo.displayName}")
    }

    @AfterEach
    fun cleanup(testInfo: TestInfo) {
        println("[DEBUG] Cleaning up after test: ${testInfo.displayName}")
        // Ensure ViewModel is cleaned up even if test didn't call onCleared()
        try {
            viewModel.onCleared()
            println("[DEBUG] ViewModel.onCleared() called for: ${testInfo.displayName}")
        } catch (e: Exception) {
            println("[DEBUG] Exception during cleanup for ${testInfo.displayName}: ${e.message}")
            // Ignore if already cleared
        }
        println("[DEBUG] Cleanup complete for: ${testInfo.displayName}")
    }

    @Test
    fun `loads configuration snapshot`() = testScope.runTest {
        println("[DEBUG] Test: loads configuration snapshot - starting")
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isLoading)
        assertEquals("BINANCE_KEY", viewModel.state.value.exchangeForm.apiKey)
        println("[DEBUG] Test: loads configuration snapshot - assertions passed, cleaning up")
        viewModel.onCleared()
        // Ensure cancellation completes
        advanceUntilIdle()
        advanceUntilIdle()
        println("[DEBUG] Test: loads configuration snapshot - complete")
    }

    @Test
    fun `save exchange validates fields`() = testScope.runTest {
        println("[DEBUG] Test: save exchange validates fields - starting")
        advanceUntilIdle()
        viewModel.updateExchangeForm { it.copy(apiKey = "", secretKey = "") }
        viewModel.saveExchangeSettings()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.validationErrors.containsKey("apiKey"))
        println("[DEBUG] Test: save exchange validates fields - assertions passed, cleaning up")
        viewModel.onCleared()
        advanceUntilIdle()
        advanceUntilIdle()
        println("[DEBUG] Test: save exchange validates fields - complete")
    }

    @Test
    fun `import configuration updates state`() = testScope.runTest {
        println("[DEBUG] Test: import configuration updates state - starting")
        advanceUntilIdle()
        viewModel.importConfiguration("sample")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.importSuccess)
        assertEquals(ThemePreference.DARK, viewModel.state.value.generalForm.theme)
        println("[DEBUG] Test: import configuration updates state - assertions passed, cleaning up")
        viewModel.onCleared()
        advanceUntilIdle()
        advanceUntilIdle()
        println("[DEBUG] Test: import configuration updates state - complete")
    }

    @Test
    fun `test connection publishes result`() = testScope.runTest {
        println("[DEBUG] Test: test connection publishes result - starting")
        advanceUntilIdle()
        viewModel.testConnection()
        advanceUntilIdle()
        assertEquals("OK", viewModel.state.value.connectionTest?.message)
        println("[DEBUG] Test: test connection publishes result - assertions passed, cleaning up")
        viewModel.onCleared()
        advanceUntilIdle()
        advanceUntilIdle()
        println("[DEBUG] Test: test connection publishes result - complete")
    }

    private class FakeConfigService : ConfigService {
        private val snapshot = MutableStateFlow(
            ConfigurationSnapshot(
                exchange = ExchangeSettings("BINANCE_KEY", "BINANCE_SECRET", "", Exchange.BINANCE),
                general = GeneralSettings(30, 5, LoggingLevel.INFO, ThemePreference.AUTO),
                traderDefaults = TraderDefaults()
            )
        )

        override fun configuration(): Flow<ConfigurationSnapshot> = snapshot

        override suspend fun saveExchangeSettings(settings: ExchangeSettings) {
            snapshot.value = snapshot.value.copy(exchange = settings)
        }

        override suspend fun saveGeneralSettings(settings: GeneralSettings) {
            snapshot.value = snapshot.value.copy(general = settings)
        }

        override suspend fun saveTraderDefaults(defaults: TraderDefaults) {
            snapshot.value = snapshot.value.copy(traderDefaults = defaults)
        }

        override suspend fun testExchangeConnection(settings: ExchangeSettings): ConnectionTestResult {
            return ConnectionTestResult(true, "OK")
        }

        override suspend fun exportConfiguration(): String = "key=value"

        override suspend fun importConfiguration(serialized: String): ConfigurationSnapshot {
            val updated = snapshot.value.copy(general = snapshot.value.general.copy(theme = ThemePreference.DARK))
            snapshot.value = updated
            return updated
        }
    }
}

