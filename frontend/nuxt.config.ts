// https://nuxt.com/docs/api/configuration/nuxt-config
import tailwindcss from "@tailwindcss/vite";

export default defineNuxtConfig({
  app:{
    head:{
      link: [
        { rel: 'icon', type: 'image/png', href: '/favicon.svg' }
      ]
    }
  },
  compatibilityDate: '2025-05-15',
  devtools: { enabled: true },
  modules: ['motion-v/nuxt'],
  css: ['~/assets/css/main.css'],

  // 静态生成配置
  nitro: {
    prerender: {
      routes: ['/login', '/home', '/home/navigation-panel', '/home/settings', '/home/about']
    }
  },

  // SPA模式配置 - 解决路由问题
  ssr: false, // 禁用服务端渲染，使用SPA模式

  // 生成配置
  generate: {
    fallback: true // 生成404.html作为fallback页面
  },

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
