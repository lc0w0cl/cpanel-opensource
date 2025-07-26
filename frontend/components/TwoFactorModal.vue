<template>
  <div v-if="show" class="modal-overlay" @click="handleOverlayClick">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h2>双因素认证</h2>
        <button @click="$emit('close')" class="close-btn">
          <Icon icon="mdi:close" />
        </button>
      </div>
      
      <div class="modal-body">
        <div class="auth-icon">
          <Icon icon="mdi:shield-key" />
        </div>
        
        <p class="auth-description">
          为了保护您的服务器安全，请输入双因素认证代码
        </p>
        
        <div class="auth-tabs">
          <button 
            :class="{ active: activeTab === 'totp' }"
            @click="activeTab = 'totp'"
            class="tab-btn"
          >
            认证器代码
          </button>
          <button 
            :class="{ active: activeTab === 'backup' }"
            @click="activeTab = 'backup'"
            class="tab-btn"
          >
            恢复代码
          </button>
        </div>
        
        <!-- 使用表单结构以支持密码管理器 -->
        <form @submit.prevent="handleVerify" class="auth-form">
          <div v-if="activeTab === 'totp'" class="auth-input-section">
            <label for="totp-code">请输入认证器应用中的6位数字代码：</label>
            <div class="verification-input">
              <input
                id="totp-code"
                name="totp-code"
                v-model="verificationCode"
                type="text"
                placeholder="000000"
                maxlength="6"
                autocomplete="one-time-code"
                inputmode="numeric"
                pattern="[0-9]*"
                @input="formatVerificationCode"
                @keyup.enter="handleVerify"
                :disabled="isVerifying"
                ref="totpInput"
                aria-label="双因素认证代码"
                aria-describedby="totp-description"
              />
            </div>
            <p id="totp-description" class="input-description">
              请输入您的认证器应用（如 Google Authenticator、Authy 等）中显示的6位数字代码
            </p>
          </div>

          <div v-if="activeTab === 'backup'" class="auth-input-section">
            <label for="backup-code">请输入8位恢复代码：</label>
            <div class="verification-input">
              <input
                id="backup-code"
                name="backup-code"
                v-model="backupCode"
                type="text"
                placeholder="12345678"
                maxlength="8"
                autocomplete="one-time-code"
                inputmode="numeric"
                pattern="[0-9]*"
                @input="formatBackupCode"
                @keyup.enter="handleVerify"
                :disabled="isVerifying"
                ref="backupInput"
                aria-label="备用恢复代码"
                aria-describedby="backup-description"
              />
            </div>
            <p id="backup-description" class="backup-note">
              恢复代码只能使用一次，使用后将失效
            </p>
          </div>
        </form>
        
        <div v-if="error" class="error-message">
          <Icon icon="mdi:alert-circle" />
          {{ error }}
        </div>
        
        <div class="modal-actions">
          <button @click="$emit('close')" class="btn-secondary" :disabled="isVerifying">
            取消
          </button>
          <button @click="handleVerify" class="btn-primary" :disabled="!canVerify || isVerifying">
            <Icon v-if="isVerifying" icon="mdi:loading" class="spin" />
            验证
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { Icon } from '@iconify/vue'

// Props
interface Props {
  show: boolean
  isVerifying?: boolean
  error?: string
}

const props = withDefaults(defineProps<Props>(), {
  isVerifying: false,
  error: ''
})

// Emits
interface Emits {
  (e: 'close'): void
  (e: 'verify', data: { verificationCode?: string; backupCode?: string }): void
}

const emit = defineEmits<Emits>()

// 响应式数据
const activeTab = ref<'totp' | 'backup'>('totp')
const verificationCode = ref('')
const backupCode = ref('')

// 模板引用
const totpInput = ref<HTMLInputElement>()
const backupInput = ref<HTMLInputElement>()

// 计算属性
const canVerify = computed(() => {
  if (activeTab.value === 'totp') {
    return verificationCode.value.length === 6
  } else {
    return backupCode.value.length === 8
  }
})

