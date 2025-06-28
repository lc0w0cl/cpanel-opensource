<template>
  <div class="wallpaper-manager">
    <!-- 主要内容区域 - 上下排列布局 -->
    <div class="main-content">
      <!-- 上部：当前壁纸预览和控制 -->
      <div class="top-section">
        <!-- 当前壁纸预览和控制面板 - 左右分栏 -->
        <div class="current-wallpaper-controls">
          <!-- 左侧：当前壁纸预览图片 -->
          <div class="current-wallpaper-section">
            <h3 class="section-title">
              <Icon icon="mdi:image" class="section-icon" />
              当前壁纸
            </h3>
            <div class="wallpaper-preview-image">
              <ClientOnly>
                <CardContainer>
                  <CardBody
                    class="group/card relative size-auto rounded-xl border border-black/[0.1]  p-6 dark:border-white/[0.2] dark:bg-black dark:hover:shadow-2xl dark:hover:shadow-emerald-500/[0.1]"
                  >
                    <CardItem
                      :translate-z="100"
                      class="mt-4 w-full"
                    >
                      <div class="preview-image-container">
                        <!-- 直接使用 img 标签显示图片 -->
                        <img
                          :src="getWallpaperDisplayUrl()"
                          :alt="currentWallpaper ? '自定义壁纸' : '默认壁纸'"
                          class="wallpaper-image  w-full rounded-xl object-cover group-hover/card:shadow-xl"
                          :style="{
                            filter: `blur(${wallpaperBlur}px)`
                          }"
                        />
                      </div>
                    </CardItem>
                  </CardBody>
                </CardContainer>
              </ClientOnly>
            </div>
          </div>

          <!-- 右侧：当前设置和控制面板 -->
          <div class="settings-and-controls">
            <!-- 当前设置显示 -->
            <div class="current-settings">
              <h3 class="section-title">
                <Icon icon="mdi:cog" class="section-icon" />
                当前设置
              </h3>
              <div class="settings-info">
                <div class="info-item">
                  <Icon icon="mdi:check-circle" class="info-icon" />
                  <span class="wallpaper-status">{{ currentWallpaper ? '自定义壁纸' : '默认壁纸' }}</span>
                </div>
                <div class="info-item">
                  <Icon icon="mdi:blur" class="info-icon" />
                  <span class="blur-value">模糊度: {{ wallpaperBlur }}px</span>
                </div>
                <div class="info-item">
                  <Icon icon="mdi:opacity" class="info-icon" />
                  <span class="mask-value">遮罩透明度: {{ wallpaperMask }}%</span>
                </div>
              </div>
            </div>

            <!-- 效果调整控制面板 -->
            <div class="controls-section">
              <h3 class="section-title">
                <Icon icon="mdi:tune" class="section-icon" />
                效果调整
              </h3>

              <!-- 模糊度调整 -->
              <div class="control-group">
                <label class="control-label">
                  <Icon icon="mdi:blur" class="control-icon" />
                  模糊度
                </label>
                <div class="slider-container">
                  <input
                    type="range"
                    min="0"
                    max="20"
                    step="1"
                    v-model="wallpaperBlur"
                    class="control-slider blur-slider"
                    @input="updateWallpaperBlur"
                  />
                  <div class="slider-labels">
                    <span>清晰</span>
                    <span class="current-value">{{ wallpaperBlur }}px</span>
                    <span>模糊</span>
                  </div>
                </div>
              </div>

              <!-- 遮罩调整 -->
              <div class="control-group">
                <label class="control-label">
                  <Icon icon="mdi:opacity" class="control-icon" />
                  遮罩透明度
                </label>
                <div class="slider-container">
                  <input
                    type="range"
                    min="0"
                    max="80"
                    step="5"
                    v-model="wallpaperMask"
                    class="control-slider mask-slider"
                    @input="updateWallpaperMask"
                  />
                  <div class="slider-labels">
                    <span>透明</span>
                    <span class="current-value">{{ wallpaperMask }}%</span>
                    <span>不透明</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 下部：壁纸库 -->
      <div class="bottom-section">
        <div class="wallpaper-gallery-section">
          <div class="gallery-header">
            <h3 class="section-title">
              <Icon icon="mdi:folder-image" class="section-icon" />
              壁纸库
              <span class="wallpaper-count" v-if="wallpaperHistory.length > 0">
                ({{ wallpaperHistory.length }})
              </span>
            </h3>
            <button class="upload-btn" @click="triggerFileUpload">
              <Icon icon="mdi:plus" class="btn-icon" />
              添加壁纸
            </button>
          </div>

          <!-- 隐藏的文件输入 -->
          <input
            ref="fileInput"
            type="file"
            accept="image/*"
            @change="handleFileSelect"
            style="display: none"
          />

          <!-- 壁纸网格 -->
          <div class="wallpaper-grid" v-if="wallpaperHistory.length > 0">
            <div
              v-for="wallpaper in wallpaperHistory"
              :key="wallpaper.id"
              class="wallpaper-item"
              :class="{ active: wallpaper.isCurrent }"
              @click="selectWallpaper(wallpaper)"
            >
              <img :src="getImageUrl(wallpaper.url)" :alt="wallpaper.name" />
              <div class="thumbnail-overlay">
                <div class="overlay-actions">
                  <button
                    class="action-btn apply-btn"
                    @click.stop="applyWallpaper(wallpaper)"
                    :title="wallpaper.isCurrent ? '当前壁纸' : '应用此壁纸'"
                  >
                    <Icon :icon="wallpaper.isCurrent ? 'mdi:check-circle' : 'mdi:check'" />
                  </button>
                  <button
                    class="action-btn delete-btn"
                    @click.stop="deleteWallpaper(wallpaper)"
                    :disabled="wallpaper.isCurrent"
                    :title="wallpaper.isCurrent ? '无法删除当前壁纸' : '删除此壁纸'"
                  >
                    <Icon icon="mdi:delete" />
                  </button>
                </div>
              </div>
              <div v-if="wallpaper.isCurrent" class="current-badge">
                <Icon icon="mdi:check-circle" />
                当前使用
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-else class="empty-state">
            <Icon icon="mdi:image-off" class="empty-icon" />
            <p class="empty-text">暂无历史壁纸</p>
            <p class="empty-hint">点击"添加壁纸"上传您的第一张壁纸</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部操作按钮 -->
    <div class="action-buttons">
      <button
        class="action-btn primary-btn"
        @click="saveSettings"
        :disabled="saving"
      >
        <Icon v-if="saving" icon="mdi:loading" class="spin btn-icon" />
        <Icon v-else icon="mdi:content-save" class="btn-icon" />
        {{ saving ? '保存中...' : '保存设置' }}
      </button>
      <button
        class="action-btn secondary-btn"
        @click="resetToDefault"
        :disabled="resetting"
      >
        <Icon v-if="resetting" icon="mdi:loading" class="spin btn-icon" />
        <Icon v-else icon="mdi:restore" class="btn-icon" />
        {{ resetting ? '重置中...' : '重置默认' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import {apiRequest} from "~/composables/useJwt";
import CardContainer from "~/components/inspira/CardContainer.vue";

// Props
interface Props {
  currentWallpaper: string
  wallpaperBlur: number
  wallpaperMask: number
}

const props = withDefaults(defineProps<Props>(), {
  currentWallpaper: '',
  wallpaperBlur: 5,
  wallpaperMask: 30
})

// Emits
const emit = defineEmits<{
  'update:currentWallpaper': [value: string]
  'update:wallpaperBlur': [value: number]
  'update:wallpaperMask': [value: number]
  'wallpaperChanged': [wallpaper: any]
  'previewWallpaper': []
}>()

// 响应式数据
const wallpaperHistory = ref<any[]>([])
const fileInput = ref<HTMLInputElement | null>(null)
const saving = ref(false)
const resetting = ref(false)

// 计算属性
const currentWallpaper = computed({
  get: () => props.currentWallpaper,
  set: (value) => emit('update:currentWallpaper', value)
})

const wallpaperBlur = computed({
  get: () => props.wallpaperBlur,
  set: (value) => emit('update:wallpaperBlur', value)
})

const wallpaperMask = computed({
  get: () => props.wallpaperMask,
  set: (value) => emit('update:wallpaperMask', value)
})

// API配置
const config = useRuntimeConfig()
const API_BASE_URL = `${config.public.apiBaseUrl}/api`

// 方法
const loadWallpaperHistory = async () => {
  try {
    const response = await apiRequest(`${API_BASE_URL}/system-config/wallpaper/history`)
    const result = await response.json()
    
    if (result.success) {
      wallpaperHistory.value = result.data
    }
  } catch (error) {
    console.error('加载壁纸历史失败:', error)
  }
}

const triggerFileUpload = () => {
  fileInput.value?.click()
}

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    uploadWallpaper(file)
  }
}

