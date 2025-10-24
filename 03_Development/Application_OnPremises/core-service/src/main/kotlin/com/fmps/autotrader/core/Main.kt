package com.fmps.autotrader.core

import com.fmps.autotrader.core.api.startApiServer
import com.fmps.autotrader.core.database.DatabaseFactory
import com.typesafe.config.ConfigFactory
import io.ktor.server.engine.*
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger("Main")

fun main() {
    logger.info("=".repeat(80))
    logger.info("FMPS AutoTrader Core Service")
    logger.info("Version: 1.0.0-SNAPSHOT")
    logger.info("=".repeat(80))
    
    var apiServer: ApplicationEngine? = null
    
    try {
        // Load configuration
        logger.info("Loading configuration...")
        val config = ConfigFactory.load()
        val apiHost = config.getString("app.host")
        val apiPort = config.getInt("app.port")
        
        // Initialize database
        logger.info("Initializing database...")
        DatabaseFactory.init(config)
        logger.info("✓ Database initialized successfully")
        
        // Initialize REST API server
        logger.info("Starting REST API server...")
        apiServer = startApiServer(host = apiHost, port = apiPort, wait = false)
        logger.info("✓ REST API server started on http://$apiHost:$apiPort")
        
        // Register shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("Shutting down...")
            
            // Stop API server
            apiServer?.stop(gracePeriodMillis = 1000, timeoutMillis = 5000)
            logger.info("✓ API server stopped")
            
            // Close database connections
            DatabaseFactory.close()
            logger.info("✓ Database connections closed")
            
            logger.info("✓ Shutdown complete")
        })
        
        // TODO: Initialize AI Trader Manager (Phase 3)
        // TODO: Initialize Exchange Connectors (Phase 2)
        
        logger.info("=".repeat(80))
        logger.info("✓ Core service is running")
        logger.info("✓ API: http://$apiHost:$apiPort/api/health")
        logger.info("Press Ctrl+C to stop")
        logger.info("=".repeat(80))
        
        // Keep the application running
        Thread.currentThread().join()
        
    } catch (e: Exception) {
        logger.error("Failed to start Core Service", e)
        apiServer?.stop(gracePeriodMillis = 0, timeoutMillis = 1000)
        DatabaseFactory.close()
        exitProcess(1)
    }
}

