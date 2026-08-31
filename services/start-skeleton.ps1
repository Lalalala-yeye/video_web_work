$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$mvnw = Join-Path $root "mvnw.cmd"

Write-Host "编译骨架..."
& $mvnw -f (Join-Path $root "pom.xml") -q -DskipTests package
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$modules = @(
    "doinb-user",
    "doinb-video",
    "doinb-live",
    "doinb-interact",
    "doinb-message",
    "doinb-gateway"
)

foreach ($module in $modules) {
    $cmd = "Set-Location '$root'; .\mvnw.cmd -pl $module -am spring-boot:run"
    Start-Process powershell -ArgumentList @("-NoExit", "-Command", $cmd) | Out-Null
    Write-Host "已启动 $module"
}

Write-Host "等各窗口出现 Started 后执行 .\verify-skeleton.ps1"
