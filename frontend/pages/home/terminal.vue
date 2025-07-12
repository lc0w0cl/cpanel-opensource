<script setup lang="ts">
import { ref, onMounted, onUnmounted, onActivated, nextTick, watch } from 'vue'
import { Icon } from '@iconify/vue'
import { Terminal } from 'xterm'
import { FitAddon } from 'xterm-addon-fit'
import { useTerminal, type ServerConnection } from '~/composables/useTerminal'
import 'xterm/css/xterm.css'
import './terminal.css'

// 页面元数据
definePageMeta({
  layout: 'dashboard',
  middleware: 'auth'
})

// 使用终端composable
const {
  servers,
  selectedServer,
  isConnecting,
  connectionError,
  isLoading,
  terminalState,
  wsStatus,
  connectToServer,
  disconnectFromServer,
  sendCommand,
  clearTerminal,
  getStatusColor,
  getStatusIcon,
  loadServersFromDatabase
} = useTerminal()

// 终端相关状态
const terminalContainer = ref<HTMLElement>()
const terminal = ref<Terminal>()
const fitAddon = ref<FitAddon>()
const showTerminal = ref(true) // 默认显示终端
const currentCommand = ref('')
const isInServerSelection = ref(true) // 是否在服务器选择模式
const isConnectedToServer = ref(false) // 是否已连接到服务器
const isTabCompleting = ref(false) // 是否正在进行Tab补全
const tabCompletionInput = ref('') // Tab补全时的原始输入

// 初始化xterm.js终端
const initTerminal = async () => {
  if (!terminalContainer.value) return

  // 清理之前的实例（如果存在）
  if (terminal.value) {
    try {
      terminal.value.dispose()
    } catch (error) {
      console.warn('Previous terminal cleanup error:', error)
    }
  }

  // 重置状态
  terminal.value = undefined
  fitAddon.value = undefined
  currentCommand.value = ''
  isInServerSelection.value = true
  isConnectedToServer.value = false

  // 创建终端实例
  terminal.value = new Terminal({
    cursorBlink: true,
    fontSize: 16,
    fontFamily: 'Consolas, "Courier New", monospace',
    theme: {
      background: '#1a1a1a',
      foreground: '#ffffff',
      cursor: '#ffffff',
      selection: '#3e3e3e',
      // 添加标准ANSI颜色支持
      black: '#000000',
      red: '#ff0000',
      green: '#00ff00',
      yellow: '#ffff00',
      blue: '#0000ff',
      magenta: '#ff00ff',
      cyan: '#00ffff',
      white: '#ffffff',
      brightBlack: '#808080',
      brightRed: '#ff8080',
      brightGreen: '#80ff80',
      brightYellow: '#ffff80',
      brightBlue: '#8080ff',
      brightMagenta: '#ff80ff',
      brightCyan: '#80ffff',
      brightWhite: '#ffffff'
    },
    cols: 80,
    rows: 24,
    // 启用ANSI颜色支持
    allowTransparency: false,
    convertEol: true,
    // 支持256色
    allowProposedApi: true
  })

  // 创建适配插件
  fitAddon.value = new FitAddon()
  terminal.value.loadAddon(fitAddon.value)

  // 打开终端
  terminal.value.open(terminalContainer.value)

  // 适配大小
  fitAddon.value.fit()

  // 监听输入
  terminal.value.onData((data) => {
    if (data === '\r') {
      // 回车键 - 执行命令
      if (currentCommand.value.trim()) {
        if (isInServerSelection.value) {
          handleServerSelection(currentCommand.value.trim())
        } else {
          handleConnectedCommand(currentCommand.value.trim())
        }
        currentCommand.value = ''
      }
      terminal.value?.write('\r\n')
    } else if (data === '\u007F') {
      // 退格键
      if (currentCommand.value.length > 0) {
        currentCommand.value = currentCommand.value.slice(0, -1)
        terminal.value?.write('\b \b')
      }
    } else if (data === '\u0003') {
      // Ctrl+C - 中断信号
      if (terminalState.isConnected && !isInServerSelection.value) {
        console.log('发送Ctrl+C中断信号')
        sendCommand('\u0003') // 发送ETX字符
        currentCommand.value = '' // 清空当前命令
        terminal.value?.write('^C\r\n') // 显示^C并换行
      }
    } else if (data === '\u0004') {
      // Ctrl+D - EOF信号
      if (terminalState.isConnected && !isInServerSelection.value) {
        console.log('发送Ctrl+D EOF信号')
        sendCommand('\u0004')
        currentCommand.value = ''
        terminal.value?.write('^D\r\n')
      }
    } else if (data === '\u001A') {
      // Ctrl+Z - 挂起信号
      if (terminalState.isConnected && !isInServerSelection.value) {
        console.log('发送Ctrl+Z挂起信号')
        sendCommand('\u001A')
        currentCommand.value = ''
        terminal.value?.write('^Z\r\n')
      }
    } else if (data === '\t') {
      // Tab键 - 自动补全
      if (terminalState.isConnected && !isInServerSelection.value) {
        // 发送Tab补全请求，包含当前已输入的内容
        console.log('发送Tab补全请求，当前命令:', currentCommand.value)
        handleTabCompletion(currentCommand.value)
        // 不要在本地终端显示Tab字符，等待服务器响应
      }
    } else if (data >= ' ') {
      // 可打印字符
      currentCommand.value += data
      terminal.value?.write(data)
    } else {
      // 其他控制字符直接发送到SSH会话
      if (terminalState.isConnected && !isInServerSelection.value) {
        console.log('发送控制字符，ASCII码:', data.charCodeAt(0))
        sendCommand(data)
      }
    }
  })

  // 显示服务器选择菜单
  showServerSelectionMenu()
}

