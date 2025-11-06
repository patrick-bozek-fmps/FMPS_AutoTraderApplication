package com.fmps.autotrader.core.patterns

import com.fmps.autotrader.core.database.repositories.Pattern
import com.fmps.autotrader.core.database.repositories.PatternRepository
import com.fmps.autotrader.core.database.repositories.TradeRepository
import com.fmps.autotrader.core.indicators.models.MACDResult
import com.fmps.autotrader.core.patterns.models.*
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.TradeAction
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.Duration

class PatternServiceTest {
    
    private lateinit var patternRepository: PatternRepository
    private lateinit var tradeRepository: TradeRepository
    private lateinit var patternService: PatternService
    
    @BeforeEach
    fun setup() {
        patternRepository = mockk()
        tradeRepository = mockk()
        patternService = PatternService(patternRepository, tradeRepository)
    }
    
    @AfterEach
    fun teardown() {
        clearAllMocks()
    }
    
    @Test
    fun `test storePattern - success`() = runBlocking {
        // Arrange
        val pattern = createTestPattern()
        val dbPatternId = 123
        
        coEvery { 
            patternRepository.create(
                name = pattern.name,
                patternType = "CUSTOM",
                tradingPair = pattern.symbol,
                timeframe = pattern.timeframe,
                tradeType = pattern.action.name,
                rsiMin = any(),
                rsiMax = any(),
                macdMin = any(),
                macdMax = any(),
                description = pattern.description,
                tags = any()
            )
        } returns dbPatternId
        
        // Act
        val result = patternService.storePattern(pattern)
        
        // Assert
        assertTrue(result.isSuccess)
        assertEquals(dbPatternId.toString(), result.getOrNull())
        coVerify(exactly = 1) { 
            patternRepository.create(
                name = any(),
                patternType = any(),
                tradingPair = any(),
                timeframe = any(),
                tradeType = any(),
                rsiMin = any(),
                rsiMax = any(),
                macdMin = any(),
                macdMax = any(),
                volumeChangeMin = any(),
                volumeChangeMax = any(),
                priceChangeMin = any(),
                priceChangeMax = any(),
                description = any(),
                tags = any()
            )
        }
    }
    
    @Test
    fun `test storePattern - repository error`() = runBlocking {
        // Arrange
        val pattern = createTestPattern()
        
        coEvery { 
            patternRepository.create(
                name = any(),
                patternType = any(),
                tradingPair = any(),
                timeframe = any(),
                tradeType = any(),
                rsiMin = any(),
                rsiMax = any(),
                macdMin = any(),
                macdMax = any(),
                volumeChangeMin = any(),
                volumeChangeMax = any(),
                priceChangeMin = any(),
                priceChangeMax = any(),
                description = any(),
                tags = any()
            )
        } throws Exception("Database error")
        
        // Act
        val result = patternService.storePattern(pattern)
        
        // Assert
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }
    
    @Test
    fun `test queryPatterns - by symbol and timeframe`() = runBlocking {
        // Arrange
        val criteria = PatternCriteria(
            symbol = "BTCUSDT",
            timeframe = "1h"
        )
        
        val dbPattern = createDbPattern(id = 1, tradingPair = "BTCUSDT", timeframe = "1h")
        
        coEvery { patternRepository.findByTradingPair("BTCUSDT") } returns listOf(dbPattern)
        
        // Act
        val patterns = patternService.queryPatterns(criteria)
        
        // Assert
        assertEquals(1, patterns.size)
        assertEquals("BTCUSDT", patterns[0].symbol)
        assertEquals("1h", patterns[0].timeframe)
    }
    
    @Test
    fun `test queryPatterns - by symbol only`() = runBlocking {
        // Arrange
        val criteria = PatternCriteria(symbol = "ETHUSDT")
        
        val dbPattern1 = createDbPattern(id = 1, tradingPair = "ETHUSDT", timeframe = "1h")
        val dbPattern2 = createDbPattern(id = 2, tradingPair = "ETHUSDT", timeframe = "4h")
        
        coEvery { patternRepository.findByTradingPair("ETHUSDT") } returns listOf(dbPattern1, dbPattern2)
        
        // Act
        val patterns = patternService.queryPatterns(criteria)
        
        // Assert
        assertEquals(2, patterns.size)
        assertTrue(patterns.all { it.symbol == "ETHUSDT" })
    }
    
