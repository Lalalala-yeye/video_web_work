import Hls from 'hls.js'
import flvjs from 'flv.js'
import { resolveMediaUrl, getWebRtcEip } from '@/utils/lanUrl'

function parseStreamKey(playUrl) {
  const m = playUrl?.match(/\/live\/([^./?]+)/)
  return m?.[1] || ''
}

function isSafariNativeHls(video) {
  if (!video.canPlayType('application/vnd.apple.mpegurl')) return false
  return /^((?!chrome|android).)*safari/i.test(navigator.userAgent)
}

function waitIceConnected(pc, timeoutMs = 15000) {
  return new Promise((resolve, reject) => {
    if (pc.iceConnectionState === 'connected' || pc.iceConnectionState === 'completed') {
      resolve()
      return
    }
    const timer = setTimeout(() => reject(new Error('timeout')), timeoutMs)
    pc.oniceconnectionstatechange = () => {
      const s = pc.iceConnectionState
      if (s === 'connected' || s === 'completed') {
        clearTimeout(timer)
        resolve()
      } else if (s === 'failed') {
        clearTimeout(timer)
        reject(new Error('ice failed'))
      }
    }
  })
}

/**
 * 观众端直播播放：屏幕分享推流优先 WHEP，断线自动重连；失败再 FLV / HLS
 */
