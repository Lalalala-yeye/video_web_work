import { get, postForm, postParams } from '@/network/request'

export function fetchVideoList(page = 1, size = 12, config = {}) {
  return get('/video/list', { page, size }, config)
}

export function fetchVideoDetail(id, config = {}) {
  return get('/video/getone', { id }, config)
}

export function fetchMyVideoDetail(id) {
  return get('/video/my/getone', { id }, { skipErrorHandler: true })
}

export function saveProgress(videoId, progress) {
  return postParams('/video/history/progress', { videoId, progress }, { skipErrorHandler: true })
}

export function fetchHistoryList(page = 1, size = 12, config = {}) {
  return get('/video/history/list', { page, size }, config)
}

export function fetchMyVideos(page = 1, size = 12, config = {}) {
  return get('/video/my/list', { page, size }, { skipErrorHandler: true, ...config })
}

export function uploadVideo({ title, description, visibility = 'public', file, cover }) {
  const form = new FormData()
  form.append('title', title)
  if (description) form.append('description', description)
  form.append('visibility', visibility)
  form.append('file', file)
  if (cover) form.append('cover', cover)
  return postForm('/video/upload', form)
}

export function updateVideo({ id, title, description, visibility = 'public', file, cover }, config = {}) {
  const form = new FormData()
  form.append('id', id)
  form.append('title', title)
  if (description != null) form.append('description', description)
  form.append('visibility', visibility)
  if (file) form.append('file', file)
  if (cover) form.append('cover', cover)
  return postForm('/video/update', form, { skipErrorHandler: true, ...config })
}

export function setVideoVisibility(id, visibility, config = {}) {
  return postParams('/video/visibility', { id, visibility }, { skipErrorHandler: true, ...config })
}

export function reportVideo(id, reason, config = {}) {
  return postParams('/video/report', { id, reason }, { skipErrorHandler: true, ...config })
}

export function deleteVideo(id, config = {}) {
  return postParams('/video/delete', { id }, { skipErrorHandler: true, ...config })
}

const STATUS_LABELS = {
  0: '待审核',
  1: '已发布',
  2: '举报待复核',
  3: '仅自己可见',
}

export function videoStatusLabel(status) {
  return STATUS_LABELS[status] ?? '未知'
}

export function statusToVisibility(status) {
  return status === 3 ? 'private' : 'public'
}
