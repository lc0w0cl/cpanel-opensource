/**
 * JWT工具函数
 */

// Token存储键名
const ACCESS_TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'

/**
 * JWT Token接口
 */
interface JwtTokens {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

/**
 * 存储JWT tokens
 */
export const setTokens = (tokens: JwtTokens) => {
  if (process.client) {
    localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken)
    
    // 同时存储到sessionStorage作为备份
    sessionStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken)
    sessionStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken)
  }
}

/**
 * 获取访问token
 */
export const getAccessToken = (): string | null => {
  if (process.client) {
    return localStorage.getItem(ACCESS_TOKEN_KEY) || sessionStorage.getItem(ACCESS_TOKEN_KEY)
  }
  return null
}

/**
 * 获取刷新token
 */
export const getRefreshToken = (): string | null => {
  if (process.client) {
    return localStorage.getItem(REFRESH_TOKEN_KEY) || sessionStorage.getItem(REFRESH_TOKEN_KEY)
  }
  return null
}

/**
 * 清除所有tokens
 */
export const clearTokens = () => {
  if (process.client) {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    sessionStorage.removeItem(ACCESS_TOKEN_KEY)
    sessionStorage.removeItem(REFRESH_TOKEN_KEY)
    
    // 清除认证状态缓存
    sessionStorage.removeItem('auth_status')
  }
}

/**
 * 检查token是否存在
 */
export const hasTokens = (): boolean => {
  return !!(getAccessToken() && getRefreshToken())
}

/**
 * 解析JWT token（不验证签名，仅用于获取payload信息）
 */
export const parseJwtToken = (token: string) => {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) {
      return null
    }
    
    const payload = parts[1]
    const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(decoded)
  } catch (error) {
    console.error('解析JWT token失败:', error)
    return null
  }
}

/**
 * 检查token是否即将过期（提前5分钟）
 */
export const isTokenExpiringSoon = (token: string): boolean => {
  const payload = parseJwtToken(token)
  if (!payload || !payload.exp) {
    return true
  }
  
  const expirationTime = payload.exp * 1000 // 转换为毫秒
  const currentTime = Date.now()
  const fiveMinutes = 5 * 60 * 1000 // 5分钟
  
  return (expirationTime - currentTime) < fiveMinutes
}

/**
 * 检查token是否已过期
 */
export const isTokenExpired = (token: string): boolean => {
  const payload = parseJwtToken(token)
  if (!payload || !payload.exp) {
    return true
  }
  
  const expirationTime = payload.exp * 1000 // 转换为毫秒
  const currentTime = Date.now()
  
  return currentTime >= expirationTime
}

/**
 * 获取token剩余有效时间（秒）
 */
export const getTokenRemainingTime = (token: string): number => {
  const payload = parseJwtToken(token)
  if (!payload || !payload.exp) {
    return 0
  }
  
  const expirationTime = payload.exp * 1000 // 转换为毫秒
  const currentTime = Date.now()
  const remaining = Math.max(0, Math.floor((expirationTime - currentTime) / 1000))
  
  return remaining
}

/**
 * 创建带有Authorization头的请求配置
 */
export const createAuthHeaders = () => {
  const token = getAccessToken()
  if (!token) {
    return {}
  }
  
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
}

/**
 * 刷新访问token
 */
export const refreshAccessToken = async (): Promise<boolean> => {
  try {
    const refreshToken = getRefreshToken()
    if (!refreshToken) {
      console.error('没有刷新token')
      return false
    }
    
    const config = useRuntimeConfig()
    const API_BASE_URL = `${config.public.apiBaseUrl}/api`
    
    const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        refreshToken: refreshToken
      })
    })
    
    if (!response.ok) {
      console.error('刷新token请求失败:', response.status)
      return false
    }
    
    const result = await response.json()
    
    if (result.success && result.data) {
      // 存储新的tokens
      setTokens(result.data)
      console.log('Token刷新成功')
      return true
    } else {
      console.error('刷新token失败:', result.message)
      return false
    }
  } catch (error) {
    console.error('刷新token异常:', error)
    return false
  }
}

/**
 * 自动刷新token（如果即将过期）
 */
export const autoRefreshToken = async (): Promise<boolean> => {
  const accessToken = getAccessToken()
  if (!accessToken) {
    return false
  }
  
  // 如果token即将过期，尝试刷新
  if (isTokenExpiringSoon(accessToken)) {
    console.log('Token即将过期，尝试自动刷新')
    return await refreshAccessToken()
  }
  
  return true
}

/**
 * 带有自动token刷新的API请求函数
 */
export const apiRequest = async (url: string, options: RequestInit = {}): Promise<Response> => {
  // 尝试自动刷新token
  await autoRefreshToken()
  
  // 添加认证头
  const headers = {
    ...createAuthHeaders(),
    ...options.headers
  }
  
  const requestOptions = {
    ...options,
    headers
  }
  
  const response = await fetch(url, requestOptions)
  
  // 如果返回401，尝试刷新token并重试
  if (response.status === 401) {
    console.log('收到401响应，尝试刷新token')
    const refreshSuccess = await refreshAccessToken()
    
    if (refreshSuccess) {
      // 重新添加认证头并重试请求
      const newHeaders = {
        ...createAuthHeaders(),
        ...options.headers
      }
      
      const retryOptions = {
        ...options,
        headers: newHeaders
      }
      
      return await fetch(url, retryOptions)
    } else {
      // 刷新失败，清除tokens并跳转到登录页
      clearTokens()
      await navigateTo('/login')
    }
  }
  
  return response
}
