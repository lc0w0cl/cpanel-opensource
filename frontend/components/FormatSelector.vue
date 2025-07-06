<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Icon } from '@iconify/vue'

interface Format {
  formatId: string
  ext: string
  resolution: string
  note?: string
  isAudio?: boolean
  isVideo?: boolean
  bitrate?: string
}

interface Props {
  visible: boolean
  videoUrl: string
  platform: string
  title: string
  artist: string
}

interface Emits {
  (e: 'close'): void
  (e: 'select', format: Format): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const { getAvailableFormats } = useMusicApi()

const isLoading = ref(false)
const formats = ref<Format[]>([])
const selectedFormat = ref<Format | null>(null)
const error = ref('')

// 计算属性：音频格式列表
const audioFormats = computed(() => {
  return formats.value.filter(f => f.isAudio).sort((a, b) => {
    // 优先显示flac格式
    if (a.ext === 'flac' && b.ext !== 'flac') return -1
    if (b.ext === 'flac' && a.ext !== 'flac') return 1
    
    // 然后按比特率排序（高到低）
    const aBitrate = parseInt(a.bitrate?.replace('k', '') || '0')
    const bBitrate = parseInt(b.bitrate?.replace('k', '') || '0')
    return bBitrate - aBitrate
  })
})

// 计算属性：视频格式列表
const videoFormats = computed(() => {
  return formats.value.filter(f => f.isVideo).sort((a, b) => {
    // 按分辨率排序（高到低）
    const aRes = parseInt(a.resolution?.replace('p', '') || '0')
    const bRes = parseInt(b.resolution?.replace('p', '') || '0')
    return bRes - aRes
  })
})

// 获取格式列表
const loadFormats = async () => {
  if (!props.videoUrl || !props.platform) return

  isLoading.value = true
  error.value = ''

  try {
    const formatList = await getAvailableFormats(props.videoUrl, props.platform)
    formats.value = formatList

    // 自动选择默认格式
    autoSelectDefaultFormat()

  } catch (err: any) {
    console.error('获取格式列表失败:', err)
    error.value = err.message || '获取格式列表失败'
  } finally {
    isLoading.value = false
  }
}

// 自动选择默认格式
const autoSelectDefaultFormat = () => {
  if (audioFormats.value.length > 0) {
    // 优先选择flac格式
    const flacFormat = audioFormats.value.find(f => f.ext === 'flac')
    if (flacFormat) {
      selectedFormat.value = flacFormat
      return
    }
    
    // 如果没有flac，选择第一个音频格式（已按质量排序）
    selectedFormat.value = audioFormats.value[0]
  } else if (videoFormats.value.length > 0) {
    // 如果没有音频格式，选择第一个视频格式
    selectedFormat.value = videoFormats.value[0]
  }
}

// 选择格式
const selectFormat = (format: Format) => {
  selectedFormat.value = format
}

// 确认选择
const confirmSelection = () => {
  if (selectedFormat.value) {
    emit('select', selectedFormat.value)
  }
}

// 关闭弹窗
const closeModal = () => {
  emit('close')
}

// 获取格式显示名称
const getFormatDisplayName = (format: Format) => {
  let name = format.ext.toUpperCase()
  
  if (format.isAudio) {
    if (format.bitrate) {
      name += ` (${format.bitrate})`
    }
    name += ' - 音频'
  } else if (format.isVideo) {
    if (format.resolution && format.resolution !== 'audio') {
      name += ` (${format.resolution})`
    }
    name += ' - 视频'
  }
  
  return name
}

// 获取格式描述
const getFormatDescription = (format: Format) => {
  return format.note || '无描述'
}

// 监听弹窗显示状态
watch(() => props.visible, (visible) => {
  if (visible) {
    loadFormats()
  } else {
    // 重置状态
    formats.value = []
    selectedFormat.value = null
    error.value = ''
  }
})
</script>

<template>
  <div v-if="visible" class="format-selector-overlay" @click="closeModal">
    <div class="format-selector-modal" @click.stop>
      <!-- 头部 -->
      <div class="modal-header">
        <div class="header-info">
          <Icon icon="mdi:format-list-bulleted" class="header-icon" />
          <div>
            <h3 class="modal-title">选择下载格式</h3>
            <p class="modal-subtitle">{{ title }} - {{ artist }}</p>
          </div>
        </div>
        <button @click="closeModal" class="close-btn">
          <Icon icon="mdi:close" />
        </button>
      </div>

      <!-- 内容 -->
      <div class="modal-content">
        <!-- 加载状态 -->
        <div v-if="isLoading" class="loading-state">
          <Icon icon="mdi:loading" class="loading-icon" />
          <p>正在获取可用格式...</p>
        </div>

        <!-- 错误状态 -->
        <div v-else-if="error" class="error-state">
          <Icon icon="mdi:alert-circle" class="error-icon" />
          <p>{{ error }}</p>
          <button @click="loadFormats" class="retry-btn">
            <Icon icon="mdi:refresh" />
            重试
          </button>
        </div>

        <!-- 格式列表 -->
        <div v-else-if="formats.length > 0" class="format-lists">
          <!-- 音频格式 -->
          <div v-if="audioFormats.length > 0" class="format-section">
            <h4 class="section-title">
              <Icon icon="mdi:music-note" />
              音频格式
            </h4>
            <div class="format-grid">
              <div
                v-for="format in audioFormats"
                :key="format.formatId"
                class="format-item"
                :class="{ selected: selectedFormat?.formatId === format.formatId }"
                @click="selectFormat(format)"
              >
                <div class="format-info">
                  <div class="format-name">{{ getFormatDisplayName(format) }}</div>
                  <div class="format-desc">{{ getFormatDescription(format) }}</div>
                </div>
                <div class="format-badge" :class="format.ext">
                  {{ format.ext.toUpperCase() }}
                </div>
              </div>
            </div>
          </div>

