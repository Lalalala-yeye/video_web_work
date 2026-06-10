/** 登录态：支持单设备多账号，当前活跃账号用于 API 请求 */

const ACCOUNTS_KEY = 'doinb_accounts'
const ACTIVE_ID_KEY = 'doinb_active_id'
const LEGACY_TOKEN_KEY = 'doinb_token'
const LEGACY_USER_KEY = 'doinb_user'

/** 用户信息更新后派发，供导航栏等组件同步 */
export const AUTH_UPDATED_EVENT = 'doinb-auth-updated'

const REFRESH_INTERVAL_MS = 5000
let lastRefreshAt = 0
let refreshPromise = null

function migrateLegacy() {
  const legacyToken = localStorage.getItem(LEGACY_TOKEN_KEY)
  const legacyUser = localStorage.getItem(LEGACY_USER_KEY)
  if (!legacyToken || !legacyUser) return
  try {
    const user = JSON.parse(legacyUser)
    if (user?.id) {
      upsertAccount(user, legacyToken)
      setActiveId(user.id)
    }
  } catch {
    /* ignore */
  }
  localStorage.removeItem(LEGACY_TOKEN_KEY)
  localStorage.removeItem(LEGACY_USER_KEY)
}

function readAccountsRaw() {
  migrateLegacy()
  const raw = localStorage.getItem(ACCOUNTS_KEY)
  if (!raw) return []
  try {
    const list = JSON.parse(raw)
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
}

function writeAccounts(list) {
  localStorage.setItem(ACCOUNTS_KEY, JSON.stringify(list))
}

function setActiveId(id) {
  if (id == null) {
    localStorage.removeItem(ACTIVE_ID_KEY)
  } else {
    localStorage.setItem(ACTIVE_ID_KEY, String(id))
  }
}

function getActiveId() {
  const id = localStorage.getItem(ACTIVE_ID_KEY)
  return id ? Number(id) : null
}

function upsertAccount(user, token) {
  const list = readAccountsRaw()
  const idx = list.findIndex(a => Number(a.user?.id) === Number(user.id))
  const entry = {
    user: {
      id: user.id,
      username: user.username,
      nickname: user.nickname,
      avatar: user.avatar,
      role: user.role,
      bio: user.bio,
    },
    token,
    updatedAt: Date.now(),
  }
  if (idx >= 0) list[idx] = entry
  else list.push(entry)
  writeAccounts(list)
  return entry
}

export function getAccounts() {
  return readAccountsRaw()
}

export function getToken() {
  const activeId = getActiveId()
  if (!activeId) return ''
  const account = readAccountsRaw().find(a => Number(a.user?.id) === activeId)
  return account?.token || ''
}

export function getUser() {
  const activeId = getActiveId()
  if (!activeId) return null
  const account = readAccountsRaw().find(a => Number(a.user?.id) === activeId)
  return account?.user ?? null
}

export function setUser(user) {
  const token = getToken()
  if (!user?.id || !token) return
  upsertAccount(user, token)
  window.dispatchEvent(new CustomEvent(AUTH_UPDATED_EVENT, { detail: user }))
}

export function isLoggedIn() {
  return !!getToken()
}

/** 登录成功后保存 token 和用户信息，并设为当前活跃账号 */
export function saveLoginResult(data) {
  if (!data?.token || !data?.user?.id) return
  upsertAccount(data.user, data.token)
  setActiveId(data.user.id)
  window.dispatchEvent(new CustomEvent(AUTH_UPDATED_EVENT, { detail: data.user }))
}

/** 切换到已登录的其他账号 */
export function switchAccount(userId) {
  const account = readAccountsRaw().find(a => Number(a.user?.id) === Number(userId))
  if (!account) return false
  setActiveId(account.user.id)
  lastRefreshAt = 0
  window.dispatchEvent(new CustomEvent(AUTH_UPDATED_EVENT, { detail: account.user }))
  return true
}

/** 移除指定账号；未指定则移除当前账号 */
export function removeAccount(userId) {
  const targetId = userId != null ? Number(userId) : getActiveId()
  if (!targetId) return

  const list = readAccountsRaw().filter(a => Number(a.user?.id) !== targetId)
  writeAccounts(list)

  if (getActiveId() === targetId) {
    if (list.length > 0) {
      setActiveId(list[0].user.id)
      window.dispatchEvent(new CustomEvent(AUTH_UPDATED_EVENT, { detail: list[0].user }))
    } else {
      setActiveId(null)
      lastRefreshAt = 0
      window.dispatchEvent(new CustomEvent(AUTH_UPDATED_EVENT, { detail: null }))
    }
  }
}

/** 退出当前账号 */
export function clearAuth() {
  removeAccount(getActiveId())
}

/** 退出全部账号 */
export function clearAllAccounts() {
  writeAccounts([])
  setActiveId(null)
  lastRefreshAt = 0
  window.dispatchEvent(new CustomEvent(AUTH_UPDATED_EVENT, { detail: null }))
}

export async function refreshStoredUser(force = false) {
  if (!isLoggedIn()) return null

  const now = Date.now()
  if (!force && now - lastRefreshAt < REFRESH_INTERVAL_MS) {
    return getUser()
  }

  if (refreshPromise) return refreshPromise

  refreshPromise = (async () => {
    try {
      const { fetchPersonalInfo } = await import('@/api/user')
      const res = await fetchPersonalInfo()
      if (res.data.code === 200 && res.data.data) {
        const token = getToken()
        upsertAccount(res.data.data, token)
        lastRefreshAt = Date.now()
        window.dispatchEvent(new CustomEvent(AUTH_UPDATED_EVENT, { detail: res.data.data }))
        return res.data.data
      }
      return getUser()
    } catch {
      return getUser()
    } finally {
      refreshPromise = null
    }
  })()

  return refreshPromise
}

export function setupAuthSync() {
  migrateLegacy()
  if (isLoggedIn()) {
    refreshStoredUser()
  }

  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && isLoggedIn()) {
      refreshStoredUser()
    }
  })
}
