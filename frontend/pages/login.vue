<script setup lang="ts">
import { ref } from 'vue'
import { Icon } from '@iconify/vue'

// 定义页面元数据，使用空白布局
definePageMeta({
  layout: 'blank'
})

// 响应式数据
const password = ref('')
const isLoading = ref(false)
const showPassword = ref(false)

// 登录处理函数
const handleLogin = async () => {
  if (!password.value.trim()) {
    return
  }

  isLoading.value = true

  try {
    // 这里将来会添加实际的登录逻辑
    console.log('登录密码:', password.value)

    // 模拟登录延迟
    await new Promise(resolve => setTimeout(resolve, 1000))

    // 登录成功后跳转到首页
    await navigateTo('/')
  } catch (error) {
    console.error('登录失败:', error)
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
  <div class="login-container">
    <div class="login-content">
      <!-- 登录卡片 -->
      <div class="login-card">
        <!-- 标题区域 -->
        <div class="login-header">
          <div class="login-icon">
            <Icon icon="mdi:shield-key" class="icon" />
          </div>
          <h1 class="login-title">面板登录</h1>
          <p class="login-subtitle">请输入访问密码</p>
        </div>

        <!-- 表单区域 -->
        <form @submit.prevent="handleLogin" class="login-form">
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
  position: relative;
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;

  /* 背景图片 - 与导航页面保持一致 */
  background-image: url('/background/机甲.png');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
}

.login-content {
  width: 100%;
  max-width: 400px;
  position: relative;
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