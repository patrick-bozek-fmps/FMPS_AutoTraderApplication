package com.fmps.autotrader.desktop.mvvm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

/**
 * Marker interface for strongly typed view events emitted by [BaseViewModel].
 */
interface ViewEvent

/**
 * Base implementation for MVVM-style view models. Provides coroutine scopes, state
 * management via [StateFlow], and a shared event channel for one-off UI actions.
 */
abstract class BaseViewModel<State : Any, Event : ViewEvent>(
    initialState: State,
    private val dispatcherProvider: DispatcherProvider
) : CoroutineScope {

    private val rootJob = SupervisorJob()
    override val coroutineContext = rootJob + dispatcherProvider.main

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    /**
     * Update the current state atomically.
     * Uses value assignment to ensure immediate emission to collectors.
     */
    protected fun setState(reducer: (State) -> State) {
        val oldState = _state.value
        val newState = reducer(oldState)
        _state.value = newState
        println("🔍 BaseViewModel (${this::class.simpleName}): State updated from ${oldState.hashCode()} to ${newState.hashCode()}")
    }

    /**
     * Emit an event to the UI. Uses a buffered shared flow to avoid blocking.
     * Always emits on main dispatcher to ensure events reach the view.
     */
    protected fun publishEvent(event: Event) {
        launch(dispatcherProvider.main) {
            _events.emit(event)
            println("🔍 BaseViewModel: Event emitted: ${event.javaClass.simpleName}")
        }
    }

    /**
     * Launch work on the IO dispatcher.
     */
    protected fun launchIO(block: suspend CoroutineScope.() -> Unit) =
        (this + dispatcherProvider.io).launch(block = block)

    /**
     * Launch work on the computation dispatcher.
     */
    protected fun launchComputation(block: suspend CoroutineScope.() -> Unit) =
        (this + dispatcherProvider.computation).launch(block = block)

    open fun onCleared() {
        cancel()
    }
}


