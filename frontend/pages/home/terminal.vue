<script setup lang="ts">
import { ref, onMounted, onUnmounted, onActivated, onDeactivated, nextTick, watch } from 'vue'
import { Icon } from '@iconify/vue'
import { Terminal } from '@xterm/xterm'
import { FitAddon } from '@xterm/addon-fit'
import { useTerminal, type ServerConnection } from '~/composables/useTerminal'
import '@xterm/xterm/css/xterm.css'
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
  getGroupedServers,
  getAllCategories,
  getCategoryName,
  loadServersFromDatabase,
  addPageReference,
  removePageReference
} = useTerminal()

// 终端相关状态
const terminalContainers = ref<Map<string, HTMLElement>>(new Map())
const terminals = ref<Map<string, Terminal>>(new Map())
const fitAddons = ref<Map<string, FitAddon>>(new Map())
const showTerminal = ref(true) // 默认显示终端
const currentCommand = ref('')
const isInServerSelection = ref(true) // 是否在服务器选择模式
const showGrouped = ref(true) // 是否显示分组
const expandedGroups = ref<Set<number>>(new Set()) // 展开的分组（使用分类ID）

// 初始化指定会话的xterm.js终端
const initTerminal = async (sessionId: string, containerElement: HTMLElement, restoreContent = false) => {
  if (!containerElement) return

  // 清理之前的实例（如果存在）
  const existingTerminal = terminals.value.get(sessionId)
  if (existingTerminal) {
    try {
      existingTerminal.dispose()
    } catch (error) {
      console.warn('Previous terminal cleanup error:', error)
    }
  }

  // 清理旧的实例
  terminals.value.delete(sessionId)
  fitAddons.value.delete(sessionId)

  // 创建终端实例
  const terminal = new Terminal({
    rendererType: "canvas", //渲染类型
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
    rows: 24,
    // 启用ANSI颜色支持
    allowTransparency: false,
    convertEol: true,
    // 支持256色
    allowProposedApi: true
  })

  // 创建适配插件
  const fitAddon = new FitAddon()
  terminal.loadAddon(fitAddon)

  // 打开终端
  terminal.open(containerElement)

  // 适配大小
  fitAddon.fit()

  // 存储实例
  terminals.value.set(sessionId, terminal)
  fitAddons.value.set(sessionId, fitAddon)

  // 监听终端大小变化，同步到后端PTY
  terminal.onResize((size) => {
    const activeSession = getActiveSession()
    if (activeSession?.isConnected && activeSession.id === sessionId) {
      console.log(`终端大小变化: ${size.cols}x${size.rows}, 会话: ${sessionId}`)
      sendResizeMessage(size.cols, size.rows, sessionId)
    }
  })

  // 初始化时发送当前终端大小到后端
  nextTick(() => {
    const activeSession = getActiveSession()
    if (activeSession?.isConnected && activeSession.id === sessionId) {
      const cols = terminal.cols
      const rows = terminal.rows
      console.log(`初始化终端大小: ${cols}x${rows}, 会话: ${sessionId}`)
      sendResizeMessage(cols, rows, sessionId)
    }
  })

  // 监听输入
  terminal.onData((data) => {
    const activeSession = getActiveSession()
    if (activeSession?.isConnected && activeSession.id === sessionId) {
      // 已连接到服务器，所有输入都直接发送给服务器
      console.log('发送输入到服务器，会话:', sessionId, 'ASCII码:', data.charCodeAt(0), '内容:', data.replace('\r', '\\r').replace('\n', '\\n').replace('\t', '\\t'))
      sendCommand(data, sessionId)
      // 不在本地显示任何内容，完全依赖服务器的响应
    } else {
      // 未连接时，显示提示信息
      if (data === '\r') {
        terminal.write('\r\n请在左侧服务器列表中选择要连接的服务器。\r\n')
      }
    }
  })

  // 根据情况显示内容
  if (restoreContent) {
    // 恢复会话时，重新显示所有历史输出
    restoreTerminalContent(sessionId, terminal)
  } else {
    // 新会话时，显示欢迎信息
    showWelcomeMessage(sessionId)
  }

  // 如果这是当前活动会话，自动聚焦终端
  if (sessionId === terminalState.activeSessionId) {
    nextTick(() => {
      terminal.focus()
    })
  }
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
const showWelcomeMessage = (sessionId?: string) => {
  const terminal = sessionId ? terminals.value.get(sessionId) : null
  if (!terminal) return

  terminal.clear()
  terminal.writeln('╔══════════════════════════════════════════════════════════════╗')
  terminal.writeln('║                    服务器连接管理系统                        ║')
  terminal.writeln('╚══════════════════════════════════════════════════════════════╝')
  terminal.writeln('')
  terminal.writeln('欢迎使用服务器连接管理系统！')
  terminal.writeln('')
  terminal.writeln('请在左侧服务器列表中点击要连接的服务器。')
  terminal.writeln('连接成功后，您将获得完整的终端访问权限。')
  terminal.writeln('')
}

// 恢复终端内容
const restoreTerminalContent = (sessionId: string, terminal: Terminal) => {
  const session = terminalState.sessions.get(sessionId)
  if (!session) return

  terminal.clear()

  if (session.isConnected) {
    // 如果会话已连接，恢复所有历史输出
    console.log('恢复会话内容，输出数量:', session.terminalOutput.length)

    session.terminalOutput.forEach(output => {
      if (typeof output === 'object' && output.type === 'output') {
        terminal.write(output.content)
      }
    })

    // 更新已处理的输出数量
    processedOutputCounts.value.set(sessionId, session.terminalOutput.length)
  } else {
    // 如果会话未连接，显示连接状态信息
    terminal.writeln(`会话 ${session.server.name} (${session.server.host}:${session.server.port})`)
    terminal.writeln('连接已断开，请重新连接。')
    terminal.writeln('')
  }
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

    // 创建新的终端会话
    const sessionId = await connectToServer(server.id)

    if (sessionId) {
      // 连接成功，创建新的终端实例
      await nextTick()
      const containerElement = terminalContainers.value.get(sessionId)
      if (containerElement) {
        await initTerminal(sessionId, containerElement, false) // 新连接不恢复内容

        const terminal = terminals.value.get(sessionId)
        if (terminal) {
          terminal.clear()
          terminal.writeln(`正在连接到 ${server.name} (${server.host}:${server.port})...`)
          terminal.writeln('')

          // 连接成功后自动聚焦终端
          nextTick(() => {
            terminal.focus()
          })
        }
      }

      isInServerSelection.value = false
      // 连接成功后，终端内容完全由服务器控制
    } else {
      // 连接失败，在当前活动终端显示错误
      const activeSession = getActiveSession()
      if (activeSession) {
        const terminal = terminals.value.get(activeSession.id)
        if (terminal) {
          terminal.writeln(`✗ 连接失败: ${connectionError.value}`)
          terminal.writeln('')
          terminal.writeln('请检查服务器配置或网络连接，然后重试。')
        }
      }
    }
  }
}

