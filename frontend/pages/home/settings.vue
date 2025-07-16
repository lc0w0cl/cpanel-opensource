<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { VueDraggable } from 'vue-draggable-plus'
import { Icon } from '@iconify/vue'
import './settings.css'
// 子页面不需要定义 layout 和 middleware，由父页面处理

// 类型定义
interface Category {
  id: number
  name: string
  type: string
  order: number
  createdAt: string
  updatedAt: string
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

// 导航分组相关数据
const navigationCategories = ref<Category[]>([])
const navigationLoading = ref(false)
const navigationSaving = ref(false)

// 服务器分组相关数据
const serverCategories = ref<Category[]>([])
const serverLoading = ref(false)
const serverSaving = ref(false)

// 新增导航分组相关
const showAddNavigationCategoryForm = ref(false)
const addNavigationCategoryForm = ref({
  name: ''
})
const addNavigationCategoryLoading = ref(false)

// 编辑导航分组相关
const editingNavigationCategoryId = ref<number | null>(null)
const editNavigationCategoryForm = ref({
  name: ''
})
const editNavigationCategoryLoading = ref(false)

// 删除导航分组相关
const showDeleteNavigationConfirm = ref(false)
const deletingNavigationCategory = ref<Category | null>(null)
const deleteNavigationCategoryLoading = ref(false)

// 新增服务器分组相关
const showAddServerCategoryForm = ref(false)
const addServerCategoryForm = ref({
  name: ''
})
const addServerCategoryLoading = ref(false)

// 编辑服务器分组相关
const editingServerCategoryId = ref<number | null>(null)
const editServerCategoryForm = ref({
  name: ''
})
const editServerCategoryLoading = ref(false)

// 删除服务器分组相关
const showDeleteServerCategoryConfirm = ref(false)
const deletingServerCategory = ref<Category | null>(null)
const deleteServerCategoryLoading = ref(false)

// 所有设置项的折叠状态
const isNavigationGroupManagementCollapsed = ref(true)
const isServerGroupManagementCollapsed = ref(true)
const isPasswordSettingsCollapsed = ref(true)
const isSystemConfigCollapsed = ref(true)
const isThemeSettingsCollapsed = ref(true)
const isMusicSettingsCollapsed = ref(true)
const isServerSettingsCollapsed = ref(true)
const isBackupRestoreCollapsed = ref(true)
const isSystemInfoCollapsed = ref(true)

// Logo设置相关
const currentLogo = ref('')
const logoUploading = ref(false)
const logoResetting = ref(false)

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

// 音乐设置相关
const musicDownloadLocation = ref('local') // 'local' 或 'server'
const musicServerDownloadPath = ref('uploads/music')
const musicSettingsLoading = ref(false)
const musicSettingsSaving = ref(false)

// Cookie设置相关
const bilibiliCookie = ref('')
const youtubeCookie = ref('')
const cookieSettingsLoading = ref(false)
const cookieSettingsSaving = ref(false)

// 服务器设置相关
const serverConfigs = ref([])
const serverSettingsLoading = ref(false)
const serverSettingsSaving = ref(false)
const showAddServerForm = ref(false)
const showEditServerForm = ref(false)
const editingServerId = ref(null)
const showDeleteServerConfirm = ref(false)
const deletingServer = ref(null)

// 分组相关
const groupedServers = computed(() => {
  const grouped = {}
  serverConfigs.value.forEach(server => {
    // 优先使用groupName，如果没有则通过categoryId查找分组名称
    let groupName = server.groupName
    if (!groupName && server.categoryId) {
      const category = serverCategories.value.find(cat => cat.id === server.categoryId)
      groupName = category ? category.name : '默认分组'
    }
    if (!groupName) {
      groupName = '默认分组'
    }

    if (!grouped[groupName]) {
      grouped[groupName] = []
    }
    grouped[groupName].push(server)
  })

  // 按组内的sortOrder排序
  Object.keys(grouped).forEach(groupName => {
    grouped[groupName].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  })

  return grouped
})

const groupNames = computed(() => {
  const serverGroupNames = Object.keys(groupedServers.value)

  // 按照服务器分组管理中的顺序排序
  const orderedGroups = []

  // 首先添加在服务器分组管理中定义的分组（按order排序）
  const managedGroups = serverCategories.value
    .sort((a, b) => (a.order || 0) - (b.order || 0))
    .map(category => category.name)
    .filter(name => serverGroupNames.includes(name))

  orderedGroups.push(...managedGroups)

  // 然后添加默认分组（如果存在）
  if (serverGroupNames.includes('默认分组') && !orderedGroups.includes('默认分组')) {
    orderedGroups.push('默认分组')
  }

  // 最后添加其他未在分组管理中定义的分组（按字母排序）
  const otherGroups = serverGroupNames
    .filter(name => !orderedGroups.includes(name))
    .sort()

  orderedGroups.push(...otherGroups)

  return orderedGroups
})

// 可用的服务器分组选项（从服务器分组管理中获取）
const availableServerGroups = computed(() => {
  const groups = serverCategories.value.map(category => category.name)
  // 添加默认分组
  if (!groups.includes('默认分组')) {
    groups.unshift('默认分组')
  }
  return groups
})
const deleteServerLoading = ref(false)

// 私钥管理相关
const privateKeys = ref([])
const privateKeysLoading = ref(false)
const showPrivateKeyManager = ref(false)
const showAddPrivateKeyForm = ref(false)
const showEditPrivateKeyForm = ref(false)
const editingPrivateKeyId = ref(null)
const privateKeyForm = ref({
  keyName: '',
  privateKey: '',
  privateKeyPassword: '',
  description: '',
  keyType: ''
})
const privateKeySaving = ref(false)
const showDeletePrivateKeyConfirm = ref(false)
const deletingPrivateKey = ref(null)
const deletePrivateKeyLoading = ref(false)
const showPrivateKeyDropdown = ref(false)
const showKeyTypeDropdown = ref(false)
const showServerGroupDropdown = ref(false)

// 服务器表单数据
const serverForm = ref({
  serverName: '',
  host: '',
  port: 22,
  username: '',
  authType: 'password', // 'password' 或 'publickey'
  password: '',
  privateKey: '',
  privateKeyPassword: '',
  selectedPrivateKeyId: '', // 选择的已保存私钥ID
  useExistingKey: false, // 是否使用已保存的私钥
  description: '',
  groupName: '', // 服务器分组
  isDefault: false
})

// 重置服务器表单
const resetServerForm = () => {
  serverForm.value = {
    serverName: '',
    host: '',
    port: 22,
    username: '',
    authType: 'password',
    password: '',
    privateKey: '',
    privateKeyPassword: '',
    selectedPrivateKeyId: '',
    useExistingKey: false,
    description: '',
    groupName: '',
    isDefault: false
  }
}

// 重置私钥表单
const resetPrivateKeyForm = () => {
  privateKeyForm.value = {
    keyName: '',
    privateKey: '',
    privateKeyPassword: '',
    description: '',
    keyType: ''
  }
}

// API配置
const config = useRuntimeConfig()
const API_BASE_URL = `${config.public.apiBaseUrl}/api`

// 获取导航分组列表
const fetchNavigationCategories = async () => {
  navigationLoading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/categories/type/navigation`)
    const result: ApiResponse<Category[]> = await response.json()

    if (result.success) {
      navigationCategories.value = result.data
    } else {
      console.error('获取导航分组失败:', result.message)
    }
  } catch (error) {
    console.error('获取导航分组失败:', error)
  } finally {
    navigationLoading.value = false
  }
}

// 获取服务器分组列表
const fetchServerCategories = async () => {
  serverLoading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/categories/type/server`)
    const result: ApiResponse<Category[]> = await response.json()

    if (result.success) {
      serverCategories.value = result.data
    } else {
      console.error('获取服务器分组失败:', result.message)
    }
  } catch (error) {
    console.error('获取服务器分组失败:', error)
  } finally {
    serverLoading.value = false
  }
}

// 保存导航分组排序
const saveNavigationCategoriesSort = async () => {
  navigationSaving.value = true
  try {
    // 更新排序号
    const sortedCategories = navigationCategories.value.map((category, index) => ({
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
      navigationCategories.value = sortedCategories
      console.log('导航分组排序保存成功')
    } else {
      console.error('保存导航分组排序失败:', result.message)
      // 重新获取数据以恢复原始顺序
      await fetchNavigationCategories()
    }
  } catch (error) {
    console.error('保存导航分组排序失败:', error)
    // 重新获取数据以恢复原始顺序
    await fetchNavigationCategories()
  } finally {
    navigationSaving.value = false
  }
}

// 保存服务器分组排序
const saveServerCategoriesSort = async () => {
  serverSaving.value = true
  try {
    // 更新排序号
    const sortedCategories = serverCategories.value.map((category, index) => ({
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
      serverCategories.value = sortedCategories
      console.log('服务器分组排序保存成功')
    } else {
      console.error('保存服务器分组排序失败:', result.message)
      // 重新获取数据以恢复原始顺序
      await fetchServerCategories()
    }
  } catch (error) {
    console.error('保存服务器分组排序失败:', error)
    // 重新获取数据以恢复原始顺序
    await fetchServerCategories()
  } finally {
    serverSaving.value = false
  }
}

// 处理导航分组拖拽结束
const handleNavigationDragEnd = () => {
  console.log('导航分组拖拽排序完成')
  saveNavigationCategoriesSort()
}

// 处理服务器分组拖拽结束
const handleServerDragEnd = () => {
  console.log('服务器分组拖拽排序完成')
  saveServerCategoriesSort()
}

// 创建新导航分组
const createNavigationCategory = async () => {
  if (!addNavigationCategoryForm.value.name.trim()) {
    console.error('导航分组名称不能为空')
    return
  }

  addNavigationCategoryLoading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/categories`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: addNavigationCategoryForm.value.name.trim(),
        type: 'navigation'
      })
    })

    const result: ApiResponse<Category> = await response.json()

    if (result.success) {
      console.log('导航分组创建成功')
      // 重置表单
      addNavigationCategoryForm.value.name = ''
      showAddNavigationCategoryForm.value = false
      // 重新获取分组列表
      await fetchNavigationCategories()
    } else {
      console.error('导航分组创建失败:', result.message)
    }
  } catch (error) {
    console.error('导航分组创建失败:', error)
  } finally {
    addNavigationCategoryLoading.value = false
  }
}

// 取消新增导航分组
const cancelAddNavigationCategory = () => {
  addNavigationCategoryForm.value.name = ''
  showAddNavigationCategoryForm.value = false
}

// 创建新服务器分组
const createServerCategory = async () => {
  if (!addServerCategoryForm.value.name.trim()) {
    console.error('服务器分组名称不能为空')
    return
  }

  addServerCategoryLoading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/categories`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: addServerCategoryForm.value.name.trim(),
        type: 'server'
      })
    })

    const result: ApiResponse<Category> = await response.json()

    if (result.success) {
      console.log('服务器分组创建成功')
      // 重置表单
      addServerCategoryForm.value.name = ''
      showAddServerCategoryForm.value = false
      // 重新获取分组列表
      await fetchServerCategories()
    } else {
      console.error('服务器分组创建失败:', result.message)
    }
  } catch (error) {
    console.error('服务器分组创建失败:', error)
  } finally {
    addServerCategoryLoading.value = false
  }
}

