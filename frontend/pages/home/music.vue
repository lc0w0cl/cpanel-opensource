<script setup lang="ts">
import { ref, computed } from 'vue'
import { Icon } from '@iconify/vue'
import { useMusicApi, type MusicSearchResult } from '~/composables/useMusicApi'

// 搜索相关状态
const searchQuery = ref('')
const searchType = ref<'keyword' | 'url'>('keyword')
const platform = ref<'bilibili' | 'youtube' | 'both'>('both')
const isSearching = ref(false)
const searchError = ref('')

// 搜索结果
const searchResults = ref<MusicSearchResult[]>([])
const selectedResults = ref<Set<string>>(new Set())

// 音乐API
const { searchMusic, downloadMusic } = useMusicApi()

// 下载相关状态
const downloadQueue = ref<MusicSearchResult[]>([])
const isDownloading = ref(false)
const downloadProgress = ref<Record<string, number>>({})

// 计算属性
const hasResults = computed(() => searchResults.value.length > 0)
const hasSelectedItems = computed(() => selectedResults.value.size > 0)
const isValidUrl = computed(() => {
  if (searchType.value !== 'url') return true
  const url = searchQuery.value.trim()
  return url.includes('bilibili.com') || url.includes('youtube.com') || url.includes('youtu.be')
})

// 搜索方法
const handleSearch = async () => {
  if (!searchQuery.value.trim()) return

  isSearching.value = true

  try {
    // 构建搜索请求
    const searchRequest = {
      query: searchQuery.value.trim(),
      searchType: searchType.value,
      platform: platform.value,
      page: 1,
      pageSize: 20
    }

    // 使用音乐API搜索
    const results = await searchMusic(searchRequest)

    searchResults.value = results
    searchError.value = ''

  } catch (error: any) {
    console.error('搜索请求失败:', error)
    searchError.value = error.message || '网络请求失败，请检查网络连接'
    searchResults.value = []
  } finally {
    isSearching.value = false
  }
}

// 切换选择
const toggleSelection = (id: string) => {
  if (selectedResults.value.has(id)) {
    selectedResults.value.delete(id)
  } else {
    selectedResults.value.add(id)
  }
}

// 全选/取消全选
const toggleSelectAll = () => {
  if (selectedResults.value.size === searchResults.value.length) {
    selectedResults.value.clear()
  } else {
    selectedResults.value = new Set(searchResults.value.map(item => item.id))
  }
}

// 添加到下载队列
const addToDownloadQueue = () => {
  const selectedItems = searchResults.value.filter(item => selectedResults.value.has(item.id))
  downloadQueue.value.push(...selectedItems)
  selectedResults.value.clear()
}

// 开始下载
const startDownload = async (item: MusicSearchResult) => {
  isDownloading.value = true
  downloadProgress.value[item.id] = 0

  try {
    // 调用下载API
    const success = await downloadMusic(item.url)

    if (success) {
      // 模拟下载进度
      const interval = setInterval(() => {
        downloadProgress.value[item.id] += Math.random() * 20
        if (downloadProgress.value[item.id] >= 100) {
          downloadProgress.value[item.id] = 100
          clearInterval(interval)

          // 检查是否所有下载完成
          const allCompleted = downloadQueue.value.every(queueItem =>
            downloadProgress.value[queueItem.id] === 100
          )
          if (allCompleted) {
            isDownloading.value = false
          }
        }
      }, 200)
    } else {
      // 下载失败
      delete downloadProgress.value[item.id]
      isDownloading.value = false
      console.error('下载失败:', item.title)
    }
  } catch (error) {
    console.error('下载异常:', error)
    delete downloadProgress.value[item.id]
    isDownloading.value = false
  }
}

// 移除下载项
const removeFromQueue = (id: string) => {
  const index = downloadQueue.value.findIndex(item => item.id === id)
  if (index > -1) {
    downloadQueue.value.splice(index, 1)
    delete downloadProgress.value[id]
  }
}

