/**
 * TODO API 相关的组合式函数
 */

// 任务接口定义
export interface Todo {
  id: number
  text: string
  completed: boolean
  categoryId?: number
  sortOrder?: number
  startDate?: string
  endDate?: string
  createdAt: string
  updatedAt: string
}

/**
 * TODO API 服务
 */
export const useTodoApi = () => {
  const config = useRuntimeConfig()
  const API_BASE_URL = `${config.public.apiBaseUrl}/api`

  /**
   * 获取所有任务
   */
  const getAllTodos = async (): Promise<Todo[]> => {
    try {
      const response = await apiRequest(`${API_BASE_URL}/todos`)
      const result = await response.json()
      
      if (result.success) {
        return result.data || []
      } else {
        console.error('获取任务列表失败:', result.message)
        return []
      }
    } catch (error) {
      console.error('获取任务列表异常:', error)
      return []
    }
  }

  /**
   * 创建新任务
   */
  const createTodo = async (text: string): Promise<Todo | null> => {
    try {
      const response = await apiRequest(`${API_BASE_URL}/todos`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ text })
      })
      const result = await response.json()
      
      if (result.success) {
        return result.data
      } else {
        console.error('创建任务失败:', result.message)
        return null
      }
    } catch (error) {
      console.error('创建任务异常:', error)
      return null
    }
  }

  /**
   * 更新任务内容
   */
  const updateTodoText = async (id: number, text: string): Promise<boolean> => {
    try {
      const response = await apiRequest(`${API_BASE_URL}/todos/${id}/text`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ text })
      })
      const result = await response.json()
      
      if (result.success) {
        return true
      } else {
        console.error('更新任务内容失败:', result.message)
        return false
      }
    } catch (error) {
      console.error('更新任务内容异常:', error)
      return false
    }
  }

  /**
   * 切换任务完成状态
   */
  const toggleTodoCompleted = async (id: number): Promise<boolean> => {
    try {
      const response = await apiRequest(`${API_BASE_URL}/todos/${id}/toggle`, {
        method: 'PUT'
      })
      const result = await response.json()
      
      if (result.success) {
        return true
      } else {
        console.error('切换任务状态失败:', result.message)
        return false
      }
    } catch (error) {
      console.error('切换任务状态异常:', error)
      return false
    }
  }

  /**
   * 删除任务
   */
  const deleteTodo = async (id: number): Promise<boolean> => {
    try {
      const response = await apiRequest(`${API_BASE_URL}/todos/${id}`, {
        method: 'DELETE'
      })
      const result = await response.json()
      
      if (result.success) {
        return true
      } else {
        console.error('删除任务失败:', result.message)
        return false
      }
    } catch (error) {
      console.error('删除任务异常:', error)
      return false
    }
  }

  /**
   * 批量更新任务排序
   */
  const updateTodosSortOrder = async (todoIds: number[]): Promise<boolean> => {
    try {
      console.log('发送排序请求，todoIds:', todoIds)

      const response = await apiRequest(`${API_BASE_URL}/todos/sort`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ todoIds })
      })
      const result = await response.json()

      console.log('排序响应:', result)

      if (result.success) {
        return true
      } else {
        console.error('更新任务排序失败:', result.message)
        return false
      }
    } catch (error) {
      console.error('更新任务排序异常:', error)
      return false
    }
  }

  /**
   * 批量设置任务完成状态
   */
  const setAllTodosCompleted = async (completed: boolean): Promise<boolean> => {
    try {
      const response = await apiRequest(`${API_BASE_URL}/todos/toggle-all`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ completed })
      })
      const result = await response.json()

      if (result.success) {
        return true
      } else {
        console.error('批量设置任务状态失败:', result.message)
        return false
      }
    } catch (error) {
      console.error('批量设置任务状态异常:', error)
      return false
    }
  }

  /**
   * 批量设置指定任务的完成状态
   */
  const setTodosCompleted = async (todoIds: number[], completed: boolean): Promise<boolean> => {
    try {
      const response = await apiRequest(`${API_BASE_URL}/todos/batch-toggle`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ todoIds, completed })
      })
      const result = await response.json()

      if (result.success) {
        return true
      } else {
        console.error('批量设置指定任务状态失败:', result.message)
        return false
      }
    } catch (error) {
      console.error('批量设置指定任务状态异常:', error)
      return false
    }
  }

  /**
   * 删除所有已完成的任务
   */
  const deleteCompletedTodos = async (): Promise<number> => {
    try {
      const response = await apiRequest(`${API_BASE_URL}/todos/completed`, {
        method: 'DELETE'
      })
      const result = await response.json()

      if (result.success) {
        return result.data.deletedCount || 0
      } else {
        console.error('删除已完成任务失败:', result.message)
        return 0
      }
    } catch (error) {
      console.error('删除已完成任务异常:', error)
      return 0
    }
  }

  /**
   * 根据日期范围获取任务列表
   */
  const getTodosByDateRange = async (startDate: string, endDate: string): Promise<Todo[]> => {
    try {
      const response = await apiRequest(`${API_BASE_URL}/todos/date-range?startDate=${startDate}&endDate=${endDate}`)
      const result = await response.json()

      if (result.success) {
        return result.data || []
      } else {
        console.error('根据日期范围获取任务列表失败:', result.message)
        return []
      }
    } catch (error) {
      console.error('根据日期范围获取任务列表异常:', error)
      return []
    }
  }

  /**
   * 根据分组ID和日期范围获取任务列表
   */
  const getTodosByCategoryAndDateRange = async (categoryId: number, startDate: string, endDate: string): Promise<Todo[]> => {
    try {
      const response = await apiRequest(`${API_BASE_URL}/todos/category/${categoryId}/date-range?startDate=${startDate}&endDate=${endDate}`)
      const result = await response.json()

      if (result.success) {
        return result.data || []
      } else {
        console.error('根据分组和日期范围获取任务列表失败:', result.message)
        return []
      }
    } catch (error) {
      console.error('根据分组和日期范围获取任务列表异常:', error)
      return []
    }
  }

  /**
   * 更新任务日期
   */
  const updateTodoDates = async (id: number, startDate?: string, endDate?: string): Promise<boolean> => {
    try {
      const response = await apiRequest(`${API_BASE_URL}/todos/${id}/dates`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ startDate, endDate })
      })
      const result = await response.json()

      if (result.success) {
        return true
      } else {
        console.error('更新任务日期失败:', result.message)
        return false
      }
    } catch (error) {
      console.error('更新任务日期异常:', error)
      return false
    }
  }

  return {
    getAllTodos,
    createTodo,
    updateTodoText,
    toggleTodoCompleted,
    deleteTodo,
    updateTodosSortOrder,
    setAllTodosCompleted,
    setTodosCompleted,
    deleteCompletedTodos,
    getTodosByDateRange,
    getTodosByCategoryAndDateRange,
    updateTodoDates
  }
}
