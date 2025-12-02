package com.fmps.autotrader.core.api.websocket

import com.fmps.autotrader.core.telemetry.TelemetryChannel
import com.fmps.autotrader.core.telemetry.TelemetryCollector
import com.fmps.autotrader.core.telemetry.TelemetryEvent
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.ApplicationConfigurationException
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import com.fmps.autotrader.core.api.plugins.prometheusRegistry

private val logger = KotlinLogging.logger {}

/**
 * Settings controlling the telemetry WebSocket behaviour.
 */
data class TelemetrySettings(
    val heartbeatInterval: Duration,
    val heartbeatTimeout: Duration,
    val rateLimitPerSecond: Int,
    val replayLimit: Int
) {
    companion object {
        fun load(environment: ApplicationEnvironment): TelemetrySettings {
            val config = environment.config
            val telemetryConfig = config.configOrNull("telemetry")
            
            // Use runtime config if available, otherwise fall back to static config
            val heartbeatIntervalSeconds = try {
                com.fmps.autotrader.core.config.RuntimeConfigManager.getTelemetryHeartbeatIntervalSeconds()
            } catch (e: Exception) {
                telemetryConfig?.propertyOrNull("heartbeatIntervalSeconds")?.getString()
                    ?.toLongOrNull()?.coerceAtLeast(5) ?: 15L
            }
            
            val heartbeatTimeoutSeconds = telemetryConfig?.propertyOrNull("heartbeatTimeoutSeconds")?.getString()
                ?.toLongOrNull()?.coerceAtLeast(heartbeatIntervalSeconds * 2) ?: heartbeatIntervalSeconds * 3
            val rateLimitPerSecond = telemetryConfig?.propertyOrNull("rateLimitPerSecond")?.getString()?.toIntOrNull()?.coerceAtLeast(10) ?: 60
            val replayLimit = telemetryConfig?.propertyOrNull("replayLimit")?.getString()?.toIntOrNull()?.coerceAtLeast(1) ?: 50

            return TelemetrySettings(
                heartbeatInterval = Duration.ofSeconds(heartbeatIntervalSeconds),
                heartbeatTimeout = Duration.ofSeconds(heartbeatTimeoutSeconds),
                rateLimitPerSecond = rateLimitPerSecond,
                replayLimit = replayLimit
            )
        }
    }
}

/**
 * Context describing an incoming telemetry connection.
 */
data class TelemetryConnectionContext(
    val clientId: String = UUID.randomUUID().toString(),
    val apiKey: String?,
    val initialChannels: Set<TelemetryChannel> = emptySet(),
    val replayOnConnect: Boolean = false,
    val remoteAddress: String?
)

@Serializable
data class TelemetryClientMessage(
    val action: String,
    val channels: List<String> = emptyList(),
    val replay: Boolean? = null
)

