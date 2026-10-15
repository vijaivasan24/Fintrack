param(
    [Parameter(Mandatory = $true)]
    [string]$JtlPath
)

if (-not (Test-Path $JtlPath)) {
    Write-Error "File not found: $JtlPath"
    exit 1
}

function Get-Percentile($sortedList, [double]$p) {
    if ($sortedList.Count -eq 0) { return 0 }
    $idx = [int][Math]::Ceiling(($p / 100.0) * $sortedList.Count) - 1
    if ($idx -lt 0) { $idx = 0 }
    if ($idx -ge $sortedList.Count) { $idx = $sortedList.Count - 1 }
    return $sortedList[$idx]
}

function Calculate-Stats($samples, [string]$groupName) {
    $count = $samples.Count
    if ($count -eq 0) {
        Write-Output "No samples found for $groupName"
        return
    }

    $elapsedList = [System.Collections.Generic.List[double]]::new()
    $errCount = 0
    $minTime = [double]::MaxValue
    $maxEnd = [double]::MinValue
    $sum = 0.0
    $maxElapsed = 0.0

    foreach ($s in $samples) {
        $el = [double]$s.elapsed
        $ts = [double]$s.timeStamp
        $elapsedList.Add($el)
        $sum += $el
        if ($el -gt $maxElapsed) { $maxElapsed = $el }
        if ($s.success -ne "true") { $errCount++ }
        if ($ts -lt $minTime) { $minTime = $ts }
        $end = $ts + $el
        if ($end -gt $maxEnd) { $maxEnd = $end }
    }

    $elapsedList.Sort()

    $avg = [Math]::Round($sum / $count, 2)
    $med = Get-Percentile $elapsedList 50
    $p90 = Get-Percentile $elapsedList 90
    $p95 = Get-Percentile $elapsedList 95
    $p99 = Get-Percentile $elapsedList 99
    $errPct = [Math]::Round(($errCount / $count) * 100.0, 2)

    $durationSec = ($maxEnd - $minTime) / 1000.0
    if ($durationSec -le 0) { $durationSec = 1.0 }
    $tput = [Math]::Round($count / $durationSec, 2)

    [PSCustomObject]@{
        Label       = $groupName
        Count       = $count
        Average_ms  = $avg
        Median_ms   = $med
        P90_ms      = $p90
        P95_ms      = $p95
        P99_ms      = $p99
        Max_ms      = $maxElapsed
        Error_Rate  = "$errPct%"
        Throughput  = "$tput/s"
    }
}

$csv = Import-Csv -Path $JtlPath

$loginSamples = [System.Collections.Generic.List[PSCustomObject]]::new()
$apiSamples = [System.Collections.Generic.List[PSCustomObject]]::new()
$apiByLabel = @{}

foreach ($row in $csv) {
    $lbl = $row.label
    if ($lbl -eq "login") {
        $loginSamples.Add($row)
    } elseif ($lbl.StartsWith("api:")) {
        $apiSamples.Add($row)
        if (-not $apiByLabel.ContainsKey($lbl)) {
            $apiByLabel[$lbl] = [System.Collections.Generic.List[PSCustomObject]]::new()
        }
        $apiByLabel[$lbl].Add($row)
    }
}

Write-Output ""
Write-Output "=========================================================================="
Write-Output "FinTrack Performance Summary: $JtlPath"
Write-Output "=========================================================================="
Write-Output ""

$overallStats = @()
if ($loginSamples.Count -gt 0) {
    $overallStats += Calculate-Stats $loginSamples "login"
}
if ($apiSamples.Count -gt 0) {
    $overallStats += Calculate-Stats $apiSamples "all api: combined"
}

$overallStats | Format-Table -AutoSize

Write-Output ""
Write-Output "Average per API label:"
Write-Output "--------------------------------------------------------------------------"

$labelStats = @()
foreach ($key in ($apiByLabel.Keys | Sort-Object)) {
    $sub = $apiByLabel[$key]
    $subCount = $sub.Count
    $subSum = 0.0
    $subErr = 0
    foreach ($item in $sub) {
        $subSum += [double]$item.elapsed
        if ($item.success -ne "true") { $subErr++ }
    }
    $subAvg = [Math]::Round($subSum / $subCount, 2)
    $subErrPct = [Math]::Round(($subErr / $subCount) * 100.0, 2)
    $labelStats += [PSCustomObject]@{
        Label       = $key
        Count       = $subCount
        Average_ms  = $subAvg
        Error_Rate  = "$subErrPct%"
    }
}

$labelStats | Format-Table -AutoSize
