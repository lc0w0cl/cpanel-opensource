<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { VueDraggable } from 'vue-draggable-plus'

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

// 排序状态管理
const sortingStates = ref<Record<string, boolean>>({})

// 为每个分组创建可拖拽的数据列表
const draggableItems = ref<Record<string, NavigationItem[]>>({})

// 分组信息数据（后续从网络获取）
const categories = ref([
  { id: 'searchEngines', name: '搜索引擎', order: 1 },
  { id: 'ecommerce', name: '电商购物', order: 2 },

])

// 导航数据（后续从网络获取，每个项目都包含categoryId）
const navigationItems = ref([
  // 搜索引擎分组
  { id: 'Bitwarden', name: 'Bitwarden', url: 'https://www.baidu.com', logo: '/logo/bitwarden-logo.svg', categoryId: 'searchEngines' },
  { id: 'google', name: 'Cloudflare', url: 'https://www.google.com', logo: '/logo/cloudflare.svg', categoryId: 'searchEngines' },
  { id: 'bing', name: 'DDNS', url: 'https://www.bing.com', logo: '/logo/ddns.svg', categoryId: 'searchEngines' },
  { id: 'bing', name: 'DeepSeek', url: 'https://www.bing.com', logo: '/logo/deepseek.svg', categoryId: 'searchEngines' },
  { id: '123', name: 'DNSPod', url: 'https://www.bing.com', logo: '/logo/DNSPod.svg', categoryId: 'searchEngines' },
  { id: '121', name: 'Docker', url: 'https://www.bing.com', logo: '/logo/docker-official.svg', categoryId: 'searchEngines' },
  { id: '134', name: 'Yacd', url: 'https://www.bing.com', logo: '/logo/yacd-128.png', categoryId: 'searchEngines' },


  // 电商购物分组
  { id: '123', name: 'DNSPod', url: 'https://www.bing.com', logo: '/logo/DNSPod.svg', categoryId: 'ecommerce' },
  { id: '121', name: 'Docker', url: 'https://www.bing.com', logo: '/logo/docker-official.svg', categoryId: 'ecommerce' },
  { id: '134', name: 'Yacd', url: 'https://www.bing.com', logo: '/logo/yacd-128.png', categoryId: 'ecommerce' },

])

// 根据分组ID获取导航项目
const getItemsByCategory = (categoryId: string) => {
  return navigationItems.value.filter(item => item.categoryId === categoryId)
}

// 初始化拖拽数据
const initializeDraggableData = () => {
  categories.value.forEach(category => {
    draggableItems.value[category.id] = getItemsByCategory(category.id)
  })
}

// 切换排序模式
const toggleSortMode = (categoryId: string) => {
  sortingStates.value[categoryId] = !sortingStates.value[categoryId]

  if (sortingStates.value[categoryId]) {
    // 进入排序模式，初始化拖拽数据
    draggableItems.value[categoryId] = [...getItemsByCategory(categoryId)]
  } else {
    // 退出排序模式，更新原始数据
    const updatedItems = draggableItems.value[categoryId]
    if (updatedItems) {
      // 移除原有的该分组项目
      navigationItems.value = navigationItems.value.filter(item => item.categoryId !== categoryId)
      // 添加新排序的项目
      navigationItems.value.push(...updatedItems)
    }
    console.log(`退出 ${categoryId} 的排序模式`)
    // TODO: 调用API保存排序结果
  }
}