// 取消新增服务器分组
const cancelAddServerCategory = () => {
  addServerCategoryForm.value.name = ''
  showAddServerCategoryForm.value = false
}

// 开始编辑导航分组
const startEditNavigationCategory = (category: Category) => {
  editingNavigationCategoryId.value = category.id
  editNavigationCategoryForm.value.name = category.name
}

// 保存编辑导航分组
const saveEditNavigationCategory = async () => {
  if (!editNavigationCategoryForm.value.name.trim()) {
    console.error('导航分组名称不能为空')
    return
  }

  if (editingNavigationCategoryId.value === null) {
    return
  }

  editNavigationCategoryLoading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/categories/${editingNavigationCategoryId.value}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: editNavigationCategoryForm.value.name.trim()
      })
    })

    const result: ApiResponse<Category> = await response.json()

    if (result.success) {
      console.log('导航分组更新成功')
      // 取消编辑状态
      cancelEditNavigationCategory()
      // 重新获取分组列表
      await fetchNavigationCategories()
    } else {
      console.error('导航分组更新失败:', result.message)
    }
  } catch (error) {
    console.error('导航分组更新失败:', error)
  } finally {
    editNavigationCategoryLoading.value = false
  }
}

// 取消编辑导航分组
const cancelEditNavigationCategory = () => {
  editingNavigationCategoryId.value = null
  editNavigationCategoryForm.value.name = ''
}

// 开始编辑服务器分组
const startEditServerCategory = (category: Category) => {
  editingServerCategoryId.value = category.id
  editServerCategoryForm.value.name = category.name
}

// 保存编辑服务器分组
const saveEditServerCategory = async () => {
  if (!editServerCategoryForm.value.name.trim()) {
    console.error('服务器分组名称不能为空')
    return
  }

  if (editingServerCategoryId.value === null) {
    return
  }

  editServerCategoryLoading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/categories/${editingServerCategoryId.value}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: editServerCategoryForm.value.name.trim()
      })
    })

    const result: ApiResponse<Category> = await response.json()

    if (result.success) {
      console.log('服务器分组更新成功')
      // 取消编辑状态
      cancelEditServerCategory()
      // 重新获取分组列表
      await fetchServerCategories()
    } else {
      console.error('服务器分组更新失败:', result.message)
    }
  } catch (error) {
    console.error('服务器分组更新失败:', error)
  } finally {
    editServerCategoryLoading.value = false
  }
}

// 取消编辑服务器分组
const cancelEditServerCategory = () => {
  editingServerCategoryId.value = null
  editServerCategoryForm.value.name = ''
}

// 显示删除导航分组确认对话框
const showDeleteNavigationCategoryConfirm = (category: Category) => {
  deletingNavigationCategory.value = category
  showDeleteNavigationConfirm.value = true
}

// 确认删除导航分组
const confirmDeleteNavigationCategory = async () => {
  if (!deletingNavigationCategory.value) {
    return
  }

  deleteNavigationCategoryLoading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/categories/${deletingNavigationCategory.value.id}`, {
      method: 'DELETE'
    })

    const result: ApiResponse<string> = await response.json()

    if (result.success) {
      console.log('导航分组删除成功')
      // 关闭确认对话框
      cancelDeleteNavigationCategory()
      // 重新获取分组列表
      await fetchNavigationCategories()
    } else {
      console.error('导航分组删除失败:', result.message)
    }
  } catch (error) {
    console.error('导航分组删除失败:', error)
  } finally {
    deleteNavigationCategoryLoading.value = false
  }
}

// 取消删除导航分组
const cancelDeleteNavigationCategory = () => {
  deletingNavigationCategory.value = null
  showDeleteNavigationConfirm.value = false
}

// 显示删除服务器分组确认对话框
const showDeleteServerCategoryConfirmDialog = (category: Category) => {
  deletingServerCategory.value = category
  showDeleteServerCategoryConfirm.value = true
}

// 确认删除服务器分组
const confirmDeleteServerCategory = async () => {
  if (!deletingServerCategory.value) {
    return
  }

  deleteServerCategoryLoading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/categories/${deletingServerCategory.value.id}`, {
      method: 'DELETE'
    })

    const result: ApiResponse<string> = await response.json()

    if (result.success) {
      console.log('服务器分组删除成功')
      // 关闭确认对话框
      cancelDeleteServerCategory()
      // 重新获取分组列表
      await fetchServerCategories()
    } else {
      console.error('服务器分组删除失败:', result.message)
    }
  } catch (error) {
    console.error('服务器分组删除失败:', error)
  } finally {
    deleteServerCategoryLoading.value = false
  }
}

// 取消删除服务器分组
const cancelDeleteServerCategory = () => {
  deletingServerCategory.value = null
  showDeleteServerCategoryConfirm.value = false
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
  const allCollapsed = isNavigationGroupManagementCollapsed.value &&
                      isServerGroupManagementCollapsed.value &&
                      isPasswordSettingsCollapsed.value &&
                      isSystemConfigCollapsed.value &&
                      isThemeSettingsCollapsed.value &&
                      isMusicSettingsCollapsed.value &&
                      isServerSettingsCollapsed.value &&
                      isBackupRestoreCollapsed.value &&
                      isSystemInfoCollapsed.value

  const newState = !allCollapsed

  isNavigationGroupManagementCollapsed.value = newState
  isServerGroupManagementCollapsed.value = newState
  isPasswordSettingsCollapsed.value = newState
  isSystemConfigCollapsed.value = newState
  isThemeSettingsCollapsed.value = newState
  isMusicSettingsCollapsed.value = newState
  isServerSettingsCollapsed.value = newState
  isBackupRestoreCollapsed.value = newState
  isSystemInfoCollapsed.value = newState
}

// 计算是否全部收起
const allSectionsCollapsed = computed(() => {
  return isNavigationGroupManagementCollapsed.value &&
         isServerGroupManagementCollapsed.value &&
         isPasswordSettingsCollapsed.value &&
         isSystemConfigCollapsed.value &&
         isThemeSettingsCollapsed.value &&
         isMusicSettingsCollapsed.value &&
         isServerSettingsCollapsed.value &&
         isBackupRestoreCollapsed.value &&
         isSystemInfoCollapsed.value
})



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

// Logo相关函数
const uploadLogo = async (event) => {
  const target = event.target
  const file = target.files?.[0]

  if (!file) return

  logoUploading.value = true

  try {
    const formData = new FormData()
    formData.append('file', file)

    const response = await apiRequest(`${API_BASE_URL}/system-config/logo/upload`, {
      method: 'POST',
      body: formData
    })

    const result = await response.json()

    if (result.success) {
      currentLogo.value = result.data.logoUrl

      // 触发logo更新事件
      if (process.client) {
        window.dispatchEvent(new CustomEvent('logoChanged', {
          detail: { logoUrl: result.data.logoUrl }
        }))
      }

      console.log('Logo上传成功')
    } else {
      throw new Error(result.message)
    }
  } catch (error) {
    console.error('Logo上传失败:', error)
  } finally {
    logoUploading.value = false
    // 清空input值，允许重复选择同一文件
    target.value = ''
  }
}

const resetLogo = async () => {
  logoResetting.value = true

  try {
    const response = await apiRequest(`${API_BASE_URL}/system-config/logo/reset`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      }
    })

    const result = await response.json()

    if (result.success) {
      currentLogo.value = ''

      // 触发logo更新事件
      if (process.client) {
        window.dispatchEvent(new CustomEvent('logoChanged', {
          detail: { logoUrl: '' }
        }))
      }

      console.log('Logo已重置为默认')
    } else {
      throw new Error(result.message)
    }
  } catch (error) {
    console.error('Logo重置失败:', error)
  } finally {
    logoResetting.value = false
  }
}

const loadLogoConfig = async () => {
  try {
    const response = await apiRequest(`${API_BASE_URL}/system-config/logo`)
    const result = await response.json()

    if (result.success) {
      currentLogo.value = result.data.logoUrl || ''
    } else {
      console.error('加载Logo配置失败:', result.message)
    }
  } catch (error) {
    console.error('加载Logo配置失败:', error)
  }
}

// 音乐设置相关函数
const loadMusicConfig = async () => {
  musicSettingsLoading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/system-config/music`)
    const result = await response.json()

    if (result.success) {
      musicDownloadLocation.value = result.data.downloadLocation || 'local'
      musicServerDownloadPath.value = result.data.serverDownloadPath || 'uploads/music'
    } else {
      console.error('加载音乐配置失败:', result.message)
    }
  } catch (error) {
    console.error('加载音乐配置失败:', error)
  } finally {
    musicSettingsLoading.value = false
  }
}

// Cookie设置相关函数
const loadCookieConfig = async () => {
  cookieSettingsLoading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/system-config/cookies`)
    const result = await response.json()

    if (result.success) {
      bilibiliCookie.value = result.data.bilibiliCookie || ''
      youtubeCookie.value = result.data.youtubeCookie || ''
    } else {
      console.error('加载Cookie配置失败:', result.message)
    }
  } catch (error) {
    console.error('加载Cookie配置失败:', error)
  } finally {
    cookieSettingsLoading.value = false
  }
}

const saveMusicConfig = async () => {
  musicSettingsSaving.value = true
  try {
    const formData = new FormData()
    formData.append('downloadLocation', musicDownloadLocation.value)
    if (musicDownloadLocation.value === 'server') {
      formData.append('serverDownloadPath', musicServerDownloadPath.value)
    }

    const response = await apiRequest(`${API_BASE_URL}/system-config/music`, {
      method: 'POST',
      body: formData
    })

    const result = await response.json()

    if (result.success) {
      console.log('音乐配置保存成功')
    } else {
      console.error('音乐配置保存失败:', result.message)
    }
  } catch (error) {
    console.error('音乐配置保存失败:', error)
  } finally {
    musicSettingsSaving.value = false
  }
}

