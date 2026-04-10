param(
    [switch]$SkipInfra,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$maven = Join-Path $root ".tools\apache-maven-3.9.6\bin\mvn.cmd"
$infraCompose = Join-Path $root "infra\docker-compose.yml"
$runtimeDir = Join-Path $root ".demo"
$logsDir = Join-Path $runtimeDir "logs"
$pidsFile = Join-Path $runtimeDir "pids.json"

function Stop-ProcessOnPorts {
    param([int[]]$Ports)

    foreach ($port in $Ports) {
        $listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty OwningProcess -Unique

        foreach ($processId in $listeners) {
            if ($processId -and $processId -ne $PID) {
                try {
                    Stop-Process -Id $processId -Force -ErrorAction Stop
                    Write-Host "Stopped stale process PID $processId on port $port"
                }
                catch {
                    Write-Host "Could not stop PID $processId on port $port"
                }
            }
        }
    }
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Java is not installed or not on PATH."
}

if (-not (Test-Path $maven)) {
    throw "Local Maven not found at $maven"
}

New-Item -ItemType Directory -Path $logsDir -Force | Out-Null

Write-Host "Cleaning up stale service ports..."
Stop-ProcessOnPorts -Ports @(8081, 8082, 8083, 8084, 8085)

if (-not $SkipInfra) {
    Write-Host "Starting Docker infrastructure..."
    docker compose -f $infraCompose up -d
}

$modules = @(
    "event-producer",
    "context-engine",
    "decision-engine",
    "notification-orchestrator",
    "delivery-service"
)

if (-not $SkipBuild) {
    Write-Host "Compiling all services..."
    foreach ($module in $modules) {
        $modulePath = Join-Path $root $module
        Write-Host " - $module"
        Push-Location $modulePath
        & $maven -q -DskipTests compile
        Pop-Location
    }
}

if (Test-Path $pidsFile) {
    Remove-Item $pidsFile -Force
}

$started = @()

Write-Host "Starting Spring Boot services..."
foreach ($module in $modules) {
    $modulePath = Join-Path $root $module
    $outLog = Join-Path $logsDir "$module.out.log"
    $errLog = Join-Path $logsDir "$module.err.log"

    $command = @"
Set-Location '$modulePath'
`$env:KAFKA_BOOTSTRAP_SERVERS = 'localhost:9092'
& '$maven' spring-boot:run
"@

    $proc = Start-Process -FilePath "powershell.exe" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $command) `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError $errLog `
        -WindowStyle Hidden `
        -PassThru

    $started += [PSCustomObject]@{
        name = $module
        pid = $proc.Id
        outLog = $outLog
        errLog = $errLog
    }
}

$started | ConvertTo-Json | Set-Content -Path $pidsFile -Encoding UTF8

Write-Host ""
Write-Host "Demo stack started."
Write-Host "Dashboard:  http://localhost:8085"
Write-Host "Kafka UI:   http://localhost:8088"
Write-Host "Trigger API: POST http://localhost:8081/api/events/demo/full-story?userId=user-1"
Write-Host ""
Write-Host "Process info saved at: $pidsFile"
Write-Host "Logs directory:        $logsDir"