          <!-- 视频格式 -->
          <div v-if="videoFormats.length > 0" class="format-section">
            <h4 class="section-title">
              <Icon icon="mdi:video" />
              视频格式
            </h4>
            <div class="format-grid">
              <div
                v-for="format in videoFormats"
                :key="format.formatId"
                class="format-item"
                :class="{ selected: selectedFormat?.formatId === format.formatId }"
                @click="selectFormat(format)"
              >
                <div class="format-info">
                  <div class="format-name">{{ getFormatDisplayName(format) }}</div>
                  <div class="format-desc">{{ getFormatDescription(format) }}</div>
                </div>
                <div class="format-badge" :class="format.ext">
                  {{ format.ext.toUpperCase() }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <Icon icon="mdi:format-list-bulleted-square" class="empty-icon" />
          <p>未找到可用格式</p>
        </div>
      </div>

      <!-- 底部操作 -->
      <div class="modal-footer">
        <button @click="closeModal" class="cancel-btn">
          <Icon icon="mdi:close" />
          取消
        </button>
        <button 
          @click="confirmSelection" 
          :disabled="!selectedFormat"
          class="confirm-btn"
        >
          <Icon icon="mdi:download" />
          下载选中格式
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.format-selector-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  padding: 1rem;
}

.format-selector-modal {
  background: linear-gradient(135deg, 
    rgba(30, 30, 30, 0.95) 0%, 
    rgba(20, 20, 20, 0.95) 100%
  );
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 1rem;
  max-width: 600px;
  width: 100%;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.5);
}

/* 头部样式 */
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.header-info {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex: 1;
  min-width: 0;
}

.header-icon {
  width: 1.5rem;
  height: 1.5rem;
  color: rgba(59, 130, 246, 0.9);
  flex-shrink: 0;
}

.modal-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 0.25rem 0;
}

.modal-subtitle {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.close-btn {
  width: 2rem;
  height: 2rem;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 0.5rem;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.9);
}

/* 内容样式 */
.modal-content {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
}

/* 状态样式 */
.loading-state,
.error-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  text-align: center;
}

.loading-icon,
.error-icon,
.empty-icon {
  width: 3rem;
  height: 3rem;
  margin-bottom: 1rem;
}

.loading-icon {
  color: rgba(59, 130, 246, 0.9);
  animation: spin 1s linear infinite;
}

.error-icon {
  color: rgba(239, 68, 68, 0.9);
}

.empty-icon {
  color: rgba(255, 255, 255, 0.4);
}

.loading-state p,
.error-state p,
.empty-state p {
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
  font-size: 0.875rem;
}

.retry-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  margin-top: 1rem;
  border: 1px solid rgba(59, 130, 246, 0.3);
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.15) 0%,
    rgba(59, 130, 246, 0.08) 100%
  );
  color: rgba(59, 130, 246, 0.9);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.retry-btn:hover {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.25) 0%,
    rgba(59, 130, 246, 0.15) 100%
  );
  border-color: rgba(59, 130, 246, 0.5);
}

/* 格式列表样式 */
.format-lists {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.format-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.section-title svg {
  width: 1.25rem;
  height: 1.25rem;
  color: rgba(59, 130, 246, 0.9);
}

.format-grid {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.format-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.05) 0%,
    rgba(255, 255, 255, 0.02) 100%
  );
  cursor: pointer;
  transition: all 0.3s ease;
}

.format-item:hover {
  border-color: rgba(59, 130, 246, 0.3);
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.1) 0%,
    rgba(59, 130, 246, 0.05) 100%
  );
}

.format-item.selected {
  border-color: rgba(59, 130, 246, 0.5);
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.2) 0%,
    rgba(59, 130, 246, 0.1) 100%
  );
}

.format-info {
  flex: 1;
  min-width: 0;
}

.format-name {
  font-size: 0.875rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 0.25rem;
}

.format-desc {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.6);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.format-badge {
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  flex-shrink: 0;
}

.format-badge.flac {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.2), rgba(34, 197, 94, 0.1));
  color: rgba(34, 197, 94, 0.9);
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.format-badge.mp3,
.format-badge.m4a {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2), rgba(59, 130, 246, 0.1));
  color: rgba(59, 130, 246, 0.9);
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.format-badge.webm,
.format-badge.ogg {
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.2), rgba(168, 85, 247, 0.1));
  color: rgba(168, 85, 247, 0.9);
  border: 1px solid rgba(168, 85, 247, 0.3);
}

.format-badge.mp4 {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.2), rgba(245, 158, 11, 0.1));
  color: rgba(245, 158, 11, 0.9);
  border: 1px solid rgba(245, 158, 11, 0.3);
}

/* 底部操作样式 */
.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 1rem;
  padding: 1.5rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.cancel-btn,
.confirm-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cancel-btn {
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  color: rgba(255, 255, 255, 0.7);
}

.cancel-btn:hover {
  border-color: rgba(255, 255, 255, 0.3);
  color: rgba(255, 255, 255, 0.9);
}

.confirm-btn {
  border: 1px solid rgba(34, 197, 94, 0.3);
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.15) 0%,
    rgba(34, 197, 94, 0.08) 100%
  );
  color: rgba(34, 197, 94, 0.9);
}

.confirm-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.25) 0%,
    rgba(34, 197, 94, 0.15) 100%
  );
  border-color: rgba(34, 197, 94, 0.5);
}

.confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 动画 */
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
