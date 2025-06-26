<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { Icon } from '@iconify/vue'
import { PhotoIcon, LinkIcon } from '@heroicons/vue/24/outline'
import { getImageUrl } from '~/lib/utils'
import { apiRequest } from '~/composables/useJwt'

// 定义组件的 props
interface Props {
  visible: boolean
  mode: 'add' | 'edit'
  categoryId?: string
  item?: {
    id: string
    name: string
    url: string
    logo: string
    categoryId: string
    description?: string
    internalUrl?: string
  }
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  mode: 'add',
  categoryId: '',
  item: undefined
})

// 定义组件的 emits
const emit = defineEmits<{
  'update:visible': [value: boolean]
  'confirm': [data: any]
}>()

// 表单数据
const formData = ref({
  name: '',
  description: '',
  url: '',
  internalUrl: '',
  logo: '',
  categoryId: '',
  iconType: 'upload' as 'upload' | 'online' | 'heroicon'
})

// 图标选择相关
const fileInput = ref<HTMLInputElement>()
const iconUrl = ref('')
const selectedHeroIcon = ref('')
const selectedFile = ref<File | null>(null)
const previewUrl = ref('')
const serverImagePath = ref('')
const selectedIconifyIcon = ref('')
const customIconifyIcon = ref('')
const imageLoadError = ref(false)

// 获取图标相关
const fetchingIcon = ref(false)



// 计算预览图标
const previewIcon = computed(() => {
  if (formData.value.iconType === 'upload') {
    // 优先显示新选择的文件预览，其次显示现有的logo（需要处理URL）
    if (previewUrl.value) {
      return previewUrl.value
    } else if (formData.value.logo) {
      return getImageUrl(formData.value.logo)
    }
    return null
  } else if (formData.value.iconType === 'online') {
    // 在线图标：优先使用iconUrl.value，其次使用formData.value.logo
    return iconUrl.value || formData.value.logo || null
  } else if (formData.value.iconType === 'iconify') {
    // Iconify图标：优先使用selectedIconifyIcon.value，其次使用formData.value.logo
    return selectedIconifyIcon.value || formData.value.logo || null
  }
  return null
})

// 监听 props 变化，初始化表单数据
watch(() => props.visible, (newVisible) => {
  if (newVisible) {
    if (props.mode === 'edit' && props.item) {
      // 编辑模式，填充现有数据
      formData.value = {
        name: props.item.name,
        description: props.item.description || '',
        url: props.item.url,
        internalUrl: props.item.internalUrl || '',
        logo: props.item.logo,
        categoryId: props.item.categoryId,
        iconType: 'upload'
      }
      // 根据现有logo判断图标类型
      if (props.item.logo.startsWith('http')) {
        formData.value.iconType = 'online'
        iconUrl.value = props.item.logo
      } else if (props.item.logo.startsWith('/uploads/')) {
        formData.value.iconType = 'upload'
        // 对于编辑模式，显示现有的图片
        previewUrl.value = ''
        // 设置服务器图片路径
        serverImagePath.value = props.item.logo
      } else {
        // Iconify 图标（包含冒号的图标名称或其他格式）
        formData.value.iconType = 'iconify'
        selectedIconifyIcon.value = props.item.logo
        customIconifyIcon.value = props.item.logo
      }
    } else {
      // 新增模式，重置表单
      formData.value = {
        name: '',
        description: '',
        url: '',
        internalUrl: '',
        logo: '',
        categoryId: props.categoryId || '',
        iconType: 'upload'
      }
      iconUrl.value = ''
      selectedIconifyIcon.value = ''
      customIconifyIcon.value = ''
      selectedFile.value = null
      previewUrl.value = ''
      serverImagePath.value = ''
      imageLoadError.value = false
    }
  }
})

