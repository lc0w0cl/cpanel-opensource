# 音质优先级匹配功能文档

## 功能概述

在自动匹配功能中，增加了音质优先级判断。如果标题中包含"无损"等高音质关键词的视频，会优先匹配，而不仅仅按播放量选择。

## 需求背景

用户希望在自动匹配时能够优先选择高音质的版本，特别是包含"无损"字样的音频，即使这些版本的播放量可能不是最高的。

## 实现方案

### 1. 音质优先级系统

#### 优先级分级
```javascript
const getAudioQualityPriority = (title: string): number => {
  // 高优先级（100分）：无损音质
  if (title.includes('无损') || titleLower.includes('flac') || titleLower.includes('lossless')) {
    return 100
  }
  
  // 中高优先级（80分）：高品质
  if (title.includes('高品质') || title.includes('高音质') || titleLower.includes('hq') || 
      titleLower.includes('320k') || titleLower.includes('320kbps')) {
    return 80
  }
  
  // 中等优先级（60分）：官方版本
  if (title.includes('官方') || title.includes('正版') || titleLower.includes('official')) {
    return 60
  }
  
  // 中等优先级（50分）：高清视频
  if (title.includes('高清') || titleLower.includes('hd') || titleLower.includes('1080p') || 
      titleLower.includes('4k')) {
    return 50
  }
  
  // 负优先级（-20分）：可能影响音质
  if (title.includes('翻唱') || title.includes('伴奏') || title.includes('卡拉OK') || 
      titleLower.includes('cover') || titleLower.includes('karaoke')) {
    return -20
  }
  
  // 默认优先级（0分）
  return 0
}
```

### 2. 匹配逻辑

#### 优先级匹配算法
```javascript
bestResult = uniqueResults.reduce((best, current) => {
  // 获取音质优先级
  const bestQualityPriority = getAudioQualityPriority(best.title || '')
  const currentQualityPriority = getAudioQualityPriority(current.title || '')
  
  // 如果音质优先级不同，选择优先级更高的
  if (currentQualityPriority !== bestQualityPriority) {
    return currentQualityPriority > bestQualityPriority ? current : best
  }
  
  // 如果音质优先级相同，则按播放量比较
  const bestPlayCount = parsePlayCount(best.playCount || '0')
  const currentPlayCount = parsePlayCount(current.playCount || '0')
  return currentPlayCount > bestPlayCount ? current : best
})
```

## 优先级分类详解

### 1. 高优先级（100分）- 无损音质
**关键词**: `无损`、`FLAC`、`lossless`

**特点**:
- 无损压缩格式
- 音质最佳
- 文件较大但保真度高

**示例标题**:
- "周杰伦 - 青花瓷 无损版"
- "Taylor Swift - Love Story (FLAC)"
- "经典老歌合集 lossless"

### 2. 中高优先级（80分）- 高品质
**关键词**: `高品质`、`高音质`、`HQ`、`320k`、`320kbps`

**特点**:
- 高比特率音频
- 音质优秀
- 文件大小适中

**示例标题**:
- "流行金曲 高品质版本"
- "古典音乐 HQ音质"
- "摇滚经典 320kbps"

### 3. 中等优先级（60分）- 官方版本
**关键词**: `官方`、`正版`、`official`

**特点**:
- 官方发布
- 版权正当
- 质量有保证

**示例标题**:
- "周杰伦 官方MV"
- "正版音乐合集"
- "Official Music Video"

### 4. 中等优先级（50分）- 高清视频
**关键词**: `高清`、`HD`、`1080p`、`4K`

**特点**:
- 视频清晰度高
- 适合MV观看
- 音频质量通常也不错

**示例标题**:
- "经典MV 高清版"
- "音乐视频 1080p"
- "演唱会 4K录制"

### 5. 默认优先级（0分）- 普通版本
**特点**:
- 没有特殊音质标识
- 按播放量排序
- 大部分内容属于此类

### 6. 负优先级（-20分）- 可能影响音质
**关键词**: `翻唱`、`伴奏`、`卡拉OK`、`cover`、`karaoke`

**特点**:
- 非原版演唱
- 可能缺少人声或乐器
- 音质可能不如原版

**示例标题**:
- "青花瓷 翻唱版"
- "经典老歌 伴奏"
- "KTV版本"

## 匹配示例

### 示例1：无损优先
```javascript
// 候选结果
const results = [
  { title: "周杰伦 - 青花瓷", playCount: "100万" },           // 优先级: 0, 播放量: 1000000
  { title: "周杰伦 - 青花瓷 无损版", playCount: "50万" },      // 优先级: 100, 播放量: 500000
  { title: "青花瓷 翻唱版", playCount: "80万" }               // 优先级: -20, 播放量: 800000
]

// 匹配结果: "周杰伦 - 青花瓷 无损版" (优先级最高)
```

