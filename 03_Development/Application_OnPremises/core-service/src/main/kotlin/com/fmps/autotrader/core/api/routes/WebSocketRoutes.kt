package com.fmps.autotrader.core.api.routes

import com.fmps.autotrader.core.api.websocket.WebSocketManager
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("WebSocketRoutes")
private val json = Json { prettyPrint = false }

/**
 * Configure WebSocket routes for real-time updates
 */
fun Routing.configureWebSocketRoutes() {
    
    // WebSocket endpoint for trader status updates
    webSocket("/ws/trader-status") {
        logger.info("New WebSocket connection: trader-status from ${call.request.local.remoteHost}")
        
        try {
            // Register session
            WebSocketManager.registerSession("trader-status", this)
            
            // Send welcome message
            send(Frame.Text(json.encodeToString(mapOf(
                "type" to "connection",
                "channel" to "trader-status",
                "message" to "Connected to trader status updates",
                "timestamp" to System.currentTimeMillis()
            ))))
            
            // Keep connection alive and handle incoming messages
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        logger.debug("Received message on trader-status: $text")
                        
                        // Echo back for ping/pong
                        if (text.contains("ping")) {
                            send(Frame.Text(json.encodeToString(mapOf(
                                "type" to "pong",
                                "timestamp" to System.currentTimeMillis()
                            ))))
                        }
                    }
                    is Frame.Close -> {
                        logger.info("WebSocket connection closed: trader-status")
                    }
                    else -> {
                        logger.debug("Received frame type: ${frame.frameType}")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error in trader-status WebSocket", e)
        } finally {
            WebSocketManager.unregisterSession(this)
            logger.info("WebSocket session ended: trader-status")
        }
    }
    
    // WebSocket endpoint for trade updates
    webSocket("/ws/trades") {
        logger.info("New WebSocket connection: trades from ${call.request.local.remoteHost}")
        
        try {
            // Register session
            WebSocketManager.registerSession("trades", this)
            
            // Send welcome message
            send(Frame.Text(json.encodeToString(mapOf(
                "type" to "connection",
                "channel" to "trades",
                "message" to "Connected to trade updates",
                "timestamp" to System.currentTimeMillis()
            ))))
            
            // Keep connection alive and handle incoming messages
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        logger.debug("Received message on trades: $text")
                        
                        // Echo back for ping/pong
                        if (text.contains("ping")) {
                            send(Frame.Text(json.encodeToString(mapOf(
                                "type" to "pong",
                                "timestamp" to System.currentTimeMillis()
                            ))))
                        }
                    }
                    is Frame.Close -> {
                        logger.info("WebSocket connection closed: trades")
                    }
                    else -> {
                        logger.debug("Received frame type: ${frame.frameType}")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error in trades WebSocket", e)
        } finally {
            WebSocketManager.unregisterSession(this)
            logger.info("WebSocket session ended: trades")
        }
    }
    
    // WebSocket endpoint for market data updates (placeholder)
    webSocket("/ws/market-data") {
        logger.info("New WebSocket connection: market-data from ${call.request.local.remoteHost}")
        
        try {
            // Register session
            WebSocketManager.registerSession("market-data", this)
            
            // Send welcome message
            send(Frame.Text(json.encodeToString(mapOf(
                "type" to "connection",
                "channel" to "market-data",
                "message" to "Connected to market data updates (placeholder)",
                "timestamp" to System.currentTimeMillis()
            ))))
            
            // Keep connection alive and handle incoming messages
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        logger.debug("Received message on market-data: $text")
                        
                        // Echo back for ping/pong
                        if (text.contains("ping")) {
                            send(Frame.Text(json.encodeToString(mapOf(
                                "type" to "pong",
                                "timestamp" to System.currentTimeMillis()
                            ))))
                        }
                    }
                    is Frame.Close -> {
                        logger.info("WebSocket connection closed: market-data")
                    }
                    else -> {
                        logger.debug("Received frame type: ${frame.frameType}")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error in market-data WebSocket", e)
        } finally {
            WebSocketManager.unregisterSession(this)
            logger.info("WebSocket session ended: market-data")
        }
    }
    
    // HTTP endpoint to get WebSocket stats
    get("/api/v1/websocket/stats") {
        call.respond(mapOf(
            "success" to true,
            "data" to WebSocketManager.getStats()
        ))
    }
}

