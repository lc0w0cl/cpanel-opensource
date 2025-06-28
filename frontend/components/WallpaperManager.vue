<template>
  <div class="wallpaper-manager">
    <!-- 当前壁纸预览 -->
    <div class="current-wallpaper-section">
      <h3 class="section-title">
        <Icon icon="mdi:image" class="section-icon" />
        当前壁纸
      </h3>
      <div class="current-wallpaper-preview">
        <div class="preview-container" @click="previewWallpaper">
          <!-- 背景图片层 -->
          <div class="background-layer" :style="{
            backgroundImage: `url(${getWallpaperDisplayUrl()})`,
            filter: `blur(${wallpaperBlur}px)`
          }"></div>
          <!-- 遮罩层 -->
          <div class="preview-mask" :style="{
            backgroundColor: `rgba(0, 0, 0, ${wallpaperMask / 100})`
          }"></div>
          <!-- 预览按钮 -->
          <div class="preview-overlay">
            <button class="preview-btn">
              <Icon icon="mdi:eye" class="btn-icon" />
              预览
            </button>
          </div>
        </div>
        <div class="preview-info">
          <span class="wallpaper-status">{{ currentWallpaper ? '自定义壁纸' : '默认壁纸' }}</span>
          <span class="blur-value">模糊度: {{ wallpaperBlur }}px</span>
          <span class="mask-value">遮罩: {{ wallpaperMask }}%</span>
        </div>
      </div>
    </div>

    <!-- 控制面板 -->
    <div class="controls-section">
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
            <span>{{ wallpaperBlur }}px</span>
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
            <span>{{ wallpaperMask }}%</span>
            <span>不透明</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 壁纸库 -->
    <div class="wallpaper-gallery-section">
      <div class="gallery-header">
        <h3 class="section-title">
          <Icon icon="mdi:folder-image" class="section-icon" />
          壁纸库
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
        >
          <div class="wallpaper-thumbnail" @click="selectWallpaper(wallpaper)">
            <img :src="getImageUrl(wallpaper.url)" :alt="wallpaper.name" />
            <div class="thumbnail-overlay">
              <div class="overlay-actions">
                <button class="action-btn apply-btn" @click.stop="applyWallpaper(wallpaper)">
                  <Icon icon="mdi:check" />
                </button>
                <button class="action-btn delete-btn" @click.stop="deleteWallpaper(wallpaper)" :disabled="wallpaper.isCurrent">
                  <Icon icon="mdi:delete" />
                </button>
              </div>
            </div>
            <div v-if="wallpaper.isCurrent" class="current-badge">
              <Icon icon="mdi:check-circle" />
              当前
            </div>
          </div>
          <div class="wallpaper-info">
            <span class="wallpaper-name">{{ formatWallpaperName(wallpaper.name) }}</span>
            <span class="wallpaper-size">{{ formatFileSize(wallpaper.size) }}</span>
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

    <!-- 操作按钮 -->
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

const previewWallpaper = () => {
  emit('previewWallpaper')
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
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

/* 当前壁纸预览 */
.current-wallpaper-section {
  margin-bottom: 1.5rem;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 1rem;
}

.section-icon {
  width: 1.25rem;
  height: 1.25rem;
  color: rgba(59, 130, 246, 0.8);
}

.current-wallpaper-preview {
  margin-bottom: 1.5rem;
}

.preview-container {
  width: 100%;
  height: 120px;
  border-radius: 0.75rem;
  overflow: hidden;
  position: relative;
  cursor: pointer;
  border: 2px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.preview-container:hover {
  border-color: rgba(59, 130, 246, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.3);
}

.background-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  transform: scale(1.1);
}

.preview-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.preview-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.preview-container:hover .preview-overlay {
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

.preview-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 0.75rem;
  padding: 0 0.5rem;
  flex-wrap: wrap;
  gap: 0.5rem;
  font-size: 0.75rem;
}

.wallpaper-status {
  color: rgba(249, 115, 22, 0.8);
  font-weight: 600;
  padding: 0.25rem 0.5rem;
  background: linear-gradient(135deg,
    rgba(249, 115, 22, 0.1) 0%,
    rgba(249, 115, 22, 0.05) 100%
  );
  border-radius: 0.375rem;
  border: 1px solid rgba(249, 115, 22, 0.2);
}

.blur-value,
.mask-value {
  color: rgba(255, 255, 255, 0.6);
}

/* 控制面板 */
.controls-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

.control-group {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.control-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.8);
}

.control-icon {
  width: 1rem;
  height: 1rem;
  color: rgba(59, 130, 246, 0.7);
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
  margin-top: 0.5rem;
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.5);
}

/* 壁纸库 */
.wallpaper-gallery-section {
  margin-top: 1rem;
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
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
}

.wallpaper-item {
  border-radius: 0.75rem;
  overflow: hidden;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  border: 2px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.wallpaper-item:hover {
  border-color: rgba(59, 130, 246, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.3);
}

.wallpaper-item.active {
  border-color: rgba(34, 197, 94, 0.5);
  box-shadow: 0 0 20px rgba(34, 197, 94, 0.3);
}

.wallpaper-thumbnail {
  position: relative;
  width: 100%;
  height: 120px;
  cursor: pointer;
  overflow: hidden;
}

.wallpaper-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.wallpaper-item:hover .wallpaper-thumbnail img {
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
  top: 0.5rem;
  right: 0.5rem;
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.9) 0%,
    rgba(34, 197, 94, 0.7) 100%
  );
  color: white;
  padding: 0.25rem 0.5rem;
  border-radius: 0.375rem;
  font-size: 0.75rem;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.wallpaper-info {
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.wallpaper-name {
  font-size: 0.875rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.wallpaper-size {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.5);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  color: rgba(255, 255, 255, 0.5);
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

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 1rem;
  margin-top: 1.5rem;
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
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  transition: all 0.3s ease;
}

.action-buttons .action-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
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
@media (max-width: 768px) {
  .controls-section {
    grid-template-columns: 1fr;
    gap: 1rem;
  }

  .wallpaper-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 0.75rem;
  }

  .wallpaper-thumbnail {
    height: 100px;
  }

  .action-buttons {
    flex-direction: column;
  }

  .gallery-header {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }
}
</style>
