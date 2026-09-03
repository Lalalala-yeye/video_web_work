param(
    [ValidateSet('video-list', 'login')]
    [string]$Scenario = 'login',
    [int]$Vus = 50,
    [int]$Duration = 90
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

Write-Host '=== 自动扩缩容演示 ==='
Write-Host '现场请另开终端一直盯着：'
Write-Host '  kubectl get hpa -n doinb -w'
Write-Host '  kubectl get pods -n doinb -l ''app in (gateway,video,user)'' -w'
Write-Host ''

Write-Host '检查 metrics-server...'
kubectl top pods -n doinb 2>$null | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw 'kubectl top 失败。kind 请先按 deploy/k8s/README.md 安装 metrics-server（加 --kubelet-insecure-tls）。'
}

Write-Host '当前 HPA：'
kubectl get hpa -n doinb
Write-Host ''
Write-Host "加压 $Scenario  ${Vus} VU  ${Duration}s（打 port-forward 后的 8081）"
Write-Host '请先在另一终端： kubectl port-forward -n doinb svc/gateway 8081:8081'
Pause

Set-Location $Root
node bench/run.mjs --label micro --base http://127.0.0.1:8081 --scenario $Scenario --vus $Vus --duration $Duration --warmup 5 --rounds 1

Write-Host ''
Write-Host '压力已停。扩容按约 2 倍加副本；缩容要等约 2 分钟稳定窗口，再每分钟最多减 20%。把脚本打印的吞吐/均时/P95/错误率记进实验记录。'
