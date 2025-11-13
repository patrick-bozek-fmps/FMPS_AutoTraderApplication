package com.fmps.autotrader.desktop.navigation

import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import tornadofx.UIComponent
import java.util.concurrent.CopyOnWriteArrayList

data class ViewDescriptor(
    val route: String,
    val title: String,
    val factory: () -> UIComponent,
    val category: String = "general"
)

sealed class NavigationEvent {
    data class Navigated(val descriptor: ViewDescriptor) : NavigationEvent()
    data class BackstackChanged(val size: Int) : NavigationEvent()
}

class NavigationService {
    private val descriptors = mutableMapOf<String, ViewDescriptor>()
    private val instances = mutableMapOf<String, UIComponent>()
    private val history = ArrayDeque<String>()
    private val listeners = CopyOnWriteArrayList<(NavigationEvent) -> Unit>()

    private val currentViewPropertyInternal = ReadOnlyObjectWrapper<UIComponent?>()
    val currentViewProperty: ReadOnlyObjectProperty<UIComponent?> = currentViewPropertyInternal.readOnlyProperty

    fun register(descriptor: ViewDescriptor) {
        require(descriptor.route.isNotBlank()) { "Route must not be blank" }
        require(!descriptors.containsKey(descriptor.route)) { "Route '${descriptor.route}' already registered" }
        descriptors[descriptor.route] = descriptor
    }

    fun registerAll(entries: Collection<ViewDescriptor>) {
        entries.forEach { register(it) }
    }

    fun navigate(route: String) {
        val descriptor = descriptors[route] ?: error("No view registered for route '$route'")
        val component = instances.getOrPut(route) { descriptor.factory() }
        if (history.isEmpty() || history.last() != route) {
            history.addLast(route)
            notifyListeners(NavigationEvent.BackstackChanged(history.size))
        }
        currentViewPropertyInternal.set(component)
        notifyListeners(NavigationEvent.Navigated(descriptor))
    }

    fun canGoBack(): Boolean = history.size > 1

    fun goBack() {
        if (!canGoBack()) return
        // Remove current
        history.removeLast()
        // Navigate to previous but do not push again
        val previousRoute = history.last()
        val descriptor = descriptors[previousRoute] ?: return
        val component = instances.getOrPut(previousRoute) { descriptor.factory() }
        currentViewPropertyInternal.set(component)
        notifyListeners(NavigationEvent.BackstackChanged(history.size))
        notifyListeners(NavigationEvent.Navigated(descriptor))
    }

    fun clear() {
        history.clear()
        instances.clear()
        currentViewPropertyInternal.set(null)
        notifyListeners(NavigationEvent.BackstackChanged(0))
    }

    fun addListener(listener: (NavigationEvent) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (NavigationEvent) -> Unit) {
        listeners -= listener
    }

    private fun notifyListeners(event: NavigationEvent) {
        listeners.forEach { it.invoke(event) }
    }
}

