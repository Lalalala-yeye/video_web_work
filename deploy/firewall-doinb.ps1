# doinb 本地开发：放行 / 删除端口（需管理员 PowerShell）
# 用法：
#   放行：  powershell -ExecutionPolicy Bypass -File .\deploy\firewall-doinb.ps1 -Action add
#   删除：  powershell -ExecutionPolicy Bypass -File .\deploy\firewall-doinb.ps1 -Action remove

param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('add', 'remove')]
    [string] $Action
)

$ErrorActionPreference = 'Stop'

# 8787 前端 Vite | 8081 后端 | 8080 SRS HLS | 1935 SRS RTMP | 8000 SRS WebRTC
$Rules = @(
    @{ Name = 'doinb-8787'; Port = 8787; Proto = 'TCP'; Desc = 'doinb frontend vite' },
    @{ Name = 'doinb-8081'; Port = 8081; Proto = 'TCP'; Desc = 'doinb backend api' },
    @{ Name = 'doinb-8080'; Port = 8080; Proto = 'TCP'; Desc = 'doinb srs hls flv' },
    @{ Name = 'doinb-1935'; Port = 1935; Proto = 'TCP'; Desc = 'doinb srs rtmp' },
    @{ Name = 'doinb-8000-tcp'; Port = 8000; Proto = 'TCP'; Desc = 'doinb srs webrtc tcp' },
    @{ Name = 'doinb-8000-udp'; Port = 8000; Proto = 'UDP'; Desc = 'doinb srs webrtc udp' }
)

function Require-Admin {
    $id = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($id)
    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        Write-Host '请以管理员身份运行 PowerShell。' -ForegroundColor Red
        exit 1
    }
}

Require-Admin

foreach ($rule in $Rules) {
    $name = $rule.Name
    if ($Action -eq 'add') {
        netsh advfirewall firewall delete rule name="$name" 2>$null | Out-Null
        netsh advfirewall firewall add rule name="$name" dir=in action=allow protocol=$rule.Proto localport=$rule.Port `
            description=$rule.Desc | Out-Null
        Write-Host "[+] 已放行 $($rule.Proto) $($rule.Port) ($name)" -ForegroundColor Green
    } else {
        netsh advfirewall firewall delete rule name="$name" | Out-Null
        Write-Host "[-] 已删除规则 $name" -ForegroundColor Yellow
    }
}

# 清理旧版非标准端口规则（8088 HLS、8010 WebRTC、8080 后端）
$LegacyNames = @('doinb-8088', 'doinb-8010-tcp', 'doinb-8010-udp')
if ($Action -eq 'add') {
    foreach ($legacy in $LegacyNames) {
        netsh advfirewall firewall delete rule name="$legacy" 2>$null | Out-Null
    }
}

if ($Action -eq 'add') {
    Write-Host ''
    Write-Host '放行完成。手机访问示例: http://你的局域网IP:8787' -ForegroundColor Cyan
} else {
    Write-Host ''
    Write-Host '相关入站规则已删除。' -ForegroundColor Cyan
}
