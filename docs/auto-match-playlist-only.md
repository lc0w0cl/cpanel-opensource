# 自动匹配功能限制为歌单模式

## 修改说明

根据用户反馈，自动匹配功能应该只在歌单解析模式下显示，而不应该在普通的关键词搜索或链接下载模式下出现。

## 问题分析

**修改前的行为**：
- 在任何搜索模式下，只要有选中的搜索结果，就会显示"自动匹配"按钮
- 这导致用户在关键词搜索或链接下载时也能看到自动匹配功能
- 但自动匹配功能的设计初衷是为歌单歌曲在B站搜索最佳资源

**修改后的行为**：
- 自动匹配功能只在歌单解析模式下显示
- 关键词搜索和链接下载模式下不显示自动匹配按钮
- 从歌单模式切换到其他模式时，自动清理相关状态

## 具体修改

### 1. 修改自动匹配按钮显示条件

**文件**: `frontend/pages/home/music.vue`

**修改前**:
```vue
<button
  v-if="hasSelectedItems && !isAutoMatching"
  @click="autoMatchMusic"
  class="auto-match-btn"
>
```

**修改后**:
```vue
<button
  v-if="hasSelectedItems && !isAutoMatching && playlistInfo"
  @click="autoMatchMusic"
  class="auto-match-btn"
>
```

### 2. 修改取消匹配按钮显示条件

**修改前**:
```vue
<button
  v-if="isAutoMatching"
  @click="cancelAutoMatch"
  class="cancel-match-btn"
>
```

**修改后**:
```vue
<button
  v-if="isAutoMatching && playlistInfo"
  @click="cancelAutoMatch"
  class="cancel-match-btn"
>
```

### 3. 修改匹配进度显示条件

**修改前**:
```vue
<div v-if="matchingProgress[result.id] !== undefined" class="matching-overlay">
```

**修改后**:
```vue
<div v-if="matchingProgress[result.id] !== undefined && playlistInfo" class="matching-overlay">
```

### 4. 修改自动匹配错误提示显示条件

**修改前**:
```vue
<div v-if="matchingError" class="search-error">
```

**修改后**:
```vue
<div v-if="matchingError && playlistInfo" class="search-error">
```

### 5. 添加搜索类型切换时的状态清理

新增 `handleSearchTypeChange` 函数：

```typescript
// 自定义搜索类型切换函数
const handleSearchTypeChange = (type: 'keyword' | 'url' | 'playlist') => {
  // 如果从歌单模式切换到其他模式，清理自动匹配相关状态
  if (searchType.value === 'playlist' && type !== 'playlist') {
    isAutoMatching.value = false
    matchingProgress.value = {}
    matchingError.value = ''
    playlistInfo.value = null
    playlistError.value = ''
  }
  
  // 设置新的搜索类型
  setSearchType(type)
}
```

更新模板中的按钮点击事件：

```vue
<button @click="handleSearchTypeChange('keyword')">关键词搜索</button>
<button @click="handleSearchTypeChange('url')">链接下载</button>
<button @click="handleSearchTypeChange('playlist')">歌单解析</button>
```

## 判断条件说明

使用 `playlistInfo` 作为判断条件的原因：

1. **准确性**: `playlistInfo` 只有在成功解析歌单后才会有值
2. **状态一致性**: 当用户切换到其他模式时，`playlistInfo` 会被清空
3. **用户体验**: 只有在真正有歌单数据时才显示自动匹配功能

## 测试场景

### 场景1: 关键词搜索模式
1. 选择"关键词搜索"
2. 搜索"周杰伦"
3. 选中一些搜索结果
4. **期望**: 不显示"自动匹配"按钮

### 场景2: 链接下载模式
1. 选择"链接下载"
2. 输入B站视频链接
3. 获取视频信息后选中
4. **期望**: 不显示"自动匹配"按钮

### 场景3: 歌单解析模式
1. 选择"歌单解析"
2. 输入QQ音乐或网易云歌单链接
3. 成功解析歌单后选中一些歌曲
4. **期望**: 显示"自动匹配"按钮

### 场景4: 模式切换状态清理
1. 在歌单模式下开始自动匹配
2. 切换到关键词搜索模式
3. **期望**: 自动匹配状态被清理，不显示匹配进度

## 功能逻辑

### 自动匹配的作用
- **目的**: 为歌单中的歌曲在B站搜索最佳匹配资源
- **场景**: 用户解析了QQ音乐或网易云歌单，但这些歌曲无法直接下载
- **解决方案**: 通过B站搜索相同歌曲名和歌手的视频作为替代资源

### 为什么只在歌单模式下需要
1. **关键词搜索**: 用户直接搜索想要的内容，结果已经是可下载的
2. **链接下载**: 用户提供了具体的视频链接，无需再次匹配
3. **歌单解析**: 歌单中的歌曲可能无法直接下载，需要在B站找替代资源

## 向后兼容性

- 现有的自动匹配功能逻辑保持不变
- 只是限制了显示条件，不影响功能实现
- 歌单解析功能完全不受影响
