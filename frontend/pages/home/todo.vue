<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { Motion } from "motion-v"
import { Icon } from '@iconify/vue'
import { VueDraggable } from 'vue-draggable-plus'

// 子页面不需要定义 layout 和 middleware，由父页面处理

// 导入API函数和类型
import { useTodoApi, type Todo } from '~/composables/useTodoApi'
import { apiRequest } from '~/composables/useJwt'

// 筛选类型
type FilterType = 'all' | 'active' | 'completed'

// 分组接口定义
interface TodoCategory {
  id: number
  name: string
  type: string
  order: number
  createdAt: string
  updatedAt: string
}

// 初始化API
const todoApi = useTodoApi()
const config = useRuntimeConfig()
const API_BASE_URL = `${config.public.apiBaseUrl}/api`



// 响应式数据
const todos = ref<Todo[]>([])
const newTodoText = ref('')
const currentFilter = ref<FilterType>('all')
const editingId = ref<number | null>(null)
const editingText = ref('')
const showEditModal = ref(false)
const editModalTodo = ref<Todo | null>(null)

// 分组相关数据
const todoCategories = ref<TodoCategory[]>([])
const selectedCategoryId = ref<number | null>(null)
const categoriesLoading = ref(false)
const showCategoryDropdown = ref(false)

// 计算属性
const sortedTodos = computed(() => {
  // 根据选择的分组过滤任务
  let filteredByCategory = todos.value
  if (selectedCategoryId.value !== null) {
    filteredByCategory = todos.value.filter(todo => todo.categoryId === selectedCategoryId.value)
  }

  // 默认按sortOrder排序，如果没有sortOrder则按创建时间倒序
  return [...filteredByCategory].sort((a, b) => {
    if (a.sortOrder !== undefined && b.sortOrder !== undefined) {
      return a.sortOrder - b.sortOrder
    }
    if (a.sortOrder !== undefined) return -1
    if (b.sortOrder !== undefined) return 1
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  })
})

// 分别获取待办和已完成的任务
const activeTodos = computed(() => {
  return sortedTodos.value.filter(todo => !todo.completed)
})

const completedTodos = computed(() => {
  return sortedTodos.value.filter(todo => todo.completed)
})

const filteredTodos = computed(() => {
  const sorted = sortedTodos.value
  switch (currentFilter.value) {
    case 'active':
      return sorted.filter(todo => !todo.completed)
    case 'completed':
      return sorted.filter(todo => todo.completed)
    default:
      return sorted
  }
})

const todoStats = computed(() => {
  const total = todos.value.length
  const completed = todos.value.filter(todo => todo.completed).length
  const active = total - completed
  return { total, completed, active }
})

const allCompleted = computed(() => {
  return todos.value.length > 0 && todos.value.every(todo => todo.completed)
})

// 获取TODO分组列表
const fetchTodoCategories = async () => {
  categoriesLoading.value = true
  try {
    const response = await apiRequest(`${API_BASE_URL}/categories/type/todo`)
    const result = await response.json()

    if (result.success) {
      todoCategories.value = result.data
    } else {
      console.error('获取TODO分组失败:', result.message)
    }
  } catch (error) {
    console.error('获取TODO分组失败:', error)
  } finally {
    categoriesLoading.value = false
  }
}

// 添加新任务
const addTodo = async () => {
  const text = newTodoText.value.trim()
  if (!text) return

  try {
    // 创建任务时传入选择的分组ID
    const requestData: any = { text }
    if (selectedCategoryId.value !== null) {
      requestData.categoryId = selectedCategoryId.value
    }

    const response = await apiRequest(`${API_BASE_URL}/todos`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestData)
    })

    const result = await response.json()
    if (result.success && result.data) {
      todos.value.unshift(result.data)
      newTodoText.value = ''
    }
  } catch (error) {
    console.error('添加任务失败:', error)
  }
}

// 切换任务完成状态
const toggleTodo = async (id: number) => {
  try {
    const success = await todoApi.toggleTodoCompleted(id)
    if (success) {
      const todo = todos.value.find(t => t.id === id)
      if (todo) {
        todo.completed = !todo.completed
        todo.updatedAt = new Date().toISOString()
      }
    }
  } catch (error) {
    console.error('切换任务状态失败:', error)
  }
}

// 删除任务
const deleteTodo = async (id: number) => {
  try {
    const success = await todoApi.deleteTodo(id)
    if (success) {
      const index = todos.value.findIndex(t => t.id === id)
      if (index > -1) {
        todos.value.splice(index, 1)
      }
    }
  } catch (error) {
    console.error('删除任务失败:', error)
  }
}

