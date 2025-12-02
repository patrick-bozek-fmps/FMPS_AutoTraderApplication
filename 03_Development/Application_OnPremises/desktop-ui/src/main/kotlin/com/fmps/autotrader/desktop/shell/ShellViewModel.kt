package com.fmps.autotrader.desktop.shell

import com.fmps.autotrader.desktop.i18n.Localization
import com.fmps.autotrader.desktop.mvvm.BaseViewModel
import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.navigation.NavigationEvent
import com.fmps.autotrader.desktop.navigation.NavigationService
import com.fmps.autotrader.desktop.services.ConnectionStatusService
import com.fmps.autotrader.desktop.services.CoreServiceClient
import com.fmps.autotrader.desktop.services.ExchangeConnectionStatusService
import com.fmps.autotrader.desktop.services.TelemetryClient
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ShellViewModel(
    dispatcherProvider: DispatcherProvider,
    private val navigationService: NavigationService,
    private val coreServiceClient: CoreServiceClient,
    private val connectionStatusService: ConnectionStatusService,
    private val telemetryClient: TelemetryClient,
    private val exchangeConnectionStatusService: ExchangeConnectionStatusService
) : BaseViewModel<ShellState, ShellEvent>(ShellState(), dispatcherProvider) {

    init {
        // Start monitoring connection status
        connectionStatusService.startMonitoring()
        
        // Start telemetry client (WebSocket) automatically on app startup
        telemetryClient.start()
        
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
        
        // Observe connection status changes
        launchIO {
            connectionStatusService.status.collectLatest { status ->
                setState { state ->
                    state.copy(
                        connectionStatus = status,
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )
                }
            }
        }
        
        // Observe connection error messages
        launchIO {
            connectionStatusService.errorMessage.collectLatest { errorMessage ->
                setState { state ->
                    state.copy(
                        connectionErrorMessage = errorMessage,
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )
                }
            }
        }
        
        // Observe exchange connection status (Binance and Bitget)
        launchIO {
            combine(
                exchangeConnectionStatusService.binanceStatus,
                exchangeConnectionStatusService.bitgetStatus
            ) { binance, bitget ->
                println("🔍 ShellViewModel: Exchange status updated - Binance=$binance, Bitget=$bitget")
                setState { state ->
                    state.copy(
                        binanceConnected = binance,
                        bitgetConnected = bitget,
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )
                }
            }.collectLatest { }
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
    
    fun showConnectionHelp() {
        val instructions = connectionStatusService.getStartInstructions()
        publishEvent(ShellEvent.ShowConnectionHelp(instructions))
    }

    private fun buildBreadcrumbs(currentTitle: String): List<String> =
        listOf("Home", currentTitle)
}

