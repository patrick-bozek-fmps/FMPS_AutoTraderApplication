# CI Failure Analyzer (Simplified)
# Retrieves failure annotations and provides actionable insights

param(
    [string]$RunId = ""
)

$ErrorActionPreference = "Stop"
$repo = "patrick-bozek-fmps/FMPS_AutoTraderApplication"

Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host "  CI Failure Analyzer" -ForegroundColor Cyan
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host ""

# Get GitHub token
$token = $env:GITHUB_TOKEN
if (-not $token) {
    $paths = @("gh", "$env:ProgramFiles\GitHub CLI\gh.exe")
    foreach ($path in $paths) {
        try {
            $token = & $path auth token 2>$null
            if ($token) { Write-Host "[OK] Retrieved token from GitHub CLI" -ForegroundColor Green; break }
        } catch { }
    }
}

if (-not $token) {
    Write-Host "[ERROR] No authentication. Run: gh auth login" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Accept" = "application/vnd.github+json"
    "Authorization" = "Bearer $token"
    "X-GitHub-Api-Version" = "2022-11-28"
}

# Get latest or specific run
if (-not $RunId) {
    $uri = "https://api.github.com/repos/$repo/actions/runs?per_page=10"
    $response = Invoke-RestMethod -Uri $uri -Headers $headers
    $run = $response.workflow_runs | Where-Object { $_.conclusion -eq "failure" } | Select-Object -First 1
    
    if (-not $run) {
        $run = $response.workflow_runs[0]
        Write-Host "[INFO] No failed runs found. Using latest run." -ForegroundColor Yellow
    }
    
    $RunId = $run.id
    Write-Host "Analyzing Run $RunId" -ForegroundColor White
    Write-Host "  Commit: $($run.head_sha.Substring(0, 7))" -ForegroundColor White
    Write-Host "  Status: $($run.status) | Conclusion: $($run.conclusion)" -ForegroundColor $(if ($run.conclusion -eq 'failure') { 'Red' } else { 'Green' })
    Write-Host ""
}

# Get jobs
$jobsResponse = Invoke-RestMethod -Uri "https://api.github.com/repos/$repo/actions/runs/$RunId/jobs" -Headers $headers
$jobs = $jobsResponse.jobs

Write-Host "Found $($jobs.Count) job(s)" -ForegroundColor Green
Write-Host ""

# Analyze jobs
$failedJobs = @()
$allFailureAnnotations = @()

foreach ($job in $jobs) {
    Write-Host ("-" * 80) -ForegroundColor Gray
    Write-Host "Job: $($job.name)" -ForegroundColor Yellow
    Write-Host "  Status: $($job.status) | Conclusion: $($job.conclusion)" -ForegroundColor Gray
    
    if ($job.conclusion -eq "failure") {
        $failedJobs += $job
    }
    
    # Get annotations
    try {
        $annotations = Invoke-RestMethod -Uri "https://api.github.com/repos/$repo/check-runs/$($job.id)/annotations" -Headers $headers
        $failures = $annotations | Where-Object { $_.annotation_level -eq "failure" }
        
        if ($failures.Count -gt 0) {
            Write-Host "  Failures: $($failures.Count)" -ForegroundColor Red
            $allFailureAnnotations += $failures
        }
    } catch {
        Write-Host "  Could not fetch annotations" -ForegroundColor Yellow
    }
    
    Write-Host ""
}

# Summary
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host "  Analysis Summary" -ForegroundColor Cyan
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host ""

if ($failedJobs.Count -eq 0) {
    Write-Host "[SUCCESS] No failed jobs!" -ForegroundColor Green
    exit 0
}

Write-Host "Failed Jobs: $($failedJobs.Count)" -ForegroundColor Red
Write-Host ""

# Show failure details
if ($allFailureAnnotations.Count -gt 0) {
    Write-Host ("-" * 80) -ForegroundColor Gray
    Write-Host "FAILURE DETAILS ($($allFailureAnnotations.Count) failures)" -ForegroundColor Red
    Write-Host ("-" * 80) -ForegroundColor Gray
    Write-Host ""
    
    foreach ($failure in $allFailureAnnotations) {
        Write-Host "[FAILURE] $($failure.title)" -ForegroundColor Red
        $msg = $failure.message
        if ($msg.Length -gt 500) { $msg = $msg.Substring(0, 500) + "..." }
        Write-Host "  $msg" -ForegroundColor Gray
        if ($failure.path) {
            Write-Host "  File: $($failure.path):$($failure.start_line)" -ForegroundColor Cyan
        }
        Write-Host ""
    }
}

# Recommendations
Write-Host "-" * 80 -ForegroundColor Gray
Write-Host "RECOMMENDED ACTIONS" -ForegroundColor Cyan
Write-Host "-" * 80 -ForegroundColor Gray
Write-Host ""

$hasTestFailures = $allFailureAnnotations | Where-Object { $_.message -match "test.*failed|FAILED" }
$hasCompileErrors = $allFailureAnnotations | Where-Object { $_.message -match "error:|compilation failed" }

if ($hasTestFailures) {
    Write-Host "TEST FAILURES DETECTED:" -ForegroundColor Yellow
    Write-Host "  1. Run tests locally: .\gradlew test --no-daemon" -ForegroundColor White
    Write-Host "  2. Review test reports in build/reports/tests/" -ForegroundColor White
    Write-Host "  3. Fix failing tests before pushing" -ForegroundColor White
    Write-Host ""
}

if ($hasCompileErrors) {
    Write-Host "COMPILATION ERRORS DETECTED:" -ForegroundColor Yellow
    Write-Host "  1. Run build locally: .\gradlew clean build --no-daemon" -ForegroundColor White
    Write-Host "  2. Check for syntax errors and missing imports" -ForegroundColor White
    Write-Host "  3. Verify all dependencies are available" -ForegroundColor White
    Write-Host ""
}

Write-Host "For full logs, visit: $($failedJobs[0].html_url)" -ForegroundColor Cyan
Write-Host ""

exit 1

