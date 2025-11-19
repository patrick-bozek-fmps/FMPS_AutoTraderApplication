# 🧪 FMPS AutoTrader - Testing Guide

> **Last Updated:** November 11, 2025  
> **Version:** 1.1

## Table of Contents
1. [Overview](#overview)
2. [Test Structure](#test-structure)
3. [Running Tests](#running-tests)
4. [Writing Tests](#writing-tests)
5. [CI/CD Integration](#cicd-integration)
6. [Best Practices](#best-practices)

---

## Overview

### Testing Philosophy
- **Test-Driven Development (TDD)**: Write tests before implementation when possible
- **High Coverage**: Minimum 80% overall, 90% for critical components
- **Fast Feedback**: Unit tests should run in seconds, not minutes
- **Realistic Integration**: Test with actual exchange testnets where possible

### Test Types
1. **Unit Tests** - Test individual components in isolation
2. **Integration Tests** - Test component interactions
3. **E2E Tests** - Test complete workflows
4. **Performance Tests** - Test under load

---

## Test Structure

```
Application_OnPremises/
├── shared/
│   └── src/test/kotlin/                    # Shared model tests
│
├── core-service/
│   ├── src/test/kotlin/                    # Unit tests
│   │   ├── traders/                        # Trading logic tests
│   │   ├── connectors/                     # Connector unit tests
│   │   ├── indicators/                     # Technical indicator tests
│   │   └── patterns/                       # Pattern matching tests
│   │
│   └── src/integrationTest/kotlin/         # Integration tests
│       ├── connectors/                     # Exchange API integration
│       └── database/                       # Database integration
│
└── desktop-ui/
    ├── src/test/kotlin/                    # UI unit tests
    └── src/e2eTest/kotlin/                 # UI E2E tests
```

---

## Running Tests

### Local Execution

#### Run all unit tests:
```bash
cd 03_Development/Application_OnPremises
./gradlew test
```

#### Run integration tests:
```bash
# Run all integration tests
./gradlew :core-service:integrationTest

# Or use the shorthand (if configured)
./gradlew integrationTest
```

**Note**: Integration tests are in a separate `integrationTest` source set and use the `@integration` tag for organization. They require exchange API keys (optional - tests skip gracefully if not configured).

#### Run all tests:
```bash
./gradlew testAll
```

#### Run tests for specific module:
```bash
./gradlew :core-service:test
./gradlew :desktop-ui:test
```

#### Run single test class:
```bash
./gradlew test --tests "AITraderTest"
```

> **Update (Nov 7, 2025):** `AITraderTest` now includes configuration update and performance metrics coverage (`test updateConfig rejects id changes`, `test recordTradeResult updates winning/losing metrics`). Run the class after trader changes to ensure regressions surface locally before pushing.

#### Run Risk Manager suite:
```bash
./gradlew test --tests "RiskManagerTest"
```

> **Update (Nov 7, 2025):** `RiskManagerTest` validates budget/leverage enforcement, exposure calculations, stop-loss execution, and emergency-stop flows. Execute this class whenever adjusting risk thresholds, PositionManager integration, or RiskManager logic.

> **Update (Nov 7, 2025 - PM):** Additional concurrency + end-to-end tests (`emergency stop is idempotent under concurrent calls`, `end to end risk flow closes positions and notifies handlers`) ensure stop-loss execution remains thread-safe and emits trader stop notifications. Re-run the suite after modifying monitoring or emergency-stop plumbing.

> **Update (Nov 7, 2025 - Evening):** `PositionManagerTest` now asserts trailing-stop adjustments and persistence failure handling (`test trailing stop adjusts with price`, `test close position fails when repository close fails`). Run these after modifying stop-loss logic or database interactions.

> **Update (Nov 11, 2025):** `ApiSecurityTest` validates API key enforcement (missing/invalid/valid) and `/metrics` protection. Run after touching Ktor interceptors, security configuration, or monitoring plumbing. `TradeRepositoryTest` also includes pagination coverage (`should return paginated trades`)—rerun whenever modifying repository filters or DTO mapping.

#### Generate coverage report (core-service):
```bash
./gradlew :core-service:jacocoTestReport
```

- HTML report: `core-service/build/reports/jacoco/test/html/index.html`
- Snapshot (Nov 11, 2025): Core-service aggregate ≥ 80 % lines; API module retains coverage via new `ApiSecurityTest` and pagination assertions. Use Jacoco HTML for precise module breakdown before/after major changes.

#### Run single test method:
```bash
./gradlew test --tests "AITraderTest.testStartTrading"
```

### Coverage Reports

#### Generate coverage report:
```bash
./gradlew jacocoTestReport
```

#### View report:
```bash
# Open in browser
start build/reports/jacoco/test/html/index.html   # Windows
open build/reports/jacoco/test/html/index.html    # macOS
```

#### Enforce coverage threshold:
```bash
./gradlew jacocoTestCoverageVerification
```

### Continuous Testing

#### Watch mode (runs tests on file change):
```bash
./gradlew test --continuous
```

---

## Writing Tests

### 1. Unit Test Example

```kotlin
@DisplayName("AI Trader Tests")
class AITraderTest {
    
    private lateinit var aiTrader: AITrader
    private lateinit var mockConnector: IExchangeConnector
    
    @BeforeEach
    fun setup() {
        mockConnector = mockk()
        aiTrader = AITrader(
            config = AITraderConfig(...),
            exchangeConnector = mockConnector
        )
    }
    
    @Test
    @DisplayName("Should start trading when conditions are met")
    fun testStartTrading() = runTest {
        // Arrange
        every { mockConnector.isConnected() } returns true
        
        // Act
        val result = aiTrader.start()
        
        // Assert
        assertTrue(result.isSuccess)
        assertEquals(AITraderState.RUNNING, aiTrader.getState())
    }
}
```

### 2. Integration Test Example

```kotlin
@DisplayName("Binance Connector Integration Tests")
@Tag("integration")
class BinanceConnectorIntegrationTest {
    
    private lateinit var connector: BinanceConnector
    
    @BeforeEach
    fun setup() {
        connector = BinanceConnector(
            apiKey = System.getenv("BINANCE_TESTNET_API_KEY"),
            apiSecret = System.getenv("BINANCE_TESTNET_API_SECRET"),
            baseUrl = "https://testnet.binance.vision"
        )
    }
    
    @Test
    @DisplayName("Should fetch candlestick data from testnet")
    fun testGetCandlesticks() = runTest {
        // Act
        val result = connector.getCandlesticks("BTCUSDT", "1h", 10)
        
        // Assert
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.size <= 10)
    }
}
```

### 3. Parameterized Test Example

```kotlin
@ParameterizedTest
@ValueSource(doubles = [100.0, 500.0, 1000.0])
@DisplayName("Should handle different budget amounts")
fun testDifferentBudgets(budget: Double) {
    val trader = createTrader(budget)
    assertEquals(budget, trader.config.budget)
}
```

### 4. Test Fixtures

```kotlin
companion object {
    @JvmStatic
    fun provideMarketConditions(): Stream<Arguments> = Stream.of(
        Arguments.of("oversold", 25.0, "BUY"),
        Arguments.of("overbought", 75.0, "SELL"),
        Arguments.of("neutral", 50.0, "HOLD")
    )
}

@ParameterizedTest
@MethodSource("provideMarketConditions")
fun testTradingDecisions(condition: String, rsi: Double, expectedSignal: String) {
    val signal = aiTrader.analyzeRSI(rsi)
    assertEquals(expectedSignal, signal)
}
```

---

## CI/CD Integration

### GitHub Actions Workflow

Every commit triggers:
1. ✅ Build all modules
2. ✅ Run all unit tests (excluding `@integration` tag)
   - Uses: `./gradlew test --no-daemon -Djunit.jupiter.excludeTags=integration`
   - Fast execution (~1-2 minutes)
3. ✅ Run integration tests (if core-service code changed AND API keys configured)
   - Uses: `./gradlew :core-service:integrationTest --no-daemon`
   - Tests are in separate `integrationTest` source set
   - Only runs if exchange API keys are configured in GitHub secrets
   - If API keys not configured, integration tests are skipped (workflow still passes)
   - **Targeted Testing**: Integration tests only run when core-service code changes to save CI time
4. ✅ Generate coverage report
5. ✅ Enforce 80% minimum coverage
6. ✅ Code style check (ktlint)
7. ✅ Static analysis (detekt)
8. ✅ Security scan (OWASP Dependency Check)

### Integration Test Execution in CI

**When Integration Tests Run**:
- Core service code changes (automatic)
- Force integration tests flag set (manual trigger)
- API keys configured in GitHub secrets (required)

**When Integration Tests Are Skipped**:
- Only UI or shared module changes (targeted testing saves time)
- API keys not configured (expected - tests skip gracefully)
- Workflow still passes ✅ (integration tests are optional)

**Why Tags Are Used**:
- **Organization**: Tags (`@integration`) organize tests by type
- **Selective Execution**: Allows running specific test subsets
- **Time Savings**: When changes don't impact complete software, only relevant tests run
- **CI Efficiency**: Integration tests only run when core-service code changes

### PR Checks

All pull requests must:
- ✅ Pass all tests
- ✅ Meet coverage threshold (80%)
- ✅ Pass code style checks
- ✅ Pass static analysis
- ✅ Have no HIGH/CRITICAL security vulnerabilities

### Viewing CI Results

1. Go to your PR on GitHub
2. Scroll to "Checks" section
3. Click on "Build and Test" to see detailed results
4. Click on "Test Results" to see individual test outcomes
5. Download artifacts for detailed reports

---

## Best Practices

### Test Naming

**Good:**
```kotlin
@Test
@DisplayName("Should calculate RSI correctly for known values")
fun testRSICalculation()

@Test
@DisplayName("Should throw exception when insufficient data provided")
fun testInsufficientData()
```

**Bad:**
```kotlin
@Test
fun test1()  // Not descriptive

@Test
fun testRSI()  // Too vague
```

### Arrange-Act-Assert Pattern

```kotlin
@Test
fun testStartTrading() {
    // Arrange: Set up test conditions
    val trader = createTrader()
    every { mockConnector.isConnected() } returns true
    
    // Act: Execute the action
    trader.start()
    
    // Assert: Verify the outcome
    assertEquals(TraderState.RUNNING, trader.state)
}
```

### Mock vs Real Dependencies

**Mock for:**
- External APIs (use testnet for integration tests)
- Database (use H2 in-memory for unit tests)
- File system
- Time/Clock

**Real for:**
- Business logic
- Calculations (RSI, MACD, etc.)
- Data transformations
- State machines

### Test Data

```kotlin
// Good: Create test data builders
fun createTestTrader(
    id: String = "test-trader",
    budget: Double = 1000.0,
    state: TraderState = TraderState.IDLE
): AITrader {
    return AITrader(id, createTestConfig(budget), mockConnector)
}

// Good: Use realistic test data
val testCandles = listOf(
    Candlestick(1000L, 50000.0, 51000.0, 49000.0, 50500.0, 100.0, 2000L)
)

// Bad: Use magic numbers
val price = 12345.6789  // What does this represent?
```

### Coverage Goals

| Component | Minimum Coverage | Target |
|-----------|------------------|---------|
| Risk Management | 90% | 95% |
| Position Management | 90% | 95% |
| Trade Execution | 90% | 95% |
| Technical Indicators | 85% | 90% |
| Exchange Connectors | 85% | 90% |
| API Endpoints | 85% | 90% |
| Pattern Storage | 80% | 85% |
| UI ViewModels | 75% | 80% |
| UI Components | 70% | 75% |

### What NOT to Test

- **Framework code** - Don't test Ktor, JavaFX, etc.
- **Getters/Setters** - Simple property access
- **Generated code** - Serialization, etc.
- **Configuration classes** - Unless they have logic
- **Main entry points** - Unless critical startup logic

### Flaky Tests

**Avoid:**
- Tests dependent on timing/delays
- Tests dependent on external services (except dedicated integration tests)
- Tests dependent on execution order
- Tests with random data without seed

**Fix flaky tests immediately** - They reduce trust in the suite.

### Test Cleanup and Isolation

**Critical:** Tests must be isolated and clean up after themselves.

#### Database Test Setup Pattern

All database tests should follow this pattern:

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyRepositoryTest {
    
    private val testDbPath = "./build/test-db/test-my-repo.db"
    private lateinit var repository: MyRepository
    
    @BeforeAll
    fun setup() {
        // Delete existing test database
        File(testDbPath).delete()
        
        // Set system properties for test database
        System.setProperty("database.url", "jdbc:sqlite:$testDbPath")
        System.setProperty("app.environment", "test")
        
        // Initialize database
        val config = ConfigFactory.load()
        DatabaseFactory.init(config)
        
        repository = MyRepository()
    }
    
    @AfterAll
    fun teardown() {
        DatabaseFactory.close()
        File(testDbPath).delete()
    }
    
    @Test
    fun `should do something`() = runBlocking {
        // Test implementation
    }
}
```

**Key Points:**
- Use `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` for shared setup
- Each test class gets its own database file: `./build/test-db/test-{name}.db`
- Always delete database file in `@BeforeAll` and `@AfterAll`
- Close `DatabaseFactory` in `@AfterAll`

#### Manager/Service Test Cleanup Pattern

When testing managers or services that maintain in-memory state:

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AITraderManagerTest {
    
    private lateinit var repository: AITraderRepository
    private lateinit var manager: AITraderManager
    
    @BeforeEach
    fun clearTraders() = runBlocking {
        // CRITICAL: Clean both database AND in-memory state
        val dbTraders = repository.findAll()
        dbTraders.forEach { dbTrader ->
            val traderId = dbTrader.id.toString()
            // Stop trader if running (ignore errors)
            manager.stopTrader(traderId).getOrNull()
            // Delete through manager to clean both DB and activeTraders map
            manager.deleteTrader(traderId).getOrNull()
        }
        // Final cleanup: clear any remaining database records
        val remainingDbTraders = repository.findAll()
        remainingDbTraders.forEach { repository.delete(it.id) }
    }
}
```

**Common Pitfall:** Only cleaning the database but not the manager's in-memory state will cause test failures (e.g., `MaxTradersExceededException`).

**Rule:** If a manager maintains in-memory state (like `activeTraders` map), always delete through the manager, not just the repository.

#### Coroutine Testing

**Use `runBlocking` for:**
- Repository tests (database operations)
- Tests that need to wait for completion
- Integration tests

**Use `runTest` for:**
- Unit tests with coroutines
- Tests that need virtual time control
- Tests with delays/timeouts

```kotlin
// Repository test - use runBlocking
@Test
fun `should create trader`() = runBlocking {
    val traderId = repository.create(...)
    assertNotNull(traderId)
}

// Unit test with coroutines - use runTest
@Test
fun `should generate signal`() = runTest {
    val signal = signalGenerator.generateSignal(...)
    assertNotNull(signal)
}
```

---

## Integration Test Environment

### Exchange Testnets

**Binance Testnet:**
- URL: `https://testnet.binance.vision`
- API Key: Set in environment variable `BINANCE_TESTNET_API_KEY`
- Secret: Set in environment variable `BINANCE_TESTNET_API_SECRET`

**Bitget Testnet:**
- URL: `https://api.bitget.com/api/spot/v1` (demo mode)
- API Key: Set in environment variable `BITGET_TESTNET_API_KEY`
- Secret: Set in environment variable `BITGET_TESTNET_API_SECRET`

### Database

**Unit Tests:**
- Use SQLite file database (`./build/test-db/test-{name}.db`)
- Each test class gets its own database file
- Database is deleted and recreated in `@BeforeAll` and `@AfterAll`
- Schema is created via Flyway migrations

**Integration Tests:**
- Use SQLite file database (same pattern as unit tests)
- Reset before each test via `@BeforeEach` cleanup

---

## Debugging Tests

### Run with Detailed Output

```bash
./gradlew test --info
```

### Run Single Test in Debug Mode

```bash
./gradlew test --tests "AITraderTest" --debug-jvm
```

Then attach your IDE debugger to port 5005.

### View Test Output

```bash
cat build/test-results/test/TEST-AITraderTest.xml
```

---

## Performance Testing

### Load Test Example

```kotlin
@Test
@DisplayName("Should handle 3 concurrent traders")
fun testConcurrentTraders() = runTest {
    val traders = (1..3).map { createTrader("trader-$it") }
    
    // Start all traders concurrently
    val jobs = traders.map { trader ->
        launch { trader.start() }
    }
    
    // Wait for all to complete
    jobs.joinAll()
    
    // Assert all are running
    traders.forEach {
        assertEquals(TraderState.RUNNING, it.state)
    }
}
```

---

## Troubleshooting

### Tests Failing Locally

1. **Clean build:**
   ```bash
   ./gradlew clean test
   ```

2. **Check dependencies:**
   ```bash
   ./gradlew dependencies
   ```

3. **Verify environment variables:**
   ```bash
   echo $BINANCE_TESTNET_API_KEY
   ```

### Tests Passing Locally but Failing in CI

1. Check for timing dependencies
2. Verify all test data is committed
3. Check for environment-specific assumptions
4. Review CI logs for specific errors
5. **Check test isolation** - Ensure tests clean up properly (both database and in-memory state)

### Test Isolation Failures

**Symptom:** Tests pass individually but fail when run together.

**Common Causes:**
1. **Manager state not cleaned** - Manager maintains in-memory state that persists between tests
   - **Fix:** Delete through manager, not just repository
   - **Example:** `AITraderManagerTest` must use `manager.deleteTrader()` not just `repository.delete()`

2. **Database not cleaned between tests**
   - **Fix:** Use `@BeforeEach` to clear test data
   - **Example:** Clear all traders before each test in `AITraderManagerTest`

3. **Shared mutable state**
   - **Fix:** Create fresh instances in `@BeforeEach` or use `@TestInstance(TestInstance.Lifecycle.PER_METHOD)`

**Debugging:**
```bash
# Run tests in isolation to identify which test is causing issues
./gradlew test --tests "AITraderManagerTest.testCreateTrader"
./gradlew test --tests "AITraderManagerTest.testCreateTraderEnforcesMaxLimit"
```

### Coverage Not Meeting Threshold

1. Run coverage report: `./gradlew jacocoTestReport`
2. Open HTML report to identify gaps
3. Focus on critical areas first
4. Add tests for uncovered branches

---

## Resources

- **JUnit 5**: https://junit.org/junit5/docs/current/user-guide/
- **Mockk**: https://mockk.io/
- **Kotest**: https://kotest.io/
- **Ktor Testing**: https://ktor.io/docs/testing.html
- **TestFX**: https://github.com/TestFX/TestFX

---

## Quick Reference

| Task | Command |
|------|---------|
| Run all unit tests | `./gradlew test` |
| Run integration tests | `./gradlew :core-service:integrationTest` |
| Run all tests | `./gradlew testAll` |
| Generate coverage | `./gradlew jacocoTestReport` |
| Check coverage threshold | `./gradlew jacocoTestCoverageVerification` |
| Run specific test | `./gradlew test --tests "ClassName"` |
| Run with continuous | `./gradlew test --continuous` |
| Clean and test | `./gradlew clean test` |
| Code style check | `./gradlew ktlintCheck` |
| Static analysis | `./gradlew detekt` |
| Security scan | `./gradlew dependencyCheckAnalyze` |

---

## Test Case Documentation

### What to Document

Each test should be self-documenting through:
1. **Descriptive test names** - Use `backtick` notation for readability
2. **@DisplayName annotations** - Human-readable descriptions
3. **Comments** - Explain complex setup or non-obvious assertions

### Example of Well-Documented Test

```kotlin
@Test
@DisplayName("Should enforce maximum trader limit of 3")
fun `test create trader enforces max limit`() = runBlocking {
    // Arrange: Create 3 traders (max limit)
    for (i in 1..3) {
        val config = createTestConfig(id = "trader-$i", name = "Trader $i")
        val result = manager.createTrader(config)
        assertTrue(result.isSuccess, "Should create trader $i")
    }

    // Act: Try to create 4th trader
    val config4 = createTestConfig(id = "trader-4", name = "Trader 4")
    val result = manager.createTrader(config4)

    // Assert: Should fail with MaxTradersExceededException
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is MaxTradersExceededException)
}
```

### Test Coverage Documentation

When adding new features, ensure:
- ✅ Unit tests for business logic
- ✅ Integration tests for component interactions
- ✅ Edge cases and error conditions covered
- ✅ Test cleanup properly implemented
- ✅ Tests are isolated and can run in any order

---

**Remember:** Good tests are the foundation of reliable software. Write tests that document behavior, catch regressions, and enable confident refactoring. 🎯

