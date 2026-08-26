# 前端 E2E 第一步（人 1：脚手架 + 账号）

测的是 **doinb 自己的页面**（`http://127.0.0.1:8787`），不是 Selenium 官网。

## 你要先开的两个服务

终端 1 — 后端（MySQL + `application-local.yml`）：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

终端 2 — 前端：

```powershell
cd web
npm install
npm run dev
```

看到 Vite 在 **8787** 端口即可。

## 跑人 1 的脚本

终端 3（**不要关**前面两个）：

```powershell
cd web
npm run e2e
```

需要本机已安装 **Chrome**。Selenium 会自己下驱动。

想关掉弹出的浏览器窗口：

```powershell
$env:E2E_HEADLESS="1"; npm run e2e
```

## 通过会长这样

```text
OK  打开首页
OK  注册密码不一致
OK  注册成功并跳转登录页
OK  错误密码提示
OK  登录成功
OK  个人中心
OK  未登录访问后台会跳转登录
01-auth 全部通过
```

失败时先看：8787/8081 是否在跑、Chrome 是否安装、控制台报错。

## 给另外四人

登录复用 `web/e2e/helpers.js` 里的 `login(driver, user, pass)`。  
不要改 `helpers.js`（有需要开 PR 给人 1）。各写各的：`02-browse.js` … `05-msg-admin.js`。
