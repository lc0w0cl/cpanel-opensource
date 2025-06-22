<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { VueDraggable } from 'vue-draggable-plus'
import NavigationItemDialog from '~/components/NavigationItemDialog.vue'
import { getImageUrl } from '~/lib/utils'
import './navigation-panel.css'

definePageMeta({
  layout: 'dashboard'
})

// 类型定义
interface Category {
  id: number
  name: string
  order: number
  createdAt?: string
  updatedAt?: string
}

interface NavigationItem {
  id: number
  name: string
  url: string
  logo: string
  categoryId: number
  description?: string
  internalUrl?: string
  sortOrder?: number
  createdAt?: string
  updatedAt?: string
}

// API响应类型
interface ApiResponse<T> {
  code: number
  message: string
  data: T
  success: boolean
}

// 右键菜单状态
const contextMenu = ref({
  visible: false,
  x: 0,
  y: 0,
  targetItem: null as any
})

// 删除确认对话框状态
const deleteConfirmDialog = ref({
  visible: false,
  targetItem: null as any
})

// 通知状态
const notification = ref({
  visible: false,
  message: '',
  type: 'success' as 'success' | 'error' | 'info'
})

// 新增/编辑弹窗状态
const itemDialog = ref({
  visible: false,
  mode: 'add' as 'add' | 'edit',
  categoryId: '',
  item: null as any
})

// 排序模式状态
const sortMode = ref({
  active: false,
  categoryId: ''
})

// 搜索功能状态
const searchQuery = ref('')
const searchInputRef = ref<HTMLInputElement>()

// 内外网切换状态
const isInternalNetwork = ref(false)

// API配置
const config = useRuntimeConfig()
const API_BASE_URL = `${config.public.apiBaseUrl}/api`

