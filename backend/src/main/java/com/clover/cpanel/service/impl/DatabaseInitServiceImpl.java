package com.clover.cpanel.service.impl;

import com.clover.cpanel.service.DatabaseInitService;
import com.clover.cpanel.service.impl.DatabaseVersionServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 数据库初始化服务实现类
 */
@Slf4j
@Service
public class DatabaseInitServiceImpl implements DatabaseInitService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DatabaseVersionServiceImpl databaseVersionService;

    @Override
    public void initializeTables() {
        log.info("开始检查数据库表结构...");

        try {
            // 首先检查并执行版本升级
            databaseVersionService.checkAndUpgrade();

            // 检查并创建TODO表
            if (!checkTodoTableExists()) {
                log.info("TODO表不存在，开始创建...");
                if (createTodoTable()) {
                    log.info("TODO表创建成功");
                } else {
                    log.error("TODO表创建失败");
                }
            } else {
                log.info("TODO表已存在");
            }

            // 检查并更新表结构
            checkAndUpdateTableStructure();

            log.info("数据库表结构检查完成");
        } catch (Exception e) {
            log.error("数据库初始化失败", e);
        }
    }

    @Override
    public boolean checkTodoTableExists() {
        try {
            String sql = """
                SELECT COUNT(*) 
                FROM information_schema.tables 
                WHERE table_schema = DATABASE() 
                AND table_name = 'panel_todos'
                """;
            
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("检查TODO表是否存在时发生错误", e);
            return false;
        }
    }

    @Override
    public boolean createTodoTable() {
        try {
            String sql = """
                CREATE TABLE IF NOT EXISTS panel_todos (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '任务ID，自增主键',
                  text VARCHAR(500) NOT NULL COMMENT '任务内容',
                  completed BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已完成',
                  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
                  created_at VARCHAR(19) COMMENT '创建时间，格式：yyyy-MM-dd HH:mm:ss',
                  updated_at VARCHAR(19) COMMENT '更新时间，格式：yyyy-MM-dd HH:mm:ss',
                  INDEX idx_sort_order (sort_order),
                  INDEX idx_completed (completed),
                  INDEX idx_created_at (created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TODO任务表'
                """;
            
            jdbcTemplate.execute(sql);
            log.info("TODO表创建SQL执行成功");
            return true;
        } catch (Exception e) {
            log.error("创建TODO表失败", e);
            return false;
        }
    }

    @Override
    public void checkAndUpdateTableStructure() {
        try {
            // 检查系统配置表是否有config_type字段
            if (!checkColumnExists("panel_system_config", "config_type")) {
                log.info("系统配置表缺少config_type字段，开始添加...");
                addConfigTypeColumn();
            }

            // 检查并初始化音乐配置
            initializeMusicConfig();

            // 可以在这里添加其他表结构更新检查

        } catch (Exception e) {
            log.error("检查表结构更新时发生错误", e);
        }
    }

    /**
     * 检查指定表的指定列是否存在
     */
    private boolean checkColumnExists(String tableName, String columnName) {
        try {
            String sql = """
                SELECT COUNT(*) 
                FROM information_schema.columns 
                WHERE table_schema = DATABASE() 
                AND table_name = ? 
                AND column_name = ?
                """;
            
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, columnName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("检查列是否存在时发生错误: table={}, column={}", tableName, columnName, e);
            return false;
        }
    }

    /**
     * 为系统配置表添加config_type字段
     */
    private void addConfigTypeColumn() {
        try {
            String sql = """
                ALTER TABLE panel_system_config 
                ADD COLUMN IF NOT EXISTS config_type VARCHAR(50) NOT NULL DEFAULT 'system' 
                COMMENT '配置类型：auth(认证配置)、theme(主题配置)、system(系统配置)'
                """;
            
            jdbcTemplate.execute(sql);
            
            // 添加索引
            String indexSql = "CREATE INDEX IF NOT EXISTS idx_config_type ON panel_system_config (config_type)";
            jdbcTemplate.execute(indexSql);
            
            // 更新现有数据
            updateExistingConfigTypes();
            
            log.info("系统配置表config_type字段添加成功");
        } catch (Exception e) {
            log.error("添加config_type字段失败", e);
        }
    }

    /**
     * 更新现有配置数据的类型
     */
    private void updateExistingConfigTypes() {
        try {
            // 更新登录密码配置为认证类型
            String authSql = "UPDATE panel_system_config SET config_type = 'auth' WHERE config_key = 'login_password'";
            jdbcTemplate.update(authSql);

            // 更新壁纸相关配置为主题类型
            String themeSql = """
                UPDATE panel_system_config
                SET config_type = 'theme'
                WHERE config_key IN ('wallpaper_url', 'wallpaper_blur', 'wallpaper_mask', 'logo_url', 'content_margin', 'content_padding')
                """;
            jdbcTemplate.update(themeSql);

            // 更新音乐相关配置为音乐类型
            String musicSql = """
                UPDATE panel_system_config
                SET config_type = 'music'
                WHERE config_key IN ('music_download_location', 'music_server_download_path', 'bilibili_cookie', 'youtube_cookie')
                """;
            jdbcTemplate.update(musicSql);

            log.info("现有配置数据类型更新完成");
        } catch (Exception e) {
            log.error("更新现有配置数据类型失败", e);
        }
    }

    /**
     * 初始化音乐配置
     */
    private void initializeMusicConfig() {
        try {
            log.info("检查音乐配置初始化状态...");

            // 检查音乐下载位置配置是否存在
            if (!checkConfigExists("music_download_location")) {
                log.info("音乐下载位置配置不存在，开始初始化...");
                String insertSql = """
                    INSERT INTO panel_system_config (config_key, config_value, description, config_type, created_at, updated_at)
                    VALUES ('music_download_location', 'local', '音乐下载位置设置', 'music', NOW(), NOW())
                    """;
                jdbcTemplate.update(insertSql);
                log.info("音乐下载位置配置初始化完成");
            }

            // 检查音乐服务器下载路径配置是否存在
            if (!checkConfigExists("music_server_download_path")) {
                log.info("音乐服务器下载路径配置不存在，开始初始化...");
                String insertSql = """
                    INSERT INTO panel_system_config (config_key, config_value, description, config_type, created_at, updated_at)
                    VALUES ('music_server_download_path', 'uploads/music', '音乐服务器下载路径', 'music', NOW(), NOW())
                    """;
                jdbcTemplate.update(insertSql);
                log.info("音乐服务器下载路径配置初始化完成");
            }

            // 检查Bilibili cookie配置是否存在
            if (!checkConfigExists("bilibili_cookie")) {
                log.info("Bilibili cookie配置不存在，开始初始化...");
                String insertSql = """
                    INSERT INTO panel_system_config (config_key, config_value, description, config_type, created_at, updated_at)
                    VALUES ('bilibili_cookie', '', 'Bilibili平台cookie配置', 'music', NOW(), NOW())
                    """;
                jdbcTemplate.update(insertSql);
                log.info("Bilibili cookie配置初始化完成");
            }

            // 检查YouTube cookie配置是否存在
            if (!checkConfigExists("youtube_cookie")) {
                log.info("YouTube cookie配置不存在，开始初始化...");
                String insertSql = """
                    INSERT INTO panel_system_config (config_key, config_value, description, config_type, created_at, updated_at)
                    VALUES ('youtube_cookie', '', 'YouTube平台cookie配置', 'music', NOW(), NOW())
                    """;
                jdbcTemplate.update(insertSql);
                log.info("YouTube cookie配置初始化完成");
            }

            log.info("音乐配置检查完成");
        } catch (Exception e) {
            log.error("初始化音乐配置失败", e);
        }
    }

    /**
     * 检查指定配置是否存在
     * @param configKey 配置键名
     * @return 是否存在
     */
    private boolean checkConfigExists(String configKey) {
        try {
            String sql = "SELECT COUNT(*) FROM panel_system_config WHERE config_key = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, configKey);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("检查配置是否存在时发生错误: {}", configKey, e);
            return false;
        }
    }
}
