package com.fmps.autotrader.desktop.config

import com.fmps.autotrader.desktop.config.ConfigurationEvent.MessageLevel
import com.fmps.autotrader.desktop.mvvm.BaseViewModel
import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.services.ConfigService
import com.fmps.autotrader.desktop.services.ConfigurationSnapshot
import com.fmps.autotrader.desktop.services.ConnectionStatus
import com.fmps.autotrader.desktop.services.ConnectionStatusService
import com.fmps.autotrader.desktop.services.Exchange
import com.fmps.autotrader.desktop.services.ExchangeConnectionStatusService
import com.fmps.autotrader.desktop.services.ExchangeSettings
import com.fmps.autotrader.desktop.services.GeneralSettings
import com.fmps.autotrader.desktop.services.ThemePreference
import com.fmps.autotrader.desktop.services.TraderDefaults
import com.fmps.autotrader.shared.model.TradingStrategy
import javafx.application.Platform
import javafx.stage.Stage
import javafx.stage.Window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import tornadofx.FX

private val logger = KotlinLogging.logger {}

class ConfigurationViewModel(
    dispatcherProvider: DispatcherProvider,
    private val configService: ConfigService,
    private val connectionStatusService: ConnectionStatusService,
    private val exchangeConnectionStatusService: ExchangeConnectionStatusService
) : BaseViewModel<ConfigurationState, ConfigurationEvent>(ConfigurationState(), dispatcherProvider) {

    // Store saved settings per exchange (since the service only stores one exchange at a time)
    // Initialize BEFORE init block to ensure it's available when init runs
    private val savedSettingsCache = mutableMapOf<Exchange, ExchangeSettings>()
    
    // Store saved general settings
    private var savedGeneralSettingsCache: GeneralSettings = GeneralSettings()
    
    // Store saved trader defaults
    private var savedTraderDefaultsCache: TraderDefaults = TraderDefaults()

    init {
        launchIO {
            configService.configuration().collectLatest { snapshot ->
                // Cache the saved settings for the exchange in the snapshot
                // IMPORTANT: Only cache if the snapshot has actual saved values (not empty)
                // This prevents overwriting our cache with empty values when switching exchanges
                if (snapshot.exchange.exchange != null && 
                    (snapshot.exchange.apiKey.isNotEmpty() || snapshot.exchange.secretKey.isNotEmpty())) {
                    // Always cache if we have values - this ensures we capture all saved exchanges
                    savedSettingsCache[snapshot.exchange.exchange] = snapshot.exchange
                    println("🔍 ViewModel: Cached saved settings for exchange: ${snapshot.exchange.exchange} (apiKey length=${snapshot.exchange.apiKey.length}, secretKey length=${snapshot.exchange.secretKey.length})")
                } else if (snapshot.exchange.exchange != null) {
                    println("🔍 ViewModel: Snapshot has exchange ${snapshot.exchange.exchange} but no saved values, not caching")
                }
                
                // Always try to populate cache from all exchanges in service
                // This ensures we capture all saved exchanges, even if they're not in the current snapshot
                // The service cache is populated when configuration is loaded from file
                println("🔍 ViewModel: Checking service cache for all exchanges...")
                for (exchange in Exchange.values()) {
                    val serviceCached = configService.getExchangeSettings(exchange)
                    if (serviceCached != null && (serviceCached.apiKey.isNotEmpty() || serviceCached.secretKey.isNotEmpty())) {
                        // Only update if we don't have it, or if the service has newer data
                        if (!savedSettingsCache.containsKey(exchange)) {
                            savedSettingsCache[exchange] = serviceCached
                            println("🔍 ViewModel: Loaded ${exchange} from service cache (apiKey length=${serviceCached.apiKey.length}, secretKey length=${serviceCached.secretKey.length})")
                        } else {
                            // Update if service has data and we don't, or if service data is different
                            val cached = savedSettingsCache[exchange]!!
                            if (cached.apiKey != serviceCached.apiKey || cached.secretKey != serviceCached.secretKey) {
                                savedSettingsCache[exchange] = serviceCached
                                println("🔍 ViewModel: Updated ${exchange} from service cache (apiKey length=${serviceCached.apiKey.length}, secretKey length=${serviceCached.secretKey.length})")
                            }
                        }
                    } else {
                        // Service doesn't have this exchange, but we might have it cached from a previous save
                        // Don't remove it from cache, just log
                        if (savedSettingsCache.containsKey(exchange)) {
                            println("🔍 ViewModel: ${exchange} not in service cache, but we have it cached locally")
                        }
                    }
                }
                println("🔍 ViewModel: Cache check complete. Cached exchanges: ${savedSettingsCache.keys}")
                
                // Load timestamps from service for all exchanges
                val loadedTimestamps = mutableMapOf<Exchange, Long>()
                for (exchange in Exchange.values()) {
                    val timestamp = configService.getExchangeTimestamp(exchange)
                    if (timestamp != null) {
                        loadedTimestamps[exchange] = timestamp
                        println("🔍 ViewModel: Loaded timestamp for $exchange: $timestamp")
                    }
                }
                
                // Cache saved general settings
                savedGeneralSettingsCache = snapshot.general
                
                // Cache saved trader defaults
                savedTraderDefaultsCache = snapshot.traderDefaults
                
                // Ensure trader defaults form has a valid strategy
                val traderDefaultsForm = snapshot.traderDefaults.toForm()
                val validatedTraderDefaultsForm = if (traderDefaultsForm.strategy.isBlank() || 
                    try { TradingStrategy.valueOf(traderDefaultsForm.strategy); false } catch (e: IllegalArgumentException) { true }) {
                    // Invalid or empty strategy, use default
                    traderDefaultsForm.copy(strategy = TradingStrategy.TREND_FOLLOWING.name)
                } else {
                    traderDefaultsForm
                }
                
                setState {
                    it.copy(
                        isLoading = false,
                        exchangeForm = snapshot.exchange.toForm(),
                        generalForm = snapshot.general.toForm(),
                        traderDefaultsForm = validatedTraderDefaultsForm,
                        lastSavedTimestamp = loadedTimestamps,
                        hasUnsavedGeneralChanges = checkHasUnsavedGeneralChanges(snapshot.general.toForm()),
                        validationErrors = it.validationErrors.filterKeys { it !in listOf("strategy") } // Clear strategy validation error if we fixed it
                    )
                }
                
                // Apply theme when configuration is loaded (ensures theme is applied on startup)
                applyThemePreference(snapshot.general.theme)
            }
        }
        
        // Monitor Core Service connection status
        launchIO {
            connectionStatusService.status.collectLatest { status ->
                setState { it.copy(coreServiceStatus = status) }
            }
        }
    }

    fun updateExchangeForm(updater: (ExchangeForm) -> ExchangeForm) {
        setState { state ->
            val updatedForm = updater(state.exchangeForm)
            val hasUnsavedChanges = checkHasUnsavedChanges(updatedForm)
            state.copy(
                exchangeForm = updatedForm,
                hasUnsavedExchangeChanges = hasUnsavedChanges
            )
        }
    }
    
    /**
     * Checks if the current exchange form has unsaved changes compared to saved values.
     * Returns true if any field differs from the saved values for the selected exchange.
     */
    private fun checkHasUnsavedChanges(form: ExchangeForm): Boolean {
        if (form.exchange == null) {
            return false // No exchange selected, no changes
        }
        
        val savedSettings = getSavedExchangeSettings(form.exchange)
        
        // If no saved settings exist, check if form has any values
        if (savedSettings == null) {
            // Consider it "changed" if user has entered any values
            return form.apiKey.isNotEmpty() || form.secretKey.isNotEmpty() || 
                   (form.exchange == Exchange.BITGET && form.passphrase.isNotEmpty())
        }
        
        // Compare current form values with saved values
        val apiKeyChanged = form.apiKey != savedSettings.apiKey
        val secretKeyChanged = form.secretKey != savedSettings.secretKey
        val passphraseChanged = if (form.exchange == Exchange.BITGET) {
            form.passphrase != savedSettings.passphrase
        } else {
            false
        }
        
        return apiKeyChanged || secretKeyChanged || passphraseChanged
    }

    fun updateGeneralForm(updater: (GeneralForm) -> GeneralForm) {
        setState { state ->
            val updatedForm = updater(state.generalForm)
            
            // Validate immediately when values change
            val validationErrors = validateGeneral(updatedForm)
            val hasUnsavedChanges = checkHasUnsavedGeneralChanges(updatedForm)
            
            // Clear old general validation errors and set new ones
            val updatedValidationErrors = state.validationErrors
                .filterKeys { it !in listOf("updateInterval", "telemetryPolling") } // Remove old general errors
                .plus(validationErrors) // Add new validation errors
            
            val newState = state.copy(
                generalForm = updatedForm,
                hasUnsavedGeneralChanges = hasUnsavedChanges,
                validationErrors = updatedValidationErrors
            )
            
            // Only auto-save if there are changes AND validation passes
            if (hasUnsavedChanges && validationErrors.isEmpty()) {
                launchIO {
                    try {
                        val settings = updatedForm.toSettings()
                        configService.saveGeneralSettings(settings)
                        savedGeneralSettingsCache = settings
                        
                        // Apply theme immediately if it changed
                        withContext(Dispatchers.Main) {
                            applyThemePreference(settings.theme)
                            setState { it.copy(hasUnsavedGeneralChanges = false) }
                        }
                        logger.info { "✅ General settings auto-saved" }
                    } catch (ex: Exception) {
                        logger.error(ex) { "❌ Failed to auto-save general settings: ${ex.message}" }
                        // Show error to user
                        withContext(Dispatchers.Main) {
                            publishEvent(ConfigurationEvent.ShowMessage(
                                "Failed to save general settings: ${ex.message}",
                                MessageLevel.ERROR
                            ))
                        }
                    }
                }
            } else if (validationErrors.isNotEmpty()) {
                // Validation failed - don't save, but errors are already in state
                logger.warn { "⚠️ General form validation failed: $validationErrors" }
            }
            
            newState
        }
    }
    
    /**
     * Checks if the current general form has unsaved changes compared to saved values.
     * Returns true if any field differs from the saved general settings.
     */
    private fun checkHasUnsavedGeneralChanges(form: GeneralForm): Boolean {
        val savedSettings = savedGeneralSettingsCache
        
        // Compare current form values with saved values
        val updateIntervalChanged = form.updateIntervalSeconds != savedSettings.updateIntervalSeconds
        val telemetryPollingChanged = form.telemetryPollingSeconds != savedSettings.telemetryPollingSeconds
        val loggingLevelChanged = form.loggingLevel != savedSettings.loggingLevel
        val themeChanged = form.theme != savedSettings.theme
        
        return updateIntervalChanged || telemetryPollingChanged || loggingLevelChanged || themeChanged
    }

    fun updateTraderDefaults(updater: (TraderDefaultsForm) -> TraderDefaultsForm) {
        setState { state ->
            val updatedForm = updater(state.traderDefaultsForm)
            
            // Validate immediately when values change
            val validationErrors = validateTraderDefaults(updatedForm)
            
            // Clear old trader defaults validation errors and set new ones
            val updatedValidationErrors = state.validationErrors
                .filterKeys { it !in listOf("budget", "leverage", "stopLoss", "strategy") } // Remove old trader errors
                .plus(validationErrors) // Add new validation errors
            
            val newState = state.copy(
                traderDefaultsForm = updatedForm,
                validationErrors = updatedValidationErrors
            )
            
            // Only auto-save if validation passes
            if (validationErrors.isEmpty()) {
                // Check if values actually changed to avoid unnecessary saves
                val hasChanges = updatedForm.budgetUsd != savedTraderDefaultsCache.budgetUsd ||
                                 updatedForm.leverage != savedTraderDefaultsCache.leverage ||
                                 updatedForm.stopLossPercent != savedTraderDefaultsCache.stopLossPercent ||
                                 updatedForm.takeProfitPercent != savedTraderDefaultsCache.takeProfitPercent ||
                                 updatedForm.strategy != savedTraderDefaultsCache.strategy
                
                if (hasChanges) {
                    // Auto-save trader defaults
                    println("🔍 updateTraderDefaults: Validation passed, values changed, triggering auto-save")
                    logger.info { "🔍 updateTraderDefaults: Validation passed, values changed, triggering auto-save: budget=${updatedForm.budgetUsd}, leverage=${updatedForm.leverage}, stopLoss=${updatedForm.stopLossPercent}, takeProfit=${updatedForm.takeProfitPercent}, strategy=${updatedForm.strategy}" }
                    launchIO {
                        try {
                            val defaults = updatedForm.toDefaults()
                            println("🔄 updateTraderDefaults: Calling configService.saveTraderDefaults()...")
                            configService.saveTraderDefaults(defaults)
                            println("✅ updateTraderDefaults: saveTraderDefaults() completed successfully")
                            
                            // Cache the saved defaults
                            savedTraderDefaultsCache = defaults
                            
                            logger.info { "✅ Trader defaults auto-saved: budget=${defaults.budgetUsd}, leverage=${defaults.leverage}, stopLoss=${defaults.stopLossPercent}, takeProfit=${defaults.takeProfitPercent}, strategy=${defaults.strategy}" }
                            println("✅ Trader defaults auto-saved: budget=${defaults.budgetUsd}, leverage=${defaults.leverage}, stopLoss=${defaults.stopLossPercent}, takeProfit=${defaults.takeProfitPercent}, strategy=${defaults.strategy}")
                        } catch (ex: Exception) {
                            logger.error(ex) { "❌ Failed to auto-save trader defaults: ${ex.message}" }
                            println("❌ Failed to auto-save trader defaults: ${ex.message}")
                            ex.printStackTrace()
                            // Show error to user
                            withContext(Dispatchers.Main) {
                                publishEvent(ConfigurationEvent.ShowMessage(
                                    "Failed to save trader defaults: ${ex.message}",
                                    MessageLevel.ERROR
                                ))
                            }
                        }
                    }
                } else {
                    println("🔍 updateTraderDefaults: Validation passed, but no changes detected (values match cache)")
                    logger.debug { "🔍 updateTraderDefaults: Validation passed, but no changes detected" }
                }
            } else {
                // Validation failed - don't save, but errors are already in state
                logger.warn { "⚠️ Trader defaults validation failed: $validationErrors" }
                println("⚠️ Trader defaults validation failed: $validationErrors")
            }
            
            newState
        }
    }
    
    /**
     * Gets saved exchange settings for a specific exchange from the ViewModel's cache.
     * If not found in cache, checks the service cache directly.
     * The cache is updated whenever settings are saved or loaded from the service.
     */
    fun getSavedExchangeSettings(exchange: Exchange): ExchangeSettings? {
        // First check ViewModel cache
        var cached = savedSettingsCache[exchange]
        if (cached != null) {
            println("🔍 getSavedExchangeSettings($exchange): found in ViewModel cache (apiKey length=${cached.apiKey.length}, secretKey length=${cached.secretKey.length})")
            return cached
        }
        
        // If not in ViewModel cache, check service cache directly
        // This handles the case where the service has loaded the config but the ViewModel hasn't cached it yet
        cached = configService.getExchangeSettings(exchange)
        if (cached != null && (cached.apiKey.isNotEmpty() || cached.secretKey.isNotEmpty())) {
            // Update ViewModel cache for future use
            savedSettingsCache[exchange] = cached
            println("🔍 getSavedExchangeSettings($exchange): found in service cache, updating ViewModel cache (apiKey length=${cached.apiKey.length}, secretKey length=${cached.secretKey.length})")
            return cached
        }
        
        println("🔍 getSavedExchangeSettings($exchange): not found in either cache")
        return null
    }

    fun saveExchangeSettings() {
        // Use both println (for immediate visibility) and logger (for proper logging)
        println("🔍 saveExchangeSettings() called")
        logger.info { "🔍 saveExchangeSettings() called" }
        val form = state.value.exchangeForm
        println("🔍 Exchange form: exchange=${form.exchange}, apiKey length=${form.apiKey.length}, secretKey length=${form.secretKey.length}")
        logger.info { "🔍 Exchange form: exchange=${form.exchange}, apiKey length=${form.apiKey.length}, secretKey length=${form.secretKey.length}" }
        val errors = validateExchange(form)
        logger.info { "🔍 Validation errors: $errors" }
        if (errors.isNotEmpty()) {
            logger.warn { "⚠️ Validation failed, setting errors in state" }
            setState { it.copy(validationErrors = errors) }
            return
        }
        logger.info { "✅ Validation passed, proceeding with save" }
        setState { it.copy(validationErrors = emptyMap(), isSaving = true) }
        launchIO {
            try {
                logger.info { "🔄 Calling configService.saveExchangeSettings()..." }
                val settings = form.toSettings()
                configService.saveExchangeSettings(settings)
                logger.info { "✅ saveExchangeSettings() completed successfully" }
                // Cache the saved settings for this exchange BEFORE updating state
                savedSettingsCache[form.exchange] = settings
                logger.info { "🔍 Cached saved settings for exchange: ${form.exchange} (apiKey length=${settings.apiKey.length}, secretKey length=${settings.secretKey.length})" }
                
                // Update state with saved timestamp and clear isSaving flag
                // Also save timestamp to service for persistence
                // StateFlow.update is thread-safe, but we need to ensure it emits immediately
                // Use withContext to switch to main thread for state update to ensure immediate emission
                val currentTimestamp = System.currentTimeMillis()
                
                // Save timestamp to service cache for persistence
                // Note: This will be saved to file when saveConfigurationToFile() is called
                // We need to update the service's timestamp cache
                // Since we can't directly access the service's private cache, we'll rely on the file save
                // The timestamp will be saved when the file is written
                
                withContext(Dispatchers.Main) {
                    val oldState = state.value
                    println("🔄 Before state update: lastSavedTimestamp = ${oldState.lastSavedTimestamp}")
                    setState { 
                        val newState = it.copy(
                            isSaving = false,
                            lastSavedTimestamp = it.lastSavedTimestamp + (form.exchange to currentTimestamp),
                            hasUnsavedExchangeChanges = false // Reset dirty flag after successful save
                        )
                        println("🔄 Inside setState reducer: new lastSavedTimestamp = ${newState.lastSavedTimestamp}")
                        newState
                    }
                    // Read the updated state to verify it was set
                    val updatedState = state.value
                    println("🔄 After state update: isSaving = ${updatedState.isSaving}, lastSavedTimestamp = ${updatedState.lastSavedTimestamp}, timestamp for ${form.exchange} = ${updatedState.lastSavedTimestamp[form.exchange]}")
                    logger.info { "🔄 State updated: isSaving = false, lastSavedTimestamp updated for ${form.exchange} = $currentTimestamp (map now has: ${updatedState.lastSavedTimestamp.keys}, value: ${updatedState.lastSavedTimestamp[form.exchange]})" }
                }
                
                // Save timestamp to service for persistence
                configService.saveExchangeTimestamp(form.exchange, currentTimestamp)
                
                // No need to show success dialog - status is already displayed in the UI via "Saved on" timestamp
                logger.info { "✅ Exchange settings saved successfully (status shown in UI)" }
            } catch (ex: Exception) {
                logger.error(ex) { "❌ Exception in saveExchangeSettings: ${ex.javaClass.simpleName} - ${ex.message}" }
                
                // Publish event first
                publishEvent(ConfigurationEvent.ShowMessage(ex.message ?: "Unable to save exchange settings", MessageLevel.ERROR))
                logger.info { "✅ Error event published" }
                
                // Small delay to ensure event is collected before state update
                delay(100)
                
                // Update state to clear isSaving flag
                setState { it.copy(isSaving = false) }
                println("🔄 State updated: isSaving = false (error case)")
            }
        }
    }

    fun saveGeneralSettings() {
        println("🔍 saveGeneralSettings() called")
        val form = state.value.generalForm
        println("🔍 General form: updateInterval=${form.updateIntervalSeconds}, telemetryPolling=${form.telemetryPollingSeconds}, loggingLevel=${form.loggingLevel}, theme=${form.theme}")
        val errors = validateGeneral(form)
        println("🔍 Validation errors: $errors")
        if (errors.isNotEmpty()) {
            println("⚠️ Validation failed, setting errors in state")
            setState { it.copy(validationErrors = errors) }
            return
        }
        println("✅ Validation passed, proceeding with save")
        setState { it.copy(validationErrors = emptyMap(), isSaving = true) }
        launchIO {
            try {
                println("🔄 Calling configService.saveGeneralSettings()...")
                val settings = form.toSettings()
                configService.saveGeneralSettings(settings)
                println("✅ saveGeneralSettings() completed successfully")
                
                // Cache the saved settings
                savedGeneralSettingsCache = settings
                
                // Apply theme preference
                withContext(Dispatchers.Main) {
                    applyThemePreference(settings.theme)
                    
                    setState { 
                        it.copy(
                            isSaving = false,
                            hasUnsavedGeneralChanges = false // Reset dirty flag after successful save
                        )
                    }
                }
                
                // No need to show success dialog - status is already displayed in the UI
                println("✅ General settings saved successfully (status shown in UI)")
            } catch (ex: Exception) {
                println("❌ Exception in saveGeneralSettings: ${ex.javaClass.simpleName} - ${ex.message}")
                ex.printStackTrace()
                publishEvent(ConfigurationEvent.ShowMessage(ex.message ?: "Unable to save general settings", MessageLevel.ERROR))
                println("✅ Error event published")
                setState { it.copy(isSaving = false) }
            }
        }
    }

    fun saveTraderDefaults() {
        println("🔍 saveTraderDefaults() called")
        val form = state.value.traderDefaultsForm
        println("🔍 Trader defaults form: budget=${form.budgetUsd}, leverage=${form.leverage}, stopLoss=${form.stopLossPercent}, strategy='${form.strategy}'")
        val errors = validateTraderDefaults(form)
        println("🔍 Validation errors: $errors")
        if (errors.isNotEmpty()) {
            println("⚠️ Validation failed, setting errors in state")
            setState { it.copy(validationErrors = errors) }
            return
        }
        println("✅ Validation passed, proceeding with save")
        setState { it.copy(validationErrors = emptyMap(), isSaving = true) }
        launchIO {
            try {
                println("🔄 Calling configService.saveTraderDefaults()...")
                configService.saveTraderDefaults(form.toDefaults())
                println("✅ saveTraderDefaults() completed successfully")
                publishEvent(ConfigurationEvent.ShowMessage("Trader defaults saved", MessageLevel.SUCCESS))
                println("✅ Success event published")
            } catch (ex: Exception) {
                println("❌ Exception in saveTraderDefaults: ${ex.javaClass.simpleName} - ${ex.message}")
                ex.printStackTrace()
                publishEvent(ConfigurationEvent.ShowMessage(ex.message ?: "Unable to save trader defaults", MessageLevel.ERROR))
                println("✅ Error event published")
            } finally {
                setState { it.copy(isSaving = false) }
                println("🔄 isSaving set to false")
            }
        }
    }

    fun testConnection() {
        logger.info { "🔍 testConnection() called" }
        val form = state.value.exchangeForm
        logger.info { "🔍 Exchange form: exchange=${form.exchange}, apiKey length=${form.apiKey.length}, secretKey length=${form.secretKey.length}" }
        // Update state immediately on main thread to show "Testing..." status
        // Use direct value assignment to ensure immediate emission
        launch(Dispatchers.Main) {
            val currentState = state.value
            val newState = currentState.copy(isTestingConnection = true, connectionTest = null)
            setState { newState }
            println("🔄 testConnection: Set isTestingConnection = true, state.value.isTestingConnection = ${state.value.isTestingConnection}")
            logger.info { "🔄 State updated: isTestingConnection = true for ${state.value.exchangeForm.exchange}" }
        }
        launchIO {
            try {
                logger.info { "🔄 Calling configService.testExchangeConnection()..." }
                val result = configService.testExchangeConnection(form.toSettings())
                logger.info { "✅ testExchangeConnection() completed: success=${result.success}, message=${result.message}" }
                
                // Update shared exchange connection status service
                logger.info { "🔍 Updating exchange connection status: exchange=${form.exchange}, success=${result.success}" }
                when (form.exchange) {
                    Exchange.BINANCE -> {
                        logger.info { "🔄 Updating Binance status to ${result.success}" }
                        exchangeConnectionStatusService.updateBinanceStatus(result.success)
                    }
                    Exchange.BITGET -> {
                        logger.info { "🔄 Updating Bitget status to ${result.success}" }
                        exchangeConnectionStatusService.updateBitgetStatus(result.success)
                    }
                    else -> {
                        logger.warn { "⚠️ Unknown exchange: ${form.exchange}, not updating status" }
                    }
                }
                
                // Update state with connection test result and exchange connection status
                // Use withContext to ensure state update happens on main thread
                withContext(Dispatchers.Main) {
                    val oldState = state.value
                    println("🔄 Before connection test state update: exchangeConnectionStatus = ${oldState.exchangeConnectionStatus}, isTestingConnection = ${oldState.isTestingConnection}")
                    setState { 
                        val newState = it.copy(
                            connectionTest = result, 
                            isTestingConnection = false,
                            exchangeConnectionStatus = it.exchangeConnectionStatus + (form.exchange to result.success)
                        )
                        println("🔄 Inside setState reducer: new exchangeConnectionStatus = ${newState.exchangeConnectionStatus}, isTestingConnection = ${newState.isTestingConnection}")
                        newState
                    }
                    val updatedState = state.value
                    println("🔄 After connection test state update: exchangeConnectionStatus = ${updatedState.exchangeConnectionStatus}, isTestingConnection = ${updatedState.isTestingConnection}, status for ${form.exchange} = ${updatedState.exchangeConnectionStatus[form.exchange]}")
                    logger.info { "🔄 State updated: connectionTest set, isTestingConnection = false, exchangeConnectionStatus updated for ${form.exchange} = ${result.success}" }
                }
                
                // Only show error dialog if connection test failed
                // Success status is already displayed in the UI via "Connection Status" label
                if (!result.success) {
                    publishEvent(
                        ConfigurationEvent.ShowMessage(
                            result.message,
                            MessageLevel.ERROR
                        )
                    )
                    logger.info { "Connection test failed - error event published" }
                } else {
                    logger.info { "Connection test succeeded (status shown in UI)" }
                }
            } catch (ex: Exception) {
                logger.error(ex) { "❌ Exception in testConnection: ${ex.javaClass.simpleName} - ${ex.message}" }
                setState { it.copy(isTestingConnection = false) }
                logger.info { "🔄 State updated: isTestingConnection = false (error case)" }
                publishEvent(ConfigurationEvent.ShowMessage(ex.message ?: "Connection test failed", MessageLevel.ERROR))
                logger.info { "✅ Error event published" }
            }
        }
    }

    fun exportConfiguration() {
        println("🔍 exportConfiguration() called")
        setState { it.copy(isSaving = true) }
        launchIO {
            try {
                println("🔄 Calling configService.exportConfiguration()...")
                val content = configService.exportConfiguration()
                println("✅ exportConfiguration() completed, content length: ${content.length}")
                setState { it.copy(exportContent = content, isSaving = false) }
                println("✅ Export content set in state")
            } catch (ex: Exception) {
                println("❌ Exception in exportConfiguration: ${ex.javaClass.simpleName} - ${ex.message}")
                ex.printStackTrace()
                publishEvent(ConfigurationEvent.ShowMessage(ex.message ?: "Export failed", MessageLevel.ERROR))
                println("✅ Error event published")
                setState { it.copy(isSaving = false) }
            }
        }
    }

    fun importConfiguration(serialized: String) {
        println("🔍 importConfiguration() called, text length: ${serialized.length}")
        if (serialized.isBlank()) {
            println("⚠️ Import text is blank, setting error")
            setState { it.copy(importError = "Import file is empty") }
            return
        }
        println("✅ Import text is not blank, proceeding with import")
        setState { it.copy(isSaving = true, importError = null, importSuccess = false) }
        launchIO {
            try {
                println("🔄 Calling configService.importConfiguration()...")
                val snapshot = configService.importConfiguration(serialized)
                println("✅ importConfiguration() completed successfully")
                setState {
                    it.copy(
                        exchangeForm = snapshot.exchange.toForm(),
                        generalForm = snapshot.general.toForm(),
                        traderDefaultsForm = snapshot.traderDefaults.toForm(),
                        importSuccess = true,
                        isSaving = false
                    )
                }
                println("✅ Imported configuration set in state")
                publishEvent(ConfigurationEvent.ShowMessage("Configuration imported", MessageLevel.SUCCESS))
                println("✅ Success event published")
            } catch (ex: Exception) {
                println("❌ Exception in importConfiguration: ${ex.javaClass.simpleName} - ${ex.message}")
                ex.printStackTrace()
                setState { it.copy(importError = ex.message ?: "Import failed", isSaving = false) }
                publishEvent(ConfigurationEvent.ShowMessage("Import failed", MessageLevel.ERROR))
                println("✅ Error event published")
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
        if (form.updateIntervalSeconds !in 1..600) errors["updateInterval"] = "Update Interval must be between 1-600 seconds"
        if (form.telemetryPollingSeconds !in 1..120) errors["telemetryPolling"] = "Telemetry Polling must be between 1-120 seconds"
        return errors
    }

    private fun validateTraderDefaults(form: TraderDefaultsForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (form.budgetUsd <= 0) errors["budget"] = "Budget must be positive"
        if (form.leverage !in 1..10) errors["leverage"] = "Leverage must be between 1 and 10"
        if (form.stopLossPercent !in 0.5..50.0) errors["stopLoss"] = "Stop loss range 0.5% - 50%"
        if (form.takeProfitPercent !in 0.5..50.0) errors["takeProfit"] = "Take profit range 0.5% - 50%"
        // Validate strategy is a valid TradingStrategy enum value
        try {
            TradingStrategy.valueOf(form.strategy)
        } catch (e: IllegalArgumentException) {
            errors["strategy"] = "Invalid strategy. Must be one of: ${TradingStrategy.values().joinToString(", ") { it.name }}"
        }
        return errors
    }

    private fun ExchangeSettings.toForm() = ExchangeForm(exchange = exchange, apiKey = apiKey, secretKey = secretKey, passphrase = passphrase)
    private fun GeneralSettings.toForm() = GeneralForm(updateIntervalSeconds, telemetryPollingSeconds, loggingLevel, theme)
    private fun TraderDefaults.toForm() = TraderDefaultsForm(budgetUsd, leverage, stopLossPercent, takeProfitPercent, strategy)
    
    /**
     * Applies the theme preference to the JavaFX application.
     * This should be called on the JavaFX Application Thread.
     */
    /**
     * Detects Windows system dark mode by reading the Windows registry.
     * Returns true if dark mode is enabled, false otherwise.
     */
    private fun detectWindowsDarkMode(): Boolean {
        return try {
            // Try JavaFX Platform.isDarkMode() first (JavaFX 17+)
            try {
                val darkModeMethod = Platform::class.java.getMethod("isDarkMode")
                val result = darkModeMethod.invoke(null) as? Boolean
                if (result != null) {
                    logger.info { "🔍 Using Platform.isDarkMode(): $result" }
                    return result
                }
            } catch (e: NoSuchMethodException) {
                // Method not available, fall back to registry
            }
            
            // Fallback: Read Windows registry
            val process = ProcessBuilder(
                "reg", "query", 
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                "/v", "AppsUseLightTheme"
            ).start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                // If AppsUseLightTheme = 0, dark mode is enabled
                // If AppsUseLightTheme = 1, light mode is enabled
                val isLight = output.contains("AppsUseLightTheme") && output.contains("0x1")
                val isDark = !isLight
                logger.info { "🔍 Using Windows registry: isDark=$isDark (AppsUseLightTheme=${if (isLight) "1" else "0"})" }
                isDark
            } else {
                // Fallback: assume dark mode if we can't detect
                logger.warn { "⚠️ Could not read Windows registry, defaulting to dark mode" }
                true
            }
        } catch (e: Exception) {
            logger.warn(e) { "⚠️ Error detecting Windows dark mode: ${e.message}, defaulting to dark" }
            true // Default to dark mode
        }
    }
    
    private fun applyThemePreference(theme: ThemePreference) {
        try {
            val actualTheme = when (theme) {
                ThemePreference.AUTO -> {
                    // Detect system theme preference using Windows registry
                    val isDark = detectWindowsDarkMode()
                    if (isDark) ThemePreference.DARK else ThemePreference.LIGHT
                }
                else -> theme
            }
            
            // Apply theme to all scenes and their roots
            Platform.runLater {
                try {
                    // Apply to primary stage scene
                    FX.primaryStage?.scene?.root?.let { root ->
                        root.styleClass.removeAll(listOf("theme-light", "theme-dark"))
                        when (actualTheme) {
                            ThemePreference.LIGHT -> root.styleClass.add("theme-light")
                            ThemePreference.DARK -> root.styleClass.add("theme-dark")
                            else -> {} // AUTO already resolved
                        }
                        logger.info { "✅ Theme applied to primary stage root: $actualTheme" }
                        
                        // Also apply theme class to sidebar and nav-buttons if they exist
                        // Find sidebar by traversing the scene graph
                        if (root is javafx.scene.Parent) {
                            root.lookupAll(".sidebar").forEach { sidebar ->
                                sidebar.styleClass.removeAll(listOf("theme-light", "theme-dark"))
                                when (actualTheme) {
                                    ThemePreference.LIGHT -> sidebar.styleClass.add("theme-light")
                                    ThemePreference.DARK -> sidebar.styleClass.add("theme-dark")
                                    else -> {}
                                }
                                logger.info { "✅ Applied theme class to sidebar: $actualTheme" }
                            }
                            
                            // Also apply to nav-buttons
                            root.lookupAll(".nav-button").forEach { navButton ->
                                navButton.styleClass.removeAll(listOf("theme-light", "theme-dark"))
                                when (actualTheme) {
                                    ThemePreference.LIGHT -> navButton.styleClass.add("theme-light")
                                    ThemePreference.DARK -> navButton.styleClass.add("theme-dark")
                                    else -> {}
                                }
                            }
                        }
                    }
                    
                    // Also apply to all windows/scenes
                    Window.getWindows().forEach { window ->
                        if (window is Stage) {
                            window.scene?.root?.let { root ->
                                root.styleClass.removeAll(listOf("theme-light", "theme-dark"))
                                when (actualTheme) {
                                    ThemePreference.LIGHT -> root.styleClass.add("theme-light")
                                    ThemePreference.DARK -> root.styleClass.add("theme-dark")
                                    else -> {}
                                }
                                
                                // Also apply theme to sidebar in this window
                                if (root is javafx.scene.Parent) {
                                    root.lookupAll(".sidebar").forEach { sidebar ->
                                        sidebar.styleClass.removeAll(listOf("theme-light", "theme-dark"))
                                        when (actualTheme) {
                                            ThemePreference.LIGHT -> sidebar.styleClass.add("theme-light")
                                            ThemePreference.DARK -> sidebar.styleClass.add("theme-dark")
                                            else -> {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                    logger.info { "✅ Theme applied: $actualTheme" }
                } catch (e: Exception) {
                    logger.error(e) { "❌ Error applying theme: ${e.message}" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "❌ Failed to apply theme preference: ${e.message}" }
        }
    }
}

