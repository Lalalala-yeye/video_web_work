/**
 * 直播地址按「当前浏览器访问的站点」解析，本机 / 局域网 / 域名均可。
 */

export function getSiteHost() {
  const host = window.location.hostname
  return host || '127.0.0.1'
}

/** @deprecated 使用 getSiteHost */
export function getLanHost() {
  return getSiteHost()
}

/** OBS 自定义推流「服务器」字段（与当前网站同主机，RTMP 端口 1935） */
export function getPushRtmpServer() {
  return `rtmp://${getSiteHost()}:1935/live`
}

/** SRS WebRTC 端口（与 deploy/srs-docker.ps1 一致，屏幕分享未启用时备用） */
export const SRS_WEBRTC_PORT = 8000

let configuredWebRtcEip = null

export function setWebRtcEip(eip) {
  configuredWebRtcEip = eip || null
}

export function getWebRtcEip() {
  if (configuredWebRtcEip) return configuredWebRtcEip
  return `${getSiteHost()}:${SRS_WEBRTC_PORT}`
}

/**
 * 观众播放：统一走 /live-media 同源代理（只需能打开本网站）
 * playUrl 为后端返回的相对路径，如 /live/{streamKey}.m3u8
 */
export function resolveMediaUrl(playUrl) {
  if (!playUrl) return playUrl
  const m = playUrl.match(/\/live\/([^?]+)/)
  if (!m) return playUrl
  return `/live-media/${m[1]}`
}

/** 观众页面不展示技术地址 */
export function resolvePlayUrl() {
  return ''
}

/**
 * SRS HLS 子列表中的 /live/ 路径改写到 /live-media/，避免命中前端路由。
 */
export function rewriteLiveMediaUrl(url) {
  if (!url) return url
  try {
    const u = new URL(url, window.location.origin)
    if (u.pathname.startsWith('/live/')) {
      const rest = u.pathname.slice('/live/'.length)
      if (rest && !/^\d+$/.test(rest)) {
        return `/live-media/${rest}${u.search}`
      }
    }
    const isLocalSrs =
      (u.hostname === '127.0.0.1' || u.hostname === 'localhost') &&
      (u.port === '8080' || u.port === '')
    if (isLocalSrs && u.pathname.startsWith('/live/')) {
      const rest = u.pathname.slice('/live/'.length)
      return `/live-media/${rest}${u.search}`
    }
  } catch {
    /* ignore */
  }
  return url
}

/** 兼容旧代码 */
export function resolveLanUrl(url) {
  if (!url) return url
  const host = getSiteHost()
  return url
    .replace(/\/\/127\.0\.0\.1(?=[:/])/g, `//${host}`)
    .replace(/\/\/localhost(?=[:/])/g, `//${host}`)
}
