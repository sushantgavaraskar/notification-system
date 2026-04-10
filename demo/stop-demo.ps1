$ErrorActionPreference = "Continue"

$root = Split-Path -Parent $PSScriptRoot
$infraCompose = Join-Path $root "infra\docker-compose.yml"
$pidsFile = Join-Path $root ".demo\pids.json"

function Stop-ProcessOnPorts {
    param([int[]]$Ports)

    foreach ($port in $Ports) {
        $listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty OwningProcess -Unique

        foreach ($processId in $listeners) {
            if ($processId -and $processId -ne $PID) {
                try {
                    Stop-Process -Id $processId -Force -ErrorAction Stop
                    Write-Host "Stopped listener PID $processId on port $port"
                }
                catch {
                    Write-Host "Could not stop listener PID $processId on port $port"
                }
            }
        }
    }
}

if (Test-Path $pidsFile) {
    $entries = Get-Content $pidsFile | ConvertFrom-Json
    foreach ($entry in $entries) {
        try {
            Stop-Process -Id $entry.pid -Force -ErrorAction Stop
            Write-Host "Stopped $($entry.name) (PID $($entry.pid))"
        }
        catch {
            Write-Host "Process already stopped for $($entry.name) (PID $($entry.pid))"
        }
    }
    Remove-Item $pidsFile -Force
}
else {
    Write-Host "No PID file found."
}

Stop-ProcessOnPorts -Ports @(8081, 8082, 8083, 8084, 8085)

Write-Host "Stopping Docker infrastructure..."
docker compose -f $infraCompose down

Write-Host "Demo stack stopped."
