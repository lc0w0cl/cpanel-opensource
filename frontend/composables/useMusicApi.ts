/**
 * 音乐API 相关的组合式函数
 */

// 音乐搜索结果接口定义
export interface MusicSearchResult {
  id: string
  title: string
  artist: string
  duration: string
  platform: string
  thumbnail: string
  url: string
  quality: string
  playCount?: string
  publishTime?: string
  description?: string
  tags?: string[]
}

// 搜索请求接口定义
export interface MusicSearchRequest {
  query: string
  searchType: 'keyword' | 'url'
  platform: 'bilibili' | 'youtube' | 'both'
  page?: number
  pageSize?: number
}

// API响应接口定义
export interface ApiResponse<T> {
  code: number
  message: string
  success: boolean
  data: T
}

/**
 * 音乐API 服务
 */
export const useMusicApi = () => {
  const config = useRuntimeConfig()
  const API_BASE_URL = `${config.public.apiBaseUrl}/api`

  /**
   * 搜索音乐
   */
  const searchMusic = async (request: MusicSearchRequest): Promise<MusicSearchResult[]> => {
    try {
      // 音乐搜索不需要认证，直接使用 fetch
      const response = await fetch(`${API_BASE_URL}/music/search`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(request)
      })

      const result: ApiResponse<MusicSearchResult[]> = await response.json()

      if (result.success) {
        return result.data || []
      } else {
        console.error('搜索音乐失败:', result.message)
        throw new Error(result.message)
      }
    } catch (error) {
      console.error('搜索音乐异常:', error)
      throw error
    }
  }

  /**
   * 获取视频详情
   */
  const getVideoDetail = async (platform: string, videoId: string): Promise<MusicSearchResult | null> => {
    try {
      // 获取视频详情不需要认证，直接使用 fetch
      const response = await fetch(`${API_BASE_URL}/music/video/${platform}/${videoId}`)
      const result: ApiResponse<MusicSearchResult> = await response.json()

      if (result.success) {
        return result.data
      } else {
        console.error('获取视频详情失败:', result.message)
        return null
      }
    } catch (error) {
      console.error('获取视频详情异常:', error)
      return null
    }
  }

  /**
   * 下载音乐（预留接口）
   */
  const downloadMusic = async (videoUrl: string, options?: any): Promise<boolean> => {
    try {
      const response = await apiRequest(`${API_BASE_URL}/music/download`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          url: videoUrl,
          ...options
        })
      })
      
      const result: ApiResponse<any> = await response.json()
      
      if (result.success) {
        return true
      } else {
        console.error('下载音乐失败:', result.message)
        return false
      }
    } catch (error) {
      console.error('下载音乐异常:', error)
      return false
    }
  }

  return {
    searchMusic,
    getVideoDetail,
    downloadMusic
  }
}
