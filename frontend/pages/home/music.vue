<script setup lang="ts">
import { ref, computed } from 'vue'
import { Icon } from '@iconify/vue'
import { useMusicApi, type MusicSearchResult } from '~/composables/useMusicApi'
import { useMusicState } from '~/composables/useMusicState'
import FormatSelector from '~/components/FormatSelector.vue'

// 使用音乐状态管理
const {
  // 状态
  searchResults,
  selectedResults,
  downloadQueue,
  downloadProgress,
  currentPlaying,
  isPlaying,
  isLoading,
  audioElement,
  currentTime,
  totalDuration,
  volume,
  searchQuery,
  searchType,
  platform,
  isSearching,
  searchError,

  // 计算属性
  hasResults,
  hasSelectedItems,
  playProgress,

  // 方法
  setSearchResults,
  clearSearchResults,
  toggleSelection,
  toggleSelectAll,
  addToDownloadQueue,
  removeFromQueue,
  clearDownloadQueue,
  setSearchQuery,
  setSearchType,
  setPlatform,
  setSearching,
  setSearchError,
  setCurrentPlaying,
  setPlaying,
  setLoading,
  setAudioElement,
  setCurrentTime,
  setTotalDuration,
  setVolume,
  setDownloadProgress,
  removeDownloadProgress,
  clearPlayingState,
  clearAllState
} = useMusicState()

// 获取独立搜索状态
const {
  keywordSearchQuery,
  urlSearchQuery,
  playlistSearchQuery,
  keywordSearchResults,
  urlSearchResults,
  playlistSearchResults,
  keywordSearching,
  urlSearching,
  playlistSearching,
  keywordSearchError,
  urlSearchError,
  playlistSearchError,
  setKeywordSearchQuery,
  setUrlSearchQuery,
  setPlaylistSearchQuery,
  setKeywordSearchResults,
  setUrlSearchResults,
  setPlaylistSearchResults,
  setKeywordSearching,
  setUrlSearching,
  setPlaylistSearching,
  setKeywordSearchError,
  setUrlSearchError,
  setPlaylistSearchError
} = useMusicState()

// 音乐API
const { searchMusic, downloadMusic, getAudioStreamByUrl, getPlayableAudioUrl, getFallbackAudioUrl, parsePlaylist, getSupportedPlatforms, getAvailableFormats, detectPlatform } = useMusicApi()

// 下载相关状态
const isDownloading = ref(false)
const isPaused = ref(false)
const downloadControllers = ref(new Map()) // 存储下载控制器

// 歌单解析相关状态
const isParsingPlaylist = ref(false)
const playlistInfo = ref(null)
const playlistError = ref('')

// 自动匹配相关状态
const isAutoMatching = ref(false)
const matchingProgress = ref<Record<string, number>>({})
const matchingError = ref('')

// 格式选择相关状态
const showFormatSelector = ref(false)
const currentDownloadItem = ref<MusicSearchResult | null>(null)

// 注意：独立搜索状态现在从 useMusicState() 获取，已在上面导入

// 计算属性：获取当前搜索内容
const currentSearchQuery = computed(() => {
  switch (searchType.value) {
    case 'keyword':
      return keywordSearchQuery.value
    case 'url':
      return urlSearchQuery.value
    case 'playlist':
      return playlistSearchQuery.value
    default:
      return ''
  }
})

// 计算属性：获取当前搜索结果
const currentSearchResults = computed(() => {
  switch (searchType.value) {
    case 'keyword':
      return keywordSearchResults.value
    case 'url':
      return urlSearchResults.value
    case 'playlist':
      return playlistSearchResults.value
    default:
      return []
  }
})

// 计算属性：获取当前搜索状态
const currentSearching = computed(() => {
  switch (searchType.value) {
    case 'keyword':
      return keywordSearching.value
    case 'url':
      return urlSearching.value
    case 'playlist':
      return playlistSearching.value
    default:
      return false
  }
})

// 计算属性：获取当前搜索错误
const currentSearchError = computed(() => {
  switch (searchType.value) {
    case 'keyword':
      return keywordSearchError.value
    case 'url':
      return urlSearchError.value
    case 'playlist':
      return playlistSearchError.value
    default:
      return ''
  }
})

// 计算属性：是否有搜索结果
const currentHasResults = computed(() => {
  return currentSearchResults.value.length > 0
})

// 计算属性：当前搜索结果是否全选
const isCurrentAllSelected = computed(() => {
  const currentResults = currentSearchResults.value
  if (currentResults.length === 0) return false

  return currentResults.every(item => selectedResults.value.has(item.id))
})

// 计算属性：验证URL有效性
const isValidUrl = computed(() => {
  if (searchType.value === 'keyword') return true
  const url = currentSearchQuery.value.trim()

  if (searchType.value === 'url') {
    return url.includes('bilibili.com') || url.includes('youtube.com') || url.includes('youtu.be')
  }

  if (searchType.value === 'playlist') {
    return url.includes('y.qq.com') || url.includes('music.163.com')
  }

  return true
})

// 搜索方法
const handleSearch = async () => {
  if (!currentSearchQuery.value.trim()) return

  // 如果是歌单解析，调用歌单解析方法
  if (searchType.value === 'playlist') {
    await handlePlaylistParse()
    return
  }

  updateCurrentSearching(true)
  updateCurrentSearchError('')

  try {
    // 构建搜索请求
    const searchRequest = {
      query: currentSearchQuery.value.trim(),
      searchType: searchType.value,
      platform: platform.value,
      page: 1,
      pageSize: 20
    }

    // 使用音乐API搜索
    const results = await searchMusic(searchRequest)

    updateCurrentSearchResults(results)
    updateCurrentSearchError('')

  } catch (error: any) {
    console.error('搜索请求失败:', error)
    updateCurrentSearchError(error.message || '网络请求失败，请检查网络连接')
    updateCurrentSearchResults([])
  } finally {
    updateCurrentSearching(false)
  }
}

// 歌单解析方法
const handlePlaylistParse = async () => {
  if (!currentSearchQuery.value.trim()) return

  updateCurrentSearching(true)
  updateCurrentSearchError('')
  playlistInfo.value = null

  try {
    const playlist = await parsePlaylist({
      url: currentSearchQuery.value.trim(),
      platform: 'auto'
    })

    playlistInfo.value = playlist

    // 将歌单中的歌曲转换为搜索结果格式
    const results = playlist.songs.map((song, index) => ({
      id: `${playlist.source}_${song.sourceId}_${index}`,
      title: song.title,
      artist: song.artist,
      duration: song.duration || '未知',
      platform: playlist.source,
      thumbnail: song.cover || playlist.cover,
      url: song.url,
      quality: '音频',
      playCount: '',
      publishTime: '',
      description: `来自歌单: ${playlist.title}`,
      tags: []
    }))

    updateCurrentSearchResults(results)
    updateCurrentSearchError('')

    showNotification(`成功解析歌单: ${playlist.title}，共 ${playlist.songCount} 首歌曲`, 'success')

  } catch (error: any) {
    console.error('歌单解析失败:', error)
    playlistError.value = error.message || '歌单解析失败，请检查链接是否正确'
    updateCurrentSearchResults([])
    updateCurrentSearchError(error.message || '歌单解析失败')
  } finally {
    updateCurrentSearching(false)
  }
}

// 批量添加歌单到下载队列
// const addPlaylistToQueue = () => {
//   if (!playlistInfo.value) return
//
//   const results = searchResults.value
//   results.forEach(result => {
//     if (!downloadQueue.value.find(item => item.id === result.id)) {
//       addToDownloadQueue(result)
//     }
//   })
//
//   showNotification(`已添加 ${results.length} 首歌曲到下载队列，将通过B站搜索下载`, 'success')
// }

// 批量下载歌单歌曲
const startPlaylistBatchDownload = async () => {
  if (!playlistInfo.value) return

  const playlistSongs = downloadQueue.value.filter(item => isPlaylistSong(item))

  if (playlistSongs.length === 0) {
    showNotification('下载队列中没有歌单歌曲', 'error')
    return
  }

  showNotification(`开始批量下载 ${playlistSongs.length} 首歌曲，将通过B站搜索最佳资源`, 'success')

  // 逐个下载，避免并发过多
  for (const song of playlistSongs) {
    if (downloadProgress.value[song.id] === undefined) {
      // 对于批量下载，使用默认格式（音频优先flac，如果没有则mp3）
      const defaultFormat = { isAudio: true, ext: 'mp3' }
      await startDownload(song, defaultFormat)
      // 每首歌之间间隔1秒，避免请求过于频繁
      await new Promise(resolve => setTimeout(resolve, 1000))
    }
  }
}

// 这些方法现在由 useMusicState 提供，无需重新定义

// 检查是否为歌单来源的歌曲
const isPlaylistSong = (item: MusicSearchResult) => {
  return item.description && item.description.includes('来自歌单:')
}

