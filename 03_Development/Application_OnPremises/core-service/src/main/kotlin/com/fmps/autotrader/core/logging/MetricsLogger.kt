package com.fmps.autotrader.core.logging

import mu.KotlinLogging
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger("com.fmps.autotrader.metrics")

/**
 * Utility object for logging performance metrics and business events.
 * Uses markers to separate metrics from regular application logs.
 */
object MetricsLogger {
    
    // Markers for different metric types
    val METRICS_MARKER: Marker = MarkerFactory.getMarker("METRICS")
    val PERFORMANCE_MARKER: Marker = MarkerFactory.getMarker("PERFORMANCE").apply {
        add(METRICS_MARKER)
    }
    val BUSINESS_MARKER: Marker = MarkerFactory.getMarker("BUSINESS").apply {
        add(METRICS_MARKER)
    }
    val SYSTEM_MARKER: Marker = MarkerFactory.getMarker("SYSTEM").apply {
        add(METRICS_MARKER)
    }
    
    /**
     * Logs a performance metric with duration in milliseconds.
     *
     * @param operation Name of the operation being measured
     * @param durationMs Duration in milliseconds
     * @param additionalInfo Optional additional context
     */
    fun logPerformance(
        operation: String,
        durationMs: Long,
        additionalInfo: Map<String, Any> = emptyMap()
    ) {
        val info = buildString {
            append("operation=$operation")
            append(", duration_ms=$durationMs")
            additionalInfo.forEach { (key, value) ->
                append(", $key=$value")
            }
        }
        logger.info(PERFORMANCE_MARKER, info)
    }
    
    /**
     * Logs a business metric (e.g., trades executed, positions opened).
     *
     * @param metric Name of the metric
     * @param value Value of the metric
     * @param unit Unit of measurement
     * @param additionalInfo Optional additional context
     */
    fun logBusinessMetric(
        metric: String,
        value: Any,
        unit: String = "count",
        additionalInfo: Map<String, Any> = emptyMap()
    ) {
        val info = buildString {
            append("metric=$metric")
            append(", value=$value")
            append(", unit=$unit")
            additionalInfo.forEach { (key, value) ->
                append(", $key=$value")
            }
        }
        logger.info(BUSINESS_MARKER, info)
    }
    
    /**
     * Logs a system metric (e.g., memory usage, thread count).
     *
     * @param metric Name of the system metric
     * @param value Value of the metric
     * @param unit Unit of measurement
     */
    fun logSystemMetric(
        metric: String,
        value: Any,
        unit: String = ""
    ) {
        val info = "metric=$metric, value=$value" + if (unit.isNotEmpty()) ", unit=$unit" else ""
        logger.info(SYSTEM_MARKER, info)
    }
    
    /**
     * Logs database query performance.
     *
     * @param query Query identifier or description
     * @param durationMs Duration in milliseconds
     * @param rowCount Number of rows affected/returned
     */
    fun logDatabaseQuery(
        query: String,
        durationMs: Long,
        rowCount: Int = 0
    ) {
        logPerformance(
            operation = "database_query",
            durationMs = durationMs,
            additionalInfo = mapOf(
                "query" to query,
                "row_count" to rowCount
            )
        )
    }
    
    /**
     * Logs API request performance.
     *
     * @param endpoint API endpoint path
     * @param method HTTP method
     * @param statusCode HTTP status code
     * @param durationMs Duration in milliseconds
     */
    fun logApiRequest(
        endpoint: String,
        method: String,
        statusCode: Int,
        durationMs: Long
    ) {
        logPerformance(
            operation = "api_request",
            durationMs = durationMs,
            additionalInfo = mapOf(
                "endpoint" to endpoint,
                "method" to method,
                "status_code" to statusCode
            )
        )
    }
    
