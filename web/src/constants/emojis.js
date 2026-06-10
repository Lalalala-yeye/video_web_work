/** Unicode 系统表情（直接插入字符） */
export const UNICODE_EMOJIS = [
  { code: '😊', label: '微笑' },
  { code: '👍', label: '赞' },
  { code: '😂', label: '笑哭' },
  { code: '❤️', label: '爱心' },
  { code: '🎉', label: '庆祝' },
  { code: '😎', label: '酷' },
]

/**
 * 图片表情（插入 [名称] 文本，展示时替换为图片）
 * 图片文件放在：web/public/emojis/  →  访问路径 /emojis/xxx.png
 */
export const IMAGE_EMOJIS = [
  { name: '[微笑]', file: '/emojis/smile.png', label: '微笑' },
  { name: '[赞]', file: '/emojis/thumbs-up.png', label: '赞' },
  { name: '[笑哭]', file: '/emojis/laugh.png', label: '笑哭' },
  { name: '[爱心]', file: '/emojis/heart.png', label: '爱心' },
  { name: '[庆祝]', file: '/emojis/party.png', label: '庆祝' },
  { name: '[酷]', file: '/emojis/cool.png', label: '酷' },
]

/** name -> 配置，供评论渲染查找 */
export const IMAGE_EMOJI_BY_NAME = Object.fromEntries(IMAGE_EMOJIS.map(item => [item.name, item]))

const BRACKET_PATTERN = /\[[^\[\]]+\]/g

/**
 * 将评论文本拆成普通文字 + 图片表情片段（Unicode 原样留在 text 段）
 */
export function parseCommentContent(text) {
  if (!text) return [{ type: 'text', value: '' }]

  const segments = []
  let lastIndex = 0
  let match

  const regex = new RegExp(BRACKET_PATTERN.source, 'g')
  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      segments.push({ type: 'text', value: text.slice(lastIndex, match.index) })
    }
    const token = match[0]
    const emoji = IMAGE_EMOJI_BY_NAME[token]
    if (emoji) {
      segments.push({ type: 'image', emoji })
    } else {
      segments.push({ type: 'text', value: token })
    }
    lastIndex = regex.lastIndex
  }

  if (lastIndex < text.length) {
    segments.push({ type: 'text', value: text.slice(lastIndex) })
  }

  return segments.length ? segments : [{ type: 'text', value: text }]
}

/** @deprecated 使用 UNICODE_EMOJIS / IMAGE_EMOJIS */
export const EMOJI_ITEMS = UNICODE_EMOJIS
