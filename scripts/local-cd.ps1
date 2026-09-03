# 本机跟 GitHub：push 到 main → Actions 打镜像 → 本脚本按 origin/main 更新 k8s。
# 人可以一直留在 小学期ZZW：脚本只快进本地 main 指针，绝不 checkout 到 main。
#
#   .\scripts\local-cd.ps1 -Watch

param(
    [string]$Branch = '',
    [switch]$Watch,
    [int]$IntervalSeconds = 30,
    [int]$WaitImagesMinutes = 45,
    [string]$GhcrOwner = ''
)

$ErrorActionPreference = 'Stop'
try {
    [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
    $OutputEncoding = [Console]::OutputEncoding
    chcp 65001 | Out-Null
} catch {}
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

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
Import-DotEnv
if (-not $Branch) {
    $Branch = if ($env:LOCAL_CD_BRANCH) { $env:LOCAL_CD_BRANCH } else { 'main' }
}
if (-not $GhcrOwner) {
    $GhcrOwner = if ($env:GHCR_OWNER) { $env:GHCR_OWNER } else { 'lalalala-yeye' }
}

$StateFile = Join-Path $Root '.local-cd-state'
$LogFile = Join-Path $Root '.local-cd.log'
$Modules = @(
    'doinb-gateway', 'doinb-user', 'doinb-video', 'doinb-live',
    'doinb-interact', 'doinb-message', 'doinb-web'
)
$K8sDeploys = @('gateway', 'user', 'video', 'live', 'interact', 'message', 'web')

function Write-CdLog([string]$Message) {
    $line = '{0} {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $Message
    Write-Host $line
    Add-Content -Path $LogFile -Value $line -Encoding utf8
}

function Test-K8sDoinb {
    kubectl get ns doinb --request-timeout=3s 2>$null | Out-Null
    return ($LASTEXITCODE -eq 0)
}

function Connect-Ghcr {
    if (-not $env:GHCR_TOKEN) { return }
    $user = if ($env:GHCR_USER) { $env:GHCR_USER } else { $GhcrOwner }
    Write-CdLog "docker login ghcr.io as $user"
    $env:GHCR_TOKEN | docker login ghcr.io -u $user --password-stdin
    if ($LASTEXITCODE -ne 0) { throw 'GHCR 登录失败。需要 read:packages 的 PAT。' }
}

function Get-RemoteSha([string]$branch) {
    git fetch origin --quiet 2>$null
    $sha = git rev-parse --verify --short=7 "origin/$branch" 2>$null
    if (-not $sha) { throw "找不到 origin/$branch" }
    return $sha.Trim()
}

function Sync-LocalMain([string]$branch) {
    $current = (git branch --show-current).Trim()
    if ($current -eq $branch) {
        Write-CdLog "当前就在 $branch，不改工作区，按 origin/$branch 部署。"
        return
    }
    # 只移动本地 main 指针，不切换当前分支、不碰工作区。
    git fetch origin "${branch}:${branch}"
    if ($LASTEXITCODE -ne 0) {
        throw "无法快进本地 $branch 到 origin/$branch（当前仍在 $current）"
    }
    Write-CdLog "本地 $branch 已快进到 origin/$branch，当前分支仍是 $current"
}

function Wait-GhcrImages([string]$owner, [string]$sha) {
    $deadline = (Get-Date).AddMinutes($WaitImagesMinutes)
    while ((Get-Date) -lt $deadline) {
        $missing = @()
        foreach ($m in $Modules) {
            cmd /c "docker pull ghcr.io/${owner}/${m}:${sha} >nul 2>&1"
            if ($LASTEXITCODE -ne 0) { $missing += $m }
        }
        if ($missing.Count -eq 0) { return $true }
        Write-CdLog "等待 CI 镜像: $($missing -join ', ')"
        Start-Sleep -Seconds 45
    }
    return $false
}

function Deploy-K8s([string]$sha) {
    Write-CdLog "更新 k8s 镜像 tag=$sha"
    foreach ($d in $K8sDeploys) {
        kubectl set image "deployment/$d" "$d=ghcr.io/${GhcrOwner}/doinb-${d}:${sha}" -n doinb
        if ($LASTEXITCODE -ne 0) { throw "kubectl set image $d 失败" }
    }
    $patchFile = Join-Path $env:TEMP 'doinb-app-version.json'
    (@{ data = @{ APP_VERSION = $sha } } | ConvertTo-Json -Compress) |
        Set-Content -Path $patchFile -Encoding ascii
    kubectl patch configmap doinb-runtime -n doinb --type merge --patch-file $patchFile
    if ($LASTEXITCODE -ne 0) { throw 'patch APP_VERSION 失败' }
    kubectl set env deployment/gateway "APP_VERSION=$sha" -n doinb
    if ($LASTEXITCODE -ne 0) { throw 'set env APP_VERSION 失败' }
    foreach ($d in $K8sDeploys) {
        kubectl rollout restart "deployment/$d" -n doinb
    }
    foreach ($d in $K8sDeploys) {
        kubectl rollout status "deployment/$d" -n doinb --timeout=300s
        if ($LASTEXITCODE -ne 0) { throw "rollout $d 失败" }
    }
}

function Save-State([string]$sha) {
    @{ sha = $sha; branch = $Branch; deployedAt = (Get-Date).ToString('o') } |
        ConvertTo-Json | Set-Content -Path $StateFile -Encoding utf8
    Write-CdLog "已部署 $sha 。打开 http://localhost:8787 看左上角版本号。"
}

function Invoke-Once {
    docker info 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Docker 未运行（Docker Desktop Kubernetes 需要它拉 GHCR 镜像）。' }
    if (-not (Test-K8sDoinb)) {
        throw '找不到 namespace doinb。先按 deploy/k8s/README.md 把应用部署到本机 Kubernetes，不要用 docker compose。'
    }
    Connect-Ghcr
    $sha = Get-RemoteSha $Branch
    $prev = ''
    if (Test-Path $StateFile) {
        try { $prev = (Get-Content $StateFile -Raw | ConvertFrom-Json).sha } catch { $prev = '' }
    }
    if ($prev -eq $sha) {
        Write-CdLog "已是 origin/$Branch 的 $sha"
        return
    }
    Write-CdLog "检测到 origin/$Branch = $sha（上次 $prev）"
    Sync-LocalMain $Branch
    if (-not (Wait-GhcrImages $GhcrOwner $sha)) {
        throw "等不到 ghcr.io/$GhcrOwner/doinb-web:$sha。看 Actions 是否已绿，或设置 GHCR_TOKEN。"
    }
    Deploy-K8s $sha
    Save-State $sha
}

Write-CdLog "local-cd branch=$Branch watch=$Watch"
if (-not (Test-Path (Join-Path $Root '.env'))) {
    Copy-Item (Join-Path $Root '.env.example') (Join-Path $Root '.env')
}

if ($Watch) {
    while ($true) {
        try { Invoke-Once } catch { Write-CdLog "本轮失败: $($_.Exception.Message)" }
        Start-Sleep -Seconds $IntervalSeconds
    }
} else {
    Invoke-Once
}
