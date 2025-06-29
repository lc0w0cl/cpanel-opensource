<script setup lang="ts">
import { ref } from 'vue'

// 应用认证中间件
definePageMeta({
  middleware: 'auth',
  layout: 'dashboard',
  pageTransition: { name: 'page'}
})

const { updateFavicon, currentFavicon } = useFavicon()

// 测试图标列表
const testIcons = [
  { name: '默认', url: '' },
  { name: 'Vue.js', url: 'https://vuejs.org/logo.svg' },
  { name: 'Nuxt.js', url: 'https://nuxt.com/icon.png' },
  { name: 'GitHub', url: 'https://github.com/favicon.ico' },
]

const testFavicon = (iconUrl: string) => {
  updateFavicon(iconUrl)
}
</script>

<template>
  <NuxtLayout>
    <div class="favicon-test-container">
      <div class="page-header">
        <h1 class="page-title">Favicon 测试页面</h1>
        <p class="page-description">测试动态favicon更新功能</p>
      </div>

      <div class="test-content">
        <div class="current-favicon">
          <h2>当前Favicon</h2>
          <p>{{ currentFavicon || '默认favicon' }}</p>
        </div>

        <div class="test-buttons">
          <h2>测试图标</h2>
          <div class="button-grid">
            <button
              v-for="icon in testIcons"
              :key="icon.name"
              @click="testFavicon(icon.url)"
              class="test-btn"
            >
              {{ icon.name }}
            </button>
          </div>
        </div>

        <div class="instructions">
          <h2>使用说明</h2>
          <ul>
            <li>点击上方按钮测试不同的favicon</li>
            <li>查看浏览器标签页图标的变化</li>
            <li>在系统设置中上传自定义logo后，favicon会自动更新</li>
            <li>重置logo后，favicon会恢复为默认图标</li>
          </ul>
        </div>
      </div>
    </div>
  </NuxtLayout>
</template>

<style scoped>
.favicon-test-container {
  padding: 2rem;
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 3rem;
}

.page-title {
  font-size: 2rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 0.5rem;
}

.page-description {
  font-size: 1rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

.test-content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.current-favicon,
.test-buttons,
.instructions {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 1rem;
  padding: 1.5rem;
}

.current-favicon h2,
.test-buttons h2,
.instructions h2 {
  font-size: 1.25rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 1rem;
}

.current-favicon p {
  color: rgba(255, 255, 255, 0.7);
  font-family: monospace;
  background: rgba(0, 0, 0, 0.2);
  padding: 0.5rem;
  border-radius: 0.5rem;
  word-break: break-all;
}

.button-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 1rem;
}

.test-btn {
  padding: 0.75rem 1rem;
  border: none;
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.2) 0%,
    rgba(59, 130, 246, 0.1) 100%
  );
  color: rgba(59, 130, 246, 0.9);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.test-btn:hover {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.3) 0%,
    rgba(59, 130, 246, 0.15) 100%
  );
  border-color: rgba(59, 130, 246, 0.5);
  transform: translateY(-1px);
}

.instructions ul {
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.6;
}

.instructions li {
  margin-bottom: 0.5rem;
}

@media (max-width: 768px) {
  .favicon-test-container {
    padding: 1rem;
  }
  
  .button-grid {
    grid-template-columns: 1fr;
  }
}
</style>
