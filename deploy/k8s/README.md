# CD-02：部署到 Kubernetes

本目录把 CD-01 产生的版本镜像部署为 MySQL、五个业务服务、网关、web。默认使用固定 tag `cde7310`，不使用 `latest`。

GitHub Actions 的 **Deploy kind + health (CD-02)** 会在打完镜像后，用本次 commit 的 SHA tag 在 runner 的 kind 集群里自动部署并探活。下面步骤用于本机集群验收。

下面分为三部分：正式部署实验、滚动更新实验、调试与故障恢复。前两部分用于正式执行和取证，第三部分仅在实验失败时使用。

# 第一部分：正式部署实验

## 1. 实验目标与资源

- Namespace：`doinb`
- MySQL：官方镜像 `mysql:8.0`，Deployment、Service、PVC
- 业务：`doinb-user` / `doinb-video` / `doinb-live` / `doinb-interact` / `doinb-message`
- 网关：`ghcr.io/lalalala-yeye/doinb-gateway:cde7310`（对外 8081）
- web：`ghcr.io/lalalala-yeye/doinb-web:cde7310`
- 存活 `/health`，就绪 `/ready`，版本 `/version`
- web `/api`：反向代理到 `http://gateway:8081/`

所有命令均在仓库根目录执行。开始前确认本地集群可用：

```powershell
kubectl cluster-info
kubectl get nodes
```

## 2. 初始化 namespace、密钥和数据库脚本

创建 namespace：

```powershell
kubectl apply -f deploy/k8s/namespace.yaml
```

创建运行时密钥。各 Java 服务使用 MySQL root 用户，因此 `MYSQL_PASSWORD` 必须与 `MYSQL_ROOT_PASSWORD` 相同；不要把真实值写入仓库或截图：

```powershell
kubectl create secret generic doinb-secrets -n doinb `
  --from-literal=MYSQL_ROOT_PASSWORD='你的MySQL密码' `
  --from-literal=MYSQL_PASSWORD='你的MySQL密码' `
  --from-literal=JWT_SECRET='至少32个字符的随机字符串' `
  --from-literal=DOINB_INTERNAL_TOKEN='doinb-internal-dev-token'
```

把现有建表 SQL 创建为 ConfigMap：

```powershell
kubectl create configmap doinb-db-init -n doinb `
  --from-file=database.sql=database/database.sql `
  --dry-run=client -o yaml | kubectl apply -f -
```

MySQL 官方镜像只会在数据卷为空时执行初始化 SQL，已有 PVC 不会重复建表。

## 3. 正式部署

应用全部清单：

```powershell
kubectl apply -k deploy/k8s
```

等待工作负载就绪，并从集群内部验证网关、业务服务和 web：

```powershell
./deploy/k8s/verify.ps1
```

记录最终 Pod、Service 和镜像：

```powershell
kubectl get pods -n doinb -o wide
kubectl get services -n doinb
kubectl get deployments -n doinb `
  -o custom-columns=NAME:.metadata.name,READY:.status.readyReplicas,IMAGE:.spec.template.spec.containers[*].image
