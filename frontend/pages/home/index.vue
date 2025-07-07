<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Motion } from "motion-v"
import { Icon } from '@iconify/vue'
import { useSystemInfo, type SystemInfo } from '~/composables/useSystemInfo'

// 子页面不需要定义 layout 和 middleware，由父页面处理

// 系统信息状态
const systemInfo = ref<SystemInfo | null>(null)
const loading = ref(true)
const error = ref('')
const refreshInterval = ref<NodeJS.Timeout | null>(null)

// 系统信息API
const {
  getSystemInfo,
  formatUptime,
  formatStorage,
  formatFrequency,
  getUsageColor,
  getUsageBgColor
} = useSystemInfo()

// 获取系统信息
const fetchSystemInfo = async () => {
  try {
    loading.value = true
    error.value = ''
    systemInfo.value = await getSystemInfo()
  } catch (err: any) {
    error.value = err.message || '获取系统信息失败'
    console.error('获取系统信息失败:', err)
  } finally {
    loading.value = false
  }
}

// 启动自动刷新
const startAutoRefresh = () => {
  // 每30秒刷新一次
  refreshInterval.value = setInterval(fetchSystemInfo, 30000)
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (refreshInterval.value) {
    clearInterval(refreshInterval.value)
    refreshInterval.value = null
  }
}

// 组件挂载时获取数据并启动自动刷新
onMounted(() => {
  fetchSystemInfo()
  startAutoRefresh()
})

// 组件卸载时停止自动刷新
onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<template>
  <NuxtLayout>
    <div class="system-dashboard">
      <!-- 页面标题 -->
      <div class="dashboard-header">
        <h1 class="dashboard-title">系统监控</h1>
        <button
          @click="fetchSystemInfo"
          :disabled="loading"
          class="refresh-btn"
          title="刷新数据"
        >
          <Icon
            icon="material-symbols:refresh"
            :class="['refresh-icon', { 'animate-spin': loading }]"
          />
        </button>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading && !systemInfo" class="loading-container">
        <div class="loading-spinner">
          <Icon icon="eos-icons:loading" class="loading-icon" />
        </div>
        <p class="loading-text">正在获取系统信息...</p>
      </div>

      <!-- 错误状态 -->
      <div v-else-if="error" class="error-container">
        <div class="error-content">
          <Icon icon="material-symbols:error-outline" class="error-icon" />
          <h3 class="error-title">获取系统信息失败</h3>
          <p class="error-message">{{ error }}</p>
          <button @click="fetchSystemInfo" class="retry-btn">
            <Icon icon="material-symbols:refresh" class="retry-icon" />
            重试
          </button>
        </div>
      </div>

      <!-- 系统信息展示 -->
      <div v-else-if="systemInfo" class="system-info-grid">
        <!-- 系统概览 - 独占第一行 -->
        <Motion
          :initial="{ opacity: 0, y: 20 }"
          :animate="{ opacity: 1, y: 0 }"
          :transition="{ duration: 0.5, delay: 0.0 }"
          class="system-overview-wrapper"
        >
          <div class="system-overview-card">
            <div class="system-header">
              <div class="system-icon-wrapper">
                <div class="system-icon-bg">
                  <Icon icon="material-symbols:computer" class="system-icon" />
                </div>
              </div>
              <div class="system-main-info">
                <h2 class="system-title">{{ systemInfo.system.osName }}</h2>
                <div class="system-meta">
                  <span class="system-version">版本 {{ systemInfo.system.osVersion }}</span>
                  <span class="system-arch">{{ systemInfo.system.architecture }}</span>
                  <span class="system-hostname">{{ systemInfo.system.hostname }}</span>
                </div>
              </div>
              <div class="system-uptime">
                <div class="uptime-label">运行时间</div>
                <div class="uptime-value">{{ formatUptime(systemInfo.system.uptime) }}</div>
              </div>
            </div>

            <div class="system-specs">
              <div class="spec-card">
                <div class="spec-icon">
                  <Icon icon="material-symbols:memory" />
                </div>
                <div class="spec-content">
                  <div class="spec-label">处理器</div>
                  <div class="spec-value">{{ systemInfo.cpu.model }}</div>
                </div>
              </div>

              <div class="spec-card">
                <div class="spec-icon">
                  <Icon icon="material-symbols:developer-board" />
                </div>
                <div class="spec-content">
                  <div class="spec-label">核心配置</div>
                  <div class="spec-value">{{ systemInfo.cpu.cores }} 核心 / {{ systemInfo.cpu.logicalProcessors }} 线程</div>
                </div>
              </div>

              <div class="spec-card">
                <div class="spec-icon">
                  <Icon icon="material-symbols:storage" />
                </div>
                <div class="spec-content">
                  <div class="spec-label">内存容量</div>
                  <div class="spec-value">{{ formatStorage(systemInfo.memory.total) }}</div>
                </div>
              </div>
            </div>
          </div>
        </Motion>

        <!-- CPU信息卡片 -->
        <Motion
          :initial="{ opacity: 0, y: 20 }"
          :animate="{ opacity: 1, y: 0 }"
          :transition="{ duration: 0.5, delay: 0.1 }"
        >
          <div class="info-card">
            <div class="card-icon cpu-icon">
              <Icon icon="material-symbols:memory" />
            </div>
            <div class="card-right">
              <div class="card-header-new">
                <span class="card-title">CPU</span>
                <span :class="['usage-value', getUsageColor(systemInfo.cpu.usage)]">
                  {{ systemInfo.cpu.usage.toFixed(1) }}%
                </span>
              </div>
              <div class="usage-bar">
                <div
                  :class="['usage-fill', getUsageBgColor(systemInfo.cpu.usage)]"
                  :style="{ width: `${systemInfo.cpu.usage}%` }"
                ></div>
              </div>
            </div>
          </div>
        </Motion>

        <!-- 内存信息卡片 -->
        <Motion
          :initial="{ opacity: 0, y: 20 }"
          :animate="{ opacity: 1, y: 0 }"
          :transition="{ duration: 0.5, delay: 0.2 }"
        >
          <div class="info-card">
            <div class="card-icon memory-icon">
              <Icon icon="material-symbols:storage" />
            </div>
            <div class="card-right">
              <div class="card-header-new">
                <span class="card-title">内存</span>
                <span :class="['usage-value', getUsageColor(systemInfo.memory.usage)]">
                  {{ systemInfo.memory.usage.toFixed(1) }}%
                </span>
              </div>
              <div class="usage-bar">
                <div
                  :class="['usage-fill', getUsageBgColor(systemInfo.memory.usage)]"
                  :style="{ width: `${systemInfo.memory.usage}%` }"
                ></div>
              </div>
            </div>
          </div>
        </Motion>

        <!-- 磁盘信息卡片 -->
        <Motion
          v-for="(disk, index) in systemInfo.disks"
          :key="disk.name"
          :initial="{ opacity: 0, y: 20 }"
          :animate="{ opacity: 1, y: 0 }"
          :transition="{ duration: 0.5, delay: 0.3 + index * 0.1 }"
        >
          <div class="info-card">
            <div class="card-icon disk-icon">
              <Icon icon="material-symbols:hard-drive-2" />
            </div>
            <div class="card-right">
              <div class="card-header-new">
                <span class="card-title">磁盘 {{ disk.name }}</span>
                <span :class="['usage-value', getUsageColor(disk.usage)]">
                  {{ disk.usage.toFixed(1) }}%
                </span>
              </div>
              <div class="usage-bar">
                <div
                  :class="['usage-fill', getUsageBgColor(disk.usage)]"
                  :style="{ width: `${disk.usage}%` }"
                ></div>
              </div>
            </div>
          </div>
        </Motion>


      </div>
    </div>
  </NuxtLayout>
