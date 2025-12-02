package com.fmps.autotrader.desktop.config

import com.fmps.autotrader.desktop.mvvm.ViewEvent
import com.fmps.autotrader.desktop.services.ConnectionStatus
import com.fmps.autotrader.desktop.services.ConnectionTestResult
import com.fmps.autotrader.desktop.services.Exchange
import com.fmps.autotrader.desktop.services.ExchangeSettings
import com.fmps.autotrader.desktop.services.GeneralSettings
import com.fmps.autotrader.desktop.services.LoggingLevel
import com.fmps.autotrader.desktop.services.ThemePreference
import com.fmps.autotrader.desktop.services.TraderDefaults

data class ConfigurationState(
    val isLoading: Boolean = true,
    val exchangeForm: ExchangeForm = ExchangeForm(),
    val generalForm: GeneralForm = GeneralForm(),
    val traderDefaultsForm: TraderDefaultsForm = TraderDefaultsForm(),
    val exportContent: String? = null,
    val importPreview: String? = null,
    val validationErrors: Map<String, String> = emptyMap(),
    val connectionTest: ConnectionTestResult? = null,
    val isTestingConnection: Boolean = false,
    val isSaving: Boolean = false,
    val importError: String? = null,
    val importSuccess: Boolean = false,
    val coreServiceStatus: ConnectionStatus = ConnectionStatus.RECONNECTING,
    // Track last saved timestamp per exchange
    val lastSavedTimestamp: Map<Exchange, Long> = emptyMap(),
    // Track connection status per exchange: null = not tested, true = connected, false = failed
    val exchangeConnectionStatus: Map<Exchange, Boolean?> = emptyMap(),
    // Track if exchange form has unsaved changes (dirty state)
    val hasUnsavedExchangeChanges: Boolean = false,
    // Track if general form has unsaved changes (dirty state)
    val hasUnsavedGeneralChanges: Boolean = false
)

data class ExchangeForm(
    val exchange: Exchange = Exchange.BINANCE,
    val apiKey: String = "",
    val secretKey: String = "",
    val passphrase: String = ""
) {
    fun toSettings() = ExchangeSettings(apiKey, secretKey, passphrase, exchange)
}

data class GeneralForm(
    val updateIntervalSeconds: Int = 30,
    val telemetryPollingSeconds: Int = 5,
    val loggingLevel: LoggingLevel = LoggingLevel.INFO,
    val theme: ThemePreference = ThemePreference.AUTO
) {
    fun toSettings() = GeneralSettings(updateIntervalSeconds, telemetryPollingSeconds, loggingLevel, theme)
}

data class TraderDefaultsForm(
    val budgetUsd: Double = 1000.0,
    val leverage: Int = 3,
    val stopLossPercent: Double = 5.0,
    val takeProfitPercent: Double = 5.0,
    val strategy: String = "TREND_FOLLOWING" // TradingStrategy enum name
) {
    fun toDefaults() = TraderDefaults(budgetUsd, leverage, stopLossPercent, takeProfitPercent, strategy)
}

sealed interface ConfigurationEvent : ViewEvent {
    data class ShowMessage(val message: String, val level: MessageLevel = MessageLevel.INFO) : ConfigurationEvent

    enum class MessageLevel { INFO, SUCCESS, ERROR }
}

