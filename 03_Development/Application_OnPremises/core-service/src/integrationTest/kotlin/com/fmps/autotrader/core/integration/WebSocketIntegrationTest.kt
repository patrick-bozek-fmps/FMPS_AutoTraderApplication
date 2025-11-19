package com.fmps.autotrader.core.integration

import com.fmps.autotrader.core.api.startApiServer
import com.fmps.autotrader.core.api.websocket.TelemetryHub
import com.fmps.autotrader.core.api.websocket.TelemetryServerMessage
import com.fmps.autotrader.core.telemetry.TelemetryChannel
import com.fmps.autotrader.core.telemetry.TelemetryCollector
import com.fmps.autotrader.core.traders.AITraderMetrics
import com.fmps.autotrader.core.traders.AITraderState
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.Duration

/**
 * Integration test for WebSocket telemetry communication
 * 
 * Tests:
 * - WebSocket connection establishment
 * - Channel subscription (trader.status, positions, market-data)
 * - Real-time message delivery
 * - Reconnection logic and heartbeat
 * - Message parsing and UI updates
 * 
 * **Tag:** @Tag("integration")
 */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class WebSocketIntegrationTest {
    
    private lateinit var server: ApplicationEngine
    private lateinit var wsClient: io.ktor.client.HttpClient
    private var serverPort: Int = 0
    private val json = Json { ignoreUnknownKeys = true }
    
    @BeforeAll
    fun setup() {
        println("=".repeat(70))
        println("WebSocket Integration Test - Setup")
        println("=".repeat(70))
        
        // Initialize test database
        TestUtilities.initTestDatabase("websocket-integration")
        
        // Start API server
        serverPort = TestUtilities.findAvailablePort()
        server = startApiServer(host = "127.0.0.1", port = serverPort, wait = false)
        
        // Wait for server to be ready
        runBlocking {
            TestUtilities.waitForServer(serverPort, maxWaitMs = 5000)
        }
        
        // Create WebSocket client
        wsClient = TestUtilities.createWebSocketClient()
        
        // Note: TelemetryCollector.reset() is internal, skip reset for integration tests
        
        println("✅ Test environment initialized")
        println("   Server running on port: $serverPort")
        println()
    }
    
    @AfterAll
    fun tearDown() {
        println("=".repeat(70))
        println("WebSocket Integration Test - Cleanup")
        println("=".repeat(70))
        
        // Close WebSocket client
        wsClient.close()
        
        // Stop server
        server.stop(gracePeriodMillis = 1000, timeoutMillis = 5000)
        
        // Cleanup database
        TestUtilities.cleanupTestDatabase("websocket-integration")
        
        println("✅ Test environment cleaned up")
    }
    
    /**
     * Helper to await a specific message type
     */
    private suspend fun DefaultClientWebSocketSession.awaitMessage(
        timeout: Duration = Duration.ofSeconds(5),
        predicate: (TelemetryServerMessage) -> Boolean
    ): TelemetryServerMessage? {
        var result: TelemetryServerMessage? = null
        withTimeout(timeout.toMillis()) {
            while (result == null) {
                val frame = incoming.receiveCatching().getOrNull() ?: break
                if (frame is Frame.Text) {
                    val message = json.decodeFromString(
                        TelemetryServerMessage.serializer(),
                        frame.readText()
                    )
                    if (predicate(message)) {
                        result = message
                    }
                }
            }
        }
        return result
    }
    
    @Test
    @Order(1)
    fun `should establish WebSocket connection`() = runBlocking {
        println("Test 1: Establish WebSocket connection")
        
        val clientId = "test-client-${System.currentTimeMillis()}"
        
        wsClient.webSocket("ws://127.0.0.1:$serverPort/ws/telemetry?clientId=$clientId") {
            // Send subscribe message
            send(Frame.Text("""{"action":"subscribe","channels":["trader-status"],"replay":false}"""))
            
            // Wait for welcome message
            val welcome = awaitMessage { it.type == "welcome" }
            assertNotNull(welcome, "Should receive welcome message")
            assertEquals("welcome", welcome!!.type)
            
            println("✅ WebSocket connection established")
        }
    }
    
    @Test
    @Order(2)
    fun `should subscribe to trader-status channel and receive events`() = runBlocking {
        println("Test 2: Subscribe to trader-status channel and receive events")
        
        val clientId = "subscribe-test-${System.currentTimeMillis()}"
        
        wsClient.webSocket("ws://127.0.0.1:$serverPort/ws/telemetry?clientId=$clientId") {
            // Subscribe to trader-status channel
            send(Frame.Text("""{"action":"subscribe","channels":["trader-status"],"replay":false}"""))
            
            // Wait for welcome message
            val welcome = awaitMessage { it.type == "welcome" }
            assertNotNull(welcome, "Should receive welcome message")
            
            // Wait for subscription confirmation
            delay(500)
            
            // Publish a trader status event
            TelemetryCollector.publishTraderStatus(
                traderId = "test-trader-1",
                name = "Test Trader",
                state = AITraderState.RUNNING,
                exchange = "BINANCE",
                symbol = "BTCUSDT",
                strategy = "TREND_FOLLOWING",
                reason = "TEST",
                metrics = AITraderMetrics.empty()
            )
            
            // Wait for event message
            val event = awaitMessage(Duration.ofSeconds(3)) { 
                it.type == "event" && it.channel == "trader-status" 
            }
            
            assertNotNull(event, "Should receive trader status event")
            assertEquals("event", event!!.type)
            assertEquals("trader-status", event.channel)
            assertNotNull(event.data, "Event data should not be null")
            
            println("✅ Trader status event received via WebSocket")
        }
    }
    
    @Test
    @Order(3)
    fun `should subscribe to multiple channels`() = runBlocking {
        println("Test 3: Subscribe to multiple channels")
        
        val clientId = "multi-channel-test-${System.currentTimeMillis()}"
        val receivedChannels = mutableSetOf<String>()
        
        wsClient.webSocket("ws://127.0.0.1:$serverPort/ws/telemetry?clientId=$clientId") {
            // Subscribe to multiple channels
            send(Frame.Text("""{"action":"subscribe","channels":["trader-status","positions","market-data"],"replay":false}"""))
            
            // Wait for welcome message
            val welcome = awaitMessage { it.type == "welcome" }
            assertNotNull(welcome, "Should receive welcome message")
            
            // Wait for subscription confirmation
            delay(500)
            
            // Verify client is subscribed to all channels
            val clients = TelemetryHub.clients()
            val client = clients.find { it.id == clientId }
            assertNotNull(client, "Client should be registered")
            assertTrue(
                client!!.subscribedChannels.containsAll(listOf("trader-status", "positions", "market-data")),
                "Client should be subscribed to all channels"
            )
            
            println("✅ Subscribed to multiple channels: ${client.subscribedChannels}")
        }
    }
    
    @Test
    @Order(4)
    fun `should receive heartbeat messages`() = runBlocking {
        println("Test 4: Receive heartbeat messages")
        
        val clientId = "heartbeat-test-${System.currentTimeMillis()}"
        var heartbeatReceived = false
        
        wsClient.webSocket("ws://127.0.0.1:$serverPort/ws/telemetry?clientId=$clientId") {
            // Subscribe
            send(Frame.Text("""{"action":"subscribe","channels":["trader-status"],"replay":false}"""))
            
            // Wait for welcome
            awaitMessage { it.type == "welcome" }
            
            // Wait for heartbeat (heartbeat interval is 15 seconds, wait up to 20 seconds)
            val heartbeat = awaitMessage(Duration.ofSeconds(20)) { 
                it.type == "heartbeat" 
            }
            
            assertNotNull(heartbeat, "Should receive heartbeat message")
            assertEquals("heartbeat", heartbeat!!.type)
            heartbeatReceived = true
            
            println("✅ Heartbeat received")
        }
        
        assertTrue(heartbeatReceived, "Heartbeat should have been received")
    }
    
    @Test
    @Order(5)
    fun `should handle reconnection gracefully`() = runBlocking {
        println("Test 5: Handle reconnection gracefully")
        
        val clientId = "reconnect-test-${System.currentTimeMillis()}"
        
        // First connection
        wsClient.webSocket("ws://127.0.0.1:$serverPort/ws/telemetry?clientId=$clientId") {
            send(Frame.Text("""{"action":"subscribe","channels":["trader-status"],"replay":false}"""))
            awaitMessage { it.type == "welcome" }
        }
        
        delay(500)
        
        // Reconnect with same client ID
        wsClient.webSocket("ws://127.0.0.1:$serverPort/ws/telemetry?clientId=$clientId") {
            send(Frame.Text("""{"action":"subscribe","channels":["trader-status"],"replay":false}"""))
            val welcome = awaitMessage { it.type == "welcome" }
            assertNotNull(welcome, "Should receive welcome message on reconnect")
        }
        
        println("✅ Reconnection handled gracefully")
    }
    
    @Test
    @Order(6)
    fun `should receive replay events when requested`() = runBlocking {
        println("Test 6: Receive replay events when requested")
        
        // Publish some events first
        TelemetryCollector.publishTraderStatus(
            traderId = "replay-trader-1",
            name = "Replay Trader 1",
            state = AITraderState.IDLE,
            exchange = "BINANCE",
            symbol = "BTCUSDT",
            strategy = "TREND_FOLLOWING",
            reason = "REPLAY_TEST",
            metrics = AITraderMetrics.empty()
        )
        
        // Wait for event to be collected
        delay(1000)
        
        val clientId = "replay-test-${System.currentTimeMillis()}"
        val replayEvents = mutableListOf<TelemetryServerMessage>()
        
        wsClient.webSocket("ws://127.0.0.1:$serverPort/ws/telemetry?clientId=$clientId") {
            // Subscribe with replay enabled
            send(Frame.Text("""{"action":"subscribe","channels":["trader-status"],"replay":true}"""))
            
            // Wait for welcome
            awaitMessage { it.type == "welcome" }
            
            // Collect replay events (replay events are sent immediately after subscription)
            // Read all available messages for up to 2 seconds
            try {
                withTimeout(Duration.ofSeconds(2).toMillis()) {
                    repeat(20) { // Try up to 20 times
                        val frame = incoming.receiveCatching().getOrNull()
                        if (frame == null) {
                            delay(100) // Wait a bit before next attempt
                            return@repeat
                        }
                        if (frame is Frame.Text) {
                            val message = json.decodeFromString(
                                TelemetryServerMessage.serializer(),
                                frame.readText()
                            )
                            if (message.type == "event" && message.replay == true) {
                                replayEvents.add(message)
                            }
                        }
                        delay(50) // Small delay between reads
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                // Timeout is expected if no more messages
            }
        }
        
        // Should have received at least one replay event
        assertTrue(replayEvents.isNotEmpty(), "Should receive replay events (received: ${replayEvents.size})")
        
        println("✅ Received ${replayEvents.size} replay events")
    }
    
    @Test
    @Order(7)
    fun `should handle unsubscribe action`() = runBlocking {
        println("Test 7: Handle unsubscribe action")
        
        val clientId = "unsubscribe-test-${System.currentTimeMillis()}"
        
        wsClient.webSocket("ws://127.0.0.1:$serverPort/ws/telemetry?clientId=$clientId") {
            // Subscribe
            send(Frame.Text("""{"action":"subscribe","channels":["trader-status","positions"],"replay":false}"""))
            awaitMessage { it.type == "welcome" }
            delay(500)
            
            // Verify subscribed
            var clients = TelemetryHub.clients()
            var client = clients.find { it.id == clientId }
            assertTrue(
                client!!.subscribedChannels.containsAll(listOf("trader-status", "positions")),
                "Should be subscribed to both channels"
            )
            
            // Unsubscribe from one channel
            send(Frame.Text("""{"action":"unsubscribe","channels":["positions"]}"""))
            delay(500)
            
            // Verify unsubscribed
            clients = TelemetryHub.clients()
            client = clients.find { it.id == clientId }
            assertTrue(
                client!!.subscribedChannels.contains("trader-status"),
                "Should still be subscribed to trader-status"
            )
            assertFalse(
                client.subscribedChannels.contains("positions"),
                "Should be unsubscribed from positions"
            )
            
            println("✅ Unsubscribe action handled correctly")
        }
    }
}