// 方法
const formatVerificationCode = () => {
  // 只允许数字
  verificationCode.value = verificationCode.value.replace(/\D/g, '')
}

const formatBackupCode = () => {
  // 只允许数字
  backupCode.value = backupCode.value.replace(/\D/g, '')
}

const handleVerify = () => {
  if (!canVerify.value || props.isVerifying) return
  
  if (activeTab.value === 'totp') {
    emit('verify', { verificationCode: verificationCode.value })
  } else {
    emit('verify', { backupCode: backupCode.value })
  }
}

const handleOverlayClick = () => {
  if (!props.isVerifying) {
    emit('close')
  }
}

const resetForm = () => {
  verificationCode.value = ''
  backupCode.value = ''
  activeTab.value = 'totp'
}

// 监听显示状态变化
watch(() => props.show, async (newShow) => {
  if (newShow) {
    resetForm()
    // 等待DOM更新后聚焦输入框
    await nextTick()
    if (activeTab.value === 'totp' && totpInput.value) {
      totpInput.value.focus()
    }
  }
})

// 监听标签切换
watch(activeTab, async () => {
  await nextTick()
  if (activeTab.value === 'totp' && totpInput.value) {
    totpInput.value.focus()
  } else if (activeTab.value === 'backup' && backupInput.value) {
    backupInput.value.focus()
  }
})
</script>

<style scoped>
/* 模态框样式 - 与整体液态玻璃风格保持一致 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(10px) saturate(150%);
  -webkit-backdrop-filter: blur(10px) saturate(150%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 1rem;
  animation: modalOverlayFadeIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.modal-content {
  max-width: 480px;
  width: 100%;
  border-radius: 1.5rem;
  overflow: hidden;
  position: relative;

  /* 液态玻璃效果 - 与登录页面保持一致 */
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 50%,
    rgba(255, 255, 255, 0.05) 100%
  );
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);

  /* 边框效果 */
  border: 1px solid rgba(255, 255, 255, 0.2);

  /* 多层阴影效果 */
  box-shadow:
    0 20px 40px rgba(0, 0, 0, 0.3),
    0 8px 16px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.3),
    inset 0 -1px 0 rgba(0, 0, 0, 0.1);

  animation: modalContentSlideIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 2rem 2rem 1rem 2rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.modal-header h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.95);
  margin: 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 0.5rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
  transform: scale(1.05);
}

.modal-body {
  padding: 1rem 2rem 2rem 2rem;
}

/* 认证图标 */
.auth-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 1.5rem;
}

.auth-icon svg {
  font-size: 3.5rem;
  color: rgba(59, 130, 246, 0.8);
  filter: drop-shadow(0 4px 8px rgba(59, 130, 246, 0.3));
}

.auth-description {
  text-align: center;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 2rem;
  line-height: 1.6;
  font-size: 0.95rem;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

/* 标签页 */
.auth-tabs {
  display: flex;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 0.75rem;
  padding: 0.25rem;
  margin-bottom: 2rem;
}

.tab-btn {
  flex: 1;
  padding: 0.75rem 1rem;
  border: none;
  background: none;
  border-radius: 0.5rem;
  font-size: 0.9rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.tab-btn.active {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.3) 0%,
    rgba(59, 130, 246, 0.2) 100%
  );
  color: rgba(255, 255, 255, 0.95);
  box-shadow:
    0 2px 8px rgba(59, 130, 246, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(59, 130, 246, 0.4);
}

.tab-btn:hover:not(.active) {
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.05);
}

/* 表单样式 */
.auth-form {
  width: 100%;
}

/* 输入区域 */
.auth-input-section {
  margin-bottom: 1.5rem;
}

.auth-input-section label {
  display: block;
  font-size: 0.9rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 0.75rem;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.verification-input input {
  width: 100%;
  padding: 1rem;
  border: none;
  border-radius: 0.75rem;
  font-size: 1.2rem;
  text-align: center;
  letter-spacing: 0.3em;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  color: rgba(255, 255, 255, 0.95);

  /* 输入框液态玻璃效果 - 与登录页面保持一致 */
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);

  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.verification-input input::placeholder {
  color: rgba(255, 255, 255, 0.5);
}

.verification-input input:focus {
  outline: none;
  border-color: rgba(59, 130, 246, 0.5);
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.1) 0%,
    rgba(59, 130, 246, 0.05) 100%
  );
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
  transform: translateY(-1px);
}