// 通过B站搜索下载歌单歌曲
const downloadPlaylistSong = async (item: MusicSearchResult, onProgress?: (progress: number) => void, abortSignal?: AbortSignal) => {
  try {
    console.log('开始搜索B站资源:', item.title, item.artist)

    // 构建多个搜索关键词，提高匹配成功率
    const searchKeywords = [
      `${item.artist} ${item.title}`,  // 歌手 + 歌曲名
      `${item.title} ${item.artist}`,  // 歌曲名 + 歌手
      item.title,                      // 仅歌曲名
      `${item.title} 音乐`,            // 歌曲名 + 音乐
      `${item.artist} ${item.title} 官方` // 歌手 + 歌曲名 + 官方
    ]

    let bestResult = null
    let allResults: any[] = []

    // 尝试多个搜索关键词
    for (const keyword of searchKeywords) {
      try {
        const searchRequest = {
          query: keyword.trim(),
          searchType: 'keyword' as const,
          platform: 'bilibili' as const,
          page: 1,
          pageSize: 5
        }

        const searchResults = await searchMusic(searchRequest)
        allResults.push(...searchResults)

        if (searchResults.length > 0) {
          console.log(`关键词 "${keyword}" 找到 ${searchResults.length} 个结果`)
          break // 找到结果就停止搜索
        }
      } catch (error) {
        console.log(`关键词 "${keyword}" 搜索失败:`, error)
        continue
      }
    }

    if (allResults.length === 0) {
      throw new Error('未找到相关资源')
    }

    // 去重并选择播放量最多的结果
    const uniqueResults = allResults.filter((result, index, self) =>
      index === self.findIndex(r => r.id === result.id)
    )

    bestResult = uniqueResults.reduce((best, current) => {
      const bestPlayCount = parseInt(best.playCount?.replace(/[^\d]/g, '') || '0')
      const currentPlayCount = parseInt(current.playCount?.replace(/[^\d]/g, '') || '0')
      return currentPlayCount > bestPlayCount ? current : best
    })

    console.log('选择最佳结果:', bestResult.title, '播放量:', bestResult.playCount)

    // 使用找到的B站资源进行下载
    const result = await downloadMusic(bestResult)

    if (result) {
      showNotification(`下载完成: ${item.title} (通过B站: ${bestResult.title})`, 'success')
      return true
    } else {
      throw new Error('下载失败')
    }

  } catch (error: any) {
    console.error('B站搜索下载失败:', error)
    showNotification(`搜索下载失败: ${item.title} - ${error.message}`, 'error')
    return false
  }
}

// 显示格式选择弹窗
const showFormatSelection = (item: MusicSearchResult) => {
  // 确保平台信息正确，如果没有平台信息则根据URL检测
  if (!item.platform) {
    item.platform = detectPlatform(item.url)
  }
  currentDownloadItem.value = item
  showFormatSelector.value = true
}

// 处理格式选择
const handleFormatSelect = async (format: any) => {
  showFormatSelector.value = false

  if (currentDownloadItem.value) {
    // 将选择的格式信息传递给下载函数
    await startDownload(currentDownloadItem.value, format)
    currentDownloadItem.value = null
  }
}

// 关闭格式选择弹窗
const closeFormatSelector = () => {
  showFormatSelector.value = false
  currentDownloadItem.value = null
}

// 开始下载
const startDownload = async (item: MusicSearchResult, selectedFormat?: any) => {
  // 检查是否已暂停
  if (isPaused.value) {
    console.log('下载已暂停，跳过:', item.title)
    return false
  }

  setDownloadProgress(item.id, 0)

  // 创建下载控制器
  const controller = new AbortController()
  downloadControllers.value.set(item.id, controller)

  try {
    console.log('开始下载:', item.title)

    let result = false

    // 如果是歌单来源的歌曲，使用B站搜索下载
    if (isPlaylistSong(item)) {
      result = await downloadPlaylistSong(item, (progress) => {
        if (!isPaused.value && !controller.signal.aborted) {
          setDownloadProgress(item.id, progress)
        }
      }, controller.signal)
    } else {
      // 普通搜索结果，使用原有下载方式
      result = await downloadMusic(item, (progress) => {
        if (!isPaused.value && !controller.signal.aborted) {
          setDownloadProgress(item.id, progress)
        }
      }, selectedFormat)

      // 如果不是歌单歌曲，显示原有的成功消息
      if (result && !isPaused.value) {
        const { getMusicSettings } = useMusicApi()
        const settings = await getMusicSettings()

        if (settings.downloadLocation === 'server') {
          showNotification(`服务器下载完成: ${item.title}`, 'success')
        } else {
          showNotification(`本地下载完成: ${item.title}`, 'success')
        }
      }
    }

    if (result && !isPaused.value) {
      // 下载成功，设置进度为100%
      setDownloadProgress(item.id, 100)

      // 3秒后从下载队列中移除
      setTimeout(() => {
        removeFromQueue(item.id)
        downloadControllers.value.delete(item.id)
      }, 3000)

      console.log('下载完成:', item.title)
    } else if (isPaused.value || controller.signal.aborted) {
      console.log('下载被暂停或取消:', item.title)
      return false
    } else {
      // 下载失败，移除进度
      removeDownloadProgress(item.id)
      downloadControllers.value.delete(item.id)
      console.error('下载失败:', item.title)

      if (!isPlaylistSong(item)) {
        showNotification(`下载失败: ${item.title}`, 'error')
      }
    }

    return result
  } catch (error: any) {
    console.error('下载异常:', error)

    // 如果是暂停导致的错误，不显示错误提示
    if (error.name === 'AbortError' || isPaused.value) {
      console.log('下载被暂停:', item.title)
      return false
    }

    removeDownloadProgress(item.id)
    downloadControllers.value.delete(item.id)

    if (!isPlaylistSong(item)) {
      showNotification(`下载异常: ${item.title}`, 'error')
    }

    return false
  }
}

// 简单的通知功能
const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
  if (!process.client) return

  // 创建通知元素
  const notification = document.createElement('div')
  notification.textContent = message
  notification.style.cssText = `
    position: fixed;
    top: 20px;
    right: 20px;
    padding: 12px 20px;
    border-radius: 8px;
    color: white;
    font-size: 14px;
    font-weight: 500;
    z-index: 10000;
    max-width: 300px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    backdrop-filter: blur(10px);
    transition: all 0.3s ease;
    ${type === 'success'
      ? 'background: linear-gradient(135deg, rgba(34, 197, 94, 0.9), rgba(34, 197, 94, 0.8));'
      : 'background: linear-gradient(135deg, rgba(239, 68, 68, 0.9), rgba(239, 68, 68, 0.8));'
  }
  `

  document.body.appendChild(notification)

  // 3秒后自动移除
  setTimeout(() => {
    notification.style.opacity = '0'
    notification.style.transform = 'translateX(100%)'
    setTimeout(() => {
      if (notification.parentNode) {
        document.body.removeChild(notification)
      }
    }, 300)
  }, 3000)
}



// 播放音乐
const playMusic = async (result: MusicSearchResult) => {
  try {
    // 如果正在播放同一首歌，则暂停/继续
    if (currentPlaying.value?.id === result.id) {
      if (isPlaying.value) {
        pauseMusic()
      } else {
        resumeMusic()
      }
      return
    }

    // 停止当前播放
    if (audioElement.value) {
      audioElement.value.pause()
      setAudioElement(null)
    }

    // 立即设置当前播放和加载状态，让播放器立即显示
    setCurrentPlaying(result)
    setLoading(true)
    setPlaying(false)

    // 获取音频流URL
    const originalAudioUrl = await getAudioStreamByUrl(result.url)

    if (!originalAudioUrl) {
      console.error('无法获取音频流')
      setLoading(false)
      setCurrentPlaying(null)
      return
    }

    // 获取可播放的音频URL（如果原始URL失败会自动使用代理）
    const playableAudioUrl = await getPlayableAudioUrl(originalAudioUrl)

    if (!playableAudioUrl) {
      console.error('无法获取可播放的音频流')
      setLoading(false)
      setCurrentPlaying(null)
      return
    }

    // 创建音频元素
    const audio = new Audio(playableAudioUrl)
    setAudioElement(audio)

    // 设置音频事件监听
    audio.addEventListener('loadedmetadata', () => {
      setTotalDuration(audio.duration)
    })

    audio.addEventListener('timeupdate', () => {
      setCurrentTime(audio.currentTime)
    })

    audio.addEventListener('ended', () => {
      setPlaying(false)
      setCurrentTime(0)
    })

    audio.addEventListener('error', async (e) => {
      console.error('音频播放错误:', e)

      // 如果是第一次加载失败，尝试使用代理URL
      if (!audio.src.includes('/proxy/audio-stream')) {
        try {
          console.log('尝试使用代理URL重新加载音频')
          const fallbackUrl = getFallbackAudioUrl(originalAudioUrl)
          audio.src = fallbackUrl
          audio.load()
          await audio.play()
          setPlaying(true)
          return
        } catch (fallbackError) {
          console.error('代理URL也加载失败:', fallbackError)
        }
      }

      // 如果代理URL也失败了，则停止播放
      setPlaying(false)
      setLoading(false)
      setCurrentPlaying(null)
    })

    // 开始播放
    audio.volume = volume.value
    await audio.play()
    setPlaying(true)

  } catch (error) {
    console.error('播放音乐失败:', error)
    setCurrentPlaying(null)
  } finally {
    setLoading(false)
  }
}

// 暂停音乐
const pauseMusic = () => {
  if (audioElement.value) {
    audioElement.value.pause()
    setPlaying(false)
  }
}

// 继续播放
const resumeMusic = () => {
  if (audioElement.value) {
    audioElement.value.play()
    setPlaying(true)
  }
}

// 停止音乐并关闭播放器
const stopMusic = () => {
  // 使用 clearPlayingState 方法来清空播放相关的状态和缓存
  clearPlayingState()
}

// 设置播放进度
const setProgress = (progress: number) => {
  if (audioElement.value && totalDuration.value > 0) {
    audioElement.value.currentTime = (progress / 100) * totalDuration.value
  }
}

