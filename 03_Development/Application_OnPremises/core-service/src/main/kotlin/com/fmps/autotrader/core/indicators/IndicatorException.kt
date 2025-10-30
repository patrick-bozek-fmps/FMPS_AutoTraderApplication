package com.fmps.autotrader.core.indicators

/**
 * Exception thrown when indicator calculation fails
 *
 * @property message Error message describing the failure
 * @property cause Original exception that caused the failure (if any)
 */
class IndicatorException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    
    companion object {
        /**
         * Create exception for insufficient data
         */
        fun insufficientData(indicator: String, required: Int, actual: Int): IndicatorException {
            return IndicatorException(
                "$indicator requires at least $required data points, but only $actual provided"
            )
        }
        
        /**
         * Create exception for invalid parameter
         */
        fun invalidParameter(parameter: String, value: Any?, reason: String): IndicatorException {
            return IndicatorException(
                "Invalid parameter '$parameter' = '$value': $reason"
            )
        }
        
        /**
         * Create exception for invalid data
         */
        fun invalidData(reason: String): IndicatorException {
            return IndicatorException("Invalid data: $reason")
        }
        
        /**
         * Create exception for calculation error
         */
        fun calculationError(indicator: String, cause: Throwable): IndicatorException {
            return IndicatorException(
                "Failed to calculate $indicator", 
                cause
            )
        }
    }
}

