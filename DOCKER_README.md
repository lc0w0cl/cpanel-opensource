# Docker部署指南

## 快速开始

### 环境要求
- Docker 20.10+
- Docker Compose 2.0+
- 至少 2GB 可用内存

### 一键部署

**Linux/Mac:**
```bash
chmod +x docker-build.sh
./docker-build.sh
```

**Windows:**
```cmd
docker-build.bat
```

### 手动部署

```bash
# 1. 复制环境变量文件
cp .env.example .env

# 2. 构建并启动服务
docker-compose up -d --build

# 3. 查看服务状态
docker-compose ps
```

## 服务组件

- **cpanel-app**: 主应用（前端+后端）- http://localhost:8080
- **mysql**: MySQL 8.0 数据库 - 端口 3306

## 环境变量配置

编辑 `.env` 文件：

```bash
# 数据库配置
DB_ROOT_PASSWORD=rootpassword
DB_NAME=cpanel
DB_USERNAME=cpanel_user
DB_PASSWORD=cpanel_password

# JWT配置
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000
JWT_ISSUER=cpanel
JWT_AUDIENCE=cpanel-users

# 应用配置
LOG_LEVEL=INFO
```

## 常用操作

### 查看日志
```bash
# 查看所有服务日志
docker-compose logs -f

# 查看应用日志
docker-compose logs -f cpanel-app

# 查看数据库日志
docker-compose logs -f mysql
```

### 重启服务
```bash
# 重启所有服务
docker-compose restart

# 重启应用
docker-compose restart cpanel-app
```

### 停止服务
```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷（谨慎使用）
docker-compose down -v
```

### 更新应用
```bash
# 重新构建并启动
docker-compose up -d --build cpanel-app
```

### 数据库操作
```bash
# 进入MySQL容器
docker-compose exec mysql mysql -u root -p

# 备份数据库
docker-compose exec mysql mysqldump -u root -p cpanel > backup.sql

# 恢复数据库
docker-compose exec -T mysql mysql -u root -p cpanel < backup.sql
```

## 访问信息

- **主应用**: http://localhost:8080
- **默认用户名**: admin
- **默认密码**: admin

## 监控和调试

### 健康检查
```bash
# 检查应用状态
curl http://localhost:8080/api/auth/status

# 检查系统信息
curl http://localhost:8080/api/system/info
```

### 资源监控
```bash
# 查看容器资源使用
docker stats

# 查看容器详细信息
docker-compose top
```

## 故障排除

### 常见问题

1. **端口冲突**
```bash
# 检查端口占用
netstat -tulpn | grep :8080
# 修改docker-compose.yml中的端口映射
```

2. **内存不足**
```bash
# 检查系统内存
free -h
# 减少JVM内存分配
```

3. **数据库连接失败**
```bash
# 检查MySQL容器状态
docker-compose logs mysql
```

4. **构建失败**
```bash
# 清理Docker缓存
docker system prune -a
# 重新构建
docker-compose build --no-cache
```

## 数据持久化

以下数据会持久化保存：
- MySQL数据: `mysql_data` 卷
- 应用上传文件: `app_uploads` 卷
- 应用日志: `app_logs` 卷

## 安全建议

1. 修改默认密码
2. 使用强密码
3. 定期备份数据
4. 监控日志文件

## 性能优化

### JVM调优
在docker-compose.yml中调整：
```yaml
environment:
  JAVA_OPTS: "-Xmx1g -Xms512m"
```

### MySQL优化
增加内存分配：
```yaml
command: --innodb-buffer-pool-size=256M
```
