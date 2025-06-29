<script setup lang="ts">
// 页面元数据配置
definePageMeta({
  layout: 'dashboard',
  middleware: 'auth'
})
</script>

<template>
  <!-- 子路由内容 -->
  <NuxtPage />
</template>
