# 音乐页面显示增强

## 更新内容

在音乐搜索页面中添加了播放量和发布时间的显示，提供更丰富的视频信息。

## 前端改进

### 1. 搜索结果显示

在搜索结果的元数据区域添加了两个新字段：

```vue
<div class="result-meta">
  <span class="duration">
    <Icon icon="mdi:clock-outline" />
    {{ result.duration }}
  </span>
  <span class="quality">
    <Icon icon="mdi:high-definition" />
    {{ result.quality }}
  </span>
  <!-- 新增：播放量 -->
  <span v-if="result.playCount" class="play-count">
    <Icon icon="mdi:play" />
    {{ result.playCount }}
  </span>
  <!-- 新增：发布时间 -->
  <span v-if="result.publishTime" class="publish-time">
    <Icon icon="mdi:calendar" />
    {{ result.publishTime }}
  </span>
</div>
```

### 2. 下载队列显示

在下载队列中也添加了相同的元数据信息：

```vue
<div class="download-meta">
  <span class="duration">
    <Icon icon="mdi:clock-outline" />
    {{ item.duration }}
  </span>
  <span v-if="item.playCount" class="play-count">
    <Icon icon="mdi:play" />
    {{ item.playCount }}
  </span>
  <span v-if="item.publishTime" class="publish-time">
    <Icon icon="mdi:calendar" />
    {{ item.publishTime }}
  </span>
</div>
```

### 3. 样式优化

#### 响应式布局
```css
.result-meta {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  flex-wrap: wrap; /* 允许换行 */
}
```

#### 差异化颜色
```css
/* 播放量 - 稍微突出 */
.play-count {
  color: rgba(255, 255, 255, 0.6);
}

/* 发布时间 - 相对次要 */
.publish-time {
  color: rgba(255, 255, 255, 0.4);
}
```

## 后端数据支持

### 1. 播放量解析

从哔哩哔哩的HTML结构中提取播放量：

```java
// 提取播放量 - 根据新结构，从stats区域获取第一个数字
Elements statsItems = item.select(".bili-video-card__stats--item span:last-child");
String playCount = "0";
if (!statsItems.isEmpty()) {
    playCount = statsItems.first().text().trim();
}
```

### 2. 发布时间解析

从HTML中提取发布时间并清理格式：

```java
// 提取发布时间 - 根据新结构
Element timeElement = item.selectFirst(".bili-video-card__info--date");
String publishTime = timeElement != null ? 
    timeElement.text().trim().replace("·", "").trim() : "";
```

### 3. 数据传输

通过 `MusicSearchResultDTO` 传输完整数据：

```java
return MusicSearchResultDTO.builder()
    .id(videoId)
    .title(title)
    .artist(artist)
    .duration(duration)
    .platform("bilibili")
    .thumbnail(thumbnail)
    .url(url)
    .quality("HD")
    .playCount(playCount)      // 播放量
    .publishTime(publishTime)  // 发布时间
    .build();
```

## 显示效果

### 1. 信息层次

- **标题**: 最突出，白色文字
- **UP主**: 次要信息，半透明白色
- **时长**: 基础信息，较暗白色
- **画质**: 基础信息，较暗白色
- **播放量**: 重要信息，中等亮度白色
- **发布时间**: 次要信息，最暗白色

### 2. 图标使用

- 🕐 时长：`mdi:clock-outline`
- 📺 画质：`mdi:high-definition`
- ▶️ 播放量：`mdi:play`
- 📅 发布时间：`mdi:calendar`

### 3. 布局特点

- 使用 `flex-wrap` 允许元素换行
- 减小间距避免拥挤
- 条件渲染避免显示空数据

## 测试方法

### 1. 可视化测试

打开 `test-music-display.html` 查看样式效果。

### 2. 功能测试

1. 启动后端服务
2. 在音乐页面搜索"稻香"
3. 检查搜索结果是否显示播放量和发布时间
4. 将结果添加到下载队列
5. 检查下载队列是否也显示相关信息

### 3. API测试

```bash
curl -X POST http://localhost:8080/api/music/search \
  -H "Content-Type: application/json" \
  -d '{"query":"稻香","searchType":"keyword","platform":"bilibili"}'
```

检查返回的JSON中是否包含 `playCount` 和 `publishTime` 字段。

## 数据示例

### 典型的搜索结果数据：

```json
{
  "id": "BV1aiT7zTEWD",
  "title": "《稻香》周杰伦 完整版无损音质！",
  "artist": "芒果味杨枝甘露",
  "duration": "03:46",
  "platform": "bilibili",
  "thumbnail": "http://localhost:8080/api/music/proxy/image?url=...",
  "url": "https://www.bilibili.com/video/BV1aiT7zTEWD/",
  "quality": "HD",
  "playCount": "5.8万",
  "publishTime": "06-04"
}
```

## 注意事项

### 1. 条件渲染

使用 `v-if` 确保只在有数据时显示：
```vue
<span v-if="result.playCount" class="play-count">
<span v-if="result.publishTime" class="publish-time">
```

### 2. 数据格式

- 播放量：保持哔哩哔哩原始格式（如"5.8万"）
- 发布时间：去除多余符号（如"·"）

### 3. 兼容性

代码向后兼容，如果后端没有返回这些字段，前端不会显示，也不会报错。

## 未来扩展

### 1. 数据格式化

可以考虑在前端格式化播放量和时间：
- 播放量：统一格式（如将"5.8万"转换为"58,000"）
- 时间：相对时间显示（如"3天前"）

### 2. 更多信息

可以添加更多元数据：
- 点赞数
- 评论数
- 视频分类
- 视频标签

### 3. 排序功能

基于播放量或发布时间进行排序。
