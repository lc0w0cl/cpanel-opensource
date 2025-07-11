<template>
  <div class="lyrics-display">
    <!-- 歌词头部 -->
    <div class="lyrics-header">
      <div class="lyrics-title">
        <Icon icon="mdi:music-note" class="lyrics-icon" />
        <span>歌词</span>
      </div>
      <div class="lyrics-controls">
        <button
          v-if="lyricsSource"
          class="lyrics-source-btn"
          :title="`歌词来源: ${lyricsSource}`"
        >
          <Icon icon="mdi:information-outline" />
          {{ lyricsSource }}
        </button>
        <button
          @click="$emit('close')"
          class="close-btn"
          title="关闭歌词"
        >
          <Icon icon="mdi:close" />
        </button>
      </div>
    </div>

    <!-- 歌词内容 -->
    <div class="lyrics-content">
      <!-- 加载状态 -->
      <div v-if="isLoading" class="lyrics-loading">
        <Icon icon="mdi:loading" class="loading-icon" />
        <span>正在获取歌词...</span>
      </div>

      <!-- 歌词文本 -->
      <div v-else-if="lyrics" class="lyrics-text">
        <div
          v-for="(line, index) in lyricsLines"
          :key="index"
          class="lyrics-line"
          :class="{ 'current-line': index === currentLineIndex }"
        >
          {{ typeof line === 'string' ? line : line.text }}
        </div>
      </div>

      <!-- 无歌词状态 -->
      <div v-else class="no-lyrics">
        <Icon icon="mdi:music-note-off" class="no-lyrics-icon" />
        <span>暂无歌词</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, nextTick } from 'vue'
import { Icon } from '@iconify/vue'

interface Props {
  lyrics: string
  lyricsType: string
  lyricsSource: string
  isLoading: boolean
  currentTime: number
}

interface Emits {
  (e: 'close'): void
}

interface LyricsLine {
  time: number
  text: string
  originalLine: string
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 当前高亮行索引
const currentLineIndex = ref(0)

// 解析LRC格式歌词
const parseLrcLyrics = (lrcText: string): LyricsLine[] => {
  if (!lrcText) return []

  const lines = lrcText.split('\n')
  const lyricsLines: LyricsLine[] = []

  for (const line of lines) {
    // 匹配时间标签 [mm:ss.xx]
    const timeMatch = line.match(/\[(\d{2}):(\d{2})\.(\d{2})\](.*)/)
    if (timeMatch) {
      const minutes = parseInt(timeMatch[1])
      const seconds = parseInt(timeMatch[2])
      const centiseconds = parseInt(timeMatch[3])
      const text = timeMatch[4].trim()

      if (text) {
        const time = minutes * 60 + seconds + centiseconds / 100
        lyricsLines.push({
          time,
          text,
          originalLine: line
        })
      }
    }
  }

  return lyricsLines.sort((a, b) => a.time - b.time)
}

// 计算歌词行数组
const lyricsLines = computed(() => {
  if (!props.lyrics) return []

  // 如果是LRC格式，解析时间标签
  if (props.lyricsType === 'lrc') {
    return parseLrcLyrics(props.lyrics)
  }

  // 普通文本格式
  return props.lyrics.split('\n')
    .filter(line => line.trim() !== '')
    .map((text, index) => ({
      time: index * 5, // 简单的时间映射
      text,
      originalLine: text
    }))
})

// 监听播放时间变化，更新当前行
watch(() => props.currentTime, (newTime) => {
  if (lyricsLines.value.length === 0) return

  // 找到当前时间对应的歌词行
  let activeIndex = -1
  for (let i = 0; i < lyricsLines.value.length; i++) {
    if (lyricsLines.value[i].time <= newTime) {
      activeIndex = i
    } else {
      break
    }
  }

  currentLineIndex.value = Math.max(0, activeIndex)

  // 自动滚动到当前行
  nextTick(() => {
    const currentElement = document.querySelector('.lyrics-line.current-line')
    if (currentElement) {
      currentElement.scrollIntoView({
        behavior: 'smooth',
        block: 'center'
      })
    }
  })
})
</script>

<style scoped>
.lyrics-display {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: linear-gradient(135deg,
    rgba(0, 0, 0, 0.9) 0%,
    rgba(0, 0, 0, 0.8) 100%
  );
  border-radius: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

.lyrics-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
}

.lyrics-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
}

.lyrics-icon {
  font-size: 1.25rem;
  color: rgba(168, 85, 247, 0.8);
}

.lyrics-controls {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.lyrics-source-btn {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.5rem;
  border-radius: 0.375rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.lyrics-source-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.9);
}

.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: 0.375rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.3s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.9);
}

.lyrics-content {
  flex: 1;
  padding: 1rem;
  overflow-y: auto;
}

.lyrics-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 0.75rem;
  color: rgba(255, 255, 255, 0.7);
}

.loading-icon {
  font-size: 2rem;
  animation: spin 1s linear infinite;
  color: rgba(168, 85, 247, 0.8);
}

.lyrics-text {
  line-height: 1.8;
  font-size: 0.95rem;
}

.lyrics-line {
  padding: 0.5rem 0;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.3s ease;
  border-radius: 0.375rem;
  padding-left: 0.75rem;
  padding-right: 0.75rem;
}

.lyrics-line.current-line {
  color: rgba(255, 255, 255, 1);
  background: rgba(168, 85, 247, 0.2);
  font-weight: 600;
  transform: scale(1.02);
}

.no-lyrics {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 0.75rem;
  color: rgba(255, 255, 255, 0.5);
}

.no-lyrics-icon {
  font-size: 3rem;
  color: rgba(255, 255, 255, 0.3);
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 滚动条样式 */
.lyrics-content::-webkit-scrollbar {
  width: 6px;
}

.lyrics-content::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

.lyrics-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
}

.lyrics-content::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}
</style>
