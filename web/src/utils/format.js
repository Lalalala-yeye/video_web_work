/** 相对时间（中文） */
export function formatRelativeTime(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const diff = Date.now() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  if (days < 365) return `${Math.floor(days / 30)}月前`
  return `${Math.floor(days / 365)}年前`
}

export function formatCount(num) {
  if (num == null) return '0'
  if (num >= 10000) return `${(num / 10000).toFixed(1)}万`
  return String(num)
}

/** 解析路由或接口中的正整数 id，无效时返回 null（避免 NaN 传给后端） */
export function parseRouteId(value) {
  if (value == null || value === '') return null
  const n = Number(value)
  if (!Number.isFinite(n) || !Number.isInteger(n) || n <= 0) return null
  return n
}
