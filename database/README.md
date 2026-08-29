# 数据库脚本

| 文件 | 用途 | 何时执行 |
| --- | --- | --- |
| `database.sql` | 建表 | 新库；compose 首次启动自动执行 |
| `migrate.sql` | 数据迁移（给旧库补列/补表） | 本机已有早期 doinb 库时手动执行 |
| `seed.sql` | 测试数据 + 演示视频记录 | compose 首次启动自动执行；本机可手动导入 |

演示账号（密码均为 `123456`）：

| 用户名 | 角色 |
| --- | --- |
| `demo_admin` | 管理员 |
| `demo_author` | 作者 |
| `demo_user` | 观众 |

换机器请看仓库根目录 README 的「用 Docker 启动」。
