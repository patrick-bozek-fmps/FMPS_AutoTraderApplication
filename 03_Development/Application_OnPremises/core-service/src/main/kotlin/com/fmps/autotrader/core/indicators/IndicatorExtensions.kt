package com.fmps.autotrader.core.indicators

import com.fmps.autotrader.core.indicators.models.BollingerBandsResult
import com.fmps.autotrader.core.indicators.models.MACDResult
import com.fmps.autotrader.shared.model.Candlestick

/**
 * Extension functions for easy indicator calculation on candlestick data
 *
 * These functions provide a convenient way to calculate indicators
 * without directly instantiating indicator classes.
 */

// ============================================
// Simple Moving Average (SMA)
// ============================================

/**
 * Calculate Simple Moving Average for the most recent period
 *
 * @param period Number of periods (default: 20)
 * @return SMA value, or null if insufficient data
 */
fun List<Candlestick>.sma(period: Int = 20): Double? {
    return SMAIndicator(period).calculate(this)
}

/**
 * Calculate SMA for all windows in the series
 *
 * @param period Number of periods (default: 20)
 * @return List of SMA values (null where insufficient data)
 */
fun List<Candlestick>.smaAll(period: Int = 20): List<Double?> {
    return SMAIndicator(period).calculateAll(this)
}

// ============================================
// Exponential Moving Average (EMA)
// ============================================

/**
 * Calculate Exponential Moving Average for the most recent period
 *
 * @param period Number of periods (default: 12)
 * @return EMA value, or null if insufficient data
 */
fun List<Candlestick>.ema(period: Int = 12): Double? {
    return EMAIndicator(period).calculate(this)
}

/**
 * Calculate EMA for all windows in the series
 *
 * @param period Number of periods (default: 12)
 * @return List of EMA values (null where insufficient data)
 */
fun List<Candlestick>.emaAll(period: Int = 12): List<Double?> {
    return EMAIndicator(period).calculateAll(this)
}

// ============================================
// Relative Strength Index (RSI)
// ============================================

/**
 * Calculate Relative Strength Index
 *
 * @param period Number of periods (default: 14)
 * @return RSI value (0-100), or null if insufficient data
 */
fun List<Candlestick>.rsi(period: Int = 14): Double? {
    return RSIIndicator(period).calculate(this)
}

/**
 * Calculate RSI for all windows in the series
 *
 * @param period Number of periods (default: 14)
 * @return List of RSI values (null where insufficient data)
 */
fun List<Candlestick>.rsiAll(period: Int = 14): List<Double?> {
    return RSIIndicator(period).calculateAll(this)
}

/**
 * Check if current RSI indicates overbought condition
 *
 * @param period RSI period (default: 14)
 * @param threshold Overbought threshold (default: 70.0)
 * @return true if overbought
 */
fun List<Candlestick>.isRSIOverbought(period: Int = 14, threshold: Double = 70.0): Boolean {
    val rsiValue = this.rsi(period) ?: return false
    return RSIIndicator.isOverbought(rsiValue, threshold)
}

/**
 * Check if current RSI indicates oversold condition
 *
 * @param period RSI period (default: 14)
 * @param threshold Oversold threshold (default: 30.0)
 * @return true if oversold
 */
fun List<Candlestick>.isRSIOversold(period: Int = 14, threshold: Double = 30.0): Boolean {
    val rsiValue = this.rsi(period) ?: return false
    return RSIIndicator.isOversold(rsiValue, threshold)
}

// ============================================
// MACD (Moving Average Convergence Divergence)
// ============================================

/**
 * Calculate MACD
 *
 * @param fastPeriod Fast EMA period (default: 12)
 * @param slowPeriod Slow EMA period (default: 26)
 * @param signalPeriod Signal line period (default: 9)
 * @return MACD result, or null if insufficient data
 */
fun List<Candlestick>.macd(
    fastPeriod: Int = 12,
    slowPeriod: Int = 26,
    signalPeriod: Int = 9
): MACDResult? {
    return MACDIndicator(fastPeriod, slowPeriod, signalPeriod).calculate(this)
}

/**
 * Calculate MACD for all windows in the series
 *
 * @param fastPeriod Fast EMA period (default: 12)
 * @param slowPeriod Slow EMA period (default: 26)
 * @param signalPeriod Signal line period (default: 9)
 * @return List of MACD results (null where insufficient data)
 */
