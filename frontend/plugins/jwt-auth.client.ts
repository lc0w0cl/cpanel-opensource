/**
 * JWT认证插件 - 客户端
 * 提供全局的JWT工具函数
 */

export default defineNuxtPlugin(() => {
  // 将JWT工具函数注册为全局可用
  return {
    provide: {
      // 这些函数已经在 composables/useJwt.ts 中定义
      // 这里只是确保它们在全局范围内可用
    }
  }
})
