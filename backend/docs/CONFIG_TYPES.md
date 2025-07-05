# 系统配置类型说明

## 概述

系统配置表 `panel_system_config` 现在支持配置类型分类，通过 `config_type` 字段来区分不同类型的配置项。

## 配置类型

### 1. 认证配置 (auth)
用于存储与用户认证相关的配置项。

**示例配置项：**
- `login_password` - 面板登录密码

**使用方式：**
```java
systemConfigService.setConfigValue("login_password", hashedPassword, "面板登录密码", ConfigType.AUTH);
```

### 2. 主题配置 (theme)
用于存储与界面主题、外观相关的配置项。

**示例配置项：**
- `wallpaper_url` - 自定义壁纸URL
- `wallpaper_blur` - 壁纸模糊度
- `wallpaper_mask` - 壁纸遮罩透明度

**使用方式：**
```java
systemConfigService.setConfigValue("wallpaper_url", "/uploads/wallpaper.jpg", "自定义壁纸URL", ConfigType.THEME);
```

### 3. 音乐配置 (music)
用于存储与音乐播放器、下载相关的配置项。

**示例配置项：**
- `music_download_location` - 音乐下载位置（local/server）
- `music_server_download_path` - 服务器下载路径

**使用方式：**
```java
systemConfigService.setConfigValue("music_download_location", "server", "音乐下载位置设置", ConfigType.MUSIC);
```

### 4. 系统配置 (system)
用于存储其他系统级别的配置项（默认类型）。

**使用方式：**
```java
// 使用默认类型
systemConfigService.setConfigValue("system_setting", "value", "系统设置");

// 或明确指定类型
systemConfigService.setConfigValue("system_setting", "value", "系统设置", ConfigType.SYSTEM);
```

## API 接口

### 根据类型获取配置列表
```http
GET /api/system-config/configs/type/{type}
```

**参数：**
- `type`: 配置类型 (auth, theme, music, system)

**响应示例：**
```json
{
  "code": 200,
  "success": true,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "configKey": "wallpaper_url",
      "configValue": "/uploads/wallpaper.jpg",
      "description": "自定义壁纸URL",
      "configType": "theme",
      "createdAt": "2024-01-01 00:00:00",
      "updatedAt": "2024-01-01 00:00:00"
    }
  ]
}
```

### 设置配置值（支持类型）
```http
POST /api/system-config/config/{key}
```

**参数：**
- `key`: 配置键名
- `value`: 配置值
- `description`: 配置描述（可选）
- `type`: 配置类型（可选，默认为 system）

## 数据库迁移

如果您有现有的数据库，请执行以下迁移脚本：

```sql
-- 添加配置类型字段
ALTER TABLE panel_system_config
ADD COLUMN IF NOT EXISTS config_type VARCHAR(50) NOT NULL DEFAULT 'system'
COMMENT '配置类型：auth(认证配置)、theme(主题配置)、music(音乐配置)、system(系统配置)';

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_config_type ON panel_system_config (config_type);

-- 更新现有数据
UPDATE panel_system_config SET config_type = 'auth' WHERE config_key = 'login_password';
UPDATE panel_system_config SET config_type = 'theme' WHERE config_key IN ('wallpaper_url', 'wallpaper_blur', 'wallpaper_mask');
UPDATE panel_system_config SET config_type = 'music' WHERE config_key IN ('music_download_location', 'music_server_download_path');
```

## 最佳实践

1. **使用常量类**：始终使用 `ConfigType` 常量类中定义的常量，避免硬编码字符串。

2. **合理分类**：
   - 认证相关的配置使用 `ConfigType.AUTH`
   - 界面主题相关的配置使用 `ConfigType.THEME`
   - 音乐播放器相关的配置使用 `ConfigType.MUSIC`
   - 其他系统配置使用 `ConfigType.SYSTEM`

3. **查询优化**：利用配置类型进行分类查询，提高查询效率。

4. **前端集成**：前端可以根据配置类型来组织设置界面，提供更好的用户体验。

## 注意事项

- 配置类型字段有数据库索引，查询性能良好
- 默认配置类型为 `system`，确保向后兼容性
- 配置类型一旦设置，建议不要随意更改，以保持数据一致性
