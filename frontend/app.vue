<script setup>
import { onMounted, onUnmounted } from 'vue'

// 使用favicon composable
const { initFavicon, cleanupFavicon } = useFavicon()

onMounted(() => {
  // 初始化favicon功能
  initFavicon()
})

onUnmounted(() => {
  // 清理favicon功能
  cleanupFavicon()
})
</script>

<template>
  <div>
    <NuxtPage />
  </div>
</template>
<style>
.page-enter-active,
.page-leave-active {
  transition: all 0.4s;
}
.page-enter-from,
.page-leave-to {
  opacity: 0;
  filter: blur(1rem);
}
</style>