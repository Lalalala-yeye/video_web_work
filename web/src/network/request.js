import axios from 'axios'
import { ElMessage } from 'element-plus'

const instance = axios.create({
  baseURL: '/api',
  timeout: 30000
})

instance.interceptors.request.use(config => {
  const token = localStorage.getItem('doinb_token')
  if (token) config.headers.Authorization = token
  return config
})

instance.interceptors.response.use(
  res => {
    if (res.data.code && res.data.code !== 200) {
      ElMessage.error(res.data.message || '请求失败')
    }
    return res
  },
  () => {
    ElMessage.error('网络错误')
    return Promise.reject()
  }
)

export const get = (url, params) => instance.get(url, { params })
export const post = (url, data) => instance.post(url, data)