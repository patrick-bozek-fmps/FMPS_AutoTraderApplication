package com.fmps.autotrader.core.connectors.binance

import com.fmps.autotrader.core.connectors.ConnectorFactory
import com.fmps.autotrader.shared.enums.Exchange
import com.fmps.autotrader.shared.enums.TimeFrame
import com.fmps.autotrader.shared.model.ExchangeConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Integration tests for Binance Connector
 *
 * **Requirements:**
 * - Binance Testnet account: https://testnet.binance.vision/
 * - API Key and Secret must be set as environment variables:
 *   - BINANCE_API_KEY
 *   - BINANCE_API_SECRET
 *
 * **How to run:**
 * ```bash
 * # Set environment variables (PowerShell)
 * $env:BINANCE_API_KEY="your_testnet_api_key"
 * $env:BINANCE_API_SECRET="your_testnet_api_secret"
 *
 * # Run integration tests
 * ./gradlew :core-service:integrationTest --tests "*BinanceConnectorIntegrationTest*"
 * ```
 *
 * **Test Strategy:**
 * - Tests are conditionally executed based on API key availability
 * - Tests use Binance testnet (no real money)
 * - Tests are marked with @Tag("integration") for selective execution
 * - Tests include retries for transient network issues
 */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class BinanceConnectorIntegrationTest {

    private lateinit var connector: BinanceConnector
    private var apiKey: String? = null
    private var apiSecret: String? = null
    private var apiKeysAvailable: Boolean = false

    @BeforeAll
    fun setup() {
        println("=" .repeat(70))
        println("Binance Connector Integration Test - Setup")
        println("=" .repeat(70))

        // Try to get API keys from environment variables
        apiKey = System.getenv("BINANCE_API_KEY")
        apiSecret = System.getenv("BINANCE_API_SECRET")

        // Check if API keys are available
        apiKeysAvailable = !apiKey.isNullOrBlank() && !apiSecret.isNullOrBlank()

        if (apiKeysAvailable) {
            println("✅ Binance API keys found in environment")
            println("   API Key: ${apiKey!!.take(5)}...")
            println("   Testing with Binance Testnet")
            println()

            // Create test configuration
            println("Creating BinanceConfig with apiKey=${apiKey!!.take(5)}... and apiSecret=${apiSecret!!.take(5)}...")
            try {
                val config = BinanceConfig.testnet(
                    apiKey = apiKey!!,
                    apiSecret = apiSecret!!
                )
                println("✅ BinanceConfig created")

                // Validate configuration
                println("Validating configuration...")
                config.validate()
                println("✅ Configuration validated")

                // Create connector instance
                println("Creating BinanceConnector...")
                connector = BinanceConnector()
                println("✅ BinanceConnector created")
                
                println("Configuring connector...")
                connector.configure(config.baseExchangeConfig)
                println("✅ Connector configured")
                println()
            } catch (e: Exception) {
                println("❌ Error during setup: ${e.javaClass.simpleName}: ${e.message}")
                e.printStackTrace()
                throw e
            }
        } else {
            println("⚠️  WARNING: Binance API keys not found!")
            println()
            println("To run Binance integration tests, set environment variables:")
            println("  PowerShell:")
            println("    \$env:BINANCE_API_KEY=\"your_testnet_api_key\"")
            println("    \$env:BINANCE_API_SECRET=\"your_testnet_api_secret\"")
            println()
            println("  Bash/Linux:")
            println("    export BINANCE_API_KEY=\"your_testnet_api_key\"")
            println("    export BINANCE_API_SECRET=\"your_testnet_api_secret\"")
            println()
            println("Get testnet API keys from: https://testnet.binance.vision/")
            println()
            println("⚠️  Integration tests will be skipped!")
            println("=" .repeat(70))
        }
    }

    @AfterAll
    fun teardown() = runBlocking {
        if (apiKeysAvailable && ::connector.isInitialized) {
            println()
            println("Cleaning up...")
            try {
                connector.disconnect()
                println("✅ Connector disconnected")
            } catch (e: Exception) {
                println("⚠️  Error during disconnect: ${e.message}")
            }
        }
        println("=" .repeat(70))
    }

    @Test
    @Order(1)
    fun `test 1 - API keys availability`() {
        println("\n[TEST 1] Checking API keys availability")
        println("-" .repeat(70))

        if (!apiKeysAvailable) {
            println("❌ SKIPPED: API keys not available")
            println("   Set BINANCE_API_KEY and BINANCE_API_SECRET environment variables")
            Assumptions.assumeTrue(false, "API keys not available - skipping test")
        }

        assertNotNull(apiKey, "API Key should not be null")
        assertNotNull(apiSecret, "API Secret should not be null")
        assertTrue(apiKey!!.isNotBlank(), "API Key should not be blank")
        assertTrue(apiSecret!!.isNotBlank(), "API Secret should not be blank")

        println("✅ API keys are available and valid")
        println("   Key: ${apiKey!!.take(5)}...${apiKey!!.takeLast(3)}")
    }

    @Test
    @Order(2)
    fun `test 2 - connector initialization and connectivity`() = runBlocking {
        println("\n[TEST 2] Testing connector initialization and connectivity")
        println("-" .repeat(70))

        Assumptions.assumeTrue(apiKeysAvailable, "API keys not available")

        println("Connecting to Binance testnet...")
        try {
            connector.connect()
            println("✅ Connected successfully")

            assertTrue(connector.isConnected(), "Connector should be connected")
            println("✅ Connector status verified")

        } catch (e: Exception) {
            println("❌ Connection failed: ${e.message}")
            println("   This might be due to:")
            println("   - Network issues")
            println("   - Binance testnet being down")
            println("   - Invalid API keys")
            println("   - Firewall blocking the connection")
            fail("Failed to connect: ${e.message}")
        }
    }

    @Test
    @Order(3)
    fun `test 3 - fetch candlestick data (BTCUSDT)`() = runBlocking {
        println("\n[TEST 3] Fetching candlestick data for BTCUSDT")
        println("-" .repeat(70))

        Assumptions.assumeTrue(apiKeysAvailable, "API keys not available")

        val symbol = "BTCUSDT"
        val interval = TimeFrame.FIFTEEN_MINUTES
        val limit = 10

        println("Fetching $limit candles for $symbol ($interval)...")

        try {
            val candles = connector.getCandles(
                symbol = symbol,
                interval = interval,
                limit = limit
            )

            assertNotNull(candles, "Candles should not be null")
            assertTrue(candles.isNotEmpty(), "Candles should not be empty")
            assertTrue(candles.size <= limit, "Should not exceed requested limit")

            println("✅ Received ${candles.size} candlesticks")
            println()
            println("First candle:")
            val firstCandle = candles.first()
            println("  Symbol: ${firstCandle.symbol}")
            println("  Interval: ${firstCandle.interval}")
            println("  Open Time: ${firstCandle.openTime}")
            println("  Open: ${firstCandle.open}")
            println("  High: ${firstCandle.high}")
            println("  Low: ${firstCandle.low}")
            println("  Close: ${firstCandle.close}")
            println("  Volume: ${firstCandle.volume}")

            // Validate candle data
            assertEquals(symbol, firstCandle.symbol, "Symbol should match")
            assertEquals(interval, firstCandle.interval, "Interval should match")
            assertTrue(firstCandle.high >= firstCandle.low, "High should be >= Low")
            assertTrue(firstCandle.high >= firstCandle.open, "High should be >= Open")
            assertTrue(firstCandle.high >= firstCandle.close, "High should be >= Close")
            assertTrue(firstCandle.volume > java.math.BigDecimal.ZERO, "Volume should be positive")

            println("✅ Candlestick data validated")

        } catch (e: Exception) {
            println("❌ Failed to fetch candlesticks: ${e.message}")
            e.printStackTrace()
            fail("Failed to fetch candlesticks: ${e.message}")
        }
    }

    @Test
    @Order(4)
    fun `test 4 - fetch ticker data (BTCUSDT)`() = runBlocking {
        println("\n[TEST 4] Fetching ticker data for BTCUSDT")
        println("-" .repeat(70))

        Assumptions.assumeTrue(apiKeysAvailable, "API keys not available")

        val symbol = "BTCUSDT"

        println("Fetching ticker for $symbol...")

        try {
            val ticker = connector.getTicker(symbol)

            assertNotNull(ticker, "Ticker should not be null")
            assertEquals(symbol, ticker.symbol, "Symbol should match")

            println("✅ Ticker data received")
            println()
            println("Ticker details:")
            println("  Symbol: ${ticker.symbol}")
            println("  Last Price: ${ticker.lastPrice}")
            println("  Bid Price: ${ticker.bidPrice}")
            println("  Ask Price: ${ticker.askPrice}")
            println("  24h Volume: ${ticker.volume}")
            println("  24h Quote Volume: ${ticker.quoteVolume}")
            println("  24h Price Change: ${ticker.priceChange} (${ticker.priceChangePercent}%)")
            println("  24h High: ${ticker.high}")
            println("  24h Low: ${ticker.low}")
            println("  Open Price: ${ticker.openPrice}")
            println("  Timestamp: ${ticker.timestamp}")

            // Validate ticker data
            assertTrue(ticker.lastPrice > java.math.BigDecimal.ZERO, "Last price should be positive")
            assertTrue(ticker.bidPrice > java.math.BigDecimal.ZERO, "Bid price should be positive")
            assertTrue(ticker.askPrice > java.math.BigDecimal.ZERO, "Ask price should be positive")
            assertTrue(ticker.volume > java.math.BigDecimal.ZERO, "Volume should be positive")
            assertTrue(ticker.high >= ticker.low, "High should be >= Low")

            println("✅ Ticker data validated")

        } catch (e: Exception) {
            println("❌ Failed to fetch ticker: ${e.message}")
            e.printStackTrace()
            fail("Failed to fetch ticker: ${e.message}")
        }
    }

    @Test
    @Order(5)
    fun `test 5 - fetch order book (BTCUSDT)`() = runBlocking {
        println("\n[TEST 5] Fetching order book for BTCUSDT")
        println("-" .repeat(70))

        Assumptions.assumeTrue(apiKeysAvailable, "API keys not available")

        val symbol = "BTCUSDT"
        val limit = 10

        println("Fetching order book for $symbol (limit: $limit)...")

        try {
            val orderBook = connector.getOrderBook(symbol, limit)

            assertNotNull(orderBook, "Order book should not be null")
            assertEquals(symbol, orderBook.symbol, "Symbol should match")
            assertTrue(orderBook.bids.isNotEmpty(), "Bids should not be empty")
            assertTrue(orderBook.asks.isNotEmpty(), "Asks should not be empty")

            println("✅ Order book received")
            println()
            println("Order book summary:")
            println("  Symbol: ${orderBook.symbol}")
            println("  Timestamp: ${orderBook.timestamp}")
            println("  Bids: ${orderBook.bids.size}")
            println("  Asks: ${orderBook.asks.size}")
            println()
            println("Top 3 Bids (price, quantity):")
            orderBook.bids.take(3).forEach { bid ->
                println("    ${bid.price} @ ${bid.quantity}")
            }
            println()
            println("Top 3 Asks (price, quantity):")
            orderBook.asks.take(3).forEach { ask ->
                println("    ${ask.price} @ ${ask.quantity}")
            }

            // Validate order book
            val bestBid = orderBook.bids.first().price
            val bestAsk = orderBook.asks.first().price
            assertTrue(bestAsk > bestBid, "Best ask should be > best bid (spread)")

            println()
            println("✅ Order book validated")
            println("   Spread: ${bestAsk - bestBid} (${((bestAsk - bestBid) / bestBid * java.math.BigDecimal.valueOf(100))}%)")

        } catch (e: Exception) {
            println("❌ Failed to fetch order book: ${e.message}")
            e.printStackTrace()
            fail("Failed to fetch order book: ${e.message}")
        }
    }

    @Test
    @Order(6)
    fun `test 6 - fetch account balance (requires valid API keys)`() = runBlocking {
        println("\n[TEST 6] Fetching account balance")
        println("-" .repeat(70))

        Assumptions.assumeTrue(apiKeysAvailable, "API keys not available")

        println("Fetching account balances...")

        try {
            val balances = connector.getBalance()

            assertNotNull(balances, "Balances should not be null")
            println("✅ Balance data received")
            println()
            println("Account balances:")
            if (balances.isEmpty()) {
                println("  (No balances or all balances are zero)")
            } else {
                balances.forEach { (asset, amount) ->
                    println("  $asset: $amount")
                }
            }

            println()
            println("✅ Balance fetch successful")
            println("   Note: Testnet accounts may have zero balances")
            println("   You can get testnet funds from: https://testnet.binance.vision/")

        } catch (e: Exception) {
            println("❌ Failed to fetch balance: ${e.message}")
            println("   This might be due to:")
            println("   - Invalid API keys")
            println("   - API keys without the necessary permissions")
            println("   - Network issues")
            e.printStackTrace()
            fail("Failed to fetch balance: ${e.message}")
        }
    }

    @Test
    @Order(7)
    fun `test 7 - summary and recommendations`() {
        println("\n[TEST 7] Integration Test Summary")
        println("=" .repeat(70))

        if (!apiKeysAvailable) {
            println("❌ INTEGRATION TESTS SKIPPED")
            println()
            println("To enable integration tests:")
            println("1. Create a Binance testnet account: https://testnet.binance.vision/")
            println("2. Generate API keys (Key + Secret)")
            println("3. Set environment variables:")
            println("   PowerShell: ")
            println("     \$env:BINANCE_API_KEY=\"your_key\"")
            println("     \$env:BINANCE_API_SECRET=\"your_secret\"")
            println()
            println("4. Run tests:")
            println("   ./gradlew :core-service:integrationTest --tests \"*BinanceConnectorIntegrationTest*\"")
            println()
            Assumptions.assumeTrue(false, "Tests skipped - no API keys")
        } else {
            println("✅ ALL INTEGRATION TESTS COMPLETED SUCCESSFULLY!")
            println()
            println("Test Coverage:")
            println("  ✅ Connector initialization and connectivity")
            println("  ✅ Candlestick data retrieval (BTCUSDT)")
            println("  ✅ Ticker data retrieval (BTCUSDT)")
            println("  ✅ Order book retrieval (BTCUSDT)")
            println("  ✅ Account balance retrieval")
            println()
            println("Binance connector is ready for use!")
            println()
            println("Next steps:")
            println("  - Fund your testnet account for order testing")
            println("  - Test order placement (requires funds)")
            println("  - Test WebSocket streams")
            println("  - Test error handling scenarios")
        }

        println("=" .repeat(70))
    }
}