const uploadWallpaper = async (file: File) => {
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('blur', wallpaperBlur.value.toString())
    formData.append('mask', wallpaperMask.value.toString())

    const response = await apiRequest(`${API_BASE_URL}/system-config/wallpaper/upload`, {
      method: 'POST',
      body: formData
    })

    const result = await response.json()

    if (result.success) {
      // 重新加载壁纸历史
      await loadWallpaperHistory()
      // 更新当前壁纸
      currentWallpaper.value = result.data.wallpaperUrl
      emit('wallpaperChanged', result.data)
    }
  } catch (error) {
    console.error('上传壁纸失败:', error)
  }
}

const selectWallpaper = (wallpaper: any) => {
  currentWallpaper.value = wallpaper.url
}

const applyWallpaper = async (wallpaper: any) => {
  try {
    const response = await apiRequest(`${API_BASE_URL}/system-config/wallpaper/${wallpaper.id}/apply`, {
      method: 'POST'
    })

    const result = await response.json()

    if (result.success) {
      currentWallpaper.value = wallpaper.url
      await loadWallpaperHistory()
      emit('wallpaperChanged', result.data)
    }
  } catch (error) {
    console.error('应用壁纸失败:', error)
  }
}

const deleteWallpaper = async (wallpaper: any) => {
  if (wallpaper.isCurrent) {
    return
  }

  if (!confirm(`确定要删除壁纸 "${formatWallpaperName(wallpaper.name)}" 吗？`)) {
    return
  }

  try {
    const response = await apiRequest(`${API_BASE_URL}/system-config/wallpaper/${wallpaper.id}`, {
      method: 'DELETE'
    })

    const result = await response.json()

    if (result.success) {
      await loadWallpaperHistory()
    }
  } catch (error) {
    console.error('删除壁纸失败:', error)
  }
}

