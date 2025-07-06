# 格式选择功能实现文档

## 功能概述

实现了音乐下载前的格式选择功能，用户在点击下载按钮时会弹出格式选择弹窗，显示所有可用的音频和视频格式，用户可以选择合适的格式进行下载。

## 技术实现

### 后端实现

#### 1. MusicSearchService.java 新增方法

```java
/**
 * 获取可用格式列表（通过视频URL）
 */
public List<Map<String, Object>> getAvailableFormats(String videoUrl) {
    // 使用 yt-dlp -F 命令获取格式列表
    ProcessBuilder processBuilder = new ProcessBuilder(
        "yt-dlp",
        "-F",                  // 列出所有可用格式
        "--no-playlist",       // 不处理播放列表
        "--cookies-from-browser", "chrome", // 使用Chrome浏览器的cookie
        videoUrl
    );
    // ... 处理输出和解析
}
```

#### 2. MusicController.java 新增API接口

```java
/**
 * 获取可用格式列表
 */
@PostMapping("/formats")
public ApiResponse<List<Map<String, Object>>> getAvailableFormats(@RequestBody Map<String, String> request)
```

### 前端实现

#### 1. FormatSelector.vue 组件

新建的格式选择组件，包含以下功能：
- 显示音频格式列表（优先显示flac格式）
- 显示视频格式列表
- 自动选择默认格式（音频优先选择flac，如果没有则选择最高质量）
- 支持用户手动选择格式
- 美观的UI设计，支持暗色主题

#### 2. useMusicApi.ts 新增方法

```typescript
/**
 * 获取可用格式列表
 */
const getAvailableFormats = async (videoUrl: string): Promise<any[]> => {
    // 调用后端API获取格式列表
}
```

#### 3. music.vue 页面修改

- 添加格式选择相关状态
- 修改下载按钮点击事件，显示格式选择弹窗
- 集成FormatSelector组件

## 功能特性

### 1. 智能格式选择
- **音频下载优先级**：flac > 高比特率mp3 > 其他音频格式
- **视频下载优先级**：高分辨率 > 低分辨率
- **自动默认选择**：根据优先级自动选择最佳格式

### 2. 用户体验优化
- **加载状态**：显示"正在获取可用格式..."
- **错误处理**：显示错误信息和重试按钮
- **空状态**：当没有可用格式时显示提示
- **响应式设计**：适配不同屏幕尺寸

### 3. 格式信息展示
- **格式类型**：清晰区分音频和视频格式
- **质量信息**：显示比特率、分辨率等信息
- **格式描述**：显示yt-dlp提供的详细描述
- **颜色标识**：不同格式使用不同颜色的标签

## 使用流程

1. **搜索音乐**：用户在搜索框中输入关键词或链接
2. **点击下载**：点击搜索结果中的下载按钮
3. **格式选择**：弹出格式选择弹窗，显示所有可用格式
4. **选择格式**：用户选择想要的格式（默认已选择最佳格式）
5. **确认下载**：点击"下载选中格式"按钮开始下载

## 技术细节

### 1. yt-dlp 命令参数
```bash
yt-dlp -F --no-playlist --cookies-from-browser chrome [URL]
```
- `-F`: 列出所有可用格式
- `--no-playlist`: 不处理播放列表
- `--cookies-from-browser chrome`: 使用Chrome浏览器的cookie

### 2. 格式解析逻辑
- 解析yt-dlp输出的格式表格
- 提取格式ID、扩展名、分辨率、比特率等信息
- 区分音频和视频格式
- 按质量排序

### 3. 前端状态管理
```typescript
// 格式选择相关状态
const showFormatSelector = ref(false)
const currentDownloadItem = ref<MusicSearchResult | null>(null)
```

## 配置说明

### 1. 依赖要求
- **后端**：需要安装yt-dlp命令行工具
- **前端**：需要@iconify/vue图标库

### 2. 浏览器兼容性
- 支持现代浏览器（Chrome、Firefox、Safari、Edge）
- 需要支持ES6+语法

## 未来扩展

### 1. 格式过滤
- 添加格式类型过滤（仅音频/仅视频）
- 添加质量范围过滤

### 2. 下载设置
- 记住用户的格式偏好
- 支持批量下载时应用相同格式

### 3. 高级功能
- 支持自定义yt-dlp参数
- 支持格式转换选项

## 注意事项

1. **性能考虑**：获取格式列表需要调用yt-dlp，可能需要几秒钟时间
2. **错误处理**：网络问题或视频不可用时需要适当的错误提示
3. **用户体验**：避免频繁弹窗，考虑添加"记住选择"功能
4. **安全性**：确保URL参数的安全性，防止命令注入

## 测试建议

1. **功能测试**：测试不同平台的视频链接（B站、YouTube）
2. **边界测试**：测试无效链接、网络错误等情况
3. **性能测试**：测试格式列表获取的响应时间
4. **UI测试**：测试不同屏幕尺寸下的显示效果
5. **Cookie测试**：测试不同cookie配置下的格式获取效果
6. **平台测试**：测试平台自动检测功能

## Cookie配置更新（最新实现）

### 平台特定Cookie
系统现在支持平台特定的cookie配置，从数据库读取：

```sql
-- 查看当前cookie配置
SELECT * FROM panel_system_config WHERE config_key IN ('bilibili_cookie', 'youtube_cookie');

-- 设置Bilibili cookie
INSERT INTO panel_system_config (config_key, config_value, description, config_type, created_at, updated_at)
VALUES ('bilibili_cookie', 'SESSDATA=xxx; bili_jct=xxx; DedeUserID=xxx', 'Bilibili平台cookie配置', 'music', NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = 'SESSDATA=xxx; bili_jct=xxx; DedeUserID=xxx', updated_at = NOW();

-- 设置YouTube cookie
INSERT INTO panel_system_config (config_key, config_value, description, config_type, created_at, updated_at)
VALUES ('youtube_cookie', 'session_token=xxx; VISITOR_INFO1_LIVE=xxx', 'YouTube平台cookie配置', 'music', NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = 'session_token=xxx; VISITOR_INFO1_LIVE=xxx', updated_at = NOW();
```

### 技术实现要点
- **平台检测**: 根据URL自动识别平台（bilibili.com、youtube.com等）
- **API调用**: 格式获取API需要传入platform参数
- **Cookie使用**: 使用`--add-header "Cookie: ..."`方式传递cookie
- **数据库存储**: cookie存储在panel_system_config表中

### yt-dlp命令示例
```bash
# Bilibili
yt-dlp -F --no-playlist --add-header "Cookie: SESSDATA=xxx; bili_jct=xxx" [URL]

# YouTube
yt-dlp -F --no-playlist --add-header "Cookie: session_token=xxx" [URL]
```
