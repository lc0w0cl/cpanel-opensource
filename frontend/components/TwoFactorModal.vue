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
/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 1rem;
}

.modal-content {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 1rem;
  max-width: 450px;
  width: 100%;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
}

.modal-header h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #6b7280;
  cursor: pointer;
  padding: 0.25rem;
  border-radius: 0.25rem;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: rgba(0, 0, 0, 0.1);
  color: #374151;
}

.modal-body {
  padding: 1.5rem;
}

/* 认证图标 */
.auth-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 1rem;
}

.auth-icon svg {
  font-size: 3rem;
  color: #3b82f6;
}

.auth-description {
  text-align: center;
  color: #6b7280;
  margin-bottom: 1.5rem;
  line-height: 1.5;
}

/* 标签页 */
.auth-tabs {
  display: flex;
  background: #f3f4f6;
  border-radius: 0.5rem;
  padding: 0.25rem;
  margin-bottom: 1.5rem;
}

.tab-btn {
  flex: 1;
  padding: 0.5rem 1rem;
  border: none;
  background: none;
  border-radius: 0.375rem;
  font-size: 0.9rem;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-btn.active {
  background: white;
  color: #1f2937;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.tab-btn:hover:not(.active) {
  color: #374151;
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
  color: #374151;
  margin-bottom: 0.5rem;
}

.verification-input input {
  width: 100%;
  padding: 0.75rem;
  border: 2px solid #d1d5db;
  border-radius: 0.5rem;
  font-size: 1.1rem;
  text-align: center;
  letter-spacing: 0.2em;
  font-family: monospace;
  transition: border-color 0.2s ease;
}

.verification-input input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.verification-input input:disabled {
  background: #f9fafb;
  color: #6b7280;
  cursor: not-allowed;
}

.input-description {
  font-size: 0.8rem;
  color: #6b7280;
  margin-top: 0.5rem;
  text-align: center;
  line-height: 1.4;
}

.backup-note {
  font-size: 0.8rem;
  color: #f59e0b;
  margin-top: 0.5rem;
  text-align: center;
  line-height: 1.4;
}

/* 错误消息 */
.error-message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 0.5rem;
  color: #dc2626;
  font-size: 0.9rem;
  margin-bottom: 1rem;
}

.error-message svg {
  flex-shrink: 0;
}

/* 按钮 */
.modal-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

.btn-primary, .btn-secondary {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  border-radius: 0.5rem;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
}

.btn-primary {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px rgba(59, 130, 246, 0.3);
}

.btn-secondary {
  background: rgba(0, 0, 0, 0.05);
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-secondary:hover:not(:disabled) {
  background: rgba(0, 0, 0, 0.1);
}

.btn-primary:disabled, .btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* 加载动画 */
.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 响应式设计 */
@media (max-width: 480px) {
  .modal-content {
    margin: 1rem;
  }
  
  .modal-actions {
    flex-direction: column;
  }
  
  .auth-tabs {
    flex-direction: column;
    gap: 0.25rem;
  }
}
</style>
