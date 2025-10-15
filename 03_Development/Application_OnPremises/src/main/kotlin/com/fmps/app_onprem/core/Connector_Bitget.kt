package de.fmps.onprem

package com.example.bitgetconnector

import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64
import java.net.HttpURLConnection
import java.net.URL

/**
 * Connector_Bitget
 *
 * Eine Klasse, die die REST-API von Bitget ansteuert.
 * Unterstützt Authentifizierung, Positionsmanagement und Kursabfragen.
 */
class Connector_Bitget(
    private val apiKey: String,
    private val apiSecret: String,
    private val passphrase: String,
    private val baseUrl: String = "https://api.bitget.com"
) {

    // ======================
    // ENUMS & STATES
    // ======================
    enum class ConnectionState { CONNECTED, DISCONNECTED, ERROR }
    enum class PositionType { LONG, SHORT }

    // ======================
    // FELDER
    // ======================
    var connectionState: ConnectionState = ConnectionState.DISCONNECTED
        private set

    // ======================
    // Öffentliche Methoden
    // ======================

    /** Prüft die Verbindung zur API */
    fun testConnection(): Boolean {
        val endpoint = "/api/spot/v1/market/ticker?symbol=BTCUSDT"
        return try {
            val response = sendPublicRequest(endpoint)
            connectionState = if (response.contains("BTCUSDT")) ConnectionState.CONNECTED else ConnectionState.ERROR
            connectionState == ConnectionState.CONNECTED
        } catch (e: Exception) {
            connectionState = ConnectionState.ERROR
            false
        }
    }

    /** Ruft den aktuellen Kurs eines Symbols ab */
    fun getTicker(symbol: String): String {
        val endpoint = "/api/spot/v1/market/ticker?symbol=$symbol"
        return sendPublicRequest(endpoint)
    }

    /** Öffnet eine Position (Long oder Short) */
    fun openPosition(symbol: String, positionType: PositionType, size: Double, leverage: Int): String {
        val endpoint = "/api/mix/v1/order/placeOrder"
        val body = """
            {
              "symbol":"$symbol",
              "marginCoin":"USDT",
              "side":"${if (positionType == PositionType.LONG) "open_long" else "open_short"}",
              "orderType":"market",
              "size":"$size",
              "leverage":"$leverage"
            }
        """.trimIndent()
        return sendPrivateRequest("POST", endpoint, body)
    }

    /** Schließt eine Position */
    fun closePosition(symbol: String, positionType: PositionType, size: Double): String {
        val endpoint = "/api/mix/v1/order/placeOrder"
        val body = """
            {
              "symbol":"$symbol",
              "marginCoin":"USDT",
              "side":"${if (positionType == PositionType.LONG) "close_long" else "close_short"}",
              "orderType":"market",
              "size":"$size"
            }
        """.trimIndent()
        return sendPrivateRequest("POST", endpoint, body)
    }

    /** Setzt oder ändert den Stop-Loss */
    fun setStopLoss(symbol: String, stopLossPrice: Double): String {
        val endpoint = "/api/mix/v1/plan/placePlan"
        val body = """
            {
              "symbol":"$symbol",
              "marginCoin":"USDT",
              "planType":"loss_plan",
              "triggerPrice":"$stopLossPrice",
              "side":"sell",
              "size":"0.001"
            }
        """.trimIndent()
        return sendPrivateRequest("POST", endpoint, body)
    }

    /** Ändert den Leverage */
    fun updateLeverage(symbol: String, leverage: Int): String {
        val endpoint = "/api/mix/v1/account/setLeverage"
        val body = """
            {
              "symbol":"$symbol",
              "marginCoin":"USDT",
              "leverage":"$leverage"
            }
        """.trimIndent()
        return sendPrivateRequest("POST", endpoint, body)
    }

    // ======================
    // Private Hilfsmethoden
    // ======================

    /** Öffentliche API-Anfrage ohne Authentifizierung */
    private fun sendPublicRequest(endpoint: String): String {
        val url = URL("$baseUrl$endpoint")
        val con = url.openConnection() as HttpURLConnection
        con.requestMethod = "GET"
        return con.inputStream.bufferedReader().use { it.readText() }
    }

    /** Private API-Anfrage mit Signierung */
    private fun sendPrivateRequest(method: String, endpoint: String, body: String = ""): String {
        val timestamp = Instant.now().toEpochMilli().toString()
        val message = "$timestamp$method$endpoint$body"
        val signature = hmacSHA256(message, apiSecret)

        val url = URL("$baseUrl$endpoint")
        val con = url.openConnection() as HttpURLConnection
        con.requestMethod = method
        con.setRequestProperty("ACCESS-KEY", apiKey)
        con.setRequestProperty("ACCESS-SIGN", signature)
        con.setRequestProperty("ACCESS-TIMESTAMP", timestamp)
        con.setRequestProperty("ACCESS-PASSPHRASE", passphrase)
        con.setRequestProperty("Content-Type", "application/json")
        con.doOutput = body.isNotEmpty()
        if (body.isNotEmpty()) {
            con.outputStream.use { it.write(body.toByteArray()) }
        }
        return con.inputStream.bufferedReader().use { it.readText() }
    }

    private fun hmacSHA256(data: String, secret: String): String {
        val hmacKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(hmacKey)
        val result = mac.doFinal(data.toByteArray())
        return Base64.getEncoder().encodeToString(result)
    }

}