// 清空搜索结果
const clearResults = () => {
  searchResults.value = []
  selectedResults.value.clear()
  searchError.value = ''
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

            <div class="header-right">
              <button
                  v-if="hasResults"
                  @click="clearResults"
                  class="clear-btn"
              >
                <Icon icon="mdi:broom" class="btn-icon" />
                清空结果
              </button>
            </div>
          </div>

          <div class="card-content">
            <!-- 搜索类型选择 -->
            <div class="search-type-tabs">
              <button
                @click="searchType = 'keyword'"
                :class="['tab-btn', { active: searchType === 'keyword' }]"
              >
                <Icon icon="mdi:magnify" />
                关键词搜索
              </button>
              <button
                @click="searchType = 'url'"
                :class="['tab-btn', { active: searchType === 'url' }]"
              >
                <Icon icon="mdi:link" />
                链接下载
              </button>
            </div>

            <!-- 平台选择 -->
            <div v-if="searchType === 'keyword'" class="platform-selection">
              <label class="platform-label">搜索平台：</label>
              <div class="platform-buttons">
                <button
                  @click="platform = 'both'"
                  :class="['platform-btn', { active: platform === 'both' }]"
                >
                  <Icon icon="mdi:earth" />
                  全部
                </button>
                <button
                  @click="platform = 'bilibili'"
                  :class="['platform-btn', { active: platform === 'bilibili' }]"
                >
                  <Icon icon="simple-icons:bilibili" />
                  哔哩哔哩
                </button>
                <button
                  @click="platform = 'youtube'"
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
                  v-model="searchQuery"
                  @keyup.enter="handleSearch"
                  type="text"
                  :placeholder="searchType === 'keyword' ? '输入歌曲名称、歌手或关键词...' : '粘贴视频链接...'"
                  class="search-input"
                  :class="{ invalid: searchType === 'url' && !isValidUrl }"
                />
                <button
                  @click="handleSearch"
                  :disabled="!searchQuery.trim() || (searchType === 'url' && !isValidUrl) || isSearching"
                  class="search-btn"
                >
                  <Icon v-if="isSearching" icon="mdi:loading" class="spin btn-icon" />
                  <Icon v-else icon="mdi:magnify" class="btn-icon" />
                  {{ isSearching ? '搜索中...' : '搜索' }}
                </button>
              </div>

              <!-- URL验证提示 -->
              <div v-if="searchType === 'url' && searchQuery && !isValidUrl" class="url-hint">
                <Icon icon="mdi:alert-circle" class="hint-icon" />
                <span>请输入有效的哔哩哔哩或YouTube链接</span>
              </div>

              <!-- 搜索错误提示 -->
              <div v-if="searchError" class="search-error">
                <Icon icon="mdi:alert-circle" class="error-icon" />
                <span>{{ searchError }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 搜索结果区域 -->
      <div v-if="hasResults" class="results-section">
        <div class="results-card">
          <div class="card-header">
            <Icon icon="mdi:playlist-music" class="card-icon" />
            <div class="card-title-section">
              <h3 class="card-title">搜索结果</h3>
              <p class="card-subtitle">找到 {{ searchResults.length }} 个结果</p>
            </div>
            <div class="header-actions">
              <button
                @click="toggleSelectAll"
                class="select-all-btn"
              >
                <Icon :icon="selectedResults.size === searchResults.length ? 'mdi:checkbox-marked' : 'mdi:checkbox-blank-outline'" />
                {{ selectedResults.size === searchResults.length ? '取消全选' : '全选' }}
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
            <div class="results-list">
              <div
                v-for="result in searchResults"
                :key="result.id"
                class="result-item"
                :class="{ selected: selectedResults.has(result.id) }"
              >
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

                <div class="result-thumbnail">
                  <img :src="result.thumbnail" :alt="result.title" class="thumbnail-img" />
                  <div class="platform-badge" :class="result.platform">
                    <Icon
                      :icon="result.platform === 'bilibili' ? 'simple-icons:bilibili' : 'mdi:youtube'"
                      class="platform-icon"
                    />
                  </div>
                </div>

                <div class="result-info">
                  <h4 class="result-title">{{ result.title }}</h4>
                  <p class="result-artist">{{ result.artist }}</p>
                  <div class="result-meta">
                    <span class="duration">
                      <Icon icon="mdi:clock-outline" />
                      {{ result.duration }}
                    </span>
                    <span class="quality">
                      <Icon icon="mdi:high-definition" />
                      {{ result.quality }}
                    </span>
                  </div>
                </div>

                <div class="result-actions">
                  <button
                    @click="startDownload(result)"
                    class="download-btn"
                    title="立即下载"
                  >
                    <Icon icon="mdi:download" />
                  </button>
                  <a
                    :href="result.url"
                    target="_blank"
                    class="view-btn"
                    title="查看原视频"
                  >
                    <Icon icon="mdi:open-in-new" />
                  </a>
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
                v-if="!isDownloading"
                @click="downloadQueue.forEach(item => startDownload(item))"
                class="start-all-btn"
              >
                <Icon icon="mdi:play" class="btn-icon" />
                开始全部下载
              </button>
              <button
                @click="downloadQueue.splice(0); Object.keys(downloadProgress).forEach(key => delete downloadProgress[key])"
                class="clear-queue-btn"
                :disabled="isDownloading"
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
                    @click="startDownload(item)"
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
      <div v-if="!hasResults && !isSearching" class="empty-state">
        <div class="empty-icon">
          <Icon icon="mdi:music-note-outline" />
        </div>
        <h3 class="empty-title">开始搜索音乐</h3>
        <p class="empty-subtitle">输入关键词或粘贴视频链接来搜索和下载音乐</p>
      </div>
    </div>
  </NuxtLayout>
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
  gap: 0.5rem;
  margin-bottom: 1.5rem;
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
.start-all-btn,
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
.start-all-btn:hover {
  color: rgba(255, 255, 255, 0.9);
  border-color: rgba(255, 255, 255, 0.3);
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

/* 结果列表 */
.results-list,
.download-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.result-item,
.download-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: border-color 0.3s ease, transform 0.2s ease;
}

.result-item:hover,
.download-item:hover {
  border-color: rgba(255, 255, 255, 0.2);
}

.result-item.selected {
  border-color: rgba(59, 130, 246, 0.5);
  background: linear-gradient(135deg,
    rgba(59, 130, 246, 0.1) 0%,
    rgba(59, 130, 246, 0.05) 100%
  );
}

/* 复选框 */
.result-checkbox {
  flex-shrink: 0;
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
.result-thumbnail,
.download-thumbnail {
  position: relative;
  flex-shrink: 0;
}

.thumbnail-img {
  width: 80px;
  height: 60px;
  border-radius: 0.5rem;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.platform-badge {
  position: absolute;
  top: -0.25rem;
  right: -0.25rem;
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid rgba(0, 0, 0, 0.8);
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
.result-info,
.download-info {
  flex: 1;
  min-width: 0;
}

.result-title,
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

.result-artist,
.download-artist {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 0.5rem 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-meta {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.duration,
.quality {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.7rem;
  color: rgba(255, 255, 255, 0.5);
}

.duration svg,
.quality svg {
  width: 0.75rem;
  height: 0.75rem;
}

/* 操作按钮 */
.result-actions,
.download-actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-shrink: 0;
}

.download-btn,
.view-btn,
.start-btn,
.remove-btn {
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

.download-btn:hover,
.start-btn:hover {
  background: linear-gradient(135deg,
    rgba(34, 197, 94, 0.15) 0%,
    rgba(34, 197, 94, 0.08) 100%
  );
  border-color: rgba(34, 197, 94, 0.3);
  color: rgba(34, 197, 94, 0.9);
}

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
</style>