package com.fmps.autotrader.core.connectors.retry

import com.fmps.autotrader.core.connectors.exceptions.*
import kotlinx.coroutines.delay
import mu.KotlinLogging
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

/**
 * Policy for retrying failed operations with exponential backoff.
 *
 * This class implements a retry mechanism with:
 * - Configurable maximum number of retries
 * - Exponential backoff with jitter
 * - Selective retry based on exception type
 * - Retry metrics and logging
 *
 * ## Usage
 * ```kotlin
 * val policy = RetryPolicy(
 *     maxRetries = 3,
 *     baseDelayMs = 1000,
 *     maxDelayMs = 30000,
 *     exponentialBackoff = true
 * )
 *
 * val result = policy.execute("fetch_market_data") {
 *     // Operation that might fail
 *     exchangeClient.getCandles()
 * }
 * ```
 *
 * @param maxRetries Maximum number of retry attempts (default: 3)
 * @param baseDelayMs Base delay in milliseconds before first retry (default: 1000ms)
 * @param maxDelayMs Maximum delay in milliseconds between retries (default: 30000ms = 30s)
 * @param exponentialBackoff Whether to use exponential backoff (default: true)
 * @param jitterFactor Factor for randomizing delay (0.0 = no jitter, 1.0 = full jitter) (default: 0.1)
 *
 * @since 1.0.0
 */
