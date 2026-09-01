# Postman / 系统测试脚本

## 全量公开接口（CI / 写报告）

对着正在跑的网关（`http://127.0.0.1:8081`）按顺序打完整集：接口清单里的对外路径，以及网关 `/health` `/ready` `/version`。

```powershell
# 项目根目录，先 docker compose up 或 ci-start.sh
npx --yes newman@6 run postman/doinb.full.postman_collection.json -e postman/doinb.ci.postman_environment.json --working-dir . --bail
```

写测试报告（把每条真实 JSON 落盘）：

```powershell
.\postman\run-report.ps1
```

或：

```powershell
node postman/run-full-report.mjs
node postman/generate-report-md.mjs
```

原始响应：`postman/out/results.json`、`postman/out/bodies/<编号>.json`。

管理员默认 `demo_admin` / `123456`（可用环境变量 `REPORT_ADMIN_USER`、`REPORT_ADMIN_PASSWORD`）。测试账号每次现场注册 `rpt_a_<时间戳>`。

Postman 导入：`postman/doinb.full.postman_collection.json` + `postman/doinb.ci.postman_environment.json`，**按顺序跑整集**。Newman：

```powershell
npx --yes newman@6 run postman/doinb.full.postman_collection.json -e postman/doinb.ci.postman_environment.json --working-dir . --reporters cli,json --reporter-json-export postman/out/newman-report.json
```

`--working-dir .` 必须指向仓库根，上传才会找到 `web/e2e/fixtures/test-video.mp4`。报告里的「实际输出」以 `run-full-report.mjs` 落盘的 JSON 为准（Newman 报表不含逐条完整 body 进 md）。

## CI 冒烟 15 条（本地快速）

Collection：`doinb.postman_collection.json`（CI **不再**只用这一集，门禁用全量集合）  
环境：`doinb.ci.postman_environment.json`（`baseUrl=http://127.0.0.1:8081`）

```bash
npx --yes newman@6 run postman/doinb.postman_collection.json -e postman/doinb.ci.postman_environment.json --bail
```

覆盖：H000、U000/U001/U002、U010/U011、U030/U031/U040、U061、S001、V000/V011、C001/C002。每条都有 Tests 断言。

## CI

`.github/workflows/ci.yml` 测试门禁顺序：

1. `services/` 五个业务服务 + 网关单测（不启 MySQL）
2. 起 MySQL → 建表 + seed → 起五服务 + 网关 8081 → Newman **全量公开接口**
3. Newman 通过后并行：Selenium E2E（15 个 UC；失败不挡门禁）；打 6 个 Java 镜像 + web
4. kind 部署本次 SHA，探活网关 `/health` `/ready` `/version`；Newman job 的 summary 里可看各服务日志和探活 JSON

单测或 Newman 失败则整条流水线红，后面不打镜像。E2E 失败仍会出截图 artifact，不阻止打镜像。

镜像 / compose / K8s 与 CI **同一套变量名**：`MYSQL_*`、`JWT_SECRET`、`DOINB_INTERNAL_TOKEN`、`DOINB_*_URL`。
