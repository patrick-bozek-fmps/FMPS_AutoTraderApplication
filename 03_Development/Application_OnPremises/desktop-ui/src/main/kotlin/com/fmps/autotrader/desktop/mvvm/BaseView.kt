package com.fmps.autotrader.desktop.mvvm

import javafx.application.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.Qualifier
import org.koin.core.context.GlobalContext
import org.koin.core.component.get
import tornadofx.View
import kotlin.reflect.KClass

/**
 * Base TornadoFX view that binds to a [BaseViewModel] and automatically bridges state and event
 * updates onto the JavaFX UI thread.
 */
abstract class BaseView<State : Any, Event : ViewEvent, VM : BaseViewModel<State, Event>>(
    private val viewModelClass: KClass<VM>,
    private val qualifier: Qualifier? = null
) : View(), KoinComponent {

    protected val viewModel: VM by lazy { GlobalContext.get().get(viewModelClass, qualifier = qualifier) }

    private val viewScope = CoroutineScope(Dispatchers.Main)
    private var bindingJob: Job? = null

    override fun onDock() {
        super.onDock()
        bindingJob = viewScope.launch {
            launch {
                viewModel.state.collect { state ->
                    withContext(Dispatchers.Main) { onStateChanged(state) }
                }
            }
            launch {
                viewModel.events.collect { event ->
                    withContext(Dispatchers.Main) { onEvent(event) }
                }
            }
        }
    }

    override fun onUndock() {
        super.onUndock()
        bindingJob?.cancel()
        if (!Platform.isFxApplicationThread()) {
            viewScope.cancel()
        }
    }

    protected open fun onStateChanged(state: State) = Unit

    protected open fun onEvent(event: Event) = Unit
}