// 开始编辑 - 打开模态框
const startEdit = (todo: Todo) => {
  editModalTodo.value = { ...todo }
  editingText.value = todo.text
  showEditModal.value = true

  // 下一个tick时聚焦输入框
  nextTick(() => {
    const editInput = document.querySelector('.edit-modal-textarea') as HTMLTextAreaElement
    if (editInput) {
      editInput.focus()
      editInput.select()
    }
  })
}

// 保存编辑
const saveEdit = async () => {
  const text = editingText.value.trim()
  if (!text) {
    cancelEdit()
    return
  }

  if (editModalTodo.value) {
    try {
      const success = await todoApi.updateTodoText(editModalTodo.value.id, text)
      if (success) {
        const todo = todos.value.find(t => t.id === editModalTodo.value!.id)
        if (todo) {
          todo.text = text
          todo.updatedAt = new Date().toISOString()
        }
      }
    } catch (error) {
      console.error('更新任务失败:', error)
    }
  }

  cancelEdit()
}

// 取消编辑
const cancelEdit = () => {
  showEditModal.value = false
  editModalTodo.value = null
  editingText.value = ''
}

// 点击模态框外部关闭
const closeModalOnClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (target.classList.contains('edit-modal-overlay')) {
    saveEdit()
  }
}

// 全选/全不选
const toggleAll = async () => {
  const shouldComplete = !allCompleted.value
  try {
    const success = await todoApi.setAllTodosCompleted(shouldComplete)
    if (success) {
      todos.value.forEach(todo => {
        todo.completed = shouldComplete
        todo.updatedAt = new Date().toISOString()
      })
    }
  } catch (error) {
    console.error('批量设置任务状态失败:', error)
  }
}

// 清除已完成任务
const clearCompleted = async () => {
  try {
    const deletedCount = await todoApi.deleteCompletedTodos()
    if (deletedCount > 0) {
      todos.value = todos.value.filter(todo => !todo.completed)
    }
  } catch (error) {
    console.error('删除已完成任务失败:', error)
  }
}



// 拖拽处理函数
const onActiveDragEnd = async (event: any) => {
  const { newIndex, oldIndex, to, from } = event

  // 如果拖拽到已完成列表
  if (to.classList.contains('completed-list')) {
    const draggedTodo = activeTodos.value[oldIndex]
    if (draggedTodo) {
      try {
        const success = await todoApi.toggleTodoCompleted(draggedTodo.id)
        if (success) {
          // 更新本地状态
          const originalTodo = todos.value.find(t => t.id === draggedTodo.id)
          if (originalTodo) {
            originalTodo.completed = true
            originalTodo.updatedAt = new Date().toISOString()
          }
        }
      } catch (error) {
        console.error('切换任务状态失败:', error)
      }
    }
  } else {
    // 在同一列表内排序
    const currentTime = new Date().toISOString()
    const todoIds = activeTodos.value.map(todo => todo.id)

    try {
      const success = await todoApi.updateTodosSortOrder(todoIds)
      if (success) {
        // 更新本地排序号
        activeTodos.value.forEach((todo, index) => {
          const originalTodo = todos.value.find(t => t.id === todo.id)
          if (originalTodo) {
            originalTodo.sortOrder = index + 1
            originalTodo.updatedAt = currentTime
          }
        })
      }
    } catch (error) {
      console.error('更新任务排序失败:', error)
    }
  }
}

const onCompletedDragEnd = async (event: any) => {
  const { newIndex, oldIndex, to, from } = event

  // 如果拖拽到待办列表
  if (to.classList.contains('active-list')) {
    const draggedTodo = completedTodos.value[oldIndex]
    if (draggedTodo) {
      try {
        const success = await todoApi.toggleTodoCompleted(draggedTodo.id)
        if (success) {
          // 更新本地状态
          const originalTodo = todos.value.find(t => t.id === draggedTodo.id)
          if (originalTodo) {
            originalTodo.completed = false
            originalTodo.updatedAt = new Date().toISOString()
          }
        }
      } catch (error) {
        console.error('切换任务状态失败:', error)
      }
    }
  }
  // 已完成列表内部不需要排序
}



// 从API加载任务
const loadTodos = async () => {
  try {
    const todoList = await todoApi.getAllTodos()
    todos.value = todoList
  } catch (error) {
    console.error('加载任务失败:', error)
    todos.value = []
  }
}

// 处理回车键
const handleKeyup = (event: KeyboardEvent) => {
  if (event.key === 'Enter') {
    if (showEditModal.value) {
      // 在模态框中，Ctrl+Enter 或 Cmd+Enter 保存
      if (event.ctrlKey || event.metaKey) {
        saveEdit()
      }
    } else {
      addTodo()
    }
  } else if (event.key === 'Escape' && showEditModal.value) {
    cancelEdit()
  }
}