```

预期结果：MySQL 1 个 Pod；user / video / live / interact / message / gateway / web 各 2 个 Pod，均为 `1/1 Running`；镜像为明确 SHA tag。

## 4. 访问页面并验证反向代理

新开一个终端转发 web：

```powershell
kubectl port-forward service/web 8080:80 -n doinb
```

再新开一个终端转发网关：

```powershell
kubectl port-forward service/gateway 8081:8081 -n doinb
```

在第三个终端验证：

```powershell
curl.exe -i http://127.0.0.1:8081/health
curl.exe -i http://127.0.0.1:8081/ready
curl.exe -i http://127.0.0.1:8081/version
curl.exe -i http://127.0.0.1:8080/api/health
```

`/health` `/ready` `/version` 应返回 HTTP 200。最后一次请求经过 web 镜像内的 Nginx `/api` 代理访问网关。浏览器打开 <http://127.0.0.1:8080>，确认前端页面可以显示。

## 5. 正式部署证据

完成第一部分后应保存三项基础证据：

1. `kubectl get pods -n doinb -o wide` 显示 MySQL、五个业务服务、网关、web 全部为 `1/1 Running`。
2. 网关 `/health` `/ready` `/version` 与 web `/api/health` 均返回 HTTP 200。
3. 浏览器能够正常打开前端页面。

# 第一部分 B：自动扩缩容

任务：给容器设好 `requests`/`limits`（已在 `java-services.yaml`），再挂 HPA，加压后 Pod 变多，停压后 Pod 变少。现场记下吞吐、均时、P95、错误率。

HPA 挂在 **gateway / video / user**（压测会打到的路径）。副本 1～4，CPU 目标 50%（相对 request 50m）。缩容窗口 30 秒，方便现场看降回去。不要给 MySQL 做 HPA。

第 4 项单体对比时请先 `kubectl delete hpa -n doinb --all` 或不要加压，避免副本数变化污染对比。

## 1. metrics-server（kind 必做）

HPA 靠它读 CPU。没有的话 `kubectl get hpa` 里 TARGETS 一直是 `<unknown>`，Pod 不会动。

```powershell
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
kubectl patch deployment metrics-server -n kube-system --type json -p "[{\"op\":\"add\",\"path\":\"/spec/template/spec/containers/0/args/-\",\"value\":\"--kubelet-insecure-tls\"}]"
kubectl rollout status deployment/metrics-server -n kube-system
kubectl top pods -n doinb
```

最后一条必须打出 CPU 数字。

## 2. 应用 HPA

```powershell
kubectl apply -k deploy/k8s
kubectl get hpa -n doinb
```

空载时 gateway/video/user 各 1～2 个 Pod（Deployment 默认 2，HPA 允许降到 1）。

## 3. 现场演示（建议三个终端）

终端 1：

```powershell
kubectl get hpa -n doinb -w
```

终端 2：

```powershell
kubectl get pods -n doinb -l "app in (gateway,video,user)" -w
```

终端 3：转发网关并加压。登录更吃 CPU，列表打不上去时用登录。

```powershell
kubectl port-forward -n doinb svc/gateway 8081:8081
```

另开终端（仓库根目录）：

```powershell
./deploy/k8s/hpa-demo.ps1 -Scenario login -Vus 50 -Duration 90
```

或直接：

```powershell
node bench/run.mjs --label micro --base http://127.0.0.1:8081 --scenario login --vus 50 --duration 90 --warmup 5 --rounds 1
```

老师应看到：TARGETS 超过 50% → REPLICAS 增加 → 新 Pod `ContainerCreating` 再到 `Running`。脚本会打出吞吐、均时、P95、错误率，截下来。

停压后等约 30～60 秒，副本减回去。

CPU 上不去时：把 `--vus` 加到 80，或确认打的是集群里的 8081 而不是本机 Docker Compose。

# 第一部分 C：故障处理

选做：**超时 + 降级**。主动停掉视频服务，网关搜索 3 秒内放弃该路，返回空视频列表，并带事先写好的 `notices` 提示；用户/直播仍聚合，网关 `/health` 正常，其它服务不跟着崩溃。

```powershell
# 本机 Docker Compose（8081 已是微服务网关）
./deploy/k8s/fault-demo.ps1 -Target compose

# Kubernetes（先 port-forward 网关 8081）
./deploy/k8s/fault-demo.ps1 -Target k8s
```

脚本会：打一次搜索 → `docker stop doinb-video`（或 K8s 把 video 副本打到 0）→ 再搜索。第二次应 `code=200`、`notices` 含「视频服务超时或不可用」、`videos` 为空。页面搜索同样会黄条提示。K8s 演示前会暂时删掉 video 的 HPA，结束再 apply 回去。

# 第二部分：滚动更新实验

## 1. 实验目的和成功判据

本实验把五个业务服务、网关、web 切到同一 SHA tag，验证更新期间由新 Pod 逐步替换旧 Pod，并在更新完成后保持服务健康。

成功需要同时满足：

- 各 Java Deployment 与 web 最终都显示 `successfully rolled out`。
- 最终镜像为明确 SHA tag，不是 `latest`。
- 新 Pod 达到 `1/1 Running` 后旧 Pod 才退出。
- 更新完成后所有正式 Pod 均为 `1/1 Running`。
- 网关 `/health` `/ready` `/version` 和 web `/api/health` 仍返回 HTTP 200，前端页面仍可访问。

## 2. 准备四个终端

| 终端 | 命令或用途 |
|---|---|
| 终端 1 | `kubectl port-forward service/web 8080:80 -n doinb` |
| 终端 2 | `kubectl port-forward service/gateway 8081:8081 -n doinb` |
| 终端 3 | `kubectl get pods -n doinb -w`，持续记录 Pod 状态变化 |
| 终端 4 | 在仓库根目录执行版本切换、状态确认和 curl |

滚动更新会删除旧 Pod，因此终端 1、2 的端口转发可能退出。这不表示 rollout 失败；脚本自身会从集群内部验证服务，更新结束后重新启动端口转发即可。

## 3. 建立旧版本基线

在终端 4 执行：

```powershell
./deploy/k8s/rollout.ps1 -Tag fade254
```

脚本会依次修改六个 Java 服务和 web 镜像，等待全部 Deployment 完成，并执行一次集群内部健康检查。完成后确认旧版本和 Pod 状态：

```powershell
kubectl get deployments -n doinb `
  -o custom-columns=NAME:.metadata.name,READY:.status.readyReplicas,IMAGE:.spec.template.spec.containers[*].image
kubectl get pods -n doinb -o wide
```

