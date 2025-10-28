package com.fmps.autotrader.core.connectors.websocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * Manages WebSocket subscriptions and routes messages to appropriate callbacks.
 *
 * This class:
 * - Tracks active subscriptions
 * - Routes incoming messages to registered callbacks
 * - Handles subscription lifecycle
 * - Provides subscription metrics
 *
 * ## Thread Safety
 * This class is thread-safe and can be accessed from multiple coroutines concurrently.
 *
 * @since 1.0.0
 */
class SubscriptionManager {

    /**
     * Represents a subscription to a WebSocket channel.
     *
     * @param id Unique subscription ID
     * @param channel Channel/stream name
     * @param callback Function to call with received messages
     */
    data class Subscription(
        val id: String,
        val channel: String,
        val callback: suspend (WebSocketMessage) -> Unit
    )

    // Active subscriptions
    private val subscriptions = ConcurrentHashMap<String, Subscription>()

    // Channel to subscription mapping (for routing)
    private val channelSubscriptions = ConcurrentHashMap<String, MutableSet<String>>()

    // Metrics
    private val messagesRouted = AtomicLong(0)
    private val routingErrors = AtomicLong(0)

    // Coroutine scope for callback execution
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Adds a new subscription.
     *
     * @param id Unique subscription ID
     * @param channel Channel name
     * @param callback Function to call with messages
     * @throws IllegalArgumentException if subscription ID already exists
     */
    fun addSubscription(
        id: String,
        channel: String,
        callback: suspend (WebSocketMessage) -> Unit
    ) {
        require(!subscriptions.containsKey(id)) {
            "Subscription with ID '$id' already exists"
        }

        val subscription = Subscription(id, channel, callback)
        subscriptions[id] = subscription

        // Add to channel mapping
        channelSubscriptions.compute(channel) { _, existing ->
            (existing ?: mutableSetOf()).apply { add(id) }
        }

        logger.debug { "Added subscription: id=$id, channel=$channel" }
    }

    /**
     * Removes a subscription.
     *
     * @param id The subscription ID to remove
     * @return true if subscription was found and removed, false otherwise
     */
    fun removeSubscription(id: String): Boolean {
        val subscription = subscriptions.remove(id) ?: return false

        // Remove from channel mapping
        channelSubscriptions.compute(subscription.channel) { _, existing ->
            existing?.apply { remove(id) }?.takeIf { it.isNotEmpty() }
        }

        logger.debug { "Removed subscription: id=$id, channel=${subscription.channel}" }
        return true
    }

    /**
     * Gets a subscription by ID.
     *
     * @param id The subscription ID
     * @return The subscription, or null if not found
     */
    fun getSubscription(id: String): Subscription? {
        return subscriptions[id]
    }

    /**
     * Gets all subscriptions for a channel.
     *
     * @param channel The channel name
     * @return List of subscriptions for the channel
     */
    fun getSubscriptionsForChannel(channel: String): List<Subscription> {
        val subscriptionIds = channelSubscriptions[channel] ?: return emptyList()
        return subscriptionIds.mapNotNull { subscriptions[it] }
    }

    /**
     * Routes a message to all subscriptions for its channel.
     *
     * Callbacks are executed in parallel coroutines to avoid blocking.
     *
     * @param message The message to route
     */
    suspend fun routeMessage(message: WebSocketMessage) {
        val subscriptionsForChannel = getSubscriptionsForChannel(message.channel)

        if (subscriptionsForChannel.isEmpty()) {
            logger.trace { "No subscriptions found for channel: ${message.channel}" }
            return
        }

        messagesRouted.incrementAndGet()
        logger.trace { "Routing message to ${subscriptionsForChannel.size} subscription(s) for channel: ${message.channel}" }

        // Invoke callbacks in parallel
        for (subscription in subscriptionsForChannel) {
            scope.launch {
                try {
                    subscription.callback(message)
                } catch (e: Exception) {
                    logger.error(e) { 
                        "Error in subscription callback (id: ${subscription.id}, channel: ${message.channel})"
                    }
                    routingErrors.incrementAndGet()
                }
            }
        }
    }

    /**
     * Removes all subscriptions.
     */
    fun clear() {
        val count = subscriptions.size
        subscriptions.clear()
        channelSubscriptions.clear()
        logger.info { "Cleared all subscriptions ($count removed)" }
    }

    /**
     * Gets the number of active subscriptions.
     *
     * @return Number of subscriptions
     */
    fun getSubscriptionCount(): Int {
        return subscriptions.size
    }

    /**
     * Gets all subscription IDs.
     *
     * @return List of subscription IDs
     */
    fun getAllSubscriptionIds(): List<String> {
        return subscriptions.keys.toList()
    }

    /**
     * Gets all active channels.
     *
     * @return List of channel names
     */
    fun getAllChannels(): List<String> {
        return channelSubscriptions.keys.toList()
    }

    /**
     * Checks if a subscription exists.
     *
     * @param id The subscription ID
     * @return true if exists, false otherwise
     */
    fun hasSubscription(id: String): Boolean {
        return subscriptions.containsKey(id)
    }

    /**
     * Checks if any subscriptions exist for a channel.
     *
     * @param channel The channel name
     * @return true if subscriptions exist, false otherwise
     */
    fun hasSubscriptionsForChannel(channel: String): Boolean {
        return channelSubscriptions.containsKey(channel)
    }

    /**
     * Gets subscription manager metrics.
     *
     * @return Metrics map
     */
    fun getMetrics(): Map<String, Any> {
        return mapOf(
            "active_subscriptions" to subscriptions.size,
            "active_channels" to channelSubscriptions.size,
            "messages_routed" to messagesRouted.get(),
            "routing_errors" to routingErrors.get(),
            "subscriptions_per_channel" to channelSubscriptions.mapValues { it.value.size }
        )
    }

    /**
     * Resets metrics.
     */
    fun resetMetrics() {
        messagesRouted.set(0)
        routingErrors.set(0)
        logger.debug { "Subscription manager metrics reset" }
    }
}

