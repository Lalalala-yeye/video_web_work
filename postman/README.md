# Postman / Newman（真库 API 冒烟）

Collection：`doinb.postman_collection.json`  
环境：`doinb.ci.postman_environment.json`（`baseUrl=http://127.0.0.1:8081`）

覆盖报告用例：H000、U000/U001/U002、U010/U011、U030/U031/U040、U061、S001、V000/V011、C001/C002。  
每条都有 Tests 断言。不要把没有断言的请求加进 CI。

## 本地

1. MySQL 建库并执行 `database/database.sql`
2. 后端用能连上该库的配置启动（端口 8081）
3. 安装 Newman 后执行：

```bash
npx --yes newman@6 run postman/doinb.postman_collection.json -e postman/doinb.ci.postman_environment.json --bail
```

也可在 Postman 里 Import 这两个 JSON 后手工跑。注册用户名每次自动生成 `ci_<时间戳>`，可重复跑。

## CI

`.github/workflows/ci.yml` 测试门禁顺序：

1. 后端单测 + MockMvc（不启 MySQL）
2. 起 MySQL → 建表 → 启动后端 → Newman
3. Newman 通过后并行：Selenium E2E（失败不挡门禁）；`docker build` 推送 `ghcr.io/<owner>/doinb-backend:<sha>` 与 `doinb-web:<sha>`（无 `latest`）

单测或 Newman 失败则整条流水线红，后面不打镜像。E2E 失败仍会出截图 artifact，不阻止打镜像。


后端镜像 / compose / K8s 与 CI **同一套变量名**，不要再用 `SPRING_DATASOURCE_*`。完整表见 `backend/src/main/resources/application-ci.yml` 文件头注释。

本机若用同一套变量（不要把真实密码提交进仓库）：

```bash
export SPRING_PROFILES_ACTIVE=ci
export MYSQL_HOST=127.0.0.1 MYSQL_PORT=3306 MYSQL_DATABASE=doinb
export MYSQL_USER=root MYSQL_PASSWORD=你的密码
export JWT_SECRET=至少32个字符的随机串
```