</template>

<style scoped>
/* 系统监控面板样式 */
.system-dashboard {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
  min-height: 100vh;
}

/* 页面标题区域 */
.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 2rem;
}

.dashboard-title {
  font-size: 2rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.refresh-btn {
  width: 3rem;
  height: 3rem;
  border-radius: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  /* 液态玻璃效果 */
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.2) 0%,
    rgba(255, 255, 255, 0.1) 50%,
    rgba(255, 255, 255, 0.05) 100%
  );
  backdrop-filter: blur(15px) saturate(150%);
  -webkit-backdrop-filter: blur(15px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.2);

  box-shadow:
    0 6px 20px rgba(0, 0, 0, 0.15),
    0 2px 6px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.refresh-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow:
    0 8px 25px rgba(0, 0, 0, 0.2),
    0 4px 10px rgba(0, 0, 0, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
}

.refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.refresh-icon {
  width: 1.5rem;
  height: 1.5rem;
  color: rgba(255, 255, 255, 0.9);
}

/* 加载状态 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  gap: 1rem;
}

.loading-spinner {
  width: 4rem;
  height: 4rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-icon {
  width: 100%;
  height: 100%;
  color: rgba(59, 130, 246, 0.8);
}

.loading-text {
  font-size: 1.125rem;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
}

/* 错误状态 */
.error-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.error-content {
  text-align: center;
  max-width: 400px;
  padding: 2rem;
  border-radius: 1rem;
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.1) 0%,
    rgba(239, 68, 68, 0.05) 100%
  );
  border: 1px solid rgba(239, 68, 68, 0.3);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
}

.error-icon {
  width: 3rem;
  height: 3rem;
  color: rgba(239, 68, 68, 0.8);
  margin-bottom: 1rem;
}

.error-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 0.5rem 0;
}

