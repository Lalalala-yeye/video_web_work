# 老师要看的：push 之后浏览器里的网站自己变

GitHub Actions 只把镜像部署到 **云端 kind**（跑完就拆），老师用浏览器打不开那台机器。演示环境一律是 **本机 Kubernetes**（Docker Desktop，namespace `doinb`），不用 docker compose。

答辩电脑上先开本脚本：它等 CI 打完 GHCR 镜像，再 `kubectl set image` 滚动更新 `doinb` 里的 Deployment。

## 答辩前（只做一次）

集群里已经有 `doinb` 命名空间和各服务。然后：

```powershell
.\scripts\local-cd.ps1 -Watch
```

窗口不要关。另开浏览器打开 http://localhost:8787，看左上角 **doinb** 旁边的版本号（来自 `/api/version`）。

若 GHCR 拉镜像 401：

```powershell
$env:GHCR_USER = '你的GitHub用户名'
$env:GHCR_TOKEN = 'read:packages 的 PAT'
```

没有 `doinb` 命名空间时脚本会直接失败，不会去起 compose。先按 `deploy/k8s/README.md` 部署。

## 录屏步骤

1. 画面里网站左上角是旧 SHA（或还没有版本号）。
2. 改一行可见文案或直接 push 当前提交，`git push origin HEAD`。
3. 打开 GitHub Actions，等 **Build versioned images** 和 **Deploy kind + health** 变绿（约 10 分钟）。
4. 本机 `local-cd` 窗口出现 `已部署 xxxxxxx`。
5. **刷新浏览器**，左上角版本号变成这次 commit 的短 SHA。全程不要手动 `kubectl apply`。

对老师说：流水线在 GitHub 测完并发布镜像；这台演示机用同一 SHA 更新本机 k8s，所以网站无需手工部署。
