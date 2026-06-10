# doinb 视频网站（video_web）

课程项目 **doinb** 的前后端仓库：Spring Boot 后端 + Vue 3 用户端，支持视频点播、评论互动、关注、通知私信、创作中心等；直播为**房间状态管理 + 演示播放**（无真实推流）；管理员仅有后端 API，无独立管理端 UI。

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
| 后端 | Java 25、Spring Boot 4、Spring Security、MyBatis-Plus、MySQL、JWT |
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
│       ├── network/request.js
│       ├── utils/auth.js    # 多账号登录态
│       ├── router/
│       └── views/
├── database/
│   └── database.sql         # 全量建表（新库执行）
├── uploads/                 # 运行时上传目录（勿提交）
└── 功能测试和完善.md         # 迭代需求与增量 SQL
```

---

## 环境要求

- **JDK 25**
- **MySQL 8.x**
- **Node.js** 20.19+ 或 22.12+（见 `web/package.json`）
- **Git**

后端使用项目自带 Maven Wrapper（`backend/mvnw.cmd`），无需单独安装 Maven。

---

## 本地启动

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
| `/studio/edit` | 创作中心 · 修改视频 |
| `/profile` | 个人中心、播放历史 |
| `/user/:id` | 他人公开展示页（关注、私信） |
| `/messages/:roomId` | 私信会话 |
| `/login`、`/register` | 登录、注册 |

---

## 功能完成度

### 已实现（用户端）

- [x] 注册 / 登录 / 退出；单设备多账号切换
- [x] 视频列表、播放、播放进度、播放历史（含封面）
- [x] 视频上传；创作中心上传 / 编辑（标题、简介、封面、文件、状态）
- [x] 评论（Unicode + 图片表情）、视频/评论赞踩、转发链接
- [x] 关注 / 取消关注、关注动态
- [x] 搜索（视频 / 用户）
- [x] 个人资料（头像上传、简介）、公开展示页
- [x] 通知（点赞、私信）、私信会话
- [x] 直播列表、直播间页、直播评论（后端 API）

### 部分完成 / 演示级

- [ ] **直播**：仅有房间创建/开播/停播 API，**无真实推流**；前端暂无主播开播管理页，播放区为占位演示
- [ ] **管理员**：仅有 `/admin/account/login` 等后端接口；**无管理后台 UI**；`role=2` 可在后端获得更高权限

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

---

## Git 提交规范

**可以提交：**

- 源代码、`application.yml`、`application-local.example.yml`
- `database/database.sql`、`功能测试和完善.md`

**禁止提交：**

- `application-local.yml`（含真实密码）
- `node_modules/`、`backend/target/`
- `uploads/`、大体积视频文件
- `.env` 等本地密钥文件

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

### 8. 昵称或提示显示 `????`

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
| 评论 / 赞踩 | `CommentController`、`ReactionController` |
| 关注 | `SubscriptionController`、`FollowButton.vue` |
| 通知 / 私信 | `NotificationController`、`MessageController` |
| 直播 | `LiveRoomController`、`LiveListView.vue`、`LiveRoomView.vue` |
| 搜索 | `SearchController`、`SearchView.vue` |

后端分层：`Controller` → `Service` → `Mapper` → MySQL。

---

## 相关文档

- [`功能测试和完善.md`](功能测试和完善.md) — 版本迭代、测试项、数据库增量 SQL
- `设计说明书/` — 课程设计说明书与需求规格
