# 课程提交包（对照任务书 01～06）

打包时把本目录打成 zip 即可。代码仍在仓库根目录开发，这里是**交作业用的目录对照**，避免把正在跑的工程挪乱。

仓库地址见 `01_source/仓库清单.md`。

| 任务书目录 | 放什么 | 本仓库对应位置 |
|---|---|---|
| `01_source` | 代码或仓库清单 | `services/`、`web/`、`backend/`（单体对照）、本目录清单 |
| `02_docs` | 需求、设计、测试、追溯、模型源文件 | `文档-已确认/` |
| `03_devops` | Docker、流水线、K8s、数据库脚本 | `.github/workflows/ci.yml`、`deploy/`、`docker-compose.yml`、`database/`、`scripts/` |
| `04_tests` | 自动化测试、压测、原始报告、实验数据 | `services/*/src/test`、`web/e2e/`、`postman/`、`bench/`、`文档-已确认/测试报告.md` |
| `05_management` | 站会、看板、贡献权重 | `文档-已确认/任务卡片/`（过程分工）；截图请自行补进本目录 |
| `06_defense` | PPT、技术总结、备用材料 | `答辩.md`、`答辩具体脚本.md`、`outputs/.../doinb-video-platform-showcase.pptx` |

各子目录里的 `README.md` 是给老师点开就能对上的索引。
