package com.fmps.autotrader.core.connectors.exceptions

/**
 * Base exception class for all exchange connector errors.
 *
 * This is the root of the exception hierarchy for exchange-related errors.
 * All exchange connector exceptions should extend this class to enable consistent error handling.
 *
 * @param message The error message
 * @param cause The underlying cause (optional)
 * @param exchangeName The name of the exchange where the error occurred (optional)
 * @param errorCode Exchange-specific error code (optional)
 * @since 1.0.0
 */
open class ExchangeException(
    message: String,
    cause: Throwable? = null,
    val exchangeName: String? = null,
    val errorCode: String? = null
) : RuntimeException(message, cause) {
    
    override fun toString(): String {
        val parts = mutableListOf<String>()
        parts.add(this::class.simpleName ?: "ExchangeException")
        
        exchangeName?.let { parts.add("exchange=$it") }
        errorCode?.let { parts.add("code=$it") }
        parts.add("message=$message")
        
        return parts.joinToString(", ", "[", "]")
    }
}

/**
 * Exception thrown when network connection to the exchange fails.
 *
 * This includes:
 * - Network timeouts
 * - DNS resolution failures
 * - Connection refused errors
 * - SSL/TLS errors
 * - WebSocket connection failures
 *
 * @param message The error message
 * @param cause The underlying cause (optional)
 * @param exchangeName The name of the exchange (optional)
 * @param retryable Whether the operation can be retried
 */
class ConnectionException(
    message: String,
    cause: Throwable? = null,
    exchangeName: String? = null,
    val retryable: Boolean = true
) : ExchangeException(message, cause, exchangeName) {
    
    companion object {
        fun timeout(exchangeName: String, endpoint: String, timeoutMs: Long): ConnectionException {
            return ConnectionException(
                message = "Connection timeout after ${timeoutMs}ms for endpoint: $endpoint",
                exchangeName = exchangeName,
                retryable = true
            )
        }
        
        fun refused(exchangeName: String, endpoint: String): ConnectionException {
            return ConnectionException(
                message = "Connection refused for endpoint: $endpoint",
                exchangeName = exchangeName,
                retryable = true
            )
        }
    }
}

/**
 * Exception thrown when API authentication fails.
 *
 * This includes:
 * - Invalid API key or secret
 * - Expired API keys
 * - Insufficient permissions
 * - Invalid signature
 * - Missing required authentication headers
 *
 * @param message The error message
 * @param cause The underlying cause (optional)
 * @param exchangeName The name of the exchange (optional)
 * @param errorCode Exchange-specific error code (optional)
 */
class AuthenticationException(
    message: String,
    cause: Throwable? = null,
    exchangeName: String? = null,
    errorCode: String? = null
) : ExchangeException(message, cause, exchangeName, errorCode) {
    
    companion object {
        fun invalidCredentials(exchangeName: String): AuthenticationException {
            return AuthenticationException(
                message = "Invalid API credentials. Check your API key and secret.",
                exchangeName = exchangeName
            )
        }
        
        fun invalidSignature(exchangeName: String): AuthenticationException {
            return AuthenticationException(
                message = "Invalid request signature. Check your API secret and request parameters.",
                exchangeName = exchangeName
            )
        }
        
        fun insufficientPermissions(exchangeName: String, requiredPermission: String): AuthenticationException {
            return AuthenticationException(
                message = "Insufficient permissions. Required: $requiredPermission",
                exchangeName = exchangeName
            )
        }
    }
}

/**
 * Exception thrown when API rate limits are exceeded.
 *
 * This includes:
 * - Request rate limit exceeded (too many requests per second/minute)
 * - Weight-based rate limit exceeded
 * - Order rate limit exceeded
 *
 * @param message The error message
 * @param cause The underlying cause (optional)
 * @param exchangeName The name of the exchange (optional)
 * @param errorCode Exchange-specific error code (optional)
 * @param retryAfterMs Milliseconds to wait before retrying (if known)
 * @param limitType Type of rate limit that was exceeded
 */
