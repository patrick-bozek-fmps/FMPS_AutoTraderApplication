package com.fmps.autotrader.core.connectors.binance

import mu.KotlinLogging
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val logger = KotlinLogging.logger {}

/**
 * Handles authentication for Binance API requests
 * 
 * Binance uses HMAC SHA256 signature-based authentication:
 * 1. Create query string with parameters + timestamp
 * 2. Sign query string with API secret using HMAC SHA256
 * 3. Append signature to query string
 * 4. Add X-MBX-APIKEY header with API key
 * 
 * **Signature Generation:**
 * ```
 * signature = hex(HMAC_SHA256(queryString, apiSecret))
 * ```
 * 
 * **Timestamp Synchronization:**
 * Binance requires timestamp to be within recvWindow (default 5000ms) of server time.
 * Use timestampOffset to adjust for clock differences.
 * 
 * @property apiKey Binance API key
 * @property apiSecret Binance API secret
 * @property recvWindow Request validity window in milliseconds
 * @property timestampOffset Server time offset in milliseconds
 */
class BinanceAuthenticator(
    private val apiKey: String,
    private val apiSecret: String,
    private val recvWindow: Long = 5000,
    private var timestampOffset: Long = 0
) {
    init {
        require(apiKey.isNotBlank()) { "Binance API key cannot be blank" }
        require(apiSecret.isNotBlank()) { "Binance API secret cannot be blank" }
        require(recvWindow > 0) { "recvWindow must be positive, got: $recvWindow" }
    }
    
    private val hmacAlgorithm = "HmacSHA256"
    private val mac: Mac = Mac.getInstance(hmacAlgorithm).apply {
        init(SecretKeySpec(apiSecret.toByteArray(), hmacAlgorithm))
    }
    
    /**
     * Creates authentication headers for signed requests
     * 
     * @return Map containing X-MBX-APIKEY header
     */
    fun createHeaders(): Map<String, String> {
        return mapOf("X-MBX-APIKEY" to apiKey)
    }
    
    /**
     * Signs a query string with HMAC SHA256
     * 
     * @param queryString Query string to sign (without signature parameter)
     * @return Complete query string with signature appended
     */
    fun signQueryString(queryString: String): String {
        // Add timestamp and recvWindow
        val timestamp = System.currentTimeMillis() + timestampOffset
        val paramsWithTimestamp = if (queryString.isNotEmpty()) {
            "$queryString&timestamp=$timestamp&recvWindow=$recvWindow"
        } else {
            "timestamp=$timestamp&recvWindow=$recvWindow"
        }
        
        // Generate signature
        val signature = generateSignature(paramsWithTimestamp)
        
        // Append signature to query string
        return "$paramsWithTimestamp&signature=$signature"
    }
    
    /**
     * Signs request parameters for signed requests
     * 
     * @param parameters Map of request parameters
     * @return Map with timestamp, recvWindow, and signature added
     */
    fun signParameters(parameters: Map<String, String>): Map<String, String> {
        val timestamp = System.currentTimeMillis() + timestampOffset
        val allParameters = parameters.toMutableMap().apply {
            put("timestamp", timestamp.toString())
            put("recvWindow", recvWindow.toString())
        }
        
        // Create query string for signing
        val queryString = allParameters.entries
            .sortedBy { it.key }
            .joinToString("&") { "${it.key}=${it.value}" }
        
        // Generate signature
        val signature = generateSignature(queryString)
        
        // Return parameters with signature
        return allParameters.apply {
            put("signature", signature)
        }
    }
    
    /**
     * Generates HMAC SHA256 signature for a message
     * 
     * @param message Message to sign
     * @return Hex-encoded signature
     */
    private fun generateSignature(message: String): String {
        synchronized(mac) {
            val signedBytes = mac.doFinal(message.toByteArray())
            return signedBytes.joinToString("") { "%02x".format(it) }
        }
    }
    
    /**
     * Updates the timestamp offset
     * Used to synchronize with Binance server time
     * 
     * @param serverTime Binance server time in milliseconds
     */
    fun updateTimestampOffset(serverTime: Long) {
        val localTime = System.currentTimeMillis()
        timestampOffset = serverTime - localTime
        
        logger.debug { "Updated timestamp offset: $timestampOffset ms (server: $serverTime, local: $localTime)" }
        
        if (Math.abs(timestampOffset) > 1000) {
            logger.warn { "Large timestamp offset detected: $timestampOffset ms. Consider synchronizing system clock." }
        }
    }
    
    /**
     * Gets the current adjusted timestamp
     * 
     * @return Current timestamp + offset in milliseconds
     */
    fun getAdjustedTimestamp(): Long {
        return System.currentTimeMillis() + timestampOffset
    }
    
    /**
     * Validates that API credentials are not empty
     * 
     * @throws IllegalStateException if credentials are invalid
     */
    fun validateCredentials() {
        require(apiKey.isNotBlank()) { "API key cannot be blank" }
        require(apiSecret.isNotBlank()) { "API secret cannot be blank" }
        logger.info { "Binance credentials validated (API key: ${apiKey.take(8)}...)" }
    }
}

