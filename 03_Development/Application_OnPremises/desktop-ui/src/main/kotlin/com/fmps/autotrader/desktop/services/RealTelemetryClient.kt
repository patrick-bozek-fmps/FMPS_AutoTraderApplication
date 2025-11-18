package com.fmps.autotrader.desktop.services

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

/**
 * Real TelemetryClient implementation connecting to the core-service WebSocket endpoint.
 * 
 * Connects to `/ws/telemetry` and handles reconnection automatically.
 */
class RealTelemetryClient(
    private val httpClient: HttpClient,
    private val baseUrl: String = "http://localhost:8080",
    private val apiKey: String? = null
) : TelemetryClient {

    private val samples = MutableSharedFlow<TelemetrySample>(extraBufferCapacity = 64)
    private val running = AtomicBoolean(false)
    private val connected = AtomicBoolean(false)
    private var connectionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val maxReconnectAttempts = 5
    private val reconnectDelayMs = 5000L

    override fun start() {
        if (running.getAndSet(true)) {
            logger.warn { "TelemetryClient already started" }
            return
        }
        
        connectionJob = scope.launch {
            connectWithRetry()
        }
    }

    override fun stop() {
        if (!running.getAndSet(false)) {
            return
        }
        
        connectionJob?.cancel()
        connectionJob = null
        connected.set(false)
        logger.info { "TelemetryClient stopped" }
    }

    override fun samples(): Flow<TelemetrySample> = samples.asSharedFlow()

    private suspend fun connectWithRetry() {
        var attempt = 0
        
        while (running.get() && attempt < maxReconnectAttempts) {
            try {
                val wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://")
                val fullUrl = "$wsUrl/ws/telemetry?channels=trader.status,risk.alert,system.warning&replay=false"
                
                logger.info { "Connecting to telemetry WebSocket (attempt ${attempt + 1}/$maxReconnectAttempts): $fullUrl" }
                
                httpClient.webSocket(fullUrl) {
                    connected.set(true)
                    attempt = 0 // Reset on successful connection
                    logger.info { "Telemetry WebSocket connected successfully" }
                    
                    try {
                        for (frame in incoming) {
                            if (!running.get()) break
                            
                            when (frame) {
                                is Frame.Text -> {
                                    val text = frame.readText()
                                    try {
                                        val sample = parseTelemetryMessage(text)
                                        samples.emit(sample)
                                    } catch (e: Exception) {
                                        logger.warn(e) { "Failed to parse telemetry message: $text" }
                                    }
                                }
                                is Frame.Close -> {
                                    logger.info { "Telemetry WebSocket closed: ${frame.readReason()}" }
                                    break
                                }
                                else -> {
                                    // Ignore other frame types
                                }
                            }
                        }
                    } catch (e: ClosedReceiveChannelException) {
                        logger.info { "Telemetry WebSocket channel closed normally" }
                    } catch (e: Exception) {
                        logger.error(e) { "Error reading telemetry WebSocket" }
                    } finally {
                        connected.set(false)
                    }
                }
            } catch (e: Exception) {
                attempt++
                connected.set(false)
                logger.warn(e) { "Telemetry WebSocket connection failed (attempt $attempt/$maxReconnectAttempts)" }
                
                if (attempt < maxReconnectAttempts && running.get()) {
                    delay(reconnectDelayMs)
                } else if (running.get()) {
                    logger.error { "Max reconnection attempts reached. Telemetry client will stop retrying." }
                    // Emit a disconnected sample to notify UI
                    samples.emit(TelemetrySample(
                        channel = "system.error",
                        payload = """{"type":"CONNECTION","message":"Telemetry connection failed after $maxReconnectAttempts attempts","timestamp":${System.currentTimeMillis()}}"""
                    ))
                }
            }
        }
    }

    private fun parseTelemetryMessage(text: String): TelemetrySample {
        // Try to parse as JSON to extract channel and payload
        return try {
            val json = Json.parseToJsonElement(text)
            val channel = when {
                json.toString().contains("\"channel\"") -> {
                    // Simple extraction - could be enhanced with proper JSON parsing
                    val match = Regex("\"channel\"\\s*:\\s*\"([^\"]+)\"").find(text)
                    match?.groupValues?.get(1) ?: "unknown"
                }
                else -> "unknown"
            }
            val payload = text // Keep full payload for backward compatibility
            TelemetrySample(channel = channel, payload = payload)
        } catch (e: Exception) {
            // Fallback: assume format is channel:payload or just use text as payload
            if (text.contains(":")) {
                val parts = text.split(":", limit = 2)
                TelemetrySample(channel = parts[0], payload = parts[1])
            } else {
                TelemetrySample(channel = "unknown", payload = text)
            }
        }
    }
    
    fun isConnected(): Boolean = connected.get()
}

