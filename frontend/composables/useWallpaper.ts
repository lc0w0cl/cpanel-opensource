import { ref, computed, readonly } from 'vue'

// 全局壁纸状态
const customWallpaper = ref('')
const wallpaperBlur = ref(5)
const wallpaperMask = ref(30)

// 预览状态（用于实时调整，不保存到数据库）
const previewWallpaper = ref('')
const previewBlur = ref(5)
const previewMask = ref(30)
const isPreviewMode = ref(false)

// 内容边距设置
const contentPadding = ref(0)

// 是否已经初始化
const isInitialized = ref(false)

// 计算背景图片URL
const backgroundImageUrl = computed(() => {
  // 如果是预览模式，使用预览状态
  const wallpaperUrl = isPreviewMode.value ? previewWallpaper.value : customWallpaper.value

  if (wallpaperUrl) {
    // 如果是完整URL（包含http或data:），直接使用
    if (wallpaperUrl.startsWith('http') || wallpaperUrl.startsWith('data:')) {
      return wallpaperUrl
    }
    // 如果是相对路径，需要拼接API基础URL
    const config = useRuntimeConfig()
    return `${config.public.apiBaseUrl}${wallpaperUrl}`
  } else {
    return '/background/机甲.png'
  }
})

// 计算CSS变量
const cssVars = computed(() => {
  // 如果是预览模式，使用预览状态
  const blur = isPreviewMode.value ? previewBlur.value : wallpaperBlur.value
  const mask = isPreviewMode.value ? previewMask.value : wallpaperMask.value

  return {
    '--bg-image': `url(${backgroundImageUrl.value})`,
    '--bg-blur': `${blur}px`,
    '--bg-mask': `${mask / 100}`,
    '--content-padding': `${contentPadding.value}px`
  }
})

// 计算背景样式
const backgroundStyle = computed(() => {
  return {
    minHeight: '100vh',
    position: 'relative'
  }
})

// 加载壁纸设置
const loadWallpaperSettings = async () => {
  if (process.client && !isInitialized.value) {
    try {
      // 首先尝试从后端加载配置
      const config = useRuntimeConfig()
      const API_BASE_URL = `${config.public.apiBaseUrl}/api`

      const response = await apiRequest(`${API_BASE_URL}/system-config/wallpaper`)
      const result = await response.json()

      if (result.success) {
        console.log('从后端加载壁纸设置:', result.data)

        // 从后端加载配置
        customWallpaper.value = result.data.wallpaperUrl || ''
        wallpaperBlur.value = result.data.wallpaperBlur !== undefined ? result.data.wallpaperBlur : 5
        wallpaperMask.value = result.data.wallpaperMask !== undefined ? result.data.wallpaperMask : 30

        // 同步到localStorage作为缓存
        if (result.data.wallpaperUrl) {
          localStorage.setItem('customWallpaper', result.data.wallpaperUrl)
        } else {
          localStorage.removeItem('customWallpaper')
        }
        localStorage.setItem('wallpaperBlur', wallpaperBlur.value.toString())
        localStorage.setItem('wallpaperMask', wallpaperMask.value.toString())
      } else {
        // 如果后端加载失败，回退到localStorage
        loadFromLocalStorage()
      }
    } catch (error) {
      console.error('从后端加载壁纸配置失败，使用本地缓存:', error)
      // 如果后端请求失败，回退到localStorage
      loadFromLocalStorage()
    }

    console.log('壁纸设置已更新:', {
      customWallpaper: customWallpaper.value,
      wallpaperBlur: wallpaperBlur.value,
      wallpaperMask: wallpaperMask.value
    })

    isInitialized.value = true
  }
}

// 加载内容边距设置
const loadContentPaddingSettings = async () => {
  if (process.client) {
    try {
      const config = useRuntimeConfig()
      const API_BASE_URL = `${config.public.apiBaseUrl}/api`

      const response = await apiRequest(`${API_BASE_URL}/system-config/content-padding`)
      const result = await response.json()

      if (result.success && result.data) {
        contentPadding.value = result.data.padding || 0
      }
    } catch (error) {
      console.error('加载内容边距设置失败:', error)
    }
  }
}