class RetryPolicy(
    val maxRetries: Int = 3,
    val baseDelayMs: Long = 1000,
    val maxDelayMs: Long = 30000,
    val exponentialBackoff: Boolean = true,
    val jitterFactor: Double = 0.1
) {

    init {
        require(maxRetries >= 0) { "maxRetries must be non-negative" }
        require(baseDelayMs > 0) { "baseDelayMs must be positive" }
        require(maxDelayMs >= baseDelayMs) { "maxDelayMs must be >= baseDelayMs" }
        require(jitterFactor in 0.0..1.0) { "jitterFactor must be between 0.0 and 1.0" }
    }

    /**
     * Executes an operation with retry logic.
     *
     * The operation will be retried up to [maxRetries] times if it fails with a retryable exception.
     * Non-retryable exceptions are thrown immediately without retrying.
     *
     * @param operationName Name of the operation (for logging)
     * @param operation The suspend function to execute
     * @return The result of the operation
     * @throws Exception The last exception if all retries are exhausted
     */
    suspend fun <T> execute(
        operationName: String,
        operation: suspend () -> T
    ): T {
        var attempt = 0
        var lastException: Exception? = null

        while (attempt <= maxRetries) {
            try {
                if (attempt > 0) {
                    logger.debug { "Retry attempt $attempt/$maxRetries for operation: $operationName" }
                }
                
                return operation()
                
            } catch (e: Exception) {
                lastException = e
                
                // Check if exception is retryable
                if (!shouldRetry(e)) {
                    logger.debug { "Exception not retryable, throwing immediately: ${e::class.simpleName}" }
                    throw e
                }
                
                // Check if we have retries left
                if (attempt >= maxRetries) {
                    logger.warn { "Max retries ($maxRetries) exhausted for operation: $operationName" }
                    throw e
                }
                
                // Calculate delay
                val delayMs = calculateDelay(attempt)
                logger.info { 
                    "Operation '$operationName' failed (attempt ${attempt + 1}/${maxRetries + 1}): ${e.message}. " +
                    "Retrying in ${delayMs}ms..."
                }
                
                // Wait before retrying
                delay(delayMs)
                attempt++
            }
        }

        // This should never happen, but just in case
        throw lastException ?: IllegalStateException("Retry logic error: no exception to throw")
    }

    /**
     * Determines if an exception should trigger a retry.
     *
     * Retryable exceptions:
     * - ConnectionException (if marked as retryable)
     * - RateLimitException (always retryable)
     * - Temporary network errors
     *
     * Non-retryable exceptions:
     * - AuthenticationException (credentials won't change on retry)
     * - InsufficientFundsException (balance won't change on retry)
     * - OrderException with certain error codes
     * - Validation errors
     *
     * @param exception The exception to check
     * @return `true` if the operation should be retried, `false` otherwise
     */
    fun shouldRetry(exception: Exception): Boolean {
        return when (exception) {
            is ConnectionException -> exception.retryable
            is RateLimitException -> true
            is AuthenticationException -> false
            is InsufficientFundsException -> false
            is OrderException -> {
                // Only retry certain order exceptions (e.g., temporary exchange issues)
                // Don't retry invalid parameters, orders not found, etc.
                exception.errorCode?.let { code ->
                    // These are example codes - actual codes depend on exchange
                    code !in listOf("INVALID_PARAMETERS", "ORDER_NOT_FOUND", "DUPLICATE_ORDER")
                } ?: false
            }
            is UnsupportedExchangeException -> false
            is ExchangeException -> true // Generic exchange exceptions are retryable by default
            else -> false // Unknown exceptions are not retried
        }
    }

    /**
     * Calculates the delay before the next retry attempt.
     *
     * Uses exponential backoff with jitter to avoid thundering herd problem:
     * - delay = min(baseDelay * 2^attempt, maxDelay)
     * - jitter = delay * (1 ± jitterFactor * random)
     *
     * @param attempt The current attempt number (0-indexed)
     * @return Delay in milliseconds
     */
    fun calculateDelay(attempt: Int): Long {
        val baseDelay = if (exponentialBackoff) {
            // Exponential backoff: baseDelayMs * 2^attempt
            val exponentialDelay = baseDelayMs * 2.0.pow(attempt).toLong()
            min(exponentialDelay, maxDelayMs)
        } else {
            // Linear backoff: baseDelayMs
            baseDelayMs
        }

        // Add jitter to avoid thundering herd
        val jitter = if (jitterFactor > 0.0) {
            val jitterRange = baseDelay * jitterFactor
            val randomJitter = Random.nextDouble(-jitterRange, jitterRange)
            randomJitter.toLong()
        } else {
            0L
        }

        return (baseDelay + jitter).coerceAtLeast(0)
    }

    /**
     * Creates a copy of this retry policy with modified parameters.
     *
     * @param maxRetries New max retries (null to keep current)
     * @param baseDelayMs New base delay (null to keep current)
     * @param maxDelayMs New max delay (null to keep current)
     * @param exponentialBackoff New exponential backoff setting (null to keep current)
     * @param jitterFactor New jitter factor (null to keep current)
     * @return A new RetryPolicy instance with updated parameters
     */
    fun copy(
        maxRetries: Int? = null,
        baseDelayMs: Long? = null,
        maxDelayMs: Long? = null,
        exponentialBackoff: Boolean? = null,
        jitterFactor: Double? = null
    ): RetryPolicy {
        return RetryPolicy(
            maxRetries = maxRetries ?: this.maxRetries,
            baseDelayMs = baseDelayMs ?: this.baseDelayMs,
            maxDelayMs = maxDelayMs ?: this.maxDelayMs,
            exponentialBackoff = exponentialBackoff ?: this.exponentialBackoff,
            jitterFactor = jitterFactor ?: this.jitterFactor
        )
    }

    companion object {
        /**
         * Default retry policy with conservative settings.
         */
        val DEFAULT = RetryPolicy(
            maxRetries = 3,
            baseDelayMs = 1000,
            maxDelayMs = 30000,
            exponentialBackoff = true,
            jitterFactor = 0.1
        )

        /**
         * Aggressive retry policy for critical operations.
         */
        val AGGRESSIVE = RetryPolicy(
            maxRetries = 5,
            baseDelayMs = 500,
            maxDelayMs = 60000,
            exponentialBackoff = true,
            jitterFactor = 0.2
        )

        /**
         * Conservative retry policy for less critical operations.
         */
        val CONSERVATIVE = RetryPolicy(
            maxRetries = 2,
            baseDelayMs = 2000,
            maxDelayMs = 15000,
            exponentialBackoff = true,
            jitterFactor = 0.1
        )

        /**
         * No retry policy - fail immediately on first error.
         * Note: baseDelayMs must be > 0 due to validation, but it doesn't matter since maxRetries = 0
         */
        val NONE = RetryPolicy(
            maxRetries = 0,
            baseDelayMs = 1,  // Must be > 0 for validation, but unused since maxRetries = 0
            maxDelayMs = 1,   // Must be >= baseDelayMs
            exponentialBackoff = false,
            jitterFactor = 0.0
        )
    }
}

/**
 * Extension function to execute an operation with a retry policy.
 *
 * @param operationName Name of the operation (for logging)
 * @param retryPolicy The retry policy to use (default: RetryPolicy.DEFAULT)
 * @param operation The suspend function to execute
 * @return The result of the operation
 */
suspend fun <T> retryWithPolicy(
    operationName: String,
    retryPolicy: RetryPolicy = RetryPolicy.DEFAULT,
    operation: suspend () -> T
): T {
    return retryPolicy.execute(operationName, operation)
}

