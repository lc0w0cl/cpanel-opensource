<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { VueDraggable } from 'vue-draggable-plus'
import NavigationItemDialog from '~/components/NavigationItemDialog.vue'
import './navigation-panel.css'

definePageMeta({
  layout: 'dashboard'
})

// 类型定义
interface Category {
  id: string
  name: string
  order: number
}

interface NavigationItem {
  id: string
  name: string
  url: string
  logo: string
  categoryId: string
  description?: string
  internalUrl?: string
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

// 分组信息数据（后续从网络获取）
const categories = ref([
  { id: 'searchEngines', name: '搜索引擎', order: 1 },
  { id: 'ecommerce', name: '电商购物', order: 2 },

])

// 导航数据（后续从网络获取，每个项目都包含categoryId）
const navigationItems = ref([
  // 搜索引擎分组
  {
    id: 'Bitwarden',
    name: 'Bitwarden',
    url: 'https://vault.bitwarden.com',
    logo: '/logo/bitwarden-logo.svg',
    categoryId: 'searchEngines',
    description: '密码管理器',
    internalUrl: 'http://192.168.1.100:8080'
  },
  {
    id: 'google',
    name: 'Cloudflare',
    url: 'https://dash.cloudflare.com',
    logo: '/logo/cloudflare.svg',
    categoryId: 'searchEngines',
    description: 'CDN和DNS服务'
  },
  {
    id: 'bing',
    name: 'DDNS',
    url: 'https://ddns.example.com',
    logo: '/logo/ddns.svg',
    categoryId: 'searchEngines',
    description: '动态DNS服务',
    internalUrl: 'http://192.168.1.101:3000'
  },
  {
    id: 'deepseek',
    name: 'DeepSeek',
    url: 'https://chat.deepseek.com',
    logo: '/logo/deepseek.svg',
    categoryId: 'searchEngines',
    description: 'AI对话助手'
  },
  {
    id: 'dnspod',
    name: 'DNSPod',
    url: 'https://console.dnspod.cn',
    logo: '/logo/DNSPod.svg',
    categoryId: 'searchEngines',
    description: 'DNS解析服务'
  },
  {
    id: 'docker',
    name: 'Docker',
    url: 'https://hub.docker.com',
    logo: '/logo/docker-official.svg',
    categoryId: 'searchEngines',
    description: '容器镜像仓库',
    internalUrl: 'http://192.168.1.102:9000'
  },
  {
    id: 'yacd',
    name: 'Yacd',
    url: 'http://yacd.haishan.me',
    logo: '/logo/yacd-128.png',
    categoryId: 'searchEngines',
    description: 'Clash代理面板',
    internalUrl: 'http://192.168.1.103:9090/ui'
  },


  // 电商购物分组
  { id: '123', name: 'DNSPod', url: 'https://www.bing.com', logo: '/logo/DNSPod.svg', categoryId: 'ecommerce' },
  { id: '121', name: 'Docker', url: 'https://www.bing.com', logo: '/logo/docker-official.svg', categoryId: 'ecommerce' },
  { id: '134', name: 'Yacd', url: 'https://www.bing.com', logo: '/logo/yacd-128.png', categoryId: 'ecommerce' },

])

// 根据分组ID获取导航项目
const getItemsByCategory = (categoryId: string) => {
  return navigationItems.value.filter(item => item.categoryId === categoryId)
}

// 为每个分组创建独立的响应式数组
const categoryItems = ref<Record<string, NavigationItem[]>>({})

// 初始化分组数据
const initializeCategoryItems = () => {
  categories.value.forEach(category => {
    categoryItems.value[category.id] = navigationItems.value.filter(item => item.categoryId === category.id)
  })
}

// 获取当前分组的可拖拽项目列表
const getDraggableItems = (categoryId: string) => {
  if (!categoryItems.value[categoryId]) {
    categoryItems.value[categoryId] = navigationItems.value.filter(item => item.categoryId === categoryId)
  }
  return categoryItems.value[categoryId]
}

// 根据分组ID获取分组名称
const getCategoryName = (categoryId: string) => {
  const category = categories.value.find(cat => cat.id === categoryId)
  return category ? category.name : categoryId
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
    // TODO: 替换为实际的API调用
    // const response = await fetch('/api/categories')
    // const data = await response.json()
    // categories.value = data.sort((a: Category, b: Category) => a.order - b.order)

    console.log('分组数据已加载（模拟数据）')
  } catch (error) {
    console.error('获取分组数据失败:', error)
  }
}

