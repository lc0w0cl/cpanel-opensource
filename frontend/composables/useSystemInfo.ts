/**
 * 系统信息管理
 */

import { apiRequest } from './useJwt'

// 类型定义
export interface CpuInfo {
  model: string
  cores: number
  logicalProcessors: number
  usage: number
  frequency: number
}

export interface MemoryInfo {
  total: number
  used: number
  available: number
  usage: number
}

export interface DiskInfo {
  name: string
  fileSystem: string
  total: number
  used: number
  available: number
  usage: number
}

export interface SystemBasicInfo {
  osName: string
  osVersion: string
  architecture: string
  hostname: string
  uptime: number
}

export interface SystemInfo {
  cpu: CpuInfo
  memory: MemoryInfo
  disks: DiskInfo[]
  system: SystemBasicInfo
}

export interface ApiResponse<T> {
  code: number
  message: string
  success: boolean
  data: T
}

/**
 * 系统信息API
 */
export const useSystemInfo = () => {
  const config = useRuntimeConfig()
  const API_BASE_URL = `${config.public.apiBaseUrl}/api`

  /**
   * 获取完整系统信息
   */
  const getSystemInfo = async (): Promise<SystemInfo> => {
    const response = await apiRequest(`${API_BASE_URL}/system/info`)
    const result: ApiResponse<SystemInfo> = await response.json()
    
    if (result.success) {
      return result.data
    } else {
      throw new Error(result.message)
    }
  }

  /**
   * 获取CPU信息
   */
  const getCpuInfo = async (): Promise<CpuInfo> => {
    const response = await apiRequest(`${API_BASE_URL}/system/cpu`)
    const result: ApiResponse<CpuInfo> = await response.json()
    
    if (result.success) {
      return result.data
    } else {
      throw new Error(result.message)
    }
  }

  /**
   * 获取内存信息
   */
  const getMemoryInfo = async (): Promise<MemoryInfo> => {
    const response = await apiRequest(`${API_BASE_URL}/system/memory`)
    const result: ApiResponse<MemoryInfo> = await response.json()
    
    if (result.success) {
      return result.data
    } else {
      throw new Error(result.message)
    }
  }

  /**
   * 获取磁盘信息
   */
  const getDiskInfo = async (): Promise<DiskInfo[]> => {
    const response = await apiRequest(`${API_BASE_URL}/system/disk`)
    const result: ApiResponse<DiskInfo[]> = await response.json()
    
    if (result.success) {
      return result.data
    } else {
      throw new Error(result.message)
    }
  }

  /**
   * 获取系统基本信息
   */
  const getSystemBasicInfo = async (): Promise<SystemBasicInfo> => {
    const response = await apiRequest(`${API_BASE_URL}/system/basic`)
    const result: ApiResponse<SystemBasicInfo> = await response.json()
    
    if (result.success) {
      return result.data
    } else {
      throw new Error(result.message)
    }
  }

  /**
   * 格式化运行时间
   */
  const formatUptime = (seconds: number): string => {
    const days = Math.floor(seconds / 86400)
    const hours = Math.floor((seconds % 86400) / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    
    if (days > 0) {
      return `${days}天 ${hours}小时 ${minutes}分钟`
    } else if (hours > 0) {
      return `${hours}小时 ${minutes}分钟`
    } else {
      return `${minutes}分钟`
    }
  }

  /**
   * 格式化存储容量
   */
  const formatStorage = (gb: number): string => {
    if (gb >= 1024) {
      return `${(gb / 1024).toFixed(1)} TB`
    } else {
      return `${gb.toFixed(1)} GB`
    }
  }

  /**
   * 格式化CPU频率
   */
  const formatFrequency = (hz: number): string => {
    if (hz >= 1000000000) {
      return `${(hz / 1000000000).toFixed(2)} GHz`
    } else if (hz >= 1000000) {
      return `${(hz / 1000000).toFixed(0)} MHz`
    } else {
      return `${hz} Hz`
    }
  }

  /**
   * 获取使用率颜色
   */
  const getUsageColor = (usage: number): string => {
    if (usage >= 90) return 'text-red-400'
    if (usage >= 70) return 'text-yellow-400'
    if (usage >= 50) return 'text-blue-400'
    return 'text-green-400'
  }

  /**
   * 获取使用率背景颜色
   */
  const getUsageBgColor = (usage: number): string => {
    if (usage >= 90) return 'bg-red-500'
    if (usage >= 70) return 'bg-yellow-500'
    if (usage >= 50) return 'bg-blue-500'
    return 'bg-green-500'
  }

  return {
    getSystemInfo,
    getCpuInfo,
    getMemoryInfo,
    getDiskInfo,
    getSystemBasicInfo,
    formatUptime,
    formatStorage,
    formatFrequency,
    getUsageColor,
    getUsageBgColor
  }
}
