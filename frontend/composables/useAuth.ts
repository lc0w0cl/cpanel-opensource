/**
 * 认证状态管理
 */

import { hasTokens, clearTokens, setTokens, apiRequest } from './useJwt'

/**
 * 设置认证状态缓存
 */
export const setAuthStatus = (authenticated: boolean) => {
  if (process.client) {
    if (authenticated) {
      sessionStorage.setItem('auth_status', JSON.stringify({
        authenticated: true,
        timestamp: Date.now()
      }))
    } else {
      sessionStorage.removeItem('auth_status')
    }
  }
}

/**
 * 获取认证状态缓存
 */
export const getAuthStatus = () => {
  if (!process.client) return null
  
  try {
    const cachedAuth = sessionStorage.getItem('auth_status')
    if (cachedAuth) {
      const authData = JSON.parse(cachedAuth)
      // 检查缓存是否在有效期内（5分钟）
      if (authData.authenticated && authData.timestamp &&
          (Date.now() - authData.timestamp < 300000)) {
        return authData
      }
    }
  } catch (e) {
    // 缓存数据无效，清除
    sessionStorage.removeItem('auth_status')
  }
  
  return null
}

/**
 * 检查用户是否已认证
 */
export const isAuthenticated = () => {
  // 首先检查缓存
  const cachedAuth = getAuthStatus()
  if (cachedAuth && cachedAuth.authenticated) {
    return true
  }
  
  // 然后检查token
  return hasTokens()
}

/**
 * 登出用户
 */
export const logout = async () => {
  try {
    // 调用后端登出API
    const config = useRuntimeConfig()
    const API_BASE_URL = `${config.public.apiBaseUrl}/api`
    
    await apiRequest(`${API_BASE_URL}/auth/logout`, {
      method: 'POST'
    })
  } catch (error) {
    console.error('登出API调用失败:', error)
  } finally {
    // 无论API调用是否成功，都清除本地状态
    clearTokens()
    setAuthStatus(false)
  }
}

/**
 * 验证当前认证状态（后台验证）
 */
export const verifyAuthStatus = async () => {
  if (!hasTokens()) {
    setAuthStatus(false)
    return false
  }
  
  try {
    const config = useRuntimeConfig()
    const API_BASE_URL = `${config.public.apiBaseUrl}/api`
    
    const response = await apiRequest(`${API_BASE_URL}/auth/status`)
    
    if (response.ok) {
      const result = await response.json()
      if (result.success && result.data.authenticated) {
        setAuthStatus(true)
        return true
      }
    }
    
    // 验证失败
    setAuthStatus(false)
    return false
    
  } catch (error) {
    console.error('认证状态验证失败:', error)
    // 网络错误时，如果有缓存则保持当前状态
    const cachedAuth = getAuthStatus()
    return cachedAuth ? cachedAuth.authenticated : false
  }
}
