package com.fmps.autotrader.core.connectors.bitget

import mu.KotlinLogging
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private val logger = KotlinLogging.logger {}

/**
 * Handles authentication for Bitget API requests
 * 
 * Bitget uses HMAC SHA256 signature-based authentication with a passphrase:
 * 1. Create prehash string: timestamp + method + requestPath + queryString (or body)
 * 2. Sign prehash string with API secret using HMAC SHA256
 * 3. Base64 encode the signature
 * 4. Add required headers:
 *    - ACCESS-KEY: API key
 *    - ACCESS-SIGN: Base64 encoded signature
 *    - ACCESS-TIMESTAMP: Request timestamp in milliseconds
 *    - ACCESS-PASSPHRASE: API passphrase
 *    - Content-Type: application/json
 *    - paptrading: 1 (when using demo/sandbox/testnet API keys)
 * 
 * **Signature Generation:**
 * ```
 * prehash = timestamp + method + requestPath + queryString
 * signature = Base64(HMAC_SHA256(prehash, apiSecret))
 * ```
 * 
 * **Timestamp Synchronization:**
 * Bitget requires timestamp to be within recvWindow of server time.
 * Use timestampOffset to adjust for clock differences.
 * 
 * **Demo/Sandbox Mode:**
 * When using demo/sandbox API keys, the `paptrading: 1` header must be included
 * to tell Bitget to route the request to the demo environment. Without this header,
 * Bitget returns error 40099 "exchange environment is incorrect".
 * 
 * @property apiKey Bitget API key
 * @property apiSecret Bitget API secret
 * @property passphrase Bitget API passphrase
 * @property testnet Whether using demo/sandbox/testnet API keys (adds paptrading header)
 * @property recvWindow Request validity window in milliseconds
 * @property timestampOffset Server time offset in milliseconds
 */
class BitgetAuthenticator(
    private val apiKey: String,
    private val apiSecret: String,
    private val passphrase: String,
    private val testnet: Boolean = false,
    private val recvWindow: Long = 5000,
    private var timestampOffset: Long = 0
) {
    init {
        require(apiKey.isNotBlank()) { "Bitget API key cannot be blank" }
        require(apiSecret.isNotBlank()) { "Bitget API secret cannot be blank" }
        require(passphrase.isNotBlank()) { "Bitget passphrase cannot be blank" }
        require(recvWindow > 0) { "recvWindow must be positive, got: $recvWindow" }
    }
    
    private val hmacAlgorithm = "HmacSHA256"
    private val mac: Mac = Mac.getInstance(hmacAlgorithm).apply {
        init(SecretKeySpec(apiSecret.toByteArray(), hmacAlgorithm))
    }
    
    /**
     * Creates authentication headers for signed requests
     * 
     * @param method HTTP method (GET, POST, DELETE, etc.)
     * @param requestPath Request path (e.g., "/api/spot/v1/account/assets")
     * @param queryString Query string (for GET requests) or request body (for POST requests)
     * @return Map containing all required Bitget authentication headers
     */
    fun createHeaders(method: String, requestPath: String, queryString: String = ""): Map<String, String> {
        val timestamp = getAdjustedTimestamp()
        val prehash = createPrehashString(timestamp, method, requestPath, queryString)
        val signature = generateSignature(prehash)
        
        val headers = mutableMapOf(
            "ACCESS-KEY" to apiKey,
            "ACCESS-SIGN" to signature,
            "ACCESS-TIMESTAMP" to timestamp.toString(),
            "ACCESS-PASSPHRASE" to passphrase,
            "Content-Type" to "application/json"
        )
        
        // Add paptrading header for demo/sandbox/testnet API keys
        // This tells Bitget to route the request to the demo environment
        // Without this header, Bitget returns error 40099 "exchange environment is incorrect"
        if (testnet) {
            headers["paptrading"] = "1"
            logger.info { "✓ Added paptrading: 1 header for demo/sandbox mode" }
            println("✓ [BitgetAuthenticator] Added paptrading: 1 header for demo/sandbox mode")
        } else {
            logger.debug { "Not adding paptrading header (production mode)" }
        }
        
        return headers
    }
    
    /**
     * Creates the prehash string for signature generation
     * 
     * Format: timestamp + method + requestPath + queryString
     * 
     * @param timestamp Request timestamp in milliseconds
     * @param method HTTP method (uppercase)
     * @param requestPath Request path starting with /
     * @param queryString Query string or request body
     * @return Prehash string
     */
    private fun createPrehashString(
        timestamp: Long,
        method: String,
        requestPath: String,
        queryString: String
    ): String {
        return "$timestamp${method.uppercase()}$requestPath$queryString"
    }
    
    /**
     * Generates HMAC SHA256 signature and Base64 encodes it
     * 
     * @param message Message to sign
     * @return Base64-encoded signature
     */
    private fun generateSignature(message: String): String {
        synchronized(mac) {
            val signedBytes = mac.doFinal(message.toByteArray())
            return Base64.getEncoder().encodeToString(signedBytes)
        }
    }
    
    /**
     * Signs a query string for GET requests
     * 
     * @param method HTTP method
     * @param requestPath Request path
     * @param parameters Map of query parameters
     * @return Query string with signature
     */
    fun signRequest(
        method: String,
        requestPath: String,
        parameters: Map<String, String> = emptyMap()
    ): Pair<String, Map<String, String>> {
        // Create query string from parameters
        val queryString = if (parameters.isNotEmpty()) {
            parameters.entries
                .sortedBy { it.key }
                .joinToString("&") { "${it.key}=${it.value}" }
        } else {
            ""
        }
        
        // For GET requests, query string is part of the URL
        // For POST requests, it would be the request body
        val headers = createHeaders(method, requestPath, if (method == "GET") "?$queryString" else queryString)
        
        return Pair(queryString, headers)
    }
    
    /**
     * Updates the timestamp offset
     * Used to synchronize with Bitget server time
     * 
     * @param serverTime Bitget server time in milliseconds
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
     * Gets the current timestamp offset
     * 
     * @return Timestamp offset in milliseconds
     */
    fun getTimestampOffset(): Long = timestampOffset
    
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
        require(passphrase.isNotBlank()) { "Passphrase cannot be blank" }
        logger.info { "Bitget credentials validated (API key: ${apiKey.take(8)}...)" }
    }
}

