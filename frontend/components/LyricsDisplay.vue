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
          :key="`${index}-${typeof line === 'string' ? line : line.time}`"
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

    <!-- 歌词来源信息和速度调节 -->
    <div v-if="lyricsSource" class="lyrics-source">
      <div class="source-info">
        <span class="source-text">歌词来源: {{ lyricsSource }}</span>
        <span v-if="timeOffset !== 0" class="time-offset">
          ({{ timeOffset > 0 ? '+' : '' }}{{ timeOffset.toFixed(1) }}s)
        </span>
      </div>

      <!-- 速度调节控制 -->
      <div class="speed-controls">
        <span class="control-label">时间调节:</span>
        <button
          @click="adjustTime(-0.5)"
          class="speed-btn speed-btn-slow"
          title="歌词慢0.5秒"
        >
          <Icon icon="mdi:rewind" />
          <span>-0.5s</span>
        </button>
        <button
          @click="resetTimeOffset"
          class="speed-btn speed-btn-reset"
          :class="{ active: timeOffset === 0 }"
          title="重置时间偏移"
        >
          <Icon icon="mdi:restore" />
          <span>重置</span>
        </button>
        <button
          @click="adjustTime(0.5)"
          class="speed-btn speed-btn-fast"
          title="歌词快0.5秒"
        >
          <Icon icon="mdi:fast-forward" />
          <span>+0.5s</span>
        </button>
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
// 时间偏移量（秒）- 只针对本次播放
const timeOffset = ref(0)

// 解析LRC格式歌词
const parseLrcLyrics = (lrcText: string): LyricsLine[] => {
  if (!lrcText) return []

  const lines = lrcText.split('\n')
  const lyricsLines: LyricsLine[] = []

  for (const line of lines) {
    // 跳过空行
    if (!line.trim()) continue

    // 匹配所有时间标签 [mm:ss.xxx] 或 [mm:ss.xx] 或 [mm:ss]
    const timeRegex = /\[(\d{1,2}):(\d{2})(?:\.(\d{2,3}))?\]/g
    const timeMatches = [...line.matchAll(timeRegex)]

    if (timeMatches.length > 0) {
      // 提取歌词文本（移除所有时间标签）
      let text = line.replace(timeRegex, '').trim()

      // 跳过元数据行（作词、作曲、编曲等）
      if (text.includes('作词') || text.includes('作曲') || text.includes('编曲') ||
          text.includes('出品') || text.includes('制作') || text.includes('发行') ||
          text.includes('录音') || text.includes('混音') || text.includes('母带') ||
          text.startsWith('作词：') || text.startsWith('作曲：') || text.startsWith('编曲：')) {
        continue
      }

      // 如果有歌词文本，为每个时间标签创建一个歌词行
      if (text) {
        for (const match of timeMatches) {
          const minutes = parseInt(match[1])
          const seconds = parseInt(match[2])
          const milliseconds = match[3] ? parseInt(match[3]) : 0

          // 根据毫秒位数计算时间
          let msValue = 0
          if (match[3]) {
            msValue = match[3].length === 3 ? milliseconds / 1000 : milliseconds / 100
          }

          const time = minutes * 60 + seconds + msValue

          lyricsLines.push({
            time,
            text,
            originalLine: line
          })
        }
      }
    }
  }

  // 按时间排序并去重（相同时间的歌词只保留一个）
  const sortedLines = lyricsLines.sort((a, b) => a.time - b.time)
  const uniqueLines: LyricsLine[] = []

  for (const line of sortedLines) {
    // 检查是否已存在相同时间和文本的歌词
    const exists = uniqueLines.find(existing =>
      Math.abs(existing.time - line.time) < 0.1 && existing.text === line.text
    )

    if (!exists) {
      uniqueLines.push(line)
    }
  }

  return uniqueLines
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

  // 如果是LRC格式，使用时间匹配
  if (props.lyricsType === 'lrc') {
    let activeIndex = -1

    // 应用时间偏移
    const adjustedTime = newTime + timeOffset.value

    // 找到当前时间对应的歌词行
    for (let i = 0; i < lyricsLines.value.length; i++) {
      const line = lyricsLines.value[i]
      if (typeof line === 'object' && line.time <= adjustedTime) {
        activeIndex = i
      } else {
        break
      }
    }

    // 如果找到了有效的索引，更新当前行
    if (activeIndex >= 0 && activeIndex !== currentLineIndex.value) {
      currentLineIndex.value = activeIndex

      // 滚动到当前行，保持在容器中央
      nextTick(() => {
        scrollToCurrentLine()
      })
    }
  } else {
    // 普通文本格式，简单的时间映射
    const newIndex = Math.min(
      Math.floor(newTime / 5),
      lyricsLines.value.length - 1
    )

    if (newIndex !== currentLineIndex.value && newIndex >= 0) {
      currentLineIndex.value = newIndex

      nextTick(() => {
        scrollToCurrentLine()
      })
    }
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

// 监听歌曲变化，重置时间偏移
watch(() => [props.songTitle, props.songArtist], () => {
  timeOffset.value = 0
  console.log('歌曲切换，时间偏移已重置')
}, { immediate: true })

// 时间调节方法
const adjustTime = (offset: number) => {
  timeOffset.value += offset
  console.log(`歌词时间调节: ${offset > 0 ? '+' : ''}${offset}s, 总偏移: ${timeOffset.value}s`)
}

// 重置时间偏移
const resetTimeOffset = () => {
  timeOffset.value = 0
  console.log('歌词时间偏移已重置')
}

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
  z-index: 900;
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
  bottom: 8rem;
  left: 50%;
  transform: translateX(-50%);
  padding: 1rem 1.5rem;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 1rem;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.7);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  min-width: 300px;
  max-width: 90vw;
}

