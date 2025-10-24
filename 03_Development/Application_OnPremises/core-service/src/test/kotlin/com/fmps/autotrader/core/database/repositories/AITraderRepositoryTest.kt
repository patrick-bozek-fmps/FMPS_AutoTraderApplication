package com.fmps.autotrader.core.database.repositories

import com.fmps.autotrader.core.database.DatabaseFactory
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AITraderRepositoryTest {
    
    private val testDbPath = "./build/test-db/test-ai-trader-repo.db"
    private lateinit var repository: AITraderRepository
    
    @BeforeAll
    fun setup() {
        File(testDbPath).delete()
        System.setProperty("database.url", "jdbc:sqlite:$testDbPath")
        System.setProperty("app.environment", "test")
        
        val config = ConfigFactory.load()
        DatabaseFactory.init(config)
        
        repository = AITraderRepository()
    }
    
    @AfterAll
    fun teardown() {
        DatabaseFactory.close()
        File(testDbPath).delete()
    }
    
    @Test
    fun `should create AI trader`() = runBlocking {
        val traderId = repository.create(
            name = "Test Trader",
            exchange = "BINANCE",
            tradingPair = "BTC/USDT",
            leverage = 10,
            initialBalance = BigDecimal("10000")
        )
        
        assertNotNull(traderId)
        assertTrue(traderId!! > 0)
    }
    
    @Test
    fun `should find AI trader by ID`() = runBlocking {
        // Clear existing traders first to avoid hitting max limit
        val existing = repository.findAll()
        existing.forEach { repository.delete(it.id) }
        
        val traderId = repository.create(
            name = "Test Trader 2",
            exchange = "BITGET",
            tradingPair = "ETH/USDT",
            leverage = 5,
            initialBalance = BigDecimal("5000")
        )
        
        assertNotNull(traderId, "Trader creation should succeed")
        val trader = repository.findById(traderId!!)
        
        assertNotNull(trader)
        assertEquals("Test Trader 2", trader!!.name)
        assertEquals("BITGET", trader.exchange)
        assertEquals("ETH/USDT", trader.tradingPair)
        assertEquals(5, trader.leverage)
        assertEquals(0, BigDecimal("5000").compareTo(trader.initialBalance))
    }
    
    @Test
    fun `should update AI trader status`() = runBlocking {
        val traderId = repository.create(
            name = "Test Trader 3",
            exchange = "BINANCE",
            tradingPair = "BTC/USDT",
            leverage = 10,
            initialBalance = BigDecimal("10000")
        )
        
        val updated = repository.updateStatus(traderId!!, "ACTIVE")
        assertTrue(updated)
        
        val trader = repository.findById(traderId)
        assertEquals("ACTIVE", trader?.status)
        assertNotNull(trader?.lastActiveAt)
    }
    
    @Test
    fun `should update AI trader balance`() = runBlocking {
        val traderId = repository.create(
            name = "Test Trader 4",
            exchange = "BINANCE",
            tradingPair = "BTC/USDT",
            leverage = 10,
            initialBalance = BigDecimal("10000")
        )
        
        val newBalance = BigDecimal("12000")
        val updated = repository.updateBalance(traderId!!, newBalance)
        assertTrue(updated)
        
        val trader = repository.findById(traderId)
        assertEquals(0, newBalance.compareTo(trader?.currentBalance))
    }
    
    @Test
    fun `should enforce maximum 3 AI traders`() = runBlocking {
        // Clear any existing traders
        val existingTraders = repository.findAll()
        existingTraders.forEach { repository.delete(it.id) }
        
        // Create 3 traders
        val trader1 = repository.create("Trader 1", "BINANCE", "BTC/USDT", 10, BigDecimal("10000"))
        val trader2 = repository.create("Trader 2", "BITGET", "ETH/USDT", 10, BigDecimal("10000"))
        val trader3 = repository.create("Trader 3", "BINANCE", "SOL/USDT", 10, BigDecimal("10000"))
        
        assertNotNull(trader1)
        assertNotNull(trader2)
        assertNotNull(trader3)
        
        // Try to create a 4th trader - should fail
        val trader4 = repository.create("Trader 4", "BINANCE", "ADA/USDT", 10, BigDecimal("10000"))
        assertNull(trader4)
        
        // Verify count
        val count = repository.count()
        assertEquals(3L, count)
        
        // Verify canCreateMore returns false
        assertFalse(repository.canCreateMore())
    }
    
    @Test
    fun `should find active traders`() = runBlocking {
        // Clear existing traders
        val existingTraders = repository.findAll()
        existingTraders.forEach { repository.delete(it.id) }
        
        // Create traders with different statuses
        val trader1 = repository.create("Active Trader 1", "BINANCE", "BTC/USDT", 10, BigDecimal("10000"))
        val trader2 = repository.create("Active Trader 2", "BITGET", "ETH/USDT", 10, BigDecimal("10000"))
        val trader3 = repository.create("Stopped Trader", "BINANCE", "SOL/USDT", 10, BigDecimal("10000"))
        
        repository.updateStatus(trader1!!, "ACTIVE")
        repository.updateStatus(trader2!!, "ACTIVE")
        repository.updateStatus(trader3!!, "STOPPED")
        
        val activeTraders = repository.findActive()
        assertEquals(2, activeTraders.size)
        assertTrue(activeTraders.all { it.status == "ACTIVE" })
    }
    
    @Test
    fun `should delete AI trader`() = runBlocking {
        // Clear existing traders first
        val existing = repository.findAll()
        existing.forEach { repository.delete(it.id) }
        
        val traderId = repository.create(
            name = "Test Trader to Delete",
            exchange = "BINANCE",
            tradingPair = "BTC/USDT",
            leverage = 10,
            initialBalance = BigDecimal("10000")
        )
        
        assertNotNull(traderId, "Trader creation should succeed")
        val deleted = repository.delete(traderId!!)
        assertTrue(deleted)
        
        val trader = repository.findById(traderId)
        assertNull(trader)
    }
}

