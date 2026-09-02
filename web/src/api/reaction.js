import { get, postParams } from '@/network/request'

export function reactVideo(videoId, reaction) {
  return postParams('/video/reaction', { videoId, reaction })
}

export function reactComment(commentId, reaction) {
  return postParams('/comment/reaction', { commentId, reaction })
}

export function fetchVideoReactionSummary(videoId, config = {}) {
  return get('/video/reaction/summary', { videoId }, { skipErrorHandler: true, ...config })
}
