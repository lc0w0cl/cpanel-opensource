<template>
  <div class="settings-2fa">
    <div class="content-wrapper">
      <div class="settings-header">
        <h1>双因素认证 (2FA)</h1>
        <p>为您的账户添加额外的安全保护</p>
      </div>

      <!-- 2FA状态卡片 -->
      <div class="status-card">
        <div class="status-info">
          <div class="status-icon" :class="{ enabled: twoFactorStatus.enabled }">
            <Icon :icon="twoFactorStatus.enabled ? 'mdi:shield-check' : 'mdi:shield-off'" />
          </div>
          <div class="status-text">
            <h3>{{ twoFactorStatus.enabled ? '2FA已启用' : '2FA未启用' }}</h3>
            <p>{{ twoFactorStatus.enabled ? '您的账户受到双因素认证保护' : '启用2FA以增强账户安全性' }}</p>
          </div>
        </div>
        <div class="status-actions">
          <button 
            v-if="!twoFactorStatus.enabled" 
            @click="showSetupModal = true"
            class="btn-primary"
          >
            <Icon icon="mdi:plus" />
            启用2FA
          </button>
          <button 
            v-else 
            @click="showDisableModal = true"
            class="btn-danger"
          >
            <Icon icon="mdi:close" />
            禁用2FA
          </button>
        </div>
      </div>

      <!-- 备用恢复码 -->
      <div v-if="twoFactorStatus.enabled" class="backup-codes-card">
        <h3>备用恢复码</h3>
        <p>当您无法使用认证器应用时，可以使用这些恢复码登录</p>
        <button @click="showRegenerateModal = true" class="btn-secondary">
          <Icon icon="mdi:refresh" />
          重新生成恢复码
        </button>
      </div>
    </div>

    <!-- 设置2FA模态框 -->
    <div v-if="showSetupModal" class="modal-overlay" @click="closeSetupModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h2>设置双因素认证</h2>
          <button @click="closeSetupModal" class="close-btn">
            <Icon icon="mdi:close" />
          </button>
        </div>
        
        <div class="modal-body">
          <div v-if="setupStep === 1" class="setup-step">
            <h3>步骤 1: 扫描二维码</h3>
            <p>使用您的认证器应用（如Google Authenticator、Authy等）扫描下方二维码：</p>
            
            <div class="qr-code-container">
              <img v-if="qrCodeImage" :src="`data:image/png;base64,${qrCodeImage}`" alt="2FA QR Code" />
              <div v-else class="loading">生成二维码中...</div>
            </div>
            
            <div class="manual-entry">
              <p>或手动输入密钥：</p>
              <div class="secret-key">{{ secretKey }}</div>
            </div>
            
            <div class="step-actions">
              <button @click="setupStep = 2" class="btn-primary" :disabled="!qrCodeImage">
                下一步
              </button>
            </div>
          </div>
          
          <div v-if="setupStep === 2" class="setup-step">
            <h3>步骤 2: 验证设置</h3>
            <p>请输入认证器应用中显示的6位数字验证码：</p>
            
            <div class="verification-input">
              <input 
                v-model="verificationCode" 
                type="text" 
                placeholder="000000"
                maxlength="6"
                @input="formatVerificationCode"
              />
            </div>
            
            <div class="step-actions">
              <button @click="setupStep = 1" class="btn-secondary">
                上一步
              </button>
              <button @click="enableTwoFactor" class="btn-primary" :disabled="!verificationCode || isEnabling">
                <Icon v-if="isEnabling" icon="mdi:loading" class="spin" />
                启用2FA
              </button>
            </div>
          </div>
          
          <!-- 备用恢复码显示 -->
          <div v-if="setupStep === 3" class="setup-step">
            <h3>设置完成！</h3>
            <p>请保存这些备用恢复码，当您无法使用认证器时可以使用它们：</p>
            
            <div class="backup-codes">
              <div v-for="code in backupCodes" :key="code" class="backup-code">
                {{ code }}
              </div>
            </div>
            
            <div class="warning">
              <Icon icon="mdi:alert" />
              <span>请将这些恢复码保存在安全的地方，每个恢复码只能使用一次。</span>
            </div>
            
            <div class="step-actions">
              <button @click="finishSetup" class="btn-primary">
                完成设置
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 禁用2FA模态框 -->
    <div v-if="showDisableModal" class="modal-overlay" @click="showDisableModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h2>禁用双因素认证</h2>
          <button @click="showDisableModal = false" class="close-btn">
            <Icon icon="mdi:close" />
          </button>
        </div>
        
        <div class="modal-body">
          <div class="warning">
            <Icon icon="mdi:alert" />
            <span>禁用2FA将降低您账户的安全性。请输入验证码以确认操作。</span>
          </div>
          
          <div class="verification-input">
            <input 
              v-model="disableVerificationCode" 
              type="text" 
              placeholder="输入6位验证码"
              maxlength="6"
            />
          </div>
          
          <div class="modal-actions">
            <button @click="showDisableModal = false" class="btn-secondary">
              取消
            </button>
            <button @click="disableTwoFactor" class="btn-danger" :disabled="!disableVerificationCode || isDisabling">
              <Icon v-if="isDisabling" icon="mdi:loading" class="spin" />
              禁用2FA
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 重新生成恢复码模态框 -->
    <div v-if="showRegenerateModal" class="modal-overlay" @click="showRegenerateModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h2>重新生成恢复码</h2>
          <button @click="showRegenerateModal = false" class="close-btn">
            <Icon icon="mdi:close" />
          </button>
        </div>
        
        <div class="modal-body">
          <div class="warning">
            <Icon icon="mdi:alert" />
            <span>重新生成恢复码将使现有的恢复码失效。请输入验证码以确认操作。</span>
          </div>
          
          <div class="verification-input">
            <input 
              v-model="regenerateVerificationCode" 
              type="text" 
              placeholder="输入6位验证码"
              maxlength="6"
            />
          </div>
          
          <div class="modal-actions">
            <button @click="showRegenerateModal = false" class="btn-secondary">
              取消
            </button>
            <button @click="regenerateBackupCodes" class="btn-primary" :disabled="!regenerateVerificationCode || isRegenerating">
              <Icon v-if="isRegenerating" icon="mdi:loading" class="spin" />
              重新生成
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Icon } from '@iconify/vue'