// 设置音量
const setVolumeValue = (newVolume: number) => {
  setVolume(newVolume)
  if (audioElement.value) {
    audioElement.value.volume = newVolume
  }
}

// 格式化时间
const formatTime = (seconds: number) => {
  if (isNaN(seconds)) return '0:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

// 获取下载按钮文本
const getDownloadButtonText = (result: MusicSearchResult) => {
  const progress = downloadProgress.value[result.id]
  if (progress !== undefined) {
    if (progress === 100) {
      return '已完成'
    } else {
      return `${Math.round(progress)}%`
    }
  }
  return '下载'
}

// 获取下载按钮标题
const getDownloadButtonTitle = (result: MusicSearchResult) => {
  const progress = downloadProgress.value[result.id]
  if (progress !== undefined) {
    if (progress === 100) {
      return '下载完成'
    } else {
      return `正在下载... ${Math.round(progress)}%`
    }
  }
  return '立即下载'
}

// 获取搜索占位符文本
const getSearchPlaceholder = () => {
  switch (searchType.value) {
    case 'keyword':
      return '输入歌曲名称、歌手或关键词...'
    case 'url':
      return '粘贴视频链接...'
    case 'playlist':
      return '粘贴歌单链接...'
    default:
      return '输入搜索内容...'
  }
}

// 获取搜索按钮文本
const getSearchButtonText = () => {
  if (isSearching.value) return '搜索中...'
  if (isParsingPlaylist.value) return '解析中...'

  switch (searchType.value) {
    case 'playlist':
      return '解析歌单'
    default:
      return '搜索'
  }
}

// 获取URL提示文本
const getUrlHintText = () => {
  switch (searchType.value) {
    case 'url':
      return '请输入有效的哔哩哔哩或YouTube链接'
    case 'playlist':
      return '请输入有效的QQ音乐或网易云音乐歌单链接'
    default:
      return '请输入有效的链接'
  }
}

// 检查下载队列中是否有歌单歌曲
const hasPlaylistSongs = computed(() => {
  return downloadQueue.value.some(item => isPlaylistSong(item))
})

// 暂停所有下载
const pauseAllDownloads = () => {
  isPaused.value = true
  isDownloading.value = false

  // 暂停所有正在进行的下载
  downloadControllers.value.forEach((controller, songId) => {
    if (controller && !controller.signal.aborted) {
      controller.abort()
    }
  })

  showNotification('已暂停所有下载任务', 'info')
  console.log('暂停所有下载，共暂停', downloadControllers.value.size, '个任务')
}

// 恢复所有下载
const resumeAllDownloads = () => {
  isPaused.value = false

  // 重新开始未完成的下载
  const unfinishedSongs = downloadQueue.value.filter(item => {
    const progress = downloadProgress.value[item.id]
    return progress === undefined || (progress < 100 && progress > 0)
  })

  if (unfinishedSongs.length > 0) {
    showNotification(`恢复下载 ${unfinishedSongs.length} 首歌曲`, 'success')
    startBatchDownload()
  } else {
    showNotification('没有需要恢复的下载任务', 'info')
  }
}

// 自动匹配选中的歌曲
const autoMatchMusic = async () => {
  const selectedSongs = currentSearchResults.value.filter(result => selectedResults.value.has(result.id))

  if (selectedSongs.length === 0) {
    showNotification('请先选择要匹配的歌曲', 'error')
    return
  }

  isAutoMatching.value = true
  matchingError.value = ''
  matchingProgress.value = {}

  try {
    showNotification(`开始自动匹配 ${selectedSongs.length} 首歌曲，将在B站搜索最佳资源`, 'success')

    // 逐个匹配歌曲
    for (let i = 0; i < selectedSongs.length; i++) {
      const song = selectedSongs[i]

      if (!isAutoMatching.value) break // 检查是否被取消

      try {
        // 设置匹配进度
        matchingProgress.value[song.id] = 0

        console.log(`开始匹配第 ${i + 1}/${selectedSongs.length} 首: ${song.title} - ${song.artist}`)

        // 构建多个搜索关键词，提高匹配成功率
        const searchKeywords = [
          `${song.artist} ${song.title}`,  // 歌手 + 歌曲名
          `${song.title} ${song.artist}`,  // 歌曲名 + 歌手
          song.title,                      // 仅歌曲名
          `${song.title} 音乐`,            // 歌曲名 + 音乐
          `${song.artist} ${song.title} 官方` // 歌手 + 歌曲名 + 官方
        ]

        let bestResult = null
        let allResults: any[] = []

        // 尝试多个搜索关键词
        for (const keyword of searchKeywords) {
          try {
            const searchRequest = {
              query: keyword.trim(),
              searchType: 'keyword' as const,
              platform: 'bilibili' as const,
              page: 1,
              pageSize: 5
            }

            const searchResults = await searchMusic(searchRequest)
            allResults.push(...searchResults)

            if (searchResults.length > 0) {
              console.log(`关键词 "${keyword}" 找到 ${searchResults.length} 个结果`)
              break // 找到结果就停止搜索
            }
          } catch (error) {
            console.log(`关键词 "${keyword}" 搜索失败:`, error)
            continue
          }
        }

        matchingProgress.value[song.id] = 50

        if (allResults.length === 0) {
          console.warn(`未找到匹配结果: ${song.title} - ${song.artist}`)
          matchingProgress.value[song.id] = 100
          continue
        }

        // 去重并选择播放量最多的结果
        const uniqueResults = allResults.filter((result, index, self) =>
          index === self.findIndex(r => r.id === result.id)
        )

        bestResult = uniqueResults.reduce((best, current) => {
          // 获取音质优先级
          const bestQualityPriority = getAudioQualityPriority(best.title || '')
          const currentQualityPriority = getAudioQualityPriority(current.title || '')

          // 如果音质优先级不同，选择优先级更高的
          if (currentQualityPriority !== bestQualityPriority) {
            return currentQualityPriority > bestQualityPriority ? current : best
          }

          // 如果音质优先级相同，则按播放量比较
          const bestPlayCount = parsePlayCount(best.playCount || '0')
          const currentPlayCount = parsePlayCount(current.playCount || '0')
          return currentPlayCount > bestPlayCount ? current : best
        })

        console.log(`匹配成功: ${song.title} -> ${bestResult.title} (播放量: ${bestResult.playCount})`)

        // 更新当前搜索结果中的信息
        const originalIndex = currentSearchResults.value.findIndex(r => r.id === song.id)
        if (originalIndex !== -1) {
          // 保存当前选中状态
          const currentSelectedIds = Array.from(selectedResults.value)

          // 创建新的搜索结果数组，保留原始的一些信息，但更新关键信息
          const updatedResults = [...currentSearchResults.value]
          updatedResults[originalIndex] = {
            ...updatedResults[originalIndex],
            duration: bestResult.duration,
            url: bestResult.url,
            thumbnail: bestResult.thumbnail,
            playCount: bestResult.playCount,
            publishTime: bestResult.publishTime,
            description: `${updatedResults[originalIndex].description || ''} (已匹配B站: ${bestResult.title})`.trim()
          }

          // 使用当前搜索结果更新方法
          updateCurrentSearchResults(updatedResults)

          // 恢复选中状态 - 使用提供的方法而不是直接操作Set
          currentSelectedIds.forEach(id => {
            if (!selectedResults.value.has(id)) {
              toggleSelection(id)
            }
          })
        }

        matchingProgress.value[song.id] = 100

        // 每首歌之间间隔1秒，避免请求过于频繁
        if (i < selectedSongs.length - 1) {
          await new Promise(resolve => setTimeout(resolve, 1000))
        }

      } catch (error: any) {
        console.error(`匹配失败: ${song.title} - ${song.artist}`, error)
        matchingProgress.value[song.id] = 100
      }
    }

    const successCount = Object.values(matchingProgress.value).filter(p => p === 100).length
    showNotification(`自动匹配完成，成功匹配 ${successCount}/${selectedSongs.length} 首歌曲`, 'success')

  } catch (error: any) {
    console.error('自动匹配过程中发生错误:', error)
    matchingError.value = error.message || '自动匹配失败'
    showNotification('自动匹配失败: ' + matchingError.value, 'error')
  } finally {
    isAutoMatching.value = false
    // 3秒后清除匹配进度
    setTimeout(() => {
      matchingProgress.value = {}
    }, 3000)
  }
}

// 取消自动匹配
const cancelAutoMatch = () => {
  isAutoMatching.value = false
  showNotification('已取消自动匹配', 'info')
}

// 更新当前搜索内容
const updateCurrentSearchQuery = (value: string) => {
  switch (searchType.value) {
    case 'keyword':
      setKeywordSearchQuery(value)
      break
    case 'url':
      setUrlSearchQuery(value)
      break
    case 'playlist':
      setPlaylistSearchQuery(value)
      break
  }
}

// 更新当前搜索状态
const updateCurrentSearching = (value: boolean) => {
  switch (searchType.value) {
    case 'keyword':
      setKeywordSearching(value)
      break
    case 'url':
      setUrlSearching(value)
      break
    case 'playlist':
      setPlaylistSearching(value)
      break
  }
}

// 更新当前搜索结果
const updateCurrentSearchResults = (results: MusicSearchResult[]) => {
  switch (searchType.value) {
    case 'keyword':
      setKeywordSearchResults(results)
      break
    case 'url':
      setUrlSearchResults(results)
      break
    case 'playlist':
      setPlaylistSearchResults(results)
      break
  }
}

// 更新当前搜索错误
const updateCurrentSearchError = (error: string) => {
  switch (searchType.value) {
    case 'keyword':
      setKeywordSearchError(error)
      break
    case 'url':
      setUrlSearchError(error)
      break
    case 'playlist':
      setPlaylistSearchError(error)
      break
  }
}

// 清空当前搜索结果
const clearCurrentSearchResults = () => {
  updateCurrentSearchResults([])
  updateCurrentSearchError('')
  // 清空选中状态 - 使用提供的方法
  if (selectedResults.value.size > 0) {
    // 获取所有选中的ID并逐个取消选择
    const selectedIds = Array.from(selectedResults.value)
    selectedIds.forEach(id => {
      if (selectedResults.value.has(id)) {
        toggleSelection(id)
      }
    })
  }
}

// 解析播放量字符串为数字
const parsePlayCount = (playCountStr: string): number => {
  if (!playCountStr || playCountStr === '0') return 0

  const str = playCountStr.toString().toLowerCase().trim()

  // 处理中文单位
  if (str.includes('万')) {
    const num = parseFloat(str.replace(/[万,]/g, ''))
    return Math.floor(num * 10000)
  } else if (str.includes('千')) {
    const num = parseFloat(str.replace(/[千,]/g, ''))
    return Math.floor(num * 1000)
  } else if (str.includes('亿')) {
    const num = parseFloat(str.replace(/[亿,]/g, ''))
    return Math.floor(num * 100000000)
  }

  // 处理英文单位
  if (str.includes('k')) {
    const num = parseFloat(str.replace(/[k,]/g, ''))
    return Math.floor(num * 1000)
  } else if (str.includes('m')) {
    const num = parseFloat(str.replace(/[m,]/g, ''))
    return Math.floor(num * 1000000)
  } else if (str.includes('b')) {
    const num = parseFloat(str.replace(/[b,]/g, ''))
    return Math.floor(num * 1000000000)
  }

  // 处理纯数字（可能包含逗号）
  const cleanStr = str.replace(/[,，]/g, '')
  const num = parseInt(cleanStr.replace(/[^\d.]/g, ''))
  return isNaN(num) ? 0 : num
}

// 获取音质优先级（数字越大优先级越高）
const getAudioQualityPriority = (title: string): number => {
  if (!title) return 0

  const titleLower = title.toLowerCase()

  // 高优先级关键词（无损音质）
  if (title.includes('无损') || titleLower.includes('flac') || titleLower.includes('lossless')) {
    return 100
  }

  // 中高优先级关键词（高品质）
  if (title.includes('高品质') || title.includes('高音质') || titleLower.includes('hq') ||
      titleLower.includes('320k') || titleLower.includes('320kbps')) {
    return 80
  }

  // 中等优先级关键词（官方版本）
  if (title.includes('官方') || title.includes('正版') || titleLower.includes('official')) {
    return 60
  }

  // 中等优先级关键词（高清）
  if (title.includes('高清') || titleLower.includes('hd') || titleLower.includes('1080p') ||
      titleLower.includes('4k')) {
    return 50
  }

  // 低优先级关键词（可能影响音质）
  if (title.includes('翻唱') || title.includes('伴奏') || title.includes('卡拉OK') ||
      titleLower.includes('cover') || titleLower.includes('karaoke')) {
    return -20
  }

  // 默认优先级
  return 0
}

// 当前搜索结果的全选/取消全选
const toggleCurrentSelectAll = () => {
  const currentResults = currentSearchResults.value
  const currentSelectedCount = currentResults.filter(item => selectedResults.value.has(item.id)).length

  if (currentSelectedCount === currentResults.length) {
    // 当前全部选中，取消全选
    currentResults.forEach(item => {
      if (selectedResults.value.has(item.id)) {
        toggleSelection(item.id)
      }
    })
  } else {
    // 未全选，选中所有
    currentResults.forEach(item => {
      if (!selectedResults.value.has(item.id)) {
        toggleSelection(item.id)
      }
    })
  }
}

// 自定义搜索类型切换函数
const handleSearchTypeChange = (type: 'keyword' | 'url' | 'playlist') => {
  // 如果从歌单模式切换到其他模式，清理自动匹配相关状态
  if (searchType.value === 'playlist' && type !== 'playlist') {
    isAutoMatching.value = false
    matchingProgress.value = {}
    matchingError.value = ''
    playlistInfo.value = null
    playlistError.value = ''
  }

  // 清空选中的结果（因为不同搜索类型的结果不同）
  if (selectedResults.value.size > 0) {
    // 获取所有选中的ID并逐个取消选择
    const selectedIds = Array.from(selectedResults.value)
    selectedIds.forEach(id => {
      if (selectedResults.value.has(id)) {
        toggleSelection(id)
      }
    })
  }

  // 设置新的搜索类型
  setSearchType(type)
}

// 智能批量下载
const startBatchDownload = async () => {
  if (downloadQueue.value.length === 0) return
  if (isPaused.value) {
    showNotification('下载已暂停，请先恢复下载', 'info')
    return
  }

  isDownloading.value = true

  const playlistSongs = downloadQueue.value.filter(item => isPlaylistSong(item))
  const regularSongs = downloadQueue.value.filter(item => !isPlaylistSong(item))

  if (playlistSongs.length > 0) {
    showNotification(`开始智能下载 ${playlistSongs.length} 首歌单歌曲 + ${regularSongs.length} 首普通歌曲`, 'success')
  }

  try {
    // 先下载普通歌曲
    for (const song of regularSongs) {
      if (isPaused.value) break
      if (downloadProgress.value[song.id] === undefined) {
        // 对于批量下载，使用默认格式
        const defaultFormat = { isAudio: true, ext: 'mp3' }
        startDownload(song, defaultFormat)
      }
    }

    // 再逐个下载歌单歌曲（需要搜索，所以串行处理）
    for (const song of playlistSongs) {
      if (isPaused.value) break
      if (downloadProgress.value[song.id] === undefined) {
        // 对于批量下载，使用默认格式
        const defaultFormat = { isAudio: true, ext: 'mp3' }
        await startDownload(song, defaultFormat)
        // 每首歌之间间隔1秒，避免请求过于频繁
        if (!isPaused.value) {
          await new Promise(resolve => setTimeout(resolve, 1000))
        }
      }
    }
  } finally {
    if (!isPaused.value) {
      isDownloading.value = false
    }
  }
}
</script>

<template>
  <NuxtLayout>
    <div class="music-container">
      <!-- 页面标题 -->
      <div class="page-header">
<!--        <div class="header-left">-->
<!--          <h1 class="page-title">音乐下载</h1>-->
<!--          <p class="page-description">从哔哩哔哩和YouTube搜索并下载音乐</p>-->
<!--        </div>-->

      </div>

      <!-- 搜索区域 -->
      <div class="search-section">
        <div class="search-card">
          <div class="card-header">
            <Icon icon="mdi:music-note-plus" class="card-icon" />
            <div class="card-title-section">
              <h3 class="card-title">搜索音乐</h3>
              <p class="card-subtitle">支持关键词搜索和直链下载</p>
            </div>

            <!-- 搜索类型选择和操作按钮 -->
            <div class="search-type-tabs">
              <div class="tab-buttons">
                <button
                    @click="handleSearchTypeChange('keyword')"
                    :class="['tab-btn', { active: searchType === 'keyword' }]"
                >
                  <Icon icon="mdi:magnify" />
                  关键词搜索
                </button>
                <button
                    @click="handleSearchTypeChange('url')"
                    :class="['tab-btn', { active: searchType === 'url' }]"
                >
                  <Icon icon="mdi:link" />
                  链接下载
                </button>
                <button
                    @click="handleSearchTypeChange('playlist')"
                    :class="['tab-btn', { active: searchType === 'playlist' }]"
                >
                  <Icon icon="mdi:playlist-music" />
                  歌单解析
                </button>
              </div>

              <div class="tab-actions">
                <button
                    v-if="currentHasResults"
                    @click="clearCurrentSearchResults"
                    class="clear-btn"
                >
                  <Icon icon="mdi:broom" class="btn-icon" />
                  清空结果
                </button>
              </div>
            </div>
          </div>

          <div class="card-content">


            <!-- 平台选择 -->
            <div v-if="searchType === 'keyword'" class="platform-selection">
              <label class="platform-label">搜索平台：</label>
              <div class="platform-buttons">
                <button
                  @click="setPlatform('both')"
                  :class="['platform-btn', { active: platform === 'both' }]"
                >
                  <Icon icon="mdi:earth" />
                  全部
                </button>
                <button
                  @click="setPlatform('bilibili')"
                  :class="['platform-btn', { active: platform === 'bilibili' }]"
                >
                  <Icon icon="simple-icons:bilibili" />
                  哔哩哔哩
                </button>
                <button
                  @click="setPlatform('youtube')"
                  :class="['platform-btn', { active: platform === 'youtube' }]"
                >
                  <Icon icon="mdi:youtube" />
                  YouTube
                </button>
              </div>
            </div>

            <!-- 搜索输入框 -->
            <div class="search-input-section">
              <div class="search-input-wrapper">
                <input
                  :value="currentSearchQuery"
                  @input="updateCurrentSearchQuery($event.target.value)"
                  @keyup.enter="handleSearch"
                  type="text"
                  :placeholder="getSearchPlaceholder()"
                  class="search-input"
                  :class="{ invalid: !isValidUrl }"
                />
                <button
                  @click="handleSearch"
                  :disabled="!currentSearchQuery.trim() || !isValidUrl || currentSearching"
                  class="search-btn"
                >
                  <Icon v-if="currentSearching" icon="mdi:loading" class="spin btn-icon" />
                  <Icon v-else-if="searchType === 'playlist'" icon="mdi:playlist-music" class="btn-icon" />
                  <Icon v-else icon="mdi:magnify" class="btn-icon" />
                  {{ getSearchButtonText() }}
                </button>
              </div>

              <!-- URL验证提示 -->
              <div v-if="currentSearchQuery && !isValidUrl" class="url-hint">
                <Icon icon="mdi:alert-circle" class="hint-icon" />
                <span>{{ getUrlHintText() }}</span>
              </div>

              <!-- 歌单解析提示 -->
              <div v-if="searchType === 'playlist'" class="playlist-hint">
                <Icon icon="mdi:information" class="hint-icon" />
                <span>支持QQ音乐和网易云音乐歌单链接</span>
              </div>

              <!-- 搜索错误提示 -->
              <div v-if="searchError" class="search-error">
                <Icon icon="mdi:alert-circle" class="error-icon" />
                <span>{{ searchError }}</span>
              </div>

              <!-- 歌单解析错误提示 -->
              <div v-if="playlistError" class="search-error">
                <Icon icon="mdi:alert-circle" class="error-icon" />
                <span>{{ playlistError }}</span>
              </div>

              <!-- 自动匹配错误提示 -->
              <div v-if="matchingError && playlistInfo" class="search-error">
                <Icon icon="mdi:alert-circle" class="error-icon" />
                <span>{{ matchingError }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 歌单信息区域 -->
      <div v-if="playlistInfo && searchType === 'playlist'" class="playlist-info-section">
        <div class="playlist-card">
          <div class="card-header">
            <Icon icon="mdi:playlist-music" class="card-icon" />
            <div class="card-title-section">
              <h3 class="card-title">{{ playlistInfo.title }}</h3>
              <p class="card-subtitle">创建者: {{ playlistInfo.creator }} · {{ playlistInfo.songCount }} 首歌曲</p>
            </div>
<!--            <div class="header-actions">-->
<!--              <button-->
<!--                @click="addPlaylistToQueue"-->
<!--                class="add-playlist-btn"-->
<!--                :title="'将通过B站搜索下载，自动选择播放量最高的资源'"-->
<!--              >-->
<!--                <Icon icon="mdi:download-multiple" class="btn-icon" />-->
<!--                全部添加到下载队列-->
<!--              </button>-->
<!--            </div>-->
          </div>

          <div class="card-content">
            <div class="playlist-meta">
              <div class="playlist-cover">
                <img :src="playlistInfo.cover" :alt="playlistInfo.title" class="cover-img" />
                <div class="platform-badge" :class="playlistInfo.source">
                  <Icon
                    :icon="playlistInfo.source === 'qq' ? 'simple-icons:qqmusic' : 'simple-icons:netease'"
                    class="platform-icon"
                  />
                </div>
              </div>
              <div class="playlist-details">
                <p v-if="playlistInfo.description" class="playlist-description">{{ playlistInfo.description }}</p>
                <div class="playlist-stats">
                  <span class="stat-item">
                    <Icon icon="mdi:music-note" />
                    {{ playlistInfo.songCount }} 首歌曲
                  </span>
                  <span class="stat-item">
                    <Icon icon="mdi:account" />
                    {{ playlistInfo.creator }}
                  </span>
                  <span class="stat-item">
                    <Icon icon="mdi:web" />
                    {{ playlistInfo.source === 'qq' ? 'QQ音乐' : '网易云音乐' }}
                  </span>
                </div>

                <!-- 下载说明 -->
                <div class="download-notice">
                  <Icon icon="mdi:information-outline" class="notice-icon" />
                  <span>歌单歌曲将通过B站搜索下载，自动选择播放量最高的资源</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 搜索结果区域 -->
      <div v-if="currentHasResults" class="results-section">
        <div class="results-card">
          <div class="card-header">
            <Icon icon="mdi:playlist-music" class="card-icon" />
            <div class="card-title-section">
              <h3 class="card-title">{{ playlistInfo ? '歌单歌曲' : '搜索结果' }}</h3>
              <p class="card-subtitle">{{ playlistInfo ? `共 ${currentSearchResults.length} 首歌曲` : `找到 ${currentSearchResults.length} 个结果` }}</p>
            </div>
            <div class="header-actions">
              <button
                @click="toggleCurrentSelectAll"
                class="select-all-btn"
              >
                <Icon :icon="isCurrentAllSelected ? 'mdi:checkbox-marked' : 'mdi:checkbox-blank-outline'" />
                {{ isCurrentAllSelected ? '取消全选' : '全选' }}
              </button>
              <button
                v-if="hasSelectedItems && !isAutoMatching && playlistInfo"
                @click="autoMatchMusic"
                class="auto-match-btn"
                title="为选中的歌曲自动在B站搜索最佳匹配资源"
              >
                <Icon icon="mdi:auto-fix" class="btn-icon" />
                自动匹配 ({{ selectedResults.size }})
              </button>
              <button
                v-if="isAutoMatching && playlistInfo"
                @click="cancelAutoMatch"
                class="cancel-match-btn"
              >
                <Icon icon="mdi:loading" class="spin btn-icon" />
                匹配中... 点击取消
              </button>
              <button
                v-if="hasSelectedItems"
                @click="addToDownloadQueue"
                class="add-to-queue-btn"
              >
                <Icon icon="mdi:download-multiple" class="btn-icon" />
                添加到下载队列 ({{ selectedResults.size }})
              </button>
            </div>
          </div>

          <div class="card-content">
            <div class="results-grid">
              <div
                v-for="result in currentSearchResults"
                :key="result.id"
                class="result-card"
                :class="{ selected: selectedResults.has(result.id) }"
              >
                <!-- 选择框 -->
                <div class="result-checkbox">
                  <button
                    @click="toggleSelection(result.id)"
                    class="checkbox-btn"
                  >
                    <Icon
                      :icon="selectedResults.has(result.id) ? 'mdi:checkbox-marked' : 'mdi:checkbox-blank-outline'"
                      class="checkbox-icon"
                    />
                  </button>
                </div>

                <!-- 缩略图区域 -->
                <div class="result-thumbnail">
                  <img :src="result.thumbnail" :alt="result.title" class="thumbnail-img" />

                  <!-- 时长显示在图片上 -->
                  <div class="duration-overlay">
                    {{ result.duration }}
                  </div>

                  <!-- 匹配进度显示 -->
                  <div v-if="matchingProgress[result.id] !== undefined && playlistInfo" class="matching-overlay">
                    <div class="matching-progress">
                      <Icon v-if="matchingProgress[result.id] < 100" icon="mdi:loading" class="spin matching-icon" />
                      <Icon v-else icon="mdi:check-circle" class="matching-icon success" />
                      <span class="matching-text">
                        {{ matchingProgress[result.id] < 100 ? '匹配中...' : '已匹配' }}
                      </span>
                    </div>
                  </div>

                  <!-- 平台标识 -->
                  <div class="platform-badge" :class="result.platform">
                    <Icon
                      :icon="result.platform === 'bilibili' ? 'simple-icons:bilibili' : 'mdi:youtube'"
                      class="platform-icon"
                    />
                  </div>
                </div>

                <!-- 信息区域 -->
                <div class="result-info">
                  <!-- 内容区域 -->
                  <div class="result-content">
                    <h4 class="result-title">{{ result.title }}</h4>
                    <p class="result-artist">{{ result.artist }}</p>
                    <div class="result-meta">
                      <span v-if="result.playCount" class="play-count">
                        <Icon icon="mdi:play" />
                        {{ result.playCount }}
                      </span>
                      <span v-if="result.publishTime" class="publish-time">
                        <Icon icon="mdi:calendar" />
                        {{ result.publishTime }}
                      </span>
                    </div>
                  </div>

                  <!-- 操作按钮 - 固定在底部 -->
                  <div class="result-actions">
                    <button
                      @click="playMusic(result)"
                      class="play-btn"
                      :class="{
                        playing: currentPlaying?.id === result.id && isPlaying,
                        loading: currentPlaying?.id === result.id && isLoading
                      }"
                      :title="currentPlaying?.id === result.id && isPlaying ? '暂停' : '播放'"
                    >
                      <Icon
                        v-if="currentPlaying?.id === result.id && isLoading"
                        icon="mdi:loading"
                        class="loading-icon"
                      />
                      <Icon
                        v-else-if="currentPlaying?.id === result.id && isPlaying"
                        icon="mdi:pause"
                      />
                      <Icon
                        v-else
                        icon="mdi:play"
                      />
                      {{ currentPlaying?.id === result.id && isPlaying ? '暂停' : '播放' }}
                    </button>
                    <button
                      @click="showFormatSelection(result)"
                      class="download-btn"
                      :class="{
                        downloading: downloadProgress[result.id] !== undefined && downloadProgress[result.id] < 100,
                        completed: downloadProgress[result.id] === 100
                      }"
                      :disabled="downloadProgress[result.id] !== undefined && downloadProgress[result.id] < 100"
                      :title="getDownloadButtonTitle(result)"
                    >
                      <Icon
                        v-if="downloadProgress[result.id] !== undefined && downloadProgress[result.id] < 100"
                        icon="mdi:loading"
                        class="loading-icon"
                      />
                      <Icon
                        v-else-if="downloadProgress[result.id] === 100"
                        icon="mdi:check-circle"
                      />
                      <Icon
                        v-else
                        icon="mdi:download"
                      />
                      {{ getDownloadButtonText(result) }}
                    </button>
                    <a
                      :href="result.url"
                      target="_blank"
                      class="view-btn"
                      title="查看原视频"
                    >
                      <Icon icon="mdi:open-in-new" />
                      查看
                    </a>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 下载队列区域 -->
      <div v-if="downloadQueue.length > 0" class="download-section">
        <div class="download-card">
          <div class="card-header">
            <Icon icon="mdi:download-multiple" class="card-icon" />
            <div class="card-title-section">
              <h3 class="card-title">下载队列</h3>
              <p class="card-subtitle">{{ downloadQueue.length }} 个待下载项目</p>
            </div>
            <div class="header-actions">
              <button
                v-if="!isDownloading && !isPaused"
                @click="startBatchDownload"
                class="start-all-btn"
              >
                <Icon icon="mdi:play" class="btn-icon" />
                {{ hasPlaylistSongs ? '智能批量下载' : '开始全部下载' }}
              </button>

              <button
                v-if="isDownloading && !isPaused"
                @click="pauseAllDownloads"
                class="pause-all-btn"
              >
                <Icon icon="mdi:pause" class="btn-icon" />
                全部暂停
              </button>

              <button
                v-if="isPaused"
                @click="resumeAllDownloads"
                class="resume-all-btn"
              >
                <Icon icon="mdi:play" class="btn-icon" />
                恢复下载
              </button>

              <button
                @click="clearDownloadQueue"
                class="clear-queue-btn"
                :disabled="isDownloading && !isPaused"
              >
                <Icon icon="mdi:delete-sweep" class="btn-icon" />
                清空队列
              </button>
            </div>
          </div>

          <div class="card-content">
            <div class="download-list">
              <div
                v-for="item in downloadQueue"
                :key="item.id"
                class="download-item"
              >
                <div class="download-thumbnail">
                  <img :src="item.thumbnail" :alt="item.title" class="thumbnail-img" />
                  <div class="platform-badge" :class="item.platform">
                    <Icon
                      :icon="item.platform === 'bilibili' ? 'simple-icons:bilibili' : 'mdi:youtube'"
                      class="platform-icon"
                    />
                  </div>
                </div>

                <div class="download-info">
                  <h4 class="download-title">{{ item.title }}</h4>
                  <p class="download-artist">{{ item.artist }}</p>

                  <!-- 元数据信息 -->
                  <div class="download-meta">
                    <span class="duration">
                      <Icon icon="mdi:clock-outline" />
                      {{ item.duration }}
                    </span>
                    <span v-if="item.playCount" class="play-count">
                      <Icon icon="mdi:play" />
                      {{ item.playCount }}
                    </span>
                    <span v-if="item.publishTime" class="publish-time">
                      <Icon icon="mdi:calendar" />
                      {{ item.publishTime }}
                    </span>
                  </div>

                  <!-- 下载进度 -->
                  <div v-if="downloadProgress[item.id] !== undefined" class="download-progress">
                    <div class="progress-bar">
                      <div
                        class="progress-fill"
                        :style="{ width: downloadProgress[item.id] + '%' }"
                      ></div>
                    </div>
                    <span class="progress-text">{{ Math.round(downloadProgress[item.id]) }}%</span>
                  </div>
                </div>

                <div class="download-actions">
                  <button
                    v-if="downloadProgress[item.id] === undefined"
                    @click="showFormatSelection(item)"
                    class="start-btn"
                    title="开始下载"
                  >
                    <Icon icon="mdi:play" />
                  </button>
                  <div
                    v-else-if="downloadProgress[item.id] < 100"
                    class="downloading-status"
                  >
                    <Icon icon="mdi:loading" class="spin" />
                  </div>
                  <div
                    v-else
                    class="completed-status"
                  >
                    <Icon icon="mdi:check-circle" class="success-icon" />
                  </div>

                  <button
                    @click="removeFromQueue(item.id)"
                    class="remove-btn"
                    title="移除"
                    :disabled="downloadProgress[item.id] !== undefined && downloadProgress[item.id] < 100"
                  >
                    <Icon icon="mdi:close" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!currentHasResults && !currentSearching" class="empty-state">
        <div class="empty-icon">
          <Icon icon="mdi:music-note-outline" />
        </div>
        <h3 class="empty-title">开始搜索音乐</h3>
        <p class="empty-subtitle">输入关键词或粘贴视频链接来搜索和下载音乐</p>
      </div>
    </div>
  </NuxtLayout>

  <!-- 全局音乐播放器 -->
  <div v-if="currentPlaying" class="music-player">
    <div class="player-container">
      <!-- 当前播放信息 -->
      <div class="player-info">
        <img
          :src="currentPlaying.thumbnail"
          :alt="currentPlaying.title"
          class="player-thumbnail"
        />
        <div class="player-text">
          <h4 class="player-title">{{ currentPlaying.title }}</h4>
          <p class="player-artist">{{ currentPlaying.artist }}</p>
        </div>
      </div>

      <!-- 播放控制 -->
      <div class="player-controls">
        <button @click="stopMusic" class="control-btn" title="停止">
          <Icon icon="mdi:stop" />
        </button>
        <button
          @click="isPlaying ? pauseMusic() : resumeMusic()"
          class="control-btn play-control"
          :disabled="isLoading"
          title="播放/暂停"
        >
          <Icon
            v-if="isLoading"
            icon="mdi:loading"
            class="loading-icon"
          />
          <Icon
            v-else-if="isPlaying"
            icon="mdi:pause"
          />
          <Icon
            v-else
            icon="mdi:play"
          />
        </button>
      </div>

      <!-- 进度条 -->
      <div class="player-progress">
        <span class="time-display">{{ formatTime(currentTime) }}</span>
        <div class="progress-container">
          <input
            type="range"
            min="0"
            max="100"
            :value="playProgress"
            @input="setProgress($event.target.value)"
            class="progress-slider"
          />
        </div>
        <span class="time-display">{{ formatTime(totalDuration) }}</span>
      </div>

      <!-- 音量控制 -->
      <div class="player-volume">
        <Icon icon="mdi:volume-high" />
        <input
          type="range"
          min="0"
          max="1"
          step="0.1"
          :value="volume"
          @input="setVolumeValue($event.target.value)"
          class="volume-slider"
        />
      </div>
    </div>
  </div>

  <!-- 格式选择弹窗 -->
  <FormatSelector
    :visible="showFormatSelector"
    :video-url="currentDownloadItem?.url || ''"
    :platform="currentDownloadItem?.platform || ''"
    :title="currentDownloadItem?.title || ''"
    :artist="currentDownloadItem?.artist || ''"
    @close="closeFormatSelector"
    @select="handleFormatSelect"
  />
