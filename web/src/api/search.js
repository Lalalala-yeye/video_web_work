import { get } from '@/network/request'

export function search(keyword, videoLimit = 12, liveLimit = 12, userLimit = 12) {
  return get('/search', { keyword, videoLimit, liveLimit, userLimit })
}
