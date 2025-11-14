package com.fmps.autotrader.desktop.traders

import com.fmps.autotrader.desktop.mvvm.BaseViewModel
import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.services.TraderDetail
import com.fmps.autotrader.desktop.services.TraderRiskLevel
import com.fmps.autotrader.desktop.services.TraderService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
                    traderService.createTrader(currentForm.toDraft())
                    publishEvent(
                        TraderManagementEvent.ShowMessage(
                            "Trader ${currentForm.name} created",
                        TraderManagementEvent.MessageType.SUCCESS
                        )
                    )
                } else {
                    traderService.updateTrader(currentForm.id!!, currentForm.toDraft())
                    publishEvent(
                        TraderManagementEvent.ShowMessage(
                            "Trader ${currentForm.name} updated",
                        TraderManagementEvent.MessageType.SUCCESS
                        )
                    )
                }
            } catch (ex: Exception) {
                publishEvent(
                    TraderManagementEvent.ShowMessage(
                        ex.message ?: "Unable to save trader",
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
                traderService.deleteTrader(traderId)
                publishEvent(
                    TraderManagementEvent.ShowMessage(
                        "Trader deleted",
                        TraderManagementEvent.MessageType.INFO
                    )
                )
                setState { it.copy(selectedTraderId = null, form = TraderForm()) }
            } catch (ex: Exception) {
                publishEvent(
                    TraderManagementEvent.ShowMessage(
                        ex.message ?: "Unable to delete trader",
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
                    ActionType.START -> traderService.startTrader(id)
                    ActionType.STOP -> traderService.stopTrader(id)
                }
                val message = when (action) {
                    ActionType.START -> "Trader started"
                    ActionType.STOP -> "Trader stopped"
                }
                publishEvent(TraderManagementEvent.ShowMessage(message))
            } catch (ex: Exception) {
                publishEvent(
                    TraderManagementEvent.ShowMessage(
                        ex.message ?: "Unable to perform action",
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
        return errors
    }

    private enum class ActionType { START, STOP }
}