// 模态框专用的键盘事件处理
const handleModalKeyup = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
    saveEdit()
  } else if (event.key === 'Escape') {
    cancelEdit()
  }
}

// 本地存储键名
const SELECTED_CATEGORY_KEY = 'todo-selected-category'

// 从本地存储加载选择的分组
const loadSelectedCategory = () => {
  try {
    const saved = localStorage.getItem(SELECTED_CATEGORY_KEY)
    if (saved !== null) {
      const categoryId = saved === 'null' ? null : parseInt(saved, 10)
      selectedCategoryId.value = isNaN(categoryId) ? null : categoryId
    }
  } catch (error) {
    console.warn('加载选择的分组失败:', error)
  }
}

// 保存选择的分组到本地存储
const saveSelectedCategory = (categoryId: number | null) => {
  try {
    localStorage.setItem(SELECTED_CATEGORY_KEY, String(categoryId))
  } catch (error) {
    console.warn('保存选择的分组失败:', error)
  }
}

// 选择分组
const selectCategory = (categoryId: number | null) => {
  selectedCategoryId.value = categoryId
  saveSelectedCategory(categoryId)
  showCategoryDropdown.value = false
}

// 获取当前选择的分组名称
const selectedCategoryName = computed(() => {
  if (selectedCategoryId.value === null) {
    return '全部任务'
  }
  const category = todoCategories.value.find(c => c.id === selectedCategoryId.value)
  return category ? category.name : '未知分组'
})

// 点击外部关闭下拉框
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  const dropdown = document.querySelector('.category-dropdown')
  if (dropdown && !dropdown.contains(target)) {
    showCategoryDropdown.value = false
  }
}

// 组件挂载时加载数据
onMounted(async () => {
  // 先加载保存的分组选择
  loadSelectedCategory()

  await loadTodos()
  await fetchTodoCategories()

  // 验证保存的分组ID是否仍然有效
  await nextTick(() => {
    if (selectedCategoryId.value !== null) {
      const categoryExists = todoCategories.value.some(c => c.id === selectedCategoryId.value)
      if (!categoryExists) {
        // 如果保存的分组不存在，重置为全部任务
        selectedCategoryId.value = null
        saveSelectedCategory(null)
      }
    }
  })

  // 添加点击外部关闭下拉框的事件监听
  document.addEventListener('click', handleClickOutside)
})

