# 图片代理功能说明

## 问题背景

哔哩哔哩的图片资源有防盗链保护，直接在前端显示会出现403错误。为了解决这个问题，我们在后端实现了图片代理功能。

## 解决方案

### 1. 后端图片代理接口

**接口地址**: `GET /api/music/proxy/image`

**参数**:
- `url`: 原始图片URL（需要URL编码）

**功能**:
- 验证图片URL是否为哔哩哔哩域名
- 使用正确的请求头获取图片
- 返回图片数据并设置适当的响应头

### 2. 自动URL转换

在 `MusicSearchService` 中，搜索结果的缩略图URL会自动转换为代理URL：

```java
// 原始URL
//i2.hdslb.com/bfs/archive/example.jpg

// 转换后的代理URL
http://localhost:8080/api/music/proxy/image?url=//i2.hdslb.com/bfs/archive/example.jpg
```

## 技术实现

### 1. 请求头设置

代理请求使用以下请求头来模拟真实浏览器：

```java
connection.setRequestProperty("User-Agent", "Mozilla/5.0 ...");
connection.setRequestProperty("Referer", "https://www.bilibili.com/");
connection.setRequestProperty("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
```

### 2. 域名验证

只允许代理哔哩哔哩的图片域名：
- `i0.hdslb.com`
- `i1.hdslb.com`
- `i2.hdslb.com`
- `s1.hdslb.com`
- `s2.hdslb.com`

### 3. 缓存设置

响应头设置了1小时的缓存：
```java
headers.setCacheControl("public, max-age=3600");
```

## 配置说明

### 1. 配置文件

在 `module.yml` 中配置代理服务器地址：

```yaml
music:
  proxy:
    base-url: http://localhost:8080
```

### 2. 环境配置

- **开发环境**: 使用 `localhost:8080`
- **生产环境**: 需要配置实际的服务器地址

## 使用方式

### 1. 自动使用

搜索音乐时，返回的结果中的缩略图URL已经自动转换为代理URL，前端直接使用即可。

### 2. 手动使用

如果需要手动代理图片：

```javascript
const originalUrl = '//i2.hdslb.com/bfs/archive/example.jpg';
const proxyUrl = `/api/music/proxy/image?url=${encodeURIComponent(originalUrl)}`;
```

## 测试方法

### 1. 使用测试页面

打开 `test-image-proxy.html` 进行可视化测试。

### 2. 直接API测试

```bash
# 测试代理接口
curl "http://localhost:8080/api/music/proxy/image?url=//i2.hdslb.com/bfs/archive/example.jpg"

# 测试搜索API（包含代理图片）
curl "http://localhost:8080/api/music/test-search?query=稻香"
```

### 3. 前端测试

在音乐页面进行搜索，检查图片是否正常显示。

## 错误处理

### 1. 无效URL

如果URL不是哔哩哔哩的图片域名，返回400错误。

### 2. 网络错误

如果无法获取原始图片，返回相应的HTTP状态码。

### 3. 日志记录

所有错误都会记录到日志中，便于调试：

```java
log.error("代理图片时发生错误", e);
```

## 性能优化

### 1. 缓存策略

- 浏览器缓存：1小时
- 可以考虑添加服务器端缓存

### 2. 连接超时

设置了合理的连接和读取超时：
```java
connection.setConnectTimeout(10000);  // 10秒
connection.setReadTimeout(10000);     // 10秒
```

### 3. 资源管理

使用 try-with-resources 确保连接正确关闭。

## 安全考虑

### 1. 域名白名单

只允许代理哔哩哔哩的图片，防止被滥用。

### 2. URL验证

对输入的URL进行严格验证。

### 3. 错误信息

不暴露敏感的内部错误信息。

## 扩展功能

### 1. 支持更多平台

可以扩展支持YouTube等其他平台的图片代理。

### 2. 图片处理

可以添加图片压缩、格式转换等功能。

### 3. CDN集成

可以将代理的图片上传到CDN以提高性能。

## 故障排除

### 1. 图片仍然403

- 检查代理URL是否正确生成
- 检查后端日志是否有错误
- 验证请求头是否正确设置

### 2. 代理失败

- 检查网络连接
- 验证原始URL是否有效
- 查看后端错误日志

### 3. 性能问题

- 检查图片大小
- 考虑添加缓存
- 优化网络连接
