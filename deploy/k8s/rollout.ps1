param(
    [string]$Namespace = "doinb",
    [string]$Registry = "ghcr.io/lalalala-yeye",
    [string]$BackendTag = "cde7310",
    [string]$WebTag = "cde7310",
    [string]$Timeout = "5m"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

foreach ($tag in @($BackendTag, $WebTag)) {
    if ([string]::IsNullOrWhiteSpace($tag) -or $tag -eq "latest") {
        throw "Image tags must be explicit and cannot be 'latest'."
    }
}

function Invoke-Kubectl {
    & kubectl @args
    if ($LASTEXITCODE -ne 0) {
        throw "kubectl $($args -join ' ') failed with exit code $LASTEXITCODE"
    }
}

$backendImage = "$Registry/doinb-backend:$BackendTag"
$webImage = "$Registry/doinb-web:$WebTag"

Write-Host "Updating backend to $backendImage"
Invoke-Kubectl set image deployment/backend "backend=$backendImage" -n $Namespace

Write-Host "Updating web to $webImage"
Invoke-Kubectl set image deployment/web "web=$webImage" -n $Namespace

Invoke-Kubectl rollout status deployment/backend -n $Namespace "--timeout=$Timeout"
Invoke-Kubectl rollout status deployment/web -n $Namespace "--timeout=$Timeout"

& "$PSScriptRoot/verify.ps1" -Namespace $Namespace -Timeout $Timeout
if ($LASTEXITCODE -ne 0) {
    throw "Post-rollout verification failed with exit code $LASTEXITCODE"
}
