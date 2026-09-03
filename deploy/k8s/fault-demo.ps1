param(
    [ValidateSet('compose', 'k8s')]
    [string]$Target = 'compose',
    [string]$Base = 'http://127.0.0.1:8081',
    [string]$Keyword = '像素'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()

function Invoke-Search {
    $enc = [uri]::EscapeDataString($Keyword)
    $url = "$Base/search?keyword=$enc&videoLimit=10&liveLimit=10&userLimit=10"
    Invoke-RestMethod -Uri $url -TimeoutSec 15
}

function Show-Result($title, $j) {
    Write-Host ""
    Write-Host "=== $title search ==="
    Write-Host ("code={0} videos={1} liveRooms={2} users={3}" -f `
        $j.code, @($j.data.videos).Count, @($j.data.liveRooms).Count, @($j.data.users).Count)
    if ($j.data.notices) {
        foreach ($n in @($j.data.notices)) { Write-Host "NOTICE: $n" }
    } else {
        Write-Host 'NOTICE: (none)'
    }
}

function Probe-Url($name, $url) {
    try {
        $j = Invoke-RestMethod -Uri $url -TimeoutSec 3
        Write-Host ("{0}: {1}" -f $name, ($j | ConvertTo-Json -Compress))
        return $true
    } catch {
        Write-Host ("{0}: DOWN" -f $name)
        return $false
    }
}

function Probe-K8sExec($name, $port) {
    $old = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $raw = kubectl exec -n doinb "deploy/$name" -- curl -fsS -m 3 "http://127.0.0.1:$port/health" 2>&1
    $code = $LASTEXITCODE
    $ErrorActionPreference = $old
    $text = (($raw | ForEach-Object { $_.ToString() }) -join "`n").Trim()
    $json = ($text -split "`r?`n" | Where-Object { $_.Trim().StartsWith('{') } | Select-Object -Last 1)
    if ($code -eq 0 -and $json) {
        Write-Host ("{0}: {1}" -f $name, $json.Trim())
        return $true
    }
    Write-Host ("{0}: DOWN" -f $name)
    return $false
}

function Show-Health($title) {
    Write-Host ""
    Write-Host "=== $title health ==="
    $up = @{}
    if ($Target -eq 'compose') {
        $up['gateway'] = Probe-Url 'gateway' 'http://127.0.0.1:8081/health'
        $up['user'] = Probe-Url 'user' 'http://127.0.0.1:8082/health'
        $up['video'] = Probe-Url 'video' 'http://127.0.0.1:8083/health'
        $up['live'] = Probe-Url 'live' 'http://127.0.0.1:8084/health'
        $up['interact'] = Probe-Url 'interact' 'http://127.0.0.1:8085/health'
        $up['message'] = Probe-Url 'message' 'http://127.0.0.1:8086/health'
        return $up
    }

    $up['gateway'] = Probe-K8sExec 'gateway' 8081
    $up['user'] = Probe-K8sExec 'user' 8082
    $up['video'] = Probe-K8sExec 'video' 8083
    $up['live'] = Probe-K8sExec 'live' 8084
    $up['interact'] = Probe-K8sExec 'interact' 8085
    $up['message'] = Probe-K8sExec 'message' 8086
    return $up
}

Write-Host 'Fault demo: stop video, search degrades; gateway/user/live/interact/message stay up.'
Write-Host "target=$Target base=$Base"

$null = Show-Health 'before'

$before = Invoke-Search
Show-Result 'before' $before

Write-Host ''
if ($Target -eq 'compose') {
    Write-Host 'docker stop doinb-video'
    docker stop doinb-video | Out-Host
} else {
    Write-Host 'k8s: delete HPA video, scale video to 0'
    kubectl delete hpa video -n doinb --ignore-not-found | Out-Host
    kubectl scale deployment/video -n doinb --replicas=0 | Out-Host
    kubectl wait --for=delete pod -l app=video -n doinb --timeout=60s 2>$null | Out-Host
}

Start-Sleep -Seconds 2

$after = Invoke-Search
Show-Result 'video down' $after

$healthAfter = Show-Health 'video down'

try {
    $list = Invoke-WebRequest -UseBasicParsing -Uri "$Base/video/list?page=1&size=12" -TimeoutSec 8
    Write-Host ("/video/list HTTP {0}" -f [int]$list.StatusCode)
} catch {
    Write-Host ("/video/list failed (expected): {0}" -f $_.Exception.Message)
}

if ($after.code -ne 200) { throw 'search code is not 200' }
if (-not $after.data.notices) { throw 'missing degrade notices' }
if (@($after.data.videos).Count -ne 0) { throw 'videos should be empty' }
foreach ($name in @('gateway', 'user', 'live', 'interact', 'message')) {
    if (-not $healthAfter.ContainsKey($name) -or -not $healthAfter[$name]) {
        throw "$name health should stay up"
    }
}
if (-not $healthAfter.ContainsKey('video') -or $healthAfter['video']) {
    throw 'video health should be DOWN'
}

Write-Host ''
Write-Host 'restore video'
if ($Target -eq 'compose') {
    docker start doinb-video | Out-Host
} else {
    kubectl scale deployment/video -n doinb --replicas=2 | Out-Host
    kubectl apply -n doinb -f (Join-Path $PSScriptRoot 'hpa.yaml') | Out-Host
}

Write-Host 'OK: search stayed 200 with notices; video DOWN; other services /health still up.'
