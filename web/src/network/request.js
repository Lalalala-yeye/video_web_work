import axios from 'axios'
import { getToken } from '@/utils/auth'
import {
  handleBusinessError,
  showKnownError,
  extractErrorMessage,
  isUnknownError,
  handleUnknownError,
} from '@/utils/httpError'

/** 普通接口超时（毫秒），超时视为未知错误并回首页 */
const DEFAULT_TIMEOUT = 30000

const pendingControllers = new Set()

const instance = axios.create({
  baseURL: '/api',
  timeout: DEFAULT_TIMEOUT,
})

function attachAbortController(config) {
  if (config.signal) return config
  const controller = new AbortController()
  config.signal = controller.signal
  config.__abortController = controller
  pendingControllers.add(controller)
  return config
}

function releaseAbortController(config) {
  const controller = config?.__abortController
  if (controller) {
    pendingControllers.delete(controller)
  }
}

function abortAllPending() {
  for (const controller of pendingControllers) {
    controller.abort()
  }
  pendingControllers.clear()
}

instance.interceptors.request.use(config => {
  attachAbortController(config)
  const token = getToken()
  if (token) config.headers.Authorization = token
  return config
})

instance.interceptors.response.use(
  res => {
    releaseAbortController(res.config)
    if (res.config?.skipErrorHandler) {
      return res
    }
    if (res.data?.code && res.data.code !== 200) {
      void handleBusinessError(res.data.message)
    }
    return res
  },
  err => {
    releaseAbortController(err.config)
    if (err.config?.skipErrorHandler || err.code === 'ERR_CANCELED') {
      return Promise.reject(err)
    }

    const skipFatalRedirect = err.config?.skipFatalRedirect

    if (isUnknownError(err) && !skipFatalRedirect) {
      void handleUnknownError(extractErrorMessage(err), abortAllPending)
      return Promise.reject(err)
    }

    void showKnownError(extractErrorMessage(err))
    return Promise.reject(err)
  }
)

export const get = (url, params, config = {}) => instance.get(url, { params, ...config })

export const post = (url, data, config = {}) => instance.post(url, data, config)

/** application/x-www-form-urlencoded，对应后端 @RequestParam */
export function postParams(url, params, config = {}) {
  const body = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value == null || value === '') return
    if (typeof value === 'number' && Number.isNaN(value)) return
    if (value === 'NaN') return
    body.append(key, value)
  })
  return instance.post(url, body, config)
}

/** multipart/form-data 上传（超时较长，失败不强制回首页） */
export function postForm(url, formData, config = {}) {
  return instance.post(url, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000,
    skipFatalRedirect: true,
    ...config,
  })
}
