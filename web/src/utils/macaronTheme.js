import { MACARON_THEMES, DEFAULT_MACARON_THEME_ID } from '@/constants/macaronThemes'

const STORAGE_KEY = 'doinb_macaron_theme'
export const MACARON_THEME_EVENT = 'doinb-macaron-theme-change'

export function getMacaronThemeId() {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved && MACARON_THEMES.some(t => t.id === saved)) {
    return saved
  }
  return DEFAULT_MACARON_THEME_ID
}

export function setMacaronThemeId(id) {
  if (!MACARON_THEMES.some(t => t.id === id)) return
  localStorage.setItem(STORAGE_KEY, id)
  applyMacaronTheme(window.location.pathname)
  window.dispatchEvent(new CustomEvent(MACARON_THEME_EVENT, { detail: id }))
}

/** 管理后台不加马卡龙类；用户端挂载 theme-macaron-{1-8} */
export function applyMacaronTheme(path = window.location.pathname) {
  const root = document.documentElement
  MACARON_THEMES.forEach(t => root.classList.remove(`theme-macaron-${t.id}`))
  root.classList.remove('theme-macaron')
  if (path.startsWith('/admin')) return
  root.classList.add(`theme-macaron-${getMacaronThemeId()}`)
}

export function getMacaronTheme() {
  return MACARON_THEMES.find(t => t.id === getMacaronThemeId()) || MACARON_THEMES[3]
}
