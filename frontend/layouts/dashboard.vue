<script setup lang="ts">
import { Motion } from "motion-v";
import { ref, onMounted, onUnmounted, computed } from 'vue';

// 壁纸相关状态
const customWallpaper = ref('')
const wallpaperBlur = ref(5)

// 计算背景样式
const backgroundStyle = computed(() => {
  return {
    minHeight: '100vh',
    position: 'relative'
  }
})

// 计算背景图片样式（用于伪元素）
const backgroundImageUrl = computed(() => {
  if (customWallpaper.value) {
    return customWallpaper.value
  } else {
    return '/background/机甲.png'
  }
})

// 计算CSS变量
const cssVars = computed(() => {
  return {
    '--bg-image': `url(${backgroundImageUrl.value})`,
    '--bg-blur': `${wallpaperBlur.value}px`
  }
})

// 加载保存的壁纸设置
const loadWallpaperSettings = () => {
  if (process.client) {
    const savedWallpaper = localStorage.getItem('customWallpaper')
    const savedBlur = localStorage.getItem('wallpaperBlur')

    if (savedWallpaper) {
      customWallpaper.value = savedWallpaper
      wallpaperBlur.value = savedBlur ? parseInt(savedBlur) : 5
    }
  }
}

// 监听壁纸变化事件
const handleWallpaperChange = () => {
  loadWallpaperSettings()
}

onMounted(() => {
  loadWallpaperSettings()

  // 监听自定义事件，当壁纸设置改变时更新
  window.addEventListener('wallpaperChanged', handleWallpaperChange)
})

onUnmounted(() => {
  if (process.client) {
    window.removeEventListener('wallpaperChanged', handleWallpaperChange)
  }
})
</script>
<template>
  <div class="layout-container" :style="{ ...backgroundStyle, ...cssVars }">
    <Sidebar class="sidebar-component" />

    <div class="content-container">
      <div class="content-glass-panel">
        <div class="glow-border-container">
          <GlowBorder
              :color="['#A07CFE', '#FE8FB5', '#FFBE7B']"
              :border-radius="10"
          />
        </div>
        <div class="content-wrapper">
          <slot /> <!-- 渲染页面内容 -->
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout-container {
  position: relative;
  overflow: hidden;
}

/* 背景图片伪元素 */
.layout-container::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: var(--bg-image);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  filter: blur(var(--bg-blur));
  z-index: -1;
  /* 扩展背景以避免模糊边缘 */
  transform: scale(1.1);
}

/* 确保内容在背景之上 */
.layout-container > * {
  position: relative;
  z-index: 1;
}
</style>

<style scoped>
.layout-container {
  position: relative;
  display: flex;
  width: 100%;
  min-height: 100vh;
}

.sidebar-component {
  height: 100vh;
  position: sticky;
  top: 0;
}



.content-container {
  flex: 1;
  padding: 1.5rem;
  max-width: 100%;
  max-height: 100vh; /* 限制最大高度为视口高度 */
  display: flex; /* 使子元素能够填充高度 */
}

/* 液态玻璃风格滚动条 - content-container */
.content-container::-webkit-scrollbar {
  width: 8px;
}

.content-container::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.02);
  border-radius: 10px;
  backdrop-filter: blur(2px);
}

.content-container::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.15) 0%,
    rgba(255, 255, 255, 0.08) 50%,
    rgba(255, 255, 255, 0.05) 100%
  );
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(4px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.content-container::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.25) 0%,
    rgba(255, 255, 255, 0.15) 50%,
    rgba(255, 255, 255, 0.1) 100%
  );
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 2px 8px rgba(255, 255, 255, 0.1);
}

.content-container::-webkit-scrollbar-thumb:active {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.3) 0%,
    rgba(255, 255, 255, 0.2) 50%,
    rgba(255, 255, 255, 0.15) 100%
  );
}

.content-glass-panel {
  border-radius: 1.5rem;
  position: relative;
  flex: 1; /* 填充父容器空间 */
  overflow: hidden; /* 隐藏溢出内容，保持边框完整 */

  /* 更透明的液态玻璃效果 */
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.06) 0%,
    rgba(255, 255, 255, 0.03) 50%,
    rgba(255, 255, 255, 0.02) 100%
  );
  backdrop-filter: blur(0px) saturate(120%);
  -webkit-backdrop-filter: blur(1px) saturate(120%);

  /* 更细腻的边框 */
  border: 1px solid rgba(255, 255, 255, 0.08);

  /* 更柔和的阴影 */
  box-shadow:
    0 4px 20px rgba(0, 0, 0, 0.06),
    0 1px 6px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.1),
    inset 0 -1px 0 rgba(0, 0, 0, 0.04);

  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.glow-border-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 1;
}

.content-wrapper {
  position: relative;
  height: 100%; /* 填满父容器高度 */
  padding: 2rem;
  z-index: 2;
  overflow-y: auto; /* 在内容包装器中处理滚动 */
}

/* 液态玻璃风格滚动条 - content-wrapper */
.content-wrapper::-webkit-scrollbar {
  width: 6px;
}

.content-wrapper::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.01);
  border-radius: 8px;
  backdrop-filter: blur(1px);
}

.content-wrapper::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.12) 0%,
    rgba(255, 255, 255, 0.06) 50%,
    rgba(255, 255, 255, 0.03) 100%
  );
  border-radius: 8px;
  border: 0.5px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(3px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.content-wrapper::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.2) 0%,
    rgba(255, 255, 255, 0.12) 50%,
    rgba(255, 255, 255, 0.08) 100%
  );
  border: 0.5px solid rgba(255, 255, 255, 0.15);
  box-shadow: 0 1px 4px rgba(255, 255, 255, 0.08);
}

.content-wrapper::-webkit-scrollbar-thumb:active {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.25) 0%,
    rgba(255, 255, 255, 0.15) 50%,
    rgba(255, 255, 255, 0.1) 100%
  );
}

/* Firefox 滚动条兼容性 */
.content-container {
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.15) rgba(255, 255, 255, 0.02);
}

.content-wrapper {
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.12) rgba(255, 255, 255, 0.01);
}


/* 响应式布局 */
@media (max-width: 768px) {
  .layout-container {
    flex-direction: column;
  }

  .sidebar-component {
    height: auto;
    position: relative;
    width: 100%;
  }
}
</style>