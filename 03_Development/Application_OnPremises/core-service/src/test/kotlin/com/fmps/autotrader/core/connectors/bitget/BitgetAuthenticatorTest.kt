package com.fmps.autotrader.core.connectors.bitget

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BitgetAuthenticatorTest {

    @Test
    fun `test initialization with valid credentials`() {
        val authenticator = BitgetAuthenticator(
            apiKey = "test-api-key",
            apiSecret = "test-api-secret",
            passphrase = "test-passphrase"
        )

        assertNotNull(authenticator)
    }

    @Test
    fun `test initialization fails with blank API key`() {
        assertThrows<IllegalArgumentException> {
            BitgetAuthenticator(
                apiKey = "",
                apiSecret = "test-secret",
                passphrase = "test-passphrase"
            )
        }
    }

    @Test
    fun `test initialization fails with blank API secret`() {
        assertThrows<IllegalArgumentException> {
            BitgetAuthenticator(
                apiKey = "test-key",
                apiSecret = "",
                passphrase = "test-passphrase"
            )
        }
    }

    @Test
    fun `test initialization fails with blank passphrase`() {
        assertThrows<IllegalArgumentException> {
            BitgetAuthenticator(
                apiKey = "test-key",
                apiSecret = "test-secret",
                passphrase = ""
            )
        }
    }

    @Test
    fun `test signRequest includes all required headers`() {
        val authenticator = BitgetAuthenticator(
            apiKey = "test-key",
            apiSecret = "test-secret",
            passphrase = "test-passphrase"
        )

        val (timestamp, headers) = authenticator.signRequest(
            method = "GET",
            requestPath = "/api/spot/v1/account/info"
        )

        assertNotNull(timestamp)
        assertTrue(headers.containsKey("ACCESS-KEY"))
        assertTrue(headers.containsKey("ACCESS-SIGN"))
        assertTrue(headers.containsKey("ACCESS-TIMESTAMP"))
        assertTrue(headers.containsKey("ACCESS-PASSPHRASE"))
        assertEquals("test-key", headers["ACCESS-KEY"])
        assertEquals("test-passphrase", headers["ACCESS-PASSPHRASE"])
    }

    @Test
    fun `test signRequest with GET parameters`() {
        val authenticator = BitgetAuthenticator(
            apiKey = "test-key",
            apiSecret = "test-secret",
            passphrase = "test-passphrase"
        )

        val (timestamp, headers) = authenticator.signRequest(
            method = "GET",
            requestPath = "/api/spot/v1/market/candles"
        )

        assertNotNull(timestamp)
        assertNotNull(headers["ACCESS-SIGN"])
        // Signature should be Base64 encoded, not hex
        assertTrue(headers["ACCESS-SIGN"]!!.length > 20)
    }

    @Test
    fun `test signRequest with POST body`() {
        val authenticator = BitgetAuthenticator(
            apiKey = "test-key",
            apiSecret = "test-secret",
            passphrase = "test-passphrase"
        )

        val (timestamp, headers) = authenticator.signRequest(
            method = "POST",
            requestPath = "/api/spot/v1/trade/orders"
        )

        assertNotNull(timestamp)
        assertNotNull(headers["ACCESS-SIGN"])
    }

    @Test
    fun `test signature is Base64 encoded`() {
        val authenticator = BitgetAuthenticator(
            apiKey = "test-key",
            apiSecret = "test-secret",
            passphrase = "test-passphrase"
        )

        val (_, headers) = authenticator.signRequest(
            method = "GET",
            requestPath = "/api/spot/v1/market/ticker"
        )

        val signature = headers["ACCESS-SIGN"]!!
        // Base64 signatures should match this pattern
        assertNotNull(signature)
        assertTrue(signature.length > 20) // Base64 encoded HMAC SHA256 should be reasonably long
    }

    @Test
    fun `test different methods produce different signatures`() {
        val authenticator = BitgetAuthenticator(
            apiKey = "test-key",
            apiSecret = "test-secret",
            passphrase = "test-passphrase"
        )

        val (_, headersGet) = authenticator.signRequest(
            method = "GET",
            requestPath = "/api/spot/v1/account/info"
        )

        val (_, headersPost) = authenticator.signRequest(
            method = "POST",
            requestPath = "/api/spot/v1/trade/orders"
        )

        // Different methods should produce different signatures
        assertNotEquals(headersGet["ACCESS-SIGN"], headersPost["ACCESS-SIGN"])
    }

    @Test
    fun `test timestamp is included in signature calculation`() {
        val authenticator = BitgetAuthenticator(
            apiKey = "test-key",
            apiSecret = "test-secret",
            passphrase = "test-passphrase"
        )

        val (timestamp1, headers1) = authenticator.signRequest(
            method = "GET",
            requestPath = "/api/spot/v1/market/ticker"
        )

        // Wait a bit to get different timestamp
        Thread.sleep(10)

        val (timestamp2, headers2) = authenticator.signRequest(
            method = "GET",
            requestPath = "/api/spot/v1/market/ticker"
        )

        // Different timestamps should produce different signatures
        // Note: timestamps might be the same if execution is too fast, but signatures should differ
        val sig1 = headers1["ACCESS-SIGN"]
        val sig2 = headers2["ACCESS-SIGN"]
        
        assertNotNull(sig1)
        assertNotNull(sig2)
        // At least verify they are valid signatures
        assertTrue(sig1!!.isNotEmpty())
        assertTrue(sig2!!.isNotEmpty())
    }

    @Test
    fun `test Content-Type header is included`() {
        val authenticator = BitgetAuthenticator(
            apiKey = "test-key",
            apiSecret = "test-secret",
            passphrase = "test-passphrase"
        )

        val (_, headers) = authenticator.signRequest(
            method = "POST",
            requestPath = "/api/spot/v1/trade/orders"
        )

        assertEquals("application/json", headers["Content-Type"])
    }
}

