# 歌单智能下载功能说明

## 功能概述

歌单解析后的歌曲采用智能下载方式，通过B站搜索找到最佳资源进行下载，确保高质量的音频获取。

## 工作原理

### 1. 歌曲识别
- 系统自动识别来自歌单的歌曲（通过描述字段判断）
- 歌单歌曲与普通搜索结果采用不同的下载策略

### 2. 智能搜索策略
当下载歌单歌曲时，系统会使用多个搜索关键词：

1. **`歌手名 + 歌曲名`** - 最精确的匹配
2. **`歌曲名 + 歌手名`** - 调换顺序增加匹配率
3. **`歌曲名`** - 仅歌曲名搜索
4. **`歌曲名 + 音乐`** - 添加"音乐"关键词
5. **`歌手名 + 歌曲名 + 官方`** - 优先官方版本

### 3. 最佳资源选择
- 从所有搜索结果中选择**播放量最高**的视频
- 自动去重，避免重复结果
- 优先选择音质较好的资源

### 4. 下载执行
- 使用选中的B站资源进行实际下载
- 支持服务器下载和本地下载两种模式
- 提供详细的下载进度和状态反馈

## 用户界面

### 歌单信息提示
- 歌单卡片显示下载说明
- 明确告知将通过B站搜索下载
- 说明自动选择播放量最高资源

### 批量下载优化
- **智能批量下载**按钮（当队列中有歌单歌曲时）
- 普通歌曲并行下载
- 歌单歌曲串行下载（避免请求过频）
- 每首歌间隔1秒，保护服务器

### 下载反馈
- 显示匹配到的B站资源信息
- 成功提示包含原歌曲名和B站资源名
- 详细的错误信息和失败原因

## 技术实现

### 前端逻辑

```typescript
// 检查是否为歌单歌曲
const isPlaylistSong = (item: MusicSearchResult) => {
  return item.description && item.description.includes('来自歌单:')
}

// 智能搜索下载
const downloadPlaylistSong = async (item: MusicSearchResult) => {
  // 多关键词搜索策略
  const searchKeywords = [
    `${item.artist} ${item.title}`,
    `${item.title} ${item.artist}`,
    item.title,
    `${item.title} 音乐`,
    `${item.artist} ${item.title} 官方`
  ]
  
  // 选择播放量最高的结果
  const bestResult = results.reduce((best, current) => {
    const bestPlayCount = parseInt(best.playCount?.replace(/[^\d]/g, '') || '0')
    const currentPlayCount = parseInt(current.playCount?.replace(/[^\d]/g, '') || '0')
    return currentPlayCount > bestPlayCount ? current : best
  })
}
```

### 批量下载策略

```typescript
const startBatchDownload = async () => {
  const playlistSongs = downloadQueue.value.filter(item => isPlaylistSong(item))
  const regularSongs = downloadQueue.value.filter(item => !isPlaylistSong(item))
  
  // 普通歌曲并行下载
  for (const song of regularSongs) {
    startDownload(song) // 不等待
  }
  
  // 歌单歌曲串行下载
  for (const song of playlistSongs) {
    await startDownload(song) // 等待完成
    await new Promise(resolve => setTimeout(resolve, 1000)) // 间隔1秒
  }
}
```

## 优势特点

### 🎯 高匹配率
- 多关键词搜索策略
- 智能关键词组合
- 自动容错处理

### 🏆 高质量资源
- 优先选择播放量高的资源
- 通常意味着更好的音质
- 更稳定的下载链接

### ⚡ 智能优化
- 区分歌单歌曲和普通搜索
- 合理的下载频率控制
- 详细的进度反馈

### 🛡️ 错误处理
- 多重搜索策略降低失败率
- 详细的错误信息提示
- 优雅的降级处理

## 使用建议

### 最佳实践
1. **歌单质量**：选择知名歌单，歌曲信息更准确
2. **网络环境**：确保网络稳定，避免搜索中断
3. **批量下载**：使用智能批量下载，系统会自动优化

### 注意事项
1. **下载时间**：歌单歌曲下载时间较长（需要搜索）
2. **匹配准确性**：部分冷门歌曲可能匹配不准确
3. **版权限制**：某些歌曲可能在B站无资源

### 故障排除
- **搜索失败**：检查歌曲名和歌手名是否正确
- **下载失败**：可能是B站资源问题，可手动搜索
- **匹配错误**：系统选择了错误的资源，可单独处理

## 未来改进

- [ ] 添加手动选择搜索结果功能
- [ ] 支持更多音乐平台搜索
- [ ] 智能歌曲名清理和标准化
- [ ] 下载历史和缓存机制
- [ ] 用户自定义搜索策略
