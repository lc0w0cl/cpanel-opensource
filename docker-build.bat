@echo off
chcp 65001 >nul

echo 🐳 Docker构建和部署脚本
echo ==================================

REM 检查Docker是否安装
docker --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker未安装，请先安装Docker
    pause
    exit /b 1
)

REM 检查docker-compose是否安装
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo ❌ docker-compose未安装，请先安装docker-compose
    pause
    exit /b 1
)

REM 检查是否在项目根目录
if not exist "docker-compose.yml" (
    echo ❌ 请在项目根目录运行此脚本
    pause
    exit /b 1
)

REM 创建.env文件（如果不存在）
if not exist ".env" (
    echo 📝 创建.env文件...
    copy ".env.example" ".env"
    echo ✅ 已创建.env文件，请根据需要修改配置
)

REM 停止现有容器
echo 🛑 停止现有容器...
docker-compose down

REM 清理旧镜像（可选）
set /p cleanup="是否清理旧的应用镜像? (y/N): "
if /i "%cleanup%"=="y" (
    echo 🧹 清理旧镜像...
    docker rmi cpanel-cpanel-app 2>nul
)

REM 构建镜像
echo 🔨 构建Docker镜像...
docker-compose build --no-cache

if errorlevel 1 (
    echo ❌ 构建失败
    pause
    exit /b 1
)

REM 启动服务
echo 🚀 启动服务...
docker-compose up -d

if errorlevel 1 (
    echo ❌ 启动失败
    pause
    exit /b 1
)

REM 等待服务启动
echo ⏳ 等待服务启动...
timeout /t 15 /nobreak >nul

REM 检查服务状态
echo 📊 检查服务状态...
docker-compose ps

REM 显示访问信息
echo.
echo ✅ 部署完成！
echo ==================================
echo 🌐 访问地址:
echo    主应用: http://localhost:8080

echo.
echo 📝 默认登录信息:
echo    用户名: admin
echo    密码: admin

echo.
echo 🔧 常用命令:
echo    查看日志: docker-compose logs -f cpanel-app
echo    重启应用: docker-compose restart cpanel-app
echo    停止服务: docker-compose down
echo    查看状态: docker-compose ps

echo.
echo 📊 系统监控:
echo    系统信息: http://localhost:8080/api/system/info
echo    健康检查: http://localhost:8080/api/auth/status

pause
