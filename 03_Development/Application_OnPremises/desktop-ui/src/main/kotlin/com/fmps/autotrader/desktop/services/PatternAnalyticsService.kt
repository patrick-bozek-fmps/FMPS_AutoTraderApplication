package com.fmps.autotrader.desktop.services

import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface PatternAnalyticsService {
    fun patternSummaries(): Flow<List<PatternSummary>>
    suspend fun patternDetail(id: String): PatternDetail
    suspend fun archivePattern(id: String): Result<Unit>
    suspend fun deletePattern(id: String): Result<Unit>
    suspend fun refresh(): Result<Unit>
}

data class PatternSummary(
    val id: String,
    val name: String,
    val exchange: String,
    val symbol: String,
    val timeframe: String,
    val trader: String,
    val successRate: Double,
    val profitFactor: Double,
    val occurrences: Int,
    val lastUpdated: Instant,
    val status: PatternPerformanceStatus
)

enum class PatternPerformanceStatus { TOP, STABLE, WARNING }

data class PatternDetail(
    val summary: PatternSummary,
    val description: String,
    val indicators: List<String>,
    val entryCriteria: List<String>,
    val exitCriteria: List<String>,
    val averageHoldMinutes: Int,
    val winRate: Double,
    val averagePnL: Double,
    val drawdown: Double,
    val performance: List<PatternPerformancePoint>,
    val distribution: List<PatternDistributionEntry>
)

data class PatternPerformancePoint(
    val timestamp: Instant,
    val successRate: Double,
    val profitFactor: Double
)

data class PatternDistributionEntry(
    val bucket: String,
    val value: Double
)


