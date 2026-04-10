param(
    [ValidateSet("message-burst", "battery-low", "user-inactive", "critical-alert", "full-story")]
    [string]$Scenario = "full-story",
    [string]$UserId = "user-1",
    [switch]$WaitForReady,
    [ValidateRange(1, 60)]
    [int]$MaxAttempts = 12,
    [ValidateRange(1, 60)]
    [int]$DelaySeconds = 5
)

if ($WaitForReady) {
    $healthScript = Join-Path $PSScriptRoot "check-demo-health.ps1"
    Write-Host "Waiting for demo stack readiness before triggering scenario..."

    & powershell -NoProfile -ExecutionPolicy Bypass -File $healthScript `
        -WaitUntilReady `
        -MaxAttempts $MaxAttempts `
        -DelaySeconds $DelaySeconds

    if ($LASTEXITCODE -ne 0) {
        throw "Demo stack did not become ready in time."
    }
}

$endpoint = "http://localhost:8081/api/events/demo/$($Scenario)?userId=$($UserId)"

Write-Host "Triggering scenario '$Scenario' for user '$UserId'..."
$response = Invoke-RestMethod -Method POST -Uri $endpoint
$response | ConvertTo-Json
