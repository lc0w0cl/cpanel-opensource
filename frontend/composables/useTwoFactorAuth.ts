import { ref } from 'vue'
import { apiRequest } from '~/composables/useJwt'

// 2FA状态接口
export interface TwoFactorStatus {
  enabled: boolean
  configured: boolean
}

// 2FA验证结果接口
export interface TwoFactorVerificationResult {
  success: boolean
  message: string
  verified?: boolean
}

export const useTwoFactorAuth = () => {
  // 响应式状态
  const twoFactorStatus = ref<TwoFactorStatus>({
    enabled: false,
    configured: false
  })
  
  const isLoading = ref(false)
  const error = ref('')

  // 获取API基础URL
  const getApiBaseUrl = () => {
    const config = useRuntimeConfig()
    return `${config.public.apiBaseUrl}/api`
  }

  /**
   * 获取2FA状态
   */
  const getTwoFactorStatus = async (): Promise<TwoFactorStatus> => {
    try {
      const API_BASE_URL = getApiBaseUrl()
      console.log('请求2FA状态，URL:', `${API_BASE_URL}/2fa/status`)

      const response = await apiRequest(`${API_BASE_URL}/2fa/status`)
      console.log('2FA状态响应状态:', response.status)

      if (!response.ok) {
        const text = await response.text()
        console.error('2FA状态请求失败，响应内容:', text)
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      const result = await response.json()
      console.log('2FA状态响应数据:', result)

      if (result.success) {
        twoFactorStatus.value = result.data
        return result.data
      } else {
        throw new Error(result.message || '获取2FA状态失败')
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取2FA状态失败'
      console.error('获取2FA状态失败:', err)
      return { enabled: false, configured: false }
    }
  }

  /**
   * 生成2FA配置
   */
  const generateTwoFactorAuth = async () => {
    try {
      isLoading.value = true
      const API_BASE_URL = getApiBaseUrl()
      console.log('请求生成2FA配置，URL:', `${API_BASE_URL}/2fa/generate`)

      const response = await apiRequest(`${API_BASE_URL}/2fa/generate`, {
        method: 'POST'
      })
      console.log('生成2FA配置响应状态:', response.status)

      if (!response.ok) {
        const text = await response.text()
        console.error('生成2FA配置请求失败，响应内容:', text)
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      const result = await response.json()
      console.log('生成2FA配置响应数据:', result)

      if (result.success) {
        return result.data
      } else {
        throw new Error(result.message || '生成2FA配置失败')
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '生成2FA配置失败'
      console.error('生成2FA配置失败:', err)
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 启用2FA
   */
  const enableTwoFactorAuth = async (verificationCode: string): Promise<boolean> => {
    try {
      isLoading.value = true
      const API_BASE_URL = getApiBaseUrl()
      const response = await apiRequest(`${API_BASE_URL}/2fa/enable`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ verificationCode })
      })
      const result = await response.json()
      
      if (result.success) {
        await getTwoFactorStatus() // 刷新状态
        return true
      } else {
        error.value = result.message || '启用2FA失败'
        return false
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '启用2FA失败'
      console.error('启用2FA失败:', err)
      return false
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 禁用2FA
   */
  const disableTwoFactorAuth = async (verificationCode: string): Promise<boolean> => {
    try {
      isLoading.value = true
      const API_BASE_URL = getApiBaseUrl()
      const response = await apiRequest(`${API_BASE_URL}/2fa/disable`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ verificationCode })
      })
      const result = await response.json()
      
      if (result.success) {
        await getTwoFactorStatus() // 刷新状态
        return true
      } else {
        error.value = result.message || '禁用2FA失败'
        return false
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '禁用2FA失败'
      console.error('禁用2FA失败:', err)
      return false
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 验证2FA代码
   */
  const verifyTwoFactorCode = async (verificationCode?: string, backupCode?: string): Promise<TwoFactorVerificationResult> => {
    try {
      isLoading.value = true
      const API_BASE_URL = getApiBaseUrl()
      
      const requestBody: any = {}
      if (verificationCode) {
        requestBody.verificationCode = verificationCode
      }
      if (backupCode) {
        requestBody.backupCode = backupCode
      }
      
      const response = await apiRequest(`${API_BASE_URL}/2fa/verify`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestBody)
      })
      const result = await response.json()
      
      return {
        success: result.success,
        message: result.message || (result.success ? '验证成功' : '验证失败'),
        verified: result.success
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : '验证2FA失败'
      error.value = message
      console.error('验证2FA失败:', err)
      return {
        success: false,
        message,
        verified: false
      }
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 重新生成备用恢复码
   */
  const regenerateBackupCodes = async (verificationCode: string): Promise<string[] | null> => {
    try {
      isLoading.value = true
      const API_BASE_URL = getApiBaseUrl()
      const response = await apiRequest(`${API_BASE_URL}/2fa/regenerate-backup-codes`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ verificationCode })
      })
      const result = await response.json()
      
      if (result.success) {
        return result.data
      } else {
        error.value = result.message || '重新生成恢复码失败'
        return null
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '重新生成恢复码失败'
      console.error('重新生成恢复码失败:', err)
      return null
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 检查是否需要2FA验证
   */
  const checkTwoFactorRequired = async (): Promise<boolean> => {
    try {
      const status = await getTwoFactorStatus()
      return status.enabled
    } catch (error) {
      console.warn('检查2FA状态失败，默认不需要2FA验证:', error)
      // 如果检查失败，默认不需要2FA验证，避免阻塞用户操作
      return false
    }
  }

  /**
   * 验证并连接（用于终端连接）
   */
  const verifyAndConnect = async (
    serverConfig: any,
    verificationCode?: string,
    backupCode?: string
  ): Promise<{ success: boolean; message: string; twoFactorRequired?: boolean; verified?: boolean }> => {
    try {
      isLoading.value = true
      const API_BASE_URL = getApiBaseUrl()
      
      const requestBody = {
        ...serverConfig,
        verificationCode,
        backupCode
      }
      
      const response = await apiRequest(`${API_BASE_URL}/terminal/verify-and-connect`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestBody)
      })
      const result = await response.json()
      
      return {
        success: result.success,
        message: result.message || (result.success ? '验证成功' : '验证失败'),
        twoFactorRequired: result.data?.twoFactorRequired,
        verified: result.data?.verified
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : '验证失败'
      error.value = message
      console.error('验证并连接失败:', err)
      return {
        success: false,
        message
      }
    } finally {
      isLoading.value = false
    }
  }

  return {
    // 状态
    twoFactorStatus,
    isLoading,
    error,
    
    // 方法
    getTwoFactorStatus,
    generateTwoFactorAuth,
    enableTwoFactorAuth,
    disableTwoFactorAuth,
    verifyTwoFactorCode,
    regenerateBackupCodes,
    checkTwoFactorRequired,
    verifyAndConnect
  }
}
