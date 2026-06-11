/**
 * 浏览器屏幕分享 → SRS WebRTC（WHIP）
 * 暂未在 UI 启用（屏幕分享功能未完善），保留实现供后续完善。
 */

import { getWebRtcEip, SRS_WEBRTC_PORT } from '@/utils/lanUrl'
import { waitStreamPlayable } from '@/utils/srsStream'

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

function preferH264Transceiver(pc, track, stream) {
  const transceiver = pc.addTransceiver(track, {
    direction: 'sendonly',
    streams: [stream],
  })
  const caps = RTCRtpSender.getCapabilities?.('video')
  if (!caps?.codecs?.length || !transceiver.setCodecPreferences) return transceiver
  const h264 = caps.codecs.filter(c => c.mimeType?.toLowerCase() === 'video/h264')
  if (!h264.length) return transceiver
  const rest = caps.codecs.filter(c => c.mimeType?.toLowerCase() !== 'video/h264')
  transceiver.setCodecPreferences([...h264, ...rest])
  return transceiver
}

async function tunePublishSenders(pc) {
  for (const sender of pc.getSenders()) {
    const track = sender.track
    if (!track) continue
    const params = sender.getParameters()
    if (!params.encodings?.length) params.encodings = [{}]
    if (track.kind === 'video') {
      params.encodings[0].maxBitrate = 2_500_000
      params.encodings[0].maxFramerate = 30
      params.degradationPreference = 'maintain-framerate'
    } else if (track.kind === 'audio') {
      params.encodings[0].maxBitrate = 128_000
    }
    try {
      await sender.setParameters(params)
    } catch {
      /* 部分浏览器不支持全部字段 */
    }
  }
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

  // 限制分辨率：过高（如 2560x1528）会导致 SRS rtc_to_rtmp empty nalu、frames=0
  const displayStream = await navigator.mediaDevices.getDisplayMedia({
    video: {
      width: { ideal: 1280, max: 1920 },
      height: { ideal: 720, max: 1080 },
      frameRate: { ideal: 24, max: 30 },
    },
    audio: true,
  })

  const onEnded = () => stopScreenPublish()
  const videoTrack = displayStream.getVideoTracks()[0]
  if (videoTrack) {
    videoTrack.contentHint = 'detail'
    videoTrack.addEventListener('ended', onEnded)
    try {
      await videoTrack.applyConstraints({
        width: { max: 1280 },
        height: { max: 720 },
        frameRate: { max: 30 },
      })
    } catch {
      /* 部分源不支持二次约束 */
    }
  }

  const pc = new RTCPeerConnection({
    iceServers: [{ urls: 'stun:stun.l.google.com:19302' }],
  })

  activeSession = { pc, displayStream, streamKey, onEnded, pushOk: false }
  onPreviewReady?.()

  const audioTrack = displayStream.getAudioTracks()[0]
  if (videoTrack) {
    preferH264Transceiver(pc, videoTrack, displayStream)
  }
  if (audioTrack) {
    pc.addTrack(audioTrack, displayStream)
  }

  await tunePublishSenders(pc)

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
  } catch (e) {
    throw new Error(`${e.message || '推流连接失败'}（本地预览仍可用，观众看不到）`)
  }

  const srsReady = await waitStreamPlayable(streamKey, 30000)
  if (srsReady) {
    activeSession.pushOk = true
  } else {
    throw new Error(
      `SRS 已连接但未产出可播放画面（frames=0）。请：① 勿共享当前浏览器标签页；② 重新执行 deploy\\srs-docker.ps1 加载新配置；③ 或改用 OBS RTMP 推流`
    )
  }

  return activeSession
}
