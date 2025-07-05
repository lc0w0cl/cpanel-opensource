# URL链接下载功能修复

## 问题描述

用户反馈：在链接下载功能里，输入了B站视频链接 `https://www.bilibili.com/video/BV1aiT7zTEWD/?spm_id_from=333.337.search-card.all.click&vd_source=dae1902f2475988975e70383b270371d`，但系统把链接当作搜索内容了，应该直接展示这个链接的内容。

## 问题分析

1. **前端行为正确**：用户选择"链接下载"模式，前端正确传递了 `searchType: 'url'`
2. **后端逻辑错误**：后端的 `MusicSearchService.searchBilibili()` 方法没有检查 `searchType`，直接把URL当作搜索关键词处理
3. **结果错误**：系统去B站搜索这个URL字符串，而不是解析URL对应的视频信息

## 修复方案

### 1. 后端修改

修改 `backend/src/main/java/com/clover/cpanel/service/MusicSearchService.java`：

#### 1.1 修改 `searchBilibili` 方法
- 添加对 `searchType` 的判断
- 当 `searchType` 为 `url` 时，调用新的URL解析方法
- 当 `searchType` 为 `keyword` 时，保持原有搜索逻辑

#### 1.2 新增 `parseBilibiliVideoUrl` 方法
- 验证URL格式
- 提取视频ID
- 使用Jsoup获取视频页面信息
- 解析标题、UP主、时长、缩略图等信息
- 返回单个视频结果

#### 1.3 修改YouTube处理逻辑
- 添加对YouTube URL的支持
- 新增 `parseYouTubeVideoUrl` 方法
- 使用yt-dlp获取YouTube视频信息

#### 1.4 新增辅助方法
- `extractYouTubeVideoId`：从YouTube URL提取视频ID
- `extractJsonValue`：从JSON字符串提取字段值

### 2. 支持的URL格式

#### B站视频URL
- `https://www.bilibili.com/video/BV1aiT7zTEWD/`
- `https://www.bilibili.com/video/BV1aiT7zTEWD/?spm_id_from=...`
- `bilibili.com/video/BV1aiT7zTEWD/`

#### YouTube视频URL
- `https://www.youtube.com/watch?v=VIDEO_ID`
- `https://youtu.be/VIDEO_ID`
- `youtube.com/watch?v=VIDEO_ID`

## 测试方法

### 1. 测试B站链接下载

1. 打开音乐下载页面
2. 选择"链接下载"标签
3. 输入B站视频URL：`https://www.bilibili.com/video/BV1aiT7zTEWD/`
4. 点击搜索
5. **期望结果**：直接显示该视频的信息，而不是搜索结果

### 2. 测试YouTube链接下载

1. 选择"链接下载"标签
2. 输入YouTube视频URL：`https://www.youtube.com/watch?v=dQw4w9WgXcQ`
3. 点击搜索
4. **期望结果**：直接显示该视频的信息

### 3. 测试关键词搜索

1. 选择"关键词搜索"标签
2. 输入关键词：`周杰伦`
3. 点击搜索
4. **期望结果**：显示搜索结果列表（原有功能不受影响）

## 修复后的行为

### URL模式（searchType: 'url'）
- 直接解析输入的视频URL
- 返回单个视频的详细信息
- 支持B站和YouTube链接

### 关键词模式（searchType: 'keyword'）
- 保持原有的搜索逻辑
- 在B站搜索关键词
- 返回搜索结果列表

## 技术细节

### B站视频信息提取
- 使用Jsoup解析HTML页面
- 提取标题、UP主、时长、缩略图等信息
- 处理图片代理URL

### YouTube视频信息提取
- 使用yt-dlp命令行工具
- 获取JSON格式的视频信息
- 解析标题、作者、时长、播放量等信息

### 错误处理
- URL格式验证
- 网络请求异常处理
- 视频信息解析失败处理
- 详细的日志记录

## 注意事项

1. **依赖要求**：YouTube功能需要系统安装yt-dlp
2. **网络要求**：需要能够访问B站和YouTube
3. **性能考虑**：URL解析比关键词搜索更快，因为只需要获取单个视频信息
4. **兼容性**：修改保持向后兼容，不影响现有的关键词搜索功能
