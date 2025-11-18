package com.fmps.autotrader.desktop.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import mu.KotlinLogging
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

/**
 * Real MarketDataService implementation using WebSocket telemetry with REST fallback.
 * 
 * Connects to telemetry WebSocket for real-time updates and falls back to REST polling
 * when WebSocket is disconnected.
 */
class RealMarketDataService(
    private val httpClient: HttpClient,
    private val telemetryClient: TelemetryClient,
    private val baseUrl: String = "http://localhost:8080",
    private val apiKey: String? = null
) : MarketDataService {

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // State flows for each data type
    private val candlesFlow = MutableStateFlow<List<Candlestick>>(emptyList())
    private val positionsFlow = MutableStateFlow<List<OpenPosition>>(emptyList())
    private val tradesFlow = MutableStateFlow<List<TradeRecord>>(emptyList())
    private val connectionStatusFlow = MutableStateFlow<ConnectionStatus>(ConnectionStatus.DISCONNECTED)
    
    // Current timeframe for candlesticks
    private var currentTimeframe: Timeframe = Timeframe.FIVE_MIN
    
    // REST polling job (active when WebSocket disconnected)
    private var restPollingJob: Job? = null
    private val isPolling = AtomicBoolean(false)
    private val restPollIntervalMs = 5000L // Poll every 5 seconds when using REST fallback
    
    init {
        // Start telemetry client
        telemetryClient.start()
        
        // Observe telemetry samples
        scope.launch {
            telemetryClient.samples().collect { sample ->
                handleTelemetrySample(sample)
            }
        }
        
        // Monitor connection status
        scope.launch {
            monitorConnectionStatus()
        }
    }

    override fun candlesticks(timeframe: Timeframe): Flow<List<Candlestick>> {
        currentTimeframe = timeframe
        // Trigger immediate fetch if using REST fallback
        if (isPolling.get()) {
            scope.launch {
                fetchCandlesticksFromRest(timeframe)
            }
        }
        return candlesFlow.asStateFlow()
    }

    override fun positions(): Flow<List<OpenPosition>> {
        // Trigger immediate fetch if using REST fallback
        if (isPolling.get()) {
            scope.launch {
                fetchPositionsFromRest()
            }
        }
        return positionsFlow.asStateFlow()
    }

    override fun tradeHistory(): Flow<List<TradeRecord>> {
        // Trigger immediate fetch if using REST fallback
        if (isPolling.get()) {
            scope.launch {
                fetchTradeHistoryFromRest()
            }
        }
        return tradesFlow.asStateFlow()
    }

    override fun connectionStatus(): Flow<ConnectionStatus> = connectionStatusFlow.asStateFlow()

    /**
     * Handles incoming telemetry samples and updates appropriate flows.
     * 
     * Telemetry channels:
     * - "positions" -> PositionTelemetryEvent (open/updated/closed positions)
     * - "market-data" -> MarketDataEvent (price updates, not candlesticks)
     * - "trader-status" -> TraderStatusEvent (trader state changes)
     * - "risk-alerts" -> RiskAlertEvent (risk violations)
     */
    private suspend fun handleTelemetrySample(sample: TelemetrySample) {
        try {
            when (sample.channel) {
                "positions" -> {
                    val position = parsePosition(sample.payload)
                    if (position != null) {
                        positionsFlow.update { positions ->
                            val updated = positions.toMutableList()
                            val index = updated.indexOfFirst { it.id == position.id }
                            if (index >= 0) {
                                updated[index] = position
                            } else {
                                updated.add(position)
                            }
                            updated
                        }
                        
                        // Track closed positions for trade history
                        // When a position is closed (status == "CLOSED"), we could add it to trade history
                        // For now, trade history is fetched from REST API (/api/v1/trades)
                    }
                }
                "market-data" -> {
                    // MarketDataEvent contains price updates, not candlestick data
                    // For now, we don't parse candlesticks from market-data events
                    // Candlesticks should come from REST API or a dedicated candlestick channel
                    // This is a known limitation - see Issue_22_REVIEW.md
                    logger.debug { "Received market-data event (price update), not processing as candlestick" }
                }
                "trader-status" -> {
                    // Trader status updates don't directly map to market data
                    // But we could use them to update position trader names if needed
                    logger.debug { "Received trader-status event" }
                }
                "system.error" -> {
                    // Connection error - switch to REST fallback
                    if (sample.payload.contains("CONNECTION")) {
                        startRestPolling()
                    }
                }
                else -> {
                    logger.debug { "Unhandled telemetry channel: ${sample.channel}" }
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to handle telemetry sample: ${sample.channel}" }
        }
    }

    /**
     * Monitors connection status and switches between WebSocket and REST fallback.
     */
    private suspend fun monitorConnectionStatus() {
        while (scope.isActive) {
            val isConnected = (telemetryClient as? RealTelemetryClient)?.isConnected() ?: false
            
            if (isConnected) {
                connectionStatusFlow.value = ConnectionStatus.CONNECTED
                stopRestPolling()
            } else {
                connectionStatusFlow.value = ConnectionStatus.DISCONNECTED
                startRestPolling()
            }
            
            delay(2000) // Check every 2 seconds
        }
    }

    /**
     * Starts REST polling when WebSocket is disconnected.
     */
    private fun startRestPolling() {
        if (isPolling.getAndSet(true)) {
            return // Already polling
        }
        
        logger.info { "Starting REST polling fallback for market data" }
        connectionStatusFlow.value = ConnectionStatus.RECONNECTING
        
        restPollingJob = scope.launch {
            while (isPolling.get() && scope.isActive) {
                try {
                    fetchCandlesticksFromRest(currentTimeframe)
                    fetchPositionsFromRest()
                    fetchTradeHistoryFromRest()
                } catch (e: Exception) {
                    logger.warn(e) { "REST polling failed, will retry" }
                }
                delay(restPollIntervalMs)
            }
        }
    }

    /**
     * Stops REST polling when WebSocket reconnects.
     */
    private fun stopRestPolling() {
        if (!isPolling.getAndSet(false)) {
            return // Not polling
        }
        
        logger.info { "Stopping REST polling fallback, WebSocket connected" }
        restPollingJob?.cancel()
        restPollingJob = null
    }

    /**
     * Fetches candlesticks from REST API.
     * 
     * Note: This endpoint may not exist yet in core-service.
     * Candlesticks should ideally come from exchange connectors or a dedicated market data service.
     * For now, this is a placeholder that gracefully handles the missing endpoint.
     * 
     * Future implementation options:
     * 1. Add /api/v1/market-data/candlesticks endpoint to core-service
     * 2. Use exchange connector WebSocket streams directly
     * 3. Aggregate from MarketDataEvent telemetry (requires OHLCV data, not just price)
     */
    private suspend fun fetchCandlesticksFromRest(timeframe: Timeframe) {
        try {
            // Attempt to fetch from potential endpoint
            // GET /api/v1/market-data/candlesticks?timeframe=5m&limit=100
            val response = httpClient.get("$baseUrl/api/v1/market-data/candlesticks") {
                parameter("timeframe", timeframe.label)
                parameter("limit", 100)
                apiKey?.let { header("X-API-Key", it) }
            }
            
            if (response.status.isSuccess()) {
                // Parse response when endpoint is implemented
                // For now, endpoint doesn't exist, so we keep existing candles
                logger.debug { "Candlestick REST endpoint responded but parsing not yet implemented" }
            }
        } catch (e: Exception) {
            // API endpoint doesn't exist yet - this is expected and documented in Issue_22_REVIEW.md
            // Gracefully handle missing endpoint without logging errors
            logger.trace(e) { "Candlestick REST endpoint not available (documented limitation)" }
        }
    }

    /**
     * Fetches open positions from REST API.
     */
    private suspend fun fetchPositionsFromRest() {
        try {
            val response = httpClient.get("$baseUrl/api/v1/trades/open") {
                apiKey?.let { header("X-API-Key", it) }
            }
            
            if (response.status.isSuccess()) {
                val apiResponse = response.body<ApiResponse<List<TradeDTO>>>()
                if (apiResponse.success) {
                    positionsFlow.value = apiResponse.data.mapNotNull { it.toOpenPosition() }
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to fetch positions from REST" }
        }
    }

    /**
     * Fetches trade history from REST API.
     */
    private suspend fun fetchTradeHistoryFromRest() {
        try {
            val response = httpClient.get("$baseUrl/api/v1/trades") {
                parameter("limit", 100)
                apiKey?.let { header("X-API-Key", it) }
            }
            
            if (response.status.isSuccess()) {
                val apiResponse = response.body<ApiResponse<List<TradeDTO>>>()
                if (apiResponse.success) {
                    tradesFlow.value = apiResponse.data.mapNotNull { it.toTradeRecord() }
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to fetch trade history from REST" }
        }
    }

    /**
     * Parses a PositionTelemetryEvent from telemetry payload.
     * 
     * Expected format: TelemetryServerMessage with:
     * - type: "event"
     * - channel: "positions"
     * - data: PositionTelemetryEvent (serialized)
     */
    private fun parsePosition(payload: String): OpenPosition? {
        return try {
            // Parse TelemetryServerMessage
            val serverMessage = Json.decodeFromString<TelemetryServerMessage>(payload)
            
            // Extract TelemetryEvent from data field
            if (serverMessage.type != "event" || serverMessage.data == null) {
                return null
            }
            
            // Parse PositionTelemetryEvent from data
            val positionEvent = Json.decodeFromJsonElement<PositionTelemetryEventDTO>(
                serverMessage.data
            )
            
            // Convert to OpenPosition (only if position is active/open)
            // Closed positions trigger trade history refresh
            if (!positionEvent.isActive || positionEvent.status == "CLOSED") {
                // When position closes, trigger trade history refresh to include the closed trade
                scope.launch {
                    fetchTradeHistoryFromRest()
                }
                return null
            }
            
            OpenPosition(
                id = positionEvent.id,
                traderName = "Trader ${positionEvent.traderId}", // Would need trader name lookup
                symbol = positionEvent.symbol,
                size = positionEvent.quantity.toDouble(),
                entryPrice = positionEvent.entryPrice.toDouble(),
                markPrice = positionEvent.currentPrice.toDouble(),
                pnl = positionEvent.unrealizedPnL.toDouble(),
                status = TraderStatus.RUNNING // Would need actual trader status
            )
        } catch (e: Exception) {
            logger.debug(e) { "Failed to parse position from telemetry: ${e.message}" }
            null
        }
    }

    /**
     * Parses a trade from position update when position is closed.
     * Note: Trade execution events don't exist in telemetry yet.
     * For now, trades are fetched from REST API.
     * 
     * @param payload Telemetry message payload (unused for now)
     * @return null - trades are fetched from REST API
     */
    @Suppress("UNUSED_PARAMETER")
    private fun parseTrade(payload: String): TradeRecord? {
        // Trades are not directly available in telemetry
        // They should be fetched from REST API (/api/v1/trades)
        // This function is kept for future use if trade.executed channel is added
        return null
    }

    /**
     * Parses candlestick from telemetry.
     * Note: MarketDataEvent only contains price updates, not OHLCV candlestick data.
     * Candlesticks should be fetched from REST API or exchange connector.
     * This function is kept for future use if market.candlestick channel is added.
     * 
     * @param payload Telemetry message payload (unused for now)
     * @return null - candlesticks are fetched from REST API or exchange connector
     */
    @Suppress("UNUSED_PARAMETER")
    private fun parseCandlestick(payload: String): Candlestick? {
        // Candlesticks are not available in telemetry yet
        // They should be fetched from REST API or exchange connector
        // This function is kept for future use if market.candlestick channel is added
        return null
    }
    
    /**
     * DTOs for parsing telemetry messages.
     * These match the TelemetryEvent structure from core-service.
     */
    @Serializable
    private data class TelemetryServerMessage(
        val type: String,
        val channel: String? = null,
        val data: JsonElement? = null,
        val replay: Boolean? = null,
        val meta: Map<String, String>? = null,
        val timestamp: Long = 0
    )
    
    @Serializable
    private data class PositionTelemetryEventDTO(
        val id: String,
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
        val status: String, // "OPEN", "UPDATED", "CLOSED"
        val reason: String? = null,
        val trailingStopActivated: Boolean = false,
        @Serializable(with = BigDecimalSerializer::class)
        val stopLossPrice: BigDecimal? = null,
        @Serializable(with = BigDecimalSerializer::class)
        val takeProfitPrice: BigDecimal? = null,
        val isActive: Boolean,
        val timestamp: Long = 0
    )
    
    @Serializable
    private class BigDecimalSerializer : kotlinx.serialization.KSerializer<BigDecimal> {
        override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("BigDecimal", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
        override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: BigDecimal) = encoder.encodeString(value.toString())
        override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): BigDecimal = BigDecimal(decoder.decodeString())
    }

    @Serializable
    private data class ApiResponse<T>(
        val success: Boolean,
        val data: T,
        val timestamp: String? = null
    )

    @Serializable
    private data class TradeDTO(
        val id: Int,
        val aiTraderId: Int,
        val exchange: String,
        val tradingPair: String,
        val side: String,
        val entryPrice: String,
        val exitPrice: String?,
        val quantity: String,
        val status: String,
        val profitLoss: String,
        val entryTime: String,
        val exitTime: String?
    ) {
        fun toOpenPosition(): OpenPosition? {
            if (status != "OPEN") return null
            val symbol = tradingPair.replace("/", "")
            return OpenPosition(
                id = id.toString(),
                traderName = "Trader $aiTraderId", // Would need trader name lookup
                symbol = symbol,
                size = quantity.toDoubleOrNull() ?: 0.0,
                entryPrice = entryPrice.toDoubleOrNull() ?: 0.0,
                markPrice = entryPrice.toDoubleOrNull() ?: 0.0, // Would need current price
                pnl = profitLoss.toDoubleOrNull() ?: 0.0,
                status = TraderStatus.RUNNING // Would need actual status
            )
        }

        fun toTradeRecord(): TradeRecord? {
            val symbol = tradingPair.replace("/", "")
            return TradeRecord(
                id = id.toString(),
                traderName = "Trader $aiTraderId",
                symbol = symbol,
                side = if (side == "LONG") TradeSide.BUY else TradeSide.SELL,
                qty = quantity.toDoubleOrNull() ?: 0.0,
                price = entryPrice.toDoubleOrNull() ?: 0.0,
                pnl = profitLoss.toDoubleOrNull() ?: 0.0,
                timestamp = Instant.parse(entryTime)
            )
        }
    }
}


