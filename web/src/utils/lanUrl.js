/**
 * 手机通过局域网 IP 访问时，把 127.0.0.1 / localhost 换成当前访问的主机名。
 * 本机浏览器访问 localhost 时不改动。
 */
export function resolveLanUrl(url) {
  if (!url) return url
  const host = window.location.hostname
  if (!host || host === 'localhost' || host === '127.0.0.1') {
    return url
  }
  return url
    .replace(/\/\/127\.0\.0\.1(?=[:/])/g, `//${host}`)
    .replace(/\/\/localhost(?=[:/])/g, `//${host}`)
}

export function getLanHost() {
  const host = window.location.hostname
  if (!host || host === 'localhost' || host === '127.0.0.1') {
    return '127.0.0.1'
  }
  return host
}

/** SRS WebRTC 端口（与 deploy/srs-docker.ps1 一致） */
export const SRS_WEBRTC_PORT = 8010

export function getWebRtcEip() {
  return `${getLanHost()}:${SRS_WEBRTC_PORT}`
}

/**
 * FLV / HLS 地址：
 * - 本机 localhost：直连 8088（避免 dev 代理干扰长连接）
 * - 手机 / 局域网 IP：走 /live-media 同源代理（只需开 8787）
 */
export function resolveMediaUrl(playUrl) {
  if (!playUrl) return playUrl
  const m = playUrl.match(/\/live\/([^?]+)/)
  if (!m) return resolveLanUrl(playUrl)

  const file = m[1]
  const host = window.location.hostname
  if (!host || host === 'localhost' || host === '127.0.0.1') {
    return `http://127.0.0.1:8088/live/${file}`
  }
  return `/live-media/${file}`
}

/** 展示用 */
export function resolvePlayUrl(playUrl) {
  return resolveMediaUrl(playUrl)
}
