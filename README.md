前置条件：
运行 Docker Desktop

## 第一步 克隆仓库
```bash
git clone https://github.com/Lalalala-yeye/video_web_work
cd video_web_work
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

