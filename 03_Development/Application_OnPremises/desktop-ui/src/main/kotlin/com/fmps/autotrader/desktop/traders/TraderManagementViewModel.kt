package com.fmps.autotrader.desktop.traders

import com.fmps.autotrader.desktop.mvvm.BaseViewModel
import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.services.TraderDetail
import com.fmps.autotrader.desktop.services.TraderRiskLevel
import com.fmps.autotrader.desktop.services.TraderService
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.network.sockets.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.pow

class TraderManagementViewModel(
    dispatcherProvider: DispatcherProvider,
    private val traderService: TraderService
) : BaseViewModel<TraderManagementState, TraderManagementEvent>(
    TraderManagementState(),
    dispatcherProvider
) {

    init {
        launchIO {
            traderService.traders().collectLatest { traders ->
                setState { state ->
                    val sanitizedSelection = state.selectedTraderId?.takeIf { id -> traders.any { it.id == id } }
                    val updatedForm = when {
                        sanitizedSelection == null && state.form.isNew.not() -> TraderForm()
                        sanitizedSelection != null -> TraderForm.fromTrader(traders.first { it.id == sanitizedSelection })
                        else -> state.form
                    }
                    val filtered = traders.filter(state.searchQuery, state.statusFilter)
                    state.copy(
                        isLoading = false,
                        traders = traders,
                        filteredTraders = filtered,
                        selectedTraderId = sanitizedSelection,
                        form = updatedForm
                    )
                }
            }
        }
    }

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
                state.copy(selectedTraderId = null, form = TraderForm())
            } else {
                state.copy(
                    selectedTraderId = trader.id,
                    form = TraderForm.fromTrader(trader)
                )
            }
        }
    }

    fun newTrader() {
        setState { it.copy(selectedTraderId = null, form = TraderForm()) }
    }

    fun updateForm(updater: (TraderForm) -> TraderForm) {
        setState { state -> state.copy(form = updater(state.form)) }
    }

    fun saveTrader() {
        val currentForm = state.value.form
        val errors = validateForm(currentForm)
        if (errors.isNotEmpty()) {
            setState { it.copy(form = currentForm.copy(errors = errors)) }
            return
        }

        setState { it.copy(isSaving = true, form = currentForm.copy(errors = emptyMap())) }
        launchIO {
            try {
                if (currentForm.isNew) {
                    executeWithRetry(
                        operation = { traderService.createTrader(currentForm.toDraft()) },
                        successMessage = "Trader ${currentForm.name} created",
                        errorMessage = "Unable to create trader"
                    )
                } else {
                    executeWithRetry(
                        operation = { traderService.updateTrader(currentForm.id!!, currentForm.toDraft()) },
                        successMessage = "Trader ${currentForm.name} updated",
                        errorMessage = "Unable to update trader"
                    )
                }
            } catch (ex: Exception) {
                val errorMsg = formatErrorMessage(ex, "Unable to save trader")
                publishEvent(
                    TraderManagementEvent.ShowMessage(
                        errorMsg,
                        TraderManagementEvent.MessageType.ERROR
                    )
                )
            } finally {
                setState { it.copy(isSaving = false) }
            }
        }
    }

    fun deleteSelectedTrader() {
        val traderId = state.value.selectedTraderId ?: return
        setState { it.copy(isSaving = true) }
        launchIO {
            try {
                executeWithRetry(
                    operation = { traderService.deleteTrader(traderId) },
                    successMessage = "Trader deleted",
                    errorMessage = "Unable to delete trader"
                )
                setState { it.copy(selectedTraderId = null, form = TraderForm()) }
            } catch (ex: Exception) {
                val errorMsg = formatErrorMessage(ex, "Unable to delete trader")
                publishEvent(
                    TraderManagementEvent.ShowMessage(
                        errorMsg,
                        TraderManagementEvent.MessageType.ERROR
                    )
                )
            } finally {
                setState { it.copy(isSaving = false) }
            }
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
        if (form.name.isBlank()) errors["name"] = "Name is required"
        if (form.budget <= 0.0) errors["budget"] = "Budget must be positive"
        if (form.baseAsset.isBlank()) errors["baseAsset"] = "Base asset required"
        if (form.quoteAsset.isBlank()) errors["quoteAsset"] = "Quote asset required"
        
        // Validate API credentials if provided
        if (form.apiKey.isNotBlank() && form.apiSecret.isBlank()) {
            errors["apiSecret"] = "API Secret is required when API Key is provided"
        }
        if (form.exchange.equals("Bitget", ignoreCase = true) && form.apiKey.isNotBlank() && form.apiPassphrase.isBlank()) {
            errors["apiPassphrase"] = "Passphrase is required for Bitget exchange"
        }
        
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
                publishEvent(
                    TraderManagementEvent.ShowMessage(
                        successMessage,
                        TraderManagementEvent.MessageType.SUCCESS
                    )
                )
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
                when (statusCode) {
                    400 -> "Invalid request: ${ex.message ?: statusDescription ?: "Bad request"}"
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

