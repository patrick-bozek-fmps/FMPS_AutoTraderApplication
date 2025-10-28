package com.fmps.autotrader.core.logging

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class LoggingContextTest {
    
    @AfterEach
    fun cleanup() {
        LoggingContext.clear()
    }
    
    @Test
    fun `setRequestId should set request ID in MDC`() {
        val requestId = "test-request-123"
        LoggingContext.setRequestId(requestId)
        
        assertEquals(requestId, LoggingContext.getRequestId())
        assertEquals(requestId, MDC.get(LoggingContext.REQUEST_ID))
    }
    
    @Test
    fun `setRequestId without parameter should generate UUID`() {
        LoggingContext.setRequestId()
        
        val requestId = LoggingContext.getRequestId()
        assertNotNull(requestId)
        assertTrue(requestId!!.isNotEmpty())
    }
    
    @Test
    fun `setUserId should set user ID in MDC`() {
        val userId = "user-456"
        LoggingContext.setUserId(userId)
        
        assertEquals(userId, LoggingContext.getUserId())
    }
    
    @Test
    fun `setTraderId should set trader ID in MDC`() {
        val traderId = "trader-789"
        LoggingContext.setTraderId(traderId)
        
        assertEquals(traderId, LoggingContext.getTraderId())
    }
    
    @Test
    fun `setSessionId should set session ID in MDC`() {
        val sessionId = "session-111"
        LoggingContext.setSessionId(sessionId)
        
        assertEquals(sessionId, LoggingContext.getSessionId())
    }
    
    @Test
    fun `setCorrelationId should set correlation ID in MDC`() {
        val correlationId = "correlation-222"
        LoggingContext.setCorrelationId(correlationId)
        
        assertEquals(correlationId, LoggingContext.getCorrelationId())
    }
    
    @Test
    fun `setOperation should set operation in MDC`() {
        val operation = "test_operation"
        LoggingContext.setOperation(operation)
        
        assertEquals(operation, LoggingContext.getOperation())
    }
    
    @Test
    fun `set should add custom key-value pair to MDC`() {
        LoggingContext.set("customKey", "customValue")
        
        assertEquals("customValue", LoggingContext.get("customKey"))
    }
    
    @Test
    fun `remove should remove key from MDC`() {
        LoggingContext.set("tempKey", "tempValue")
        assertEquals("tempValue", LoggingContext.get("tempKey"))
        
        LoggingContext.remove("tempKey")
        assertNull(LoggingContext.get("tempKey"))
    }
    
    @Test
    fun `clear should remove all MDC context`() {
        LoggingContext.setRequestId("request-1")
        LoggingContext.setUserId("user-1")
        LoggingContext.set("custom", "value")
        
        LoggingContext.clear()
        
        assertNull(LoggingContext.getRequestId())
        assertNull(LoggingContext.getUserId())
        assertNull(LoggingContext.get("custom"))
    }
    
    @Test
    fun `getContext should return copy of MDC context map`() {
        LoggingContext.setRequestId("request-1")
        LoggingContext.setUserId("user-1")
        
        val context = LoggingContext.getContext()
        
        assertEquals(2, context.size)
        assertEquals("request-1", context[LoggingContext.REQUEST_ID])
        assertEquals("user-1", context[LoggingContext.USER_ID])
    }
    
    @Test
    fun `setContext should set entire MDC context from map`() {
        val contextMap = mapOf(
            LoggingContext.REQUEST_ID to "request-1",
            LoggingContext.USER_ID to "user-1",
            "custom" to "value"
        )
        
        LoggingContext.setContext(contextMap)
        
        assertEquals("request-1", LoggingContext.getRequestId())
        assertEquals("user-1", LoggingContext.getUserId())
        assertEquals("value", LoggingContext.get("custom"))
    }
    
    @Test
    fun `withLoggingContext should execute block with context and clean up`() {
        var executedWithContext = false
        
        withLoggingContext(
            requestId = "request-123",
            userId = "user-456",
            traderId = "trader-789"
        ) {
            assertEquals("request-123", LoggingContext.getRequestId())
            assertEquals("user-456", LoggingContext.getUserId())
            assertEquals("trader-789", LoggingContext.getTraderId())
            executedWithContext = true
        }
        
        assertTrue(executedWithContext)
        assertNull(LoggingContext.getRequestId())
        assertNull(LoggingContext.getUserId())
        assertNull(LoggingContext.getTraderId())
    }
    
    @Test
    fun `withLoggingContext should restore previous context after execution`() {
        LoggingContext.setRequestId("original-request")
        LoggingContext.setUserId("original-user")
        
        withLoggingContext(
            requestId = "temp-request",
            userId = "temp-user"
        ) {
            assertEquals("temp-request", LoggingContext.getRequestId())
            assertEquals("temp-user", LoggingContext.getUserId())
        }
        
        assertEquals("original-request", LoggingContext.getRequestId())
        assertEquals("original-user", LoggingContext.getUserId())
    }
    
    @Test
    fun `withLoggingContext should support additional context`() {
        withLoggingContext(
            additionalContext = mapOf("custom1" to "value1", "custom2" to "value2")
        ) {
            assertEquals("value1", LoggingContext.get("custom1"))
            assertEquals("value2", LoggingContext.get("custom2"))
        }
        
        assertNull(LoggingContext.get("custom1"))
        assertNull(LoggingContext.get("custom2"))
    }
    
    @Test
    fun `withRequestId should execute block with auto-generated request ID`() {
        withRequestId {
            val requestId = LoggingContext.getRequestId()
            assertNotNull(requestId)
            assertTrue(requestId!!.isNotEmpty())
        }
        
        assertNull(LoggingContext.getRequestId())
    }
    
    @Test
    fun `withRequestId should execute block with provided request ID`() {
        val customRequestId = "custom-request-id"
        
        withRequestId(customRequestId) {
            assertEquals(customRequestId, LoggingContext.getRequestId())
        }
        
        assertNull(LoggingContext.getRequestId())
    }
    
    @Test
    fun `withTraderContext should execute block with trader ID`() {
        withTraderContext("trader-123") {
            assertEquals("trader-123", LoggingContext.getTraderId())
        }
        
        assertNull(LoggingContext.getTraderId())
    }
    
    @Test
    fun `withLoggingContext should return block result`() {
        val result = withLoggingContext(requestId = "request-1") {
            "test-result"
        }
        
        assertEquals("test-result", result)
    }
    
    @Test
    fun `withLoggingContext should propagate exceptions`() {
        var exceptionThrown = false
        
        try {
            withLoggingContext(requestId = "request-1") {
                throw IllegalStateException("Test exception")
            }
        } catch (e: IllegalStateException) {
            exceptionThrown = true
        }
        
        assertTrue(exceptionThrown)
        assertNull(LoggingContext.getRequestId())  // Context should still be cleaned up
    }
}

