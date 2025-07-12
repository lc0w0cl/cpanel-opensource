import { ref, reactive, onMounted } from 'vue'
import { apiRequest } from '~/composables/useJwt'

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

  // 连接到服务器（虚拟实现）
  const connectToServer = async (serverId: number) => {
    const server = getServerById(serverId)
    if (!server) {
      connectionError.value = '服务器不存在'
      return false
    }

    isConnecting.value = true
    connectionError.value = ''
    
    try {
      // 模拟连接延迟
      await new Promise(resolve => setTimeout(resolve, 2000))
      
      // 更新服务器状态
      server.status = 'connected'
      server.lastConnected = new Date().toLocaleString('zh-CN')
      
      // 更新终端状态
      terminalState.isConnected = true
      terminalState.currentServer = server
      terminalState.connectionHistory.push(`Connected to ${server.name} at ${server.lastConnected}`)
      
      // 添加欢迎信息到终端输出
      terminalState.terminalOutput = [
        `Welcome to ${server.name}`,
        `Last login: ${server.lastConnected}`,
        `${server.username}@${server.host}:~$ `
      ]
      
      selectedServer.value = server
      return true
    } catch (error) {
      connectionError.value = '连接失败'
      server.status = 'disconnected'
      return false
    } finally {
      isConnecting.value = false
    }
  }

  // 断开连接
  const disconnectFromServer = () => {
    if (terminalState.currentServer) {
      terminalState.currentServer.status = 'disconnected'
      terminalState.connectionHistory.push(`Disconnected from ${terminalState.currentServer.name} at ${new Date().toLocaleString('zh-CN')}`)
    }
    
    terminalState.isConnected = false
    terminalState.currentServer = undefined
    terminalState.terminalOutput = []
    selectedServer.value = null
  }

  // 发送命令到终端（虚拟实现）
  const sendCommand = (command: string) => {
    if (!terminalState.isConnected || !terminalState.currentServer) {
      return
    }

    // 添加命令到输出
    terminalState.terminalOutput.push(`${terminalState.currentServer.username}@${terminalState.currentServer.host}:~$ ${command}`)
    
    // 模拟命令响应
    setTimeout(() => {
      let response = ''
      
      switch (command.toLowerCase().trim()) {
        case 'ls':
          response = 'Documents  Downloads  Pictures  Videos  workspace'
          break
        case 'pwd':
          response = `/home/${terminalState.currentServer?.username}`
          break
        case 'whoami':
          response = terminalState.currentServer?.username || 'unknown'
          break
        case 'date':
          response = new Date().toString()
          break
        case 'uname -a':
          response = 'Linux server 5.4.0-74-generic #83-Ubuntu SMP Sat May 8 02:35:39 UTC 2021 x86_64 x86_64 x86_64 GNU/Linux'
          break
        case 'df -h':
          response = `Filesystem      Size  Used Avail Use% Mounted on
/dev/sda1        20G  8.5G   11G  45% /
tmpfs           2.0G     0  2.0G   0% /dev/shm`
          break
        case 'free -h':
          response = `              total        used        free      shared  buff/cache   available
Mem:           4.0G        1.2G        1.8G         50M        1.0G        2.6G
Swap:          2.0G          0B        2.0G`
          break
        case 'clear':
          terminalState.terminalOutput = []
          return
        case 'exit':
          disconnectFromServer()
          return
        default:
          response = `bash: ${command}: command not found`
      }
      
      if (response) {
        terminalState.terminalOutput.push(response)
        terminalState.terminalOutput.push(`${terminalState.currentServer?.username}@${terminalState.currentServer?.host}:~$ `)
      }
    }, 100)
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
