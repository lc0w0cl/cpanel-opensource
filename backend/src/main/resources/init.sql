-- 创建分类表
CREATE TABLE IF NOT EXISTS panel_categories (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID，自增主键',
  name VARCHAR(100) NOT NULL COMMENT '分类名称',
  `order` INT NOT NULL DEFAULT 0 COMMENT '分类排序序号',
  created_at VARCHAR(19) COMMENT '创建时间，格式：yyyy-MM-dd HH:mm:ss',
  updated_at VARCHAR(19) COMMENT '更新时间，格式：yyyy-MM-dd HH:mm:ss',
  INDEX idx_order (`order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导航分类表';

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
INSERT INTO panel_categories (name, `order`, created_at, updated_at) VALUES
('搜索引擎', 1, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('电商购物', 2, '2023-01-01 00:00:00', '2023-01-01 00:00:00');

-- 初始化示例导航项数据
INSERT INTO panel_navigation_items (name, url, logo, category_id, description, internal_url, sort_order, created_at, updated_at) VALUES
('Bitwarden', 'https://vault.bitwarden.com', '/logo/bitwarden-logo.svg', 1, '密码管理器', 'http://192.168.1.100:8080', 1, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('Cloudflare', 'https://dash.cloudflare.com', '/logo/cloudflare.svg', 1, 'CDN和DNS服务', NULL, 2, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('DDNS', 'https://ddns.example.com', '/logo/ddns.svg', 1, '动态DNS服务', 'http://192.168.1.101:3000', 3, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('DeepSeek', 'https://chat.deepseek.com', '/logo/deepseek.svg', 1, 'AI对话助手', NULL, 4, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('DNSPod', 'https://console.dnspod.cn', '/logo/DNSPod.svg', 1, 'DNS解析服务', NULL, 5, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('Docker', 'https://hub.docker.com', '/logo/docker-official.svg', 1, '容器镜像仓库', 'http://192.168.1.102:9000', 6, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('Yacd', 'http://yacd.haishan.me', '/logo/yacd-128.png', 1, 'Clash代理面板', 'http://192.168.1.103:9090/ui', 7, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('DNSPod', 'https://www.bing.com', '/logo/DNSPod.svg', 2, NULL, NULL, 1, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('Docker', 'https://www.bing.com', '/logo/docker-official.svg', 2, NULL, NULL, 2, '2023-01-01 00:00:00', '2023-01-01 00:00:00'),
('Yacd', 'https://www.bing.com', '/logo/yacd-128.png', 2, NULL, NULL, 3, '2023-01-01 00:00:00', '2023-01-01 00:00:00');

-- 创建系统配置表
CREATE TABLE IF NOT EXISTS panel_system_config (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID，自增主键',
  config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键名',
  config_value TEXT COMMENT '配置值',
  description VARCHAR(255) COMMENT '配置描述',
  created_at VARCHAR(19) COMMENT '创建时间，格式：yyyy-MM-dd HH:mm:ss',
  updated_at VARCHAR(19) COMMENT '更新时间，格式：yyyy-MM-dd HH:mm:ss',
  INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';
