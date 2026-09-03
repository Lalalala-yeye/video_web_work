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
kubectl apply -f deploy/k8s/namespace.yaml

kubectl create secret generic doinb-secrets -n doinb `
  --from-literal=MYSQL_ROOT_PASSWORD='test' `
  --from-literal=MYSQL_PASSWORD='test' `
  --from-literal=JWT_SECRET='compose-demo-secret-at-least-32-chars!!' `
  --from-literal=DOINB_INTERNAL_TOKEN='doinb-internal-dev-token'

kubectl create configmap doinb-db-init -n doinb `
  --from-file=001-schema.sql=database/database.sql `
  --from-file=002-seed.sql=database/seed.sql

kubectl apply -k deploy/k8s
kubectl get pods -n doinb
```

镜像来自 GHCR。若 Pod 是 `ImagePullBackOff`，先 `docker login ghcr.io`（需要 `read:packages` 的 PAT）。

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
