<template>
  <div class="debug-page">
    <h1>2FA API 调试页面</h1>
    
    <div class="debug-section">
      <h2>API 连接测试</h2>
      <button @click="testApiConnection" class="test-btn">测试API连接</button>
      <div v-if="apiTestResult" class="result">
        <h3>测试结果:</h3>
        <pre>{{ apiTestResult }}</pre>
      </div>
    </div>

    <div class="debug-section">
      <h2>2FA状态测试</h2>
      <button @click="test2FAStatus" class="test-btn">测试2FA状态</button>
      <div v-if="statusTestResult" class="result">
        <h3>状态测试结果:</h3>
        <pre>{{ statusTestResult }}</pre>
      </div>
    </div>

    <div class="debug-section">
      <h2>环境信息</h2>
      <div class="env-info">
        <p><strong>API Base URL:</strong> {{ apiBaseUrl }}</p>
        <p><strong>当前环境:</strong> {{ isDevelopment ? '开发环境' : '生产环境' }}</p>
        <p><strong>当前页面URL:</strong> {{ currentUrl }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

// 响应式数据
const apiTestResult = ref('')
const statusTestResult = ref('')
const apiBaseUrl = ref('')
const isDevelopment = ref(false)
const currentUrl = ref('')

// 获取配置
const config = useRuntimeConfig()

onMounted(() => {
  apiBaseUrl.value = config.public.apiBaseUrl
  isDevelopment.value = config.public.isDevelopment
  currentUrl.value = window.location.href
})

// 测试API连接
const testApiConnection = async () => {
  try {
    apiTestResult.value = '正在测试...'
    
    const testUrl = `${config.public.apiBaseUrl}/api/system/info`
    console.log('测试URL:', testUrl)
    
    const response = await fetch(testUrl, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    console.log('响应状态:', response.status)
    console.log('响应头:', Object.fromEntries(response.headers.entries()))
    
    if (response.ok) {
      const data = await response.json()
      apiTestResult.value = JSON.stringify({
        status: 'SUCCESS',
        statusCode: response.status,
        data: data
      }, null, 2)
    } else {
      const text = await response.text()
      apiTestResult.value = JSON.stringify({
        status: 'ERROR',
        statusCode: response.status,
        statusText: response.statusText,
        responseText: text.substring(0, 500) + (text.length > 500 ? '...' : '')
      }, null, 2)
    }
  } catch (error) {
    console.error('API测试失败:', error)
    apiTestResult.value = JSON.stringify({
      status: 'EXCEPTION',
      error: error instanceof Error ? error.message : String(error),
      stack: error instanceof Error ? error.stack : undefined
    }, null, 2)
  }
}

// 测试2FA状态
const test2FAStatus = async () => {
  try {
    statusTestResult.value = '正在测试...'
    
    const testUrl = `${config.public.apiBaseUrl}/api/2fa/status`
    console.log('测试2FA状态URL:', testUrl)
    
    // 先测试不带认证的请求
    const response = await fetch(testUrl, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    console.log('2FA状态响应状态:', response.status)
    console.log('2FA状态响应头:', Object.fromEntries(response.headers.entries()))
    
    if (response.ok) {
      const data = await response.json()
      statusTestResult.value = JSON.stringify({
        status: 'SUCCESS',
        statusCode: response.status,
        data: data
      }, null, 2)
    } else {
      const text = await response.text()
      statusTestResult.value = JSON.stringify({
        status: 'ERROR',
        statusCode: response.status,
        statusText: response.statusText,
        responseText: text.substring(0, 500) + (text.length > 500 ? '...' : ''),
        isHtml: text.includes('<!DOCTYPE')
      }, null, 2)
    }
  } catch (error) {
    console.error('2FA状态测试失败:', error)
    statusTestResult.value = JSON.stringify({
      status: 'EXCEPTION',
      error: error instanceof Error ? error.message : String(error),
      stack: error instanceof Error ? error.stack : undefined
    }, null, 2)
  }
}
</script>

<style scoped>
.debug-page {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
  font-family: monospace;
}

.debug-section {
  margin-bottom: 2rem;
  padding: 1rem;
  border: 1px solid #ccc;
  border-radius: 0.5rem;
  background: #f9f9f9;
}

.test-btn {
  padding: 0.5rem 1rem;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 0.25rem;
  cursor: pointer;
  margin-bottom: 1rem;
}

.test-btn:hover {
  background: #0056b3;
}

.result {
  margin-top: 1rem;
  padding: 1rem;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 0.25rem;
}

.result pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-size: 0.9rem;
  line-height: 1.4;
}

.env-info {
  background: #fff;
  padding: 1rem;
  border-radius: 0.25rem;
  border: 1px solid #ddd;
}

.env-info p {
  margin: 0.5rem 0;
}

h1 {
  color: #333;
  margin-bottom: 2rem;
}

h2 {
  color: #555;
  margin-bottom: 1rem;
}

h3 {
  color: #666;
  margin-bottom: 0.5rem;
}
</style>
