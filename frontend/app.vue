<script setup>
import { onMounted, onUnmounted } from 'vue'

// 使用favicon composable
const { initFavicon, cleanupFavicon } = useFavicon()

// 使用壁纸composable
const { cleanupWallpaper } = useWallpaper()

onMounted(() => {
  // 初始化favicon功能
  initFavicon()
})

onUnmounted(() => {
  // 清理favicon功能
  cleanupFavicon()

  // 清理壁纸功能
  cleanupWallpaper()
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