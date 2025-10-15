package de.fmps.onprem

class Connector_TradingView {

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

data class TradingViewSignal(
    val symbol: String,
    val signal: String,
    val strategy: String? = null,
    val indicator: String? = null,
    val value: Double? = null
)

class Connector_TradingView(
    private val port: Int = 8081
) {

    private val objectMapper = jacksonObjectMapper()
    private var lastSignal: TradingViewSignal? = null

    fun startServer(onSignal: (TradingViewSignal) -> Unit) {
        val server = HttpServer.create(InetSocketAddress(port), 0)
        server.createContext("/webhook") { exchange ->
            if (exchange.requestMethod == "POST") {
                val body = exchange.requestBody.bufferedReader().readText()
                val signal = objectMapper.readValue(body, TradingViewSignal::class.java)
                lastSignal = signal
                onSignal(signal)
                val response = "Signal received: ${signal.symbol} ${signal.signal}"
                exchange.sendResponseHeaders(200, response.length.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            } else {
                exchange.sendResponseHeaders(405, 0)
            }
        }
        server.start()
        println("📡 TradingView Connector läuft auf Port $port")
    }

    fun getLastSignal(): TradingViewSignal? = lastSignal
}


}


