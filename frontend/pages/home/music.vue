<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Icon } from '@iconify/vue'
import { useMusicApi, type MusicSearchResult } from '~/composables/useMusicApi'
import { useMusicState } from '~/composables/useMusicState'
import FormatSelector from '~/components/FormatSelector.vue'
import LyricsDisplay from '~/components/LyricsDisplay.vue'
import './music.css'

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

  // 歌词状态
  currentLyrics,
  lyricsType,
  lyricsSource,
  isLyricsLoading,
  showLyrics,

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
  clearAllState,

  // 歌词方法
  setCurrentLyrics,
  setLyricsType,
  setLyricsSource,
  setLyricsLoading,
  setShowLyrics,
  clearLyrics,

  // 歌单信息
  playlistInfo,
  setPlaylistInfo,
  clearPlaylistInfo
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
const { searchMusic, downloadMusic, getAudioStreamByUrl, getPlayableAudioUrl, getFallbackAudioUrl, parsePlaylist, getSupportedPlatforms, getAvailableFormats, detectPlatform, processImageUrl, getLyrics } = useMusicApi()

// 下载相关状态
const isDownloading = ref(false)
const isPaused = ref(false)
const downloadControllers = ref(new Map()) // 存储下载控制器

// 歌单解析相关状态
const isParsingPlaylist = ref(false)
const playlistError = ref('')

// 歌单过滤状态
const playlistFilter = ref('')

// 自动匹配相关状态
const isAutoMatching = ref(false)
const matchingProgress = ref<Record<string, number>>({})
const matchingError = ref('')

// 格式选择相关状态
const showFormatSelector = ref(false)
const currentDownloadItem = ref<MusicSearchResult | null>(null)

// 下载抽屉相关状态
const showDownloadDrawer = ref(false)

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

// 计算属性：过滤后的歌单结果
const filteredPlaylistResults = computed(() => {
  const results = currentSearchResults.value
  if (!playlistFilter.value.trim() || searchType.value !== 'playlist') {
    return results
  }

  const keywords = playlistFilter.value.trim().toLowerCase().split(/\s+/)

  return results.filter(song => {
    const searchText = [
      song.title,
      song.artist,
      song.album || '',
      song.description || ''
    ].join(' ').toLowerCase()

    return keywords.every(keyword => searchText.includes(keyword))
  })
})

// 计算属性：是否有搜索结果
const currentHasResults = computed(() => {
  return currentSearchResults.value.length > 0
})

