package com.fmps.autotrader.core.database.repositories

import com.fmps.autotrader.core.database.DatabaseFactory
import com.fmps.autotrader.core.database.schema.TradesTable
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.math.BigDecimal
import org.jetbrains.exposed.sql.deleteAll

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TradeRepositoryTest {
    
    private val testDbPath = "./build/test-db/test-trade-repo.db"
    private lateinit var tradeRepository: TradeRepository
    private lateinit var aiTraderRepository: AITraderRepository
    private var testTraderId: Int = 0
    
    @BeforeAll
    fun setup() = runBlocking {
        // Clean up and ensure directory exists
        val testDbFile = File(testDbPath)
        testDbFile.parentFile?.mkdirs()
        testDbFile.delete()
        
        System.setProperty("database.url", "jdbc:sqlite:$testDbPath")
        System.setProperty("app.environment", "test")
        
        val config = ConfigFactory.load()
        DatabaseFactory.init(config)
        
        tradeRepository = TradeRepository()
        aiTraderRepository = AITraderRepository()
        
        // Clear any existing traders (in case of test interference)
        val existingTraders = aiTraderRepository.findAll()
        existingTraders.forEach { aiTraderRepository.delete(it.id) }
        
        // Create a test AI trader
        val traderId = aiTraderRepository.create(
            name = "Test Trader",
            exchange = "BINANCE",
            tradingPair = "BTC/USDT",
            leverage = 10,
            initialBalance = BigDecimal("10000")
        )
        
        testTraderId = requireNotNull(traderId) {
            "Failed to create test AI trader in setup. This should not happen in a clean test environment."
        }
    }
    
    @AfterAll
    fun teardown() {
        DatabaseFactory.close()
        File(testDbPath).delete()
    }
    
    @Test
    fun `should create a trade`() = runBlocking {
        val tradeId = tradeRepository.create(
            aiTraderId = testTraderId,
            tradeType = "LONG",
            exchange = "BINANCE",
            tradingPair = "BTC/USDT",
            leverage = 10,
            entryPrice = BigDecimal("50000"),
            entryAmount = BigDecimal("0.1"),
            stopLossPrice = BigDecimal("49000"),
            takeProfitPrice = BigDecimal("52000"),
            rsiValue = BigDecimal("45.50"),
            macdValue = BigDecimal("0.15")
        )
        
        assertTrue(tradeId > 0)
        
        val trade = tradeRepository.findById(tradeId)
        assertNotNull(trade)
        assertEquals("LONG", trade?.tradeType)
        assertEquals("OPEN", trade?.status)
    }
    
    @Test
    fun `should close a trade and calculate P&L`() = runBlocking {
        // Create a LONG trade
        val tradeId = tradeRepository.create(
            aiTraderId = testTraderId,
            tradeType = "LONG",
            exchange = "BINANCE",
            tradingPair = "BTC/USDT",
            leverage = 10,
            entryPrice = BigDecimal("50000"),
            entryAmount = BigDecimal("0.1"),
            stopLossPrice = BigDecimal("49000"),
            takeProfitPrice = BigDecimal("52000")
        )
        
        // Close the trade with profit
        val closed = tradeRepository.close(
            tradeId = tradeId,
            exitPrice = BigDecimal("52000"),
            exitAmount = BigDecimal("0.1"),
            exitReason = "TAKE_PROFIT",
            fees = BigDecimal("10")
        )
        
        assertTrue(closed)
        
        val trade = tradeRepository.findById(tradeId)
        assertNotNull(trade)
        assertEquals("CLOSED", trade?.status)
        assertEquals("TAKE_PROFIT", trade?.exitReason)
        assertNotNull(trade?.profitLoss)
        assertNotNull(trade?.profitLossPercent)
        
        // Verify profit is positive (price went up for LONG)
        assertTrue(trade?.profitLoss!! > BigDecimal.ZERO)
    }
    
    @Test
    fun `should update stop loss`() = runBlocking {
        val tradeId = tradeRepository.create(
            aiTraderId = testTraderId,
            tradeType = "LONG",
            exchange = "BINANCE",
            tradingPair = "BTC/USDT",
            leverage = 10,
            entryPrice = BigDecimal("50000"),
            entryAmount = BigDecimal("0.1"),
            stopLossPrice = BigDecimal("49000"),
            takeProfitPrice = BigDecimal("52000")
        )
        
        val newStopLoss = BigDecimal("49500")
        val updated = tradeRepository.updateStopLoss(tradeId, newStopLoss, trailingActivated = true)
        
        assertTrue(updated)
        
        val trade = tradeRepository.findById(tradeId)
        assertEquals(0, newStopLoss.compareTo(trade?.stopLossPrice))
        assertTrue(trade?.trailingStopActivated == true)
    }
    
    @Test
    fun `should find open trades for AI trader`() = runBlocking {
        // Close all existing open trades first
        val existingOpen = tradeRepository.findOpenTrades(testTraderId)
        existingOpen.forEach { trade ->
            tradeRepository.close(trade.id, BigDecimal("50000"), BigDecimal("0.1"), "MANUAL")
        }
        
        // Create multiple trades
        val trade1 = tradeRepository.create(
            aiTraderId = testTraderId,
            tradeType = "LONG",
            exchange = "BINANCE",
            tradingPair = "BTC/USDT",
            leverage = 10,
            entryPrice = BigDecimal("50000"),
            entryAmount = BigDecimal("0.1"),
            stopLossPrice = BigDecimal("49000"),
            takeProfitPrice = BigDecimal("52000")
        )
        
        tradeRepository.create(
            aiTraderId = testTraderId,
            tradeType = "SHORT",
            exchange = "BINANCE",
            tradingPair = "ETH/USDT",
            leverage = 10,
            entryPrice = BigDecimal("3000"),
            entryAmount = BigDecimal("1.0"),
            stopLossPrice = BigDecimal("3100"),
            takeProfitPrice = BigDecimal("2900")
        )
        
        // Close one trade
        tradeRepository.close(trade1, BigDecimal("52000"), BigDecimal("0.1"), "TAKE_PROFIT")
        
        // Find open trades
        val openTrades = tradeRepository.findOpenTrades(testTraderId)
        
        assertEquals(1, openTrades.size)
        assertEquals("SHORT", openTrades[0].tradeType)
        assertEquals("OPEN", openTrades[0].status)
    }
    
    @Test
    fun `should calculate trade statistics`() = runBlocking {
        // Note: Existing trades from other tests will be included in statistics
        // This is acceptable for testing the statistics calculation logic
        
        // Create and close multiple trades
        val trade1 = tradeRepository.create(
            aiTraderId = testTraderId,
            tradeType = "LONG",
            exchange = "BINANCE",
            tradingPair = "BTC/USDT",
            leverage = 10,
            entryPrice = BigDecimal("50000"),
            entryAmount = BigDecimal("0.1"),
            stopLossPrice = BigDecimal("49000"),
            takeProfitPrice = BigDecimal("52000")
        )
        tradeRepository.close(trade1, BigDecimal("52000"), BigDecimal("0.1"), "TAKE_PROFIT")
        
        val trade2 = tradeRepository.create(
            aiTraderId = testTraderId,
            tradeType = "LONG",
            exchange = "BINANCE",
            tradingPair = "BTC/USDT",
            leverage = 10,
            entryPrice = BigDecimal("50000"),
            entryAmount = BigDecimal("0.1"),
            stopLossPrice = BigDecimal("49000"),
            takeProfitPrice = BigDecimal("52000")
        )
        tradeRepository.close(trade2, BigDecimal("49000"), BigDecimal("0.1"), "STOP_LOSS")
        
        // Get statistics
        val stats = tradeRepository.getStatistics(testTraderId)
        
        assertNotNull(stats)
        assertTrue(stats.totalTrades >= 2)
        assertTrue(stats.successfulTrades >= 1)
        assertTrue(stats.failedTrades >= 1)
        assertTrue(stats.successRate > BigDecimal.ZERO)
    }

    @Test
    fun `should return paginated trades`() = runBlocking {
        DatabaseFactory.dbQuery { TradesTable.deleteAll() }

        repeat(30) { index ->
            val tradeId = tradeRepository.create(
                aiTraderId = testTraderId,
                tradeType = if (index % 2 == 0) "LONG" else "SHORT",
                exchange = "BINANCE",
                tradingPair = "BTC/USDT",
                leverage = 5,
                entryPrice = BigDecimal("50000") + BigDecimal(index * 100),
                entryAmount = BigDecimal("0.05"),
                stopLossPrice = BigDecimal("49000"),
                takeProfitPrice = BigDecimal("52000")
            )

            if (index % 4 == 0) {
                tradeRepository.close(
                    tradeId = tradeId,
                    exitPrice = BigDecimal("50500"),
                    exitAmount = BigDecimal("0.05"),
                    exitReason = "MANUAL"
                )
            }
        }

        val pageSize = 10
        val paged = tradeRepository.findPaged(
            page = 2,
            pageSize = pageSize,
            status = "OPEN",
            aiTraderId = testTraderId
        )

        assertEquals(pageSize, paged.items.size)
        assertTrue(paged.total >= paged.items.size.toLong())
        assertTrue(paged.items.all { it.status == "OPEN" })
        assertTrue(paged.items.all { it.aiTraderId == testTraderId })
    }
}

