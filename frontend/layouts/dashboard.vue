<script setup lang="ts">
  import { Motion } from "motion-v";
</script>
<template>
  <div class="layout-container" style="background-image: url('/background/机甲.png'); background-size: cover; background-position: center; min-height: 100vh;">
    <Sidebar class="sidebar-component" />

    <div class="content-container">
      <div class="content-glass-panel">
        <GlowBorder
            :color="['#A07CFE', '#FE8FB5', '#FFBE7B']"
            :border-radius="10"
        />
        <slot /> <!-- 渲染页面内容 -->
      </div>
    </div>
  </div>
</template>

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
  overflow-y: auto;
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
  padding: 2rem;
  position: relative;
  overflow-y: auto; /* 允许内容溢出时在面板内滚动 */
  flex: 1; /* 填充父容器空间 */

  /* 其他样式保持不变 */

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

/* 液态玻璃风格滚动条 - content-glass-panel */
.content-glass-panel::-webkit-scrollbar {
  width: 6px;
}

.content-glass-panel::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.01);
  border-radius: 8px;
  backdrop-filter: blur(1px);
}

.content-glass-panel::-webkit-scrollbar-thumb {
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

.content-glass-panel::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.2) 0%,
    rgba(255, 255, 255, 0.12) 50%,
    rgba(255, 255, 255, 0.08) 100%
  );
  border: 0.5px solid rgba(255, 255, 255, 0.15);
  box-shadow: 0 1px 4px rgba(255, 255, 255, 0.08);
}

.content-glass-panel::-webkit-scrollbar-thumb:active {
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

.content-glass-panel {
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