    @Test
    fun `test queryPatterns - with filters`() = runBlocking {
        // Arrange
        val criteria = PatternCriteria(
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT",
            minSuccessRate = 0.7,
            minUsageCount = 5,
            action = TradeAction.LONG
        )
        
        val dbPattern1 = createDbPattern(
            id = 1,
            tradingPair = "BTCUSDT",
            tradeType = "LONG",
            successRate = BigDecimal("80"),
            totalOccurrences = 10
        )
        val dbPattern2 = createDbPattern(
            id = 2,
            tradingPair = "BTCUSDT",
            tradeType = "LONG",
            successRate = BigDecimal("50"),
            totalOccurrences = 3
        )
        
        coEvery { patternRepository.findByTradingPair("BTCUSDT") } returns listOf(dbPattern1, dbPattern2)
        
        // Act
        val patterns = patternService.queryPatterns(criteria)
        
        // Assert
        assertEquals(1, patterns.size) // Only pattern1 should pass filters
        assertEquals(0.8, patterns[0].successRate, 0.01)
    }
    
    @Test
    fun `test queryPatterns - empty result`() = runBlocking {
        // Arrange
        val criteria = PatternCriteria(symbol = "UNKNOWN")
        
        coEvery { patternRepository.findByTradingPair("UNKNOWN") } returns emptyList()
        
        // Act
        val patterns = patternService.queryPatterns(criteria)
        
        // Assert
        assertTrue(patterns.isEmpty())
    }
    
    @Test
    fun `test matchPatterns - finds matching patterns`() = runBlocking {
        // Arrange
        val conditions = createMarketConditions(
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT",
            indicators = mapOf(
                "RSI" to 65.0,
                "MACD" to MACDResult(0.0015, 0.001, 0.0005)
            )
        )
        
        val dbPattern = createDbPattern(
            id = 1,
            tradingPair = "BTCUSDT",
            rsiMin = BigDecimal("60"),
            rsiMax = BigDecimal("70")
        )
        
        coEvery { patternRepository.findByTradingPair("BTCUSDT") } returns listOf(dbPattern)
        
        // Act
        val matches = patternService.matchPatterns(conditions, minRelevance = 0.5)
        
        // Assert
        assertTrue(matches.isNotEmpty())
        assertTrue(matches[0].relevanceScore >= 0.5)
        assertEquals(dbPattern.id.toString(), matches[0].pattern.id)
    }
    
    @Test
    fun `test matchPatterns - filters by min relevance`() = runBlocking {
        // Arrange
        val conditions = createMarketConditions(
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT"
        )
        
        val dbPattern = createDbPattern(id = 1, tradingPair = "BTCUSDT")
        
        coEvery { patternRepository.findByTradingPair("BTCUSDT") } returns listOf(dbPattern)
        
        // Act
        val matches = patternService.matchPatterns(conditions, minRelevance = 0.9)
        
        // Assert
        // Should filter out low-relevance patterns
        assertTrue(matches.all { it.relevanceScore >= 0.9 })
    }
    
    @Test
    fun `test matchPatterns - limits results`() = runBlocking {
        // Arrange
        val conditions = createMarketConditions(symbol = "BTCUSDT")
        
        val dbPatterns = (1..20).map { 
            createDbPattern(id = it, tradingPair = "BTCUSDT")
        }
        
        coEvery { patternRepository.findByTradingPair("BTCUSDT") } returns dbPatterns
        
        // Act
        val matches = patternService.matchPatterns(conditions, maxResults = 5)
        
        // Assert
        assertTrue(matches.size <= 5)
    }
    
    @Test
    fun `test updatePatternPerformance - success`() = runBlocking {
        // Arrange
        val patternId = "123"
        val outcome = TradeOutcome(
            patternId = patternId,
            success = true,
            returnAmount = BigDecimal("100.50"),
            timestamp = Instant.now()
        )
        
        val dbPattern = createDbPattern(id = 123)
        
        coEvery { patternRepository.findById(123) } returns dbPattern
        coEvery { patternRepository.updateStatistics(123, outcome.returnAmount, true) } returns true
        
        // Act
        val result = patternService.updatePatternPerformance(patternId, outcome)
        
        // Assert
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { patternRepository.updateStatistics(123, outcome.returnAmount, true) }
    }
    
