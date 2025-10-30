package com.fmps.autotrader.core.indicators

import com.fmps.autotrader.core.indicators.models.MACDResult
import com.fmps.autotrader.shared.model.Candlestick
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * MACD (Moving Average Convergence Divergence) Indicator
 *
 * MACD is a trend-following momentum indicator that shows the relationship
 * between two moving averages. It consists of three components:
 * 1. MACD Line: Difference between fast and slow EMA
 * 2. Signal Line: EMA of the MACD line
 * 3. Histogram: Difference between MACD and Signal line
 *
 * **Formula**:
 * - MACD Line = EMA(fast) - EMA(slow)
 * - Signal Line = EMA(signal) of MACD Line
 * - Histogram = MACD Line - Signal Line
 *
 * **Interpretation**:
 * - MACD > Signal: Bullish momentum
 * - MACD < Signal: Bearish momentum
 * - MACD crosses above Signal: Bullish signal
 * - MACD crosses below Signal: Bearish signal
 * - Histogram > 0: Increasing momentum
 * - Histogram < 0: Decreasing momentum
 *
 * @property fastPeriod Fast EMA period (default: 12)
 * @property slowPeriod Slow EMA period (default: 26)
 * @property signalPeriod Signal line EMA period (default: 9)
 */
class MACDIndicator(
    private val fastPeriod: Int = 12,
    private val slowPeriod: Int = 26,
    private val signalPeriod: Int = 9
) : ITechnicalIndicator<MACDResult> {
    
    init {
        require(fastPeriod > 0) { "Fast period must be positive, got: $fastPeriod" }
        require(slowPeriod > 0) { "Slow period must be positive, got: $slowPeriod" }
        require(signalPeriod > 0) { "Signal period must be positive, got: $signalPeriod" }
        require(fastPeriod < slowPeriod) { "Fast period ($fastPeriod) must be less than slow period ($slowPeriod)" }
    }
    
    private val fastEMA = EMAIndicator(fastPeriod)
    private val slowEMA = EMAIndicator(slowPeriod)
    
    override fun calculate(data: List<Candlestick>): MACDResult? {
        if (!validateData(data)) {
            logger.warn { 
                "Insufficient data for MACD($fastPeriod,$slowPeriod,$signalPeriod): need ${getRequiredDataPoints()}, got ${data.size}" 
            }
            return null
        }
        
        try {
            // Calculate fast and slow EMAs
            val fast = fastEMA.calculate(data) ?: return null
            val slow = slowEMA.calculate(data) ?: return null
            
            // MACD Line = Fast EMA - Slow EMA
            val macdLine = fast - slow
            
            // To calculate signal line, we need MACD values for 'signalPeriod' periods
            // Calculate all MACD values
            val macdValues = calculateMACDLine(data)
            
            // If we don't have enough MACD values for signal line, return MACD with zero signal
            if (macdValues.size < signalPeriod) {
                return MACDResult(
                    macd = macdLine,
                    signal = 0.0,
                    histogram = macdLine
                )
            }
            
            // Calculate signal line (EMA of MACD line)
            val signalLine = calculateSignalLine(macdValues)
            
            // Histogram = MACD - Signal
            val histogram = macdLine - signalLine
            
            return MACDResult(
                macd = macdLine,
                signal = signalLine,
                histogram = histogram
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Error calculating MACD($fastPeriod,$slowPeriod,$signalPeriod)" }
            throw IndicatorException.calculationError("MACD", e)
        }
    }
    
    /**
     * Calculate MACD line for all data points
     *
     * @param data Candlestick data
     * @return List of MACD line values
     */
    private fun calculateMACDLine(data: List<Candlestick>): List<Double> {
        val fastEMAs = fastEMA.calculateAll(data)
        val slowEMAs = slowEMA.calculateAll(data)
        
        return fastEMAs.zip(slowEMAs) { fast, slow ->
            if (fast != null && slow != null) fast - slow else null
        }.filterNotNull()
    }
    
    /**
     * Calculate signal line (EMA of MACD line)
     *
     * @param macdValues MACD line values
     * @return Signal line value
     */
    private fun calculateSignalLine(macdValues: List<Double>): Double {
        if (macdValues.size < signalPeriod) {
            return macdValues.average()
        }
        
        // Calculate EMA of MACD values
        val multiplier = 2.0 / (signalPeriod + 1)
        
        // Initial signal = SMA of first 'signalPeriod' MACD values
        val initial = macdValues.take(signalPeriod).average()
        var signal = initial
        
        // Apply EMA formula for remaining values
        for (i in signalPeriod until macdValues.size) {
            signal = (macdValues[i] - signal) * multiplier + signal
        }
        
        return signal
    }
    
    /**
     * Calculate MACD for all possible windows in the data series
     *
     * @param data List of candlesticks
     * @return List of MACD results (null for periods with insufficient data)
     */
    fun calculateAll(data: List<Candlestick>): List<MACDResult?> {
        val result = mutableListOf<MACDResult?>()
        
        if (data.size < getRequiredDataPoints()) {
            return List(data.size) { null }
        }
        
        // Fill nulls for insufficient data periods
        repeat(slowPeriod - 1) {
            result.add(null)
        }
        
        // Calculate MACD for each window
        for (i in slowPeriod - 1 until data.size) {
            val window = data.subList(0, i + 1)
            result.add(calculate(window))
        }
        
        return result
    }
    
    override fun getName(): String = "MACD"
    
    override fun getRequiredDataPoints(): Int {
        // Need enough data for slow EMA + signal line calculation
        return slowPeriod + signalPeriod - 1
    }
    
    override fun reset() {
        fastEMA.reset()
        slowEMA.reset()
    }
    
    override fun toString(): String = "MACD(fast=$fastPeriod, slow=$slowPeriod, signal=$signalPeriod)"
}

