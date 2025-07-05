-- 数据库迁移脚本：添加音乐配置类型和初始数据
-- 执行时间：2024年

-- 1. 更新配置类型字段注释（如果需要）
ALTER TABLE panel_system_config 
MODIFY COLUMN config_type VARCHAR(50) NOT NULL DEFAULT 'system' 
COMMENT '配置类型：auth(认证配置)、theme(主题配置)、music(音乐配置)、system(系统配置)';

-- 2. 添加音乐配置的初始数据
INSERT IGNORE INTO panel_system_config (config_key, config_value, description, config_type, created_at, updated_at) VALUES
('music_download_location', 'local', '音乐下载位置设置', 'music', NOW(), NOW()),
('music_server_download_path', 'uploads/music', '音乐服务器下载路径', 'music', NOW(), NOW());

-- 3. 验证更新结果
SELECT config_key, config_value, config_type, description 
FROM panel_system_config 
WHERE config_type = 'music'
ORDER BY config_key;