</template>

<style scoped>
/* 音乐下载页面样式 */
.music-container {
  min-height: 100vh;
  padding: 2rem;
  max-width: 100%;
  contain: layout style paint;
}

/* 页面标题 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 2rem;
  padding-left: 0.5rem;
  padding-right: 0.5rem;
}

.header-left {
  flex: 1;
}

.header-right {
  flex-shrink: 0;
}

.page-title {
  font-size: 1.4rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 0.5rem 0;
  text-align: left;
}

.page-description {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
  text-align: left;
}

/* 清空按钮 */
.clear-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.15) 0%,
    rgba(239, 68, 68, 0.08) 100%
  );
  color: rgba(239, 68, 68, 0.9);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.clear-btn:hover {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.25) 0%,
    rgba(239, 68, 68, 0.15) 100%
  );
  border-color: rgba(239, 68, 68, 0.5);
  color: rgba(239, 68, 68, 1);
  transform: translateY(-1px);
}

/* 卡片基础样式 */
.search-card,
.results-card,
.download-card {
  border-radius: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  overflow: hidden;
  transition: border-color 0.3s ease, transform 0.2s ease;
  margin-bottom: 1.5rem;
}

.search-card:hover,
.results-card:hover,
.download-card:hover {
  border-color: rgba(255, 255, 255, 0.2);
}

