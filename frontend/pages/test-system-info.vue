<script setup lang="ts">
import { ref } from 'vue'
import { useSystemInfo } from '~/composables/useSystemInfo'

// 测试数据
const testData = ref({
  cpu: {
    model: "Intel(R) Core(TM) i7-10700K CPU @ 3.80GHz",
    cores: 8,
    logicalProcessors: 16,
    usage: 25.67,
    frequency: 3800000000
  },
  memory: {
    total: 32.0,
    used: 16.45,
    available: 15.55,
    usage: 51.41
  },
  disks: [
    {
      name: "C:\\",
      fileSystem: "NTFS",
      total: 931.51,
      used: 456.78,
      available: 474.73,
      usage: 49.03
    },
    {
      name: "D:\\",
      fileSystem: "NTFS",
      total: 1863.01,
      used: 1200.50,
      available: 662.51,
      usage: 64.44
    }
  ],
  system: {
    osName: "Windows",
    osVersion: "Windows 11 version 10.0.22000",
    architecture: "amd64",
    hostname: "DESKTOP-ABC123",
    uptime: 86400
  }
})

const {
  formatUptime,
  formatStorage,
  formatFrequency,
  getUsageColor,
  getUsageBgColor
} = useSystemInfo()
</script>

<template>
  <div class="test-container">
    <h1>系统信息测试页面</h1>
    
    <div class="test-section">
      <h2>格式化函数测试</h2>
      <div class="test-item">
        <strong>formatUptime(86400):</strong> {{ formatUptime(86400) }}
      </div>
      <div class="test-item">
        <strong>formatStorage(931.51):</strong> {{ formatStorage(931.51) }}
      </div>
      <div class="test-item">
        <strong>formatStorage(2048):</strong> {{ formatStorage(2048) }}
      </div>
      <div class="test-item">
        <strong>formatFrequency(3800000000):</strong> {{ formatFrequency(3800000000) }}
      </div>
    </div>

    <div class="test-section">
      <h2>使用率颜色测试</h2>
      <div class="test-item">
        <strong>getUsageColor(25):</strong> <span :class="getUsageColor(25)">{{ getUsageColor(25) }}</span>
      </div>
      <div class="test-item">
        <strong>getUsageColor(55):</strong> <span :class="getUsageColor(55)">{{ getUsageColor(55) }}</span>
      </div>
      <div class="test-item">
        <strong>getUsageColor(75):</strong> <span :class="getUsageColor(75)">{{ getUsageColor(75) }}</span>
      </div>
      <div class="test-item">
        <strong>getUsageColor(95):</strong> <span :class="getUsageColor(95)">{{ getUsageColor(95) }}</span>
      </div>
    </div>

    <div class="test-section">
      <h2>测试数据预览</h2>
      <pre>{{ JSON.stringify(testData, null, 2) }}</pre>
    </div>
  </div>
</template>

<style scoped>
.test-container {
  padding: 2rem;
  max-width: 800px;
  margin: 0 auto;
  color: white;
}

.test-section {
  margin-bottom: 2rem;
  padding: 1rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.5rem;
  background: rgba(255, 255, 255, 0.05);
}

.test-item {
  margin-bottom: 0.5rem;
  padding: 0.5rem;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 0.25rem;
}

pre {
  background: rgba(0, 0, 0, 0.3);
  padding: 1rem;
  border-radius: 0.5rem;
  overflow-x: auto;
  font-size: 0.875rem;
}

h1, h2 {
  color: rgba(255, 255, 255, 0.9);
}

/* 使用率颜色类 */
.text-green-400 { color: rgb(74, 222, 128); }
.text-blue-400 { color: rgb(96, 165, 250); }
.text-yellow-400 { color: rgb(251, 191, 36); }
.text-red-400 { color: rgb(248, 113, 113); }
</style>
