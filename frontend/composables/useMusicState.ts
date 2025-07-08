/**
 * 音乐状态管理 composable
 * 用于在页面切换时保持音乐搜索结果和播放状态
 */

import { ref, computed, readonly } from 'vue'
import type { MusicSearchResult, PlaylistInfo } from './useMusicApi'

// 全局音乐状态
const searchResults = ref<MusicSearchResult[]>([])
const selectedResults = ref<Set<string>>(new Set())
const downloadQueue = ref<MusicSearchResult[]>([])
const downloadProgress = ref<Record<string, number>>({})

// 播放状态
const currentPlaying = ref<MusicSearchResult | null>(null)
const isPlaying = ref(false)
const isLoading = ref(false)
const audioElement = ref<HTMLAudioElement | null>(null)
const currentTime = ref(0)
const totalDuration = ref(0)
const volume = ref(1)

// 搜索状态
const searchQuery = ref('')
const searchType = ref<'keyword' | 'url' | 'playlist'>('keyword')
const platform = ref<'bilibili' | 'youtube' | 'both'>('both')
const isSearching = ref(false)
const searchError = ref('')

// 独立的搜索内容状态
const keywordSearchQuery = ref('')
const urlSearchQuery = ref('')
const playlistSearchQuery = ref('')

// 独立的搜索结果状态
const keywordSearchResults = ref<MusicSearchResult[]>([])
const urlSearchResults = ref<MusicSearchResult[]>([])
const playlistSearchResults = ref<MusicSearchResult[]>([])

// 独立的搜索状态
const keywordSearching = ref(false)
const urlSearching = ref(false)
const playlistSearching = ref(false)

// 独立的搜索错误状态
const keywordSearchError = ref('')
const urlSearchError = ref('')
const playlistSearchError = ref('')

// 歌单信息状态
const playlistInfo = ref<PlaylistInfo | null>(null)

// 状态持久化键名
const STORAGE_KEYS = {
  SEARCH_RESULTS: 'music_search_results',
  SEARCH_QUERY: 'music_search_query',
  SEARCH_TYPE: 'music_search_type',
  SEARCH_PLATFORM: 'music_search_platform',
  DOWNLOAD_QUEUE: 'music_download_queue',
  VOLUME: 'music_volume',
  CURRENT_PLAYING: 'music_current_playing',

  // 独立搜索内容
  KEYWORD_SEARCH_QUERY: 'music_keyword_search_query',
  URL_SEARCH_QUERY: 'music_url_search_query',
  PLAYLIST_SEARCH_QUERY: 'music_playlist_search_query',

  // 独立搜索结果
  KEYWORD_SEARCH_RESULTS: 'music_keyword_search_results',
  URL_SEARCH_RESULTS: 'music_url_search_results',
  PLAYLIST_SEARCH_RESULTS: 'music_playlist_search_results',

  // 独立搜索错误
  KEYWORD_SEARCH_ERROR: 'music_keyword_search_error',
  URL_SEARCH_ERROR: 'music_url_search_error',
  PLAYLIST_SEARCH_ERROR: 'music_playlist_search_error',

  // 歌单信息
  PLAYLIST_INFO: 'music_playlist_info'
}

