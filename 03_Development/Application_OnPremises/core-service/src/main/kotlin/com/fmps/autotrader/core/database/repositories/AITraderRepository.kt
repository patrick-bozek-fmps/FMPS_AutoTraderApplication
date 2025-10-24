package com.fmps.autotrader.core.database.repositories

import com.fmps.autotrader.core.database.DatabaseFactory.dbQuery
import com.fmps.autotrader.core.database.schema.AITradersTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Repository for AI Trader operations
 * 
 * Provides CRUD operations for managing up to 3 AI trader instances
 */
class AITraderRepository {
    private val logger = LoggerFactory.getLogger(AITraderRepository::class.java)
    
    /**
     * Create a new AI trader
     * 
     * @return The ID of the created trader, or null if max limit reached
     */
    suspend fun create(
        name: String,
        exchange: String,
        tradingPair: String,
        leverage: Int = 10,
        initialBalance: BigDecimal,
        stopLossPercent: BigDecimal = BigDecimal("0.02"),
        takeProfitPercent: BigDecimal = BigDecimal("0.05")
    ): Int? = dbQuery {
        // Check if we've reached the max limit (3 traders)
        val activeCount = AITradersTable.selectAll().count()
        if (activeCount >= 3) {
            logger.warn("Cannot create AI trader: maximum limit (3) reached")
            return@dbQuery null
        }
        
        AITradersTable.insert {
            it[AITradersTable.name] = name
            it[AITradersTable.status] = "STOPPED"
            it[AITradersTable.exchange] = exchange
            it[AITradersTable.tradingPair] = tradingPair
            it[AITradersTable.leverage] = leverage
            it[AITradersTable.initialBalance] = initialBalance
            it[AITradersTable.currentBalance] = initialBalance
            it[AITradersTable.stopLossPercent] = stopLossPercent
            it[AITradersTable.takeProfitPercent] = takeProfitPercent
        }[AITradersTable.id]
    }
    
    /**
     * Get an AI trader by ID
     */
    suspend fun findById(id: Int): AITrader? = dbQuery {
        AITradersTable.selectAll()
            .where { AITradersTable.id eq id }
            .map { rowToAITrader(it) }
            .singleOrNull()
    }
    
    /**
     * Get all AI traders
     */
    suspend fun findAll(): List<AITrader> = dbQuery {
        AITradersTable.selectAll()
            .map { rowToAITrader(it) }
    }
    
    /**
     * Get active AI traders
     */
    suspend fun findActive(): List<AITrader> = dbQuery {
        AITradersTable.selectAll()
            .where { AITradersTable.status eq "ACTIVE" }
            .map { rowToAITrader(it) }
    }
    
    /**
     * Update AI trader status
     */
    suspend fun updateStatus(id: Int, status: String): Boolean = dbQuery {
        val updated = AITradersTable.update({ AITradersTable.id eq id }) {
            it[AITradersTable.status] = status
            it[updatedAt] = LocalDateTime.now()
            if (status == "ACTIVE") {
                it[lastActiveAt] = LocalDateTime.now()
            }
        }
        updated > 0
    }
    
    /**
     * Update AI trader balance
     */
    suspend fun updateBalance(id: Int, newBalance: BigDecimal): Boolean = dbQuery {
        val updated = AITradersTable.update({ AITradersTable.id eq id }) {
            it[currentBalance] = newBalance
            it[updatedAt] = LocalDateTime.now()
        }
        updated > 0
    }
    
