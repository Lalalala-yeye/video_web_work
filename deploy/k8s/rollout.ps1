param(
    [string]$Namespace = "doinb",
    [string]$Registry = "ghcr.io/lalalala-yeye",
    [string]$Tag = "cde7310",
    [string]$Timeout = "5m"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if ([string]::IsNullOrWhiteSpace($Tag) -or $Tag -eq "latest") {
    throw "Image tags must be explicit and cannot be 'latest'."
}

function Invoke-Kubectl {
    & kubectl @args
    if ($LASTEXITCODE -ne 0) {
        throw "kubectl $($args -join ' ') failed with exit code $LASTEXITCODE"
    }
}

$modules = @(
    @{ Deploy = "user"; Container = "user"; Image = "$Registry/doinb-user:$Tag" },
    @{ Deploy = "video"; Container = "video"; Image = "$Registry/doinb-video:$Tag" },
    @{ Deploy = "live"; Container = "live"; Image = "$Registry/doinb-live:$Tag" },
    @{ Deploy = "interact"; Container = "interact"; Image = "$Registry/doinb-interact:$Tag" },
    @{ Deploy = "message"; Container = "message"; Image = "$Registry/doinb-message:$Tag" },
    @{ Deploy = "gateway"; Container = "gateway"; Image = "$Registry/doinb-gateway:$Tag" },
    @{ Deploy = "web"; Container = "web"; Image = "$Registry/doinb-web:$Tag" }
)

foreach ($m in $modules) {
    Write-Host "Updating $($m.Deploy) to $($m.Image)"
    Invoke-Kubectl set image "deployment/$($m.Deploy)" "$($m.Container)=$($m.Image)" -n $Namespace
}

Invoke-Kubectl set env configmap/doinb-runtime "APP_VERSION=$Tag" -n $Namespace
foreach ($m in $modules) {
    if ($m.Deploy -eq "web") { continue }
    Invoke-Kubectl rollout restart "deployment/$($m.Deploy)" -n $Namespace
}

foreach ($m in $modules) {
    Invoke-Kubectl rollout status "deployment/$($m.Deploy)" -n $Namespace "--timeout=$Timeout"
}

& "$PSScriptRoot/verify.ps1" -Namespace $Namespace -Timeout $Timeout
if ($LASTEXITCODE -ne 0) {
    throw "Post-rollout verification failed with exit code $LASTEXITCODE"
}
