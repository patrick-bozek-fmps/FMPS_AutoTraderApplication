package com.fmps.autotrader.core.connectors.bitget

import com.fmps.autotrader.core.connectors.exceptions.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Handles Bitget-specific error codes and maps them to framework exceptions
 * 
 * **Common Bitget Error Codes:**
 * - 40001: Invalid request parameters
 * - 40002: Invalid API key
 * - 40003: Invalid signature
 * - 40004: Request timestamp expired
 * - 40005: Invalid passphrase
 * - 40006: IP not in whitelist
 * - 40007: Access denied
 * - 40008: API key expired
 * - 40014: Rate limit exceeded
 * - 43001: Insufficient balance
 * - 43002: Order not found
 * - 43003: Order already cancelled
 * - 43111: Market order size too small
 * - 43112: Market order size too large
 * 
 * @see ExchangeException
 */
class BitgetErrorHandler {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Handles HTTP error responses from Bitget API
     * 
     * @param statusCode HTTP status code
     * @param responseBody Response body (JSON)
     * @throws ExchangeException Appropriate exception based on error code
     */
    fun handleHttpError(statusCode: Int, responseBody: String) {
        try {
            val jsonResponse = json.parseToJsonElement(responseBody).jsonObject
            val code = jsonResponse["code"]?.jsonPrimitive?.content ?: statusCode.toString()
            val message = jsonResponse["msg"]?.jsonPrimitive?.content ?: "Unknown error"
            
            logger.error { "Bitget API error: code=$code, message=$message" }
            
            // Map error code to exception
            throw when (code) {
                // Authentication errors
                "40002", "40003", "40005", "40008" -> AuthenticationException(
                    message = "Bitget authentication failed: $message",
                    exchangeName = "BITGET"
                )
                
                // Permission/access errors
                "40006", "40007" -> AuthenticationException(
                    message = "Bitget access denied: $message",
                    exchangeName = "BITGET"
                )
                
                // Timestamp errors
                "40004" -> ConnectionException(
                    message = "Bitget timestamp error: $message. Please sync your system clock.",
                    exchangeName = "BITGET"
                )
                
                // Rate limit errors
                "40014" -> RateLimitException(
                    message = "Bitget rate limit exceeded: $message",
                    exchangeName = "BITGET",
                    retryAfterMs = 60000  // 60 seconds in milliseconds
                )
                
                // Insufficient balance
                "43001" -> InsufficientFundsException(
                    message = "Bitget insufficient balance: $message",
                    exchangeName = "BITGET"
                )
                
                // Order errors
                "43002", "43003" -> OrderException(
                    message = "Bitget order error: $message",
                    exchangeName = "BITGET"
                )
                
                // Order size errors
                "43111", "43112" -> OrderException(
                    message = "Bitget order size error: $message",
                    exchangeName = "BITGET"
                )
                
                // Invalid parameters
                "40001" -> ExchangeException(
                    message = "Bitget invalid parameters: $message",
                    exchangeName = "BITGET"
                )
                
                // Generic error
                else -> ExchangeException(
                    message = "Bitget API error (code: $code): $message",
                    exchangeName = "BITGET"
                )
            }
            
        } catch (e: ExchangeException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse Bitget error response" }
            throw ExchangeException(
                message = "Bitget API error (HTTP $statusCode): $responseBody",
                cause = e,
                exchangeName = "BITGET"
            )
        }
    }
    
    /**
     * Checks if an error is retryable
     * 
     * @param exception The exception to check
     * @return true if the error is retryable
     */
    fun isRetryable(exception: ExchangeException): Boolean {
        return when (exception) {
            is RateLimitException -> true
            is ConnectionException -> true
            else -> false
        }
    }
}