.source-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  white-space: nowrap;
}

.source-text {
  color: rgba(255, 255, 255, 0.8);
}

.time-offset {
  color: rgba(168, 85, 247, 0.9);
  font-weight: 500;
  font-size: 0.85rem;
}

.speed-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.control-label {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.6);
  margin-right: 0.5rem;
}

.speed-btn {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.4rem 0.8rem;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.5rem;
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
  backdrop-filter: blur(5px);
}

.speed-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.3);
  color: rgba(255, 255, 255, 0.9);
  transform: translateY(-1px);
}

.speed-btn:active {
  transform: translateY(0);
}

.speed-btn-slow:hover {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.4);
  color: rgba(239, 68, 68, 1);
}

.speed-btn-fast:hover {
  background: rgba(34, 197, 94, 0.2);
  border-color: rgba(34, 197, 94, 0.4);
  color: rgba(34, 197, 94, 1);
}

.speed-btn-reset:hover {
  background: rgba(168, 85, 247, 0.2);
  border-color: rgba(168, 85, 247, 0.4);
  color: rgba(168, 85, 247, 1);
}

.speed-btn-reset.active {
  background: rgba(168, 85, 247, 0.3);
  border-color: rgba(168, 85, 247, 0.5);
  color: rgba(168, 85, 247, 1);
}

.speed-btn span {
  font-weight: 500;
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

/* 平板端适配 */
@media (max-width: 768px) and (min-width: 481px) {
  .lyrics-source {
    bottom: 7rem;
    max-width: 88vw;
    font-size: 0.85rem;
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

  .lyrics-source {
    bottom: 6rem;
    font-size: 0.8rem;
    min-width: 280px;
    max-width: 85vw;
    padding: 0.8rem 1rem;
  }

  .speed-controls {
    flex-wrap: wrap;
    gap: 0.4rem;
  }

  .speed-btn {
    padding: 0.3rem 0.6rem;
    font-size: 0.7rem;
  }

  .control-label {
    font-size: 0.75rem;
    margin-right: 0.3rem;
  }
}
</style>
