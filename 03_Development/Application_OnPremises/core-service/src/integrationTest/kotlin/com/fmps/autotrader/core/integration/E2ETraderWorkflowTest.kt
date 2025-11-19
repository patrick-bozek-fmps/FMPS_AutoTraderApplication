package com.fmps.autotrader.core.integration

import com.fmps.autotrader.core.api.startApiServer
import com.fmps.autotrader.core.connectors.ConnectorFactory
import com.fmps.autotrader.core.database.repositories.AITraderRepository
import com.fmps.autotrader.core.traders.AITraderManager
import com.fmps.autotrader.shared.enums.Exchange
import io.ktor.server.engine.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

/**
 * End-to-end integration test for complete trader lifecycle workflow
 * 
 * Tests the complete workflow:
 * 1. Create trader via API → Core Service → Database
 * 2. Start trader → Exchange connection → Market data subscription
 * 3. Monitor trader status
 * 4. Stop trader → Resource cleanup → State persistence
 * 
 * **Requirements:**
 * - Test database (SQLite in-memory or file-based)
 * - Mock exchange connectors (no real API keys required for basic tests)
 * 
 * **Tag:** @Tag("integration")
 */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class E2ETraderWorkflowTest {
    
    private lateinit var server: ApplicationEngine
    private lateinit var traderManager: AITraderManager
    private lateinit var repository: AITraderRepository
    private lateinit var connectorFactory: ConnectorFactory
    private var serverPort: Int = 0
    private var createdTraderId: String? = null
    
    @BeforeAll
    fun setup() {
        println("=".repeat(70))
        println("E2E Trader Workflow Integration Test - Setup")
        println("=".repeat(70))
        
        // Initialize test database
        TestUtilities.initTestDatabase("e2e-trader-workflow")
        
        // Initialize components
        repository = AITraderRepository()
        connectorFactory = ConnectorFactory.getInstance()
        traderManager = AITraderManager(repository, connectorFactory, maxTraders = 3)
        
        // Start API server
        serverPort = TestUtilities.findAvailablePort()
        server = startApiServer(host = "127.0.0.1", port = serverPort, wait = false)
        
        // Wait for server to be ready
        runBlocking {
            TestUtilities.waitForServer(serverPort, maxWaitMs = 5000)
        }
        
        println("✅ Test environment initialized")
        println("   Server running on port: $serverPort")
        println()
    }
    
    @AfterAll
    fun tearDown() {
        println("=".repeat(70))
        println("E2E Trader Workflow Integration Test - Cleanup")
        println("=".repeat(70))
        
        // Stop all traders
        runBlocking {
            traderManager.getAllTraders().forEach { trader ->
                runCatching {
                    traderManager.stopTrader(trader.config.id)
                }
            }
        }
        
        // Stop server
        server.stop(gracePeriodMillis = 1000, timeoutMillis = 5000)
        
        // Cleanup database
        TestUtilities.cleanupTestDatabase("e2e-trader-workflow")
        
        println("✅ Test environment cleaned up")
    }
    
    @Test
    @Order(1)
    fun `should create trader via manager`() = runBlocking {
        println("Test 1: Create trader via AITraderManager")
        
        val config = TestUtilities.createTestTraderConfig(
            name = "E2E Test Trader",
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT",
            budgetUsd = 1000.0
        )
        
        val result = traderManager.createTrader(config)
        
        assertTrue(result.isSuccess, "Trader creation should succeed")
        val traderId = result.getOrNull()
        assertNotNull(traderId, "Trader ID should be returned")
        
        createdTraderId = traderId
        
        // Verify trader exists in database
        val trader = repository.findById(traderId!!.toInt())
        assertNotNull(trader, "Trader should exist in database")
        assertEquals("E2E Test Trader", trader!!.name)
        assertEquals("BINANCE", trader.exchange)
        assertEquals("BTC/USDT", trader.tradingPair) // Database stores with slash separator
        
        println("✅ Trader created successfully: ID=$traderId")
    }
    
    @Test
    @Order(2)
    fun `should start trader and verify state`() = runBlocking {
        println("Test 2: Start trader and verify state")
        
        val traderId = createdTraderId ?: fail("Trader ID not available")
        
        // Start trader
        val startResult = traderManager.startTrader(traderId)
        assertTrue(startResult.isSuccess, "Trader start should succeed: ${startResult.exceptionOrNull()?.message}")
        
        // Give trader time to initialize
        delay(1000)
        
        // Verify trader state
        val trader = traderManager.getTrader(traderId)
        assertNotNull(trader, "Trader should exist in manager")
        assertTrue(
            trader!!.getState().name in listOf("RUNNING", "STARTING"),
            "Trader should be RUNNING or STARTING, but was: ${trader.getState()}"
        )
        
        // Verify database state
        val dbTrader = repository.findById(traderId.toInt())
        assertNotNull(dbTrader, "Trader should exist in database")
        assertEquals("ACTIVE", dbTrader!!.status, 
            "Database status should be ACTIVE (maps from STARTING/RUNNING states), but was: ${dbTrader.status}")
        
        println("✅ Trader started successfully: State=${trader.getState()}")
    }
    
    @Test
    @Order(3)
    fun `should monitor trader metrics`() = runBlocking {
        println("Test 3: Monitor trader metrics")
        
        val traderId = createdTraderId ?: fail("Trader ID not available")
        
        val trader = traderManager.getTrader(traderId)
        assertNotNull(trader, "Trader should exist")
        
        // Get metrics
        val metrics = trader!!.getMetrics()
        assertNotNull(metrics, "Metrics should be available")
        assertNotNull(metrics.startTime, "Start time should be set")
        
        println("✅ Trader metrics retrieved:")
        println("   Start time: ${metrics.startTime}")
        println("   Uptime: ${metrics.uptime}")
        println("   Total trades: ${metrics.totalTrades}")
    }
    
    @Test
    @Order(4)
    fun `should stop trader and verify cleanup`() = runBlocking {
        println("Test 4: Stop trader and verify cleanup")
        
        val traderId = createdTraderId ?: fail("Trader ID not available")
        
        // Stop trader
        val stopResult = traderManager.stopTrader(traderId)
        assertTrue(stopResult.isSuccess, "Trader stop should succeed: ${stopResult.exceptionOrNull()?.message}")
        
        // Give trader time to stop
        delay(1000)
        
        // Verify trader state
        val trader = traderManager.getTrader(traderId)
        assertNotNull(trader, "Trader should still exist in manager")
        assertTrue(
            trader!!.getState().name in listOf("STOPPED", "IDLE"),
            "Trader should be STOPPED or IDLE, but was: ${trader.getState()}"
        )
        
        // Verify database state
        val dbTrader = repository.findById(traderId.toInt())
        assertNotNull(dbTrader, "Trader should exist in database")
        assertTrue(
            dbTrader!!.status in listOf("STOPPED", "IDLE"),
            "Database status should be STOPPED or IDLE, but was: ${dbTrader.status}"
        )
        
        println("✅ Trader stopped successfully: State=${trader.getState()}")
    }
    
    @Test
    @Order(5)
    fun `should delete trader and verify removal`() = runBlocking {
        println("Test 5: Delete trader and verify removal")
        
        val traderId = createdTraderId ?: fail("Trader ID not available")
        
        // Delete trader
        val deleteResult = traderManager.deleteTrader(traderId)
        assertTrue(deleteResult.isSuccess, "Trader deletion should succeed: ${deleteResult.exceptionOrNull()?.message}")
        
        // Verify trader removed from manager
        val trader = traderManager.getTrader(traderId)
        assertNull(trader, "Trader should be removed from manager")
        
        // Verify trader removed from database
        val dbTrader = repository.findById(traderId.toInt())
        assertNull(dbTrader, "Trader should be removed from database")
        
        println("✅ Trader deleted successfully")
    }
    
    @Test
    @Order(6)
    fun `should enforce maximum trader limit`() = runBlocking {
        println("Test 6: Enforce maximum trader limit (3 traders)")
        
        // Create 3 traders (max limit)
        val traderIds = mutableListOf<String>()
        repeat(3) { index ->
            val config = TestUtilities.createTestTraderConfig(
                name = "Max Limit Test Trader ${index + 1}",
                exchange = Exchange.BINANCE,
                symbol = "BTCUSDT",
                budgetUsd = 1000.0
            )
            
            val result = traderManager.createTrader(config)
            assertTrue(result.isSuccess, "Trader ${index + 1} creation should succeed")
            traderIds.add(result.getOrNull()!!)
        }
        
        // Try to create 4th trader (should fail)
        val config4 = TestUtilities.createTestTraderConfig(
            name = "Max Limit Test Trader 4",
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT",
            budgetUsd = 1000.0
        )
        
        val result4 = traderManager.createTrader(config4)
        assertTrue(result4.isFailure, "4th trader creation should fail")
        assertTrue(
            result4.exceptionOrNull() is com.fmps.autotrader.core.traders.MaxTradersExceededException,
            "Should throw MaxTradersExceededException"
        )
        
        // Cleanup: delete test traders
        traderIds.forEach { id ->
            runCatching {
                traderManager.deleteTrader(id)
            }
        }
        
        println("✅ Maximum trader limit enforced correctly")
    }
}

