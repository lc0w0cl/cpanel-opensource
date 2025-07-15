import { ref, reactive, onMounted, watch, onUnmounted } from 'vue'
import { apiRequest } from '~/composables/useJwt'
import { useWebSocket, type WebSocketMessage } from '~/composables/useWebSocket'

// 服务器连接信息接口
export interface ServerConnection {
  id: number
  name: string
  host: string
  port: number
  username: string
  protocol: 'ssh' | 'telnet'
  description?: string
  status: 'connected' | 'disconnected' | 'connecting'
  lastConnected?: string
  icon: string // 服务器图标（国旗或其他标识）
  groupName?: string // 服务器分组
}

// 终端连接会话接口
export interface TerminalSession {
  id: string
  server: ServerConnection
  isConnected: boolean
  terminalOutput: any[]
  lastActivity: Date
}

// 终端状态接口
export interface TerminalState {
  sessions: Map<string, TerminalSession>
  activeSessionId?: string
  connectionHistory: string[]
}

// 获取API基础URL
const getApiBaseUrl = () => {
  const config = useRuntimeConfig()
  return `${config.public.apiBaseUrl}/api`
}

// 获取WebSocket基础URL
const getWebSocketBaseUrl = () => {
  const config = useRuntimeConfig()
  if (config.public.isDevelopment) {
    // 开发环境使用localhost
    return 'ws://localhost:8080'
  } else {
    // 生产环境使用当前域名
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${protocol}//${window.location.host}`
  }
}

// 根据地区/描述推断服务器图标
const getServerIconByLocation = (serverName: string, description: string): string => {
  const name = serverName.toLowerCase()
  const desc = description.toLowerCase()

  // 根据服务器名称推断
  if (name.includes('工控') || name.includes('本地') || name.includes('factory')) {
    return 'material-symbols:factory'
  }
  if (name.includes('西雅图') || name.includes('seattle') || desc.includes('us') || desc.includes('美国')) {
    return 'flagpack:us'
  }
  if (name.includes('首尔') || name.includes('seoul') || name.includes('韩国') || desc.includes('kr')) {
    return 'flagpack:kr'
  }
  if (name.includes('上海') || name.includes('广州') || name.includes('北京') || name.includes('深圳') ||
      name.includes('中国') || desc.includes('cn') || desc.includes('腾讯') || desc.includes('阿里')) {
    return 'flagpack:cn'
  }
  if (name.includes('东京') || name.includes('大阪') || name.includes('日本') || desc.includes('jp')) {
    return 'flagpack:jp'
  }
  if (name.includes('德国') || name.includes('柏林') || desc.includes('de') || desc.includes('德')) {
    return 'flagpack:de'
  }
  if (name.includes('英国') || name.includes('伦敦') || desc.includes('gb') || desc.includes('uk')) {
    return 'flagpack:gb'
  }
  if (name.includes('法国') || name.includes('巴黎') || desc.includes('fr')) {
    return 'flagpack:fr'
  }
  if (name.includes('新加坡') || desc.includes('sg') || desc.includes('singapore')) {
    return 'flagpack:sg'
  }
  if (name.includes('香港') || desc.includes('hk') || desc.includes('hong kong')) {
    return 'flagpack:hk'
  }

  // 默认图标
  return 'material-symbols:dns'
}

// 从数据库配置转换为终端服务器格式
const convertToServerConnection = (config: any): ServerConnection => {
  return {
    id: config.id,
    name: config.serverName || 'Unknown Server',
    host: config.host || 'localhost',
    port: config.port || 22,
    username: config.username || 'root',
    protocol: 'ssh', // 目前只支持SSH
    description: config.description || '',
    status: 'disconnected',
    lastConnected: undefined,
    icon: config.icon || getServerIconByLocation(config.serverName || '', config.description || ''),
    groupName: config.groupName || '默认分组'
  }
}

