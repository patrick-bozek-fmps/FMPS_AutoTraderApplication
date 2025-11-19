package com.fmps.autotrader.core.api

import com.fmps.autotrader.core.api.plugins.*
import com.fmps.autotrader.core.database.DatabaseFactory
import com.fmps.autotrader.core.logging.LoggingContext
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

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
    logger.info { "Starting FMPS AutoTrader API Server..." }
    logger.info { "Configuration: host=$host, port=$port, wait=$wait" }
    
    // Set operation context for startup logging
    LoggingContext.setOperation("server_startup")
    
    try {
        val server = embeddedServer(
            Netty,
            port = port,
            host = host,
            module = Application::module
        )
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info { "Shutting down API Server..." }
            LoggingContext.setOperation("server_shutdown")
            server.stop(1000, 5000)
            logger.info { "✓ API Server stopped successfully" }
            LoggingContext.clear()
        })
        
        server.start(wait = wait)
        logger.info { "✓ API Server started successfully on http://$host:$port" }
        logger.info { "Environment: ${System.getProperty("app.env", "development")}" }
        
        return server
    } catch (e: Exception) {
        logger.error(e) { "✗ Failed to start API Server" }
        throw e
    } finally {
        LoggingContext.remove("operation")
    }
}

/**
 * Standalone entry point for running just the API server
 * Useful for development and testing
 */
fun main() {
    logger.info { "=== FMPS AutoTrader Application ===" }
    logger.info { "Version: 1.0.0-SNAPSHOT" }
    logger.info { "Loading configuration..." }
    
    try {
        val config = ConfigFactory.load()
        val host = config.getString("server.host")
        val port = config.getInt("server.port")
        
        logger.info { "Configuration loaded successfully" }
        
        // Initialize database
        logger.info { "Initializing database..." }
        DatabaseFactory.init(config)
        logger.info { "✓ Database initialized successfully" }
        
        // Register shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info { "Shutting down..." }
            DatabaseFactory.close()
            logger.info { "✓ Database connections closed" }
        })
        
        startApiServer(host = host, port = port, wait = true)
    } catch (e: Exception) {
        logger.error(e) { "Failed to start application" }
        DatabaseFactory.close()
        throw e
    }
}

/**
 * Configure Ktor application with all plugins and routes
 */
fun Application.module() {
    logger.info { "Configuring Ktor application modules..." }
    
    try {
        // Configure server features/plugins
        logger.debug { "Configuring monitoring..." }
        configureMonitoring()
        
        logger.debug { "Configuring serialization..." }
        configureSerialization()
        
        logger.debug { "Configuring HTTP features..." }
        configureHTTP()
        
        logger.debug { "Configuring security..." }
        configureSecurity()
        
        logger.debug { "Configuring WebSockets..." }
        configureWebSockets()
        
        logger.debug { "Configuring routing..." }
        configureRouting()
        
        logger.info { "✓ Ktor application configured successfully" }
    } catch (e: Exception) {
        logger.error(e) { "✗ Failed to configure Ktor application" }
        throw e
    }
}