// Iconify图标到终端字符的映射
const iconifyToTerminal = (iconName: string): string => {
  const iconMap: Record<string, string> = {
    'flagpack:us': '🇺🇸', // 美国国旗
    'flagpack:kr': '🇰🇷', // 韩国国旗
    'flagpack:cn': '🇨🇳', // 中国国旗
    'material-symbols:factory': '🏭', // 工厂图标
    'flagpack:jp': '🇯🇵', // 日本国旗
    'flagpack:de': '🇩🇪', // 德国国旗
    'flagpack:gb': '🇬🇧', // 英国国旗
    'flagpack:fr': '🇫🇷', // 法国国旗
    'flagpack:sg': '🇸🇬', // 新加坡国旗
    'flagpack:hk': '🇭🇰', // 香港旗帜
  }

  return iconMap[iconName] || '🌐' // 默认全球图标
}

// 获取服务器图标
const getServerIcon = (server: ServerConnection) => {
  return iconifyToTerminal(server.icon)
}

// 获取服务器图标的CSS颜色类
const getServerIconColor = (iconName: string) => {
  const colorMap: Record<string, string> = {
    'flagpack:us': 'text-blue-400',
    'flagpack:kr': 'text-red-400',
    'flagpack:cn': 'text-red-400',
    'material-symbols:factory': 'text-gray-300',
    'flagpack:jp': 'text-red-400',
    'flagpack:de': 'text-yellow-400',
    'flagpack:gb': 'text-blue-400',
    'flagpack:fr': 'text-blue-400',
    'flagpack:sg': 'text-red-400',
    'flagpack:hk': 'text-red-400',
  }

  return colorMap[iconName] || 'text-gray-300'
}

