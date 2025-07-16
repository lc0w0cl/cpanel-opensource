<template>
  <div class="test-page">
    <h1 class="text-2xl font-bold mb-4">布局测试页面</h1>
    
    <div class="space-y-4">
      <div class="bg-white/10 backdrop-blur-sm rounded-lg p-4">
        <h2 class="text-lg font-semibold mb-2">壁纸状态</h2>
        <div class="space-y-2 text-sm">
          <div>初始化状态: {{ isInitialized ? '已初始化' : '未初始化' }}</div>
          <div>加载状态: {{ isLoading ? '加载中' : '已完成' }}</div>
          <div>内容边距: {{ contentPadding }}px</div>
          <div>背景图片: {{ backgroundImageUrl ? '已设置' : '默认' }}</div>
        </div>
      </div>
      
      <div class="bg-white/10 backdrop-blur-sm rounded-lg p-4">
        <h2 class="text-lg font-semibold mb-2">测试内容</h2>
        <p class="text-sm opacity-80">
          这是一个测试页面，用于验证页面刷新时边距加载的平滑性。
          刷新页面观察边距是否会突然跳跃。
        </p>
      </div>
      
      <div class="bg-white/10 backdrop-blur-sm rounded-lg p-4">
        <h2 class="text-lg font-semibold mb-2">操作按钮</h2>
        <div class="space-x-2">
          <button 
            @click="refreshPage" 
            class="px-4 py-2 bg-blue-500/20 hover:bg-blue-500/30 rounded-lg transition-colors"
          >
            刷新页面
          </button>
          <button 
            @click="togglePadding" 
            class="px-4 py-2 bg-green-500/20 hover:bg-green-500/30 rounded-lg transition-colors"
          >
            切换边距
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 使用布局
definePageMeta({
  layout: 'dashboard'
})

// 获取壁纸状态
const {
  isInitialized,
  isLoading,
  contentPadding,
  backgroundImageUrl
} = useWallpaper()

// 刷新页面
const refreshPage = () => {
  window.location.reload()
}

// 切换边距（用于测试过渡效果）
const togglePadding = () => {
  // 触发内容边距变更事件
  const newPadding = contentPadding.value === 16 ? 32 : 16
  window.dispatchEvent(new CustomEvent('content-padding-changed', {
    detail: { padding: newPadding }
  }))
}
</script>

<style scoped>
.test-page {
  max-width: 800px;
  margin: 0 auto;
}
</style>