/* 卡片头部 */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.card-icon {
  width: 1.5rem;
  height: 1.5rem;
  color: rgba(255, 255, 255, 0.9);
  margin-right: 1rem;
}

.card-title-section {
  flex: 1;
  min-width: 0;
}

.card-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 0.25rem 0;
}

.card-subtitle {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

/* 卡片内容 */
.card-content {
  padding: 1.5rem;
}

/* 搜索类型标签 */
.search-type-tabs {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  gap: 1rem;
}

.tab-buttons {
  display: flex;
  gap: 0.5rem;
}

.tab-actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tab-btn:hover {
  color: rgba(255, 255, 255, 0.9);
  border-color: rgba(255, 255, 255, 0.3);
}

.tab-btn.active {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.15) 0%,
    rgba(59, 130, 246, 0.08) 100%
  );
  border-color: rgba(59, 130, 246, 0.3);
  color: rgba(255, 255, 255, 0.9);
}

.tab-btn svg {
  width: 1rem;
  height: 1rem;
}

/* 平台选择 */
.platform-selection {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.platform-label {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

.platform-buttons {
  display: flex;
  gap: 0.5rem;
}

.platform-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.8rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.platform-btn:hover {
  color: rgba(255, 255, 255, 0.9);
  border-color: rgba(255, 255, 255, 0.3);
}

.platform-btn.active {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.15) 0%,
    rgba(59, 130, 246, 0.08) 100%
  );
  border-color: rgba(59, 130, 246, 0.3);
  color: rgba(255, 255, 255, 0.9);
}