// 处理拖拽排序
const handleDragEnd = (categoryId: string) => {
  console.log(`${categoryId} 分组排序已更新`)
  // 这里可以添加保存排序的逻辑
  // TODO: 调用API保存排序结果
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
  // 初始化拖拽数据
  initializeDraggableData()
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
    // 这里可以打开编辑对话框
    console.log('编辑项目:', item)
    alert(`编辑 ${item.name} 功能待实现`)
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

// 获取分类键名（保留以兼容旧代码）
const getCategoryKey = (category: string) => {
  return category
}

// 点击其他地方隐藏菜单
const handleClickOutside = (event: MouseEvent) => {
  if (contextMenu.value.visible) {
    hideContextMenu()
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
            <button
              class="sort-button"
              :class="{ 'sort-active': sortingStates[category.id] }"
              @click="toggleSortMode(category.id)"
              :title="sortingStates[category.id] ? '退出排序模式' : '开启排序模式'"
            >
              <svg class="sort-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path d="M3 6h18M7 12h10m-7 6h4"></path>
              </svg>
            </button>
          </div>

          <!-- 排序模式下的玻璃边框容器 -->
          <div
            :class="[
              'nav-grid-container',
              { 'sorting-mode': sortingStates[category.id] }
            ]"
          >
            <!-- 非排序模式：普通网格 -->
            <div v-if="!sortingStates[category.id]" class="nav-grid">
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
                <span class="nav-label">{{ item.name }}</span>
              </a>
            </div>

            <!-- 排序模式：可拖拽网格 -->
            <VueDraggable
              v-else
              v-model="draggableItems[category.id]"
              class="nav-grid draggable-grid"
              :animation="200"
              ghost-class="ghost-item"
              chosen-class="chosen-item"
              drag-class="drag-item"
              @end="handleDragEnd(category.id)"
            >
              <div
                v-for="item in draggableItems[category.id] || []"
                :key="item.id"
                class="nav-item draggable-item"
              >
                <div class="nav-icon">
                  <img :src="item.logo" :alt="item.name" class="icon-logo">
                </div>
                <span class="nav-label">{{ item.name }}</span>
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
    </section>
  </NuxtLayout>
</template>

<style scoped>






.navigation-panel {
  position: relative;
  width: 100%;
  /* 如果需要最小高度，可以使用 calc 减去其他元素的高度 */
  /* min-height: calc(100vh - 80px); */
}

.navigation-content {
  max-width: 100%;
  padding: 0 1rem;
}

.nav-section {
  margin-bottom: 3rem;
}

/* 分组标题区域 */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
  padding-left: 0.5rem;
  padding-right: 0.5rem;
}

.section-title {
  font-size: 1.4rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  text-align: left;
  margin: 0;
}

/* 排序按钮样式 */
.sort-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  border: none;
  border-radius: 0.75rem;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  /* 默认状态 */
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  border: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.sort-button:hover {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 100%
  );
  border: 1px solid rgba(255, 255, 255, 0.3);
  transform: translateY(-1px) scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.sort-button.sort-active {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.3) 0%,
    rgba(59, 130, 246, 0.2) 100%
  );
  border: 1px solid rgba(59, 130, 246, 0.4);
  box-shadow: 0 0 20px rgba(59, 130, 246, 0.3);
}

.sort-button.sort-active:hover {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.4) 0%,
    rgba(59, 130, 246, 0.25) 100%
  );
  border: 1px solid rgba(59, 130, 246, 0.5);
}

.sort-icon {
  width: 1.2rem;
  height: 1.2rem;
  stroke-width: 2;
  color: rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.sort-button:hover .sort-icon {
  color: rgba(255, 255, 255, 0.95);
}

.sort-button.sort-active .sort-icon {
  color: rgba(59, 130, 246, 1);
}

/* 网格容器样式 */
.nav-grid-container {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 排序模式下的玻璃边框效果 */
.nav-grid-container.sorting-mode {
  padding: 1.5rem;
  border-radius: 1.5rem;
  position: relative;

  /* 玻璃边框效果 */
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 50%,
    rgba(255, 255, 255, 0.02) 100%
  );
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 2px solid rgba(59, 130, 246, 0.3);

  /* 阴影效果 */
  box-shadow:
    0 8px 32px rgba(59, 130, 246, 0.15),
    0 4px 16px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.2),
    inset 0 -1px 0 rgba(0, 0, 0, 0.05);

  /* 动画效果 */
  animation: sortingModeActivate 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 排序模式激活动画 */
@keyframes sortingModeActivate {
  from {
    opacity: 0;
    transform: scale(0.98);
    border-color: transparent;
    box-shadow: none;
  }
  to {
    opacity: 1;
    transform: scale(1);
    border-color: rgba(59, 130, 246, 0.3);
    box-shadow:
      0 8px 32px rgba(59, 130, 246, 0.15),
      0 4px 16px rgba(0, 0, 0, 0.1),
      inset 0 1px 0 rgba(255, 255, 255, 0.2),
      inset 0 -1px 0 rgba(0, 0, 0, 0.05);
  }
}

.nav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 1.5rem;
}

