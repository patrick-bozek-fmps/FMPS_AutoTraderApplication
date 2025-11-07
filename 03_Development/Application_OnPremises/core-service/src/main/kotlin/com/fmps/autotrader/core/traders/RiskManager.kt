package com.fmps.autotrader.core.traders

import com.fmps.autotrader.shared.model.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

/**
 * Centralised risk management component responsible for enforcing
 * application-wide and per-trader risk constraints.
 */
class RiskManager(
    private val positionProvider: RiskPositionProvider,
    private val riskConfig: RiskConfig,
    private val clock: Clock = Clock.systemUTC(),
    private val monitoringScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {

    private val mutex = Mutex()
    private val registeredTraders = mutableSetOf<String>()
    private val emergencyStoppedTraders = mutableSetOf<String>()

    private var stopTraderHandler: (suspend (String) -> Result<Unit>)? = null
    private var stopAllHandler: (suspend () -> Result<Unit>)? = null

    private var monitoringJob: Job? = null

    private val mathContext = MathContext(8, RoundingMode.HALF_EVEN)
    private val stopLossManager = StopLossManager(positionProvider, riskConfig, clock)

    init {
        if (positionProvider is PositionManager) {
            positionProvider.attachRiskManager(this)
        }
    }

    suspend fun registerTrader(traderId: String) {
        mutex.withLock {
            registeredTraders.add(traderId)
        }
    }

    suspend fun deregisterTrader(traderId: String) {
        mutex.withLock {
            registeredTraders.remove(traderId)
            emergencyStoppedTraders.remove(traderId)
        }
    }

    fun registerStopHandlers(
        traderStopHandler: suspend (String) -> Result<Unit>,
        globalStopHandler: (suspend () -> Result<Unit>)? = null
    ) {
        synchronized(this) {
            this.stopTraderHandler = traderStopHandler
            this.stopAllHandler = globalStopHandler
        }
    }

    suspend fun validateTraderCreation(config: AITraderConfig): Result<Unit> {
        if (riskConfig.maxTotalBudget <= BigDecimal.ZERO) {
            return Result.failure(
                RiskValidationException(
                    RiskViolation(
                        RiskViolationType.BUDGET,
                        "No money available to create trader"
                    )
                )
            )
        }

        val leverage = BigDecimal.valueOf(max(1, config.maxRiskLevel).toLong())
        return validateBudget(config.maxStakeAmount, null, leverage)
    }

    suspend fun validateBudget(
        requiredAmount: BigDecimal,
        traderId: String?,
        leverage: BigDecimal = BigDecimal.ONE
    ): Result<Unit> {
        if (requiredAmount <= BigDecimal.ZERO) {
            return Result.success(Unit)
        }

        val effective = requiredAmount.abs().multiply(leverage.max(BigDecimal.ONE))
        val currentTotalExposure = calculateTotalExposure()
        val projectedTotal = currentTotalExposure + effective

        if (projectedTotal > riskConfig.maxTotalBudget) {
            val violation = RiskViolation(
                RiskViolationType.BUDGET,
                "Total budget would be exceeded",
                mapOf(
                    "required" to effective,
                    "available" to (riskConfig.maxTotalBudget - currentTotalExposure)
                )
            )
            return Result.failure(RiskValidationException(violation))
        }

        if (traderId != null) {
            val traderExposure = calculateExposure(traderId)
            val projectedTraderExposure = traderExposure + effective
            if (projectedTraderExposure > riskConfig.maxExposurePerTrader) {
                val violation = RiskViolation(
                    RiskViolationType.EXPOSURE,
                    "Trader exposure limit would be exceeded",
                    mapOf(
                        "traderId" to traderId,
                        "required" to effective,
                        "available" to (riskConfig.maxExposurePerTrader - traderExposure)
                    )
                )
                return Result.failure(RiskValidationException(violation))
            }
        }

        return Result.success(Unit)
    }

    suspend fun validateLeverage(leverage: BigDecimal, traderId: String?): Result<Unit> {
        val normalizedLeverage = leverage.max(BigDecimal.ONE)

        traderId?.let {
            val current = currentMaxLeverageForTrader(it)
            val maxAllowed = BigDecimal.valueOf(riskConfig.maxLeveragePerTrader.toLong())
            if (current.max(normalizedLeverage) > maxAllowed) {
                return Result.failure(
                    RiskValidationException(
                        RiskViolation(
                            RiskViolationType.LEVERAGE,
                            "Trader leverage limit exceeded",
                            mapOf(
                                "traderId" to traderId,
                                "current" to current,
                                "requested" to normalizedLeverage,
                                "limit" to maxAllowed
                            )
                        )
                    )
                )
            }
        }

        val totalCurrent = currentMaxLeverageOverall()
        val totalAllowed = BigDecimal.valueOf(riskConfig.maxTotalLeverage.toLong())
        if (totalCurrent.max(normalizedLeverage) > totalAllowed) {
            return Result.failure(
                RiskValidationException(
                    RiskViolation(
                        RiskViolationType.LEVERAGE,
                        "Global leverage limit exceeded",
                        mapOf(
                            "current" to totalCurrent,
                            "requested" to normalizedLeverage,
                            "limit" to totalAllowed
                        )
                    )
                )
            )
        }

        return Result.success(Unit)
    }

    suspend fun canOpenPosition(
        traderId: String,
        notionalAmount: BigDecimal,
        leverage: BigDecimal
    ): Result<Boolean> {
        val violations = mutableListOf<RiskViolation>()

        if (isTraderEmergencyStopped(traderId)) {
            violations += RiskViolation(
                RiskViolationType.EMERGENCY,
                "Trader is currently under emergency stop",
                mapOf("traderId" to traderId)
            )
            logger.warn { "Blocking position opening for trader $traderId due to active emergency stop" }
            return Result.success(false)
        }

        val budgetResult = validateBudget(notionalAmount, traderId, leverage)
        if (budgetResult.isFailure) {
            violations += (budgetResult.exceptionOrNull() as? RiskValidationException)?.violation
                ?: RiskViolation(RiskViolationType.BUDGET, "Budget validation failed")
        }

        val leverageResult = validateLeverage(leverage, traderId)
        if (leverageResult.isFailure) {
            violations += (leverageResult.exceptionOrNull() as? RiskValidationException)?.violation
                ?: RiskViolation(RiskViolationType.LEVERAGE, "Leverage validation failed")
        }

        val projectedExposure = calculateExposure(traderId) + notionalAmount.multiply(leverage)
        if (projectedExposure > riskConfig.maxExposurePerTrader) {
            violations += RiskViolation(
                RiskViolationType.EXPOSURE,
                "Trader exposure limit would be exceeded",
                mapOf(
                    "traderId" to traderId,
                    "projected" to projectedExposure,
                    "limit" to riskConfig.maxExposurePerTrader
                )
            )
        }

        val projectedTotalExposure = calculateTotalExposure() + notionalAmount.multiply(leverage)
        if (projectedTotalExposure > riskConfig.maxTotalExposure) {
            violations += RiskViolation(
                RiskViolationType.EXPOSURE,
                "Total exposure limit would be exceeded",
                mapOf(
                    "projected" to projectedTotalExposure,
                    "limit" to riskConfig.maxTotalExposure
                )
            )
        }

        if (violations.isNotEmpty()) {
            return Result.success(false)
        }

        val score = calculateRiskScore(traderId)
        val allowed = score.recommendation != RiskRecommendation.BLOCK &&
            score.recommendation != RiskRecommendation.EMERGENCY_STOP

        return Result.success(allowed)
    }

    suspend fun calculateExposure(traderId: String): BigDecimal {
        val positions = positionProvider.getPositionsByTrader(traderId)
        return positions.fold(BigDecimal.ZERO) { acc, managedPosition ->
            acc + managedPosition.position.notionalValue()
        }
    }

    suspend fun calculateTotalExposure(): BigDecimal {
        val positions = positionProvider.getAllPositions()
        return positions.fold(BigDecimal.ZERO) { acc, managedPosition ->
            acc + managedPosition.position.notionalValue()
        }
    }

    suspend fun checkRiskLimits(traderId: String): RiskCheckResult {
        val violations = mutableListOf<RiskViolation>()

        val exposure = calculateExposure(traderId)
        if (exposure > riskConfig.maxExposurePerTrader) {
            violations += RiskViolation(
                RiskViolationType.EXPOSURE,
                "Trader exposure limit exceeded",
                mapOf(
                    "traderId" to traderId,
                    "current" to exposure,
                    "limit" to riskConfig.maxExposurePerTrader
                )
            )
        }

        val totalExposure = calculateTotalExposure()
        if (totalExposure > riskConfig.maxTotalExposure) {
            violations += RiskViolation(
                RiskViolationType.EXPOSURE,
                "Total exposure limit exceeded",
                mapOf("current" to totalExposure, "limit" to riskConfig.maxTotalExposure)
            )
        }

        val dailyLoss = stopLossManager.calculateRollingPnL(traderId)
        if (riskConfig.maxDailyLoss > BigDecimal.ZERO && dailyLoss < riskConfig.maxDailyLoss.negate()) {
            violations += RiskViolation(
                RiskViolationType.DAILY_LOSS,
                "Daily loss limit exceeded",
                mapOf("loss" to dailyLoss, "limit" to riskConfig.maxDailyLoss)
            )
        }

        if (isTraderEmergencyStopped(traderId)) {
            violations += RiskViolation(
                RiskViolationType.EMERGENCY,
                "Trader is currently under emergency stop",
                mapOf("traderId" to traderId)
            )
        }

        val riskScore = calculateRiskScore(traderId)
        val allowed = violations.isEmpty() &&
            riskScore.recommendation != RiskRecommendation.BLOCK &&
            riskScore.recommendation != RiskRecommendation.EMERGENCY_STOP

        return RiskCheckResult(allowed, violations, riskScore)
    }

    suspend fun emergencyStop(traderId: String? = null): Result<Unit> {
        return mutex.withLock {
            if (traderId == null) {
                logger.error { "Emergency stop invoked for all traders" }
                stopAllHandler?.invoke()?.onFailure { logger.error(it) { "Failed to stop all traders" } }
                val positions = positionProvider.getAllPositions()
                positions.forEach { position ->
                    positionProvider.closePosition(position.positionId, "EMERGENCY_STOP")
                        .onFailure { logger.error(it) { "Failed to close position ${position.positionId}" } }
                }
                emergencyStoppedTraders.addAll(registeredTraders)
            } else {
                logger.error { "Emergency stop invoked for trader $traderId" }
                stopLossManager.executeStopLoss(traderId, "EMERGENCY_STOP")
                stopTraderHandler?.invoke(traderId)
                    ?.onFailure { logger.error(it) { "Failed to stop trader $traderId" } }
                emergencyStoppedTraders.add(traderId)
            }
            Result.success(Unit)
        }
    }

    fun startMonitoring() {
        if (monitoringJob != null) return
        val interval = Duration.ofSeconds(riskConfig.monitoringIntervalSeconds)
        monitoringJob = monitoringScope.launch {
            while (true) {
                try {
                    val traders = mutex.withLock { registeredTraders.toSet() }
                    for (traderId in traders) {
                        if (stopLossManager.checkTraderStopLoss(traderId)) {
                            logger.error { "Trader $traderId exceeded rolling loss threshold; initiating emergency stop" }
                            emergencyStop(traderId)
                            continue
                        }

                        val positions = positionProvider.getPositionsByTrader(traderId)
                        positions.filter { stopLossManager.checkPositionStopLoss(it) }
                            .forEach { position ->
                                logger.warn {
                                    "Stop-loss triggered for position ${position.positionId} (trader $traderId); closing position"
                                }
                                positionProvider.closePosition(position.positionId, "STOP_LOSS")
                                    .onFailure { logger.error(it) { "Failed to close position ${position.positionId}" } }
                            }

                        val result = checkRiskLimits(traderId)
                        if (!result.isAllowed) {
                            logger.warn { "Risk violations detected for trader $traderId: ${result.violations}" }
                        }
                        if (result.riskScore?.recommendation == RiskRecommendation.EMERGENCY_STOP) {
                            logger.error { "Triggering emergency stop for trader $traderId due to high risk" }
                            emergencyStop(traderId)
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error during risk monitoring" }
                }
                delay(interval.toMillis())
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    suspend fun calculateRiskScore(traderId: String): RiskScore {
        val totalExposure = calculateTotalExposure()
        val traderExposure = calculateExposure(traderId)
        val budgetScore = ratio(traderExposure, riskConfig.maxExposurePerTrader)
        val totalBudgetScore = ratio(totalExposure, riskConfig.maxTotalBudget)
        val leverageScore = ratio(currentMaxLeverageForTrader(traderId), BigDecimal.valueOf(riskConfig.maxLeveragePerTrader.toLong()))
        val totalLeverageScore = ratio(currentMaxLeverageOverall(), BigDecimal.valueOf(riskConfig.maxTotalLeverage.toLong()))
        val dailyPnL = stopLossManager.calculateRollingPnL(traderId)
        val realizedLoss = if (dailyPnL < BigDecimal.ZERO) dailyPnL.abs() else BigDecimal.ZERO
        val pnlScore = if (riskConfig.maxDailyLoss > BigDecimal.ZERO) {
            ratio(realizedLoss, riskConfig.maxDailyLoss)
        } else 0.0

        val combinedBudget = max(budgetScore, totalBudgetScore)
        val combinedLeverage = max(leverageScore, totalLeverageScore)
        val exposureScore = max(combinedBudget, ratio(totalExposure, riskConfig.maxTotalExposure))

        val overall = min(1.0, 0.35 * combinedBudget + 0.30 * combinedLeverage + 0.20 * exposureScore + 0.15 * pnlScore)
        val recommendation = when {
            overall >= 0.9 || pnlScore >= 1.0 -> RiskRecommendation.EMERGENCY_STOP
            overall >= 0.75 -> RiskRecommendation.BLOCK
            overall >= 0.5 -> RiskRecommendation.WARN
            else -> RiskRecommendation.ALLOW
        }

        return RiskScore(
            overallScore = overall,
            budgetScore = combinedBudget,
            leverageScore = combinedLeverage,
            exposureScore = exposureScore,
            pnlScore = pnlScore,
            recommendation = recommendation
        )
    }

    private suspend fun currentMaxLeverageForTrader(traderId: String): BigDecimal {
        val positions = positionProvider.getPositionsByTrader(traderId)
        return positions.maxOfOrNull { it.position.leverage } ?: BigDecimal.ONE
    }

    private suspend fun currentMaxLeverageOverall(): BigDecimal {
        val positions = positionProvider.getAllPositions()
        return positions.maxOfOrNull { it.position.leverage } ?: BigDecimal.ONE
    }

    private suspend fun isTraderEmergencyStopped(traderId: String): Boolean {
        return mutex.withLock { emergencyStoppedTraders.contains(traderId) }
    }

    private fun ratio(value: BigDecimal, limit: BigDecimal): Double {
        if (limit <= BigDecimal.ZERO) return 0.0
        return value.abs().divide(limit, mathContext).toDouble().coerceIn(0.0, 1.0)
    }

}

private fun Position.notionalValue(): BigDecimal {
    val notional = quantity.multiply(currentPrice).abs()
    return notional.multiply(leverage.max(BigDecimal.ONE))
}

