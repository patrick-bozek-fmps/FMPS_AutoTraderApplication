package com.fmps.autotrader.core.integration

import com.fmps.autotrader.core.api.startApiServer
import com.fmps.autotrader.core.database.DatabaseFactory
import com.fmps.autotrader.core.traders.AITraderConfig
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.TimeFrame
import com.fmps.autotrader.shared.model.TradingStrategy
import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.io.File
import java.math.BigDecimal
import java.net.ServerSocket
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * Test utilities and helpers for integration tests
 */
object TestUtilities {
    
    private val portCounter = AtomicInteger(19000)
    
    /**
     * Find an available port for testing
     */
    fun findAvailablePort(): Int {
        return try {
            ServerSocket(0).use { socket ->
                socket.localPort
            }
        } catch (e: Exception) {
            portCounter.getAndIncrement()
        }
    }
    
    /**
     * Create test database configuration
     */
    fun createTestDatabaseConfig(testName: String): com.typesafe.config.Config {
        val testDbFile = File("build/test-db/integration-$testName.db")
        testDbFile.parentFile?.mkdirs()
        if (testDbFile.exists()) {
            testDbFile.delete()
        }
        
        return ConfigFactory.parseString("""
            database {
                driver = "org.sqlite.JDBC"
                url = "jdbc:sqlite:${testDbFile.absolutePath.replace("\\", "/")}"
                hikari {
                    maximumPoolSize = 5
                    minimumIdle = 1
                    connectionTimeout = 10000
                    idleTimeout = 300000
                    maxLifetime = 900000
                    autoCommit = true
                    poolName = "IntegrationTestPool-$testName"
                }
                flyway {
                    baselineOnMigrate = true
                    baselineVersion = "1"
                    locations = ["classpath:db/migration"]
                    validateOnMigrate = true
                    cleanDisabled = false
                }
            }
        """).withFallback(ConfigFactory.load("application-test.conf"))
    }
    
    /**
     * Initialize test database
     */
    fun initTestDatabase(testName: String) {
        val config = createTestDatabaseConfig(testName)
        DatabaseFactory.init(config)
    }
    
    /**
     * Clean up test database
     */
    fun cleanupTestDatabase(testName: String) {
        DatabaseFactory.close()
        val testDbFile = File("build/test-db/integration-$testName.db")
        if (testDbFile.exists()) {
            testDbFile.delete()
        }
    }
    
    /**
     * Start test API server
     */
    fun startTestApiServer(testName: String): ApplicationEngine {
        val port = findAvailablePort()
        val config = createTestDatabaseConfig(testName)
        DatabaseFactory.init(config)
        
        return startApiServer(
            host = "127.0.0.1",
            port = port,
            wait = false
        )
    }
    
    /**
     * Create HTTP client for API testing
     */
    fun createHttpClient(apiKey: String? = null): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                })
            }
        }
    }
    
    /**
     * Create WebSocket client for telemetry testing
     */
    fun createWebSocketClient(apiKey: String? = null): HttpClient {
        return HttpClient(CIO) {
            install(WebSockets)
        }
    }
    
    /**
     * Wait for server to be ready
     */
    suspend fun waitForServer(port: Int, maxWaitMs: Long = 5000) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            try {
                val client = HttpClient(CIO)
                val response: io.ktor.client.statement.HttpResponse = client.get("http://127.0.0.1:$port/api/health")
                val statusValue = response.status.value
                client.close()
                if (statusValue in 200..299) {
                    return
                }
            } catch (e: Exception) {
                // Server not ready yet
            }
            delay(100)
        }
        throw IllegalStateException("Server did not become ready within $maxWaitMs ms")
    }
    
    /**
     * Create test trader configuration
     */
    fun createTestTraderConfig(
        name: String = "Test Trader",
        exchange: Exchange = Exchange.BINANCE,
        symbol: String = "BTCUSDT",
        budgetUsd: Double = 1000.0
    ): AITraderConfig {
        return AITraderConfig(
            id = "test-${System.currentTimeMillis()}",
            name = name,
            exchange = exchange,
            symbol = symbol,
            virtualMoney = true,
            maxStakeAmount = BigDecimal.valueOf(budgetUsd),
            maxRiskLevel = 5,
            maxTradingDuration = Duration.ofHours(24),
            minReturnPercent = 5.0,
            strategy = TradingStrategy.TREND_FOLLOWING,
            candlestickInterval = TimeFrame.ONE_HOUR
        )
    }
    
    /**
     * Check if exchange API keys are available
     */
    fun areExchangeApiKeysAvailable(exchange: Exchange): Boolean {
        return when (exchange) {
            Exchange.BINANCE -> {
                val key = System.getenv("BINANCE_API_KEY")
                val secret = System.getenv("BINANCE_API_SECRET")
                !key.isNullOrBlank() && !secret.isNullOrBlank()
            }
            Exchange.BITGET -> {
                val key = System.getenv("BITGET_API_KEY")
                val secret = System.getenv("BITGET_API_SECRET")
                val passphrase = System.getenv("BITGET_API_PASSPHRASE")
                !key.isNullOrBlank() && !secret.isNullOrBlank() && !passphrase.isNullOrBlank()
            }
            else -> false
        }
    }
    
    /**
     * Retry helper for flaky operations
     */
    suspend fun <T> retry(
        maxAttempts: Int = 3,
        delayMs: Long = 1000,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxAttempts - 1) {
                    delay(delayMs * (attempt + 1))
                }
            }
        }
        throw lastException ?: IllegalStateException("Retry failed")
    }
}

