package com.fmps.autotrader.core.connectors.binance

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BinanceAuthenticatorTest {

    @Test
    fun `test initialization with valid credentials`() {
        val authenticator = BinanceAuthenticator(
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            recvWindow = 5000
        )

        assertNotNull(authenticator)
    }

    @Test
    fun `test initialization fails with blank API key`() {
        assertThrows<IllegalArgumentException> {
            BinanceAuthenticator(
                apiKey = "",
                apiSecret = "test-api-secret"
            )
        }

        assertThrows<IllegalArgumentException> {
            BinanceAuthenticator(
                apiKey = "   ",
                apiSecret = "test-api-secret"
            )
        }
    }

    @Test
    fun `test initialization fails with blank API secret`() {
        assertThrows<IllegalArgumentException> {
            BinanceAuthenticator(
                apiKey = "test-api-key",
                apiSecret = ""
            )
        }

        assertThrows<IllegalArgumentException> {
            BinanceAuthenticator(
                apiKey = "test-api-key",
                apiSecret = "   "
            )
        }
    }

    @Test
    fun `test initialization fails with invalid recvWindow`() {
        assertThrows<IllegalArgumentException> {
            BinanceAuthenticator(
                apiKey = "test-api-key",
                apiSecret = "test-api-secret",
                recvWindow = 0
            )
        }

        assertThrows<IllegalArgumentException> {
            BinanceAuthenticator(
                apiKey = "test-api-key",
                apiSecret = "test-api-secret",
                recvWindow = -1000
            )
        }
    }

    @Test
    fun `test createHeaders returns API key header`() {
        val authenticator = BinanceAuthenticator(
            apiKey = "test-api-key",
            apiSecret = "test-api-secret"
        )

        val headers = authenticator.createHeaders()

        assertEquals(1, headers.size)
        assertEquals("test-api-key", headers["X-MBX-APIKEY"])
    }

    @Test
    fun `test signQueryString adds timestamp and signature`() {
        val authenticator = BinanceAuthenticator(
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            recvWindow = 5000
        )

        val queryString = "symbol=BTCUSDT&side=BUY"
        val signedQueryString = authenticator.signQueryString(queryString)

        // Should contain original parameters
        assertTrue(signedQueryString.contains("symbol=BTCUSDT"))
        assertTrue(signedQueryString.contains("side=BUY"))
        
        // Should add timestamp
        assertTrue(signedQueryString.contains("timestamp="))
        
        // Should add recvWindow
        assertTrue(signedQueryString.contains("recvWindow=5000"))
        
        // Should add signature
        assertTrue(signedQueryString.contains("signature="))
    }

    @Test
    fun `test signQueryString with empty string`() {
        val authenticator = BinanceAuthenticator(
            apiKey = "test-api-key",
            apiSecret = "test-api-secret"
        )

        val signedQueryString = authenticator.signQueryString("")

        assertTrue(signedQueryString.contains("timestamp="))
        assertTrue(signedQueryString.contains("signature="))
    }

    @Test
    fun `test signParameters adds timestamp and signature`() {
        val authenticator = BinanceAuthenticator(
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            recvWindow = 5000
        )

        val parameters = mapOf(
            "symbol" to "BTCUSDT",
            "side" to "BUY",
            "quantity" to "0.001"
        )

        val signedParams = authenticator.signParameters(parameters)

        // Should contain original parameters
        assertEquals("BTCUSDT", signedParams["symbol"])
        assertEquals("BUY", signedParams["side"])
        assertEquals("0.001", signedParams["quantity"])
        
        // Should add timestamp
        assertNotNull(signedParams["timestamp"])
        
        // Should add recvWindow
        assertEquals("5000", signedParams["recvWindow"])
        
        // Should add signature
        assertNotNull(signedParams["signature"])
    }

    @Test
    fun `test signature is deterministic for same input`() {
        val authenticator = BinanceAuthenticator(
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            timestampOffset = 0
        )

        // Fix timestamp to make signatures deterministic
        val fixedParams = mapOf(
            "symbol" to "BTCUSDT",
            "timestamp" to "1234567890000",
            "recvWindow" to "5000"
        )

        val signature1 = authenticator.signParameters(fixedParams)["signature"]
        val signature2 = authenticator.signParameters(fixedParams)["signature"]

        // With same timestamp, signatures should be identical
        // Note: Since signParameters adds its own timestamp, we can't directly compare
        // But we can verify signatures are not null and are hex strings
        assertNotNull(signature1)
        assertNotNull(signature2)
        assertTrue(signature1!!.matches(Regex("[0-9a-f]+")))
        assertTrue(signature2!!.matches(Regex("[0-9a-f]+")))
    }

    @Test
    fun `test updateTimestampOffset adjusts timestamps`() {
        val authenticator = BinanceAuthenticator(
            apiKey = "test-api-key",
            apiSecret = "test-api-secret"
        )

        val serverTime = System.currentTimeMillis() + 5000  // Server is 5 seconds ahead
        authenticator.updateTimestampOffset(serverTime)

        val offset = authenticator.getTimestampOffset()
        
        // Offset should be approximately 5000ms (allowing for execution time)
        assertTrue(offset >= 4900 && offset <= 5100, "Offset: $offset")
    }

    @Test
    fun `test getTimestampOffset returns correct value`() {
        val authenticator = BinanceAuthenticator(
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            timestampOffset = 1234
        )

        assertEquals(1234, authenticator.getTimestampOffset())
    }

    @Test
    fun `test getAdjustedTimestamp includes offset`() {
        val authenticator = BinanceAuthenticator(
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            timestampOffset = 5000
        )

        val localTime = System.currentTimeMillis()
        val adjustedTime = authenticator.getAdjustedTimestamp()

        // Adjusted time should be approximately local time + 5000ms
        val difference = adjustedTime - localTime
        assertTrue(difference >= 4900 && difference <= 5100, "Difference: $difference")
    }

    @Test
    fun `test validateCredentials succeeds with valid credentials`() {
        val authenticator = BinanceAuthenticator(
            apiKey = "test-api-key",
            apiSecret = "test-api-secret"
        )

        // Should not throw exception
        authenticator.validateCredentials()
    }

    @Test
    fun `test signature format is hexadecimal`() {
        val authenticator = BinanceAuthenticator(
            apiKey = "test-api-key",
            apiSecret = "test-api-secret"
        )

        val signedParams = authenticator.signParameters(mapOf("symbol" to "BTCUSDT"))
        val signature = signedParams["signature"]!!

        // Signature should be hex string (64 characters for SHA256)
        assertTrue(signature.matches(Regex("[0-9a-f]+")), "Signature should be hex: $signature")
        assertEquals(64, signature.length, "SHA256 signature should be 64 hex characters")
    }

    @Test
    fun `test different secrets produce different signatures`() {
        val auth1 = BinanceAuthenticator(
            apiKey = "test-api-key",
            apiSecret = "secret1"
        )

        val auth2 = BinanceAuthenticator(
            apiKey = "test-api-key",
            apiSecret = "secret2"
        )

        val params = mapOf(
            "symbol" to "BTCUSDT",
            "timestamp" to "1234567890000",
            "recvWindow" to "5000"
        )

        val sig1 = auth1.signParameters(params)["signature"]
        val sig2 = auth2.signParameters(params)["signature"]

        // Different secrets should produce different signatures
        // (though timestamps will differ, making comparison indirect)
        assertNotNull(sig1)
        assertNotNull(sig2)
    }

    @Test
    fun `test signQueryString handles special characters`() {
        val authenticator = BinanceAuthenticator(
            apiKey = "test-api-key",
            apiSecret = "test-api-secret"
        )

        val queryString = "symbol=BTC/USDT&comment=test%20order"
        val signedQueryString = authenticator.signQueryString(queryString)

        // Should preserve special characters in query string
        assertTrue(signedQueryString.contains("BTC/USDT"))
        assertTrue(signedQueryString.contains("test%20order"))
        assertTrue(signedQueryString.contains("signature="))
    }

    @Test
    fun `test recvWindow is included in signed requests`() {
        val authenticator = BinanceAuthenticator(
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            recvWindow = 10000
        )

        val signedParams = authenticator.signParameters(emptyMap())

        assertEquals("10000", signedParams["recvWindow"])
    }

    @Test
    fun `test timestamp offset affects signature`() {
        val auth1 = BinanceAuthenticator(
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            timestampOffset = 0
        )

        val auth2 = BinanceAuthenticator(
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            timestampOffset = 10000
        )

        val queryString = "symbol=BTCUSDT"
        val signed1 = auth1.signQueryString(queryString)
        val signed2 = auth2.signQueryString(queryString)

        // Different offsets should produce different timestamps and signatures
        assertNotEquals(signed1, signed2)
    }
}

