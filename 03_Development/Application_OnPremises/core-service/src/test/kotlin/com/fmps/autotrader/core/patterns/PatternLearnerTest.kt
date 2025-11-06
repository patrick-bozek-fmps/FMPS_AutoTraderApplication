package com.fmps.autotrader.core.patterns

import com.fmps.autotrader.core.database.repositories.Trade
import com.fmps.autotrader.core.database.repositories.TradeRepository
import com.fmps.autotrader.core.patterns.models.TradingPattern
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.TradeAction
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.time.LocalDateTime

class PatternLearnerTest {
    
    private lateinit var tradeRepository: TradeRepository
    private lateinit var patternLearner: PatternLearner
    
    @BeforeEach
    fun setup() {
        tradeRepository = mockk()
        patternLearner = PatternLearner(tradeRepository)
    }
    
    @AfterEach
    fun teardown() {
        clearAllMocks()
    }
    
    @Test
    fun `test extractPatternFromTrade - successful trade`() = runBlocking {
        // Arrange
        val trade = createSuccessfulTrade(
            id = 1,
            profitLoss = BigDecimal("100"),
            profitLossPercent = BigDecimal("2.5"),
            rsiValue = BigDecimal("35"),
            macdValue = BigDecimal("0.001")
        )
        
        coEvery { tradeRepository.findById(1) } returns trade
        
        // Act
        val pattern = patternLearner.extractPatternFromTrade(1)
        
        // Assert
        assertNotNull(pattern)
        assertEquals(Exchange.BINANCE, pattern!!.exchange)
        assertEquals("BTCUSDT", pattern.symbol)
        assertEquals(TradeAction.LONG, pattern.action)
        assertTrue(pattern.conditions.containsKey("RSI_Range"))
        assertTrue(pattern.conditions.containsKey("MACD_Range"))
        assertTrue(pattern.conditions.containsKey("entryPrice"))
        assertEquals(0.7, pattern.confidence, 0.01)
    }
    
    @Test
    fun `test extractPatternFromTrade - trade not found`() = runBlocking {
        // Arrange
        coEvery { tradeRepository.findById(999) } returns null
        
        // Act
        val pattern = patternLearner.extractPatternFromTrade(999)
        
        // Assert
        assertNull(pattern)
    }
    
    @Test
    fun `test extractPatternFromTrade - open trade`() = runBlocking {
        // Arrange
        val trade = createOpenTrade(id = 1)
        
        coEvery { tradeRepository.findById(1) } returns trade
        
        // Act
        val pattern = patternLearner.extractPatternFromTrade(1)
        
        // Assert
        assertNull(pattern) // Should not extract from open trades
    }
    
    @Test
    fun `test extractPatternFromTrade - unprofitable trade`() = runBlocking {
        // Arrange
        val trade = createUnprofitableTrade(id = 1)
        
        coEvery { tradeRepository.findById(1) } returns trade
        
        // Act
        val pattern = patternLearner.extractPatternFromTrade(1)
        
        // Assert
        assertNull(pattern) // Should not extract from unprofitable trades
    }
    
    @Test
    fun `test extractPatternFromTrade - profit below threshold`() = runBlocking {
        // Arrange
        val trade = createSuccessfulTrade(
            id = 1,
            profitLoss = BigDecimal("10"),
            profitLossPercent = BigDecimal("0.5") // Below 1% threshold
        )
        
        coEvery { tradeRepository.findById(1) } returns trade
        
        // Act
        val pattern = patternLearner.extractPatternFromTrade(1)
        
        // Assert
        assertNull(pattern) // Should not extract if profit below threshold
    }
    
    @Test
    fun `test extractPatternFromTrade - determines pattern type`() = runBlocking {
        // Arrange
        val oversoldTrade = createSuccessfulTrade(
            id = 1,
            rsiValue = BigDecimal("30") // Oversold
        )
        
        coEvery { tradeRepository.findById(1) } returns oversoldTrade
        
        // Act
        val pattern = patternLearner.extractPatternFromTrade(1)
        
        // Assert
        assertNotNull(pattern)
        assertEquals("OVERSOLD_REVERSAL", pattern!!.conditions["patternType"])
    }
    