const updateWallpaperBlur = () => {
  // 实时预览
  if (process.client) {
    window.dispatchEvent(new CustomEvent('wallpaperPreviewChange', {
      detail: {
        wallpaperUrl: currentWallpaper.value,
        wallpaperBlur: wallpaperBlur.value,
        wallpaperMask: wallpaperMask.value,
        isPreview: true
      }
    }))
  }
}

const updateWallpaperMask = () => {
  // 实时预览
  if (process.client) {
    window.dispatchEvent(new CustomEvent('wallpaperPreviewChange', {
      detail: {
        wallpaperUrl: currentWallpaper.value,
        wallpaperBlur: wallpaperBlur.value,
        wallpaperMask: wallpaperMask.value,
        isPreview: true
      }
    }))
  }
}


const saveSettings = async () => {
  saving.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/system-config/wallpaper/settings`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        wallpaperUrl: currentWallpaper.value || null,
        wallpaperBlur: wallpaperBlur.value,
        wallpaperMask: wallpaperMask.value
      })
    })

    const result = await response.json()

    if (result.success) {
      emit('wallpaperChanged', {
        wallpaperUrl: currentWallpaper.value,
        wallpaperBlur: wallpaperBlur.value,
        wallpaperMask: wallpaperMask.value
      })
    }
  } catch (error) {
    console.error('保存设置失败:', error)
  } finally {
    saving.value = false
  }
}

const resetToDefault = async () => {
  resetting.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/system-config/wallpaper/reset`, {
      method: 'POST'
    })

    const result = await response.json()

    if (result.success) {
      currentWallpaper.value = ''
      wallpaperBlur.value = 5
      wallpaperMask.value = 30
      await loadWallpaperHistory()
      emit('wallpaperChanged', {
        wallpaperUrl: '',
        wallpaperBlur: 5,
        wallpaperMask: 30
      })
    }
  } catch (error) {
    console.error('重置失败:', error)
  } finally {
    resetting.value = false
  }
}

const getWallpaperDisplayUrl = () => {
  let wallpaperUrl = currentWallpaper.value || '/background/机甲.png'
  
  if (wallpaperUrl.startsWith('http') || wallpaperUrl.startsWith('data:') || wallpaperUrl.startsWith('/background/')) {
    return wallpaperUrl
  }
  
  return `${config.public.apiBaseUrl}${wallpaperUrl}`
}

const getImageUrl = (url: string) => {
  if (!url || url.startsWith('http') || url.startsWith('data:')) {
    return url
  }
  return `${config.public.apiBaseUrl}${url}`
}

const formatWallpaperName = (name: string) => {
  // 移除时间戳和随机ID，只保留有意义的部分
  return name.replace(/wallpaper_\d+_[a-f0-9]+\./i, '壁纸.')
}

