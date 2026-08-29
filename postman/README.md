# Postman / 系统测试脚本

## 全量 59 条（写测试报告用）

对着正在跑的后端（`http://127.0.0.1:8081`）按报告第 4 节顺序执行，把每条真实 JSON 写进 `postman/out/`，并刷新 `交付文档/测试报告.md`。

```powershell
# 项目根目录，先 docker compose up
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

## CI 冒烟 15 条（Newman）

Collection：`doinb.postman_collection.json`  
环境：`doinb.ci.postman_environment.json`（`baseUrl=http://127.0.0.1:8081`）

```bash
npx --yes newman@6 run postman/doinb.postman_collection.json -e postman/doinb.ci.postman_environment.json --bail
```

覆盖：H000、U000/U001/U002、U010/U011、U030/U031/U040、U061、S001、V000/V011、C001/C002。每条都有 Tests 断言。

## CI

`.github/workflows/ci.yml` 测试门禁顺序：

1. 后端单测 + MockMvc（不启 MySQL）
2. 起 MySQL → 建表 → 启动后端 → Newman（15 条冒烟）
3. Newman 通过后并行：Selenium E2E（失败不挡门禁）；`docker build` 推送 `ghcr.io/<owner>/doinb-backend:<sha>` 与 `doinb-web:<sha>`（无 `latest`）

单测或 Newman 失败则整条流水线红，后面不打镜像。E2E 失败仍会出截图 artifact，不阻止打镜像。

后端镜像 / compose / K8s 与 CI **同一套变量名**，不要再用 `SPRING_DATASOURCE_*`。完整表见 `backend/src/main/resources/application-ci.yml` 文件头注释。