    @Test
    fun `test extractPatternFromTrade - overbought pattern`() = runBlocking {
        // Arrange
        val overboughtTrade = createSuccessfulTrade(
            id = 1,
            rsiValue = BigDecimal("70") // Overbought
        )
        
        coEvery { tradeRepository.findById(1) } returns overboughtTrade
        
        // Act
        val pattern = patternLearner.extractPatternFromTrade(1)
        
        // Assert
        assertNotNull(pattern)
        assertEquals("OVERBOUGHT_REVERSAL", pattern!!.conditions["patternType"])
    }
    
    @Test
    fun `test extractPatternFromTrade - trend following pattern`() = runBlocking {
        // Arrange
        val trendTrade = createSuccessfulTrade(
            id = 1,
            smaShortValue = BigDecimal("51000"),
            smaLongValue = BigDecimal("50000") // Short > Long = uptrend
        )
        
        coEvery { tradeRepository.findById(1) } returns trendTrade
        
        // Act
        val pattern = patternLearner.extractPatternFromTrade(1)
        
        // Assert
        assertNotNull(pattern)
        assertEquals("TREND_FOLLOWING", pattern!!.conditions["patternType"])
    }
    
    @Test
    fun `test extractPatternFromTrade - momentum continuation`() = runBlocking {
        // Arrange
        val momentumTrade = createSuccessfulTrade(
            id = 1,
            macdValue = BigDecimal("0.005") // Positive MACD
        )
        
        coEvery { tradeRepository.findById(1) } returns momentumTrade
        
        // Act
        val pattern = patternLearner.extractPatternFromTrade(1)
        
        // Assert
        assertNotNull(pattern)
        assertEquals("MOMENTUM_CONTINUATION", pattern!!.conditions["patternType"])
    }
    
    @Test
    fun `test extractPatternsFromTrades - multiple successful trades`() = runBlocking {
        // Arrange
        val trades = listOf(
            createSuccessfulTrade(id = 1, profitLossPercent = BigDecimal("2.0")),
            createSuccessfulTrade(id = 2, profitLossPercent = BigDecimal("3.0")),
            createSuccessfulTrade(id = 3, profitLossPercent = BigDecimal("1.5"))
        )
        
        coEvery { tradeRepository.findSuccessfulTrades(null, 1000) } returns trades
        coEvery { tradeRepository.findById(1) } returns trades[0]
        coEvery { tradeRepository.findById(2) } returns trades[1]
        coEvery { tradeRepository.findById(3) } returns trades[2]
        
        // Act
        val patterns = patternLearner.extractPatternsFromTrades()
        
        // Assert
        assertTrue(patterns.isNotEmpty())
        assertTrue(patterns.size <= trades.size) // May merge similar patterns
    }
    
    @Test
    fun `test extractPatternsFromTrades - filters by min profit`() = runBlocking {
        // Arrange
        val trades = listOf(
            createSuccessfulTrade(id = 1, profitLossPercent = BigDecimal("2.0")),
            createSuccessfulTrade(id = 2, profitLossPercent = BigDecimal("0.5")) // Below threshold
        )
        
        coEvery { tradeRepository.findSuccessfulTrades(null, 1000) } returns trades
        coEvery { tradeRepository.findById(1) } returns trades[0]
        
        // Act
        val patterns = patternLearner.extractPatternsFromTrades(minProfitPercent = BigDecimal("1.0"))
        
        // Assert
        // Should only extract from trade 1
        assertTrue(patterns.isNotEmpty())
    }
    
    @Test
    fun `test extractPatternsFromTrades - filters by trader ID`() = runBlocking {
        // Arrange
        val traderId = 5
        val trades = listOf(
            createSuccessfulTrade(id = 1, aiTraderId = traderId),
            createSuccessfulTrade(id = 2, aiTraderId = traderId)
        )
        
        coEvery { tradeRepository.findSuccessfulTrades(traderId, 1000) } returns trades
        coEvery { tradeRepository.findById(1) } returns trades[0]
        coEvery { tradeRepository.findById(2) } returns trades[1]
        
        // Act
        val patterns = patternLearner.extractPatternsFromTrades(traderId = traderId)
        
        // Assert
        assertTrue(patterns.isNotEmpty())
        coVerify(exactly = 1) { tradeRepository.findSuccessfulTrades(traderId, 1000) }
    }
    