// 从 localStorage 恢复状态
const restoreState = () => {
  if (!process.client) return

  try {
    // 恢复搜索结果
    const savedResults = localStorage.getItem(STORAGE_KEYS.SEARCH_RESULTS)
    if (savedResults) {
      searchResults.value = JSON.parse(savedResults)
    }

    // 恢复搜索参数
    const savedQuery = localStorage.getItem(STORAGE_KEYS.SEARCH_QUERY)
    if (savedQuery) {
      searchQuery.value = savedQuery
    }

    const savedType = localStorage.getItem(STORAGE_KEYS.SEARCH_TYPE)
    if (savedType) {
      searchType.value = savedType as 'keyword' | 'url' | 'playlist'
    }

    const savedPlatform = localStorage.getItem(STORAGE_KEYS.SEARCH_PLATFORM)
    if (savedPlatform) {
      platform.value = savedPlatform as 'bilibili' | 'youtube' | 'both'
    }

    // 恢复下载队列
    const savedQueue = localStorage.getItem(STORAGE_KEYS.DOWNLOAD_QUEUE)
    if (savedQueue) {
      downloadQueue.value = JSON.parse(savedQueue)
    }

    // 恢复音量设置
    const savedVolume = localStorage.getItem(STORAGE_KEYS.VOLUME)
    if (savedVolume) {
      volume.value = parseFloat(savedVolume)
    }

    // 恢复当前播放信息（但不自动播放）
    const savedPlaying = localStorage.getItem(STORAGE_KEYS.CURRENT_PLAYING)
    if (savedPlaying) {
      currentPlaying.value = JSON.parse(savedPlaying)
    }

    // 恢复独立搜索内容
    const savedKeywordQuery = localStorage.getItem(STORAGE_KEYS.KEYWORD_SEARCH_QUERY)
    if (savedKeywordQuery) {
      keywordSearchQuery.value = savedKeywordQuery
    }

    const savedUrlQuery = localStorage.getItem(STORAGE_KEYS.URL_SEARCH_QUERY)
    if (savedUrlQuery) {
      urlSearchQuery.value = savedUrlQuery
    }

    const savedPlaylistQuery = localStorage.getItem(STORAGE_KEYS.PLAYLIST_SEARCH_QUERY)
    if (savedPlaylistQuery) {
      playlistSearchQuery.value = savedPlaylistQuery
    }

    // 恢复独立搜索结果
    const savedKeywordResults = localStorage.getItem(STORAGE_KEYS.KEYWORD_SEARCH_RESULTS)
    if (savedKeywordResults) {
      keywordSearchResults.value = JSON.parse(savedKeywordResults)
    }

    const savedUrlResults = localStorage.getItem(STORAGE_KEYS.URL_SEARCH_RESULTS)
    if (savedUrlResults) {
      urlSearchResults.value = JSON.parse(savedUrlResults)
    }

    const savedPlaylistResults = localStorage.getItem(STORAGE_KEYS.PLAYLIST_SEARCH_RESULTS)
    if (savedPlaylistResults) {
      playlistSearchResults.value = JSON.parse(savedPlaylistResults)
    }

    // 恢复独立搜索错误
    const savedKeywordError = localStorage.getItem(STORAGE_KEYS.KEYWORD_SEARCH_ERROR)
    if (savedKeywordError) {
      keywordSearchError.value = savedKeywordError
    }

    const savedUrlError = localStorage.getItem(STORAGE_KEYS.URL_SEARCH_ERROR)
    if (savedUrlError) {
      urlSearchError.value = savedUrlError
    }

    const savedPlaylistError = localStorage.getItem(STORAGE_KEYS.PLAYLIST_SEARCH_ERROR)
    if (savedPlaylistError) {
      playlistSearchError.value = savedPlaylistError
    }

    // 恢复歌单信息
    const savedPlaylistInfo = localStorage.getItem(STORAGE_KEYS.PLAYLIST_INFO)
    if (savedPlaylistInfo) {
      playlistInfo.value = JSON.parse(savedPlaylistInfo)
    }

  } catch (error) {
    console.error('恢复音乐状态失败:', error)
  }
}

