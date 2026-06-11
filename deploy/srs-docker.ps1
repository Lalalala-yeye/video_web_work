# SRS local Docker (RTMP + HLS + WebRTC)
# Usage: .\deploy\srs-docker.ps1
#        .\deploy\srs-docker.ps1 -Candidate 192.168.1.10
#
# Host ports: 1935 RTMP | 1985 API | 8080 HLS/FLV | 8000 WebRTC
# Spring Boot backend uses 8081 to avoid conflict with SRS 8080

param(
    [string]$Candidate = ''
)

$ErrorActionPreference = 'Stop'

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

docker rm -f srs-live 2>$null

Write-Host "CANDIDATE=$Candidate"

docker run -d --name srs-live `
  -p 1935:1935 `
  -p 8080:8080 `
  -p 1985:1985 `
  -p 8000:8000/tcp `
  -p 8000:8000/udp `
  -e CANDIDATE=$Candidate `
  -v $Mount `
  registry.cn-hangzhou.aliyuncs.com/ossrs/srs:5 `
  objs/srs -c conf/srs-local.conf

Write-Host 'SRS started: RTMP 1935 | HLS http://127.0.0.1:8080/live/{streamKey}.m3u8 | API 1985 | WebRTC 8000'
Write-Host 'Firewall: deploy\firewall-doinb.ps1 -Action add'