预期各服务镜像都以 `:fade254` 结尾，Java 服务与 web 副本数分别为 2，当前 Pod 均为 `1/1 Running`。保存“旧版本部署成功和镜像 tag”的截图，作为更新前基线。

如果端口转发已经退出，重新启动后可补做旧版本健康检查：

```powershell
curl.exe -i http://127.0.0.1:8081/health
curl.exe -i http://127.0.0.1:8080/api/health
```

## 4. 执行正式滚动更新

确认终端 3 正在运行以下监听命令：

```powershell
kubectl get pods -n doinb -w
```

在终端 4 将服务更新到当前版本：

```powershell
./deploy/k8s/rollout.ps1 -Tag cde7310
```

`rollout.ps1` 的执行逻辑是：

1. 拒绝空 tag 和 `latest`。
2. 更新 user / video / live / interact / message / gateway / web 的容器镜像，并写入 ConfigMap `APP_VERSION`。
3. 分别等待上述 Deployment rollout 完成。
4. 调用 `verify.ps1`，等待 MySQL、五服务、网关、web 全部就绪。
5. 创建临时 curl Pod，从集群内部检查 `http://gateway:8081/health`、`/ready`、`/version` 和 `http://web/`，成功后删除临时 Pod。

终端 4 最终应出现：

```text
deployment "gateway" successfully rolled out
deployment "web" successfully rolled out
OK: probes passed.
```

## 5. 判断终端 3 中的新旧 Pod

`kubectl get pods -w` 不会直接标注“新 Pod”或“旧 Pod”，需要比较 Pod 名中的 ReplicaSet 哈希。正常过程是：

```text
新 Pod：Pending → Init/ContainerCreating → Running 0/1 → Running 1/1
旧 Pod：Running 1/1 → Terminating → 从列表消失
```

本次实际实验中的 ReplicaSet 变化为：

| 服务 | `fade254` 旧 ReplicaSet | `cde7310` 新 ReplicaSet |
|---|---|---|
| gateway（演示时以实际 `kubectl get pods -w` 为准） | 见集群输出 | 见集群输出 |
| web | `74785676f4` | `79647c57b6` |

因此看到 `gateway-85dfdcdcbc-*` 或 `web-79647c57b6-*` 从 `Pending` 变为 `1/1 Running`，随后 `gateway-85497996d7-*` 或 `web-74785676f4-*` 进入 `Terminating`，就是滚动替换过程。

旧 Java Pod 在终止阶段可能短暂显示 `Error`，Java 进程收到终止信号时可能以非零状态退出。只要旧 Pod 随后消失、新 Pod 全部 Ready、Deployment 显示 rollout 成功，就不属于新版本启动失败。

## 6. 更新后的最终验证和取证

等待终端 3 不再产生新状态后，在终端 4 执行：

```powershell
kubectl get pods -n doinb -o wide
kubectl get deployments -n doinb `
  -o custom-columns=NAME:.metadata.name,READY:.status.readyReplicas,IMAGE:.spec.template.spec.containers[*].image