.error-message {
  color: rgba(255, 255, 255, 0.7);
  margin: 0 0 1.5rem 0;
}

.retry-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  border-radius: 0.5rem;
  border: none;
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.8) 0%,
    rgba(59, 130, 246, 0.6) 100%
  );
  color: white;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.retry-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.retry-icon {
  width: 1rem;
  height: 1rem;
}

/* 系统信息网格 */
.system-info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 1.5rem;
}

/* 系统概览包装器 - 独占一行 */
.system-overview-wrapper {
  grid-column: 1 / -1;
  margin-bottom: 1.5rem;
}

/* 系统概览卡片 */
.system-overview-card {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 1.25rem;
  padding: 2rem;
  transition: all 0.3s ease;
}

.system-overview-card:hover {
  transform: translateY(-2px);
}

/* 系统头部 */
.system-header {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  margin-bottom: 2rem;
}

/* 系统图标包装器 */
.system-icon-wrapper {
  flex-shrink: 0;
}

.system-icon-bg {
  width: 4rem;
  height: 4rem;
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.2) 0%,
    rgba(147, 51, 234, 0.2) 100%
  );
  border: 1px solid rgba(59, 130, 246, 0.3);
  border-radius: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.system-icon-bg::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(45deg,
    transparent 30%,
    rgba(255, 255, 255, 0.1) 50%,
    transparent 70%
  );
  transform: translateX(-100%);
  transition: transform 0.6s ease;
}

.system-icon-bg:hover::before {
  transform: translateX(100%);
}

.system-icon {
  width: 2rem;
  height: 2rem;
  color: rgba(255, 255, 255, 0.9);
  z-index: 1;
}

/* 系统主要信息 */
.system-main-info {
  flex: 1;
  min-width: 0;
}

.system-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.95);
  margin-bottom: 0.5rem;
  line-height: 1.2;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(255, 255, 255, 0.8) 100%
  );
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.system-meta {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.system-version,
.system-arch,
.system-hostname {
  padding: 0.25rem 0.75rem;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  line-height: 1.4;
}

.system-version {
  background: rgba(34, 197, 94, 0.15);
  border: 1px solid rgba(34, 197, 94, 0.3);
  color: rgba(34, 197, 94, 1);
}

.system-arch {
  background: rgba(59, 130, 246, 0.15);
  border: 1px solid rgba(59, 130, 246, 0.3);
  color: rgba(59, 130, 246, 1);
}

.system-hostname {
  background: rgba(168, 85, 247, 0.15);
  border: 1px solid rgba(168, 85, 247, 0.3);
  color: rgba(168, 85, 247, 1);
}

/* 运行时间 */
.system-uptime {
  text-align: right;
  flex-shrink: 0;
}

.uptime-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 0.25rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.uptime-value {
  font-size: 1.125rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.3;
}

/* 系统规格 */
.system-specs {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1rem;
}

.spec-card {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 0.875rem;
  padding: 1.25rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  transition: all 0.3s ease;
}

.spec-card:hover {
  border-color: rgba(255, 255, 255, 0.12);
  transform: translateY(-1px);
}

.spec-icon {
  width: 2.5rem;
  height: 2.5rem;
  background: transparent;
  border-radius: 0.625rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.spec-icon svg {
  width: 1.25rem;
  height: 1.25rem;
  color: rgba(255, 255, 255, 0.8);
}

.spec-content {
  flex: 1;
  min-width: 0;
}

.spec-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 0.25rem;
  line-height: 1.3;
}

.spec-value {
  font-size: 0.95rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.4;
}

/* 信息卡片 */
.info-card {
  border-radius: 1rem;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;

  /* 透明效果 */
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.2);

  /* 阴影效果 */
  box-shadow:
    0 20px 40px rgba(0, 0, 0, 0.15),
    0 8px 16px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.3),
    inset 0 -1px 0 rgba(0, 0, 0, 0.1);
}

.info-card:hover {
  transform: translateY(-4px);
  box-shadow:
    0 25px 50px rgba(0, 0, 0, 0.2),
    0 12px 20px rgba(0, 0, 0, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.4),
    inset 0 -1px 0 rgba(0, 0, 0, 0.1);
}

/* 卡片头部 */
.card-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem 1.5rem 1rem 1.5rem;
}

