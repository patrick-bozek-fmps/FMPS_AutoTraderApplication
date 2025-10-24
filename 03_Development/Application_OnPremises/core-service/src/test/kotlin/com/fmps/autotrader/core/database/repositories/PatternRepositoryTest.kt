package com.fmps.autotrader.core.database.repositories

import com.fmps.autotrader.core.database.DatabaseFactory
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PatternRepositoryTest {
    
    private val testDbPath = "./build/test-db/test-pattern-repo.db"
    private lateinit var repository: PatternRepository
    
    @BeforeAll
    fun setup() {
        File(testDbPath).delete()
        System.setProperty("database.url", "jdbc:sqlite:$testDbPath")
        System.setProperty("app.environment", "test")
        
        val config = ConfigFactory.load()
        DatabaseFactory.init(config)
        
        repository = PatternRepository()
    }
    
    @AfterAll
    fun teardown() {
        DatabaseFactory.close()
        File(testDbPath).delete()
    }
    
    @Test
    fun `should create a pattern`() = runBlocking {
        val patternId = repository.create(
            name = "RSI Oversold Reversal",
            patternType = "REVERSAL",
            tradingPair = "BTC/USDT",
            timeframe = "1h",
            tradeType = "LONG",
            rsiMin = BigDecimal("20"),
            rsiMax = BigDecimal("30"),
            macdMin = BigDecimal("-0.5"),
            macdMax = BigDecimal("0"),
            description = "RSI oversold with MACD turning positive"
        )
        
        assertTrue(patternId > 0)
        
        val pattern = repository.findById(patternId)
        assertNotNull(pattern)
        assertEquals("RSI Oversold Reversal", pattern?.name)
        assertEquals("REVERSAL", pattern?.patternType)
        assertEquals(0, pattern?.totalOccurrences)
    }
    
    @Test
    fun `should update pattern statistics after successful trade`() = runBlocking {
        val patternId = repository.create(
            name = "Test Pattern",
            patternType = "BREAKOUT",
            tradingPair = "BTC/USDT",
            timeframe = "15m",
            tradeType = "LONG"
        )
        
        // Simulate a successful trade
        val updated = repository.updateStatistics(
            patternId = patternId,
            profitLoss = BigDecimal("100"),
            isSuccessful = true
        )
        
        assertTrue(updated)
        
        val pattern = repository.findById(patternId)
        assertNotNull(pattern)
        assertEquals(1, pattern?.totalOccurrences)
        assertEquals(1, pattern?.successfulTrades)
        assertEquals(0, pattern?.failedTrades)
        assertEquals(0, BigDecimal("100.00").compareTo(pattern?.successRate))
        assertEquals(0, BigDecimal("100").compareTo(pattern?.averageProfitLoss))
    }
    
    @Test
    fun `should update pattern statistics after failed trade`() = runBlocking {
        val patternId = repository.create(
            name = "Test Pattern 2",
            patternType = "CONTINUATION",
            tradingPair = "ETH/USDT",
            timeframe = "1h",
            tradeType = "SHORT"
        )
        
        // Simulate a failed trade
        val updated = repository.updateStatistics(
            patternId = patternId,
            profitLoss = BigDecimal("-50"),
            isSuccessful = false
        )
        
        assertTrue(updated)
        
        val pattern = repository.findById(patternId)
        assertNotNull(pattern)
        assertEquals(1, pattern?.totalOccurrences)
        assertEquals(0, pattern?.successfulTrades)
        assertEquals(1, pattern?.failedTrades)
        assertEquals(0, BigDecimal("0.00").compareTo(pattern?.successRate))
        assertEquals(0, BigDecimal("-50").compareTo(pattern?.averageProfitLoss))
    }
    
    @Test
    fun `should calculate confidence based on occurrences`() = runBlocking {
        val patternId = repository.create(
            name = "Confidence Test Pattern",
            patternType = "REVERSAL",
            tradingPair = "BTC/USDT",
            timeframe = "1h",
            tradeType = "LONG"
        )
        
        // Add 5 successful trades (below min occurrences of 10)
        repeat(5) {
            repository.updateStatistics(patternId, BigDecimal("100"), isSuccessful = true)
        }
        
        var pattern = repository.findById(patternId)
        assertNotNull(pattern)
        assertEquals(5, pattern?.totalOccurrences)
        
        // Confidence should be proportional (50% of min occurrences, so confidence = success_rate * 0.5)
        val expectedConfidence = BigDecimal("100") * BigDecimal("0.5")
        assertTrue(pattern?.confidence!! < BigDecimal("100"))
        
        // Add 5 more successful trades (now at min occurrences of 10)
        repeat(5) {
            repository.updateStatistics(patternId, BigDecimal("100"), isSuccessful = true)
        }
        
        pattern = repository.findById(patternId)
        assertEquals(10, pattern?.totalOccurrences)
        
        // Confidence should now be full (100% success rate)
        assertEquals(BigDecimal("100.00"), pattern?.confidence)
    }
    
    @Test
    fun `should find matching patterns`() = runBlocking {
        // Create patterns with different RSI ranges
        val pattern1 = repository.create(
            name = "Oversold Pattern",
            patternType = "REVERSAL",
            tradingPair = "BTC/USDT",
            timeframe = "1h",
            tradeType = "LONG",
            rsiMin = BigDecimal("20"),
            rsiMax = BigDecimal("30")
        )
        
        // Give it high confidence
        repeat(10) {
            repository.updateStatistics(pattern1, BigDecimal("100"), isSuccessful = true)
        }
        
        val pattern2 = repository.create(
            name = "Overbought Pattern",
            patternType = "REVERSAL",
            tradingPair = "BTC/USDT",
            timeframe = "1h",
            tradeType = "SHORT",
            rsiMin = BigDecimal("70"),
            rsiMax = BigDecimal("80")
        )
        
        // Give it high confidence
        repeat(10) {
            repository.updateStatistics(pattern2, BigDecimal("100"), isSuccessful = true)
        }
        
        // Find patterns matching oversold conditions
        val matchingPatterns = repository.findMatchingPatterns(
            tradingPair = "BTC/USDT",
            timeframe = "1h",
            tradeType = "LONG",
            rsiValue = BigDecimal("25"),
            macdValue = null,
            minConfidence = BigDecimal("50")
        )
        
        assertTrue(matchingPatterns.isNotEmpty())
        assertTrue(matchingPatterns.any { it.name == "Oversold Pattern" })
        assertFalse(matchingPatterns.any { it.name == "Overbought Pattern" })
    }
    
    @Test
    fun `should get top performing patterns`() = runBlocking {
        // Create multiple patterns with different success rates
        val pattern1 = repository.create(
            name = "High Success Pattern",
            patternType = "BREAKOUT",
            tradingPair = "BTC/USDT",
            timeframe = "1h",
            tradeType = "LONG"
        )
        
        repeat(10) {
            repository.updateStatistics(pattern1, BigDecimal("100"), isSuccessful = true)
        }
        
        val pattern2 = repository.create(
            name = "Medium Success Pattern",
            patternType = "CONTINUATION",
            tradingPair = "ETH/USDT",
            timeframe = "1h",
            tradeType = "LONG"
        )
        
        repeat(5) {
            repository.updateStatistics(pattern2, BigDecimal("100"), isSuccessful = true)
        }
        repeat(5) {
            repository.updateStatistics(pattern2, BigDecimal("-50"), isSuccessful = false)
        }
        
        // Get top patterns
        val topPatterns = repository.getTopPatterns(limit = 10, minOccurrences = 5)
        
        assertTrue(topPatterns.isNotEmpty())
        // First pattern should have higher success rate
        assertTrue(topPatterns[0].successRate >= topPatterns[topPatterns.size - 1].successRate)
    }
    
    @Test
    fun `should deactivate and activate patterns`() = runBlocking {
        val patternId = repository.create(
            name = "Activation Test Pattern",
            patternType = "REVERSAL",
            tradingPair = "BTC/USDT",
            timeframe = "1h",
            tradeType = "LONG"
        )
        
        var pattern = repository.findById(patternId)
        assertTrue(pattern?.isActive == true)
        
        // Deactivate
        repository.deactivate(patternId)
        pattern = repository.findById(patternId)
        assertFalse(pattern?.isActive == true)
        
        // Activate
        repository.activate(patternId)
        pattern = repository.findById(patternId)
        assertTrue(pattern?.isActive == true)
    }
    
    @Test
    fun `should find active patterns only`() = runBlocking {
        val pattern1 = repository.create(
            name = "Active Pattern",
            patternType = "BREAKOUT",
            tradingPair = "BTC/USDT",
            timeframe = "1h",
            tradeType = "LONG"
        )
        
        val pattern2 = repository.create(
            name = "Inactive Pattern",
            patternType = "REVERSAL",
            tradingPair = "BTC/USDT",
            timeframe = "1h",
            tradeType = "LONG"
        )
        
        repository.deactivate(pattern2)
        
        val activePatterns = repository.findActive()
        
        assertTrue(activePatterns.any { it.name == "Active Pattern" })
        assertFalse(activePatterns.any { it.name == "Inactive Pattern" })
    }
}

