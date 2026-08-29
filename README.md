前置条件：
运行 Docker Desktop

## 第一步 克隆仓库
```bash
git clone https://github.com/Lalalala-yeye/video_web_work
cd video_web
git checkout main
```
## 第二步 启动容器
```bash
copy .env.example .env
docker compose up --build -d
```
## 进行检查
浏览器打开：

网站：http://localhost:8787
健康检查：http://localhost:8081/health
或走前端反代：http://localhost:8787/api/health

## 换机器时如何带上已有视频

仓库里带了 **3 条课设演示样片**（约 23MB，见 `backend/demo-media/`）。新电脑 `docker compose up --build` 后，空库会写入 `seed.sql`，首页可直接播这些样片。

你自己上传的视频仍在 `backend/uploads/` 和 MySQL 数据卷里，**不会**随 `git clone` 过去。

**做法 A（推荐，数据永远最新）：** 不要在新电脑再部署一套。旧电脑保持 `docker compose up`，新电脑浏览器打开 `http://旧电脑局域网IP:8787`。

**做法 B（必须在新电脑再部署一套时）：** 把「数据库 + 上传文件」打包带走。

旧电脑（容器正在跑）：

```powershell
.\scripts\export-data.ps1
```

把生成的 `doinb-data.zip` 拷到新电脑项目根目录，先完成上面的「启动容器」，再：

```powershell
.\scripts\import-data.ps1
```

不要把 zip / `uploads/` / mp4 提交进 Git（仓库体积会爆，GitHub 单文件上限 100MB）。