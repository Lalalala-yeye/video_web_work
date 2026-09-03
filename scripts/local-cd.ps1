# 本机跟 GitHub：push → Actions 打镜像 → 本脚本只更新本机 Kubernetes（namespace doinb），不用 Compose。
# 答辩演示：先开 -Watch，浏览器开 8787，push 后等 CI 绿，刷新页面左上角版本号会变，不要手动 kubectl。
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

function Test-WorkingTreeClean {
    return [string]::IsNullOrWhiteSpace((git status --porcelain))
}

function Sync-Repo([string]$branch) {
    if (-not (Test-WorkingTreeClean)) {
        Write-CdLog '工作区有未提交改动，跳过 git pull。'
        return $false
    }
    $current = (git branch --show-current).Trim()
    if ($current -ne $branch) { git checkout $branch }
    git merge --ff-only "origin/$branch"
    if ($LASTEXITCODE -ne 0) { throw "无法快进 origin/$branch" }
    return $true
}

function Wait-GhcrImages([string]$owner, [string]$sha) {
    $deadline = (Get-Date).AddMinutes($WaitImagesMinutes)
    while ((Get-Date) -lt $deadline) {
        $missing = @()
        foreach ($m in $Modules) {
            docker pull "ghcr.io/${owner}/${m}:${sha}" 2>&1 | Out-Null
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
    kubectl patch configmap doinb-runtime -n doinb --type merge -p "{`"data`":{`"APP_VERSION`":`"$sha`"}}"
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
    if (-not (Sync-Repo $Branch)) { return }
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