@Serializable
data class TelemetryServerMessage(
    val type: String,
    val channel: String? = null,
    val data: JsonElement? = null,
    val replay: Boolean? = null,
    val meta: Map<String, String>? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class TelemetryClientInfo(
    val id: String,
    val remoteAddress: String?,
    val subscribedChannels: List<String>,
    val connectedAtEpochMillis: Long,
    val lastHeartbeatEpochMillis: Long,
    val rateLimitPerSecond: Int,
    val authenticated: Boolean,
    val isOpen: Boolean
)

@Serializable
data class TelemetryMetricsSnapshot(
    val activeConnections: Int,
    val messagesSent: Long,
    val messagesDropped: Long,
    val messagesPerChannel: Map<String, Long>,
    val messagesDroppedPerChannel: Map<String, Long>
)

private data class ClientSession(
    val id: String,
    val session: WebSocketSession,
    val apiKey: String?,
    val remoteAddress: String?,
    val subscribedChannels: MutableSet<TelemetryChannel>,
    val rateTracker: RateTracker,
    val lastHeartbeat: AtomicLong,
    val connectedAt: Instant,
    val rateLimitPerSecond: Int,
    val sendMutex: Mutex = Mutex(),
    var heartbeatJob: Job? = null
)

private class RateTracker(private val limitPerSecond: Int) {
    private val counter = AtomicInteger(0)
    private val windowStart = AtomicLong(System.currentTimeMillis())

    fun tryAcquire(): Boolean {
        val now = System.currentTimeMillis()
        val start = windowStart.get()
        if (now - start >= 1000) {
            windowStart.set(now)
            counter.set(0)
        }
        return counter.incrementAndGet() <= limitPerSecond
    }
}

/**
 * Telemetry hub used by the WebSocket route to manage subscriptions and broadcast events.
 */
object TelemetryHub {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile
    private var settings: TelemetrySettings? = null

    private val sessions = ConcurrentHashMap<String, ClientSession>()

    private val activeConnections = AtomicInteger(0)
    private val messagesSent = AtomicLong(0)
    private val messagesDropped = AtomicLong(0)
    private val messagesPerChannel = ConcurrentHashMap<TelemetryChannel, AtomicLong>().apply {
        TelemetryChannel.values().forEach { channel -> this[channel] = AtomicLong(0) }
    }
    private val droppedPerChannel = ConcurrentHashMap<TelemetryChannel, AtomicLong>().apply {
        TelemetryChannel.values().forEach { channel -> this[channel] = AtomicLong(0) }
    }
    private val binderRegistered = AtomicBoolean(false)

    private val eventCollectorJob = scope.launch {
        TelemetryCollector.events().collect { event ->
            dispatchEvent(event)
        }
    }

    fun metrics(): TelemetryMetricsSnapshot = TelemetryMetricsSnapshot(
        activeConnections = activeConnections.get(),
        messagesSent = messagesSent.get(),
        messagesDropped = messagesDropped.get(),
        messagesPerChannel = messagesPerChannel.mapKeys { it.key.wireName }.mapValues { it.value.get() },
        messagesDroppedPerChannel = droppedPerChannel.mapKeys { it.key.wireName }.mapValues { it.value.get() }
    )

    suspend fun handleConnection(
        session: WebSocketSession,
        environment: ApplicationEnvironment,
        context: TelemetryConnectionContext
    ) {
        val telemetrySettings = settings ?: TelemetrySettings.load(environment).also { settings = it }
        ensureMetricsBound()

        val clientSession = registerSession(session, context, telemetrySettings)
        try {
            sendWelcome(clientSession, telemetrySettings)
            if (context.initialChannels.isNotEmpty()) {
                subscribe(clientSession, context.initialChannels, telemetrySettings, context.replayOnConnect)
            }

            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Text -> handleClientMessage(clientSession, telemetrySettings, frame.readText())
                    is Frame.Ping -> session.send(Frame.Pong(frame.data))
                    is Frame.Close -> break
                    else -> {
                        // ignore
                    }
                }
            }
        } catch (_: CancellationException) {
            // connection cancelled
        } catch (ex: Exception) {
            logger.error(ex) { "Telemetry session error for client=${clientSession.id}" }
        }
        unregisterSession(clientSession.id)
    }

    private suspend fun handleClientMessage(
        clientSession: ClientSession,
        telemetrySettings: TelemetrySettings,
        payload: String
    ) {
        clientSession.lastHeartbeat.set(System.currentTimeMillis())
        val message = runCatching { json.decodeFromString(TelemetryClientMessage.serializer(), payload) }
            .getOrElse {
                sendMessage(
                    clientSession,
                    TelemetryServerMessage(
                        type = "error",
                        meta = mapOf("reason" to "invalid_payload"),
                        timestamp = System.currentTimeMillis()
                    )
                )
                return
            }

        when (message.action.lowercase()) {
            "subscribe" -> {
                val channels = message.channels.mapNotNull { TelemetryChannel.fromWireName(it) }.toSet()
                if (channels.isEmpty()) {
                    sendMessage(
                        clientSession,
                        TelemetryServerMessage(
                            type = "error",
                            meta = mapOf("reason" to "invalid_channels"),
                            timestamp = System.currentTimeMillis()
                        )
                    )
                } else {
                    subscribe(clientSession, channels, telemetrySettings, message.replay == true)
                }
            }

            "unsubscribe" -> {
                val channels = message.channels.mapNotNull { TelemetryChannel.fromWireName(it) }.toSet()
                unsubscribe(clientSession, channels)
            }

            "heartbeat" -> {
                sendMessage(
                    clientSession,
                    TelemetryServerMessage(
                        type = "heartbeat",
                        meta = mapOf("direction" to "ack"),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            else -> {
                sendMessage(
                    clientSession,
                    TelemetryServerMessage(
                        type = "error",
                        meta = mapOf("reason" to "unknown_action"),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private suspend fun registerSession(
        session: WebSocketSession,
        context: TelemetryConnectionContext,
        settings: TelemetrySettings
    ): ClientSession {
        val client = ClientSession(
            id = context.clientId,
            session = session,
            apiKey = context.apiKey,
            remoteAddress = context.remoteAddress,
            subscribedChannels = context.initialChannels.toMutableSet(),
            rateTracker = RateTracker(settings.rateLimitPerSecond),
            lastHeartbeat = AtomicLong(System.currentTimeMillis()),
            connectedAt = Instant.now(),
            rateLimitPerSecond = settings.rateLimitPerSecond
        )
        val previous = sessions.put(client.id, client)
        previous?.let { unregisterSession(it.id) }
        activeConnections.incrementAndGet()

        client.heartbeatJob = scope.launch {
            runHeartbeatLoop(client, settings)
        }
        logger.info { "Telemetry client connected id=${client.id} remote=${client.remoteAddress}" }
        return client
    }

    private fun ensureMetricsBound() {
        if (binderRegistered.compareAndSet(false, true)) {
            TelemetryMetricsBinder.register(prometheusRegistry)
        }
    }

    private suspend fun runHeartbeatLoop(client: ClientSession, settings: TelemetrySettings) {
        val heartbeatIntervalMs = settings.heartbeatInterval.toMillis().coerceAtLeast(5000L)
        val timeoutMs = settings.heartbeatTimeout.toMillis().coerceAtLeast(heartbeatIntervalMs * 2)
        while (client.session.isOpen()) {
            delay(heartbeatIntervalMs)
            val elapsed = System.currentTimeMillis() - client.lastHeartbeat.get()
            if (elapsed > timeoutMs) {
                logger.warn { "Telemetry heartbeat timeout for client=${client.id}" }
                runCatching {
                    client.session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Heartbeat timeout"))
                }
                break
            }
            sendMessage(
                client,
                TelemetryServerMessage(
                    type = "heartbeat",
                    meta = mapOf("direction" to "server"),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun sendWelcome(client: ClientSession, settings: TelemetrySettings) {
        val message = TelemetryServerMessage(
            type = "welcome",
            meta = mapOf(
                "clientId" to client.id,
                "heartbeatIntervalSeconds" to settings.heartbeatInterval.seconds.toString(),
                "rateLimitPerSecond" to settings.rateLimitPerSecond.toString()
            ),
            timestamp = System.currentTimeMillis()
        )
        sendMessage(client, message)
    }

    private suspend fun subscribe(
        client: ClientSession,
        channels: Set<TelemetryChannel>,
        settings: TelemetrySettings,
        replay: Boolean
    ) {
        client.subscribedChannels.addAll(channels)
        sendMessage(
            client,
            TelemetryServerMessage(
                type = "ack",
                channel = "subscription",
                data = null,
                replay = replay,
                meta = mapOf(
                    "channels" to channels.joinToString { it.wireName }
                ),
                timestamp = System.currentTimeMillis()
            )
        )
        logger.info {
            "Telemetry subscribe client=${client.id} channels=${channels.joinToString { it.wireName }} replay=$replay"
        }

        if (replay) {
            channels.forEach { channel ->
                val snapshots = TelemetryCollector.snapshot(channel)
                    .takeLast(settings.replayLimit)
                snapshots.forEach { event ->
                    sendEvent(client, event, isReplay = true)
                }
            }
        }
    }

    private suspend fun unsubscribe(client: ClientSession, channels: Set<TelemetryChannel>) {
        if (channels.isEmpty()) return
        client.subscribedChannels.removeAll(channels)
        sendMessage(
            client,
            TelemetryServerMessage(
                type = "ack",
                channel = "unsubscribe",
                meta = mapOf("channels" to channels.joinToString { it.wireName }),
                timestamp = System.currentTimeMillis()
            )
        )
        logger.info {
            "Telemetry unsubscribe client=${client.id} channels=${channels.joinToString { it.wireName }}"
        }
    }

    private suspend fun dispatchEvent(event: TelemetryEvent) {
        val sessionsSnapshot = sessions.values.toList()
        for (client in sessionsSnapshot) {
            if (event.channel !in client.subscribedChannels) continue
            if (!client.rateTracker.tryAcquire()) {
                messagesDropped.incrementAndGet()
                 droppedPerChannel[event.channel]?.incrementAndGet()
                 TelemetryMetricsBinder.recordDropped(event.channel)
                continue
            }
            sendEvent(client, event, isReplay = false)
        }
    }

    private suspend fun sendEvent(client: ClientSession, event: TelemetryEvent, isReplay: Boolean) {
        val element = json.parseToJsonElement(json.encodeToString(TelemetryEvent.serializer(), event))
        val message = TelemetryServerMessage(
            type = "event",
            channel = event.channel.wireName,
            data = element,
            replay = if (isReplay) true else null,
            timestamp = System.currentTimeMillis()
        )
        sendMessage(client, message)
        messagesSent.incrementAndGet()
        messagesPerChannel[event.channel]?.incrementAndGet()
        TelemetryMetricsBinder.recordMessage(event.channel)
    }

    private suspend fun sendMessage(client: ClientSession, message: TelemetryServerMessage) {
        if (!client.session.isOpen()) return
        client.sendMutex.withLock {
            if (!client.session.isOpen()) return
            val payload = json.encodeToString(TelemetryServerMessage.serializer(), message)
            withContext(Dispatchers.IO) {
                runCatching {
                    client.session.send(Frame.Text(payload))
                }.onFailure { throwable ->
                    logger.debug { "Failed to send telemetry message to client=${client.id}: ${throwable.message}" }
                }
            }
        }
    }

    private suspend fun unregisterSession(clientId: String) {
        val client = sessions.remove(clientId) ?: return
        activeConnections.decrementAndGet()
        runCatching {
            client.heartbeatJob?.cancelAndJoin()
        }
        logger.info { "Telemetry client disconnected id=$clientId" }
    }

    suspend fun disconnectClient(clientId: String, reason: String = "Admin disconnect"): Boolean {
        val client = sessions[clientId] ?: return false
        logger.warn { "Telemetry client forced disconnect id=$clientId reason=$reason" }
        return runCatching {
            client.session.close(CloseReason(CloseReason.Codes.NORMAL, reason))
            client.heartbeatJob?.cancel()
            true
        }.getOrElse {
            logger.error(it) { "Failed to disconnect telemetry client id=$clientId" }
            false
        }
    }

    fun clients(): List<TelemetryClientInfo> {
        return sessions.values.map { client ->
            TelemetryClientInfo(
                id = client.id,
                remoteAddress = client.remoteAddress,
                subscribedChannels = client.subscribedChannels.map { it.wireName }.sorted(),
                connectedAtEpochMillis = client.connectedAt.toEpochMilli(),
                lastHeartbeatEpochMillis = client.lastHeartbeat.get(),
                rateLimitPerSecond = client.rateLimitPerSecond,
                authenticated = client.apiKey != null,
                isOpen = client.session.isOpen()
            )
        }
    }

    internal fun activeConnectionCount(): Int = activeConnections.get()
    internal fun messagesSentTotal(): Long = messagesSent.get()
    internal fun messagesDroppedTotal(): Long = messagesDropped.get()
    internal fun messagesSentForChannel(channel: TelemetryChannel): Long =
        messagesPerChannel[channel]?.get() ?: 0L
    internal fun messagesDroppedForChannel(channel: TelemetryChannel): Long =
        droppedPerChannel[channel]?.get() ?: 0L
}

private fun ApplicationConfig.configOrNull(path: String): ApplicationConfig? =
    try {
        config(path)
    } catch (_: ApplicationConfigurationException) {
        null
    }

private fun WebSocketSession.isOpen(): Boolean = !outgoing.isClosedForSend
