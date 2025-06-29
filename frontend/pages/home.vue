<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'

// 页面元数据配置
definePageMeta({
  layout: 'dashboard',
  middleware: 'auth'
})

// 壁纸相关状态
const backgroundImageUrl = ref('/background/机甲.png')
const wallpaperBlur = ref(0)
const wallpaperMask = ref(0)
const isPreviewMode = ref(false)
const previewBlur = ref(0)
const previewMask = ref(0)

// CSS变量
const cssVars = computed(() => ({
  '--content-padding': `${contentPadding.value}px`
}))

// 内容边距设置
const contentPadding = ref(0)

// API配置
const config = useRuntimeConfig()
const API_BASE_URL = `${config.public.apiBaseUrl}/api`

// 背景样式
const backgroundStyle = computed(() => ({
  '--bg-image': `url(${backgroundImageUrl.value})`,
  '--bg-blur': `${isPreviewMode.value ? previewBlur.value : wallpaperBlur.value}px`,
  '--bg-mask': `rgba(0, 0, 0, ${(isPreviewMode.value ? previewMask.value : wallpaperMask.value) / 100})`
}))

// 加载壁纸设置
const loadWallpaperSettings = async () => {
  if (process.client) {
    try {
      const response = await apiRequest(`${API_BASE_URL}/system-config/wallpaper`)
      const result = await response.json()

      if (result.success && result.data) {
        const settings = result.data
        backgroundImageUrl.value = settings.backgroundImage || '/background/机甲.png'
        wallpaperBlur.value = settings.blur || 0
        wallpaperMask.value = settings.mask || 0
      }
    } catch (error) {
      console.error('加载壁纸设置失败:', error)
    }
  }
}

// 加载内容边距设置
const loadContentPaddingSettings = async () => {
  if (process.client) {
    try {
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

// 监听壁纸变更事件
const handleWallpaperChanged = (event) => {
  const { backgroundImage, blur, mask } = event.detail
  backgroundImageUrl.value = backgroundImage || '/background/机甲.png'
  wallpaperBlur.value = blur || 0
  wallpaperMask.value = mask || 0
}

// 监听内容边距变更事件
const handleContentPaddingChanged = (event) => {
  contentPadding.value = event.detail.padding || 0
}

// 监听壁纸预览事件
const handleWallpaperPreview = (event) => {
  const { backgroundImage, blur, mask, isPreview } = event.detail
  isPreviewMode.value = isPreview
  if (isPreview) {
    previewBlur.value = blur || 0
    previewMask.value = mask || 0
    backgroundImageUrl.value = backgroundImage || '/background/机甲.png'
  }
}

onMounted(async () => {
  // 加载设置
  await loadWallpaperSettings()
  await loadContentPaddingSettings()

  // 监听自定义事件
  window.addEventListener('wallpaper-changed', handleWallpaperChanged)
  window.addEventListener('content-padding-changed', handleContentPaddingChanged)
  window.addEventListener('wallpaper-preview', handleWallpaperPreview)
})

onUnmounted(() => {
  // 清理事件监听器
  window.removeEventListener('wallpaper-changed', handleWallpaperChanged)
  window.removeEventListener('content-padding-changed', handleContentPaddingChanged)
  window.removeEventListener('wallpaper-preview', handleWallpaperPreview)
})
</script>

<template>
  <div class="home-layout" :style="{ ...backgroundStyle, ...cssVars }">
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

    <!-- 侧边栏 -->
    <Sidebar class="sidebar-component" />

    <!-- 主内容区域 -->
    <div class="content-container">
      <div class="content-glass-panel">
        <div class="glow-border-container">
          <GlowBorder
              :color="['#A07CFE', '#FE8FB5', '#FFBE7B']"
              :border-radius="10"
          />
        </div>
        <div class="content-wrapper">
          <!-- 子路由内容 -->
          <NuxtPage />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-layout {
  display: flex;
  min-height: 100vh;
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
  z-index: -2;
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
  transition: all 0.3s ease;
}

.sidebar-component {
  height: 100vh;
  position: sticky;
  top: 0;
  z-index: 10;
}

.content-container {
  flex: 1;
  padding: 1.5rem;
  max-width: 100%;
  max-height: 100vh;
  display: flex;
  overflow-y: auto;
}

.content-glass-panel {
  width: 100%;
  height: fit-content;
  min-height: calc(100vh - 3rem);
  position: relative;
  border-radius: 10px;
  backdrop-filter: blur(10px);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  overflow: hidden;
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
  z-index: 2;
  padding: var(--content-padding, 0px);
  height: 100%;
  min-height: calc(100vh - 3rem);
}

/* 滚动条样式 */
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
  border-color: rgba(255, 255, 255, 0.2);
}
</style>