const saveCookieConfig = async () => {
  cookieSettingsSaving.value = true
  try {
    const formData = new FormData()
    formData.append('bilibiliCookie', bilibiliCookie.value)
    formData.append('youtubeCookie', youtubeCookie.value)

    const response = await apiRequest(`${API_BASE_URL}/system-config/cookies`, {
      method: 'POST',
      body: formData
    })

    const result = await response.json()

    if (result.success) {
      console.log('Cookie配置保存成功')
    } else {
      console.error('Cookie配置保存失败:', result.message)
    }
  } catch (error) {
    console.error('Cookie配置保存失败:', error)
  } finally {
    cookieSettingsSaving.value = false
  }
}

// 服务器设置相关函数
const loadServerConfigs = async () => {
  serverSettingsLoading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/servers`)
    const result = await response.json()

    if (result.success) {
      serverConfigs.value = result.data || []
    } else {
      console.error('加载服务器配置失败:', result.message)
    }
  } catch (error) {
    console.error('加载服务器配置失败:', error)
  } finally {
    serverSettingsLoading.value = false
  }
}

const saveServerConfig = async () => {
  if (!serverForm.value.serverName.trim()) {
    console.error('服务器名称不能为空')
    return
  }
  if (!serverForm.value.host.trim()) {
    console.error('服务器地址不能为空')
    return
  }
  if (!serverForm.value.username.trim()) {
    console.error('用户名不能为空')
    return
  }

  if (serverForm.value.authType === 'password' && !serverForm.value.password.trim()) {
    console.error('密码认证时密码不能为空')
    return
  }
  if (serverForm.value.authType === 'publickey' && !serverForm.value.privateKey.trim()) {
    console.error('公钥认证时私钥不能为空')
    return
  }

  serverSettingsSaving.value = true
  try {
    const formData = { ...serverForm.value }

    // 处理分组信息
    if (!formData.groupName || !formData.groupName.trim()) {
      // 如果没有选择分组，使用默认分组
      formData.groupName = '默认分组'
    }

    // 后端会根据groupName自动处理categoryId，所以保留groupName字段

    const url = editingServerId.value
      ? `${API_BASE_URL}/servers/${editingServerId.value}`
      : `${API_BASE_URL}/servers`

    const method = editingServerId.value ? 'PUT' : 'POST'

    const response = await apiRequest(url, {
      method: method,
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(formData)
    })

    const result = await response.json()

    if (result.success) {
      console.log('服务器配置保存成功')
      resetServerForm()
      showAddServerForm.value = false
      showEditServerForm.value = false
      editingServerId.value = null
      await loadServerConfigs()
    } else {
      console.error('服务器配置保存失败:', result.message)
    }
  } catch (error) {
    console.error('服务器配置保存失败:', error)
  } finally {
    serverSettingsSaving.value = false
  }
}

const startEditServer = (server) => {
  editingServerId.value = server.id

  // 如果服务器有categoryId，需要找到对应的分组名称
  let groupName = server.groupName || ''
  if (server.categoryId && !groupName) {
    const category = serverCategories.value.find(cat => cat.id === server.categoryId)
    if (category) {
      groupName = category.name
    }
  }

  serverForm.value = {
    serverName: server.serverName || '',
    host: server.host || '',
    port: server.port || 22,
    username: server.username || '',
    authType: server.authType || 'password',
    password: '', // 不显示已保存的密码
    privateKey: '', // 不显示已保存的私钥
    privateKeyPassword: '',
    selectedPrivateKeyId: '',
    useExistingKey: false,
    description: server.description || '',
    groupName: groupName,
    isDefault: server.isDefault || false
  }
  showEditServerForm.value = true
}

const cancelServerEdit = () => {
  resetServerForm()
  showAddServerForm.value = false
  showEditServerForm.value = false
  editingServerId.value = null
}

const showDeleteServerConfirmDialog = (server) => {
  deletingServer.value = server
  showDeleteServerConfirm.value = true
}

const confirmDeleteServer = async () => {
  if (!deletingServer.value) return

  deleteServerLoading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/servers/${deletingServer.value.id}`, {
      method: 'DELETE'
    })

    const result = await response.json()

    if (result.success) {
      console.log('服务器配置删除成功')
      showDeleteServerConfirm.value = false
      deletingServer.value = null
      await loadServerConfigs()
    } else {
      console.error('服务器配置删除失败:', result.message)
    }
  } catch (error) {
    console.error('服务器配置删除失败:', error)
  } finally {
    deleteServerLoading.value = false
  }
}

const cancelDeleteServer = () => {
  deletingServer.value = null
  showDeleteServerConfirm.value = false
}

const setDefaultServer = async (serverId) => {
  try {
    const response = await apiRequest(`${API_BASE_URL}/servers/${serverId}/set-default`, {
      method: 'POST'
    })

    const result = await response.json()

    if (result.success) {
      console.log('默认服务器设置成功')
      await loadServerConfigs()
    } else {
      console.error('默认服务器设置失败:', result.message)
    }
  } catch (error) {
    console.error('默认服务器设置失败:', error)
  }
}

// 拖拽排序处理
const onServerDragEnd = async (groupName, newOrder) => {
  try {
    // 更新本地排序
    const serversInGroup = newOrder.map((server, index) => ({
      ...server,
      sortOrder: index
    }))

    // 更新serverConfigs中对应的服务器
    serversInGroup.forEach(updatedServer => {
      const index = serverConfigs.value.findIndex(s => s.id === updatedServer.id)
      if (index !== -1) {
        serverConfigs.value[index] = updatedServer
      }
    })

    // 发送到后端保存排序
    const response = await apiRequest(`${API_BASE_URL}/servers/update-order`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        servers: serversInGroup.map(server => ({
          id: server.id,
          sortOrder: server.sortOrder
        }))
      })
    })

    const result = await response.json()
    if (!result.success) {
      console.error('更新服务器排序失败:', result.message)
      // 如果失败，重新加载服务器列表
      await loadServerConfigs()
    }
  } catch (error) {
    console.error('更新服务器排序失败:', error)
    // 如果失败，重新加载服务器列表
    await loadServerConfigs()
  }
}

const testServerConnection = async () => {
  if (!serverForm.value.host.trim() || !serverForm.value.username.trim()) {
    console.error('请填写完整的服务器信息')
    return
  }

  try {
    const response = await apiRequest(`${API_BASE_URL}/servers/test`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(serverForm.value)
    })

    const result = await response.json()

    if (result.success) {
      console.log('服务器连接测试成功')
    } else {
      console.error('服务器连接测试失败:', result.message)
    }
  } catch (error) {
    console.error('服务器连接测试失败:', error)
  }
}

// 私钥管理相关函数
const loadPrivateKeys = async () => {
  privateKeysLoading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/system-config/private-keys`)
    const result = await response.json()

    if (result.success) {
      privateKeys.value = result.data || []
    } else {
      console.error('加载私钥配置失败:', result.message)
    }
  } catch (error) {
    console.error('加载私钥配置失败:', error)
  } finally {
    privateKeysLoading.value = false
  }
}

const savePrivateKey = async () => {
  if (!privateKeyForm.value.keyName.trim()) {
    console.error('私钥名称不能为空')
    return
  }
  if (!privateKeyForm.value.privateKey.trim()) {
    console.error('私钥内容不能为空')
    return
  }

  privateKeySaving.value = true
  try {
    const url = editingPrivateKeyId.value
      ? `${API_BASE_URL}/system-config/private-keys/${editingPrivateKeyId.value}`
      : `${API_BASE_URL}/system-config/private-keys`

    const method = editingPrivateKeyId.value ? 'PUT' : 'POST'

    const response = await apiRequest(url, {
      method: method,
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(privateKeyForm.value)
    })

    const result = await response.json()

    if (result.success) {
      console.log('私钥配置保存成功')
      resetPrivateKeyForm()
      showAddPrivateKeyForm.value = false
      showEditPrivateKeyForm.value = false
      editingPrivateKeyId.value = null
      await loadPrivateKeys()
    } else {
      console.error('私钥配置保存失败:', result.message)
    }
  } catch (error) {
    console.error('私钥配置保存失败:', error)
  } finally {
    privateKeySaving.value = false
  }
}

const startEditPrivateKey = async (privateKey) => {
  try {
    // 获取私钥详情（包含私钥内容）
    const response = await apiRequest(`${API_BASE_URL}/system-config/private-keys/${privateKey.id}`)
    const result = await response.json()

    if (result.success) {
      editingPrivateKeyId.value = privateKey.id
      privateKeyForm.value = {
        keyName: result.data.keyName || '',
        privateKey: result.data.privateKey || '',
        privateKeyPassword: result.data.privateKeyPassword || '',
        description: result.data.description || '',
        keyType: result.data.keyType || ''
      }
      showEditPrivateKeyForm.value = true
    } else {
      console.error('获取私钥详情失败:', result.message)
    }
  } catch (error) {
    console.error('获取私钥详情失败:', error)
  }
}

const cancelPrivateKeyEdit = () => {
  resetPrivateKeyForm()
  showAddPrivateKeyForm.value = false
  showEditPrivateKeyForm.value = false
  editingPrivateKeyId.value = null
}

const showDeletePrivateKeyConfirmDialog = (privateKey) => {
  deletingPrivateKey.value = privateKey
  showDeletePrivateKeyConfirm.value = true
}

const confirmDeletePrivateKey = async () => {
  if (!deletingPrivateKey.value) return

  deletePrivateKeyLoading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/system-config/private-keys/${deletingPrivateKey.value.id}`, {
      method: 'DELETE'
    })

    const result = await response.json()

    if (result.success) {
      console.log('私钥配置删除成功')
      showDeletePrivateKeyConfirm.value = false
      deletingPrivateKey.value = null
      await loadPrivateKeys()
    } else {
      console.error('私钥配置删除失败:', result.message)
    }
  } catch (error) {
    console.error('私钥配置删除失败:', error)
  } finally {
    deletePrivateKeyLoading.value = false
  }
}

const cancelDeletePrivateKey = () => {
  deletingPrivateKey.value = null
  showDeletePrivateKeyConfirm.value = false
}

