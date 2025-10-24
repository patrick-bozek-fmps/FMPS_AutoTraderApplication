package com.fmps.autotrader.core.database.repositories

import com.fmps.autotrader.core.database.DatabaseFactory.dbQuery
import com.fmps.autotrader.core.database.schema.PatternsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Repository for Pattern operations
 * 
 * Provides CRUD operations for managing trading patterns (simple pattern storage for v1.0)
 */
class PatternRepository {
    private val logger = LoggerFactory.getLogger(PatternRepository::class.java)
    
    /**
     * Create a new pattern
     */
    suspend fun create(
        name: String?,
        patternType: String,
        tradingPair: String,
        timeframe: String,
        tradeType: String,
        rsiMin: BigDecimal? = null,
        rsiMax: BigDecimal? = null,
        macdMin: BigDecimal? = null,
        macdMax: BigDecimal? = null,
        volumeChangeMin: BigDecimal? = null,
        volumeChangeMax: BigDecimal? = null,
        priceChangeMin: BigDecimal? = null,
        priceChangeMax: BigDecimal? = null,
        description: String? = null,
        tags: String? = null
    ): Int = dbQuery {
        PatternsTable.insert {
            it[PatternsTable.name] = name
            it[PatternsTable.patternType] = patternType
            it[PatternsTable.tradingPair] = tradingPair
            it[PatternsTable.timeframe] = timeframe
            it[PatternsTable.tradeType] = tradeType
            it[PatternsTable.rsiMin] = rsiMin
            it[PatternsTable.rsiMax] = rsiMax
            it[PatternsTable.macdMin] = macdMin
            it[PatternsTable.macdMax] = macdMax
            it[PatternsTable.volumeChangeMin] = volumeChangeMin
            it[PatternsTable.volumeChangeMax] = volumeChangeMax
            it[PatternsTable.priceChangeMin] = priceChangeMin
            it[PatternsTable.priceChangeMax] = priceChangeMax
            it[PatternsTable.description] = description
            it[PatternsTable.tags] = tags
        }[PatternsTable.id]
    }
    
