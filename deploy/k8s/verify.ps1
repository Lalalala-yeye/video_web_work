param(
    [string]$Namespace = "doinb",
    [string]$Timeout = "5m"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Invoke-Kubectl {
    & kubectl @args
    if ($LASTEXITCODE -ne 0) {
        throw "kubectl $($args -join ' ') failed with exit code $LASTEXITCODE"
    }
}

Write-Host "Waiting for MySQL, backend and web..."
Invoke-Kubectl rollout status deployment/mysql -n $Namespace "--timeout=$Timeout"
Invoke-Kubectl rollout status deployment/backend -n $Namespace "--timeout=$Timeout"
Invoke-Kubectl rollout status deployment/web -n $Namespace "--timeout=$Timeout"

Write-Host "`nCurrent workloads:"
Invoke-Kubectl get "pods,services" -n $Namespace -o wide

$checkPod = "doinb-healthcheck-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
$checkCommand = "curl -fsS http://backend:8081/health && echo && curl -fsS http://web/ > /dev/null"

Write-Host "`nChecking backend /health and the web Service from inside the cluster..."
Invoke-Kubectl run $checkPod -n $Namespace --image=curlimages/curl:8.12.1 --restart=Never --rm -i --command "--" sh -c $checkCommand

Write-Host "`nOK: backend /health returned successfully and web / was reachable."
Write-Host "To access locally, run: kubectl port-forward service/web 8080:80 -n $Namespace"
