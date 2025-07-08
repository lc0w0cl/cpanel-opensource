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
  playlistName?: string
  vip?: boolean
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

// 音乐设置接口定义
export interface MusicSettings {
  downloadLocation: 'local' | 'server'
  serverDownloadPath: string
}

// 歌单歌曲接口定义
export interface PlaylistSong {
  title: string
  artist: string
  album: string
  url: string
  cover: string
  source: string
  sourceId: string
  duration?: string
  playlistName?: string
  vip?: boolean
}

// 歌单信息接口定义
export interface PlaylistInfo {
  title: string
  creator: string
  cover: string
  url: string
  source: string
  songs: PlaylistSong[]
  songCount: number
  description?: string
}

// 歌单解析请求接口定义
export interface PlaylistParseRequest {
  url: string
  platform?: 'qq' | 'netease' | 'auto'
}

/**
 * 音乐API 服务
 */
export const useMusicApi = () => {
  const config = useRuntimeConfig()

  // 根据环境自动处理URL
  const getFullUrl = (path: string) => {
    if (path.startsWith('/')) {
      // 相对路径，根据环境添加基础URL
      // 使用更可靠的环境判断：检查apiBaseUrl是否为空
      return config.public.apiBaseUrl
        ? `${config.public.apiBaseUrl}${path}`
        : path
    }
    // 绝对路径直接返回
    return path
  }

  const API_BASE_URL = getFullUrl('/api')

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
   * 解析歌单
   */
  const parsePlaylist = async (request: PlaylistParseRequest): Promise<PlaylistInfo> => {
    try {
      // 歌单解析不需要认证，直接使用 fetch
      const response = await fetch(`${API_BASE_URL}/music/parse-playlist`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          url: request.url,
          platform: request.platform || 'auto'
        })
      })

      const result: ApiResponse<PlaylistInfo> = await response.json()

      if (result.success) {
        return result.data
      } else {
        console.error('解析歌单失败:', result.message)
        throw new Error(result.message)
      }
    } catch (error) {
      console.error('解析歌单异常:', error)
      throw error
    }
  }

  /**
   * 获取支持的歌单平台
   */
  const getSupportedPlatforms = async (): Promise<string[]> => {
    try {
      const response = await fetch(`${API_BASE_URL}/music/supported-platforms`)
      const result: ApiResponse<string[]> = await response.json()

      if (result.success) {
        return result.data || []
      } else {
        console.error('获取支持平台失败:', result.message)
        return []
      }
    } catch (error) {
      console.error('获取支持平台异常:', error)
      return []
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
   * 获取音频流URL（用于在线播放）
   */
  const getAudioStream = async (platform: string, videoId: string): Promise<string | null> => {
    try {
      const response = await apiRequest(`${API_BASE_URL}/music/stream/${platform}/${videoId}`)
      const result: ApiResponse<string> = await response.json()

      if (result.success) {
        return result.data
      } else {
        console.error('获取音频流失败:', result.message)
        return null
      }
    } catch (error) {
      console.error('获取音频流异常:', error)
      return null
    }
  }

  /**
   * 通过URL获取音频流
   */
  const getAudioStreamByUrl = async (videoUrl: string): Promise<string | null> => {
    try {
      const response = await fetch(`${API_BASE_URL}/music/stream-url`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ url: videoUrl })
      })

      const result: ApiResponse<string> = await response.json()

      if (result.success) {
        return result.data
      } else {
        console.error('获取音频流失败:', result.message)
        return null
      }
    } catch (error) {
      console.error('获取音频流异常:', error)
      return null
    }
  }

  /**
   * 生成代理音频URL - 解决403错误，使用流式代理
   */
  const getProxyAudioUrl = (originalUrl: string): string => {
    if (!originalUrl) return ''

    // 对URL进行编码
    const encodedUrl = encodeURIComponent(originalUrl)

    // 构建代理URL - 使用流式代理，支持Range请求
    const proxyPath = `/api/music/proxy/audio-stream?url=${encodedUrl}`
    return getFullUrl(proxyPath)
  }

  /**
   * 获取可播放的音频URL - 智能选择原始URL或代理URL
   */
  const getPlayableAudioUrl = async (originalUrl: string): Promise<string> => {
    if (!originalUrl) return ''

    // 检查URL是否可能需要代理
    const needsProxy = originalUrl.includes('bilivideo.cn') ||
                      originalUrl.includes('googlevideo.com') ||
                      originalUrl.includes('upos-sz-') ||
                      originalUrl.includes('xy124x163x222x164xy')

    if (needsProxy) {
      console.log('检测到可能需要代理的URL，使用代理:', originalUrl)
      return getProxyAudioUrl(originalUrl)
    }

    // 对于其他URL，先尝试原始URL
    return originalUrl
  }

  /**
   * 当音频加载失败时，获取代理URL作为备用
   */
  const getFallbackAudioUrl = (originalUrl: string): string => {
    console.log('音频加载失败，切换到代理URL:', originalUrl)
    return getProxyAudioUrl(originalUrl)
  }

  /**
   * 获取音乐设置
   */
  const getMusicSettings = async (): Promise<MusicSettings> => {
    try {
      const response = await apiRequest(`${API_BASE_URL}/system-config/music`)
      const result: ApiResponse<MusicSettings> = await response.json()

      if (result.success) {
        return result.data
      } else {
        console.error('获取音乐设置失败:', result.message)
        // 返回默认设置
        return {
          downloadLocation: 'local',
          serverDownloadPath: 'uploads/music'
        }
      }
    } catch (error) {
      console.error('获取音乐设置异常:', error)
      // 返回默认设置
      return {
        downloadLocation: 'local',
        serverDownloadPath: 'uploads/music'
      }
    }
  }

  /**
   * 服务器端下载音乐
   */
  const downloadMusicToServer = async (musicResult: any, onProgress?: (progress: number) => void, selectedFormat?: any): Promise<boolean> => {
    try {
      console.log('开始服务器下载音乐:', musicResult.title)

      onProgress?.(10)

      const response = await apiRequest(`${API_BASE_URL}/music/download-to-server`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          url: musicResult.url,
          title: musicResult.title,
          artist: musicResult.artist,
          platform: musicResult.platform,
          selectedFormat: selectedFormat,
          playlistName: musicResult.playlistName
        })
      })

      const result: ApiResponse<{ filePath: string; fileName: string }> = await response.json()

      if (result.success) {
        onProgress?.(100)
        console.log('服务器下载成功:', result.data.fileName)
        return true
      } else {
        console.error('服务器下载失败:', result.message)
        return false
      }
    } catch (error) {
      console.error('服务器下载异常:', error)
      return false
    }
  }

  /**
   * 智能下载音乐 - 根据设置选择下载方式
   */
  const downloadMusic = async (musicResult: any, onProgress?: (progress: number) => void, selectedFormat?: any): Promise<boolean> => {
    try {
      console.log('开始下载音乐:', musicResult.title)

      // 获取音乐设置
      onProgress?.(5)
      const settings = await getMusicSettings()

      if (settings.downloadLocation === 'server') {
        // 服务器下载
        return await downloadMusicToServer(musicResult, onProgress, selectedFormat)
      } else {
        // 本地下载（原有逻辑）
        return await downloadMusicToLocal(musicResult, onProgress, selectedFormat)
      }
    } catch (error) {
      console.error('下载音乐异常:', error)
      return false
    }
  }

  /**
   * 本地下载音乐（重命名原有函数）
   */
  const downloadMusicToLocal = async (musicResult: any, onProgress?: (progress: number) => void, selectedFormat?: any): Promise<boolean> => {
    try {
      console.log('开始本地下载音乐:', musicResult.title)

      // 检查是否需要格式转换（如果选择了特殊格式）
      const needsFormatConversion = selectedFormat && (
        selectedFormat.ext === 'flac' ||
        selectedFormat.ext === 'opus' ||
        selectedFormat.ext === 'aac' ||
            selectedFormat.note.includes('flac')||
        (selectedFormat.formatId && selectedFormat.formatId !== 'best')
      )

      if (needsFormatConversion) {
        // 使用后端yt-dlp进行流式下载（推荐）
        console.log('检测到需要格式转换，使用后端流式下载:', selectedFormat)
        return await downloadMusicWithStreamConversion(musicResult, onProgress, selectedFormat)
      } else {
        // 使用原有的直接下载方式
        return await downloadMusicDirectly(musicResult, onProgress, selectedFormat)
      }

    } catch (error) {
      console.error('本地下载异常:', error)
      return false
    }
  }

  /**
   * 使用后端yt-dlp进行流式下载（推荐使用，对服务器友好）
   */
  const downloadMusicWithStreamConversion = async (musicResult: any, onProgress?: (progress: number) => void, selectedFormat?: any): Promise<boolean> => {
    try {
      console.log('使用后端流式下载:', musicResult.title)

      onProgress?.(10)

      const response = await apiRequest(`${API_BASE_URL}/music/download-stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          url: musicResult.url,
          title: musicResult.title,
          artist: musicResult.artist,
          platform: musicResult.platform,
          selectedFormat: selectedFormat
        })
      })

      onProgress?.(30)

      if (response.ok) {
        // 从响应头获取文件名
        const contentDisposition = response.headers.get('Content-Disposition')
        let fileName = generateFileName(musicResult.title, musicResult.artist, selectedFormat)

        if (contentDisposition) {
          const fileNameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
          if (fileNameMatch && fileNameMatch[1]) {
            fileName = fileNameMatch[1].replace(/['"]/g, '')
          }
        }

        onProgress?.(50)

        // 获取响应流并创建Blob
        const blob = await response.blob()
        onProgress?.(90)

        // 创建下载链接
        const downloadUrl = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = downloadUrl
        link.download = fileName
        link.style.display = 'none'

        // 触发下载
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)

        // 清理URL对象
        setTimeout(() => {
          window.URL.revokeObjectURL(downloadUrl)
        }, 1000)

        onProgress?.(100)
        console.log('流式下载成功:', fileName)
        return true
      } else {
        console.error('后端流式下载失败:', response.status, response.statusText)
        return false
      }

    } catch (error) {
      console.error('流式下载异常:', error)
      return false
    }
  }

  /**
   * 使用后端yt-dlp进行格式转换下载（旧版本，保留作为备用）
   * @deprecated 推荐使用 downloadMusicWithStreamConversion，对服务器更友好
   */
  const downloadMusicWithFormatConversion = async (musicResult: any, onProgress?: (progress: number) => void, selectedFormat?: any): Promise<boolean> => {
    try {
      console.log('使用后端格式转换下载（旧版本）:', musicResult.title)

      onProgress?.(10)

      const response = await apiRequest(`${API_BASE_URL}/music/download-for-local`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          url: musicResult.url,
          title: musicResult.title,
          artist: musicResult.artist,
          platform: musicResult.platform,
          selectedFormat: selectedFormat
        })
      })

      onProgress?.(70)

      if (response.ok) {
        const blob = await response.blob()
        onProgress?.(90)

        // 生成文件名
        const fileName = generateFileName(musicResult.title, musicResult.artist, selectedFormat)

        // 创建下载链接
        const downloadUrl = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = downloadUrl
        link.download = fileName
        link.style.display = 'none'

        // 触发下载
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)

        // 清理URL对象
        setTimeout(() => {
          window.URL.revokeObjectURL(downloadUrl)
        }, 1000)

        onProgress?.(100)
        console.log('格式转换下载成功:', fileName)
        return true
      } else {
        console.error('后端下载失败:', response.status, response.statusText)
        return false
      }

    } catch (error) {
      console.error('格式转换下载异常:', error)
      return false
    }
  }

  /**
   * 直接下载音频文件（原有逻辑）
   */
  const downloadMusicDirectly = async (musicResult: any, onProgress?: (progress: number) => void, selectedFormat?: any): Promise<boolean> => {
    try {
      console.log('使用直接下载方式:', musicResult.title)

      // 1. 获取音频流URL
      onProgress?.(20)
      const audioUrl = await getAudioStreamByUrl(musicResult.url)
      if (!audioUrl) {
        console.error('无法获取音频流URL')
        return false
      }

      // 2. 获取可播放的音频URL（处理403问题）
      onProgress?.(40)
      const playableUrl = await getPlayableAudioUrl(audioUrl)
      if (!playableUrl) {
        console.error('无法获取可播放的音频URL')
        return false
      }

      // 3. 下载音频文件到本地
      onProgress?.(60)
      const success = await downloadAudioFile(playableUrl, musicResult.title, musicResult.artist, onProgress, selectedFormat)

      if (success) {
        onProgress?.(100)
        console.log('直接下载成功:', musicResult.title)
        return true
      } else {
        console.error('直接下载失败:', musicResult.title)
        return false
      }

    } catch (error) {
      console.error('直接下载异常:', error)
      return false
    }
  }

  /**
   * 下载音频文件到本地
   */
  const downloadAudioFile = async (
    audioUrl: string,
    title: string,
    artist: string,
    onProgress?: (progress: number) => void,
    selectedFormat?: any
  ): Promise<boolean> => {
    try {
      // 显示下载提示
      console.log('正在下载音频文件...')
      onProgress?.(70)

      // 获取音频数据
      const response = await fetch(audioUrl)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      onProgress?.(85)
      const blob = await response.blob()

      // 创建下载链接
      const downloadUrl = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = downloadUrl

      // 生成文件名
      const fileName = generateFileName(title, artist, selectedFormat)
      link.download = fileName

      // 设置链接样式（隐藏）
      link.style.display = 'none'

      // 触发下载
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)

      // 清理URL对象
      setTimeout(() => {
        window.URL.revokeObjectURL(downloadUrl)
      }, 1000)

      onProgress?.(95)
      return true
    } catch (error) {
      console.error('下载音频文件失败:', error)
      return false
    }
  }

  /**
   * 获取可用格式列表
   */
  const getAvailableFormats = async (videoUrl: string, platform: string): Promise<any[]> => {
    try {
      console.log('获取可用格式列表:', videoUrl, '平台:', platform)

      const response = await apiRequest(`${API_BASE_URL}/music/formats`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          url: videoUrl,
          platform: platform
        })
      })

      const result: ApiResponse<any[]> = await response.json()

      if (result.success) {
        console.log('获取到格式列表:', result.data)
        return result.data || []
      } else {
        console.error('获取格式列表失败:', result.message)
        throw new Error(result.message)
      }
    } catch (error) {
      console.error('获取格式列表异常:', error)
      throw error
    }
  }

  /**
   * 检测URL的平台
   */
  const detectPlatform = (url: string): string => {
    if (url.includes('bilibili.com') || url.includes('b23.tv')) {
      return 'bilibili'
    } else if (url.includes('youtube.com') || url.includes('youtu.be')) {
      return 'youtube'
    } else {
      // 默认返回bilibili，因为大部分情况下是bilibili
      return 'bilibili'
    }
  }

  /**
   * 生成下载文件名
   */
  const generateFileName = (title: string, artist: string, selectedFormat?: any): string => {
    // 清理文件名中的非法字符
    const cleanTitle = title.replace(/[<>:"/\\|?*]/g, '').trim()
    const cleanArtist = artist.replace(/[<>:"/\\|?*]/g, '').trim()

    // 根据选择的格式确定文件扩展名
    let extension = '.mp3' // 默认扩展名

    if (selectedFormat) {
      if (selectedFormat.isAudio) {
        // 音频格式处理
        if (selectedFormat.ext === 'flac' || selectedFormat.note.includes('flac')) {
          extension = '.flac'
        } else if (selectedFormat.ext === 'm4a') {
          // extension = '.m4a'
        } else if (selectedFormat.ext === 'webm') {
          extension = '.webm'
        } else if (selectedFormat.ext === 'ogg') {
          extension = '.ogg'
        } else if (selectedFormat.ext === 'aac') {
          extension = '.aac'
        } else if (selectedFormat.ext === 'opus') {
          extension = '.opus'
        } else {
          extension = '.mp3' // 音频默认mp3
        }
      } else if (selectedFormat.isVideo) {
        // 视频格式处理
        if (selectedFormat.ext === 'mp4') {
          extension = '.mp4'
        } else if (selectedFormat.ext === 'webm') {
          extension = '.webm'
        } else if (selectedFormat.ext === 'mkv') {
          extension = '.mkv'
        } else if (selectedFormat.ext === 'avi') {
          extension = '.avi'
        } else {
          extension = '.mp4' // 视频默认mp4
        }
      } else {
        // 如果格式类型不明确，根据扩展名判断
        extension = '.' + selectedFormat.ext
      }
    }

    // 生成文件名：艺术家 - 歌曲名.扩展名
    if (cleanArtist && cleanArtist !== cleanTitle) {
      return `${cleanArtist} - ${cleanTitle}${extension}`
    } else {
      return `${cleanTitle}${extension}`
    }
  }

  return {
    searchMusic,
    parsePlaylist,
    getSupportedPlatforms,
    getVideoDetail,
    getAudioStream,
    getAudioStreamByUrl,
    getProxyAudioUrl,
    getPlayableAudioUrl,
    getFallbackAudioUrl,
    getMusicSettings,
    downloadMusic,
    downloadMusicToLocal,
    downloadMusicToServer,
    downloadMusicWithStreamConversion,  // 新的流式下载方法（推荐）
    downloadMusicWithFormatConversion,  // 旧的下载方法（备用）
    downloadMusicDirectly,
    downloadAudioFile,
    getAvailableFormats,
    detectPlatform,
    generateFileName,
    processImageUrl
  }

  /**
   * 处理图片URL - 如果是代理URL则添加正确的基础地址
   */
  function processImageUrl(imageUrl: string): string {
    if (!imageUrl) return ''

    // 如果是相对路径的代理URL，使用getFullUrl处理
    if (imageUrl.startsWith('/api/music/proxy/image')) {
      const processedUrl = getFullUrl(imageUrl)
      console.log('处理图片URL:', {
        original: imageUrl,
        processed: processedUrl,
        apiBaseUrl: config.public.apiBaseUrl,
        isDevelopment: config.public.isDevelopment
      })
      return processedUrl
    }

    // 其他情况直接返回
    return imageUrl
  }
}
