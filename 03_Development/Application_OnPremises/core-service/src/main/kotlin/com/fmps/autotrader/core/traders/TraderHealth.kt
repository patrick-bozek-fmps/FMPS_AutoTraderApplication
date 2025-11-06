package com.fmps.autotrader.core.traders

import java.time.Instant

/**
 * Represents the health status of an AI Trader instance.
 *
 * Used by HealthMonitor to track trader health metrics and identify issues.
 *
 * @property isHealthy Whether the trader is in a healthy state
 * @property status Current trader state (RUNNING, ERROR, etc.)
 * @property lastUpdate Timestamp of last health check
 * @property lastSignalTime Timestamp of last signal generation (null if never generated)
 * @property exchangeConnectorHealthy Whether the exchange connector is healthy
 * @property errorCount Number of errors encountered
 * @property issues List of health issues identified
 *
 * @since 1.0.0
 */
data class TraderHealth(
    val isHealthy: Boolean,
    val status: AITraderState,
    val lastUpdate: Instant,
    val lastSignalTime: Instant? = null,
    val exchangeConnectorHealthy: Boolean = true,
    val errorCount: Int = 0,
    val issues: List<String> = emptyList()
) {
    companion object {
        /**
         * Create a healthy trader health status.
         */
        fun healthy(
            status: AITraderState,
            lastSignalTime: Instant? = null
        ): TraderHealth {
            return TraderHealth(
                isHealthy = true,
                status = status,
                lastUpdate = Instant.now(),
                lastSignalTime = lastSignalTime,
                exchangeConnectorHealthy = true,
                errorCount = 0,
                issues = emptyList()
            )
        }

        /**
         * Create an unhealthy trader health status with issues.
         */
        fun unhealthy(
            status: AITraderState,
            issues: List<String>,
            errorCount: Int = 0,
            exchangeConnectorHealthy: Boolean = true
        ): TraderHealth {
            return TraderHealth(
                isHealthy = false,
                status = status,
                lastUpdate = Instant.now(),
                exchangeConnectorHealthy = exchangeConnectorHealthy,
                errorCount = errorCount,
                issues = issues
            )
        }
    }
}






