#!/bin/bash

# Docker构建和部署脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_message() {
    echo -e "${2}${1}${NC}"
}

print_message "🐳 Docker构建和部署脚本" $BLUE
echo "=================================="

# 检查Docker是否安装
if ! command -v docker &> /dev/null; then
    print_message "❌ Docker未安装，请先安装Docker" $RED
    exit 1
fi

# 检查docker-compose是否安装
if ! command -v docker-compose &> /dev/null; then
    print_message "❌ docker-compose未安装，请先安装docker-compose" $RED
    exit 1
fi

# 检查是否在项目根目录
if [ ! -f "docker-compose.yml" ]; then
    print_message "❌ 请在项目根目录运行此脚本" $RED
    exit 1
fi

# 创建.env文件（如果不存在）
if [ ! -f ".env" ]; then
    print_message "📝 创建.env文件..." $YELLOW
    cp .env.example .env
    print_message "✅ 已创建.env文件，请根据需要修改配置" $GREEN
fi

# 停止现有容器
print_message "🛑 停止现有容器..." $YELLOW
docker-compose down

# 清理旧镜像（可选）
read -p "是否清理旧的应用镜像? (y/N): " cleanup
if [[ $cleanup =~ ^[Yy]$ ]]; then
    print_message "🧹 清理旧镜像..." $YELLOW
    docker rmi cpanel-cpanel-app 2>/dev/null || true
fi

# 构建镜像
print_message "🔨 构建Docker镜像..." $BLUE
docker-compose build --no-cache

# 启动服务
print_message "🚀 启动服务..." $GREEN
docker-compose up -d

# 等待服务启动
print_message "⏳ 等待服务启动..." $YELLOW
sleep 15

# 检查服务状态
print_message "📊 检查服务状态..." $BLUE
docker-compose ps

# 显示访问信息
echo ""
print_message "✅ 部署完成！" $GREEN
echo "=================================="
print_message "🌐 访问地址:" $BLUE
echo "   主应用: http://localhost:8080"

echo ""
print_message "📝 默认登录信息:" $BLUE
echo "   用户名: admin"
echo "   密码: admin"

echo ""
print_message "🔧 常用命令:" $BLUE
echo "   查看日志: docker-compose logs -f cpanel-app"
echo "   重启应用: docker-compose restart cpanel-app"
echo "   停止服务: docker-compose down"
echo "   查看状态: docker-compose ps"

echo ""
print_message "📊 系统监控:" $BLUE
echo "   系统信息: http://localhost:8080/api/system/info"
echo "   健康检查: http://localhost:8080/api/auth/status"
