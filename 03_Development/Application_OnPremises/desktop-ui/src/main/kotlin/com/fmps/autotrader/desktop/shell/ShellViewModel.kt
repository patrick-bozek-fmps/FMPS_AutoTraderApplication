package com.fmps.autotrader.desktop.shell

import com.fmps.autotrader.desktop.i18n.Localization
import com.fmps.autotrader.desktop.mvvm.BaseViewModel
import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.navigation.NavigationEvent
import com.fmps.autotrader.desktop.navigation.NavigationService
import com.fmps.autotrader.desktop.services.CoreServiceClient
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ShellViewModel(
    dispatcherProvider: DispatcherProvider,
    private val navigationService: NavigationService,
    private val coreServiceClient: CoreServiceClient
) : BaseViewModel<ShellState, ShellEvent>(ShellState(), dispatcherProvider) {

    init {
        navigationService.addListener { event ->
            when (event) {
                is NavigationEvent.Navigated -> setState { state ->
                    state.copy(
                        currentRoute = event.descriptor.route,
                        breadcrumbs = buildBreadcrumbs(event.descriptor.title),
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )
                }
                is NavigationEvent.BackstackChanged -> setState { state ->
                    state.copy(
                        canNavigateBack = event.size > 1,
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )
                }
            }
        }

        launchIO {
            coreServiceClient.traderSummaries().collectLatest { summaries ->
                setState { state ->
                    state.copy(
                        traderSummaries = summaries,
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    fun navigate(route: String) {
        runCatching { navigationService.navigate(route) }
            .onFailure {
                publishEvent(ShellEvent.Toast("Unable to navigate to '$route' (${it.message})"))
            }
    }

    fun goBack() {
        if (navigationService.canGoBack()) {
            navigationService.goBack()
        } else {
            publishEvent(ShellEvent.Toast(Localization.string("toast.no_previous", "No previous view available")))
        }
    }

    private fun buildBreadcrumbs(currentTitle: String): List<String> =
        listOf("Home", currentTitle)
}

