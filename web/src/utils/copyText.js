import { ElMessage } from 'element-plus'

export async function copyText(text, successMessage = '已复制') {
  if (!text) {
    ElMessage.warning('没有可复制的内容')
    return false
  }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(successMessage)
    return true
  } catch {
    try {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.left = '-9999px'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
      ElMessage.success(successMessage)
      return true
    } catch {
      ElMessage.error('复制失败，请手动复制')
      return false
    }
  }
}
