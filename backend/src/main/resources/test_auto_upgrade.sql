-- 测试自动升级功能的SQL脚本
-- 此脚本用于模拟不同版本的数据库状态，测试自动升级是否正常工作

-- 1. 模拟1.0.0版本的数据库状态（删除音乐配置和版本信息）
DELETE FROM panel_system_config WHERE config_key IN ('music_download_location', 'music_server_download_path', 'database_version');

-- 2. 查看当前配置状态
SELECT 'Before Auto Upgrade:' as status;
SELECT config_key, config_value, config_type, description 
FROM panel_system_config 
ORDER BY config_type, config_key;

-- 3. 重启应用后，应该会自动执行以下操作：
--    - 检测数据库版本（如果没有版本信息，默认为1.0.0）
--    - 发现需要升级到1.2.0
--    - 自动添加音乐配置
--    - 更新数据库版本为1.2.0

-- 4. 验证升级结果的查询（应用重启后手动执行）
/*
SELECT 'After Auto Upgrade:' as status;
SELECT config_key, config_value, config_type, description 
FROM panel_system_config 
WHERE config_type = 'music' OR config_key = 'database_version'
ORDER BY config_key;
*/
