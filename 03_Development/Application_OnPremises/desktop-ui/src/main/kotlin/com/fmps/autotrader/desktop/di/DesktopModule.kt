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
import com.fmps.autotrader.desktop.services.PatternAnalyticsService
import com.fmps.autotrader.desktop.services.StubConfigService
import com.fmps.autotrader.desktop.services.StubCoreServiceClient
import com.fmps.autotrader.desktop.services.StubTelemetryClient
import com.fmps.autotrader.desktop.services.StubMarketDataService
import com.fmps.autotrader.desktop.services.StubPatternAnalyticsService
import com.fmps.autotrader.desktop.services.HttpClientFactory
import com.fmps.autotrader.desktop.services.RealTraderService
import com.fmps.autotrader.desktop.services.StubTraderService
import com.fmps.autotrader.desktop.services.TelemetryClient
import com.fmps.autotrader.desktop.services.TraderService
import io.ktor.client.*
import com.fmps.autotrader.desktop.shell.ShellViewModel
import com.fmps.autotrader.desktop.monitoring.MonitoringView
import com.fmps.autotrader.desktop.monitoring.MonitoringViewModel
import com.fmps.autotrader.desktop.patterns.PatternAnalyticsView
import com.fmps.autotrader.desktop.patterns.PatternAnalyticsViewModel
import com.fmps.autotrader.desktop.traders.TraderManagementView
import com.fmps.autotrader.desktop.traders.TraderManagementViewModel
import org.koin.dsl.module

val desktopModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single { NavigationService() }
    
    // HTTP Client for REST API calls
    single<HttpClient> { HttpClientFactory.create() }
    
    single<CoreServiceClient> { StubCoreServiceClient() }
    single<TelemetryClient> { StubTelemetryClient() }
    single<TraderService> { RealTraderService(get()) } // Use RealTraderService instead of stub
    single<MarketDataService> { StubMarketDataService() }
    single<ConfigService> { StubConfigService() }
    single<PatternAnalyticsService> { StubPatternAnalyticsService() }

    factory { ShellViewModel(get(), get(), get()) }
    factory { DashboardViewModel(get(), get(), get(), get()) }
    factory { DashboardView() }
    factory { TraderManagementViewModel(get(), get()) }
    factory { TraderManagementView() }
    factory { MonitoringViewModel(get(), get()) }
    factory { MonitoringView() }
    factory { PatternAnalyticsViewModel(get(), get()) }
    factory { PatternAnalyticsView() }
    factory { ConfigurationViewModel(get(), get()) }
    factory { ConfigurationView() }
}