.nav-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  text-decoration: none;
  color: white;
  border-radius: 1rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  border: 2px solid transparent;
}

.nav-item:hover {
  transform: translateY(-2px) scale(1.02);
  border: 2px solid rgba(255, 255, 255, 0.3);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  box-shadow:
    0 8px 25px rgba(0, 0, 0, 0.15),
    0 4px 10px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.nav-icon {
  width: 3rem;
  height: 3rem;
  border-radius: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.nav-item:hover .nav-icon {
  transform: scale(1.05);
}

.icon-logo {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 0.75rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.nav-item:hover .icon-logo {
  transform: scale(1.05);
  filter: brightness(1.1) saturate(1.2);
}

/* 保留原有的文字图标样式以备后用 */
.icon-text {
  font-size: 1.5rem;
  font-weight: 700;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  display: none; /* 暂时隐藏文字图标 */
}

.nav-label {
  font-size: 1rem;
  font-weight: 600;
  text-align: left;
  color: rgba(255, 255, 255, 0.9);
  transition: all 0.3s ease;
  flex: 1;
}

.nav-item:hover .nav-label {
  transform: translateX(2px);
}

/* 拖拽相关样式 */
.draggable-item {
  cursor: move;
  position: relative;
}

.draggable-item:hover {
  cursor: move;
}

.drag-handle {
  position: absolute;
  top: 50%;
  right: 0.5rem;
  transform: translateY(-50%);
  width: 1.2rem;
  height: 1.2rem;
  opacity: 0;
  transition: all 0.3s ease;
  pointer-events: none;
}

.draggable-item:hover .drag-handle {
  opacity: 0.6;
}

.drag-handle svg {
  width: 100%;
  height: 100%;
  stroke-width: 2;
  color: rgba(255, 255, 255, 0.7);
}

/* 拖拽状态样式 */
.ghost-item {
  opacity: 0.5;
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.2) 0%,
    rgba(59, 130, 246, 0.1) 100%
  );
  border: 2px dashed rgba(59, 130, 246, 0.5) !important;
  transform: scale(0.95);
}

.chosen-item {
  transform: scale(1.02);
  z-index: 1000;
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.3) !important;
  border: 2px solid rgba(59, 130, 246, 0.5) !important;
}

.drag-item {
  transform: rotate(5deg) scale(1.05);
  z-index: 1001;
  opacity: 0.9;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.3) !important;
}

/* 拖拽网格样式 */
.draggable-grid {
  min-height: 100px;
}

/* 排序模式下的导航项目样式调整 */
.sorting-mode .nav-item {
  border: 2px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.05) 0%,
    rgba(255, 255, 255, 0.02) 100%
  );
}

.sorting-mode .nav-item:hover {
  border: 2px solid rgba(59, 130, 246, 0.4);
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.1) 0%,
    rgba(59, 130, 246, 0.05) 100%
  );
  box-shadow:
    0 8px 25px rgba(59, 130, 246, 0.2),
    0 4px 10px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

/* 统一透明玻璃效果 - 移除了各网站特色颜色样式 */
/* 所有图标现在都使用 .nav-icon 的基础液态玻璃样式 */

/* 右键菜单样式 */
.context-menu {
  position: fixed;
  z-index: 1000;
  min-width: 120px;
  padding: 0.5rem 0;
  border-radius: 0.75rem;
  pointer-events: auto;

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
    0 8px 32px rgba(0, 0, 0, 0.2),
    0 4px 16px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.3),
    inset 0 -1px 0 rgba(0, 0, 0, 0.1);

  /* 动画 */
  animation: contextMenuFadeIn 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
}

.context-menu-item:hover {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.2) 0%,
    rgba(255, 255, 255, 0.1) 100%
  );
  transform: translateX(2px);
}

