package com.fmps.autotrader.core.telemetry

import com.fmps.autotrader.core.traders.AITraderMetrics
import com.fmps.autotrader.core.traders.AITraderState
import com.fmps.autotrader.core.traders.ManagedPosition
import com.fmps.autotrader.core.traders.RiskRecommendation
import com.fmps.autotrader.core.traders.RiskViolation
import com.fmps.autotrader.core.traders.RiskViolationType
import com.fmps.autotrader.shared.dto.BigDecimalSerializer
import com.fmps.autotrader.shared.dto.InstantSerializer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import mu.KotlinLogging
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}

/**
 * Enumerates the telemetry channels supported by the WebSocket infrastructure.
 */
@Serializable
enum class TelemetryChannel(val wireName: String) {
    TRADER_STATUS("trader-status"),
    POSITIONS("positions"),
    RISK_ALERTS("risk-alerts"),
    MARKET_DATA("market-data");

    companion object {
        fun fromWireName(value: String): TelemetryChannel? = values().firstOrNull { it.wireName.equals(value, ignoreCase = true) }
    }
}

/**
 * Marker interface for telemetry events sent to clients.
 */
@Serializable
sealed interface TelemetryEvent {
    val channel: TelemetryChannel
    val id: String?
    val timestamp: Long
}

@Serializable
data class TraderStatusEvent(
    override val id: String,
    val name: String,
    val status: AITraderState,
    val exchange: String,
    val symbol: String,
    val strategy: String,
    val reason: String,
    val metrics: AITraderMetrics? = null,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant = Instant.now()
) : TelemetryEvent {
    override val channel: TelemetryChannel = TelemetryChannel.TRADER_STATUS
    override val timestamp: Long = updatedAt.toEpochMilli()
}

@Serializable
enum class PositionTelemetryStatus { OPEN, UPDATED, CLOSED }

@Serializable
data class PositionTelemetryEvent(
    override val id: String,
    val traderId: String,
    val symbol: String,
    val action: String,
    @Serializable(with = BigDecimalSerializer::class)
    val quantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val entryPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val currentPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val unrealizedPnL: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val realizedPnL: BigDecimal? = null,
    val status: PositionTelemetryStatus,
    val reason: String? = null,
    val trailingStopActivated: Boolean = false,
    @Serializable(with = BigDecimalSerializer::class)
    val stopLossPrice: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val takeProfitPrice: BigDecimal? = null,
    val isActive: Boolean,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant = Instant.now()
) : TelemetryEvent {
    override val channel: TelemetryChannel = TelemetryChannel.POSITIONS
    override val timestamp: Long = updatedAt.toEpochMilli()
}

@Serializable
enum class RiskAlertSeverity { INFO, WARNING, CRITICAL }

@Serializable
data class RiskViolationPayload(
    val type: RiskViolationType,
    val message: String,
    val details: Map<String, String?>
)

@Serializable
data class RiskAlertEvent(
    override val id: String,
    val traderId: String?,
    val severity: RiskAlertSeverity,
    val recommendation: RiskRecommendation? = null,
    val message: String,
    val violations: List<RiskViolationPayload> = emptyList(),
    val status: String = "ACTIVE",
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant = Instant.now()
) : TelemetryEvent {
    override val channel: TelemetryChannel = TelemetryChannel.RISK_ALERTS
    override val timestamp: Long = createdAt.toEpochMilli()
}

@Serializable
data class MarketDataEvent(
    override val id: String,
    val symbol: String,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
    val source: String,
    val change24h: Double? = null,
    val isSnapshot: Boolean = false,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant = Instant.now()
) : TelemetryEvent {
    override val channel: TelemetryChannel = TelemetryChannel.MARKET_DATA
    override val timestamp: Long = updatedAt.toEpochMilli()
}

/**
 * Centralised telemetry collector used by core components to publish events that are
 * later dispatched to WebSocket clients.
 */
object TelemetryCollector {
    private const val MAX_RISK_ALERT_HISTORY = 50

    private val eventsFlow = MutableSharedFlow<TelemetryEvent>(extraBufferCapacity = 128)

    private val traderSnapshots = ConcurrentHashMap<String, TraderStatusEvent>()
    private val positionSnapshots = ConcurrentHashMap<String, PositionTelemetryEvent>()
    private val marketSnapshots = ConcurrentHashMap<String, MarketDataEvent>()
    private val riskAlertSnapshots = ConcurrentHashMap<String, RiskAlertEvent>()
    private val riskAlertOrder = CopyOnWriteArrayList<String>()