// 断开指定会话的连接处理
const handleDisconnect = (sessionId?: string) => {
  const targetSessionId = sessionId || terminalState.activeSessionId
  if (!targetSessionId) return

  disconnectFromServer(targetSessionId)

  const terminalInstance = terminals.value.get(targetSessionId)
  if (terminalInstance) {
    terminalInstance.writeln('')
    terminalInstance.writeln('已断开服务器连接')
    terminalInstance.writeln('')

    // 清理终端实例
    try {
      terminalInstance.dispose()
    } catch (error) {
      console.warn('Terminal dispose error:', error)
    }
  }

  terminals.value.delete(targetSessionId)
  fitAddons.value.delete(targetSessionId)
  terminalContainers.value.delete(targetSessionId)
  processedOutputCounts.value.delete(targetSessionId)
  hasUnreadOutput.value.delete(targetSessionId)

  // 如果没有其他会话，回到服务器选择模式
  if (getAllSessions().length === 0) {
    isInServerSelection.value = true
  }
}

// 清空终端处理
const handleClearTerminal = (sessionId?: string) => {
  const targetSessionId = sessionId || terminalState.activeSessionId
  if (!targetSessionId) return

  const session = terminalState.sessions.get(targetSessionId)
  const terminal = terminals.value.get(targetSessionId)

  if (terminal) {
    if (session?.isConnected) {
      // 发送clear命令给服务器，让服务器处理
      sendCommand('clear\r', targetSessionId)
    } else {
      showWelcomeMessage(targetSessionId)
    }
  }
}

