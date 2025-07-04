# 在线音乐播放功能实现

## 功能概述

实现了完整的在线音乐播放功能，用户可以直接在浏览器中播放搜索到的音乐，无需下载到本地。

## 技术架构

### 1. 后端音频流服务

#### yt-dlp集成
```java
// 获取音频流URL
ProcessBuilder processBuilder = new ProcessBuilder(
    "yt-dlp",
    "--get-url",           // 只获取URL，不下载
    "--format", "bestaudio/best",  // 获取最佳音频格式
    "--no-playlist",       // 不处理播放列表
    videoUrl
);
```

#### API接口
- `GET /api/music/stream/{platform}/{videoId}` - 通过平台和视频ID获取音频流
- `POST /api/music/stream-url` - 通过视频URL获取音频流

### 2. 前端音频播放器

#### HTML5 Audio API
- 使用原生 `Audio` 对象进行播放
- 支持流媒体播放，无需完整下载
- 自动处理各种音频格式

#### 播放状态管理
```typescript
const currentPlaying = ref<MusicSearchResult | null>(null)
const isPlaying = ref(false)
const isLoading = ref(false)
const audioElement = ref<HTMLAudioElement | null>(null)
```

## 核心功能

### 1. 音频流获取

#### 后端实现
```java
public String getAudioStreamUrlByUrl(String videoUrl) {
    ProcessBuilder processBuilder = new ProcessBuilder(
        "yt-dlp", "--get-url", "--format", "bestaudio/best", 
        "--no-playlist", videoUrl
    );
    
    // 执行命令并获取音频流URL
    Process process = processBuilder.start();
    // ... 处理输出
}
```

#### 前端调用
```typescript
const getAudioStream = async (platform: string, videoId: string) => {
    const response = await fetch(`${API_BASE_URL}/music/stream/${platform}/${videoId}`)
    const result = await response.json()
    return result.success ? result.data : null
}
```

### 2. 播放控制

#### 播放音乐
```typescript
const playMusic = async (result: MusicSearchResult) => {
    // 获取音频流URL
    const audioUrl = await getAudioStream(result.platform, result.id)
    
    // 创建音频元素
    const audio = new Audio(audioUrl)
    audioElement.value = audio
    
    // 设置事件监听
    audio.addEventListener('timeupdate', () => {
        currentTime.value = audio.currentTime
    })
    
    // 开始播放
    await audio.play()
    isPlaying.value = true
}
```

#### 播放控制方法
- `playMusic()` - 播放指定音乐
- `pauseMusic()` - 暂停播放
- `resumeMusic()` - 继续播放
- `stopMusic()` - 停止播放
- `setProgress()` - 设置播放进度
- `setVolume()` - 设置音量

### 3. 用户界面

#### 卡片播放按钮
```vue
<button @click="playMusic(result)" class="play-btn">
  <Icon v-if="isLoading" icon="mdi:loading" class="loading-icon" />
  <Icon v-else-if="isPlaying" icon="mdi:pause" />
  <Icon v-else icon="mdi:play" />
  {{ isPlaying ? '暂停' : '播放' }}
</button>
```

#### 全局播放器
- 固定在页面底部
- 显示当前播放信息
- 播放控制按钮
- 进度条和时间显示
- 音量控制

## 界面设计

### 1. 卡片播放按钮

#### 状态指示
- **播放状态**: 紫色背景，暂停图标
- **加载状态**: 旋转加载图标
- **默认状态**: 播放图标

#### 样式特性
```css
.play-btn.playing {
  background: linear-gradient(135deg,
    rgba(168, 85, 247, 0.2) 0%,
    rgba(168, 85, 247, 0.1) 100%
  );
  border-color: rgba(168, 85, 247, 0.4);
  color: rgba(168, 85, 247, 1);
}
```

### 2. 全局播放器

#### 布局结构
```
[缩略图] [标题/艺术家] [停止] [播放/暂停] [进度条] [音量]
```