// 处理文件上传
const handleFileUpload = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    // 验证文件类型
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/bmp', 'image/webp', 'image/svg+xml']
    if (!allowedTypes.includes(file.type)) {
      alert('请选择有效的图片文件（jpg, jpeg, png, gif, bmp, webp, svg）')
      return
    }

    // 验证文件大小（10MB）
    const maxSize = 10 * 1024 * 1024
    if (file.size > maxSize) {
      alert('文件大小不能超过10MB')
      return
    }

    selectedFile.value = file

    // 创建预览URL
    const reader = new FileReader()
    reader.onload = (e) => {
      previewUrl.value = e.target?.result as string
    }
    reader.readAsDataURL(file)

    // 显示将要上传的文件名（预估服务器路径）
    serverImagePath.value = `将上传: ${file.name}`
  }
}

// 处理在线图标URL变化
const handleIconUrlChange = () => {
  if (formData.value.iconType === 'online') {
    formData.value.logo = iconUrl.value
    // 重置图片加载错误状态
    imageLoadError.value = false
  }
}

// 处理图片加载错误
const handleImageError = () => {
  imageLoadError.value = true
}

// 处理图片加载成功
const handleImageLoad = () => {
  imageLoadError.value = false
}

// 处理 Iconify 图标选择
const handleIconifyIconSelect = (iconName: string) => {
  selectedIconifyIcon.value = iconName
  customIconifyIcon.value = iconName
  if (formData.value.iconType === 'iconify') {
    formData.value.logo = iconName
  }
}

// 处理自定义 Iconify 图标输入
const handleCustomIconifyChange = () => {
  selectedIconifyIcon.value = customIconifyIcon.value
  if (formData.value.iconType === 'iconify') {
    formData.value.logo = customIconifyIcon.value
  }
}

// 打开 Iconify 图标库
const openIconLibrary = () => {
  window.open('https://icon-sets.iconify.design/', '_blank')
}

// 获取网站图标
const fetchWebsiteIcon = async () => {
  if (!formData.value.url.trim()) {
    alert('请先输入网站地址')
    return
  }

  fetchingIcon.value = true
  try {
    const config = useRuntimeConfig()
    const API_BASE_URL = `${config.public.apiBaseUrl}/api`

    // 使用带token的apiRequest方法
    const response = await apiRequest(`${API_BASE_URL}/navigation-items/fetch-icon?url=${encodeURIComponent(formData.value.url)}`)
    const result = await response.json()

    if (result.success && result.data) {
      // 设置为在线图标模式
      formData.value.iconType = 'online'
      iconUrl.value = result.data
      formData.value.logo = result.data
      imageLoadError.value = false

      console.log('成功获取网站图标:', result.data)
    } else {
      console.error('获取图标失败:', result.message)
      alert(result.message || '获取图标失败，请手动设置图标')
    }
  } catch (error) {
    console.error('获取图标时发生错误:', error)
    alert('获取图标时发生错误，请检查网络连接')
  } finally {
    fetchingIcon.value = false
  }
}

// 关闭弹窗
const closeDialog = () => {
  emit('update:visible', false)
}

// 确认操作
const handleConfirm = () => {
  // 基本验证
  if (!formData.value.name.trim()) {
    alert('请输入导航项名称')
    return
  }
  if (!formData.value.url.trim()) {
    alert('请输入导航项URL')
    return
  }
  if (!formData.value.categoryId) {
    alert('请选择分类')
    return
  }

  // 根据图标类型处理数据
  if (formData.value.iconType === 'upload') {
    // 文件上传模式
    if (props.mode === 'add' && !selectedFile.value) {
      alert('请选择图标文件')
      return
    }

    // 准备FormData
    const formDataToSend = new FormData()
    if (selectedFile.value) {
      formDataToSend.append('logoFile', selectedFile.value)
    }
    formDataToSend.append('name', formData.value.name)
    formDataToSend.append('url', formData.value.url)
    formDataToSend.append('categoryId', formData.value.categoryId)
    if (formData.value.description) {
      formDataToSend.append('description', formData.value.description)
    }
    if (formData.value.internalUrl) {
      formDataToSend.append('internalUrl', formData.value.internalUrl)
    }

    console.log('文件上传模式，FormData已准备')
    emit('confirm', { formData: formDataToSend, isUpload: true })
  } else {
    // 传统模式（在线图标或 Iconify 图标）
    if (formData.value.iconType === 'online') {
      formData.value.logo = iconUrl.value
    } else if (formData.value.iconType === 'iconify') {
      formData.value.logo = selectedIconifyIcon.value
    }

    if (!formData.value.logo) {
      alert('请设置图标')
      return
    }

    console.log('传统模式，表单数据:', formData.value)
    emit('confirm', { formData: formData.value, isUpload: false })
  }

  closeDialog()
}