// 窗口大小调整处理
const handleResize = () => {
  // 调整所有终端的大小
  fitAddons.value.forEach((fitAddon, sessionId) => {
    const terminal = terminals.value.get(sessionId)
    if (fitAddon && terminal) {
      try {
        fitAddon.fit()

        // 如果是已连接的会话，发送新的大小到后端
        const session = terminalState.sessions.get(sessionId)
        if (session?.isConnected) {
          // 使用nextTick确保fit()完成后再获取新的大小
          nextTick(() => {
            const cols = terminal.cols
            const rows = terminal.rows
            console.log(`窗口调整后终端大小: ${cols}x${rows}, 会话: ${sessionId}`)
            sendResizeMessage(cols, rows, sessionId)
          })
        }
      } catch (error) {
        console.warn('Terminal resize error:', sessionId, error)
      }
    }
  })
}

// 监听服务器加载状态变化
watch([isLoading, servers], ([loading, serverList], [prevLoading, prevServerList]) => {
  // 当加载完成且在服务器选择模式时，刷新欢迎信息
  if (prevLoading && !loading && isInServerSelection.value) {
    // 如果没有活动会话，显示欢迎信息
    if (!getActiveSession()) {
      console.log('服务器加载完成，显示欢迎界面')
    }
  }
}, { deep: true })

// 跟踪每个会话已处理的输出数量
const processedOutputCounts = ref<Map<string, number>>(new Map())
// 跟踪每个会话是否有未读输出
const hasUnreadOutput = ref<Map<string, boolean>>(new Map())
// 定时器用于更新活动时间显示
const activityUpdateTimer = ref<NodeJS.Timeout | null>(null)

// 监听终端会话变化，将SSH输出显示到对应的xterm
watch(() => terminalState.sessions, (sessions) => {
  sessions.forEach((session, sessionId) => {
    const terminal = terminals.value.get(sessionId)
    if (terminal && session.terminalOutput.length > 0) {
      const processedCount = processedOutputCounts.value.get(sessionId) || 0
      const newOutputs = session.terminalOutput.slice(processedCount)

      newOutputs.forEach(output => {
        if (typeof output === 'object' && output.type === 'output') {
          const content = output.content
          // 直接显示服务器返回的所有内容，不做任何处理
          terminal.write(content)

          // 如果不是当前活动会话，标记为有未读输出
          if (sessionId !== terminalState.activeSessionId) {
            hasUnreadOutput.value.set(sessionId, true)
          }
        }
      })

      // 更新已处理的输出数量
      processedOutputCounts.value.set(sessionId, session.terminalOutput.length)
    }
  })
}, { deep: true })

// 监听活动会话变化
watch(() => terminalState.activeSessionId, (activeSessionId) => {
  if (activeSessionId) {
    isInServerSelection.value = false
    // 确保活动会话的终端容器可见
    nextTick(() => {
      const activeSession = getActiveSession()
      if (activeSession) {
        const terminal = terminals.value.get(activeSessionId)
        const fitAddon = fitAddons.value.get(activeSessionId)
        if (terminal && fitAddon) {
          try {
            fitAddon.fit()
            // 自动聚焦到活动终端
            terminal.focus()
          } catch (error) {
            console.warn('Terminal fit error:', error)
          }
        }
      }
    })
  } else if (getAllSessions().length === 0) {
    isInServerSelection.value = true
  }
})

// 键盘快捷键处理
const handleKeyDown = (event: KeyboardEvent) => {
  // Ctrl+Tab 切换到下一个tab
  if (event.ctrlKey && event.key === 'Tab') {
    event.preventDefault()
    const sessions = getAllSessions()
    if (sessions.length > 1) {
      const currentIndex = sessions.findIndex(s => s.id === terminalState.activeSessionId)
      const nextIndex = (currentIndex + 1) % sessions.length
      switchTab(sessions[nextIndex].id)
    }
  }
  // Ctrl+Shift+Tab 切换到上一个tab
  else if (event.ctrlKey && event.shiftKey && event.key === 'Tab') {
    event.preventDefault()
    const sessions = getAllSessions()
    if (sessions.length > 1) {
      const currentIndex = sessions.findIndex(s => s.id === terminalState.activeSessionId)
      const prevIndex = currentIndex === 0 ? sessions.length - 1 : currentIndex - 1
      switchTab(sessions[prevIndex].id)
    }
  }
  // Ctrl+W 关闭当前tab
  else if (event.ctrlKey && event.key === 'w') {
    event.preventDefault()
    const activeSession = getActiveSession()
    if (activeSession) {
      closeTab(activeSession.id)
    }
  }
}