const selectExistingPrivateKey = async (keyId) => {
  if (!keyId) {
    serverForm.value.privateKey = ''
    serverForm.value.privateKeyPassword = ''
    return
  }

  try {
    const response = await apiRequest(`${API_BASE_URL}/system-config/private-keys/${keyId}`)
    const result = await response.json()

    if (result.success) {
      serverForm.value.privateKey = result.data.privateKey || ''
      serverForm.value.privateKeyPassword = result.data.privateKeyPassword || ''
    } else {
      console.error('获取私钥详情失败:', result.message)
    }
  } catch (error) {
    console.error('获取私钥详情失败:', error)
  }
}

// 私钥下拉选择相关方法
const getSelectedPrivateKeyName = () => {
  if (!serverForm.value.selectedPrivateKeyId) return ''
  const selectedKey = privateKeys.value.find(key => key.id === serverForm.value.selectedPrivateKeyId)
  if (!selectedKey) return ''

  let name = selectedKey.keyName
  if (selectedKey.description) {
    name += ` (${selectedKey.description})`
  }
  return name
}

const selectPrivateKeyOption = async (keyId) => {
  serverForm.value.selectedPrivateKeyId = keyId
  showPrivateKeyDropdown.value = false
  await selectExistingPrivateKey(keyId)
}

const selectKeyTypeOption = (keyType) => {
  privateKeyForm.value.keyType = keyType
  showKeyTypeDropdown.value = false
}

// 服务器分组选择相关方法
const selectServerGroupOption = (groupName) => {
  serverForm.value.groupName = groupName
  showServerGroupDropdown.value = false
}

const getSelectedServerGroupName = () => {
  return serverForm.value.groupName || '请选择分组'
}

// 点击外部关闭下拉框
const handleClickOutside = (event) => {
  const selectWrapper = event.target.closest('.custom-select-wrapper')
  if (!selectWrapper) {
    showPrivateKeyDropdown.value = false
    showKeyTypeDropdown.value = false
    showServerGroupDropdown.value = false
  }
}

// 动画事件处理函数
const onEnter = (el: HTMLElement) => {
  // 获取元素的实际高度
  el.style.height = 'auto'
  const height = el.offsetHeight
  el.style.height = '0'

  // 强制重绘
  el.offsetHeight

  // 设置目标高度
  el.style.height = height + 'px'
}

const onAfterEnter = (el: HTMLElement) => {
  // 动画完成后设置为auto，允许内容动态调整
  el.style.height = 'auto'
}

const onLeave = (el: HTMLElement) => {
  // 设置当前高度
  el.style.height = el.offsetHeight + 'px'

  // 强制重绘
  el.offsetHeight

  // 设置目标高度为0
  el.style.height = '0'
}

const onAfterLeave = (el: HTMLElement) => {
  // 清理样式
  el.style.height = ''
}

// 页面加载时获取数据
onMounted(async () => {
  fetchNavigationCategories()
  fetchServerCategories()
  await loadSavedWallpaper()
  await loadLogoConfig()
  await loadMusicConfig()
  await loadCookieConfig()
  await loadServerConfigs()
  await loadPrivateKeys()

  // 添加点击外部关闭下拉框的事件监听
  document.addEventListener('click', handleClickOutside)
})

