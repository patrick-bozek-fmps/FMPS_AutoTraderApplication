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
import mu.KotlinLogging
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
     */
    private suspend fun handleTelemetrySample(sample: TelemetrySample) {
        try {
            when (sample.channel) {
                "market.candlestick" -> {
                    val candle = parseCandlestick(sample.payload)
                    if (candle != null) {
                        candlesFlow.update { candles ->
                            val updated = candles.toMutableList()
                            // Replace or add candlestick
                            val index = updated.indexOfFirst { it.timestamp == candle.timestamp }
                            if (index >= 0) {
                                updated[index] = candle
                            } else {
                                updated.add(candle)
                                updated.sortBy { it.timestamp }
                            }
                            // Keep only recent candles (last 100)
                            updated.takeLast(100)
                        }
                    }
                }
                "position.update" -> {
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
                    }
                }
                "trade.executed" -> {
                    val trade = parseTrade(sample.payload)
                    if (trade != null) {
                        tradesFlow.update { trades ->
                            val updated = trades.toMutableList()
                            // Add new trade at the beginning
                            updated.add(0, trade)
                            // Keep only recent trades (last 100)
                            updated.take(100)
                        }
                    }
                }
                "system.error" -> {
                    // Connection error - switch to REST fallback
                    if (sample.payload.contains("CONNECTION")) {
                        startRestPolling()
                    }
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
     */
    private suspend fun fetchCandlesticksFromRest(timeframe: Timeframe) {
        try {
            // Note: This endpoint may not exist yet in core-service
            // For now, we'll use a placeholder that could be implemented
            // GET /api/v1/market-data/candlesticks?timeframe=5m&limit=100
            val response = httpClient.get("$baseUrl/api/v1/market-data/candlesticks") {
                parameter("timeframe", timeframe.label)
                parameter("limit", 100)
                apiKey?.let { header("X-API-Key", it) }
            }
            
            if (response.status.isSuccess()) {
                // Parse response (structure depends on actual API)
                // For now, keep existing candles if API not available
            }
        } catch (e: Exception) {
            // API endpoint may not exist yet - this is expected
            logger.debug(e) { "Market data REST endpoint not available (expected if not implemented)" }
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

    // Parsing functions for telemetry messages
    private fun parseCandlestick(payload: String): Candlestick? {
        return try {
            // Parse candlestick from JSON (structure depends on actual telemetry format)
            // Placeholder implementation - will be enhanced when telemetry format is finalized
            Json.parseToJsonElement(payload)
            null
        } catch (e: Exception) {
            logger.debug(e) { "Failed to parse candlestick from telemetry" }
            null
        }
    }

    private fun parsePosition(payload: String): OpenPosition? {
        return try {
            // Parse position from JSON - will be enhanced when telemetry format is finalized
            Json.parseToJsonElement(payload)
            null
        } catch (e: Exception) {
            logger.debug(e) { "Failed to parse position from telemetry" }
            null
        }
    }

    private fun parseTrade(payload: String): TradeRecord? {
        return try {
            // Parse trade from JSON - will be enhanced when telemetry format is finalized
            Json.parseToJsonElement(payload)
            null
        } catch (e: Exception) {
            logger.debug(e) { "Failed to parse trade from telemetry" }
            null
        }
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

