package com.fmps.autotrader.core.traders

import java.math.BigDecimal

/**
 * Recommendation returned by [RiskScore] to indicate the suggested action
 * for the calling component.
 */
enum class RiskRecommendation {
    ALLOW,
    WARN,
    BLOCK,
    EMERGENCY_STOP
}

/**
 * Classifies the type of risk violation detected during validation.
 */
enum class RiskViolationType {
    BUDGET,
    LEVERAGE,
    EXPOSURE,
    DAILY_LOSS,
    STOP_LOSS,
    EMERGENCY,
    UNKNOWN
}

/**
 * Represents a specific violation detected during a risk check.
 */
data class RiskViolation(
    val type: RiskViolationType,
    val message: String,
    val details: Map<String, Any?> = emptyMap()
)

/**
 * Result of a comprehensive risk check.
 */
data class RiskCheckResult(
    val isAllowed: Boolean,
    val violations: List<RiskViolation> = emptyList(),
    val riskScore: RiskScore? = null
)

/**
 * Captures the composite risk assessment used to drive trading decisions.
 */
data class RiskScore(
    val overallScore: Double,
    val budgetScore: Double,
    val leverageScore: Double,
    val exposureScore: Double,
    val pnlScore: Double,
    val recommendation: RiskRecommendation
) {
    init {
        require(overallScore.isFinite()) { "overallScore must be finite" }
        require(budgetScore.isFinite()) { "budgetScore must be finite" }
        require(leverageScore.isFinite()) { "leverageScore must be finite" }
        require(exposureScore.isFinite()) { "exposureScore must be finite" }
        require(pnlScore.isFinite()) { "pnlScore must be finite" }
    }
}

/**
 * Exception thrown when a risk validation fails.
 */
class RiskValidationException(val violation: RiskViolation) : Exception(violation.message)

/**
 * Configuration used by [RiskManager] to enforce risk policies.
 */
data class RiskConfig(
    val maxTotalBudget: BigDecimal,
    val maxLeveragePerTrader: Int,
    val maxTotalLeverage: Int,
    val maxExposurePerTrader: BigDecimal,
    val maxTotalExposure: BigDecimal,
    val maxDailyLoss: BigDecimal,
    val stopLossPercentage: Double,
    val monitoringIntervalSeconds: Long = 10L
) {
    init {
        require(maxTotalBudget >= BigDecimal.ZERO) { "maxTotalBudget cannot be negative" }
        require(maxLeveragePerTrader > 0) { "maxLeveragePerTrader must be positive" }
        require(maxTotalLeverage > 0) { "maxTotalLeverage must be positive" }
        require(maxExposurePerTrader >= BigDecimal.ZERO) { "maxExposurePerTrader cannot be negative" }
        require(maxTotalExposure >= BigDecimal.ZERO) { "maxTotalExposure cannot be negative" }
        require(stopLossPercentage >= 0.0) { "stopLossPercentage cannot be negative" }
        require(monitoringIntervalSeconds > 0) { "monitoringIntervalSeconds must be positive" }
    }
}

/**
 * Abstraction over position data needed by [RiskManager].
 */
interface RiskPositionProvider {
    suspend fun getPositionsByTrader(traderId: String): List<ManagedPosition>
    suspend fun getAllPositions(): List<ManagedPosition>
    suspend fun getHistoryByTrader(traderId: String): List<PositionHistory>
    suspend fun closePosition(positionId: String, reason: String): Result<ManagedPosition>
}

