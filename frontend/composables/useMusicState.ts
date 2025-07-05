/**
 * 音乐状态管理 composable
 * 用于在页面切换时保持音乐搜索结果和播放状态
 */

import { ref, computed, readonly } from 'vue'
import type { MusicSearchResult } from './useMusicApi'

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
const searchType = ref<'keyword' | 'url'>('keyword')
const platform = ref<'bilibili' | 'youtube' | 'both'>('both')
const isSearching = ref(false)
const searchError = ref('')

// 状态持久化键名
const STORAGE_KEYS = {
  SEARCH_RESULTS: 'music_search_results',
  SEARCH_QUERY: 'music_search_query',
  SEARCH_TYPE: 'music_search_type',
  SEARCH_PLATFORM: 'music_search_platform',
  DOWNLOAD_QUEUE: 'music_download_queue',
  VOLUME: 'music_volume',
  CURRENT_PLAYING: 'music_current_playing'
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
      searchType.value = savedType as 'keyword' | 'url'
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
  downloadProgress.value = {}
  searchQuery.value = ''
  searchError.value = ''

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
  const addToDownloadQueue = () => {
    const selectedItems = searchResults.value.filter(item => selectedResults.value.has(item.id))
    downloadQueue.value.push(...selectedItems)
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
    restoreState
  }
}