// API调用函数
const api = {
  // 分类相关API
  async getCategories(): Promise<Category[]> {
    const response = await fetch(`${API_BASE_URL}/categories`)
    const result: ApiResponse<Category[]> = await response.json()
    if (result.success) {
      return result.data
    } else {
      throw new Error(result.message)
    }
  },

  // 导航项相关API
  async getNavigationItems(): Promise<NavigationItem[]> {
    const response = await fetch(`${API_BASE_URL}/navigation-items`)
    const result: ApiResponse<NavigationItem[]> = await response.json()
    if (result.success) {
      return result.data
    } else {
      throw new Error(result.message)
    }
  },

  async getNavigationItemsByCategory(categoryId: number): Promise<NavigationItem[]> {
    const response = await fetch(`${API_BASE_URL}/navigation-items/category/${categoryId}`)
    const result: ApiResponse<NavigationItem[]> = await response.json()
    if (result.success) {
      return result.data
    } else {
      throw new Error(result.message)
    }
  },

  async searchNavigationItems(name?: string): Promise<NavigationItem[]> {
    const url = name
      ? `${API_BASE_URL}/navigation-items/search?name=${encodeURIComponent(name)}`
      : `${API_BASE_URL}/navigation-items/search`
    const response = await fetch(url)
    const result: ApiResponse<NavigationItem[]> = await response.json()
    if (result.success) {
      return result.data
    } else {
      throw new Error(result.message)
    }
  },

  async createNavigationItem(item: Omit<NavigationItem, 'id' | 'createdAt' | 'updatedAt'>): Promise<NavigationItem> {
    const response = await fetch(`${API_BASE_URL}/navigation-items`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(item)
    })
    const result: ApiResponse<NavigationItem> = await response.json()
    if (result.success) {
      return result.data
    } else {
      throw new Error(result.message)
    }
  },

  async createNavigationItemWithUpload(formData: FormData): Promise<NavigationItem> {
    const response = await fetch(`${API_BASE_URL}/navigation-items`, {
      method: 'POST',
      body: formData
    })
    const result: ApiResponse<NavigationItem> = await response.json()
    if (result.success) {
      return result.data
    } else {
      throw new Error(result.message)
    }
  },

  async updateNavigationItem(item: NavigationItem): Promise<NavigationItem> {
    const response = await fetch(`${API_BASE_URL}/navigation-items/${item.id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(item)
    })
    const result: ApiResponse<NavigationItem> = await response.json()
    if (result.success) {
      return result.data
    } else {
      throw new Error(result.message)
    }
  },

  async updateNavigationItemWithUpload(id: number, formData: FormData): Promise<NavigationItem> {
    const response = await fetch(`${API_BASE_URL}/navigation-items/${id}/upload`, {
      method: 'PUT',
      body: formData
    })
    const result: ApiResponse<NavigationItem> = await response.json()
    if (result.success) {
      return result.data
    } else {
      throw new Error(result.message)
    }
  },

  async deleteNavigationItem(id: number): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/navigation-items/${id}`, {
      method: 'DELETE'
    })
    const result: ApiResponse<string> = await response.json()
    if (!result.success) {
      throw new Error(result.message)
    }
  },

  async updateNavigationItemsSort(items: NavigationItem[]): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/navigation-items/sort`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(items)
    })
    const result: ApiResponse<string> = await response.json()
    if (!result.success) {
      throw new Error(result.message)
    }
  }
}

// 分组信息数据（从后端API获取）
const categories = ref<Category[]>([])

// 导航数据（从后端API获取）
const navigationItems = ref<NavigationItem[]>([])

// 加载状态
const loading = ref({
  categories: false,
  navigationItems: false
})

// 根据分组ID获取导航项目
const getItemsByCategory = (categoryId: number) => {
  const items = navigationItems.value.filter(item => item.categoryId === categoryId)

  // 如果有搜索关键词，进行过滤
  if (searchQuery.value.trim()) {
    return filterItemsBySearch(items)
  }

  return items
}

// 搜索过滤函数
const filterItemsBySearch = (items: NavigationItem[]) => {
  const query = searchQuery.value.trim().toLowerCase()
  if (!query) return items

  return items.filter(item =>
    item.name.toLowerCase().includes(query) ||
    (item.description && item.description.toLowerCase().includes(query)) ||
    item.url.toLowerCase().includes(query) ||
    (item.internalUrl && item.internalUrl.toLowerCase().includes(query))
  )
}

// 获取所有搜索匹配的项目（跨分组）
const getAllSearchResults = () => {
  if (!searchQuery.value.trim()) return []

  return filterItemsBySearch(navigationItems.value)
}

// 检查是否有搜索结果
const hasSearchResults = computed(() => {
  if (!searchQuery.value.trim()) return true
  return getAllSearchResults().length > 0
})

// 清除搜索
const clearSearch = () => {
  searchQuery.value = ''
  // 清除搜索后重新聚焦搜索框
  focusSearchInput()
}

// 切换内外网
const toggleNetwork = () => {
  isInternalNetwork.value = !isInternalNetwork.value
  const networkType = isInternalNetwork.value ? '内网' : '外网'
  showNotification(`切换到${networkType}环境`, 'info')
}

// 获取导航项的实际URL
const getNavigationUrl = (item: NavigationItem) => {
  // 如果是内网模式且有内网地址，使用内网地址
  if (isInternalNetwork.value && item.internalUrl) {
    return item.internalUrl
  }
  // 否则使用外网地址
  return item.url
}

// 处理键盘事件
const handleKeydown = (event: KeyboardEvent) => {
  // 按下 Escape 键清除搜索
  if (event.key === 'Escape' && searchQuery.value) {
    clearSearch()
    event.preventDefault()
  }
}

// 为每个分组创建独立的响应式数组
const categoryItems = ref<Record<number, NavigationItem[]>>({})

// 初始化分组数据
const initializeCategoryItems = () => {
  categories.value.forEach(category => {
    categoryItems.value[category.id] = navigationItems.value.filter(item => item.categoryId === category.id)
  })
}

// 获取当前分组的可拖拽项目列表
const getDraggableItems = (categoryId: number) => {
  if (!categoryItems.value[categoryId]) {
    categoryItems.value[categoryId] = navigationItems.value.filter(item => item.categoryId === categoryId)
  }
  return categoryItems.value[categoryId]
}

// 根据分组ID获取分组名称
const getCategoryName = (categoryId: number) => {
  const category = categories.value.find(cat => cat.id === categoryId)
  return category ? category.name : `分类${categoryId}`
}

// 显示通知
const showNotification = (message: string, type: 'success' | 'error' | 'info' = 'success') => {
  notification.value = {
    visible: true,
    message,
    type
  }

  // 3秒后自动隐藏
  setTimeout(() => {
    notification.value.visible = false
  }, 3000)
}

// 隐藏通知
const hideNotification = () => {
  notification.value.visible = false
}

// 从网络获取分组数据
const fetchCategories = async () => {
  try {
    loading.value.categories = true
    const data = await api.getCategories()
    categories.value = data.sort((a: Category, b: Category) => a.order - b.order)
    console.log('分组数据已加载:', data)
  } catch (error) {
    console.error('获取分组数据失败:', error)
    showNotification('获取分组数据失败: ' + error.message, 'error')
  } finally {
    loading.value.categories = false
  }
}

// 从网络获取导航数据
const fetchNavigationItems = async () => {
  try {
    loading.value.navigationItems = true
    const data = await api.getNavigationItems()
    navigationItems.value = data
    console.log('导航数据已加载:', data)
  } catch (error) {
    console.error('获取导航数据失败:', error)
    showNotification('获取导航数据失败: ' + error.message, 'error')
  } finally {
    loading.value.navigationItems = false
  }
}

// 初始化数据
const initializeData = async () => {
  await Promise.all([
    fetchCategories(),
    fetchNavigationItems()
  ])
  // 初始化分组数据
  initializeCategoryItems()
}

// 处理右键点击
const handleRightClick = (event: MouseEvent, item: any, category: string) => {
  event.preventDefault()
  event.stopPropagation()

  // 获取鼠标位置
  const mouseX = event.clientX
  const mouseY = event.clientY

  // 获取sidebar的宽度偏移
  const sidebar = document.querySelector('.sidebar-component') as HTMLElement
  const sidebarWidth = sidebar ? sidebar.offsetWidth : 0

  // 获取layout-container的偏移（因为它只有90%宽度）
  const layoutContainer = document.querySelector('.layout-container') as HTMLElement
  const layoutOffset = layoutContainer ? layoutContainer.getBoundingClientRect().left : 0

  console.log('调试信息:', {
    mouseX,
    mouseY,
    sidebarWidth,
    layoutOffset,
    totalOffset: sidebarWidth + layoutOffset
  })

  // 菜单尺寸
  const menuWidth = 120
  const menuHeight = 100

  // 计算最终位置，减去sidebar偏移，添加小偏移
  let finalX = mouseX - sidebarWidth + 10
  let finalY = mouseY + 10

  // 边界检测（考虑sidebar偏移）
  const contentAreaWidth = window.innerWidth - sidebarWidth
  if (finalX + menuWidth > contentAreaWidth) {
    finalX = mouseX - sidebarWidth - menuWidth - 10
  }

  if (finalY + menuHeight > window.innerHeight) {
    finalY = mouseY - menuHeight - 10
  }

  // 确保不超出边界（考虑sidebar偏移）
  finalX = Math.max(10 - sidebarWidth, finalX)
  finalY = Math.max(10, finalY)

  console.log('最终菜单位置:', { x: finalX, y: finalY })

  contextMenu.value = {
    visible: true,
    x: finalX,
    y: finalY,
    targetItem: { ...item, category }
  }
}

// 隐藏右键菜单
const hideContextMenu = () => {
  contextMenu.value.visible = false
}

// 编辑项目
const editItem = () => {
  const item = contextMenu.value.targetItem
  if (item) {
    showEditItemDialog(item)
  }
  hideContextMenu()
}

// 显示删除确认对话框
const showDeleteConfirm = () => {
  const item = contextMenu.value.targetItem
  if (item) {
    deleteConfirmDialog.value = {
      visible: true,
      targetItem: item
    }
  }
  hideContextMenu()
}

// 隐藏删除确认对话框
const hideDeleteConfirm = () => {
  deleteConfirmDialog.value.visible = false
  deleteConfirmDialog.value.targetItem = null
}

// 确认删除项目
const confirmDelete = async () => {
  const item = deleteConfirmDialog.value.targetItem
  if (!item) {
    console.error('没有选中的项目')
    hideDeleteConfirm()
    return
  }

  try {
    // 调用后端API删除
    await api.deleteNavigationItem(item.id)

    // 从本地数据中删除（乐观更新）
    const index = navigationItems.value.findIndex(navItem => navItem.id === item.id)
    if (index !== -1) {
      navigationItems.value.splice(index, 1)
    }

    console.log(`成功删除项目: ${item.name}`)
    showNotification(`已成功删除 "${item.name}"`, 'success')

  } catch (error) {
    console.error('删除项目失败:', error)
    showNotification(`删除失败: ${error.message || '未知错误'}`, 'error')
  } finally {
    hideDeleteConfirm()
  }
}

// 删除项目（旧版本，保留以兼容）
const deleteItem = () => {
  showDeleteConfirm()
}

// 显示新增导航项弹窗
const showAddItemDialog = (categoryId: number) => {
  itemDialog.value = {
    visible: true,
    mode: 'add',
    categoryId: categoryId.toString(),
    item: null
  }
}

// 显示编辑导航项弹窗
const showEditItemDialog = (item: any) => {
  itemDialog.value = {
    visible: true,
    mode: 'edit',
    categoryId: item.categoryId.toString(),
    item: item
  }
}

// 处理弹窗确认
const handleDialogConfirm = async (data: { formData: any, isUpload: boolean }) => {
  try {
    if (itemDialog.value.mode === 'add') {
      // 新增逻辑
      let newItem: NavigationItem

      if (data.isUpload) {
        // 使用文件上传API
        newItem = await api.createNavigationItemWithUpload(data.formData)
      } else {
        // 使用传统API，data.formData是普通对象
        const formData = data.formData
        const newItemData = {
          name: formData.name,
          url: formData.url,
          logo: formData.logo,
          categoryId: parseInt(formData.categoryId),
          description: formData.description,
          internalUrl: formData.internalUrl
        }
        newItem = await api.createNavigationItem(newItemData)
      }

      // 添加到本地数据
      navigationItems.value.push(newItem)

      // 更新分组数据
      const categoryId = newItem.categoryId
      if (!categoryItems.value[categoryId]) {
        categoryItems.value[categoryId] = []
      }
      categoryItems.value[categoryId].push(newItem)

      console.log('新增导航项:', newItem)
      showNotification(`已成功新增 "${newItem.name}"`, 'success')
    } else {
      // 编辑逻辑
      const index = navigationItems.value.findIndex(item => item.id === itemDialog.value.item?.id)
      if (index !== -1) {
        let result: NavigationItem

        if (data.isUpload) {
          // 使用文件上传API更新
          result = await api.updateNavigationItemWithUpload(itemDialog.value.item.id, data.formData)
        } else {
          // 使用传统API更新，data.formData是普通对象
          const formData = data.formData
          const updatedItem = {
            ...navigationItems.value[index],
            name: formData.name,
            url: formData.url,
            logo: formData.logo,
            description: formData.description,
            internalUrl: formData.internalUrl
          }
          result = await api.updateNavigationItem(updatedItem)
        }

        // 更新本地数据
        navigationItems.value[index] = result

        // 更新分组数据
        const categoryId = result.categoryId
        const categoryIndex = categoryItems.value[categoryId]?.findIndex(item => item.id === result.id)
        if (categoryIndex !== -1) {
          categoryItems.value[categoryId][categoryIndex] = result
        }

        console.log('编辑导航项:', result)
        showNotification(`已成功编辑 "${result.name}"`, 'success')
      } else {
        showNotification('编辑失败：未找到对应项目', 'error')
      }
    }

    // 关闭弹窗
    itemDialog.value.visible = false
  } catch (error) {
    console.error('操作失败:', error)
    showNotification(`操作失败: ${error.message || '未知错误'}`, 'error')
  }
}

// 开启排序模式
const enableSortMode = (categoryId: number) => {
  sortMode.value = {
    active: true,
    categoryId: categoryId.toString()
  }
  // showNotification('已开启排序模式，可以拖拽调整顺序', 'info')
}

// 关闭排序模式
const disableSortMode = () => {
  sortMode.value = {
    active: false,
    categoryId: ''
  }
  // showNotification('已退出排序模式', 'success')
}

// 处理拖拽排序
const handleDragEnd = async (categoryId: number) => {
  console.log('排序完成，分组:', categoryId)
  console.log('当前顺序:', categoryItems.value[categoryId])

  try {
    // 更新排序号
    const sortedItems = categoryItems.value[categoryId].map((item, index) => ({
      ...item,
      sortOrder: index + 1
    }))

    // 调用后端API保存排序
    await api.updateNavigationItemsSort(sortedItems)

    // 同步更新主数据数组
    const otherItems = navigationItems.value.filter(item => item.categoryId !== categoryId)
    navigationItems.value = [...otherItems, ...sortedItems]

    // 更新本地分组数据
    categoryItems.value[categoryId] = sortedItems

    showNotification('排序已保存', 'success')
    console.log('排序已保存到数据库')
  } catch (error) {
    console.error('保存排序失败:', error)
    showNotification('排序保存失败: ' + error.message, 'error')

    // 如果保存失败，重新加载数据以恢复原始顺序
    await fetchNavigationItems()
    initializeCategoryItems()
  }
}

// 检查是否为当前排序的分组
const isSortingCategory = (categoryId: number) => {
  return sortMode.value.active && sortMode.value.categoryId === categoryId.toString()
}

// 获取分类键名（保留以兼容旧代码）
const getCategoryKey = (category: string) => {
  return category
}

// 点击其他地方隐藏菜单
const handleClickOutside = (event: MouseEvent) => {
  if (contextMenu.value.visible) {
    hideContextMenu()
  }
  // 如果点击的不是排序相关的元素，退出排序模式
  if (sortMode.value.active) {
    const target = event.target as HTMLElement
    if (!target.closest('.nav-section') && !target.closest('.sort-btn')) {
      disableSortMode()
    }
  }
}

// 自动聚焦搜索框
const focusSearchInput = () => {
  // 使用 nextTick 确保 DOM 已经渲染完成
  nextTick(() => {
    if (searchInputRef.value) {
      searchInputRef.value.focus()
    }
  })
}

// 生命周期
onMounted(async () => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('keydown', handleKeydown)
  await initializeData()

  // 页面加载完成后自动聚焦搜索框
  focusSearchInput()
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <NuxtLayout>
    <section class="navigation-panel">
      <div class="navigation-content">
        <!-- 搜索过滤区域 -->
        <div class="search-section">
          <div class="search-container">
            <div class="search-input-wrapper">
              <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <circle cx="11" cy="11" r="8"></circle>
                <path d="m21 21-4.35-4.35"></path>
              </svg>
              <input
                ref="searchInputRef"
                v-model="searchQuery"
                type="text"
                class="search-input"
                placeholder="搜索导航项目..."
                @keydown="handleKeydown"
              />
              <button
                v-if="searchQuery"
                class="clear-search-btn"
                @click="clearSearch"
                title="清除搜索"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>

            <!-- 内外网切换按钮 -->
            <button
              class="network-switch-btn"
              @click="toggleNetwork"
              :title="isInternalNetwork ? '当前：内网环境，点击切换到外网' : '当前：外网环境，点击切换到内网'"
            >
              <!-- 外网图标 -->
              <svg v-if="!isInternalNetwork" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <circle cx="12" cy="12" r="10"></circle>
                <path d="M2 12h20"></path>
                <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path>
              </svg>
              <!-- 内网图标 -->
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path d="M9 12l2 2 4-4"></path>
                <path d="M21 12c-1 0-3-1-3-3s2-3 3-3 3 1 3 3-2 3-3 3"></path>
                <path d="M3 12c1 0 3-1 3-3s-2-3-3-3-3 1-3 3 2 3 3 3"></path>
                <path d="M12 21c0-1-1-3-3-3s-3 2-3 3 1 3 3 3 3-2 3-3"></path>
                <path d="M12 3c0 1-1 3-3 3s-3-2-3-3 1-3 3-3 3 2 3 3"></path>
              </svg>
            </button>
          </div>
        </div>

        <!-- 搜索结果显示 -->
        <div v-if="searchQuery.trim() && hasSearchResults" class="search-results-section">
          <div class="search-results-header">
            <h3 class="search-results-title">
              搜索结果 ({{ getAllSearchResults().length }} 项)
            </h3>
          </div>
          <div class="nav-grid">
            <a
              v-for="item in getAllSearchResults()"
              :key="item.id"
              :href="getNavigationUrl(item)"
              target="_blank"
              class="nav-item"
              @contextmenu="handleRightClick($event, item, item.categoryId)"
            >
              <div class="nav-icon">
                <img :src="getImageUrl(item.logo)" :alt="item.name" class="icon-logo">
              </div>
              <div class="nav-content">
                <span class="nav-label">{{ item.name }}</span>
                <span v-if="item.description" class="nav-description">{{ item.description }}</span>
                <span class="nav-category">{{ getCategoryName(item.categoryId) }}</span>
              </div>
            </a>
          </div>
        </div>

        <!-- 无搜索结果提示 -->
        <div v-else-if="searchQuery.trim() && !hasSearchResults" class="no-results-section">
          <div class="no-results-content">
            <svg class="no-results-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <circle cx="11" cy="11" r="8"></circle>
              <path d="m21 21-4.35-4.35"></path>
              <line x1="11" y1="8" x2="11" y2="12"></line>
              <line x1="11" y1="16" x2="11.01" y2="16"></line>
            </svg>
            <h3 class="no-results-title">未找到匹配的导航项</h3>
            <p class="no-results-description">
              尝试使用不同的关键词，或者
              <button class="clear-search-link" @click="clearSearch">清除搜索</button>
              查看所有项目
            </p>
          </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading.categories || loading.navigationItems" class="loading-section">
          <div class="loading-content">
            <div class="loading-spinner"></div>
            <p class="loading-text">正在加载数据...</p>
          </div>
        </div>

        <!-- 动态渲染分组 -->
        <div
          v-else-if="!searchQuery.trim()"
          v-for="category in categories"
          :key="category.id"
          class="nav-section"
        >
          <div class="section-header">
            <h3 class="section-title">{{ category.name }}</h3>
            <div class="section-actions">
              <button
                class="sort-btn"
                :class="{ active: isSortingCategory(category.id) }"
                @click="isSortingCategory(category.id) ? disableSortMode() : enableSortMode(category.id)"
                :title="isSortingCategory(category.id) ? '退出排序' : '排序'"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path d="M3 6h18M7 12h10m-7 6h4"></path>
                </svg>
              </button>
              <button
                class="add-item-btn"
                @click="showAddItemDialog(category.id)"
                title="新增导航项"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <line x1="12" y1="5" x2="12" y2="19"></line>
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                </svg>
              </button>
            </div>
          </div>
          <div
            class="nav-grid-container"
            :class="{ 'sorting-mode': isSortingCategory(category.id) }"
          >
            <!-- 非排序模式：普通显示 -->
            <div v-if="!isSortingCategory(category.id)" class="nav-grid">
              <a
                v-for="item in getItemsByCategory(category.id)"
                :key="item.id"
                :href="getNavigationUrl(item)"
                target="_blank"
                class="nav-item"
                @contextmenu="handleRightClick($event, item, category.id)"
              >
                <div class="nav-icon">
                  <img :src="getImageUrl(item.logo)" :alt="item.name" class="icon-logo">
                </div>
                <div class="nav-content">
                  <span class="nav-label">{{ item.name }}</span>
                  <span v-if="item.description" class="nav-description">{{ item.description }}</span>
                </div>
              </a>
            </div>

            <!-- 排序模式：可拖拽 -->
            <VueDraggable
              v-else
              v-model="categoryItems[category.id]"
              class="nav-grid draggable-grid"
              :animation="200"
              ghost-class="ghost-item"
              chosen-class="chosen-item"
              drag-class="drag-item"
              handle=".drag-handle"
              @end="handleDragEnd(category.id)"
            >
              <div
                v-for="item in categoryItems[category.id]"
                :key="item.id"
                class="nav-item draggable-item"
                :data-category="category.id"
              >
                <div class="nav-icon">
                  <img :src="getImageUrl(item.logo)" :alt="item.name" class="icon-logo">
                </div>
                <div class="nav-content">
                  <span class="nav-label">{{ item.name }}</span>
                  <span v-if="item.description" class="nav-description">{{ item.description }}</span>
                </div>
                <div class="drag-handle">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <circle cx="9" cy="12" r="1"></circle>
                    <circle cx="9" cy="5" r="1"></circle>
                    <circle cx="9" cy="19" r="1"></circle>
                    <circle cx="15" cy="12" r="1"></circle>
                    <circle cx="15" cy="5" r="1"></circle>
                    <circle cx="15" cy="19" r="1"></circle>
                  </svg>
                </div>
              </div>
            </VueDraggable>
          </div>
        </div>
      </div>

      <!-- 右键菜单 -->
      <div
        v-if="contextMenu.visible"
        class="context-menu"
        :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
        @click.stop
      >
        <div class="context-menu-item" @click="editItem">
          <svg class="menu-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
            <path d="m18.5 2.5 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
          </svg>
          编辑
        </div>
        <div class="context-menu-item delete-item" @click="deleteItem">
          <svg class="menu-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <polyline points="3,6 5,6 21,6"></polyline>
            <path d="m19,6v14a2,2 0 0,1 -2,2H7a2,2 0 0,1 -2,-2V6m3,0V4a2,2 0 0,1 2,-2h4a2,2 0 0,1 2,2v2"></path>
            <line x1="10" y1="11" x2="10" y2="17"></line>
            <line x1="14" y1="11" x2="14" y2="17"></line>
          </svg>
          删除
        </div>
      </div>

      <!-- 删除确认对话框 -->
      <div
        v-if="deleteConfirmDialog.visible"
        class="delete-confirm-overlay"
        @click="hideDeleteConfirm"
      >
        <div class="delete-confirm-dialog" @click.stop>
          <div class="dialog-header">
            <svg class="warning-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"></path>
              <path d="M12 9v4"></path>
              <path d="m12 17 .01 0"></path>
            </svg>
            <h3>确认删除</h3>
          </div>
          <div class="dialog-content">
            <p>确定要删除 <strong>{{ deleteConfirmDialog.targetItem?.name }}</strong> 吗？</p>
            <p class="warning-text">此操作不可撤销。</p>
          </div>
          <div class="dialog-actions">
            <button class="btn-cancel" @click="hideDeleteConfirm">取消</button>
            <button class="btn-confirm" @click="confirmDelete">删除</button>
          </div>
        </div>
      </div>

      <!-- 通知组件 -->
      <div
        v-if="notification.visible"
        :class="[
          'notification',
          `notification-${notification.type}`
        ]"
        @click="hideNotification"
      >
        <div class="notification-content">
          <svg v-if="notification.type === 'success'" class="notification-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path d="M9 12l2 2 4-4"></path>
            <circle cx="12" cy="12" r="10"></circle>
          </svg>
          <svg v-else-if="notification.type === 'error'" class="notification-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="15" y1="9" x2="9" y2="15"></line>
            <line x1="9" y1="9" x2="15" y2="15"></line>
          </svg>
          <svg v-else class="notification-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="16" x2="12" y2="12"></line>
            <line x1="12" y1="8" x2="12.01" y2="8"></line>
          </svg>
          <span class="notification-message">{{ notification.message }}</span>
        </div>
      </div>

      <!-- 新增/编辑弹窗 -->
      <NavigationItemDialog
        v-model:visible="itemDialog.visible"
        :mode="itemDialog.mode"
        :category-id="itemDialog.categoryId"
        :item="itemDialog.item"
        @confirm="handleDialogConfirm"
      />
    </section>
  </NuxtLayout>
</template>