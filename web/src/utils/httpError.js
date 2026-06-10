import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'

let handlingUnknownError = false
let alertChain = Promise.resolve()

let lastBusinessMsg = ''
let lastBusinessAt = 0

/** 后端业务错误（HTTP 200 但 code !== 200）—— 轻量 toast，短时间相同文案去重 */
export function handleBusinessError(message) {
  const msg = message || '请求失败'
  const now = Date.now()
  if (msg === lastBusinessMsg && now - lastBusinessAt < 1000) {
    return Promise.resolve()
  }
  lastBusinessMsg = msg
  lastBusinessAt = now
  ElMessage.warning(msg)
  return Promise.resolve()
}

/** 可识别的 HTTP / 业务错误：弹窗提示，串行队列避免同时弹出多个 */
export function showKnownError(message, title = '提示') {
  alertChain = alertChain.then(async () => {
    if (handlingUnknownError) return
    await ElMessageBox.alert(message || '请求失败', title, {
      confirmButtonText: '知道了',
      type: 'warning',
    })
  }).catch(() => {})
  return alertChain
}

/** 从 axios 错误对象提取可读文案 */
export function extractErrorMessage(err) {
  if (!err) return '请求失败'
  const data = err.response?.data
  if (typeof data === 'string' && data.trim()) return data
  if (data?.message) return data.message
  const headerMsg = err.response?.headers?.message
  if (headerMsg) return headerMsg
  if (err.code === 'ECONNABORTED') return '请求超时，服务器长时间无响应'
  if (!err.response) return '网络连接失败，请检查后端是否已启动'
  const status = err.response.status
  if (status === 401 || status === 403) return '登录已失效或无权限，请重新登录'
  if (status === 404) return '请求的资源不存在'
  if (status >= 500) return '服务器异常，请稍后重试'
  return '请求失败'
}

/** 是否为无法明确处理的异常（超时、断网、5xx 无详情等） */
export function isUnknownError(err) {
  if (!err || err.code === 'ERR_CANCELED') return false
  if (err.code === 'ECONNABORTED') return true
  if (!err.response) return true
  const status = err.response.status
  if (status >= 500) {
    const data = err.response.data
    return !(data && typeof data === 'object' && data.message)
  }
  return false
}

/**
 * 未知错误：取消进行中的请求并返回首页
 * @param {string} message
 * @param {() => void} abortAll
 */
export async function handleUnknownError(message, abortAll) {
  if (handlingUnknownError) return
  handlingUnknownError = true
  abortAll?.()
  try {
    await ElMessageBox.alert(
      message || '连接异常或长时间无响应，已结束本次访问。',
      '页面异常',
      {
        confirmButtonText: '返回首页',
        showClose: false,
        type: 'error',
      }
    )
  } finally {
    handlingUnknownError = false
    if (router.currentRoute.value.path !== '/') {
      router.replace('/')
    }
  }
}
