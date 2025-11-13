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
     */
    protected fun setState(reducer: (State) -> State) {
        _state.update(reducer)
    }

    /**
     * Emit an event to the UI. Uses a buffered shared flow to avoid blocking.
     */
    protected fun publishEvent(event: Event) {
        if (!_events.tryEmit(event)) {
            launch(dispatcherProvider.main) {
                _events.emit(event)
            }
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