// 处理遮罩层点击
const handleOverlayClick = (event: MouseEvent) => {
  if (event.target === event.currentTarget) {
    closeDialog()
  }
}
</script>

<template>
  <div
    v-if="visible"
    class="dialog-overlay"
    @click="handleOverlayClick"
  >
    <div class="dialog-container" @click.stop>
      <div class="dialog-header">
        <h3 class="dialog-title">
          {{ mode === 'add' ? '新增导航项' : '编辑导航项' }}
        </h3>
        <button class="close-button" @click="closeDialog">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </button>
      </div>
      
      <div class="dialog-content">
        <!-- 效果预览区域 -->
        <div class="preview-section">
          <label class="form-label">效果预览</label>
          <div class="preview-card">
            <div class="preview-icon">
              <!-- 图片预览（上传文件、在线图标） -->
              <img
                v-if="previewIcon && typeof previewIcon === 'string' && (previewIcon.startsWith('http') || previewIcon.startsWith('/uploads/') || previewIcon.startsWith('data:')) && !imageLoadError"
                :src="previewIcon"
                :alt="formData.name"
                class="preview-icon-img"
                @load="handleImageLoad"
                @error="handleImageError"
              />
              <!-- Iconify图标预览 -->
              <Icon
                v-else-if="previewIcon && typeof previewIcon === 'string' && formData.iconType === 'iconify'"
                :icon="previewIcon"
                class="preview-icon-svg"
              />
              <!-- 图片加载失败或无图标时的占位符 -->
              <div v-else class="preview-icon-placeholder">
                <PhotoIcon v-if="formData.iconType === 'upload'" class="placeholder-icon" />
                <LinkIcon v-else-if="formData.iconType === 'online'" class="placeholder-icon" />
                <Icon v-else-if="formData.iconType === 'iconify'" icon="mdi:star" class="placeholder-icon" />
                <PhotoIcon v-else class="placeholder-icon" />
              </div>
            </div>
            <div class="preview-content">
              <div class="preview-title">{{ formData.name || '导航项标题' }}</div>
              <div class="preview-description">{{ formData.description || '导航项描述' }}</div>
            </div>
          </div>
        </div>

        <!-- 标题和描述 (共享一行) -->
        <div class="form-row">
          <div class="form-group flex-1">
            <label class="form-label">标题</label>
            <input
              v-model="formData.name"
              type="text"
              class="form-input"
              placeholder="请输入导航项标题"
            />
          </div>
          <div class="form-group flex-1">
            <label class="form-label">描述</label>
            <input
              v-model="formData.description"
              type="text"
              class="form-input"
              placeholder="请输入导航项描述"
            />
          </div>
        </div>

        <!-- 图标风格选择 -->
        <div class="form-group">
          <label class="form-label">图标风格</label>
          <div class="icon-type-tabs">
            <button
              type="button"
              :class="['tab-button', { active: formData.iconType === 'upload' }]"
              @click="formData.iconType = 'upload'"
            >
              <PhotoIcon class="tab-icon" />
              上传图片
            </button>
            <button
              type="button"
              :class="['tab-button', { active: formData.iconType === 'online' }]"
              @click="formData.iconType = 'online'"
            >
              <LinkIcon class="tab-icon" />
              在线图标
            </button>
            <button
              type="button"
              :class="['tab-button', { active: formData.iconType === 'iconify' }]"
              @click="formData.iconType = 'iconify'"
            >
              <Icon icon="mdi:star" class="tab-icon" />
              Iconify图标
            </button>
          </div>

          <!-- 上传图片 -->
          <div v-if="formData.iconType === 'upload'" class="icon-upload-area">
            <div class="upload-row">
              <input
                ref="fileInput"
                type="file"
                accept="image/*"
                class="file-input"
                @change="handleFileUpload"
              />
              <button
                type="button"
                class="upload-button"
                @click="fileInput?.click()"
              >
                <PhotoIcon class="upload-icon" />
                选择图片文件
              </button>
              <div class="server-path-input">
                <input
                  v-model="serverImagePath"
                  type="text"
                  class="form-input h-full"
                  placeholder="服务器图片路径"
                  readonly
                />
              </div>
            </div>
          </div>

          <!-- 在线图标 -->
          <div v-if="formData.iconType === 'online'" class="icon-url-input">
            <input
              v-model="iconUrl"
              type="url"
              class="form-input"
              placeholder="请输入图标URL地址，如：https://example.com/icon.png"
              @input="handleIconUrlChange"
            />
            <div v-if="iconUrl && imageLoadError" class="url-error-tip">
              <svg class="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="15" y1="9" x2="9" y2="15"></line>
                <line x1="9" y1="9" x2="15" y2="15"></line>
              </svg>
              <span>图片加载失败，请检查URL是否正确</span>
            </div>
            <div v-else-if="iconUrl && !imageLoadError && previewIcon" class="url-success-tip">
              <svg class="success-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path d="M9 12l2 2 4-4"></path>
                <circle cx="12" cy="12" r="10"></circle>
              </svg>
              <span>图片加载成功</span>
            </div>
          </div>

          <!-- Iconify 图标选择 -->
          <div v-if="formData.iconType === 'iconify'" class="iconify-section">
            <!-- 自定义图标输入 -->
            <div class="custom-icon-input">
              <input
                v-model="customIconifyIcon"
                type="text"
                class="form-input"
                placeholder="输入 Iconify 图标名称，如：mdi:home"
                @input="handleCustomIconifyChange"
              />
              <button
                type="button"
                class="icon-library-btn"
                @click="openIconLibrary"
                title="打开 Iconify 图标库"
              >
                <LinkIcon class="library-icon" />
                图标库
              </button>
            </div>

            <!-- 使用说明 -->
            <div class="usage-tip">
              <p>💡 点击"图标库"按钮访问 Iconify 官网，搜索并复制图标名称到上方输入框</p>
              <p>📝 支持格式：<code>mdi:home</code>、<code>fa:star</code>、<code>feather:heart</code> 等</p>
            </div>
          </div>
        </div>

        <!-- 地址 -->
        <div class="form-group">
          <label class="form-label">地址</label>
          <div class="url-input-group">
            <input
              v-model="formData.url"
              type="url"
              class="form-input url-input"
              placeholder="请输入外网访问地址"
            />
            <button
              type="button"
              class="fetch-icon-btn"
              @click="fetchWebsiteIcon"
              :disabled="fetchingIcon || !formData.url.trim()"
              title="自动获取网站图标"
            >
              <Icon v-if="fetchingIcon" icon="mdi:loading" class="spin btn-icon" />
              <Icon v-else icon="mdi:image-search" class="btn-icon" />
              {{ fetchingIcon ? '获取中...' : '获取图标' }}
            </button>
          </div>
        </div>

        <!-- 内网地址 -->
        <div class="form-group">
          <label class="form-label">内网地址</label>
          <input
            v-model="formData.internalUrl"
            type="url"
            class="form-input"
            placeholder="请输入内网访问地址（可选）"
          />
        </div>
      </div>

      <div class="dialog-actions">
        <button class="btn-cancel" @click="closeDialog">
          取消
        </button>
        <button class="btn-confirm" @click="handleConfirm">
          {{ mode === 'add' ? '新增' : '保存' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: overlayFadeIn 0.3s ease;
}

.dialog-container {
  min-width: 600px;
  max-width: 800px;
  width: 90vw;
  max-height: 90vh;
  border-radius: 1rem;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  /* 液态玻璃效果 */
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 50%,
    rgba(255, 255, 255, 0.05) 100%
  );
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.2);

  /* 阴影效果 */
  box-shadow:
    0 20px 40px rgba(0, 0, 0, 0.3),
    0 8px 16px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.3),
    inset 0 -1px 0 rgba(0, 0, 0, 0.1);

  animation: dialogSlideIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem 1.5rem 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
}

