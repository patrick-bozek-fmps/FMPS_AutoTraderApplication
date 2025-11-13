package com.fmps.autotrader.desktop.navigation

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tornadofx.UIComponent

class NavigationServiceTest {

    @Test
    fun `register and navigate sets current view`() {
        val service = NavigationService()
        service.register(ViewDescriptor(route = "home", title = "Home", factory = { fakeView() }))

        service.navigate("home")

        val current = service.currentViewProperty.get()
        assertNotNull(current)
        assertEquals("Home", current!!.title)
    }

    @Test
    fun `go back restores previous view`() {
        val service = NavigationService()
        service.register(ViewDescriptor(route = "home", title = "Home", factory = { fakeView("Home") }))
        service.register(ViewDescriptor(route = "settings", title = "Settings", factory = { fakeView("Settings") }))

        service.navigate("home")
        service.navigate("settings")
        service.goBack()

        val current = service.currentViewProperty.get()
        assertEquals("Home", current!!.title)
    }

    @Test
    fun `duplicate route registration throws`() {
        val service = NavigationService()
        service.register(ViewDescriptor(route = "home", title = "Home", factory = { fakeView() }))

        assertThrows(IllegalArgumentException::class.java) {
            service.register(ViewDescriptor(route = "home", title = "Duplicate", factory = { fakeView() }))
        }
    }

    private fun fakeView(title: String = "Home"): UIComponent {
        val component = mockk<UIComponent>(relaxed = true)
        every { component.title } returns title
        return component
    }
}

