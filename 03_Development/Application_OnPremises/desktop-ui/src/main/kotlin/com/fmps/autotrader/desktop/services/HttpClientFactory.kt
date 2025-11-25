package com.fmps.autotrader.desktop.services

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Factory for creating configured HttpClient instances for core-service communication.
 */
object HttpClientFactory {
    fun create(baseUrl: String = "http://localhost:8080"): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = false
                })
            }
            install(Logging) {
                level = LogLevel.INFO
            }
            // Install WebSockets plugin for telemetry and real-time updates
            install(WebSockets) {
                pingInterval = 30_000 // 30 seconds
                maxFrameSize = Long.MAX_VALUE
            }
            engine {
                requestTimeout = 30_000
            }
        }
    }
}