.dialog-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.close-button {
  width: 2rem;
  height: 2rem;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.close-button:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
}

.close-button svg {
  width: 1.25rem;
  height: 1.25rem;
  stroke-width: 2;
}

.dialog-content {
  padding: 1.5rem;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 0.5rem;
}

.form-input {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.75rem;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.875rem;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.form-input:focus {
  outline: none;
  border-color: rgba(59, 130, 246, 0.5);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 100%
  );
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

/* 新增样式 */
.preview-section {
  margin-bottom: 2rem;
}

.preview-card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  padding: 1rem;
  border-radius: 0.75rem;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  border: 1px solid rgba(255, 255, 255, 0.1);
  max-width: 300px;
  margin: 0 auto;
}

.preview-icon {
  width: 3rem;
  height: 3rem;
  border-radius: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.preview-icon-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 0.75rem;
}

.preview-icon-svg {
  width: 2rem;
  height: 2rem;
  color: rgba(255, 255, 255, 0.8);
}

.preview-icon-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 0.75rem;
}

.placeholder-icon {
  width: 1.5rem;
  height: 1.5rem;
  color: rgba(255, 255, 255, 0.4);
}

.preview-content {
  flex: 1;
  min-width: 0;
  text-align: left;
}

.preview-title {
  font-size: 1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 0.25rem;
}

