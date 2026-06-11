# doinb 前端备忘 · 表情包目录

> 更新：2026-06。核心业务功能已基本可用，**AI 相关能力后续再做**；当前阶段以**前端体验与界面打磨**为主。

---

## 组员本地跑起来（前端开发）

**环境**：Node 20.19+ 或 22.12+、JDK 25、MySQL 8、Git。只改页面可先不管直播；要测直播还需 **Docker Desktop**。

**1. 拉代码**（先 `git pull`，再 `git checkout -b feature/你的名字`）

```powershell
git clone https://github.com/Lalalala-yeye/video_web_work.git
cd video_web
```

**2. 数据库**（首次一次即可）

```sql
CREATE DATABASE doinb DEFAULT CHARACTER SET utf8mb4;
```

在 `doinb` 里执行仓库根目录 `database/database.sql`。老库缺表时对照根目录 `功能测试和完善.md` 补增量 SQL。

**3. 后端**

```powershell
copy backend\src\main\resources\application-local.example.yml backend\src\main\resources\application-local.yml
```

改 `application-local.yml` 里的 MySQL 密码和 `jwt.secret`（至少 32 字符）。密码含 `@`、`:` 等请用英文双引号，例如 `password: "@abc"`。

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

浏览器打开 http://localhost:8081/health ，应看到 `doinb-backend ok`。**改过 Java 后必须重启后端**，否则新接口会 404。

**4. 前端**（新开一个终端）

```powershell
cd web
npm install
npm run dev
```

访问 http://localhost:8787 。`/api` 由 Vite 代理到 `8081`，一般**只开这两个服务**就能开发点播、评论、创作中心、管理页等。

**5. 直播（可选）**

```powershell
.\deploy\srs-docker.ps1
```

创作中心用 **OBS 开播**（服务器 `rtmp://本机IP:1935/live` + 推流码）。局域网观看请用 **IP 打开网站**（如 `http://192.168.x.x:8787`），不要只用 `localhost`。Windows 防火墙可执行 `deploy\firewall-doinb.ps1`。

**常见踩坑**

| 现象 | 处理 |
|------|------|
| 前端能开但接口全失败 | 确认后端在 **8081** 跑着，且 `health` 正常 |
| 登录/注册报错 | 检查 MySQL 是否启动、`doinb` 是否已建表 |
| 改 Vue 不生效 | 保存后应热更新；不行就重启 `npm run dev` |
| 直播间黑屏 | 先确认 OBS 已推流，再 `curl http://127.0.0.1:1985/api/v1/streams/` 看 `frames>0` |

更完整的仓库说明见根目录 [`README.md`](../../../README.md)。

---

## 项目进度（简）

| 模块 | 状态 | 说明 |
|------|------|------|
| 视频点播 | ✅ | 上传、审核、播放、评论、点赞 |
| 用户 / 关注 / 搜索 | ✅ | 登录、多账号、个人页、订阅动态 |
| 直播 | ✅ | OBS + SRS 推流，观众页 HLS/FLV 播放；浏览器屏幕分享暂未开放 |
| 管理后台 | ✅ | 待审视频、举报复审 |
| AI 能力 | ⏸ | 暂缓，接口与 UI 后续单独立项 |
| 前端 UI/UX | 🔄 | **当前重点**：布局、动效、组件统一、移动端适配 |

本地开发步骤见上文「组员本地跑起来」。

---

## 表情包图片放哪？

把 PNG 放在**本目录**（与 README 同级）：

```
web/public/emojis/
├── smile.png
├── thumbs-up.png
├── laugh.png
├── heart.png
├── party.png
├── cool.png
└── README.md
```

Vite 将 `public/` 原样映射到站点根路径：

- 磁盘：`web/public/emojis/smile.png`
- 访问：`http://localhost:8787/emojis/smile.png`
- 配置：`/emojis/smile.png`（`web/src/constants/emojis.js` → `IMAGE_EMOJIS`）

**不要**放在 `src/assets/`，也**不要**走后端 `/uploads/`（那是用户视频/头像）。

> 当前目录仅有 README，尚无 png。评论里图片表情会显示占位失败，补齐上述文件即可。

---

## 两种表情怎么用

| 类型 | 配置 | 插入评论 | 展示 |
|------|------|----------|------|
| Unicode | `UNICODE_EMOJIS` | 直接插入 😊 | 文字 emoji |
| 图片 | `IMAGE_EMOJIS` | 插入 `[微笑]` | 渲染为 `<img src="/emojis/smile.png">` |

新增图片表情：

1. 将 `xxx.png` 放入本目录  
2. 在 `web/src/constants/emojis.js` 的 `IMAGE_EMOJIS` 增加一项，例如：  
   `{ name: '[doge]', file: '/emojis/doge.png', label: 'doge' }`

评论解析与渲染：`emojis.js` 的 `parseCommentContent`；组件 `CommentItem.vue`、`EmojiPicker.vue`。

可从 teriteri 等项目的 `/public/emoji/` 复制 png，按文件名与配置对齐即可。

---

## 前端改动常用路径

```
web/src/views/          # 页面（Home、Live、Studio、Admin…）
web/src/components/     # 通用组件
web/src/constants/      # 静态配置（含 emojis.js）
web/src/utils/          # 播放、鉴权、局域网地址等
web/public/             # 静态资源（本目录、like、图标等）
```

设计参考：`static/Web UI Template Design/`（如有冲突以线上实际页面为准）。

---

## 后续前端可排期（不含 AI）

- [ ] 补齐 `public/emojis/*.png` 资源
- [ ] 统一创作中心 / 直播间 / 列表页视觉（间距、卡片、空状态）
- [ ] 移动端导航与播放器交互
- [ ] 直播列表封面、在线人数等展示优化
- [ ] 构建与部署文档（`npm run build` + Nginx 反代）

AI 相关需求确定后再单独开分支，不在本轮前端改造范围内。
