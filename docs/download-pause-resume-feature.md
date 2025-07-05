# 下载暂停/恢复功能说明

## 功能概述

在智能批量下载右侧添加了全部暂停功能，用户可以随时暂停正在进行的下载任务，并在需要时恢复下载。

## 功能特性

### 🛑 全部暂停
- **触发条件**：正在下载时显示"全部暂停"按钮
- **功能**：立即停止所有正在进行的下载任务
- **状态保持**：保留下载进度，不清除队列
- **视觉反馈**：黄色按钮，暂停图标

### ▶️ 恢复下载
- **触发条件**：暂停状态下显示"恢复下载"按钮
- **功能**：继续未完成的下载任务
- **智能恢复**：自动识别未完成的歌曲
- **视觉反馈**：绿色按钮，播放图标

### 🎯 智能状态管理
- **状态检测**：自动检测下载状态和暂停状态
- **按钮切换**：根据状态动态显示对应按钮
- **进度保持**：暂停时保留已有下载进度

## 用户界面

### 按钮状态切换
```
未下载状态：
[▶️ 智能批量下载] [🗑️ 清空队列]

下载中状态：
[⏸️ 全部暂停] [🗑️ 清空队列(禁用)]

暂停状态：
[▶️ 恢复下载] [🗑️ 清空队列]
```

### 视觉设计
- **智能批量下载**：默认样式，白色边框
- **全部暂停**：黄色主题，警告色调
- **恢复下载**：绿色主题，成功色调
- **清空队列**：下载中时禁用，暂停时可用

## 技术实现

### 状态管理
```typescript
// 下载相关状态
const isDownloading = ref(false)      // 是否正在下载
const isPaused = ref(false)           // 是否已暂停
const downloadControllers = ref(new Map()) // 下载控制器
```

### 暂停机制
```typescript
const pauseAllDownloads = () => {
  isPaused.value = true
  isDownloading.value = false
  
  // 中止所有正在进行的下载
  downloadControllers.value.forEach((controller, songId) => {
    if (controller && !controller.signal.aborted) {
      controller.abort()
    }
  })
  
  showNotification('已暂停所有下载任务', 'info')
}
```

### 恢复机制
```typescript
const resumeAllDownloads = () => {
  isPaused.value = false
  
  // 找到未完成的下载任务
  const unfinishedSongs = downloadQueue.value.filter(item => {
    const progress = downloadProgress.value[item.id]
    return progress === undefined || (progress < 100 && progress > 0)
  })
  
  if (unfinishedSongs.length > 0) {
    startBatchDownload() // 重新开始下载
  }
}
```

### 下载控制
```typescript
const startDownload = async (item: MusicSearchResult) => {
  // 检查暂停状态
  if (isPaused.value) {
    return false
  }
  
  // 创建可中止的控制器
  const controller = new AbortController()
  downloadControllers.value.set(item.id, controller)
  
  // 在下载过程中检查暂停状态
  if (!isPaused.value && !controller.signal.aborted) {
    // 继续下载
  }
}
```

## 使用场景

### 1. 网络限制
- **场景**：网络带宽有限，需要暂停下载
- **操作**：点击"全部暂停"，释放网络资源
- **恢复**：网络空闲时点击"恢复下载"

### 2. 系统资源管理
- **场景**：系统资源紧张，需要暂停下载
- **操作**：暂停下载，释放CPU和内存
- **恢复**：资源充足时恢复下载

### 3. 优先级调整
- **场景**：需要优先下载其他内容
- **操作**：暂停当前下载，处理优先任务
- **恢复**：完成优先任务后恢复下载

### 4. 错误处理
- **场景**：发现下载错误，需要检查设置
- **操作**：暂停下载，检查配置
- **恢复**：修复问题后恢复下载

## 状态流转

```
初始状态 → 开始下载 → 下载中 → 暂停 → 恢复 → 完成
   ↓           ↓         ↓       ↓      ↓      ↓
[开始]    [暂停按钮]  [暂停中] [恢复按钮] [下载中] [完成]
```

### 状态说明
1. **初始状态**：显示"智能批量下载"按钮
2. **下载中**：显示"全部暂停"按钮，禁用清空队列
3. **暂停状态**：显示"恢复下载"按钮，启用清空队列
4. **恢复下载**：重新进入下载状态
5. **下载完成**：回到初始状态

## 通知系统

### 暂停通知
- **消息**：`"已暂停所有下载任务"`
- **类型**：信息通知（蓝色）
- **时机**：点击暂停按钮时

### 恢复通知
- **消息**：`"恢复下载 X 首歌曲"`
- **类型**：成功通知（绿色）
- **时机**：点击恢复按钮且有未完成任务时

### 无任务通知
- **消息**：`"没有需要恢复的下载任务"`
- **类型**：信息通知（蓝色）
- **时机**：点击恢复按钮但无未完成任务时

## 错误处理

### AbortError处理
```typescript
catch (error: any) {
  // 如果是暂停导致的错误，不显示错误提示
  if (error.name === 'AbortError' || isPaused.value) {
    console.log('下载被暂停:', item.title)
    return false
  }
  
  // 其他错误正常处理
  showNotification(`下载异常: ${item.title}`, 'error')
}
```

### 状态一致性
- 暂停时清理所有下载控制器
- 恢复时重新创建控制器
- 确保UI状态与实际状态一致

## 最佳实践

### 用户操作建议
1. **大批量下载**：建议分批下载，便于管理
2. **网络不稳定**：及时暂停，避免重复下载
3. **系统繁忙**：暂停下载，优先处理其他任务
4. **错误排查**：暂停下载，检查日志和设置

### 开发注意事项
1. **状态同步**：确保UI状态与下载状态同步
2. **资源清理**：暂停时及时清理控制器
3. **错误区分**：区分暂停和真实错误
4. **用户反馈**：提供清晰的状态提示

## 未来改进

- [ ] 单个歌曲暂停/恢复功能
- [ ] 下载优先级调整
- [ ] 暂停时间统计
- [ ] 自动暂停条件设置
- [ ] 下载队列重排序
- [ ] 暂停原因记录
