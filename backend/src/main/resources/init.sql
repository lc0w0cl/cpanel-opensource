-- 设置字符集
SET NAMES utf8mb4;
SET CHARACTER_SET_CLIENT = utf8mb4;
SET CHARACTER_SET_RESULTS = utf8mb4;
SET CHARACTER_SET_CONNECTION = utf8mb4;

-- 创建分类表
CREATE TABLE IF NOT EXISTS panel_categories (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID，自增主键',
  name VARCHAR(100) NOT NULL COMMENT '分类名称',
  type VARCHAR(20) NOT NULL DEFAULT 'navigation' COMMENT '分类类型：navigation(导航分类)、server(服务器分组)',
  `order` INT NOT NULL DEFAULT 0 COMMENT '分类排序序号',
  created_at VARCHAR(19) COMMENT '创建时间，格式：yyyy-MM-dd HH:mm:ss',
  updated_at VARCHAR(19) COMMENT '更新时间，格式：yyyy-MM-dd HH:mm:ss',
  INDEX idx_order (`order`),
  INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表（支持导航分类和服务器分组）';

-- 创建导航项表
CREATE TABLE IF NOT EXISTS panel_navigation_items (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '导航项ID，自增主键',
  name VARCHAR(100) NOT NULL COMMENT '导航项名称',
  url VARCHAR(500) NOT NULL COMMENT '导航项外部URL',
  logo VARCHAR(255) NOT NULL COMMENT '导航项图标路径',
  category_id INT NOT NULL COMMENT '所属分类ID，外键关联panel_categories表',
  description VARCHAR(255) COMMENT '导航项描述',
  internal_url VARCHAR(500) COMMENT '导航项内部URL',
  sort_order INT DEFAULT 0 COMMENT '排序序号',
  created_at VARCHAR(19) COMMENT '创建时间，格式：yyyy-MM-dd HH:mm:ss',
  updated_at VARCHAR(19) COMMENT '更新时间，格式：yyyy-MM-dd HH:mm:ss',
  INDEX idx_category_id (category_id),
  INDEX idx_name (name),
  INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导航项表';

-- 初始化示例分类数据
INSERT INTO panel_categories (name, type, `order`, created_at, updated_at) VALUES
('常用工具', 'navigation', 1, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('开发工具', 'navigation', 2, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('娱乐休闲', 'navigation', 3, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('生产环境', 'server', 1, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('测试环境', 'server', 2, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('开发环境', 'server', 3, '2023-01-01 00:00:00', '2023-01-01 00:00:00');

-- 初始化示例导航项数据
INSERT INTO panel_navigation_items (name, url, logo, category_id, description, internal_url, sort_order, created_at, updated_at) VALUES
('密码管理器', 'https://vault.bitwarden.com', '/logo/bitwarden-logo.svg', 1, '安全的密码管理工具', 'http://192.168.1.100:8080', 1, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('云服务', 'https://dash.cloudflare.com', '/logo/cloudflare.svg', 1, 'CDN和DNS服务', NULL, 2, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('动态DNS', 'https://ddns.example.com', '/logo/ddns.svg', 1, '动态DNS解析服务', 'http://192.168.1.101:3000', 3, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('AI助手', 'https://chat.deepseek.com', '/logo/deepseek.svg', 1, '智能对话助手', NULL, 4, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('DNS解析', 'https://console.dnspod.cn', '/logo/DNSPod.svg', 1, 'DNS域名解析服务', NULL, 5, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('容器管理', 'https://hub.docker.com', '/logo/docker-official.svg', 2, '容器镜像仓库', 'http://192.168.1.102:9000', 1, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('代理面板', 'http://yacd.haishan.me', '/logo/yacd-128.png', 2, 'Clash代理管理面板', 'http://192.168.1.103:9090/ui', 2, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('在线音乐', 'https://music.163.com', '/logo/ddns.svg', 3, '网易云音乐', NULL, 1, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('视频网站', 'https://www.bilibili.com', '/logo/ddns.svg', 3, '哔哩哔哩视频网站', NULL, 2, '2023-01-01 00:00:00', '2023-01-01 00:00:00');

-- 创建系统配置表
CREATE TABLE IF NOT EXISTS panel_system_config (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID，自增主键',
  config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键名',
  config_value TEXT COMMENT '配置值',
  description VARCHAR(255) COMMENT '配置描述',
  config_type VARCHAR(50) NOT NULL DEFAULT 'system' COMMENT '配置类型：auth(认证配置)、theme(主题配置)、music(音乐配置)、system(系统配置)',
  created_at VARCHAR(19) COMMENT '创建时间，格式：yyyy-MM-dd HH:mm:ss',
  updated_at VARCHAR(19) COMMENT '更新时间，格式：yyyy-MM-dd HH:mm:ss',
  INDEX idx_config_key (config_key),
  INDEX idx_config_type (config_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 创建服务器配置表
CREATE TABLE IF NOT EXISTS panel_servers (
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
  category_id INT COMMENT '所属分类ID，外键关联panel_categories表（type=server）',
  group_name VARCHAR(100) DEFAULT '默认分组' COMMENT '服务器分组（兼容字段，优先使用category_id）',
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
  INDEX idx_group_name (group_name),
  INDEX idx_category_id (category_id),
  FOREIGN KEY (category_id) REFERENCES panel_categories(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务器配置表';

-- 初始化音乐配置数据
INSERT IGNORE INTO panel_system_config (config_key, config_value, description, config_type, created_at, updated_at) VALUES
('music_download_location', 'local', '音乐下载位置设置', 'music', '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('music_server_download_path', 'uploads/music', '音乐服务器下载路径', 'music', '2023-01-01 00:00:00', '2023-01-01 00:00:00');

-- 创建TODO任务表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TODO任务表';
