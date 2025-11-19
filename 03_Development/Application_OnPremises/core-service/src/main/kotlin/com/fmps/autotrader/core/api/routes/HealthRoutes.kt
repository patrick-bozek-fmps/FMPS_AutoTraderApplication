package com.fmps.autotrader.core.api.routes

import com.fmps.autotrader.core.api.plugins.prometheusRegistry
import com.fmps.autotrader.core.database.DatabaseFactory
import com.fmps.autotrader.core.database.repositories.AITraderRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: String,
    val uptime: Long
)

@Serializable
data class StatusResponse(
    val status: String,
    val timestamp: String,
    val activeTraders: Long,
    val databaseStats: Map<String, String>
)

@Serializable
data class VersionResponse(
    val version: String,
    val apiVersion: String,
    val buildDate: String
)

fun Route.configureHealthRoutes() {
    val startTime = System.currentTimeMillis()
    
    // Health check endpoint
    get("/api/health") {
        val uptime = System.currentTimeMillis() - startTime
        call.respond(
            HttpStatusCode.OK,
            HealthResponse(
                status = "UP",
                timestamp = Instant.now().toString(),
                uptime = uptime
            )
        )
    }
    
    // Detailed status endpoint
    get("/api/status") {
        try {
            // Check if database is initialized
            val isDbInitialized = try {
                DatabaseFactory.getDatabase()
                true
            } catch (e: IllegalStateException) {
                false
            }
            
            if (!isDbInitialized) {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    StatusResponse(
                        status = "DEGRADED",
                        timestamp = Instant.now().toString(),
                        activeTraders = 0,
                        databaseStats = mapOf(
                            "status" to "not_initialized",
                            "error" to "Database not initialized. Please ensure DatabaseFactory.init() is called during application startup."
                        )
                    )
                )
                return@get
            }
            
            // Call repository (route handler is already suspend context)
            val aiTraderRepo = AITraderRepository()
            val activeTraders = aiTraderRepo.findActive().size.toLong()
            
            // Get database connection stats
            val dbStats = try {
                val stats = DatabaseFactory.getStatistics()
                mapOf(
                    "status" to "connected",
                    "type" to "SQLite",
                    "activeConnections" to stats["activeConnections"].toString(),
                    "idleConnections" to stats["idleConnections"].toString(),
                    "totalConnections" to stats["totalConnections"].toString()
                )
            } catch (e: Exception) {
                mapOf(
                    "status" to "connected",
                    "type" to "SQLite",
                    "warning" to "Could not retrieve connection statistics"
                )
            }
            
            call.respond(
                HttpStatusCode.OK,
                StatusResponse(
                    status = "OPERATIONAL",
                    timestamp = Instant.now().toString(),
                    activeTraders = activeTraders,
                    databaseStats = dbStats
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                StatusResponse(
                    status = "DEGRADED",
                    timestamp = Instant.now().toString(),
                    activeTraders = 0,
                    databaseStats = mapOf("error" to (e.message ?: "Unknown error"))
                )
            )
        }
    }
    
    // Version information endpoint
    get("/api/version") {
        call.respond(
            HttpStatusCode.OK,
            VersionResponse(
                version = "1.0.0-SNAPSHOT",
                apiVersion = "v1",
                buildDate = Instant.now().toString() // TODO: Set from build process
            )
        )
    }

    // Prometheus metrics endpoint
    get("/metrics") {
        val metricsContentType = ContentType.parse("text/plain; version=0.0.4; charset=utf-8")
        call.respondText(
            text = prometheusRegistry.scrape(),
            contentType = metricsContentType
        )
    }
}


