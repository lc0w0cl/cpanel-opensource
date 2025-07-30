<script setup lang="ts">
import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue'
import {Motion} from "motion-v"
import {Icon} from '@iconify/vue'
import {VueDraggable} from 'vue-draggable-plus'
// Ant Design Vue 组件通过插件全局注册
import dayjs, {type Dayjs} from 'dayjs'
import zhCN from 'ant-design-vue/es/locale/zh_CN'

// 子页面不需要定义 layout 和 middleware，由父页面处理
// 导入API函数和类型
import {type Todo, useTodoApi} from '~/composables/useTodoApi'
import {apiRequest} from '~/composables/useJwt'

// 筛选类型
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
const editingText = ref('')
const showEditModal = ref(false)
const editModalTodo = ref<Todo | null>(null)
const editingDateRange = ref<[Dayjs, Dayjs] | null>(null)

// 分组相关数据
const todoCategories = ref<TodoCategory[]>([])
const selectedCategoryId = ref<number | null>(null)
const categoriesLoading = ref(false)
const showCategoryDropdown = ref(false)

// 日期相关数据
const selectedDateRange = ref<[Dayjs, Dayjs] | null>(null)
const showDateFilter = ref(false)
const newTodoDateRange = ref<[Dayjs, Dayjs] | null>(null)

// Ant Design Vue 中文配置
const locale = zhCN

// 日历相关数据
const currentCalendarDate = ref(dayjs())
const showCalendar = ref(true)

// 计算属性
const sortedTodos = computed(() => {
  // 根据选择的分组过滤任务
  let filteredByCategory = todos.value
  if (selectedCategoryId.value !== null) {
    filteredByCategory = todos.value.filter(todo => todo.categoryId === selectedCategoryId.value)
  }

  // 根据日期范围过滤任务
  let filteredByDate = filteredByCategory
  if (selectedDateRange.value) {
    const [filterStart, filterEnd] = selectedDateRange.value
    filteredByDate = filteredByCategory.filter(todo => {
      // 如果任务没有设置日期，则显示
      if (!todo.startDate && !todo.endDate) return true

      // 检查日期范围重叠
      const todoStart = todo.startDate ? dayjs(todo.startDate) : dayjs('1900-01-01')
      const todoEnd = todo.endDate ? dayjs(todo.endDate) : dayjs('2100-12-31')

      return todoStart.isSameOrBefore(filterEnd) && todoEnd.isSameOrAfter(filterStart)
    })
  }

  // 默认按sortOrder排序，如果没有sortOrder则按创建时间倒序
  return [...filteredByDate].sort((a, b) => {
    if (a.sortOrder !== undefined && b.sortOrder !== undefined) {
      return a.sortOrder - b.sortOrder
    }
    if (a.sortOrder !== undefined) return -1
    if (b.sortOrder !== undefined) return 1
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  })
})

// 分别获取待办和已完成的任务
const activeTodos = ref<Todo[]>([])
const completedTodos = ref<Todo[]>([])

// 同步活动任务和已完成任务
const syncTodoLists = () => {
  const sorted = sortedTodos.value
  activeTodos.value = sorted.filter(todo => !todo.completed)
  completedTodos.value = sorted.filter(todo => todo.completed)
}

// 监听sortedTodos变化，同步到activeTodos和completedTodos
watch(sortedTodos, syncTodoLists, { immediate: true })


const todoStats = computed(() => {
  // 根据选择的分组过滤任务
  let filteredTodos = todos.value
  if (selectedCategoryId.value !== null) {
    filteredTodos = todos.value.filter(todo => todo.categoryId === selectedCategoryId.value)
  }

  const total = filteredTodos.length
  const completed = filteredTodos.filter(todo => todo.completed).length
  const active = total - completed
  return { total, completed, active }
})

