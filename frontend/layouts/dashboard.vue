<script setup lang="ts">
import { Motion } from "motion-v";
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue';

// 壁纸相关状态
const customWallpaper = ref('')
const wallpaperBlur = ref(5)
const wallpaperMask = ref(30)

// 预览状态（用于实时调整，不保存到数据库）
const previewWallpaper = ref('')
const previewBlur = ref(5)
const previewMask = ref(30)
const isPreviewMode = ref(false)

// 计算背景样式
const backgroundStyle = computed(() => {
  return {
    minHeight: '100vh',
    position: 'relative'
  }
})

// 计算背景图片样式（用于伪元素）
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
    '--bg-mask': `${mask / 100}`
  }
})

// 加载保存的壁纸设置
const loadWallpaperSettings = async () => {
  if (process.client) {
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
  }
}

// 从localStorage加载配置的回退方法
const loadFromLocalStorage = () => {
  const savedWallpaper = localStorage.getItem('customWallpaper')
  const savedBlur = localStorage.getItem('wallpaperBlur')
  const savedMask = localStorage.getItem('wallpaperMask')

  console.log('从localStorage加载壁纸设置:', {
    savedWallpaper,
    savedBlur,
    savedMask
  })

  // 加载自定义壁纸（如果有的话）
  if (savedWallpaper) {
    customWallpaper.value = savedWallpaper
  } else {
    customWallpaper.value = ''
  }

  // 始终加载模糊和遮罩设置，即使没有自定义壁纸
  wallpaperBlur.value = savedBlur !== null ? parseInt(savedBlur) : 5
  wallpaperMask.value = savedMask !== null ? parseInt(savedMask) : 30
}

// 监听壁纸变化事件（应用壁纸时触发，保存到数据库）
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
    await loadWallpaperSettings()
  }

  // 强制重新渲染
  await nextTick()
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

  // 强制重新渲染
  await nextTick()
}

onMounted(async () => {
  await loadWallpaperSettings()

  // 监听自定义事件，当壁纸设置改变时更新
  if (process.client) {
    window.addEventListener('wallpaperChanged', handleWallpaperChange)
    window.addEventListener('wallpaperPreviewChange', handleWallpaperPreviewChange)
  }
})

onUnmounted(() => {
  if (process.client) {
    window.removeEventListener('wallpaperChanged', handleWallpaperChange)
    window.removeEventListener('wallpaperPreviewChange', handleWallpaperPreviewChange)
  }
})
</script>
<template>
  <div class="layout-container" :style="{ ...backgroundStyle, ...cssVars }">
    <!-- 动态背景层 -->
    <div
      class="dynamic-background"
      :style="{
        backgroundImage: `url(${backgroundImageUrl})`,
        filter: `blur(${isPreviewMode ? previewBlur : wallpaperBlur}px)`
      }"
    ></div>

    <!-- 动态遮罩层 -->
    <div
      class="dynamic-mask"
      :style="{
        backgroundColor: `rgba(0, 0, 0, ${(isPreviewMode ? previewMask : wallpaperMask) / 100})`
      }"
    ></div>

    <div class="content-container">
      <div class="content-glass-panel">
        <div class="glow-border-container">
          <GlowBorder
              :color="['#A07CFE', '#FE8FB5', '#FFBE7B']"
              :border-radius="10"
          />
        </div>
        <div class="content-wrapper">
          <slot /> <!-- 渲染页面内容 -->
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout-container {
  position: relative;
  overflow: hidden;
}

/* 动态背景层 */
.dynamic-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  z-index: -2;
  /* 扩展背景以避免模糊边缘 */
  transform: scale(1.1);
  transition: all 0.3s ease;
}

/* 动态遮罩层 */
.dynamic-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -1;
  pointer-events: none;
  transition: all 0.3s ease;
}

/* 确保内容在背景之上 */
.layout-container > .sidebar-component,
.layout-container > .content-container {
  position: relative;
  z-index: 1;
}
</style>

<style scoped>
.layout-container {
  position: relative;
  display: flex;
  width: 100%;
  min-height: 100vh;
}

.sidebar-component {
  height: 100vh;
  position: sticky;
  top: 0;
}



.content-container {
  flex: 1;
  padding: 1.5rem;
  max-width: 100%;
  max-height: 100vh; /* 限制最大高度为视口高度 */
  display: flex; /* 使子元素能够填充高度 */
}

/* 液态玻璃风格滚动条 - content-container */
.content-container::-webkit-scrollbar {
  width: 8px;
}

.content-container::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.02);
  border-radius: 10px;
  backdrop-filter: blur(2px);
}

.content-container::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 50%,
    rgba(255, 255, 255, 0.05) 100%
  );
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(4px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.content-container::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.25) 0%,
    rgba(255, 255, 255, 0.15) 50%,
    rgba(255, 255, 255, 0.1) 100%
  );
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 2px 8px rgba(255, 255, 255, 0.1);
}

.content-container::-webkit-scrollbar-thumb:active {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.3) 0%,
    rgba(255, 255, 255, 0.2) 50%,
    rgba(255, 255, 255, 0.15) 100%
  );
}

.content-glass-panel {
  border-radius: 1.5rem;
  position: relative;
  flex: 1; /* 填充父容器空间 */
  overflow: hidden; /* 隐藏溢出内容，保持边框完整 */

  /* 更透明的液态玻璃效果 */
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.06) 0%,
    rgba(255, 255, 255, 0.03) 50%,
    rgba(255, 255, 255, 0.02) 100%
  );
  backdrop-filter: blur(0px) saturate(120%);
  -webkit-backdrop-filter: blur(1px) saturate(120%);

  /* 更细腻的边框 */
  border: 1px solid rgba(255, 255, 255, 0.08);

  /* 更柔和的阴影 */
  box-shadow:
    0 4px 20px rgba(0, 0, 0, 0.06),
    0 1px 6px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.1),
    inset 0 -1px 0 rgba(0, 0, 0, 0.04);

  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.glow-border-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 1;
}

.content-wrapper {
  position: relative;
  height: 100%; /* 填满父容器高度 */
  padding: 2rem;
  z-index: 2;
  overflow-y: auto; /* 在内容包装器中处理滚动 */
}

/* 液态玻璃风格滚动条 - content-wrapper */
.content-wrapper::-webkit-scrollbar {
  width: 6px;
}

.content-wrapper::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.01);
  border-radius: 8px;
  backdrop-filter: blur(1px);
}

.content-wrapper::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.12) 0%,
    rgba(255, 255, 255, 0.06) 50%,
    rgba(255, 255, 255, 0.03) 100%
  );
  border-radius: 8px;
  border: 0.5px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(3px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.content-wrapper::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.2) 0%,
    rgba(255, 255, 255, 0.12) 50%,
    rgba(255, 255, 255, 0.08) 100%
  );
  border: 0.5px solid rgba(255, 255, 255, 0.15);
  box-shadow: 0 1px 4px rgba(255, 255, 255, 0.08);
}

.content-wrapper::-webkit-scrollbar-thumb:active {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.25) 0%,
    rgba(255, 255, 255, 0.15) 50%,
    rgba(255, 255, 255, 0.1) 100%
  );
}

/* Firefox 滚动条兼容性 */
.content-container {
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.15) rgba(255, 255, 255, 0.02);
}

.content-wrapper {
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.12) rgba(255, 255, 255, 0.01);
}


/* 响应式布局 */
@media (max-width: 768px) {
  .layout-container {
    flex-direction: column;
  }

  .sidebar-component {
    height: auto;
    position: relative;
    width: 100%;
  }
}
</style>