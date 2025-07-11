<template>
  <div class="lyrics-fullscreen">
    <!-- 关闭按钮 -->
    <button @click="$emit('close')" class="close-btn">
      <Icon icon="mdi:close" />
    </button>

    <!-- 歌曲信息 -->
    <div class="song-info">
      <h2 class="song-title">{{ songTitle }}</h2>
      <p class="song-artist">{{ songArtist }}</p>
    </div>

    <!-- 歌词容器 -->
    <div class="lyrics-container" ref="lyricsContainer">
      <!-- 加载状态 -->
      <div v-if="isLoading" class="lyrics-loading">
        <Icon icon="mdi:loading" class="loading-icon" />
        <span>正在获取歌词...</span>
      </div>

      <!-- 歌词文本 -->
      <div v-else-if="lyrics && lyricsLines.length > 0" class="lyrics-scroll">
        <!-- 上方占位，确保第一行歌词可以滚动到中央 -->
        <div class="lyrics-spacer-top"></div>

        <div
          v-for="(line, index) in lyricsLines"
          :key="index"
          class="lyrics-line"
          :class="{
            'current-line': index === currentLineIndex,
            'prev-line': index === currentLineIndex - 1,
            'next-line': index === currentLineIndex + 1,
            'far-line': Math.abs(index - currentLineIndex) > 1
          }"
        >
          {{ typeof line === 'string' ? line : line.text }}
        </div>

        <!-- 下方占位，确保最后一行歌词可以滚动到中央 -->
        <div class="lyrics-spacer-bottom"></div>
      </div>

      <!-- 无歌词状态 -->
      <div v-else class="no-lyrics">
        <Icon icon="mdi:music-note-off" class="no-lyrics-icon" />
        <span>暂无歌词</span>
        <p class="no-lyrics-tip">{{ lyricsSource ? `来源: ${lyricsSource}` : '尝试播放其他歌曲' }}</p>
      </div>
    </div>

    <!-- 歌词来源信息 -->
    <div v-if="lyricsSource" class="lyrics-source">
      歌词来源: {{ lyricsSource }}
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
  songTitle: string
  songArtist: string
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
// 歌词容器引用
const lyricsContainer = ref<HTMLElement>()

// 解析LRC格式歌词
const parseLrcLyrics = (lrcText: string): LyricsLine[] => {
  if (!lrcText) return []

  const lines = lrcText.split('\n')
  const lyricsLines: LyricsLine[] = []

  for (const line of lines) {
    // 匹配时间标签 [mm:ss.xxx] 或 [mm:ss.xx]
    const timeMatch = line.match(/\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)/)
    if (timeMatch) {
      const minutes = parseInt(timeMatch[1])
      const seconds = parseInt(timeMatch[2])
      const milliseconds = parseInt(timeMatch[3])
      const text = timeMatch[4].trim()

      if (text) {
        // 根据毫秒位数计算时间
        const msValue = timeMatch[3].length === 3 ? milliseconds / 1000 : milliseconds / 100
        const time = minutes * 60 + seconds + msValue
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

  const newIndex = Math.max(0, activeIndex)
  if (newIndex !== currentLineIndex.value) {
    currentLineIndex.value = newIndex

    // 滚动到当前行，保持在容器中央
    nextTick(() => {
      scrollToCurrentLine()
    })
  }
})

// 监听歌词数据变化，初始化滚动位置
watch(() => [props.lyrics, lyricsLines.value.length], () => {
  if (lyricsLines.value.length > 0) {
    nextTick(() => {
      // 延迟一点时间确保DOM完全渲染
      setTimeout(() => {
        scrollToCurrentLine()
      }, 100)
    })
  }
}, { immediate: true })

// 滚动到当前行的函数
const scrollToCurrentLine = () => {
  if (!lyricsContainer.value) return

  const container = lyricsContainer.value
  const scrollContainer = container.querySelector('.lyrics-scroll') as HTMLElement

  if (!scrollContainer) return

  // 找到当前行元素
  const currentLineElement = scrollContainer.querySelector('.lyrics-line.current-line') as HTMLElement

  if (!currentLineElement) return

  // 计算滚动位置，让当前行在容器中央
  const containerHeight = scrollContainer.clientHeight
  const lineOffsetTop = currentLineElement.offsetTop
  const lineHeight = currentLineElement.clientHeight

  // 计算目标滚动位置：当前行的中心 - 容器高度的一半
  const targetScrollTop = lineOffsetTop + (lineHeight / 2) - (containerHeight / 2)

  scrollContainer.scrollTo({
    top: Math.max(0, targetScrollTop),
    behavior: 'smooth'
  })
}
</script>

<style scoped>
.lyrics-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg,
    rgba(0, 0, 0, 0.95) 0%,
    rgba(20, 20, 30, 0.95) 50%,
    rgba(0, 0, 0, 0.95) 100%
  );
  backdrop-filter: blur(20px);
  z-index: 2000;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.close-btn {
  position: absolute;
  top: 2rem;
  right: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 3rem;
  height: 3rem;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.3);
  background: rgba(0, 0, 0, 0.5);
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 1.5rem;
  z-index: 10;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 1);
  border-color: rgba(255, 255, 255, 0.5);
  transform: scale(1.1);
}

