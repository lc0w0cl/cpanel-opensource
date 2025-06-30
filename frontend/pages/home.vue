<script setup lang="ts">
// 页面元数据配置
definePageMeta({
  layout: 'dashboard',
  middleware: 'auth'
})
</script>

<template>
  <div class="home-container">
    <!-- 侧边栏 -->
    <Sidebar class="sidebar-component" />

    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 子路由内容 -->
      <NuxtPage />
    </div>
  </div>
</template>

<style scoped>
/* 主容器布局 */
.home-container {
  display: flex;
  width: 100%;
  min-height: 100vh;
}

.sidebar-component {
  height: 100vh;
  position: sticky;
  top: 0;
}

.main-content {
  flex: 1;
  overflow-y: auto;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .home-container {
    flex-direction: column;
  }

  .sidebar-component {
    height: auto;
    position: relative;
    width: 100%;
  }

  .main-content {
    overflow-y: visible;
  }
}
</style>
