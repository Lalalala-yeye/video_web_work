# doinb 视频网站（video_web）

课程项目 **doinb** 的前后端代码仓库，仿 B 站风格的视频平台。当前已完成**项目骨架**与前后端联调，业务功能（登录、视频、评论等）按模块迭代开发中。

仓库地址：https://github.com/Lalalala-yeye/video_web_work.git

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 25、Spring Boot 4、Spring Security、MyBatis-Plus、MySQL、JWT |
| 前端 | Vue 3、Vite、Vue Router、Element Plus、Axios |

---

## 目录结构

```
video_web/
├── backend/          # Spring Boot 后端（端口 8080）
│   └── src/main/
│       ├── java/com/doinb/backend/
│       └── resources/
│           ├── application.yml                 # 公共配置（可提交）
│           ├── application-local.example.yml   # 本地配置模板（可提交）
│           └── application-local.yml           # 个人本地配置（勿提交）
└── web/              # Vue 用户端（端口 8787）
    └── src/
        ├── network/request.js   # Axios 封装
        ├── router/
        └── views/
```

---

## 环境要求

请提前安装：

- **JDK 25**
- **MySQL 8.x**（本机安装并启动服务）
- **Node.js** 20.19+ 或 22.12+（见 `web/package.json` 的 `engines`）
- **Git**

后端使用项目自带的 Maven Wrapper，**不需要单独安装 Maven**（直接用 `mvnw.cmd`）。

---

## 快速开始（给组员）

### 1. 克隆仓库

```bash
git clone https://github.com/Lalalala-yeye/video_web_work.git
cd video_web
```

### 2. 准备 MySQL

在本机 MySQL 中执行（只需一次）：

```sql
CREATE DATABASE doinb DEFAULT CHARACTER SET utf8mb4;
```

每人使用**自己的本机 MySQL**，数据互不影响。

### 3. 配置后端本地密钥

```bash
cd backend/src/main/resources
copy application-local.example.yml application-local.yml
```

编辑 `application-local.yml`，填写你自己的 MySQL 账号密码，并设置 JWT 密钥，例如：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/doinb?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: "你的MySQL密码"

jwt:
  secret: "随便写一串足够长的随机字符串"
```

> **注意（YAML 语法）**  
> 若密码以 `@`、`:`、`#` 等特殊字符开头或包含特殊字符，**必须用英文双引号包起来**，例如 `password: "@abc123"`。  
> 否则 Spring Boot 启动时会报 YAML 解析错误。

`application-local.yml` 已在 `.gitignore` 中，**不要提交到 GitHub**。

### 4. 启动后端

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

启动成功后访问：http://localhost:8080/health  

应返回：

```json
{"code":200,"message":"OK","data":"doinb-backend ok"}
```

### 5. 启动前端

新开一个终端：

```powershell
cd web
npm install
npm run dev
```

浏览器打开：http://localhost:8787  

首页应显示后端健康检查结果（`doinb-backend ok`）。

前端通过 Vite 代理访问后端：`/api/*` → `http://localhost:8080/*`（见 `web/vite.config.js`）。

---

## 端口约定

| 服务 | 端口 | 说明 |
|------|------|------|
| 后端 HTTP | 8080 | REST API |
| 前端开发服务器 | 8787 | `npm run dev` |

修改后端端口时，请同步修改 `web/vite.config.js` 里 `proxy.target`。

---

## API 约定（前后端统一）

- 统一返回格式：`{ "code": 200, "message": "OK", "data": ... }`
- 登录后 Token 存于浏览器：`localStorage.doinb_token`
- 请求头携带：`Authorization: <token>`（登录模块实现后生效）

---

## 当前进度

- [x] 后端骨架（Security、CORS、统一返回、健康检查）
- [x] 前端骨架（路由、Axios、Element Plus、联调 `/health`）
- [ ] 用户注册 / 登录
- [ ] 视频播放 / 上传
- [ ] 评论 / 搜索
- [ ] 订阅 / 直播

---

## Git 提交规范

**可以提交：**

- 源代码、`application.yml`、`application-local.example.yml`
- 数据库建表 SQL（后续放在 `backend/database/`）

**禁止提交：**

- `application-local.yml`（含真实密码）
- `node_modules/`、`backend/target/`
- 视频文件、`uploads/` 目录
- `.env` 等本地环境文件

提交前建议执行：

```powershell
git status
```

确认没有误加入 `application-local.yml` 或 `target/`。

---

## 常见问题

### 1. 后端启动报 YAML 错误（`found character '@'`）

密码未加引号。把 `password: @xxx` 改成 `password: "@xxx"`。

### 2. 后端报数据库连接失败

- 确认 MySQL 服务已启动
- 确认已创建数据库 `doinb`
- 检查 `application-local.yml` 中的用户名、密码

### 3. 前端首页显示「网络错误」

- 确认后端已在 8080 端口运行
- 确认先启后端，再启前端

### 4. `npm install` 报 Node 版本不符

升级 Node 到 20.19+ 或 22.12+，或使用 nvm 切换版本。

---

## 开发分工建议

| 模块 | 主要目录 |
|------|----------|
| 用户 / 鉴权 | `backend/.../controller`、`service/user` |
| 视频 | `backend/.../video`、`web/src/views` |
| 评论 / 搜索 | 按模块新建 Controller + Service + Mapper |

后端分层约定：`Controller` → `Service` → `Mapper` → MySQL。

---

## 联系方式

有问题先在组内沟通；配置类问题可对照本 README 的「快速开始」逐步排查。