.song-info {
  text-align: center;
  padding: 3rem 2rem 2rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.song-title {
  font-size: 2rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.95);
  margin: 0 0 0.5rem 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.song-artist {
  font-size: 1.2rem;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
  font-weight: 400;
}

.lyrics-container {
  flex: 1;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.lyrics-scroll {
  width: 100%;
  max-height: 100%;
  overflow-y: auto;
  padding: 0 2rem;
  scroll-behavior: smooth;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.lyrics-spacer-top {
  height: calc(50vh - 100px);
  flex-shrink: 0;
}

.lyrics-spacer-bottom {
  height: calc(50vh - 100px);
  flex-shrink: 0;
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

.lyrics-line {
  text-align: center;
  padding: 1.5rem 2rem;
  font-size: 1.6rem;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.4);
  transition: all 0.8s cubic-bezier(0.4, 0, 0.2, 1);
  font-weight: 400;
  margin: 0.8rem 0;
  width: 100%;
  max-width: 800px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80px;
}

.lyrics-line.prev-line,
.lyrics-line.next-line {
  color: rgba(255, 255, 255, 0.6);
  font-size: 1.3rem;
  opacity: 0.8;
}

.lyrics-line.far-line {
  color: rgba(255, 255, 255, 0.3);
  font-size: 1.1rem;
  opacity: 0.5;
}

.lyrics-line.current-line {
  color: rgba(255, 255, 255, 1);
  font-size: 2.4rem;
  font-weight: 700;
  text-shadow: 0 0 30px rgba(168, 85, 247, 0.6);
  padding: 1.5rem 2rem;
  margin: 1.5rem 0;
  transform: scale(1.1);
  animation: currentLinePulse 4s ease-in-out infinite;
  position: relative;
  z-index: 10;
}

@keyframes currentLinePulse {
  0%, 100% {
    text-shadow: 0 0 20px rgba(168, 85, 247, 0.5);
  }
  50% {
    text-shadow: 0 0 30px rgba(168, 85, 247, 0.8);
  }
}

.lyrics-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 1.5rem;
  color: rgba(255, 255, 255, 0.8);
  font-size: 1.2rem;
}

.loading-icon {
  font-size: 3rem;
  animation: spin 1s linear infinite;
  color: rgba(168, 85, 247, 0.8);
}

.no-lyrics {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 1rem;
  color: rgba(255, 255, 255, 0.6);
  text-align: center;
}

.no-lyrics-icon {
  font-size: 4rem;
  color: rgba(255, 255, 255, 0.3);
  margin-bottom: 1rem;
}

.no-lyrics-tip {
  font-size: 1rem;
  color: rgba(255, 255, 255, 0.4);
  margin: 0.5rem 0 0 0;
}

.lyrics-source {
  position: absolute;
  bottom: 2rem;
  left: 50%;
  transform: translateX(-50%);
  padding: 0.5rem 1rem;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 1rem;
  color: rgba(255, 255, 255, 0.6);
  font-size: 0.9rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
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
.lyrics-scroll::-webkit-scrollbar {
  width: 6px;
}

.lyrics-scroll::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

.lyrics-scroll::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
}

.lyrics-scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.5);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .close-btn {
    top: 1rem;
    right: 1rem;
    width: 2.5rem;
    height: 2.5rem;
    font-size: 1.2rem;
  }

  .song-info {
    padding: 2rem 1rem 1.5rem;
  }

  .song-title {
    font-size: 1.5rem;
  }

  .song-artist {
    font-size: 1rem;
  }

  .lyrics-scroll {
    padding: 0 1rem;
  }

  .lyrics-spacer-top,
  .lyrics-spacer-bottom {
    height: calc(40vh - 80px);
  }

  .lyrics-line {
    font-size: 1.3rem;
    padding: 1rem 1.5rem;
    min-height: 60px;
  }

  .lyrics-line.current-line {
    font-size: 1.8rem;
    padding: 1.2rem 1.5rem;
  }

  .lyrics-line.prev-line,
  .lyrics-line.next-line {
    font-size: 1.1rem;
  }

  .lyrics-line.far-line {
    font-size: 1rem;
  }
}

@media (max-width: 480px) {
  .lyrics-spacer-top,
  .lyrics-spacer-bottom {
    height: calc(35vh - 60px);
  }

  .lyrics-line {
    font-size: 1.1rem;
    padding: 0.8rem 1rem;
    min-height: 50px;
  }

  .lyrics-line.current-line {
    font-size: 1.5rem;
    padding: 1rem 1.2rem;
  }

  .lyrics-line.prev-line,
  .lyrics-line.next-line {
    font-size: 0.95rem;
  }

  .lyrics-line.far-line {
    font-size: 0.85rem;
  }
}
</style>