export function attachLivePlayer(video, playUrl, { onError, onPlaying } = {}) {
  const mediaUrl = resolveMediaUrl(playUrl)
  const flvUrl = mediaUrl.replace(/\.m3u8(\?.*)?$/i, '.flv')
  const streamKey = parseStreamKey(playUrl)

  let flvPlayer = null
  let hlsInstance = null
  let whepPc = null
  let mediaStream = null
  let reconnectTimer = null
  let stallTimer = null
  let destroyed = false
  let mode = 'whep'
  let lastAdvanceAt = 0
  let lastVideoTime = 0

  const clearReconnect = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  const clearStall = () => {
    if (stallTimer) {
      clearInterval(stallTimer)
      stallTimer = null
    }
  }

  const cleanup = () => {
    destroyed = true
    clearReconnect()
    clearStall()
    if (whepPc) {
      whepPc.close()
      whepPc = null
    }
    if (flvPlayer) {
      flvPlayer.pause()
      flvPlayer.unload()
      flvPlayer.detachMediaElement()
      flvPlayer.destroy()
      flvPlayer = null
    }
    if (hlsInstance) {
      hlsInstance.destroy()
      hlsInstance = null
    }
    mediaStream = null
    video.onplaying = null
    video.onerror = null
    video.removeAttribute('src')
    video.srcObject = null
    video.load()
  }

  const handleError = () => {
    if (!destroyed) onError?.()
  }

  const handlePlaying = () => {
    if (!destroyed) onPlaying?.()
  }

  const tryPlay = () => {
    video.play().then(handlePlaying).catch(() => {})
  }

  const startStallWatch = () => {
    clearStall()
    lastAdvanceAt = Date.now()
    lastVideoTime = video.currentTime
    stallTimer = setInterval(() => {
      if (destroyed) return
      const t = video.currentTime
      if (t > lastVideoTime + 0.05) {
        lastVideoTime = t
        lastAdvanceAt = Date.now()
        return
      }
      if (Date.now() - lastAdvanceAt > 5000) {
        if (mode === 'whep') {
          scheduleWhepReconnect()
        } else {
          handleError()
        }
      }
    }, 1000)
  }

  const scheduleWhepReconnect = () => {
    if (destroyed || mode !== 'whep' || reconnectTimer) return
    lastAdvanceAt = Date.now()
    reconnectTimer = setTimeout(async () => {
      reconnectTimer = null
      if (destroyed || mode !== 'whep') return
      const ok = await connectWhep()
      if (!ok && !destroyed) {
        mode = 'flv'
        tryFlv()
      }
    }, 800)
  }

  const connectWhep = async () => {
    if (destroyed || !streamKey) return false
    try {
      if (whepPc) {
        whepPc.close()
        whepPc = null
      }
      whepPc = new RTCPeerConnection({
        iceServers: [{ urls: 'stun:stun.l.google.com:19302' }],
      })
      whepPc.addTransceiver('video', { direction: 'recvonly' })
      whepPc.addTransceiver('audio', { direction: 'recvonly' })

      whepPc.ontrack = (event) => {
        if (!mediaStream) {
          mediaStream = new MediaStream()
          video.srcObject = mediaStream
        }
        const kind = event.track.kind
        mediaStream.getTracks()
          .filter(t => t.kind === kind)
          .forEach(t => mediaStream.removeTrack(t))
        mediaStream.addTrack(event.track)
        event.track.onended = () => scheduleWhepReconnect()
        tryPlay()
        startStallWatch()
      }

      whepPc.oniceconnectionstatechange = () => {
        const s = whepPc?.iceConnectionState
        if (s === 'failed' || s === 'disconnected') {
          scheduleWhepReconnect()
        }
      }

      const offer = await whepPc.createOffer()
      await whepPc.setLocalDescription(offer)

      const eip = getWebRtcEip()
      const whepUrl = `/srs-api/rtc/v1/whep/?app=live&stream=${encodeURIComponent(streamKey)}&eip=${eip}`
      const res = await fetch(whepUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/sdp' },
        body: offer.sdp,
      })
      if (!res.ok) return false

      const answerSdp = await res.text()
      await whepPc.setRemoteDescription({ type: 'answer', sdp: answerSdp })
      await waitIceConnected(whepPc)
      tryPlay()
      startStallWatch()
      return true
    } catch {
      if (whepPc) {
        whepPc.close()
        whepPc = null
      }
      return false
    }
  }

  const tryHls = () => {
    if (destroyed) return
    mode = 'hls'
    if (isSafariNativeHls(video)) {
      video.src = mediaUrl
      video.onerror = () => handleError()
      tryPlay()
      startStallWatch()
      return
    }
    if (!Hls.isSupported()) {
      handleError()
      return
    }
    hlsInstance = new Hls({
      enableWorker: true,
      lowLatencyMode: true,
      liveSyncDurationCount: 3,
      liveMaxLatencyDurationCount: 8,
    })
    hlsInstance.loadSource(mediaUrl)
    hlsInstance.attachMedia(video)
    hlsInstance.on(Hls.Events.MANIFEST_PARSED, () => {
      tryPlay()
      startStallWatch()
    })
    hlsInstance.on(Hls.Events.ERROR, (_, data) => {
      if (data.fatal) handleError()
    })
    video.onplaying = handlePlaying
  }

  const tryFlv = () => {
    if (destroyed) return
    mode = 'flv'
    if (!flvjs.isSupported()) {
      tryHls()
      return
    }
    flvPlayer = flvjs.createPlayer(
      { type: 'flv', url: flvUrl, isLive: true, hasAudio: false, hasVideo: true },
      { enableWorker: true, enableStashBuffer: false, stashInitialSize: 128, lazyLoad: false }
    )
    flvPlayer.attachMediaElement(video)
    flvPlayer.load()
    flvPlayer.on(flvjs.Events.ERROR, () => {
      if (destroyed) return
      if (flvPlayer) {
        flvPlayer.destroy()
        flvPlayer = null
      }
      tryHls()
    })
    video.onplaying = () => {
      handlePlaying()
      startStallWatch()
    }
    tryPlay()
    startStallWatch()
  }

  const bootstrap = async () => {
    mode = 'whep'
    const whepOk = await connectWhep()
    if (!destroyed && !whepOk) {
      tryFlv()
    }
  }

  cleanup()
  destroyed = false
  bootstrap()

  return { destroy: cleanup }
}