kubectl rollout history deployment/gateway -n doinb
kubectl rollout history deployment/web -n doinb
```

最终应只剩当前 ReplicaSet 的各服务副本以及一个 MySQL，全部为 `1/1 Running`。如果端口转发已退出，重新执行第一部分第 4 节中的命令，然后验证：

```powershell
curl.exe -i http://127.0.0.1:8081/health
curl.exe -i http://127.0.0.1:8080/api/health
```

建议按以下顺序保存证据：

1. 旧版本 `fade254` 部署成功和镜像列表。
2. 终端 3 中新 Pod 启动、旧 Pod 终止的过程。
3. 终端 4 中两个 Deployment 的 `successfully rolled out` 和脚本内部健康检查成功。
4. 最终 `cde7310` 镜像、5 个 Pod 全部 Running、rollout history。
5. 更新后两个 HTTP 200 和前端页面。

本次实际结果和六张截图已归档在 [`EVIDENCE.md`](EVIDENCE.md)。仓库证据还包括本目录清单、验收记录以及包含这些文件的 commit。

# 第三部分：调试与故障恢复

正式实验失败时，按“资源状态 → 事件 → 日志 → 服务连通性”的顺序检查。所有命令都要带 `-n doinb`，否则默认查询 `default` namespace，会出现 `No resources found`。

## 1. 快速定位故障位置

先执行：

```powershell
kubectl get pods -n doinb -o wide
kubectl get "deployments,services,endpoints,pvc" -n doinb
kubectl get events -n doinb --sort-by=.lastTimestamp
```

常见状态含义：

| 状态 | 通常原因 | 优先检查 |
|---|---|---|
| `Pending` | 无可用节点、PVC 未绑定或资源不足 | `describe pod`、`get pvc` |
| `Init:0/1` | 业务服务正在等待 MySQL 3306 | MySQL Pod、Service 和 endpoints |
| `ImagePullBackOff` | tag 不存在、仓库私有或拉取失败 | Pod Events、镜像名和登录配置 |
| `CrashLoopBackOff` | 应用启动后退出 | 当前日志和 `--previous` 日志 |
| `Running 0/1` | 容器运行但探针未通过 | Pod Events、探针地址和应用日志 |
| 旧 Pod `Terminating/Error` | 滚动更新正在清理旧副本 | 等待新副本就绪后再次查询最终状态 |

查看单个 Pod 的事件和容器状态：

```powershell
kubectl describe pod -n doinb <Pod名称>
kubectl logs -n doinb <Pod名称> --all-containers=true
kubectl logs -n doinb <Pod名称> --all-containers=true --previous
```

`--previous` 只在容器发生过重启或退出时有内容。

## 2. MySQL 与业务服务启动问题

五个业务服务都有 `wait-for-mysql` initContainer。若长期停在 `Init:0/1`：

```powershell
kubectl get pod -n doinb -l app=mysql
kubectl get service/mysql endpoints/mysql -n doinb
kubectl logs -n doinb deployment/mysql
kubectl logs -n doinb <业务 Pod名称> -c wait-for-mysql
kubectl get pvc -n doinb
```

判断方法：

- MySQL Pod 不是 `1/1 Running`：先处理 MySQL 日志、PVC 或 Secret。
- `endpoints/mysql` 没有地址：MySQL readiness 尚未通过，Service 暂时不会转发流量。
- PVC 为 `Pending`：本地集群可能没有默认 StorageClass，使用 `kubectl get storageclass` 检查。
- MySQL 日志出现认证错误：检查 `doinb-secrets` 是否同时包含 `MYSQL_ROOT_PASSWORD` 和 `MYSQL_PASSWORD`，且两者创建时填写相同值。

只检查 Secret 是否存在以及包含哪些键，不要把真实值输出到日志或截图：

```powershell
kubectl get secret doinb-secrets -n doinb
kubectl describe secret doinb-secrets -n doinb
kubectl get configmap doinb-db-init -n doinb
```

如果表没有创建，先确认是不是复用了旧 PVC。初始化 SQL 只在空数据卷首次启动时执行。不要为了调试直接删除 PVC；删除会丢失数据库数据。确实允许清空实验数据时，才重新创建 namespace/PVC。

## 3. 网关 / 业务服务探针失败

Java 服务为 `Running 0/1`，或 Events 中出现 readiness/liveness failure 时（以网关为例，其它服务把名字换成 user/video/live/interact/message）：

```powershell
kubectl describe deployment gateway -n doinb
kubectl describe pod -n doinb -l app=gateway
kubectl logs -n doinb deployment/gateway --tail=200
kubectl get endpoints gateway -n doinb
```

从集群内部直接请求健康接口：

```powershell
kubectl run gateway-debug -n doinb `
  --image=curlimages/curl:8.12.1 --restart=Never --rm -i `
  --command -- curl -v http://gateway:8081/health
