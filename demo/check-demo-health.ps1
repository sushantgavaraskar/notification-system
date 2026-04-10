param(
    [switch]$ShowLogHints,
    [switch]$WaitUntilReady,
    [ValidateRange(1, 60)]
    [int]$MaxAttempts = 12,
    [ValidateRange(1, 60)]
    [int]$DelaySeconds = 5
)

$ErrorActionPreference = "Continue"

$root = Split-Path -Parent $PSScriptRoot
$infraCompose = Join-Path $root "infra\docker-compose.yml"
$pidsFile = Join-Path $root ".demo\pids.json"

$script:results = [System.Collections.Generic.List[object]]::new()

function Add-Result {
    param(
        [string]$Component,
        [string]$Check,
        [bool]$Ok,
        [string]$Detail
    )

    if ($null -eq $script:results) {
        $script:results = [System.Collections.Generic.List[object]]::new()
    }

    $script:results.Add([PSCustomObject]@{
        Component = $Component
        Check = $Check
        Status = if ($Ok) { "OK" } else { "FAIL" }
        Detail = $Detail
    })
}

function Check-Http {
    param(
        [string]$Component,
        [string]$Url
    )

    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 4
        $ok = $response.StatusCode -ge 200 -and $response.StatusCode -lt 500
        Add-Result -Component $Component -Check "HTTP" -Ok $ok -Detail ("{0} [{1}]" -f $Url, $response.StatusCode)
    }
    catch {
        Add-Result -Component $Component -Check "HTTP" -Ok $false -Detail ("{0} ({1})" -f $Url, $_.Exception.Message)
    }
}

function Check-Infra {
    param([string]$ComposeFile)

    $expected = @("zookeeper", "kafka", "redis", "postgres", "kafka-ui")

    try {
        $running = docker compose -f $ComposeFile ps --status running --services 2>$null
        if ($LASTEXITCODE -ne 0) {
            Add-Result -Component "infra" -Check "docker-compose" -Ok $false -Detail "Unable to query docker compose status"
            return
        }

        foreach ($service in $expected) {
            $isUp = $running -contains $service
            $containerDetail = if ($isUp) { "running" } else { "not running" }
            Add-Result -Component $service -Check "container" -Ok $isUp -Detail $containerDetail
        }
    }
    catch {
        Add-Result -Component "infra" -Check "docker-compose" -Ok $false -Detail $_.Exception.Message
    }
}

function Check-ServiceProcesses {
    param([string]$PidFile)

    if (-not (Test-Path $PidFile)) {
        Add-Result -Component "services" -Check "pid-file" -Ok $false -Detail "PID file not found. Start demo first (demo/start-demo.ps1)."
        return
    }

    $entries = Get-Content $PidFile -Raw | ConvertFrom-Json
    foreach ($entry in $entries) {
        $proc = Get-Process -Id $entry.pid -ErrorAction SilentlyContinue
        $isRunning = $null -ne $proc
        $processDetail = if ($isRunning) { "PID $($entry.pid) running" } else { "PID $($entry.pid) not running" }
        Add-Result -Component $entry.name -Check "process" -Ok $isRunning -Detail $processDetail

        if (Test-Path $entry.outLog) {
            $started = Select-String -Path $entry.outLog -Pattern "Started .*Application in" -SimpleMatch:$false -Quiet
            $startupDetail = if ($started) { "startup completed" } else { "startup marker not found yet" }
            Add-Result -Component $entry.name -Check "startup-log" -Ok $started -Detail $startupDetail
        }
        else {
            Add-Result -Component $entry.name -Check "startup-log" -Ok $false -Detail "log file not found"
        }

        if (Test-Path $entry.errLog) {
            $failed = Select-String -Path $entry.errLog -Pattern "APPLICATION FAILED TO START" -SimpleMatch -Quiet
            if ($failed) {
                Add-Result -Component $entry.name -Check "error-log" -Ok $false -Detail "APPLICATION FAILED TO START detected"
            }
            else {
                Add-Result -Component $entry.name -Check "error-log" -Ok $true -Detail "no startup failure marker"
            }
        }
    }
}

function Invoke-HealthChecks {
    $script:results = [System.Collections.Generic.List[object]]::new()

    Check-Infra -ComposeFile $infraCompose
    Check-ServiceProcesses -PidFile $pidsFile
    Check-Http -Component "event-producer" -Url "http://localhost:8081/api/events/demo/scenarios"
    Check-Http -Component "delivery-service" -Url "http://localhost:8085/api/health"
    Check-Http -Component "kafka-ui" -Url "http://localhost:8088"

    return $script:results
}

function Print-Hints {
    Write-Host "\nHints:" -ForegroundColor Yellow
    Write-Host "- Start stack: powershell -ExecutionPolicy Bypass -File .\\demo\\start-demo.ps1"
    Write-Host "- Stop stack:  powershell -ExecutionPolicy Bypass -File .\\demo\\stop-demo.ps1"
    Write-Host "- Check logs in .\\demo\\logs"
}

$attempt = 1
do {
    if ($WaitUntilReady) {
        Write-Host "\n[Health Check Attempt $attempt/$MaxAttempts]"
    }

    $results = Invoke-HealthChecks
    $results | Format-Table -AutoSize

    $fails = @($results | Where-Object { $_.Status -eq "FAIL" })
    if ($fails.Count -eq 0) {
        Write-Host "\nDEMO HEALTH: READY" -ForegroundColor Green
        exit 0
    }

    if (-not $WaitUntilReady) {
        break
    }

    if ($attempt -lt $MaxAttempts) {
        Write-Host "\nDEMO HEALTH: NOT READY ($($fails.Count) checks failed). Retrying in $DelaySeconds second(s)..." -ForegroundColor Yellow
        Start-Sleep -Seconds $DelaySeconds
    }

    $attempt++
}
while ($attempt -le $MaxAttempts)

Write-Host "\nDEMO HEALTH: NOT READY ($($fails.Count) checks failed)" -ForegroundColor Red

if ($ShowLogHints) {
    Print-Hints
}

exit 1
