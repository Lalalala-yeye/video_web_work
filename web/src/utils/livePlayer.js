import Hls from 'hls.js'
import flvjs from 'flv.js'
import { resolveMediaUrl, rewriteLiveMediaUrl } from '@/utils/lanUrl'

function toFlvUrl(mediaUrl) {
  return mediaUrl.replace(/\.m3u8(\?.*)?$/i, '.flv')
}

/** 每次进入直播间用新 URL，避免命中旧 m3u8/缓存而从片头播 */
function withLiveCacheBust(url) {
  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}_live=${Date.now()}`
}

function isSafariNativeHls(video) {
  if (!video.canPlayType('application/vnd.apple.mpegurl')) return false
  return /^((?!chrome|android).)*safari/i.test(navigator.userAgent)
}

function seekVideoToLiveEdge(video, hls) {
  if (!video) return
  const edge = hls?.liveSyncPosition
  if (Number.isFinite(edge) && edge > 0) {
    video.currentTime = edge
    return
  }
  const d = video.duration
  if (Number.isFinite(d) && d !== Infinity && d > 2) {
    video.currentTime = Math.max(0, d - 1.5)
  } else if (Number.isFinite(d) && d === Infinity && video.seekable?.length) {
    const end = video.seekable.end(video.seekable.length - 1)
    if (Number.isFinite(end) && end > 1) {
      video.currentTime = Math.max(0, end - 1.5)
    }
  }
}

function createHlsInstance(video, mediaUrl, handlers) {
  const { onError, onPlaying, tryPlay, startStallWatch } = handlers
  const liveUrl = withLiveCacheBust(mediaUrl)

  if (isSafariNativeHls(video)) {
    video.src = withLiveCacheBust(rewriteLiveMediaUrl(mediaUrl))
    video.onerror = () => onError()
    video.onloadedmetadata = () => {
      seekVideoToLiveEdge(video)
      tryPlay()
    }
    video.onplaying = () => onPlaying()
    startStallWatch()
    return null
  }
  if (!Hls.isSupported()) {
    onError()
    return null
  }
  const hls = new Hls({
    enableWorker: true,
    lowLatencyMode: true,
    backBufferLength: 0,
    liveSyncDuration: 2,
    liveMaxLatencyDuration: 8,
    maxLiveSyncPlaybackRate: 1.5,
    liveSyncDurationCount: 2,
    liveMaxLatencyDurationCount: 6,
    startPosition: -1,
    xhrSetup(xhr, url) {
      xhr.open('GET', rewriteLiveMediaUrl(url), true)
    },
  })
  hls.loadSource(liveUrl)
  hls.attachMedia(video)
  hls.on(Hls.Events.MANIFEST_PARSED, () => {
    seekVideoToLiveEdge(video, hls)
    tryPlay()
    startStallWatch()
  })
  hls.on(Hls.Events.LEVEL_UPDATED, () => {
    if (!video || video.paused) return
    const lag = hls.liveSyncPosition - video.currentTime
    if (Number.isFinite(lag) && lag > 12) {
      seekVideoToLiveEdge(video, hls)
    }
  })
  hls.on(Hls.Events.ERROR, (_, data) => {
    if (data.fatal) onError()
  })
  video.onplaying = () => onPlaying()
  return hls
}

/**
 * 观众播放：FLV（实时）→ HLS 回退；每次挂载从直播最新位置开始
 */
export function attachLivePlayer(video, playUrl, { onError, onPlaying } = {}) {
  const mediaUrl = resolveMediaUrl(playUrl)
  const flvUrl = withLiveCacheBust(toFlvUrl(mediaUrl))

  let flvPlayer = null
  let hlsInstance = null
  let stallTimer = null
  let lagTimer = null
  let destroyed = false
  let playingReported = false
  let lastAdvanceAt = 0
  let lastVideoTime = 0

  const clearStall = () => {
    if (stallTimer) {
      clearInterval(stallTimer)
      stallTimer = null
    }
  }

  const clearLag = () => {
    if (lagTimer) {
      clearInterval(lagTimer)
      lagTimer = null
    }
  }

  const cleanup = () => {
    destroyed = true
    clearStall()
    clearLag()
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
    video.onplaying = null
    video.onerror = null
    video.removeAttribute('src')
    video.srcObject = null
    video.load()
    playingReported = false
  }

  const handleError = () => {
    if (!destroyed) onError?.()
  }

  const reportPlaying = () => {
    if (destroyed || playingReported) return
    playingReported = true
    onPlaying?.()
  }

  const tryPlay = () => {
    if (destroyed) return
    video.play()
      .then(() => reportPlaying())
      .catch(() => {})
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
        reportPlaying()
        return
      }
      if (Date.now() - lastAdvanceAt > 12000) {
        handleError()
      }
    }, 1000)
  }

  const startLagWatch = (hls) => {
    clearLag()
    lagTimer = setInterval(() => {
      if (destroyed || !hls) return
      const edge = hls.liveSyncPosition
      if (!Number.isFinite(edge)) return
      const lag = edge - video.currentTime
      if (lag > 15) {
        seekVideoToLiveEdge(video, hls)
      }
    }, 5000)
  }

  const handlers = { onError: handleError, onPlaying: reportPlaying, tryPlay, startStallWatch }

  const tryHls = () => {
    if (destroyed) return
    hlsInstance = createHlsInstance(video, mediaUrl, {
      onError: () => {
        if (destroyed) return
        if (hlsInstance) {
          hlsInstance.destroy()
          hlsInstance = null
          clearLag()
        }
        handleError()
      },
      onPlaying: reportPlaying,
      tryPlay,
      startStallWatch,
    })
    if (hlsInstance) {
      startLagWatch(hlsInstance)
    } else if (!isSafariNativeHls(video) && !Hls.isSupported()) {
      handleError()
    }
  }

  const tryFlv = () => {
    if (destroyed) return
    if (!flvjs.isSupported()) {
      tryHls()
      return
    }
    flvPlayer = flvjs.createPlayer(
      {
        type: 'flv',
        url: flvUrl,
        isLive: true,
        hasAudio: true,
        hasVideo: true,
      },
      {
        enableWorker: false,
        enableStashBuffer: false,
        stashInitialSize: 128,
        lazyLoad: false,
        autoCleanupSourceBuffer: true,
      }
    )
    flvPlayer.attachMediaElement(video)
    flvPlayer.on(flvjs.Events.ERROR, () => {
      if (destroyed) return
      if (flvPlayer) {
        flvPlayer.destroy()
        flvPlayer = null
      }
      tryHls()
    })
    flvPlayer.on(flvjs.Events.MEDIA_INFO, () => tryPlay())
    flvPlayer.on(flvjs.Events.STATISTICS_INFO, () => {
      if (video.readyState >= 2) reportPlaying()
    })
    video.onplaying = () => reportPlaying()
    flvPlayer.load()
    tryPlay()
    startStallWatch()
  }

  // FLV 为真直播，每次进入从当前推流位置播放；失败再 HLS 并追到最新
  tryFlv()

  return { destroy: cleanup }
}
