package com.fmps.autotrader.core.api.routes

import com.fmps.autotrader.core.api.security.ApiSecuritySettingsLoader
import com.fmps.autotrader.core.api.websocket.TelemetryConnectionContext
import com.fmps.autotrader.core.api.websocket.TelemetryHub
import com.fmps.autotrader.core.api.websocket.TelemetryMetricsSnapshot
import com.fmps.autotrader.core.telemetry.TelemetryChannel
import com.fmps.autotrader.core.telemetry.TelemetryCollector
import com.fmps.autotrader.shared.dto.ApiResponse
import com.fmps.autotrader.shared.dto.ErrorDetail
import com.fmps.autotrader.shared.dto.ErrorResponse
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import mu.KotlinLogging
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

private val telemetryLogger = KotlinLogging.logger { }

@Serializable
data class TelemetryStatsResponse(
    val metrics: TelemetryMetricsSnapshot,
    val channelSnapshots: Map<String, Int>
)

@Serializable
data class TelemetryClientDisconnectResponse(
    val clientId: String,
    val disconnected: Boolean,
    val reason: String
)

/**
 * Configure WebSocket routes for telemetry streaming.
 */
fun Routing.configureWebSocketRoutes() {
    val environment = application.environment
    val securitySettings = ApiSecuritySettingsLoader.load(environment.config)

    webSocket("/ws/telemetry") {
        val remoteHost = call.request.local.remoteHost
        val requestedChannels = call.request.queryParameters["channels"]
            ?.split(',')
            ?.mapNotNull { TelemetryChannel.fromWireName(it.trim()) }
            ?.toSet()
            ?: emptySet()
        val replayOnConnect = call.request.queryParameters["replay"]?.toBoolean() ?: false
        val clientId = call.request.queryParameters["clientId"] ?: UUID.randomUUID().toString()

        val providedKey = call.request.headers[securitySettings?.headerName ?: "X-API-Key"]
            ?: call.request.queryParameters[securitySettings?.queryParamName ?: "apiKey"]

        if (securitySettings?.enabled == true) {
            val isExcluded = securitySettings.excludedPaths.any { path ->
                when {
                    path.endsWith("*") -> call.request.path().startsWith(path.removeSuffix("*"))
                    else -> call.request.path() == path
                }
            }
            if (!isExcluded && !securitySettings.isKeyValid(providedKey)) {
                telemetryLogger.warn { "Telemetry connection rejected for client=$clientId - invalid API key" }
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                return@webSocket
            }
        }

        val context = TelemetryConnectionContext(
            clientId = clientId,
            apiKey = providedKey,
            initialChannels = requestedChannels,
            replayOnConnect = replayOnConnect,
            remoteAddress = remoteHost
        )

        TelemetryHub.handleConnection(this, environment, context)
    }

    get("/api/v1/websocket/stats") {
        val channelCounts = TelemetryChannel.values().associate { channel ->
            channel.wireName to TelemetryCollector.snapshot(channel).size
        }
        val metrics = TelemetryHub.metrics()
        call.respond(
            ApiResponse(
                data = TelemetryStatsResponse(metrics, channelCounts),
                timestamp = Instant.now().toString()
            )
        )
    }

    get("/api/v1/websocket/clients") {
        val clients = TelemetryHub.clients()
        call.respond(
            ApiResponse(
                data = clients,
                timestamp = Instant.now().toString()
            )
        )
    }

    delete("/api/v1/websocket/clients/{clientId}") {
        val clientId = call.parameters["clientId"]
            ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "CLIENT_ID_REQUIRED",
                        message = "Client ID is required"
                    ),
                    timestamp = Instant.now().toString()
                )
            )
        val reason = call.request.queryParameters["reason"] ?: "Admin disconnect"
        val disconnected = TelemetryHub.disconnectClient(clientId, reason)
        if (disconnected) {
            call.respond(
                ApiResponse(
                    data = TelemetryClientDisconnectResponse(
                        clientId = clientId,
                        disconnected = true,
                        reason = reason
                    ),
                    timestamp = Instant.now().toString()
                )
            )
        } else {
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "CLIENT_NOT_FOUND",
                        message = "Client '$clientId' is not connected"
                    ),
                    timestamp = Instant.now().toString()
                )
            )
        }
    }
}

