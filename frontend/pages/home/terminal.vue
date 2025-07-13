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
  getGroupedServers,
  getAllGroups,
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
const showGrouped = ref(true) // 是否显示分组
const expandedGroups = ref<Set<string>>(new Set()) // 展开的分组

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
    if (terminalState.isConnected) {
      // 已连接到服务器，所有输入都直接发送给服务器
      console.log('发送输入到服务器，ASCII码:', data.charCodeAt(0), '内容:', data.replace('\r', '\\r').replace('\n', '\\n').replace('\t', '\\t'))
      sendCommand(data)
      // 不在本地显示任何内容，完全依赖服务器的响应
    } else {
      // 未连接时，显示提示信息
      if (data === '\r') {
        terminal.value?.write('\r\n请在左侧服务器列表中选择要连接的服务器。\r\n')
      }
    }
  })

  // 显示欢迎信息
  showWelcomeMessage()
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

// 显示欢迎信息
const showWelcomeMessage = () => {
  if (!terminal.value) return

  terminal.value.clear()
  terminal.value.writeln('╔══════════════════════════════════════════════════════════════╗')
  terminal.value.writeln('║                    服务器连接管理系统                        ║')
  terminal.value.writeln('╚══════════════════════════════════════════════════════════════╝')
  terminal.value.writeln('')
  terminal.value.writeln('欢迎使用服务器连接管理系统！')
  terminal.value.writeln('')
  terminal.value.writeln('请在左侧服务器列表中点击要连接的服务器。')
  terminal.value.writeln('连接成功后，您将获得完整的终端访问权限。')
  terminal.value.writeln('')
}

// 服务器选择处理已移除，现在通过Vue界面进行选择

// 处理连接后的命令（已移除，所有命令都直接发送给服务器）
// 保留函数定义以避免破坏现有调用，但不再使用
const handleConnectedCommand = (command: string) => {
  // 此函数已废弃，所有输入都直接发送给服务器处理
  console.warn('handleConnectedCommand 已废弃，不应该被调用')
}

// 通过索引连接服务器（Vue界面点击）
const connectToServerByIndex = async (index: number) => {
  if (index >= 0 && index < servers.value.length) {
    const server = servers.value[index]
    if (terminal.value) {
      terminal.value.clear()
      terminal.value.writeln(`正在连接到 ${server.name} (${server.host}:${server.port})...`)
      terminal.value.writeln('')
    }

    const success = await connectToServer(server.id)

    if (success) {
      isInServerSelection.value = false
      isConnectedToServer.value = true
      // 连接成功后，终端内容完全由服务器控制
    } else if (terminal.value) {
      terminal.value.writeln(`✗ 连接失败: ${connectionError.value}`)
      terminal.value.writeln('')
      terminal.value.writeln('请检查服务器配置或网络连接，然后重试。')
    }
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
      showWelcomeMessage()
    }, 1000)
  }
}