// 显示服务器选择菜单
const showServerSelectionMenu = () => {
  if (!terminal.value) return

  terminal.value.clear()
  terminal.value.writeln('╔══════════════════════════════════════════════════════════════╗')
  terminal.value.writeln('║                    服务器连接管理系统                        ║')
  terminal.value.writeln('╚══════════════════════════════════════════════════════════════╝')
  terminal.value.writeln('')

  // 检查是否正在加载
  if (isLoading.value) {
    terminal.value.writeln('正在加载服务器配置...')
    terminal.value.writeln('')
    terminal.value.write('请稍候...')
    return
  }

  // 检查是否有连接错误
  if (connectionError.value) {
    terminal.value.writeln('❌ 加载服务器配置失败:')
    terminal.value.writeln(`   ${connectionError.value}`)
    terminal.value.writeln('')
    terminal.value.writeln('可用命令:')
    terminal.value.writeln('  reload  - 重新加载服务器配置')
    terminal.value.writeln('  clear   - 清空屏幕')
    terminal.value.writeln('  exit    - 退出系统')
    terminal.value.writeln('')
    terminal.value.write('请输入命令: ')
    return
  }

  // 检查是否有服务器
  if (servers.value.length === 0) {
    terminal.value.writeln('⚠️  未找到可用的服务器配置')
    terminal.value.writeln('')
    terminal.value.writeln('请先在设置页面添加服务器配置，然后重新加载。')
    terminal.value.writeln('')
    terminal.value.writeln('可用命令:')
    terminal.value.writeln('  reload  - 重新加载服务器配置')
    terminal.value.writeln('  clear   - 清空屏幕')
    terminal.value.writeln('  exit    - 退出系统')
    terminal.value.writeln('')
    terminal.value.write('请输入命令: ')
    return
  }

  terminal.value.writeln('可用服务器列表:')
  terminal.value.writeln('')

  servers.value.forEach((server, index) => {
    // 获取服务器图标
    let serverIcon = getServerIcon(server)

    // 如果已连接，添加连接指示器
    if (server.status === 'connected') {
      serverIcon += '●'
    }

    // 构建服务器信息行
    let serverLine = `[${index + 1}] ${serverIcon} ${server.name}`

    // 添加提供商信息（如果有）
    if (server.description) {
      serverLine += ` | ${server.description}`
    }

    // 添加IP地址和端口
    serverLine += ` | ${server.host}:${server.port}`

    terminal.value?.writeln(serverLine)
  })

  terminal.value.writeln('')
  terminal.value.writeln('可用命令:')
  terminal.value.writeln(`  1-${servers.value.length}     - 连接到对应编号的服务器`)
  terminal.value.writeln('  list    - 重新显示服务器列表')
  terminal.value.writeln('  reload  - 重新加载服务器配置')
  terminal.value.writeln('  status  - 显示连接状态')
  terminal.value.writeln('  clear   - 清空屏幕')
  terminal.value.writeln('  exit    - 退出系统')
  terminal.value.writeln('')
  terminal.value.write(`请选择服务器 (1-${servers.value.length}) 或输入命令: `)
}

