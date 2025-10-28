package com.fmps.autotrader.core.logging

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MetricsLoggerTest {
    
    @Test
    fun `logPerformance should log operation with duration`() {
        // This test verifies the method doesn't throw exceptions
        MetricsLogger.logPerformance("test_operation", 100L)
    }
    
    @Test
    fun `logPerformance should log with additional info`() {
        val additionalInfo = mapOf(
            "user_id" to "123",
            "action" to "create"
        )
        
        MetricsLogger.logPerformance("test_operation", 150L, additionalInfo)
    }
    
    @Test
    fun `logBusinessMetric should log metric with value`() {
        MetricsLogger.logBusinessMetric("users_created", 5)
    }
    
    @Test
    fun `logBusinessMetric should log with custom unit`() {
        MetricsLogger.logBusinessMetric("response_size", 1024, "bytes")
    }
    
    @Test
    fun `logBusinessMetric should log with additional info`() {
        val additionalInfo = mapOf(
            "region" to "US",
            "status" to "active"
        )
        
        MetricsLogger.logBusinessMetric("active_users", 100, "count", additionalInfo)
    }
    
    @Test
    fun `logSystemMetric should log system metrics`() {
        MetricsLogger.logSystemMetric("cpu_usage", 75.5, "percent")
    }
    
    @Test
    fun `logSystemMetric should log without unit`() {
        MetricsLogger.logSystemMetric("thread_count", 42)
    }
    
    @Test
    fun `logDatabaseQuery should log query performance`() {
        MetricsLogger.logDatabaseQuery("SELECT * FROM users", 50L, 100)
    }
    
    @Test
    fun `logApiRequest should log API request metrics`() {
        MetricsLogger.logApiRequest("/api/traders", "GET", 200, 150L)
    }
    
    @Test
    fun `logExchangeApiCall should log exchange API performance`() {
        MetricsLogger.logExchangeApiCall("Binance", "place_order", 200L, true)
    }
    
    @Test
    fun `logExchangeApiCall should log failed calls`() {
        MetricsLogger.logExchangeApiCall("Bitget", "get_balance", 500L, false)
    }
    
    @Test
    fun `logTradeExecution should log trade metrics`() {
        MetricsLogger.logTradeExecution(
            traderId = "trader-123",
            action = "BUY",
            symbol = "BTC/USDT",
            amount = 0.5,
            price = 50000.0
        )
    }
    
    @Test
    fun `logTraderStatusChange should log status transitions`() {
        MetricsLogger.logTraderStatusChange(
            traderId = "trader-123",
            previousStatus = "ACTIVE",
            newStatus = "PAUSED"
        )
    }
    
    @Test
    fun `logPositionPnL should log profit and loss`() {
        MetricsLogger.logPositionPnL(
            traderId = "trader-123",
            positionId = "pos-456",
            pnl = 1500.0,
            pnlPercent = 15.5
        )
    }
    
    @Test
    fun `measureAndLog should execute block and log duration`() {
        val result = measureAndLog("test_operation") {
            Thread.sleep(10)
            "test-result"
        }
        
        assertEquals("test-result", result)
    }
    
    @Test
    fun `measureAndLog should log with additional info`() {
        val additionalInfo = mapOf("test" to "value")
        
        val result = measureAndLog("test_operation", additionalInfo) {
            42
        }
        
        assertEquals(42, result)
    }
    
    @Test
    fun `measureDatabaseQuery should execute query and log duration`() {
        val mockData = listOf("item1", "item2", "item3")
        
        val result = measureDatabaseQuery("find_all_items") {
            mockData
        }
        
        assertEquals(mockData, result)
        assertEquals(3, result.size)
    }
    
    @Test
    fun `measureDatabaseQuery should handle array results`() {
        val mockData = arrayOf("a", "b", "c")
        
        val result = measureDatabaseQuery("find_array") {
            mockData
        }
        
        assertEquals(mockData, result)
    }
    
    @Test
    fun `measureDatabaseQuery should handle single results`() {
        val result = measureDatabaseQuery("find_one") {
            "single-item"
        }
        
        assertEquals("single-item", result)
    }
    
    @Test
    fun `measureExchangeApiCall should execute and log successful call`() {
        val result = measureExchangeApiCall("Binance", "get_balance") {
            mapOf("BTC" to 1.5, "USDT" to 10000.0)
        }
        
        assertEquals(2, result.size)
    }
    
    @Test
    fun `measureExchangeApiCall should log failed calls with exception`() {
        var exceptionThrown = false
        
        try {
            measureExchangeApiCall("Bitget", "place_order") {
                throw RuntimeException("API Error")
            }
        } catch (e: RuntimeException) {
            exceptionThrown = true
            assertEquals("API Error", e.message)
        }
        
        assertTrue(exceptionThrown)
    }
    
    @Test
    fun `measureAndLog should propagate exceptions`() {
        var exceptionThrown = false
        
        try {
            measureAndLog("failing_operation") {
                throw IllegalStateException("Test exception")
            }
        } catch (e: IllegalStateException) {
            exceptionThrown = true
        }
        
        assertTrue(exceptionThrown)
    }
}

