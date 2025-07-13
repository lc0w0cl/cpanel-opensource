/**
 * 全局终端状态管理插件
 * 负责在页面切换时保持SSH连接状态
 */

import { reactive, ref } from 'vue'
import type { TerminalState, TerminalSession } from '~/composables/useTerminal'

// 全局终端状态
const globalTerminalState = reactive<TerminalState>({
  sessions: new Map<string, TerminalSession>(),
  activeSessionId: undefined,
  connectionHistory: []
})

// 全局WebSocket连接管理
const globalWsConnections = ref<Map<string, any>>(new Map())

// 页面引用计数 - 用于智能连接管理
const pageReferenceCount = ref(0)

// 连接清理定时器
let connectionCleanupTimer: NodeJS.Timeout | null = null

// 连接保持配置
const CONNECTION_KEEP_ALIVE_TIMEOUT = 5 * 60 * 1000 // 5分钟无页面引用后清理连接
const CLEANUP_CHECK_INTERVAL = 30 * 1000 // 30秒检查一次

/**
 * 增加页面引用计数
 */
export const addPageReference = () => {
  pageReferenceCount.value++
  console.log('页面引用计数增加:', pageReferenceCount.value)
  
  // 如果有页面引用，取消清理定时器
  if (connectionCleanupTimer) {
    clearTimeout(connectionCleanupTimer)
    connectionCleanupTimer = null
    console.log('取消连接清理定时器')
  }
}

/**
 * 减少页面引用计数
 */
export const removePageReference = () => {
  pageReferenceCount.value = Math.max(0, pageReferenceCount.value - 1)
  console.log('页面引用计数减少:', pageReferenceCount.value)
  
  // 如果没有页面引用，启动清理定时器
  if (pageReferenceCount.value === 0) {
    scheduleConnectionCleanup()
  }
}

/**
 * 安排连接清理
 */
const scheduleConnectionCleanup = () => {
  console.log('安排连接清理，将在', CONNECTION_KEEP_ALIVE_TIMEOUT / 1000, '秒后执行')
  
  connectionCleanupTimer = setTimeout(() => {
    if (pageReferenceCount.value === 0) {
      console.log('执行连接清理')
      cleanupAllConnections()
    } else {
      console.log('有页面引用，跳过连接清理')
    }
  }, CONNECTION_KEEP_ALIVE_TIMEOUT)
}

/**
 * 清理所有连接
 */
const cleanupAllConnections = () => {
  console.log('开始清理所有SSH连接')
  
  // 断开所有WebSocket连接
  globalWsConnections.value.forEach((wsConnection, sessionId) => {
    try {
      console.log('断开WebSocket连接:', sessionId)
      wsConnection.send({
        type: 'disconnect',
        data: { sessionId }
      })
      wsConnection.disconnect()
    } catch (error) {
      console.warn('断开连接时出错:', sessionId, error)
    }
  })
  
  // 清理状态
  globalTerminalState.sessions.clear()
  globalWsConnections.value.clear()
  globalTerminalState.activeSessionId = undefined
  
  console.log('所有连接已清理')
}

/**
 * 获取全局终端状态
 */
export const getGlobalTerminalState = () => globalTerminalState

/**
 * 获取全局WebSocket连接
 */
export const getGlobalWsConnections = () => globalWsConnections

/**
 * 设置WebSocket连接
 */
export const setGlobalWsConnection = (sessionId: string, connection: any) => {
  globalWsConnections.value.set(sessionId, connection)
}

/**
 * 移除WebSocket连接
 */
export const removeGlobalWsConnection = (sessionId: string) => {
  const connection = globalWsConnections.value.get(sessionId)
  if (connection) {
    try {
      connection.disconnect()
    } catch (error) {
      console.warn('断开连接时出错:', error)
    }
  }
  globalWsConnections.value.delete(sessionId)
}

/**
 * 检查是否有活动连接
 */
export const hasActiveConnections = () => {
  return globalTerminalState.sessions.size > 0
}

/**
 * 获取连接统计信息
 */
export const getConnectionStats = () => {
  return {
    totalSessions: globalTerminalState.sessions.size,
    activeSessions: Array.from(globalTerminalState.sessions.values()).filter(s => s.isConnected).length,
    pageReferences: pageReferenceCount.value,
    hasCleanupTimer: connectionCleanupTimer !== null
  }
}

// 插件定义
export default defineNuxtPlugin(() => {
  // 监听页面可见性变化
  if (process.client) {
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) {
        console.log('页面隐藏，保持连接状态')
      } else {
        console.log('页面显示，恢复连接状态')
      }
    })
    
    // 监听页面卸载
    window.addEventListener('beforeunload', () => {
      console.log('页面即将卸载，保持连接状态')
      // 不在这里清理连接，让定时器处理
    })
    
    // 定期检查连接状态
    setInterval(() => {
      const stats = getConnectionStats()
      if (stats.totalSessions > 0) {
        console.log('连接状态检查:', stats)
      }
    }, CLEANUP_CHECK_INTERVAL)
  }
  
  return {
    provide: {
      terminalState: {
        addPageReference,
        removePageReference,
        getGlobalTerminalState,
        getGlobalWsConnections,
        setGlobalWsConnection,
        removeGlobalWsConnection,
        hasActiveConnections,
        getConnectionStats,
        cleanupAllConnections
      }
    }
  }
})
