package com.fmps.autotrader.core.connectors.binance

import com.fmps.autotrader.core.connectors.exceptions.*
import mu.KotlinLogging
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

private val logger = KotlinLogging.logger {}

/**
 * Handles error responses from Binance API
 * 
 * Maps Binance error codes to framework exception types
 */
class BinanceErrorHandler {
    
    /**
     * Checks HTTP response and throws appropriate exception if error occurred
     */
    suspend fun handleResponse(response: HttpResponse) {
        // Success responses
        if (response.status.value in 200..299) {
            return
        }
        
        val errorBody = try {
            response.bodyAsText()
        } catch (e: Exception) {
            logger.error(e) { "Failed to read error response body" }
            throw ExchangeException(
                message = "HTTP ${response.status.value}: Failed to read error details",
                exchangeName = "BINANCE"
            )
        }
        
        logger.warn { "Binance API error: ${response.status} - $errorBody" }
        
        // Parse error response
        val errorJson = try {
            Json.parseToJsonElement(errorBody).jsonObject
        } catch (e: Exception) {
            // If parsing fails, throw generic exception
            throw ExchangeException(
                message = "HTTP ${response.status.value}: $errorBody",
                exchangeName = "BINANCE"
            )
        }
        
        val errorCode = errorJson["code"]?.jsonPrimitive?.intOrNull ?: response.status.value
        val errorMessage = errorJson["msg"]?.jsonPrimitive?.content ?: "Unknown error"
        
        // Map error code to appropriate exception
        val exception = when (errorCode) {
            // Connection/Timestamp errors
            -1021 -> ConnectionException(
                message = "Timestamp outside recvWindow: $errorMessage. System clock may be out of sync.",
                exchangeName = "BINANCE"
            )
            
            // Authentication errors
            -1022 -> AuthenticationException(
                message = "Invalid signature: $errorMessage. Check API secret key.",
                exchangeName = "BINANCE"
            )
            -2015 -> AuthenticationException(
                message = "Invalid API key: $errorMessage",
                exchangeName = "BINANCE"
            )
            -2014 -> AuthenticationException(
                message = "API key format invalid: $errorMessage",
                exchangeName = "BINANCE"
            )
            
            // Rate limiting
            -1003 -> RateLimitException(
                message = "Rate limit exceeded: $errorMessage",
                exchangeName = "BINANCE"
            )
            429 -> RateLimitException(
                message = "Too many requests: $errorMessage",
                exchangeName = "BINANCE"
            )
            
            // Order errors
            -2010 -> InsufficientFundsException(
                message = "Insufficient balance: $errorMessage",
                exchangeName = "BINANCE"
            )
            -2011 -> OrderException(
                message = "Order not found: $errorMessage",
                exchangeName = "BINANCE"
            )
            -1013 -> OrderException(
                message = "Invalid quantity: $errorMessage",
                exchangeName = "BINANCE"
            )
            -1014 -> OrderException(
                message = "Unknown order type: $errorMessage",
                exchangeName = "BINANCE"
            )
            -2013 -> OrderException(
                message = "Order does not exist: $errorMessage",
                exchangeName = "BINANCE"
            )
            
            // Symbol errors  
            -1111, -1121 -> ExchangeException(
                message = "Invalid symbol: $errorMessage",
                exchangeName = "BINANCE",
                errorCode = errorCode.toString()
            )
            
            // Parameter errors
            -1100 -> ExchangeException(
                message = "Illegal characters in parameter: $errorMessage",
                exchangeName = "BINANCE",
                errorCode = errorCode.toString()
            )
            -1101 -> ExchangeException(
                message = "Too many parameters: $errorMessage",
                exchangeName = "BINANCE",
                errorCode = errorCode.toString()
            )
            -1102 -> ExchangeException(
                message = "Mandatory parameter missing: $errorMessage",
                exchangeName = "BINANCE",
                errorCode = errorCode.toString()
            )
            -1104 -> ExchangeException(
                message = "Not all parameters sent: $errorMessage",
                exchangeName = "BINANCE",
                errorCode = errorCode.toString()
            )
            -1105 -> ExchangeException(
                message = "Parameter empty: $errorMessage",
                exchangeName = "BINANCE",
                errorCode = errorCode.toString()
            )
            -1106 -> ExchangeException(
                message = "Parameter not required: $errorMessage",
                exchangeName = "BINANCE",
                errorCode = errorCode.toString()
            )
            
            // Server errors
            in -1000..-1 -> ConnectionException(
                message = "Binance server error [$errorCode]: $errorMessage",
                exchangeName = "BINANCE"
            )
            
            // HTTP errors
            400 -> ExchangeException(
                message = "Bad request: $errorMessage",
                exchangeName = "BINANCE"
            )
            401 -> AuthenticationException(
                message = "Unauthorized: $errorMessage",
                exchangeName = "BINANCE"
            )
            403 -> AuthenticationException(
                message = "Forbidden: $errorMessage",
                exchangeName = "BINANCE"
            )
            404 -> ExchangeException(
                message = "Not found: $errorMessage",
                exchangeName = "BINANCE"
            )
            418 -> RateLimitException(
                message = "IP auto-banned: $errorMessage",
                exchangeName = "BINANCE"
            )
            500, 502, 503, 504 -> ConnectionException(
                message = "Binance service unavailable: $errorMessage",
                exchangeName = "BINANCE"
            )
            
            // Unknown error
            else -> ExchangeException(
                message = "Binance error [$errorCode]: $errorMessage",
                exchangeName = "BINANCE",
                errorCode = errorCode.toString()
            )
        }
        
        logger.error { "Mapped Binance error $errorCode to ${exception::class.simpleName}: ${exception.message}" }
        throw exception
    }
    
    /**
     * Parses Retry-After header from response
     */
    private fun parseRetryAfter(response: HttpResponse): Long? {
        return response.headers["Retry-After"]?.toLongOrNull()
            ?.also { logger.debug { "Retry-After header: $it seconds" } }
    }
    
    /**
     * Checks if an error code is retryable
     */
    fun isRetryable(errorCode: Int): Boolean {
        return when (errorCode) {
            -1003, // Rate limit
            -1021, // Timestamp
            429,   // Too many requests
            500, 502, 503, 504 -> true  // Server errors
            else -> false
        }
    }
    
    /**
     * Gets a user-friendly error message for an error code
     */
    fun getErrorDescription(errorCode: Int): String {
        return when (errorCode) {
            -1000 -> "Unknown error occurred"
            -1001 -> "Server disconnected"
            -1002 -> "Unauthorized request"
            -1003 -> "Too many requests"
            -1013 -> "Invalid quantity"
            -1014 -> "Unknown order type"
            -1015 -> "Invalid order side"
            -1021 -> "Timestamp outside recvWindow"
            -1022 -> "Invalid signature"
            -2010 -> "Insufficient balance"
            -2011 -> "Order not found"
            -2013 -> "Order does not exist"
            -2015 -> "Invalid API key"
            else -> "Error code: $errorCode"
        }
    }
}

