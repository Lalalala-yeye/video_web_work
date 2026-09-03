# 课设演示样片

clone 后由 `doinb-video` 启动时拷到 `/app/uploads`（compose 挂宿主机目录，k8s 的 emptyDir 同样走这一步），配合 `database/seed.sql` 在首页显示。

| 文件 | 首页标题 | 约大小 |
|------|----------|--------|
| `videos/demo-1938.mp4` | 1938 增兵徐州 | 5.5 MB |
| `videos/demo-pixel.mp4` | 像素角色绘制 | 3 MB |
| `videos/demo-chart.mp4` | 实时数据折线图 | 10 MB |

不要往这里塞用户自己上传的大文件（GitHub 单文件上限 100MB，超过 50MB 会告警）。
