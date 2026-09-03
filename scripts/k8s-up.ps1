# 本机 Kubernetes 首次部署（namespace doinb）。人可以停在任意分支，不切 main。
#
#   .\scripts\k8s-up.ps1
#
# 之后用 .\scripts\local-cd.ps1 -Watch 跟 origin/main。

param(
    [string]$Branch = '',
    [string]$Tag = '',
    [string]$GhcrOwner = '',
    [int]$Replicas = 1,
    [int]$WaitImagesMinutes = 15
)

$ErrorActionPreference = 'Stop'
try {
    [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
    $OutputEncoding = [Console]::OutputEncoding
    chcp 65001 | Out-Null
} catch {}

$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

function Write-UpLog([string]$Message) {
    Write-Host ('{0} {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $Message)
}

function Import-DotEnv {
    $p = Join-Path $Root '.env'
    if (-not (Test-Path $p)) { return }
    Get-Content $p -Encoding utf8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq '' -or $line.StartsWith('#') -or $line -notmatch '=') { return }
        $k, $v = $line.Split('=', 2)
        $k = $k.Trim()
        $v = $v.Trim().Trim("'").Trim('"')
        if ($k -and -not [Environment]::GetEnvironmentVariable($k, 'Process')) {
            Set-Item -Path "Env:$k" -Value $v
        }
    }
}

function Invoke-Kubectl {
    & kubectl @args
    if ($LASTEXITCODE -ne 0) { throw "kubectl $($args -join ' ') 失败" }
}

$Modules = @(
    'doinb-gateway', 'doinb-user', 'doinb-video', 'doinb-live',
    'doinb-interact', 'doinb-message', 'doinb-web'
)
$K8sDeploys = @('gateway', 'user', 'video', 'live', 'interact', 'message', 'web')

Import-DotEnv
if (-not (Test-Path (Join-Path $Root '.env'))) {
    Copy-Item (Join-Path $Root '.env.example') (Join-Path $Root '.env')
    Import-DotEnv
}
if (-not $Branch) {
    $Branch = if ($env:LOCAL_CD_BRANCH) { $env:LOCAL_CD_BRANCH } else { 'main' }
}
if (-not $GhcrOwner) {
    $GhcrOwner = if ($env:GHCR_OWNER) { $env:GHCR_OWNER } else { 'lalalala-yeye' }
}

$mysqlPassword = if ($env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD } else { 'test' }
$jwtSecret = if ($env:JWT_SECRET) { $env:JWT_SECRET } else { 'compose-demo-secret-at-least-32-chars!!' }
$internalToken = if ($env:DOINB_INTERNAL_TOKEN) { $env:DOINB_INTERNAL_TOKEN } else { 'doinb-internal-dev-token' }

Write-UpLog '检查 Docker 与 kubectl'
docker info 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Docker 未运行。请打开 Docker Desktop 并启用 Kubernetes。' }
kubectl cluster-info --request-timeout=5s | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'kubectl 连不上集群。Docker Desktop → Settings → Kubernetes → Enable。' }

if ($env:GHCR_TOKEN) {
    $user = if ($env:GHCR_USER) { $env:GHCR_USER } else { $GhcrOwner }
    Write-UpLog "docker login ghcr.io as $user"
    $env:GHCR_TOKEN | docker login ghcr.io -u $user --password-stdin
    if ($LASTEXITCODE -ne 0) { throw 'GHCR 登录失败。需要 read:packages 的 PAT。' }
}

if (-not $Tag) {
    git fetch origin --quiet 2>$null
    $Tag = (git rev-parse --verify --short=7 "origin/$Branch" 2>$null)
    if (-not $Tag) { $Tag = 'cde7310' }
    $Tag = $Tag.Trim()
}
Write-UpLog "使用镜像 tag=$Tag replicas=$Replicas"

