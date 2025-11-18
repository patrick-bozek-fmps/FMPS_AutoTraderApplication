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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigurationViewModelTest {

    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var testScope: TestScope
    private lateinit var fakeService: FakeConfigService
    private lateinit var viewModel: ConfigurationViewModel

    @BeforeEach
    fun setup() {
        val dispatcher = StandardTestDispatcher()
        testScope = TestScope(dispatcher)
        dispatcherProvider = object : DispatcherProvider {
            override val main = dispatcher
            override val io = dispatcher
            override val computation = dispatcher
        }
        fakeService = FakeConfigService()
        viewModel = ConfigurationViewModel(dispatcherProvider, fakeService)
    }

    @AfterEach
    fun cleanup() {
        // Additional cleanup if needed - onCleared() is called in each test
        testScope.cancel()
    }

    @Test
    fun `loads configuration snapshot`() = testScope.runTest {
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isLoading)
        assertEquals("BINANCE_KEY", viewModel.state.value.exchangeForm.apiKey)
        viewModel.onCleared()
        advanceUntilIdle()
    }

    @Test
    fun `save exchange validates fields`() = testScope.runTest {
        advanceUntilIdle()
        viewModel.updateExchangeForm { it.copy(apiKey = "", secretKey = "") }
        viewModel.saveExchangeSettings()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.validationErrors.containsKey("apiKey"))
        viewModel.onCleared()
        advanceUntilIdle()
    }

    @Test
    fun `import configuration updates state`() = testScope.runTest {
        advanceUntilIdle()
        viewModel.importConfiguration("sample")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.importSuccess)
        assertEquals(ThemePreference.DARK, viewModel.state.value.generalForm.theme)
        viewModel.onCleared()
        advanceUntilIdle()
    }

    @Test
    fun `test connection publishes result`() = testScope.runTest {
        advanceUntilIdle()
        viewModel.testConnection()
        advanceUntilIdle()
        assertEquals("OK", viewModel.state.value.connectionTest?.message)
        viewModel.onCleared()
        advanceUntilIdle()
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

