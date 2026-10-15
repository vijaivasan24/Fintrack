# Stops FinTrack backend by its recorded process ID
$pidFile = "$PSScriptRoot\backend.pid"
if (Test-Path $pidFile) {
    $procId = (Get-Content $pidFile).Trim()
    if ($procId) {
        Write-Output "Stopping backend process PID $procId..."
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}

$portConn = Get-NetTCPConnection -LocalPort 8089 -ErrorAction SilentlyContinue
if ($portConn) {
    $owningPid = $portConn.OwningProcess | Select-Object -First 1
    if ($owningPid) {
        Write-Output "Stopping remaining port 8089 process PID $owningPid..."
        Stop-Process -Id $owningPid -Force -ErrorAction SilentlyContinue
    }
}
Write-Output "Backend stopped."
