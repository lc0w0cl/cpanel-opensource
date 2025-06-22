<script setup lang="ts">
    definePageMeta({
     layout: 'dashboard',
     middleware: 'auth'
   })
</script>

<template>
    <NuxtLayout>
    <section>
        <p>此页面将在 /about 路由下显示。</p>
    </section>
  </NuxtLayout>
</template>