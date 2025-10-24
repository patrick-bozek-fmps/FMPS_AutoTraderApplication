package com.fmps.autotrader.core.api

import com.fmps.autotrader.core.api.plugins.*
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

/**
 * Starts the Ktor REST API server
 * 
 * @param host The host address to bind to (default: 0.0.0.0)
 * @param port The port to listen on (default: 8080)
 * @param wait Whether to block the calling thread (default: false for integration)
 * @return The ApplicationEngine instance
 */
fun startApiServer(
    host: String = "0.0.0.0",
    port: Int = 8080,
    wait: Boolean = false
): ApplicationEngine {
    logger.info("Starting FMPS AutoTrader API Server...")
    logger.info("Host: $host")
    logger.info("Port: $port")
    
    val server = embeddedServer(
        Netty,
        port = port,
        host = host,
        module = Application::module
    )
    
    server.start(wait = wait)
    logger.info("✓ API Server started successfully on http://$host:$port")
    
    return server
}

/**
 * Standalone entry point for running just the API server
 * Useful for development and testing
 */
fun main() {
    val config = ConfigFactory.load()
    val host = config.getString("app.host")
    val port = config.getInt("app.port")
    
    startApiServer(host = host, port = port, wait = true)
}

/**
 * Configure Ktor application with all plugins and routes
 */
fun Application.module() {
    // Configure server features/plugins
    configureMonitoring()
    configureSerialization()
    configureHTTP()
    configureWebSockets()
    configureRouting()
}