```

- 返回 HTTP 200：网关与 Service 正常，继续检查 web 或本地端口转发。
- 连接被拒绝：确认容器监听 8081，以及 `Service/gateway` 的 `targetPort`。
- 返回 500 或连接超时：重点查看该服务日志和 MySQL 连通性。
- 没有 gateway endpoints：readiness 未通过，Service 不会选择该 Pod。

## 4. web、Nginx 与 `/api` 代理问题

先分别测试前端和代理链路：

```powershell
kubectl run web-debug -n doinb `
  --image=curlimages/curl:8.12.1 --restart=Never --rm -i `
  --command -- sh -c "curl -v http://web/ && curl -v http://web/api/health"
```

再检查：

```powershell
kubectl logs -n doinb deployment/web --tail=200
kubectl get service/web endpoints/web service/gateway endpoints/gateway -n doinb
```

- `http://web/` 成功、`/api/health` 失败：检查 gateway Service/endpoints 和 `web/nginx.conf`。
- 两者都失败：检查 web Pod 探针、Nginx 日志和 Service 80 端口。
- 网关直连成功但 web 代理失败：确认 Service 名仍为 `gateway`，端口仍为 8081；Nginx 使用 Kubernetes DNS 名 `gateway:8081`。

## 5. 镜像拉取失败

出现 `ImagePullBackOff` 或 `ErrImagePull` 时：

```powershell
kubectl describe pod -n doinb <Pod名称>
kubectl get deployment gateway web -n doinb `
  -o custom-columns=NAME:.metadata.name,IMAGE:.spec.template.spec.containers[*].image
```

重点查看 Events 中是否为 tag 不存在、网络失败或 `unauthorized`。当前清单使用公开 GHCR 镜像；如果包改成 private，需要创建 registry Secret，并在 Pod 模板中配置 `imagePullSecrets`。不要用 `latest` 规避 tag 错误，应该改成 Actions 实际生成的 7 位 SHA。

## 6. 本地端口转发问题

`port-forward` 会占用当前终端，并绑定本机端口。出现连接失败时先确认转发窗口仍在运行；滚动更新删除了被转发的旧 Pod 后，转发进程退出是正常现象。

重新启动：

```powershell
kubectl port-forward service/web 8080:80 -n doinb
kubectl port-forward service/gateway 8081:8081 -n doinb
```

如果端口被占用，可改用其他本地端口：

```powershell
kubectl port-forward service/web 18080:80 -n doinb
kubectl port-forward service/gateway 18081:8081 -n doinb
```

此时访问 `http://127.0.0.1:18080` 和 `http://127.0.0.1:18081/health`。Windows 上可用下面的命令查看占用进程：

```powershell
netstat -ano | findstr :8080
netstat -ano | findstr :8081
```

## 7. 滚动更新卡住或失败

查看 Deployment、ReplicaSet 和发布状态：

```powershell
kubectl rollout status deployment/gateway -n doinb --timeout=5m
kubectl rollout status deployment/web -n doinb --timeout=5m
kubectl get replicasets -n doinb
kubectl describe deployment gateway -n doinb
kubectl describe deployment web -n doinb
```

新 Pod 长期不能 Ready 时，Deployment 因 `maxUnavailable: 0` 会保留旧 Pod，因此应先按本部分第 1～5 节修复新 Pod，不要直接删除仍可用的旧副本。

确认新版本确实无法修复时再回退：

```powershell
kubectl rollout undo deployment/gateway -n doinb
kubectl rollout undo deployment/web -n doinb
kubectl rollout status deployment/gateway -n doinb --timeout=5m
kubectl rollout status deployment/web -n doinb --timeout=5m
./deploy/k8s/verify.ps1
```

`rollout.ps1` 会拒绝空 tag 和 `latest`。若脚本失败，先阅读它输出的具体 Deployment，再执行对应的 `describe` 和 `logs`，不要连续反复修改 tag。

## 8. 调试结束后的确认与清理

修复后统一执行：

```powershell
./deploy/k8s/verify.ps1
kubectl get pods -n doinb -o wide
kubectl get deployments -n doinb `
  -o custom-columns=NAME:.metadata.name,READY:.status.readyReplicas,IMAGE:.spec.template.spec.containers[*].image
```

确认临时调试 Pod 已由 `--rm` 自动删除。若命令中断导致它们残留，可删除明确命名的临时 Pod：

```powershell
kubectl delete pod gateway-debug web-debug -n doinb --ignore-not-found
```

停止端口转发只需在对应终端按 `Ctrl+C`。

> 当前 user / video 上传目录使用 `emptyDir`。这适合本次实验，但 Pod 被替换后上传文件不会保留，多副本之间也不共享；生产环境应改用共享存储或对象存储。
