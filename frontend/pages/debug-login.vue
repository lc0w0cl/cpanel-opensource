<template>
  <div class="debug-container">
    <h1>登录调试页面</h1>
    
    <div class="debug-section">
      <h2>1. 测试基本登录</h2>
      <button @click="testBasicLogin" :disabled="loading">
        {{ loading ? '测试中...' : '测试登录' }}
      </button>
      
      <div v-if="result" class="result">
        <h3>结果:</h3>
        <pre>{{ JSON.stringify(result, null, 2) }}</pre>
      </div>
      
      <div v-if="error" class="error">
        <h3>错误:</h3>
        <pre>{{ error }}</pre>
      </div>
    </div>
    
    <div class="debug-section">
      <h2>2. 检查JWT工具函数</h2>
      <button @click="testJwtFunctions">测试JWT函数</button>
      
      <div v-if="jwtTest" class="result">
        <h3>JWT函数测试:</h3>
        <pre>{{ JSON.stringify(jwtTest, null, 2) }}</pre>
      </div>
    </div>
    
    <div class="debug-section">
      <h2>3. 检查配置</h2>
      <button @click="checkConfig">检查配置</button>
      
      <div v-if="configTest" class="result">
        <h3>配置信息:</h3>
        <pre>{{ JSON.stringify(configTest, null, 2) }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

// 定义页面元数据
definePageMeta({
  layout: 'blank'
})

const loading = ref(false)
const result = ref(null)
const error = ref('')
const jwtTest = ref(null)
const configTest = ref(null)

// 测试基本登录
const testBasicLogin = async () => {
  loading.value = true
  result.value = null
  error.value = ''
  
  try {
    const config = useRuntimeConfig()
    const API_BASE_URL = `${config.public.apiBaseUrl}/api`
    
    console.log('API_BASE_URL:', API_BASE_URL)
    
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        password: 'admin'
      })
    })
    
    console.log('Response status:', response.status)
    console.log('Response headers:', Object.fromEntries(response.headers.entries()))
    
    const data = await response.json()
    console.log('Response data:', data)
    
    result.value = {
      status: response.status,
      headers: Object.fromEntries(response.headers.entries()),
      data: data
    }
    
  } catch (err: any) {
    console.error('Login test error:', err)
    error.value = err.message || err.toString()
  } finally {
    loading.value = false
  }
}

// 测试JWT函数
const testJwtFunctions = async () => {
  try {
    // 动态导入JWT函数
    const jwtModule = await import('~/composables/useJwt')
    
    jwtTest.value = {
      hasTokens: typeof jwtModule.hasTokens,
      setTokens: typeof jwtModule.setTokens,
      apiRequest: typeof jwtModule.apiRequest,
      clearTokens: typeof jwtModule.clearTokens,
      currentTokens: {
        hasTokens: jwtModule.hasTokens(),
        accessToken: jwtModule.getAccessToken() ? 'exists' : 'null',
        refreshToken: jwtModule.getRefreshToken() ? 'exists' : 'null'
      }
    }
  } catch (err: any) {
    jwtTest.value = {
      error: err.message || err.toString()
    }
  }
}

// 检查配置
const checkConfig = () => {
  try {
    const config = useRuntimeConfig()
    
    configTest.value = {
      apiBaseUrl: config.public.apiBaseUrl,
      environment: process.env.NODE_ENV,
      client: process.client,
      server: process.server,
      currentUrl: process.client ? window.location.href : 'server-side'
    }
  } catch (err: any) {
    configTest.value = {
      error: err.message || err.toString()
    }
  }
}
</script>

<style scoped>
.debug-container {
  padding: 2rem;
  max-width: 800px;
  margin: 0 auto;
  font-family: monospace;
}

.debug-section {
  margin-bottom: 2rem;
  padding: 1rem;
  border: 1px solid #ccc;
  border-radius: 8px;
}

button {
  padding: 0.5rem 1rem;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.result {
  margin-top: 1rem;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 4px;
}

.error {
  margin-top: 1rem;
  padding: 1rem;
  background: #f8d7da;
  border: 1px solid #f5c6cb;
  border-radius: 4px;
  color: #721c24;
}

pre {
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