// 组件挂载
onMounted(async () => {
  console.log('终端页面挂载，初始化UI')

  // 确保DOM完全渲染
  await nextTick()

  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
  // 监听键盘快捷键
  window.addEventListener('keydown', handleKeyDown)

  // 启动活动时间更新定时器
  activityUpdateTimer.value = setInterval(() => {
    // 强制更新组件以刷新活动时间显示
    // 这里我们不需要做任何事，Vue的响应式系统会自动更新
  }, 30000) // 每30秒更新一次

  // 恢复现有的终端会话（如果有的话）
  const sessions = getAllSessions()
  if (sessions.length > 0) {
    console.log('发现现有会话，恢复终端实例:', sessions.length)
    isInServerSelection.value = false

    // 为每个会话恢复终端实例
    sessions.forEach(session => {
      nextTick(() => {
        const containerElement = terminalContainers.value.get(session.id)
        if (containerElement) {
          initTerminal(session.id, containerElement, true) // 传入 true 表示恢复内容
        }
      })
    })
  }
})

// 页面激活时重新适配大小并恢复连接状态（从其他页面返回时）
onActivated(async () => {
  console.log('终端页面激活，恢复连接状态')
  addPageReference()

  await nextTick()
  setTimeout(() => {
    handleResize()

    // 恢复所有终端实例
    const sessions = getAllSessions()
    sessions.forEach(session => {
      const containerElement = terminalContainers.value.get(session.id)
      if (containerElement && !terminals.value.has(session.id)) {
        console.log('恢复终端实例:', session.id)
        initTerminal(session.id, containerElement, true) // 传入 true 表示恢复内容
      }
    })
  }, 100)
})

// 页面失活时保持连接状态（切换到其他页面时）
onDeactivated(() => {
  console.log('终端页面失活，保持连接状态')
  removePageReference()
})

// 组件卸载（只清理UI相关资源，保持连接状态）
onUnmounted(() => {
  console.log('终端页面卸载，清理UI资源但保持连接状态')

  window.removeEventListener('resize', handleResize)
  window.removeEventListener('keydown', handleKeyDown)

  // 清理定时器
  if (activityUpdateTimer.value) {
    clearInterval(activityUpdateTimer.value)
  }

  // 注意：不再自动断开所有连接，让全局状态管理器处理
  // disconnectAllSessions()

  // 清理所有终端实例（UI层面）
  terminals.value.forEach((terminal, sessionId) => {
    try {
      terminal.dispose()
    } catch (error) {
      console.warn('Terminal cleanup error:', sessionId, error)
    }
  })

  // 清理UI状态（但保持连接状态）
  terminals.value.clear()
  fitAddons.value.clear()
  terminalContainers.value.clear()
  processedOutputCounts.value.clear()
  hasUnreadOutput.value.clear()

  // 移除页面引用
  removePageReference()
})

// 分组展开/收起处理
const toggleGroup = (categoryId: number | string) => {
  const id = typeof categoryId === 'string' ? parseInt(categoryId) : categoryId
  if (expandedGroups.value.has(id)) {
    expandedGroups.value.delete(id)
  } else {
    expandedGroups.value.add(id)
  }
}

// 检查分组是否展开
const isGroupExpanded = (categoryId: number | string) => {
  const id = typeof categoryId === 'string' ? parseInt(categoryId) : categoryId
  return expandedGroups.value.has(id)
}

// 初始化时展开所有分组
onMounted(() => {
  // 延迟展开所有分组，等待服务器数据加载
  setTimeout(() => {
    const categories = getAllCategories()
    categories.forEach(category => expandedGroups.value.add(category.id))
  }, 1000)
})

// Tab页管理功能
const switchTab = (sessionId: string) => {
  switchToSession(sessionId)
  // 清除未读标记
  hasUnreadOutput.value.set(sessionId, false)

  // 切换标签页后自动聚焦终端
  nextTick(() => {
    const terminal = terminals.value.get(sessionId)
    if (terminal) {
      terminal.focus()
    }
  })
}

