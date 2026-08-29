# 在另一台电脑导入「最新视频数据」
# 用法：先 git clone、copy .env、docker compose up -d，再：
#   .\scripts\import-data.ps1
#   .\scripts\import-data.ps1 -Archive D:\doinb-data.zip
param(
    [string]$Archive = ''
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

function Get-MysqlPassword {
    $password = 'test'
    $envFile = Join-Path $Root '.env'
    if (Test-Path $envFile) {
        Get-Content $envFile | ForEach-Object {
            if ($_ -match '^\s*MYSQL_PASSWORD=(.*)$') {
                $password = $Matches[1].Trim().Trim('"').Trim("'")
            }
        }
    }
    return $password
}

if (-not $Archive) {
    $Archive = Join-Path $Root 'doinb-data.zip'
}
if (-not (Test-Path $Archive)) {
    Write-Host "找不到数据包：$Archive" -ForegroundColor Red
    Write-Host '请先在旧电脑执行 .\scripts\export-data.ps1，再把 doinb-data.zip 拷过来。'
    exit 1
}

$running = docker ps --filter 'name=^doinb-mysql$' --format '{{.Names}}'
if (-not $running) {
    Write-Host '未找到正在运行的 doinb-mysql。请先：copy .env.example .env ； docker compose up -d' -ForegroundColor Red
    exit 1
}

$staging = Join-Path $Root '.doinb-data-import'
if (Test-Path $staging) {
    Remove-Item $staging -Recurse -Force
}
New-Item -ItemType Directory -Path $staging | Out-Null

Write-Host "正在解压 $Archive ..."
Expand-Archive -Path $Archive -DestinationPath $staging -Force

$sql = Get-ChildItem $staging -Filter 'doinb.sql' -Recurse | Select-Object -First 1
if (-not $sql) {
    throw 'zip 里没有 doinb.sql，请确认是用 export-data.ps1 导出的数据包'
}

$uploadsSrc = Get-ChildItem $staging -Directory -Recurse |
    Where-Object { $_.Name -eq 'uploads' } |
    Select-Object -First 1
$uploadsDst = Join-Path $Root 'backend\uploads'
if ($uploadsSrc) {
    Write-Host "正在覆盖 $uploadsDst ..."
    if (-not (Test-Path $uploadsDst)) {
        New-Item -ItemType Directory -Path $uploadsDst | Out-Null
    }
    Copy-Item (Join-Path $uploadsSrc.FullName '*') $uploadsDst -Recurse -Force
} else {
    Write-Warning 'zip 里没有 uploads 目录，视频文件可能缺失'
}

$password = Get-MysqlPassword
Write-Host '正在导入数据库（会覆盖当前库里的演示种子数据）...'
docker cp $sql.FullName 'doinb-mysql:/tmp/doinb.sql'
docker exec doinb-mysql mysql -uroot "-p$password" -e 'source /tmp/doinb.sql'
if ($LASTEXITCODE -ne 0) { throw '导入数据库失败，请检查 MYSQL_PASSWORD 是否与 .env 一致' }
docker exec doinb-mysql rm -f /tmp/doinb.sql | Out-Null

Remove-Item $staging -Recurse -Force
Write-Host '正在重启后端...'
docker compose restart backend | Out-Null

Write-Host '导入完成。打开 http://localhost:8787 应能看到旧电脑上的视频。' -ForegroundColor Green
