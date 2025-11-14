package com.fmps.autotrader.desktop.config

import com.fmps.autotrader.desktop.config.ConfigurationEvent.MessageLevel
import com.fmps.autotrader.desktop.mvvm.BaseViewModel
import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.services.ConfigService
import com.fmps.autotrader.desktop.services.ConfigurationSnapshot
import com.fmps.autotrader.desktop.services.Exchange
import com.fmps.autotrader.desktop.services.ExchangeSettings
import com.fmps.autotrader.desktop.services.GeneralSettings
import com.fmps.autotrader.desktop.services.TraderDefaults
import kotlinx.coroutines.flow.collectLatest

class ConfigurationViewModel(
    dispatcherProvider: DispatcherProvider,
    private val configService: ConfigService
) : BaseViewModel<ConfigurationState, ConfigurationEvent>(ConfigurationState(), dispatcherProvider) {

    init {
        launchIO {
            configService.configuration().collectLatest { snapshot ->
                setState {
                    it.copy(
                        isLoading = false,
                        exchangeForm = snapshot.exchange.toForm(),
                        generalForm = snapshot.general.toForm(),
                        traderDefaultsForm = snapshot.traderDefaults.toForm()
                    )
                }
            }
        }
    }

    fun updateExchangeForm(updater: (ExchangeForm) -> ExchangeForm) {
        setState { it.copy(exchangeForm = updater(it.exchangeForm)) }
    }

    fun updateGeneralForm(updater: (GeneralForm) -> GeneralForm) {
        setState { it.copy(generalForm = updater(it.generalForm)) }
    }

    fun updateTraderDefaults(updater: (TraderDefaultsForm) -> TraderDefaultsForm) {
        setState { it.copy(traderDefaultsForm = updater(it.traderDefaultsForm)) }
    }

    fun saveExchangeSettings() {
        val form = state.value.exchangeForm
        val errors = validateExchange(form)
        if (errors.isNotEmpty()) {
            setState { it.copy(validationErrors = errors) }
            return
        }
        setState { it.copy(validationErrors = emptyMap(), isSaving = true) }
        launchIO {
            try {
                configService.saveExchangeSettings(form.toSettings())
                publishEvent(ConfigurationEvent.ShowMessage("Exchange settings saved", MessageLevel.SUCCESS))
            } catch (ex: Exception) {
                publishEvent(ConfigurationEvent.ShowMessage(ex.message ?: "Unable to save exchange settings", MessageLevel.ERROR))
            } finally {
                setState { it.copy(isSaving = false) }
            }
        }
    }

    fun saveGeneralSettings() {
        val form = state.value.generalForm
        val errors = validateGeneral(form)
        if (errors.isNotEmpty()) {
            setState { it.copy(validationErrors = errors) }
            return
        }
        setState { it.copy(validationErrors = emptyMap(), isSaving = true) }
        launchIO {
            try {
                configService.saveGeneralSettings(form.toSettings())
                publishEvent(ConfigurationEvent.ShowMessage("General settings saved", MessageLevel.SUCCESS))
            } catch (ex: Exception) {
                publishEvent(ConfigurationEvent.ShowMessage(ex.message ?: "Unable to save general settings", MessageLevel.ERROR))
            } finally {
                setState { it.copy(isSaving = false) }
            }
        }
    }

    fun saveTraderDefaults() {
        val form = state.value.traderDefaultsForm
        val errors = validateTraderDefaults(form)
        if (errors.isNotEmpty()) {
            setState { it.copy(validationErrors = errors) }
            return
        }
        setState { it.copy(validationErrors = emptyMap(), isSaving = true) }
        launchIO {
            try {
                configService.saveTraderDefaults(form.toDefaults())
                publishEvent(ConfigurationEvent.ShowMessage("Trader defaults saved", MessageLevel.SUCCESS))
            } catch (ex: Exception) {
                publishEvent(ConfigurationEvent.ShowMessage(ex.message ?: "Unable to save trader defaults", MessageLevel.ERROR))
            } finally {
                setState { it.copy(isSaving = false) }
            }
        }
    }

    fun testConnection() {
        val form = state.value.exchangeForm
        setState { it.copy(isTestingConnection = true) }
        launchIO {
            try {
                val result = configService.testExchangeConnection(form.toSettings())
                setState { it.copy(connectionTest = result) }
                publishEvent(
                    ConfigurationEvent.ShowMessage(
                        result.message,
                        if (result.success) MessageLevel.SUCCESS else MessageLevel.ERROR
                    )
                )
            } catch (ex: Exception) {
                publishEvent(ConfigurationEvent.ShowMessage(ex.message ?: "Connection test failed", MessageLevel.ERROR))
            } finally {
                setState { it.copy(isTestingConnection = false) }
            }
        }
    }

    fun exportConfiguration() {
        setState { it.copy(isSaving = true) }
        launchIO {
            try {
                val content = configService.exportConfiguration()
                setState { it.copy(exportContent = content, isSaving = false) }
            } catch (ex: Exception) {
                publishEvent(ConfigurationEvent.ShowMessage(ex.message ?: "Export failed", MessageLevel.ERROR))
                setState { it.copy(isSaving = false) }
            }
        }
    }

    fun importConfiguration(serialized: String) {
        if (serialized.isBlank()) {
            setState { it.copy(importError = "Import file is empty") }
            return
        }
        setState { it.copy(isSaving = true, importError = null, importSuccess = false) }
        launchIO {
            try {
                val snapshot = configService.importConfiguration(serialized)
                setState {
                    it.copy(
                        exchangeForm = snapshot.exchange.toForm(),
                        generalForm = snapshot.general.toForm(),
                        traderDefaultsForm = snapshot.traderDefaults.toForm(),
                        importSuccess = true,
                        isSaving = false
                    )
                }
                publishEvent(ConfigurationEvent.ShowMessage("Configuration imported", MessageLevel.SUCCESS))
            } catch (ex: Exception) {
                setState { it.copy(importError = ex.message ?: "Import failed", isSaving = false) }
                publishEvent(ConfigurationEvent.ShowMessage("Import failed", MessageLevel.ERROR))
            }
        }
    }

    private fun validateExchange(form: ExchangeForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (form.apiKey.isBlank()) errors["apiKey"] = "API key required"
        if (form.secretKey.isBlank()) errors["secretKey"] = "Secret key required"
        if (form.exchange == Exchange.BITGET && form.passphrase.isBlank()) {
            errors["passphrase"] = "Passphrase required for Bitget"
        }
        return errors
    }

    private fun validateGeneral(form: GeneralForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (form.updateIntervalSeconds !in 10..600) errors["updateInterval"] = "Interval must be between 10-600 seconds"
        if (form.telemetryPollingSeconds !in 1..120) errors["telemetryPolling"] = "Telemetry polling 1-120 seconds"
        return errors
    }

    private fun validateTraderDefaults(form: TraderDefaultsForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (form.budgetUsd <= 0) errors["budget"] = "Budget must be positive"
        if (form.leverage !in 1..10) errors["leverage"] = "Leverage must be between 1 and 10"
        if (form.stopLossPercent !in 0.5..50.0) errors["stopLoss"] = "Stop loss range 0.5% - 50%"
        if (form.strategy.isBlank()) errors["strategy"] = "Strategy name required"
        return errors
    }

    private fun ExchangeSettings.toForm() = ExchangeForm(exchange = exchange, apiKey = apiKey, secretKey = secretKey, passphrase = passphrase)
    private fun GeneralSettings.toForm() = GeneralForm(updateIntervalSeconds, telemetryPollingSeconds, loggingLevel, theme)
    private fun TraderDefaults.toForm() = TraderDefaultsForm(budgetUsd, leverage, stopLossPercent, strategy)
}

