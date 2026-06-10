import { get, postForm, postParams } from '@/network/request'

export function fetchVideoList(page = 1, size = 12) {
  return get('/video/list', { page, size })
}

export function fetchVideoDetail(id) {
  return get('/video/getone', { id })
}

export function fetchMyVideoDetail(id) {
  return get('/video/my/getone', { id }, { skipErrorHandler: true })
}

export function saveProgress(videoId, progress) {
  return postParams('/video/history/progress', { videoId, progress }, { skipErrorHandler: true })
}

export function fetchHistoryList(page = 1, size = 12) {
  return get('/video/history/list', { page, size })
}

export function fetchMyVideos(page = 1, size = 12, config = {}) {
  return get('/video/my/list', { page, size }, { skipErrorHandler: true, ...config })
}

export function uploadVideo({ title, description, file, cover }) {
  const form = new FormData()
  form.append('title', title)
  if (description) form.append('description', description)
  form.append('file', file)
  if (cover) form.append('cover', cover)
  return postForm('/video/upload', form)
}

export function updateVideo({ id, title, description, file, cover, status }, config = {}) {
  const form = new FormData()
  form.append('id', id)
  form.append('title', title)
  if (description != null) form.append('description', description)
  if (file) form.append('file', file)
  if (cover) form.append('cover', cover)
  return postForm('/video/update', form, { skipErrorHandler: true, ...config })
}

export function updateVideoStatus(id, status, config = {}) {
  return postParams('/video/status', { id, status }, { skipErrorHandler: true, ...config })
}

export function deleteVideo(id, config = {}) {
  return postParams('/video/delete', { id }, { skipErrorHandler: true, ...config })
}

const STATUS_LABELS = { 0: '审核中', 1: '已发布', 2: '已下架' }

export function videoStatusLabel(status) {
  return STATUS_LABELS[status] ?? '未知'
}