// 页面卸载时移除事件监听
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <NuxtLayout>
    <div class="settings-container">
      <!-- 页面标题 -->
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">系统设置</h1>
          <p class="page-description">分别管理导航分组、服务器分组和系统配置</p>
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
        <!-- 分组管理 - 左右布局 -->
        <div class="settings-item group-management-item full-width">
          <ClientOnly>
            <div class="settings-wrapper">
              <div class="item-header">
                <div class="header-content">
                  <Icon icon="mdi:folder-multiple" class="header-icon" />
                  <div>
                    <h2 class="item-title">分组管理</h2>
                    <p class="item-description">分别管理导航分组和服务器分组</p>
                  </div>
                </div>
              </div>

              <!-- 左右分栏布局 -->
              <div class="group-management-layout">
                <!-- 左侧：导航分组管理 -->
                <div class="group-section navigation-groups">
                  <div class="section-header" @click="isNavigationGroupManagementCollapsed = !isNavigationGroupManagementCollapsed">
                    <div class="header-content">
                      <Icon icon="mdi:navigation" class="header-icon" />
                      <div>
                        <h3 class="section-title">导航分组</h3>
                        <p class="section-description">管理导航页面的分组</p>
                      </div>
                    </div>
                    <div class="header-actions">
                      <button
                        v-if="!showAddNavigationCategoryForm && !isNavigationGroupManagementCollapsed"
                        class="add-category-btn"
                        @click.stop="showAddNavigationCategoryForm = true"
                      >
                        <Icon icon="mdi:plus" class="btn-icon" />
                        新增分组
                      </button>
                      <button
                        v-if="navigationSaving && !isNavigationGroupManagementCollapsed"
                        class="save-button saving"
                        disabled
                      >
                        <Icon icon="mdi:loading" class="spin" />
                        保存中...
                      </button>
                      <button class="collapse-btn" :class="{ collapsed: isNavigationGroupManagementCollapsed }">
                        <Icon icon="mdi:chevron-down" class="collapse-icon" />
                      </button>
                    </div>
                  </div>

                  <Transition
                    name="expand"
                    mode="out-in"
                    @enter="onEnter"
                    @after-enter="onAfterEnter"
                    @leave="onLeave"
                    @after-leave="onAfterLeave"
                  >
                    <div v-if="!isNavigationGroupManagementCollapsed" class="section-content">
                      <!-- 新增导航分组表单 -->
                      <div v-if="showAddNavigationCategoryForm" class="add-category-form">
                        <div class="form-header">
                          <h4 class="form-title">新增导航分组</h4>
                          <p class="form-description">输入分组名称创建新的导航分组</p>
                        </div>

                        <div class="form-group">
                          <label class="form-label">分组名称</label>
                          <input
                            v-model="addNavigationCategoryForm.name"
                            type="text"
                            class="form-input"
                            placeholder="请输入分组名称"
                            @keyup.enter="createNavigationCategory"
                            :disabled="addNavigationCategoryLoading"
                          />
                        </div>

                        <div class="form-actions">
                          <button
                            class="cancel-btn"
                            @click="cancelAddNavigationCategory"
                            :disabled="addNavigationCategoryLoading"
                          >
                            取消
                          </button>
                          <button
                            class="save-btn"
                            @click="createNavigationCategory"
                            :disabled="addNavigationCategoryLoading || !addNavigationCategoryForm.name.trim()"
                          >
                            <Icon v-if="addNavigationCategoryLoading" icon="mdi:loading" class="spin btn-icon" />
                            <Icon v-else icon="mdi:check" class="btn-icon" />
                            {{ addNavigationCategoryLoading ? '创建中...' : '创建' }}
                          </button>
                        </div>
                      </div>

                      <div v-else-if="navigationLoading" class="loading-state compact">
                        <Icon icon="mdi:loading" class="loading-icon spin" />
                        <p>加载中...</p>
                      </div>

                      <div v-else-if="navigationCategories.length === 0" class="empty-state compact">
                        <Icon icon="mdi:folder-off" class="empty-icon" />
                        <p>暂无导航分组</p>
                      </div>

                      <VueDraggable
                        v-else
                        v-model="navigationCategories"
                        class="categories-list"
                        :animation="200"
                        ghost-class="ghost-item"
                        chosen-class="chosen-item"
                        drag-class="drag-item"
                        @end="handleNavigationDragEnd"
                      >
                        <div
                          v-for="(category, index) in navigationCategories"
                          :key="category.id"
                          class="category-item"
                        >
                          <div class="drag-handle">
                            <Icon icon="mdi:drag-vertical" class="drag-icon" />
                          </div>

                          <div class="category-info">
                            <!-- 编辑状态 -->
                            <div v-if="editingNavigationCategoryId === category.id" class="edit-form">
                              <input
                                v-model="editNavigationCategoryForm.name"
                                type="text"
                                class="edit-input"
                                placeholder="请输入分组名称"
                                @keyup.enter="saveEditNavigationCategory"
                                @keyup.esc="cancelEditNavigationCategory"
                                :disabled="editNavigationCategoryLoading"
                              />
                              <div class="edit-actions">
                                <button
                                  class="edit-save-btn"
                                  @click="saveEditNavigationCategory"
                                  :disabled="editNavigationCategoryLoading || !editNavigationCategoryForm.name.trim()"
                                >
                                  <Icon v-if="editNavigationCategoryLoading" icon="mdi:loading" class="spin" />
                                  <Icon v-else icon="mdi:check" />
                                </button>
                                <button
                                  class="edit-cancel-btn"
                                  @click="cancelEditNavigationCategory"
                                  :disabled="editNavigationCategoryLoading"
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
                            <div v-if="editingNavigationCategoryId !== category.id" class="action-buttons">
                              <button
                                class="edit-btn"
                                @click="startEditNavigationCategory(category)"
                                title="编辑分组"
                              >
                                <Icon icon="mdi:pencil" />
                              </button>
                              <button
                                class="delete-btn"
                                @click="showDeleteNavigationCategoryConfirm(category)"
                                title="删除分组"
                              >
                                <Icon icon="mdi:delete" />
                              </button>
                            </div>
                          </div>
                        </div>
                      </VueDraggable>
                    </div>
                  </Transition>
                </div>

                <!-- 右侧：服务器分组管理 -->
                <div class="group-section server-groups">
                  <div class="section-header" @click="isServerGroupManagementCollapsed = !isServerGroupManagementCollapsed">
                    <div class="header-content">
                      <Icon icon="mdi:server" class="header-icon" />
                      <div>
                        <h3 class="section-title">服务器分组</h3>
                        <p class="section-description">管理SSH服务器的分组</p>
                      </div>
                    </div>
                    <div class="header-actions">
                      <button
                        v-if="!showAddServerCategoryForm && !isServerGroupManagementCollapsed"
                        class="add-category-btn"
                        @click.stop="showAddServerCategoryForm = true"
                      >
                        <Icon icon="mdi:plus" class="btn-icon" />
                        新增分组
                      </button>
                      <button
                        v-if="serverSaving && !isServerGroupManagementCollapsed"
                        class="save-button saving"
                        disabled
                      >
                        <Icon icon="mdi:loading" class="spin" />
                        保存中...
                      </button>
                      <button class="collapse-btn" :class="{ collapsed: isServerGroupManagementCollapsed }">
                        <Icon icon="mdi:chevron-down" class="collapse-icon" />
                      </button>
                    </div>
                  </div>

                  <Transition
                    name="expand"
                    mode="out-in"
                    @enter="onEnter"
                    @after-enter="onAfterEnter"
                    @leave="onLeave"
                    @after-leave="onAfterLeave"
                  >
                    <div v-if="!isServerGroupManagementCollapsed" class="section-content">
                      <!-- 新增服务器分组表单 -->
                      <div v-if="showAddServerCategoryForm" class="add-category-form">
                        <div class="form-header">
                          <h4 class="form-title">新增服务器分组</h4>
                          <p class="form-description">输入分组名称创建新的服务器分组</p>
                        </div>

                        <div class="form-group">
                          <label class="form-label">分组名称</label>
                          <input
                            v-model="addServerCategoryForm.name"
                            type="text"
                            class="form-input"
                            placeholder="请输入分组名称"
                            @keyup.enter="createServerCategory"
                            :disabled="addServerCategoryLoading"
                          />
                        </div>

                        <div class="form-actions">
                          <button
                            class="cancel-btn"
                            @click="cancelAddServerCategory"
                            :disabled="addServerCategoryLoading"
                          >
                            取消
                          </button>
                          <button
                            class="save-btn"
                            @click="createServerCategory"
                            :disabled="addServerCategoryLoading || !addServerCategoryForm.name.trim()"
                          >
                            <Icon v-if="addServerCategoryLoading" icon="mdi:loading" class="spin btn-icon" />
                            <Icon v-else icon="mdi:check" class="btn-icon" />
                            {{ addServerCategoryLoading ? '创建中...' : '创建' }}
                          </button>
                        </div>
                      </div>

                      <div v-else-if="serverLoading" class="loading-state compact">
                        <Icon icon="mdi:loading" class="loading-icon spin" />
                        <p>加载中...</p>
                      </div>

                      <div v-else-if="serverCategories.length === 0" class="empty-state compact">
                        <Icon icon="mdi:folder-off" class="empty-icon" />
                        <p>暂无服务器分组</p>
                      </div>

                      <VueDraggable
                        v-else
                        v-model="serverCategories"
                        class="categories-list"
                        :animation="200"
                        ghost-class="ghost-item"
                        chosen-class="chosen-item"
                        drag-class="drag-item"
                        @end="handleServerDragEnd"
                      >
                        <div
                          v-for="(category, index) in serverCategories"
                          :key="category.id"
                          class="category-item"
                        >
                          <div class="drag-handle">
                            <Icon icon="mdi:drag-vertical" class="drag-icon" />
                          </div>

                          <div class="category-info">
                            <!-- 编辑状态 -->
                            <div v-if="editingServerCategoryId === category.id" class="edit-form">
                              <input
                                v-model="editServerCategoryForm.name"
                                type="text"
                                class="edit-input"
                                placeholder="请输入分组名称"
                                @keyup.enter="saveEditServerCategory"
                                @keyup.esc="cancelEditServerCategory"
                                :disabled="editServerCategoryLoading"
                              />
                              <div class="edit-actions">
                                <button
                                  class="edit-save-btn"
                                  @click="saveEditServerCategory"
                                  :disabled="editServerCategoryLoading || !editServerCategoryForm.name.trim()"
                                >
                                  <Icon v-if="editServerCategoryLoading" icon="mdi:loading" class="spin" />
                                  <Icon v-else icon="mdi:check" />
                                </button>
                                <button
                                  class="edit-cancel-btn"
                                  @click="cancelEditServerCategory"
                                  :disabled="editServerCategoryLoading"
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
                            <div v-if="editingServerCategoryId !== category.id" class="action-buttons">
                              <button
                                class="edit-btn"
                                @click="startEditServerCategory(category)"
                                title="编辑分组"
                              >
                                <Icon icon="mdi:pencil" />
                              </button>
                              <button
                                class="delete-btn"
                                @click="showDeleteServerCategoryConfirmDialog(category)"
                                title="删除分组"
                              >
                                <Icon icon="mdi:delete" />
                              </button>
                            </div>
                          </div>
                        </div>
                      </VueDraggable>
                    </div>
                  </Transition>
                </div>
              </div>

              <BorderBeam
                  v-if="!isNavigationGroupManagementCollapsed || !isServerGroupManagementCollapsed"
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

              <Transition
                name="expand"
                mode="out-in"
                @enter="onEnter"
                @after-enter="onAfterEnter"
                @leave="onLeave"
                @after-leave="onAfterLeave"
              >
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
              </Transition>

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

              <Transition
                name="expand"
                mode="out-in"
                @enter="onEnter"
                @after-enter="onAfterEnter"
                @leave="onLeave"
                @after-leave="onAfterLeave"
              >
                <div v-if="!isSystemConfigCollapsed" class="item-content">
                  <!-- Logo设置 -->
                  <div class="config-section">
                    <div class="section-header">
                      <Icon icon="mdi:image" class="section-icon" />
                      <h3 class="section-title">Logo设置</h3>
                    </div>

                    <div class="logo-settings">
                      <!-- Logo预览 -->
                      <div class="logo-preview">
                        <div class="preview-container">
                          <img
                            v-if="currentLogo"
                            :src="currentLogo.startsWith('http') ? currentLogo : `${config.public.apiBaseUrl}${currentLogo}`"
                            alt="当前Logo"
                            class="logo-image"
                          />
                          <div v-else class="default-logo">
                            <Icon icon="mdi:image-outline" class="default-icon" />
                            <span>默认Logo</span>
                          </div>
                        </div>
                      </div>

                      <!-- Logo操作按钮 -->
                      <div class="logo-actions">
                        <label class="upload-btn" :class="{ disabled: logoUploading }">
                          <Icon
                            :icon="logoUploading ? 'mdi:loading' : 'mdi:upload'"
                            :class="['btn-icon', { 'animate-spin': logoUploading }]"
                          />
                          {{ logoUploading ? '上传中...' : '上传Logo' }}
                          <input
                            type="file"
                            accept="image/png,image/jpeg,image/jpg,image/svg+xml"
                            @change="uploadLogo"
                            :disabled="logoUploading"
                            style="display: none;"
                          />
                        </label>

                        <button
                          class="reset-btn"
                          @click="resetLogo"
                          :disabled="logoResetting || !currentLogo"
                        >
                          <Icon
                            :icon="logoResetting ? 'mdi:loading' : 'mdi:restore'"
                            :class="['btn-icon', { 'animate-spin': logoResetting }]"
                          />
                          {{ logoResetting ? '重置中...' : '重置默认' }}
                        </button>
                      </div>

                      <div class="logo-tips">
                        <p>支持 PNG、JPG、SVG 格式，建议尺寸 64x64 像素</p>
                      </div>
                    </div>
                  </div>

                  <div class="coming-soon">
                    <Icon icon="mdi:wrench" class="coming-soon-icon" />
                    <p>更多设置功能即将推出...</p>
                  </div>
                </div>
              </Transition>

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

              <Transition
                name="expand"
                mode="out-in"
                @enter="onEnter"
                @after-enter="onAfterEnter"
                @leave="onLeave"
                @after-leave="onAfterLeave"
              >
                <div v-if="!isThemeSettingsCollapsed" class="item-content">
                <!-- 壁纸管理器 -->
                <WallpaperManager
                  v-model:currentWallpaper="currentWallpaper"
                  v-model:wallpaperBlur="wallpaperBlur"
                  v-model:wallpaperMask="wallpaperMask"
                  @wallpaperChanged="handleWallpaperChanged"
                />
              </div>
              </Transition>

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

        <!-- 音乐设置 -->
        <div class="settings-item music-settings-item">
          <ClientOnly>
            <div class="settings-wrapper">
              <div class="item-header" @click="isMusicSettingsCollapsed = !isMusicSettingsCollapsed">
                <div class="header-content">
                  <Icon icon="mdi:music" class="header-icon" />
                  <div>
                    <h2 class="item-title">音乐设置</h2>
                    <p class="item-description">配置音乐下载选项</p>
                  </div>
                </div>
                <div class="header-actions">
                  <button class="collapse-btn" :class="{ collapsed: isMusicSettingsCollapsed }">
                    <Icon icon="mdi:chevron-down" class="collapse-icon" />
                  </button>
                </div>
              </div>

              <Transition
                name="expand"
                mode="out-in"
                @enter="onEnter"
                @after-enter="onAfterEnter"
                @leave="onLeave"
                @after-leave="onAfterLeave"
              >
                <div v-if="!isMusicSettingsCollapsed" class="item-content">
                  <div v-if="musicSettingsLoading" class="loading-state compact">
                    <Icon icon="mdi:loading" class="loading-icon spin" />
                    <p>加载中...</p>
                  </div>

                  <div v-else class="music-settings">
                    <!-- 下载位置设置 -->
                    <div class="config-section">
                      <div class="section-header">
                        <Icon icon="mdi:download" class="section-icon" />
                        <h3 class="section-title">下载位置</h3>
                      </div>

                      <div class="download-location-settings">
                        <div class="radio-group">
                          <label class="radio-option" :class="{ active: musicDownloadLocation === 'local' }">
                            <input
                              type="radio"
                              v-model="musicDownloadLocation"
                              value="local"
                              @change="saveMusicConfig"
                              :disabled="musicSettingsSaving"
                            />
                            <div class="radio-content">
                              <Icon icon="mdi:laptop" class="option-icon" />
                              <div class="option-text">
                                <span class="option-title">下载到本地</span>
                                <span class="option-description">音乐文件将下载到浏览器默认下载目录</span>
                              </div>
                            </div>
                          </label>

                          <label class="radio-option" :class="{ active: musicDownloadLocation === 'server' }">
                            <input
                              type="radio"
                              v-model="musicDownloadLocation"
                              value="server"
                              @change="saveMusicConfig"
                              :disabled="musicSettingsSaving"
                            />
                            <div class="radio-content">
                              <Icon icon="mdi:server" class="option-icon" />
                              <div class="option-text">
                                <span class="option-title">下载到服务器</span>
                                <span class="option-description">音乐文件将保存到服务器指定目录</span>
                              </div>
                            </div>
                          </label>
                        </div>

                        <!-- 服务器路径设置 -->
                        <div v-if="musicDownloadLocation === 'server'" class="server-path-settings">
                          <div class="form-group">
                            <label class="form-label">服务器下载路径</label>
                            <div class="path-input-group">
                              <input
                                v-model="musicServerDownloadPath"
                                type="text"
                                class="form-input"
                                placeholder="例如: uploads/music 或 /music/download"
                                @blur="saveMusicConfig"
                                :disabled="musicSettingsSaving"
                              />
                              <button
                                class="save-path-btn"
                                @click="saveMusicConfig"
                                :disabled="musicSettingsSaving"
                              >
                                <Icon v-if="musicSettingsSaving" icon="mdi:loading" class="spin btn-icon" />
                                <Icon v-else icon="mdi:check" class="btn-icon" />
                                {{ musicSettingsSaving ? '保存中...' : '保存' }}
                              </button>
                            </div>
                            <p class="form-hint">
                              支持相对路径（如 uploads/music）或绝对路径（如 /music/download）。
                              请确保服务器对指定路径有读写权限。
                            </p>
                          </div>
                        </div>

                        <!-- 保存状态提示 -->
                        <div v-if="musicSettingsSaving" class="saving-indicator">
                          <Icon icon="mdi:loading" class="spin" />
                          <span>正在保存设置...</span>
                        </div>
                      </div>
                    </div>

                    <!-- Cookie设置 -->
                    <div class="config-section">
                      <div class="section-header">
                        <Icon icon="mdi:cookie" class="section-icon" />
                        <h3 class="section-title">Cookie设置</h3>
                      </div>

                      <div v-if="cookieSettingsLoading" class="loading-state compact">
                        <Icon icon="mdi:loading" class="loading-icon spin" />
                        <p>加载中...</p>
                      </div>

                      <div v-else class="cookie-settings">
                        <!-- Bilibili Cookie设置 -->
                        <div class="cookie-group">
                          <div class="cookie-header">
                            <Icon icon="mdi:play-circle" class="platform-icon bilibili-icon" />
                            <h4 class="cookie-title">Bilibili Cookie</h4>
                          </div>
                          <div class="form-group">
                            <label class="form-label">Cookie值</label>
                            <textarea
                              v-model="bilibiliCookie"
                              class="cookie-textarea"
                              placeholder="请输入Bilibili的Cookie值..."
                              rows="3"
                              :disabled="cookieSettingsSaving"
                            ></textarea>
                            <p class="form-hint">
                              用于访问Bilibili需要登录的内容，提高解析成功率
                            </p>
                          </div>
                        </div>

                        <!-- YouTube Cookie设置 -->
                        <div class="cookie-group">
                          <div class="cookie-header">
                            <Icon icon="mdi:youtube" class="platform-icon youtube-icon" />
                            <h4 class="cookie-title">YouTube Cookie</h4>
                          </div>
                          <div class="form-group">
                            <label class="form-label">Cookie值</label>
                            <textarea
                              v-model="youtubeCookie"
                              class="cookie-textarea"
                              placeholder="请输入YouTube的Cookie值..."
                              rows="3"
                              :disabled="cookieSettingsSaving"
                            ></textarea>
                            <p class="form-hint">
                              用于访问YouTube需要登录的内容，提高解析成功率
                            </p>
                          </div>
                        </div>

                        <!-- Cookie保存按钮 -->
                        <div class="cookie-actions">
                          <button
                            class="save-cookie-btn"
                            @click="saveCookieConfig"
                            :disabled="cookieSettingsSaving"
                          >
                            <Icon v-if="cookieSettingsSaving" icon="mdi:loading" class="spin btn-icon" />
                            <Icon v-else icon="mdi:content-save" class="btn-icon" />
                            {{ cookieSettingsSaving ? '保存中...' : '保存Cookie设置' }}
                          </button>
                        </div>

                        <!-- Cookie保存状态提示 -->
                        <div v-if="cookieSettingsSaving" class="saving-indicator">
                          <Icon icon="mdi:loading" class="spin" />
                          <span>正在保存Cookie设置...</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </Transition>

              <BorderBeam
                  v-if="!isMusicSettingsCollapsed"
                  :size="200"
                  :duration="16"
                  :delay="8"
                  :border-width="1.5"
                  color-from="#ec4899"
                  color-to="#be185d"
              />
            </div>
          </ClientOnly>
        </div>

        <!-- 服务器设置 -->
        <div class="settings-item server-settings-item">
          <ClientOnly>
            <div class="settings-wrapper">
              <div class="item-header" @click="isServerSettingsCollapsed = !isServerSettingsCollapsed">
                <div class="header-content">
                  <Icon icon="mdi:server" class="header-icon" />
                  <div>
                    <h2 class="item-title">服务器设置</h2>
                    <p class="item-description">配置SSH服务器连接</p>
                  </div>
                </div>
                <div class="header-actions">
                  <button
                    v-if="!showAddServerForm && !showEditServerForm && !isServerSettingsCollapsed"
                    class="add-server-btn"
                    @click.stop="showAddServerForm = true"
                  >
                    <Icon icon="mdi:plus" class="btn-icon" />
                    添加服务器
                  </button>
                  <button class="collapse-btn" :class="{ collapsed: isServerSettingsCollapsed }">
                    <Icon icon="mdi:chevron-down" class="collapse-icon" />
                  </button>
                </div>
              </div>

              <Transition
                name="expand"
                mode="out-in"
                @enter="onEnter"
                @after-enter="onAfterEnter"
                @leave="onLeave"
                @after-leave="onAfterLeave"
              >
                <div v-if="!isServerSettingsCollapsed" class="item-content">
                  <div v-if="serverSettingsLoading" class="loading-state compact">
                    <Icon icon="mdi:loading" class="loading-icon spin" />
                    <p>加载中...</p>
                  </div>

                  <!-- 添加服务器表单 -->
                  <div v-else-if="showAddServerForm || showEditServerForm" class="server-form">
                    <div class="form-header">
                      <h3 class="form-title">{{ showEditServerForm ? '编辑服务器' : '添加服务器' }}</h3>
                      <p class="form-description">配置SSH服务器连接信息</p>
                    </div>

                    <div class="form-grid">
                      <!-- 基本信息 -->
                      <div class="form-section">
                        <h4 class="section-title">基本信息</h4>

                        <div class="form-group">
                          <label class="form-label">服务器名称</label>
                          <input
                            v-model="serverForm.serverName"
                            type="text"
                            class="form-input"
                            placeholder="请输入服务器名称"
                            :disabled="serverSettingsSaving"
                          />
                        </div>

                        <div class="form-row">
                          <div class="form-group">
                            <label class="form-label">服务器地址</label>
                            <input
                              v-model="serverForm.host"
                              type="text"
                              class="form-input"
                              placeholder="IP地址或域名"
                              :disabled="serverSettingsSaving"
                            />
                          </div>
                          <div class="form-group">
                            <label class="form-label">端口</label>
                            <input
                              v-model="serverForm.port"
                              type="number"
                              class="form-input"
                              placeholder="22"
                              min="1"
                              max="65535"
                              :disabled="serverSettingsSaving"
                            />
                          </div>
                        </div>

                        <div class="form-group">
                          <label class="form-label">用户名</label>
                          <input
                            v-model="serverForm.username"
                            type="text"
                            class="form-input"
                            placeholder="SSH用户名"
                            :disabled="serverSettingsSaving"
                          />
                        </div>

                        <div class="form-group">
                          <label class="form-label">服务器分组</label>
                          <div class="custom-select-wrapper">
                            <div
                              class="custom-select"
                              :class="{
                                'is-open': showServerGroupDropdown,
                                'is-disabled': serverSettingsSaving,
                                'has-value': serverForm.groupName
                              }"
                              @click="!serverSettingsSaving && (showServerGroupDropdown = !showServerGroupDropdown)"
                            >
                              <div class="select-display">
                                <span v-if="!serverForm.groupName" class="placeholder">
                                  请选择分组
                                </span>
                                <span v-else class="selected-value">
                                  {{ serverForm.groupName }}
                                </span>
                              </div>
                              <Icon
                                icon="mdi:chevron-down"
                                class="select-arrow"
                                :class="{ 'is-open': showServerGroupDropdown }"
                              />
                            </div>

                            <Transition name="dropdown">
                              <div v-if="showServerGroupDropdown" class="select-dropdown">
                                <div class="dropdown-content">
                                  <div
                                    v-for="groupName in availableServerGroups"
                                    :key="groupName"
                                    class="dropdown-item"
                                    :class="{ 'is-selected': serverForm.groupName === groupName }"
                                    @click="selectServerGroupOption(groupName)"
                                  >
                                    <div class="item-content">
                                      <Icon icon="mdi:folder" class="item-icon" />
                                      <span class="item-name">{{ groupName }}</span>
                                    </div>
                                    <Icon v-if="serverForm.groupName === groupName" icon="mdi:check" class="check-icon" />
                                  </div>
                                </div>
                              </div>
                            </Transition>
                          </div>
                          <p class="form-hint">
                            选择服务器所属的分组，可在"服务器分组管理"中创建新分组
                          </p>
                        </div>

                        <div class="form-group">
                          <label class="form-label">描述</label>
                          <textarea
                            v-model="serverForm.description"
                            class="form-textarea"
                            placeholder="服务器描述（可选）"
                            rows="2"
                            :disabled="serverSettingsSaving"
                          ></textarea>
                        </div>
                      </div>

                      <!-- 认证信息 -->
                      <div class="form-section">
                        <h4 class="section-title">认证方式</h4>

                        <div class="auth-type-selector">
                          <label class="radio-option" :class="{ active: serverForm.authType === 'password' }">
                            <input
                              type="radio"
                              v-model="serverForm.authType"
                              value="password"
                              :disabled="serverSettingsSaving"
                            />
                            <div class="radio-content">
                              <Icon icon="mdi:key" class="option-icon" />
                              <span class="option-title">密码认证</span>
                            </div>
                          </label>

                          <label class="radio-option" :class="{ active: serverForm.authType === 'publickey' }">
                            <input
                              type="radio"
                              v-model="serverForm.authType"
                              value="publickey"
                              :disabled="serverSettingsSaving"
                            />
                            <div class="radio-content">
                              <Icon icon="mdi:key-variant" class="option-icon" />
                              <span class="option-title">公钥认证</span>
                            </div>
                          </label>
                        </div>

                        <!-- 密码认证 -->
                        <div v-if="serverForm.authType === 'password'" class="auth-fields">
                          <div class="form-group">
                            <label class="form-label">密码</label>
                            <input
                              v-model="serverForm.password"
                              type="password"
                              class="form-input"
                              placeholder="SSH登录密码"
                              :disabled="serverSettingsSaving"
                            />
                          </div>
                        </div>

                        <!-- 公钥认证 -->
                        <div v-if="serverForm.authType === 'publickey'" class="auth-fields">
                          <!-- 私钥选择方式 -->
                          <div class="private-key-selector">
                            <div class="selector-header">
                              <button
                                type="button"
                                class="manage-keys-btn"
                                @click="showPrivateKeyManager = true"
                                :disabled="serverSettingsSaving"
                              >
                                <Icon icon="mdi:key-plus" class="btn-icon" />
                                管理私钥
                              </button>
                            </div>

                            <div class="key-source-options">
                              <label class="radio-option" :class="{ active: !serverForm.useExistingKey }">
                                <input
                                  type="radio"
                                  v-model="serverForm.useExistingKey"
                                  :value="false"
                                  :disabled="serverSettingsSaving"
                                />
                                <div class="radio-content">
                                  <Icon icon="mdi:text-box" class="option-icon" />
                                  <span class="option-title">手动输入私钥</span>
                                </div>
                              </label>

                              <label class="radio-option" :class="{ active: serverForm.useExistingKey }">
                                <input
                                  type="radio"
                                  v-model="serverForm.useExistingKey"
                                  :value="true"
                                  :disabled="serverSettingsSaving"
                                />
                                <div class="radio-content">
                                  <Icon icon="mdi:key-chain" class="option-icon" />
                                  <span class="option-title">选择已保存的私钥</span>
                                </div>
                              </label>
                            </div>
                          </div>

                          <!-- 选择已保存的私钥 -->
                          <div v-if="serverForm.useExistingKey" class="existing-key-selector">
                            <div class="form-group">
                              <label class="form-label">选择私钥</label>
                              <div class="custom-select-wrapper">
                                <div
                                  class="custom-select"
                                  :class="{
                                    'is-open': showPrivateKeyDropdown,
                                    'is-disabled': serverSettingsSaving,
                                    'has-value': serverForm.selectedPrivateKeyId
                                  }"
                                  @click="!serverSettingsSaving && (showPrivateKeyDropdown = !showPrivateKeyDropdown)"
                                >
                                  <div class="select-display">
                                    <span v-if="!serverForm.selectedPrivateKeyId" class="placeholder">
                                      请选择私钥
                                    </span>
                                    <span v-else class="selected-value">
                                      {{ getSelectedPrivateKeyName() }}
                                    </span>
                                  </div>
                                  <Icon
                                    icon="mdi:chevron-down"
                                    class="select-arrow"
                                    :class="{ 'is-open': showPrivateKeyDropdown }"
                                  />
                                </div>

                                <Transition name="dropdown">
                                  <div v-if="showPrivateKeyDropdown" class="select-dropdown">
                                    <div class="dropdown-content">
                                      <div
                                        class="dropdown-item"
                                        :class="{ 'is-selected': !serverForm.selectedPrivateKeyId }"
                                        @click="selectPrivateKeyOption('')"
                                      >
                                        <span class="item-text">请选择私钥</span>
                                      </div>
                                      <div
                                        v-for="key in privateKeys"
                                        :key="key.id"
                                        class="dropdown-item"
                                        :class="{ 'is-selected': serverForm.selectedPrivateKeyId === key.id }"
                                        @click="selectPrivateKeyOption(key.id)"
                                      >
                                        <div class="item-content">
                                          <span class="item-name">{{ key.keyName }}</span>
                                          <span v-if="key.description" class="item-description">{{ key.description }}</span>
                                          <span v-if="key.keyType" class="item-type">{{ key.keyType }}</span>
                                        </div>
                                        <Icon v-if="serverForm.selectedPrivateKeyId === key.id" icon="mdi:check" class="check-icon" />
                                      </div>
                                    </div>
                                  </div>
                                </Transition>
                              </div>
                            </div>
                          </div>

                          <!-- 手动输入私钥 -->
                          <div v-else class="manual-key-input">
                            <div class="form-group">
                              <label class="form-label">私钥</label>
                              <textarea
                                v-model="serverForm.privateKey"
                                class="form-textarea"
                                placeholder="请粘贴私钥内容（PEM格式）"
                                rows="8"
                                :disabled="serverSettingsSaving"
                              ></textarea>
                              <p class="form-hint">
                                支持RSA、ECDSA、Ed25519等格式的私钥
                              </p>
                            </div>

                            <div class="form-group">
                              <label class="form-label">私钥密码（可选）</label>
                              <input
                                v-model="serverForm.privateKeyPassword"
                                type="password"
                                class="form-input"
                                placeholder="如果私钥有密码保护，请输入"
                                :disabled="serverSettingsSaving"
                              />
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>

                    <!-- 表单操作按钮 -->
                    <div class="form-actions">
                      <div class="action-left">
                        <button
                          class="test-btn"
                          @click="testServerConnection"
                          :disabled="serverSettingsSaving"
                        >
                          <Icon icon="mdi:connection" class="btn-icon" />
                          测试连接
                        </button>
                      </div>

                      <div class="action-right">
                        <button
                          class="cancel-btn"
                          @click="cancelServerEdit"
                          :disabled="serverSettingsSaving"
                        >
                          取消
                        </button>
                        <button
                          class="save-btn"
                          @click="saveServerConfig"
                          :disabled="serverSettingsSaving || !serverForm.serverName.trim() || !serverForm.host.trim() || !serverForm.username.trim()"
                        >
                          <Icon v-if="serverSettingsSaving" icon="mdi:loading" class="spin btn-icon" />
                          <Icon v-else icon="mdi:check" class="btn-icon" />
                          {{ serverSettingsSaving ? '保存中...' : '保存' }}
                        </button>
                      </div>
                    </div>
                  </div>

                  <!-- 服务器列表 -->
                  <div v-else class="server-list">
                    <div v-if="serverConfigs.length === 0" class="empty-state compact">
                      <Icon icon="mdi:server-off" class="empty-icon" />
                      <p>暂无服务器配置</p>
                      <button class="add-first-btn" @click="showAddServerForm = true">
                        <Icon icon="mdi:plus" class="btn-icon" />
                        添加第一个服务器
                      </button>
                    </div>

                    <div v-else class="servers-container">
                      <div
                        v-for="groupName in groupNames"
                        :key="groupName"
                        class="server-group"
                      >
                        <div class="group-header">
                          <div class="group-info">
                            <Icon icon="mdi:folder" class="group-icon" />
                            <h3 class="group-name">{{ groupName }}</h3>
                            <span class="group-count">({{ groupedServers[groupName].length }})</span>
                          </div>
                        </div>

                        <VueDraggable
                          v-model="groupedServers[groupName]"
                          :animation="200"
                          ghost-class="ghost"
                          chosen-class="chosen"
                          drag-class="drag"
                          @end="onServerDragEnd(groupName, groupedServers[groupName])"
                          class="servers-grid"
                        >
                          <div
                            v-for="server in groupedServers[groupName]"
                            :key="server.id"
                            class="server-card"
                            :class="{ 'is-default': server.isDefault }"
                          >
                            <div class="server-header">
                              <div class="server-info">
                                <h4 class="server-name">{{ server.serverName }}</h4>
                                <p class="server-address">{{ server.username }}@{{ server.host }}:{{ server.port }}</p>
                              </div>
                              <div class="server-badges">
                                <span v-if="server.isDefault" class="default-badge">默认</span>
                                <span class="auth-badge" :class="server.authType">
                                  <Icon :icon="server.authType === 'password' ? 'mdi:key' : 'mdi:key-variant'" />
                                  {{ server.authType === 'password' ? '密码' : '公钥' }}
                                </span>
                                <div class="drag-handle" title="拖拽排序">
                                  <Icon icon="mdi:drag" />
                                </div>
                              </div>
                            </div>

                            <div v-if="server.description" class="server-description">
                              {{ server.description }}
                            </div>

                            <div class="server-actions">
                              <button
                                v-if="!server.isDefault"
                                class="set-default-btn"
                                @click="setDefaultServer(server.id)"
                                title="设为默认"
                              >
                                <Icon icon="mdi:star-outline" />
                              </button>
                              <button
                                class="edit-btn"
                                @click="startEditServer(server)"
                                title="编辑"
                              >
                                <Icon icon="mdi:pencil" />
                              </button>
                              <button
                                class="delete-btn"
                                @click="showDeleteServerConfirmDialog(server)"
                                title="删除"
                                :disabled="server.isDefault"
                              >
                                <Icon icon="mdi:delete" />
                              </button>
                            </div>
                          </div>
                        </VueDraggable>
                      </div>
                    </div>
                  </div>
                </div>
              </Transition>

              <BorderBeam
                  v-if="!isServerSettingsCollapsed"
                  :size="220"
                  :duration="14"
                  :delay="7"
                  :border-width="1.5"
                  color-from="#06b6d4"
                  color-to="#0891b2"
              />
            </div>
          </ClientOnly>
        </div>

        <!-- 私钥管理器模态框 -->
        <div v-if="showPrivateKeyManager" class="modal-overlay" @click="showPrivateKeyManager = false">
          <div class="private-key-manager-modal" @click.stop>
            <div class="modal-header">
              <h3 class="modal-title">
                <Icon icon="mdi:key-variant" class="title-icon" />
                私钥管理
              </h3>
              <button class="modal-close-btn" @click="showPrivateKeyManager = false">
                <Icon icon="mdi:close" />
              </button>
            </div>

            <div class="modal-content">
              <!-- 私钥表单 -->
              <div v-if="showAddPrivateKeyForm || showEditPrivateKeyForm" class="private-key-form">
                <div class="form-header">
                  <h4 class="form-title">{{ showEditPrivateKeyForm ? '编辑私钥' : '添加私钥' }}</h4>
                </div>

                <div class="form-group">
                  <label class="form-label">私钥名称</label>
                  <input
                    v-model="privateKeyForm.keyName"
                    type="text"
                    class="form-input"
                    placeholder="请输入私钥名称"
                    :disabled="privateKeySaving"
                  />
                </div>

                <div class="form-group">
                  <label class="form-label">私钥类型（可选）</label>
                  <div class="custom-select-wrapper">
                    <div
                      class="custom-select"
                      :class="{
                        'is-open': showKeyTypeDropdown,
                        'is-disabled': privateKeySaving,
                        'has-value': privateKeyForm.keyType
                      }"
                      @click="!privateKeySaving && (showKeyTypeDropdown = !showKeyTypeDropdown)"
                    >
                      <div class="select-display">
                        <span v-if="!privateKeyForm.keyType" class="placeholder">
                          自动检测
                        </span>
                        <span v-else class="selected-value">
                          {{ privateKeyForm.keyType }}
                        </span>
                      </div>
                      <Icon
                        icon="mdi:chevron-down"
                        class="select-arrow"
                        :class="{ 'is-open': showKeyTypeDropdown }"
                      />
                    </div>

                    <Transition name="dropdown">
                      <div v-if="showKeyTypeDropdown" class="select-dropdown">
                        <div class="dropdown-content">
                          <div
                            class="dropdown-item"
                            :class="{ 'is-selected': !privateKeyForm.keyType }"
                            @click="selectKeyTypeOption('')"
                          >
                            <span class="item-text">自动检测</span>
                            <Icon v-if="!privateKeyForm.keyType" icon="mdi:check" class="check-icon" />
                          </div>
                          <div
                            v-for="type in ['RSA', 'ECDSA', 'Ed25519']"
                            :key="type"
                            class="dropdown-item"
                            :class="{ 'is-selected': privateKeyForm.keyType === type }"
                            @click="selectKeyTypeOption(type)"
                          >
                            <span class="item-text">{{ type }}</span>
                            <Icon v-if="privateKeyForm.keyType === type" icon="mdi:check" class="check-icon" />
                          </div>
                        </div>
                      </div>
                    </Transition>
                  </div>
                </div>

                <div class="form-group">
                  <label class="form-label">私钥内容</label>
                  <textarea
                    v-model="privateKeyForm.privateKey"
                    class="form-textarea"
                    placeholder="请粘贴私钥内容（PEM格式）"
                    rows="10"
                    :disabled="privateKeySaving"
                  ></textarea>
                </div>

                <div class="form-group">
                  <label class="form-label">私钥密码（可选）</label>
                  <input
                    v-model="privateKeyForm.privateKeyPassword"
                    type="password"
                    class="form-input"
                    placeholder="如果私钥有密码保护，请输入"
                    :disabled="privateKeySaving"
                  />
                </div>

                <div class="form-group">
                  <label class="form-label">描述（可选）</label>
                  <input
                    v-model="privateKeyForm.description"
                    type="text"
                    class="form-input"
                    placeholder="私钥用途描述"
                    :disabled="privateKeySaving"
                  />
                </div>

                <div class="form-actions">
                  <button
                    class="cancel-btn"
                    @click="cancelPrivateKeyEdit"
                    :disabled="privateKeySaving"
                  >
                    取消
                  </button>
                  <button
                    class="save-btn"
                    @click="savePrivateKey"
                    :disabled="privateKeySaving || !privateKeyForm.keyName.trim() || !privateKeyForm.privateKey.trim()"
                  >
                    <Icon v-if="privateKeySaving" icon="mdi:loading" class="spin btn-icon" />
                    <Icon v-else icon="mdi:check" class="btn-icon" />
                    {{ privateKeySaving ? '保存中...' : '保存' }}
                  </button>
                </div>
              </div>

              <!-- 私钥列表 -->
              <div v-else class="private-key-list">
                <div class="list-header">
                  <h4 class="list-title">已保存的私钥</h4>
                  <button
                    class="add-key-btn"
                    @click="showAddPrivateKeyForm = true"
                  >
                    <Icon icon="mdi:plus" class="btn-icon" />
                    添加私钥
                  </button>
                </div>

                <div v-if="privateKeysLoading" class="loading-state">
                  <Icon icon="mdi:loading" class="loading-icon spin" />
                  <p>加载中...</p>
                </div>

                <div v-else-if="privateKeys.length === 0" class="empty-state">
                  <Icon icon="mdi:key-off" class="empty-icon" />
                  <p>暂无私钥配置</p>
                  <button class="add-first-btn" @click="showAddPrivateKeyForm = true">
                    <Icon icon="mdi:plus" class="btn-icon" />
                    添加第一个私钥
                  </button>
                </div>

                <div v-else class="keys-list">
                  <div
                    v-for="key in privateKeys"
                    :key="key.id"
                    class="key-item"
                  >
                    <div class="key-info">
                      <h5 class="key-name">{{ key.keyName }}</h5>
                      <p v-if="key.description" class="key-description">{{ key.description }}</p>
                      <div class="key-meta">
                        <span v-if="key.keyType" class="key-type">{{ key.keyType }}</span>
                      </div>
                    </div>
                    <div class="key-actions">
                      <button
                        class="edit-btn"
                        @click="startEditPrivateKey(key)"
                        title="编辑"
                      >
                        <Icon icon="mdi:pencil" />
                      </button>
                      <button
                        class="delete-btn"
                        @click="showDeletePrivateKeyConfirmDialog(key)"
                        title="删除"
                      >
                        <Icon icon="mdi:delete" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
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

              <Transition
                name="expand"
                mode="out-in"
                @enter="onEnter"
                @after-enter="onAfterEnter"
                @leave="onLeave"
                @after-leave="onAfterLeave"
              >
                <div v-if="!isBackupRestoreCollapsed" class="item-content">
                <div class="coming-soon">
                  <Icon icon="mdi:database" class="coming-soon-icon" />
                  <p>备份恢复功能即将推出...</p>
                </div>
              </div>
              </Transition>

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

              <Transition
                name="expand"
                mode="out-in"
                @enter="onEnter"
                @after-enter="onAfterEnter"
                @leave="onLeave"
                @after-leave="onAfterLeave"
              >
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
              </Transition>

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

    <!-- 导航分组删除确认对话框 -->
    <div v-if="showDeleteNavigationConfirm" class="modal-overlay" @click="cancelDeleteNavigationCategory">
      <div class="delete-confirm-dialog" @click.stop>
        <div class="dialog-header">
          <Icon icon="mdi:alert-circle" class="warning-icon" />
          <h3 class="dialog-title">确认删除导航分组</h3>
        </div>

        <div class="dialog-content">
          <p class="dialog-message">
            您确定要删除导航分组 <strong>"{{ deletingNavigationCategory?.name }}"</strong> 吗？
          </p>
          <p class="dialog-warning">
            此操作不可撤销，该分组下的所有导航项也将被删除。
          </p>
        </div>

        <div class="dialog-actions">
          <button
            class="dialog-cancel-btn"
            @click="cancelDeleteNavigationCategory"
            :disabled="deleteNavigationCategoryLoading"
          >
            取消
          </button>
          <button
            class="dialog-confirm-btn"
            @click="confirmDeleteNavigationCategory"
            :disabled="deleteNavigationCategoryLoading"
          >
            <Icon v-if="deleteNavigationCategoryLoading" icon="mdi:loading" class="spin btn-icon" />
            <Icon v-else icon="mdi:delete" class="btn-icon" />
            {{ deleteNavigationCategoryLoading ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 服务器分组删除确认对话框 -->
    <div v-if="showDeleteServerCategoryConfirm" class="modal-overlay" @click="cancelDeleteServerCategory">
      <div class="delete-confirm-dialog" @click.stop>
        <div class="dialog-header">
          <Icon icon="mdi:alert-circle" class="warning-icon" />
          <h3 class="dialog-title">确认删除服务器分组</h3>
        </div>

        <div class="dialog-content">
          <p class="dialog-message">
            您确定要删除服务器分组 <strong>"{{ deletingServerCategory?.name }}"</strong> 吗？
          </p>
          <p class="dialog-warning">
            此操作不可撤销，该分组下的所有服务器也将被移动到默认分组。
          </p>
        </div>

        <div class="dialog-actions">
          <button
            class="dialog-cancel-btn"
            @click="cancelDeleteServerCategory"
            :disabled="deleteServerCategoryLoading"
          >
            取消
          </button>
          <button
            class="dialog-confirm-btn"
            @click="confirmDeleteServerCategory"
            :disabled="deleteServerCategoryLoading"
          >
            <Icon v-if="deleteServerCategoryLoading" icon="mdi:loading" class="spin btn-icon" />
            <Icon v-else icon="mdi:delete" class="btn-icon" />
            {{ deleteServerCategoryLoading ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 服务器配置删除确认对话框 -->
    <div v-if="showDeleteServerConfirm" class="modal-overlay" @click="cancelDeleteServer">
      <div class="delete-confirm-dialog" @click.stop>
        <div class="dialog-header">
          <Icon icon="mdi:alert-circle" class="warning-icon" />
          <h3 class="dialog-title">确认删除服务器配置</h3>
        </div>

        <div class="dialog-content">
          <p class="dialog-message">
            您确定要删除服务器配置 <strong>"{{ deletingServer?.serverName }}"</strong> 吗？
          </p>
          <p class="dialog-warning">
            此操作不可撤销，服务器配置信息将被永久删除。
          </p>
        </div>

        <div class="dialog-actions">
          <button
            class="dialog-cancel-btn"
            @click="cancelDeleteServer"
            :disabled="deleteServerLoading"
          >
            取消
          </button>
          <button
            class="dialog-confirm-btn"
            @click="confirmDeleteServer"
            :disabled="deleteServerLoading"
          >
            <Icon v-if="deleteServerLoading" icon="mdi:loading" class="spin btn-icon" />
            <Icon v-else icon="mdi:delete" class="btn-icon" />
            {{ deleteServerLoading ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 私钥删除确认对话框 -->
    <div v-if="showDeletePrivateKeyConfirm" class="modal-overlay" @click="cancelDeletePrivateKey">
      <div class="delete-confirm-dialog" @click.stop>
        <div class="dialog-header">
          <Icon icon="mdi:alert-circle" class="warning-icon" />
          <h3 class="dialog-title">确认删除私钥</h3>
        </div>

        <div class="dialog-content">
          <p class="dialog-message">
            您确定要删除私钥 <strong>"{{ deletingPrivateKey?.keyName }}"</strong> 吗？
          </p>
          <p class="dialog-warning">
            此操作不可撤销，私钥配置信息将被永久删除。
          </p>
        </div>

        <div class="dialog-actions">
          <button
            class="dialog-cancel-btn"
            @click="cancelDeletePrivateKey"
            :disabled="deletePrivateKeyLoading"
          >
            取消
          </button>
          <button
            class="dialog-confirm-btn"
            @click="confirmDeletePrivateKey"
            :disabled="deletePrivateKeyLoading"
          >
            <Icon v-if="deletePrivateKeyLoading" icon="mdi:loading" class="spin btn-icon" />
            <Icon v-else icon="mdi:delete" class="btn-icon" />
            {{ deletePrivateKeyLoading ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>
  </NuxtLayout>
</template>

<style scoped>

</style>