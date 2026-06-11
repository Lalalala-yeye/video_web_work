/** 查询 SRS 流状态（经 Vite 代理 /srs-api） */

export function parseStreamKeyFromPlayUrl(playUrl) {
  const m = playUrl?.match(/\/live\/([^./?]+)/)
  return m?.[1] || ''
}

export async function fetchSrsStreams() {
  try {
    const res = await fetch('/srs-api/api/v1/streams/')
    if (!res.ok) return []
    const data = await res.json()
    return data.streams || []
  } catch {
    return []
  }
}

/** 推流端 ICE 已连上、SRS 标记 active（可能尚无画面） */
export function isStreamPublishingOnSrs(streams, streamKey) {
  if (!streamKey) return false
  const s = streams.find(item => item.name === streamKey)
  return Boolean(s?.publish?.active)
}

/** 已转成 FLV/HLS 可播（frames>0 或 recv_30s 有码率） */
export function isStreamPlayableOnSrs(streams, streamKey) {
  if (!streamKey) return false
  const s = streams.find(item => item.name === streamKey)
  if (!s?.publish?.active) return false
  const frames = s.frames ?? 0
  const recv = s.kbps?.recv_30s ?? 0
  return frames > 0 || recv >= 30
}

export async function isStreamPublishing(streamKey) {
  const streams = await fetchSrsStreams()
  return isStreamPublishingOnSrs(streams, streamKey)
}

export async function isStreamPlayable(streamKey) {
  const streams = await fetchSrsStreams()
  return isStreamPlayableOnSrs(streams, streamKey)
}

export async function waitStreamPublishing(streamKey, timeoutMs = 45000) {
  const start = Date.now()
  while (Date.now() - start < timeoutMs) {
    if (await isStreamPublishing(streamKey)) return true
    await new Promise(r => setTimeout(r, 1500))
  }
  return false
}

/** 等待 SRS 上出现可播放的推流（观众 / 主播推流成功后调用） */
export async function waitStreamPlayable(streamKey, timeoutMs = 45000) {
  const start = Date.now()
  while (Date.now() - start < timeoutMs) {
    if (await isStreamPlayable(streamKey)) return true
    await new Promise(r => setTimeout(r, 1500))
  }
  return false
}
