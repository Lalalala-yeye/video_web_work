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
    Write-Host "=== $title ==="
    Write-Host ("code={0} videos={1} liveRooms={2} users={3}" -f `
        $j.code, @($j.data.videos).Count, @($j.data.liveRooms).Count, @($j.data.users).Count)
    if ($j.data.notices) {
        foreach ($n in @($j.data.notices)) { Write-Host "NOTICE: $n" }
    } else {
        Write-Host 'NOTICE: (none)'
    }
}

Write-Host 'Fault demo: stop video, search degrades, gateway/user/live stay up.'
Write-Host "target=$Target base=$Base"

$health = Invoke-RestMethod -Uri "$Base/health" -TimeoutSec 5
Write-Host ("gateway health: {0}" -f ($health | ConvertTo-Json -Compress))

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

$gw = Invoke-RestMethod -Uri "$Base/health" -TimeoutSec 5
Write-Host ("gateway still: {0}" -f ($gw | ConvertTo-Json -Compress))

try {
    $list = Invoke-WebRequest -UseBasicParsing -Uri "$Base/video/list?page=1&size=12" -TimeoutSec 8
    Write-Host ("/video/list HTTP {0}" -f [int]$list.StatusCode)
} catch {
    Write-Host ("/video/list failed (expected): {0}" -f $_.Exception.Message)
}

if ($after.code -ne 200) { throw 'search code is not 200' }
if (-not $after.data.notices) { throw 'missing degrade notices' }
if (@($after.data.videos).Count -ne 0) { throw 'videos should be empty' }

Write-Host ''
Write-Host 'restore video'
if ($Target -eq 'compose') {
    docker start doinb-video | Out-Host
} else {
    kubectl scale deployment/video -n doinb --replicas=2 | Out-Host
    kubectl apply -n doinb -f (Join-Path $PSScriptRoot 'hpa.yaml') | Out-Host
}

Write-Host 'OK: search stayed 200 with notices; isolation passed.'
