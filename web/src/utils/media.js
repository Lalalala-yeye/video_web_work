/** 将后端相对路径转为前端可访问 URL */
export function resolveMediaUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  const path = url.startsWith('/') ? url : `/${url}`
  return `/api${path}`
}
