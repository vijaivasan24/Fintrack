$conn = Get-NetTCPConnection -LocalPort 8089 -ErrorAction SilentlyContinue | Select-Object -First 1
if ($conn) {
    $conn.OwningProcess | Out-File -FilePath "$PSScriptRoot\backend.pid" -Encoding ascii
    Write-Output "Recorded Backend PID: $($conn.OwningProcess)"
} else {
    Write-Error "No process listening on port 8089"
}
