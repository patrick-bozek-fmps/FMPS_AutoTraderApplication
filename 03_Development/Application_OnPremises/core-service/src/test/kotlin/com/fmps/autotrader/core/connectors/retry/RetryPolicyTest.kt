package com.fmps.autotrader.core.connectors.retry

import com.fmps.autotrader.core.connectors.exceptions.*
import com.fmps.autotrader.shared.enums.Exchange
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.system.measureTimeMillis

class RetryPolicyTest {

    @Test
    fun `test initialization with valid parameters`() {
        val policy = RetryPolicy(
            maxRetries = 3,
            baseDelayMs = 1000,
            maxDelayMs = 30000,
            exponentialBackoff = true,
            jitterFactor = 0.1
        )

        assertEquals(3, policy.maxRetries)
        assertEquals(1000, policy.baseDelayMs)
        assertEquals(30000, policy.maxDelayMs)
        assertTrue(policy.exponentialBackoff)
        assertEquals(0.1, policy.jitterFactor, 0.01)
    }

    @Test
    fun `test initialization fails with invalid maxRetries`() {
        assertThrows<IllegalArgumentException> {
            RetryPolicy(maxRetries = -1)
        }
    }

    @Test
    fun `test initialization fails with invalid baseDelayMs`() {
        assertThrows<IllegalArgumentException> {
            RetryPolicy(baseDelayMs = 0)
        }

        assertThrows<IllegalArgumentException> {
            RetryPolicy(baseDelayMs = -1000)
        }
    }

    @Test
    fun `test initialization fails with invalid maxDelayMs`() {
        assertThrows<IllegalArgumentException> {
            RetryPolicy(baseDelayMs = 5000, maxDelayMs = 1000)
        }
    }

    @Test
    fun `test initialization fails with invalid jitterFactor`() {
        assertThrows<IllegalArgumentException> {
            RetryPolicy(jitterFactor = -0.1)
        }

        assertThrows<IllegalArgumentException> {
            RetryPolicy(jitterFactor = 1.5)
        }
    }

    @Test
    fun `test execute succeeds on first attempt`() = runBlocking {
        val policy = RetryPolicy(maxRetries = 3)
        var attempts = 0

        val result = policy.execute("test_operation") {
            attempts++
            "success"
        }

        assertEquals("success", result)
        assertEquals(1, attempts, "Should succeed on first attempt")
    }

    @Test
    fun `test execute retries on retryable exception`() = runBlocking {
        val policy = RetryPolicy(maxRetries = 2, baseDelayMs = 100)
        var attempts = 0

        val result = policy.execute("test_operation") {
            attempts++
            if (attempts < 2) {
                throw ConnectionException("Temporary failure", retryable = true)
            }
            "success"
        }

        assertEquals("success", result)
        assertEquals(2, attempts, "Should succeed on second attempt")
    }

    @Test
    fun `test execute throws non-retryable exception immediately`() = runBlocking {
        val policy = RetryPolicy(maxRetries = 3)
        var attempts = 0

        val exception = assertThrows<AuthenticationException> {
            policy.execute("test_operation") {
                attempts++
                throw AuthenticationException("Invalid API key")
            }
        }

        assertEquals(1, attempts, "Should not retry non-retryable exception")
        assertTrue(exception.message!!.contains("Invalid API key"))
    }

    @Test
    fun `test execute throws after max retries exhausted`() = runBlocking {
        val policy = RetryPolicy(maxRetries = 2, baseDelayMs = 100)
        var attempts = 0

        val exception = assertThrows<ConnectionException> {
            policy.execute("test_operation") {
                attempts++
                throw ConnectionException("Persistent failure", retryable = true)
            }
        }

        assertEquals(3, attempts, "Should attempt maxRetries + 1 times")
        assertTrue(exception.message!!.contains("Persistent failure"))
    }

    @Test
    fun `test shouldRetry for ConnectionException`() {
        val policy = RetryPolicy()

        assertTrue(policy.shouldRetry(ConnectionException("test", retryable = true)))
        assertFalse(policy.shouldRetry(ConnectionException("test", retryable = false)))
    }

    @Test
    fun `test shouldRetry for RateLimitException`() {
        val policy = RetryPolicy()

        assertTrue(policy.shouldRetry(RateLimitException("Rate limit exceeded")))
    }

    @Test
    fun `test shouldRetry for AuthenticationException`() {
        val policy = RetryPolicy()

        assertFalse(policy.shouldRetry(AuthenticationException("Invalid credentials")))
    }