    /**
     * Logs exchange API call performance.
     *
     * @param exchange Exchange name (e.g., "Binance", "Bitget")
     * @param operation Operation type (e.g., "place_order", "get_balance")
     * @param durationMs Duration in milliseconds
     * @param success Whether the call was successful
     */
    fun logExchangeApiCall(
        exchange: String,
        operation: String,
        durationMs: Long,
        success: Boolean
    ) {
        logPerformance(
            operation = "exchange_api_call",
            durationMs = durationMs,
            additionalInfo = mapOf(
                "exchange" to exchange,
                "operation" to operation,
                "success" to success
            )
        )
    }
    
    /**
     * Logs trade execution metrics.
     *
     * @param traderId AI Trader ID
     * @param action Trade action (BUY/SELL)
     * @param symbol Trading pair symbol
     * @param amount Trade amount
     * @param price Execution price
     */
    fun logTradeExecution(
        traderId: String,
        action: String,
        symbol: String,
        amount: Double,
        price: Double
    ) {
        logBusinessMetric(
            metric = "trade_executed",
            value = 1,
            unit = "count",
            additionalInfo = mapOf(
                "trader_id" to traderId,
                "action" to action,
                "symbol" to symbol,
                "amount" to amount,
                "price" to price
            )
        )
    }
    
    /**
     * Logs AI trader status change.
     *
     * @param traderId AI Trader ID
     * @param previousStatus Previous status
     * @param newStatus New status
     */
    fun logTraderStatusChange(
        traderId: String,
        previousStatus: String,
        newStatus: String
    ) {
        logBusinessMetric(
            metric = "trader_status_change",
            value = newStatus,
            unit = "status",
            additionalInfo = mapOf(
                "trader_id" to traderId,
                "previous_status" to previousStatus
            )
        )
    }
    
    /**
     * Logs position P&L update.
     *
     * @param traderId AI Trader ID
     * @param positionId Position ID
     * @param pnl Profit/Loss amount
     * @param pnlPercent Profit/Loss percentage
     */
    fun logPositionPnL(
        traderId: String,
        positionId: String,
        pnl: Double,
        pnlPercent: Double
    ) {
        logBusinessMetric(
            metric = "position_pnl",
            value = pnl,
            unit = "currency",
            additionalInfo = mapOf(
                "trader_id" to traderId,
                "position_id" to positionId,
                "pnl_percent" to pnlPercent
            )
        )
    }
}

/**
 * Extension function to measure and log the execution time of a block.
 *
 * Example:
 * ```
 * val result = measureAndLog("process_data") {
 *     // Your code here
 *     processData()
 * }
 * // Automatically logs: "operation=process_data, duration_ms=123"
 * ```
 */
inline fun <T> measureAndLog(
    operation: String,
    additionalInfo: Map<String, Any> = emptyMap(),
    block: () -> T
): T {
    var result: T
    val duration = measureTimeMillis {
        result = block()
    }
    MetricsLogger.logPerformance(operation, duration, additionalInfo)
    return result
}

/**
 * Extension function to measure database operation time.
 *
 * Example:
 * ```
 * val users = measureDatabaseQuery("find_all_users") {
 *     userRepository.findAll()
 * }
 * ```
 */
inline fun <T> measureDatabaseQuery(
    queryDescription: String,
    block: () -> T
): T {
    var result: T? = null
    var rowCount = 0
    val duration = measureTimeMillis {
        val blockResult = block()
        result = blockResult
        // Try to get row count if result is a collection
        rowCount = when (blockResult) {
            is Collection<*> -> blockResult.size
            is Array<*> -> blockResult.size
            else -> 1
        }
    }
    MetricsLogger.logDatabaseQuery(queryDescription, duration, rowCount)
    return result!!
}

/**
 * Extension function to measure exchange API call time.
 *
 * Example:
 * ```
 * val balance = measureExchangeApiCall("Binance", "get_balance") {
 *     binanceApi.getBalance()
 * }
 * ```
 */
inline fun <T> measureExchangeApiCall(
    exchange: String,
    operation: String,
    block: () -> T
): T {
    var result: T? = null
    var success = false
    val duration = measureTimeMillis {
        try {
            result = block()
            success = true
        } catch (e: Exception) {
            success = false
            throw e
        }
    }
    MetricsLogger.logExchangeApiCall(exchange, operation, duration, success)
    return result!!
}

