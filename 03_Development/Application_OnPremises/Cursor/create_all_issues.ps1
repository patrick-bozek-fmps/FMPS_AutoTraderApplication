# Script to create all GitHub issues for FMPS AutoTrader project
# Run this from the repository root

$ghPath = "C:\Program Files\GitHub CLI\gh.exe"

# Array of all issues to create
$issues = @(
    @{
        title = "Implement configuration management"
        body = @"
Create configuration system using HOCON format.

**Tasks:**
- [ ] Add Typesafe Config dependency
- [ ] Create ``application.conf`` structure
- [ ] Create ``ConfigLoader`` class
- [ ] Define configuration sections (Server, Database, Exchange API, Trading limits, Logging)
- [ ] Support environment variable overrides
- [ ] Add configuration validation
- [ ] Create configuration documentation

**Acceptance Criteria:**
- Configuration loads from file
- Environment variables override settings
- Invalid configuration throws clear errors
- Documentation complete

**Dependencies:** #1

**Estimate:** 1 day
"@
        milestone = "Foundation & Infrastructure"
        labels = "priority: high,type: infrastructure,module: core"
    },
    @{
        title = "Set up testing infrastructure"
        body = @"
Configure testing framework with coverage reporting.

**Tasks:**
- [ ] Apply test configuration from ``gradle-test-config-example.kts``
- [ ] Add JUnit 5, Mockk, Kotest dependencies
- [ ] Configure JaCoCo for coverage
- [ ] Set up integration test source sets
- [ ] Configure ktlint for code style
- [ ] Configure detekt for static analysis
- [ ] Create test utilities and fixtures
- [ ] Configure H2 in-memory DB for tests
- [ ] Set up test logging configuration
- [ ] Verify CI/CD pipeline runs tests

**Acceptance Criteria:**
- ``./gradlew test`` runs all unit tests
- ``./gradlew integrationTest`` runs integration tests
- Coverage report generated
- 80%+ coverage enforced
- CI/CD pipeline passes

**Dependencies:** #1

**Estimate:** 2 days
"@
        milestone = "Foundation & Infrastructure"
        labels = "priority: critical,type: test"
    },
    @{
        title = "Create API endpoint structure"
        body = @"
Define REST API routes and structure.

**Tasks:**
- [ ] Create route modules (``/api/traders``, ``/api/positions``, ``/api/history``, ``/api/config``, ``/api/health``, ``/ws``)
- [ ] Implement request validation
- [ ] Add API versioning (v1)
- [ ] Create OpenAPI/Swagger documentation
- [ ] Implement rate limiting
- [ ] Add request ID tracking
- [ ] Write API endpoint tests

**Acceptance Criteria:**
- All endpoints defined
- OpenAPI doc generated
- Request validation works
- Tests pass with 85%+ coverage

**Dependencies:** #3

**Estimate:** 2 days
"@
        milestone = "Foundation & Infrastructure"
        labels = "priority: high,type: infrastructure,module: core"
    },
    @{
        title = "Create repository layer"
        body = @"
Implement data access layer with repository pattern.

**Tasks:**
- [ ] Create repository interfaces (AITraderRepository, TradeHistoryRepository, PatternRepository, PositionRepository)
- [ ] Implement repositories with Exposed
- [ ] Add CRUD operations
- [ ] Implement query methods
- [ ] Add transaction management
- [ ] Create repository tests with H2

**Acceptance Criteria:**
- All repositories implemented
- CRUD operations work
- Transactions handled correctly
- 90%+ test coverage

**Dependencies:** #2, #5

**Estimate:** 2 days
"@
        milestone = "Foundation & Infrastructure"
        labels = "priority: critical,type: infrastructure,module: database"
    }
)

# Create each issue
foreach ($issue in $issues) {
    Write-Host "Creating issue: $($issue.title)..." -ForegroundColor Cyan
    
    & $ghPath issue create `
        --title $issue.title `
        --body $issue.body `
        --milestone $issue.milestone `
        --label $issue.labels
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✓ Created successfully" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Failed to create" -ForegroundColor Red
    }
    
    Start-Sleep -Milliseconds 500  # Rate limiting
}

Write-Host "`nDone! Check: https://github.com/patrick-bozek-fmps/FMPS_AutoTraderApplication/issues" -ForegroundColor Yellow

