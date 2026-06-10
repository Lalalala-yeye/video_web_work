import { get, postParams } from '@/network/request'

export function fetchPendingVideos(page = 1, size = 10) {
  return get('/admin/video/pending', { page, size })
}

export function fetchReportReviewVideos(page = 1, size = 10) {
  return get('/admin/video/report-review', { page, size })
}

export function approveVideo(videoId) {
  return postParams('/admin/video/approve', { videoId })
}

export function rejectVideo(videoId) {
  return postParams('/admin/video/reject', { videoId })
}

export function adminDeleteVideo(videoId) {
  return postParams('/admin/video/delete', { videoId })
}
