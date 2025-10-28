# GitHub Actions CI Status Checker
# This script checks the status of the most recent CI workflow run

param(
    [int]$WaitSeconds = 0,
    [switch]$Watch
)

$ErrorActionPreference = "Stop"

# Configuration
$repo = "patrick-bozek-fmps/FMPS_AutoTraderApplication"
$owner = "patrick-bozek-fmps"
$repoName = "FMPS_AutoTraderApplication"

Write-Host "=".PadRight(70, '=') -ForegroundColor Cyan
Write-Host "  GitHub Actions CI Status Checker" -ForegroundColor Cyan
Write-Host "=".PadRight(70, '=') -ForegroundColor Cyan
Write-Host ""

# Try to get GitHub token
$token = $env:GITHUB_TOKEN
if (-not $token) {
    Write-Host "[INFO] No GITHUB_TOKEN environment variable found" -ForegroundColor Yellow
    Write-Host "[INFO] Attempting to use 'gh' CLI for authentication..." -ForegroundColor Yellow
    
    try {
        # Try to find gh.exe
        $ghPath = Get-Command gh -ErrorAction SilentlyContinue
        if ($ghPath) {
            $token = & gh auth token 2>$null
            if ($token) {
                Write-Host "[OK] Successfully retrieved token from 'gh' CLI" -ForegroundColor Green
            }
        } else {
            # Try common installation paths
            $commonPaths = @(
                "$env:ProgramFiles\GitHub CLI\gh.exe",
                "$env:LOCALAPPDATA\Programs\GitHub CLI\gh.exe",
                "$env:ProgramFiles(x86)\GitHub CLI\gh.exe"
            )
            
            foreach ($path in $commonPaths) {
                if (Test-Path $path) {
                    $token = & $path auth token 2>$null
                    if ($token) {
                        Write-Host "[OK] Successfully retrieved token from 'gh' at: $path" -ForegroundColor Green
                        break
                    }
                }
            }
        }
    } catch {
        Write-Host "[WARN] Could not retrieve token from 'gh' CLI" -ForegroundColor Yellow
    }
}

