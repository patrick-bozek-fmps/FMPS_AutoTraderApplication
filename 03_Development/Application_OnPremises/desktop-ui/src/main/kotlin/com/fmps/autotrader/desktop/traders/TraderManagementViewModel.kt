package com.fmps.autotrader.desktop.traders

import com.fmps.autotrader.desktop.mvvm.BaseViewModel
import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.services.TelemetryClient
import com.fmps.autotrader.desktop.services.TelemetrySample
import com.fmps.autotrader.desktop.services.TraderDetail
import com.fmps.autotrader.desktop.services.TraderRiskLevel
import com.fmps.autotrader.desktop.services.TraderService
import com.fmps.autotrader.desktop.services.TraderStatus
import com.fmps.autotrader.desktop.services.ConfigService
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.network.sockets.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.pow
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class TraderManagementViewModel(
    dispatcherProvider: DispatcherProvider,
    private val traderService: TraderService,
    private val telemetryClient: TelemetryClient,
    private val configService: ConfigService
) : BaseViewModel<TraderManagementState, TraderManagementEvent>(
    TraderManagementState(),
    dispatcherProvider
) {

    private val json = Json { ignoreUnknownKeys = true }
    
    // Cache trader defaults for use when creating traders
    var cachedTraderDefaults: com.fmps.autotrader.desktop.services.TraderDefaults? = null
        private set

    init {
        // Load trader defaults from configuration
        launchIO {
            configService.configuration().collectLatest { snapshot ->
                cachedTraderDefaults = snapshot.traderDefaults
                logger.info { "🔍 Trader defaults loaded: budget=${snapshot.traderDefaults.budgetUsd}, leverage=${snapshot.traderDefaults.leverage}, stopLoss=${snapshot.traderDefaults.stopLossPercent}, strategy=${snapshot.traderDefaults.strategy}" }
            }
        }
        // Start telemetry client for real-time updates
        telemetryClient.start()
        
        // Observe trader list from service (REST polling)
        launchIO {
            traderService.traders().collectLatest { traders ->
                println("🔍 Traders flow updated: ${traders.size} traders received")
                setState { state ->
                    // If selectedTraderId is set but trader not found yet, keep it (might be loading)
                    // Otherwise, sanitize to only valid trader IDs
                    val sanitizedSelection = if (state.selectedTraderId != null && traders.any { it.id == state.selectedTraderId }) {
                        state.selectedTraderId
                    } else if (state.selectedTraderId != null && traders.isEmpty()) {
                        // Keep selection if list is empty (might be loading)
                        state.selectedTraderId
                    } else {
                        state.selectedTraderId?.takeIf { id -> traders.any { it.id == id } }
                    }
                    // Only update form if we have a selected trader, otherwise preserve the current form
                    // This allows newTrader() to set the form without it being cleared
                    val updatedForm = when {
                        sanitizedSelection != null -> {
                            println("🔍 Updating form from selected trader: ${sanitizedSelection}")
                            val trader = traders.first { it.id == sanitizedSelection }
                            // Always update form from trader when selection changes (user explicitly selected a trader)
                            // This ensures saved values are loaded correctly
                            // Note: Strategy is preserved from current form since API doesn't support it
                            val formFromTrader = TraderForm.fromTrader(trader)
                            // Preserve strategy from current form if it's valid, otherwise use default
                            val preservedStrategy = if (state.form.strategy.isNotBlank() && 
                                try { 
                                    com.fmps.autotrader.shared.model.TradingStrategy.valueOf(state.form.strategy)
                                    true 
                                } catch (e: Exception) { false }) {
                                state.form.strategy
                            } else {
                                formFromTrader.strategy // Use default from trader
                            }
                            formFromTrader.copy(strategy = preservedStrategy)
                        }
                        state.form.isNew && state.form.name.isBlank() -> {
                            // If form is new and empty (from newTrader()), preserve it
                            println("🔍 Preserving new trader form (isNew=true, name is blank)")
                            state.form
                        }
                        sanitizedSelection == null && state.form.isNew.not() && state.form.id != null -> {
                            // Only clear form if we had a selection that no longer exists and form is not new
                            println("🔍 Clearing form (selected trader no longer exists)")
                            TraderForm()
                        }
                        else -> {
                            println("🔍 Keeping existing form (isNew=${state.form.isNew}, name='${state.form.name}')")
                            state.form
                        }
                    }
                    val filtered = traders.filter(state.searchQuery, state.statusFilter)
                    println("🔍 Filtered traders: ${filtered.size} (from ${traders.size} total)")
                    state.copy(
                        isLoading = false,
                        traders = traders,
                        filteredTraders = filtered,
                        selectedTraderId = sanitizedSelection,
                        form = updatedForm,
                        hasUnsavedChanges = state.hasUnsavedChanges // Preserve unsaved changes flag
                    )
                }
            }
        }
        
        // Observe telemetry for real-time trader status updates
        observeTelemetry()
    }

    override fun onCleared() {
        telemetryClient.stop()
        super.onCleared()
    }

    /**
     * Observes telemetry samples for trader status updates.
     * Updates trader list in real-time when status changes occur.
     */
    private fun observeTelemetry() {
        launchIO {
            telemetryClient.samples()
                .onEach { sample ->
                    handleTelemetrySample(sample)
                }
                .collectLatest { }
        }
    }

    /**
     * Handles telemetry samples related to trader updates.
     */
    private suspend fun handleTelemetrySample(sample: TelemetrySample) {
        when (sample.channel) {
            "trader.status" -> {
                try {
                    // Parse trader status update from telemetry
                    val traderUpdate = parseTraderStatusUpdate(sample.payload)
                    if (traderUpdate != null) {
                        // Update trader in current state
                        setState { state ->
                            val updatedTraders = state.traders.map { trader ->
                                if (trader.id == traderUpdate.id) {
                                    trader.copy(
                                        status = traderUpdate.status,
                                        profitLoss = traderUpdate.profitLoss ?: trader.profitLoss
                                    )
                                } else {
                                    trader
                                }
                            }
                            val filtered = updatedTraders.filter(state.searchQuery, state.statusFilter)
                            state.copy(
                                traders = updatedTraders,
                                filteredTraders = filtered
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Log but don't fail on telemetry parsing errors
                }
            }
        }
    }

    /**
     * Parses trader status update from telemetry payload.
     */
    private fun parseTraderStatusUpdate(payload: String): TraderStatusUpdate? {
        return try {
            val jsonElement = json.parseToJsonElement(payload)
            // Extract trader ID and status from telemetry message
            // Format may vary, but typically: {"traderId": "123", "status": "RUNNING", "profitLoss": 100.0}
            val traderId = jsonElement.jsonObject["traderId"]?.jsonPrimitive?.content
            val statusStr = jsonElement.jsonObject["status"]?.jsonPrimitive?.content
            val profitLoss = jsonElement.jsonObject["profitLoss"]?.jsonPrimitive?.content?.toDoubleOrNull()
            
            if (traderId != null && statusStr != null) {
                val status = when (statusStr.uppercase()) {
                    "ACTIVE", "RUNNING" -> TraderStatus.RUNNING
                    "PAUSED", "STOPPED", "STOPPING" -> TraderStatus.STOPPED
                    "ERROR" -> TraderStatus.ERROR
                    else -> TraderStatus.STOPPED
                }
                TraderStatusUpdate(traderId, status, profitLoss)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private data class TraderStatusUpdate(
        val id: String,
        val status: TraderStatus,
        val profitLoss: Double? = null
    )

    fun updateSearch(query: String) {
        setState { state ->
            val filtered = state.traders.filter(query, state.statusFilter)
            state.copy(searchQuery = query, filteredTraders = filtered)
        }
    }

    fun updateStatusFilter(filter: TraderStatusFilter) {
        setState { state ->
            val filtered = state.traders.filter(state.searchQuery, filter)
            state.copy(statusFilter = filter, filteredTraders = filtered)
        }
    }

    fun selectTrader(trader: TraderDetail?) {
        setState { state ->
            if (trader == null) {
                state.copy(selectedTraderId = null, form = TraderForm(), hasUnsavedChanges = false)
            } else {
                state.copy(
                    selectedTraderId = trader.id,
                    form = TraderForm.fromTrader(trader),
                    hasUnsavedChanges = false // Reset when selecting a trader
                )
            }
        }
    }

    /**
     * Selects a trader by ID. This is useful when navigating from other views.
     * The trader will be selected once it's available in the traders list.
     */
    fun selectTraderById(traderId: String) {
        logger.info { "🔍 selectTraderById called for trader: $traderId" }
        launchIO {
            // Try multiple times with increasing delays to ensure traders are loaded
            var attempts = 0
            val maxAttempts = 5
            while (attempts < maxAttempts) {
                delay(300L * (attempts + 1)) // 300ms, 600ms, 900ms, 1200ms, 1500ms
                setState { state ->
                    // Find the trader in the current list
                    val trader = state.traders.find { it.id == traderId }
                    if (trader != null) {
                        logger.info { "✅ Trader found and selected: $traderId (${trader.name}), previous selection: ${state.selectedTraderId}" }
                        // Always update selection, even if it's the same trader (ensures UI updates)
                        state.copy(
                            selectedTraderId = trader.id,
                            form = TraderForm.fromTrader(trader),
                            hasUnsavedChanges = false
                        )
                    } else {
                        // If trader not found yet, store the ID to select later
                        // This will be picked up by the traders().collectLatest when the trader appears
                        if (attempts == 0) {
                            logger.info { "⏳ Trader not found yet, storing ID for later selection: $traderId (have ${state.traders.size} traders)" }
                        }
                        // Store the ID even if trader not found yet
                        state.copy(selectedTraderId = traderId)
                    }
                }
                // Check if we successfully selected the trader
                val currentState = state.value
                if (currentState.selectedTraderId == traderId && currentState.traders.any { it.id == traderId }) {
                    logger.info { "✅ Successfully selected trader: $traderId" }
                    break
                }
                attempts++
            }
            if (attempts >= maxAttempts) {
                val finalState = state.value
                logger.warn { "⚠️ Could not select trader after $maxAttempts attempts: $traderId (have ${finalState.traders.size} traders, current selection: ${finalState.selectedTraderId})" }
            }
        }
    }

    fun newTrader() {
        println("🔍 newTrader() called")
        // Check if we can create more traders (max 3)
        if (state.value.traders.size >= 3) {
            setFormError("newTrader", "Maximum of 3 AI traders already created. Please delete a trader first.")
            return
        }
        clearFormError("newTrader")
        // Pre-fill form with trader defaults when creating a new trader
        val defaults = cachedTraderDefaults ?: com.fmps.autotrader.desktop.services.TraderDefaults()
        println("🔍 Using defaults: budget=${defaults.budgetUsd}, leverage=${defaults.leverage}, stopLoss=${defaults.stopLossPercent}%, takeProfit=${defaults.takeProfitPercent}%, strategy=${defaults.strategy}")
        
        val newForm = TraderForm(
            name = "",
            exchange = "Binance",
            strategy = defaults.strategy,
            riskLevel = TraderRiskLevel.BALANCED,
            baseAsset = "BTC",
            quoteAsset = "USDT",
            budget = defaults.budgetUsd,
            leverage = defaults.leverage,
            stopLossPercent = defaults.stopLossPercent,
            takeProfitPercent = defaults.takeProfitPercent
        )
        println("🔍 Created new form: budget=${newForm.budget}, leverage=${newForm.leverage}, stopLoss=${newForm.stopLossPercent}%, takeProfit=${newForm.takeProfitPercent}%")
        
        setState { 
            it.copy(
                selectedTraderId = null, 
                form = newForm,
                hasUnsavedChanges = false
            ) 
        }
        println("✅ State updated with new trader form")
        logger.info { "🔍 New trader form initialized with defaults: budget=${defaults.budgetUsd}, strategy=${defaults.strategy}, leverage=${defaults.leverage}, stopLoss=${defaults.stopLossPercent}%, takeProfit=${defaults.takeProfitPercent}%" }
    }

    fun updateForm(updater: (TraderForm) -> TraderForm) {
        setState { state -> 
            val updatedForm = updater(state.form)
            // Check if there are unsaved changes by comparing with the selected trader
            val hasChanges = if (state.selectedTraderId != null) {
                val selectedTrader = state.traders.firstOrNull { it.id == state.selectedTraderId }
                selectedTrader?.let { trader ->
                    updatedForm.name != trader.name ||
                    updatedForm.budget != trader.budget ||
                    updatedForm.leverage != (trader.leverage ?: 0) ||
                    updatedForm.stopLossPercent != ((trader.stopLossPercentage ?: 0.0) * 100.0) ||
                    updatedForm.takeProfitPercent != ((trader.takeProfitPercentage ?: 0.0) * 100.0) ||
                    updatedForm.exchange != trader.exchange ||
                    updatedForm.baseAsset != trader.baseAsset ||
                    updatedForm.quoteAsset != trader.quoteAsset ||
                    updatedForm.strategy != trader.strategy ||
                    updatedForm.riskLevel != trader.riskLevel
                } ?: true // If trader not found, consider it changed
            } else {
                // For new traders, check if form is not empty
                updatedForm.name.isNotBlank() || updatedForm.budget > 0.0
            }
            state.copy(form = updatedForm, hasUnsavedChanges = hasChanges)
        }
    }

    fun saveTrader() {
        println("🔍 saveTrader() called")
        val currentForm = state.value.form
        println("🔍 Current form: name=${currentForm.name}, baseAsset=${currentForm.baseAsset}, quoteAsset=${currentForm.quoteAsset}, budget=${currentForm.budget}, isNew=${currentForm.isNew}")
        val errors = validateForm(currentForm)
        println("🔍 Validation errors: $errors")
        if (errors.isNotEmpty()) {
            println("⚠️ Validation failed, setting errors in form state")
            setState { it.copy(form = currentForm.copy(errors = errors)) }
            return
        }

        println("✅ Validation passed, proceeding with save")
        setState { it.copy(isSaving = true, form = currentForm.copy(errors = emptyMap())) }
        launchIO {
            try {
                if (currentForm.isNew) {
                    println("🔄 Creating new trader...")
                    // Get trader defaults to use for leverage and stopLoss
                    val defaults = cachedTraderDefaults ?: com.fmps.autotrader.desktop.services.TraderDefaults()
                    println("🔍 Using trader defaults: leverage=${defaults.leverage}, stopLoss=${defaults.stopLossPercent}%")
                    
                    // Create draft with form values (user can override defaults)
                    val draft = currentForm.toDraft()
                    println("🔍 Draft created: leverage=${draft.leverage}, stopLoss=${draft.stopLossPercentage}, takeProfit=${draft.takeProfitPercentage}")
                    
                    val createdTrader = executeWithRetry(
                        operation = { 
                            println("🔄 Calling traderService.createTrader()...")
                            val result = traderService.createTrader(draft)
                            println("🔄 traderService.createTrader() returned: ${result.id}")
                            result
                        },
                        successMessage = "", // No success dialog
                        errorMessage = "Unable to create trader"
                    )
                    println("✅ executeWithRetry completed, trader created: ${createdTrader.id}")
                    // Select the newly created trader and keep form visible
                    setState { 
                        println("🔄 Selecting new trader and updating state...")
                        it.copy(
                            isSaving = false,
                            selectedTraderId = createdTrader.id, // Select the new trader
                            hasUnsavedChanges = false // No unsaved changes after save
                        ) 
                    }
                    println("✅ New trader selected and state updated")
                } else {
                    println("🔄 Updating existing trader...")
                    // Get trader defaults to use for leverage and stopLoss
                    val defaults = cachedTraderDefaults ?: com.fmps.autotrader.desktop.services.TraderDefaults()
                    println("🔍 Using trader defaults for update: leverage=${defaults.leverage}, stopLoss=${defaults.stopLossPercent}%")
                    
                    // Create draft with form values (user can override defaults)
                    val draft = currentForm.toDraft()
                    println("🔍 Draft for update: leverage=${draft.leverage}, stopLoss=${draft.stopLossPercentage}, takeProfit=${draft.takeProfitPercentage}")
                    
                    // Update configuration (leverage, stopLoss, takeProfit)
                    val updatedTrader = executeWithRetry(
                        operation = { traderService.updateTrader(currentForm.id!!, draft) },
                        successMessage = "", // No success dialog
                        errorMessage = "Unable to update trader"
                    )
                    
                    // If budget changed, update it separately via balance endpoint
                    val selectedTrader = state.value.traders.firstOrNull { it.id == currentForm.id }
                    if (selectedTrader != null && currentForm.budget != selectedTrader.budget) {
                        println("🔄 Budget changed from ${selectedTrader.budget} to ${currentForm.budget}, updating balance...")
                        try {
                            traderService.updateTraderBalance(currentForm.id!!, currentForm.budget)
                            println("✅ Balance updated successfully")
                        } catch (e: Exception) {
                            println("❌ Failed to update balance: ${e.message}")
                            throw e // Re-throw to show error
                        }
                    }
                    
                    // Refresh the form from the updated trader to ensure all values are synced
                    // The traders flow will update automatically, but we need to ensure form reflects saved state
                    // Preserve strategy from current form since API doesn't support it
                    val formFromTrader = TraderForm.fromTrader(updatedTrader)
                    val preservedStrategy = if (currentForm.strategy.isNotBlank() && 
                        try { 
                            com.fmps.autotrader.shared.model.TradingStrategy.valueOf(currentForm.strategy)
                            true 
                        } catch (e: Exception) { false }) {
                        currentForm.strategy
                    } else {
                        formFromTrader.strategy
                    }
                    setState { 
                        it.copy(
                            isSaving = false, 
                            hasUnsavedChanges = false,
                            form = formFromTrader.copy(strategy = preservedStrategy) // Reload form from saved trader, preserve strategy
                        ) 
                    }
                }
            } catch (ex: Exception) {
                println("❌ Exception in saveTrader: ${ex.javaClass.simpleName} - ${ex.message}")
                ex.printStackTrace()
                val errorMsg = formatErrorMessage(ex, "Unable to save trader")
                publishEvent(
                    TraderManagementEvent.ShowMessage(
                        errorMsg,
                        TraderManagementEvent.MessageType.ERROR
                    )
                )
                setState { it.copy(isSaving = false) }
            }
        }
    }

    fun deleteSelectedTrader() {
        val traderId = state.value.selectedTraderId
        if (traderId == null) {
            setFormError("delete", "Please select a trader first.")
            return
        }
        val selectedTrader = state.value.traders.firstOrNull { it.id == traderId }
        if (selectedTrader?.status == TraderStatus.RUNNING) {
            setFormError("delete", "Cannot delete active trader. Stop the trader first.")
            return
        }
        clearFormError("delete")
        setState { it.copy(isSaving = true) }
        launchIO {
            try {
                executeWithRetry(
                    operation = { traderService.deleteTrader(traderId) },
                    successMessage = "", // No success dialog
                    errorMessage = "Unable to delete trader"
                )
                // After deletion, select the first trader in the list if available
                val remainingTraders = state.value.traders.filterNot { it.id == traderId }
                val newSelection = remainingTraders.firstOrNull()?.id
                setState { 
                    it.copy(
                        selectedTraderId = newSelection,
                        form = if (newSelection != null) {
                            TraderForm.fromTrader(remainingTraders.first { it.id == newSelection })
                        } else {
                            TraderForm()
                        },
                        hasUnsavedChanges = false
                    ) 
                }
            } catch (ex: Exception) {
                val errorMsg = formatErrorMessage(ex, "Unable to delete trader")
                setFormError("delete", errorMsg)
            } finally {
                setState { it.copy(isSaving = false) }
            }
        }
    }
    
    fun setFormError(field: String, message: String) {
        val fieldName = field // Capture in local variable
        val errorMessage = message // Capture in local variable
        setState { currentState ->
            currentState.copy(
                form = currentState.form.copy(
                    errors = currentState.form.errors + (fieldName to errorMessage)
                )
            )
        }
    }
    
    fun clearFormError(field: String) {
        val fieldToRemove = field // Capture in local variable to avoid lambda capture issues
        setState { currentState ->
            currentState.copy(
                form = currentState.form.copy(
                    errors = currentState.form.errors - fieldToRemove
                )
            )
        }
    }
    
    fun clearAllFormErrors() {
        setState { currentState ->
            currentState.copy(
                form = currentState.form.copy(
                    errors = emptyMap()
                )
            )
        }
    }

    fun startTrader(id: String) = performLifecycleAction(id, ActionType.START)

    fun stopTrader(id: String) = performLifecycleAction(id, ActionType.STOP)

    private fun performLifecycleAction(id: String, action: ActionType) {
        launchIO {
            try {
                when (action) {
                    ActionType.START -> {
                        executeWithRetry(
                            operation = { traderService.startTrader(id) },
                            successMessage = "Trader started",
                            errorMessage = "Unable to start trader"
                        )
                    }
                    ActionType.STOP -> {
                        executeWithRetry(
                            operation = { traderService.stopTrader(id) },
                            successMessage = "Trader stopped",
                            errorMessage = "Unable to stop trader"
                        )
                    }
                }
            } catch (ex: Exception) {
                val errorMsg = formatErrorMessage(ex, "Unable to perform action")
                publishEvent(
                    TraderManagementEvent.ShowMessage(
                        errorMsg,
                        TraderManagementEvent.MessageType.ERROR
                    )
                )
            }
        }
    }

    private fun validateForm(form: TraderForm): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        logger.debug { "🔍 Validating form: name='${form.name}', budget=${form.budget}, baseAsset='${form.baseAsset}', quoteAsset='${form.quoteAsset}'" }
        
        if (form.name.isBlank()) {
            errors["name"] = "Name is required"
        }
        if (form.name.length > 100) {
            errors["name"] = "Name must be 100 characters or less"
        }
        
        if (form.budget <= 0.0) {
            errors["budget"] = "Budget must be positive"
        }
        if (form.budget > 1000000.0) {
            errors["budget"] = "Budget must be less than 1,000,000 USDT"
        }
        
        if (form.baseAsset.isBlank()) {
            errors["baseAsset"] = "Base asset is required"
        }
        
        if (form.quoteAsset.isBlank()) {
            errors["quoteAsset"] = "Quote asset is required"
        }
        
        if (form.leverage !in 1..125) {
            errors["leverage"] = "Leverage must be between 1 and 125"
        }
        
        if (form.stopLossPercent !in 0.5..50.0) {
            errors["stopLoss"] = "Stop loss must be between 0.5% and 50%"
        }
        
        if (form.takeProfitPercent !in 0.5..50.0) {
            errors["takeProfit"] = "Take profit must be between 0.5% and 50%"
        }
        
        // Validate strategy is a valid TradingStrategy enum value
        try {
            com.fmps.autotrader.shared.model.TradingStrategy.valueOf(form.strategy)
        } catch (e: IllegalArgumentException) {
            errors["strategy"] = "Invalid strategy. Must be one of: ${com.fmps.autotrader.shared.model.TradingStrategy.values().joinToString(", ") { it.name }}"
        }
        
        logger.debug { "🔍 Validation result: ${errors.size} errors" }
        return errors
    }

    /**
     * Executes an operation with exponential backoff retry logic.
     * Retries up to 3 times for transient errors (network, timeouts, 5xx).
     */
    private suspend fun <T> executeWithRetry(
        operation: suspend () -> T,
        successMessage: String,
        errorMessage: String,
        maxRetries: Int = 3,
        initialDelayMs: Long = 500
    ): T {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                val result = operation()
                // Only show success message if provided (not empty)
                if (successMessage.isNotBlank()) {
                    publishEvent(
                        TraderManagementEvent.ShowMessage(
                            successMessage,
                            TraderManagementEvent.MessageType.SUCCESS
                        )
                    )
                }
                return result
            } catch (ex: Exception) {
                lastException = ex
                val isRetryable = isRetryableError(ex)
                if (!isRetryable || attempt == maxRetries - 1) {
                    // Don't retry non-retryable errors or if we've exhausted retries
                    throw ex
                }
                // Exponential backoff: 500ms, 1000ms, 2000ms
                val delayMs = initialDelayMs * (2.0.pow(attempt)).toLong()
                delay(delayMs)
            }
        }
        throw lastException ?: Exception(errorMessage)
    }

    /**
     * Determines if an error is retryable (network issues, timeouts, server errors).
     */
    private fun isRetryableError(ex: Exception): Boolean {
        return when {
            ex is ConnectTimeoutException -> true
            ex is SocketTimeoutException -> true
            ex.message?.contains("timeout", ignoreCase = true) == true -> true
            ex.message?.contains("connection", ignoreCase = true) == true -> true
            ex.message?.contains("network", ignoreCase = true) == true -> true
            ex is ClientRequestException -> {
                val statusCode = ex.response.status.value
                statusCode in 500..599 || statusCode == 408 || statusCode == 429
            }
            ex is ServerResponseException -> true
            else -> false
        }
    }

    /**
     * Formats error messages with structured information from HTTP responses.
     */
    private fun formatErrorMessage(ex: Exception, defaultMessage: String): String {
        return when (ex) {
            is ClientRequestException -> {
                val statusCode = ex.response.status.value
                val statusDescription = ex.response.status.description
                // For 400 errors, try to parse the error response for better messages
                when (statusCode) {
                    400 -> {
                        // Use ex.message which already contains the parsed error message from RealTraderService
                        ex.message ?: "Invalid request. Please check your input and try again."
                    }
                    401 -> "Authentication failed: Please check your API credentials"
                    403 -> "Access forbidden: Insufficient permissions"
                    404 -> "Resource not found: The trader may have been deleted"
                    409 -> "Conflict: Trader limit reached or resource conflict"
                    429 -> "Rate limit exceeded: Please wait before retrying"
                    else -> ex.message ?: statusDescription ?: defaultMessage
                }
            }
            is ServerResponseException -> {
                val statusCode = ex.response.status.value
                "Server error (${statusCode}): ${ex.response.status.description ?: "Internal server error"}"
            }
            else -> ex.message ?: defaultMessage
        }
    }

    private enum class ActionType { START, STOP }
}

