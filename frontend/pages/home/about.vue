<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { Motion } from "motion-v"
import { Icon } from '@iconify/vue'
import { VueDraggable } from 'vue-draggable-plus'

// 子页面不需要定义 layout 和 middleware，由父页面处理

// 任务接口定义
interface Todo {
  id: number
  text: string
  completed: boolean
  createdAt: string
  updatedAt: string
  sortOrder?: number
}

// 筛选类型
type FilterType = 'all' | 'active' | 'completed'



// 响应式数据
const todos = ref<Todo[]>([])
const newTodoText = ref('')
const currentFilter = ref<FilterType>('all')
const editingId = ref<number | null>(null)
const editingText = ref('')
const draggableTodos = ref<Todo[]>([])

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

const filteredTodos = computed(() => {
  const sorted = sortedTodos.value
  const filtered = (() => {
    switch (currentFilter.value) {
      case 'active':
        return sorted.filter(todo => !todo.completed)
      case 'completed':
        return sorted.filter(todo => todo.completed)
      default:
        return sorted
    }
  })()

  // 更新拖拽数组
  draggableTodos.value = [...filtered]

  return filtered
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

// 生成唯一ID
const generateId = () => {
  return Date.now() + Math.random()
}

// 添加新任务
const addTodo = () => {
  const text = newTodoText.value.trim()
  if (!text) return

  const newTodo: Todo = {
    id: generateId(),
    text,
    completed: false,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    sortOrder: getNextSortOrder()
  }

  todos.value.unshift(newTodo)
  newTodoText.value = ''
  saveTodos()
}

// 获取下一个排序号
const getNextSortOrder = () => {
  const maxOrder = Math.max(...todos.value.map(t => t.sortOrder || 0), 0)
  return maxOrder + 1
}

// 切换任务完成状态
const toggleTodo = (id: number) => {
  const todo = todos.value.find(t => t.id === id)
  if (todo) {
    todo.completed = !todo.completed
    todo.updatedAt = new Date().toISOString()
    saveTodos()
  }
}

// 删除任务
const deleteTodo = (id: number) => {
  const index = todos.value.findIndex(t => t.id === id)
  if (index > -1) {
    todos.value.splice(index, 1)
    saveTodos()
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
const saveEdit = () => {
  const text = editingText.value.trim()
  if (!text) {
    cancelEdit()
    return
  }

  const todo = todos.value.find(t => t.id === editingId.value)
  if (todo) {
    todo.text = text
    todo.updatedAt = new Date().toISOString()
    saveTodos()
  }

  cancelEdit()
}

// 取消编辑
const cancelEdit = () => {
  editingId.value = null
  editingText.value = ''
}

// 全选/全不选
const toggleAll = () => {
  const shouldComplete = !allCompleted.value
  todos.value.forEach(todo => {
    todo.completed = shouldComplete
    todo.updatedAt = new Date().toISOString()
  })
  saveTodos()
}

// 清除已完成任务
const clearCompleted = () => {
  todos.value = todos.value.filter(todo => !todo.completed)
  saveTodos()
}



// 拖拽排序结束处理
const onDragEnd = () => {
  const currentTime = new Date().toISOString()

  // 更新排序号
  draggableTodos.value.forEach((todo, index) => {
    const originalTodo = todos.value.find(t => t.id === todo.id)
    if (originalTodo) {
      originalTodo.sortOrder = index + 1
      originalTodo.updatedAt = currentTime
    }
  })

  saveTodos()
}



// 保存到localStorage
const saveTodos = () => {
  localStorage.setItem('todos', JSON.stringify(todos.value))
}

// 从localStorage加载
const loadTodos = () => {
  try {
    const saved = localStorage.getItem('todos')
    if (saved) {
      todos.value = JSON.parse(saved)
    }
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
onMounted(() => {
  loadTodos()
})
</script>

<template>
  <NuxtLayout>
    <div class="todo-dashboard">
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

      <!-- 主要内容区域 -->
      <div class="todo-content">
        <!-- 添加任务卡片 -->
        <Motion
          :initial="{ opacity: 0, y: 20 }"
          :animate="{ opacity: 1, y: 0 }"
          :transition="{ duration: 0.5, delay: 0.1 }"
        >
          <div class="todo-card add-todo-card">
            <div class="card-header">
              <div class="card-icon add-icon">
                <Icon icon="material-symbols:add" />
              </div>
              <div class="card-title-section">
                <h3 class="card-title">添加新任务</h3>
                <p class="card-subtitle">输入任务内容并按回车键添加</p>
              </div>
            </div>
            <div class="card-content">
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
          </div>
        </Motion>

        <!-- 筛选和批量操作卡片 -->
        <Motion
          :initial="{ opacity: 0, y: 20 }"
          :animate="{ opacity: 1, y: 0 }"
          :transition="{ duration: 0.5, delay: 0.2 }"
        >
          <div class="todo-card filter-card">
            <div class="card-content">
              <div class="filter-controls">
                <!-- 筛选按钮 -->
                <div class="filter-buttons">
                  <button
                    @click="currentFilter = 'all'"
                    :class="['filter-btn', { active: currentFilter === 'all' }]"
                  >
                    <Icon icon="material-symbols:list" />
                    全部
                  </button>
                  <button
                    @click="currentFilter = 'active'"
                    :class="['filter-btn', { active: currentFilter === 'active' }]"
                  >
                    <Icon icon="material-symbols:radio-button-unchecked" />
                    待完成
                  </button>
                  <button
                    @click="currentFilter = 'completed'"
                    :class="['filter-btn', { active: currentFilter === 'completed' }]"
                  >
                    <Icon icon="material-symbols:check-circle" />
                    已完成
                  </button>
                </div>

                <!-- 批量操作按钮 -->
                <div class="bulk-actions" v-if="todos.length > 0">
                    <button
                      @click="toggleAll"
                      class="bulk-btn"
                      :title="allCompleted ? '全部标记为未完成' : '全部标记为完成'"
                    >
                      <Icon :icon="allCompleted ? 'material-symbols:check-box' : 'material-symbols:check-box-outline-blank'" />
                      {{ allCompleted ? '全不选' : '全选' }}
                    </button>
                    <button
                      @click="clearCompleted"
                      :disabled="todoStats.completed === 0"
                      class="bulk-btn clear-btn"
                      title="清除已完成任务"
                    >
                      <Icon icon="material-symbols:delete-sweep" />
                      清除已完成
                    </button>
                  </div>
                </div>
              </div>
            </div>
        </Motion>

        <!-- 拖拽提示 -->
        <div v-if="currentFilter !== 'all' && todos.length > 0" class="drag-hint-card">
          <Icon icon="material-symbols:info" class="hint-icon" />
          <span class="hint-text">在"全部"筛选下可拖拽调整任务顺序</span>
        </div>

        <!-- 任务列表 -->
        <div class="todo-list">
          <!-- 空状态 -->
          <Motion
            v-if="filteredTodos.length === 0"
            :initial="{ opacity: 0, y: 20 }"
            :animate="{ opacity: 1, y: 0 }"
            :transition="{ duration: 0.5, delay: 0.3 }"
          >
            <div class="empty-state">
              <div class="empty-icon">
                <Icon
                  :icon="currentFilter === 'all' ? 'material-symbols:task-alt' :
                         currentFilter === 'active' ? 'material-symbols:radio-button-unchecked' :
                         'material-symbols:check-circle'"
                />
              </div>
              <h3 class="empty-title">
                {{ currentFilter === 'all' ? '暂无任务' :
                   currentFilter === 'active' ? '没有待完成的任务' :
                   '没有已完成的任务' }}
              </h3>
              <p class="empty-subtitle">
                {{ currentFilter === 'all' ? '添加一个新任务开始吧！' :
                   currentFilter === 'active' ? '所有任务都已完成！' :
                   '还没有完成任何任务' }}
              </p>
            </div>
          </Motion>

          <!-- 任务项 - 支持拖拽排序 -->
          <VueDraggable
            v-model="draggableTodos"
            @end="onDragEnd"
            :disabled="currentFilter !== 'all'"
            handle=".drag-handle"
            ghost-class="ghost-item"
            chosen-class="chosen-item"
            drag-class="drag-item"
            :animation="200"
          >
            <Motion
              v-for="(todo, index) in draggableTodos"
              :key="todo.id"
              :initial="{ opacity: 0, y: 20 }"
              :animate="{ opacity: 1, y: 0 }"
              :transition="{ duration: 0.5, delay: 0.3 + index * 0.05 }"
            >
              <div :class="['todo-item', { completed: todo.completed, draggable: currentFilter === 'all' }]">
                <!-- 拖拽手柄 -->
                <div
                  v-if="currentFilter === 'all'"
                  class="drag-handle"
                  title="拖拽排序"
                >
                  <Icon icon="material-symbols:drag-indicator" class="drag-icon" />
                </div>

                <!-- 完成状态切换 -->
                <button
                  @click="toggleTodo(todo.id)"
                  class="todo-checkbox"
                  :title="todo.completed ? '标记为未完成' : '标记为完成'"
                >
                  <Icon
                    :icon="todo.completed ? 'material-symbols:check-circle' : 'material-symbols:radio-button-unchecked'"
                    :class="['checkbox-icon', { completed: todo.completed }]"
                  />
                </button>

                <!-- 任务内容 -->
                <div class="todo-content-area" @dblclick="startEdit(todo)">
                  <input
                    v-if="editingId === todo.id"
                    v-model="editingText"
                    @keyup="handleKeyup"
                    @blur="saveEdit"
                    class="todo-edit-input"
                    ref="editInput"
                    maxlength="200"
                  />
                  <span v-else :class="['todo-text', { completed: todo.completed }]">
                    {{ todo.text }}
                  </span>
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
            </Motion>
          </VueDraggable>
        </div>
      </div>
    </div>
  </NuxtLayout>
</template>

<style scoped>
/* Todo应用样式 */
.todo-dashboard {
  padding: 2rem;
  max-width: 1200px;
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
}

.dashboard-title {
  font-size: 2rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

/* 统计摘要 */
.stats-summary {
  display: flex;
  gap: 1.5rem;
  align-items: center;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-radius: 0.75rem;

  /* 液态玻璃效果 */
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 50%,
    rgba(255, 255, 255, 0.05) 100%
  );
  backdrop-filter: blur(15px) saturate(150%);
  -webkit-backdrop-filter: blur(15px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.2);

  box-shadow:
    0 6px 20px rgba(0, 0, 0, 0.15),
    0 2px 6px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
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
  font-size: 1.125rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.9);
  min-width: 1.5rem;
  text-align: center;
}

.stat-label {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
}

/* 主要内容区域 */
.todo-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Todo卡片基础样式 */
.todo-card {
  border-radius: 1rem;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

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
    0 20px 40px rgba(0, 0, 0, 0.15),
    0 8px 16px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.3),
    inset 0 -1px 0 rgba(0, 0, 0, 0.1);
}

.todo-card:hover {
  transform: translateY(-2px);
  box-shadow:
    0 25px 50px rgba(0, 0, 0, 0.2),
    0 12px 20px rgba(0, 0, 0, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.4),
    inset 0 -1px 0 rgba(0, 0, 0, 0.1);
}

/* 卡片头部 */
.card-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem 1.5rem 1rem 1.5rem;
}

.card-icon {
  width: 3rem;
  height: 3rem;
  border-radius: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.card-icon svg {
  width: 1.5rem;
  height: 1.5rem;
  color: rgba(255, 255, 255, 0.9);
}

/* 添加图标样式 */
.add-icon {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.3) 0%,
    rgba(59, 130, 246, 0.2) 100%
  );
  border: 1px solid rgba(59, 130, 246, 0.4);
}

.card-title-section {
  flex: 1;
  min-width: 0;
}

.card-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 0.25rem 0;
}

.card-subtitle {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

/* 卡片内容 */
.card-content {
  padding: 0 1.5rem 1.5rem 1.5rem;
}

/* 添加任务表单 */
.add-todo-form {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.todo-input {
  flex: 1;
  padding: 0.875rem 1rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.875rem;
  transition: all 0.3s ease;
}

.todo-input::placeholder {
  color: rgba(255, 255, 255, 0.5);
}

.todo-input:focus {
  outline: none;
  border-color: rgba(59, 130, 246, 0.5);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.add-btn {
  width: 3rem;
  height: 3rem;
  border-radius: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.8) 0%,
    rgba(59, 130, 246, 0.6) 100%
  );

  box-shadow:
    0 6px 20px rgba(59, 130, 246, 0.3),
    0 2px 6px rgba(59, 130, 246, 0.2);
}

.add-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow:
    0 8px 25px rgba(59, 130, 246, 0.4),
    0 4px 10px rgba(59, 130, 246, 0.3);
}

.add-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.add-btn-icon {
  width: 1.5rem;
  height: 1.5rem;
  color: white;
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
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
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
    rgba(59, 130, 246, 0.3) 0%,
    rgba(59, 130, 246, 0.2) 100%
  );
  border-color: rgba(59, 130, 246, 0.4);
  color: rgba(255, 255, 255, 0.9);
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
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
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
    rgba(239, 68, 68, 0.3) 0%,
    rgba(239, 68, 68, 0.2) 100%
  );
  border-color: rgba(239, 68, 68, 0.4);
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
  border-radius: 0.75rem;
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.15) 0%,
    rgba(59, 130, 246, 0.08) 100%
  );
  border: 1px solid rgba(59, 130, 246, 0.3);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
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

