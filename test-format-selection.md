# 格式选择功能测试

## 功能概述
验证根据用户选择的格式来确定下载文件的扩展名，而不是简单地从URL推断。

## 核心改进

### 1. 前端改进
- **generateFileName函数**: 接受selectedFormat参数而不是audioUrl
- **下载流程**: 用户选择格式后传递给下载函数
- **批量下载**: 使用默认格式（音频mp3）

### 2. 后端改进
- **服务器下载接口**: 接受selectedFormat参数
- **文件名生成**: 根据格式信息确定扩展名

## 格式处理逻辑

### 音频格式优先级
1. **flac**: 如果选择flac格式，保存为.flac文件
2. **m4a**: 保存为.m4a文件
3. **webm**: 保存为.webm文件
4. **ogg**: 保存为.ogg文件
5. **aac**: 保存为.aac文件
6. **opus**: 保存为.opus文件
7. **默认**: 其他音频格式默认保存为.mp3

### 视频格式优先级
1. **mp4**: 保存为.mp4文件（默认）
2. **webm**: 保存为.webm文件
3. **mkv**: 保存为.mkv文件
4. **avi**: 保存为.avi文件
5. **默认**: 其他视频格式默认保存为.mp4

## 测试用例

### 1. 音频格式测试
```javascript
// 测试flac格式
const flacFormat = {
  formatId: "251",
  ext: "flac",
  resolution: "audio",
  note: "audio only flac 3381k",
  isAudio: true,
  isVideo: false,
  bitrate: "3381k"
}

// 预期文件名: "艺术家 - 歌曲名.flac"
const fileName = generateFileName("测试歌曲", "测试艺术家", flacFormat)
// 结果: "测试艺术家 - 测试歌曲.flac"
```

### 2. 视频格式测试
```javascript
// 测试mp4格式
const mp4Format = {
  formatId: "18",
  ext: "mp4",
  resolution: "720p",
  note: "720p mp4",
  isAudio: false,
  isVideo: true
}

// 预期文件名: "艺术家 - 歌曲名.mp4"
const fileName = generateFileName("测试视频", "测试作者", mp4Format)
// 结果: "测试作者 - 测试视频.mp4"
```

### 3. 默认格式测试
```javascript
// 测试无格式信息
const fileName = generateFileName("测试歌曲", "测试艺术家")
// 结果: "测试艺术家 - 测试歌曲.mp3" (默认mp3)

// 测试批量下载默认格式
const defaultFormat = { isAudio: true, ext: 'mp3' }
const fileName = generateFileName("测试歌曲", "测试艺术家", defaultFormat)
// 结果: "测试艺术家 - 测试歌曲.mp3"
```

## API测试

### 1. 前端API调用
```javascript
// 获取格式列表
const formats = await getAvailableFormats(videoUrl, platform)

// 用户选择格式后下载
const selectedFormat = formats[0] // 用户选择的格式
await downloadMusic(musicResult, onProgress, selectedFormat)
```

### 2. 后端API接口
```bash
# 服务器下载测试
curl -X POST http://localhost:8080/api/music/download-to-server \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.bilibili.com/video/BV1xx411c7mu",
    "title": "测试歌曲",
    "artist": "测试艺术家",
    "platform": "bilibili",
    "selectedFormat": {
      "formatId": "251",
      "ext": "flac",
      "resolution": "audio",
      "note": "audio only flac 3381k",
      "isAudio": true,
      "isVideo": false,
      "bitrate": "3381k"
    }
  }'
```

## 验证要点

### 1. 格式选择流程
- [ ] 点击下载按钮显示格式选择弹窗
- [ ] 格式列表正确显示音频和视频格式
- [ ] 默认选择flac格式（如果可用）
- [ ] 用户可以手动选择其他格式

### 2. 文件名生成
- [ ] 音频格式正确映射到对应扩展名
- [ ] 视频格式正确映射到对应扩展名
- [ ] 默认格式处理正确
- [ ] 文件名中非法字符正确清理

### 3. 下载功能
- [ ] 本地下载使用正确的文件名
- [ ] 服务器下载使用正确的文件名
- [ ] 批量下载使用默认格式
- [ ] 格式信息正确传递到后端

### 4. 边界情况
- [ ] 无格式信息时使用默认扩展名
- [ ] 未知格式时的处理
- [ ] 格式信息不完整时的处理

## 预期结果

### 1. 用户体验
- 用户可以看到所有可用格式
- 音频下载优先选择flac格式
- 视频下载默认选择mp4格式
- 文件名准确反映选择的格式

### 2. 文件输出
```
# 音频文件示例
艺术家 - 歌曲名.flac  (选择flac格式)
艺术家 - 歌曲名.mp3   (选择mp3格式或默认)
艺术家 - 歌曲名.m4a   (选择m4a格式)

# 视频文件示例
作者 - 视频名.mp4     (选择mp4格式或默认)
作者 - 视频名.webm    (选择webm格式)
作者 - 视频名.mkv     (选择mkv格式)
```

### 3. 后端日志
```
INFO - 开始服务器下载音乐: title=测试歌曲, artist=测试艺术家
DEBUG - 选择的格式: {ext=flac, isAudio=true, bitrate=3381k}
INFO - 生成文件名: 测试艺术家 - 测试歌曲.flac
INFO - 服务器下载成功: 测试艺术家 - 测试歌曲.flac
```

## 总结

通过这个改进，系统现在能够：

1. **精确格式控制**: 根据用户选择的具体格式确定文件扩展名
2. **智能默认选择**: 音频优先flac，视频优先mp4
3. **完整流程支持**: 从格式选择到文件下载的完整流程
4. **批量下载兼容**: 批量下载时使用合理的默认格式

这确保了下载的文件具有正确的扩展名，提升了用户体验和文件管理的便利性。
