import { get, post } from '@/network/request'

/** 用户注册 */
export function register(username, password, confirmedPassword) {
  return post('/user/account/register', { username, password, confirmedPassword })
}

/** 用户登录 */
export function login(username, password) {
  return post('/user/account/login', { username, password })
}

/** 退出登录 */
export function logout() {
  return get('/user/account/logout')
}

/** 获取当前登录用户信息 */
export function fetchPersonalInfo() {
  return get('/user/personal/info')
}