const allCompleted = computed(() => {
  // 根据选择的分组过滤任务
  let filteredTodos = todos.value
  if (selectedCategoryId.value !== null) {
    filteredTodos = todos.value.filter(todo => todo.categoryId === selectedCategoryId.value)
  }

  return filteredTodos.length > 0 && filteredTodos.every(todo => todo.completed)
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
    // 创建任务时传入选择的分组ID和日期
    const requestData: any = { text }
    if (selectedCategoryId.value !== null) {
      requestData.categoryId = selectedCategoryId.value
    }
    if (newTodoDateRange.value) {
      const [startDate, endDate] = newTodoDateRange.value
      requestData.startDate = startDate.format('YYYY-MM-DD')
      requestData.endDate = endDate.format('YYYY-MM-DD')
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
      newTodoDateRange.value = null
    }
  } catch (error) {
    console.error('添加任务失败:', error)
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

  // 初始化编辑日期
  if (todo.startDate && todo.endDate) {
    editingDateRange.value = [dayjs(todo.startDate), dayjs(todo.endDate)]
  } else if (todo.startDate) {
    editingDateRange.value = [dayjs(todo.startDate), dayjs(todo.startDate)]
  } else if (todo.endDate) {
    editingDateRange.value = [dayjs(todo.endDate), dayjs(todo.endDate)]
  } else {
    editingDateRange.value = null
  }

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
      // 更新文本
      const textSuccess = await todoApi.updateTodoText(editModalTodo.value.id, text)

      // 更新日期
      let dateSuccess = true
      if (editingDateRange.value) {
        const [startDate, endDate] = editingDateRange.value
        dateSuccess = await todoApi.updateTodoDates(
          editModalTodo.value.id,
          startDate.format('YYYY-MM-DD'),
          endDate.format('YYYY-MM-DD')
        )
      } else {
        // 清除日期
        dateSuccess = await todoApi.updateTodoDates(editModalTodo.value.id, undefined, undefined)
      }

      if (textSuccess && dateSuccess) {
        const todo = todos.value.find(t => t.id === editModalTodo.value!.id)
        if (todo) {
          todo.text = text
          if (editingDateRange.value) {
            const [startDate, endDate] = editingDateRange.value
            todo.startDate = startDate.format('YYYY-MM-DD')
            todo.endDate = endDate.format('YYYY-MM-DD')
          } else {
            todo.startDate = undefined
            todo.endDate = undefined
          }
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
  editingDateRange.value = null
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

  // 根据选择的分组过滤任务
  let filteredTodos = todos.value
  if (selectedCategoryId.value !== null) {
    filteredTodos = todos.value.filter(todo => todo.categoryId === selectedCategoryId.value)
  }

  try {
    // 如果有选择分组，只对该分组的任务进行操作
    if (selectedCategoryId.value !== null) {
      // 批量更新当前分组的任务
      const todoIds = filteredTodos.map(todo => todo.id)
      const success = await todoApi.setTodosCompleted(todoIds, shouldComplete)
      if (success) {
        filteredTodos.forEach(todo => {
          todo.completed = shouldComplete
          todo.updatedAt = new Date().toISOString()
        })
      }
    } else {
      // 如果是全部任务，使用原来的方法
      const success = await todoApi.setAllTodosCompleted(shouldComplete)
      if (success) {
        todos.value.forEach(todo => {
          todo.completed = shouldComplete
          todo.updatedAt = new Date().toISOString()
        })
      }
    }
  } catch (error) {
    console.error('批量设置任务状态失败:', error)
  }
}

// 清除已完成任务
const clearCompleted = async () => {
  try {
    // 根据选择的分组过滤任务
    let completedTodos = todos.value.filter(todo => todo.completed)
    if (selectedCategoryId.value !== null) {
      completedTodos = completedTodos.filter(todo => todo.categoryId === selectedCategoryId.value)
    }

    if (selectedCategoryId.value !== null) {
      // 如果有选择分组，只删除该分组的已完成任务
      const deletePromises = completedTodos.map(todo => todoApi.deleteTodo(todo.id))
      const results = await Promise.all(deletePromises)
      const successCount = results.filter(result => result).length

      if (successCount > 0) {
        // 从本地状态中移除已删除的任务
        const deletedIds = completedTodos.slice(0, successCount).map(todo => todo.id)
        todos.value = todos.value.filter(todo => !deletedIds.includes(todo.id))
      }
    } else {
      // 如果是全部任务，使用原来的方法
      const deletedCount = await todoApi.deleteCompletedTodos()
      if (deletedCount > 0) {
        todos.value = todos.value.filter(todo => !todo.completed)
      }
    }
  } catch (error) {
    console.error('删除已完成任务失败:', error)
  }
}



// 拖拽处理函数
const onActiveDragEnd = async (event: any) => {
  const {oldIndex, newIndex, to, from, item} = event

  console.log('拖拽事件详情:', {
    oldIndex,
    newIndex,
    to: to?.className,
    from: from?.className,
    item: item?.textContent?.trim()
  })

  // 如果拖拽到已完成列表
  if (to.classList.contains('completed-list')) {
    // 使用更可靠的方式获取被拖拽的任务
    // 从 DOM 元素中获取任务 ID
    const taskId = item?.getAttribute('data-task-id')
    if (taskId) {
      const draggedTodo = todos.value.find(t => t.id === parseInt(taskId))
      if (draggedTodo && !draggedTodo.completed) {
        console.log('拖拽任务到已完成列表:', draggedTodo)
        try {
          const success = await todoApi.toggleTodoCompleted(draggedTodo.id)
          if (success) {
            // 更新本地状态
            draggedTodo.completed = true
            draggedTodo.updatedAt = new Date().toISOString()
            console.log('任务状态更新成功:', draggedTodo)
          } else {
            console.error('任务状态更新失败')
            // 重新加载数据以恢复正确状态
            await loadTodos()
          }
        } catch (error) {
          console.error('切换任务状态失败:', error)
          // 重新加载数据以恢复正确状态
          await loadTodos()
        }
      }
    }
  } else {
    // 在同一列表内排序
    const currentTime = new Date().toISOString()

    // 注意：此时activeTodos.value已经是拖拽后的新顺序
    const todoIds = activeTodos.value.map(todo => todo.id)

    console.log('开始更新排序，拖拽后的activeTodos:', activeTodos.value.map(t => ({ id: t.id, text: t.text, sortOrder: t.sortOrder })))
    console.log('发送的todoIds（新顺序）:', todoIds)

    try {
      const success = await todoApi.updateTodosSortOrder(todoIds)
      console.log('排序更新结果:', success)

      if (success) {
        // 更新原始todos数组中对应任务的排序号
        activeTodos.value.forEach((todo, index) => {
          const originalTodo = todos.value.find(t => t.id === todo.id)
          if (originalTodo) {
            const newSortOrder = index + 1
            console.log(`更新任务 ${todo.id} 的排序从 ${originalTodo.sortOrder} 到 ${newSortOrder}`)
            originalTodo.sortOrder = newSortOrder
            originalTodo.updatedAt = currentTime

            // 同时更新activeTodos中的sortOrder
            todo.sortOrder = newSortOrder
            todo.updatedAt = currentTime
          }
        })

        console.log('排序更新完成，当前todos状态:', todos.value.filter(t => !t.completed).map(t => ({ id: t.id, text: t.text, sortOrder: t.sortOrder })))

        // 重新加载数据以验证更新
        setTimeout(async () => {
          console.log('3秒后重新加载数据以验证排序更新...')
          await loadTodos()
        }, 3000)
      } else {
        console.error('排序更新失败，重新加载原始数据')
        await loadTodos()
      }
    } catch (error) {
      console.error('更新任务排序失败:', error)
      await loadTodos()
    }
  }
}

const onCompletedDragEnd = async (event: any) => {
  const {oldIndex, newIndex, to, from, item} = event

  console.log('已完成任务拖拽事件详情:', {
    oldIndex,
    newIndex,
    to: to?.className,
    from: from?.className,
    item: item?.textContent?.trim()
  })

  // 如果拖拽到待办列表
  if (to.classList.contains('active-list')) {
    // 使用更可靠的方式获取被拖拽的任务
    // 从 DOM 元素中获取任务 ID
    const taskId = item?.getAttribute('data-task-id')
    if (taskId) {
      const draggedTodo = todos.value.find(t => t.id === parseInt(taskId))
      if (draggedTodo && draggedTodo.completed) {
        console.log('拖拽已完成任务到待办列表:', draggedTodo)
        try {
          const success = await todoApi.toggleTodoCompleted(draggedTodo.id)
          if (success) {
            // 更新本地状态
            draggedTodo.completed = false
            draggedTodo.updatedAt = new Date().toISOString()
            console.log('任务状态更新成功:', draggedTodo)
          } else {
            console.error('任务状态更新失败')
            // 重新加载数据以恢复正确状态
            await loadTodos()
          }
        } catch (error) {
          console.error('切换任务状态失败:', error)
          // 重新加载数据以恢复正确状态
          await loadTodos()
        }
      }
    }
  }
  // 已完成列表内部不需要排序
}



// 从API加载任务
const loadTodos = async () => {
  try {
    todos.value = await todoApi.getAllTodos()
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

// 日期过滤相关函数
const clearDateFilter = () => {
  selectedDateRange.value = null
}

const applyDateFilter = () => {
  // 日期过滤会通过计算属性自动应用
  showDateFilter.value = false
}

// 格式化日期显示
const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  return dayjs(dateStr).format('YYYY-MM-DD')
}

// 获取日期范围显示文本
const dateRangeText = computed(() => {
  if (!selectedDateRange.value) {
    return '全部日期'
  }
  const [startDate, endDate] = selectedDateRange.value
  return `${startDate.format('YYYY-MM-DD')} 至 ${endDate.format('YYYY-MM-DD')}`
})

// 日历相关计算属性和方法
const calendarTodos = computed(() => {
  const todosByDate = new Map<string, Todo[]>()

  todos.value.forEach(todo => {
    if (todo.startDate || todo.endDate) {
      const startDate = todo.startDate || todo.endDate
      const endDate = todo.endDate || todo.startDate

      if (startDate && endDate) {
        try {
          const start = dayjs(startDate)
          const end = dayjs(endDate)

          // 验证日期是否有效
          if (!start.isValid() || !end.isValid()) {
            return
          }

          // 为日期范围内的每一天添加任务
          let current = start
          while (current.isSameOrBefore(end, 'day')) {
            const dateKey = current.format('YYYY-MM-DD')
            if (!todosByDate.has(dateKey)) {
              todosByDate.set(dateKey, [])
            }
            todosByDate.get(dateKey)!.push(todo)
            current = current.add(1, 'day')
          }
        } catch (error) {
          // 忽略无效的日期
        }
      }
    }
  })

  return todosByDate
})

// 获取指定日期的任务
const getTodosForDate = (date: any): Todo[] => {
  try {
    // 确保 date 是 dayjs 对象
    const dayjsDate = dayjs.isDayjs(date) ? date : dayjs(date)

    if (!dayjsDate.isValid()) {
      return []
    }

    const dateKey = dayjsDate.format('YYYY-MM-DD')
    const todosForDate = calendarTodos.value.get(dateKey) || []

    return todosForDate
  } catch (error) {
    return []
  }
}

// 获取任务的徽章状态
const getBadgeStatus = (todo: Todo): 'success' | 'processing' | 'default' | 'error' | 'warning' => {
  if (todo.completed) {
    return 'success'
  }

  const today = dayjs()

  // 如果有结束日期，根据日期判断紧急程度
  if (todo.endDate) {
    const endDate = dayjs(todo.endDate)
    const daysUntilDue = endDate.diff(today, 'day')

    if (daysUntilDue < 0) {
      return 'error' // 已过期
    } else if (daysUntilDue === 0) {
      return 'warning' // 今天到期
    } else if (daysUntilDue <= 2) {
      return 'warning' // 即将到期（2天内）
    } else {
      return 'processing' // 正常进行中
    }
  }

  // 如果有开始日期但没有结束日期
  if (todo.startDate) {
    const startDate = dayjs(todo.startDate)
    if (startDate.isSameOrBefore(today, 'day')) {
      return 'processing' // 已开始
    } else {
      return 'default' // 未开始
    }
  }

  // 没有日期的任务
  return 'default'
}

// 获取任务显示文本
const getTodoDisplayText = (todo: Todo): string => {
  const maxLength = 10
  if (todo.text.length <= maxLength) {
    return todo.text
  }

  // 智能截断：优先在空格处截断
  const truncated = todo.text.slice(0, maxLength)
  const lastSpaceIndex = truncated.lastIndexOf(' ')

  if (lastSpaceIndex > maxLength * 0.6) {
    return truncated.slice(0, lastSpaceIndex) + '...'
  }

  return truncated + '...'
}

// 日历日期点击处理
const onCalendarDateClick = (date: any) => {
  const dayjsDate = dayjs.isDayjs(date) ? date : dayjs(date)
  const todosForDate = getTodosForDate(dayjsDate)
  if (todosForDate.length > 0) {
    // 可以在这里添加显示当天任务的逻辑
    console.log(`${dayjsDate.format('YYYY-MM-DD')} 的任务:`, todosForDate)
  }
}

// 切换日历显示
const toggleCalendar = () => {
  showCalendar.value = !showCalendar.value
}

// 点击外部关闭下拉框
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  const categoryDropdown = document.querySelector('.category-dropdown')
  const dateFilter = document.querySelector('.date-filter')

  if (categoryDropdown && !categoryDropdown.contains(target)) {
    showCategoryDropdown.value = false
  }

  if (dateFilter && !dateFilter.contains(target)) {
    showDateFilter.value = false
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

        <!-- 分组选择下拉框和日期过滤 -->
        <div class="header-right">
          <!-- 日期过滤器 -->
          <div class="date-filter">
            <div class="date-filter-trigger" @click="showDateFilter = !showDateFilter">
              <Icon icon="mdi:calendar-range" class="date-icon" />
              <span class="date-text">{{ dateRangeText }}</span>
              <Icon
                icon="mdi:chevron-down"
                class="dropdown-arrow"
                :class="{ 'rotated': showDateFilter }"
              />
            </div>

            <Transition name="dropdown">
              <div v-if="showDateFilter" class="date-filter-dropdown" @click.stop>
                <div class="date-filter-content">
                  <div class="date-input-group">
                    <label class="date-label">选择日期范围</label>
                    <a-config-provider :locale="locale">
                      <a-range-picker
                        v-model:value="selectedDateRange"
                        class="ant-date-picker"
                        :placeholder="['开始日期', '结束日期']"
                        format="YYYY-MM-DD"
                        :allowClear="true"
                      />
                    </a-config-provider>
                  </div>
                  <div class="date-filter-actions">
                    <button @click="clearDateFilter" class="date-btn clear-btn">
                      清除
                    </button>
                    <button @click="applyDateFilter" class="date-btn apply-btn">
                      应用
                    </button>
                  </div>
                </div>
              </div>
            </Transition>
          </div>

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
          <a-config-provider :locale="locale">
            <a-range-picker
              v-model:value="newTodoDateRange"
              class="ant-date-picker-inline"
              :placeholder="['开始日期', '结束日期']"
              format="YYYY-MM-DD"
              :allowClear="true"
              size="small"
            />
          </a-config-provider>
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

      <!-- 主要内容区域 - 三列布局 -->
      <div class="todo-content">
        <!-- 三列容器 -->
        <div class="three-column-container">

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
                    :data-task-id="todo.id"
                    class="todo-item active-item draggable-item"
                  >
                    <!-- 任务内容 -->
                    <div class="todo-content-area" @dblclick="startEdit(todo)">
                      <span class="todo-text">{{ todo.text }}</span>
                      <div v-if="todo.startDate || todo.endDate" class="todo-dates">
                        <Icon icon="mdi:calendar" class="date-icon" />
                        <span class="date-range">
                          <span v-if="todo.startDate">{{ formatDate(todo.startDate) }}</span>
                          <span v-if="todo.startDate && todo.endDate"> - </span>
                          <span v-if="todo.endDate">{{ formatDate(todo.endDate) }}</span>
                        </span>
                      </div>
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
                    :data-task-id="todo.id"
                    class="todo-item completed-item draggable-item"
                  >
                    <!-- 任务内容 -->
                    <div class="todo-content-area" @dblclick="startEdit(todo)">
                      <span class="todo-text completed">{{ todo.text }}</span>
                      <div v-if="todo.startDate || todo.endDate" class="todo-dates">
                        <Icon icon="mdi:calendar" class="date-icon" />
                        <span class="date-range">
                          <span v-if="todo.startDate">{{ formatDate(todo.startDate) }}</span>
                          <span v-if="todo.startDate && todo.endDate"> - </span>
                          <span v-if="todo.endDate">{{ formatDate(todo.endDate) }}</span>
                        </span>
                      </div>
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

          <!-- 第三列：通知事项日历 -->
          <Motion
            :initial="{ opacity: 0, x: 20 }"
            :animate="{ opacity: 1, x: 0 }"
            :transition="{ duration: 0.6, delay: 0.4 }"
            class="calendar-column"
          >
            <div class="calendar-section">
              <div class="calendar-header">
                <div class="calendar-title-group">
                  <Icon icon="mdi:calendar-month" class="calendar-icon" />
                  <h3 class="calendar-title">通知事项日历</h3>
                </div>
                <button
                  @click="toggleCalendar"
                  class="calendar-toggle-btn"
                  :title="showCalendar ? '隐藏日历' : '显示日历'"
                >
                  <Icon
                    :icon="showCalendar ? 'mdi:calendar-minus' : 'mdi:calendar-plus'"
                    class="toggle-icon"
                  />
                </button>
              </div>

              <Transition name="calendar-slide">
                <div v-if="showCalendar" class="calendar-content">
                  <a-config-provider :locale="locale">
                    <a-calendar
                      v-model:value="currentCalendarDate"
                      class="todo-calendar"
                      :fullscreen="false"
                    >
                      <template #dateCellRender="{ current }">
                        <ul class="calendar-events">
                          <li
                            v-for="todo in getTodosForDate(current).slice(0, 4)"
                            :key="todo.id"
                            @click="startEdit(todo)"
                          >
                            <a-badge
                              :status="getBadgeStatus(todo)"
                              :text="getTodoDisplayText(todo)"
                              :title="todo.text"
                            />
                          </li>
                          <li v-if="getTodosForDate(current).length > 4">
                            <a-badge
                              status="default"
                              :text="`+${getTodosForDate(current).length - 4} 更多`"
                              :title="`还有 ${getTodosForDate(current).length - 4} 个任务`"
                            />
                          </li>
                        </ul>
                      </template>
                    </a-calendar>
                  </a-config-provider>

                  <!-- 当前日期的任务详情 -->
                  <div class="calendar-details">
                    <h4 class="details-title">
                      <Icon icon="mdi:calendar-today" class="details-icon" />
                      {{ currentCalendarDate.format('YYYY年MM月DD日') }} 的任务
                    </h4>
                    <div class="details-content">
                      <div
                        v-if="getTodosForDate(currentCalendarDate).length === 0"
                        class="no-todos-today"
                      >
                        <Icon icon="mdi:calendar-check" class="no-todos-icon" />
                        <span>今天没有安排任务</span>
                      </div>
                      <div
                        v-else
                        class="today-todos"
                      >
                        <div
                          v-for="todo in getTodosForDate(currentCalendarDate)"
                          :key="todo.id"
                          class="today-todo-item"
                          :class="{ 'completed': todo.completed }"
                          @click="startEdit(todo)"
                        >
                          <div class="todo-status">
                            <Icon
                              :icon="todo.completed ? 'mdi:check-circle' : 'mdi:circle-outline'"
                              class="status-icon"
                            />
                          </div>
                          <div class="calendar-todo-content">
                            <span class="todo-title">{{ todo.text }}</span>
                            <div v-if="todo.startDate !== todo.endDate" class="todo-date-range">
                              {{ formatDate(todo.startDate) }} - {{ formatDate(todo.endDate) }}
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </Transition>
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
              <div class="edit-form-group">
                <label class="edit-form-label">任务内容</label>
                <textarea
                  v-model="editingText"
                  @keyup="handleModalKeyup"
                  class="edit-modal-textarea"
                  placeholder="输入任务内容..."
                  maxlength="500"
                  rows="4"
                  autofocus
                ></textarea>
              </div>

              <div class="edit-form-group">
                <label class="edit-form-label">任务日期</label>
                <a-config-provider :locale="locale">
                  <a-range-picker
                    v-model:value="editingDateRange"
                    class="edit-date-picker"
                    :placeholder="['开始日期', '结束日期']"
                    format="YYYY-MM-DD"
                    :allowClear="true"
                  />
                </a-config-provider>
              </div>

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
  width: 100%;
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

/* 日期过滤器样式 */
.date-filter {
  position: relative;
  margin-right: 1rem;
}

.date-filter-trigger {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: transparent;
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 180px;
}

.date-filter-trigger:hover {
  border-color: rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.08);
  transform: translateY(-1px);
}

.date-icon {
  width: 1.25rem;
  height: 1.25rem;
  color: rgba(59, 130, 246, 0.8);
  flex-shrink: 0;
}

.date-text {
  flex: 1;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.date-filter-dropdown {
  position: absolute;
  top: calc(100% + 0.5rem);
  right: 0;
  min-width: 280px;
  background: rgba(30, 30, 30, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.75rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  z-index: 1000;
  padding: 1rem;
}

.date-filter-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.date-input-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.date-label {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

/* Ant Design Vue DatePicker 样式覆盖 */
:deep(.ant-date-picker) {
  background: rgba(255, 255, 255, 0.05) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  border-radius: 0.5rem !important;
  color: rgba(255, 255, 255, 0.9) !important;
  width: 280px !important;
  max-width: 100% !important;
}

:deep(.ant-date-picker:hover) {
  border-color: rgba(255, 255, 255, 0.3) !important;
  background: rgba(255, 255, 255, 0.08) !important;
}

:deep(.ant-date-picker-focused) {
  border-color: rgba(59, 130, 246, 0.5) !important;
  background: rgba(255, 255, 255, 0.1) !important;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1) !important;
}

:deep(.ant-date-picker .ant-date-picker-input) {
  color: rgba(255, 255, 255, 0.9) !important;
}

:deep(.ant-date-picker .ant-date-picker-input > input) {
  color: rgba(255, 255, 255, 0.9) !important;
  background: transparent !important;
  font-size: 0.875rem !important;
}

:deep(.ant-date-picker .ant-date-picker-input > input::placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
}

:deep(.ant-date-picker .ant-date-picker-input > input::-webkit-input-placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
}

:deep(.ant-date-picker .ant-date-picker-input > input::-moz-placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
}

:deep(.ant-date-picker .ant-date-picker-separator) {
  color: rgba(255, 255, 255, 0.6) !important;
}

:deep(.ant-date-picker .ant-date-picker-clear) {
  color: rgba(255, 255, 255, 0.6) !important;
}

:deep(.ant-date-picker .ant-date-picker-clear:hover) {
  color: rgba(255, 255, 255, 0.9) !important;
}

:deep(.ant-date-picker .ant-date-picker-suffix) {
  color: rgba(255, 255, 255, 0.6) !important;
}

/* 修复选中后的文字颜色 */
:deep(.ant-date-picker .ant-date-picker-input input[value]) {
  color: rgba(255, 255, 255, 0.9) !important;
}

:deep(.ant-date-picker.ant-date-picker-focused .ant-date-picker-input > input) {
  color: rgba(255, 255, 255, 0.9) !important;
}

/* 全局 Ant Design Vue 日期选择器面板样式 */
:global(.ant-picker-dropdown) {
  background: rgba(30, 30, 30, 0.98) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  border-radius: 0.75rem !important;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5) !important;
  backdrop-filter: blur(10px) !important;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Helvetica, Arial, sans-serif !important;
}

:global(.ant-picker-panel) {
  background: transparent !important;
  border: none !important;
  color: rgba(255, 255, 255, 0.9) !important;
}

:global(.ant-picker-header) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.15) !important;
  background: rgba(255, 255, 255, 0.05) !important;
}

:global(.ant-picker-header button) {
  color: rgba(255, 255, 255, 0.9) !important;
  border: none !important;
  background: transparent !important;
}

:global(.ant-picker-header button:hover) {
  color: rgba(255, 255, 255, 1) !important;
  background: rgba(255, 255, 255, 0.15) !important;
}

:global(.ant-picker-header .ant-picker-header-view) {
  color: rgba(255, 255, 255, 0.9) !important;
}

:global(.ant-picker-header .ant-picker-header-view button) {
  color: rgba(255, 255, 255, 0.9) !important;
}

:global(.ant-picker-header .ant-picker-header-view button:hover) {
  color: rgba(255, 255, 255, 1) !important;
  background: rgba(255, 255, 255, 0.15) !important;
}

:global(.ant-picker-content) {
  background: transparent !important;
}

:global(.ant-picker-content th) {
  color: rgba(255, 255, 255, 0.7) !important;
  background: rgba(255, 255, 255, 0.05) !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1) !important;
}

:global(.ant-picker-cell) {
  color: rgba(255, 255, 255, 0.9) !important;
}

:global(.ant-picker-cell .ant-picker-cell-inner) {
  color: rgba(255, 255, 255, 0.9) !important;
  background: transparent !important;
  border: 1px solid transparent !important;
}

:global(.ant-picker-cell:hover .ant-picker-cell-inner) {
  background: rgba(59, 130, 246, 0.3) !important;
  color: rgba(255, 255, 255, 1) !important;
  border: 1px solid rgba(59, 130, 246, 0.5) !important;
}

:global(.ant-picker-cell-selected .ant-picker-cell-inner) {
  background: rgba(59, 130, 246, 0.8) !important;
  color: rgba(255, 255, 255, 1) !important;
  border: 1px solid rgba(59, 130, 246, 1) !important;
}

/* 范围内日期的独立选中样式，不连续 */
:global(.ant-picker-cell-in-range .ant-picker-cell-inner) {
  background: rgba(59, 130, 246, 0.4) !important;
  color: rgba(255, 255, 255, 1) !important;
  border: 1px solid rgba(59, 130, 246, 0.6) !important;
  border-radius: 4px !important;
}

/* 开始和结束日期的强调样式 */
:global(.ant-picker-cell-range-start .ant-picker-cell-inner),
:global(.ant-picker-cell-range-end .ant-picker-cell-inner) {
  background: rgba(59, 130, 246, 0.9) !important;
  color: rgba(255, 255, 255, 1) !important;
  border: 2px solid rgba(59, 130, 246, 1) !important;
  border-radius: 4px !important;
  font-weight: 600 !important;
}

/* 悬停时的范围预览效果 */
:global(.ant-picker-cell-range-hover-start .ant-picker-cell-inner),
:global(.ant-picker-cell-range-hover-end .ant-picker-cell-inner) {
  background: rgba(59, 130, 246, 0.6) !important;
  color: rgba(255, 255, 255, 1) !important;
  border: 2px solid rgba(59, 130, 246, 0.8) !important;
  border-radius: 4px !important;
}

:global(.ant-picker-cell-range-hover .ant-picker-cell-inner) {
  background: rgba(59, 130, 246, 0.25) !important;
  color: rgba(255, 255, 255, 1) !important;
  border: 1px solid rgba(59, 130, 246, 0.4) !important;
  border-radius: 4px !important;
}

/* 移除范围选择的边框连接效果 */
:global(.ant-picker-cell-in-range) {
  position: relative !important;
}

:global(.ant-picker-cell-in-range::before) {
  display: none !important;
}

:global(.ant-picker-cell-range-start::before),
:global(.ant-picker-cell-range-end::before) {
  display: none !important;
}

/* 范围内日期的悬停效果 */
:global(.ant-picker-cell-in-range:hover .ant-picker-cell-inner) {
  background: rgba(59, 130, 246, 0.6) !important;
  color: rgba(255, 255, 255, 1) !important;
  border: 1px solid rgba(59, 130, 246, 0.8) !important;
  border-radius: 4px !important;
  transform: scale(1.05) !important;
  transition: all 0.2s ease !important;
}

/* 确保外层容器透明，只有inner有样式 */
:global(.ant-picker-cell-range-start),
:global(.ant-picker-cell-range-end),
:global(.ant-picker-cell-in-range),
:global(.ant-picker-cell) {
  background: transparent !important;
}

/* 移除所有可能的连接线和伪元素 */
:global(.ant-picker-cell-range-start::before),
:global(.ant-picker-cell-range-start::after),
:global(.ant-picker-cell-range-end::before),
:global(.ant-picker-cell-range-end::after),
:global(.ant-picker-cell-in-range::before),
:global(.ant-picker-cell-in-range::after) {
  display: none !important;
  content: none !important;
}

/* 移除可能的范围连接背景 */
:global(.ant-picker-panel-container .ant-picker-cell-range-start),
:global(.ant-picker-panel-container .ant-picker-cell-range-end),
:global(.ant-picker-panel-container .ant-picker-cell-in-range) {
  background-color: transparent !important;
}

/* 确保每个日期都是独立的圆角框 */
:global(.ant-picker-cell .ant-picker-cell-inner) {
  border-radius: 4px !important;
  margin: 1px !important;
}

:global(.ant-picker-today .ant-picker-cell-inner) {
  border: 1px solid rgba(59, 130, 246, 0.8) !important;
  color: rgba(59, 130, 246, 1) !important;
  font-weight: 600 !important;
}

:global(.ant-picker-cell-disabled .ant-picker-cell-inner) {
  color: rgba(255, 255, 255, 0.3) !important;
  background: transparent !important;
}

:global(.ant-picker-footer) {
  border-top: 1px solid rgba(255, 255, 255, 0.15) !important;
  background: rgba(255, 255, 255, 0.05) !important;
}

:global(.ant-picker-footer .ant-picker-now-btn) {
  color: rgba(59, 130, 246, 0.9) !important;
  border: none !important;
  background: transparent !important;
}

:global(.ant-picker-footer .ant-picker-now-btn:hover) {
  color: rgba(59, 130, 246, 1) !important;
  background: rgba(59, 130, 246, 0.15) !important;
}

/* 中文按钮和文字样式 */
:global(.ant-picker-footer .ant-picker-now-btn),
:global(.ant-picker-header-view button),
:global(.ant-picker-super-prev-icon),
:global(.ant-picker-super-next-icon),
:global(.ant-picker-prev-icon),
:global(.ant-picker-next-icon) {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Helvetica, Arial, sans-serif !important;
}

/* 确保所有日期选择器文字都使用中文字体 */
:global(.ant-picker-dropdown *) {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Helvetica, Arial, sans-serif !important;
}

/* 年月选择面板样式 */
:global(.ant-picker-year-panel),
:global(.ant-picker-month-panel) {
  background: rgba(30, 30, 30, 0.98) !important;
}

:global(.ant-picker-year-panel .ant-picker-cell .ant-picker-cell-inner),
:global(.ant-picker-month-panel .ant-picker-cell .ant-picker-cell-inner) {
  color: rgba(255, 255, 255, 0.9) !important;
}

/* 时间选择器样式 */
:global(.ant-picker-time-panel) {
  background: rgba(30, 30, 30, 0.98) !important;
  border-left: 1px solid rgba(255, 255, 255, 0.15) !important;
}

:global(.ant-picker-time-panel .ant-picker-time-panel-column) {
  color: rgba(255, 255, 255, 0.9) !important;
}

:global(.ant-picker-time-panel .ant-picker-time-panel-cell) {
  color: rgba(255, 255, 255, 0.9) !important;
}

:global(.ant-picker-time-panel .ant-picker-time-panel-cell:hover) {
  background: rgba(59, 130, 246, 0.2) !important;
}

/* 确保所有日期文字都可见 */
:global(.ant-picker-dropdown *) {
  color: rgba(255, 255, 255, 0.9) !important;
}

:global(.ant-picker-dropdown .ant-picker-cell-inner) {
  color: rgba(255, 255, 255, 0.9) !important;
  font-weight: 500 !important;
}

:global(.ant-picker-dropdown .ant-picker-header-view button) {
  color: rgba(255, 255, 255, 0.9) !important;
  font-weight: 600 !important;
}

/* 修复可能的透明度问题 */
:global(.ant-picker-dropdown) {
  opacity: 1 !important;
}

:global(.ant-picker-panel-container) {
  background: rgba(30, 30, 30, 0.98) !important;
}

/* 强制设置日期数字的颜色 */
:global(.ant-picker-cell-inner) {
  color: rgba(255, 255, 255, 0.9) !important;
  background: transparent !important;
}

/* 周标题 */
:global(.ant-picker-content thead th) {
  color: rgba(255, 255, 255, 0.8) !important;
  font-weight: 600 !important;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Helvetica, Arial, sans-serif !important;
}

/* 额外的输入框文字颜色修复 */
:deep(.ant-picker) {
  color: rgba(255, 255, 255, 0.9) !important;
}

:deep(.ant-picker input) {
  color: rgba(255, 255, 255, 0.9) !important;
  -webkit-text-fill-color: rgba(255, 255, 255, 0.9) !important;
}

:deep(.ant-picker-input) {
  color: rgba(255, 255, 255, 0.9) !important;
}

:deep(.ant-picker-input input) {
  color: rgba(255, 255, 255, 0.9) !important;
  -webkit-text-fill-color: rgba(255, 255, 255, 0.9) !important;
}

/* 强制覆盖可能的自动填充样式 */
:deep(.ant-picker input:-webkit-autofill) {
  -webkit-text-fill-color: rgba(255, 255, 255, 0.9) !important;
  -webkit-box-shadow: 0 0 0 1000px rgba(255, 255, 255, 0.05) inset !important;
}

:deep(.ant-picker input:-webkit-autofill:hover) {
  -webkit-text-fill-color: rgba(255, 255, 255, 0.9) !important;
  -webkit-box-shadow: 0 0 0 1000px rgba(255, 255, 255, 0.05) inset !important;
}

:deep(.ant-picker input:-webkit-autofill:focus) {
  -webkit-text-fill-color: rgba(255, 255, 255, 0.9) !important;
  -webkit-box-shadow: 0 0 0 1000px rgba(255, 255, 255, 0.05) inset !important;
}

.date-filter-actions {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
}

.date-btn {
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.date-btn.clear-btn {
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.7);
}

.date-btn.clear-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
}

.date-btn.apply-btn {
  background: rgba(59, 130, 246, 0.15);
  color: rgba(59, 130, 246, 0.9);
  border-color: rgba(59, 130, 246, 0.3);
}

.date-btn.apply-btn:hover {
  background: rgba(59, 130, 246, 0.25);
  color: rgba(59, 130, 246, 1);
  border-color: rgba(59, 130, 246, 0.5);
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
  background: transparent;
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 160px;
}

.category-dropdown-trigger:hover:not(:disabled) {
  border-color: rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.08);
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
  min-width: 203px;
  max-height: 300px;
  overflow-y: auto;
  background: rgba(30, 30, 30, 0.95);
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
  background: rgba(255, 255, 255, 0.08);
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

/* 下拉框过渡动画 */
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

.three-column-container {
  display: grid;
  grid-template-columns: 0.4fr 0.4fr 0.6fr;
  gap: 2rem;
  min-height: 600px;
}

/* 日历列样式 */
.calendar-column {
  display: flex;
  flex-direction: column;
}

.calendar-section {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 1rem;
  padding: 1.5rem;
  height: fit-content;
  backdrop-filter: blur(10px);
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.calendar-title-group {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.calendar-icon {
  width: 1.5rem;
  height: 1.5rem;
  color: rgba(59, 130, 246, 0.8);
}

.calendar-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.calendar-toggle-btn {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.calendar-toggle-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
  border-color: rgba(255, 255, 255, 0.3);
}

.toggle-icon {
  width: 1.25rem;
  height: 1.25rem;
}

/* 日历内容样式 */
.calendar-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* 日历组件样式覆盖 */
:deep(.todo-calendar) {
  background: transparent !important;
  border: none !important;
}

:deep(.todo-calendar .ant-picker-calendar-header) {
  background: rgba(255, 255, 255, 0.05) !important;
  border-radius: 0.5rem !important;
  padding: 0.75rem !important;
  margin-bottom: 1rem !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
}

:deep(.todo-calendar .ant-picker-calendar-header .ant-picker-calendar-year-select),
:deep(.todo-calendar .ant-picker-calendar-header .ant-picker-calendar-month-select) {
  color: rgba(255, 255, 255, 0.9) !important;
  background: rgba(255, 255, 255, 0.05) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  border-radius: 0.375rem !important;
}

:deep(.todo-calendar .ant-picker-content) {
  background: transparent !important;
}

:deep(.todo-calendar .ant-picker-content th) {
  color: rgba(255, 255, 255, 0.7) !important;
  background: rgba(255, 255, 255, 0.05) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  font-weight: 600 !important;
}

:deep(.todo-calendar .ant-picker-content td) {
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  background: rgba(255, 255, 255, 0.02) !important;
  transition: all 0.3s ease !important;
}

:deep(.todo-calendar .ant-picker-content td:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
}

:deep(.todo-calendar .ant-picker-cell-selected) {
  background: rgba(59, 130, 246, 0.2) !important;
  border-color: rgba(59, 130, 246, 0.5) !important;
}

:deep(.todo-calendar .ant-picker-cell-today) {
  border-color: rgba(59, 130, 246, 0.8) !important;
}

/* 日历事件列表样式 */
.calendar-events {
  list-style: none;
  margin: 0;
  padding: 0;
  height: 100%;
  overflow: hidden;
}

.calendar-events li {
  margin-bottom: 2px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.calendar-events li:hover {
  transform: translateX(2px);
}

/* 覆盖 Ant Design Badge 样式以适配深色主题 */
:deep(.calendar-events .ant-badge-status) {
  overflow: hidden;
  white-space: nowrap;
  width: 100%;
  text-overflow: ellipsis;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.9) !important;
  line-height: 1.2;
}

:deep(.calendar-events .ant-badge-status-success) {
  background-color: rgba(34, 197, 94, 0.8) !important;
}

:deep(.calendar-events .ant-badge-status-processing) {
  background-color: rgba(59, 130, 246, 0.8) !important;
}

:deep(.calendar-events .ant-badge-status-warning) {
  background-color: rgba(245, 158, 11, 0.8) !important;
}

:deep(.calendar-events .ant-badge-status-error) {
  background-color: rgba(239, 68, 68, 0.8) !important;
}

:deep(.calendar-events .ant-badge-status-default) {
  background-color: rgba(156, 163, 175, 0.8) !important;
}

:deep(.calendar-events .ant-badge-status-text) {
  color: rgba(255, 255, 255, 0.9) !important;
  font-size: 11px !important;
  margin-left: 8px !important;
}

/* 日历单元格高度调整 */
:deep(.todo-calendar .ant-picker-content td) {
  height: 80px !important;
  vertical-align: top !important;
  padding: 4px !important;
}

/* 今天的日期特殊样式 */
:deep(.todo-calendar .ant-picker-cell-today .calendar-events) {
  background: rgba(59, 130, 246, 0.05) !important;
  border-radius: 4px !important;
  padding: 2px !important;
}

/* 有任务的日期背景 */
:deep(.todo-calendar .ant-picker-content td:has(.calendar-events li)) {
  background: rgba(255, 255, 255, 0.03) !important;
}

/* 选中日期的样式 */
:deep(.todo-calendar .ant-picker-cell-selected .calendar-events) {
  background: rgba(59, 130, 246, 0.1) !important;
  border-radius: 4px !important;
  padding: 2px !important;
}

/* 日历详情样式 */
.calendar-details {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 0.75rem;
  padding: 1rem;
}

.details-title {
  font-size: 1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 1rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.details-icon {
  width: 1.125rem;
  height: 1.125rem;
  color: rgba(59, 130, 246, 0.8);
}

.details-content {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.no-todos-today {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1.5rem;
  color: rgba(255, 255, 255, 0.6);
  font-size: 0.875rem;
}

.no-todos-icon {
  width: 1.25rem;
  height: 1.25rem;
  color: rgba(59, 130, 246, 0.6);
}

.today-todos {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.today-todo-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 0.5rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.today-todo-item:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
  transform: translateY(-1px);
}

.today-todo-item.completed {
  opacity: 0.7;
}

.todo-status {
  display: flex;
  align-items: center;
}

.status-icon {
  width: 1.25rem;
  height: 1.25rem;
  color: rgba(59, 130, 246, 0.8);
}

.today-todo-item.completed .status-icon {
  color: rgba(34, 197, 94, 0.8);
}

.calendar-todo-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.todo-title {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.3;
}

.today-todo-item.completed .todo-title {
  text-decoration: line-through;
  color: rgba(255, 255, 255, 0.6);
}

.todo-date-range {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.6);
}

/* 日历滑动过渡动画 */
.calendar-slide-enter-active,
.calendar-slide-leave-active {
  transition: all 0.4s ease;
}

.calendar-slide-enter-from {
  opacity: 0;
  transform: translateY(-20px);
  max-height: 0;
}

.calendar-slide-leave-to {
  opacity: 0;
  transform: translateY(-20px);
  max-height: 0;
}

.calendar-slide-enter-to,
.calendar-slide-leave-from {
  opacity: 1;
  transform: translateY(0);
  max-height: 1000px;
}

/* 列样式 */
.todo-column {
  display: flex;
  flex-direction: column;
  height: 100%;
  border-radius: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: transparent;
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
  background: transparent;
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
  background: transparent;
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
  background: transparent;
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.875rem;
  transition: all 0.3s ease;
  min-width: 0;
  height: 42px;
  box-sizing: border-box;
}

.todo-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.todo-input:focus {
  outline: none;
  border-color: rgba(59, 130, 246, 0.5);
  background: rgba(255, 255, 255, 0.05);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

/* 内联日期选择器样式 */
:deep(.ant-date-picker-inline) {
  background: rgba(255, 255, 255, 0.05) !important;
  border: 1px solid rgba(255, 255, 255, 0.15) !important;
  border-radius: 0.5rem !important;
  color: rgba(255, 255, 255, 0.9) !important;
  font-size: 0.875rem !important;
  width: 320px !important;
  height: 42px !important;
  padding: 0.5rem 0.75rem !important;
  flex-shrink: 0;
}

:deep(.ant-date-picker-inline:hover) {
  border-color: rgba(255, 255, 255, 0.25) !important;
  background: rgba(255, 255, 255, 0.08) !important;
}

:deep(.ant-date-picker-inline.ant-date-picker-focused) {
  border-color: rgba(59, 130, 246, 0.4) !important;
  background: rgba(255, 255, 255, 0.08) !important;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1) !important;
}

:deep(.ant-date-picker-inline .ant-date-picker-input) {
  color: rgba(255, 255, 255, 0.9) !important;
  height: 100% !important;
  display: flex !important;
  align-items: center !important;
}

:deep(.ant-date-picker-inline .ant-date-picker-input > input) {
  color: rgba(255, 255, 255, 0.9) !important;
  background: transparent !important;
  font-size: 0.875rem !important;
  -webkit-text-fill-color: rgba(255, 255, 255, 0.9) !important;
  height: 100% !important;
  line-height: 1.5 !important;
  padding: 0 !important;
}

:deep(.ant-date-picker-inline .ant-date-picker-input > input::placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
  font-size: 0.875rem !important;
}

/* 确保日期选择器内容不会溢出 */
:deep(.ant-date-picker-inline .ant-date-picker-input) {
  overflow: hidden !important;
}

:deep(.ant-date-picker-inline .ant-date-picker-input > input) {
  text-overflow: ellipsis !important;
  white-space: nowrap !important;
  overflow: hidden !important;
}

:deep(.ant-date-picker-inline .ant-date-picker-separator) {
  color: rgba(255, 255, 255, 0.6) !important;
}

:deep(.ant-date-picker-inline .ant-date-picker-suffix) {
  color: rgba(255, 255, 255, 0.6) !important;
}

:deep(.ant-date-picker-inline .ant-date-picker-clear) {
  color: rgba(255, 255, 255, 0.6) !important;
}

/* 小尺寸 DatePicker 样式 */
:deep(.ant-date-picker-small) {
  background: rgba(255, 255, 255, 0.05) !important;
  border: 1px solid rgba(255, 255, 255, 0.15) !important;
  border-radius: 0.375rem !important;
  color: rgba(255, 255, 255, 0.9) !important;
  font-size: 0.8rem !important;
  width: 240px !important;
  max-width: 100% !important;
}

:deep(.ant-date-picker-small:hover) {
  border-color: rgba(255, 255, 255, 0.25) !important;
  background: rgba(255, 255, 255, 0.08) !important;
}

:deep(.ant-date-picker-small.ant-date-picker-focused) {
  border-color: rgba(59, 130, 246, 0.4) !important;
  background: rgba(255, 255, 255, 0.08) !important;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1) !important;
}

:deep(.ant-date-picker-small .ant-date-picker-input) {
  color: rgba(255, 255, 255, 0.9) !important;
}

:deep(.ant-date-picker-small .ant-date-picker-input > input) {
  color: rgba(255, 255, 255, 0.9) !important;
  background: transparent !important;
  font-size: 0.8rem !important;
}

:deep(.ant-date-picker-small .ant-date-picker-input > input::placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
  font-size: 0.8rem !important;
}

:deep(.ant-date-picker-small .ant-date-picker-input > input::-webkit-input-placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
}

:deep(.ant-date-picker-small .ant-date-picker-input > input::-moz-placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
}

/* 修复小尺寸选中后的文字颜色 */
:deep(.ant-date-picker-small .ant-date-picker-input input[value]) {
  color: rgba(255, 255, 255, 0.9) !important;
}

:deep(.ant-date-picker-small.ant-date-picker-focused .ant-date-picker-input > input) {
  color: rgba(255, 255, 255, 0.9) !important;
}

:deep(.ant-date-picker-small .ant-date-picker-separator) {
  color: rgba(255, 255, 255, 0.6) !important;
}

:deep(.ant-date-picker-small .ant-date-picker-suffix) {
  color: rgba(255, 255, 255, 0.6) !important;
}

:deep(.ant-date-picker-small .ant-date-picker-clear) {
  color: rgba(255, 255, 255, 0.6) !important;
}



.add-btn {
  width: 42px;
  height: 42px;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(59, 130, 246, 0.3);
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(59, 130, 246, 0.1);
  color: rgba(59, 130, 246, 0.9);
  flex-shrink: 0;
}

.add-btn:hover:not(:disabled) {
  background: rgba(59, 130, 246, 0.2);
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

.filter-btn svg {
  width: 1rem;
  height: 1rem;
}



/* 批量操作 */


.bulk-btn svg {
  width: 1rem;
  height: 1rem;
}


/* 任务列表 - 网格布局 */
.todo-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 0.25rem;
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
  background: transparent;
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
  background: transparent;
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
  background: rgba(255, 255, 255, 0.05);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.active-item {
  border-color: rgba(59, 130, 246, 0.3);
  background: rgba(59, 130, 246, 0.05);
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
  background: rgba(34, 197, 94, 0.05);
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
  background: transparent;
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
  font-size: 1rem;
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

/* 任务日期显示 */
.todo-dates {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  margin-top: 0.25rem;
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.6);
}

.todo-dates .date-icon {
  width: 0.875rem;
  height: 0.875rem;
  color: rgba(59, 130, 246, 0.7);
}

.date-range {
  font-weight: 500;
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
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.edit-modal {
  background: rgba(30, 30, 30, 0.95);
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
  background: transparent;
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
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.edit-form-group {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.edit-form-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 0.5rem;
}

.edit-modal-textarea {
  width: 100%;
  min-height: 200px;
  padding: 1rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.05);
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
  background: rgba(255, 255, 255, 0.1);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

/* 编辑模态框中的日期选择器样式 */
:deep(.edit-date-picker) {
  width: 100% !important;
  background: rgba(255, 255, 255, 0.05) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  border-radius: 0.75rem !important;
  color: rgba(255, 255, 255, 0.9) !important;
  font-size: 1rem !important;
  height: 48px !important;
  padding: 0.75rem 1rem !important;
}

:deep(.edit-date-picker:hover) {
  border-color: rgba(255, 255, 255, 0.3) !important;
  background: rgba(255, 255, 255, 0.08) !important;
}

:deep(.edit-date-picker.ant-date-picker-focused) {
  border-color: rgba(59, 130, 246, 0.5) !important;
  background: rgba(255, 255, 255, 0.1) !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1) !important;
}

:deep(.edit-date-picker .ant-date-picker-input) {
  color: rgba(255, 255, 255, 0.9) !important;
  height: 100% !important;
  display: flex !important;
  align-items: center !important;
}

:deep(.edit-date-picker .ant-date-picker-input > input) {
  color: rgba(255, 255, 255, 0.9) !important;
  background: transparent !important;
  font-size: 1rem !important;
  -webkit-text-fill-color: rgba(255, 255, 255, 0.9) !important;
  height: 100% !important;
  line-height: 1.5 !important;
  padding: 0 !important;
}

:deep(.edit-date-picker .ant-date-picker-input > input::placeholder) {
  color: rgba(255, 255, 255, 0.4) !important;
  font-size: 1rem !important;
}

:deep(.edit-date-picker .ant-date-picker-separator) {
  color: rgba(255, 255, 255, 0.6) !important;
}

:deep(.edit-date-picker .ant-date-picker-suffix) {
  color: rgba(255, 255, 255, 0.6) !important;
}

:deep(.edit-date-picker .ant-date-picker-clear) {
  color: rgba(255, 255, 255, 0.6) !important;
}

.edit-modal-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.875rem;
  margin-top: 0.5rem;
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
  background: transparent;
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
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.7);
}

.cancel-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
  border-color: rgba(255, 255, 255, 0.3);
}

.save-btn {
  background: rgba(59, 130, 246, 0.15);
  color: rgba(59, 130, 246, 0.9);
  border-color: rgba(59, 130, 246, 0.3);
}

.save-btn:hover:not(:disabled) {
  background: rgba(59, 130, 246, 0.25);
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

  .date-filter-trigger {
    min-width: 150px;
  }

  .date-filter-dropdown {
    left: 0;
    right: 0;
    min-width: auto;
  }

  .add-todo-form {
    flex-direction: column;
    gap: 1rem;
  }

  :deep(.ant-date-picker) {
    width: 100% !important;
    max-width: 280px !important;
  }

  :deep(.ant-date-picker-small) {
    width: 100% !important;
    max-width: 240px !important;
  }

  :deep(.ant-date-picker-inline) {
    width: 100% !important;
    max-width: 320px !important;
  }

  .three-column-container {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }

  /* 移动端日历样式调整 */
  .calendar-section {
    padding: 1rem;
  }

  .calendar-header {
    margin-bottom: 1rem;
  }

  .calendar-title {
    font-size: 1.125rem;
  }

  :deep(.todo-calendar .ant-picker-calendar-header) {
    padding: 0.5rem !important;
    margin-bottom: 0.75rem !important;
  }

  :deep(.calendar-events .ant-badge-status) {
    font-size: 10px !important;
  }

  :deep(.calendar-events .ant-badge-status-text) {
    font-size: 10px !important;
    margin-left: 6px !important;
  }

  .calendar-details {
    padding: 0.75rem;
  }

  .details-title {
    font-size: 0.875rem;
  }

  .today-todo-item {
    padding: 0.5rem;
  }

  .todo-title {
    font-size: 0.8rem;
  }
}

  /* 编辑模态框移动端适配 */
  .edit-modal-content {
    width: 95vw !important;
    max-width: none !important;
    margin: 1rem !important;
    max-height: 90vh !important;
  }

  .edit-modal-body {
    padding: 1rem !important;
    gap: 1rem !important;
  }

  .edit-form-group {
    gap: 0.5rem !important;
  }

  .edit-modal-textarea {
    min-height: 120px !important;
    font-size: 0.875rem !important;
    padding: 0.75rem !important;
  }

  :deep(.edit-date-picker) {
    font-size: 0.875rem !important;
    height: 42px !important;
    padding: 0.5rem 0.75rem !important;
  }

  :deep(.edit-date-picker .ant-date-picker-input > input) {
    font-size: 0.875rem !important;
  }

</style>