.verification-input input:disabled {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.05) 0%,
    rgba(255, 255, 255, 0.02) 100%
  );
  color: rgba(255, 255, 255, 0.4);
  cursor: not-allowed;
  opacity: 0.6;
}

.input-description {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 0.75rem;
  text-align: center;
  line-height: 1.5;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.backup-note {
  font-size: 0.8rem;
  color: rgba(251, 191, 36, 0.9);
  margin-top: 0.75rem;
  text-align: center;
  line-height: 1.5;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  background: linear-gradient(135deg,
    rgba(251, 191, 36, 0.1) 0%,
    rgba(251, 191, 36, 0.05) 100%
  );
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(251, 191, 36, 0.2);
}

/* 错误消息 */
.error-message {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.15) 0%,
    rgba(239, 68, 68, 0.1) 100%
  );
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 0.75rem;
  color: rgba(255, 255, 255, 0.95);
  font-size: 0.9rem;
  margin-bottom: 1.5rem;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.error-message svg {
  flex-shrink: 0;
  color: rgba(239, 68, 68, 0.8);
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.3));
}

/* 按钮 */
.modal-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 1rem;
}

.btn-primary, .btn-secondary {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem 1.5rem;
  border-radius: 0.75rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  text-decoration: none;
  font-size: 0.95rem;
}

.btn-primary {
  /* 按钮液态玻璃效果 - 与登录页面保持一致 */
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.4) 0%,
    rgba(59, 130, 246, 0.3) 50%,
    rgba(59, 130, 246, 0.2) 100%
  );
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(59, 130, 246, 0.5);
  color: rgba(255, 255, 255, 0.95);

  /* 阴影效果 */
  box-shadow:
    0 4px 16px rgba(59, 130, 246, 0.3),
    0 2px 8px rgba(59, 130, 246, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.5) 0%,
    rgba(59, 130, 246, 0.4) 50%,
    rgba(59, 130, 246, 0.3) 100%
  );
  box-shadow:
    0 8px 25px rgba(59, 130, 246, 0.4),
    0 4px 12px rgba(59, 130, 246, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

.btn-secondary {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.8);
}

.btn-secondary:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 100%
  );
  color: rgba(255, 255, 255, 0.95);
  transform: translateY(-1px);
}

.btn-primary:disabled, .btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* 动画效果 */
.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes modalOverlayFadeIn {
  from {
    opacity: 0;
    backdrop-filter: blur(0px);
    -webkit-backdrop-filter: blur(0px);
  }
  to {
    opacity: 1;
    backdrop-filter: blur(10px) saturate(150%);
    -webkit-backdrop-filter: blur(10px) saturate(150%);
  }
}

@keyframes modalContentSlideIn {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 响应式设计 */
@media (max-width: 640px) {
  .modal-overlay {
    padding: 0.5rem;
  }

  .modal-content {
    max-width: 100%;
    margin: 0;
  }

  .modal-header {
    padding: 1.5rem 1.5rem 1rem 1.5rem;
  }

  .modal-body {
    padding: 1rem 1.5rem 1.5rem 1.5rem;
  }

  .modal-actions {
    flex-direction: column;
    gap: 0.75rem;
  }

  .auth-tabs {
    flex-direction: column;
    gap: 0.25rem;
  }

  .tab-btn {
    padding: 1rem;
  }

  .verification-input input {
    font-size: 1.1rem;
    padding: 0.875rem;
  }
}

@media (max-width: 480px) {
  .modal-header h2 {
    font-size: 1.25rem;
  }

  .auth-icon svg {
    font-size: 2.5rem;
  }

  .auth-description {
    font-size: 0.9rem;
  }
}
</style>