// 组件卸载时移除事件监听
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <NuxtLayout>
    <div class="todo-dashboard">
      <!-- 页面标题 -->
      <div class="dashboard-header">
        <div class="header-left">
          <h1 class="dashboard-title">待办事项</h1>
          <div class="stats-summary">
            <div class="stat-item">
              <Icon icon="material-symbols:task-alt" class="stat-icon" />
              <span class="stat-value">{{ todoStats.total }}</span>
              <span class="stat-label">总计</span>
            </div>
            <div class="stat-item">
              <Icon icon="material-symbols:radio-button-unchecked" class="stat-icon active" />
              <span class="stat-value">{{ todoStats.active }}</span>
              <span class="stat-label">待完成</span>
            </div>
            <div class="stat-item">
              <Icon icon="material-symbols:check-circle" class="stat-icon completed" />
              <span class="stat-value">{{ todoStats.completed }}</span>
              <span class="stat-label">已完成</span>
            </div>
          </div>
        </div>

        <!-- 分组选择下拉框 -->
        <div class="header-right">
          <div class="category-selector">
            <div class="category-dropdown" @click.stop>
              <button
                class="category-dropdown-trigger"
                @click="showCategoryDropdown = !showCategoryDropdown"
                :disabled="categoriesLoading"
              >
                <Icon icon="mdi:format-list-checks" class="dropdown-icon" />
                <span class="dropdown-text">{{ selectedCategoryName }}</span>
                <Icon
                  icon="mdi:chevron-down"
                  class="dropdown-arrow"
                  :class="{ 'rotated': showCategoryDropdown }"
                />
              </button>

              <Transition name="dropdown">
                <div v-if="showCategoryDropdown" class="category-dropdown-menu">
                  <div class="dropdown-item" @click="selectCategory(null)">
                    <Icon icon="mdi:view-list" class="item-icon" />
                    <span class="item-text">全部任务</span>
                    <Icon
                      v-if="selectedCategoryId === null"
                      icon="mdi:check"
                      class="check-icon"
                    />
                  </div>

                  <div class="dropdown-divider"></div>

                  <div
                    v-for="category in todoCategories"
                    :key="category.id"
                    class="dropdown-item"
                    @click="selectCategory(category.id)"
                  >
                    <Icon icon="mdi:folder" class="item-icon" />
                    <span class="item-text">{{ category.name }}</span>
                    <Icon
                      v-if="selectedCategoryId === category.id"
                      icon="mdi:check"
                      class="check-icon"
                    />
                  </div>

                  <div v-if="todoCategories.length === 0 && !categoriesLoading" class="dropdown-empty">
                    <Icon icon="mdi:folder-off" class="empty-icon" />
                    <span>暂无分组</span>
                  </div>

                  <div v-if="categoriesLoading" class="dropdown-loading">
                    <Icon icon="mdi:loading" class="loading-icon spin" />
                    <span>加载中...</span>
                  </div>
                </div>
              </Transition>
            </div>
          </div>
        </div>
      </div>

      <!-- 添加任务区域 -->
      <Motion
        :initial="{ opacity: 0, y: 20 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.5, delay: 0.1 }"
      >

        <div class="add-todo-form">
          <input
              v-model="newTodoText"
              @keyup="handleKeyup"
              type="text"
              placeholder="输入新任务..."
              class="todo-input"
              maxlength="200"
          />
          <button
              @click="addTodo"
              :disabled="!newTodoText.trim()"
              class="add-btn"
              title="添加任务"
          >
            <Icon icon="material-symbols:add" class="add-btn-icon" />
          </button>
        </div>
      </Motion>

      <!-- 主要内容区域 - 两列布局 -->
      <div class="todo-content">
        <!-- 黄金比例容器 -->
        <div class="golden-ratio-container">

          <!-- 左列：待办事项 -->
          <Motion
            :initial="{ opacity: 0, x: -20 }"
            :animate="{ opacity: 1, x: 0 }"
            :transition="{ duration: 0.5, delay: 0.2 }"
          >
            <div class="todo-column active-column">
              <div class="column-header">
                <div class="column-title">
                  <Icon icon="material-symbols:radio-button-unchecked" class="column-icon" />
                  <h3>待办事项</h3>
                  <span class="count-badge">{{ activeTodos.length }}</span>
                </div>
                <div class="column-actions" v-if="activeTodos.length > 0">
                  <button
                    @click="toggleAll"
                    class="todo-action-btn"
                    :title="allCompleted ? '全部标记为未完成' : '全部标记为完成'"
                  >
                    <Icon icon="material-symbols:check-box-outline-blank" />
                  </button>
                </div>
              </div>

              <div class="column-content">
                <!-- 待办任务列表 -->
                <VueDraggable
                  v-model="activeTodos"
                  @end="onActiveDragEnd"
                  group="todos"
                  ghost-class="ghost-item"
                  chosen-class="chosen-item"
                  drag-class="drag-item"
                  :animation="200"
                  class="todo-list active-list"
                >
                  <div
                    v-for="todo in activeTodos"
                    :key="todo.id"
                    class="todo-item active-item draggable-item"
                  >
                    <!-- 任务内容 -->
                    <div class="todo-content-area" @dblclick="startEdit(todo)">
                      <span class="todo-text">{{ todo.text }}</span>
                    </div>

                    <!-- 操作按钮 -->
                    <div class="todo-actions">
                      <button
                        @click="startEdit(todo)"
                        class="todo-action-btn edit-btn"
                        title="编辑任务"
                      >
                        <Icon icon="material-symbols:edit" />
                      </button>
                      <button
                        @click="deleteTodo(todo.id)"
                        class="todo-action-btn delete-btn"
                        title="删除任务"
                      >
                        <Icon icon="material-symbols:delete" />
                      </button>
                    </div>
                  </div>
                </VueDraggable>

                <!-- 空状态 -->
                <div v-if="activeTodos.length === 0" class="empty-state">
                  <div class="empty-icon">
                    <Icon icon="material-symbols:radio-button-unchecked" />
                  </div>
                  <h4 class="empty-title">没有待办事项</h4>
                  <p class="empty-subtitle">添加一个新任务开始吧！</p>
                </div>
              </div>
            </div>
          </Motion>

          <!-- 右列：已完成事项 -->
          <Motion
            :initial="{ opacity: 0, x: 20 }"
            :animate="{ opacity: 1, x: 0 }"
            :transition="{ duration: 0.5, delay: 0.3 }"
          >
            <div class="todo-column completed-column">
              <div class="column-header">
                <div class="column-title">
                  <Icon icon="material-symbols:check-circle" class="column-icon completed" />
                  <h3>已完成</h3>
                  <span class="count-badge completed">{{ completedTodos.length }}</span>
                </div>
                <div class="column-actions" v-if="completedTodos.length > 0">
                  <button
                    @click="clearCompleted"
                    class="todo-action-btn clear-btn"
                    title="清除已完成任务"
                  >
                    <Icon icon="material-symbols:delete-sweep" />
                  </button>
                </div>
              </div>

              <div class="column-content">
                <!-- 已完成任务列表 -->
                <VueDraggable
                  v-model="completedTodos"
                  @end="onCompletedDragEnd"
                  group="todos"
                  ghost-class="ghost-item"
                  chosen-class="chosen-item"
                  drag-class="drag-item"
                  :animation="200"
                  class="todo-list completed-list"
                >
                  <div
                    v-for="todo in completedTodos"
                    :key="todo.id"
                    class="todo-item completed-item draggable-item"
                  >
                    <!-- 任务内容 -->
                    <div class="todo-content-area" @dblclick="startEdit(todo)">
                      <span class="todo-text completed">{{ todo.text }}</span>
                    </div>

                    <!-- 操作按钮 -->
                    <div class="todo-actions">
                      <button
                        @click="startEdit(todo)"
                        class="todo-action-btn edit-btn"
                        title="编辑任务"
                      >
                        <Icon icon="material-symbols:edit" />
                      </button>
                      <button
                        @click="deleteTodo(todo.id)"
                        class="todo-action-btn delete-btn"
                        title="删除任务"
                      >
                        <Icon icon="material-symbols:delete" />
                      </button>
                    </div>
                  </div>
                </VueDraggable>

                <!-- 空状态 -->
                <div v-if="completedTodos.length === 0" class="empty-state">
                  <div class="empty-icon">
                    <Icon icon="material-symbols:check-circle" />
                  </div>
                  <h4 class="empty-title">没有已完成的任务</h4>
                  <p class="empty-subtitle">完成一些任务来看看效果吧！</p>
                </div>
              </div>
            </div>
          </Motion>
        </div>
      </div>
    </div>

    <!-- 编辑模态框 -->
    <Teleport to="body">
      <div
        v-if="showEditModal"
        class="edit-modal-overlay"
        @click="closeModalOnClickOutside"
      >
        <Motion
          :initial="{ opacity: 0, scale: 0.9 }"
          :animate="{ opacity: 1, scale: 1 }"
          :exit="{ opacity: 0, scale: 0.9 }"
          :transition="{ duration: 0.2 }"
        >
          <div class="edit-modal" @click.stop>
            <div class="edit-modal-header">
              <h3 class="edit-modal-title">编辑任务</h3>
              <button
                @click="cancelEdit"
                class="edit-modal-close"
                title="关闭"
              >
                <Icon icon="material-symbols:close" />
              </button>
            </div>

            <div class="edit-modal-body">
              <textarea
                v-model="editingText"
                @keyup="handleModalKeyup"
                class="edit-modal-textarea"
                placeholder="输入任务内容..."
                maxlength="500"
                rows="6"
                autofocus
              ></textarea>
              <div class="edit-modal-info">
                <span class="char-count">{{ editingText.length }}/500</span>
                <span class="edit-hint">Ctrl+Enter 保存，Esc 取消</span>
              </div>
            </div>

            <div class="edit-modal-footer">
              <button
                @click="cancelEdit"
                class="edit-modal-btn cancel-btn"
              >
                取消
              </button>
              <button
                @click="saveEdit"
                class="edit-modal-btn save-btn"
                :disabled="!editingText.trim()"
              >
                保存
              </button>
            </div>
          </div>
        </Motion>
      </div>
    </Teleport>
  </NuxtLayout>
