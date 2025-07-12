import { ref, onUnmounted } from 'vue'

/**
 * WebSocket连接状态
 */
export type WebSocketStatus = 'connecting' | 'connected' | 'disconnected' | 'error'

/**
 * WebSocket消息类型
 */
export interface WebSocketMessage {
  type: string
  data: any
}

/**
 * WebSocket连接配置
 */
export interface WebSocketConfig {
  url: string
  protocols?: string[]
  reconnectInterval?: number
  maxReconnectAttempts?: number
}

/**
 * WebSocket组合式函数
 */
export const useWebSocket = (config: WebSocketConfig) => {
  const socket = ref<WebSocket | null>(null)
  const status = ref<WebSocketStatus>('disconnected')
  const lastMessage = ref<WebSocketMessage | null>(null)
  const error = ref<string>('')
  
  let reconnectAttempts = 0
  let reconnectTimer: NodeJS.Timeout | null = null
  
  const maxReconnectAttempts = config.maxReconnectAttempts || 5
  const reconnectInterval = config.reconnectInterval || 3000

  /**
   * 连接WebSocket
   */
  const connect = () => {
    if (socket.value?.readyState === WebSocket.OPEN) {
      return
    }

    status.value = 'connecting'
    error.value = ''

    try {
      socket.value = new WebSocket(config.url, config.protocols)

      socket.value.onopen = () => {
        status.value = 'connected'
        reconnectAttempts = 0
        console.log('WebSocket连接已建立')
      }

      socket.value.onmessage = (event) => {
        try {
          console.log('收到WebSocket原始消息:', event.data)

          // 直接解析JSON，因为后端现在使用Jackson正确序列化
          const message: WebSocketMessage = JSON.parse(event.data)
          lastMessage.value = message

          console.log('解析后的消息:', message)
        } catch (e) {
          console.error('解析WebSocket消息失败:', e)
          console.error('原始消息:', event.data)
          console.error('消息长度:', event.data?.length)
          console.error('消息类型:', typeof event.data)

          // 如果JSON解析失败，尝试处理为纯文本输出
          if (typeof event.data === 'string') {
            try {
              // 尝试修复常见的JSON问题
              let fixedData = event.data
                .replace(/\\/g, '\\\\')  // 转义反斜杠
                .replace(/"/g, '\\"')    // 转义引号
                .replace(/\n/g, '\\n')   // 转义换行
                .replace(/\r/g, '\\r')   // 转义回车
                .replace(/\t/g, '\\t')   // 转义制表符

              // 如果不是JSON格式，包装成输出消息
              if (!fixedData.trim().startsWith('{')) {
                const textMessage: WebSocketMessage = {
                  type: 'output',
                  data: event.data
                }
                lastMessage.value = textMessage
                console.log('处理为文本消息:', textMessage)
              } else {
                // 尝试再次解析修复后的JSON
                const message: WebSocketMessage = JSON.parse(fixedData)
                lastMessage.value = message
                console.log('修复后解析成功:', message)
              }
            } catch (e2) {
              console.error('修复JSON也失败了:', e2)
              // 最后的备选方案：作为纯文本处理
              const fallbackMessage: WebSocketMessage = {
                type: 'output',
                data: event.data
              }
              lastMessage.value = fallbackMessage
            }
          }
        }
      }

      socket.value.onclose = (event) => {
        status.value = 'disconnected'
        console.log('WebSocket连接已关闭:', event.code, event.reason)
        
        // 如果不是主动关闭，尝试重连
        if (event.code !== 1000 && reconnectAttempts < maxReconnectAttempts) {
          scheduleReconnect()
        }
      }

      socket.value.onerror = (event) => {
        status.value = 'error'
        error.value = 'WebSocket连接错误'
        console.error('WebSocket错误:', event)
      }

    } catch (e) {
      status.value = 'error'
      error.value = 'WebSocket连接失败'
      console.error('创建WebSocket连接失败:', e)
    }
  }

  /**
   * 断开WebSocket连接
   */
  const disconnect = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    
    if (socket.value) {
      socket.value.close(1000, 'User disconnected')
      socket.value = null
    }
    
    status.value = 'disconnected'
  }

  /**
   * 发送消息
   */
  const send = (message: WebSocketMessage) => {
    if (socket.value?.readyState === WebSocket.OPEN) {
      socket.value.send(JSON.stringify(message))
      return true
    } else {
      console.warn('WebSocket未连接，无法发送消息')
      return false
    }
  }

  /**
   * 安排重连
   */
  const scheduleReconnect = () => {
    if (reconnectAttempts >= maxReconnectAttempts) {
      console.error('达到最大重连次数，停止重连')
      return
    }

    reconnectAttempts++
    console.log(`${reconnectInterval}ms后尝试第${reconnectAttempts}次重连`)
    
    reconnectTimer = setTimeout(() => {
      connect()
    }, reconnectInterval)
  }

  /**
   * 手动重连
   */
  const reconnect = () => {
    disconnect()
    reconnectAttempts = 0
    setTimeout(connect, 1000)
  }

  // 组件卸载时清理
  onUnmounted(() => {
    disconnect()
  })

  return {
    socket: readonly(socket),
    status: readonly(status),
    lastMessage: readonly(lastMessage),
    error: readonly(error),
    connect,
    disconnect,
    send,
    reconnect
  }
}