    /**
     * Update AI trader configuration
     */
    suspend fun updateConfiguration(
        id: Int,
        leverage: Int? = null,
        stopLossPercent: BigDecimal? = null,
        takeProfitPercent: BigDecimal? = null,
        maxDailyLoss: BigDecimal? = null,
        rsiPeriod: Int? = null,
        rsiOverbought: Int? = null,
        rsiOversold: Int? = null,
        macdFast: Int? = null,
        macdSlow: Int? = null,
        macdSignal: Int? = null,
        smaShort: Int? = null,
        smaLong: Int? = null
    ): Boolean = dbQuery {
        val updated = AITradersTable.update({ AITradersTable.id eq id }) {
            leverage?.let { value -> it[AITradersTable.leverage] = value }
            stopLossPercent?.let { value -> it[AITradersTable.stopLossPercent] = value }
            takeProfitPercent?.let { value -> it[AITradersTable.takeProfitPercent] = value }
            maxDailyLoss?.let { value -> it[AITradersTable.maxDailyLoss] = value }
            rsiPeriod?.let { value -> it[AITradersTable.rsiPeriod] = value }
            rsiOverbought?.let { value -> it[AITradersTable.rsiOverbought] = value }
            rsiOversold?.let { value -> it[AITradersTable.rsiOversold] = value }
            macdFast?.let { value -> it[AITradersTable.macdFast] = value }
            macdSlow?.let { value -> it[AITradersTable.macdSlow] = value }
            macdSignal?.let { value -> it[AITradersTable.macdSignal] = value }
            smaShort?.let { value -> it[AITradersTable.smaShort] = value }
            smaLong?.let { value -> it[AITradersTable.smaLong] = value }
            it[updatedAt] = LocalDateTime.now()
        }
        updated > 0
    }
    
    /**
     * Delete an AI trader
     */
    suspend fun delete(id: Int): Boolean = dbQuery {
        val deleted = AITradersTable.deleteWhere { AITradersTable.id eq id }
        deleted > 0
    }
    
    /**
     * Count total AI traders
     */
    suspend fun count(): Long = dbQuery {
        AITradersTable.selectAll().count()
    }
    
    /**
     * Check if we can create more traders (max 3)
     */
    suspend fun canCreateMore(): Boolean = dbQuery {
        AITradersTable.selectAll().count() < 3
    }
    
    /**
     * Convert database row to AITrader data class
     */
    private fun rowToAITrader(row: ResultRow): AITrader =
        AITrader(
            id = row[AITradersTable.id],
            name = row[AITradersTable.name],
            status = row[AITradersTable.status],
            exchange = row[AITradersTable.exchange],
            tradingPair = row[AITradersTable.tradingPair],
            leverage = row[AITradersTable.leverage],
            initialBalance = row[AITradersTable.initialBalance],
            currentBalance = row[AITradersTable.currentBalance],
            stopLossPercent = row[AITradersTable.stopLossPercent],
            takeProfitPercent = row[AITradersTable.takeProfitPercent],
            maxDailyLoss = row[AITradersTable.maxDailyLoss],
            rsiPeriod = row[AITradersTable.rsiPeriod],
            rsiOverbought = row[AITradersTable.rsiOverbought],
            rsiOversold = row[AITradersTable.rsiOversold],
            macdFast = row[AITradersTable.macdFast],
            macdSlow = row[AITradersTable.macdSlow],
            macdSignal = row[AITradersTable.macdSignal],
            smaShort = row[AITradersTable.smaShort],
            smaLong = row[AITradersTable.smaLong],
            createdAt = row[AITradersTable.createdAt],
            updatedAt = row[AITradersTable.updatedAt],
            lastActiveAt = row[AITradersTable.lastActiveAt]
        )
}

/**
 * Data class representing an AI Trader
 */
data class AITrader(
    val id: Int,
    val name: String,
    val status: String,
    val exchange: String,
    val tradingPair: String,
    val leverage: Int,
    val initialBalance: BigDecimal,
    val currentBalance: BigDecimal,
    val stopLossPercent: BigDecimal,
    val takeProfitPercent: BigDecimal,
    val maxDailyLoss: BigDecimal,
    val rsiPeriod: Int,
    val rsiOverbought: Int,
    val rsiOversold: Int,
    val macdFast: Int,
    val macdSlow: Int,
    val macdSignal: Int,
    val smaShort: Int,
    val smaLong: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val lastActiveAt: LocalDateTime?
)

