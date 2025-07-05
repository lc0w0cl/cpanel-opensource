# 数据库自动升级机制

## 概述

本系统实现了完全自动化的数据库升级机制，无需手动执行任何SQL脚本。应用启动时会自动检测数据库版本并执行必要的升级操作。

## 工作原理

### 1. 版本检测
- 应用启动时，`DatabaseVersionService` 会检查当前数据库版本
- 版本信息存储在 `panel_system_config` 表的 `database_version` 配置项中
- 如果没有版本信息，默认为 `1.0.0`

### 2. 自动升级
- 比较当前版本与目标版本（当前为 `1.2.0`）
- 如果需要升级，自动执行相应的升级逻辑
- 升级完成后更新版本号

### 3. 升级内容
- **1.0.0 → 1.1.0**: 添加TODO功能相关表
- **1.1.0 → 1.2.0**: 添加音乐配置功能
- **1.0.0 → 1.2.0**: 直接升级，包含所有功能

## 核心组件

### ApplicationStartupListener
```java
@Component
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent>
```
- 应用启动完成后触发
- 调用 `DatabaseInitService.initializeTables()`

### DatabaseVersionService
```java
@Service
public class DatabaseVersionServiceImpl implements DatabaseVersionService
```
- 管理数据库版本
- 执行版本升级逻辑
- 当前支持的版本：1.0.0, 1.1.0, 1.2.0

### DatabaseInitService
```java
@Service
public class DatabaseInitServiceImpl implements DatabaseInitService
```
- 检查和创建数据库表
- 初始化配置数据
- 更新表结构

## 音乐配置自动初始化

### 新增配置项
1. **music_download_location**
   - 默认值: `local`
   - 描述: 音乐下载位置设置
   - 类型: `music`

2. **music_server_download_path**
   - 默认值: `uploads/music`
   - 描述: 音乐服务器下载路径
   - 类型: `music`

### 初始化逻辑
```java
private void initializeMusicConfig() {
    // 检查配置是否存在
    if (!checkConfigExists("music_download_location")) {
        // 自动插入默认配置
    }
}
```

## 使用方式

### 全新部署
1. 启动应用
2. 系统自动创建所有表和配置
3. 数据库版本设置为最新版本

### 现有系统升级
1. 更新应用代码
2. 重启应用
3. 系统自动检测版本差异
4. 执行必要的升级操作
5. 无需手动干预

## 安全特性

### 幂等性
- 所有升级操作都是幂等的
- 重复执行不会产生副作用
- 使用 `INSERT IGNORE` 和 `IF NOT EXISTS` 确保安全

### 错误处理
- 升级失败不会影响应用启动
- 详细的错误日志记录
- 支持部分升级成功的情况

### 向后兼容
- 新版本完全兼容旧数据
- 不会删除或修改现有数据
- 只添加新的配置和表结构

## 监控和日志

### 启动日志示例
```
INFO  - === 应用启动完成，开始执行初始化操作 ===
INFO  - 正在执行数据库初始化...
INFO  - 检测到数据库需要升级: 1.1.0 -> 1.2.0
INFO  - 执行1.1.0到1.2.0的数据库升级...
INFO  - 检查音乐配置初始化状态...
INFO  - 音乐下载位置配置不存在，开始初始化...
INFO  - 音乐下载位置配置初始化完成
INFO  - 数据库升级完成: 1.1.0 -> 1.2.0
INFO  - 🎉 CPanel应用启动成功！
INFO  - 🎵 音乐设置功能已就绪，支持本地和服务器下载配置
```

## 测试升级

### 模拟旧版本
```sql
-- 删除音乐配置，模拟1.1.0版本
DELETE FROM panel_system_config 
WHERE config_key IN ('music_download_location', 'music_server_download_path');

-- 设置版本为1.1.0
UPDATE panel_system_config 
SET config_value = '1.1.0' 
WHERE config_key = 'database_version';
```

### 验证升级结果
```sql
-- 检查音乐配置是否自动添加
SELECT * FROM panel_system_config 
WHERE config_type = 'music';

-- 检查版本是否更新
SELECT config_value FROM panel_system_config 
WHERE config_key = 'database_version';
```

## 注意事项

1. **备份数据**: 虽然升级是安全的，但建议在生产环境升级前备份数据
2. **日志监控**: 关注启动日志，确认升级成功
3. **配置验证**: 升级后可以通过设置页面验证音乐配置是否正常
4. **版本一致性**: 确保所有实例使用相同的应用版本

## 故障排除

### 常见问题
1. **升级失败**: 检查数据库连接和权限
2. **配置缺失**: 手动执行 `initializeMusicConfig()` 逻辑
3. **版本不一致**: 手动更新 `database_version` 配置

### 手动修复
如果自动升级失败，可以手动执行：
```sql
-- 手动添加音乐配置
INSERT IGNORE INTO panel_system_config 
(config_key, config_value, description, config_type, created_at, updated_at) 
VALUES 
('music_download_location', 'local', '音乐下载位置设置', 'music', NOW(), NOW()),
('music_server_download_path', 'uploads/music', '音乐服务器下载路径', 'music', NOW(), NOW());

-- 更新版本号
UPDATE panel_system_config 
SET config_value = '1.2.0' 
WHERE config_key = 'database_version';
```
