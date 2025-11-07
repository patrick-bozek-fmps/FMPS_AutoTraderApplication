package com.fmps.autotrader.core.traders

import com.fmps.autotrader.core.connectors.IExchangeConnector
import com.fmps.autotrader.core.traders.strategies.ITradingStrategy
import com.fmps.autotrader.core.traders.strategies.StrategyFactory
import com.fmps.autotrader.shared.enums.TimeFrame
import com.fmps.autotrader.shared.model.Position
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

/**
 * AI Trader Core Class
 *
 * This is the main class that orchestrates automated trading.
 * It manages the trading lifecycle, processes market data, generates signals,
 * and executes trades based on the configured strategy.
 *
 * Key Features:
 * - Lifecycle management (start, stop, pause, resume)
 * - Market data processing
 * - Signal generation
 * - State management (thread-safe)
 * - Error handling and recovery
 * - Performance metrics tracking
 *
 * @property config Trader configuration
 * @property exchangeConnector Exchange connector for market data and order execution
 * @property positionManager Position manager (optional, will be added in Issue #13)
 * @property riskManager Risk manager (optional, will be added in Issue #14)
 *
 * @since 1.0.0
 */
class AITrader(
    initialConfig: AITraderConfig,
    private val exchangeConnector: IExchangeConnector,
    private val positionManager: Any? = null, // PositionManager - will be added in Issue #13
    private val riskManager: Any? = null, // RiskManager - will be added in Issue #14
    private val patternService: com.fmps.autotrader.core.patterns.PatternService? = null // PatternService - Issue #15
) {
    private var currentConfig: AITraderConfig = initialConfig
    val config: AITraderConfig
        get() = currentConfig

    // State management
    private val state = AtomicReference<AITraderState>(AITraderState.IDLE)
    private val stateMutex = Mutex()

    // Strategy and processors
    private var strategy: ITradingStrategy = StrategyFactory.createStrategy(currentConfig)
    private var marketDataProcessor: MarketDataProcessor = MarketDataProcessor(strategy)
    private var signalGenerator: SignalGenerator = createSignalGenerator(strategy, currentConfig)
    
    // Pattern integration helper (Issue #15) - will be set via setter if dependencies are available
    private var patternIntegrationHelper: com.fmps.autotrader.core.patterns.PatternIntegrationHelper? = null
    
    /**
     * Set pattern integration helper (for Issue #15).
     * 
     * This allows AITrader to update pattern performance and extract patterns from trades.
     * Should be called after AITrader is created if PatternService and TradeRepository are available.
     */
    fun setPatternIntegrationHelper(helper: com.fmps.autotrader.core.patterns.PatternIntegrationHelper) {
        this.patternIntegrationHelper = helper
        logger.info { "Pattern integration helper set for AI Trader ${config.id}" }
    }

    // Trading loop
    private var tradingJob: Job? = null
    private val traderScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Metrics
    private var metrics: AITraderMetrics = AITraderMetrics.withStartTime()
    private val metricsMutex = Mutex()

    // Current position (will be managed by PositionManager in Issue #13)
    private var currentPosition: Position? = null

    /**
     * Get current state (thread-safe).
     */
    fun getState(): AITraderState = state.get()

    /**
     * Get current metrics (thread-safe).
     */
    suspend fun getMetrics(): AITraderMetrics = metricsMutex.withLock {
        metrics.copy(
            uptime = if (metrics.startTime != null) {
                Duration.between(metrics.startTime, Instant.now())
            } else {
                Duration.ZERO
            }
        )
    }

    /**
     * Start the trader.
     *
     * This method:
     * 1. Transitions state to STARTING
     * 2. Connects to exchange
     * 3. Starts the trading loop
     * 4. Transitions to RUNNING
     *
     * @return Result indicating success or failure
     */
    suspend fun start(): Result<Unit> {
        return stateMutex.withLock {
            if (state.get() != AITraderState.IDLE && state.get() != AITraderState.STOPPED) {
                return Result.failure(
                    IllegalStateException("Cannot start trader in state: ${state.get()}")
                )
            }

            try {
                state.set(AITraderState.STARTING)
                logger.info { "Starting AI Trader ${config.id} (${config.name})" }

                // Connect to exchange
                if (!exchangeConnector.isConnected()) {
                    exchangeConnector.connect()
                }

                // Reset metrics
                metrics = AITraderMetrics.withStartTime()

                // Start trading loop
                tradingJob = traderScope.launch {
                    tradingLoop()
                }

                state.set(AITraderState.RUNNING)
                logger.info { "AI Trader ${config.id} started successfully" }

                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Error starting AI Trader ${config.id}" }
                state.set(AITraderState.ERROR)
                Result.failure(e)
            }
        }
    }

    /**
     * Stop the trader.
     *
     * This method:
     * 1. Transitions state to STOPPING
     * 2. Cancels trading loop
     * 3. Closes positions (if any)
     * 4. Transitions to STOPPED
     *
     * @return Result indicating success or failure
     */
    suspend fun stop(): Result<Unit> {
        return stateMutex.withLock {
            if (state.get() == AITraderState.STOPPED || state.get() == AITraderState.IDLE) {
                return Result.success(Unit) // Already stopped
            }

            try {
                state.set(AITraderState.STOPPING)
                logger.info { "Stopping AI Trader ${config.id}" }

                // Cancel trading loop
                tradingJob?.cancel()
                tradingJob?.join()

                // Close positions (will be handled by PositionManager in Issue #13)
                // For now, just log
                currentPosition?.let {
                    logger.info { "Closing position for trader ${config.id}" }
                    // TODO: Close position via PositionManager
                }

                state.set(AITraderState.STOPPED)
                logger.info { "AI Trader ${config.id} stopped successfully" }

                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Error stopping AI Trader ${config.id}" }
                state.set(AITraderState.ERROR)
                Result.failure(e)
            }
        }
    }

    /**
     * Pause the trader.
     *
     * Temporarily stops trading but maintains state.
     *
     * @return Result indicating success or failure
     */
    suspend fun pause(): Result<Unit> {
        return stateMutex.withLock {
            if (state.get() != AITraderState.RUNNING) {
                return Result.failure(
                    IllegalStateException("Cannot pause trader in state: ${state.get()}")
                )
            }

            state.set(AITraderState.PAUSED)
            logger.info { "AI Trader ${config.id} paused" }
            Result.success(Unit)
        }
    }

    /**
     * Resume the trader.
     *
     * Resumes trading from paused state.
     *
     * @return Result indicating success or failure
     */
    suspend fun resume(): Result<Unit> {
        return stateMutex.withLock {
            if (state.get() != AITraderState.PAUSED) {
                return Result.failure(
                    IllegalStateException("Cannot resume trader in state: ${state.get()}")
                )
            }

            state.set(AITraderState.RUNNING)
            logger.info { "AI Trader ${config.id} resumed" }
            Result.success(Unit)
        }
    }

    /**
     * Update configuration.
     *
     * @param newConfig New configuration
     * @return Result indicating success or failure
     */
    suspend fun updateConfig(newConfig: AITraderConfig): Result<Unit> {
        return stateMutex.withLock {
            if (state.get() == AITraderState.RUNNING) {
                return@withLock Result.failure(
                    IllegalStateException("Cannot update config while trader is running")
                )
            }

            if (newConfig.id != currentConfig.id) {
                return@withLock Result.failure(
                    IllegalArgumentException(
                        "Cannot change trader ID during config update (current=${currentConfig.id}, new=${newConfig.id})"
                    )
                )
            }

            try {
                rebuildForNewConfig(newConfig)
                logger.info { "Configuration updated for trader ${newConfig.id}" }
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Failed to update configuration for trader ${currentConfig.id}" }
                Result.failure(e)
            }
        }
    }

    /**
     * Main trading loop.
     *
     * This coroutine:
     * 1. Fetches market data
     * 2. Processes market data
     * 3. Generates signals
     * 4. Executes signals (if valid)
     * 5. Updates metrics
     * 6. Sleeps until next interval
     */
    private suspend fun tradingLoop() {
        logger.info { "Trading loop started for trader ${config.id}" }

        while (currentCoroutineContext().isActive && state.get() == AITraderState.RUNNING) {
            try {
                // Fetch market data
                val candles = fetchMarketData()
                if (candles == null || candles.isEmpty()) {
                    logger.warn { "No market data available, retrying..." }
                    delay(5000) // Wait 5 seconds before retry
                    continue
                }

                // Process market data
                val processedData = marketDataProcessor.processCandlesticks(candles)
                if (processedData == null) {
                    logger.warn { "Failed to process market data, skipping iteration" }
                    delay(getIntervalDuration())
                    continue
                }

                // Generate signal
                val signal = signalGenerator.generateSignal(processedData, currentPosition)

                // Execute signal (if valid and actionable)
                if (signal.isActionable() && signal.meetsConfidenceThreshold()) {
                    executeSignal(signal)
                } else if (signal.isClose()) {
                    // Close position if CLOSE signal
                    executeCloseSignal()
                }

                // Update metrics (will be enhanced in Issue #13 with actual trade data)

                // Sleep until next interval
                delay(getIntervalDuration())

            } catch (e: CancellationException) {
                logger.info { "Trading loop cancelled for trader ${config.id}" }
                break
            } catch (e: Exception) {
                logger.error(e) { "Error in trading loop for trader ${config.id}" }
                state.set(AITraderState.ERROR)
                delay(10000) // Wait 10 seconds before retrying
            }
        }

        logger.info { "Trading loop ended for trader ${config.id}" }
    }

    /**
     * Fetch market data from exchange.
     */
    private suspend fun fetchMarketData(): List<com.fmps.autotrader.shared.model.Candlestick>? {
        return try {
            exchangeConnector.getCandles(
                symbol = config.symbol,
                interval = config.candlestickInterval,
                limit = 100
            )
        } catch (e: Exception) {
            logger.error(e) { "Error fetching market data" }
            null
        }
    }

    /**
     * Execute a trading signal.
     *
     * This will be enhanced in Issue #13 with PositionManager integration.
     */
    private suspend fun executeSignal(signal: TradingSignal) {
        logger.info {
            "Executing signal: ${signal.action} " +
                    "with confidence ${signal.confidence} " +
                    "for trader ${config.id}"
        }

        // TODO: Integrate with PositionManager (Issue #13)
        // TODO: Integrate with RiskManager (Issue #14)
        // For now, just log
        updateMetricsForSignal(signal)
        
        // Track pattern usage if pattern was matched (Issue #15)
        signal.matchedPatternId?.let { patternId ->
            logger.debug { "Pattern $patternId was used in signal generation" }
            // Pattern usage tracking will be done when trade is actually created
            // This happens in PositionManager when trade is opened (Issue #13)
        }
        
        logger.debug { "Signal execution: ${signal.reason}" }
    }

    /**
     * Execute close signal.
     */
    private suspend fun executeCloseSignal() {
        logger.info { "Executing close signal for trader ${config.id}" }

        // TODO: Integrate with PositionManager (Issue #13)
        // When position is closed via PositionManager:
        // 1. patternIntegrationHelper?.updatePatternPerformance(tradeId, patternId)
        // 2. patternIntegrationHelper?.learnFromTrade(tradeId) if profitable
        
        currentPosition = null
        metricsMutex.withLock {
            metrics = metrics.recordCloseExecution()
        }
    }
    
    /**
     * Track pattern usage when a trade is opened.
     * 
     * This should be called by PositionManager (Issue #13) when a trade is created.
     * 
     * @param tradeId Database trade ID
     * @param patternId Pattern ID that was used (from TradingSignal.matchedPatternId)
     */
    suspend fun trackPatternUsage(tradeId: Int, patternId: String?) {
        patternIntegrationHelper?.trackPatternUsage(patternId, tradeId)
    }
    
    /**
     * Update pattern performance when a trade closes.
     * 
     * This should be called by PositionManager (Issue #13) when a trade is closed.
     * 
     * @param tradeId Database trade ID
     * @param patternId Pattern ID that was used (optional, can be retrieved from trade)
     */
    suspend fun updatePatternPerformance(tradeId: Int, patternId: String? = null) {
        patternIntegrationHelper?.updatePatternPerformance(tradeId, patternId)
    }
    
    /**
     * Learn pattern from a successful trade.
     * 
     * This should be called by PositionManager (Issue #13) when a profitable trade closes.
     * 
     * @param tradeId Database trade ID
     */
    suspend fun learnPatternFromTrade(tradeId: Int) {
        patternIntegrationHelper?.learnFromTrade(tradeId)
    }

    /**
     * Get interval duration based on candlestick interval.
     */
    private fun getIntervalDuration(): Long {
        return when (config.candlestickInterval) {
            TimeFrame.ONE_MINUTE -> 60_000L
            TimeFrame.FIVE_MINUTES -> 5 * 60_000L
            TimeFrame.FIFTEEN_MINUTES -> 15 * 60_000L
            TimeFrame.ONE_HOUR -> 60 * 60_000L
            TimeFrame.ONE_DAY -> 24 * 60 * 60_000L
            else -> 60_000L // Default to 1 minute
        }
    }

    /**
     * Cleanup resources.
     */
    suspend fun cleanup() {
        stop()
        traderScope.cancel()
        marketDataProcessor.clearCache()
        strategy.reset()
        logger.info { "Cleanup completed for trader ${config.id}" }
    }

    private fun createSignalGenerator(strategy: ITradingStrategy, config: AITraderConfig): SignalGenerator {
        return SignalGenerator(
            strategy = strategy,
            minConfidenceThreshold = 0.5,
            patternService = patternService,
            config = config,
            patternWeight = 0.3
        )
    }

    private fun rebuildForNewConfig(newConfig: AITraderConfig) {
        strategy.reset()
        currentConfig = newConfig
        strategy = StrategyFactory.createStrategy(currentConfig)
        marketDataProcessor = MarketDataProcessor(strategy)
        signalGenerator = createSignalGenerator(strategy, currentConfig)
    }

    private suspend fun updateMetricsForSignal(signal: TradingSignal) {
        metricsMutex.withLock {
            metrics = metrics.recordSignalExecution(signal.action, signal.confidence)
        }
    }

    suspend fun recordTradeResult(profit: BigDecimal) {
        metricsMutex.withLock {
            metrics = metrics.recordTradeResult(profit)
        }
    }
}

