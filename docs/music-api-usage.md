# 音乐搜索API使用说明

## 概述

本项目实现了哔哩哔哩音乐搜索功能，采用模拟浏览器地址栏的方式获取搜索结果。

## 架构说明

### 前端架构
- 使用 `apiRequest` 函数进行API调用
- 通过 `useMusicApi` 组合式函数封装音乐相关API
- 支持TypeScript类型定义

### 后端架构
- Spring Boot + Jsoup 实现网页爬取
- 模拟真实浏览器请求头
- 统一的API响应格式

## API接口

### 1. 搜索音乐
**接口**: `POST /api/music/search`

**请求参数**:
```json
{
  "query": "周杰伦",
  "searchType": "keyword",
  "platform": "bilibili",
  "page": 1,
  "pageSize": 20
}
```

**响应格式**:
```json
{
  "code": 200,
  "message": "操作成功",
  "success": true,
  "data": [
    {
      "id": "BV1xx411c7mu",
      "title": "周杰伦 - 青花瓷",
      "artist": "周杰伦",
      "duration": "3:58",
      "platform": "bilibili",
      "thumbnail": "https://...",
      "url": "https://www.bilibili.com/video/BV1xx411c7mu",
      "quality": "HD",
      "playCount": "100万",
      "publishTime": "2023-01-01"
    }
  ]
}
```

### 2. 获取视频详情
**接口**: `GET /api/music/video/{platform}/{videoId}`

## 前端使用方式

### 1. 导入音乐API
```typescript
import { useMusicApi, type MusicSearchResult } from '~/composables/useMusicApi'

const { searchMusic, downloadMusic } = useMusicApi()
```

### 2. 搜索音乐
```typescript
const searchRequest = {
  query: '周杰伦',
  searchType: 'keyword' as const,
  platform: 'bilibili' as const,
  page: 1,
  pageSize: 20
}

try {
  const results = await searchMusic(searchRequest)
  console.log('搜索结果:', results)
} catch (error) {
  console.error('搜索失败:', error)
}
```

### 3. 下载音乐
```typescript
const success = await downloadMusic(videoUrl)
if (success) {
  console.log('下载开始')
} else {
  console.log('下载失败')
}
```

## 特性说明

### 1. 模拟浏览器请求
- 完整的浏览器请求头
- 支持重定向和Cookie
- 防反爬虫机制

### 2. 多平台支持
- 哔哩哔哩搜索（已实现）
- YouTube搜索（待实现）
- 统一的数据格式

### 3. 错误处理
- 网络错误处理
- 业务逻辑错误处理
- 用户友好的错误提示

### 4. 类型安全
- TypeScript类型定义
- 接口参数验证
- 响应数据类型检查

## 开发环境配置

### 1. 后端配置
```yaml
# application.yml
spring:
  application:
    name: cpanel
```

### 2. 前端配置
```typescript
// nuxt.config.ts
runtimeConfig: {
  public: {
    apiBaseUrl: process.env.NODE_ENV === 'development'
      ? 'http://localhost:8080'
      : ''
  }
}
```

## 测试方法

### 1. 启动后端服务
```bash
cd backend
mvn spring-boot:run
```

### 2. 启动前端服务
```bash
cd frontend
npm run dev
```

### 3. 访问音乐页面
打开浏览器访问: `http://localhost:3000/home/music`

### 4. 测试搜索功能
1. 输入搜索关键词（如：周杰伦）
2. 选择搜索平台（哔哩哔哩）
3. 点击搜索按钮
4. 查看搜索结果

## 注意事项

1. **网络请求**: 需要确保网络连接正常
2. **反爬虫**: 哔哩哔哩可能有反爬虫机制，请适度使用
3. **版权**: 请遵守相关平台的版权规定
4. **性能**: 大量请求可能影响性能，建议添加缓存机制

## 扩展功能

### 1. YouTube支持
可以类似地实现YouTube搜索功能

### 2. 下载功能
集成yt-dlp实现真实下载功能

### 3. 缓存机制
添加Redis缓存提高性能

### 4. 分页支持
实现搜索结果分页加载
