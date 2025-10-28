# 🧪 FMPS AutoTrader - Testing Guide

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
./gradlew integrationTest
```

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
            id = "test-trader",
            config = TradingConfig(...),
            connector = mockConnector
        )
    }
    
    @Test
    @DisplayName("Should start trading when conditions are met")
    fun testStartTrading() = runTest {
        // Arrange
        every { mockConnector.isConnected() } returns true
        
        // Act
        aiTrader.start()
        
        // Assert
        assertEquals(TraderState.RUNNING, aiTrader.state)
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
2. ✅ Run all unit tests
3. ✅ Run integration tests
4. ✅ Generate coverage report
5. ✅ Enforce 80% minimum coverage
6. ✅ Code style check (ktlint)
7. ✅ Static analysis (detekt)
8. ✅ Security scan (OWASP Dependency Check)

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
- Use H2 in-memory database
- Schema created/destroyed per test class

**Integration Tests:**
- Use SQLite file database
- Reset before each test

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
| Run integration tests | `./gradlew integrationTest` |
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

**Remember:** Good tests are the foundation of reliable software. Write tests that document behavior, catch regressions, and enable confident refactoring. 🎯