fun List<Candlestick>.macdAll(
    fastPeriod: Int = 12,
    slowPeriod: Int = 26,
    signalPeriod: Int = 9
): List<MACDResult?> {
    return MACDIndicator(fastPeriod, slowPeriod, signalPeriod).calculateAll(this)
}

/**
 * Check if current MACD indicates bullish momentum (MACD > Signal)
 *
 * @param fastPeriod Fast EMA period (default: 12)
 * @param slowPeriod Slow EMA period (default: 26)
 * @param signalPeriod Signal line period (default: 9)
 * @return true if bullish
 */
fun List<Candlestick>.isMACDBullish(
    fastPeriod: Int = 12,
    slowPeriod: Int = 26,
    signalPeriod: Int = 9
): Boolean {
    val macdResult = this.macd(fastPeriod, slowPeriod, signalPeriod) ?: return false
    return macdResult.isBullish()
}

/**
 * Check if current MACD indicates bearish momentum (MACD < Signal)
 *
 * @param fastPeriod Fast EMA period (default: 12)
 * @param slowPeriod Slow EMA period (default: 26)
 * @param signalPeriod Signal line period (default: 9)
 * @return true if bearish
 */
fun List<Candlestick>.isMACDBearish(
    fastPeriod: Int = 12,
    slowPeriod: Int = 26,
    signalPeriod: Int = 9
): Boolean {
    val macdResult = this.macd(fastPeriod, slowPeriod, signalPeriod) ?: return false
    return macdResult.isBearish()
}

// ============================================
// Bollinger Bands
// ============================================

/**
 * Calculate Bollinger Bands
 *
 * @param period Number of periods (default: 20)
 * @param stdDevMultiplier Standard deviation multiplier (default: 2.0)
 * @return Bollinger Bands result, or null if insufficient data
 */
fun List<Candlestick>.bollingerBands(
    period: Int = 20,
    stdDevMultiplier: Double = 2.0
): BollingerBandsResult? {
    return BollingerBandsIndicator(period, stdDevMultiplier).calculate(this)
}

/**
 * Calculate Bollinger Bands for all windows in the series
 *
 * @param period Number of periods (default: 20)
 * @param stdDevMultiplier Standard deviation multiplier (default: 2.0)
 * @return List of Bollinger Bands results (null where insufficient data)
 */
fun List<Candlestick>.bollingerBandsAll(
    period: Int = 20,
    stdDevMultiplier: Double = 2.0
): List<BollingerBandsResult?> {
    return BollingerBandsIndicator(period, stdDevMultiplier).calculateAll(this)
}

/**
 * Check if Bollinger Bands are squeezing (low volatility)
 *
 * @param period Number of periods (default: 20)
 * @param stdDevMultiplier Standard deviation multiplier (default: 2.0)
 * @param threshold Bandwidth threshold (default: 0.05 = 5%)
 * @return true if squeezing
 */
fun List<Candlestick>.isBBSqueeze(
    period: Int = 20,
    stdDevMultiplier: Double = 2.0,
    threshold: Double = 0.05
): Boolean {
    val bb = this.bollingerBands(period, stdDevMultiplier) ?: return false
    return bb.isSqueeze(threshold)
}

/**
 * Check if price is touching the upper Bollinger Band
 *
 * @param period Number of periods (default: 20)
 * @param stdDevMultiplier Standard deviation multiplier (default: 2.0)
 * @param tolerance Percentage tolerance (default: 0.01 = 1%)
 * @return true if touching upper band
 */
fun List<Candlestick>.isTouchingUpperBB(
    period: Int = 20,
    stdDevMultiplier: Double = 2.0,
    tolerance: Double = 0.01
): Boolean {
    val bb = this.bollingerBands(period, stdDevMultiplier) ?: return false
    val currentPrice = this.last().close.toDouble()
    return bb.isTouchingUpperBand(currentPrice, tolerance)
}

/**
 * Check if price is touching the lower Bollinger Band
 *
 * @param period Number of periods (default: 20)
 * @param stdDevMultiplier Standard deviation multiplier (default: 2.0)
 * @param tolerance Percentage tolerance (default: 0.01 = 1%)
 * @return true if touching lower band
 */
fun List<Candlestick>.isTouchingLowerBB(
    period: Int = 20,
    stdDevMultiplier: Double = 2.0,
    tolerance: Double = 0.01
): Boolean {
    val bb = this.bollingerBands(period, stdDevMultiplier) ?: return false
    val currentPrice = this.last().close.toDouble()
    return bb.isTouchingLowerBand(currentPrice, tolerance)
}

