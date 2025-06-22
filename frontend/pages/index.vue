<script setup lang="ts">
  import { Motion } from "motion-v";

   definePageMeta({
     layout: 'dashboard',
     middleware: 'auth'
   })
</script>

<template>
 <NuxtLayout>
    <h1>首页</h1>
 </NuxtLayout>
</template>

<style>

</style>