// 处理服务器选择
const handleServerSelection = async (input: string) => {
  if (!terminal.value) return
  
  const command = input.toLowerCase().trim()
  
  // 处理数字选择
  const serverIndex = parseInt(command) - 1
  if (!isNaN(serverIndex) && serverIndex >= 0 && serverIndex < servers.value.length) {
    const server = servers.value[serverIndex]
    terminal.value.writeln(`正在连接到 ${server.name}...`)
    
    const success = await connectToServer(server.id)
    
    if (success) {
      isInServerSelection.value = false
      isConnectedToServer.value = true
      
      // 显示连接成功信息
      terminal.value.clear()
      terminal.value.writeln(`✓ 成功连接到 ${server.name}`)
      terminal.value.writeln(`地址: ${server.host}:${server.port}`)
      terminal.value.writeln(`用户: ${server.username}`)
      terminal.value.writeln(`协议: ${server.protocol.toUpperCase()}`)
      terminal.value.writeln('')
      terminal.value.writeln('欢迎使用服务器终端！输入 "help" 查看可用命令，输入 "disconnect" 断开连接。')
      terminal.value.writeln('')
      terminal.value.write(`${server.username}@${server.host}:~$ `)
    } else {
      terminal.value.writeln(`✗ 连接失败: ${connectionError.value}`)
      terminal.value.write('请选择服务器 (1-4) 或输入命令: ')
    }
    return
  }
  
  // 处理命令
  switch (command) {
    case 'list':
      showServerSelectionMenu()
      break
    case 'reload':
      terminal.value.writeln('正在重新加载服务器配置...')
      await loadServersFromDatabase()
      setTimeout(() => {
        showServerSelectionMenu()
      }, 500)
      break
    case 'status':
      if (servers.value.length === 0) {
        terminal.value.writeln('暂无服务器配置')
      } else {
        terminal.value.writeln('连接状态:')
        servers.value.forEach((server, index) => {
          const serverIcon = getServerIcon(server)
          const statusIcon = server.status === 'connected' ? '●' : '○'
          const statusText = server.status === 'connected' ? '已连接' : '未连接'
          terminal.value?.writeln(`  [${index + 1}] ${serverIcon}${statusIcon} ${server.name}: ${statusText}`)
        })
      }
      terminal.value.writeln('')
      terminal.value.write(`请选择服务器 (1-${servers.value.length || 0}) 或输入命令: `)
      break
    case 'clear':
      showServerSelectionMenu()
      break
    case 'exit':
      terminal.value.writeln('感谢使用服务器连接管理系统！')
      terminal.value.writeln('')
      setTimeout(() => {
        showServerSelectionMenu()
      }, 1000)
      break
    default:
      terminal.value.writeln(`未知命令: ${input}`)
      if (servers.value.length > 0) {
        terminal.value.writeln(`输入 1-${servers.value.length} 选择服务器，或输入 "list" 查看服务器列表`)
        terminal.value.write(`请选择服务器 (1-${servers.value.length}) 或输入命令: `)
      } else {
        terminal.value.writeln('输入 "reload" 重新加载服务器配置')
        terminal.value.write('请输入命令: ')
      }
  }
}

// 处理连接后的命令
const handleConnectedCommand = (command: string) => {
  if (!terminal.value || !terminalState.currentServer) return
  
  const cmd = command.toLowerCase().trim()
  
  if (cmd === 'disconnect') {
    handleDisconnect()
    return
  }
  
  if (cmd === 'help') {
    terminal.value.writeln('可用命令:')
    terminal.value.writeln('  ls        - 列出文件和目录')
    terminal.value.writeln('  pwd       - 显示当前目录')
    terminal.value.writeln('  whoami    - 显示当前用户')
    terminal.value.writeln('  date      - 显示当前日期时间')
    terminal.value.writeln('  uname -a  - 显示系统信息')
    terminal.value.writeln('  df -h     - 显示磁盘使用情况')
    terminal.value.writeln('  free -h   - 显示内存使用情况')
    terminal.value.writeln('  clear     - 清空屏幕')
    terminal.value.writeln('  disconnect - 断开连接')
    terminal.value.writeln('')
    terminal.value.write(`${terminalState.currentServer.username}@${terminalState.currentServer.host}:~$ `)
    return
  }
  
  if (cmd === 'clear') {
    terminal.value.clear()
    terminal.value.write(`${terminalState.currentServer.username}@${terminalState.currentServer.host}:~$ `)
    return
  }
  
  // 发送命令到SSH服务器
  const success = sendCommand(command)
  if (!success) {
    terminal.value.writeln('发送命令失败：连接已断开')
    terminal.value.write(`${terminalState.currentServer.username}@${terminalState.currentServer.host}:~$ `)
  }
}

// 断开连接处理
const handleDisconnect = () => {
  disconnectFromServer()
  isInServerSelection.value = true
  isConnectedToServer.value = false
  currentCommand.value = ''
  
  if (terminal.value) {
    terminal.value.writeln('')
    terminal.value.writeln('已断开服务器连接')
    terminal.value.writeln('')
    setTimeout(() => {
      showServerSelectionMenu()
    }, 1000)
  }
}

