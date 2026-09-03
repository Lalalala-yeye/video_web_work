param(
    [string]$Namespace = "doinb",
    [string]$Timeout = "5m"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Invoke-Kubectl {
    $kubectlArgs = foreach ($a in $args) {
        if ($a -is [System.Array]) { $a -join ',' } else { $a }
    }
    & kubectl @kubectlArgs
    if ($LASTEXITCODE -ne 0) {
        throw "kubectl $($kubectlArgs -join ' ') failed with exit code $LASTEXITCODE"
    }
}

$apps = @("mysql", "user", "video", "live", "interact", "message", "gateway", "web")
Write-Host "Waiting for $($apps -join ', ')..."
foreach ($app in $apps) {
    Invoke-Kubectl rollout status "deployment/$app" -n $Namespace "--timeout=$Timeout"
}

Write-Host "`nCurrent workloads:"
Invoke-Kubectl get "pods,services" -n $Namespace -o wide

$checkPod = "doinb-healthcheck-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
$checkCommand = 'curl -fsS http://gateway:8081/health && echo && curl -fsS http://gateway:8081/ready && echo && curl -fsS http://gateway:8081/version && echo && curl -fsS http://user:8082/health && echo && curl -fsS http://video:8083/health && echo && curl -fsS http://web/ >/dev/null && curl -fsS http://web/api/health && echo'

Write-Host "`nChecking gateway /health /ready /version, user, video, and web /api/health..."
Invoke-Kubectl run $checkPod -n $Namespace --image=curlimages/curl:8.12.1 --restart=Never --rm -i --command "--" sh -c $checkCommand

Write-Host "`nOK: probes passed."
Write-Host "To access locally, run: kubectl port-forward service/web 8080:80 -n $Namespace"
