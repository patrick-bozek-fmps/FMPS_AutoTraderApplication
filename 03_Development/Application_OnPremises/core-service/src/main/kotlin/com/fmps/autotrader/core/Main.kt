package com.fmps.autotrader.core

import com.fmps.autotrader.core.database.DatabaseFactory
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger("Main")

fun main() {
    logger.info("=".repeat(80))
    logger.info("FMPS AutoTrader Core Service")
    logger.info("Version: 1.0.0-SNAPSHOT")
    logger.info("=".repeat(80))
    
    try {
        // Load configuration
        logger.info("Loading configuration...")
        val config = ConfigFactory.load()
        
        // Initialize database
        logger.info("Initializing database...")
        DatabaseFactory.init(config)
        logger.info("✓ Database initialized successfully")
        
        // Register shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("Shutting down...")
            DatabaseFactory.close()
            logger.info("✓ Shutdown complete")
        })
        
        // TODO: Initialize REST API server (Issue #3)
        // TODO: Initialize AI Trader Manager (Phase 3)
        // TODO: Initialize Exchange Connectors (Phase 2)
        
        logger.info("=".repeat(80))
        logger.info("✓ Core service is running")
        logger.info("Press Ctrl+C to stop")
        logger.info("=".repeat(80))
        
        // Keep the application running
        Thread.currentThread().join()
        
    } catch (e: Exception) {
        logger.error("Failed to start Core Service", e)
        exitProcess(1)
    }
}

