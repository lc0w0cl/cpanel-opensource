<script setup lang="ts">
import { ref } from 'vue'
import { Icon } from '@iconify/vue'
import IconCloud from '~/components/inspira/IconCloud.vue';

// 定义页面元数据，使用空白布局
definePageMeta({
  layout: 'blank'
})

const slugs = [
  "typescript",
  "javascript",
  "dart",
  "java",
  "react",
  "flutter",
  "android",
  "html5",
  "css3",
  "nodedotjs",
  "express",
  "nextdotjs",
  "prisma",
  "amazonaws",
  "postgresql",
  "firebase",
  "nginx",
  "vercel",
  "testinglibrary",
  "jest",
  "cypress",
  "docker",
  "git",
  "jira",
  "github",
  "gitlab",
  "visualstudiocode",
  "androidstudio",
  "sonarqube",
  "figma",
];

const imageUrls = slugs.map((slug) => `https://cdn.simpleicons.org/${slug}/${slug}`);

// 添加关键CSS到头部，防止布局跳动
useHead({
  style: [{
    innerHTML: `
      .login-container {
        position: fixed !important;
        top: 0 !important;
        left: 0 !important;
        width: 100vw !important;
        height: 100vh !important;
        display: flex !important;
        align-items: center !important;
        justify-content: center !important;
        background-image: url('/background/机甲.png') !important;
        background-size: cover !important;
        background-position: center !important;
        overflow: hidden !important;
        z-index: 1000 !important;
      }
    `
  }]
})

// 导入JWT工具函数
import { hasTokens, setTokens, apiRequest } from '~/composables/useJwt'

// 响应式数据
const password = ref('')
const isLoading = ref(false)
const showPassword = ref(false)
const errorMessage = ref('')
const isClient = ref(false)

// 检查是否已登录（简化版本）
onMounted(async () => {
  // 标记为客户端环境
  isClient.value = true

  // 等待一个tick确保DOM完全渲染
  await nextTick()

  // 检查是否有有效的认证缓存
  if (process.client) {
    const cachedAuth = sessionStorage.getItem('auth_status')
    if (cachedAuth) {
      try {
        const authData = JSON.parse(cachedAuth)
        // 如果缓存显示已登录且在有效期内，直接跳转
        if (authData.authenticated && authData.timestamp &&
            (Date.now() - authData.timestamp < 300000)) { // 缓存5分钟
          console.log('检测到有效登录状态，跳转到首页')
          await navigateTo('/')
          return
        }
      } catch (e) {
        // 缓存数据无效，清除
        sessionStorage.removeItem('auth_status')
      }
    }
  }
})

// 登录处理函数
const handleLogin = async () => {
  if (!password.value.trim()) {
    errorMessage.value = '请输入密码'
    return
  }

  isLoading.value = true
  errorMessage.value = ''

  try {
    const config = useRuntimeConfig()
    const API_BASE_URL = `${config.public.apiBaseUrl}/api`

    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        password: password.value.trim()
      })
    })

    const result = await response.json()

    if (result.success && result.data) {
      // 存储JWT tokens
      setTokens(result.data)

      // 设置认证状态缓存
      if (process.client) {
        sessionStorage.setItem('auth_status', JSON.stringify({
          authenticated: true,
          timestamp: Date.now()
        }))
      }

      console.log('登录成功')

      // 登录成功后跳转到首页
      await navigateTo('/')
    } else {
      errorMessage.value = result.message || '登录失败'
    }
  } catch (error) {
    console.error('登录失败:', error)
    errorMessage.value = '网络错误，请稍后重试'
  } finally {
    isLoading.value = false
  }
}

// 切换密码显示/隐藏
const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}

// 键盘事件处理
const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter') {
    handleLogin()
  }
}
</script>

<template>
  <div class="login-container" :class="{ 'is-loaded': isClient }">
    <div class="login-content" v-show="isClient">
      <!-- 登录卡片 -->
      <div class="login-card">
        <!-- 标题区域 -->
        <div class="login-header">

          <div class="grid place-content-center">
            <IconCloud :images="imageUrls" />
          </div>
        </div>

        <!-- 表单区域 -->
        <form @submit.prevent="handleLogin" class="login-form">
          <!-- 错误消息 -->
          <div v-if="errorMessage" class="error-message">
            <Icon icon="mdi:alert-circle" class="error-icon" />
            <span>{{ errorMessage }}</span>
          </div>

          <div class="password-input-wrapper">
            <div class="input-icon">
              <Icon icon="mdi:lock" class="icon" />
            </div>
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              class="password-input"
              placeholder="请输入密码"
              @keydown="handleKeydown"
              :disabled="isLoading"
            />
            <button
              type="button"
              class="toggle-password-btn"
              @click="togglePasswordVisibility"
              :disabled="isLoading"
            >
              <Icon
                :icon="showPassword ? 'mdi:eye-off' : 'mdi:eye'"
                class="icon"
              />
            </button>
          </div>

          <button
            type="submit"
            class="login-button"
            :disabled="!password.trim() || isLoading"
          >
            <Icon
              v-if="isLoading"
              icon="mdi:loading"
              class="icon loading-icon"
            />
            <Icon
              v-else
              icon="mdi:login"
              class="icon"
            />
            <span>{{ isLoading ? '登录中...' : '登录' }}</span>
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  z-index: 1000;

  /* 背景图片 - 与导航页面保持一致 */
  background-image: url('/background/机甲.png');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;

  /* 防止布局跳动和滚动 */
  overflow: hidden;
}

