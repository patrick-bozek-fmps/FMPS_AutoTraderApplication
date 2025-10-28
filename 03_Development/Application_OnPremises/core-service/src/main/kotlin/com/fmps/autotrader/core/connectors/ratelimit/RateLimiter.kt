package com.fmps.autotrader.core.connectors.ratelimit

import com.fmps.autotrader.core.connectors.exceptions.RateLimitException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

private val logger = KotlinLogging.logger {}

/**
 * Token bucket rate limiter for controlling API request rates.
 *
 * This class implements the token bucket algorithm:
 * - A bucket holds tokens representing allowed requests
 * - Tokens are added at a fixed rate
 * - Each request consumes one or more tokens
 * - If insufficient tokens, the request waits or throws an exception
 *
 * ## Features
 * - Per-endpoint rate limiting
 * - Weight-based limiting (different endpoints can cost different amounts)
 * - Thread-safe for concurrent access
 * - Automatic token refill
 * - Metrics for monitoring
 *
 * ## Usage
 * ```kotlin
 * val limiter = RateLimiter(
 *     requestsPerSecond = 20.0,
 *     burstCapacity = 50
 * )
 *
 * // Acquire permission before making request
 * limiter.acquire("market_data", weight = 1)
 * // Make API request
 * ```
 *
 * @param requestsPerSecond Maximum requests per second (tokens added per second)
 * @param burstCapacity Maximum number of tokens that can be accumulated (bucket capacity)
 * @param perEndpointLimit Whether to apply limits per endpoint (default: false - global limit)
 *
 * @since 1.0.0
 */
