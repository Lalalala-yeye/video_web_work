/** 浏览器屏幕分享 → SRS WebRTC（WHIP），观众通过 WHEP/FLV 观看 */

import { getWebRtcEip, SRS_WEBRTC_PORT } from '@/utils/lanUrl'

let activeSession = null

function whipUrl(streamKey) {
  const key = encodeURIComponent(streamKey)
  const eip = getWebRtcEip()
  return `/srs-api/rtc/v1/whip/?app=live&stream=${key}&eip=${eip}`
}

function waitIceConnected(pc, timeoutMs = 20000) {
  return new Promise((resolve, reject) => {
    if (pc.iceConnectionState === 'connected' || pc.iceConnectionState === 'completed') {
      resolve()
      return
    }
    const timer = setTimeout(() => {
      reject(new Error(`WebRTC 推流连接超时，请检查防火墙是否放行 ${SRS_WEBRTC_PORT}（TCP+UDP）`))
    }, timeoutMs)
    pc.oniceconnectionstatechange = () => {
      const state = pc.iceConnectionState
      if (state === 'connected' || state === 'completed') {
        clearTimeout(timer)
        resolve()
      } else if (state === 'failed') {
        clearTimeout(timer)
        reject(new Error(`WebRTC ICE 失败，请确认 SRS 已启动且端口 ${SRS_WEBRTC_PORT} 已放行`))
      }
    }
  })
}

export function isScreenPublishing() {
  return activeSession != null
}

export function isPushActive() {
  return activeSession?.pushOk === true
}

export function getPublishingStreamKey() {
  return activeSession?.streamKey ?? null
}

export function getLocalPreviewStream() {
  return activeSession?.displayStream ?? null
}

export function stopScreenPublish() {
  if (!activeSession) return
  const { pc, displayStream, onEnded } = activeSession
  if (onEnded && displayStream) {
    const track = displayStream.getVideoTracks()[0]
    if (track) track.removeEventListener('ended', onEnded)
  }
  displayStream?.getTracks().forEach(t => t.stop())
  if (pc) pc.close()
  activeSession = null
}

/**
 * 选择屏幕/窗口；本地预览立即生效，再尝试推到 SRS
 */
export async function startScreenPublish(streamKey, { onPreviewReady } = {}) {
  if (!streamKey) {
    throw new Error('缺少推流密钥')
  }
  if (!navigator.mediaDevices?.getDisplayMedia) {
    throw new Error('当前浏览器不支持屏幕分享')
  }

  stopScreenPublish()

  const displayStream = await navigator.mediaDevices.getDisplayMedia({
    video: { frameRate: { ideal: 30, max: 30 } },
    audio: true,
  })

  const onEnded = () => stopScreenPublish()
  const videoTrack = displayStream.getVideoTracks()[0]
  if (videoTrack) videoTrack.addEventListener('ended', onEnded)

  const pc = new RTCPeerConnection({
    iceServers: [{ urls: 'stun:stun.l.google.com:19302' }],
  })

  // 先建立会话 → 本地预览立刻可用，不依赖 SRS 是否连上
  activeSession = { pc, displayStream, streamKey, onEnded, pushOk: false }
  onPreviewReady?.()

  displayStream.getTracks().forEach(track => pc.addTrack(track, displayStream))

  const offer = await pc.createOffer()
  await pc.setLocalDescription(offer)

  const res = await fetch(whipUrl(streamKey), {
    method: 'POST',
    headers: { 'Content-Type': 'application/sdp' },
    body: offer.sdp,
  })

  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(
      `SRS 推流失败 (${res.status})，本地预览仍可用。${text ? text.slice(0, 80) : '请确认 SRS 容器在运行'}`
    )
  }

  const answerSdp = await res.text()
  await pc.setRemoteDescription({ type: 'answer', sdp: answerSdp })

  try {
    await waitIceConnected(pc)
    activeSession.pushOk = true
  } catch (e) {
    throw new Error(`${e.message || '推流连接失败'}（本地预览仍可用，观众可能看不到）`)
  }

  return activeSession
}