/* 任务列表 */
.todo-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  text-align: center;

  border-radius: 1rem;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.empty-icon {
  width: 4rem;
  height: 4rem;
  margin-bottom: 1.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 1rem;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 100%
  );
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.empty-icon svg {
  width: 2rem;
  height: 2rem;
  color: rgba(255, 255, 255, 0.6);
}

.empty-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
  margin: 0 0 0.5rem 0;
}

.empty-subtitle {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

/* 任务项 */
.todo-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.5rem;
  border-radius: 0.75rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.12) 0%,
    rgba(255, 255, 255, 0.06) 100%
  );
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.15);

  box-shadow:
    0 8px 16px rgba(0, 0, 0, 0.1),
    0 2px 4px rgba(0, 0, 0, 0.05);
}

.todo-item:hover {
  transform: translateY(-1px);
  box-shadow:
    0 12px 24px rgba(0, 0, 0, 0.15),
    0 4px 8px rgba(0, 0, 0, 0.1);
  border-color: rgba(255, 255, 255, 0.25);
}

.todo-item.completed {
  opacity: 0.7;
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
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  cursor: grab;
  border-radius: 0.5rem;
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
  width: 1.25rem;
  height: 1.25rem;
  color: rgba(255, 255, 255, 0.5);
  transition: all 0.3s ease;
}

