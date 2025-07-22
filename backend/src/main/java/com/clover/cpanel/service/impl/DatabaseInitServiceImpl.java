package com.clover.cpanel.service.impl;

import com.clover.cpanel.service.DatabaseInitService;
import com.clover.cpanel.service.impl.DatabaseVersionServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

            // 检查并创建2FA表
            if (!checkTwoFactorAuthTableExists()) {
                log.info("2FA表不存在，开始创建...");
                if (createTwoFactorAuthTable()) {
                    log.info("2FA表创建成功");
                } else {
                    log.error("2FA表创建失败");
                }
            } else {
                log.info("2FA表已存在");
            }

            // 检查并更新表结构
            checkAndUpdateTableStructure();

            // 初始化TODO分组数据
            initializeTodoCategories();

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
                  category_id INT COMMENT '所属分组ID，外键关联panel_categories表（type=todo）',
                  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
                  created_at VARCHAR(19) COMMENT '创建时间，格式：yyyy-MM-dd HH:mm:ss',
                  updated_at VARCHAR(19) COMMENT '更新时间，格式：yyyy-MM-dd HH:mm:ss',
                  INDEX idx_sort_order (sort_order),
                  INDEX idx_completed (completed),
                  INDEX idx_created_at (created_at),
                  INDEX idx_category_id (category_id),
                  FOREIGN KEY (category_id) REFERENCES panel_categories(id) ON DELETE SET NULL
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
    public boolean checkTwoFactorAuthTableExists() {
        try {
            String sql = """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                AND table_name = 'panel_two_factor_auth'
                """;

            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("检查2FA表是否存在时发生错误", e);
            return false;
        }
    }

    @Override
    public boolean createTwoFactorAuthTable() {
        try {
            String sql = """
                CREATE TABLE IF NOT EXISTS panel_two_factor_auth (
                  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID，自增主键',
                  user_id VARCHAR(100) NOT NULL COMMENT '用户标识（对应JWT中的subject）',
                  secret_key VARCHAR(255) NOT NULL COMMENT '2FA密钥（Base32编码）',
                  enabled BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否启用2FA',
                  backup_codes TEXT COMMENT '备用恢复码（JSON数组格式）',
                  last_used_code VARCHAR(10) COMMENT '最后使用的验证码（防止重复使用）',
                  last_used_time BIGINT COMMENT '最后使用验证码的时间戳',
                  created_at VARCHAR(19) COMMENT '创建时间，格式：yyyy-MM-dd HH:mm:ss',
                  updated_at VARCHAR(19) COMMENT '更新时间，格式：yyyy-MM-dd HH:mm:ss',
                  UNIQUE KEY uk_user_id (user_id),
                  INDEX idx_enabled (enabled),
                  INDEX idx_created_at (created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='2FA认证配置表'
                """;

            jdbcTemplate.execute(sql);
            log.info("2FA表创建SQL执行成功");
            return true;
        } catch (Exception e) {
            log.error("创建2FA表失败", e);
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

            // 检查并创建服务器表
            if (!checkServerTableExists()) {
                log.info("服务器表不存在，开始创建...");
                if (createServerTable()) {
                    log.info("服务器表创建成功");
                } else {
                    log.error("服务器表创建失败");
                }
            } else {
                log.info("服务器表已存在");
            }

            // 检查并初始化服务器配置
            initializeServerConfig();

            // 迁移旧的服务器配置数据
            migrateServerConfigData();

            // 可以在这里添加其他表结构更新检查

        } catch (Exception e) {
            log.error("检查表结构更新时发生错误", e);
        }
    }

    /**
     * 检查服务器表是否存在
     * @return 是否存在
     */
    private boolean checkServerTableExists() {
        try {
            String sql = """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE()
                AND table_name = 'panel_servers'
                """;

            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("检查服务器表是否存在时发生错误", e);
            return false;
        }
    }

    /**
     * 创建服务器表
     * @return 是否创建成功
     */
    private boolean createServerTable() {
        try {
            String sql = """
                CREATE TABLE panel_servers (
                  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '服务器ID，自增主键',
                  server_name VARCHAR(100) NOT NULL COMMENT '服务器名称',
                  host VARCHAR(255) NOT NULL COMMENT '服务器主机地址',
                  port INT NOT NULL DEFAULT 22 COMMENT '服务器端口',
                  username VARCHAR(100) NOT NULL COMMENT '用户名',
                  auth_type VARCHAR(20) NOT NULL COMMENT '认证类型：password或publickey',
                  password TEXT COMMENT '密码（当认证类型为password时使用）',
                  private_key TEXT COMMENT '私钥内容（当认证类型为publickey时使用）',
                  private_key_password VARCHAR(255) COMMENT '私钥密码',
                  description TEXT COMMENT '服务器描述',
                  icon VARCHAR(100) DEFAULT 'material-symbols:dns' COMMENT '服务器图标（Iconify图标名称）',
                  group_name VARCHAR(100) DEFAULT '默认分组' COMMENT '服务器分组',
                  is_default BOOLEAN DEFAULT FALSE COMMENT '是否为默认服务器',
                  status VARCHAR(20) DEFAULT 'active' COMMENT '服务器状态：active或inactive',
                  sort_order INT DEFAULT 1 COMMENT '排序顺序',
                  created_at VARCHAR(19) COMMENT '创建时间，格式：yyyy-MM-dd HH:mm:ss',
                  updated_at VARCHAR(19) COMMENT '更新时间，格式：yyyy-MM-dd HH:mm:ss',
                  INDEX idx_server_name (server_name),
                  INDEX idx_host (host),
                  INDEX idx_status (status),
                  INDEX idx_sort_order (sort_order),
                  INDEX idx_is_default (is_default),
                  INDEX idx_group_name (group_name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务器配置表'
                """;

            jdbcTemplate.execute(sql);
            log.info("服务器表创建SQL执行成功");
            return true;
        } catch (Exception e) {
            log.error("创建服务器表失败", e);
            return false;
        }
    }

    /**
     * 迁移旧的服务器配置数据
     */
    private void migrateServerConfigData() {
        try {
            // 检查是否已经迁移过
            String checkSql = "SELECT COUNT(*) FROM panel_servers";
            Integer serverCount = jdbcTemplate.queryForObject(checkSql, Integer.class);
            if (serverCount != null && serverCount > 0) {
                log.info("服务器表已有数据，跳过迁移");
                return;
            }

            // 查询旧的服务器配置
            String querySql = """
                SELECT id, config_key, config_value, description
                FROM panel_system_config
                WHERE config_type = 'server' AND config_key LIKE 'server_config_%'
                """;

            List<Map<String, Object>> oldConfigs = jdbcTemplate.queryForList(querySql);

            if (oldConfigs.isEmpty()) {
                log.info("没有找到需要迁移的服务器配置");
                return;
            }

            log.info("开始迁移 {} 个服务器配置", oldConfigs.size());

            int migratedCount = 0;
            for (Map<String, Object> config : oldConfigs) {
                try {
                    String configValue = (String) config.get("config_value");
                    if (configValue != null && !configValue.trim().isEmpty()) {
                        Map<String, Object> serverConfig = parseServerConfigJson(configValue);
                        if (insertServerFromConfig(serverConfig)) {
                            migratedCount++;
                        }
                    }
                } catch (Exception e) {
                    log.warn("迁移服务器配置失败: {}", config.get("config_key"), e);
                }
            }

            log.info("服务器配置迁移完成，成功迁移 {} 个配置", migratedCount);

        } catch (Exception e) {
            log.error("迁移服务器配置数据失败", e);
        }
    }

    /**
     * 解析服务器配置JSON
     */
    private Map<String, Object> parseServerConfigJson(String configJson) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 移除大括号
            String content = configJson.trim();
            if (content.startsWith("{") && content.endsWith("}")) {
                content = content.substring(1, content.length() - 1);
            }

            // 分割键值对
            String[] pairs = content.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replaceAll("\"", "");
                    String value = keyValue[1].trim().replaceAll("\"", "");

                    // 尝试转换数字
                    if ("port".equals(key)) {
                        try {
                            result.put(key, Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            result.put(key, value);
                        }
                    } else {
                        result.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析服务器配置JSON失败", e);
        }
        return result;
    }

    /**
     * 从配置Map插入服务器记录
     */
    private boolean insertServerFromConfig(Map<String, Object> config) {
        try {
            String serverName = (String) config.get("serverName");
            String host = (String) config.get("host");
            Object portObj = config.get("port");
            Integer port = portObj instanceof Integer ? (Integer) portObj : 22;
            String username = (String) config.get("username");
            String authType = (String) config.get("authType");
            String password = (String) config.get("password");
            String privateKey = (String) config.get("privateKey");
            String privateKeyPassword = (String) config.get("privateKeyPassword");
            String description = (String) config.get("description");

            // 根据服务器名称和描述推断图标
            String icon = getServerIconByLocation(serverName, description);

            String insertSql = """
                INSERT INTO panel_servers (
                    server_name, host, port, username, auth_type,
                    password, private_key, private_key_password,
                    description, icon, group_name, is_default, status, sort_order,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                """;

            // 根据服务器名称推断分组
            String groupName = inferGroupFromServerName(serverName, description);

            int result = jdbcTemplate.update(insertSql,
                    serverName, host, port, username, authType,
                    password, privateKey, privateKeyPassword,
                    description, icon, groupName, false, "active", 1);

            return result > 0;
        } catch (Exception e) {
            log.error("插入服务器配置失败", e);
            return false;
        }
    }

    /**
     * 根据服务器名称和描述推断图标
     */
    private String getServerIconByLocation(String serverName, String description) {
        if (serverName == null) serverName = "";
        if (description == null) description = "";

        String name = serverName.toLowerCase();
        String desc = description.toLowerCase();

        // 根据服务器名称推断
        if (name.contains("工控") || name.contains("本地") || name.contains("factory")) {
            return "material-symbols:factory";
        }
        if (name.contains("西雅图") || name.contains("seattle") || desc.contains("us") || desc.contains("美国")) {
            return "flagpack:us";
        }
        if (name.contains("首尔") || name.contains("seoul") || name.contains("韩国") || desc.contains("kr")) {
            return "flagpack:kr";
        }
        if (name.contains("上海") || name.contains("广州") || name.contains("北京") || name.contains("深圳") ||
            name.contains("中国") || desc.contains("cn") || desc.contains("腾讯") || desc.contains("阿里")) {
            return "flagpack:cn";
        }
        if (name.contains("东京") || name.contains("大阪") || name.contains("日本") || desc.contains("jp")) {
            return "flagpack:jp";
        }
        if (name.contains("德国") || name.contains("柏林") || desc.contains("de") || desc.contains("德")) {
            return "flagpack:de";
        }
        if (name.contains("英国") || name.contains("伦敦") || desc.contains("gb") || desc.contains("uk")) {
            return "flagpack:gb";
        }
        if (name.contains("法国") || name.contains("巴黎") || desc.contains("fr")) {
            return "flagpack:fr";
        }
        if (name.contains("新加坡") || desc.contains("sg") || desc.contains("singapore")) {
            return "flagpack:sg";
        }
        if (name.contains("香港") || desc.contains("hk") || desc.contains("hong kong")) {
            return "flagpack:hk";
        }

        // 默认图标
        return "material-symbols:dns";
    }

    /**
     * 根据服务器名称和描述推断分组
     */
    private String inferGroupFromServerName(String serverName, String description) {
        if (serverName == null) serverName = "";
        if (description == null) description = "";

        String name = serverName.toLowerCase();
        String desc = description.toLowerCase();

        // 根据服务器名称和描述推断分组
        if (name.contains("工控") || name.contains("本地") || name.contains("factory") || name.contains("内网")) {
            return "内网服务器";
        }
        if (name.contains("生产") || name.contains("prod") || name.contains("production")) {
            return "生产环境";
        }
        if (name.contains("测试") || name.contains("test") || name.contains("staging")) {
            return "测试环境";
        }
        if (name.contains("开发") || name.contains("dev") || name.contains("development")) {
            return "开发环境";
        }
        if (name.contains("数据库") || name.contains("database") || name.contains("db") || name.contains("mysql") || name.contains("redis")) {
            return "数据库服务器";
        }
        if (name.contains("web") || name.contains("nginx") || name.contains("apache") || name.contains("前端")) {
            return "Web服务器";
        }
        if (name.contains("api") || name.contains("后端") || name.contains("backend")) {
            return "API服务器";
        }
        if (desc.contains("us") || desc.contains("美国") || name.contains("西雅图") || name.contains("seattle")) {
            return "美国服务器";
        }
        if (desc.contains("cn") || desc.contains("中国") || name.contains("上海") || name.contains("广州") || name.contains("北京")) {
            return "中国服务器";
        }
        if (desc.contains("kr") || desc.contains("韩国") || name.contains("首尔") || name.contains("seoul")) {
            return "韩国服务器";
        }
        if (desc.contains("jp") || desc.contains("日本") || name.contains("东京") || name.contains("大阪")) {
            return "日本服务器";
        }
        if (desc.contains("hk") || desc.contains("香港") || name.contains("香港")) {
            return "香港服务器";
        }
        if (desc.contains("sg") || desc.contains("新加坡") || name.contains("新加坡")) {
            return "新加坡服务器";
        }

        // 默认分组
        return "默认分组";
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
     * 初始化服务器配置
     */
    private void initializeServerConfig() {
        try {
            log.info("检查服务器配置初始化状态...");

            // 检查默认服务器配置是否存在
            if (!checkConfigExists("default_server_id")) {
                log.info("默认服务器配置不存在，开始初始化...");
                String insertSql = """
                    INSERT INTO panel_system_config (config_key, config_value, description, config_type, created_at, updated_at)
                    VALUES ('default_server_id', '', '默认服务器ID', 'server', NOW(), NOW())
                    """;
                jdbcTemplate.update(insertSql);
                log.info("默认服务器配置初始化完成");
            }

            log.info("服务器配置检查完成");
        } catch (Exception e) {
            log.error("初始化服务器配置失败", e);
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

    @Override
    public void initializeTodoCategories() {
        try {
            log.info("检查TODO分组初始化状态...");

            // 检查是否已存在TODO分组
            String checkSql = "SELECT COUNT(*) FROM panel_categories WHERE type = 'todo'";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);

            if (count == null || count == 0) {
                log.info("TODO分组不存在，开始初始化...");

                // 插入默认的TODO分组
                String insertSql = """
                    INSERT INTO panel_categories (name, type, `order`, created_at, updated_at) VALUES
                    ('工作任务', 'todo', 1, NOW(), NOW()),
                    ('个人事务', 'todo', 2, NOW(), NOW())
                    """;

                jdbcTemplate.update(insertSql);
                log.info("TODO分组初始化完成");
            } else {
                log.info("TODO分组已存在，跳过初始化");
            }
        } catch (Exception e) {
            log.error("初始化TODO分组失败", e);
        }
    }
}