.context-menu-item.delete-item:hover {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.2) 0%,
    rgba(239, 68, 68, 0.1) 100%
  );
  color: #ef4444;
}

.menu-icon {
  width: 1rem;
  height: 1rem;
  stroke-width: 2;
  flex-shrink: 0;
}

/* 菜单动画 */
@keyframes contextMenuFadeIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(-5px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 防止右键菜单在导航项上的默认行为 */
.nav-item {
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
  user-select: none;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .navigation-content {
    padding: 0 1rem;
  }

  .nav-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 1.2rem;
  }

  .nav-icon {
    width: 2.6rem;
    height: 2.6rem;
  }

  .icon-text {
    font-size: 1.3rem;
  }

  .icon-logo {
    border-radius: 0.5rem;
  }

  .nav-label {
    font-size: 0.8rem;
  }

  .section-title {
    font-size: 1.2rem;
  }
}

@media (max-width: 480px) {
  .nav-grid {
    grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
    gap: 1rem;
  }

  .nav-item {
    padding: 0.6rem 0.8rem;
    gap: 0.6rem;
  }

  .nav-icon {
    width: 2.4rem;
    height: 2.4rem;
  }

  .icon-text {
    font-size: 1.1rem;
  }

  .icon-logo {
    border-radius: 0.4rem;
  }

  .nav-label {
    font-size: 0.75rem;
  }

  .section-title {
    font-size: 1.1rem;
  }
}

/* 删除确认对话框样式 */
.delete-confirm-overlay {
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
  gap: 0.75rem;
  padding: 1.5rem 1.5rem 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.warning-icon {
  width: 1.5rem;
  height: 1.5rem;
  color: #f59e0b;
  stroke-width: 2;
  flex-shrink: 0;
}

.dialog-header h3 {
  font-size: 1.25rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.dialog-content {
  padding: 1rem 1.5rem 1.5rem;
}

.dialog-content p {
  margin: 0 0 0.75rem;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.5;
}

.dialog-content p:last-child {
  margin-bottom: 0;
}

.dialog-content strong {
  color: rgba(255, 255, 255, 0.95);
  font-weight: 600;
}

.warning-text {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.6) !important;
  font-style: italic;
}

.dialog-actions {
  display: flex;
  gap: 0.75rem;
  padding: 0 1.5rem 1.5rem;
  justify-content: flex-end;
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
    #ef4444 0%,
    #dc2626 100%
  );
  color: white;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.btn-confirm:hover {
  background: linear-gradient(135deg,
    #dc2626 0%,
    #b91c1c 100%
  );
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
}

/* 通知组件样式 */
.notification {
  position: fixed;
  top: 2rem;
  right: 2rem;
  z-index: 3000;
  min-width: 300px;
  max-width: 400px;
  border-radius: 0.75rem;
  overflow: hidden;
  cursor: pointer;

  /* 液态玻璃效果 */
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.2);

  /* 阴影效果 */
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.2),
    0 4px 16px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.3),
    inset 0 -1px 0 rgba(0, 0, 0, 0.1);

  animation: notificationSlideIn 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.notification-success {
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.2) 0%,
    rgba(34, 197, 94, 0.1) 100%
  );
}

.notification-error {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.2) 0%,
    rgba(239, 68, 68, 0.1) 100%
  );
}

.notification-info {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.2) 0%,
    rgba(59, 130, 246, 0.1) 100%
  );
}

.notification-content {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
}

.notification-icon {
  width: 1.25rem;
  height: 1.25rem;
  stroke-width: 2;
  flex-shrink: 0;
}

.notification-success .notification-icon {
  color: #22c55e;
}

.notification-error .notification-icon {
  color: #ef4444;
}

.notification-info .notification-icon {
  color: #3b82f6;
}

.notification-message {
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
  line-height: 1.4;
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

@keyframes notificationSlideIn {
  from {
    opacity: 0;
    transform: translateX(100%) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .delete-confirm-dialog {
    min-width: 320px;
    max-width: 90vw;
    margin: 1rem;
  }

  .notification {
    top: 1rem;
    right: 1rem;
    left: 1rem;
    min-width: auto;
    max-width: none;
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