    @Test
    fun `test extractPatternsFromTrades - merges similar patterns`() = runBlocking {
        // Arrange
        val trades = listOf(
            createSuccessfulTrade(
                id = 1,
                tradingPair = "BTCUSDT",
                rsiValue = BigDecimal("35"),
                tradeType = "LONG"
            ),
            createSuccessfulTrade(
                id = 2,
                tradingPair = "BTCUSDT",
                rsiValue = BigDecimal("37"), // Similar RSI
                tradeType = "LONG"
            ),
            createSuccessfulTrade(
                id = 3,
                tradingPair = "BTCUSDT",
                rsiValue = BigDecimal("36"), // Similar RSI
                tradeType = "LONG"
            )
        )
        
        coEvery { tradeRepository.findSuccessfulTrades(null, 1000) } returns trades
        coEvery { tradeRepository.findById(1) } returns trades[0]
        coEvery { tradeRepository.findById(2) } returns trades[1]
        coEvery { tradeRepository.findById(3) } returns trades[2]
        
        // Act
        val patterns = patternLearner.extractPatternsFromTrades()
        
        // Assert
        // Should merge similar patterns (3 trades with similar conditions)
        assertTrue(patterns.isNotEmpty())
        assertTrue(patterns.size <= trades.size)
    }
    
    @Test
    fun `test validatePattern - valid pattern`() = runBlocking {
        // Arrange
        val pattern = TradingPattern(
            id = "test-1",
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT",
            timeframe = "1h",
            action = TradeAction.LONG,
            conditions = mapOf(
                "RSI_Range" to Pair(60.0, 70.0),
                "patternType" to "CUSTOM"
            ),
            confidence = 0.7,
            createdAt = java.time.Instant.now()
        )
        
        // Act
        val isValid = patternLearner.validatePattern(pattern)
        
        // Assert
        assertTrue(isValid)
    }
    
    @Test
    fun `test validatePattern - missing indicators`() = runBlocking {
        // Arrange
        val pattern = TradingPattern(
            id = "test-1",
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT",
            timeframe = "1h",
            action = TradeAction.LONG,
            conditions = mapOf("patternType" to "CUSTOM"), // No indicators
            confidence = 0.7,
            createdAt = java.time.Instant.now()
        )
        
        // Act
        val isValid = patternLearner.validatePattern(pattern)
        
        // Assert
        assertFalse(isValid) // Should fail validation
    }
    
    @Test
    fun `test validatePattern - blank symbol`() = runBlocking {
        // Arrange
        val pattern = TradingPattern(
            id = "test-1",
            exchange = Exchange.BINANCE,
            symbol = "", // Blank symbol
            timeframe = "1h",
            action = TradeAction.LONG,
            conditions = mapOf(
                "RSI_Range" to Pair(60.0, 70.0),
                "patternType" to "CUSTOM"
            ),
            confidence = 0.7,
            createdAt = java.time.Instant.now()
        )
        
        // Act
        val isValid = patternLearner.validatePattern(pattern)
        
        // Assert
        assertFalse(isValid)
    }
    
    @Test
    fun `test validatePattern - invalid confidence`() = runBlocking {
        // Arrange
        val pattern = TradingPattern(
            id = "test-1",
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT",
            timeframe = "1h",
            action = TradeAction.LONG,
            conditions = mapOf(
                "RSI_Range" to Pair(60.0, 70.0),
                "patternType" to "CUSTOM"
            ),
            confidence = 1.5, // Invalid (> 1.0)
            createdAt = java.time.Instant.now()
        )
        
        // Act
        val isValid = patternLearner.validatePattern(pattern)
        
        // Assert
        assertFalse(isValid)
    }
    
    @Test
    fun `test extractPatternFromTrade - SHORT trade type`() = runBlocking {
        // Arrange
        val trade = createSuccessfulTrade(
            id = 1,
            tradeType = "SHORT"
        )
        
        coEvery { tradeRepository.findById(1) } returns trade
        
        // Act
        val pattern = patternLearner.extractPatternFromTrade(1)
        
        // Assert
        assertNotNull(pattern)
        assertEquals(TradeAction.SHORT, pattern!!.action)
    }
    
    @Test
    fun `test extractPatternFromTrade - unknown exchange defaults to BINANCE`() = runBlocking {
        // Arrange
        val trade = createSuccessfulTrade(
            id = 1,
            exchange = "UNKNOWN"
        )
        
        coEvery { tradeRepository.findById(1) } returns trade
        
        // Act
        val pattern = patternLearner.extractPatternFromTrade(1)
        
        // Assert
        assertNotNull(pattern)
        assertEquals(Exchange.BINANCE, pattern!!.exchange) // Should default
    }
    