const closeTab = (sessionId: string) => {
  handleDisconnect(sessionId)
  // 清理未读标记
  hasUnreadOutput.value.delete(sessionId)
}

const getTabTitle = (session: any) => {
  return `${session.server.name} (${session.server.host})`
}

const getTabSubtitle = (session: any) => {
  if (session.isConnected) {
    const duration = Math.floor((Date.now() - session.lastActivity.getTime()) / 1000)
    if (duration < 60) {
      return `活动 ${duration}s 前`
    } else if (duration < 3600) {
      return `活动 ${Math.floor(duration / 60)}m 前`
    } else {
      return `活动 ${Math.floor(duration / 3600)}h 前`
    }
  }
  return '未连接'
}

const getTabIcon = (session: any) => {
  return session.server.icon
}

// 右键菜单功能
const showContextMenu = ref(false)
const contextMenuPosition = ref({ x: 0, y: 0 })
const contextMenuSessionId = ref<string>('')

const handleTabRightClick = (event: MouseEvent, sessionId: string) => {
  event.preventDefault()
  contextMenuSessionId.value = sessionId
  contextMenuPosition.value = { x: event.clientX, y: event.clientY }
  showContextMenu.value = true
}

const closeContextMenu = () => {
  showContextMenu.value = false
}

const duplicateTab = (sessionId: string) => {
  const session = terminalState.sessions.get(sessionId)
  if (session) {
    // 连接到同一个服务器
    connectToServerByIndex(servers.value.findIndex(s => s.id === session.server.id))
  }
  closeContextMenu()
}

const closeOtherTabs = (sessionId: string) => {
  const sessions = getAllSessions()
  sessions.forEach(session => {
    if (session.id !== sessionId) {
      closeTab(session.id)
    }
  })
  closeContextMenu()
}

const closeAllTabs = () => {
  const sessions = getAllSessions()
  sessions.forEach(session => {
    closeTab(session.id)
  })
  closeContextMenu()
}