.platform-btn svg {
  width: 0.875rem;
  height: 0.875rem;
}

/* 搜索输入区域 */
.search-input-section {
  margin-bottom: 1.5rem;
}

.search-input-wrapper {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  margin-bottom: 0.75rem;
}

.search-input {
  flex: 1;
  padding: 0.875rem 1rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.875rem;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.search-input:focus {
  outline: none;
  border-color: rgba(255, 255, 255, 0.5);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.12) 0%,
    rgba(255, 255, 255, 0.06) 100%
  );
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.1);
}

.search-input.invalid {
  border-color: rgba(239, 68, 68, 0.5);
}

.search-input.invalid:focus {
  border-color: rgba(239, 68, 68, 0.5);
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
}

.search-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.875rem 1.25rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(59, 130, 246, 0.3);
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.15) 0%,
    rgba(59, 130, 246, 0.08) 100%
  );
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.search-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.25) 0%,
    rgba(59, 130, 246, 0.15) 100%
  );
  border-color: rgba(59, 130, 246, 0.5);
  color: rgba(255, 255, 255, 1);
  transform: translateY(-1px);
}

.search-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.btn-icon {
  width: 1rem;
  height: 1rem;
}

/* URL提示 */
.url-hint {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.15) 0%,
    rgba(239, 68, 68, 0.08) 100%
  );
  border: 1px solid rgba(239, 68, 68, 0.3);
  font-size: 0.8rem;
  color: rgba(239, 68, 68, 0.9);
}

