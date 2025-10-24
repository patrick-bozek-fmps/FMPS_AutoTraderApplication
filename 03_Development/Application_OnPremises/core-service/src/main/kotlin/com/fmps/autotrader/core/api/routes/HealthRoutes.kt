package com.fmps.autotrader.core.api.routes

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
            val aiTraderRepo = AITraderRepository()
            val activeTraders = aiTraderRepo.findActive().size.toLong()
            
            // Get database connection stats
            val dbStats = mapOf(
                "status" to "connected",
                "type" to "SQLite"
            )
            
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
                    databaseStats = mapOf("error" to e.message.orEmpty())
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
}


