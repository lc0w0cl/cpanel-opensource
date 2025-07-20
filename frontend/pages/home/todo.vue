<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { Motion } from "motion-v"
import { Icon } from '@iconify/vue'
import { VueDraggable } from 'vue-draggable-plus'

// 子页面不需要定义 layout 和 middleware，由父页面处理

// 导入API函数和类型
import { useTodoApi, type Todo } from '~/composables/useTodoApi'

// 筛选类型
type FilterType = 'all' | 'active' | 'completed'

// 初始化API
const todoApi = useTodoApi()



// 响应式数据
const todos = ref<Todo[]>([])
const newTodoText = ref('')
const currentFilter = ref<FilterType>('all')
const editingId = ref<number | null>(null)
const editingText = ref('')

// 计算属性
const sortedTodos = computed(() => {
  // 默认按sortOrder排序，如果没有sortOrder则按创建时间倒序
  return [...todos.value].sort((a, b) => {
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

// 添加新任务
const addTodo = async () => {
  const text = newTodoText.value.trim()
  if (!text) return

  try {
    const newTodo = await todoApi.createTodo(text)
    if (newTodo) {
      todos.value.unshift(newTodo)
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

// 开始编辑
const startEdit = (todo: Todo) => {
  editingId.value = todo.id
  editingText.value = todo.text

  // 下一个tick时聚焦输入框
  nextTick(() => {
    const editInput = document.querySelector('.todo-edit-input') as HTMLInputElement
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

  if (editingId.value) {
    try {
      const success = await todoApi.updateTodoText(editingId.value, text)
      if (success) {
        const todo = todos.value.find(t => t.id === editingId.value)
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
  editingId.value = null
  editingText.value = ''
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



// 拖拽排序结束处理
const onDragEnd = async () => {
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
    if (editingId.value) {
      saveEdit()
    } else {
      addTodo()
    }
  } else if (event.key === 'Escape' && editingId.value) {
    cancelEdit()
  }
}

// 组件挂载时加载数据
onMounted(async () => {
  await loadTodos()
})
</script>

<template>
  <NuxtLayout>
    <div class="todo-dashboard !mx-0">
      <!-- 页面标题 -->
      <div class="dashboard-header">
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

      <!-- 添加任务区域 -->
      <Motion
        :initial="{ opacity: 0, y: 20 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.5, delay: 0.1 }"
      >
        <div class="add-todo-section">
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
                    class="action-btn"
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
                  @end="onDragEnd"
                  handle=".drag-handle"
                  ghost-class="ghost-item"
                  chosen-class="chosen-item"
                  drag-class="drag-item"
                  :animation="200"
                  class="todo-list"
                >
                  <div
                    v-for="todo in activeTodos"
                    :key="todo.id"
                    class="todo-item active-item"
                  >
                    <!-- 拖拽手柄 -->
                    <div class="drag-handle" title="拖拽排序">
                      <Icon icon="material-symbols:drag-indicator" class="drag-icon" />
                    </div>

                    <!-- 完成状态切换 -->
                    <button
                      @click="toggleTodo(todo.id)"
                      class="todo-checkbox"
                      title="标记为完成"
                    >
                      <Icon icon="material-symbols:radio-button-unchecked" class="checkbox-icon" />
                    </button>

                    <!-- 任务内容 -->
                    <div class="todo-content-area" @dblclick="startEdit(todo)">
                      <textarea
                        v-if="editingId === todo.id"
                        v-model="editingText"
                        @keyup="handleKeyup"
                        @blur="saveEdit"
                        class="todo-edit-input"
                        maxlength="200"
                        rows="2"
                      ></textarea>
                      <span v-else class="todo-text">{{ todo.text }}</span>
                    </div>

                    <!-- 操作按钮 -->
                    <div class="todo-actions">
                      <button
                        v-if="editingId !== todo.id"
                        @click="startEdit(todo)"
                        class="action-btn edit-btn"
                        title="编辑任务"
                      >
                        <Icon icon="material-symbols:edit" />
                      </button>
                      <button
                        @click="deleteTodo(todo.id)"
                        class="action-btn delete-btn"
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
                    class="action-btn clear-btn"
                    title="清除已完成任务"
                  >
                    <Icon icon="material-symbols:delete-sweep" />
                  </button>
                </div>
              </div>

              <div class="column-content">
                <!-- 已完成任务列表 -->
                <div class="todo-list">
                  <div
                    v-for="todo in completedTodos"
                    :key="todo.id"
                    class="todo-item completed-item"
                  >
                    <!-- 完成状态切换 -->
                    <button
                      @click="toggleTodo(todo.id)"
                      class="todo-checkbox"
                      title="标记为未完成"
                    >
                      <Icon icon="material-symbols:check-circle" class="checkbox-icon completed" />
                    </button>

                    <!-- 任务内容 -->
                    <div class="todo-content-area" @dblclick="startEdit(todo)">
                      <textarea
                        v-if="editingId === todo.id"
                        v-model="editingText"
                        @keyup="handleKeyup"
                        @blur="saveEdit"
                        class="todo-edit-input"
                        maxlength="200"
                        rows="2"
                      ></textarea>
                      <span v-else class="todo-text completed">{{ todo.text }}</span>
                    </div>

                    <!-- 操作按钮 -->
                    <div class="todo-actions">
                      <button
                        v-if="editingId !== todo.id"
                        @click="startEdit(todo)"
                        class="action-btn edit-btn"
                        title="编辑任务"
                      >
                        <Icon icon="material-symbols:edit" />
                      </button>
                      <button
                        @click="deleteTodo(todo.id)"
                        class="action-btn delete-btn"
                        title="删除任务"
                      >
                        <Icon icon="material-symbols:delete" />
                      </button>
                    </div>
                  </div>
                </div>

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

/* 添加任务区域 */
.add-todo-section {
  margin-bottom: 2rem;
  padding: 1.5rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
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
  padding: 1.5rem;
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

/* 列内容 */
.column-content {
  flex: 1;
  padding: 1rem;
  overflow-y: auto;
}

/* 添加任务表单 */
.add-todo-form {
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
  display: flex;
  align-items: stretch;
  gap: 0.25rem;
  padding: 0.5rem;
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

.todo-item.draggable {
  cursor: grab;
}

.todo-item.draggable:active {
  cursor: grabbing;
}

/* 拖拽手柄 */
.drag-handle {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  width: 1rem;
  height: 1rem;
  padding-top: 0.125rem;
  cursor: grab;
  border-radius: 0.125rem;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.drag-handle:hover {
  background: rgba(255, 255, 255, 0.1);
}

.drag-handle:active {
  cursor: grabbing;
}

.drag-icon {
  width: 0.75rem;
  height: 0.75rem;
  color: rgba(255, 255, 255, 0.4);
  transition: all 0.3s ease;
}

.drag-handle:hover .drag-icon {
  color: rgba(255, 255, 255, 0.7);
}

/* 拖拽状态样式 */
.ghost-item {
  opacity: 0.5;
  border: 2px dashed rgba(59, 130, 246, 0.5);
  transform: rotate(2deg);
}

.chosen-item {
  transform: scale(1.02);
  border-color: rgba(59, 130, 246, 0.5);
}

.drag-item {
  transform: rotate(5deg);
  opacity: 0.8;
}

/* 复选框 */
.todo-checkbox {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  width: 1rem;
  height: 1rem;
  padding-top: 0.125rem;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 0.125rem;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.todo-checkbox:hover {
  background: rgba(255, 255, 255, 0.08);
}

.checkbox-icon {
  width: 0.875rem;
  height: 0.875rem;
  color: rgba(255, 255, 255, 0.6);
  transition: all 0.3s ease;
}

.checkbox-icon.completed {
  color: rgba(34, 197, 94, 0.9);
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

.todo-edit-input {
  width: 100%;
  padding: 0.25rem;
  border-radius: 0.25rem;
  border: 1px solid rgba(59, 130, 246, 0.5);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.12) 0%,
    rgba(255, 255, 255, 0.06) 100%
  );
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.8rem;
  line-height: 1.3;
  transition: all 0.3s ease;
  resize: none;
  min-height: 35px;
  max-height: 50px;
}

.todo-edit-input:focus {
  outline: none;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
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

.todo-actions .action-btn {
  width: 2rem;
  height: 1rem;
  border-radius: 0.125rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: scale(1.1);
}

.action-btn svg {
  width: 0.75rem;
  height: 0.75rem;
  color: rgba(255, 255, 255, 0.8);
}

.edit-btn:hover {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.15) 0%,
    rgba(59, 130, 246, 0.08) 100%
  );
  border-color: rgba(59, 130, 246, 0.3);
  color: rgba(59, 130, 246, 0.9);
}

.edit-btn:hover svg {
  color: rgba(59, 130, 246, 0.9);
}

.delete-btn:hover {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.15) 0%,
    rgba(239, 68, 68, 0.08) 100%
  );
  border-color: rgba(239, 68, 68, 0.3);
  color: rgba(239, 68, 68, 0.9);
}

.delete-btn:hover svg {
  color: rgba(239, 68, 68, 0.9);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .todo-dashboard {
    padding: 1.5rem;
  }

  .golden-ratio-container {
    grid-template-columns: 1fr;
    grid-template-rows: 1fr 1fr;
    min-height: 800px;
    gap: 1.5rem;
  }

  .todo-list {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 0.25rem;
  }

  .todo-item {
    min-height: 50px;
    padding: 0.375rem;
    gap: 0.125rem;
    aspect-ratio: 1 / 0.35;
  }

  .stats-summary {
    gap: 1rem;
  }

  .stat-item {
    padding: 0.5rem 0.75rem;
  }

  .stat-value {
    font-size: 1rem;
  }

  .stat-label {
    font-size: 0.8rem;
  }
}

@media (max-width: 768px) {
  .todo-dashboard {
    padding: 1rem;
  }

  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }

  .dashboard-title {
    font-size: 1.25rem;
  }

  .stats-summary {
    width: 100%;
    justify-content: space-between;
  }

  .add-todo-section {
    padding: 1rem;
  }

  .golden-ratio-container {
    min-height: 700px;
    gap: 1rem;
  }

  .column-header {
    padding: 1rem;
  }

  .column-content {
    padding: 0.75rem;
  }

  .todo-list {
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 0.25rem;
  }

  .todo-item {
    padding: 0.25rem;
    gap: 0.125rem;
    min-height: 50px;
    aspect-ratio: 1 / 0.3;
  }

  .todo-actions {
    flex-direction: row;
    gap: 0.125rem;
    padding-top: 0;
  }

  .action-btn {
    width: 0.875rem;
    height: 0.875rem;
  }

  .action-btn svg {
    width: 0.625rem;
    height: 0.625rem;
  }

  .todo-actions {
    gap: 0.25rem;
  }
}

@media (max-width: 480px) {
  .todo-dashboard {
    padding: 0.75rem;
  }

  .dashboard-title {
    font-size: 1.125rem;
  }

  .stats-summary {
    flex-direction: column;
    gap: 0.75rem;
  }

  .stat-item {
    width: 100%;
    justify-content: center;
  }

  .add-todo-section {
    padding: 0.75rem;
  }

  .add-todo-form {
    flex-direction: column;
    gap: 0.75rem;
  }

  .todo-input {
    width: 100%;
  }

  .add-btn {
    width: 100%;
    height: 2.5rem;
  }

  .golden-ratio-container {
    min-height: 600px;
    gap: 0.75rem;
  }

  .column-header {
    padding: 0.75rem;
  }

  .column-title h3 {
    font-size: 1rem;
  }

  .column-content {
    padding: 0.5rem;
  }

  .todo-list {
    grid-template-columns: 1fr;
    gap: 0.125rem;
  }

  .todo-item {
    padding: 0.25rem;
    gap: 0.125rem;
    min-height: 40px;
    /* 在小屏幕上更扁平的比例 */
    aspect-ratio: 1 / 0.25;
  }

  .todo-text {
    font-size: 0.75rem;
    -webkit-line-clamp: 2;
  }

  .todo-actions {
    flex-direction: row;
    gap: 0.125rem;
    padding-top: 0;
  }

  .action-btn {
    width: 0.75rem;
    height: 0.75rem;
  }

  .action-btn svg {
    width: 0.5rem;
    height: 0.5rem;
  }

  .drag-handle {
    width: 0.75rem;
    height: 0.75rem;
  }

  .drag-icon {
    width: 0.5rem;
    height: 0.5rem;
  }

  .todo-checkbox {
    width: 0.75rem;
    height: 0.75rem;
  }

  .checkbox-icon {
    width: 0.625rem;
    height: 0.625rem;
  }

  .drag-handle {
    width: 1.25rem;
    height: 1.25rem;
  }

  .drag-icon {
    width: 0.875rem;
    height: 0.875rem;
  }

  .todo-checkbox {
    width: 1.5rem;
    height: 1.5rem;
  }

  .checkbox-icon {
    width: 1rem;
    height: 1rem;
  }

  .action-btn {
    width: 1.5rem;
    height: 1.5rem;
  }

  .action-btn svg {
    width: 0.75rem;
    height: 0.75rem;
  }

  .empty-state {
    padding: 2rem 1rem;
    height: 150px;
  }

  .empty-icon {
    width: 2.5rem;
    height: 2.5rem;
    margin-bottom: 0.75rem;
  }

  .empty-icon svg {
    width: 1.25rem;
    height: 1.25rem;
  }

  .empty-title {
    font-size: 0.9rem;
  }

  .empty-subtitle {
    font-size: 0.75rem;
  }
}
</style>