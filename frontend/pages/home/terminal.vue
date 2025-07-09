<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
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
  terminalState,
  connectToServer,
  disconnectFromServer,
  sendCommand,
  clearTerminal,
  getStatusColor,
  getStatusIcon
} = useTerminal()

// 终端相关状态
const terminalContainer = ref<HTMLElement>()
const terminal = ref<Terminal>()
const fitAddon = ref<FitAddon>()
const showTerminal = ref(true) // 默认显示终端
const currentCommand = ref('')
const isInServerSelection = ref(true) // 是否在服务器选择模式
const isConnectedToServer = ref(false) // 是否已连接到服务器

// 初始化xterm.js终端
const initTerminal = async () => {
  if (!terminalContainer.value) return

  // 创建终端实例
  terminal.value = new Terminal({
    cursorBlink: true,
    fontSize: 14,
    fontFamily: 'Consolas, "Courier New", monospace',
    theme: {
      background: '#1a1a1a',
      foreground: '#ffffff',
      cursor: '#ffffff',
      selection: '#3e3e3e'
    },
    cols: 80,
    rows: 24
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
    } else if (data >= ' ') {
      // 可打印字符
      currentCommand.value += data
      terminal.value?.write(data)
    }
  })

  // 显示服务器选择菜单
  showServerSelectionMenu()
}

