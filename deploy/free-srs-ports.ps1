# 释放 SRS 标准端口（需管理员 PowerShell）
# 用法：powershell -ExecutionPolicy Bypass -File .\deploy\free-srs-ports.ps1

$ErrorActionPreference = 'Stop'

function Require-Admin {
    $id = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($id)
    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        Write-Host '请以管理员身份运行此脚本。' -ForegroundColor Red
        exit 1
    }
}

Require-Admin

Write-Host 'Stopping old SRS container...'
docker rm -f srs-live 2>$null | Out-Null

$ports = @(8000, 8080)
foreach ($port in $ports) {
    $lines = netstat -ano | Select-String "LISTENING" | Select-String ":$port\s"
    foreach ($line in $lines) {
        $pid = ($line -split '\s+')[-1]
        if ($pid -match '^\d+$') {
            $proc = Get-Process -Id $pid -ErrorAction SilentlyContinue
            $name = if ($proc) { $proc.ProcessName } else { 'unknown' }
            Write-Host "Port $port -> PID $pid ($name)"
            if ($name -eq 'java') {
                Write-Host '  Stopping Spring Boot (will restart on 8081)...' -ForegroundColor Yellow
                taskkill /F /PID $pid 2>$null
            } elseif ($name -eq 'Manager') {
                Write-Host '  Stopping Manager.exe (occupies WebRTC port 8000)...' -ForegroundColor Yellow
                taskkill /F /PID $pid 2>$null
            } else {
                Write-Host "  Skipped. Stop $name manually if needed." -ForegroundColor Yellow
            }
        }
    }
}

Write-Host ''
Write-Host 'Port check:' -ForegroundColor Cyan
netstat -ano | Select-String 'LISTENING' | Select-String ':8000 |:8080 '
