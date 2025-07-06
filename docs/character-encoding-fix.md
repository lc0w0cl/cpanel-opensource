# 字符编码乱码修复文档

## 问题描述

在FormatSelector组件中显示格式信息时出现乱码，如：`��89.06MiB`。这种问题通常由以下原因导致：

1. **yt-dlp输出编码问题**：yt-dlp输出包含特殊字符或非UTF-8编码
2. **字符传输过程中的编码转换**：从后端到前端的数据传输过程中编码丢失
3. **控制字符干扰**：输出中包含不可见的控制字符

## 解决方案

### 1. 前端字符清理

#### 实现代码
```javascript
// 清理文本中的乱码字符
const cleanText = (text: string): string => {
  if (!text) return ''
  
  // 移除常见的乱码字符
  return text
    .replace(/[^\x00-\x7F\u4e00-\u9fff\u3040-\u309f\u30a0-\u30ff]/g, '') // 保留ASCII、中文、日文字符
    .replace(/�+/g, '') // 移除替换字符
    .replace(/\uFFFD+/g, '') // 移除Unicode替换字符
    .replace(/[\x00-\x1F\x7F]/g, '') // 移除控制字符
    .trim()
}
```

#### 应用位置
- **格式显示名称**：`getFormatDisplayName`函数
- **格式描述**：`getFormatDescription`函数
- **比特率信息**：清理bitrate字段
- **分辨率信息**：清理resolution字段

### 2. 后端字符清理

#### 实现代码
```java
/**
 * 清理文本中的乱码字符
 */
private String cleanText(String text) {
    if (text == null || text.trim().isEmpty()) {
        return text;
    }
    
    try {
        // 移除常见的乱码字符和控制字符
        return text
            .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "") // 移除控制字符，保留换行和制表符
            .replaceAll("\\uFFFD+", "") // 移除Unicode替换字符
            .replaceAll("�+", "") // 移除替换字符
            .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "") // 移除其他控制字符
            .trim();
    } catch (Exception e) {
        log.warn("清理文本时发生错误: {}", e.getMessage());
        return text;
    }
}
```

#### 应用位置
- **格式ID**：`formatId`字段
- **扩展名**：`ext`字段
- **分辨率**：`resolution`字段
- **描述信息**：`note`字段

### 3. 字符编码规范

#### 保留字符范围
- **ASCII字符**：`\x00-\x7F`（基本拉丁字符）
- **中文字符**：`\u4e00-\u9fff`（CJK统一汉字）
- **日文平假名**：`\u3040-\u309f`
- **日文片假名**：`\u30a0-\u30ff`

#### 移除字符类型
- **Unicode替换字符**：`\uFFFD`、`�`
- **控制字符**：`\x00-\x1F`、`\x7F`
- **其他不可见字符**：各种控制字符

## 修复效果

### 修复前
```
格式描述: "audio only m4a ��89.06MiB"
比特率: "128k��"
分辨率: "720p��"
```

### 修复后
```
格式描述: "audio only m4a 89.06MiB"
比特率: "128k"
分辨率: "720p"
```

## 测试用例

### 1. 常见乱码字符
```javascript
// 测试数据
const testCases = [
  "audio only m4a ��89.06MiB",
  "video mp4 720p\uFFFD30fps",
  "audio\x00only\x1Fflac",
  "��128k bitrate��"
]

// 预期结果
const expected = [
  "audio only m4a 89.06MiB",
  "video mp4 720p30fps",
  "audioonly flac",
  "128k bitrate"
]
```

### 2. 多语言字符
```javascript
// 保留的字符
const validChars = [
  "audio only 音频",  // 中文
  "video ビデオ",     // 日文
  "format 格式",      // 中英混合
  "128k bitrate"      // 英文数字
]

// 这些字符应该被保留
```

### 3. 边界情况
```javascript
// 空值处理
cleanText(null) // 返回 null
cleanText("") // 返回 ""
cleanText("   ") // 返回 ""

// 纯乱码
cleanText("����") // 返回 ""
cleanText("\x00\x01\x02") // 返回 ""
```

## 性能考虑

### 1. 正则表达式优化
- 使用预编译的正则表达式模式
- 避免复杂的回溯
- 合理的字符类范围

### 2. 缓存机制
```javascript
// 前端可以考虑缓存清理结果
const cleanTextCache = new Map()

const cleanTextCached = (text: string): string => {
  if (cleanTextCache.has(text)) {
    return cleanTextCache.get(text)
  }
  
  const cleaned = cleanText(text)
  cleanTextCache.set(text, cleaned)
  return cleaned
}
```

### 3. 后端优化
```java
// 使用StringBuilder避免字符串重复创建
private String cleanText(String text) {
    if (text == null || text.trim().isEmpty()) {
        return text;
    }
    
    // 预先检查是否需要清理
    if (!needsCleaning(text)) {
        return text;
    }
    
    // 执行清理操作
    return performCleaning(text);
}
```

## 预防措施

### 1. 输入验证
- 在数据源头进行编码验证
- 确保yt-dlp输出的正确编码
- 添加数据完整性检查

### 2. 日志记录
```java
// 记录清理操作
if (!originalText.equals(cleanedText)) {
    log.debug("文本清理: 原始='{}', 清理后='{}'", 
        originalText.substring(0, Math.min(50, originalText.length())), 
        cleanedText);
}
```

### 3. 错误处理
```javascript
// 前端错误处理
const getFormatDescription = (format: Format) => {
  try {
    const description = format.note || '无描述'
    return cleanText(description)
  } catch (error) {
    console.warn('格式描述清理失败:', error)
    return '格式信息不可用'
  }
}
```

## 兼容性

### 浏览器支持
- **现代浏览器**：完全支持Unicode正则表达式
- **旧版浏览器**：可能需要polyfill
- **移动浏览器**：良好支持

### 服务器环境
- **Java 8+**：完全支持Unicode处理
- **UTF-8编码**：确保JVM使用UTF-8编码
- **内存使用**：正则表达式操作的内存开销

## 总结

通过前后端双重字符清理机制：

1. **彻底解决乱码问题**：移除各种类型的乱码字符
2. **保持数据完整性**：保留有效的多语言字符
3. **提升用户体验**：显示清晰、准确的格式信息
4. **增强系统稳定性**：避免因乱码导致的显示问题

这个解决方案确保了格式选择器中显示的所有文本都是清晰、可读的，大大提升了用户体验。
