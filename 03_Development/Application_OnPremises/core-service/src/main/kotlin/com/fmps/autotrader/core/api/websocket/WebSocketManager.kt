package com.fmps.autotrader.core.api.websocket

import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * WebSocket message types
 */
@Serializable
data class WSMessage(
    val type: String,
    val channel: String,
    val data: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Manages WebSocket connections and broadcasts
 */
object WebSocketManager {
    private val logger = LoggerFactory.getLogger(WebSocketManager::class.java)
    
    // Track connections by channel
    private val traderStatusSessions = ConcurrentHashMap.newKeySet<WebSocketSession>()
    private val tradeSessions = ConcurrentHashMap.newKeySet<WebSocketSession>()
    private val marketDataSessions = ConcurrentHashMap.newKeySet<WebSocketSession>()
    
    private val connectionCount = AtomicInteger(0)
    private val json = Json { prettyPrint = false }
    
    /**
     * Register a new WebSocket session for a specific channel
     */
    fun registerSession(channel: String, session: WebSocketSession) {
        when (channel) {
            "trader-status" -> traderStatusSessions.add(session)
            "trades" -> tradeSessions.add(session)
            "market-data" -> marketDataSessions.add(session)
            else -> {
                logger.warn("Unknown channel: $channel")
                return
            }
        }
        
        connectionCount.incrementAndGet()
        logger.info("WebSocket session registered for channel '$channel'. Total connections: ${connectionCount.get()}")
    }
    
    /**
     * Unregister a WebSocket session from all channels
     */
    fun unregisterSession(session: WebSocketSession) {
        val removed = traderStatusSessions.remove(session) ||
                      tradeSessions.remove(session) ||
                      marketDataSessions.remove(session)
        
        if (removed) {
            connectionCount.decrementAndGet()
            logger.info("WebSocket session unregistered. Total connections: ${connectionCount.get()}")
        }
    }
    
    /**
     * Broadcast trader status update to all subscribed clients
     */
    suspend fun broadcastTraderStatus(traderId: Long, status: String, data: Map<String, Any>) {
        val message = WSMessage(
            type = "trader-status-update",
            channel = "trader-status",
            data = json.encodeToString(mapOf(
                "traderId" to traderId,
                "status" to status,
                "details" to data
            ))
        )
        
        broadcast(traderStatusSessions, message)
    }
    
    /**
     * Broadcast trade update to all subscribed clients
     */
    suspend fun broadcastTradeUpdate(tradeId: Long, action: String, data: Map<String, Any>) {
        val message = WSMessage(
            type = "trade-update",
            channel = "trades",
            data = json.encodeToString(mapOf(
                "tradeId" to tradeId,
                "action" to action,
                "details" to data
            ))
        )
        
        broadcast(tradeSessions, message)
    }
    
    /**
     * Broadcast market data update to all subscribed clients
     */
    suspend fun broadcastMarketData(symbol: String, price: Double, data: Map<String, Any>) {
        val message = WSMessage(
            type = "market-data-update",
            channel = "market-data",
            data = json.encodeToString(mapOf(
                "symbol" to symbol,
                "price" to price,
                "details" to data
            ))
        )
        
        broadcast(marketDataSessions, message)
    }
    
    /**
     * Broadcast message to all sessions in a set
     */
    private suspend fun broadcast(sessions: Set<WebSocketSession>, message: WSMessage) {
        val messageText = json.encodeToString(message)
        val deadSessions = mutableListOf<WebSocketSession>()
        
        sessions.forEach { session ->
            try {
                session.send(Frame.Text(messageText))
            } catch (e: ClosedSendChannelException) {
                logger.debug("Session closed, removing from active sessions")
                deadSessions.add(session)
            } catch (e: Exception) {
                logger.error("Error broadcasting to session", e)
                deadSessions.add(session)
            }
        }
        
        // Remove dead sessions
        deadSessions.forEach { unregisterSession(it) }
        
        if (deadSessions.isNotEmpty()) {
            logger.debug("Removed ${deadSessions.size} dead sessions")
        }
    }
    
    /**
     * Get current connection statistics
     */
    fun getStats(): Map<String, Int> {
        return mapOf(
            "totalConnections" to connectionCount.get(),
            "traderStatusSubscribers" to traderStatusSessions.size,
            "tradeSubscribers" to tradeSessions.size,
            "marketDataSubscribers" to marketDataSessions.size
        )
    }
}

