package com.fmps.autotrader.core.api.plugins

import io.ktor.server.application.*
import io.ktor.server.websocket.*
import java.time.Duration

/**
 * Configure WebSocket support for real-time updates
 */
fun Application.configureWebSockets() {
    install(WebSockets) {
        // Ping period - send ping frame every 15 seconds
        pingPeriod = Duration.ofSeconds(15)
        
        // Timeout period - close connection if no ping/pong in 15 seconds
        timeout = Duration.ofSeconds(15)
        
        // Maximum frame size in bytes
        maxFrameSize = Long.MAX_VALUE
        
        // Maximum messages to be queued before starting to drop them
        masking = false
    }
}