</template>

<style scoped>
/* Todo应用样式 */
.todo-dashboard {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
  min-height: 100vh;
}

/* 页面标题区域 */
.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 2rem;
  flex-wrap: wrap;
  gap: 1rem;
  padding-left: 0.5rem;
  padding-right: 0.5rem;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 2rem;
  flex-wrap: wrap;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.dashboard-title {
  font-size: 1.4rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

/* 统计摘要 */
.stats-summary {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: border-color 0.3s ease;
}

.stat-item:hover {
  border-color: rgba(255, 255, 255, 0.2);
}

.stat-icon {
  width: 1.25rem;
  height: 1.25rem;
  color: rgba(255, 255, 255, 0.8);
}

.stat-icon.active {
  color: rgba(59, 130, 246, 0.9);
}

.stat-icon.completed {
  color: rgba(34, 197, 94, 0.9);
}

.stat-value {
  font-size: 1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  min-width: 1.5rem;
  text-align: center;
}

.stat-label {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 500;
}

/* 分组选择器样式 */
.category-selector {
  position: relative;
}

.category-dropdown {
  position: relative;
}

.category-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 160px;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.category-dropdown-trigger:hover:not(:disabled) {
  border-color: rgba(255, 255, 255, 0.3);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.12) 0%,
    rgba(255, 255, 255, 0.06) 100%
  );
  transform: translateY(-1px);
}

