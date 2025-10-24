package com.fmps.autotrader.shared.dto

import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for Configuration
 * Used for API requests and responses
 */
@Serializable
data class ConfigurationDTO(
    val id: Long? = null,
    val key: String,
    val value: String,
    val category: String,
    val description: String? = null,
    val isSystemConfig: Boolean = false,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Request DTO for updating configuration value
 */
@Serializable
data class UpdateConfigurationRequest(
    val value: String
)

/**
 * Response DTO with multiple configurations
 */
@Serializable
data class ConfigurationListResponse(
    val configurations: List<ConfigurationDTO>,
    val total: Long
)

