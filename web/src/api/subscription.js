import { get, postParams } from '@/network/request'

export function followUser(targetId) {
  return postParams('/subscription/follow', { targetId })
}

export function unfollowUser(targetId) {
  return postParams('/subscription/unfollow', { targetId })
}

export function fetchFollowing(page = 1, size = 12, config = {}) {
  return get('/subscription/following', { page, size }, config)
}

export function fetchFeed(page = 1, size = 12, config = {}) {
  return get('/subscription/feed', { page, size }, config)
}
