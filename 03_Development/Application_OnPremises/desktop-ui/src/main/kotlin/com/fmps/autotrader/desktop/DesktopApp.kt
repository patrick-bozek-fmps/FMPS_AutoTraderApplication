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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javafx.animation.Timeline
import javafx.animation.KeyFrame
import javafx.util.Duration

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
        
        // Monitor system theme changes for AUTO theme mode
        setupSystemThemeListener()
    }
    
    private var systemThemeMonitor: Timeline? = null
    
    /**
     * Detects Windows system dark mode by reading the Windows registry.
     * Returns true if dark mode is enabled, false otherwise.
     */
    private fun detectWindowsDarkMode(): Boolean {
        return try {
            // Try JavaFX Platform.isDarkMode() first (JavaFX 17+)
            try {
                val darkModeMethod = javafx.application.Platform::class.java.getMethod("isDarkMode")
                val result = darkModeMethod.invoke(null) as? Boolean
                if (result != null) {
                    println("🔍 Using Platform.isDarkMode(): $result")
                    return result
                }
            } catch (e: NoSuchMethodException) {
                // Method not available, fall back to registry
            }
            
            // Fallback: Read Windows registry
            val process = ProcessBuilder(
                "reg", "query", 
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                "/v", "AppsUseLightTheme"
            ).start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                // If AppsUseLightTheme = 0, dark mode is enabled
                // If AppsUseLightTheme = 1, light mode is enabled
                val isLight = output.contains("AppsUseLightTheme") && output.contains("0x1")
                val isDark = !isLight
                println("🔍 Using Windows registry: isDark=$isDark (AppsUseLightTheme=${if (isLight) "1" else "0"})")
                isDark
            } else {
                // Fallback: assume dark mode if we can't detect
                println("⚠️ Could not read Windows registry, defaulting to dark mode")
                true
            }
        } catch (e: Exception) {
            println("⚠️ Error detecting Windows dark mode: ${e.message}, defaulting to dark")
            true // Default to dark mode
        }
    }
    
    private fun setupSystemThemeListener() {
        // Use a timer to periodically check system theme (JavaFX doesn't have a direct listener)
        // Check every 2 seconds if AUTO theme is active
        systemThemeMonitor = Timeline(
            KeyFrame(
                Duration.seconds(2.0),
                javafx.event.EventHandler {
                    javafx.application.Platform.runLater {
                        try {
                            if (!::koinApp.isInitialized) return@runLater
                            
                            val koin = koinApp.koin
                            val configService = koin.get<com.fmps.autotrader.desktop.services.ConfigService>()
                            
                            // Use coroutine to check theme asynchronously
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    val currentTheme = configService.configuration().first().general.theme
                                    println("🔍 Theme monitor: Current preference = $currentTheme")
                                    
                                    if (currentTheme == com.fmps.autotrader.desktop.services.ThemePreference.AUTO) {
                                        // Detect current system theme using Windows registry
                                        val isDark = detectWindowsDarkMode()
                                        val systemTheme = if (isDark) com.fmps.autotrader.desktop.services.ThemePreference.DARK else com.fmps.autotrader.desktop.services.ThemePreference.LIGHT
                                        println("🔍 Theme monitor: System theme = $systemTheme (isDark=$isDark)")
                                        
                                        // Check if we need to update the theme
                                        javafx.stage.Window.getWindows().forEach { window ->
                                            if (window is Stage) {
                                                window.scene?.root?.let { root ->
                                                    val currentAppliedTheme = when {
                                                        root.styleClass.contains("theme-light") -> com.fmps.autotrader.desktop.services.ThemePreference.LIGHT
                                                        root.styleClass.contains("theme-dark") -> com.fmps.autotrader.desktop.services.ThemePreference.DARK
                                                        else -> null
                                                    }
                                                    
                                                    println("🔍 Theme monitor: Applied theme = $currentAppliedTheme, System theme = $systemTheme")
                                                    
                                                    // If the applied theme doesn't match system theme, update it
                                                    if (currentAppliedTheme != systemTheme) {
                                                        println("🎨 System theme changed: $currentAppliedTheme -> $systemTheme, updating application theme")
                                                        applyThemePreference(com.fmps.autotrader.desktop.services.ThemePreference.AUTO, root)
                                                        
                                                        // Also trigger ShellView theme update via onDock if needed
                                                        // The ShellView.onDock() will handle applying theme to its root
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    println("⚠️ Error in theme monitoring: ${e.message}")
                                    e.printStackTrace()
                                }
                            }
                        } catch (e: Exception) {
                            println("⚠️ Error setting up theme monitoring: ${e.message}")
                        }
                    }
                }
            )
        ).apply {
            cycleCount = javafx.animation.Animation.INDEFINITE
            play()
            println("✅ System theme monitor started (checks every 2 seconds)")
        }
    }

    override fun start(stage: Stage) {
        stage.title = Localization.string("app.title", "FMPS AutoTrader Desktop")
        stage.minWidth = 1080.0
        stage.minHeight = 720.0
        
        // Load theme BEFORE showing the stage to avoid flash of wrong theme
        // Try reading config file directly first (faster than waiting for async service load)
        var initialTheme: com.fmps.autotrader.desktop.services.ThemePreference = com.fmps.autotrader.desktop.services.ThemePreference.DARK
        try {
            val configFilePath = System.getProperty("user.home") + java.io.File.separator + ".fmps-autotrader" + java.io.File.separator + "desktop-config.conf"
            val configFile = java.io.File(configFilePath)
            if (configFile.exists() && configFile.length() > 0) {
                val content = configFile.readText()
                // Parse theme directly from file
                var inGeneralSection = false
                for (line in content.lines()) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("general {")) {
                        inGeneralSection = true
                    } else if (trimmed == "}") {
                        inGeneralSection = false
                    } else if (inGeneralSection && trimmed.startsWith("theme")) {
                        val value = trimmed.substringAfter("=").trim().removeSurrounding("\"")
                        try {
                            initialTheme = com.fmps.autotrader.desktop.services.ThemePreference.valueOf(value.uppercase())
                            println("🎨 Loaded theme directly from file: $initialTheme")
                            break
                        } catch (e: Exception) {
                            println("⚠️ Invalid theme value in file: $value, using default")
                        }
                    }
                }
            } else {
                println("⚠️ Config file not found, using default theme")
            }
        } catch (e: Exception) {
            println("⚠️ Error reading config file directly, will try service: ${e.message}")
            // Fallback to service-based loading
            try {
                val koin = koinApp.koin
                val configService = koin.get<com.fmps.autotrader.desktop.services.ConfigService>()
                initialTheme = runBlocking {
                    var retries = 0
                    var theme: com.fmps.autotrader.desktop.services.ThemePreference? = null
                    while (retries < 10 && theme == null) {
                        kotlinx.coroutines.delay(300)
                        try {
                            val snapshot = configService.configuration().first()
                            theme = snapshot.general.theme
                            println("🎨 Successfully loaded theme from service on attempt ${retries + 1}: $theme")
                        } catch (e: Exception) {
                            retries++
                            if (retries < 10) {
                                println("⚠️ Attempt $retries failed to load theme from service, retrying...")
                            }
                        }
                    }
                    theme ?: com.fmps.autotrader.desktop.services.ThemePreference.DARK
                }
            } catch (e: Exception) {
                println("⚠️ Error loading theme from service, defaulting to dark: ${e.message}")
            }
        }
        
        // Handle AUTO theme by detecting system preference
        val actualTheme = when (initialTheme) {
            com.fmps.autotrader.desktop.services.ThemePreference.AUTO -> {
                val isDark = detectWindowsDarkMode()
                val resolved = if (isDark) com.fmps.autotrader.desktop.services.ThemePreference.DARK else com.fmps.autotrader.desktop.services.ThemePreference.LIGHT
                println("🎨 AUTO theme detected system preference: ${if (isDark) "DARK" else "LIGHT"} -> $resolved")
                resolved
            }
            else -> initialTheme
        }
        println("🎨 Final theme for startup: $initialTheme -> $actualTheme")
        
        super.start(stage)
        
        // Apply theme immediately after stage is created (before showing)
        // Use actualTheme (resolved AUTO) instead of initialTheme
        stage.scene?.root?.let { root ->
            applyThemePreference(actualTheme, root)
            println("🎨 Applied theme to root BEFORE show: $actualTheme (from $initialTheme), style classes: ${root.styleClass}")
        }
        
        // Also apply via Platform.runLater as backup
        javafx.application.Platform.runLater {
            stage.scene?.root?.let { root ->
                applyThemePreference(actualTheme, root)
                println("🎨 Applied theme to root AFTER show: $actualTheme (from $initialTheme), style classes: ${root.styleClass}")
            }
        }
        
        navigationService.navigate("dashboard")
    }
    
    private fun applyThemePreference(theme: com.fmps.autotrader.desktop.services.ThemePreference, root: javafx.scene.Node?) {
        if (root == null) return
        
        val actualTheme = when (theme) {
            com.fmps.autotrader.desktop.services.ThemePreference.AUTO -> {
                val isDark = detectWindowsDarkMode()
                if (isDark) com.fmps.autotrader.desktop.services.ThemePreference.DARK else com.fmps.autotrader.desktop.services.ThemePreference.LIGHT
            }
            else -> theme
        }
        
        println("🎨 applyThemePreference called: theme=$theme, actualTheme=$actualTheme, root=${root.javaClass.simpleName}")
        
        // Apply to the root node itself
        root.styleClass.removeAll(listOf("theme-light", "theme-dark"))
        when (actualTheme) {
            com.fmps.autotrader.desktop.services.ThemePreference.LIGHT -> {
                root.styleClass.add("theme-light")
                println("🎨 Added theme-light class to root. Style classes: ${root.styleClass}")
            }
            com.fmps.autotrader.desktop.services.ThemePreference.DARK -> {
                root.styleClass.add("theme-dark")
                println("🎨 Added theme-dark class to root. Style classes: ${root.styleClass}")
            }
            else -> {}
        }
        
        // Also apply to scene root if different
        if (root is javafx.scene.Parent) {
            root.scene?.root?.let { sceneRoot ->
                if (sceneRoot != root) {
                    sceneRoot.styleClass.removeAll(listOf("theme-light", "theme-dark"))
                    when (actualTheme) {
                        com.fmps.autotrader.desktop.services.ThemePreference.LIGHT -> {
                            sceneRoot.styleClass.add("theme-light")
                            println("🎨 Added theme-light class to scene root. Style classes: ${sceneRoot.styleClass}")
                        }
                        com.fmps.autotrader.desktop.services.ThemePreference.DARK -> {
                            sceneRoot.styleClass.add("theme-dark")
                            println("🎨 Added theme-dark class to scene root. Style classes: ${sceneRoot.styleClass}")
                        }
                        else -> {}
                    }
                }
            }
        }
        
        // Also apply to all windows/scenes
        javafx.stage.Window.getWindows().forEach { window ->
            if (window is Stage) {
                window.scene?.root?.let { sceneRoot ->
                    if (sceneRoot != root) {
                        sceneRoot.styleClass.removeAll(listOf("theme-light", "theme-dark"))
                        when (actualTheme) {
                            com.fmps.autotrader.desktop.services.ThemePreference.LIGHT -> sceneRoot.styleClass.add("theme-light")
                            com.fmps.autotrader.desktop.services.ThemePreference.DARK -> sceneRoot.styleClass.add("theme-dark")
                            else -> {}
                        }
                    }
                }
            }
        }
        
        // Also apply theme class to sidebar and nav-buttons if they exist
        // Find sidebar by traversing the scene graph
        if (root is javafx.scene.Parent) {
            root.lookupAll(".sidebar").forEach { sidebar ->
                sidebar.styleClass.removeAll(listOf("theme-light", "theme-dark"))
                when (actualTheme) {
                    com.fmps.autotrader.desktop.services.ThemePreference.LIGHT -> sidebar.styleClass.add("theme-light")
                    com.fmps.autotrader.desktop.services.ThemePreference.DARK -> sidebar.styleClass.add("theme-dark")
                    else -> {}
                }
                println("🎨 Applied theme class to sidebar: $actualTheme")
            }
            
            // Also apply to nav-buttons
            root.lookupAll(".nav-button").forEach { navButton ->
                navButton.styleClass.removeAll(listOf("theme-light", "theme-dark"))
                when (actualTheme) {
                    com.fmps.autotrader.desktop.services.ThemePreference.LIGHT -> navButton.styleClass.add("theme-light")
                    com.fmps.autotrader.desktop.services.ThemePreference.DARK -> navButton.styleClass.add("theme-dark")
                    else -> {}
                }
            }
        }
    }

    override fun stop() {
        try {
            // Stop theme monitoring
            systemThemeMonitor?.stop()
            systemThemeMonitor = null
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
        
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
                        println("🔍 Attempting to create TraderManagementView...")
                        val view = koin.get<TraderManagementView>()
                        println("✅ TraderManagementView created successfully")
                        view
                    } catch (e: Exception) {
                        println("❌ Failed to create TraderManagementView")
                        println("   Error type: ${e.javaClass.simpleName}")
                        println("   Error message: ${e.message}")
                        e.printStackTrace()
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

