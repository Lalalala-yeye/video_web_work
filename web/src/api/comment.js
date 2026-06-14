import { get, postParams } from '@/network/request'
import { parseRouteId } from '@/utils/format'

/** targetType: 1=视频 2=直播间 */
export function fetchComments(targetId, targetType, page = 1, size = 20) {
  const id = parseRouteId(targetId)
  if (id == null) return Promise.reject(new Error('无效的目标 id'))
  return get('/comment/list', { targetId: id, targetType, page, size })
}

export function addComment(targetId, targetType, content) {
  const id = parseRouteId(targetId)
  if (id == null) return Promise.reject(new Error('无效的目标 id'))
  return postParams('/comment/add', { targetId: id, targetType, content })
}
