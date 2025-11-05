package com.fmps.autotrader.core.patterns

import com.fmps.autotrader.core.indicators.models.BollingerBandsResult
import com.fmps.autotrader.core.indicators.models.MACDResult
import com.fmps.autotrader.core.patterns.models.MarketConditions
import com.fmps.autotrader.core.patterns.models.TradingPattern
import org.slf4j.LoggerFactory
import kotlin.math.abs
import kotlin.math.min

/**
 * Calculates relevance scores for pattern matching.
 * 
 * Relevance score indicates how well current market conditions match
 * a stored pattern. Score ranges from 0.0 (no match) to 1.0 (perfect match).
 * 
 * Components of relevance:
 * - Indicator similarity (RSI, MACD, SMA, EMA, Bollinger Bands)
 * - Price level similarity
 * - Market trend similarity
 * - Pattern performance (success rate)
 * - Pattern recency (recently used patterns score higher)
 */
class RelevanceCalculator {
    private val logger = LoggerFactory.getLogger(RelevanceCalculator::class.java)
    
    // Default weights for relevance components
    private val defaultWeights = mapOf(
        "indicators" to 0.4,
        "performance" to 0.3,
        "recency" to 0.2,
        "price" to 0.1
    )
    
    /**
     * Calculate relevance score for a pattern against current market conditions.
     * 
     * @param pattern The trading pattern to match
     * @param conditions Current market conditions
     * @param weights Optional custom weights for relevance components
     * @return Relevance score (0.0-1.0)
     */
    fun calculateRelevance(
        pattern: TradingPattern,
        conditions: MarketConditions,
        weights: Map<String, Double> = defaultWeights
    ): Double {
        // Check basic compatibility first
        if (!isCompatible(pattern, conditions)) {
            return 0.0
        }
        
        val componentScores = mutableMapOf<String, Double>()
        
        // Calculate indicator similarity
        componentScores["indicators"] = calculateIndicatorSimilarity(pattern, conditions)
        
        // Calculate performance score
        componentScores["performance"] = calculatePerformanceScore(pattern)
        
        // Calculate recency score
        componentScores["recency"] = calculateRecencyScore(pattern)
        
        // Calculate price similarity
        componentScores["price"] = calculatePriceSimilarity(pattern, conditions)
        
        // Combine scores with weights
        val relevance = calculateOverallRelevance(componentScores, weights)
        
        logger.debug(
            "Pattern ${pattern.id} relevance: $relevance (indicators=${componentScores["indicators"]}, " +
            "performance=${componentScores["performance"]}, recency=${componentScores["recency"]}, " +
            "price=${componentScores["price"]})"
        )
        
        return relevance.coerceIn(0.0, 1.0)
    }
    
    /**
     * Check if pattern is compatible with market conditions (same exchange, symbol, timeframe)
     */
    private fun isCompatible(pattern: TradingPattern, conditions: MarketConditions): Boolean {
        return pattern.exchange == conditions.exchange &&
               pattern.symbol == conditions.symbol
               // Note: timeframe matching is optional - can match across timeframes
    }
    
    /**
     * Calculate similarity based on technical indicators.
     * 
     * Compares RSI, MACD, SMA, EMA, and Bollinger Bands values.
     */
    private fun calculateIndicatorSimilarity(
        pattern: TradingPattern,
        conditions: MarketConditions
    ): Double {
        var totalScore = 0.0
        var count = 0
        
        // RSI comparison
        val patternRSI = pattern.conditions["RSI"] as? Double
        val currentRSI = conditions.getRSI()
        if (patternRSI != null && currentRSI != null) {
            val rsiRange = pattern.conditions["RSI_Range"] as? Pair<Double, Double>
            if (rsiRange != null) {
                // Pattern has RSI range
                val score = if (currentRSI in rsiRange.first..rsiRange.second) {
                    1.0
                } else {
                    // Calculate distance from range
                    val distance = when {
                        currentRSI < rsiRange.first -> rsiRange.first - currentRSI
                        currentRSI > rsiRange.second -> currentRSI - rsiRange.second
                        else -> 0.0
                    }
                    // Score decreases with distance (max 20 RSI points = 0 score)
                    (1.0 - (distance / 20.0)).coerceIn(0.0, 1.0)
                }
                totalScore += score
                count++
            } else {
                // Pattern has single RSI value
                val score = calculateValueSimilarity(patternRSI, currentRSI, tolerance = 5.0, maxRange = 100.0)
                totalScore += score
                count++
            }
        }
        
        // MACD comparison
        val patternMACD = pattern.conditions["MACD"] as? MACDResult
        val currentMACD = conditions.getMACD()
        if (patternMACD != null && currentMACD != null) {
            val macdScore = calculateMACDSimilarity(patternMACD, currentMACD)
            totalScore += macdScore
            count++
        }
        
        // SMA comparison
        val patternSMA = pattern.conditions["SMA"] as? Double
        val currentSMA = conditions.getSMA()
        if (patternSMA != null && currentSMA != null) {
            val price = conditions.currentPrice.toDouble()
            val score = calculateValueSimilarity(patternSMA, currentSMA, tolerance = price * 0.02, maxRange = price * 0.1)
            totalScore += score
            count++
        }
        
        // EMA comparison
        val patternEMA = pattern.conditions["EMA"] as? Double
        val currentEMA = conditions.getEMA()
        if (patternEMA != null && currentEMA != null) {
            val price = conditions.currentPrice.toDouble()
            val score = calculateValueSimilarity(patternEMA, currentEMA, tolerance = price * 0.02, maxRange = price * 0.1)
            totalScore += score
            count++
        }
        
        // Bollinger Bands comparison
        val patternBB = pattern.conditions["BollingerBands"] as? BollingerBandsResult
        val currentBB = conditions.getBollingerBands()
        if (patternBB != null && currentBB != null) {
            val bbScore = calculateBollingerBandsSimilarity(patternBB, currentBB, conditions.currentPrice.toDouble())
            totalScore += bbScore
            count++
        }
        
        return if (count > 0) totalScore / count else 0.5 // Default to neutral if no indicators
    }
    
