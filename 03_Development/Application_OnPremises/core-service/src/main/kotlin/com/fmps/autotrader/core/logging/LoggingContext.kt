package com.fmps.autotrader.core.logging

import org.slf4j.MDC
import java.util.UUID

/**
 * Utility object for managing Mapped Diagnostic Context (MDC) in logging.
 * Provides thread-safe context management for request tracing and debugging.
 */
object LoggingContext {
    
    // MDC Keys
    const val REQUEST_ID = "requestId"
    const val USER_ID = "userId"
    const val TRADER_ID = "traderId"
    const val SESSION_ID = "sessionId"
    const val CORRELATION_ID = "correlationId"
    const val OPERATION = "operation"
    
    /**
     * Sets a request ID in the MDC. If not provided, generates a new UUID.
     */
    fun setRequestId(requestId: String = UUID.randomUUID().toString()) {
        MDC.put(REQUEST_ID, requestId)
    }
    
    /**
     * Gets the current request ID from MDC.
     */
    fun getRequestId(): String? = MDC.get(REQUEST_ID)
    
    /**
     * Sets a user ID in the MDC.
     */
    fun setUserId(userId: String) {
        MDC.put(USER_ID, userId)
    }
    
    /**
     * Gets the current user ID from MDC.
     */
    fun getUserId(): String? = MDC.get(USER_ID)
    
    /**
     * Sets a trader ID in the MDC.
     */
    fun setTraderId(traderId: String) {
        MDC.put(TRADER_ID, traderId)
    }
    
    /**
     * Gets the current trader ID from MDC.
     */
    fun getTraderId(): String? = MDC.get(TRADER_ID)
    
    /**
     * Sets a session ID in the MDC.
     */
    fun setSessionId(sessionId: String) {
        MDC.put(SESSION_ID, sessionId)
    }
    
    /**
     * Gets the current session ID from MDC.
     */
    fun getSessionId(): String? = MDC.get(SESSION_ID)
    
    /**
     * Sets a correlation ID in the MDC.
     */
    fun setCorrelationId(correlationId: String) {
        MDC.put(CORRELATION_ID, correlationId)
    }
    
    /**
     * Gets the current correlation ID from MDC.
     */
    fun getCorrelationId(): String? = MDC.get(CORRELATION_ID)
    
    /**
     * Sets an operation name in the MDC.
     */
    fun setOperation(operation: String) {
        MDC.put(OPERATION, operation)
    }
    
    /**
     * Gets the current operation from MDC.
     */
    fun getOperation(): String? = MDC.get(OPERATION)
    
    /**
     * Sets a custom key-value pair in the MDC.
     */
    fun set(key: String, value: String) {
        MDC.put(key, value)
    }
    
    /**
     * Gets a custom value from MDC by key.
     */
    fun get(key: String): String? = MDC.get(key)
    
    /**
     * Removes a key from MDC.
     */
    fun remove(key: String) {
        MDC.remove(key)
    }
    
    /**
     * Clears all MDC context.
     */
    fun clear() {
        MDC.clear()
    }
    
    /**
     * Gets the entire MDC context as a map.
     */
    fun getContext(): Map<String, String> = MDC.getCopyOfContextMap() ?: emptyMap()
    
    /**
     * Sets the entire MDC context from a map.
     */
    fun setContext(contextMap: Map<String, String>) {
        MDC.setContextMap(contextMap)
    }
}

/**
 * Extension function to execute a block of code with MDC context.
 * Automatically cleans up context after execution.
 *
 * Example:
 * ```
 * withLoggingContext(
 *     requestId = "req-123",
 *     userId = "user-456"
 * ) {
 *     logger.info("Processing request")  // Will include requestId and userId in logs
 * }
 * ```
 */
inline fun <T> withLoggingContext(
    requestId: String? = null,
    userId: String? = null,
    traderId: String? = null,
    sessionId: String? = null,
    correlationId: String? = null,
    operation: String? = null,
    additionalContext: Map<String, String> = emptyMap(),
    block: () -> T
): T {
    // Save existing context
    val existingContext = LoggingContext.getContext()
    
    try {
        // Set new context
        requestId?.let { LoggingContext.setRequestId(it) }
        userId?.let { LoggingContext.setUserId(it) }
        traderId?.let { LoggingContext.setTraderId(it) }
        sessionId?.let { LoggingContext.setSessionId(it) }
        correlationId?.let { LoggingContext.setCorrelationId(it) }
        operation?.let { LoggingContext.setOperation(it) }
        additionalContext.forEach { (key, value) -> LoggingContext.set(key, value) }
        
        // Execute block
        return block()
    } finally {
        // Restore previous context
        LoggingContext.clear()
        if (existingContext.isNotEmpty()) {
            LoggingContext.setContext(existingContext)
        }
    }
}

/**
 * Extension function to execute a block with a new request ID.
 * Convenient for API endpoints and background tasks.
 *
 * Example:
 * ```
 * withRequestId { 
 *     logger.info("Processing request")  // Includes auto-generated requestId
 * }
 * ```
 */
inline fun <T> withRequestId(requestId: String = UUID.randomUUID().toString(), block: () -> T): T {
    return withLoggingContext(requestId = requestId, block = block)
}

/**
 * Extension function to execute a block with trader context.
 *
 * Example:
 * ```
 * withTraderContext(traderId = "trader-123") {
 *     logger.info("Executing trade")  // Includes traderId in logs
 * }
 * ```
 */
inline fun <T> withTraderContext(traderId: String, block: () -> T): T {
    return withLoggingContext(traderId = traderId, block = block)
}