const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 生命周期
onMounted(() => {
  loadWallpaperHistory()
})
</script>

<style scoped>
.wallpaper-manager {
  width: 100%;
  max-width: none;
}

/* 主要内容区域 - 上下排列 */
.main-content {
  display: flex;
  flex-direction: column;
  gap: 2.5rem;
  margin-bottom: 2rem;
}

/* 上部区域 */
.top-section {
  width: 100%;
}

/* 当前壁纸和控制面板 - 左右分栏 */
.current-wallpaper-controls {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2.5rem;
  align-items: start;
}

/* 下部区域 */
.bottom-section {
  width: 100%;
}

/* 当前壁纸预览 */
.current-wallpaper-section {
  margin-bottom: 0;
}

/* 壁纸预览图片容器 */
.wallpaper-preview-image {
  margin-top: 1rem;
}

.preview-image-container {
  position: relative;
  width: 100%;
  /* 16:9 aspect ratio */
  aspect-ratio: 16 / 9;
  border-radius: 1rem;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
}

.preview-image-container:hover {
  transform: translateY(-3px);
}

/* 壁纸图片 */
.wallpaper-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 1rem;
  transition: all 0.3s ease;
}

/* 壁纸遮罩层 */
.wallpaper-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  border-radius: 1rem;
}

/* 右侧设置和控制区域 */
.settings-and-controls {
  display: flex;
  flex-direction: column;
  gap: 2rem;
  height: 100%;
  justify-content: space-between;
}

/* 当前设置显示 */
.current-settings {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.06) 0%,
    rgba(255, 255, 255, 0.03) 100%
  );
  border-radius: 1rem;
  padding: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.settings-info {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 1rem;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 1.1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 1.25rem;
}

.section-icon {
  width: 1.5rem;
  height: 1.5rem;
  color: rgba(59, 130, 246, 0.8);
}

.wallpaper-count {
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 400;
}

/* 旧的预览容器样式已移除，使用新的图片显示方式 */

.preview-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3; /* 确保在遮罩层之上 */
  opacity: 0;
  transition: opacity 0.3s ease;
  background: rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(2px);
}

.preview-image-container:hover .preview-overlay {
  opacity: 1;
}

.preview-btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.9) 0%,
    rgba(59, 130, 246, 0.7) 100%
  );
  color: white;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.preview-btn:hover {
  transform: scale(1.05);
}

/* 预览信息样式已移至右侧设置区域 */

.info-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.875rem;
}

.info-icon {
  width: 1.125rem;
  height: 1.125rem;
  color: rgba(59, 130, 246, 0.7);
  flex-shrink: 0;
}

.wallpaper-status {
  color: rgba(34, 197, 94, 0.9);
  font-weight: 500;
}

.blur-value,
.mask-value {
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

/* 控制面板 */
.controls-section {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.06) 0%,
    rgba(255, 255, 255, 0.03) 100%
  );
  border-radius: 1rem;
  padding: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.control-group {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.control-group:last-child {
  margin-bottom: 0;
}

.control-label {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 0.5rem;
}

.control-icon {
  width: 1.125rem;
  height: 1.125rem;
  color: rgba(59, 130, 246, 0.8);
}

.slider-container {
  position: relative;
}

.control-slider {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  outline: none;
  cursor: pointer;
  -webkit-appearance: none;
  appearance: none;
  background: linear-gradient(to right,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(59, 130, 246, 0.3) 100%
  );
}

.control-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.9) 0%,
    rgba(59, 130, 246, 0.7) 100%
  );
  border: 2px solid rgba(255, 255, 255, 0.2);
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.control-slider::-webkit-slider-thumb:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.slider-labels {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.75rem;
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.6);
}

.current-value {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.2) 0%,
    rgba(59, 130, 246, 0.1) 100%
  );
  color: rgba(59, 130, 246, 0.9);
  padding: 0.25rem 0.5rem;
  border-radius: 0.375rem;
  font-weight: 600;
  border: 1px solid rgba(59, 130, 246, 0.2);
}

/* 壁纸库 */
.wallpaper-gallery-section {
  width: 100%;
}

.gallery-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

.upload-btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.9) 0%,
    rgba(34, 197, 94, 0.7) 100%
  );
  color: white;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s ease;
}

.upload-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(34, 197, 94, 0.3);
}

.wallpaper-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.25rem;
  margin-top: 1rem;
  /* 设置合适的高度用于滚动 */
  max-height: 400px;
  overflow-y: auto;
  /* 自定义滚动条样式 */
  scrollbar-width: thin;
  scrollbar-color: rgba(59, 130, 246, 0.5) rgba(255, 255, 255, 0.1);
}

