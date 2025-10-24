package com.fmps.autotrader.core.api

import com.fmps.autotrader.core.api.plugins.*
import com.fmps.autotrader.core.database.DatabaseFactory
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

fun main() {
    // Load configuration
    val config = ConfigFactory.load()
    val host = config.getString("app.host")
    val port = config.getInt("app.port")
    
    logger.info("Starting FMPS AutoTrader API Server...")
    logger.info("Host: $host")
    logger.info("Port: $port")
    
    // Initialize database first
    logger.info("Initializing database...")
    DatabaseFactory.init(config)
    logger.info("Database initialized successfully")
    
    // Start Ktor server
    embeddedServer(
        Netty,
        port = port,
        host = host,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    // Configure server features/plugins
    configureMonitoring()
    configureSerialization()
    configureHTTP()
    configureRouting()
}


