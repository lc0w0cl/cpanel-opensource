<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { VueDraggable } from 'vue-draggable-plus'
import { Icon } from '@iconify/vue'

// 子页面不需要定义 layout 和 middleware，由父页面处理

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
    const groupName = server.groupName || '默认分组'
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
  return Object.keys(groupedServers.value).sort()
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
                      isMusicSettingsCollapsed.value &&
                      isServerSettingsCollapsed.value &&
                      isBackupRestoreCollapsed.value &&
                      isSystemInfoCollapsed.value

  const newState = !allCollapsed

  isGroupManagementCollapsed.value = newState
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
  return isGroupManagementCollapsed.value &&
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
    const url = editingServerId.value
      ? `${API_BASE_URL}/servers/${editingServerId.value}`
      : `${API_BASE_URL}/servers`

    const method = editingServerId.value ? 'PUT' : 'POST'

    const response = await apiRequest(url, {
      method: method,
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(serverForm.value)
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
    groupName: server.groupName || '',
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

// 点击外部关闭下拉框
const handleClickOutside = (event) => {
  const selectWrapper = event.target.closest('.custom-select-wrapper')
  if (!selectWrapper) {
    showPrivateKeyDropdown.value = false
    showKeyTypeDropdown.value = false
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
  fetchCategories()
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

          <Transition
            name="expand"
            mode="out-in"
            @enter="onEnter"
            @after-enter="onAfterEnter"
            @leave="onLeave"
            @after-leave="onAfterLeave"
          >
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
          </Transition>

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
                          <input
                            v-model="serverForm.groupName"
                            type="text"
                            class="form-input"
                            placeholder="例如：生产环境、测试环境、美国服务器等"
                            :disabled="serverSettingsSaving"
                          />
                          <p class="form-hint">
                            用于在终端页面中对服务器进行分组显示，如果不填写将归入"默认分组"
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

    <!-- 服务器删除确认对话框 -->
    <div v-if="showDeleteServerConfirm" class="modal-overlay" @click="cancelDeleteServer">
      <div class="delete-confirm-dialog" @click.stop>
        <div class="dialog-header">
          <Icon icon="mdi:alert-circle" class="warning-icon" />
          <h3 class="dialog-title">确认删除服务器</h3>
        </div>

        <div class="dialog-content">
          <p class="dialog-message">
            您确定要删除服务器 <strong>"{{ deletingServer?.serverName }}"</strong> 吗？
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
.settings-container {
  min-height: 100vh;
  padding: 2rem;
  max-width: 100%;
  /* 优化渲染性能 */
  contain: layout style paint;
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
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: 1.5rem;
  align-items: start;
}

/* 设置项 */
.settings-item {
  border-radius: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  overflow: hidden;
  transition: border-color 0.3s ease, transform 0.2s ease;
  height: fit-content;
  /* 性能优化 */
  contain: layout style;
  transform: translateZ(0);
  backface-visibility: hidden;
}

.settings-item:hover {
  border-color: rgba(255, 255, 255, 0.2);
}

/* 设置项特殊样式 */
.password-settings-item,
.group-management-item,
.system-config-item,
.music-settings-item,
.backup-restore-item,
.system-info-item {
  position: relative;
}

/* 服务器设置项 - 扩大宽度 */
.server-settings-item {
  position: relative;
  grid-column: 1 / -1; /* 占据整行 */
}

.server-settings-item .settings-wrapper {
  max-width: none; /* 移除最大宽度限制 */
}

.server-settings-item .item-content {
  //padding: 2rem; /* 增加内边距 */
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

/* 音乐设置项 - 扩大宽度，和主题设置一样长 */
.music-settings-item {
  position: relative;
  grid-column: 1 / -1; /* 占据整行 */
}

.music-settings-item .settings-wrapper {
  max-width: none; /* 移除最大宽度限制 */
}

.music-settings-item .item-content {
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
  //padding: 0 1.5rem 1.5rem 1.5rem;
  overflow: hidden;
  transform-origin: top;
  /* 启用硬件加速 */
  transform: translateZ(0);
  backface-visibility: hidden;
  perspective: 1000px;
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

/* 展开/收起动画 - 高性能版本 */
.expand-enter-active {
  transition: height 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94),
              opacity 0.25s ease-out 0.1s,
              transform 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  overflow: hidden;
  will-change: height, opacity, transform;
  transform: translateZ(0); /* 启用硬件加速 */
}

.expand-leave-active {
  transition: height 0.3s cubic-bezier(0.55, 0.06, 0.68, 0.19),
              opacity 0.2s ease-in,
              transform 0.3s cubic-bezier(0.55, 0.06, 0.68, 0.19);
  overflow: hidden;
  will-change: height, opacity, transform;
  transform: translateZ(0); /* 启用硬件加速 */
}

.expand-enter-from {
  opacity: 0;
  transform: translateY(-6px) scale(0.99);
}

.expand-enter-to {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.expand-leave-from {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.expand-leave-to {
  opacity: 0;
  transform: translateY(-6px) scale(0.99);
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
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.form-label {
  font-size: 0.8rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.8);
}

.form-input {
  padding: 0.5rem 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.375rem;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.8rem;
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
  gap: 0.75rem;
  justify-content: flex-end;
  margin-top: 0.375rem;
}

.cancel-btn,
.save-btn {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 0.375rem;
  font-size: 0.8rem;
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

/* 配置区域样式 */
.config-section {
  margin-bottom: 2rem;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.section-icon {
  width: 1.25rem;
  height: 1.25rem;
  color: rgba(59, 130, 246, 0.8);
}

.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

/* Logo设置样式 */
.logo-settings {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.logo-preview {
  display: flex;
  justify-content: center;
}

.preview-container {
  width: 80px;
  height: 80px;
  border-radius: 0.75rem;
  border: 2px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
  overflow: hidden;
}

.logo-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.default-logo {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  color: rgba(255, 255, 255, 0.5);
}

.default-icon {
  width: 2rem;
  height: 2rem;
}

.default-logo span {
  font-size: 0.75rem;
}

.logo-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.upload-btn, .reset-btn {
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
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.upload-btn {
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.2) 0%,
    rgba(34, 197, 94, 0.1) 100%
  );
  color: rgba(34, 197, 94, 0.9);
  border-color: rgba(34, 197, 94, 0.3);
}

.upload-btn:hover:not(.disabled) {
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
  border-color: rgba(239, 68, 68, 0.3);
}

.reset-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.3) 0%,
    rgba(239, 68, 68, 0.15) 100%
  );
  border-color: rgba(239, 68, 68, 0.5);
  transform: translateY(-1px);
}

.upload-btn.disabled,
.reset-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.logo-tips {
  text-align: center;
}

.logo-tips p {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
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

  /* 移动端音乐设置优化 */
  .path-input-group {
    flex-direction: column;
    align-items: stretch;
  }

  .save-path-btn {
    justify-content: center;
  }
}

/* 音乐设置样式 */
.music-settings {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.download-location-settings {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.radio-group {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.radio-option {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 0.375rem;
  background: rgba(255, 255, 255, 0.02);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.radio-option:hover {
  border-color: rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.04);
  transform: translateY(-1px);
}

.radio-option.active {
  border-color: rgba(236, 72, 153, 0.5);
  background: linear-gradient(135deg,
    rgba(236, 72, 153, 0.15) 0%,
    rgba(236, 72, 153, 0.08) 100%
  );
}

.radio-option input[type="radio"] {
  appearance: none;
  width: 1rem;
  height: 1rem;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  background: transparent;
  cursor: pointer;
  transition: all 0.3s ease;
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.radio-option input[type="radio"]:checked {
  border-color: rgba(236, 72, 153, 0.8);
  background: radial-gradient(circle, rgba(236, 72, 153, 0.8) 30%, transparent 30%);
}

.radio-content {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  flex: 1;
}

.option-icon {
  width: 1.25rem;
  height: 1.25rem;
  color: rgba(236, 72, 153, 0.8);
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.option-text {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.option-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.option-description {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.3;
}

.server-path-settings {
  margin-top: 0.75rem;
  padding: 0.75rem;
  border: 1px solid rgba(236, 72, 153, 0.2);
  border-radius: 0.375rem;
  background: linear-gradient(135deg,
    rgba(236, 72, 153, 0.08) 0%,
    rgba(236, 72, 153, 0.04) 100%
  );
}

.path-input-group {
  display: flex;
  gap: 0.5rem;
  align-items: flex-end;
}

.path-input-group .form-input {
  flex: 1;
}

.save-path-btn {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid rgba(236, 72, 153, 0.3);
  border-radius: 0.375rem;
  background: linear-gradient(135deg,
    rgba(236, 72, 153, 0.2) 0%,
    rgba(236, 72, 153, 0.1) 100%
  );
  color: rgba(236, 72, 153, 0.9);
  font-size: 0.8rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.save-path-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(236, 72, 153, 0.3) 0%,
    rgba(236, 72, 153, 0.15) 100%
  );
  border-color: rgba(236, 72, 153, 0.5);
  transform: translateY(-1px);
}

.save-path-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.form-hint {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 0.5rem;
  margin-bottom: 0;
}

.saving-indicator {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.15) 0%,
    rgba(34, 197, 94, 0.08) 100%
  );
  color: rgba(34, 197, 94, 0.9);
  font-size: 0.875rem;
  border: 1px solid rgba(34, 197, 94, 0.3);
}

/* Cookie设置样式 */
.cookie-settings {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.cookie-group {
  padding: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 0.75rem;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.02) 0%,
    rgba(255, 255, 255, 0.01) 100%
  );
  transition: all 0.3s ease;
}

.cookie-group:hover {
  border-color: rgba(255, 255, 255, 0.2);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.04) 0%,
    rgba(255, 255, 255, 0.02) 100%
  );
}

.cookie-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.platform-icon {
  width: 1.5rem;
  height: 1.5rem;
  flex-shrink: 0;
}

.bilibili-icon {
  color: #00a1d6;
}

.youtube-icon {
  color: #ff0000;
}

.cookie-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.cookie-textarea {
  width: 100%;
  min-height: 80px;
  padding: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.5rem;
  background: rgba(0, 0, 0, 0.2);
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.875rem;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  resize: vertical;
  transition: all 0.3s ease;
}

.cookie-textarea:focus {
  outline: none;
  border-color: rgba(59, 130, 246, 0.5);
  background: rgba(0, 0, 0, 0.3);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.cookie-textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cookie-textarea::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.cookie-actions {
  display: flex;
  justify-content: center;
  margin-top: 1.5rem;
}

.save-cookie-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.875rem 1.5rem;
  border: none;
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.2) 0%,
    rgba(59, 130, 246, 0.1) 100%
  );
  color: rgba(59, 130, 246, 0.9);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.save-cookie-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.3) 0%,
    rgba(59, 130, 246, 0.15) 100%
  );
  border-color: rgba(59, 130, 246, 0.5);
  transform: translateY(-1px);
}

.save-cookie-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* 服务器设置样式 */
.server-form {
  background: rgba(255, 255, 255, 0.02);
  border-radius: 0.75rem;
  padding: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.form-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 2.5rem;
  margin-top: 1rem;
}

@media (max-width: 1024px) {
  .form-grid {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }
}

.form-section {
  background: rgba(255, 255, 255, 0.02);
  border-radius: 0.375rem;
  padding: 1rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 1rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.form-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 0.75rem;
}

@media (max-width: 640px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}

.form-textarea {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.375rem;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.8rem;
  resize: vertical;
  min-height: 60px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.form-textarea:focus {
  outline: none;
  border-color: rgba(59, 130, 246, 0.5);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-textarea::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.auth-type-selector {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.auth-fields {
  margin-top: 0.75rem;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.action-left,
.action-right {
  display: flex;
  gap: 0.5rem;
}

.test-btn {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border: 1px solid rgba(34, 197, 94, 0.3);
  border-radius: 0.375rem;
  background: rgba(34, 197, 94, 0.1);
  color: rgba(34, 197, 94, 0.9);
  font-size: 0.8rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.test-btn:hover:not(:disabled) {
  background: rgba(34, 197, 94, 0.2);
  border-color: rgba(34, 197, 94, 0.5);
  transform: translateY(-1px);
}

.test-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.server-list {
  margin-top: 1rem;
}

/* 服务器容器和分组样式 */
.servers-container {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.server-group {
  background: rgba(255, 255, 255, 0.01);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 0.5rem;
  padding: 1rem;
}

.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.group-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.group-icon {
  font-size: 1.25rem;
  color: rgba(168, 85, 247, 0.8);
}

.group-name {
  font-size: 1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.group-count {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.1);
  padding: 0.125rem 0.5rem;
  border-radius: 0.75rem;
}

.servers-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1rem;
  max-width: none;
}

.server-card {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 0.5rem;
  padding: 1rem;
  transition: all 0.3s ease;
  position: relative;
  min-height: 100px;
}

.server-card:hover {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

/* 拖拽相关样式 */
.drag-handle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.5rem;
  height: 1.5rem;
  color: rgba(255, 255, 255, 0.4);
  cursor: grab;
  border-radius: 0.25rem;
  transition: all 0.3s ease;
}

.drag-handle:hover {
  color: rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.1);
}

.drag-handle:active {
  cursor: grabbing;
}

/* 拖拽状态样式 */
.ghost {
  opacity: 0.5;
  background: rgba(59, 130, 246, 0.1);
  border: 2px dashed rgba(59, 130, 246, 0.3);
}

.chosen {
  transform: rotate(5deg);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.3);
}

.drag {
  transform: rotate(5deg);
  opacity: 0.8;
}

.server-card.is-default {
  border-color: rgba(59, 130, 246, 0.4);
  background: rgba(59, 130, 246, 0.05);
}

.server-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.5rem;
}

.server-info {
  flex: 1;
}

.server-name {
  font-size: 0.9rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 0.125rem 0;
}

.server-address {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.server-badges {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  align-items: flex-end;
}

.default-badge {
  padding: 0.125rem 0.375rem;
  border-radius: 0.25rem;
  background: rgba(59, 130, 246, 0.2);
  color: rgba(59, 130, 246, 0.9);
  font-size: 0.7rem;
  font-weight: 500;
}

.auth-badge {
  display: flex;
  align-items: center;
  gap: 0.125rem;
  padding: 0.125rem 0.375rem;
  border-radius: 0.25rem;
  font-size: 0.7rem;
  font-weight: 500;
}

.auth-badge.password {
  background: rgba(34, 197, 94, 0.2);
  color: rgba(34, 197, 94, 0.9);
}

.auth-badge.publickey {
  background: rgba(168, 85, 247, 0.2);
  color: rgba(168, 85, 247, 0.9);
}

.group-badge {
  display: flex;
  align-items: center;
  gap: 0.125rem;
  padding: 0.125rem 0.375rem;
  background: rgba(168, 85, 247, 0.2);
  color: rgba(168, 85, 247, 0.9);
  border-radius: 0.25rem;
  font-size: 0.7rem;
  font-weight: 500;
}

.server-description {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 0.75rem;
  line-height: 1.3;
}

.server-actions {
  display: flex;
  gap: 0.375rem;
  justify-content: flex-end;
}

.server-actions button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  border: none;
  border-radius: 0.25rem;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 0.8rem;
}

.set-default-btn {
  background: rgba(255, 193, 7, 0.1);
  color: rgba(255, 193, 7, 0.8);
  border: 1px solid rgba(255, 193, 7, 0.2);
}

.set-default-btn:hover {
  background: rgba(255, 193, 7, 0.2);
  border-color: rgba(255, 193, 7, 0.4);
}

.edit-btn {
  background: rgba(59, 130, 246, 0.1);
  color: rgba(59, 130, 246, 0.8);
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.edit-btn:hover {
  background: rgba(59, 130, 246, 0.2);
  border-color: rgba(59, 130, 246, 0.4);
}

.delete-btn {
  background: rgba(239, 68, 68, 0.1);
  color: rgba(239, 68, 68, 0.8);
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.delete-btn:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.4);
}

.delete-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.add-server-btn,
.add-first-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border: 1px solid rgba(34, 197, 94, 0.3);
  border-radius: 0.5rem;
  background: rgba(34, 197, 94, 0.1);
  color: rgba(34, 197, 94, 0.9);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.add-server-btn:hover,
.add-first-btn:hover {
  background: rgba(34, 197, 94, 0.2);
  border-color: rgba(34, 197, 94, 0.5);
  transform: translateY(-1px);
}

.add-first-btn {
  margin-top: 1rem;
}

/* 私钥管理样式 */
.private-key-selector {
  margin-bottom: 0.75rem;
}

.selector-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 0.75rem;
}

.manage-keys-btn {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border: 1px solid rgba(168, 85, 247, 0.3);
  border-radius: 0.25rem;
  background: rgba(168, 85, 247, 0.1);
  color: rgba(168, 85, 247, 0.9);
  font-size: 0.75rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.manage-keys-btn:hover:not(:disabled) {
  background: rgba(168, 85, 247, 0.2);
  border-color: rgba(168, 85, 247, 0.5);
}

.key-source-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.form-select {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.375rem;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.8rem;
}

.form-select:focus {
  outline: none;
  border-color: rgba(59, 130, 246, 0.5);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-select option {
  background: rgba(30, 30, 30, 0.95);
  color: rgba(255, 255, 255, 0.9);
}

/* 自定义下拉选择器样式 */
.custom-select-wrapper {
  position: relative;
}

.custom-select {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.375rem;
  background: rgba(255, 255, 255, 0.05);
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.custom-select:hover:not(.is-disabled) {
  border-color: rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.08);
}

.custom-select.is-open {
  border-color: rgba(59, 130, 246, 0.5);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.custom-select.is-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.select-display {
  flex: 1;
  display: flex;
  align-items: center;
}

.placeholder {
  color: rgba(255, 255, 255, 0.4);
  font-size: 0.8rem;
}

.selected-value {
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.8rem;
}

.select-arrow {
  font-size: 1.1rem;
  color: rgba(255, 255, 255, 0.6);
  transition: transform 0.3s ease;
}

.select-arrow.is-open {
  transform: rotate(180deg);
}

.select-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 1000;
  margin-top: 0.25rem;
}

.dropdown-content {
  background: rgba(20, 20, 20, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.5rem;
  backdrop-filter: blur(20px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.3);
  max-height: 200px;
  overflow-y: auto;
}

.dropdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem 0.75rem;
  cursor: pointer;
  transition: all 0.3s ease;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.dropdown-item:last-child {
  border-bottom: none;
}

.dropdown-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.dropdown-item.is-selected {
  background: rgba(59, 130, 246, 0.15);
  border-color: rgba(59, 130, 246, 0.3);
}

.item-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.item-text {
  color: rgba(255, 255, 255, 0.4);
  font-size: 0.75rem;
}

.item-name {
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.8rem;
  font-weight: 500;
}

.item-description {
  color: rgba(255, 255, 255, 0.6);
  font-size: 0.7rem;
}

.item-type {
  display: inline-block;
  padding: 0.0625rem 0.25rem;
  border-radius: 0.1875rem;
  background: rgba(168, 85, 247, 0.2);
  color: rgba(168, 85, 247, 0.9);
  font-size: 0.6rem;
  font-weight: 500;
  margin-top: 0.125rem;
  width: fit-content;
}

.check-icon {
  color: rgba(34, 197, 94, 0.8);
  font-size: 0.875rem;
}

/* 下拉动画 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.3s ease;
}

.dropdown-enter-from {
  opacity: 0;
  transform: translateY(-10px) scale(0.95);
}

.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.95);
}

/* 私钥管理器模态框 */
.private-key-manager-modal {
  background: rgba(20, 20, 20, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 1rem;
  width: 90vw;
  max-width: 800px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  backdrop-filter: blur(20px);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.25rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.title-icon {
  font-size: 1.5rem;
  color: rgba(168, 85, 247, 0.8);
}

.modal-close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border: none;
  border-radius: 0.375rem;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.3s ease;
}

.modal-close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.9);
}

.modal-content {
  flex: 1;
  padding: 1.5rem;
  overflow-y: auto;
}

.private-key-form {
  max-width: 600px;
  margin: 0 auto;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

.list-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.add-key-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border: 1px solid rgba(34, 197, 94, 0.3);
  border-radius: 0.5rem;
  background: rgba(34, 197, 94, 0.1);
  color: rgba(34, 197, 94, 0.9);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.add-key-btn:hover {
  background: rgba(34, 197, 94, 0.2);
  border-color: rgba(34, 197, 94, 0.5);
}

.keys-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.key-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 0.5rem;
  transition: all 0.3s ease;
}

.key-item:hover {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.2);
}

.key-info {
  flex: 1;
}

.key-name {
  font-size: 1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 0.25rem 0;
}

.key-description {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 0.5rem 0;
}

.key-meta {
  display: flex;
  gap: 0.5rem;
}

.key-type {
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
  background: rgba(168, 85, 247, 0.2);
  color: rgba(168, 85, 247, 0.9);
  font-size: 0.75rem;
  font-weight: 500;
}

.key-actions {
  display: flex;
  gap: 0.375rem;
}

.key-actions button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  border: none;
  border-radius: 0.25rem;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 0.8rem;
}

.key-actions .edit-btn {
  background: rgba(59, 130, 246, 0.1);
  color: rgba(59, 130, 246, 0.8);
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.key-actions .edit-btn:hover {
  background: rgba(59, 130, 246, 0.2);
  border-color: rgba(59, 130, 246, 0.4);
}

.key-actions .delete-btn {
  background: rgba(239, 68, 68, 0.1);
  color: rgba(239, 68, 68, 0.8);
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.key-actions .delete-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.4);
}
</style>