.category-dropdown-trigger:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.dropdown-icon {
  width: 1.25rem;
  height: 1.25rem;
  color: rgba(34, 197, 94, 0.8);
  flex-shrink: 0;
}

.dropdown-text {
  flex: 1;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dropdown-arrow {
  width: 1rem;
  height: 1rem;
  color: rgba(255, 255, 255, 0.6);
  transition: transform 0.3s ease;
  flex-shrink: 0;
}

.dropdown-arrow.rotated {
  transform: rotate(180deg);
}

.category-dropdown-menu {
  position: absolute;
  top: calc(100% + 0.5rem);
  right: 0;
  min-width: 220px;
  max-height: 300px;
  overflow-y: auto;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 100%
  );
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.75rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  z-index: 1000;
  padding: 0.5rem 0;
}


.dropdown-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.8);
}

.dropdown-item:hover {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  color: rgba(255, 255, 255, 0.95);
}

.item-icon {
  width: 1rem;
  height: 1rem;
  color: rgba(255, 255, 255, 0.6);
  flex-shrink: 0;
}

.item-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.check-icon {
  width: 1rem;
  height: 1rem;
  color: rgba(34, 197, 94, 0.8);
  flex-shrink: 0;
}

.dropdown-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.1);
  margin: 0.5rem 0;
}

.dropdown-empty,
.dropdown-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1rem;
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.875rem;
}

.empty-icon,
.loading-icon {
  width: 1rem;
  height: 1rem;
}

.loading-icon.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 下拉框动画 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.3s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.95);
}

.dropdown-enter-to,
.dropdown-leave-from {
  opacity: 1;
  transform: translateY(0) scale(1);
}



/* 主要内容区域 - 两列布局 */
.todo-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.golden-ratio-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;
  min-height: 600px;
}

/* 列样式 */
.todo-column {
  display: flex;
  flex-direction: column;
  height: 100%;
  border-radius: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.05) 0%,
    rgba(255, 255, 255, 0.02) 100%
  );
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  overflow: hidden;
  transition: border-color 0.3s ease, transform 0.2s ease;
}

.todo-column:hover {
  border-color: rgba(255, 255, 255, 0.2);
}

.active-column {
  border-color: rgba(59, 130, 246, 0.2);
}

.completed-column {
  border-color: rgba(34, 197, 94, 0.2);
}

/* 列头部 */
.column-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
}

.column-title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.column-title h3 {
  font-size: 1.1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.column-icon {
  width: 1.25rem;
  height: 1.25rem;
  color: rgba(59, 130, 246, 0.8);
}

.column-icon.completed {
  color: rgba(34, 197, 94, 0.8);
}

.count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.5rem;
  height: 1.5rem;
  padding: 0 0.5rem;
  border-radius: 0.75rem;
  background: rgba(59, 130, 246, 0.2);
  color: rgba(59, 130, 246, 0.9);
  font-size: 0.75rem;
  font-weight: 600;
}

.count-badge.completed {
  background: rgba(34, 197, 94, 0.2);
  color: rgba(34, 197, 94, 0.9);
}

.column-actions {
  display: flex;
  gap: 0.5rem;
}

.column-actions .todo-action-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.column-actions .todo-action-btn:hover:not(:disabled) {
  color: rgba(255, 255, 255, 0.9);
  border-color: rgba(255, 255, 255, 0.3);
}

.column-actions .todo-action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.column-actions .todo-action-btn.clear-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.15) 0%,
    rgba(239, 68, 68, 0.08) 100%
  );
  border-color: rgba(239, 68, 68, 0.3);
  color: rgba(239, 68, 68, 0.9);
}

.column-actions .todo-action-btn svg {
  width: 1rem;
  height: 1rem;
}

/* 列内容 */
.column-content {
  flex: 1;
  padding: 1rem;
  overflow-y: auto;
}

/* 添加任务表单 */
.add-todo-form {
  margin-bottom: 2rem;
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.todo-input {
  flex: 1;
  padding: 0.5rem 0.75rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
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

.todo-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.todo-input:focus {
  outline: none;
  border-color: rgba(59, 130, 246, 0.5);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.12) 0%,
    rgba(255, 255, 255, 0.06) 100%
  );
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.add-btn {
  width: 2rem;
  height: 2rem;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(59, 130, 246, 0.3);
  cursor: pointer;
  transition: all 0.3s ease;
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.15) 0%,
    rgba(59, 130, 246, 0.08) 100%
  );
  color: rgba(59, 130, 246, 0.9);
}

