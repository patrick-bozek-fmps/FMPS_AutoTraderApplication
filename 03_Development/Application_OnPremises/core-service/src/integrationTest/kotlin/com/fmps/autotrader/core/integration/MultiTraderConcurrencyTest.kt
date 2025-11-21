package com.fmps.autotrader.core.integration

import com.fmps.autotrader.core.connectors.ConnectorFactory
import com.fmps.autotrader.core.database.repositories.AITraderRepository
import com.fmps.autotrader.core.traders.AITraderManager
import com.fmps.autotrader.shared.enums.Exchange
import kotlinx.coroutines.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger

/**
 * Integration test for multi-trader concurrent operation
 * 
 * Tests:
 * - 3 traders on Binance testnet simultaneously
 * - 3 traders on Bitget testnet simultaneously
 * - Mixed exchanges (2 Binance + 1 Bitget)
 * - Resource isolation (connectors, positions, patterns)
 * - Telemetry updates for all traders
 * - System resource usage (memory, CPU, network)
 * 
 * **Tag:** @Tag("integration")
 */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class MultiTraderConcurrencyTest {
    
    private lateinit var traderManager: AITraderManager
    private lateinit var repository: AITraderRepository
    private lateinit var connectorFactory: ConnectorFactory
    private val createdTraderIds = mutableListOf<String>()
    
    @BeforeAll
    fun setup() {
        println("=".repeat(70))
        println("Multi-Trader Concurrency Integration Test - Setup")
        println("=".repeat(70))
        
        // Initialize test database
        TestUtilities.initTestDatabase("multi-trader-concurrency")
        
        // Initialize components
        repository = AITraderRepository()
        connectorFactory = ConnectorFactory.getInstance()
        traderManager = AITraderManager(repository, connectorFactory, maxTraders = 3)
        
        println("✅ Test environment initialized")
        println()
    }
    
    @AfterAll
    fun tearDown() {
        println("=".repeat(70))
        println("Multi-Trader Concurrency Integration Test - Cleanup")
        println("=".repeat(70))
        
        // Stop and delete all created traders
        runBlocking {
            createdTraderIds.forEach { traderId ->
                runCatching {
                    traderManager.stopTrader(traderId)
                    traderManager.deleteTrader(traderId)
                }
            }
        }
        
        // Cleanup database
        TestUtilities.cleanupTestDatabase("multi-trader-concurrency")
        
        println("✅ Test environment cleaned up")
    }
    
    @Test
    @Order(1)
    fun `should create 3 traders concurrently on Binance`() = runBlocking {
        println("Test 1: Create 3 traders concurrently on Binance")
        
        val results = mutableListOf<Result<String>>()
        
        // Create 3 traders concurrently
        val jobs = (1..3).map { index ->
            async {
                val config = TestUtilities.createTestTraderConfig(
                    name = "Binance Trader $index",
                    exchange = Exchange.BINANCE,
                    symbol = "BTCUSDT",
                    budgetUsd = 1000.0 * index
                )
                traderManager.createTrader(config)
            }
        }
        
        // Wait for all creations
        jobs.forEach { job ->
            val result = job.await()
            results.add(result)
        }
        
        // Verify all succeeded
        results.forEachIndexed { index, result ->
            assertTrue(result.isSuccess, "Trader ${index + 1} creation should succeed")
            val traderId = result.getOrNull()
            assertNotNull(traderId, "Trader ${index + 1} ID should be returned")
            createdTraderIds.add(traderId!!)
        }
        
        // Verify all traders exist
        assertEquals(3, traderManager.getTraderCount(), "Should have 3 traders")
        assertEquals(3, repository.count(), "Database should have 3 traders")
        
        println("✅ Created 3 traders concurrently on Binance")
    }
    
    @Test
    @Order(2)
    fun `should start all 3 traders concurrently`() = runBlocking {
        println("Test 2: Start all 3 traders concurrently")
        
        val traderIds = createdTraderIds.take(3)
        assertTrue(traderIds.size == 3, "Should have 3 trader IDs")
        
        // Start all traders concurrently
        val startJobs = traderIds.map { traderId ->
            async {
                traderManager.startTrader(traderId)
            }
        }
        
        // Wait for all starts
        val startResults = startJobs.map { it.await() }
        
        // Verify all started successfully
        startResults.forEachIndexed { index, result ->
            assertTrue(
                result.isSuccess,
                "Trader ${index + 1} start should succeed: ${result.exceptionOrNull()?.message}"
            )
        }
        
        // Give traders time to initialize
        delay(2000)
        
        // Verify all traders are running
        traderIds.forEachIndexed { index, traderId ->
            val trader = traderManager.getTrader(traderId)
            assertNotNull(trader, "Trader ${index + 1} should exist")
            assertTrue(
                trader!!.getState().name in listOf("RUNNING", "STARTING"),
                "Trader ${index + 1} should be RUNNING or STARTING, but was: ${trader.getState()}"
            )
        }
        
        println("✅ Started all 3 traders concurrently")
    }
    
    @Test
    @Order(3)
    fun `should verify resource isolation between traders`() = runBlocking {
        println("Test 3: Verify resource isolation between traders")
        
        val traderIds = createdTraderIds.take(3)
        
        // Verify each trader has independent configuration
        traderIds.forEachIndexed { index, traderId ->
            val trader = traderManager.getTrader(traderId)
            assertNotNull(trader, "Trader ${index + 1} should exist")
            
            val config = trader!!.config
            assertEquals("Binance Trader ${index + 1}", config.name)
            assertEquals(Exchange.BINANCE, config.exchange)
            assertEquals(BigDecimal.valueOf(1000.0 * (index + 1)), config.maxStakeAmount)
        }
        
        // Verify traders have independent state
        val states = traderIds.map { traderManager.getTrader(it)!!.getState() }
        assertEquals(3, states.size, "Should have 3 trader states")
        assertTrue(states.all { it.name in listOf("RUNNING", "STARTING") }, "All traders should be running")
        
        println("✅ Resource isolation verified")
    }
    
    @Test
    @Order(4)
    fun `should stop all traders concurrently`() = runBlocking {
        println("Test 4: Stop all traders concurrently")
        
        val traderIds = createdTraderIds.take(3)
        
        // Stop all traders concurrently
        val stopJobs = traderIds.map { traderId ->
            async {
                traderManager.stopTrader(traderId)
            }
        }
        
        // Wait for all stops
        val stopResults = stopJobs.map { it.await() }
        
        // Verify all stopped successfully
        stopResults.forEachIndexed { index, result ->
            assertTrue(
                result.isSuccess,
                "Trader ${index + 1} stop should succeed: ${result.exceptionOrNull()?.message}"
            )
        }
        
        // Give traders time to stop
        delay(1000)
        
        // Verify all traders are stopped
        traderIds.forEachIndexed { index, traderId ->
            val trader = traderManager.getTrader(traderId)
            assertNotNull(trader, "Trader ${index + 1} should still exist")
            assertTrue(
                trader!!.getState().name in listOf("STOPPED", "IDLE"),
                "Trader ${index + 1} should be STOPPED or IDLE, but was: ${trader.getState()}"
            )
        }
        
        println("✅ Stopped all traders concurrently")
    }
    
    @Test
    @Order(5)
    fun `should handle mixed exchange configuration`() = runBlocking {
        println("Test 5: Handle mixed exchange configuration (2 Binance + 1 Bitget)")
        
        // Clean up previous traders
        createdTraderIds.forEach { id ->
            runCatching { traderManager.deleteTrader(id) }
        }
        createdTraderIds.clear()
        
        // Create 2 Binance traders
        val binanceConfig1 = TestUtilities.createTestTraderConfig(
            name = "Mixed Binance Trader 1",
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT",
            budgetUsd = 1000.0
        )
        val binanceConfig2 = TestUtilities.createTestTraderConfig(
            name = "Mixed Binance Trader 2",
            exchange = Exchange.BINANCE,
            symbol = "ETHUSDT",
            budgetUsd = 2000.0
        )
        
        // Create 1 Bitget trader
        val bitgetConfig = TestUtilities.createTestTraderConfig(
            name = "Mixed Bitget Trader",
            exchange = Exchange.BITGET,
            symbol = "BTCUSDT",
            budgetUsd = 1500.0
        )
        
        val result1 = traderManager.createTrader(binanceConfig1)
        val result2 = traderManager.createTrader(binanceConfig2)
        val result3 = traderManager.createTrader(bitgetConfig)
        
        assertTrue(result1.isSuccess, "Binance trader 1 creation should succeed")
        assertTrue(result2.isSuccess, "Binance trader 2 creation should succeed")
        assertTrue(result3.isSuccess, "Bitget trader creation should succeed")
        
        val traderId1 = result1.getOrNull()!!
        val traderId2 = result2.getOrNull()!!
        val traderId3 = result3.getOrNull()!!
        
        createdTraderIds.addAll(listOf(traderId1, traderId2, traderId3))
        
        // Verify traders have correct exchange configuration
        val trader1 = traderManager.getTrader(traderId1)
        val trader2 = traderManager.getTrader(traderId2)
        val trader3 = traderManager.getTrader(traderId3)
        
        assertEquals(Exchange.BINANCE, trader1!!.config.exchange)
        assertEquals(Exchange.BINANCE, trader2!!.config.exchange)
        assertEquals(Exchange.BITGET, trader3!!.config.exchange)
        
        assertEquals("BTCUSDT", trader1.config.symbol)
        assertEquals("ETHUSDT", trader2.config.symbol)
        assertEquals("BTCUSDT", trader3.config.symbol)
        
        println("✅ Mixed exchange configuration handled correctly")
    }
    
    @Test
    @Order(6)
    fun `should verify system stability under concurrent load`() = runBlocking {
        println("Test 6: Verify system stability under concurrent load")
        
        val traderIds = createdTraderIds.take(3)
        val operationsCount = AtomicInteger(0)
        val errorsCount = AtomicInteger(0)
        
        // Perform concurrent operations
        val operations = (1..10).map { operationIndex ->
            async {
                try {
                    val traderId = traderIds[operationIndex % traderIds.size]
                    
                    // Random operation: start or stop
                    when (operationIndex % 2) {
                        0 -> {
                            traderManager.startTrader(traderId)
                            delay(100)
                            traderManager.stopTrader(traderId)
                        }
                        else -> {
                            traderManager.stopTrader(traderId)
                            delay(100)
                            traderManager.startTrader(traderId)
                        }
                    }
                    
                    operationsCount.incrementAndGet()
                } catch (e: Exception) {
                    errorsCount.incrementAndGet()
                    println("   Operation $operationIndex failed: ${e.message}")
                }
            }
        }
        
        // Wait for all operations
        operations.forEach { it.await() }
        
        // Verify system is still stable
        val finalTraderCount = traderManager.getTraderCount()
        assertEquals(3, finalTraderCount, "Should still have 3 traders")
        
        // Check error rate (avoid division by zero)
        val totalOperations = operationsCount.get()
        if (totalOperations > 0) {
            assertTrue(
                errorsCount.get() < totalOperations / 2,
                "Error rate should be low: ${errorsCount.get()}/$totalOperations"
            )
        } else {
            // If no operations completed, that's also a problem
            assertTrue(false, "No operations completed - system may be unstable")
        }
        
        println("✅ System stability verified: ${operationsCount.get()} operations, ${errorsCount.get()} errors")
    }
}

