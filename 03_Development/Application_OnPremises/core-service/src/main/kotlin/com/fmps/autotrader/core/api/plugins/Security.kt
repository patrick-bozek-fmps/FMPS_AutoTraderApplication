package com.fmps.autotrader.core.api.plugins

import com.fmps.autotrader.shared.dto.ErrorDetail
import com.fmps.autotrader.shared.dto.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.config.ApplicationConfigurationException
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import mu.KotlinLogging
import java.time.Instant

private val securityLogger = KotlinLogging.logger {}

/**
 * Configures simple API key security for REST endpoints.
 *
 * Uses configuration from `security.api` block. Example:
 *
 * security {
 *   api {
 *     enabled = true
 *     header = "X-API-Key"
 *     queryParam = "apiKey"
 *     envKey = "FMPS_API_KEY"
 *     keys = ["dev-api-key"]
 *     excludedPaths = ["/api/health"]
 *   }
 * }
 */
fun Application.configureSecurity() {
    val apiConfig = environment.config.configOrNull("security.api")
    if (apiConfig == null) {
        securityLogger.warn { "Security configuration missing: security.api block not found. API key enforcement disabled." }
        return
    }

    val enabled = apiConfig.propertyOrNull("enabled")?.getString()?.toBooleanStrictOrNull() ?: false
    if (!enabled) {
        securityLogger.info { "API key security disabled via configuration." }
        return
    }

    val headerName = apiConfig.propertyOrNull("header")?.getString()?.trim()?.takeIf { it.isNotEmpty() } ?: "X-API-Key"
    val queryParamName = apiConfig.propertyOrNull("queryParam")?.getString()?.trim()?.takeIf { it.isNotEmpty() } ?: "apiKey"
    val excludedPaths = apiConfig.propertyOrNull("excludedPaths")?.getList()?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
    val envVarName = apiConfig.propertyOrNull("envKey")?.getString()?.trim()?.takeIf { it.isNotEmpty() }
    val configuredKeys = apiConfig.propertyOrNull("keys")?.getList()?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    val singleKey = apiConfig.propertyOrNull("key")?.getString()?.trim()?.takeIf { it.isNotEmpty() }
    val envKey = envVarName?.let { System.getenv(it)?.trim()?.takeIf(String::isNotEmpty) }

    val validKeys = (configuredKeys + listOfNotNull(singleKey, envKey)).toSet()

    if (validKeys.isEmpty()) {
        securityLogger.error { "API key security is enabled but no keys are configured. Enforcement skipped." }
        return
    }

    securityLogger.info {
        val maskedKeys = validKeys.map { maskKey(it) }
        "API key security enabled. Header='$headerName', QueryParam='$queryParamName', Keys=${maskedKeys.joinToString()}, ExcludedPaths=${excludedPaths.joinToString()}"
    }

    intercept(ApplicationCallPipeline.Plugins) {
        val requestPath = call.request.path()
        if (isExcludedPath(requestPath, excludedPaths)) {
            return@intercept
        }

        val providedKey = call.request.headers[headerName] ?: call.request.queryParameters[queryParamName]

        if (providedKey == null || providedKey !in validKeys) {
            val errorCode = if (providedKey == null) "API_KEY_MISSING" else "API_KEY_INVALID"
            val detail = ErrorDetail(
                code = errorCode,
                message = if (providedKey == null) {
                    "API key is required. Provide header '$headerName' or query parameter '$queryParamName'."
                } else {
                    "Provided API key is not valid."
                },
                details = mapOf(
                    "path" to requestPath,
                    "header" to headerName,
                    "queryParam" to queryParamName
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

private fun ApplicationConfig.configOrNull(path: String): ApplicationConfig? =
    try {
        config(path)
    } catch (_: ApplicationConfigurationException) {
        null
    }

private fun isExcludedPath(requestPath: String, excluded: List<String>): Boolean =
    excluded.any { configured ->
        when {
            configured.endsWith("*") -> requestPath.startsWith(configured.removeSuffix("*"))
            else -> requestPath == configured
        }
    }



