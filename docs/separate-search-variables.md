# 独立搜索变量实现文档

## 功能概述

将关键词搜索、链接下载、歌单解析三种搜索方式的搜索内容使用独立的变量保存，用户在不同搜索方式之间切换时可以保持各自的搜索内容。

## 问题分析

### 原有实现问题
- 所有搜索类型共用一个`searchQuery`变量
- 用户切换搜索类型时，之前输入的内容会丢失
- 用户体验不佳，需要重新输入内容

### 用户场景
1. 用户在关键词搜索中输入"周杰伦"
2. 切换到链接下载，输入B站链接
3. 再切换回关键词搜索时，"周杰伦"内容丢失
4. 用户需要重新输入，体验不佳

## 解决方案

### 1. 独立变量设计

#### 变量定义
```javascript
// 独立的搜索内容变量
const keywordSearchQuery = ref('')  // 关键词搜索内容
const urlSearchQuery = ref('')      // 链接下载内容
const playlistSearchQuery = ref('') // 歌单解析内容
```

#### 计算属性
```javascript
// 获取当前搜索内容
const currentSearchQuery = computed(() => {
  switch (searchType.value) {
    case 'keyword':
      return keywordSearchQuery.value
    case 'url':
      return urlSearchQuery.value
    case 'playlist':
      return playlistSearchQuery.value
    default:
      return ''
  }
})
```

### 2. 更新函数

#### 搜索内容更新
```javascript
// 更新当前搜索内容
const updateCurrentSearchQuery = (value: string) => {
  switch (searchType.value) {
    case 'keyword':
      keywordSearchQuery.value = value
      break
    case 'url':
      urlSearchQuery.value = value
      break
    case 'playlist':
      playlistSearchQuery.value = value
      break
  }
}
```

### 3. 模板更新

#### 输入框绑定
```vue
<input
  :value="currentSearchQuery"
  @input="updateCurrentSearchQuery($event.target.value)"
  @keyup.enter="handleSearch"
  type="text"
  :placeholder="getSearchPlaceholder()"
  class="search-input"
  :class="{ invalid: !isValidUrl }"
/>
```

#### 条件判断更新
```vue
<!-- 搜索按钮禁用条件 -->
:disabled="!currentSearchQuery.trim() || !isValidUrl || isSearching || isParsingPlaylist"

<!-- URL验证提示 -->
<div v-if="currentSearchQuery && !isValidUrl" class="url-hint">
```

## 技术实现

### 1. 状态管理

#### 原有方式
```javascript
// 全局状态，所有搜索类型共用
const { searchQuery, setSearchQuery } = useMusicState()
```

#### 新的方式
```javascript
// 本地状态，每种搜索类型独立
const keywordSearchQuery = ref('')
const urlSearchQuery = ref('')
const playlistSearchQuery = ref('')

// 通过计算属性获取当前内容
const currentSearchQuery = computed(() => {
  // 根据searchType返回对应的搜索内容
})
```

### 2. 数据流

#### 输入流程
1. 用户在输入框中输入内容
2. 触发`updateCurrentSearchQuery`函数
3. 根据当前`searchType`更新对应的变量
4. 计算属性`currentSearchQuery`自动更新

#### 切换流程
1. 用户点击搜索类型标签
2. `searchType`发生变化
3. 计算属性`currentSearchQuery`返回新类型的搜索内容
4. 输入框显示对应的内容

### 3. 验证逻辑

#### URL验证更新
```javascript
const isValidUrl = computed(() => {
  if (searchType.value === 'keyword') return true
  const url = currentSearchQuery.value.trim() // 使用当前搜索内容

  if (searchType.value === 'url') {
    return url.includes('bilibili.com') || url.includes('youtube.com') || url.includes('youtu.be')
  }

  if (searchType.value === 'playlist') {
    return url.includes('y.qq.com') || url.includes('music.163.com')
  }

  return true
})
```

## 用户体验提升

### 1. 内容保持
- **关键词搜索**: 保存用户输入的歌曲名、歌手名等
- **链接下载**: 保存用户粘贴的视频链接
- **歌单解析**: 保存用户输入的歌单链接

### 2. 无缝切换
- 用户可以在不同搜索类型间自由切换
- 每种类型的搜索内容都会被保留
- 避免重复输入，提升效率

### 3. 智能验证
- 每种搜索类型有独立的验证逻辑
- 实时显示对应的提示信息
- 防止用户输入错误的内容格式

## 使用场景示例

### 场景1：多任务搜索
```
1. 用户在关键词搜索输入"周杰伦 - 青花瓷"
2. 切换到链接下载，粘贴B站链接
3. 切换到歌单解析，输入QQ音乐歌单链接
4. 再切换回关键词搜索，"周杰伦 - 青花瓷"仍然存在
```

### 场景2：对比搜索
```
1. 用户在关键词搜索找到一首歌
2. 切换到链接下载，直接下载高质量版本
3. 切换回关键词搜索，继续寻找其他歌曲
4. 所有搜索内容都被保留，方便对比
```

### 场景3：批量操作
```
1. 用户在歌单解析中解析一个歌单
2. 切换到关键词搜索，手动搜索缺失的歌曲
3. 切换到链接下载，下载特定的高质量版本
4. 各种搜索内容独立保存，操作更灵活
```

## 兼容性考虑

### 1. 向后兼容
- 保持原有的搜索功能不变
- 不影响现有的搜索逻辑
- 平滑升级，无需数据迁移

### 2. 状态同步
- 与全局状态管理保持兼容
- 搜索类型切换逻辑不变
- 搜索结果处理逻辑不变

### 3. 性能影响
- 增加的内存开销很小（3个字符串变量）
- 计算属性有缓存机制，性能良好
- 不影响搜索和下载的性能

## 测试验证

### 1. 功能测试
- [ ] 关键词搜索内容独立保存
- [ ] 链接下载内容独立保存
- [ ] 歌单解析内容独立保存
- [ ] 搜索类型切换时内容保持

### 2. 交互测试
- [ ] 输入框内容正确显示
- [ ] 搜索按钮状态正确
- [ ] URL验证提示正确
- [ ] 占位符文本正确

### 3. 边界测试
- [ ] 空内容处理
- [ ] 特殊字符处理
- [ ] 长文本处理
- [ ] 快速切换处理

## 总结

通过实现独立的搜索变量：

1. **提升用户体验**: 用户可以在不同搜索类型间无缝切换
2. **提高操作效率**: 避免重复输入，节省时间
3. **增强功能灵活性**: 支持多任务并行操作
4. **保持系统稳定性**: 不影响现有功能，平滑升级

这个改进让音乐搜索功能更加人性化和高效，显著提升了用户的使用体验。
