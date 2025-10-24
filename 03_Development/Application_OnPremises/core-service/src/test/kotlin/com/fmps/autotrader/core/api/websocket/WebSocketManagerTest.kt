package com.fmps.autotrader.core.api.websocket

import io.ktor.websocket.*
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * Tests for WebSocket Manager
 */
class WebSocketManagerTest {
    
    @BeforeEach
    fun setup() {
        // Note: In a real scenario, we'd need to clean up sessions
        // For now, we just test basic functionality
    }
    
    @Test
    fun `should return initial stats`() {
        val stats = WebSocketManager.getStats()
        
        assertNotNull(stats)
        assertTrue(stats.containsKey("totalConnections"))
        assertTrue(stats.containsKey("traderStatusSubscribers"))
        assertTrue(stats.containsKey("tradeSubscribers"))
        assertTrue(stats.containsKey("marketDataSubscribers"))
    }
    
    @Test
    fun `should register session for trader-status channel`() {
        val mockSession = mockk<WebSocketSession>(relaxed = true)
        val initialStats = WebSocketManager.getStats()
        val initialTotal = initialStats["totalConnections"] ?: 0
        
        WebSocketManager.registerSession("trader-status", mockSession)
        
        val newStats = WebSocketManager.getStats()
        val newTotal = newStats["totalConnections"] ?: 0
        
        assertEquals(initialTotal + 1, newTotal)
    }
    
    @Test
    fun `should register session for trades channel`() {
        val mockSession = mockk<WebSocketSession>(relaxed = true)
        val initialStats = WebSocketManager.getStats()
        val initialTotal = initialStats["totalConnections"] ?: 0
        
        WebSocketManager.registerSession("trades", mockSession)
        
        val newStats = WebSocketManager.getStats()
        val newTotal = newStats["totalConnections"] ?: 0
        
        assertEquals(initialTotal + 1, newTotal)
    }
    
    @Test
    fun `should register session for market-data channel`() {
        val mockSession = mockk<WebSocketSession>(relaxed = true)
        val initialStats = WebSocketManager.getStats()
        val initialTotal = initialStats["totalConnections"] ?: 0
        
        WebSocketManager.registerSession("market-data", mockSession)
        
        val newStats = WebSocketManager.getStats()
        val newTotal = newStats["totalConnections"] ?: 0
        
        assertEquals(initialTotal + 1, newTotal)
    }
    
    @Test
    fun `should unregister session`() {
        val mockSession = mockk<WebSocketSession>(relaxed = true)
        
        WebSocketManager.registerSession("trader-status", mockSession)
        val statsAfterRegister = WebSocketManager.getStats()
        val totalAfterRegister = statsAfterRegister["totalConnections"] ?: 0
        
        WebSocketManager.unregisterSession(mockSession)
        val statsAfterUnregister = WebSocketManager.getStats()
        val totalAfterUnregister = statsAfterUnregister["totalConnections"] ?: 0
        
        assertEquals(totalAfterRegister - 1, totalAfterUnregister)
    }
    
    @Test
    fun `should create WSMessage with correct structure`() {
        val message = WSMessage(
            type = "test",
            channel = "test-channel",
            data = "test-data"
        )
        
        assertEquals("test", message.type)
        assertEquals("test-channel", message.channel)
        assertEquals("test-data", message.data)
        assertTrue(message.timestamp > 0)
    }
    
    // Note: Broadcast tests require actual WebSocket sessions to test properly
    // The broadcast methods are tested through integration testing
}

