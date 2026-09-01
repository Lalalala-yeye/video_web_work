# doinb 微服务

网关 + 5 个业务服务，可分别构建、测试、打镜像、部署。

CI / compose / K8s 对外入口是 **网关 8081**（前端 `/api` 与 Newman 不用改路径）。本机直接 `spring-boot:run` 时网关默认仍是 **8080**，避免和旧进程抢端口；compose 会把网关映射到 8081。

## 端口

| 进程 | 端口 | 探活 |
|------|------|------|
| 网关 `doinb-gateway` | 8080 本机 / 8081 CI | `/health` 存活，`/ready` 就绪，`/version` 版本 |
| 用户 `doinb-user` | 8082 | 同上（`/ready` 会探数据库） |
| 视频 `doinb-video` | 8083 | 同上 |
| 直播 `doinb-live` | 8084 | 同上 |
| 互动 `doinb-interact` | 8085 | 同上 |
| 消息 `doinb-message` | 8086 | 同上 |

## 启动

本机 JDK 25，在 `services/` 下分别启动，或仓库根目录：

```powershell
docker compose up -d --build
```

探活：

```powershell
curl http://127.0.0.1:8081/health
curl http://127.0.0.1:8081/ready
curl http://127.0.0.1:8081/version
```

业务服务同样有这三个接口（8082–8086）。看日志：

```powershell
docker compose logs -f gateway user video live interact message
```

CI 打镜像：`services/Dockerfile`，`--build-arg MODULE=doinb-user --build-arg PORT=8082`。

## 约定

- 对外路径与清单一致
- 服务间同步 HTTP + `X-Internal-Token`
- 网关验 JWT 后注入 `X-User-Id`、`X-User-Role`
- 浏览器打不到 `/internal/**`
- 返回 `{code,message,data}`
- `JWT_SECRET`、`DOINB_INTERNAL_TOKEN` 全组相同