// 显示服务器选择菜单
const showServerSelectionMenu = () => {
  if (!terminal.value) return
  
  terminal.value.clear()
  terminal.value.writeln('\x1b[1;36m╔══════════════════════════════════════════════════════════════╗\x1b[0m')
  terminal.value.writeln('\x1b[1;36m║                    服务器连接管理系统                        ║\x1b[0m')
  terminal.value.writeln('\x1b[1;36m╚══════════════════════════════════════════════════════════════╝\x1b[0m')
  terminal.value.writeln('')
  terminal.value.writeln('\x1b[1;33m可用服务器列表:\x1b[0m')
  terminal.value.writeln('')
  
  servers.value.forEach((server, index) => {
    const statusColor = server.status === 'connected' ? '\x1b[1;32m' : '\x1b[1;37m'
    const statusText = server.status === 'connected' ? '[已连接]' : '[未连接]'
    terminal.value?.writeln(`\x1b[1;32m${index + 1}.\x1b[0m ${statusColor}${server.name}\x1b[0m ${statusText}`)
    terminal.value?.writeln(`   \x1b[90m地址: ${server.host}:${server.port} | 用户: ${server.username} | 协议: ${server.protocol.toUpperCase()}\x1b[0m`)
    terminal.value?.writeln(`   \x1b[90m描述: ${server.description}\x1b[0m`)
    if (server.lastConnected) {
      terminal.value?.writeln(`   \x1b[90m最后连接: ${server.lastConnected}\x1b[0m`)
    }
    terminal.value?.writeln('')
  })
  
  terminal.value.writeln('\x1b[1;33m可用命令:\x1b[0m')
  terminal.value.writeln('  \x1b[1;32m1-4\x1b[0m     - 连接到对应编号的服务器')
  terminal.value.writeln('  \x1b[1;32mlist\x1b[0m    - 重新显示服务器列表')
  terminal.value.writeln('  \x1b[1;32mstatus\x1b[0m  - 显示连接状态')
  terminal.value.writeln('  \x1b[1;32mclear\x1b[0m   - 清空屏幕')
  terminal.value.writeln('  \x1b[1;32mexit\x1b[0m    - 退出系统')
  terminal.value.writeln('')
  terminal.value.write('\x1b[1;36m请选择服务器 (1-4) 或输入命令: \x1b[0m')
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
      terminal.value.writeln(`\x1b[1;32m✓ 成功连接到 ${server.name}\x1b[0m`)
      terminal.value.writeln(`\x1b[90m地址: ${server.host}:${server.port}\x1b[0m`)
      terminal.value.writeln(`\x1b[90m用户: ${server.username}\x1b[0m`)
      terminal.value.writeln(`\x1b[90m协议: ${server.protocol.toUpperCase()}\x1b[0m`)
      terminal.value.writeln('')
      terminal.value.writeln('欢迎使用服务器终端！输入 "help" 查看可用命令，输入 "disconnect" 断开连接。')
      terminal.value.writeln('')
      terminal.value.write(`${server.username}@${server.host}:~$ `)
    } else {
      terminal.value.writeln(`\x1b[1;31m✗ 连接失败: ${connectionError.value}\x1b[0m`)
      terminal.value.write('\x1b[1;36m请选择服务器 (1-4) 或输入命令: \x1b[0m')
    }
    return
  }
  
  // 处理命令
  switch (command) {
    case 'list':
      showServerSelectionMenu()
      break
    case 'status':
      terminal.value.writeln('\x1b[1;33m连接状态:\x1b[0m')
      servers.value.forEach((server, index) => {
        const statusColor = server.status === 'connected' ? '\x1b[1;32m' : '\x1b[1;31m'
        const statusText = server.status === 'connected' ? '已连接' : '未连接'
        terminal.value?.writeln(`  ${index + 1}. ${server.name}: ${statusColor}${statusText}\x1b[0m`)
      })
      terminal.value.writeln('')
      terminal.value.write('\x1b[1;36m请选择服务器 (1-4) 或输入命令: \x1b[0m')
      break
    case 'clear':
      showServerSelectionMenu()
      break
    case 'exit':
      terminal.value.writeln('\x1b[1;33m感谢使用服务器连接管理系统！\x1b[0m')
      terminal.value.writeln('')
      setTimeout(() => {
        showServerSelectionMenu()
      }, 1000)
      break
    default:
      terminal.value.writeln(`\x1b[1;31m未知命令: ${input}\x1b[0m`)
      terminal.value.writeln('输入 1-4 选择服务器，或输入 "list" 查看服务器列表')
      terminal.value.write('\x1b[1;36m请选择服务器 (1-4) 或输入命令: \x1b[0m')
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
    terminal.value.writeln('\x1b[1;33m可用命令:\x1b[0m')
    terminal.value.writeln('  \x1b[1;32mls\x1b[0m        - 列出文件和目录')
    terminal.value.writeln('  \x1b[1;32mpwd\x1b[0m       - 显示当前目录')
    terminal.value.writeln('  \x1b[1;32mwhoami\x1b[0m    - 显示当前用户')
    terminal.value.writeln('  \x1b[1;32mdate\x1b[0m      - 显示当前日期时间')
    terminal.value.writeln('  \x1b[1;32muname -a\x1b[0m  - 显示系统信息')
    terminal.value.writeln('  \x1b[1;32mdf -h\x1b[0m     - 显示磁盘使用情况')
    terminal.value.writeln('  \x1b[1;32mfree -h\x1b[0m   - 显示内存使用情况')
    terminal.value.writeln('  \x1b[1;32mclear\x1b[0m     - 清空屏幕')
    terminal.value.writeln('  \x1b[1;32mdisconnect\x1b[0m - 断开连接')
    terminal.value.writeln('')
    terminal.value.write(`${terminalState.currentServer.username}@${terminalState.currentServer.host}:~$ `)
    return
  }
  
  if (cmd === 'clear') {
    terminal.value.clear()
    terminal.value.write(`${terminalState.currentServer.username}@${terminalState.currentServer.host}:~$ `)
    return
  }
  
  // 模拟命令响应
  setTimeout(() => {
    let response = ''
    
    switch (cmd) {
      case 'ls':
        response = '\x1b[1;34mDocuments\x1b[0m  \x1b[1;34mDownloads\x1b[0m  \x1b[1;34mPictures\x1b[0m  \x1b[1;34mVideos\x1b[0m  \x1b[1;34mworkspace\x1b[0m'
        break
      case 'pwd':
        response = `/home/${terminalState.currentServer?.username}`
        break
      case 'whoami':
        response = terminalState.currentServer?.username || 'unknown'
        break
      case 'date':
        response = new Date().toLocaleString('zh-CN')
        break
      case 'uname -a':
        response = 'Linux server 5.4.0-74-generic #83-Ubuntu SMP Sat May 8 02:35:39 UTC 2021 x86_64 x86_64 x86_64 GNU/Linux'
        break
      case 'df -h':
        response = `\x1b[1;37mFilesystem      Size  Used Avail Use% Mounted on\x1b[0m
/dev/sda1        20G  8.5G   11G  45% /
tmpfs           2.0G     0  2.0G   0% /dev/shm
/dev/sda2       100G   45G   50G  47% /home`
        break
      case 'free -h':
        response = `\x1b[1;37m              total        used        free      shared  buff/cache   available\x1b[0m
Mem:           4.0G        1.2G        1.8G         50M        1.0G        2.6G
Swap:          2.0G          0B        2.0G`
        break
      default:
        response = `\x1b[1;31mbash: ${command}: command not found\x1b[0m`
    }
    
    if (response) {
      terminal.value?.writeln(response)
      terminal.value?.write(`${terminalState.currentServer?.username}@${terminalState.currentServer?.host}:~$ `)
    }
  }, 100)
}

// 断开连接处理
const handleDisconnect = () => {
  disconnectFromServer()
  isInServerSelection.value = true
  isConnectedToServer.value = false
  currentCommand.value = ''
  
  if (terminal.value) {
    terminal.value.writeln('')
    terminal.value.writeln('\x1b[1;33m已断开服务器连接\x1b[0m')
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
  if (fitAddon.value) {
    fitAddon.value.fit()
  }
}

// 组件挂载
onMounted(async () => {
  await nextTick()
  await initTerminal()
  
  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
})

// 组件卸载
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  
  // 清理终端
  if (terminal.value) {
    terminal.value.dispose()
  }
})
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
