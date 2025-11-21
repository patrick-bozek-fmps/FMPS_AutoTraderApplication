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
    // Delay ViewModel retrieval until onDock() to avoid issues during View construction
    private var _viewModel: VM? = null
    
    protected val viewModel: VM
        get() {
            if (_viewModel == null) {
                _viewModel = retrieveViewModel()
            }
            return _viewModel ?: throw IllegalStateException(
                "ViewModel ${viewModelClass.simpleName} is not available. " +
                "This might happen if accessed during View construction. " +
                "Access the ViewModel only after the View is fully constructed (e.g., in onDock())."
            )
        }
    
    private fun retrieveViewModel(): VM? {
        return try {
            // Use GlobalContext - this works even when View is created via factory
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
                // Log the error but don't throw - allow View construction to complete
                // The ViewModel will be available after onDock() is called
                println("⚠️  Warning: Could not retrieve ViewModel ${viewModelClass.simpleName} during construction: ${e.message}")
                println("   This is normal if accessed during View construction. ViewModel will be available after onDock().")
                null
            }
        }
    }

    private val viewScope = CoroutineScope(Dispatchers.Main)
    private var bindingJob: Job? = null

    override fun onDock() {
        super.onDock()
        // Ensure ViewModel is initialized before accessing it
        // This is safe because onDock() is called after View construction
        val vm = viewModel
        bindingJob = viewScope.launch {
            launch {
                vm.state.collect { state ->
                    withContext(Dispatchers.Main) { onStateChanged(state) }
                }
            }
            launch {
                vm.events.collect { event ->
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