class RateLimiter(
    val requestsPerSecond: Double,
    val burstCapacity: Int,
    val perEndpointLimit: Boolean = false
) {

    /**
     * Token bucket for managing rate limits.
     */
    private class TokenBucket(
        val capacity: Int,
        val refillRatePerMs: Double
    ) {
        private var tokens: Double = capacity.toDouble()
        private var lastRefillTime: Long = System.currentTimeMillis()
        private val mutex = Mutex()

        /**
         * Attempts to acquire tokens from the bucket.
         *
         * @param tokens Number of tokens to acquire
         * @param wait Whether to wait if tokens are unavailable
         * @return `true` if tokens were acquired, `false` otherwise
         */
        suspend fun acquire(tokens: Int = 1, wait: Boolean = true): Boolean {
            mutex.withLock {
                refillTokens()

                if (this.tokens >= tokens) {
                    this.tokens -= tokens
                    return true
                }

                if (!wait) {
                    return false
                }

                // Calculate wait time
                val tokensNeeded = tokens - this.tokens
                val waitTimeMs = (tokensNeeded / refillRatePerMs).toLong()

                logger.debug { "Waiting ${waitTimeMs}ms for $tokens tokens" }
                return false // Caller should handle waiting
            }
        }

        /**
         * Calculates the wait time needed to acquire tokens.
         *
         * @param tokens Number of tokens needed
         * @return Wait time in milliseconds, or 0 if tokens are available
         */
        suspend fun calculateWaitTime(tokens: Int = 1): Long {
            mutex.withLock {
                refillTokens()

                if (this.tokens >= tokens) {
                    return 0
                }

                val tokensNeeded = tokens - this.tokens
                return (tokensNeeded / refillRatePerMs).toLong()
            }
        }

        /**
         * Refills tokens based on elapsed time since last refill.
         */
        private fun refillTokens() {
            val now = System.currentTimeMillis()
            val elapsedMs = now - lastRefillTime

            if (elapsedMs > 0) {
                val tokensToAdd = elapsedMs * refillRatePerMs
                tokens = min(tokens + tokensToAdd, capacity.toDouble())
                lastRefillTime = now
            }
        }

        /**
         * Gets the current number of available tokens.
         */
        suspend fun getAvailableTokens(): Double {
            mutex.withLock {
                refillTokens()
                return tokens
            }
        }
    }

    private val refillRatePerMs = requestsPerSecond / 1000.0

    // Global bucket for all requests
    private val globalBucket = TokenBucket(burstCapacity, refillRatePerMs)

    // Per-endpoint buckets (if enabled)
    private val endpointBuckets = ConcurrentHashMap<String, TokenBucket>()

    // Metrics
    private var totalRequests: Long = 0
    private var totalWaitTimeMs: Long = 0
    private var rejectedRequests: Long = 0

    init {
        require(requestsPerSecond > 0) { "requestsPerSecond must be positive" }
        require(burstCapacity > 0) { "burstCapacity must be positive" }
        
        logger.info { 
            "RateLimiter initialized: ${requestsPerSecond} req/s, burst=$burstCapacity, " +
            "perEndpoint=$perEndpointLimit"
        }
    }

    /**
     * Acquires permission to make a request, waiting if necessary.
     *
     * This method will wait until sufficient tokens are available, then consume them.
     *
     * @param endpoint The endpoint being called (for per-endpoint limiting)
     * @param weight The weight/cost of this request (default: 1)
     * @throws RateLimitException if rate limit cannot be satisfied
     */
    suspend fun acquire(endpoint: String = "", weight: Int = 1) {
        require(weight > 0) { "weight must be positive" }

        val startTime = System.currentTimeMillis()
        totalRequests++

        try {
            // Check global bucket
            val globalWaitTime = globalBucket.calculateWaitTime(weight)
            
            // Check endpoint bucket if per-endpoint limiting is enabled
            val endpointWaitTime = if (perEndpointLimit && endpoint.isNotEmpty()) {
                val bucket = getOrCreateEndpointBucket(endpoint)
                bucket.calculateWaitTime(weight)
            } else {
                0L
            }

            // Wait for the longest required time
            val waitTime = maxOf(globalWaitTime, endpointWaitTime)
            
            if (waitTime > 0) {
                logger.debug { "Rate limit: waiting ${waitTime}ms for endpoint=$endpoint, weight=$weight" }
                delay(waitTime)
                totalWaitTimeMs += waitTime
            }

            // Acquire tokens from buckets
            globalBucket.acquire(weight, wait = false)
            if (perEndpointLimit && endpoint.isNotEmpty()) {
                val bucket = getOrCreateEndpointBucket(endpoint)
                bucket.acquire(weight, wait = false)
            }

        } catch (e: Exception) {
            rejectedRequests++
            logger.error(e) { "Failed to acquire rate limit for endpoint=$endpoint" }
            throw RateLimitException(
                message = "Rate limit acquisition failed: ${e.message}",
                cause = e
            )
        }

        val elapsedTime = System.currentTimeMillis() - startTime
        logger.trace { "Rate limit acquired for endpoint=$endpoint in ${elapsedTime}ms" }
    }

    /**
     * Tries to acquire permission without waiting.
     *
     * @param endpoint The endpoint being called
     * @param weight The weight/cost of this request
     * @return `true` if permission was acquired, `false` if rate limit would be exceeded
     */
    suspend fun tryAcquire(endpoint: String = "", weight: Int = 1): Boolean {
        require(weight > 0) { "weight must be positive" }

        totalRequests++

        // Check global bucket
        if (!globalBucket.acquire(weight, wait = false)) {
            rejectedRequests++
            return false
        }

        // Check endpoint bucket if per-endpoint limiting is enabled
        if (perEndpointLimit && endpoint.isNotEmpty()) {
            val bucket = getOrCreateEndpointBucket(endpoint)
            if (!bucket.acquire(weight, wait = false)) {
                rejectedRequests++
                return false
            }
        }

        return true
    }

    /**
     * Gets or creates a token bucket for an endpoint.
     */
    private fun getOrCreateEndpointBucket(endpoint: String): TokenBucket {
        return endpointBuckets.computeIfAbsent(endpoint) {
            TokenBucket(burstCapacity, refillRatePerMs)
        }
    }

    /**
     * Gets the current available tokens in the global bucket.
     *
     * @return Number of available tokens
     */
    suspend fun getAvailableTokens(): Double {
        return globalBucket.getAvailableTokens()
    }

    /**
     * Gets the current available tokens for a specific endpoint.
     *
     * @param endpoint The endpoint to check
     * @return Number of available tokens, or null if per-endpoint limiting is disabled
     */
    suspend fun getAvailableTokens(endpoint: String): Double? {
        if (!perEndpointLimit || endpoint.isEmpty()) {
            return null
        }
        
        return endpointBuckets[endpoint]?.getAvailableTokens()
    }

    /**
     * Gets rate limiter metrics.
     *
     * @return Metrics including total requests, wait time, and rejection rate
     */
    fun getMetrics(): RateLimiterMetrics {
        return RateLimiterMetrics(
            totalRequests = totalRequests,
            rejectedRequests = rejectedRequests,
            totalWaitTimeMs = totalWaitTimeMs,
            averageWaitTimeMs = if (totalRequests > 0) totalWaitTimeMs / totalRequests else 0,
            rejectionRate = if (totalRequests > 0) rejectedRequests.toDouble() / totalRequests else 0.0,
            requestsPerSecond = requestsPerSecond,
            burstCapacity = burstCapacity
        )
    }

    /**
     * Resets the rate limiter metrics.
     */
    fun resetMetrics() {
        totalRequests = 0
        totalWaitTimeMs = 0
        rejectedRequests = 0
        logger.debug { "Rate limiter metrics reset" }
    }

    companion object {
        /**
         * Creates a rate limiter from requests per minute.
         *
         * @param requestsPerMinute Maximum requests per minute
         * @param burstCapacity Burst capacity (default: requestsPerMinute / 10)
         * @param perEndpointLimit Per-endpoint limiting (default: false)
         * @return Configured RateLimiter instance
         */
        fun fromRequestsPerMinute(
            requestsPerMinute: Int,
            burstCapacity: Int? = null,
            perEndpointLimit: Boolean = false
        ): RateLimiter {
            val requestsPerSecond = requestsPerMinute / 60.0
            val burst = burstCapacity ?: (requestsPerMinute / 10).coerceAtLeast(10)
            
            return RateLimiter(
                requestsPerSecond = requestsPerSecond,
                burstCapacity = burst,
                perEndpointLimit = perEndpointLimit
            )
        }
    }
}

/**
 * Data class containing rate limiter metrics.
 */
data class RateLimiterMetrics(
    val totalRequests: Long,
    val rejectedRequests: Long,
    val totalWaitTimeMs: Long,
    val averageWaitTimeMs: Long,
    val rejectionRate: Double,
    val requestsPerSecond: Double,
    val burstCapacity: Int
) {
    override fun toString(): String {
        return "RateLimiterMetrics(" +
                "total=$totalRequests, " +
                "rejected=$rejectedRequests (${String.format("%.2f%%", rejectionRate * 100)}), " +
                "avgWait=${averageWaitTimeMs}ms, " +
                "rate=${requestsPerSecond}/s, " +
                "burst=$burstCapacity)"
    }
}