export const useTerminal = () => {
  // 响应式状态
  const servers = ref<ServerConnection[]>([])
  const selectedServer = ref<ServerConnection | null>(null)
  const isConnecting = ref(false)
  const connectionError = ref('')
  const isLoading = ref(false)

  // 使用全局状态管理
  const { $terminalState } = useNuxtApp()
  const terminalState = $terminalState.getGlobalTerminalState()
  const wsConnections = $terminalState.getGlobalWsConnections()

  // WebSocket连接管理
  const config = useRuntimeConfig()

  // 创建WebSocket连接
  const createWebSocketConnection = (sessionId: string) => {
    const wsBaseUrl = getWebSocketBaseUrl()
    const wsUrl = `${wsBaseUrl}/ws/terminal`

    const wsConnection = useWebSocket({
      url: wsUrl,
      reconnectInterval: 3000,
      maxReconnectAttempts: 5
    })

    $terminalState.setGlobalWsConnection(sessionId, wsConnection)
    return wsConnection
  }

  // 处理WebSocket消息
  const handleWebSocketMessage = (sessionId: string, message: WebSocketMessage) => {
    const session = terminalState.sessions.get(sessionId)
    if (!session) return

    switch (message.type) {
      case 'connected':
        session.isConnected = true
        session.lastActivity = new Date()
        isConnecting.value = false
        connectionError.value = ''
        console.log('SSH连接成功:', sessionId, message.data)
        break
      case 'output':
        // 将输出添加到对应会话的终端
        console.log('收到输出消息，会话:', sessionId, '长度:', message.data?.length)
        session.terminalOutput.push({
          type: 'output',
          content: message.data,
          timestamp: new Date()
        })
        session.lastActivity = new Date()
        break
      case 'error':
        connectionError.value = message.data
        isConnecting.value = false
        console.error('SSH连接错误:', sessionId, message.data)
        break
      case 'disconnected':
        session.isConnected = false
        console.log('SSH连接已断开:', sessionId, message.data)
        // 从会话列表中移除
        terminalState.sessions.delete(sessionId)
        $terminalState.removeGlobalWsConnection(sessionId)
        // 如果是当前活动会话，切换到其他会话
        if (terminalState.activeSessionId === sessionId) {
          const remainingSessions = Array.from(terminalState.sessions.keys())
          terminalState.activeSessionId = remainingSessions.length > 0 ? remainingSessions[0] : undefined
        }
        break
    }
  }

  // 从数据库加载服务器配置
  const loadServersFromDatabase = async () => {
    isLoading.value = true
    connectionError.value = '' // 清除之前的错误
    try {
      const API_BASE_URL = getApiBaseUrl()
      const response = await apiRequest(`${API_BASE_URL}/servers`)
      const result = await response.json()

      if (result.success && result.data) {
        // 转换数据库配置为终端服务器格式
        const serverConfigs = result.data.map((config: any) => convertToServerConnection(config))
        servers.value = serverConfigs
        console.log('成功加载服务器配置:', serverConfigs.length, '个服务器')
      } else {
        console.error('加载服务器配置失败:', result.message)
        connectionError.value = '加载服务器配置失败: ' + (result.message || '未知错误')
      }
    } catch (error) {
      console.error('加载服务器配置出错:', error)
      connectionError.value = '无法连接到服务器配置API'
    } finally {
      isLoading.value = false
    }
  }

  // 获取所有服务器
  const getServers = () => {
    return servers.value
  }

  // 根据ID获取服务器
  const getServerById = (id: number) => {
    return servers.value.find(server => server.id === id)
  }

  // 获取分组化的服务器列表
  const getGroupedServers = () => {
    const grouped: Record<string, ServerConnection[]> = {}

    servers.value.forEach(server => {
      const groupName = server.groupName || '默认分组'
      if (!grouped[groupName]) {
        grouped[groupName] = []
      }
      grouped[groupName].push(server)
    })

    return grouped
  }

  // 获取所有分组名称
  const getAllGroups = () => {
    const groups = new Set<string>()
    servers.value.forEach(server => {
      groups.add(server.groupName || '默认分组')
    })
    return Array.from(groups).sort()
  }

  // 生成唯一的会话ID
  const generateSessionId = (serverId: number) => {
    return `session_${serverId}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  }

  // 连接到服务器（真实SSH连接）
  const connectToServer = async (serverId: number) => {
    const server = getServerById(serverId)
    if (!server) {
      connectionError.value = '服务器不存在'
      return null
    }

    if (isConnecting.value) return null

    isConnecting.value = true
    connectionError.value = ''
    selectedServer.value = server

    // 生成唯一的会话ID
    const sessionId = generateSessionId(serverId)

    try {
      // 创建新的WebSocket连接
      const wsConnection = createWebSocketConnection(sessionId)

      // 监听该连接的消息
      watch(wsConnection.lastMessage, (message) => {
        if (message) {
          handleWebSocketMessage(sessionId, message)
        }
      })

      // 建立WebSocket连接
      wsConnection.connect()

      // 等待WebSocket连接建立
      await new Promise((resolve, reject) => {
        const timeout = setTimeout(() => reject(new Error('WebSocket连接超时')), 10000)
        const checkConnection = () => {
          if (wsConnection.status.value === 'connected') {
            clearTimeout(timeout)
            resolve(true)
          } else if (wsConnection.status.value === 'error') {
            clearTimeout(timeout)
            reject(new Error('WebSocket连接失败'))
          } else {
            setTimeout(checkConnection, 100)
          }
        }
        checkConnection()
      })

      // 创建终端会话
      const session: TerminalSession = {
        id: sessionId,
        server: { ...server },
        isConnected: false,
        terminalOutput: [],
        lastActivity: new Date()
      }

      // 添加到会话列表
      terminalState.sessions.set(sessionId, session)

      // 设置为当前活动会话
      terminalState.activeSessionId = sessionId

      // 获取服务器配置详情（包含认证信息）
      const API_BASE_URL = getApiBaseUrl()
      const response = await apiRequest(`${API_BASE_URL}/servers/${server.id}/auth-info`)
      const result = await response.json()

      if (!result.success) {
        throw new Error('获取服务器配置失败: ' + result.message)
      }

      const serverConfig = result.data
      if (!serverConfig) {
        throw new Error('服务器配置不存在')
      }

      console.log('服务器配置详情:', serverConfig)

      // 验证必需字段
      if (!serverConfig.host) {
        throw new Error('服务器地址不能为空')
      }
      if (!serverConfig.username) {
        throw new Error('用户名不能为空')
      }
      if (!serverConfig.authType) {
        throw new Error('认证类型不能为空')
      }

      // 发送SSH连接请求
      const connectMessage: WebSocketMessage = {
        type: 'connect',
        data: {
          sessionId: sessionId,
          host: serverConfig.host,
          port: serverConfig.port || 22,
          username: serverConfig.username,
          authType: serverConfig.authType,
          password: serverConfig.password || null,
          privateKey: serverConfig.privateKey || null,
          privateKeyPassword: serverConfig.privateKeyPassword || null
        }
      }

      console.log('发送连接消息:', connectMessage)

      const sent = wsConnection.send(connectMessage)
      if (!sent) {
        throw new Error('发送连接请求失败')
      }

      // 等待连接结果
      return new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error('SSH连接超时'))
        }, 30000) // 30秒超时

        const checkResult = () => {
          const currentSession = terminalState.sessions.get(sessionId)
          if (currentSession?.isConnected) {
            clearTimeout(timeout)
            // 更新服务器状态
            server.status = 'connected'
            server.lastConnected = new Date().toLocaleString('zh-CN')
            currentSession.server = { ...server }
            terminalState.connectionHistory.push(`Connected to ${server.name} at ${server.lastConnected}`)
            resolve(sessionId)
          } else if (connectionError.value) {
            clearTimeout(timeout)
            // 清理失败的会话
            terminalState.sessions.delete(sessionId)
            $terminalState.removeGlobalWsConnection(sessionId)
            reject(new Error(connectionError.value))
          } else {
            setTimeout(checkResult, 100)
          }
        }
        checkResult()
      })

    } catch (error) {
      connectionError.value = error instanceof Error ? error.message : '连接失败'
      server.status = 'disconnected'
      // 清理失败的会话
      terminalState.sessions.delete(sessionId)
      $terminalState.removeGlobalWsConnection(sessionId)
      console.error(`连接服务器失败: ${server.name}`, error)
      return null
    } finally {
      isConnecting.value = false
    }
  }

  // 断开指定会话的连接
  const disconnectFromServer = (sessionId?: string) => {
    const targetSessionId = sessionId || terminalState.activeSessionId
    if (!targetSessionId) return

    const session = terminalState.sessions.get(targetSessionId)
    const wsConnection = wsConnections.value.get(targetSessionId)

    if (session && wsConnection) {
      // 发送断开连接消息
      wsConnection.send({
        type: 'disconnect',
        data: { sessionId: targetSessionId }
      })

      // 更新会话中的服务器状态
      session.server.status = 'disconnected'

      // 同时更新servers数组中对应服务器的状态
      const serverInList = servers.value.find(s => s.id === session.server.id)
      if (serverInList) {
        serverInList.status = 'disconnected'
      }

      terminalState.connectionHistory.push(`Disconnected from ${session.server.name} at ${new Date().toLocaleString('zh-CN')}`)

      // 断开WebSocket连接
      wsConnection.disconnect()
    }

    // 从会话列表中移除
    terminalState.sessions.delete(targetSessionId)
    $terminalState.removeGlobalWsConnection(targetSessionId)

    // 如果是当前活动会话，切换到其他会话
    if (terminalState.activeSessionId === targetSessionId) {
      const remainingSessions = Array.from(terminalState.sessions.keys())
      terminalState.activeSessionId = remainingSessions.length > 0 ? remainingSessions[0] : undefined
    }
  }

  // 断开所有连接
  const disconnectAllSessions = () => {
    const sessionIds = Array.from(terminalState.sessions.keys())
    sessionIds.forEach(sessionId => disconnectFromServer(sessionId))
  }

  // 发送命令到指定会话的终端
  const sendCommand = (command: string, sessionId?: string) => {
    const targetSessionId = sessionId || terminalState.activeSessionId
    if (!targetSessionId) {
      console.warn('没有活动的终端会话')
      return false
    }

    const session = terminalState.sessions.get(targetSessionId)
    const wsConnection = wsConnections.value.get(targetSessionId)

    if (!session?.isConnected || !wsConnection) {
      console.warn('会话未连接到服务器')
      return false
    }

    // 通过WebSocket发送命令到SSH会话
    const message = {
      type: 'command',
      data: {
        sessionId: targetSessionId,
        command: command
      }
    }

    const sent = wsConnection.send(message)

    if (!sent) {
      console.error('发送命令失败：WebSocket未连接')
      return false
    }

    return true
  }

  // 发送终端大小调整消息
  const sendResizeMessage = (cols: number, rows: number, sessionId?: string) => {
    const targetSessionId = sessionId || terminalState.activeSessionId
    if (!targetSessionId) {
      console.warn('没有活动的终端会话')
      return false
    }

    const session = terminalState.sessions.get(targetSessionId)
    const wsConnection = wsConnections.value.get(targetSessionId)

    if (!session?.isConnected || !wsConnection) {
      console.warn('会话未连接到服务器')
      return false
    }

    // 通过WebSocket发送resize消息到SSH会话
    const message = {
      type: 'resize',
      data: {
        sessionId: targetSessionId,
        cols: cols,
        rows: rows
      }
    }

    const sent = wsConnection.send(message)

    if (!sent) {
      console.error('发送resize消息失败：WebSocket未连接')
      return false
    }

    console.log(`发送终端大小调整消息: ${cols}x${rows}, 会话: ${targetSessionId}`)
    return true
  }

  // 清空指定会话的终端输出
  const clearTerminal = (sessionId?: string) => {
    const targetSessionId = sessionId || terminalState.activeSessionId
    if (!targetSessionId) return

    const session = terminalState.sessions.get(targetSessionId)
    if (session) {
      session.terminalOutput = []
    }
  }

  // 切换活动会话
  const switchToSession = (sessionId: string) => {
    if (terminalState.sessions.has(sessionId)) {
      terminalState.activeSessionId = sessionId
    }
  }

  // 获取当前活动会话
  const getActiveSession = () => {
    if (!terminalState.activeSessionId) return null
    return terminalState.sessions.get(terminalState.activeSessionId) || null
  }

  // 获取所有会话列表
  const getAllSessions = () => {
    return Array.from(terminalState.sessions.values())
  }

  // 获取连接状态颜色
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'connected':
        return 'text-green-500'
      case 'connecting':
        return 'text-yellow-500'
      case 'disconnected':
      default:
        return 'text-gray-500'
    }
  }

  // 获取连接状态图标
  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'connected':
        return 'material-symbols:check-circle'
      case 'connecting':
        return 'material-symbols:sync'
      case 'disconnected':
      default:
        return 'material-symbols:radio-button-unchecked'
    }
  }

  // 页面引用计数管理
  const pageReferenceAdded = ref(false)

  // 添加页面引用
  const addPageReference = () => {
    if (!pageReferenceAdded.value) {
      $terminalState.addPageReference()
      pageReferenceAdded.value = true
    }
  }

  // 移除页面引用
  const removePageReference = () => {
    if (pageReferenceAdded.value) {
      $terminalState.removePageReference()
      pageReferenceAdded.value = false
    }
  }

  // 初始化时加载服务器配置并添加页面引用
  onMounted(() => {
    loadServersFromDatabase()
    addPageReference()
  })

  // 组件卸载时移除页面引用（但不断开连接）
  onUnmounted(() => {
    removePageReference()
  })

  return {
    // 状态
    servers,
    selectedServer,
    isConnecting,
    connectionError,
    isLoading,
    terminalState,

    // 方法
    getServers,
    getServerById,
    getGroupedServers,
    getAllGroups,
    connectToServer,
    disconnectFromServer,
    disconnectAllSessions,
    sendCommand,
    sendResizeMessage,
    clearTerminal,
    switchToSession,
    getActiveSession,
    getAllSessions,
    getStatusColor,
    getStatusIcon,
    loadServersFromDatabase,

    // 页面引用管理
    addPageReference,
    removePageReference
  }
}
