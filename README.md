# CPanel 导航面板 Linux 部署教程

## 📋 目录
- [系统要求](#系统要求)
- [快速部署（推荐）](#快速部署推荐)
- [配置说明](#配置说明)
- [常见问题](#常见问题)
- [维护操作](#维护操作)

## 🖥️ 系统要求

### 最低配置
- **操作系统**: Ubuntu 20.04+ / CentOS 7+ / Debian 10+
- **内存**: 1GB RAM
- **存储**: 5GB 可用空间
- **CPU**: 1核心

### 推荐配置
- **操作系统**: Ubuntu 22.04 LTS
- **内存**: 2GB RAM
- **存储**: 10GB 可用空间
- **CPU**: 2核心

## 🚀 快速部署（推荐）

### 使用预构建 Docker 镜像部署

#### 1. 安装 Docker 和 Docker Compose

**Ubuntu/Debian:**
```bash
# 更新包索引
sudo apt update

# 安装 Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 启动 Docker 服务
sudo systemctl start docker
sudo systemctl enable docker

# 将当前用户添加到 docker 组
sudo usermod -aG docker $USER

# 重新登录以使组权限生效
newgrp docker
```

**CentOS/RHEL:**
```bash
# 安装 Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 启动 Docker 服务
sudo systemctl start docker
sudo systemctl enable docker

# 将当前用户添加到 docker 组
sudo usermod -aG docker $USER

# 重新登录以使组权限生效
newgrp docker
```

#### 2. 下载配置文件

```bash
# 创建项目目录
mkdir -p /opt/cpanel
cd /opt/cpanel

# 下载环境变量配置模板
wget https://raw.githubusercontent.com/lc0w0cl/cpanel-opensource/main/.env.example -O .env

# 下载 docker-compose.yml 文件
wget https://raw.githubusercontent.com/lc0w0cl/cpanel-opensource/main/docker-compose.yml

# 下载数据库初始化脚本
mkdir -p init
wget https://raw.githubusercontent.com/lc0w0cl/cpanel-opensource/main/backend/src/main/resources/init.sql -O init/init.sql
```

#### 3. 配置环境变量

```bash
# 编辑环境变量文件
nano .env

# 修改以下关键配置：
# DB_ROOT_PASSWORD=your_strong_root_password_here
# DB_PASSWORD=your_strong_db_password_here
# JWT_SECRET=your_very_long_jwt_secret_key_for_authentication_system_2024

# 设置文件权限
chmod 600 .env
```

#### 5. 启动服务

```bash
# 启动所有服务
docker compose up -d

# 查看服务状态
docker compose ps

# 查看启动日志
docker compose logs -f
```

#### 6. 验证部署

```bash
# 等待服务启动（约2-3分钟）
echo "等待服务启动..."
sleep 180

# 检查服务健康状态
curl -f http://localhost:8081/api/auth/status

# 如果返回 {"status":"ok"} 则部署成功
echo "部署完成！访问 http://your-server-ip:8081 使用应用"
```

## 🔧 一键部署脚本

如果你想要更简单的部署方式，可以使用以下一键部署脚本：

```bash
# 下载并执行一键部署脚本
wget -O deploy-cpanel.sh https://raw.githubusercontent.com/lc0w0cl/cpanel-opensource/main/deploy.sh
chmod +x deploy-cpanel.sh
sudo ./deploy-cpanel.sh
```

### 一键脚本内容

```bash
#!/bin/bash
# CPanel 一键部署脚本

set -e

echo "🚀 开始部署 CPanel 导航面板..."

# 检查是否为 root 用户
if [[ $EUID -eq 0 ]]; then
   echo "❌ 请不要使用 root 用户运行此脚本"
   exit 1
fi

# 安装 Docker
echo "📦 安装 Docker..."
if ! command -v docker &> /dev/null; then
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -aG docker $USER
    echo "✅ Docker 安装完成"
else
    echo "✅ Docker 已安装"
fi

# 创建项目目录
echo "📁 创建项目目录..."
sudo mkdir -p /opt/cpanel
sudo chown $USER:$USER /opt/cpanel
cd /opt/cpanel

# 下载配置文件
echo "⬇️ 下载配置文件..."
wget -q https://raw.githubusercontent.com/lc0w0cl/cpanel-opensource/main/.env.example -O .env
mkdir -p init
wget -q https://raw.githubusercontent.com/lc0w0cl/cpanel-opensource/main/backend/src/main/resources/init.sql -O init/init.sql

# 生成随机密码
DB_ROOT_PASSWORD=$(openssl rand -base64 32)
DB_PASSWORD=$(openssl rand -base64 32)
JWT_SECRET=$(openssl rand -base64 64)

# 更新环境变量
sed -i "s/your_strong_root_password_here/$DB_ROOT_PASSWORD/g" .env
sed -i "s/your_strong_db_password_here/$DB_PASSWORD/g" .env
sed -i "s/your_very_long_jwt_secret_key_for_authentication_system_2024/$JWT_SECRET/g" .env

# 创建 docker-compose.yml
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: cpanel-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_DATABASE: ${DB_NAME:-cpanel}
      MYSQL_USER: ${DB_USERNAME:-cpanel_user}
      MYSQL_PASSWORD: ${DB_PASSWORD}
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    networks:
      - cpanel-network
    command:
      - --default-authentication-plugin=mysql_native_password
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci

  cpanel-app:
    image: c0w0c/cpanel-app:1.0
    container_name: cpanel-app
    restart: unless-stopped
    environment:
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: ${DB_NAME:-cpanel}
      DB_USERNAME: ${DB_USERNAME:-cpanel_user}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION: ${JWT_EXPIRATION:-86400000}
      JWT_REFRESH_EXPIRATION: ${JWT_REFRESH_EXPIRATION:-604800000}
      JWT_ISSUER: ${JWT_ISSUER:-cpanel}
      JWT_AUDIENCE: ${JWT_AUDIENCE:-cpanel-users}
      SPRING_PROFILES_ACTIVE: docker
      LOG_LEVEL: ${LOG_LEVEL:-INFO}
      TZ: Asia/Shanghai
      JAVA_OPTS: "-Xmx512m -Xms256m"
    ports:
      - "8081:8080"
    volumes:
      - app_uploads:/app/uploads
      - app_logs:/app/logs
    networks:
      - cpanel-network
    depends_on:
      - mysql

networks:
  cpanel-network:
    driver: bridge

volumes:
  mysql_data:
    driver: local
  app_uploads:
    driver: local
  app_logs:
    driver: local
EOF

# 启动服务
echo "🚀 启动服务..."
newgrp docker << EONG
docker compose up -d
EONG

echo "⏳ 等待服务启动..."
sleep 60

# 检查服务状态
if curl -f http://localhost:8081/api/auth/status &> /dev/null; then
    echo "✅ 部署成功！"
    echo "🌐 访问地址: http://$(hostname -I | awk '{print $1}'):8081"
    echo "📝 配置文件位置: /opt/cpanel/.env"
    echo "📊 查看日志: cd /opt/cpanel && docker compose logs -f"
else
    echo "❌ 部署可能存在问题，请检查日志: docker compose logs"
fi
```

## ⚙️ 配置说明

### 环境变量说明

| 变量名 | 说明 | 默认值 | 必填 |
|--------|------|--------|------|
| `DB_HOST` | 数据库主机 | localhost | ✅ |
| `DB_PORT` | 数据库端口 | 3306 | ✅ |
| `DB_NAME` | 数据库名称 | cpanel | ✅ |
| `DB_USERNAME` | 数据库用户名 | cpanel_user | ✅ |
| `DB_PASSWORD` | 数据库密码 | - | ✅ |
| `JWT_SECRET` | JWT 密钥 | - | ✅ |
| `JWT_EXPIRATION` | JWT 过期时间(ms) | 86400000 | ❌ |
| `JWT_REFRESH_EXPIRATION` | 刷新令牌过期时间(ms) | 604800000 | ❌ |
| `LOG_LEVEL` | 日志级别 | INFO | ❌ |

### 端口说明

- **8081**: 应用主端口（Docker 部署）
- **8080**: 应用主端口（手动部署）
- **3306**: MySQL 数据库端口

### 目录结构

```
/opt/cpanel/
├── backend/                 # 后端源码
├── frontend/               # 前端源码
├── uploads/                # 文件上传目录
├── logs/                   # 日志目录
├── docker-compose.yml      # Docker 编排文件
├── Dockerfile             # Docker 镜像构建文件
└── .env                   # 环境变量配置
```

## 🔒 安全配置

### 1. 防火墙配置

```bash
# Ubuntu/Debian (使用 ufw)
sudo ufw allow 22/tcp          # SSH
sudo ufw allow 8081/tcp        # 应用端口
sudo ufw enable

# CentOS/RHEL (使用 firewalld)
sudo firewall-cmd --permanent --add-port=22/tcp
sudo firewall-cmd --permanent --add-port=8081/tcp
sudo firewall-cmd --reload
```

### 2. SSL/TLS 配置 (使用 Nginx 反向代理)

```bash
# 安装 Nginx
sudo apt install -y nginx  # Ubuntu/Debian
# 或
sudo yum install -y nginx   # CentOS/RHEL

# 创建 Nginx 配置
sudo tee /etc/nginx/sites-available/cpanel << 'EOF'
server {
    listen 80;
    server_name your-domain.com;

    # 重定向到 HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    # SSL 证书配置
    ssl_certificate /etc/ssl/certs/cpanel.crt;
    ssl_certificate_key /etc/ssl/private/cpanel.key;

    # SSL 安全配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;

    # 反向代理配置
    location / {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # 静态文件缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        proxy_pass http://localhost:8081;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
EOF

# 启用站点
sudo ln -s /etc/nginx/sites-available/cpanel /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### 3. 使用 Let's Encrypt 免费 SSL 证书

```bash
# 安装 Certbot
sudo apt install -y certbot python3-certbot-nginx  # Ubuntu/Debian
# 或
sudo yum install -y certbot python3-certbot-nginx   # CentOS/RHEL

# 获取 SSL 证书
sudo certbot --nginx -d your-domain.com

# 设置自动续期
sudo crontab -e
# 添加以下行：
# 0 12 * * * /usr/bin/certbot renew --quiet
```

## 🛠️ 常见问题

### Q1: 服务启动失败，提示端口被占用
```bash
# 查看端口占用情况
sudo netstat -tlnp | grep :8081
# 或
sudo ss -tlnp | grep :8081

# 杀死占用端口的进程
sudo kill -9 <PID>

# 重新启动服务
docker compose restart  # Docker 部署
# 或
sudo systemctl restart cpanel  # 手动部署
```

### Q2: 数据库连接失败
```bash
# 检查 MySQL 服务状态
sudo systemctl status mysql

# 检查数据库连接
mysql -u cpanel_user -p -h localhost cpanel

# 检查防火墙设置
sudo ufw status  # Ubuntu/Debian
sudo firewall-cmd --list-all  # CentOS/RHEL
```

### Q3: 文件上传失败
```bash
# 检查上传目录权限
ls -la /opt/cpanel/uploads/

# 修复权限
sudo chown -R cpanel:cpanel /opt/cpanel/uploads/
sudo chmod 755 /opt/cpanel/uploads/

# 检查磁盘空间
df -h
```

### Q4: 前端页面无法访问
```bash
# 检查 Nginx 状态
sudo systemctl status nginx

# 检查 Nginx 配置
sudo nginx -t

# 查看 Nginx 错误日志
sudo tail -f /var/log/nginx/error.log
```

## 🔄 维护操作

### 备份数据

```bash
#!/bin/bash
# 创建备份脚本 /opt/cpanel/backup.sh

BACKUP_DIR="/opt/cpanel/backups"
DATE=$(date +%Y%m%d_%H%M%S)

# 创建备份目录
mkdir -p $BACKUP_DIR

# 备份数据库
mysqldump -u cpanel_user -p cpanel > $BACKUP_DIR/database_$DATE.sql

# 备份上传文件
tar -czf $BACKUP_DIR/uploads_$DATE.tar.gz -C /opt/cpanel uploads/

# 备份配置文件
cp /opt/cpanel/.env $BACKUP_DIR/env_$DATE.backup

# 清理7天前的备份
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete
find $BACKUP_DIR -name "*.tar.gz" -mtime +7 -delete
find $BACKUP_DIR -name "*.backup" -mtime +7 -delete

echo "备份完成: $DATE"
```

```bash
# 设置定时备份
chmod +x /opt/cpanel/backup.sh
sudo crontab -e
# 添加以下行（每天凌晨2点备份）：
# 0 2 * * * /opt/cpanel/backup.sh
```

### 更新应用

**Docker 部署更新:**
```bash
cd /opt/cpanel

# 停止当前服务
docker compose down

# 拉取最新镜像
docker pull c0w0c/cpanel-app:latest

# 更新 docker-compose.yml 中的镜像版本
sed -i 's/c0w0c\/cpanel-app:1.0/c0w0c\/cpanel-app:latest/g' docker-compose.yml

# 重新启动服务
docker compose up -d

# 查看更新日志
docker compose logs -f cpanel-app
```

### 监控和日志

```bash
# 查看应用日志
docker compose logs -f cpanel-app  # Docker 部署
# 或
sudo journalctl -u cpanel -f      # 手动部署

# 查看系统资源使用情况
htop
# 或
docker stats  # Docker 部署

# 查看磁盘使用情况
df -h

# 查看数据库状态
docker compose exec mysql mysql -u root -p -e "SHOW PROCESSLIST;"  # Docker 部署
# 或
mysql -u root -p -e "SHOW PROCESSLIST;"  # 手动部署
```

### 性能优化

```bash
# 优化 MySQL 配置
sudo tee -a /etc/mysql/mysql.conf.d/mysqld.cnf << 'EOF'
[mysqld]
# 性能优化配置
innodb_buffer_pool_size = 256M
innodb_log_file_size = 64M
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT
query_cache_type = 1
query_cache_size = 32M
EOF

# 重启 MySQL
sudo systemctl restart mysql
```

## 📞 技术支持

如果在部署过程中遇到问题，可以通过以下方式获取帮助：

1. **查看项目文档**: [GitHub Repository](https://github.com/your-repo/cpanel)
2. **提交 Issue**: [GitHub Issues](https://github.com/your-repo/cpanel/issues)
3. **社区讨论**: [GitHub Discussions](https://github.com/your-repo/cpanel/discussions)

## 📝 更新日志

- **v1.0.0** (2024-01-01): 初始版本发布
- **v1.1.0** (2024-02-01): 添加文件上传功能
- **v1.2.0** (2024-03-01): 添加在线图标和 Iconify 支持

---

**部署完成后，访问 `http://your-server-ip:8081` 即可使用 CPanel 导航面板！**
