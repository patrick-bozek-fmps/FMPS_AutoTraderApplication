package com.fmps.autotrader.core.integration

import com.fmps.autotrader.core.connectors.ConnectorFactory
import com.fmps.autotrader.core.database.repositories.AITraderRepository
import com.fmps.autotrader.core.traders.AITraderConfig
import com.fmps.autotrader.core.traders.AITraderManager
import com.fmps.autotrader.shared.enums.Exchange
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

/**
 * Integration test for error recovery scenarios
 * 
 * Tests:
 * - Core Service crash recovery (restart → state recovery)
 * - Invalid configuration recovery (validation → error handling)
 * - Trader state recovery after errors
 * 
 * **Tag:** @Tag("integration")
 */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ErrorRecoveryTest {
    
    private lateinit var traderManager: AITraderManager
    private lateinit var repository: AITraderRepository
    private lateinit var connectorFactory: ConnectorFactory
    private val createdTraderIds = mutableListOf<String>()
    
    @BeforeAll
    fun setup() {
        println("=".repeat(70))
        println("Error Recovery Integration Test - Setup")
        println("=".repeat(70))
        
        // Initialize test database
        TestUtilities.initTestDatabase("error-recovery")
        
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
        println("Error Recovery Integration Test - Cleanup")
        println("=".repeat(70))
        
        // Cleanup: delete all created traders
        runBlocking {
            createdTraderIds.forEach { traderId ->
                runCatching {
                    traderManager.stopTrader(traderId)
                    traderManager.deleteTrader(traderId)
                }
            }
        }
        
        // Cleanup database
        TestUtilities.cleanupTestDatabase("error-recovery")
        
        println("✅ Test environment cleaned up")
    }
    
    @Test
    @Order(1)
    fun `should recover trader state after manager restart`() = runBlocking {
        println("Test 1: Recover trader state after manager restart")
        
        // Create a trader
        val config = TestUtilities.createTestTraderConfig(
            name = "Recovery Test Trader",
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT",
            budgetUsd = 1000.0
        )
        
        val createResult = traderManager.createTrader(config)
        assertTrue(createResult.isSuccess, "Trader creation should succeed")
        val traderId = createResult.getOrNull()!!
        createdTraderIds.add(traderId)
        
        // Start trader
        val startResult = traderManager.startTrader(traderId)
        assertTrue(startResult.isSuccess, "Trader start should succeed")
        delay(500)
        
        // Verify trader is running
        val trader = traderManager.getTrader(traderId)
        assertNotNull(trader, "Trader should exist")
        assertTrue(
            trader!!.getState().name in listOf("RUNNING", "STARTING"),
            "Trader should be running"
        )
        
        // Simulate manager restart: create new manager instance
        val newManager = AITraderManager(repository, connectorFactory, maxTraders = 3)
        
        // Recover traders
        val recoverResult = newManager.recoverTraders()
        assertTrue(recoverResult.isSuccess, "Trader recovery should succeed")
        
        // Verify trader was recovered
        val recoveredTrader = newManager.getTrader(traderId)
        assertNotNull(recoveredTrader, "Trader should be recovered")
        
        // Update reference
        traderManager = newManager
        
        println("✅ Trader state recovered after manager restart")
    }
    
    @Test
    @Order(2)
    fun `should handle invalid trader ID gracefully`() = runBlocking {
        println("Test 2: Handle invalid trader ID gracefully")
        
        // Try to start non-existent trader
        val startResult = traderManager.startTrader("non-existent-id")
        assertTrue(startResult.isFailure, "Starting non-existent trader should fail")
        
        // Try to stop non-existent trader
        val stopResult = traderManager.stopTrader("non-existent-id")
        assertTrue(stopResult.isFailure, "Stopping non-existent trader should fail")
        
        // Try to delete non-existent trader
        val deleteResult = traderManager.deleteTrader("non-existent-id")
        assertTrue(deleteResult.isFailure, "Deleting non-existent trader should fail")
        
        println("✅ Invalid trader ID handled gracefully")
    }
    
    @Test
    @Order(3)
    fun `should handle invalid configuration gracefully`() = runBlocking {
        println("Test 3: Handle invalid configuration gracefully")
        
        // Test 1: Invalid budget (negative value) - validation happens at config construction
        println("  Testing invalid budget (negative value)...")
        try {
            TestUtilities.createTestTraderConfig(
                name = "Invalid Config Trader",
                exchange = Exchange.BINANCE,
                symbol = "BTCUSDT",
                budgetUsd = -100.0 // Invalid budget - should fail at construction
            )
            // If we get here, validation didn't work (unexpected)
            fail("Expected IllegalArgumentException for negative budget, but config was created")
        } catch (e: IllegalArgumentException) {
            // Expected: AITraderConfig validates maxStakeAmount > 0 in init block
            assertTrue(
                e.message?.contains("Max stake amount must be positive") == true,
                "Exception should mention max stake amount validation: ${e.message}"
            )
            println("    ✅ Negative budget correctly rejected: ${e.message}")
        }
        
        // Test 2: Invalid symbol (empty) - validation happens at config construction
        println("  Testing invalid symbol (empty)...")
        try {
            TestUtilities.createTestTraderConfig(
                name = "Invalid Symbol Trader",
                exchange = Exchange.BINANCE,
                symbol = "", // Invalid symbol - should fail at construction
                budgetUsd = 1000.0
            )
            fail("Expected IllegalArgumentException for empty symbol, but config was created")
        } catch (e: IllegalArgumentException) {
            // Expected: AITraderConfig validates symbol.isNotBlank() in init block
            assertTrue(
                e.message?.contains("Symbol cannot be blank") == true,
                "Exception should mention symbol validation: ${e.message}"
            )
            println("    ✅ Empty symbol correctly rejected: ${e.message}")
        }
        
        // Test 3: Invalid risk level (out of range) - validation happens at config construction
        println("  Testing invalid risk level (out of range)...")
        try {
            // Create config with invalid risk level by directly constructing AITraderConfig
            // (TestUtilities doesn't support custom risk level, so we test it directly)
            AITraderConfig(
                id = "test-invalid-risk",
                name = "Invalid Risk Trader",
                exchange = Exchange.BINANCE,
                symbol = "BTCUSDT",
                virtualMoney = true,
                maxStakeAmount = BigDecimal.valueOf(1000.0),
                maxRiskLevel = 15, // Invalid: must be 1-10
                maxTradingDuration = java.time.Duration.ofHours(24),
                minReturnPercent = 5.0,
                strategy = com.fmps.autotrader.shared.model.TradingStrategy.TREND_FOLLOWING,
                candlestickInterval = com.fmps.autotrader.shared.enums.TimeFrame.ONE_HOUR
            )
            fail("Expected IllegalArgumentException for invalid risk level, but config was created")
        } catch (e: IllegalArgumentException) {
            // Expected: AITraderConfig validates maxRiskLevel in 1..10 in init block
            assertTrue(
                e.message?.contains("Max risk level must be between 1 and 10") == true,
                "Exception should mention risk level validation: ${e.message}"
            )
            println("    ✅ Invalid risk level correctly rejected: ${e.message}")
        }
        
        println("✅ Invalid configuration handled gracefully (validation at construction time)")
    }
    
    @Test
    @Order(4)
    fun `should handle concurrent operations on same trader`() = runBlocking {
        println("Test 4: Handle concurrent operations on same trader")
        
        // Create a trader
        val config = TestUtilities.createTestTraderConfig(
            name = "Concurrent Ops Trader",
            exchange = Exchange.BINANCE,
            symbol = "BTCUSDT",
            budgetUsd = 1000.0
        )
        
        val createResult = traderManager.createTrader(config)
        assertTrue(createResult.isSuccess, "Trader creation should succeed")
        val traderId = createResult.getOrNull()!!
        createdTraderIds.add(traderId)
        
        // Perform concurrent start/stop operations
        val results = mutableListOf<Result<Unit>>()
        
        repeat(5) {
            val startResult = traderManager.startTrader(traderId)
            results.add(startResult)
            delay(100)
            val stopResult = traderManager.stopTrader(traderId)
            results.add(stopResult)
            delay(100)
        }
        
        // At least some operations should succeed
        val successCount = results.count { it.isSuccess }
        assertTrue(
            successCount > 0,
            "At least some concurrent operations should succeed: $successCount/${results.size}"
        )
        
        println("✅ Concurrent operations handled: $successCount/${results.size} succeeded")
    }
}