    @Test
    fun `test updatePatternPerformance - pattern not found`() = runBlocking {
        // Arrange
        val patternId = "999"
        val outcome = TradeOutcome(
            patternId = patternId,
            success = true,
            returnAmount = BigDecimal("100"),
            timestamp = Instant.now()
        )
        
        coEvery { patternRepository.findById(999) } returns null
        
        // Act
        val result = patternService.updatePatternPerformance(patternId, outcome)
        
        // Assert
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `test prunePatterns - by age`() = runBlocking {
        // Arrange
        val criteria = PruneCriteria(
            maxAge = Duration.ofDays(90)
        )
        
        val oldPattern = createDbPattern(
            id = 1,
            createdAt = LocalDateTime.now().minusDays(100)
        )
        val newPattern = createDbPattern(
            id = 2,
            createdAt = LocalDateTime.now().minusDays(30)
        )
        
        coEvery { patternRepository.findActive() } returns listOf(oldPattern, newPattern)
        coEvery { patternRepository.deactivate(1) } returns true
        
        // Act
        val result = patternService.prunePatterns(criteria)
        
        // Assert
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        coVerify(exactly = 1) { patternRepository.deactivate(1) }
        coVerify(exactly = 0) { patternRepository.deactivate(2) }
    }
    
    @Test
    fun `test prunePatterns - by success rate`() = runBlocking {
        // Arrange
        val criteria = PruneCriteria(
            minSuccessRate = 0.5
        )
        
        val goodPattern = createDbPattern(
            id = 1,
            successRate = BigDecimal("80")
        )
        val badPattern = createDbPattern(
            id = 2,
            successRate = BigDecimal("30")
        )
        
        coEvery { patternRepository.findActive() } returns listOf(goodPattern, badPattern)
        coEvery { patternRepository.deactivate(2) } returns true
        
        // Act
        val result = patternService.prunePatterns(criteria)
        
        // Assert
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        coVerify(exactly = 1) { patternRepository.deactivate(2) }
    }
    
    @Test
    fun `test prunePatterns - by usage count`() = runBlocking {
        // Arrange
        val criteria = PruneCriteria(
            minUsageCount = 5
        )
        
        val usedPattern = createDbPattern(
            id = 1,
            totalOccurrences = 10
        )
        val unusedPattern = createDbPattern(
            id = 2,
            totalOccurrences = 2
        )
        
        coEvery { patternRepository.findActive() } returns listOf(usedPattern, unusedPattern)
        coEvery { patternRepository.deactivate(2) } returns true
        
        // Act
        val result = patternService.prunePatterns(criteria)
        
        // Assert
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }
    
    @Test
    fun `test prunePatterns - keep top N`() = runBlocking {
        // Arrange
        val criteria = PruneCriteria(
            maxPatterns = 3
        )
        
        val patterns = (1..5).map { 
            createDbPattern(
                id = it,
                successRate = BigDecimal(100 - it * 10),
                totalOccurrences = 10 - it
            )
        }
        
        coEvery { patternRepository.findActive() } returns patterns
        coEvery { patternRepository.deactivate(any()) } returns true
        
        // Act
        val result = patternService.prunePatterns(criteria)
        
        // Assert
        assertTrue(result.isSuccess)
        // Should keep top 3, remove 2
        assertTrue(result.getOrNull()!! >= 2)
    }
    
    @Test
    fun `test prunePatterns - empty criteria`() = runBlocking {
        // Arrange
        val criteria = PruneCriteria()
        
        // Act
        val result = patternService.prunePatterns(criteria)
        
        // Assert
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
        coVerify(exactly = 0) { patternRepository.findActive() }
    }
    
    @Test
    fun `test getPattern - found`() = runBlocking {
        // Arrange
        val patternId = "123"
        val dbPattern = createDbPattern(id = 123)
        
        coEvery { patternRepository.findById(123) } returns dbPattern
        
        // Act
        val pattern = patternService.getPattern(patternId)
        
        // Assert
        assertNotNull(pattern)
        assertEquals("123", pattern?.id)
    }
    
    @Test
    fun `test getPattern - not found`() = runBlocking {
        // Arrange
        val patternId = "999"
        
        coEvery { patternRepository.findById(999) } returns null
        
        // Act
        val pattern = patternService.getPattern(patternId)
        
        // Assert
        assertNull(pattern)
    }
    
    @Test
    fun `test getPattern - invalid ID`() = runBlocking {
        // Arrange
        val patternId = "invalid"
        
        // Act
        val pattern = patternService.getPattern(patternId)
        
        // Assert
        assertNull(pattern)
        coVerify(exactly = 0) { patternRepository.findById(any()) }
    }
    
    @Test
    fun `test getTopPerformingPatterns`() = runBlocking {
        // Arrange
        val dbPatterns = listOf(
            createDbPattern(id = 1, successRate = BigDecimal("90"), totalOccurrences = 10),
            createDbPattern(id = 2, successRate = BigDecimal("80"), totalOccurrences = 8),
            createDbPattern(id = 3, successRate = BigDecimal("70"), totalOccurrences = 6)
        )
        
        coEvery { patternRepository.getTopPatterns(10, 5) } returns dbPatterns
        
        // Act
        val patterns = patternService.getTopPerformingPatterns(limit = 10, minOccurrences = 5)
        
        // Assert
        assertEquals(3, patterns.size)
        assertEquals(0.9, patterns[0].successRate, 0.01)
    }
    
    // Helper methods
    
    private fun createTestPattern(
        id: String = "test-pattern-1",
        exchange: Exchange = Exchange.BINANCE,
        symbol: String = "BTCUSDT",
        timeframe: String = "1h",
        action: TradeAction = TradeAction.LONG,
        conditions: Map<String, Any> = mapOf(
            "RSI_Range" to Pair(60.0, 70.0),
            "MACD_Range" to Pair(0.001, 0.002),
            "patternType" to "CUSTOM"
        )
    ): TradingPattern {
        return TradingPattern(
            id = id,
            name = "Test Pattern",
            exchange = exchange,
            symbol = symbol,
            timeframe = timeframe,
            action = action,
            conditions = conditions,
            confidence = 0.7,
            createdAt = Instant.now(),
            lastUsedAt = null,
            usageCount = 0,
            successCount = 0,
            successRate = 0.0,
            averageReturn = BigDecimal.ZERO,
            description = "Test pattern description",
            tags = listOf("test")
        )
    }
    
    private fun createDbPattern(
        id: Int = 1,
        name: String? = "Test Pattern",
        exchange: String = "BINANCE",
        tradingPair: String = "BTCUSDT",
        timeframe: String = "1h",
        tradeType: String = "LONG",
        rsiMin: BigDecimal? = BigDecimal("60"),
        rsiMax: BigDecimal? = BigDecimal("70"),
        macdMin: BigDecimal? = null,
        macdMax: BigDecimal? = null,
        successRate: BigDecimal = BigDecimal("70"),
        totalOccurrences: Int = 5,
        successfulTrades: Int = 4,
        createdAt: LocalDateTime = LocalDateTime.now()
    ): Pattern {
        return Pattern(
            id = id,
            name = name,
            patternType = "CUSTOM",
            exchange = exchange,
            tradingPair = tradingPair,
            timeframe = timeframe,
            tradeType = tradeType,
            rsiMin = rsiMin,
            rsiMax = rsiMax,
            macdMin = macdMin,
            macdMax = macdMax,
            volumeChangeMin = null,
            volumeChangeMax = null,
            priceChangeMin = null,
            priceChangeMax = null,
            totalOccurrences = totalOccurrences,
            successfulTrades = successfulTrades,
            failedTrades = totalOccurrences - successfulTrades,
            successRate = successRate,
            averageProfitLoss = BigDecimal("100"),
            bestProfitLoss = BigDecimal("200"),
            worstProfitLoss = BigDecimal("-50"),
            confidence = successRate,
            minOccurrencesForConfidence = 5,
            isActive = true,
            description = "Test pattern",
            tags = null,
            createdAt = createdAt,
            updatedAt = LocalDateTime.now(),
            lastMatchedAt = null
        )
    }
    
    private fun createMarketConditions(
        exchange: Exchange = Exchange.BINANCE,
        symbol: String = "BTCUSDT",
        currentPrice: BigDecimal = BigDecimal("50000"),
        indicators: Map<String, Any> = emptyMap(),
        timestamp: Instant = Instant.now()
    ): MarketConditions {
        return MarketConditions(
            exchange = exchange,
            symbol = symbol,
            currentPrice = currentPrice,
            indicators = indicators,
            timestamp = timestamp
        )
    }
}

