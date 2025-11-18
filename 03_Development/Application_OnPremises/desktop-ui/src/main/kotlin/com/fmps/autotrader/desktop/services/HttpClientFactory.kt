package com.fmps.autotrader.desktop.services

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
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
            engine {
                requestTimeout = 30_000
            }
        }
    }
}