Write-UpLog '拉取 GHCR 镜像'
$deadline = (Get-Date).AddMinutes($WaitImagesMinutes)
$missing = @()
do {
    $missing = @()
    foreach ($m in $Modules) {
        cmd /c "docker pull ghcr.io/${GhcrOwner}/${m}:${Tag} >nul 2>&1"
        if ($LASTEXITCODE -ne 0) { $missing += $m }
    }
    if ($missing.Count -eq 0) { break }
    Write-UpLog "等待镜像: $($missing -join ', ')"
    Start-Sleep -Seconds 30
} while ((Get-Date) -lt $deadline)
if ($missing.Count -gt 0) {
    throw "拉不到 ghcr.io/$GhcrOwner/doinb-web:$Tag。看 Actions 是否已绿，或设置 GHCR_TOKEN。"
}

Write-UpLog '创建 namespace / Secret / 数据库脚本'
Invoke-Kubectl apply -f (Join-Path $Root 'deploy/k8s/namespace.yaml') | Out-Host

$secretYaml = kubectl create secret generic doinb-secrets -n doinb `
    --from-literal=MYSQL_ROOT_PASSWORD=$mysqlPassword `
    --from-literal=MYSQL_PASSWORD=$mysqlPassword `
    --from-literal=JWT_SECRET=$jwtSecret `
    --from-literal=DOINB_INTERNAL_TOKEN=$internalToken `
    --dry-run=client -o yaml
if ($LASTEXITCODE -ne 0) { throw '生成 Secret 失败' }
$secretYaml | kubectl apply -f -
if ($LASTEXITCODE -ne 0) { throw 'apply Secret 失败' }

$cmYaml = kubectl create configmap doinb-db-init -n doinb `
    --from-file=001-schema.sql=database/database.sql `
    --from-file=002-seed.sql=database/seed.sql `
    --dry-run=client -o yaml
if ($LASTEXITCODE -ne 0) { throw '生成数据库 ConfigMap 失败' }
$cmYaml | kubectl apply -f -
if ($LASTEXITCODE -ne 0) { throw 'apply 数据库 ConfigMap 失败' }

Write-UpLog 'apply deploy/k8s'
Invoke-Kubectl apply -k (Join-Path $Root 'deploy/k8s') | Out-Host

Write-UpLog "设置镜像并写入 APP_VERSION=$Tag"
foreach ($d in $K8sDeploys) {
    Invoke-Kubectl set image "deployment/$d" "$d=ghcr.io/${GhcrOwner}/doinb-${d}:${Tag}" -n doinb | Out-Host
    if ($d -ne 'web') {
        Invoke-Kubectl scale "deployment/$d" -n doinb --replicas=$Replicas | Out-Host
    }
}
Invoke-Kubectl scale deployment/web -n doinb --replicas=$Replicas | Out-Host

$patchFile = Join-Path $env:TEMP 'doinb-app-version.json'
(@{ data = @{ APP_VERSION = $Tag } } | ConvertTo-Json -Compress) |
    Set-Content -Path $patchFile -Encoding ascii
Invoke-Kubectl patch configmap doinb-runtime -n doinb --type merge --patch-file $patchFile | Out-Host
Invoke-Kubectl set env deployment/gateway "APP_VERSION=$Tag" -n doinb | Out-Host

Write-UpLog '等待工作负载就绪'
foreach ($d in @('mysql') + $K8sDeploys) {
    Invoke-Kubectl rollout status "deployment/$d" -n doinb --timeout=300s | Out-Host
}

$stateFile = Join-Path $Root '.local-cd-state'
@{ sha = $Tag; branch = $Branch; deployedAt = (Get-Date).ToString('o') } |
    ConvertTo-Json | Set-Content -Path $stateFile -Encoding utf8

Write-Host ''
Invoke-Kubectl get pods,svc -n doinb -o wide | Out-Host
Write-UpLog "完成。浏览器打开 http://localhost:8787 ，版本号应是 $Tag"
Write-UpLog '之后在本机开着: .\scripts\local-cd.ps1 -Watch'
