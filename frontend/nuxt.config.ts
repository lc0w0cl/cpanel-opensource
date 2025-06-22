// https://nuxt.com/docs/api/configuration/nuxt-config
import tailwindcss from "@tailwindcss/vite";

export default defineNuxtConfig({
  compatibilityDate: '2025-05-15',
  devtools: { enabled: true },
  modules: ['motion-v/nuxt'],
  css: ['~/assets/css/main.css'],
  vite: {
    plugins: [
      tailwindcss(),
    ],
  },
  debug: true,
  runtimeConfig: {
    public: {
      // 后端API基础URL
      apiBaseUrl: process.env.NODE_ENV === 'development'
        ? 'http://localhost:8080'
        : '', // 生产环境使用相对路径
      // 是否为开发环境
      isDevelopment: process.env.NODE_ENV === 'development'
    }
  }
})
