# doinb 视频网站（video_web）

课程项目 **doinb** 的前后端仓库：Spring Boot 后端 + Vue 3 用户端，支持视频点播、评论互动、关注、通知私信、创作中心、管理后台等；直播为**房间状态管理 + OBS 推流地址 + 演示播放**（无内置浏览器推流）。

仓库地址：https://github.com/Lalalala-yeye/video_web_work.git

更细的需求与测试记录见 [`功能测试和完善.md`](功能测试和完善.md)。

---

## 写在前面

做特定功能时：

```bash
1. git pull origin main          # 先同步最新的 main
2. git checkout -b feature/xxx   # 新建自己的分支
3. 改代码、commit
4. git push origin feature/xxx   # 只 push 自己的分支
5. 在 GitHub 上开 Pull Request：feature/xxx → main
6. Merge
```

不要没 `git pull` 就开始写；务必新建分支再改，不要直接在 main 上覆盖。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 25、Spring Boot 4、Spring Security、MyBatis-Plus 3.5（Boot4 Starter）、MySQL、JWT |
| 前端 | Vue 3、Vite、Vue Router、Element Plus、Axios |
| 存储 | MySQL 存元数据；视频/封面/头像存本地 `uploads/` |

---

## 目录结构

```
video_web/
├── backend/                 # Spring Boot（端口 8080）
│   └── src/main/
│       ├── java/com/doinb/backend/
│       └── resources/
│           ├── application.yml
│           ├── application-local.example.yml   # 模板（可提交）
│           └── application-local.yml           # 本地配置（勿提交）
├── web/                     # Vue 用户端（端口 8787）
│   ├── public/              # favicon、like 图标、emojis 等静态资源
│   └── src/
│       ├── api/             # 按模块封装的接口
│       ├── components/      # 导航、卡片、评论、管理表格等
│       ├── network/request.js
│       ├── utils/auth.js    # 多账号登录态
│       ├── router/
│       └── views/           # 首页、播放、创作中心、管理后台等
├── docker-compose.yml       # 前端 + 后端 + MySQL 三容器
├── .env.example             # 容器环境变量模板
├── database/
│   ├── database.sql         # 建表（新库 / compose 首次自动执行）
│   ├── migrate.sql          # 旧库增量迁移
│   └── seed.sql             # 测试数据（compose 首次自动执行）
├── uploads/                 # 运行时上传目录（勿提交）
├── 交付文档/                 # 用户手册等交付材料
└── 功能测试和完善.md         # 迭代需求与增量 SQL
```

---

## 环境要求

