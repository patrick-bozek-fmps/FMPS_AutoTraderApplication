package com.fmps.autotrader.desktop.di

import com.fmps.autotrader.desktop.mvvm.DefaultDispatcherProvider
import com.fmps.autotrader.desktop.mvvm.DispatcherProvider
import com.fmps.autotrader.desktop.navigation.NavigationService
import com.fmps.autotrader.desktop.services.CoreServiceClient
import com.fmps.autotrader.desktop.services.StubCoreServiceClient
import com.fmps.autotrader.desktop.services.StubTelemetryClient
import com.fmps.autotrader.desktop.services.TelemetryClient
import com.fmps.autotrader.desktop.shell.ShellViewModel
import com.fmps.autotrader.desktop.views.ConfigurationPlaceholderView
import com.fmps.autotrader.desktop.views.DashboardView
import com.fmps.autotrader.desktop.views.MonitoringPlaceholderView
import com.fmps.autotrader.desktop.views.PatternAnalyticsPlaceholderView
import com.fmps.autotrader.desktop.views.TraderManagementPlaceholderView
import org.koin.dsl.module

val desktopModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single { NavigationService() }
    single<CoreServiceClient> { StubCoreServiceClient() }
    single<TelemetryClient> { StubTelemetryClient() }

    factory { ShellViewModel(get(), get(), get()) }
    factory { DashboardView() }
    factory { TraderManagementPlaceholderView() }
    factory { MonitoringPlaceholderView() }
    factory { ConfigurationPlaceholderView() }
    factory { PatternAnalyticsPlaceholderView() }
}

