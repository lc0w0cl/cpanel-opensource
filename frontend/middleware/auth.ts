export default defineNuxtRouteMiddleware(async (to, from) => {
  // 如果是登录页面，直接允许访问
  if (to.path === '/login') {
    return
  }

  // 检查是否有JWT token
  if (!hasTokens()) {
    console.log('没有JWT token，重定向到登录页面')
    return navigateTo('/login')
  }

  // 在客户端运行时，优先使用缓存的认证状态
  if (process.client) {
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
  }

  // 服务端渲染或缓存失效时，进行服务端验证
  try {
    const config = useRuntimeConfig()
    const API_BASE_URL = `${config.public.apiBaseUrl}/api`

    // 使用JWT token进行认证
    const response = await apiRequest(`${API_BASE_URL}/auth/status`)

    if (!response.ok) {
      // 如果是401错误，token可能已过期，尝试刷新
      if (response.status === 401) {
        console.log('Token可能已过期，尝试刷新')
        const refreshSuccess = await refreshAccessToken()
        
        if (refreshSuccess) {
          // 刷新成功，重新验证
          const retryResponse = await apiRequest(`${API_BASE_URL}/auth/status`)
          if (retryResponse.ok) {
            const retryResult = await retryResponse.json()
            if (retryResult.success && retryResult.data.authenticated) {
              // 更新缓存
              if (process.client) {
                sessionStorage.setItem('auth_status', JSON.stringify({
                  authenticated: true,
                  timestamp: Date.now()
                }))
              }
              return
            }
          }
        }
        
        // 刷新失败，清除token并重定向
        clearTokens()
        return navigateTo('/login')
      }
      
      // 其他网络错误，如果是客户端且有缓存，暂时允许访问
      if (process.client) {
        const cachedAuth = sessionStorage.getItem('auth_status')
        if (cachedAuth) {
          try {
            const authData = JSON.parse(cachedAuth)
            if (authData.authenticated) {
              console.warn('网络错误，使用缓存状态')
              return // 暂时允许访问
            }
          } catch (e) {
            // 缓存无效
          }
        }
        sessionStorage.removeItem('auth_status')
      }
      return navigateTo('/login')
    }

    const result = await response.json()

    if (!result.success || !result.data.authenticated) {
      // 认证失败，清除token并重定向
      clearTokens()
      if (process.client) {
        sessionStorage.removeItem('auth_status')
      }
      return navigateTo('/login')
    }

    // 认证成功，缓存状态
    if (process.client) {
      sessionStorage.setItem('auth_status', JSON.stringify({
        authenticated: true,
        timestamp: Date.now()
      }))
    }

  } catch (error) {
    console.error('认证验证失败:', error)
    
    // 网络错误，如果是客户端且有缓存，暂时允许访问
    if (process.client) {
      const cachedAuth = sessionStorage.getItem('auth_status')
      if (cachedAuth) {
        try {
          const authData = JSON.parse(cachedAuth)
          if (authData.authenticated && 
              (Date.now() - authData.timestamp < 300000)) {
            console.warn('网络错误，使用缓存状态')
            return // 暂时允许访问
          }
        } catch (e) {
          // 缓存无效
        }
      }
      sessionStorage.removeItem('auth_status')
    }
    
    return navigateTo('/login')
  }
})

// 后台验证函数
async function verifyAuthInBackground() {
  try {
    const config = useRuntimeConfig()
    const API_BASE_URL = `${config.public.apiBaseUrl}/api`

    const response = await apiRequest(`${API_BASE_URL}/auth/status`)

    if (!response.ok) {
      if (response.status === 401) {
        // Token过期，尝试刷新
        const refreshSuccess = await refreshAccessToken()
        if (!refreshSuccess) {
          // 刷新失败，清除缓存并重定向
          clearTokens()
          sessionStorage.removeItem('auth_status')
          await navigateTo('/login')
        }
      }
      return
    }

    const result = await response.json()

    if (!result.success || !result.data.authenticated) {
      // 认证失败，清除缓存并重定向
      clearTokens()
      sessionStorage.removeItem('auth_status')
      await navigateTo('/login')
    } else {
      // 更新缓存时间戳
      sessionStorage.setItem('auth_status', JSON.stringify({
        authenticated: true,
        timestamp: Date.now()
      }))
    }
  } catch (error) {
    console.error('后台认证验证失败:', error)
    // 网络错误时不强制跳转，保持当前状态
  }
}