- **Git**
- **用 Docker 启动（推荐换机器 / 课设容器化）**：安装 [Docker Desktop](https://www.docker.com/products/docker-desktop/) 即可，不必本机装 JDK / Node / MySQL。
- **本机开发**：JDK 25、MySQL 8.x、Node.js 20.19+ 或 22.12+（见 `web/package.json`）。后端用项目自带 Maven Wrapper（`backend/mvnw.cmd`）。

---

## 用 Docker 启动（换机器按这里做）

前端、后端、数据库各跑在独立容器里。数据库用官方 `mysql:8.0`；前后端用仓库里的 Dockerfile。首次会构建镜像，需要几分钟。

```powershell
git clone https://github.com/Lalalala-yeye/video_web_work.git
cd video_web
copy .env.example .env
docker compose up --build -d
```

打开 http://localhost:8787  

健康检查：http://localhost:8081/health  

| 容器 | 宿主机端口 | 说明 |
|------|------------|------|
| web | 8787 | Nginx 静态页，`/api` 反代到后端 |
| backend | 8081 | Spring Boot |
| mysql | 3307 | 映射到容器 3306，避免和本机 MySQL 抢端口 |

建表脚本 `database/database.sql` 与测试数据 `database/seed.sql` 仅在 **空数据卷第一次启动** 时自动执行。演示账号（密码均为 `123456`）：

| 用户名 | 角色 |
|--------|------|
| `demo_admin` | 管理员 |
| `demo_author` | 作者 |
| `demo_user` | 观众 |

本机已有早期数据库、缺列缺表时，手动执行 `database/migrate.sql`（compose 新库不必跑）。

```powershell
docker compose logs -f backend
docker compose down          # 停容器，保留数据
docker compose down -v       # 停容器并清空数据库（会丢演示数据）
```

变量名与 CI 相同，见 `.env.example`（`MYSQL_*`、`JWT_SECRET`）。不要把填好的 `.env` 提交进 Git。

---

## CI 测试门禁

`.github/workflows/ci.yml`（workflow 名 **Test gate**）三层，一层不过后面不跑：

| 顺序 | Job | 测什么 |
|------|-----|--------|
| 1 | Backend unit + API tests | JUnit 单测 + MockMvc，不启 MySQL |
| 2 | Postman Newman | 真库 API 冒烟（15 条） |
| 3 | Selenium E2E | 无头 Chrome 点页面：账号 / 互动 / 创作中心+直播 / 通知私信后台 |

E2E 需要管理员种子账号 `demo_admin` / `123456`（`database/seed.sql`）。OBS 真推流不进 CI。


---

## 本地启动（不容器化）

### 1. 克隆仓库

```bash
git clone https://github.com/Lalalala-yeye/video_web_work.git
cd video_web
```

### 2. 准备 MySQL

```sql
CREATE DATABASE doinb DEFAULT CHARACTER SET utf8mb4;
```

在 `doinb` 库中执行 **`database/database.sql`**（新库一次执行即可）。

若库是早期版本、已存在部分表，请对照 **`功能测试和完善.md`** 中的「数据库增量」段落按需补执行。

### 3. 配置后端

复制模板并填写本机账号密码与 JWT 密钥：

```powershell
copy backend\src\main\resources\application-local.example.yml backend\src\main\resources\application-local.yml
```

示例：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/doinb?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: "你的MySQL密码"

jwt:
  secret: "至少32个字符的随机字符串"
```

> **YAML 注意**：密码含 `@`、`:`、`#` 等特殊字符时，必须用英文双引号包裹，例如 `password: "@abc123"`。

### 4. 启动后端

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

健康检查：http://localhost:8080/health  

期望返回：

```json
{"code":200,"message":"OK","data":"doinb-backend ok"}
```

**改过后端 Java 代码后必须重新编译并重启**，否则会出现 `No static resource xxx` 等新接口 404。

### 5. 启动前端

新开终端（必须在 `web/` 目录）：

```powershell
cd web
npm install
npm run dev
```

浏览器访问：http://localhost:8787  

Vite 代理：`/api/*` → `http://localhost:8080/*`（见 `web/vite.config.js`）。

---

## 端口约定

| 服务 | 端口 | 说明 |
|------|------|------|
| 后端 HTTP | 8080 | REST API |
| 前端开发服务器 | 8787 | `npm run dev` |

修改后端端口时，请同步修改 `web/vite.config.js` 的 `proxy.target`。

---

## API 与鉴权约定

- 统一响应：`{ "code": 200, "message": "OK", "data": ... }`（`code !== 200` 为业务失败）
- 请求基路径：前端 `/api`，代理到后端根路径
- 登录后请求头：`Authorization: <token>`（支持 `Bearer xxx` 或直接 token）
- **多账号**：Token 与用户信息存 `localStorage.doinb_accounts`，当前活跃账号存 `doinb_active_id`；导航栏可切换/添加账号
- 静态媒体：`/api/uploads/**` 对应本地上传文件

---

## 前端主要路由

| 路径 | 说明 |
|------|------|
| `/` | 首页视频列表 |
| `/video/:id` | 视频播放、赞踩、评论 |
| `/live`、`/live/:id` | 直播列表、直播间（演示播放器 + 评论） |
| `/subscribe` | 关注动态 |
| `/search` | 搜索 |
| `/studio/upload` | 创作中心 · 上传视频 |
| `/studio/edit`、`/studio/edit/:id` | 创作中心 · 修改视频（左侧列表切换稿件） |
| `/studio/live` | 创作中心 · 我的直播（创建房间、开播/停播、OBS 推流说明） |
| `/admin/dashboard` | 管理后台 · 概览（需 `role=2`） |
| `/admin/pending` | 待审核稿件（新上传 / 修改后复审） |
| `/admin/report` | 举报待复核 |
| `/admin/preview/:id` | 管理员预览视频 |
| `/profile` | 个人中心、播放历史 |
| `/user/:id` | 他人公开展示页（关注、私信） |
| `/messages/:roomId` | 私信会话 |
| `/login`、`/register` | 登录、注册 |

---

## 功能完成度

### 已实现（用户端）

- [x] 注册 / 登录 / 退出；单设备多账号切换
- [x] 视频列表、播放、播放进度、播放历史（含封面）
- [x] 视频上传；创作中心上传 / 编辑（标题、简介、封面、文件、可见范围；切换稿件时自动清空上传缓存）
- [x] 评论（Unicode + 图片表情）、视频/评论赞踩、转发链接
- [x] 关注 / 取消关注、关注动态
- [x] 搜索（视频 / 用户）
- [x] 个人资料（头像上传、简介）、公开展示页
- [x] 通知（点赞、私信）、私信会话
- [x] 直播列表、直播间页、直播评论；创作中心直播房间管理
- [x] 管理后台 UI：待审核、举报复审、视频预览与通过/驳回

### 部分完成 / 演示级

- [ ] **直播播放**：支持房间状态与 OBS 推流地址展示，**无内置浏览器推流**；观众端播放区为演示占位
- [ ] **直播推流**：需自行使用 OBS 等工具推至提示的 RTMP 地址；浏览器屏幕分享尚未完善

### 基础设施

- [x] JWT 鉴权、统一异常与 UTF-8
- [x] 本地上传（`uploads/videos`、`uploads/covers`、`uploads/avatars`）

---

## 角色说明

| role | 含义 |
|------|------|
| 0 | 普通用户（历史数据，登录时会升级为发布者） |
| 1 | 发布者（注册默认；可上传、编辑自己的视频） |
| 2 | 管理员（可管理他人视频/直播间等；需数据库手动设置） |

设置管理员示例：

```sql
UPDATE users SET role = 2 WHERE username = '你的管理员账号';
```

管理员登录后，可在创作中心侧栏进入 **管理后台**，或直接访问 `/admin/dashboard`。

---

## Git 提交规范

**可以提交：**

- 源代码、`application.yml`、`application-local.example.yml`
- `database/database.sql`、`database/migrate.sql`、`database/seed.sql`
- `docker-compose.yml`、`.env.example`、`功能测试和完善.md`

**禁止提交：**

- `application-local.yml`（含真实密码）
- `node_modules/`、`backend/target/`
- `uploads/`、大体积视频文件
- `.env` 等本地密钥文件（提交 `.env.example` 即可）

提交前：

```powershell
git status
```

确认未误加 `application-local.yml` 或 `target/`。

---

## 常见问题

### 1. 后端启动报 YAML 错误（`found character '@'`）

密码未加引号。改为 `password: "@xxx"`。

### 2. 数据库连接失败

确认 MySQL 已启动、已建库 `doinb`、已执行 `database/database.sql`，并检查 `application-local.yml`。

### 3. 前端「网络错误」

先启后端（8080），再启前端；确认 Vite 代理指向正确。

### 4. `npm` 报 `ENOENT ... video_web\package.json`

必须在 **`web/`** 目录执行 `npm install` / `npm run dev`，不要在仓库根目录。

### 5. 接口报 `No static resource xxx`

后端未加载最新代码。在 `backend/` 下重新 `.\mvnw.cmd spring-boot:run`（或 IDE 重启应用）。

### 6. 新功能（通知、私信、视频编辑等）表不存在

对已有库执行 `功能测试和完善.md` 里 v0.2 等增量 SQL，或重建库并执行完整 `database/database.sql`。

### 7. 播放本地上传视频卡顿

演示视频建议 **50MB 以内、1080p 以下**；过大文件经开发代理播放可能占用大量内存。

### 8. 8080 端口被占用

旧的后端进程未退出。Windows 可先查占用：`netstat -ano | findstr :8080`，再结束对应 `java.exe` 进程，或直接使用已在运行的实例。

### 9. 登录报 500 / JWT 相关错误

`application-local.yml` 中 `jwt.secret` 长度须 **≥ 32 字符**。

### 10. 昵称或提示显示 `????`

多为历史脏数据或旧版本编码问题。可执行：

```sql
UPDATE users SET nickname = CONCAT('用户_', username) WHERE nickname LIKE '??_%';
```

并确认后端 `application.yml` 中 `spring.servlet.encoding` 为 UTF-8。

---

## 开发分工参考

| 模块 | 主要目录 |
|------|----------|
| 用户 / 鉴权 | `UserAccountController`、`UserController`、`web/src/utils/auth.js` |
| 视频 | `VideoController`、`web/src/views/studio/`、`VideoDetailView.vue` |
| 管理后台 | `AdminVideoController`、`web/src/views/admin/` |
| 评论 / 赞踩 | `CommentController`、`ReactionController` |
| 关注 | `SubscriptionController`、`FollowButton.vue` |
| 通知 / 私信 | `NotificationController`、`MessageController` |
| 直播 | `LiveRoomController`、`LiveListView.vue`、`LiveRoomView.vue` |
| 搜索 | `SearchController`、`SearchView.vue` |

后端分层：`Controller` → `Service` → `Mapper` → MySQL。

---

## 相关文档

- [`功能测试和完善.md`](功能测试和完善.md) — 版本迭代、测试项、数据库增量 SQL
- [`交付文档/用户手册.md`](交付文档/用户手册.md) — 面向最终用户的操作说明
- `设计说明书/` — 课程设计说明书与需求规格
