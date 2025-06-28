// 导入JWT工具函数
import { hasTokens, clearTokens, apiRequest, refreshAccessToken, isTokenExpired, getAccessToken } from '~/composables/useJwt'

export default defineNuxtRouteMiddleware(async (to, from) => {
  // 如果是登录页面或调试页面，直接允许访问
  if (to.path === '/login' || to.path === '/debug-login') {
    return
  }

  // 只在客户端检查token（避免服务端渲染时的问题）
  if (process.client) {
    // 检查是否有JWT token
    if (!hasTokens()) {
      console.log('没有JWT token，重定向到登录页面')
      return navigateTo('/login')
    }
  } else {
    // 服务端渲染时暂时允许访问，让客户端处理认证
    return
  }

  // 在客户端运行时，优先使用缓存的认证状态
  const cachedAuth = sessionStorage.getItem('auth_status')
  if (cachedAuth) {
    try {
      const authData = JSON.parse(cachedAuth)
      // 如果缓存显示已登录且在有效期内，直接允许访问
      if (authData.authenticated && authData.timestamp &&
          (Date.now() - authData.timestamp < 300000)) { // 缓存5分钟
        // 在后台异步验证，不阻塞页面渲染
        verifyAuthInBackground()
        return
      }
    } catch (e) {
      // 缓存数据无效，清除
      sessionStorage.removeItem('auth_status')
    }
  }

  // 如果没有缓存，检查JWT token是否有效
  console.log('认证缓存已过期，检查JWT token有效性')

  const accessToken = getAccessToken()
  if (!accessToken) {
    console.log('没有访问token，重定向到登录页面')
    return navigateTo('/login')
  }

  // 检查token是否过期
  if (isTokenExpired(accessToken)) {
    console.log('访问token已过期，尝试刷新')
    // 尝试刷新token
    const refreshSuccess = await refreshAccessToken()
    if (!refreshSuccess) {
      console.log('Token刷新失败，重定向到登录页面')
      clearTokens()
      return navigateTo('/login')
    }
  }

  // Token有效，更新认证缓存并允许访问
  console.log('JWT token有效，更新认证缓存')
  sessionStorage.setItem('auth_status', JSON.stringify({
    authenticated: true,
    timestamp: Date.now()
  }))

  // 在后台异步验证，不阻塞页面渲染
  verifyAuthInBackground()
})

// 后台验证函数
async function verifyAuthInBackground() {
  try {
    const config = useRuntimeConfig()
    const API_BASE_URL = `${config.public.apiBaseUrl}/api`

    const response = await apiRequest(`${API_BASE_URL}/auth/status`)

    if (!response.ok) {
      if (response.status === 401) {
        console.log('后台验证：Token过期，尝试刷新')
        // Token过期，尝试刷新
        const refreshSuccess = await refreshAccessToken()
        if (!refreshSuccess) {
          // 刷新失败，清除缓存并重定向
          console.log('后台验证：Token刷新失败，清除认证状态')
          clearTokens()
          sessionStorage.removeItem('auth_status')
          await navigateTo('/login')
        } else {
          console.log('后台验证：Token刷新成功')
          // 刷新成功，更新缓存
          sessionStorage.setItem('auth_status', JSON.stringify({
            authenticated: true,
            timestamp: Date.now()
          }))
        }
      } else {
        console.warn('后台验证：服务器响应错误', response.status)
      }
      return
    }

    const result = await response.json()

    if (!result.success || !result.data.authenticated) {
      // 认证失败，清除缓存并重定向
      console.log('后台验证：认证失败，清除认证状态')
      clearTokens()
      sessionStorage.removeItem('auth_status')
      await navigateTo('/login')
    } else {
      // 更新缓存时间戳
      console.log('后台验证：认证成功，更新缓存')
      sessionStorage.setItem('auth_status', JSON.stringify({
        authenticated: true,
        timestamp: Date.now()
      }))
    }
  } catch (error) {
    console.error('后台认证验证失败:', error)
    // 网络错误时不强制跳转，保持当前状态
    // 但如果是严重错误（如token完全无效），可能需要清除状态
    const accessToken = getAccessToken()
    if (!accessToken || isTokenExpired(accessToken)) {
      console.log('后台验证：检测到无效token，清除认证状态')
      clearTokens()
      sessionStorage.removeItem('auth_status')
      await navigateTo('/login')
    }
  }
}
