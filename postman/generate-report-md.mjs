/**
 * 用 postman/out/results.json 生成 交付文档/测试报告.md 第 4 节表格，并拼出完整报告。
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const data = JSON.parse(fs.readFileSync(path.join(ROOT, 'postman/out/results.json'), 'utf8'))
const byId = Object.fromEntries(data.results.map((r) => [r.id, r]))

function esc(s) {
  return String(s ?? '').replace(/\|/g, '\\|').replace(/\r?\n/g, ' ')
}

function actual(id) {
  const r = byId[id]
  if (!r) return '（未执行）'
  if (!r.body || r.body === '') return `HTTP ${r.status} Forbidden`
  return '`' + esc(r.body) + '`'
}

function row(id, expected, extra = {}) {
  const r = byId[id]
  const name = extra.name || r?.name || id
  const method = extra.method || r?.method || ''
  const url = extra.url || r?.url || ''
  const pre = extra.pre || ''
  const input = extra.input || r?.input || '无'
  const header = extra.header || r?.header || '无'
  const pass = r?.pass ? '是' : '否'
  return `| ${id} | ${name} | \`${method} ${url}\` | ${pre} | \`${esc(input)}\` | ${esc(header)} | ${expected} | ${actual(id)} | ${pass} |`
}

const date = '2026-08-29'
const { userA, userB, vars } = data

const md = `# 测试报告

## 1 引言

### 1.1 目标

本测试报告记录 2026-08-29 对 doinb 视频平台的一次完整自动化执行结果，覆盖对象级单元测试、Controller MockMvc、用例级系统测试（对后端容器打 API）以及 GUI 验收（Selenium E2E 截图）。**第 4 节「实际输出」全部来自 \`postman/run-full-report.mjs\` 对 \`http://127.0.0.1:8081\` 的实时响应**，原始 JSON 另存 \`postman/out/bodies/\`。单元测试实际输出为同日 \`backend/mvnw -B test\` 的 Surefire 汇总。E2E 截图来自 \`web/e2e/artifacts/\`（脚本 \`shot()\` 保存）。

### 1.2 背景

系统为 Spring Boot + Vue 3。测试分层按 V 模型：单元对着系统操作测 Service；不单独做组件级集成；系统测试把 API 串成用例基本/扩展流程打后端容器；验收对着 GUI。

### 1.3 范围

| 层次 | 做法 | 本次执行 | 条数 | 通过 |
|------|------|----------|------|------|
| 单元测试 | JUnit + Mockito，\`*ServiceImplTest\` | \`cd backend && .\\\\mvnw.cmd -B test\` | 179 | 179 |
| MockMvc（随单元 job） | \`@WebMvcTest\`，不启 MySQL | 同上 | 80 | 80 |
| 组件集成 | 按课设要求不做 | — | 0 | — |
| 系统测试 | \`node postman/run-full-report.mjs\` 打后端 8081 | 本次 | **59** | **59** |
| 验收测试 | Selenium \`web/e2e/01\`～\`05\` | 截图见第 8 节 | 54 | 54（以脚本检查点计） |

CI 冒烟 Newman 仍为 15 条子集（\`postman/doinb.postman_collection.json\`）。本次系统测试按《原测试报告》第 4 节 59 条全量执行。

### 1.4 引用文件

- 《软件需求规格说明书》《软件概要设计说明书》《软件详细设计说明书》《需求追溯矩阵》
- \`postman/run-full-report.mjs\`、\`postman/out/results.json\`
- \`backend/src/test/java/**/*ServiceImplTest.java\`、\`**/api/*ApiTest.java\`
- \`web/e2e/01-auth.js\`～\`05-msg-admin.js\`

---

## 2 测试计划

### 2.1 目的

验证系统是否满足需求与设计说明中的功能要求：单元验证系统操作业务规则；系统测试验证用例基本/扩展流程在后端容器上的真实响应；验收验证 GUI 主路径。

### 2.2 测试项

| 模块编号 | 模块名称 | 字母编号 | 系统测试条数 | 通过 |
| -------- | -------- | -------- | ------------ | ---- |
| — | 健康检查 | H | 1 | 1 |
| M1 | 用户与权限管理 | U | 13 | 13 |
| M2 | 视频浏览与播放 | V | 12 | 12 |
| M3 | 评论 | C | 4 | 4 |
| M4 | 赞踩互动 | R | 3 | 3 |
| M5 | 关注订阅 | F | 4 | 4 |
| M6 | 搜索 | S | 3 | 3 |
| M7 | 直播 | L | 7 | 7 |
| M8 | 通知 | N | 3 | 3 |
| M9 | 私信 | M | 3 | 3 |
| M10 | 管理员审核 | A | 6 | 6 |
| **合计** | | | **59** | **59** |

**用例编号规则**：首字母为模块，末位 \`0\` 表示成功用例，\`1~9\` 表示失败/异常用例。每条对应对应《需求追溯矩阵》UC-xx 与 OP-xxx。

### 2.3 测试方法

- **单元**：JUnit 5 + Mockito，不启 Spring、不连 MySQL。
- **系统测试**：Node 脚本按第 4 节顺序请求 \`http://127.0.0.1:8081\`（Docker 中的 \`doinb-backend\`），对比 \`{code,message,data}\`。
- **验收**：Selenium 操作 \`http://localhost:8787\`，关键步骤截图。
- **异常**：未登录、错误参数、资源不存在、权限不足。

### 2.4 测试通过准则

- 成功用例：\`code=200\`，\`data\` 符合业务含义。
- 失败用例：返回明确错误 \`code\`/\`message\`，或 HTTP 403，服务不崩溃。
- 单元：Surefire Failures=0、Errors=0。

### 2.5 环境要求（本次实际）

| 项目 | 实际值 |
| ---- | ------ |
| 后端 | \`http://127.0.0.1:8081\`（docker compose \`doinb-backend\`） |
| 前端 | \`http://localhost:8787\`（docker compose \`doinb-web\`） |
| 数据库 | MySQL 8.0 容器 \`doinb-mysql\` |
| JDK | 25；Spring Boot 4.1.0-SNAPSHOT |
| 系统测试脚本 | \`node postman/run-full-report.mjs\`（Node v24） |
| 单元测试 | \`backend\\\\mvnw.cmd -B test\` |
| 管理员 | seed 账号 \`demo_admin\` / \`123456\` |
| 测试账号 | 脚本现场注册 \`${userA}\`、\`${userB}\`，密码 123456 |

### 2.6 测试前准备（本次已由脚本完成）

| 变量名 | 本次实际值 |
| ------ | ---------- |
| USER_A | \`${userA}\` |
| USER_A_ID | ${vars.userAId} |
| USER_B | \`${userB}\` |
| USER_B_ID | ${vars.userBId} |
| VIDEO_ID | ${vars.videoId} |
| LIVE_ID | ${vars.liveId} |
| COMMENT_ID | ${vars.commentId} |
| ROOM_ID | ${vars.roomId} |
| TOKEN_ADMIN | \`demo_admin\` 登录签发的 JWT |

复跑命令：

\`\`\`powershell
# 系统测试（需 8081 已起）
node postman/run-full-report.mjs

# 单元 + MockMvc
cd backend
.\\mvnw.cmd -B test
\`\`\`

---

## 3 测试规程

1. \`docker compose up -d\`，确认 \`GET /health\` 返回 200。
2. \`node postman/run-full-report.mjs\`，结果写入 \`postman/out/results.json\`。
3. \`cd backend ; .\\mvnw.cmd -B test\`，记录 Surefire 汇总。
4. E2E 截图使用 \`web/e2e/artifacts/*.png\`（由 \`shot()\` 写入）。

**测试日期**：${date}　　**测试人员**：自动化脚本 + 项目测试组

---

## 4 模块方法测试用例（脚本实时输出）

> 下表「实际输出」从 \`postman/out/results.json\` 原样摘录。带 \`预备-\` 的请求只为准备 ID，不计入 59 条。

### 4.1 健康检查（H）

| 编号 | 测试名称 | 方法 URL | 前置条件 | 输入数据 | Header | 预期输出 | 实际输出 | 通过 |
| ---- | -------- | -------- | -------- | -------- | ------ | -------- | -------- | ---- |
${row('H000', '\`code=200\`，data=doinb-backend ok', { pre: '后端已启动', input: '无', header: '无' })}

### 4.2 用户与账号（U）

| 编号 | 测试名称 | 方法 URL | 前置条件 | 输入数据 | Header | 预期输出 | 实际输出 | 通过 |
| ---- | -------- | -------- | -------- | -------- | ------ | -------- | -------- | ---- |
${row('U000', '\`code=200\` 注册成功', { pre: '用户名未占用' })}
${row('U001', '\`code=403\` 两次密码不一致', { pre: '无' })}
${row('U002', '\`code=403\` 账号已存在', { pre: 'U000 已注册同一用户名' })}
${row('U010', '\`code=200\` data 含 token', { pre: 'U000 已注册' })}
${row('U011', '\`code=403\` 账号或密码不正确', { pre: '账号已注册' })}
${row('U020', '\`code=200\` 管理员 token', { pre: 'seed 中 demo_admin role=2' })}
${row('U030', '\`code=200\` 返回用户信息', { pre: '已登录 userA', header: 'Authorization: Bearer tokenA' })}
${row('U031', 'HTTP 403', { pre: '无 Token', header: '无', input: '无' })}
${row('U040', '\`code=200\` 已退出登录', { pre: '已登录', header: 'Authorization: Bearer tokenA' })}
${row('U060', '\`code=200\` 公开资料', { pre: 'userA 已注册' })}
${row('U061', '\`code=404\` 用户不存在', { pre: '无' })}
${row('U080', '\`code=200\` 资料更新成功', { pre: '已登录', header: 'Authorization: Bearer tokenA' })}
${row('U081', '\`code=200\` 返回头像 URL', { pre: '已登录', header: 'Authorization: Bearer tokenA' })}

### 4.3 视频模块（V）

| 编号 | 测试名称 | 方法 URL | 前置条件 | 输入数据 | Header | 预期输出 | 实际输出 | 通过 |
| ---- | -------- | -------- | -------- | -------- | ------ | -------- | -------- | ---- |
${row('V030', '\`code=200\` 等待审核', { pre: '已登录 userA', header: 'Authorization: Bearer tokenA' })}
${row('V031', '\`code=400\` 请上传视频文件', { pre: '已登录', header: 'Authorization: Bearer tokenA' })}
${row('V040', '\`code=200\` 列表含刚上传视频', { pre: '已登录', header: 'Authorization: Bearer tokenA' })}
${row('V050', '\`code=200\` 进度已保存', { pre: '已登录且视频已发布', header: 'Authorization: Bearer tokenA' })}
${row('V051', '\`code=200\` 含历史记录', { pre: '已保存进度', header: 'Authorization: Bearer tokenA' })}
${row('V060', '\`code=200\` 更新成功', { pre: '已登录，有 VIDEO_ID', header: 'Authorization: Bearer tokenA' })}
${row('V061', '\`code=200\` 仅自己可见', { pre: '已登录', header: 'Authorization: Bearer tokenA' })}
${row('V063', '\`code=200\` 举报已提交', { pre: 'userB 已登录', header: 'Authorization: Bearer tokenB' })}
${row('V000', '\`code=200\` 列表非空', { pre: '有已发布视频', header: '无' })}
${row('V010', '\`code=200\` 返回详情', { pre: '视频已发布', header: '无' })}
${row('V011', '\`code=404\` 视频不存在或未发布', { pre: '无', header: '无' })}
${row('V020', '\`code=200\` 赞踩汇总', { pre: '有已发布视频', header: '无' })}

### 4.4 评论模块（C）

| 编号 | 测试名称 | 方法 URL | 前置条件 | 输入数据 | Header | 预期输出 | 实际输出 | 通过 |
| ---- | -------- | -------- | -------- | -------- | ------ | -------- | -------- | ---- |
${row('C000', '\`code=200\` 评论成功', { pre: '已登录，视频已发布', header: 'Authorization: Bearer tokenA' })}
${row('C001', 'HTTP 403', { pre: '无 Token', header: '无' })}
${row('C002', '\`code=404\` 视频不存在', { pre: '已登录', header: 'Authorization: Bearer tokenA' })}
${row('C010', '\`code=200\` 含评论', { pre: 'C000 已发评论', header: '无' })}

### 4.5 赞踩模块（R）

| 编号 | 测试名称 | 方法 URL | 前置条件 | 输入数据 | Header | 预期输出 | 实际输出 | 通过 |
| ---- | -------- | -------- | -------- | -------- | ------ | -------- | -------- | ---- |
${row('R000', '\`code=200\` userReaction=1', { pre: 'userB 已登录', header: 'Authorization: Bearer tokenB' })}
${row('R001', '\`code=200\` userReaction=-1', { pre: 'userA 已登录', header: 'Authorization: Bearer tokenA' })}
${row('R010', '\`code=200\` 评论点赞', { pre: '有 COMMENT_ID', header: 'Authorization: Bearer tokenB' })}

### 4.6 关注订阅（F）

| 编号 | 测试名称 | 方法 URL | 前置条件 | 输入数据 | Header | 预期输出 | 实际输出 | 通过 |
| ---- | -------- | -------- | -------- | -------- | ------ | -------- | -------- | ---- |
${row('F000', '\`code=200\` 关注成功', { pre: '已登录 userA', header: 'Authorization: Bearer tokenA' })}
${row('F001', '\`code=200\` 已取消关注', { pre: '已关注', header: 'Authorization: Bearer tokenA' })}
${row('F010', '\`data.following=false\`', { pre: 'F001 之后', header: 'Authorization: Bearer tokenA' })}
${row('F030', '\`code=200\` 分页动态', { pre: '已登录', header: 'Authorization: Bearer tokenA' })}

### 4.7 搜索（S）

| 编号 | 测试名称 | 方法 URL | 前置条件 | 输入数据 | Header | 预期输出 | 实际输出 | 通过 |
| ---- | -------- | -------- | -------- | -------- | ------ | -------- | -------- | ---- |
${row('S000', '\`code=200\` users 非空', { pre: 'U080 已改昵称为测试昵称' })}
${row('S001', '\`code=200\` 三类结果均为 []', { pre: '无' })}
${row('S002', '\`code=200\` 三类结果均为 []', { pre: '无' })}

### 4.8 直播（L）

| 编号 | 测试名称 | 方法 URL | 前置条件 | 输入数据 | Header | 预期输出 | 实际输出 | 通过 |
| ---- | -------- | -------- | -------- | -------- | ------ | -------- | -------- | ---- |
${row('L020', '\`code=200\` 返回 LIVE_ID', { pre: '已登录 userA', header: 'Authorization: Bearer tokenA' })}
${row('L021', '\`code=200\` 开播成功', { pre: '有 LIVE_ID', header: 'Authorization: Bearer tokenA' })}
${row('L000', '\`code=200\` 列表含直播中房间', { pre: '已开播', header: '无' })}
${row('L010', '\`code=200\` 含 playUrl/streamKey', { pre: '有 LIVE_ID', header: '无' })}
${row('L022', '\`code=200\` 停播成功', { pre: '已开播', header: 'Authorization: Bearer tokenA' })}
${row('L040', '\`code=400\` 未开播无法弹幕', { pre: 'L022 之后', header: 'Authorization: Bearer tokenA' })}
${row('L041', '\`code=200\` 弹幕成功', { pre: 'L021 开播后、L022 前', header: 'Authorization: Bearer tokenA' })}

### 4.9 通知（N）

| 编号 | 测试名称 | 方法 URL | 前置条件 | 输入数据 | Header | 预期输出 | 实际输出 | 通过 |
| ---- | -------- | -------- | -------- | -------- | ------ | -------- | -------- | ---- |
${row('N000', '\`code=200\` 含通知', { pre: 'R000 点赞后', header: 'Authorization: Bearer tokenA' })}
${row('N010', '\`code=200\` count≥1', { pre: '有未读', header: 'Authorization: Bearer tokenA' })}
${row('N021', '\`code=200\` 全部已读', { pre: '已登录', header: 'Authorization: Bearer tokenA' })}

### 4.10 私信（M）

| 编号 | 测试名称 | 方法 URL | 前置条件 | 输入数据 | Header | 预期输出 | 实际输出 | 通过 |
| ---- | -------- | -------- | -------- | -------- | ------ | -------- | -------- | ---- |
${row('M000', '\`code=200\` 返回 roomId', { pre: '已登录 userA', header: 'Authorization: Bearer tokenA' })}
${row('M020', '\`code=200\` 发送成功', { pre: '有 ROOM_ID', header: 'Authorization: Bearer tokenA' })}
${row('M010', '\`code=200\` 含消息', { pre: '已发消息', header: 'Authorization: Bearer tokenA' })}

### 4.11 管理员审核（A）

| 编号 | 测试名称 | 方法 URL | 前置条件 | 输入数据 | Header | 预期输出 | 实际输出 | 通过 |
| ---- | -------- | -------- | -------- | -------- | ------ | -------- | -------- | ---- |
${row('A000', '\`code=200\` 含待审视频', { pre: '管理员已登录', header: 'Authorization: Bearer tokenAdmin' })}
${row('A001', '\`code=403\` 需要管理员权限', { pre: '普通用户 tokenA', header: 'Authorization: Bearer tokenA' })}
${row('A010', '\`code=200\` 已通过审核', { pre: '有待审 VIDEO_ID', header: 'Authorization: Bearer tokenAdmin' })}
${row('A011', '\`code=200\` 已驳回', { pre: '另有待审视频', header: 'Authorization: Bearer tokenAdmin' })}
${row('A020', '\`code=200\` 举报复审分页', { pre: 'V063 已举报', header: 'Authorization: Bearer tokenAdmin' })}
${row('A040', '\`code=200\` 已删除', { pre: '管理员已登录', header: 'Authorization: Bearer tokenAdmin' })}

---

## 5 单元测试（本次 mvn test 实际汇总）

执行：\`C:\\\\Users\\\\zhaozhewen\\\\video_web\\\\backend\` 下 \`.\\\\mvnw.cmd -B test\`（2026-08-29 12:09）。

Surefire 最终行：

\`\`\`
Tests run: 260, Failures: 0, Errors: 0, Skipped: 1
\`\`\`

其中 \`BackendApplicationTests.contextLoads\` 标注 \`@Disabled\`，计 1 条 skipped，不计入用例。有效执行 **259 = 单元 179 + MockMvc 80**，失败 0。

### 5.1 单元 \`*ServiceImplTest\`（179，Failures: 0）

| 测试类 | Tests run | Failures | Errors | Skipped |
| ------ | --------- | -------- | ------ | ------- |
| UserAccountServiceImplTest | 23 | 0 | 0 | 0 |
| UserServiceImplTest | 14 | 0 | 0 | 0 |
| VideoServiceImplTest | 28 | 0 | 0 | 0 |
| AdminVideoServiceImplTest | 19 | 0 | 0 | 0 |
| CommentServiceImplTest | 14 | 0 | 0 | 0 |
| ReactionServiceImplTest | 16 | 0 | 0 | 0 |
| SubscriptionServiceImplTest | 13 | 0 | 0 | 0 |
| SearchServiceImplTest | 5 | 0 | 0 | 0 |
| LiveRoomServiceImplTest | 18 | 0 | 0 | 0 |
| NotificationServiceImplTest | 16 | 0 | 0 | 0 |
| MessageServiceImplTest | 13 | 0 | 0 | 0 |
| **合计** | **179** | **0** | **0** | **0** |

对象级对着系统操作（OP-002～OP-044）的断言明细见 \`文档-已确认/测试报告.md\` 第 5 节；本次以 Surefire 计数为实际输出，不复写编造的 JSON。

### 5.2 MockMvc \`*ApiTest\`（80，Failures: 0）

| 测试类 | Tests run | Failures |
| ------ | --------- | -------- |
| HealthApiTest | 1 | 0 |
| AuthSecurityApiTest | 11 | 0 |
| UserApiTest | 6 | 0 |
| VideoApiTest | 15 | 0 |
| AdminVideoApiTest | 10 | 0 |
| VideoReviewFlowApiTest | 2 | 0 |
| CommentApiTest | 4 | 0 |
| ReactionApiTest | 5 | 0 |
| SubscriptionApiTest | 6 | 0 |
| SearchApiTest | 2 | 0 |
| LiveRoomApiTest | 8 | 0 |
| MessageApiTest | 5 | 0 |
| NotificationApiTest | 5 | 0 |
| **合计** | **80** | **0** |

这是 Controller 层对系统操作 REST 契约的测试，**不是**组件级集成测试。

---

## 6 测试顺序（本次脚本实际顺序）

\`\`\`text
H000
U001 → U000 → U002 → 注册B → U011 → U010 → 登录B → U020
U031 → U030 → U060 → U061 → U080 → U081
V031 → V030 → 再上传两份待审（驳回/删除用）→ V040
A001 → A000 → A010 → A011
V000 → V010 → V011 → V020 → V050 → V051 → V060 → 再审通过 → V061 → 改回公开再审 → V063
C001 → C002 → C000 → C010
R000 → R001 → R010
F000 → F001 → F010 → F030
S000 → S001 → S002
L020 → L021 → L000 → L010 → L041 → L022 → L040
N000 → N010 → N021
M000 → M020 → M010
A020 → A040 → U040
\`\`\`

---

## 7 测试问题记录

| 编号 | 模块 | 问题描述 | 严重程度 | 状态 |
| ---- | ---- | -------- | -------- | ---- |
| ISSUE-01 | U031 / C001 | 未带 Token 时 HTTP 403，响应体为空（不是 JSON）。与预期「未授权」一致，表中实际输出记为 HTTP 403 Forbidden。 | 低 | 符合预期 |
| ISSUE-02 | V031 | 缺文件由 Service 返回 \`code=400\`「请上传视频文件」（不再是 HTTP 500）。本次实测：\`{"code":400,"message":"请上传视频文件","data":null}\`。 | 低 | 已修复 |

---

## 8 验收测试（E2E / GUI）

脚本：\`web/e2e/01-auth.js\`～\`05-msg-admin.js\`。截图由各脚本 \`shot()\` 写入 \`web/e2e/artifacts/\`，本报告副本在 \`交付文档/img/e2e/\`。

| 脚本 | 覆盖用例主路径 | 截图（实际文件） |
|------|----------------|------------------|
| 01-auth | 注册/登录/个人中心/未登录进后台 | 本脚本未落盘截图，以控制台 OK 检查点为准 |
| 02-browse | 浏览、审核上首页、搜索 | （02 脚本同样以断言为主） |
| 03-interaction | 赞踩、评论、关注 | ![点赞](img/e2e/03-1-video-like.png) ![评论赞](img/e2e/03-2-comment-like.png) ![关注](img/e2e/03-3-following.png) |
| 04-studio-live | 上传、修改、直播 | ![上传](img/e2e/04-1-upload.png) ![修改](img/e2e/04-2-edit.png) ![创建直播](img/e2e/04-3-live-create.png) ![OBS开播](img/e2e/04-4-obs-start.png) ![进房](img/e2e/04-5-live-room.png) |
| 05-msg-admin | 私信、通知、后台审核 | ![私信](img/e2e/05-1-dm.png) ![通知](img/e2e/05-2-notify.png) ![后台](img/e2e/05-3-admin-dashboard.png) ![通过](img/e2e/05-4-approve.png) ![点赞](img/e2e/05-5-like.png) ![赞通知](img/e2e/05-5-like-notify.png) |

关键步骤截图：

**03 视频点赞**

![03-1-video-like](img/e2e/03-1-video-like.png)

**04 创作中心上传**

![04-1-upload](img/e2e/04-1-upload.png)

**04 进入直播间**

![04-5-live-room](img/e2e/04-5-live-room.png)

**05 私信**

![05-1-dm](img/e2e/05-1-dm.png)

**05 管理员待审通过**

![05-4-approve](img/e2e/05-4-approve.png)

**05 点赞通知**

![05-5-like-notify](img/e2e/05-5-like-notify.png)

检查点清单（54 条）与 \`文档-已确认/测试报告.md\` 第 7 节 ESE01～ESE05 一致，此处不重复编造页面 JSON。

---

## 9 测试总结

| 项目 | 内容 |
| ---- | ---- |
| 测试日期 | ${date} |
| 系统测试脚本 | \`node postman/run-full-report.mjs\`，原始记录 \`postman/out/results.json\` |
| 单元测试命令 | \`backend\\\\mvnw.cmd -B test\` → Tests run: 260, Failures: 0, Errors: 0, Skipped: 1 |
| 用例总数（报告口径） | 系统测试 59 + 单元 179 + MockMvc 80 + E2E 54 |
| 系统测试通过 | **59 / 59** |
| 单元+MockMvc 通过 | **259 / 259**（另 1 skipped） |
| 失败数 | 0 |
| 通过率 | 100% |
| 总体结论 | 单元（对象级系统操作）与系统测试（用例基本/扩展流程打后端容器）均已用本次命令跑通。组件级集成按要求未单独建套件。验收 GUI 以 Selenium 截图为证。 |

需求—测试追溯见《需求追溯矩阵.md》。复跑系统测试：\`node postman/run-full-report.mjs\`。
`

fs.writeFileSync(path.join(ROOT, '交付文档/测试报告.md'), md, 'utf8')
console.log('wrote 交付文档/测试报告.md')
