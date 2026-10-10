import axios from 'axios'

export const api = axios.create({
  baseURL: '/api',
  timeout: 120_000,
})

export const client = api

api.interceptors.request.use(config => {
  const token = localStorage.getItem('chatbase_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('chatbase_token')
      localStorage.removeItem('chatbase_user')
      localStorage.removeItem('chatbase_original_username')
      localStorage.removeItem('chatbase_role')
      localStorage.removeItem('chatbase_admin_id')
      window.location.href = '/login'
      return Promise.reject(error)
    }
    // 统一错误文案：优先后端 message，其次 HTTP 状态提示
    const backendMsg = error.response?.data?.message
    const fallbackMap: Record<number, string> = {
      400: '请求参数错误',
      403: '没有权限执行此操作',
      404: '资源不存在',
      413: '文件过大',
      500: '服务器内部错误',
      502: '网关错误',
      503: '服务暂时不可用',
    }
    const normalized = new Error(
      backendMsg ||
      fallbackMap[error.response?.status as number] ||
      (error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '网络异常，请稍后重试')
    )
    ;(normalized as any).original = error
    ;(normalized as any).status = error.response?.status
    return Promise.reject(normalized)
  }
)