    @Test
    fun `test shouldRetry for InsufficientFundsException`() {
        val policy = RetryPolicy()

        assertFalse(policy.shouldRetry(InsufficientFundsException("Insufficient balance")))
    }

    @Test
    fun `test shouldRetry for OrderException with retryable error code`() {
        val policy = RetryPolicy()

        // Retryable order exception (generic error code)
        val retryableException = OrderException("Temporary issue", errorCode = "TEMPORARY_ERROR")
        assertTrue(policy.shouldRetry(retryableException))
    }

    @Test
    fun `test shouldRetry for OrderException with non-retryable error code`() {
        val policy = RetryPolicy()

        // Non-retryable order exceptions
        assertFalse(policy.shouldRetry(OrderException("Invalid params", errorCode = "INVALID_PARAMETERS")))
        assertFalse(policy.shouldRetry(OrderException("Not found", errorCode = "ORDER_NOT_FOUND")))
        assertFalse(policy.shouldRetry(OrderException("Duplicate", errorCode = "DUPLICATE_ORDER")))
    }

    @Test
    fun `test shouldRetry for UnsupportedExchangeException`() {
        val policy = RetryPolicy()

        assertFalse(policy.shouldRetry(UnsupportedExchangeException.forExchange("UNKNOWN")))
    }

    @Test
    fun `test shouldRetry for generic ExchangeException`() {
        val policy = RetryPolicy()

        assertTrue(policy.shouldRetry(ExchangeException("Generic exchange error", exchangeName = "BINANCE")))
    }

    @Test
    fun `test shouldRetry for unknown exception`() {
        val policy = RetryPolicy()

        assertFalse(policy.shouldRetry(IllegalArgumentException("Unknown exception")))
    }

    @Test
    fun `test calculateDelay with exponential backoff`() {
        val policy = RetryPolicy(
            maxRetries = 5,
            baseDelayMs = 1000,
            maxDelayMs = 30000,
            exponentialBackoff = true,
            jitterFactor = 0.0  // No jitter for predictable testing
        )

        // Attempt 0: 1000 * 2^0 = 1000ms
        val delay0 = policy.calculateDelay(0)
        assertEquals(1000, delay0)

        // Attempt 1: 1000 * 2^1 = 2000ms
        val delay1 = policy.calculateDelay(1)
        assertEquals(2000, delay1)

        // Attempt 2: 1000 * 2^2 = 4000ms
        val delay2 = policy.calculateDelay(2)
        assertEquals(4000, delay2)

        // Attempt 3: 1000 * 2^3 = 8000ms
        val delay3 = policy.calculateDelay(3)
        assertEquals(8000, delay3)

        // Attempt 4: 1000 * 2^4 = 16000ms
        val delay4 = policy.calculateDelay(4)
        assertEquals(16000, delay4)
    }

    @Test
    fun `test calculateDelay respects maxDelayMs`() {
        val policy = RetryPolicy(
            baseDelayMs = 1000,
            maxDelayMs = 5000,
            exponentialBackoff = true,
            jitterFactor = 0.0
        )

        // Attempt 5: 1000 * 2^5 = 32000ms, but capped at maxDelayMs
        val delay = policy.calculateDelay(5)
        assertEquals(5000, delay, "Delay should be capped at maxDelayMs")
    }

    @Test
    fun `test calculateDelay with linear backoff`() {
        val policy = RetryPolicy(
            baseDelayMs = 1000,
            exponentialBackoff = false,
            jitterFactor = 0.0
        )

        // All attempts should have same delay
        assertEquals(1000, policy.calculateDelay(0))
        assertEquals(1000, policy.calculateDelay(1))
        assertEquals(1000, policy.calculateDelay(2))
    }

    @Test
    fun `test calculateDelay with jitter adds randomness`() {
        val policy = RetryPolicy(
            baseDelayMs = 1000,
            jitterFactor = 0.1,
            exponentialBackoff = false
        )

        val delays = (0..10).map { policy.calculateDelay(0) }

        // With jitter, not all delays should be exactly 1000ms
        val uniqueDelays = delays.toSet()
        assertTrue(uniqueDelays.size > 1, "Jitter should produce different delays")

        // All delays should be within jitter range (1000 ± 100)
        delays.forEach { delay ->
            assertTrue(delay in 900..1100, "Delay $delay should be within jitter range")
        }
    }

    @Test
    fun `test calculateDelay never returns negative value`() {
        val policy = RetryPolicy(
            baseDelayMs = 100,
            jitterFactor = 1.0,  // Maximum jitter
            exponentialBackoff = false
        )

        // Even with maximum jitter, delay should never be negative
        repeat(100) {
            val delay = policy.calculateDelay(0)
            assertTrue(delay >= 0, "Delay should never be negative: $delay")
        }
    }