.preview-description {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.6);
}

.form-row {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.flex-1 {
  flex: 1;
}

.form-row .form-group {
  margin-bottom: 0;
}

.icon-type-tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.tab-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.05) 0%,
    rgba(255, 255, 255, 0.02) 100%
  );
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tab-button:hover {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  color: rgba(255, 255, 255, 0.9);
}

.tab-button.active {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.3) 0%,
    rgba(59, 130, 246, 0.2) 100%
  );
  border-color: rgba(59, 130, 246, 0.5);
  color: rgba(255, 255, 255, 0.9);
}

.tab-icon {
  width: 1rem;
  height: 1rem;
}

.icon-upload-area {
  margin-top: 0.5rem;
}

.upload-row {
  display: flex;
  gap: 1rem;
  align-items: stretch;
}

.server-path-input {
  flex: 1;
  min-width: 200px;
}

.server-path-input .form-input[readonly] {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.05) 0%,
    rgba(255, 255, 255, 0.02) 100%
  );
  color: rgba(255, 255, 255, 0.7);
  cursor: default;
  border-color: rgba(255, 255, 255, 0.15);
}

.file-input {
  display: none;
}

.upload-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  min-width: 160px;
  padding: 1rem 1.5rem;
  border: 2px dashed rgba(255, 255, 255, 0.3);
  border-radius: 0.75rem;
  background: transparent;
  color: rgba(255, 255, 255, 0.6);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.upload-button:hover {
  border-color: rgba(59, 130, 246, 0.5);
  background: rgba(59, 130, 246, 0.1);
  color: rgba(255, 255, 255, 0.8);
}

.upload-icon {
  width: 1.5rem;
  height: 1.5rem;
}

.icon-url-input {
  margin-top: 0.5rem;
}

.url-error-tip,
.url-success-tip {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
  padding: 0.5rem 0.75rem;
  border-radius: 0.5rem;
  font-size: 0.875rem;
}

.url-error-tip {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: rgba(239, 68, 68, 0.9);
}

.url-success-tip {
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.2);
  color: rgba(34, 197, 94, 0.9);
}

.error-icon,
.success-icon {
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
}

.hero-icons-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.hero-icon-button {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.05) 0%,
    rgba(255, 255, 255, 0.02) 100%
  );
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.3s ease;
}

.hero-icon-button:hover {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  color: rgba(255, 255, 255, 0.9);
}

.hero-icon-button.selected {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.3) 0%,
    rgba(59, 130, 246, 0.2) 100%
  );
  border-color: rgba(59, 130, 246, 0.5);
  color: rgba(255, 255, 255, 0.9);
}

.hero-icon {
  width: 1.5rem;
  height: 1.5rem;
}

.hero-icon-label {
  font-size: 0.75rem;
  text-align: center;
}

/* Iconify 图标样式 */
.iconify-section {
  margin-top: 0.5rem;
}