    fun events(): SharedFlow<TelemetryEvent> = eventsFlow.asSharedFlow()

    fun snapshot(channel: TelemetryChannel): List<TelemetryEvent> = when (channel) {
        TelemetryChannel.TRADER_STATUS -> traderSnapshots.values.sortedBy { it.timestamp }
        TelemetryChannel.POSITIONS -> positionSnapshots.values.sortedBy { it.timestamp }
        TelemetryChannel.MARKET_DATA -> marketSnapshots.values.sortedBy { it.timestamp }
        TelemetryChannel.RISK_ALERTS -> riskAlertOrder.mapNotNull { riskAlertSnapshots[it] }
    }

    internal fun reset() {
        traderSnapshots.clear()
        positionSnapshots.clear()
        marketSnapshots.clear()
        riskAlertSnapshots.clear()
        riskAlertOrder.clear()
    }

    suspend fun publishTraderStatus(
        traderId: String,
        name: String,
        state: AITraderState,
        exchange: String,
        symbol: String,
        strategy: String,
        reason: String,
        metrics: AITraderMetrics? = null,
        timestamp: Instant = Instant.now()
    ) {
        val event = TraderStatusEvent(
            id = traderId,
            name = name,
            status = state,
            exchange = exchange,
            symbol = symbol,
            strategy = strategy,
            reason = reason,
            metrics = metrics,
            updatedAt = timestamp
        )
        traderSnapshots[traderId] = event
        emit(event)
    }

    suspend fun publishPositionSnapshot(
        position: ManagedPosition,
        status: PositionTelemetryStatus,
        reason: String? = null,
        realizedPnL: BigDecimal? = null,
        timestamp: Instant = Instant.now()
    ) {
        val event = PositionTelemetryEvent(
            id = position.positionId,
            traderId = position.traderId,
            symbol = position.position.symbol,
            action = position.position.action.name,
            quantity = position.position.quantity,
            entryPrice = position.position.entryPrice,
            currentPrice = position.position.currentPrice,
            unrealizedPnL = position.position.unrealizedPnL,
            realizedPnL = realizedPnL,
            status = status,
            reason = reason,
            trailingStopActivated = position.trailingStopActivated,
            stopLossPrice = position.stopLossPrice,
            takeProfitPrice = position.takeProfitPrice,
            isActive = status != PositionTelemetryStatus.CLOSED,
            updatedAt = timestamp
        )

        if (status == PositionTelemetryStatus.CLOSED) {
            positionSnapshots.remove(position.positionId)
        } else {
            positionSnapshots[position.positionId] = event
        }
        emit(event)
    }

    suspend fun publishRiskAlert(
        alertId: String,
        traderId: String?,
        severity: RiskAlertSeverity,
        message: String,
        violations: List<RiskViolation>,
        recommendation: RiskRecommendation? = null,
        status: String = "ACTIVE",
        timestamp: Instant = Instant.now()
    ) {
        val payloadViolations = violations.map { violation ->
            RiskViolationPayload(
                type = violation.type,
                message = violation.message,
                details = violation.details.mapValues { it.value?.toString() }
            )
        }

        val event = RiskAlertEvent(
            id = alertId,
            traderId = traderId,
            severity = severity,
            recommendation = recommendation,
            message = message,
            violations = payloadViolations,
            status = status,
            createdAt = timestamp
        )

        riskAlertSnapshots[alertId] = event
        riskAlertOrder.remove(alertId)
        riskAlertOrder.add(alertId)
        if (riskAlertOrder.size > MAX_RISK_ALERT_HISTORY) {
            val removedId = riskAlertOrder.removeAt(0)
            riskAlertSnapshots.remove(removedId)
        }
        emit(event)
    }

    suspend fun publishMarketData(
        symbol: String,
        price: BigDecimal,
        source: String,
        change24h: Double? = null,
        isSnapshot: Boolean = false,
        timestamp: Instant = Instant.now()
    ) {
        val event = MarketDataEvent(
            id = symbol.uppercase(),
            symbol = symbol,
            price = price,
            source = source,
            change24h = change24h,
            isSnapshot = isSnapshot,
            updatedAt = timestamp
        )
        marketSnapshots[event.id] = event
        emit(event)
    }

    private suspend fun emit(event: TelemetryEvent) {
        val emitted = eventsFlow.tryEmit(event)
        if (!emitted) {
            logger.warn { "Telemetry buffer full. Dropping event for channel=${event.channel.wireName}." }
        }
    }
}
