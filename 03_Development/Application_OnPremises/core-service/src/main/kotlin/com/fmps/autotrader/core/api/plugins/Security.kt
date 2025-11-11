package com.fmps.autotrader.core.api.plugins

import com.fmps.autotrader.core.api.security.ApiSecuritySettingsLoader
import com.fmps.autotrader.shared.dto.ErrorDetail
import com.fmps.autotrader.shared.dto.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import mu.KotlinLogging
import java.time.Instant

private val securityLogger = KotlinLogging.logger {}

/**
 * Configures API key security for REST endpoints using the shared security settings loader.
 */
fun Application.configureSecurity() {
    val settings = ApiSecuritySettingsLoader.load(environment.config)
    if (settings == null) {
        securityLogger.warn { "Security configuration missing: security.api block not found. API key enforcement disabled." }
        return
    }

    if (!settings.enabled) {
        securityLogger.info { "API key security disabled via configuration." }
        return
    }

    if (settings.validKeys.isEmpty() && settings.metricsKey == null) {
        securityLogger.error { "API key security is enabled but no keys are configured. Enforcement skipped." }
        return
    }

    securityLogger.info {
        val maskedKeys = settings.validKeys.map { maskKey(it) }
        val maskedMetricsKey = settings.metricsKey?.let { maskKey(it) }
        buildString {
            append("API key security enabled. Header='${settings.headerName}', QueryParam='${settings.queryParamName}', ")
            append("Keys=${maskedKeys.joinToString()}")
            if (maskedMetricsKey != null) {
                append(", MetricsKey=$maskedMetricsKey")
            }
            if (settings.excludedPaths.isNotEmpty()) {
                append(", ExcludedPaths=${settings.excludedPaths.joinToString()}")
            }
        }
    }

    intercept(ApplicationCallPipeline.Plugins) {
        val requestPath = call.request.path()
        if (isExcludedPath(requestPath, settings.excludedPaths)) {
            return@intercept
        }

        val providedKey = call.request.headers[settings.headerName]
            ?: call.request.queryParameters[settings.queryParamName]

        if (!settings.isKeyValid(providedKey)) {
            val errorCode = if (providedKey == null) "API_KEY_MISSING" else "API_KEY_INVALID"
            val detail = ErrorDetail(
                code = errorCode,
                message = if (providedKey == null) {
                    "API key is required. Provide header '${settings.headerName}' or query parameter '${settings.queryParamName}'."
                } else {
                    "Provided API key is not valid."
                },
                details = mapOf(
                    "path" to requestPath,
                    "header" to settings.headerName,
                    "queryParam" to settings.queryParamName
                )
            )

            securityLogger.warn { "Unauthorized request to $requestPath - reason=$errorCode" }

            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(
                    success = false,
                    error = detail,
                    timestamp = Instant.now().toString()
                )
            )
            finish()
        }
    }
}

private fun maskKey(key: String, unmaskedStart: Int = 3, unmaskedEnd: Int = 2): String {
    if (key.length <= unmaskedStart + unmaskedEnd) return "*".repeat(key.length)
    val start = key.substring(0, unmaskedStart)
    val end = key.substring(key.length - unmaskedEnd)
    return "$start***$end"
}

private fun isExcludedPath(requestPath: String, excluded: List<String>): Boolean =
    excluded.any { configured ->
        when {
            configured.endsWith("*") -> requestPath.startsWith(configured.removeSuffix("*"))
            else -> requestPath == configured
        }
    }



