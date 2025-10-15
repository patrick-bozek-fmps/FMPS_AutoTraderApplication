package de.fmps.onprem

class Connector_Binance {

import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.net.URLEncoder
import java.util.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

data class Candle(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

class Connector_Binance(
    private val apiKey: String,
    private val apiSecret: String,
    private val baseUrl: String = "https://api.binance.com"
) {

    private val mapper = jacksonObjectMapper()

    // --- Signierung für private Requests ---
    private fun sign(queryString: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(apiSecret.toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        val hash = mac.doFinal(queryString.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    // --- Verbindung prüfen ---
    fun isConnected(): Boolean {
        return try {
            val url = URL("$baseUrl/api/v3/ping")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 3000
            conn.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    // --- Candlestick-Daten abrufen ---
    fun getCandles(symbol: String, interval: String = "1m", limit: Int = 100): List<Candle>? {
        val url = URL("$baseUrl/api/v3/klines?symbol=$symbol&interval=$interval&limit=$limit")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        return try {
            if (conn.responseCode == 200) {
                val data: List<List<Any>> = mapper.readValue(conn.inputStream)
                data.map {
                    Candle(
                        openTime = (it[0] as Number).toLong(),
                        open = (it[1] as String).toDouble(),
                        high = (it[2] as String).toDouble(),
                        low = (it[3] as String).toDouble(),
                        close = (it[4] as String).toDouble(),
                        volume = (it[5] as String).toDouble()
                    )
                }
            } else null
        } finally {
            conn.disconnect()
        }
    }

    // --- Beispiel: RSI Berechnung ---
    fun calculateRSI(closes: List<Double>, period: Int = 14): Double? {
        if (closes.size <= period) return null
        var gains = 0.0
        var losses = 0.0
        for (i in 1..period) {
            val diff = closes[i] - closes[i - 1]
            if (diff > 0) gains += diff else losses -= diff
        }
        val avgGain = gains / period
        val avgLoss = losses / period
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1 + rs))
    }

    // --- Order öffnen ---
    fun openPosition(symbol: String, side: String, quantity: Double, price: Double? = null): String {
        val timestamp = System.currentTimeMillis()
        val query = "symbol=$symbol&side=$side&type=MARKET&quantity=$quantity&timestamp=$timestamp"
        val signature = sign(query)
        val fullUrl = "$baseUrl/api/v3/order?$query&signature=$signature"

        val conn = URL(fullUrl).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("X-MBX-APIKEY", apiKey)
        return conn.inputStream.bufferedReader().readText()
    }

    // --- Position schließen ---
    fun closePosition(symbol: String, side: String, quantity: Double): String {
        val closeSide = if (side == "BUY") "SELL" else "BUY"
        return openPosition(symbol, closeSide, quantity)
    }

    // --- Hebel ändern (Futures) ---
    fun setLeverage(symbol: String, leverage: Int): String {
        val timestamp = System.currentTimeMillis()
        val query = "symbol=$symbol&leverage=$leverage&timestamp=$timestamp"
        val signature = sign(query)
        val fullUrl = "https://fapi.binance.com/fapi/v1/leverage?$query&signature=$signature"
        val conn = URL(fullUrl).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("X-MBX-APIKEY", apiKey)
        return conn.inputStream.bufferedReader().readText()
    }
}


}