// 计算属性：当前搜索结果是否全选
const isCurrentAllSelected = computed(() => {
  const currentResults = searchType.value === 'playlist' ? filteredPlaylistResults.value : currentSearchResults.value
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
  setPlaylistInfo(null)

  try {
    const playlist = await parsePlaylist({
      url: currentSearchQuery.value.trim(),
      platform: 'auto'
    })

    setPlaylistInfo(playlist)

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
      tags: [],
      playlistName: playlist.title,
      vip: song.vip || false
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

  // 创建歌单歌曲列表的副本，避免在遍历过程中队列被修改导致遍历异常
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
  // 优先使用 playlistName 字段判断
  if (item.playlistName) {
    return true
  }
  // 兼容旧的判断方式
  return item.description && item.description.includes('来自歌单:')
}

// 检查歌曲是否已经匹配过或者本身就是可下载的
const isMatchedOrDownloadable = (item: MusicSearchResult) => {
  // 1. 检查是否已经通过自动匹配
  if (item.description && item.description.includes('(已匹配B站:')) {
    return true
  }

  // 2. 检查是否本身就是来自 bilibili/youtube 的链接（URL搜索结果）
  if (searchType.value === 'url' && (item.platform === 'bilibili' || item.platform === 'youtube')) {
    return true
  }

  // 3. 检查是否是关键词搜索的结果（这些本身就是可下载的）
  if (searchType.value === 'keyword') {
    return true
  }

  if(item.platform == 'netease' && item.vip==false) {
    return true
  }

  return false
}

// 获取当前播放列表（支持歌单和搜索结果）
const getCurrentPlaylist = (): MusicSearchResult[] => {
  // 如果当前播放的是歌单歌曲，返回歌单歌曲列表
  if (currentPlaying.value && isPlaylistSong(currentPlaying.value)) {
    return currentSearchResults.value.filter(song => isPlaylistSong(song))
  }

  // 否则返回当前搜索结果作为播放列表
  return currentSearchResults.value
}

// 获取歌曲在当前播放列表中的索引
const getCurrentSongIndex = (song: MusicSearchResult): number => {
  const playlist = getCurrentPlaylist()
  return playlist.findIndex(item => item.id === song.id)
}

// 获取下一首歌
const getNextSong = (currentSong?: MusicSearchResult): MusicSearchResult | null => {
  const song = currentSong || currentPlaying.value
  if (!song) return null

  const playlist = getCurrentPlaylist()
  if (playlist.length === 0) return null

  const currentIndex = getCurrentSongIndex(song)

  if (currentIndex === -1) {
    // 如果没找到当前歌曲，返回第一首
    return playlist[0]
  }

  // 如果是最后一首，返回null（不循环播放）
  if (currentIndex >= playlist.length - 1) {
    return null
  }

  // 返回下一首歌
  return playlist[currentIndex + 1]
}

// 获取上一首歌
const getPreviousSong = (currentSong?: MusicSearchResult): MusicSearchResult | null => {
  const song = currentSong || currentPlaying.value
  if (!song) return null

  const playlist = getCurrentPlaylist()
  if (playlist.length === 0) return null

  const currentIndex = getCurrentSongIndex(song)

  if (currentIndex === -1) {
    // 如果没找到当前歌曲，返回最后一首
    return playlist[playlist.length - 1]
  }

  // 如果是第一首，返回null（不循环播放）
  if (currentIndex <= 0) {
    return null
  }

  // 返回上一首歌
  return playlist[currentIndex - 1]
}

// 获取歌单中的下一首歌（保留兼容性）
const getNextPlaylistSong = (currentSong: MusicSearchResult): MusicSearchResult | null => {
  return getNextSong(currentSong)
}

// 播放下一首歌
const playNextSong = async () => {
  if (!currentPlaying.value) {
    showNotification('当前没有正在播放的歌曲', 'error')
    return
  }

  const nextSong = getNextSong(currentPlaying.value)

  if (nextSong) {
    console.log('播放下一首:', nextSong.title, '-', nextSong.artist)
    showNotification(`播放下一首: ${nextSong.title}`, 'info')
    await playMusic(nextSong)
  } else {
    console.log('已经是最后一首歌曲')
    showNotification('已经是最后一首歌曲', 'info')
  }
}

// 播放上一首歌
const playPreviousSong = async () => {
  if (!currentPlaying.value) {
    showNotification('当前没有正在播放的歌曲', 'error')
    return
  }

  const previousSong = getPreviousSong(currentPlaying.value)

  if (previousSong) {
    console.log('播放上一首:', previousSong.title, '-', previousSong.artist)
    showNotification(`播放上一首: ${previousSong.title}`, 'info')
    await playMusic(previousSong)
  } else {
    console.log('已经是第一首歌曲')
    showNotification('已经是第一首歌曲', 'info')
  }
}

// 自动播放歌单中的下一首歌（保留兼容性）
const playNextPlaylistSong = async () => {
  await playNextSong()
}

// 获取歌曲在播放列表中的位置信息
const getPlaylistPosition = (song: MusicSearchResult): string => {
  if (!song) return ''

  const playlist = getCurrentPlaylist()
  const currentIndex = getCurrentSongIndex(song)

  if (currentIndex === -1 || playlist.length === 0) {
    return ''
  }

  return `${currentIndex + 1}/${playlist.length}`
}

// 计算属性：是否有上一首歌曲
const hasPreviousSong = computed(() => {
  if (!currentPlaying.value) return false
  return getPreviousSong(currentPlaying.value) !== null
})

// 计算属性：是否有下一首歌曲
const hasNextSong = computed(() => {
  if (!currentPlaying.value) return false
  return getNextSong(currentPlaying.value) !== null
})

// 从已匹配的歌曲中提取B站资源信息
const extractMatchedResult = (item: MusicSearchResult): MusicSearchResult | null => {
  if (!item.description || !item.description.includes('(已匹配B站:')) {
    return null
  }

  try {
    // 已匹配的歌曲实际上已经包含了完整的B站资源信息
    // 自动匹配功能会更新歌曲的 url、thumbnail、playCount 等字段
    // 我们只需要创建一个新的结果对象，确保平台设置为 bilibili
    const matchedResult: MusicSearchResult = {
      ...item,
      id: `matched_${item.id}`,
      platform: 'bilibili', // 确保平台设置为 bilibili
      quality: '音频'
    }

    console.log('使用已匹配的B站资源信息:', {
      title: matchedResult.title,
      url: matchedResult.url,
      thumbnail: matchedResult.thumbnail,
      playCount: matchedResult.playCount
    })

    return matchedResult

  } catch (error) {
    console.error('提取匹配结果时发生错误:', error)
    return null
  }
}

// 通过B站搜索下载歌单歌曲
const downloadPlaylistSong = async (item: MusicSearchResult, onProgress?: (progress: number) => void, abortSignal?: AbortSignal) => {
  try {
    // 检查是否已经匹配过
    if (item.description && item.description.includes('(已匹配B站:')) {
      console.log('歌曲已匹配，直接使用匹配结果下载:', item.title)

      // 从描述中提取匹配的B站资源信息
      const matchedResult = extractMatchedResult(item)
      if (matchedResult) {
        console.log('使用已匹配的B站资源:', matchedResult.title)

        // 直接使用匹配结果进行下载
        const result = await downloadMusic(matchedResult, onProgress)

        if (result) {
          showNotification(`下载完成: ${item.title} (使用已匹配资源: ${matchedResult.title})`, 'success')
          return true
        } else {
          throw new Error('下载失败')
        }
      }
    }else if(item.platform == 'netease'){
      // 如果是网易云音乐尝试直接下载
      console.log('尝试直接下载网易云音乐歌曲:', item.title)
      const result = await downloadMusic(item, onProgress)
      if (result) {
        showNotification(`下载完成: ${item.title}`, 'success')
        return true
      } else {
        throw new Error('下载失败')
      }
    }

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
        // 为搜索结果添加歌单名称
        const resultsWithPlaylistName = searchResults.map(result => ({
          ...result,
          playlistName: item.playlistName
        }))
        allResults.push(...resultsWithPlaylistName)

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
    const result = await downloadMusic(bestResult, onProgress)

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

    // 如果当前正在加载其他歌曲，则忽略新的播放请求
    if (isLoading.value && currentPlaying.value?.id !== result.id) {
      console.log('当前正在加载其他歌曲，忽略播放请求:', result.title)
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

    audio.addEventListener('ended', async () => {
      setPlaying(false)
      setCurrentTime(0)

      // 如果当前播放的是歌单中的歌曲，自动播放下一首
      if (currentPlaying.value && isPlaylistSong(currentPlaying.value)) {
        console.log('歌曲播放结束，检查是否有下一首歌单歌曲')

        // 延迟1秒后播放下一首，给用户一个缓冲时间
        setTimeout(async () => {
          // 再次检查是否还在播放状态（用户可能在这1秒内手动操作了）
          if (!isPlaying.value && currentPlaying.value) {
            await playNextPlaylistSong()
          }
        }, 1000)
      }
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

    // 获取歌词（异步，不阻塞播放）
    loadLyrics(result)

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

// 键盘快捷键处理
const handleKeydown = (event: KeyboardEvent) => {
  // 只在没有输入框聚焦时响应快捷键
  if (event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement) {
    return
  }

  switch (event.code) {
    case 'Space':
      event.preventDefault()
      if (currentPlaying.value) {
        if (isPlaying.value) {
          pauseMusic()
        } else {
          resumeMusic()
        }
      }
      break
    case 'ArrowLeft':
      event.preventDefault()
      if (hasPreviousSong.value && !isLoading.value) {
        playPreviousSong()
      }
      break
    case 'ArrowRight':
      event.preventDefault()
      if (hasNextSong.value && !isLoading.value) {
        playNextSong()
      }
      break
    case 'Escape':
      event.preventDefault()
      stopMusic()
      break
  }
}

// 组件挂载时添加键盘事件监听
onMounted(() => {
  if (process.client) {
    document.addEventListener('keydown', handleKeydown)
  }
})

// 组件卸载时移除键盘事件监听
onUnmounted(() => {
  if (process.client) {
    document.removeEventListener('keydown', handleKeydown)
  }
})

// 获取歌词
const loadLyrics = async (music: MusicSearchResult) => {
  try {
    setLyricsLoading(true)
    clearLyrics()

    console.log('开始获取歌词:', music.title, music.artist)

    const lyricsData = await getLyrics(music.url, music.title, music.artist)

    console.log('歌词API返回数据:', lyricsData)

    if (lyricsData) {
      console.log('歌词内容:', lyricsData.lyrics)
      console.log('歌词类型:', lyricsData.type)
      console.log('歌词来源:', lyricsData.source)

      setCurrentLyrics(lyricsData.lyrics || '')
      setLyricsType(lyricsData.type || '')
      setLyricsSource(lyricsData.source || '')
      console.log('歌词设置成功')
    } else {
      console.log('未获取到歌词')
      setCurrentLyrics('暂无歌词')
      setLyricsType('placeholder')
      setLyricsSource('')
    }

  } catch (error) {
    console.error('获取歌词失败:', error)
    setCurrentLyrics('歌词获取失败')
    setLyricsType('error')
    setLyricsSource('')
  } finally {
    setLyricsLoading(false)
  }
}

// 切换歌词显示
const toggleLyrics = () => {
  setShowLyrics(!showLyrics.value)
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

  // 立即将所有选中的歌曲设置为loading状态
  selectedSongs.forEach(song => {
    matchingProgress.value[song.id] = 0
  })

  // 跟踪匹配成功和失败的歌曲
  const matchedSuccessfully = new Set<string>()
  const matchedFailed = new Set<string>()

  try {
    showNotification(`开始自动匹配 ${selectedSongs.length} 首歌曲，将在B站搜索最佳资源`, 'success')

    // 逐个匹配歌曲
    for (let i = 0; i < selectedSongs.length; i++) {
      const song = selectedSongs[i]

      if (!isAutoMatching.value) break // 检查是否被取消

      try {
        // 歌曲已经在loading状态，开始实际匹配
        console.log(`开始匹配第 ${i + 1}/${selectedSongs.length} 首: ${song.title} - ${song.artist}`)

        // 构建多个搜索关键词，提高匹配成功率
        const title = song.title.replace(/[()（）]/g, '')
        const searchKeywords = [
          `${song.artist} ${title}`,  // 歌手 + 歌曲名
          `${title} ${song.artist}`,  // 歌曲名 + 歌手
          title,                      // 仅歌曲名
          `${title} 音乐`,            // 歌曲名 + 音乐
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
            // 为搜索结果添加歌单名称
            const resultsWithPlaylistName = searchResults.map(result => ({
              ...result,
              playlistName: song.playlistName
            }))
            allResults.push(...resultsWithPlaylistName)

            if (searchResults.length > 0) {
              console.log(`关键词 "${keyword}" 找到 ${searchResults.length} 个结果`)
              break // 找到结果就停止搜索
            }
          } catch (error) {
            console.log(`关键词 "${keyword}" 搜索失败:`, error)
            continue
          }
        }

        // 搜索完成，更新进度
        updateMatchingProgress(song.id, 50)

        if (allResults.length === 0) {
          console.warn(`未找到匹配结果: ${song.title} - ${song.artist}`)
          matchedFailed.add(song.id)
          updateMatchingProgress(song.id, 100)
          continue
        }

        // 开始处理结果，更新进度
        updateMatchingProgress(song.id, 70)

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

        // 标记为匹配成功
        matchedSuccessfully.add(song.id)

        // 开始更新结果，更新进度
        updateMatchingProgress(song.id, 90)

        // 更新当前搜索结果中的信息
        const originalIndex = currentSearchResults.value.findIndex(r => r.id === song.id)
        if (originalIndex !== -1) {
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
        }

        // 匹配成功，设置为100%并在1秒后清除loading状态
        updateMatchingProgress(song.id, 100)
        setTimeout(() => {
          if (matchingProgress.value[song.id] === 100) {
            delete matchingProgress.value[song.id]
          }
        }, 1000)

        // 每首歌之间间隔1秒，避免请求过于频繁
        if (i < selectedSongs.length - 1) {
          await new Promise(resolve => setTimeout(resolve, 1000))
        }

      } catch (error: any) {
        console.error(`匹配失败: ${song.title} - ${song.artist}`, error)
        // 标记为匹配失败
        matchedFailed.add(song.id)
        // 匹配失败，设置为100%并在1秒后清除loading状态
        updateMatchingProgress(song.id, 100)
        setTimeout(() => {
          if (matchingProgress.value[song.id] === 100) {
            delete matchingProgress.value[song.id]
          }
        }, 1000)
      }
    }

    const successCount = matchedSuccessfully.size
    const failedCount = matchedFailed.size

    // 自动取消选中匹配成功的歌曲
    matchedSuccessfully.forEach(songId => {
      if (selectedResults.value.has(songId)) {
        toggleSelection(songId)
      }
    })

    if (successCount > 0) {
      showNotification(`自动匹配完成，成功匹配 ${successCount}/${selectedSongs.length} 首歌曲，已自动取消选中匹配成功的歌曲`, 'success')
    } else {
      showNotification(`自动匹配完成，未找到匹配的歌曲`, 'warning')
    }

  } catch (error: any) {
    console.error('自动匹配过程中发生错误:', error)
    matchingError.value = error.message || '自动匹配失败'
    showNotification('自动匹配失败: ' + matchingError.value, 'error')
  } finally {
    isAutoMatching.value = false
    // 2秒后清除所有剩余的匹配进度
    setTimeout(() => {
      matchingProgress.value = {}
    }, 2000)
  }
}

// 更新匹配进度的辅助函数
const updateMatchingProgress = (songId: string, progress: number) => {
  if (matchingProgress.value[songId] !== undefined) {
    matchingProgress.value[songId] = progress
  }
}

// 取消自动匹配
const cancelAutoMatch = () => {
  isAutoMatching.value = false
  // 清除所有匹配进度，让loading状态消失
  matchingProgress.value = {}
  showNotification('已取消自动匹配', 'info')
}

// 计算按钮数量，用于布局优化
const getButtonCount = (result: MusicSearchResult) => {
  let count = 2 // 播放和查看按钮总是存在

  // 下载按钮（总是显示）
  count += 1

  // 匹配按钮（只在歌单模式下且歌曲未匹配时显示）
  if (playlistInfo.value && !isMatchedOrDownloadable(result) && matchingProgress.value[result.id] === undefined) {
    count += 1
  }

  // 匹配中按钮（只在匹配进行中时显示）
  if (playlistInfo.value && matchingProgress.value[result.id] !== undefined && matchingProgress.value[result.id] < 100) {
    count += 1
  }

  return count
}

// 单独匹配单首歌曲
const matchSingleSong = async (song: MusicSearchResult) => {
  // 检查是否已经匹配过
  if (isMatchedOrDownloadable(song)) {
    showNotification('该歌曲已经匹配过或可直接下载', 'info')
    return
  }

  // 检查是否正在进行批量匹配
  if (isAutoMatching.value) {
    showNotification('正在进行批量匹配，请等待完成后再进行单独匹配', 'warning')
    return
  }

  // 设置匹配进度
  matchingProgress.value[song.id] = 0

  try {
    console.log(`开始单独匹配: ${song.title} - ${song.artist}`)
    showNotification(`开始匹配: ${song.title}`, 'info')

    // 构建多个搜索关键词，提高匹配成功率
    const title = song.title.replace(/[()（）]/g, '')
    const searchKeywords = [
      `${song.artist} ${title}`,  // 歌手 + 歌曲名
      `${title} ${song.artist}`,  // 歌曲名 + 歌手
      title,                      // 仅歌曲名
      `${title} 音乐`,            // 歌曲名 + 音乐
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
        // 为搜索结果添加歌单名称
        const resultsWithPlaylistName = searchResults.map(result => ({
          ...result,
          playlistName: song.playlistName
        }))
        allResults.push(...resultsWithPlaylistName)

        if (searchResults.length > 0) {
          console.log(`关键词 "${keyword}" 找到 ${searchResults.length} 个结果`)
          break // 找到结果就停止搜索
        }
      } catch (error) {
        console.log(`关键词 "${keyword}" 搜索失败:`, error)
        continue
      }
    }

    // 搜索完成，更新进度
    updateMatchingProgress(song.id, 50)

    if (allResults.length === 0) {
      console.warn(`未找到匹配结果: ${song.title} - ${song.artist}`)
      showNotification(`未找到匹配结果: ${song.title}`, 'warning')
      updateMatchingProgress(song.id, 100)
      setTimeout(() => {
        delete matchingProgress.value[song.id]
      }, 1000)
      return
    }

    // 开始处理结果，更新进度
    updateMatchingProgress(song.id, 70)

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

    // 开始更新结果，更新进度
    updateMatchingProgress(song.id, 90)

    // 更新当前搜索结果中的信息
    const originalIndex = currentSearchResults.value.findIndex(r => r.id === song.id)
    if (originalIndex !== -1) {
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
    }

    // 匹配成功，设置为100%并在1秒后清除loading状态
    updateMatchingProgress(song.id, 100)
    setTimeout(() => {
      if (matchingProgress.value[song.id] === 100) {
        delete matchingProgress.value[song.id]
      }
    }, 1000)

    showNotification(`匹配成功: ${song.title} -> ${bestResult.title}`, 'success')

  } catch (error: any) {
    console.error(`单独匹配失败: ${song.title} - ${song.artist}`, error)
    showNotification(`匹配失败: ${song.title} - ${error.message}`, 'error')

    // 匹配失败，设置为100%并在1秒后清除loading状态
    updateMatchingProgress(song.id, 100)
    setTimeout(() => {
      if (matchingProgress.value[song.id] === 100) {
        delete matchingProgress.value[song.id]
      }
    }, 1000)
  }
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

  // 如果是歌单模式，清空歌单信息和过滤器
  if (searchType.value === 'playlist') {
    setPlaylistInfo(null)
    playlistError.value = ''
    playlistFilter.value = ''
  }

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
  if (title.includes('无损') || titleLower.includes('flac') || titleLower.includes('lossless') || title.includes("Hi-res")) {
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
  const currentResults = searchType.value === 'playlist' ? filteredPlaylistResults.value : currentSearchResults.value
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
  // 如果从歌单模式切换到其他模式，只清理自动匹配相关状态，保留歌单信息
  if (searchType.value === 'playlist' && type !== 'playlist') {
    isAutoMatching.value = false
    matchingProgress.value = {}
    matchingError.value = ''
    playlistFilter.value = '' // 清空过滤器
    // 注意：不清空歌单信息，让用户可以在不同搜索模式间切换
    // setPlaylistInfo(null)  // 已注释掉
    // playlistError.value = ''  // 已注释掉
  }

  // 如果切换到歌单模式，清空过滤器
  if (type === 'playlist') {
    playlistFilter.value = ''
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

// 切换下载抽屉
const toggleDownloadDrawer = () => {
  showDownloadDrawer.value = !showDownloadDrawer.value
}

// 全部下载
const startBatchDownload = async () => {
  if (downloadQueue.value.length === 0) return
  if (isPaused.value) {
    showNotification('下载已暂停，请先恢复下载', 'info')
    return
  }

  isDownloading.value = true

  // 创建下载队列的副本，避免在遍历过程中队列被修改导致遍历异常
  const songsToDownload = [...downloadQueue.value]
  showNotification(`开始下载 ${songsToDownload.length} 首歌曲`, 'success')

  try {
    // 下载所有歌曲
    for (const song of songsToDownload) {
      if (isPaused.value) break
      if (downloadProgress.value[song.id] === undefined) {
        // 对于批量下载，使用默认格式
        const defaultFormat = { isAudio: true, ext: 'mp3' }

        // 如果是歌单歌曲，需要串行处理（需要搜索）
        if (isPlaylistSong(song)) {
          await startDownload(song, defaultFormat)
          // 每首歌之间间隔1秒，避免请求过于频繁
          if (!isPaused.value) {
            await new Promise(resolve => setTimeout(resolve, 1000))
          }
        } else {
          // 普通歌曲可以并行下载
          startDownload(song, defaultFormat)
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
            <div class="card-title-section" style="flex:1">
              <h3 class="card-title">搜索音乐</h3>
              <p class="card-subtitle flex items-center">
                <span>支持关键词搜索和直链下载</span>
                <span  v-if="searchType === 'playlist'" class="playlist-hint ml-2">
                   <Icon icon="mdi:information" class="hint-icon" />
                  <span>支持QQ音乐和网易云音乐歌单链接</span>
                </span>
              </p>
              <!-- 歌单解析提示 -->


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
                    v-if="downloadQueue.length > 0"
                    @click="toggleDownloadDrawer"
                    class="download-queue-btn"
                    :class="{ active: showDownloadDrawer }"
                >
                  <Icon icon="mdi:download-multiple" class="btn-icon" />
                  下载队列 ({{ downloadQueue.length }})
                </button>
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



      <!-- 搜索结果区域 -->
      <div v-if="currentHasResults" class="results-section">
        <div class="results-card">
          <div class="card-header">
            <Icon icon="mdi:playlist-music" class="card-icon" />
            <div class="card-title-section">
              <h3 class="card-title">{{ playlistInfo ? '歌单歌曲' : '搜索结果' }}</h3>
              <p class="card-subtitle">{{ playlistInfo ? `共 ${filteredPlaylistResults.length} 首歌曲${playlistFilter ? ` (过滤自 ${currentSearchResults.length} 首)` : ''}` : `找到 ${currentSearchResults.length} 个结果` }}</p>
            </div>

            <!-- 歌单信息 - 显示在标题右边 -->
            <div v-if="playlistInfo" class="playlist-info-inline">
              <div class="playlist-cover-small">
                <img :src="processImageUrl(playlistInfo.cover)" :alt="playlistInfo.title" class="cover-img-small" />
                <div class="platform-badge-small" :class="playlistInfo.source">
                  <Icon
                    :icon="playlistInfo.source === 'qq' ? 'simple-icons:qqmusic' : 'simple-icons:netease'"
                    class="platform-icon-small"
                  />
                </div>
              </div>
              <div class="playlist-details-inline">
                <h4 class="playlist-title-inline">{{ playlistInfo.title }}</h4>
                <div class="playlist-meta-inline">
                  <span class="meta-item">
                    <Icon icon="mdi:account" class="meta-icon" />
                    {{ playlistInfo.creator }}
                  </span>
                  <span class="meta-item">
                    <Icon icon="mdi:web" class="meta-icon" />
                    {{ playlistInfo.source === 'qq' ? 'QQ音乐' : '网易云音乐' }}
                  </span>
                </div>
              </div>

              <!-- 歌单过滤输入框 -->
              <div class="playlist-filter-container">
                <div class="filter-input-wrapper">
                  <Icon icon="mdi:magnify" class="filter-icon" />
                  <input
                    v-model="playlistFilter"
                    type="text"
                    placeholder="过滤歌曲..."
                    class="filter-input"
                  />
                  <button
                    v-if="playlistFilter"
                    @click="playlistFilter = ''"
                    class="filter-clear-btn"
                    title="清除过滤"
                  >
                    <Icon icon="mdi:close" class="clear-icon" />
                  </button>
                </div>
              </div>
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
                匹配中 ({{ Object.values(matchingProgress).filter(p => p < 100).length }}/{{ Object.keys(matchingProgress).length }}) 点击取消
              </button>
              <button
                v-if="hasSelectedItems"
                @click="() => {
                  console.log('添加到下载队列 - 当前搜索结果数量:', currentSearchResults.length)
                  console.log('添加到下载队列 - 选中项数量:', selectedResults.size)

                  // 过滤出已匹配或可下载的歌曲
                  const selectedSongs = currentSearchResults.filter(result => selectedResults.has(result.id))
                  const matchedSongs = selectedSongs.filter(song => isMatchedOrDownloadable(song))
                  const unmatchedSongs = selectedSongs.filter(song => !isMatchedOrDownloadable(song))

                  // if (unmatchedSongs.length > 0) {
                  //   showNotification(`有 ${unmatchedSongs.length} 首歌曲未匹配，只能添加已匹配的 ${matchedSongs.length} 首歌曲到下载队列`, 'warning')
                  // }

                  if (matchedSongs.length > 0) {
                    // 只添加已匹配的歌曲到下载队列
                    addToDownloadQueue(currentSearchResults, true) // 传入 true 表示只添加匹配的歌曲
                    console.log('添加后下载队列数量:', downloadQueue.length)
                    showNotification(`成功添加 ${matchedSongs.length} 首已匹配歌曲到下载队列`, 'success')
                  } else {
                    // showNotification('没有已匹配的歌曲可以添加到下载队列', 'error')
                  }
                  showNotification(`成功添加 ${matchedSongs.length} 首已匹配歌曲到下载队列，${unmatchedSongs.length} 首未匹配，无法下载`, 'success')
                }"
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
                v-for="result in (searchType === 'playlist' ? filteredPlaylistResults : currentSearchResults)"
                :key="result.id"
                class="result-card"
                :class="{
                  selected: selectedResults.has(result.id),
                  playing: currentPlaying?.id === result.id && isPlaying,
                  loading: currentPlaying?.id === result.id && isLoading
                }"
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
                <div class="result-thumbnail" @click="toggleSelection(result.id)" style="cursor: pointer;">
                  <img :src="processImageUrl(result.thumbnail)" :alt="result.title" class="thumbnail-img" />

                  <!-- 播放状态指示器 -->
                  <div v-if="currentPlaying?.id === result.id" class="playing-indicator">
                    <div class="playing-icon-wrapper">
                      <Icon
                        v-if="isLoading"
                        icon="mdi:loading"
                        class="playing-icon loading"
                      />
                      <Icon
                        v-else-if="isPlaying"
                        icon="mdi:volume-high"
                        class="playing-icon playing"
                      />
                      <Icon
                        v-else
                        icon="mdi:pause"
                        class="playing-icon paused"
                      />
                    </div>
                    <div v-if="isPlaying" class="sound-waves">
                      <div class="wave wave-1"></div>
                      <div class="wave wave-2"></div>
                      <div class="wave wave-3"></div>
                    </div>
                  </div>

                  <!-- 时长显示在图片上 -->
                  <div class="duration-overlay">
                    {{ result.duration }}
                  </div>

                  <!-- 匹配进度显示 -->
                  <div v-if="matchingProgress[result.id] !== undefined && playlistInfo" class="matching-overlay">
                    <div class="matching-progress">
                      <Icon v-if="matchingProgress[result.id] < 100" icon="mdi:loading" class="spin matching-icon loading" />
                      <Icon v-else icon="mdi:check-circle" class="matching-icon success" />
                      <span class="matching-text">
                        {{ matchingProgress[result.id] < 100 ? '自动匹配中...' : '匹配完成' }}
                      </span>
                      <!-- 进度条 -->
                      <div v-if="matchingProgress[result.id] < 100" class="progress-bar">
                        <div class="progress-fill" :style="{ width: matchingProgress[result.id] + '%' }"></div>
                      </div>
                    </div>
                  </div>

                  <!-- 平台标识 -->
                  <div class="platform-badge" :class="result.platform">
                    <Icon
                      v-if="result.platform === 'bilibili'"
                      icon="simple-icons:bilibili"
                      class="platform-icon"
                    />
                    <Icon
                      v-else-if="result.platform === 'youtube'"
                      icon="mdi:youtube"
                      class="platform-icon"
                    />
                    <img
                      v-else-if="result.platform === 'qq'"
                      src="/logo/qqmusic.ico"
                      class="platform-icon"
                      alt="QQ音乐"
                    />
                    <img
                      v-else-if="result.platform === 'netease'"
                      src="/logo/netease.ico"
                      class="platform-icon"
                      alt="网易云音乐"
                    />
                    <Icon
                      v-else
                      icon="mdi:music"
                      class="platform-icon"
                    />
                  </div>
                </div>

                <!-- 信息区域 -->
                <div class="result-info">
                  <!-- 内容区域 -->
                  <div class="result-content">
                    <div class="title-row">
                      <h4 class="result-title">{{ result.title }}</h4>
                      <span v-if="result.vip" class="vip-badge">
                        <Icon icon="mdi:crown" class="vip-icon" />
                        VIP
                      </span>
                      <span v-if="result.description && result.description.includes('(已匹配B站:')" class="matched-badge">
                        <Icon icon="mdi:check-circle" class="matched-icon" />
                        已匹配
                      </span>
                    </div>
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
                  <div class="result-actions" :class="{ 'four-buttons': getButtonCount(result) >= 4 }">
                    <button
                      @click="playMusic(result)"
                      class="play-btn"
                      :class="{
                        playing: currentPlaying?.id === result.id && isPlaying,
                        loading: currentPlaying?.id === result.id && isLoading,
                        disabled: isLoading && currentPlaying?.id !== result.id
                      }"
                      :disabled="isLoading && currentPlaying?.id !== result.id"
                      :title="
                        isLoading && currentPlaying?.id !== result.id
                          ? '其他歌曲正在加载中...'
                          : currentPlaying?.id === result.id && isPlaying
                            ? '暂停'
                            : '播放'
                      "
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

                    <!-- 单独匹配按钮 - 只在歌单模式下且歌曲未匹配时显示 -->
                    <button
                      v-if="playlistInfo && !isMatchedOrDownloadable(result) && matchingProgress[result.id] === undefined"
                      @click="matchSingleSong(result)"
                      class="match-btn"
                      :disabled="isAutoMatching"
                      title="为这首歌曲单独匹配B站资源"
                    >
                      <Icon icon="mdi:auto-fix" />
                      匹配
                    </button>

                    <!-- 匹配中状态按钮 -->
                    <button
                      v-if="playlistInfo && matchingProgress[result.id] !== undefined && matchingProgress[result.id] < 100"
                      class="matching-btn"
                      disabled
                      title="正在匹配中..."
                    >
                      <Icon icon="mdi:loading" class="spin" />
                      匹配中
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
          :src="processImageUrl(currentPlaying.thumbnail)"
          :alt="currentPlaying.title"
          class="player-thumbnail"
        />
        <div class="player-text">
          <div class="player-title-row">
            <h4 class="player-title">{{ currentPlaying.title }}</h4>
            <span v-if="currentPlaying.vip" class="vip-badge-small">
              <Icon icon="mdi:crown" class="vip-icon-small" />
              VIP
            </span>
          </div>
          <p class="player-artist">{{ currentPlaying.artist }}</p>
          <p v-if="isPlaylistSong(currentPlaying)" class="player-playlist">
            <Icon icon="mdi:playlist-music" class="playlist-icon" />
            来自歌单: {{ currentPlaying.playlistName || playlistInfo?.title }}
            <span v-if="getPlaylistPosition(currentPlaying)" class="playlist-position">
              ({{ getPlaylistPosition(currentPlaying) }})
            </span>
          </p>
          <p v-else-if="getCurrentPlaylist().length > 1 && getPlaylistPosition(currentPlaying)" class="player-playlist">
            <Icon icon="mdi:music-note-multiple" class="playlist-icon" />
            播放列表
            <span class="playlist-position">
              ({{ getPlaylistPosition(currentPlaying) }})
            </span>
          </p>
        </div>
      </div>

      <!-- 播放控制 -->
      <div class="player-controls">
        <button @click="stopMusic" class="control-btn" title="停止">
          <Icon icon="mdi:stop" />
        </button>

        <button
          @click="playPreviousSong"
          class="control-btn"
          :disabled="!hasPreviousSong || isLoading"
          :class="{ disabled: !hasPreviousSong }"
          title="上一首"
        >
          <Icon icon="mdi:skip-previous" />
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

        <button
          @click="playNextSong"
          class="control-btn"
          :disabled="!hasNextSong || isLoading"
          :class="{ disabled: !hasNextSong }"
          title="下一首"
        >
          <Icon icon="mdi:skip-next" />
        </button>

        <button
          @click="toggleLyrics"
          class="control-btn lyrics-btn"
          :class="{ active: showLyrics }"
          title="显示/隐藏歌词"
        >
          <Icon icon="mdi:music-note-outline" />
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

  <!-- 全屏歌词显示 -->
  <LyricsDisplay
    v-if="showLyrics && currentPlaying"
    :lyrics="currentLyrics"
    :lyrics-type="lyricsType"
    :lyrics-source="lyricsSource"
    :is-loading="isLyricsLoading"
    :current-time="currentTime"
    :song-title="currentPlaying.title"
    :song-artist="currentPlaying.artist"
    @close="setShowLyrics(false)"
  />


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

  <!-- 下载队列抽屉 -->
  <div v-if="showDownloadDrawer" class="drawer-overlay" @click="showDownloadDrawer = false">
    <div class="download-drawer" @click.stop>
      <div class="drawer-header">
        <div class="drawer-title-section">
          <Icon icon="mdi:download-multiple" class="drawer-icon" />
          <div>
            <h3 class="drawer-title">下载队列</h3>
            <p class="drawer-subtitle">{{ downloadQueue.length }} 个待下载项目</p>
          </div>
        </div>
        <button @click="showDownloadDrawer = false" class="drawer-close-btn">
          <Icon icon="mdi:close" />
        </button>
      </div>

      <div class="drawer-actions">
        <button
          v-if="!isDownloading && !isPaused && downloadQueue.length > 0"
          @click="startBatchDownload"
          class="drawer-action-btn primary"
        >
          <Icon icon="mdi:play" class="btn-icon" />
          开始全部下载
        </button>

        <button
          v-if="isDownloading && !isPaused"
          @click="pauseAllDownloads"
          class="drawer-action-btn warning"
        >
          <Icon icon="mdi:pause" class="btn-icon" />
          全部暂停
        </button>

        <button
          v-if="isPaused"
          @click="resumeAllDownloads"
          class="drawer-action-btn success"
        >
          <Icon icon="mdi:play" class="btn-icon" />
          恢复下载
        </button>

        <button
          @click="clearDownloadQueue"
          class="drawer-action-btn danger"
          :disabled="isDownloading && !isPaused"
        >
          <Icon icon="mdi:delete-sweep" class="btn-icon" />
          清空队列
        </button>
      </div>

      <div class="drawer-content">
        <div v-if="downloadQueue.length === 0" class="drawer-empty">
          <Icon icon="mdi:download-off-outline" class="empty-icon" />
          <p class="empty-text">下载队列为空</p>
          <p class="empty-hint">选择歌曲并添加到队列开始下载</p>
        </div>

        <div v-else class="drawer-list">
          <div
            v-for="item in downloadQueue"
            :key="item.id"
            class="drawer-item"
          >
            <div class="item-thumbnail">
              <img :src="processImageUrl(item.thumbnail)" :alt="item.title" class="thumbnail-img" />
              <div class="platform-badge" :class="item.platform">
                <Icon
                  :icon="item.platform === 'bilibili' ? 'simple-icons:bilibili' : 'mdi:youtube'"
                  class="platform-icon"
                />
              </div>
            </div>

            <div class="item-info">
              <div class="item-title-row">
                <h4 class="item-title">{{ item.title }}</h4>
                <span v-if="item.vip" class="vip-badge-small">
                  <Icon icon="mdi:crown" class="vip-icon-small" />
                  VIP
                </span>
              </div>
              <p class="item-artist">{{ item.artist }}</p>

              <!-- 元数据信息 -->
              <div class="item-meta">
                <span class="duration">
                  <Icon icon="mdi:clock-outline" />
                  {{ item.duration }}
                </span>
                <span v-if="item.playCount" class="play-count">
                  <Icon icon="mdi:play" />
                  {{ item.playCount }}
                </span>
              </div>

              <!-- 下载进度 -->
              <div v-if="downloadProgress[item.id] !== undefined" class="item-progress">
                <div class="progress-bar">
                  <div
                    class="progress-fill"
                    :style="{ width: downloadProgress[item.id] + '%' }"
                  ></div>
                </div>
                <span class="progress-text">{{ Math.round(downloadProgress[item.id]) }}%</span>
              </div>
            </div>

            <div class="item-actions">
              <button
                v-if="downloadProgress[item.id] === undefined"
                @click="showFormatSelection(item)"
                class="item-action-btn start"
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
                class="item-action-btn remove"
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
</template>

<style scoped>

</style>