/**
 * useFavicon composable
 * 用于动态更新网站favicon
 */

export const useFavicon = () => {
  const currentFavicon = ref('')

  /**
   * 更新favicon
   * @param iconUrl 图标URL，为空时使用默认favicon
   */
  const updateFavicon = (iconUrl) => {
    if (!process.client) return

    try {
      // 移除现有的favicon链接
      const existingLinks = document.querySelectorAll('link[rel*="icon"]')
      existingLinks.forEach(link => link.remove())

      // 创建新的favicon链接
      const link = document.createElement('link')
      link.rel = 'icon'
      
      if (iconUrl) {
        // 使用自定义图标
        link.href = iconUrl
        
        // 根据文件扩展名设置type
        const extension = iconUrl.split('.').pop()?.toLowerCase()
        switch (extension) {
          case 'svg':
            link.type = 'image/svg+xml'
            break
          case 'png':
            link.type = 'image/png'
            break
          case 'jpg':
          case 'jpeg':
            link.type = 'image/jpeg'
            break
          case 'ico':
            link.type = 'image/x-icon'
            break
          default:
            link.type = 'image/png'
        }
      } else {
        // 使用默认favicon
        link.href = '/favicon.svg'
        link.type = 'image/svg+xml'
      }

      // 添加到head
      document.head.appendChild(link)
      currentFavicon.value = iconUrl

      console.log('Favicon已更新:', iconUrl || '默认')
    } catch (error) {
      console.error('更新favicon失败:', error)
    }
  }

  /**
   * 加载当前logo配置并更新favicon
   */
  const loadAndUpdateFavicon = async () => {
    try {
      const config = useRuntimeConfig()
      const API_BASE_URL = `${config.public.apiBaseUrl}/api`
      
      const response = await apiRequest(`${API_BASE_URL}/system-config/logo`)
      const result = await response.json()
      
      if (result.success && result.data.logoUrl) {
        let logoUrl = result.data.logoUrl
        
        // 如果是相对路径，需要拼接完整URL
        if (!logoUrl.startsWith('http') && !logoUrl.startsWith('data:')) {
          logoUrl = `${config.public.apiBaseUrl}${logoUrl}`
        }
        
        updateFavicon(logoUrl)
      } else {
        updateFavicon('')
      }
    } catch (error) {
      console.error('加载logo配置失败:', error)
      updateFavicon('')
    }
  }

  /**
   * 监听logo变更事件
   */
  const handleLogoChanged = (event) => {
    const logoUrl = event.detail.logoUrl
    
    if (logoUrl) {
      const config = useRuntimeConfig()
      let fullUrl = logoUrl
      
      // 如果是相对路径，需要拼接完整URL
      if (!logoUrl.startsWith('http') && !logoUrl.startsWith('data:')) {
        fullUrl = `${config.public.apiBaseUrl}${logoUrl}`
      }
      
      updateFavicon(fullUrl)
    } else {
      updateFavicon('')
    }
  }

  /**
   * 初始化favicon功能
   */
  const initFavicon = () => {
    if (!process.client) return

    // 加载当前配置
    loadAndUpdateFavicon()

    // 监听logo变更事件
    window.addEventListener('logoChanged', handleLogoChanged)
  }

  /**
   * 清理favicon功能
   */
  const cleanupFavicon = () => {
    if (!process.client) return

    window.removeEventListener('logoChanged', handleLogoChanged)
  }

  return {
    currentFavicon: readonly(currentFavicon),
    updateFavicon,
    loadAndUpdateFavicon,
    initFavicon,
    cleanupFavicon
  }
}
