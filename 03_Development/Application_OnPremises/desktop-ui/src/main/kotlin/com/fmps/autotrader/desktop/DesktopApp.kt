package com.fmps.autotrader.desktop

import com.fmps.autotrader.desktop.dashboard.DashboardView
import com.fmps.autotrader.desktop.di.desktopModule
import com.fmps.autotrader.desktop.i18n.Localization
import com.fmps.autotrader.desktop.navigation.NavigationService
import com.fmps.autotrader.desktop.navigation.ViewDescriptor
import com.fmps.autotrader.desktop.shell.ShellView
import com.fmps.autotrader.desktop.monitoring.MonitoringView
import com.fmps.autotrader.desktop.traders.TraderManagementView
import com.fmps.autotrader.desktop.config.ConfigurationView
import com.fmps.autotrader.desktop.patterns.PatternAnalyticsView
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
        try {
            // Clear navigation service if it was initialized
            if (::navigationService.isInitialized) {
                navigationService.clear()
            }
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
        
        try {
            // Only call super.stop() if the FX application is actually running
            // This prevents "No FX application found" errors during initialization failures
            super.stop()
        } catch (e: IllegalStateException) {
            // If FX application wasn't initialized, ignore the error
            // This can happen if the application failed during init() or start()
            if (e.message?.contains("No FX application found") != true) {
                throw e
            }
        }
        
        try {
            // Stop Koin if it was initialized
            if (::koinApp.isInitialized) {
                stopKoin()
            }
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }

    private fun registerNavigation() {
        val koin = koinApp.koin
        navigationService.registerAll(
            listOf(
                ViewDescriptor(route = "dashboard", title = "Overview", factory = { 
                    try {
                        koin.get<DashboardView>()
                    } catch (e: Exception) {
                        throw RuntimeException("Failed to create DashboardView: ${e.message}", e)
                    }
                }),
                ViewDescriptor(route = "traders", title = "AI Traders", factory = { 
                    try {
                        koin.get<TraderManagementView>()
                    } catch (e: Exception) {
                        throw RuntimeException("Failed to create TraderManagementView: ${e.message}", e)
                    }
                }),
                ViewDescriptor(route = "monitoring", title = "Monitoring", factory = { 
                    try {
                        println("🔍 Attempting to create MonitoringView...")
                        val view = koin.get<MonitoringView>()
                        println("✅ MonitoringView created successfully")
                        view
                    } catch (e: Exception) {
                        println("❌ Failed to create MonitoringView: ${e.message}")
                        e.printStackTrace()
                        throw RuntimeException("Failed to create MonitoringView: ${e.message}", e)
                    }
                }),
                ViewDescriptor(route = "configuration", title = "Configuration", factory = { 
                    try {
                        println("🔍 Attempting to create ConfigurationView...")
                        val view = koin.get<ConfigurationView>()
                        println("✅ ConfigurationView created successfully")
                        view
                    } catch (e: Exception) {
                        println("❌ Failed to create ConfigurationView: ${e.message}")
                        e.printStackTrace()
                        throw RuntimeException("Failed to create ConfigurationView: ${e.message}", e)
                    }
                }),
                ViewDescriptor(route = "patterns", title = "Pattern Analytics", factory = { 
                    try {
                        println("🔍 Attempting to create PatternAnalyticsView...")
                        val view = koin.get<PatternAnalyticsView>()
                        println("✅ PatternAnalyticsView created successfully")
                        view
                    } catch (e: Exception) {
                        println("❌ Failed to create PatternAnalyticsView: ${e.message}")
                        e.printStackTrace()
                        throw RuntimeException("Failed to create PatternAnalyticsView: ${e.message}", e)
                    }
                })
            )
        )
    }
}

fun main(args: Array<String>) {
    launch<DesktopApp>(*args)
}