.hint-icon {
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
}

/* 搜索错误提示 */
.search-error {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.15) 0%,
    rgba(239, 68, 68, 0.08) 100%
  );
  border: 1px solid rgba(239, 68, 68, 0.3);
  font-size: 0.8rem;
  color: rgba(239, 68, 68, 0.9);
  margin-top: 0.75rem;
}

.error-icon {
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
}



/* 按钮样式 */
.select-all-btn,
.add-to-queue-btn,
.auto-match-btn,
.cancel-match-btn,
.start-all-btn,
.pause-all-btn,
.resume-all-btn,
.clear-queue-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.select-all-btn:hover,
.start-all-btn:hover,
.resume-all-btn:hover {
  color: rgba(255, 255, 255, 0.9);
  border-color: rgba(255, 255, 255, 0.3);
}

/* 暂停按钮特殊样式 */
.pause-all-btn {
  border-color: rgba(251, 191, 36, 0.3);
  background: linear-gradient(135deg,
    rgba(251, 191, 36, 0.2) 0%,
    rgba(251, 191, 36, 0.1) 100%
  );
  color: rgba(251, 191, 36, 1);
}

.pause-all-btn:hover {
  border-color: rgba(251, 191, 36, 0.5);
  background: linear-gradient(135deg,
    rgba(251, 191, 36, 0.3) 0%,
    rgba(251, 191, 36, 0.15) 100%
  );
}

/* 恢复按钮特殊样式 */
.resume-all-btn {
  border-color: rgba(34, 197, 94, 0.3);
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.2) 0%,
    rgba(34, 197, 94, 0.1) 100%
  );
  color: rgba(34, 197, 94, 1);
}

.resume-all-btn:hover {
  border-color: rgba(34, 197, 94, 0.5);
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.3) 0%,
    rgba(34, 197, 94, 0.15) 100%
  );
}

.add-to-queue-btn {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.15) 0%,
    rgba(59, 130, 246, 0.08) 100%
  );
  border-color: rgba(59, 130, 246, 0.3);
  color: rgba(255, 255, 255, 0.9);
}

.add-to-queue-btn:hover {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.25) 0%,
    rgba(59, 130, 246, 0.15) 100%
  );
  border-color: rgba(59, 130, 246, 0.5);
  color: rgba(255, 255, 255, 1);
}

.clear-queue-btn {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.15) 0%,
    rgba(239, 68, 68, 0.08) 100%
  );
  border-color: rgba(239, 68, 68, 0.3);
  color: rgba(239, 68, 68, 0.9);
}

.clear-queue-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.25) 0%,
    rgba(239, 68, 68, 0.15) 100%
  );
  border-color: rgba(239, 68, 68, 0.5);
  color: rgba(239, 68, 68, 1);
}

.clear-queue-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 自动匹配按钮样式 */
.auto-match-btn {
  background: linear-gradient(135deg,
    rgba(168, 85, 247, 0.15) 0%,
    rgba(168, 85, 247, 0.08) 100%
  );
  border-color: rgba(168, 85, 247, 0.3);
  color: rgba(168, 85, 247, 0.9);
}

.auto-match-btn:hover {
  background: linear-gradient(135deg,
    rgba(168, 85, 247, 0.25) 0%,
    rgba(168, 85, 247, 0.15) 100%
  );
  border-color: rgba(168, 85, 247, 0.5);
  color: rgba(168, 85, 247, 1);
}

/* 取消匹配按钮样式 */
.cancel-match-btn {
  background: linear-gradient(135deg,
    rgba(251, 191, 36, 0.15) 0%,
    rgba(251, 191, 36, 0.08) 100%
  );
  border-color: rgba(251, 191, 36, 0.3);
  color: rgba(251, 191, 36, 0.9);
}

.cancel-match-btn:hover {
  background: linear-gradient(135deg,
    rgba(251, 191, 36, 0.25) 0%,
    rgba(251, 191, 36, 0.15) 100%
  );
  border-color: rgba(251, 191, 36, 0.5);
  color: rgba(251, 191, 36, 1);
}

/* 结果网格 */
.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.5rem;
  padding: 0.5rem;
}

/* 下载列表保持原样 */
.download-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/* 结果卡片 - 垂直布局 */
.result-card {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  border-radius: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
}

.result-card:hover {
  border-color: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.result-card.selected {
  border-color: rgba(59, 130, 246, 0.6);
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.15) 0%,
    rgba(59, 130, 246, 0.08) 100%
  );
}

/* 下载项保持原有水平布局 */
.download-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: border-color 0.3s ease, transform 0.2s ease;
}

.download-item:hover {
  border-color: rgba(255, 255, 255, 0.2);
}

/* 复选框 */
.result-checkbox {
  position: absolute;
  top: 0.5rem;
  left: 0.5rem;
  z-index: 10;
}

.checkbox-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 0.5rem;
  transition: all 0.3s ease;
}

.checkbox-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.checkbox-icon {
  width: 1.25rem;
  height: 1.25rem;
  color: rgba(255, 255, 255, 0.6);
  transition: all 0.3s ease;
}

.checkbox-btn:hover .checkbox-icon {
  color: rgba(255, 255, 255, 0.9);
}

/* 缩略图 */
.result-thumbnail {
  position: relative;
  width: 100%;
  aspect-ratio: 16/9;
  overflow: hidden;
  border-radius: 0.5rem 0.5rem 0 0;
}

.download-thumbnail {
  position: relative;
  flex-shrink: 0;
}

/* 结果卡片中的缩略图 */
.result-card .thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.result-card:hover .thumbnail-img {
  transform: scale(1.05);
}

/* 下载列表中的缩略图 */
.download-thumbnail .thumbnail-img {
  width: 80px;
  height: 60px;
  border-radius: 0.5rem;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* 时长覆盖层 */
.duration-overlay {
  position: absolute;
  bottom: 0.5rem;
  right: 0.5rem;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
  font-size: 0.75rem;
  font-weight: 500;
  backdrop-filter: blur(4px);
}

/* 匹配进度覆盖层 */
.matching-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem 0.5rem 0 0;
  backdrop-filter: blur(4px);
}

.matching-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  color: white;
  text-align: center;
}

.matching-icon {
  width: 2rem;
  height: 2rem;
  color: rgba(168, 85, 247, 0.9);
}

.matching-icon.success {
  color: rgba(34, 197, 94, 0.9);
}

.matching-text {
  font-size: 0.75rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

.platform-badge {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(4px);
}

.platform-badge.bilibili {
  background: #00a1d6;
}

.platform-badge.youtube {
  background: #ff0000;
}

.platform-icon {
  width: 0.75rem;
  height: 0.75rem;
  color: white;
}

/* 信息区域 */
.result-info {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  flex: 1;
  justify-content: space-between;
}

/* 内容区域 */
.result-content {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.download-info {
  flex: 1;
  min-width: 0;
}

/* 结果卡片标题 */
.result-card .result-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 下载列表标题 */
.download-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 0.25rem 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 艺术家信息 */
.result-artist,
.download-artist {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-meta {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 0.25rem;
}

.duration,
.quality,
.play-count,
.publish-time {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.7rem;
  color: rgba(255, 255, 255, 0.5);
}

.duration svg,
.quality svg,
.play-count svg,
.publish-time svg {
  width: 0.75rem;
  height: 0.75rem;
}

/* 播放量特殊样式 */
.play-count {
  color: rgba(255, 255, 255, 0.6);
}

/* 发布时间特殊样式 */
.publish-time {
  color: rgba(255, 255, 255, 0.4);
}

/* 下载队列元数据 */
.download-meta {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-wrap: wrap;
  margin: 0.25rem 0;
}

/* 加载动画 */
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

/* 操作按钮 */
.result-actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  margin-top: 1rem;
  flex-shrink: 0;
}

.download-actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-shrink: 0;
}

/* 结果卡片中的按钮 */
.result-card .play-btn,
.result-card .download-btn,
.result-card .view-btn {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.5rem 0.75rem;
  border-radius: 0.375rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.75rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
}

/* 下载列表中的按钮 */
.download-actions .start-btn,
.download-actions .remove-btn {
  width: 2rem;
  height: 2rem;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.08) 0%,
    rgba(255, 255, 255, 0.04) 100%
  );
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
}

