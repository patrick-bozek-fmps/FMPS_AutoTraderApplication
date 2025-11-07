package com.fmps.autotrader.core.connectors.websocket

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SubscriptionManagerTest {

    private lateinit var manager: SubscriptionManager

    @BeforeEach
    fun setup() {
        manager = SubscriptionManager()
    }

    @Test
    fun `test addSubscription creates new subscription`() {
        val callback: suspend (WebSocketMessage) -> Unit = { }

        manager.addSubscription(
            id = "sub1",
            channel = "channel1",
            callback = callback
        )

        assertTrue(manager.hasSubscription("sub1"))
        assertEquals(1, manager.getSubscriptionCount())
    }

    @Test
    fun `test addSubscription fails for duplicate ID`() {
        val callback: suspend (WebSocketMessage) -> Unit = { }

        manager.addSubscription("sub1", "channel1", callback)

        val exception = assertThrows<IllegalArgumentException> {
            manager.addSubscription("sub1", "channel2", callback)
        }

        assertTrue(exception.message!!.contains("already exists"))
    }

    @Test
    fun `test addSubscription allows same channel multiple times with different IDs`() {
        val callback: suspend (WebSocketMessage) -> Unit = { }

        manager.addSubscription("sub1", "channel1", callback)
        manager.addSubscription("sub2", "channel1", callback)

        assertEquals(2, manager.getSubscriptionCount())
        assertEquals(2, manager.getSubscriptionsForChannel("channel1").size)
    }

    @Test
    fun `test removeSubscription removes existing subscription`() {
        val callback: suspend (WebSocketMessage) -> Unit = { }
        manager.addSubscription("sub1", "channel1", callback)

        val removed = manager.removeSubscription("sub1")

        assertTrue(removed)
        assertFalse(manager.hasSubscription("sub1"))
        assertEquals(0, manager.getSubscriptionCount())
    }

    @Test
    fun `test removeSubscription returns false for non-existent subscription`() {
        val removed = manager.removeSubscription("non_existent")

        assertFalse(removed)
    }

    @Test
    fun `test getSubscription returns correct subscription`() {
        val callback: suspend (WebSocketMessage) -> Unit = { }
        manager.addSubscription("sub1", "channel1", callback)

        val subscription = manager.getSubscription("sub1")

        assertNotNull(subscription)
        assertEquals("sub1", subscription!!.id)
        assertEquals("channel1", subscription.channel)
    }

    @Test
    fun `test getSubscription returns null for non-existent subscription`() {
        val subscription = manager.getSubscription("non_existent")

        assertNull(subscription)
    }

    @Test
    fun `test getSubscriptionsForChannel returns all subscriptions`() {
        val callback: suspend (WebSocketMessage) -> Unit = { }

        manager.addSubscription("sub1", "channel1", callback)
        manager.addSubscription("sub2", "channel1", callback)
        manager.addSubscription("sub3", "channel2", callback)

        val channel1Subs = manager.getSubscriptionsForChannel("channel1")
        val channel2Subs = manager.getSubscriptionsForChannel("channel2")

        assertEquals(2, channel1Subs.size)
        assertEquals(1, channel2Subs.size)
    }

    @Test
    fun `test getSubscriptionsForChannel returns empty list for non-existent channel`() {
        val subscriptions = manager.getSubscriptionsForChannel("non_existent")

        assertTrue(subscriptions.isEmpty())
    }

    @Test
    fun `test routeMessage delivers to correct subscriptions`() = runBlocking {
        val receivedMessages = mutableListOf<WebSocketMessage>()
        val callback: suspend (WebSocketMessage) -> Unit = { msg ->
            receivedMessages.add(msg)
        }

        manager.addSubscription("sub1", "channel1", callback)
        manager.addSubscription("sub2", "channel1", callback)
        manager.addSubscription("sub3", "channel2", callback)

        val message1 = WebSocketMessage("channel1", "data1")
        val message2 = WebSocketMessage("channel2", "data2")

        manager.routeMessage(message1)
        manager.routeMessage(message2)

        try {
            withTimeout(500) {
                while (receivedMessages.size < 3) {
                    delay(10)
                }
            }
        } catch (ex: TimeoutCancellationException) {
            fail("Expected 3 delivered messages but received ${receivedMessages.size}")
        }

        // Message 1 should be delivered to sub1 and sub2
        // Message 2 should be delivered to sub3
        assertEquals(3, receivedMessages.size)
        assertTrue(receivedMessages.any { it.channel == "channel1" && it.data == "data1" })
        assertTrue(receivedMessages.any { it.channel == "channel2" && it.data == "data2" })
    }

    @Test
    fun `test routeMessage handles no subscriptions gracefully`() = runBlocking {
        val message = WebSocketMessage("non_existent_channel", "data")

        // Should not throw exception
        manager.routeMessage(message)

        val metrics = manager.getMetrics()
        assertEquals(0L, metrics["messages_routed"] as Long)
    }

    @Test
    fun `test routeMessage increments metrics`() = runBlocking {
        val callback: suspend (WebSocketMessage) -> Unit = { }
        manager.addSubscription("sub1", "channel1", callback)

        val message = WebSocketMessage("channel1", "data")
        manager.routeMessage(message)

        delay(100)  // Wait for routing

        val metrics = manager.getMetrics()
        assertEquals(1, metrics["messages_routed"] as Long)
    }

    @Test
    fun `test routeMessage handles callback exceptions`() = runBlocking {
        val callback: suspend (WebSocketMessage) -> Unit = {
            throw RuntimeException("Callback error")
        }

        manager.addSubscription("sub1", "channel1", callback)

        val message = WebSocketMessage("channel1", "data")
        
        // Should not throw exception even if callback fails
        manager.routeMessage(message)

        delay(200)  // Wait for error handling

        val metrics = manager.getMetrics()
        val routingErrors = metrics["routing_errors"] as Long
        assertTrue(routingErrors >= 1, "Should have recorded routing error")
    }

    @Test
    fun `test clear removes all subscriptions`() {
        val callback: suspend (WebSocketMessage) -> Unit = { }

        manager.addSubscription("sub1", "channel1", callback)
        manager.addSubscription("sub2", "channel2", callback)
        manager.addSubscription("sub3", "channel3", callback)

        assertEquals(3, manager.getSubscriptionCount())

        manager.clear()

        assertEquals(0, manager.getSubscriptionCount())
        assertTrue(manager.getAllSubscriptionIds().isEmpty())
        assertTrue(manager.getAllChannels().isEmpty())
    }

    @Test
    fun `test getSubscriptionCount returns correct count`() {
        val callback: suspend (WebSocketMessage) -> Unit = { }

        assertEquals(0, manager.getSubscriptionCount())

        manager.addSubscription("sub1", "channel1", callback)
        assertEquals(1, manager.getSubscriptionCount())

        manager.addSubscription("sub2", "channel2", callback)
        assertEquals(2, manager.getSubscriptionCount())

        manager.removeSubscription("sub1")
        assertEquals(1, manager.getSubscriptionCount())
    }

    @Test
    fun `test getAllSubscriptionIds returns all IDs`() {
        val callback: suspend (WebSocketMessage) -> Unit = { }

        manager.addSubscription("sub1", "channel1", callback)
        manager.addSubscription("sub2", "channel2", callback)
        manager.addSubscription("sub3", "channel3", callback)

        val ids = manager.getAllSubscriptionIds()

        assertEquals(3, ids.size)
        assertTrue(ids.contains("sub1"))
        assertTrue(ids.contains("sub2"))
        assertTrue(ids.contains("sub3"))
    }

    @Test
    fun `test getAllChannels returns all unique channels`() {
        val callback: suspend (WebSocketMessage) -> Unit = { }

        manager.addSubscription("sub1", "channel1", callback)
        manager.addSubscription("sub2", "channel1", callback)
        manager.addSubscription("sub3", "channel2", callback)

        val channels = manager.getAllChannels()

        assertEquals(2, channels.size)
        assertTrue(channels.contains("channel1"))
        assertTrue(channels.contains("channel2"))
    }

    @Test
    fun `test hasSubscription checks existence correctly`() {
        val callback: suspend (WebSocketMessage) -> Unit = { }

        assertFalse(manager.hasSubscription("sub1"))

        manager.addSubscription("sub1", "channel1", callback)
        assertTrue(manager.hasSubscription("sub1"))

        manager.removeSubscription("sub1")
        assertFalse(manager.hasSubscription("sub1"))
    }

    @Test
    fun `test hasSubscriptionsForChannel checks existence correctly`() {
        val callback: suspend (WebSocketMessage) -> Unit = { }

        assertFalse(manager.hasSubscriptionsForChannel("channel1"))

        manager.addSubscription("sub1", "channel1", callback)
        assertTrue(manager.hasSubscriptionsForChannel("channel1"))

        manager.removeSubscription("sub1")
        assertFalse(manager.hasSubscriptionsForChannel("channel1"))
    }

    @Test
    fun `test hasSubscriptionsForChannel with multiple subscriptions`() {
        val callback: suspend (WebSocketMessage) -> Unit = { }

        manager.addSubscription("sub1", "channel1", callback)
        manager.addSubscription("sub2", "channel1", callback)

        assertTrue(manager.hasSubscriptionsForChannel("channel1"))

        // Remove one subscription - channel should still have subscriptions
        manager.removeSubscription("sub1")
        assertTrue(manager.hasSubscriptionsForChannel("channel1"))

        // Remove last subscription - channel should be gone
        manager.removeSubscription("sub2")
        assertFalse(manager.hasSubscriptionsForChannel("channel1"))
    }

    @Test
    fun `test getMetrics returns all expected fields`() {
        val metrics = manager.getMetrics()

        assertNotNull(metrics["active_subscriptions"])
        assertNotNull(metrics["active_channels"])
        assertNotNull(metrics["messages_routed"])
        assertNotNull(metrics["routing_errors"])
        assertNotNull(metrics["subscriptions_per_channel"])
    }

    @Test
    fun `test getMetrics shows correct counts`() {
        val callback: suspend (WebSocketMessage) -> Unit = { }

        manager.addSubscription("sub1", "channel1", callback)
        manager.addSubscription("sub2", "channel1", callback)
        manager.addSubscription("sub3", "channel2", callback)

        val metrics = manager.getMetrics()

        assertEquals(3, metrics["active_subscriptions"])
        assertEquals(2, metrics["active_channels"])
        
        @Suppress("UNCHECKED_CAST")
        val subsPerChannel = metrics["subscriptions_per_channel"] as Map<String, Int>
        assertEquals(2, subsPerChannel["channel1"])
        assertEquals(1, subsPerChannel["channel2"])
    }

    @Test
    fun `test resetMetrics clears counters`() = runBlocking {
        val callback: suspend (WebSocketMessage) -> Unit = { }
        manager.addSubscription("sub1", "channel1", callback)

        val message = WebSocketMessage("channel1", "data")
        manager.routeMessage(message)
        delay(100)

        var metrics = manager.getMetrics()
        val messagesRoutedBefore = metrics["messages_routed"] as Long
        assertTrue(messagesRoutedBefore > 0)

        manager.resetMetrics()

        metrics = manager.getMetrics()
        assertEquals(0L, metrics["messages_routed"])
        assertEquals(0L, metrics["routing_errors"])
    }

    @Test
    fun `test concurrent subscription management`() {
        val callback: suspend (WebSocketMessage) -> Unit = { }

        // Add subscriptions concurrently
        val ids = (1..100).map { "sub$it" }
        ids.forEach { id ->
            manager.addSubscription(id, "channel1", callback)
        }

        assertEquals(100, manager.getSubscriptionCount())
        assertEquals(100, manager.getSubscriptionsForChannel("channel1").size)
    }

    @Test
    fun `test message routing with complex data types`() = runBlocking {
        val receivedData = mutableListOf<Any>()
        val callback: suspend (WebSocketMessage) -> Unit = { msg ->
            receivedData.add(msg.data)
        }

        manager.addSubscription("sub1", "channel1", callback)

        // Test with different data types
        manager.routeMessage(WebSocketMessage("channel1", "string"))
        manager.routeMessage(WebSocketMessage("channel1", 123))
        manager.routeMessage(WebSocketMessage("channel1", mapOf("key" to "value")))

        delay(200)

        assertEquals(3, receivedData.size)
        assertTrue(receivedData.contains("string"))
        assertTrue(receivedData.contains(123))
    }

    @Test
    fun `test subscription callback receives correct data`() = runBlocking {
        var receivedChannel: String? = null
        var receivedData: Any? = null

        val callback: suspend (WebSocketMessage) -> Unit = { msg ->
            receivedChannel = msg.channel
            receivedData = msg.data
        }

        manager.addSubscription("sub1", "test_channel", callback)

        val message = WebSocketMessage("test_channel", "test_data")
        manager.routeMessage(message)

        delay(100)

        assertEquals("test_channel", receivedChannel)
        assertEquals("test_data", receivedData)
    }
}