// 清空终端处理
const handleClearTerminal = () => {
  if (terminal.value) {
    if (isInServerSelection.value) {
      showWelcomeMessage()
    } else if (terminalState.isConnected) {
      // 发送clear命令给服务器，让服务器处理
      sendCommand('clear\r')
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
  // 当加载完成且在服务器选择模式时，刷新欢迎信息
  if (prevLoading && !loading && isInServerSelection.value && terminal.value) {
    setTimeout(() => {
      showWelcomeMessage()
    }, 100)
  }
}, { deep: true })

// 监听终端输出变化，将SSH输出显示到xterm
watch(() => terminalState.terminalOutput, (newOutput) => {
  if (terminal.value && newOutput.length > 0) {
    const lastOutput = newOutput[newOutput.length - 1]
    if (typeof lastOutput === 'object' && lastOutput.type === 'output') {
      const content = lastOutput.content
      // 直接显示服务器返回的所有内容，不做任何处理
      terminal.value.write(content)
    }
  }
}, { deep: true })

// 监听连接状态变化
watch(() => terminalState.isConnected, (connected) => {
  if (connected && terminal.value) {
    isInServerSelection.value = false
    // 不在前端清空或写入任何内容，完全依赖服务器的输出
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

// 分组展开/收起处理
const toggleGroup = (groupName: string) => {
  if (expandedGroups.value.has(groupName)) {
    expandedGroups.value.delete(groupName)
  } else {
    expandedGroups.value.add(groupName)
  }
}

// 检查分组是否展开
const isGroupExpanded = (groupName: string) => {
  return expandedGroups.value.has(groupName)
}

// 初始化时展开所有分组
onMounted(() => {
  // 延迟展开所有分组，等待服务器数据加载
  setTimeout(() => {
    const groups = getAllGroups()
    groups.forEach(group => expandedGroups.value.add(group))
  }, 1000)
})

// Tab补全处理已移除，所有输入都直接发送给服务器
// 保留变量定义以避免编译错误

</script>

<template>
  <NuxtLayout>
    <div class="terminal-dashboard">
      <!-- 左侧服务器列表面板 -->
      <div class="server-list-sidebar">
        <div class="panel-header">
          <h3 class="panel-title">
            <Icon icon="material-symbols:dns" class="title-icon" />
            服务器列表
          </h3>
          <button @click="showGrouped = !showGrouped" class="action-btn group-btn" :title="showGrouped ? '平铺显示' : '分组显示'">
            <Icon :icon="showGrouped ? 'material-symbols:view-list' : 'material-symbols:folder'" class="btn-icon" />
          </button>
          <button @click="loadServersFromDatabase" class="action-btn reload-btn" title="重新加载">
            <Icon icon="material-symbols:refresh" class="btn-icon" />
          </button>
        </div>

        <div class="server-list">
          <!-- 分组显示模式 -->
          <template v-if="showGrouped">
            <div v-for="(groupServers, groupName) in getGroupedServers()" :key="groupName" class="server-group">
              <div class="group-header" @click="toggleGroup(groupName)">
                <Icon
                  :icon="isGroupExpanded(groupName) ? 'material-symbols:expand-less' : 'material-symbols:expand-more'"
                  class="group-expand-icon"
                />
                <Icon icon="material-symbols:folder" class="group-icon" />
                <span class="group-name">{{ groupName }}</span>
                <span class="group-count">({{ groupServers.length }})</span>
              </div>
              <Transition name="group-expand">
                <div v-show="isGroupExpanded(groupName)" class="group-servers">
                  <div
                    v-for="(server, index) in groupServers"
                    :key="server.id"
                    class="server-item"
                    :class="{
                      'connected': server.status === 'connected',
                      'active': terminalState.currentServer?.id === server.id
                    }"
                    @click="connectToServerByIndex(servers.findIndex(s => s.id === server.id))"
                  >
                    <div class="server-item-header">
                      <Icon :icon="server.icon" class="server-icon" :class="getServerIconColor(server.icon)" />
                      <div class="server-status">
                        <Icon
                          :icon="getStatusIcon(server.status)"
                          :class="getStatusColor(server.status)"
                          class="status-icon"
                        />
                      </div>
                    </div>

                    <div class="server-item-info">
                      <h4 class="server-name">{{ server.name }}</h4>
                      <p class="server-address">{{ server.host }}:{{ server.port }}</p>
                      <p class="server-user">{{ server.username }}</p>
                    </div>
                  </div>
                </div>
              </Transition>
            </div>
          </template>

          <!-- 平铺显示模式 -->
          <template v-else>
            <div
              v-for="(server, index) in servers"
              :key="server.id"
              class="server-item"
              :class="{
                'connected': server.status === 'connected',
                'active': terminalState.currentServer?.id === server.id
              }"
              @click="connectToServerByIndex(index)"
            >
              <div class="server-item-header">
                <Icon :icon="server.icon" class="server-icon" :class="getServerIconColor(server.icon)" />
                <div class="server-status">
                  <Icon
                    :icon="getStatusIcon(server.status)"
                    :class="getStatusColor(server.status)"
                    class="status-icon"
                  />
                </div>
              </div>

              <div class="server-item-info">
                <h4 class="server-name">{{ server.name }}</h4>
                <p class="server-address">{{ server.host }}:{{ server.port }}</p>
                <p class="server-user">{{ server.username }}</p>
              </div>
            </div>
          </template>
        </div>

        <!-- 空状态 -->
        <div v-if="servers.length === 0 && !isLoading" class="empty-state">
          <Icon icon="material-symbols:dns-outline" class="empty-icon" />
          <p>暂无服务器配置</p>
          <button @click="loadServersFromDatabase" class="action-btn primary">
            <Icon icon="material-symbols:refresh" class="btn-icon" />
            重新加载
          </button>
        </div>

        <!-- 加载状态 -->
        <div v-if="isLoading" class="loading-state">
          <Icon icon="material-symbols:sync" class="loading-icon animate-spin" />
          <p>加载中...</p>
        </div>
      </div>

      <!-- 右侧终端面板 -->
      <div class="terminal-main">
        <!-- 连接错误提示 -->
        <div v-if="connectionError" class="error-message">
          <Icon icon="material-symbols:error" class="error-icon" />
          {{ connectionError }}
        </div>

        <div class="terminal-panel">
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
              <span v-else>请选择服务器进行连接</span>
            </div>
            <div class="terminal-controls">
              <button v-if="isConnectedToServer" @click="handleClearTerminal" class="control-btn" title="清空">
                <Icon icon="material-symbols:clear-all" />
              </button>
              <button v-if="isConnectedToServer" @click="handleDisconnect" class="control-btn" title="断开">
                <Icon icon="material-symbols:close" />
              </button>
            </div>
          </div>

          <div class="terminal-container">
            <div ref="terminalContainer" class="xterm-container"></div>
          </div>
        </div>
      </div>
    </div>
  </NuxtLayout>
</template>
