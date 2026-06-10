import { get, post, postForm } from '@/network/request'

/** 用户注册 */
export function register(username, password, confirmedPassword) {
  return post('/user/account/register', { username, password, confirmedPassword }, { skipErrorHandler: true })
}

/** 用户登录 */
export function login(username, password) {
  return post('/user/account/login', { username, password }, { skipErrorHandler: true })
}

/** 退出登录 */
export function logout() {
  return get('/user/account/logout', {}, { skipErrorHandler: true })
}

/** 获取当前登录用户信息 */
export function fetchPersonalInfo(config = {}) {
  return get('/user/personal/info', {}, { skipErrorHandler: true, ...config })
}

/** 用户公开展示页 */
export function fetchUserShowcase(uid, page = 1, size = 12) {
  return get('/user/profile/showcase', { uid, page, size })
}

/** 上传头像 */
export function uploadAvatar(file) {
  const form = new FormData()
  form.append('file', file)
  return postForm('/user/avatar/upload', form)
}