class RateLimitException(
    message: String,
    cause: Throwable? = null,
    exchangeName: String? = null,
    errorCode: String? = null,
    val retryAfterMs: Long? = null,
    val limitType: RateLimitType = RateLimitType.REQUEST
) : ExchangeException(message, cause, exchangeName, errorCode) {
    
    enum class RateLimitType {
        REQUEST,    // General request rate limit
        WEIGHT,     // Weight-based rate limit
        ORDER,      // Order-specific rate limit
        WEBSOCKET   // WebSocket connection limit
    }
    
    companion object {
        fun requestLimit(exchangeName: String, retryAfterMs: Long? = null): RateLimitException {
            return RateLimitException(
                message = "Request rate limit exceeded. ${retryAfterMs?.let { "Retry after ${it}ms" } ?: ""}",
                exchangeName = exchangeName,
                retryAfterMs = retryAfterMs,
                limitType = RateLimitType.REQUEST
            )
        }
        
        fun weightLimit(exchangeName: String, currentWeight: Int, maxWeight: Int): RateLimitException {
            return RateLimitException(
                message = "Weight-based rate limit exceeded: $currentWeight/$maxWeight",
                exchangeName = exchangeName,
                limitType = RateLimitType.WEIGHT
            )
        }
        
        fun orderLimit(exchangeName: String): RateLimitException {
            return RateLimitException(
                message = "Order rate limit exceeded. Too many orders per second.",
                exchangeName = exchangeName,
                limitType = RateLimitType.ORDER
            )
        }
    }
}

/**
 * Exception thrown when account has insufficient funds for an operation.
 *
 * This includes:
 * - Insufficient balance for order placement
 * - Insufficient margin
 * - Account restrictions
 *
 * @param message The error message
 * @param cause The underlying cause (optional)
 * @param exchangeName The name of the exchange (optional)
 * @param errorCode Exchange-specific error code (optional)
 * @param asset The asset that has insufficient balance (optional)
 * @param required Required amount (optional)
 * @param available Available amount (optional)
 */
class InsufficientFundsException(
    message: String,
    cause: Throwable? = null,
    exchangeName: String? = null,
    errorCode: String? = null,
    val asset: String? = null,
    val required: String? = null,
    val available: String? = null
) : ExchangeException(message, cause, exchangeName, errorCode) {
    
    companion object {
        fun forAsset(
            exchangeName: String,
            asset: String,
            required: String,
            available: String
        ): InsufficientFundsException {
            return InsufficientFundsException(
                message = "Insufficient $asset balance. Required: $required, Available: $available",
                exchangeName = exchangeName,
                asset = asset,
                required = required,
                available = available
            )
        }
    }
}

/**
 * Exception thrown when an order operation fails.
 *
 * This includes:
 * - Order placement rejection
 * - Order cancellation failure
 * - Order not found
 * - Invalid order parameters
 * - Market conditions prevent order execution
 *
 * @param message The error message
 * @param cause The underlying cause (optional)
 * @param exchangeName The name of the exchange (optional)
 * @param errorCode Exchange-specific error code (optional)
 * @param orderId The order ID if applicable (optional)
 * @param symbol The trading pair symbol if applicable (optional)
 */
class OrderException(
    message: String,
    cause: Throwable? = null,
    exchangeName: String? = null,
    errorCode: String? = null,
    val orderId: String? = null,
    val symbol: String? = null
) : ExchangeException(message, cause, exchangeName, errorCode) {
    
    companion object {
        fun notFound(exchangeName: String, orderId: String, symbol: String): OrderException {
            return OrderException(
                message = "Order not found: $orderId for symbol $symbol",
                exchangeName = exchangeName,
                orderId = orderId,
                symbol = symbol
            )
        }
        
        fun invalidParameters(exchangeName: String, reason: String): OrderException {
            return OrderException(
                message = "Invalid order parameters: $reason",
                exchangeName = exchangeName
            )
        }
        
        fun rejected(exchangeName: String, orderId: String?, reason: String): OrderException {
            return OrderException(
                message = "Order rejected: $reason",
                exchangeName = exchangeName,
                orderId = orderId
            )
        }
    }
}

/**
 * Exception thrown when an exchange or feature is not supported.
 *
 * @param message The error message
 * @param exchangeName The name of the exchange (optional)
 */
class UnsupportedExchangeException(
    message: String,
    exchangeName: String? = null
) : ExchangeException(message, exchangeName = exchangeName) {
    
    companion object {
        fun forExchange(exchangeName: String): UnsupportedExchangeException {
            return UnsupportedExchangeException(
                message = "Exchange not supported: $exchangeName",
                exchangeName = exchangeName
            )
        }
    }
}