// 从网络获取导航数据
const fetchNavigationItems = async () => {
  try {
    // TODO: 替换为实际的API调用
    // const response = await fetch('/api/navigation-items')
    // const data = await response.json()
    // navigationItems.value = data

    console.log('导航数据已加载（模拟数据）')
  } catch (error) {
    console.error('获取导航数据失败:', error)
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
    // 从本地数据中删除
    const index = navigationItems.value.findIndex(navItem => navItem.id === item.id)
    if (index === -1) {
      throw new Error('未找到要删除的项目')
    }

    // 从本地数据中删除（乐观更新）
    navigationItems.value.splice(index, 1)

    console.log(`成功删除项目: ${item.name}`)

    // 显示成功提示
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
const showAddItemDialog = (categoryId: string) => {
  itemDialog.value = {
    visible: true,
    mode: 'add',
    categoryId: categoryId,
    item: null
  }
}

// 显示编辑导航项弹窗
const showEditItemDialog = (item: any) => {
  itemDialog.value = {
    visible: true,
    mode: 'edit',
    categoryId: item.categoryId,
    item: item
  }
}

// 处理弹窗确认
const handleDialogConfirm = (data: any) => {
  if (itemDialog.value.mode === 'add') {
    // 新增逻辑
    const newItem: NavigationItem = {
      id: Date.now().toString(), // 临时ID，实际应该由后端生成
      name: data.name,
      url: data.url,
      logo: data.logo,
      categoryId: data.categoryId,
      description: data.description,
      internalUrl: data.internalUrl
    }

    // 添加到本地数据
    navigationItems.value.push(newItem)

    // 更新分组数据
    if (!categoryItems.value[data.categoryId]) {
      categoryItems.value[data.categoryId] = []
    }
    categoryItems.value[data.categoryId].push(newItem)

    console.log('新增导航项:', newItem)
    showNotification(`已成功新增 "${data.name}"`, 'success')
  } else {
    // 编辑逻辑
    const index = navigationItems.value.findIndex(item => item.id === itemDialog.value.item?.id)
    if (index !== -1) {
      // 更新主数据
      navigationItems.value[index] = {
        ...navigationItems.value[index],
        name: data.name,
        url: data.url,
        logo: data.logo,
        description: data.description,
        internalUrl: data.internalUrl
      }

      // 更新分组数据
      const categoryId = navigationItems.value[index].categoryId
      const categoryIndex = categoryItems.value[categoryId]?.findIndex(item => item.id === itemDialog.value.item?.id)
      if (categoryIndex !== -1) {
        categoryItems.value[categoryId][categoryIndex] = navigationItems.value[index]
      }

      console.log('编辑导航项:', navigationItems.value[index])
      showNotification(`已成功编辑 "${data.name}"`, 'success')
    } else {
      showNotification('编辑失败：未找到对应项目', 'error')
    }
  }
}

// 开启排序模式
const enableSortMode = (categoryId: string) => {
  sortMode.value = {
    active: true,
    categoryId: categoryId
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
const handleDragEnd = (categoryId: string) => {
  console.log('排序完成，分组:', categoryId)
  console.log('当前顺序:', categoryItems.value[categoryId])

  // 同步更新主数据数组
  const otherItems = navigationItems.value.filter(item => item.categoryId !== categoryId)
  navigationItems.value = [...otherItems, ...categoryItems.value[categoryId]]

  showNotification('排序已更新', 'success')
  // 这里可以添加保存排序到后端的逻辑
}

// 检查是否为当前排序的分组
const isSortingCategory = (categoryId: string) => {
  return sortMode.value.active && sortMode.value.categoryId === categoryId
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

// 生命周期
onMounted(async () => {
  document.addEventListener('click', handleClickOutside)
  await initializeData()
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <NuxtLayout>
    <section class="navigation-panel">
      <div class="navigation-content">
        <!-- 动态渲染分组 -->
        <div
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
                :href="item.url"
                target="_blank"
                class="nav-item"
                @contextmenu="handleRightClick($event, item, category.id)"
              >
                <div class="nav-icon">
                  <img :src="item.logo" :alt="item.name" class="icon-logo">
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
                  <img :src="item.logo" :alt="item.name" class="icon-logo">
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