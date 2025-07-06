# 平台特定Cookie实现文档

## 概述

实现了基于平台的cookie配置系统，支持Bilibili和YouTube平台的独立cookie配置。系统会根据URL自动检测平台，并使用对应平台的cookie来获取格式列表。

## 核心改进

### 1. 平台检测
- **自动识别**: 根据URL自动检测平台类型
- **支持平台**: Bilibili (bilibili.com, b23.tv), YouTube (youtube.com, youtu.be)
- **默认处理**: 无法识别的URL默认使用Bilibili平台

### 2. Cookie管理
- **独立配置**: 每个平台有独立的cookie配置
- **数据库存储**: cookie存储在`panel_system_config`表中
- **动态读取**: 运行时从数据库读取最新配置

### 3. yt-dlp集成
- **Header方式**: 使用`--add-header "Cookie: ..."`传递cookie
- **平台特定**: 根据平台使用对应的cookie
- **安全处理**: 避免cookie泄露到日志中

## 技术实现

### 后端修改

#### 1. MusicSearchService.java
```java
// 新增平台参数
public List<Map<String, Object>> getAvailableFormats(String videoUrl, String platform)

// 根据平台获取cookie
private String getCookieByPlatform(String platform) {
    String cookieKey;
    switch (platform.toLowerCase()) {
        case "bilibili":
            cookieKey = "bilibili_cookie";
            break;
        case "youtube":
            cookieKey = "youtube_cookie";
            break;
        default:
            return null;
    }
    return systemConfigService.getConfigValue(cookieKey);
}

// 构建yt-dlp命令
List<String> command = new ArrayList<>();
command.add("yt-dlp");
command.add("-F");
command.add("--no-playlist");

if (cookieValue != null && !cookieValue.trim().isEmpty()) {
    command.add("--add-header");
    command.add("Cookie: " + cookieValue.trim());
}
```

#### 2. MusicController.java
```java
@PostMapping("/formats")
public ApiResponse<List<Map<String, Object>>> getAvailableFormats(@RequestBody Map<String, String> request) {
    String url = request.get("url");
    String platform = request.get("platform");
    
    // 验证参数
    if (platform == null || platform.trim().isEmpty()) {
        return ApiResponse.error("平台标识不能为空");
    }
    
    List<Map<String, Object>> formats = musicSearchService.getAvailableFormats(url, platform);
    return ApiResponse.success(formats);
}
```

#### 3. DatabaseInitServiceImpl.java
```java
// 初始化Bilibili cookie配置
if (!checkConfigExists("bilibili_cookie")) {
    String insertSql = """
        INSERT INTO panel_system_config (config_key, config_value, description, config_type, created_at, updated_at)
        VALUES ('bilibili_cookie', '', 'Bilibili平台cookie配置', 'music', NOW(), NOW())
        """;
    jdbcTemplate.update(insertSql);
}

// 初始化YouTube cookie配置
if (!checkConfigExists("youtube_cookie")) {
    String insertSql = """
        INSERT INTO panel_system_config (config_key, config_value, description, config_type, created_at, updated_at)
        VALUES ('youtube_cookie', '', 'YouTube平台cookie配置', 'music', NOW(), NOW())
        """;
    jdbcTemplate.update(insertSql);
}
```

### 前端修改

#### 1. useMusicApi.ts
```typescript
// 新增平台检测函数
const detectPlatform = (url: string): string => {
  if (url.includes('bilibili.com') || url.includes('b23.tv')) {
    return 'bilibili'
  } else if (url.includes('youtube.com') || url.includes('youtu.be')) {
    return 'youtube'
  } else {
    return 'bilibili' // 默认
  }
}

// 修改API调用，添加平台参数
const getAvailableFormats = async (videoUrl: string, platform: string): Promise<any[]> => {
  const response = await apiRequest(`${API_BASE_URL}/music/formats`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      url: videoUrl,
      platform: platform
    })
  })
  // ...
}
```