### 示例2：相同优先级按播放量
```javascript
// 候选结果
const results = [
  { title: "经典老歌 高品质", playCount: "60万" },            // 优先级: 80, 播放量: 600000
  { title: "流行金曲 320kbps", playCount: "90万" },          // 优先级: 80, 播放量: 900000
  { title: "普通版本", playCount: "120万" }                  // 优先级: 0, 播放量: 1200000
]

// 匹配结果: "流行金曲 320kbps" (相同优先级中播放量最高)
```

### 示例3：官方版本优先
```javascript
// 候选结果
const results = [
  { title: "歌手A - 新歌", playCount: "200万" },             // 优先级: 0, 播放量: 2000000
  { title: "歌手A - 新歌 官方MV", playCount: "150万" },       // 优先级: 60, 播放量: 1500000
  { title: "新歌 翻唱版", playCount: "180万" }               // 优先级: -20, 播放量: 1800000
]

// 匹配结果: "歌手A - 新歌 官方MV" (官方版本优先)
```

## 技术实现

### 1. 关键词检测
```javascript
// 大小写不敏感检测
const titleLower = title.toLowerCase()

// 中文关键词检测
if (title.includes('无损')) { /* 处理逻辑 */ }

// 英文关键词检测
if (titleLower.includes('flac')) { /* 处理逻辑 */ }
```

### 2. 优先级比较
```javascript
// 优先级优先，播放量次之
if (currentQualityPriority !== bestQualityPriority) {
  return currentQualityPriority > bestQualityPriority ? current : best
}

// 相同优先级时比较播放量
const bestPlayCount = parsePlayCount(best.playCount || '0')
const currentPlayCount = parsePlayCount(current.playCount || '0')
return currentPlayCount > bestPlayCount ? current : best
```

### 3. 扩展性设计
```javascript
// 可配置的关键词权重
const qualityKeywords = {
  lossless: { keywords: ['无损', 'flac', 'lossless'], priority: 100 },
  highQuality: { keywords: ['高品质', '高音质', 'hq', '320k'], priority: 80 },
  official: { keywords: ['官方', '正版', 'official'], priority: 60 },
  // ... 更多配置
}
```

## 用户体验提升

### 1. 智能匹配
- **音质优先**: 自动选择最佳音质版本
- **播放量兼顾**: 在相同音质下选择热门版本
- **避免低质**: 自动避开翻唱、伴奏等版本

### 2. 匹配准确性
- **多维度评估**: 不仅看播放量，还考虑音质
- **关键词识别**: 准确识别音质相关标识
- **优先级清晰**: 明确的优先级体系

### 3. 结果可预期
- **无损优先**: 有无损版本时优先选择
- **官方优先**: 有官方版本时优先选择
- **避免误选**: 避免选择翻唱、伴奏版本

## 配置和扩展

### 1. 关键词扩展
```javascript
// 可以添加更多音质关键词
const additionalKeywords = {
  'DTS': 90,        // DTS音频
  'Dolby': 85,      // 杜比音效
  'Studio': 70,     // 录音室版本
  'Remaster': 65,   // 重制版
  'Live': 40        // 现场版本
}
```

### 2. 语言支持
```javascript
// 支持多语言关键词
const multiLanguageKeywords = {
  zh: ['无损', '高品质', '官方'],
  en: ['lossless', 'hq', 'official'],
  ja: ['ロスレス', '高品質', '公式']
}
```

### 3. 用户自定义
```javascript
// 允许用户自定义优先级
const userPreferences = {
  preferLossless: true,     // 优先无损
  preferOfficial: true,     // 优先官方
  avoidCover: true,         // 避免翻唱
  minPlayCount: 1000        // 最低播放量要求
}
```

## 测试用例

### 1. 基本功能测试
- [ ] 无损版本优先于普通版本
- [ ] 高品质版本优先于低品质版本
- [ ] 官方版本优先于非官方版本
- [ ] 相同优先级按播放量排序

### 2. 关键词识别测试
- [ ] 中文关键词正确识别
- [ ] 英文关键词正确识别
- [ ] 大小写不敏感
- [ ] 复合关键词处理

### 3. 边界情况测试
- [ ] 空标题处理
- [ ] 无关键词标题处理
- [ ] 多个关键词同时存在
- [ ] 冲突关键词处理

### 4. 性能测试
- [ ] 大量候选结果的处理速度
- [ ] 关键词检测的效率
- [ ] 内存使用情况

## 总结

通过实现音质优先级匹配功能：

1. **提升匹配质量**: 优先选择高音质版本，提升用户体验
2. **智能化选择**: 不仅考虑播放量，还考虑音质和版本类型
3. **避免低质内容**: 自动避开翻唱、伴奏等可能影响体验的版本
4. **可扩展设计**: 支持添加更多关键词和优先级规则
5. **用户友好**: 自动为用户选择最佳版本，无需手动筛选

这个功能确保了自动匹配能够为用户提供最佳的音质体验，特别是对于追求高音质的用户来说，"无损"版本会被优先选择。
