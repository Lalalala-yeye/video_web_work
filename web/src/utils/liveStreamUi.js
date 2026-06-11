/** 直播相关面向用户的展示文案（不暴露完整密钥/内网地址） */

export function maskStreamKey(streamKey) {
  if (!streamKey) return '—'
  if (streamKey.length <= 8) return '已生成'
  return `${streamKey.slice(0, 4)}···${streamKey.slice(-4)}`
}
