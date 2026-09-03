# 03_devops Docker / CI/CD / Kubernetes / 数据库

| 任务书要求 | 仓库路径 |
|---|---|
| Docker Compose（微服务） | `docker-compose.yml` |
| Docker Compose（对比用单体） | `docker-compose.mono.yml` |
| 镜像 Dockerfile | `services/Dockerfile`、`services/Dockerfile.video`、`web/Dockerfile` |
| CI/CD 流水线 | `.github/workflows/ci.yml` |
| 本机首次部署 | `scripts/k8s-up.ps1` |
| 本机跟随 CI 滚动更新 | `scripts/local-cd.ps1`、`deploy/local-cd.md` |
| Kubernetes 清单 | `deploy/k8s/`（`kustomization.yaml`、`java-services.yaml`、`mysql.yaml`、`hpa.yaml`、`web.yaml`） |
| HPA / 故障演示 | `deploy/k8s/hpa-demo.ps1`、`deploy/k8s/fault-demo.ps1` |
| 滚动更新 | `deploy/k8s/rollout.ps1`、`deploy/k8s/verify.ps1` |
| 数据库脚本 | `database/database.sql`、`database/seed.sql` |
| 部署说明 | `deploy/k8s/README.md`、`交付文档/部署文档.md` |

CD 镜像 tag 为 git 短 SHA，不用 `latest`。GitHub Actions 的 kind 部署老师浏览器打不开；答辩机用本机 Docker Desktop Kubernetes + `local-cd.ps1 -Watch`。