// 清空终端处理
const handleClearTerminal = () => {
  if (terminal.value) {
    if (isInServerSelection.value) {
      showServerSelectionMenu()
    } else if (terminalState.currentServer) {
      terminal.value.clear()
      terminal.value.write(`${terminalState.currentServer.username}@${terminalState.currentServer.host}:~$ `)
    }
  }
}

// 窗口大小调整处理
const handleResize = () => {
  if (fitAddon.value && terminal.value) {
    try {
      fitAddon.value.fit()
    } catch (error) {
      console.warn('Terminal resize error:', error)
    }
  }
}

// 监听服务器加载状态变化
watch([isLoading, servers], ([loading, serverList], [prevLoading, prevServerList]) => {
  // 当加载完成且在服务器选择模式时，刷新菜单
  if (prevLoading && !loading && isInServerSelection.value && terminal.value) {
    setTimeout(() => {
      showServerSelectionMenu()
    }, 100)
  }
}, { deep: true })

// 监听终端输出变化，将SSH输出显示到xterm
watch(() => terminalState.terminalOutput, (newOutput) => {
  if (terminal.value && newOutput.length > 0) {
    const lastOutput = newOutput[newOutput.length - 1]
    if (typeof lastOutput === 'object' && lastOutput.type === 'output') {
      const content = lastOutput.content

      // 检查是否是Tab补全结果
      if (isTabCompleting.value && tabCompletionInput.value) {
        handleTabCompletionOutput(content)
      } else {
        // 正常输出
        terminal.value.write(content)
      }
    }
  }
}, { deep: true })

// 监听连接状态变化
watch(() => terminalState.isConnected, (connected) => {
  if (connected && terminal.value) {
    isInServerSelection.value = false
    terminal.value.clear()
    terminal.value.writeln('SSH连接已建立')
    terminal.value.writeln('')
  }
})

// 组件挂载
onMounted(async () => {
  // 确保DOM完全渲染
  await nextTick()

  // 延迟一点时间确保容器元素完全可用
  setTimeout(async () => {
    try {
      await initTerminal()
      // 监听窗口大小变化
      window.addEventListener('resize', handleResize)
    } catch (error) {
      console.error('Terminal initialization error:', error)
    }
  }, 100)
})

// 页面激活时重新初始化（从其他页面返回时）
onActivated(async () => {
  // 如果终端容器存在但终端实例不存在，重新初始化
  if (terminalContainer.value && !terminal.value) {
    await nextTick()
    setTimeout(async () => {
      try {
        await initTerminal()
      } catch (error) {
        console.error('Terminal reinitialization error:', error)
      }
    }, 100)
  } else if (terminal.value && fitAddon.value) {
    // 如果终端存在，重新适配大小
    setTimeout(() => {
      try {
        fitAddon.value?.fit()
      } catch (error) {
        console.warn('Terminal resize error:', error)
      }
    }, 100)
  }
})

// 组件卸载
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)

  // 清理终端
  if (terminal.value) {
    try {
      // 先清理插件
      if (fitAddon.value) {
        terminal.value.dispose()
        fitAddon.value = undefined
      } else {
        terminal.value.dispose()
      }
    } catch (error) {
      console.warn('Terminal cleanup error:', error)
    } finally {
      terminal.value = undefined
    }
  }
})

// 处理Tab补全
const handleTabCompletion = (currentInput: string) => {
  if (!terminalState.isConnected) {
    return
  }

  console.log('发送Tab补全，当前输入:', currentInput)

  // 标记正在进行Tab补全
  isTabCompleting.value = true
  tabCompletionInput.value = currentInput

  // 发送Tab补全请求
  const commandWithTab = currentInput + '\t'
  const success = sendCommand(commandWithTab)

  if (!success) {
    console.error('发送Tab补全请求失败')
    isTabCompleting.value = false
  }

  // 设置超时
  setTimeout(() => {
    if (isTabCompleting.value) {
      isTabCompleting.value = false
      console.log('Tab补全超时')
    }
  }, 2000)
}

