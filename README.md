前置条件：Docker Desktop 打开 Kubernetes（Settings → Kubernetes → Enable），本机有 `kubectl`。

## 第一步 克隆仓库

```powershell
git clone https://github.com/Lalalala-yeye/video_web_work
cd video_web_work
git checkout main
```

## 第二步 部署到本机 Kubernetes

演示环境在 namespace `doinb`，不用 docker compose。清单和 HPA 说明见 `deploy/k8s/README.md`。

```powershell
copy .env.example .env
.\scripts\k8s-up.ps1
```

脚本会建 namespace、Secret、数据库初始化脚本，`kubectl apply -k deploy/k8s`，再把镜像换成当前 `origin/main` 的 SHA。本机 16GB 默认各服务 1 个副本（`.\scripts\k8s-up.ps1 -Replicas 2` 与课设清单一致）。

镜像来自 GHCR。若报 401 / ImagePullBackOff：

```powershell
$env:GHCR_USER = '你的GitHub用户名'
$env:GHCR_TOKEN = 'read:packages 的 PAT'
.\scripts\k8s-up.ps1
```

## 进行检查

浏览器打开：

网站：http://localhost:8787  
健康检查：http://localhost:8787/api/health  

（`web-local` 把前端映射到 8787，不必 port-forward。）

## push 后本机网站自动变（答辩用）

GitHub 上的 kind 老师用浏览器打不开。演示机先开着：

```powershell
.\scripts\local-cd.ps1 -Watch
```

再 push。CI 打完镜像后脚本会更新本机 `doinb` 里的 Deployment。刷新 http://localhost:8787，左上角版本号会变成新的 git SHA。说明见 `deploy/local-cd.md`。