// 响应式数据
const twoFactorStatus = ref({
  enabled: false,
  configured: false
})

const showSetupModal = ref(false)
const showDisableModal = ref(false)
const showRegenerateModal = ref(false)
const setupStep = ref(1)

const qrCodeImage = ref('')
const secretKey = ref('')
const backupCodes = ref<string[]>([])

const verificationCode = ref('')
const disableVerificationCode = ref('')
const regenerateVerificationCode = ref('')

const isEnabling = ref(false)
const isDisabling = ref(false)
const isRegenerating = ref(false)

// 方法
const formatVerificationCode = () => {
  // 只允许数字
  verificationCode.value = verificationCode.value.replace(/\D/g, '')
}

const closeSetupModal = () => {
  showSetupModal.value = false
  setupStep.value = 1
  verificationCode.value = ''
  qrCodeImage.value = ''
  secretKey.value = ''
}

const finishSetup = () => {
  closeSetupModal()
  loadTwoFactorStatus()
}

// API调用方法
const loadTwoFactorStatus = async () => {
  try {
    const { apiRequest } = await import('~/composables/useJwt')
    const config = useRuntimeConfig()
    const apiUrl = `${config.public.apiBaseUrl}/api/2fa/status`
    console.log('请求2FA状态URL:', apiUrl)

    const response = await apiRequest(apiUrl)
    console.log('2FA状态响应:', response.status, response.statusText)

    if (!response.ok) {
      const text = await response.text()
      console.error('2FA状态请求失败:', text)

      // 如果是401错误，可能是认证问题
      if (response.status === 401) {
        console.warn('2FA API需要认证，但当前用户可能未登录')
        twoFactorStatus.value = { enabled: false, configured: false }
        return
      }

      // 如果后端服务没有启动，暂时禁用2FA功能
      if (response.status === 0 || text.includes('<!DOCTYPE')) {
        console.warn('后端服务可能没有启动，暂时禁用2FA功能')
        twoFactorStatus.value = { enabled: false, configured: false }
        return
      }

      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    const result = await response.json()
    console.log('2FA状态响应数据:', result)

    if (result.success) {
      twoFactorStatus.value = result.data
    } else {
      throw new Error(result.message || '获取2FA状态失败')
    }
  } catch (error) {
    console.error('获取2FA状态失败:', error)
    // 发生错误时，默认设置为未启用状态
    twoFactorStatus.value = { enabled: false, configured: false }

    // 显示用户友好的错误信息
    if (error instanceof Error && error.message.includes('401')) {
      console.warn('2FA功能需要登录后使用')
    } else if (error instanceof Error && error.message.includes('fetch')) {
      alert('无法连接到服务器，请检查后端服务是否正常运行')
    }
  }
}

const generateTwoFactor = async () => {
  try {
    const { apiRequest } = await import('~/composables/useJwt')
    const config = useRuntimeConfig()
    const apiUrl = `${config.public.apiBaseUrl}/api/2fa/generate`

    const response = await apiRequest(apiUrl, {
      method: 'POST'
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    const result = await response.json()
    if (result.success) {
      qrCodeImage.value = result.data.qrCodeImage
      secretKey.value = result.data.secretKey
      backupCodes.value = JSON.parse(result.data.backupCodes)
    } else {
      throw new Error(result.message || '生成2FA配置失败')
    }
  } catch (error) {
    console.error('生成2FA配置失败:', error)
    alert('生成2FA配置失败: ' + (error instanceof Error ? error.message : '未知错误'))
  }
}

const enableTwoFactor = async () => {
  if (!verificationCode.value) return

  isEnabling.value = true
  try {
    const { apiRequest } = await import('~/composables/useJwt')
    const config = useRuntimeConfig()
    const apiUrl = `${config.public.apiBaseUrl}/api/2fa/enable`

    const response = await apiRequest(apiUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        verificationCode: verificationCode.value
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    const result = await response.json()
    if (result.success) {
      setupStep.value = 3
    } else {
      alert('验证码错误，请重试: ' + (result.message || ''))
    }
  } catch (error) {
    console.error('启用2FA失败:', error)
    alert('启用2FA失败: ' + (error instanceof Error ? error.message : '未知错误'))
  } finally {
    isEnabling.value = false
  }
}

const disableTwoFactor = async () => {
  if (!disableVerificationCode.value) return
  
  isDisabling.value = true
  try {
    const response = await fetch('/api/2fa/disable', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
      },
      body: JSON.stringify({
        verificationCode: disableVerificationCode.value
      })
    })
    const result = await response.json()
    if (result.success) {
      showDisableModal.value = false
      disableVerificationCode.value = ''
      loadTwoFactorStatus()
      alert('2FA已禁用')
    } else {
      alert('验证码错误，请重试')
    }
  } catch (error) {
    console.error('禁用2FA失败:', error)
    alert('禁用2FA失败')
  } finally {
    isDisabling.value = false
  }
}

const regenerateBackupCodes = async () => {
  if (!regenerateVerificationCode.value) return
  
  isRegenerating.value = true
  try {
    const response = await fetch('/api/2fa/regenerate-backup-codes', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
      },
      body: JSON.stringify({
        verificationCode: regenerateVerificationCode.value
      })
    })
    const result = await response.json()
    if (result.success) {
      showRegenerateModal.value = false
      regenerateVerificationCode.value = ''
      alert('恢复码已重新生成')
    } else {
      alert('验证码错误，请重试')
    }
  } catch (error) {
    console.error('重新生成恢复码失败:', error)
    alert('重新生成恢复码失败')
  } finally {
    isRegenerating.value = false
  }
}

// 监听设置模态框打开，生成2FA配置
watch(() => showSetupModal.value, (newValue) => {
  if (newValue) {
    generateTwoFactor()
  }
})

// 页面加载时获取状态
onMounted(async () => {
  // 检查用户是否已登录
  const { checkAuthStatus } = await import('~/composables/useJwt')
  const isAuthenticated = await checkAuthStatus()

  if (!isAuthenticated) {
    console.warn('用户未登录，无法访问2FA设置')
    // 可以选择重定向到登录页面
    // await navigateTo('/login')
    return
  }

  loadTwoFactorStatus()
})
</script>
