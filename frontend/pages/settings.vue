<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { VueDraggable } from 'vue-draggable-plus'
import { Icon } from '@iconify/vue'

// 应用认证中间件
definePageMeta({
  middleware: 'auth',
  layout: 'dashboard'
})

// 类型定义
interface Category {
  id: number
  name: string
  order: number
  createdAt: string
  updatedAt: string
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 响应式数据
const categories = ref<Category[]>([])
const loading = ref(false)
const saving = ref(false)

// 新增分组相关
const showAddCategoryForm = ref(false)
const addCategoryForm = ref({
  name: ''
})
const addCategoryLoading = ref(false)

// 编辑分组相关
const editingCategoryId = ref<number | null>(null)
const editCategoryForm = ref({
  name: ''
})
const editCategoryLoading = ref(false)

// 删除分组相关
const showDeleteConfirm = ref(false)
const deletingCategory = ref<Category | null>(null)
const deleteCategoryLoading = ref(false)

// 所有设置项的折叠状态
const isGroupManagementCollapsed = ref(true)
const isPasswordSettingsCollapsed = ref(true)
const isSystemConfigCollapsed = ref(true)
const isThemeSettingsCollapsed = ref(true)
const isBackupRestoreCollapsed = ref(true)
const isSystemInfoCollapsed = ref(true)

// 密码设置相关
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordLoading = ref(false)
const showPasswordForm = ref(false)

// 壁纸设置相关
const currentWallpaper = ref('')
const wallpaperBlur = ref(5)
const wallpaperMask = ref(30)
const wallpaperResetting = ref(false)

// API配置
const config = useRuntimeConfig()
const API_BASE_URL = `${config.public.apiBaseUrl}/api`

// 获取分组列表
const fetchCategories = async () => {
  loading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/categories`)
    const result: ApiResponse<Category[]> = await response.json()

    if (result.success) {
      categories.value = result.data
    } else {
      console.error('获取分组失败:', result.message)
    }
  } catch (error) {
    console.error('获取分组失败:', error)
  } finally {
    loading.value = false
  }
}

// 保存分组排序
const saveCategoriesSort = async () => {
  saving.value = true
  try {
    // 更新排序号
    const sortedCategories = categories.value.map((category, index) => ({
      ...category,
      order: index + 1
    }))

    const response = await apiRequest(`${API_BASE_URL}/categories/sort`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(sortedCategories)
    })

    const result: ApiResponse<string> = await response.json()

    if (result.success) {
      categories.value = sortedCategories
      console.log('分组排序保存成功')
    } else {
      console.error('保存排序失败:', result.message)
      // 重新获取数据以恢复原始顺序
      await fetchCategories()
    }
  } catch (error) {
    console.error('保存排序失败:', error)
    // 重新获取数据以恢复原始顺序
    await fetchCategories()
  } finally {
    saving.value = false
  }
}

// 处理拖拽结束
const handleDragEnd = () => {
  console.log('拖拽排序完成')
  saveCategoriesSort()
}

// 创建新分组
const createCategory = async () => {
  if (!addCategoryForm.value.name.trim()) {
    console.error('分组名称不能为空')
    return
  }

  addCategoryLoading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/categories`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: addCategoryForm.value.name.trim()
      })
    })

    const result: ApiResponse<Category> = await response.json()

    if (result.success) {
      console.log('分组创建成功')
      // 重置表单
      addCategoryForm.value.name = ''
      showAddCategoryForm.value = false
      // 重新获取分组列表
      await fetchCategories()
    } else {
      console.error('分组创建失败:', result.message)
    }
  } catch (error) {
    console.error('分组创建失败:', error)
  } finally {
    addCategoryLoading.value = false
  }
}

// 取消新增分组
const cancelAddCategory = () => {
  addCategoryForm.value.name = ''
  showAddCategoryForm.value = false
}

// 开始编辑分组
const startEditCategory = (category: Category) => {
  editingCategoryId.value = category.id
  editCategoryForm.value.name = category.name
}

// 保存编辑分组
const saveEditCategory = async () => {
  if (!editCategoryForm.value.name.trim()) {
    console.error('分组名称不能为空')
    return
  }

  if (editingCategoryId.value === null) {
    return
  }

  editCategoryLoading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/categories/${editingCategoryId.value}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: editCategoryForm.value.name.trim()
      })
    })

    const result: ApiResponse<Category> = await response.json()

    if (result.success) {
      console.log('分组更新成功')
      // 取消编辑状态
      cancelEditCategory()
      // 重新获取分组列表
      await fetchCategories()
    } else {
      console.error('分组更新失败:', result.message)
    }
  } catch (error) {
    console.error('分组更新失败:', error)
  } finally {
    editCategoryLoading.value = false
  }
}

// 取消编辑分组
const cancelEditCategory = () => {
  editingCategoryId.value = null
  editCategoryForm.value.name = ''
}

// 显示删除确认对话框
const showDeleteCategoryConfirm = (category: Category) => {
  deletingCategory.value = category
  showDeleteConfirm.value = true
}

// 确认删除分组
const confirmDeleteCategory = async () => {
  if (!deletingCategory.value) {
    return
  }

  deleteCategoryLoading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/categories/${deletingCategory.value.id}`, {
      method: 'DELETE'
    })

    const result: ApiResponse<string> = await response.json()

    if (result.success) {
      console.log('分组删除成功')
      // 关闭确认对话框
      cancelDeleteCategory()
      // 重新获取分组列表
      await fetchCategories()
    } else {
      console.error('分组删除失败:', result.message)
    }
  } catch (error) {
    console.error('分组删除失败:', error)
  } finally {
    deleteCategoryLoading.value = false
  }
}

// 取消删除分组
const cancelDeleteCategory = () => {
  deletingCategory.value = null
  showDeleteConfirm.value = false
}

// 修改密码
const changePassword = async () => {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    console.error('两次输入的密码不一致')
    return
  }

  if (passwordForm.value.newPassword.length < 4) {
    console.error('密码长度不能少于4位')
    return
  }

  passwordLoading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/auth/change-password`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        currentPassword: passwordForm.value.currentPassword,
        newPassword: passwordForm.value.newPassword
      })
    })

    const result: ApiResponse<string> = await response.json()

    if (result.success) {
      console.log('密码修改成功')
      // 重置表单
      passwordForm.value = {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      }
      showPasswordForm.value = false
    } else {
      console.error('密码修改失败:', result.message)
    }
  } catch (error) {
    console.error('密码修改失败:', error)
  } finally {
    passwordLoading.value = false
  }
}