.add-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.25) 0%,
    rgba(59, 130, 246, 0.15) 100%
  );
  border-color: rgba(59, 130, 246, 0.5);
  color: rgba(59, 130, 246, 1);
  transform: translateY(-1px);
}

.add-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.add-btn-icon {
  width: 1rem;
  height: 1rem;
}

/* 筛选控制 */
.filter-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.filter-buttons {
  display: flex;
  gap: 0.5rem;
}

.filter-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.filter-btn:hover {
  color: rgba(255, 255, 255, 0.9);
  border-color: rgba(255, 255, 255, 0.3);
}

.filter-btn.active {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.15) 0%,
    rgba(59, 130, 246, 0.08) 100%
  );
  border-color: rgba(59, 130, 246, 0.3);
  color: rgba(59, 130, 246, 0.9);
}

.filter-btn svg {
  width: 1rem;
  height: 1rem;
}



/* 批量操作 */
.bulk-actions {
  display: flex;
  gap: 0.5rem;
}

.bulk-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.bulk-btn:hover:not(:disabled) {
  color: rgba(255, 255, 255, 0.9);
  border-color: rgba(255, 255, 255, 0.3);
}

.bulk-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.bulk-btn.clear-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.15) 0%,
    rgba(239, 68, 68, 0.08) 100%
  );
  border-color: rgba(239, 68, 68, 0.3);
  color: rgba(239, 68, 68, 0.9);
}

.bulk-btn svg {
  width: 1rem;
  height: 1rem;
}

/* 拖拽提示卡片 */
.drag-hint-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.15) 0%,
    rgba(59, 130, 246, 0.08) 100%
  );
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.drag-hint-card .hint-icon {
  width: 1.25rem;
  height: 1.25rem;
  color: rgba(59, 130, 246, 0.8);
  flex-shrink: 0;
}

.drag-hint-card .hint-text {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.4;
}

/* 任务列表 - 网格布局 */
.todo-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 0.25rem;
  //height: 100%;
  padding: 0.125rem;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 1.5rem 1rem;
  text-align: center;
  height: 150px;
  border-radius: 0.5rem;
  border: 2px dashed rgba(255, 255, 255, 0.1);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.02) 0%,
    rgba(255, 255, 255, 0.01) 100%
  );
  grid-column: 1 / -1; /* 占满整行 */
}

.empty-icon {
  width: 3rem;
  height: 3rem;
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
}

.empty-icon svg {
  width: 1.5rem;
  height: 1.5rem;
  color: rgba(255, 255, 255, 0.4);
}

.empty-title {
  font-size: 1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.7);
  margin: 0 0 0.5rem 0;
}

.empty-subtitle {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

/* 任务项 - 拉长的卡片显示更多内容 */
.todo-item {
  width: 100%;
  display: flex;
  align-items: stretch;
  gap: 0.5rem;
  padding: 0.75rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  transition: all 0.3s ease;
  /* 拉长比例：宽度:高度 = 1:0.4 */
  aspect-ratio: 1 / 0.4;
  min-height: 60px;
  max-height: 100px;
  position: relative;
  overflow: hidden;
}

.todo-item:hover {
  border-color: rgba(255, 255, 255, 0.2);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.12) 0%,
    rgba(255, 255, 255, 0.06) 100%
  );
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.active-item {
  border-color: rgba(59, 130, 246, 0.3);
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.08) 0%,
    rgba(59, 130, 246, 0.04) 100%
  );
}

.active-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(to bottom, rgba(59, 130, 246, 0.8), rgba(59, 130, 246, 0.4));
}

.completed-item {
  border-color: rgba(34, 197, 94, 0.3);
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.08) 0%,
    rgba(34, 197, 94, 0.04) 100%
  );
  opacity: 0.9;
}

.completed-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(to bottom, rgba(34, 197, 94, 0.8), rgba(34, 197, 94, 0.4));
}

/* 可拖拽项样式 */
.draggable-item {
  cursor: grab;
  transition: all 0.3s ease;
}

.draggable-item:active {
  cursor: grabbing;
}

.draggable-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 拖拽状态样式 */
.ghost-item {
  opacity: 0.5;
  border: 2px dashed rgba(59, 130, 246, 0.5);
  transform: rotate(2deg);
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.1) 0%,
    rgba(59, 130, 246, 0.05) 100%
  );
}

