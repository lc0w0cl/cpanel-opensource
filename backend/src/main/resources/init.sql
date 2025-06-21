-- 创建分类表
CREATE TABLE IF NOT EXISTS categories (
  id VARCHAR(50) PRIMARY KEY COMMENT '分类ID，主键',
  name VARCHAR(100) NOT NULL COMMENT '分类名称',
  `order` INT NOT NULL DEFAULT 0 COMMENT '分类排序序号',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_order (`order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导航分类表';

-- 创建导航项表
CREATE TABLE IF NOT EXISTS navigation_items (
  id VARCHAR(50) PRIMARY KEY COMMENT '导航项ID，主键',
  name VARCHAR(100) NOT NULL COMMENT '导航项名称',
  url VARCHAR(500) NOT NULL COMMENT '导航项外部URL',
  logo VARCHAR(255) NOT NULL COMMENT '导航项图标路径',
  category_id VARCHAR(50) NOT NULL COMMENT '所属分类ID，外键关联categories表',
  description VARCHAR(255) COMMENT '导航项描述',
  internal_url VARCHAR(500) COMMENT '导航项内部URL',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
  INDEX idx_category_id (category_id),
  INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导航项表';

-- 初始化示例分类数据
INSERT INTO categories (id, name, `order`) VALUES
('searchEngines', '搜索引擎', 1),
('ecommerce', '电商购物', 2)
ON DUPLICATE KEY UPDATE name=VALUES(name), `order`=VALUES(`order`);

-- 初始化示例导航项数据
INSERT INTO navigation_items (id, name, url, logo, category_id, description, internal_url) VALUES
('Bitwarden', 'Bitwarden', 'https://vault.bitwarden.com', '/logo/bitwarden-logo.svg', 'searchEngines', '密码管理器', 'http://192.168.1.100:8080'),
('google', 'Cloudflare', 'https://dash.cloudflare.com', '/logo/cloudflare.svg', 'searchEngines', 'CDN和DNS服务', NULL),
('bing', 'DDNS', 'https://ddns.example.com', '/logo/ddns.svg', 'searchEngines', '动态DNS服务', 'http://192.168.1.101:3000'),
('deepseek', 'DeepSeek', 'https://chat.deepseek.com', '/logo/deepseek.svg', 'searchEngines', 'AI对话助手', NULL),
('dnspod', 'DNSPod', 'https://console.dnspod.cn', '/logo/DNSPod.svg', 'searchEngines', 'DNS解析服务', NULL),
('docker', 'Docker', 'https://hub.docker.com', '/logo/docker-official.svg', 'searchEngines', '容器镜像仓库', 'http://192.168.1.102:9000'),
('yacd', 'Yacd', 'http://yacd.haishan.me', '/logo/yacd-128.png', 'searchEngines', 'Clash代理面板', 'http://192.168.1.103:9090/ui'),
('123', 'DNSPod', 'https://www.bing.com', '/logo/DNSPod.svg', 'ecommerce', NULL, NULL),
('121', 'Docker', 'https://www.bing.com', '/logo/docker-official.svg', 'ecommerce', NULL, NULL),
('134', 'Yacd', 'https://www.bing.com', '/logo/yacd-128.png', 'ecommerce', NULL, NULL)
ON DUPLICATE KEY UPDATE
  name=VALUES(name),
  url=VALUES(url),
  logo=VALUES(logo),
  category_id=VALUES(category_id),
  description=VALUES(description),
  internal_url=VALUES(internal_url);