// 取消密码修改
const cancelPasswordChange = () => {
  passwordForm.value = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
  showPasswordForm.value = false
}

// 全部展开/收起功能
const toggleAllSections = () => {
  const allCollapsed = isGroupManagementCollapsed.value &&
                      isPasswordSettingsCollapsed.value &&
                      isSystemConfigCollapsed.value &&
                      isThemeSettingsCollapsed.value &&
                      isBackupRestoreCollapsed.value &&
                      isSystemInfoCollapsed.value

  const newState = !allCollapsed

  isGroupManagementCollapsed.value = newState
  isPasswordSettingsCollapsed.value = newState
  isSystemConfigCollapsed.value = newState
  isThemeSettingsCollapsed.value = newState
  isBackupRestoreCollapsed.value = newState
  isSystemInfoCollapsed.value = newState
}

// 计算是否全部收起
const allSectionsCollapsed = computed(() => {
  return isGroupManagementCollapsed.value &&
         isPasswordSettingsCollapsed.value &&
         isSystemConfigCollapsed.value &&
         isThemeSettingsCollapsed.value &&
         isBackupRestoreCollapsed.value &&
         isSystemInfoCollapsed.value
})

// 壁纸相关方法（保留预览功能）



const previewWallpaper = () => {
  let wallpaperUrl = currentWallpaper.value || '/background/机甲.png'

  // 处理相对路径URL
  if (wallpaperUrl && !wallpaperUrl.startsWith('http') && !wallpaperUrl.startsWith('data:') && !wallpaperUrl.startsWith('/background/')) {
    const config = useRuntimeConfig()
    wallpaperUrl = `${config.public.apiBaseUrl}${wallpaperUrl}`
  }

  // 创建全屏预览容器
  const previewContainer = document.createElement('div')
  previewContainer.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    z-index: 9999;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.8);
  `

  // 创建背景图片元素
  const backgroundDiv = document.createElement('div')
  backgroundDiv.style.cssText = `
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-image: url(${wallpaperUrl});
    background-size: cover;
    background-position: center;
    background-repeat: no-repeat;
    filter: blur(${wallpaperBlur.value}px);
    transform: scale(1.1);
  `

  // 创建遮罩层
  const maskDiv = document.createElement('div')
  maskDiv.style.cssText = `
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, ${wallpaperMask.value / 100});
    z-index: 1;
  `

  // 创建提示文字
  const hintText = document.createElement('div')
  hintText.style.cssText = `
    position: relative;
    z-index: 10;
    color: white;
    font-size: 1.5rem;
    font-weight: 600;
    text-align: center;
    background: rgba(0, 0, 0, 0.5);
    padding: 1rem 2rem;
    border-radius: 0.5rem;
    backdrop-filter: blur(10px);
  `
  hintText.textContent = '点击任意位置关闭预览'

  previewContainer.appendChild(backgroundDiv)
  previewContainer.appendChild(maskDiv)
  previewContainer.appendChild(hintText)

  previewContainer.addEventListener('click', () => {
    document.body.removeChild(previewContainer)
  })

  document.body.appendChild(previewContainer)

  // 3秒后自动关闭
  setTimeout(() => {
    if (document.body.contains(previewContainer)) {
      document.body.removeChild(previewContainer)
    }
  }, 3000)
}



const resetWallpaper = async () => {
  wallpaperResetting.value = true
  try {
    // 调用后端API重置壁纸
    const response = await apiRequest(`${API_BASE_URL}/system-config/wallpaper/reset`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      }
    })

    const result = await response.json()

    if (result.success) {
      // 更新本地状态
      currentWallpaper.value = ''
      wallpaperBlur.value = 5
      wallpaperMask.value = 30

      // 清除localStorage
      localStorage.removeItem('customWallpaper')
      localStorage.removeItem('wallpaperBlur')
      localStorage.removeItem('wallpaperMask')

      // 触发布局更新（重置壁纸，保存到数据库）
      if (process.client) {
        window.dispatchEvent(new CustomEvent('wallpaperChanged', {
          detail: {
            wallpaperUrl: '',
            wallpaperBlur: 5,
            wallpaperMask: 30,
            isPreview: false // 标记为已保存状态
          }
        }))
      }

      console.log('壁纸已还原为默认')
    } else {
      throw new Error(result.message)
    }
  } catch (error) {
    console.error('壁纸还原失败:', error)
  } finally {
    wallpaperResetting.value = false
  }
}

// 处理壁纸变更事件
const handleWallpaperChanged = (wallpaperData: any) => {
  // 保存到localStorage作为缓存
  if (wallpaperData.wallpaperUrl) {
    localStorage.setItem('customWallpaper', wallpaperData.wallpaperUrl)
  } else {
    localStorage.removeItem('customWallpaper')
  }
  localStorage.setItem('wallpaperBlur', wallpaperData.wallpaperBlur.toString())
  localStorage.setItem('wallpaperMask', wallpaperData.wallpaperMask.toString())

  // 触发布局更新
  if (process.client) {
    window.dispatchEvent(new CustomEvent('wallpaperChanged', {
      detail: {
        wallpaperUrl: wallpaperData.wallpaperUrl,
        wallpaperBlur: wallpaperData.wallpaperBlur,
        wallpaperMask: wallpaperData.wallpaperMask,
        isPreview: false // 标记为已保存状态
      }
    }))
  }
}

const loadSavedWallpaper = async () => {
  try {
    // 首先尝试从后端加载配置
    const response = await apiRequest(`${API_BASE_URL}/system-config/wallpaper`)
    const result = await response.json()

    if (result.success) {
      // 从后端加载配置
      currentWallpaper.value = result.data.wallpaperUrl || ''
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
}

// 从localStorage加载配置的回退方法
const loadFromLocalStorage = () => {
  const savedWallpaper = localStorage.getItem('customWallpaper')
  const savedBlur = localStorage.getItem('wallpaperBlur')
  const savedMask = localStorage.getItem('wallpaperMask')

  // 加载自定义壁纸（如果有的话）
  if (savedWallpaper) {
    currentWallpaper.value = savedWallpaper
  }

  // 始终加载模糊和遮罩设置，即使没有自定义壁纸
  wallpaperBlur.value = savedBlur !== null ? parseInt(savedBlur) : 5
  wallpaperMask.value = savedMask !== null ? parseInt(savedMask) : 30
}

// 获取壁纸显示URL（处理相对路径）
const getWallpaperDisplayUrl = () => {
  let wallpaperUrl = currentWallpaper.value || '/background/机甲.png'

  // 如果是完整URL（包含http或data:）或默认背景，直接使用
  if (wallpaperUrl.startsWith('http') || wallpaperUrl.startsWith('data:') || wallpaperUrl.startsWith('/background/')) {
    return wallpaperUrl
  }

  // 如果是相对路径，需要拼接API基础URL
  const config = useRuntimeConfig()
  return `${config.public.apiBaseUrl}${wallpaperUrl}`
}

// 页面加载时获取数据
onMounted(async () => {
  fetchCategories()
  await loadSavedWallpaper()
})
</script>

<template>
  <NuxtLayout>
    <div class="settings-container">
      <!-- 页面标题 -->
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">系统设置</h1>
          <p class="page-description">管理导航分组和系统配置</p>
        </div>
        <div class="header-right">
          <button class="toggle-all-btn" @click="toggleAllSections">
            <Icon :icon="allSectionsCollapsed ? 'mdi:unfold-more-horizontal' : 'mdi:unfold-less-horizontal'" class="btn-icon" />
            {{ allSectionsCollapsed ? '全部展开' : '全部收起' }}
          </button>
        </div>
      </div>

      <!-- 设置网格布局 -->
      <div class="settings-grid">
        <!-- 分组管理 -->
        <div class="settings-item group-management-item">
          <ClientOnly>
            <div class="settings-wrapper">
              <div class="item-header" @click="isGroupManagementCollapsed = !isGroupManagementCollapsed">
                <div class="header-content">
                  <Icon icon="mdi:folder-multiple" class="header-icon" />
                  <div>
                    <h2 class="item-title">分组管理</h2>
                    <p class="item-description">拖拽调整分组显示顺序</p>
                  </div>
                </div>
                <div class="header-actions">
                  <button
                    v-if="!showAddCategoryForm && !isGroupManagementCollapsed"
                    class="add-category-btn"
                    @click.stop="showAddCategoryForm = true"
                  >
                    <Icon icon="mdi:plus" class="btn-icon" />
                    新增分组
                  </button>
                  <button
                    v-if="saving && !isGroupManagementCollapsed"
                    class="save-button saving"
                    disabled
                  >
                    <Icon icon="mdi:loading" class="spin" />
                    保存中...
                  </button>
                  <button class="collapse-btn" :class="{ collapsed: isGroupManagementCollapsed }">
                    <Icon icon="mdi:chevron-down" class="collapse-icon" />
                  </button>
                </div>
              </div>

          <div v-if="!isGroupManagementCollapsed" class="item-content">
            <!-- 新增分组表单 -->
            <div v-if="showAddCategoryForm" class="add-category-form">
              <div class="form-header">
                <h3 class="form-title">新增分组</h3>
                <p class="form-description">输入分组名称创建新的导航分组</p>
              </div>

              <div class="form-group">
                <label class="form-label">分组名称</label>
                <input
                  v-model="addCategoryForm.name"
                  type="text"
                  class="form-input"
                  placeholder="请输入分组名称"
                  @keyup.enter="createCategory"
                  :disabled="addCategoryLoading"
                />
              </div>

              <div class="form-actions">
                <button
                  class="cancel-btn"
                  @click="cancelAddCategory"
                  :disabled="addCategoryLoading"
                >
                  取消
                </button>
                <button
                  class="save-btn"
                  @click="createCategory"
                  :disabled="addCategoryLoading || !addCategoryForm.name.trim()"
                >
                  <Icon v-if="addCategoryLoading" icon="mdi:loading" class="spin btn-icon" />
                  <Icon v-else icon="mdi:check" class="btn-icon" />
                  {{ addCategoryLoading ? '创建中...' : '创建' }}
                </button>
              </div>
            </div>

            <div v-else-if="loading" class="loading-state compact">
              <Icon icon="mdi:loading" class="loading-icon spin" />
              <p>加载中...</p>
            </div>

            <div v-else-if="categories.length === 0" class="empty-state compact">
              <Icon icon="mdi:folder-off" class="empty-icon" />
              <p>暂无数据</p>
            </div>

            <VueDraggable
              v-else
              v-model="categories"
              class="categories-list"
              :animation="200"
              ghost-class="ghost-item"
              chosen-class="chosen-item"
              drag-class="drag-item"
              @end="handleDragEnd"
            >
              <div
                v-for="(category, index) in categories"
                :key="category.id"
                class="category-item"
              >
                <div class="drag-handle">
                  <Icon icon="mdi:drag-vertical" class="drag-icon" />
                </div>

                <div class="category-info">
                  <!-- 编辑状态 -->
                  <div v-if="editingCategoryId === category.id" class="edit-form">
                    <input
                      v-model="editCategoryForm.name"
                      type="text"
                      class="edit-input"
                      placeholder="请输入分组名称"
                      @keyup.enter="saveEditCategory"
                      @keyup.esc="cancelEditCategory"
                      :disabled="editCategoryLoading"
                    />
                    <div class="edit-actions">
                      <button
                        class="edit-save-btn"
                        @click="saveEditCategory"
                        :disabled="editCategoryLoading || !editCategoryForm.name.trim()"
                      >
                        <Icon v-if="editCategoryLoading" icon="mdi:loading" class="spin" />
                        <Icon v-else icon="mdi:check" />
                      </button>
                      <button
                        class="edit-cancel-btn"
                        @click="cancelEditCategory"
                        :disabled="editCategoryLoading"
                      >
                        <Icon icon="mdi:close" />
                      </button>
                    </div>
                  </div>

                  <!-- 显示状态 -->
                  <div v-else>
                    <div class="category-name">{{ category.name }}</div>
                    <div class="category-meta">
                      #{{ index + 1 }}
                    </div>
                  </div>
                </div>

                <div class="category-actions">
                  <span class="order-badge">{{ index + 1 }}</span>

                  <!-- 操作按钮 -->
                  <div v-if="editingCategoryId !== category.id" class="action-buttons">
                    <button
                      class="edit-btn"
                      @click="startEditCategory(category)"
                      title="编辑分组"
                    >
                      <Icon icon="mdi:pencil" />
                    </button>
                    <button
                      class="delete-btn"
                      @click="showDeleteCategoryConfirm(category)"
                      title="删除分组"
                    >
                      <Icon icon="mdi:delete" />
                    </button>
                  </div>
                </div>
              </div>
            </VueDraggable>
              </div>

              <BorderBeam
                  v-if="!isGroupManagementCollapsed"
                  :size="200"
                  :duration="15"
                  :delay="0"
                  :border-width="1.5"
                  color-from="#34d399"
                  color-to="#10b981"
              />
            </div>
          </ClientOnly>
        </div>

        <!-- 密码设置 -->
        <div class="settings-item password-settings-item">
          <ClientOnly>
            <div class="password-settings-wrapper">
              <div class="item-header" @click="isPasswordSettingsCollapsed = !isPasswordSettingsCollapsed">
                <div class="header-content">
                  <Icon icon="mdi:lock" class="header-icon" />
                  <div>
                    <h2 class="item-title">登录密码</h2>
                    <p class="item-description">设置面板登录密码</p>
                  </div>
                </div>
                <div class="header-actions">
                  <button
                      v-if="!showPasswordForm && !isPasswordSettingsCollapsed"
                      class="change-password-btn"
                      @click.stop="showPasswordForm = true"
                  >
                    <Icon icon="mdi:pencil" class="btn-icon" />
                    修改密码
                  </button>
                  <button class="collapse-btn" :class="{ collapsed: isPasswordSettingsCollapsed }">
                    <Icon icon="mdi:chevron-down" class="collapse-icon" />
                  </button>
                </div>
              </div>

              <div v-if="!isPasswordSettingsCollapsed" class="item-content">
                <div v-if="!showPasswordForm" class="password-info">
                  <div class="info-item">
                    <Icon icon="mdi:information" class="info-icon" />
                    <div class="info-content">
                      <p class="info-title">当前状态</p>
                      <p class="info-description">密码保护已启用，默认密码为 "admin"</p>
                    </div>
                  </div>
                </div>

                <div v-else class="password-form">
                  <div class="form-group">
                    <label class="form-label">当前密码</label>
                    <input
                        v-model="passwordForm.currentPassword"
                        type="password"
                        class="form-input"
                        placeholder="输入当前密码"
                    />
                  </div>

                  <div class="form-group">
                    <label class="form-label">新密码</label>
                    <input
                        v-model="passwordForm.newPassword"
                        type="password"
                        class="form-input"
                        placeholder="输入新密码（至少4位）"
                    />
                  </div>

                  <div class="form-group">
                    <label class="form-label">确认新密码</label>
                    <input
                        v-model="passwordForm.confirmPassword"
                        type="password"
                        class="form-input"
                        placeholder="再次输入新密码"
                    />
                  </div>

                  <div class="form-actions">
                    <button
                        class="cancel-btn"
                        @click="cancelPasswordChange"
                        :disabled="passwordLoading"
                    >
                      取消
                    </button>
                    <button
                        class="save-btn"
                        @click="changePassword"
                        :disabled="passwordLoading || !passwordForm.newPassword || passwordForm.newPassword !== passwordForm.confirmPassword"
                    >
                      <Icon v-if="passwordLoading" icon="mdi:loading" class="spin btn-icon" />
                      <Icon v-else icon="mdi:check" class="btn-icon" />
                      {{ passwordLoading ? '保存中...' : '保存' }}
                    </button>
                  </div>
                </div>
              </div>

              <BorderBeam
                  v-if="!isPasswordSettingsCollapsed"
                  :size="250"
                  :duration="12"
                  :delay="9"
                  :border-width="2"
              />
            </div>
          </ClientOnly>
        </div>



        <!-- 系统配置 -->
        <div class="settings-item system-config-item">
          <ClientOnly>
            <div class="settings-wrapper">
              <div class="item-header" @click="isSystemConfigCollapsed = !isSystemConfigCollapsed">
                <div class="header-content">
                  <Icon icon="mdi:cog" class="header-icon" />
                  <div>
                    <h2 class="item-title">系统配置</h2>
                    <p class="item-description">其他系统设置选项</p>
                  </div>
                </div>
                <div class="header-actions">
                  <button class="collapse-btn" :class="{ collapsed: isSystemConfigCollapsed }">
                    <Icon icon="mdi:chevron-down" class="collapse-icon" />
                  </button>
                </div>
              </div>

              <div v-if="!isSystemConfigCollapsed" class="item-content">
                <div class="coming-soon">
                  <Icon icon="mdi:wrench" class="coming-soon-icon" />
                  <p>更多设置功能即将推出...</p>
                </div>
              </div>

              <BorderBeam
                  v-if="!isSystemConfigCollapsed"
                  :size="180"
                  :duration="18"
                  :delay="3"
                  :border-width="1.5"
                  color-from="#6366f1"
                  color-to="#8b5cf6"
              />
            </div>
          </ClientOnly>
        </div>

        <!-- 主题设置 -->
        <div class="settings-item theme-settings-item">
          <ClientOnly>
            <div class="settings-wrapper">
              <div class="item-header" @click="isThemeSettingsCollapsed = !isThemeSettingsCollapsed">
                <div class="header-content">
                  <Icon icon="mdi:palette" class="header-icon" />
                  <div>
                    <h2 class="item-title">主题设置</h2>
                    <p class="item-description">自定义界面主题</p>
                  </div>
                </div>
                <div class="header-actions">
                  <button class="collapse-btn" :class="{ collapsed: isThemeSettingsCollapsed }">
                    <Icon icon="mdi:chevron-down" class="collapse-icon" />
                  </button>
                </div>
              </div>

              <div v-if="!isThemeSettingsCollapsed" class="item-content">
                <!-- 壁纸管理器 -->
                <WallpaperManager
                  v-model:currentWallpaper="currentWallpaper"
                  v-model:wallpaperBlur="wallpaperBlur"
                  v-model:wallpaperMask="wallpaperMask"
                  @wallpaperChanged="handleWallpaperChanged"
                  @previewWallpaper="previewWallpaper"
                />
              </div>

              <BorderBeam
                  v-if="!isThemeSettingsCollapsed"
                  :size="220"
                  :duration="20"
                  :delay="6"
                  :border-width="1.5"
                  color-from="#f59e0b"
                  color-to="#ef4444"
              />
            </div>
          </ClientOnly>
        </div>

        <!-- 备份恢复 -->
        <div class="settings-item backup-restore-item">
          <ClientOnly>
            <div class="settings-wrapper">
              <div class="item-header" @click="isBackupRestoreCollapsed = !isBackupRestoreCollapsed">
                <div class="header-content">
                  <Icon icon="mdi:backup-restore" class="header-icon" />
                  <div>
                    <h2 class="item-title">备份恢复</h2>
                    <p class="item-description">数据备份与恢复</p>
                  </div>
                </div>
                <div class="header-actions">
                  <button class="collapse-btn" :class="{ collapsed: isBackupRestoreCollapsed }">
                    <Icon icon="mdi:chevron-down" class="collapse-icon" />
                  </button>
                </div>
              </div>

              <div v-if="!isBackupRestoreCollapsed" class="item-content">
                <div class="coming-soon">
                  <Icon icon="mdi:database" class="coming-soon-icon" />
                  <p>备份恢复功能即将推出...</p>
                </div>
              </div>

              <BorderBeam
                  v-if="!isBackupRestoreCollapsed"
                  :size="190"
                  :duration="22"
                  :delay="9"
                  :border-width="1.5"
                  color-from="#06b6d4"
                  color-to="#0891b2"
              />
            </div>
          </ClientOnly>
        </div>

        <!-- 系统信息 -->
        <div class="settings-item system-info-item">
          <ClientOnly>
            <div class="settings-wrapper">
              <div class="item-header" @click="isSystemInfoCollapsed = !isSystemInfoCollapsed">
                <div class="header-content">
                  <Icon icon="mdi:information" class="header-icon" />
                  <div>
                    <h2 class="item-title">系统信息</h2>
                    <p class="item-description">查看系统版本信息</p>
                  </div>
                </div>
                <div class="header-actions">
                  <button class="collapse-btn" :class="{ collapsed: isSystemInfoCollapsed }">
                    <Icon icon="mdi:chevron-down" class="collapse-icon" />
                  </button>
                </div>
              </div>

              <div v-if="!isSystemInfoCollapsed" class="item-content">
                <div class="system-info">
                  <div class="info-row">
                    <span class="info-label">版本号</span>
                    <span class="info-value">v1.0.0</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">构建时间</span>
                    <span class="info-value">2024-01-01</span>
                  </div>
                  <div class="info-row">
                    <span class="info-label">技术栈</span>
                    <span class="info-value">Vue 3 + Spring Boot</span>
                  </div>
                </div>
              </div>

              <BorderBeam
                  v-if="!isSystemInfoCollapsed"
                  :size="160"
                  :duration="25"
                  :delay="12"
                  :border-width="1.5"
                  color-from="#84cc16"
                  color-to="#65a30d"
              />
            </div>
          </ClientOnly>
        </div>
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <div v-if="showDeleteConfirm" class="modal-overlay" @click="cancelDeleteCategory">
      <div class="delete-confirm-dialog" @click.stop>
        <div class="dialog-header">
          <Icon icon="mdi:alert-circle" class="warning-icon" />
          <h3 class="dialog-title">确认删除分组</h3>
        </div>

        <div class="dialog-content">
          <p class="dialog-message">
            您确定要删除分组 <strong>"{{ deletingCategory?.name }}"</strong> 吗？
          </p>
          <p class="dialog-warning">
            此操作不可撤销，该分组下的所有导航项也将被删除。
          </p>
        </div>

        <div class="dialog-actions">
          <button
            class="dialog-cancel-btn"
            @click="cancelDeleteCategory"
            :disabled="deleteCategoryLoading"
          >
            取消
          </button>
          <button
            class="dialog-confirm-btn"
            @click="confirmDeleteCategory"
            :disabled="deleteCategoryLoading"
          >
            <Icon v-if="deleteCategoryLoading" icon="mdi:loading" class="spin btn-icon" />
            <Icon v-else icon="mdi:delete" class="btn-icon" />
            {{ deleteCategoryLoading ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>
  </NuxtLayout>
</template>

<style scoped>
.settings-container {
  min-height: 100vh;
  padding: 2rem;
  max-width: 100%;
}

/* 页面标题 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 2rem;
  padding-left: 0.5rem;
  padding-right: 0.5rem;
}

.header-left {
  flex: 1;
}

.header-right {
  flex-shrink: 0;
}

.page-title {
  font-size: 1.4rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 0.5rem 0;
  text-align: left;
}

.page-description {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
  text-align: left;
}

/* 全部展开/收起按钮 */
.toggle-all-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
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
}

.toggle-all-btn:hover {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.25) 0%,
    rgba(59, 130, 246, 0.15) 100%
  );
  border-color: rgba(59, 130, 246, 0.5);
  color: rgba(59, 130, 246, 1);
  transform: translateY(-1px);
}

/* 设置网格布局 */
.settings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 1.5rem;
  align-items: start;
}

/* 设置项 */
.settings-item {
  border-radius: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  overflow: hidden;
  transition: all 0.3s ease;
  height: fit-content;
}

.settings-item:hover {
  border-color: rgba(255, 255, 255, 0.2);
}

/* 设置项特殊样式 */
.password-settings-item,
.group-management-item,
.system-config-item,
.backup-restore-item,
.system-info-item {
  position: relative;
}

/* 主题设置项 - 扩大宽度 */
.theme-settings-item {
  position: relative;
  grid-column: 1 / -1; /* 占据整行 */
}

.theme-settings-item .settings-wrapper {
  max-width: none; /* 移除最大宽度限制 */
}

.theme-settings-item .item-content {
  padding: 2rem; /* 增加内边距 */
}

.password-settings-wrapper,
.settings-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

/* 移除全宽样式，所有设置项都使用网格布局 */

.item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.5rem;
  cursor: pointer;
  transition: all 0.3s ease;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.item-header:hover {
  background: rgba(255, 255, 255, 0.02);
}

.header-content {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.header-icon {
  width: 1.5rem;
  height: 1.5rem;
  color: rgba(59, 130, 246, 0.8);
}

.item-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 0.25rem 0;
}

.item-description {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 1rem;
}

.save-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.2) 0%,
    rgba(34, 197, 94, 0.1) 100%
  );
  color: rgba(34, 197, 94, 0.9);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.save-button.saving {
  opacity: 0.7;
  cursor: not-allowed;
}

.item-content {
  padding: 0 1.5rem 1.5rem 1.5rem;
}

/* 折叠按钮样式 */
.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border: none;
  border-radius: 0.5rem;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: all 0.3s ease;
}

.collapse-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
}

.collapse-icon {
  width: 1.25rem;
  height: 1.25rem;
  transition: transform 0.3s ease;
}

.collapse-btn.collapsed .collapse-icon {
  transform: rotate(-90deg);
}

/* 加载和空状态 */
.loading-state,
.empty-state,
.coming-soon {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  text-align: center;
}

.loading-icon,
.empty-icon,
.coming-soon-icon {
  width: 3rem;
  height: 3rem;
  color: rgba(255, 255, 255, 0.4);
  margin-bottom: 1rem;
}

.loading-state p,
.empty-state p,
.coming-soon p {
  color: rgba(255, 255, 255, 0.6);
  font-size: 1rem;
  margin: 0;
}

/* 紧凑状态样式 */
.loading-state.compact,
.empty-state.compact {
  padding: 2rem 1rem;
}

.loading-state.compact .loading-icon,
.empty-state.compact .empty-icon {
  width: 2rem;
  height: 2rem;
  margin-bottom: 0.5rem;
}

.loading-state.compact p,
.empty-state.compact p {
  font-size: 0.875rem;
}

/* 分组列表 */
.categories-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border-radius: 0.5rem;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: grab;
  position: relative;
  overflow: hidden;
}

.category-item:hover {
  transform: translateY(-1px);
  border-color: rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.02);
}

.category-item:active {
  cursor: grabbing;
}

.drag-handle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: 0.375rem;
  background: rgba(168, 85, 247, 0.1);
  border: 1px solid rgba(168, 85, 247, 0.2);
  cursor: grab;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
}

.drag-handle:hover {
  background: rgba(168, 85, 247, 0.15);
  border-color: rgba(168, 85, 247, 0.3);
  transform: translateY(-1px);
}

.drag-handle:active {
  cursor: grabbing;
}

.drag-icon {
  width: 1rem;
  height: 1rem;
  color: rgba(168, 85, 247, 0.8);
}

.category-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.category-name {
  font-size: 1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
  transition: all 0.3s ease;
}

.category-meta {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.5);
  text-align: left;
}

.category-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-left: 0.5rem;
}

.edit-btn,
.delete-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 0.375rem;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 0.75rem;
}

.edit-btn {
  background: linear-gradient(135deg,
    rgba(255, 193, 7, 0.2) 0%,
    rgba(255, 193, 7, 0.1) 100%
  );
  color: rgba(255, 193, 7, 0.9);
  border: 1px solid rgba(255, 193, 7, 0.3);
}

.edit-btn:hover {
  background: linear-gradient(135deg,
    rgba(255, 193, 7, 0.3) 0%,
    rgba(255, 193, 7, 0.15) 100%
  );
  border-color: rgba(255, 193, 7, 0.5);
  transform: translateY(-1px) scale(1.05);
}

.delete-btn {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.2) 0%,
    rgba(239, 68, 68, 0.1) 100%
  );
  color: rgba(239, 68, 68, 0.9);
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.delete-btn:hover {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.3) 0%,
    rgba(239, 68, 68, 0.15) 100%
  );
  border-color: rgba(239, 68, 68, 0.5);
  transform: translateY(-1px) scale(1.05);
}

/* 编辑表单样式 */
.edit-form {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
}

.edit-input {
  flex: 1;
  padding: 0.5rem 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.875rem;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.edit-input:focus {
  outline: none;
  border-color: rgba(59, 130, 246, 0.5);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.12) 0%,
    rgba(255, 255, 255, 0.06) 100%
  );
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.edit-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.edit-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.edit-save-btn,
.edit-cancel-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: 0.5rem;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 0.875rem;
}

.edit-save-btn {
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.2) 0%,
    rgba(34, 197, 94, 0.1) 100%
  );
  color: rgba(34, 197, 94, 0.9);
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.edit-save-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.3) 0%,
    rgba(34, 197, 94, 0.15) 100%
  );
  border-color: rgba(34, 197, 94, 0.5);
  transform: translateY(-1px) scale(1.05);
}

.edit-cancel-btn {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.2) 0%,
    rgba(239, 68, 68, 0.1) 100%
  );
  color: rgba(239, 68, 68, 0.9);
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.edit-cancel-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.3) 0%,
    rgba(239, 68, 68, 0.15) 100%
  );
  border-color: rgba(239, 68, 68, 0.5);
  transform: translateY(-1px) scale(1.05);
}

.edit-save-btn:disabled,
.edit-cancel-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* 删除确认对话框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: modalFadeIn 0.3s ease;
}

.delete-confirm-dialog {
  min-width: 400px;
  max-width: 500px;
  border-radius: 1rem;
  overflow: hidden;

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
  gap: 1rem;
  padding: 1.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.warning-icon {
  width: 2rem;
  height: 2rem;
  color: rgba(255, 193, 7, 0.9);
}

.dialog-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.dialog-content {
  padding: 1.5rem;
}

.dialog-message {
  font-size: 1rem;
  color: rgba(255, 255, 255, 0.8);
  margin: 0 0 1rem 0;
  line-height: 1.5;
}

.dialog-warning {
  font-size: 0.875rem;
  color: rgba(255, 193, 7, 0.8);
  margin: 0;
  line-height: 1.4;
}

.dialog-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  padding: 1.5rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.dialog-cancel-btn,
.dialog-confirm-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.dialog-cancel-btn {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 100%
  );
  color: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.dialog-cancel-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.25) 0%,
    rgba(255, 255, 255, 0.15) 100%
  );
  border-color: rgba(255, 255, 255, 0.3);
  color: rgba(255, 255, 255, 1);
}

.dialog-confirm-btn {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.2) 0%,
    rgba(239, 68, 68, 0.1) 100%
  );
  color: rgba(239, 68, 68, 0.9);
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.dialog-confirm-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.3) 0%,
    rgba(239, 68, 68, 0.15) 100%
  );
  border-color: rgba(239, 68, 68, 0.5);
  color: rgba(239, 68, 68, 1);
}

.dialog-cancel-btn:disabled,
.dialog-confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 动画 */
@keyframes modalFadeIn {
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
    transform: translateY(-20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.order-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.2) 0%,
    rgba(59, 130, 246, 0.1) 100%
  );
  border: 1px solid rgba(59, 130, 246, 0.3);
  color: rgba(59, 130, 246, 0.9);
  font-size: 0.625rem;
  font-weight: 600;
}

/* 拖拽状态样式 */
.ghost-item {
  opacity: 0.5;
  transform: scale(0.95);
  background: linear-gradient(135deg,
    rgba(168, 85, 247, 0.2) 0%,
    rgba(168, 85, 247, 0.1) 100%
  );
  border: 2px dashed rgba(168, 85, 247, 0.5);
}

.chosen-item {
  transform: scale(1.02);
  box-shadow: 0 8px 25px rgba(168, 85, 247, 0.3);
  z-index: 1000;
}

.drag-item {
  transform: rotate(2deg) scale(1.05);
  box-shadow: 0 12px 30px rgba(168, 85, 247, 0.4);
  z-index: 1001;
}

/* 动画 */
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

/* 新增分组按钮样式 */
.add-category-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border: 1px solid rgba(34, 197, 94, 0.3);
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.15) 0%,
    rgba(34, 197, 94, 0.08) 100%
  );
  color: rgba(34, 197, 94, 0.9);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.add-category-btn:hover {
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.25) 0%,
    rgba(34, 197, 94, 0.15) 100%
  );
  border-color: rgba(34, 197, 94, 0.5);
  color: rgba(34, 197, 94, 1);
  transform: translateY(-1px);
}

/* 新增分组表单样式 */
.add-category-form {
  padding: 1.5rem;
  margin-bottom: 1.5rem;
  border-radius: 0.75rem;
  background: rgba(34, 197, 94, 0.03);
  border: 1px solid rgba(34, 197, 94, 0.15);
}

.form-header {
  margin-bottom: 1.5rem;
}

.form-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 0.5rem 0;
}

.form-description {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

/* 密码设置样式 */
.change-password-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border: 1px solid rgba(255, 193, 7, 0.3);
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(255, 193, 7, 0.15) 0%,
    rgba(255, 193, 7, 0.08) 100%
  );
  color: rgba(255, 193, 7, 0.9);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.change-password-btn:hover {
  background: linear-gradient(135deg,
    rgba(255, 193, 7, 0.25) 0%,
    rgba(255, 193, 7, 0.15) 100%
  );
  border-color: rgba(255, 193, 7, 0.5);
  color: rgba(255, 193, 7, 1);
  transform: translateY(-1px);
}

.btn-icon {
  width: 1rem;
  height: 1rem;
}

.password-info {
  padding: 1rem 0;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem;
  border-radius: 0.5rem;
  background: rgba(59, 130, 246, 0.05);
  border: 1px solid rgba(59, 130, 246, 0.15);
}

.info-icon {
  width: 1.5rem;
  height: 1.5rem;
  color: rgba(59, 130, 246, 0.8);
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.info-content {
  flex: 1;
}

.info-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 0.25rem 0;
}

.info-description {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
  line-height: 1.4;
}

.password-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.8);
}

.form-input {
  padding: 0.75rem 1rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
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
    rgba(255, 255, 255, 0.12) 0%,
    rgba(255, 255, 255, 0.06) 100%
  );
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.form-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 0.5rem;
}

.cancel-btn,
.save-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cancel-btn {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.15) 0%,
    rgba(239, 68, 68, 0.08) 100%
  );
  color: rgba(239, 68, 68, 0.9);
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.cancel-btn:hover {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.25) 0%,
    rgba(239, 68, 68, 0.15) 100%
  );
  border-color: rgba(239, 68, 68, 0.5);
  color: rgba(239, 68, 68, 1);
}

.save-btn {
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.2) 0%,
    rgba(34, 197, 94, 0.1) 100%
  );
  color: rgba(34, 197, 94, 0.9);
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.save-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.3) 0%,
    rgba(34, 197, 94, 0.15) 100%
  );
  border-color: rgba(34, 197, 94, 0.5);
  color: rgba(34, 197, 94, 1);
}

.save-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 系统信息样式 */
.system-info {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.info-label {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
}

.info-value {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
}

/* 主题设置样式已移至 WallpaperManager 组件 */



.wallpaper-upload-preview {
  margin-bottom: 1.5rem;
}

.upload-preview-container {
  width: 100%;
  height: 200px;
  border-radius: 0.75rem;
  border: 2px dashed rgba(249, 115, 22, 0.3);
  background-color: rgba(255, 255, 255, 0.02);
  position: relative;
  cursor: pointer;
  transition: all 0.3s ease;
  overflow: hidden;
}

/* 背景图片层 */
.background-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  border-radius: 0.75rem;
  /* 扩展背景以避免模糊边缘 */
  transform: scale(1.1);
  z-index: 1;
}

.upload-preview-container:hover {
  border-color: rgba(249, 115, 22, 0.5);
  transform: translateY(-2px);
}

.preview-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 0.75rem;
  pointer-events: none;
  z-index: 2;
}

/* 上传覆盖层 */
.upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg,
    rgba(0, 0, 0, 0.3) 0%,
    rgba(0, 0, 0, 0.1) 100%
  );
  border-radius: 0.75rem;
  transition: all 0.3s ease;
  z-index: 3;
}

.upload-preview-container:hover .upload-overlay {
  background: linear-gradient(135deg,
    rgba(0, 0, 0, 0.5) 0%,
    rgba(0, 0, 0, 0.2) 100%
  );
}

/* 无壁纸上传状态 */
.no-wallpaper-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  text-align: center;
  color: rgba(255, 255, 255, 0.8);
}

.upload-icon {
  width: 3rem;
  height: 3rem;
  color: rgba(249, 115, 22, 0.7);
  margin-bottom: 0.5rem;
}

.upload-text {
  font-size: 1rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

.upload-hint {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

/* 有壁纸状态 */
.has-wallpaper-upload {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.wallpaper-actions-overlay {
  display: flex;
  gap: 1rem;
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.3s ease;
}

.upload-preview-container:hover .wallpaper-actions-overlay {
  opacity: 1;
  transform: translateY(0);
}

.overlay-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border: none;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.change-btn {
  background: linear-gradient(135deg,
    rgba(249, 115, 22, 0.8) 0%,
    rgba(249, 115, 22, 0.6) 100%
  );
  color: white;
}

.change-btn:hover {
  background: linear-gradient(135deg,
    rgba(249, 115, 22, 0.9) 0%,
    rgba(249, 115, 22, 0.7) 100%
  );
  transform: translateY(-1px);
}

.overlay-btn.preview-btn {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.8) 0%,
    rgba(59, 130, 246, 0.6) 100%
  );
  color: white;
}

.overlay-btn.preview-btn:hover {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.9) 0%,
    rgba(59, 130, 246, 0.7) 100%
  );
  transform: translateY(-1px);
}

.preview-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.75rem;
  padding: 0 0.5rem;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.wallpaper-status {
  font-size: 0.8rem;
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
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 500;
}



/* 模糊度控制 */
.blur-control,
.mask-control {
  margin-bottom: 1.5rem;
}

.control-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 1rem;
}

.control-icon {
  width: 1rem;
  height: 1rem;
  color: rgba(249, 115, 22, 0.7);
}

.slider-container {
  position: relative;
}

.blur-slider,
.mask-slider {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  outline: none;
  cursor: pointer;
  -webkit-appearance: none;
  appearance: none;
}

.blur-slider {
  background: linear-gradient(to right,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(249, 115, 22, 0.3) 100%
  );
}

.mask-slider {
  background: linear-gradient(to right,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(0, 0, 0, 0.5) 100%
  );
}

.blur-slider::-webkit-slider-thumb,
.mask-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.2);
  cursor: pointer;
  transition: all 0.3s ease;
}

.blur-slider::-webkit-slider-thumb {
  background: linear-gradient(135deg,
    rgba(249, 115, 22, 0.9) 0%,
    rgba(249, 115, 22, 0.7) 100%
  );
}

.mask-slider::-webkit-slider-thumb {
  background: linear-gradient(135deg,
    rgba(0, 0, 0, 0.8) 0%,
    rgba(0, 0, 0, 0.6) 100%
  );
}

.blur-slider::-webkit-slider-thumb:hover,
.mask-slider::-webkit-slider-thumb:hover {
  transform: scale(1.1);
  border-color: rgba(255, 255, 255, 0.4);
}

.blur-slider::-moz-range-thumb,
.mask-slider::-moz-range-thumb {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.2);
  cursor: pointer;
  transition: all 0.3s ease;
}

.blur-slider::-moz-range-thumb {
  background: linear-gradient(135deg,
    rgba(249, 115, 22, 0.9) 0%,
    rgba(249, 115, 22, 0.7) 100%
  );
}

.mask-slider::-moz-range-thumb {
  background: linear-gradient(135deg,
    rgba(0, 0, 0, 0.8) 0%,
    rgba(0, 0, 0, 0.6) 100%
  );
}

.slider-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 0.5rem;
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.5);
}

/* 操作按钮 */
.wallpaper-actions {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border: none;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  flex: 1;
  min-width: 120px;
  justify-content: center;
}

.preview-btn {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.2) 0%,
    rgba(59, 130, 246, 0.1) 100%
  );
  color: rgba(59, 130, 246, 0.9);
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.preview-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.3) 0%,
    rgba(59, 130, 246, 0.15) 100%
  );
  border-color: rgba(59, 130, 246, 0.5);
  transform: translateY(-1px);
}

.apply-btn {
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.2) 0%,
    rgba(34, 197, 94, 0.1) 100%
  );
  color: rgba(34, 197, 94, 0.9);
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.apply-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.3) 0%,
    rgba(34, 197, 94, 0.15) 100%
  );
  border-color: rgba(34, 197, 94, 0.5);
  transform: translateY(-1px);
}

.reset-btn {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.2) 0%,
    rgba(239, 68, 68, 0.1) 100%
  );
  color: rgba(239, 68, 68, 0.9);
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.reset-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.3) 0%,
    rgba(239, 68, 68, 0.15) 100%
  );
  border-color: rgba(239, 68, 68, 0.5);
  transform: translateY(-1px);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .settings-grid {
    grid-template-columns: 1fr;
    gap: 1rem;
  }
}

@media (max-width: 768px) {
  .settings-container {
    padding: 1rem;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }

  .header-right {
    width: 100%;
  }

  .toggle-all-btn {
    width: 100%;
    justify-content: center;
  }

  .page-title {
    font-size: 1.5rem;
  }

  .settings-grid {
    grid-template-columns: 1fr;
    gap: 1rem;
  }

  .item-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }

  .header-content {
    width: 100%;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .category-item {
    padding: 0.75rem;
    gap: 0.75rem;
  }

  .category-name {
    font-size: 0.875rem;
  }

  .category-meta {
    font-size: 0.7rem;
  }

  .form-actions {
    flex-direction: column;
  }

  .cancel-btn,
  .save-btn {
    width: 100%;
    justify-content: center;
  }

  /* 移动端编辑和删除按钮优化 */
  .edit-btn,
  .delete-btn {
    width: 2.5rem;
    height: 2.5rem;
    font-size: 1rem;
  }

  .action-buttons {
    gap: 0.75rem;
  }

  /* 移动端编辑表单优化 */
  .edit-form {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }

  .edit-actions {
    justify-content: center;
    gap: 1rem;
  }

  .edit-save-btn,
  .edit-cancel-btn {
    width: 3rem;
    height: 3rem;
    font-size: 1rem;
  }

  /* 移动端对话框优化 */
  .delete-confirm-dialog {
    min-width: 320px;
    max-width: 90vw;
    margin: 1rem;
  }

  .dialog-actions {
    flex-direction: column;
    gap: 0.75rem;
  }

  .dialog-cancel-btn,
  .dialog-confirm-btn {
    width: 100%;
    justify-content: center;
    padding: 1rem;
  }

  /* 移动端壁纸设置优化 */
  .wallpaper-actions {
    flex-direction: column;
  }

  .action-btn {
    width: 100%;
    min-width: auto;
  }

  .upload-preview-container {
    height: 150px;
  }

  .upload-icon {
    width: 2.5rem;
    height: 2.5rem;
  }

  .wallpaper-actions-overlay {
    flex-direction: column;
    gap: 0.5rem;
  }

  .overlay-btn {
    padding: 0.5rem 1rem;
    font-size: 0.8rem;
  }
}
</style>