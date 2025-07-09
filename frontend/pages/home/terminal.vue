<script setup lang="ts">
import {ref, onMounted, onUnmounted, nextTick} from 'vue'
import {Motion} from "motion-v"
import {Icon} from '@iconify/vue'
import {Terminal} from 'xterm'
import {FitAddon} from 'xterm-addon-fit'
import {useTerminal, type ServerConnection} from '~/composables/useTerminal'
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
const showTerminal = ref(false)
const currentCommand = ref('')

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
        sendCommand(currentCommand.value.trim())
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

  // 显示欢迎信息
  terminal.value.writeln('Terminal Ready - Please select a server to connect')
  terminal.value.write('$ ')
}

// 连接服务器处理
const handleConnect = async (server: ServerConnection) => {
  if (isConnecting.value) return

  const success = await connectToServer(server.id)

  if (success && terminal.value) {
    showTerminal.value = true
    await nextTick()

    // 清空终端并显示连接信息
    terminal.value.clear()
    terminal.value.writeln(`Connected to ${server.name} (${server.host}:${server.port})`)
    terminal.value.writeln(`Welcome to ${server.name}`)
    terminal.value.writeln(`Last login: ${server.lastConnected}`)
    terminal.value.write(`${server.username}@${server.host}:~$ `)

    // 重新适配大小
    fitAddon.value?.fit()
  }
}

// 断开连接处理
const handleDisconnect = () => {
  disconnectFromServer()
  showTerminal.value = false
  currentCommand.value = ''

  if (terminal.value) {
    terminal.value.clear()
    terminal.value.writeln('Terminal Ready - Please select a server to connect')
    terminal.value.write('$ ')
  }
}

// 清空终端处理
const handleClearTerminal = () => {
  clearTerminal()
  if (terminal.value && terminalState.currentServer) {
    terminal.value.clear()
    terminal.value.write(`${terminalState.currentServer.username}@${terminalState.currentServer.host}:~$ `)
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

    <!-- 页面标题 -->


    <div class="header-actions">
      <button
          v-if="terminalState.isConnected"
          @click="handleClearTerminal"
          class="action-btn clear-btn"
          title="清空终端"
      >
        <Icon icon="material-symbols:clear-all" class="btn-icon"/>
        清空
      </button>
      <button
          v-if="terminalState.isConnected"
          @click="handleDisconnect"
          class="action-btn disconnect-btn"
          title="断开连接"
      >
        <Icon icon="material-symbols:power-off" class="btn-icon"/>
        断开
      </button>
    </div>
    <!-- 连接错误提示 -->
    <div v-if="connectionError" class="error-message">
      <Icon icon="material-symbols:error" class="error-icon"/>
      {{ connectionError }}
    </div>

    <!-- 主要内容区域 -->
    <div class="terminal-content">
      <!-- 服务器列表 -->
      <div class="servers-panel" :class="{ 'collapsed': showTerminal }">
        <div class="panel-header">
          <h2 class="panel-title">服务器列表</h2>
          <div class="connection-status" v-if="terminalState.currentServer">
            <Icon :icon="getStatusIcon(terminalState.currentServer.status)"
                  :class="['status-icon', getStatusColor(terminalState.currentServer.status)]"/>
            <span class="status-text">{{ terminalState.currentServer.name }}</span>
          </div>
        </div>

        <div class="servers-list">
          <Motion
              v-for="(server, index) in servers"
              :key="server.id"
              :initial="{ opacity: 0, y: 20 }"
              :animate="{ opacity: 1, y: 0 }"
              :transition="{ duration: 0.3, delay: index * 0.1 }"
          >
            <div
                class="server-card"
                :class="{ 
                  'selected': selectedServer?.id === server.id,
                  'connected': server.status === 'connected',
                  'connecting': server.status === 'connecting'
                }"
            >
              <div class="server-info">
                <div class="server-header">
                  <h3 class="server-name">{{ server.name }}</h3>
                  <div class="server-status">
                    <Icon :icon="getStatusIcon(server.status)"
                          :class="['status-icon', getStatusColor(server.status)]"/>
                    <span :class="['status-text', getStatusColor(server.status)]">
                        {{
                        server.status === 'connected' ? '已连接' :
                            server.status === 'connecting' ? '连接中' : '未连接'
                      }}
                      </span>
                  </div>
                </div>

                <div class="server-details">
                  <div class="detail-item">
                    <Icon icon="material-symbols:dns" class="detail-icon"/>
                    <span>{{ server.host }}:{{ server.port }}</span>
                  </div>
                  <div class="detail-item">
                    <Icon icon="material-symbols:person" class="detail-icon"/>
                    <span>{{ server.username }}</span>
                  </div>
                  <div class="detail-item">
                    <Icon icon="material-symbols:security" class="detail-icon"/>
                    <span>{{ server.protocol.toUpperCase() }}</span>
                  </div>
                </div>

                <p class="server-description">{{ server.description }}</p>

                <div class="server-meta">
                    <span class="last-connected">
                      最后连接: {{ server.lastConnected || '从未连接' }}
                    </span>
                </div>
              </div>

              <div class="server-actions">
                <button
                    v-if="server.status !== 'connected'"
                    @click="handleConnect(server)"
                    :disabled="isConnecting"
                    class="connect-btn"
                >
                  <Icon v-if="!isConnecting" icon="material-symbols:play-arrow" class="btn-icon"/>
                  <Icon v-else icon="material-symbols:sync" class="btn-icon animate-spin"/>
                  {{ isConnecting ? '连接中...' : '连接' }}
                </button>

                <button
                    v-else
                    @click="handleDisconnect"
                    class="disconnect-btn"
                >
                  <Icon icon="material-symbols:stop" class="btn-icon"/>
                  断开
                </button>
              </div>
            </div>
          </Motion>
        </div>
      </div>

      <!-- 终端面板 -->
      <div class="terminal-panel" :class="{ 'visible': showTerminal }">
        <div class="terminal-header">
          <div class="terminal-title">
            <Icon icon="material-symbols:terminal" class="terminal-icon"/>
            <span v-if="terminalState.currentServer">
                {{ terminalState.currentServer.name }} - {{ terminalState.currentServer.host }}
              </span>
            <span v-else>终端</span>
          </div>
          <div class="terminal-controls">
            <button @click="handleClearTerminal" class="control-btn" title="清空">
              <Icon icon="material-symbols:clear-all"/>
            </button>
            <button @click="handleDisconnect" class="control-btn" title="断开">
              <Icon icon="material-symbols:close"/>
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
