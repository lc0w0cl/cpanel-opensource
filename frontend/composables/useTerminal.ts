import { ref, reactive, onMounted, watch } from 'vue'
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
}

// 终端状态接口
export interface TerminalState {
  isConnected: boolean
  currentServer?: ServerConnection
  connectionHistory: string[]
  terminalOutput: string[]
}

// 获取API基础URL
const getApiBaseUrl = () => {
  const config = useRuntimeConfig()
  return `${config.public.apiBaseUrl}/api`
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
    icon: getServerIconByLocation(config.serverName || '', config.description || '')
  }
}

export const useTerminal = () => {
  // 响应式状态
  const servers = ref<ServerConnection[]>([])
  const selectedServer = ref<ServerConnection | null>(null)
  const isConnecting = ref(false)
  const connectionError = ref('')
  const isLoading = ref(false)

  const terminalState = reactive<TerminalState>({
    isConnected: false,
    currentServer: undefined,
    connectionHistory: [],
    terminalOutput: []
  })

  // WebSocket连接
  const config = useRuntimeConfig()
  const wsUrl = `ws://localhost:8080/ws/terminal`

  const {
    status: wsStatus,
    lastMessage,
    connect: wsConnect,
    disconnect: wsDisconnect,
    send: wsSend
  } = useWebSocket({
    url: wsUrl,
    reconnectInterval: 3000,
    maxReconnectAttempts: 5
  })

  // 监听WebSocket消息
  watch(lastMessage, (message) => {
    if (message) {
      handleWebSocketMessage(message)
    }
  })

  // 处理WebSocket消息
  const handleWebSocketMessage = (message: WebSocketMessage) => {
    switch (message.type) {
      case 'connected':
        terminalState.isConnected = true
        isConnecting.value = false
        connectionError.value = ''
        console.log('SSH连接成功:', message.data)
        break
      case 'output':
        // 将输出添加到终端
        terminalState.terminalOutput.push({
          type: 'output',
          content: message.data,
          timestamp: new Date()
        })
        break
      case 'error':
        connectionError.value = message.data
        isConnecting.value = false
        console.error('SSH连接错误:', message.data)
        break
      case 'disconnected':
        terminalState.isConnected = false
        terminalState.currentServer = undefined
        selectedServer.value = null
        console.log('SSH连接已断开:', message.data)
        break
    }
  }

  // 从数据库加载服务器配置
  const loadServersFromDatabase = async () => {
    isLoading.value = true
    connectionError.value = '' // 清除之前的错误
    try {
      const API_BASE_URL = getApiBaseUrl()
      const response = await apiRequest(`${API_BASE_URL}/system-config/servers`)
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

  // 连接到服务器（真实SSH连接）
  const connectToServer = async (serverId: number) => {
    const server = getServerById(serverId)
    if (!server) {
      connectionError.value = '服务器不存在'
      return false
    }

    if (isConnecting.value) return false

    isConnecting.value = true
    connectionError.value = ''
    selectedServer.value = server

    try {
      // 首先建立WebSocket连接
      if (wsStatus.value !== 'connected') {
        wsConnect()
        // 等待WebSocket连接建立
        await new Promise((resolve, reject) => {
          const timeout = setTimeout(() => reject(new Error('WebSocket连接超时')), 10000)
          const checkConnection = () => {
            if (wsStatus.value === 'connected') {
              clearTimeout(timeout)
              resolve(true)
            } else if (wsStatus.value === 'error') {
              clearTimeout(timeout)
              reject(new Error('WebSocket连接失败'))
            } else {
              setTimeout(checkConnection, 100)
            }
          }
          checkConnection()
        })
      }

      // 获取服务器配置详情（包含认证信息）
      const API_BASE_URL = getApiBaseUrl()
      const response = await apiRequest(`${API_BASE_URL}/system-config/servers/${server.id}/auth-info`)
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

      const sent = wsSend(connectMessage)
      if (!sent) {
        throw new Error('发送连接请求失败')
      }

      // 等待连接结果
      return new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error('SSH连接超时'))
        }, 30000) // 30秒超时

        const checkResult = () => {
          if (terminalState.isConnected) {
            clearTimeout(timeout)
            // 更新服务器状态
            server.status = 'connected'
            server.lastConnected = new Date().toLocaleString('zh-CN')
            terminalState.currentServer = server
            terminalState.connectionHistory.push(`Connected to ${server.name} at ${server.lastConnected}`)
            resolve(true)
          } else if (connectionError.value) {
            clearTimeout(timeout)
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
      console.error(`连接服务器失败: ${server.name}`, error)
      return false
    } finally {
      isConnecting.value = false
    }
  }

  // 断开连接
  const disconnectFromServer = () => {
    if (terminalState.currentServer) {
      // 发送断开连接消息
      wsSend({
        type: 'disconnect',
        data: {}
      })

      terminalState.currentServer.status = 'disconnected'
      terminalState.connectionHistory.push(`Disconnected from ${terminalState.currentServer.name} at ${new Date().toLocaleString('zh-CN')}`)
    }

    terminalState.isConnected = false
    terminalState.currentServer = undefined
    terminalState.terminalOutput = []
    selectedServer.value = null

    // 断开WebSocket连接
    wsDisconnect()
  }

  // 发送命令到终端（真实SSH实现）
  const sendCommand = (command: string) => {
    if (!terminalState.isConnected || !terminalState.currentServer) {
      console.warn('未连接到服务器')
      return false
    }

    // 通过WebSocket发送命令到SSH会话
    const sent = wsSend({
      type: 'command',
      data: {
        command: command
      }
    })

    if (!sent) {
      console.error('发送命令失败：WebSocket未连接')
      return false
    }

    return true
  }

  // 清空终端输出
  const clearTerminal = () => {
    terminalState.terminalOutput = []
    if (terminalState.currentServer) {
      terminalState.terminalOutput.push(`${terminalState.currentServer.username}@${terminalState.currentServer.host}:~$ `)
    }
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

  // 初始化时加载服务器配置
  onMounted(() => {
    loadServersFromDatabase()
  })

  return {
    // 状态
    servers,
    selectedServer,
    isConnecting,
    connectionError,
    isLoading,
    terminalState,
    wsStatus,

    // 方法
    getServers,
    getServerById,
    connectToServer,
    disconnectFromServer,
    sendCommand,
    clearTerminal,
    getStatusColor,
    getStatusIcon,
    loadServersFromDatabase
  }
}
