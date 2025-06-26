# GlowBorder 滚动问题修复说明

## 问题描述

在 `dashboard.vue` 布局中，当 `<slot>` 中的内容超出视口高度需要滚动时，`GlowBorder` 组件的高度没有随着内容变化而调整，导致发光边框效果被截断，造成视觉上的割裂感。

## 问题原因

1. **定位问题**：`GlowBorder` 使用 `absolute inset-0` 定位，只覆盖父容器的可视区域
2. **滚动层级问题**：滚动容器和边框容器在同一层级，导致边框无法跟随内容滚动
3. **高度计算问题**：边框高度基于容器可视高度，而非内容实际高度

## 解决方案

### 1. 重新设计布局结构

```html
<div class="content-container">
  <div class="content-glass-panel">
    <!-- 发光边框容器 - 固定在玻璃面板边界 -->
    <div class="glow-border-container">
      <GlowBorder />
    </div>
    <!-- 内容包装器 - 处理内容滚动 -->
    <div class="content-wrapper">
      <slot />
    </div>
  </div>
</div>
```

### 2. CSS 层级和定位优化

#### 玻璃面板容器
```css
.content-glass-panel {
  position: relative;
  flex: 1;
  overflow: hidden; /* 保持边框完整性 */
  /* 玻璃效果样式 */
}
```

#### 发光边框容器
```css
.glow-border-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 1;
}
```

#### 内容包装器
```css
.content-wrapper {
  position: relative;
  height: 100%;
  padding: 2rem;
  z-index: 2;
  overflow-y: auto; /* 处理内容滚动 */
}
```

### 3. 滚动条样式迁移

将滚动条样式从 `.content-glass-panel` 迁移到 `.content-wrapper`，确保滚动效果应用到正确的容器。

## 技术要点

### 1. 层级分离
- **边框层**：固定在玻璃面板边界，不参与滚动
- **内容层**：独立处理内容滚动，不影响边框

### 2. 定位策略
- **边框**：绝对定位覆盖整个玻璃面板
- **内容**：相对定位，在边框之上

### 3. 溢出处理
- **外层**：`overflow: hidden` 保持边框完整
- **内层**：`overflow-y: auto` 处理内容滚动

## 效果对比

### 修复前
- ❌ 内容滚动时边框被截断
- ❌ 视觉割裂感明显
- ❌ 边框高度不跟随内容

### 修复后
- ✅ 边框始终覆盖完整的玻璃面板
- ✅ 内容滚动不影响边框显示
- ✅ 视觉效果连贯统一

## 兼容性

- ✅ 支持 Webkit 内核浏览器（Chrome、Safari、Edge）
- ✅ 支持 Firefox（通过 scrollbar-width 和 scrollbar-color）
- ✅ 响应式设计兼容
- ✅ 触摸设备滚动支持

## 注意事项

1. **z-index 管理**：确保内容层在边框层之上
2. **滚动性能**：使用 `will-change` 优化动画性能
3. **边框圆角**：保持与玻璃面板圆角一致
4. **指针事件**：边框层设置 `pointer-events: none`

## 未来优化建议

1. **动态边框**：考虑根据内容高度动态调整边框效果
2. **滚动指示器**：添加滚动位置指示器
3. **平滑滚动**：实现更流畅的滚动动画
4. **虚拟滚动**：对于大量内容考虑虚拟滚动优化