/* Webkit 浏览器滚动条样式 */
.wallpaper-grid::-webkit-scrollbar {
  width: 8px;
}

.wallpaper-grid::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}

.wallpaper-grid::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.6) 0%,
    rgba(59, 130, 246, 0.4) 100%
  );
  border-radius: 4px;
  transition: background 0.3s ease;
}

.wallpaper-grid::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.8) 0%,
    rgba(59, 130, 246, 0.6) 100%
  );
}

.wallpaper-item {
  position: relative;
  border-radius: 0.875rem;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  /* 16:9 aspect ratio (1920:1080) */
  aspect-ratio: 16 / 9;
}

.wallpaper-item:hover {
  border-color: rgba(59, 130, 246, 0.4);
  transform: translateY(-4px);
  box-shadow: 0 12px 35px rgba(0, 0, 0, 0.25);
}

.wallpaper-item.active {
  border-color: rgba(34, 197, 94, 0.6);
  box-shadow: 0 0 25px rgba(34, 197, 94, 0.4);
}

.wallpaper-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.wallpaper-item:hover img {
  transform: scale(1.05);
}

.thumbnail-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0) 0%,
    rgba(0, 0, 0, 0.7) 100%
  );
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 1rem;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.wallpaper-item:hover .thumbnail-overlay {
  opacity: 1;
}

.overlay-actions {
  display: flex;
  gap: 0.5rem;
}

.action-btn {
  width: 2rem;
  height: 2rem;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  font-size: 0.875rem;
}

.apply-btn {
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.9) 0%,
    rgba(34, 197, 94, 0.7) 100%
  );
  color: white;
}

.delete-btn {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.9) 0%,
    rgba(239, 68, 68, 0.7) 100%
  );
  color: white;
}

.delete-btn:disabled {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.3);
  cursor: not-allowed;
}

.action-btn:hover:not(:disabled) {
  transform: scale(1.1);
}

.current-badge {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.95) 0%,
    rgba(34, 197, 94, 0.8) 100%
  );
  color: white;
  padding: 0.375rem 0.75rem;
  border-radius: 0.5rem;
  font-size: 0.75rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.375rem;
  box-shadow: 0 2px 8px rgba(34, 197, 94, 0.3);
  backdrop-filter: blur(10px);
}



/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 3rem 1rem;
  color: rgba(255, 255, 255, 0.5);
  min-height: 200px;
}

.empty-icon {
  width: 3rem;
  height: 3rem;
  margin-bottom: 1rem;
  opacity: 0.5;
}

.empty-text {
  font-size: 1rem;
  font-weight: 500;
  margin-bottom: 0.5rem;
}

.empty-hint {
  font-size: 0.875rem;
  opacity: 0.7;
}

/* 底部操作按钮 */
.action-buttons {
  display: flex;
  gap: 1.5rem;
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.primary-btn {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.9) 0%,
    rgba(59, 130, 246, 0.7) 100%
  );
  color: white;
}

.secondary-btn {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  color: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.action-buttons .action-btn {
  flex: 1;
  padding: 1rem 2rem;
  border: none;
  border-radius: 0.75rem;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

.action-buttons .action-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.3);
}

.action-buttons .action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.btn-icon {
  width: 1rem;
  height: 1rem;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .current-wallpaper-controls {
    grid-template-columns: 1fr;
    gap: 2rem;
  }
}

@media (max-width: 768px) {
  .main-content {
    gap: 1.5rem;
  }

  .current-wallpaper-controls {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }

  .preview-image-container {
    /* 在移动端保持16:9比例 */
    aspect-ratio: 16 / 9;
  }

  .controls-section {
    padding: 1rem;
  }

  .control-group {
    margin-bottom: 1rem;
  }

  .wallpaper-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 1rem;
    max-height: 300px;
  }

  .action-buttons {
    flex-direction: column;
    gap: 1rem;
  }

  .gallery-header {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }

  .section-title {
    font-size: 1rem;
  }

  .section-icon {
    width: 1.25rem;
    height: 1.25rem;
  }
}

@media (max-width: 480px) {
  .preview-image-container {
    /* 在小屏幕设备上保持16:9比例 */
    aspect-ratio: 16 / 9;
  }

  .wallpaper-grid {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 0.75rem;
    max-height: 250px;
  }

  .action-buttons .action-btn {
    padding: 0.875rem 1.5rem;
    font-size: 0.875rem;
  }
}
</style>
