# SRS local Docker (RTMP + HLS + WebRTC)
# Usage: .\deploy\srs-docker.ps1
#        .\deploy\srs-docker.ps1 -Candidate 192.168.1.10
#
# Host ports: 1935 RTMP | 1985 API | 8080 HLS/FLV | 8000 WebRTC（可选）
# Spring Boot backend uses 8081 to avoid conflict with SRS 8080

param(
    [string]$Candidate = ''
)

$ErrorActionPreference = 'Stop'

function Test-PortFree([int]$Port) {
    $inUse = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    return -not $inUse
}

if (-not $Candidate) {
    $Candidate = (
        Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
        Where-Object {
            $_.IPAddress -notlike '127.*' -and
            $_.IPAddress -notlike '169.254.*' -and
            $_.PrefixOrigin -ne 'WellKnown'
        } |
        Sort-Object InterfaceMetric |
        Select-Object -First 1
    ).IPAddress
}

if (-not $Candidate) {
    Write-Warning 'No LAN IP found. WebRTC CANDIDATE=*. Use: .\deploy\srs-docker.ps1 -Candidate YOUR_IP'
    $Candidate = '*'
}

$ConfFile = Join-Path $PSScriptRoot 'srs-docker.conf'
if (-not (Test-Path $ConfFile)) {
    Write-Host "Missing config: $ConfFile" -ForegroundColor Red
    exit 1
}

$Mount = $ConfFile + ':/usr/local/srs/conf/srs-local.conf'

try { docker rm -f srs-live 2>$null } catch { }

Write-Host "CANDIDATE=$Candidate"

$portArgs = @(
    '-p', '1935:1935',
    '-p', '8080:8080',
    '-p', '1985:1985'
)

if (Test-PortFree 8000) {
    $portArgs += @('-p', '8000:8000/tcp', '-p', '8000:8000/udp')
    Write-Host 'WebRTC port 8000 will be mapped.'
} else {
    Write-Warning 'Port 8000 is busy; skipping WebRTC mapping (OBS RTMP + HLS/FLV still work).'
}

docker run -d --name srs-live `
    @portArgs `
    -e CANDIDATE=$Candidate `
    -v $Mount `
    registry.cn-hangzhou.aliyuncs.com/ossrs/srs:5 `
    objs/srs -c conf/srs-local.conf

if ($LASTEXITCODE -ne 0) {
    Write-Host 'SRS container failed to start. Check Docker and port conflicts.' -ForegroundColor Red
    exit 1
}

Start-Sleep -Seconds 2

$inspect = docker inspect srs-live --format '{{json .NetworkSettings.Ports}}' 2>$null | ConvertFrom-Json
$required = @(8080, 1935, 1985)
$missing = @()
foreach ($p in $required) {
    $key = "$p/tcp"
    if (-not $inspect.$key) { $missing += $p }
}

if ($missing.Count -gt 0) {
    Write-Host "SRS started but critical ports not mapped: $($missing -join ', ')" -ForegroundColor Red
    docker ps -a --filter name=srs-live
    exit 1
}

try {
    $null = Invoke-WebRequest -Uri 'http://127.0.0.1:1985/api/v1/versions' -TimeoutSec 5 -UseBasicParsing
} catch {
    Write-Warning "SRS API not reachable yet: $($_.Exception.Message)"
}

Write-Host 'SRS started: RTMP 1935 | HLS http://127.0.0.1:8080/live/{streamKey}.m3u8 | API 1985' -ForegroundColor Green
if (Test-PortFree 8000) {
    Write-Host 'WebRTC 8000 mapped (browser screen share only).'
} else {
    Write-Host 'WebRTC 8000 not mapped (use OBS RTMP for streaming).'
}
Write-Host 'Firewall: deploy\firewall-doinb.ps1 -Action add'