// 从localStorage加载配置的回退方法
const loadFromLocalStorage = () => {
  const savedWallpaper = localStorage.getItem('customWallpaper')
  const savedBlur = localStorage.getItem('wallpaperBlur')
  const savedMask = localStorage.getItem('wallpaperMask')

  // 加载自定义壁纸（如果有的话）
  if (savedWallpaper) {
    customWallpaper.value = savedWallpaper
  }

  // 始终加载模糊和遮罩设置，即使没有自定义壁纸
  wallpaperBlur.value = savedBlur !== null ? parseInt(savedBlur) : 5
  wallpaperMask.value = savedMask !== null ? parseInt(savedMask) : 30

  console.log('从localStorage加载壁纸设置:', {
    customWallpaper: customWallpaper.value,
    wallpaperBlur: wallpaperBlur.value,
    wallpaperMask: wallpaperMask.value
  })
}

// 监听壁纸变化事件（保存到数据库时触发）
const handleWallpaperChange = async (event: CustomEvent) => {
  console.log('收到壁纸变化事件（已保存）')

  // 如果事件包含详细信息，直接使用
  if (event.detail) {
    customWallpaper.value = event.detail.wallpaperUrl || ''
    wallpaperBlur.value = event.detail.wallpaperBlur !== undefined ? event.detail.wallpaperBlur : 5
    wallpaperMask.value = event.detail.wallpaperMask !== undefined ? event.detail.wallpaperMask : 30

    // 退出预览模式
    isPreviewMode.value = false
  } else {
    // 否则从后端重新加载
    isInitialized.value = false // 重置初始化状态，允许重新加载
    await loadWallpaperSettings()
  }
}

// 监听壁纸预览变化事件（实时调整时触发，不保存到数据库）
const handleWallpaperPreviewChange = async (event: CustomEvent) => {
  console.log('收到壁纸预览变化事件（未保存）')

  if (event.detail) {
    // 更新预览状态
    previewWallpaper.value = event.detail.wallpaperUrl || ''
    previewBlur.value = event.detail.wallpaperBlur !== undefined ? event.detail.wallpaperBlur : 5
    previewMask.value = event.detail.wallpaperMask !== undefined ? event.detail.wallpaperMask : 30

    // 进入预览模式
    isPreviewMode.value = true
  }
}

// 监听内容边距变更事件
const handleContentPaddingChanged = (event) => {
  contentPadding.value = event.detail.padding || 0
}

// 初始化壁纸系统
const initializeWallpaper = async () => {
  if (!isInitialized.value) {
    await loadWallpaperSettings()
    await loadContentPaddingSettings()

    // 监听自定义事件
    if (process.client) {
      window.addEventListener('wallpaperChanged', handleWallpaperChange)
      window.addEventListener('wallpaperPreviewChange', handleWallpaperPreviewChange)
      window.addEventListener('content-padding-changed', handleContentPaddingChanged)
    }
  }
}

// 清理事件监听器
const cleanupWallpaper = () => {
  if (process.client) {
    window.removeEventListener('wallpaperChanged', handleWallpaperChange)
    window.removeEventListener('wallpaperPreviewChange', handleWallpaperPreviewChange)
    window.removeEventListener('content-padding-changed', handleContentPaddingChanged)
  }
}

export const useWallpaper = () => {
  return {
    // 状态
    customWallpaper: readonly(customWallpaper),
    wallpaperBlur: readonly(wallpaperBlur),
    wallpaperMask: readonly(wallpaperMask),
    previewWallpaper: readonly(previewWallpaper),
    previewBlur: readonly(previewBlur),
    previewMask: readonly(previewMask),
    isPreviewMode: readonly(isPreviewMode),
    contentPadding: readonly(contentPadding),
    isInitialized: readonly(isInitialized),
    
    // 计算属性
    backgroundImageUrl,
    cssVars,
    backgroundStyle,
    
    // 方法
    initializeWallpaper,
    cleanupWallpaper,
    loadWallpaperSettings,
    loadContentPaddingSettings
  }
}
