/** 登录态：token 与用户信息存 localStorage（与 README 约定一致） */

const TOKEN_KEY = 'doinb_token'
const USER_KEY = 'doinb_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function getUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function setUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function isLoggedIn() {
  return !!getToken()
}

/** 登录成功后保存 token 和用户信息 */
export function saveLoginResult(data) {
  if (data?.token) setToken(data.token)
  if (data?.user) setUser(data.user)
}
