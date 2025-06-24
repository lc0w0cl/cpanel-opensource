# CPanel 导航面板 Linux 部署教程

## 系统截图
![img.png](截图/监控页.png)

![img_1.png](截图/导航页.png)
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

1. **查看项目文档**: [GitHub Repository](https://github.com/lc0w0cl/cpanel-opensource)
2. **提交 Issue**: [GitHub Issues](https://github.com/your-repo/cpanel/issues)
3. **社区讨论**: [GitHub Discussions](https://github.com/your-repo/cpanel/discussions)

## 📝 更新日志

- **v1.0.0** (2024-01-01): 初始版本发布
- **v1.1.0** (2024-02-01): 添加文件上传功能
- **v1.2.0** (2024-03-01): 添加在线图标和 Iconify 支持

---

**部署完成后，访问 `http://your-server-ip:8081` 即可使用 CPanel 导航面板！** 
