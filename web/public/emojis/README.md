# 表情包图片目录

## 图片放哪？

把 PNG 文件放在**本目录**（与 README 同级）：

```
video_web/web/public/emojis/
├── smile.png
├── thumbs-up.png
├── laugh.png
├── heart.png
├── party.png
├── cool.png
└── README.md
```

Vite 会把 `public/` 下的文件原样映射到网站根路径，因此：

- 磁盘路径：`web/public/emojis/smile.png`
- 浏览器访问：`http://localhost:8787/emojis/smile.png`
- 配置里写：`/emojis/smile.png`（见 `web/src/constants/emojis.js` 的 `IMAGE_EMOJIS`）

**不要**放在 `src/assets/`，也**不要**走后端 `/uploads/`（那是用户上传的视频/头像）。

## 两种表情怎么用

| 类型 | 配置 | 插入到评论 | 展示 |
|------|------|------------|------|
| 系统 Unicode | `UNICODE_EMOJIS` | 直接插入 😊 | 文字 emoji |
| 图片表情 | `IMAGE_EMOJIS` | 插入 `[微笑]` | 替换为 `<img src="/emojis/smile.png">` |

新增图片表情时：

1. 把 `xxx.png` 放进本目录  
2. 在 `web/src/constants/emojis.js` 的 `IMAGE_EMOJIS` 增加一行，例如：  
   `{ name: '[doge]', file: '/emojis/doge.png', label: 'doge' }`

可从 teriteri 等项目的 `/public/emoji/` 复制 png，改文件名与配置对应即可。
