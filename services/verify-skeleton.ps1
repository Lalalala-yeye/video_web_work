$ErrorActionPreference = "Continue"

function Assert-Health([string]$name, [string]$url) {
    try {
        $resp = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 5
        if ($resp.code -eq 200) {
            Write-Host "OK  $name  $($resp.data)"
        } else {
            Write-Host "FAIL $name  code=$($resp.code) $($resp.message)"
        }
    } catch {
        Write-Host "FAIL $name  $_"
    }
}

Assert-Health "gateway " "http://127.0.0.1:8080/health"
Assert-Health "user    " "http://127.0.0.1:8082/health"
Assert-Health "video   " "http://127.0.0.1:8083/health"
Assert-Health "live    " "http://127.0.0.1:8084/health"
Assert-Health "interact" "http://127.0.0.1:8085/health"
Assert-Health "message " "http://127.0.0.1:8086/health"

try {
    $search = Invoke-RestMethod -Uri "http://127.0.0.1:8080/search?keyword=test" -Method Get -TimeoutSec 8
    Write-Host "OK  search   videos=$($search.data.videos.Count) lives=$($search.data.liveRooms.Count) users=$($search.data.users.Count)"
} catch {
    Write-Host "FAIL search  $_"
}