if (-not $token) {
    Write-Host ""
    Write-Host "[ERROR] No authentication available" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please either:" -ForegroundColor Yellow
    Write-Host "  1. Set GITHUB_TOKEN environment variable:" -ForegroundColor Yellow
    Write-Host "     `$env:GITHUB_TOKEN = 'your_token'" -ForegroundColor Yellow
    Write-Host "  2. Ensure 'gh' CLI is authenticated:" -ForegroundColor Yellow
    Write-Host "     gh auth login" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

# Function to get workflow runs
function Get-WorkflowRuns {
    $uri = "https://api.github.com/repos/$repo/actions/runs?per_page=5"
    $headers = @{
        "Accept" = "application/vnd.github+json"
        "Authorization" = "Bearer $token"
        "X-GitHub-Api-Version" = "2022-11-28"
    }
    
    try {
        $response = Invoke-RestMethod -Uri $uri -Headers $headers -Method Get
        return $response.workflow_runs
    } catch {
        Write-Host "[ERROR] Failed to fetch workflow runs: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

# Function to display run status
function Show-RunStatus {
    param($run)
    
    $statusColor = switch ($run.status) {
        "completed" { 
            if ($run.conclusion -eq "success") { "Green" }
            elseif ($run.conclusion -eq "failure") { "Red" }
            else { "Yellow" }
        }
        "in_progress" { "Cyan" }
        "queued" { "Gray" }
        default { "White" }
    }
    
    $icon = switch ($run.conclusion) {
        "success" { "[OK]" }
        "failure" { "[X]" }
        default { 
            if ($run.status -eq "in_progress") { "[...]" }
            elseif ($run.status -eq "queued") { "[WAIT]" }
            else { "[?]" }
        }
    }
    
    $shortSha = $run.head_sha.Substring(0, 7)
    $createdAt = [DateTime]::Parse($run.created_at).ToLocalTime().ToString("yyyy-MM-dd HH:mm")
    $conclusionText = if ($run.conclusion) { $run.conclusion } else { 'N/A' }
    
    Write-Host "$icon" -ForegroundColor $statusColor -NoNewline
    Write-Host " $shortSha " -NoNewline
    Write-Host "| $($run.status.PadRight(12))" -NoNewline
    Write-Host "| $($conclusionText.PadRight(10))" -NoNewline
    Write-Host "| $createdAt"
}

# Main logic
do {
    if ($Watch -and $WaitSeconds -gt 0) {
        Clear-Host
        Write-Host "=".PadRight(70, '=') -ForegroundColor Cyan
        Write-Host "  GitHub Actions CI Status Checker (Watching...)" -ForegroundColor Cyan
        Write-Host "=".PadRight(70, '=') -ForegroundColor Cyan
        Write-Host ""
    }
    
    Write-Host "Repository: $repo" -ForegroundColor Cyan
    Write-Host "Fetching latest workflow runs..." -ForegroundColor Cyan
    Write-Host ""
    
    $runs = Get-WorkflowRuns
    
    if ($runs.Count -eq 0) {
        Write-Host "[WARN] No workflow runs found" -ForegroundColor Yellow
        exit 0
    }
    
    Write-Host "Recent Workflow Runs:" -ForegroundColor White
    Write-Host ("-" * 70) -ForegroundColor Gray
    Write-Host "Status  | Commit  | State        | Conclusion | Created At" -ForegroundColor Gray
    Write-Host ("-" * 70) -ForegroundColor Gray
    
    foreach ($run in $runs) {
        Show-RunStatus $run
    }
    
    Write-Host ""
    
    # Show details of the most recent run
    $latestRun = $runs[0]
    Write-Host "=".PadRight(70, '=') -ForegroundColor Cyan
    Write-Host "  Latest Run Details" -ForegroundColor Cyan
    Write-Host "=".PadRight(70, '=') -ForegroundColor Cyan
    $conclusionText = if ($latestRun.conclusion) { $latestRun.conclusion } else { 'N/A' }
    Write-Host "Commit SHA:  $($latestRun.head_sha.Substring(0, 7))" -ForegroundColor White
    Write-Host "Status:      $($latestRun.status)" -ForegroundColor White
    Write-Host "Conclusion:  $conclusionText" -ForegroundColor White
    Write-Host "Started:     $([DateTime]::Parse($latestRun.created_at).ToLocalTime())" -ForegroundColor White
    if ($latestRun.updated_at) {
        Write-Host "Updated:     $([DateTime]::Parse($latestRun.updated_at).ToLocalTime())" -ForegroundColor White
    }
    Write-Host "Run URL:     $($latestRun.html_url)" -ForegroundColor White
    Write-Host ""
    
    # Final status message
    if ($latestRun.status -eq "completed") {
        if ($latestRun.conclusion -eq "success") {
            Write-Host "[SUCCESS] Latest CI run passed!" -ForegroundColor Green
            Write-Host "You can proceed with the next step." -ForegroundColor Green
            $exitCode = 0
        } elseif ($latestRun.conclusion -eq "failure") {
            Write-Host "[FAILURE] Latest CI run failed!" -ForegroundColor Red
            Write-Host "Please review the logs and fix the issues." -ForegroundColor Red
            $exitCode = 1
        } else {
            Write-Host "[WARNING] Latest CI run completed with status: $($latestRun.conclusion)" -ForegroundColor Yellow
            $exitCode = 2
        }
    } else {
        Write-Host "[IN PROGRESS] CI is still running..." -ForegroundColor Cyan
        $exitCode = 3
    }
    
    if ($Watch -and $WaitSeconds -gt 0 -and $latestRun.status -ne "completed") {
        Write-Host ""
        Write-Host "Waiting $WaitSeconds seconds before checking again... (Ctrl+C to stop)" -ForegroundColor Gray
        Start-Sleep -Seconds $WaitSeconds
    } else {
        break
    }
} while ($Watch)

Write-Host ""
exit $exitCode

