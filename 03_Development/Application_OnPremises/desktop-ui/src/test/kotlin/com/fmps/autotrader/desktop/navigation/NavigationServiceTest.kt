package com.fmps.autotrader.desktop.navigation

import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.layout.StackPane
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import tornadofx.Fragment
import tornadofx.UIComponent

class NavigationServiceTest {

    @Test
    fun `register and navigate sets current view`() {
        val service = NavigationService()
        service.register(ViewDescriptor(route = "home", title = "Home", factory = { FakeView() }))

        service.navigate("home")

        val current = service.currentViewProperty.get()
        assertNotNull(current)
        assertEquals("Home", current!!.title)
    }

    @Test
    fun `go back restores previous view`() {
        val service = NavigationService()
        service.register(ViewDescriptor(route = "home", title = "Home", factory = { FakeView("Home") }))
        service.register(ViewDescriptor(route = "settings", title = "Settings", factory = { FakeView("Settings") }))

        service.navigate("home")
        service.navigate("settings")
        service.goBack()

        val current = service.currentViewProperty.get()
        assertEquals("Home", current!!.title)
    }

    @Test
    fun `duplicate route registration throws`() {
        val service = NavigationService()
        service.register(ViewDescriptor(route = "home", title = "Home", factory = { FakeView() }))

        assertThrows(IllegalArgumentException::class.java) {
            service.register(ViewDescriptor(route = "home", title = "Duplicate", factory = { FakeView() }))
        }
    }

    private class FakeView(title: String = "Home") : Fragment(title) {
        override val root: StackPane = StackPane()
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun initToolkit() {
            // Initialises the JavaFX toolkit for tests.
            JFXPanel()
            if (!Platform.isImplicitExit()) {
                Platform.setImplicitExit(false)
            }
        }
    }
}