.custom-icon-input {
  display: flex;
  gap: 1rem;
  align-items: stretch;
  margin-bottom: 1rem;
}



.icon-library-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  min-width: 120px;
  padding: 0.75rem 1rem;
  border: 1px solid rgba(59, 130, 246, 0.3);
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.15) 0%,
    rgba(59, 130, 246, 0.08) 100%
  );
  color: rgba(59, 130, 246, 0.9);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
  white-space: nowrap;
  flex-shrink: 0;
}

.icon-library-btn:hover {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.25) 0%,
    rgba(59, 130, 246, 0.15) 100%
  );
  border-color: rgba(59, 130, 246, 0.5);
  color: rgba(59, 130, 246, 1);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

.library-icon {
  width: 1rem;
  height: 1rem;
  stroke-width: 2;
}

.usage-tip {
  margin-top: 1rem;
  padding: 1rem;
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.1) 0%,
    rgba(34, 197, 94, 0.05) 100%
  );
  border: 1px solid rgba(34, 197, 94, 0.2);
}

.usage-tip p {
  margin: 0 0 0.5rem 0;
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.4;
}

.usage-tip p:last-child {
  margin-bottom: 0;
}

.usage-tip code {
  background: rgba(255, 255, 255, 0.1);
  padding: 0.125rem 0.375rem;
  border-radius: 0.25rem;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.8rem;
  color: rgba(34, 197, 94, 0.9);
}

/* URL输入组样式 */
.url-input-group {
  display: flex;
  gap: 0.75rem;
  align-items: stretch;
}

.url-input {
  flex: 1;
}

.fetch-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  min-width: 120px;
  padding: 0.75rem 1rem;
  border: 1px solid rgba(34, 197, 94, 0.3);
  border-radius: 0.75rem;
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.15) 0%,
    rgba(34, 197, 94, 0.08) 100%
  );
  color: rgba(34, 197, 94, 0.9);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
  white-space: nowrap;
  flex-shrink: 0;
}

.fetch-icon-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.25) 0%,
    rgba(34, 197, 94, 0.15) 100%
  );
  border-color: rgba(34, 197, 94, 0.5);
  color: rgba(34, 197, 94, 1);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(34, 197, 94, 0.2);
}

.fetch-icon-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.btn-icon {
  width: 1rem;
  height: 1rem;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}



.dialog-actions {
  display: flex;
  gap: 0.75rem;
  padding: 0 1.5rem 1.5rem;
  justify-content: flex-end;
  flex-shrink: 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
}

.btn-cancel,
.btn-confirm {
  padding: 0.75rem 1.5rem;
  border-radius: 0.75rem;
  font-weight: 600;
  font-size: 0.875rem;
  border: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.btn-cancel {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  color: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.btn-cancel:hover {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 100%
  );
  color: rgba(255, 255, 255, 0.9);
  transform: translateY(-1px);
}

.btn-confirm {
  background: linear-gradient(135deg,
    #3b82f6 0%,
    #2563eb 100%
  );
  color: white;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.btn-confirm:hover {
  background: linear-gradient(135deg,
    #2563eb 0%,
    #1d4ed8 100%
  );
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

/* 动画 */
@keyframes overlayFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes dialogSlideIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(-20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .dialog-container {
    min-width: 320px;
    max-width: 95vw;
    margin: 1rem;
  }

  .form-row {
    flex-direction: column;
    gap: 0;
  }

  .form-row .form-group {
    margin-bottom: 1.5rem;
  }

  .icon-type-tabs {
    flex-direction: column;
  }

  .hero-icons-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .preview-card {
    flex-direction: column;
    text-align: center;
    max-width: none;
  }

  .preview-content {
    text-align: center;
    width: 100%;
  }

  .url-input-group {
    flex-direction: column;
    gap: 0.75rem;
  }

  .fetch-icon-btn {
    width: 100%;
    justify-content: center;
    min-width: auto;
  }

  .dialog-actions {
    flex-direction: column-reverse;
  }

  .btn-cancel,
  .btn-confirm {
    width: 100%;
    justify-content: center;
  }
}
</style>
