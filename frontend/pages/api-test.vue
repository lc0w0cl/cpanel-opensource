<template>
  <div class="api-test-page">
    <h1>API 连接测试</h1>
    
    <div class="test-section">
      <h2>分类 API 测试</h2>
      <button @click="testCategoriesAPI" :disabled="loading.categories">
        {{ loading.categories ? '测试中...' : '测试分类 API' }}
      </button>
      <div v-if="results.categories" class="result">
        <h3>分类数据:</h3>
        <pre>{{ JSON.stringify(results.categories, null, 2) }}</pre>
      </div>
      <div v-if="errors.categories" class="error">
        <h3>错误:</h3>
        <pre>{{ errors.categories }}</pre>
      </div>
    </div>

    <div class="test-section">
      <h2>导航项 API 测试</h2>
      <button @click="testNavigationItemsAPI" :disabled="loading.navigationItems">
        {{ loading.navigationItems ? '测试中...' : '测试导航项 API' }}
      </button>
      <div v-if="results.navigationItems" class="result">
        <h3>导航项数据:</h3>
        <pre>{{ JSON.stringify(results.navigationItems, null, 2) }}</pre>
      </div>
      <div v-if="errors.navigationItems" class="error">
        <h3>错误:</h3>
        <pre>{{ errors.navigationItems }}</pre>
      </div>
    </div>

    <div class="test-section">
      <h2>创建测试数据</h2>
      <button @click="createTestData" :disabled="loading.createTest">
        {{ loading.createTest ? '创建中...' : '创建测试分类和导航项' }}
      </button>
      <div v-if="results.createTest" class="result">
        <h3>创建结果:</h3>
        <pre>{{ JSON.stringify(results.createTest, null, 2) }}</pre>
      </div>
      <div v-if="errors.createTest" class="error">
        <h3>错误:</h3>
        <pre>{{ errors.createTest }}</pre>
      </div>
    </div>

    <div class="test-section">
      <h2>排序功能测试</h2>
      <button @click="testSortFunction" :disabled="loading.sortTest">
        {{ loading.sortTest ? '测试中...' : '测试排序功能' }}
      </button>
      <div v-if="results.sortTest" class="result">
        <h3>排序测试结果:</h3>
        <pre>{{ JSON.stringify(results.sortTest, null, 2) }}</pre>
      </div>
      <div v-if="errors.sortTest" class="error">
        <h3>错误:</h3>
        <pre>{{ errors.sortTest }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const API_BASE_URL = 'http://localhost:8080/api'

const loading = ref({
  categories: false,
  navigationItems: false,
  createTest: false,
  sortTest: false
})

const results = ref({
  categories: null as any,
  navigationItems: null as any,
  createTest: null as any,
  sortTest: null as any
})

const errors = ref({
  categories: null as any,
  navigationItems: null as any,
  createTest: null as any,
  sortTest: null as any
})

const testCategoriesAPI = async () => {
  loading.value.categories = true
  errors.value.categories = null
  results.value.categories = null
  
  try {
    const response = await fetch(`${API_BASE_URL}/categories`)
    const data = await response.json()
    results.value.categories = data
  } catch (error) {
    errors.value.categories = error.message
  } finally {
    loading.value.categories = false
  }
}

const testNavigationItemsAPI = async () => {
  loading.value.navigationItems = true
  errors.value.navigationItems = null
  results.value.navigationItems = null
  
  try {
    const response = await fetch(`${API_BASE_URL}/navigation-items`)
    const data = await response.json()
    results.value.navigationItems = data
  } catch (error) {
    errors.value.navigationItems = error.message
  } finally {
    loading.value.navigationItems = false
  }
}

const createTestData = async () => {
  loading.value.createTest = true
  errors.value.createTest = null
  results.value.createTest = null

  try {
    // 创建测试分类
    const categoryResponse = await fetch(`${API_BASE_URL}/categories`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: '测试分类',
        order: 1
      })
    })
    const categoryData = await categoryResponse.json()

    if (categoryData.success) {
      // 创建测试导航项
      const itemResponse = await fetch(`${API_BASE_URL}/navigation-items`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: '测试导航项',
          url: 'https://example.com',
          logo: '/logo/test.svg',
          categoryId: categoryData.data.id,
          description: '这是一个测试导航项'
        })
      })
      const itemData = await itemResponse.json()

      results.value.createTest = {
        category: categoryData,
        navigationItem: itemData
      }
    } else {
      errors.value.createTest = categoryData.message
    }
  } catch (error) {
    errors.value.createTest = error.message
  } finally {
    loading.value.createTest = false
  }
}

const testSortFunction = async () => {
  loading.value.sortTest = true
  errors.value.sortTest = null
  results.value.sortTest = null

  try {
    // 1. 获取导航项
    const itemsResponse = await fetch(`${API_BASE_URL}/navigation-items`)
    const itemsData = await itemsResponse.json()

    if (itemsData.success && itemsData.data.length > 0) {
      // 2. 取前几个项目进行排序测试
      const testItems = itemsData.data.slice(0, 3).map((item, index) => ({
        ...item,
        sortOrder: 3 - index // 反向排序
      }))

      // 3. 调用排序API
      const sortResponse = await fetch(`${API_BASE_URL}/navigation-items/sort`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(testItems)
      })
      const sortData = await sortResponse.json()

      // 4. 再次获取数据验证排序
      const verifyResponse = await fetch(`${API_BASE_URL}/navigation-items`)
      const verifyData = await verifyResponse.json()

      results.value.sortTest = {
        originalItems: itemsData.data.slice(0, 3),
        sortedItems: testItems,
        sortResult: sortData,
        verifyItems: verifyData.data.slice(0, 3)
      }
    } else {
      errors.value.sortTest = '没有找到导航项进行排序测试'
    }
  } catch (error) {
    errors.value.sortTest = error.message
  } finally {
    loading.value.sortTest = false
  }
}
</script>

<style scoped>
.api-test-page {
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

button {
  padding: 0.5rem 1rem;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 0.25rem;
  cursor: pointer;
  margin-bottom: 1rem;
}

button:disabled {
  background: #6b7280;
  cursor: not-allowed;
}

.result {
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.3);
  padding: 1rem;
  border-radius: 0.25rem;
  margin-top: 1rem;
}

.error {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  padding: 1rem;
  border-radius: 0.25rem;
  margin-top: 1rem;
}

pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-size: 0.875rem;
}
</style>
