package com.fmps.autotrader.core.connectors.binance

import com.fmps.autotrader.core.connectors.exceptions.*
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BinanceErrorHandlerTest {

    private val errorHandler = BinanceErrorHandler()

    private fun createMockClient(status: HttpStatusCode, responseBody: String): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(
                        content = ByteReadChannel(responseBody),
                        status = status,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        }
    }

    @Test
    fun `test handleResponse succeeds for 200 OK`() = runBlocking {
        val client = createMockClient(HttpStatusCode.OK, """{"status":"success"}""")
        val response = client.get("https://api.binance.com/test")

        // Should not throw exception
        errorHandler.handleResponse(response)
    }

    @Test
    fun `test handleResponse throws ConnectionException for timestamp error`() = runBlocking {
        val client = createMockClient(
            HttpStatusCode.BadRequest,
            """{"code":-1021,"msg":"Timestamp outside recvWindow"}"""
        )
        val response = client.get("https://api.binance.com/test")

        val exception = assertThrows<ConnectionException> {
            errorHandler.handleResponse(response)
        }

        assertTrue(exception.message!!.contains("Timestamp outside recvWindow"))
    }

    @Test
    fun `test handleResponse throws AuthenticationException for invalid signature`() = runBlocking {
        val client = createMockClient(
            HttpStatusCode.BadRequest,
            """{"code":-1022,"msg":"Invalid signature"}"""
        )
        val response = client.get("https://api.binance.com/test")

        val exception = assertThrows<AuthenticationException> {
            errorHandler.handleResponse(response)
        }

        assertTrue(exception.message!!.contains("Invalid signature"))
    }

    @Test
    fun `test handleResponse throws AuthenticationException for invalid API key`() = runBlocking {
        val client = createMockClient(
            HttpStatusCode.BadRequest,
            """{"code":-2015,"msg":"Invalid API key"}"""
        )
        val response = client.get("https://api.binance.com/test")

        val exception = assertThrows<AuthenticationException> {
            errorHandler.handleResponse(response)
        }

        assertTrue(exception.message!!.contains("Invalid API key"))
    }

    @Test
    fun `test handleResponse throws RateLimitException for rate limit error`() = runBlocking {
        val client = createMockClient(
            HttpStatusCode.BadRequest,
            """{"code":-1003,"msg":"Rate limit exceeded"}"""
        )
        val response = client.get("https://api.binance.com/test")

        val exception = assertThrows<RateLimitException> {
            errorHandler.handleResponse(response)
        }

        assertTrue(exception.message!!.contains("Rate limit exceeded"))
    }

    @Test
    fun `test handleResponse throws RateLimitException for HTTP 429`() = runBlocking {
        val client = createMockClient(
            HttpStatusCode.TooManyRequests,
            """{"code":429,"msg":"Too many requests"}"""
        )
        val response = client.get("https://api.binance.com/test")

        val exception = assertThrows<RateLimitException> {
            errorHandler.handleResponse(response)
        }

        assertTrue(exception.message!!.contains("Too many requests"))
    }

    @Test
    fun `test handleResponse throws InsufficientFundsException`() = runBlocking {
        val client = createMockClient(
            HttpStatusCode.BadRequest,
            """{"code":-2010,"msg":"Insufficient balance"}"""
        )
        val response = client.get("https://api.binance.com/test")

        val exception = assertThrows<InsufficientFundsException> {
            errorHandler.handleResponse(response)
        }

        assertTrue(exception.message!!.contains("Insufficient balance"))
    }

    @Test
    fun `test handleResponse throws OrderException for order not found`() = runBlocking {
        val client = createMockClient(
            HttpStatusCode.BadRequest,
            """{"code":-2011,"msg":"Order not found"}"""
        )
        val response = client.get("https://api.binance.com/test")

        val exception = assertThrows<OrderException> {
            errorHandler.handleResponse(response)
        }

        assertTrue(exception.message!!.contains("Order not found"))
    }

    @Test
    fun `test handleResponse throws OrderException for invalid quantity`() = runBlocking {
        val client = createMockClient(
            HttpStatusCode.BadRequest,
            """{"code":-1013,"msg":"Invalid quantity"}"""
        )
        val response = client.get("https://api.binance.com/test")

        val exception = assertThrows<OrderException> {
            errorHandler.handleResponse(response)
        }

        assertTrue(exception.message!!.contains("Invalid quantity"))
    }

    @Test
    fun `test handleResponse throws ExchangeException for unknown error`() = runBlocking {
        val client = createMockClient(
            HttpStatusCode.BadRequest,
            """{"code":-9999,"msg":"Unknown error"}"""
        )
        val response = client.get("https://api.binance.com/test")

        val exception = assertThrows<ExchangeException> {
            errorHandler.handleResponse(response)
        }

        assertTrue(exception.message!!.contains("Unknown error"))
    }

    @Test
    fun `test handleResponse handles malformed JSON`() = runBlocking {
        val client = createMockClient(
            HttpStatusCode.BadRequest,
            "Invalid JSON response"
        )
        val response = client.get("https://api.binance.com/test")

        val exception = assertThrows<ExchangeException> {
            errorHandler.handleResponse(response)
        }

        assertTrue(exception.message!!.contains("Invalid JSON response"))
    }

    @Test
    fun `test handleResponse handles HTTP 500 errors`() = runBlocking {
        val client = createMockClient(
            HttpStatusCode.InternalServerError,
            """{"code":500,"msg":"Internal server error"}"""
        )
        val response = client.get("https://api.binance.com/test")

        val exception = assertThrows<ExchangeException> {
            errorHandler.handleResponse(response)
        }

        // Should handle server errors
        assertNotNull(exception.message)
    }

    @Test
    fun `test handleResponse passes through success responses`() = runBlocking {
        for (statusCode in listOf(HttpStatusCode.OK, HttpStatusCode.Created, HttpStatusCode.Accepted)) {
            val client = createMockClient(statusCode, """{"result":"success"}""")
            val response = client.get("https://api.binance.com/test")

            // Should not throw
            errorHandler.handleResponse(response)
        }
    }
}

