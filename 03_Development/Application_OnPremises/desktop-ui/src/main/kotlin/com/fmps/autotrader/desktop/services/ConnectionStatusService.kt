package com.fmps.autotrader.desktop.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

// Using existing ConnectionStatus from MarketDataService

/**
 * Service that monitors the connection status to the core service.
 * Tracks both REST API and WebSocket connectivity.
 */
class ConnectionStatusService(
    private val httpClient: HttpClient,
    private val telemetryClient: TelemetryClient,
    private val baseUrl: String = "http://localhost:8080"
) {
    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.RECONNECTING)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val monitoring = AtomicBoolean(false)
    private var monitoringJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val healthCheckIntervalMs = 5000L
    
    /**
     * Start monitoring connection status.
     */
    fun startMonitoring() {
        if (monitoring.getAndSet(true)) {
            logger.warn { "ConnectionStatusService already monitoring" }
            return
        }
        
        logger.info { "Starting connection status monitoring" }
        monitoringJob = scope.launch {
            while (monitoring.get()) {
                checkConnectionStatus()
                delay(healthCheckIntervalMs)
            }
        }
    }
    
    /**
     * Stop monitoring connection status.
     */
    fun stopMonitoring() {
        if (!monitoring.getAndSet(false)) {
            return
        }
        
        logger.info { "Stopping connection status monitoring" }
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    /**
     * Check the current connection status by:
     * 1. Testing REST API health endpoint
     * 2. Checking WebSocket connection status (if available)
     */
    private suspend fun checkConnectionStatus() {
        try {
            // Check REST API health endpoint (try /health or /api/health)
            val response = withTimeout(3000) {
                httpClient.get("$baseUrl/api/health")
            }
            
            if (response.status.isSuccess()) {
                // REST API is working - this is sufficient for "Connected" status
                // WebSocket is optional for real-time features
                _status.value = ConnectionStatus.CONNECTED
                _errorMessage.value = null
                
                // Optionally check WebSocket status for informational purposes
                val isWebSocketConnected = (telemetryClient as? RealTelemetryClient)?.isConnected() == true
                if (!isWebSocketConnected) {
                    // WebSocket not connected, but don't change status - just log it
                    logger.debug { "REST API connected, but WebSocket is not connected. Real-time features may not work." }
                }
            } else {
                _status.value = ConnectionStatus.DISCONNECTED
                _errorMessage.value = "Core service returned error: ${response.status}"
            }
        } catch (e: Exception) {
            _status.value = ConnectionStatus.DISCONNECTED
            val userFriendlyMessage = when {
                e.message?.contains("Connection refused") == true -> 
                    "Cannot connect to core service. Please ensure the core service is running on localhost:8080"
                e.message?.contains("timeout") == true -> 
                    "Connection timeout. The core service may be slow to respond."
                e.message?.contains("getsockopt") == true -> 
                    "Cannot connect to core service. Please ensure the core service is running."
                else -> 
                    "Connection error: ${e.message ?: "Unknown error"}"
            }
            _errorMessage.value = userFriendlyMessage
            logger.debug(e) { "Connection check failed" }
        }
    }
    
    /**
     * Get user-friendly instructions for starting the core service.
     */
    fun getStartInstructions(): String {
        return """
            To start the core service:
            
            1. Open a terminal in the project root directory
            2. Navigate to: 03_Development/Application_OnPremises
            3. Run: .\gradlew.bat :core-service:run
            
            The service will start on http://localhost:8080
        """.trimIndent()
    }
}

