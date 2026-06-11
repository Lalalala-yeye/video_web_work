import { get, postParams } from '@/network/request'

export function fetchLiveList(page = 1, size = 12, config = {}) {
  return get('/live/list', { page, size }, config)
}

export function fetchLiveDetail(id, config = {}) {
  return get('/live/getone', { id }, config)
}

export function createLiveRoom(title) {
  return postParams('/live/create', { title })
}

export function startLive(id) {
  return postParams('/live/start', { id })
}

export function stopLive(id) {
  return postParams('/live/stop', { id })
}

export function fetchMyLiveList(page = 1, size = 12) {
  return get('/live/my/list', { page, size })
}
