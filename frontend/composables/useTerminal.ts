import { ref, reactive } from 'vue'

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

// 虚拟服务器数据
const mockServers: ServerConnection[] = [
  {
    id: 1,
    name: '工控机',
    host: '192.168.6.5',
    port: 22,
    username: 'root',
    protocol: 'ssh',
    description: '本地工控设备',
    status: 'disconnected',
    lastConnected: '2024-01-15 14:30:00',
    icon: 'material-symbols:factory'
  },
  {
    id: 2,
    name: '西雅图',
    host: '107.174.222.14',
    port: 22,
    username: 'root',
    protocol: 'ssh',
    description: 'Racknerd',
    status: 'disconnected',
    lastConnected: '2024-01-14 09:15:00',
    icon: 'flagpack:us'
  },
  {
    id: 3,
    name: '首尔',
    host: '43.164.131.321',
    port: 22,
    username: 'root',
    protocol: 'ssh',
    description: '腾讯云 | 瑞驰',
    status: 'disconnected',
    lastConnected: '2024-01-13 16:45:00',
    icon: 'flagpack:kr'
  },
  {
    id: 4,
    name: '上海',
    host: '124.223.5.123',
    port: 22,
    username: 'root',
    protocol: 'ssh',
    description: '腾讯云',
    status: 'disconnected',
    lastConnected: '2024-01-12 11:20:00',
    icon: 'flagpack:cn'
  },
  {
    id: 5,
    name: '广州',
    host: '1.117.230.33',
    port: 22,
    username: 'root',
    protocol: 'ssh',
    description: '腾讯云',
    status: 'disconnected',
    lastConnected: '2024-01-11 08:15:00',
    icon: 'flagpack:cn'
  }
]

export const useTerminal = () => {
  // 响应式状态
  const servers = ref<ServerConnection[]>([...mockServers])
  const selectedServer = ref<ServerConnection | null>(null)
  const isConnecting = ref(false)
  const connectionError = ref('')
  
  const terminalState = reactive<TerminalState>({
    isConnected: false,
    currentServer: undefined,
    connectionHistory: [],
    terminalOutput: []
  })

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

  return {
    // 状态
    servers,
    selectedServer,
    isConnecting,
    connectionError,
    terminalState,
    
    // 方法
    getServers,
    getServerById,
    connectToServer,
    disconnectFromServer,
    sendCommand,
    clearTerminal,
    getStatusColor,
    getStatusIcon
  }
}
