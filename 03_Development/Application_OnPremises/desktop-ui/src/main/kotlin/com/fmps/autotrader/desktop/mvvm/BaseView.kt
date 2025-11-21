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

    // Get ViewModel from Koin
    // Use GlobalContext.get() first as it works even when Views are created via factory
    // Fall back to getKoin() if GlobalContext is not available
    protected val viewModel: VM by lazy {
        try {
            // First, try GlobalContext - this works even when View is created via factory
            val koin = org.koin.core.context.GlobalContext.get()
            if (qualifier != null) {
                koin.get(viewModelClass, qualifier = qualifier) as VM
            } else {
                koin.get(viewModelClass) as VM
            }
        } catch (e: Exception) {
            // If GlobalContext fails, try getKoin() from KoinComponent
            try {
                val koin = getKoin()
                if (qualifier != null) {
                    koin.get(viewModelClass, qualifier = qualifier) as VM
                } else {
                    koin.get(viewModelClass) as VM
                }
            } catch (e2: Exception) {
                throw IllegalStateException(
                    "Failed to get ViewModel ${viewModelClass.simpleName}. " +
                    "Make sure it's registered in Koin module. " +
                    "Original error: ${e.message}, Secondary error: ${e2.message}",
                    e
                )
            }
        }
    }

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

