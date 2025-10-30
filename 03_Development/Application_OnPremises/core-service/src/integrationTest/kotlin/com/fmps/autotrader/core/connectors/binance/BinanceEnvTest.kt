package com.fmps.autotrader.core.connectors.binance

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag

/**
 * Simple test to verify environment variables are passed correctly to integration tests
 */
@Tag("integration")
class BinanceEnvTest {
    
    @Test
    fun `test environment variables are available`() {
        val apiKey = System.getenv("BINANCE_API_KEY")
        val apiSecret = System.getenv("BINANCE_API_SECRET")
        
        println("BINANCE_API_KEY: ${if (apiKey.isNullOrBlank()) "NOT SET" else "${apiKey.take(10)}..."}")
        println("BINANCE_API_SECRET: ${if (apiSecret.isNullOrBlank()) "NOT SET" else "${apiSecret.take(10)}..."}")
        
        assertNotNull(apiKey, "BINANCE_API_KEY should be set")
        assertNotNull(apiSecret, "BINANCE_API_SECRET should be set")
        assertFalse(apiKey.isBlank(), "BINANCE_API_KEY should not be blank")
        assertFalse(apiSecret.isBlank(), "BINANCE_API_SECRET should not be blank")
        
        assertTrue(apiKey.length > 10, "API key should be reasonably long")
        assertTrue(apiSecret.length > 10, "API secret should be reasonably long")
    }
}