.login-content {
  width: 100%;
  max-width: 400px;
  position: relative;

  /* 初始状态隐藏，防止布局跳动 */
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.3s ease-out;
}

/* 客户端加载完成后显示 */
.login-container.is-loaded .login-content {
  opacity: 1;
  transform: translateY(0);
}

.login-card {
  border-radius: 1.5rem;
  padding: 2.5rem;
  position: relative;
  overflow: hidden;

  /* 液态玻璃效果 - 与导航页面保持一致 */
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 50%,
    rgba(255, 255, 255, 0.02) 100%
  );
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);

  /* 边框效果 */
  border: 1px solid rgba(255, 255, 255, 0.1);

  /* 多层阴影效果 */
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.2),
    0 4px 16px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.2),
    inset 0 -1px 0 rgba(0, 0, 0, 0.1);

  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.login-card:hover {
  transform: translateY(-2px);
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.25),
    0 6px 20px rgba(0, 0, 0, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.25),
    inset 0 -1px 0 rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 2rem;
}

.login-icon {
  width: 4rem;
  height: 4rem;
  margin: 0 auto 1rem;
  border-radius: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;

  /* 图标背景效果 */
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.3) 0%,
    rgba(59, 130, 246, 0.15) 100%
  );
  border: 1px solid rgba(59, 130, 246, 0.4);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.login-icon .icon {
  width: 2rem;
  height: 2rem;
  color: rgba(255, 255, 255, 0.9);
}

.login-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 0.5rem;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
}

.login-subtitle {
  font-size: 0.95rem;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.15) 0%,
    rgba(239, 68, 68, 0.1) 100%
  );
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.9rem;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.error-icon {
  width: 1.125rem;
  height: 1.125rem;
  color: rgba(239, 68, 68, 0.8);
  flex-shrink: 0;
}

.password-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 1rem;
  z-index: 2;
  pointer-events: none;
}

.input-icon .icon {
  width: 1.25rem;
  height: 1.25rem;
  color: rgba(255, 255, 255, 0.8);
}

.password-input {
  width: 100%;
  padding: 1rem 3rem 1rem 3rem;
  border-radius: 0.75rem;
  border: none;
  outline: none;
  font-size: 1rem;
  color: #ffffff;

  /* 输入框液态玻璃效果 */
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);

  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.password-input::placeholder {
  color: rgba(255, 255, 255, 0.6);
}

.password-input:focus {
  border-color: rgba(59, 130, 246, 0.5);
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.1) 0%,
    rgba(59, 130, 246, 0.05) 100%
  );
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
}

.password-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.toggle-password-btn {
  position: absolute;
  right: 1rem;
  z-index: 2;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.25rem;
  border-radius: 0.375rem;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.toggle-password-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.toggle-password-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toggle-password-btn .icon {
  width: 1.25rem;
  height: 1.25rem;
  color: rgba(255, 255, 255, 0.8);
}

.login-button {
  width: 100%;
  padding: 1rem 1.5rem;
  border-radius: 0.75rem;
  border: none;
  outline: none;
  font-size: 1rem;
  font-weight: 600;
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;

  /* 按钮液态玻璃效果 */
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.4) 0%,
    rgba(59, 130, 246, 0.3) 50%,
    rgba(59, 130, 246, 0.2) 100%
  );
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(59, 130, 246, 0.5);

  /* 阴影效果 */
  box-shadow:
    0 4px 16px rgba(59, 130, 246, 0.3),
    0 2px 8px rgba(59, 130, 246, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);

  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.login-button:hover:not(:disabled) {
  transform: translateY(-1px);
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.5) 0%,
    rgba(59, 130, 246, 0.4) 50%,
    rgba(59, 130, 246, 0.3) 100%
  );
  box-shadow:
    0 6px 20px rgba(59, 130, 246, 0.4),
    0 3px 12px rgba(59, 130, 246, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.25);
}

.login-button:active:not(:disabled) {
  transform: translateY(0);
}

.login-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.login-button .icon {
  width: 1.25rem;
  height: 1.25rem;
}

.loading-icon {
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



/* 响应式设计 */
@media (max-width: 480px) {
  .login-container {
    padding: 1rem;
  }

  .login-card {
    padding: 2rem 1.5rem;
  }

  .login-title {
    font-size: 1.5rem;
  }

  .login-icon {
    width: 3.5rem;
    height: 3.5rem;
  }

  .login-icon .icon {
    width: 1.75rem;
    height: 1.75rem;
  }

  .password-input {
    padding: 0.875rem 2.75rem 0.875rem 2.75rem;
    font-size: 0.9rem;
  }

  .login-button {
    padding: 0.875rem 1.25rem;
    font-size: 0.9rem;
  }
}
</style>