// 保存状态到 localStorage
const saveState = () => {
  if (!process.client) return

  try {
    localStorage.setItem(STORAGE_KEYS.SEARCH_RESULTS, JSON.stringify(searchResults.value))
    localStorage.setItem(STORAGE_KEYS.SEARCH_QUERY, searchQuery.value)
    localStorage.setItem(STORAGE_KEYS.SEARCH_TYPE, searchType.value)
    localStorage.setItem(STORAGE_KEYS.SEARCH_PLATFORM, platform.value)
    localStorage.setItem(STORAGE_KEYS.DOWNLOAD_QUEUE, JSON.stringify(downloadQueue.value))
    localStorage.setItem(STORAGE_KEYS.VOLUME, volume.value.toString())

    // 保存独立搜索内容
    localStorage.setItem(STORAGE_KEYS.KEYWORD_SEARCH_QUERY, keywordSearchQuery.value)
    localStorage.setItem(STORAGE_KEYS.URL_SEARCH_QUERY, urlSearchQuery.value)
    localStorage.setItem(STORAGE_KEYS.PLAYLIST_SEARCH_QUERY, playlistSearchQuery.value)

    // 保存独立搜索结果
    localStorage.setItem(STORAGE_KEYS.KEYWORD_SEARCH_RESULTS, JSON.stringify(keywordSearchResults.value))
    localStorage.setItem(STORAGE_KEYS.URL_SEARCH_RESULTS, JSON.stringify(urlSearchResults.value))
    localStorage.setItem(STORAGE_KEYS.PLAYLIST_SEARCH_RESULTS, JSON.stringify(playlistSearchResults.value))

    // 保存独立搜索错误
    localStorage.setItem(STORAGE_KEYS.KEYWORD_SEARCH_ERROR, keywordSearchError.value)
    localStorage.setItem(STORAGE_KEYS.URL_SEARCH_ERROR, urlSearchError.value)
    localStorage.setItem(STORAGE_KEYS.PLAYLIST_SEARCH_ERROR, playlistSearchError.value)

    // 保存歌单信息
    if (playlistInfo.value) {
      localStorage.setItem(STORAGE_KEYS.PLAYLIST_INFO, JSON.stringify(playlistInfo.value))
    } else {
      localStorage.removeItem(STORAGE_KEYS.PLAYLIST_INFO)
    }

    if (currentPlaying.value) {
      localStorage.setItem(STORAGE_KEYS.CURRENT_PLAYING, JSON.stringify(currentPlaying.value))
    } else {
      localStorage.removeItem(STORAGE_KEYS.CURRENT_PLAYING)
    }
  } catch (error) {
    console.error('保存音乐状态失败:', error)
  }
}

// 清空播放相关状态
const clearPlayingState = () => {
  // 停止播放
  if (audioElement.value) {
    audioElement.value.pause()
    audioElement.value = null
  }
  currentPlaying.value = null
  isPlaying.value = false
  isLoading.value = false
  currentTime.value = 0
  totalDuration.value = 0

  // 清除播放相关的本地存储
  if (process.client) {
    localStorage.removeItem(STORAGE_KEYS.CURRENT_PLAYING)
  }
}

// 清空所有状态
const clearAllState = () => {
  searchResults.value = []
  selectedResults.value.clear()
  downloadQueue.value = []

  // 清空独立搜索内容
  keywordSearchQuery.value = ''
  urlSearchQuery.value = ''
  playlistSearchQuery.value = ''

  // 清空独立搜索结果
  keywordSearchResults.value = []
  urlSearchResults.value = []
  playlistSearchResults.value = []

  // 清空独立搜索错误
  keywordSearchError.value = ''
  urlSearchError.value = ''
  playlistSearchError.value = ''
  downloadProgress.value = {}
  searchQuery.value = ''
  searchError.value = ''

  // 清空歌单信息
  playlistInfo.value = null

  // 停止播放
  if (audioElement.value) {
    audioElement.value.pause()
    audioElement.value = null
  }
  currentPlaying.value = null
  isPlaying.value = false
  isLoading.value = false
  currentTime.value = 0
  totalDuration.value = 0

  // 清除本地存储
  if (process.client) {
    Object.values(STORAGE_KEYS).forEach(key => {
      localStorage.removeItem(key)
    })
  }
}

// 初始化状态（只在客户端执行一次）
let isInitialized = false
if (process.client && !isInitialized) {
  restoreState()
  isInitialized = true
  
  // 监听页面卸载，保存状态
  window.addEventListener('beforeunload', saveState)
}

