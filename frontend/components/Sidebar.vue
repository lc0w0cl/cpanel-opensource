<template>
  <div class="sidebar-container">
    <div class="sidebar-buttons">
      <NuxtLink to="/" class="nav-button">
        <HomeIcon class="icon" />
        <span class="button-text">首页</span>
      </NuxtLink>
      <NuxtLink to="/navigation-panel" class="nav-button">
        <Squares2X2Icon class="icon" />
        <span class="button-text">导航</span>
      </NuxtLink>
      <NuxtLink to="/settings" class="nav-button">
        <Squares2X2Icon class="icon" />
        <span class="button-text">设置</span>
      </NuxtLink>
      <NuxtLink to="/about" class="nav-button">
        <InformationCircleIcon class="icon" />
        <span class="button-text">关于</span>
      </NuxtLink>

      <!-- 登出按钮 -->
      <button
        @click="handleLogout"
        class="nav-button logout-button"
        :disabled="isLoggingOut"
      >
        <ArrowRightOnRectangleIcon v-if="!isLoggingOut" class="icon" />
        <div v-else class="loading-spinner"></div>
        <span class="button-text">{{ isLoggingOut ? '登出中...' : '登出' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { HomeIcon, Squares2X2Icon, InformationCircleIcon, ArrowRightOnRectangleIcon } from '@heroicons/vue/24/outline'

const isLoggingOut = ref(false)

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
  justify-content: center;
}

.sidebar-buttons {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.nav-button {
  width: 6rem;
  height: 4.5rem;
  border-radius: 1.2rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.3rem;
  position: relative;
  overflow: hidden;

  /* 苹果液态玻璃效果 */
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.2) 0%,
    rgba(255, 255, 255, 0.1) 50%,
    rgba(255, 255, 255, 0.05) 100%
  );
  backdrop-filter: blur(15px) saturate(150%);
  -webkit-backdrop-filter: blur(15px) saturate(150%);

  /* 渐变边框 */
  border: 1px solid transparent;
  background-clip: padding-box;

  /* 多层阴影 */
  box-shadow:
    0 6px 20px rgba(0, 0, 0, 0.15),
    0 2px 6px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.3),
    inset 0 -1px 0 rgba(0, 0, 0, 0.1);

  color: white;
  font-weight: 600;
  font-size: 0.9rem;
  text-decoration: none;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 按钮边框渐变效果 */
.nav-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 1.2rem;
  padding: 1px;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.4) 0%,
    rgba(255, 255, 255, 0.2) 50%,
    rgba(255, 255, 255, 0.1) 100%
  );
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask-composite: xor;
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  pointer-events: none;
}

.nav-button:hover {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.25) 0%,
    rgba(255, 255, 255, 0.15) 50%,
    rgba(255, 255, 255, 0.08) 100%
  );
  transform: translateY(-3px) scale(1.02);
  box-shadow:
    0 12px 30px rgba(0, 0, 0, 0.2),
    0 4px 12px rgba(0, 0, 0, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.4),
    inset 0 -1px 0 rgba(0, 0, 0, 0.1);
  color: #fbbf24;
}

.nav-button:hover .icon {
  transform: scale(1.15) rotate(5deg);
  color: #fbbf24;
  filter: drop-shadow(0 2px 4px rgba(251, 191, 36, 0.3));
}

/* 激活状态 */
.nav-button:active {
  transform: translateY(-1px) scale(0.98);
  transition: all 0.1s ease;
}

.icon {
  width: 1.6rem;
  height: 1.6rem;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.2));
}

.button-text {
  font-weight: 600;
  font-size: 0.75rem;
  letter-spacing: 0.025em;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
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
  color: #ef4444;
}

.logout-button:hover .icon {
  color: #ef4444;
  filter: drop-shadow(0 2px 4px rgba(239, 68, 68, 0.3));
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
    border-radius: 1rem;
  }
}
</style>