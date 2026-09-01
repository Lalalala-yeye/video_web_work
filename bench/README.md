# 课设第 4 项：单体 vs 微服务 对比压测

同一份脚本、同一组参数，**同一台机器先后**打改造前单体和改造后微服务。组员可以一起记表、截图，但不要两人两台电脑各测一边。

需要 **Node 18+**（前端 `web/` 已经在用），没有 k6 / JMeter。

## 测哪 3 个接口

两边路径一致，直接打 Java 端口（默认 **8081**），不要打前端 8787。

| 接口 | 方法 | 路径 | 为什么选它 |
|---|---|---|---|
| 登录 | POST | `/user/account/login` | CPU（BCrypt）+ 写路径 |
| 视频列表 | GET | `/video/list?page=1&size=12` | 最常见读路径 |
| 搜索 | GET | `/search?keyword=像素` | 微服务会经网关扇出到 user/video/live |

账号用 seed：`demo_user` / `123456`。库必须是同一份 `database/seed.sql`。

## 组员约定（必须相同，否则不能比）

| 项 | 值 |
|---|---|
| 并发 VU | 50 |
| 每轮时长 | 30 秒 |
| 预热 | 10 秒（丢弃，不记表） |
| 每接口轮次 | 3 |
| 微服务副本 | 全 1，**关掉 HPA** |
| 故障注入 | **不要开** |

机器扛不住时，全组改成 `--vus 20`，两边都用 20，不要一边 50 一边 20。

## 改造前（老单体）怎么起

根目录现在的 `docker-compose.yml` 已经是微服务，**不会**再起 `doinb-backend`。老系统在 `backend/`，入口同样是 **8081**。

先看健康检查是不是单体：

```text
GET http://127.0.0.1:8081/health
```

返回 `"data": "doinb-backend ok"` 就是老的，可以直接压。若是 `"doinb-gateway ok"`，说明 8081 被微服务占着，先停微服务再起单体。

本机已经有旧容器（`docker ps` 能看到 `doinb-backend`）时，**不要重建**，先把本机另起的 Java（例如 E2E 的 18081）停掉，避免抢 CPU，然后：

```powershell
node bench/run.mjs --label mono --base http://127.0.0.1:8081
```

没有旧容器的组员，用单独的单体 compose（和微服务抢 8081 / 3307，同一时刻只能起一套）：

```powershell
# 若 8081 已被微服务占用：
docker compose down

docker compose -f docker-compose.mono.yml up --build -d
# 等 health 变成 doinb-backend ok 再压
node bench/run.mjs --label mono --base http://127.0.0.1:8081

# 测完再换成微服务
docker compose -f docker-compose.mono.yml down
docker compose up --build -d
node bench/run.mjs --label micro --base http://127.0.0.1:8081
```

不要用 `cd backend; mvnw spring-boot:run` 去和 Docker 里的微服务比：一边容器一边宿主机 JVM，数字不能用。

## 怎么跑

先确认目标活着：

```text
http://127.0.0.1:8081/health
```

**先测哪边都行，但同一时刻只起一套。** 测完停掉，再起另一套，避免抢 CPU。

```powershell
# 改造后：当前仓库 docker compose（网关 8081）
cd <仓库根目录>
node bench/run.mjs --label micro --base http://127.0.0.1:8081
```

```powershell
# 改造前：见上一节，health 必须是 doinb-backend ok
node bench/run.mjs --label mono --base http://127.0.0.1:8081
```

只测某一个接口：

```powershell
node bench/run.mjs --label micro --scenario video-list
```

先看接口通不通、不加压：

```powershell
node bench/run.mjs --label micro --dry-run
```

可选参数：`--vus 50 --duration 30 --warmup 10 --rounds 3 --keyword 像素 --user demo_user --password 123456`

跑完会打印 Markdown 表，并写入：

- `bench/results/out/<时间>_micro.json`
- `bench/results/out/<时间>_micro.md`

把表贴进 `bench/TEMPLATE.md`。Docker 若能采到 `doinb-backend` 或 `doinb-gateway` 等容器，CPU/内存会自动填；采不到就用任务管理器或 `kubectl top` 补。

## 环境注意

- 打 **8081 后端**，不要打 Vite / nginx 的 `/api`。
- 微服务多一跳网关，这是「改造后」的一部分，对比时写进说明即可。
- 本机若另起过 E2E（例如 18081），不要拿它和 Docker 8081 混着比。
- 登录接口会打满 CPU，属预期；看错误率是否接近 0，以及 P95 差多少。

## 记什么

任务书要：并发、吞吐、平均耗时、P95、错误率、CPU、内存。脚本表头已经按这个出。每个接口、每个版本 3 轮都记，对比时用 3 轮平均。