.chosen-item {
  transform: scale(1.02);
  border-color: rgba(59, 130, 246, 0.5);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

.drag-item {
  transform: rotate(5deg);
  opacity: 0.8;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
}

/* 拖拽区域提示 */
.todo-list {
  min-height: 100px;
  border-radius: 0.5rem;
  transition: all 0.3s ease;
}

.todo-list:empty::before {
  content: '拖拽任务到这里';
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100px;
  color: rgba(255, 255, 255, 0.4);
  font-size: 0.875rem;
  border: 2px dashed rgba(255, 255, 255, 0.1);
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.02) 0%,
    rgba(255, 255, 255, 0.01) 100%
  );
}


/* 任务内容区域 */
.todo-content-area {
  flex: 1;
  min-width: 0;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0.125rem 0;
}

.todo-text {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.3;
  word-break: break-word;
  transition: all 0.3s ease;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-weight: 500;
}

.todo-text.completed {
  text-decoration: line-through;
  color: rgba(255, 255, 255, 0.5);
}



/* 任务操作按钮 */
.todo-actions {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  align-items: center;
  justify-content: flex-start;
  padding-top: 0.125rem;
  flex-shrink: 0;
}

.todo-actions .todo-action-btn {
  width: 2rem;
  height: 2rem;
  border-radius: 0.125rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
}

.todo-action-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: scale(1.1);
}

.todo-action-btn svg {
  width: 0.75rem;
  height: 0.75rem;
  color: rgba(255, 255, 255, 0.8);
}

.todo-action-btn.edit-btn:hover {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.15) 0%,
    rgba(59, 130, 246, 0.08) 100%
  );
  border-color: rgba(59, 130, 246, 0.3);
  color: rgba(59, 130, 246, 0.9);
}

.todo-action-btn.edit-btn:hover svg {
  color: rgba(59, 130, 246, 0.9);
}

.todo-action-btn.delete-btn:hover {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.15) 0%,
    rgba(239, 68, 68, 0.08) 100%
  );
  border-color: rgba(239, 68, 68, 0.3);
  color: rgba(239, 68, 68, 0.9);
}

.todo-action-btn.delete-btn:hover svg {
  color: rgba(239, 68, 68, 0.9);
}

/* 编辑模态框样式 */
.edit-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.edit-modal {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 100%
  );
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 1rem;
  width: 100%;
  max-width: 800px;
  min-width: 50vw;
  max-height: 80vh;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.edit-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
}

.edit-modal-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.edit-modal-close {
  width: 2rem;
  height: 2rem;
  border-radius: 0.5rem;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.edit-modal-close:hover {
  background: rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.9);
}

.edit-modal-close svg {
  width: 1.25rem;
  height: 1.25rem;
}

.edit-modal-body {
  padding: 1.5rem;
}

.edit-modal-textarea {
  width: 100%;
  min-height: 200px;
  padding: 1rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  color: rgba(255, 255, 255, 0.9);
  font-size: 1rem;
  line-height: 1.5;
  resize: vertical;
  transition: all 0.3s ease;
  font-family: inherit;
}

.edit-modal-textarea::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.edit-modal-textarea:focus {
  outline: none;
  border-color: rgba(59, 130, 246, 0.5);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 100%
  );
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.edit-modal-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.75rem;
  font-size: 0.875rem;
}

.char-count {
  color: rgba(255, 255, 255, 0.6);
}

.edit-hint {
  color: rgba(255, 255, 255, 0.5);
  font-style: italic;
}

.edit-modal-footer {
  display: flex;
  gap: 0.75rem;
  padding: 1.5rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.05) 0%,
    rgba(255, 255, 255, 0.02) 100%
  );
  justify-content: flex-end;
}

.edit-modal-btn {
  padding: 0.75rem 1.5rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 80px;
}

.cancel-btn {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  color: rgba(255, 255, 255, 0.7);
}

.cancel-btn:hover {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 100%
  );
  color: rgba(255, 255, 255, 0.9);
  border-color: rgba(255, 255, 255, 0.3);
}

.save-btn {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.2) 0%,
    rgba(59, 130, 246, 0.1) 100%
  );
  color: rgba(59, 130, 246, 0.9);
  border-color: rgba(59, 130, 246, 0.3);
}

.save-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.3) 0%,
    rgba(59, 130, 246, 0.15) 100%
  );
  color: rgba(59, 130, 246, 1);
  border-color: rgba(59, 130, 246, 0.5);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

.save-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 响应式样式 */
@media (max-width: 768px) {
  .dashboard-header {
    flex-direction: column;
    align-items: stretch;
    gap: 1.5rem;
  }

  .header-left {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }

  .header-right {
    justify-content: center;
  }

  .stats-summary {
    justify-content: center;
    flex-wrap: wrap;
  }

  .category-dropdown-trigger {
    min-width: 200px;
  }

  .category-dropdown-menu {
    left: 0;
    right: 0;
    min-width: auto;
  }

  .golden-ratio-container {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }
}
</style>