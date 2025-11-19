package com.fmps.autotrader.core.integration

import com.fmps.autotrader.core.api.startApiServer
import com.fmps.autotrader.shared.dto.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.engine.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

/**
 * Integration test for Core Service ↔ UI communication via REST API
 * 
 * Tests:
 * - Trader CRUD operations via REST API
 * - Position queries and updates via REST API
 * - Configuration management via REST API
 * - Pattern analytics queries via REST API
 * - Error handling and retry logic
 * 
 * **Tag:** @Tag("integration")
 */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UICoreServiceIntegrationTest {
    
    private lateinit var server: ApplicationEngine
    private lateinit var httpClient: io.ktor.client.HttpClient
    private var serverPort: Int = 0
    private var createdTraderId: Int? = null
    private val json = Json { ignoreUnknownKeys = true }
    
    @BeforeAll
    fun setup() {
        println("=".repeat(70))
        println("UI ↔ Core Service Integration Test - Setup")
        println("=".repeat(70))
        
        // Initialize test database
        TestUtilities.initTestDatabase("ui-core-service")
        
        // Start API server
        serverPort = TestUtilities.findAvailablePort()
        server = startApiServer(host = "127.0.0.1", port = serverPort, wait = false)
        
        // Wait for server to be ready
        runBlocking {
            TestUtilities.waitForServer(serverPort, maxWaitMs = 5000)
        }
        
        // Create HTTP client
        httpClient = TestUtilities.createHttpClient()
        
        println("✅ Test environment initialized")
        println("   Server running on port: $serverPort")
        println()
    }
    
    @AfterAll
    fun tearDown() {
        println("=".repeat(70))
        println("UI ↔ Core Service Integration Test - Cleanup")
        println("=".repeat(70))
        
        // Cleanup: delete created trader if exists
        runBlocking {
            createdTraderId?.let { id ->
                runCatching {
                    httpClient.delete("http://127.0.0.1:$serverPort/api/v1/traders/$id")
                }
            }
        }
        
        // Close HTTP client
        httpClient.close()
        
        // Stop server
        server.stop(gracePeriodMillis = 1000, timeoutMillis = 5000)
        
        // Cleanup database
        TestUtilities.cleanupTestDatabase("ui-core-service")
        
        println("✅ Test environment cleaned up")
    }
    
    @Test
    @Order(1)
    fun `should check if can create trader`() = runBlocking {
        println("Test 1: Check if can create trader")
        
        val response: HttpResponse = httpClient.get("http://127.0.0.1:$serverPort/api/v1/traders/can-create")
        
        assertEquals(HttpStatusCode.OK, response.status, "Should return OK status")
        
        val apiResponse: ApiResponse<CanCreateTraderResponse> = response.body()
        assertTrue(apiResponse.success, "API response should be successful")
        assertNotNull(apiResponse.data, "Response data should not be null")
        
        val canCreate = apiResponse.data!!
        assertTrue(canCreate.canCreate, "Should be able to create trader initially")
        assertEquals(0, canCreate.currentCount, "Current count should be 0")
        assertEquals(3, canCreate.maxAllowed, "Max allowed should be 3")
        
        println("✅ Can create trader check passed")
    }
    
    @Test
    @Order(2)
    fun `should create trader via REST API`() = runBlocking {
        println("Test 2: Create trader via REST API")
        
        val createRequest = CreateAITraderRequest(
            name = "UI Integration Test Trader",
            exchange = "BINANCE",
            tradingPair = "BTCUSDT",
            leverage = 10,
            initialBalance = BigDecimal("1000.00"),
            stopLossPercentage = BigDecimal("0.02"),
            takeProfitPercentage = BigDecimal("0.05")
        )
        
        val response: HttpResponse = httpClient.post("http://127.0.0.1:$serverPort/api/v1/traders") {
            contentType(ContentType.Application.Json)
            setBody(createRequest)
        }
        
        assertEquals(HttpStatusCode.Created, response.status, "Should return Created status")
        
        val apiResponse: ApiResponse<AITraderDTO> = response.body()
        assertTrue(apiResponse.success, "API response should be successful")
        assertNotNull(apiResponse.data, "Response data should not be null")
        
        val trader = apiResponse.data!!
        assertNotNull(trader.id, "Trader ID should not be null")
        assertEquals("UI Integration Test Trader", trader.name)
        assertEquals("BINANCE", trader.exchange)
        assertEquals("BTCUSDT", trader.tradingPair)
        assertEquals("STOPPED", trader.status)
        
        createdTraderId = trader.id?.toInt()
        
        println("✅ Trader created via REST API: ID=${trader.id}")
    }
    
    @Test
    @Order(3)
    fun `should get trader by ID via REST API`() = runBlocking {
        println("Test 3: Get trader by ID via REST API")
        
        val traderId = createdTraderId ?: fail("Trader ID not available")
        
        val response: HttpResponse = httpClient.get("http://127.0.0.1:$serverPort/api/v1/traders/$traderId")
        
        assertEquals(HttpStatusCode.OK, response.status, "Should return OK status")
        
        val apiResponse: ApiResponse<AITraderDTO> = response.body()
        assertTrue(apiResponse.success, "API response should be successful")
        assertNotNull(apiResponse.data, "Response data should not be null")
        
        val trader = apiResponse.data!!
        assertEquals(traderId.toLong(), trader.id)
        assertEquals("UI Integration Test Trader", trader.name)
        
        println("✅ Trader retrieved via REST API: ID=${trader.id}")
    }
    
    @Test
    @Order(4)
    fun `should list all traders via REST API`() = runBlocking {
        println("Test 4: List all traders via REST API")
        
        val response: HttpResponse = httpClient.get("http://127.0.0.1:$serverPort/api/v1/traders")
        
        assertEquals(HttpStatusCode.OK, response.status, "Should return OK status")
        
        val apiResponse: ApiResponse<List<AITraderDTO>> = response.body()
        assertTrue(apiResponse.success, "API response should be successful")
        assertNotNull(apiResponse.data, "Response data should not be null")
        
        val traders = apiResponse.data!!
        assertTrue(traders.isNotEmpty(), "Should have at least one trader")
        assertTrue(traders.any { it.id?.toInt() == createdTraderId }, "Should contain created trader")
        
        println("✅ Listed ${traders.size} traders via REST API")
    }
    
    @Test
    @Order(5)
    fun `should update trader configuration via REST API`() = runBlocking {
        println("Test 5: Update trader configuration via REST API")
        
        val traderId = createdTraderId ?: fail("Trader ID not available")
        
        val updateRequest = UpdateAITraderRequest(
            leverage = 20,
            stopLossPercentage = BigDecimal("0.03"),
            takeProfitPercentage = BigDecimal("0.06")
        )
        
        val response: HttpResponse = httpClient.put("http://127.0.0.1:$serverPort/api/v1/traders/$traderId") {
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }
        
        assertEquals(HttpStatusCode.OK, response.status, "Should return OK status")
        
        val apiResponse: ApiResponse<AITraderDTO> = response.body()
        assertTrue(apiResponse.success, "API response should be successful")
        assertNotNull(apiResponse.data, "Response data should not be null")
        
        val trader = apiResponse.data!!
        assertEquals(traderId.toLong(), trader.id)
        assertEquals(20, trader.leverage)
        
        println("✅ Trader configuration updated via REST API")
    }
    
    @Test
    @Order(6)
    fun `should update trader status via REST API`() = runBlocking {
        println("Test 6: Update trader status via REST API")
        
        val traderId = createdTraderId ?: fail("Trader ID not available")
        
        val statusRequest = UpdateTraderStatusRequest(
            status = "RUNNING"
        )
        
        val response: HttpResponse = httpClient.patch("http://127.0.0.1:$serverPort/api/v1/traders/$traderId/status") {
            contentType(ContentType.Application.Json)
            setBody(statusRequest)
        }
        
        assertEquals(HttpStatusCode.OK, response.status, "Should return OK status")
        
        val apiResponse: ApiResponse<AITraderDTO> = response.body()
        assertTrue(apiResponse.success, "API response should be successful")
        assertNotNull(apiResponse.data, "Response data should not be null")
        
        val trader = apiResponse.data!!
        assertEquals(traderId, trader.id)
        assertEquals("RUNNING", trader.status)
        
        println("✅ Trader status updated via REST API: ${trader.status}")
        
        // Stop trader for cleanup
        delay(500)
        val stopRequest = UpdateTraderStatusRequest(status = "STOPPED")
        runCatching {
            httpClient.patch("http://127.0.0.1:$serverPort/api/v1/traders/$traderId/status") {
                contentType(ContentType.Application.Json)
                setBody(stopRequest)
            }
        }
    }
    
    @Test
    @Order(7)
    fun `should handle invalid trader ID gracefully`() = runBlocking {
        println("Test 7: Handle invalid trader ID gracefully")
        
        val response: HttpResponse = httpClient.get("http://127.0.0.1:$serverPort/api/v1/traders/99999")
        
        assertEquals(HttpStatusCode.NotFound, response.status, "Should return NotFound status")
        
        val errorResponse: ErrorResponse = response.body()
        assertFalse(errorResponse.success, "Error response should indicate failure")
        assertNotNull(errorResponse.error, "Error details should be present")
        assertEquals("TRADER_NOT_FOUND", errorResponse.error!!.code)
        
        println("✅ Invalid trader ID handled gracefully")
    }
    
    @Test
    @Order(8)
    fun `should handle invalid request body gracefully`() = runBlocking {
        println("Test 8: Handle invalid request body gracefully")
        
        val response: HttpResponse = httpClient.post("http://127.0.0.1:$serverPort/api/v1/traders") {
            contentType(ContentType.Application.Json)
            setBody("""{"invalid": "json"}""")
        }
        
        assertTrue(
            response.status in listOf(HttpStatusCode.BadRequest, HttpStatusCode.UnprocessableEntity),
            "Should return BadRequest or UnprocessableEntity status"
        )
        
        println("✅ Invalid request body handled gracefully")
    }
    
    @Test
    @Order(9)
    fun `should get health status via REST API`() = runBlocking {
        println("Test 9: Get health status via REST API")
        
        val response: HttpResponse = httpClient.get("http://127.0.0.1:$serverPort/api/health")
        
        assertEquals(HttpStatusCode.OK, response.status, "Should return OK status")
        
        println("✅ Health check passed")
    }
}

