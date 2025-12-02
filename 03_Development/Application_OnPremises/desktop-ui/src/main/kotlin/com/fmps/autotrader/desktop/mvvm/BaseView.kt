package com.fmps.autotrader.desktop.mvvm

import javafx.application.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
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
            println("🔍 BaseView: Attempting to retrieve ViewModel ${viewModelClass.simpleName}...")
            // Use GlobalContext - this works even when View is created via factory
            val koin = org.koin.core.context.GlobalContext.get()
            println("🔍 BaseView: Got Koin instance from GlobalContext")
            val vm = if (qualifier != null) {
                koin.get(viewModelClass, qualifier = qualifier) as VM
            } else {
                koin.get(viewModelClass) as VM
            }
            println("✅ BaseView: Successfully retrieved ViewModel ${viewModelClass.simpleName}")
            vm
        } catch (e: Exception) {
            println("⚠️  BaseView: GlobalContext failed, trying getKoin()...")
            // If GlobalContext fails, try getKoin() from KoinComponent
            try {
                val koin = getKoin()
                println("🔍 BaseView: Got Koin instance from getKoin()")
                val vm = if (qualifier != null) {
                    koin.get(viewModelClass, qualifier = qualifier) as VM
                } else {
                    koin.get(viewModelClass) as VM
                }
                println("✅ BaseView: Successfully retrieved ViewModel ${viewModelClass.simpleName} via getKoin()")
                vm
            } catch (e2: Exception) {
                // Log the error but don't throw - allow View construction to complete
                // The ViewModel will be available after onDock() is called
                println("❌ BaseView: Could not retrieve ViewModel ${viewModelClass.simpleName}")
                println("   GlobalContext error: ${e.message}")
                println("   getKoin() error: ${e2.message}")
                println("   This is normal if accessed during View construction. ViewModel will be available after onDock().")
                e2.printStackTrace()
                null
            }
        }
    }

    private val viewScope = CoroutineScope(Dispatchers.Main)
    private var bindingJob: Job? = null

    override fun onDock() {
        super.onDock()
        println("🔍 BaseView.onDock() called for ${this::class.simpleName}")
        // Ensure ViewModel is initialized before accessing it
        // This is safe because onDock() is called after View construction
        val vm = viewModel
        val viewClassName = this::class.simpleName ?: "UnknownView"
        println("🔍 BaseView: Starting event and state collection for $viewClassName")
        bindingJob = viewScope.launch {
            launch(Dispatchers.Main) {
                println("🔍 BaseView: Starting state collection for $viewClassName")
                // Use collectLatest to ensure we always process the latest state immediately
                // This cancels any pending onStateChanged calls if a new state arrives
                vm.state.collectLatest { state ->
                    println("🔍 BaseView: State changed for $viewClassName, calling onStateChanged()")
                    // We're already on Main thread, so call directly
                    // onStateChanged should be fast and non-blocking
                    println("🔍 BaseView: About to call onStateChanged() on Main thread")
                    onStateChanged(state) 
                }
            }
            launch {
                println("🔍 BaseView: Starting event collection for ${this::class.simpleName}")
                vm.events.collect { event ->
                    println("🔍 BaseView: Event received for ${this::class.simpleName}: ${event.javaClass.simpleName}, calling onEvent()")
                    withContext(Dispatchers.Main) { 
                        println("🔍 BaseView: Calling onEvent() on JavaFX thread for ${this::class.simpleName}")
                        onEvent(event) 
                    }
                }
            }
        }
        println("🔍 BaseView: Event and state collection started for ${this::class.simpleName}")
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