// 处理Tab补全输出
const handleTabCompletionOutput = (output: string) => {
  console.log('处理Tab补全输出:', output)

  try {
    // 清除Tab补全状态
    isTabCompleting.value = false

    const originalInput = tabCompletionInput.value
    tabCompletionInput.value = ''

    // 分析补全结果
    if (output.includes(originalInput)) {
      // 查找补全后的完整命令
      const lines = output.split(/\r?\n/)
      let completedCommand = ''

      for (const line of lines) {
        const trimmedLine = line.trim()
        if (trimmedLine.startsWith(originalInput) && trimmedLine !== originalInput) {
          completedCommand = trimmedLine
          break
        }
      }

      if (completedCommand) {
        // 找到了补全结果，更新当前命令
        console.log('找到补全结果:', completedCommand)

        // 清除当前显示的输入
        for (let i = 0; i < currentCommand.value.length; i++) {
          terminal.value?.write('\b \b')
        }

        // 写入补全后的命令
        currentCommand.value = completedCommand
        terminal.value?.write(completedCommand)

        return
      }
    }

    // 如果没有找到单一补全结果，可能是选项列表，直接显示
    if (output.trim()) {
      terminal.value?.write('\r\n' + output)
      // 重新显示提示符和当前输入
      if (terminalState.currentServer) {
        terminal.value?.write(`\r\n${terminalState.currentServer.username}@${terminalState.currentServer.host}:~$ `)
        terminal.value?.write(currentCommand.value)
      }
    }

  } catch (error) {
    console.error('处理Tab补全输出失败:', error)
    // 出错时直接显示原始输出
    terminal.value?.write(output)
  }
}
</script>

<template>
  <NuxtLayout>
    <div class="terminal-dashboard">
      <!-- 页面标题 -->

        <div class="header-actions">
          <button
            v-if="isConnectedToServer"
            @click="handleClearTerminal"
            class="action-btn clear-btn"
            title="清空终端"
          >
            <Icon icon="material-symbols:clear-all" class="btn-icon" />
            清空
          </button>
          <button
            v-if="isConnectedToServer"
            @click="handleDisconnect"
            class="action-btn disconnect-btn"
            title="断开连接"
          >
            <Icon icon="material-symbols:power-off" class="btn-icon" />
            断开
          </button>
        </div>

      <!-- 连接错误提示 -->
      <div v-if="connectionError" class="error-message">
        <Icon icon="material-symbols:error" class="error-icon" />
        {{ connectionError }}
      </div>

      <!-- 终端面板 -->
      <div class="terminal-panel-fullscreen">
        <div class="terminal-header">
          <div class="terminal-title">
            <Icon icon="material-symbols:terminal" class="terminal-icon" />
            <Icon
              v-if="terminalState.currentServer"
              :icon="terminalState.currentServer.icon"
              class="server-icon"
            />
            <span v-if="terminalState.currentServer">
              {{ terminalState.currentServer.name }} - {{ terminalState.currentServer.host }}
            </span>
            <span v-else>服务器连接管理系统</span>
          </div>
          <div class="terminal-controls" v-if="isConnectedToServer">
            <button @click="handleClearTerminal" class="control-btn" title="清空">
              <Icon icon="material-symbols:clear-all" />
            </button>
            <button @click="handleDisconnect" class="control-btn" title="断开">
              <Icon icon="material-symbols:close" />
            </button>
          </div>
        </div>

        <div class="terminal-container">
          <div ref="terminalContainer" class="xterm-container"></div>
        </div>
      </div>
    </div>
  </NuxtLayout>
</template>
