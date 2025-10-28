package com.fmps.autotrader.core.connectors.websocket

import com.fmps.autotrader.core.connectors.exceptions.ConnectionException
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * Abstract base class for managing WebSocket connections to exchanges.
 *
 * This class provides:
 * - WebSocket connection management
 * - Subscription tracking and lifecycle
 * - Automatic reconnection on disconnect
 * - Message parsing and routing to callbacks
 * - Ping/pong handling for keep-alive
 * - Connection health monitoring
 *
 * Subclasses must implement exchange-specific message parsing and subscription logic.
 *
 * ## Usage (Subclass Implementation)
 * ```kotlin
 * class BinanceWebSocketManager(httpClient: HttpClient, baseUrl: String) :
 *     WebSocketManager(httpClient, baseUrl) {
 *
 *     override suspend fun parseMessage(message: String): WebSocketMessage? {
 *         // Parse Binance-specific JSON
 *     }
 *
 *     override suspend fun buildSubscriptionMessage(channel: String, params: Map<String, Any>): String {
 *         // Build Binance-specific subscription message
 *     }
 * }
 * ```
 *
 * @param httpClient The HTTP client with WebSocket support
 * @param baseUrl The WebSocket base URL (e.g., "wss://stream.binance.com:9443/ws")
 *
 * @since 1.0.0
 */