    /**
     * Calculate similarity between two numeric values.
     * 
     * @param patternValue Pattern's expected value
     * @param currentValue Current market value
     * @param tolerance Acceptable difference (within tolerance = 1.0 score)
     * @param maxRange Maximum range for comparison (beyond this = 0.0 score)
     */
    private fun calculateValueSimilarity(
        patternValue: Double,
        currentValue: Double,
        tolerance: Double,
        maxRange: Double
    ): Double {
        val difference = abs(patternValue - currentValue)
        
        if (difference <= tolerance) {
            return 1.0
        } else if (difference >= maxRange) {
            return 0.0
        } else {
            // Linear interpolation between tolerance and maxRange
            return 1.0 - ((difference - tolerance) / (maxRange - tolerance))
        }
    }
    
    /**
     * Calculate MACD similarity.
     */
    private fun calculateMACDSimilarity(pattern: MACDResult, current: MACDResult): Double {
        // Compare MACD line, signal line, and histogram
        val macdScore = calculateValueSimilarity(pattern.macd, current.macd, tolerance = 0.001, maxRange = 0.01)
        val signalScore = calculateValueSimilarity(pattern.signal, current.signal, tolerance = 0.001, maxRange = 0.01)
        val histogramScore = calculateValueSimilarity(pattern.histogram, current.histogram, tolerance = 0.001, maxRange = 0.01)
        
        // Also check trend alignment
        val trendScore = if (pattern.isBullish() == current.isBullish() &&
                             pattern.hasPositiveMomentum() == current.hasPositiveMomentum()) {
            1.0
        } else {
            0.5
        }
        
        return (macdScore * 0.3 + signalScore * 0.3 + histogramScore * 0.2 + trendScore * 0.2)
    }
    
    /**
     * Calculate Bollinger Bands similarity.
     */
    private fun calculateBollingerBandsSimilarity(
        pattern: BollingerBandsResult,
        current: BollingerBandsResult,
        currentPrice: Double
    ): Double {
        // Compare band positions
        val upperScore = calculateValueSimilarity(pattern.upper, current.upper, tolerance = currentPrice * 0.01, maxRange = currentPrice * 0.05)
        val lowerScore = calculateValueSimilarity(pattern.lower, current.lower, tolerance = currentPrice * 0.01, maxRange = currentPrice * 0.05)
        val middleScore = calculateValueSimilarity(pattern.middle, current.middle, tolerance = currentPrice * 0.01, maxRange = currentPrice * 0.05)
        
        // Compare %B position
        val percentBScore = if (pattern.percentB != null && current.percentB != null) {
            calculateValueSimilarity(pattern.percentB, current.percentB, tolerance = 0.1, maxRange = 0.5)
        } else {
            0.5
        }
        
        return (upperScore * 0.25 + lowerScore * 0.25 + middleScore * 0.25 + percentBScore * 0.25)
    }
    
    /**
     * Calculate performance score based on pattern's success rate and usage.
     */
    private fun calculatePerformanceScore(pattern: TradingPattern): Double {
        // Higher success rate = higher score
        val successScore = pattern.successRate
        
        // More usage = more reliable (but cap at 10 uses)
        val usageScore = min(pattern.usageCount.toDouble() / 10.0, 1.0)
        
        // Combine: 70% success rate, 30% usage
        return (successScore * 0.7 + usageScore * 0.3)
    }
    
