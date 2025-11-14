package com.fmps.autotrader.desktop.di

import com.fmps.autotrader.desktop.config.ConfigurationView
import com.fmps.autotrader.desktop.config.ConfigurationViewModel
import com.fmps.autotrader.desktop.dashboard.DashboardView
import com.fmps.autotrader.desktop.dashboard.DashboardViewModel
import com.fmps.autotrader.desktop.mvvm.DefaultDispatcherProvider
import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.navigation.NavigationService
import com.fmps.autotrader.desktop.services.ConfigService
import com.fmps.autotrader.desktop.services.CoreServiceClient
import com.fmps.autotrader.desktop.services.MarketDataService
import com.fmps.autotrader.desktop.services.StubConfigService
import com.fmps.autotrader.desktop.services.StubCoreServiceClient
import com.fmps.autotrader.desktop.services.StubTelemetryClient
import com.fmps.autotrader.desktop.services.StubMarketDataService
import com.fmps.autotrader.desktop.services.StubTraderService
import com.fmps.autotrader.desktop.services.TelemetryClient
import com.fmps.autotrader.desktop.services.TraderService
import com.fmps.autotrader.desktop.shell.ShellViewModel
import com.fmps.autotrader.desktop.monitoring.MonitoringView
import com.fmps.autotrader.desktop.monitoring.MonitoringViewModel
import com.fmps.autotrader.desktop.traders.TraderManagementView
import com.fmps.autotrader.desktop.traders.TraderManagementViewModel
import com.fmps.autotrader.desktop.views.ConfigurationPlaceholderView
import com.fmps.autotrader.desktop.views.PatternAnalyticsPlaceholderView
import org.koin.dsl.module

val desktopModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single { NavigationService() }
    single<CoreServiceClient> { StubCoreServiceClient() }
    single<TelemetryClient> { StubTelemetryClient() }
    single<TraderService> { StubTraderService() }
    single<MarketDataService> { StubMarketDataService() }
    single<ConfigService> { StubConfigService() }

    factory { ShellViewModel(get(), get(), get()) }
    factory { DashboardViewModel(get(), get(), get()) }
    factory { DashboardView() }
    factory { TraderManagementViewModel(get(), get()) }
    factory { TraderManagementView() }
    factory { MonitoringViewModel(get(), get()) }
    factory { MonitoringView() }
    factory { ConfigurationViewModel(get(), get()) }
    factory { ConfigurationView() }
    factory { PatternAnalyticsPlaceholderView() }
}

