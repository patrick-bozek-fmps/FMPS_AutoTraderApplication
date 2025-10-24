# GitHub Actions Annotations Checker
# This script retrieves annotations (warnings, errors) from the most recent CI workflow run

param(
    [string]$RunId = "",
    [switch]$ShowAll
)

$ErrorActionPreference = "Stop"

# Configuration
$repo = "patrick-bozek-fmps/FMPS_AutoTraderApplication"

Write-Host "=".PadRight(70, '=') -ForegroundColor Cyan
Write-Host "  GitHub Actions Annotations Checker" -ForegroundColor Cyan
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

$headers = @{
    "Accept" = "application/vnd.github+json"
    "Authorization" = "Bearer $token"
    "X-GitHub-Api-Version" = "2022-11-28"
}

# Get latest workflow run if RunId not specified
if (-not $RunId) {
    Write-Host "Fetching latest workflow run..." -ForegroundColor Cyan
    $uri = "https://api.github.com/repos/$repo/actions/runs?per_page=1"
    
    try {
        $response = Invoke-RestMethod -Uri $uri -Headers $headers -Method Get
        if ($response.workflow_runs.Count -eq 0) {
            Write-Host "[ERROR] No workflow runs found" -ForegroundColor Red
            exit 1
        }
        $run = $response.workflow_runs[0]
        $RunId = $run.id
        
        Write-Host "Latest Run:" -ForegroundColor White
        Write-Host "  Commit:     $($run.head_sha.Substring(0, 7))" -ForegroundColor White
        Write-Host "  Status:     $($run.status)" -ForegroundColor White
        Write-Host "  Conclusion: $(if ($run.conclusion) { $run.conclusion } else { 'N/A' })" -ForegroundColor White
        Write-Host "  Run ID:     $RunId" -ForegroundColor White
        Write-Host ""
    } catch {
        Write-Host "[ERROR] Failed to fetch workflow runs: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

# Get jobs for this workflow run
Write-Host "Fetching jobs for run $RunId..." -ForegroundColor Cyan
$uri = "https://api.github.com/repos/$repo/actions/runs/$RunId/jobs"

try {
    $jobsResponse = Invoke-RestMethod -Uri $uri -Headers $headers -Method Get
    $jobs = $jobsResponse.jobs
    
    if ($jobs.Count -eq 0) {
        Write-Host "[WARN] No jobs found for this workflow run" -ForegroundColor Yellow
        exit 0
    }
    
    Write-Host "Found $($jobs.Count) job(s)" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "[ERROR] Failed to fetch jobs: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Collect all annotations from all jobs
$allAnnotations = @()
$annotationCounts = @{
    "warning" = 0
    "failure" = 0
    "notice" = 0
}

foreach ($job in $jobs) {
    Write-Host "Job: $($job.name)" -ForegroundColor Yellow
    Write-Host "  Status: $($job.status) | Conclusion: $(if ($job.conclusion) { $job.conclusion } else { 'N/A' })" -ForegroundColor Gray
    
    # Get annotations for this job
    $uri = "https://api.github.com/repos/$repo/check-runs/$($job.id)/annotations"
    
    try {
        $annotations = Invoke-RestMethod -Uri $uri -Headers $headers -Method Get
        
        if ($annotations.Count -gt 0) {
            Write-Host "  Found $($annotations.Count) annotation(s)" -ForegroundColor Cyan
            
            foreach ($annotation in $annotations) {
                $allAnnotations += [PSCustomObject]@{
                    Job = $job.name
                    Level = $annotation.annotation_level
                    Title = $annotation.title
                    Message = $annotation.message
                    Path = $annotation.path
                    StartLine = $annotation.start_line
                    EndLine = $annotation.end_line
                }
                
                $annotationCounts[$annotation.annotation_level]++
            }
        } else {
            Write-Host "  No annotations" -ForegroundColor Green
        }
    } catch {
        Write-Host "  [WARN] Could not fetch annotations: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    
    Write-Host ""
}

# Display summary
Write-Host "=".PadRight(70, '=') -ForegroundColor Cyan
Write-Host "  Annotations Summary" -ForegroundColor Cyan
Write-Host "=".PadRight(70, '=') -ForegroundColor Cyan
Write-Host "Total Annotations: $($allAnnotations.Count)" -ForegroundColor White
Write-Host "  Warnings: $($annotationCounts['warning'])" -ForegroundColor Yellow
Write-Host "  Failures: $($annotationCounts['failure'])" -ForegroundColor Red
Write-Host "  Notices:  $($annotationCounts['notice'])" -ForegroundColor Cyan
Write-Host ""

if ($allAnnotations.Count -eq 0) {
    Write-Host "[SUCCESS] No annotations found - Clean build!" -ForegroundColor Green
    exit 0
}

# Display annotations grouped by level
$levels = @("failure", "warning", "notice")
foreach ($level in $levels) {
    $levelAnnotations = $allAnnotations | Where-Object { $_.Level -eq $level }
    
    if ($levelAnnotations.Count -gt 0) {
        $color = switch ($level) {
            "failure" { "Red" }
            "warning" { "Yellow" }
            "notice" { "Cyan" }
            default { "White" }
        }
        
        Write-Host ("-" * 70) -ForegroundColor Gray
        Write-Host "$($level.ToUpper())S ($($levelAnnotations.Count))" -ForegroundColor $color
        Write-Host ("-" * 70) -ForegroundColor Gray
        
        $grouped = $levelAnnotations | Group-Object -Property Title
        
        foreach ($group in $grouped) {
            $count = $group.Count
            $title = $group.Name
            
            if ($count -gt 1) {
                Write-Host "[x$count] $title" -ForegroundColor $color
            } else {
                Write-Host "[x1] $title" -ForegroundColor $color
            }
            
            if ($ShowAll) {
                foreach ($annotation in $group.Group) {
                    Write-Host "  Job: $($annotation.Job)" -ForegroundColor Gray
                    Write-Host "  Message: $($annotation.Message.Substring(0, [Math]::Min(200, $annotation.Message.Length)))" -ForegroundColor Gray
                    if ($annotation.Path) {
                        Write-Host "  File: $($annotation.Path):$($annotation.StartLine)" -ForegroundColor Gray
                    }
                    Write-Host ""
                }
            }
        }
        
        if (-not $ShowAll -and $levelAnnotations.Count -gt 0) {
            Write-Host ""
            Write-Host "Use -ShowAll to see full details" -ForegroundColor Gray
        }
        
        Write-Host ""
    }
}

# Exit code based on severity
if ($annotationCounts["failure"] -gt 0) {
    Write-Host "[FAILURE] $($annotationCounts['failure']) failure annotation(s) found" -ForegroundColor Red
    exit 1
} elseif ($annotationCounts["warning"] -gt 0) {
    Write-Host "[WARNING] $($annotationCounts['warning']) warning annotation(s) found" -ForegroundColor Yellow
    exit 2
} else {
    Write-Host "[SUCCESS] Only notices found (no warnings or failures)" -ForegroundColor Green
    exit 0
}