/* 下载按钮悬停效果 */
.download-btn:hover:not(:disabled),
.start-btn:hover {
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.15) 0%,
    rgba(34, 197, 94, 0.08) 100%
  );
  border-color: rgba(34, 197, 94, 0.3);
  color: rgba(34, 197, 94, 0.9);
}

/* 下载按钮状态样式 */
.download-btn.downloading {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.15) 0%,
    rgba(59, 130, 246, 0.08) 100%
  );
  border-color: rgba(59, 130, 246, 0.3);
  color: rgba(59, 130, 246, 0.9);
  cursor: not-allowed;
  opacity: 0.8;
}

.download-btn.completed {
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.2) 0%,
    rgba(34, 197, 94, 0.1) 100%
  );
  border-color: rgba(34, 197, 94, 0.4);
  color: rgba(34, 197, 94, 1);
}

.download-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* 播放按钮样式 */
.play-btn:hover {
  background: linear-gradient(135deg,
    rgba(168, 85, 247, 0.15) 0%,
    rgba(168, 85, 247, 0.08) 100%
  );
  border-color: rgba(168, 85, 247, 0.3);
  color: rgba(168, 85, 247, 0.9);
}

.play-btn.playing {
  background: linear-gradient(135deg,
    rgba(168, 85, 247, 0.2) 0%,
    rgba(168, 85, 247, 0.1) 100%
  );
  border-color: rgba(168, 85, 247, 0.4);
  color: rgba(168, 85, 247, 1);
}

.play-btn.loading {
  opacity: 0.7;
  cursor: not-allowed;
}

/* 查看按钮悬停效果 */
.view-btn:hover {
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.15) 0%,
    rgba(59, 130, 246, 0.08) 100%
  );
  border-color: rgba(59, 130, 246, 0.3);
  color: rgba(255, 255, 255, 0.9);
}

.remove-btn:hover:not(:disabled) {
  background: linear-gradient(135deg,
    rgba(239, 68, 68, 0.15) 0%,
    rgba(239, 68, 68, 0.08) 100%
  );
  border-color: rgba(239, 68, 68, 0.3);
  color: rgba(239, 68, 68, 0.9);
}

.remove-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.download-btn svg,
.view-btn svg,
.start-btn svg,
.remove-btn svg {
  width: 1rem;
  height: 1rem;
}

/* 下载进度 */
.download-progress {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.progress-bar {
  flex: 1;
  height: 0.5rem;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 0.25rem;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg,
    rgba(34, 197, 94, 0.8) 0%,
    rgba(34, 197, 94, 1) 100%
  );
  border-radius: 0.25rem;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
  min-width: 3rem;
  text-align: right;
}

/* 下载状态 */
.downloading-status,
.completed-status {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
}

.downloading-status svg {
  width: 1.25rem;
  height: 1.25rem;
  color: rgba(255, 255, 255, 0.9);
}

.success-icon {
  width: 1.25rem;
  height: 1.25rem;
  color: rgba(34, 197, 94, 0.9);
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  text-align: center;
  border-radius: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  margin-top: 2rem;
}

.empty-icon {
  width: 4rem;
  height: 4rem;
  margin-bottom: 1.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.empty-icon svg {
  width: 2rem;
  height: 2rem;
  color: rgba(255, 255, 255, 0.6);
}

.empty-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
  margin: 0 0 0.5rem 0;
}

.empty-subtitle {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

/* 动画 */
.spin {
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
@media (max-width: 768px) {
  .music-container {
    padding: 1rem;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
  }

  .platform-selection {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }

  .search-input-wrapper {
    flex-direction: column;
    gap: 0.75rem;
  }

  .search-btn {
    width: 100%;
    justify-content: center;
  }

  .result-item,
  .download-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }

  .result-actions,
  .download-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .thumbnail-img {
    width: 100%;
    height: auto;
    max-width: 200px;
  }
}

@media (max-width: 480px) {
  .music-container {
    padding: 0.75rem;
  }

  .search-type-tabs {
    flex-direction: column;
  }

  .platform-buttons {
    flex-direction: column;
    width: 100%;
  }

  .platform-btn {
    justify-content: center;
  }
}

/* 全局音乐播放器 */
.music-player {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(135deg,
    rgba(0, 0, 0, 0.9) 0%,
    rgba(0, 0, 0, 0.8) 100%
  );
  backdrop-filter: blur(20px);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding: 1rem;
  z-index: 1000;
}

.player-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 1rem;
}

/* 播放信息 */
.player-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 200px;
}

.player-thumbnail {
  width: 50px;
  height: 50px;
  border-radius: 0.5rem;
  object-fit: cover;
}

.player-text {
  flex: 1;
  min-width: 0;
}

.player-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 0.25rem 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.player-artist {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 播放控制 */
.player-controls {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.control-btn {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.control-btn:hover {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 100%
  );
  border-color: rgba(255, 255, 255, 0.3);
  color: rgba(255, 255, 255, 1);
}

.control-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.play-control {
  width: 3rem;
  height: 3rem;
  background: linear-gradient(135deg,
    rgba(168, 85, 247, 0.2) 0%,
    rgba(168, 85, 247, 0.1) 100%
  );
  border-color: rgba(168, 85, 247, 0.3);
  color: rgba(168, 85, 247, 1);
}

.play-control:hover {
  background: linear-gradient(135deg,
    rgba(168, 85, 247, 0.3) 0%,
    rgba(168, 85, 247, 0.15) 100%
  );
}

/* 进度条 */
.player-progress {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin: 0 1rem;
}

.time-display {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.6);
  min-width: 35px;
  text-align: center;
}

.progress-container {
  flex: 1;
}

.progress-slider {
  width: 100%;
  height: 4px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.2);
  outline: none;
  cursor: pointer;
  -webkit-appearance: none;
}

.progress-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: rgba(168, 85, 247, 1);
  cursor: pointer;
  transition: all 0.3s ease;
}

.progress-slider::-webkit-slider-thumb:hover {
  transform: scale(1.2);
}

.progress-slider::-moz-range-thumb {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: rgba(168, 85, 247, 1);
  cursor: pointer;
  border: none;
}

/* 音量控制 */
.player-volume {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 100px;
}

.volume-slider {
  width: 60px;
  height: 3px;
  border-radius: 1.5px;
  background: rgba(255, 255, 255, 0.2);
  outline: none;
  cursor: pointer;
  -webkit-appearance: none;
}

.volume-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.8);
  cursor: pointer;
}

.volume-slider::-moz-range-thumb {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  border: none;
}

/* 歌单信息样式 */
.playlist-info-section {
  margin-bottom: 1.5rem;
}

.playlist-card {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.05) 100%
  );
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 1rem;
  backdrop-filter: blur(10px);
  overflow: hidden;
}

.playlist-meta {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
}

.playlist-cover {
  position: relative;
  flex-shrink: 0;
}

.cover-img {
  width: 120px;
  height: 120px;
  border-radius: 0.75rem;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.playlist-details {
  flex: 1;
  min-width: 0;
}

.playlist-description {
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.875rem;
  line-height: 1.5;
  margin-bottom: 1rem;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.playlist-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: rgba(255, 255, 255, 0.6);
  font-size: 0.875rem;
}

.stat-item svg {
  width: 1rem;
  height: 1rem;
}

.add-playlist-btn {
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  border: 1px solid rgba(168, 85, 247, 0.3);
  background: linear-gradient(135deg,
    rgba(168, 85, 247, 0.2) 0%,
    rgba(168, 85, 247, 0.1) 100%
  );
  color: rgba(168, 85, 247, 1);
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
}

.add-playlist-btn:hover {
  background: linear-gradient(135deg,
    rgba(168, 85, 247, 0.3) 0%,
    rgba(168, 85, 247, 0.15) 100%
  );
  border-color: rgba(168, 85, 247, 0.5);
}

/* 歌单解析提示样式 */
.playlist-hint {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
  padding: 0.5rem;
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.1) 0%,
    rgba(59, 130, 246, 0.05) 100%
  );
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 0.5rem;
  color: rgba(59, 130, 246, 0.9);
  font-size: 0.875rem;
}

.playlist-hint .hint-icon {
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
}

/* 下载说明样式 */
.download-notice {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 1rem;
  padding: 0.75rem;
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.1) 0%,
    rgba(34, 197, 94, 0.05) 100%
  );
  border: 1px solid rgba(34, 197, 94, 0.2);
  border-radius: 0.5rem;
  color: rgba(34, 197, 94, 0.9);
  font-size: 0.875rem;
}

.download-notice .notice-icon {
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
}

/* 播放器响应式设计 */
@media (max-width: 768px) {
  .player-container {
    flex-direction: column;
    gap: 0.75rem;
  }

  .player-info {
    min-width: auto;
    width: 100%;
  }

  .player-progress {
    margin: 0;
    width: 100%;
  }

  .player-volume {
    min-width: auto;
  }

  /* 歌单响应式 */
  .playlist-meta {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .cover-img {
    width: 100px;
    height: 100px;
  }

  .playlist-stats {
    justify-content: center;
  }
}
</style>