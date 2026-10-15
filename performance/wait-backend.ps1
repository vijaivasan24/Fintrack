# Waits until backend responds on port 8089
$attempts = 0
$maxAttempts = 60
$ready = $false

while ($attempts -lt $maxAttempts) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8089/api/health" -TimeoutSec 2 -UseBasicParsing -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            $ready = $true
            break
        }
    } catch {
        # still booting
    }
    Start-Sleep -Seconds 1
    $attempts++
}

if ($ready) {
    Write-Output "Backend is ready on port 8089 after $attempts seconds."
} else {
    Write-Error "Backend failed to respond on port 8089 within $maxAttempts seconds."
    exit 1
}
