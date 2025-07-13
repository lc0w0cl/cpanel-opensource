<template>
  <div class="p-6">
    <h1 class="text-2xl font-bold mb-4">终端连接状态测试</h1>
    
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <!-- 连接统计 -->
      <div class="bg-white rounded-lg shadow p-4">
        <h2 class="text-lg font-semibold mb-3">连接统计</h2>
        <div class="space-y-2">
          <div>总会话数: {{ stats.totalSessions }}</div>
          <div>活动会话数: {{ stats.activeSessions }}</div>
          <div>页面引用数: {{ stats.pageReferences }}</div>
          <div>清理定时器: {{ stats.hasCleanupTimer ? '已启动' : '未启动' }}</div>
        </div>
        <button @click="refreshStats" class="mt-3 px-4 py-2 bg-blue-500 text-white rounded">
          刷新统计
        </button>
      </div>
      
      <!-- 会话列表 -->
      <div class="bg-white rounded-lg shadow p-4">
        <h2 class="text-lg font-semibold mb-3">活动会话</h2>
        <div v-if="sessions.length === 0" class="text-gray-500">
          暂无活动会话
        </div>
        <div v-else class="space-y-2">
          <div v-for="session in sessions" :key="session.id" class="border rounded p-2">
            <div class="font-medium">{{ session.server.name }}</div>
            <div class="text-sm text-gray-600">{{ session.server.host }}:{{ session.server.port }}</div>
            <div class="text-sm" :class="session.isConnected ? 'text-green-600' : 'text-red-600'">
              {{ session.isConnected ? '已连接' : '未连接' }}
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 操作按钮 -->
    <div class="mt-6 space-x-4">
      <NuxtLink to="/home/terminal" class="px-4 py-2 bg-green-500 text-white rounded">
        前往终端页面
      </NuxtLink>
      <NuxtLink to="/home/dashboard" class="px-4 py-2 bg-blue-500 text-white rounded">
        前往仪表板
      </NuxtLink>
      <button @click="cleanupConnections" class="px-4 py-2 bg-red-500 text-white rounded">
        手动清理连接
      </button>
    </div>
    
    <!-- 日志 -->
    <div class="mt-6 bg-gray-100 rounded-lg p-4">
      <h2 class="text-lg font-semibold mb-3">操作日志</h2>
      <div class="max-h-40 overflow-y-auto">
        <div v-for="(log, index) in logs" :key="index" class="text-sm text-gray-700">
          {{ log }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useTerminal } from '~/composables/useTerminal'

// 页面元数据
definePageMeta({
  layout: 'dashboard',
  middleware: 'auth'
})

const { getAllSessions } = useTerminal()
const { $terminalState } = useNuxtApp()

const stats = ref({
  totalSessions: 0,
  activeSessions: 0,
  pageReferences: 0,
  hasCleanupTimer: false
})

const sessions = ref<any[]>([])
const logs = ref<string[]>([])

const addLog = (message: string) => {
  const timestamp = new Date().toLocaleTimeString()
  logs.value.unshift(`[${timestamp}] ${message}`)
  if (logs.value.length > 50) {
    logs.value = logs.value.slice(0, 50)
  }
}

const refreshStats = () => {
  stats.value = $terminalState.getConnectionStats()
  sessions.value = getAllSessions()
  addLog('刷新统计信息')
}

const cleanupConnections = () => {
  $terminalState.cleanupAllConnections()
  addLog('手动清理所有连接')
  setTimeout(refreshStats, 100)
}

let refreshTimer: NodeJS.Timeout | null = null

onMounted(() => {
  addLog('测试页面挂载')
  refreshStats()
  
  // 每秒刷新一次统计
  refreshTimer = setInterval(refreshStats, 1000)
})

onUnmounted(() => {
  addLog('测试页面卸载')
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>
