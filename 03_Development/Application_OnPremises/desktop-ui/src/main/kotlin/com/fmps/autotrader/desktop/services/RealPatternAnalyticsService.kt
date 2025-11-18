package com.fmps.autotrader.desktop.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import com.fmps.autotrader.shared.dto.PatternDTO
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

/**
 * Real PatternAnalyticsService implementation using core-service REST API.
 * 
 * Features:
 * - Real pattern data via `/api/v1/patterns` endpoint (uses actual PatternRepository)
 * - Archive/delete operations via `/api/v1/patterns/{id}/deactivate` and `/api/v1/patterns/{id}` DELETE
 * - Graceful fallback when endpoints return empty data (mapper TODO in backend)
 * - Retry logic with exponential backoff for transient failures
 * 
 * Note: Pattern endpoints may return empty lists until mapper is implemented in backend.
 * This service gracefully handles empty responses and maintains local state as fallback.
 * 
 * Security Note: Archive/delete operations lack confirmation dialogs and audit trails.
 * These are tracked under Epic 6 security tasks.
 */
class RealPatternAnalyticsService(
    private val httpClient: HttpClient,
    private val baseUrl: String = "http://localhost:8080",
    private val apiKey: String? = null
) : PatternAnalyticsService {

    private val json = Json { ignoreUnknownKeys = true }
    private val summariesFlow = MutableStateFlow<List<PatternSummary>>(emptyList())
    private val detailCache = mutableMapOf<String, PatternDetail>()
    
    private val maxRetries = 3
    private val initialRetryDelayMs = 500L
    private val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        // Load initial patterns
        scope.launch {
            loadPatterns()
        }
    }
    
    /**
     * Executes an operation with retry logic and exponential backoff.
     */
    private suspend fun <T> executeWithRetry(
        operation: suspend () -> T,
        onRetry: (Int, Throwable) -> Unit = { attempt, error -> 
            logger.debug(error) { "Retry attempt $attempt failed" }
        }
    ): T {
        var lastException: Throwable? = null
        for (attempt in 0 until maxRetries) {
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1 && isRetryableError(e)) {
                    onRetry(attempt + 1, e)
                    delay(initialRetryDelayMs * (1 shl attempt)) // Exponential backoff
                } else {
                    break
                }
            }
        }
        throw lastException ?: Exception("Operation failed after $maxRetries attempts")
    }
    
    /**
     * Determines if an error is retryable (transient network/server errors).
     */
    private fun isRetryableError(e: Exception): Boolean {
        return when {
            e is ConnectTimeoutException -> true
            e is SocketTimeoutException -> true
            e.message?.contains("timeout", ignoreCase = true) == true -> true
            e.message?.contains("connection", ignoreCase = true) == true -> true
            else -> false
        }
    }

    override fun patternSummaries(): Flow<List<PatternSummary>> = summariesFlow.asStateFlow()

    override suspend fun patternDetail(id: String): PatternDetail {
        // Check cache first
        detailCache[id]?.let { return it }
        
        // Try to fetch from REST API
        return try {
            val response = executeWithRetry<HttpResponse>(
                operation = {
                    httpClient.get("$baseUrl/api/v1/patterns/$id") {
                        apiKey?.let { header("X-API-Key", it) }
                    }
                }
            )

            if (response.status.isSuccess()) {
                val apiResponse = response.body<ApiResponse<PatternDTO>>()
                if (apiResponse.success && apiResponse.data != null) {
                    val detail = convertToPatternDetail(apiResponse.data)
                    detailCache[id] = detail
                    detail
                } else {
                    // Fallback: create detail from summary if available
                    createDetailFromSummary(id)
                }
            } else {
                // Fallback: create detail from summary if available
                createDetailFromSummary(id)
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to fetch pattern detail for $id, using cached or fallback" }
            createDetailFromSummary(id)
        }
    }

    override suspend fun archivePattern(id: String): Result<Unit> {
        // Archive = deactivate pattern
        return try {
            val response = executeWithRetry<HttpResponse>(
                operation = {
                    httpClient.patch("$baseUrl/api/v1/patterns/$id/deactivate") {
                        apiKey?.let { header("X-API-Key", it) }
                    }
                }
            )

            if (response.status.isSuccess()) {
                logger.info { "Pattern $id archived (deactivated) successfully" }
                // Update local state
                summariesFlow.update { list ->
                    list.map { if (it.id == id) it.copy(status = PatternPerformanceStatus.STABLE) else it }
                }
                // TODO: Audit log pattern archival when audit system is implemented (Epic 6)
                Result.success(Unit)
            } else {
                val error = response.body<ErrorResponse>()
                logger.warn { "Failed to archive pattern $id: ${error.error.message}" }
                Result.failure(Exception("Failed to archive pattern: ${error.error.message}"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error archiving pattern $id" }
            Result.failure(e)
        }
    }

    override suspend fun deletePattern(id: String): Result<Unit> {
        return try {
            val response = executeWithRetry<HttpResponse>(
                operation = {
                    httpClient.delete("$baseUrl/api/v1/patterns/$id") {
                        apiKey?.let { header("X-API-Key", it) }
                    }
                }
            )

            if (response.status.isSuccess()) {
                logger.info { "Pattern $id deleted successfully" }
                // Update local state
                summariesFlow.update { list -> list.filterNot { it.id == id } }
                detailCache.remove(id)
                // TODO: Audit log pattern deletion when audit system is implemented (Epic 6)
                Result.success(Unit)
            } else {
                val error = response.body<ErrorResponse>()
                logger.warn { "Failed to delete pattern $id: ${error.error.message}" }
                Result.failure(Exception("Failed to delete pattern: ${error.error.message}"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error deleting pattern $id" }
            Result.failure(e)
        }
    }

    override suspend fun refresh(): Result<Unit> {
        return try {
            loadPatterns()
            detailCache.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error refreshing patterns" }
            Result.failure(e)
        }
    }

    /**
     * Loads patterns from REST API.
     */
    private suspend fun loadPatterns() {
        try {
            val response = executeWithRetry<HttpResponse>(
                operation = {
                    httpClient.get("$baseUrl/api/v1/patterns") {
                        apiKey?.let { header("X-API-Key", it) }
                    }
                }
            )

            if (response.status.isSuccess()) {
                val apiResponse = response.body<ApiResponse<List<PatternDTO>>>()
                if (apiResponse.success && apiResponse.data != null && apiResponse.data.isNotEmpty()) {
                    val summaries = apiResponse.data.map { convertToPatternSummary(it) }
                    summariesFlow.value = summaries
                    logger.debug { "Loaded ${summaries.size} patterns from API" }
                } else {
                    // Endpoint returned empty list (mapper not implemented yet)
                    logger.debug { "Pattern endpoint returned empty list (mapper TODO in backend), keeping existing state" }
                }
            } else {
                logger.debug { "Pattern endpoint not available, keeping existing state" }
            }
        } catch (e: Exception) {
            logger.debug(e) { "Could not load patterns from API, keeping existing state" }
        }
    }

    /**
     * Converts PatternDTO to PatternSummary.
     */
    private fun convertToPatternSummary(dto: PatternDTO): PatternSummary {
        // Calculate profit factor from average profit/loss
        val profitFactor = if (dto.averageLoss.toDouble() > 0) {
            dto.averageProfit.toDouble() / dto.averageLoss.toDouble()
        } else {
            1.0
        }
        
        // Determine status based on success rate and profit factor
        val status = when {
            dto.successRate.toDouble() >= 70.0 && profitFactor >= 2.0 -> PatternPerformanceStatus.TOP
            dto.successRate.toDouble() >= 50.0 && profitFactor >= 1.2 -> PatternPerformanceStatus.STABLE
            else -> PatternPerformanceStatus.WARNING
        }
        
        // Parse timeframe from description or use default
        val timeframe = extractTimeframe(dto.description) ?: "1h"
        
        // Parse trader name (not available in DTO, use placeholder)
        val trader = "Pattern Trader" // TODO: Link to actual trader when available
        
        return PatternSummary(
            id = dto.id?.toString() ?: "unknown",
            name = dto.name,
            exchange = dto.exchange,
            symbol = dto.tradingPair,
            timeframe = timeframe,
            trader = trader,
            successRate = dto.successRate.toDouble(),
            profitFactor = profitFactor,
            occurrences = dto.totalTrades.toInt(),
            lastUpdated = parseInstant(dto.updatedAt),
            status = status
        )
    }

    /**
     * Converts PatternDTO to PatternDetail.
     */
    private fun convertToPatternDetail(dto: PatternDTO): PatternDetail {
        val summary = convertToPatternSummary(dto)
        
        // Parse indicators from JSON string
        val indicators = parseIndicators(dto.indicators)
        
        // Calculate derived metrics
        val winRate = if (dto.totalTrades > 0) {
            (dto.winningTrades.toDouble() / dto.totalTrades) * 100.0
        } else {
            0.0
        }
        
        // Calculate average PnL from expected value or average profit
        val averagePnL = if (dto.totalTrades > 0) {
            val expectedValue = dto.expectedValue()
            expectedValue.toDouble()
        } else {
            dto.averageProfit.toDouble()
        }
        
        // Generate performance points (simplified - would need historical data)
        val performance = generatePerformancePoints(summary.successRate, summary.profitFactor)
        
        // Generate distribution (simplified - would need actual distribution data)
        val distribution = listOf(
            PatternDistributionEntry(dto.exchange, 1.0) // Single exchange for now
        )
        
        return PatternDetail(
            summary = summary,
            description = dto.description ?: "Pattern for ${dto.tradingPair} on ${dto.exchange}",
            indicators = indicators,
            entryCriteria = extractEntryCriteria(dto),
            exitCriteria = extractExitCriteria(dto),
            averageHoldMinutes = 60, // TODO: Calculate from actual trade data
            winRate = winRate,
            averagePnL = averagePnL,
            drawdown = 0.0, // TODO: Calculate from actual trade data
            performance = performance,
            distribution = distribution
        )
    }

    /**
     * Creates a PatternDetail from cached summary (fallback when API doesn't return detail).
     */
    private suspend fun createDetailFromSummary(id: String): PatternDetail {
        val summary = summariesFlow.value.firstOrNull { it.id == id }
            ?: throw IllegalArgumentException("Pattern $id not found")
        
        // Create basic detail from summary
        return PatternDetail(
            summary = summary,
            description = "Pattern focusing on ${summary.symbol} on ${summary.exchange}",
            indicators = listOf("RSI", "MACD"), // Placeholder
            entryCriteria = listOf("Pattern match detected"), // Placeholder
            exitCriteria = listOf("Target profit", "Stop loss"), // Placeholder
            averageHoldMinutes = 60, // Placeholder
            winRate = summary.successRate,
            averagePnL = summary.profitFactor * 10.0, // Placeholder calculation
            drawdown = 0.0, // Placeholder
            performance = generatePerformancePoints(summary.successRate, summary.profitFactor),
            distribution = listOf(PatternDistributionEntry(summary.exchange, 1.0))
        ).also { detailCache[id] = it }
    }

    /**
     * Parses indicators from JSON string.
     * TODO: Implement full JSON parsing when indicator structure is finalized
     */
    private fun parseIndicators(indicatorsJson: String): List<String> {
        // Simplified parsing for now - return common indicators
        // Full parsing will be implemented when indicator JSON structure is finalized
        return listOf("RSI", "MACD") // Placeholder
    }

    /**
     * Extracts entry criteria from PatternDTO.
     */
    private fun extractEntryCriteria(dto: PatternDTO): List<String> {
        val criteria = mutableListOf<String>()
        
        if (dto.candlestickPattern != null) {
            criteria.add("Candlestick pattern: ${dto.candlestickPattern}")
        }
        
        // Add indicator-based criteria
        val indicators = parseIndicators(dto.indicators)
        criteria.addAll(indicators.map { "$it signal" })
        
        if (criteria.isEmpty()) {
            criteria.add("Pattern match detected")
        }
        
        return criteria
    }

    /**
     * Extracts exit criteria from PatternDTO.
     */
    private fun extractExitCriteria(dto: PatternDTO): List<String> {
        return listOf(
            "Take profit target",
            "Stop loss protection"
        ) // Placeholder - would need actual exit criteria from pattern definition
    }

    /**
     * Extracts timeframe from description or returns default.
     */
    private fun extractTimeframe(description: String?): String? {
        if (description == null) return null
        
        val timeframePattern = Regex("""(\d+[mhd])""", RegexOption.IGNORE_CASE)
        return timeframePattern.find(description)?.value
    }

    /**
     * Parses ISO date-time string to Instant.
     */
    private fun parseInstant(dateTimeString: String): Instant {
        return try {
            Instant.parse(dateTimeString)
        } catch (e: Exception) {
            try {
                LocalDateTime.parse(dateTimeString, dateTimeFormatter).atZone(java.time.ZoneId.systemDefault()).toInstant()
            } catch (e2: Exception) {
                logger.debug(e2) { "Failed to parse date: $dateTimeString" }
                Instant.now() // Fallback to current time
            }
        }
    }

    /**
     * Generates performance points for chart display.
     */
    private fun generatePerformancePoints(successRate: Double, profitFactor: Double): List<PatternPerformancePoint> {
        val now = Instant.now()
        return (0 until 20).map { index ->
            val timestamp = now.minusSeconds(index * 3600L) // Hourly points
            PatternPerformancePoint(
                timestamp = timestamp,
                successRate = successRate + (kotlin.random.Random.nextDouble(-5.0, 5.0)),
                profitFactor = profitFactor + (kotlin.random.Random.nextDouble(-0.2, 0.2))
            )
        }.reversed()
    }

    @Serializable
    private data class ApiResponse<T>(
        val success: Boolean,
        val data: T? = null,
        val timestamp: String? = null
    )


    @Serializable
    private data class ErrorResponse(
        val success: Boolean,
        val error: ErrorDetail,
        val timestamp: String? = null
    )

    @Serializable
    private data class ErrorDetail(
        val code: String,
        val message: String
    )
}

