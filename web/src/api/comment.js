import { get, postParams } from '@/network/request'

/** targetType: 1=视频 2=直播间 */
export function fetchComments(targetId, targetType, page = 1, size = 20) {
  return get('/comment/list', { targetId, targetType, page, size })
}

export function addComment(targetId, targetType, content) {
  return postParams('/comment/add', { targetId, targetType, content })
}
