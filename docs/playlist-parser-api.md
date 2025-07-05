# 歌单解析API文档

## 概述

歌单解析功能支持解析QQ音乐和网易云音乐的歌单，提取歌单信息和歌曲列表。

## 支持的平台

- **QQ音乐**: 支持新版和旧版歌单链接
- **网易云音乐**: 支持标准歌单链接

## API接口

### 1. 解析歌单

**接口**: `POST /api/music/parse-playlist`

**请求体**:
```json
{
  "url": "https://y.qq.com/n/ryqq/playlist/8668419170",
  "platform": "auto"
}
```

**参数说明**:
- `url`: 歌单链接（必填）
- `platform`: 平台类型，可选值：`qq`、`netease`、`auto`（默认为auto自动识别）

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "success": true,
  "data": {
    "title": "我的歌单",
    "creator": "用户名",
    "cover": "https://example.com/cover.jpg",
    "url": "https://y.qq.com/n/ryqq/playlist/8668419170",
    "source": "qq",
    "songCount": 20,
    "description": "歌单描述",
    "songs": [
      {
        "title": "歌曲名",
        "artist": "歌手名",
        "album": "专辑名",
        "url": "https://y.qq.com/n/ryqq/songDetail/xxx",
        "cover": "https://example.com/song-cover.jpg",
        "source": "qq",
        "sourceId": "123456",
        "duration": "03:45"
      }
    ]
  }
}
```

### 2. 获取支持的平台

**接口**: `GET /api/music/supported-platforms`

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "success": true,
  "data": ["qq", "netease"]
}
```

## 支持的链接格式

### QQ音乐
- 新版链接: `https://y.qq.com/n/ryqq/playlist/8668419170`
- 旧版链接: `https://y.qq.com/n/yqq/playlist/7266910918.html`
- 分享链接: `https://y.qq.com/n/ryqq/playlist?id=8668419170`

### 网易云音乐
- 标准链接: `https://music.163.com/#/playlist?id=123456`
- 新版链接: `https://music.163.com/playlist/123456/`

## 前端使用示例

```typescript
import { useMusicApi, type PlaylistInfo } from '~/composables/useMusicApi'

const { parsePlaylist, getSupportedPlatforms } = useMusicApi()

// 解析歌单
const parsePlaylistExample = async () => {
  try {
    const playlist: PlaylistInfo = await parsePlaylist({
      url: 'https://y.qq.com/n/ryqq/playlist/8668419170',
      platform: 'auto'
    })
    
    console.log('歌单信息:', playlist)
    console.log('歌曲数量:', playlist.songCount)
    console.log('歌曲列表:', playlist.songs)
  } catch (error) {
    console.error('解析失败:', error)
  }
}

// 获取支持的平台
const getSupportedPlatformsExample = async () => {
  try {
    const platforms = await getSupportedPlatforms()
    console.log('支持的平台:', platforms)
  } catch (error) {
    console.error('获取平台失败:', error)
  }
}
```

## 错误处理

常见错误及解决方案：

1. **无效的歌单链接**: 检查链接格式是否正确
2. **不支持的平台**: 目前只支持QQ音乐和网易云音乐
3. **网络请求失败**: 检查网络连接或稍后重试
4. **歌单不存在或已删除**: 确认歌单链接有效性

## 注意事项

1. 网易云音乐的API可能有反爬虫机制，解析可能不稳定
2. 歌单解析不需要用户认证
3. 解析结果中的歌曲链接为原平台链接，不是直接播放链接
4. 建议在前端添加加载状态提示，因为解析可能需要几秒时间