    @Test
    fun `test copy creates new instance with updated parameters`() {
        val original = RetryPolicy(
            maxRetries = 3,
            baseDelayMs = 1000,
            maxDelayMs = 30000,
            exponentialBackoff = true,
            jitterFactor = 0.1
        )

        val modified = original.copy(
            maxRetries = 5,
            baseDelayMs = 2000
        )

        assertEquals(5, modified.maxRetries)
        assertEquals(2000, modified.baseDelayMs)
        assertEquals(30000, modified.maxDelayMs)  // Unchanged
        assertTrue(modified.exponentialBackoff)   // Unchanged
        assertEquals(0.1, modified.jitterFactor, 0.01)  // Unchanged
    }

    @Test
    fun `test copy with no parameters creates identical instance`() {
        val original = RetryPolicy(
            maxRetries = 3,
            baseDelayMs = 1000
        )

        val copy = original.copy()

        assertEquals(original.maxRetries, copy.maxRetries)
        assertEquals(original.baseDelayMs, copy.baseDelayMs)
        assertEquals(original.maxDelayMs, copy.maxDelayMs)
        assertEquals(original.exponentialBackoff, copy.exponentialBackoff)
        assertEquals(original.jitterFactor, copy.jitterFactor, 0.01)
    }

    @Test
    fun `test DEFAULT policy has expected values`() {
        val policy = RetryPolicy.DEFAULT

        assertEquals(3, policy.maxRetries)
        assertEquals(1000, policy.baseDelayMs)
        assertEquals(30000, policy.maxDelayMs)
        assertTrue(policy.exponentialBackoff)
        assertEquals(0.1, policy.jitterFactor, 0.01)
    }

    @Test
    fun `test AGGRESSIVE policy has expected values`() {
        val policy = RetryPolicy.AGGRESSIVE

        assertEquals(5, policy.maxRetries)
        assertEquals(500, policy.baseDelayMs)
        assertEquals(60000, policy.maxDelayMs)
        assertTrue(policy.exponentialBackoff)
        assertEquals(0.2, policy.jitterFactor, 0.01)
    }

    @Test
    fun `test CONSERVATIVE policy has expected values`() {
        val policy = RetryPolicy.CONSERVATIVE

        assertEquals(2, policy.maxRetries)
        assertEquals(2000, policy.baseDelayMs)
        assertEquals(15000, policy.maxDelayMs)
        assertTrue(policy.exponentialBackoff)
        assertEquals(0.1, policy.jitterFactor, 0.01)
    }

    @Test
    fun `test NONE policy has expected values`() {
        val policy = RetryPolicy.NONE

        assertEquals(0, policy.maxRetries)
        assertFalse(policy.exponentialBackoff)
        assertEquals(0.0, policy.jitterFactor, 0.01)
    }

    @Test
    fun `test NONE policy does not retry`() = runBlocking {
        val policy = RetryPolicy.NONE
        var attempts = 0

        val exception = assertThrows<ConnectionException> {
            policy.execute("test_operation") {
                attempts++
                throw ConnectionException("Failure", retryable = true)
            }
        }

        assertEquals(1, attempts, "NONE policy should not retry")
    }

    @Test
    fun `test retryWithPolicy extension function`() = runBlocking {
        var attempts = 0

        val result = retryWithPolicy(
            operationName = "test_operation",
            retryPolicy = RetryPolicy(maxRetries = 2, baseDelayMs = 100)
        ) {
            attempts++
            if (attempts < 2) {
                throw ConnectionException("Temporary failure", retryable = true)
            }
            "success"
        }

        assertEquals("success", result)
        assertEquals(2, attempts)
    }

    @Test
    fun `test retry with actual delay timing`() = runBlocking {
        val policy = RetryPolicy(
            maxRetries = 2,
            baseDelayMs = 200,
            exponentialBackoff = true,
            jitterFactor = 0.0  // No jitter for predictable timing
        )

        var attempts = 0
        val timeTaken = measureTimeMillis {
            try {
                policy.execute("test_operation") {
                    attempts++
                    throw ConnectionException("Always fails", retryable = true)
                }
            } catch (e: ConnectionException) {
                // Expected
            }
        }

        assertEquals(3, attempts)
        // Expected delays: 200ms (first retry) + 400ms (second retry) = 600ms total
        assertTrue(timeTaken >= 550, "Time taken: ${timeTaken}ms (expected ~600ms)")
        assertTrue(timeTaken < 800, "Time taken: ${timeTaken}ms (should not be too slow)")
    }
}

