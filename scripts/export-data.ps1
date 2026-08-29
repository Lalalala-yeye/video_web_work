# 导出当前这台机器上的「最新视频数据」：MySQL 全库 + backend/uploads
# 用法（项目根目录、容器已在跑）：
#   .\scripts\export-data.ps1
# 生成 doinb-data.zip，拷到另一台电脑后执行 import-data.ps1
param(
    [string]$OutFile = ''
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

$running = docker ps --filter 'name=^doinb-mysql$' --format '{{.Names}}'
if (-not $running) {
    Write-Host '未找到正在运行的 doinb-mysql。请先在本机执行：docker compose up -d' -ForegroundColor Red
    exit 1
}

if (-not $OutFile) {
    $OutFile = Join-Path $Root 'doinb-data.zip'
}

$staging = Join-Path $Root '.doinb-data-export'
if (Test-Path $staging) {
    Remove-Item $staging -Recurse -Force
}
New-Item -ItemType Directory -Path $staging | Out-Null

$password = Get-MysqlPassword
Write-Host "正在导出数据库 doinb ..."
docker exec doinb-mysql mysqldump `
    -uroot "-p$password" `
    --default-character-set=utf8mb4 `
    --single-transaction `
    --routines `
    --databases doinb `
    -r /tmp/doinb.sql
if ($LASTEXITCODE -ne 0) { throw 'mysqldump 失败，请检查 MYSQL_PASSWORD 是否与 .env 一致' }
docker cp 'doinb-mysql:/tmp/doinb.sql' (Join-Path $staging 'doinb.sql')
docker exec doinb-mysql rm -f /tmp/doinb.sql | Out-Null

$uploads = Join-Path $Root 'backend\uploads'
$destUploads = Join-Path $staging 'uploads'
if (Test-Path $uploads) {
    Write-Host "正在打包上传文件 $uploads ..."
    Copy-Item $uploads $destUploads -Recurse
} else {
    Write-Warning "没有找到 $uploads，数据包里将不含视频文件"
    New-Item -ItemType Directory -Path $destUploads | Out-Null
}

if (Test-Path $OutFile) {
    Remove-Item $OutFile -Force
}

Write-Host "正在压缩 $OutFile ..."
Compress-Archive -Path (Join-Path $staging '*') -DestinationPath $OutFile -CompressionLevel Fastest
Remove-Item $staging -Recurse -Force

$sizeMb = [math]::Round((Get-Item $OutFile).Length / 1MB, 1)
Write-Host "导出完成：$OutFile （约 ${sizeMb} MB）" -ForegroundColor Green
Write-Host '请把这个 zip 拷到另一台电脑的项目根目录，再执行：.\scripts\import-data.ps1'