.drag-handle:hover .drag-icon {
  color: rgba(255, 255, 255, 0.8);
}

/* 拖拽状态样式 */
.ghost-item {
  opacity: 0.5;
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.2) 0%,
    rgba(59, 130, 246, 0.1) 100%
  );
  border: 2px dashed rgba(59, 130, 246, 0.5);
  transform: rotate(2deg);
}

.chosen-item {
  transform: scale(1.02);
  box-shadow:
    0 15px 30px rgba(0, 0, 0, 0.2),
    0 6px 12px rgba(0, 0, 0, 0.15);
}

.drag-item {
  transform: rotate(5deg);
  opacity: 0.8;
}

/* 复选框 */
.todo-checkbox {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 0.5rem;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.todo-checkbox:hover {
  background: rgba(255, 255, 255, 0.1);
}

.checkbox-icon {
  width: 1.5rem;
  height: 1.5rem;
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
}

.todo-text {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.5;
  word-break: break-word;
  transition: all 0.3s ease;
}

.todo-text.completed {
  text-decoration: line-through;
  color: rgba(255, 255, 255, 0.5);
}

.todo-edit-input {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(59, 130, 246, 0.5);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 100%
  );
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.875rem;
  line-height: 1.5;
}

.todo-edit-input:focus {
  outline: none;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
}