#### 响应式设计
- 桌面端：水平布局
- 移动端：垂直堆叠布局

#### 视觉特性
- 毛玻璃效果背景
- 固定在底部，不遮挡内容
- 平滑的动画过渡

## 技术特性

### 1. 流媒体播放
- 无需等待完整下载
- 支持实时播放
- 自动缓冲管理

### 2. 格式兼容性
- 自动选择最佳音频格式
- 支持多种编码格式
- 浏览器原生解码

### 3. 性能优化
- 懒加载音频流
- 内存管理优化
- 网络请求优化

### 4. 错误处理
- 网络错误恢复
- 音频加载失败处理
- 用户友好的错误提示

## 使用流程

### 1. 搜索音乐
1. 用户输入关键词搜索
2. 显示搜索结果卡片
3. 每个卡片包含播放按钮

### 2. 开始播放
1. 点击播放按钮
2. 后端调用yt-dlp获取音频流
3. 前端创建Audio对象
4. 开始播放并显示播放器

### 3. 播放控制
1. 全局播放器显示在底部
2. 支持播放/暂停/停止
3. 进度条拖拽控制
4. 音量调节

## 依赖要求

### 1. 后端依赖
- **yt-dlp**: 视频/音频流提取工具
- **ffmpeg**: 音频处理（yt-dlp依赖）
- **Java ProcessBuilder**: 执行外部命令

### 2. 前端依赖
- **HTML5 Audio API**: 音频播放
- **Vue 3 Composition API**: 状态管理
- **CSS Grid/Flexbox**: 响应式布局

### 3. 系统要求
- Windows/Linux/macOS
- yt-dlp可执行文件
- ffmpeg可执行文件

## 安装配置

### 1. 安装yt-dlp
```bash
# Windows
pip install yt-dlp

# 或下载可执行文件
# https://github.com/yt-dlp/yt-dlp/releases
```

### 2. 安装ffmpeg
```bash
# Windows
# 下载并配置环境变量
# https://ffmpeg.org/download.html

# Linux
sudo apt install ffmpeg

# macOS
brew install ffmpeg
```

### 3. 验证安装
```bash
yt-dlp --version
ffmpeg -version
```

## 测试方法

### 1. 使用测试页面
打开 `test-music-player.html` 进行功能测试：
- 输入视频链接
- 测试音频流获取
- 验证播放控制功能

### 2. API测试
```bash
# 测试音频流获取
curl -X POST http://localhost:8080/api/music/stream-url \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.bilibili.com/video/BV1xx411c7mu"}'
```

### 3. 前端集成测试
1. 启动后端服务
2. 访问音乐页面
3. 搜索音乐并测试播放

## 故障排除

### 1. 音频流获取失败
- 检查yt-dlp是否正确安装
- 验证视频URL是否有效
- 查看后端日志错误信息

### 2. 播放失败
- 检查浏览器音频支持
- 验证音频流URL是否可访问
- 检查网络连接状态

### 3. 性能问题
- 检查网络带宽
- 优化音频质量设置
- 考虑添加缓存机制

## 扩展功能

### 1. 播放列表
- 支持多首歌曲队列
- 自动播放下一首
- 随机播放模式

### 2. 音质选择
- 提供多种音质选项
- 根据网络状况自适应
- 用户偏好设置

### 3. 离线缓存
- 本地音频缓存
- 离线播放支持
- 缓存管理功能

### 4. 社交功能
- 播放历史记录
- 收藏夹功能
- 分享播放链接

## 安全考虑

### 1. 版权合规
- 仅用于个人学习和测试
- 遵守平台使用条款
- 不用于商业用途

### 2. 资源保护
- 限制并发请求数量
- 防止滥用API接口
- 合理的缓存策略

### 3. 隐私保护
- 不存储用户播放记录
- 不收集个人信息
- 透明的数据使用政策