    /**
     * Update pattern statistics after a trade
     */
    suspend fun updateStatistics(
        patternId: Int,
        profitLoss: BigDecimal,
        isSuccessful: Boolean
    ): Boolean = dbQuery {
        // Get current statistics
        val pattern = PatternsTable.selectAll()
            .where { PatternsTable.id eq patternId }
            .singleOrNull()
            ?: return@dbQuery false
        
        val totalOccurrences = pattern[PatternsTable.totalOccurrences] + 1
        val successfulTrades = if (isSuccessful) {
            pattern[PatternsTable.successfulTrades] + 1
        } else {
            pattern[PatternsTable.successfulTrades]
        }
        val failedTrades = if (!isSuccessful) {
            pattern[PatternsTable.failedTrades] + 1
        } else {
            pattern[PatternsTable.failedTrades]
        }
        
        val successRate = (successfulTrades.toBigDecimal() / totalOccurrences.toBigDecimal()) * BigDecimal("100")
        
        val currentAveragePL = pattern[PatternsTable.averageProfitLoss]
        val newAveragePL = ((currentAveragePL * (totalOccurrences - 1).toBigDecimal()) + profitLoss) / 
                           totalOccurrences.toBigDecimal()
        
        val bestPL = maxOf(pattern[PatternsTable.bestProfitLoss], profitLoss)
        val worstPL = minOf(pattern[PatternsTable.worstProfitLoss], profitLoss)
        
        // Calculate confidence (based on occurrences and success rate)
        val minOccurrences = pattern[PatternsTable.minOccurrencesForConfidence]
        val confidence = if (totalOccurrences >= minOccurrences) {
            successRate
        } else {
            successRate * (totalOccurrences.toBigDecimal() / minOccurrences.toBigDecimal())
        }
        
        val updated = PatternsTable.update({ PatternsTable.id eq patternId }) {
            it[PatternsTable.totalOccurrences] = totalOccurrences
            it[PatternsTable.successfulTrades] = successfulTrades
            it[PatternsTable.failedTrades] = failedTrades
            it[PatternsTable.successRate] = successRate
            it[PatternsTable.averageProfitLoss] = newAveragePL
            it[PatternsTable.bestProfitLoss] = bestPL
            it[PatternsTable.worstProfitLoss] = worstPL
            it[PatternsTable.confidence] = confidence
            it[lastMatchedAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
        }
        
        updated > 0
    }
    
    /**
     * Find pattern by ID
     */
    suspend fun findById(id: Int): Pattern? = dbQuery {
        PatternsTable.selectAll()
            .where { PatternsTable.id eq id }
            .map { rowToPattern(it) }
            .singleOrNull()
    }
    
    /**
     * Find all active patterns
     */
    suspend fun findActive(): List<Pattern> = dbQuery {
        PatternsTable.selectAll()
            .where { PatternsTable.isActive eq true }
            .orderBy(PatternsTable.confidence to SortOrder.DESC)
            .map { rowToPattern(it) }
    }
    
    /**
     * Find patterns by trading pair
     */
    suspend fun findByTradingPair(tradingPair: String): List<Pattern> = dbQuery {
        PatternsTable.selectAll()
            .where { 
                (PatternsTable.tradingPair eq tradingPair) and (PatternsTable.isActive eq true)
            }
            .orderBy(PatternsTable.confidence to SortOrder.DESC)
            .map { rowToPattern(it) }
    }
    
    /**
     * Find matching patterns for current market conditions
     */
    suspend fun findMatchingPatterns(
        tradingPair: String,
        timeframe: String,
        tradeType: String,
        rsiValue: BigDecimal?,
        macdValue: BigDecimal?,
        minConfidence: BigDecimal = BigDecimal("60")
    ): List<Pattern> = dbQuery {
        val query = PatternsTable.selectAll()
            .where {
                (PatternsTable.tradingPair eq tradingPair) and
                (PatternsTable.timeframe eq timeframe) and
                (PatternsTable.tradeType eq tradeType) and
                (PatternsTable.isActive eq true) and
                (PatternsTable.confidence greaterEq minConfidence)
            }
        
        // Filter by RSI range if provided
        if (rsiValue != null) {
            query.andWhere {
                ((PatternsTable.rsiMin.isNull()) or (PatternsTable.rsiMin lessEq rsiValue)) and
                ((PatternsTable.rsiMax.isNull()) or (PatternsTable.rsiMax greaterEq rsiValue))
            }
        }
        
        // Filter by MACD range if provided
        if (macdValue != null) {
            query.andWhere {
                ((PatternsTable.macdMin.isNull()) or (PatternsTable.macdMin lessEq macdValue)) and
                ((PatternsTable.macdMax.isNull()) or (PatternsTable.macdMax greaterEq macdValue))
            }
        }
        
        query.orderBy(PatternsTable.confidence to SortOrder.DESC)
            .map { rowToPattern(it) }
    }
    
    /**
     * Get top performing patterns
     */
    suspend fun getTopPatterns(limit: Int = 10, minOccurrences: Int = 5): List<Pattern> = dbQuery {
        PatternsTable.selectAll()
            .where {
                (PatternsTable.isActive eq true) and
                (PatternsTable.totalOccurrences greaterEq minOccurrences)
            }
            .orderBy(PatternsTable.successRate to SortOrder.DESC, PatternsTable.averageProfitLoss to SortOrder.DESC)
            .limit(limit)
            .map { rowToPattern(it) }
    }
    
    /**
     * Deactivate a pattern
     */
    suspend fun deactivate(id: Int): Boolean = dbQuery {
        val updated = PatternsTable.update({ PatternsTable.id eq id }) {
            it[isActive] = false
            it[updatedAt] = LocalDateTime.now()
        }
        updated > 0
    }
    
    /**
     * Activate a pattern
     */
    suspend fun activate(id: Int): Boolean = dbQuery {
        val updated = PatternsTable.update({ PatternsTable.id eq id }) {
            it[isActive] = true
            it[updatedAt] = LocalDateTime.now()
        }
        updated > 0
    }
    
    /**
     * Delete a pattern
     */
    suspend fun delete(id: Int): Boolean = dbQuery {
        val deleted = PatternsTable.deleteWhere { PatternsTable.id eq id }
        deleted > 0
    }
    
    /**
     * Convert database row to Pattern data class
     */
    private fun rowToPattern(row: ResultRow): Pattern =
        Pattern(
            id = row[PatternsTable.id],
            name = row[PatternsTable.name],
            patternType = row[PatternsTable.patternType],
            tradingPair = row[PatternsTable.tradingPair],
            timeframe = row[PatternsTable.timeframe],
            tradeType = row[PatternsTable.tradeType],
            rsiMin = row[PatternsTable.rsiMin],
            rsiMax = row[PatternsTable.rsiMax],
            macdMin = row[PatternsTable.macdMin],
            macdMax = row[PatternsTable.macdMax],
            volumeChangeMin = row[PatternsTable.volumeChangeMin],
            volumeChangeMax = row[PatternsTable.volumeChangeMax],
            priceChangeMin = row[PatternsTable.priceChangeMin],
            priceChangeMax = row[PatternsTable.priceChangeMax],
            totalOccurrences = row[PatternsTable.totalOccurrences],
            successfulTrades = row[PatternsTable.successfulTrades],
            failedTrades = row[PatternsTable.failedTrades],
            successRate = row[PatternsTable.successRate],
            averageProfitLoss = row[PatternsTable.averageProfitLoss],
            bestProfitLoss = row[PatternsTable.bestProfitLoss],
            worstProfitLoss = row[PatternsTable.worstProfitLoss],
            confidence = row[PatternsTable.confidence],
            minOccurrencesForConfidence = row[PatternsTable.minOccurrencesForConfidence],
            isActive = row[PatternsTable.isActive],
            description = row[PatternsTable.description],
            tags = row[PatternsTable.tags],
            createdAt = row[PatternsTable.createdAt],
            updatedAt = row[PatternsTable.updatedAt],
            lastMatchedAt = row[PatternsTable.lastMatchedAt]
        )
}

/**
 * Data class representing a Pattern
 */
data class Pattern(
    val id: Int,
    val name: String?,
    val patternType: String,
    val tradingPair: String,
    val timeframe: String,
    val tradeType: String,
    val rsiMin: BigDecimal?,
    val rsiMax: BigDecimal?,
    val macdMin: BigDecimal?,
    val macdMax: BigDecimal?,
    val volumeChangeMin: BigDecimal?,
    val volumeChangeMax: BigDecimal?,
    val priceChangeMin: BigDecimal?,
    val priceChangeMax: BigDecimal?,
    val totalOccurrences: Int,
    val successfulTrades: Int,
    val failedTrades: Int,
    val successRate: BigDecimal,
    val averageProfitLoss: BigDecimal,
    val bestProfitLoss: BigDecimal,
    val worstProfitLoss: BigDecimal,
    val confidence: BigDecimal,
    val minOccurrencesForConfidence: Int,
    val isActive: Boolean,
    val description: String?,
    val tags: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val lastMatchedAt: LocalDateTime?
)

