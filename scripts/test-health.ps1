#Requires -Version 5.1
[CmdletBinding()]
param(
    [string]$BaseUrl = $env:BASE_URL,
    [int]$Retries = 30,
    [int]$DelaySeconds = 1,
    [int]$TimeoutSeconds = 5,
    [switch]$IncludeMetrics
)

if ([string]::IsNullOrWhiteSpace($BaseUrl)) { $BaseUrl = 'http://localhost:8080' }

function Join-Url([string]$base, [string]$path) {
    $baseTrim = $base.TrimEnd('/')
    $pathTrim = if ($path.StartsWith('/')) { $path } else { '/' + $path }
    return "$baseTrim$pathTrim"
}

function Test-Endpoint {
    param(
        [string]$Url,
        [string]$Name,
        [string]$ExpectContains,
        [switch]$Required,
        [int]$Retries,
        [int]$DelaySeconds,
        [int]$TimeoutSeconds
    )

    $attempt = 0
    $passed = $false
    $statusCode = $null
    $errorMsg = $null
    $content = $null
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    while ($attempt -lt $Retries -and -not $passed) {
        $attempt++
        try {
            $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec $TimeoutSeconds -Headers @{ 'Accept' = 'application/json, text/plain, */*' }
            $statusCode = [int]$resp.StatusCode
            $content = $resp.Content
            if ($statusCode -ge 200 -and $statusCode -lt 300) {
                if ([string]::IsNullOrEmpty($ExpectContains) -or ($content -match [Regex]::Escape($ExpectContains))) {
                    $passed = $true
                    break
                } else {
                    $errorMsg = "Response didn't contain expected text '$ExpectContains'"
                }
            } else {
                $errorMsg = "Unexpected status code $statusCode"
            }
        } catch {
            $errorMsg = $_.Exception.Message
        }

        if ($attempt -lt $Retries) { Start-Sleep -Seconds $DelaySeconds }
    }
    $stopwatch.Stop()

    return [pscustomobject]@{
        Name        = $Name
        Url         = $Url
        Required    = [bool]$Required
        Attempts    = $attempt
        StatusCode  = $statusCode
        Passed      = $passed
        DurationSec = [Math]::Round($stopwatch.Elapsed.TotalSeconds, 3)
        Error       = if ($passed) { $null } else { $errorMsg }
        Snippet     = if ($passed) { $null } else { if ($content) { $content.Substring(0, [Math]::Min(200, $content.Length)) } else { $null } }
    }
}

$tests = @(
    @{ Path = '/actuator/health';           Name = 'Actuator Health'; Expect = 'UP';    Required = $true }
    @{ Path = '/actuator/health/liveness';  Name = 'Liveness';        Expect = 'UP';    Required = $true }
    @{ Path = '/actuator/health/readiness'; Name = 'Readiness';       Expect = 'UP';    Required = $true }
    @{ Path = '/api/v1/check/ping';         Name = 'Ping';            Expect = 'pong';  Required = $false }
)

if ($IncludeMetrics) {
    $tests += @(
        @{ Path = '/actuator/info';        Name = 'Actuator Info';      Expect = '';      Required = $false }
        @{ Path = '/actuator/prometheus';  Name = 'Prometheus Metrics'; Expect = '#';     Required = $false }
    )
}

Write-Host "==> Health check: $BaseUrl" -ForegroundColor Cyan
Write-Host "    Retries: $Retries, Delay: ${DelaySeconds}s, Timeout per call: ${TimeoutSeconds}s" -ForegroundColor DarkCyan

$results = @()
foreach ($t in $tests) {
    $url = Join-Url $BaseUrl $t.Path
    $r = Test-Endpoint -Url $url -Name $t.Name -ExpectContains $t.Expect -Required:([bool]$t.Required) -Retries $Retries -DelaySeconds $DelaySeconds -TimeoutSeconds $TimeoutSeconds
    $results += $r
    $emoji = if ($r.Passed) { '✅' } else { if ($r.Required) { '❌' } else { '⚠️' } }
    $color = if ($r.Passed) { 'Green' } elseif ($r.Required) { 'Red' } else { 'Yellow' }
    $code = if ($r.StatusCode) { $r.StatusCode } else { '-' }
    Write-Host ("{0} {1} [{2}] ({3}s) -> {4}" -f $emoji, $r.Name, $code, $r.DurationSec, $r.Url) -ForegroundColor $color
    if (-not $r.Passed -and $r.Error) { Write-Host ("    Reason: {0}" -f $r.Error) -ForegroundColor $color }
}

$failedRequired = $results | Where-Object { -not $_.Passed -and $_.Required }
$failedOptional = $results | Where-Object { -not $_.Passed -and -not $_.Required }

Write-Host ""
if ($failedRequired.Count -eq 0) {
    Write-Host "All required health checks passed." -ForegroundColor Green
} else {
    Write-Host ("{0} required health check(s) failed:" -f $failedRequired.Count) -ForegroundColor Red
    foreach ($f in $failedRequired) {
        Write-Host (" - {0} -> {1}" -f $f.Name, $f.Url) -ForegroundColor Red
    }
}

if ($failedOptional.Count -gt 0) {
    Write-Host ("{0} optional check(s) failed (not blocking):" -f $failedOptional.Count) -ForegroundColor Yellow
    foreach ($f in $failedOptional) {
        Write-Host (" - {0} -> {1}" -f $f.Name, $f.Url) -ForegroundColor Yellow
    }
}

if ($failedRequired.Count -gt 0) { exit 1 } else { exit 0 }
