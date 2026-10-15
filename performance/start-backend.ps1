# Starts FinTrack backend on port 8089 with local,perf profiles using built JAR
$jarPath = "$PSScriptRoot\..\backend\target\fintrack-backend-0.0.1-SNAPSHOT.jar"
$logPath = "$PSScriptRoot\..\backend\backend-perf.log"
$errPath = "$PSScriptRoot\..\backend\backend-perf.err"
$pidPath = "$PSScriptRoot\backend.pid"

$proc = Start-Process -FilePath "java.exe" -ArgumentList "-Dspring.profiles.active=local,perf", "-Dserver.port=8089", "-jar", "`"$jarPath`"" -PassThru -RedirectStandardOutput $logPath -RedirectStandardError $errPath

$proc.Id | Out-File -FilePath $pidPath -Encoding ascii
Write-Output "Started Java backend process with PID $($proc.Id)"
