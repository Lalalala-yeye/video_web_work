import { get, postParams } from '@/network/request'

export function fetchNotifications(page = 1, size = 20) {
  return get('/notification/list', { page, size }, { skipErrorHandler: true })
}

export function fetchUnreadCount() {
  return get('/notification/unread-count', {}, { skipErrorHandler: true })
}

export function markNotificationRead(id) {
  return postParams('/notification/read', id != null ? { id } : {})
}
