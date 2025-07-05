<template>
  <div class="sidebar-container">
    <!-- Logo区域 -->
    <div class="logo-section">
      <div class="logo-container">
        <!-- 加载状态 -->
        <div v-if="!logoLoaded" class="logo-loading">
          <Icon icon="mdi:loading" class="loading-icon animate-spin" />
        </div>
        <!-- Logo加载完成后显示 -->
        <template v-else>
          <LiquidLogo
            v-if="currentLogo && !logoError"
            :image-url="getLogoUrl(currentLogo)"
            :show-processing="false"
            @error="logoError = true"
          />
          <div v-else class="default-logo">
            <Icon icon="mdi:view-dashboard" class="default-logo-icon" />
          </div>
        </template>
      </div>
    </div>

    <div class="sidebar-buttons">
      <NuxtLink to="/home" class="nav-button">
        <Icon icon="material-symbols:home-outline" class="icon" />
        <span class="button-text">首页</span>
      </NuxtLink>
      <NuxtLink to="/home/navigation-panel" class="nav-button">
        <Icon icon="material-symbols:dashboard-outline" class="icon" />
        <span class="button-text">导航</span>
      </NuxtLink>
      <NuxtLink to="/home/todo" class="nav-button">
        <Icon icon="material-symbols:event-note" class="icon" />
        <span class="button-text">TODO</span>
      </NuxtLink>
      <NuxtLink to="/home/music" class="nav-button">
        <Icon icon="material-symbols:music-video-outline" class="icon" />
        <span class="button-text">音乐</span>
      </NuxtLink>
      <NuxtLink to="/home/settings" class="nav-button">
        <Icon icon="material-symbols:settings-outline" class="icon" />
        <span class="button-text">设置</span>
      </NuxtLink>


      <!-- 登出按钮 -->
      <button
        @click="handleLogout"
        class="nav-button logout-button"
        :disabled="isLoggingOut"
      >
        <Icon v-if="!isLoggingOut" icon="material-symbols:logout" class="icon" />
        <div v-else class="loading-spinner"></div>
        <span class="button-text">{{ isLoggingOut ? '登出中...' : '登出' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { Icon } from '@iconify/vue'
import LiquidLogo from "~/components/inspira/liquid_logo/LiquidLogo.vue";

const isLoggingOut = ref(false)
const currentLogo = ref('')
const logoLoaded = ref(false) // 添加logo加载状态
const logoError = ref(false) // 添加logo错误状态

// 获取logo URL的辅助函数
const getLogoUrl = (logoPath) => {
  if (!logoPath) return ''

  // 如果是完整URL，直接返回
  if (logoPath.startsWith('http') || logoPath.startsWith('data:')) {
    return logoPath
  }

  // 如果是相对路径，需要拼接API基础URL
  const config = useRuntimeConfig()
  return `${config.public.apiBaseUrl}${logoPath}`
}

// 加载logo配置
const loadLogo = async () => {
  try {
    // 重置错误状态
    logoError.value = false

    const config = useRuntimeConfig()
    const API_BASE_URL = `${config.public.apiBaseUrl}/api`

    const response = await apiRequest(`${API_BASE_URL}/system-config/logo`)
    const result = await response.json()

    if (result.success) {
      currentLogo.value = result.data.logoUrl || ''
    }
  } catch (error) {
    console.error('加载Logo失败:', error)
  } finally {
    // 无论成功还是失败，都标记为已加载
    logoLoaded.value = true
  }
}

// 监听logo变更事件
const handleLogoChanged = (event) => {
  currentLogo.value = event.detail.logoUrl || ''
  // 重置错误状态
  logoError.value = false
  // 确保logo已加载状态为true，这样新logo能立即显示
  logoLoaded.value = true
}

onMounted(async () => {
  // 等待组件完全挂载后再加载logo
  await nextTick()
  await loadLogo()

  if (process.client) {
    window.addEventListener('logoChanged', handleLogoChanged)
  }
})

onUnmounted(() => {
  if (process.client) {
    window.removeEventListener('logoChanged', handleLogoChanged)
  }
})

// 登出处理函数
const handleLogout = async () => {
  if (isLoggingOut.value) return

  isLoggingOut.value = true

  try {
    // 使用新的认证管理函数
    const { logout } = await import('~/composables/useAuth')
    await logout()

    console.log('登出成功')

    // 重定向到登录页面
    await navigateTo('/login')

  } catch (error) {
    console.error('登出失败:', error)
  } finally {
    isLoggingOut.value = false
  }
}
</script>

<style scoped>
.sidebar-container {
  padding: 1.5rem;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  /* 移除背景，让壁纸透过 */
  background: transparent;
}

/* Logo区域样式 */
.logo-section {
  margin-bottom: 2rem;
}

.logo-container {
  width: 4rem;
  height: 4rem;
  border-radius: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 50%,
    rgba(255, 255, 255, 0.05) 100%
  );
  backdrop-filter: blur(10px) saturate(120%);
  -webkit-backdrop-filter: blur(10px) saturate(120%);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow:
    0 4px 15px rgba(0, 0, 0, 0.1),
    0 1px 4px rgba(0, 0, 0, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.logo-container:hover {
  transform: translateY(-2px);
  box-shadow:
    0 6px 20px rgba(0, 0, 0, 0.15),
    0 2px 6px rgba(0, 0, 0, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.25);
}

.logo-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 0.75rem;
}

.default-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.default-logo-icon {
  width: 2rem;
  height: 2rem;
  color: rgba(255, 255, 255, 0.8);
}

/* Logo加载状态样式 */
.logo-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.loading-icon {
  width: 1.5rem;
  height: 1.5rem;
  color: rgba(255, 255, 255, 0.6);
}

/* 旋转动画 */
.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.sidebar-buttons {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.nav-button {
  width: 6rem;
  height: 4.5rem;
  border-radius: 0.75rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.3rem;
  position: relative;
  overflow: hidden;

  /* 简洁的边框样式，与其他页面保持一致 */
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: transparent;

  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
  font-size: 0.9rem;
  text-decoration: none;
  transition: all 0.3s ease;
}

.nav-button:hover {
  border-color: rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.9);
}

.nav-button:hover .icon {
  color: rgba(255, 255, 255, 0.9);
}

/* 激活状态 */
.nav-button:active {
  transform: scale(0.98);
  transition: all 0.1s ease;
}

.icon {
  width: 1.6rem;
  height: 1.6rem;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.3s ease;
}

.button-text {
  font-weight: 500;
  font-size: 0.75rem;
  letter-spacing: 0.025em;
  transition: all 0.3s ease;
}

/* 登出按钮特殊样式 */
.logout-button {
  border: none;
  cursor: pointer;
}

.logout-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.logout-button:hover {
  color: rgba(239, 68, 68, 0.9);
  border-color: rgba(239, 68, 68, 0.3);
}

.logout-button:hover .icon {
  color: rgba(239, 68, 68, 0.9);
}

/* 加载动画 */
.loading-spinner {
  width: 1.6rem;
  height: 1.6rem;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid #ffffff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .sidebar-buttons {
    flex-direction: row;
    gap: 1rem;
  }

  .sidebar-container {
    padding: 1rem;
  }

  .nav-button {
    width: 5rem;
    height: 4rem;
    border-radius: 0.75rem;
  }
}
</style>