// 设置终端容器引用
const setTerminalContainer = (sessionId: string, element: HTMLElement | null) => {
  if (element) {
    terminalContainers.value.set(sessionId, element)
    // 如果是新创建的会话或恢复的会话，初始化终端
    if (!terminals.value.has(sessionId)) {
      nextTick(() => {
        console.log('为会话创建终端实例:', sessionId)
        // 检查是否是恢复的会话
        const session = terminalState.sessions.get(sessionId)
        const isRestoring = session && session.terminalOutput.length > 0
        initTerminal(sessionId, element, isRestoring)
      })
    }
  } else {
    terminalContainers.value.delete(sessionId)
  }
}

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
          <div class="panel-actions">
            <button @click="showGrouped = !showGrouped" class="action-btn group-btn" :title="showGrouped ? '平铺显示' : '分组显示'">
              <Icon :icon="showGrouped ? 'material-symbols:view-list' : 'material-symbols:folder'" class="btn-icon" />
            </button>
            <button @click="loadServersFromDatabase" class="action-btn reload-btn" title="重新加载">
              <Icon icon="material-symbols:refresh" class="btn-icon" />
            </button>
          </div>
        </div>

        <div class="server-list">
          <!-- 分组显示模式 -->
          <template v-if="showGrouped">
            <div v-for="(groupServers, categoryId) in getGroupedServers()" :key="categoryId" class="server-group">
              <div class="group-header" @click="toggleGroup(categoryId)">
                <Icon
                  :icon="isGroupExpanded(categoryId) ? 'material-symbols:expand-less' : 'material-symbols:expand-more'"
                  class="group-expand-icon"
                />
                <Icon icon="material-symbols:folder" class="group-icon" />
                <span class="group-name">{{ getCategoryName(parseInt(categoryId)) }}</span>
                <span class="group-count">({{ groupServers.length }})</span>
              </div>
              <Transition name="group-expand">
                <div v-show="isGroupExpanded(categoryId)" class="group-servers">
                  <div
                    v-for="(server, index) in groupServers"
                    :key="server.id"
                    class="server-item"
                    :class="{
                      'connected': server.status === 'connected',
                      'active': getActiveSession()?.server.id === server.id
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
                      <p v-if="server.description" class="server-description">{{ server.description }}</p>
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
                'active': getActiveSession()?.server.id === server.id
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
                <p v-if="server.description" class="server-description">{{ server.description }}</p>
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

        <!-- Tab页导航 -->
        <div v-if="getAllSessions().length > 0" class="terminal-tabs">
          <div class="tabs-header">
            <span class="tabs-count">{{ getAllSessions().length }} 个连接</span>
            <div class="tabs-actions">
              <button @click="disconnectAllSessions()" class="action-btn small" title="关闭所有连接">
                <Icon icon="material-symbols:close-fullscreen" />
              </button>
            </div>
          </div>
          <div class="tabs-container">
            <div
              v-for="session in getAllSessions()"
              :key="session.id"
              class="tab-item"
              :class="{
                'active': session.id === terminalState.activeSessionId,
                'has-unread': hasUnreadOutput.get(session.id)
              }"
              @click="switchTab(session.id)"
              @contextmenu="handleTabRightClick($event, session.id)"
            >
              <div class="tab-content">
                <Icon :icon="getTabIcon(session)" class="tab-icon" />
                <div class="tab-info">
                  <span class="tab-title">{{ getTabTitle(session) }}</span>
                  <span class="tab-subtitle">{{ getTabSubtitle(session) }}</span>
                </div>
                <div class="tab-status">
                  <Icon
                    :icon="session.isConnected ? 'material-symbols:check-circle' : 'material-symbols:sync'"
                    :class="session.isConnected ? 'text-green-500' : 'text-yellow-500'"
                    class="status-icon"
                  />
                </div>
                <button
                  @click.stop="closeTab(session.id)"
                  class="tab-close"
                  title="关闭连接"
                >
                  <Icon icon="material-symbols:close" />
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 终端面板 -->
        <div class="terminal-panel">
          <!-- 欢迎界面 -->
          <div v-if="isInServerSelection" class="welcome-panel">
            <div class="welcome-content">
              <Icon icon="material-symbols:terminal" class="welcome-icon" />
              <h3>服务器连接管理系统</h3>
              <p>请在左侧服务器列表中选择要连接的服务器</p>
              <p>支持同时连接多个服务器，使用Tab页进行切换</p>
            </div>
          </div>

          <!-- 终端容器 -->
          <div v-else class="terminal-containers">
            <div
              v-for="session in getAllSessions()"
              :key="session.id"
              :ref="(el) => setTerminalContainer(session.id, el as HTMLElement)"
              class="xterm-container"
              :class="{ 'active': session.id === terminalState.activeSessionId }"
            ></div>
          </div>

          <!-- 终端控制栏 -->
          <div v-if="!isInServerSelection && getActiveSession()" class="terminal-controls-bar">
            <div class="terminal-info">
              <Icon icon="material-symbols:terminal" class="terminal-icon" />
              <Icon
                v-if="getActiveSession()"
                :icon="getActiveSession()?.server.icon"
                class="server-icon"
              />
              <span v-if="getActiveSession()">
                {{ getActiveSession()?.server.name }} - {{ getActiveSession()?.server.host }}
              </span>
            </div>
            <div class="terminal-controls">
              <button @click="handleClearTerminal()" class="control-btn" title="清空">
                <Icon icon="material-symbols:clear-all" />
              </button>
              <button @click="handleDisconnect()" class="control-btn" title="断开">
                <Icon icon="material-symbols:close" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="showContextMenu"
      class="context-menu"
      :style="{ left: contextMenuPosition.x + 'px', top: contextMenuPosition.y + 'px' }"
      @click="closeContextMenu"
    >
      <div class="context-menu-item" @click="duplicateTab(contextMenuSessionId)">
        <Icon icon="material-symbols:content-copy" />
        <span>复制连接</span>
      </div>
      <div class="context-menu-item" @click="closeOtherTabs(contextMenuSessionId)">
        <Icon icon="material-symbols:close" />
        <span>关闭其他</span>
      </div>
      <div class="context-menu-item" @click="closeAllTabs()">
        <Icon icon="material-symbols:close-fullscreen" />
        <span>关闭所有</span>
      </div>
    </div>

    <!-- 点击遮罩关闭菜单 -->
    <div
      v-if="showContextMenu"
      class="context-menu-overlay"
      @click="closeContextMenu"
    ></div>
  </NuxtLayout>
</template>
