import { get, postParams } from '@/network/request'

import { parseRouteId } from '@/utils/format'

export function followUser(targetId) {
  const id = parseRouteId(targetId)
  if (id == null) return Promise.reject(new Error('无效的用户 id'))
  return postParams('/subscription/follow', { targetId: id })
}

export function unfollowUser(targetId) {
  const id = parseRouteId(targetId)
  if (id == null) return Promise.reject(new Error('无效的用户 id'))
  return postParams('/subscription/unfollow', { targetId: id })
}

export function fetchFollowing(page = 1, size = 12, config = {}) {
  return get('/subscription/following', { page, size }, config)
}

export function fetchFeed(page = 1, size = 12, config = {}) {
  return get('/subscription/feed', { page, size }, config)
}
