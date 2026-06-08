import { get, postForm, postParams } from '@/network/request'

export function fetchVideoList(page = 1, size = 12) {
  return get('/video/list', { page, size })
}

export function fetchVideoDetail(id) {
  return get('/video/getone', { id })
}

export function saveProgress(videoId, progress) {
  return postParams('/video/history/progress', { videoId, progress })
}

export function fetchHistoryList(page = 1, size = 12) {
  return get('/video/history/list', { page, size })
}

export function fetchMyVideos(page = 1, size = 12) {
  return get('/video/my/list', { page, size })
}

export function uploadVideo({ title, description, file, cover }) {
  const form = new FormData()
  form.append('title', title)
  if (description) form.append('description', description)
  form.append('file', file)
  if (cover) form.append('cover', cover)
  return postForm('/video/upload', form)
}