abstract class WebSocketManager(
    protected val httpClient: HttpClient,
    protected val baseUrl: String
) {

    // Connection state
    private val connected = AtomicBoolean(false)
    private var session: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Subscription management
    protected val subscriptionManager = SubscriptionManager()

    // Metrics
    private val messagesReceived = AtomicLong(0)
    private val messagesSent = AtomicLong(0)
    private val errors = AtomicLong(0)
    private val reconnectAttempts = AtomicLong(0)

    // Reconnection settings
    private val autoReconnect = true
    private val maxReconnectAttempts = 5
    private val reconnectDelayMs = 5000L

    /**
     * Connects to the WebSocket endpoint.
     *
     * @param path Optional path to append to base URL
     * @throws ConnectionException if connection fails
     */
    suspend fun connect(path: String = "") {
        require(!connected.get()) { "Already connected" }

        val url = if (path.isEmpty()) baseUrl else "$baseUrl/$path"
        logger.info { "Connecting to WebSocket: $url" }

        try {
            connectionJob = scope.launch {
                connectWithRetry(url)
            }

            // Wait a moment to ensure connection is established
            delay(1000)

            if (!connected.get()) {
                throw ConnectionException(
                    message = "Failed to establish WebSocket connection",
                    retryable = true
                )
            }

            logger.info { "✓ WebSocket connected: $url" }

        } catch (e: Exception) {
            logger.error(e) { "✗ WebSocket connection failed" }
            throw ConnectionException(
                message = "WebSocket connection failed: ${e.message}",
                cause = e,
                retryable = true
            )
        }
    }

    /**
     * Connects with automatic retry logic.
     */
    private suspend fun connectWithRetry(url: String) {
        var attempts = 0

        while (attempts < maxReconnectAttempts && !connected.get()) {
            try {
                if (attempts > 0) {
                    reconnectAttempts.incrementAndGet()
                    logger.info { "Reconnect attempt $attempts/$maxReconnectAttempts" }
                    delay(reconnectDelayMs)
                }

                // Establish WebSocket connection
                httpClient.webSocket(url) {
                    session = this
                    connected.set(true)
                    logger.info { "WebSocket session established" }

                    // Start message receiving loop
                    receiveMessages()
                }

                // Connection closed (will retry if autoReconnect is true)
                connected.set(false)
                session = null

                if (autoReconnect) {
                    logger.warn { "WebSocket disconnected. Reconnecting..." }
                    attempts++
                } else {
                    break
                }

            } catch (e: CancellationException) {
                logger.debug { "WebSocket connection cancelled" }
                connected.set(false)
                throw e

            } catch (e: Exception) {
                logger.error(e) { "WebSocket connection error (attempt ${attempts + 1})" }
                errors.incrementAndGet()
                connected.set(false)
                attempts++
            }
        }

        if (attempts >= maxReconnectAttempts) {
            logger.error { "Max reconnect attempts ($maxReconnectAttempts) reached. Giving up." }
        }
    }

    /**
     * Disconnects from the WebSocket.
     */
    suspend fun disconnect() {
        if (!connected.get()) {
            logger.debug { "WebSocket already disconnected" }
            return
        }

        logger.info { "Disconnecting WebSocket..." }

        try {
            // Cancel connection job
            connectionJob?.cancelAndJoin()

            // Close session
            session?.close(CloseReason(CloseReason.Codes.NORMAL, "Client disconnect"))
            session = null

            connected.set(false)
            logger.info { "✓ WebSocket disconnected" }

        } catch (e: Exception) {
            logger.error(e) { "Error during WebSocket disconnect" }
            connected.set(false)
        }
    }

    /**
     * Receives and processes messages from the WebSocket.
     */
    private suspend fun DefaultClientWebSocketSession.receiveMessages() {
        try {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        messagesReceived.incrementAndGet()
                        
                        logger.trace { "Received message: ${text.take(200)}" }
                        
                        try {
                            handleMessage(text)
                        } catch (e: Exception) {
                            logger.error(e) { "Error handling message: ${text.take(100)}" }
                            errors.incrementAndGet()
                        }
                    }

                    is Frame.Binary -> {
                        logger.debug { "Received binary frame (${frame.data.size} bytes)" }
                        // Most crypto exchanges use text frames, but binary can be supported if needed
                    }

                    is Frame.Ping -> {
                        logger.trace { "Received ping, sending pong" }
                        send(Frame.Pong(frame.data))
                    }

                    is Frame.Pong -> {
                        logger.trace { "Received pong" }
                    }

                    is Frame.Close -> {
                        val reason = frame.readReason()
                        logger.info { "WebSocket closed: ${reason?.code} - ${reason?.message}" }
                    }

                    else -> {
                        logger.debug { "Received unknown frame type: ${frame.frameType}" }
                    }
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            logger.info { "WebSocket channel closed" }
        } catch (e: CancellationException) {
            logger.debug { "Message receiving cancelled" }
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error in message receiving loop" }
            errors.incrementAndGet()
        }
    }

    /**
     * Handles an incoming message.
     */
    private suspend fun handleMessage(text: String) {
        // Parse message
        val message = parseMessage(text) ?: return

        // Route message to appropriate subscription
        subscriptionManager.routeMessage(message)
    }

    /**
     * Sends a message through the WebSocket.
     *
     * @param message The message to send
     * @throws ConnectionException if not connected
     */
    suspend fun send(message: String) {
        val currentSession = session
            ?: throw ConnectionException("WebSocket not connected", retryable = true)

        try {
            currentSession.send(Frame.Text(message))
            messagesSent.incrementAndGet()
            logger.trace { "Sent message: ${message.take(200)}" }

        } catch (e: Exception) {
            logger.error(e) { "Failed to send message" }
            errors.incrementAndGet()
            throw ConnectionException(
                message = "Failed to send WebSocket message: ${e.message}",
                cause = e,
                retryable = true
            )
        }
    }

    /**
     * Checks if WebSocket is connected.
     *
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean = connected.get()

    /**
     * Subscribes to a WebSocket channel.
     *
     * @param subscriptionId Unique subscription ID
     * @param channel Channel/stream name
     * @param params Subscription parameters
     * @param callback Function to call with received data
     */
    suspend fun subscribe(
        subscriptionId: String,
        channel: String,
        params: Map<String, Any> = emptyMap(),
        callback: suspend (WebSocketMessage) -> Unit
    ) {
        logger.info { "Subscribing to channel: $channel (id: $subscriptionId)" }

        // Register subscription
        subscriptionManager.addSubscription(subscriptionId, channel, callback)

        // Send subscription message
        val subscribeMessage = buildSubscriptionMessage(channel, params)
        send(subscribeMessage)

        logger.info { "✓ Subscribed to $channel" }
    }

    /**
     * Unsubscribes from a WebSocket channel.
     *
     * @param subscriptionId The subscription ID to unsubscribe
     */
    suspend fun unsubscribe(subscriptionId: String) {
        val subscription = subscriptionManager.getSubscription(subscriptionId)
        if (subscription == null) {
            logger.warn { "Subscription not found: $subscriptionId" }
            return
        }

        logger.info { "Unsubscribing from: ${subscription.channel}" }

        // Send unsubscribe message if needed
        val unsubscribeMessage = buildUnsubscriptionMessage(subscription.channel)
        if (unsubscribeMessage != null) {
            send(unsubscribeMessage)
        }

        // Remove subscription
        subscriptionManager.removeSubscription(subscriptionId)

        logger.info { "✓ Unsubscribed from ${subscription.channel}" }
    }

    /**
     * Gets WebSocket metrics.
     *
     * @return Metrics map
     */
    fun getMetrics(): Map<String, Any> {
        return mapOf(
            "connected" to isConnected(),
            "messages_received" to messagesReceived.get(),
            "messages_sent" to messagesSent.get(),
            "errors" to errors.get(),
            "reconnect_attempts" to reconnectAttempts.get(),
            "active_subscriptions" to subscriptionManager.getSubscriptionCount()
        )
    }

    // ============================================
    // Abstract Methods (Exchange-Specific)
    // ============================================

    /**
     * Parses an incoming WebSocket message.
     *
     * Subclasses must implement this to parse exchange-specific message formats.
     *
     * @param message The raw message string
     * @return Parsed WebSocketMessage, or null if message should be ignored
     */
    protected abstract suspend fun parseMessage(message: String): WebSocketMessage?

    /**
     * Builds a subscription message for a channel.
     *
     * Subclasses must implement this to build exchange-specific subscription messages.
     *
     * @param channel The channel to subscribe to
     * @param params Subscription parameters
     * @return The subscription message string (usually JSON)
     */
    protected abstract suspend fun buildSubscriptionMessage(
        channel: String,
        params: Map<String, Any>
    ): String

    /**
     * Builds an unsubscription message for a channel.
     *
     * Subclasses can override this if the exchange requires explicit unsubscribe messages.
     * If null is returned, no unsubscribe message is sent.
     *
     * @param channel The channel to unsubscribe from
     * @return The unsubscription message string, or null if not needed
     */
    protected open suspend fun buildUnsubscriptionMessage(channel: String): String? {
        return null // Default: no explicit unsubscribe needed
    }
}

/**
 * Represents a parsed WebSocket message.
 *
 * @param channel The channel this message belongs to
 * @param data The parsed data (exchange-specific)
 */
data class WebSocketMessage(
    val channel: String,
    val data: Any
)

