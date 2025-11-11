package com.fmps.autotrader.core.api.security

import io.ktor.server.config.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Represents API key security settings loaded from application configuration.
 */
data class ApiSecuritySettings(
    val enabled: Boolean,
    val headerName: String,
    val queryParamName: String,
    val validKeys: Set<String>,
    val excludedPaths: List<String>,
    val metricsKey: String?
) {
    fun isKeyValid(candidate: String?): Boolean {
        if (!enabled) return true
        if (candidate == null) return false
        if (metricsKey != null && candidate == metricsKey) return true
        return candidate in validKeys
    }
}

/**
 * Loads API key security settings from the application configuration.
 */
object ApiSecuritySettingsLoader {

    fun load(config: ApplicationConfig): ApiSecuritySettings? {
        val securityConfig = config.configOrNull("security.api") ?: return null

        val enabled = securityConfig.propertyOrNull("enabled")?.getString()?.toBooleanStrictOrNull() ?: false
        val headerName = securityConfig.propertyOrNull("header")?.getString()?.trim()?.takeIf { it.isNotEmpty() }
            ?: "X-API-Key"
        val queryParamName = securityConfig.propertyOrNull("queryParam")?.getString()?.trim()?.takeIf { it.isNotEmpty() }
            ?: "apiKey"
        val excludedPaths = securityConfig.propertyOrNull("excludedPaths")?.getList()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        val metricsKey = securityConfig.propertyOrNull("metricsKey")?.getString()?.trim()?.takeIf { it.isNotEmpty() }

        val configuredKeys = securityConfig.propertyOrNull("keys")?.getList()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        val singleKey = securityConfig.propertyOrNull("key")?.getString()?.trim()?.takeIf { it.isNotEmpty() }
        val envVarName = securityConfig.propertyOrNull("envKey")?.getString()?.trim()?.takeIf { it.isNotEmpty() }
        val envKey = envVarName?.let { System.getenv(it)?.trim()?.takeIf { value -> value.isNotEmpty() } }

        val validKeys = (configuredKeys + listOfNotNull(singleKey, envKey)).toSet()

        if (enabled && validKeys.isEmpty() && metricsKey == null) {
            logger.error { "API key security is enabled but no keys are configured. Enforcement will effectively allow only metrics key if provided." }
        }

        return ApiSecuritySettings(
            enabled = enabled,
            headerName = headerName,
            queryParamName = queryParamName,
            validKeys = validKeys,
            excludedPaths = excludedPaths,
            metricsKey = metricsKey
        )
    }
}

private fun ApplicationConfig.configOrNull(path: String): ApplicationConfig? =
    try {
        config(path)
    } catch (_: ApplicationConfigurationException) {
        null
    }