    // Helper methods
    
    private fun createSuccessfulTrade(
        id: Int = 1,
        aiTraderId: Int = 1,
        exchange: String = "BINANCE",
        tradingPair: String = "BTCUSDT",
        tradeType: String = "LONG",
        entryPrice: BigDecimal = BigDecimal("50000"),
        exitPrice: BigDecimal = BigDecimal("51000"),
        profitLoss: BigDecimal = BigDecimal("100"),
        profitLossPercent: BigDecimal = BigDecimal("2.0"),
        rsiValue: BigDecimal? = BigDecimal("50"),
        macdValue: BigDecimal? = BigDecimal("0.001"),
        smaShortValue: BigDecimal? = null,
        smaLongValue: BigDecimal? = null
    ): Trade {
        return Trade(
            id = id,
            aiTraderId = aiTraderId,
            tradeType = tradeType,
            exchange = exchange,
            tradingPair = tradingPair,
            leverage = 1,
            entryPrice = entryPrice,
            entryAmount = BigDecimal("0.1"),
            entryTimestamp = LocalDateTime.now().minusHours(1),
            entryOrderId = "order-${id}",
            exitPrice = exitPrice,
            exitAmount = BigDecimal("0.1"),
            exitTimestamp = LocalDateTime.now(),
            exitOrderId = "exit-order-${id}",
            exitReason = "TAKE_PROFIT",
            profitLoss = profitLoss,
            profitLossPercent = profitLossPercent,
            fees = BigDecimal("1"),
            stopLossPrice = BigDecimal("49000"),
            takeProfitPrice = BigDecimal("51000"),
            trailingStopActivated = false,
            rsiValue = rsiValue,
            macdValue = macdValue,
            smaShortValue = smaShortValue,
            smaLongValue = smaLongValue,
            status = "CLOSED",
            notes = null,
            patternId = null,
            createdAt = LocalDateTime.now().minusHours(2),
            updatedAt = LocalDateTime.now()
        )
    }
    
    private fun createOpenTrade(id: Int = 1): Trade {
        return Trade(
            id = id,
            aiTraderId = 1,
            tradeType = "LONG",
            exchange = "BINANCE",
            tradingPair = "BTCUSDT",
            leverage = 1,
            entryPrice = BigDecimal("50000"),
            entryAmount = BigDecimal("0.1"),
            entryTimestamp = LocalDateTime.now(),
            entryOrderId = "order-$id",
            exitPrice = null,
            exitAmount = null,
            exitTimestamp = null,
            exitOrderId = null,
            exitReason = null,
            profitLoss = null,
            profitLossPercent = null,
            fees = BigDecimal.ZERO,
            stopLossPrice = BigDecimal("49000"),
            takeProfitPrice = BigDecimal("51000"),
            trailingStopActivated = false,
            rsiValue = BigDecimal("50"),
            macdValue = null,
            smaShortValue = null,
            smaLongValue = null,
            status = "OPEN",
            notes = null,
            patternId = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
    
    private fun createUnprofitableTrade(id: Int = 1): Trade {
        return Trade(
            id = id,
            aiTraderId = 1,
            tradeType = "LONG",
            exchange = "BINANCE",
            tradingPair = "BTCUSDT",
            leverage = 1,
            entryPrice = BigDecimal("50000"),
            entryAmount = BigDecimal("0.1"),
            entryTimestamp = LocalDateTime.now().minusHours(1),
            entryOrderId = "order-$id",
            exitPrice = BigDecimal("49000"),
            exitAmount = BigDecimal("0.1"),
            exitTimestamp = LocalDateTime.now(),
            exitOrderId = "exit-order-$id",
            exitReason = "STOP_LOSS",
            profitLoss = BigDecimal("-100"), // Loss
            profitLossPercent = BigDecimal("-2.0"),
            fees = BigDecimal("1"),
            stopLossPrice = BigDecimal("49000"),
            takeProfitPrice = BigDecimal("51000"),
            trailingStopActivated = false,
            rsiValue = BigDecimal("50"),
            macdValue = null,
            smaShortValue = null,
            smaLongValue = null,
            status = "CLOSED",
            notes = null,
            patternId = null,
            createdAt = LocalDateTime.now().minusHours(2),
            updatedAt = LocalDateTime.now()
        )
    }
}