.card-icon {
  width: 3rem;
  height: 3rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: transparent;
  border: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.card-icon svg {
  width: 1.5rem;
  height: 1.5rem;
  color: rgba(255, 255, 255, 0.9);
}

/* CPU图标样式 */
.cpu-icon {
  background: transparent;
  border: none;
}

/* 内存图标样式 */
.memory-icon {
  background: transparent;
  border: none;
}

/* 磁盘图标样式 */
.disk-icon {
  background: transparent;
  border: none;
}

/* 系统图标样式 */
.system-icon {
  background: transparent;
  border: none;
}

/* 右侧内容区域 */
.card-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

/* 新的头部布局 */
.card-header-new {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 系统信息内容 */
.system-info-content {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.card-title {
  font-size: 1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.card-subtitle {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}



.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.info-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.info-label {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
}

.info-value {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
  text-align: right;
  max-width: 60%;
  word-break: break-word;
}

/* 使用率部分 */
.usage-section {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.usage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.usage-label {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
}

.usage-value {
  font-size: 1rem;
  font-weight: 700;
}

/* 使用率进度条 */
.usage-bar {
  width: 100%;
  height: 0.375rem;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 0.25rem;
  overflow: hidden;
  position: relative;
}

.usage-fill {
  height: 100%;
  border-radius: 0.25rem;
  transition: all 0.3s ease;
  position: relative;
}

.usage-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.3) 50%,
    transparent 100%
  );
  animation: shimmer 2s infinite;
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

/* 使用率颜色类 */
.text-green-400 { color: rgb(74, 222, 128); }
.text-blue-400 { color: rgb(96, 165, 250); }
.text-yellow-400 { color: rgb(251, 191, 36); }
.text-red-400 { color: rgb(248, 113, 113); }

.bg-green-500 { background-color: rgb(34, 197, 94); }
.bg-blue-500 { background-color: rgb(59, 130, 246); }
.bg-yellow-500 { background-color: rgb(234, 179, 8); }
.bg-red-500 { background-color: rgb(239, 68, 68); }

/* 系统信息卡片特殊样式 */
.system-card .info-item:last-child {
  margin-bottom: 0;
}

/* 动画类 */
.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .system-info-grid {
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 1.25rem;
  }
}

@media (max-width: 768px) {
  .system-dashboard {
    padding: 1rem;
  }

  .dashboard-header {
    margin-bottom: 1.5rem;
  }

  .dashboard-title {
    font-size: 1.75rem;
  }

  .refresh-btn {
    width: 2.5rem;
    height: 2.5rem;
  }

  .refresh-icon {
    width: 1.25rem;
    height: 1.25rem;
  }

  .system-info-grid {
    grid-template-columns: 1fr;
    gap: 1rem;
  }

  .info-card {
    border-radius: 0.75rem;
  }

  .card-header {
    padding: 1.25rem 1.25rem 0.75rem 1.25rem;
  }

  .card-content {
    padding: 0 1.25rem 1.25rem 1.25rem;
  }

  .card-icon {
    width: 2.5rem;
    height: 2.5rem;
  }

  .card-icon svg {
    width: 1.25rem;
    height: 1.25rem;
  }

  /* 移动端系统概览调整 */
  .system-overview-card {
    padding: 1.5rem;
  }

  .system-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
    margin-bottom: 1.5rem;
  }

  .system-icon-bg {
    width: 3rem;
    height: 3rem;
  }

  .system-icon {
    width: 1.5rem;
    height: 1.5rem;
  }

  .system-title {
    font-size: 1.5rem;
  }

  .system-meta {
    gap: 0.5rem;
  }

  .system-version,
  .system-arch,
  .system-hostname {
    font-size: 0.8rem;
    padding: 0.2rem 0.6rem;
  }

  .system-uptime {
    text-align: left;
    width: 100%;
  }

  .system-specs {
    grid-template-columns: 1fr;
    gap: 0.75rem;
  }

  .spec-card {
    padding: 1rem;
  }

  .spec-icon {
    width: 2rem;
    height: 2rem;
  }

  .spec-icon svg {
    width: 1rem;
    height: 1rem;
  }

  .card-title {
    font-size: 1rem;
  }

  .card-subtitle {
    font-size: 0.8rem;
  }

  .info-item {
    padding: 0.5rem 0;
  }

  .info-label,
  .info-value {
    font-size: 0.8rem;
  }

  .usage-label {
    font-size: 0.8rem;
  }

  .usage-value {
    font-size: 0.9rem;
  }
}

@media (max-width: 480px) {
  .system-dashboard {
    padding: 0.75rem;
  }

  .dashboard-header {
    flex-direction: column;
    gap: 1rem;
    text-align: center;
  }

  .dashboard-title {
    font-size: 1.5rem;
  }

  .card-header {
    padding: 1rem 1rem 0.5rem 1rem;
    gap: 0.75rem;
  }

  .card-content {
    padding: 0 1rem 1rem 1rem;
  }

  .info-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.25rem;
    padding: 0.75rem 0;
  }

  .info-value {
    max-width: 100%;
    text-align: left;
    font-size: 0.875rem;
  }

  .usage-section {
    margin-top: 0.75rem;
    padding-top: 0.75rem;
  }

  .loading-container {
    min-height: 300px;
  }

  .error-content {
    padding: 1.5rem;
    margin: 0 0.5rem;
  }
}
</style>