#### 2. FormatSelector.vue
```vue
<script setup lang="ts">
interface Props {
  visible: boolean
  videoUrl: string
  platform: string  // 新增平台参数
  title: string
  artist: string
}

// 获取格式列表时传递平台参数
const loadFormats = async () => {
  if (!props.videoUrl || !props.platform) return
  const formatList = await getAvailableFormats(props.videoUrl, props.platform)
  // ...
}
</script>
```

#### 3. music.vue
```vue
<script setup lang="ts">
// 显示格式选择时确保平台信息
const showFormatSelection = (item: MusicSearchResult) => {
  if (!item.platform) {
    item.platform = detectPlatform(item.url)
  }
  currentDownloadItem.value = item
  showFormatSelector.value = true
}
</script>

<template>
  <FormatSelector
    :visible="showFormatSelector"
    :video-url="currentDownloadItem?.url || ''"
    :platform="currentDownloadItem?.platform || ''"
    :title="currentDownloadItem?.title || ''"
    :artist="currentDownloadItem?.artist || ''"
    @close="closeFormatSelector"
    @select="handleFormatSelect"
  />
</template>
```

## 数据库配置

### 配置表结构
```sql
-- 查看cookie配置
SELECT config_key, config_value, description 
FROM panel_system_config 
WHERE config_key IN ('bilibili_cookie', 'youtube_cookie');

-- 设置Bilibili cookie
UPDATE panel_system_config 
SET config_value = 'SESSDATA=xxx; bili_jct=xxx; DedeUserID=xxx; buvid3=xxx' 
WHERE config_key = 'bilibili_cookie';

-- 设置YouTube cookie
UPDATE panel_system_config 
SET config_value = 'session_token=xxx; VISITOR_INFO1_LIVE=xxx; YSC=xxx' 
WHERE config_key = 'youtube_cookie';
```

### Cookie获取方法

#### Bilibili Cookie
1. 登录bilibili.com
2. 打开开发者工具 (F12)
3. 切换到Network标签
4. 刷新页面
5. 找到任意请求，查看Request Headers中的Cookie
6. 复制SESSDATA、bili_jct、DedeUserID等关键字段

#### YouTube Cookie
1. 登录youtube.com
2. 打开开发者工具 (F12)
3. 切换到Application标签
4. 在左侧找到Cookies > https://www.youtube.com
5. 复制session_token、VISITOR_INFO1_LIVE等关键字段

## 使用流程

1. **配置Cookie**: 在设置页面配置各平台的cookie
2. **搜索音乐**: 输入关键词或URL进行搜索
3. **平台检测**: 系统自动检测URL对应的平台
4. **格式获取**: 使用对应平台的cookie获取格式列表
5. **格式选择**: 用户选择想要的格式进行下载

## 优势特点

1. **精确匹配**: 每个平台使用专门的cookie，提高成功率
2. **自动检测**: 无需手动选择平台，系统自动识别
3. **灵活配置**: 支持在设置页面独立配置各平台cookie
4. **安全处理**: cookie不会完整显示在日志中
5. **向后兼容**: 保持原有功能的同时增强cookie支持

## 注意事项

1. **Cookie有效期**: 需要定期更新cookie以保持有效性
2. **隐私安全**: cookie包含敏感信息，请妥善保管
3. **平台限制**: 不同平台对cookie的要求可能不同
4. **网络环境**: 某些网络环境可能影响cookie的使用效果

## 故障排除

### 常见问题
1. **格式获取失败**: 检查cookie是否有效，是否需要更新
2. **平台检测错误**: 确认URL格式是否正确
3. **权限问题**: 确认cookie对应的账号有访问权限

### 调试方法
1. 查看后端日志确认平台检测和cookie使用情况
2. 手动测试yt-dlp命令验证cookie有效性
3. 检查数据库中cookie配置是否正确

## 总结

通过实现平台特定的cookie配置，系统现在能够更精确地处理不同平台的内容获取需求。这种设计提高了格式获取的成功率，同时保持了良好的用户体验和系统的可维护性。
