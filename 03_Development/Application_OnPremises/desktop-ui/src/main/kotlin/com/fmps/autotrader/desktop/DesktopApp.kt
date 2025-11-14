package com.fmps.autotrader.desktop

import com.fmps.autotrader.desktop.dashboard.DashboardView
import com.fmps.autotrader.desktop.di.desktopModule
import com.fmps.autotrader.desktop.i18n.Localization
import com.fmps.autotrader.desktop.navigation.NavigationService
import com.fmps.autotrader.desktop.navigation.ViewDescriptor
import com.fmps.autotrader.desktop.shell.ShellView
import com.fmps.autotrader.desktop.traders.TraderManagementView
import com.fmps.autotrader.desktop.views.ConfigurationPlaceholderView
import com.fmps.autotrader.desktop.views.MonitoringPlaceholderView
import com.fmps.autotrader.desktop.views.PatternAnalyticsPlaceholderView
import javafx.stage.Stage
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.component.get
import tornadofx.App
import tornadofx.launch
import tornadofx.importStylesheet

class DesktopApp : App(ShellView::class) {
    private lateinit var koinApp: KoinApplication
    private lateinit var navigationService: NavigationService

    override fun init() {
        super.init()
        importStylesheet("/styles/theme.css")
        koinApp = startKoin {
            modules(desktopModule)
        }
        navigationService = koinApp.koin.get()
        registerNavigation()
    }

    override fun start(stage: Stage) {
        stage.title = Localization.string("app.title", "FMPS AutoTrader Desktop")
        stage.minWidth = 1080.0
        stage.minHeight = 720.0
        super.start(stage)
        navigationService.navigate("dashboard")
    }

    override fun stop() {
        navigationService.clear()
        super.stop()
        stopKoin()
    }

    private fun registerNavigation() {
        val koin = koinApp.koin
        navigationService.registerAll(
            listOf(
                ViewDescriptor(route = "dashboard", title = "Overview", factory = { koin.get<DashboardView>() }),
                ViewDescriptor(route = "traders", title = "AI Traders", factory = { koin.get<TraderManagementView>() }),
                ViewDescriptor(route = "monitoring", title = "Monitoring", factory = { koin.get<MonitoringPlaceholderView>() }),
                ViewDescriptor(route = "configuration", title = "Configuration", factory = { koin.get<ConfigurationPlaceholderView>() }),
                ViewDescriptor(route = "patterns", title = "Pattern Analytics", factory = { koin.get<PatternAnalyticsPlaceholderView>() })
            )
        )
    }
}

fun main(args: Array<String>) {
    launch<DesktopApp>(*args)
}

