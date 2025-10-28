package com.fmps.autotrader.core.config

/**
 * Exception thrown when configuration loading, parsing, or validation fails.
 */
class ConfigurationException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