/* 任务操作按钮 */
.todo-actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-shrink: 0;
}

.action-btn {
  width: 2rem;
  height: 2rem;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-btn:hover {
  border-color: rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
}

.action-btn svg {
  width: 1rem;
  height: 1rem;
  color: rgba(255, 255, 255, 0.7);
}

.edit-btn:hover {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.3) 0%,
    rgba(59, 130, 246, 0.2) 100%
  );
  border-color: rgba(59, 130, 246, 0.4);
}

.edit-btn:hover svg {
  color: rgba(255, 255, 255, 0.9);
}

.delete-btn:hover {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.3) 0%,
    rgba(239, 68, 68, 0.2) 100%
  );
  border-color: rgba(239, 68, 68, 0.4);
}

.delete-btn:hover svg {
  color: rgba(255, 255, 255, 0.9);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .todo-dashboard {
    padding: 1.5rem;
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
    font-size: 1.75rem;
  }

  .stats-summary {
    width: 100%;
    justify-content: space-between;
  }

  .filter-controls {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }

  .filter-buttons {
    justify-content: center;
  }

  .bulk-actions {
    justify-content: center;
  }

  .card-header {
    padding: 1.25rem 1.25rem 0.75rem 1.25rem;
  }

  .card-content {
    padding: 0 1.25rem 1.25rem 1.25rem;
  }

  .todo-item {
    padding: 0.875rem 1rem;
    gap: 0.75rem;
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
    font-size: 1.5rem;
  }

  .stats-summary {
    flex-direction: column;
    gap: 0.75rem;
  }

  .stat-item {
    width: 100%;
    justify-content: center;
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

  .filter-buttons {
    flex-direction: column;
    gap: 0.5rem;
  }

  .filter-btn {
    justify-content: center;
  }

  .bulk-actions {
    flex-direction: column;
    gap: 0.5rem;
  }

  .bulk-btn {
    justify-content: center;
  }

  .drag-handle {
    width: 1.5rem;
    height: 1.5rem;
  }

  .drag-icon {
    width: 1rem;
    height: 1rem;
  }

  .card-header {
    padding: 1rem;
    gap: 0.75rem;
  }

  .card-content {
    padding: 0 1rem 1rem 1rem;
  }

  .card-icon {
    width: 2.5rem;
    height: 2.5rem;
  }

  .card-icon svg {
    width: 1.25rem;
    height: 1.25rem;
  }

  .card-title {
    font-size: 1rem;
  }

  .card-subtitle {
    font-size: 0.8rem;
  }

  .todo-item {
    padding: 0.75rem;
    gap: 0.5rem;
  }

  .todo-checkbox {
    width: 1.75rem;
    height: 1.75rem;
  }

  .checkbox-icon {
    width: 1.25rem;
    height: 1.25rem;
  }

  .action-btn {
    width: 1.75rem;
    height: 1.75rem;
  }

  .action-btn svg {
    width: 0.875rem;
    height: 0.875rem;
  }

  .empty-state {
    padding: 3rem 1rem;
  }

  .empty-icon {
    width: 3rem;
    height: 3rem;
    margin-bottom: 1rem;
  }

  .empty-icon svg {
    width: 1.5rem;
    height: 1.5rem;
  }

  .empty-title {
    font-size: 1.125rem;
  }

  .empty-subtitle {
    font-size: 0.8rem;
  }
}
</style>