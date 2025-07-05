# 音乐下载设置功能说明

## 功能概述

音乐下载设置功能允许用户配置音乐文件的下载方式，支持两种下载模式：
- **本地下载**：音乐文件下载到用户浏览器的默认下载目录
- **服务器下载**：音乐文件保存到服务器指定目录，可通过URL访问

## 使用方法

### 1. 访问设置页面

1. 登录系统后，进入 **设置** 页面
2. 找到 **音乐设置** 区块
3. 点击展开音乐设置选项

### 2. 配置下载方式

#### 本地下载模式
- 选择 **"下载到本地"** 选项
- 音乐文件将下载到浏览器默认下载目录
- 适合个人使用，文件直接保存在用户设备上

#### 服务器下载模式
- 选择 **"下载到服务器"** 选项
- 配置服务器下载路径（默认：`uploads/music`）
- 音乐文件将保存到服务器指定目录
- 适合团队共享，文件可通过URL访问

### 3. 下载音乐

1. 在音乐页面搜索想要的音乐
2. 点击 **下载** 按钮
3. 系统会根据设置自动选择下载方式：
   - **本地下载**：浏览器会提示保存文件位置
   - **服务器下载**：文件保存到服务器，显示"服务器下载完成"提示

## 技术实现

### 前端实现

#### 设置界面
- 位置：`frontend/pages/home/settings.vue`
- 功能：提供音乐下载设置的用户界面
- 特性：
  - 单选按钮选择下载方式
  - 服务器路径配置输入框
  - 实时保存设置

#### 下载逻辑
- 位置：`frontend/composables/useMusicApi.ts`
- 功能：智能下载音乐，根据设置选择下载方式
- 流程：
  1. 获取用户音乐设置
  2. 根据设置调用对应的下载方法
  3. 显示相应的成功/失败提示

### 后端实现

#### 配置管理
- 控制器：`SystemConfigController.java`
- API接口：
  - `GET /api/system-config/music` - 获取音乐设置
  - `POST /api/system-config/music` - 保存音乐设置

#### 服务器下载
- 控制器：`MusicController.java`
- API接口：`POST /api/music/download-to-server`
- 功能：
  1. 获取音频流URL
  2. 下载音频文件到服务器
  3. 返回文件信息和访问URL

#### 静态资源访问
- 配置：`WebConfig.java`
- 映射：`/uploads/**` → `file:./uploads/`
- 功能：提供下载文件的HTTP访问

### 数据库配置

#### 配置项
```sql
-- 音乐下载位置设置
INSERT INTO panel_system_config 
(config_key, config_value, description, config_type) 
VALUES 
('music_download_location', 'local', '音乐下载位置设置', 'music');

-- 音乐服务器下载路径
INSERT INTO panel_system_config 
(config_key, config_value, description, config_type) 
VALUES 
('music_server_download_path', 'uploads/music', '音乐服务器下载路径', 'music');
```

#### 自动初始化
- 服务：`DatabaseInitServiceImpl.java`
- 功能：应用启动时自动检查并初始化音乐配置
- 特性：幂等操作，重复执行安全

## 文件组织

### 服务器下载文件结构
```
uploads/
└── music/
    ├── 周杰伦 - 稻香.mp3
    ├── 邓紫棋 - 光年之外.m4a
    └── ...
```

### 文件命名规则
- 格式：`艺术家 - 歌曲名.扩展名`
- 清理非法字符：`< > : " / \ | ? *`
- 支持扩展名：`.mp3`, `.m4a`, `.webm`, `.ogg`

### 文件访问
- 本地访问：`http://localhost:8080/uploads/music/周杰伦 - 稻香.mp3`
- 生产环境：`https://yourdomain.com/uploads/music/周杰伦 - 稻香.mp3`

## 配置选项

### 下载位置
- **local**：下载到本地（默认）
- **server**：下载到服务器

### 服务器路径
- 默认：`uploads/music`
- 可自定义：相对于应用根目录的路径
- 示例：`files/audio`, `media/downloads`

## 注意事项

1. **权限要求**：服务器下载需要应用对目标目录有写权限
2. **存储空间**：服务器下载会占用服务器存储空间
3. **网络带宽**：服务器下载会消耗服务器网络带宽
4. **文件管理**：服务器下载的文件需要定期清理管理
5. **安全考虑**：确保下载目录不在Web根目录下的敏感位置

## 故障排除

### 常见问题

1. **设置不生效**
   - 检查数据库连接
   - 确认配置已保存
   - 刷新页面重试

2. **服务器下载失败**
   - 检查目录权限
   - 确认磁盘空间充足
   - 查看后端日志

3. **文件无法访问**
   - 检查静态资源配置
   - 确认文件路径正确
   - 验证Web服务器配置

### 日志查看
```bash
# 查看应用日志
tail -f logs/application.log

# 搜索音乐下载相关日志
grep "音乐下载\|music download" logs/application.log
```

## 更新历史

- **v1.2.0**：新增音乐下载设置功能
  - 支持本地和服务器下载模式
  - 自动数据库配置初始化
  - 智能下载方式选择
