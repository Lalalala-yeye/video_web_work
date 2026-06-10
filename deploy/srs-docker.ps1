# 本地 SRS（RTMP + HLS + WebRTC 屏幕分享）
# 用法：在 PowerShell 中执行 .\deploy\srs-docker.ps1

$ErrorActionPreference = "Stop"

docker rm -f srs-live 2>$null

# WebRTC 默认 8000；若本机 8000 被占用，改用 8010（与 srsScreenPublish.js 中 EIP 一致）
$WebRtcPort = 8010

docker run -d --name srs-live `
  -p 1935:1935 `
  -p 8088:8080 `
  -p 1985:1985 `
  -p "${WebRtcPort}:8000/tcp" `
  -p "${WebRtcPort}:8000/udp" `
  -e CANDIDATE=127.0.0.1 `
  registry.cn-hangzhou.aliyuncs.com/ossrs/srs:5 `
  objs/srs -c conf/docker.conf

Write-Host "SRS 已启动：RTMP 1935 | HLS http://127.0.0.1:8088/live/{streamKey}.m3u8 | API 1985 | WebRTC $WebRtcPort"