/**
 * 音乐状态管理 composable
 */
export const useMusicState = () => {
  // 计算属性
  const hasResults = computed(() => searchResults.value.length > 0)
  const hasSelectedItems = computed(() => selectedResults.value.size > 0)
  const playProgress = computed(() => {
    if (totalDuration.value === 0) return 0
    return (currentTime.value / totalDuration.value) * 100
  })

  // 搜索结果管理
  const setSearchResults = (results: MusicSearchResult[]) => {
    searchResults.value = results
    selectedResults.value.clear()
    saveState()
  }

  const clearSearchResults = () => {
    searchResults.value = []
    selectedResults.value.clear()
    searchError.value = ''
    saveState()
  }

  // 选择管理
  const toggleSelection = (id: string) => {
    if (selectedResults.value.has(id)) {
      selectedResults.value.delete(id)
    } else {
      selectedResults.value.add(id)
    }
  }

  const toggleSelectAll = () => {
    if (selectedResults.value.size === searchResults.value.length) {
      selectedResults.value.clear()
    } else {
      selectedResults.value = new Set(searchResults.value.map(item => item.id))
    }
  }

  // 下载队列管理
  const addToDownloadQueue = (sourceResults?: MusicSearchResult[], onlyMatched?: boolean) => {
    // 如果提供了源结果，使用提供的结果；否则使用全局搜索结果
    const sourceItems = sourceResults || searchResults.value
    let selectedItems = sourceItems.filter(item => selectedResults.value.has(item.id))

    // 如果指定只添加匹配的歌曲，则进行过滤
    if (onlyMatched) {
      selectedItems = selectedItems.filter(item => {
        // 检查是否已经通过自动匹配
        if (item.description && item.description.includes('(已匹配B站:')) {
          return true
        }

        //网易平台歌曲可以直接下载
        if (item.platform == 'netease' && item.vip == false){
          return true
        }

        // 检查是否本身就是来自 bilibili/youtube 的链接或关键词搜索结果
        // 注意：这里无法直接访问 searchType，所以通过其他方式判断
        // 如果有 playlistName 说明是歌单歌曲，需要匹配才能下载
        if (item.playlistName || (item.description && item.description.includes('来自歌单:'))) {
          return false // 歌单歌曲必须匹配后才能下载
        }

        // 其他情况（关键词搜索、URL搜索）都可以下载
        return true
      })
    }

    // 避免重复添加
    selectedItems.forEach(item => {
      if (!downloadQueue.value.find(queueItem => queueItem.id === item.id)) {
        downloadQueue.value.push(item)
      }
    })

    selectedResults.value.clear()
    saveState()
  }

  const removeFromQueue = (id: string) => {
    const index = downloadQueue.value.findIndex(item => item.id === id)
    if (index > -1) {
      downloadQueue.value.splice(index, 1)
      delete downloadProgress.value[id]
      saveState()
    }
  }

  const clearDownloadQueue = () => {
    downloadQueue.value = []
    downloadProgress.value = {}
    saveState()
  }

  // 搜索状态管理
  const setSearchQuery = (query: string) => {
    searchQuery.value = query
    saveState()
  }

  const setSearchType = (type: 'keyword' | 'url') => {
    searchType.value = type
    saveState()
  }

  const setPlatform = (p: 'bilibili' | 'youtube' | 'both') => {
    platform.value = p
    saveState()
  }

  const setSearching = (searching: boolean) => {
    isSearching.value = searching
  }

  const setSearchError = (error: string) => {
    searchError.value = error
  }

  // 播放状态管理
  const setCurrentPlaying = (music: MusicSearchResult | null) => {
    currentPlaying.value = music
    saveState()
  }

  const setPlaying = (playing: boolean) => {
    isPlaying.value = playing
  }

  const setLoading = (loading: boolean) => {
    isLoading.value = loading
  }

  const setAudioElement = (audio: HTMLAudioElement | null) => {
    audioElement.value = audio
  }

  const setCurrentTime = (time: number) => {
    currentTime.value = time
  }

  const setTotalDuration = (duration: number) => {
    totalDuration.value = duration
  }

  const setVolume = (vol: number) => {
    volume.value = vol
    saveState()
  }

  // 下载进度管理
  const setDownloadProgress = (id: string, progress: number) => {
    downloadProgress.value[id] = progress
  }

  const removeDownloadProgress = (id: string) => {
    delete downloadProgress.value[id]
  }

  // 独立搜索状态管理方法
  const setKeywordSearchQuery = (query: string) => {
    keywordSearchQuery.value = query
    saveState()
  }

  const setUrlSearchQuery = (query: string) => {
    urlSearchQuery.value = query
    saveState()
  }

  const setPlaylistSearchQuery = (query: string) => {
    playlistSearchQuery.value = query
    saveState()
  }

  const setKeywordSearchResults = (results: MusicSearchResult[]) => {
    keywordSearchResults.value = results
    saveState()
  }

  const setUrlSearchResults = (results: MusicSearchResult[]) => {
    urlSearchResults.value = results
    saveState()
  }

  const setPlaylistSearchResults = (results: MusicSearchResult[]) => {
    playlistSearchResults.value = results
    saveState()
  }

  const setKeywordSearching = (searching: boolean) => {
    keywordSearching.value = searching
  }

  const setUrlSearching = (searching: boolean) => {
    urlSearching.value = searching
  }

  const setPlaylistSearching = (searching: boolean) => {
    playlistSearching.value = searching
  }

  const setKeywordSearchError = (error: string) => {
    keywordSearchError.value = error
    saveState()
  }

  const setUrlSearchError = (error: string) => {
    urlSearchError.value = error
    saveState()
  }

  const setPlaylistSearchError = (error: string) => {
    playlistSearchError.value = error
    saveState()
  }

  // 歌单信息管理
  const setPlaylistInfo = (info: PlaylistInfo | null) => {
    playlistInfo.value = info
    saveState()
  }

  const clearPlaylistInfo = () => {
    playlistInfo.value = null
    saveState()
  }

  return {
    // 只读状态
    searchResults: readonly(searchResults),
    selectedResults: readonly(selectedResults),
    downloadQueue: readonly(downloadQueue),
    downloadProgress: readonly(downloadProgress),
    currentPlaying: readonly(currentPlaying),
    isPlaying: readonly(isPlaying),
    isLoading: readonly(isLoading),
    audioElement: readonly(audioElement),
    currentTime: readonly(currentTime),
    totalDuration: readonly(totalDuration),
    volume: readonly(volume),
    searchQuery: readonly(searchQuery),
    searchType: readonly(searchType),
    platform: readonly(platform),
    isSearching: readonly(isSearching),
    searchError: readonly(searchError),

    // 独立搜索状态
    keywordSearchQuery: readonly(keywordSearchQuery),
    urlSearchQuery: readonly(urlSearchQuery),
    playlistSearchQuery: readonly(playlistSearchQuery),
    keywordSearchResults: readonly(keywordSearchResults),
    urlSearchResults: readonly(urlSearchResults),
    playlistSearchResults: readonly(playlistSearchResults),
    keywordSearching: readonly(keywordSearching),
    urlSearching: readonly(urlSearching),
    playlistSearching: readonly(playlistSearching),
    keywordSearchError: readonly(keywordSearchError),
    urlSearchError: readonly(urlSearchError),
    playlistSearchError: readonly(playlistSearchError),

    // 歌单信息
    playlistInfo: readonly(playlistInfo),

    // 计算属性
    hasResults,
    hasSelectedItems,
    playProgress,
    
    // 状态管理方法
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
    saveState,
    restoreState,

    // 独立搜索状态管理方法
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
    setPlaylistSearchError,

    // 歌单信息管理方法
    setPlaylistInfo,
    clearPlaylistInfo
  }
}
