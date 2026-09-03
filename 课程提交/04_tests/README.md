# 04_tests 自动化测试 / 压测 / 原始报告 / 实验数据

| 任务书要求 | 仓库路径 | 关键数据 |
|---|---|---|
| 单元测试 | `services/*/src/test`、`backend/src/test/**/*ServiceImplTest.java` | 单体 179 + MockMvc 80；微服务 CI 约 171 |
| 系统/集成（打网关） | `postman/doinb.full.postman_collection.json`、`postman/run-full-report.mjs` | 报告 **59/59** |
| 端到端 | `web/e2e/01-auth.js`～`05-msg-admin.js`、截图 `web/e2e/artifacts/` | 报告 54 检查点，覆盖 UC-01～15 |
| 测试报告 | `文档-已确认/测试报告.md` | 原始 JSON：`postman/out/`（若本机跑过） |
| 压测脚本 | `bench/run.mjs`、`bench/run.ps1` | 50 VU / 30s / 3 轮 |
| 实验数据 | `bench/TEMPLATE.md`、`bench/results/out/` | 单体 vs 微服务：登录约 33 req/s 持平；列表 645 vs 116；搜索 474 vs 83；错误率 0 |
| HPA / 故障原始操作 | `deploy/k8s/hpa-demo.ps1`、`fault-demo.ps1` | 现场终端输出即原始记录 |

CI 门禁：单测或 Newman 失败不打镜像；E2E `continue-on-error`。
