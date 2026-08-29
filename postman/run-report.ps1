# 全量系统测试（59 条）
# 前置：docker compose 已起，http://127.0.0.1:8081/health 为 200
# 用法（项目根目录）：
#   .\postman\run-report.ps1
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
node postman/run-full-report.mjs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
node postman/generate-report-md.mjs
Write-Host '完成。原始 JSON：postman/out/results.json ；报告：交付文档/测试报告.md'
