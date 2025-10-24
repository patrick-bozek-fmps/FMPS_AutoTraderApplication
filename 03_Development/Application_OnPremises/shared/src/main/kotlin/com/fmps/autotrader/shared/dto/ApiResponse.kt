package com.fmps.autotrader.shared.dto

import kotlinx.serialization.Serializable

/**
 * Standard API Response wrapper for successful responses
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean = true,
    val data: T,
    val timestamp: String
)

/**
 * Standard API Error Response
 */
@Serializable
data class ErrorResponse(
    val success: Boolean = false,
    val error: ErrorDetail,
    val timestamp: String
)

/**
 * Detailed error information
 */
@Serializable
data class ErrorDetail(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null
)

/**
 * Paginated list response
 */
@Serializable
data class PaginatedResponse<T>(
    val success: Boolean = true,
    val data: List<T>,
    val pagination: PaginationInfo,
    val timestamp: String
)

/**
 * Pagination metadata
 */
@Serializable
data class PaginationInfo(
    val page: Int,
    val pageSize: Int,
    val totalItems: Long,
    val totalPages: Int
) {
    companion object {
        fun from(page: Int, pageSize: Int, totalItems: Long): PaginationInfo {
            val totalPages = (totalItems / pageSize + if (totalItems % pageSize > 0) 1 else 0).toInt()
            return PaginationInfo(page, pageSize, totalItems, totalPages)
        }
    }
}

/**
 * Simple success response with message
 */
@Serializable
data class MessageResponse(
    val success: Boolean = true,
    val message: String,
    val timestamp: String
)

