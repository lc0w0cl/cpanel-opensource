# 背景图片目录功能

## 概述

为了更好地组织上传的文件，系统现在支持将背景图片/壁纸文件存储在专门的 `uploads/backgrounds/` 子目录中。

## 目录结构

```
uploads/
├── backgrounds/              # 背景图片专用目录
│   ├── README.md            # 目录说明文件
│   ├── test_background.png  # 测试图片
│   └── wallpaper_*.png      # 用户上传的背景图片
├── *.png/jpg/...            # 导航项图标文件
└── ...                      # 其他上传文件
```

## 功能特点

### 1. 自动目录创建
- 系统会在首次上传背景图片时自动创建 `backgrounds` 目录
- 无需手动创建或维护目录结构

### 2. 文件分类存储
- **背景图片**: 存储在 `uploads/backgrounds/` 目录
- **导航图标**: 存储在 `uploads/` 根目录
- **其他文件**: 根据类型存储在相应目录

### 3. URL路径支持
- 背景图片访问路径: `/uploads/backgrounds/文件名`
- 系统自动处理子目录路径的URL生成和访问

## 技术实现

### 后端实现

#### 1. 新增接口方法
```java
// FileUploadService.java
String uploadFileToSubDirectory(MultipartFile file, String subDirectory, String fileName);
```

#### 2. 实现类更新
```java
// FileUploadServiceImpl.java
@Override
public String uploadFileToSubDirectory(MultipartFile file, String subDirectory, String fileName) {
    // 确保子目录存在
    File subDir = new File(uploadDir, subDirectory);
    if (!subDir.exists()) {
        subDir.mkdirs();
    }
    
    // 保存文件到子目录
    Path filePath = Paths.get(subDir.getAbsolutePath(), uniqueFileName);
    Files.copy(file.getInputStream(), filePath);
    
    // 返回包含子目录的URL
    return urlPrefix + subDirectory + "/" + uniqueFileName;
}
```

#### 3. 控制器更新
```java
// SystemConfigController.java
// 使用新方法上传背景图片
String wallpaperUrl = fileUploadService.uploadFileToSubDirectory(file, "backgrounds", "wallpaper");
```

### 前端支持

#### 1. URL处理
前端的 `getImageUrl()` 工具函数已支持子目录路径：
```typescript
// 自动处理 /uploads/backgrounds/ 路径
export function getImageUrl(logoPath: string): string {
  if (!logoPath || !logoPath.startsWith('/uploads/')) {
    return logoPath;
  }
  // 开发环境加上API前缀，生产环境直接使用
  return config.public.isDevelopment ? `${config.public.apiBaseUrl}${logoPath}` : logoPath;
}
```

#### 2. 上传功能
主题设置页面的壁纸上传功能会自动使用新的子目录结构。

## 使用示例

### 1. 上传背景图片
```bash
POST /api/system-config/wallpaper/upload
Content-Type: multipart/form-data

file: [图片文件]
blur: 5
mask: 30
```

**响应示例:**
```json
{
  "success": true,
  "data": {
    "wallpaperUrl": "/uploads/backgrounds/wallpaper_20250628_133409_9ff7a345.png",
    "wallpaperBlur": 5,
    "wallpaperMask": 30
  }
}
```

### 2. 访问背景图片
- **开发环境**: `http://localhost:8080/uploads/backgrounds/wallpaper_20250628_133409_9ff7a345.png`
- **生产环境**: `/uploads/backgrounds/wallpaper_20250628_133409_9ff7a345.png`

## 兼容性

### 向后兼容
- 现有的根目录文件仍然可以正常访问
- 旧的壁纸URL路径继续有效
- 不影响现有的导航图标上传功能

### 渐进式迁移
- 新上传的背景图片自动使用子目录
- 旧文件可以保持原位置或手动迁移
- 系统同时支持两种路径格式

## 测试验证

### 1. 功能测试
使用测试页面 `frontend/test-backgrounds.html` 验证：
- 子目录图片访问
- API路径访问
- 文件上传功能

### 2. 目录测试
```bash
# 验证目录结构
ls -la uploads/backgrounds/

# 测试文件访问
curl http://localhost:8080/uploads/backgrounds/test_background.png
```

## 注意事项

1. **权限设置**: 确保Web服务器对 `uploads/backgrounds/` 目录有读写权限
2. **文件清理**: 定期清理未使用的背景图片文件
3. **备份策略**: 将子目录包含在备份策略中
4. **监控空间**: 监控子目录的磁盘使用情况

## 未来扩展

可以考虑为其他文件类型创建专门的子目录：
- `uploads/avatars/` - 用户头像
- `uploads/documents/` - 文档文件
- `uploads/temp/` - 临时文件

这种结构化的文件组织方式有助于：
- 提高文件管理效率
- 简化备份和清理操作
- 增强系统的可维护性
