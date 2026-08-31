# doinb 微服务骨架

单体 `backend/` 先不要动。本目录是拆进程用的空壳：网关验 JWT、按路径转发，5 个业务服务只提供 `/health` 和内部接口桩。

前端联调入口暂时是 **8080**（避免和现网单体 8081 抢端口）。切流时把网关改成 8081，停掉单体即可。

## 端口

| 进程 | 端口 | 说明 |
|------|------|------|
| 网关 `doinb-gateway` | 8080 | 对外入口、`/search` 聚合 |
| 用户 `doinb-user` | 8082 | |
| 视频 `doinb-video` | 8083 | |
| 直播 `doinb-live` | 8084 | |
| 互动 `doinb-interact` | 8085 | |
| 消息 `doinb-message` | 8086 | |

## 启动

本机已装 JDK 25。在 `services/` 下：

```powershell
.\start-skeleton.ps1
```

或分别开窗口：

```powershell
.\mvnw.cmd -pl doinb-gateway -am spring-boot:run
.\mvnw.cmd -pl doinb-user -am spring-boot:run
.\mvnw.cmd -pl doinb-video -am spring-boot:run
.\mvnw.cmd -pl doinb-live -am spring-boot:run
.\mvnw.cmd -pl doinb-interact -am spring-boot:run
.\mvnw.cmd -pl doinb-message -am spring-boot:run
```

探活：

```powershell
.\verify-skeleton.ps1
```

应看到网关与 5 个服务的 `/health` 为 200，`GET /search?keyword=test` 返回空列表（桩）。

## 约定（不要改）

- 对外路径与单体一致，见 `文档-已确认/服务接口清单.md`
- 服务间同步 HTTP + JSON，头：`X-Internal-Token`
- 网关验 JWT 后注入 `X-User-Id`、`X-User-Role`（`user` / `admin`）
- 浏览器打不到 `/internal/**`
- 返回体继续用 `{code,message,data}`
- JWT 密钥、内部令牌全组相同：`JWT_SECRET`、`DOINB_INTERNAL_TOKEN`

任务卡片：`文档-已确认/任务卡片/`
内部接口：`文档-已确认/内部接口契约.md`
