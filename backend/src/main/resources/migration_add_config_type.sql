-- 数据库迁移脚本：为系统配置表添加配置类型字段
-- 执行时间：2024年

-- 1. 为现有表添加 config_type 字段（如果不存在）
ALTER TABLE panel_system_config 
ADD COLUMN IF NOT EXISTS config_type VARCHAR(50) NOT NULL DEFAULT 'system' 
COMMENT '配置类型：auth(认证配置)、theme(主题配置)、system(系统配置)';

-- 2. 添加索引（如果不存在）
CREATE INDEX IF NOT EXISTS idx_config_type ON panel_system_config (config_type);

-- 3. 更新现有数据的配置类型
-- 更新登录密码相关配置为认证类型
UPDATE panel_system_config 
SET config_type = 'auth' 
WHERE config_key = 'login_password';

-- 更新壁纸相关配置为主题类型
UPDATE panel_system_config 
SET config_type = 'theme' 
WHERE config_key IN ('wallpaper_url', 'wallpaper_blur', 'wallpaper_mask');

-- 其他配置保持默认的 system 类型

-- 4. 验证更新结果
SELECT config_key, config_type, description 
FROM panel_system_config 
ORDER BY config_type, config_key;