    /**
     * Calculate recency score based on when pattern was last used.
     * 
     * Recently used patterns score higher.
     */
    private fun calculateRecencyScore(pattern: TradingPattern): Double {
        val lastUsed = pattern.lastUsedAt
        if (lastUsed == null) {
            return 0.5 // Neutral if never used
        }
        
        val daysSinceLastUse = java.time.Duration.between(lastUsed, java.time.Instant.now()).toDays()
        
        // Score decreases over time
        // 0 days = 1.0, 7 days = 0.7, 30 days = 0.3, 90+ days = 0.1
        return when {
            daysSinceLastUse <= 1 -> 1.0
            daysSinceLastUse <= 7 -> 1.0 - (daysSinceLastUse - 1) * 0.05
            daysSinceLastUse <= 30 -> 0.7 - (daysSinceLastUse - 7) * 0.017
            daysSinceLastUse <= 90 -> 0.3 - (daysSinceLastUse - 30) * 0.003
            else -> 0.1
        }.coerceIn(0.0, 1.0)
    }
    
    /**
     * Calculate price similarity.
     * 
     * Note: Price similarity is less important than indicators since prices change constantly.
     */
    private fun calculatePriceSimilarity(
        pattern: TradingPattern,
        conditions: MarketConditions
    ): Double {
        val patternPrice = pattern.conditions["entryPrice"] as? Double
        if (patternPrice == null) return 0.5 // Neutral if no price data
        
        val currentPrice = conditions.currentPrice.toDouble()
        val priceRange = pattern.conditions["priceRange"] as? Pair<Double, Double>
        
        if (priceRange != null) {
            // Pattern has price range
            return if (currentPrice in priceRange.first..priceRange.second) {
                1.0
            } else {
                // Calculate distance from range
                val distance = when {
                    currentPrice < priceRange.first -> priceRange.first - currentPrice
                    currentPrice > priceRange.second -> currentPrice - priceRange.second
                    else -> 0.0
                }
                val rangeSize = priceRange.second - priceRange.first
                (1.0 - (distance / (rangeSize * 2))).coerceIn(0.0, 1.0)
            }
        } else {
            // Pattern has single price
            val tolerance = currentPrice * 0.02 // 2% tolerance
            val maxRange = currentPrice * 0.1 // 10% max range
            return calculateValueSimilarity(patternPrice, currentPrice, tolerance, maxRange)
        }
    }
    
    /**
     * Combine component scores into overall relevance score.
     */
    private fun calculateOverallRelevance(
        componentScores: Map<String, Double>,
        weights: Map<String, Double>
    ): Double {
        var totalScore = 0.0
        var totalWeight = 0.0
        
        for ((component, weight) in weights) {
            val score = componentScores[component] ?: 0.5 // Default to neutral if missing
            totalScore += score * weight
            totalWeight += weight
        }
        
        return if (totalWeight > 0) totalScore / totalWeight else 0.5
    }
    
    /**
     * Calculate similarity for a specific indicator value.
     * 
     * Used by pattern matching to compare individual indicators.
     */
    fun calculateIndicatorSimilarity(
        patternValue: Any,
        currentValue: Any,
        tolerance: Double = 0.05 // 5% default tolerance
    ): Double {
        return when {
            patternValue is Double && currentValue is Double -> {
                val maxValue = maxOf(abs(patternValue), abs(currentValue), 1.0)
                calculateValueSimilarity(patternValue, currentValue, tolerance * maxValue, maxValue * 0.2)
            }
            patternValue is MACDResult && currentValue is MACDResult -> {
                calculateMACDSimilarity(patternValue, currentValue)
            }
            patternValue is BollingerBandsResult && currentValue is BollingerBandsResult -> {
                val price = maxOf(abs(patternValue.middle), abs(currentValue.middle), 1.0)
                calculateBollingerBandsSimilarity(patternValue, currentValue, price)
            }
            else -> {
                // Try to compare as numbers
                try {
                    val patternNum = (patternValue as? Number)?.toDouble() ?: return 0.0
                    val currentNum = (currentValue as? Number)?.toDouble() ?: return 0.0
                    val maxValue = maxOf(abs(patternNum), abs(currentNum), 1.0)
                    calculateValueSimilarity(patternNum, currentNum, tolerance * maxValue, maxValue * 0.2)
                } catch (e: Exception) {
                    logger.warn("Cannot calculate similarity for ${patternValue::class.simpleName} and ${currentValue::class.simpleName}")
                    0.0
                }
            }
        }
    }
}

